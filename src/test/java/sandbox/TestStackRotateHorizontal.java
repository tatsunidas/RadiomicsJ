package sandbox;

import RadiomicsJ.Utils;
import ij.ImagePlus;

public class TestStackRotateHorizontal {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
//		String mr = "https://imagej.nih.gov/ij/images/t1-head.zip";
		String mri = "https://imagej.nih.gov/ij/images/mri-stack.zip";
		ImagePlus imp = new ImagePlus(mri);
		ImagePlus ho = Utils.stackHorizontalRotation(imp);
		ImagePlus ver = Utils.stackVerticalRotation(ho);
		Utils.backDirectionAfterHorizonVirticalRotation(ver).show();
	}

}
