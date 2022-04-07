package sandbox;

import RadiomicsJ.TestDataLoader;
import ij.ImagePlus;

/*
 * samples https://imagej.nih.gov/ij/images/
 */
public class TestNormalize {

	public static void main(String[] args) throws Throwable {
		
		ImagePlus ds_pair[] = TestDataLoader.sample_ct1();
		ImagePlus imp = ds_pair[0];
//		ImagePlus mask = ds_pair[1];
		ImagePlus std_imp = RadiomicsJ.ImagePreprocessing.normalize(imp, null,1);
		std_imp.show();
		ij.IJ.saveAsTiff(std_imp, "ct_normalized.tif");
		System.exit(0);
	}
}
