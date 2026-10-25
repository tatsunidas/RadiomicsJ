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
		ImageStack img = new ImageStack(SIZE, SIZE);
		ImageStack msk = new ImageStack(SIZE, SIZE);
		for (int z = 0; z < SIZE; z++) {
			FloatProcessor ip = new FloatProcessor(SIZE, SIZE);
			FloatProcessor mp = new FloatProcessor(SIZE, SIZE);
			float v = z < 5 ? positive : (z < 7 ? zero : negative);
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
			if (enabled) {
				Properties p = new Properties();
				p.setProperty("BOOL_enableAuxiliaryStatistics", "1");
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
