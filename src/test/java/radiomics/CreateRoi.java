package radiomics;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.plugin.FolderOpener;
import ij.process.ImageProcessor;
import io.github.tatsunidas.radiomics.main.Utils;

public class CreateRoi {

	public static void main(String[] args) {
//		checkSlice();
		checkStack();
	}
	
	static void checkSlice() {
		String maskPath = "/home/tatsunidas/デスクトップ/batch_test_radj/T1_LEFT_PLAQUE/MASKS/case_003/Mask_p_008.tif";
		ImagePlus mask = FolderOpener.open(maskPath);
		Roi[] rs = Utils.createRoiSet(mask, 255);

		int w = mask.getWidth();
		int h = mask.getHeight();
		int nSlices = 1;
		
		//rebuild mask from rois
		ImagePlus rebuiltMask = IJ.createImage("Rebuilt Mask", "8-bit black", w, h, nSlices);
		
		for (int p = 1; p <= nSlices; p++) {
			Roi r = rs[p - 1];
			if (r != null) {
				ImageProcessor ip = rebuiltMask.getStack().getProcessor(p);
				ip.setValue(255);
				ip.fill(r);
			}
		}
		//show rebuild images
		rebuiltMask.show();
	}
	
	static void checkStack() {
		String imagePath = "/home/tatsunidas/デスクトップ/batch_test_radj/T1_LEFT_PLAQUE/IMAGES/case_003";
		String maskPath = "/home/tatsunidas/デスクトップ/batch_test_radj/T1_LEFT_PLAQUE/MASKS/case_003";
		ImagePlus image = FolderOpener.open(imagePath);
		ImagePlus mask = FolderOpener.open(maskPath);
		mask = Utils.padMaskStack(mask, image, new int[] {7,8});
		Roi[] rs = Utils.createRoiSet(mask, 255);

		int w = image.getWidth();
		int h = image.getHeight();
		int nSlices = image.getNSlices();
		
		//rebuild mask from rois
		ImagePlus rebuiltMask = IJ.createImage("Rebuilt Mask", "8-bit black", w, h, nSlices);
		
		for (int p = 1; p <= nSlices; p++) {
			Roi r = rs[p - 1];
			if (r != null) {
				ImageProcessor ip = rebuiltMask.getStack().getProcessor(p);
				ip.setValue(255);
				ip.fill(r);
			}
		}
		//show rebuild images
		rebuiltMask.show();
	}
	
}
