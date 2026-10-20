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
 * The separable undecimated wavelet filter against the IBSI 2 consensus response maps of
 * test set 1 (5.a, 6.a, 7.a).
 *
 * @author tatsunidas <t_kobayashi@vis-ionary.com>
 */
public class TestWaveletFilter {

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

	private static ImagePlus checker() {
		return TestDataLoader.loadNifTi(PHANTOM + "checkerboard/image/checkerboard.nii.gz");
	}

	private static ImagePlus w(ImagePlus img, String name, String letters, int level, Padding padding, Pooling pooling) {
		return ImageFiltering.waveletFilter(img, name, letters, level, padding, pooling, false);
	}

	@Test
	public void ibsi2TestSet1MatchesTheConsensusMaps() {
		ImagePlus impulse = impulse(), sphere = sphere(), checker = checker();
		assertTrue(againstReference(w(impulse, "db2", "LHL", 1, Padding.CONSTANT, null), "5_a_1-ValidCRM.nii") <= 0.01);
		assertTrue(againstReference(w(impulse, "db2", "LHL", 1, Padding.CONSTANT, Pooling.AVERAGE), "5_a_2-ValidCRM.nii") <= 0.01);
		assertTrue(againstReference(w(sphere, "coif1", "HHL", 1, Padding.PERIODIC, null), "6_a_1-ValidCRM.nii") <= 0.01);
		assertTrue(againstReference(w(sphere, "coif1", "HHL", 1, Padding.PERIODIC, Pooling.AVERAGE), "6_a_2-ValidCRM.nii") <= 0.01);
		assertTrue(againstReference(w(checker, "haar", "LLL", 2, Padding.MIRROR, Pooling.AVERAGE), "7_a_1-ValidCRM.nii") <= 0.01);
		assertTrue(againstReference(w(checker, "haar", "HHH", 2, Padding.MIRROR, Pooling.AVERAGE), "7_a_2-ValidCRM.nii") <= 0.01);
	}

	@Test
	public void aWrongSettingIsNotTheReference() {
		ImagePlus impulse = impulse(), sphere = sphere(), checker = checker();
		// the order of the letters, the wavelet, the pooling
		assertTrue(againstReference(w(impulse, "db2", "HLL", 1, Padding.CONSTANT, null), "5_a_1-ValidCRM.nii") > 0.01);
		assertTrue(againstReference(w(impulse, "db3", "LHL", 1, Padding.CONSTANT, null), "5_a_1-ValidCRM.nii") > 0.01);
		assertTrue(againstReference(w(impulse, "db2", "LHL", 1, Padding.CONSTANT, null), "5_a_2-ValidCRM.nii") > 0.01);
		assertTrue(againstReference(w(impulse, "db2", "LHL", 1, Padding.CONSTANT, Pooling.MAX), "5_a_2-ValidCRM.nii") > 0.01);
		// padding (the sphere is clear of the edge, so only the checkerboard shows it)
		assertTrue(againstReference(w(checker, "haar", "LLL", 2, Padding.CONSTANT, Pooling.AVERAGE), "7_a_1-ValidCRM.nii") > 0.01);
		assertTrue(againstReference(w(checker, "haar", "HHH", 2, Padding.PERIODIC, Pooling.AVERAGE), "7_a_2-ValidCRM.nii") > 0.01);
		// the wavelet
		assertTrue(againstReference(w(sphere, "coif2", "HHL", 1, Padding.PERIODIC, null), "6_a_1-ValidCRM.nii") > 0.01);
		// the level
		assertTrue(againstReference(w(checker, "haar", "LLL", 1, Padding.MIRROR, Pooling.AVERAGE), "7_a_1-ValidCRM.nii") > 0.01);
		assertTrue(againstReference(w(checker, "haar", "HHH", 1, Padding.MIRROR, Pooling.AVERAGE), "7_a_2-ValidCRM.nii") > 0.01);
		// the planar filter is not the volumetric one
		assertTrue(againstReference(ImageFiltering.waveletFilter(impulse, "db2", "LH", 1, Padding.CONSTANT, null, true),
				"5_a_1-ValidCRM.nii") > 0.01);
	}

