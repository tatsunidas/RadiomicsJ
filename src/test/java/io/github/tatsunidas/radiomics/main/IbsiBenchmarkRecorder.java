package io.github.tatsunidas.radiomics.main;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;

import io.github.tatsunidas.radiomics.main.Validation.ValidationConfigType;

/**
 * Records the IBSI 1 validation reports of this version into docs/benchmark/ (run it at each release).
 * <p>
 * It is NOT part of the default test run (the name does not match the surefire patterns, and the CT phantom
 * takes minutes). Run it explicitly:
 * <pre>mvn -B test -Dtest=IbsiBenchmarkRecorder</pre>
 * The CT phantom (configurations C and D) is read from src/test/resources; the images are not distributed.
 */
public class IbsiBenchmarkRecorder {

	@Test
	public void record() throws Exception {
		File dir = new File("docs/benchmark");
		dir.mkdirs();
		String[][] runs = { { "P", "digital_phantom" }, { "C", "config_C" }, { "D", "config_D" } };
		for (String[] r : runs) {
			ValidationReport rep = Validation.report(ValidationConfigType.valueOf(r[0]));
			String base = "ibsi_1_report_" + rep.radiomicsjVersion() + "_" + r[1];
			Files.write(new File(dir, base + ".json").toPath(), rep.toJson().getBytes(StandardCharsets.UTF_8));
			Files.write(new File(dir, base + ".csv").toPath(), rep.toCsv().getBytes(StandardCharsets.UTF_8));
			System.out.println("RECORDED " + base + " " + rep.counts());
		}
	}
}
