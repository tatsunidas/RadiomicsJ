package RadiomicsJ;

public enum IntensityVolumeHistogramFeatureType {
	VolumeAtIntensityFraction10("BC2M_1"),
	VolumeAtIntensityFraction90("BC2M_2"),
	IntensityAtVolumeFraction10("GBPN_1"), 
	IntensityAtVolumeFraction90("GBPN_2"), 
	VolumeFractionDifferenceBetweenIntensityFractions("DDTU"),
	IntensityFractionDifferenceBetweenVolumeFractions("CNV2"),
	AreaUnderTheIVHCurve("9CMM"),
	;
	private String id;
	
	private IntensityVolumeHistogramFeatureType(String id) {
		this.id = id;
	}
	
	public String id() {
		return id;
	}
	
	public static String findType(String id) {
		for(IntensityVolumeHistogramFeatureType f : IntensityVolumeHistogramFeatureType.values()) {
			if(f.id().equals(id)) {
				return f.name();
			}
		}
		return null;
	}
}
