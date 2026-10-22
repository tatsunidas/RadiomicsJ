/*
 * Copyright [2026] [Tatsuaki Kobayashi]

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package io.github.tatsunidas.radiomics.gpu;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.vecmath.Point3f;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import io.github.tatsunidas.radiomics.features.LocalIntensityFeatures;
import io.github.tatsunidas.radiomics.features.MorphologicalFeatures;
import io.github.tatsunidas.radiomics.main.ImageFiltering;
import io.github.tatsunidas.radiomics.main.RadiomicsJ;

/**
 * Estimates how much faster the GPU tasks run on this machine.
 * <p>
 * The machine is measured, not assumed: small synthetic problems of each task run through the real code, once on the
 * CPU and once on the GPU, and {@code seconds = a + b * work + c * data} is fitted to each side. The fit is kept in
 * {@code ~/.radiomicsj/gpu-calibration.json} for the same device, CPU core count and RadiomicsJ version.
 * The estimate only advises; nothing here switches the GPU on or off.
 * <p>
 * This class does not touch JOCL itself (the device is reached through {@link Gpu}).
 */
public final class GpuEstimator {

	public enum Task {
		/** {@code ImageFiltering.gaborFilter} */
		GABOR,
		/** global intensity peak of the local intensity features */
		LOCAL_PEAK,
		/** maximum 3D diameter of the mesh */
		MAX_DIAMETER,
		/** {@code ImageFiltering.simoncelliFilter}, the Fourier transforms */
		SIMONCELLI
	}

	/**
	 * One problem size. {@code work} is what the arithmetic scales with and {@code data} what is moved or traversed
	 * once; they differ between the CPU and the GPU code where the algorithms differ (Gabor: FFT on the CPU, direct
	 * convolution on the GPU).
	 */
	public static final class Problem {
		public final Task task;
		final double cpuWork, cpuData, gpuWork, gpuData;

		private Problem(Task task, double cpuWork, double cpuData, double gpuWork, double gpuData) {
			this.task = task;
			this.cpuWork = cpuWork;
			this.cpuData = cpuData;
			this.gpuWork = gpuWork;
			this.gpuData = gpuData;
		}

		/**
		 * @param spacing voxel size x, y, z in mm
		 * @param planar true filters the axial plane only
		 * @param angles number of orientations
		 */
		public static Problem gabor(int w, int h, int d, double[] spacing, double sigmaMm, double lambdaMm, double gamma,
				boolean planar, int angles) {
			int[] size = { w, h, d };
			int[][] planes = planar || d < 2 ? new int[][] { { 2, 1, 0 } } : new int[][] { { 2, 1, 0 }, { 1, 2, 0 }, { 0, 2, 1 } };
			double sigmaLong = sigmaMm * Math.max(1, 1 / gamma);
			double cpuWork = 0, gpuWork = 0, data = 0;
			for (int[] plane : planes) {
				int slices = size[plane[0]], rows = size[plane[1]], cols = size[plane[2]];
				int rr = Math.min(rows / 2, (int) Math.ceil(6 * sigmaLong / spacing[plane[1]]));
				int rc = Math.min(cols / 2, (int) Math.ceil(6 * sigmaLong / spacing[plane[2]]));
				double lr = Integer.highestOneBit(Math.max(1, rows + 2 * rr - 1)) << 1;
				double lc = Integer.highestOneBit(Math.max(1, cols + 2 * rc - 1)) << 1;
				double voxels = (double) slices * rows * cols;
				cpuWork += angles * slices * lr * lc * Math.log(lr * lc) / Math.log(2);
				gpuWork += angles * voxels * (2 * rr + 1) * (2 * rc + 1);
				data += angles * voxels;
			}
			return new Problem(Task.GABOR, cpuWork, data, gpuWork, data);
		}

