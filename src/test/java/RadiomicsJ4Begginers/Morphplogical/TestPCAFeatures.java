package RadiomicsJ4Begginers.Morphplogical;

import com.vis.radiomics.features.MorphologicalFeatureType;
import com.vis.radiomics.features.MorphologicalFeatures;
import com.vis.radiomics.main.TestDataLoader;

import ij.ImagePlus;

public class TestPCAFeatures {

	public static void main(String[] args) {
		ImagePlus[] imgAndMask = TestDataLoader.digital_phantom1();
		MorphologicalFeatures molph = new MorphologicalFeatures(imgAndMask[0], imgAndMask[1], 1);
		
		Double major = molph.calculate(MorphologicalFeatureType.MajorAxisLength.id());
		Double minor = molph.calculate(MorphologicalFeatureType.MinorAxisLength.id());
		Double least = molph.calculate(MorphologicalFeatureType.LeastAxisLength.id());
		Double elong = molph.calculate(MorphologicalFeatureType.Elongation.id());
		Double flat = molph.calculate(MorphologicalFeatureType.Flatness.id());
		Double v_aee = molph.calculate(MorphologicalFeatureType.VolumeDensity_ApproximateEnclosingEllipsoid.id());
		Double a_aee = molph.calculate(MorphologicalFeatureType.AreaDensity_ApproximateEnclosingEllipsoid.id());
		System.out.println(MorphologicalFeatureType.MajorAxisLength+":" + major);
		System.out.println(MorphologicalFeatureType.MinorAxisLength+":" + minor);
		System.out.println(MorphologicalFeatureType.LeastAxisLength+":" + least);
		System.out.println(MorphologicalFeatureType.Elongation+":" + elong);
		System.out.println(MorphologicalFeatureType.Flatness+":" + flat);
		System.out.println(MorphologicalFeatureType.VolumeDensity_ApproximateEnclosingEllipsoid+":" + v_aee);
		System.out.println(MorphologicalFeatureType.AreaDensity_ApproximateEnclosingEllipsoid+":" + a_aee);
	}

}
