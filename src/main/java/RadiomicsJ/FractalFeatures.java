package RadiomicsJ;

import java.util.Arrays;
import java.util.HashMap;
import javax.swing.JOptionPane;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.measure.CurveFitter;
import ij.util.Tools;

/**
Calculate the so-called "capacity" fractal dimension.  The algorithm
is called, in fractal parlance, the "box counting" method.  In the
simplest terms, the routine counts the number of boxes of a given size
needed to cover a one pixel wide, binary (black on white) border.
The procedure is repeated for boxes that are 2 to 64 pixels wide. 
The output consists of two columns labeled "size" and "count". A plot 
is generated with the log of size on the x-axis and the log of count on
the y-axis and the data is fitted with a straight line. The slope (S) 
of the line is the negative of the fractal dimension, i.e. D=-S.

A full description of the technique can be found in T. G. Smith,
Jr., G. D. Lange and W. B. Marks, Fractal Methods and Results in Cellular Morphology,
which appeared in J. Neurosci. Methods, 69:1123-126, 1996.


@author tatsunidas.

*/
public class FractalFeatures {
	
	int[] boxSizes = new int[] {2,3,4,6,8,12,16,32,64};
	
	int[] boxCounts;
	int maxBoxSize;
	Roi[] edges;
	double eps = Math.ulp(1.0);// 2.220446049250313E-16
	
	private ImagePlus mask;
	private int label;
	
	private double D;

	public FractalFeatures(ImagePlus imp, ImagePlus mask, int label, Integer slice, int[] boxSizes) {
		if(imp == null) {
			return;
		}
		this.label = label;
		if (mask != null) {
			if (imp.getWidth() != mask.getWidth() || imp.getHeight() != mask.getHeight() || imp.getNSlices() != mask.getNSlices()) {
				JOptionPane.showMessageDialog(null, "RadiomicsJ(FractalFeature): please input same dimension image and mask.");
				return;
			}
		}else {
			// create full face mask
			Calibration cal = imp.getCalibration();
			mask = ImagePreprocessing.createMask(imp.getWidth(), imp.getHeight(), imp.getNSlices(), null, this.label, cal.pixelWidth, cal.pixelHeight,cal.pixelDepth);
		}
		if(boxSizes != null) {
			if (boxSizes.length>1) {
				Arrays.sort(boxSizes);
				this.boxSizes = new int[boxSizes.length];
				this.boxSizes = boxSizes;
			}
		}
		
		boxCounts = new int[this.boxSizes.length];
		for (int i=0; i<this.boxSizes.length; i++) {
			maxBoxSize = Math.max(maxBoxSize, this.boxSizes[i]);
		}
		
		if(slice != null) {//force 2D
			if(imp.getNSlices()<slice || slice<1) {
				System.out.println("RadiomicsJ : Slice position out of range. return null. FractalFeatures.");
				return;
			}
			this.mask = mask;
		}else {			
			//isovoxelize
			this.mask = Utils.isoVoxelizeWithInterpolation(mask, true);
			this.mask = Utils.createMaskCopyAsGray8(this.mask, this.label);
		}
		edges = Utils.createRoiSet(this.mask, this.label);
	}
	
	public Double calculate(String id) {
		String name = FractalFeatureType.findType(id);
		if (name.equals(FractalFeatureType.Capacity.name())) {
			return getCapacity();
		}
		return null;
	}
	
	private Double getCapacity() {
		if(edges == null || edges.length==0) {
			this.D = 0d;
			return this.D;
		}
		/*
		 * get dimension
		 */
		//get margin by aabb
		HashMap<String, double[]> xyzAABB = Utils.getRoiBoundingBoxInfo(mask, this.label, true);

		int n = boxSizes.length;
		float[] sizes_log = new float[n];
		float[] counts_log = new float[n];
		//init box count array
		for(int i=0; i<n; i++) {
			boxCounts[i] = 0;
		}
		for (int i=0; i<n; i++) {
			int s = boxSizes[i];
			if(s < 1) {
				continue;
			}
			count(i, s, xyzAABB);
			counts_log[i] = (float)Math.log(boxCounts[i]+eps);
			sizes_log[i] = (float)Math.log(s);
		}
		CurveFitter cf = new CurveFitter(Tools.toDouble(sizes_log), Tools.toDouble(counts_log));
		cf.doFit(CurveFitter.STRAIGHT_LINE);
		double[] p = cf.getParams();
		D = -p[1];
		return D;
	}
	
	void count(int pos, int size, HashMap<String, double[]> xyzAABB) {
		
		int x_min = (int) xyzAABB.get("x")[0];
		int x_max = (int) xyzAABB.get("x")[1];
		int y_min = (int) xyzAABB.get("y")[0];
		int y_max = (int) xyzAABB.get("y")[1];
		int z_min = (int) xyzAABB.get("z")[0];
		int z_max = (int) xyzAABB.get("z")[1]+1;//adjust for only have 1 slice.
		
		for(int z=z_min; z<z_max; z+=size) {//less than.
			for(int y=y_min;y<=y_max;y+=size) {//less or equal
				for(int x=x_min;x<=x_max;x+=size) {
					//search in volume
					for(int rz=z;rz< Math.min(z+size, z_max);rz++) {
						boolean found = false;
						Roi edge = edges[rz];
						if (edge == null) {
							continue;
						}
						int num = edge.getFloatPolygon().npoints;
						if(num == 0) {
							continue;
						}
						float[] xPts = edge.getFloatPolygon().xpoints;
						float[] yPts = edge.getFloatPolygon().ypoints;
						for (int k = 0; k < num; k++) {
							int px = (int) xPts[k];
							int py = (int) yPts[k];
							if ((px < Math.min(x+size, x_max) && px >= x) && (py < Math.min(y+size, y_max) && py >= y)) {
								boxCounts[pos]++;
								found = true;
								break;
							}
						}
						if(found) {
							break;
						}
					}
				}
			}
		}
	}
}
