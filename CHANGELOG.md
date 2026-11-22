# Changelog

## 2.7.4 (2026-11-22)

An audit of the open points of 2.7.2 (the list "Known and not changed") against the IBSI definition and an independent implementation
(numpy, scipy). The NGTDM now follows IBSI in the cases where it did not, and the zone search of the GLDZM is linear in time.
**A normal region of interest gives the same values as 2.7.3** (checked: the values of a normal region are equal to the 2.7.3 values, and the IBSI 1
reports are the same); only a degenerate region, or a region with a voxel without neighbour, changes.

### Fixed
- **NGTDM, uniform and degenerate regions** (IBSI, the neighbouring grey tone difference based features): the coarseness is 10^6 when its denominator
  is 0 (it was Infinity for a uniform region); the contrast and the busyness are 0 when N_g,p = 1 (they were NaN for a uniform region).
- **NGTDM, N_g,p** is the number of grey levels with p_i > 0, as IBSI defines it. It was the number of grey levels with s_i != 0, so two flat blocks that do not touch
  (every s_i is 0) gave N_g,p = 0 and a contrast of NaN.
- **NGTDM, a voxel without any neighbour** in the region is no longer counted in n_i and N_v,c (IBSI: a voxel without neighbour is excluded, and adds nothing to s_i).
  A region with an isolated voxel gave slightly wrong values for every NGTDM feature. A region in which no voxel has a neighbour (one voxel) has N_v,c = 0:
  IBSI gives no value, and the features are NaN (the strength is 0, as IBSI says for a sum of s_i of 0).
- **GLDZM, time**: the search of the 26-connected zones is a breadth first search over a visited array. The former search took about n^7 of the edge length n
  for one large zone (a uniform region of 41^3 took 47 s, 55^3 took 411 s; now 0.06 s for 41^3). The zones are the same, so every GLDZM feature is the same.
  A large zone appears with a coarse discretisation, or in a PET. `collectBlobs` also works on an object made with the constructor for sub-classes.

### Checked and not a problem (the items of "Known and not changed" that were examined)
- GLDZM `Ns` that is not reset: `map2matrix` is called once, so it is not reachable. The matrix of a one-voxel region, of one slice and of a uniform region are computed.
- The fractal boxes with a count of 0: not reachable for a region of interest (every box size counts at least one box).
- An array of more than 2^31 voxels: 1290^3 voxels; not a region of interest.

### Known and not changed
- **Fractal (`Fractal_Capacity`)** counts the vertices of the outline polygon of the region (`getFloatPolygon()`), not the pixels of the border that the description of
  the class ("the boxes that cover a one pixel wide border", Smith et al., the ImageJ box counter) says. A straight edge has few vertices, so the count
  is too small for a region with straight edges: a 56 x 56 rectangle on 6 slices gives the counts [3, 2, 2, 1, 1, 1, 1] for the box sizes 2, 3, 4, 6, 8, 12, 16
  (D = 0.56); counting the border pixels gives [324, 144, 104, 36, 24, 16, 12] (D = 1.63). For a disc of radius 30 the counts are 20% lower
  (D 1.52 against 1.59), for an irregular blob 4% lower (D 2.068 against 2.078). The value is not changed, so that analyses made with the earlier versions
  stay comparable; IBSI 1 does not define this feature, so there is no reference value. Use it with this in mind.
- Not examined: the weighting of the GLCM, the `eps` of the entropies, the direction of the ratio of the perimeter and the surface, the memory of a matrix with thousands of grey levels,
  the time of GLAM for large regions.

### Tests
- `NgtdmIbsiTest` (5): the values are computed by a plain implementation of the IBSI definition in the test and by an independent implementation in Python; a normal
  region, a uniform region, two flat blocks, a region with an isolated voxel, a one-voxel region. Four of them fail on 2.7.3 (the normal region does not).
- `GldzmBlobsTest` (4): the zones of six random volumes are the same as those of a copy of the former search and of a plain queue search; the features of three volumes
  are the values 2.7.3 gave; a uniform 41^3 region is found within 30 s (47 s on 2.7.3: the test fails there).

