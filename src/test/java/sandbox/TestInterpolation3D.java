package sandbox;

import RadiomicsJ.TestDataLoader;
import RadiomicsJ.Utils;
import ij.IJ;
import ij.ImagePlus;

public class TestInterpolation3D {

	public static void main(String[] args) {
		
		// ct test
		ImagePlus ds_pair[] = TestDataLoader.sample_ct1();
		ImagePlus imp = ds_pair[0];
		ImagePlus mask = ds_pair[1];
		
		ImagePlus triinterp = Utils.trilinearInterpolation(imp, false, 2.0, 2.0, 2.0);
		ImagePlus triinterp_m = Utils.trilinearInterpolation(mask, true, 2.0, 2.0, 2.0);
		IJ.saveAsTiff(triinterp, "trilinear.tif");
		IJ.saveAsTiff(triinterp_m, "trilinear_mask.tif");
//		
		ImagePlus nninterp = Utils.nearestNeighbourInterpolation(imp, false, 2.0, 2.0, 2.0);
		ImagePlus nninterp_m = Utils.nearestNeighbourInterpolation(mask, true, 2.0, 2.0, 2.0);
		IJ.saveAsTiff(nninterp, "nearest.tif");
		IJ.saveAsTiff(nninterp_m, "nearest_mask.tif");
		
		//digital phantom test
//		ImagePlus ds_pair[] = TestDataLoader.digital_phantom1();
//		ImagePlus imp = ds_pair[0];
//		ImagePlus mask = ds_pair[1];
		
//		ImagePlus triinterp = Utils.trilinearInterpolation(imp, false, 2.0, 2.0, 2.0);
//		ImagePlus triinterp_m = Utils.trilinearInterpolation(mask, true, 2.0, 2.0, 2.0);
//		IJ.saveAsTiff(triinterp, "trilinear.tif");
//		IJ.saveAsTiff(triinterp_m, "trilinear_mask.tif");
		
//		ImagePlus nninterp = Utils.nearestNeighbourInterpolation(imp, false, 2.0, 2.0, 2.0);
//		ImagePlus nninterp_m = Utils.nearestNeighbourInterpolation(mask, true, 2.0, 2.0, 2.0);
//		IJ.saveAsTiff(nninterp, "nearest.tif");
//		IJ.saveAsTiff(nninterp_m, "nearest3d_mask.tif");
		
		/**
		 * mathmaticaly same measns ?? NN vs nearest-2d-3times ??
		 */
//		ImagePlus nn_combi_interp_m = Utils.isoVoxelizeNoInterpolation(mask);
//		IJ.saveAsTiff(nn_combi_interp_m, "nearest-2d-3times_mask.tif");
		
	}

}
