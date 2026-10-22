package io.github.tatsunidas.radiomics.gpu;

import static org.jocl.CL.*;

import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_kernel;
import org.jocl.cl_mem;

/**
 * The Fourier transform, band-pass multiplication and inverse transform of the Simoncelli filter on the device, in
 * single precision. A line is transformed by one work group in local memory: radix 2 for a power of two, Bluestein's
 * chirp-z transform otherwise, the same scheme as the CPU code. The twiddle factors and the chirp spectrum are made
 * here in double precision and rounded once.
 */
final class SimoncelliGpu {

	private static final int MAX_LINE = 2048;// padded line length m, so m / 2 work items and m float2 in local memory

	private SimoncelliGpu() {
	}

	private static final class Axis {
		int n, m, logm;
		cl_mem tw, chirp, bf;
	}

	/**
	 * @param vol real input, voxel (x, y, z) at (z * e[1] + y) * e[0] + x
	 * @param nu2 squared frequency of every index along each axis
	 * @param vb upper edge of the band, the lower one is vb / 4
	 * @return the real part of the filtered volume, null when a line is too long for the device
	 */
	static float[] filter(Cl cl, float[] vol, int[] e, double vb, double[][] nu2) {
		Axis[] axes = new Axis[3];
		for (int a = 0; a < 3; a++) {
			if (e[a] < 2) {
				continue;
			}
			int m = (e[a] & (e[a] - 1)) == 0 ? e[a] : Integer.highestOneBit(2 * e[a] - 1) << 1;
			if (m > MAX_LINE || m / 2 > cl.maxWorkGroup || (long) m * 8 > cl.localMem) {
				return null;
			}
		}
		// the device is shared by the worker threads of a planar run: one volume at a time
		synchronized (cl) {
			int total = e[0] * e[1] * e[2];
			float[] host = new float[2 * total];
			for (int i = 0; i < total; i++) {
				host[2 * i] = vol[i];
			}
			cl_mem data = cl.rw(Pointer.to(host), 2L * total * Sizeof.cl_float);
			cl_mem[] nb = new cl_mem[3];
			cl_kernel fft = cl.kernel("fft.cl", "fftLines", "");
			cl_kernel band = cl.kernel("fft.cl", "bandpass", "");
			try {
				for (int a = 0; a < 3; a++) {
					if (e[a] >= 2) {
						axes[a] = axis(cl, e[a]);
					}
					float[] f = new float[e[a]];
					for (int i = 0; i < f.length; i++) {
						f[i] = (float) nu2[a][i];
					}
					nb[a] = cl.in(Pointer.to(f), (long) f.length * Sizeof.cl_float);
				}
				for (int a = 0; a < 3; a++) {
					pass(cl, fft, data, e, a, axes[a], false);
				}
				int i = 0;
				cl.arg(band, i++, data);
				for (int a = 0; a < 3; a++) {
					cl.arg(band, i++, nb[a]);
				}
				cl.arg(band, i++, e[0]);
				cl.arg(band, i++, e[1]);
				cl.arg(band, i++, (float) vb);
				cl.run(band, e[0], e[1], e[2]);
				for (int a = 0; a < 3; a++) {
					pass(cl, fft, data, e, a, axes[a], true);
				}
				cl.read(data, Pointer.to(host), 2L * total * Sizeof.cl_float);
				float[] out = new float[total];
				for (int j = 0; j < total; j++) {
					out[j] = host[2 * j];
				}
				return out;
			} finally {
				clReleaseMemObject(data);
				for (cl_mem m : nb) {
					if (m != null) {
						clReleaseMemObject(m);
					}
				}
				for (Axis a : axes) {
					if (a != null) {
						clReleaseMemObject(a.tw);
						clReleaseMemObject(a.chirp);
						clReleaseMemObject(a.bf);
					}
				}
				clReleaseKernel(fft);
				clReleaseKernel(band);
			}
		}
	}

