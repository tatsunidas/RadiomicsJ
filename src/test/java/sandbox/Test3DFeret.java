package sandbox;

import RadiomicsJ.RadiomicsJ;
import RadiomicsJ.TestDataLoader;
import ij.ImagePlus;

public class Test3DFeret {

	public static void main(String[] args) {
		
//		int w = 10;
//		int h = 20;
//		int l = 30;
//		ImagePlus mask = new ImagePlus();
//		Roi rect = new Roi(10, 10, w, h);
//		ImageStack stack = new ImageStack(40, 40);
//		for(int i=1;i<=40;i++) {
//			ByteProcessor bp = new ByteProcessor(40, 40);
//			if(i>=6 && i<6+l) {
//				bp.setRoi(rect);
//				bp.setColor(255);
//				bp.fill();
//			}
//			stack.addSlice(bp);
//		}
//		mask.setStack(stack);
//		mask.deleteRoi();
//		imp.show();
		
		RadiomicsJ.targetLabel = 1;
		ImagePlus ds_pair[] = TestDataLoader.digital_phantom1();
		ImagePlus imp = ds_pair[0];
		ImagePlus mask = ds_pair[1];
		mask.setCalibration(imp.getCalibration());
//		System.out.println(mask.getCalibration().pixelWidth);
//		System.out.println(mask.getCalibration().pixelHeight);
//		System.out.println(mask.getCalibration().pixelDepth);
		
//		int[] bb = new Ellipsoid_3DTool().getBoundingBox(mask);
//		double[] bb = new Ellipsoid_3DTool().getOrientedBoundingBox(mask);
//		double[]bb = new Ellipsoid_3DTool().getEllipsoidPoles(mask);
//		double x = Math.abs(bb[1]-bb[0]);
//		double y = Math.abs(bb[3]-bb[2]);
////		double z = Math.abs(bb[5]-bb[4]);
//		System.out.println(x+" "+y+" "+z);
//		System.out.println(Math.sqrt(x*x+y*y+z*z));
//		System.out.println(Math.sqrt(8*8+6*6+6*6));
		
//		IJ.saveAs(mask, "tif", "fill_hole");
		
//		double[] res = new Ellipsoid_3DTool().getMajorMinorLeastAxisLength(mask, false);
//		System.out.println(res[0]+" "+res[1]+" "+res[2]);
		
		
		
	}

}
