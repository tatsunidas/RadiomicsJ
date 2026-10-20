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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Properties;
import java.util.Random;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.ResultsTable;
import ij.process.FloatProcessor;
import io.github.tatsunidas.radiomics.main.ImageFiltering;
import io.github.tatsunidas.radiomics.main.ImageFiltering.Padding;
import io.github.tatsunidas.radiomics.main.ImageFiltering.Pooling;
import io.github.tatsunidas.radiomics.main.RadiomicsJ;

/**
 * Separable wavelets in the extraction settings: the features of the filtered image
 * join the row of the original.
 *
 * @author tatsunidas <t_kobayashi@vis-ionary.com>
 */
public class TestWaveletExtraction {

	private static final int SIZE = 20;
	private static final String[] ORIGINAL_ONLY = { "Morphology_", "Shape2D_", "Diagnostics_", "OperationalInfo_" };

	@AfterEach
	public void reset() {
		RadiomicsJ.resetSettings();
	}

	private static ImagePlus calibrated(ImagePlus imp, double sz) {
		imp.getCalibration().pixelWidth = 1d;
		imp.getCalibration().pixelHeight = 1d;
		imp.getCalibration().pixelDepth = sz;
		return imp;
	}

	private static ImagePlus image(double sz) {
		Random rnd = new Random(7);
		ImageStack st = new ImageStack(SIZE, SIZE);
		for (int z = 0; z < SIZE; z++) {
			FloatProcessor ip = new FloatProcessor(SIZE, SIZE);
			for (int i = 0; i < SIZE * SIZE; i++) {
				ip.setf(i, 100f + 50f * rnd.nextFloat());
			}
			st.addSlice(ip);
		}
		return calibrated(new ImagePlus("image", st), sz);
	}

	private static ImagePlus mask(double sz) {
		ImageStack st = new ImageStack(SIZE, SIZE);
		for (int z = 0; z < SIZE; z++) {
			FloatProcessor mp = new FloatProcessor(SIZE, SIZE);
			for (int y = 0; y < SIZE; y++) {
				for (int x = 0; x < SIZE; x++) {
					mp.setf(x, y, x >= 5 && x < 15 && y >= 5 && y < 15 && z >= 5 && z < 15 ? 1f : 0f);
				}
			}
			st.addSlice(mp);
		}
		return calibrated(new ImagePlus("mask", st), sz);
	}

	private static ResultsTable extract(ImagePlus img, ImagePlus msk, String wavelet, Pooling pooling, boolean force2D) throws Exception {
		RadiomicsJ rj = new RadiomicsJ();// the constructor resets the settings
		RadiomicsJ.force2D = force2D;
		RadiomicsJ.waveletSets = wavelet == null ? null : wavelet.split(";");
		RadiomicsJ.waveletPooling = pooling;
		return force2D ? rj.extractAllSlice(img, msk, 1) : rj.extractAll(img, msk, 1);
	}

	private static boolean originalOnly(String h) {
		for (String p : ORIGINAL_ONLY) {
			if (h.startsWith(p)) {
				return true;
			}
		}
		return false;
	}

	/** every column of the plain extraction of the filtered image has its twin under the prefix, with the same values */
	private static void assertTwins(ResultsTable manual, ResultsTable viaSettings, String prefix) {
		int compared = 0;
		for (String h : manual.getHeadings()) {
			if (h.equals(" ") || originalOnly(h)) {
				continue;
			}
			assertTrue(viaSettings.columnExists(viaSettings.getColumnIndex(prefix + h)), "missing " + prefix + h);
			double a = manual.getValue(h, 0);
			double b = viaSettings.getValue(prefix + h, 0);
			if (Double.isNaN(a)) {
				assertTrue(Double.isNaN(b), h);
			} else {
				assertEquals(a, b, 1e-9 * Math.max(1d, Math.abs(a)), h);
			}
			compared++;
		}
		assertTrue(compared > 50, "compared only " + compared + " columns");
	}

	private static String firstHistogram(ResultsTable rt) {
		for (String h : rt.getHeadings()) {
			if (h.startsWith("IntensityHistogram_")) {
				return h.substring("IntensityHistogram_".length());
			}
		}
		throw new AssertionError("no IntensityHistogram column");
	}

