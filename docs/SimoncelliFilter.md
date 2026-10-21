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
  **The margin is a limit of this filter, not a setting that converges.** Measured once in 2D
  (one 64 x 64 slice of the checkerboard, and a 64 x 64 ramp with noise; levels 1 to 3; `MIRROR` and
  `CONSTANT`), against the response with a margin of 256 voxels, as a fraction of the range:
  with margins of 8, 16, 32, 64 and 128 voxels the largest difference falls slowly, by a third to a half
  per doubling of the margin, for example 1.4, 1.1, 0.75, 0.43 and 0.17 % for the checkerboard at level 1 with
  `MIRROR`. The response with a margin of 256 is itself not converged (128 against 256 still differs
  by 0.15 to 0.4 %), so a margin that gives a response within, say, 0.1 % would be several hundred
  voxels per side, which a 3D volume cannot afford (a margin of 200 on 64 voxels is 464 voxels on an
  axis). The default (16 voxels at level 1, 32 at levels 2 and 3 for a 64 voxel axis) differs from
  the margin of 256 by 0.9 to 2.1 % at the edge and 0.5 to 1.7 % at 16 voxels from it in these
  images; with a margin of 8 voxels, a ramp at level 3 differs by 66 % at the edge. The default was
  kept. For a result that does not depend on this choice use `PERIODIC` padding, or a margin that is
  large against the image. The 3D case was not measured.
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
reproduced exactly and the cause is unknown.** It is 0.38 % of the range, inside the 1% criterion, and
the test pins it (between 0.1 % and 0.5 %) so that a change shows. It was not tuned away.

What was measured, once, and where it stopped (a NumPy replica of the filter gives the same 0.3784 %):

- The reference level 1 map divided by the checkerboard in the Fourier domain gives the gain the
  reference used. It differs from the formula by -0.0035 at 1.3 < |nu| < 1.6, then by +0.003 to +0.010
  up to |nu| = pi, and by up to 0.009 just past pi, where the formula is 0. That is what a band
  that reaches slightly beyond pi would give (compare the scaling of `nuB` below).
- Scaling `nuB` of level 1 from pi to pi * 1.004 lowers the largest difference from 0.378 % to
  0.070 % of the range (best of a scan of 1.000 to 1.012 in steps of 0.001; 1.005 gives 0.087 %, 1.003
  gives 0.119 %), and it does not reach 0. This scales the band edges and the argument of the
  cosine together. Moving only the upper edge, or only the lower edge, by that factor leaves the
  0.38 % as it is (the argument alone was not tried).
- Levels 2 and 3 need no such factor, so it is not a property of the grid. No rule that gives a
  factor of 1.004 for level 1 and 1 for the others was found, and the reference code is not available
  here. The factor is therefore not used.

A wrong level, a slice-wise instead of a volumetric filter and another padding (on the
checkerboard, which has content at its edge) all miss the reference by more than 1%. Exchanging two
axes of a cube is checked to give the exchanged response, the transform is checked against a direct
DFT for lengths that are not powers of two, and a constant image has no response; none of this
is tested by the reference.

The CT phantom features of configuration B (8.B, 9.B) are in `docs/IBSI2_reference_features.md`.