		/**
		 * @param roiVoxels voxels of the region
		 * @param spacing voxel size x, y, z in mm
		 */
		public static Problem localPeak(int w, int h, int d, long roiVoxels, double[] spacing) {
			double r = Math.cbrt(3 / (4 * Math.PI)) * 10;// the sphere of 1 cm3, in mm
			int rx = (int) Math.round(r / spacing[0]), ry = (int) Math.round(r / spacing[1]), rz = (int) Math.round(r / spacing[2]);
			long offsets = 0;
			for (int z = -rz; z <= rz; z++) {
				for (int y = -ry; y <= ry; y++) {
					for (int x = -rx; x <= rx; x++) {
						if (Math.sqrt(Math.pow(x * spacing[0], 2) + Math.pow(y * spacing[1], 2) + Math.pow(z * spacing[2], 2)) <= r) {
							offsets++;
						}
					}
				}
			}
			double work = (double) roiVoxels * Math.max(1, offsets);
			double data = (double) w * h * d;
			return new Problem(Task.LOCAL_PEAK, work, data, work, data);
		}

		/**
		 * @param padded true for a padding other than PERIODIC, which extends every axis by min(N / 2, 2^(level + 3))
		 * @param planar true filters every axial slice on its own
		 */
		public static Problem simoncelli(int w, int h, int d, int level, boolean padded, boolean planar) {
			int[] n = { w, h, planar ? 1 : d };
			int[] e = new int[3];
			for (int a = 0; a < 3; a++) {
				e[a] = n[a] + (padded && n[a] > 1 ? 2 * Math.min(n[a] / 2, 1 << Math.min(level + 3, 20)) : 0);
			}
			double voxels = (double) e[0] * e[1] * e[2];
			double work = 0;
			for (int a = 0; a < 3; a++) {
				if (e[a] < 2) {
					continue;
				}
				boolean pow2 = (e[a] & (e[a] - 1)) == 0;
				double m = pow2 ? e[a] : Integer.highestOneBit(2 * e[a] - 1) << 1;
				// a power of two is one transform of the line, the chirp-z transform is three of the padded line
				work += voxels / e[a] * (pow2 ? 1 : 3) * m * Math.log(m) / Math.log(2);
			}
			work *= 2;// forward and inverse
			if (planar) {
				work *= d;
				voxels *= d;
			}
			return new Problem(Task.SIMONCELLI, work, voxels, work, voxels);
		}

		/**
		 * @param vertices vertices of the mesh, each position counted once
		 */
		public static Problem maxDiameter(long vertices) {
			double n = vertices;
			return new Problem(Task.MAX_DIAMETER, n * n, n, n * n, n);
		}
	}

	public static final class Estimate {
		public final Task task;
		/** seconds on the CPU */
		public final double cpuSeconds;
		/** seconds on the GPU, NaN when the GPU cannot be used */
		public final double gpuSeconds;
		/** the problem is more than 10 times larger than the largest one measured */
		public final boolean extrapolated;
		/** why the GPU has no estimate, or null */
		public final String note;

		Estimate(Task task, double cpuSeconds, double gpuSeconds, boolean extrapolated, String note) {
			this.task = task;
			this.cpuSeconds = cpuSeconds;
			this.gpuSeconds = gpuSeconds;
			this.extrapolated = extrapolated;
			this.note = note;
		}

		public boolean gpuAvailable() {
			return !Double.isNaN(gpuSeconds);
		}

		/** CPU seconds divided by GPU seconds: above 1 the GPU is faster; NaN without a GPU */
		public double speedup() {
			return gpuAvailable() ? cpuSeconds / gpuSeconds : Double.NaN;
		}

		public boolean recommendGpu() {
			return gpuAvailable() && gpuSeconds < cpuSeconds;
		}

		@Override
		public String toString() {
			if (!gpuAvailable()) {
				return String.format("%-13s CPU %8.2f s   GPU n/a (%s)", task, cpuSeconds, note);
			}
			return String.format("%-13s CPU %8.2f s   GPU %8.2f s   x%.1f  -> %s%s", task, cpuSeconds, gpuSeconds, speedup(),
					recommendGpu() ? "GPU" : "CPU", extrapolated ? "   (extrapolated: far beyond the measured sizes)" : "");
		}
	}

