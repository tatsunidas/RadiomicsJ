/*
 * Copyright [2022] [Tatsuaki Kobayashi]

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package io.github.tatsunidas.radiomics.main;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Structured result of the IBSI 1 validation, see {@link Validation#report(Validation.ValidationConfigType)}.
 * <p>
 * Unlike {@link Validation#ibsiDigitalPhantom()}, building a report prints nothing, writes no file and
 * returns one row per reference value, so that an application can show it and a user can cite it.
 *
 * <h3>Rule of the verdict</h3>
 * <ul>
 * <li>The reference has a tolerance &gt; 0: {@link Verdict#PASS} when |computed - reference| &lt;= tolerance.</li>
 * <li>The reference has no tolerance (0 or empty in the table): the IBSI reference manual gives no tolerance
 * for such values and does <b>not</b> state how to round the computed value. This library therefore uses
 * its <b>own</b> criterion: the computed value and the reference, both rounded half up to N significant digits
 * (N = max(3, digits of the printed reference value)), are equal ({@link Verdict#PASS_AT_REFERENCE_DIGITS}).
 * It is counted separately and must not be reported as an IBSI pass.</li>
 * <li>Otherwise {@link Verdict#FAIL}.</li>
 * </ul>
 * The relative 1% / 20% rules of {@link Validation#ibsiDigitalPhantom()} are not used here.
 */
public final class ValidationReport {

	/** Text of the rule, written into every JSON. */
	public static final String RULE = "tolerance > 0: pass when |computed - reference| <= tolerance. "
			+ "No tolerance: the IBSI manual states neither a tolerance nor a rounding rule, so this library uses its own "
			+ "criterion (computed value and reference rounded half up to N = max(3, digits of the printed reference) "
			+ "significant digits are equal), counted separately from a pass. The relative 1%/20% rules of Validation.ibsiDigitalPhantom() are not used.";

	public enum Verdict {
		/** tolerance &gt; 0 and the computed value is inside it. */
		PASS,
		/** no tolerance in the table; equal at N significant digits (this library's own criterion, see {@link ValidationReport#RULE}). */
		PASS_AT_REFERENCE_DIGITS,
		/** the computed value is outside the tolerance, or differs at the reference decimals. */
		FAIL,
		/** a RadiomicsJ column is expected for the reference, but no (or NaN) value was produced. */
		NOT_COMPUTED,
		/** RadiomicsJ does not provide the feature (e.g. 2D / 2.5D / slice-merged aggregation, or no column name). */
		NOT_SUPPORTED,
		/** the reference is in the table but not in the current IBSI manual; shown only, never judged. */
		NO_MANUAL_REFERENCE
	}

	/** One reference value and what RadiomicsJ computed for it. */
	public static final class Row {
		public final String family;
		public final String imageBiomarker;
		public final String radiomicsjName;
		/** RadiomicsJ result column, or null when there is none. */
		public final String column;
		public final String consensus;
		public final double reference;
		/** null when the table gives no tolerance. */
		public final Double tolerance;
		/** null when not computed. */
		public final Double computed;
		public final Verdict verdict;
		/** text of the table's reference_status column, or null. */
		public final String referenceStatus;

		Row(String family, String imageBiomarker, String radiomicsjName, String column, String consensus, double reference,
				Double tolerance, Double computed, Verdict verdict, String referenceStatus) {
			this.family = family;
			this.imageBiomarker = imageBiomarker;
			this.radiomicsjName = radiomicsjName;
			this.column = column;
			this.consensus = consensus;
			this.reference = reference;
			this.tolerance = tolerance;
			this.computed = computed;
			this.verdict = verdict;
			this.referenceStatus = referenceStatus;
		}
	}

	private final String dataSet;
	private final String radiomicsjVersion;
	private final String referenceFile;
	private final String referenceSha256;
	private final String settingsFile;
	private final String settings;
	private final List<Row> rows;
	private final int skippedDiagnostics;

	ValidationReport(String dataSet, String radiomicsjVersion, String referenceFile, String referenceSha256,
			String settingsFile, String settings, List<Row> rows, int skippedDiagnostics) {
		this.dataSet = dataSet;
		this.radiomicsjVersion = radiomicsjVersion;
		this.referenceFile = referenceFile;
		this.referenceSha256 = referenceSha256;
		this.settingsFile = settingsFile;
		this.settings = settings;
		this.rows = Collections.unmodifiableList(new ArrayList<>(rows));
		this.skippedDiagnostics = skippedDiagnostics;
	}

	/** "digital phantom", "configuration C" or "configuration D". */
	public String dataSet() {
		return dataSet;
	}

	public String radiomicsjVersion() {
		return radiomicsjVersion;
	}

	/** classpath name of the reference table. */
	public String referenceFile() {
		return referenceFile;
	}

	/** SHA-256 (hex) of the reference table that was read. */
	public String referenceSha256() {
		return referenceSha256;
	}

	/** the settings file (.properties) used, and its text. */
	public String settingsFile() {
		return settingsFile;
	}

	public String settings() {
		return settings;
	}

