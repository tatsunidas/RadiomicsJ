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

import java.util.Random;

import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.process.FloatProcessor;
import io.github.tatsunidas.radiomics.main.ImageFiltering;
import io.github.tatsunidas.radiomics.main.ImageFiltering.Padding;

/**
 * Time of the Laplacian of Gaussian filter on a CT sized volume.
 * Run by hand: java -cp ... radiomics.LoGBenchmark [width height depth spacing]
 *
 * @author tatsunidas <t_kobayashi@vis-ionary.com>
 */
public class LoGBenchmark {

	public static void main(String[] args) {
		int w = args.length > 0 ? Integer.parseInt(args[0]) : 512;
		int h = args.length > 1 ? Integer.parseInt(args[1]) : 512;
		int d = args.length > 2 ? Integer.parseInt(args[2]) : 300;
		double s = args.length > 3 ? Double.parseDouble(args[3]) : 0.7;
		Random r = new Random(1L);
		ImageStack stack = new ImageStack(w, h);
		for (int z = 0; z < d; z++) {
			float[] p = new float[w * h];
			for (int i = 0; i < p.length; i++) {
				p[i] = (float) (r.nextGaussian() * 100.0);
			}
			stack.addSlice(new FloatProcessor(w, h, p));
		}
		ImagePlus imp = new ImagePlus("bench", stack);
		Calibration cal = imp.getCalibration();
		cal.pixelWidth = s;
		cal.pixelHeight = s;
		cal.pixelDepth = s;
		System.out.println(w + " x " + h + " x " + d + ", " + s + " mm, "
				+ Runtime.getRuntime().availableProcessors() + " cores");
		for (double sigma : new double[] { 1.0, 3.0, 6.0 }) {
			for (boolean planar : new boolean[] { false, true }) {
				long t0 = System.nanoTime();
				ImageFiltering.laplacianOfGaussian(imp, sigma, 4.0, Padding.MIRROR, planar);
				System.out.printf("sigma %.1f mm %s: %.1f s%n", sigma, planar ? "2D" : "3D",
						(System.nanoTime() - t0) / 1e9);
			}
		}
	}
}
