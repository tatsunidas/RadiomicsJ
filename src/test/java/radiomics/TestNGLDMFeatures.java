package radiomics;

import ij.ImagePlus;
import RadiomicsJ.*;

public class TestNGLDMFeatures {
	
	public static void main(String[] args) throws Exception {
		ImagePlus ds_pair[] = TestDataLoader.digital_phantom1();
		ImagePlus imp = ds_pair[0];
		ImagePlus mask = ds_pair[1];
		
		RadiomicsJ.targetLabel = 1;
		
		//ImagePlus img, ImagePlus mask, int label, Integer alpha, Integer delta, boolean useBinCount, Integer nBins, Double binWidth
		NGLDMFeatures f = new NGLDMFeatures(imp, mask ,RadiomicsJ.targetLabel, 0, 1, true, 6, null);
//		System.out.println(f.toString());
		
		System.out.println(f.calculate(NGLDMFeatureType.LowDependenceEmphasis.id()));//OK
		System.out.println(f.calculate(NGLDMFeatureType.HighDependenceEmphasis.id()));//OK
		System.out.println(f.calculate(NGLDMFeatureType.LowGrayLevelCountEmphasis.id()));//OK
		System.out.println(f.calculate(NGLDMFeatureType.HighGrayLevelCountEmphasis.id()));//OK
		System.out.println(f.calculate(NGLDMFeatureType.LowDependenceLowGrayLevelEmphasis.id()));//ok
		System.out.println(f.calculate(NGLDMFeatureType.LowDependenceHighGrayLevelEmphasis.id()));//ok
		System.out.println(f.calculate(NGLDMFeatureType.HighDependenceLowGrayLevelEmphasis.id()));//ok
		System.out.println(f.calculate(NGLDMFeatureType.HighDependenceHighGrayLevelEmphasis.id()));//ok
		System.out.println(f.calculate(NGLDMFeatureType.GrayLevelNonUniformity.id()));//ok
		System.out.println(f.calculate(NGLDMFeatureType.GrayLevelNonUniformityNormalized.id()));//ok
		System.out.println(f.calculate(NGLDMFeatureType.DependenceCountNonUniformity.id()));//ok
		System.out.println(f.calculate(NGLDMFeatureType.DependenceCountNonUniformityNormalized.id()));//ok
		System.out.println(f.calculate(NGLDMFeatureType.DependenceCountPercentage.id()));//ok
		System.out.println(f.calculate(NGLDMFeatureType.GrayLevelVariance.id()));//ok
		System.out.println(f.calculate(NGLDMFeatureType.DependenceCountVariance.id()));//ok
		System.out.println(f.calculate(NGLDMFeatureType.DependenceCountEntropy.id()));//ok
		System.out.println(f.calculate(NGLDMFeatureType.DependenceCountEnergy.id()));//ok
		System.exit(0);
	}
	
	/*
	 * ATTENTION !! 
	 * this cannot reproduce. 
	 * Because difinition is not the same about valid count neighbours. 
	 * 
	 * see, Table 3.162 in IBSI. 
	 * 
	 * Original image with grey levels and pixels with a complete neighbourhood within the square (a);
	 * corresponding neighbouring grey level dependence matrix for distance d = √ 2
	 * and coarseness parameter a = 0 (b). Element s(i, j) of the NGLDM indicates
	 * the number of neighbourhoods with a center pixel with grey level i and
	 * neighbouring grey level dependence k within the image. 
	 * 
	 * here ;
	 * Note that in our definition a complete neighbourhood is no longer required.
	 * Thus every voxel is considered as a center voxel with a neighbourhood, instead of being
	 * constrained to the voxels within the square in panel (a).
	 * 
	 */
	public static void testIBSI() throws Exception {
		
		// ibsi
		Integer pixels[][] = new Integer[4][];
		Integer r0[] = new Integer[] { 1, 2, 2, 3 };
		Integer r1[] = new Integer[] { 1, 2, 3, 3 };
		Integer r2[] = new Integer[] { 4, 2, 4, 1 };
		Integer r3[] = new Integer[] { 4, 1, 2, 3 };
		pixels[0] = r0;
		pixels[1] = r1;
		pixels[2] = r2;
		pixels[3] = r3;
		byte[] pixelsByte = new byte[4 * 4];
		int num = 0;
		for (Integer[] r : pixels) {
			for (Integer p : r) {
				pixelsByte[num++] = Byte.valueOf(String.valueOf(p));
			}
		}

		Integer mask[][] = new Integer[4][];
		Integer m0[] = new Integer[] { 0, 0, 0, 0 };
		Integer m1[] = new Integer[] { 0, 1, 1, 0 };
		Integer m2[] = new Integer[] { 0, 1, 1, 0 };
		Integer m3[] = new Integer[] { 0, 0, 0, 0 };
		mask[0] = m0;
		mask[1] = m1;
		mask[2] = m2;
		mask[3] = m3;
		byte[] maskByte = new byte[4 * 4];
		num = 0;
		for (Integer[] m : mask) {
			for (Integer p : m) {
				maskByte[num++] = Byte.valueOf(String.valueOf(p));
			}
		}
		RadiomicsJ.targetLabel = 1;
//		int nBins = 4;// 1 to 4
//		NGLDMFeatures test = new NGLDMFeatures(
//				new ImagePlus("test-2d", new ByteProcessor(pixels[0].length, pixels.length, pixelsByte)),
//				new ImagePlus("mask-2d", new ByteProcessor(pixels[0].length, pixels.length, maskByte)), 
//				null,
//				null,
//				nBins);
//		System.out.println(test.toString());
	}
}
