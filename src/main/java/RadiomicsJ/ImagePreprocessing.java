package RadiomicsJ;

import javax.swing.JOptionPane;

import org.apache.commons.math3.stat.StatUtils;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

/**
 * 
 * FloatProcessor is recommended. 
 * If you use ip.duplicate(), should pay attention to set calibration again.
 * 
 * @author tatsunidas
 *
 */
public class ImagePreprocessing {
	
	public static boolean checkMask(ImagePlus imp, ImagePlus mask) {
		
		if(imp == null || mask == null) {
			System.out.println("RadiomicsJ:ImagePreprocessing::checkMask failed.");
			System.out.println("Image or mask is null.");
			return false;
		}
		
		int iw = imp.getWidth();
		int ih = imp.getHeight();
		int is = imp.getNSlices();
		
		int mw = mask.getWidth();
		int mh = mask.getHeight();
		int ms = mask.getNSlices();
		
		if(iw != mw || ih != mh || is != ms) {
			System.out.println("RadiomicsJ:ImagePreprocessing::checkMask failed.");
			System.out.println("Stack size incorrect;");
			System.out.println("img(w,h,s):"+iw+" "+ih+" "+is);
			System.out.println("mask(w,h,s):"+mw+" "+mh+" "+ms);
			return false;
		}
		
		if(!Utils.isValidMaskLabel(RadiomicsJ.targetLabel)) {
			System.out.println("RadiomicsJ:ImagePreprocessing::checkMask failed.");
			System.out.println("Mask label is incorrect. please set to 1 to 255.");
			return false;
		}
		return true;//clear
	}
	
	/**
	 * create mask(float) that labeled inner roi with specified label.
	 * if roiset null, return full face mask with label.
	 */
	public static ImagePlus createMask(int w, int h, int numOfSlices, Roi[] roiset, Integer label, double vx, double vy, double vz) {
		if(roiset != null && numOfSlices != roiset.length) {
			JOptionPane.showMessageDialog(null, "RadiomicsJ: please input same size images and roiset (e.g, 3 slice and 3 roi). return null.");
			return null;
		}
		if(label == null) {
			System.err.println("RadiomicsJ:ImagePreprocessing:getMask::Please specify mask label value...");
			return null;
		}
		/*
		 * create mask
		 */
		ImageStack maskStack = new ImageStack(w, h);
		for(int i=0;i<numOfSlices;i++) {
			Roi roi = null;
			if(roiset != null) {
				roi = roiset[i];
			}else {
				roi = new Roi(0,0,w,h);//blank rect roi
			}
			ImageProcessor fp = new FloatProcessor(w, h);// all pixels 0 intensity.
			if(roi != null) {
				for(int col=0;col<w;col++) {
					for(int row=0;row<h;row++) {
						if(roi.contains(col, row)) {
							fp.setf(col, row, label);
						}
					}
				}
			}
			fp.setInterpolationMethod(RadiomicsJ.interpolation2D);
			maskStack.addSlice(fp);
		}
		Calibration cal = new Calibration();
		cal.pixelWidth = vx;
		cal.pixelHeight = vy;
		cal.pixelDepth = vz;
		ImagePlus mask = new ImagePlus("mask", maskStack);
		mask.setCalibration(cal);
		return mask;
	}
	
	
	/**
	 * get resegmented mask using specified label values.
	 * 
	 * please also check RadiomicsJ.label.
	 * RadiomicsJ using RadiomicsJ.label value when calculations.
	 * 
	 * @param mask:single image or stack.
	 * @param roiset:null-able. this roiset must has same size to mask stack size. if no roi, set to null for the paired mask processor position. 
	 * @param label
	 * @return
	 */
	public static ImagePlus getResegmentedMask(ImagePlus mask, Roi[] roiset, Integer label) {
		if(mask == null) {
			System.out.println("ImagePreprocessing:getResegmentedMask::Mask Null Error");
			return null;
		}
		if(roiset != null && roiset.length != 0) {
			if(roiset.length != mask.getNSlices()) {
				System.out.println("ImagePreprocessing:getResegmentedMask::Roi Error");
				System.out.println("This roiset size does not match mask stack size.");
				System.out.println("roiset size:"+roiset.length+", mask stack size:"+mask.getNSlices());
				return null;
			}
			//to allow point roi.
//			for(Roi r : roiset) {
//				if(!r.isArea()) {
//					System.out.println("ImagePreprocessing:getResegmentedMask::Roi Error");
//					System.out.println("This roi is not area type. can not processing.");
//					System.out.println(r.getName());
//					return null;
//				}
//			}
		}
		int w = mask.getWidth();
		int h = mask.getHeight();
		int s = mask.getNSlices();
		ImageStack stack = new ImageStack(w,h);
		for(int z=0; z<s; z++) {
			ImageProcessor mp = mask.getStack().getProcessor(z+1);
			ImageProcessor mp2 = new FloatProcessor(w,h);
			Roi roi = null;
			if(roiset != null) {
				roi = roiset[z];
			}
			for (int row = 0; row < h; row++) {
				for (int col = 0; col < w; col++) {
					int val = (int) mp.getf(col, row);
					if (roi != null) {
						if (roi.contains(col, row)) {
							if (val == label) {
								mp2.setf(col, row, label);
							} else {
								mp2.setf(col, row, 0);
							}
						} else {
							mp2.setf(col, row, 0);
						}
					}else {
						if (val == label) {
							mp2.set(col, row, label);
						} else {
							mp2.set(col, row, 0);
						}
					}
				}
			}
			stack.addSlice(mp2);
		}
		ImagePlus resegmented = new ImagePlus("mask with label ("+label+")", stack);
		Calibration cal = mask.getCalibration();
		if(cal != null) {
			resegmented.setCalibration(cal.copy());
		}
		return resegmented;
	}
	
