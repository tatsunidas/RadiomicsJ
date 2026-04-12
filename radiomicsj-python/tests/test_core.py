import os
import pytest
import pandas as pd
import numpy as np
from radiomicsj import calculate_features

'''
First of all, remove project_dir/radiomicsj-python/radiomicsj/jars
Attention: Do not remove taregt/radiomicsj-python/radiomicsj/jars

mvn clean install

if project_dir/radiomicsj-python/radiomicsj/jars is not created,
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
    
def create_digital_phantom():
    """
    IBSI Digital Phantom 1 の画像とマスクをNumPy配列として生成します。
    Shape: (Z=4, Y=4, X=5)
    """
    # Image (Z, Y, X)
    image = np.array([
        # slice 0
        [[1, 4, 4, 1, 1],
         [1, 4, 6, 1, 1],
         [4, 1, 6, 4, 1],
         [4, 4, 6, 4, 1]],
        # slice 1
        [[1, 4, 4, 1, 1],
         [1, 1, 6, 1, 1],
         [1, 1, 3, 1, 1],
         [4, 4, 6, 1, 1]],
        # slice 2
        [[1, 4, 4, 1, 1],
         [1, 1, 1, 1, 1],
         [1, 1, 9, 1, 1],
         [1, 1, 6, 1, 1]],
        # slice 3
        [[1, 4, 4, 1, 1],
         [1, 1, 1, 1, 1],
         [1, 1, 1, 1, 1],
         [1, 1, 6, 1, 1]]
    ], dtype=np.float32)

    # Mask (Z, Y, X)
    mask = np.array([
        # slice 0
        [[1, 1, 1, 1, 1],
         [1, 1, 1, 1, 1],
         [1, 1, 1, 1, 1],
         [1, 1, 1, 1, 1]],
        # slice 1
        [[1, 1, 1, 1, 1],
         [1, 1, 1, 1, 1],
         [0, 1, 1, 1, 1],
         [1, 1, 1, 1, 1]],
        # slice 2
        [[1, 1, 1, 0, 0],
         [1, 1, 1, 1, 1],
         [1, 1, 0, 1, 1],
         [1, 1, 1, 1, 1]],
        # slice 3
        [[1, 1, 1, 0, 0],
         [1, 1, 1, 1, 1],
         [1, 1, 1, 1, 1],
         [1, 1, 1, 1, 1]]
    ], dtype=np.float32)
    
    return image, mask


def test_calculate_features_numpy_phantom():
    """NumPy配列を直接渡してIBSIファントムの特徴量を計算するテスト"""
    
    # 1. ファントムデータの生成
    image_np, mask_np = create_digital_phantom()
    
    # Javaコードの Calibration に合わせた Spacing (Width=2.0, Height=2.0, Depth=2.0)
    spacing = (2.0, 2.0, 2.0)
    
    print(f"\n[Test] Numpy Phantom Shape: Image={image_np.shape}, Mask={mask_np.shape}")
    print(f"[Test] Spacing: {spacing}")

    # 2. 特徴量計算（ファイルパスの代わりにNumPy配列を渡す）
    df = calculate_features(image_np, mask_np, spacing=spacing)
    
    # 3. 検証
    assert isinstance(df, pd.DataFrame), "戻り値がDataFrameではありません"
    assert not df.empty, "DataFrameが空です（計算に失敗しています）"
    
    print("\n--- Features from Memory (NumPy) ---")
    print(df.head())