package RadiomicsJ;

import java.util.ArrayList;

import javax.swing.JOptionPane;

import org.apache.commons.math3.stat.StatUtils;

import ij.ImagePlus;
import ij.measure.Calibration;

/**
 * 
 * image is stack or one slice.
 * mask is null-able. if mask is null, all pixels calculated.
 * 
 * 3.3 Intensity-based statistical features
 * 3.3.1 Mean intensity
 * 3.3.2 Intensity variance
 * 3.3.3 Intensity skewness
 * 3.3.4 (Excess) intensity kurtosis : see, getKurtosis1, getKurtosis2
 * 3.3.5 Median intensity
 * 3.3.6 Minimum intensity
 * 3.3.7 10th intensity percentile
 * 3.3.8 90th intensity percentile
 * 3.3.9 Maximum intensity
 * 3.3.10 Intensity interquartile range
 * 3.3.11 Intensity range (max-min)
 * 3.3.12 Intensity-based mean absolute deviation
 * 3.3.13 Intensity-based robust mean absolute deviation
 * 3.3.14 Intensity-based median absolute deviation
 * 3.3.15 Intensity-based coefficient of variation
 * 3.3.16 Intensity-based quartile coefficient of dispersion
 * 3.3.17 Intensity-based energy
 * 3.3.18 Root mean square intensity
 * 
 * Attention
 * DO NOT standardization.
 */
public class IntensityBasedStatisticalFeatures {
	
	/**
	 * for Energy, TotalEnergy, RMS.
	 * same as voxel array shift.
	 * If using CT data, or data normalized with mean 0, consider setting this parameter to a fixed value (e.g. 2000) that ensures non-negative numbers in the image.
	 */
	private double densityShift = 0d;//in unit value !
	
	/**
	 * mask label value
	 */
	protected Integer label;

	ImagePlus orgImg;
	ImagePlus orgMask;
	Calibration orgCal;// backup
	
	protected double[] voxels = null;
	
	public IntensityBasedStatisticalFeatures() {
		//for IntensityHistgramFeatures
	}
	
	/**
	 * 
	 * @param img : original
	 * @param mask : label mask. if null, full face mask is applied.
	 */
	public IntensityBasedStatisticalFeatures(ImagePlus img, ImagePlus mask, int label) {
		if (img == null) {
			return;
		}
		if (img.getType() == ImagePlus.COLOR_RGB) {
			JOptionPane.showMessageDialog(null, "RadiomicsJ can read only grayscale images(8/16/32 bits)...sorry.");
			return;
		}
		
		this.label = label;
		
		if (mask != null) {
			if (img.getWidth() != mask.getWidth() || img.getHeight() != mask.getHeight()
					|| img.getNSlices() != mask.getNSlices()) {
				JOptionPane.showMessageDialog(null,
						"RadiomicsJ: please should be same dimension(w,h,s) images and masks.");
				return;
			}
		}else {
			// create full face mask
			mask = ImagePreprocessing.createMask(img.getWidth(), img.getHeight(), img.getNSlices(), null, this.label,img.getCalibration().pixelWidth, img.getCalibration().pixelHeight,img.getCalibration().pixelDepth);
		}
		
		orgImg = img;
		orgMask = mask;
		orgCal = img.getCalibration().copy();
		/*
		 * density shift value is no negative. 
		 */
		this.densityShift = RadiomicsJ.densityShift;
		if(this.densityShift < 0d) {
			System.out.println("Density shift value is should be non negative. Sorry, Use zero instead in this calculation.");
			this.densityShift = 0.0;
		}
		//get voxels and sort array.
		voxels = Utils.getVoxels(orgImg, orgMask, this.label);
	}
	
	protected int countRoiVoxel() {
		if(voxels == null) {
			voxels = Utils.getVoxels(orgImg, orgMask, this.label);
		}
		return voxels.length;
	}
	
	
	/*
	 * first order features -> Intensity based statistical features (without entropy uniformity)
	 * Intensity histogram features -> apply bins to Intensity based statistical features. and entropy uniformity.
	 */
	
