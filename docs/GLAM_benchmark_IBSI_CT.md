# GLAM benchmark : IBSI CT radiomics phantom

A published reference result for the GLAM feature family, so that another
implementation can be checked against RadiomicsJ, and so that a change inside
RadiomicsJ that moves these numbers is noticed.

GLAM itself has no benchmark. The paper that introduces it validates on clinical
glioma cohorts through a concordance index, not through per feature reference
values, and IBSI does not cover GLAM because it is not an IBSI feature. This
document is therefore a reference *result*, not a reference *standard*: it says
what RadiomicsJ computes, under conditions fixed precisely enough to reproduce.

What gives it weight is the preprocessing. Every step before GLAM is IBSI
configuration D, which RadiomicsJ passes against the published IBSI reference
values, and the diagnostics below reproduce the IBSI voxel counts exactly. So
the input to GLAM is a quantity the community has already agreed on.

RadiomicsJ 2.3.0.

---

## 1. The data

The **IBSI CT radiomics phantom**: one lung cancer CT with a delineated gross
tumour volume, published by the Image Biomarker Standardisation Initiative as
the reference case for their feature validation.

| | |
|---|---|
| source | IBSI data sets, `ibsi_1_ct_radiomics_phantom` |
| repository | <https://github.com/theibsi/data_sets> |
| IBSI reference manual | <https://arxiv.org/abs/1612.07003> |
| format used here | NIfTI, `nifti/image/phantom.nii.gz` and `nifti/mask/mask.nii.gz` |
| dimensions | 204 x 201 x 60 voxels |
| voxel spacing | 0.977 x 0.977 x 3.0 mm |
| intensity range | -1000 to 3065 HU |
| roi label | 1 |
| roi voxels | 125256 |

**The image is not redistributed with RadiomicsJ.** Download it from the
repository above. To verify you have the same files:

```
sha256  909129fdf99d5f5fd17bc568b1834d31edce8663ea6746c6020720bfc1d7bbcb  phantom.nii.gz
sha256  0e9243e473b1255c60e6460df465d0b07b65436d6e0c691fe3d17bca0594ae78  mask.nii.gz
```

During development the files live under
`src/test/resources/data_sets-master/ibsi_1_ct_radiomics_phantom/`, which is
outside the distributed jar. Everything that reads them is a debug only tool.

---

## 2. Conditions

### 2.1 Preprocessing, IBSI configuration D

| setting | value |
|---|---|
| `DOUBLEARRAY_resamplingFactorXYZ` | `2.0,2.0,2.0` (2 mm isotropic) |
| `INT_interpolation3D` | 100, trilinear |
| `INT_interpolation_mask3D` | 100, trilinear |
| `DOUBLE_Mask_PartialVolumeThareshold` | 0.5 |
| `BOOL_interpolation_intensity_rounding` | 1, IBSI table 5.1 for calibrated discrete intensities |
| `BOOL_removeOutliers` | 1 |
| `DOUBLE_zScore` | 3 |
| `BOOL_USE_FixedBinNumber` | 1 |
| `INT_binCount` | 32 |
| `BOOL_force2D` | 0 |
| `INT_label` | 1 |

### 2.2 GLAM

| setting | value | why |
|---|---|---|
| `BOOL_enableGLAM` | 1 | |
| `INT_GLAM_maxRadius` | 50 | 50 voxels of 2 mm is 100 mm, the reach the paper uses |
| `BOOL_GLAM_boundaryCorrection` | 1 | normalise each shell by the part inside the roi, as the paper states |
| `INT_GLAM_maxReferenceVoxels` | 0 | every roi voxel is a centre, no sub sampling, exact |
| `INT_GLAM_numRandomisations` | 0 | closed form randomised state, exact and free of sampling noise |
| `INT_GLAM_savitzkyGolayWindow` | 7 | |
| `INT_GLAM_savitzkyGolayPolynomial` | 3 | |
| `DOUBLE_GLAM_peakProminence` | 4 | |
| `INT_GLAM_maxLocalShellRadius` | 30 | |

Nothing here is stochastic, so the result is bit for bit reproducible.

### 2.3 What the preprocessing produced

These are the numbers to check first if a reproduction disagrees. The two ROI
counts are the same ones RadiomicsJ matches against the IBSI reference values.

