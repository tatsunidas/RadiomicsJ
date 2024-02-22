package RadiomicsJ4Begginers.Morphplogical;

import ij.ImagePlus;
import io.github.tatsunidas.radiomics.features.MorphologicalFeatureType;
import io.github.tatsunidas.radiomics.features.MorphologicalFeatures;
import io.github.tatsunidas.radiomics.main.TestDataLoader;

public class TestVolumeAndAreaDensity {

	public static void main(String[] args) {
		ImagePlus[] imgAndMask = TestDataLoader.digital_phantom1();
		MorphologicalFeatures molph = new MorphologicalFeatures(imgAndMask[0], imgAndMask[1], 1);
		//volume
		Double v_aabb_frac = molph.calculate(MorphologicalFeatureType.VolumeDensity_AxisAlignedBoundingBox.id());
		Double v_aee_frac = molph.calculate(MorphologicalFeatureType.VolumeDensity_ApproximateEnclosingEllipsoid.id());
		Double v_convexhull_frac = molph.calculate(MorphologicalFeatureType.VolumeDensity_ConvexHull.id());
		//area
		Double a_aabb_frac = molph.calculate(MorphologicalFeatureType.AreaDensity_AxisAlignedBoundingBox.id());
		Double a_aee_frac = molph.calculate(MorphologicalFeatureType.AreaDensity_ApproximateEnclosingEllipsoid.id());
		Double a_convexhull_frac = molph.calculate(MorphologicalFeatureType.AreaDensity_ConvexHull.id());
		
		System.out.println("Volume densities; ");
		System.out.println(MorphologicalFeatureType.VolumeDensity_AxisAlignedBoundingBox +":"+ v_aabb_frac);
		System.out.println(MorphologicalFeatureType.VolumeDensity_ApproximateEnclosingEllipsoid +":"+ v_aee_frac);
		System.out.println(MorphologicalFeatureType.VolumeDensity_ConvexHull +":"+ v_convexhull_frac);
		System.out.println("Area densities ; ");
		System.out.println(MorphologicalFeatureType.AreaDensity_AxisAlignedBoundingBox +":"+ a_aabb_frac);
		System.out.println(MorphologicalFeatureType.AreaDensity_ApproximateEnclosingEllipsoid +":"+ a_aee_frac);
		System.out.println(MorphologicalFeatureType.AreaDensity_ConvexHull +":"+ a_convexhull_frac);
	}

}
