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

import java.util.Arrays;
import java.util.Properties;
import java.util.Random;

import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import io.github.tatsunidas.radiomics.main.RadiomicsJ;
import io.github.tatsunidas.radiomics.main.SettingParams;

/**
 * CPU time of each feature family on a synthetic CT sized volume, one family at a time.
 * Run by hand: java -cp ... radiomics.FamilyBenchmark [size repeats family ...]
 * size is the edge of a cube in voxels. With no family given, all of them are run.
 *
 * @author tatsunidas <t_kobayashi@vis-ionary.com>
 */
public class FamilyBenchmark {

	private static final String[] FAMILIES = { "Morphological", "LocalIntensityFeatures", "IntensityBasedStatistics",
			"IntensityHistogram", "IntensityVolumeHistogram", "GLCM", "GLRLM", "GLSZM", "GLDZM", "GLAM", "NGTDM",
			"NGLDM" };

	public static void main(String[] args) throws Exception {
		String source = args.length > 0 ? args[0] : "64";// an edge length, or real:<STS id>:<modality> e.g. real:001:CT
		int repeats = args.length > 1 ? Integer.parseInt(args[1]) : 3;
		String[] families = args.length > 2 ? Arrays.copyOfRange(args, 2, args.length) : FAMILIES;
		ImagePlus[] pair;
		if (source.startsWith("real:")) {
			String[] t = source.split(":");
			pair = io.github.tatsunidas.radiomics.main.TestDataLoader.validationDataAt(t[1], t[2]);
		} else {
			pair = synthetic(Integer.parseInt(source));
		}
		System.out.println(source + ": " + pair[0].getWidth() + "x" + pair[0].getHeight() + "x" + pair[0].getNSlices()
				+ " voxels, spacing " + pair[0].getCalibration().pixelWidth + "x" + pair[0].getCalibration().pixelHeight
				+ "x" + pair[0].getCalibration().pixelDepth + ", roi voxels " + roiVoxels(pair[1]) + ", "
				+ Runtime.getRuntime().availableProcessors() + " cores, java " + System.getProperty("java.version"));
		for (String family : families) {
			double[] sec = new double[repeats + 1];// the first run is a warm up and is not reported
			for (int i = 0; i < sec.length; i++) {
				RadiomicsJ radiomics = new RadiomicsJ();
				radiomics.loadSettings(settings(family));
				long t0 = System.nanoTime();
				radiomics.execute(pair[0], pair[1], 1);
				sec[i] = (System.nanoTime() - t0) / 1e9;
			}
			double[] timed = Arrays.copyOfRange(sec, 1, sec.length);
			Arrays.sort(timed);
			System.out.printf("%-26s median %7.3f s  min %7.3f s  max %7.3f s  (warm up %.3f s)%n", family,
					timed[timed.length / 2], timed[0], timed[timed.length - 1], sec[0]);
		}
	}

	private static Properties settings(String family) {
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

	/** A smooth blob with Gaussian noise inside an ellipsoid ROI. The seed is fixed. */
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

	private static int roiVoxels(ImagePlus mask) {
		int count = 0;
		ImageStack s = mask.getStack();
		for (int z = 1; z <= s.getSize(); z++) {
			ij.process.ImageProcessor ip = s.getProcessor(z);
			for (int i = 0; i < ip.getPixelCount(); i++) {
				if (ip.getf(i) != 0) {
					count++;
				}
			}
		}
		return count;
	}
}
