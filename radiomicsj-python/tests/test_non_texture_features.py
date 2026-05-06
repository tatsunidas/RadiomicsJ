import numpy as np
import radiomicsj

def test_non_texture_classes():
    print("--- 準備: ダミー画像の作成 ---")
    # 計算が早く終わるように小さなサイズ(5x10x10)で作成
    z, h, w = 5, 10, 10
    image_np = np.random.rand(z, h, w).astype(np.float32) * 100
    
    # 中央部分だけをマスク（1.0）にする
    mask_np = np.zeros((z, h, w), dtype=np.float32)
    mask_np[1:4, 2:8, 2:8] = 1.0
    
    spacing = (1.0, 1.0, 1.0)

    print("画像サイズ:", image_np.shape)
    print("マスク内ボクセル数:", np.sum(mask_np))
    print("=========================================\n")

    # --- 1. Morphological (形態学的特徴) ---
    print("[1] Testing Morphological...")
    morph = radiomicsj.Morphological(image_np, mask_np, spacing)
    # 特定の特徴量だけを計算するテスト
    morph_res = morph.calculate_features([
        radiomicsj.Morphological.VolumeMesh,
        radiomicsj.Morphological.Sphericity
    ])
    print(f"  -> Selected Features: {morph_res}")

    # --- 2. IntensityBasedStatistical (強度ベース統計) ---
    print("\n[2] Testing IntensityBasedStatistical...")
    ibs = radiomicsj.IntensityBasedStatistical(image_np, mask_np, spacing)
    # 全特徴量を計算するテスト
    ibs_res = ibs.get_all_features()
    print(f"  -> Features calculated: {len(ibs_res)} features")
    # ピックアップして表示
    print(f"  -> Mean: {ibs_res.get('Mean')}, Skewness: {ibs_res.get('Skewness')}")

    # --- 3. IntensityHistogram (強度ヒストグラム) ---
    print("\n[3] Testing IntensityHistogram...")
    # ヒストグラム系は n_bins などの設定が可能
    ih = radiomicsj.IntensityHistogram(image_np, mask_np, spacing, use_bin_count=True, n_bins=16)
    ih_res = ih.calculate_features([
        radiomicsj.IntensityHistogram.Entropy,
        radiomicsj.IntensityHistogram.Uniformity
    ])
    print(f"  -> Selected Features: {ih_res}")

    # --- 4. IntensityVolumeHistogram (強度体積ヒストグラム) ---
    print("\n[4] Testing IntensityVolumeHistogram...")
    ivh = radiomicsj.IntensityVolumeHistogram(image_np, mask_np, spacing, use_bin_count=True, n_bins=16)
    ivh_res = ivh.get_all_features()
    print(f"  -> Features calculated: {len(ivh_res)} features")
    print(f"  -> VolumeAtIntensityFraction10: {ivh_res.get('VolumeAtIntensityFraction10')}")

    # --- 5. LocalIntensity (局所強度) ---
    print("\n[5] Testing LocalIntensity...")
    li = radiomicsj.LocalIntensity(image_np, mask_np, spacing)
    li_res = li.get_all_features()
    print(f"  -> Features calculated: {len(li_res)} features")
    print(f"  -> GlobalIntensityPeak: {li_res.get('GlobalIntensityPeak')}")

    print("\n=========================================")
    print("ALL NON-TEXTURE TESTS PASSED SUCCESSFULLY! 🎉")

if __name__ == "__main__":
    test_non_texture_classes()