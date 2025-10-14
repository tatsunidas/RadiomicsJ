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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jogamp.vecmath.Point3i;

import ij.ImagePlus;
import ij.measure.Calibration;
import io.github.tatsunidas.radiomics.main.ImagePreprocessing;
import io.github.tatsunidas.radiomics.main.RadiomicsJ;
import io.github.tatsunidas.radiomics.main.Utils;

/**
 * GrayLevelSizeZoneMatrix
 * 
 * If get stackoverflow, set VM atguments,
 * -Xss=32m
 * 
 * @author tatsunidas t_kobayashi@vis-ionary.com
 *
 */
public class GLSZMFeatures extends AbstractRadiomicsFeature implements Texture{
	
	ImagePlus discImg;// discretised
	Calibration orgCal;// backup
	
	int w ;
	int h ;
	int s ;
	
	final int label;
	double[][] glszm_raw;
	double[][] glszm;// normalized
	
	HashMap<Integer, int[]> angles = Utils.buildAngles();
	ArrayList<Integer> angle_ids;//0 to 26
	
	boolean normalization = true;// always true;
	int nBins;// 1 to N
	double binWidth;
	double Ns = 0d;//sum of zone size count, without normalize
	double mu_i=0.0;
	double mu_j=0.0;
	double eps = Math.ulp(1.0);// 2.220446049250313E-16
	
