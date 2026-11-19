package io.github.tatsunidas.radiomics.features;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ij.ImagePlus;
import io.github.tatsunidas.radiomics.main.RadiomicsJ;
import io.github.tatsunidas.radiomics.main.Utils;

/**
 * IBSI 1 section 3.5. The reference below is written from the formulas of the manual, with floating point values and
 * loops over the grid, not with the bin numbers of the implementation:
 * nu_i = 1 - (1/N) sum[X < i], gamma_i = (i - min G) / (max G - min G),
 * I_x = the smallest grid intensity with nu_i <= x, V_x = nu of the smallest grid intensity with gamma_i >= x,
 * AUC = the trapezoidal rule on the grid.
 */
class IvhDefinitionTest {

    private static final IntensityVolumeHistogramFeatureType[] ORDER = {
            IntensityVolumeHistogramFeatureType.VolumeAtIntensityFraction10,
            IntensityVolumeHistogramFeatureType.VolumeAtIntensityFraction90,
            IntensityVolumeHistogramFeatureType.IntensityAtVolumeFraction10,
            IntensityVolumeHistogramFeatureType.IntensityAtVolumeFraction90,
            IntensityVolumeHistogramFeatureType.VolumeFractionDifferenceBetweenIntensityFractions,
            IntensityVolumeHistogramFeatureType.IntensityFractionDifferenceBetweenVolumeFractions,
            IntensityVolumeHistogramFeatureType.AreaUnderTheIVHCurve };

    private double binWidth;
    private int binCount;

    @BeforeEach
    void save() {
        binWidth = RadiomicsJ.IVH_binWidth;
        binCount = RadiomicsJ.IVH_binCount;
    }

    @AfterEach
    void restore() {
        RadiomicsJ.rangeMin = null;
        RadiomicsJ.rangeMax = null;
        RadiomicsJ.IVH_binWidth = binWidth;
        RadiomicsJ.IVH_binCount = binCount;
    }

    private static double[] ivh(float[] values, int mode) throws Exception {
        ImagePlus[] v = TestVolumes.volume(values.length, 1, 1, (x, y, z) -> values[x]);
        return ivh(v[0], v[1], mode);
    }

    private static double[] ivh(ImagePlus img, ImagePlus mask, int mode) throws Exception {
        IntensityVolumeHistogramFeatures f = new IntensityVolumeHistogramFeatures(img, mask, 1, mode);
        double[] r = new double[ORDER.length];
        for (int i = 0; i < r.length; i++) {
            Double d = f.calculate(ORDER[i].id());
            r[i] = d == null ? Double.NaN : d;
        }
        return r;
    }

    private static float[] repeat(float v, int n) {
        float[] a = new float[n];
        java.util.Arrays.fill(a, v);
        return a;
    }

    private static float[] concat(float[]... parts) {
        int n = 0;
        for (float[] p : parts) {
            n += p.length;
        }
        float[] a = new float[n];
        int o = 0;
        for (float[] p : parts) {
            System.arraycopy(p, 0, a, o, p.length);
            o += p.length;
        }
        return a;
    }

    /** the 7 features from the grid intensities g (ascending, step w) and the discretised ROI intensities xd. */
    private static double[] reference(double[] xd, double[] g, double w) {
        int n = xd.length;
        java.util.function.DoubleUnaryOperator nu = i -> {
            int below = 0;
            for (double x : xd) {
                if (x < i - w / 2) {
                    below++;
                }
            }
            return 1d - (double) below / n;
        };
        double gmin = g[0], gmax = g[g.length - 1];
        double[] r = new double[7];
        // V_x
        for (int p : new int[] { 10, 90 }) {
            double v = Double.NaN;
            for (double i : g) {
                if ((i - gmin) / (gmax - gmin) >= p / 100d - 1e-12) {
                    v = nu.applyAsDouble(i);
                    break;
                }
            }
            r[p == 10 ? 0 : 1] = v;
        }
        // I_x: also past the end of G
        for (int p : new int[] { 10, 90 }) {
            double res = Double.NaN;
            for (double i = g[0]; i < gmax + 2 * 1000 * w; i += w) {
                if (nu.applyAsDouble(i) <= p / 100d + 1e-12) {
                    res = i;
                    break;
                }
            }
            r[p == 10 ? 2 : 3] = res;
        }
        r[4] = r[0] - r[1];
        r[5] = r[2] - r[3];
        double auc = 0;
        for (int j = 0; j + 1 < g.length; j++) {
            auc += (nu.applyAsDouble(g[j]) + nu.applyAsDouble(g[j + 1])) / 2d * ((g[j + 1] - g[j]) / (gmax - gmin));
        }
        r[6] = auc;
        return r;
    }

