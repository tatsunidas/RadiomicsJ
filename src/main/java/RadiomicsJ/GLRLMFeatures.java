package RadiomicsJ;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import javax.swing.JOptionPane;

import org.apache.commons.math3.stat.StatUtils;
import org.scijava.vecmath.Point3i;

import ij.IJ;
import ij.ImagePlus;
import ij.measure.Calibration;

public class GLRLMFeatures {
	
	ImagePlus orgImg;
	ImagePlus discImg;// discretised
	ImagePlus orgMask;
	Calibration orgCal;// backup
	
	int w;
	int h;
	int s;
	Integer[][][] voxels = null;
	
	int label;
	java.util.HashMap<Integer, double[][]> glrlm_raw;// angle_id and glrlm at it angle.
	java.util.HashMap<Integer, double[][]> glrlm;// angle_id and normalized glrlm at it angle.
	boolean normalization = true;// always true;
	int nBins;// 1 to N

	HashMap<Integer, HashMap<String, Object>> coeffs;// angle_od and coefficients of it angle.
	double eps = Math.ulp(1.0);// 2.220446049250313E-16

	String[] weighting_norms = new String[] { "no_weighting", "manhattan", "euclidian", "infinity" };
	String weightingNorm = null;
	
	public GLRLMFeatures(ImagePlus img, 
						 ImagePlus mask, 
						 int label,
						 boolean useBinCount,
						 Integer nBins,
						 Double binWidth,
						 String weightingNorm) throws Exception {
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
						this.label,img.getCalibration().pixelWidth, img.getCalibration().pixelHeight,img.getCalibration().pixelDepth);
			}
			
			if (nBins != null) {
				this.nBins = nBins;
			} else {
				this.nBins = RadiomicsJ.nBins;
			}

			setWeightingNorm(weightingNorm);
			
			orgCal = img.getCalibration().copy();
			this.orgImg = img;
			this.orgMask = mask;
			
			if(RadiomicsJ.discretisedImp != null) {
				discImg = RadiomicsJ.discretisedImp;
			}else {
				if(useBinCount) {
					discImg = Utils.discrete(this.orgImg, this.orgMask, this.label, this.nBins);
				}else {
					/*
					 * do Fixed Bin Width
					 */
					discImg = Utils.discreteByBinWidth(this.orgImg, this.orgMask, this.label, binWidth);
					this.nBins = Utils.getNumOfBinsByMax(discImg, this.orgMask, this.label);
				}
			}
			w = discImg.getWidth();
			h= discImg.getHeight();
			s = discImg.getNSlices();
			voxels = Utils.prepareVoxels(discImg, orgMask, label, nBins);
			
			try{
				fillGLRLM();
			}catch(StackOverflowError e) {
				System.out.println("Stack Overflow occured when executing fillGLRLM().");
				System.out.println("RadiomicsJ: please increase stack memmory size using VM arguments, like example,\n java -Xss=32m -jar RadiomicsJ.jar");
//				JOptionPane.showMessageDialog(null, "RadiomicsJ: please increase stack memmory size using VM arguments, like example,\n java -Xss=32m -jar RadiomicsJ.jar");
				return;
			}
			
		}
	}
	
	
	private void setWeightingNorm(String weightingNorm) {
		if (weightingNorm == null) {
			this.weightingNorm = null;// none weighting (ignore weighting).
			return;
		}
		boolean found = false;
		for (String methodname : weighting_norms) {
			if (weightingNorm.equals(methodname)) {
				found = true;
				this.weightingNorm = weightingNorm;
			}
		}
		if (!found) {
			this.weightingNorm = null;// none weighting (ignore weighting).
		}
	}
	
	
	public void fillGLRLM() throws Exception {
		glrlm_raw = new HashMap<Integer, double[][]>();//angle_id and 
		HashMap<Integer, int[]> angles = Utils.buildAngles();
		ArrayList<Integer> angle_ids = new ArrayList<>(angles.keySet());
		Collections.sort(angle_ids);
		int num_of_angles = angle_ids.size();
		/*
		 * only calculate about 13 angles
		 */
		for (int a = 14; a < num_of_angles; a++) {
			double[][] glrlm_at_a = calcGLRLM(a, angles.get(Integer.valueOf(a)));
			glrlm_raw.put(a, glrlm_at_a);
		}
		normalize(glrlm_raw);
		
	}
	
	
	public double[][] calcGLRLM(final Integer angle_id, final int[] angle) throws Exception {
		//[z][y][x], temp voxels at it angle, for count up.
		Integer[][][] voxels = copyVoxels(this.voxels);
		HashMap<Integer, HashMap<Integer,Integer>> glrlm_map_a = new HashMap<Integer, HashMap<Integer,Integer>>();
		for(int grayLevel=1;grayLevel<=nBins;grayLevel++) {
			//size and count
			HashMap<Integer,Integer> glrlm_row = new HashMap<Integer,Integer>();
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
//							Integer run_length = 0;//init run length
//							run_length = countNeighbor(grayLevel, voxels, run_length, x, y, z, angle);
//							glrlm_row.put(run_length, glrlm_row.get(run_length) == null ? 1:glrlm_row.get(run_length)+1);
							Integer run_length = countNeighbor2(grayLevel, voxels, x, y, z, angle);
							glrlm_row.put(run_length, glrlm_row.get(run_length) == null ? 1:glrlm_row.get(run_length)+1);
						}
					}
				}
			}
			//discrete level is 1 to Ng.
			glrlm_map_a.put(grayLevel, glrlm_row);
		}
		return map2matrix(glrlm_map_a);
	}
	
	
	public HashMap<Integer, double[][]> normalize(java.util.HashMap<Integer, double[][]> glrlm_raw) {
		glrlm = new HashMap<Integer, double[][]>();// final normalized glrlm set of each angles
		ArrayList<Integer> angles = new ArrayList<>(glrlm_raw.keySet());
		Collections.sort(angles);
		for (Integer a : angles) {
			double[][] glrlm_raw_at_a = glrlm_raw.get(a);
			// skip all zero matrix
			if (glrlm_raw_at_a == null) {
				glrlm.put(a, null);
				continue;
			}
			int run_len_max = glrlm_raw_at_a[0].length;
			double[][] norm_glcm_at_a = new double[nBins][run_len_max];
			// init array
			for (int y = 0; y < nBins; y++) {
				for (int x = 0; x < run_len_max; x++) {
					norm_glcm_at_a[y][x] = 0d;
				}
			}

			// do weighting.
			/*
			 * future work... see,
			 * https://pyradiomics.readthedocs.io/en/latest/_modules/radiomics/glcm.html#
			 * RadiomicsGLCM
			 */
//			if(weightingNorm != null) {
//				double weight = 1.0d;
//				double px = orgCal.pixelWidth;
//				double py = orgCal.pixelHeight;
//				double pz = orgCal.pixelHeight;
//				if(weightingNorm.equals("manhattan")) {
//					/*
//					 * ピクセルスペ�?�スを合算してから2�?
//					 * weights[a_idx] = numpy.exp(-numpy.sum(numpy.abs(a) * pixelSpacing) ** 2)
//					 */
//					weight = Math.exp(-(Math.pow((Math.abs(phi) * pixelSpacingX)+(Math.abs(phi) * pixelSpacingY),2)));
//				}else if(weightingNorm.equals("euclidian")) {
//					/*
//					 * ピクセルスペ�?�スそれぞれ�?2乗してから合�?
//					 * weights[a_idx] = numpy.exp(-numpy.sum((numpy.abs(a) * pixelSpacing) ** 2))  # sqrt ^ 2 = 1
//					 */
//					weight = Math.exp(-((Math.pow(Math.abs(phi) * pixelSpacingX,2)+Math.pow(Math.abs(phi) * pixelSpacingY,2))));
//				}else if(weightingNorm.equals("infinity")){
//					/*
//					 * ピクセルスペ�?�スが大きいほ�?のみで計�?
//					 * weights[a_idx] = numpy.exp(-max(numpy.abs(a) * pixelSpacing) ** 2)
//					 */
//					weight = Math.exp(-(Math.pow(Math.abs(phi) * Math.max(pixelSpacingX, pixelSpacingY),2)));
//				}else if(weightingNorm.equals("no_weighting")){
//					weight = 1d;
//				}
//				for (int i=0; i<nBins; i++)  {
//					for (int j=0; j<nBins; j++) {
//						glcm[i][j] = glcm[i][j]*weight;//weighted
//					}
//				}
//			}

			double sum = 0d;
			for (int i = 0; i < nBins; i++) {
				for (int j = 0; j < run_len_max; j++) {
					sum += glrlm_raw_at_a[i][j];
				}
			}
			for (int i = 0; i < nBins; i++) {
				for (int j = 0; j < run_len_max; j++) {
					norm_glcm_at_a[i][j] = glrlm_raw_at_a[i][j] / sum;
				}
			}
			glrlm.put(a, norm_glcm_at_a);
		}

		calculateCoefficients();

		return glrlm;
	}
	
	/*
	 * old implementation
	 * occur stack overflow.
	 */
