package radiomics;

import ij.ImagePlus;
import RadiomicsJ.*;

public class TestIntensityHistogramFeatures {
	
	public static void main(String[] args) throws Exception {
		ImagePlus ds_pair[] = TestDataLoader.digital_phantom1();
		ImagePlus imp = ds_pair[0];
		ImagePlus mask = ds_pair[1];
		
		RadiomicsJ.targetLabel = 1;
		RadiomicsJ.nBins = Utils.getNumOfBinsByMinMaxRange(imp, mask, RadiomicsJ.targetLabel);//digital phantom  of roi, max.
		
//		IntensityHistogramFeatures ihf = new IntensityHistogramFeatures(imp, mask, null);
		
//		System.out.println(ihf.calculate(RadiomicsJ.IntensityBasedStatisticalFeatureTypes.Mean.id()));//OK
//		System.out.println(ihf.calculate(RadiomicsJ.IntensityBasedStatisticalFeatureTypes.Variance.id()));//OK
//		System.out.println(ihf.calculate(RadiomicsJ.IntensityBasedStatisticalFeatureTypes.Skewness.id()));//OK
//		System.out.println(ihf.calculate(RadiomicsJ.IntensityBasedStatisticalFeatureTypes.Kurtosis.id())-3);//OK, 3.3.4 (Excess) intensity kurtosis
//		System.out.println(ihf.calculate(RadiomicsJ.IntensityBasedStatisticalFeatureTypes.Median.id()));//ok
//		System.out.println(ihf.calculate(RadiomicsJ.IntensityBasedStatisticalFeatureTypes.Minimum.id()));//OK
//		System.out.println(ihf.calculate(RadiomicsJ.IntensityBasedStatisticalFeatureTypes.Percentile10.id()));//OK
//		System.out.println(ihf.calculate(RadiomicsJ.IntensityBasedStatisticalFeatureTypes.Percentile90.id()));//OK
//		System.out.println(ihf.calculate(RadiomicsJ.IntensityBasedStatisticalFeatureTypes.Maximum.id()));//OK
//		System.out.println(ihf.calculate(RadiomicsJ.IntensityBasedStatisticalFeatureTypes.Mode.id()));//OK
//		System.out.println(ihf.calculate(RadiomicsJ.IntensityBasedStatisticalFeatureTypes.Interquartile.id()));//OK
//		System.out.println(ihf.calculate(RadiomicsJ.IntensityBasedStatisticalFeatureTypes.Range.id()));//ok
//		System.out.println(ihf.calculate(RadiomicsJ.IntensityBasedStatisticalFeatureTypes.MeanAbsoluteDeviation.id()));
//		System.out.println(ihf.calculate(RadiomicsJ.IntensityBasedStatisticalFeatureTypes.RobustMeanAbsoluteDeviation.id()));
//		System.out.println(ihf.calculate(RadiomicsJ.IntensityBasedStatisticalFeatureTypes.MedianAbsoluteDeviation.id()));
//		System.out.println(ihf.calculate(RadiomicsJ.IntensityBasedStatisticalFeatureTypes.CoefficientOfVariation.id()));
//		System.out.println(ihf.calculate(RadiomicsJ.IntensityBasedStatisticalFeatureTypes.QuartileCoefficientOfDispersion.id()));
//		System.out.println(ihf.calculate(RadiomicsJ.IntensityBasedStatisticalFeatureTypes.Entropy.id()));//ok
//		System.out.println(ihf.calculate(RadiomicsJ.IntensityBasedStatisticalFeatureTypes.Uniformity.id()));//or named energy
//		System.out.println(ihf.calculate(RadiomicsJ.IntensityBasedStatisticalFeatureTypes.MaximumHistogramGradient.id()));//ok
//		System.out.println(ihf.calculate(RadiomicsJ.IntensityBasedStatisticalFeatureTypes.MaximumHistogramGradientIntensity.id()));//ok
//		System.out.println(ihf.calculate(RadiomicsJ.IntensityBasedStatisticalFeatureTypes.MinimumHistogramGradient.id()));//ok
//		System.out.println(ihf.calculate(RadiomicsJ.IntensityBasedStatisticalFeatureTypes.MinimumHistogramGradientIntensity.id()));//ok
		
		System.exit(0);
	}
}
