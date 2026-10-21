package radiomics.gpu;

import static org.jocl.CL.*;

import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_kernel;
import org.jocl.cl_mem;

import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.process.ImageProcessor;
import io.github.tatsunidas.radiomics.main.ImageFiltering;
import io.github.tatsunidas.radiomics.main.ImageFiltering.Padding;
import io.github.tatsunidas.radiomics.main.ImageFiltering.Pooling;

/**
 * Prototype of ImageFiltering.gaborFilter with the per plane convolution done on the GPU
 * as a direct sum instead of the FFT. The orientation pooling and the placing back on the image grid
 * are the CPU code, copied. Returns the response map as float[slice][y * w + x].
 */
public final class GaborGpu {

	/** nanoseconds of the last call: prepare, upload, kernel, download, rest */
	public static final long[] lastNs = new long[5];

	private GaborGpu() {
	}

	public static float[][] gabor(Cl cl, boolean fp64, ImagePlus imp, double sigma, double lambda, double gamma,
			double theta, Padding padding, Pooling pooling, double dTheta, boolean planar) {
		java.util.Arrays.fill(lastNs, 0);
		long t0 = System.nanoTime();
		int steps = ImageFiltering.gaborSteps(pooling, dTheta);
		int w = imp.getWidth(), h = imp.getHeight(), d = imp.getStackSize();
		Calibration cal = imp.getCalibration();
		double[] spacing = { cal.pixelWidth, cal.pixelHeight, d > 1 ? cal.pixelDepth : 1 };
		float[] flat = new float[w * h * d];
		ImageStack in = imp.getStack();
		for (int z = 0; z < d; z++) {
			ImageProcessor ip = in.getProcessor(z + 1);
			for (int i = 0; i < w * h; i++) {
				flat[z * w * h + i] = (float) cal.getCValue(ip.getf(i));
			}
		}
		int[][] planes = planar || d < 2 ? new int[][] { { 2, 1, 0 } } : new int[][] { { 2, 1, 0 }, { 1, 2, 0 }, { 0, 2, 1 } };
		int[] size = { w, h, d };
		int[] stride = { 1, w, w * h };// the step of one unit along x, y, z
		float[][] sum = new float[d][w * h];
		lastNs[0] += System.nanoTime() - t0;

		long t1 = System.nanoTime();
		cl_mem srcBuf = cl.in(Pointer.to(flat), (long) flat.length * Sizeof.cl_float);
		lastNs[1] += System.nanoTime() - t1;
		cl_kernel k = cl.kernel("gabor.cl", "gabor", fp64 ? "-DUSE_FP64" : "");
		try {
			for (int[] plane : planes) {
				int slices = size[plane[0]], rows = size[plane[1]], cols = size[plane[2]];
				float[][] pooled = null;
				for (int step = 0; step < steps; step++) {
					float[][] r = plane(cl, k, fp64, srcBuf, size, stride, plane, spacing, sigma, lambda, gamma,
							theta + step * dTheta, padding);
					long t5 = System.nanoTime();
					if (pooled == null) {
						pooled = r;
					} else {
						for (int s = 0; s < slices; s++) {
							for (int i = 0; i < rows * cols; i++) {
								float a = pooled[s][i], b = r[s][i];
								pooled[s][i] = pooling == Pooling.MAX ? Math.max(a, b) : pooling == Pooling.MIN ? Math.min(a, b) : a + b;
							}
						}
					}
					lastNs[4] += System.nanoTime() - t5;
				}
				long t6 = System.nanoTime();
				float norm = (pooling == Pooling.AVERAGE ? steps : 1) * planes.length;
				for (int s = 0; s < slices; s++) {
					for (int rr = 0; rr < rows; rr++) {
						for (int c = 0; c < cols; c++) {
							int[] xyz = new int[3];
							xyz[plane[0]] = s;
							xyz[plane[1]] = rr;
							xyz[plane[2]] = c;
							sum[xyz[2]][xyz[1] * w + xyz[0]] += pooled[s][rr * cols + c] / norm;
						}
					}
				}
				lastNs[4] += System.nanoTime() - t6;
			}
		} finally {
			clReleaseKernel(k);
			clReleaseMemObject(srcBuf);
		}
		return sum;
	}

