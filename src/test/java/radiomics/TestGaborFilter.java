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
 * The Gabor filter against the IBSI 2 consensus response maps of test set 1 (4.a, 4.b).
 * The phantoms have a voxel spacing of 2 mm.
 *
 * @author tatsunidas <t_kobayashi@vis-ionary.com>
 */
public class TestGaborFilter {

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

	private static ImagePlus sphere() {
		return TestDataLoader.loadNifTi(PHANTOM + "sphere/image/sphere.nii.gz");
	}

	private static final double PI = Math.PI;

	// 4.a: sigma 10 mm, lambda 4 mm, gamma 1/2; 4.b: sigma 20 mm, lambda 8 mm, gamma 5/2 (voxels of 2 mm)
	private static ImagePlus a(double theta, Padding padding, Pooling pooling, double dTheta, boolean planar) {
		return ImageFiltering.gaborFilter(impulse(), 10, 4, 0.5, theta, padding, pooling, dTheta, planar);
	}

	private static ImagePlus b(double theta, Padding padding, Pooling pooling, double dTheta, boolean planar) {
		return ImageFiltering.gaborFilter(sphere(), 20, 8, 2.5, theta, padding, pooling, dTheta, planar);
	}

	@Test
	public void ibsi2TestSet1MatchesTheConsensusMaps() {
		assertTrue(againstReference(a(PI / 3, Padding.CONSTANT, null, 0, true), "4_a_1-ValidCRM.nii") <= 0.01);
		assertTrue(againstReference(a(0, Padding.CONSTANT, Pooling.AVERAGE, PI / 4, false), "4_a_2-ValidCRM.nii") <= 0.01);
		assertTrue(againstReference(b(5 * PI / 4, Padding.MIRROR, null, 0, true), "4_b_1-ValidCRM.nii") <= 0.01);
		assertTrue(againstReference(b(0, Padding.MIRROR, Pooling.AVERAGE, PI / 8, false), "4_b_2-ValidCRM.nii") <= 0.01);
	}

	@Test
	public void aWrongSettingIsNotTheReference() {
		// the sign of the orientation
		assertTrue(againstReference(a(-PI / 3, Padding.CONSTANT, null, 0, true), "4_a_1-ValidCRM.nii") > 0.01);
		assertTrue(againstReference(b(-5 * PI / 4, Padding.MIRROR, null, 0, true), "4_b_1-ValidCRM.nii") > 0.01);
		// the axial plane alone is not the average of the three orthogonal planes
		assertTrue(againstReference(b(0, Padding.MIRROR, Pooling.AVERAGE, PI / 8, true), "4_b_2-ValidCRM.nii") > 0.01);
		// padding, pooling and the step
		assertTrue(againstReference(b(5 * PI / 4, Padding.CONSTANT, null, 0, true), "4_b_1-ValidCRM.nii") > 0.01);
		assertTrue(againstReference(b(0, Padding.MIRROR, Pooling.MAX, PI / 8, false), "4_b_2-ValidCRM.nii") > 0.01);
		assertTrue(againstReference(b(0, Padding.MIRROR, Pooling.AVERAGE, PI / 4, false), "4_b_2-ValidCRM.nii") > 0.01);
		// sigma and lambda are millimetres, not voxels
		assertTrue(againstReference(ImageFiltering.gaborFilter(impulse(), 5, 2, 0.5, PI / 3, Padding.CONSTANT, null, 0, true),
				"4_a_1-ValidCRM.nii") > 0.01);
	}

	private static ImagePlus noise(int n) {
		Random rnd = new Random(1);
		ImageStack st = new ImageStack(n, n);
		for (int z = 0; z < 3; z++) {
			float[] p = new float[n * n];
			for (int i = 0; i < p.length; i++) {
				p[i] = rnd.nextFloat() * 100f;
			}
			st.addSlice(new FloatProcessor(n, n, p));
		}
		return new ImagePlus("noise", st);
	}

	/** out(x, y) = in(source(x, y)) on every slice */
	private static ImagePlus moved(ImagePlus in, java.util.function.Function<int[], int[]> source) {
		int n = in.getWidth();
		float[][] v = voxels(in);
		ImageStack st = new ImageStack(n, n);
		for (int z = 0; z < v.length; z++) {
			float[] o = new float[n * n];
			for (int y = 0; y < n; y++) {
				for (int x = 0; x < n; x++) {
					int[] s = source.apply(new int[] { x, y });
					o[y * n + x] = v[z][s[1] * n + s[0]];
				}
			}
			st.addSlice(new FloatProcessor(n, n, o));
		}
		return new ImagePlus("moved", st);
	}

