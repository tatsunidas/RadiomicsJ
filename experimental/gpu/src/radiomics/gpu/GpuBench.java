package radiomics.gpu;

import java.util.Arrays;
import java.util.Random;

import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import io.github.tatsunidas.radiomics.features.NGTDMFeatures;
import io.github.tatsunidas.radiomics.main.ImageFiltering;
import io.github.tatsunidas.radiomics.main.ImageFiltering.Padding;
import io.github.tatsunidas.radiomics.main.ImageFiltering.Pooling;
import io.github.tatsunidas.radiomics.main.TestDataLoader;

/**
 * CPU against GPU for the Gabor filter and the NGTDM matrix, same input, both run here.
 * java ... radiomics.gpu.GpuBench <kernel dir> <case: 64 | 128 | real:001:CT> [repeats]
 * CSV lines: case,task,variant,median_s,min_s,max_s,max_abs_err,note
 */
public class GpuBench {

	static int repeats = 3;

	public static void main(String[] a) throws Exception {
		String kernelDir = a[0];
		String source = a[1];
		repeats = a.length > 2 ? Integer.parseInt(a[2]) : 3;
		ImagePlus[] pair = source.startsWith("real:") ? TestDataLoader.validationDataAt(source.split(":")[1], source.split(":")[2])
				: synthetic(Integer.parseInt(source));
		ImagePlus img = pair[0], mask = pair[1];
		System.out.println("# " + source + " " + img.getWidth() + "x" + img.getHeight() + "x" + img.getNSlices() + ", "
				+ Runtime.getRuntime().availableProcessors() + " cores");
		System.out.println("case,task,variant,median_s,min_s,max_s,max_abs_err,note");

		Cl cl = null;
		try {
			cl = Cl.open(kernelDir);
			System.out.println("# device " + cl.deviceName + " fp64=" + cl.fp64);
		} catch (Cl.GpuUnavailable e) {
			System.out.println("# GPU unavailable, CPU rows only: " + e.getMessage());
		}
		gabor(source, cl, img, 5.0, 2.0, 1.5, 0.0, null, 0.0);
		gabor(source, cl, img, 5.0, 2.0, 1.5, 0.0, Pooling.MAX, Math.PI / 4);
		ngtdm(source, cl, img, mask, 1);
		ngtdm(source, cl, img, mask, 2);
	}

	static void gabor(String src, Cl cl, ImagePlus img, double sigma, double lambda, double gamma, double theta,
			Pooling pooling, double dTheta) {
		String task = "gabor_s" + sigma + (pooling == null ? "_single" : "_pool" + (int) Math.rint(Math.PI / dTheta));
		float[][][] ref = new float[1][][];
		double[] t = time(() -> ref[0] = toArray(ImageFiltering.gaborFilter(img, sigma, lambda, gamma, theta, Padding.MIRROR, pooling, dTheta, false)));
		row(src, task, "cpu_fft_double", t, 0, "");
		if (cl == null) {
			return;
		}
		for (boolean fp64 : new boolean[] { false, true }) {
			if (fp64 && !cl.fp64) {
				continue;
			}
			float[][][] out = new float[1][][];
			long[] tot = new long[5];
			double[] g = time(() -> {
				out[0] = GaborGpu.gabor(cl, fp64, img, sigma, lambda, gamma, theta, Padding.MIRROR, pooling, dTheta, false);
				for (int i = 0; i < 5; i++) {
					tot[i] = GaborGpu.lastNs[i];
				}
			});
			double err = 0, scale = 0;
			for (int z = 0; z < ref[0].length; z++) {
				for (int i = 0; i < ref[0][z].length; i++) {
					err = Math.max(err, Math.abs(ref[0][z][i] - out[0][z][i]));
					scale = Math.max(scale, Math.abs(ref[0][z][i]));
				}
			}
			row(src, task, fp64 ? "gpu_direct_fp64" : "gpu_direct_fp32", g, err,
					String.format("max|ref|=%.4g rel=%.2e; last run s: prep %.3f up %.3f kernel %.3f down %.3f rest %.3f", scale,
							err / scale, tot[0] / 1e9, tot[1] / 1e9, tot[2] / 1e9, tot[3] / 1e9, tot[4] / 1e9));
		}
	}

