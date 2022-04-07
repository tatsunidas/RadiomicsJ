package RadiomicsJ;

/*
 * 3.1.1 Volume (mesh) RNU0 
 * 3.1.2 Volume (voxel counting) YEKZ 
 * 3.1.3 Surface area (mesh) C0JK 
 * 3.1.4 Surface to volume ratio 2PR5 
 * 3.1.5 Compactness 1 SKGS
 * 3.1.6 Compactness 2 BQWJ 
 * 3.1.7 Spherical disproportion KRCK 
 * 3.1.8 Sphericity QCFX 
 * 3.1.9 Asphericity 25C7 
 * 3.1.10 Centre of mass shift KLMA 
 * 3.1.11 Maximum 3D diameter L0JK 
 * 3.1.12 Major axis length TDIC 
 * 3.1.13 Minor axis length P9VJ
 * 3.1.14 Least axis length 7J51 
 * 3.1.15 Elongation Q3CK 
 * 3.1.16 Flatness N17B
 * 3.1.17 Volume density (axis-aligned bounding box) PBX1
 * 3.1.18 Area density (axis-aligned bounding box) R59B 
 * 3.1.19 Volume density (oriented minimum bounding box) ZH1A//deprecated 
 * 3.1.20 Area density (oriented minimum bounding box) IQYR//deprecated 
 * 3.1.21 Volume density (approximate enclosing ellipsoid) 6BDE 
 * 3.1.22 Area density (approximate enclosing ellipsoid) RDD2 
 * 3.1.23 Volume density (minimum volume enclosing ellipsoid) SWZ1//deprecated 
 * 3.1.24 Area density (minimum volume enclosing ellipsoid) BRI8//deprecated, NOT IMPLEMENTED 
 * 3.1.25 Volume density (convex hull) R3ER 
 * 3.1.26 Area density (convex hull) 7T7F 
 * 3.1.27 Integrated intensity 99N0 
 * 3.1.28 Moran's I index N365 
 * 3.1.29 Geary's C measure NPT7
 */
public enum MorphologicalFeatureType{
	VolumeMesh("RNU0"),
	VolumeVoxelCounting("YEKZ"),
	SurfaceAreaMesh("C0JK"),
	SurfaceToVolumeRatio("2PR5"),
	Compactness1("SKGS"),
	Compactness2("BQWJ"),
	SphericalDisproportion("KRCK"),
	Sphericity("QCFX"),
	Asphericity("25C7"),
	CentreOfMassShift("KLMA"),
	Maximum3DDiameter("L0JK"),
	MajorAxisLength("TDIC"),
	MinorAxisLength("P9VJ"),
	LeastAxisLength("7J51"),
	Elongation("Q3CK"),
	Flatness("N17B"),
	VolumeDensity_AxisAlignedBoundingBox("PBX1"),
	AreaDensity_AxisAlignedBoundingBox("R59B"),
	VolumeDensity_OrientedMinimumBoundingBox("ZH1A"),
	AreaDensity_OrientedMinimumBoundingBox("IQYR"),
	VolumeDensity_ApproximateEnclosingEllipsoid("6BDE"),
	AreaDensity_ApproximateEnclosingEllipsoid("RDD2"),
	VolumeDensity_MinimumVolumeEnclosingEllipsoid("SWZ1"),
	AreaDensity_MinimumVolumeEnclosingEllipsoid("BRI8"),
	VolumeDensity_ConvexHull("R3ER"),
	AreaDensity_ConvexHull("7T7F"),
	IntegratedIntensity("99N0"),
	MoransIIndex("N365"),
	GearysCMeasure("NPT7");
	
	private String id;
	
	private MorphologicalFeatureType(String id) {
		this.id = id;
	}
	
	public String id() {
		return id;
	}
	
	public static String findType(String code) {
		for(MorphologicalFeatureType f : MorphologicalFeatureType.values()) {
			if(f.id().equals(code)) {
				return f.name();
			}
		}
		return null;
	}
}
