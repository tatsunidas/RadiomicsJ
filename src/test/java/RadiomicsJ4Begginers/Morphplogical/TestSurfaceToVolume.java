package RadiomicsJ4Begginers.Morphplogical;

import features.MorphologicalFeatureType;
import features.MorphologicalFeatures;
import features.TestDataLoader;
import ij.ImagePlus;

public class TestSurfaceToVolume {

	public static void main(String[] args) {
		ImagePlus[] imgAndMask = TestDataLoader.digital_phantom1();
		MorphologicalFeatures molph = new MorphologicalFeatures(imgAndMask[0], imgAndMask[1], 1);
		
		Double svr = molph.calculate(MorphologicalFeatureType.SurfaceToVolumeRatio.id());
		System.out.println("Surface to volume ratio -Mesh-:" + svr);
	}

}
