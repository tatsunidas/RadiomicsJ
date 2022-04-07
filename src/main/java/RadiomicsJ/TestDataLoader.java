package RadiomicsJ;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ByteProcessor;

public class TestDataLoader {
	
	static String parent_dir = "/data_sets-master/";
	
	public static ImagePlus[] digital_phantom1() {
		String p2i = parent_dir+"ibsi_1_digital_phantom/nifti/image/phantom.nii.gz";
		String p2m = parent_dir+"ibsi_1_digital_phantom/nifti/mask/mask.nii.gz";
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
	
	public static ImagePlus[] sample_ct1() {
		String p2i = parent_dir+"ibsi_1_ct_radiomics_phantom/nifti/image/phantom.nii.gz";
		String p2m = parent_dir+"ibsi_1_ct_radiomics_phantom/nifti/mask/mask.nii.gz";
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
