package radiomics;

import com.vis.radiomics.features.*;
import com.vis.radiomics.main.RadiomicsJ;
import com.vis.radiomics.main.TestDataLoader;
import com.vis.radiomics.main.Utils;

import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

public class TestGLRLMFeatures {
	
	public static void main(String[] args) throws Exception {
		
		checkMatrix();
//		checkFeatures();
		
		System.exit(0);
	}
	
	public static void checkMatrix() {
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
		GLRLMFeatures test =null;
		try {
			test = new GLRLMFeatures(
					imp,
					null,
					1,
					true,
					nBins,
					null,
					null);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// angle new int[] {0,0,1} is ordered by z,y,x (dim2, dim1, dim0)
		double[][] mat_0=null;
		double[][] mat_45=null;
		double[][] mat_90=null;
		double[][] mat_135=null;
		try {
			mat_0 = test.calcGLRLM(Utils.getAngleVectorKey(new int[] { 0, 0, 1 }), new int[] { 0, 0, 1 });
			mat_45 = test.calcGLRLM(Utils.getAngleVectorKey(new int[] { 0, 1, 1 }), new int[] { 0, 1, 1 });
			mat_90 = test.calcGLRLM(Utils.getAngleVectorKey(new int[] { 0, 1, 0 }), new int[] { 0, 1, 0 });
			mat_135 = test.calcGLRLM(Utils.getAngleVectorKey(new int[] { 0, 1, -1 }), new int[] { 0, 1, -1 });
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("Check : (x,y,z)(1,0,0) vector →");
		System.out.println(test.toString(mat_0));
		
		System.out.println("Check : (x,y,z)(1,1,0) vector ↗");
		System.out.println(test.toString(mat_45));
		
		System.out.println("Check : (x,y,z)(0,1,0) vector ↑");
		System.out.println(test.toString(mat_90));
		
		System.out.println("Check : (x,y,z)(-1,1,0) vector ↖");
		System.out.println(test.toString(mat_135));
		
		System.out.println("Normalized distribution at 0 degree");
		double[][] mat_norm = test.normalize(mat_0);
		System.out.println(test.toString(mat_norm));
		
	}
	
	static void checkFeatures() {
		ImagePlus ds_pair[] = TestDataLoader.digital_phantom1();
		ImagePlus imp = ds_pair[0];
		ImagePlus mask = ds_pair[1];
		
		RadiomicsJ.targetLabel = 1;
		
		GLRLMFeatures f = null;
		try {
			f = new GLRLMFeatures(
					imp,
					mask,
					1,
					true,
					Utils.getNumOfBinsByMinMaxRange(imp, mask, 1),
					null,
					null);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		System.out.println(GLRLMFeatureType.ShortRunEmphasis+":"+f.calculate(GLRLMFeatureType.ShortRunEmphasis.id()));//OK
		System.out.println(GLRLMFeatureType.LongRunEmphasis+":"+f.calculate(GLRLMFeatureType.LongRunEmphasis.id()));//OK
		System.out.println(GLRLMFeatureType.LowGrayLevelRunEmphasis+":"+f.calculate(GLRLMFeatureType.LowGrayLevelRunEmphasis.id()));//OK
		System.out.println(GLRLMFeatureType.HighGrayLevelRunEmphasis +":"+f.calculate(GLRLMFeatureType.HighGrayLevelRunEmphasis.id()));//OK
		System.out.println(GLRLMFeatureType.ShortRunLowGrayLevelEmphasis+":"+f.calculate(GLRLMFeatureType.ShortRunLowGrayLevelEmphasis.id()));//OK
		System.out.println(GLRLMFeatureType.ShortRunHighGrayLevelEmphasis+":"+f.calculate(GLRLMFeatureType.ShortRunHighGrayLevelEmphasis.id()));//OK
		System.out.println(GLRLMFeatureType.LongRunLowGrayLevelEmphasis+":"+f.calculate(GLRLMFeatureType.LongRunLowGrayLevelEmphasis.id()));//OK
		System.out.println(GLRLMFeatureType.LongRunHighGrayLevelEmphasis+":"+f.calculate(GLRLMFeatureType.LongRunHighGrayLevelEmphasis.id()));//OK
		System.out.println(GLRLMFeatureType.GrayLevelNonUniformity+":"+f.calculate(GLRLMFeatureType.GrayLevelNonUniformity.id()));//ok
		System.out.println(GLRLMFeatureType.GrayLevelNonUniformityNormalized+":"+f.calculate(GLRLMFeatureType.GrayLevelNonUniformityNormalized.id()));//OK
		System.out.println(GLRLMFeatureType.RunLengthNonUniformity+":"+f.calculate(GLRLMFeatureType.RunLengthNonUniformity.id()));//ok
		System.out.println(GLRLMFeatureType.RunLengthNonUniformityNormalized+":"+f.calculate(GLRLMFeatureType.RunLengthNonUniformityNormalized.id()));//OK
		System.out.println(GLRLMFeatureType.RunPercentage+":"+f.calculate(GLRLMFeatureType.RunPercentage.id()));//ok
		System.out.println(GLRLMFeatureType.GrayLevelVariance+":"+f.calculate(GLRLMFeatureType.GrayLevelVariance.id()));//OK
		System.out.println(GLRLMFeatureType.RunLengthVariance+":"+f.calculate(GLRLMFeatureType.RunLengthVariance.id()));//OK
		System.out.println(GLRLMFeatureType.RunEntropy+":"+f.calculate(GLRLMFeatureType.RunEntropy.id()));//ok
	}
}
