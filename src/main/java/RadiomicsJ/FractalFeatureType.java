package RadiomicsJ;

public enum FractalFeatureType {
	Capacity("0");//fractal dimension

	private String id;
	
	private FractalFeatureType(String id) {
		this.id = id;
	}
	
	public String id() {
		return id;
	}
	
	public static String findType(String id) {
		for(FractalFeatureType fractal : FractalFeatureType.values()) {
			if(fractal.id().equals(id)) {
				return fractal.name();
			}
		}
		return null;
	}
}
