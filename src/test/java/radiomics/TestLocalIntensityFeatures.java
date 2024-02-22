package radiomics;

import ij.ImagePlus;
import io.github.tatsunidas.radiomics.features.LocalIntensityFeatureType;
import io.github.tatsunidas.radiomics.features.LocalIntensityFeatures;
import io.github.tatsunidas.radiomics.main.RadiomicsJ;
import io.github.tatsunidas.radiomics.main.TestDataLoader;

public class TestLocalIntensityFeatures {

	public static void main(String[] args) {
		ImagePlus ds_pair[] = TestDataLoader.digital_phantom1();
		ImagePlus imp = ds_pair[0];
		ImagePlus mask = ds_pair[1];
		
		RadiomicsJ.targetLabel = 1;
		
		LocalIntensityFeatures f = new LocalIntensityFeatures(imp, mask, RadiomicsJ.targetLabel);
		System.out.println(LocalIntensityFeatureType.LocalIntensityPeak +":"+ f.calculate(LocalIntensityFeatureType.LocalIntensityPeak.id()));
		System.out.println(LocalIntensityFeatureType.GlobalIntensityPeak +":"+ f.calculate(LocalIntensityFeatureType.GlobalIntensityPeak.id()));
		
		System.exit(0);
	}

}
