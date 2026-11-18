package io.github.tatsunidas.radiomics.features;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import ij.ImagePlus;

/**
 * A region on one slice has pairs only in the 4 in-plane angles of the 13; the 9 other angles have no matrix and are skipped,
 * but the unused entries of the array stayed 0 in the mean over 13: every GLCM feature was multiplied by about 4/13 = 0.3077.
 */
class GlcmSingleSliceTest {

    private static GLCMFeatures glcm(ImagePlus[] v) throws Exception {
        return new GLCMFeatures(v[0], v[1], 1, 1, true, 8, null, null);
    }

    /** A uniform image has one grey level: the joint maximum and the angular second moment are exactly 1. */
    @Test
    void aUniformRegionGivesOneOnOneSliceAndOnThreeSlices() throws Exception {
        TestVolumes.Px uniform = (x, y, z) -> 5f;
        double[] oneSlice = new double[2];
        int k = 0;
        for (ImagePlus[] v : new ImagePlus[][] {
                TestVolumes.volume(40, 40, 1, uniform),                                       // one slice
                TestVolumes.volume(40, 40, 3, uniform),                                       // three slices, all in the ROI
                TestVolumes.volume(40, 40, 3, uniform, (x, y, z) -> z == 1) }) {              // three slices, the middle one only
            GLCMFeatures f = glcm(v);
            Double asm = f.calculate(GLCMFeatureType.AngularSecondMoment.id());
            Double max = f.calculate(GLCMFeatureType.JointMaximum.id());
            assertEquals(1.0, asm, 1e-12, "AngularSecondMoment, case " + k);
            assertEquals(1.0, max, 1e-12, "JointMaximum, case " + k);
            k++;
        }
    }

    /**
     * A checkerboard of 2 levels on one slice: in the angles 0 and 90 degrees every neighbour differs, and in the 2 diagonals
     * every neighbour is equal. The mean over the 4 in-plane angles is what the features report.
     */
    @Test
    void theMeanIsOverTheAnglesThatHaveAMatrix() throws Exception {
        ImagePlus[] v = TestVolumes.volume(40, 40, 1, (x, y, z) -> ((x + y) % 2 == 0) ? 0f : 1f);
        GLCMFeatures f = glcm(v);
        // joint maximum per angle: about 0.5 for 0 and 90 degrees (pairs (1,2) and (2,1)) and about 0.5 for the diagonals
        // ((1,1) and (2,2); the borders of the board make the two counts differ a little), so the mean over the 4 angles is
        // about 0.5; the mean over 13 with 9 zeros would be 0.5 * 4 / 13 = 0.1538
        Double max = f.calculate(GLCMFeatureType.JointMaximum.id());
        assertEquals(0.5, max, 1e-3, "JointMaximum");
        for (GLCMFeatureType t : GLCMFeatureType.values()) {
            Double value = f.calculate(t.id());
            assertTrue(value != null, t.name());
        }
    }
}