    private static void assertAll7(double[] expected, double[] actual, double tol) {
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], actual[i], tol, ORDER[i].name());
        }
    }

    /** The digital phantom: 74 voxels, the grey levels 1 (50 voxels), 3 (1), 4 (16) and 6 (7); table 3.70, I10 = 5, I90 = 2, V10 = 0.324, V90 = 0.0946, AUC 0.32. */
    @Test
    void theDigitalPhantom() throws Exception {
        double[] r = ivh(concat(repeat(1, 50), repeat(3, 1), repeat(4, 16), repeat(6, 7)), 0);
        assertEquals(24d / 74, r[0], 1e-12);
        assertEquals(7d / 74, r[1], 1e-12);
        assertEquals(5, r[2], 0);
        assertEquals(2, r[3], 0);
        assertEquals(24d / 74 - 7d / 74, r[4], 1e-12);
        assertEquals(3, r[5], 0);
        assertEquals(0.3203, r[6], 5e-4);
        double[] g = { 1, 2, 3, 4, 5, 6 };
        double[] xd = new double[74];
        int o = 0;
        for (int i = 0; i < 50; i++) xd[o++] = 1;
        xd[o++] = 3;
        for (int i = 0; i < 16; i++) xd[o++] = 4;
        for (int i = 0; i < 7; i++) xd[o++] = 6;
        assertAll7(reference(xd, g, 1), r, 1e-12);
    }

    /** 1..10 once each: nu_10 is exactly 0.1 and nu_2 exactly 0.9 (a tie); "at most x%" takes the bin itself: I10 = 10, I90 = 2. */
    @Test
    void aTieOfTheVolumeFractionAndX() throws Exception {
        double[] r = ivh(new float[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 }, 0);
        assertEquals(10, r[2], 0);
        assertEquals(2, r[3], 0);
        assertEquals(8, r[5], 0);
    }

    /** Intensities that no voxel has are on the grid: 1 once and 10 nine times: nu is 0.9 on 2..10, so I90 = 2 (absent) and I10 = 11 (past the top). */
    @Test
    void anAbsentGridPointIsTheAnswer() throws Exception {
        double[] r = ivh(concat(repeat(1, 1), repeat(10, 9)), 0);
        assertEquals(2, r[3], 0);
        assertEquals(11, r[2], 0);
        double[] g = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        double[] xd = concat(repeat(1, 1), repeat(10, 9)).length == 10 ? new double[] { 1, 10, 10, 10, 10, 10, 10, 10, 10, 10 } : null;
        assertAll7(reference(xd, g, 1), r, 1e-12);
    }

    /** One value in the ROI: the area is 0 (IBSI), also with a re-segmentation range; the volume fractions follow the range. */
    @Test
    void aSingleValueRoi() throws Exception {
        double[] r = ivh(repeat(100, 20), 0);
        assertEquals(0d, r[6], 0);
        assertTrue(Double.isNaN(r[0]), "no range: G is one point, gamma is undefined");
        RadiomicsJ.rangeMin = -1000d;
        RadiomicsJ.rangeMax = 400d;
        r = ivh(repeat(100, 20), 0);
        assertEquals(0d, r[6], 0);
        assertEquals(1d, r[0], 0);
        assertEquals(0d, r[1], 0);
        assertEquals(101, r[2], 0);
    }

    /** A range that is open above (rangeMin only): G is [rangeMin, max(X)] (IBSI 3.5). */
    @Test
    void aRangeOpenAbove() throws Exception {
        RadiomicsJ.rangeMin = 0d;
        float[] v = new float[100];
        for (int i = 0; i < 100; i++) v[i] = i + 1;
        double[] r = ivh(v, 0);
        double[] g = new double[101];
        for (int i = 0; i <= 100; i++) g[i] = i;
        double[] xd = new double[100];
        for (int i = 0; i < 100; i++) xd[i] = i + 1;
        assertAll7(reference(xd, g, 1), r, 1e-12);
    }

    /** A CT interpolated without rounding gives the same IVH as the rounded image (all 7 features), also at 88.99999 and at the halves (to even). */
    @Test
    void fractionalIntensitiesGiveTheIvhOfTheRoundedImage() throws Exception {
        Random rnd = new Random(7);
        float[] v = new float[4000];
        for (int i = 0; i < v.length; i++) {
            v[i] = (float) (rnd.nextGaussian() * 60 - 400);
        }
        v[0] = 88.99999f;
        v[1] = 88.5f;
        v[2] = 89.5f;
        v[3] = -0.0f;
        RadiomicsJ.rangeMin = -1000d;
        RadiomicsJ.rangeMax = 400d;
        ImagePlus[] a = TestVolumes.volume(v.length, 1, 1, (x, y, z) -> v[x]);
        ImagePlus rounded = Utils.roundIntensitiesToNearestInteger(a[0]);
        double[] fractional = ivh(a[0], a[1], 0);
        double[] fromRounded = ivh(rounded, a[1], 0);
        assertAll7(fromRounded, fractional, 0);
        // the reference of the manual on the rounded values
        double[] g = new double[1401];
        for (int i = 0; i <= 1400; i++) g[i] = -1000 + i;
        double[] xd = new double[v.length];
        for (int i = 0; i < v.length; i++) xd[i] = Math.rint(v[i]);
        assertAll7(reference(xd, g, 1), fractional, 1e-12);
    }

    /** Configuration C: bin width 2.5 on [-1000, 400]; I10 and I90 are the bin centres one bin above the highest bin that has more than x%. */
    @Test
    void fixedBinWidthOfTheContinuousCase() throws Exception {
        Random rnd = new Random(11);
        float[] v = new float[6000];
        for (int i = 0; i < v.length; i++) {
            v[i] = (float) (rnd.nextGaussian() * 90 - 300);
        }
        RadiomicsJ.rangeMin = -1000d;
        RadiomicsJ.rangeMax = 400d;
        RadiomicsJ.IVH_binWidth = 2.5;
        double[] r = ivh(v, 1);
        int nb = 560;
        double[] g = new double[nb];
        for (int j = 1; j <= nb; j++) g[j - 1] = -1000 + (j - 0.5) * 2.5;
        double[] xd = new double[v.length];
        for (int i = 0; i < v.length; i++) xd[i] = -1000 + (Math.floor(((double) v[i] + 1000) / 2.5) + 0.5) * 2.5;
        assertAll7(reference(xd, g, 2.5), r, 1e-9);
    }

    /** A voxel exactly at rangeMax is in the bin Nb + 1, outside G; the volume fractions are still defined and I_x is found past G. */
    @Test
    void aValueAtTheTopOfTheRange() throws Exception {
        RadiomicsJ.rangeMin = -1000d;
        RadiomicsJ.rangeMax = 400d;
        RadiomicsJ.IVH_binWidth = 2.5;
        float[] v = concat(repeat(-500, 50), repeat(400, 50));
        double[] r = ivh(v, 1);
        // nu is 0.5 up to the bin of -500 (bin 201, centre -498.75 ...): I10 is above 400's bin? the top bin has all 50 voxels at 400: nu = 0.5 there.
        assertTrue(r[2] > 400, "the 50 voxels at 400 are in 50% of the volume, so the answer is above their bin: " + r[2]);
        assertEquals(1.0, r[0], 0);
    }

    /** A range that the bin width does not divide: 1000.0 / 0.3 = 3333.33 bins; the last partial bin is not in G. */
    @Test
    void aRangeThatTheBinWidthDoesNotDivide() throws Exception {
        RadiomicsJ.rangeMin = 0d;
        RadiomicsJ.rangeMax = 1d;
        RadiomicsJ.IVH_binWidth = 0.3;
        float[] v = { 0.05f, 0.1f, 0.35f, 0.5f, 0.65f, 0.7f, 0.95f, 0.8f };
        double[] r = ivh(v, 1);
        double[] g = { 0.15, 0.45, 0.75 };
        double[] xd = new double[v.length];
        for (int i = 0; i < v.length; i++) xd[i] = (Math.floor(v[i] / 0.3) + 0.5) * 0.3;
        assertAll7(reference(xd, g, 0.3), r, 1e-9);
    }

    /** Fixed bin number: G is [1, Ng] (not the re-segmentation range). */
    @Test
    void fixedBinNumber() throws Exception {
        Random rnd = new Random(3);
        float[] v = new float[3000];
        for (int i = 0; i < v.length; i++) {
            v[i] = (float) (rnd.nextGaussian() * 50 + 200);
        }
        RadiomicsJ.rangeMin = -1000d;
        RadiomicsJ.rangeMax = 400d;
        RadiomicsJ.IVH_binCount = 100;
        double[] r = ivh(v, 2);
        double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
        for (float x : v) {
            min = Math.min(min, x);
            max = Math.max(max, x);
        }
        double[] g = new double[100];
        for (int i = 0; i < 100; i++) g[i] = i + 1;
        double[] xd = new double[v.length];
        for (int i = 0; i < v.length; i++) {
            xd[i] = v[i] >= max ? 100 : (int) (100 * ((v[i] - min) / (max - min))) + 1;
        }
        assertAll7(reference(xd, g, 1), r, 1e-12);
    }

    /** No voxel in the ROI: every feature is NaN. */
    @Test
    void anEmptyRoi() throws Exception {
        ImagePlus[] v = TestVolumes.volume(4, 1, 1, (x, y, z) -> 5f, (x, y, z) -> false);
        double[] r = ivh(v[0], v[1], 0);
        for (double d : r) {
            assertTrue(Double.isNaN(d));
        }
    }
}
