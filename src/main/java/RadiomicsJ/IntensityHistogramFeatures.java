package RadiomicsJ;

import javax.swing.JOptionPane;
import ij.ImagePlus;

/**
 * numOfBins is required.
 * 
 * 3.4.1 Mean discretised intensity X6K6
 * 3.4.2 Discretised intensity variance CH89
 * 3.4.3 Discretised intensity skewness 88K1
 * 3.4.4 (Excess) discretised intensity kurtosis C3I7
 * 3.4.5 Median discretised intensity WIFQ
 * 3.4.6 Minimum discretised intensity 1PR8
 * 3.4.7 10th discretised intensity percentile GPMT
 * 3.4.8 90th discretised intensity percentile OZ0C
 * 3.4.9 Maximum discretised intensity 3NCY
 * 3.4.10 Intensity histogram mode AMMC
 * 3.4.11 Discretised intensity interquartile range WR0O
 * 3.4.12 Discretised intensity range (max-min) 5Z3W
 * 3.4.13 Intensity histogram mean absolute deviation D2ZX
 * 3.4.14 Intensity histogram robust mean absolute deviation WRZB
 * 3.4.15 Intensity histogram median absolute deviation 4RNL
 * 3.4.16 Intensity histogram coefficient of variation CWYJ
 * 3.4.17 Intensity histogram quartile coefficient of dispersion SLWD
 * 3.4.18 Discretised intensity entropy TLU2
 * 3.4.19 Discretised intensity uniformity BJ5W
 * 3.4.20 Maximum histogram gradient 12CE
 * 3.4.21 Maximum histogram gradient intensity 8E6O
 * 3.4.22 Minimum histogram gradient VQB3
 * 3.4.23 Minimum histogram gradient intensity RHQZ
 * 
 * @author tatsunidas
 *
 */
public class IntensityHistogramFeatures extends IntensityBasedStatisticalFeatures{

	/*
	 * image and mask are located at parent class.
	 */
	protected Integer nBins = null;
	int[] hist = null;//3d basis

	/**
	 * 
	 * @param img
	 * @param mask
	 * @param nBins:if you want to use one by one step histogram, Utils.getNumOfBinsByMinMaxRange()
	 * @throws Exception
	 */
	public IntensityHistogramFeatures(ImagePlus img, ImagePlus mask, Integer label, boolean useBinCount, Integer nBins, Double binWidth) throws Exception {
		super(img,mask,label);
		if (img == null) {
			JOptionPane.showMessageDialog(null, "RadiomicsJ: please input none null img...");
			return;
		}
		if (img.getType() == ImagePlus.COLOR_RGB) {
			JOptionPane.showMessageDialog(null, "RadiomicsJ can read only grayscale images(8/16/32 bits)...sorry.");
			return;
		}
		if(nBins == null) {
			this.nBins = RadiomicsJ.nBins;
		}else {
			this.nBins = nBins;
		}
		if(binWidth == null) {
			binWidth = RadiomicsJ.binWidth;
		}
		/*
		 * create mask
		 */
		if(mask != null) {
			if (img.getWidth() != mask.getWidth() || img.getHeight() != mask.getHeight() || img.getNSlices() != mask.getNSlices()) {
				JOptionPane.showMessageDialog(null, "RadiomicsJ: please input same dimension image and mask.");
				return;
			}
			//super.IntensityBasedStatisticalFeatures variables
			orgMask = Utils.createMaskCopy(mask);
		}else {
			orgMask = ImagePreprocessing.createMask(img.getWidth(), img.getHeight(), img.getNSlices(), null, super.label,img.getCalibration().pixelWidth, img.getCalibration().pixelHeight,img.getCalibration().pixelDepth);
		}
		/*
		 * super.IntensityBasedStatisticalFeatures variables
		 * calibrations already set in super objects.
		 */
		if(useBinCount) {
			orgImg = Utils.discrete(img, orgMask, super.label, this.nBins);
		}else {
			orgImg = Utils.discreteByBinWidth(img, orgMask, super.label, binWidth);
		}
		//replace discretised voxels
		super.voxels = Utils.getVoxels(orgImg, orgMask, this.label);
		hist = Utils.getHistogram(voxels);
	}
	

