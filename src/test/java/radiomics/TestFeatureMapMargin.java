/*
 * Copyright [2026] [Tatsuaki Kobayashi]

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package radiomics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.FloatProcessor;
import io.github.tatsunidas.radiomics.features.GLCMFeatureType;
import io.github.tatsunidas.radiomics.features.GLCMFeatures;
import io.github.tatsunidas.radiomics.features.RadiomicsFeature;
import io.github.tatsunidas.radiomics.main.FeatureVisualizationMap;
import io.github.tatsunidas.radiomics.main.RadiomicsJ;

/**
 * The margin around the roi in the feature visualization map.
 *
 * A voxel on the edge of the roi used to be measured in a window that the roi
 * only partly filled, so its value differed from a voxel in the middle for that
 * reason alone. Growing the mask first gives those voxels a full neighbourhood.
 *
 * @author tatsunidas <t_kobayashi@vis-ionary.com>
 *         (implemented with the coding assistant Claude, Anthropic)
 */
public class TestFeatureMapMargin {

	private static final int SIZE = 24;
	private static final int FILTER = 7;

	public static void main(String[] args) throws Exception {
		TestFeatureMapMargin test = new TestFeatureMapMargin();
		test.marginKeepsTheGeometryAndFixesTheEdge();
		test.roiTouchingTheImageBorderIsPaddedNotClipped();
		System.out.println("feature map margin: all clear.");
	}

	/**
	 * The map keeps the geometry of the input, and the edge of the roi stops
	 * standing out against its interior.
	 */
	@Test
	public void marginKeepsTheGeometryAndFixesTheEdge() throws Exception {
		ImagePlus image = texture();
		// a roi well inside the image, so only the margin is at play
		ImagePlus mask = box(6, SIZE - 7);

		ImagePlus withoutMargin = map(image, mask, 0);
		ImagePlus withMargin = map(image, mask, 3);

		assertEquals(SIZE, withMargin.getWidth());
		assertEquals(SIZE, withMargin.getHeight());
		assertEquals(withoutMargin.getWidth(), withMargin.getWidth());
		assertEquals(withoutMargin.getHeight(), withMargin.getHeight());
		assertEquals(withoutMargin.getNSlices(), withMargin.getNSlices());

		int z = SIZE / 2;
		double edgeGapWithout = edgeAgainstInterior(withoutMargin, mask, z);
		double edgeGapWith = edgeAgainstInterior(withMargin, mask, z);
		assertTrue(edgeGapWith < edgeGapWithout,
				"the margin should bring the roi edge closer to its interior, "
						+ edgeGapWithout + " -> " + edgeGapWith);

		// outside the roi nothing is written, exactly as before
		// only the requested slice is produced, so the output stack holds one plane
		FloatProcessor ip = withMargin.getStack().getProcessor(1).convertToFloatProcessor();
		FloatProcessor maskIp = mask.getStack().getProcessor(z + 1).convertToFloatProcessor();
		for (int y = 0; y < SIZE; y++) {
			for (int x = 0; x < SIZE; x++) {
				if (maskIp.getf(x, y) < 1) {
					assertEquals(0f, ip.getf(x, y), 0f, "outside the roi the map stays empty");
				}
			}
		}
	}

	/**
	 * When the roi runs into the edge of the image there is nothing to grow into,
	 * so the image is padded first. The map must still cover the whole roi.
	 */
	@Test
	public void roiTouchingTheImageBorderIsPaddedNotClipped() throws Exception {
		ImagePlus image = texture();
		ImagePlus mask = box(0, SIZE - 1);// the roi is the whole image

		ImagePlus withMargin = map(image, mask, 3);
		assertEquals(SIZE, withMargin.getWidth());
		assertEquals(SIZE, withMargin.getHeight());

		int z = SIZE / 2;
		FloatProcessor ip = withMargin.getStack().getProcessor(1).convertToFloatProcessor();
		int written = 0;
		for (int y = 0; y < SIZE; y++) {
			for (int x = 0; x < SIZE; x++) {
				if (ip.getf(x, y) != 0f) {
					written++;
				}
			}
		}
		assertEquals(SIZE * SIZE, written, "every roi voxel should carry a value, corners included");
	}

