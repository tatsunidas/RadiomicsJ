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

import java.util.Properties;

import org.junit.jupiter.api.Test;

import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.ResultsTable;
import ij.process.FloatProcessor;
import io.github.tatsunidas.radiomics.main.RadiomicsJ;

/**
 * The re-segmentation range is a closed interval and either bound can be given alone.
 * Before, a range with only one bound was ignored without a message.
 *
 * @author tatsunidas <t_kobayashi@vis-ionary.com>
 */
public class TestRangeOneSided {

	private static final int SIZE = 10;
	private static final String COUNT = "Diagnostics_DiagnosticsResegmentedROIIntMaskVoxelCount";
	private static final String MEAN = "IntensityBasedStatistical_Mean";

	/** 1000 voxels, all in the roi; the layer z has the value z + 1 (100 voxels each). */
	private static ResultsTable extract(String rangeMin, String rangeMax) throws Exception {
		ImageStack img = new ImageStack(SIZE, SIZE);
		ImageStack msk = new ImageStack(SIZE, SIZE);
		for (int z = 0; z < SIZE; z++) {
			FloatProcessor ip = new FloatProcessor(SIZE, SIZE);
			FloatProcessor mp = new FloatProcessor(SIZE, SIZE);
			for (int y = 0; y < SIZE; y++) {
				for (int x = 0; x < SIZE; x++) {
					ip.setf(x, y, z + 1f);
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
			if (rangeMin != null) {
				p.setProperty("DOUBLE_rangeMin", rangeMin);
			}
			if (rangeMax != null) {
				p.setProperty("DOUBLE_rangeMax", rangeMax);
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

	@Test
	public void noRangeKeepsEveryVoxel() throws Exception {
		assertEquals(1000d, extract(null, null).getValue(COUNT, 0));
	}

	@Test
	public void lowerBoundAloneExcludesBelowAndKeepsTheBoundary() throws Exception {
		ResultsTable rt = extract("4", null);
		assertEquals(700d, rt.getValue(COUNT, 0));// values 4..10 (4 is kept: closed interval)
		assertEquals(7d, rt.getValue(MEAN, 0), 1e-12);// (4 + ... + 10) / 7
	}

	@Test
	public void upperBoundAloneExcludesAboveAndKeepsTheBoundary() throws Exception {
		ResultsTable rt = extract(null, "7");
		assertEquals(700d, rt.getValue(COUNT, 0));// values 1..7
		assertEquals(4d, rt.getValue(MEAN, 0), 1e-12);
	}

	@Test
	public void bothBoundsAreUnchanged() throws Exception {
		ResultsTable rt = extract("4", "7");
		assertEquals(400d, rt.getValue(COUNT, 0));
		assertEquals(5.5d, rt.getValue(MEAN, 0), 1e-12);
	}
}
