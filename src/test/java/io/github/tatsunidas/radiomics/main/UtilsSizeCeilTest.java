package io.github.tatsunidas.radiomics.main;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** ceil(n * spacing / new spacing) of a resampled axis: a result that is an integer up to the rounding must stay that integer. */
class UtilsSizeCeilTest {

    @Test
    void anExactIntegerStaysEvenWhenTheFloatingPointResultIsAFewUlpAbove() {
        // 100 * (0.55 / 1.0) = 55.00000000000001 in double: Math.ceil gave 56
        assertEquals(56, (int) Math.ceil(100 * (0.55 / 1.0)), "the plain ceil of this product is the cause");
        assertEquals(55, Utils.sizeCeil(100 * (0.55 / 1.0)));
        assertEquals(440, Utils.sizeCeil(512 * (0.859375 / 1.0)));
        assertEquals(70, Utils.sizeCeil(100 * (0.7 / 1.0)));
        assertEquals(420, Utils.sizeCeil(300 * (0.7 / 0.5)));
    }

    /** The functions themselves: 100 pixels of 0.55 mm resampled to 1 mm have 55 pixels (not 56). */
    @Test
    void theResamplingFunctionsGiveTheExactSize() {
        for (int k = 0; k < 2; k++) {
            ij.ImageStack st = new ij.ImageStack(100, 4);
            for (int z = 0; z < 4; z++) {
                st.addSlice(new ij.process.FloatProcessor(100, 4, new float[400]));
            }
            ij.ImagePlus imp = new ij.ImagePlus("i", st);
            imp.getCalibration().pixelWidth = 0.55;
            imp.getCalibration().pixelHeight = 1.0;
            imp.getCalibration().pixelDepth = 1.0;
            imp.getCalibration().setUnit("mm");
            ij.ImagePlus out = (k == 0) ? Utils.trilinearInterpolation(imp, false, 1.0, 1.0, 1.0)
                    : Utils.nearestNeighbourInterpolation(imp, 1.0, 1.0, 1.0);
            assertEquals(55, out.getWidth(), (k == 0 ? "trilinear" : "nearest neighbour") + ": the width");
            assertEquals(4, out.getHeight());
            assertEquals(4, out.getNSlices());
        }
    }

    @Test
    void aRealFractionIsStillRoundedUp() {
        assertEquals(359, Utils.sizeCeil(512 * (0.7 / 1.0)));   // 358.4
        assertEquals(410, Utils.sizeCeil(512 * (0.8 / 1.0)));   // 409.6
        assertEquals(11, Utils.sizeCeil(10.000001));            // one millionth above an integer is a real fraction
        assertEquals(10, Utils.sizeCeil(9.5));
    }

    @Test
    void everySizeOfTheCommonSpacingsIsTheExactCeil() {
        // the exact value is n * p / q for the integers p, q (the spacings in 1/1000 mm): the size is ceil of the exact quotient
        for (int n : new int[] { 64, 100, 128, 200, 256, 320, 512 }) {
            for (int p = 300; p <= 1500; p += 5) {          // 0.300 ... 1.500 mm
                for (int q : new int[] { 500, 1000, 1500, 2000 }) {
                    long exact = (long) Math.ceil((double) ((long) n * p) / q - 1e-12); // exact in rational arithmetic: n*p/q
                    long num = (long) n * p;
                    long expected = (num + q - 1) / q;         // integer ceil
                    assertEquals(expected, Utils.sizeCeil(n * (p / 1000.0) / (q / 1000.0)), "n=" + n + " p=" + p + " q=" + q + " exact=" + exact);
                }
            }
        }
    }
}
