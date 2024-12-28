package io.github.tatsunidas.radiomics.features;

import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JOptionPane;

import org.scijava.vecmath.Point3i;

import ij.ImagePlus;
import io.github.tatsunidas.radiomics.main.Utils;

/**
 * Gray Level Kinetics Zone Matrix.
 * 
 * Various textures can be computed using this class.
 * For example, if we specify in KineticsMap a Velocity obtained from blood flow analysis, we call it Gray Level Fluid Zone Matrix.
 * 
 * GLKZMFeatures glkzm = GLKZMFeatures(img, mask, label, useBinCount, nBins, binWidth);
 * glkzm.setKineticsMap(kinetics, useBinCount, kinetics_nBins_low_value, null);
 * glkzm.fillMatrix();
 * 
 * then, get any results,
 * Double res = glkzm.calculate(GLDZMFeatureType.SmallDistanceEmphasis.id()); 
 * 
 * @author tatsunidas
 *
 */
public class GLKZMFeatures extends GLDZMFeatures{
	
	ImagePlus kineticsMap = null;//Org
	ImagePlus kineticsDiscMap = null;//Discretized kinetic map (used as distance map)
	int kinetics_nBins;

	public GLKZMFeatures(ImagePlus img, ImagePlus mask, int label, boolean useBinCount, Integer nBins, Double binWidth)
			throws Exception {
		readyToCalculate(img, mask, label, useBinCount, nBins, binWidth);
	}
	
	public void setKineticsMap(ImagePlus kinetics, boolean useBinCount, Integer kinetics_nBins, Double binWidth)throws Exception {
		if(kinetics == null || kinetics.getNSlices()==0) {
			throw new IllegalArgumentException("Please input valid kinetics map.");
		}
		if (img.getWidth() != kinetics.getWidth() || img.getHeight() != kinetics.getHeight() || img.getNSlices() != kinetics.getNSlices()) {
			JOptionPane.showMessageDialog(null, "RadiomicsJ: please input same dimension image, mask and kinetics.");
			return;
		}
		kineticsMap = kinetics;
		if(useBinCount) {
			kineticsDiscMap = Utils.discrete(kineticsMap, this.mask, this.label, kinetics_nBins);
			this.kinetics_nBins = kinetics_nBins;
		}else {
			/*
			 * do Fixed Bin Width
			 */
			kineticsDiscMap = Utils.discreteByBinWidth(kineticsMap, this.mask, this.label, binWidth);
			this.kinetics_nBins = Utils.getNumOfBinsByMax(kineticsDiscMap, this.mask, this.label);
		}
	}
	
	public void fillMatrix() throws Exception {
		gldzm_raw = null;//init
		gldzm = null;
		//<grayVal - count zones by distance>
		HashMap<Integer, Integer[]> gldzm_map = new HashMap<Integer,Integer[]>();
		int w = discImg.getWidth();
		int h= discImg.getHeight();
		int s = discImg.getNSlices();
		Integer[][][] voxels = Utils.prepareVoxels(discImg, mask, label, nBins);//[z][y][x], temp voxels at it angle, for count up.
		Integer[][][] distance_map = getDistanceMap(kineticsDiscMap);
		//search distance max
		int distance_max = 1;
		for(int z=0;z<s;z++) {
			for (int y = 0; y<h; y++) {
				for (int x = 0; x < w; x++) {
					if(distance_map[z][y][x] == null) {
						continue;
					}
					if (distance_max < distance_map[z][y][x]) {
						distance_max = distance_map[z][y][x];
					}
				}
			}
		}
		//get blobs
		ArrayList<ArrayList<Point3i>> blobs = collectBlobs(voxels);
		
		for(ArrayList<Point3i> blob : blobs) {
			if(blob == null || blob.size() < 1) {
				continue;
			}
			Integer blob_grayLevel = null;
			//search distance min in blob area
			int d = Integer.MAX_VALUE;
			for(Point3i p : blob) {
				int dv = distance_map[p.z][p.y][p.x];
				if (d > dv) {
					d = dv;
					if(blob_grayLevel == null) {
						blob_grayLevel = Integer.valueOf(voxels[p.z][p.y][p.x]);
					}
				}
			}
			if(d < 1) {
				continue;
			}
			if(gldzm_map.get(blob_grayLevel) == null) {
				//init matrix
				//[zoneDistance1_count, zoneDistance2_count...] array for each gray level.
				Integer[] distance_zone_count = new Integer[distance_max];
//				for(Integer c : distance_zone_count) {
//					c = Integer.valueOf(0);//DO NOT USE
//				}
				int itr = 0;
				for(@SuppressWarnings("unused") Integer c : distance_zone_count) {
					distance_zone_count[itr++] = Integer.valueOf(0);
				}
				distance_zone_count[d-1] = Integer.valueOf(1);//d is 1 based.
				gldzm_map.put(blob_grayLevel, distance_zone_count);
			}else {
				gldzm_map.get(blob_grayLevel)[d-1]++;
			}
		}
		gldzm_raw = map2matrix(gldzm_map);
		gldzm = normalize(gldzm_raw);
		calculateCoefficients();
	}
	
	@Override
	public Integer[][][] getDistanceMap(ImagePlus kineticsDiscMap) {
		try {
			return Utils.prepareVoxels(kineticsDiscMap, mask, label, kinetics_nBins);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
