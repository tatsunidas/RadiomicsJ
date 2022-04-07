package sandbox;

import RadiomicsJ.ImageFiltering;
import ij.ImagePlus;

public class TestWaveCoeffs {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String mri = "https://imagej.nih.gov/ij/images/mri-stack.zip";
		ImagePlus imp = new ImagePlus(mri);
		ImageFiltering.getWaveletCoeffs(imp, 256).show();
		
	}

}
