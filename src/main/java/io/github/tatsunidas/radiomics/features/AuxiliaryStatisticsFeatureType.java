package io.github.tatsunidas.radiomics.features;

/**
 * Statistics that are not defined by IBSI. The ids are deliberately not IBSI-style codes.
 */
public enum AuxiliaryStatisticsFeatureType {
	MeanOfPositiveIntensities("0"),//mean of the intensity-mask voxels whose value is > 0
	ProportionOfPositiveIntensities("1"),//number of voxels > 0 / number of intensity-mask voxels
	NormalisedStandardDeviation("2"),//ln(SD) / ln(N), SD is the population standard deviation
	ReciprocalKurtosis("3"),//1 / (excess kurtosis + 3)
	UniformityOfPositiveIntensities("4");//sum of squared bin probabilities over the voxels > 0, bins of the whole roi

	private String id;

	private AuxiliaryStatisticsFeatureType(String id) {
		this.id = id;
	}

	public String id() {
		return id;
	}

	public static String findType(String id) {
		for(AuxiliaryStatisticsFeatureType t : AuxiliaryStatisticsFeatureType.values()) {
			if(t.id().equals(id)) {
				return t.name();
			}
		}
		return null;
	}
}
