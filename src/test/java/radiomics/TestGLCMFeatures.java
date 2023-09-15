package radiomics;

import com.vis.radiomics.features.*;
import com.vis.radiomics.main.RadiomicsJ;
import com.vis.radiomics.main.TestDataLoader;

import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

public class TestGLCMFeatures {
	
	public static void main(String[] args) throws Exception {
		
		checkMatrix();
		
//		ImagePlus ds_pair[] = TestDataLoader.digital_phantom1();
//		ImagePlus imp = ds_pair[0];
//		ImagePlus mask = ds_pair[1];
//		
//		RadiomicsJ.targetLabel = 1;
//		
//		GLCMFeatures f = new GLCMFeatures(imp, mask, 1 ,1,true,6,null,null);
		/*
			DifferenceAverage(4),
			DifferenceVariance(5),
			DifferenceEntropy(6),
			SumAverage(7),
			SumVariance(8),
			SumEntropy(9),
			AngularSecondMoment(10),
			Contrast(11),
			Dissimilarity(12),
			InverseDifference(13),
			NormalizedInverseDifference(14),
			InverseDifferenceMoment(15),
			NormalizedInverseDifferenceMoment(16),
			InverseVariance(17),
			Correlation(18),
			Autocorrection(19),
			ClusterTendency(20),
			ClusterShade(21),
			ClusterProminence(22),
			InformationalMeasureOfCorrelation1(23),
			InformationalMeasureOfCorrelation2(24)
		 */
//		System.out.println(f.calculate(GLCMFeatureType.JointMaximum.id()));//OK
//		System.out.println(f.calculate(GLCMFeatureType.JointAverage.id()));//OK
//		System.out.println(f.calculate(GLCMFeatureType.JointVariance.id()));//OK
//		System.out.println(f.calculate(GLCMFeatureType.JointEntropy.id()));//OK
//		System.out.println(f.calculate(GLCMFeatureType.DifferenceAverage.id()));//OK
//		System.out.println(f.calculate(GLCMFeatureType.DifferenceVariance.id()));//OK
//		System.out.println(f.calculate(GLCMFeatureType.DifferenceEntropy.id()));//OK
//		System.out.println(f.calculate(GLCMFeatureType.SumAverage.id()));//OK
//		System.out.println(f.calculate(GLCMFeatureType.SumVariance.id()));//OK
//		System.out.println(f.calculate(GLCMFeatureType.SumEntropy.id()));//OK
//		System.out.println(f.calculate(RadiomicsJ.GLCMFeatureTypes.AngularSecondMoment.id()));//OK
//		System.out.println(f.calculate(RadiomicsJ.GLCMFeatureTypes.Contrast.id()));//OK
//		System.out.println(f.calculate(RadiomicsJ.GLCMFeatureTypes.Dissimilarity.id()));//OK
//		System.out.println(f.calculate(RadiomicsJ.GLCMFeatureTypes.InverseDifference.id()));//OK
//		System.out.println(f.calculate(RadiomicsJ.GLCMFeatureTypes.NormalizedInverseDifference.id()));//OK
//		System.out.println(f.calculate(RadiomicsJ.GLCMFeatureTypes.InverseDifferenceMoment.id()));//OK
//		System.out.println(f.calculate(RadiomicsJ.GLCMFeatureTypes.NormalizedInverseDifferenceMoment.id()));//OK
//		System.out.println(f.calculate(RadiomicsJ.GLCMFeatureTypes.InverseVariance.id()));//OK
//		System.out.println(f.calculate(RadiomicsJ.GLCMFeatureTypes.Correlation.id()));//OK
//		System.out.println(f.calculate(RadiomicsJ.GLCMFeatureTypes.Autocorrection.id()));//OK
//		System.out.println(f.calculate(RadiomicsJ.GLCMFeatureTypes.ClusterTendency.id()));//OK
//		System.out.println(f.calculate(RadiomicsJ.GLCMFeatureTypes.ClusterShade.id()));//OK
//		System.out.println(f.calculate(RadiomicsJ.GLCMFeatureTypes.ClusterProminence.id()));//OK
//		System.out.println(f.calculate(RadiomicsJ.GLCMFeatureTypes.InformationalMeasureOfCorrelation1.id()));//OK
//		System.out.println(f.calculate(RadiomicsJ.GLCMFeatureTypes.InformationalMeasureOfCorrelation2.id()));//OK
		System.exit(0);
	}
	
	public static void checkMatrix() {
		byte pixels[] = new byte[16];
		byte r0[] = new byte[] { 1, 2, 2, 3 };
		byte r1[] = new byte[] { 1, 2, 3, 3 };
		byte r2[] = new byte[] { 4, 2, 4, 1 };
		byte r3[] = new byte[] { 4, 1, 2, 3 };
		int i= 0;
		for(byte[] r: new byte[][] {r0,r1,r2,r3}) {
			for(byte v:r) {
				pixels[i++] = v;
			}
		}
		ImageProcessor bp = new ByteProcessor(4, 4, pixels);
		ImagePlus imp = new ImagePlus("sample", bp);
		int nBins = 4;// 1 to 4
		int delta = 1;
		
		GLCMFeatures test = null;
		try {
			test = new GLCMFeatures(imp, null, 1,delta, true, nBins, null, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// angle new int[] {0,0,1} is ordered by z,y,x (dim2, dim1, dim0)
		double[][] mat_0 = test.calcGLCM2(0, new int[] { 0, 0, 1 }, delta);
		System.out.println("Check : (x,y,z)(1,0,0) vector →");
		System.out.println(test.toString(mat_0));
		double[][] mat_45 = test.calcGLCM2(0, new int[] { 0, 1, 1 }, delta);
		System.out.println("Check : (x,y,z)(1,1,0) vector ↗");
		System.out.println(test.toString(mat_45));
		double[][] mat_90 = test.calcGLCM2(0, new int[] { 0, 1, 0 }, delta);
		System.out.println("Check : (x,y,z)(0,1,0) vector ↑");
		System.out.println(test.toString(mat_90));
		double[][] mat_135 = test.calcGLCM2(0, new int[] { 0, 1, -1 }, delta);
		System.out.println("Check : (x,y,z)(-1,1,0) vector ↖");
		System.out.println(test.toString(mat_135));
		
		System.out.println("Normalized distribution at 0 degree");
		double[][] mat_norm = test.normalize(mat_0);
		System.out.println(test.toString(mat_norm));
		
		System.out.println(Math.pow(2, 3));
	}
	
}
