package radiomics.gpu;

import static org.jocl.CL.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.stream.IntStream;

import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_kernel;
import org.jocl.cl_mem;

import ij.ImagePlus;
import io.github.tatsunidas.radiomics.features.NGTDMFeatures;
import io.github.tatsunidas.radiomics.main.Utils;

/**
 * Prototype of NGTDMFeatures.fillNGTDM(false). The neighbour sums and counts are computed on the GPU (or in parallel on
 * the CPU for comparison), in integers. The matrix is then finished on the host, in the voxel order of the original loops
 * and with the same double operations, so the result should be equal bit for bit.
 * Inputs are read from a constructed NGTDMFeatures object by reflection (its fields are package private).
 */
public final class NgtdmGpu {

	/** nanoseconds of the last GPU call: prepare, upload, kernel, download, finish on the host */
	public static final long[] lastNs = new long[5];

	private NgtdmGpu() {
	}

	/** Everything the matrix needs, read once from a built NGTDMFeatures. */
	public static final class Input {
		public int w, h, s, nBins, label, delta;
		public float[] val;
		public byte[] valid, center;
		public int[] level;
		public int[] offsets;// x, y, z per neighbour
		/** false when the integer shortcut is not exact: a disc value that is not a whole number, or a sum past int32 */
		public boolean exact = true;
	}

	@SuppressWarnings("unchecked")
	public static Input read(NGTDMFeatures f) throws Exception {
		Input in = new Input();
		ImagePlus disc = (ImagePlus) get(f, "discImg");
		ImagePlus mask = (ImagePlus) get(f, "mask");
		in.w = (Integer) get(f, "w");
		in.h = (Integer) get(f, "h");
		in.s = (Integer) get(f, "s");
		in.nBins = (Integer) get(f, "nBins");
		in.label = (Integer) get(f, "label");
		in.delta = (Integer) get(f, "delta");
		HashMap<Integer, int[]> angles = (HashMap<Integer, int[]>) get(f, "angles");
		ArrayList<Integer> ids = new ArrayList<>(angles.keySet());
		Collections.sort(ids);
		ArrayList<int[]> offs = new ArrayList<>();
		for (int i = 1; i <= in.delta; i++) {
			for (Integer id : ids) {
				if (id == 13) {
					continue;// 0,0,0
				}
				int[] a = angles.get(id);
				offs.add(new int[] { a[2] * i, a[1] * i * -1, a[0] * i });
			}
		}
		in.offsets = new int[3 * offs.size()];
		for (int i = 0; i < offs.size(); i++) {
			System.arraycopy(offs.get(i), 0, in.offsets, 3 * i, 3);
		}
		int n = in.w * in.h * in.s;
		float maxVal = 0;
		in.val = new float[n];
		in.valid = new byte[n];
		in.center = new byte[n];
		in.level = new int[n];
		for (int z = 0; z < in.s; z++) {
			float[][] d = disc.getStack().getProcessor(z + 1).getFloatArray();// [x][y]
			float[][] m = mask.getStack().getProcessor(z + 1).getFloatArray();
			for (int y = 0; y < in.h; y++) {
				for (int x = 0; x < in.w; x++) {
					int idx = x + in.w * (y + in.h * z);
					float v = d[x][y];
					in.val[idx] = Float.isNaN(v) ? 0f : v;
					boolean inRoi = ((int) m[x][y]) == in.label;
					boolean ok = inRoi && !Float.isNaN(v);
					in.valid[idx] = (byte) (ok ? 1 : 0);
					if (ok) {
						int lv = (int) v;
						if (lv >= 1 && lv <= in.nBins) {
							in.center[idx] = 1;
							in.level[idx] = lv;
						}
						if (v != Math.rint(v)) {
							in.exact = false;
						}
						maxVal = Math.max(maxVal, Math.abs(v));
					}
				}
			}
		}
		if ((double) offs.size() * maxVal >= Integer.MAX_VALUE) {// the sums are int32
			in.exact = false;
		}
		return in;
	}

	private static Object get(Object o, String name) throws Exception {
		for (Class<?> c = NGTDMFeatures.class; c != null; c = c.getSuperclass()) {// mask is declared in a superclass
			try {
				Field fl = c.getDeclaredField(name);
				fl.setAccessible(true);
				return fl.get(o);
			} catch (NoSuchFieldException e) {
				// try the parent
			}
		}
		throw new NoSuchFieldException(name);
	}

