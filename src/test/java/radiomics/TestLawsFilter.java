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

import org.junit.jupiter.api.Test;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.FloatProcessor;
import io.github.tatsunidas.radiomics.main.ImageFiltering;
import io.github.tatsunidas.radiomics.main.ImageFiltering.Padding;
import io.github.tatsunidas.radiomics.main.ImageFiltering.Pooling;
import io.github.tatsunidas.radiomics.main.TestDataLoader;

/**
 * The Laws kernel filter against the IBSI 2 consensus response maps of test set 1
 * (3.a, 3.b, 3.c).
 *
 * @author tatsunidas <t_kobayashi@vis-ionary.com>
 */
public class TestLawsFilter {

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

	private static ImagePlus impulse() {
		return TestDataLoader.loadNifTi(PHANTOM + "impulse/image/impulse.nii.gz");
	}

	private static ImagePlus checker() {
		return TestDataLoader.loadNifTi(PHANTOM + "checkerboard/image/checkerboard.nii.gz");
	}

	@Test
	public void ibsi2TestSet1MatchesTheConsensusMaps() {
		ImagePlus impulse = impulse();
		ImagePlus checker = checker();
		assertTrue(againstReference(ImageFiltering.lawsFilter(impulse, "E5L5S5", Padding.CONSTANT, null, 0, false), "3_a_1-ValidCRM.nii") <= 0.01);
		assertTrue(againstReference(ImageFiltering.lawsFilter(impulse, "E5L5S5", Padding.CONSTANT, Pooling.MAX, 0, false), "3_a_2-ValidCRM.nii") <= 0.01);
		assertTrue(againstReference(ImageFiltering.lawsFilter(impulse, "E5L5S5", Padding.CONSTANT, Pooling.MAX, 7, false), "3_a_3-ValidCRM.nii") <= 0.01);
		assertTrue(againstReference(ImageFiltering.lawsFilter(checker, "E3W5R5", Padding.MIRROR, null, 0, false), "3_b_1-ValidCRM.nii") <= 0.01);
		assertTrue(againstReference(ImageFiltering.lawsFilter(checker, "E3W5R5", Padding.MIRROR, Pooling.MAX, 0, false), "3_b_2-ValidCRM.nii") <= 0.01);
		assertTrue(againstReference(ImageFiltering.lawsFilter(checker, "E3W5R5", Padding.MIRROR, Pooling.MAX, 7, false), "3_b_3-ValidCRM.nii") <= 0.01);
		assertTrue(againstReference(ImageFiltering.lawsFilter(checker, "L5S5", Padding.MIRROR, null, 0, true), "3_c_1-ValidCRM.nii") <= 0.01);
		assertTrue(againstReference(ImageFiltering.lawsFilter(checker, "L5S5", Padding.MIRROR, Pooling.MAX, 0, true), "3_c_2-ValidCRM.nii") <= 0.01);
		assertTrue(againstReference(ImageFiltering.lawsFilter(checker, "L5S5", Padding.MIRROR, Pooling.MAX, 7, true), "3_c_3-ValidCRM.nii") <= 0.01);
	}

	@Test
	public void aWrongSettingIsNotTheReference() {
		ImagePlus impulse = impulse();
		ImagePlus checker = checker();
		// the kernels are not symmetric, so the order and the direction matter
		assertTrue(againstReference(ImageFiltering.lawsFilter(impulse, "S5L5E5", Padding.CONSTANT, null, 0, false), "3_a_1-ValidCRM.nii") > 0.01);
		assertTrue(againstReference(ImageFiltering.lawsFilter(impulse, "E5S5L5", Padding.CONSTANT, null, 0, false), "3_a_1-ValidCRM.nii") > 0.01);
		// no pooling, another pooling, no energy map, another padding
		assertTrue(againstReference(ImageFiltering.lawsFilter(impulse, "E5L5S5", Padding.CONSTANT, null, 0, false), "3_a_2-ValidCRM.nii") > 0.01);
		assertTrue(againstReference(ImageFiltering.lawsFilter(impulse, "E5L5S5", Padding.CONSTANT, Pooling.AVERAGE, 0, false), "3_a_2-ValidCRM.nii") > 0.01);
		assertTrue(againstReference(ImageFiltering.lawsFilter(impulse, "E5L5S5", Padding.CONSTANT, Pooling.MAX, 0, false), "3_a_3-ValidCRM.nii") > 0.01);
		assertTrue(againstReference(ImageFiltering.lawsFilter(checker, "E3W5R5", Padding.CONSTANT, null, 0, false), "3_b_1-ValidCRM.nii") > 0.01);
		// the planar filter is not the volumetric one with a 3rd kernel left out
		assertTrue(againstReference(ImageFiltering.lawsFilter(checker, "L5S5", Padding.MIRROR, null, 0, true), "3_c_2-ValidCRM.nii") > 0.01);
	}

