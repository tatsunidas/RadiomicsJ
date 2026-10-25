package io.github.tatsunidas.radiomics.features;

import ij.ImagePlus;

/**
 * Statistics outside IBSI, on the same intensity-mask voxels as the intensity based statistical features
 * (after resampling and re-segmentation, not discretised).
 */
public class AuxiliaryStatisticsFeatures extends IntensityBasedStatisticalFeatures {

	public AuxiliaryStatisticsFeatures(ImagePlus img, ImagePlus mask, int label) {
		super(img, mask, label);
	}

	public Double calculate(String id) {
		String name = AuxiliaryStatisticsFeatureType.findType(id);
		if (AuxiliaryStatisticsFeatureType.MeanOfPositiveIntensities.name().equals(name)) {
			return getMeanOfPositiveIntensities();
		}
		return null;
	}

	/**
	 * Mean of the voxels with value > 0. Zero is not positive. NaN when there is no positive voxel.
	 */
	public Double getMeanOfPositiveIntensities() {
		double sum = 0d;
		long n = 0;
		for (double v : voxels) {
			if (v > 0d) {
				sum += v;
				n++;
			}
		}
		return n == 0 ? Double.NaN : sum / n;
	}
}