	// ------------------------------------------------------------------

	private ImagePlus map(ImagePlus image, ImagePlus mask, int margin) throws Exception {
		RadiomicsJ.resetSettings();
		Map<String, Object> settings = new HashMap<>();
		settings.put(RadiomicsFeature.LABEL, Integer.valueOf(1));
		settings.put(RadiomicsFeature.USE_BIN_COUNT, Boolean.TRUE);
		settings.put(RadiomicsFeature.nBins, Integer.valueOf(8));
		Map<String, ImagePlus> maps = FeatureVisualizationMap.generate(image, mask, SIZE / 2 + 1, FILTER, true, 1,
				margin, GLCMFeatures.class, settings, GLCMFeatureType.JointEntropy);
		RadiomicsJ.resetSettings();
		return maps.values().iterator().next();
	}

	/**
	 * How far the ring of roi voxels on the edge sits from the mean of the
	 * interior. A homogeneous texture should give roughly the same value
	 * everywhere, so a large gap means the window, not the texture, is talking.
	 */
	private double edgeAgainstInterior(ImagePlus featureMap, ImagePlus mask, int z) {
		FloatProcessor ip = featureMap.getStack().getProcessor(1).convertToFloatProcessor();
		FloatProcessor maskIp = mask.getStack().getProcessor(z + 1).convertToFloatProcessor();
		double edgeSum = 0d;
		int edgeCount = 0;
		double innerSum = 0d;
		int innerCount = 0;
		int w = featureMap.getWidth();
		int h = featureMap.getHeight();
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				if (maskIp.getf(x, y) < 1) {
					continue;
				}
				boolean onEdge = false;
				for (int dy = -1; dy <= 1 && !onEdge; dy++) {
					for (int dx = -1; dx <= 1; dx++) {
						int xx = x + dx;
						int yy = y + dy;
						if (xx < 0 || yy < 0 || xx >= w || yy >= h || maskIp.getf(xx, yy) < 1) {
							onEdge = true;
							break;
						}
					}
				}
				if (onEdge) {
					edgeSum += ip.getf(x, y);
					edgeCount++;
				} else {
					innerSum += ip.getf(x, y);
					innerCount++;
				}
			}
		}
		return Math.abs(edgeSum / edgeCount - innerSum / innerCount);
	}

	/** A homogeneous random texture, so any spatial trend comes from the window. */
	private ImagePlus texture() {
		java.util.Random random = new java.util.Random(20260802L);
		ImageStack stack = new ImageStack(SIZE, SIZE);
		for (int z = 0; z < SIZE; z++) {
			FloatProcessor ip = new FloatProcessor(SIZE, SIZE);
			for (int y = 0; y < SIZE; y++) {
				for (int x = 0; x < SIZE; x++) {
					ip.setf(x, y, random.nextInt(100));
				}
			}
			stack.addSlice(ip);
		}
		return calibrated(new ImagePlus("texture", stack));
	}

	private ImagePlus box(int from, int to) {
		ImageStack stack = new ImageStack(SIZE, SIZE);
		for (int z = 0; z < SIZE; z++) {
			FloatProcessor ip = new FloatProcessor(SIZE, SIZE);
			for (int y = from; y <= to; y++) {
				for (int x = from; x <= to; x++) {
					ip.setf(x, y, 1f);
				}
			}
			stack.addSlice(ip);
		}
		return calibrated(new ImagePlus("mask", stack));
	}

	private ImagePlus calibrated(ImagePlus imp) {
		imp.getCalibration().pixelWidth = 1d;
		imp.getCalibration().pixelHeight = 1d;
		imp.getCalibration().pixelDepth = 1d;
		return imp;
	}
}
