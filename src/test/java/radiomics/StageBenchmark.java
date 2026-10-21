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
package radiomics;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.Callable;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.measure.ResultsTable;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import io.github.tatsunidas.radiomics.main.ImageFiltering;
import io.github.tatsunidas.radiomics.main.ImageFiltering.Padding;
import io.github.tatsunidas.radiomics.main.ImageFiltering.Pooling;
import io.github.tatsunidas.radiomics.main.RadiomicsJ;
import io.github.tatsunidas.radiomics.main.SettingParams;

/**
 * CPU time of each stage of the extraction workflow (read, preprocess, image filters, features, output)
 * on a synthetic volume. Prints one CSV line per task:
 * stage,task,median_s,min_s,max_s. Run by hand:
 * java -cp ... radiomics.StageBenchmark [size repeats skipSlowFeatures(0|1)]
 *
 * @author tatsunidas <t_kobayashi@vis-ionary.com>
 */
public class StageBenchmark {

	private static final String[] FAMILIES = { "Morphological", "LocalIntensityFeatures", "IntensityBasedStatistics",
			"IntensityHistogram", "IntensityVolumeHistogram", "GLCM", "GLRLM", "GLSZM", "GLDZM", "GLAM", "NGTDM",
			"NGLDM" };
	private static final List<String> SLOW = Arrays.asList("Morphological", "GLAM", "NGTDM");

	private static int repeats;

	public static void main(String[] args) throws Exception {
		int size = args.length > 0 ? Integer.parseInt(args[0]) : 64;
		repeats = args.length > 1 ? Integer.parseInt(args[1]) : 3;
		boolean skipSlow = args.length > 2 && args[2].equals("1");
		ImagePlus[] pair = synthetic(size);
		ImagePlus img = pair[0];
		System.out.println("# " + size + "^3 voxels, " + Runtime.getRuntime().availableProcessors() + " cores, java "
				+ System.getProperty("java.version"));
		System.out.println("stage,task,median_s,min_s,max_s");

		// 1 read : the file is written once, then opened. DICOM / NIfTI decoding is not covered.
		File tif = File.createTempFile("stagebench", ".tif");
		tif.deleteOnExit();
		IJ.saveAs(img, "tif", tif.getAbsolutePath());
		time("1_read", "open_tiff", () -> IJ.openImage(tif.getAbsolutePath()));

		// 2 preprocess : resample (trilinear, to 1.5 mm), outlier removal, through preprocess()
		time("2_preprocess", "preprocess_no_resample", () -> {
			RadiomicsJ r = new RadiomicsJ();
			r.loadSettings(base("IntensityBasedStatistics"));
			r.preprocess(img, pair[1], 1);
			return null;
		});
		time("2_preprocess", "preprocess_resample_1.5mm_trilinear", () -> {
			Properties p = base("IntensityBasedStatistics");
			p.put(SettingParams.DOUBLEARRAY_resamplingFactorXYZ.name(), "1.5,1.5,1.5");
			p.put(SettingParams.INT_interpolation3D.name(), String.valueOf(RadiomicsJ.TRILINEAR));
			p.put(SettingParams.INT_interpolation_mask3D.name(), String.valueOf(RadiomicsJ.TRILINEAR));
			RadiomicsJ r = new RadiomicsJ();
			r.loadSettings(p);
			r.preprocess(img, pair[1], 1);
			return null;
		});

		// 3 image filters : one pass each, 3D
		for (double sigma : new double[] { 1.0, 3.0 }) {
			time("3_filter", "LoG_sigma" + sigma + "mm",
					() -> ImageFiltering.laplacianOfGaussian(img, sigma, 4.0, Padding.MIRROR, false));
		}
		time("3_filter", "mean_support5", () -> ImageFiltering.meanFilter(img, 5, Padding.MIRROR, false));
		time("3_filter", "laws_L5E5E5",
				() -> ImageFiltering.lawsFilter(img, "L5E5E5", Padding.MIRROR, Pooling.MAX, 0, false));
		time("3_filter", "gabor_5_2_1.5_0",
				() -> ImageFiltering.gaborFilter(img, 5.0, 2.0, 1.5, 0.0, Padding.MIRROR, Pooling.MAX, Math.PI / 8, false));
		time("3_filter", "wavelet_db3_LLH_1",
				() -> ImageFiltering.waveletFilter(img, "db3", "LLH", 1, Padding.MIRROR, Pooling.MAX, false));
		time("3_filter", "simoncelli_level1", () -> ImageFiltering.simoncelliFilter(img, 1, Padding.MIRROR, false));

		// 4 features : whole execute() with one family on, so every row includes the preprocess baseline
		for (String family : FAMILIES) {
			if (skipSlow && SLOW.contains(family)) {
				continue;
			}
			time("4_features", family, () -> {
				RadiomicsJ r = new RadiomicsJ();
				r.loadSettings(base(family));
				return r.execute(img, pair[1], 1);
			});
		}

		// 5 output : the result table of a full run (all fast families) written as csv
		Properties all = base("IntensityBasedStatistics");
		for (String f : FAMILIES) {
			if (!SLOW.contains(f)) {
				all.put("BOOL_enable" + f, "1");
			}
		}
		RadiomicsJ r = new RadiomicsJ();
		r.loadSettings(all);
		ResultsTable table = r.execute(img, pair[1], 1);
		File csv = File.createTempFile("stagebench", ".csv");
		csv.deleteOnExit();
		time("5_output", "save_csv_" + table.getLastColumn() + "cols", () -> {
			table.saveAs(csv.getAbsolutePath());
			return null;
		});
	}

