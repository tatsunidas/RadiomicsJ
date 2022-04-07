package radiomics;

import RadiomicsJ.RadiomicsJ;
import RadiomicsJ.Shape2DFeatureType;
import RadiomicsJ.Shape2DFeatures;
import RadiomicsJ.TestDataLoader;
import ij.ImagePlus;

public class TestShape2D {

	public static void main(String[] args) {
		ImagePlus ds_pair[] = TestDataLoader.digital_phantom1();
		ImagePlus imp = ds_pair[0];
		ImagePlus mask = ds_pair[1];
		
		RadiomicsJ.targetLabel = 1;
		
		ImagePlus one = new ImagePlus("one",imp.getStack().getProcessor(1).duplicate());
		one.setCalibration(imp.getCalibration().copy());//must set, if you need calibration. else, you you want raw gray, delete this line.
		ImagePlus one_mask = new ImagePlus("one_mask",mask.getStack().getProcessor(1).duplicate());
		one_mask.setCalibration(mask.getCalibration().copy());
		
		Shape2DFeatures f = new Shape2DFeatures(one, one_mask, 1, RadiomicsJ.targetLabel);
		System.out.println(f.calculate(Shape2DFeatureType.PixelSurface.id()));
		System.out.println(f.calculate(Shape2DFeatureType.Perimeter.id()));
		System.out.println(f.calculate(Shape2DFeatureType.PerimeterToPixelSurfaceRatio.id()));
		System.out.println(f.calculate(Shape2DFeatureType.Sphericity.id()));
		System.out.println(f.calculate(Shape2DFeatureType.SphericalDisproportion.id()));
		System.out.println(f.calculate(Shape2DFeatureType.FerretAngle.id()));
		
	}

}
