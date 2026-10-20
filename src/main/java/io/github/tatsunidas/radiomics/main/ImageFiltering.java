/*
 * Copyright 2022 Tatsuaki Kobayashi

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

package io.github.tatsunidas.radiomics.main;

import java.util.stream.IntStream;

import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import io.github.tatsunidas.ij.plugin.wavelet.Wavelet_Denoise;

/**
 * 
 * @author tatsunidas <t_kobayashi@vis-ionary.com>
 *
 */
public class ImageFiltering {
	
	/**
	 * |LL|HL|
	 *  -------
	 * |LH|HH|
	 * 
	 * @param imp
	 * @param rect_size: size % 2 = 0 !
	 * @return coeffs (LL,HL,LH,HH images)
	 */
	public static ImagePlus getWaveletCoeffs(ImagePlus imp, int rect_size){
		if(rect_size % 2 != 0) {
			System.out.println("Invalid rect size inputted, forcely change to 256.");
			rect_size = 256;
		}
		Calibration cal = imp.getCalibration().copy();
		Wavelet_Denoise wd = new Wavelet_Denoise();
		int s = imp.getStackSize();
		int w = rect_size;
		int h = rect_size;
		ImageStack stack = new ImageStack(rect_size/2, rect_size/2);
		for(int z=0;z<s;z++) {
			
			ImageProcessor tip = imp.getStack().getProcessor(z+1).duplicate().resize(rect_size, rect_size);
			ImagePlus timp = new ImagePlus(""+(z+1), tip);
			wd.setup("", timp);
			wd.execute(timp.getProcessor(), false);
			ImageProcessor ll = new FloatProcessor(w/2, h/2);
			ImageProcessor lh = new FloatProcessor(w/2, h/2);
			ImageProcessor hl = new FloatProcessor(w/2, h/2);
			ImageProcessor hh = new FloatProcessor(w/2, h/2);
			double[][][] res = wd.imageData.getCoefficients();
			for (int wy = 0; wy < h; wy++) {
				for (int wx = 0; wx < w; wx++) {
					if(wx < w/2 && wy < h/2) {
						ll.setf(wx, wy, (float) res[0][wy][wx]);
					}else if(wx >= w/2 && wy < h/2) {
						lh.setf(wx-(w/2), wy, (float) res[0][wy][wx]);
					}else if(wx < w/2 && wy >= h/2) {
						hl.setf(wx, wy-(h/2), (float) res[0][wy][wx]);
					}else {
						hh.setf(wx-(w/2), wy-(h/2), (float) res[0][wy][wx]);
					}
				}
			}
			stack.addSlice(ll);
			stack.addSlice(lh);
			stack.addSlice(hl);
			stack.addSlice(hh);
		}
		ImagePlus wavCoeff = new ImagePlus("wavelet-coeffs",stack);
		cal.disableDensityCalibration();
		wavCoeff.setCalibration(cal);
		return wavCoeff;
	}

	/**
	 * How a convolution reads past the edge of the image, named as in IBSI 2
	 * (reference manual section 2.2).
	 */
	public enum Padding {
		/** zero outside the image */
		CONSTANT,
		/** the edge voxel repeated */
		NEAREST,
		/** reflected about the edge, the edge voxel included: a b c | c b a */
		MIRROR,
		/** the image repeated: a b c | a b c */
		PERIODIC
	}

	/**
	 * Lets the slices and rows of a filter run on several cores. The output is the
	 * same either way, since every voxel is summed in the same order.
	 */
	public static boolean parallel = true;