	//TODO
//	public static ImagePlus cropToTumorMask() {
//		
//	}
	
	/**
	 * calculate standardized matrix.
	 * @param imp
	 * @return
	 */
	public static ImagePlus normalizeImageSliceBySlice(ImagePlus imp, ImagePlus mask, int label) {
		double scale = RadiomicsJ.normalizeScale;
		boolean removeOutlier = RadiomicsJ.removeOutliers;
		double z_score = RadiomicsJ.zScore;
		int w = imp.getWidth();
		int h = imp.getHeight();
		int s = imp.getNSlices();
		Calibration cal_i = imp.getCalibration().copy();
		Calibration cal_m = mask.getCalibration().copy();
		ImageStack stack = new ImageStack(w,h);//normalized stack
		for(int z=0;z<s;z++) {
			ImagePlus slice_i = new ImagePlus("i_"+(z+1), imp.getStack().getProcessor(z+1).duplicate());
			slice_i.setCalibration(cal_i.copy());
			ImagePlus slice_m = new ImagePlus("m_"+(z+1), mask.getStack().getProcessor(z+1).duplicate());
			slice_m.setCalibration(cal_m.copy());
			double[] pixels = Utils.getVoxels(slice_i, slice_m, label);
			double mean = StatUtils.mean(pixels);
			double variance = StatUtils.variance(pixels);
			double stdDev = Math.sqrt(variance);
			double outlierUpper = mean + z_score*stdDev;
			double outlierLower = mean - z_score*stdDev;
			//ImagePlus array is array[w][h]
			float[][] stdValues = new float[w][h];
			for(int y=0;y<h;y++) {
				for(int x=0;x<w;x++) {
					// col is x, row is y.
					float v = (float)((slice_i.getProcessor().getPixelValue(x, y)-mean)/stdDev);
					if(removeOutlier) {
						if (v > outlierUpper) {
							v = (float)outlierUpper;
						}else if(v < outlierLower) {
							v = (float)outlierLower;
						}
						stdValues[x][y] = v * (float)scale;//finally, apply scale
					}else {
						stdValues[x][y] = v * (float)scale;
					}
				}
			}
			stack.addSlice(new FloatProcessor(stdValues));
		}
		cal_i.disableDensityCalibration();
		ImagePlus std_imp = new ImagePlus("", stack);
		std_imp.setCalibration(cal_i);
		return std_imp;
	}
	
