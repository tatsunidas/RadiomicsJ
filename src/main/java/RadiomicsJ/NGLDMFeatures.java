package RadiomicsJ;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import javax.swing.JOptionPane;

import org.scijava.vecmath.Point3i;

import ij.ImagePlus;
import ij.process.ImageProcessor;

/**
 * @author tatsunidas <t_kobayashi@vis-ionary.com>
 *
 */
public class NGLDMFeatures {
	
	double[][] gldm = null;
	HashMap<Integer, HashMap<Integer,Integer>> gldm_map;
	int alpha = 0;//coarseness parameter of neighbor
	int delta = 1;//neighbor range
	HashMap<Integer, int[]> angles = Utils.buildAngles();
	ArrayList<Integer> angle_ids;
	
	ImagePlus img;
	ImagePlus mask;
	ImagePlus discImg;
	
	int w;
	int h;
	int s;
	
	int label = 1;
	int nBins;//discretised gray level 1 to N
	
	//basic stats
	double Ns = 0.0;//sum of GLDM(without normalization)
	int size_max = 0;//Nd
	double eps = Math.ulp(1.0);// 2.220446049250313E-16
	
	public NGLDMFeatures(ImagePlus img, ImagePlus mask, int label, Integer alpha, Integer delta, boolean useBinCount, Integer nBins, Double binWidth) throws Exception {
		if (img == null) {
			return;
		} else {
			if (img.getType() == ImagePlus.COLOR_RGB) {
				JOptionPane.showMessageDialog(null, "RadiomicsJ can read only grayscale images(8/16/32 bits)...sorry.");
				return;
			}
			this.label = label;
			
			if (mask != null) {
				if (img.getWidth() != mask.getWidth() || img.getHeight() != mask.getHeight()) {
					JOptionPane.showMessageDialog(null, "RadiomicsJ: please input same dimension image and mask.");
					return;
				}
			}else {
				// create full face mask
				mask = ImagePreprocessing.createMask(img.getWidth(), img.getHeight(), img.getNSlices(), null,
						this.label, img.getCalibration().pixelWidth,img.getCalibration().pixelHeight,img.getCalibration().pixelDepth);
			}
			
			if(nBins != null) {
				this.nBins = nBins;
			}else {
				this.nBins = RadiomicsJ.nBins;
			}
			
			if(binWidth == null) {
				binWidth = RadiomicsJ.binWidth;
			}
			
			if (delta != null && delta > 0) {
				this.delta = delta;
			}else {
				this.delta = RadiomicsJ.deltaNGLevelDM;
			}
			
			if(alpha != null  && alpha >= 0) {
				this.alpha = alpha;
			}else {
				this.alpha = RadiomicsJ.alpha;
			}
			
			this.img = img;
			this.mask = mask;
			
			// discretised by roi mask.
			if(RadiomicsJ.discretisedImp != null) {
				discImg = RadiomicsJ.discretisedImp;
			}else {
				if(useBinCount) {
					discImg = Utils.discrete(this.img, this.mask, this.label, this.nBins);
				}else {
					/*
					 * do Fixed Bin Width
					 */
					discImg = Utils.discreteByBinWidth(this.img, this.mask, this.label, binWidth);
					this.nBins = Utils.getNumOfBinsByMax(discImg, this.mask, this.label);
				}
			}
			w = discImg.getWidth();
			h = discImg.getHeight();
			s = discImg.getNSlices();
			
			angle_ids = new ArrayList<>(angles.keySet());//0 to 26
			Collections.sort(angle_ids);
			
			fillGLDM2();
		}
	}
	
	
	public void fillGLDM() {
		int w = discImg.getWidth();
		int h = discImg.getHeight();
		int s = discImg.getNSlices();
		gldm_map = new HashMap<Integer, HashMap<Integer,Integer>>();
		for(int grayLevel=1;grayLevel<=nBins;grayLevel++) {
			//size and count
			HashMap<Integer,Integer> gldm_row = new HashMap<Integer,Integer>();
			for(int z=0;z<s;z++) {
				for (int y = 0; y < h; y++) {
					for (int x = 0; x < w; x++) {
						int lbl = (int) mask.getStack().getProcessor(z+1).getPixelValue(x, y);
						if(lbl != label) {
							continue;
						}
						float val = discImg.getStack().getProcessor(z+1).getPixelValue(x, y);
						if(Float.isNaN(val)) {
							continue;
						}
						if (((int)val) == grayLevel) {
							ArrayList<Point3i> neighbor = connectedNeighbor(x,y,z,w,h,s);
							int count = 0;
							for (Point3i p : neighbor) {
//								discImg.setSlice(p.z+1);
//								mask.setSlice(p.z+1);
								float fv = discImg.getStack().getProcessor(p.z+1).getPixelValue(p.x, p.y);
								if(!Float.isNaN(fv) && (int)mask.getStack().getProcessor(p.z+1).getPixelValue(p.x, p.y)==this.label) {
									if (Math.abs(grayLevel - ((int)fv)) <= alpha) {
										count++;
									}
								}
							}
							gldm_row.put(count, gldm_row.get(count) == null ? 1 : gldm_row.get(count) + 1);
						}
					}
				}
			}
			gldm_map.put(grayLevel, gldm_row);
		}
		map2matrix(gldm_map);
	}
	
	
	public void fillGLDM2() {
		gldm_map = new HashMap<Integer, HashMap<Integer,Integer>>();
		for(int grayLevel=1;grayLevel<=nBins;grayLevel++) {
			//size and count
			HashMap<Integer,Integer> gldm_row = new HashMap<Integer,Integer>();
			for(int z=0;z<s;z++) {
				float[][] iSlice = discImg.getStack().getProcessor(z+1).getFloatArray();
				float[][] mSlice = mask.getStack().getProcessor(z+1).getFloatArray();
				for (int y = 0; y < h; y++) {
					for (int x = 0; x < w; x++) {
						int lbl = (int) mSlice[x][y];
						if(lbl != label) {
							continue;
						}
						float val = iSlice[x][y];
						if(Float.isNaN(val)) {
							continue;
						}
						if (((int)val) == grayLevel) {
							ArrayList<Point3i> neighbor = connectedNeighbor(x,y,z,w,h,s);
							int count = 0;
							for (Point3i p : neighbor) {
								ImageProcessor ip = discImg.getStack().getProcessor(p.z+1);
								ImageProcessor mp = mask.getStack().getProcessor(p.z+1);
								float fv = ip.getf(p.x, p.y);
								if(!Float.isNaN(fv) && (int)mp.getf(p.x, p.y)==this.label) {
									if (Math.abs(grayLevel - ((int)fv)) <= alpha) {
										count++;
									}
								}
							}
							gldm_row.put(count, gldm_row.get(count) == null ? 1 : gldm_row.get(count) + 1);
						}
					}
				}
			}
			gldm_map.put(grayLevel, gldm_row);
		}
		map2matrix(gldm_map);
	}
	
		
//	private ArrayList<Point3i> connectedNeighbor(int seedX, int seedY, int seedZ, int max_w , int max_h, int max_z){
//		ArrayList<Point3i> neighbor = new ArrayList<Point3i>();
//		for(int z=seedZ-delta;z<=seedZ+delta;z++) {
//			for(int y=seedY-delta;y<=seedY+delta;y++) {
//				for(int x=seedX-delta;x<=seedX+delta;x++) {
//					if(x == seedX && y == seedY && z == seedZ) {
//						continue;
//					}
//					Point3i np = new Point3i(x, y, z);
//					if(!Utils.isOutOfRange(np, max_w , max_h, max_z)) {
//						neighbor.add(np);
//					}
//				}
//			}
//		}
//		return neighbor;
//	}
	
