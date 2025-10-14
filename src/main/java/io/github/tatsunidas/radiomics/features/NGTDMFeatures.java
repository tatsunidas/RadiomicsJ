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

import ij.IJ;
import ij.ImagePlus;
import ij.measure.Calibration;
import ij.process.ImageProcessor;
import io.github.tatsunidas.radiomics.main.ImagePreprocessing;
import io.github.tatsunidas.radiomics.main.RadiomicsJ;
import io.github.tatsunidas.radiomics.main.Utils;

/**
 * @author tatsunidas <t_kobayashi@vis-ionary.com>
 */
public class NGTDMFeatures extends AbstractRadiomicsFeature implements Texture{

	/*
	 * ngtdm does not need normalize
	 */
	double[][] ngtdm = null;//[i, Ni, Pi, Si] in row
	
	ImagePlus discImg;//discretized images by using roi mask.
	
	int w;
	int h;
	int s;
	
	final int label;
	int nBins;//discretised gray level
	double binWidth;
	int delta = 1;//search range
	HashMap<Integer, int[]> angles = Utils.buildAngles();
	ArrayList<Integer> angle_ids;
	
	//basic stats
	double Nvp;
	double Ngp; //number of gray levels, where pi not equal 0.0.
	double eps = Math.ulp(1.0);// 2.220446049250313E-16
	
	Map<String, Object> settings;
	