	private static final String[] SIDES = { "cpu", "gpu" };

	private static Map<String, double[]> cached;
	private static String cachedKey;

	private GpuEstimator() {
	}

	/** estimates for the given problems; measures the machine first when there is no matching calibration */
	public static synchronized List<Estimate> estimate(boolean recalibrate, Problem... problems) {
		Map<String, double[]> model = calibrate(recalibrate);
		String reason = gpuReason();
		List<Estimate> out = new ArrayList<>();
		for (Problem p : problems) {
			double[] cpu = model.get(p.task + ".cpu");
			double[] gpu = model.get(p.task + ".gpu");
			double c = predict(cpu, p.cpuWork, p.cpuData);
			if (gpu == null) {
				out.add(new Estimate(p.task, c, Double.NaN, false, reason));
				continue;
			}
			double g = predict(gpu, p.gpuWork, p.gpuData);
			boolean far = p.cpuWork > 10 * cpu[3] || p.gpuWork > 10 * gpu[3];
			out.add(new Estimate(p.task, c, g, far, null));
		}
		return out;
	}

	/** prints the estimates for a few typical sizes */
	public static void printTable(PrintStream out, boolean recalibrate) {
		double[] ct = { 0.98, 0.98, 2.5 };
		double[] iso = { 1, 1, 1 };
		List<String> names = new ArrayList<>();
		List<Problem> ps = new ArrayList<>();
		names.add("small 64x64x32 volume, ROI 50 %");
		ps.add(Problem.gabor(64, 64, 32, iso, 5, 2, 1.5, false, 4));
		ps.add(Problem.localPeak(64, 64, 32, 64 * 64 * 16, iso));
		ps.add(Problem.maxDiameter(5000));
		ps.add(Problem.simoncelli(64, 64, 32, 1, true, false));
		names.add("CT-sized 198x202x59, ROI 117145 voxels, 22080 unique mesh vertices");
		ps.add(Problem.gabor(198, 202, 59, ct, 5, 2, 1.5, false, 4));
		ps.add(Problem.localPeak(198, 202, 59, 117145, ct));
		ps.add(Problem.maxDiameter(22080));
		ps.add(Problem.simoncelli(198, 202, 59, 1, true, false));
		List<Estimate> es = estimate(recalibrate, ps.toArray(new Problem[0]));
		out.println("GPU estimate on this machine (device: " + deviceName() + ", CPU cores: " + Runtime.getRuntime().availableProcessors() + ")");
		out.println("Seconds are predicted from small measured problems, see " + file().getPath());
		for (int i = 0; i < es.size(); i++) {
			if (i % 4 == 0) {
				out.println(names.get(i / 4) + ":");
			}
			out.println("  " + es.get(i));
		}
	}

	// ---- calibration ---------------------------------------------------------------------------------------------

	private static String deviceName() {
		Cl cl = Gpu.get();
		return cl == null ? "none" : cl.deviceName;
	}

	private static String gpuReason() {
		String r = Gpu.unavailableReason();
		return r != null ? r : "no GPU device";
	}

	static String key() {
		return deviceName() + "|" + Runtime.getRuntime().availableProcessors() + "|" + RadiomicsJ.version;
	}

	static File file() {
		String p = System.getProperty("radiomicsj.calibration");
		return p != null ? new File(p) : new File(System.getProperty("user.home"), ".radiomicsj/gpu-calibration.json");
	}

	/** @return model name ({@code TASK.cpu}, {@code TASK.gpu}) to {a, b, c, largest measured work} */
	static synchronized Map<String, double[]> calibrate(boolean force) {
		boolean saved = RadiomicsJ.BOOL_USE_GPU;
		RadiomicsJ.BOOL_USE_GPU = true;// the estimate does not depend on the setting that decides what runs
		try {
			return calibrateOnDevice(force);
		} finally {
			RadiomicsJ.BOOL_USE_GPU = saved;
		}
	}

