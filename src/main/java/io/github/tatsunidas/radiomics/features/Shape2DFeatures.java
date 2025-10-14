package io.github.tatsunidas.radiomics.features;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.stat.correlation.Covariance;

import ij.ImagePlus;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.measure.ResultsTable;
import ij.plugin.filter.Analyzer;
import ij.process.ImageStatistics;
import io.github.tatsunidas.radiomics.main.ImagePreprocessing;
import io.github.tatsunidas.radiomics.main.Utils;

/**
 * 
 * Usage
 * Pixel Size Calibration : User define (if not set, processing with pixel base.)
 * Intensity Calibration : User define
 * Bins : No need
 * density shift : No need
 * 
 * @author tatsunidas
 *
 */
public class Shape2DFeatures extends AbstractRadiomicsFeature{
	
	Calibration orgCal;// backup
	final int slice_pos;//1 to N
	final int label;
	
	Roi roi;
	
	Analyzer analyzer;
	
	int w;
	int h;
	
	double[] eigenValues;
	double eps = Math.ulp(1.0);// 2.220446049250313E-16
	
	private Map<String, Object> settings;
	
	/**
	 * 
	 * @param img
	 * @param mask
	 * @param slice : 1 to N.
	 * @param settings
	 */
	public Shape2DFeatures(ImagePlus img, ImagePlus mask, int slice, Map<String, Object> settings) {
		super(img,mask,settings);
		this.slice_pos = slice;
		if(slice > img.getNSlices() || slice < 1) {
			throw new IllegalArgumentException("RadiomicsJ:Shape2D please input valid slice number. input images has "+img.getNSlices()+" slices, but specified slice position is "+slice+" (out of range).");
		}
		Object labelValue = settings.get(RadiomicsFeature.LABEL);
		if (labelValue == null) {
			throw new IllegalArgumentException("'label' is missing in settings.");
		}
		if (!(labelValue instanceof Integer)) {
			throw new IllegalArgumentException("'label' must be an Integer.");
		}
		this.label = (Integer) labelValue;
		buildup(settings);
	}
	
	/**
	 * 
	 * @param img
	 * @param mask : null-able
	 * @param slice : slice position, 1 to N
	 */
	public Shape2DFeatures(ImagePlus img, ImagePlus mask, int slice, int label) {
		super(img,mask,null);
		this.label = label;
		this.slice_pos = slice;

		int iw = img.getWidth();
		int ih = img.getHeight();
		int is = img.getNSlices();
		
		this.w = iw;
		this.h = ih;
		
		if(slice > is || slice < 1) {
			throw new IllegalArgumentException("RadiomicsJ:Shape2D please input valid slice number. input images has "+is+" slices, but specified slice position is "+slice+" (out of range).");
		}
		
		if (mask == null) {
			// create full face mask
			this.mask = ImagePreprocessing.createMask(iw, ih, is, null, label, img.getCalibration().pixelWidth,img.getCalibration().pixelHeight, img.getCalibration().pixelDepth);
		}
		
		this.img.setPosition(slice_pos);
		this.mask.setPosition(slice_pos);
		
		orgCal = img.getCalibration().copy();
		
		this.roi = Utils.createRoi(this.mask, this.slice_pos, this.label);
		if(this.roi == null) {
			this.roi = new Roi(0,0,w,h);
		}
		this.img.setRoi(this.roi);
		int measurements = Analyzer.ALL_STATS;
		ImageStatistics stats = this.img.getStatistics(measurements);
		Analyzer.setMeasurements(measurements);
		analyzer = new Analyzer();
		analyzer.saveResults(stats, roi);
		
		settings.put("IMAGE", this.img);
		settings.put("MASK", this.mask);
		settings.put("SLICE_POS", this.slice_pos);
		settings.put("ROI", roi);
		settings.put("LABEL", this.label);
		settings.put("IMAGE_STATS", stats);
	}
	
