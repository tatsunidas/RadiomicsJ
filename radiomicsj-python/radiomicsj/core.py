
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
if __version__.startswith("${"):
    __version__ = "0.0.0.dev0"

def start_jvm():
    """JVMを起動し、RadiomicsJ.jarをクラスパスに追加する"""
    if jpype.isJVMStarted():
        return
    
    # jarファイルのパスを取得
    package_dir = os.path.dirname(os.path.abspath(__file__))
    jars_dir = os.path.join(package_dir, "jars")
    
    if not os.path.exists(jars_dir):
        raise FileNotFoundError(f"Jars directory not found at: {jars_dir}")
    
    # JVMの起動 (必要に応じてメモリ割り当てなどを追加)
    jar_path = os.path.join(jars_dir, "*")
    jpype.startJVM(classpath=[jar_path])

def calculate_features(image_path, mask_path, config_prop_path=None) -> pd.DataFrame:
    """
    RadiomicsJの機能を利用するラッパー関数
    Do not Use Tab indent, Use 4 spaces instead.
    """
    if not jpype.isJVMStarted():
        start_jvm()
        
    # Javaのクラスをインポートして使用する例
    # do not use imoirt state to avoid conflict with the python default io package.
    #from io.github.tatsunidas.radiomics.main import RadiomicsJ
    # Javaのクラスをインポートして使用する例
    # Pythonの標準モジュール 'io' との衝突を避けるため、JClassを使用します
    RadiomicsJ = jpype.JClass("io.github.tatsunidas.radiomics.main.RadiomicsJ")
    from ij.measure import ResultsTable
    radj = RadiomicsJ()
    if config_prop_path is not None and os.path.exists(config_prop_path):
	    radj.loadSettings(config_prop_path)
    res_table = radj.execute(image_path, mask_path, RadiomicsJ.targetLabel)
    # 6. 結果が空の場合は空のDataFrameを返す
    if res_table is None or res_table.size() == 0:
        return pd.DataFrame()

    # 7. ImageJ ResultsTable -> Pandas DataFrame への変換処理
    # Javaの配列 (String[]) を Pythonのリストに変換
    headings = [str(h) for h in res_table.getHeadings()]
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
                row_dict[h] = str(str_val) if str_val is not None else float('nan')
            else:
                row_dict[h] = val
                
        data.append(row_dict)

    # リスト辞書からDataFrameを作成
    df = pd.DataFrame(data)
    
    return df
    
    
    