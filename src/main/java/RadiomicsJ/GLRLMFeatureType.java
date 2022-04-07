package RadiomicsJ;

public enum GLRLMFeatureType{
	ShortRunEmphasis("22OV"),
	LongRunEmphasis("W4KF"),
	LowGrayLevelRunEmphasis("V3SW"),
	HighGrayLevelRunEmphasis("G3QZ"),
	ShortRunLowGrayLevelEmphasis("HTZT"),
	ShortRunHighGrayLevelEmphasis("GD3A"),
	LongRunLowGrayLevelEmphasis("IVPO"),
	LongRunHighGrayLevelEmphasis("3KUM"),
	GrayLevelNonUniformity("R5YN"),
	GrayLevelNonUniformityNormalized("OVBL"),
	RunLengthNonUniformity("W92Y"),
	RunLengthNonUniformityNormalized("IC23"),
	RunPercentage("9ZK5"),
	GrayLevelVariance("8CE5"),
	RunLengthVariance("SXLW"),
	RunEntropy("HJ9O"),
	;

	private String id;
	
	private GLRLMFeatureType(String id) {
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
		for(GLRLMFeatureType f : GLRLMFeatureType.values()) {
			if(f.id().equals(id)) {
				return f.name();
			}
		}
		return null;
	}
}
