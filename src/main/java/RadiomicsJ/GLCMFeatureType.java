package RadiomicsJ;

/**
 * 3.6.1 Joint maximum GYBY
 * 3.6.2 Joint average 60VM
 * 3.6.3 Joint variance UR99
 * 3.6.4 Joint entropy TU9B
 * 3.6.5 Difference average TF7R
 * 3.6.6 Difference variance D3YU
 * 3.6.7 Difference entropy NTRS
 * 3.6.8 Sum average ZGXS
 * 3.6.9 Sum variance OEEB
 * 3.6.10 Sum entropy P6QZ
 * 3.6.11 Angular second moment 8ZQL
 * 3.6.12 Contrast ACUI
 * 3.6.13 Dissimilarity 8S9J
 * 3.6.14 Inverse difference IB1Z
 * 3.6.15 Normalised inverse difference NDRX
 * 3.6.16 Inverse difference moment WF0Z
 * 3.6.17 Normalised inverse difference moment 1QCO
 * 3.6.18 Inverse variance E8JP
 * 3.6.19 Correlation NI2N
 * 3.6.20 Autocorrelation QWB0
 * 3.6.21 Cluster tendency DG8W
 * 3.6.22 Cluster shade 7NFM
 * 3.6.23 Cluster prominence AE86
 * 3.6.24 Information correlation 1 R8DG
 * 3.6.25 Information correlation 2 JN9H
 *
 */
public enum GLCMFeatureType{
	JointMaximum("GYBY"),
	JointAverage("60VM"),
	JointVariance("UR99"),
	JointEntropy("TU9B"),
	DifferenceAverage("TF7R"),
	DifferenceVariance("D3YU"),
	DifferenceEntropy("NTRS"),
	SumAverage("ZGXS"),
	SumVariance("OEEB"),
	SumEntropy("P6QZ"),
	AngularSecondMoment("8ZQL"),
	Contrast("ACUI"),
	Dissimilarity("8S9J"),
	InverseDifference("IB1Z"),
	NormalizedInverseDifference("NDRX"),
	InverseDifferenceMoment("WF0Z"),
	NormalizedInverseDifferenceMoment("1QCO"),
	InverseVariance("E8JP"),
	Correlation("NI2N"),
	Autocorrection("QWB0"),
	ClusterTendency("DG8W"),
	ClusterShade("7NFM"),
	ClusterProminence("AE86"),
	InformationalMeasureOfCorrelation1("R8DG"),
	InformationalMeasureOfCorrelation2("JN9H")
	;

	private String id;
	
	private GLCMFeatureType(String id) {
		this.id = id;
	}
	
	public String id() {
		return id;
	}
	
	public static String findType(String id) {
		for(GLCMFeatureType glcm : GLCMFeatureType.values()) {
			if(glcm.id().equals(id)) {
				return glcm.name();
			}
		}
		return null;
	}
}
