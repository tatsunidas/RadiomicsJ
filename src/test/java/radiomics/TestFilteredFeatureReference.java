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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import ij.ImagePlus;
import ij.measure.ResultsTable;
import io.github.tatsunidas.radiomics.features.IntensityBasedStatisticalFeatureType;
import io.github.tatsunidas.radiomics.main.ImageFiltering.Padding;
import io.github.tatsunidas.radiomics.main.ImageFiltering.Pooling;
import io.github.tatsunidas.radiomics.main.RadiomicsJ;
import io.github.tatsunidas.radiomics.main.TestDataLoader;

/**
 * Intensity based statistical features of filtered images of the IBSI 2 lung cancer CT
 * phantom, against the reference values and tolerances of
 * https://github.com/theibsi/ibsi_2_reference_data (reference_values.csv).
 *
 * Configuration B of the reference manual (Table 6.2): 3D, tricubic spline to
 * 1 x 1 x 1 mm, intensities rounded, mask trilinear with partial volume 0.5,
 * re-segmentation to [-1000, 400] HU, mirror padding. Configuration A filters
 * every slice and still takes the features over the whole roi, which RadiomicsJ
 * does not offer (BOOL_force2D takes the features slice by slice), so A is not tested.
 *
 * @author tatsunidas <t_kobayashi@vis-ionary.com>
 */
public class TestFilteredFeatureReference {

	private static final String PHANTOM = "data_sets-master/ibsi_2_ct_radiomics_phantom/nifti/";
	private static final String REFERENCE = "data_sets-master/ibsi_2_reference_data/reference_feature_values/reference_values.csv";

	/** feature tag of the csv to the IBSI identifier */
	private static final Map<String, String> IDS = new LinkedHashMap<>();
	static {
		String[][] t = { { "stat_mean", "Q4LE" }, { "stat_var", "ECT3" }, { "stat_skew", "KE2A" }, { "stat_kurt", "IPH6" },
				{ "stat_median", "Y12H" }, { "stat_min", "1GSF" }, { "stat_p10", "QG58" }, { "stat_p90", "8DWT" },
				{ "stat_max", "84IY" }, { "stat_iqr", "SALO" }, { "stat_range", "2OJQ" }, { "stat_mad", "4FUA" },
				{ "stat_rmad", "1128" }, { "stat_medad", "N72L" }, { "stat_cov", "7TET" }, { "stat_qcod", "9S40" },
				{ "stat_energy", "N8CA" }, { "stat_rms", "5ZWQ" } };
		for (String[] p : t) {
			IDS.put(p[0], p[1]);
		}
	}

	@AfterEach
	public void reset() {
		RadiomicsJ.resetSettings();
	}

	private static List<String[]> reference(String filterId) throws Exception {
		List<String[]> rows = new ArrayList<>();
		try (BufferedReader r = new BufferedReader(new InputStreamReader(
				Thread.currentThread().getContextClassLoader().getResourceAsStream(REFERENCE), StandardCharsets.UTF_8))) {
			String line = r.readLine();
			while ((line = r.readLine()) != null) {
				String[] c = line.split(";");
				if (c[0].equals(filterId)) {
					rows.add(c);
				}
			}
		}
		return rows;
	}

	private static ResultsTable configurationB(double[] logSigma, int[] meanSupport) throws Exception {
		return configurationB(logSigma, meanSupport, null);
	}

	private static ResultsTable configurationB(double[] logSigma, int[] meanSupport, String laws) throws Exception {
		return configurationB(logSigma, meanSupport, laws, null);
	}

