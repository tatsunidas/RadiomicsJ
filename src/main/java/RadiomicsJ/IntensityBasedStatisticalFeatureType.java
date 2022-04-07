package RadiomicsJ;


//first order features	
public enum IntensityBasedStatisticalFeatureType{
	Mean("Q4LE"),
	Variance("ECT3"),
	Skewness("KE2A"),
	Kurtosis("IPH6"),//excess kurtosis
	Median("Y12H"),
	Minimum("1GSF"),
	Percentile10("QG58"),
	Percentile90("8DWT"),
	Maximum("84IY"),
	Interquartile("SALO"),
	Range("2OJQ"),
	MeanAbsoluteDeviation("4FUA"),
	RobustMeanAbsoluteDeviation("1128"),
	MedianAbsoluteDeviation("N72L"),
	CoefficientOfVariation("7TET"),
	QuartileCoefficientOfDispersion("9S40"),
	Energy("N8CA"),
	RootMeanSquared("5ZWQ"),
	TotalEnergy("1"),//origin
	StandardDeviation("2"),//origin
	StandardError("3"),//origin
	;
		
	private String id;
		
	private IntensityBasedStatisticalFeatureType(String id) {
		this.id = id;
	}
				
	public String id() {
		return id;
	}
		
	public static String findType(String id) {
		for(IntensityBasedStatisticalFeatureType fof : IntensityBasedStatisticalFeatureType.values()) {
			if(fof.id().equals(id)) {
				return fof.name();
			}
		}
		return null;
	}
}