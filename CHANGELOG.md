# Changelog

## Unreleased

### Added (GPU foundation)

- `BOOL_USE_GPU` (default 1; `--gpu` / `--no-gpu` on the command line) and the package `io.github.tatsunidas.radiomics.gpu`:
  an OpenCL context (JOCL 2.0.5, an optional dependency, kept in the CLI and Python jars) that later changes use for
  GPU code. Nothing computes on the GPU yet, so no result changes. When JOCL, the OpenCL library or a GPU device is
  missing, `Gpu.get()` returns null with one log line and the CPU code runs. On the development machine (only
  `libOpenCL.so.1` installed) the GPU is reported as not usable until `libOpenCL.so` is on `LD_LIBRARY_PATH`.

### Added (GPU speed estimate)

- `GpuEstimator` (package `io.github.tatsunidas.radiomics.gpu`) estimates, for Gabor, the global intensity peak and the
  maximum 3D diameter, how long the CPU and the GPU take on the given problem size and which is faster. It measures
  the machine: small synthetic problems run through the real code on each side (about a minute, once), and
  `seconds = a + b * work + c * data` is fitted per task and side. The fit is stored in
  `~/.radiomicsj/gpu-calibration.json` for the same device, CPU core count and RadiomicsJ version. It only advises;
  what runs is still decided by `BOOL_USE_GPU`. A problem more than 10 times beyond the largest measured one is
  marked as extrapolated.
- Command line `--gpu-estimate` (typical sizes; `--gpu-recalibrate` measures again). Python:
  `RadiomicsJExtractor.estimate_gpu(shape, spacing, roi_voxels, mesh_vertices, ...)` returns a DataFrame.
- On the development machine (GTX 1050 Ti, 16 core CPU) the estimates were compared with runs that were not used for
  the fit: the faster side was predicted correctly in 12 of 12 cases, the CPU or GPU seconds were off by up to about 2 times.
- Note on the Gabor line above: measured again with the estimator's check, the same real CT case took 5.3 s on the CPU
  and 4.4 s on the GPU; the earlier 1.3 s on the CPU was not reproduced and its cause is not known.

### Added (Gabor on the GPU)

- `ImageFiltering.gaborFilter` convolves each plane on the GPU (direct sum, 32 bit float, `gabor.cl`) when a device is
  open, in place of the FFT; pooling and placing back on the grid are unchanged. A device failure falls back to the FFT
  with one warning. Against the CPU map on a volume of unequal voxel spacing the largest difference is 2e-4 % of the
  range or less (all four paddings, single and pooled 3D); the IBSI 2 test set 1 (4.a, 4.b) stays within its 1 %
  tolerance (largest 0.11 %), and the negative controls still fail.
- Speed is not better everywhere: on the development machine (GTX 1050 Ti, 16 core CPU) the real CT of 198x202x59 with
  sigma 5 mm, lambda 2 mm, gamma 1.5, MIRROR, MAX pooling of 4 orientations took 4.0 s on the GPU and 1.3 s on the CPU.
  `BOOL_USE_GPU=0` / `--no-gpu` selects the CPU.
- Python: `RadiomicsJExtractor(use_gpu=...)` sets `BOOL_USE_GPU`.

### Added (LocalIntensity global peak on the GPU)

- `GlobalIntensityPeak` takes the mean in the sphere of every voxel of the region on the GPU (single precision,
  `localpeak.cl`), then averages only the voxels within the float error bound of the largest one with the existing CPU
  code, so the value equals the CPU value bit for bit (4 synthetic cases and the real CT `STS_001`, ROI 117,145 voxels:
  the same 64 bit pattern). On the development machine (GTX 1050 Ti) the real CT took 0.07 to 0.14 s on the GPU and
  1.5 to 1.8 s on the CPU. When no device is open, or the sphere is one voxel, the existing loop runs.
- Unchanged and noted: the peak starts from 0, so a region whose sphere means are all negative gives 0.

### Changed, speed (maximum 3D diameter)

- `Morphological` maximum 3D diameter: the mesh lists a vertex once per triangle, so equal vertices are dropped before the
  all-pairs search (the maximum is unchanged), and with a GPU the device finds the points whose farthest neighbour is within
  the float error of the largest, which the CPU code then measures again. The value equals the old double loop bit for bit
  (4 synthetic clouds, and the IBSI CT `STS_001` mesh of 132,468 vertices). On the IBSI CT: old loop 60.2 s, new CPU 2.1 s,
  new GPU 0.03 to 0.14 s (GTX 1050 Ti).

### Changed, speed (NGTDM)

- `NGTDMFeatures.fillNGTDM` gathers the sum and the number of valid neighbours of every voxel in parallel (over the
  slices) instead of scanning the whole volume once per gray level with a list of neighbours per voxel. The matrix is
  finished in the voxel order z, y, x with the same double operations, so the values are the same: on the IBSI
  validation CT (STS_001) and on a 64^3 synthetic volume, all NGTDM features are equal bit for bit with the previous
  code, for delta 1 and 2. Measured on one machine (16 cores), one run each, the NGTDM family took 60.6 s (delta 1)
  and 142.3 s (delta 2) before and 0.31 s and 0.22 s after on the CT. `fillNGTDM` still does not reset `Ngp`, so a
  second call on the same object adds to it (unchanged).

