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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.process.FloatProcessor;
import io.github.tatsunidas.radiomics.main.ImageFiltering;
import io.github.tatsunidas.radiomics.main.ImageFiltering.Padding;
import io.github.tatsunidas.radiomics.main.TestDataLoader;

/**
 * The Laplacian of Gaussian filter against equation 4.2 of the IBSI 2 reference
 * manual and against the IBSI 2 consensus response maps of test set 1.
 *
 * @author tatsunidas <t_kobayashi@vis-ionary.com>
 */
public class TestLoGFilter {

	private static final String PHANTOM = "data_sets-master/ibsi_2_digital_phantom/nifti/";
	private static final String REFERENCE = "data_sets-master/ibsi_2_reference_data/reference_response_maps/";

	/** IBSI 2 equation 4.2, sigma and k in voxels */
	private static double log(double sigma, int dim, double r2) {
		return -(1.0 / (sigma * sigma)) * Math.pow(1.0 / (Math.sqrt(2.0 * Math.PI) * sigma), dim)
				* (dim - r2 / (sigma * sigma)) * Math.exp(-r2 / (2.0 * sigma * sigma));
	}

	private static float[][] voxels(ImagePlus imp) {
		int d = imp.getStackSize();
		float[][] v = new float[d][];
		Calibration cal = imp.getCalibration();
		for (int z = 0; z < d; z++) {
			float[] s = new float[imp.getWidth() * imp.getHeight()];
			for (int i = 0; i < s.length; i++) {
				s[i] = (float) cal.getCValue(imp.getStack().getProcessor(z + 1).getf(i));
			}
			v[z] = s;
		}
		return v;
	}

	private static ImagePlus volume(float[][] v, int w, int h, double sx, double sy, double sz) {
		ImageStack stack = new ImageStack(w, h);
		for (float[] s : v) {
			stack.addSlice(new FloatProcessor(w, h, s.clone()));
		}
		ImagePlus imp = new ImagePlus("volume", stack);
		Calibration cal = imp.getCalibration();
		cal.pixelWidth = sx;
		cal.pixelHeight = sy;
		cal.pixelDepth = sz;
		cal.setUnit("mm");
		return imp;
	}

	private static float[][] random(int w, int h, int d, long seed) {
		Random r = new Random(seed);
		float[][] v = new float[d][w * h];
		for (float[] s : v) {
			for (int i = 0; i < s.length; i++) {
				s[i] = (float) (r.nextGaussian() * 100.0);
			}
		}
		return v;
	}

	/** largest difference between an impulse response and A g[k] sampled at the given sigma (voxels) */
	private static double impulseError(float[][] res, float[][] img, int w, int h, double sigma, int radius) {
		int cx = -1, cy = -1, cz = -1;
		double amp = 0;
		for (int z = 0; z < img.length; z++) {
			for (int i = 0; i < img[z].length; i++) {
				if (img[z][i] != 0) {
					assertEquals(-1, cz, "the phantom has a single impulse");
					cx = i % w;
					cy = i / w;
					cz = z;
					amp = img[z][i];
				}
			}
		}
		double worst = 0;
		for (int z = 0; z < res.length; z++) {
			for (int i = 0; i < res[z].length; i++) {
				int dx = i % w - cx, dy = i / w - cy, dz = z - cz;
				double expected = 0;
				if (Math.abs(dx) <= radius && Math.abs(dy) <= radius && Math.abs(dz) <= radius) {
					expected = amp * log(sigma, 3, dx * dx + dy * dy + dz * dz);
				}
				worst = Math.max(worst, Math.abs(res[z][i] - expected));
			}
		}
		return worst;
	}

	private static double maxAbs(float[][] v) {
		double m = 0;
		for (float[] s : v) {
			for (float f : s) {
				m = Math.max(m, Math.abs(f));
			}
		}
		return m;
	}