## 2.7.3 (2026-11-19)

The intensity-volume histogram (IVH) features are computed as IBSI 1 (section 3.5) defines them. The IBSI configuration C had one
failure, the intensity at 10% volume (86.25 for a reference of 88.8 +- 0.2); the cause was found in the definition, and the same
mistakes were in the other IVH features. **Only the IVH features change** (`IntensityVolumeHistogram_*`); no other family is touched.

### Fixed
- **Intensity at volume fraction (I10, I90)**: the definition is "the smallest discretised intensity that is in at most x% of the volume", searched
  on the whole grid of intensities, also on the intensities that no voxel has. The code looked at the intensities that exist only, from the top, and
  returned the first one with a volume fraction of x% or more, which is one grid step too low. Configuration C: I10 86.25 -> 88.75 (reference 88.8),
  I90 -423.75 -> -421.25 (reference -421 +- 14); both were exactly one bin (2.5) low. A voxel count that is exactly x% (a tie) takes the bin itself.
- **`+1` on the intensity (discrete case)**: the keys of the histogram had `+1` added ("1-based"). It is right only by chance, for a grid of step 1 and
  a gap of one bin. It was wrong for intensities that are not integers (a CT interpolated without the rounding of the IBSI, 68QD, which is off by default),
  and for a tie. **A non-integer intensity in mode 0 is now rounded half to even to the grid of 1, as the IBSI rounding does: the IVH is the same as
  the IVH of the rounded image.** No other treatment of the data depends on its content.
- **Total range G**: mode 1 (fixed bin width w) uses G = [Xmin + 0.5w, Xmax - 0.5w] (it used the re-segmentation range); mode 2 (fixed bin number Ng) uses
  G = [1, Ng] (it used the re-segmentation range on the bin numbers, so the intensity fractions were meaningless). Mode 0 uses the re-segmentation range;
  a range that is open on one side takes the missing side from the data (it was ignored unless both sides were given). The number of bins of mode 1 is
  computed with decimal arithmetic (the range and the bin width are decimal numbers).
- **Volume at intensity fraction (V10, V90)** and the **area under the IVH curve**: evaluated on the whole grid of G (they used the existing intensities only).
  The area is 0 for a ROI with one discretised intensity (the IBSI definition), and NaN for an empty ROI and for features that need an intensity range
  that does not exist (they were `null`).
- The constructor that takes a settings map reads the bin width and the bin number from it (it read the static values).
- Mode 0 prints a diagnostic to `System.err` when the rounding to the grid of 1 puts every voxel of a ROI into one value although the ROI has different values (an ADC map or a
  PET in SUV, for which mode 0 is not meant). No feature value depends on it. Mode 0 is for calibrated, discrete intensities (CT in HU); use mode 1 or 2 otherwise.

### Checked
- IBSI 1 reports (digital phantom, configurations C and D), recorded with this version: **configuration C has no failure (it was 1)**; configuration D and the
  digital phantom have the same verdict for every feature as 2.7.2. The area under the IVH curve is excluded by default and the manual says it has no reference
  value; the values of the IBSI validation file (IBSI_ValidationFile.xlsx) are reproduced: digital phantom 0.3203 (0.32), configuration C 0.6807 (0.681 +- 0.003),
  configuration D 0.5630 (0.563 +- 0.013) (it was 0.4095 for the phantom). A test keeps these values (`IvhAucReferenceTest`).
- 11 new tests (`IvhDefinitionTest`) compare all 7 features with a reference written from the formulas of the manual (floating point, loops over the grid); 10 of them fail
  on the code of 2.7.2. The digital phantom of the manual (table 3.70), ties, a gap, a ROI with one value, a range open above, a value at the top of the range,
  a range that the bin width does not divide, the fixed bin number, an empty ROI, and a CT without rounding against the rounded CT (88.99999, the halves) are covered.