### Changed, speed

- The mesh of the morphological features (marching cubes, `MCTriangulator`) reads the voxel values once instead
  of calling `getProcessor(z).getf()` at every cube corner, which made ImageJ run `setMinAndMax` each time. The
  mesh and every morphological value are the same: on the IBSI validation CT (STS_001, ROI 117,145 voxels)
  the 132,468 mesh vertices have the same SHA-256 and all 23 morphological values are equal bit for bit.
  Measured on one machine (16 cores), the mesh took 60.8 s before and 0.23 s after, and the morphological
  family 95.2 s before and 68.7 s after (one run each); what remains is mostly the all-pairs loop of the
  maximum 3D diameter.

## 2.5.2 (2026-10-21)

A command line that reports failures by its exit status, a structured IBSI 1 report
(`Validation.report`), no Log4j warning line any more, and dependency updates. The feature
values are the same as in 2.5.1. `loadSettings` now throws instead of ending the JVM, which a
calling program has to handle. The documentation of the Gabor and Simoncelli filters now states
what is known about their small differences from the IBSI 2 reference maps, and the limits of
the margin of the Simoncelli padding.

### Changed, settings files

- `loadSettings(String)` and `loadSettingsFromResource(String)` throw instead of ending the
  JVM with `System.exit(0)` when the properties file does not exist
  (`IllegalArgumentException`) or cannot be read (`UncheckedIOException`). A program that
  calls them now has to handle these exceptions. The command line prints the reason and
  ends with status 1.

### Changed, dependencies

- `log4j-to-jul` 2.26.1 is added at runtime, so the line `Log4j API could not find a logging
  provider.` that POI 5 caused on the first read of the validation Excel file is gone (the
  messages of POI go to `java.util.logging`). `log4j-api` is named as 2.26.1, where POI 5.5.1
  brings 2.24.3, because 2.24.3 is affected by GHSA-qv9r-c865-cp47 (invalid JSON for non-finite
  values in `MapMessage`, only with a JSON layout of Log4j Core; RadiomicsJ and POI do not use
  it). `log4j-to-jul` is optional, so a Maven project that depends on RadiomicsJ does not get
  it (its priority is higher than that of Log4j Core, and it would replace the logging of that
  project); it is in the jars of the Python package and of the command line.
- Test tooling: `maven-surefire-plugin` 3.6.0, `junit-jupiter-api` and
  `junit-jupiter-engine` 5.14.4 (both were 5.7.1). With surefire 2.22.2 the newer junit ran
  no test at all, so the update needed the newer surefire. The CI now fails when fewer
  tests ran than the sources declare.
- `maven-antrun-plugin` 3.2.0, `poi-ooxml` 5.5.1 (and its dependencies `commons-io` 2.21.0,
  `commons-compress` 1.28.0, `commons-lang3` 3.18.0, `commons-codec` 1.20.0,
  `commons-collections4` 4.5.0). The values read from the validation Excel file are the
  same as with 5.4.0.

### Fixed, command line

- `RadiomicsJ.main` now exits with status 1 when it fails: unparsable arguments, missing
  image or mask folders, unequal numbers of images and masks, an exception during the
  extraction, and test data that cannot be prepared. Before, these ended with status 0 and
  looked like a success to scripts. `-h` and a finished run still end with 0. A script that
  ignored the status of a failed run will see the failure from now on.
- `exec.mainClass` in the `pom.xml` named the old package `com.vis.radiomics`, so
  `mvn exec:java` could not find the class. It is now
  `io.github.tatsunidas.radiomics.main.RadiomicsJ`.
- When the extraction gives no result (for example a mask without a voxel of the target
  label), the command line prints the reason to stderr and exits with status 1. Before, it
  opened a dialog (`JOptionPane`), which fails with a `HeadlessException` where there is no
  display.

### Added

- `Validation.report(ValidationConfigType)` returns a structured `ValidationReport` of the IBSI 1 benchmark
  (digital phantom `P`; CT phantom `C` and `D` from the source tree): one row per reference value with
  the reference, the tolerance, the computed value and a verdict, plus the RadiomicsJ version, the SHA-256
  of the reference table and the settings. `toJson()` and `toCsv()` write it out. It prints nothing and
  writes no file. See `docs/IBSI_validation.md`.
- Verdict rule of the report: with a tolerance, `|computed - reference| <= tolerance`. Without a tolerance
  (the IBSI manual states neither a tolerance nor a rounding rule), RadiomicsJ's own criterion of equality
  at N = max(3, significant digits of the reference) digits, counted separately
  (`PASS_AT_REFERENCE_DIGITS`). The relative 1% / 20% rules of `ibsiDigitalPhantom()` are not used.
- Reports of this version for the digital phantom and configurations C and D in `docs/benchmark/`
  (`IbsiBenchmarkRecorder`).

### Changed

- The tolerances of 91 rows of `IBSI_ValidationFile.xlsx` (configuration C: 23, D: 68) now follow the
  current IBSI 1 reference manual; all reference values agree. Rows that are not in the manual's tables are
  marked in a new `reference_status` column. The old methods `ibsiDigitalPhantom()` and `ibsi_ct_PAT1()` are
  unchanged and keep their rules. See `docs/IBSI_validation.md` and
  `docs/benchmark/ibsi_1_reference_table_tolerance_changes.csv`.


