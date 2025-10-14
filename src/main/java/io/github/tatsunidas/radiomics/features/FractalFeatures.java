/*
 * Copyright [2022] [Tatsuaki Kobayashi]

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package io.github.tatsunidas.radiomics.features;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import ij.ImagePlus;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.measure.CurveFitter;
import ij.util.Tools;
import io.github.tatsunidas.radiomics.main.ImagePreprocessing;
import io.github.tatsunidas.radiomics.main.Utils;

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
public class FractalFeatures extends AbstractRadiomicsFeature{
	
	int[] boxSizes = new int[] {2,3,4,6,8,12,16,32,64};
	
	int[] boxCounts;
	int maxBoxSize;
	Roi[] edges;
	double eps = Math.ulp(1.0);// 2.220446049250313E-16
	
	final private int label;
	
	private double D;
	
	public FractalFeatures(ImagePlus imp, ImagePlus mask, Map<String, Object> settings) {
		super(imp, mask, settings);
		Object labelValue = settings.get(RadiomicsFeature.LABEL);
		if (labelValue == null) {
			throw new IllegalArgumentException("'label' is missing in settings.");
		}
		if (!(labelValue instanceof Integer)) {
			throw new IllegalArgumentException("'label' must be an Integer.");
		}
		this.label = (Integer) labelValue;
		buildup(settings);
	}

	public FractalFeatures(ImagePlus img, ImagePlus mask, int label, int[] boxSizes) {
		super(img,mask,null);
		this.label = label;
		if(boxSizes != null) {
			this.boxSizes = boxSizes;
			if (this.boxSizes.length > 0) {
				Arrays.sort(this.boxSizes);
				// 最後の要素が最大値 (ループ不要)
				maxBoxSize = this.boxSizes[this.boxSizes.length - 1];
				// boxCountsの初期化
				boxCounts = new int[this.boxSizes.length];
			}else {
				throw new IllegalArgumentException("FractalFeature: Invalid Box Sizes! Cannot execute calculations !");
			}
		}
		//slice is 1 to N range.
		if (mask == null) {
			// create full face mask
			this.mask = ImagePreprocessing.createMask(
					img.getWidth(), 
					img.getHeight(), 
					img.getNSlices(), 
					null,
					this.label,
					img.getCalibration().pixelWidth,
					img.getCalibration().pixelHeight,
					img.getCalibration().pixelDepth
					);
		}
		
		//isovoxelize
		this.mask = Utils.isoVoxelizeWithInterpolation(this.mask, true);
		this.mask = Utils.createMaskCopyAsGray8(this.mask, this.label);
		this.edges = Utils.createRoiSet(this.mask, this.label);
		
		settings.put(RadiomicsFeature.LABEL, Integer.valueOf(label));
		settings.put(RadiomicsFeature.BOX_SIZES, boxSizes);
	}
	
	public void buildup(Map<String, Object> settings) {
		
		//after label defined.
		if (mask == null) {
			// create full face mask
			Calibration cal = img.getCalibration();
			mask = ImagePreprocessing.createMask(img.getWidth(), img.getHeight(), img.getNSlices(), null, this.label, cal.pixelWidth, cal.pixelHeight,cal.pixelDepth);
		}
		
		Object boxSizesValue = settings.get(RadiomicsFeature.BOX_SIZES);
		if (boxSizesValue != null) {
			if (!(boxSizesValue instanceof int[])) {
				throw new IllegalArgumentException("FractalFeature: 'box_sizes' must be an int[].");
			}
			this.boxSizes = (int[]) boxSizesValue;
			if (this.boxSizes.length > 0) {
				Arrays.sort(this.boxSizes);
				// 最後の要素が最大値 (ループ不要)
				maxBoxSize = this.boxSizes[this.boxSizes.length - 1];
				// boxCountsの初期化
				boxCounts = new int[this.boxSizes.length];
			}else {
				throw new IllegalArgumentException("FractalFeature: Invalid Box Sizes! Cannot execute calculations !");
			}
		}
		//isovoxelize
		this.mask = Utils.isoVoxelizeWithInterpolation(this.mask, true);
		this.mask = Utils.createMaskCopyAsGray8(this.mask, this.label);
		this.edges = Utils.createRoiSet(this.mask, this.label);
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

	@Override
	public Set<String> getAvailableFeatures() {
		Set<String> names = new HashSet<String>();
		for(FractalFeatureType t : FractalFeatureType.values()) {
			names.add(t.name());
		}
		return names;
	}

	@Override
	public String getFeatureFamilyName() {
		return "Fractal";
	}

	@Override
	public Map<String, Object> getSettings() {
		return settings;
	}
}
