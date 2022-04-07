package sandbox;

import RadiomicsJ.TestDataLoader;
import RadiomicsJ.Utils;
import ij.ImagePlus;

/**
 * see, trilinear interpolation
 * @author tatsunidas
 *
 */
public class TestResample {
	
	public static void main(String[] args) {

		ImagePlus ct[] = TestDataLoader.sample_ct1();
		
		ImagePlus imp = ct[0];
		ImagePlus mask = ct[1];
		
		ImagePlus res = Utils.resample3D(imp, false, 2d,2d,2d);
		System.out.println(res.getCalibration().pixelWidth);
		System.out.println(res.getCalibration().pixelHeight);
		System.out.println(res.getCalibration().pixelDepth);
		res.show();
//		IJ.saveAs(res, "tif", "resample_test_ct.tif");
		
		ImagePlus resMask = Utils.resample3D(mask, true,2,2,2);
		System.out.println(resMask.getCalibration().pixelWidth);
		System.out.println(resMask.getCalibration().pixelHeight);
		System.out.println(resMask.getCalibration().pixelDepth);
		resMask.show();
//		IJ.saveAs(resMask, "tif", "resample_test_ct_mask.tif");
		
	}
}
