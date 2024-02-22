package RadiomicsJ4Begginers.Morphplogical;

import ij.ImagePlus;
import io.github.tatsunidas.radiomics.features.MorphologicalFeatureType;
import io.github.tatsunidas.radiomics.features.MorphologicalFeatures;
import io.github.tatsunidas.radiomics.main.TestDataLoader;

public class TestIntegratedIntensity {

	public static void main(String[] args) {
		ImagePlus[] imgAndMask = TestDataLoader.digital_phantom1();
		MorphologicalFeatures molph = new MorphologicalFeatures(imgAndMask[0], imgAndMask[1], 1);
		Double ii = molph.calculate(MorphologicalFeatureType.IntegratedIntensity.id());
		System.out.println(MorphologicalFeatureType.IntegratedIntensity+" : "+ ii);
	}

}