## 2.5.1 (2026-10-21)

Security update of a dependency. The feature values are the same as in 2.5.0.

### Security

- `poi-ooxml` is updated from 4.1.2 to 5.4.0, which fixes CVE-2025-31672 (improper input
  validation in OOXML file parsing). Apache POI is used only to read the Excel files of the
  validation (`Validation`); feature extraction does not use it.

POI 5 uses the Log4j API, and no logging implementation is bundled. When the validation
reads its Excel file for the first time, Log4j prints one line, `Log4j API could not find a
logging provider.`, and carries on. The values read from the file are the same as with 4.1.2.

### Changed, dependencies

- Added as transitive dependencies of POI: `commons-io` 2.18.0, `commons-lang3` 3.16.0 and
  `log4j-api` 2.24.3 (the logging API only).
- Replaced `poi-ooxml-schemas` 4.1.2 by `poi-ooxml-lite` 5.4.0.
- Updated: `xmlbeans` 3.1.0 to 5.3.0, `commons-compress` 1.19 to 1.27.1, `commons-codec`
  1.13 to 1.17.1, `SparseBitSet` 1.2 to 1.3, `curvesapi` 1.06 to 1.08.

### Changed, build

- Build and documentation plugins updated: `maven-javadoc-plugin` 3.12.0,
  `exec-maven-plugin` 3.6.4.

## 2.5.0 (2026-10-20)

Adds the convolutional filters of IBSI 2 to the extraction: Laplacian of Gaussian,
mean, Laws kernels, Gabor, separable wavelets (Haar, Daubechies, Coiflet) and the
Simoncelli wavelet. Each is a setting that adds the features of the filtered image to the
same result row, under its own column prefix, and each matches the IBSI 2 reference
response maps and, on the CT phantom, the reference feature values (details below and in
`docs/`). Nothing changes while no filter is set.

**Two fixes change feature values**, whether or not a filter is used:
the tricubic spline interpolation (`TRICUBIC_SPLINE`) now interpolates, and four
diagnostics of the re-segmented morphological mask are corrected. See "Fixed,
interpolation" and "Fixed, diagnostics". Results computed with 2.4.1 or earlier through the
spline interpolation are not comparable with this release.

Slice-wise extraction (`BOOL_force2D`) no longer hides slices that failed.

### Added, image filtering

- **Laplacian of Gaussian filter as defined by IBSI 2**,
  `ImageFiltering.laplacianOfGaussian(imp, sigmaMm, truncate, padding, planar)`.
  - The kernel is equation 4.2 of the IBSI 2 reference manual (arXiv 2006.05470
    v9): sigma is given in mm and converted to voxels, and the kernel is cut off
    at `floor(truncate * sigma + 0.5)` voxels.
  - There are four paddings (constant, nearest, mirror, periodic), and the
    filter runs either on each axial slice or on the volume.
  - A bright blob responds negatively, and the kernel is neither scale
    normalised nor corrected to sum to zero, both as in the manual.
  - The response is per voxel squared, so the filter refuses a grid that is not
    isotropic (in plane only, for the planar filter). Resample first.
  - It matches the IBSI 2 consensus maps of test set 1 (2.a, 2.b, 2.c) to within
    0.04% of their range, against a tolerance of 1%.
  - Definition, arguments and timings: `docs/LoG_filter.md`.
- **Laplacian of Gaussian scales in the extraction settings.**
  `DOUBLEARRAY_logSigmaMm=1.5,3.0` adds the features of the filtered image to the
  result row, in columns prefixed `log_sigma1.5mm_` and `log_sigma3.0mm_`.
  `DOUBLE_logTruncate` (default 4) and `STRING_logPadding` (default `MIRROR`)
  complete the filter. Nothing changes when no scale is set.
  - The filter runs on the resampled image, so the voxels must be isotropic by
    then (in plane only with `BOOL_force2D`). Otherwise the extraction stops and
    says to set `DOUBLEARRAY_resamplingFactorXYZ`.
  - Re-segmentation and normalisation follow the intensities of the original, so
    the filtered images reuse its re-segmented mask. They are only discretised
    again, with the same bin settings: `DOUBLE_binWidth` is in the units of the
    original image, so a fixed bin number is the safer choice.
  - Morphology, Shape2D, diagnostics and operational information describe the
    original image and are not repeated.

- **Mean filter as defined by IBSI 2**, `ImageFiltering.meanFilter(imp, support, padding, planar)`,
  and `INTARRAY_meanSupport` / `STRING_meanPadding` in the extraction settings. Columns
  are prefixed `mean_M<support>_`. It matches the IBSI 2 consensus maps of tests 1.a.1
  to 1.a.4 and 1.b.1 (within 6e-6 % of their range). Details: `docs/MeanFilter.md`.
- **Laws kernel filter as defined by IBSI 2**,
  `ImageFiltering.lawsFilter(imp, kernels, padding, pooling, energyDelta, planar)`, and
  `STRING_lawsKernels` / `STRING_lawsPadding` / `STRING_lawsPooling` / `INT_lawsEnergyDelta`
  in the extraction settings. Columns are prefixed `laws_<kernel>[_<pooling>][_d<delta>]_`.
  Optional pseudo rotation invariance (the 24 right angle rotations of appendix A in 3D,
  4 in 2D; max, average or min pooling) and the energy map. It matches the nine IBSI 2
  consensus maps of tests 3.a, 3.b and 3.c (within 1e-5 % of their range). Details:
  `docs/LawsFilter.md`.