	private static double worst(float[][] a, float[][] b) {
		double worst = 0;
		for (int z = 0; z < a.length; z++) {
			for (int i = 0; i < a[z].length; i++) {
				worst = Math.max(worst, Math.abs(a[z][i] - b[z][i]));
			}
		}
		return worst;
	}

	@Test
	public void aQuarterTurnOfTheImageIsAQuarterTurnOfTheOrientation() {
		int n = 25;
		ImagePlus img = noise(n);
		java.util.function.Function<int[], int[]> turn = p -> new int[] { p[1], n - 1 - p[0] };
		for (Padding padding : new Padding[] { Padding.MIRROR, Padding.PERIODIC, Padding.CONSTANT }) {
			double theta = 0.4;
			float[][] turnedImage = voxels(ImageFiltering.gaborFilter(moved(img, turn), 2, 3, 1.5, theta + PI / 2, padding, null, 0, true));
			float[][] turnedMap = voxels(moved(ImageFiltering.gaborFilter(img, 2, 3, 1.5, theta, padding, null, 0, true), turn));
			assertTrue(worst(turnedImage, turnedMap) < 1e-3, padding + " " + worst(turnedImage, turnedMap));
			// the orientation left as it was is another map
			float[][] same = voxels(ImageFiltering.gaborFilter(moved(img, turn), 2, 3, 1.5, theta, padding, null, 0, true));
			assertTrue(worst(same, turnedMap) > 1, padding + " " + worst(same, turnedMap));
		}
	}

	@Test
	public void aHalfTurnOrAWholeStepLeavesThePooledMapAsItWas() {
		ImagePlus img = noise(25);
		float[][] a = voxels(ImageFiltering.gaborFilter(img, 2, 3, 1.5, 0, Padding.MIRROR, null, 0, true));
		float[][] b = voxels(ImageFiltering.gaborFilter(img, 2, 3, 1.5, PI, Padding.MIRROR, null, 0, true));
		assertTrue(worst(a, b) < 1e-3);
		float[][] p = voxels(ImageFiltering.gaborFilter(img, 2, 3, 1.5, 0, Padding.MIRROR, Pooling.AVERAGE, PI / 4, true));
		float[][] q = voxels(ImageFiltering.gaborFilter(img, 2, 3, 1.5, PI / 4, Padding.MIRROR, Pooling.AVERAGE, PI / 4, true));
		assertTrue(worst(p, q) < 1e-3);
	}

	@Test
	public void settingsAreChecked() {
		ImagePlus img = noise(9);
		assertThrows(IllegalArgumentException.class, () -> ImageFiltering.gaborFilter(img, 0, 3, 1, 0, Padding.MIRROR, null, 0, true));
		assertThrows(IllegalArgumentException.class, () -> ImageFiltering.gaborFilter(img, 2, -3, 1, 0, Padding.MIRROR, null, 0, true));
		assertThrows(IllegalArgumentException.class, () -> ImageFiltering.gaborFilter(img, 2, 3, 0, 0, Padding.MIRROR, null, 0, true));
		// pi must be a whole number of steps
		assertThrows(IllegalArgumentException.class, () -> ImageFiltering.gaborFilter(img, 2, 3, 1, 0, Padding.MIRROR, Pooling.MAX, 1.0, true));
		assertThrows(IllegalArgumentException.class, () -> ImageFiltering.gaborFilter(img, 2, 3, 1, 0, Padding.MIRROR, Pooling.MAX, 0, true));
		assertThrows(IllegalArgumentException.class, () -> ImageFiltering.gaborSet("5:2:1.5"));
		assertThrows(IllegalArgumentException.class, () -> ImageFiltering.gaborSet("5:x:1.5:0"));
		assertThrows(IllegalArgumentException.class, () -> ImageFiltering.gaborSet("-5:2:1.5:0"));
		assertEquals(0.5, ImageFiltering.gaborSet(" 5 : 2 : 0.5 : -1 ")[2], 0);
	}
}
