package radiomics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import io.github.tatsunidas.radiomics.gpu.Cl;
import io.github.tatsunidas.radiomics.gpu.Gpu;
import io.github.tatsunidas.radiomics.main.RadiomicsJ;

/**
 * The GPU path is optional: every reason for it to be unusable must give the CPU code, never an exception.
 * Whether a device is present depends on the machine, so the device case reports which branch it took.
 */
public class TestGpuFoundation {

	@Test
	public void settingOffGivesNoContext() {
		boolean old = RadiomicsJ.BOOL_USE_GPU;
		try {
			RadiomicsJ.BOOL_USE_GPU = false;
			assertNull(Gpu.get());
		} finally {
			RadiomicsJ.BOOL_USE_GPU = old;
		}
	}

	/** Prints "GPU=none" or "GPU=<device>" and exits 0; used by the test below in a JVM without JOCL. */
	public static void main(String[] args) {
		Cl cl = Gpu.get();
		System.out.println("GPU=" + (cl == null ? "none (" + Gpu.unavailableReason() + ")" : cl.deviceName));
	}

	@Test
	public void withoutJoclTheCpuPathIsUsed() throws Exception {
		List<String> cp = new ArrayList<String>();
		for (String e : System.getProperty("java.class.path").split(File.pathSeparator)) {
			if (!new File(e).getName().toLowerCase().startsWith("jocl")) {
				cp.add(e);
			}
		}
		List<String> cmd = new ArrayList<String>();
		cmd.add(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
		cmd.add("-Djava.awt.headless=true");
		cmd.add("-cp");
		cmd.add(String.join(File.pathSeparator, cp));
		cmd.add(TestGpuFoundation.class.getName());
		Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
		String out = new String(readAll(p.getInputStream()));
		assertTrue(p.waitFor(120, TimeUnit.SECONDS));
		assertEquals(0, p.exitValue(), out);
		assertTrue(out.contains("GPU=none (JOCL is not on the classpath)"), out);
	}

	@Test
	public void deviceRunsAKernelOrFallsBack() {
		Cl cl = Gpu.get();
		if (cl == null) {
			System.out.println("GPU not used on this machine: " + Gpu.unavailableReason());
			assertNotNull(Gpu.unavailableReason());
			return;
		}
		System.out.println("GPU device: " + cl.deviceName);
		float[] a = new float[1000];
		float[] b = new float[1000];
		for (int i = 0; i < a.length; i++) {
			a[i] = i;
			b[i] = 2 * i + 0.5f;
		}
		float[] c = cl.selfTestAdd(a, b);
		for (int i = 0; i < a.length; i++) {
			assertEquals(a[i] + b[i], c[i], 0f);
		}
	}

	private static byte[] readAll(java.io.InputStream in) throws java.io.IOException {
		java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
		byte[] buf = new byte[4096];
		int r;
		while ((r = in.read(buf)) > 0) {
			bos.write(buf, 0, r);
		}
		return bos.toByteArray();
	}
}
