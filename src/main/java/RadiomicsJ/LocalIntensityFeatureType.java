package RadiomicsJ;

public enum LocalIntensityFeatureType {
	LocalIntensityPeak("VJGA"),
	GlobalIntensityPeak("0F91"),
	;
	private String id;
	
	private LocalIntensityFeatureType(String id) {
		this.id = id;
	}
	
	public String id() {
		return id;
	}
	
	public static String findType(String id) {
		for(LocalIntensityFeatureType f : LocalIntensityFeatureType.values()) {
			if(f.id().equals(id)) {
				return f.name();
			}
		}
		return null;
	}
}
