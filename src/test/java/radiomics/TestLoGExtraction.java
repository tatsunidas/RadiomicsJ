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
import io.github.tatsunidas.radiomics.main.RadiomicsJ;

/**
 * Laplacian of Gaussian scales in the extraction settings: the features of the
 * filtered image join the row of the original.
 *
 * @author tatsunidas <t_kobayashi@vis-ionary.com>
 */
public class TestLoGExtraction {

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

	private static ResultsTable extract(ImagePlus img, ImagePlus msk, double[] sigmas, boolean force2D) throws Exception {
		RadiomicsJ rj = new RadiomicsJ();// the constructor resets the settings
		RadiomicsJ.force2D = force2D;
		RadiomicsJ.logSigmaMm = sigmas;
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

	@Test
	public void filteredFeaturesEqualTheFeaturesOfTheFilteredImage3D() throws Exception {
		ImagePlus img = image(1d), msk = mask(1d);
		ResultsTable rt = extract(img, msk, new double[] { 2d }, false);
		ImagePlus filtered = ImageFiltering.laplacianOfGaussian(img, 2d, 4d, Padding.MIRROR, false);
		ResultsTable manual = extract(calibrated(filtered, 1d), msk, null, false);
		assertEquals(1, rt.size());
		assertTwins(manual, rt, "log_sigma2.0mm_");
	}

	@Test
	public void filteredFeaturesEqualTheFeaturesOfTheFilteredImage2D() throws Exception {
		ImagePlus img = image(3d), msk = mask(3d);// slice spacing differs, a planar filter does not mind
		ResultsTable rt = extract(img, msk, new double[] { 2d }, true);
		ImagePlus filtered = ImageFiltering.laplacianOfGaussian(img, 2d, 4d, Padding.MIRROR, true);
		ResultsTable manual = extract(calibrated(filtered, 3d), msk, null, true);
		assertEquals(manual.size(), rt.size());
		assertTwins(manual, rt, "log_sigma2.0mm_");
	}

	@Test
	public void withoutScalesTheRowIsWhatItWas() throws Exception {
		ImagePlus img = image(1d), msk = mask(1d);
		ResultsTable plain = extract(img, msk, null, false);
		ResultsTable two = extract(img, msk, new double[] { 1.5d, 3d }, false);
		for (String h : plain.getHeadings()) {
			assertFalse(h.startsWith("log_"), h);
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
		for (String p : new String[] { "log_sigma1.5mm_", "log_sigma3.0mm_" }) {
			assertTrue(two.columnExists(two.getColumnIndex(p + "IntensityHistogram_" + firstHistogram(plain))), p);
			for (String h : two.getHeadings()) {
				if (h.startsWith(p)) {
					assertFalse(originalOnly(h.substring(p.length())), h);
				}
			}
		}
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
	public void anAnisotropicGridIsRefusedWithAReason() {
		ImagePlus img = image(3d), msk = mask(3d);
		Exception e = assertThrows(IllegalArgumentException.class, () -> extract(img, msk, new double[] { 2d }, false));
		assertTrue(e.getMessage().contains("resamplingFactorXYZ"), e.getMessage());
		// and the same grid is fine without a scale
		assertEquals(1, assertDoesNotThrowRow(img, msk));
	}

	private static int assertDoesNotThrowRow(ImagePlus img, ImagePlus msk) {
		try {
			return extract(img, msk, null, false).size();
		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}

	@Test
	public void aScaleThatIsNotPositiveIsRefused() {
		ImagePlus img = image(1d), msk = mask(1d);
		assertThrows(IllegalArgumentException.class, () -> extract(img, msk, new double[] { 0d }, false));
	}

	@Test
	public void theScalesComeFromTheSettingsFile() throws Exception {
		ImagePlus img = image(1d), msk = mask(1d);
		RadiomicsJ rj = new RadiomicsJ();
		Properties prop = new Properties();
		prop.setProperty("DOUBLEARRAY_logSigmaMm", "1.5,3.0");
		prop.setProperty("DOUBLE_logTruncate", "3");
		prop.setProperty("STRING_logPadding", "nearest");
		rj.loadSettings(prop);
		assertEquals(2, RadiomicsJ.logSigmaMm.length);
		assertEquals(3d, RadiomicsJ.logTruncate);
		assertEquals(Padding.NEAREST, RadiomicsJ.logPadding);
		ResultsTable rt = rj.extractAll(img, msk, 1);
		ImagePlus filtered = ImageFiltering.laplacianOfGaussian(img, 3d, 3d, Padding.NEAREST, false);
		RadiomicsJ.resetSettings();
		assertEquals(null, RadiomicsJ.logSigmaMm);
		ResultsTable manual = extract(calibrated(filtered, 1d), msk, null, false);
		assertTwins(manual, rt, "log_sigma3.0mm_");
	}

	@Test
	public void meanFilterColumnsEqualTheFeaturesOfTheFilteredImage() throws Exception {
		ImagePlus img = image(3d), msk = mask(3d);// the support is in voxels, so the spacing may differ
		for (boolean force2D : new boolean[] { false, true }) {
			RadiomicsJ rj = new RadiomicsJ();
			Properties prop = new Properties();
			prop.setProperty("INTARRAY_meanSupport", "3,5");
			prop.setProperty("STRING_meanPadding", "periodic");
			rj.loadSettings(prop);
			RadiomicsJ.force2D = force2D;
			ResultsTable rt = force2D ? rj.extractAllSlice(img, msk, 1) : rj.extractAll(img, msk, 1);
			assertEquals(2, RadiomicsJ.meanSupport.length);
			ImagePlus filtered = ImageFiltering.meanFilter(img, 5, Padding.PERIODIC, force2D);
			ResultsTable manual = extract(calibrated(filtered, 3d), msk, null, force2D);
			assertTwins(manual, rt, "mean_M5_");
			assertTrue(rt.columnExists(rt.getColumnIndex("mean_M3_" + "IntensityHistogram_" + firstHistogram(manual))));
		}
	}

	@Test
	public void anEvenMeanSupportIsRefused() {
		ImagePlus img = image(1d), msk = mask(1d);
		assertThrows(IllegalArgumentException.class, () -> {
			RadiomicsJ rj = new RadiomicsJ();
			RadiomicsJ.meanSupport = new int[] { 4 };
			rj.extractAll(img, msk, 1);
		});
	}
}
