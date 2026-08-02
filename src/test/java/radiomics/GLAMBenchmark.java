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

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import ij.ImagePlus;
import ij.measure.ResultsTable;
import io.github.tatsunidas.radiomics.features.GLAMFeatureType;
import io.github.tatsunidas.radiomics.features.GLAMFeatures;
import io.github.tatsunidas.radiomics.features.GLAMMatrixType;
import io.github.tatsunidas.radiomics.main.RadiomicsJ;
import io.github.tatsunidas.radiomics.main.SettingParams;
import io.github.tatsunidas.radiomics.main.TestDataLoader;
import io.github.tatsunidas.radiomics.main.Utils;

/**
 * Produces the published GLAM benchmark on the IBSI CT radiomics phantom.
 *
 * The image is not redistributed with RadiomicsJ. It comes from the IBSI data
 * sets repository and lives under src/test/resources during development, so
 * this generator is a debug only tool. See docs/GLAM_benchmark_IBSI_CT.md for
 * the data description and the download link.
 *
 * The preprocessing is IBSI configuration D, which RadiomicsJ already passes
 * against the IBSI reference values, plus the GLAM settings. Everything is
 * fixed and deterministic, so the numbers can be reproduced exactly.
 *
 * <pre>
 * mvn test-compile exec:java -Dexec.mainClass=radiomics.GLAMBenchmark -Dexec.classpathScope=test
 * </pre>
 *
 * @author tatsunidas <t_kobayashi@vis-ionary.com>
 *         (implemented with the coding assistant Claude, Anthropic)
 */
public class GLAMBenchmark {

	/** IBSI configuration D, plus the GLAM settings. */
	private static final double RESAMPLE_MM = 2.0;
	private static final int BIN_COUNT = 32;
	private static final double Z_SCORE = 3.0;
	private static final int MAX_RADIUS = 50;// 50 voxels of 2 mm, so 100 mm as in the paper

	private static final File OUTPUT_DIR = new File("docs/benchmark");

	public static void main(String[] args) throws Exception {
		ImagePlus[] pair;
		try {
			pair = TestDataLoader.sample_ct1();
		} catch (Throwable t) {
			System.out.println("[DEBUG ONLY] The IBSI CT radiomics phantom is not on the classpath.");
			System.out.println("[DEBUG ONLY] Run this from the source tree, see docs/GLAM_benchmark_IBSI_CT.md.");
			return;
		}
		if (pair == null || pair[0] == null || pair[1] == null) {
			System.out.println("[DEBUG ONLY] Could not open the IBSI CT radiomics phantom.");
			return;
		}
		ImagePlus image = pair[0];
		ImagePlus mask = pair[1];
		OUTPUT_DIR.mkdirs();

		System.out.println("=== source data ===");
		System.out.println("  dimensions      : " + image.getWidth() + " x " + image.getHeight() + " x "
				+ image.getNSlices());
		System.out.println("  voxel spacing   : " + image.getCalibration().pixelWidth + " x "
				+ image.getCalibration().pixelHeight + " x " + image.getCalibration().pixelDepth + " mm");
		System.out.println("  roi voxels      : " + Utils.getVoxels(image, mask, 1).length);

		ResultsTable table = extract(image, mask);
		writeFeatures(table);
		writeMatricesAndRdf(image, mask);
		System.out.println();
		System.out.println("Benchmark written to " + OUTPUT_DIR.getAbsolutePath());
	}

	private static Properties settings() {
		Properties prop = new Properties();
		prop.put(SettingParams.INT_label.name(), "1");
		// IBSI configuration D
		prop.put(SettingParams.DOUBLEARRAY_resamplingFactorXYZ.name(),
				RESAMPLE_MM + "," + RESAMPLE_MM + "," + RESAMPLE_MM);
		prop.put(SettingParams.INT_interpolation3D.name(), String.valueOf(RadiomicsJ.TRILINEAR));
		prop.put(SettingParams.INT_interpolation_mask3D.name(), String.valueOf(RadiomicsJ.TRILINEAR));
		prop.put(SettingParams.BOOL_interpolation_intensity_rounding.name(), "1");
		prop.put(SettingParams.BOOL_removeOutliers.name(), "1");
		prop.put(SettingParams.DOUBLE_zScore.name(), String.valueOf(Z_SCORE));
		prop.put(SettingParams.BOOL_USE_FixedBinNumber.name(), "1");
		prop.put(SettingParams.INT_binCount.name(), String.valueOf(BIN_COUNT));
		prop.put(SettingParams.BOOL_force2D.name(), "0");
		// GLAM
		prop.put(SettingParams.BOOL_enableGLAM.name(), "1");
		prop.put(SettingParams.INT_GLAM_maxRadius.name(), String.valueOf(MAX_RADIUS));
		prop.put(SettingParams.BOOL_GLAM_boundaryCorrection.name(), "1");
		prop.put(SettingParams.INT_GLAM_maxReferenceVoxels.name(), "0");// exact, no sub sampling
		prop.put(SettingParams.INT_GLAM_numRandomisations.name(), "0");// exact closed form
		prop.put(SettingParams.INT_GLAM_savitzkyGolayWindow.name(), "7");
		prop.put(SettingParams.INT_GLAM_savitzkyGolayPolynomial.name(), "3");
		prop.put(SettingParams.DOUBLE_GLAM_peakProminence.name(), "4");
		prop.put(SettingParams.INT_GLAM_maxLocalShellRadius.name(), "30");
		return prop;
	}

