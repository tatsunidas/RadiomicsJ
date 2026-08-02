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
package radiomics;

import java.util.Random;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.FloatProcessor;
import io.github.tatsunidas.radiomics.features.GLAMFeatures;
import io.github.tatsunidas.radiomics.features.GLAMMatrixType;
import io.github.tatsunidas.radiomics.features.GLCMFeatureType;
import io.github.tatsunidas.radiomics.features.GLCMFeatures;
import io.github.tatsunidas.radiomics.main.RadiomicsJ;

/**
 * A worked example of what GLAM sees that the conventional families do not.
 *
 * Three digital phantoms are built with the very same intensity histogram, so
 * every first order feature is identical by construction. What differs is only
 * the length scale on which the gray levels are organised.
 *
 * <ul>
 * <li><b>salt and pepper</b>: the gray levels are scattered voxel by voxel</li>
 * <li><b>blocks</b>: the same gray levels are grouped into 8 voxel blocks</li>
 * <li><b>onion</b>: the same gray levels form concentric shells</li>
 * </ul>
 *
 * The GLCM separates the scattered phantom from the two organised ones, because
 * it looks one voxel ahead. It cannot say much about how blocks differ from
 * shells, because that difference only appears several voxels away. The radial
 * distribution function of GLAM covers every distance at once, so it does.
 *
 * Run it with
 *
 * <pre>
 * mvn -o exec:java -Dexec.mainClass=radiomics.GLAMExample -Dexec.classpathScope=test
 * </pre>
 *
 * @author tatsunidas <t_kobayashi@vis-ionary.com>
 *         (implemented with the coding assistant Claude, Anthropic)
 */
public class GLAMExample {

	private static final int SIZE = 32;
	private static final int LEVELS = 4;
	private static final int MAX_RADIUS = 16;

	public static void main(String[] args) throws Exception {
		int[][] phantoms = { saltAndPepper(), blocks(), onion() };
		String[] names = { "salt and pepper", "blocks", "onion" };

		System.out.println("Three phantoms of " + SIZE + "^3 voxels, " + LEVELS + " gray levels.");
		System.out.println();
		printHistograms(names, phantoms);
		System.out.println();
		printGlcm(names, phantoms);
		System.out.println();
		printGlam(names, phantoms);
		System.out.println();
		printRadialProfile(names, phantoms);
	}

	// ------------------------------------------------------------------
	// three arrangements of one and the same histogram
	// ------------------------------------------------------------------

	/** Every level appears equally often, scattered voxel by voxel. */
	private static int[] saltAndPepper() {
		int[] levels = equalCounts();
		shuffle(levels, new Random(20260802L));
		return levels;
	}

	/** The same counts, but constant over 8 voxel blocks. */
	private static int[] blocks() {
		int block = 8;
		int perAxis = SIZE / block;
		int[] blockLevel = new int[perAxis * perAxis * perAxis];
		for (int i = 0; i < blockLevel.length; i++) {
			blockLevel[i] = i % LEVELS;
		}
		shuffle(blockLevel, new Random(20260802L));
		int[] levels = new int[SIZE * SIZE * SIZE];
		for (int z = 0; z < SIZE; z++) {
			for (int y = 0; y < SIZE; y++) {
				for (int x = 0; x < SIZE; x++) {
					int b = (z / block) * perAxis * perAxis + (y / block) * perAxis + (x / block);
					levels[index(x, y, z)] = blockLevel[b];
				}
			}
		}
		return levels;
	}

	/** The same counts again, ordered by distance from the centre. */
	private static int[] onion() {
		double c = (SIZE - 1) / 2d;
		int total = SIZE * SIZE * SIZE;
		Integer[] order = new Integer[total];
		final double[] distance = new double[total];
		for (int z = 0; z < SIZE; z++) {
			for (int y = 0; y < SIZE; y++) {
				for (int x = 0; x < SIZE; x++) {
					int i = index(x, y, z);
					order[i] = i;
					distance[i] = (x - c) * (x - c) + (y - c) * (y - c) + (z - c) * (z - c);
				}
			}
		}
		java.util.Arrays.sort(order, (a, b) -> Double.compare(distance[a], distance[b]));
		int[] levels = new int[total];
		int perLevel = total / LEVELS;
		for (int rank = 0; rank < total; rank++) {
			levels[order[rank]] = Math.min(rank / perLevel, LEVELS - 1);
		}
		return levels;
	}

	private static int[] equalCounts() {
		int total = SIZE * SIZE * SIZE;
		int[] levels = new int[total];
		for (int i = 0; i < total; i++) {
			levels[i] = i % LEVELS;
		}
		return levels;
	}

	private static void shuffle(int[] values, Random random) {
		for (int i = values.length - 1; i > 0; i--) {
			int j = random.nextInt(i + 1);
			int tmp = values[i];
			values[i] = values[j];
			values[j] = tmp;
		}
	}

	private static int index(int x, int y, int z) {
		return (z * SIZE + y) * SIZE + x;
	}

	// ------------------------------------------------------------------
	// reporting
	// ------------------------------------------------------------------

	private static void printHistograms(String[] names, int[][] phantoms) {
		System.out.println("Intensity histogram, identical by construction");
		System.out.printf("%-18s", "");
		for (int level = 0; level < LEVELS; level++) {
			System.out.printf("%12s", "level " + level);
		}
		System.out.println();
		for (int p = 0; p < phantoms.length; p++) {
			int[] counts = new int[LEVELS];
			for (int level : phantoms[p]) {
				counts[level]++;
			}
			System.out.printf("%-18s", names[p]);
			for (int level = 0; level < LEVELS; level++) {
				System.out.printf("%12d", counts[level]);
			}
			System.out.println();
		}
	}

