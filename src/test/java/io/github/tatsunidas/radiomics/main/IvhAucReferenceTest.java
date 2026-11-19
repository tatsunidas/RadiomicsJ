package io.github.tatsunidas.radiomics.main;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import ij.ImagePlus;
import ij.measure.ResultsTable;

/**
 * IBSI 1 (3.5.5) gives no reference value for the area under the IVH curve and the feature is excluded by default,
 * but the validation file of the IBSI (IBSI_ValidationFile.xlsx) has values that the grid trapezoidal rule must reproduce:
 * digital phantom 0.32, configuration C 0.681 +- 0.003, configuration D 0.563 +- 0.013.
 * The value in the manual is not a pass/fail criterion of the library; this test only keeps the agreement.
 */
class IvhAucReferenceTest {

    private static double auc(boolean phantom, String settings) throws Exception {
        int idx = -1;
        for (int i = 0; i < RadiomicsJ.excluded_list.length; i++) {
            if (RadiomicsJ.excluded_list[i].equals("AreaUnderTheIVHCurve")) {
                idx = i;
            }
        }
        String saved = RadiomicsJ.excluded_list[idx];
        try {
            RadiomicsJ.excluded_list[idx] = "not_a_feature";
            ImagePlus[] ds = phantom ? TestDataLoader.digital_phantom1_scratch() : TestDataLoader.sample_ct1();
            RadiomicsJ radi = new RadiomicsJ();
            radi.loadSettingsFromResource(settings);
            ResultsTable res = radi.execute(ds[0], ds[1], RadiomicsJ.targetLabel);
            return res.getValueAsDouble(res.getColumnIndex("IntensityVolumeHistogram_AreaUnderTheIVHCurve"), 0);
        } finally {
            RadiomicsJ.excluded_list[idx] = saved;
        }
    }

    @Test
    void digitalPhantom() throws Exception {
        assertEquals(0.32, auc(true, "validation/ParamsTestDigitalPhantom1.properties"), 0.005);
    }

    @Test
    void configurationC() throws Exception {
        assertEquals(0.681, auc(false, "validation/ParamsTestCT_PAT1_Config_C.properties"), 0.003);
    }

    @Test
    void configurationD() throws Exception {
        assertEquals(0.563, auc(false, "validation/ParamsTestCT_PAT1_Config_D.properties"), 0.013);
    }
}
