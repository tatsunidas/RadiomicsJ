package io.github.tatsunidas.radiomics.features;

import ij.ImagePlus;
import io.github.tatsunidas.radiomics.main.RadiomicsJ;
import io.github.tatsunidas.radiomics.main.Utils;

/**
 * Statistics outside IBSI, on the same intensity-mask voxels as the intensity based statistical features
 * (after resampling and re-segmentation, not discretised, except where noted).
 * An undefined value is NaN, never 0.
 */
public class AuxiliaryStatisticsFeatures extends IntensityBasedStatisticalFeatures {

	private final boolean useBinCount;
	private final int nBins;
	private final double binWidth;

	/**
	 * @param useBinCount, nBins, binWidth: the discretisation of the intensity histogram features; only
	 *        UniformityOfPositiveIntensities uses them. null nBins or binWidth means the RadiomicsJ default.
	 */
	public AuxiliaryStatisticsFeatures(ImagePlus img, ImagePlus mask, int label, boolean useBinCount, Integer nBins,
			Double binWidth) {
		super(img, mask, label);
		this.useBinCount = useBinCount;
		this.nBins = nBins == null ? RadiomicsJ.nBins : nBins;
		this.binWidth = binWidth == null ? RadiomicsJ.binWidth : binWidth;
	}

	public Double calculate(String id) {
		String name = AuxiliaryStatisticsFeatureType.findType(id);
		if (AuxiliaryStatisticsFeatureType.MeanOfPositiveIntensities.name().equals(name)) {
			return getMeanOfPositiveIntensities();
		} else if (AuxiliaryStatisticsFeatureType.ProportionOfPositiveIntensities.name().equals(name)) {
			return getProportionOfPositiveIntensities();
		} else if (AuxiliaryStatisticsFeatureType.NormalisedStandardDeviation.name().equals(name)) {
			return getNormalisedStandardDeviation();
		} else if (AuxiliaryStatisticsFeatureType.ReciprocalKurtosis.name().equals(name)) {
			return getReciprocalKurtosis();
		} else if (AuxiliaryStatisticsFeatureType.UniformityOfPositiveIntensities.name().equals(name)) {
			return getUniformityOfPositiveIntensities();
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

	/**
	 * Number of voxels with value > 0 divided by the number of voxels. NaN when there is no voxel.
	 */
	public Double getProportionOfPositiveIntensities() {
		if (voxels.length == 0) {
			return Double.NaN;
		}
		long n = 0;
		for (double v : voxels) {
			if (v > 0d) {
				n++;
			}
		}
		return (double) n / voxels.length;
	}

	/**
	 * ln(SD) / ln(N). SD is the population standard deviation (divided by N), N the number of voxels.
	 * NaN when ln(N) is 0 (N below 2) or SD is not positive. Negative when SD is below 1.
	 */
	public Double getNormalisedStandardDeviation() {
		long n = voxels.length;
		Double sd = getStandardDeviation();
		if (n < 2 || sd == null || Double.isNaN(sd) || sd <= 0d) {
			return Double.NaN;
		}
		return Math.log(sd) / Math.log(n);
	}

	/**
	 * 1 / (excess kurtosis + 3), that is m2^2 / m4. NaN when the variance is 0.
	 */
	public Double getReciprocalKurtosis() {
		Double ek = getExcessKurtosis();
		if (ek == null || Double.isNaN(ek) || Double.isInfinite(ek)) {
			return Double.NaN;
		}
		double k = ek + 3d;
		return k == 0d ? Double.NaN : 1d / k;
	}

	/**
	 * Sum of squared bin probabilities over the voxels with original value > 0. The bins are those of the
	 * discretised whole roi (the same discretisation as the intensity histogram features), not a new one made from
	 * the positive voxels. NaN when there is no positive voxel.
	 */
	public Double getUniformityOfPositiveIntensities() {
		ImagePlus disc;
		try {
			disc = useBinCount ? Utils.discrete(img, mask, label, nBins)
					: Utils.discreteByBinWidth(img, mask, label, binWidth);
		} catch (Exception e) {
			throw new IllegalStateException("discretisation failed", e);
		}
		double[] bins = Utils.getVoxels(disc, mask, label);// same voxel order as voxels
		java.util.HashMap<Double, Integer> counts = new java.util.HashMap<>();
		long positives = 0;
		for (int i = 0; i < voxels.length; i++) {
			if (voxels[i] > 0d) {
				counts.merge(bins[i], 1, Integer::sum);
				positives++;
			}
		}
		if (positives == 0) {
			return Double.NaN;
		}
		double uni = 0d;
		for (int c : counts.values()) {
			double p = (double) c / positives;
			uni += p * p;
		}
		return uni;
	}
}