	public Double calculate(String id) {
		String name = IntensityBasedStatisticalFeatureType.findType(id);
		
		if (name.equals(IntensityBasedStatisticalFeatureType.Mean.name())) {
			return getMean();
		} else if (name.equals(IntensityBasedStatisticalFeatureType.Variance.name())) {
			return getVariance();
		} else if (name.equals(IntensityBasedStatisticalFeatureType.Skewness.name())) {
			return getSkewness1();
		} else if (name.equals(IntensityBasedStatisticalFeatureType.Kurtosis.name())) {
			return getKurtosis();
		} else if (name.equals(IntensityBasedStatisticalFeatureType.Median.name())) {
			return getMedian();
		} else if (name.equals(IntensityBasedStatisticalFeatureType.Minimum.name())) {
			return getMinimum();
		} else if (name.equals(IntensityBasedStatisticalFeatureType.Percentile10.name())) {
			return getPercentile(10);
		} else if (name.equals(IntensityBasedStatisticalFeatureType.Percentile90.name())) {
			return getPercentile(90);
		} else if (name.equals(IntensityBasedStatisticalFeatureType.Maximum.name())) {
			return getMaximum();
		} else if (name.equals(IntensityBasedStatisticalFeatureType.Interquartile.name())) {
			return getInterquartileRange();
		} else if (name.equals(IntensityBasedStatisticalFeatureType.Range.name())) {
			return getMinMaxRange();
		} else if (name.equals(IntensityBasedStatisticalFeatureType.MeanAbsoluteDeviation.name())) {
			return getMeanAbsoluteDeviation();
		} else if (name.equals(IntensityBasedStatisticalFeatureType.RobustMeanAbsoluteDeviation.name())) {
			return getRobustMeanAbsoluteDeviation();
		} else if (name.equals(IntensityBasedStatisticalFeatureType.MedianAbsoluteDeviation.name())) {
			return getMedianAbsoluteDeviation();
		} else if (name.equals(IntensityBasedStatisticalFeatureType.CoefficientOfVariation.name())) {
			return getCoefficientOfVariation();
		} else if (name.equals(IntensityBasedStatisticalFeatureType.QuartileCoefficientOfDispersion.name())) {
			return getQuartileCoefficientOfDispersion();
		} else if (name.equals(IntensityBasedStatisticalFeatureType.Energy.name())) {
			return getEnergy();
		} else if (name.equals(IntensityBasedStatisticalFeatureType.RootMeanSquared.name())) {
			return getRootMeanSquared();
		} else if (name.equals(IntensityBasedStatisticalFeatureType.TotalEnergy.name())) {
			return getTotalEnergy();
		}  else if (name.equals(IntensityBasedStatisticalFeatureType.StandardDeviation.name())) {
			return getStandardDeviation();
		} else if (name.equals(IntensityBasedStatisticalFeatureType.StandardError.name())) {
			return getStandardError();
		}
		return null;
	}

	public Double getEnergy() {
		Double energy = 0d;
		int size = voxels.length;
		for(int i=0; i<size; i++) {
			double v = voxels[i] + densityShift;
			if(v != 0.0) {
				v = Math.pow(v, 2);
				energy += v;
			}
		}
		return energy;
	}

	public Double getTotalEnergy() {
		Double energy = getEnergy();
		if (energy != null) {
			double voxelSize = orgCal.pixelWidth * orgCal.pixelHeight * orgCal.pixelDepth;
			if(voxelSize > 0.0) {
				return voxelSize * energy;
			}else {
				return energy;
			}
			
		} else {
			return null;
		}
	}
	
	public Double getMinimum() {
		return StatUtils.min(voxels);
	}
	
	public Double getMaximum() {
		return StatUtils.max(voxels);
	}
	
	public Double getMean() {
		double sum = 0d;
		int numOfVoxel = voxels.length;
		for(int i=0;i<numOfVoxel;i++) {
			sum += voxels[i];
		}
		return numOfVoxel != 0d ? sum/numOfVoxel : null;
	}
	
	public Double getMedian() {
		Double median = null;
		if (voxels.length % 2 == 0)
		    median = ((double)voxels[voxels.length/2] + (double)voxels[voxels.length/2 - 1])/2;
		else
		    median = (double) voxels[voxels.length/2];
		return median;
	}
	
