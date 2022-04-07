package RadiomicsJ;

import javax.swing.JOptionPane;

import ij.ImagePlus;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.filter.Analyzer;
import ij.process.ImageStatistics;

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
public class Shape2DFeatures {
	
	ImagePlus orgImg;//stack-able
	ImagePlus orgMask;//stack-able
	Calibration orgCal;// backup
	int slice_pos = 1;//1 to N
	
	ImagePlus img;//single processor image
	ImagePlus mask;//single processor mask
	Roi roi;
	
	ImageStatistics stats;
	Analyzer analyzer;
	
	int w;
	int h;
	
	double eps = Math.ulp(1.0);// 2.220446049250313E-16
	
	/**
	 * 
	 * @param img
	 * @param mask : null-able
	 * @param slice : slice position, 1 to N
	 */
	public Shape2DFeatures(ImagePlus img, ImagePlus mask, Integer slice, int label) {
		if (img == null) {
			return;
		}
		if (img.getType() == ImagePlus.COLOR_RGB) {
			JOptionPane.showMessageDialog(null, "RadiomicsJ can read only grayscale images(8/16/32 bits)...sorry.");
			return;
		}
		
		int iw = img.getWidth();
		int ih = img.getHeight();
		int is = img.getNSlices();
		
		this.w = iw;
		this.h = ih;
		
		if(slice == null) {
			if(is == 1) {
				slice = 1;
			}else {
				JOptionPane.showMessageDialog(null, "RadiomicsJ:Shape2D please input valid slice number.");
				return;
			}
		}else {
			if(slice > is || slice < 1) {
				JOptionPane.showMessageDialog(null, "RadiomicsJ:Shape2D please input valid slice number. input images has "+is+" slices, but specified slice position is "+slice+" (out of range).");
				return;
			}
		}
		this.slice_pos = slice;
//		this.label = label;
		if (mask != null) {
			int mw = mask.getWidth();
			int mh = mask.getHeight();
			int ms = mask.getNSlices();
			if (iw != mw || ih != mh || is != ms) {
				JOptionPane.showMessageDialog(null, "RadiomicsJ:Shape2D please input same dimension image and mask.");
				return;
			}
		}else {
			// create full face mask
			mask = ImagePreprocessing.createMask(iw, ih, is, null, label, img.getCalibration().pixelWidth,img.getCalibration().pixelHeight, img.getCalibration().pixelDepth);
		}
		
		orgImg = img;
		orgMask = mask;
		orgCal = img.getCalibration().copy();
		
//		orgMask.setCalibration(orgCal);
//		orgMask.getCalibration().disableDensityCalibration();
		
		/*
		 * create slice imp.
		 */
		this.img = new ImagePlus("image_"+slice, orgImg.getStack().getProcessor(slice));
		this.img.setCalibration(orgCal);
		this.mask = new ImagePlus("mask_"+slice, orgMask.getStack().getProcessor(slice));
		this.mask = Utils.createMaskCopy(this.mask);//only labeling area
		this.mask.setCalibration(orgCal);
		this.mask.getCalibration().disableDensityCalibration();
		this.roi = Utils.createRoi(this.mask, label);
		if(this.roi == null) {
			this.roi = new Roi(0,0,w,h);
		}
		img.setRoi(this.roi);
		int measurements = Analyzer.getMeasurements(); // defined in Set Measurements dialog
		measurements |= Measurements.AREA+Measurements.PERIMETER; //make sure area and perimeter are measured
		stats = img.getStatistics(measurements);
		Analyzer.setMeasurements(measurements);
		analyzer = new Analyzer();
		analyzer.saveResults(stats, roi);
		
	}
	
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
		}
		/*
		 * deprecated, deligate to Morphological features.
		 */
//		else if (name.equals(Shape2DFeatureType.Maximum2DDiameter.name())) {
//			return getMaximumDiameter();
//		}else if (name.equals(Shape2DFeatureType.MajorAxisLength.name())) {
//			return getMajorAxisLength();
//		}else if (name.equals(Shape2DFeatureType.MinorAxisLength.name())) {
//			return getMinorAxisLength();
//		}else if (name.equals(Shape2DFeatureType.Elongation.name())) {
//			return getElongation();
//		}
		return null;
	}
		
	public Double getAreaByPixelSurface() {
		return stats.area;//calibrated
	}
	
	public Double getPerimeter() {
		@SuppressWarnings("static-access")
		ResultsTable rt =analyzer.getResultsTable(); // get the system results table
		int counter = rt.getCounter();
		double perimeter = rt.getValueAsDouble(ResultsTable.PERIMETER, counter-1);
		return perimeter;
	}
	
	public Double getPerimeterSurfaceRatio() {
		@SuppressWarnings("static-access")
		ResultsTable rt =analyzer.getResultsTable(); // get the system results table
		int counter = rt.getCounter();
		double area = rt.getValueAsDouble(ResultsTable.AREA, counter-1);
		double perimeter = rt.getValueAsDouble(ResultsTable.PERIMETER, counter-1);
		return perimeter==0.0 ? 0.0 : area/(perimeter+eps);
	}
	
	public Double getCircularity() {
		@SuppressWarnings("static-access")
		ResultsTable rt =analyzer.getResultsTable(); // get the system results table
		int counter = rt.getCounter();
		double area = rt.getValueAsDouble(ResultsTable.AREA, counter-1);
		double perimeter = rt.getValueAsDouble(ResultsTable.PERIMETER, counter-1);
		return perimeter==0.0 ? 0.0 : 4.0*Math.PI*(area/(perimeter*perimeter));
	}
	
	
	public Double getSphericity() {
		@SuppressWarnings("static-access")
		ResultsTable rt = analyzer.getResultsTable(); // get the system results table
		int counter = rt.getCounter();//rt.size() is more suitable ?
		double area = rt.getValueAsDouble(ResultsTable.AREA, counter-1);
		double perimeter = rt.getValueAsDouble(ResultsTable.PERIMETER, counter-1);
		return (2.0 * Math.sqrt(Math.PI*area))/(perimeter+eps);
	}
	
	public Double getSphericalDisportion() {
		double sph = getSphericity();
		return 1.0/(sph+eps);
	}
	
	public Double getFerretAngle() {
		return roi.getFeretValues()[1];
	}
	
	/*
	 * largest pairwise Euclidian distance
	 * This is the same values of Morphological feature's one.
	 */
	@Deprecated
	public Double getMaximumDiameter() {
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
	
	/*
	 * This is the same values of Morphological feature's one.
	 */
	@Deprecated
	public Double getMajorAxisLength() {
		return null;
	}
	
	/*
	 * This is the same values of Morphological feature's one.
	 */
	@Deprecated
	public Double getMinorAxisLength() {
		return null;
	}
	
	/*
	 * This is the same values of Morphological feature's one.
	 */
	@Deprecated
	public Double getElongation() {
		return null;
	}	
}
