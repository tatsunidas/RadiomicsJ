package io.github.tatsunidas.radiomics.features;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import ij.ImagePlus;

/**
 * NGTDM as IBSI defines it (ibsi.readthedocs.io, the neighbouring grey tone difference based features):
 * <ul>
 * <li>n_i, and so N_v,c, count only the voxels that have at least one neighbour in the region;</li>
 * <li>N_g,p is the number of grey levels with p_i &gt; 0 (not with s_i &ne; 0);</li>
 * <li>coarseness is 10^6 when its denominator is 0; contrast and busyness are 0 when N_g,p = 1; strength is 0 when the sum of s_i is 0.</li>
 * </ul>
 * The expected values are computed twice: by a plain implementation in this test and by an independent implementation in Python (numpy),
 * whose values are written here as constants. For a normal region both equal the values 2.7.3 gave.
 */
class NgtdmIbsiTest {

    private static final String[] NAMES = { "Coarseness", "Contrast", "Busyness", "Complexity", "Strength" };

    private static final int W = 12, H = 12, D = 6;

    private static double[] ngtdm(ImagePlus[] v) throws Exception {
        NGTDMFeatures f = new NGTDMFeatures(v[0], v[1], 1, 1, false, null, 1.0);
        double[] out = new double[NAMES.length];
        for (int i = 0; i < NAMES.length; i++) {
            out[i] = f.calculate(NGTDMFeatureType.valueOf(NAMES[i]).id());
        }
        return out;
    }

