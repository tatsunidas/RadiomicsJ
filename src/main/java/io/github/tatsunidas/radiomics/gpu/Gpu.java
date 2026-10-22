package io.github.tatsunidas.radiomics.gpu;

import java.util.logging.Level;
import java.util.logging.Logger;

import io.github.tatsunidas.radiomics.main.RadiomicsJ;

/**
 * Entry point of the optional GPU(OpenCL) path.
 * This class does not touch JOCL itself, so it loads without JOCL on the classpath.
 * Any reason that makes the GPU unusable(setting off, no JOCL, no libOpenCL, no device, a kernel that does not build)
 * gives {@code null}, and the caller runs its CPU code.
 */
public final class Gpu {

	private static final Logger LOG = Logger.getLogger(Gpu.class.getName());

	private static boolean tried = false;
	private static Cl cl = null;
	private static String reason = null;

	private Gpu() {
	}

	/**
	 * @return the OpenCL context, or null when the GPU must not or cannot be used
	 */
	public static synchronized Cl get() {
		if (!RadiomicsJ.BOOL_USE_GPU) {
			return null;
		}
		if (!tried) {
			tried = true;
			try {
				cl = Cl.open();
			} catch (Throwable t) {// a missing JOCL or libOpenCL is a LinkageError, not an Exception
				reason = describe(t);
				LOG.log(Level.INFO, "GPU is not used, the CPU code runs instead: " + reason);
			}
		}
		return cl;
	}

	/**
	 * Why the GPU is not available, or null when it is(or has not been tried yet).
	 */
	public static synchronized String unavailableReason() {
		return reason;
	}

	/**
	 * Called when a GPU run fails after the device was opened: the GPU is switched off for the rest of the process.
	 */
	public static synchronized void fail(Throwable t) {
		reason = describe(t);
		LOG.log(Level.WARNING, "GPU run failed, the CPU code runs instead: " + reason);
		if (cl != null) {
			try {
				cl.close();
			} catch (Throwable ignore) {
				// the device is being given up anyway
			}
			cl = null;
		}
	}

	private static String describe(Throwable t) {
		if (t instanceof NoClassDefFoundError || t instanceof ClassNotFoundException) {
			return "JOCL is not on the classpath";
		}
		if (t instanceof UnsatisfiedLinkError) {
			return "OpenCL library is not usable (" + t.getMessage() + ")";
		}
		return t.getMessage() != null ? t.getMessage() : t.toString();
	}
}
