package io.github.tatsunidas.radiomics.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import ij.ImagePlus;
import io.github.tatsunidas.radiomics.main.Validation.ValidationConfigType;
import io.github.tatsunidas.radiomics.main.ValidationReport.Verdict;

/**
 * The structured IBSI 1 report: it reproduces the measured counts, judges with the documented rule,
 * and must fail when the reference or the settings are wrong (negative tests).
 */
public class TestValidationReport {

	private static byte[] resourceBytes(String path) throws Exception {
		try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			byte[] b = new byte[8192];
			int n;
			while ((n = in.read(b)) > 0) {
				bos.write(b, 0, n);
			}
			return bos.toByteArray();
		}
	}

	private static Map<Verdict, Integer> noZeros(Map<Verdict, Integer> m) {
		Map<Verdict, Integer> r = new TreeMap<>();
		for (Map.Entry<Verdict, Integer> e : m.entrySet()) {
			if (e.getValue() != 0) {
				r.put(e.getKey(), e.getValue());
			}
		}
		return r;
	}

	@Test
	public void digitalPhantomReproducesTheMeasuredCounts() throws Exception {
		ValidationReport r = Validation.report(ValidationConfigType.P);
		// measured with RadiomicsJ 2.5.1 (2026-10-21)
		assertEquals(168, r.checked(), "checked rows");
		assertEquals(25, r.counts().get(Verdict.PASS));
		assertEquals(143, r.counts().get(Verdict.PASS_AT_REFERENCE_DIGITS));
		assertEquals(0, r.counts().get(Verdict.FAIL), () -> "failures: " + failures(r));
		assertEquals(1, r.counts().get(Verdict.NOT_COMPUTED), "reference without a RadiomicsJ column: DependenceCountPercentage");
		assertEquals(177, r.counts().get(Verdict.NOT_SUPPORTED), "2D / 2.5D / slice-merged aggregations RadiomicsJ does not provide");
		assertEquals(3, r.counts().get(Verdict.NO_MANUAL_REFERENCE));
		assertEquals(349, r.rows().size());
		assertEquals(168, r.passed());
		assertEquals(ValidationReport.class.getName(), r.getClass().getName());
		// the table is identified by its SHA-256, computed here independently
		assertEquals(IbsiReferenceTable.sha256(resourceBytes("validation/IBSI_ValidationFile.xlsx")), r.referenceSha256());
		assertTrue(r.settings().contains("INT_binCount=6"));
		assertEquals("digital phantom", r.dataSet());
		String json = r.toJson();
		assertTrue(json.contains("\"rule\""));
		assertTrue(json.contains("\"referenceSha256\": \"" + r.referenceSha256() + "\""));
		assertEquals(349, json.split("\"verdict\"", -1).length - 1);
		assertEquals(350, r.toCsv().split("\n").length, "header + 349 rows");
	}

	private static List<String> failures(ValidationReport r) {
		List<String> l = new ArrayList<>();
		for (ValidationReport.Row row : r.rows()) {
			if (row.verdict == Verdict.FAIL) {
				l.add(row.family + "/" + row.imageBiomarker + " computed=" + row.computed + " reference=" + row.reference);
			}
		}
		return l;
	}

	@Test
	public void reportPrintsNothingAndWritesNoFile() throws Exception {
		Map<String, Long> before = tree(new File("."));
		PrintStream out0 = System.out;
		PrintStream err0 = System.err;
		ByteArrayOutputStream outBuf = new ByteArrayOutputStream();
		ByteArrayOutputStream errBuf = new ByteArrayOutputStream();
		try {
			System.setOut(new PrintStream(outBuf));
			System.setErr(new PrintStream(errBuf));
			Validation.report(ValidationConfigType.P);
		} finally {
			System.setOut(out0);
			System.setErr(err0);
		}
		assertEquals("", outBuf.toString(), "stdout");
		assertEquals("", errBuf.toString(), "stderr");
		assertEquals(before, tree(new File(".")), "working directory (file names and modification times) is unchanged");
	}

	/** file name -> modification time, below the working directory (target and .git excluded). */
	private static Map<String, Long> tree(File dir) {
		Map<String, Long> m = new TreeMap<>();
		File[] fs = dir.listFiles();
		if (fs == null) {
			return m;
		}
		for (File f : fs) {
			if (f.getName().equals("target") || f.getName().equals(".git")) {
				continue;
			}
			m.put(f.getPath(), f.lastModified());
			if (f.isDirectory()) {
				m.putAll(tree(f));
			}
		}
		return m;
	}

	@Test
	public void aChangedReferenceValueFailsOnlyThatFeature() throws Exception {
		byte[] xlsx = resourceBytes("validation/IBSI_ValidationFile.xlsx");
		ValidationReport base = Validation.report(ValidationConfigType.P);
		// change the reference of Statistics / Mean in the digital phantom sheet (2.15 -> 3.15)
		byte[] changed;
		try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
			Sheet s = wb.getSheet("digital phantom");
			boolean done = false;
			for (Row row : s) {
				Cell fam = row.getCell(1);
				Cell name = row.getCell(3);
				if (fam != null && name != null && "Statistics".equals(fam.getStringCellValue()) && "Mean".equals(name.getStringCellValue())) {
					Cell v = row.getCell(5);
					assertEquals(2.15, v.getNumericCellValue(), 1e-12);
					v.setCellValue(3.15);
					done = true;
				}
			}
			assertTrue(done, "the row to change exists");
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			wb.write(bos);
			changed = bos.toByteArray();
		}
		ValidationReport r = Validation.report(ValidationConfigType.P, new ByteArrayInputStream(changed), null);
		assertEquals(1, r.counts().get(Verdict.FAIL), () -> "failures: " + failures(r));
		ValidationReport.Row failed = null;
		for (ValidationReport.Row row : r.rows()) {
			if (row.verdict == Verdict.FAIL) {
				failed = row;
			}
		}
		assertEquals("Mean", failed.radiomicsjName);
		assertEquals("Statistics", failed.family);
		assertEquals(base.passed() - 1, r.passed(), "every other feature is unchanged");
		assertNotEquals(base.referenceSha256(), r.referenceSha256());
	}

	@Test
	public void aDifferentBinCountFailsTheDiscretisedFamiliesOnly() throws Exception {
		ValidationReport base = Validation.report(ValidationConfigType.P);
		String settings = base.settings().replace("INT_binCount=6", "INT_binCount=5");
		assertNotEquals(base.settings(), settings);
		ValidationReport r = Validation.report(ValidationConfigType.P, null, settings);
		assertTrue(r.counts().get(Verdict.FAIL) > 0, "bin count 6 -> 5 must be detected");
		List<String> discretised = Arrays.asList("Intensity histogram", "Co-occurrence matrix (3D, averaged)", "Run length matrix (3D, averaged)",
				"Size zone matrix (3D)", "Distance zone matrix (3D)", "Neighbourhood grey tone difference matrix (3D)",
				"Neighbouring grey level dependence matrix (3D)");
		int failInDiscretised = 0;
		for (ValidationReport.Row row : r.rows()) {
			if (row.verdict == Verdict.FAIL) {
				assertTrue(discretised.contains(row.family), "a failure outside the discretised families: " + row.family + "/" + row.imageBiomarker);
				failInDiscretised++;
			}
		}
		assertTrue(failInDiscretised > 0);
		// Morphology, local intensity and the first-order statistics do not depend on the bin count and stay as before
		for (int i = 0; i < r.rows().size(); i++) {
			ValidationReport.Row a = base.rows().get(i);
			ValidationReport.Row b = r.rows().get(i);
			if (a.family.equals("Morphology") || a.family.equals("Local intensity") || a.family.equals("Statistics")) {
				assertEquals(a.verdict, b.verdict, a.family + "/" + a.imageBiomarker);
			}
		}
	}

	@Test
	public void theVerdictRule() {
		// with a tolerance
		assertEquals(Verdict.PASS, Validation.judge(10.4, 10, 0.4));
		assertEquals(Verdict.PASS, Validation.judge(9.6, 10, 0.4));
		assertEquals(Verdict.FAIL, Validation.judge(10.41, 10, 0.4));
		assertEquals(Verdict.PASS, Validation.judge(0.0155, 0.015, 0.0005), "floating point at the edge of the tolerance");
		assertEquals(Verdict.FAIL, Validation.judge(86.25, 88.8, null), "the CT phantom Intensity at 10% volume (no tolerance in the old table)");
		// no tolerance: 3 significant digits at least
		assertEquals(Verdict.PASS_AT_REFERENCE_DIGITS, Validation.judge(1494.6, 1490, null));
		assertEquals(Verdict.FAIL, Validation.judge(1496, 1490, null));
		assertEquals(Verdict.PASS_AT_REFERENCE_DIGITS, Validation.judge(2.1549, 2.15, null));
		assertEquals(Verdict.FAIL, Validation.judge(2.16, 2.15, null));
		assertEquals(Verdict.PASS_AT_REFERENCE_DIGITS, Validation.judge(8.004, 8, null));
		assertEquals(Verdict.FAIL, Validation.judge(8.2, 8, null));
		assertEquals(Verdict.PASS_AT_REFERENCE_DIGITS, Validation.judge(-0.1568, -0.157, null));
		assertEquals(Verdict.FAIL, Validation.judge(-0.1576, -0.157, null));
		// the legacy rule (relative error < 1% passes) is NOT used: 1% off must fail
		assertEquals(Verdict.FAIL, Validation.judge(100.9, 100, null));
	}

	@Test
	public void referencesWithoutToleranceHaveAtMostThreeSignificantDigits() throws Exception {
		// the table convention that the "N = max(3, digits)" rule is based on
		IbsiReferenceTable t;
		try (InputStream in = new ByteArrayInputStream(resourceBytes("validation/IBSI_ValidationFile.xlsx"))) {
			t = IbsiReferenceTable.read(in);
		}
		int checked = 0;
		for (String ds : new String[] { "digital phantom", "configuration C", "configuration D" }) {
			for (IbsiReferenceTable.Entry e : t.of(ds)) {
				if (e.value != null && e.tolerance == null && !e.family.startsWith("Diagnostics")) {
					int digits = BigDecimal.valueOf(e.value).stripTrailingZeros().precision();
					assertTrue(digits <= 3, ds + " / " + e.imageBiomarker + " = " + e.value + " has " + digits + " significant digits");
					checked++;
				}
			}
		}
		assertTrue(checked > 300, "rows checked: " + checked);
	}

	@Test
	public void configurationsWithoutSettingsAreRefused() {
		for (ValidationConfigType t : new ValidationConfigType[] { ValidationConfigType.A, ValidationConfigType.B, ValidationConfigType.E }) {
			assertThrows(IllegalArgumentException.class, () -> Validation.report(t));
		}
	}

	@Test
	public void theCodeBuiltDigitalPhantomEqualsTheDistributedFiles() {
		ImagePlus[] scratch = TestDataLoader.digital_phantom1_scratch();
		ImagePlus[] files = TestDataLoader.digital_phantom1();
		for (int k = 0; k < 2; k++) {
			assertEquals(files[k].getStackSize(), scratch[k].getStackSize());
			assertEquals(files[k].getWidth(), scratch[k].getWidth());
			assertEquals(files[k].getHeight(), scratch[k].getHeight());
			int voxels = 0;
			for (int z = 1; z <= files[k].getStackSize(); z++) {
				for (int y = 0; y < files[k].getHeight(); y++) {
					for (int x = 0; x < files[k].getWidth(); x++) {
						assertEquals(files[k].getStack().getProcessor(z).getPixelValue(x, y),
								scratch[k].getStack().getProcessor(z).getPixelValue(x, y), 0d, "image " + k + " at " + x + "," + y + "," + z);
						voxels++;
					}
				}
			}
			assertTrue(voxels > 0);
		}
	}

	@Test
	public void theLegacyMethodStillReturnsTrue() {
		assertFalse(Validation.ibsiDigitalPhantom() == false);
	}
}