	public NGTDMFeatures(ImagePlus img, ImagePlus mask, Map<String,Object> settings) {
		super(img,mask,settings);
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

	/**
	 * 
	 * @param img: original image without descretized
	 * @param mask: ROI mask
	 * @param label: target intensity in mask
	 * @param delta: neighbor range
	 * @param useBinCount: if true, use FixedBinNumber, else use FixedBinSize with binWidth
	 * @param nBins: if useBinCount true, descretized by this nBins. if null, use default 32.
	 * @param binWidth : if useBinCount false, descretized by FixedBinSize
	 * @throws Exception
	 */
	public NGTDMFeatures(ImagePlus img/*no descretized*/, ImagePlus mask, int label, Integer delta, boolean useBinCount, Integer nBins, Double binWidth) throws Exception {
		super(img,mask,null);
		this.label = label;
		
		w = this.img.getWidth();
		h = this.img.getHeight();
		s = this.img.getNSlices();
		
		//check mask
		if (mask == null) {
			Calibration cal = img.getCalibration();
			double px = cal.pixelWidth;
			double py = cal.pixelHeight;
			double pz = cal.pixelDepth;
			this.mask = ImagePreprocessing.createMask(w, h, s, null, this.label, px, py, pz);
		}
		
		// discretised by roi mask.
		if(nBins != null) {
			this.nBins = nBins;
		}else {
			this.nBins = RadiomicsJ.nBins;
		}
		
		if(binWidth != null) {
			this.binWidth = binWidth;
		}else {
			this.binWidth = RadiomicsJ.binWidth;
		}
		
		if (RadiomicsJ.discretiseImp != null) {
			discImg = RadiomicsJ.discretiseImp;
		} else {
			if (useBinCount) {
				/*
				 * Fixed Bin Number
				 */
				discImg = Utils.discrete(this.img, this.mask, this.label, this.nBins);
			} else {
				/*
				 * Fixed Bin Size
				 */
				if(binWidth == null) {
					binWidth = RadiomicsJ.binWidth;
				}
				discImg = Utils.discreteByBinWidth(this.img, this.mask, this.label, binWidth);
				this.nBins = Utils.getNumOfBinsByMax(discImg,this.mask, this.label);
			}
		}
			
		if (delta != null && delta > 0) {
			this.delta = delta;
		} else {
			this.delta = RadiomicsJ.deltaNGToneDM;
		}

		angle_ids = new ArrayList<>(angles.keySet());// 0 to 26
		Collections.sort(angle_ids);
		fillNGTDM(false);
		
		settings = new HashMap<>();
		settings.put(RadiomicsFeature.IMAGE, img);
		settings.put(RadiomicsFeature.MASK, mask);
		settings.put(RadiomicsFeature.DISC_IMG, discImg);
		settings.put(RadiomicsFeature.LABEL, label);
		settings.put(RadiomicsFeature.DELTA, delta);
		settings.put(RadiomicsFeature.USE_BIN_COUNT, useBinCount);
		settings.put(RadiomicsFeature.nBins, this.nBins);
		settings.put(RadiomicsFeature.BinWidth, binWidth);
	}
	
	public void buildup(Map<String,Object> settings) {
		if (mask == null) {
			// create full face mask
			mask = ImagePreprocessing.createMask(img.getWidth(), img.getHeight(), img.getNSlices(), null, this.label,
					img.getCalibration().pixelWidth, img.getCalibration().pixelHeight, img.getCalibration().pixelDepth);
		}
		w = this.img.getWidth();
		h = this.img.getHeight();
		s = this.img.getNSlices();

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
				
		// discretised by roi mask.
		try {
			if (useBinCount) {
				discImg = Utils.discrete(this.img, this.mask, this.label, this.nBins);
			} else {
				// Fixed Bin Width
				discImg = Utils.discreteByBinWidth(this.img, this.mask, this.label, this.binWidth);
				this.nBins = Utils.getNumOfBinsByMax(discImg, this.mask, this.label);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		Object deltaValue = settings.get(RadiomicsFeature.DELTA);
		if (deltaValue != null && !(deltaValue instanceof Integer)) {
			throw new IllegalArgumentException("'delta' must be an Integer.");
		}
		if (deltaValue == null) {
			this.delta = RadiomicsJ.deltaNGToneDM;
		}else {
			this.delta = (Integer)deltaValue;
		}
				
		angle_ids = new ArrayList<>(angles.keySet());// 0 to 26
		Collections.sort(angle_ids);
		fillNGTDM(false);;
	}
	
	public void fillNGTDM(boolean amadaAlgorithms) {
		ngtdm = new double[nBins][4];//[i, Ni, Pi, Si]
		Nvp = 0d; //sum of neighbor i
		for(int grayLevel=1;grayLevel<=nBins;grayLevel++) {
			double si = 0d; //Neighbourhood grey tone difference
			int ni = 0;// the total number of voxels have this gray level
			for(int z=0;z<s;z++) {
				float[][] iSlice = discImg.getStack().getProcessor(z+1).getFloatArray();
				float[][] mSlice = mask.getStack().getProcessor(z+1).getFloatArray();
				for(int y=0;y<h;y++) {
					for(int x=0;x<w;x++) {
						//check voxel is in roi
						int lbl = (int) mSlice[x][y];
						if(lbl != this.label) {
							continue;
						}
						float val = iSlice[x][y];
						if(Float.isNaN(val)) {
							continue;
						}
						if(((int)val) == grayLevel) {
							double blob_sum = 0.0;
							int numOfValidNeighbor = 0;
							ArrayList<Point3i> neighbor = connectedNeighbor(x,y,z,w,h,s);
							for(Point3i p : neighbor) {
								if(!amadaAlgorithms) {
									int lbl_ = (int) mask.getStack().getProcessor(p.z + 1).getPixelValue(p.x, p.y);
									if (lbl_ != this.label) {
										continue;
									}
								}
								ImageProcessor ip = discImg.getStack().getProcessor(p.z+1);
								float fv = ip.getf(p.x, p.y);
								if(!Float.isNaN(fv)) {
									blob_sum += fv;
									numOfValidNeighbor++;
								}
							}
							if(numOfValidNeighbor != 0) {
								si += Math.abs(grayLevel-(blob_sum/numOfValidNeighbor));
							}
							ni++;
						}
					}
				}
			}
			ngtdm[grayLevel-1] = new double[]{(double)grayLevel, (double)ni, 0.0d, si};
			Nvp += ni;
			if(si != 0.0) {
				Ngp++;
			}
		}
		//finally, add "Pi"
		for(int grayLevel=1;grayLevel<=nBins;grayLevel++) {
			ngtdm[grayLevel-1][2] = ngtdm[grayLevel-1][1]/Nvp;
		}
	}
	
	private ArrayList<Point3i> connectedNeighbor(int seedX, int seedY, int seedZ, int max_w , int max_h, int max_s){
		ArrayList<Point3i> neighbors = new ArrayList<Point3i>();
		for(int i=1; i<= delta;i++) {
			for(Integer a_id:angle_ids) {
				if(Integer.valueOf(13) == a_id) {
					continue;//0,0,0
				}
				int[] a = angles.get(a_id);
				int nX = seedX+(a[2]*i);
				int nY = seedY+(a[1]*i*-1);
				int nZ = seedZ+(a[0]*i);
				Point3i neighbor = new Point3i(nX,nY,nZ);
				if(!Utils.isOutOfRange(new Point3i(nX,nY,nZ), max_w , max_h, max_s)) neighbors.add(neighbor);
			}
		}
		return neighbors;
	}
	
	public Double calculate(String id) {
		String name = NGTDMFeatureType.findType(id);
		if (name.equals(NGTDMFeatureType.Coarseness.name())) {
			return getCoarseness();
		} else if (name.equals(NGTDMFeatureType.Contrast.name())) {
			return getContrast();
		} else if (name.equals(NGTDMFeatureType.Busyness.name())) {
			return getBusyness();
		} else if (name.equals(NGTDMFeatureType.Complexity.name())) {
			return getComplexity();
		} else if (name.equals(NGTDMFeatureType.Strength.name())) {
			return getStrength();
		}
		return null;
	}
	
	private Double getCoarseness() {
		double coa = 0.0;
		for(int i=1;i<=nBins;i++) {
			coa += ngtdm[i-1][2] * ngtdm[i-1][3];
		}
		return 1.0/coa;
	}
	
	private Double getContrast() {
		double cntra = 0.0;
		double sumSi = 0.0d;
		for(int i=1;i<=nBins;i++) {
			for(int j=1;j<=nBins;j++) {
				if(i-j != 0 && ngtdm[i-1][2] != 0.0 && ngtdm[j-1][2] != 0.0) {
					cntra += ngtdm[i-1][2] * ngtdm[j-1][2] * Math.pow(i-j,2);
				}
			}
			sumSi += ngtdm[i-1][3];
		}
		return (1/(Ngp*(Ngp-1))) * cntra * (1/Nvp) * sumSi;
	}
	
	private Double getBusyness() {
		double busy1 = 0.0;
		double busy2 = 0.0;
		for(int i=1;i<=nBins;i++) {
			for(int j=1;j<=nBins;j++) {
				if(ngtdm[i-1][2] != 0.0 && ngtdm[j-1][2] != 0.0) {
					busy1 += Math.abs(i*ngtdm[i-1][2] - j*ngtdm[j-1][2]);
				}
			}
			if(ngtdm[i-1][2] != 0.0) {
				/*
				 * pi�?0のとき�?�無�?
				 */
				busy2 += ngtdm[i-1][2] * ngtdm[i-1][3];
			}
		}
		return busy2/busy1;
	}
	
	private Double getComplexity() {
		double comp = 0.0;
		for(int i=1;i<=nBins;i++) {
			for(int j=1;j<=nBins;j++) {
				if(i-j == 0) {
					continue;
				}
				if(ngtdm[i-1][2] != 0.0 && ngtdm[j-1][2] != 0.0) {
					/*
					 * pi,pj�?0のとき�?�無�?
					 */
					comp += Math.abs(i- j)*(ngtdm[i-1][2]*ngtdm[i-1][3]+ngtdm[j-1][2]*ngtdm[j-1][3])/(ngtdm[i-1][2]+ngtdm[j-1][2]);
				}
			}
		}
		return comp/Nvp;
	}
	
	private Double getStrength() {
		double stre = 0.0;
		double sumSi = 0.0;
		for(int i=1;i<=nBins;i++) {
			for(int j=1;j<=nBins;j++) {
				if(i-j == 0) {
					continue;
				}
				if(ngtdm[i-1][2] != 0.0 && ngtdm[j-1][2] != 0.0) {
					/*
					 * pi=0,pj=0 ignore.
					 */
					stre += (ngtdm[i-1][2]+ngtdm[j-1][2])*Math.pow(i-j,2);
				}
			}
			//when pi=0, si=0. np.
			sumSi += ngtdm[i-1][3];
		}
		return sumSi == 0.0 ? 0.0:stre/sumSi;
	}
	
	/** This method returns the size zone matrix.
	 * @return The 'Size Zone Matrix'.*/
	public double[][] getMatrix(){
		return ngtdm ;
	}

	public String toString(){
		StringBuffer sb = new StringBuffer() ;
		sb.append("i\tNi\tPi\tSi");
		sb.append("\n");
		for (int i=0; i<ngtdm.length;i++) {
			String gray = IJ.d2s(ngtdm[i][0], 0);
			String n = IJ.d2s(ngtdm[i][1], 0);
			String p = IJ.d2s(ngtdm[i][2], 3);
			String s = IJ.d2s(ngtdm[i][3], 3);
				sb.append(gray + "\t" + n + "\t" + p + "\t" + s);
				sb.append("\n");
		}
		return sb.toString() ;
	}

	@Override
	public Set<String> getAvailableFeatures() {
		Set<String> names = new HashSet<String>();
		for(NGTDMFeatureType t : NGTDMFeatureType.values()) {
			names.add(t.name());
		}
		return names;
	}

	@Override
	public String getFeatureFamilyName() {
		return "NGTDM";
	}

	@Override
	public Map<String, Object> getSettings() {
		return settings;
	}

}
