package io.github.tatsunidas.radiomics.gpu;

import static org.jocl.CL.*;

import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_kernel;
import org.jocl.cl_mem;

/**
 * The per plane convolution of ImageFiltering.gaborFilter as a direct sum on the GPU, in place of the FFT.
 * Reached only through {@link Cl#gaborPlane}, so only when a device is open.
 */
final class GaborGpu {

	private GaborGpu() {
	}

	/**
	 * @param src the volume as one array, voxel (x, y, z) at x * stride[0] + y * stride[1] + z * stride[2]
	 * @param stride the step in src of one unit along the slice, row and column axis of the plane
	 * @param rowAt padded row index to source row, -1 is a zero (same for colAt)
	 * @return the modulus, [slice][row * cols + col]
	 */
	static float[][] plane(Cl cl, float[] src, int[] stride, int slices, int rows, int cols, int rr, int rc,
			int[] rowAt, int[] colAt, double sr, double sc, double sigma, double lambda, double gamma, double theta,
			boolean fp64) {
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
		float[] dst = new float[slices * rows * cols];
		int esz = fp64 ? Sizeof.cl_double : Sizeof.cl_float;
		cl_kernel k = cl.kernel("gabor.cl", "gabor", fp64 ? "-DUSE_FP64" : "");
		cl_mem srcB = cl.in(Pointer.to(src), (long) src.length * Sizeof.cl_float);
		cl_mem rowB = cl.in(Pointer.to(rowAt), (long) rowAt.length * Sizeof.cl_int);
		cl_mem colB = cl.in(Pointer.to(colAt), (long) colAt.length * Sizeof.cl_int);
		cl_mem reB = cl.in(fp64 ? Pointer.to(kre64) : Pointer.to(kre32), (long) kr * kc * esz);
		cl_mem imB = cl.in(fp64 ? Pointer.to(kim64) : Pointer.to(kim32), (long) kr * kc * esz);
		cl_mem dstB = cl.out((long) dst.length * Sizeof.cl_float);
		try {
			int a = 0;
			cl.arg(k, a++, srcB);
			cl.arg(k, a++, rowB);
			cl.arg(k, a++, colB);
			cl.arg(k, a++, stride[0]);
			cl.arg(k, a++, stride[1]);
			cl.arg(k, a++, stride[2]);
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
			cl.read(dstB, Pointer.to(dst), (long) dst.length * Sizeof.cl_float);
		} finally {
			for (cl_mem m : new cl_mem[] { srcB, rowB, colB, reB, imB, dstB }) {
				clReleaseMemObject(m);
			}
			clReleaseKernel(k);
		}
		float[][] out = new float[slices][rows * cols];
		for (int s = 0; s < slices; s++) {
			System.arraycopy(dst, s * rows * cols, out[s], 0, rows * cols);
		}
		return out;
	}
}