	@Test
	public void impulseGivesTheKernelOfEquation42() {
		ImagePlus imp = TestDataLoader.loadNifTi(PHANTOM + "impulse/image/impulse.nii.gz");
		assertEquals(2.0, imp.getCalibration().pixelWidth, 1e-9);
		float[][] img = voxels(imp);
		float[][] res = voxels(ImageFiltering.laplacianOfGaussian(imp, 3.0, 4.0, Padding.CONSTANT, false));
		int w = imp.getWidth(), h = imp.getHeight();
		double tol = 1e-5 * maxAbs(res);
		// sigma 3 mm on a 2 mm grid is 1.5 voxels, half width floor(4 * 1.5 + 0.5) = 6
		assertTrue(impulseError(res, img, w, h, 1.5, 6) <= tol);
		// a sigma 10% off, or sigma taken as voxels instead of mm, is not the same kernel
		assertTrue(impulseError(res, img, w, h, 1.65, 7) > tol);
		assertTrue(impulseError(res, img, w, h, 3.0, 12) > tol);
		// a bright impulse responds negatively at its centre
		float min = Float.MAX_VALUE;
		for (float[] s : res) {
			for (float f : s) {
				min = Math.min(min, f);
			}
		}
		assertEquals(-maxAbs(res), min, 0.0);
	}

	/**
	 * Every voxel within 1% of the intensity range of the consensus map, the
	 * compliance criterion of IBSI 2 section 7.1. Returns that fraction.
	 */
	private static double againstReference(float[][] res, String reference) {
		float[][] ref = voxels(TestDataLoader.loadNifTi(REFERENCE + reference));
		assertEquals(ref.length, res.length);
		double lo = Double.MAX_VALUE, hi = -Double.MAX_VALUE, worst = 0;
		for (int z = 0; z < ref.length; z++) {
			assertEquals(ref[z].length, res[z].length);
			for (int i = 0; i < ref[z].length; i++) {
				lo = Math.min(lo, ref[z][i]);
				hi = Math.max(hi, ref[z][i]);
				worst = Math.max(worst, Math.abs(res[z][i] - ref[z][i]));
			}
		}
		double fraction = worst / (hi - lo);
		System.out.println("IBSI 2 " + reference + ": largest difference " + worst + ", "
				+ (100.0 * fraction) + "% of the reference range " + (hi - lo));
		return fraction;
	}

	@Test
	public void ibsi2TestSet1MatchesTheConsensusMaps() {
		ImagePlus impulse = TestDataLoader.loadNifTi(PHANTOM + "impulse/image/impulse.nii.gz");
		ImagePlus checker = TestDataLoader.loadNifTi(PHANTOM + "checkerboard/image/checkerboard.nii.gz");
		assertTrue(againstReference(
				voxels(ImageFiltering.laplacianOfGaussian(impulse, 3.0, 4.0, Padding.CONSTANT, false)),
				"2_a-ValidCRM.nii") <= 0.01);
		assertTrue(againstReference(
				voxels(ImageFiltering.laplacianOfGaussian(checker, 5.0, 4.0, Padding.MIRROR, false)),
				"2_b-ValidCRM.nii") <= 0.01);
		assertTrue(againstReference(
				voxels(ImageFiltering.laplacianOfGaussian(checker, 5.0, 4.0, Padding.MIRROR, true)),
				"2_c-ValidCRM.nii") <= 0.01);
		// the checkerboard reaches the edge, so the padding the test set asks for matters
		assertTrue(againstReference(
				voxels(ImageFiltering.laplacianOfGaussian(checker, 5.0, 4.0, Padding.CONSTANT, false)),
				"2_b-ValidCRM.nii") > 0.01);
	}

