
'''
@Copy right Visionary Imaging Services, Inc.
@since 2026
'''

import os
import math
import jpype
import jpype.imports
import pandas as pd

__version__ = "${project.version}"

def start_jvm():
    """JVMを起動し、RadiomicsJ.jarをクラスパスに追加する"""
    if jpype.isJVMStarted():
        return
    
    # jarファイルのパスを取得
    package_dir = os.path.dirname(os.path.abspath(__file__))
    jar_path = os.path.join(package_dir, "jars", "RadiomicsJ.jar")
    
    if not os.path.exists(jar_path):
        raise FileNotFoundError(f"RadiomicsJ jar not found at: {jar_path}")
    
    # JVMの起動 (必要に応じてメモリ割り当てなどを追加)
    jpype.startJVM(classpath=[jar_path])

def calculate_features(image_path, mask_path, config_prop_path=None) -> pd.DataFrame:
    """
    RadiomicsJの機能を利用するラッパー関数
    """
    if not jpype.isJVMStarted():
        start_jvm()
        
    # Javaのクラスをインポートして使用する例
    from io.github.tatsunidas.radiomics.main import RadiomicsJ
    from ij.measure import ResultsTable
	radj = RadiomicsJ();
	if config_prop_path is not None and os.path.exists(config_prop_path):
		radj.loadSettingsFromResource(config_prop_path);
	res_table = radj.execute(images, masks, RadiomicsJ.targetLabel);
	# 6. 結果が空の場合は空のDataFrameを返す
    if res_table is None or res_table.size() == 0:
        return pd.DataFrame()

    # 7. ImageJ ResultsTable -> Pandas DataFrame への変換処理
    # Javaの配列 (String[]) を Pythonのリストに変換
    headings = list(res_table.getHeadings())
    num_rows = res_table.size()
    
    data = []
    for i in range(num_rows):
        row_dict = {}
        for h in headings:
            # getValue() は数値を返すが、文字列セル（画像名など）の場合は Double.NaN を返す仕様
            val = res_table.getValue(h, i)
            
            if math.isnan(val):
                # NaNだった場合、文字列として再取得を試みる
                str_val = res_table.getStringValue(h, i)
                # null (None) でなければ文字列を採用
                row_dict[h] = str_val if str_val is not None else val
            else:
                row_dict[h] = val
                
        data.append(row_dict)

    # リスト辞書からDataFrameを作成
    df = pd.DataFrame(data)
    
    return df
    
    
    