	private static ResultsTable extract(ImagePlus image, ImagePlus mask) throws Exception {
		Properties prop = settings();
		// diagnostics document what the preprocessing actually produced
		prop.put(SettingParams.BOOL_enableDiagnostics.name(), "1");
		for (String family : new String[] { "BOOL_enableOperationalInfo", "BOOL_enableMorphological",
				"BOOL_enableLocalIntensityFeatures", "BOOL_enableIntensityBasedStatistics",
				"BOOL_enableIntensityHistogram", "BOOL_enableIntensityVolumeHistogram", "BOOL_enableGLCM",
				"BOOL_enableGLRLM", "BOOL_enableGLSZM", "BOOL_enableGLDZM", "BOOL_enableNGTDM", "BOOL_enableNGLDM",
				"BOOL_enableFractal" }) {
			prop.put(family, "0");
		}
		RadiomicsJ radiomics = new RadiomicsJ();
		radiomics.loadSettings(prop);
		long start = System.nanoTime();
		ResultsTable table = radiomics.execute(image, mask, 1);
		System.out.printf("%n=== extraction ===%n  took %.1f s%n", (System.nanoTime() - start) / 1e9);
		return table;
	}

	private static void writeFeatures(ResultsTable table) throws Exception {
		File file = new File(OUTPUT_DIR, "glam_ibsi_ct_features.csv");
		int written = 0;
		try (PrintWriter out = new PrintWriter(file, StandardCharsets.UTF_8.name())) {
			out.println("feature,value");
			for (String heading : table.getHeadings()) {
				String value = table.getStringValue(heading, 0);
				if (heading.startsWith("GLAM_")) {
					// the raw double, not the three decimal rendering of the table
					double raw = table.getValue(heading, 0);
					out.println(heading + "," + (Double.isNaN(raw) ? "NaN" : Double.toString(raw)));
					written++;
				} else {
					out.println(heading + "," + value);
				}
			}
		}
		System.out.println("  GLAM features written : " + written);
		System.out.println();
		System.out.println("=== diagnostics ===");
		for (String heading : table.getHeadings()) {
			if (!heading.startsWith("GLAM_")) {
				System.out.println("  " + heading + " = " + table.getStringValue(heading, 0));
			}
		}
	}

	/**
	 * The affinity matrices and the radial distribution function themselves, so
	 * that another implementation can be compared before the reduction statistics
	 * hide where a difference comes from.
	 */
	private static void writeMatricesAndRdf(ImagePlus image, ImagePlus mask) throws Exception {
		RadiomicsJ radiomics = new RadiomicsJ();
		radiomics.loadSettings(settings());
		// the very same preprocessing the extraction used, so the matrices and the
		// features published here cannot drift apart
		radiomics.preprocess(image, mask, 1);
		RadiomicsJ.discretiseImp = null;
		GLAMFeatures glam = new GLAMFeatures(radiomics.getAnalysisReadyImage(), radiomics.getAnalysisReadyMask(),
				RadiomicsJ.label_, true, BIN_COUNT, null, MAX_RADIUS);

		for (GLAMMatrixType type : GLAMMatrixType.values()) {
			double[][] matrix = glam.getMatrix(type);
			File file = new File(OUTPUT_DIR, "matrix_" + type.name() + ".csv");
			try (PrintWriter out = new PrintWriter(file, StandardCharsets.UTF_8.name())) {
				for (double[] row : matrix) {
					StringBuilder sb = new StringBuilder();
					for (int i = 0; i < row.length; i++) {
						if (i > 0) {
							sb.append(',');
						}
						sb.append(Double.isNaN(row[i]) ? "NaN" : Double.toString(row[i]));
					}
					out.println(sb);
				}
			}
		}

		double[][][] g = glam.getRadialDistributionFunction();
		int levels = glam.getNumberOfBins();
		File file = new File(OUTPUT_DIR, "radial_distribution_function.csv");
		try (PrintWriter out = new PrintWriter(file, StandardCharsets.UTF_8.name())) {
			StringBuilder header = new StringBuilder("r");
			for (int alpha = 0; alpha < levels; alpha++) {
				for (int beta = 0; beta < levels; beta++) {
					header.append(",g_").append(alpha).append('_').append(beta);
				}
			}
			out.println(header);
			for (int r = 1; r <= glam.getMaxRadius(); r++) {
				StringBuilder sb = new StringBuilder().append(r);
				for (int alpha = 0; alpha < levels; alpha++) {
					for (int beta = 0; beta < levels; beta++) {
						double value = g[r][alpha][beta];
						sb.append(',').append(Double.isNaN(value) ? "NaN" : Double.toString(value));
					}
				}
				out.println(sb);
			}
		}
		System.out.println();
		System.out.println("=== glam inputs ===");
		System.out.println("  gray levels     : " + levels);
		System.out.println("  max radius      : " + glam.getMaxRadius() + " voxels");
		int occupied = 0;
		double[][] virial = glam.getMatrix(GLAMMatrixType.SecondVirialCoefficient);
		for (int alpha = 0; alpha < levels; alpha++) {
			if (!Double.isNaN(virial[alpha][alpha])) {
				occupied++;
			}
		}
		System.out.println("  occupied levels : " + occupied + " of " + levels);
		// keep the classpath handover clean for anything that runs afterwards
		RadiomicsJ.discretiseImp = null;
		for (GLAMFeatureType type : new GLAMFeatureType[] { GLAMFeatureType.SecondVirialCoefficient_Mean,
				GLAMFeatureType.SecondVirialCoefficient_Minimum, GLAMFeatureType.CoordinationNumber_DiagonalMean,
				GLAMFeatureType.InverseCorrelationLength_Mean }) {
			System.out.println("  " + type.name() + " = " + glam.calculate(type.id()));
		}
	}
}
