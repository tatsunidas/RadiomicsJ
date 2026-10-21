package radiomics.gpu;

import static org.jocl.CL.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_command_queue;
import org.jocl.cl_context;
import org.jocl.cl_device_id;
import org.jocl.cl_kernel;
import org.jocl.cl_mem;
import org.jocl.cl_platform_id;
import org.jocl.cl_program;

/** A thin OpenCL context for the prototypes. Any failure to reach a device is a GpuUnavailable, so callers can fall back. */
public final class Cl implements AutoCloseable {

	public static final class GpuUnavailable extends RuntimeException {
		private static final long serialVersionUID = 1L;

		public GpuUnavailable(String m, Throwable cause) {
			super(m, cause);
		}
	}

	public final String deviceName;
	public final boolean fp64;
	final cl_context context;
	final cl_command_queue queue;
	final cl_device_id device;
	private final String kernelDir;

	private Cl(cl_context c, cl_command_queue q, cl_device_id d, String name, boolean fp64, String kernelDir) {
		this.context = c;
		this.queue = q;
		this.device = d;
		this.deviceName = name;
		this.fp64 = fp64;
		this.kernelDir = kernelDir;
	}

	/** First GPU device of the first platform that has one. */
	public static Cl open(String kernelDir) {
		try {
			setExceptionsEnabled(true);
			int[] n = new int[1];
			clGetPlatformIDs(0, null, n);
			cl_platform_id[] ps = new cl_platform_id[n[0]];
			clGetPlatformIDs(ps.length, ps, null);
			for (cl_platform_id p : ps) {
				int[] m = new int[1];
				try {
					clGetDeviceIDs(p, CL_DEVICE_TYPE_GPU, 0, null, m);
				} catch (org.jocl.CLException e) {
					continue;// this platform has no GPU
				}
				cl_device_id[] ds = new cl_device_id[m[0]];
				clGetDeviceIDs(p, CL_DEVICE_TYPE_GPU, m[0], ds, null);
				cl_device_id d = ds[0];
				String name = info(d, CL_DEVICE_NAME);
				boolean fp64 = info(d, CL_DEVICE_EXTENSIONS).contains("cl_khr_fp64");
				cl_context ctx = clCreateContext(null, 1, new cl_device_id[] { d }, null, null, null);
				cl_command_queue q = clCreateCommandQueue(ctx, d, 0, null);
				return new Cl(ctx, q, d, name, fp64, kernelDir);
			}
			throw new GpuUnavailable("no OpenCL GPU device", null);
		} catch (GpuUnavailable e) {
			throw e;
		} catch (Throwable t) {// UnsatisfiedLinkError (no libOpenCL.so) is an Error, not an Exception
			throw new GpuUnavailable("OpenCL is not usable: " + t, t);
		}
	}

	private static String info(cl_device_id d, int key) {
		long[] s = new long[1];
		clGetDeviceInfo(d, key, 0, null, s);
		byte[] b = new byte[(int) s[0]];
		clGetDeviceInfo(d, key, b.length, Pointer.to(b), null);
		return new String(b, 0, Math.max(0, b.length - 1), StandardCharsets.UTF_8);
	}

	private final java.util.Map<String, cl_program> programs = new java.util.HashMap<>();

	/** The program is built once per source and option string, so only the first call pays for the build. */
	cl_kernel kernel(String file, String name, String options) {
		cl_program cached = programs.get(file + "|" + options);
		if (cached != null) {
			return clCreateKernel(cached, name, null);
		}
		try {
			String src = new String(Files.readAllBytes(Paths.get(kernelDir, file)), StandardCharsets.UTF_8);
			cl_program prog = clCreateProgramWithSource(context, 1, new String[] { src }, null, null);
			try {
				clBuildProgram(prog, 0, null, options, null, null);
			} catch (org.jocl.CLException e) {
				long[] s = new long[1];
				clGetProgramBuildInfo(prog, device, CL_PROGRAM_BUILD_LOG, 0, null, s);
				byte[] b = new byte[(int) s[0]];
				clGetProgramBuildInfo(prog, device, CL_PROGRAM_BUILD_LOG, b.length, Pointer.to(b), null);
				throw new GpuUnavailable("kernel build failed: " + new String(b), e);
			}
			programs.put(file + "|" + options, prog);
			return clCreateKernel(prog, name, null);
		} catch (java.io.IOException e) {
			throw new GpuUnavailable("kernel source not readable: " + file, e);
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

	@Override
	public void close() {
		clReleaseCommandQueue(queue);
		clReleaseContext(context);
	}
}
