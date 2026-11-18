package io.github.tatsunidas.radiomics.features;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

import ij.ImagePlus;
import io.github.tatsunidas.radiomics.main.Utils;

/**
 * The squares of the zone size, the distance, the run length and the grey level were multiplied as int: over 46340 the
 * product overflowed (a wrong value), at 65536 it became 0 (a division 0/0 gave NaN). The references are computed here in double.
 */
class FeatureOverflowRegressionTest {

    private static final double TOL = 1e-9;

    private static void near(double expected, Double actual, String what) {
        assertTrue(actual != null && !actual.isNaN() && !actual.isInfinite(), what + " is " + actual);
        assertEquals(expected, actual, Math.abs(expected) * TOL, what);
    }

    /** GLSZM: a zone of N - w voxels (grey level 1) and a zone of w voxels (grey level 2), each half of the zones. */
    @Test
    void glszmWithAZoneOfMoreThan46340Voxels() throws Exception {
        for (int[] d : new int[][] { { 40, 40, 30 }, { 52, 52, 25 }, { 100, 100, 20 } }) { // 48,000 / 67,600 / 200,000 voxels
            ImagePlus[] v = TestVolumes.bigBlock(d[0], d[1], d[2]);
            double n = (double) d[0] * d[1] * d[2];
            double big = n - d[0];
            double small = d[0];
            GLSZMFeatures f = new GLSZMFeatures(v[0], v[1], 1, true, 2, null);
            String at = " (" + (long) n + " voxels)";
            near(0.5 * big * big + 0.5 * small * small, f.calculate(GLSZMFeatureType.LargeZoneEmphasis.id()), "LargeZoneEmphasis" + at);
            near(0.5 / (big * big) + 0.5 / (small * small), f.calculate(GLSZMFeatureType.SmallZoneEmphasis.id()), "SmallZoneEmphasis" + at);
            // the grey level of the big zone is 1, that of the small zone is 2
            near(0.5 * 1.0 / (big * big) + 0.5 * 4.0 / (small * small), f.calculate(GLSZMFeatureType.SmallZoneHighGrayLevelEmphasis.id()),
                    "SmallZoneHighGrayLevelEmphasis" + at);
        }
    }

    /** GLDZM: the centre voxel of a cube has the grey level 4096 and the distance 16: i*i*j*j = 2^32 = 0 as int. */
    @Test
    void gldzmWithAHighGreyLevelAndALargeDistance() throws Exception {
        ImagePlus[] v = TestVolumes.volume(31, 31, 31, (x, y, z) -> (x == 15 && y == 15 && z == 15) ? 4095f : 0f);
        GLDZMFeatures f = new GLDZMFeatures(v[0], v[1], 1, false, null, 1.0); // bin width 1: 4096 grey levels
        double[][] p = f.getMatrix(false);
        assertEquals(4096, p.length, "grey levels");
        double ref = 0;
        for (int i = 1; i <= p.length; i++) {
            for (int j = 1; j <= p[0].length; j++) {
                ref += p[i - 1][j - 1] / ((double) i * i * j * j);
            }
        }
        assertTrue(ref > 0.4, "the reference " + ref);
        near(ref, f.calculate(GLDZMFeatureType.SmallDistanceLowGrayLevelEmphasis.id()), "SmallDistanceLowGrayLevelEmphasis");
    }

    /** GLRLM: with 300 grey levels the cell (256, 256) has i*i*j*j = 2^32 = 0 as int, and 0/0 was NaN even for an empty cell. */
    @Test
    void glrlmWithMoreThan255GreyLevels() throws Exception {
        Random r = new Random(5);
        ImagePlus[] v = TestVolumes.volume(24, 24, 8, (x, y, z) -> (float) r.nextInt(3000));
        GLRLMFeatures f = new GLRLMFeatures(v[0], v[1], 1, true, 300, null, null);
        double sum = 0;
        int angles = 0;
        for (java.util.Map.Entry<Integer, int[]> e : Utils.buildAngles().entrySet()) {
            if (e.getKey() < 14) {
                continue;
            }
            double[][] p = f.getMatrix(e.getValue());
            double a = 0;
            for (int i = 1; i <= p.length; i++) {
                for (int j = 1; j <= p[0].length; j++) {
                    a += p[i - 1][j - 1] / ((double) i * i * j * j);
                }
            }
            sum += a;
            angles++;
        }
        assertEquals(13, angles);
        near(sum / angles, f.calculate(GLRLMFeatureType.ShortRunLowGrayLevelEmphasis.id()), "ShortRunLowGrayLevelEmphasis");
        Double lrhge = f.calculate(GLRLMFeatureType.LongRunHighGrayLevelEmphasis.id());
        assertTrue(lrhge != null && Double.isFinite(lrhge), "LongRunHighGrayLevelEmphasis " + lrhge);
    }

    /** NGLDM: 4096 grey levels and a dependence of 16 (i*j = 65536): the same overflow. */
    @Test
    void ngldmWithAHighGreyLevelAndALargeDependence() throws Exception {
        // a uniform block of the highest grey level: its voxels have 26 equal neighbours inside
        ImagePlus[] v = TestVolumes.volume(14, 14, 14, (x, y, z) -> (x >= 2 && x < 12 && y >= 2 && y < 12 && z >= 2 && z < 12) ? 4095f : 0f);
        NGLDMFeatures f = new NGLDMFeatures(v[0], v[1], 1, 0, 1, false, null, 1.0);
        double[][] p = f.getMatrix(); // the counts, not normalised: a feature is divided by the sum of the counts
        assertEquals(4096, p.length);
        double ref = 0;
        double total = 0;
        for (int i = 1; i <= p.length; i++) {
            for (int j = 1; j <= p[0].length; j++) {
                ref += p[i - 1][j - 1] / ((double) i * i * j * j);
                total += p[i - 1][j - 1];
            }
        }
        near(ref / total, f.calculate(NGLDMFeatureType.LowDependenceLowGrayLevelEmphasis.id()), "LowDependenceLowGrayLevelEmphasis");
    }
}