	/**
	 * The kernels of an orthonormal wavelet: the low-pass sums to sqrt(2) and has unit energy,
	 * the high-pass sums to zero. Seen through the response of a unit impulse in 3D, whose sum
	 * and energy are the products over the axes.
	 */
	@Test
	public void theKernelsAreOrthonormalWavelets() {
		ImagePlus delta = unit(65);
		for (String name : new String[] { "haar", "db2", "db3", "db4", "coif1", "coif2", "coif3" }) {
			for (int level = 1; level <= 2; level++) {
				double[] lll = sumAndEnergy(w(delta, name, "LLL", level, Padding.CONSTANT, null));
				double[] hll = sumAndEnergy(w(delta, name, "HLL", level, Padding.CONSTANT, null));
				double[] hhh = sumAndEnergy(w(delta, name, "HHH", level, Padding.CONSTANT, null));
				// every iteration filters all axes with a low-pass first, the last one with its letters
				double lowSum = Math.pow(Math.sqrt(2), 3 * level);
				assertEquals(lowSum, lll[0], 1e-3 * lowSum, name + " " + level);
				assertEquals(0d, hll[0], 1e-4, name + " " + level);
				assertEquals(0d, hhh[0], 1e-4, name + " " + level);
				assertEquals(1d, lll[1], 1e-4, name + " " + level);
				assertEquals(1d, hll[1], 1e-4, name + " " + level);
				assertEquals(1d, hhh[1], 1e-4, name + " " + level);
			}
		}
	}

	private static double[] sumAndEnergy(ImagePlus res) {
		double sum = 0, energy = 0;
		for (float[] s : voxels(res)) {
			for (float v : s) {
				sum += v;
				energy += (double) v * v;
			}
		}
		return new double[] { sum, energy };
	}

	private static ImagePlus unit(int n) {
		ImageStack st = new ImageStack(n, n);
		for (int z = 0; z < n; z++) {
			float[] p = new float[n * n];
			if (z == n / 2) {
				p[(n / 2) * n + n / 2] = 1f;
			}
			st.addSlice(new FloatProcessor(n, n, p));
		}
		return new ImagePlus("unit", st);
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
	public void averagePoolingIsEquivariantToRightAngleRotations() {
		int n = 12;
		ImagePlus img = noise(n);
		java.util.List<java.util.function.Function<int[], int[]>> turns = java.util.Arrays.asList(
				p -> new int[] { p[1], n - 1 - p[0], p[2] },
				p -> new int[] { p[0], p[2], n - 1 - p[1] },
				p -> new int[] { n - 1 - p[2], p[1], p[0] });
		String[][] filters = { { "db3", "LHL", "1" }, { "haar", "HHH", "2" } };
		for (String[] f : filters) {
			int level = Integer.parseInt(f[2]);
			for (Padding padding : new Padding[] { Padding.MIRROR, Padding.PERIODIC, Padding.CONSTANT }) {
				for (java.util.function.Function<int[], int[]> turn : turns) {
					float[][] a = voxels(w(moved(img, n, turn), f[0], f[1], level, padding, Pooling.AVERAGE));
					float[][] b = voxels(moved(w(img, f[0], f[1], level, padding, Pooling.AVERAGE), n, turn));
					double worst = 0;
					for (int z = 0; z < n; z++) {
						for (int i = 0; i < a[z].length; i++) {
							worst = Math.max(worst, Math.abs(a[z][i] - b[z][i]));
						}
					}
					assertTrue(worst < 1e-3, f[0] + f[1] + " " + padding + " worst " + worst);
				}
			}
		}
		// and without pooling the response of the turned image is not the turned response
		float[][] a = voxels(w(moved(img, n, turns.get(0)), "db3", "LHL", 1, Padding.MIRROR, null));
		float[][] b = voxels(moved(w(img, "db3", "LHL", 1, Padding.MIRROR, null), n, turns.get(0)));
		double worst = 0;
		for (int z = 0; z < n; z++) {
			for (int i = 0; i < a[z].length; i++) {
				worst = Math.max(worst, Math.abs(a[z][i] - b[z][i]));
			}
		}
		assertTrue(worst > 1, "worst " + worst);
	}

	@Test
	public void settingsAreChecked() {
		ImagePlus img = noise(9);
		assertThrows(IllegalArgumentException.class, () -> w(img, "db9", "LLL", 1, Padding.MIRROR, null));
		assertThrows(IllegalArgumentException.class, () -> w(img, "db2", "LL", 1, Padding.MIRROR, null));
		assertThrows(IllegalArgumentException.class, () -> w(img, "db2", "LLX", 1, Padding.MIRROR, null));
		assertThrows(IllegalArgumentException.class, () -> w(img, "db2", "LLL", 0, Padding.MIRROR, null));
		assertThrows(IllegalArgumentException.class, () -> ImageFiltering.waveletFilter(img, "db2", "LLL", 1, Padding.MIRROR, null, true));
		assertThrows(IllegalArgumentException.class, () -> ImageFiltering.waveletSet("db2:LLL"));
		assertThrows(IllegalArgumentException.class, () -> ImageFiltering.waveletSet("db2:LLL:x"));
		// case and spaces are free
		assertEquals(voxels(w(img, "db2", "LHL", 1, Padding.MIRROR, null))[3][40],
				voxels(w(img, " DB2 ", "lhl", 1, Padding.MIRROR, null))[3][40], 0f);
	}
}
