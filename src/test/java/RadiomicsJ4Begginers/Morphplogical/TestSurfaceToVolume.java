package RadiomicsJ4Begginers.Morphplogical;

import ij.ImagePlus;
import io.github.tatsunidas.radiomics.features.MorphologicalFeatureType;
import io.github.tatsunidas.radiomics.features.MorphologicalFeatures;
import io.github.tatsunidas.radiomics.main.TestDataLoader;

public class TestSurfaceToVolume {

	public static void main(String[] args) {
		ImagePlus[] imgAndMask = TestDataLoader.digital_phantom1();
		MorphologicalFeatures molph = new MorphologicalFeatures(imgAndMask[0], imgAndMask[1], 1);
		
		Double svr = molph.calculate(MorphologicalFeatureType.SurfaceToVolumeRatio.id());
		System.out.println("Surface to volume ratio -Mesh-:" + svr);
	}

}
