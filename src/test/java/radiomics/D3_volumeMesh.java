package radiomics;

import ij.ImagePlus;
import io.github.tatsunidas.radiomics.features.MorphologicalFeatureType;
import io.github.tatsunidas.radiomics.features.MorphologicalFeatures;
import io.github.tatsunidas.radiomics.main.TestDataLoader;

public class D3_volumeMesh {

	public static void main(String[] args) {
		ImagePlus[] set = TestDataLoader.digital_phantom1_scratch();
		MorphologicalFeatures mf = new MorphologicalFeatures(set[0], set[1], 1);
		System.out.println(mf.calculate(MorphologicalFeatureType.VolumeMesh.id()));
	}
}
