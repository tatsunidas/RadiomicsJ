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
 * Gabor filters in the extraction settings: the features of the filtered image
 * join the row of the original.
 *
 * @author tatsunidas <t_kobayashi@vis-ionary.com>
 */
public class TestGaborExtraction {

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

	private static ResultsTable extract(ImagePlus img, ImagePlus msk, String gabor, Pooling pooling, double dTheta, boolean force2D) throws Exception {
		RadiomicsJ rj = new RadiomicsJ();// the constructor resets the settings
		RadiomicsJ.force2D = force2D;
		RadiomicsJ.gaborSets = gabor == null ? null : gabor.split(";");
		RadiomicsJ.gaborPooling = pooling;
		RadiomicsJ.gaborDeltaTheta = dTheta;
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
		// anisotropic: sigma and lambda are millimetres
		ImagePlus img = image(3d), msk = mask(3d);
		for (boolean force2D : new boolean[] { false, true }) {
			ResultsTable rt = extract(img, msk, "2:3:1.5:0.4", Pooling.AVERAGE, Math.PI / 4, force2D);
			ImagePlus filtered = ImageFiltering.gaborFilter(img, 2, 3, 1.5, 0.4, Padding.MIRROR, Pooling.AVERAGE, Math.PI / 4, force2D);
			ResultsTable manual = extract(calibrated(filtered, 3d), msk, null, null, 0, force2D);
			assertEquals(manual.size(), rt.size());
			assertTwins(manual, rt, "gabor_s2_l3_g1.5_t0.4_average_dt0.7854_");
		}
	}

	@Test
	public void withoutGaborFiltersTheRowIsWhatItWas() throws Exception {
		ImagePlus img = image(1d), msk = mask(1d);
		ResultsTable plain = extract(img, msk, null, null, 0, false);
		ResultsTable two = extract(img, msk, "2:3:1.5:0;3:4:1:1", null, 0, false);
		for (String h : plain.getHeadings()) {
			assertFalse(h.startsWith("gabor_"), h);
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
		for (String p : new String[] { "gabor_s2_l3_g1.5_t0_", "gabor_s3_l4_g1_t1_" }) {
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
		prop.setProperty("STRING_gaborSets", "2:3:1.5:0 ; 3:4:1:1");
		prop.setProperty("STRING_gaborPadding", "nearest");
		prop.setProperty("STRING_gaborPooling", "max");
		prop.setProperty("DOUBLE_gaborDeltaTheta", "1.5707963267948966");
		rj.loadSettings(prop);
		assertEquals(2, RadiomicsJ.gaborSets.length);
		assertEquals(Padding.NEAREST, RadiomicsJ.gaborPadding);
		assertEquals(Pooling.MAX, RadiomicsJ.gaborPooling);
		assertEquals(Math.PI / 2, RadiomicsJ.gaborDeltaTheta, 0);
		ResultsTable rt = rj.extractAll(img, msk, 1);
		ImagePlus filtered = ImageFiltering.gaborFilter(img, 3, 4, 1, 1, Padding.NEAREST, Pooling.MAX, Math.PI / 2, false);
		RadiomicsJ.resetSettings();
		assertEquals(null, RadiomicsJ.gaborSets);
		ResultsTable manual = extract(calibrated(filtered, 1d), msk, null, null, 0, false);
		assertTwins(manual, rt, "gabor_s3_l4_g1_t1_max_dt1.571_");
	}

	@Test
	public void aWrongSetOrStepIsRefused() {
		ImagePlus img = image(1d), msk = mask(1d);
		assertThrows(IllegalArgumentException.class, () -> extract(img, msk, "2:3:1.5", null, 0, false));
		assertThrows(IllegalArgumentException.class, () -> extract(img, msk, "2:3:1.5", null, 0, true));
		assertThrows(IllegalArgumentException.class, () -> extract(img, msk, "0:3:1.5:0", null, 0, true));
		assertThrows(IllegalArgumentException.class, () -> extract(img, msk, "2:3:1.5:0", Pooling.MAX, 1.0, false));
		assertThrows(IllegalArgumentException.class, () -> extract(img, msk, "2:3:1.5:0", Pooling.MAX, 1.0, true));
	}
}
