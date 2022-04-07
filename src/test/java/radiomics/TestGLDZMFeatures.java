package radiomics;

import ij.ImagePlus;
import RadiomicsJ.*;

public class TestGLDZMFeatures {
	
	public static void main(String[] args) throws Exception {
		
//		ImagePlus ibsi = TestDataLoader.get2DTestImage(1);
//		GLDZMFeatures f = new GLDZMFeatures(ibsi, null ,4);
//		System.out.println(f.toString(f.getMatrix(true)));
		
		ImagePlus ds_pair[] = TestDataLoader.digital_phantom1();
		ImagePlus imp = ds_pair[0];
		ImagePlus mask = ds_pair[1];
		
		RadiomicsJ.targetLabel = 1;
		boolean useBinCount = true;
		
		GLDZMFeatures f = new GLDZMFeatures(imp, mask, RadiomicsJ.targetLabel, useBinCount, 6, null);
		System.out.println(f.toString(f.getMatrix(true)));
		
		/*
		 * GLDZM'ID is sharing IDs of DLSZM.
		 */
//		System.out.println(f.calculate(RadiomicsJ.GLSZMFeatureTypes.SmallZoneEmphasis.id()));//OK
//		System.out.println(f.calculate(RadiomicsJ.GLSZMFeatureTypes.LargeZoneEmphasis.id()));//OK
//		System.out.println(f.calculate(RadiomicsJ.GLSZMFeatureTypes.LowGrayLevelZoneEmphasis.id()));//OK
//		System.out.println(f.calculate(RadiomicsJ.GLSZMFeatureTypes.HighGrayLevelZoneEmphasis.id()));//OK
//		System.out.println(f.calculate(RadiomicsJ.GLSZMFeatureTypes.SmallZoneLowGrayLevelEmphasis.id()));//OK
//		System.out.println(f.calculate(RadiomicsJ.GLSZMFeatureTypes.SmallZoneHighGrayLevelEmphasis.id()));//OK
//		System.out.println(f.calculate(RadiomicsJ.GLSZMFeatureTypes.LargeZoneLowGrayLevelEmphasis.id()));//OK
//		System.out.println(f.calculate(RadiomicsJ.GLSZMFeatureTypes.LargeZoneHighGrayLevelEmphasis.id()));//OK
//		System.out.println(f.calculate(RadiomicsJ.GLSZMFeatureTypes.GrayLevelNonUniformity.id()));//OK
//		System.out.println(f.calculate(RadiomicsJ.GLSZMFeatureTypes.GrayLevelNonUniformityNormalized.id()));//OK
//		System.out.println(f.calculate(RadiomicsJ.GLSZMFeatureTypes.SizeZoneNonUniformity.id()));//OK
//		System.out.println(f.calculate(RadiomicsJ.GLSZMFeatureTypes.SizeZoneNonUniformityNormalized.id()));//OK
//		System.out.println(f.calculate(RadiomicsJ.GLSZMFeatureTypes.ZonePercentage.id()));//OK
//		System.out.println(f.calculate(RadiomicsJ.GLSZMFeatureTypes.GrayLevelVariance.id()));//OK
//		System.out.println(f.calculate(RadiomicsJ.GLSZMFeatureTypes.ZoneSizeVariance.id()));//OK
//		System.out.println(f.calculate(RadiomicsJ.GLSZMFeatureTypes.ZoneSizeEntropy.id()));//OK
//		
		System.exit(0);
	}
}
