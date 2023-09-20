package radiomics;

import com.vis.radiomics.features.*;
import com.vis.radiomics.main.RadiomicsJ;
import com.vis.radiomics.main.TestDataLoader;
import com.vis.radiomics.main.Utils;

import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

public class TestNGLDMFeatures {
	
	public static void main(String[] args) throws Exception {
//		checkMatrix();
		checkFeatures();
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

		byte roi_mask[] = new byte[16];
		byte m0[] = new byte[] { 0, 0, 0, 0 };
		byte m1[] = new byte[] { 0, 1, 1, 0 };
		byte m2[] = new byte[] { 0, 1, 1, 0 };
		byte m3[] = new byte[] { 0, 0, 0, 0 };
		i= 0;
		for(byte[] r: new byte[][] {m0,m1,m2,m3}) {
			for(byte v:r) {
				roi_mask[i++] = v;
			}
		}
		ImageProcessor bp2 = new ByteProcessor(4, 4, roi_mask);
		ImagePlus mask = new ImagePlus("sample_mask", bp2);
		int alpha = 0;
		int delta = 1;

		NGLDMFeatures f = null;
		try {
			f = new NGLDMFeatures(imp/*descretised*/, mask, RadiomicsJ.targetLabel, alpha, delta);
		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println(f.toString());

	}
	
	static void checkFeatures() throws Exception {
		ImagePlus ds_pair[] = TestDataLoader.digital_phantom1();
		ImagePlus imp = ds_pair[0];
		ImagePlus mask = ds_pair[1];
		
		RadiomicsJ.targetLabel = 1;
		int nBins = Utils.getNumOfBinsByMinMaxRange(imp, mask, RadiomicsJ.targetLabel);
		
		int alpha = 0;
		int delta = 1;
		
		//ImagePlus img, ImagePlus mask, int label, Integer alpha, Integer delta, boolean useBinCount, Integer nBins, Double binWidth
		NGLDMFeatures f = new NGLDMFeatures(imp, mask ,RadiomicsJ.targetLabel, alpha, delta, true, nBins, null);
		System.out.println(f.toString());
		
		System.out.println(NGLDMFeatureType.LowDependenceEmphasis+":"+f.calculate(NGLDMFeatureType.LowDependenceEmphasis.id()));//OK
		System.out.println(NGLDMFeatureType.HighDependenceEmphasis+":"+f.calculate(NGLDMFeatureType.HighDependenceEmphasis.id()));//OK
		System.out.println(NGLDMFeatureType.LowGrayLevelCountEmphasis+":"+f.calculate(NGLDMFeatureType.LowGrayLevelCountEmphasis.id()));//OK
		System.out.println(NGLDMFeatureType.HighGrayLevelCountEmphasis+":"+f.calculate(NGLDMFeatureType.HighGrayLevelCountEmphasis.id()));//OK
		System.out.println(NGLDMFeatureType.LowDependenceLowGrayLevelEmphasis+":"+f.calculate(NGLDMFeatureType.LowDependenceLowGrayLevelEmphasis.id()));//ok
		System.out.println(NGLDMFeatureType.LowDependenceHighGrayLevelEmphasis+":"+f.calculate(NGLDMFeatureType.LowDependenceHighGrayLevelEmphasis.id()));//ok
		System.out.println(NGLDMFeatureType.HighDependenceLowGrayLevelEmphasis+":"+f.calculate(NGLDMFeatureType.HighDependenceLowGrayLevelEmphasis.id()));//ok
		System.out.println(NGLDMFeatureType.HighDependenceHighGrayLevelEmphasis+":"+f.calculate(NGLDMFeatureType.HighDependenceHighGrayLevelEmphasis.id()));//ok
		System.out.println(NGLDMFeatureType.GrayLevelNonUniformity+":"+f.calculate(NGLDMFeatureType.GrayLevelNonUniformity.id()));//ok
		System.out.println(NGLDMFeatureType.GrayLevelNonUniformityNormalized+":"+f.calculate(NGLDMFeatureType.GrayLevelNonUniformityNormalized.id()));//ok
		System.out.println(NGLDMFeatureType.DependenceCountNonUniformity+":"+f.calculate(NGLDMFeatureType.DependenceCountNonUniformity.id()));//ok
		System.out.println(NGLDMFeatureType.DependenceCountNonUniformityNormalized+":"+f.calculate(NGLDMFeatureType.DependenceCountNonUniformityNormalized.id()));//ok
		System.out.println(NGLDMFeatureType.DependenceCountPercentage+":"+f.calculate(NGLDMFeatureType.DependenceCountPercentage.id()));//ok
		System.out.println(NGLDMFeatureType.GrayLevelVariance+":"+f.calculate(NGLDMFeatureType.GrayLevelVariance.id()));//ok
		System.out.println(NGLDMFeatureType.DependenceCountVariance+":"+f.calculate(NGLDMFeatureType.DependenceCountVariance.id()));//ok
		System.out.println(NGLDMFeatureType.DependenceCountEntropy+":"+f.calculate(NGLDMFeatureType.DependenceCountEntropy.id()));//ok
		System.out.println(NGLDMFeatureType.DependenceCountEnergy+":"+f.calculate(NGLDMFeatureType.DependenceCountEnergy.id()));//ok
		System.exit(0);
	}
}