	/**
	 * Laplacian of Gaussian filter as defined by IBSI 2 (reference manual
	 * arXiv 2006.05470 v9, section 4.3, equation 4.2):
	 *
	 * g[k] = -(1 / s^2) (1 / (sqrt(2 pi) s))^D (D - |k|^2 / s^2) exp(-|k|^2 / (2 s^2))
	 *
	 * with s the scale in voxels (sigma in mm over the voxel spacing) and D the
	 * number of dimensions. A bright blob gives a negative response. The kernel is
	 * neither scale normalised nor corrected to sum to zero, and is cut off at a
	 * half width of floor(truncate * s + 0.5) voxels.
	 *
	 * The response is in units per voxel squared, so the grid must be isotropic:
	 * in all three axes for a volumetric filter, and in plane for a planar one.
	 * Resample the image first otherwise.
	 *
	 * @param imp image, read through its density calibration (for instance HU)
	 * @param sigmaMm scale of the Gaussian in the unit of the calibration (mm)
	 * @param truncate half width of the kernel as a multiple of sigma
	 * @param padding how the image is extended past its edge
	 * @param planar true filters every axial slice on its own (D = 2), false the volume (D = 3)
	 * @return response map in 32 bit float, on the grid of the input
	 */
	public static ImagePlus laplacianOfGaussian(ImagePlus imp, double sigmaMm, double truncate,
			Padding padding, boolean planar) {
		if (imp == null || padding == null) {
			throw new IllegalArgumentException("RadiomicsJ:LoG::image and padding are required.");
		}
		if (!(sigmaMm > 0) || !(truncate > 0)) {
			throw new IllegalArgumentException(
					"RadiomicsJ:LoG::sigma and truncate must be positive, got sigma " + sigmaMm + ", truncate " + truncate);
		}
		Calibration cal = imp.getCalibration();
		double sx = cal.pixelWidth;
		double sy = cal.pixelHeight;
		double sz = cal.pixelDepth;
		if (!sameSpacing(sx, sy) || (!planar && !sameSpacing(sx, sz))) {
			throw new IllegalArgumentException("RadiomicsJ:LoG::the voxel spacing " + sx + " x " + sy + " x " + sz
					+ " is not isotropic" + (planar ? " in plane" : "")
					+ ". Resample the image to isotropic voxels first.");
		}
		double sigma = sigmaMm / sx;
		int radius = (int) Math.floor(truncate * sigma + 0.5);
		double[] g = new double[2 * radius + 1];
		double[] g2 = new double[2 * radius + 1];
		double norm = 1.0 / (Math.sqrt(2.0 * Math.PI) * sigma);
		for (int k = -radius; k <= radius; k++) {
			double e = norm * Math.exp(-(double) k * k / (2.0 * sigma * sigma));
			g[k + radius] = e;
			g2[k + radius] = e * ((double) k * k / (sigma * sigma * sigma * sigma) - 1.0 / (sigma * sigma));
		}

		int w = imp.getWidth();
		int h = imp.getHeight();
		int d = imp.getStackSize();
		float[][] f = calibratedVoxels(imp);

		// separable: LoG = Gxx Gy Gz + Gx Gyy Gz + Gx Gy Gzz
		float[][] gx = convolve(f, w, h, d, g, padding, 0);
		float[][] g2x = convolve(f, w, h, d, g2, padding, 0);
		float[][] gxgy = convolve(gx, w, h, d, g, padding, 1);
		float[][] planarSum = convolve(gx, w, h, d, g2, padding, 1);
		float[][] g2xgy = convolve(g2x, w, h, d, g, padding, 1);
		for (int z = 0; z < d; z++) {
			float[] a = planarSum[z];
			float[] b = g2xgy[z];
			for (int i = 0; i < a.length; i++) {
				a[i] += b[i];
			}
		}
		float[][] out;
		if (planar) {
			out = planarSum;
		} else {
			float[][] zzTerm = convolve(gxgy, w, h, d, g2, padding, 2);
			out = convolve(planarSum, w, h, d, g, padding, 2);
			for (int z = 0; z < d; z++) {
				float[] a = out[z];
				float[] b = zzTerm[z];
				for (int i = 0; i < a.length; i++) {
					a[i] += b[i];
				}
			}
		}

		return response("LoG", out, imp);
	}

