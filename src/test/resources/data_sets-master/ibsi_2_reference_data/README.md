# IBSI 2 reference response maps (Laplacian of Gaussian)

Three of the consensus response maps published by the Image Biomarker
Standardisation Initiative at https://github.com/theibsi/ibsi_2_reference_data
(CC0 1.0, see LICENSE), copied unchanged:

| file | phantom | sigma | truncation | padding | dimension |
|---|---|---|---|---|---|
| 2_a-ValidCRM.nii | impulse | 3.0 mm | 4 sigma | constant (zero) | 3D |
| 2_b-ValidCRM.nii | checkerboard | 5.0 mm | 4 sigma | mirror | 3D |
| 2_c-ValidCRM.nii | checkerboard | 5.0 mm | 4 sigma | mirror | 2D (axial) |

The settings are those of test set 1, table 6.1 of the IBSI 2 reference manual
(arXiv 2006.05470 v9). A response map complies when every voxel differs from the
reference by at most 1% of the reference's intensity range (section 7.1). The
phantoms themselves are in `../ibsi_2_digital_phantom/`.
