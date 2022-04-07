package RadiomicsJ;

/**
 * shared with GLDZM.
 * @author tatsunidas
 *
 */
public enum GLSZMFeatureType{
	SmallZoneEmphasis("5QRC"),
	LargeZoneEmphasis("48P8"),
	LowGrayLevelZoneEmphasis("XMSY"),
	HighGrayLevelZoneEmphasis("5GN9"),
	SmallZoneLowGrayLevelEmphasis("5RAI"),
	SmallZoneHighGrayLevelEmphasis("HW1V"),
	LargeZoneLowGrayLevelEmphasis("YH51"),
	LargeZoneHighGrayLevelEmphasis("J17V"),
	GrayLevelNonUniformity("JNSA"),
	GrayLevelNonUniformityNormalized("Y1RO"),
	SizeZoneNonUniformity("4JP3"),
	SizeZoneNonUniformityNormalized("VB3A"),
	ZonePercentage("P30P"),
	GrayLevelVariance("BYLV"),
	ZoneSizeVariance("3NSA"),
	ZoneSizeEntropy("GU8N"),
	;

	private String id;
	
	private GLSZMFeatureType(String id) {
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
		for(GLSZMFeatureType glszm : GLSZMFeatureType.values()) {
			if(glszm.id().equals(id)) {
				return glszm.name();
			}
		}
		return null;
	}
}