//	private int countNeighbor(final int grayLevel, Integer[][][] voxels, Integer count, final int seedX, final int seedY, final int seedZ, final int[] angle) {
//		if (voxels[seedZ][seedY][seedX] == null || voxels[seedZ][seedY][seedX] == Integer.MIN_VALUE) {
//			return count;
//		}
//		if(voxels[seedZ][seedY][seedX] == Integer.valueOf(grayLevel)) {
//			count++;
//			voxels[seedZ][seedY][seedX] = Integer.MIN_VALUE;
//		}
//		ArrayList<Point3i> neighbor = connectedNeighbor2(voxels,grayLevel, seedX, seedY, seedZ, w, h, s, angle);
//		if(neighbor == null || neighbor.size() == 0) {
//			return count;
//		}
//		for(Point3i np : neighbor) {
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
//				count = countNeighbor(grayLevel,voxels, count, np.x, np.y, np.z, angle);
//			}
//		}
//		return count;
//	}
	
	private int countNeighbor2(final int grayLevel, Integer[][][] voxels, final int seedX, final int seedY, final int seedZ, final int[] angle) {
		ArrayList<Point3i> runs = new ArrayList<Point3i>();
		if(voxels[seedZ][seedY][seedX] == grayLevel) {
			runs.add(new Point3i(seedX, seedY, seedZ));
			voxels[seedZ][seedY][seedX] = Integer.MIN_VALUE;
		}else {
			return 0;
		}
		Point3i fp = new Point3i();
		Point3i bp = new Point3i();
		int seedX_f = seedX;
		int seedY_f = seedY;
		int seedZ_f = seedZ;
		int seedX_b = seedX;
		int seedY_b = seedY;
		int seedZ_b = seedZ;

		while(fp!=null || bp!=null) {
			if(fp !=null) {
				fp = connectedForword(voxels, grayLevel, seedX_f, seedY_f, seedZ_f, w, h, s, angle);
			}
			if(bp != null) {
				bp = connectedBackword(voxels, grayLevel, seedX_b, seedY_b, seedZ_b, w, h, s, angle);
			}
			if(fp != null) {
				runs.add(fp);
				seedX_f = fp.x;
				seedY_f = fp.y;
				seedZ_f = fp.z;
				voxels[seedZ_f][seedY_f][seedX_f] = Integer.MIN_VALUE;
			}
			if(bp != null) {
				runs.add(bp);
				seedX_b = bp.x;
				seedY_b = bp.y;
				seedZ_b = bp.z;
				voxels[seedZ_b][seedY_b][seedX_b] = Integer.MIN_VALUE;
			}
		};
		
		return runs.size();
	}
	
	
