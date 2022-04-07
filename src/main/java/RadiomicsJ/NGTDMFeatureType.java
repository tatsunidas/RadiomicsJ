package RadiomicsJ;

public enum NGTDMFeatureType {
	Coarseness("QCDE"),
	Contrast("65HE"),
	Busyness("NQ30"),
	Complexity("HDEZ"),
	Strength("1X9X");

	private String id;
	
	private NGTDMFeatureType(String id) {
		this.id = id;
	}
	
	public String id() {
		return id;
	}
	
	public static String findType(String id) {
		for(NGTDMFeatureType ngtdm : NGTDMFeatureType.values()) {
			if(ngtdm.id().equals(id)) {
				return ngtdm.name();
			}
		}
		return null;
	}
}
