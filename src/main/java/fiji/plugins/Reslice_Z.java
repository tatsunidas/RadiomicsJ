package fiji.plugins;

import RadiomicsJ.RadiomicsJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.measure.Calibration;
import ij.plugin.filter.PlugInFilter;
import ij.process.Blitter;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

/* commented by tatsunidas
 * 
 * from https://github.com/fiji/VIB/blob/master/src/main/java/Reslice_Z.java
 * 
 * 
 */
public class Reslice_Z implements PlugInFilter {

	private ImagePlus image;

	@Override
	public void run(ImageProcessor ip) {
		double pd = image.getCalibration().pixelDepth;
		GenericDialog gd = new GenericDialog("Reslice_Z");
		gd.addNumericField("New pixel depth", pd, 3);
		gd.showDialog();
		if(gd.wasCanceled())
			return;

		pd = gd.getNextNumber();
		reslice(image, pd).show();
	}

	/**
	 * 
	 * Reslice_Z, fast algorithms.
	 * 
	 * @param image
	 * @param pixelDepth
	 * @return
	 */
	public static ImagePlus reslice(ImagePlus image, double pixelDepth) {

		int w = image.getWidth();
		int h = image.getHeight();

		Calibration cal = image.getCalibration();

		ImageStack stack = image.getStack();
		int numSlices = (int)Math.round(image.getStackSize() * cal.pixelDepth /
					pixelDepth);

		// Create a new Stack
		ImageStack newStack = new ImageStack(w, h);
		for(int z = 1; z <= numSlices; z++) {
//			double currentPos = z * pixelDepth;
			double currentPos = (z-0.5) * pixelDepth;

			// getSliceBefore
			int ind_p = (int)Math.floor(currentPos / cal.pixelDepth);
			int ind_n = ind_p + 1;

			double d_p = currentPos - ind_p*cal.pixelDepth;
			double d_n = ind_n*cal.pixelDepth - currentPos;

			if(ind_n >= stack.getSize()) {
				ind_n = stack.getSize() - 1;
			}
			
			ImageProcessor before = stack.getProcessor(ind_p + 1).duplicate();
			ImageProcessor after  = stack.getProcessor(ind_n + 1).duplicate();

			before.multiply(d_n / (d_n + d_p));
			after.multiply(d_p / (d_n + d_p));

			before.copyBits(after, 0, 0, Blitter.ADD);

			newStack.addSlice("", before);
		}
		ImagePlus result = new ImagePlus("Resliced", newStack);
		cal = cal.copy();
		cal.pixelDepth = pixelDepth;
		result.setCalibration(cal);
		return result;
	}
	
	/**
	 * with image interpolation.
	 * very slow....
	 * 
	 * @param image
	 * @param pixelDepth
	 * @return
	 */
	@Deprecated
	public static ImagePlus resliceUsingInterpolation(ImagePlus image, boolean isMask, double pixelDepth) {

		int w = image.getWidth();
		int h = image.getHeight();
		int s = image.getStackSize();
		int imageType = image.getType();

		Calibration cal = image.getCalibration();
		int numSlices = (int)Math.round(s * cal.pixelDepth / pixelDepth);

		// Create a new Stack
		ImageStack newStack = new ImageStack(w, h);
		//init
		for(int z=0;z<numSlices;z++) {
			ImageProcessor ip = null;
			if(imageType==ImagePlus.GRAY8) {
				ip = new ByteProcessor(w, h);
			}else if(imageType==ImagePlus.GRAY16) {
				ip = new ShortProcessor(w, h);
			}else if(imageType==ImagePlus.GRAY32) {
				ip = new FloatProcessor(w, h);
			}else {
				return null;
			}
			ip.setInterpolate(true);
			ip.setInterpolationMethod(RadiomicsJ.interpolation2D);
			
			newStack.addSlice(ip);
		}
		for(int x=0;x<w;x++) {
			//create yz
			ImageProcessor yz = null;
			if(imageType==ImagePlus.GRAY8) {
				yz = new ByteProcessor(s, h);
			}else if(imageType==ImagePlus.GRAY16) {
				yz = new ShortProcessor(s, h);
			}else if(imageType==ImagePlus.GRAY32) {
				yz = new FloatProcessor(s, h);
			}else {
				return null;
			}
			for(int y=0;y<h;y++) {
				for(int z=0;z<s;z++) {
					int val = image.getStack().getProcessor(z+1).get(x, y);
					yz.set(z, y, val);
				}
			}
			yz.setInterpolate(true);
			yz.setInterpolationMethod(RadiomicsJ.interpolation2D);
			yz = yz.resize(numSlices, h);
			for(int z2=0;z2<numSlices;z2++) {
				for(int y2=0;y2<h;y2++) {
					newStack.getProcessor(z2+1).set(x, y2, yz.get(z2, y2));
				}
			}
			System.out.println("resampling... current col : "+(x+1)+" / "+w);
		}
		ImagePlus result = null;
		if(!isMask) {
			result = new ImagePlus("Resliced", newStack);
		}else {
			result = new ImagePlus("Resliced-Mask", newStack);
		}
		cal = cal.copy();
		cal.pixelDepth = pixelDepth;
		result.setCalibration(cal);
		return result;
	}
	
	/*
	 * no interpolation
	 */
	public static ImagePlus resliceMask(ImagePlus mask, double pixelDepth) {

		int w = mask.getWidth();
		int h = mask.getHeight();

		Calibration cal = mask.getCalibration();

		ImageStack stack = mask.getStack();
		int numSlices = (int)Math.round(mask.getStackSize() * cal.pixelDepth / pixelDepth);

		// Create a new Stack
		ImageStack newStack = new ImageStack(w, h);
		for(int z = 1; z <= numSlices; z++) {
			double currentPos = (z-0.5) * pixelDepth;
			// getSliceBefore
			int ind_p = (int)Math.floor(currentPos / cal.pixelDepth);
			ImageProcessor before = stack.getProcessor(ind_p + 1).duplicate();
			newStack.addSlice("", before);
		}
		ImagePlus result = new ImagePlus("Resliced", newStack);
		cal = cal.copy();
		cal.pixelDepth = pixelDepth;
		result.setCalibration(cal);
		return result;
	}

	@Override
	public int setup(String arg, ImagePlus img) {
		this.image = img;
		return DOES_8G | DOES_16 | DOES_32 | DOES_RGB;
	}
}