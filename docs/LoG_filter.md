# Laplacian of Gaussian filter

`ImageFiltering.laplacianOfGaussian(imp, sigmaMm, truncate, padding, planar)`
implements the Laplacian of Gaussian (LoG) filter of IBSI 2, the Image Biomarker
Standardisation Initiative's reference manual for convolutional filters
(arXiv 2006.05470 v9, 2024).

## Definition

The kernel is equation 4.2 of the manual:

    g[k] = -(1 / s^2) (1 / (sqrt(2 pi) s))^D (D - |k|^2 / s^2) exp(-|k|^2 / (2 s^2))

- `s` is the scale in voxels: `sigmaMm` divided by the voxel spacing.
- `D` is 2 for a planar filter (every axial slice on its own) and 3 for a
  volumetric one.
- A bright blob gives a **negative** response at its centre.
- The kernel is not multiplied by `s^2` (no scale normalisation) and is not
  corrected to sum to zero, as in the manual.
- The kernel is cut off at a half width of `floor(truncate * s + 0.5)` voxels,
  so it is `1 + 2 floor(truncate * s + 0.5)` voxels wide (manual, section 4.3).
- It is computed as a sum of separable one dimensional convolutions
  (`Gxx Gy Gz + Gx Gyy Gz + Gx Gy Gzz`). That is the same kernel, cut off to the
  same cube.
- The input is read through its density calibration (for instance in HU).
- The output is 32 bit float on the input grid, with no density calibration.

## Arguments

None of them has a default inside the function. The caller always passes all of
them.

| argument | meaning |
|---|---|
| `sigmaMm` | scale of the Gaussian, in the unit of the calibration (mm) |
| `truncate` | half width of the kernel as a multiple of sigma. The manual uses 4 in every test set. |
| `padding` | `CONSTANT` (zero), `NEAREST`, `MIRROR` (the edge voxel included: a b c \| c b a), `PERIODIC`. Manual section 2.2. It matters only within the half width of the edge. |
| `planar` | `true` filters each axial slice on its own, `false` filters the volume |

## Isotropic grids only

The response is in units per voxel squared, as in the manual. The manual assumes
the image has been interpolated to isotropic voxels before it is filtered. The
filter therefore refuses a grid whose spacings differ by more than one part in a
million:

- for a volumetric filter, in any of the three axes;
- for a planar filter, between the two in-plane axes. The slice spacing does not
  matter there.

Resample the image to isotropic voxels first.

## In the extraction settings

    DOUBLEARRAY_logSigmaMm=1.5,3.0
    DOUBLE_logTruncate=4
    STRING_logPadding=MIRROR

Every scale adds the features of its filtered image to the result row, as
`log_sigma<scale>mm_<family>_<feature>` (for example
`log_sigma3.0mm_IntensityHistogram_...`). With `BOOL_force2D` the filter is planar.

- The filter takes the resampled image, so it must be isotropic by then
  (`DOUBLEARRAY_resamplingFactorXYZ`). If it is not, the extraction stops with
  that advice.
- The range filter, outlier removal and normalisation belong to the intensities
  of the original, so the filtered images use the original's re-segmented mask.
- The filtered image is discretised again with the same bin settings. A fixed bin
  width is in the units of the original image and does not suit a filter
  response, so use a fixed bin number.
- Morphology, Shape2D, diagnostics and operational information are not repeated.
- There are no IBSI 2 reference values for the features of a filtered image, only
  for the response maps. `TestLoGExtraction` checks that every column equals the
  plain extraction of the filtered image.

## Agreement with IBSI 2

Test set 1 of the manual (table 6.1), against the consensus response maps of
https://github.com/theibsi/ibsi_2_reference_data. A map complies when every voxel
is within 1% of the reference's intensity range.

| test | phantom | sigma | padding | dimension | largest difference | of the range |
|---|---|---|---|---|---|---|
| 2.a | impulse | 3.0 mm | constant | 3D | 9.1e-5 | 0.0014% |
| 2.b | checkerboard | 5.0 mm | mirror | 3D | 0.017 | 0.033% |
| 2.c | checkerboard | 5.0 mm | mirror | 2D | 0.011 | 0.024% |

Filtering 2.b with constant padding instead puts the edge 35% of the range away
from the reference, so the padding is what the test set exercises there.
`TestLoGFilter` runs all of this.

## Speed

Measured on a 512 x 512 x 300 volume of 0.7 mm voxels, with 8 cores and
truncate 4:

| sigma | 3D | 2D |
|---|---|---|
| 1 mm | 14 s | 11 s |
| 3 mm | 26 s | 14 s |
| 6 mm | 48 s | 33 s |

The filter holds about seven float copies of the volume at its peak, about 2 GB
here. For the features of a region of interest, filter only the bounding box of
the roi, grown by the half width of the kernel. Inside the roi, that gives the
same values bit for bit as filtering the whole image.

`radiomics.LoGBenchmark` in the tests repeats the measurement.