	public Double calculate(String id) {
		String name = IntensityHistogramFeatureType.findType(id);
		if(name.equals(IntensityHistogramFeatureType.MeanDiscretisedIntensity.name())) {
			return super.calculate(IntensityBasedStatisticalFeatureType.Mean.id());
		}else if (name.equals(IntensityHistogramFeatureType.Variance.name())) {
			return super.calculate(IntensityBasedStatisticalFeatureType.Variance.id());
		}else if (name.equals(IntensityHistogramFeatureType.Skewness.name())) {
			return super.calculate(IntensityBasedStatisticalFeatureType.Skewness.id());
		}else if (name.equals(IntensityHistogramFeatureType.Kurtosis.name())) {
			return super.calculate(IntensityBasedStatisticalFeatureType.Kurtosis.id());
		}else if (name.equals(IntensityHistogramFeatureType.Median.name())) {
			return super.calculate(IntensityBasedStatisticalFeatureType.Median.id());
		}else if (name.equals(IntensityHistogramFeatureType.Minimum.name())) {
			return super.calculate(IntensityBasedStatisticalFeatureType.Minimum.id());
		}else if (name.equals(IntensityHistogramFeatureType.Percentile10.name())) {
			return super.calculate(IntensityBasedStatisticalFeatureType.Percentile10.id());
		}else if (name.equals(IntensityHistogramFeatureType.Percentile90.name())) {
			return super.calculate(IntensityBasedStatisticalFeatureType.Percentile90.id());
		}else if (name.equals(IntensityHistogramFeatureType.Maximum.name())) {
			return super.calculate(IntensityBasedStatisticalFeatureType.Maximum.id());
		}else if (name.equals(IntensityHistogramFeatureType.Interquartile.name())) {
			return super.calculate(IntensityBasedStatisticalFeatureType.Interquartile.id());
		}else if (name.equals(IntensityHistogramFeatureType.Range.name())) {
			return super.calculate(IntensityBasedStatisticalFeatureType.Range.id());
		}else if (name.equals(IntensityHistogramFeatureType.MeanAbsoluteDeviation.name())) {
			return super.calculate(IntensityBasedStatisticalFeatureType.MeanAbsoluteDeviation.id());
		}else if (name.equals(IntensityHistogramFeatureType.RobustMeanAbsoluteDeviation.name())) {
			return super.calculate(IntensityBasedStatisticalFeatureType.RobustMeanAbsoluteDeviation.id());
		}else if (name.equals(IntensityHistogramFeatureType.MedianAbsoluteDeviation.name())) {
			return super.calculate(IntensityBasedStatisticalFeatureType.MedianAbsoluteDeviation.id());
		}else if (name.equals(IntensityHistogramFeatureType.CoefficientOfVariation.name())) {
			return super.calculate(IntensityBasedStatisticalFeatureType.CoefficientOfVariation.id());
		}else if (name.equals(IntensityHistogramFeatureType.QuartileCoefficientOfDispersion.name())) {
			return super.calculate(IntensityBasedStatisticalFeatureType.QuartileCoefficientOfDispersion.id());
		}
		//histogram basis
		else if (name.equals(IntensityHistogramFeatureType.Mode.name())) {
			return getMode();
		}else if (name.equals(IntensityHistogramFeatureType.Entropy.name())) {
			return getEntropy();
		}else if (name.equals(IntensityHistogramFeatureType.Uniformity.name())) {
			return getUniformity();
		}else if (name.equals(IntensityHistogramFeatureType.MaximumHistogramGradient.name())) {
			return getMaximumHistogramGradient();
		}else if (name.equals(IntensityHistogramFeatureType.MaximumHistogramGradientIntensity.name())) {
			return getMaximumHistogramGradientIntensity();
		}else if (name.equals(IntensityHistogramFeatureType.MinimumHistogramGradient.name())) {
			return getMinimumHistogramGradient();
		}else if (name.equals(IntensityHistogramFeatureType.MinimumHistogramGradientIntensity.name())) {
			return getMinimumHistogramGradientIntensity();
		}
		return super.calculate(id);
	}
	
	
	private Double getMode() {
		if(hist == null) {
			return null;
		}
		Double mode = 0d;
		Double histMax = 0d;
		int ind = 1;
		for(int c : hist) {
			if(c > histMax) {
				histMax = (double)c;
				mode = (double)ind;
			}
			ind++;
		}
		return mode;
	}

