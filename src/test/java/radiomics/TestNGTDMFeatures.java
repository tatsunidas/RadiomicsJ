package radiomics;

import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import io.github.tatsunidas.radiomics.features.*;
import io.github.tatsunidas.radiomics.main.RadiomicsJ;
import io.github.tatsunidas.radiomics.main.TestDataLoader;

public class TestNGTDMFeatures {

	public static void main(String[] args) throws Exception {

//		testIBSI();
		testPhantom1();
		System.exit(0);
	}

	/**
	 * if you want reproduce IBSI Fig.18, use fillNGTDM_Amadasun
	 * 
	 * @throws Exception
	 */
	static void testIBSI() throws Exception {
		// ibsi
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
		
		RadiomicsJ.targetLabel = 1;
		int delta=1;
		NGTDMFeatures test = new NGTDMFeatures(imp/*descretized*/,mask,RadiomicsJ.targetLabel,delta);
		// re-calculate
		test.fillNGTDM(true);
		System.out.println(test.toString());
	}
	
	public static void testPhantom1() throws Exception {
		ImagePlus ds_pair[] = TestDataLoader.digital_phantom1();
		ImagePlus imp = ds_pair[0];
		ImagePlus mask = ds_pair[1];

		RadiomicsJ.targetLabel = 1;

		//ImagePlus img, ImagePlus mask, int label, Integer delta, boolean useBinCount, Integer nBins, Double binWidth
		NGTDMFeatures f = new NGTDMFeatures(imp, mask ,RadiomicsJ.targetLabel, 1, true, 6 ,null);
		System.out.println(f.toString());
		
		System.out.println(NGTDMFeatureType.Coarseness+":"+f.calculate(NGTDMFeatureType.Coarseness.id()));//OK
		System.out.println(NGTDMFeatureType.Contrast+":"+f.calculate(NGTDMFeatureType.Contrast.id()));//OK
		System.out.println(NGTDMFeatureType.Busyness+":"+f.calculate(NGTDMFeatureType.Busyness.id()));//OK
		System.out.println(NGTDMFeatureType.Complexity+":"+f.calculate(NGTDMFeatureType.Complexity.id()));//OK
		System.out.println(NGTDMFeatureType.Strength+":"+f.calculate(NGTDMFeatureType.Strength.id()));//OK
	}
}
