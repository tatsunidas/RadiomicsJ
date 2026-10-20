# Gabor filter

`ImageFiltering.gaborFilter(imp, sigmaMm, lambdaMm, gamma, theta, padding, pooling, dTheta, planar)`
implements the Gabor filter of IBSI 2 (reference manual arXiv 2006.05470 v9, section 4.5):

    g[k] = exp(-(k1'^2 + gamma^2 k2'^2) / (2 sigma^2) + j 2 pi k1' / lambda)
    (k1', k2') = (cos t k1 + sin t k2, sin t k1 - cos t k2)

- The response map is the modulus of the 2D convolution of an image plane with g, k1
  along the columns and k2 along the rows. Phase, real and imaginary parts are not offered.
- `sigmaMm` and `lambdaMm` are in mm and are evaluated at the physical position of a voxel,
  that is sigma / s voxels on an axis of spacing s. `theta` is in radians.
- The kernel is cut to the image along each axis (an odd width, N or N + 1; the manual
  does not say what N is, the image size gives the reference maps) and where the envelope
  has fallen below 1e-7 of its peak (6 sigma along the long axis of the ellipse), which
  keeps the kernel small. The convolution is by FFT.
- With a `pooling` (MAX, AVERAGE, MIN) the orientations `theta`, `theta + dTheta`, ... below
  `theta + pi` are pooled voxel by voxel. A half turn gives the same modulus, so it is enough;
  pi must be a whole number of steps. `null` uses `theta` alone.
- A volume is filtered in its three orthogonal planes (xy, xz, yz) and the three response
  maps are averaged, as the manual proposes for 3D images. With `planar` (`BOOL_force2D`)
  only the axial plane is filtered, so a Gabor filter takes the axial plane alone where
  Laws and the mean filter take the 2D variant of the same kernel.
- Paddings are `CONSTANT`, `NEAREST`, `MIRROR`, `PERIODIC`, as in `docs/LoG_filter.md`.

## In the extraction settings

    STRING_gaborSets=5:2:1.5:0;3:4:1:1.5708
    STRING_gaborPadding=MIRROR
    STRING_gaborPooling=AVERAGE
    DOUBLE_gaborDeltaTheta=0.39269908169872414

A set is `sigma(mm):lambda(mm):gamma:theta(rad)`, sets are separated by semicolons.
Every set adds the features of its filtered image to the result row, as
`gabor_s<sigma>_l<lambda>_g<gamma>_t<theta>[_<pooling>_dt<step>]_<family>_<feature>`, the
numbers to 4 significant digits, for instance `gabor_s5_l2_g1.5_t0_average_dt0.3927_`.
A malformed set, a non positive sigma, lambda or gamma, or a step that does not divide pi
stops the extraction. Re-segmentation, the discretisation and the families left out are as
for the LoG filter.

## Agreement with IBSI 2

Test set 1: 4.a on the impulse (zero padding, sigma 10 mm, lambda 4 mm, gamma 1/2) and
4.b on the sphere (mirror padding, sigma 20 mm, lambda 8 mm, gamma 5/2), voxels of 2 mm; the
`.1` maps at one orientation (pi/3 and 5 pi/4), the `.2` maps pooled by average over steps of
pi/4 and pi/8 and averaged over the orthogonal planes. A map complies when every voxel is
within 1% of the reference's range.

| test | largest difference | of the range |
|---|---|---|
| 4.a.1 | 9.5e-7 | 4e-7 % |
| 4.a.2 | 1.5e-5 | 6e-6 % |
| 4.b.1 | 0.29 | 0.011 % |
| 4.b.2 | 1.28 | 0.11 % |

The 4.b maps are about four orders of magnitude further from the reference than the others.
The cause is not known: it does not depend on the kernel width (the same with a kernel twice
as wide), and an independent NumPy implementation gives the same numbers, so it is a property
of the reference data, not of this code. It is well within the 1% criterion.

The opposite sign of the orientation (42 % and 97 %), the axial plane alone for 4.b.2
(110 %), another padding, another pooling or step, and sigma and lambda taken as voxels all
miss the reference by more than 1% (`TestGaborFilter`). A quarter turn of the image is also
checked to be a quarter turn of the orientation, which the reference does not test. The CT
phantom features of configuration B (5.B) are in `docs/IBSI2_reference_features.md`.