	public List<Row> rows() {
		return rows;
	}

	/** Diagnostics rows of the table (another chapter of the manual), which are not compared. */
	public int skippedDiagnostics() {
		return skippedDiagnostics;
	}

	public Map<Verdict, Integer> counts() {
		Map<Verdict, Integer> m = new EnumMap<>(Verdict.class);
		for (Verdict v : Verdict.values()) {
			m.put(v, 0);
		}
		for (Row r : rows) {
			m.put(r.verdict, m.get(r.verdict) + 1);
		}
		return m;
	}

	/** rows with a computed value that were judged: PASS + PASS_AT_REFERENCE_DIGITS + FAIL. */
	public int checked() {
		Map<Verdict, Integer> c = counts();
		return c.get(Verdict.PASS) + c.get(Verdict.PASS_AT_REFERENCE_DIGITS) + c.get(Verdict.FAIL);
	}

	public int passed() {
		Map<Verdict, Integer> c = counts();
		return c.get(Verdict.PASS) + c.get(Verdict.PASS_AT_REFERENCE_DIGITS);
	}

	public String toJson() {
		StringBuilder sb = new StringBuilder(64 * 1024);
		sb.append("{\n");
		kv(sb, "dataSet", dataSet, true);
		kv(sb, "radiomicsjVersion", radiomicsjVersion, true);
		kv(sb, "referenceFile", referenceFile, true);
		kv(sb, "referenceSha256", referenceSha256, true);
		kv(sb, "settingsFile", settingsFile, true);
		kv(sb, "settings", settings, true);
		kv(sb, "rule", RULE, true);
		sb.append("  \"skippedDiagnostics\": ").append(skippedDiagnostics).append(",\n");
		sb.append("  \"checked\": ").append(checked()).append(",\n");
		sb.append("  \"counts\": {");
		boolean first = true;
		for (Map.Entry<Verdict, Integer> e : counts().entrySet()) {
			sb.append(first ? "" : ", ").append('"').append(e.getKey().name()).append("\": ").append(e.getValue());
			first = false;
		}
		sb.append("},\n  \"rows\": [\n");
		for (int i = 0; i < rows.size(); i++) {
			Row r = rows.get(i);
			sb.append("    {");
			sb.append("\"family\": ").append(str(r.family));
			sb.append(", \"imageBiomarker\": ").append(str(r.imageBiomarker));
			sb.append(", \"radiomicsjName\": ").append(str(r.radiomicsjName));
			sb.append(", \"column\": ").append(str(r.column));
			sb.append(", \"consensus\": ").append(str(r.consensus));
			sb.append(", \"reference\": ").append(num(r.reference));
			sb.append(", \"tolerance\": ").append(num(r.tolerance));
			sb.append(", \"computed\": ").append(num(r.computed));
			sb.append(", \"verdict\": \"").append(r.verdict.name()).append('"');
			sb.append(", \"referenceStatus\": ").append(str(r.referenceStatus));
			sb.append(i == rows.size() - 1 ? "}\n" : "},\n");
		}
		sb.append("  ]\n}\n");
		return sb.toString();
	}

	public String toCsv() {
		StringBuilder sb = new StringBuilder(32 * 1024);
		sb.append("family,image_biomarker,radiomicsj_name,column,consensus,reference,tolerance,computed,verdict,reference_status\n");
		for (Row r : rows) {
			sb.append(csv(r.family)).append(',').append(csv(r.imageBiomarker)).append(',').append(csv(r.radiomicsjName)).append(',')
					.append(csv(r.column)).append(',').append(csv(r.consensus)).append(',').append(r.reference).append(',')
					.append(r.tolerance == null ? "" : r.tolerance.toString()).append(',')
					.append(r.computed == null ? "" : r.computed.toString()).append(',').append(r.verdict.name()).append(',')
					.append(csv(r.referenceStatus)).append('\n');
		}
		return sb.toString();
	}

	private static void kv(StringBuilder sb, String k, String v, boolean comma) {
		sb.append("  \"").append(k).append("\": ").append(str(v)).append(comma ? ",\n" : "\n");
	}

	private static String num(Double d) {
		return d == null || d.isNaN() || d.isInfinite() ? "null" : d.toString();
	}

	private static String str(String s) {
		if (s == null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder(s.length() + 2).append('"');
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			switch (c) {
			case '"':
				sb.append("\\\"");
				break;
			case '\\':
				sb.append("\\\\");
				break;
			case '\n':
				sb.append("\\n");
				break;
			case '\r':
				sb.append("\\r");
				break;
			case '\t':
				sb.append("\\t");
				break;
			default:
				if (c < 0x20) {
					sb.append(String.format("\\u%04x", (int) c));
				} else {
					sb.append(c);
				}
			}
		}
		return sb.append('"').toString();
	}

	private static String csv(String s) {
		if (s == null) {
			return "";
		}
		if (s.indexOf(',') < 0 && s.indexOf('"') < 0 && s.indexOf('\n') < 0) {
			return s;
		}
		return "\"" + s.replace("\"", "\"\"") + "\"";
	}
}