//	private ArrayList<Point3i> connectedNeighbor(final int seedX, final int seedY, final int seedZ, final int max_w , final int max_h, final int max_s, final int[] angle){
//		ArrayList<Point3i> connected = new ArrayList<Point3i>();
//		/*
//		 * forward and backward
//		 */
//		int connected_f_x = seedX + angle[2];//forwarding
//		int connected_f_y = seedY + angle[1];//forwarding
//		int connected_f_z = seedZ + angle[0];//forwarding
//		int connected_b_x = seedX - angle[2];//backward
//		int connected_b_y = seedY - angle[1];//backward
//		int connected_b_z = seedZ - angle[0];//backward
//		if(!Utils.isOutOfRange(new Point3i(connected_f_x, connected_f_y, connected_f_z), max_w, max_h, max_s)) {
//			connected.add(new Point3i(connected_f_x, connected_f_y, connected_f_z));
//		}
//		if(!Utils.isOutOfRange(new Point3i(connected_b_x, connected_b_y, connected_b_z), max_w, max_h, max_s)) {
//			connected.add(new Point3i(connected_b_x, connected_b_y, connected_b_z));
//		}
//		return connected;
//	}
	
//	private ArrayList<Point3i> connectedNeighbor2(Integer[][][] voxels, final int grayLevel, final int seedX, final int seedY, final int seedZ, final int max_w , final int max_h, final int max_s, final int[] angle){
//		ArrayList<Point3i> connected = new ArrayList<Point3i>();
//		/*
//		 * forward and backward
//		 */
//		int connected_f_x = seedX + angle[2];//forwarding
//		int connected_f_y = seedY + angle[1];//forwarding
//		int connected_f_z = seedZ + angle[0];//forwarding
//		int connected_b_x = seedX - angle[2];//backward
//		int connected_b_y = seedY - angle[1];//backward
//		int connected_b_z = seedZ - angle[0];//backward
//		if(!Utils.isOutOfRange(new Point3i(connected_f_x, connected_f_y, connected_f_z), max_w, max_h, max_s)) {
//			if(voxels[connected_f_z][connected_f_y][connected_f_x] != null) {
//				if(voxels[connected_f_z][connected_f_y][connected_f_x] == grayLevel) {
//					connected.add(new Point3i(connected_f_x, connected_f_y, connected_f_z));
//				}
//			}
//		}
//		if(!Utils.isOutOfRange(new Point3i(connected_b_x, connected_b_y, connected_b_z), max_w, max_h, max_s)) {
//			if(voxels[connected_b_z][connected_b_y][connected_b_x] != null) {
//				if(voxels[connected_b_z][connected_b_y][connected_b_x] == grayLevel) {
//					connected.add(new Point3i(connected_b_x, connected_b_y, connected_b_z));
//				}
//			}
//		}
//		return connected.size() > 0 ? connected:null;
//	}
	
	private Point3i connectedForword(Integer[][][] voxels, final int grayLevel, final int seedX, final int seedY, final int seedZ, final int max_w , final int max_h, final int max_s, final int[] angle){
		/*
		 * forward
		 */
		int connected_f_x = seedX + angle[2];//forwarding
		int connected_f_y = seedY + angle[1];//forwarding
		int connected_f_z = seedZ + angle[0];//forwarding
		if(!Utils.isOutOfRange(new Point3i(connected_f_x, connected_f_y, connected_f_z), max_w, max_h, max_s)) {
			if(voxels[connected_f_z][connected_f_y][connected_f_x] != null) {
				if(voxels[connected_f_z][connected_f_y][connected_f_x] == grayLevel) {
					return new Point3i(connected_f_x, connected_f_y, connected_f_z);
				}
			}
		}
		return null;
	}
	
	private Point3i connectedBackword(Integer[][][] voxels, final int grayLevel, final int seedX, final int seedY, final int seedZ, final int max_w , final int max_h, final int max_s, final int[] angle){
		/*
		 * backward
		 */
		int connected_b_x = seedX - angle[2];
		int connected_b_y = seedY - angle[1];
		int connected_b_z = seedZ - angle[0];

		if(!Utils.isOutOfRange(new Point3i(connected_b_x, connected_b_y, connected_b_z), max_w, max_h, max_s)) {
			if(voxels[connected_b_z][connected_b_y][connected_b_x] != null) {
				if(voxels[connected_b_z][connected_b_y][connected_b_x] == grayLevel) {
					return new Point3i(connected_b_x, connected_b_y, connected_b_z);
				}
			}
		}
		return null;
	}
	
	private Integer[][][] copyVoxels(final Integer[][][] v){
		int s = v.length;
		int h = v[0].length;
		int w = v[0][0].length;
		Integer v2[][][] = new Integer[s][h][w];
		for(int z=0;z<s;z++) {
			for(int y=0;y<h;y++) {
				for(int x=0;x<w;x++) {
					if(v[z][y][x] != null) {
						v2[z][y][x] = Integer.valueOf((int)v[z][y][x]); 
					}
				}
			}
		}
		return v2;
	}
	
	
	private double[][] map2matrix(HashMap<Integer, HashMap<Integer,Integer>> glrlm_map){
		int length_max = 0;
		ArrayList<Integer> grayValues = new ArrayList<>(glrlm_map.keySet());
		Collections.sort(grayValues);
		for(Integer gl:grayValues) {
			HashMap<Integer,Integer> length_count_pair = glrlm_map.get(gl);
			for(Integer len:length_count_pair.keySet()) {
				if(len > length_max) {
					length_max = len;
				}
			}
		}
//		glrlm = new double[discreteLevel][length_max];//also ok, here, keep (i,j) shape, 
		double[][] glrlm = new double[nBins][nBins];//(i,j matrices)
		
		for(int row=1;row<=nBins;row++) {
			HashMap<Integer,Integer> length_count_pair = glrlm_map.get(row);
			for(int col=1;col<=nBins;col++) {
				Integer count = length_count_pair.get(col);
				if(count == null) {
					count = 0;
				}
				glrlm[row-1][col-1] = (double)count; 
			}
		}
		
//		if(!weightingNorm.equals("no_weighting")) {
//			double weight = 1.0d;
//			double pixelSpacingX = orgCal.pixelWidth;
//			double pixelSpacingY = orgCal.pixelHeight;
//			if(weightingNorm.equals("manhattan")) {
//				/*
//				 * ピクセルスペ�?�スを合算してから2�?
//				 */
//				weight = Math.exp(-(Math.pow((Math.abs(phi) * pixelSpacingX)+(Math.abs(phi) * pixelSpacingY),2)));
////				weights[a_idx] = numpy.exp(-numpy.sum(numpy.abs(a) * pixelSpacing) ** 2)
//			}else if(weightingNorm.equals("euclidian")) {
//				/*
//				 * ピクセルスペ�?�スそれぞれ�?2乗してから合�?
//				 */
//				weight = Math.exp(-((Math.pow(Math.abs(phi) * pixelSpacingX,2)+Math.pow(Math.abs(phi) * pixelSpacingY,2))));
////				weights[a_idx] = numpy.exp(-numpy.sum((numpy.abs(a) * pixelSpacing) ** 2))  # sqrt ^ 2 = 1
//			}else if(weightingNorm.equals("infinity")){
//				/*
//				 * ピクセルスペ�?�スが大きいほ�?のみで計�?
//				 */
//				weight = Math.exp(-(Math.pow(Math.abs(phi) * Math.max(pixelSpacingX, pixelSpacingY),2)));
////				weights[a_idx] = numpy.exp(-max(numpy.abs(a) * pixelSpacing) ** 2)
//			}
//			//先にGLRLMを計算しておく�?要がある�?
//			for(int row=1;row<=discreteLevel;row++) {
//				HashMap<Integer,Integer> length_count_pair = glrlm_map.get(row);
//				for(int col=1;col<=discreteLevel;col++) {
//					Integer count = length_count_pair.get(col);
//					if(count == null) {
//						count = 0;
//					}
//					glrlm[row-1][col-1] = (double)count;
//				}
//			}
//			//weighting and get sum for norm
//			for (int i=0; i<discreteLevel; i++)  {
//				for (int j=0; j<discreteLevel; j++) {
//					glrlm[i][j] = glrlm[i][j]*weight;//weighted
//					Nr += glrlm[i][j];
//				}
//			}
//			//get normalized matrix
//			for (int i=0; i<discreteLevel; i++)  {
//				for (int j=0; j<discreteLevel; j++) {
//					n_glrlm[i][j] = glrlm[i][j]/Nr;
//				}
//			}
//		}else {
//			//no weighting
//			for(int row=1;row<=discreteLevel;row++) {
//				HashMap<Integer,Integer> length_count_pair = glrlm_map.get(row);
//				for(int col=1;col<=discreteLevel;col++) {
//					Integer count = length_count_pair.get(col);
//					if(count == null) {
//						count = 0;
//					}
//					glrlm[row-1][col-1] = (double)count; 
//					Nr += (double)count;
//				}
//			}
//			for(int row=1;row<=discreteLevel;row++) {
//				for(int col=1;col<=discreteLevel;col++) {
//					n_glrlm[row-1][col-1] = glrlm[row-1][col-1]/Nr;
//				}
//			}
//		}
		return glrlm;
	}
	
	private void calculateCoefficients(){
		if(glrlm == null || glrlm.size() < 1) {
			return;
		}
		coeffs = new HashMap<Integer, HashMap<String,Object>>();
		ArrayList<Integer> angles = new ArrayList<>(glrlm.keySet());
		Collections.sort(angles);
		for(Integer a : angles) {
			double[][] glrlm_a = glrlm.get(a);
			if(glrlm != null) {
				HashMap<String, Object> coeffs_a = new HashMap<String,Object>();
				double mu_i = 0.0;
				double mu_j = 0.0;
				// sum the glrlm mu
				for (int i=0; i<glrlm_a.length; i++){
					for (int j=0; j<glrlm_a[0].length; j++) {
						mu_i += glrlm_a [i][j] * i;
						mu_j += glrlm_a [i][j] * j;
					} 
				}
				coeffs_a.put("mu_i", mu_i);
				coeffs_a.put("mu_j", mu_j);
				coeffs.put(a, coeffs_a);
			}
		}
	}
	
	public Double calculate(String id) {
		String name = GLRLMFeatureType.findType(id);
		if (name.equals(GLRLMFeatureType.ShortRunEmphasis.name())) {
			return getShortRunEmphasis();
		} else if (name.equals(GLRLMFeatureType.LongRunEmphasis.name())) {
			return getLongRunEmphasis();
		} else if (name.equals(GLRLMFeatureType.LowGrayLevelRunEmphasis.name())) {
			return getLowGrayLevelRunEmphasis();
		} else if (name.equals(GLRLMFeatureType.HighGrayLevelRunEmphasis.name())) {
			return getHighGrayLevelRunEmphasis();
		} else if (name.equals(GLRLMFeatureType.ShortRunLowGrayLevelEmphasis.name())) {
			return getShortRunLowGrayLevelEmphasis();
		} else if (name.equals(GLRLMFeatureType.ShortRunHighGrayLevelEmphasis.name())) {
			return getShortRunHighGrayLevelEmphasis();
		} else if (name.equals(GLRLMFeatureType.LongRunLowGrayLevelEmphasis.name())) {
			return getLongRunLowGrayLevelEmphasis();
		} else if (name.equals(GLRLMFeatureType.LongRunHighGrayLevelEmphasis.name())) {
			return getLongRunHighGrayLevelEmphasis();
		} else if (name.equals(GLRLMFeatureType.GrayLevelNonUniformity.name())) {
			return getGrayLevelNonUniformity();
		} else if (name.equals(GLRLMFeatureType.GrayLevelNonUniformityNormalized.name())) {
			return getGrayLevelNonUniformityNormalized();
		} else if (name.equals(GLRLMFeatureType.RunLengthNonUniformity.name())) {
			return getRunLengthNonUniformity();
		} else if (name.equals(GLRLMFeatureType.RunLengthNonUniformityNormalized.name())) {
			return getRunLengthNonUniformityNormalized();
		} else if (name.equals(GLRLMFeatureType.RunPercentage.name())) {
			return getRunPercentage();
		} else if (name.equals(GLRLMFeatureType.GrayLevelVariance.name())) {
			return getGrayLevelVariance();
		} else if (name.equals(GLRLMFeatureType.RunLengthVariance.name())) {
			return getRunLengthVariance();
		} else if (name.equals(GLRLMFeatureType.RunEntropy.name())) {
			return getRunEntropy();
		}
		return null;
	}
	
	private Double getShortRunEmphasis() {
		ArrayList<Integer> angles = new ArrayList<>(glrlm.keySet());
		Collections.sort(angles);
		double[] res_set = new double[angles.size()];
		int itr = 0;
		for (Integer a : angles) {
			if(glrlm.get(a)==null) {
				continue;
			}
			double glrlm_a[][] = glrlm.get(a);
			int Nr_a = glrlm_a[0].length;//length size
			Double sre_a = 0.0d;
//			double Ns_a = 0d;//sum
//			for (int i=1; i<=nBins; i++)  {
//				for (int j=1; j<=Nr_a; j++) {
//					Ns_a += glrlm_a[i-1][j-1];
//				}
//			}
			for (int j=1; j<=Nr_a; j++)  {
				double r_j = 0;
				for (int i=1; i<=nBins; i++) {
					r_j += glrlm_a[i-1][j-1];
				}
				sre_a += r_j/(j*j);
			}
//			sre_a = sre_a/Ns_a;//normalizing is already done
			res_set[itr] = sre_a;
			itr++;
		}
		return StatUtils.mean(res_set);
	}
	
	private Double getLongRunEmphasis() {
		ArrayList<Integer> angles = new ArrayList<>(glrlm.keySet());
		Collections.sort(angles);
		double[] res_set = new double[angles.size()];
		int itr = 0;
		for (Integer a : angles) {
			if(glrlm.get(a)==null) {
				continue;
			}
			double glrlm_a[][] = glrlm.get(a);
			int Nr_a = glrlm_a[0].length;//length size
			Double lre_a = 0.0d;
//			double Ns_a = 0d;//sum
//			for (int i=1; i<=nBins; i++)  {
//				for (int j=1; j<=Nr_a; j++) {
//					Ns_a += glrlm_a[i-1][j-1];
//				}
//			}
			for (int j=1; j<=Nr_a; j++)  {
				double r_j = 0;
				for (int i=1; i<=nBins; i++) {
					r_j += glrlm_a[i-1][j-1];
				}
				lre_a += r_j*(j*j);
			}
//			lre_a = lre_a/Ns_a;//normalizing is already done
			res_set[itr] = lre_a;
			itr++;
		}
		return StatUtils.mean(res_set);
	}
	
	
	private Double getLowGrayLevelRunEmphasis() {
		ArrayList<Integer> angles = new ArrayList<>(glrlm.keySet());
		Collections.sort(angles);
		double[] res_set = new double[angles.size()];
		int itr = 0;
		for (Integer a : angles) {
			if(glrlm.get(a)==null) {
				continue;
			}
			double glrlm_a[][] = glrlm.get(a);
			int Nr_a = glrlm_a[0].length;//length size
			Double lgre_a = 0.0d;
//			double Ns_a = 0d;//sum
//			for (int i=1; i<=nBins; i++)  {
//				for (int j=1; j<=Nr_a; j++) {
//					Ns_a += glrlm_a[i-1][j-1];
//				}
//			}
			for (int i=1; i<=nBins; i++)  {
				double r_i = 0;
				for (int j=1; j<=Nr_a; j++) {
					r_i += glrlm_a[i-1][j-1];
				}
				lgre_a += r_i/(i*i);
			}
//			lgre_a = lgre_a/Ns_a;//normalizing is already done
			res_set[itr] = lgre_a;
			itr++;
		}
		return StatUtils.mean(res_set);
	}
	
	
	private Double getHighGrayLevelRunEmphasis() {
		ArrayList<Integer> angles = new ArrayList<>(glrlm.keySet());
		Collections.sort(angles);
		double[] res_set = new double[angles.size()];
		int itr = 0;
		for (Integer a : angles) {
			if(glrlm.get(a)==null) {
				continue;
			}
			double glrlm_a[][] = glrlm.get(a);
			int Nr_a = glrlm_a[0].length;//length size
			Double lgre_a = 0.0d;
//			double Ns_a = 0d;//sum
//			for (int i=1; i<=nBins; i++)  {
//				for (int j=1; j<=Nr_a; j++) {
//					Ns_a += glrlm_a[i-1][j-1];
//				}
//			}
			for (int i=1; i<=nBins; i++)  {
				double r_i = 0;
				for (int j=1; j<=Nr_a; j++) {
					r_i += glrlm_a[i-1][j-1];
				}
				lgre_a += r_i*(i*i);
			}
//			lgre_a = lgre_a/Ns_a;//normalizing is already done
			res_set[itr] = lgre_a;
			itr++;
		}
		return StatUtils.mean(res_set);
	}
	
	
	private Double getShortRunLowGrayLevelEmphasis() {
		ArrayList<Integer> angles = new ArrayList<>(glrlm.keySet());
		Collections.sort(angles);
		double[] res_set = new double[angles.size()];
		int itr = 0;
		for (Integer a : angles) {
			if(glrlm.get(a)==null) {
				continue;
			}
			double glrlm_a[][] = glrlm.get(a);
			int Nr_a = glrlm_a[0].length;//length size
			Double srlge_a = 0.0d;
//			double Ns_a = 0d;//sum
//			for (int i=1; i<=nBins; i++)  {
//				for (int j=1; j<=Nr_a; j++) {
//					Ns_a += glrlm_a[i-1][j-1];
//				}
//			}
			for (int i=1; i<=nBins; i++)  {
				for (int j=1; j<=Nr_a; j++) {
					srlge_a += glrlm_a[i-1][j-1]/(i*i*j*j);
				}
			}
//			srlge_a = srlge_a/Ns_a;//normalizing is already done
			res_set[itr] = srlge_a;
			itr++;
		}
		return StatUtils.mean(res_set);
	}
	
	private Double getShortRunHighGrayLevelEmphasis() {
		ArrayList<Integer> angles = new ArrayList<>(glrlm.keySet());
		Collections.sort(angles);
		double[] res_set = new double[angles.size()];
		int itr = 0;
		for (Integer a : angles) {
			if(glrlm.get(a)==null) {
				continue;
			}
			double glrlm_a[][] = glrlm.get(a);
			int Nr_a = glrlm_a[0].length;//length size
			Double srhge_a = 0.0d;
			double Ns_a = 0d;//sum
			for (int i=1; i<=nBins; i++)  {
				for (int j=1; j<=Nr_a; j++) {
					Ns_a += glrlm_a[i-1][j-1];
				}
			}
			for (int i=1; i<=nBins; i++)  {
				for (int j=1; j<=Nr_a; j++) {
					srhge_a += (glrlm_a[i-1][j-1]*(i*i))/(j*j);
				}
			}
			srhge_a = srhge_a/Ns_a;
			res_set[itr] = srhge_a;
			itr++;
		}
		return StatUtils.mean(res_set);
	}
	
	private Double getLongRunLowGrayLevelEmphasis() {
		ArrayList<Integer> angles = new ArrayList<>(glrlm.keySet());
		Collections.sort(angles);
		double[] res_set = new double[angles.size()];
		int itr = 0;
		for (Integer a : angles) {
			if(glrlm.get(a)==null) {
				continue;
			}
			double glrlm_a[][] = glrlm.get(a);
			int Nr_a = glrlm_a[0].length;//length size
			Double lrlge_a = 0.0d;
//			double Ns_a = 0d;//sum
//			for (int i=1; i<=nBins; i++)  {
//				for (int j=1; j<=Nr_a; j++) {
//					Ns_a += glrlm_a[i-1][j-1];
//				}
//			}
			for (int i=1; i<=nBins; i++)  {
				for (int j=1; j<=Nr_a; j++) {
					lrlge_a += (glrlm_a[i-1][j-1]*(j*j))/(i*i);
				}
			}
//			lrlge_a = lrlge_a/Ns_a;//normalizing is already done
			res_set[itr] = lrlge_a;
			itr++;
		}
		return StatUtils.mean(res_set);
	}
	
	private Double getLongRunHighGrayLevelEmphasis() {
		ArrayList<Integer> angles = new ArrayList<>(glrlm.keySet());
		Collections.sort(angles);
		double[] res_set = new double[angles.size()];
		int itr = 0;
		for (Integer a : angles) {
			if(glrlm.get(a)==null) {
				continue;
			}
			double glrlm_a[][] = glrlm.get(a);
			int Nr_a = glrlm_a[0].length;//length size
			Double lrhge_a = 0.0d;
//			double Ns_a = 0d;//sum
//			for (int i=1; i<=nBins; i++)  {
//				for (int j=1; j<=Nr_a; j++) {
//					Ns_a += glrlm_a[i-1][j-1];
//				}
//			}
			for (int i=1; i<=nBins; i++)  {
				for (int j=1; j<=Nr_a; j++) {
					lrhge_a += glrlm_a[i-1][j-1]*(i*i*j*j);
				}
			}
//			lrhge_a = lrhge_a/Ns_a;//normalizing is already done
			res_set[itr] = lrhge_a;
			itr++;
		}
		return StatUtils.mean(res_set);
	}
	
	private Double getGrayLevelNonUniformity() {
		ArrayList<Integer> angles = new ArrayList<>(glrlm.keySet());
		Collections.sort(angles);
		double[] res_set = new double[angles.size()];
		int itr = 0;
		for (Integer a : angles) {
			if(glrlm.get(a)==null) {
				continue;
			}
			double glrlm_a[][] = glrlm_raw.get(a);//IMPORTANT, no-normalized
			int Nr_a = glrlm_a[0].length;//length size
			Double gnu_a = 0.0d;
			double Ns_a = 0d;//sum
			for (int i=1; i<=nBins; i++)  {
				for (int j=1; j<=Nr_a; j++) {
					Ns_a += glrlm_a[i-1][j-1];
				}
			}
			for (int i=1; i<=nBins; i++)  {
				double r_i = 0;
				for (int j=1; j<=Nr_a; j++) {
					r_i += glrlm_a[i-1][j-1];
				}
				gnu_a += r_i*r_i;
			}
			gnu_a = gnu_a/Ns_a;
			res_set[itr] = gnu_a;
			itr++;
		}
		return StatUtils.mean(res_set);
	}
	
	private Double getGrayLevelNonUniformityNormalized() {
		ArrayList<Integer> angles = new ArrayList<>(glrlm.keySet());
		Collections.sort(angles);
		double[] res_set = new double[angles.size()];
		int itr = 0;
		for (Integer a : angles) {
			if(glrlm.get(a)==null) {
				continue;
			}
			double glrlm_a[][] = glrlm.get(a);
			int Nr_a = glrlm_a[0].length;//length size
			Double gnu_a = 0.0d;
//			double Ns_a = 0d;//sum
//			for (int i=1; i<=nBins; i++)  {
//				for (int j=1; j<=Nr_a; j++) {
//					Ns_a += glrlm_a[i-1][j-1];
//				}
//			}
			for (int i=1; i<=nBins; i++)  {
				double r_i = 0;
				for (int j=1; j<=Nr_a; j++) {
					r_i += glrlm_a[i-1][j-1];
				}
				gnu_a += r_i*r_i;
			}
//			gnu_a = gnu_a/(Ns_a*Ns_a);//normalizing is already done
			res_set[itr] = gnu_a;
			itr++;
		}
		return StatUtils.mean(res_set);
	}
	
	private Double getRunLengthNonUniformity() {
		ArrayList<Integer> angles = new ArrayList<>(glrlm.keySet());
		Collections.sort(angles);
		double[] res_set = new double[angles.size()];
		int itr = 0;
		for (Integer a : angles) {
			if(glrlm.get(a)==null) {
				continue;
			}
			double glrlm_a[][] = glrlm_raw.get(a);//IMPORTANT, no-normalized
			int Nr_a = glrlm_a[0].length;//length size
			Double rlnu_a = 0.0d;
			double Ns_a = 0d;//sum
			for (int i=1; i<=nBins; i++)  {
				for (int j=1; j<=Nr_a; j++) {
					Ns_a += glrlm_a[i-1][j-1];
				}
			}
			for (int j=1; j<=Nr_a; j++)  {
				double r_j = 0;
				for (int i=1; i<=nBins; i++) {
					r_j += glrlm_a[i-1][j-1];
				}
				rlnu_a += r_j*r_j;
			}
			rlnu_a = rlnu_a/Ns_a;
			res_set[itr] = rlnu_a;
			itr++;
		}
		return StatUtils.mean(res_set);
	}
	
	private Double getRunLengthNonUniformityNormalized() {
		ArrayList<Integer> angles = new ArrayList<>(glrlm.keySet());
		Collections.sort(angles);
		double[] res_set = new double[angles.size()];
		int itr = 0;
		for (Integer a : angles) {
			double glrlm_a[][] = glrlm.get(a);
			int Nr_a = glrlm_a[0].length;//length size
			Double rlnu_a = 0.0d;
//			double Ns_a = 0d;//sum
//			for (int i=1; i<=nBins; i++)  {
//				for (int j=1; j<=Nr_a; j++) {
//					Ns_a += glrlm_a[i-1][j-1];
//				}
//			}
			for (int j=1; j<=Nr_a; j++)  {
				double r_j = 0;
				for (int i=1; i<=nBins; i++) {
					r_j += glrlm_a[i-1][j-1];
				}
				rlnu_a += r_j*r_j;
			}
//			rlnu_a = rlnu_a/(Ns_a*Ns_a);//normalizing is already done
			res_set[itr] = rlnu_a;
			itr++;
		}
		return StatUtils.mean(res_set);
	}
	
	private Double getRunPercentage() {
		ArrayList<Integer> angles = new ArrayList<>(glrlm.keySet());
		Collections.sort(angles);
		double[] res_set = new double[angles.size()];
		int itr = 0;
		double voxels[] = Utils.getVoxels(discImg, orgMask, this.label);
		for (Integer a : angles) {
			double glrlm_a[][] = glrlm_raw.get(a);//IMPORTANT, no-normalized
			int Nr_a = glrlm_a[0].length;//length size
			Double rp_a = 0.0d;
			double Ns_a = 0d;//sum
			for (int i=1; i<=nBins; i++)  {
				for (int j=1; j<=Nr_a; j++) {
					Ns_a += glrlm_a[i-1][j-1];
				}
			}
			rp_a = Ns_a/voxels.length;
			res_set[itr] = rp_a;
			itr++;
		}
		return StatUtils.mean(res_set);
	}
	
	private Double getGrayLevelVariance() {
		ArrayList<Integer> angles = new ArrayList<>(glrlm.keySet());
		Collections.sort(angles);
		double[] res_set = new double[angles.size()];
		int itr = 0;
		for (Integer a : angles) {
			if(glrlm.get(a)==null) {
				continue;
			}
			double glrlm_a[][] = glrlm.get(a);
			int Nr_a = glrlm_a[0].length;//length size
			Double glv_a = 0.0d;
//			double Ns_a = 0d;//sum
//			for (int i=1; i<=nBins; i++)  {
//				for (int j=1; j<=Nr_a; j++) {
//					Ns_a += glrlm_a[i-1][j-1];
//				}
//			}
			double myu_i = 0d;
			for (int i=1; i<=nBins; i++)  {
				for (int j=1; j<=Nr_a; j++) {
//					myu_i += i * (glrlm_a[i-1][j-1]/Ns_a);
					myu_i += i * (glrlm_a[i-1][j-1]);//normalizing is already done.
				}
			}
			for (int i=1; i<=nBins; i++)  {
				for (int j=1; j<=Nr_a; j++) {
//					glv_a += (i - myu_i)*(i-myu_i)*(glrlm_a[i-1][j-1]/Ns_a);
					glv_a += (i - myu_i)*(i-myu_i)*(glrlm_a[i-1][j-1]);//normalizing is already done.
				}
			}
			res_set[itr] = glv_a;
			itr++;
		}
		return StatUtils.mean(res_set);
	}
	
	private Double getRunLengthVariance() {
		ArrayList<Integer> angles = new ArrayList<>(glrlm.keySet());
		Collections.sort(angles);
		double[] res_set = new double[angles.size()];
		int itr = 0;
		for (Integer a : angles) {
			if(glrlm.get(a)==null) {
				continue;
			}
			double glrlm_a[][] = glrlm.get(a);
			int Nr_a = glrlm_a[0].length;//length size
			Double rlv_a = 0.0d;
//			double Ns_a = 0d;//sum
//			for (int i=1; i<=nBins; i++)  {
//				for (int j=1; j<=Nr_a; j++) {
//					Ns_a += glrlm_a[i-1][j-1];
//				}
//			}
			double myu_j = 0d;
			for (int i=1; i<=nBins; i++)  {
				for (int j=1; j<=Nr_a; j++) {
//					myu_j += j * (glrlm_a[i-1][j-1]/Ns_a);
					myu_j += j * (glrlm_a[i-1][j-1]);//normalizing is already done.
				}
			}
			for (int i=1; i<=nBins; i++)  {
				for (int j=1; j<=Nr_a; j++) {
//					rlv_a += (j - myu_j)*(j-myu_j)*(glrlm_a[i-1][j-1]/Ns_a);
					rlv_a += (j - myu_j)*(j-myu_j)*(glrlm_a[i-1][j-1]);//normalizing is already done.
				}
			}
			res_set[itr] = rlv_a;
			itr++;
		}
		return StatUtils.mean(res_set);
	}
	
	private Double getRunEntropy() {
		ArrayList<Integer> angles = new ArrayList<>(glrlm.keySet());
		Collections.sort(angles);
		double[] res_set = new double[angles.size()];
		int itr = 0;
		for (Integer a : angles) {
			if(glrlm.get(a)==null) {
				continue;
			}
			double glrlm_a[][] = glrlm.get(a);
			int Nr_a = glrlm_a[0].length;//length size
//			double Ns_a = 0d;//sum
//			for (int i=1; i<=nBins; i++)  {
//				for (int j=1; j<=Nr_a; j++) {
//					Ns_a += glrlm_a[i-1][j-1];
//				}
//			}
			double ent_a = 0.0d;
			for (int i=1; i<=nBins; i++)  {
				for (int j=1; j<=Nr_a; j++) {
//					double p_ij = glrlm_a[i-1][j-1]/(Ns_a+eps);//normalizing is already done.
					double p_ij = glrlm_a[i-1][j-1] == 0d ? glrlm_a[i-1][j-1]+eps : glrlm_a[i-1][j-1];
					ent_a -= p_ij * (Math.log(p_ij)/Math.log(2d));
				}
			}
			res_set[itr] = ent_a;
			itr++;
		}
		return StatUtils.mean(res_set);
	}


