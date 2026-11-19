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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import ij.ImagePlus;
import io.github.tatsunidas.radiomics.main.ImagePreprocessing;
import io.github.tatsunidas.radiomics.main.RadiomicsJ;
import io.github.tatsunidas.radiomics.main.Utils;

/**
 * The (cumulative) intensity-volume histogram (IVH), IBSI 1 section 3.5.
 *
 * <p>All modes are computed on one integer grid of bin numbers k, so no value is compared in floating point
 * and no branch depends on the content of the data:
 * <ul>
 * <li>mode 0 (no discretisation: calibrated, discrete intensities such as CT): k = round-half-even(X), x(k) = k,
 *     G = the re-segmentation range (a missing side is taken from the data). A non-integer intensity
 *     (a CT interpolated without rounding) gives the same IVH as the rounded image.</li>
 * <li>mode 1 (fixed bin width w, continuous intensities such as PET): k is the bin number of
 *     {@link Utils#discreteByBinWidth}, x(k) = Xmin + (k - 0.5) w, G = bins 1..Nb, that is
 *     [Xmin + 0.5w, Xmax - 0.5w] in intensity.</li>
 * <li>mode 2 (fixed bin number Ng, arbitrary units): k = bin number, x(k) = k, G = [1, Ng].</li>
 * </ul>
 * With nu_k the fraction of the volume with bin number k or more, and gamma_k = (k - kmin)/(kmax - kmin):
 * the intensity at volume fraction p is x(k) of the smallest k in G with nu_k &lt;= p (searched on the whole grid,
 * also on bins that no voxel has, and past kmax when no bin of G qualifies); the volume at intensity fraction p
 * is nu_k of the smallest k with gamma_k &gt;= p; the area under the curve is the trapezoidal rule on the grid.
 * @author tatsunidas
 *
 */
public class IntensityVolumeHistogramFeatures extends AbstractRadiomicsFeature{
	
	final int label;
	boolean continuousCalibrated = false;
	
	/** sorted bin numbers that at least one voxel has. */
	private long[] bins = new long[0];
	/** atOrAbove[j]: number of voxels in bins[j..]. */
	private long[] atOrAbove = new long[0];
	private long total = 0;
	/** total range G in bin numbers. */
	private long kmin, kmax;
	/** x(k) = offset + step * k. */
	private double offset, step = 1d;
	private int mode;
	