	public Double getMinMaxRange() {
		Double min = getMinimum();
		Double max = getMaximum();
		return max - min;
	}
	
	public Double getPercentile(int p_th) {
		if(voxels == null || voxels.length < 1) {
			return null;
		}
//		return StatUtils.percentile(voxelArray, p_th);//Compute the estimated percentile
//		return new Percentile().evaluate(voxelArray, p_th);//Compute the estimated percentile
//		int index = (int) Math.ceil((double)(p_th / 100.0) * (double)voxelArray.length);
		int index = (int) Math.floor((double)(p_th / 100.0) * (double)voxels.length);
		if(index >= voxels.length) {
			index = voxels.length-1;
		}else if(index < 0) {
			index = 0;
		}
		return voxels[index];
	}
	
	public Double getInterquartileRange() {
		return getPercentile(75) - getPercentile(25);
	}
	
	private Double getQuartileCoefficientOfDispersion(){
		if(voxels == null || voxels.length < 1) {
			return null;
		}
		int index_75 = (int) Math.floor((double)(75 / 100.0) * (double)voxels.length);
		int index_25 = (int) Math.floor((double)(25 / 100.0) * (double)voxels.length);
		if(index_75 >= voxels.length) {
			index_75 = voxels.length-1;
		}else if(index_75 < 0) {
			index_75 = 0;
		}
		if(index_25 >= voxels.length) {
			index_25 = voxels.length-1;
		}else if(index_25 < 0) {
			index_25 = 0;
		}
		double p75 = voxels[index_75];
		double p25 = voxels[index_25];
		return (p75-p25)/(p75+p25);
	}
	
	public Double getMeanAbsoluteDeviation() {
		if(voxels == null || voxels.length == 0) {
			return null;
		}
		double absSum = 0d;
		double mean = getMean();
	    for (int i = 0; i < voxels.length; i++) {
	    	absSum += Math.abs(voxels[i] - mean);
	    }
	    // Return mean absolute deviation about mean.
	    return absSum / voxels.length;
	}
	
	public Double getMedianAbsoluteDeviation() {
		if(voxels == null || voxels.length == 0) {
			return null;
		}
		double absSum = 0d;
		double median = StatUtils.percentile(voxels, 50);
	    for (int i = 0; i < voxels.length; i++) {
	    	absSum += Math.abs(voxels[i] - median);
	    }
	    // Return mean absolute deviation about mean.
	    return absSum / voxels.length;
	}
	
	public Double getRobustMeanAbsoluteDeviation() {
		if(voxels == null || voxels.length == 0) {
			return null;
		}
		double absSum = 0d;		
		ArrayList<Double> percenileArr = new ArrayList<Double>();
		double p10 = StatUtils.percentile(voxels, 10);
		double p90 = StatUtils.percentile(voxels, 90);
		for(int i=0;i<voxels.length;i++) {
			if(voxels[i] >= p10 && voxels[i] <= p90) {
				percenileArr.add(voxels[i]);
			}
		}
//		//can not cast Double[] to double[] directly.
		double[] perArr = new double[percenileArr.size()];
		int num = 0;
		for(double d:percenileArr) {
			perArr[num++] = d;
		}
		double mean = StatUtils.mean(perArr);
	    for (int i = 0; i < perArr.length; i++) {
	    	absSum += Math.abs(perArr[i] - mean);
	    }
	    // Return mean absolute deviation about mean.
	    return absSum / perArr.length;
	}
	
	public Double getRootMeanSquared() {
		if(voxels == null || voxels.length == 0) {
			return null;
		}
		/*
		 * this is because to considering densityShift,
		 * use getEnergy.
		 */
		return Math.sqrt(getEnergy()/voxels.length);
	}
	
	public Double getStandardDeviation() {
		if(voxels == null || voxels.length == 0) {
			return null;
		}
		double n = voxels.length;
		double sumsq = 0d;
		double mean = StatUtils.mean(voxels);
		for(double v : voxels) {
			sumsq += Math.pow(v-mean, 2);
		}
//		StatUtils.variance(voxels);//do not use...
		double var = sumsq/n;
		return Math.sqrt(var);
	}
	
