package radiomics;

import com.vis.radiomics.features.*;
import com.vis.radiomics.main.RadiomicsJ;
import com.vis.radiomics.main.TestDataLoader;
import com.vis.radiomics.main.Utils;

import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

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
