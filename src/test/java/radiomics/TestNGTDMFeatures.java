package radiomics;

import ij.ImagePlus;
import RadiomicsJ.*;

public class TestNGTDMFeatures {

	public static void main(String[] args) throws Exception {

//		testIBSI();
		testPhantom1();
		System.exit(0);
	}

	/**
	 * ATTENTION!!
	 * IBSI example of table 3.156 is not using valid neighbor count.
	 * 
	 * see, IBSI Table 3.156
	 * In our definition complete neighbourhood are no longer required. 
	 * In our definition the NGTDM would be calculated on the entire pixel area, and not solely on those pixels within the roi.//but this is strange !! can not reproduce digital phantom1 !.
	 * 
	 * In the testPhantom1();, I can completely reproduce results.
	 * 
	 * @throws Exception
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
//		NGTDMFeatures test = new NGTDMFeatures(
//				new ImagePlus("test-2d", new ByteProcessor(pixels[0].length, pixels.length, pixelsByte)),
//				new ImagePlus("mask-2d", new ByteProcessor(pixels[0].length, pixels.length, maskByte)), nBins, 1);
//		System.out.println(test.toString());
	}
	
	public static void testPhantom1() throws Exception {
		ImagePlus ds_pair[] = TestDataLoader.digital_phantom1();
		ImagePlus imp = ds_pair[0];
		ImagePlus mask = ds_pair[1];

		RadiomicsJ.targetLabel = 1;

		//ImagePlus img, ImagePlus mask, int label, Integer delta, boolean useBinCount, Integer nBins, Double binWidth
		NGTDMFeatures f = new NGTDMFeatures(imp, mask ,RadiomicsJ.targetLabel, 1, true, 6 ,null);
//		System.out.println(f.toString());
		
		System.out.println(f.calculate(NGTDMFeatureType.Coarseness.id()));//OK
		System.out.println(f.calculate(NGTDMFeatureType.Contrast.id()));//OK
		System.out.println(f.calculate(NGTDMFeatureType.Busyness.id()));//OK
		System.out.println(f.calculate(NGTDMFeatureType.Complexity.id()));//OK
		System.out.println(f.calculate(NGTDMFeatureType.Strength.id()));//OK
	}
}
