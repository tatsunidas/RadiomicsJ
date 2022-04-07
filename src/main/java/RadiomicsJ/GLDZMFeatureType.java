package RadiomicsJ;

/**
 * shared with GLDZM.
 * @author tatsunidas
 *
 */
public enum GLDZMFeatureType{
	SmallDistanceEmphasis("0GBI"),
	LargeDistanceEmphasis("MB4I"),
	LowGrayLevelZoneEmphasis("S1RA"),
	HighGrayLevelZoneEmphasis("K26C"),
	SmallDistanceLowGrayLevelEmphasis("RUVG"),
	SmallDistanceHighGrayLevelEmphasis("DKNJ"),
	LargeDistanceLowGrayLevelEmphasis("A7WM"),
	LargeDistanceHighGrayLevelEmphasis("KLTH"),
	GrayLevelNonUniformity("VFT7"),
	GrayLevelNonUniformityNormalized("7HP3"),
	ZoneDistanceNonUniformity("V294"),
	ZoneDistanceNonUniformityNormalized("IATH"),
	ZonePercentage("VIWW"),
	GrayLevelVariance("QK93"),
	ZoneDistanceVariance("7WT1"),
	ZoneDistanceEntropy("GBDU"),
	;

	private String id;
	
	private GLDZMFeatureType(String id) {
		this.id = id;
	}
	
	public String id() {
		return id;
	}
	
	@Override
	public String toString() {
		return this.name();
	}
	
	public static String findType(String id) {
		for(GLDZMFeatureType gldzm : GLDZMFeatureType.values()) {
			if(gldzm.id().equals(id)) {
				return gldzm.name();
			}
		}
		return null;
	}
}