	static void ngtdm(String src, Cl cl, ImagePlus img, ImagePlus mask, int delta) throws Exception {
		String task = "ngtdm_delta" + delta;
		NGTDMFeatures f = new NGTDMFeatures(img, mask, 1, delta, true, 32, null);// builds the matrix once
		double[] t = time(() -> f.fillNGTDM(false));
		double[][] ref = f.getMatrix();
		row(src, task, "cpu_original", t, 0, "");
		NgtdmGpu.Input in = null;
		try {
			in = NgtdmGpu.read(f);
		} catch (Exception e) {
			System.out.println("# input read failed: " + e);
			return;
		}
		final NgtdmGpu.Input input = in;
		if (!in.exact) {
			System.out.println("# " + task + ": integer shortcut is not exact for this input, would fall back to the original CPU path");
			return;
		}
		double[][][] m = new double[1][][];
		double[] s = time(() -> m[0] = NgtdmGpu.cpuSinglePass(input));
		row(src, task, "cpu_parallel_gather", s, diff(ref, m[0]), sameBits(ref, m[0]) ? "bit-identical" : "DIFFERS");
		if (cl == null) {
			return;
		}
		long[] tot = new long[5];
		double[] g = time(() -> {
			m[0] = NgtdmGpu.gpu(cl, input);
			for (int i = 0; i < 5; i++) {
				tot[i] = NgtdmGpu.lastNs[i];
			}
		});
		row(src, task, "gpu_gather", g, diff(ref, m[0]),
				(sameBits(ref, m[0]) ? "bit-identical" : "DIFFERS") + String.format("; last run s: prep %.3f up %.3f kernel %.3f down %.3f finish %.3f",
						tot[0] / 1e9, tot[1] / 1e9, tot[2] / 1e9, tot[3] / 1e9, tot[4] / 1e9));
	}

	// ---- helpers ----
	static double[] time(Runnable r) {
		long t0 = System.nanoTime();
		r.run();// the first call is also the cold one (JIT, program build); it is not in the table
		double cold = (System.nanoTime() - t0) / 1e9;
		double[] s = new double[repeats];
		for (int i = 0; i < repeats; i++) {
			long t = System.nanoTime();
			r.run();
			s[i] = (System.nanoTime() - t) / 1e9;
		}
		Arrays.sort(s);
		return new double[] { s[s.length / 2], s[0], s[s.length - 1], cold };
	}

	static void row(String src, String task, String variant, double[] t, double err, String note) {
		System.out.printf("%s,%s,%s,%.4f,%.4f,%.4f,%.3g,cold %.3f s; %s%n", src, task, variant, t[0], t[1], t[2], err, t[3], note);
	}

	static double diff(double[][] a, double[][] b) {
		double d = 0;
		for (int i = 0; i < a.length; i++) {
			for (int j = 0; j < a[i].length; j++) {
				d = Math.max(d, Math.abs(a[i][j] - b[i][j]));
			}
		}
		return d;
	}

	static boolean sameBits(double[][] a, double[][] b) {
		for (int i = 0; i < a.length; i++) {
			if (!Arrays.equals(a[i], b[i])) {
				return false;
			}
		}
		return true;
	}

	static float[][] toArray(ImagePlus imp) {
		int d = imp.getStackSize();
		float[][] f = new float[d][];
		for (int z = 0; z < d; z++) {
			ImageProcessor ip = imp.getStack().getProcessor(z + 1);
			f[z] = new float[ip.getPixelCount()];
			for (int i = 0; i < f[z].length; i++) {
				f[z][i] = ip.getf(i);
			}
		}
		return f;
	}

	static ImagePlus[] synthetic(int n) {
		Random r = new Random(1L);
		ImageStack img = new ImageStack(n, n), msk = new ImageStack(n, n);
		double c = (n - 1) / 2.0;
		for (int z = 0; z < n; z++) {
			float[] p = new float[n * n];
			byte[] m = new byte[n * n];
			for (int y = 0; y < n; y++) {
				for (int x = 0; x < n; x++) {
					double dx = (x - c) / (0.45 * n), dy = (y - c) / (0.40 * n), dz = (z - c) / (0.42 * n);
					double rr = dx * dx + dy * dy + dz * dz;
					p[y * n + x] = (float) (40.0 * Math.exp(-rr) + 20.0 * Math.sin(x * 0.2) * Math.cos(y * 0.15) + r.nextGaussian() * 10.0);
					m[y * n + x] = (byte) (rr <= 1.0 ? 1 : 0);
				}
			}
			img.addSlice(new FloatProcessor(n, n, p));
			msk.addSlice(new ByteProcessor(n, n, m));
		}
		ImagePlus[] out = { new ImagePlus("img", img), new ImagePlus("mask", msk) };
		for (ImagePlus imp : out) {
			Calibration cal = imp.getCalibration();
			cal.pixelWidth = 1.0;
			cal.pixelHeight = 1.0;
			cal.pixelDepth = 1.0;
			cal.setUnit("mm");
		}
		return out;
	}
}