	private ArrayList<Point3i> connectedNeighbor(int seedX, int seedY, int seedZ, int max_w , int max_h, int max_s){
		
		ArrayList<Point3i> connect26 = new ArrayList<Point3i>();
		for(Integer a_id:angle_ids) {
			if(Integer.valueOf(13) == a_id) {
				continue;//0,0,0
			}
			int[] a = angles.get(a_id);
			int nX = seedX+(a[2]*delta);
			int nY = seedY+(a[1]*delta);
			int nZ = seedZ+(a[0]*delta);
			Point3i neighbor = new Point3i(nX,nY,nZ);
			if(!Utils.isOutOfRange(new Point3i(nX,nY,nZ), max_w , max_h, max_s)) connect26.add(neighbor);
		}
		return connect26;
	}
	
	
	private double[][] map2matrix(HashMap<Integer, HashMap<Integer,Integer>> gldm_map){
		size_max = 0;
		for(Integer gl:gldm_map.keySet()) {
			HashMap<Integer,Integer> size_count_pair = gldm_map.get(gl);
			for(Integer size:size_count_pair.keySet()) {
				if(size > size_max) {
					size_max = size;
				}
			}
		}
		gldm = new double[nBins][size_max+1];// 0 to num of count
		Ns = 0.0;
		for(int row=1;row<=nBins;row++) {
			HashMap<Integer,Integer> size_count_pair = gldm_map.get(row);
			for(int col=0;col<=size_max;col++) { //start from 0
				Integer count = size_count_pair.get(col);
				if(count == null) {
					count = 0;
				}
				gldm[row-1][col] = (double)count; 
				Ns += (double)count;
			}
		}
		return gldm;
	}
	

