package com.vis.radiomics.main;

import java.awt.color.ColorSpace;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.process.ByteProcessor;
import ij.process.ShortProcessor;

public class TestDataLoader {
	
	static String parent_dir = "data_sets-master/";
	
	public static ImagePlus[] digital_phantom1() {
		String p2i = parent_dir+"ibsi_1_digital_phantom/nifti/image/phantom.nii.gz";
		String p2m = parent_dir+"ibsi_1_digital_phantom/nifti/mask/mask.nii.gz";
		try{
	        URL url_i = TestDataLoader.class.getClassLoader().getResource(p2i);
	        Object img = IJ.runPlugIn("Nifti_Reader", new File(url_i.toURI()).getAbsolutePath());
	        URL url_m = TestDataLoader.class.getClassLoader().getResource(p2m);
	        Object mask = IJ.runPlugIn("Nifti_Reader", new File(url_m.toURI()).getAbsolutePath());
	        return new ImagePlus[] {(ImagePlus)img, (ImagePlus)mask};
	    }catch(URISyntaxException ioe){
	        ioe.printStackTrace();
	    }   
	    return null;
	}
	
	public static ImagePlus[] digital_phantom1_scratch() {
		// color model
		ComponentColorModel cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY),
				false /* hasAlpha */, false /* isAlphaPremultiplied */, ColorModel.OPAQUE /* transparency */,
				DataBuffer.TYPE_BYTE);
		/*
		 * image
		 */
		// slice 0
		short[] r0_0 = new short[] { 1, 4, 4, 1, 1 };
		short[] r0_1 = new short[] { 1, 4, 6, 1, 1 };
		short[] r0_2 = new short[] { 4, 1, 6, 4, 1 };
		short[] r0_3 = new short[] { 4, 4, 6, 4, 1 };
		// slice 1
		short[] r1_0 = new short[] { 1, 4, 4, 1, 1 };
		short[] r1_1 = new short[] { 1, 1, 6, 1, 1 };
		short[] r1_2 = new short[] { 1, 1, 3, 1, 1 };
		short[] r1_3 = new short[] { 4, 4, 6, 1, 1 };
		// slice 2
		short[] r2_0 = new short[] { 1, 4, 4, 1, 1 };
		short[] r2_1 = new short[] { 1, 1, 1, 1, 1 };
		short[] r2_2 = new short[] { 1, 1, 9, 1, 1 };
		short[] r2_3 = new short[] { 1, 1, 6, 1, 1 };
		// slice 3
		short[] r3_0 = new short[] { 1, 4, 4, 1, 1 };
		short[] r3_1 = new short[] { 1, 1, 1, 1, 1 };
		short[] r3_2 = new short[] { 1, 1, 1, 1, 1 };
		short[] r3_3 = new short[] { 1, 1, 6, 1, 1 };

		short[][] s0 = new short[][] { r0_0, r0_1, r0_2, r0_3 };
		short[][] s1 = new short[][] { r1_0, r1_1, r1_2, r1_3 };
		short[][] s2 = new short[][] { r2_0, r2_1, r2_2, r2_3 };
		short[][] s3 = new short[][] { r3_0, r3_1, r3_2, r3_3 };
		
		ImageStack stack = new ImageStack(5, 4);
		for (short[][] s : new short[][][] { s0, s1, s2, s3 }) {
			short[] pix = new short[5 * 4];
			int pos = 0;
			for (short[] r : s) {
				for (short p : r) {
					pix[pos++] = p;
				}
			}
			ShortProcessor sp = new ShortProcessor(5, 4, pix, cm);
			stack.addSlice(sp);
		}
		ImagePlus im = new ImagePlus("ibsi digital phantom", stack);
		Calibration cal = im.getCalibration();
		cal.pixelWidth = 2.0d;
		cal.pixelHeight = 2.0d;
		cal.pixelDepth = 2.0d;
		cal.setUnit("mm");
		im.setCalibration(cal);

		/*
		 * mask
		 */
		// slice 0
		short[] r0_0_m = new short[] { 1, 1, 1, 1, 1 };
		short[] r0_1_m = new short[] { 1, 1, 1, 1, 1 };
		short[] r0_2_m = new short[] { 1, 1, 1, 1, 1 };
		short[] r0_3_m = new short[] { 1, 1, 1, 1, 1 };
		// slice 1
		short[] r1_0_m = new short[] { 1, 1, 1, 1, 1 };
		short[] r1_1_m = new short[] { 1, 1, 1, 1, 1 };
		short[] r1_2_m = new short[] { 0, 1, 1, 1, 1 };
		short[] r1_3_m = new short[] { 1, 1, 1, 1, 1 };
		// slice 2
		short[] r2_0_m = new short[] { 1, 1, 1, 0, 0 };
		short[] r2_1_m = new short[] { 1, 1, 1, 1, 1 };
		short[] r2_2_m = new short[] { 1, 1, 0, 1, 1 };
		short[] r2_3_m = new short[] { 1, 1, 1, 1, 1 };
		// slice 3
		short[] r3_0_m = new short[] { 1, 1, 1, 0, 0 };
		short[] r3_1_m = new short[] { 1, 1, 1, 1, 1 };
		short[] r3_2_m = new short[] { 1, 1, 1, 1, 1 };
		short[] r3_3_m = new short[] { 1, 1, 1, 1, 1 };

		short[][] s0_m = new short[][] { r0_0_m, r0_1_m, r0_2_m, r0_3_m };
		short[][] s1_m = new short[][] { r1_0_m, r1_1_m, r1_2_m, r1_3_m };
		short[][] s2_m = new short[][] { r2_0_m, r2_1_m, r2_2_m, r2_3_m };
		short[][] s3_m = new short[][] { r3_0_m, r3_1_m, r3_2_m, r3_3_m };
		
		ImageStack stack_m = new ImageStack(5, 4);
		for (short[][] s : new short[][][] { s0_m, s1_m, s2_m, s3_m }) {
			short[] pix = new short[5 * 4];
			int pos = 0;
			for (short[] r : s) {
				for (short p : r) {
					pix[pos++] = p;
				}
			}
			ShortProcessor sp = new ShortProcessor(5, 4, pix, cm);
			stack_m.addSlice(sp);
		}
		ImagePlus msk = new ImagePlus("ibsi digital phantom mask", stack_m);
		Calibration cal_m = msk.getCalibration();
		cal_m.pixelWidth = 2.0d;
		cal_m.pixelHeight = 2.0d;
		cal_m.pixelDepth = 2.0d;
		cal_m.setUnit("mm");
		msk.setCalibration(cal_m);
		return new ImagePlus[] { im, msk };
	}
	
	
	public static ImagePlus[] sample_ct1() {
		String p2i = parent_dir+"ibsi_1_ct_radiomics_phantom/nifti/image/phantom.nii.gz";
		String p2m = parent_dir+"ibsi_1_ct_radiomics_phantom/nifti/mask/mask.nii.gz";
		try{
	        URL url_i = TestDataLoader.class.getClassLoader().getResource(p2i);
	        Object img = IJ.runPlugIn("Nifti_Reader", new File(url_i.toURI()).getAbsolutePath());
	        URL url_m = TestDataLoader.class.getClassLoader().getResource(p2m);
	        Object mask = IJ.runPlugIn("Nifti_Reader", new File(url_m.toURI()).getAbsolutePath());
	        return new ImagePlus[] {(ImagePlus)img, (ImagePlus)mask};
	    }catch(URISyntaxException ioe){
	        ioe.printStackTrace();
	    }   
	    return null;
	}
	
	/**
	 *  name_id is the number between 001 to 051.
	 *  modality : CT, MR_T1, PET
	 * @return
	 */
	public static ImagePlus[] validationDataAt(String name_id, String modality) {
		String p2i = parent_dir+"ibsi_1_validation/nifti/STS_"+name_id+"/"+modality+"_image.nii.gz";
		String p2m = parent_dir+"ibsi_1_validation/nifti/STS_"+name_id+"/"+modality+"_mask.nii.gz";
		try{
	        URL url_i = TestDataLoader.class.getResource(p2i);
	        Object img = IJ.runPlugIn("Nifti_Reader", new File(url_i.toURI()).getAbsolutePath());
	        URL url_m = TestDataLoader.class.getResource(p2m);
	        Object mask = IJ.runPlugIn("Nifti_Reader", new File(url_m.toURI()).getAbsolutePath());
	        return new ImagePlus[] {(ImagePlus)img, (ImagePlus)mask};
	    }catch(URISyntaxException ioe){
	        ioe.printStackTrace();
	    }   
	    return null;
	}
	
	public static ImagePlus loadNifTi(String path) {
		Object img = IJ.runPlugIn("Nifti_Reader", new File(path).getAbsolutePath());
        return (ImagePlus)img;
	}
	
	/**
	 * 
	 * @param type : 0 pyradiomics, 1 ibsi, 2 gldzm
	 * @return
	 */
	public static ImagePlus get2DTestImage(int type){
		if(type ==0) {
			Integer pixels[][] = new Integer[5][];
			Integer r0[] = new Integer [] { 5, 2, 5, 4, 4 };
			Integer r1[] = new Integer [] { 3, 3, 3, 1, 3 };
			Integer r2[] = new Integer [] { 2, 1, 1, 1, 3 };
			Integer r3[] = new Integer [] { 4, 2, 2, 2, 3 };
			Integer r4[] = new Integer [] { 3, 5, 3, 3, 2 };
			pixels[0] = r0;
			pixels[1] = r1;
			pixels[2] = r2;
			pixels[3] = r3;
			pixels[4] = r4;
			byte[] pixelsByte = new byte[5 * 5];
			int num = 0;
			for(int i=0;i<5;i++) {
				for(int j=0;j<5;j++) {
					pixelsByte[num++] = Byte.valueOf(String.valueOf(pixels[i][j]));
				}
			}
			return new ImagePlus("test-2d-pyradiomics", new ByteProcessor(pixels[0].length, pixels.length, pixelsByte));
		}else if(type == 1) {
			Integer pixels[][] = new Integer[4][];
			Integer r0[] = new Integer [] { 1, 2, 2, 3 };
			Integer r1[] = new Integer [] { 1, 2, 3, 3 };
			Integer r2[] = new Integer [] { 4, 2, 4, 1 };
			Integer r3[] = new Integer [] { 4, 1, 2, 3 };
			pixels[0] = r0;
			pixels[1] = r1;
			pixels[2] = r2;
			pixels[3] = r3;
			byte[] pixelsByte = new byte[4 * 4];
			int num = 0;
			for(int i=0;i<4;i++) {
				for(int j=0;j<4;j++) {
					pixelsByte[num++] = Byte.valueOf(String.valueOf(pixels[i][j]));
				}
			}
			return new ImagePlus("test-2d-ibsi", new ByteProcessor(pixels[0].length, pixels.length, pixelsByte));
		}else if(type == 2) {
			Integer test[][] = new Integer[8][];
			Integer[] r0_2 = new Integer[] { 0, 0, 0, 0, 0, 0, 0, 0 };
			Integer[] r1_2 = new Integer[] { 0, 0, 1, 1, 0, 0, 0, 0 };
			Integer[] r2_2 = new Integer[] { 0, 1, 1, 1, 1, 1, 0, 0 };
			Integer[] r3_2 = new Integer[] { 0, 0, 0, 1, 1, 1, 1, 0 };
			Integer[] r4_2 = new Integer[] { 0, 1, 0, 1, 0, 1, 1, 0 };
			Integer[] r5_2 = new Integer[] { 0, 1, 1, 1, 1, 1, 1, 0 };
			Integer[] r6_2 = new Integer[] { 0, 0, 1, 0, 1, 1, 0, 0 };
			Integer[] r7_2 = new Integer[] { 0, 0, 0, 0, 0, 0, 0, 0 };
			test[0] = r0_2;
			test[1] = r1_2;
			test[2] = r2_2;
			test[3] = r3_2;
			test[4] = r4_2;
			test[5] = r5_2;
			test[6] = r6_2;
			test[7] = r7_2;
			byte[] pixelsByte = new byte[8 * 8];
			int num = 0;
			for(int i=0;i<8;i++) {
				for(int j=0;j<8;j++) {
					pixelsByte[num++] = Byte.valueOf(String.valueOf(test[i][j]));
				}
			}
			return new ImagePlus("test-2d-gldzm", new ByteProcessor(test[0].length, test.length, pixelsByte));
		}
		return null;
	}

}
