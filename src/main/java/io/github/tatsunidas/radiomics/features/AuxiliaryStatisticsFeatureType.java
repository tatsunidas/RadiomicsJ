package io.github.tatsunidas.radiomics.features;

/**
 * Statistics that are not defined by IBSI. The ids are deliberately not IBSI-style codes.
 */
public enum AuxiliaryStatisticsFeatureType {
	MeanOfPositiveIntensities("0");//mean of the intensity-mask voxels whose value is > 0

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
