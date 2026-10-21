package radiomics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The command line must tell a failure from a success by its exit status,
 * because scripts and CI read nothing else.
 * Each case runs RadiomicsJ.main in its own JVM.
 */
public class TestMainExitStatus {

	private static int run(String... args) throws IOException, InterruptedException {
		return run(ProcessBuilder.Redirect.INHERIT, args);
	}

	private static int run(ProcessBuilder.Redirect out, String... args) throws IOException, InterruptedException {
		List<String> cmd = new ArrayList<String>();
		cmd.add(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
		cmd.add("-Djava.awt.headless=true");
		cmd.add("-cp");
		cmd.add(System.getProperty("java.class.path"));
		cmd.add("io.github.tatsunidas.radiomics.main.RadiomicsJ");
		cmd.addAll(Arrays.asList(args));
		Process p = new ProcessBuilder(cmd).redirectErrorStream(true)
				.redirectOutput(out).start();
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

	/**
	 * An image with a mask that has no voxel of the target label gives no result table.
	 * The command line reports it as a failure, without a dialog and without an exception.
	 */
	@Test
	public void emptyMaskExitsWithOneAndReportsIt(@TempDir Path dir) throws Exception {
		Path images = Files.createDirectory(dir.resolve("images"));
		Path masks = Files.createDirectory(dir.resolve("masks"));
		BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_BYTE_GRAY);
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				img.getRaster().setSample(x, y, 0, (x * 16 + y * 7) % 256);
			}
		}
		ImageIO.write(img, "tiff", images.resolve("img.tif").toFile());
		ImageIO.write(new BufferedImage(16, 16, BufferedImage.TYPE_BYTE_GRAY), "tiff", masks.resolve("mask.tif").toFile());
		Path log = dir.resolve("out.log");
		int status = run(ProcessBuilder.Redirect.to(log.toFile()), "-i", images.toString(), "-m", masks.toString());
		String out = new String(Files.readAllBytes(log), java.nio.charset.StandardCharsets.UTF_8);
		assertEquals(1, status, out);
		assertTrue(out.contains("can not perform feature extraction"), out);
		assertFalse(out.contains("HeadlessException"), out);
	}
}
