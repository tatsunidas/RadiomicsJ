# RadiomicsJ (Python Wrapper)

RadiomicsJ(Python wrapper) is an image feature extraction tool implemented in Java.

## Main Function
- Radiomics Feature calculation.
- Calculation results will return by Pandas DataFrame.

## Install Requirements

### When use : pip
You need install Java (>= JDK 8), and add it's jdk/bin to environment path.

```bash
pip install radiomicsj

## Example

```python
from radiomicsj.core import RadiomicsJExtractor

# init
extractor = RadiomicsJExtractor("path/to/params.properties")

results = []
for image_path, mask_path in my_image_list:
    df = extractor.execute(image_path, mask_path)
    results.append(df)

# If you want to set properties case by case.
# extractor.load_settings("path/to/another_params.properties")