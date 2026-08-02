/*
 * Copyright [2026] [Tatsuaki Kobayashi]

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
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import io.github.tatsunidas.radiomics.main.ImagePreprocessing;
import io.github.tatsunidas.radiomics.main.RadiomicsJ;
import io.github.tatsunidas.radiomics.main.Utils;

/**
 * Gray Level Affinity Metrics, the GLAM family.
 *
 * Where the GLCM asks how often two gray levels sit next to each other, GLAM
 * asks the same question at every distance at once. The voxels of the roi are
 * treated as a mixture of interacting particles, and the radial distribution
 * function
 *
 * <pre>
 *   g(alpha, beta, r)
 * </pre>
 *
 * gives the likelihood of meeting a voxel of gray level beta at distance r from
 * a voxel of gray level alpha, relative to the likelihood one would meet by
 * pure chance. A value above one means the two gray levels cluster at that
 * distance, a value below one means they avoid each other, and a value of one
 * means they are arranged independently.
 *
 * From those curves the framework derives a set of nBins x nBins matrices, each
 * of which condenses one physical aspect of the arrangement, for instance the
 * net affinity between two gray levels (second virial coefficient) or the
 * amount of disorder relative to a randomly shuffled roi (configurational
 * disorder index). See {@link GLAMMatrixType}. The reported features are
 * statistics of those matrices, exactly as the GLCM features are statistics of
 * the co-occurrence matrix.
 *
 * <h2>Distances are measured in voxels</h2>
 * The radial distribution function is evaluated on the voxel lattice, so the
 * roi should be resampled to isotropic voxels before the extraction. A warning
 * is logged when the voxel spacing is not isotropic.
 *
 * <h2>The randomised reference state</h2>
 * Several matrices compare the observed arrangement against the arrangement one
 * would see if the gray levels were shuffled over the roi at random. That
 * reference state can be estimated by actually shuffling the roi a number of
 * times, which is what the original framework does, but it also has a closed
 * form. Writing phi(r) for the fraction of the shell at distance r that still
 * falls inside the roi, N for the number of roi voxels and N(alpha) for the
 * number of voxels of gray level alpha, the expected randomised curve is
 *
 * <pre>
 *   g_random(alpha, beta, r) = phi(r) * N / (N - 1)                        alpha != beta
 *   g_random(alpha, alpha, r) = phi(r) * N * (N(alpha) - 1)
 *                                            / (N(alpha) * (N - 1))
 * </pre>
 *
 * because shuffling destroys every spatial correlation and leaves only the
 * geometry of the roi plus the fact that a voxel is never its own neighbour.
 * RadiomicsJ uses that closed form by default: it is exact, reproducible and
 * costs nothing, whereas a few random shuffles leave a noise floor that shows
 * up directly in the derived features. Set the number of randomisations above
 * zero to fall back to the sampled estimate.
 *
 * Reference: Physics-Informed Multiscale Decoding of Tissue Microstructure,
 * The Gray Level Affinity Metrics (GLAM) Framework, Journal of Imaging
 * Informatics in Medicine (2026), doi 10.1007/s10278-026-02132-6
 *
 * @author tatsunidas <t_kobayashi@vis-ionary.com>
 *         (implemented with the coding assistant Claude, Anthropic)
 */
public class GLAMFeatures extends AbstractRadiomicsFeature implements Texture {

	/** Discretised image, gray levels run from 1 to nBins inside the roi. */
	ImagePlus discImg;

	final int label;
	int nBins;
	double binWidth;

	/** Largest distance, in voxels, at which the radial distribution is evaluated. */
	int maxRadius;
	/**
	 * Reference voxels per gray level, or zero to use every voxel of the roi.
	 * Sub sampling trades accuracy for speed on very large rois.
	 */
	int maxReferenceVoxels;
	/** Divide each shell by the part of it that lies inside the roi. */
	boolean boundaryCorrection;
	/** Number of shuffles for the randomised state, zero selects the closed form. */
	int numRandomisations;
	long randomSeed;

	int savitzkyGolayWindow;
	int savitzkyGolayPolynomial;
	double peakProminence;
	int maxLocalShellRadius;

	/** Roi voxels, sorted by slice so that distant slices can be skipped. */
	private int[] voxelX;
	private int[] voxelY;
	private int[] voxelZ;
	private int[] voxelLevel;
	private int[] sliceStart;
	private boolean[] isReferenceVoxel;

	private int roiVoxelCount;
	private int[] levelCounts;
	private int totalReferenceCount;

	/** Shell index of a squared lattice distance, i.e. floor(sqrt(d2)). */
	private int[] shellOfSquaredDistance;
	private double[] idealShellVolume;

	/** g[r][alpha][beta], r running from 1 to maxRadius. */
	private double[][][] rdf;
	private double[][][] rdfRandom;
	/** Number of roi voxels found in shell r, summed over all reference voxels. */
	private double[] shellOccupancy;

	private EnumMap<GLAMMatrixType, double[][]> matrices;

	private static final double LOG_FLOOR = 1e-9;
	private static final double ACTIVE_THRESHOLD = 1e-6;

