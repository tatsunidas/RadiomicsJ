# Changelog

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
