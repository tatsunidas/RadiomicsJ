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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.stat.StatUtils;
import org.jogamp.vecmath.Point3i;

import ij.ImagePlus;
import ij.measure.Calibration;
import io.github.tatsunidas.radiomics.main.ImagePreprocessing;
import io.github.tatsunidas.radiomics.main.Utils;

/**
 * 
 * @author tatsunidas
 *
 */
public class LocalIntensityFeatures extends AbstractRadiomicsFeature{
	
	final int label;
	Calibration orgCal;
	
	int w;
	int h;
	int s;
	
	double px;
	double py;
	double pz;
		
	public LocalIntensityFeatures(ImagePlus img, ImagePlus mask, Map<String, Object> settings) {
		super(img, mask, settings);
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
	
	public LocalIntensityFeatures(ImagePlus img, ImagePlus mask, int label) {
		super(img,mask,null);
		this.label = label;
		orgCal = img.getCalibration().copy();
		if (mask == null)  {
			// create full face mask
			this.mask = ImagePreprocessing.createMask(img.getWidth(), img.getHeight(), img.getNSlices(), null, this.label, 
					orgCal.pixelWidth, orgCal.pixelHeight,orgCal.pixelDepth);
		}
		
		w = img.getWidth();
		h = img.getHeight();
		s = img.getNSlices();
		
		px = orgCal.pixelWidth;
		py = orgCal.pixelHeight;
		pz = orgCal.pixelDepth;
		
		settings.put(RadiomicsFeature.IMAGE, this.img);
		settings.put(RadiomicsFeature.MASK, this.mask);
		settings.put(RadiomicsFeature.LABEL, this.label);
	}
	
	@Override
	public void buildup(Map<String,Object> settings) {
		orgCal = img.getCalibration().copy();
		if (mask == null)  {
			// create full face mask
			mask = ImagePreprocessing.createMask(img.getWidth(), img.getHeight(), img.getNSlices(), null, this.label, 
					orgCal.pixelWidth, orgCal.pixelHeight,orgCal.pixelDepth);
		}
		w = img.getWidth();
		h = img.getHeight();
		s = img.getNSlices();
		
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
		double[] voxels = Utils.getVoxels(img, mask, this.label);
		double max = StatUtils.max(voxels);
		double local_int_peak = 0;
		for(int z=0;z<s;z++) {
			float[][] iSlice = img.getStack().getProcessor(z+1).getFloatArray();
			float[][] mSlice = mask.getStack().getProcessor(z+1).getFloatArray();
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
			float[][] iSlice = img.getStack().getProcessor(z+1).getFloatArray();
			float[][] mSlice = mask.getStack().getProcessor(z+1).getFloatArray();
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
			img.setPosition(z+1);
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
						double val = img.getProcessor().getPixelValue(x, y);
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


	@Override
	public Set<String> getAvailableFeatures() {
		Set<String> names = new HashSet<String>();
		for(LocalIntensityFeatureType t : LocalIntensityFeatureType.values()) {
			names.add(t.name());
		}
		return names;
	}


	@Override
	public String getFeatureFamilyName() {
		return "LocalIntensity";
	}


	@Override
	public Map<String, Object> getSettings() {
		return settings;
	}
}
