package RadiomicsJ4Begginers.Morphplogical;

import features.MorphologicalFeatureType;
import features.MorphologicalFeatures;
import features.TestDataLoader;
import ij.ImagePlus;

public class TestMaximum3DDiameter {

	public static void main(String[] args) {
		ImagePlus[] imgAndMask = TestDataLoader.digital_phantom1();
		MorphologicalFeatures molph = new MorphologicalFeatures(imgAndMask[0], imgAndMask[1], 1);
		
		Double feret = molph.calculate(MorphologicalFeatureType.Maximum3DDiameter.id());
		System.out.println("Maximum 3D Diameter:" + feret);
	}

}