- Five real CT tumours (public NSCLC Radiogenomics), default settings (not interpolated, integer HU): **no column changes**. The analysis settings of the study
  (1 mm isotropic, bin width 25 HU, IVH mode 0, fractional HU): **6 IVH columns change, no other column**: for example the intensity at 10% volume 56.35 -> 56,
  -678.96 -> -679 (an integer now); the volume fractions change by up to 1e-4 in absolute value (V90 0.00467 -> 0.00472) because the intensity thresholds are on the grid.

### Who is affected
| Input | Change |
|---|---|
| IVH mode 0, integer intensities (CT, not interpolated) | I10, I90, V10, V90: same, except for a tie (a volume fraction of exactly x%); the area changes |
| IVH mode 0, non-integer intensities (interpolated CT without rounding, PET) | all 7 IVH features: those of the intensities rounded to integers. **A PET (SUV) has to use mode 1** (fixed bin width); in mode 0 its grid is 1 SUV |
| IVH mode 1 | I10, I90: one bin up; V90 can change (the range G); the area changes |
| IVH mode 2 | V10, V90 and the area change (the range G is [1, Ng]) |
| A ROI with one intensity, or empty | the area is 0 (one value); NaN for an empty ROI |

### Not changed
- The area under the IVH curve stays in the list of the features that are excluded by default (the IBSI manual has no reference value for it and says it should not be used).
- Rounding of a CT after interpolation (IBSI 68QD) stays off by default.
- `Utils.discreteByBinWidth` computes the bin number of a decimal bin width in binary floating point (a voxel exactly at a bin edge can fall in the neighbouring bin);
  it is used by the texture features too and is recorded for a later version.

## 2.7.2 (2026-11-18)

Fixes for features that gave a wrong value, a NaN or an Infinity without any message for some inputs. They were found when a CT lung
tumour of 279,000 voxels gave a negative GLSZM large zone emphasis and a NaN small zone emphasis, and then by reading all the feature
code for the same kind of mistake (an `int` multiplication that overflows) and for other silent errors. **A value changes only for the
inputs listed under "Who is affected"; every other value is unchanged.** On five real CT tumours (public NSCLC Radiogenomics data,
93,000 to 279,000 voxels, 230 columns, default settings) 19 to 38 columns changed, all of them in the lists below, and none of the
GLCM, GLDZM, NGLDM, NGTDM and fractal columns. The IBSI 1 reports (digital phantom, configurations C and D) have the same counts and
the same verdict for every feature as the reports of 2.7.0. 2.7.1 (the parallel GLRLM) is included.

### Fixed (integer overflow of a square: wrong value, NaN)
- GLSZM `j*j` (zone size), GLRLM and NGLDM and GLDZM `i*i*j*j` (grey level and run length, dependence, distance) and the single squares
  of the same kind were multiplied as `int`. Over 46,340 the square wrapped (a wrong, often negative, value); at 65,536 it was 0 and a
  division 0/0 gave NaN. All of them are `double` now.
- GLSZM: a zone of more than 46,340 voxels gave a wrong `LargeZoneEmphasis` (a 48,000-voxel block: -9.97e8 instead of +1.15e9) and
  a zone of 65,536 voxels or more gave NaN for `SmallZoneEmphasis` and `SmallZoneHighGrayLevelEmphasis`. On the real tumours: large
  zone emphasis -71,386 -> 1,638,167 and -199,405 -> 437,689 (negative values are impossible), 106,136 -> 897,179 (positive, wrong); the
  two NaN became 0.606 and 0.519.
- GLRLM `ShortRunLowGrayLevelEmphasis` was NaN with 256 or more grey levels (the empty cell (256, 256) gave 0/0), and GLDZM
  `SmallDistanceLowGrayLevelEmphasis` was Infinity for a grey level 4096 at a distance of 16 (and the same for every grey level times distance
  that is a multiple of 65,536). Fixed bin widths of 1 HU make such grey levels common.

