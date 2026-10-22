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
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Random;

import org.junit.jupiter.api.Test;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import io.github.tatsunidas.radiomics.features.LocalIntensityFeatures;
import io.github.tatsunidas.radiomics.gpu.Gpu;
import io.github.tatsunidas.radiomics.main.RadiomicsJ;

/**
 * The global intensity peak with the GPU in the search and without it must be the same double, bit for bit.
 * Where no GPU is usable both runs are the CPU code, the reason is kept, and the test says so.
 */
public class TestLocalIntensityGpu {

	private static ImagePlus[] volume(double px, double py, double pz, double offset) {
		Random rnd = new Random(3);
		int w = 30, h = 26, s = 14;
		ImageStack img = new ImageStack(w, h), msk = new ImageStack(w, h);
		for (int z = 0; z < s; z++) {
			float[] p = new float[w * h];
			byte[] m = new byte[w * h];
			for (int y = 0; y < h; y++) {
				for (int x = 0; x < w; x++) {
					p[y * w + x] = (float) (rnd.nextGaussian() * 60 + offset + 5 * Math.sin(x * 0.4));
					double dx = (x - 15) / 12.0, dy = (y - 13) / 10.0, dz = (z - 7) / 6.0;
					m[y * w + x] = (byte) (dx * dx + dy * dy + dz * dz <= 1 ? 1 : 0);
				}
			}
			img.addSlice(new FloatProcessor(w, h, p));
			msk.addSlice(new ByteProcessor(w, h, m));
		}
		ImagePlus[] o = { new ImagePlus("i", img), new ImagePlus("m", msk) };
		for (ImagePlus i : o) {
			i.getCalibration().pixelWidth = px;
			i.getCalibration().pixelHeight = py;
			i.getCalibration().pixelDepth = pz;
		}
		return o;
	}

	private static double peak(ImagePlus[] v, boolean gpu) {
		boolean saved = RadiomicsJ.BOOL_USE_GPU;
		try {
			RadiomicsJ.BOOL_USE_GPU = gpu;
			return new LocalIntensityFeatures(v[0], v[1], 1).calculate("0F91");
		} finally {
			RadiomicsJ.BOOL_USE_GPU = saved;
		}
	}

	@Test
	public void gpuPeakEqualsTheCpuPeakBitForBit() {
		boolean onGpu = Gpu.get() != null;
		System.out.println(onGpu ? "LocalIntensity: run on the GPU"
				: "LocalIntensity: no GPU, CPU code only (" + Gpu.unavailableReason() + ")");
		if (!onGpu) {
			assertNotNull(Gpu.unavailableReason());
		}
		double[][] cases = { { 1, 1, 1, 100 }, { 0.9, 1.1, 2.5, 100 }, { 2, 2, 5, -300 }, { 8, 8, 8, 40 } };
		for (double[] c : cases) {
			double cpu = peak(volume(c[0], c[1], c[2], c[3]), false);
			double gpu = peak(volume(c[0], c[1], c[2], c[3]), true);
			System.out.println("spacing " + c[0] + "," + c[1] + "," + c[2] + " offset " + c[3] + ": CPU " + cpu + " GPU " + gpu);
			assertEquals(Double.doubleToLongBits(cpu), Double.doubleToLongBits(gpu));
		}
	}
}