	@Test
	public void filteredFeaturesEqualTheFeaturesOfTheFilteredImage() throws Exception {
		// the kernels are in voxels, so the spacing may differ
		ImagePlus img = image(3d), msk = mask(3d);
		for (boolean force2D : new boolean[] { false, true }) {
			String letters = force2D ? "LH" : "LHL";
			ResultsTable rt = extract(img, msk, "db3:" + letters + ":2", Pooling.AVERAGE, force2D);
			ImagePlus filtered = ImageFiltering.waveletFilter(img, "db3", letters, 2, Padding.MIRROR, Pooling.AVERAGE, force2D);
			ResultsTable manual = extract(calibrated(filtered, 3d), msk, null, null, force2D);
			assertEquals(manual.size(), rt.size());
			assertTwins(manual, rt, "wavelet_db3_" + letters + "_l2_average_");
		}
	}

	@Test
	public void withoutWaveletsTheRowIsWhatItWas() throws Exception {
		ImagePlus img = image(1d), msk = mask(1d);
		ResultsTable plain = extract(img, msk, null, null, false);
		ResultsTable two = extract(img, msk, "haar:HHH:1;coif1:LLH:1", null, false);
		for (String h : plain.getHeadings()) {
			assertFalse(h.startsWith("wavelet_"), h);
			if (h.equals(" ") || h.startsWith("OperationalInfo_")) {
				continue;
			}
			double a = plain.getValue(h, 0), b = two.getValue(h, 0);
			if (Double.isNaN(a)) {
				assertTrue(Double.isNaN(b), h);
			} else {
				assertEquals(a, b, 0d, h);
			}
		}
		assertEquals(1, two.size());
		for (String p : new String[] { "wavelet_haar_HHH_l1_", "wavelet_coif1_LLH_l1_" }) {
			assertTrue(two.columnExists(two.getColumnIndex(p + "IntensityHistogram_" + firstHistogram(plain))), p);
			for (String h : two.getHeadings()) {
				if (h.startsWith(p)) {
					assertFalse(originalOnly(h.substring(p.length())), h);
				}
			}
		}
	}

	@Test
	public void theSettingsComeFromTheSettingsFile() throws Exception {
		ImagePlus img = image(1d), msk = mask(1d);
		RadiomicsJ rj = new RadiomicsJ();
		Properties prop = new Properties();
		prop.setProperty("STRING_waveletSets", "haar:hhh:1 ; DB2:LHL:2");
		prop.setProperty("STRING_waveletPadding", "periodic");
		prop.setProperty("STRING_waveletPooling", "max");
		rj.loadSettings(prop);
		assertEquals(2, RadiomicsJ.waveletSets.length);
		assertEquals(Padding.PERIODIC, RadiomicsJ.waveletPadding);
		assertEquals(Pooling.MAX, RadiomicsJ.waveletPooling);
		ResultsTable rt = rj.extractAll(img, msk, 1);
		ImagePlus filtered = ImageFiltering.waveletFilter(img, "db2", "LHL", 2, Padding.PERIODIC, Pooling.MAX, false);
		RadiomicsJ.resetSettings();
		assertEquals(null, RadiomicsJ.waveletSets);
		ResultsTable manual = extract(calibrated(filtered, 1d), msk, null, null, false);
		assertTwins(manual, rt, "wavelet_db2_LHL_l2_max_");
	}

	@Test
	public void aWrongSetIsRefused() {
		ImagePlus img = image(1d), msk = mask(1d);
		assertThrows(IllegalArgumentException.class, () -> extract(img, msk, "haar:HH:1", null, false));
		assertThrows(IllegalArgumentException.class, () -> extract(img, msk, "haar:HHH:1", null, true));
		assertThrows(IllegalArgumentException.class, () -> extract(img, msk, "db9:HHH:1", null, false));
		assertThrows(IllegalArgumentException.class, () -> extract(img, msk, "haar:HHH:0", null, false));
		assertThrows(IllegalArgumentException.class, () -> extract(img, msk, "haar:HHH", null, false));
	}
}
