# Mean filter

`ImageFiltering.meanFilter(imp, support, padding, planar)` implements the mean
filter of IBSI 2 (reference manual arXiv 2006.05470 v9, section 4.2).

- Every voxel becomes the mean over a cube of `support` (M) voxels centred on it, a
  square on its axial slice when `planar` is true. M is odd.
- Voxels the cube reaches outside the image take the value of the padding
  (`CONSTANT` is 0, `NEAREST`, `MIRROR`, `PERIODIC`), as in `docs/LoG_filter.md`.
- M is in voxels, so the voxel spacing does not matter and an anisotropic grid is
  accepted.
- The input is read through its density calibration. The output is 32 bit float on
  the input grid, with no density calibration.

## In the extraction settings

    INTARRAY_meanSupport=3,5
    STRING_meanPadding=MIRROR

Every support adds the features of its filtered image to the result row, as
`mean_M<support>_<family>_<feature>`. With `BOOL_force2D` the filter is planar. The
re-segmentation, the discretisation and the families left out of a filtered image
are as for the LoG filter (`docs/LoG_filter.md`).

## Agreement with IBSI 2

Test set 1 (tests 1.a.1 to 1.a.4 on the checkerboard, M = 15, 3D, and 1.b.1 on the
impulse, 2D), against the consensus response maps. A map complies when every voxel
is within 1% of the reference's range.

| test | padding | dimension | largest difference | of the range |
|---|---|---|---|---|
| 1.a.1 | constant | 3D | 7.6e-6 | 6e-6 % |
| 1.a.2 | nearest | 3D | 1.5e-5 | 6e-6 % |
| 1.a.3 | periodic | 3D | 0 | 0 % |
| 1.a.4 | mirror | 3D | 1.5e-5 | 6e-6 % |
| 1.b.1 | constant | 2D | 4e-17 | 4e-15 % |

Each map is far from the other paddings' maps and from a support of 13 or a planar
filter, so the test set separates them. `TestMeanFilter` runs all of this.