| diagnostic | value |
|---|---|
| interpolated image dimensions | 100 x 99 x 90 |
| interpolated voxel size | 2 x 2 x 2 mm |
| interpolated roi voxel count | **45985** |
| re-segmented roi voxel count | **44465** |
| interpolated roi bounding box | 49 x 49 x 40 |
| gray levels after discretisation | 32 |
| gray levels actually occupied | **30 of 32** (25 and 29 are empty) |

Two of the 32 bins contain no voxel at all. Their row and column are
**undefined**, not zero: a gray level that does not occur has no reference voxel
to measure from and a density of zero to divide by. RadiomicsJ reports those
cells as NaN and the reduction statistics skip them, so every value below is the
statistic over the 30 occupied levels.

---

## 3. Results

Extraction took 7.7 s on 8 cores.

| matrix | Mean | Variance | Skewness | Kurtosis | Minimum | Maximum | DiagonalMean | OffDiagonalMean |
|---|---|---|---|---|---|---|---|---|
| **RDFPeakPosition** | 23.7133 | 454.853 | 0.0740817 | -1.84996 | 1 | 50 | 1.06667 | 24.4943 |
| **RDFDispersionRatio** | 8.26312 | 2816.44 | 9.80501 | 109.344 | 0.0200375 | 766.403 | 14.1336 | 8.08523 |
| **LogRDFPeakHeight** | 1.80668 | 1.44321 | 2.79307 | 9.17928 | 0 | 8.1378 | 2.62998 | 1.77829 |
| **LogRDFMedian** | 0.788672 | 0.477323 | 6.12712 | 38.1279 | 0.212624 | 6.17195 | 0.92842 | 0.784438 |
| **LogRDFVariance** | 0.214412 | 0.120358 | 3.55281 | 18.2895 | 0.00502621 | 3.25857 | 0.257082 | 0.213118 |
| **LogRDFSkewness** | 0.57998 | 0.729982 | -0.147022 | 0.00114091 | -2.51591 | 2.65535 | 1.54693 | 0.550678 |
| **LogRDFKurtosis** | 0.516764 | 3.89646 | 1.59859 | 2.43536 | -1.69094 | 8.51066 | 2.84998 | 0.446061 |
| **SecondVirialCoefficient** | -1.8464e+05 | 1.5407e+11 | -2.0284 | 4.23705 | -2.7513e+06 | 2.5364e+05 | -9317.63 | -1.9069e+05 |
| **PotentialEnergy** | -1.0314e+06 | 4.3698e+12 | -2.61494 | 8.61881 | -1.7316e+07 | 1.3285e+05 | -3.8567e+05 | -1.0537e+06 |
| **Compressibility** | 1.4049e+06 | 2.9962e+13 | 3.71574 | 12.3961 | -3.0205e+05 | 2.5801e+07 | — | — |
| **CoordinationNumber** | 6167.03 | 5.3985e+08 | 4.86054 | 24.3608 | 0.00828512 | 1.6789e+05 | 3696.08 | 6243.24 |
| **InverseCorrelationLength** | 0.121953 | 0.0244478 | 1.22403 | 1.12958 | 0 | 0.756293 | 0.335262 | 0.114597 |
| **StructuralPressureIndex** | -131.228 | 8.2188e+05 | -12.4024 | 176.597 | -14821.9 | 4327.92 | -830.182 | -107.126 |
| **ConfigurationalDisorderIndex** | 0.994953 | 0.00571064 | -1.00683 | 12.5494 | 0.351271 | 1.42023 | 0.961004 | 0.995982 |
| **WassersteinDistance** | 32149.5 | 5.8694e+09 | 3.96462 | 16.5766 | 0 | 5.7932e+05 | 40517 | 31860.9 |
| **AssemblyCoupling** | 5.06973 | 1.7618e+08 | -1.69473 | 58.6756 | -1.5598e+05 | 1.2356e+05 | 521.497 | -15.5874 |
| **PhenotypicDistance** | 10.2756 | 121.113 | 1.70081 | 2.10878 | 0 | 48.5 | 0 | 10.6299 |
| **LocalPackingFraction** | 0.0320039 | 0.00648698 | 3.57258 | 12.3122 | 5.3115e-07 | 0.589367 | 0.0345209 | 0.0319263 |
| **FrustrationIndex** | -133.07 | 8.4001e+05 | -12.2353 | 170.918 | -14074.8 | 4298.67 | -1019.41 | -106.212 |

