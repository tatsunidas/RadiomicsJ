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

import org.apache.commons.math3.stat.StatUtils;

import ij.IJ;
import ij.ImagePlus;
import ij.measure.Calibration;
import io.github.tatsunidas.radiomics.main.ImagePreprocessing;
import io.github.tatsunidas.radiomics.main.RadiomicsJ;
import io.github.tatsunidas.radiomics.main.Utils;

/**
 * 3.6.1 Joint maximum GYBY 3.6.2 Joint average 60VM 3.6.3 Joint variance UR99
 * 3.6.4 Joint entropy TU9B 3.6.5 Difference average TF7R 3.6.6 Difference
 * variance D3YU 3.6.7 Difference entropy NTRS 3.6.8 Sum average ZGXS 3.6.9 Sum
 * variance OEEB 3.6.10 Sum entropy P6QZ 3.6.11 Angular second moment 8ZQL
 * 3.6.12 Contrast ACUI 3.6.13 Dissimilarity 8S9J 3.6.14 Inverse difference IB1Z
 * 3.6.15 Normalised inverse difference NDRX 3.6.16 Inverse difference moment
 * WF0Z 3.6.17 Normalised inverse difference moment 1QCO 3.6.18 Inverse variance
 * E8JP 3.6.19 Correlation NI2N 3.6.20 Autocorrelation QWB0 3.6.21 Cluster
 * tendency DG8W 3.6.22 Cluster shade 7NFM 3.6.23 Cluster prominence AE86 3.6.24
 * Information correlation 1 R8DG 3.6.25 Information correlation 2 JN9H
 * 
 * @author tatsunidas
 *
 */
public class GLCMFeatures extends AbstractRadiomicsFeature implements Texture{

	ImagePlus discImg;// discretized
	Calibration orgCal;// backup
	
	int w;
	int h;
	int s;
	
	//axis aligned bb
	HashMap<String, double[]> aabb;
	
	final int label;
	java.util.HashMap<Integer, double[][]> glcm_raw;// angle_id and glcm at it angle.
	java.util.HashMap<Integer, double[][]> glcm;// angle_id and normalized glcm at it angle.
	boolean symmetry = true;// always true;
	boolean normalization = true;// always true;
	int nBins;// 1 to N
	double binWidth;
	int delta = 1;
	
	HashMap<Integer, HashMap<String, Object>> coeffs;// angle_od and coefficients of it angle.
	public static final String Px = "Px";
	public static final String Py = "Py";
	double eps = Math.ulp(1.0);// 2.220446049250313E-16

	final String[] weighting_methods = new String[] { "no_weighting", "manhattan", "euclidian", "infinity" };
	private String weightingMethod = null;


