package radiomics;

import ij.ImagePlus;
import RadiomicsJ.*;

public class TestGLSZMFeatures {
	
	public static void main(String[] args) throws Exception {
		ImagePlus ds_pair[] = TestDataLoader.digital_phantom1();
		ImagePlus imp = ds_pair[0];
		ImagePlus mask = ds_pair[1];
		
		RadiomicsJ.targetLabel = 1;
		boolean useBinCount = true;
		Integer nBins = 6;
		Double binWidth = null;
		 
		GLSZMFeatures f = new GLSZMFeatures(imp, mask ,RadiomicsJ.targetLabel, useBinCount, nBins, binWidth);
		
		
		System.out.println(f.calculate(GLSZMFeatureType.SmallZoneEmphasis.id()));//OK
		System.out.println(f.calculate(GLSZMFeatureType.LargeZoneEmphasis.id()));//OK
		System.out.println(f.calculate(GLSZMFeatureType.LowGrayLevelZoneEmphasis.id()));//OK
		System.out.println(f.calculate(GLSZMFeatureType.HighGrayLevelZoneEmphasis.id()));//OK
		System.out.println(f.calculate(GLSZMFeatureType.SmallZoneLowGrayLevelEmphasis.id()));//OK
		System.out.println(f.calculate(GLSZMFeatureType.SmallZoneHighGrayLevelEmphasis.id()));//OK
		System.out.println(f.calculate(GLSZMFeatureType.LargeZoneLowGrayLevelEmphasis.id()));//OK
		System.out.println(f.calculate(GLSZMFeatureType.LargeZoneHighGrayLevelEmphasis.id()));//OK
		System.out.println(f.calculate(GLSZMFeatureType.GrayLevelNonUniformity.id()));//OK
		System.out.println(f.calculate(GLSZMFeatureType.GrayLevelNonUniformityNormalized.id()));//OK
		System.out.println(f.calculate(GLSZMFeatureType.SizeZoneNonUniformity.id()));//OK
		System.out.println(f.calculate(GLSZMFeatureType.SizeZoneNonUniformityNormalized.id()));//OK
		System.out.println(f.calculate(GLSZMFeatureType.ZonePercentage.id()));//OK
		System.out.println(f.calculate(GLSZMFeatureType.GrayLevelVariance.id()));//OK
		System.out.println(f.calculate(GLSZMFeatureType.ZoneSizeVariance.id()));//OK
		System.out.println(f.calculate(GLSZMFeatureType.ZoneSizeEntropy.id()));//OK
		
		System.exit(0);
	}
}
