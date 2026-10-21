# IBSI 1 validation report

`Validation.report(ValidationConfigType)` runs the IBSI 1 benchmark and returns a structured
`ValidationReport` (one row per reference value). It prints nothing and writes no file, so an
application can show the result and a user can cite it.

```java
ValidationReport r = Validation.report(Validation.ValidationConfigType.P); // digital phantom
System.out.println(r.passed() + " / " + r.checked());
String json = r.toJson();   // includes RadiomicsJ version, SHA-256 of the reference table, settings, rule
String csv  = r.toCsv();    // one row per reference value
```

| type | data | works from |
|---|---|---|
| `P` | IBSI digital phantom (built in code) | the distributed jar |
| `C`, `D` | IBSI CT radiomics phantom, configurations C and D | the source tree (the CT images are not in the jar) |

Configurations A, B and E have no settings file and are refused (`IllegalArgumentException`).
The static settings of `RadiomicsJ` are changed while the report runs and are reset at the end:
call it while no other thread uses RadiomicsJ.

## Rows and verdicts

Each row of the reference table that has a reference value gets one verdict.

| verdict | meaning |
|---|---|
| `PASS` | the reference has a tolerance > 0 and `|computed - reference| <= tolerance` |
| `PASS_AT_REFERENCE_DIGITS` | no tolerance in the table; equal at N significant digits (see below) |
| `FAIL` | outside the tolerance, or different at the digits |
| `NOT_COMPUTED` | RadiomicsJ should provide the feature but produced no value |
| `NOT_SUPPORTED` | RadiomicsJ does not provide it (2D, 2.5D and slice-merged aggregations) |
| `NO_MANUAL_REFERENCE` | the table has a value that is not in the current IBSI manual; shown, never judged |

Diagnostics rows (another chapter of the manual) are not compared.

### The rule for references without a tolerance

The IBSI reference manual gives no tolerance for most digital-phantom values and **does not state how
to round a computed value**. RadiomicsJ therefore uses its own criterion and counts it separately from
a pass: the computed value and the reference, both rounded half up to
N = max(3, significant digits of the printed reference) digits, must be equal. All such references in
the table have at most three significant digits (this is tested). Do not report
`PASS_AT_REFERENCE_DIGITS` as an IBSI pass; report the two counts.

The relative 1% / 20% rules of `Validation.ibsiDigitalPhantom()` are **not** used by the report
(that method is unchanged).

## Results of this version

`docs/benchmark/ibsi_1_report_<version>_*.json` and `.csv` hold the reports of the digital phantom and of
configurations C and D, recorded with `mvn -B test -Dtest=IbsiBenchmarkRecorder` (not part of the default
test run; the CT phantom takes minutes). The digital phantom is validated in the default tests
(`TestValidationReport`).

## The reference table

`src/main/resources/validation/IBSI_ValidationFile.xlsx` was compared with the tables of the IBSI 1
reference manual (https://ibsi.readthedocs.io/en/latest/03_Image_features.html, read on 2026-10-21; the
manual states no version). All reference **values** of the digital phantom and of configurations C and D
agree with the manual. The tolerances of 91 rows (configuration C: 23, D: 68) differed and were changed to
the current manual's values: see `docs/benchmark/ibsi_1_reference_table_tolerance_changes.csv`
(`old_tolerance` / `new_tolerance`). Six rows that are not in the manual's tables have the status
`not-in-ibsi1-manual` in the new `reference_status` column. Configurations A, B and E are not used by
RadiomicsJ and were not changed.

## Sources and licence

IBSI reference manual and reference values: https://ibsi.readthedocs.io/ (Zwanenburg et al., Radiology
2020; the IBSI consortium). The digital phantom is licensed under CC BY 4.0 (original design: Alex
Zwanenburg; DICOM version courtesy of David Clunie); the CT radiomics phantom is licensed under CC BY-NC 3.0
and is not distributed with the jar. A passing report is a record that RadiomicsJ reproduces the IBSI
reference values; it is not a guarantee of clinical correctness.