	public static ImagePlus normalize(ImagePlus imp, ImagePlus mask, int label) {
		if(mask == null) {
			Calibration cal = imp.getCalibration();
			mask = createMask(imp.getWidth(), imp.getHeight(), imp.getNSlices(), null, label, cal.pixelWidth, cal.pixelHeight,cal.pixelDepth);
		}
		double scale = RadiomicsJ.normalizeScale;
		int w = imp.getWidth();
		int h = imp.getHeight();
		int s = imp.getNSlices();
		double[] voxels = Utils.getVoxels(imp, mask, label);
		double mean = StatUtils.mean(voxels);
		double stdDev = Math.sqrt(StatUtils.variance(voxels));
		ImageStack stack_imp = new ImageStack(w,h);
		for(int z=0;z<s;z++) {
			FloatProcessor ip = imp.getStack().getProcessor(z+1).convertToFloatProcessor();
			FloatProcessor mp = mask.getStack().getProcessor(z+1).convertToFloatProcessor();
			FloatProcessor fp = new FloatProcessor(w, h);
			for(int y=0;y<h;y++) {
				for(int x=0;x<w;x++) {
					int lbl = (int) mp.getf(x, y);
					if(lbl != label) {
						fp.setf(x, y, Float.NaN);
						continue;
					}
					float v = ip.getf(x, y);
					float sv = (float)((v-mean)/stdDev);
					fp.setf(x, y, (sv * (float)scale));//apply scaling
				}
			}
			stack_imp.addSlice(fp);
		}
		Calibration cal = imp.getCalibration().copy();
		cal.disableDensityCalibration();
		ImagePlus std_imp = new ImagePlus("normalized", stack_imp);
		std_imp.setCalibration(cal);
		return std_imp;
	}
	
	/**
	 * 
	 * @param imp
	 * @param mask
	 * @return : standardized image, with replacing outlier min max.
	 */
	@Deprecated
	public static ImagePlus[] normalizeAndRemoveOutliers(ImagePlus imp, ImagePlus mask, int label) {
		if(mask == null) {
			Calibration cal = imp.getCalibration();
			mask = createMask(imp.getWidth(), imp.getHeight(), imp.getNSlices(), null, label,cal.pixelWidth, cal.pixelHeight,cal.pixelDepth);
		}
		double scale = RadiomicsJ.normalizeScale;
		boolean removeOutlier = RadiomicsJ.removeOutliers;
		double z_score = RadiomicsJ.zScore;
		int w = imp.getWidth();
		int h = imp.getHeight();
		int s = imp.getNSlices();
		double[] voxels = Utils.getVoxels(imp, mask, label);
		double mean = StatUtils.mean(voxels);
		double stdDev = Math.sqrt(StatUtils.variance(voxels));
		double outlierUpper = mean + z_score*stdDev;
		double outlierLower = mean - z_score*stdDev;
		ImageStack stack_imp = new ImageStack(w,h);
		ImageStack stack_mask = new ImageStack(w,h);
		for(int z=0;z<s;z++) {
			FloatProcessor fp = new FloatProcessor(w, h);
			ByteProcessor bp = new ByteProcessor(w, h);
			for(int y=0;y<h;y++) {
				for(int x=0;x<w;x++) {
					int lbl = mask.getStack().getProcessor(z+1).getPixel(x, y);
					if(lbl != label) {
						fp.setf(x, y, Float.NaN);
						bp.set(x, y, 0);
						continue;
					}
					float v = imp.getStack().getProcessor(z+1).getPixelValue(x, y);
					float sv = (float)((v-mean)/stdDev);
					if(removeOutlier) {
						int mv = 1; //mask value
						if (v > outlierUpper) {
							v = (float)outlierUpper;
							mv = 0;
						}else if(v < outlierLower) {
							v = (float)outlierLower;
							mv = 0;
						}
						fp.setf(x, y, (sv * (float)scale));//apply scaling
						bp.set(x, y, mv);
					}else {
						fp.setf(x, y, (sv * (float)scale));//apply scaling
						bp.set(x, y, lbl);
					}
				}
			}
			stack_imp.addSlice(fp);
			stack_mask.addSlice(bp);
		}
		Calibration cal = imp.getCalibration().copy();
		cal.disableDensityCalibration();
		ImagePlus std_imp = new ImagePlus("normalized", stack_imp);
		std_imp.setCalibration(cal);
		ImagePlus newMask = new ImagePlus("normalized_mask", stack_mask);
		newMask.setCalibration(cal);
		return new ImagePlus[] {std_imp, newMask};
	}
	