	private static void pass(Cl cl, cl_kernel k, cl_mem data, int[] e, int axis, Axis a, boolean inverse) {
		if (a == null) {
			return;
		}
		int len = e[axis];
		int stride = axis == 0 ? 1 : axis == 1 ? e[0] : e[0] * e[1];
		long lines = (long) e[0] * e[1] * e[2] / len;
		int t = Math.max(1, a.m / 2);
		int i = 0;
		cl.arg(k, i++, data);
		cl.arg(k, i++, a.tw);
		cl.arg(k, i++, a.chirp);
		cl.arg(k, i++, a.bf);
		cl.argLocal(k, i++, (long) a.m * 2 * Sizeof.cl_float);
		cl.arg(k, i++, len);
		cl.arg(k, i++, a.m);
		cl.arg(k, i++, a.logm);
		cl.arg(k, i++, stride);
		cl.arg(k, i++, inverse ? 1 : 0);
		cl.run1(k, lines * t, t);
	}

	private static Axis axis(Cl cl, int n) {
		Axis a = new Axis();
		a.n = n;
		a.m = (n & (n - 1)) == 0 ? n : Integer.highestOneBit(2 * n - 1) << 1;
		a.logm = Integer.numberOfTrailingZeros(a.m);
		float[] tw = new float[a.m];
		for (int k = 0; k < a.m / 2; k++) {
			tw[2 * k] = (float) Math.cos(2 * Math.PI * k / a.m);
			tw[2 * k + 1] = (float) -Math.sin(2 * Math.PI * k / a.m);
		}
		float[] chirp = new float[2 * n];
		float[] bf = new float[2 * a.m];
		if (a.m != n) {
			double[] br = new double[a.m], bi = new double[a.m];
			for (int k = 0; k < n; k++) {
				double ang = Math.PI * ((long) k * k % (2L * n)) / n;
				double wr = Math.cos(ang), wi = -Math.sin(ang);
				chirp[2 * k] = (float) wr;
				chirp[2 * k + 1] = (float) wi;
				br[k] = wr;
				bi[k] = -wi;
				if (k > 0) {
					br[a.m - k] = wr;
					bi[a.m - k] = -wi;
				}
			}
			forward(br, bi);
			for (int k = 0; k < a.m; k++) {
				bf[2 * k] = (float) br[k];
				bf[2 * k + 1] = (float) bi[k];
			}
		}
		a.tw = cl.in(Pointer.to(tw), (long) tw.length * Sizeof.cl_float);
		a.chirp = cl.in(Pointer.to(chirp), (long) chirp.length * Sizeof.cl_float);
		a.bf = cl.in(Pointer.to(bf), (long) bf.length * Sizeof.cl_float);
		return a;
	}

	/** in-place radix 2 forward transform in double precision, for the chirp spectrum only */
	private static void forward(double[] re, double[] im) {
		int n = re.length;
		for (int i = 1, j = 0; i < n; i++) {
			int bit = n >> 1;
			for (; (j & bit) != 0; bit >>= 1) {
				j ^= bit;
			}
			j ^= bit;
			if (i < j) {
				double t = re[i];
				re[i] = re[j];
				re[j] = t;
				t = im[i];
				im[i] = im[j];
				im[j] = t;
			}
		}
		for (int len = 2; len <= n; len <<= 1) {
			double ang = -2 * Math.PI / len;
			for (int i = 0; i < n; i += len) {
				for (int k = 0; k < len / 2; k++) {
					double wr = Math.cos(ang * k), wi = Math.sin(ang * k);
					int x = i + k, y = i + k + len / 2;
					double tr = re[y] * wr - im[y] * wi, ti = re[y] * wi + im[y] * wr;
					re[y] = re[x] - tr;
					im[y] = im[x] - ti;
					re[x] += tr;
					im[x] += ti;
				}
			}
		}
	}
}