	public Double calculate(String id) {
		String name = NGLDMFeatureType.findType(id);
		if (name.equals(NGLDMFeatureType.LowDependenceEmphasis.name())) {
			return getLowDependenceEmphasis();
		} else if (name.equals(NGLDMFeatureType.HighDependenceEmphasis.name())) {
			return getHighDependenceEmphasis();
		} else if(name.equals(NGLDMFeatureType.LowGrayLevelCountEmphasis.name())) {
			return getLowGrayLevelEmphasis();
		} else if(name.equals(NGLDMFeatureType.HighGrayLevelCountEmphasis.name())) {
			return getHighGrayLevelEmphasis();
		} else if (name.equals(NGLDMFeatureType.LowDependenceLowGrayLevelEmphasis.name())) {
			return getLowDependenceLowGrayLevelEmphasis();
		} else if (name.equals(NGLDMFeatureType.LowDependenceHighGrayLevelEmphasis.name())) {
			return getLowDependenceHighGrayLevelEmphasis();
		} else if (name.equals(NGLDMFeatureType.HighDependenceLowGrayLevelEmphasis.name())) {
			return getHighDependenceLowGrayLevelEmphasis();
		} else if (name.equals(NGLDMFeatureType.HighDependenceHighGrayLevelEmphasis.name())) {
			return getHighDependenceHighGrayLevelEmphasis();
		} else if (name.equals(NGLDMFeatureType.GrayLevelNonUniformity.name())) {
			return getGrayLevelNonUniformity();
		} else if (name.equals(NGLDMFeatureType.GrayLevelNonUniformityNormalized.name())) {
			return getGrayLevelNonUniformityNormalized();
		} else if (name.equals(NGLDMFeatureType.DependenceCountNonUniformity.name())) {
			return getDependenceNonUniformity();
		} else if (name.equals(NGLDMFeatureType.DependenceCountNonUniformityNormalized.name())) {
			return getDependenceNonUniformityNormalized();
		} else if (name.equals(NGLDMFeatureType.DependenceCountPercentage.name())) {
			return getDependencePercentage();
		} else if (name.equals(NGLDMFeatureType.GrayLevelVariance.name())) {
			return getGrayLevelVariance();
		} else if (name.equals(NGLDMFeatureType.DependenceCountVariance.name())) {
			return getDependenceVariance();
		} else if (name.equals(NGLDMFeatureType.DependenceCountEntropy.name())) {
			return getDependenceEntropy();
		} else if (name.equals(NGLDMFeatureType.DependenceCountEnergy.name())) {
			return getDependenceCountEnergy();
		}
		return null;
	}
	
	private Double getLowDependenceEmphasis() {
		Double sde = 0.0d;
		for(int j=1; j<=gldm[0].length; j++) {
			double sj = 0d;
			for(int i=1; i<=gldm.length; i++) {
				sj += gldm[i-1][j-1];
			}
			sde += sj/(j*j);
		}
		return sde/Ns;
	}
	
	private Double getHighDependenceEmphasis() {
		Double lde = 0.0d;
		for(int j=1; j<=gldm[0].length; j++) {
			double sj = 0d;
			for(int i=1; i<=gldm.length; i++) {
				sj += gldm[i-1][j-1];
			}
			lde += sj*j*j;
		}
		return lde/Ns;
	}
	
	private Double getLowGrayLevelEmphasis() {
		Double lgle = 0.0d;
		for(int i=1; i<=gldm.length; i++) {
			double si = 0d;
			for(int j=1; j<=gldm[0].length; j++) {
				si += gldm[i-1][j-1];
			}
			lgle += si/(i*i);
		}
		return lgle/Ns;
	}
	
	private Double getHighGrayLevelEmphasis() {
		Double hgle = 0.0d;
		for(int i=1; i<=gldm.length; i++) {
			double si = 0d;
			for(int j=1; j<=gldm[0].length; j++) {
				si += gldm[i-1][j-1];
			}
			hgle += si*i*i;
		}
		return hgle/Ns;
	}
	
	
	private Double getLowDependenceLowGrayLevelEmphasis() {
		Double sdlgle = 0.0d;
		for(int i=1; i<=gldm.length; i++) {
			for(int j=1; j<=gldm[0].length; j++) {
				sdlgle += gldm[i-1][j-1] / (i*i*j*j);
			}
		}
		return sdlgle/Ns;
	}
	
	private Double getLowDependenceHighGrayLevelEmphasis() {
		Double sdhgle = 0.0d;
		for(int i=1; i<=gldm.length; i++) {
			for(int j=1; j<=gldm[0].length; j++) {
				sdhgle += (gldm[i-1][j-1]*i*i)/(j*j);
			}
		}
		return sdhgle/Ns;
	}
	
	private Double getHighDependenceLowGrayLevelEmphasis() {
		Double ldlgle = 0.0d;
		for(int i=1; i<=gldm.length; i++) {
			for(int j=1; j<=gldm[0].length; j++) {
				ldlgle += (gldm[i-1][j-1]*j*j)/(i*i);
			}
		}
		return ldlgle/Ns;
	}
	
