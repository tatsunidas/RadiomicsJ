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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.ResultsTable;
import ij.process.FloatProcessor;
import io.github.tatsunidas.radiomics.main.RadiomicsJ;

/**
 * extractAllSlice must not hide a slice that failed: a failure of every slice is an
 * exception, a partial failure is reported.
 *
 * @author tatsunidas <t_kobayashi@vis-ionary.com>
 */
public class TestExtractAllSliceFailures {

	private static final int SIZE = 20;

	@AfterEach
	public void reset() {
		RadiomicsJ.resetSettings();
	}

	private static ImagePlus image(double sy) {
		Random rnd = new Random(7);
		ImageStack st = new ImageStack(SIZE, SIZE);
		for (int z = 0; z < SIZE; z++) {
			FloatProcessor ip = new FloatProcessor(SIZE, SIZE);
			for (int i = 0; i < SIZE * SIZE; i++) {
				ip.setf(i, 100f + 50f * rnd.nextFloat());
			}
			st.addSlice(ip);
		}
		return calibrated(new ImagePlus("image", st), sy);
	}

	/** a cube of 10 voxels, so that 10 slices of 20 are blank */
	private static ImagePlus mask(double sy) {
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
		return calibrated(new ImagePlus("mask", st), sy);
	}

	private static ImagePlus calibrated(ImagePlus imp, double sy) {
		imp.getCalibration().pixelWidth = 1d;
		imp.getCalibration().pixelHeight = sy;
		imp.getCalibration().pixelDepth = 1d;
		return imp;
	}

	@Test
	public void aFailureOfEverySliceIsAnException() {
		// the LoG filter refuses a slice whose voxels are not isotropic in plane, on every slice
		ImagePlus img = image(2d), msk = mask(2d);
		RadiomicsJ rj = new RadiomicsJ();
		RadiomicsJ.force2D = true;
		RadiomicsJ.logSigmaMm = new double[] { 1.5 };
		IllegalStateException e = assertThrows(IllegalStateException.class, () -> rj.extractAllSlice(img, msk, 1));
		assertTrue(e.getMessage().contains("every slice"), e.getMessage());
		assertTrue(e.getCause() != null && String.valueOf(e.getCause().getMessage()).contains("LoG"), String.valueOf(e.getCause()));
	}

	@Test
	public void aRunWithoutFailuresReportsNone() {
		ImagePlus img = image(1d), msk = mask(1d);
		RadiomicsJ rj = new RadiomicsJ();
		RadiomicsJ.force2D = true;
		ResultsTable rt = rj.extractAllSlice(img, msk, 1);
		// 10 slices hold the mask, the other 10 are blank and are not failures
		assertEquals(10, rt.size());
		assertTrue(rj.getSkippedSlices().isEmpty());
	}

	/** the mask with the 8th slice (1 based) reduced to the given number of voxels, 0 for blank */
	private static ImagePlus maskWithSlice8(int voxels) {
		ImagePlus msk = mask(1d);
		FloatProcessor mp = (FloatProcessor) msk.getStack().getProcessor(8);
		for (int i = 0; i < SIZE * SIZE; i++) {
			mp.setf(i, 0f);
		}
		for (int i = 0; i < voxels; i++) {
			mp.setf(10 + i, 10, 1f);
		}
		return msk;
	}

	@Test
	public void aSliceThatFailsIsReportedAndTheOthersAreKept() {
		ImagePlus img = image(1d);
		RadiomicsJ rj = new RadiomicsJ();
		RadiomicsJ.force2D = true;
		// a ROI of one voxel has no covariance, so its slice fails for lack of data
		ResultsTable partial = rj.extractAllSlice(img, maskWithSlice8(1), 1);
		assertEquals(java.util.Arrays.asList(8), rj.getSkippedSlices());
		assertEquals(9, partial.size());
		// the other slices are what they are when that slice is simply blank
		ResultsTable blank = rj.extractAllSlice(img, maskWithSlice8(0), 1);
		assertTrue(rj.getSkippedSlices().isEmpty(), "blank is not a failure, and the list is made anew per call");
		assertEquals(9, blank.size());
		String column = "IntensityBasedStatistical_" + io.github.tatsunidas.radiomics.features.IntensityBasedStatisticalFeatureType.findType("Q4LE");
		for (int r = 0; r < 9; r++) {
			assertEquals(blank.getValue(column, r), partial.getValue(column, r), 0d, "row " + r);
		}
	}
}