	private static ResultsTable configurationB(double[] logSigma, int[] meanSupport, String laws, String gabor)
			throws Exception {
		ImagePlus img = TestDataLoader.loadNifTi(PHANTOM + "image/phantom.nii.gz");
		ImagePlus msk = TestDataLoader.loadNifTi(PHANTOM + "mask/mask.nii.gz");
		RadiomicsJ rj = new RadiomicsJ();
		RadiomicsJ.interpolation3D = RadiomicsJ.TRICUBIC_SPLINE;
		RadiomicsJ.interpolation_mask3D = RadiomicsJ.TRILINEAR;
		RadiomicsJ.mask_PartialVolumeThreshold = 0.5;
		RadiomicsJ.interpolation_intensity_rounding = true;
		RadiomicsJ.rangeMin = -1000d;
		RadiomicsJ.rangeMax = 400d;
		rj.loadSettings(settings());
		RadiomicsJ.logSigmaMm = logSigma;
		RadiomicsJ.logPadding = Padding.MIRROR;
		RadiomicsJ.meanSupport = meanSupport;
		RadiomicsJ.meanPadding = Padding.MIRROR;
		if (laws != null) {
			RadiomicsJ.lawsKernels = new String[] { laws };
			RadiomicsJ.lawsPadding = Padding.MIRROR;
			RadiomicsJ.lawsPooling = Pooling.MAX;
			RadiomicsJ.lawsEnergyDelta = 7;
		}
		if (gabor != null) {
			RadiomicsJ.gaborSets = new String[] { gabor };
			RadiomicsJ.gaborPadding = Padding.MIRROR;
			RadiomicsJ.gaborPooling = Pooling.AVERAGE;
			RadiomicsJ.gaborDeltaTheta = Math.PI / 8;
		}
		return rj.extractAll(img, msk, 1);
	}

	private static java.util.Properties settings() {
		java.util.Properties p = new java.util.Properties();
		p.setProperty("DOUBLEARRAY_resamplingFactorXYZ", "1.0,1.0,1.0");
		p.setProperty("BOOL_USE_FixedBinNumber", "1");
		p.setProperty("INT_binCount", "32");
		// only the intensity based statistics are compared, the texture families are slow on this volume
		for (String family : new String[] { "OperationalInfo", "Diagnostics", "Morphological", "LocalIntensityFeatures",
				"IntensityHistogram", "IntensityVolumeHistogram", "Shape2D", "GLCM", "GLRLM", "GLSZM", "GLDZM", "GLAM",
				"NGTDM", "NGLDM", "Fractal" }) {
			p.setProperty("BOOL_enable" + family, "0");
		}
		p.setProperty("BOOL_enableIntensityBasedStatistics", "1");
		return p;
	}

	/** returns the tags that are out of tolerance, and prints every comparison */
	private static List<String> compare(String filterId, ResultsTable rt, String prefix) throws Exception {
		List<String> failed = new ArrayList<>();
		int n = 0;
		for (String[] c : reference(filterId)) {
			String id = IDS.get(c[2]);
			if (id == null) {
				continue;
			}
			String column = prefix + "IntensityBasedStatistical_" + IntensityBasedStatisticalFeatureType.findType(id);
			double ref = Double.parseDouble(c[3]), tol = Double.parseDouble(c[4]);
			double v = rt.getValue(column, 0);
			boolean ok = Math.abs(v - ref) <= tol;
			System.out.println(String.format("%s %-12s %14.6g  ref %12.6g +- %-10.4g %s", filterId, c[2], v, ref, tol,
					ok ? "ok" : "OUT"));
			if (!ok) {
				failed.add(c[2]);
			}
			n++;
		}
		assertTrue(n == IDS.size(), "compared " + n + " of " + IDS.size() + " features for " + filterId);
		return failed;
	}

	@Test
	public void noFilterMatchesTheReferenceOfConfigurationB() throws Exception {
		List<String> failed = compare("1.B", configurationB(null, null), "");
		assertTrue(failed.isEmpty(), "out of tolerance: " + failed);
	}

	@Test
	public void meanFilterMatchesTheReferenceOfConfigurationB() throws Exception {
		List<String> failed = compare("2.B", configurationB(null, new int[] { 5 }), "mean_M5_");
		assertTrue(failed.isEmpty(), "out of tolerance: " + failed);
	}

	@Test
	public void laplacianOfGaussianMatchesTheReferenceOfConfigurationB() throws Exception {
		List<String> failed = compare("3.B", configurationB(new double[] { 1.5d }, null), "log_sigma1.5mm_");
		assertTrue(failed.isEmpty(), "out of tolerance: " + failed);
	}

	@Test
	public void lawsMatchesTheReferenceOfConfigurationB() throws Exception {
		List<String> failed = compare("4.B", configurationB(null, null, "L5E5E5"), "laws_L5E5E5_max_d7_");
		assertTrue(failed.isEmpty(), "out of tolerance: " + failed);
	}

	@Test
	public void gaborMatchesTheReferenceOfConfigurationB() throws Exception {
		List<String> failed = compare("5.B", configurationB(null, null, null, "5:2:1.5:0"),
				"gabor_s5_l2_g1.5_t0_average_dt0.3927_");
		assertTrue(failed.isEmpty(), "out of tolerance: " + failed);
	}
}
