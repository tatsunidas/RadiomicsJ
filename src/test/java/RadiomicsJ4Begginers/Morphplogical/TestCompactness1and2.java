package RadiomicsJ4Begginers.Morphplogical;

import features.MorphologicalFeatureType;
import features.MorphologicalFeatures;
import features.TestDataLoader;
import ij.ImagePlus;

public class TestCompactness1and2 {

	public static void main(String[] args) {
		ImagePlus[] imgAndMask = TestDataLoader.digital_phantom1();
		MorphologicalFeatures molph = new MorphologicalFeatures(imgAndMask[0], imgAndMask[1], 1);
		
		Double c1 = molph.calculate(MorphologicalFeatureType.Compactness1.id());
		Double c2 = molph.calculate(MorphologicalFeatureType.Compactness2.id());
		System.out.println("Compactness 1 :" + c1);
		System.out.println("Compactness 2 :" + c2);
	}

}
