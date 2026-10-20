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
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.ResultsTable;
import ij.process.FloatProcessor;
import io.github.tatsunidas.radiomics.main.RadiomicsJ;

/**
 * Re-segmentation narrows the intensity mask only, so the diagnostics of the
 * re-segmented morphological mask describe the mask the morphological features
 * are computed on.
 *
 * @author tatsunidas <t_kobayashi@vis-ionary.com>
 */
public class TestDiagnosticsMorphMask {

	private static final int SIZE = 10;
	/** roi from 1 to 8 on every axis: 8 x 8 x 8 = 512 voxels */
	private static final int LO = 1, HI = 8;

	public static void main(String[] args) throws Exception {
		print("with a range filter", extract(true));
		print("without", extract(false));
	}

	private static void print(String title, ResultsTable rt) {
		System.out.println(title);
		for (String c : new String[] { "InterpolatedROIMorphMaskVoxelCount", "ResegmentedROIIntMaskVoxelCount",
				"ResegmentedROIMorphMaskVoxelCount", "ResegmentedROIIntMaskBoundingBoxDimensionZ",
				"ResegmentedROIMorphMaskBoundingBoxDimensionZ" }) {
			System.out.println("  " + c + " = " + diag(rt, c));
		}
	}

	/**
	 * A flat image of 100 with the top layer of the roi at 1000, so a range of
	 * [0, 500] takes that layer out of the intensity mask.
	 */
	private static ResultsTable extract(boolean rangeFilter) throws Exception {
		ImageStack img = new ImageStack(SIZE, SIZE);
		ImageStack msk = new ImageStack(SIZE, SIZE);
		for (int z = 0; z < SIZE; z++) {
			FloatProcessor ip = new FloatProcessor(SIZE, SIZE);
			FloatProcessor mp = new FloatProcessor(SIZE, SIZE);
			for (int y = 0; y < SIZE; y++) {
				for (int x = 0; x < SIZE; x++) {
					ip.setf(x, y, z == HI ? 1000f : 100f);
					boolean in = x >= LO && x <= HI && y >= LO && y <= HI && z >= LO && z <= HI;
					mp.setf(x, y, in ? 1f : 0f);
				}
			}
			img.addSlice(ip);
			msk.addSlice(mp);
		}
		ImagePlus image = calibrated(new ImagePlus("image", img));
		ImagePlus mask = calibrated(new ImagePlus("mask", msk));
		RadiomicsJ.resetSettings();
		try {
			// the constructor resets the settings, so the range goes in after it
			RadiomicsJ rj = new RadiomicsJ();
			if (rangeFilter) {
				RadiomicsJ.rangeMin = 0d;
				RadiomicsJ.rangeMax = 500d;
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

	private static double diag(ResultsTable rt, String name) {
		return rt.getValue("Diagnostics_Diagnostics" + name, 0);
	}

	@Test
	public void theReSegmentedMorphologicalMaskIsTheInterpolatedOne() throws Exception {
		ResultsTable rt = extract(true);
		// the range filter took the top layer out of the intensity mask
		assertEquals(448d, diag(rt, "ResegmentedROIIntMaskVoxelCount"));
		assertEquals(7d, diag(rt, "ResegmentedROIIntMaskBoundingBoxDimensionZ"));
		// while the morphological mask keeps it
		assertEquals(512d, diag(rt, "InterpolatedROIMorphMaskVoxelCount"));
		assertEquals(512d, diag(rt, "ResegmentedROIMorphMaskVoxelCount"));
		assertNotEquals(diag(rt, "ResegmentedROIIntMaskVoxelCount"), diag(rt, "ResegmentedROIMorphMaskVoxelCount"));
		assertEquals(8d, diag(rt, "ResegmentedROIMorphMaskBoundingBoxDimensionX"));
		assertEquals(8d, diag(rt, "ResegmentedROIMorphMaskBoundingBoxDimensionY"));
		assertEquals(8d, diag(rt, "ResegmentedROIMorphMaskBoundingBoxDimensionZ"));
		// and it is the mask the morphological features measured: 512 voxels of 1 mm3
		assertEquals(512d, rt.getValue("Morphology_VolumeVoxelCounting", 0), 1e-9);
	}

	@Test
	public void withoutReSegmentationTheMasksAgree() throws Exception {
		ResultsTable rt = extract(false);
		assertEquals(512d, diag(rt, "ResegmentedROIIntMaskVoxelCount"));
		assertEquals(512d, diag(rt, "ResegmentedROIMorphMaskVoxelCount"));
		assertEquals(8d, diag(rt, "ResegmentedROIIntMaskBoundingBoxDimensionZ"));
		assertEquals(8d, diag(rt, "ResegmentedROIMorphMaskBoundingBoxDimensionZ"));
	}
}
