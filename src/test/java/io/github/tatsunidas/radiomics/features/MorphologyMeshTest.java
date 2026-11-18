package io.github.tatsunidas.radiomics.features;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import javax.vecmath.Point3f;

import org.junit.jupiter.api.Test;

import ij.ImagePlus;

/**
 * The mesh volume added float products measured from the origin of the image: the same shape had a different volume at each
 * position. The surface area measured the distance between the cross product and the origin of the calibration instead of
 * the length of the cross product: it was wrong for an image whose origin is not 0.
 */
class MorphologyMeshTest {

    private static ImagePlus[] sphere(int w, int h, int s, int cx, int cy, int cz, double r) {
        return TestVolumes.volume(w, h, s, (x, y, z) -> 100f,
                (x, y, z) -> Math.hypot(Math.hypot(x - cx, y - cy), z - cz) <= r);
    }

    /** The volume of the same mesh in double, relative to the centroid of the vertices. */
    private static double referenceVolume(List<Point3f> pts) {
        double cx = 0, cy = 0, cz = 0;
        for (Point3f p : pts) {
            cx += p.x;
            cy += p.y;
            cz += p.z;
        }
        cx /= pts.size();
        cy /= pts.size();
        cz /= pts.size();
        double v = 0;
        for (int i = 0; i + 2 < pts.size(); i += 3) {
            Point3f p1 = pts.get(i), p2 = pts.get(i + 1), p3 = pts.get(i + 2);
            double x1 = p1.x - cx, y1 = p1.y - cy, z1 = p1.z - cz;
            double x2 = p2.x - cx, y2 = p2.y - cy, z2 = p2.z - cz;
            double x3 = p3.x - cx, y3 = p3.y - cy, z3 = p3.z - cz;
            v += x1 * (y2 * z3 - y3 * z2) - x2 * (y1 * z3 - y3 * z1) + x3 * (y1 * z2 - y2 * z1);
        }
        return Math.abs(v / 6.0);
    }

    @Test
    void theMeshVolumeDoesNotDependOnThePosition() {
        int[][] centres = { { 40, 40, 40 }, { 128, 128, 60 }, { 215, 215, 80 } };
        Double first = null;
        for (int[] c : centres) {
            ImagePlus[] v = sphere(256, 256, 120, c[0], c[1], c[2], 20);
            MorphologicalFeatures f = new MorphologicalFeatures(v[0], v[1], 1);
            Double volume = f.calculate(MorphologicalFeatureType.VolumeMesh.id());
            double ref = referenceVolume(f.points);
            assertEquals(ref, volume, ref * 1e-9, "the volume of the mesh at " + c[0] + "," + c[1] + "," + c[2]);
            if (first == null) {
                first = volume;
            }
            assertEquals(first, volume, first * 1e-9, "the same shape at another position");
        }
        assertTrue(first > 30000 && first < 36000, "a sphere of radius 20: about 33,500 mm3, got " + first);
    }

    @Test
    void theSurfaceAreaDoesNotDependOnTheOriginOfTheCalibration() {
        double[] areas = new double[3];
        double[] origins = { 0, 50, -300 };
        for (int k = 0; k < origins.length; k++) {
            ImagePlus[] v = sphere(64, 64, 64, 32, 32, 32, 12);
            v[0].getCalibration().xOrigin = origins[k];
            v[0].getCalibration().yOrigin = origins[k];
            v[0].getCalibration().zOrigin = origins[k];
            MorphologicalFeatures f = new MorphologicalFeatures(v[0], v[1], 1);
            areas[k] = f.calculate(MorphologicalFeatureType.SurfaceAreaMesh.id());
        }
        assertTrue(areas[0] > 1700 && areas[0] < 1950, "a sphere of radius 12: 4 pi r^2 = 1810, got " + areas[0]);
        assertEquals(areas[0], areas[1], areas[0] * 1e-9, "origin 50");
        assertEquals(areas[0], areas[2], areas[0] * 1e-9, "origin -300");
    }
}
