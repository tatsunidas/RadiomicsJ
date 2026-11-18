package io.github.tatsunidas.radiomics.features;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import ij.ImagePlus;

/**
 * The matrix was always nBins x nBins: a run longer than nBins was dropped silently (and when all runs were dropped the
 * normalisation gave 0/0 = NaN). Now the matrix is as wide as the longest run, but never narrower than nBins.
 */
class GlrlmLongRunTest {

    private static double sumOf(double[][] m) {
        double s = 0;
        for (double[] row : m) {
            for (double c : row) {
                s += c;
            }
        }
        return s;
    }

    /** Rows of 100 voxels alternate between two grey levels: every horizontal (0 degree) run is 100 voxels long. */
    @Test
    void runsLongerThanTheNumberOfBinsAreKept() throws Exception {
        ImagePlus[] v = TestVolumes.volume(100, 100, 1, (x, y, z) -> (y % 2 == 0) ? 0f : 10f);
        GLRLMFeatures f = new GLRLMFeatures(v[0], v[1], 1, true, 32, null, null);
        double[][] m = f.getMatrix(new int[] { 0, 0, 1 });
        assertEquals(32, m.length, "grey levels");
        assertEquals(100, m[0].length, "the matrix is as wide as the longest run (100 > 32)");
        assertEquals(1.0, sumOf(m), 1e-12, "no run is dropped: the normalised matrix sums to 1");
        // 100 runs (one per row), 50 per grey level, each 100 voxels: p(1,100) = p(32,100) = 0.5
        assertEquals(0.5, m[0][99], 1e-12);
        assertEquals(0.5, m[31][99], 1e-12);
    }

    @Test
    void featuresOfAHorizontalRunOnlyDirectionAreFinite() throws Exception {
        ImagePlus[] v = TestVolumes.volume(100, 100, 1, (x, y, z) -> (y % 2 == 0) ? 0f : 10f);
        GLRLMFeatures f = new GLRLMFeatures(v[0], v[1], 1, true, 32, null, null);
        for (GLRLMFeatureType t : GLRLMFeatureType.values()) {
            Double value = f.calculate(t.id());
            assertTrue(value != null && !value.isNaN(), t.name() + " = " + value);
        }
    }

    /** When no run is longer than nBins the matrix keeps the width nBins (the same values as before 2.7.2). */
    @Test
    void theWidthIsStillTheNumberOfBinsWhenAllRunsAreShort() throws Exception {
        ImagePlus[] v = TestVolumes.volume(8, 8, 1, (x, y, z) -> (float) ((x + 2 * y) % 5));
        GLRLMFeatures f = new GLRLMFeatures(v[0], v[1], 1, true, 32, null, null);
        double[][] m = f.getMatrix(new int[] { 0, 0, 1 });
        assertEquals(32, m.length);
        assertEquals(32, m[0].length, "the longest run is 1 (the neighbours differ): the width stays nBins");
        assertEquals(1.0, sumOf(m), 1e-12);
    }
}