	public IntensityVolumeHistogramFeatures(ImagePlus img, ImagePlus mask, Map<String,Object> settings) {
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

	/**
	 * @param img
	 * @param mask
	 * @param label
	 * @param mode: 0:No discretize (default), 1 binWidth, 2 binCount. Any other value is mode 0.
	 * @throws Exception
	 */
	public IntensityVolumeHistogramFeatures(ImagePlus img, ImagePlus mask, int label, int mode) throws Exception {
		super(img, mask, null);
		this.label = label;
		if(mask == null) {
			this.mask = ImagePreprocessing.createMask(img.getWidth(), img.getHeight(), img.getNSlices(), null, this.label,img.getCalibration().pixelWidth, img.getCalibration().pixelHeight,img.getCalibration().pixelDepth);
		}
		this.img = img;
		settings.put(RadiomicsFeature.IMAGE, this.img);
		settings.put(RadiomicsFeature.MASK, this.mask);
		settings.put(RadiomicsFeature.LABEL, this.label);
		settings.put(RadiomicsFeature.IVH_MODE, mode);
		settings.put(RadiomicsFeature.BinCount_IVH, RadiomicsJ.IVH_binCount);
		settings.put(RadiomicsFeature.BinWidth_IVH, RadiomicsJ.IVH_binWidth);
		build(mode, RadiomicsJ.IVH_binCount, RadiomicsJ.IVH_binWidth);
	}
	
	public void buildup(Map<String,Object> settings) {
		if(mask == null) {
			mask = ImagePreprocessing.createMask(img.getWidth(), img.getHeight(), img.getNSlices(), null, this.label,img.getCalibration().pixelWidth, img.getCalibration().pixelHeight,img.getCalibration().pixelDepth);
		}
		Object modeValue = settings.get(RadiomicsFeature.IVH_MODE);
		if (modeValue == null) {
			throw new IllegalArgumentException("'IVH_MODE' is missing in settings.");
		}
		if (!(modeValue instanceof Integer)) {
			throw new IllegalArgumentException("'IVH_MODE' must be an Integer.");
		}
		Object bc = settings.get(RadiomicsFeature.BinCount_IVH);
		Object bw = settings.get(RadiomicsFeature.BinWidth_IVH);
		try {
			build((Integer)modeValue,
					bc instanceof Number ? ((Number)bc).intValue() : RadiomicsJ.IVH_binCount,
					bw instanceof Number ? ((Number)bw).doubleValue() : RadiomicsJ.IVH_binWidth);
		} catch (Exception e) {
			throw new IllegalArgumentException("IntensityVolumeHistogramFeature calculation was failed. Please check image data.", e);
		}
	}
	
	private void build(int mode, int binCount, double binWidth) throws Exception {
		this.mode = (mode == 1 || mode == 2) ? mode : 0;
		this.continuousCalibrated = this.mode == 1;
		final Double rMin = RadiomicsJ.rangeMin;
		final Double rMax = RadiomicsJ.rangeMax;
		long[] k;
		if(this.mode == 1) {
			ImagePlus dci = Utils.discreteByBinWidth(this.img, this.mask, this.label, binWidth);
			if(dci == null) {
				throw new IllegalArgumentException("IntensityVolumeHistogramFeature calculation was failed. Please check image data.");
			}
			k = toBins(Utils.getVoxels(dci, this.mask, this.label), false);
			double[] org = finite(Utils.getVoxels(this.img, this.mask, this.label));
			if(org.length == 0) {
				return;
			}
			double xmin = rMin != null ? rMin : Arrays.stream(org).min().getAsDouble();
			double xmax = rMax != null ? rMax : Arrays.stream(org).max().getAsDouble();
			offset = xmin - 0.5 * binWidth;
			step = binWidth;
			kmin = 1;
			// the range and the bin width are decimal numbers: divide them as decimals, not as binary floating point.
			kmax = new BigDecimal(Double.toString(xmax)).subtract(new BigDecimal(Double.toString(xmin)))
					.divide(new BigDecimal(Double.toString(binWidth)), 0, RoundingMode.FLOOR).longValue();
		}else if(this.mode == 2) {
			ImagePlus dci = Utils.discrete(this.img, this.mask, this.label, binCount);
			if(dci == null) {
				throw new IllegalArgumentException("IntensityVolumeHistogramFeature calculation was failed. Please check image data.");
			}
			k = toBins(Utils.getVoxels(dci, this.mask, this.label), false);
			offset = 0d;
			step = 1d;
			kmin = 1;
			kmax = Math.max(1, binCount);
		}else {
			k = toBins(Utils.getVoxels(this.img, this.mask, this.label), true);
			offset = 0d;
			step = 1d;
			if(k.length == 0) {
				return;
			}
			long dmin = Arrays.stream(k).min().getAsLong();
			long dmax = Arrays.stream(k).max().getAsLong();
			kmin = rMin != null ? (long)Math.ceil(rMin) : dmin;
			kmax = rMax != null ? (long)Math.floor(rMax) : dmax;
		}
		count(k);
	}
	
	private static double[] finite(double[] v) {
		return Arrays.stream(v).filter(d -> !Double.isNaN(d)).toArray();
	}
	
	/** bin number of every voxel. mode 0 rounds half to even, as Utils.roundIntensitiesToNearestInteger does. */
	private static long[] toBins(double[] v, boolean round) {
		return Arrays.stream(finite(v)).mapToLong(d -> round ? (long)Math.rint(d) : Math.round(d)).toArray();
	}
	
	private void count(long[] k) {
		TreeMap<Long, Long> freq = new TreeMap<>();
		for(long b : k) {
			freq.merge(b, 1L, Long::sum);
		}
		bins = new long[freq.size()];
		atOrAbove = new long[freq.size()];
		int i = 0;
		for(long b : freq.keySet()) {
			bins[i++] = b;
		}
		long run = 0;
		for(int j = bins.length - 1; j >= 0; j--) {
			run += freq.get(bins[j]);
			atOrAbove[j] = run;
		}
		total = run;
	}
	
	/** number of voxels with bin number k or more. */
	private long countAtOrAbove(long k) {
		int j = Arrays.binarySearch(bins, k);
		if(j < 0) {
			j = -j - 1;
		}
		return j >= bins.length ? 0L : atOrAbove[j];
	}
	
	private double intensityOf(long k) {
		return offset + step * k;
	}
	
	public Double calculate(String id) {
		String name = IntensityVolumeHistogramFeatureType.findType(id);
		if (name.equals(IntensityVolumeHistogramFeatureType.VolumeAtIntensityFraction10.name())) {
			return volumeAtIntensityFraction(10);
		}else if (name.equals(IntensityVolumeHistogramFeatureType.VolumeAtIntensityFraction90.name())) {
			return volumeAtIntensityFraction(90);
		}else if (name.equals(IntensityVolumeHistogramFeatureType.IntensityAtVolumeFraction10.name())) {
			return intensityAtVolumeFraction(10);
		}else if (name.equals(IntensityVolumeHistogramFeatureType.IntensityAtVolumeFraction90.name())) {
			return intensityAtVolumeFraction(90);
		}else if (name.equals(IntensityVolumeHistogramFeatureType.VolumeFractionDifferenceBetweenIntensityFractions.name())) {
			return volumeFractionDifference();
		}else if (name.equals(IntensityVolumeHistogramFeatureType.IntensityFractionDifferenceBetweenVolumeFractions.name())) {
			return intensityFractionDifference();
		}else if (name.equals(IntensityVolumeHistogramFeatureType.AreaUnderTheIVHCurve.name())) {
			return areaUnderTheIVHCurve();
		}
		return null;
	}
	
	/**
	 * The intensity at volume fraction p%: the smallest intensity on the grid that is in at most p% of the volume.
	 * nu changes only just above an occupied bin, so the smallest k is kmin or one of (occupied bin + 1).
	 * The last candidate (the highest occupied bin + 1) has nu = 0, so an answer always exists.
	 */
	private double intensityAtVolumeFraction(int p) {
		if(total == 0) {
			return Double.NaN;
		}
		if(countAtOrAbove(kmin) * 100L <= p * total) {
			return intensityOf(kmin);
		}
		for(long b : bins) {
			long k = b + 1;
			if(k > kmin && countAtOrAbove(k) * 100L <= p * total) {
				return intensityOf(k);
			}
		}
		return Double.NaN;// unreachable
	}
	
	/** The volume at intensity fraction p%: nu of the smallest grid point whose intensity fraction is at least p%. */
	private double volumeAtIntensityFraction(int p) {
		if(total == 0 || kmax <= kmin) {
			return Double.NaN;
		}
		long k = kmin + (p * (kmax - kmin) + 99L) / 100L;// ceil(p (kmax-kmin) / 100), exact
		return (double) countAtOrAbove(k) / total;
	}
	
	private double volumeFractionDifference() {
		return volumeAtIntensityFraction(10) - volumeAtIntensityFraction(90);
	}
	
	private double intensityFractionDifference() {
		return intensityAtVolumeFraction(10) - intensityAtVolumeFraction(90);
	}
	
	/**
	 * The area under the IVH curve (van Velden et al.): the trapezoidal rule of nu over gamma on the whole grid of G.
	 * It is 0 when the ROI has only one discretised intensity. IBSI gives no reference values for this feature.
	 */
	private double areaUnderTheIVHCurve() {
		if(total == 0) {
			return Double.NaN;
		}
		if(bins.length == 1) {
			return 0d;
		}
		if(kmax <= kmin) {
			return Double.NaN;
		}
		double sum = 0d;
		long prev = countAtOrAbove(kmin);
		for(long k = kmin + 1; k <= kmax; k++) {
			long cur = countAtOrAbove(k);
			sum += (double)(prev + cur) / 2d;
			prev = cur;
		}
		return sum / total / (kmax - kmin);
	}
	
	public String toString() {
		System.out.println("===  IVH result (mode " + mode + ", grid " + kmin + ".." + kmax + ")  ===");
		for(long k = kmin; k <= kmax; k++) {
			System.out.println("bin:" + k + ", intensity:" + intensityOf(k) + ", nu:" + (double) countAtOrAbove(k) / total);
		}
		System.out.println("=== IVH result end ===");
		return null;
	}

	@Override
	public Set<String> getAvailableFeatures() {
		Set<String> names = new HashSet<String>();
		for(IntensityVolumeHistogramFeatureType t : IntensityVolumeHistogramFeatureType.values()) {
			names.add(t.name());
		}
		return names;
	}

	@Override
	public String getFeatureFamilyName() {
		return "IntensityVolumeHistogram";
	}

	@Override
	public Map<String, Object> getSettings() {
		return settings;
	}

}