	private static Map<String, double[]> calibrateOnDevice(boolean force) {
		String key = key();
		if (!force) {
			if (cached != null && key.equals(cachedKey)) {
				return cached;
			}
			Map<String, double[]> m = load(key);
			if (m != null) {
				cached = m;
				cachedKey = key;
				return m;
			}
		}
		Map<String, double[]> m = measure();
		save(key, m);
		cached = m;
		cachedKey = key;
		return m;
	}

	static synchronized void forget() {
		cached = null;
		cachedKey = null;
	}

	private static Map<String, double[]> measure() {
		boolean saved = RadiomicsJ.BOOL_USE_GPU;
		Map<String, double[]> model = new LinkedHashMap<>();
		try {
			boolean haveGpu = Gpu.get() != null;
			for (Task task : Task.values()) {
				List<Object[]> cases = cases(task);// {Problem, Runnable}
				for (String side : SIDES) {
					boolean gpu = side.equals("gpu");
					if (gpu && !haveGpu) {
						continue;
					}
					RadiomicsJ.BOOL_USE_GPU = gpu;
					((Runnable) cases.get(0)[1]).run();// warm-up: JIT, kernel build, context
					double[][] x = new double[cases.size()][2];
					double[] y = new double[cases.size()];
					double top = 0;
					for (int i = 0; i < cases.size(); i++) {
						Problem p = (Problem) cases.get(i)[0];
						double t = seconds((Runnable) cases.get(i)[1]);
						x[i][0] = gpu ? p.gpuWork : p.cpuWork;
						x[i][1] = gpu ? p.gpuData : p.cpuData;
						y[i] = t;
						top = Math.max(top, x[i][0]);
					}
					if (gpu && Gpu.get() == null) {// the device gave up during the run: those seconds were the CPU's
						break;
					}
					double[] abc = fit(x, y);
					model.put(task + "." + side, new double[] { abc[0], abc[1], abc[2], top });
				}
			}
		} finally {
			RadiomicsJ.BOOL_USE_GPU = saved;
		}
		return model;
	}

	private static double seconds(Runnable r) {
		double best = Double.MAX_VALUE;
		for (int rep = 0; rep < 2; rep++) {
			long t0 = System.nanoTime();
			r.run();
			double t = (System.nanoTime() - t0) / 1e9;
			best = Math.min(best, t);
			if (t > 3) {
				break;
			}
		}
		return best;
	}