	/**
	 * Mean filter as defined by IBSI 2 (reference manual section 4.2): every voxel
	 * becomes the mean over a cube of support M voxels (a square for a planar
	 * filter) centred on it. Voxels the cube reaches outside the image take the
	 * value the padding gives them, which is 0 for constant padding.
	 *
	 * The support is in voxels, so the voxel spacing does not matter.
	 *
	 * @param imp image, read through its density calibration (for instance HU)
	 * @param support M, an odd number of voxels
	 * @param padding how the image is extended past its edge
	 * @param planar true filters every axial slice on its own, false the volume
	 * @return response map in 32 bit float, on the grid of the input
	 */
	public static ImagePlus meanFilter(ImagePlus imp, int support, Padding padding, boolean planar) {
		if (imp == null || padding == null) {
			throw new IllegalArgumentException("RadiomicsJ:meanFilter::image and padding are required.");
		}
		if (support < 1 || support % 2 == 0) {
			throw new IllegalArgumentException(
					"RadiomicsJ:meanFilter::the support must be an odd number of voxels, got " + support);
		}
		int w = imp.getWidth();
		int h = imp.getHeight();
		int d = imp.getStackSize();
		double[] kernel = new double[support];
		java.util.Arrays.fill(kernel, 1.0 / support);
		float[][] out = convolve(calibratedVoxels(imp), w, h, d, kernel, padding, 0);
		out = convolve(out, w, h, d, kernel, padding, 1);
		if (!planar) {
			out = convolve(out, w, h, d, kernel, padding, 2);
		}
		return response("mean", out, imp);
	}

	/** How the responses of the right angle rotations of a filter are combined voxel by voxel. */
	public enum Pooling {
		MAX, AVERAGE, MIN
	}

	private static final double SQRT2 = Math.sqrt(2), SQRT6 = Math.sqrt(6), SQRT10 = Math.sqrt(10), SQRT70 = Math.sqrt(70);

	/** the 1D Laws kernels, normalised as in IBSI 2 section 4.4 */
	private static double[] lawsKernel(String name) {
		switch (name) {
		case "L3": return scaled(1 / SQRT6, 1, 2, 1);
		case "L5": return scaled(1 / SQRT70, 1, 4, 6, 4, 1);
		case "E3": return scaled(1 / SQRT2, -1, 0, 1);
		case "E5": return scaled(1 / SQRT10, -1, -2, 0, 2, 1);
		case "S3": return scaled(1 / SQRT6, -1, 2, -1);
		case "S5": return scaled(1 / SQRT6, -1, 0, 2, 0, -1);
		case "W5": return scaled(1 / SQRT10, -1, 2, 0, -2, 1);
		case "R5": return scaled(1 / SQRT70, 1, -4, 6, -4, 1);
		default:
			throw new IllegalArgumentException("RadiomicsJ:lawsFilter::unknown Laws kernel " + name
					+ ", use L3, L5, E3, E5, S3, S5, W5 or R5.");
		}
	}

	private static double[] scaled(double factor, double... v) {
		double[] k = new double[v.length];
		for (int i = 0; i < v.length; i++) {
			k[i] = factor * v[i];
		}
		return k;
	}

	/**
	 * The right angle rotations of a separable filter g1 x g2 (x g3), IBSI 2
	 * appendix A: for each axis k1, k2, (k3) the number of the 1D kernel it takes,
	 * negative when that kernel is flipped. 4 rotations in 2D, 24 in 3D.
	 */
	static final int[][] ROTATIONS_2D = {
			{1, 2}, {-2, 1}, {-1, -2}, {2, -1} };
	static final int[][] ROTATIONS_3D = {
			{1, 2, 3}, {-3, 2, 1}, {-1, 2, -3}, {3, 2, -1}, {2, 3, 1}, {2, -3, -1},
			{2, -1, 3}, {-1, -2, 3}, {-2, 1, 3}, {-3, -1, 2}, {-3, -2, -1}, {-3, 1, -2},
			{-2, -1, -3}, {1, -2, -3}, {2, 1, -3}, {3, -1, -2}, {3, -2, 1}, {3, 1, 2},
			{-1, 3, 2}, {-2, 3, -1}, {1, 3, -2}, {-1, -3, -2}, {-2, -3, 1}, {1, -3, 2} };

