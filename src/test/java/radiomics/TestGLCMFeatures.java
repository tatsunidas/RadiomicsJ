package radiomics;

import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import io.github.tatsunidas.radiomics.features.*;
import io.github.tatsunidas.radiomics.main.RadiomicsJ;
import io.github.tatsunidas.radiomics.main.TestDataLoader;
import io.github.tatsunidas.radiomics.main.Utils;

public class TestGLCMFeatures {
	
	public static void main(String[] args) throws Exception {
		
//		checkMatrix();
		testFeatures();
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
		int delta = 1;//distance
		
		//init
		GLCMFeatures test = null;
		try {
			test = new GLCMFeatures(imp, null, 1,delta, true, nBins, null, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// angle new int[] {0,0,1} is ordered by z,y,x (dim2, dim1, dim0)
		double[][] mat_0 = test.calcGLCM2(Utils.getAngleVectorKey(new int[] { 0, 0, 1 }), new int[] { 0, 0, 1 }, delta);
		System.out.println("Check : (x,y,z)(1,0,0) vector →");
		System.out.println(test.toString(mat_0));
		double[][] mat_45 = test.calcGLCM2(Utils.getAngleVectorKey(new int[] { 0, 1, 1 }), new int[] { 0, 1, 1 }, delta);
		System.out.println("Check : (x,y,z)(1,1,0) vector ↗");
		System.out.println(test.toString(mat_45));
		double[][] mat_90 = test.calcGLCM2(Utils.getAngleVectorKey(new int[] { 0, 1, 0 }), new int[] { 0, 1, 0 }, delta);
		System.out.println("Check : (x,y,z)(0,1,0) vector ↑");
		System.out.println(test.toString(mat_90));
		double[][] mat_135 = test.calcGLCM2(Utils.getAngleVectorKey(new int[] { 0, 1, -1 }), new int[] { 0, 1, -1 }, delta);
		System.out.println("Check : (x,y,z)(-1,1,0) vector ↖");
		System.out.println(test.toString(mat_135));
		
		System.out.println("Normalized distribution at 0 degree");
		double[][] mat_norm = test.normalize(mat_0);
		System.out.println(test.toString(mat_norm));
		
	}
	
	public static void testFeatures() {
		ImagePlus ds_pair[] = TestDataLoader.digital_phantom1();
		ImagePlus imp = ds_pair[0];
		ImagePlus mask = ds_pair[1];
		
		RadiomicsJ.targetLabel = 1;
		
		GLCMFeatures glcm = null;
		try {
			glcm = new GLCMFeatures(imp, mask, 1 ,1,true,6,null,null);
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println(GLCMFeatureType.JointMaximum+":"+glcm.calculate(GLCMFeatureType.JointMaximum.id()));//OK
		System.out.println(GLCMFeatureType.JointAverage+":"+glcm.calculate(GLCMFeatureType.JointAverage.id()));//OK
		System.out.println(GLCMFeatureType.JointVariance+":"+glcm.calculate(GLCMFeatureType.JointVariance.id()));//OK
		System.out.println(GLCMFeatureType.JointEntropy+":"+glcm.calculate(GLCMFeatureType.JointEntropy.id()));//OK
		System.out.println(GLCMFeatureType.DifferenceAverage+":"+glcm.calculate(GLCMFeatureType.DifferenceAverage.id()));//OK
		System.out.println(GLCMFeatureType.DifferenceVariance+":"+glcm.calculate(GLCMFeatureType.DifferenceVariance.id()));//OK
		System.out.println(GLCMFeatureType.DifferenceEntropy+":"+glcm.calculate(GLCMFeatureType.DifferenceEntropy.id()));//OK
		System.out.println(GLCMFeatureType.SumAverage+":"+glcm.calculate(GLCMFeatureType.SumAverage.id()));//OK
		System.out.println(GLCMFeatureType.SumVariance+":"+glcm.calculate(GLCMFeatureType.SumVariance.id()));//OK
		System.out.println(GLCMFeatureType.SumEntropy+":"+glcm.calculate(GLCMFeatureType.SumEntropy.id()));//OK
		System.out.println(GLCMFeatureType.AngularSecondMoment+":"+glcm.calculate(GLCMFeatureType.AngularSecondMoment.id()));//OK
		System.out.println(GLCMFeatureType.Contrast+":"+glcm.calculate(GLCMFeatureType.Contrast.id()));//OK
		System.out.println(GLCMFeatureType.Dissimilarity+":"+glcm.calculate(GLCMFeatureType.Dissimilarity.id()));//OK
		System.out.println(GLCMFeatureType.InverseDifference+":"+glcm.calculate(GLCMFeatureType.InverseDifference.id()));//OK
		System.out.println(GLCMFeatureType.NormalizedInverseDifference+":"+glcm.calculate(GLCMFeatureType.NormalizedInverseDifference.id()));//OK
		System.out.println(GLCMFeatureType.InverseDifferenceMoment+":"+glcm.calculate(GLCMFeatureType.InverseDifferenceMoment.id()));//OK
		System.out.println(GLCMFeatureType.NormalizedInverseDifferenceMoment+":"+glcm.calculate(GLCMFeatureType.NormalizedInverseDifferenceMoment.id()));//OK
		System.out.println(GLCMFeatureType.InverseVariance+":"+glcm.calculate(GLCMFeatureType.InverseVariance.id()));//OK
		System.out.println(GLCMFeatureType.Correlation+":"+glcm.calculate(GLCMFeatureType.Correlation.id()));//OK
		System.out.println(GLCMFeatureType.Autocorrection+":"+glcm.calculate(GLCMFeatureType.Autocorrection.id()));//OK
		System.out.println(GLCMFeatureType.ClusterTendency+":"+glcm.calculate(GLCMFeatureType.ClusterTendency.id()));//OK
		System.out.println(GLCMFeatureType.ClusterShade+":"+glcm.calculate(GLCMFeatureType.ClusterShade.id()));//OK
		System.out.println(GLCMFeatureType.ClusterProminence+":"+glcm.calculate(GLCMFeatureType.ClusterProminence.id()));//OK
		System.out.println(GLCMFeatureType.InformationalMeasureOfCorrelation1+":"+glcm.calculate(GLCMFeatureType.InformationalMeasureOfCorrelation1.id()));//OK
		System.out.println(GLCMFeatureType.InformationalMeasureOfCorrelation2+":"+glcm.calculate(GLCMFeatureType.InformationalMeasureOfCorrelation2.id()));
	}
	
}