	public void buildup(Map<String, Object> settings) {
		/*
		 * label and slice_pos are already specified at constructor.
		 */
		int iw = img.getWidth();
		int ih = img.getHeight();
		int is = img.getNSlices();
		
		this.w = iw;
		this.h = ih;
		
		orgCal = img.getCalibration().copy();
		
		if (mask == null) {
			// create full face mask
			this.mask = ImagePreprocessing.createMask(iw, ih, is, null, label, orgCal.pixelWidth, orgCal.pixelHeight, orgCal.pixelDepth);
		}
		
		this.img.setPosition(slice_pos);
		this.mask.setPosition(slice_pos);
		
		this.roi = Utils.createRoi(this.mask, this.slice_pos, this.label);
		if(this.roi == null) {
			this.roi = new Roi(0,0,w,h);
		}
		this.img.setRoi(this.roi);
		int measurements = Analyzer.ALL_STATS;
		ImageStatistics stats = this.img.getStatistics(measurements);
		Analyzer.setMeasurements(measurements);
		analyzer = new Analyzer();
		analyzer.saveResults(stats, roi);
	}
	
	/*
	 * AREA
	 * MEAN//intensity
	 * STD_DEV//intensity
	 * MODE//intensity
	 * MIN_MAX//intensity
	 * CENTROID//TODO
	 * CENTER_OF_MASS//TODO
	 * PERIMETER
	 * RECT//TODO
	 * ELLIPSE//TODO
	 * SHAPE_DESCRIPTORS//TODO
	 * FERET
	 * INTEGRATED_DENSITY//intensity
	 * MEDIAN//intensity
	 * SKEWNESS//intensity histogram
	 * KURTOSIS//intensity histogram
	 * AREA_FRACTION;
	 */
	public Double calculate(String id) {
		String name = Shape2DFeatureType.findType(id);
		if (name.equals(Shape2DFeatureType.PixelSurface.name())) {
			return getAreaByPixelSurface();
		} else if (name.equals(Shape2DFeatureType.Perimeter.name())) {
			return getPerimeter();
		} else if (name.equals(Shape2DFeatureType.PerimeterToPixelSurfaceRatio.name())) {
			return getPerimeterSurfaceRatio();
		} else if (name.equals(Shape2DFeatureType.Sphericity.name())) {
			return getSphericity();
		}else if (name.equals(Shape2DFeatureType.SphericalDisproportion.name())) {
			return getSphericalDisportion();
		}else if (name.equals(Shape2DFeatureType.Circularity.name())) {
			return getCircularity();
		}else if(name.equals(Shape2DFeatureType.FerretAngle.name())) {
			return getFerretAngle();
		}else if (name.equals(Shape2DFeatureType.Maximum2DDiameter.name())) {
			return getMaximumDiameter();
		}else if (name.equals(Shape2DFeatureType.MajorAxisLength.name())) {
			return getMajorAxisLength();
		}else if (name.equals(Shape2DFeatureType.MinorAxisLength.name())) {
			return getMinorAxisLength();
		}else if (name.equals(Shape2DFeatureType.LeastAxisLength.name())) {
			return getLeastAxisLength();
		}else if (name.equals(Shape2DFeatureType.Elongation.name())) {
			return getElongation();
		}else if (name.equals(Shape2DFeatureType.AreaFraction.name())) {
			return getAreaFraction();
		}
		return null;
	}
	
	/**
	 * Do not use stats.area.
	 * @return area.
	 */
	private Double getAreaByPixelSurface() {
		int cnt = 0;
		for(int i=0; i<w ;i++) {
			for(int j=0; j<h ;j++) {
				if(roi.contains(i, j)) {
					cnt++;
				}
			}
		}
		return cnt * orgCal.pixelHeight * orgCal.pixelWidth;
	}
	
	private Double getPerimeter() {
		@SuppressWarnings("static-access")
		ResultsTable rt =analyzer.getResultsTable(); // get the system results table
		int counter = rt.getCounter();
		double perimeter = rt.getValueAsDouble(ResultsTable.PERIMETER, counter-1);
		return perimeter;
	}
	
