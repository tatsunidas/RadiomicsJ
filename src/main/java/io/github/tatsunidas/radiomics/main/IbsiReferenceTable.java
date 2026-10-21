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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * The IBSI reference values (IBSI_ValidationFile.xlsx), read straight from a stream.
 * Nothing is written to the working directory (unlike the legacy loader of {@link Validation}).
 * Columns are found by their header names; a missing header column is an error, not a silent skip.
 */
final class IbsiReferenceTable {

	/** One row of a sheet. {@code value} is null when the table has no reference value. */
	static final class Entry {
		final String dataSet;
		final String family;
		final String imageBiomarker;
		final String radiomicsjName;
		final String consensus;
		final Double value;
		/** null when the table has no tolerance (0 or empty). */
		final Double tolerance;
		final String referenceStatus;

		Entry(String dataSet, String family, String imageBiomarker, String radiomicsjName, String consensus, Double value,
				Double tolerance, String referenceStatus) {
			this.dataSet = dataSet;
			this.family = family;
			this.imageBiomarker = imageBiomarker;
			this.radiomicsjName = radiomicsjName;
			this.consensus = consensus;
			this.value = value;
			this.tolerance = tolerance;
			this.referenceStatus = referenceStatus;
		}
	}

	final String sha256;
	final List<Entry> entries;

	private IbsiReferenceTable(String sha256, List<Entry> entries) {
		this.sha256 = sha256;
		this.entries = entries;
	}

	static IbsiReferenceTable read(InputStream in) throws IOException {
		byte[] bytes = readAll(in);
		List<Entry> out = new ArrayList<>();
		DataFormatter fmt = new DataFormatter();
		try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
			for (int s = 0; s < wb.getNumberOfSheets(); s++) {
				Sheet sheet = wb.getSheetAt(s);
				Row header = sheet.getRow(sheet.getFirstRowNum());
				if (header == null) {
					continue;
				}
				Map<String, Integer> col = new HashMap<>();
				for (Cell c : header) {
					col.put(fmt.formatCellValue(c).trim(), c.getColumnIndex());
				}
				for (String need : new String[] { "data_set", "family", "image_biomarker", "RadiomicsJ_NAME", "consensus",
						"benchmark_value", "tolerance" }) {
					if (!col.containsKey(need)) {
						throw new IOException("reference table sheet \"" + sheet.getSheetName() + "\": column \"" + need + "\" not found");
					}
				}
				for (int r = sheet.getFirstRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
					Row row = sheet.getRow(r);
					if (row == null) {
						continue;
					}
					String dataSet = text(row, col.get("data_set"), fmt);
					if (dataSet == null) {
						continue;
					}
					out.add(new Entry(dataSet, text(row, col.get("family"), fmt), text(row, col.get("image_biomarker"), fmt),
							text(row, col.get("RadiomicsJ_NAME"), fmt), text(row, col.get("consensus"), fmt),
							number(row, col.get("benchmark_value")), tolerance(number(row, col.get("tolerance"))),
							col.containsKey("reference_status") ? text(row, col.get("reference_status"), fmt) : null));
				}
			}
		}
		return new IbsiReferenceTable(sha256(bytes), out);
	}

	/** entries of one data set ("digital phantom", "configuration C", ...). */
	List<Entry> of(String dataSet) {
		List<Entry> l = new ArrayList<>();
		for (Entry e : entries) {
			if (e.dataSet.equals(dataSet)) {
				l.add(e);
			}
		}
		return l;
	}

	private static Double tolerance(Double t) {
		return t == null || t == 0d ? null : t;
	}

	private static String text(Row row, Integer c, DataFormatter fmt) {
		if (c == null) {
			return null;
		}
		Cell cell = row.getCell(c);
		if (cell == null || cell.getCellType() == CellType.BLANK) {
			return null;
		}
		String s = fmt.formatCellValue(cell).trim();
		return s.isEmpty() ? null : s;
	}

	private static Double number(Row row, Integer c) {
		if (c == null) {
			return null;
		}
		Cell cell = row.getCell(c);
		if (cell == null || cell.getCellType() != CellType.NUMERIC) {
			return null;
		}
		return cell.getNumericCellValue();
	}

	private static byte[] readAll(InputStream in) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		byte[] buf = new byte[16384];
		int n;
		while ((n = in.read(buf)) > 0) {
			bos.write(buf, 0, n);
		}
		return bos.toByteArray();
	}

	static String sha256(byte[] bytes) {
		try {
			byte[] d = MessageDigest.getInstance("SHA-256").digest(bytes);
			StringBuilder sb = new StringBuilder();
			for (byte b : d) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
