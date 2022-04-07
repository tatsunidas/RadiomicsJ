package RadiomicsJ;

import java.awt.Font;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.StringTokenizer;

import org.apache.commons.math3.stat.StatUtils;
import org.scijava.vecmath.Point3i;

import fiji.plugins.Reslice_Z;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Plot;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.measure.ResultsTable;
import ij.plugin.Duplicator;
import ij.plugin.filter.ThresholdToSelection;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;

public class Utils {
	
	public static boolean isValidNumOfBinsForGrayScale(int type, int numOfBins) {
		if (type == ImagePlus.GRAY8) {
			if (numOfBins > 0 && numOfBins <= (int) Math.pow(2, 8)) {
				return true;
			}
		} else if (type == ImagePlus.GRAY16) {
			if (numOfBins > 0 && numOfBins <= (int) Math.pow(2, 16)) {
				return true;
			}
		} else if (type == ImagePlus.GRAY32) {
			if (numOfBins > 0 && numOfBins <= (int) Math.pow(2, 32)) {
				return true;
			}
		} else {
			return false;
		}
		return false;
	}
	
	public static boolean isValidWeightingNormName(String val) {
		if(val == null) {
			return false;
		}
		if(val.length()<1) {
			return false;
		}
		val = val.toLowerCase();
		for(String wn:RadiomicsJ.weighting_norms) {
			if(val.equals(wn)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Label value is handled in int.
	 * @param label
	 * @return
	 */
	public static boolean isValidMaskLabel(int label) {
		if(label < 1) {
			return false;
		}
		if(label > 255) {
			return false;
		}
		return true;
	}
	
	
	public static boolean isBlankMaskSlice(ImageProcessor ip, int label) {
		int w = ip.getWidth();
		int h = ip.getHeight();
		for(int y=0;y<h;y++) {
			for(int x=0;x<w;x++) {
				double v = ip.getPixelValue(x, y);
				if((int)v == label) {
					return false;
				}
			}
		}
		return true;
	}
	
	public static boolean isBlankMaskStack(ImagePlus imp, int label) {
		int s = imp.getNSlices();
		for(int z=0;z<s;z++) {
			if(!isBlankMaskSlice(imp.getStack().getProcessor(z+1), label)){
				return false;
			}
		}
		return true;
	}
	
	
	public static ImagePlus createImageCopy(ImagePlus imp) {
		ImagePlus dup = new Duplicator().run(imp);
		int s = dup.getNSlices();
		for(int z=0;z<s;z++) {
			ImageProcessor ip = dup.getStack().getProcessor(z+1);
			ip.setInterpolate(true);
			ip.setInterpolationMethod(RadiomicsJ.interpolation2D);
		}
		return dup;
	}
	
	public static ImagePlus initMaskAsFloatAndConvertLabelOne(ImagePlus mask, Integer targetLabel) {
		Calibration cal = mask.getCalibration().copy();
		/*
		 * to avoid : Wrong type for this stack
		 */
		ImageStack fStack = new ImageStack(mask.getWidth(), mask.getHeight());
		int w = mask.getWidth();
		int h = mask.getHeight();
		int s = mask.getNSlices();
		for(int z=0;z<s;z++) {
			ImageProcessor mp = mask.getStack().getProcessor(z+1).duplicate();
			mp.setCalibrationTable(cal.getCTable());
			FloatProcessor fp = (FloatProcessor) mp.convertToFloat();//using with density calibration
			fp.setInterpolate(true);
			fp.setInterpolationMethod(RadiomicsJ.interpolation2D);
			for(int y=0;y<h;y++) {
				for(int x=0;x<w;x++) {
					if(Float.isNaN(fp.getf(x, y))) {
						fp.setf(x, y, 0);
						continue;
					}
					if(targetLabel == (int)fp.getf(x, y)) {
						fp.setf(x, y,RadiomicsJ.label_);
					}else {
						fp.setf(x, y, 0);
					}
				}
			}
			fStack.addSlice(fp);
		}
		ImagePlus dupFloat = new ImagePlus("mask_init", fStack);
		cal.disableDensityCalibration();
		dupFloat.setCalibration(cal);
		return dupFloat;
	}
	
	public static ImagePlus createImageCopyAsFloat(ImagePlus imp , boolean isMask) {
		Calibration cal = imp.getCalibration().copy();
		/*
		 * to avoid : Wrong type for this stack
		 */
		ImageStack fStack = new ImageStack(imp.getWidth(), imp.getHeight());
		int s = imp.getNSlices();
		for(int z=0;z<s;z++) {
			ImageProcessor ip = imp.getStack().getProcessor(z+1).duplicate();
			ip.setCalibrationTable(cal.getCTable());
			FloatProcessor fp = ip.convertToFloatProcessor();//using with density calibration
			fp.setInterpolate(true);
			fp.setInterpolationMethod(RadiomicsJ.interpolation2D);
			fStack.addSlice(fp);
		}
		ImagePlus dupFloat = null;
		if(!isMask) {
			dupFloat = new ImagePlus("original_float", fStack);
		}else {
			dupFloat = new ImagePlus("mask_float", fStack);
		}
		cal.disableDensityCalibration();
		dupFloat.setCalibration(cal);
		return dupFloat;
	}
	
	public static ImagePlus createMaskCopy(ImagePlus mask) {
		ImagePlus dup = new Duplicator().run(mask);
		int s = dup.getNSlices();
		for(int z=0;z<s;z++) {
			ImageProcessor mp = mask.getStack().getProcessor(z+1);
			mp.setInterpolationMethod(RadiomicsJ.interpolation2D);
		}
		return dup;
	}
	
	public static ImagePlus createMaskCopyAsGray8(ImagePlus mask, int label) {
		int w = mask.getWidth();
		int h = mask.getHeight();
		int s = mask.getNSlices();
		ImagePlus grayMask = null;
		ImageStack stack = new ImageStack(w, h);
		for(int z=0;z<s;z++) {
			ImageProcessor ip = mask.getStack().getProcessor(z+1);
			float[][] mSlice = ip.getFloatArray();
			ByteProcessor bp = new ByteProcessor(w, h);
			for(int y=0;y<h;y++) {
				for(int x=0;x<w;x++) {
					if((int)mSlice[x][y] == label) {
						bp.set(x, y, label);
					}else {
						bp.set(x, y, 0);
					}
				}
			}
			bp.setInterpolate(true);
			bp.setInterpolationMethod(RadiomicsJ.interpolation2D);
			stack.addSlice(bp);
		}
		grayMask = new ImagePlus("mask gray8", stack);
		grayMask.setCalibration(mask.getCalibration().copy());
		grayMask.getCalibration().disableDensityCalibration();
		return grayMask;
	}

	/**
	 * Create roi from single slice mask.
	 * if roi is shaperoi, can handle with example,
	 * 
	 * System.out.println(roi.getClass().getName());
	 * if(roi instanceof ij.gui.ShapeRoi) {
	 * 		ShapeRoi sr = (ShapeRoi)roi;
	 * 		Roi[] blobs = ((ij.gui.ShapeRoi) roi).getRois();
	 * }
	 * 
	 * @param mask
	 * @return roi
	 */
	public static Roi createRoi(ImagePlus mask, int label) {
		if (mask == null || mask.getNSlices()>1) {
			return null;
		}
		ImagePlus temp = createMaskCopy(mask);
		int w = temp.getWidth();
		int h = temp.getHeight();
		for(int y=0;y<h;y++) {
			for(int x=0;x<w;x++) {
				int val = (int)temp.getProcessor().getf(x, y);
				if(val == label) {
					temp.getProcessor().set(x, y, 255);
				}else {
					temp.getProcessor().set(x, y, 0);
				}
			}
		}
		temp.getProcessor().setThreshold(0, 0, ImageProcessor.NO_LUT_UPDATE);
		temp.getProcessor().setBinaryThreshold();
		ByteProcessor mask8bit = temp.createThresholdMask();
		/*
		 * to avoid around boundary shape roi creation
		 */
		mask8bit.invert();
		ThresholdToSelection tts = new ThresholdToSelection();
		ij.gui.Roi roi = tts.convert(mask8bit);
		return roi;
	}
	
	/**
	 * create roi to find edges.
	 * @param mask
	 * @return
	 */
	public static Roi[] createRoiSet(ImagePlus mask, int label) {
		if (mask == null) {
			return null;
		}
		ImagePlus temp = createMaskCopyAsGray8(mask,label);
		int w = temp.getWidth();
		int h = temp.getHeight();
		int s = temp.getNSlices();
		Roi[] rs = new Roi[s];
		for (int z = 0; z < s; z++) {
			ImageProcessor tmp = temp.getStack().getProcessor(z+1);
			for (int y = 0; y < h; y++) {
				for (int x = 0; x < w; x++) {
					int val = (int) tmp.getf(x, y);
					if (val == label) {
						tmp.setf(x, y, 255);
					} else {
						tmp.setf(x, y, 0);
					}
				}
			}
//			tmp.threshold(label-1);//get only one line, but range are fazzy
			if((int)tmp.maxValue() >= 254) {
				tmp.setThreshold(0, 0, ImageProcessor.NO_LUT_UPDATE);
				tmp.setBinaryThreshold();
				/*
				 * to avoid around boundary shape roi creation
				 * 
				 * if not good, try this one. (this another procedure, inverting is no need.)
				 * this.imp.getProcessor().setAutoThreshold("Otsu", this.blackBackground, ImageProcessor.BLACK_AND_WHITE_LUT);
				 * ByteProcessor binary = Thresholder.createMask(this.imp);
				 * 
				 */
				tmp.invert();
				ThresholdToSelection tts = new ThresholdToSelection();
				ij.gui.Roi roi = tts.convert(tmp);
				rs[z] = roi;
			}else {
				//blank slice
				rs[z] = null;
			}
		}
		return rs;
	}
	
	public static double[] getVoxels(ImagePlus img, ImagePlus mask, int label) {
		ArrayList<Double> voxels = new ArrayList<Double>();
		int w = img.getWidth();
		int h = img.getHeight();
		int s = img.getNSlices();
		
		if(mask != null) {
			for (int z = 0; z < s; z++) {
				ImageProcessor ip = img.getStack().getProcessor(z+1);
				ImageProcessor mp = mask.getStack().getProcessor(z+1);
				for (int y = 0; y < h; y++) {
					for (int x = 0; x < w; x++) {
						int lbl_val = (int) mp.getf(x, y);
						if (lbl_val == label) {
							voxels.add((double) ip.getf(x, y));
						}
					}
				}
			}
		}else {//collect all voxels
			for (int z = 0; z < s; z++) {
				ImageProcessor ip = img.getStack().getProcessor(z+1);
				for (int y = 0; y < h; y++) {
					for (int x = 0; x < w; x++) {
						voxels.add((double) ip.getf(x,y));
					}
				}
			}
		}
		
		double[] voxelArray = new double[voxels.size()];
		for(int i = 0; i < voxels.size(); i++) voxelArray[i] = voxels.get(i);
		Arrays.sort(voxelArray);//for percentile
		return voxelArray;
	}
	
//	public static ImagePlus isoVoxelize(ImagePlus imp, boolean isMask) {
//		if(imp == null) {
//			return null;
//		}
//		imp.deleteRoi();
//		Calibration cal = imp.getCalibration();
//		if(cal == null) {
//			return imp;
//		}
//		double vx = cal.pixelWidth;
//		double vy = cal.pixelHeight;
//		double vz = cal.pixelDepth;
//		if(vx == 1.0 && vy == 1.0 && vz == 1.0) {
//			return imp;
//		}
//		double isoVoxelSize = 1.0;
//		if(!isMask) {
//			//z
//			ImagePlus rz = Reslice_Z.reslice(imp, isoVoxelSize);
//			//y
//			ImagePlus ry = Reslice_Z.reslice(stackHorizontalRotation(rz), isoVoxelSize);
//			//x
//			ImagePlus rx = Reslice_Z.reslice(stackVerticalRotation(ry), isoVoxelSize);
//			//
//			return backDirectionAfterHorizonVirticalRotation(rx);
//		}else {
//			//z
//			ImagePlus rz = Reslice_Z.resliceMask(imp, isoVoxelSize);
//			//y
//			ImagePlus ry = Reslice_Z.resliceMask(stackHorizontalRotation(rz), isoVoxelSize);
//			//x
//			ImagePlus rx = Reslice_Z.resliceMask(stackVerticalRotation(ry), isoVoxelSize);
//			//
//			return backDirectionAfterHorizonVirticalRotation(rx);
//		}
//	}
	
	public static ImagePlus isoVoxelizeNoInterpolation(ImagePlus imp) {
		if(imp == null) {
			return null;
		}
		imp.deleteRoi();
		Calibration cal = imp.getCalibration();
		if(cal == null) {
			return imp;
		}
		double vx = cal.pixelWidth;
		double vy = cal.pixelHeight;
		double vz = cal.pixelDepth;
		if(vx == 1.0 && vy == 1.0 && vz == 1.0) {
			return imp;
		}
		double isoVoxelSize = 1.0;
		//z
		ImagePlus rz = Reslice_Z.resliceMask(imp, isoVoxelSize);
		//y
		ImagePlus ry = Reslice_Z.resliceMask(stackHorizontalRotation(rz), isoVoxelSize);
		//x
		ImagePlus rx = Reslice_Z.resliceMask(stackVerticalRotation(ry), isoVoxelSize);
		//
		return backDirectionAfterHorizonVirticalRotation(rx);
	}
	
	public static ImagePlus resample3DWithoutInterpolation(ImagePlus imp, double x,double y, double z) {
		if(imp == null) {
			return null;
		}
		imp.deleteRoi();
		Calibration cal = imp.getCalibration();
		if(cal == null) {
			return imp;
		}
		double vx = cal.pixelWidth;
		double vy = cal.pixelHeight;
		double vz = cal.pixelDepth;
		if(vx == x && vy == y && vz == z) {
			return imp;
		}
		//z
		ImagePlus rz = Reslice_Z.resliceMask(imp, z);
		//y
		ImagePlus ry = Reslice_Z.resliceMask(stackHorizontalRotation(rz), y);
		//x
		ImagePlus rx = Reslice_Z.resliceMask(stackVerticalRotation(ry), x);
		//
		return backDirectionAfterHorizonVirticalRotation(rx);
	}
	
	public static ImagePlus isoVoxelizeWithInterpolation(ImagePlus imp, boolean isMask) {
		if(imp == null) {
			return null;
		}
		imp.deleteRoi();
		Calibration cal = imp.getCalibration();
		if(cal == null) {
			return imp;
		}
		double vx = cal.pixelWidth;
		double vy = cal.pixelHeight;
		double vz = cal.pixelDepth;
		if(vx == 1.0 && vy == 1.0 && vz == 1.0) {
			return imp;
		}
		double isoVoxelSize = 1.0;
		return resample3D(imp, isMask, isoVoxelSize, isoVoxelSize, isoVoxelSize);
	}
	
	public static ImagePlus resample2D(ImagePlus imp, boolean isMask, double resampleX, double resampleY , int method) {
		if(imp == null) {
			return null;
		}
		imp.deleteRoi();
		Calibration cal = imp.getCalibration().copy();
		if(cal == null) {
			return imp;
		}
		double vx = cal.pixelWidth;
		double vy = cal.pixelHeight;
		if(vx == resampleX && vy == resampleY) {
			return imp;
		}
		if(resampleX == 0 || resampleY == 0) {
			System.out.println("please set valid number of x,y. These values are not allowed zero.");
			return null;
		}
		
		double sx = cal.pixelWidth/resampleX;
		double sy = cal.pixelHeight/resampleY;
		int newW = (int) Math.ceil(imp.getWidth()*sx);
		int newH = (int) Math.ceil(imp.getHeight()*sy);
		ImageStack stack = new ImageStack(newW,newH);
		int size = imp.getNSlices();
		for(int z=0;z<size;z++) {
			ImageProcessor ip = imp.getStack().getProcessor(z+1).duplicate();
			ip.setCalibrationTable(cal.getCTable());
			ip.setInterpolationMethod(method);
			ip = ip.resize(newW, newH);
			if(!isMask) {
				stack.addSlice(ip);
			}else {
				float[][] pixels = ip.getFloatArray();
				for(int ny=0;ny<newH;ny++) {
					for(int nx=0;nx<newW;nx++) {
						if(pixels[nx][ny] > RadiomicsJ.mask_PartialVolumeThareshold) {
							pixels[nx][ny] = RadiomicsJ.label_;//always 1
						}
					}
				}
				stack.addSlice(new FloatProcessor(pixels));
			}
		}
		ImagePlus resampled2D = new ImagePlus("resampled", stack);
		cal.pixelWidth = resampleX;
		cal.pixelHeight = resampleY;
		cal.disableDensityCalibration();//because result is float processor.
		resampled2D.setCalibration(cal);
		return resampled2D;
	}
	
	/**
	 * 
	 * @param imp
	 * @param isMask
	 * @param x : new resample voxels width
	 * @param y : new resample voxels height
	 * @param z : new resample voxels depth
	 * @return
	 */
	public static ImagePlus resample3D(ImagePlus imp, boolean isMask, double x, double y, double z) {
		if(!isMask) {
			if(RadiomicsJ.interpolation3D == RadiomicsJ.TRILINEAR) {
				return trilinearInterpolation(imp, isMask, x, y, z);
			}else if(RadiomicsJ.interpolation3D == ImageProcessor.NEAREST_NEIGHBOR){
				return nearestNeighbourInterpolation(imp, isMask, x, y, z);
			}
			//add new interpolation methods
		}else {
			if(RadiomicsJ.interpolation_mask3D == RadiomicsJ.TRILINEAR) {
				return trilinearInterpolation(imp, isMask, x, y, z);
			}else if(RadiomicsJ.interpolation_mask3D == ImageProcessor.NEAREST_NEIGHBOR){
				return nearestNeighbourInterpolation(imp, isMask, x, y, z);
			}else if(RadiomicsJ.interpolation_mask3D == RadiomicsJ.NONE_3D_INTERPOLATION){
				return resample3d_withoutInterpolation(imp, x,y,z);
			}
			//add new interpolation methods
		}
		return null;
	}
	
	/**
	 * this method is alternative methods.
	 * however, trilinear is more suitable.
	 * Use trilinear interpolation instead.
	 * see, resample3D().
	 */
	public static ImagePlus resample3d_withoutInterpolation(ImagePlus imp, double x, double y, double z) {
		if(imp == null) {
			return null;
		}
		imp.deleteRoi();
		Calibration cal = imp.getCalibration().copy();
		if(cal == null) {
			return imp;
		}
		double vx = cal.pixelWidth;
		double vy = cal.pixelHeight;
		double vz = cal.pixelDepth;
		if(vx == x && vy == y && vz == z) {
			return imp;
		}
		if(x == 0 || y == 0 || z == 0) {
			System.out.println("please set valid number of x,y,z. These values are not allowed zero.");
			return null;
		}
		//z
		ImagePlus rz = Reslice_Z.resliceMask(imp, z);
		//y
		ImagePlus ry = Reslice_Z.resliceMask(stackHorizontalRotation(rz), y);
		//x
		ImagePlus rx = Reslice_Z.resliceMask(stackVerticalRotation(ry), x);
		ImagePlus resampled = backDirectionAfterHorizonVirticalRotation(rx);
		cal.pixelWidth = x;
		cal.pixelHeight = y;
		cal.pixelDepth = z;
		resampled.setCalibration(cal);
		return resampled;
	}
	
	/**
	 * use trilinear interpolation instead.
	 * @param imp
	 * @param isMask
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	@Deprecated
	public static ImagePlus resampleWithNoMaskInterpolation(ImagePlus imp, boolean isMask, double x, double y, double z) {
		if(imp == null) {
			return null;
		}
		imp.deleteRoi();
		Calibration cal = imp.getCalibration();
		if(cal == null) {
			return imp;
		}
		double vx = cal.pixelWidth;
		double vy = cal.pixelHeight;
		double vz = cal.pixelDepth;
		if(vx == x && vy == y && vz == z) {
			return imp;
		}
		if(x == 0 || y == 0 || z == 0) {
			System.out.println("please set valid number of x,y,z. These values are not allowed zero.");
			return null;
		}
		if(!isMask) {
			//z
			ImagePlus rz = Reslice_Z.resliceUsingInterpolation(imp, isMask, z);
			//y
			ImagePlus ry = Reslice_Z.resliceUsingInterpolation(stackHorizontalRotation(rz), isMask, y);
			//x
			ImagePlus rx = Reslice_Z.resliceUsingInterpolation(stackVerticalRotation(ry), isMask, x);
			//
			return backDirectionAfterHorizonVirticalRotation(rx);
		}else {
			//z
			ImagePlus rz = Reslice_Z.resliceMask(imp, z);
			//y
			ImagePlus ry = Reslice_Z.resliceMask(stackHorizontalRotation(rz), y);
			//x
			ImagePlus rx = Reslice_Z.resliceMask(stackVerticalRotation(ry), x);
			//
			return backDirectionAfterHorizonVirticalRotation(rx);
		}
	}
	
	
	/**
	 * 
	 * @param imp
	 * @param isMask : if mask, should be input label one mask.
	 * @param resampleX
	 * @param resampleY
	 * @param resampleZ
	 * @return
	 */
	public static ImagePlus trilinearInterpolation(ImagePlus imp, boolean isMask, double resampleX, double resampleY, double resampleZ) {
		if(imp == null){
			return null;
		}
		if(resampleX < 0d || resampleY < 0d || resampleZ < 0d) {
			return null;
		}
		Calibration cal = imp.getCalibration().copy();
		int w = imp.getWidth();
		int h = imp.getHeight();
		int s = imp.getStackSize();
		int imageType = imp.getType();
		if(imageType != ImagePlus.GRAY8 && imageType != ImagePlus.GRAY16 && imageType != ImagePlus.GRAY32) {
			return null;
		}

		/*
		 * At IBSI PAT1 test, rounded dim was not match with reference.
		 */
//		int newW = (int)Math.round(w * (cal.pixelWidth / resampleX));
//		int newH = (int)Math.round(h * (cal.pixelHeight / resampleY));
//		int newS = (int)Math.round(s * (cal.pixelDepth / resampleZ));
		
		int newW = (int)Math.ceil(w * (cal.pixelWidth / resampleX));
		int newH = (int)Math.ceil(h * (cal.pixelHeight / resampleY));
		int newS = (int)Math.ceil(s * (cal.pixelDepth / resampleZ));
		
		if(newS == s) {
			//force use bilinear
			return resample2D(imp, isMask, resampleX, resampleY, ImageProcessor.BILINEAR);
		}
		
		float[] voxels = new float[w*h*s];
		float[] deformed = new float[newW*newH*newS];
		int itr = 0;
		for (int z = 0; z < s; z++) {
			ImageProcessor ip = imp.getStack().getProcessor(z+1);
			for(int y = 0; y < h; y++) {
				for(int x = 0; x < w; x++) {
					voxels[itr++] = ip.getf(x, y);
				}
			}
		}
		
		int i, j, k;
		double x, y, z;
		float ratio[] = new float[3];
		ratio[0] = (float)newW / (float)w;
		ratio[1] = (float)newH / (float)h;
		ratio[2] = (float)newS / (float)s;
		
		int slice = newW * newH;
		int index;
		for (i = 0; i < newW; i++) {
			x = (i / ratio[0]);
			if(x > w-1) x = w-1;
			for (j = 0; j < newH; j++) {
				y = (j / ratio[1]);
				if(y > h-1) y = h-1;
				for (k = 0; k < newS; k++) {
					z = (k / ratio[2]);
					if(z > s-1) z = s-1;
					index = k * slice + j * newW + i;
					float interp = (float) (TrilinearInterpolation2(voxels, w, h, s, x, y, z));
					if(!isMask) {//img
						deformed[index] = interp;
					}else {//mask
//						double rangeMin = (double)(RadiomicsJ.label)-RadiomicsJ.mask_PartialVolumeThareshold;
//						double rangeMax = (double)(RadiomicsJ.label)+RadiomicsJ.mask_PartialVolumeThareshold;
						/*
						 * label-threshold <= label < label+threshold
						 */
						/*
						 * In IBSI, 
						 * interp >= RadiomicsJ.mask_PartialVolumeThareshold
						 * 
						 * but this methods always need label 1 mask.
						 */
						if(interp >= RadiomicsJ.mask_PartialVolumeThareshold) {
//						if(interp >= rangeMin && interp < rangeMax) {
							deformed[index] = (float)RadiomicsJ.label_;
						}else {
							deformed[index] = 0f;
						}
//						deformed[index] = interp;//debug
					}
				}
			}
		}
		ImageStack stack = new ImageStack(newW, newH);
		for(int z2=0;z2<newS;z2++) {
			float[] s_fp = new float[newW*newH];
			int start = newW*newH*z2;
			for(int s2=start;s2<start+slice;s2++) {
				s_fp[s2-start] = deformed[s2];
			}
			ImageProcessor ip = new FloatProcessor(newW, newH, s_fp);
			stack.addSlice(ip);
		}	
		ImagePlus interp = new ImagePlus("tri", stack);
		cal.pixelWidth = resampleX;
		cal.pixelHeight = resampleY;
		cal.pixelDepth = resampleZ;
		interp.setCalibration(cal);
		interp.getCalibration().disableDensityCalibration();
		return interp;
	}
	
//	/**
//	 * Test another procedure. but too slow.
//	 * @param imp
//	 * @param isMask
//	 * @param resampleX
//	 * @param resampleY
//	 * @param resampleZ
//	 * @return
//	 */
//	@Deprecated
//	public static ImagePlus trilinearInterpolationNeo(ImagePlus imp, boolean isMask, double resampleX, double resampleY, double resampleZ) {
//		if(imp == null){
//			return null;
//		}
//		if(resampleX < 0d || resampleY < 0d || resampleZ < 0d) {
//			return null;
//		}
//		Calibration cal = imp.getCalibration().copy();
//		int w = imp.getWidth();
//		int h = imp.getHeight();
//		int s = imp.getStackSize();
//		if(s == 1) {
//			System.out.println("This stack is only one slice, please use Bilinear instead, return null!");
//			return null;
//		}
//		int imageType = imp.getType();
//		if(imageType != ImagePlus.GRAY8 && imageType != ImagePlus.GRAY16 && imageType != ImagePlus.GRAY32) {
//			return null;
//		}
//
//		int newW = (int)Math.ceil(w * (cal.pixelWidth / resampleX));
//		int newH = (int)Math.ceil(h * (cal.pixelHeight / resampleY));
//		int newS = (int)Math.ceil(s * (cal.pixelDepth / resampleZ));
//		
//		float ratio[] = new float[3];
//		ratio[0] = (float)newW / (float)w;
//		ratio[1] = (float)newH / (float)h;
//		ratio[2] = (float)newS / (float)s;
//		
//		ImageStack triStack = new ImageStack(newW,newH);
//		for(int z = 0;z<newS;z++) {
//			ImageProcessor deformed = new FloatProcessor(newW, newH);
//			for(int y = 0;y<newH;y++) {
//				for(int x = 0;x<newW;x++) {
//					double interpX = (x/ratio[0]);
//					double interpY = (y/ratio[1]);
//					double interpZ = (z/ratio[2]);
//					if(interpX > w-1) interpX = w-1;
//					if(interpY > h-1) interpY = h-1;
//					if(interpZ > s-1) interpZ = s-1;
//					float interp = (float) (TrilinearInterpolation3(imp, w, h, s, interpX, interpY, interpZ));
//					if(!isMask) {//img
//						deformed.setf(x, y, interp);
//					}else {//mask
//						double rangeMin = (double)(RadiomicsJ.label)-RadiomicsJ.mask_PartialVolumeThareshold;
//						double rangeMax = (double)(RadiomicsJ.label)+RadiomicsJ.mask_PartialVolumeThareshold;
//						/*
//						 * label-threshold <= label < label+threshold
//						 */
//						/*
//						 * In IBSI, 
//						 * interp >= RadiomicsJ.mask_PartialVolumeThareshold
//						 * 
//						 * but this methods always need label 1 mask.
//						 */
////						if(interp >= RadiomicsJ.mask_PartialVolumeThareshold) {
//						if(interp >= rangeMin && interp < rangeMax) {
//							deformed.setf(x, y, (float)RadiomicsJ.label);
//						}else {
//							deformed.setf(x, y, 0f);
//						}
//					}
//				}
//			}
//			triStack.addSlice(deformed);
//		}
//		ImagePlus interp = new ImagePlus("tri", triStack);
//		cal.pixelWidth = resampleX;
//		cal.pixelHeight = resampleY;
//		cal.pixelDepth = resampleZ;
//		interp.setCalibration(cal);
//		return interp;
//	}
	
	public static double TrilinearInterpolation(float[] oldV, int XN, int YN,
			int ZN, double x, double y, double z) {
		int i0, j0, k0, i1, j1, k1;
		double dx, dy, dz, hx, hy, hz;
		if (x < 0 || x > (XN - 1) || y < 0 || y > (YN - 1) || z < 0
				|| z > (ZN - 1)) {
			return 0;
		} else {
			j1 = (int) Math.ceil(x);
			i1 = (int) Math.ceil(y);
			k1 = (int) Math.ceil(z);
			j0 = (int) Math.floor(x);
			i0 = (int) Math.floor(y);
			k0 = (int) Math.floor(z);
			dx = x - j0;
			dy = y - i0;
			dz = z - k0;
			
			// Introduce more variables to reduce computation
			hx = 1.0f - dx;
			hy = 1.0f - dy;
			hz = 1.0f - dz;
			// Optimized below
			int slice = XN * YN;
			k0 *= slice;
			k1 *= slice;
			i0 *= XN;
			i1 *= XN;
			return   (((oldV[k0 + i0 + j0] * hx + oldV[k0 + i0 + j1] * dx) * hy 
											+ (oldV[k0 + i1 + j0] * hx + oldV[k0 + i1 + j1] * dx) * dy) * hz 
															+ ((oldV[k1 + i0 + j0] * hx + oldV[k1 + i0 + j1] * dx) * hy 
																			+ (oldV[k1 + i1 + j0] * hx + oldV[k1 + i1 + j1] * dx) * dy)* dz);

		}
	}
	
	/**
	 * This method will output the same result to TrilinearInterpolation() described above.
	 * @param oldV : pre interpolation voxels
	 * @param XN : pre interpolation image w
	 * @param YN : pre interpolation image h
	 * @param ZN : pre interpolation image slices
	 * @param x : interpolation x with pre interpolation image scale.
	 * @param y : interpolation y with pre interpolation image scale.
	 * @param z : interpolation z with pre interpolation image scale.
	 * @return
	 */
	public static double TrilinearInterpolation2(float[] oldV, int XN, int YN, int ZN, double x, double y, double z) {
		
		int x0, y0, z0, x1, y1, z1;
		double dx, dy, dz, hx, hy, hz;
		
		if (x < 0 || x > (XN - 1) || y < 0 || y > (YN - 1) || z < 0 || z > (ZN - 1)) {
			return 0;
		} else {
			x1 = (int) Math.ceil(x);
			y1 = (int) Math.ceil(y);
			z1 = (int) Math.ceil(z);
			x0 = (int) Math.floor(x);
			y0 = (int) Math.floor(y);
			z0 = (int) Math.floor(z);
			
			//this procedure will occur out of index when force2d.
//			if(x != 0) {
//				x1 = (int) Math.ceil(x);
//				x0 = (int) Math.floor(x);
//			}else {
//				x1 = 1;
//				x0 = 0;
//			}
//			if(y != 0) {
//				y1 = (int) Math.ceil(y);
//				y0 = (int) Math.floor(y);
//			}else {
//				y1 = 1;
//				y0 = 0;
//			}
//			if(z != 0) {
//				z1 = (int) Math.ceil(z);
//				z0 = (int) Math.floor(z);
//			}else {
//				z1 = 1;
//				z0 = 0;
//			}

//			dx = (x1-x0) != 0 ? ((x - x0)/(x1-x0)) : (x - x0);
//			dy = (y1-y0) != 0 ? ((y - y0)/(y1-y0)) : (y - y0);
//			dz = (z1-z0) != 0 ? ((z - z0)/(z1-z0)) : (z - z0);
			dx = (x-x0);
			dy = (y-y0);
			dz = (z-z0);
			
//			System.out.println(dx+" "+dy+" "+dz);
			
			if (dx<0) {
				x -= 1;
				dx += 1;
			}
			if (dy<0){
				y -= 1;
				dy += 1;
			}
			if (dz<0){
				z -= 1;
				dz += 1;
			}

			// Introduce more variables to reduce computation
			hx = 1.0f - dx;
			hy = 1.0f - dy;
			hz = 1.0f - dz;
			
			// Optimized below
			int slice = XN * YN;
			z0 *= slice;
			z1 *= slice;
			y0 *= XN;
			y1 *= XN;
			
			float c000 = oldV[x0 + y0 + z0];
			float c100 = oldV[x1 + y0 + z0];
			float c001 = oldV[x0 + y0 + z1];
			float c101 = oldV[x1 + y0 + z1];
			float c010 = oldV[x0 + y1 + z0];
			float c110 = oldV[x1 + y1 + z0];
			float c011 = oldV[x0 + y1 + z1];
			float c111 = oldV[x1 + y1 + z1];
			
			double c00 = c000*hx + c100*dx;
			double c01 = c001*hx + c101*dx;
			double c10 = c010*hx + c110*dx;
			double c11 = c011*hx + c111*dx;
			
			double c0 = c00*hy + c10*dy;
			double c1 = c01*hy + c11*dy;
			
			double pix = c0*hz + c1*dz;
			
			// trilinear interpolation
//			double[] interpWeights = new double[8];
//			interpWeights[0] = hx*hy*hz;
//			interpWeights[1] = (1-hx)*(hy)*(hz);
//			interpWeights[2] = (hx)*(1-hy)*(hz);
//			interpWeights[3] = (hx)*(hy)*(1-hz);
//			interpWeights[4] = (1-hx)*(1-hy)*(hz);
//			interpWeights[5] = (hx)*(1-hy)*(1-hz);
//			interpWeights[6] = (1-hx)*(hy)*(1-hz);
//			interpWeights[7] = (1-hx)*(1-hy)*(1-hz);
//			
//			double pix = c000 * interpWeights[0];
//			pix += c100 * interpWeights[1];
//			pix += c010 * interpWeights[2];
//			pix += c001 * interpWeights[3];
//			pix += c110 * interpWeights[4];
//			pix += c011 * interpWeights[5];
//			pix += c101 * interpWeights[6];
//			pix += c111 * interpWeights[7];
			
			return pix;
		}
	}
	
	/*
	 * same result of above.
	 */
	@SuppressWarnings("unused")
	private static double TrilinearInterpolation4(float[] preVoxels, int preW, int preH, int preS, double x, double y, double z) {
		
		int tx = (int)x;
		double dx = x - tx;
		int tx1 = (tx < preW-1) ? tx+1 : tx;
		int ty = (int)y;
		double dy = y - ty;
		int ty1 = (ty < preH-1) ? ty+1 : ty;
		int tz = (int)z;
		double dz = z - tz;
		int tz1 = (tz < preS-1) ? tz+1 : tz;
		
		int slice = preW * preH;
		tz *= slice;
		tz1 *= slice;
		ty *= preW;
		ty1 *= preW;
		
		float v000 = preVoxels[tx + ty + tz];
		float v100 = preVoxels[tx1 + ty + tz];
		float v001 = preVoxels[tx + ty + tz1];
		float v101 = preVoxels[tx1 + ty + tz1];
		float v010 = preVoxels[tx + ty1 + tz];
		float v110 = preVoxels[tx1 + ty1 + tz];
		float v011 = preVoxels[tx + ty1 + tz1];
		float v111 = preVoxels[tx1 + ty1 + tz1];
		
		return (double) (
				(v100 - v000)*dx + 
				(v010 - v000)*dy + 
				(v001 - v000)*dz +
				(v110 - v100 - v010 + v000)*dx*dy +
				(v011 - v010 - v001 + v000)*dy*dz +
				(v101 - v100 - v001 + v000)*dx*dz +
				(v111 + v100 + v010 + v001 - v110 - v101 - v011 - v000)*dx*dy*dz + v000 );
	}
	
	
//	public static double TrilinearInterpolation3(ImagePlus preImp, int preW, int preH, int preS, double x, double y, double z) {
//		
//		int x0, y0, z0, x1, y1, z1;
//		double dx, dy, dz, hx, hy, hz;
//		
//		if (x < 0 || x > (preW - 1) || y < 0 || y > (preH - 1) || z < 0 || z > (preS - 1)) {
//			return 0;
//		} else {
//			
//			x1 = (int) Math.ceil(x);
//			x0 = (int) Math.floor(x);
//			y1 = (int) Math.ceil(y);
//			y0 = (int) Math.floor(y);
//			z1 = (int) Math.ceil(z);
//			z0 = (int) Math.floor(z);
//			
//			dx = (x1-x0) != 0 ? (x - x0)/(x1-x0) : (x - x0);
//			dy = (y1-y0) != 0 ? (y - y0)/(y1-y0) : (y - y0);
//			dz = (z1-z0) != 0 ? (z - z0)/(z1-z0) : (z - z0);
//			
//			// Introduce more variables to reduce computation
//			hx = 1.0f - dx;
//			hy = 1.0f - dy;
//			hz = 1.0f - dz;
//			
//			float c000 = isOutOfRange(new Point3i(x0, y0, z0), preW, preH, preS) ? 0f : preImp.getStack().getProcessor(z0+1).getPixelValue(x0,y0);
//			float c100 = isOutOfRange(new Point3i(x1, y0, z0), preW, preH, preS) ? 0f : preImp.getStack().getProcessor(z0+1).getPixelValue(x1,y0);
//			float c001 = isOutOfRange(new Point3i(x0, y0, z1), preW, preH, preS) ? 0f : preImp.getStack().getProcessor(z1+1).getPixelValue(x0,y0);
//			float c101 = isOutOfRange(new Point3i(x1, y0, z1), preW, preH, preS) ? 0f : preImp.getStack().getProcessor(z1+1).getPixelValue(x1,y0);
//			float c010 = isOutOfRange(new Point3i(x0, y1, z0), preW, preH, preS) ? 0f : preImp.getStack().getProcessor(z0+1).getPixelValue(x0,y1);
//			float c110 = isOutOfRange(new Point3i(x1, y1, z0), preW, preH, preS) ? 0f : preImp.getStack().getProcessor(z0+1).getPixelValue(x1,y1);
//			float c011 = isOutOfRange(new Point3i(x0, y1, z1), preW, preH, preS) ? 0f : preImp.getStack().getProcessor(z1+1).getPixelValue(x0,y1);
//			float c111 = isOutOfRange(new Point3i(x1, y1, z1), preW, preH, preS) ? 0f : preImp.getStack().getProcessor(z1+1).getPixelValue(x1,y1);
//			
//			double c00 = c000*hx + c100*dx;
//			double c01 = c001*hx + c101*dx;
//			double c10 = c010*hx + c110*dx;
//			double c11 = c011*hx + c111*dx;
//			
//			double c0 = c00*hy + c10*dy;
//			double c1 = c01*hy + c11*dy;
//			
//			return c0*hz + c1*dz;
//		}
//	}
	
	/**
	 * 
	 * @param imp
	 * @param isMask : label 1 mask needed.
	 * @param resampleX
	 * @param resampleY
	 * @param resampleZ
	 * @return
	 */
	public static ImagePlus nearestNeighbourInterpolation(ImagePlus imp, boolean isMask, double resampleX, double resampleY, double resampleZ) {
		if(imp == null){
			return null;
		}
		if(resampleX < 0d || resampleY < 0d || resampleZ < 0d) {
			return null;
		}
		Calibration cal = imp.getCalibration().copy();
		int w = imp.getWidth();
		int h = imp.getHeight();
		int s = imp.getStackSize();
		if(s == 1) {
			System.out.println("This stack is only one slice, please use Bilinear instead, return null!");
			return null;
		}
		int imageType = imp.getType();
		if(imageType != ImagePlus.GRAY8 && imageType != ImagePlus.GRAY16 && imageType != ImagePlus.GRAY32) {
			return null;
		}

		int newW = (int)Math.round(w * (cal.pixelWidth / resampleX));
		int newH = (int)Math.round(h * (cal.pixelHeight / resampleY));
		int newS = (int)Math.round(s * (cal.pixelDepth / resampleZ));
		
		float[] voxels = new float[w*h*s];
		float[] deformed = new float[newW*newH*newS];
		int itr = 0;
		for (int z = 0; z < s; z++) {
			ImageProcessor ip = imp.getStack().getProcessor(z+1);
			for(int y = 0; y < h; y++) {
				for(int x = 0; x < w; x++) {
					voxels[itr++] = ip.getf(x, y);
				}
			}
		}
		itr = 0;//reset
		
		int i, j, k;
		double x, y, z;
		float ratio[] = new float[3];
		ratio[0] = (float) newW / w;
		ratio[1] = (float) newH / h;
		ratio[2] = (float) newS / s;
		
		int slice = newW * newH;
		int index;
		for (i = 0; i < newW; i++) {
			x = (i / ratio[0]);
			if(x > w-1) x = w-1;
			for (j = 0; j < newH; j++) {
				y = (j / ratio[1]);
				if(y > h-1) y = h-1;
				for (k = 0; k < newS; k++) {
					z = (k / ratio[2]);
					if(z > s-1) z = s-1;
					index = k * slice + j * newW + i;
					float interp = (float) (NNInterpolation(voxels, w, h, s, x, y, z));
					if(!isMask) {//img
						deformed[index] = interp;
					}else {//mask
//						double rangeMin = (RadiomicsJ.label)-RadiomicsJ.mask_PartialVolumeThareshold;
//						double rangeMax = (RadiomicsJ.label)+RadiomicsJ.mask_PartialVolumeThareshold;
						if(interp > RadiomicsJ.mask_PartialVolumeThareshold) {
							deformed[index] = (float)RadiomicsJ.label_;
						}
					}
				}
			}
		}
		ImageStack stack = new ImageStack(newW, newH);
		for(int z2=0;z2<newS;z2++) {
			float[] s_fp = new float[newW*newH];
			int start = newW*newH*z2;
			for(int s2=start;s2<start+slice;s2++) {
				s_fp[s2-start] = deformed[s2];
			}
			ImageProcessor ip = new FloatProcessor(newW, newH, s_fp);
			stack.addSlice(ip);
		}	
		ImagePlus interp = new ImagePlus("NN", stack);
		cal.pixelWidth = resampleX;
		cal.pixelHeight = resampleY;
		cal.pixelDepth = resampleZ;
		interp.setCalibration(cal);
		interp.getCalibration().disableDensityCalibration();
		return interp;
	}
	
	
	// Nearest Neighbor interpolation
	public static double NNInterpolation(float[] oldV, int XN, int YN, int ZN, double x, double y, double z) {
		double d000 = 0.0, d001 = 0.0, d010 = 0.0, d011 = 0.0;
		double d100 = 0.0, d101 = 0.0, d110 = 0.0, d111 = 0.0;
		double value = 0.0, dist = 0.0;
		int x0 = (int) x, y0 = (int) y, z0 = (int) z;
		int x1 = x0 + 1, y1 = y0 + 1, z1 = z0 + 1;

		/*
		 * if (x == (double) (XN - 1)) x1 = XN - 1; if (y == (double) (YN - 1)) y1 = YN
		 * - 1; if (z == (double) (ZN - 1)) z1 = ZN - 1;
		 */

		if (!((x0 < 0) || (x1 > (XN - 1)) || (y0 < 0) || (y1 > (YN - 1)) || (z0 < 0) || (z1 > (ZN - 1)))) {
			d000 = DoubleDistance(z, y, x, (double) z0, (double) y0, (double) x0);
			d100 = DoubleDistance(z, y, x, (double) z0, (double) y0, (double) x1);
			d010 = DoubleDistance(z, y, x, (double) z0, (double) y1, (double) x0);
			d110 = DoubleDistance(z, y, x, (double) z0, (double) y1, (double) x1);

			d001 = DoubleDistance(z, y, x, (double) z1, (double) y0, (double) x0);
			d101 = DoubleDistance(z, y, x, (double) z1, (double) y0, (double) x1);
			d011 = DoubleDistance(z, y, x, (double) z1, (double) y1, (double) x0);
			d111 = DoubleDistance(z, y, x, (double) z1, (double) y1, (double) x1);

			dist = d000;
			int slice = XN * YN;
			// value = oldV.getDouble(x0, y0, z0);
			value = oldV[z0 * slice + y0 * XN + x0];

			if (dist > d001) {
				dist = d001;
				// value = oldV.getDouble(x0, y0, z1);
				value = oldV[z1 * slice + y0 * XN + x0];
			}

			if (dist > d010) {
				dist = d010;
				// value = oldV.getDouble(x0, y1, z0);
				value = oldV[z0 * slice + y1 * XN + x0];
			}

			if (dist > d011) {
				dist = d011;
				// value = oldV.getDouble(x0, y1, z1);
				value = oldV[z1 * slice + y1 * XN + x0];
			}

			if (dist > d100) {
				dist = d100;
				// value = oldV.getDouble(x1, y0, z0);
				value = oldV[z0 * slice + y0 * XN + x1];
			}

			if (dist > d101) {
				dist = d101;
				// value = oldV.getDouble(x1, y0, z1);
				value = oldV[z1 * slice + y0 * XN + x1];
			}

			if (dist > d110) {
				dist = d110;
				// value = oldV.getDouble(x1, y1, z0);
				value = oldV[z0 * slice + y1 * XN + x1];
			}

			if (dist > d111) {
				dist = d111;
				// value = oldV.getDouble(x1, y1, z1);
				value = oldV[z1 * slice + y1 * XN + x1];
			}

			return value;
		} else {
			return 0;
		}
	}
	
	public static double DoubleDistance(double z0, double y0, double x0,
			double z1, double y1, double x1) {
		return Math.sqrt((x0 - x1) * (x0 - x1) + (y0 - y1) * (y0 - y1) + (z0 - z1) * (z0 - z1));
	}
	
	
	/*
	 * rotate stack horizontally.
	 */
	public static ImagePlus stackHorizontalRotation(ImagePlus imp) {
		if(imp == null) {
			return null;
		}
		if(imp.getType() == ImagePlus.COLOR_RGB) {
			System.out.println("Sorry, cannot stack rotate using RGB...return null.");
			return null;
		}
		int w = imp.getWidth();
		int h = imp.getHeight();
		int z = imp.getNSlices();
		ImageStack stack = new ImageStack(w, z);
		for(int y=0 ; y<h; y++) {
			ImageProcessor ip = imp.getProcessor().createProcessor(w, z);
			for(int s =z-1 ; s>=0; s--) {
				ImageProcessor ip2 = imp.getStack().getProcessor(s+1);
				for(int x =0 ; x<w; x++) {
					float v = ip2.getPixelValue(x, y);
					ip.setf(x, (z-1)-s, v);
				}
			}
			stack.addSlice(null, ip, y);
		}
		ImagePlus res = new ImagePlus("rotated-H",stack);
		Calibration cal = imp.getCalibration().copy();
		double px = cal.pixelWidth;
		double py = cal.pixelHeight;
		double pz = cal.pixelDepth;
		cal.pixelWidth = px;
		cal.pixelHeight = pz;
		cal.pixelDepth = py;
		res.setCalibration(cal);
		return res;
	}
	
	public static ImagePlus stackVerticalRotation(ImagePlus imp) {
		if(imp == null) {
			return null;
		}
		if(imp.getType() == ImagePlus.COLOR_RGB) {
			System.out.println("Sorry, cannot stack rotate using RGB...return null.");
			return null;
		}
		int w = imp.getWidth();
		int h = imp.getHeight();
		int z = imp.getNSlices();
		ImageStack stack = new ImageStack(z, h);
		for(int x=w-1 ; x>=0; x--) {
			ImageProcessor ip = imp.getProcessor().createProcessor(z, h);
			for(int y=0 ; y<h; y++) {
				for(int s=0 ; s<z; s++) {
					//keep this state.
					float v = imp.getStack().getProcessor(s+1).getPixelValue(x, y);
					ip.setf(s, y, v);
				}
			}
			stack.addSlice(null, ip, (w-1)-x);//1-based
		}
		ImagePlus res = new ImagePlus("rotated-V",stack);
		Calibration cal = imp.getCalibration().copy();
		double px = cal.pixelWidth;
		double py = cal.pixelHeight;
		double pz = cal.pixelDepth;
		cal.pixelWidth = pz;
		cal.pixelHeight = py;
		cal.pixelDepth = px;
		res.setCalibration(cal);
		return res;
	}
	
	public static ImagePlus backDirectionAfterHorizonVirticalRotation(ImagePlus imp){
		if(imp == null) {
			return null;
		}
		if(imp.getType() == ImagePlus.COLOR_RGB) {
			System.out.println("Sorry, cannot using RGB...return null.");
			return null;
		}
		int w = imp.getWidth();//h
		int h = imp.getHeight();//z
		int z = imp.getNSlices();//w
		ImageStack stack = new ImageStack(z, w);
		for(int y=h-1 ; y>=0; y--) {
			ImageProcessor ip = imp.getProcessor().createProcessor(z, w);
			for(int x=0 ; x<w; x++) {
				for(int s=z-1 ; s>=0; s--) {
					//keep this state.
					float v = imp.getStack().getProcessor(s+1).getPixelValue(x, y);
					ip.setf((z-1)-s, x, v);
				}
			}
			stack.addSlice(null, ip, (h-1)-y);//1-based
		}
		ImagePlus res = new ImagePlus("rotated-back",stack);
		Calibration cal = imp.getCalibration().copy();
		double px = cal.pixelWidth;
		double py = cal.pixelHeight;
		double pz = cal.pixelDepth;
		cal.pixelWidth = pz;
		cal.pixelHeight = px;
		cal.pixelDepth = py;
		res.setCalibration(cal);
		return res;
	}
	
	/**
	 * nBins, for one by one intensity step histogram.
	 * discretisation interval is 1.
	 * 
	 * NOTE
	 * This method does NOT discrete to Continuous calibrated image intensities, see ISBI manual.
	 */
	public static Integer getNumOfBinsByMinMaxRange(ImagePlus img, ImagePlus mask, int label) {
		double[] voxels = Utils.getVoxels(img, mask, label);//get voxels in Roi
		double max = StatUtils.max(voxels);//max voxels in Roi
		double min = StatUtils.min(voxels);//min voxels in Roi
		return (int)((max - min)+1);
	}
	
	public static Integer getNumOfBinsByMax(ImagePlus discretisedImg, ImagePlus mask, int label) {
		double[] voxels = Utils.getVoxels(discretisedImg, mask, label);//get voxels in Roi
		return (int)StatUtils.max(voxels);//max voxels in Roi
	}
	
	
	/**
	 * create discretied image of inside roi.
	 * external roi voxel value is set to Float.NaN.
	 * 
	 * Fixed bin number (FBN) : IBSI recommended. radiomicsj default (see, RadiomicsJ.BOOL_PreferredBinCount=true).
	 * Fixed bin size (FBS) = nBis determined by binWidth(ifyou use binWidth, please set to RadiomicsJ.BOOL_PreferredBinCount=false).
	 * 
	 * @param org
	 * @param mask
	 * @param label
	 * @param nBins
	 * @return
	 * @throws Exception
	 */
	public static ImagePlus discrete(ImagePlus org, ImagePlus mask, int label, int nBins) throws Exception {
		
		if(mask == null) {
			//create full face mask
			mask = ImagePreprocessing.createMask(org.getWidth(), org.getHeight(), org.getNSlices(), null, label, org.getCalibration().pixelWidth,org.getCalibration().pixelHeight,org.getCalibration().pixelDepth);
		}
		
		int w = org.getWidth();
		int h = org.getHeight();
		int s = org.getNSlices();
		ImagePlus discreImp = org.createImagePlus();
		double[] voxels = Utils.getVoxels(org, mask, label);//get voxels in Roi
		int numOfVoxel = voxels.length;//voxels in Roi
		double max = StatUtils.max(voxels);//max voxels in Roi
		double min = StatUtils.min(voxels);//min voxels in Roi
		if(nBins < 1) {
			nBins = 1;
		}
		
		int pixelCount = 0;
		ImageStack discreteStack = new ImageStack(w, h);
		for (int z = 0; z < s; z++) {
			ImageProcessor ip = org.getStack().getProcessor(z+1);
			ImageProcessor mp = mask.getStack().getProcessor(z+1);
			ImageProcessor fp = new FloatProcessor(w, h);
			for (int y = 0; y < h; y++) {
				for (int x = 0; x < w; x++) {
					int lbl = (int) mp.getf(x, y);
					double val = ip.getf(x, y);
					if (lbl == label) {
						int discVal = Integer.MIN_VALUE;
						if(val >= max) {
							discVal = nBins;
						}else {
							discVal = (int)(nBins * ((val - min)/(max-min)))+1;
						}
						fp.setf(x, y, Integer.valueOf(discVal).floatValue());
						pixelCount++;
					}else {
						fp.setf(x, y, Float.NaN);
					}
				}
			}
			discreteStack.addSlice(fp);
		}
		/*
		 * validate whether all pixels catch-up.
		 */
		if(numOfVoxel != pixelCount) {
			throw new Exception("Histogram extraction failed...:histogram(Fixed bin number) count="+pixelCount+", pixels in image(inside roi)="+numOfVoxel);
		}
		
		//summary
		if(RadiomicsJ.debug) {
			System.out.println("------ DISCRETISATION SUMMARY ------");
			System.out.println("scale : " + (nBins / (max - min)));
			System.out.println("calibrated intensity min : " + min);
			System.out.println("calibrated intensity max : " + max);
			System.out.println("number of bins (discretise resolution) : " + nBins);
			System.out.println("------------------------------------");
		}
		
		discreImp.setStack(discreteStack);
		discreImp.setTitle("discreted by "+nBins+"(nBins)");
		return discreImp;
	}
	
	
	public static ImagePlus discreteByBinWidth(ImagePlus org, ImagePlus mask, int label, double binWidth) throws Exception {
		if(org == null) {
			return null;
		}
		
		int w = org.getWidth();
		int h = org.getHeight();
		int s = org.getNSlices();
		if(mask == null) {
			//full face mask
			mask = ImagePreprocessing.createMask(w, h, s, null, label,org.getCalibration().pixelWidth,org.getCalibration().pixelHeight,org.getCalibration().pixelDepth);
		}
		double[] voxels = Utils.getVoxels(org, mask, label);//get voxels in Roi
		double max = StatUtils.max(voxels);//max voxels in Roi
		double min = StatUtils.min(voxels);//min voxels in Roi
		/*
		 * to maintain consistency between samples, 
		 * we strongly recommend to always set the same minimum value for all samples as 
		 * defined by the lower bound of the re-segmentation range (e.g. HU of -500 for CT, SUV of 0 for PET, etc.).
		 */
		if(RadiomicsJ.rangeMin != null) {
			min = RadiomicsJ.rangeMin;
		}
		
		if(Math.abs(max-min) < binWidth) {
			System.out.println("Utils:discreteByBinWidth::binWidth values is invalid. this stack's gray intensity range is "+(max-min)+"\n"+"But, inputed binWidth is "+binWidth+". retun null.");
			return null;
		}
		
		ImagePlus discreImp = createImageCopyAsFloat(org, false);
		
		for (int z = 0; z < s; z++) {
			ImageProcessor ip = discreImp.getStack().getProcessor(z+1);
			ImageProcessor mp = mask.getStack().getProcessor(z+1);
			for (int y = 0; y < h; y++) {
				for (int x = 0; x < w; x++) {
					int lbl = (int) mp.getf(x, y);
					if (lbl == label) {
						double val = ip.getf(x, y);
						int discVal = (int)(((val - min)/binWidth)+1f);//round by casting int.
						ip.setf(x, y, Integer.valueOf(discVal).floatValue());//then back to float.
					}else {
						ip.setf(x, y, Float.NaN);
					}
				}
			}
		}
		/*
		 * see, IBSI reference.
		 */
		Integer nBins_max = (int)((max-min)/binWidth)+1;
		Integer nBins_min = (int)((StatUtils.min(voxels)-min)/binWidth)+1;
//		Integer nBins = (int) Math.round((max - min) / binWidth);
		
		//summary
		if(RadiomicsJ.debug) {
			System.out.println("------ DISCRETISATION SUMMARY(ContinuousCalibrated) ------");
			System.out.println("calibrated intensity min : " + min);
			System.out.println("calibrated intensity max : " + max);
			System.out.println("discretise resolution -max-: " + nBins_max);
			System.out.println("discretise resolution -min-: " + nBins_min);
			System.out.println("------------------------------------");
		}
		
		discreImp.setTitle("discretised_binwidth");
		return discreImp;
	}
	
	
	public static ImagePlus convertBinCenter2Intensity(ImagePlus orgImp, ImagePlus discretisedImp, ImagePlus mask, int label, double binWidth) {
		if(discretisedImp == null || orgImp == null) {
			return null;
		}
		int w = discretisedImp.getWidth();
		int h = discretisedImp.getHeight();
		int s = discretisedImp.getNSlices();
		if(mask == null) {
			//full face mask
			mask = ImagePreprocessing.createMask(w, h, s, null, label,orgImp.getCalibration().pixelWidth,orgImp.getCalibration().pixelHeight,orgImp.getCalibration().pixelDepth);
		}
		
		double min = 0d;
		if(RadiomicsJ.rangeMin != null) {
			min = RadiomicsJ.rangeMin;
		}else {
			double[] voxels = Utils.getVoxels(orgImp, mask, label);//get voxels in Roi
			min = StatUtils.min(voxels);//min voxels from original in Roi
		}
		
		ImageStack binCentered = new ImageStack(w,h);
		for (int z = 0; z < s; z++) {
			FloatProcessor fp = new FloatProcessor(w,h);
			ImageProcessor ip = discretisedImp.getStack().getProcessor(z+1);
			ImageProcessor mp = mask.getStack().getProcessor(z+1);
			for (int y = 0; y < h; y++) {
				for (int x = 0; x < w; x++) {
					int lbl = (int) mp.getf(x, y);
					if (lbl == label) {
						double dVal = ip.getf(x, y);
						float centered = (float)(min + (dVal-0.5)*binWidth);
						fp.setf(x, y, centered);
					}else {
						fp.setf(x, y, Float.NaN);
					}
				}
			}
			fp.setInterpolationMethod(RadiomicsJ.interpolation2D);
			binCentered.addSlice(fp);
		}
		ImagePlus imp4ivh = new ImagePlus("ivh-bincentered",binCentered);
		Calibration cal = discretisedImp.getCalibration().copy();
		cal.disableDensityCalibration();
		imp4ivh.setCalibration(cal);
		return imp4ivh;
	}
	
	/**
	 * Create dummy voxels for iterative voxel counting. 
	 * @param discretisedImp
	 * @param mask
	 * @param label
	 * @param nBins
	 * @return voxels[z][y][x] //[slice [row [col ]]]
	 * @throws Exception
	 */
	static Integer[][][] prepareVoxels(ImagePlus discretisedImp, ImagePlus mask, int label, int nBins) throws Exception {
		int max = 0;
		int w = discretisedImp.getWidth();
		int h = discretisedImp.getHeight();
		int s = discretisedImp.getNSlices();
		Integer voxels[][][] = new Integer[s][h][w];
		for(int z=0;z<s;z++) {
			float[][] mSlice = mask.getStack().getProcessor(z+1).getFloatArray();
			float[][] iSlice = discretisedImp.getStack().getProcessor(z+1).getFloatArray();
			for (int y = 0; y < h; y++) {
				for (int x = 0; x < w; x++) {
					int lbl = (int) mSlice[x][y];
					if(label == lbl) {
						int val = (int) iSlice[x][y];
						voxels[z][y][x] = Integer.valueOf(val);
						if (max < val) {
							max = val;
						}
					}
				}
			}
		}
		if(max > nBins) {
			System.out.println("GLRLMFeatures:preparePixels:Invalid discrete level.");
			throw new Exception();
		}
		return voxels;
	}
	
	public static boolean isOutOfRange(Point3i p, int max_w , int max_h, int max_s) {
		boolean outOfRange = false;
		if(p.x < 0 || p.x >= max_w) {
			outOfRange = true;
		}
		if(p.y < 0 || p.y >= max_h) {
			outOfRange = true;
		}
		if(p.z < 0 || p.z >= max_s) {
			outOfRange = true;
		}
		return outOfRange;
	}
	
	/**
	 * 
	 * @return 26 + own angles. symmetrical calculation needed only angle 14 to 27.
	 */
	protected static HashMap<Integer, int[]> buildAngles() {
		// angle 0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26
		// dim z -1 0 1 -1 0 1 -1 0 1 -1 0 1 -1 0 1 -1 0 1 -1 0 1 -1 0 1 -1 0 1
		// dim y -1 -1 -1 0 0 0 1 1 1 -1 -1 -1 0 0 0 1 1 1 -1 -1 -1 0 0 0 1 1 1
		// dim x -1 -1 -1 -1 -1 -1 -1 -1 -1 0 0 0 0 0 0 0 0 0 1 1 1 1 1 1 1 1 1
		HashMap<Integer, int[]> angles = new HashMap<>();
		angles.put(Integer.valueOf(0), new int[] { -1, -1, -1 });// z,y,x
		angles.put(Integer.valueOf(1), new int[] { 0, -1, -1 });
		angles.put(Integer.valueOf(2), new int[] { 1, -1, -1 });
		angles.put(Integer.valueOf(3), new int[] { -1, 0, -1 });
		angles.put(Integer.valueOf(4), new int[] { 0, 0, -1 });
		angles.put(Integer.valueOf(5), new int[] { 1, 0, -1 });
		angles.put(Integer.valueOf(6), new int[] { -1, 1, -1 });
		angles.put(Integer.valueOf(7), new int[] { 0, 1, -1 });
		angles.put(Integer.valueOf(8), new int[] { 1, 1, -1 });
		angles.put(Integer.valueOf(9), new int[] { -1, -1, 0 });
		angles.put(Integer.valueOf(10), new int[] { 0, -1, 0 });
		angles.put(Integer.valueOf(11), new int[] { 1, -1, 0 });
		angles.put(Integer.valueOf(12), new int[] { -1, 0, 0 });
		angles.put(Integer.valueOf(13), new int[] { 0, 0, 0 });// own voxel
		angles.put(Integer.valueOf(14), new int[] { 1, 0, 0 });
		angles.put(Integer.valueOf(15), new int[] { -1, 1, 0 });
		angles.put(Integer.valueOf(16), new int[] { 0, 1, 0 });
		angles.put(Integer.valueOf(17), new int[] { 1, 1, 0 });
		angles.put(Integer.valueOf(18), new int[] { -1, -1, 1 });
		angles.put(Integer.valueOf(19), new int[] { 0, -1, 1 });
		angles.put(Integer.valueOf(20), new int[] { 1, -1, 1 });
		angles.put(Integer.valueOf(21), new int[] { -1, 0, 1 });
		angles.put(Integer.valueOf(22), new int[] { 0, 0, 1 });
		angles.put(Integer.valueOf(23), new int[] { 1, 0, 1 });
		angles.put(Integer.valueOf(24), new int[] { -1, 1, 1 });
		angles.put(Integer.valueOf(25), new int[] { 0, 1, 1 });
		angles.put(Integer.valueOf(26), new int[] { 1, 1, 1 });
		return angles;
	}
	
	/**
	 * Note that the lowest bin always has value 1, and not 0. This ensures
	 * consistency for calculations of texture features, where for some features
	 * grey level 0 is not allowed .
	 * 
	 * @param discretedImp
	 * @param mask
	 * @param nBins
	 * @return
	 */
	public static int[] getHistogram(ImagePlus discretisedImp, ImagePlus mask, int label) {
		if(discretisedImp == null || mask == null) {
			System.out.println("getHistogram::mask required... return null");
			return null;
		}		
		int w = discretisedImp.getWidth();
		int h = discretisedImp.getHeight();
		int s = discretisedImp.getNSlices();
		double[] voxels = Utils.getVoxels(discretisedImp, mask, label);//get voxels in ROI.
		double max = StatUtils.max(voxels);
		double min = StatUtils.min(voxels);
		// Generate histogram
		int nBins = (int)max;
		/*
		 * histgram must start from 0
		 */
		int histogram[] = new int[nBins];
        double scale = nBins/(max-min);
        int index;
		for (int slice = 1; slice <= s; slice++) {
			for (int y = 0; y < h; y++) {
				for (int x = 0; x < w; x++) {
					int lbl = (int) mask.getStack().getProcessor(slice).getPixelValue(x, y);
					if(lbl == label) {
						double v = discretisedImp.getStack().getProcessor(slice).getPixelValue(x, y);
						if (v >= min && v <= max) {
							index = (int) (scale * (v - min));
							if (index >= nBins) {
								index = nBins-1;
							}
							histogram[index]++;
						}
					}
				}
			}
		}
		return histogram;
	}
	
	public static int[] getHistogram(double[] discretisedVoxels) {
		if(discretisedVoxels == null || discretisedVoxels.length == 0) {
			System.out.println("getHistogram::discretised voxels required... return null");
			return null;
		}
		double[] voxels = discretisedVoxels;
		int size = voxels.length;
		double max = StatUtils.max(voxels);
		// Generate histogram
		int nBins = (int)max;
		/*
		 * histgram must start from 0
		 */
		int histogram[] = new int[nBins];
		//init
		for(int i=0;i<histogram.length;i++) histogram[i] = 0;
        for(int i=0;i<size;i++) histogram[(int)voxels[i]-1]++;
		return histogram;
	}
	
	/**
	 * Axis aligned bounding box info
	 * @param mask : mask stack, should be have same size(w,h,s) to original images
	 * @param label : mask label, be integer. null-able.
	 * @return aabb info.
	 */
	public static HashMap<String, double[]> getRoiBoundingBoxInfo(ImagePlus mask, int label, boolean verbose){
		double[] xMinMax = new double[] {Double.MAX_VALUE,0};
		double[] yMinMax = new double[] {Double.MAX_VALUE,0};
		double[] zMinMax = new double[] {Double.MAX_VALUE,0};
		int w = mask.getWidth();
		int h = mask.getHeight();
		int s = mask.getNSlices();
		for(int z=0;z<s;z++) {
			float[][] mSlice = mask.getStack().getProcessor(z+1).getFloatArray();
			for(int y=0;y<h;y++) {
				for(int x=0;x<w;x++) {
					int v = (int)mSlice[x][y];
					if(v == label) {
						if(xMinMax[0] > x) {
							xMinMax[0] = (double)x;
						}
						if(xMinMax[1] < x) {
							xMinMax[1] = (double)x;
						}
						if(yMinMax[0] > y) {
							yMinMax[0] = (double)y;
						}
						if(yMinMax[1] < y) {
							yMinMax[1] = (double)y;
						}
						if(zMinMax[0] > z) {
							zMinMax[0] = (double)z;
						}
						if(zMinMax[1] < z) {
							zMinMax[1] = (double)z;
						}
					}
				}
			}
		}
		HashMap<String, double[]> xyz = new HashMap<String, double[]>();
		xyz.put("x", xMinMax);
		xyz.put("y", yMinMax);
		xyz.put("z", zMinMax);
		if(verbose) {
			System.out.println("========== roi range ==========");
			System.out.println("x min "+xMinMax[0]+" x max "+xMinMax[1]);
			System.out.println("y min "+yMinMax[0]+" y max "+yMinMax[1]);
			System.out.println("z min "+zMinMax[0]+" z max "+zMinMax[1]);
			System.out.println("===============================");
		}
		return xyz;
	}
	
	/**
	 * 
	 * @param imp
	 * @param binWidthInUnitValue : in unit value. if not calibrated, use in gray value.
	 * @throws Exception 
	 */
	public static void showHistogramByBinWidth(ImagePlus singleimp, Roi roi, double binWidthInUnitValue) throws Exception {
		double binWidth = binWidthInUnitValue;
		singleimp.deleteRoi();
		if(roi !=null) {
			singleimp.setRoi(roi);
		}
//		double min = imp.getProcessor().getMin();//DO NOT USE, this is display min.
//		double max = imp.getProcessor().getMax();//DO NOT USE, this is display max.
		ImageStatistics stats = singleimp.getStatistics();  //inner roi area stats.
//		double gray_min = stats.histMin;//no calibrated
//		double gray_max = stats.histMax;//no calibrated
		double calibrated_min = stats.min;
		double calibrated_max = stats.max;
		if(Math.abs(calibrated_max - calibrated_min) < binWidth) {
			throw new Exception("Invalid binWidth !! binWidth must to smaller than (max -min)");
		}
		
		/*
		 * Math.abs is fail safe to avoid 
		 * System.out.println(Math.ceil(-0.23));  // -0.0
		 */
		int nBins = (int) Math.ceil(Math.abs(calibrated_max - calibrated_min)/binWidthInUnitValue);
		int[] hist = new int[nBins];
		
		for(int i=0;i<nBins;i++) {
			double range_lower = calibrated_min + (i * binWidth);
			double range_upper = range_lower + binWidth;
			for(int r=0;r<singleimp.getHeight();r++) {
				for(int c=0;c<singleimp.getWidth();c++) {
					if(roi!= null) {
						if(roi.contains(r, c)) {
							double val = singleimp.getProcessor().getPixelValue(c, r);
							if(val >= range_lower && val < range_upper) {
								hist[i]++;
							}
						}
					}else {
						double val = singleimp.getProcessor().getPixelValue(c, r);
						if(val >= range_lower && val < range_upper) {
							hist[i]++;
						}
					}
				}
			}
		}
		/*
		 * validate whether all pixels catch-up.
		 */
		int pixelCount = 0;
		for(int r=0;r<singleimp.getHeight();r++) {
			for(int c=0;c<singleimp.getWidth();c++) {
				if(roi != null) {
					if(roi.contains(r, c)) {
						pixelCount ++;
					}
				}else {
					pixelCount ++;
				}
			}
		}
		int sum = 0; //
		for (int n : hist) {
			sum += n;
		}
		
		if(sum != pixelCount) {
			throw new Exception("Histogram extraction failed...:histgram count="+sum+", pixels in image/roi="+pixelCount);
		}
		
		Plot plot = new Plot("Histogram_BinWidth", "Value", "Frequency");
		plot.setColor("black", "#999999");
		plot.setFont(new Font("SansSerif",Font.PLAIN,14));
		double y[] = Arrays.stream(hist).asDoubleStream().toArray();
		int n = y.length;
		double[] x = new double[n];
		double min = stats.min;
		for (int i=0; i<n; i++)
			x[i] = min+i*binWidth;
		plot.add("bar", x, y);
		plot.addLegend(nBins+" bins", "auto");
		plot.show();
	}
	
	public static void showHistogramByBinCount(ImagePlus imp, Roi roi, int nBins) throws Exception {
		if(imp == null) {
			return;
		}
		imp.deleteRoi();
		if(roi !=null) {
			imp.setRoi(roi);
		}
		if(!imp.getCalibration().calibrated()) {
			if (!isValidNumOfBinsForGrayScale(imp.getType(), nBins)) {
				System.out.println("nBins something strange ??, can not show histgram.");
				return;
			}
			imp.plotHistogram(nBins);//using Roi imagestatistics.
			return;
		}
		
		/*
		 * if calibrated
		 */
		ImageStatistics stats = imp.getStatistics();//using Roi imagestatistics.
		double calibrated_min = stats.min;
		double calibrated_max = stats.max;
		int[] hist = new int[nBins];
		double binWidth = (calibrated_max - calibrated_min)/nBins;
		if(Math.abs(calibrated_max - calibrated_min) < binWidth) {
			throw new Exception("Invalid binWidth !! binWidth must smaller than (max -min)");
		}
		//validation pixels catch-up
		int histCount = 0;
		int pixelCount = 0;
		for(int i=0;i<nBins;i++) {
			double range_lower = calibrated_min + (i * binWidth);
			double range_upper = range_lower + binWidth;
			if(i == nBins-1) {//important
				range_upper += 1;
			}
			for(int r=0;r<imp.getHeight();r++) {
				for(int c=0;c<imp.getWidth();c++) {
					double val = imp.getProcessor().getPixelValue(c, r);
					if(val >= range_lower && val < range_upper) {
						hist[i]++;
						histCount++;
					}
					if(i == 0) {//only one iteration in nBins loop.
						pixelCount++;
					}
				}
			}
		}
		if(histCount != pixelCount) {
			throw new Exception("Histogram extraction failed...:histgram count="+histCount+", pixels in image/roi="+pixelCount);
		}
		Plot plot = new Plot("Histogram_BinCount", "Value", "Frequency");
		plot.setColor("black", "#999999");
		plot.setFont(new Font("SansSerif",Font.PLAIN,14));
		double y[] = Arrays.stream(hist).asDoubleStream().toArray();
		int n = y.length;
		double[] x = new double[n];
		double min = stats.min;
		for (int i=0; i<n; i++)
			x[i] = min+i*binWidth;
		plot.add("bar", x, y);
		plot.addLegend(nBins+" bins", "auto");
		plot.show();
	}
	
	public static ResultsTable combineTables(ResultsTable to, ResultsTable from) {
		if(to == null || to.getCounter() == 0) {
			return from;
		}else {
			if(from != null) {
				to.incrementCounter();
				String[] headings = from.getHeadings();
				int row  = 0;
				for(String h : headings) {
					if(h.contains("ID") || h.contains("OperationalInfo_")) {
						String v = from.getStringValue(h, row);
						to.addValue(h, v);
					}else {
						double v = from.getValue(h, row);
						to.addValue(h, v);
					}
				}
			}
		}
		return to;
	}
	
	/** Breaks the specified string into an array
	 of ints. Returns null if there is an error.*/
	public static int[] s2ints(String s) {
		StringTokenizer st = new StringTokenizer(s, ", \t");
		int nInts = st.countTokens();
		int[] ints = new int[nInts];
		for(int i=0; i<nInts; i++) {
			try {ints[i] = Integer.parseInt(st.nextToken());}
			catch (NumberFormatException e) {return null;}
		}
		return ints;
	}
	
	public static double[] s2doubles(String s) {
		StringTokenizer st = new StringTokenizer(s, ", \t");
		int nInts = st.countTokens();
		double[] vals = new double[nInts];
		for(int i=0; i<nInts; i++) {
			try {vals[i] = Double.parseDouble(st.nextToken());}
			catch (NumberFormatException e) {return null;}
		}
		return vals;
	}
}
