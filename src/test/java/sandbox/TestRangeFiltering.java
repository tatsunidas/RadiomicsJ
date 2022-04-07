package sandbox;

import RadiomicsJ.ImagePreprocessing;
import RadiomicsJ.TestDataLoader;
import ij.IJ;
import ij.ImagePlus;

public class TestRangeFiltering {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
ImagePlus ct[] = TestDataLoader.sample_ct1();
		
		ImagePlus imp = ct[0];
		ImagePlus mask = ct[1];
		
		ImagePlus resegmentedMask = ImagePreprocessing.rangeFiltering(imp, mask, 1, 400,-1000);
		resegmentedMask.show();
		/*
		 * ok
		 */
		IJ.saveAsTiff(resegmentedMask, "resegmented.tif");
	}
}
