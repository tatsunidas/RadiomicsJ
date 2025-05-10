package radiomics;

import ij.ImagePlus;
import io.github.tatsunidas.radiomics.features.FractalFeatureType;
import io.github.tatsunidas.radiomics.features.FractalFeatures;

public class Fractal2D {

	/**
	 * Full face mask will output -0.0. 
	 * @param args
	 */
	public static void main(String[] args) {
		ImagePlus imp = new ImagePlus("/home/tatsunidas/git/RadiomicsJ/src/test/resources/2d_image/ct.dcm.tif");
		ImagePlus mask = new ImagePlus("/home/tatsunidas/git/RadiomicsJ/src/test/resources/2d_image/ct.dcm_mask.tif");
//		FractalFeatures ff = new FractalFeatures(imp, mask, 255, 1, null);
		FractalFeatures ff = new FractalFeatures(imp, null, 255, 1, null);
		System.out.println(ff.calculate(FractalFeatureType.Capacity.id()));
	}

}