	private Double getPerimeterSurfaceRatio() {
		@SuppressWarnings("static-access")
		ResultsTable rt =analyzer.getResultsTable(); // get the system results table
		int counter = rt.getCounter();
		double area = rt.getValueAsDouble(ResultsTable.AREA, counter-1);
		double perimeter = rt.getValueAsDouble(ResultsTable.PERIMETER, counter-1);
		return perimeter==0.0 ? 0.0 : area/(perimeter+eps);
	}
	
	private Double getCircularity() {
		@SuppressWarnings("static-access")
		ResultsTable rt =analyzer.getResultsTable(); // get the system results table
		int counter = rt.getCounter();
		double area = rt.getValueAsDouble(ResultsTable.AREA, counter-1);
		double perimeter = rt.getValueAsDouble(ResultsTable.PERIMETER, counter-1);
		return perimeter==0.0 ? 0.0 : 4.0*Math.PI*(area/(perimeter*perimeter));
	}
	
	
	private Double getSphericity() {
		@SuppressWarnings("static-access")
		ResultsTable rt = analyzer.getResultsTable(); // get the system results table
		int counter = rt.getCounter();//rt.size() is more suitable ?
		double area = rt.getValueAsDouble(ResultsTable.AREA, counter-1);
		double perimeter = rt.getValueAsDouble(ResultsTable.PERIMETER, counter-1);
		return (2.0 * Math.sqrt(Math.PI*area))/(perimeter+eps);
	}
	
	private Double getSphericalDisportion() {
		double sph = getSphericity();
		return 1.0/(sph+eps);
	}
	
	private Double getFerretAngle() {
		return roi.getFeretValues()[1];
	}
	
	/*
	 * largest pairwise Euclidian distance
	 * This is the same values of Morphological feature's one.
	 */
	private Double getMaximumDiameter() {
		double max = 0d;
		int n = roi.getFloatPolygon().npoints;
		float[] pxs = roi.getFloatPolygon().xpoints; 
		float[] pys = roi.getFloatPolygon().ypoints; 
		double pw = orgCal.pixelWidth;
		double ph = orgCal.pixelHeight;
		for(int i=0;i<n;i++) {
			float fx = pxs[i];
			float fy = pys[i];
			for(int j=0;j<n;j++) {
				if(i==j) {
					continue;
				}
				float fx2 = pxs[j];
				float fy2 = pys[j];
				double l = Math.sqrt(Math.pow((fx-fx2)*pw,2)+Math.pow((fy-fy2)*ph, 2));
				if(l > max) {
					max = l;
				}
			}
		}
		return max;
	}
	
	private Double getMajorAxisLength() {
		if(eigenValues != null) {
			return Math.sqrt(eigenValues[0])*4;
		}
		/*
		 * no using mesh
		 */
		int nPoints = 0;
		float[][] mSlice = mask.getProcessor().getFloatArray();
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int lbl = (int) mSlice[x][y];
				if (lbl == this.label) {
					nPoints++;
				}
			}
		}
		
		//create points in a double array
		double[][] pts = new double[nPoints][];
		int n = 0;
		double dx = mask.getCalibration().pixelWidth;
		double dy = mask.getCalibration().pixelHeight;
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int lbl = (int) mSlice[x][y];
				if (lbl == this.label) {
					pts[n] = new double[] { x * dx, y * dy };
					n++;
				}
			}
		}
		/*
		 * pca
		 */
		//create real matrix
		RealMatrix realMatrix = MatrixUtils.createRealMatrix(pts);
		//create covariance matrix of points, then find eigenvectors
		Covariance covariance = new Covariance(realMatrix);
		RealMatrix covarianceMatrix = covariance.getCovarianceMatrix();
		EigenDecomposition ed = new EigenDecomposition(covarianceMatrix);