	public GLAMFeatures(ImagePlus img, ImagePlus mask, Map<String, Object> settings) {
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
	 * @param img         gray level image
	 * @param mask        roi mask, null creates a mask that covers the whole image
	 * @param label       roi label to analyse
	 * @param useBinCount true for a fixed bin number, false for a fixed bin width
	 * @param nBins       number of bins, null falls back to the global setting
	 * @param binWidth    width of a bin, null falls back to the global setting
	 * @param maxRadius   largest distance in voxels, null falls back to the setting
	 */
	public GLAMFeatures(ImagePlus img, ImagePlus mask, int label, boolean useBinCount, Integer nBins, Double binWidth,
			Integer maxRadius) throws Exception {
		super(img, mask, null);
		this.label = label;

		if (mask == null) {
			mask = ImagePreprocessing.createMask(img.getWidth(), img.getHeight(), img.getNSlices(), null, this.label,
					img.getCalibration().pixelWidth, img.getCalibration().pixelHeight,
					img.getCalibration().pixelDepth);
		}
		this.mask = mask;

		this.nBins = nBins == null ? RadiomicsJ.nBins : nBins;
		this.binWidth = binWidth == null ? RadiomicsJ.binWidth : binWidth;
		this.maxRadius = maxRadius == null ? RadiomicsJ.glamMaxRadius : maxRadius;
		applyGlobalAlgorithmSettings();

		if (RadiomicsJ.discretiseImp != null) {
			discImg = RadiomicsJ.discretiseImp;
		} else if (useBinCount) {
			discImg = Utils.discrete(this.img, this.mask, this.label, this.nBins);
		} else {
			discImg = Utils.discreteByBinWidth(this.img, this.mask, this.label, this.binWidth);
			this.nBins = Utils.getNumOfBinsByMax(discImg, this.mask, this.label);
		}

		compute();

		this.settings.put(RadiomicsFeature.IMAGE, this.img);
		this.settings.put(RadiomicsFeature.MASK, this.mask);
		this.settings.put(RadiomicsFeature.DISC_IMG, this.discImg);
		this.settings.put(RadiomicsFeature.LABEL, this.label);
		this.settings.put(RadiomicsFeature.USE_BIN_COUNT, useBinCount);
		this.settings.put(RadiomicsFeature.nBins, this.nBins);
		this.settings.put(RadiomicsFeature.BinWidth, this.binWidth);
		this.settings.put(RadiomicsFeature.GLAM_MAX_RADIUS, this.maxRadius);
	}

	@Override
	public void buildup(Map<String, Object> settings) {
		Object useBinValue = settings.get(RadiomicsFeature.USE_BIN_COUNT);
		if (useBinValue == null) {
			throw new IllegalArgumentException("'useBinCount:boolean' is missing in settings.");
		}
		if (!(useBinValue instanceof Boolean)) {
			throw new IllegalArgumentException("'useBinCount' must be a Boolean.");
		}
		boolean useBinCount = (Boolean) useBinValue;

		Object nBinsValue = settings.get(RadiomicsFeature.nBins);
		if (nBinsValue == null && useBinCount) {
			throw new IllegalArgumentException("'nBins' is missing in settings.");
		}
		if (nBinsValue != null && !(nBinsValue instanceof Integer)) {
			throw new IllegalArgumentException("'nBins' must be an Integer.");
		}
		this.nBins = nBinsValue == null ? RadiomicsJ.nBins : (Integer) nBinsValue;

		Object binWidthValue = settings.get(RadiomicsFeature.BinWidth);
		if (binWidthValue == null && !useBinCount) {
			throw new IllegalArgumentException("'BinWidth' is missing in settings.");
		}
		if (binWidthValue != null && !(binWidthValue instanceof Double)) {
			throw new IllegalArgumentException("'BinWidth' must be a Double.");
		}
		this.binWidth = binWidthValue == null ? RadiomicsJ.binWidth : (Double) binWidthValue;

		Object radiusValue = settings.get(RadiomicsFeature.GLAM_MAX_RADIUS);
		if (radiusValue != null && !(radiusValue instanceof Integer)) {
			throw new IllegalArgumentException("'GLAM_MAX_RADIUS' must be an Integer.");
		}
		this.maxRadius = radiusValue == null ? RadiomicsJ.glamMaxRadius : (Integer) radiusValue;
		applyGlobalAlgorithmSettings();

		if (mask == null) {
			mask = ImagePreprocessing.createMask(img.getWidth(), img.getHeight(), img.getNSlices(), null, this.label,
					img.getCalibration().pixelWidth, img.getCalibration().pixelHeight,
					img.getCalibration().pixelDepth);
		}

		try {
			if (RadiomicsJ.discretiseImp != null) {
				discImg = RadiomicsJ.discretiseImp;
			} else if (useBinCount) {
				discImg = Utils.discrete(this.img, this.mask, this.label, this.nBins);
			} else {
				discImg = Utils.discreteByBinWidth(this.img, this.mask, this.label, this.binWidth);
				this.nBins = Utils.getNumOfBinsByMax(discImg, this.mask, this.label);
			}
			compute();
		} catch (Exception e) {
			throw new IllegalStateException("GLAMFeatures: could not build the affinity matrices.", e);
		}

		settings.put(RadiomicsFeature.DISC_IMG, this.discImg);
		settings.put(RadiomicsFeature.nBins, this.nBins);
		settings.put(RadiomicsFeature.GLAM_MAX_RADIUS, this.maxRadius);
	}

	private void applyGlobalAlgorithmSettings() {
		this.maxReferenceVoxels = RadiomicsJ.glamMaxReferenceVoxels;
		this.boundaryCorrection = RadiomicsJ.glamBoundaryCorrection;
		this.numRandomisations = RadiomicsJ.glamNumRandomisations;
		this.randomSeed = RadiomicsJ.glamRandomSeed;
		this.savitzkyGolayWindow = RadiomicsJ.glamSavitzkyGolayWindow;
		this.savitzkyGolayPolynomial = RadiomicsJ.glamSavitzkyGolayPolynomial;
		this.peakProminence = RadiomicsJ.glamPeakProminence;
		this.maxLocalShellRadius = RadiomicsJ.glamMaxLocalShellRadius;
		if (this.maxRadius < 2) {
			this.maxRadius = 2;
		}
	}

	// ------------------------------------------------------------------
	// radial distribution function
	// ------------------------------------------------------------------

	private void compute() {
		requireVolume();
		warnWhenVoxelsAreNotIsotropic();
		collectRoiVoxels();
		prepareLookupTables();
		rdf = calculateRadialDistribution(voxelLevel);
		rdfRandom = numRandomisations > 0 ? sampledRandomState() : closedFormRandomState();
		matrices = buildMatrices();
	}

	/**
	 * GLAM is defined on a volume. Every shell is a spherical shell and every
	 * integral carries the 3D volume element, so a single slice would silently
	 * produce numbers that no longer mean what the framework says they mean.
	 */
	private void requireVolume() {
		if (img.getNSlices() < 2) {
			throw new IllegalArgumentException(
					"GLAM is a three dimensional descriptor: it measures a radial distribution over spherical shells. "
							+ "A single slice cannot be analysed, please pass a stack.");
		}
	}

	private void warnWhenVoxelsAreNotIsotropic() {
		double sx = img.getCalibration().pixelWidth;
		double sy = img.getCalibration().pixelHeight;
		double sz = img.getCalibration().pixelDepth;
		double tolerance = 1e-4 * Math.max(sx, Math.max(sy, sz));
		if (Math.abs(sx - sy) > tolerance || Math.abs(sx - sz) > tolerance) {
			IJ.log("GLAM: the voxels are not isotropic (" + sx + ", " + sy + ", " + sz
					+ "). GLAM measures distances on the voxel lattice, so please resample to isotropic voxels first.");
		}
	}

	/**
	 * Collects the discretised roi voxels into flat arrays, ordered by slice.
	 * Gray levels are stored zero based, so that they index the matrices directly.
	 */
	private void collectRoiVoxels() {
		int w = discImg.getWidth();
		int h = discImg.getHeight();
		int s = discImg.getNSlices();

		List<int[]> collected = new ArrayList<>();
		sliceStart = new int[s + 1];
		for (int z = 0; z < s; z++) {
			sliceStart[z] = collected.size();
			ImageProcessor dp = discImg.getStack().getProcessor(z + 1);
			ImageProcessor mp = mask.getStack().getProcessor(z + 1);
			for (int y = 0; y < h; y++) {
				for (int x = 0; x < w; x++) {
					if ((int) mp.getf(x, y) != label) {
						continue;
					}
					float value = dp.getf(x, y);
					if (Float.isNaN(value)) {
						continue;
					}
					int level = (int) value - 1;// discretisation is one based
					if (level < 0) {
						level = 0;
					}
					collected.add(new int[] { x, y, z, level });
				}
			}
		}
		sliceStart[s] = collected.size();

		roiVoxelCount = collected.size();
		if (roiVoxelCount == 0) {
			throw new IllegalStateException("GLAMFeatures: the roi does not contain any voxel with label " + label);
		}

		voxelX = new int[roiVoxelCount];
		voxelY = new int[roiVoxelCount];
		voxelZ = new int[roiVoxelCount];
		voxelLevel = new int[roiVoxelCount];
		int maxLevel = 0;
		for (int i = 0; i < roiVoxelCount; i++) {
			int[] v = collected.get(i);
			voxelX[i] = v[0];
			voxelY[i] = v[1];
			voxelZ[i] = v[2];
			voxelLevel[i] = v[3];
			maxLevel = Math.max(maxLevel, v[3]);
		}
		if (maxLevel + 1 > nBins) {
			nBins = maxLevel + 1;
		}

		levelCounts = new int[nBins];
		for (int i = 0; i < roiVoxelCount; i++) {
			levelCounts[voxelLevel[i]]++;
		}
		selectReferenceVoxels();
	}

	/**
	 * Marks the voxels that act as the centre of a radial distribution. Every roi
	 * voxel is used unless the sub sampling limit is set, in which case an evenly
	 * spread subset of each gray level is taken.
	 */
	private void selectReferenceVoxels() {
		isReferenceVoxel = new boolean[roiVoxelCount];
		if (maxReferenceVoxels <= 0) {
			java.util.Arrays.fill(isReferenceVoxel, true);
			totalReferenceCount = roiVoxelCount;
			return;
		}
		int[] seen = new int[nBins];
		for (int i = 0; i < roiVoxelCount; i++) {
			int level = voxelLevel[i];
			int total = levelCounts[level];
			if (total <= maxReferenceVoxels) {
				isReferenceVoxel[i] = true;
			} else {
				// keep an evenly spread subset, so that the choice is reproducible
				long before = (long) seen[level] * maxReferenceVoxels / total;
				long after = (long) (seen[level] + 1) * maxReferenceVoxels / total;
				isReferenceVoxel[i] = after > before;
			}
			seen[level]++;
			if (isReferenceVoxel[i]) {
				totalReferenceCount++;
			}
		}
	}

	private void prepareLookupTables() {
		int limit = (maxRadius + 1) * (maxRadius + 1);
		shellOfSquaredDistance = new int[limit];
		for (int d2 = 0; d2 < limit; d2++) {
			shellOfSquaredDistance[d2] = (int) Math.floor(Math.sqrt(d2));
		}
		idealShellVolume = new double[maxRadius + 1];
		for (int r = 1; r <= maxRadius; r++) {
			double outer = r + 0.5;
			double inner = r - 0.5;
			idealShellVolume[r] = (4d / 3d) * Math.PI * (outer * outer * outer - inner * inner * inner);
		}
	}

	/**
	 * Per shell pair counts, accumulated over the reference voxels.
	 */
	private static final class ShellCounts {
		final double[][][] pairs;// [r][alpha][beta], raw neighbour counts
		final double[][][] ratios;// [r][alpha][beta], neighbour counts over shell occupancy
		final double[][] validReferences;// [r][alpha]
		final double[] occupancy;// [r], roi voxels seen in the shell

		ShellCounts(int maxRadius, int levels) {
			pairs = new double[maxRadius + 1][levels][levels];
			ratios = new double[maxRadius + 1][levels][levels];
			validReferences = new double[maxRadius + 1][levels];
			occupancy = new double[maxRadius + 1];
		}

		void add(ShellCounts other) {
			for (int r = 1; r < pairs.length; r++) {
				occupancy[r] += other.occupancy[r];
				for (int a = 0; a < pairs[r].length; a++) {
					validReferences[r][a] += other.validReferences[r][a];
					for (int b = 0; b < pairs[r][a].length; b++) {
						pairs[r][a][b] += other.pairs[r][a][b];
						ratios[r][a][b] += other.ratios[r][a][b];
					}
				}
			}
		}
	}

	/**
	 * The radial distribution function of one labelling of the roi.
	 *
	 * @param levelOfVoxel gray level of every roi voxel, zero based
	 * @return g[r][alpha][beta] with r from 1 to maxRadius
	 */
	private double[][][] calculateRadialDistribution(int[] levelOfVoxel) {
		final int workers = Math.max(1, Math.min(Runtime.getRuntime().availableProcessors(), roiVoxelCount));
		List<ShellCounts> partial = IntStream.range(0, workers).parallel().mapToObj(worker -> {
			ShellCounts local = new ShellCounts(maxRadius, nBins);
			int from = (int) ((long) roiVoxelCount * worker / workers);
			int to = (int) ((long) roiVoxelCount * (worker + 1) / workers);
			scanNeighbourhoods(from, to, levelOfVoxel, local);
			return local;
		}).collect(java.util.stream.Collectors.toList());

		ShellCounts merged = new ShellCounts(maxRadius, nBins);
		for (ShellCounts local : partial) {
			merged.add(local);
		}

		shellOccupancy = merged.occupancy;
		double[][][] g = new double[maxRadius + 1][nBins][nBins];
		int[] counts = new int[nBins];
		int[] references = new int[nBins];
		for (int i = 0; i < roiVoxelCount; i++) {
			counts[levelOfVoxel[i]]++;
			if (isReferenceVoxel[i]) {
				references[levelOfVoxel[i]]++;
			}
		}
		for (int r = 1; r <= maxRadius; r++) {
			for (int alpha = 0; alpha < nBins; alpha++) {
				for (int beta = 0; beta < nBins; beta++) {
					if (counts[alpha] == 0 || counts[beta] == 0) {
						/*
						 * A gray level that does not occur in the roi has no reference voxels to
						 * measure from and a density of zero to divide by, so the whole row and the
						 * whole column are undefined. Reporting zero here would be reporting "no
						 * affinity", which is a measurement, not the absence of one.
						 */
						g[r][alpha][beta] = Double.NaN;
						continue;
					}
					double densityOfBeta = counts[beta] / (double) roiVoxelCount;
					if (boundaryCorrection) {
						double valid = merged.validReferences[r][alpha];
						if (valid > 0d) {
							g[r][alpha][beta] = (merged.ratios[r][alpha][beta] / valid) / densityOfBeta;
						}
					} else if (references[alpha] > 0) {
						g[r][alpha][beta] = (merged.pairs[r][alpha][beta] / idealShellVolume[r]) / references[alpha]
								/ densityOfBeta;
					}
				}
			}
		}
		return g;
	}

	/**
	 * Counts, for every reference voxel in the given range, how many roi voxels of
	 * each gray level sit in each distance shell around it.
	 */
	private void scanNeighbourhoods(int from, int to, int[] levelOfVoxel, ShellCounts out) {
		final int levels = nBins;
		final int radius = maxRadius;
		final int squaredLimit = (radius + 1) * (radius + 1);
		final int slices = sliceStart.length - 1;
		int[] neighboursPerShell = new int[(radius + 1) * levels];
		int[] occupancyPerShell = new int[radius + 1];
		int[] touchedShells = new int[radius + 1];

		for (int i = from; i < to; i++) {
			if (!isReferenceVoxel[i]) {
				continue;
			}
			final int xi = voxelX[i];
			final int yi = voxelY[i];
			final int zi = voxelZ[i];
			final int alpha = levelOfVoxel[i];

			int touched = 0;
			int jFrom = sliceStart[Math.max(0, zi - radius)];
			int jTo = sliceStart[Math.min(slices, zi + radius + 1)];
			for (int j = jFrom; j < jTo; j++) {
				int dz = voxelZ[j] - zi;
				int dy = voxelY[j] - yi;
				int dx = voxelX[j] - xi;
				int squared = dz * dz + dy * dy + dx * dx;
				if (squared == 0 || squared >= squaredLimit) {
					continue;
				}
				int shell = shellOfSquaredDistance[squared];
				if (occupancyPerShell[shell]++ == 0) {
					touchedShells[touched++] = shell;
				}
				neighboursPerShell[shell * levels + levelOfVoxel[j]]++;
			}

			for (int t = 0; t < touched; t++) {
				int shell = touchedShells[t];
				int occupancy = occupancyPerShell[shell];
				double[] pairRow = out.pairs[shell][alpha];
				double[] ratioRow = out.ratios[shell][alpha];
				int base = shell * levels;
				for (int beta = 0; beta < levels; beta++) {
					int count = neighboursPerShell[base + beta];
					if (count == 0) {
						continue;
					}
					pairRow[beta] += count;
					ratioRow[beta] += count / (double) occupancy;
					neighboursPerShell[base + beta] = 0;
				}
				out.validReferences[shell][alpha]++;
				out.occupancy[shell] += occupancy;
				occupancyPerShell[shell] = 0;
			}
		}
	}

	/**
	 * The randomised reference state in closed form. See the class comment for the
	 * derivation.
	 */
	private double[][][] closedFormRandomState() {
		double[][][] g = new double[maxRadius + 1][nBins][nBins];
		double n = roiVoxelCount;
		if (n < 2) {
			return g;
		}
		for (int r = 1; r <= maxRadius; r++) {
			double availability = boundaryCorrection ? 1d
					: shellOccupancy[r] / (totalReferenceCount * idealShellVolume[r]);
			for (int alpha = 0; alpha < nBins; alpha++) {
				if (levelCounts[alpha] == 0) {
					java.util.Arrays.fill(g[r][alpha], Double.NaN);
					continue;
				}
				for (int beta = 0; beta < nBins; beta++) {
					if (levelCounts[beta] == 0) {
						g[r][alpha][beta] = Double.NaN;
						continue;
					}
					double correction;
					if (alpha == beta) {
						// a voxel is never counted as its own neighbour
						correction = n * (levelCounts[alpha] - 1d) / (levelCounts[alpha] * (n - 1d));
					} else {
						correction = n / (n - 1d);
					}
					g[r][alpha][beta] = availability * correction;
				}
			}
		}
		return g;
	}

	/**
	 * The randomised reference state estimated by shuffling the gray levels over
	 * the roi, as the original framework does.
	 */
	private double[][][] sampledRandomState() {
		double[] savedOccupancy = shellOccupancy;
		double[][][] mean = new double[maxRadius + 1][nBins][nBins];
		int[] shuffled = voxelLevel.clone();
		Random random = new Random(randomSeed);
		for (int run = 0; run < numRandomisations; run++) {
			for (int i = shuffled.length - 1; i > 0; i--) {
				int j = random.nextInt(i + 1);
				int tmp = shuffled[i];
				shuffled[i] = shuffled[j];
				shuffled[j] = tmp;
			}
			double[][][] g = calculateRadialDistribution(shuffled);
			for (int r = 1; r <= maxRadius; r++) {
				for (int alpha = 0; alpha < nBins; alpha++) {
					for (int beta = 0; beta < nBins; beta++) {
						mean[r][alpha][beta] += g[r][alpha][beta] / numRandomisations;
					}
				}
			}
		}
		shellOccupancy = savedOccupancy;
		return mean;
	}

	// ------------------------------------------------------------------
	// affinity matrices
	// ------------------------------------------------------------------

	/**
	 * True when both gray levels actually occur in the roi. A gray level that does
	 * not occur has no reference voxels to measure from and a density of zero to
	 * divide by, so every quantity involving it is undefined rather than zero.
	 */
	private boolean isDefined(int alpha, int beta) {
		return levelCounts[alpha] > 0 && levelCounts[beta] > 0;
	}

	private double[] curveOf(double[][][] g, int alpha, int beta) {
		double[] curve = new double[maxRadius];
		for (int r = 1; r <= maxRadius; r++) {
			curve[r - 1] = g[r][alpha][beta];
		}
		return curve;
	}

	private double[] distances() {
		double[] r = new double[maxRadius];
		for (int i = 0; i < maxRadius; i++) {
			r[i] = i + 1;
		}
		return r;
	}

	private EnumMap<GLAMMatrixType, double[][]> buildMatrices() {
		EnumMap<GLAMMatrixType, double[][]> result = new EnumMap<>(GLAMMatrixType.class);
		for (GLAMMatrixType type : GLAMMatrixType.values()) {
			result.put(type, filled(Double.NaN));
		}
		double[] r = distances();

		double[][] potentialEnergy = result.get(GLAMMatrixType.PotentialEnergy);
		double[][] virial = result.get(GLAMMatrixType.SecondVirialCoefficient);
		double[][] compressibility = result.get(GLAMMatrixType.Compressibility);
		double[][] coordination = result.get(GLAMMatrixType.CoordinationNumber);
		double[][] packing = result.get(GLAMMatrixType.LocalPackingFraction);
		double[][] pressure = result.get(GLAMMatrixType.StructuralPressureIndex);
		double[][] disorder = result.get(GLAMMatrixType.ConfigurationalDisorderIndex);
		double[][] wasserstein = result.get(GLAMMatrixType.WassersteinDistance);
		double[][] decayRate = result.get(GLAMMatrixType.InverseCorrelationLength);
		double[][] peakPosition = result.get(GLAMMatrixType.RDFPeakPosition);
		double[][] dispersion = result.get(GLAMMatrixType.RDFDispersionRatio);
		double[][] logPeak = result.get(GLAMMatrixType.LogRDFPeakHeight);
		double[][] logMedian = result.get(GLAMMatrixType.LogRDFMedian);
		double[][] logVariance = result.get(GLAMMatrixType.LogRDFVariance);
		double[][] logSkewness = result.get(GLAMMatrixType.LogRDFSkewness);
		double[][] logKurtosis = result.get(GLAMMatrixType.LogRDFKurtosis);

		/*
		 * Every row of every matrix is derived from one g(alpha, beta, r) curve on its
		 * own, and each row writes only into its own cells, so the rows can be built
		 * side by side. It is worth it: each cell runs a smoothing pass, a peak search
		 * and a non linear fit, which dominates the cost on small rois.
		 */
		IntStream.range(0, nBins).parallel().forEach(alpha -> {
			double densityOfAlpha = levelCounts[alpha] / (double) roiVoxelCount;
			for (int beta = 0; beta < nBins; beta++) {
				if (!isDefined(alpha, beta)) {
					// leave every matrix at NaN for a gray level the roi does not contain
					continue;
				}
				double densityOfBeta = levelCounts[beta] / (double) roiVoxelCount;
				double[] g = curveOf(rdf, alpha, beta);
				double[] gRandom = curveOf(rdfRandom, alpha, beta);

				shapeStatistics(g, r, alpha, beta, peakPosition, dispersion, logPeak, logMedian, logVariance,
						logSkewness, logKurtosis);
				virial[alpha][beta] = secondVirialCoefficient(g, gRandom, r);
				potentialEnergy[alpha][beta] = potentialOfMeanForceEnergy(g, r);
				pressure[alpha][beta] = structuralPressureIndex(g, r, densityOfAlpha, densityOfBeta);
				disorder[alpha][beta] = configurationalDisorderIndex(g, gRandom);
				wasserstein[alpha][beta] = wassersteinDistance(g, gRandom, r, densityOfBeta);
				decayRate[alpha][beta] = inverseCorrelationLength(g, gRandom, r);

				int shellEnd = firstCoordinationShell(g, 15);
				if (shellEnd >= 0) {
					double number = coordinationNumber(g, r, densityOfBeta, shellEnd);
					coordination[alpha][beta] = number;
					double boundary = r[shellEnd] + 0.5;
					double shellVolume = (4d / 3d) * Math.PI * boundary * boundary * boundary;
					packing[alpha][beta] = shellVolume > 0d ? number / shellVolume : Double.NaN;
				}
				if (alpha == beta) {
					compressibility[alpha][alpha] = isothermalCompressibility(g, r);
				}
			}
		});

		result.put(GLAMMatrixType.PhenotypicDistance, phenotypicDistance(r));
		result.put(GLAMMatrixType.AssemblyCoupling, assemblyCoupling(wasserstein));
		result.put(GLAMMatrixType.FrustrationIndex, frustrationIndex(pressure, disorder));
		return result;
	}

	private double[][] filled(double value) {
		double[][] m = new double[nBins][nBins];
		for (double[] row : m) {
			java.util.Arrays.fill(row, value);
		}
		return m;
	}

	/**
	 * Statistics of the shape of one g(r) curve. Distances beyond the reach of the
	 * roi contribute nothing, so they are excluded from the moments.
	 */
	private void shapeStatistics(double[] g, double[] r, int alpha, int beta, double[][] peakPosition,
			double[][] dispersion, double[][] logPeak, double[][] logMedian, double[][] logVariance,
			double[][] logSkewness, double[][] logKurtosis) {
		if (g.length == 0) {
			return;
		}
		peakPosition[alpha][beta] = r[GLAMNumerics.argMax(g, 0, g.length)];

		int activeCount = 0;
		for (double value : g) {
			if (value > ACTIVE_THRESHOLD) {
				activeCount++;
			}
		}
		double[] active = new double[activeCount];
		double[] activeLog = new double[activeCount];
		int k = 0;
		double logMax = Double.NEGATIVE_INFINITY;
		for (double value : g) {
			double logValue = Math.log1p(value);
			logMax = Math.max(logMax, logValue);
			if (value > ACTIVE_THRESHOLD) {
				active[k] = value;
				activeLog[k] = logValue;
				k++;
			}
		}
		logPeak[alpha][beta] = logMax;

		if (activeCount > 2) {
			double meanValue = GLAMNumerics.mean(active);
			dispersion[alpha][beta] = meanValue > 0d ? GLAMNumerics.variance(active) / meanValue : 0d;
			logMedian[alpha][beta] = GLAMNumerics.median(activeLog);
			logVariance[alpha][beta] = GLAMNumerics.variance(activeLog);
			logSkewness[alpha][beta] = GLAMNumerics.skewness(activeLog);
			logKurtosis[alpha][beta] = GLAMNumerics.kurtosis(activeLog);
		}
	}

	/**
	 * Net affinity between two gray levels. Negative values mean the two levels
	 * attract each other more than chance would explain.
	 */
	private double secondVirialCoefficient(double[] g, double[] gRandom, double[] r) {
		double[] integrand = new double[g.length];
		for (int i = 0; i < g.length; i++) {
			integrand[i] = (g[i] - gRandom[i]) * r[i] * r[i];
		}
		return -2d * Math.PI * GLAMNumerics.trapezoid(integrand);
	}

	/**
	 * Energy of the potential of mean force, W(r) = -ln g(r), weighted by g(r).
	 */
	private double potentialOfMeanForceEnergy(double[] g, double[] r) {
		double[] integrand = new double[g.length];
		for (int i = 0; i < g.length; i++) {
			double meanForcePotential = -Math.log(g[i] + LOG_FLOOR);
			integrand[i] = meanForcePotential * g[i] * r[i] * r[i];
		}
		return 4d * Math.PI * GLAMNumerics.trapezoid(integrand);
	}

	/**
	 * Susceptibility of the arrangement to density fluctuations. Only the distances
	 * the roi actually reaches are integrated.
	 */
	private double isothermalCompressibility(double[] g, double[] r) {
		int count = 0;
		for (double value : g) {
			if (value > ACTIVE_THRESHOLD) {
				count++;
			}
		}
		if (count <= 1) {
			return 0d;
		}
		double[] integrand = new double[count];
		double[] abscissa = new double[count];
		int k = 0;
		for (int i = 0; i < g.length; i++) {
			if (g[i] > ACTIVE_THRESHOLD) {
				integrand[k] = (g[i] - 1d) * r[i] * r[i];
				abscissa[k] = r[i];
				k++;
			}
		}
		return 4d * Math.PI * GLAMNumerics.trapezoid(integrand, abscissa);
	}

	/**
	 * Last index of the first coordination shell, that is the first minimum of g(r)
	 * beyond its first peak. Returns a negative value when no shell can be found.
	 */
	private int firstCoordinationShell(double[] g, int fallbackSearchRange) {
		double[] smooth = GLAMNumerics.savitzkyGolay(g, savitzkyGolayWindow, savitzkyGolayPolynomial);
		int[] peaks = GLAMNumerics.findPeaks(smooth, peakProminence);
		int peakIndex;
		if (peaks.length == 0) {
			int searchRange = Math.min(smooth.length, fallbackSearchRange);
			if (searchRange == 0) {
				return -1;
			}
			peakIndex = GLAMNumerics.argMax(smooth, 0, searchRange);
			if (peakIndex == 0) {
				return -1;
			}
		} else {
			peakIndex = peaks[0];
		}
		double[] inverted = new double[smooth.length - peakIndex];
		for (int i = 0; i < inverted.length; i++) {
			inverted[i] = -smooth[peakIndex + i];
		}
		int[] minima = GLAMNumerics.localMaxima(inverted);
		if (minima.length == 0) {
			int guess = Math.min(peakIndex * 2, smooth.length - 1);
			return guess <= peakIndex ? smooth.length - 1 : guess;
		}
		return peakIndex + minima[0];
	}

	/**
	 * Number of neighbours of gray level beta inside the first coordination shell
	 * around a voxel of gray level alpha.
	 */
	private double coordinationNumber(double[] g, double[] r, double densityOfBeta, int shellEnd) {
		double[] integrand = new double[shellEnd + 1];
		for (int i = 0; i <= shellEnd; i++) {
			integrand[i] = g[i] * r[i] * r[i];
		}
		return 4d * Math.PI * densityOfBeta * GLAMNumerics.trapezoid(integrand);
	}

	/**
	 * Pressure like descriptor built from the gradient of the potential of mean
	 * force, in analogy with the virial equation of state.
	 */
	private double structuralPressureIndex(double[] g, double[] r, double densityOfAlpha, double densityOfBeta) {
		if (g.length < 2) {
			return Double.NaN;
		}
		double[] smooth = GLAMNumerics.savitzkyGolay(g, savitzkyGolayWindow, savitzkyGolayPolynomial);
		double[] meanForcePotential = new double[smooth.length];
		for (int i = 0; i < smooth.length; i++) {
			meanForcePotential[i] = -Math.log(Math.max(smooth[i], LOG_FLOOR));
		}
		double[] force = GLAMNumerics.gradient(meanForcePotential);
		double[] integrand = new double[g.length];
		for (int i = 0; i < g.length; i++) {
			integrand[i] = r[i] * force[i] * g[i] * r[i] * r[i];
		}
		return -(densityOfAlpha * densityOfBeta / 6d) * 4d * Math.PI * GLAMNumerics.trapezoid(integrand);
	}

	/**
	 * How much of the observed ordering survives once the randomised arrangement is
	 * discounted, averaged over the first coordination shell and weighted by how
	 * much the two states actually differ.
	 */
	private double configurationalDisorderIndex(double[] g, double[] gRandom) {
		double[] smooth = GLAMNumerics.savitzkyGolay(g, savitzkyGolayWindow, savitzkyGolayPolynomial);
		double[] smoothRandom = GLAMNumerics.savitzkyGolay(gRandom, savitzkyGolayWindow, savitzkyGolayPolynomial);
		int shellEnd = firstCoordinationShell(g, maxLocalShellRadius);
		if (shellEnd < 0) {
			return Double.NaN;
		}
		int end = Math.max(shellEnd, 1);
		double weightedSum = 0d;
		double weightSum = 0d;
		for (int i = 0; i < end && i < smooth.length; i++) {
			double logStructured = Math.log(Math.max(smooth[i], LOG_FLOOR));
			double logRandom = Math.log(Math.max(smoothRandom[i], LOG_FLOOR));
			double difference = logStructured - logRandom;
			double index = logStructured / difference;
			if (!Double.isFinite(index)) {
				continue;
			}
			double weight = Math.abs(difference);
			weightedSum += index * weight;
			weightSum += weight;
		}
		return weightSum > 1e-9 ? weightedSum / weightSum : Double.NaN;
	}

	/**
	 * Transport distance between the cumulative neighbour counts of the observed
	 * and the randomised arrangement, the assembly cost of the texture.
	 */
	private double wassersteinDistance(double[] g, double[] gRandom, double[] r, double densityOfBeta) {
		double[] structured = new double[g.length];
		double[] random = new double[g.length];
		for (int i = 0; i < g.length; i++) {
			double shell = 4d * Math.PI * densityOfBeta * r[i] * r[i];
			structured[i] = g[i] * shell;
			random[i] = gRandom[i] * shell;
		}
		double[] cumulativeStructured = GLAMNumerics.cumulativeTrapezoid(structured);
		double[] cumulativeRandom = GLAMNumerics.cumulativeTrapezoid(random);
		double[] gap = new double[g.length];
		for (int i = 0; i < g.length; i++) {
			gap[i] = Math.abs(cumulativeStructured[i] - cumulativeRandom[i]);
		}
		return GLAMNumerics.trapezoid(gap);
	}

	/**
	 * Decay rate of the spatial correlation that remains after the randomised
	 * arrangement is subtracted, i.e. the reciprocal of the correlation length.
	 */
	private double inverseCorrelationLength(double[] g, double[] gRandom, double[] r) {
		double[] excess = new double[g.length];
		for (int i = 0; i < g.length; i++) {
			excess[i] = g[i] - gRandom[i];
		}
		double[] smooth = GLAMNumerics.savitzkyGolay(excess, savitzkyGolayWindow, savitzkyGolayPolynomial);
		int searchRange = r.length / 2;
		if (searchRange < 3) {
			return Double.NaN;
		}
		int peakIndex = 0;
		double best = Math.abs(smooth[0]);
		for (int i = 1; i < searchRange; i++) {
			if (Math.abs(smooth[i]) > best) {
				best = Math.abs(smooth[i]);
				peakIndex = i;
			}
		}
		if (peakIndex + 3 >= r.length) {
			return Double.NaN;
		}
		int length = r.length - peakIndex;
		double[] x = new double[length];
		double[] y = new double[length];
		for (int i = 0; i < length; i++) {
			x[i] = r[peakIndex + i];
			y[i] = Math.abs(smooth[peakIndex + i]);
		}
		return GLAMNumerics.fitExponentialDecayRate(x, y, Math.abs(smooth[peakIndex]), 0.3);
	}

	/**
	 * Transport distance between the self correlation profiles of two gray levels.
	 * It compares how each level is packed in space, independently of its
	 * intensity, and is a true distance: symmetric, non negative and zero on the
	 * diagonal.
	 */
	private double[][] phenotypicDistance(double[] r) {
		double[][] cumulative = new double[nBins][];
		for (int alpha = 0; alpha < nBins; alpha++) {
			cumulative[alpha] = new double[r.length];
			if (levelCounts[alpha] == 0) {
				continue;
			}
			double density = levelCounts[alpha] / (double) roiVoxelCount;
			double[] g = curveOf(rdf, alpha, alpha);
			double[] integrand = new double[g.length];
			for (int i = 0; i < g.length; i++) {
				integrand[i] = 4d * Math.PI * density * g[i] * r[i] * r[i];
			}
			double[] profile = GLAMNumerics.cumulativeTrapezoid(integrand);
			double total = profile[profile.length - 1];
			if (total > 1e-9) {
				for (int i = 0; i < profile.length; i++) {
					cumulative[alpha][i] = profile[i] / total;
				}
			}
		}
		double[][] distance = filled(Double.NaN);
		double[] gap = new double[r.length];
		for (int alpha = 0; alpha < nBins; alpha++) {
			for (int beta = 0; beta < nBins; beta++) {
				if (!isDefined(alpha, beta)) {
					// a gray level that does not occur has no spatial topology to compare
					continue;
				}
				for (int i = 0; i < r.length; i++) {
					gap[i] = Math.abs(cumulative[alpha][i] - cumulative[beta][i]);
				}
				distance[alpha][beta] = GLAMNumerics.trapezoid(gap);
			}
		}
		return distance;
	}

	/**
	 * Mixed second derivative of the assembly cost over the two gray levels: how
	 * much building one gray level interferes with building the other.
	 */
	private double[][] assemblyCoupling(double[][] wasserstein) {
		/*
		 * The undefined entries stay undefined on purpose. Substituting zero for a
		 * gray level the roi does not contain would let that fabricated zero flow into
		 * the differences of its neighbours, which are perfectly good gray levels, and
		 * quietly change their coupling.
		 */
		double[][] alongAlpha = new double[nBins][nBins];
		for (int beta = 0; beta < nBins; beta++) {
			double[] column = new double[nBins];
			for (int alpha = 0; alpha < nBins; alpha++) {
				column[alpha] = wasserstein[alpha][beta];
			}
			double[] derivative = firstOrderGradient(column);
			for (int alpha = 0; alpha < nBins; alpha++) {
				alongAlpha[alpha][beta] = derivative[alpha];
			}
		}
		double[][] coupling = new double[nBins][nBins];
		for (int alpha = 0; alpha < nBins; alpha++) {
			coupling[alpha] = firstOrderGradient(alongAlpha[alpha]);
		}
		return coupling;
	}

	/**
	 * Central differences inside, plain one sided differences at both ends, which
	 * is the default of numpy's gradient and the convention the reference uses for
	 * the coupling matrix.
	 *
	 * A central difference does not read the sample it sits on, so an undefined
	 * sample would otherwise survive as a perfectly finite derivative of a curve
	 * that has a hole exactly there. The derivative at a point where the function
	 * is undefined is undefined, so that case is marked explicitly.
	 */
	private static double[] firstOrderGradient(double[] v) {
		int n = v.length;
		double[] out = new double[n];
		if (n < 2) {
			java.util.Arrays.fill(out, Double.NaN);
			return out;
		}
		for (int i = 1; i < n - 1; i++) {
			out[i] = (v[i + 1] - v[i - 1]) * 0.5;
		}
		out[0] = v[1] - v[0];
		out[n - 1] = v[n - 1] - v[n - 2];
		for (int i = 0; i < n; i++) {
			if (Double.isNaN(v[i])) {
				out[i] = Double.NaN;
			}
		}
		return out;
	}

	/**
	 * Structural stress divided by structural disorder, the ratio that marks the
	 * transition from a jammed to a yielding arrangement.
	 */
	private double[][] frustrationIndex(double[][] pressure, double[][] disorder) {
		double[][] index = filled(Double.NaN);
		for (int alpha = 0; alpha < nBins; alpha++) {
			for (int beta = 0; beta < nBins; beta++) {
				double stress = pressure[alpha][beta];
				double chaos = disorder[alpha][beta];
				if (!Double.isNaN(stress) && !Double.isNaN(chaos)) {
					index[alpha][beta] = stress / (chaos + 1e-6);
				}
			}
		}
		return index;
	}

	// ------------------------------------------------------------------
	// feature access
	// ------------------------------------------------------------------

	public Double calculate(String id) {
		String name = GLAMFeatureType.findType(id);
		if (name == null) {
			return null;
		}
		GLAMFeatureType type = GLAMFeatureType.valueOf(name);
		double[][] matrix = matrices.get(type.matrix());
		if (matrix == null) {
			return null;
		}
		return reduce(matrix, type.statistic());
	}

	/**
	 * The affinity matrix itself, for callers that want to inspect or visualise it.
	 * Element (alpha, beta) belongs to the gray levels alpha + 1 and beta + 1 of
	 * the discretised image.
	 */
	public double[][] getMatrix(GLAMMatrixType type) {
		double[][] matrix = matrices.get(type);
		if (matrix == null) {
			return null;
		}
		double[][] copy = new double[matrix.length][];
		for (int i = 0; i < matrix.length; i++) {
			copy[i] = matrix[i].clone();
		}
		return copy;
	}

	/**
	 * The radial distribution function, indexed as g[r][alpha][beta] with r running
	 * from 1 to the maximum radius. Index zero of the first dimension is unused.
	 */
	public double[][][] getRadialDistributionFunction() {
		return rdf;
	}

	/**
	 * The randomised reference state, indexed like
	 * {@link #getRadialDistributionFunction()}.
	 */
	public double[][][] getRandomRadialDistributionFunction() {
		return rdfRandom;
	}

	public int getMaxRadius() {
		return maxRadius;
	}

	public int getNumberOfBins() {
		return nBins;
	}

	private Double reduce(double[][] matrix, GLAMStatistic statistic) {
		switch (statistic) {
		case DiagonalMean: {
			double[] diagonal = collect(matrix, Selection.DIAGONAL);
			return diagonal.length == 0 ? Double.NaN : GLAMNumerics.mean(diagonal);
		}
		case OffDiagonalMean: {
			double[] offDiagonal = collect(matrix, Selection.OFF_DIAGONAL);
			return offDiagonal.length == 0 ? Double.NaN : GLAMNumerics.mean(offDiagonal);
		}
		default:
			break;
		}
		double[] values = collect(matrix, Selection.ALL);
		if (values.length == 0) {
			return Double.NaN;
		}
		switch (statistic) {
		case Mean:
			return GLAMNumerics.mean(values);
		case Variance:
			return GLAMNumerics.variance(values);
		case Skewness:
			return GLAMNumerics.skewness(values);
		case Kurtosis:
			return GLAMNumerics.kurtosis(values);
		case Minimum: {
			double min = Double.POSITIVE_INFINITY;
			for (double v : values) {
				min = Math.min(min, v);
			}
			return min;
		}
		case Maximum: {
			double max = Double.NEGATIVE_INFINITY;
			for (double v : values) {
				max = Math.max(max, v);
			}
			return max;
		}
		default:
			return Double.NaN;
		}
	}

	private enum Selection {
		ALL, DIAGONAL, OFF_DIAGONAL
	}

	/**
	 * Flattens the requested part of a matrix, dropping the elements that are not
	 * defined.
	 */
	private double[] collect(double[][] matrix, Selection selection) {
		double[] buffer = new double[matrix.length * matrix.length];
		int found = 0;
		for (int alpha = 0; alpha < matrix.length; alpha++) {
			for (int beta = 0; beta < matrix.length; beta++) {
				if (selection == Selection.DIAGONAL && alpha != beta) {
					continue;
				}
				if (selection == Selection.OFF_DIAGONAL && alpha == beta) {
					continue;
				}
				double value = matrix[alpha][beta];
				if (Double.isNaN(value) || Double.isInfinite(value)) {
					continue;
				}
				buffer[found++] = value;
			}
		}
		double[] out = new double[found];
		System.arraycopy(buffer, 0, out, 0, found);
		return out;
	}

	@Override
	public Set<String> getAvailableFeatures() {
		Set<String> names = new LinkedHashSet<String>();
		for (GLAMFeatureType t : GLAMFeatureType.values()) {
			names.add(t.name());
		}
		return names;
	}

	@Override
	public String getFeatureFamilyName() {
		return "GLAM";
	}

	@Override
	public Map<String, Object> getSettings() {
		return settings;
	}

	/**
	 * Human readable dump of one affinity matrix.
	 */
	public String toString(double[][] matrix) {
		StringBuilder sb = new StringBuilder();
		for (double[] row : matrix) {
			for (double value : row) {
				sb.append(IJ.d2s(value, 4));
				sb.append(" ");
			}
			sb.append("\n");
		}
		return sb.toString();
	}
}
