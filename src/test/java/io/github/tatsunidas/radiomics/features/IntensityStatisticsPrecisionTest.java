package io.github.tatsunidas.radiomics.features;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

import ij.ImagePlus;

/**
 * The skewness and the kurtosis summed raw moments (sum of v^3 - 3 mean sum of v^2 + ...): all the digits are lost when the mean is
 * large against the standard deviation, and a region of one constant value gave about 1e6 instead of NaN.
 */
class IntensityStatisticsPrecisionTest {

    private static float[][] values(double mean, double sd, int w, int h, int s) {
        Random r = new Random(7);
        float[][] v = new float[s][w * h];
        for (int z = 0; z < s; z++) {
            for (int i = 0; i < w * h; i++) {
                v[z][i] = (float) (mean + sd * r.nextGaussian());
            }
        }
        return v;
    }

    private static IntensityBasedStatisticalFeatures features(float[][] vals, int w, int h) {
        ImagePlus[] v = TestVolumes.volume(w, h, vals.length, (x, y, z) -> vals[z][y * w + x]);
        return new IntensityBasedStatisticalFeatures(v[0], v[1], 1);
    }

    /** The reference: the central moments of the same (float) values, summed in double. */
    private static double[] reference(float[][] vals) {
        double n = 0, sum = 0;
        for (float[] sl : vals) {
            for (float x : sl) {
                sum += x;
                n++;
            }
        }
        double mean = sum / n, m2 = 0, m3 = 0, m4 = 0;
        for (float[] sl : vals) {
            for (float x : sl) {
                double d = x - mean;
                m2 += d * d;
                m3 += d * d * d;
                m4 += d * d * d * d;
            }
        }
        m2 /= n;
        m3 /= n;
        m4 /= n;
        return new double[] { m3 / Math.pow(m2, 1.5), m4 / (m2 * m2) - 3.0 };
    }

    @Test
    void aLargeMeanAgainstTheStandardDeviation() {
        for (double[] ms : new double[][] { { 30000, 2.5 }, { 1000, 2.5 }, { 1e6, 10 } }) {
            float[][] vals = values(ms[0], ms[1], 64, 64, 16);
            double[] ref = reference(vals);
            IntensityBasedStatisticalFeatures f = features(vals, 64, 64);
            Double skew = f.calculate(IntensityBasedStatisticalFeatureType.Skewness.id());
            Double kurt = f.calculate(IntensityBasedStatisticalFeatureType.Kurtosis.id());
            String at = " (mean " + ms[0] + ", sd " + ms[1] + ")";
            assertEquals(ref[0], skew, Math.max(1e-9, Math.abs(ref[0]) * 1e-9), "skewness" + at);
            assertEquals(ref[1], kurt, Math.max(1e-9, Math.abs(ref[1]) * 1e-9), "excess kurtosis" + at);
        }
    }

    @Test
    void ctAndPetLikeValuesStayTheSame() {
        for (double[] ms : new double[][] { { -800, 100 }, { 40, 15 }, { 10000, 8000 }, { 0, 2.5 } }) {
            float[][] vals = values(ms[0], ms[1], 64, 64, 16);
            double[] ref = reference(vals);
            IntensityBasedStatisticalFeatures f = features(vals, 64, 64);
            assertEquals(ref[0], f.calculate(IntensityBasedStatisticalFeatureType.Skewness.id()), 1e-9, "skewness " + ms[0]);
            assertEquals(ref[1], f.calculate(IntensityBasedStatisticalFeatureType.Kurtosis.id()), 1e-9, "excess kurtosis " + ms[0]);
        }
    }

    @Test
    void aRegionOfOneConstantValueIsNaN() {
        for (float c : new float[] { 100.3f, 0f, 7f, -1000f }) {
            float[][] vals = new float[4][16 * 16];
            for (float[] sl : vals) {
                java.util.Arrays.fill(sl, c);
            }
            IntensityBasedStatisticalFeatures f = features(vals, 16, 16);
            assertTrue(f.calculate(IntensityBasedStatisticalFeatureType.Skewness.id()).isNaN(), "skewness of " + c);
            assertTrue(f.calculate(IntensityBasedStatisticalFeatureType.Kurtosis.id()).isNaN(), "kurtosis of " + c);
        }
    }
}
