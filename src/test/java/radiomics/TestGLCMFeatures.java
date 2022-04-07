package radiomics;

import ij.ImagePlus;
import RadiomicsJ.*;

public class TestGLCMFeatures {
	
	public static void main(String[] args) throws Exception {
		ImagePlus ds_pair[] = TestDataLoader.digital_phantom1();
		ImagePlus imp = ds_pair[0];
		ImagePlus mask = ds_pair[1];
		
		RadiomicsJ.targetLabel = 1;
		
		GLCMFeatures f = new GLCMFeatures(imp, mask, 1 ,1,true,6,null,null);
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
		System.out.println(f.calculate(GLCMFeatureType.JointMaximum.id()));//OK
		System.out.println(f.calculate(GLCMFeatureType.JointAverage.id()));//OK
		System.out.println(f.calculate(GLCMFeatureType.JointVariance.id()));//OK
		System.out.println(f.calculate(GLCMFeatureType.JointEntropy.id()));//OK
		System.out.println(f.calculate(GLCMFeatureType.DifferenceAverage.id()));//OK
		System.out.println(f.calculate(GLCMFeatureType.DifferenceVariance.id()));//OK
		System.out.println(f.calculate(GLCMFeatureType.DifferenceEntropy.id()));//OK
		System.out.println(f.calculate(GLCMFeatureType.SumAverage.id()));//OK
		System.out.println(f.calculate(GLCMFeatureType.SumVariance.id()));//OK
		System.out.println(f.calculate(GLCMFeatureType.SumEntropy.id()));//OK
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
}
