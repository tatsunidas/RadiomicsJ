'''
@CopyRight : Visionary Imaging Services, Inc.
'''

import os
import math
import jpype
import jpype.imports
import pandas as pd
import numpy as np

# Mavenのビルド時に自動置換されるバージョン情報
__version__ = "${project.version}"
if __version__.startswith("${"):
    __version__ = "0.0.0.dev0"

def start_jvm():
    """JVMを起動し、jarsフォルダ内のすべてのJARをクラスパスに追加する"""
    if jpype.isJVMStarted():
        return
    
    package_dir = os.path.dirname(os.path.abspath(__file__))
    jars_dir = os.path.join(package_dir, "jars")
    
    if not os.path.exists(jars_dir):
        raise FileNotFoundError(f"Jars directory not found at: {jars_dir}")
    
    jar_path = os.path.join(jars_dir, "*")
    jpype.startJVM(classpath=[jar_path])


class RadiomicsJExtractor:
    """
    RadiomicsJの機能を利用して特徴量を抽出するクラス。
    インスタンス化時に一度だけ設定を読み込むことで、連続処理を高速化します。
    """
    
    def __init__(self, config_prop_path: str = None):
        """
        Args:
            config_prop_path (str, optional): 設定プロパティファイルのパス。
        """
        if not jpype.isJVMStarted():
            start_jvm()
            
        self._RadiomicsJClass = jpype.JClass("io.github.tatsunidas.radiomics.main.RadiomicsJ")
        self.radj = self._RadiomicsJClass()
        
        # ImageJのパッケージを取得
        self.ij = jpype.JPackage("ij")
        
        if config_prop_path is not None:
            self.load_settings(config_prop_path)

    def load_settings(self, config_prop_path: str):
        """プロパティファイルを読み込み、設定を更新します。"""
        if not os.path.exists(config_prop_path):
            raise FileNotFoundError(f"Config file not found: {config_prop_path}")
        self.radj.loadSettings(config_prop_path)

    def _numpy_to_imageplus(self, arr: np.ndarray, spacing: tuple, title: str) -> "ij.ImagePlus":
       """
       NumPy ndarray to ImagePlus directly.
       arr : (Z, Y, X) 3d-array, if ndarray sorted by ordered (X, Y, Z), do np.transpose(arr, (2, 1, 0)) before input. 
       spacing : (x,y,z) voxel size
       """
       # ImageJは内部的にfloat配列を扱うため、float32に変換
       arr_f = arr.astype(np.float32)
       
       if arr.ndim == 2:
           h, w = arr_f.shape
             # メモリコピーを最小限にJArrayへ変換
           jarr = jpype.JArray(jpype.JFloat)(arr_f.ravel())
           proc = self.ij.process.FloatProcessor(w, h, jarr)
           img_plus = self.ij.ImagePlus(title, proc)
            
           cal = img_plus.getCalibration()
           cal.pixelWidth = spacing[0]
           cal.pixelHeight = spacing[1]
            
       elif arr.ndim == 3:
           d, h, w = arr_f.shape
           stack = self.ij.ImageStack(w, h)
           
           for i in range(d):
                 # 1スライスごとにJArrayに変換してスタックに追加
               slice_arr = arr_f[i, :, :]
               jarr = jpype.JArray(jpype.JFloat)(slice_arr.ravel())
               proc = self.ij.process.FloatProcessor(w, h, jarr)
               stack.addSlice(str(i), proc)
                
           img_plus = self.ij.ImagePlus(title, stack)
            
           cal = img_plus.getCalibration()
           cal.pixelWidth = spacing[0]
           cal.pixelHeight = spacing[1]
           cal.pixelDepth = spacing[2]
       else:
           raise ValueError("NumPy array must be 2D or 3D.")
            
       return img_plus

    def execute(self, image, mask, spacing=(1.0, 1.0, 1.0)) -> pd.DataFrame:
       """
        特徴量を計算し、Pandas DataFrameとして返します。
       Args:
           image: ファイルパス(str) または NumPy配列(np.ndarray)
           mask: ファイルパス(str) または NumPy配列(np.ndarray)
           spacing (tuple): NumPy配列の場合のピクセル間隔 (x, y, z)。デフォルトは(1.0, 1.0, 1.0)
            
       Returns:
           pd.DataFrame: 計算された特徴量
       """
       # Imageの変換
       if isinstance(image, np.ndarray):
           target_image = self._numpy_to_imageplus(image, spacing, title="target_image")
       else:
           target_image = image

       # Maskの変換
       if isinstance(mask, np.ndarray):
           target_mask = self._numpy_to_imageplus(mask, spacing, title="target_mask")
       else:
           target_mask = mask

       # Java側の実行 (JPypeが自動で String と ImagePlus のオーバーロードを判別してくれます)
       res_table = self.radj.execute(target_image, target_mask, self._RadiomicsJClass.targetLabel)
        
       if res_table is None or res_table.size() == 0:
           return pd.DataFrame()

       # DataFrameへの変換処理
       headings = [str(h) for h in res_table.getHeadings()]
       num_rows = res_table.size()
       data_list = []
       for i in range(num_rows):
           row_dict = {}
           for h in headings:
               val = res_table.getValue(h, i)
               if math.isnan(val):
                   str_val = res_table.getStringValue(h, i)
                   row_dict[h] = str(str_val) if str_val is not None else float('nan')
               else:
                   row_dict[h] = val
           data_list.append(row_dict)
        
       return pd.DataFrame(data_list)

def calculate_features(image, mask, config_prop_path: str = None, spacing=(1.0, 1.0, 1.0)) -> pd.DataFrame:
    """単一処理用の簡易ラッパー関数"""
    extractor = RadiomicsJExtractor(config_prop_path)
    return extractor.execute(image, mask, spacing=spacing)
    