	/**
	 * Laws kernel filter as defined by IBSI 2 (reference manual section 4.4).
	 *
	 * The response map h of "E5L5S5" is the image convolved with E5 along x, L5
	 * along y and S5 along z. With a pooling, the responses of all right angle
	 * rotations of the kernel (appendix A) are combined voxel by voxel, which makes
	 * the filter pseudo rotation invariant. With an energy distance, the absolute
	 * response (after the pooling) is averaged over a cube of 2 delta + 1 voxels,
	 * with the same padding.
	 *
	 * @param imp image, read through its density calibration
	 * @param kernels the 1D kernels, 3 names for a volumetric filter or 2 for a planar one,
	 *        concatenated (E5L5S5) or separated by commas or spaces; L3 L5 E3 E5 S3 S5 W5 R5
	 * @param padding how the image is extended past its edge
	 * @param pooling null for the response of the kernel as given
	 * @param energyDelta Chebyshev distance of the energy map in voxels, 0 or less for none
	 * @param planar true filters every axial slice on its own, false the volume
	 * @return response map in 32 bit float, on the grid of the input
	 */
	public static ImagePlus lawsFilter(ImagePlus imp, String kernels, Padding padding, Pooling pooling, int energyDelta,
			boolean planar) {
		if (imp == null || padding == null || kernels == null) {
			throw new IllegalArgumentException("RadiomicsJ:lawsFilter::image, kernels and padding are required.");
		}
		double[][] g = lawsKernels(kernels, planar);
		int dims = g.length;
		int w = imp.getWidth();
		int h = imp.getHeight();
		int d = imp.getStackSize();
		float[][] src = calibratedVoxels(imp);
		int[][] rotations = pooling == null ? new int[][] { planar ? ROTATIONS_2D[0] : ROTATIONS_3D[0] }
				: planar ? ROTATIONS_2D : ROTATIONS_3D;
		float[][] pooled = null;
		for (int[] rotation : rotations) {
			float[][] r = src;
			for (int axis = 0; axis < dims; axis++) {
				int pick = Math.abs(rotation[axis]) - 1;
				// convolve() correlates, so a kernel is reversed to convolve and a flipped one is not
				r = convolve(r, w, h, d, rotation[axis] < 0 ? g[pick] : reversed(g[pick]), padding, axis);
			}
			if (pooled == null) {
				pooled = r;
				continue;
			}
			for (int z = 0; z < d; z++) {
				for (int i = 0; i < r[z].length; i++) {
					float a = pooled[z][i], b = r[z][i];
					pooled[z][i] = pooling == Pooling.MAX ? Math.max(a, b) : pooling == Pooling.MIN ? Math.min(a, b) : a + b;
				}
			}
		}
		if (pooling == Pooling.AVERAGE) {
			for (float[] s : pooled) {
				for (int i = 0; i < s.length; i++) {
					s[i] /= rotations.length;
				}
			}
		}
		ImagePlus res = response("Laws", pooled, imp);
		if (energyDelta > 0) {
			for (int z = 1; z <= d; z++) {
				float[] s = (float[]) res.getStack().getProcessor(z).getPixels();
				for (int i = 0; i < s.length; i++) {
					s[i] = Math.abs(s[i]);
				}
			}
			res = meanFilter(res, 2 * energyDelta + 1, padding, planar);
		}
		return res;
	}

	/** the 1D kernels named by a Laws kernel set, which must hold 3 names (2 for a planar filter) */
	public static double[][] lawsKernels(String kernels, boolean planar) {
		String[] names = kernels.trim().split("[\\s,]+|(?<=\\d)(?=[A-Z])");
		int dims = planar ? 2 : 3;
		if (names.length != dims) {
			throw new IllegalArgumentException("RadiomicsJ:lawsFilter::" + (planar ? "a planar" : "a volumetric")
					+ " filter takes " + dims + " kernels, got '" + kernels + "'.");
		}
		double[][] g = new double[dims][];
		for (int i = 0; i < dims; i++) {
			g[i] = lawsKernel(names[i]);
		}
		return g;
	}

	private static double[] reversed(double[] k) {
		double[] r = new double[k.length];
		for (int i = 0; i < k.length; i++) {
			r[i] = k[k.length - 1 - i];
		}
		return r;
	}

