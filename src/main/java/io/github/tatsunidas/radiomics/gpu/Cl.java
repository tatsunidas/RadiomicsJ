package io.github.tatsunidas.radiomics.gpu;

import static org.jocl.CL.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.jocl.CLException;
import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_command_queue;
import org.jocl.cl_context;
import org.jocl.cl_device_id;
import org.jocl.cl_kernel;
import org.jocl.cl_mem;
import org.jocl.cl_platform_id;
import org.jocl.cl_program;

/**
 * An OpenCL context on the first GPU device found. The only class that uses JOCL; reach it through {@link Gpu#get()}.
 * Kernel sources are jar resources under /radiomics/gpu/.
 */
public final class Cl implements AutoCloseable {

	public final String deviceName;
	/** true when the device supports double precision. */
	public final boolean fp64;
	private final cl_context context;
	private final cl_command_queue queue;
	private final cl_device_id device;
	private final Map<String, cl_program> programs = new HashMap<String, cl_program>();

	private Cl(cl_context c, cl_command_queue q, cl_device_id d, String name, boolean fp64) {
		this.context = c;
		this.queue = q;
		this.device = d;
		this.deviceName = name;
		this.fp64 = fp64;
	}

	/**
	 * First GPU device of the first platform that has one.
	 * 
	 * @throws IllegalStateException when there is no GPU device(JOCL or libOpenCL missing is a LinkageError)
	 */
	static Cl open() {
		setExceptionsEnabled(true);
		int[] n = new int[1];
		clGetPlatformIDs(0, null, n);
		cl_platform_id[] ps = new cl_platform_id[n[0]];
		clGetPlatformIDs(ps.length, ps, null);
		for (cl_platform_id p : ps) {
			int[] m = new int[1];
			try {
				clGetDeviceIDs(p, CL_DEVICE_TYPE_GPU, 0, null, m);
			} catch (CLException e) {
				continue;// this platform has no GPU
			}
			cl_device_id[] ds = new cl_device_id[m[0]];
			clGetDeviceIDs(p, CL_DEVICE_TYPE_GPU, m[0], ds, null);
			cl_device_id d = ds[0];
			String name = info(d, CL_DEVICE_NAME);
			boolean fp64 = info(d, CL_DEVICE_EXTENSIONS).contains("cl_khr_fp64");
			cl_context ctx = clCreateContext(null, 1, new cl_device_id[] { d }, null, null, null);
			cl_command_queue q = clCreateCommandQueue(ctx, d, 0, null);
			return new Cl(ctx, q, d, name, fp64);
		}
		throw new IllegalStateException("no OpenCL GPU device");
	}

	private static String info(cl_device_id d, int key) {
		long[] s = new long[1];
		clGetDeviceInfo(d, key, 0, null, s);
		byte[] b = new byte[(int) s[0]];
		clGetDeviceInfo(d, key, b.length, Pointer.to(b), null);
		return new String(b, 0, Math.max(0, b.length - 1), StandardCharsets.UTF_8);
	}

	/** The program is built once per resource and option string, so only the first call pays for the build. */
	cl_kernel kernel(String resource, String name, String options) {
		String key = resource + "|" + options;
		cl_program cached = programs.get(key);
		if (cached != null) {
			return clCreateKernel(cached, name, null);
		}
		String src = readResource(resource);
		cl_program prog = clCreateProgramWithSource(context, 1, new String[] { src }, null, null);
		try {
			clBuildProgram(prog, 0, null, options, null, null);
		} catch (CLException e) {
			long[] s = new long[1];
			clGetProgramBuildInfo(prog, device, CL_PROGRAM_BUILD_LOG, 0, null, s);
			byte[] b = new byte[(int) s[0]];
			clGetProgramBuildInfo(prog, device, CL_PROGRAM_BUILD_LOG, b.length, Pointer.to(b), null);
			throw new IllegalStateException("kernel build failed (" + resource + "): " + new String(b, StandardCharsets.UTF_8), e);
		}
		programs.put(key, prog);
		return clCreateKernel(prog, name, null);
	}

