package RadiomicsJ;

import java.util.ArrayList;

import javax.swing.JOptionPane;

import org.apache.commons.math3.stat.StatUtils;
import org.scijava.vecmath.Point3i;

import ij.ImagePlus;
import ij.measure.Calibration;

public class LocalIntensityFeatures {
	
	int label;
	ImagePlus orgImg;
	ImagePlus orgMask;
	Calibration orgCal;
	
	int w;
	int h;
	int s;
	
	double px;
	double py;
	double pz;
	
	public static void main(String args[]) {
		System.out.println(Math.cbrt(3/(4*Math.PI)));
	}

	
	public LocalIntensityFeatures(ImagePlus img, ImagePlus mask, Integer label) {
		if (img == null) {
			return;
		}
		if (img.getType() == ImagePlus.COLOR_RGB) {
			JOptionPane.showMessageDialog(null, "RadiomicsJ can read only grayscale images(8/16/32 bits)...sorry.");
			return;
		}
		this.label = label;
		
		if (mask != null) {
			if (img.getWidth() != mask.getWidth() || img.getHeight() != mask.getHeight()
					|| img.getNSlices() != mask.getNSlices()) {
				JOptionPane.showMessageDialog(null,
						"RadiomicsJ: please should be same dimension(w,h,s) images and masks.");
				return;
			}
		}else {
			// create full face mask
			mask = ImagePreprocessing.createMask(img.getWidth(), img.getHeight(), img.getNSlices(), null, this.label, img.getCalibration().pixelWidth, img.getCalibration().pixelHeight,img.getCalibration().pixelDepth);
		}
		
		orgImg = img;
		orgMask = mask;
		orgCal = img.getCalibration().copy();
		
		w = orgImg.getWidth();
		h = orgImg.getHeight();
		s = orgImg.getNSlices();
		
		px = orgCal.pixelWidth;
		py = orgCal.pixelHeight;
		pz = orgCal.pixelDepth;
	}
	
	public Double calculate(String id) {
		String name = LocalIntensityFeatureType.findType(id);
		
		if (name.equals(LocalIntensityFeatureType.LocalIntensityPeak.name())) {
			return getLocalIntensityPeak();
		} else if (name.equals(LocalIntensityFeatureType.GlobalIntensityPeak.name())) {
			return getGlobalIntensityPeak();
		}
		return null;
	}
	
	private Double getLocalIntensityPeak() {		
		double r = Math.cbrt(3/(4*Math.PI));//spherical volume radius.//0.6203504908994 cm
		r *= 10;// from "cm" to "mm"
		//search maximum
		double[] voxels = Utils.getVoxels(orgImg, orgMask, this.label);
		double max = StatUtils.max(voxels);
		double local_int_peak = 0;
		for(int z=0;z<s;z++) {
			float[][] iSlice = orgImg.getStack().getProcessor(z+1).getFloatArray();
			float[][] mSlice = orgMask.getStack().getProcessor(z+1).getFloatArray();
			for(int y=0;y<h;y++) {
				for(int x=0;x<w;x++) {
					int lbl = (int) mSlice[x][y];
					if(lbl == label) {
						double val = iSlice[x][y];
						if(val >= max) {
							//find max voxel
							Point3i peak = new Point3i(x,y,z);
							double svs[] = getSphericalVoxels(peak, r, px, py, pz);
							if(svs != null) {
								double res = StatUtils.mean(svs);
								if(local_int_peak < res) {
									local_int_peak = res;
								}
							}else {
								// if null, only exists peak point it self.
								if(local_int_peak < val) {
									local_int_peak = val;
								}
							}
						}
					}
				}
			}
		}
		return local_int_peak;
	}
	
	private Double getGlobalIntensityPeak() {
		double r = Math.cbrt(3/(4*Math.PI));//spherical volume radius.//0.6203504908994 cm
		r *= 10;// from "cm" to "mm"
		double global_int_peak = 0;
		for(int z=0;z<s;z++) {
			float[][] iSlice = orgImg.getStack().getProcessor(z+1).getFloatArray();
			float[][] mSlice = orgMask.getStack().getProcessor(z+1).getFloatArray();
			for(int y=0;y<h;y++) {
				for(int x=0;x<w;x++) {
					int lbl = (int) mSlice[x][y];
					if(lbl == label) {
						Point3i app = new Point3i(x,y,z);
						double svs[] = getSphericalVoxels(app, r, px, py, pz);
						if(svs != null) {
							double res = StatUtils.mean(svs);
							if(global_int_peak < res) {
								global_int_peak = res;
							}
						}else {
							// if null, only exists peak point it self.
							double val = iSlice[x][y];
							if(val > global_int_peak) {
								global_int_peak = val;
							}
						}
					}
				}
			}
		}
		return global_int_peak;
	}
	
	/*
	 * Unit is mm !! both radius and voxel sizes. 
	 */
	private double[] getSphericalVoxels(Point3i peak, double radius, double px, double py, double pz) {
		int searchRangeX = (int) Math.round(radius/px);
		int searchRangeY = (int) Math.round(radius/py);
		int searchRangeZ = (int) Math.round(radius/pz);
		if(searchRangeX == 0 && searchRangeY == 0 && searchRangeZ == 0) {
			return null;
		}
		ArrayList<Double> spherical_voxels = new ArrayList<>();
		for(int z=peak.z-searchRangeZ; z<=peak.z+searchRangeZ;z++) {
			if(z < 0 || z >= s) continue;
			orgImg.setPosition(z+1);
			for(int y=peak.y-searchRangeY;y<=peak.y+searchRangeY;y++) {
				if(y < 0 || y >= h) continue;
				for(int x=peak.x-searchRangeX;x<=peak.x+searchRangeX;x++) {
					if(x < 0 || x >= w) continue;
					double length = Math.sqrt(Math.pow((x-peak.x)*px, 2) + Math.pow((y-peak.y)*py, 2) + Math.pow((z-peak.z)*pz, 2));
					if(length > radius) {
						continue;
					}
					/*
					 * including itself.
					 */
//					if(x == peak.x && y == peak.y && z == peak.z) {
//						continue;
//					}
					/*
					 * Local intensity was calculated by using without mask. 
					 */
//					int lbl = (int) orgMask.getStack().getProcessor(z+1).getPixelValue(x, y);
//					if(lbl == label) {
						double val = orgImg.getProcessor().getPixelValue(x, y);
						spherical_voxels.add(val);
//					}
				}
			}
		}
		if(spherical_voxels.size() < 1) {
			return null;
		}
		Double[] res = spherical_voxels.toArray(new Double[spherical_voxels.size()]);
		double[] res2 = new double[res.length];
		int i = 0;
		for(Double v:res) {
			res2[i++] = v;
		}
		res = null;
		spherical_voxels = null;
		return res2;
	}
}
