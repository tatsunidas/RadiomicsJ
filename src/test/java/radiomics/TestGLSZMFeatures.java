package radiomics;

import com.vis.radiomics.features.*;
import com.vis.radiomics.main.RadiomicsJ;
import com.vis.radiomics.main.TestDataLoader;

import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

public class TestGLSZMFeatures {
	
	public static void main(String[] args) throws Exception {
		
		checkMatrix();
		System.exit(0);
	}
	
	static void checkMatrix() {
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
		
		GLSZMFeatures f = null;
		try {
			f = new GLSZMFeatures(imp, null ,RadiomicsJ.targetLabel, true /*useBinCount*/, nBins, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		System.out.println(f.toString());
		
	}
	
	static void checkFeatures() {
		ImagePlus ds_pair[] = TestDataLoader.digital_phantom1();
		ImagePlus imp = ds_pair[0];
		ImagePlus mask = ds_pair[1];
		
		RadiomicsJ.targetLabel = 1;
		boolean useBinCount = true;
		Integer nBins = 6;
		Double binWidth = null;
		 
		GLSZMFeatures f = null;
		try {
			f = new GLSZMFeatures(imp, mask ,RadiomicsJ.targetLabel, useBinCount, nBins, binWidth);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		System.out.println(GLSZMFeatureType.SmallZoneEmphasis+":"+f.calculate(GLSZMFeatureType.SmallZoneEmphasis.id()));//OK
		System.out.println(GLSZMFeatureType.LargeZoneEmphasis+":"+f.calculate(GLSZMFeatureType.LargeZoneEmphasis.id()));//OK
		System.out.println(GLSZMFeatureType.LowGrayLevelZoneEmphasis+":"+f.calculate(GLSZMFeatureType.LowGrayLevelZoneEmphasis.id()));//OK
		System.out.println(GLSZMFeatureType.HighGrayLevelZoneEmphasis+":"+f.calculate(GLSZMFeatureType.HighGrayLevelZoneEmphasis.id()));//OK
		System.out.println(GLSZMFeatureType.SmallZoneLowGrayLevelEmphasis+":"+f.calculate(GLSZMFeatureType.SmallZoneLowGrayLevelEmphasis.id()));//OK
		System.out.println(GLSZMFeatureType.SmallZoneHighGrayLevelEmphasis+":"+f.calculate(GLSZMFeatureType.SmallZoneHighGrayLevelEmphasis.id()));//OK
		System.out.println(GLSZMFeatureType.LargeZoneLowGrayLevelEmphasis+":"+f.calculate(GLSZMFeatureType.LargeZoneLowGrayLevelEmphasis.id()));//OK
		System.out.println(GLSZMFeatureType.LargeZoneHighGrayLevelEmphasis+":"+f.calculate(GLSZMFeatureType.LargeZoneHighGrayLevelEmphasis.id()));//OK
		System.out.println(GLSZMFeatureType.GrayLevelNonUniformity+":"+f.calculate(GLSZMFeatureType.GrayLevelNonUniformity.id()));//OK
		System.out.println(GLSZMFeatureType.GrayLevelNonUniformityNormalized+":"+f.calculate(GLSZMFeatureType.GrayLevelNonUniformityNormalized.id()));//OK
		System.out.println(GLSZMFeatureType.SizeZoneNonUniformity+":"+f.calculate(GLSZMFeatureType.SizeZoneNonUniformity.id()));//OK
		System.out.println(GLSZMFeatureType.SizeZoneNonUniformityNormalized+":"+f.calculate(GLSZMFeatureType.SizeZoneNonUniformityNormalized.id()));//OK
		System.out.println(GLSZMFeatureType.ZonePercentage+":"+f.calculate(GLSZMFeatureType.ZonePercentage.id()));//OK
		System.out.println(GLSZMFeatureType.GrayLevelVariance+":"+f.calculate(GLSZMFeatureType.GrayLevelVariance.id()));//OK
		System.out.println(GLSZMFeatureType.ZoneSizeVariance+":"+f.calculate(GLSZMFeatureType.ZoneSizeVariance.id()));//OK
		System.out.println(GLSZMFeatureType.ZoneSizeEntropy+":"+f.calculate(GLSZMFeatureType.ZoneSizeEntropy.id()));//OK
	}
}
