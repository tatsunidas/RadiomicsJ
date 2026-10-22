package io.github.tatsunidas.radiomics.gpu;

import static org.jocl.CL.*;

import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_kernel;
import org.jocl.cl_mem;

/**
 * The farthest squared distance of every point of a cloud, on the GPU in single precision.
 * Reached only through {@link Cl#maxSquaredDistances}, so only when a device is open.
 */
final class MaxDistGpu {

	private MaxDistGpu() {
	}

	static float[] far(Cl cl, float[] xyz) {
		int n = xyz.length / 3;
		float[] out = new float[n];
		cl_kernel k = cl.kernel("maxdist.cl", "maxdist", "");
		cl_mem inB = cl.in(Pointer.to(xyz), (long) xyz.length * Sizeof.cl_float);
		cl_mem outB = cl.out((long) n * Sizeof.cl_float);
		try {
			cl.arg(k, 0, inB);
			cl.arg(k, 1, n);
			cl.arg(k, 2, outB);
			cl.run(k, n, 1, 1);
			cl.read(outB, Pointer.to(out), (long) n * Sizeof.cl_float);
		} finally {
			clReleaseMemObject(inB);
			clReleaseMemObject(outB);
			clReleaseKernel(k);
		}
		return out;
	}
}