	private static void time(String stage, String task, Callable<?> body) throws Exception {
		double[] sec = new double[repeats + 1];// the first run is a warm up and is not reported
		for (int i = 0; i < sec.length; i++) {
			long t0 = System.nanoTime();
			body.call();
			sec[i] = (System.nanoTime() - t0) / 1e9;
		}
		double[] t = Arrays.copyOfRange(sec, 1, sec.length);
		Arrays.sort(t);
		System.out.printf("%s,%s,%.4f,%.4f,%.4f%n", stage, task, t[t.length / 2], t[0], t[t.length - 1]);
	}

	private static Properties base(String family) {
		Properties prop = new Properties();
		prop.put(SettingParams.INT_label.name(), "1");
		prop.put(SettingParams.BOOL_force2D.name(), "0");
		prop.put(SettingParams.BOOL_USE_FixedBinNumber.name(), "1");
		prop.put(SettingParams.INT_binCount.name(), "32");
		for (String f : FAMILIES) {
			prop.put("BOOL_enable" + f, f.equals(family) ? "1" : "0");
		}
		for (String f : new String[] { "Shape2D", "Homological", "Fractal", "OperationalInfo", "Diagnostics" }) {
			prop.put("BOOL_enable" + f, "0");
		}
		return prop;
	}

	private static ImagePlus[] synthetic(int n) {
		Random r = new Random(1L);
		ImageStack img = new ImageStack(n, n);
		ImageStack msk = new ImageStack(n, n);
		double c = (n - 1) / 2.0;
		for (int z = 0; z < n; z++) {
			float[] p = new float[n * n];
			byte[] m = new byte[n * n];
			for (int y = 0; y < n; y++) {
				for (int x = 0; x < n; x++) {
					double dx = (x - c) / (0.45 * n), dy = (y - c) / (0.40 * n), dz = (z - c) / (0.42 * n);
					double rr = dx * dx + dy * dy + dz * dz;
					p[y * n + x] = (float) (40.0 * Math.exp(-rr) + 20.0 * Math.sin(x * 0.2) * Math.cos(y * 0.15)
							+ r.nextGaussian() * 10.0);
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
