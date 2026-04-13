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
## Citation
If you use RadiomicsJ in your research, please cite:
> Kobayashi T. RadiomicsJ: a library to compute radiomic features. Radiol Phys Technol. 2022 Sep;15(3):255-263. doi: 10.1007/s12194-022-00664-4. Epub 2022 Jul 6. PMID: 35792994.

