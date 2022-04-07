package RadiomicsJ;

public enum IntensityHistogramFeatureType {
	/*
	 * basically, all values are discretised !!
	 */
	MeanDiscretisedIntensity("X6K6"),
	Variance("CH89"),
	Skewness("88K1"),
	Kurtosis("C3I7"),
	Median("WIFQ"),
	Minimum("1PR8"),
	Percentile10("GPMT"),
	Percentile90("OZ0C"),
	Maximum("3NCY"),
	Mode("AMMC"),//histogram
	Interquartile("WR0O"),
	Range("5Z3W"),
	MeanAbsoluteDeviation("D2ZX"),
	RobustMeanAbsoluteDeviation("WRZB"),
	MedianAbsoluteDeviation("4RNL"),
	CoefficientOfVariation("CWYJ"),
	QuartileCoefficientOfDispersion("SLWD"),
	//histogram
	Entropy("TLU2"),
	Uniformity("BJ5W"),
	MaximumHistogramGradient("12CE"),
	MaximumHistogramGradientIntensity("8E6O"),
	MinimumHistogramGradient("VQB3"),
	MinimumHistogramGradientIntensity("RHQZ"),
	;
	
	private String id;
	
	private IntensityHistogramFeatureType(String id) {
		this.id = id;
	}
			
	public String id() {
		return id;
	}
	
	public static String findType(String id) {
		for(IntensityHistogramFeatureType fof : IntensityHistogramFeatureType.values()) {
			if(fof.id().equals(id)) {
				return fof.name();
			}
		}
		return null;
	}
}
