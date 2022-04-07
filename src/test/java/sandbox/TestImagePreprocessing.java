package sandbox;

import RadiomicsJ.ImagePreprocessing;
import ij.ImagePlus;
import ij.gui.OvalRoi;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.process.ImageStatistics;

public class TestImagePreprocessing {
	
	public static String mr = "https://imagej.nih.gov/ij/images/t1-head.zip";
	public static String ct = "https://imagej.nih.gov/ij/images/CT%20Scan.dcm";

	public static void main(String[] args) {
		TestImagePreprocessing testPrepro = new TestImagePreprocessing();
//		testPrepro.maskTest1();
//		testPrepro.maskTest2();
//		testPrepro.maskTest3();
//		testPrepro.maskTest4();
		testPrepro.maskTest5();
	}
	
	/*
	 * create full face mask
	 */
	public void maskTest1() {
		ImagePlus img = new ImagePlus(mr);
		Calibration cal = img.getCalibration();
		ImagePlus maskFullFace = ImagePreprocessing.createMask(img.getWidth(), img.getHeight(), img.getNSlices(), null, 255, cal.pixelWidth,cal.pixelHeight,cal.pixelDepth);
		ImageStatistics stats = maskFullFace.getStatistics();
		System.out.println("min max:"+stats.min+" "+stats.max);
		maskFullFace.show();
	}
	
	/*
	 * create mask from roiset
	 */
	public void maskTest2() {
		ImagePlus img = new ImagePlus(mr);
		int s = img.getNSlices();
		Roi roiset[] = new Roi[img.getNSlices()];
		for(int z=0;z<s;z++) {
			Roi roi = null;
			if((z+1) % 2 == 0) {
				roi = new Roi(z,z, 50,50);
			}
			roiset[z] = roi;
		}
		Calibration cal = img.getCalibration();
		ImagePlus mask = ImagePreprocessing.createMask(img.getWidth(), img.getHeight(), img.getNSlices(), roiset, 255,cal.pixelWidth,cal.pixelHeight,cal.pixelDepth);
		mask.setPosition(2);
		ImageStatistics stats = mask.getStatistics();//current ip stats.
		/*
		 * or,
		 * mask.getStack().getProcessor(z+1).getStatistics()//but, no-calibrated.
		 */
		System.out.println("min max:"+stats.min+" "+stats.max);
		mask.show();
	}
	
	/*
	 * create resegmented mask, using same label
	 */
	public void maskTest3() {
		ImagePlus img = new ImagePlus(mr);
		int s = img.getNSlices();
		Roi roisetRect[] = new Roi[img.getNSlices()];
		Roi roisetOval[] = new Roi[img.getNSlices()];
		for(int z=0;z<s;z++) {
			Roi roi = null;
			if((z+1) % 2 == 0) {
				roi = new Roi(z,z, 50,50);
			}
			roisetRect[z] = roi;
		}
		for(int z=0;z<s;z++) {
			Roi roi = null;
			if((z+1) % 2 == 0) {
				roi = new OvalRoi(z+5,z+5, 40,40);
			}
			roisetOval[z] = roi;
		}
		Calibration cal = img.getCalibration();
		ImagePlus maskRect = ImagePreprocessing.createMask(img.getWidth(), img.getHeight(), img.getNSlices(), roisetRect, 255,cal.pixelWidth,cal.pixelHeight,cal.pixelDepth);
		ImagePlus mask = ImagePreprocessing.getResegmentedMask(maskRect, roisetOval, 255);
		mask.show();
		
	}
	
	/*
	 * extract mask from multi label mask.
	 */
	public void maskTest4() {
		/*
		 * this image has only one and two pixel intensity value.
		 */
		ImagePlus multiLabel = new ImagePlus("D:\\Dropbox\\Graphy-PlugIns\\radiomics\\src\\test\\resources\\MaskLabelTest.tif");
		ImagePlus lblOneMask = ImagePreprocessing.getResegmentedMask(multiLabel, null, 1);
		System.out.println("min max : "+lblOneMask.getStatistics().min+" "+lblOneMask.getStatistics().max);
		lblOneMask.getProcessor().setBinaryThreshold();
		ImagePlus binaryOne = new ImagePlus("lbl one",lblOneMask.createThresholdMask());
		binaryOne.show();
		ImagePlus lblTwoMask = ImagePreprocessing.getResegmentedMask(multiLabel, null, 2);
		System.out.println("min max : "+lblTwoMask.getStatistics().min+" "+lblTwoMask.getStatistics().max);
		lblTwoMask.getProcessor().setBinaryThreshold();
		ImagePlus binaryTwo = new ImagePlus("lbl two",lblTwoMask.createThresholdMask());
		binaryTwo.show();
	}
	
	/*
	 * extract mask by specified label and roiset.
	 */
	public void maskTest5() {
		/*
		 * this image has only one and two pixel intensity value.
		 */
		ImagePlus multiLabel = new ImagePlus("D:\\Dropbox\\Graphy-PlugIns\\radiomics\\src\\test\\resources\\MaskLabelTest.tif");
		Roi roi = new OvalRoi(125,125,60,60);
		Roi roiset[] = new Roi[] {roi};
		ImagePlus lblTwoMask = ImagePreprocessing.getResegmentedMask(multiLabel, roiset, 2);
		System.out.println("min max : "+lblTwoMask.getStatistics().min+" "+lblTwoMask.getStatistics().max);
		lblTwoMask.getProcessor().setBinaryThreshold();
		ImagePlus binaryTwo = new ImagePlus("lbl two",lblTwoMask.createThresholdMask());
		binaryTwo.show();
	}

}
