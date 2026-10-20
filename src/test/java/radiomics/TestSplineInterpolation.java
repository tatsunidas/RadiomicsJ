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

import java.util.Random;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.FloatProcessor;
import io.github.tatsunidas.radiomics.main.RadiomicsJ;
import io.github.tatsunidas.radiomics.main.TestDataLoader;
import io.github.tatsunidas.radiomics.main.Utils;

/**
 * The tricubic spline interpolation against IBSI 1 configuration E (the lung cancer CT
 * phantom, 2 x 2 x 2 mm, tricubic spline, intensities rounded, trilinear mask with a
 * partial volume of 0.5), whose interpolated image and roi have reference values
 * (config E of validation/IBSI_ValidationFile.xlsx), and against the property that
 * defines an interpolating spline: it passes through the samples.
 *
 * @author tatsunidas <t_kobayashi@vis-ionary.com>
 */
public class TestSplineInterpolation {

	@AfterEach
	public void reset() {
		RadiomicsJ.resetSettings();
	}

	@Test
	public void ibsi1ConfigurationEInterpolatedImageAndRoi() {
		ImagePlus img = TestDataLoader.loadNifTi("data_sets-master/ibsi_1_ct_radiomics_phantom/nifti/image/phantom.nii.gz");
		ImagePlus msk = TestDataLoader.loadNifTi("data_sets-master/ibsi_1_ct_radiomics_phantom/nifti/mask/mask.nii.gz");
		ImagePlus f = Utils.createImageCopyAsFloat(img, false);
		ImagePlus m = Utils.initMaskAsFloatAndConvertLabelOne(msk, 1);
		RadiomicsJ.mask_PartialVolumeThreshold = 0.5;
		ImagePlus ri = Utils.roundIntensitiesToNearestInteger(Utils.resample3D(f, false, 2, 2, 2, RadiomicsJ.TRICUBIC_SPLINE));
		ImagePlus rm = Utils.resample3D(m, true, 2, 2, 2, RadiomicsJ.TRILINEAR);
		assertEquals(100, ri.getWidth(), 1);
		assertEquals(99, ri.getHeight(), 0.8);
		assertEquals(90, ri.getStackSize());

		double isum = 0, imin = Double.MAX_VALUE, imax = -Double.MAX_VALUE, sum = 0, min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
		long in = 0, n = 0;
		for (int z = 1; z <= ri.getStackSize(); z++) {
			float[] a = (float[]) ri.getStack().getProcessor(z).convertToFloat().getPixels();
			float[] k = (float[]) rm.getStack().getProcessor(z).convertToFloat().getPixels();
			for (int i = 0; i < a.length; i++) {
				imin = Math.min(imin, a[i]);
				imax = Math.max(imax, a[i]);
				isum += a[i];
				in++;
				if (k[i] == 1f) {
					sum += a[i];
					min = Math.min(min, a[i]);
					max = Math.max(max, a[i]);
					n++;
				}
			}
		}
		System.out.println("config E: image mean " + isum / in + " min " + imin + " max " + imax + "; roi n " + n + " mean "
				+ sum / n + " min " + min + " max " + max);
		// reference values and tolerances of the config E sheet
		assertEquals(-270d, isum / in, 3d);
		assertEquals(-1111d, imin, 10d);
		assertEquals(2637d, imax, 30d);
		assertEquals(45985d, n, 100d);
		assertEquals(-48.3, sum / n, 0.1);
		assertEquals(-966d, min, 1d);
		assertEquals(627d, max, 5d);
	}

	@Test
	public void passesThroughTheSamples() {
		int w = 12, h = 10, d = 9;
		Random r = new Random(3);
		ImageStack st = new ImageStack(w, h);
		for (int z = 0; z < d; z++) {
			FloatProcessor ip = new FloatProcessor(w, h);
			for (int i = 0; i < w * h; i++) {
				ip.setf(i, (float) (1000 * r.nextGaussian()));
			}
			st.addSlice(ip);
		}
		ImagePlus imp = new ImagePlus("v", st);
		imp.getCalibration().pixelWidth = 1d;
		imp.getCalibration().pixelHeight = 1d;
		imp.getCalibration().pixelDepth = 1d;
		ImagePlus same = Utils.tricubicSplineInterporation(imp, 1d, 1d, 1d);
		assertEquals(d, same.getStackSize());
		double worst = 0, spread = 0;
		for (int z = 1; z <= d; z++) {
			for (int i = 0; i < w * h; i++) {
				worst = Math.max(worst, Math.abs(same.getStack().getProcessor(z).getf(i) - imp.getStack().getProcessor(z).getf(i)));
				spread = Math.max(spread, Math.abs(imp.getStack().getProcessor(z).getf(i)));
			}
		}
		// a smoothing basis, as before, is off by a large part of the spread
		assertEquals(0d, worst / spread, 1e-4);
	}
}