	private Double getHighDependenceHighGrayLevelEmphasis() {
		Double ldhgle = 0.0d;
		for(int i=1; i<=gldm.length; i++) {
			for(int j=1; j<=gldm[0].length; j++) {
				ldhgle += gldm[i-1][j-1]*i*i*j*j;
			}
		}
		return ldhgle/Ns;
	}
	
	private Double getGrayLevelNonUniformity() {
		Double gln = 0.0d;
		for(int i=1; i<=gldm.length; i++) {
			double inner = 0.0;
			for(int j=1; j<=gldm[0].length; j++) {
				inner += gldm[i-1][j-1];
			}
			gln += (inner * inner);
		}
		return gln/Ns;
	}
	
	private Double getGrayLevelNonUniformityNormalized() {
		Double glnn = 0.0d;
		for(int i=1; i<=gldm.length; i++) {
			double inner = 0.0;
			for(int j=1; j<=gldm[0].length; j++) {
				inner += gldm[i-1][j-1];
			}
			glnn += (inner * inner);
		}
		return glnn/(Ns*Ns);
	}
	
	
	private Double getDependenceNonUniformity() {
		Double dn = 0.0d;
		for(int j=1; j<=gldm[0].length; j++) {
			double inner = 0.0;
			for(int i=1; i<=gldm.length; i++) {
				inner += gldm[i-1][j-1];
			}
			dn += (inner * inner);
		}
		return dn/Ns;
	}
	
	private Double getDependenceNonUniformityNormalized() {
		Double dnn = 0.0d;
		for(int j=1; j<=gldm[0].length; j++) {
			double inner = 0.0;
			for(int i=1; i<=gldm.length; i++) {
				inner += gldm[i-1][j-1];
			}
			dnn += (inner * inner);
		}
		return dnn/(Ns*Ns);
	}
	
	/*
	 * Dependence count percentage may be completely omitted as it evaluates to 1 when complete neighbourhoods are not required, as is the case under our definition.
	 */
	@Deprecated
	private Double getDependencePercentage() {
		return 1d;
	}
	
	private Double getGrayLevelVariance() {
		double myu_i = 0d;
		for(int i=1; i<=gldm.length; i++) {
			for(int j=1; j<=gldm[0].length; j++) {
				myu_i += i*(gldm[i-1][j-1]/Ns);
			}
		}
		Double glv = 0.0d;
		for(int i=1; i<=gldm.length; i++) {
			for(int j=1; j<=gldm[0].length; j++) {
				glv += (gldm[i-1][j-1]/Ns) * Math.pow(i-myu_i,2);
			}
		}
		return glv;
	}
	
	private Double getDependenceVariance() {
		double myu_j = 0d;
		for(int i=1; i<=gldm.length; i++) {
			for(int j=1; j<=gldm[0].length; j++) {
				myu_j += j*(gldm[i-1][j-1]/Ns);
			}
		}
		Double dv = 0.0d;
		for(int i=1; i<=gldm.length; i++) {
			for(int j=1; j<=gldm[0].length; j++) {
				dv += (gldm[i-1][j-1]/Ns) * Math.pow(j-myu_j,2);
			}
		}
		return dv;
	}
	
	private Double getDependenceEntropy() {
		Double de = 0.0d;
		for(int i=1; i<=gldm.length; i++) {
			for(int j=1; j<=gldm[0].length; j++) {
				if(gldm[i-1][j-1] == 0.0) {
					double pij = (gldm[i-1][j-1]+eps)/Ns;
					de -= pij * (Math.log(pij)/Math.log(2.0));
				}else {
					de -= (gldm[i-1][j-1]/Ns) * (Math.log(gldm[i-1][j-1]/Ns)/Math.log(2.0));
				}
			}
		}
		return de;
	}
	
	private Double getDependenceCountEnergy() {
		Double de = 0.0d;
		for(int i=1; i<=gldm.length; i++) {
			for(int j=1; j<=gldm[0].length; j++) {
				if(gldm[i-1][j-1] == 0.0) {
					double pij = (gldm[i-1][j-1]+eps)/Ns;
					de += pij * pij;
				}else {
					double pij = gldm[i-1][j-1]/Ns;
					de += pij*pij;
				}
			}
		}
		return de;
	}
	
	
	public double[][] getMatrix(){
		return gldm ;
	}

	public String toString(){
		StringBuffer sb = new StringBuffer() ;
		for (int j=0; j<gldm.length;j++) {
			for (int i=0; i<gldm[0].length;i++) {
				sb.append(gldm[j][i] + " ");
			}
			sb.append("\n");
		}
		return sb.toString() ;
	}
}