//		SingularValueDecomposition svd = new SingularValueDecomposition(covarianceMatrix);
		eigenValues = ed.getRealEigenvalues();
		return Math.sqrt(eigenValues[0])*4;
	}
	
	private Double getMinorAxisLength() {
		if(eigenValues != null) {
			return Math.sqrt(eigenValues[1])*4;
		}
		/*
		 * no using mesh
		 */
		int nPoints = 0;
		float[][] mSlice = mask.getProcessor().getFloatArray();
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int lbl = (int) mSlice[x][y];
				if (lbl == this.label) {
					nPoints++;
				}
			}
		}
		
		//create points in a double array
		double[][] pts = new double[nPoints][];
		int n = 0;
		double dx = mask.getCalibration().pixelWidth;
		double dy = mask.getCalibration().pixelHeight;
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int lbl = (int) mSlice[x][y];
				if (lbl == this.label) {
					pts[n] = new double[] { x * dx, y * dy };
					n++;
				}
			}
		}
		/*
		 * pca
		 */
		//create real matrix
		RealMatrix realMatrix = MatrixUtils.createRealMatrix(pts);
		//create covariance matrix of points, then find eigenvectors
		Covariance covariance = new Covariance(realMatrix);
		RealMatrix covarianceMatrix = covariance.getCovarianceMatrix();
		EigenDecomposition ed = new EigenDecomposition(covarianceMatrix);
//		SingularValueDecomposition svd = new SingularValueDecomposition(covarianceMatrix);
		eigenValues = ed.getRealEigenvalues();
		return eigenValues[1] < eigenValues[2] ? Math.sqrt(eigenValues[2])*4 : Math.sqrt(eigenValues[1])*4;
	}
	
	/**
	 * 2D Shape does not have least axis.
	 * @return null
	 */ 
	@Deprecated
	private Double getLeastAxisLength() {
		return Double.NaN;
	}
	
	private Double getElongation() {
		if(eigenValues != null) {
			double axis[] = eigenValues;
			double major = axis[0];
			double minor = axis[1];
			return Math.sqrt(minor/major);
		}
		
		int nPoints = 0;
		float[][] mSlice = mask.getProcessor().getFloatArray();
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int lbl = (int) mSlice[x][y];
				if (lbl == this.label) {
					nPoints++;
				}
			}
		}
		
		//create points in a double array
		double[][] pts = new double[nPoints][];
		int n = 0;
		double dx = mask.getCalibration().pixelWidth;
		double dy = mask.getCalibration().pixelHeight;
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int lbl = (int) mSlice[x][y];
				if (lbl == this.label) {
					pts[n] = new double[] { x * dx, y * dy };
					n++;
				}
			}
		}
		/*
		 * pca
		 */
		//create real matrix
		RealMatrix realMatrix = MatrixUtils.createRealMatrix(pts);
		//create covariance matrix of points, then find eigenvectors
		Covariance covariance = new Covariance(realMatrix);
		RealMatrix covarianceMatrix = covariance.getCovarianceMatrix();
		EigenDecomposition ed = new EigenDecomposition(covarianceMatrix);
//		SingularValueDecomposition svd = new SingularValueDecomposition(covarianceMatrix);
		double axis[] = ed.getRealEigenvalues();
		double major = axis[0];
		double minor = axis[1];
		return Math.sqrt(minor/major);
	}
	
	private Double getAreaFraction(){
		@SuppressWarnings("static-access")
		ResultsTable rt =analyzer.getResultsTable(); // get the system results table
		int counter = rt.getCounter();
		double af = rt.getValueAsDouble(ResultsTable.AREA_FRACTION, counter-1);
		return af;
	}

	@Override
	public Set<String> getAvailableFeatures() {
		Set<String> names = new HashSet<>();
		for(Shape2DFeatureType t : Shape2DFeatureType.values()) {
			names.add(t.name());
		}
		return names;
	}

	@Override
	public String getFeatureFamilyName() {
		return "SHAPE2D";
	}

	@Override
	public Map<String, Object> getSettings() {
		return settings;
	}
}
