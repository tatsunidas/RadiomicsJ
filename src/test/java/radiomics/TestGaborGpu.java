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
import io.github.tatsunidas.radiomics.main.ImageFiltering.Pooling;
import io.github.tatsunidas.radiomics.main.RadiomicsJ;

/**
 * The Gabor response map on the GPU (direct convolution, 32 bit) against the CPU (FFT, 64 bit),
 * on a volume with unequal voxel spacing. Where no GPU is usable both runs are the CPU code,
 * the reason is kept, and the test says so instead of counting it as a GPU pass.
 */
public class TestGaborGpu {

	private static ImagePlus volume() {
		Random rnd = new Random(7);
		ImageStack st = new ImageStack(40, 33);
		for (int z = 0; z < 12; z++) {
			float[] p = new float[40 * 33];
			for (int i = 0; i < p.length; i++) {
				p[i] = (float) (rnd.nextGaussian() * 80 + 40);
			}
			st.addSlice(new FloatProcessor(40, 33, p));
		}
		ImagePlus imp = new ImagePlus("v", st);
		imp.getCalibration().pixelWidth = 0.9;
		imp.getCalibration().pixelHeight = 1.1;
		imp.getCalibration().pixelDepth = 2.5;
		return imp;
	}

	private static double[][] values(ImagePlus r) {
		double[][] v = new double[r.getStackSize()][];
		for (int z = 0; z < v.length; z++) {
			float[] f = (float[]) r.getStack().getProcessor(z + 1).getPixels();
			v[z] = new double[f.length];
			for (int i = 0; i < f.length; i++) {
				v[z][i] = f[i];
			}
		}
		return v;
	}

	/** largest difference as a fraction of the range of the CPU map, the measure of IBSI 2 section 7.1 */
	private static double gpuAgainstCpu(Padding pad, Pooling pool, double dTheta, boolean planar) {
		boolean saved = RadiomicsJ.BOOL_USE_GPU;
		try {
			RadiomicsJ.BOOL_USE_GPU = false;
			double[][] cpu = values(ImageFiltering.gaborFilter(volume(), 4, 3, 0.7, 0.4, pad, pool, dTheta, planar));
			RadiomicsJ.BOOL_USE_GPU = true;
			double[][] gpu = values(ImageFiltering.gaborFilter(volume(), 4, 3, 0.7, 0.4, pad, pool, dTheta, planar));
			double lo = Double.MAX_VALUE, hi = -Double.MAX_VALUE, worst = 0;
			for (int z = 0; z < cpu.length; z++) {
				for (int i = 0; i < cpu[z].length; i++) {
					lo = Math.min(lo, cpu[z][i]);
					hi = Math.max(hi, cpu[z][i]);
					worst = Math.max(worst, Math.abs(cpu[z][i] - gpu[z][i]));
				}
			}
			System.out.println("Gabor GPU vs CPU " + pad + " " + pool + " planar=" + planar + ": largest difference "
					+ worst + " (" + (100 * worst / (hi - lo)) + "% of the range)");
			return worst / (hi - lo);
		} finally {
			RadiomicsJ.BOOL_USE_GPU = saved;
		}
	}

	@Test
	public void gpuMapMatchesTheCpuMapOrTheCpuIsUsed() {
		boolean onGpu = Gpu.get() != null;
		System.out.println(onGpu ? "Gabor: run on the GPU" : "Gabor: no GPU, CPU code only (" + Gpu.unavailableReason() + ")");
		if (!onGpu) {
			assertNotNull(Gpu.unavailableReason());
		}
		for (Padding pad : Padding.values()) {
			assertTrue(gpuAgainstCpu(pad, null, 0, true) <= 1e-4, "single, " + pad);
			assertTrue(gpuAgainstCpu(pad, Pooling.AVERAGE, Math.PI / 4, false) <= 1e-4, "pooled 3D, " + pad);
		}
	}
}
