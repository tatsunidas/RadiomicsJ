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
package io.github.tatsunidas.radiomics.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.tatsunidas.radiomics.gpu.GpuEstimator.Estimate;
import io.github.tatsunidas.radiomics.gpu.GpuEstimator.Problem;
import io.github.tatsunidas.radiomics.gpu.GpuEstimator.Task;

/**
 * The fit and the calibration file. The measuring itself takes about a minute and depends on the machine, so it is
 * not run here: a calibration file with known numbers stands in, and the estimate must come out of those numbers.
 */
public class TestGpuEstimator {

	@TempDir
	File dir;

	@BeforeEach
	public void pointToTemporaryFile() {
		System.setProperty("radiomicsj.calibration", new File(dir, "cal.json").getPath());
		GpuEstimator.forget();
	}

	@AfterEach
	public void restore() {
		System.clearProperty("radiomicsj.calibration");
		GpuEstimator.forget();
	}

	@Test
	public void fitFindsTheCoefficientsOfExactData() {
		double[][] x = { { 1e6, 1e3 }, { 4e6, 2e3 }, { 9e6, 5e3 }, { 2e7, 4e3 }, { 5e7, 9e3 } };
		double[] y = new double[x.length];
		for (int i = 0; i < x.length; i++) {
			y[i] = 0.05 + 2e-8 * x[i][0] + 3e-5 * x[i][1];
		}
		double[] f = GpuEstimator.fit(x, y);
		assertEquals(0.05, f[0], 1e-6);
		assertEquals(2e-8, f[1], 1e-12);
		assertEquals(3e-5, f[2], 1e-9);
	}

	@Test
	public void fitNeverGivesANegativeCoefficient() {
		// time that falls with the data size is noise: that term is dropped, the others kept
		double[][] x = { { 1e6, 1 }, { 2e6, 2 }, { 3e6, 3 }, { 4e6, 4 } };
		double[] y = { 1.1, 2.0, 2.9, 4.0 };
		double[] f = GpuEstimator.fit(x, y);
		for (double c : f) {
			assertTrue(c >= 0, "coefficient " + c);
		}
		assertEquals(2.0, GpuEstimator.predict(new double[] { f[0], f[1], f[2], 0 }, 2e6, 2), 0.3);
	}

	private Map<String, double[]> known(boolean withGpu) {
		Map<String, double[]> m = new LinkedHashMap<>();
		for (Task t : Task.values()) {
			m.put(t + ".cpu", new double[] { 0.1, 1e-8, 0, 1e9 });
			if (withGpu) {
				m.put(t + ".gpu", new double[] { 0.5, 1e-10, 0, 1e9 });
			}
		}
		return m;
	}

	@Test
	public void estimateComesFromTheStoredCalibrationAndTellsWhoWinsEitherWay() {
		GpuEstimator.save(GpuEstimator.key(), known(true));
		// small: the GPU's fixed 0.5 s loses to the CPU; large: the GPU's 100 times smaller slope wins
		Problem small = Problem.maxDiameter(1000);// work 1e6
		Problem large = Problem.maxDiameter(100000);// work 1e10
		List<Estimate> es = GpuEstimator.estimate(false, small, large);
		assertEquals(0.1 + 1e-8 * 1e6, es.get(0).cpuSeconds, 1e-9);
		assertEquals(0.5 + 1e-10 * 1e6, es.get(0).gpuSeconds, 1e-9);
		assertFalse(es.get(0).recommendGpu());
		assertTrue(es.get(1).recommendGpu());
		assertEquals(es.get(1).cpuSeconds / es.get(1).gpuSeconds, es.get(1).speedup(), 1e-12);
		assertFalse(es.get(1).extrapolated);// 1e10 is 10 times the measured 1e9, the limit itself is not beyond it
		assertTrue(GpuEstimator.estimate(false, Problem.maxDiameter(200000)).get(0).extrapolated);// 4e10
	}

	@Test
	public void withoutGpuModelTheEstimateSaysWhy() {
		GpuEstimator.save(GpuEstimator.key(), known(false));
		Estimate e = GpuEstimator.estimate(false, Problem.localPeak(64, 64, 32, 1000, new double[] { 1, 1, 1 })).get(0);
		assertFalse(e.gpuAvailable());
		assertFalse(e.recommendGpu());
		assertNotNull(e.note);
		assertTrue(Double.isNaN(e.speedup()));
		assertTrue(e.cpuSeconds > 0);
	}

	@Test
	public void calibrationOfAnotherMachineIsNotReused() {
		GpuEstimator.save("another device|3|0.0", known(true));
		assertNull(GpuEstimator.load(GpuEstimator.key()));
		assertNotNull(GpuEstimator.load("another device|3|0.0"));
	}

	@Test
	public void problemWorkGrowsWithTheSizeOfTheProblem() {
		double[] sp = { 1, 1, 1 };
		Problem a = Problem.gabor(64, 64, 8, sp, 3, 2, 1.5, true, 1);
		Problem b = Problem.gabor(128, 128, 8, sp, 3, 2, 1.5, true, 1);
		Problem c = Problem.gabor(128, 128, 8, sp, 3, 2, 1.5, false, 1);
		assertTrue(b.cpuWork > a.cpuWork && b.gpuWork > a.gpuWork);
		assertTrue(c.cpuWork > b.cpuWork && c.gpuWork > b.gpuWork);// three planes instead of one
		Problem s1 = Problem.simoncelli(64, 64, 32, 1, true, false);
		Problem s2 = Problem.simoncelli(128, 128, 32, 1, true, false);
		Problem sp2 = Problem.simoncelli(128, 128, 32, 1, true, true);
		assertTrue(s2.cpuWork > s1.cpuWork);
		assertTrue(sp2.cpuWork < s2.cpuWork);// planar: two axes of each slice, no third axis
	}
}
