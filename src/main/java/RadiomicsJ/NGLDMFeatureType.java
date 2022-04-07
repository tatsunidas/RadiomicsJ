package RadiomicsJ;

public enum NGLDMFeatureType {
	LowDependenceEmphasis("SODN"),
	HighDependenceEmphasis("IMOQ"),
	LowGrayLevelCountEmphasis("TL9H"),
	HighGrayLevelCountEmphasis("OAE7"),
	LowDependenceLowGrayLevelEmphasis("EQ3F"),
	LowDependenceHighGrayLevelEmphasis("JA6D"),
	HighDependenceLowGrayLevelEmphasis("NBZI"), 
	HighDependenceHighGrayLevelEmphasis("9QMG"),
	GrayLevelNonUniformity("FP8K"),
	GrayLevelNonUniformityNormalized("5SPA"),
	DependenceCountNonUniformity("Z87G"),
	DependenceCountNonUniformityNormalized("OKJI"),
	DependenceCountPercentage("6XV8"),
	GrayLevelVariance("1PFV"),
	DependenceCountVariance("DNX2"),
	DependenceCountEntropy("FCBV"),
	DependenceCountEnergy("CAS9"),
	;

	private String id;
	
	private NGLDMFeatureType(String id) {
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
		for(NGLDMFeatureType gldm : NGLDMFeatureType.values()) {
			if(gldm.id().equals(id)) {
				return gldm.name();
			}
		}
		return null;
	}
}
