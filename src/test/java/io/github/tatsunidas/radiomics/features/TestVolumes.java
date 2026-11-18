package io.github.tatsunidas.radiomics.features;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.FloatProcessor;

/** Small synthetic volumes for the tests of the features. */
final class TestVolumes {

    private TestVolumes() {
    }

    interface Px {
        float at(int x, int y, int z);
    }

    interface Roi {
        boolean in(int x, int y, int z);
    }

    /** An image and a mask (label 1). The calibration is 1 mm in every direction. */
    static ImagePlus[] volume(int w, int h, int s, Px image, Roi roi) {
        ImageStack img = new ImageStack(w, h);
        ImageStack msk = new ImageStack(w, h);
        for (int z = 0; z < s; z++) {
            float[] ip = new float[w * h];
            float[] mp = new float[w * h];
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    ip[y * w + x] = image.at(x, y, z);
                    mp[y * w + x] = roi.in(x, y, z) ? 1f : 0f;
                }
            }
            img.addSlice(new FloatProcessor(w, h, ip));
            msk.addSlice(new FloatProcessor(w, h, mp));
        }
        ImagePlus i = new ImagePlus("img", img);
        ImagePlus m = new ImagePlus("mask", msk);
        for (ImagePlus p : new ImagePlus[] { i, m }) {
            p.getCalibration().pixelWidth = 1;
            p.getCalibration().pixelHeight = 1;
            p.getCalibration().pixelDepth = 1;
            p.getCalibration().setUnit("mm");
        }
        return new ImagePlus[] { i, m };
    }

    static ImagePlus[] volume(int w, int h, int s, Px image) {
        return volume(w, h, s, image, (x, y, z) -> true);
    }

    /** All the voxels but the first row of the first slice are 0; that row is 10: two grey levels, one big block of zeros. */
    static ImagePlus[] bigBlock(int w, int h, int s) {
        return volume(w, h, s, (x, y, z) -> (z == 0 && y == 0) ? 10f : 0f);
    }
}
