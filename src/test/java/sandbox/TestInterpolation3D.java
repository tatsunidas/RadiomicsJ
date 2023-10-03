package sandbox;

import com.vis.radiomics.features.DiagnosticsInfo;
import com.vis.radiomics.main.RadiomicsJ;
import com.vis.radiomics.main.TestDataLoader;
import com.vis.radiomics.main.Utils;

import ij.IJ;
import ij.ImagePlus;

public class TestInterpolation3D {

	public static void main(String[] args) {
//		testTrilinear();
		testTricubicSpline();
	}
	
	public static void testTrilinear() {
		// ct test
		ImagePlus ds_pair[] = TestDataLoader.sample_ct1();
		ImagePlus imp = ds_pair[0];
		ImagePlus mask = ds_pair[1];

		ImagePlus triinterp = Utils.trilinearInterpolation(imp, false, 2.0, 2.0, 2.0);
		ImagePlus triinterp_m = Utils.trilinearInterpolation(mask, true, 2.0, 2.0, 2.0);
//				IJ.saveAsTiff(triinterp, "trilinear.tif");
//				IJ.saveAsTiff(triinterp_m, "trilinear_mask.tif");

		DiagnosticsInfo diag = new DiagnosticsInfo(imp, mask, triinterp, triinterp_m, triinterp_m, RadiomicsJ.label_);
		diag.toString();
		triinterp.show();
	}
	
	public static void testTricubicSpline() {
		// ct test
		ImagePlus ds_pair[] = TestDataLoader.sample_ct1();
		ImagePlus imp = ds_pair[0];
		ImagePlus mask = ds_pair[1];

		ImagePlus triinterp = Utils.tricubicSplineInterpolation(imp, false, 2.0, 2.0, 2.0);
		triinterp.show();
//		ImagePlus triinterp_m = Utils.trilinearInterpolation(mask, true, 2.0, 2.0, 2.0);
//		DiagnosticsInfo diag = new DiagnosticsInfo(imp, mask, triinterp, triinterp_m, triinterp_m, RadiomicsJ.label_);
//		diag.toString();
	}

}