    /** A plain implementation of the IBSI definition (3D, distance 1, 26 neighbours, bins of width 1 from the minimum of the region). */
    private static double[] reference(float[][][] img, boolean[][][] roi) {
        double lo = Double.MAX_VALUE;
        for (int z = 0; z < D; z++) for (int y = 0; y < H; y++) for (int x = 0; x < W; x++) if (roi[z][y][x]) lo = Math.min(lo, img[z][y][x]);
        int[][][] g = new int[D][H][W];
        int G = 0;
        for (int z = 0; z < D; z++) for (int y = 0; y < H; y++) for (int x = 0; x < W; x++) {
            g[z][y][x] = (int) Math.floor(img[z][y][x] - lo) + 1;
            if (roi[z][y][x]) G = Math.max(G, g[z][y][x]);
        }
        double[] n = new double[G + 1], s = new double[G + 1];
        for (int z = 0; z < D; z++) for (int y = 0; y < H; y++) for (int x = 0; x < W; x++) {
            if (!roi[z][y][x]) continue;
            double sum = 0;
            int cnt = 0;
            for (int dz = -1; dz <= 1; dz++) for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dy == 0 && dz == 0) continue;
                int zz = z + dz, yy = y + dy, xx = x + dx;
                if (zz < 0 || yy < 0 || xx < 0 || zz >= D || yy >= H || xx >= W || !roi[zz][yy][xx]) continue;
                sum += g[zz][yy][xx];
                cnt++;
            }
            if (cnt == 0) continue; // IBSI: no neighbour, not counted
            n[g[z][y][x]]++;
            s[g[z][y][x]] += Math.abs(g[z][y][x] - sum / cnt);
        }
        double nv = 0, sumS = 0, ps = 0;
        for (int i = 1; i <= G; i++) nv += n[i];
        double[] p = new double[G + 1];
        int ngp = 0;
        for (int i = 1; i <= G; i++) { p[i] = n[i] / nv; sumS += s[i]; ps += p[i] * s[i]; if (p[i] > 0) ngp++; }
        double coarseness = ps == 0 ? 1.0E6 : 1.0 / ps;
        double contrast, busyness;
        if (ngp == 1) {
            contrast = 0;
            busyness = 0;
        } else {
            double c = 0, den = 0;
            for (int i = 1; i <= G; i++) for (int j = 1; j <= G; j++) if (p[i] > 0 && p[j] > 0) { c += p[i] * p[j] * (i - j) * (i - j); den += Math.abs(i * p[i] - j * p[j]); }
            contrast = c / (ngp * (ngp - 1.0)) * sumS / nv;
            busyness = ps / den;
        }
        double comp = 0, st = 0;
        for (int i = 1; i <= G; i++) for (int j = 1; j <= G; j++) if (i != j && p[i] > 0 && p[j] > 0) {
            comp += Math.abs(i - j) * (p[i] * s[i] + p[j] * s[j]) / (p[i] + p[j]);
            st += (p[i] + p[j]) * (i - j) * (i - j);
        }
        return new double[] { coarseness, contrast, busyness, comp / nv, sumS == 0 ? 0 : st / sumS };
    }

    private interface Img { float at(int x, int y, int z); }
    private interface In { boolean at(int x, int y, int z); }

    private static void check(String label, Img img, In roi, double[] pythonValues) throws Exception {
        ImagePlus[] v = TestVolumes.volume(W, H, D, img::at, roi::at);
        float[][][] a = new float[D][H][W];
        boolean[][][] r = new boolean[D][H][W];
        for (int z = 0; z < D; z++) for (int y = 0; y < H; y++) for (int x = 0; x < W; x++) { a[z][y][x] = img.at(x, y, z); r[z][y][x] = roi.at(x, y, z); }
        double[] got = ngtdm(v);
        double[] plain = reference(a, r);
        for (int i = 0; i < NAMES.length; i++) {
            assertEquals(plain[i], got[i], 1e-12 * Math.max(1, Math.abs(plain[i])), label + " " + NAMES[i] + " (plain implementation)");
            assertEquals(pythonValues[i], got[i], 1e-12 * Math.max(1, Math.abs(pythonValues[i])), label + " " + NAMES[i] + " (Python)");
        }
    }

    private static float formula(int x, int y, int z) {
        return (x * 3 + y * 5 + z * 7) % 9;
    }

    @Test
    void aNormalRegionIsUnchanged_asIn273() throws Exception {
        check("normal", NgtdmIbsiTest::formula, (x, y, z) -> x >= 2 && x < 10 && y >= 2 && y < 10 && z >= 1 && z < 5,
                new double[] { 0.014840914150289966, 0.43763503738931125, 2.4984952594266883, 70.87360501432808, 0.3947067099527026 });
    }

    @Test
    void aUniformRegion_coarseness1e6_contrastBusynessComplexityStrengthZero() throws Exception {
        check("uniform", (x, y, z) -> 5f, (x, y, z) -> x >= 2 && x < 10 && y >= 2 && y < 10 && z >= 1 && z < 5,
                new double[] { 1.0E6, 0.0, 0.0, 0.0, 0.0 });
    }

    /** Two flat blocks that do not touch (levels 2 and 7): every s_i is 0, n_i > 0 for two levels (N_g,p = 2, not 0). */
    @Test
    void twoSeparateFlatBlocks_ngpIsTwo_notZero() throws Exception {
        check("two blocks", (x, y, z) -> (x < 4 && y < 4) ? 2f : ((x >= 7 && y >= 7) ? 7f : 0f),
                (x, y, z) -> z >= 1 && z < 4 && ((x >= 1 && x < 4 && y >= 1 && y < 4) || (x >= 7 && x < 10 && y >= 7 && y < 10)),
                new double[] { 1.0E6, 0.0, 0.0, 0.0, 0.0 });
    }

    /** A block and one voxel far from it: the voxel has no neighbour in the region, so it is not counted (N_v,c = 144, not 145). */
    @Test
    void aVoxelWithoutNeighboursIsNotCounted() throws Exception {
        check("isolated voxel", (x, y, z) -> (x == 10 && y == 10 && z == 4) ? 8f : formula(x, y, z),
                (x, y, z) -> (x >= 2 && x < 8 && y >= 2 && y < 8 && z >= 1 && z < 5) || (x == 10 && y == 10 && z == 4),
                new double[] { 0.025838381265136417, 0.4479411444280724, 1.4513293079469554, 72.28509113435585, 0.6890235004036377 });
        ImagePlus[] v = TestVolumes.volume(W, H, D, (x, y, z) -> (x == 10 && y == 10 && z == 4) ? 8f : formula(x, y, z),
                (x, y, z) -> (x >= 2 && x < 8 && y >= 2 && y < 8 && z >= 1 && z < 5) || (x == 10 && y == 10 && z == 4));
        double nvc = 0;
        for (double[] row : new NGTDMFeatures(v[0], v[1], 1, 1, false, null, 1.0).getMatrix()) {
            nvc += row[1];
        }
        assertEquals(144.0, nvc, 0.0, "the sum of n_i");
    }

    /**
     * One voxel has no neighbour: N_v,c is 0, so p_i = 0/0 and the features are not defined (IBSI gives no value for it); they are NaN, not a number made up.
     * Strength is the exception: IBSI says it is 0 when the sum of s_i is 0, and it is.
     */
    @Test
    void aSingleVoxelRegion_isNotDefined() throws Exception {
        ImagePlus[] v = TestVolumes.volume(W, H, D, NgtdmIbsiTest::formula, (x, y, z) -> x == 5 && y == 5 && z == 2);
        double[] got = ngtdm(v);
        for (int i = 0; i < 4; i++) {
            assertTrue(Double.isNaN(got[i]), NAMES[i] + " is NaN: " + got[i]);
        }
        assertEquals(0.0, got[4], 0.0, "Strength: the sum of s_i is 0");
    }
}