	private static void printGlcm(String[] names, int[][] phantoms) throws Exception {
		System.out.println("GLCM, one voxel apart");
		System.out.printf("%-18s%16s%16s%16s%n", "", "JointEntropy", "Contrast", "Correlation");
		for (int p = 0; p < phantoms.length; p++) {
			RadiomicsJ.resetSettings();
			GLCMFeatures glcm = new GLCMFeatures(image(phantoms[p]), mask(), 1, 1, true, LEVELS, null, null);
			System.out.printf("%-18s%16.4f%16.4f%16.4f%n", names[p],
					glcm.calculate(GLCMFeatureType.JointEntropy.id()),
					glcm.calculate(GLCMFeatureType.Contrast.id()),
					glcm.calculate(GLCMFeatureType.Correlation.id()));
			RadiomicsJ.resetSettings();
		}
	}

	private static void printGlam(String[] names, int[][] phantoms) throws Exception {
		System.out.println("GLAM, every distance up to " + MAX_RADIUS + " voxels");
		System.out.printf("%-18s%18s%18s%18s%18s%n", "", "B2 Minimum", "B2 Maximum", "InvCorrLen Mean",
				"CoordNum DiagMean");
		for (int p = 0; p < phantoms.length; p++) {
			GLAMFeatures glam = glam(phantoms[p]);
			System.out.printf("%-18s%18.2f%18.2f%18.4f%18.3f%n", names[p],
					glam.calculate("SecondVirialCoefficient_Minimum"),
					glam.calculate("SecondVirialCoefficient_Maximum"),
					glam.calculate("InverseCorrelationLength_Mean"),
					glam.calculate("CoordinationNumber_DiagonalMean"));
			RadiomicsJ.resetSettings();
		}
		System.out.println();
		System.out.println("A negative second virial coefficient means the two gray levels attract each other,");
		System.out.println("a positive one means they avoid each other.");
		System.out.println();
		System.out.println("Second virial coefficient of every gray level with itself, B2(a,a)");
		System.out.printf("%-18s", "");
		for (int level = 0; level < LEVELS; level++) {
			System.out.printf("%14s", "level " + level);
		}
		System.out.println();
		for (int p = 0; p < phantoms.length; p++) {
			GLAMFeatures glam = glam(phantoms[p]);
			double[][] virial = glam.getMatrix(GLAMMatrixType.SecondVirialCoefficient);
			System.out.printf("%-18s", names[p]);
			for (int level = 0; level < LEVELS; level++) {
				System.out.printf("%14.1f", virial[level][level]);
			}
			System.out.println();
			RadiomicsJ.resetSettings();
		}
		System.out.println();
		System.out.println("In the onion the innermost level is a compact core, so it attracts itself strongly,");
		System.out.println("while the outermost level is a thin shell whose own voxels sit far apart. Averaging");
		System.out.println("over the diagonal would cancel exactly that structure, so read the levels, or use");
		System.out.println("the Minimum and Maximum statistics, rather than the DiagonalMean.");
	}

	/**
	 * The self affinity curve g(alpha, alpha, r) of the darkest gray level: the
	 * plain evidence of what the summary features condense.
	 */
	private static void printRadialProfile(String[] names, int[][] phantoms) throws Exception {
		System.out.println("Self affinity of gray level 0, g(0,0,r)");
		System.out.printf("%-18s", "r =");
		for (int r = 1; r <= 12; r++) {
			System.out.printf("%8d", r);
		}
		System.out.println();
		for (int p = 0; p < phantoms.length; p++) {
			GLAMFeatures glam = glam(phantoms[p]);
			double[][][] g = glam.getRadialDistributionFunction();
			System.out.printf("%-18s", names[p]);
			for (int r = 1; r <= 12; r++) {
				System.out.printf("%8.3f", g[r][0][0]);
			}
			System.out.println();
			RadiomicsJ.resetSettings();
		}
		System.out.println();
		System.out.println("1.0 means the arrangement is indistinguishable from chance at that distance.");
	}

	private static GLAMFeatures glam(int[] levels) throws Exception {
		RadiomicsJ.resetSettings();
		RadiomicsJ.glamMaxRadius = MAX_RADIUS;
		return new GLAMFeatures(image(levels), mask(), 1, true, LEVELS, null, MAX_RADIUS);
	}

	private static ImagePlus image(int[] levels) {
		ImageStack stack = new ImageStack(SIZE, SIZE);
		for (int z = 0; z < SIZE; z++) {
			FloatProcessor fp = new FloatProcessor(SIZE, SIZE);
			for (int y = 0; y < SIZE; y++) {
				for (int x = 0; x < SIZE; x++) {
					fp.setf(x, y, levels[index(x, y, z)]);
				}
			}
			stack.addSlice(fp);
		}
		return calibrated(new ImagePlus("phantom", stack));
	}

	private static ImagePlus mask() {
		ImageStack stack = new ImageStack(SIZE, SIZE);
		for (int z = 0; z < SIZE; z++) {
			FloatProcessor fp = new FloatProcessor(SIZE, SIZE);
			for (int y = 0; y < SIZE; y++) {
				for (int x = 0; x < SIZE; x++) {
					fp.setf(x, y, 1f);
				}
			}
			stack.addSlice(fp);
		}
		return calibrated(new ImagePlus("mask", stack));
	}

	private static ImagePlus calibrated(ImagePlus imp) {
		imp.getCalibration().pixelWidth = 1d;
		imp.getCalibration().pixelHeight = 1d;
		imp.getCalibration().pixelDepth = 1d;
		return imp;
	}
}
