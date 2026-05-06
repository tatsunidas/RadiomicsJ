import numpy as np
import radiomicsj

# ターミナルで radiomicsj-python ディレクトリに移動し、以下のコマンドを実行します。
# python tests/test_features.py
# もしくは pytest を使う場合
# pytest tests/test_features.py -s

def test_all_texture_classes():
    print("--- 準備: ダミー画像の作成 ---")
    # Z, Y, X の3Dダミー画像とマスクを作成
    # 計算が早く終わるように小さなサイズ(3x10x10)にしています
    image_np = np.random.rand(3, 10, 10).astype(np.float32) * 100
    mask_np = np.ones((3, 10, 10), dtype=np.float32)
    spacing = (1.0, 1.0, 1.0)

    print("画像サイズ:", image_np.shape)
    print("=========================================\n")

    # --- 1. GLCM ---
    print("[1] Testing GLCM...")
    glcm = radiomicsj.GLCM(image_np, mask_np, spacing, n_bins=16)
    glcm_features = glcm.get_all_features()
    glcm_matrix = glcm.get_matrix(angle=(0, 1, 0)) # 角度が必要
    print(f"  -> Features calculated: {len(glcm_features)} features")
    print(f"  -> Matrix shape: {glcm_matrix.shape if glcm_matrix is not None else 'None'}")

    # --- 2. GLRLM ---
    print("\n[2] Testing GLRLM...")
    glrlm = radiomicsj.GLRLM(image_np, mask_np, spacing, n_bins=16)
    glrlm_features = glrlm.get_all_features()
    glrlm_matrix = glrlm.get_matrix(angle=(0, 1, 0)) # 角度が必要
    print(f"  -> Features calculated: {len(glrlm_features)} features")
    print(f"  -> Matrix shape: {glrlm_matrix.shape if glrlm_matrix is not None else 'None'}")

    # --- 3. GLSZM ---
    print("\n[3] Testing GLSZM...")
    glszm = radiomicsj.GLSZM(image_np, mask_np, spacing, n_bins=16)
    glszm_features = glszm.get_all_features()
    glszm_matrix = glszm.get_matrix(raw=False) # 角度は不要、rawフラグが必要
    print(f"  -> Features calculated: {len(glszm_features)} features")
    print(f"  -> Matrix shape: {glszm_matrix.shape if glszm_matrix is not None else 'None'}")

    # --- 4. GLDZM ---
    print("\n[4] Testing GLDZM...")
    gldzm = radiomicsj.GLDZM(image_np, mask_np, spacing, n_bins=16)
    gldzm_features = gldzm.get_all_features()
    gldzm_matrix = gldzm.get_matrix(raw=False) # 角度は不要、rawフラグが必要
    print(f"  -> Features calculated: {len(gldzm_features)} features")
    print(f"  -> Matrix shape: {gldzm_matrix.shape if gldzm_matrix is not None else 'None'}")

    # --- 5. NGLDM ---
    print("\n[5] Testing NGLDM...")
    ngldm = radiomicsj.NGLDM(image_np, mask_np, spacing, n_bins=16, alpha=0, delta=1)
    ngldm_features = ngldm.get_all_features()
    ngldm_matrix = ngldm.get_matrix() # 引数不要
    print(f"  -> Features calculated: {len(ngldm_features)} features")
    print(f"  -> Matrix shape: {ngldm_matrix.shape if ngldm_matrix is not None else 'None'}")

    # --- 6. NGTDM ---
    print("\n[6] Testing NGTDM...")
    ngtdm = radiomicsj.NGTDM(image_np, mask_np, spacing, n_bins=16, delta=1)
    ngtdm_features = ngtdm.get_all_features()
    ngtdm_matrix = ngtdm.get_matrix() # 引数不要
    print(f"  -> Features calculated: {len(ngtdm_features)} features")
    print(f"  -> Matrix shape: {ngtdm_matrix.shape if ngtdm_matrix is not None else 'None'}")

    print("\n=========================================")
    print("ALL TESTS PASSED SUCCESSFULLY! 🎉")

if __name__ == "__main__":
    test_all_texture_classes()
    