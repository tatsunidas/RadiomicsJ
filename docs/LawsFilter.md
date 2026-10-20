# Laws kernel filter

`ImageFiltering.lawsFilter(imp, kernels, padding, pooling, energyDelta, planar)`
implements the Laws kernels of IBSI 2 (reference manual arXiv 2006.05470 v9,
section 4.4).

- The 1D kernels are normalised as in the manual: L3, L5 (level), E3, E5 (edge),
  S3, S5 (spot), W5 (wave), R5 (ripple).
- The response of `E5L5S5` is the image convolved with E5 along x, L5 along y and S5
  along z, in that order (2 names with `planar`, on each axial slice). The kernels E5,
  W5 are not symmetric, so the order and the convolution direction matter.
- With a `pooling` (MAX, AVERAGE, MIN) the responses of all right angle rotations of
  the kernel are pooled voxel by voxel (appendix A: 24 rotations in 3D, 4 in 2D),
  which makes the filter pseudo rotation invariant. `null` keeps the kernel as given.
- With `energyDelta` of 1 or more, the absolute response (after the pooling) is
  averaged over a cube of 2 delta + 1 voxels, with the same padding. 0 gives none.
- The kernels and delta are in voxels, so the voxel spacing does not matter.
- Paddings are `CONSTANT`, `NEAREST`, `MIRROR`, `PERIODIC`, as in `docs/LoG_filter.md`.
- The input is read through its density calibration. The output is 32 bit float on the
  input grid, with no density calibration.

## In the extraction settings

    STRING_lawsKernels=L5E5E5,E5L5S5
    STRING_lawsPadding=MIRROR
    STRING_lawsPooling=MAX
    INT_lawsEnergyDelta=7

Every kernel set adds the features of its filtered image to the result row, as
`laws_<kernel>[_<pooling>][_d<delta>]_<family>_<feature>`, for instance
`laws_L5E5E5_max_d7_`. With `BOOL_force2D` the filter is planar and takes 2 names. A
wrong number of names or an unknown kernel stops the extraction. Re-segmentation, the
discretisation and the families left out are as for the LoG filter.

## Agreement with IBSI 2

Test set 1 (3.a on the impulse, zero padding, 3D E5L5S5; 3.b on the checkerboard, mirror
padding, 3D E3W5R5; 3.c on the checkerboard, mirror padding, 2D L5S5), each as the plain
response, with max pooling over the rotations, and with the energy map of delta 7. A map
complies when every voxel is within 1% of the reference's range.

| test | largest difference | of the range |
|---|---|---|
| 3.a.1 | 3.8e-6 | 2e-6 % |
| 3.a.2 | 7.6e-6 | 8e-6 % |
| 3.a.3 | 1.2e-7 | 1e-5 % |
| 3.b.1 | 2.1e-6 | 1e-6 % |
| 3.b.2 | 7.6e-6 | 9e-6 % |
| 3.b.3 | 4.8e-7 | 6e-6 % |
| 3.c.1 | 1.5e-5 | 4e-6 % |
| 3.c.2 | 1.5e-5 | 4e-6 % |
| 3.c.3 | 7.6e-6 | 1e-5 % |

The other kernel orders, no or another pooling, a missing energy map, another padding
and a volumetric instead of a planar filter all miss the reference by more than 1%
(`TestLawsFilter`). Max pooling is also checked to be equivariant to quarter turns of
the image about two axes, which the reference does not test. The CT phantom features of
configuration B (4.B) are in `docs/IBSI2_reference_features.md`.
