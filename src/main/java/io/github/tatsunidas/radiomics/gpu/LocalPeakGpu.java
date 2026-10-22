package io.github.tatsunidas.radiomics.gpu;

import static org.jocl.CL.*;

import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_kernel;
import org.jocl.cl_mem;

/**
 * The mean in the sphere around every voxel of a region, on the GPU in single precision.
 * Reached only through {@link Cl#localPeakMeans}, so only when a device is open.
 */
final class LocalPeakGpu {

	private LocalPeakGpu() {
	}

	/**
	 * @param img voxel (x, y, z) at (z * h + y) * w + x
	 * @param roi 1 for a voxel of the region
	 * @return the mean per voxel, -infinity outside the region
	 */
	static float[] means(Cl cl, float[] img, byte[] roi, int[] dx, int[] dy, int[] dz, int w, int h, int s) {
		float[] out = new float[img.length];
		cl_kernel k = cl.kernel("localpeak.cl", "localpeak", "");
		cl_mem imgB = cl.in(Pointer.to(img), (long) img.length * Sizeof.cl_float);
		cl_mem roiB = cl.in(Pointer.to(roi), roi.length);
		cl_mem dxB = cl.in(Pointer.to(dx), (long) dx.length * Sizeof.cl_int);
		cl_mem dyB = cl.in(Pointer.to(dy), (long) dy.length * Sizeof.cl_int);
		cl_mem dzB = cl.in(Pointer.to(dz), (long) dz.length * Sizeof.cl_int);
		cl_mem outB = cl.out((long) out.length * Sizeof.cl_float);
		try {
			int a = 0;
			cl.arg(k, a++, imgB);
			cl.arg(k, a++, roiB);
			cl.arg(k, a++, dxB);
			cl.arg(k, a++, dyB);
			cl.arg(k, a++, dzB);
			cl.arg(k, a++, dx.length);
			cl.arg(k, a++, w);
			cl.arg(k, a++, h);
			cl.arg(k, a++, s);
			cl.arg(k, a++, outB);
			cl.run(k, w, h, s);
			cl.read(outB, Pointer.to(out), (long) out.length * Sizeof.cl_float);
		} finally {
			for (cl_mem m : new cl_mem[] { imgB, roiB, dxB, dyB, dzB, outB }) {
				clReleaseMemObject(m);
			}
			clReleaseKernel(k);
		}
		return out;
	}
}
