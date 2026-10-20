# Separable undecimated wavelet filter

`ImageFiltering.waveletFilter(imp, wavelet, combination, level, padding, pooling, planar)`
implements the separable undecimated wavelet transform of IBSI 2 (reference manual
arXiv 2006.05470 v9, sections 4.6.2 and 4.6.3, with the implementation troubleshooting steps).

- The 1D kernels are the decomposition filters of PyWavelets 1.8.0 (`dec_lo` for L,
  `dec_hi` for H), convolved with the image: `haar`, `db2`, `db3`, `db4`, `coif1`,
  `coif2`, `coif3`. The manual's Haar kernels are gL = [1, 1]/sqrt 2 and gH = [-1, 1]/sqrt 2.
  Other names are refused; more of PyWavelets' families can be added to the table.
- A kernel of even length gets one zero appended, to make it odd.
- The kernels of level j are those of level 1 dilated by 2^(j-1) with the a trous
  algorithm: zeros between the taps and the trailing zero kept, the second alternative of
  the manual (PyWavelets, MATLAB). The Haar high-pass of level 2 is [-a, 0, a, 0, 0].
- The response of `LHL` has L along x, H along y and L along z (2 letters with `planar`,
  on each axial slice). Level j filters all axes with the low-pass kernel of each level
  below j, then with the letters of the combination at level j. Each iteration pads its
  input again, so the edge is treated at every level.
- With a `pooling` (MAX, AVERAGE, MIN) every iteration uses the same right angle rotation
  of the kernels (appendix A: 24 in 3D, 4 in 2D) and the responses of the rotations are
  pooled voxel by voxel at the end. `null` uses the kernels as given. The manual advises
  to limit the pooling to level 1; it is available at any level.
- The kernels are in voxels, so the voxel spacing does not matter.
- Paddings are `CONSTANT`, `NEAREST`, `MIRROR`, `PERIODIC`, as in `docs/LoG_filter.md`.
  The input is read through its density calibration; the output is 32 bit float.
- Decimated transforms and non-separable wavelets (Simoncelli) are not covered here.

## In the extraction settings

    STRING_waveletSets=db3:LLH:1;haar:HHH:2
    STRING_waveletPadding=MIRROR
    STRING_waveletPooling=AVERAGE

A set is `name:combination:level`, sets are separated by semicolons. Every set adds the
features of its filtered image to the result row, as
`wavelet_<name>_<combination>_l<level>[_<pooling>]_<family>_<feature>`, for instance
`wavelet_db3_LLH_l1_average_`. With `BOOL_force2D` the combination takes 2 letters. A wrong
number of letters, an unknown name or a level below 1 stops the extraction.
Re-segmentation, the discretisation and the families left out are as for the LoG filter.

## Agreement with IBSI 2

Test set 1: 5.a on the impulse (zero padding, Daubechies 2, LHL, level 1), 6.a on the sphere
(periodic padding, Coiflet 1, HHL, level 1) and 7.a on the checkerboard (mirror padding, Haar,
level 2, LLL and HHH); the `.1` maps without rotations (5.a.1, 6.a.1) and the others pooled by
average over the 24 rotations. A map complies when every voxel is within 1% of the reference's range.

| test | largest difference | of the range |
|---|---|---|
| 5.a.1 | 7.6e-6 | 3e-6 % |
| 5.a.2 | 7.6e-6 | 1e-5 % |
| 6.a.1 | 1.5e-5 | 3e-6 % |
| 6.a.2 | 1.1e-5 | 7e-6 % |
| 7.a.1 | 2.4e-4 | 1e-5 % |
| 7.a.2 | 1.5e-5 | 6e-6 % |

Another letter order, another wavelet, no or another pooling, another padding (on the
checkerboard; the sphere is clear of the edge), another level and a planar instead of a
volumetric filter all miss the reference by more than 1% (`TestWaveletFilter`). The
kernels are also checked to be orthonormal wavelets (low-pass sum sqrt 2, high-pass sum 0,
unit energy) and average pooling to be equivariant to quarter turns of the image, which the
reference does not test. The CT phantom features of configuration B (6.B, 7.B) are in
`docs/IBSI2_reference_features.md`.