- **Gabor filter as defined by IBSI 2**,
  `ImageFiltering.gaborFilter(imp, sigmaMm, lambdaMm, gamma, theta, padding, pooling, dTheta, planar)`,
  and `STRING_gaborSets` / `STRING_gaborPadding` / `STRING_gaborPooling` / `DOUBLE_gaborDeltaTheta`
  in the extraction settings. The modulus of the 2D response, optionally pooled over
  orientations, and for a volume averaged over the three orthogonal planes. Columns are
  prefixed `gabor_s<sigma>_l<lambda>_g<gamma>_t<theta>[_<pooling>_dt<step>]_`. It matches the
  four IBSI 2 consensus maps of tests 4.a and 4.b (within 0.11 % of their range). Details:
  `docs/GaborFilter.md`.
- **Separable undecimated wavelet filter as defined by IBSI 2**,
  `ImageFiltering.waveletFilter(imp, wavelet, combination, level, padding, pooling, planar)`,
  and `STRING_waveletSets` / `STRING_waveletPadding` / `STRING_waveletPooling` in the
  extraction settings (Haar, Daubechies 2 to 4, Coiflet 1 to 3). Columns are prefixed
  `wavelet_<name>_<combination>_l<level>[_<pooling>]_`. Optional pseudo rotation invariance
  with the right angle rotations of appendix A. It matches the six IBSI 2 consensus maps of
  tests 5.a, 6.a and 7.a (within 1e-5 % of their range). Details: `docs/WaveletFilter.md`.
