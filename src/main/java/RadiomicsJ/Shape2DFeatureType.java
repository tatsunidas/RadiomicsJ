package RadiomicsJ;

public enum Shape2DFeatureType {
	PixelSurface("0"),
	Perimeter("1"),
	PerimeterToPixelSurfaceRatio("2"),
	Sphericity("3"),
	Circularity("4"),
	SphericalDisproportion("5"),
	FerretAngle("6"),
//	Maximum2DDiameter("6"),
//	MajorAxisLength("7"),
//	MinorAxisLength("8"),
//	Elongation("9"),
	;
	
	private String id;
	
	private Shape2DFeatureType(String id) {
		this.id = id;
	}
	
	public String id() {
		return id;
	}
	
	public static String findType(String id) {
		for(Shape2DFeatureType shape : Shape2DFeatureType.values()) {
			if(shape.id().equals(id)) {
				return shape.name();
			}
		}
		return null;
	}
}
