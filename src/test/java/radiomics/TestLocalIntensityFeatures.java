package radiomics;

import RadiomicsJ.LocalIntensityFeatureType;
import RadiomicsJ.LocalIntensityFeatures;
import RadiomicsJ.RadiomicsJ;
import RadiomicsJ.TestDataLoader;
import ij.ImagePlus;

public class TestLocalIntensityFeatures {

	public static void main(String[] args) {
		ImagePlus ds_pair[] = TestDataLoader.digital_phantom1();
		ImagePlus imp = ds_pair[0];
		ImagePlus mask = ds_pair[1];
		
		RadiomicsJ.targetLabel = 1;
		
		LocalIntensityFeatures f = new LocalIntensityFeatures(imp, mask, RadiomicsJ.targetLabel);
		System.out.println(f.calculate(LocalIntensityFeatureType.LocalIntensityPeak.id()));
		System.out.println(f.calculate(LocalIntensityFeatureType.GlobalIntensityPeak.id()));
		
		System.exit(0);
	}

}