	public GLSZMFeatures(ImagePlus img, ImagePlus mask, Map<String, Object> settings) {
		super(img,mask,null);
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
	
	public GLSZMFeatures(ImagePlus img, 
						 ImagePlus mask, 
						 int label, 
						 boolean useBinCount,
						 Integer nBins,
						 Double binWidth) throws Exception {
		super(img,mask, null);
		this.label = label;
		
		if (mask == null) {
			// create full face mask
			mask = ImagePreprocessing.createMask(img.getWidth(), img.getHeight(), img.getNSlices(), null,
					this.label,img.getCalibration().pixelWidth, img.getCalibration().pixelHeight,img.getCalibration().pixelDepth);
		}
		
		this.orgCal = img.getCalibration();
		this.mask = mask;
		
		if(nBins == null) {
			this.nBins = RadiomicsJ.nBins;
		}else {
			this.nBins = nBins;
		}
		
		if(binWidth == null) {
			this.binWidth = RadiomicsJ.binWidth;
		}else {
			this.binWidth = binWidth;
		}
		
		angle_ids = new ArrayList<>(angles.keySet());//0 to 26
		Collections.sort(angle_ids);
		
		// discretised by roi mask.
		if(RadiomicsJ.discretiseImp != null) {
			discImg = RadiomicsJ.discretiseImp;
		}else {
			if(useBinCount) {
				discImg = Utils.discrete(this.img, this.mask, this.label, this.nBins);
			}else {
				/*
				 * do Fixed Bin Width
				 */
				discImg = Utils.discreteByBinWidth(this.img, this.mask, this.label, this.binWidth);
				this.nBins = Utils.getNumOfBinsByMax(discImg, this.mask, this.label);
			}
		}
		
		w = discImg.getWidth();
		h= discImg.getHeight();
		s = discImg.getNSlices();
		
		try{
			fillGLSZM();
		}catch(StackOverflowError e) {
			System.out.println("Stack Overflow occured when executing fillGLSZM().");
			System.out.println("RadiomicsJ: please increase stack memmory size using VM arguments, like example,\n java -Xss=32m -jar RadiomicsJ.jar");
//			JOptionPane.showMessageDialog(null, "RadiomicsJ: please increase stack memmory size using VM arguments, like example,\n java -Xss=32m -jar RadiomicsJ.jar");
			return;
		}
		
		settings.put(RadiomicsFeature.IMAGE, img);
		settings.put(RadiomicsFeature.MASK, mask);
		settings.put(RadiomicsFeature.DISC_IMG, discImg);
		settings.put(RadiomicsFeature.LABEL, label);
		settings.put(RadiomicsFeature.USE_BIN_COUNT, useBinCount);
		settings.put(RadiomicsFeature.nBins, this.nBins);
		settings.put(RadiomicsFeature.BinWidth, binWidth);
	}
	
	public void buildup(Map<String, Object> settings) {
		
		Object useBinValue = settings.get(RadiomicsFeature.USE_BIN_COUNT);
		if (useBinValue == null) {
			throw new IllegalArgumentException("'useBinCount:boolean' is missing in settings.");
		}
		if (!(useBinValue instanceof Boolean)) {
			throw new IllegalArgumentException("'useBinCount' must be a Boolean.");
		}
		boolean useBinCount = (Boolean)useBinValue;
		
		Object nBinsValue = settings.get(RadiomicsFeature.nBins);
		if (nBinsValue == null && useBinCount == true) {
			throw new IllegalArgumentException("'nBins' is missing in settings.");
		}
		if (nBinsValue != null && !(nBinsValue instanceof Integer)) {
			throw new IllegalArgumentException("'nBins' must be an Integer.");
		}
		if(nBinsValue == null) {
			this.nBins = RadiomicsJ.nBins;
		}else {
			this.nBins = (Integer) nBinsValue;
		}
		
		Object bwValue = settings.get(RadiomicsFeature.BinWidth);
		if (bwValue == null && useBinCount == false) {
			throw new IllegalArgumentException("'BinWidth' is missing in settings.");
		}
		if (bwValue != null && !(bwValue instanceof Double)) {
			throw new IllegalArgumentException("'BinWidth' must be a Double.");
		}
		if(bwValue == null) {
			this.binWidth = RadiomicsJ.binWidth;
		}else {
			this.binWidth = (Double)bwValue;
		}
		
//		Object norm = settings.get(RadiomicsFeature.WEIGHTING_NORM);
//		if(norm != null && (norm instanceof String)) {
//			setWeightingNorm((String)norm);
//		}
		
		if (mask == null) {
			// create full face mask
			mask = ImagePreprocessing.createMask(
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
		
		this.orgCal = this.img.getCalibration();
		// discretised by roi mask.
		if (RadiomicsJ.discretiseImp != null) {
			discImg = RadiomicsJ.discretiseImp;
		} else {
			if (useBinCount) {
				try {
					discImg = Utils.discrete(this.img, this.mask, this.label, this.nBins);
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				/*
				 * Bin Width
				 */
				try {
					discImg = Utils.discreteByBinWidth(this.img, this.mask, this.label, this.binWidth);
				} catch (Exception e) {
					e.printStackTrace();
				}
				this.nBins = Utils.getNumOfBinsByMax(discImg, this.mask, this.label);
			}
		}
		w = this.img.getWidth();
		h = this.img.getHeight();
		s = this.img.getNSlices();
		angle_ids = new ArrayList<>(angles.keySet());//0 to 26
		Collections.sort(angle_ids);
		try{
			fillGLSZM();
		}catch(StackOverflowError | Exception e) {
			System.out.println(e);
//			System.out.println("Stack Overflow occured when executing fillGLSZM().");
//			System.out.println("RadiomicsJ: please increase stack memmory size using VM arguments, like example,\n java -Xss=32m -jar RadiomicsJ.jar");
//			JOptionPane.showMessageDialog(null, "RadiomicsJ: please increase stack memmory size using VM arguments, like example,\n java -Xss=32m -jar RadiomicsJ.jar");
			return;
		}
	}
	

	private void fillGLSZM() throws Exception {
		glszm_raw = null;//init
		glszm = null;
		HashMap<Integer, HashMap<Integer,Integer>> glszm_map = new HashMap<Integer, HashMap<Integer,Integer>>();
		Integer[][][] voxels = Utils.prepareVoxels(discImg, mask, label, nBins);//[z][y][x], temp voxels at it angle, for count up.
		for(int grayLevel=1;grayLevel<=nBins;grayLevel++) {
			//size and count
			HashMap<Integer,Integer> glszm_row = new HashMap<Integer,Integer>();
			for(int z=0;z<s;z++) {
				for(int y=0;y<h;y++) {
					for(int x=0;x<w;x++) {
						if (voxels[z][y][x] == null) {
							continue;
						}
						if (voxels[z][y][x] == Integer.MIN_VALUE) {
							continue;
						}
						if(voxels[z][y][x] == grayLevel) {
							Integer sizeOfZone = 0;//init
//							sizeOfZone = countNeighbor(grayLevel, voxels, sizeOfZone, x, y, z);//grayLevel, voxels, zone, x, y, z
							sizeOfZone = countNeighbor2(grayLevel, voxels, sizeOfZone, x, y, z);//grayLevel, voxels, zone, x, y, z
							glszm_row.put(sizeOfZone, glszm_row.get(sizeOfZone) == null ? 1:glszm_row.get(sizeOfZone)+1);
						}
					}
				}
			}
			///1 to Ng
			glszm_map.put(grayLevel, glszm_row);
		}
		glszm_raw = map2matrix(glszm_map);
		glszm = normalize(glszm_raw);//count Ns
		calculateCoefficients();
	}
	
	
	private double[][] normalize(double[][] glszm_raw){
		// skip all zero matrix
		if (glszm_raw == null) {
			return null;
		}
		int h = glszm_raw.length;
		int w = glszm_raw[0].length;
		double[][] norm_glszm = new double[h][w];
		// init array
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				norm_glszm[y][x] = 0d;
			}
		}

		for (int i = 0; i < h; i++) {
			for (int j = 0; j < w; j++) {
				norm_glszm[i][j] = glszm_raw[i][j] / Ns;
			}
		}
		return norm_glszm;
	}

	/*
	 * old implementation
	 * it is occur stackoverflow
	 */
//	private int countNeighbor(int grayLevel, Integer[][][] voxels, Integer sizeOfZone, int seedX, int seedY, int seedZ) {
//		/*
//		 * IMPORTANT
//		 */
//		/*************************************************/
//		if (voxels[seedZ][seedY][seedX] == null || voxels[seedZ][seedY][seedX] == Integer.MIN_VALUE) {
//			return sizeOfZone;
//		}
//		/*************************************************/
//
//		if (voxels[seedZ][seedY][seedX] == Integer.valueOf(grayLevel)) {
//			sizeOfZone++;
//			voxels[seedZ][seedY][seedX] = Integer.MIN_VALUE;
//		}
//		ArrayList<Point3i> connect26 = connectedNeighbor(seedX, seedY, seedZ, w, h, s);
//		for(Point3i np : connect26) {
//			if(np == null) {
//				//out of pixels coordinate
//				continue;
//			}
//			if(voxels[np.z][np.y][np.x] == null) {
//				continue;
//			}
//			if(voxels[np.z][np.y][np.x] == Integer.MIN_VALUE) {
//				continue;
//			}
//			if(voxels[np.z][np.y][np.x] == grayLevel) {
//				sizeOfZone = countNeighbor(grayLevel, voxels, sizeOfZone, np.x, np.y, np.z);
//			}
//		}
//		return sizeOfZone;
//	}
	
	private int countNeighbor2(final int grayLevel, Integer[][][] voxels, Integer sizeOfZone, int seedX, int seedY, int seedZ) {
		/*
		 * IMPORTANT
		 */
		/*************************************************/
		if (voxels[seedZ][seedY][seedX] == null || voxels[seedZ][seedY][seedX] == Integer.MIN_VALUE) {
			return sizeOfZone;
		}
		/*************************************************/

		if (voxels[seedZ][seedY][seedX] == Integer.valueOf(grayLevel)) {
			sizeOfZone++;
			voxels[seedZ][seedY][seedX] = Integer.MIN_VALUE;
		}
		//26 connected
		ArrayList<Point3i> connected = connectedNeighbor2(voxels, grayLevel, seedX, seedY, seedZ, w, h, s);
		int seedX2 = 0;
		int seedY2 = 0;
		int seedZ2 = 0;
		while(connected!=null) {
			if(connected != null) {
				ArrayList<Point3i> connected_itr = new ArrayList<>();
				for (Point3i np : connected) {
					if (np == null) {
						// out of pixels coordinate
						continue;
					}
					if (voxels[np.z][np.y][np.x] == null) {
						continue;
					}
					if (voxels[np.z][np.y][np.x] == Integer.MIN_VALUE) {
						continue;
					}
					if (voxels[np.z][np.y][np.x] == grayLevel) {
						sizeOfZone++;
						voxels[np.z][np.y][np.x] = Integer.MIN_VALUE;
						seedX2 = np.x;
						seedY2 = np.y;
						seedZ2 = np.z;
						ArrayList<Point3i> connected_ = connectedNeighbor2(voxels, grayLevel, seedX2, seedY2, seedZ2, w, h, s);
						if(connected_ != null) {
							connected_itr.addAll(connected_);
						}
					}
				}
				connected.addAll(connected_itr);
				connected = updateConnectedNeighbours(voxels, grayLevel, connected);
			}
		}
		return sizeOfZone;
	}
	
	
//	private ArrayList<Point3i> connectedNeighbor(int seedX, int seedY, int seedZ, int max_w , int max_h, int max_s){
//		ArrayList<Point3i> connect26 = new ArrayList<Point3i>();
//		for(Integer a_id:angle_ids) {
//			if(Integer.valueOf(13) == a_id) {
//				continue;//0,0,0
//			}
//			int[] a = angles.get(a_id);
//			int nX = seedX+a[2];
//			int nY = seedY+(a[1]*-1);
//			int nZ = seedZ+a[0];
//			Point3i neighbor = new Point3i(nX,nY,nZ);
//			if(!Utils.isOutOfRange(new Point3i(nX,nY,nZ), max_w , max_h, max_s)) connect26.add(neighbor);
//		}
//		return connect26;
//	}
	
	private ArrayList<Point3i> connectedNeighbor2(Integer[][][] voxels, final int grayLevel, final int seedX, final int seedY, final int seedZ, final int max_w , final int max_h, final int max_s){
		ArrayList<Point3i> connected = new ArrayList<Point3i>();
		for(Integer a_id:angle_ids) {
			if(Integer.valueOf(13) == a_id) {
				continue;//0,0,0
			}
			int[] a = angles.get(a_id);
			int nX = seedX+a[2];
			int nY = seedY+(a[1]*-1);//adjust y direction from vector
			int nZ = seedZ+a[0];
			Point3i neighbour = new Point3i(nX,nY,nZ);
			if(!Utils.isOutOfRange(neighbour, max_w, max_h, max_s)) {
				if(voxels[nZ][nY][nX] != null) {
					if(voxels[nZ][nY][nX] == grayLevel) {
						connected.add(neighbour);
					}
				}
			}
		}
		return connected.size() > 0 ? connected:null;
	}
	
	private ArrayList<Point3i> updateConnectedNeighbours(final Integer[][][] voxels, final int grayLevel, ArrayList<Point3i> connected) {
		if(connected == null) {
			return null;
		}
		if(connected.size() ==0 || connected.isEmpty()) {
			connected = null;
			return null;
		}
		ArrayList<Point3i> remove = new ArrayList<Point3i>();
		for(Point3i p:connected) {
			if (voxels[p.z][p.y][p.x] == null || voxels[p.z][p.y][p.x] == Integer.MIN_VALUE) {
				remove.add(p);
			}
		}
		connected.removeAll(remove);
		if(connected.size() ==0 || connected.isEmpty()) {
			connected = null;
			return null;
		}else {
			return connected;
		}
	}
	
	
	/*
	 * grayLevel - <zone size, zone count>
	 */
	private double[][] map2matrix(HashMap<Integer, HashMap<Integer,Integer>> glszm_map){
		int size_max = 0;
		Ns = 0.0d;
		for(Integer gl:glszm_map.keySet()) {
			HashMap<Integer,Integer> size_count_pair = glszm_map.get(gl);
			for(Integer size:size_count_pair.keySet()) {
				if(size > size_max) {
					size_max = size;
				}
			}
		}
		glszm_raw = new double[nBins][size_max];//[row][col]
		for(int row=1;row<=nBins;row++) {
			HashMap<Integer,Integer> size_count_pair = glszm_map.get(row);
			if(size_count_pair == null) {
				continue;
			}
			for(int col=1;col<=size_max;col++) {
				Integer count = size_count_pair.get(col);
				if(count == null) {
					count = 0;
				}
				glszm_raw[row-1][col-1] = (double)count;
				Ns += (double)count;
			}
		}
		return glszm_raw;
	}
	
	private void calculateCoefficients(){
		mu_i = 0.0;
		mu_j = 0.0;
		// sum the glszm mu
		for (int i=1; i<=glszm.length; i++){
			for (int j=1; j<=glszm[0].length; j++) {
				mu_i += glszm [i-1][j-1] * i;
				mu_j += glszm [i-1][j-1] * j;
			} 
		}
	}
	
	public Double calculate(String id) {
		String name = GLSZMFeatureType.findType(id);
		if (name.equals(GLSZMFeatureType.SmallZoneEmphasis.name())) {
			return getSmallZoneEmphasis();
		} else if (name.equals(GLSZMFeatureType.LargeZoneEmphasis.name())) {
			return getLargeZoneEmphasis();
		} else if (name.equals(GLSZMFeatureType.GrayLevelNonUniformity.name())) {
			return getGrayLevelNonUniformity();
		} else if (name.equals(GLSZMFeatureType.GrayLevelNonUniformityNormalized.name())) {
			return getGrayLevelNonUniformityNormalized();
		} else if (name.equals(GLSZMFeatureType.SizeZoneNonUniformity.name())) {
			return getSizeZoneNonUniformity();
		} else if (name.equals(GLSZMFeatureType.SizeZoneNonUniformityNormalized.name())) {
			return getSizeZoneNonUniformityNormalized();
		} else if (name.equals(GLSZMFeatureType.ZonePercentage.name())) {
			return getZonePercentage();
		} else if (name.equals(GLSZMFeatureType.GrayLevelVariance.name())) {
			return getGrayLevelVariance();
		} else if (name.equals(GLSZMFeatureType.ZoneSizeVariance.name())) {
			return getZoneVariance();
		} else if (name.equals(GLSZMFeatureType.ZoneSizeEntropy.name())) {
			return getZoneEntropy();
		} else if (name.equals(GLSZMFeatureType.LowGrayLevelZoneEmphasis.name())) {
			return getLowGrayLevelZoneEmphasis();
		} else if (name.equals(GLSZMFeatureType.HighGrayLevelZoneEmphasis.name())) {
			return getHighGrayLevelZoneEmphasis();
		} else if (name.equals(GLSZMFeatureType.SmallZoneLowGrayLevelEmphasis.name())) {
			return getSmallZoneLowGrayLevelEmphasis();
		} else if (name.equals(GLSZMFeatureType.SmallZoneHighGrayLevelEmphasis.name())) {
			return getSmallZoneHighGrayLevelEmphasis();
		} else if (name.equals(GLSZMFeatureType.LargeZoneLowGrayLevelEmphasis.name())) {
			return getLargeZoneLowGrayLevelEmphasis();
		} else if (name.equals(GLSZMFeatureType.LargeZoneHighGrayLevelEmphasis.name())) {
			return getLargeZoneHighGrayLevelEmphasis();
		}
		return null;
	}
	
	private Double getSmallZoneEmphasis() {
		Double sae = 0.0d;
		for(int j=1; j<=glszm[0].length; j++) {
			double s_j = 0d;
			for(int i=1; i<=glszm.length; i++) {
				s_j += glszm[i-1][j-1];
			}
			sae += s_j/(j*j);
		}
//		sae /= Nz;//already normalized
		return sae;
	}
	
	private Double getLargeZoneEmphasis() {
		Double lae = 0.0d;
		for(int j=1; j<=glszm[0].length; j++) {
			double s_j = 0d;
			for(int i=1; i<=glszm.length; i++) {
				s_j += glszm[i-1][j-1];
			}
			lae += s_j*(j*j);
		}
//		lae /= Nz;//already normalized
		return lae;
	}
	
	
	private Double getLowGrayLevelZoneEmphasis() {
		Double lglze = 0.0d;
		for(int i=1; i<=glszm.length; i++) {
			double s_i = 0d;
			for(int j=1; j<=glszm[0].length; j++) {
				s_i += glszm[i-1][j-1];
			}
			lglze += s_i/(i*i);
		}
//		return lglze/Nz;//already normalized
		return lglze;
	}
	
	private Double getHighGrayLevelZoneEmphasis() {
		Double lglze = 0.0d;
		for(int i=1; i<=glszm.length; i++) {
			double s_i = 0d;
			for(int j=1; j<=glszm[0].length; j++) {
				s_i += glszm[i-1][j-1];
			}
			lglze += s_i*(i*i);
		}
//		return lglze/Nz;//already normalized
		return lglze;
	}
	
	
	private Double getSmallZoneLowGrayLevelEmphasis() {
		Double salgle = 0.0d;
		for (int i = 1; i <= glszm.length; i++) {
			for (int j = 1; j <= glszm[0].length; j++) {
				salgle += (glszm[i - 1][j - 1]) / ((double)i * (double)i * (double)j * (double)j);
			}
		}
//		return salgle/Nz;
		return salgle;//already normalized
	}
	
	private Double getSmallZoneHighGrayLevelEmphasis() {
		Double sahgle = 0.0d;
		for(int i=1; i<=glszm.length; i++) {
			for(int j=1; j<=glszm[0].length; j++) {
				sahgle += (glszm[i-1][j-1]*i*i)/(j*j);
			}
		}
//		return sahgle/Nz;
		return sahgle;//already normalized
	}
	
	private Double getLargeZoneLowGrayLevelEmphasis() {
		Double lalgle = 0.0d;
		for(int i=1; i<=glszm.length; i++) {
			for(int j=1; j<=glszm[0].length; j++) {
				lalgle += (glszm[i-1][j-1]*(double)j*(double)j)/((double)i*(double)i);
			}
		}
//		return lalgle/Nz;
		return lalgle;//already normalized
	}
	
	private Double getLargeZoneHighGrayLevelEmphasis() {
		Double lahgle = 0.0d;
		for(int i=1; i<=glszm.length; i++) {
			for(int j=1; j<=glszm[0].length; j++) {
				lahgle += glszm[i-1][j-1]*i*i*j*j;
			}
		}
//		return lahgle/Nz;
		return lahgle;//already normalized
	}
	
	private Double getGrayLevelNonUniformity() {
		Double gln = 0.0d;
		for(int i=1; i<=glszm.length; i++) {
			double s_i = 0.0;
			for(int j=1; j<=glszm[0].length; j++) {
				s_i += glszm_raw[i-1][j-1];//IMPORTANT
			}
			gln += (s_i * s_i);
		}
		return gln/Ns;
	}
	
	private Double getGrayLevelNonUniformityNormalized() {
		Double glnn = 0.0d;
		for(int i=1; i<=glszm.length; i++) {
			double s_i = 0.0;
			for(int j=1; j<=glszm[0].length; j++) {
				s_i += glszm[i-1][j-1];//IMPORTANT
			}
			glnn += (s_i * s_i);
		}
		return glnn;//already normalized
	}
	
	private Double getSizeZoneNonUniformity() {
		Double szn = 0.0d;
		for(int j=1; j<=glszm[0].length; j++) {
			double inner = 0.0;
			for(int i=1; i<=glszm.length; i++) {
				inner += glszm_raw[i-1][j-1];//IMPORTANT
			}
			szn += (inner * inner);
		}
		szn /= Ns;
		return szn;
	}
	
	private Double getSizeZoneNonUniformityNormalized() {
		Double sznn = 0.0d;
		for(int j=1; j<=glszm[0].length; j++) {
			double inner = 0.0;
			for(int i=1; i<=glszm.length; i++) {
				inner += glszm[i-1][j-1];//IMPORTANT
			}
			sznn += (inner * inner);
		}
		return sznn;
	}
	
	private Double getZonePercentage() {
		int Nv = Utils.getVoxels(discImg, mask, this.label).length;
		if(Nv == 0) {
			return null;
		}
		return  Ns/Nv;
	}
	
	private Double getGrayLevelVariance() {
		Double glv = 0.0d;
		for(int i=1; i<=glszm.length; i++) {
			for(int j=1; j<=glszm[0].length; j++) {
				glv += glszm[i-1][j-1] * Math.pow(i-mu_i,2);
			}
		}
		return glv;
	}
	
	/*
	 * zone size variance
	 */
	private Double getZoneVariance() {
		Double zv = 0.0d;
		for(int i=1; i<=glszm.length; i++) {
			for(int j=1; j<=glszm[0].length; j++) {
				zv += glszm[i-1][j-1] * Math.pow(j-mu_j,2);
			}
		}
		return zv;
	}
	
	private Double getZoneEntropy() {
		Double ze = 0.0d;
		for(int i=1; i<=glszm.length; i++) {
			for(int j=1; j<=glszm[0].length; j++) {
				double p_ij = glszm[i-1][j-1];
				if(p_ij == 0d) {
					p_ij = eps;
				}
				ze -= p_ij * (Math.log(p_ij)/Math.log(2.0));
			}
		}
		return ze;
	}

	
	/** This method returns the size zone matrix.
	 * @return The 'Size Zone Matrix'.*/
	public double[][] getMatrix(boolean raw){
		if(!raw) {
			return glszm;//normalized
		}else {
			return glszm_raw;
		}
	}

	public String toString(double[][] glszm){
		StringBuffer sb = new StringBuffer() ;
		for (int j=0; j<glszm.length;j++) {
			for (int i=0; i<glszm[0].length;i++) {
				sb.append(glszm[j][i] + " ");
			}
			sb.append("\n");
		}
		return sb.toString() ;
	}
	
	public String toString(){
		StringBuffer sb = new StringBuffer() ;
		sb.append("row[↓]:gray level (1 to N)"+"\n");
		sb.append("col[→]:zone size (1 to N)"+"\n");
		sb.append("=========================="+"\n");
		for (int j=0; j<glszm_raw.length;j++) {
			for (int i=0; i<glszm_raw[0].length;i++) {
				sb.append(glszm_raw[j][i] + " ");
			}
			sb.append("\n");
		}
		return sb.toString() ;
	}


	@Override
	public Set<String> getAvailableFeatures() {
		Set<String> names = new HashSet<String>();
		for(GLSZMFeatureType t : GLSZMFeatureType.values()) {
			names.add(t.name());
		}
		return names;
	}


	@Override
	public String getFeatureFamilyName() {
		return "GLSZM";
	}


	@Override
	public Map<String, Object> getSettings() {
		return settings;
	}
}