	@Test
	public void paddingOnlyMattersNearTheEdge() {
		int n = 24, radius = 3; // sigma 1 mm on 1 mm voxels, truncate 3
		float[][] v = random(n, n, n, 1L);
		ImagePlus imp = volume(v, n, n, 1.0, 1.0, 1.0);
		float[][][] res = new float[Padding.values().length][][];
		for (Padding p : Padding.values()) {
			res[p.ordinal()] = voxels(ImageFiltering.laplacianOfGaussian(imp, 1.0, 3.0, p, false));
		}
		for (int z = radius; z < n - radius; z++) {
			for (int y = radius; y < n - radius; y++) {
				for (int x = radius; x < n - radius; x++) {
					float a = res[0][z][y * n + x];
					for (int p = 1; p < res.length; p++) {
						assertEquals(Float.floatToIntBits(a), Float.floatToIntBits(res[p][z][y * n + x]));
					}
				}
			}
		}
		// at a corner the four paddings all read something different
		for (int p = 0; p < res.length; p++) {
			for (int q = p + 1; q < res.length; q++) {
				assertTrue(res[p][0][0] != res[q][0][0], Padding.values()[p] + " and " + Padding.values()[q]);
			}
		}
	}

	@Test
	public void mirrorRepeatsTheEdgeVoxel() {
		// a single row, filtered in plane: the y pass sees a column of one voxel
		float[] row = { 7f, -3f, 11f, 2f, 5f };
		int n = row.length;
		ImagePlus imp = volume(new float[][] { row }, n, 1, 1.0, 1.0, 1.0);
		double sigma = 1.0;
		int radius = 2;
		float[] res = voxels(ImageFiltering.laplacianOfGaussian(imp, sigma, 2.0, Padding.MIRROR, true))[0];
		double sumG = 0, sumG2 = 0;
		double[] g = new double[2 * radius + 1], g2 = new double[2 * radius + 1];
		for (int k = -radius; k <= radius; k++) {
			double e = Math.exp(-k * k / (2 * sigma * sigma)) / (Math.sqrt(2 * Math.PI) * sigma);
			g[k + radius] = e;
			g2[k + radius] = e * (k * k / Math.pow(sigma, 4) - 1 / (sigma * sigma));
			sumG += g[k + radius];
			sumG2 += g2[k + radius];
		}
		// extended by hand, two voxels each side: with the edge -3 7 | 7 -3 11 2 5 | 5 2,
		// without it 11 -3 | 7 -3 11 2 5 | 2 11
		float[] withEdge = { -3f, 7f, 7f, -3f, 11f, 2f, 5f, 5f, 2f };
		float[] withoutEdge = { 11f, -3f, 7f, -3f, 11f, 2f, 5f, 2f, 11f };
		boolean differsFromWithout = false;
		for (int x = 0; x < n; x++) {
			double a = 0, b = 0, c = 0, e = 0;
			for (int k = 0; k < g.length; k++) {
				a += g2[k] * withEdge[x + k];
				b += g[k] * withEdge[x + k];
				c += g2[k] * withoutEdge[x + k];
				e += g[k] * withoutEdge[x + k];
			}
			double expected = a * sumG + b * sumG2;
			double without = c * sumG + e * sumG2;
			assertEquals(expected, res[x], 1e-5 * Math.abs(expected) + 1e-6);
			if (Math.abs(without - res[x]) > 1e-3) {
				differsFromWithout = true;
			}
		}
		assertTrue(differsFromWithout);
	}

	@Test
	public void aCropWithAMarginOfTheHalfWidthGivesTheSameRoi() {
		int n = 40, radius = 6; // sigma 2 mm on 1 mm voxels, truncate 3
		float[][] v = random(n, n, n, 2L);
		float[][] full = voxels(ImageFiltering.laplacianOfGaussian(volume(v, n, n, 1, 1, 1), 2.0, 3.0,
				Padding.MIRROR, false));
		int lo = 15, hi = 25; // roi box, well inside the image
		assertTrue(cropMatches(v, full, n, lo, hi, radius));
		assertFalse(cropMatches(v, full, n, lo, hi, radius - 1));
	}

