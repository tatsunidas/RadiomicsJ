package radiomics;

import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import io.github.tatsunidas.radiomics.features.*;
import io.github.tatsunidas.radiomics.main.RadiomicsJ;
import io.github.tatsunidas.radiomics.main.TestDataLoader;
import io.github.tatsunidas.radiomics.main.Utils;

public class TestGLDZMFeatures {
	
	public static void main(String[] args) throws Exception {
//		checkMatrix();
		checkFeatures();
	}
	
	static void checkMatrix() {
		// create 2d array
		byte pixels[] = new byte[16];
		byte r0[] = new byte[] { 1, 2, 2, 3 };
		byte r1[] = new byte[] { 1, 2, 3, 3 };
		byte r2[] = new byte[] { 4, 2, 4, 1 };
		byte r3[] = new byte[] { 4, 1, 2, 3 };
		// flatten to create ByteProcessor
		int i= 0;
		for(byte[] r: new byte[][] {r0,r1,r2,r3}) {
			for(byte v:r) {
				pixels[i++] = v;
			}
		}
		ImageProcessor bp = new ByteProcessor(4, 4, pixels);
		ImagePlus imp = new ImagePlus("sample", bp);
		int nBins = 4;// 1 to 4
		
		//init
		GLDZMFeatures test =null;
		try {
			test = new GLDZMFeatures(
					imp,
					null,
					1,
					true,
					nBins,
					null);
		} catch (Exception e) {
			e.printStackTrace();
		}
		/**
		 * In IBSI, their are showing the result of 4-connectedness 2D.
		 * But, in reference, "We consider 26-connectedness for a 3D approach and 8-connectedness in the 2D approach."
		 * So, RadiomicsJ calculate under 8-connectedness in 2D. 
		 */
		System.out.println(test.toString(test.getMatrix(true)));
	}
	
	static void checkFeatures() throws Exception {
		ImagePlus ds_pair[] = TestDataLoader.digital_phantom1();
		ImagePlus imp = ds_pair[0];
		ImagePlus mask = ds_pair[1];
		
		RadiomicsJ.targetLabel = 1;
		boolean useBinCount = true;
		
		GLDZMFeatures f = new GLDZMFeatures(imp, mask, RadiomicsJ.targetLabel, useBinCount, 6, null);
		System.out.println(f.toString(f.getMatrix(true)));
		
		/*
		 * GLDZM'ID is sharing IDs with DLSZM.
		 */
		System.out.println(GLDZMFeatureType.SmallDistanceEmphasis+":"+f.calculate(GLDZMFeatureType.SmallDistanceEmphasis.id()));//OK
		System.out.println(GLDZMFeatureType.LargeDistanceEmphasis+":"+f.calculate(GLDZMFeatureType.LargeDistanceEmphasis.id()));//OK
		System.out.println(GLDZMFeatureType.LowGrayLevelZoneEmphasis+":"+f.calculate(GLDZMFeatureType.LowGrayLevelZoneEmphasis.id()));//OK
		System.out.println(GLDZMFeatureType.HighGrayLevelZoneEmphasis+":"+f.calculate(GLDZMFeatureType.HighGrayLevelZoneEmphasis.id()));//OK
		System.out.println(GLDZMFeatureType.SmallDistanceLowGrayLevelEmphasis+":"+f.calculate(GLDZMFeatureType.SmallDistanceLowGrayLevelEmphasis.id()));//OK
		System.out.println(GLDZMFeatureType.SmallDistanceHighGrayLevelEmphasis+":"+f.calculate(GLDZMFeatureType.SmallDistanceHighGrayLevelEmphasis.id()));//OK
		System.out.println(GLDZMFeatureType.LargeDistanceLowGrayLevelEmphasis+":"+f.calculate(GLDZMFeatureType.LargeDistanceLowGrayLevelEmphasis.id()));//OK
		System.out.println(GLDZMFeatureType.LargeDistanceHighGrayLevelEmphasis+":"+f.calculate(GLDZMFeatureType.LargeDistanceHighGrayLevelEmphasis.id()));//OK
		System.out.println(GLDZMFeatureType.GrayLevelNonUniformity+":"+f.calculate(GLDZMFeatureType.GrayLevelNonUniformity.id()));//OK
		System.out.println(GLDZMFeatureType.GrayLevelNonUniformityNormalized+":"+f.calculate(GLDZMFeatureType.GrayLevelNonUniformityNormalized.id()));//OK
		System.out.println(GLDZMFeatureType.ZoneDistanceNonUniformity+":"+f.calculate(GLDZMFeatureType.ZoneDistanceNonUniformity.id()));//OK
		System.out.println(GLDZMFeatureType.ZoneDistanceNonUniformityNormalized+":"+f.calculate(GLDZMFeatureType.ZoneDistanceNonUniformityNormalized.id()));//OK
		System.out.println(GLDZMFeatureType.ZonePercentage+":"+f.calculate(GLDZMFeatureType.ZonePercentage.id()));//OK
		System.out.println(GLDZMFeatureType.GrayLevelVariance+":"+f.calculate(GLDZMFeatureType.GrayLevelVariance.id()));//OK
		System.out.println(GLDZMFeatureType.ZoneDistanceVariance+":"+f.calculate(GLDZMFeatureType.ZoneDistanceVariance.id()));//OK
		System.out.println(GLDZMFeatureType.ZoneDistanceEntropy+":"+f.calculate(GLDZMFeatureType.ZoneDistanceEntropy.id()));//OK
		System.exit(0);
	}
}
