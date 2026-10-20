# IBSI 2 reference feature values, configuration B

**Status: not reached.** The unfiltered baseline does not reproduce its reference, so
the mean and LoG rows below cannot be read as a verdict on those filters.

`TestFilteredFeatureReference` (disabled) runs the 3D configuration B of the manual
(Table 6.2) on the IBSI 2 lung cancer CT phantom: tricubic spline to 1 x 1 x 1 mm,
intensities rounded, trilinear mask with partial volume 0.5, re-segmentation to
[-1000, 400] HU, mirror padding. It compares the 18 intensity based statistical
features with `reference_values.csv` of `theibsi/ibsi_2_reference_data`.
Configuration A (a 2D filter, the features over the whole roi) is not testable:
`BOOL_force2D` takes the features slice by slice.

| id | filter | within tolerance | out of tolerance |
|---|---|---|---|
| 1.B | none | 11 of 18 | variance, median, minimum, 90th percentile, maximum, range, root mean square |
| 2.B | mean, M = 5 | 12 of 18 | median, minimum, 90th percentile, maximum, range, quartile coefficient of dispersion |
| 3.B | LoG, sigma 1.5 mm | 3 of 18 | 15 features (all but mean, kurtosis, coefficient of variation) |

Largest gaps of the baseline: minimum -925 against -997 (+-3), maximum 396 against
377 (+-15), median 39 against 41 (+-0.7). The extremes of the resampled image are
what differs, which points at the interpolation or the mask of the pipeline rather
than at a filter. This has not been traced; the cause is unknown.

The filters themselves agree with the IBSI 2 response maps (`TestMeanFilter`,
`TestLoGFilter`, within 0.04% of the range).