- **Simoncelli wavelet as defined by IBSI 2**,
  `ImageFiltering.simoncelliFilter(imp, level, padding, planar)`, and `INTARRAY_simoncelliLevels` /
  `STRING_simoncelliPadding` in the extraction settings. The B map of each level, computed in
  the Fourier domain (any length by Bluestein's algorithm). Columns are prefixed
  `simoncelli_l<level>_`. It matches the IBSI 2 consensus maps of tests 8.a.2 and 8.a.3 within 5e-6 %
  of their range and 8.a.1 within 0.38 % (cause of that residual unknown). Details:
  `docs/SimoncelliFilter.md`.
- **The Laplacian of Gaussian and the mean filter are two passes of one list.** Their
  settings and column names are as before.
- The features of filtered images agree with the IBSI 2 reference values of the CT
  phantom, configuration B: no filter, mean (M = 5), LoG (1.5 mm), Laws (L5E5E5,
  max pooling, energy map of distance 7), Gabor (5 mm, 2 mm, 3/2, average over steps of pi/8 and
  the orthogonal planes), Daubechies 3 (LLH level 1, HHH level 2, average pooling) and Simoncelli (levels 1 and 2) are 18 of 18 features within tolerance each, except 8.B (Simoncelli level 1), where the reference holds no value for the quartile coefficient of dispersion and 17 of 17 are within tolerance. See `docs/IBSI2_reference_features.md`.

### Changed, build and test

- **CI on real runners.** `mvn test` runs on Linux (JDK 11, 17, 21, and Java 8 as the
  runtime of a library built by JDK 21, which is how it is released), Windows and macOS;
  a second job builds the Python wheel, installs it in a clean environment and computes the
  IBSI 1 digital phantom on the three systems with Python 3.10 and 3.12. CodeQL and
  Dependabot are on. The GitHub Release takes its notes from this file.
- The developer entry of the pom reads Tatsunidas, Visionary Imaging Services, Inc. (the
  company name was misspelled).

### Fixed, interpolation

- **`TRICUBIC_SPLINE` interpolates.** It applied the cubic B-spline basis to the samples
  without the prefilter that turns them into spline coefficients, so it smoothed the
  image: the result did not pass through the samples, and it could not reach the
  overshoot of a spline. The prefilter is now applied, the weights are computed at the
  exact position instead of a 1/256 table, and the edges are mirrored.
  - **Values change** wherever the spline interpolation was used. On the IBSI 1 CT
    phantom at 2 mm the interpolated image now has the minimum -1111 and the
    maximum 2637 of the IBSI 1 configuration E reference (before -1000 and 1382),
    and the roi mean, minimum and maximum -48.3, -966 and 627 (before -49.6, -929
    and 459). `TestSplineInterpolation` checks it.
  - `TRICUBIC_POLYNOMIAL` and the other interpolations are as before.

### Fixed, slice-wise extraction (`BOOL_force2D`)

- **`extractAllSlice` no longer hides slices that failed.** It left a slice that raised an
  exception out of the result and printed a stack trace without the slice number; when
  every slice failed it returned `null`. It now names the slice and the reason, throws an
  `IllegalStateException` (with the first failure as its cause) when every slice that holds
  the mask failed, and otherwise returns the other slices with the skipped ones listed in
  `getSkippedSlices()` (1 based). Slices with a blank mask are still skipped without being
  failures. Columns and values of a successful run are unchanged.

### Fixed, diagnostics

- **The re-segmented morphological mask is the interpolated mask.**
  `DiagnosticsResegmentedROIMorphMaskVoxelCount` and
  `DiagnosticsResegmentedROIMorphMaskBoundingBoxDimensionX/Y/Z` carried the values
  of the re-segmented intensity mask. Re-segmentation (range filter, outlier
  removal) narrows only the intensity mask, and the morphological features are
  computed on the interpolated mask, so these four values now describe that
  mask.
  - The values change only when a range filter or outlier removal is used.
  - In a 512 voxel roi whose top layer is outside the range, the count is now
    512, not 448, and the z extent of the bounding box is now 8, not 7.

## 2.4.1

### Fixed, GLAM

- **The anisotropy warning is written once per run, not once per window.** GLAM
  warns that distances are measured on the voxel lattice when the spacing is not
  isotropic, and a feature visualization map builds one GLAM per analysis window
  from the same calibration: a single map over an anisotropic image left about
  64,000 identical lines, 9.5 MB, in the log. The warning now names a spacing at
  most once, and an extraction and a feature map each let the next image speak up
  again.

## 2.4.0

Makes the feature visualization map fast enough to offer GLAM on it. Nothing
about the values changes: every map and every feature comes out exactly as it
did in 2.3.0, only sooner.

### Changed, feature visualization map

- **The pixels are read out once.** Cutting an analysis window converted a whole
  slice of the source to float for every window that touched it. On a 512 by 512
  image that is a quarter of a million conversions per voxel of the map, which
  dwarfed the feature calculation the map exists to perform.
- **The rows of a slice are measured in parallel**, since every window is
  measured on its own and writes into its own cell of the output. Together with
  the change above, a GLCM map over a 12k voxel roi with a kernel of seven went
  from 13.2 s to 3.6 s on eight cores.
- `FeatureVisualizationMap.parallel` turns the threads off, for profiling or for
  callers that already spread whole maps across the cores themselves.
- `generateFeatureMap` takes a `ProgressListener`, told after every slice how far
  the map has got, since one over a whole stack can run for minutes and a caller
  that shows it to someone has to say so. Throwing from the listener aborts the
  map, which is how a caller cancels one.

### Changed, GLAM

- **The affinity matrices are built the first time one is asked for**, rather
  than all nineteen up front. Asking for every feature still builds all of them
  and costs what it did, because no two matrices share an expensive step. A
  feature map, which wants one matrix and pays for it once per window, got 2.2x
  faster.
- Splitting the gray levels across threads is now skipped when the caller is
  already inside a parallel job, where it only added scheduling overhead.

> Note for feature maps: GLAM measures every pair of voxels in a window, so its
> cost grows with roughly the sixth power of the kernel width. Measured on a 12k
> voxel roi at stride 3: kernel 7 took 0.45 s, kernel 11 took 2.7 s and kernel 15
> took 17 s. Give it a roi, not a whole slice.

## 2.3.0

Adds the GLAM feature family, gray level affinity metrics.

### Added, GLAM

GLAM asks the question a co-occurrence matrix asks, but at every distance at once.
It treats the roi voxels as a mixture of interacting particles and computes the
radial distribution function `g(alpha, beta, r)`: how likely a voxel of gray level
beta is at distance r from a voxel of gray level alpha, relative to pure chance.
From those curves it derives 19 nBins x nBins affinity matrices and summarises
each of them with 8 statistics, giving **150 new features**.

Reference: Physics-Informed Multiscale Decoding of Tissue Microstructure, The Gray
Level Affinity Metrics (GLAM) Framework, Journal of Imaging Informatics in
Medicine (2026), doi 10.1007/s10278-026-02132-6.

- `GLAMFeatures`, `GLAMFeatureType`, `GLAMMatrixType`, `GLAMStatistic`,
  and the package private `GLAMNumerics`.
- The affinity matrices: RDFPeakPosition, RDFDispersionRatio, LogRDFPeakHeight,
  LogRDFMedian, LogRDFVariance, LogRDFSkewness, LogRDFKurtosis,
  SecondVirialCoefficient, PotentialEnergy, Compressibility, CoordinationNumber,
  InverseCorrelationLength, StructuralPressureIndex,
  ConfigurationalDisorderIndex, WassersteinDistance, AssemblyCoupling,
  PhenotypicDistance, LocalPackingFraction, FrustrationIndex.
- `GLAMFeatures.getMatrix(GLAMMatrixType)` and
  `GLAMFeatures.getRadialDistributionFunction()` expose the descriptors
  themselves, for interpretation and visualisation.
- Settings: `BOOL_enableGLAM`, `INT_GLAM_maxRadius`,
  `BOOL_GLAM_boundaryCorrection`, `INT_GLAM_maxReferenceVoxels`,
  `INT_GLAM_numRandomisations`, `LONG_GLAM_randomSeed`,
  `INT_GLAM_savitzkyGolayWindow`, `INT_GLAM_savitzkyGolayPolynomial`,
  `DOUBLE_GLAM_peakProminence`, `INT_GLAM_maxLocalShellRadius`. They are
  documented in the sample properties files.
- Python wrapper: `radiomicsj.GLAM`, with `get_matrix`, `get_matrices`,
  `get_rdf` and the usual `get_all_features`.
- ImageJ plugin: a GLAM checkbox.
- `docs/GLAM_note_ja.md`, the write up with a worked example, and
  `docs/make_glam_figures.py` with the figures it uses.
- `docs/GLAM_benchmark_IBSI_CT.md`, a published reference result on the IBSI CT
  radiomics phantom: the exact conditions, the 150 feature values, the 19
  affinity matrices and the radial distribution function itself, so that another
  implementation can be compared and a regression here would be noticed. The
  preprocessing is IBSI configuration D, and the diagnostics reproduce the IBSI
  voxel counts (interpolated roi 45985, re-segmented 44465). The image is not
  redistributed, `docs/GLAM_benchmark_IBSI_CT.md` links to the IBSI data sets
  repository and gives the checksums. Generator:
  `src/test/java/radiomics/GLAMBenchmark.java`.
- `RadiomicsJ.getAnalysisReadyImage()` and `getAnalysisReadyMask()`, the image
  and intensity mask the feature families actually run on, once `preprocess()`
  or `execute()` has run.

GLAM is **off by default**. It is not part of the IBSI feature set, and it scans
every pair of roi voxels, so it costs more than the other families.

### Verified against the reference implementation

Five digital phantoms (checkerboard, layered sphere, random field, clustered
blobs, empty level) times two normalisation modes were compared element by
element against the python implementation that accompanies the paper.

| | result |
| --- | --- |
| radial distribution function | matches, relative error below 1e-9 |
| 18 of the 19 affinity matrices | matches, relative error below 1e-9 |
| InverseCorrelationLength | matches, relative error below 1e-4, see below |

`src/test/java/radiomics/TestGLAMFeatures.java` with the reference values in
`src/test/resources/glam/`.

### Four deliberate departures from the reference implementation

The first three are switchable, so the reference behaviour can be reproduced
exactly. The fourth is a correctness fix.

- **The randomised reference state is computed in closed form.** Several GLAM
  matrices compare the observed arrangement against a roi whose gray levels have
  been shuffled. Shuffling destroys every spatial correlation and leaves only the
  geometry of the roi plus the fact that a voxel is never its own neighbour, so
  that state has an exact expression. Measured against the Monte Carlo estimate,
  the deviation falls exactly as one over the square root of the number of
  shuffles (0.0165, 0.0048, 0.0059, 0.0018 for 10, 40, 160 and 640 shuffles), so
  the closed form is its unbiased limit. The reference draws four shuffles, which
  leaves a noise floor in every derived feature. Set `INT_GLAM_numRandomisations`
  above zero to use the sampled estimate instead.
- **Boundary correction is on by default.** The paper states that a geometric
  availability factor normalises each shell for roi edge effects, but the
  reference pipeline does not pass it and uses ideal spherical shells, which lets
  the shape of the roi leak into g(r). Set `BOOL_GLAM_boundaryCorrection=0` for
  the reference behaviour.
- **The correlation length fit is converged.** The reference calls curve_fit with
  `ftol=1e-3`, which stops well short of the minimum: refitting the identical
  data with a tight stopping rule reaches a strictly lower residual in 73 of 76
  cases.
- **A gray level that does not occur in the roi is reported as undefined, not as
  zero.** It has no reference voxel to measure from and a density of zero to
  divide by, so its whole row and column are undefined. The reference fills those
  cells with zero and then computes on them, which reports a second virial
  coefficient of exactly zero (no net affinity), a peak position of one and a
  finite phenotypic distance for a gray level that is not in the image; through
  `AssemblyCoupling`, which differences across the gray levels, those fabricated
  zeros also reach neighbouring levels that are perfectly good. RadiomicsJ marks
  them NaN and the reduction statistics skip them, so a feature is the statistic
  over the occupied gray levels. Empty bins are ordinary in practice: 2 of the 32
  bins are empty in the IBSI CT benchmark. Locked in by the `empty_level`
  phantom, which also checks that every occupied gray level still matches the
  reference exactly.

### Known limits of GLAM

- GLAM measures distances on the voxel lattice, so the roi should be resampled to
  isotropic voxels first. A warning is logged otherwise.
- GLAM is a three dimensional descriptor, defined over spherical shells. It is
  skipped under `force2D` and rejects single slice input rather than reporting
  numbers whose normalisation no longer matches their definition.
- The cost grows with the square of the roi size. On 8 cores a dense roi of 64000
  voxels takes about 7 s, one of 125000 voxels about 22 s. Sub sampling the
  centres with `INT_GLAM_maxReferenceVoxels=1000` ran about 3 times faster on a
  113000 voxel roi and moved the second virial coefficient by 0.15 %.
- **ConfigurationalDisorderIndex loses most of its information under boundary
  correction, and FrustrationIndex with it.** The index divides the observed
  ordering by how far the observed and the randomised state differ. Boundary
  correction pins the randomised state to one, which drives that difference to
  zero and the ratio to one: on a blocky phantom the spread over the matrix
  falls from 1.12 without the correction to 0.17 with it, and on the IBSI CT
  phantom every element sits at one. Where the arrangement really is random both
  logarithms approach zero instead and the ratio becomes numerically unstable.
  In other words its variation comes from the geometry of the roi rather than
  from the tissue. Set `BOOL_GLAM_boundaryCorrection=0` when those two matrices
  matter. See `docs/images/glam_boundary_correction.png`.
- Only the statistical mechanics classes of the framework are implemented. The
  geometric and topological classes (fractal dimension, lacunarity, Betti
  numbers, percolation, granulometry, nematic order) are not part of this
  release; `FractalFeatures` and `BettiNumberMap` already cover part of that
  ground.


### Changed, feature visualization map

- **The roi is grown by a margin before the analysis windows are cut**, 3 voxels
  by default, configurable through the new `margin` argument of
  `FeatureVisualizationMap.generate` / `generateFeatureMap` and of the python
  `generate_feature_map`. A voxel on the edge of the roi used to see a window
  the roi only partly filled, so its value was computed from fewer voxels than a
  voxel in the middle and differed for that reason alone. The margin only widens
  the mask the windows come from: the map still carries values exactly on the
  original roi and keeps the geometry of the input image. When the grown mask
  would reach past the edge of the image, the image is padded first by marching
  outwards from the border and filling each new voxel with the mean of the
  neighbours already known, which carries the border intensity outwards without
  inventing an edge. Pass `0` for the previous behaviour.

### Changed, build

- `central-publishing-maven-plugin` 0.3.0 to 0.8.0. 0.3.0 could upload a bundle
  but not read the status back, so `mvn deploy` ended in a BUILD FAILURE after a
  successful upload.

### Fixed

- The python test of the IBSI digital phantom looked for the data sets under
  `src/main/resources`, where they no longer are since 2.2.0 moved them to
  `src/test/resources`, and read the reference values through an absolute path
  from one developer machine.

## 2.2.0

This release makes RadiomicsJ pass the IBSI benchmark for the CT radiomics phantom.

Validation status against the reference values shipped in
`src/main/resources/validation/IBSI_ValidationFile.xlsx`:

| data set | before | after |
| --- | --- | --- |
| IBSI digital phantom | All Clear (3 rounding artefacts reported) | **All Clear, 0 no-match** |
| CT phantom, configuration C | could not run | **PASS** (165 clear, 1 no-match) |
| CT phantom, configuration D | could not run | **PASS** (168 clear, 0 no-match) |

The diagnostics now reproduce the reference voxel counts exactly
(interpolated ROI 45985, re-segmented ROI 45981 for config C and 44465 for config D,
interpolated dimensions 100x99x90).

### IMPORTANT : feature values change

Any workflow that uses interpolation(resampling) and/or re-segmentation produces
different values than 2.1.x. The results of a study should not be mixed between versions.
Feature values do NOT change when neither interpolation nor re-segmentation is used.

### Fixed, image processing

- **Interpolation grid alignment.** The interpolation grid is now centred on the centre of
  the original grid, as IBSI 5.2.1 requires. The scale is derived from the voxel spacings
  instead of the ceiling rounded grid size, that used to stretch the image by up to one
  voxel over the field of view. Applied to trilinear, nearest neighbour and both tricubic
  interpolations. (`Utils.gridOrigin`)
- **Intensity rounding after interpolation.** New setting
  `BOOL_interpolation_intensity_rounding`, required by IBSI table 5.1 for calibrated and
  discrete intensities such as CT Hounsfield Units. Default is off, so that continuous
  modalities (PET SUV, MR) are untouched. Enabled in the CT phantom validation settings.
- **Re-segmentation range is a closed interval.** `[rangeMin, rangeMax]` used to be
  evaluated exclusively, so voxels exactly on the boundary (e.g, -1000 HU on CT) were
  dropped from the roi.
- **Morphological mask and intensity mask are now separated** (IBSI 2.5, ECJF/G5KJ/SEFI).
  The morphological mask is not re-segmented, and is used by morphological features,
  by Shape2D and by the GLDZM distance to the roi edge. The intensity mask is used by
  intensity, histogram and texture features. Within the morphological family, centre of
  mass shift, integrated intensity, Moran's I index and Geary's C measure take their
  intensity voxel set from the intensity mask.
- **Morphological mesh is generated on the analysis mask.** The mask used to be resampled
  to 1 mm before meshing, which dilated the roi by the partial volume threshold, and a mesh
  decimation of 2 compensated for it. The two cancelled out only for 2 mm data sets.
- **Mask interpolation settings are applied.** `INT_interpolation_mask2D` and
  `INT_interpolation_mask3D` were parsed but never used, so masks were always resampled with
  the interpolation method of the image.
- **Masks can be resampled with tricubic interpolation.** `Utils.resample3D` returned null
  for masks when a tricubic method was selected, which crashed the extraction with a
  NullPointerException. Interpolated masks are now thresholded by
  `Utils.binarizeMaskByPartialVolume`.
- **2D mask binarisation** now uses `>= mask_PartialVolumeThreshold`, as the 3D path does,
  and clears the sub threshold values instead of keeping the interpolated fraction.

### Fixed, features

- **Axis aligned bounding box.** The minimum corner was initialised to zero, and mesh point
  coordinates are never negative, so the box started at the image origin and both volume and
  area density were far too small. It happened to be correct only when the roi touched the
  origin, as in the digital phantom.
- **Percentiles** now use the IBSI nearest rank definition, `ceil(k/100 * n)`. The former
  `floor(k/100 * n)` returned the next higher value whenever `k/100 * n` was an integer.
  Affects `Percentile10`, `Percentile90`, `Interquartile` and
  `QuartileCoefficientOfDispersion`, on both intensity statistics and intensity histogram.
- **Fixed bin width discretisation** no longer fails when the intensity range is narrower
  than the bin width. All voxels simply fall into the first bin, which is a valid result and
  used to end in a NullPointerException.
- **Single bin histograms** no longer throw ArrayIndexOutOfBoundsException in the four
  histogram gradient features. Reachable with `INT_binCount=1` as well.
- Division guards for `CoefficientOfVariation` (zero mean) and
  `QuartileCoefficientOfDispersion` (P75 + P25 = 0).

### Fixed, settings and state

- **Settings no longer leak between instances.** They are static fields, so a previous
  instance, or a previous run in the same JVM, used to keep its settings alive.
  The constructor now restores the defaults through `RadiomicsJ.resetSettings()`, and
  `loadSettings()` applies the user values on top. Set static fields directly only *after*
  `new RadiomicsJ()`.
- **A fixed bin width run no longer overwrites `RadiomicsJ.nBins`.** The bin count derived
  from the discretisation is kept separately, as an instance value.
- **`RadiomicsJ.discretiseImp` is cleared when an extraction finishes.** This static is a
  hand-off to the feature classes so that they can skip re-discretisation. It used to
  survive the extraction, so a later direct use of a feature class or of
  `FeatureVisualizationMap` in the same JVM silently reused the discretised image of the
  previous extraction.
- `feature == Double.NaN` is always false in Java. 13 occurrences replaced by
  `Double.isNaN(feature)`.
- 2D extraction accepts a null mask and creates a full face mask, as the 3D path does.
- `extractAllSlice` reads the last row of the per slice table, not row 0. In the ImageJ GUI,
  a results window left open made every slice repeat the values of a previous run.
- `Utils.resample2D` returns a copy when the image is already at the requested spacing,
  instead of the input object itself.
- `Utils.createRoi` rejects slice position 0 with a clear message. Slice positions are
  1 to N, as in ImageJ.
- A negative `DOUBLE_densityShift` is reported instead of being silently ignored.

### Fixed, packaging and tooling

- **The bundled data sets are reachable again.** `TestDataLoader` looks them up under
  `data_sets-master/`, but the build copied their contents to the classpath root, so
  `RadiomicsJ -t`, `Validation.ibsi_ct_PAT1()` and the plugin demo all failed with a
  NullPointerException. The resource target path is fixed, and resources are now extracted
  to a temporary file when they live inside a jar. The validation data path
  `ibsi_1_validation` is corrected to `ibsi_validation`.
- **The validation program compared rounded values.** It read results through
  `ResultsTable.getStringValue()`, which formats with 3 decimal places, turning 0.00155 into
  0.002 and reporting a 30% error. The raw double is used now. The three no-matches that the
  digital phantom used to report were artefacts of this.

### Changed, distribution

- **The IBSI data sets are no longer packaged into the jar.** They moved from
  `src/main/resources/data_sets-master` to `src/test/resources/data_sets-master`.

  | | 2.1.18 | 2.2.0 |
  | --- | --- | --- |
  | `radiomicsj-<version>.jar` | 26.6 MB | **0.50 MB** |
  | PyPI package (`radiomicsj/jars/`) | 47.8 MB | **21.7 MB** |

  The IBSI digital phantom validation is unaffected and still runs from the distributed
  jar, because `TestDataLoader.digital_phantom1_scratch()` builds the phantom in code.
  The reference values (`validation/IBSI_ValidationFile.xlsx`) and the settings files stay
  in the jar as well.

  Everything that reads a bundled data set is now a **debug only** feature, that needs
  `src/test/resources` (`target/test-classes`) on the classpath;
  `Validation.ibsi_ct_PAT1()`, `TestDataLoader.digital_phantom1()`, `sample_ct1()`,
  `validationDataAt()`, the CLI `-t -tdt 1` and `-tdt 2`, and the plugin demo main.
  They log what is missing and how to run them, instead of throwing a
  NullPointerException. The CLI `-t -tdt 0` now uses the in-code phantom, so it keeps
  working from the distributed jar.

### Added

- `RadiomicsJ.resetSettings()`
- `Utils.gridOrigin(int n, int newN, double scale)`
- `Utils.roundIntensitiesToNearestInteger(ImagePlus)`
- `Utils.binarizeMaskByPartialVolume(ImagePlus)`
- `MorphologicalFeatures(ImagePlus img, ImagePlus mask, ImagePlus intensityMask, int label)`
- `GLDZMFeatures(ImagePlus img, ImagePlus mask, ImagePlus morphoMask, int label, boolean useBinCount, Integer nBins, Double binWidth)`
- Setting `BOOL_interpolation_intensity_rounding`
- README examples for 3D extraction, 2D(force2D) extraction, and 3D/2D feature
  visualisation maps

Existing constructors and signatures are kept, and delegate with the new argument set to
null, so callers of 2.1.x keep compiling and behave as before.

### Known issues

- Configuration C reports one no-match,
  `IntensityVolumeHistogram_IntensityAtVolumeFraction10` (86.25 against 88.8). The reference
  provides no tolerance for this row, and the validator classifies it as an ignorable error.
- Configurations A, B and E of the CT phantom are not covered.
  `ParamsTestCT_PAT1_Config_E.properties` is missing, and A and B are 2D configurations
  whose settings files do not exist.
