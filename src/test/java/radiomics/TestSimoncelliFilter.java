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
import io.github.tatsunidas.radiomics.main.TestDataLoader;

/**
 * The Simoncelli wavelet against the IBSI 2 consensus response maps of test set 1 (8.a).
 *
 * @author tatsunidas <t_kobayashi@vis-ionary.com>
 */
public class TestSimoncelliFilter {

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

	private static ImagePlus sim(ImagePlus img, int level, Padding padding, boolean planar) {
		return ImageFiltering.simoncelliFilter(img, level, padding, planar);
	}

	@Test
	public void ibsi2TestSet1MatchesTheConsensusMaps() {
		ImagePlus checker = checker();
		// 8.a.1 keeps a residual of 0.38 % of the range, which the frequency grid does not explain (docs/SimoncelliFilter.md)
		double first = againstReference(sim(checker, 1, Padding.PERIODIC, false), "8_a_1-ValidCRM.nii");
		assertTrue(first <= 0.01);
		assertTrue(first > 0.001 && first < 0.005, "the residual of 8.a.1 is " + first);
		assertTrue(againstReference(sim(checker, 2, Padding.PERIODIC, false), "8_a_2-ValidCRM.nii") <= 1e-6);
		assertTrue(againstReference(sim(checker, 3, Padding.PERIODIC, false), "8_a_3-ValidCRM.nii") <= 1e-6);
	}

	@Test
	public void aWrongSettingIsNotTheReference() {
		ImagePlus checker = checker();
		// the level
		assertTrue(againstReference(sim(checker, 1, Padding.PERIODIC, false), "8_a_2-ValidCRM.nii") > 0.01);
		assertTrue(againstReference(sim(checker, 3, Padding.PERIODIC, false), "8_a_2-ValidCRM.nii") > 0.01);
		assertTrue(againstReference(sim(checker, 2, Padding.PERIODIC, false), "8_a_3-ValidCRM.nii") > 0.01);
		// each slice on its own is not the volume
		assertTrue(againstReference(sim(checker, 2, Padding.PERIODIC, true), "8_a_2-ValidCRM.nii") > 0.01);
		// the checkerboard has content at its edge, so another padding shows
		assertTrue(againstReference(sim(checker, 1, Padding.MIRROR, false), "8_a_1-ValidCRM.nii") > 0.01);
		assertTrue(againstReference(sim(checker, 2, Padding.CONSTANT, false), "8_a_2-ValidCRM.nii") > 0.01);
	}

	private static ImagePlus noise(int w, int h, int d) {
		Random rnd = new Random(1);
		ImageStack st = new ImageStack(w, h);
		for (int z = 0; z < d; z++) {
			float[] p = new float[w * h];
			for (int i = 0; i < p.length; i++) {
				p[i] = rnd.nextFloat() * 100f;
			}
			st.addSlice(new FloatProcessor(w, h, p));
		}
		return new ImagePlus("noise", st);
	}

	/** out(x, y, z) = in(source(x, y, z)) on a cube */
	private static ImagePlus moved(ImagePlus in, java.util.function.Function<int[], int[]> source) {
		int n = in.getWidth();
		float[][] v = voxels(in);
		ImageStack st = new ImageStack(n, n);
		for (int z = 0; z < n; z++) {
			float[] o = new float[n * n];
			for (int y = 0; y < n; y++) {
				for (int x = 0; x < n; x++) {
					int[] s = source.apply(new int[] { x, y, z });
					o[y * n + x] = v[s[2]][s[1] * n + s[0]];
				}
			}
			st.addSlice(new FloatProcessor(n, n, o));
		}
		return new ImagePlus("moved", st);
	}

	@Test
	public void theFilterIsEquivariantToExchangingTwoAxes() {
		int n = 14;
		ImagePlus img = noise(n, n, n);
		// the frequency grid is the same on every axis of a cube, so exchanging axes is exact
		java.util.List<java.util.function.Function<int[], int[]>> swaps = java.util.Arrays.asList(
				p -> new int[] { p[1], p[0], p[2] }, p -> new int[] { p[2], p[1], p[0] }, p -> new int[] { p[0], p[2], p[1] });
		for (Padding padding : Padding.values()) {
			for (java.util.function.Function<int[], int[]> swap : swaps) {
				float[][] a = voxels(sim(moved(img, swap), 1, padding, false));
				float[][] b = voxels(moved(sim(img, 1, padding, false), swap));
				double worst = 0;
				for (int z = 0; z < n; z++) {
					for (int i = 0; i < a[z].length; i++) {
						worst = Math.max(worst, Math.abs(a[z][i] - b[z][i]));
					}
				}
				assertTrue(worst < 1e-3, padding + " worst " + worst);
			}
		}
	}