	/** voxel values through the density calibration of the image, one array per slice */
	private static float[][] calibratedVoxels(ImagePlus imp) {
		Calibration cal = imp.getCalibration();
		int w = imp.getWidth();
		int h = imp.getHeight();
		int d = imp.getStackSize();
		float[][] f = new float[d][];
		ImageStack in = imp.getStack();
		for (int z = 0; z < d; z++) {
			ImageProcessor ip = in.getProcessor(z + 1);
			float[] s = new float[w * h];
			for (int i = 0; i < s.length; i++) {
				s[i] = (float) cal.getCValue(ip.getf(i));
			}
			f[z] = s;
		}
		return f;
	}

	/** a response map on the grid of the source, without a density calibration */
	private static ImagePlus response(String title, float[][] voxels, ImagePlus source) {
		int w = source.getWidth();
		int h = source.getHeight();
		ImageStack stack = new ImageStack(w, h);
		for (float[] v : voxels) {
			stack.addSlice(new FloatProcessor(w, h, v));
		}
		ImagePlus res = new ImagePlus(title, stack);
		Calibration resCal = source.getCalibration().copy();
		resCal.disableDensityCalibration();
		res.setCalibration(resCal);
		return res;
	}

	/**
	 * Float headers carry about seven significant digits, so spacings that agree
	 * to one part in a million are the same spacing.
	 */
	private static boolean sameSpacing(double a, double b) {
		return Math.abs(a - b) <= 1e-6 * Math.max(Math.abs(a), Math.abs(b));
	}

	/**
	 * One dimensional convolution along x (axis 0), y (1) or z (2). The kernel is
	 * symmetric, so correlation and convolution agree.
	 */
	private static float[][] convolve(float[][] src, int w, int h, int d, double[] kernel, Padding padding, int axis) {
		int radius = kernel.length / 2;
		float[][] dst = new float[d][w * h];
		int n = axis == 0 ? w : axis == 1 ? h : d;
		int[] index = new int[n + 2 * radius];
		for (int i = 0; i < index.length; i++) {
			index[i] = padIndex(i - radius, n, padding);
		}
		// one task per slice for x and y, per row for z; every task writes its own voxels
		int tasks = axis == 2 ? h : d;
		IntStream range = IntStream.range(0, tasks);
		if (parallel) {
			range = range.parallel();
		}
		range.forEach(t -> {
			if (axis == 2) {
				int y = t;
				for (int x = 0; x < w; x++) {
					int p = y * w + x;
					for (int z = 0; z < d; z++) {
						double sum = 0;
						for (int k = 0; k < kernel.length; k++) {
							int zz = index[z + k];
							if (zz >= 0) {
								sum += kernel[k] * src[zz][p];
							}
						}
						dst[z][p] = (float) sum;
					}
				}
			} else {
				int z = t;
				float[] s = src[z];
				float[] o = dst[z];
				for (int y = 0; y < h; y++) {
					for (int x = 0; x < w; x++) {
						double sum = 0;
						for (int k = 0; k < kernel.length; k++) {
							int q = axis == 0 ? index[x + k] : index[y + k];
							if (q >= 0) {
								sum += kernel[k] * (axis == 0 ? s[y * w + q] : s[q * w + x]);
							}
						}
						o[y * w + x] = (float) sum;
					}
				}
			}
		});
		return dst;
	}

	/**
	 * Where index i of an axis of length n reads from, or -1 for a zero.
	 */
	private static int padIndex(int i, int n, Padding padding) {
		if (i >= 0 && i < n) {
			return i;
		}
		switch (padding) {
		case CONSTANT:
			return -1;
		case NEAREST:
			return i < 0 ? 0 : n - 1;
		case MIRROR: {
			int m = Math.floorMod(i, 2 * n);
			return m < n ? m : 2 * n - 1 - m;
		}
		case PERIODIC:
			return Math.floorMod(i, n);
		default:
			throw new IllegalStateException("RadiomicsJ:LoG::unknown padding " + padding);
		}
	}

}
