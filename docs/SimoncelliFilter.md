# Simoncelli wavelet filter

`ImageFiltering.simoncelliFilter(imp, level, padding, planar)` (and a variant that sets the
margin) implements the non-separable Simoncelli wavelet of IBSI 2 (reference manual
arXiv 2006.05470 v9, section 4.6.4, equation 4.8). Each level gives one response map, the
"B map"; the wavelet is circularly symmetric, so there is no rotation to pool.

- The B map of a level is the image times the band-pass function in the Fourier domain,
  `g[nu] = cos(pi/2 * log2(2|nu| / nuB))` for `nuB/4 <= |nu| <= nuB` and 0 elsewhere, with
  `nuB = pi / 2^(level-1)` rad/sample; the response is the real part of the inverse transform.
- `|nu|` is the norm of the angular frequencies of the axes, in rad/sample. The voxel spacing is
  not used, so an anisotropic grid gives an anisotropic filter.
- The frequency of an axis of M samples runs over `linspace(-pi, pi, M)` and is placed with the
  zero frequency at M/2 (numpy's `ifftshift`), so the zero frequency sits half a sample off the
  grid for an even M. The standard grid `2 pi k / M` does not reproduce the reference maps; see below.
- `PERIODIC` padding filters the image itself, which is the Hadamard product of the manual. For
  `CONSTANT`, `NEAREST` or `MIRROR` the image is first extended on each axis by a margin of
  `min(N/2, 2^(level+3))` voxels (two periods of the lowest frequency of the band), filtered and
  cropped. This choice is not in the manual. The response inside the image depends on it slightly:
  doubling the margin changes the response more than half a margin from the edge by 0.2 to 2 % of
  its range on a test image with a strong ramp (`TestSimoncelliFilter`); at the edge it changes it
  by up to 6 %. With `PERIODIC` there is no such choice.
- `planar` (`BOOL_force2D`) filters every axial slice in 2D.
- The input is read through its density calibration; the output is 32 bit float.
- The Shannon wavelet (equation 4.7) and the Riesz transform are not covered.

## In the extraction settings

    INTARRAY_simoncelliLevels=1,2
    STRING_simoncelliPadding=MIRROR

Every level adds the features of its B map to the result row, as
`simoncelli_l<level>_<family>_<feature>`. A level below 1 stops the extraction. Re-segmentation,
the discretisation and the families left out are as for the LoG filter.

## Agreement with IBSI 2

Test 8.a on the checkerboard (periodic padding), B maps of levels 1, 2 and 3. A map complies when
every voxel is within 1% of the reference's range.

| test | largest difference | of the range |
|---|---|---|
| 8.a.1 (level 1) | 1.1 | 0.38 % |
| 8.a.2 (level 2) | 1.5e-5 | 5e-6 % |
| 8.a.3 (level 3) | 3.8e-6 | 3e-6 % |

The frequency grid was chosen on these maps. Other grids, each tried on the same three maps (the
last column is 8.a.3):

| grid | 8.a.1 | 8.a.2 | 8.a.3 |
|---|---|---|---|
| `linspace(-pi, pi, M)`, zero frequency at M/2 (used) | 0.38 % | 5e-6 % | 3e-6 % |
| `2 pi k / M` (standard FFT frequencies) | 0.95 % | 2.5 % | 8.0 % |
| `2 pi k / (M-1)` | 0.5 % | 1.0 % | 1.8 % |

In the Fourier domain the used grid reproduces the level 2 and 3 maps to 1e-7. **Level 1 is not
reproduced exactly and the cause is unknown.** The residual is a smooth deviation of the gain of
up to 0.01 over 1.2 < |nu| < pi; scaling `nuB` of level 1 by 1.004 lowers the difference to 0.07 %,
but not to zero, so it is not a scale. It is 0.38 % of the range, inside the 1% criterion, and the test
pins it (between 0.1 % and 0.5 %) so that a change shows. It was not tuned away.

A wrong level, a slice-wise instead of a volumetric filter and another padding (on the
checkerboard, which has content at its edge) all miss the reference by more than 1%. Exchanging two
axes of a cube is checked to give the exchanged response, the transform is checked against a direct
DFT for lengths that are not powers of two, and a constant image has no response; none of this
is tested by the reference.

The CT phantom features of configuration B (8.B, 9.B) are in `docs/IBSI2_reference_features.md`.
