package io.github.tatsunidas.radiomics.features;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;

import javax.vecmath.Point3i;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import ij.ImagePlus;

/**
 * The zones (26-connected sets of voxels of one grey level) are found by a breadth first search over a visited array.
 * The search used to remove visited voxels from lists, which took about n^7 of the edge length n for one large zone
 * (47 s for 41^3, 411 s for 55^3 in 2.7.3).
 *
 * The zones must be the same as before: they are compared with a copy of the former search and with a plain queue search written for this test,
 * and the features of three volumes are compared with the values 2.7.3 gave for the same volumes.
 */
class GldzmBlobsTest {

    // ---- the former search (2.7.3), copied; the neighbours are the 26 offsets ----

    private static final int[][] OFFS26 = offsets26();

    private static int[][] offsets26() {
        List<int[]> l = new ArrayList<>();
        for (int dz = -1; dz <= 1; dz++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    if (dx != 0 || dy != 0 || dz != 0) {
                        l.add(new int[] { dx, dy, dz });
                    }
                }
            }
        }
        return l.toArray(new int[0][]);
    }

    private static ArrayList<Point3i> formerConnected(Integer[][][] v, int gray, int sx, int sy, int sz, int w, int h, int s) {
        ArrayList<Point3i> connected = new ArrayList<>();
        for (int[] o : OFFS26) {
            int nx = sx + o[0], ny = sy + o[1], nz = sz + o[2];
            if (nx < 0 || nx >= w || ny < 0 || ny >= h || nz < 0 || nz >= s) {
                continue;
            }
            if (v[nz][ny][nx] != null && v[nz][ny][nx] == gray) {
                connected.add(new Point3i(nx, ny, nz));
            }
        }
        return connected.size() > 0 ? connected : null;
    }

    private static ArrayList<Point3i> formerSearch(int gray, Integer[][][] px, int sx, int sy, int sz, int w, int h, int s) {
        if (px[sz][sy][sx] == null || px[sz][sy][sx] == Integer.MIN_VALUE || px[sz][sy][sx] != gray) {
            return null;
        }
        px[sz][sy][sx] = Integer.MIN_VALUE;
        ArrayList<Point3i> blob = new ArrayList<>();
        blob.add(new Point3i(sx, sy, sz));
        ArrayList<Point3i> connected = formerConnected(px, gray, sx, sy, sz, w, h, s);
        while (connected != null) {
            ArrayList<Point3i> itr = new ArrayList<>();
            for (Point3i np : connected) {
                if (px[np.z][np.y][np.x] == null || px[np.z][np.y][np.x] == Integer.MIN_VALUE) {
                    continue;
                }
                if (px[np.z][np.y][np.x] == gray) {
                    blob.add(new Point3i(np.x, np.y, np.z));
                    px[np.z][np.y][np.x] = Integer.MIN_VALUE;
                    ArrayList<Point3i> c = formerConnected(px, gray, np.x, np.y, np.z, w, h, s);
                    if (c != null) {
                        itr.addAll(c);
                    }
                }
            }
            connected.addAll(itr);
            ArrayList<Point3i> remove = new ArrayList<>();
            for (Point3i p : connected) {
                if (px[p.z][p.y][p.x] == null || px[p.z][p.y][p.x] == Integer.MIN_VALUE) {
                    remove.add(p);
                }
            }
            connected.removeAll(remove);
            connected = connected.isEmpty() ? null : connected;
        }
        return blob;
    }

    private static List<ArrayList<Point3i>> formerBlobs(Integer[][][] src) {
        int s = src.length, h = src[0].length, w = src[0][0].length;
        Integer[][][] px = new Integer[s][h][w];
        for (int z = 0; z < s; z++) for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) px[z][y][x] = src[z][y][x] == null ? Integer.MIN_VALUE : src[z][y][x];
        List<ArrayList<Point3i>> blobs = new ArrayList<>();
        for (int z = 0; z < s; z++) for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            if (px[z][y][x] != Integer.MIN_VALUE) {
                blobs.add(formerSearch(px[z][y][x], px, x, y, z, w, h, s));
            }
        }
        return blobs;
    }

    // ---- a plain queue search, written for this test ----

    private static List<ArrayList<Point3i>> queueBlobs(Integer[][][] src) {
        int s = src.length, h = src[0].length, w = src[0][0].length;
        boolean[][][] seen = new boolean[s][h][w];
        List<ArrayList<Point3i>> blobs = new ArrayList<>();
        for (int z = 0; z < s; z++) for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            if (src[z][y][x] == null || seen[z][y][x]) {
                continue;
            }
            int g = src[z][y][x];
            ArrayList<Point3i> blob = new ArrayList<>();
            ArrayDeque<int[]> q = new ArrayDeque<>();
            q.add(new int[] { x, y, z });
            seen[z][y][x] = true;
            while (!q.isEmpty()) {
                int[] c = q.poll();
                blob.add(new Point3i(c[0], c[1], c[2]));
                for (int dz = -1; dz <= 1; dz++) for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) {
                    int nx = c[0] + dx, ny = c[1] + dy, nz = c[2] + dz;
                    if (nx < 0 || ny < 0 || nz < 0 || nx >= w || ny >= h || nz >= s || seen[nz][ny][nx] || src[nz][ny][nx] == null) {
                        continue;
                    }
                    if (src[nz][ny][nx] == g) {
                        seen[nz][ny][nx] = true;
                        q.add(new int[] { nx, ny, nz });
                    }
                }
            }
            blobs.add(blob);
        }
        return blobs;
    }

    /** The set of zones, each as the sorted set of its voxels, so the order of the voxels and of the zones does not matter. */
    private static TreeSet<String> asSet(List<ArrayList<Point3i>> blobs) {
        TreeSet<String> out = new TreeSet<>();
        for (ArrayList<Point3i> b : blobs) {
            TreeSet<String> vox = new TreeSet<>();
            for (Point3i p : b) {
                vox.add(String.format("%03d,%03d,%03d", p.z, p.y, p.x));
            }
            out.add(String.join(";", vox));
        }
        return out;
    }

    private static Integer[][][] randomVolume(long seed, int w, int h, int s, int levels, double fill) {
        Random r = new Random(seed);
        Integer[][][] v = new Integer[s][h][w];
        for (int z = 0; z < s; z++) for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            if (r.nextDouble() < fill) {
                // blocky: neighbouring voxels often share a level, so the zones are neither single voxels nor one block
                v[z][y][x] = 1 + (int) (levels * Math.abs(Math.sin(0.35 * x + 0.5 * y + 0.7 * z + r.nextInt(2))));
            }
        }
        return v;
    }

    @Test
    void zonesAreTheSameAsTheFormerSearchAndAPlainQueueSearch() {
        GLDZMFeatures f = new GLDZMFeatures(1);
        for (long seed = 1; seed <= 6; seed++) {
            Integer[][][] src = randomVolume(seed, 13, 11, 8, 4, seed % 2 == 0 ? 0.9 : 0.6);
            TreeSet<String> now = asSet(f.collectBlobs(src));
            TreeSet<String> former = asSet(formerBlobs(src));
            TreeSet<String> queue = asSet(queueBlobs(src));
            assertTrue(now.size() > 20, "seed " + seed + ": there are many zones: " + now.size());
            assertEquals(former, now, "seed " + seed + ": the same zones as the former search");
            assertEquals(queue, now, "seed " + seed + ": the same zones as a plain queue search");
        }
    }

    @Test
    void aVoxelOutsideTheRegionIsNotPartOfAnyZone() {
        Integer[][][] src = new Integer[3][3][3];
        src[1][1][1] = 2;
        src[1][1][2] = 2;
        src[0][0][0] = 2; // 26-connected with nothing else? (0,0,0) and (1,1,1) are diagonal neighbours
        List<ArrayList<Point3i>> blobs = new GLDZMFeatures(1).collectBlobs(src);
        assertEquals(1, blobs.size());
        assertEquals(3, blobs.get(0).size(), "the diagonal neighbour joins the zone (26-connected)");
    }

    // ---- the features of three volumes, as 2.7.3 gave them ----

    private static ImagePlus[] v1() {
        return TestVolumes.volume(14, 14, 10, (x, y, z) -> ((x * 7 + y * 13 + z * 5 + (x * y) % 5) % 6) * 40f,
                (x, y, z) -> (x - 7) * (x - 7) / 36.0 + (y - 7) * (y - 7) / 36.0 + (z - 5) * (z - 5) / 16.0 <= 1.0);
    }

    private static ImagePlus[] v2() {
        return TestVolumes.volume(20, 20, 12, (x, y, z) -> ((x / 4 + y / 4 + z / 3) % 3) * 50f,
                (x, y, z) -> x >= 2 && x < 18 && y >= 2 && y < 18 && z >= 1 && z < 11);
    }

    /** Zones away from the edge: a ball (level 1) and a smaller ball (level 2) in a region of level 0, so the distances are larger than 1. */
    private static ImagePlus[] v3() {
        return TestVolumes.volume(22, 22, 16, (x, y, z) -> {
            double d1 = Math.hypot(Math.hypot(x - 9, y - 9), (z - 7) * 1.0);
            double d2 = Math.hypot(Math.hypot(x - 14, y - 14), (z - 8) * 1.0);
            return d1 <= 3.5 ? 60f : (d2 <= 2.0 ? 120f : 0f);
        }, (x, y, z) -> x >= 2 && x < 20 && y >= 2 && y < 20 && z >= 1 && z < 15);
    }

    private static void assertFeatures(ImagePlus[] v, double binWidth, double[] expected) throws Exception {
        GLDZMFeatures g = new GLDZMFeatures(v[0], v[1], 1, false, null, binWidth);
        GLDZMFeatureType[] types = GLDZMFeatureType.values();
        assertEquals(expected.length, types.length);
        for (int i = 0; i < types.length; i++) {
            assertEquals(expected[i], g.calculate(types[i].id()), 1e-12, types[i].name());
        }
    }

    @Test
    void featuresAreThoseOf273_forThreeVolumes() throws Exception {
        assertFeatures(v1(), 40.0, new double[] { 1.0, 1.0, 0.3222222222222222, 14.65625, 0.3222222222222222, 14.65625, 0.3222222222222222, 14.65625,
                5.6875, 0.177734375, 32.0, 1.0, 0.0547008547008547, 3.4755859375, 0.0, 2.5397170014627597 });
        assertFeatures(v2(), 50.0, new double[] { 1.0, 1.0, 0.45370370370370366, 4.666666666666666, 0.45370370370370366, 4.666666666666666,
                0.45370370370370366, 4.666666666666666, 1.0, 0.3333333333333333, 3.0, 1.0, 0.001171875, 0.6666666666666666, 0.0, 1.5849625007212946 });
        assertFeatures(v3(), 60.0, new double[] { 0.375, 11.0, 0.45370370370370366, 4.666666666666666, 0.34085648148148145, 0.6041666666666666,
                2.259259259259259, 69.66666666666666, 1.0, 0.3333333333333333, 1.6666666666666667, 0.5555555555555556, 6.613756613756613E-4,
                0.6666666666666666, 1.9999999999999998, 1.584962500721364 });
    }

    /** One large zone: 41^3 took 47 s in 2.7.3. The limit is loose (a slow machine), but far below the old time. */
    @Test
    @Timeout(30)
    void oneLargeZoneIsFound_inLinearTime() throws Exception {
        int n = 41;
        ImagePlus[] v = TestVolumes.volume(n + 4, n + 4, n + 4, (x, y, z) -> 100f, (x, y, z) -> x >= 2 && x < n + 2 && y >= 2 && y < n + 2 && z >= 2 && z < n + 2);
        GLDZMFeatures g = new GLDZMFeatures(v[0], v[1], 1, false, null, 30.0);
        assertEquals(1.0, g.calculate(GLDZMFeatureType.SmallDistanceEmphasis.id()), 1e-12);
        assertEquals(1.0 / ((double) n * n * n), g.calculate(GLDZMFeatureType.ZonePercentage.id()), 1e-15, "one zone in n^3 voxels");
    }
}