	public Double getStandardError() {
		if(voxels == null || voxels.length == 0) {
			return null;
		}
		double n = voxels.length;
		double sumsq = 0d;
		double mean = StatUtils.mean(voxels);
		for(double v : voxels) {
			sumsq += Math.pow(v-mean, 2);
		}
//		StatUtils.variance(voxels);//do not use...
		double var = sumsq/n;
		double sd = Math.sqrt(var);
		return sd/Math.sqrt(voxels.length);
	}
	
	public Double getCoefficientOfVariation() {
		Double stde = getStandardDeviation();
		Double mean = getMean();
		return stde/mean;
	}
	
	public Double getVariance() {
		if(voxels == null || voxels.length == 0) {
			return null;
		}
		double n = voxels.length;
		double sumsq = 0d;
		double mean = StatUtils.mean(voxels);
		for(double v : voxels) {
			sumsq += Math.pow(v-mean, 2);
		}
//		return StatUtils.variance(voxels);//do not use...
		return sumsq/n;
	}
	
	public Double getSkewness1() {
		int pixelCount = voxels.length;
		double dv, dv2, sum2=0.0, sum3=0.0;
  		for(int z=0; z<pixelCount; z++) {
  			dv = voxels[z];
  			dv2 = dv*dv;
  			sum2 += dv2;
  			sum3 += dv*dv2;
  		}
  		double mean = StatUtils.mean(voxels);
  	    double mean2 = mean*mean;
  	    double variance = sum2/pixelCount - mean2;
  	    double sDeviation = Math.sqrt(variance);
  	    Double ij_skewness = ((sum3 - 3.0*mean*sum2)/(double)pixelCount + 2.0*mean*mean2)/(variance*sDeviation);
	    return ij_skewness;
	}
	
	/**
	 * get same result of getSkewness1()
	 * @return
	 */
	public Double getSkewness2() {
		if(voxels == null || voxels.length ==0) {
			return null;
		}
		int pixelCount = voxels.length;
		double mean = StatUtils.mean(voxels);
		double sum2 = 0d;//sum of pow(v-mean,2) 
		double sum3 = 0d;//sum of pow(v-mean,3)
		for(int i=0;i<pixelCount;i++) {
			sum2 += Math.pow((voxels[i]-mean), 2);
			sum3 += Math.pow((voxels[i]-mean), 3);
		}
//		System.out.println("skewness1 : "+(sum3/pixelCount)/Math.pow(Math.sqrt(sum2/pixelCount), 3));//pyradiomics, same result of IJ
//		System.out.println("skewness1 : "+(sum3/pixelCount)/Math.pow(sum2/(pixelCount+0.0), 3.0/2.0));//IBSI, really almost same as IJ.
		return (sum3/(double)pixelCount)/Math.pow(Math.sqrt(sum2/(double)pixelCount), 3);
	}
	
	/**
	 * same as ( getKurtosis() - 3.0 )
	 * @return
	 */
	public Double getExcessKurtosis() {
		if(voxels == null || voxels.length ==0) {
			return null;
		}
		int pixelCount = voxels.length;
		double dv, dv2, sum2=0.0, sum3=0.0, sum4=0.0;
		for(int i=0; i<pixelCount; i++) {
			dv = voxels[i];
			dv2 = dv*dv;
			sum2 += dv2;
			sum3 += dv*dv2;
			sum4 += dv2*dv2;
		}
		double mean = StatUtils.mean(voxels);
	    double mean2 = mean*mean;
	    double variance = sum2/pixelCount - mean2;
	    return (((sum4 - 4.0*mean*sum3 + 6.0*mean2*sum2)/pixelCount - 3.0*mean2*mean2)/(variance*variance)-3.0);
	}
	
	public Double getKurtosis() {
		if(voxels == null || voxels.length ==0) {
			return null;
		}
		int pixelCount = voxels.length;
		double sum2=0.0, sum4=0.0;
		double mean = StatUtils.mean(voxels);
		for(int i=0;i<pixelCount;i++) {
			sum2 += Math.pow((voxels[i]-mean), 2);
			sum4 += Math.pow((voxels[i]-mean), 4);
		}
	    return (sum4/pixelCount) / Math.pow(sum2/pixelCount,2);
	}
}