	/*
	 * https://github.com/cmbruns/fiji-plugins/blob/master/Shannon_Entropy/
	 * Shannon_Entropy.java
	 * 
	 * binWidth or binCount
	 * TODO : Roi and mask ...
	 * 
	 */
	private Double getEntropy() {
		if(hist == null) {
			return null;
		}
		double totalpixel = voxels.length;
		Double ent = 0d;
		for(int i=0;i<hist.length;i++) {
			if(hist[i] > 0) {
				double p = (double)hist[i]/(double)totalpixel;
				if(p == 0.0) {
					continue;
				}
				ent -= p * ((Math.log(p)/Math.log(2.0)));
			}
		}
		return ent;
	}
	
	/**
	 * Note that this feature is sometimes referred to as energy.
	 * @return
	 */
	public Double getUniformity() {
		double totalpixel = voxels.length;
		Double uni = 0d;
		for(int i=0;i<hist.length;i++) {
			if(hist[i] > 0) {
				double p = (double)hist[i]/(double)totalpixel;
				uni += Math.pow(p, 2);
			}
		}
		return uni;
	}
	
	
	private Double getMaximumHistogramGradient() {
		double maxH = 0d;
		double histGrad = 0d;
		for(int i=0;i<hist.length;i++) {
			if(i==0) {
				histGrad = hist[i+1] - hist[i];
				if(maxH < histGrad) {
					maxH = histGrad;
				}
				continue;
			}
			if(i==hist.length-1) {
				histGrad = hist[hist.length-1] - hist[hist.length-2];
				if(maxH < histGrad) {
					maxH = histGrad;
				}
				break;
			}
			histGrad = (hist[i+1] - hist[i-1])/2;
			if(maxH < histGrad) {
				maxH = histGrad;
			}
		}
		return maxH;
	}
	
	private Double getMaximumHistogramGradientIntensity() {
		double maxH = 0d;
		double histGrad = 0d;
		double index = 0;
		for(int i=0;i<hist.length;i++) {
			if(i==0) {
				histGrad = hist[i+1] - hist[i];
				if(maxH < histGrad) {
					maxH = histGrad;
					index = (i+1d);
				}
				continue;
			}
			if(i==hist.length-1) {
				histGrad = hist[hist.length-1] - hist[hist.length-2];
				if(maxH < histGrad) {
					maxH = histGrad;
					index = (i+1d);
				}
				break;
			}
			histGrad = (hist[i+1] - hist[i-1])/2;
			if(maxH < histGrad) {
				maxH = histGrad;
				index = (i+1d);
			}
		}
		return index;
	}
	
	private Double getMinimumHistogramGradient() {
		double minH = 0d;
		double histGrad = 0d;
		for(int i=0;i<hist.length;i++) {
			if(i==0) {
				histGrad = hist[i+1] - hist[i];
				if(minH > histGrad) {
					minH = histGrad;
				}
				continue;
			}
			if(i==hist.length-1) {
				histGrad = hist[hist.length-1] - hist[hist.length-2];
				if(minH > histGrad) {
					minH = histGrad;
				}
				break;
			}
			histGrad = (hist[i+1] - hist[i-1])/2;
			if(minH > histGrad) {
				minH = histGrad;
			}
		}
		return minH;
	}
	
	private Double getMinimumHistogramGradientIntensity() {
		double minH = 0d;
		double histGrad = 0d;
		double index = 0d;
		for(int i=0;i<hist.length;i++) {
			if(i==0) {
				histGrad = hist[i+1] - hist[i];
				if(minH > histGrad) {
					minH = histGrad;
					index = (i+1d);
				}
				continue;
			}
			if(i==hist.length-1) {
				histGrad = hist[hist.length-1] - hist[hist.length-2];
				if(minH > histGrad) {
					minH = histGrad;
					index = (i+1d);
				}
				break;
			}
			histGrad = (hist[i+1] - hist[i-1])/2;
			if(minH > histGrad) {
				minH = histGrad;
				index = (i+1d);
			}
		}
		return index;
	}
}