(150 features)


### Files

| file | contents |
|---|---|
| [`benchmark/glam_ibsi_ct_features.csv`](benchmark/glam_ibsi_ct_features.csv) | the 150 GLAM features at full double precision, plus the diagnostics |
| [`benchmark/matrix_*.csv`](benchmark) | the 19 affinity matrices, 32 x 32, `NaN` for the empty gray levels |
| [`benchmark/radial_distribution_function.csv`](benchmark/radial_distribution_function.csv) | g(alpha, beta, r) itself, r from 1 to 50 |

The matrices and the radial distribution function are published as well as the
features, because they let a disagreement be located. If g(r) already differs,
the problem is in the pair counting or the normalisation; if g(r) agrees and a
matrix does not, the problem is in that matrix's formula; if both agree and a
feature does not, the problem is in the reduction statistic.

---

## 4. Reproducing it

```bash
git clone https://github.com/tatsunidas/RadiomicsJ.git
cd RadiomicsJ
# place the IBSI files under
#   src/test/resources/data_sets-master/ibsi_1_ct_radiomics_phantom/nifti/
mvn test-compile exec:java -Dexec.mainClass=radiomics.GLAMBenchmark -Dexec.classpathScope=test
```

The generator is `src/test/java/radiomics/GLAMBenchmark.java`. It holds every
setting listed above, runs the ordinary RadiomicsJ pipeline, and writes
`docs/benchmark/`. The matrices are taken from the same preprocessed image the
features came from, so the two cannot drift apart.

From another language, through the python wrapper:

```python
import numpy as np, radiomicsj

# image and mask already resampled to 2 mm isotropic and re-segmented
glam = radiomicsj.GLAM(image, mask, spacing=(2.0, 2.0, 2.0), label=1,
                       n_bins=32, max_radius=50,
                       boundary_correction=True,
                       max_reference_voxels=0, num_randomisations=0)
features = glam.get_all_features()
```

---

## 5. Reading the numbers

A few of the rows are worth a comment, because a plausible looking value is not
always a meaningful one.

- **`SecondVirialCoefficient`** spans -2.75e6 to 2.54e5. The scale comes from the
  r^2 weight integrated out to 50 voxels, so it is not comparable across
  different `INT_GLAM_maxRadius`. The *sign* is what carries meaning: negative is
  net attraction between two gray levels, positive is net repulsion.
- **`PhenotypicDistance` DiagonalMean is exactly 0** and the matrix is symmetric.
  That is the definition, not a coincidence, and it is a cheap check that an
  implementation is correct.
- **`ConfigurationalDisorderIndex` sits at 0.995 with a variance of 0.0057**, i.e.
  it is flat. That is expected here and is not a defect of this data set: the
  index divides the observed ordering by how far the observed and the randomised
  state differ, and boundary correction pins the randomised state to one, which
  collapses the ratio. Use `BOOL_GLAM_boundaryCorrection=0` when this matrix, or
  `FrustrationIndex` which divides by it, matters. See the discussion in
  [GLAM_note_ja.md](GLAM_note_ja.md).
- **`RDFPeakPosition` Maximum is 50**, the largest radius evaluated. For the
  sparsest gray levels the curve has no interior peak, so the maximum lands on
  the edge of the search range. Raising `INT_GLAM_maxRadius` will move it.

---

## 6. Scope

This benchmark covers the statistical mechanics classes of the GLAM framework,
which is what RadiomicsJ 2.3.0 implements: 19 affinity matrices and the 150
statistics over them. The geometric and topological classes of the original
framework (fractal dimension, lacunarity, Betti numbers, percolation,
granulometry, nematic order) are not part of it.

Separately from this benchmark, the implementation is validated element by
element against the reference python implementation released with the paper, on
five digital phantoms in two normalisation modes. See
`src/test/java/radiomics/TestGLAMFeatures.java`.

---

*Apache License 2.0. Implemented with the coding assistant Claude (Anthropic).*