### Fixed (GLRLM: runs longer than the number of bins were dropped)
- The matrix was always `nBins x nBins`, so a run longer than `nBins` (32 by default) was dropped without a message; when all runs of a
  direction were dropped the normalisation gave 0/0 = NaN (rows of 100 voxels with 32 bins: NaN). The matrix is now as wide as the longest run
  of the direction, never narrower than `nBins`: **when no run is longer than `nBins` the matrix and every value are exactly the same as
  before.** On the real tumours only the one with 13 runs of 33 to 45 voxels (of 1.8 million) changed: 16 GLRLM columns, the largest
  `RunLengthVariance` +3.7e-3 and `LongRunEmphasis` +1.9e-3 (relative).

### Fixed (GLCM: a region on one slice)
- A region on one slice has pairs only in the 4 in-plane angles of the 13; the other 9 were skipped, but the mean was taken over all 13
  entries of the array, so **every GLCM feature was multiplied by about 4/13 = 0.3077** (a uniform region gave an angular second moment of 0.3077
  instead of 1). The mean is over the angles that have a matrix. A region on two or more slices, and `force2D`, are not affected.
  `InformationalMeasureOfCorrelation2` skips an angle whose value is NaN and is now averaged over the others.

### Fixed (skewness and kurtosis)
- They were computed from raw moments, which loses the digits when the mean is large against the standard deviation (mean 30,000, SD 2.5:
  skewness 0.089 instead of 0.0005, excess kurtosis 5,419 instead of 0.0007) and gave -1.1e6 and 7.3e11 instead of NaN for a region of one constant value (100.3). They are computed
  from the deviations from the mean now and are NaN for a constant region. CT and PET values were already correct to 1e-9 and change by less than 1e-10
  (relative). Also `ReciprocalKurtosis` of the auxiliary statistics.

### Fixed (mesh volume and surface area)
- The mesh volume added `float` triple products measured from the origin of the image; for a region far from the origin the large terms cancel and
  the float rounding does not, so the same shape had a different volume at each position (a sphere of 33,360 mm3 gave 33,344 mm3, -0.05%, 215 mm from
  the origin; more farther) and the shape features derived from it (sphericity, compactness, volume densities, integrated intensity) too. It is computed in double relative to the first vertex now (the convex
  hull volume as well). On the real tumours the volume changed by 5e-5 to 2.5e-4 (relative).
- The surface area of the mesh and of the bounding box area densities measured the distance between the cross product and the origin
  of the calibration instead of the length of the cross product: an image whose calibration origin is not 0 gave a wrong surface area (it depended
  on the origin) and so did every feature using it. With origin 0 (the usual case) the area is unchanged
  (1e-8 relative).

### Fixed (intensity histogram gradient)
- The central difference `(h[i+1] - h[i-1]) / 2` of the integer histogram was an integer division (the 0.5 of an odd difference was
  cut off), and the maximum and the minimum started from 0: a histogram whose gradients are all negative gave a maximum of 0 and the bin 0. Changes
  `MaximumHistogramGradient`, `MinimumHistogramGradient` and their intensities (a value of an odd difference gets its 0.5).

### Fixed (size of a resampled image)
- `ceil(n * spacing / new spacing)` came out one voxel too large when the exact result is an integer and the floating point result is a
  few ULP above it (100 pixels of 0.55 mm to 1 mm gave 56, not 55; a half voxel shift of the grid). A result within 4 ULP of an integer is that integer.
  Resampling to a spacing that gives such a size changes slightly.

### Who is affected
| Change | Inputs whose values change |
|---|---|
| Overflow of a square (GLSZM, GLRLM, NGLDM, GLDZM) | a zone, run or distance of more than 46,340, or 256 or more grey levels, or a grey level times a distance or run that is a multiple of 65,536 |
| GLRLM matrix width | a run longer than `nBins` |
| GLCM mean | a region on one slice (3D mode); `InformationalMeasureOfCorrelation2` when an angle gives NaN |
| Skewness, kurtosis | a mean larger than about 400 standard deviations, or a constant region (NaN); otherwise below 1e-10 |
| Mesh volume (and the shape features from it) | all regions, by 5e-5 to 2.5e-4 on CT tumours (more far from the image origin) |
| Surface area | an image whose calibration origin is not 0 |
| Histogram gradient | an odd difference between the neighbours of a bin; an all-negative or all-positive gradient |
| Resampled size | a size that is an exact integer |