//	public String toString(){
//		StringBuffer sb = new StringBuffer() ;
//		for (int j=0; j<glrlm.length;j++) {
//			for (int i=0; i<glrlm[0].length;i++) {
//				sb.append(glrlm[j][i] + " ");
//			}
//			sb.append("\n");
//		}
//		return sb.toString() ;
//	}
	
	public double[][] getMatrix(int[] angle) {
		if (glrlm == null || glrlm.size() < 1) {
			return null;
		}
		HashMap<Integer, int[]> angles = Utils.buildAngles();
		ArrayList<Integer> angles_key = new ArrayList<>(glrlm.keySet());
		for (Integer a_id : angles_key) {
			int[] ang = angles.get(a_id);
			if (ang[0] == angle[0] && ang[1] == angle[1] && ang[2] == angle[2]) {
				return glrlm.get(a_id);
			}
		}
		return null;
	}
	
	@SuppressWarnings("unused")
	private void checkCoefficients(int angle_id, String key) {
		if (coeffs != null && coeffs.size() > 0) {
			HashMap<String, Object> coeff_a = coeffs.get(angle_id);
			Object obj = coeff_a.get(key);
			if (obj != null) {
				if (obj instanceof Double) {
					System.out.println(key + " is " + (double) obj);
				} else if (obj instanceof double[]) {
					System.out.println(key + " is ");
					double[] o = (double[]) obj;
					for (int i = 0; i < o.length; i++) {
						System.out.println(o[i]);
					}
				}
			}
		}
	}

	public String toString(double[][] mat) {
		String res = "";
		for (int row = 0; row < mat.length; row++) {
			StringBuilder sb = new StringBuilder();
			for (int col = 0; col < mat[0].length; col++) {
				sb.append(IJ.d2s(mat[row][col], 2));
				sb.append(" ");
			}
			sb.append("\n");
			res = res + sb.toString();
		}
		return res;
	}

}
