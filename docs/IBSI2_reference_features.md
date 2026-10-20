# IBSI 2 reference feature values, configuration B

`TestFilteredFeatureReference` runs the 3D configuration B of the manual (Table 6.2)
on the IBSI 2 lung cancer CT phantom: tricubic spline to 1 x 1 x 1 mm, intensities
rounded, trilinear mask with partial volume 0.5, re-segmentation to [-1000, 400] HU,
mirror padding. It compares the 18 intensity based statistical features with
`reference_values.csv` of `theibsi/ibsi_2_reference_data`.

| id | filter | within tolerance |
|---|---|---|
| 1.B | none | 18 of 18 |
| 2.B | mean, M = 5 | 18 of 18 |
| 3.B | LoG, sigma 1.5 mm | 18 of 18 |
| 4.B | Laws L5E5E5, 3D rotation invariance (max), energy map delta 7 | 18 of 18 |

Configuration A (a 2D filter, the features over the whole roi) is not tested:
`BOOL_force2D` takes the features slice by slice.

## History

Until 2.5.0 the unfiltered baseline 1.B missed its reference (7 of 18 features, for
instance a minimum of -925 against -997). The cause was the tricubic spline
interpolation, which applied the B-spline basis to the samples without the prefilter
that turns them into coefficients: it smoothed instead of interpolating. The IBSI 1
configuration E reference of the interpolated image (minimum -1111, maximum 2637)
showed it, and `TestSplineInterpolation` keeps it.
