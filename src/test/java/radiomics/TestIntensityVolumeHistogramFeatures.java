package radiomics;

import ij.ImagePlus;
import RadiomicsJ.*;

public class TestIntensityVolumeHistogramFeatures {
	
	public static void main(String[] args) throws Exception {
		ImagePlus ds_pair[] = TestDataLoader.digital_phantom1();
		ImagePlus imp = ds_pair[0];
		ImagePlus mask = ds_pair[1];
		
		RadiomicsJ.targetLabel = 1;
		RadiomicsJ.nBins = Utils.getNumOfBinsByMinMaxRange(imp, mask, RadiomicsJ.targetLabel);//digital phantom  of roi, max.
		RadiomicsJ.debug = true;
		
		IntensityVolumeHistogramFeatures ivhf = new IntensityVolumeHistogramFeatures(imp, mask, 1, 0);
		
//		System.out.println(ivhf.calculate(IntensityVolumeHistogramFeatureType.VolumeAtIntensityFraction10.id()));//OK
//		System.out.println(ivhf.calculate(IntensityVolumeHistogramFeatureType.VolumeAtIntensityFraction90.id()));//OK
//		System.out.println(ivhf.calculate(IntensityVolumeHistogramFeatureType.IntensityAtVolumeFraction10.id()));//OK
//		System.out.println(ivhf.calculate(IntensityVolumeHistogramFeatureType.IntensityAtVolumeFraction90.id()));//OK
//		System.out.println(ivhf.calculate(IntensityVolumeHistogramFeatureType.VolumeFractionDifferenceBetweenIntensityFractions.id()));//OK
//		System.out.println(ivhf.calculate(IntensityVolumeHistogramFeatureType.IntensityFractionDifferenceBetweenVolumeFractions.id()));//OK
		System.out.println(ivhf.calculate(IntensityVolumeHistogramFeatureType.AreaUnderTheIVHCurve.id()));//OK

		System.exit(0);
	}
}
