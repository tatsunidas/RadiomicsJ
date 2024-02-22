package radiomics;

import ij.ImagePlus;
import io.github.tatsunidas.radiomics.features.*;
import io.github.tatsunidas.radiomics.main.RadiomicsJ;
import io.github.tatsunidas.radiomics.main.TestDataLoader;
import io.github.tatsunidas.radiomics.main.Utils;

public class TestIntensityHistogramFeatures {
	
	public static void main(String[] args) throws Exception {
		ImagePlus ds_pair[] = TestDataLoader.digital_phantom1();
		ImagePlus imp = ds_pair[0];
		ImagePlus mask = ds_pair[1];
		
		RadiomicsJ.targetLabel = 1;
		RadiomicsJ.nBins = Utils.getNumOfBinsByMinMaxRange(imp, mask, RadiomicsJ.targetLabel);//digital phantom  of roi, max.
		
		IntensityHistogramFeatures ihf = new IntensityHistogramFeatures(imp, mask, RadiomicsJ.targetLabel, true, RadiomicsJ.nBins, null);
		
		System.out.println(IntensityHistogramFeatureType.MeanDiscretisedIntensity+":"+ihf.calculate(IntensityHistogramFeatureType.MeanDiscretisedIntensity.id()));//OK
		System.out.println(IntensityHistogramFeatureType.Variance+":"+ihf.calculate(IntensityHistogramFeatureType.Variance.id()));//OK
		System.out.println(IntensityHistogramFeatureType.Skewness+":"+ihf.calculate(IntensityHistogramFeatureType.Skewness.id()));//OK
		System.out.println(IntensityHistogramFeatureType.Kurtosis+":"+(ihf.calculate(IntensityHistogramFeatureType.Kurtosis.id())-3));//OK, 3.3.4 (Excess) intensity kurtosis
		System.out.println(IntensityHistogramFeatureType.Median+":"+ihf.calculate(IntensityHistogramFeatureType.Median.id()));//ok
		System.out.println(IntensityHistogramFeatureType.Minimum+":"+ihf.calculate(IntensityHistogramFeatureType.Minimum.id()));//OK
		System.out.println(IntensityHistogramFeatureType.Percentile10+":"+ihf.calculate(IntensityHistogramFeatureType.Percentile10.id()));//OK
		System.out.println(IntensityHistogramFeatureType.Percentile90+":"+ihf.calculate(IntensityHistogramFeatureType.Percentile90.id()));//OK
		System.out.println(IntensityHistogramFeatureType.Maximum+":"+ihf.calculate(IntensityHistogramFeatureType.Maximum.id()));//OK
		System.out.println(IntensityHistogramFeatureType.Mode+":"+ihf.calculate(IntensityHistogramFeatureType.Mode.id()));//OK
		System.out.println(IntensityHistogramFeatureType.Interquartile+":"+ihf.calculate(IntensityHistogramFeatureType.Interquartile.id()));//OK
		System.out.println(IntensityHistogramFeatureType.Range+":"+ihf.calculate(IntensityHistogramFeatureType.Range.id()));//ok
		System.out.println(IntensityHistogramFeatureType.MeanAbsoluteDeviation+":"+ihf.calculate(IntensityHistogramFeatureType.MeanAbsoluteDeviation.id()));
		System.out.println(IntensityHistogramFeatureType.RobustMeanAbsoluteDeviation+":"+ihf.calculate(IntensityHistogramFeatureType.RobustMeanAbsoluteDeviation.id()));
		System.out.println(IntensityHistogramFeatureType.MedianAbsoluteDeviation+":"+ihf.calculate(IntensityHistogramFeatureType.MedianAbsoluteDeviation.id()));
		System.out.println(IntensityHistogramFeatureType.CoefficientOfVariation+":"+ihf.calculate(IntensityHistogramFeatureType.CoefficientOfVariation.id()));
		System.out.println(IntensityHistogramFeatureType.QuartileCoefficientOfDispersion+":"+ihf.calculate(IntensityHistogramFeatureType.QuartileCoefficientOfDispersion.id()));
		System.out.println(IntensityHistogramFeatureType.Entropy+":"+ihf.calculate(IntensityHistogramFeatureType.Entropy.id()));//ok
		System.out.println(IntensityHistogramFeatureType.Uniformity+":"+ihf.calculate(IntensityHistogramFeatureType.Uniformity.id()));//or named energy
		System.out.println(IntensityHistogramFeatureType.MaximumHistogramGradient+":"+ihf.calculate(IntensityHistogramFeatureType.MaximumHistogramGradient.id()));//ok
		System.out.println(IntensityHistogramFeatureType.MaximumHistogramGradientIntensity+":"+ihf.calculate(IntensityHistogramFeatureType.MaximumHistogramGradientIntensity.id()));//ok
		System.out.println(IntensityHistogramFeatureType.MinimumHistogramGradient+":"+ihf.calculate(IntensityHistogramFeatureType.MinimumHistogramGradient.id()));//ok
		System.out.println(IntensityHistogramFeatureType.MinimumHistogramGradientIntensity+":"+ihf.calculate(IntensityHistogramFeatureType.MinimumHistogramGradientIntensity.id()));//ok
		
		System.exit(0);
	}
}
