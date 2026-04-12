import os
import pytest
import pandas as pd
from radiomicsj import calculate_features

'''
First of all, remove project_dir/radiomicsj-python/radiomicsj/jars
Attention: Do not remove taregt/radiomicsj-python/radiomicsj/jars

mvn clean install
copy taregt/radiomicsj-python/radiomicsj/jars to project_dir/radiomicsj-python/radiomicsj/jars

change dir to project_dir/radiomicsj-python
(base) tatsunidas@pop-os:~/radiomicsj-workspace/RadiomicsJ/radiomicsj-python$ 
(base) tatsunidas@pop-os:~/radiomicsj-workspace/RadiomicsJ/radiomicsj-python$ pip install -e .
(base) tatsunidas@pop-os:~/radiomicsj-workspace/RadiomicsJ/radiomicsj-python$ pytest tests/test_core.py -s

'''

# テスト用の画像ファイルのパス
TEST_DIR = os.path.dirname(os.path.abspath(__file__))
SAMPLE_IMAGE = os.path.join(TEST_DIR, "ibsi_1_digital_phantom/nifti/image/")
SAMPLE_MASK = os.path.join(TEST_DIR,  "ibsi_1_digital_phantom/nifti/mask/")
SAMPLE_CONFIG = os.path.join(TEST_DIR, "Params.properties")

def test_calculate_features_with_config():
    """カスタム設定ファイルを用いた特徴量計算テスト"""
    df = calculate_features(SAMPLE_IMAGE, SAMPLE_MASK, config_prop_path=SAMPLE_CONFIG)
    assert isinstance(df, pd.DataFrame)
    assert not df.empty
    print("\n--- Configured Features ---")
    print(df.head())