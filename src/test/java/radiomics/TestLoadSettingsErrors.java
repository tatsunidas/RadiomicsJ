package radiomics;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.UncheckedIOException;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.tatsunidas.radiomics.main.RadiomicsJ;

/**
 * A settings file that cannot be read is reported by an exception.
 * It must not end the JVM of the caller: if it does, this class cannot finish.
 */
public class TestLoadSettingsErrors {

	@AfterEach
	public void resetSettings() {
		RadiomicsJ.resetSettings();
	}

	@Test
	public void missingFileThrowsWithThePath(@TempDir Path dir) {
		String missing = dir.resolve("no-such.properties").toString();
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
				() -> new RadiomicsJ().loadSettings(missing));
		assertTrue(e.getMessage().contains(missing), e.getMessage());
	}

	@Test
	public void unreadableFileThrows(@TempDir Path dir) {
		// a directory exists, but it cannot be read as a properties file
		assertThrows(UncheckedIOException.class, () -> new RadiomicsJ().loadSettings(dir.toString()));
	}

	@Test
	public void readableFileLoads() {
		assertDoesNotThrow(() -> new RadiomicsJ().loadSettings("settings_3D_example.properties"));
	}

	@Test
	public void nullPathKeepsTheSettings() {
		assertDoesNotThrow(() -> new RadiomicsJ().loadSettings((String) null));
	}
}
