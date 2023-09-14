package RadiomicsJ4Begginers.Morphplogical;

import com.vis.radiomics.features.MorphologicalFeatureType;
import com.vis.radiomics.features.MorphologicalFeatures;
import com.vis.radiomics.main.TestDataLoader;

import ij.ImagePlus;

public class TestAsphericity {

	public static void main(String[] args) {
		ImagePlus[] imgAndMask = TestDataLoader.digital_phantom1();
		MorphologicalFeatures molph = new MorphologicalFeatures(imgAndMask[0], imgAndMask[1], 1);
		
		Double asph = molph.calculate(MorphologicalFeatureType.Asphericity.id());
		System.out.println("Asphericity:" + asph);
	}

}