	private static boolean cropMatches(float[][] v, float[][] full, int n, int lo, int hi, int margin) {
		int a = lo - margin, m = hi - lo + 2 * margin;
		float[][] crop = new float[m][m * m];
		for (int z = 0; z < m; z++) {
			for (int y = 0; y < m; y++) {
				for (int x = 0; x < m; x++) {
					crop[z][y * m + x] = v[a + z][(a + y) * n + a + x];
				}
			}
		}
		float[][] res = voxels(ImageFiltering.laplacianOfGaussian(volume(crop, m, m, 1, 1, 1), 2.0, 3.0,
				Padding.MIRROR, false));
		for (int z = lo; z < hi; z++) {
			for (int y = lo; y < hi; y++) {
				for (int x = lo; x < hi; x++) {
					float c = res[z - a][(y - a) * m + x - a];
					if (Float.floatToIntBits(c) != Float.floatToIntBits(full[z][y * n + x])) {
						return false;
					}
				}
			}
		}
		return true;
	}

	@Test
	public void planarStaysInItsSlice() {
		int n = 21;
		float[][] v = new float[9][n * n];
		v[4][10 * n + 10] = 100f;
		float[][] planar = voxels(ImageFiltering.laplacianOfGaussian(volume(v, n, n, 0.7, 0.7, 5.0), 1.4, 4.0,
				Padding.CONSTANT, true));
		for (int z = 0; z < v.length; z++) {
			for (float f : planar[z]) {
				if (z != 4) {
					assertEquals(0f, f, 0f);
				}
			}
		}
		assertTrue(maxAbs(new float[][] { planar[4] }) > 0);
		float[][] volumetric = voxels(ImageFiltering.laplacianOfGaussian(volume(v, n, n, 1, 1, 1), 1.0, 4.0,
				Padding.CONSTANT, false));
		assertTrue(maxAbs(new float[][] { volumetric[3] }) > 0);
	}

	@Test
	public void anAnisotropicGridIsRefused() {
		float[][] v = random(8, 8, 8, 3L);
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
				() -> ImageFiltering.laplacianOfGaussian(volume(v, 8, 8, 0.5, 1.0, 2.0), 1.0, 4.0, Padding.MIRROR, false));
		assertTrue(e.getMessage().contains("Resample"));
		assertThrows(IllegalArgumentException.class,
				() -> ImageFiltering.laplacianOfGaussian(volume(v, 8, 8, 0.7, 0.7, 5.0), 1.0, 4.0, Padding.MIRROR, false));
		assertThrows(IllegalArgumentException.class,
				() -> ImageFiltering.laplacianOfGaussian(volume(v, 8, 8, 0.5, 1.0, 1.0), 1.0, 4.0, Padding.MIRROR, true));
		// in plane only the two in-plane spacings have to agree
		ImageFiltering.laplacianOfGaussian(volume(v, 8, 8, 0.7, 0.7, 5.0), 1.0, 4.0, Padding.MIRROR, true);
		assertThrows(IllegalArgumentException.class,
				() -> ImageFiltering.laplacianOfGaussian(volume(v, 8, 8, 1, 1, 1), 0.0, 4.0, Padding.MIRROR, false));
		assertThrows(IllegalArgumentException.class,
				() -> ImageFiltering.laplacianOfGaussian(volume(v, 8, 8, 1, 1, 1), 1.0, -1.0, Padding.MIRROR, false));
	}

	@Test
	public void runningOnOneCoreGivesTheSameMap() {
		int n = 32;
		ImagePlus imp = volume(random(n, n, n, 4L), n, n, 1, 1, 1);
		boolean before = ImageFiltering.parallel;
		try {
			ImageFiltering.parallel = true;
			float[][] a = voxels(ImageFiltering.laplacianOfGaussian(imp, 2.0, 4.0, Padding.NEAREST, false));
			ImageFiltering.parallel = false;
			float[][] b = voxels(ImageFiltering.laplacianOfGaussian(imp, 2.0, 4.0, Padding.NEAREST, false));
			for (int z = 0; z < n; z++) {
				assertArrayEquals(a[z], b[z], 0f);
			}
		} finally {
			ImageFiltering.parallel = before;
		}
	}
}
