# RadiomicsJ (Python Wrapper)

[![PyPI version](https://badge.fury.io/py/radiomicsj.svg)](https://pypi.org/project/radiomicsj/)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

RadiomicsJ is a Python wrapper for the Java-based radiomics feature extraction library **RadiomicsJ**. It provides highly accurate, IBSI-compliant feature calculations directly from Python, utilizing JPype to run on the JVM.

## Prerequisites
- **Java 8 or higher** must be installed and available in your system path.
- **Note:** If you are using Google Colab, the JDK is pre-installed, so no additional setup is required.

## Installation
```bash
pip install radiomicsj
```

## Quick Start

You can compute features by passing file paths or NumPy arrays directly.

### 1. Using File Paths
```python
from radiomicsj import calculate_features

'''
path to folders

# pattern1 : series folder
image_folder/images
mask_folder/masks

# pattern2 : NIfTI or Tiff, also specify parent folder.
image_folder/image.nii.gz
mask_folder/mask.nii.gz
image_folder/image.tif
mask_folder/mask.tif
'''

df = calculate_features(payh_to_image_folder, path_to_mask_folder)
print(df.head())
```
### 2. Using NumPy Arrays (In-Memory Processing)
```python
import numpy as np
from radiomicsj import calculate_features

# z,y,z order
image_np = np.random.rand(5, 10, 10)
mask_np = np.ones((5, 10, 10)) # radiomicsj default label is 1.

# Specify voxel spacing (Width, Height, Depth)
df = calculate_features(image_np, mask_np, spacing=(1.0, 1.0, 1.0))
```

### Using a Configuration file
```python
from radiomicsj.core import RadiomicsJExtractor

# Initialize with a properties file
extractor = RadiomicsJExtractor("path/to/params.properties")

results = []
for path_to_image, path_to_mask in my_image_list:
    df = extractor.execute(path_to_image, path_to_mask)
    results.append(df)

# To update properties dynamically:
# extractor.load_settings("path/to/another_params.properties")
```

## Advanced Usage
### 1. Specific Feature Calculation (Texture & Non-Texture)
You can directly access individual feature classes (e.g., GLCM, GLSZM, Morphological, IntensityBasedStatistical) to extract specific features or retrieve raw texture matrices as NumPy arrays.

```python
import numpy as np
from radiomicsj import GLCM, IntensityBasedStatistical

image_np = np.random.rand(5, 20, 20)
mask_np = np.ones((5, 20, 20))
spacing = (1.0, 1.0, 1.0)

# --- Texture Features ---
glcm = GLCM(image_np, mask_np, spacing, n_bins=16)

# Calculate ONLY specific features using IDE-friendly constants
features = glcm.calculate_features([GLCM.JointEntropy, GLCM.Contrast])
print(features) 
# -> {'JointEntropy': 2.45, 'Contrast': 5.12}

# Get the raw GLCM matrix as a NumPy array for specific angle (Z, Y, X)
matrix = glcm.get_matrix(angle=(0, 1, 0))
print(matrix.shape) 
# -> (16, 16)

# --- Non-Texture Features ---
stats = IntensityBasedStatistical(image_np, mask_np, spacing)
stat_features = stats.calculate_features([IntensityBasedStatistical.Mean, IntensityBasedStatistical.Skewness])
```
>Note regarding Discretized Intensity-Based Statistics: Discretized IntensityBasedStatisticalFeatures are obtained by passing pre-discretized image data to the standard IntensityBasedStatisticalFeature class.

### 2. Feature Map Generation (Visualization Texture Feature)
Generate 2D/3D parametric feature maps using a sliding window. RadiomicsJ optimizes this heavily by supporting strided calculations in Java followed by fast scipy-based linear interpolation in Python, drastically reducing computation time while keeping boundaries smooth and clean.

```python
import numpy as np
import matplotlib.pyplot as plt
from radiomicsj.features import generate_feature_map
from radiomicsj import GLCM

# ... load your image_np and mask_np ...

settings = {
    "label": 1,
    "useBinCount": True,
    "nBins": 16
}

# Generate Feature Map
fmap = generate_feature_map(
    image_np=image_np,
    mask_np=mask_np,
    mask_label=1,
    spacing=(1.0, 1.0, 1.0),
    feature_class=GLCM,           # Target texture class
    feature_id=GLCM.JointEntropy, # Target feature
    settings=settings,
    filter_size=5,                # Sliding window size
    d2_mode=True,                 # 2D (True) or 3D (False) mode
    stride=2,                     # Skip pixels to speed up calculation, then interpolate
    slice_idx=-1                  # Calculate for all slices
)

# Plot the result
target_z = fmap.shape[0] // 2
fmap_slice = fmap[target_z, :, :].copy()
fmap_slice[mask_np[target_z, :, :] == 0] = np.nan # Make background transparent

plt.imshow(image_np[target_z, :, :], cmap='gray')
plt.imshow(fmap_slice, cmap='jet', alpha=0.6)
plt.title("GLCM Joint Entropy Feature Map")
plt.colorbar()
plt.show()
```

## Citation
If you use RadiomicsJ in your research, please cite:
> Kobayashi T. RadiomicsJ: a library to compute radiomic features. Radiol Phys Technol. 2022 Sep;15(3):255-263. doi: 10.1007/s12194-022-00664-4. Epub 2022 Jul 6. PMID: 35792994.