	/** Matrix [i, Ni, Pi, Si] from the per voxel neighbour sums and counts, as fillNGTDM(false) makes it. */
	public static double[][] finish(Input in, int[] sum, int[] cnt) {
		double[] si = new double[in.nBins + 1];
		int[] ni = new int[in.nBins + 1];
		for (int idx = 0; idx < in.level.length; idx++) {// z, y, x order, as the original loops
			int lv = in.level[idx];
			if (lv == 0) {
				continue;
			}
			if (cnt[idx] != 0) {
				si[lv] += Math.abs(lv - ((double) sum[idx] / cnt[idx]));
			}
			ni[lv]++;
		}
		double nvp = 0;
		double[][] m = new double[in.nBins][];
		for (int g = 1; g <= in.nBins; g++) {
			m[g - 1] = new double[] { g, ni[g], 0.0, si[g] };
			nvp += ni[g];
		}
		for (int g = 1; g <= in.nBins; g++) {
			m[g - 1][2] = m[g - 1][1] / nvp;
		}
		return m;
	}

	/** The same neighbour sums on the CPU, parallel over slices: the CPU baseline that has the GPU's structure. */
	public static double[][] cpuSinglePass(Input in) {
		int n = in.level.length;
		int[] sum = new int[n], cnt = new int[n];
		IntStream.range(0, in.s).parallel().forEach(z -> {
			for (int y = 0; y < in.h; y++) {
				for (int x = 0; x < in.w; x++) {
					int idx = x + in.w * (y + in.h * z);
					if (in.center[idx] == 0) {
						continue;
					}
					int sm = 0, c = 0;
					for (int k = 0; k < in.offsets.length; k += 3) {
						int nx = x + in.offsets[k], ny = y + in.offsets[k + 1], nz = z + in.offsets[k + 2];
						if (nx < 0 || nx >= in.w || ny < 0 || ny >= in.h || nz < 0 || nz >= in.s) {
							continue;
						}
						int j = nx + in.w * (ny + in.h * nz);
						if (in.valid[j] != 0) {
							sm += (int) in.val[j];
							c++;
						}
					}
					sum[idx] = sm;
					cnt[idx] = c;
				}
			}
		});
		return finish(in, sum, cnt);
	}

	public static double[][] gpu(Cl cl, Input in) {
		java.util.Arrays.fill(lastNs, 0);
		long t0 = System.nanoTime();
		int n = in.level.length;
		int[] sum = new int[n], cnt = new int[n];
		long t1 = System.nanoTime();
		lastNs[0] += t1 - t0;
		cl_mem val = cl.in(Pointer.to(in.val), (long) n * Sizeof.cl_float);
		cl_mem valid = cl.in(Pointer.to(in.valid), n);
		cl_mem center = cl.in(Pointer.to(in.center), n);
		cl_mem off = cl.in(Pointer.to(in.offsets), (long) in.offsets.length * Sizeof.cl_int);
		cl_mem sumB = cl.out((long) n * Sizeof.cl_int);
		cl_mem cntB = cl.out((long) n * Sizeof.cl_int);
		long t2 = System.nanoTime();
		lastNs[1] += t2 - t1;
		cl_kernel k = cl.kernel("ngtdm.cl", "ngtdm_gather", "");
		try {
			int a = 0;
			cl.arg(k, a++, val);
			cl.arg(k, a++, valid);
			cl.arg(k, a++, center);
			cl.arg(k, a++, in.w);
			cl.arg(k, a++, in.h);
			cl.arg(k, a++, in.s);
			cl.arg(k, a++, off);
			cl.arg(k, a++, in.offsets.length / 3);
			cl.arg(k, a++, sumB);
			cl.arg(k, a++, cntB);
			cl.run(k, in.w, in.h, in.s);
			long t3 = System.nanoTime();
			lastNs[2] += t3 - t2;
			cl.read(sumB, Pointer.to(sum), (long) n * Sizeof.cl_int);
			cl.read(cntB, Pointer.to(cnt), (long) n * Sizeof.cl_int);
			long t4 = System.nanoTime();
			lastNs[3] += t4 - t3;
			double[][] m = finish(in, sum, cnt);
			lastNs[4] += System.nanoTime() - t4;
			return m;
		} finally {
			clReleaseKernel(k);
			for (cl_mem m : new cl_mem[] { val, valid, center, off, sumB, cntB }) {
				clReleaseMemObject(m);
			}
		}
	}
}
