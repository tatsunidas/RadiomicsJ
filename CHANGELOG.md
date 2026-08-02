# Changelog

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
