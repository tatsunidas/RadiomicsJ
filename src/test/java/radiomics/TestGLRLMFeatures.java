package radiomics;

import ij.ImagePlus;
import RadiomicsJ.*;

public class TestGLRLMFeatures {
	
	public static void main(String[] args) throws Exception {
		ImagePlus ds_pair[] = TestDataLoader.digital_phantom1();
		ImagePlus imp = ds_pair[0];
		ImagePlus mask = ds_pair[1];
		
		RadiomicsJ.targetLabel = 1;
		
		GLRLMFeatures f = new GLRLMFeatures(
				imp,
				mask,
				1,
				true,
				6,
				null,
				null);
		
		System.out.println(f.calculate(GLRLMFeatureType.ShortRunEmphasis.id()));//OK
//		System.out.println(f.calculate(RadiomicsJ.GLRLMFeatureTypes.LongRunEmphasis.id()));//OK
//		System.out.println(f.calculate(RadiomicsJ.GLRLMFeatureTypes.LowGrayLevelRunEmphasis.id()));//OK
//		System.out.println(RadiomicsJ.GLRLMFeatureTypes.HighGrayLevelRunEmphasis +" , "+f.calculate(RadiomicsJ.GLRLMFeatureTypes.HighGrayLevelRunEmphasis.id()));//OK
//		System.out.println(f.calculate(RadiomicsJ.GLRLMFeatureTypes.ShortRunLowGrayLevelEmphasis.id()));//OK
//		System.out.println(f.calculate(RadiomicsJ.GLRLMFeatureTypes.ShortRunHighGrayLevelEmphasis.id()));//OK
//		System.out.println(f.calculate(RadiomicsJ.GLRLMFeatureTypes.LongRunLowGrayLevelEmphasis.id()));//OK
//		System.out.println(f.calculate(RadiomicsJ.GLRLMFeatureTypes.LongRunHighGrayLevelEmphasis.id()));//OK
//		System.out.println(f.calculate(RadiomicsJ.GLRLMFeatureTypes.GrayLevelNonUniformity.id()));//ok
//		System.out.println(f.calculate(RadiomicsJ.GLRLMFeatureTypes.GrayLevelNonUniformityNormalized.id()));//OK
//		System.out.println(f.calculate(RadiomicsJ.GLRLMFeatureTypes.RunLengthNonUniformity.id()));//ok
//		System.out.println(f.calculate(RadiomicsJ.GLRLMFeatureTypes.RunLengthNonUniformityNormalized.id()));//OK
//		System.out.println(f.calculate(RadiomicsJ.GLRLMFeatureTypes.RunPercentage.id()));//ok
//		System.out.println(f.calculate(RadiomicsJ.GLRLMFeatureTypes.GrayLevelVariance.id()));//OK
//		System.out.println(f.calculate(RadiomicsJ.GLRLMFeatureTypes.RunLengthVariance.id()));//OK
//		System.out.println(f.calculate(RadiomicsJ.GLRLMFeatureTypes.RunEntropy.id()));//ok

		System.exit(0);
	}
}