	private static float[][] plane(Cl cl, cl_kernel k, boolean fp64, cl_mem srcBuf, int[] size, int[] stride,
			int[] plane, double[] spacing, double sigma, double lambda, double gamma, double theta, Padding padding) {
		long t0 = System.nanoTime();
		int slices = size[plane[0]], rows = size[plane[1]], cols = size[plane[2]];
		double sr = spacing[plane[1]], sc = spacing[plane[2]];
		double sigmaLong = sigma * Math.max(1, 1 / gamma);
		int rr = Math.min(rows / 2, (int) Math.ceil(6 * sigmaLong / sr));
		int rc = Math.min(cols / 2, (int) Math.ceil(6 * sigmaLong / sc));
		int kr = 2 * rr + 1, kc = 2 * rc + 1;
		double cos = Math.cos(theta), sin = Math.sin(theta);
		float[] kre32 = null, kim32 = null;
		double[] kre64 = null, kim64 = null;
		if (fp64) {
			kre64 = new double[kr * kc];
			kim64 = new double[kr * kc];
		} else {
			kre32 = new float[kr * kc];
			kim32 = new float[kr * kc];
		}
		for (int i = 0; i < kr; i++) {
			double y = (i - rr) * sr;
			for (int j = 0; j < kc; j++) {
				double x = (j - rc) * sc;
				double k1 = cos * x + sin * y, k2 = sin * x - cos * y;
				double env = Math.exp(-(k1 * k1 + gamma * gamma * k2 * k2) / (2 * sigma * sigma));
				double phase = 2 * Math.PI * k1 / lambda;
				if (fp64) {
					kre64[i * kc + j] = env * Math.cos(phase);
					kim64[i * kc + j] = env * Math.sin(phase);
				} else {
					kre32[i * kc + j] = (float) (env * Math.cos(phase));
					kim32[i * kc + j] = (float) (env * Math.sin(phase));
				}
			}
		}
		int[] rowAt = new int[rows + 2 * rr], colAt = new int[cols + 2 * rc];
		for (int i = 0; i < rowAt.length; i++) {
			rowAt[i] = padIndex(i - rr, rows, padding);
		}
		for (int i = 0; i < colAt.length; i++) {
			colAt[i] = padIndex(i - rc, cols, padding);
		}
		float[] dst = new float[slices * rows * cols];
		long t1 = System.nanoTime();
		GaborGpu.lastNs[0] += t1 - t0;

		int esz = fp64 ? Sizeof.cl_double : Sizeof.cl_float;
		cl_mem rowB = cl.in(Pointer.to(rowAt), (long) rowAt.length * Sizeof.cl_int);
		cl_mem colB = cl.in(Pointer.to(colAt), (long) colAt.length * Sizeof.cl_int);
		cl_mem reB = cl.in(fp64 ? Pointer.to(kre64) : Pointer.to(kre32), (long) kr * kc * esz);
		cl_mem imB = cl.in(fp64 ? Pointer.to(kim64) : Pointer.to(kim32), (long) kr * kc * esz);
		cl_mem dstB = cl.out((long) dst.length * Sizeof.cl_float);
		long t2 = System.nanoTime();
		GaborGpu.lastNs[1] += t2 - t1;

		int a = 0;
		cl.arg(k, a++, srcBuf);
		cl.arg(k, a++, rowB);
		cl.arg(k, a++, colB);
		cl.arg(k, a++, stride[plane[0]]);
		cl.arg(k, a++, stride[plane[1]]);
		cl.arg(k, a++, stride[plane[2]]);
		cl.arg(k, a++, rows);
		cl.arg(k, a++, cols);
		cl.arg(k, a++, rr);
		cl.arg(k, a++, rc);
		cl.arg(k, a++, kr);
		cl.arg(k, a++, kc);
		cl.arg(k, a++, reB);
		cl.arg(k, a++, imB);
		cl.arg(k, a++, dstB);
		cl.run(k, cols, rows, slices);
		long t3 = System.nanoTime();
		GaborGpu.lastNs[2] += t3 - t2;

		cl.read(dstB, Pointer.to(dst), (long) dst.length * Sizeof.cl_float);
		for (cl_mem m : new cl_mem[] { rowB, colB, reB, imB, dstB }) {
			clReleaseMemObject(m);
		}
		long t4 = System.nanoTime();
		GaborGpu.lastNs[3] += t4 - t3;
		float[][] out = new float[slices][rows * cols];
		for (int s = 0; s < slices; s++) {
			System.arraycopy(dst, s * rows * cols, out[s], 0, rows * cols);
		}
		GaborGpu.lastNs[4] += System.nanoTime() - t4;
		return out;
	}

	// same as ImageFiltering.padIndex, which is private
	private static int padIndex(int i, int n, Padding padding) {
		if (i >= 0 && i < n) {
			return i;
		}
		switch (padding) {
		case CONSTANT:
			return -1;
		case NEAREST:
			return i < 0 ? 0 : n - 1;
		case MIRROR: {
			int m = Math.floorMod(i, 2 * n);
			return m < n ? m : 2 * n - 1 - m;
		}
		case PERIODIC:
			return Math.floorMod(i, n);
		default:
			throw new IllegalStateException("unknown padding " + padding);
		}
	}
}