### Known and not changed
- Items that need the IBSI definition first: the intensity at volume fraction of the IVH (`intensity + 1` for the raw and the binned intensity),
  NGTDM of a uniform region (Infinity, NaN) and its `Ngp`, the GLDZM `Ns` that is not reset and its slow neighbour search for large regions, the
  fractal boxes with a count of 0, the memory of a matrix with thousands of grey levels, and the time of GLAM for large regions.

### Tests
- 21 new tests compare every fix with a reference computed in the test (a zone, run or distance of 48,000, 67,600 and 200,000 voxels; 300 and
  4,096 grey levels; a run of 100 with 32 bins; a uniform region on one and three slices; mean 30,000 and SD 2.5; a sphere at three positions; the
  origin of the calibration; the histograms [1,1,4,1] and [5,3,1]; the sizes of common spacings) and fail on the code of 2.7.1. The whole suite
  (154 tests) passes.

## 2.7.1 (2026-11-15)

Faster GLRLM. The 13 directions of the grey level run length matrix are now calculated in parallel. No feature value
changes: the matrices are integer run counts, so the result is identical, bit for bit, to the sequential code.

### Changed (performance: GLRLM directions in parallel)

- `GLRLMFeatures.fillGLRLM` calculates the 13 directions in parallel and puts the matrices into the map in the same
  order as before. The shared voxels are only read; each direction keeps its visited voxels in a bit set instead of a
  boxed copy of the whole volume. `GLRLMFeatures.parallel` (default true) selects the original sequential code path
  when set to false. `calcGLRLM(Integer, int[])` is unchanged.
- Checked on two real CT lung tumours (NSCLC Radiogenomics, TCIA; 15,804 and 133,455 voxels; fixed bin width 25; 16
  cores): all 76 columns of the call are bit-identical to RadiomicsJ 2.7.0 (three runs each). The GLRLM family alone,
  as one call of the ROI feature table including the fixed cost of loading the volume, went from 108 s to 23-27 s and
  from 198 s to 50-55 s. The peak resident memory of the JVM was 5% and 13% higher (9.4 to 9.8 GB and 15.8 to 17.9 GB;
  an upper bound that depends on the garbage collector, not a measured heap size).
- A new unit test compares the parallel and the sequential code on random volumes and a small phantom-like volume over
  all 16 GLRLM features, bit for bit, and fails when the directions are mixed up or a direction is dropped. The full
  test suite (133 tests, including the IBSI 1 benchmark report) passes.
- Not changed: the other families (GLSZM, NGLDM, NGTDM and the others) still run one after another on one thread. They
  have the same structure (a pass over the whole volume for each grey level) and are candidates for later work.

## 2.7.0 (2026-10-26)

New feature family `AuxiliaryStatistics` (IBSI-external, off by default) and a re-segmentation range that works with
one bound. Other feature columns are unchanged.

### Changed (re-segmentation range with one bound)

- `DOUBLE_rangeMin` and `DOUBLE_rangeMax` now work alone. Before, the range was applied only when both were set, and a
  range with one bound was ignored without a message (the lower bound alone was already used for the bin edges of the
  fixed bin width discretisation). A missing bound is open; the interval stays closed. Results change only for settings
  that give one bound alone. Settings with both bounds or none (the IBSI benchmark configurations) are unchanged.

### Added (auxiliary statistics family)

- New feature family `AuxiliaryStatistics`, switched by `BOOL_enableAuxiliaryStatistics` (default off). It holds
  statistics that IBSI does not define. First member: `AuxiliaryStatistics_MeanOfPositiveIntensities`, the mean of the
  intensity-mask voxels whose value is greater than 0 (zero is not positive; NaN when there is none). It uses the same
  voxels as the intensity based statistical features (after resampling and re-segmentation, not discretised).
  Existing columns are unchanged whether the family is on or off.
