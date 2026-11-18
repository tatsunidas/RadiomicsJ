package io.github.tatsunidas.radiomics.features;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import ij.ImagePlus;

/**
 * The central difference (h[i+1] - h[i-1]) / 2 of the integer histogram was an integer division (0.5 was cut off) and the maximum
 * and the minimum started from 0 (a histogram of only negative gradients gave a maximum of 0 and the bin 0).
 */
class HistogramGradientTest {

    /** The voxels of a 1-row image; the values 1..nBins are the bins (no change by the discretisation). */
    private static IntensityHistogramFeatures of(int nBins, float... values) throws Exception {
        ImagePlus[] v = TestVolumes.volume(values.length, 1, 1, (x, y, z) -> values[x]);
        return new IntensityHistogramFeatures(v[0], v[1], 1, true, nBins, null);
    }

    private static double get(IntensityHistogramFeatures f, IntensityHistogramFeatureType t) {
        return f.calculate(t.id());
    }

    /** The histogram [1, 1, 4, 1]: the gradients are 0, (4-1)/2 = 1.5, (1-1)/2 = 0, 1-4 = -3. */
    @Test
    void anOddDifferenceKeepsTheHalf() throws Exception {
        IntensityHistogramFeatures f = of(4, 1, 2, 3, 3, 3, 3, 4);
        assertEquals(1.5, get(f, IntensityHistogramFeatureType.MaximumHistogramGradient), 1e-12);
        assertEquals(2.0, get(f, IntensityHistogramFeatureType.MaximumHistogramGradientIntensity), 1e-12);
        assertEquals(-3.0, get(f, IntensityHistogramFeatureType.MinimumHistogramGradient), 1e-12);
        assertEquals(4.0, get(f, IntensityHistogramFeatureType.MinimumHistogramGradientIntensity), 1e-12);
    }

    /** The histogram [5, 3, 1]: every gradient is -2 (the maximum is negative, not 0; the first bin has it). */
    @Test
    void aHistogramOfOnlyNegativeGradients() throws Exception {
        IntensityHistogramFeatures f = of(3, 1, 1, 1, 1, 1, 2, 2, 2, 3);
        assertEquals(-2.0, get(f, IntensityHistogramFeatureType.MaximumHistogramGradient), 1e-12);
        assertEquals(1.0, get(f, IntensityHistogramFeatureType.MaximumHistogramGradientIntensity), 1e-12);
        assertEquals(-2.0, get(f, IntensityHistogramFeatureType.MinimumHistogramGradient), 1e-12);
        assertEquals(1.0, get(f, IntensityHistogramFeatureType.MinimumHistogramGradientIntensity), 1e-12);
    }

    /** The histogram [1, 3, 5]: every gradient is +2 (the minimum is positive, not 0). */
    @Test
    void aHistogramOfOnlyPositiveGradients() throws Exception {
        IntensityHistogramFeatures f = of(3, 1, 2, 2, 2, 3, 3, 3, 3, 3);
        assertEquals(2.0, get(f, IntensityHistogramFeatureType.MaximumHistogramGradient), 1e-12);
        assertEquals(2.0, get(f, IntensityHistogramFeatureType.MinimumHistogramGradient), 1e-12);
        assertEquals(1.0, get(f, IntensityHistogramFeatureType.MinimumHistogramGradientIntensity), 1e-12);
    }
}
