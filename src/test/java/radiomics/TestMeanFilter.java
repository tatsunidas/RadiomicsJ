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

import org.junit.jupiter.api.Test;

import ij.ImagePlus;
import io.github.tatsunidas.radiomics.main.ImageFiltering;
import io.github.tatsunidas.radiomics.main.ImageFiltering.Padding;
import io.github.tatsunidas.radiomics.main.TestDataLoader;

/**
 * The mean filter against the IBSI 2 consensus response maps of test set 1.
 *
 * @author tatsunidas <t_kobayashi@vis-ionary.com>
 */
public class TestMeanFilter {

	private static final String PHANTOM = "data_sets-master/ibsi_2_digital_phantom/nifti/";
	private static final String REFERENCE = "data_sets-master/ibsi_2_reference_data/reference_response_maps/";

	private static float[][] voxels(ImagePlus imp) {
		int d = imp.getStackSize();
		float[][] v = new float[d][];
		for (int z = 0; z < d; z++) {
			float[] s = new float[imp.getWidth() * imp.getHeight()];
			for (int i = 0; i < s.length; i++) {
				s[i] = (float) imp.getCalibration().getCValue(imp.getStack().getProcessor(z + 1).getf(i));
			}
			v[z] = s;
		}
		return v;
	}

	/** largest difference to the consensus map as a fraction of its range; compliance is 1% (IBSI 2 section 7.1) */
	private static double againstReference(ImagePlus res, String reference) {
		float[][] r = voxels(res);
		float[][] ref = voxels(TestDataLoader.loadNifTi(REFERENCE + reference));
		assertEquals(ref.length, r.length);
		double lo = Double.MAX_VALUE, hi = -Double.MAX_VALUE, worst = 0;
		for (int z = 0; z < ref.length; z++) {
			for (int i = 0; i < ref[z].length; i++) {
				lo = Math.min(lo, ref[z][i]);
				hi = Math.max(hi, ref[z][i]);
				worst = Math.max(worst, Math.abs(r[z][i] - ref[z][i]));
			}
		}
		double fraction = worst / (hi - lo);
		System.out.println("IBSI 2 " + reference + ": largest difference " + worst + ", " + (100.0 * fraction)
				+ "% of the reference range " + (hi - lo));
		return fraction;
	}

	private static ImagePlus checker() {
		return TestDataLoader.loadNifTi(PHANTOM + "checkerboard/image/checkerboard.nii.gz");
	}

	@Test
	public void ibsi2TestSet1MatchesTheConsensusMaps() {
		ImagePlus checker = checker();
		ImagePlus impulse = TestDataLoader.loadNifTi(PHANTOM + "impulse/image/impulse.nii.gz");
		assertTrue(againstReference(ImageFiltering.meanFilter(checker, 15, Padding.CONSTANT, false), "1_a_1-ValidCRM.nii") <= 0.01);
		assertTrue(againstReference(ImageFiltering.meanFilter(checker, 15, Padding.NEAREST, false), "1_a_2-ValidCRM.nii") <= 0.01);
		assertTrue(againstReference(ImageFiltering.meanFilter(checker, 15, Padding.PERIODIC, false), "1_a_3-ValidCRM.nii") <= 0.01);
		assertTrue(againstReference(ImageFiltering.meanFilter(checker, 15, Padding.MIRROR, false), "1_a_4-ValidCRM.nii") <= 0.01);
		assertTrue(againstReference(ImageFiltering.meanFilter(impulse, 15, Padding.CONSTANT, true), "1_b_1-ValidCRM.nii") <= 0.01);
	}

	@Test
	public void aWrongPaddingOrDimensionIsNotTheReference() {
		ImagePlus checker = checker();
		// the checkerboard reaches the edge, so each padding gives its own map
		assertTrue(againstReference(ImageFiltering.meanFilter(checker, 15, Padding.MIRROR, false), "1_a_1-ValidCRM.nii") > 0.01);
		assertTrue(againstReference(ImageFiltering.meanFilter(checker, 15, Padding.CONSTANT, false), "1_a_4-ValidCRM.nii") > 0.01);
		assertTrue(againstReference(ImageFiltering.meanFilter(checker, 15, Padding.PERIODIC, false), "1_a_2-ValidCRM.nii") > 0.01);
		// and a support of 13 or a planar filter is not the 3D support 15
		assertTrue(againstReference(ImageFiltering.meanFilter(checker, 13, Padding.NEAREST, false), "1_a_2-ValidCRM.nii") > 0.01);
		assertTrue(againstReference(ImageFiltering.meanFilter(checker, 15, Padding.NEAREST, true), "1_a_2-ValidCRM.nii") > 0.01);
	}

	@Test
	public void theSupportMustBeOdd() {
		ImagePlus checker = checker();
		assertThrows(IllegalArgumentException.class, () -> ImageFiltering.meanFilter(checker, 4, Padding.MIRROR, false));
		assertThrows(IllegalArgumentException.class, () -> ImageFiltering.meanFilter(checker, 0, Padding.MIRROR, false));
		assertThrows(IllegalArgumentException.class, () -> ImageFiltering.meanFilter(checker, -3, Padding.MIRROR, false));
		assertEquals(64, ImageFiltering.meanFilter(checker, 3, Padding.MIRROR, false).getStackSize());
	}

	@Test
	public void aSupportOfOneIsTheIdentity() {
		ImagePlus checker = checker();
		float[][] v = voxels(ImageFiltering.meanFilter(checker, 1, Padding.CONSTANT, false));
		float[][] src = voxels(checker);
		for (int z = 0; z < v.length; z++) {
			for (int i = 0; i < v[z].length; i++) {
				assertEquals(src[z][i], v[z][i], 0f);
			}
		}
	}
}
