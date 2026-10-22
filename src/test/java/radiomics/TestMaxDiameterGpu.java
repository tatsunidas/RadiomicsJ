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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.vecmath.Point3f;

import org.junit.jupiter.api.Test;

import io.github.tatsunidas.radiomics.features.MorphologicalFeatures;
import io.github.tatsunidas.radiomics.gpu.Gpu;
import io.github.tatsunidas.radiomics.main.RadiomicsJ;

/**
 * The largest distance of a point cloud, with the GPU and without it, against the plain double loop over every pair
 * that the feature used before: the same double, bit for bit. Where no GPU is usable both runs are the CPU code,
 * the reason is kept, and the test says so.
 */
public class TestMaxDiameterGpu {

	/** the code that was in getMaximum3DDiameterByMesh */
	private static double everyPair(List<Point3f> points) {
		Double max = 0d;
		for (int n = 0; n < points.size(); n++) {
			for (int m = 0; m < points.size(); m++) {
				if (n == m) {
					continue;
				}
				double d = points.get(n).distance(points.get(m));
				if (max < d) {
					max = d;
				}
			}
		}
		return max;
	}

	private static List<Point3f> cloud(int n, int triangleCopies, boolean sphere, long seed) {
		Random rnd = new Random(seed);
		List<Point3f> v = new ArrayList<>();
		for (int i = 0; i < n; i++) {
			if (sphere) {
				// many points at the same distance from the centre: the maximum is nearly tied
				double a = rnd.nextDouble() * 2 * Math.PI, b = Math.acos(2 * rnd.nextDouble() - 1);
				v.add(new Point3f((float) (30 * Math.sin(b) * Math.cos(a)), (float) (30 * Math.sin(b) * Math.sin(a)),
						(float) (30 * Math.cos(b))));
			} else {
				v.add(new Point3f(rnd.nextFloat() * 90, rnd.nextFloat() * 70, rnd.nextFloat() * 40));
			}
		}
		List<Point3f> listed = new ArrayList<>();
		for (Point3f p : v) {// a vertex is listed once per triangle
			for (int c = 0; c < triangleCopies; c++) {
				listed.add(new Point3f(p));
			}
		}
		return listed;
	}

	@Test
	public void gpuDistanceEqualsTheEveryPairLoopBitForBit() {
		boolean onGpu = Gpu.get() != null;
		System.out.println(onGpu ? "MaxDiameter: run on the GPU"
				: "MaxDiameter: no GPU, CPU code only (" + Gpu.unavailableReason() + ")");
		if (!onGpu) {
			assertNotNull(Gpu.unavailableReason());
		}
		boolean saved = RadiomicsJ.BOOL_USE_GPU;
		try {
			for (boolean sphere : new boolean[] { false, true }) {
				for (int copies : new int[] { 1, 6 }) {
					List<Point3f> pts = cloud(1500, copies, sphere, 11 + copies);
					double ref = everyPair(pts);
					RadiomicsJ.BOOL_USE_GPU = false;
					double cpu = MorphologicalFeatures.maximumDistance(pts);
					RadiomicsJ.BOOL_USE_GPU = true;
					double gpu = MorphologicalFeatures.maximumDistance(pts);
					System.out.println((sphere ? "sphere" : "box") + " x" + copies + ": every pair " + ref + " cpu " + cpu + " gpu " + gpu);
					assertEquals(Double.doubleToLongBits(ref), Double.doubleToLongBits(cpu));
					assertEquals(Double.doubleToLongBits(ref), Double.doubleToLongBits(gpu));
				}
			}
		} finally {
			RadiomicsJ.BOOL_USE_GPU = saved;
		}
	}
}
