package RadiomicsJ;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import javax.swing.JOptionPane;

import org.scijava.vecmath.Point3i;

import ij.ImagePlus;
import ij.process.ImageProcessor;

public class NGTDMFeatures {

	/*
	 * ngtdm does not need normalize
	 */
	double[][] ngtdm = null;//[i, Ni, Pi, Si] in row
	
	ImagePlus img;//original
	ImagePlus mask;
	ImagePlus discImg;//discretised images by using roi mask.
	
	int w;
	int h;
	int s;
	
	int label = 1;
	int nBins;//discretised gray level
	int delta = 1;//search range
	HashMap<Integer, int[]> angles = Utils.buildAngles();
	ArrayList<Integer> angle_ids;
	
	//basic stats
	double Nvp;
	double Ngp; //number of gray levels, where pi not equal 0.0.
	double eps = Math.ulp(1.0);// 2.220446049250313E-16

	public NGTDMFeatures(ImagePlus img, ImagePlus mask, int label, Integer delta, boolean useBinCount, Integer nBins, Double binWidth) throws Exception {
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
						this.label, img.getCalibration().pixelWidth,img.getCalibration().pixelHeight, img.getCalibration().pixelDepth);
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
				this.delta = RadiomicsJ.deltaNGToneDM;
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
			
			fillNGTDM2();
		}
	}
	
	//slow, use version 2, fillNGTDM2()
	public void fillNGTDM() {
		int w = discImg.getWidth();
		int h = discImg.getHeight();
		int s = discImg.getNSlices();
		ngtdm = new double[nBins][4];//[i, Ni, Pi, Si]
		Nvp = 0d; //sum of neighbor i
		for(int grayLevel=1;grayLevel<=nBins;grayLevel++) {
			double si = 0d; //Neighbourhood grey tone difference
			int ni = 0;// the total number of voxels with grey level
			for(int z=0;z<s;z++) {
				for(int y=0;y<h;y++) {
					for(int x=0;x<w;x++) {
//						discImg.setSlice(z+1);
//						mask.setSlice(z+1);
						float val = discImg.getStack().getProcessor(z+1).getPixelValue(x, y);
						if(Float.isNaN(val)) {
							continue;
						}
						int lbl = (int) mask.getStack().getProcessor(z+1).getPixelValue(x, y);
						if(lbl != this.label) {
							continue;
						}
						if(((int)val) == grayLevel) {
							ArrayList<Point3i> neighbor = connectedNeighbor(x,y,z,w,h,s);
							double blob_sum = 0.0;
							int numOfValidNeighbor = 0;
							for(Point3i p : neighbor) {
//								mask.setSlice(p.z+1);
								lbl = (int) mask.getStack().getProcessor(p.z+1).getPixelValue(p.x, p.y);
								if(lbl != this.label) {
									continue;
								}
//								discImg.setSlice(p.z+1);
								float fv = discImg.getStack().getProcessor(p.z+1).getPixelValue(p.x, p.y);
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
	
	
	public void fillNGTDM2() {
		ngtdm = new double[nBins][4];//[i, Ni, Pi, Si]
		Nvp = 0d; //sum of neighbor i
		for(int grayLevel=1;grayLevel<=nBins;grayLevel++) {
			double si = 0d; //Neighbourhood grey tone difference
			int ni = 0;// the total number of voxels with grey level
			for(int z=0;z<s;z++) {
				float[][] iSlice = discImg.getStack().getProcessor(z+1).getFloatArray();
				float[][] mSlice = mask.getStack().getProcessor(z+1).getFloatArray();
				for(int y=0;y<h;y++) {
					for(int x=0;x<w;x++) {
						float val = iSlice[x][y];
						if(Float.isNaN(val)) {
							continue;
						}
						int lbl = (int) mSlice[x][y];
						if(lbl != this.label) {
							continue;
						}
						if(((int)val) == grayLevel) {
							ArrayList<Point3i> neighbor = connectedNeighbor(x,y,z,w,h,s);
							double blob_sum = 0.0;
							int numOfValidNeighbor = 0;
							for(Point3i p : neighbor) {
								ImageProcessor ip = discImg.getStack().getProcessor(p.z+1);
								ImageProcessor mp = mask.getStack().getProcessor(p.z+1);
								lbl = (int) mp.getf(p.x, p.y);
								if(lbl != this.label) {
									continue;
								}
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
		sb.append("i  Ni  Pi  Si");
		sb.append("\n");
		for (int i=0; i<ngtdm.length;i++) {
				sb.append(ngtdm[i][0] + " " + ngtdm[i][1] + " " + ngtdm[i][2] + " " + ngtdm[i][3]);
				sb.append("\n");
		}
		return sb.toString() ;
	}

}
