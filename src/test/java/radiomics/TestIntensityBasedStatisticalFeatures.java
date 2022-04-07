package radiomics;

import ij.ImagePlus;
import RadiomicsJ.*;

public class TestIntensityBasedStatisticalFeatures {
	
	public static void main(String[] args) {
		ImagePlus ds_pair[] = TestDataLoader.digital_phantom1();
		ImagePlus imp = ds_pair[0];
		ImagePlus mask = ds_pair[1];
		
		RadiomicsJ.targetLabel = 1;
		
		IntensityBasedStatisticalFeatures ibsf = new IntensityBasedStatisticalFeatures(imp, mask, RadiomicsJ.targetLabel);
		System.out.println(ibsf.calculate(IntensityBasedStatisticalFeatureType.Mean.id()));//OK
		System.out.println(ibsf.calculate(IntensityBasedStatisticalFeatureType.Variance.id()));//OK
		System.out.println(ibsf.calculate(IntensityBasedStatisticalFeatureType.Skewness.id()));//OK
		System.out.println(ibsf.calculate(IntensityBasedStatisticalFeatureType.Kurtosis.id())-3);//OK, 3.3.4 (Excess) intensity kurtosis
		System.out.println(ibsf.calculate(IntensityBasedStatisticalFeatureType.Median.id()));//ok
		System.out.println(ibsf.calculate(IntensityBasedStatisticalFeatureType.Minimum.id()));//OK
		System.out.println(ibsf.calculate(IntensityBasedStatisticalFeatureType.Percentile10.id()));//OK
		System.out.println(ibsf.calculate(IntensityBasedStatisticalFeatureType.Percentile90.id()));//OK
		System.out.println(ibsf.calculate(IntensityBasedStatisticalFeatureType.Maximum.id()));//OK
		System.out.println(ibsf.calculate(IntensityBasedStatisticalFeatureType.Interquartile.id()));//OK
		System.out.println(ibsf.calculate(IntensityBasedStatisticalFeatureType.Range.id()));//ok
		System.out.println(ibsf.calculate(IntensityBasedStatisticalFeatureType.MeanAbsoluteDeviation.id()));
		System.out.println(ibsf.calculate(IntensityBasedStatisticalFeatureType.RobustMeanAbsoluteDeviation.id()));
		System.out.println(ibsf.calculate(IntensityBasedStatisticalFeatureType.MedianAbsoluteDeviation.id()));
		System.out.println(ibsf.calculate(IntensityBasedStatisticalFeatureType.CoefficientOfVariation.id()));
		System.out.println(ibsf.calculate(IntensityBasedStatisticalFeatureType.QuartileCoefficientOfDispersion.id()));
		System.out.println(ibsf.calculate(IntensityBasedStatisticalFeatureType.Energy.id()));//ok
		System.out.println(ibsf.calculate(IntensityBasedStatisticalFeatureType.RootMeanSquared.id()));//ok
		
		System.exit(0);
	}
}
