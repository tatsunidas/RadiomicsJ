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
