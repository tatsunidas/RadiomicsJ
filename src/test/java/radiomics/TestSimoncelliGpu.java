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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.FloatProcessor;
import io.github.tatsunidas.radiomics.gpu.Gpu;
import io.github.tatsunidas.radiomics.main.ImageFiltering;
import io.github.tatsunidas.radiomics.main.ImageFiltering.Padding;
import io.github.tatsunidas.radiomics.main.RadiomicsJ;

/**
 * The Simoncelli response map on the GPU (single precision) against the CPU (double). Where no GPU is usable both runs
 * are the CPU code, the reason is kept, and the test says so instead of counting it as a GPU pass.
 */
public class TestSimoncelliGpu {

	private static ImagePlus volume(int w, int h, int d) {
		Random rnd = new Random(7);
		ImageStack st = new ImageStack(w, h);
		for (int z = 0; z < d; z++) {
			float[] p = new float[w * h];
			for (int i = 0; i < p.length; i++) {
				p[i] = (float) (rnd.nextGaussian() * 80 + 40);
			}
			st.addSlice(new FloatProcessor(w, h, p));
		}
		return new ImagePlus("v", st);
	}

	private static float[][] values(ImagePlus r) {
		float[][] v = new float[r.getStackSize()][];
		for (int z = 0; z < v.length; z++) {
			v[z] = ((float[]) r.getStack().getProcessor(z + 1).getPixels()).clone();
		}
		return v;
	}

	/** largest difference as a fraction of the range of the CPU map, the measure of IBSI 2 section 7.1 */
	private static double gpuAgainstCpu(int w, int h, int d, int level, Padding pad, boolean planar) {
		boolean saved = RadiomicsJ.BOOL_USE_GPU;
		try {
			RadiomicsJ.BOOL_USE_GPU = false;
			float[][] cpu = values(ImageFiltering.simoncelliFilter(volume(w, h, d), level, pad, planar));
			RadiomicsJ.BOOL_USE_GPU = true;
			float[][] gpu = values(ImageFiltering.simoncelliFilter(volume(w, h, d), level, pad, planar));
			double lo = Double.MAX_VALUE, hi = -Double.MAX_VALUE, worst = 0;
			for (int z = 0; z < cpu.length; z++) {
				for (int i = 0; i < cpu[z].length; i++) {
					lo = Math.min(lo, cpu[z][i]);
					hi = Math.max(hi, cpu[z][i]);
					worst = Math.max(worst, Math.abs(cpu[z][i] - gpu[z][i]));
				}
			}
			System.out.println("Simoncelli GPU vs CPU " + w + "x" + h + "x" + d + " level " + level + " " + pad + " planar="
					+ planar + ": largest difference " + worst + " (" + (100 * worst / (hi - lo)) + "% of the range)");
			return worst / (hi - lo);
		} finally {
			RadiomicsJ.BOOL_USE_GPU = saved;
		}
	}

	@Test
	public void gpuMapMatchesTheCpuMapOrTheCpuIsUsed() {
		boolean onGpu = Gpu.get() != null;
		System.out.println(onGpu ? "Simoncelli: run on the GPU" : "Simoncelli: no GPU, CPU code only (" + Gpu.unavailableReason() + ")");
		if (!onGpu) {
			assertNotNull(Gpu.unavailableReason());
		}
		for (Padding pad : Padding.values()) {
			// lines of 40, 33 and 12 voxels: Bluestein on every axis, whatever the margin
			assertTrue(gpuAgainstCpu(40, 33, 12, 1, pad, false) <= 1e-4, "3D, " + pad);
			assertTrue(gpuAgainstCpu(40, 33, 12, 2, pad, true) <= 1e-4, "planar, " + pad);
		}
		// powers of two, with PERIODIC the lines are not padded at all
		assertTrue(gpuAgainstCpu(32, 16, 8, 1, Padding.PERIODIC, false) <= 1e-4, "power of two");
	}

	@Test
	public void aLineTooLongForTheDeviceIsLeftToTheCpu() {
		// 3000 voxels need a padded line of 8192: the device path declines, the result is the CPU one bit for bit
		boolean saved = RadiomicsJ.BOOL_USE_GPU;
		try {
			RadiomicsJ.BOOL_USE_GPU = false;
			float[][] cpu = values(ImageFiltering.simoncelliFilter(volume(3000, 2, 1), 1, Padding.PERIODIC, false));
			RadiomicsJ.BOOL_USE_GPU = true;
			float[][] gpu = values(ImageFiltering.simoncelliFilter(volume(3000, 2, 1), 1, Padding.PERIODIC, false));
			for (int i = 0; i < cpu[0].length; i++) {
				assertEquals(cpu[0][i], gpu[0][i], 0f, "voxel " + i);
			}
		} finally {
			RadiomicsJ.BOOL_USE_GPU = saved;
		}
	}
}
