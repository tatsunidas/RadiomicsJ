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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Properties;

import org.junit.jupiter.api.Test;

import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.ResultsTable;
import ij.process.FloatProcessor;
import io.github.tatsunidas.radiomics.main.RadiomicsJ;

/**
 * The auxiliary statistics family: off by default, mean of the voxels with value > 0 (zero is not positive).
 *
 * @author tatsunidas <t_kobayashi@vis-ionary.com>
 */
public class TestAuxiliaryStatistics {

	private static final int SIZE = 10;
	private static final String MPP = "AuxiliaryStatistics_MeanOfPositiveIntensities";

	/** 1000 voxels, all in the roi: +2.0 x 500 (z 0-4), 0.0 x 200 (z 5-6), -1.0 x 300 (z 7-9). */
	private static ResultsTable extract(boolean enabled, float positive, float zero, float negative) throws Exception {
		float[] layers = new float[SIZE];
		for (int z = 0; z < SIZE; z++) {
			layers[z] = z < 5 ? positive : (z < 7 ? zero : negative);
		}
		return extractLayers(enabled, layers, null);
	}

	/** every voxel of a layer has the layer value; the whole image is the roi */
	private static ResultsTable extractLayers(boolean enabled, float[] layers, Properties extra) throws Exception {
		ImageStack img = new ImageStack(SIZE, SIZE);
		ImageStack msk = new ImageStack(SIZE, SIZE);
		for (int z = 0; z < SIZE; z++) {
			FloatProcessor ip = new FloatProcessor(SIZE, SIZE);
			FloatProcessor mp = new FloatProcessor(SIZE, SIZE);
			float v = layers[z];
			for (int y = 0; y < SIZE; y++) {
				for (int x = 0; x < SIZE; x++) {
					ip.setf(x, y, v);
					mp.setf(x, y, 1f);
				}
			}
			img.addSlice(ip);
			msk.addSlice(mp);
		}
		ImagePlus image = calibrated(new ImagePlus("image", img));
		ImagePlus mask = calibrated(new ImagePlus("mask", msk));
		RadiomicsJ.resetSettings();
		try {
			RadiomicsJ rj = new RadiomicsJ();
			Properties p = new Properties();
			if (extra != null) {
				p.putAll(extra);
			}
			if (enabled) {
				p.setProperty("BOOL_enableAuxiliaryStatistics", "1");
			}
			if (!p.isEmpty()) {
				rj.loadSettings(p);
			}
			return rj.extractAll(image, mask, 1);
		} finally {
			RadiomicsJ.resetSettings();
		}
	}

	private static ImagePlus calibrated(ImagePlus imp) {
		imp.getCalibration().pixelWidth = 1d;
		imp.getCalibration().pixelHeight = 1d;
		imp.getCalibration().pixelDepth = 1d;
		return imp;
	}

	private static boolean hasColumn(ResultsTable rt, String name) {
		return rt.getColumnIndex(name) != ResultsTable.COLUMN_NOT_FOUND;
	}

	@Test
	public void offByDefault() throws Exception {
		assertFalse(hasColumn(extract(false, 2f, 0f, -1f), MPP));
	}

	@Test
	public void zeroIsNotPositive() throws Exception {
		ResultsTable rt = extract(true, 2f, 0f, -1f);
		// (500 x 2.0) / 500. Counting zero as positive would give 1000 / 700 = 1.428...
		assertEquals(2.0d, rt.getValue(MPP, 0), 0d);
	}

	@Test
	public void noPositiveVoxelIsNaN() throws Exception {
		ResultsTable rt = extract(true, -3f, 0f, -1f);
		assertTrue(hasColumn(rt, MPP));
		assertTrue(Double.isNaN(rt.getValue(MPP, 0)));
	}

	private static final String PPP = "AuxiliaryStatistics_ProportionOfPositiveIntensities";
	private static final String NSD = "AuxiliaryStatistics_NormalisedStandardDeviation";
	private static final String RK = "AuxiliaryStatistics_ReciprocalKurtosis";
	private static final String UPP = "AuxiliaryStatistics_UniformityOfPositiveIntensities";

	private static final double REL = 1e-12;

	private static void assertRel(double expected, double actual, String what) {
		assertEquals(expected, actual, Math.abs(expected) * REL, what);
	}

