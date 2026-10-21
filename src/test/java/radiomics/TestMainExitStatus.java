package radiomics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The command line must tell a failure from a success by its exit status,
 * because scripts and CI read nothing else.
 * Each case runs RadiomicsJ.main in its own JVM.
 */
public class TestMainExitStatus {

	private static int run(String... args) throws IOException, InterruptedException {
		List<String> cmd = new ArrayList<String>();
		cmd.add(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
		cmd.add("-Djava.awt.headless=true");
		cmd.add("-cp");
		cmd.add(System.getProperty("java.class.path"));
		cmd.add("io.github.tatsunidas.radiomics.main.RadiomicsJ");
		cmd.addAll(Arrays.asList(args));
		Process p = new ProcessBuilder(cmd).redirectErrorStream(true)
				.redirectOutput(ProcessBuilder.Redirect.INHERIT).start();
		if (!p.waitFor(240, TimeUnit.SECONDS)) {
			p.destroyForcibly();
			throw new IllegalStateException("RadiomicsJ did not finish: " + Arrays.toString(args));
		}
		return p.exitValue();
	}

	@Test
	public void helpExitsWithZero() throws Exception {
		assertEquals(0, run("-h"));
	}

	@Test
	public void digitalPhantomTestModeExitsWithZero() throws Exception {
		assertEquals(0, run("-t", "-tdt", "0"));
	}

	@Test
	public void unknownOptionExitsWithOne() throws Exception {
		assertEquals(1, run("--no-such-option"));
	}

	@Test
	public void missingMaskFolderExitsWithOne() throws Exception {
		assertEquals(1, run("-i", "some-image-folder"));
	}

	@Test
	public void unequalNumberOfImagesAndMasksExitsWithOne(@TempDir Path dir) throws Exception {
		Path images = Files.createDirectory(dir.resolve("images"));
		Path masks = Files.createDirectory(dir.resolve("masks"));
		Files.createFile(images.resolve("a.dcm"));
		Files.createFile(images.resolve("b.dcm"));
		Files.createFile(masks.resolve("a.dcm"));
		assertEquals(1, run("-i", images.toString(), "-m", masks.toString()));
	}

	@Test
	public void missingSettingsFileExitsWithOne() throws Exception {
		assertEquals(1, run("-t", "-tdt", "0", "-s", "no-such-settings.properties"));
	}
}