	private static List<Object[]> cases(Task task) {
		List<Object[]> out = new ArrayList<>();
		double[] iso = { 1, 1, 1 };
		switch (task) {
		case GABOR: {
			int[][] sizes = { { 64, 64, 8, 5 }, { 128, 128, 8, 5 }, { 128, 128, 16, 2 }, { 192, 192, 8, 3 }, { 96, 96, 16, 5 }, { 256, 256, 4, 2 } };
			for (int[] s : sizes) {
				ImagePlus imp = new ImagePlus("g", stack(s[0], s[1], s[2], 1));
				imp.getCalibration().pixelWidth = imp.getCalibration().pixelHeight = imp.getCalibration().pixelDepth = 1;
				double sigma = s[3];
				out.add(new Object[] { Problem.gabor(s[0], s[1], s[2], iso, sigma, 2, 1.5, true, 1), (Runnable) () -> ImageFiltering
						.gaborFilter(imp, sigma, 2, 1.5, 0, ImageFiltering.Padding.MIRROR, null, 0, true) });
			}
			break;
		}
		case LOCAL_PEAK: {
			double[] sp = { 1, 1, 2 };
			int[][] sizes = { { 32, 32, 16, 50 }, { 48, 48, 24, 50 }, { 64, 64, 30, 50 }, { 80, 80, 40, 50 }, { 100, 100, 40, 12 } };
			for (int[] s : sizes) {
				ImageStack img = stack(s[0], s[1], s[2], 7);
				ImageStack msk = new ImageStack(s[0], s[1]);
				long roi = 0;
				double frac = Math.cbrt(s[3] / 100.0);
				for (int z = 0; z < s[2]; z++) {
					byte[] m = new byte[s[0] * s[1]];
					for (int y = 0; y < s[1]; y++) {
						for (int x = 0; x < s[0]; x++) {
							double dx = (x - s[0] / 2.0) / (s[0] / 2.0 * frac * 1.4), dy = (y - s[1] / 2.0) / (s[1] / 2.0 * frac * 1.4),
									dz = (z - s[2] / 2.0) / (s[2] / 2.0 * frac * 1.4);
							if (dx * dx + dy * dy + dz * dz <= 1) {
								m[y * s[0] + x] = 1;
								roi++;
							}
						}
					}
					msk.addSlice(new ByteProcessor(s[0], s[1], m));
				}
				ImagePlus ii = new ImagePlus("i", img), mm = new ImagePlus("m", msk);
				for (ImagePlus p : new ImagePlus[] { ii, mm }) {
					p.getCalibration().pixelWidth = sp[0];
					p.getCalibration().pixelHeight = sp[1];
					p.getCalibration().pixelDepth = sp[2];
				}
				out.add(new Object[] { Problem.localPeak(s[0], s[1], s[2], roi, sp),
						(Runnable) () -> new LocalIntensityFeatures(ii, mm, 1).calculate("0F91") });
			}
			break;
		}
		case SIMONCELLI: {
			int[][] sizes = { { 32, 32, 16 }, { 48, 48, 24 }, { 64, 64, 32 }, { 96, 96, 40 }, { 128, 128, 48 } };
			for (int[] s : sizes) {
				ImagePlus imp = new ImagePlus("s", stack(s[0], s[1], s[2], 3));
				out.add(new Object[] { Problem.simoncelli(s[0], s[1], s[2], 1, true, false),
						(Runnable) () -> ImageFiltering.simoncelliFilter(imp, 1, ImageFiltering.Padding.MIRROR, false) });
			}
			break;
		}
		default: {
			for (int n : new int[] { 1000, 2000, 4000, 8000, 16000, 32000 }) {
				Random rnd = new Random(n);
				List<Point3f> pts = new ArrayList<>();
				for (int i = 0; i < n; i++) {
					pts.add(new Point3f(rnd.nextFloat() * 90, rnd.nextFloat() * 70, rnd.nextFloat() * 40));
				}
				out.add(new Object[] { Problem.maxDiameter(n), (Runnable) () -> MorphologicalFeatures.maximumDistance(pts) });
			}
		}
		}
		return out;
	}

	private static ImageStack stack(int w, int h, int d, long seed) {
		Random rnd = new Random(seed);
		ImageStack s = new ImageStack(w, h);
		for (int z = 0; z < d; z++) {
			float[] p = new float[w * h];
			for (int i = 0; i < p.length; i++) {
				p[i] = (float) (rnd.nextGaussian() * 60);
			}
			s.addSlice(new FloatProcessor(w, h, p));
		}
		return s;
	}

	// ---- fit -----------------------------------------------------------------------------------------------------

	static double predict(double[] m, double work, double data) {
		return Math.max(0, m[0] + m[1] * work + m[2] * data);
	}