	@Test
	public void secondaryStatisticsOnThePhantom() throws Exception {
		ResultsTable rt = extract(true, 2f, 0f, -1f);
		// 500 x 2.0, 200 x 0.0, 300 x -1.0: mean 0.7, population variance 1.81
		assertRel(0.5d, rt.getValue(PPP, 0), "PPP");// counting zero as positive would give 0.7
		// population SD (a sample SD, divided by n-1, would differ)
		assertRel(Math.log(Math.sqrt(1.81)) / Math.log(1000d), rt.getValue(NSD, 0), "nSD");
		// m4 = (500 x 1.3^4 + 200 x 0.7^4 + 300 x 1.7^4) / 1000; 1/(excess kurtosis + 3) = m2^2 / m4
		double m4 = (500 * Math.pow(1.3, 4) + 200 * Math.pow(0.7, 4) + 300 * Math.pow(1.7, 4)) / 1000d;
		assertRel(1.81 * 1.81 / m4, rt.getValue(RK, 0), "1/(kurtosis+3)");// omitting the +3 would differ
		assertRel(1d, rt.getValue(UPP, 0), "UPP");// one positive value, one bin
	}

	@Test
	public void uniformityOfPositivesUsesTheBinsOfTheWholeRoi() throws Exception {
		// bin width 2. 400 x -8.0, 300 x 1.0, 300 x 2.5.
		// Bins of the whole roi (from -8): 1.0 -> bin 5, 2.5 -> bin 6, so p = 0.5 each and UPP = 0.5.
		// Bins made again from the positives only (from 1.0): both in bin 1, UPP would be 1.0.
		float[] layers = { -8f, -8f, -8f, -8f, 1f, 1f, 1f, 2.5f, 2.5f, 2.5f };
		Properties p = new Properties();
		p.setProperty("BOOL_USE_FixedBinNumber", "0");
		p.setProperty("DOUBLE_binWidth", "2");
		ResultsTable rt = extractLayers(true, layers, p);
		assertRel(0.5d, rt.getValue(UPP, 0), "UPP");
	}

	@Test
	public void undefinedIsNaNNotZero() throws Exception {
		// every voxel the same value: SD = 0 and the kurtosis is 0/0
		ResultsTable same = extract(true, 3f, 3f, 3f);
		assertTrue(Double.isNaN(same.getValue(NSD, 0)));
		assertTrue(Double.isNaN(same.getValue(RK, 0)));
		assertRel(1d, same.getValue(PPP, 0), "PPP");
		// no positive voxel: PPP is a defined 0, UPP is undefined
		ResultsTable none = extract(true, -3f, -2f, -1f);
		assertEquals(0d, none.getValue(PPP, 0), 0d);
		assertTrue(Double.isNaN(none.getValue(UPP, 0)));
		assertTrue(Double.isNaN(none.getValue(MPP, 0)));
	}

	@Test
	public void oneVoxelHasNoNormalisedStandardDeviation() throws Exception {
		ImageStack st = new ImageStack(1, 1);
		FloatProcessor fp = new FloatProcessor(1, 1);
		fp.setf(0, 0, 5f);
		st.addSlice(fp);
		ImagePlus imp = calibrated(new ImagePlus("one", st));
		RadiomicsJ.resetSettings();
		try {
			io.github.tatsunidas.radiomics.features.AuxiliaryStatisticsFeatures f = new io.github.tatsunidas.radiomics.features.AuxiliaryStatisticsFeatures(
					imp, null, 1, true, 32, 25d);
			assertTrue(Double.isNaN(f.getNormalisedStandardDeviation()));
			assertEquals(1d, f.getProportionOfPositiveIntensities(), 0d);
		} finally {
			RadiomicsJ.resetSettings();
		}
	}

	@Test
	public void otherColumnsAreBitIdenticalWithAndWithout() throws Exception {
		ResultsTable off = extract(false, 2f, 0f, -1f);
		ResultsTable on = extract(true, 2f, 0f, -1f);
		for (String h : off.getHeadings()) {
			if (h.startsWith("Operational") || h.equals(" ") || !hasColumn(off, h)) {
				continue;
			}
			assertTrue(hasColumn(on, h), h);
			double a = off.getValue(h, 0), b = on.getValue(h, 0);
			assertEquals(Double.doubleToLongBits(a), Double.doubleToLongBits(b), h);
		}
	}
}