- Four more members: `ProportionOfPositiveIntensities` (voxels > 0 divided by the voxel count),
  `NormalisedStandardDeviation` (ln(SD) / ln(N), SD divided by N; NaN for N below 2 or SD 0),
  `ReciprocalKurtosis` (1 / (excess kurtosis + 3); NaN when the variance is 0) and
  `UniformityOfPositiveIntensities` (sum of squared bin probabilities over the voxels > 0, with the bins of the whole
  ROI as in the intensity histogram features, not bins made again from the positive voxels). An undefined value is
  NaN, never 0. No published reference values exist for these, so they are checked against hand-computed values only.

## 2.6.0 (2026-10-24)

GPU computation (OpenCL, optional) for the Gabor and Simoncelli filters, the global intensity peak and the maximum
3D diameter, with a speed estimate for this machine. It is on by default and falls back to the CPU code when no
device is usable. Verified on one machine (Linux, GTX 1050 Ti); not run on a Windows or macOS GPU, and the Python
`estimate_gpu` is not checked from a pip package.

### Fixed (the command line did not end on a machine with a display)

- `RadiomicsJ.main` showed the result window after saving the result, and the window kept the JVM alive, so a batch run
  with `-o` never returned (it ended at once with `-Djava.awt.headless=true`; also in 2.5.2). With `-o` the result is
  now saved without the window and the command ends with status 0; without `-o` the window is shown as before.

### Fixed (LocalIntensityPeak and GlobalIntensityPeak of a negative region)

- The largest sphere mean was searched from a start value of 0, so a region whose largest mean is negative (for
  example a lung region in HU) returned 0 instead of that negative mean. The search now starts from minus infinity.
  Only such regions change (checked on a synthetic region shifted to about -300: before 0, now -269.09, equal to a
  separate brute-force computation in the test); the IBSI CT (STS_001) gives the same double, bit for bit, on the CPU
  and on the GPU. A region without any voxel of the label still returns 0.

### Added (GPU foundation)

- `BOOL_USE_GPU` (default 1; `--gpu` / `--no-gpu` on the command line) and the package `io.github.tatsunidas.radiomics.gpu`:
  an OpenCL context (JOCL 2.0.5, an optional dependency, kept in the CLI and Python jars) that later changes use for
  GPU code. Nothing computes on the GPU yet, so no result changes. When JOCL, the OpenCL library or a GPU device is
  missing, `Gpu.get()` returns null with one log line and the CPU code runs. On the development machine (only
  `libOpenCL.so.1` installed) the GPU is reported as not usable until `libOpenCL.so` is on `LD_LIBRARY_PATH`.

### Added (Simoncelli on the GPU)

- `ImageFiltering.simoncelliFilter` runs its Fourier transform, band-pass multiplication and inverse transform on the
  GPU in single precision when a device is usable (`Gpu.get()`), else on the CPU as before. One work group transforms
  one line in local memory (radix 2, and Bluestein's chirp-z transform for other lengths, as the CPU code does);
  a padded line longer than 2048 voxels, or beyond the device's work group or local memory limit, stays on the CPU.
  The map is not bit-identical: on the real CT (STS_001, level 1) the largest difference is 2.7e-7 of the response
  range (fp32), and below 1e-6 of the range on the test volumes.
- Measured on the development machine (GTX 1050 Ti vs 16 core CPU), real CT 198x202x59, one pass, 3D, MIRROR
  (padded to 230x234x91): CPU 2.7-3.2 s, GPU 0.18-0.30 s (PERIODIC: CPU 2.7 s, GPU 0.08 s). An earlier run of the same
  CPU case took 5.4-6.0 s with the same code; the cause of that difference is not known.
- `GpuEstimator.Task.SIMONCELLI`, `Problem.simoncelli(...)`; `--gpu-estimate` prints it; Python `estimate_gpu(..., simoncelli_level=1)`.

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