	private static ImagePlus noise(int n) {
		Random rnd = new Random(1);
		ImageStack st = new ImageStack(n, n);
		for (int z = 0; z < n; z++) {
			float[] p = new float[n * n];
			for (int i = 0; i < p.length; i++) {
				p[i] = rnd.nextFloat() * 100f;
			}
			st.addSlice(new FloatProcessor(n, n, p));
		}
		return new ImagePlus("noise", st);
	}

	/** out(x, y, z) = in(source(x, y, z)) with source returning the flat index of the cube */
	private static ImagePlus moved(ImagePlus in, int n, java.util.function.Function<int[], int[]> source) {
		float[][] v = voxels(in);
		ImageStack st = new ImageStack(n, n);
		float[][] o = new float[n][n * n];
		for (int z = 0; z < n; z++) {
			for (int y = 0; y < n; y++) {
				for (int x = 0; x < n; x++) {
					int[] s = source.apply(new int[] { x, y, z });
					o[z][y * n + x] = v[s[2]][s[1] * n + s[0]];
				}
			}
			st.addSlice(new FloatProcessor(n, n, o[z]));
		}
		return new ImagePlus("moved", st);
	}

	@Test
	public void maxPoolingOverRightAnglesIsEquivariantToRightAngleRotations() {
		int n = 12;
		ImagePlus img = noise(n);
		// quarter turns about z and about x, as index maps (bijections of the cube)
		java.util.List<java.util.function.Function<int[], int[]>> turns = java.util.Arrays.asList(
				p -> new int[] { p[1], n - 1 - p[0], p[2] },
				p -> new int[] { p[0], p[2], n - 1 - p[1] },
				p -> new int[] { n - 1 - p[2], p[1], p[0] });
		for (Padding padding : new Padding[] { Padding.MIRROR, Padding.PERIODIC, Padding.CONSTANT }) {
			for (java.util.function.Function<int[], int[]> turn : turns) {
				// the filter of the turned image equals the turned filter of the image
				float[][] a = voxels(ImageFiltering.lawsFilter(moved(img, n, turn), "E5L5S5", padding, Pooling.MAX, 0, false));
				float[][] b = voxels(moved(ImageFiltering.lawsFilter(img, "E5L5S5", padding, Pooling.MAX, 0, false), n, turn));
				double worst = 0;
				for (int z = 0; z < n; z++) {
					for (int i = 0; i < a[z].length; i++) {
						worst = Math.max(worst, Math.abs(a[z][i] - b[z][i]));
					}
				}
				assertTrue(worst < 1e-3, padding + " worst " + worst);
			}
		}
		// and without pooling the response of the turned image is not the turned response
		float[][] a = voxels(ImageFiltering.lawsFilter(moved(img, n, turns.get(0)), "E5L5S5", Padding.MIRROR, null, 0, false));
		float[][] b = voxels(moved(ImageFiltering.lawsFilter(img, "E5L5S5", Padding.MIRROR, null, 0, false), n, turns.get(0)));
		double worst = 0;
		for (int z = 0; z < n; z++) {
			for (int i = 0; i < a[z].length; i++) {
				worst = Math.max(worst, Math.abs(a[z][i] - b[z][i]));
			}
		}
		assertTrue(worst > 1, "worst " + worst);
	}

	@Test
	public void kernelsAreChecked() {
		ImagePlus checker = checker();
		assertThrows(IllegalArgumentException.class, () -> ImageFiltering.lawsFilter(checker, "E5L5", Padding.MIRROR, null, 0, false));
		assertThrows(IllegalArgumentException.class, () -> ImageFiltering.lawsFilter(checker, "E5L5S5", Padding.MIRROR, null, 0, true));
		assertThrows(IllegalArgumentException.class, () -> ImageFiltering.lawsFilter(checker, "E5L5X5", Padding.MIRROR, null, 0, false));
		// separators are free
		float[][] a = voxels(ImageFiltering.lawsFilter(checker, "E5L5S5", Padding.MIRROR, null, 0, false));
		float[][] b = voxels(ImageFiltering.lawsFilter(checker, "E5, L5 S5", Padding.MIRROR, null, 0, false));
		assertEquals(a[10][100], b[10][100], 0f);
	}
}
