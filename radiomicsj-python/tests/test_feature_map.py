import numpy as np
import matplotlib.pyplot as plt
from radiomicsj.features import generate_feature_map
from radiomicsj import GLCM # importしておく

def test_visualization_map():
    print("--- 1. 準備: ダミー画像の作成 ---")
    # Z=15, Y=60, X=60 の3D画像を作成
    z, h, w = 15, 60, 60
    image_np = np.zeros((z, h, w), dtype=np.float32)
    mask_np = np.zeros((z, h, w), dtype=np.float32)

    # 単なるランダムノイズだと特徴量マップが平坦になるため、
    # X軸方向にグラデーションをかけつつノイズを足して「テクスチャの変化」を作ります
    for y in range(h):
        for x in range(w):
            image_np[:, y, x] = x + np.random.rand(z) * 10

    # 中央に円形（球体）のマスク(ROI)を作成
    cy, cx = h // 2, w // 2
    radius = 20
    for y in range(h):
        for x in range(w):
            if (y - cy)**2 + (x - cx)**2 <= radius**2:
                mask_np[:, y, x] = 1.0

    spacing = (1.0, 1.0, 1.0)
    
    # RadiomicsFeatureに渡すパラメータ設定
    # ※ Java側で RadiomicsFeature.LABEL 等の定数は単なる文字列（"label"等）です
    settings = {
        "label": 1,
        "useBinCount": True,
        "nBins": 16
    }

    print("--- 2. Feature Mapの計算開始 ---")
    # 🌟 ここがポイント：stride=2 で間引き計算を行い、Python側で元のサイズに補間します
	feature_map = generate_feature_map(
        image_np=image_np,
        mask_np=mask_np,
        mask_label=1,
        spacing=spacing,
        feature_class=GLCM,              # 🌟 文字列ではなくクラスを直接渡す！
        feature_id=GLCM.JointEntropy,    # 🌟 IDEでオートコンプリートが効く！
        settings=settings,
        filter_size=5,
        d2_mode=True,
        stride=2,
        slice_idx=-1
    )

    print(f"--- 3. 計算完了！ 結果のShape: {feature_map.shape} ---")
    print("描画ウィンドウを開きます...")

    # 表示するスライス（真ん中のスライス Z=7）
    target_z = z // 2

    fig, axes = plt.subplots(1, 3, figsize=(15, 5))
    
    # (1) 元画像
    ax = axes[0]
    im1 = ax.imshow(image_np[target_z, :, :], cmap='gray')
    ax.set_title(f"Original Image (Slice Z={target_z})")
    fig.colorbar(im1, ax=ax)

    # (2) マスク
    ax = axes[1]
    im2 = ax.imshow(mask_np[target_z, :, :], cmap='gray')
    ax.set_title("ROI Mask")
    fig.colorbar(im2, ax=ax)

    # (3) 特徴量マップ
    ax = axes[2]
    # 💡 描画のテクニック：背景（マスクが0の場所）を np.nan にすると、
    # 　Matplotlibがそこを透明（または白）として綺麗に描画してくれます。
    fmap_slice = feature_map[target_z, :, :].copy()
    fmap_slice[mask_np[target_z, :, :] == 0] = np.nan
    
    im3 = ax.imshow(fmap_slice, cmap='jet') # 特徴量マップは jet 等のカラーマップが見やすいです
    ax.set_title("Feature Map (GLCM - Joint Entropy)\nStride=2 + Interpolated")
    fig.colorbar(im3, ax=ax)

    plt.tight_layout()
    plt.show()

if __name__ == "__main__":
    test_visualization_map()