	public GLCMFeatures(ImagePlus img, ImagePlus mask, Map<String,Object> settings) {
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
	 * delta: distance between i and j. 1 is default.\n angle: if 2d, 0, 45, 90,
	 * 135, \n else if 3d, 13 angles(in symmetrical).
	 * weightingNorm:"manhattan""euclidian""infinity""no_weighting", no weighting is
	 * default.
	 * 
	 * @throws Exception
	 */
	public GLCMFeatures(ImagePlus img, ImagePlus mask, int label, Integer delta, boolean useBinCount, Integer nBins,
			Double binWidth, String weightingNorm) throws Exception {
		super(img, mask, null);
		//first, define label.
		this.label = label;
		
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
		
		this.mask = mask;
		this.orgCal = this.img.getCalibration();
		
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
		
		if (delta != null && delta > 0) {
			this.delta = delta;
		}else {
			this.delta = RadiomicsJ.deltaGLCM;
		}

		//todo
		setWeightingNorm(weightingNorm);
		
		// discretised by roi mask.
		if(RadiomicsJ.discretiseImp != null) {
			discImg = RadiomicsJ.discretiseImp;
		}else {
			if(useBinCount) {
				discImg = Utils.discrete(this.img, this.mask, this.label, this.nBins);
			}else {
				/*
				 * Bin Width
				 */
				discImg = Utils.discreteByBinWidth(this.img, this.mask, this.label, this.binWidth);
				this.nBins = Utils.getNumOfBinsByMax(discImg, this.mask, this.label);
			}
		}
		w = this.img.getWidth();
		h = this.img.getHeight();
		s = this.img.getNSlices();
		aabb = Utils.getRoiBoundingBoxInfo(this.mask, this.label, RadiomicsJ.debug);
		
		calcGLCM();
		
		settings.put(RadiomicsFeature.IMAGE, this.img);
		settings.put(RadiomicsFeature.MASK, this.mask);
		settings.put(RadiomicsFeature.DISC_IMG, this.discImg);
		settings.put(RadiomicsFeature.LABEL, this.label);
		settings.put(RadiomicsFeature.DELTA, this.delta);
		settings.put(RadiomicsFeature.USE_BIN_COUNT, useBinCount);
		settings.put(RadiomicsFeature.nBins, this.nBins);
		settings.put(RadiomicsFeature.BinWidth, this.binWidth);
		settings.put(RadiomicsFeature.WEIGHTING_NORM, weightingNorm);
	}
	
	@Override
	public void buildup(Map<String, Object> settings) {
				
		Object deltaValue = settings.get(RadiomicsFeature.DELTA);
		if (deltaValue != null && !(deltaValue instanceof Integer)) {
			throw new IllegalArgumentException("'delta' must be an Integer.");
		}
		if (deltaValue == null) {
			this.delta = RadiomicsJ.deltaGLCM;
		}else {
			this.delta = (Integer)deltaValue;
		}
		
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
		
		Object norm = settings.get(RadiomicsFeature.WEIGHTING_NORM);
		if(norm != null && (norm instanceof String)) {
			setWeightingNorm((String)norm);
		}
		
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
		aabb = Utils.getRoiBoundingBoxInfo(this.mask, this.label, RadiomicsJ.debug);

		calcGLCM();
	}

	public Double calculate(String id) {
		String name = GLCMFeatureType.findType(id);
		if (name.equals(GLCMFeatureType.JointMaximum.name())) {
			return getJointMaximum();
		} else if (name.equals(GLCMFeatureType.JointAverage.name())) {
			return getJointAverage();
		} else if (name.equals(GLCMFeatureType.JointVariance.name())) {
			return getJointVariance();
		} else if (name.equals(GLCMFeatureType.JointEntropy.name())) {
			return getJointEntropy();
		} else if (name.equals(GLCMFeatureType.DifferenceAverage.name())) {
			return getDifferenceAverage();
		} else if (name.equals(GLCMFeatureType.DifferenceVariance.name())) {
			return getDifferenceVariance();
		} else if (name.equals(GLCMFeatureType.DifferenceEntropy.name())) {
			return getDifferenceEntropy();
		} else if (name.equals(GLCMFeatureType.SumAverage.name())) {
			return getSumAverage();
		} else if (name.equals(GLCMFeatureType.SumVariance.name())) {
			return getSumVariance();
		} else if (name.equals(GLCMFeatureType.SumEntropy.name())) {
			return getSumEntropy();
		} else if (name.equals(GLCMFeatureType.AngularSecondMoment.name())) {
			return getAngular2ndMoment();
		} else if (name.equals(GLCMFeatureType.Contrast.name())) {
			return getContrast();
		} else if (name.equals(GLCMFeatureType.Dissimilarity.name())) {
			return getDissimilarity();
		} else if (name.equals(GLCMFeatureType.InverseDifference.name())) {
			return getInverseDifference();
		} else if (name.equals(GLCMFeatureType.NormalizedInverseDifference.name())) {
			return getNormalisedInverseDifference();
		} else if (name.equals(GLCMFeatureType.InverseDifferenceMoment.name())) {
			return getInverseDifferenceMoment();
		} else if (name.equals(GLCMFeatureType.NormalizedInverseDifferenceMoment.name())) {
			return getNormalisedInverseDifferenceMoment();
		} else if (name.equals(GLCMFeatureType.InverseVariance.name())) {
			return getInverseVariance();
		} else if (name.equals(GLCMFeatureType.Correlation.name())) {
			return getCorrelation();
		} else if (name.equals(GLCMFeatureType.Autocorrection.name())) {
			return getAutocorrection();
		} else if (name.equals(GLCMFeatureType.ClusterTendency.name())) {
			return getClusterTendency();
		} else if (name.equals(GLCMFeatureType.ClusterShade.name())) {
			return getClusterShade();
		} else if (name.equals(GLCMFeatureType.ClusterProminence.name())) {
			return getClusterProminence();
		} else if (name.equals(GLCMFeatureType.InformationalMeasureOfCorrelation1.name())) {
			return getInformationalMeasureOfCorrelation1();
		} else if (name.equals(GLCMFeatureType.InformationalMeasureOfCorrelation2.name())) {
			return getInformationalMeasureOfCorrelation2();
		}
		return null;
	}

	private void setWeightingNorm(String weightingMethod) {
		if (weightingMethod == null) {
			this.weightingMethod = null;// none weighting (ignore weighting).
			return;
		}
		for (String methodname : weighting_methods) {
			if (weightingMethod.equals(methodname)) {
				this.weightingMethod = methodname;
				return;
			}
		}
		this.weightingMethod = null;// no weighting (ignore weighting).
	}

	/**
	 * calculate glcm on 2d/3d (in 26 components, symmetrical.).
	 * 
	 * @return
	 */
	public void calcGLCM() {
		glcm_raw = new java.util.HashMap<Integer, double[][]>();// phi_id and it glcm
		HashMap<Integer, int[]> angles = Utils.buildAngles();
		ArrayList<Integer> angle_ids = new ArrayList<>(angles.keySet());
		Collections.sort(angle_ids);
		int num_of_angles = angle_ids.size();
		if (symmetry) {
			/*
			 * only calculate about 13 angles (symmetrical )
			 */
			for (int a = 14; a < num_of_angles; a++) {
				double[][] glcm_at_a = calcGLCM2(a, angles.get(Integer.valueOf(a)), this.delta);
				//GLCM matrices are weighted by weighting factor W and then normalized.
				glcm_at_a = weighting(angles.get(Integer.valueOf(a)), glcm_at_a);
				glcm_raw.put(a, glcm_at_a);
			}
		} else {
			for (int a = 0; a < num_of_angles; a++) {
				if (a == 13) {// skip own angle int[]{0,0,0}
					continue;
				}
				double[][] glcm_at_a = calcGLCM2(a, angles.get(Integer.valueOf(a)), this.delta);
				//GLCM matrices are weighted by weighting factor W and then normalized.
				glcm_at_a = weighting(angles.get(Integer.valueOf(a)), glcm_at_a);
				glcm_raw.put(a, glcm_at_a);
			}
		}
		normalize(glcm_raw);
	}

	/**
	 * first implemented methods.
	 * @param angleID
	 * @param angle
	 * @param delta
	 * @return
	 */
	@Deprecated
	public double[][] calcGLCM(int angleID, int[] angle, int delta) {

		ImagePlus img = discImg;

		if (glcm_raw == null) {
			glcm_raw = new java.util.HashMap<Integer, double[][]>();
		}
		if (delta < 1) {
			delta = 1;
		}

		double[][] glcm_at_angle = new double[nBins][nBins];
		// init by padding zero.
		for (int y = 0; y < nBins; y++) {
			for (int x = 0; x < nBins; x++) {
				glcm_at_angle[y][x] = 0d;
			}
		}
		int w = img.getWidth();
		int h = img.getHeight();
		int s = img.getNSlices();

		int offsetX = angle[2] * delta;
		int offsetY = angle[1] * delta * -1;//adjust vector direction and coordinate direction in Y axis.
		int offsetZ = angle[0] * delta;

		for (int z = 0; z < s; z++) {
			for (int y = 0; y < h; y++) {
				for (int x = 0; x < w; x++) {
					int dx = x + offsetX;
					int dy = y + offsetY;
					int dz = z + offsetZ;
					if ((dx >= 0 && dx < w) && (dy >= 0 && dy < h) && (dz >= 0 && dz < s)) {
						int lbli = (int) mask.getStack().getProcessor(z+1).getPixelValue(x, y);
						if (lbli == this.label) {
							int vi = (int) img.getStack().getProcessor(z+1).getPixelValue(x, y);
							int lblj = (int) mask.getStack().getProcessor(dz+1).getPixelValue(dx, dy);
							if (lblj == this.label) {
								int vj = (int) img.getStack().getProcessor(dz+1).getPixelValue(dx, dy);
								// discretised pixels is 1 to nBins
								glcm_at_angle[vi - 1][vj - 1]++;
								if (symmetry) {
									glcm_at_angle[vj - 1][vi - 1]++;
								}
							}
						}
					}
				}
			}
		}
		// null validate
		/*
		 * if glcm matrix (at specified angle) values are all zero, return null.
		 */
		boolean return_null = true;
		for (int y = 0; y < nBins; y++) {
			for (int x = 0; x < nBins; x++) {
				if (glcm_at_angle[y][x] > 0d) {
					return_null = false;
				}
			}
		}
		if (return_null) {
			return null;
		}
		return glcm_at_angle;
	}
	
	/*
	 * faster
	 */
	public double[][] calcGLCM2(int angleID, int[] angle, int delta) {

		ImagePlus img = discImg;

		if (glcm_raw == null) {
			glcm_raw = new java.util.HashMap<Integer, double[][]>();
		}
		if (delta < 1) {
			delta = 1;
		}

		double[][] glcm_at_angle = new double[nBins][nBins];
		// init
		for (int y = 0; y < nBins; y++) {
			for (int x = 0; x < nBins; x++) {
				glcm_at_angle[y][x] = 0d;
			}
		}
		
		int offsetX = angle[2] * delta;
		int offsetY = angle[1] * delta * -1;//adjust vector direction and coordinate direction in Y axis.
		int offsetZ = angle[0] * delta;
		
		if(this.aabb == null) {
			aabb = Utils.getRoiBoundingBoxInfo(mask, this.label, RadiomicsJ.debug);
		}
		int xMin = (int) aabb.get("x")[0];
		int xMax = (int) aabb.get("x")[1];
		int yMin = (int) aabb.get("y")[0];
		int yMax = (int) aabb.get("y")[1];
		int zMin = (int) aabb.get("z")[0];
		int zMax = (int) aabb.get("z")[1];

		for (int z=zMin; z <= zMax; z++) {
			//reference slice
			float[][] mSlice_i = mask.getStack().getProcessor(z+1).getFloatArray();
			float[][] iSlice_i = img.getStack().getProcessor(z+1).getFloatArray();
			//searching slice
			int dz = z + offsetZ;
			float[][] mSlice_j = null;
			float[][] iSlice_j = null;
			if(dz >= 0 && dz < s) {
				mSlice_j = mask.getStack().getProcessor(dz+1).getFloatArray();
				iSlice_j = img.getStack().getProcessor(dz+1).getFloatArray();
			}
			for (int y = yMin; y <= yMax; y++) {
				int dy = y + offsetY;
				for (int x = xMin; x <= xMax; x++) {
					int dx = x + offsetX;
					if ((dx >= 0 && dx < w) && (dy >= 0 && dy < h) && (dz >= 0 && dz < s)) {
						int lbli = (int) mSlice_i[x][y];
						if (lbli == this.label) {
							/*
							 * int value, because pixels were discretised.
							 */
							int vi = (int) iSlice_i[x][y];
							int lblj = (int) mSlice_j[dx][dy];
							if (lblj == this.label) {
								int vj = (int) iSlice_j[dx][dy];
								// discretized pixels is 1 to nBins
								glcm_at_angle[vi - 1][vj - 1]++;
								if (symmetry) {
									glcm_at_angle[vj - 1][vi - 1]++;
								}
							}
						}
					}
				}
			}
		}
		// null validate
		/*
		 * if glcm matrix (at specified angle) values are all zero, return null.
		 */
		boolean return_null = true;
		for (int y = 0; y < nBins; y++) {
			for (int x = 0; x < nBins; x++) {
				if (glcm_at_angle[y][x] > 0d) {
					return_null = false;
				}
			}
		}
		if (return_null) {
			return null;
		}
		return glcm_at_angle;
	}
	
	public double[][] normalize(double[][] glcm_raw){
		double[][] norm_glcm = new double[nBins][nBins];
		// init array
		for (int y = 0; y < nBins; y++) {
			for (int x = 0; x < nBins; x++) {
				norm_glcm[y][x] = 0d;
			}
		}
		double sum = 0d;
		for (int i = 0; i < nBins; i++) {
			for (int j = 0; j < nBins; j++) {
				sum += glcm_raw[i][j];
			}
		}
		for (int i = 0; i < nBins; i++) {
			for (int j = 0; j < nBins; j++) {
				norm_glcm[i][j] = glcm_raw[i][j] / sum;
			}
		}
		return norm_glcm;
	}

	public HashMap<Integer, double[][]> normalize(java.util.HashMap<Integer, double[][]> glcm_raw) {

		glcm = new HashMap<Integer, double[][]>();// final glcm set of each angles
		ArrayList<Integer> anglesKey = new ArrayList<>(glcm_raw.keySet());
		Collections.sort(anglesKey);
		
		/*
		 * if do distance weighting, do first merge all angles, then calculate features.
		 */
		for (Integer a : anglesKey) {
			double[][] glcm_raw_at_a = glcm_raw.get(a);
			// skip all zero matrix
			if (glcm_raw_at_a == null) {
				glcm.put(a, null);
				continue;
			}
			double[][] norm_glcm_at_a = normalize(glcm_raw_at_a);
			glcm.put(a, norm_glcm_at_a);
		}

		calculateCoefficients();

		return glcm;
	}

	private void calculateCoefficients() {

		/*
		 * self.coefficients['i'] = i //meshgrid 1 of nBins*nBins self.coefficients['j']
		 * = j //meshgrid 2 of nBins*nBins self.coefficients['kValuesSum'] = kValuesSum
		 * //numpy.arange(2, (Ng * 2) + 1, dtype='float')
		 * self.coefficients['kValuesDiff'] = kValuesDiff //numpy.arange(0, Ng,
		 * dtype='float') self.coefficients['px'] = px // self.P_glcm.sum(2,
		 * keepdims=True), marginal row probabilities self.coefficients['py'] = py //
		 * self.P_glcm.sum(1, keepdims=True), marginal column probabilities
		 * self.coefficients['ux'] = ux // numpy.sum(i[None, :, :, None] * self.P_glcm,
		 * (1, 2), keepdims=True), the glcm's mean gray level intensity of px (but not
		 * divide sum of matrix) self.coefficients['uy'] = uy // numpy.sum(j[None, :, :,
		 * None] * self.P_glcm, (1, 2), keepdims=True), the glcm's mean gray level
		 * intensity of py (but not divide sum of matrix) self.coefficients['pxAddy'] =
		 * pxAddy //numpy.array([numpy.sum(self.P_glcm[:, i + j == k, :], 1) for k in
		 * kValuesSum]).transpose((1, 0, 2)) # shape = (Nv, 2*Ng-1, angles)
		 * self.coefficients['pxSuby'] = pxSuby //numpy.array([numpy.sum(self.P_glcm[:,
		 * numpy.abs(i - j) == k, :], 1) for k in kValuesDiff]).transpose((1, 0, 2)) #
		 * shape = (Nv, Ng, angles) self.coefficients['HXY'] = HXY //HXY = (-1) *
		 * numpy.sum((self.P_glcm * numpy.log2(self.P_glcm + eps)), (1, 2)) # shape =
		 * (Nv, angles)
		 */

		/*
		 * coeffs : angle and coeffs of it angle glcm
		 */
		coeffs = new HashMap<Integer, HashMap<String, Object>>();
		ArrayList<Integer> angles = new ArrayList<>(glcm.keySet());
		Collections.sort(angles);
		for (Integer a : angles) {
			double[][] glcm_a = glcm.get(a);
			if (glcm_a == null) {
				this.coeffs.put(a, null);
				continue;
			}
			HashMap<String, Object> coeffs_a = new HashMap<>();
			double[] px = new double[nBins];
			double[] py = new double[nBins];
			//p_{i+j}
			double[] pXAddY = new double[(nBins * 2) - 1];// 2 to nBins*2
			//p_{i-j}
			double[] pXSubY = new double[nBins];
			double ux = 0.0;
			double uy = 0.0;
			double stdevx = 0.0;
			double stdevy = 0.0;

			// Px(i) the marginal row probabilities
			// Py(i) the marginal column probabilities
			// First, initialize the arrays to 0
			for (int i = 0; i < nBins; i++) {
				px[i] = 0.0;
				py[i] = 0.0;
			}

			for (int i = 0; i < nBins; i++) {
				for (int j = 0; j < nBins; j++) {
					px[i] += glcm_a[i][j];// sum of the cols at row
					py[i] += glcm_a[j][i];// sum of the rows at col
				}
			}
			// calculate meanx and meany
			for (int i = 1; i <= nBins; i++) {
				ux += (i * px[i - 1]);
				uy += (i * py[i - 1]);
			}
			// calculate stdevx and stdevy
			for (int i = 1; i <= nBins; i++) {
				stdevx += ((Math.pow((i - ux), 2)) * px[i - 1]);
				stdevy += ((Math.pow((i - uy), 2)) * py[i - 1]);
			}
			stdevx = Math.sqrt(stdevx);
			stdevy = Math.sqrt(stdevy);

			int addK_max = nBins * 2;
			for (int k = 2; k <= addK_max; k++) {
				for (int i = 1; i <= nBins; i++) {
					for (int j = 1; j <= nBins; j++) {
						if (k == (i + j)) {
							pXAddY[k - 2] += glcm_a[i - 1][j - 1];
						}
					}
				}
			}
			int subK_max = nBins;
			for (int k = 0; k < subK_max; k++) {
				for (int i = 1; i <= nBins; i++) {
					for (int j = 1; j <= nBins; j++) {
						if (k == Math.abs(i - j)) {
							pXSubY[k] += glcm_a[i - 1][j - 1];
						}
					}
				}
			}
			coeffs_a.put("Px", px);
			coeffs_a.put("Py", py);
			coeffs_a.put("MeanX", ux);
			coeffs_a.put("MeanY", uy);
			coeffs_a.put("pXAddY", pXAddY);
			coeffs_a.put("pXSubY", pXSubY);
			coeffs_a.put("StdDevX", stdevx);
			coeffs_a.put("StdDevY", stdevy);
			this.coeffs.put(a, coeffs_a);
		}
	}
	
	/**
	 * "manhattan"
	 * "euclidian" //default
	 * "infinity" | Chebyshev distance
	 * 
	 * @param angleVector
	 * @param glcm_raw
	 * @return weighted glcm_raw
	 */
	public double[][] weighting(int[] angleVector, double[][] glcm_raw){
		double dx = orgCal.pixelWidth * angleVector[2];
		double dy = orgCal.pixelHeight* angleVector[1];
		double dz = orgCal.pixelDepth* angleVector[0];
		double distance = 1d;
		if(this.weightingMethod == null || this.weightingMethod.equals("no_weighting")) {
			return glcm_raw;
		}else if (this.weightingMethod.equals("manhattan")) {
			distance = dx+dy+dz;
		}else if (this.weightingMethod.equals("euclidian")) {
			distance = Math.sqrt(Math.pow(dx, 2)+Math.pow(dy, 2)+Math.pow(dz, 2));
		}else if (this.weightingMethod.equals("infinity")) {
			double max = 0d;
			for(double v : new double[] {dx,dy,dz}) {
				if(max < v) {
					max = v;
				}
			}
			distance = max;
		}else {
			return glcm_raw;
		}
		double w = Math.exp(-1*Math.pow(distance,2));
		double[][] weighted = new double[nBins][nBins];
		for(int i=0;i<nBins;i++) {
			for(int j=0;j<nBins;j++) {
				weighted[i][j] = glcm_raw[i][j]*w;
			}
		}
		return weighted;
	}

	public double getJointMaximum() {
		ArrayList<Integer> angles = new ArrayList<>(glcm.keySet());
		Collections.sort(angles);
		double[] res_set = new double[angles.size()];
		int itr = 0;
		for (Integer a : angles) {
			double glcm_a[][] = glcm.get(a);
			//for example, at force2D calculation only used 4 angles.
			if(glcm_a ==null) {
				continue;
			}
			double max_a = 0d;
			for (int i = 1; i <= nBins; i++) {
				for (int j = 1; j <= nBins; j++) {
					double v = glcm_a[i - 1][j - 1];
					if (max_a < v) {
						max_a = v;
					}
				}
			}
			res_set[itr] = max_a;
			itr++;
		}
		return StatUtils.mean(res_set);
	}

	// =====================================================================================================
	public double getJointAverage() {
		ArrayList<Integer> angles = new ArrayList<>(glcm.keySet());
		Collections.sort(angles);
		double[] res_set = new double[angles.size()];
		int itr = 0;
		for (Integer a : angles) {
			double glcm_a[][] = glcm.get(a);
			//for example, at force2D calculation only used 4 angles.
			if(glcm_a ==null) {
				continue;
			}
			double res_a = 0d;
			for (int i = 1; i <= nBins; i++) {
				for (int j = 1; j <= nBins; j++) {
					res_a += glcm_a[i - 1][j - 1] * (double) (i);
				}
			}
			res_set[itr] = res_a;
			itr++;
		}
		return StatUtils.mean(res_set);
	}

	// =====================================================================================================
	// calculate the variance ("variance" in Walker 1995; "Sum of Squares: Variance"
	// in Haralick 1973)
	// also called Sum of Squares.
	public double getJointVariance() {
		ArrayList<Integer> angles = new ArrayList<>(glcm.keySet());
		Collections.sort(angles);
		double[] res_set = new double[angles.size()];
		int itr = 0;
		for (Integer a : angles) {
			double glcm_a[][] = glcm.get(a);
			//for example, at force2D calculation only used 4 angles.
			if(glcm_a ==null) {
				continue;
			}
			double myu_a = 0d;// joint average at angle
			for (int i = 1; i <= nBins; i++) {
				for (int j = 1; j <= nBins; j++) {
					myu_a += glcm_a[i - 1][j - 1] * (double) (i);
				}
			}

			double res_a = 0d;
			for (int i = 1; i <= nBins; i++) {
				for (int j = 1; j <= nBins; j++) {
					res_a += Math.pow(i - myu_a, 2) * glcm_a[i - 1][j - 1];
				}
			}
			res_set[itr] = res_a;
			itr++;
		}
		return StatUtils.mean(res_set);
	}

	// ===============================================================================================
	// calculate the entropy (Haralick et al., 1973; Walker, et al., 1995)
	public double getJointEntropy() {
		ArrayList<Integer> angles = new ArrayList<>(glcm.keySet());
		Collections.sort(angles);
		double[] res_set = new double[angles.size()];
		int itr = 0;
		for (Integer a : angles) {
			double glcm_a[][] = glcm.get(a);
			//for example, at force2D calculation only used 4 angles.
			if(glcm_a ==null) {
				continue;
			}
			double entropy = 0.0;
			for (int i = 1; i <= nBins; i++) {
				for (int j = 1; j <= nBins; j++) {
					if (glcm_a[i - 1][j - 1] != 0) {
						entropy = entropy - (glcm_a[i - 1][j - 1] * ((Math.log(glcm_a[i - 1][j - 1]+eps)) / Math.log(2.0)));
					}
				}
			}
			res_set[itr] = entropy;
			itr++;
		}
		return StatUtils.mean(res_set);
	}

	// =====================================================================================================
	public double getDifferenceAverage() {
		ArrayList<Integer> angles = new ArrayList<>(glcm.keySet());
		Collections.sort(angles);
		double[] res_set = new double[angles.size()];
		int itr = 0;
		for (Integer a : angles) {
			if(coeffs.get(a)==null) {
				continue;
			}
			double[] pXSubY = (double[]) coeffs.get(a).get("pXSubY");
			double res_a = 0.0;
			for (int k = 0; k < nBins; k++) {
				res_a += pXSubY[k] * k;
			}
			res_set[itr] = res_a;
			itr++;
		}
		return StatUtils.mean(res_set);
	}

	// =====================================================================================================
	public double getDifferenceVariance() {
		ArrayList<Integer> angles = new ArrayList<>(glcm.keySet());
		Collections.sort(angles);
		double[] res_set = new double[angles.size()];
		int itr = 0;
		for (Integer a : angles) {
			if(coeffs.get(a)==null) {
				continue;
			}
			double[] pXSubY = (double[]) coeffs.get(a).get("pXSubY");
			double res_a = 0.0;
			double diff_avg = 0d;
			for (int k = 0; k < nBins; k++) {
				diff_avg += pXSubY[k] * k;
			}
			for (int k = 0; k < nBins; k++) {
				res_a += Math.pow((k - diff_avg), 2) * pXSubY[k];
			}
			res_set[itr] = res_a;
			itr++;
		}
		return StatUtils.mean(res_set);
	}

	// =====================================================================================================
	public double getDifferenceEntropy() {
		ArrayList<Integer> angles = new ArrayList<>(glcm.keySet());
		Collections.sort(angles);
		double[] res_set = new double[angles.size()];
		int itr = 0;
		for (Integer a : angles) {
			if(coeffs.get(a)==null) {
				continue;
			}
			double[] pXSubY = (double[]) coeffs.get(a).get("pXSubY");
			double res_a = 0.0;

			for (int k = 0; k < nBins; k++) {
				res_a -= pXSubY[k] * (Math.log(pXSubY[k]+eps) / Math.log(2.0));
			}
			res_set[itr] = res_a;
			itr++;
		}
		return StatUtils.mean(res_set);
	}

	public double getSumAverage() {
		ArrayList<Integer> angles = new ArrayList<>(glcm.keySet());
		Collections.sort(angles);
		double[] res_set = new double[angles.size()];
		int itr = 0;
		for (Integer a : angles) {
			if(coeffs.get(a)==null) {
				continue;
			}
			double[] pXAddY = (double[]) coeffs.get(a).get("pXAddY");
			double res_a = 0.0;
			for (int k = 2; k <= nBins * 2; k++) {
				res_a += pXAddY[k - 2] * k;
			}
			res_set[itr] = res_a;
			itr++;
		}
		return StatUtils.mean(res_set);
	}

	public double getSumVariance() {
		ArrayList<Integer> angles = new ArrayList<>(glcm.keySet());
		Collections.sort(angles);
		double[] res_set = new double[angles.size()];
		int itr = 0;
		for (Integer a : angles) {
			if(coeffs.get(a)==null) {
				continue;
			}
			double[] pXAddY = (double[]) coeffs.get(a).get("pXAddY");
			double myu = 0;
			for (int k = 2; k <= nBins * 2; k++) {
				myu += pXAddY[k - 2] * k;
			}
			double res_a = 0.0;
			for (int k = 2; k <= nBins * 2; k++) {
				res_a += Math.pow(k - myu, 2) * pXAddY[k - 2];
			}
			res_set[itr] = res_a;
			itr++;
		}
		return StatUtils.mean(res_set);
	}

	public double getSumEntropy() {
		ArrayList<Integer> angles = new ArrayList<>(glcm.keySet());
		Collections.sort(angles);
		double[] res_set = new double[angles.size()];
		int itr = 0;
		for (Integer a : angles) {
			if(coeffs.get(a)==null) {
				continue;
			}
			double[] pXAddY = (double[]) coeffs.get(a).get("pXAddY");
			double res_a = 0.0;
			for (int k = 2; k <= nBins * 2; k++) {
				res_a += pXAddY[k - 2] * (Math.log(pXAddY[k - 2]+eps) / Math.log(2.0));
			}
			res_set[itr] = -1 * res_a;
			itr++;
		}
		return StatUtils.mean(res_set);
	}

	// =====================================================================================================
	// calculate the angular second moment (asm)
	/**
	 * also known as 'energy' (formula 15.38, Bankman, 2009)
	 * 
	 * @return
	 */
	public double getAngular2ndMoment() {
		ArrayList<Integer> angles = new ArrayList<>(glcm.keySet());
		Collections.sort(angles);
		double[] res_set = new double[angles.size()];
		int itr = 0;
		for (Integer a : angles) {
			double glcm_a[][] = glcm.get(a);
			if(glcm_a==null) {
				continue;
			}
			double res_a = 0.0;
			for (int i = 1; i <= nBins; i++) {
				for (int j = 1; j <= nBins; j++) {
					if (glcm_a[i - 1][j - 1] != 0) {
						res_a += glcm_a[i - 1][j - 1] * glcm_a[i - 1][j - 1];
					}
				}
			}
			res_set[itr] = res_a;
			itr++;
		}
		return StatUtils.mean(res_set);
	}

	// ===============================================================================================
	// (formula 15.39, Bankman, 2009) energy weighted by pixel value difference
	// same as Inertia.
	public double getContrast() {
		ArrayList<Integer> angles = new ArrayList<>(glcm.keySet());
		Collections.sort(angles);
		double[] res_set = new double[angles.size()];
		int itr = 0;
		for (Integer a : angles) {
			double glcm_a[][] = glcm.get(a);
			if(glcm_a==null) {
				continue;
			}
			double contrast = 0.0;
			for (int i = 1; i <= nBins; i++) {
				for (int j = 1; j <= nBins; j++) {
					if (glcm_a[i - 1][j - 1] != 0) {
						contrast += Math.pow(i - j, 2) * (glcm_a[i - 1][j - 1]);
					}
				}
			}
			res_set[itr] = contrast;
			itr++;
		}
		return StatUtils.mean(res_set);
	}

	public double getDissimilarity() {
		ArrayList<Integer> angles = new ArrayList<>(glcm.keySet());
		Collections.sort(angles);
		double[] res_set = new double[angles.size()];
		int itr = 0;
		for (Integer a : angles) {
			double glcm_a[][] = glcm.get(a);
			if(glcm_a==null) {
				continue;
			}
			double res_a = 0.0;
			for (int i = 1; i <= nBins; i++) {
				for (int j = 1; j <= nBins; j++) {
					if (glcm_a[i - 1][j - 1] != 0) {
						res_a += Math.abs(i - j) * (glcm_a[i - 1][j - 1]);
					}
				}
			}
			res_set[itr] = res_a;
			itr++;
		}
		return StatUtils.mean(res_set);
	}

	public double getInverseDifference() {
		ArrayList<Integer> angles = new ArrayList<>(glcm.keySet());
		Collections.sort(angles);
		double[] res_set = new double[angles.size()];
		int itr = 0;
		for (Integer a : angles) {
			if(coeffs.get(a)==null) {
				continue;
			}
			double[] pXSubY = (double[]) coeffs.get(a).get("pXSubY");
			double res_a = 0.0;
			for (int k = 0; k < nBins; k++) {
				res_a += pXSubY[k] / (1.0 + k);
			}
			res_set[itr] = res_a;
			itr++;
		}
		return StatUtils.mean(res_set);
	}

	public double getNormalisedInverseDifference() {
		ArrayList<Integer> angles = new ArrayList<>(glcm.keySet());
		Collections.sort(angles);
		double[] res_set = new double[angles.size()];
		int itr = 0;
		for (Integer a : angles) {
			if(coeffs.get(a)==null) {
				continue;
			}
			double[] pXSubY = (double[]) coeffs.get(a).get("pXSubY");
			double res_a = 0.0;
			for (int k = 0; k < nBins; k++) {
				res_a += pXSubY[k] / (1.0 + k / (double) nBins);
			}
			res_set[itr] = res_a;
			itr++;
		}
		return StatUtils.mean(res_set);
	}

	public double getInverseDifferenceMoment() {
		ArrayList<Integer> angles = new ArrayList<>(glcm.keySet());
		Collections.sort(angles);
		double[] res_set = new double[angles.size()];
		int itr = 0;
		for (Integer a : angles) {
			double glcm_a[][] = glcm.get(a);
			if(glcm_a==null) {
				continue;
			}
			double res_a = 0.0;
			for (int i = 1; i <= nBins; i++) {
				for (int j = 1; j <= nBins; j++) {
					if (glcm_a[i - 1][j - 1] != 0) {
						res_a += glcm_a[i - 1][j - 1] / (1.0 + (Math.pow(i - j, 2)));
					}
				}
			}
			res_set[itr] = res_a;
			itr++;
		}
		return StatUtils.mean(res_set);
	}

	public double getNormalisedInverseDifferenceMoment() {
		ArrayList<Integer> angles = new ArrayList<>(glcm.keySet());
		Collections.sort(angles);
		double[] res_set = new double[angles.size()];
		int itr = 0;
		for (Integer a : angles) {
			if(glcm.get(a)==null) {
				continue;
			}
			double glcm_a[][] = glcm.get(a);
			double res_a = 0.0;
			for (int i = 1; i <= nBins; i++) {
				for (int j = 1; j <= nBins; j++) {
					if (glcm_a[i - 1][j - 1] != 0) {
						res_a += glcm_a[i - 1][j - 1] / (1.0 + (Math.pow(i - j, 2) / Math.pow(nBins, 2)));
					}
				}
			}
			res_set[itr] = res_a;
			itr++;
		}
		return StatUtils.mean(res_set);
	}

	public double getInverseVariance() {
		ArrayList<Integer> angles = new ArrayList<>(glcm.keySet());
		Collections.sort(angles);
		double[] res_set = new double[angles.size()];
		int itr = 0;
		for (Integer a : angles) {
			if(glcm.get(a)==null) {
				continue;
			}
			double[] pXSubY = (double[]) coeffs.get(a).get("pXSubY");
			double res_a = 0.0;
			for (int k = 1; k < nBins; k++) { // 1 <= k <= Ng-1
//				if (!Double.isNaN(Double.valueOf(pXSubY[k])) && pXSubY[k] != 0) {
					res_a += pXSubY[k] / (double) (k * k);
//				}
			}
			res_set[itr] = res_a;
			itr++;
		}
		return StatUtils.mean(res_set);
	}

	// =====================================================================================================
	/**
	 * calculate the correlation methods based on Haralick 1973 (and MatLab), Walker
	 * 1995 are included below Haralick/Matlab result reported for correlation
	 * currently; will give Walker as an option in the future
	 */
	public double getCorrelation() {
		ArrayList<Integer> angles = new ArrayList<>(glcm.keySet());
		Collections.sort(angles);
		double[] res_set = new double[angles.size()];
		int itr = 0;
		for (Integer a : angles) {
			if(glcm.get(a)==null) {
				continue;
			}
			double[][] glcm_a = glcm.get(a);
			double meanX = (double) coeffs.get(a).get("MeanX");
			double meanY = (double) coeffs.get(a).get("MeanY");
			double stdevX = (double) coeffs.get(a).get("StdDevX");
			double stdevY = (double) coeffs.get(a).get("StdDevY");
			double res_a = 0.0;
			for (int i = 1; i <= nBins; i++) {
				for (int j = 1; j <= nBins; j++) {
					res_a += ((i - meanX) * (j - meanY)) * glcm_a[i - 1][j - 1];
				}
			}
			res_set[itr] = (1d / (stdevX * stdevY)) * res_a;
			itr++;
		}
		return StatUtils.mean(res_set);

	}

	// =====================================================================================================
	public double getAutocorrection() {
		ArrayList<Integer> angles = new ArrayList<>(glcm.keySet());
		Collections.sort(angles);
		double[] res_set = new double[angles.size()];
		int itr = 0;
		for (Integer a : angles) {
			if(glcm.get(a)==null) {
				continue;
			}
			double glcm_a[][] = glcm.get(a);
			double res_a = 0d;
			for (int i = 1; i <= nBins; i++) {
				for (int j = 1; j <= nBins; j++) {
					res_a += glcm_a[i - 1][j - 1] * (double) (i * j);
				}
			}
			res_set[itr] = res_a;
			itr++;
		}
		return StatUtils.mean(res_set);
	}

	// =====================================================================================================
	public double getClusterTendency() {
		ArrayList<Integer> angles = new ArrayList<>(glcm.keySet());
		Collections.sort(angles);
		double[] res_set = new double[angles.size()];
		int itr = 0;
		for (Integer a : angles) {
			if(glcm.get(a)==null) {
				continue;
			}
			double glcm_a[][] = glcm.get(a);
			double meanX = (double) coeffs.get(a).get("MeanX");
			double meanY = (double) coeffs.get(a).get("MeanY");
			double res_a = 0d;
			for (int i = 1; i <= nBins; i++) {
				for (int j = 1; j <= nBins; j++) {
					res_a += (Math.pow((i + j - meanX - meanY), 2) * glcm_a[i - 1][j - 1]);
				}
			}
			res_set[itr] = res_a;
			itr++;
		}
		return StatUtils.mean(res_set);
	}

	// =====================================================================================================
	/**
	 * Shade calculate the shade (Walker, et al., 1995; Connors, et al. 1984)
	 * 
	 * @return
	 */
	public double getClusterShade() {
		ArrayList<Integer> angles = new ArrayList<>(glcm.keySet());
		Collections.sort(angles);
		double[] res_set = new double[angles.size()];
		int itr = 0;
		for (Integer a : angles) {
			if(glcm.get(a)==null) {
				continue;
			}
			double glcm_a[][] = glcm.get(a);
			double meanX = (double) coeffs.get(a).get("MeanX");
			double meanY = (double) coeffs.get(a).get("MeanY");
			double res_a = 0d;
			for (int i = 1; i <= nBins; i++) {
				for (int j = 1; j <= nBins; j++) {
					res_a += (Math.pow((i + j - meanX - meanY), 3) * glcm_a[i - 1][j - 1]);
				}
			}
			res_set[itr] = res_a;
			itr++;
		}
		return StatUtils.mean(res_set);
	}

	// =====================================================================================================
	public double getClusterProminence() {
		ArrayList<Integer> angles = new ArrayList<>(glcm.keySet());
		Collections.sort(angles);
		double[] res_set = new double[angles.size()];
		int itr = 0;
		for (Integer a : angles) {
			if(glcm.get(a)==null) {
				continue;
			}
			double glcm_a[][] = glcm.get(a);
			double meanX = (double) coeffs.get(a).get("MeanX");
			double meanY = (double) coeffs.get(a).get("MeanY");
			double res_a = 0d;
			for (int i = 1; i <= nBins; i++) {
				for (int j = 1; j <= nBins; j++) {
					res_a += (Math.pow((i + j - meanX - meanY), 4) * glcm_a[i - 1][j - 1]);
				}
			}
			res_set[itr] = res_a;
			itr++;
		}
		return StatUtils.mean(res_set);
	}

	// ===============================================================================================
	// calculate the energy- same as Angular 2nd Moment-
	public double getJointEnergy() {
		return getAngular2ndMoment();
	}

	// ===============================================================================================
	public double getInformationalMeasureOfCorrelation1() {
		ArrayList<Integer> angles = new ArrayList<>(glcm.keySet());
		Collections.sort(angles);
		double[] res_set = new double[angles.size()];
		int itr = 0;
		for (Integer a : angles) {
			if(glcm.get(a)==null) {
				continue;
			}
			double glcm_a[][] = glcm.get(a);
			double[] Px = (double[]) coeffs.get(a).get("Px");
			double[] Py = (double[]) coeffs.get(a).get("Py");
			double HXY = 0.0; // JointEntropy
			double HXY1 = 0.0;
			double HX = 0d;
			for (int i = 1; i <= nBins; i++) {
				HX -= Px[i - 1] * ((Math.log(Px[i - 1]+ eps)) / Math.log(2.0));
				for (int j = 1; j <= nBins; j++) {
					HXY -= (glcm_a[i - 1][j - 1] * ((Math.log(glcm_a[i - 1][j - 1]+ eps)) / Math.log(2.0)));
					HXY1 -= (glcm_a[i - 1][j - 1] * (Math.log((Px[i - 1] * Py[j - 1]) + eps) / Math.log(2.0)));
				}
			}
			res_set[itr] = (HXY - HXY1) / HX;
			itr++;
		}
		return StatUtils.mean(res_set);
	}

	// ===============================================================================================
	public double getInformationalMeasureOfCorrelation2() {
		ArrayList<Integer> angles = new ArrayList<>(glcm.keySet());
		Collections.sort(angles);
		double[] res_set = new double[angles.size()];
		int itr = 0;
		for (Integer a : angles) {
			if(glcm.get(a)==null) {
				continue;
			}
			double glcm_a[][] = glcm.get(a);
			double[] Px = (double[]) coeffs.get(a).get("Px");
			double[] Py = (double[]) coeffs.get(a).get("Py");
			double HXY = 0.0; // JointEntropy
			double HXY2 = 0.0;
			for (int i = 1; i <= nBins; i++) {
				for (int j = 1; j <= nBins; j++) {
					HXY = HXY - glcm_a[i - 1][j - 1] * (Math.log((glcm_a[i - 1][j - 1]) + eps) / Math.log(2.0));
					HXY2 = HXY2 - Px[i - 1] * Py[j - 1] * (Math.log((Px[i - 1] * Py[j - 1]) + eps) / Math.log(2.0));
				}
			}
			double res_a = Math.sqrt(1. - (Math.exp(-2. * (HXY2 - HXY))));
			if (!Double.isNaN(res_a)) {
				res_set[itr] = res_a;
				itr++;
			}
		}
		return StatUtils.mean(res_set);
	}

//	//===============================================================================================
//	// calculate the MCC
//	public double getMaximalCorrelationCoefficient() {
//		double MCC = 0.0;
////		double[][] Qs = new double[nBins][nBins];
//		double[] Qs = new double[nBins*nBins];
//		double [] px = new double [nBins];
//		double [] py = new double [nBins];
//		for (int i=0; i<nBins; i++){
//			px[i] = 0.0;
//			py[i] = 0.0;
//		}
//		// sum the glcm rows to px(i)
//		for (int i = 0; i < nBins; i++) {
//			for (int j = 0; j < nBins; j++) {
//				px[i] += glcm[i][j];
//			}
//		}
//		// sum the glcm rows to Py(j)
//		for (int j = 0; j < nBins; j++) {
//			for (int i = 0; i < nBins; i++) {
//				py[j] += glcm[i][j];
//			}
//		}
//		int num = 0;
//		for(int i=0;i<nBins;i++) {
//			for(int j=0;j<nBins;j++) {
//				for(int k=0;k<nBins;k++) {
////					Qs[i][j] += glcm[i][k]*glcm[j][k]/(px[i]*py[k]);
//					if((px[i]*py[k]) == 0.0) {
//						continue;
//					}
//					Qs[num] += glcm[i][k]*glcm[j][k]/(px[i]*py[k]);
//				}
//				num++;
//			}
//		}
//		Arrays.sort(Qs);//ascending		
//		MCC = Math.sqrt(Qs[Qs.length-2]);//second largest eigen value of Q
//		return MCC;
//	}

	// ===============================================================================================
	// calculate the IDMN
//	public double getInverseDifferenceMomentNormalized() {
//		double IDMN = 0.0;
//		for(int k=0;k<nBins;k++) {
//			IDMN += pXSubY[k]/(1.0+(k*k/(double)nBins*nBins));
//		}
//		return IDMN;
//	}

//	public double getInverseDifferenceNormalized() {
//		double IDN = 0.d;
//		for(int k=0;k<nBins;k++) {
//			IDN += pXSubY[k]/(1.0+(k/(double)nBins));
//		}
//		return IDN;
//	}

//	public double getSumSquare() {
//		double ss = 0.0;
//		for (int i=1; i<=nBins; i++) {
//			for (int j=1; j<=nBins; j++) {
//				ss += Math.pow(i-meanx,2)*glcm[i-1][j-1];
//			}
//		}
//		return ss;
//	}

	// ===============================================================================================
	// calculate the homogeneity (Parker)
	// "Local Homogeneity" from Conners, et al., 1984 is calculated the same as IDM
	// above
	// Parker's implementation is below; absolute value of i-j is taken rather than
	// square
	// - matlab textbook also uses non-squred absolute difference |i-j|
	// -- using absolute value, flat image (diagonal) will be 1.
	/*
	 * mathematically equal to Inverse Difference
	 */
	@Deprecated
	public double getHomogeneity() {
		return getInverseDifference();
	}

	// ===============================================================================================
	// calculate the variance ("variance" in Walker 1995; "Sum of Squares: Variance"
	// in Haralick 1973)
	/**
	 * same as JointVariance
	 */
	@Deprecated
	public double getSumSquare() {
		return getJointVariance();
	}

	// ===============================================================================================
	// calculate the inertia (Walker, et al., 1995; Connors, et al. 1984)
	// same as contrast
	@Deprecated
	public double getInertia() {
		return getContrast();
	}

	public double[][] getMatrix(int[] angle) {
		if (glcm == null || glcm.size() < 1) {
			return null;
		}
		HashMap<Integer, int[]> angles = Utils.buildAngles();
		ArrayList<Integer> angles_key = new ArrayList<>(glcm.keySet());
		for (Integer a_id : angles_key) {
			int[] ang = angles.get(a_id);
			if (ang[0] == angle[0] && ang[1] == angle[1] && ang[2] == angle[2]) {
				return glcm.get(a_id);
			}
		}
		return null;
	}

//	public String toString() {
//		String res = "";
//		for(int row =0;row<glcm.length;row++) {
//			StringBuilder sb = new StringBuilder();
//			for(int col =0;col<glcm[0].length;col++) {
//				sb.append(IJ.d2s(glcm[row][col],2));
//				sb.append(" ");
//			}
//			sb.append("\n");
//			res = res + sb.toString();
//		}
//		return res;
//	}

	public void checkCoefficients(int angle_id, String key) {
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

	/**
	 * toString(getMatrix())
	 * 
	 * @param mat
	 * @return
	 */
	public String toString(double[][] mat) {
		String res = "";
		for (int row = 0; row < mat.length; row++) {
			StringBuilder sb = new StringBuilder();
			for (int col = 0; col < mat[0].length; col++) {
				sb.append(IJ.d2s(mat[row][col], 3));
				sb.append(" ");
			}
			sb.append("\n");
			res = res + sb.toString();
		}
		return res;
	}

	@Override
	public Set<String> getAvailableFeatures() {
		Set<String> names = new HashSet<String>();
		for(GLCMFeatureType t : GLCMFeatureType.values()) {
			names.add(t.name());
		}
		return names;
	}

	@Override
	public String getFeatureFamilyName() {
		return "GLCM";
	}

	@Override
	public Map<String, Object> getSettings() {
		return settings;
	}
}