	private static String readResource(String resource) {
		try (InputStream in = Cl.class.getResourceAsStream("/radiomics/gpu/" + resource)) {
			if (in == null) {
				throw new IllegalStateException("kernel source not found in the jar: " + resource);
			}
			java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
			byte[] buf = new byte[8192];
			int r;
			while ((r = in.read(buf)) > 0) {
				bos.write(buf, 0, r);
			}
			return new String(bos.toByteArray(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new IllegalStateException("kernel source not readable: " + resource, e);
		}
	}

	/**
	 * Runs a small kernel on the device: c = a + b. Used to check that the GPU path works end to end.
	 */
	public float[] selfTestAdd(float[] a, float[] b) {
		int n = a.length;
		cl_kernel k = kernel("selftest.cl", "vadd", "");
		cl_mem ma = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, (long) n * Sizeof.cl_float, Pointer.to(a), null);
		cl_mem mb = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, (long) n * Sizeof.cl_float, Pointer.to(b), null);
		cl_mem mc = clCreateBuffer(context, CL_MEM_WRITE_ONLY, (long) n * Sizeof.cl_float, null, null);
		try {
			clSetKernelArg(k, 0, Sizeof.cl_mem, Pointer.to(ma));
			clSetKernelArg(k, 1, Sizeof.cl_mem, Pointer.to(mb));
			clSetKernelArg(k, 2, Sizeof.cl_mem, Pointer.to(mc));
			clEnqueueNDRangeKernel(queue, k, 1, null, new long[] { n }, null, 0, null, null);
			clFinish(queue);
			float[] c = new float[n];
			clEnqueueReadBuffer(queue, mc, CL_TRUE, 0, (long) n * Sizeof.cl_float, Pointer.to(c), 0, null, null);
			return c;
		} finally {
			clReleaseMemObject(ma);
			clReleaseMemObject(mb);
			clReleaseMemObject(mc);
			clReleaseKernel(k);
		}
	}

	cl_mem in(Pointer p, long bytes) {
		return clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, bytes, p, null);
	}

	cl_mem out(long bytes) {
		return clCreateBuffer(context, CL_MEM_WRITE_ONLY, bytes, null, null);
	}

	void arg(cl_kernel k, int i, cl_mem m) {
		clSetKernelArg(k, i, Sizeof.cl_mem, Pointer.to(m));
	}

	void arg(cl_kernel k, int i, int v) {
		clSetKernelArg(k, i, Sizeof.cl_int, Pointer.to(new int[] { v }));
	}

	void run(cl_kernel k, long x, long y, long z) {
		clEnqueueNDRangeKernel(queue, k, 3, null, new long[] { x, y, z }, null, 0, null, null);
		clFinish(queue);
	}

	void read(cl_mem m, Pointer dst, long bytes) {
		clEnqueueReadBuffer(queue, m, CL_TRUE, 0, bytes, dst, 0, null, null);
	}

	/**
	 * One orientation of the Gabor filter in one plane, by direct convolution on the device.
	 * See {@link GaborGpu#plane}; the result is stored slice, row, column.
	 */
	public float[][] gaborPlane(float[] src, int[] stride, int slices, int rows, int cols, int rr, int rc,
			int[] rowAt, int[] colAt, double sr, double sc, double sigma, double lambda, double gamma, double theta,
			boolean useFp64) {
		return GaborGpu.plane(this, src, stride, slices, rows, cols, rr, rc, rowAt, colAt, sr, sc, sigma, lambda, gamma,
				theta, useFp64 && fp64);
	}

	@Override
	public void close() {
		for (cl_program p : programs.values()) {
			clReleaseProgram(p);
		}
		programs.clear();
		clReleaseCommandQueue(queue);
		clReleaseContext(context);
	}
}