	/**
	 * keep original pixels values. but replace upper and lower values using z-score. 
	 * @param imp
	 * @param mask
	 * @param label
	 * @return
	 */
	public static ImagePlus outlierFiltering(ImagePlus imp, ImagePlus mask, int label) {
		if(mask == null) {
			Calibration cal = imp.getCalibration();
			mask = createMask(imp.getWidth(), imp.getHeight(), imp.getNSlices(), null, label, cal.pixelWidth, cal.pixelHeight,cal.pixelDepth);
		}
		double z_score = RadiomicsJ.zScore;
		int w = imp.getWidth();
		int h = imp.getHeight();
		int s = imp.getNSlices();
		double[] voxels = Utils.getVoxels(imp, mask, label);
		double mean = StatUtils.mean(voxels);
		double n = voxels.length;
		double sumsq = 0d;
		for(double v : voxels) {
			sumsq += Math.pow(v-mean, 2);
		}
		double stdDev = Math.sqrt(sumsq/n);
//		double stdDev = Math.sqrt(StatUtils.variance(voxels));//almost same 
		
		double outlierUpper = mean + z_score*stdDev;//calibrated
		double outlierLower = mean - z_score*stdDev;//calibrated
//		double maxRaw = imp.getCalibration().getRawValue(outlierUpper);//back scale to raw
//		double minRaw = imp.getCalibration().getRawValue(outlierLower);
		ImageStack stack_mask = new ImageStack(w,h);
		for(int z=0;z<s;z++) {
			ImageProcessor iip = imp.getStack().getProcessor(z+1);
			ImageProcessor mip = mask.getStack().getProcessor(z+1);
			//dup is a new image, so without calibaration is OK at here.
			ImageProcessor dup = mip.duplicate().convertToFloat();
			for(int y=0;y<h;y++) {
				for(int x=0;x<w;x++) {
					int lbl = (int)mip.getf(x, y);//IJ coordinate
					float v = iip.getf(x, y);
					if(lbl != label || v > outlierUpper || v < outlierLower) {
						dup.setf(x, y, 0);
					}else if(lbl == label){
						dup.setf(x, y, 1);
					}else {
						dup.setf(x, y, 0);
					}
				}
			}
			stack_mask.addSlice(dup);
		}
		Calibration mCal = mask.getCalibration().copy();
		mCal.disableDensityCalibration();
		ImagePlus newMask = new ImagePlus("removeOutlier_mask", stack_mask);
		newMask.setCalibration(mCal);
		IJ.saveAsTiff(newMask, "removeoutliers.tif");
		return newMask;
	}
	
	public static ImagePlus rangeFiltering(ImagePlus imp, ImagePlus mask, int label, double rangeMax, double rangeMin) {
		if(mask == null) {
			System.out.println("RangeFiltering can not allow null mask.");
			return null;
		}
		if(rangeMax <= rangeMin) {
			System.out.println("RangeFiltering can not performed. range min is larger than max.");
			return mask;
		}
		int w = imp.getWidth();
		int h = imp.getHeight();
		int s = imp.getNSlices();
		ImageStack stack_mask = new ImageStack(w,h);
		int included = 0;
		int excluded = 0;
		for(int z=0;z<s;z++) {
			ImageProcessor iarr = imp.getStack().getProcessor(z+1);
			ImageProcessor marr = mask.getStack().getProcessor(z+1);
			FloatProcessor m2 = new FloatProcessor(w, h);
			for(int y=0;y<h;y++) {
				for(int x=0;x<w;x++) {
					int lbl = (int)marr.getf(x,y);
					float v = iarr.getf(x,y);
					if(lbl != label) {
						m2.setf(x, y, 0);
					}else {
						if(!(v <= rangeMin) && !(v >= rangeMax)) {
							m2.setf(x, y, lbl);
							included++;
						}else {
							m2.setf(x, y, 0);
							excluded++;
						}
					}
				}
			}
			stack_mask.addSlice(m2);
		}
		Calibration cal = imp.getCalibration().copy();
		cal.disableDensityCalibration();
		ImagePlus newMask = new ImagePlus("range_mask", stack_mask);
		newMask.setCalibration(cal);
		System.out.println("RangeFiler : filtered roi points "+included+", and exclude "+excluded+" voxels on mask.");
		return newMask;
	}
}