	/**
	 * Least squares of {@code y = a + b x0 + c x1} with the relative error weighted(each row divided by y), none of the
	 * coefficients negative: a negative one is dropped and the rest fitted again.
	 */
	static double[] fit(double[][] x, double[] y) {
		int n = y.length;
		boolean[] on = { true, true, true };
		while (true) {
			int k = 0;
			int[] col = new int[3];
			for (int j = 0; j < 3; j++) {
				if (on[j]) {
					col[k++] = j;
				}
			}
			if (k == 0) {
				return new double[3];
			}
			double[][] a = new double[k][k + 1];
			for (int i = 0; i < n; i++) {
				double wgt = 1 / Math.max(y[i], 1e-9);
				double[] row = new double[k];
				for (int j = 0; j < k; j++) {
					row[j] = (col[j] == 0 ? 1 : x[i][col[j] - 1]) * wgt;
				}
				for (int p = 0; p < k; p++) {
					for (int q = 0; q < k; q++) {
						a[p][q] += row[p] * row[q];
					}
					a[p][k] += row[p] * y[i] * wgt;
				}
			}
			double[] sol = solve(a, k);
			int worst = -1;
			double most = 0;
			for (int j = 0; j < k; j++) {
				if (sol[j] < most) {
					most = sol[j];
					worst = col[j];
				}
			}
			if (worst < 0) {
				double[] out = new double[3];
				for (int j = 0; j < k; j++) {
					out[col[j]] = sol[j];
				}
				return out;
			}
			on[worst] = false;
		}
	}

	private static double[] solve(double[][] a, int k) {
		for (int i = 0; i < k; i++) {
			int piv = i;
			for (int r = i + 1; r < k; r++) {
				if (Math.abs(a[r][i]) > Math.abs(a[piv][i])) {
					piv = r;
				}
			}
			double[] t = a[i];
			a[i] = a[piv];
			a[piv] = t;
			if (Math.abs(a[i][i]) < 1e-300) {
				a[i][i] = 1e-300;// collinear columns: the ridge keeps it solvable, the sign test then drops one
			}
			for (int r = 0; r < k; r++) {
				if (r != i) {
					double f = a[r][i] / a[i][i];
					for (int c = i; c <= k; c++) {
						a[r][c] -= f * a[i][c];
					}
				}
			}
		}
		double[] s = new double[k];
		for (int i = 0; i < k; i++) {
			s[i] = a[i][k] / a[i][i];
		}
		return s;
	}

	// ---- cache file ----------------------------------------------------------------------------------------------

	static void save(String key, Map<String, double[]> m) {
		StringBuilder sb = new StringBuilder("{\n  \"key\": \"" + key.replace("\\", "\\\\").replace("\"", "\\\"") + "\"");
		for (Map.Entry<String, double[]> e : m.entrySet()) {
			double[] v = e.getValue();
			sb.append(",\n  \"").append(e.getKey()).append("\": [").append(v[0]).append(", ").append(v[1]).append(", ")
					.append(v[2]).append(", ").append(v[3]).append("]");
		}
		sb.append("\n}\n");
		try {
			File f = file();
			if (f.getParentFile() != null) {
				f.getParentFile().mkdirs();
			}
			Files.write(f.toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			System.err.println("GpuEstimator: could not save the calibration (" + e.getMessage() + "), it is used for this run only.");
		}
	}

	static Map<String, double[]> load(String key) {
		File f = file();
		if (!f.isFile()) {
			return null;
		}
		try {
			String s = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
			Matcher k = Pattern.compile("\"key\":\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(s);
			if (!k.find() || !k.group(1).replace("\\\"", "\"").replace("\\\\", "\\").equals(key)) {
				return null;
			}
			Map<String, double[]> m = new LinkedHashMap<>();
			Matcher e = Pattern.compile("\"([A-Z_]+\\.(?:cpu|gpu))\":\\s*\\[([^\\]]*)\\]").matcher(s);
			while (e.find()) {
				String[] t = e.group(2).split(",");
				if (t.length != 4) {
					return null;
				}
				double[] v = new double[4];
				for (int i = 0; i < 4; i++) {
					v[i] = Double.parseDouble(t[i].trim());
				}
				m.put(e.group(1), v);
			}
			for (Task task : Task.values()) {
				if (!m.containsKey(task + ".cpu")) {
					return null;
				}
			}
			return m;
		} catch (IOException | NumberFormatException ex) {
			return null;
		}
	}
}