	@Test
	public void theMarginHasNoSayInsideTheImage() {
		// a smooth image with a ramp, as a CT slice has; the response near the edge depends on the extension
		int n = 64;
		ImageStack st = new ImageStack(n, n);
		float[] p = new float[n * n];
		for (int y = 0; y < n; y++) {
			for (int x = 0; x < n; x++) {
				p[y * n + x] = (float) (100 * Math.sin(0.35 * x) * Math.cos(0.2 * y) + 5 * x + 3 * y);
			}
		}
		st.addSlice(new FloatProcessor(n, n, p));
		ImagePlus img = new ImagePlus("smooth", st);
		for (Padding padding : new Padding[] { Padding.MIRROR, Padding.NEAREST, Padding.CONSTANT }) {
			for (int level = 1; level <= 3; level++) {
				int m = Math.min(n / 2, 1 << (level + 3));
				float[] a = voxels(ImageFiltering.simoncelliFilter(img, level, padding, m, true))[0];
				float[] b = voxels(ImageFiltering.simoncelliFilter(img, level, padding, 2 * m, true))[0];
				float[] r = a.clone();
				java.util.Arrays.sort(r);
				double span = r[r.length - 1] - r[0], whole = 0, inside = 0;
				for (int y = 0; y < n; y++) {
					for (int x = 0; x < n; x++) {
						double d = Math.abs(a[y * n + x] - b[y * n + x]);
						whole = Math.max(whole, d);
						if (x >= m / 2 && x < n - m / 2 && y >= m / 2 && y < n - m / 2) {
							inside = Math.max(inside, d);
						}
					}
				}
				System.out.println("Simoncelli " + padding + " level " + level + ": margin " + m + " against " + 2 * m
						+ " changes the whole image by " + (100 * whole / span) + "% and the inside (more than m/2 from the edge) by "
						+ (100 * inside / span) + "% of the range");
				assertTrue(inside / span < 0.02, padding + " level " + level + " " + inside / span);
			}
		}
	}

	@Test
	public void aConstantImageHasNoBandPassResponse() {
		ImageStack st = new ImageStack(9, 7);
		for (int z = 0; z < 5; z++) {
			float[] p = new float[63];
			java.util.Arrays.fill(p, 42f);
			st.addSlice(new FloatProcessor(9, 7, p));
		}
		ImagePlus img = new ImagePlus("constant", st);
		for (Padding padding : new Padding[] { Padding.NEAREST, Padding.MIRROR, Padding.PERIODIC }) {
			for (float[] s : voxels(sim(img, 1, padding, false))) {
				for (float v : s) {
					assertEquals(0f, v, 1e-4f, padding.toString());
				}
			}
		}
	}

	@Test
	public void theFourierTransformOfAnyLengthIsTheDiscreteFourierTransform() throws Exception {
		java.lang.reflect.Method fft = ImageFiltering.class.getDeclaredMethod("fftAny", double[].class, double[].class, int.class, boolean.class);
		fft.setAccessible(true);
		Random rnd = new Random(3);
		for (int n : new int[] { 2, 3, 5, 12, 15, 17, 64, 90 }) {
			double[] re = new double[n], im = new double[n];
			for (int i = 0; i < n; i++) {
				re[i] = rnd.nextGaussian();
				im[i] = rnd.nextGaussian();
			}
			double[] fr = re.clone(), fi = im.clone();
			fft.invoke(null, fr, fi, n, false);
			double worst = 0;
			for (int k = 0; k < n; k++) {
				double sr = 0, si = 0;
				for (int t = 0; t < n; t++) {
					double ang = -2 * Math.PI * k * t / n;
					sr += re[t] * Math.cos(ang) - im[t] * Math.sin(ang);
					si += re[t] * Math.sin(ang) + im[t] * Math.cos(ang);
				}
				worst = Math.max(worst, Math.hypot(sr - fr[k], si - fi[k]));
			}
			assertTrue(worst < 1e-9, "n " + n + " worst " + worst);
			// and the inverse brings it back
			fft.invoke(null, fr, fi, n, true);
			for (int t = 0; t < n; t++) {
				assertEquals(re[t], fr[t], 1e-9, "n " + n);
				assertEquals(im[t], fi[t], 1e-9, "n " + n);
			}
		}
	}
}
