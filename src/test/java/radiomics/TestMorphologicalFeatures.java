package radiomics;

import RadiomicsJ.MorphologicalFeatureType;
import RadiomicsJ.MorphologicalFeatures;
import RadiomicsJ.RadiomicsJ;
import RadiomicsJ.TestDataLoader;
import ij.ImagePlus;

public class TestMorphologicalFeatures {

	public static void main(String[] args) {
		ImagePlus ds_pair[] = TestDataLoader.digital_phantom1();
//		ImagePlus ds_pair[] = TestDataLoader.sample_ct1();
		ImagePlus imp = ds_pair[0];
		ImagePlus mask = ds_pair[1];
		
		RadiomicsJ.targetLabel = 1;
		
		MorphologicalFeatures mf = new MorphologicalFeatures(imp, mask, RadiomicsJ.targetLabel);
		System.out.println(mf.calculate(MorphologicalFeatureType.VolumeMesh.id()));//ok
		System.out.println(mf.calculate(MorphologicalFeatureType.VolumeVoxelCounting.id()));//ok
		System.out.println(mf.calculate(MorphologicalFeatureType.VolumeMesh.id()));//ok
		System.out.println(mf.calculate(MorphologicalFeatureType.SurfaceToVolumeRatio.id()));//ok
		System.out.println(mf.calculate(MorphologicalFeatureType.Compactness1.id()));//ok
		System.out.println(mf.calculate(MorphologicalFeatureType.Compactness2.id()));//ok
		System.out.println(mf.calculate(MorphologicalFeatureType.SphericalDisproportion.id()));//ok
		System.out.println(mf.calculate(MorphologicalFeatureType.Sphericity.id()));//ok
		System.out.println(mf.calculate(MorphologicalFeatureType.Asphericity.id()));//ok
		System.out.println(mf.calculate(MorphologicalFeatureType.CentreOfMassShift.id()));//ok
		System.out.println(mf.calculate(MorphologicalFeatureType.Maximum3DDiameter.id()));//OK
		System.out.println(mf.calculate(MorphologicalFeatureType.MajorAxisLength.id()));//ok
		System.out.println(mf.calculate(MorphologicalFeatureType.MinorAxisLength.id()));//ok
		System.out.println(mf.calculate(MorphologicalFeatureType.LeastAxisLength.id()));//ok
		System.out.println(mf.calculate(MorphologicalFeatureType.Elongation.id()));//ok
		System.out.println(mf.calculate(MorphologicalFeatureType.Flatness.id()));//ok
		System.out.println(mf.calculate(MorphologicalFeatureType.VolumeDensity_AxisAlignedBoundingBox.id()));//ok
		System.out.println(mf.calculate(MorphologicalFeatureType.AreaDensity_AxisAlignedBoundingBox.id()));//ok
		System.out.println(mf.calculate(MorphologicalFeatureType.VolumeDensity_OrientedMinimumBoundingBox.id()));//dep
		System.out.println(mf.calculate(MorphologicalFeatureType.AreaDensity_OrientedMinimumBoundingBox.id()));//dep
		System.out.println(mf.calculate(MorphologicalFeatureType.VolumeDensity_ApproximateEnclosingEllipsoid.id()));//ok
		System.out.println(mf.calculate(MorphologicalFeatureType.AreaDensity_ApproximateEnclosingEllipsoid.id()));//ok
		System.out.println(mf.calculate(MorphologicalFeatureType.VolumeDensity_ConvexHull.id()));//allowable, more suitable mesh convex hull needed in future
		System.out.println(mf.calculate(MorphologicalFeatureType.AreaDensity_ConvexHull.id()));//allowable, more suitable mesh convex hull needed in future
		System.out.println(mf.calculate(MorphologicalFeatureType.IntegratedIntensity.id()));//OK
		System.out.println(mf.calculate(MorphologicalFeatureType.MoransIIndex.id()));//OK
		System.out.println(mf.calculate(MorphologicalFeatureType.GearysCMeasure.id()));//OK
//		System.exit(0);
	}

}
