package radiomics;

import com.vis.radiomics.features.*;
import com.vis.radiomics.main.RadiomicsJ;
import com.vis.radiomics.main.TestDataLoader;
import com.vis.radiomics.main.Utils;

import ij.ImagePlus;

public class TestIntensityVolumeHistogramFeatures {
	
	public static void main(String[] args) throws Exception {
		ImagePlus ds_pair[] = TestDataLoader.digital_phantom1();
		ImagePlus imp = ds_pair[0];
		ImagePlus mask = ds_pair[1];
		
		RadiomicsJ.targetLabel = 1;
		RadiomicsJ.nBins = Utils.getNumOfBinsByMinMaxRange(imp, mask, RadiomicsJ.targetLabel);//digital phantom  of roi, max.
		RadiomicsJ.debug = false;
		
		IntensityVolumeHistogramFeatures ivhf = new IntensityVolumeHistogramFeatures(imp, mask, 1, 0);
		
		ivhf.toString();//show IVH matrix
		
		System.out.println(IntensityVolumeHistogramFeatureType.VolumeAtIntensityFraction10+":"+ivhf.calculate(IntensityVolumeHistogramFeatureType.VolumeAtIntensityFraction10.id()));//OK
		System.out.println(IntensityVolumeHistogramFeatureType.VolumeAtIntensityFraction90+":"+ivhf.calculate(IntensityVolumeHistogramFeatureType.VolumeAtIntensityFraction90.id()));//OK
		System.out.println(IntensityVolumeHistogramFeatureType.IntensityAtVolumeFraction10+":"+ivhf.calculate(IntensityVolumeHistogramFeatureType.IntensityAtVolumeFraction10.id()));//OK
		System.out.println(IntensityVolumeHistogramFeatureType.IntensityAtVolumeFraction90+":"+ivhf.calculate(IntensityVolumeHistogramFeatureType.IntensityAtVolumeFraction90.id()));//OK
		System.out.println(IntensityVolumeHistogramFeatureType.VolumeFractionDifferenceBetweenIntensityFractions+":"+ivhf.calculate(IntensityVolumeHistogramFeatureType.VolumeFractionDifferenceBetweenIntensityFractions.id()));//OK
		System.out.println(IntensityVolumeHistogramFeatureType.IntensityFractionDifferenceBetweenVolumeFractions+":"+ivhf.calculate(IntensityVolumeHistogramFeatureType.IntensityFractionDifferenceBetweenVolumeFractions.id()));//OK
		System.out.println(IntensityVolumeHistogramFeatureType.AreaUnderTheIVHCurve+":"+ivhf.calculate(IntensityVolumeHistogramFeatureType.AreaUnderTheIVHCurve.id()));//OK

		System.exit(0);
	}
}
