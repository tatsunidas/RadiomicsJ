"""GLAM の結果を目視評価するための図を生成します。

前半は合成デジタルファントム（ヒストグラムが同一で並び方だけが違う 3 種）、
後半は IBSI CT radiomics phantom の実データ（GTV）を使います。

    cd radiomicsj-python
    python3 ../docs/make_glam_figures.py

出力は docs/images/ に PNG で保存されます。
"""
import os
import sys

import matplotlib
matplotlib.use("Agg")
import matplotlib.font_manager as fm
import matplotlib.pyplot as plt
import numpy as np
from matplotlib.colors import SymLogNorm


def use_japanese_font():
    """日本語のラベルが豆腐にならないよう、CJK フォントがあれば登録します。"""
    candidates = [
        "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
        "/usr/share/fonts/truetype/fonts-japanese-gothic.ttf",
        "/System/Library/Fonts/ヒラギノ角ゴシック W3.ttc",
        "C:/Windows/Fonts/meiryo.ttc",
    ]
    for path in candidates:
        if os.path.exists(path):
            fm.fontManager.addfont(path)
            name = fm.FontProperties(fname=path).get_name()
            plt.rcParams["font.family"] = name
            plt.rcParams["axes.unicode_minus"] = False
            return name
    print("  日本語フォントが見つかりませんでした。ラベルが文字化けする場合があります。")
    return None

REPO = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
sys.path.insert(0, os.path.join(REPO, "radiomicsj-python"))
OUT = os.path.join(REPO, "docs", "images")

import radiomicsj  # noqa: E402

SIZE = 32
LEVELS = 4
MAX_RADIUS = 16


# --------------------------------------------------------------------------
# 合成ファントム。3 つとも各濃度値ちょうど 8192 ボクセルで、並べ方だけが違う。
# --------------------------------------------------------------------------
def salt_and_pepper():
    rng = np.random.default_rng(20260802)
    values = np.arange(SIZE ** 3) % LEVELS
    rng.shuffle(values)
    return values.reshape(SIZE, SIZE, SIZE).astype(np.float32)


def blocks(block=8):
    per_axis = SIZE // block
    rng = np.random.default_rng(20260802)
    block_level = np.arange(per_axis ** 3) % LEVELS
    rng.shuffle(block_level)
    return np.kron(block_level.reshape(per_axis, per_axis, per_axis),
                   np.ones((block, block, block))).astype(np.float32)


def onion():
    c = (SIZE - 1) / 2.0
    z, y, x = np.mgrid[:SIZE, :SIZE, :SIZE]
    d = (z - c) ** 2 + (y - c) ** 2 + (x - c) ** 2
    order = np.argsort(d.ravel(), kind="stable")
    levels = np.empty(SIZE ** 3, dtype=np.float32)
    levels[order] = np.minimum(np.arange(SIZE ** 3) // (SIZE ** 3 // LEVELS), LEVELS - 1)
    return levels.reshape(SIZE, SIZE, SIZE)


PHANTOMS = [("salt and pepper", salt_and_pepper()),
            ("blocks", blocks()),
            ("onion", onion())]


def glam_of(image, mask=None, n_bins=LEVELS, max_radius=MAX_RADIUS):
    if mask is None:
        mask = np.ones_like(image, dtype=np.float32)
    return radiomicsj.GLAM(image, mask, spacing=(1.0, 1.0, 1.0),
                           n_bins=n_bins, max_radius=max_radius)


def divergent(matrix):
    """0 を中心に、桁の広い値を潰さずに見せるための色スケール。"""
    finite = matrix[np.isfinite(matrix)]
    if finite.size == 0:
        return None
    scale = np.max(np.abs(finite))
    if scale == 0:
        return None
    return SymLogNorm(linthresh=max(scale * 1e-3, 1e-12), vmin=-scale, vmax=scale)


# --------------------------------------------------------------------------
# 図 1: ファントムの見た目と自己親和性曲線
# --------------------------------------------------------------------------
def figure_phantoms_and_rdf(results):
    fig, axes = plt.subplots(2, 3, figsize=(15, 9))
    mid = SIZE // 2
    for col, ((name, image), glam) in enumerate(zip(PHANTOMS, results)):
        ax = axes[0, col]
        ax.imshow(image[mid], cmap="viridis", vmin=0, vmax=LEVELS - 1, interpolation="nearest")
        ax.set_title(f"{name}\n(slice z={mid})")
        ax.set_xticks([]); ax.set_yticks([])

        ax = axes[1, col]
        rdf = glam.get_rdf()
        r = np.arange(1, MAX_RADIUS + 1)
        for level in range(LEVELS):
            ax.plot(r, rdf[1:, level, level], marker="o", ms=3, label=f"level {level}")
        ax.axhline(1.0, ls="--", c="gray", lw=1)
        ax.set_ylim(0, 4.2)
        ax.set_xlabel("distance r [voxel]")
        if col == 0:
            ax.set_ylabel(r"self affinity  $g(\alpha,\alpha,r)$")
        ax.legend(fontsize=8)
        ax.grid(alpha=0.3)
    fig.suptitle("同じヒストグラム、違う並べ方。破線 1.0 は「偶然と区別がつかない」水準",
                 fontsize=13)
    fig.tight_layout()
    path = os.path.join(OUT, "glam_phantoms_rdf.png")
    fig.savefig(path, dpi=110)
    plt.close(fig)
    return path


# --------------------------------------------------------------------------
# 図 2: 親和性行列のヒートマップ
# --------------------------------------------------------------------------
MATRICES = [
    ("SecondVirialCoefficient", "第二ビリアル係数 B2\n負=引き合う / 正=避け合う", True),
    ("CoordinationNumber", "配位数 Z\n第一配位殻の隣人数", False),
    ("ConfigurationalDisorderIndex", "配置無秩序度 CDI", True),
    ("PhenotypicDistance", "表現型距離\n空間トポロジーの隔たり", False),
]


def figure_matrices(results):
    fig, axes = plt.subplots(len(MATRICES), 3, figsize=(13, 4 * len(MATRICES)))
    for row, (key, title, symmetric_scale) in enumerate(MATRICES):
        values = [glam.get_matrix(key) for glam in results]
        shared = None
        if not symmetric_scale:
            finite = np.concatenate([m[np.isfinite(m)].ravel() for m in values])
            if finite.size:
                shared = (float(finite.min()), float(finite.max()))
        for col, ((name, _), matrix) in enumerate(zip(PHANTOMS, values)):
            ax = axes[row, col]
            if symmetric_scale:
                im = ax.imshow(matrix, cmap="RdBu_r", norm=divergent(matrix),
                               interpolation="nearest")
            else:
                im = ax.imshow(matrix, cmap="viridis", interpolation="nearest",
                               vmin=None if shared is None else shared[0],
                               vmax=None if shared is None else shared[1])
            ax.set_xticks(range(LEVELS)); ax.set_yticks(range(LEVELS))
            ax.set_xlabel(r"$\beta$"); ax.set_ylabel(r"$\alpha$")
            ax.set_title(f"{name}", fontsize=10)
            for a in range(LEVELS):
                for b in range(LEVELS):
                    v = matrix[a, b]
                    ax.text(b, a, "nan" if not np.isfinite(v) else f"{v:.3g}",
                            ha="center", va="center", fontsize=7,
                            color="k" if symmetric_scale else "w")
            fig.colorbar(im, ax=ax, fraction=0.046)
        axes[row, 0].text(-0.45, 0.5, title, transform=axes[row, 0].transAxes,
                          rotation=90, va="center", ha="center", fontsize=11)
    fig.suptitle("GLAM 親和性行列。行 alpha, 列 beta は離散化後の濃度値", fontsize=13)
    fig.tight_layout(rect=[0.03, 0, 1, 0.98])
    path = os.path.join(OUT, "glam_matrices.png")
    fig.savefig(path, dpi=110)
    plt.close(fig)
    return path


# --------------------------------------------------------------------------
# 図 3: 実データ、IBSI CT radiomics phantom の GTV
# --------------------------------------------------------------------------
def load_ct_phantom(target_spacing=1.0, hu_range=(-500.0, 400.0)):
    """CT と GTV を読み込み、GLAM が前提とする等方ボクセルへ揃えます。

    hu_range は IBSI の CT phantom 設定 C/D と同じ再セグメント範囲です。
    肺野から軟部組織まで一気に含む生の HU レンジで固定ビン数を切ると、
    ほとんどのボクセルが 1 つのビンに落ち、残りのビンが数十ボクセルに
    なってしまい、g(r) が数千倍に跳ねて図が読めなくなります。
    """
    import SimpleITK as sitk
    import scipy.ndimage

    base = os.path.join(REPO, "src", "test", "resources", "data_sets-master",
                        "ibsi_1_ct_radiomics_phantom", "nifti")
    image = sitk.ReadImage(os.path.join(base, "image", "phantom.nii.gz"))
    mask = sitk.ReadImage(os.path.join(base, "mask", "mask.nii.gz"))

    sx, sy, sz = image.GetSpacing()             # sitk は x, y, z の順
    volume = sitk.GetArrayFromImage(image).astype(np.float32)   # numpy は z, y, x の順
    roi = sitk.GetArrayFromImage(mask).astype(np.float32)
    zoom = (sz / target_spacing, sy / target_spacing, sx / target_spacing)

    volume = scipy.ndimage.zoom(volume, zoom, order=1)
    roi = scipy.ndimage.zoom(roi, zoom, order=0)

    roi = (roi > 0).astype(np.float32)
    if hu_range is not None:
        # 範囲外のボクセルを roi から外す（再セグメント）
        low, high = hu_range
        roi[(volume < low) | (volume > high)] = 0.0

    # ROI の外接箱で切り出して、無駄なボクセルを持ち回らないようにします
    zz, yy, xx = np.nonzero(roi > 0)
    sl = (slice(zz.min(), zz.max() + 1),
          slice(yy.min(), yy.max() + 1),
          slice(xx.min(), xx.max() + 1))
    return volume[sl], roi[sl]


def figure_real_data(n_bins=16, max_radius=30, max_reference_voxels=1500):
    volume, roi = load_ct_phantom()
    voxels = int(roi.sum())
    print(f"  IBSI CT phantom GTV: {volume.shape}, roi {voxels} voxels")

    glam = radiomicsj.GLAM(volume, roi, spacing=(1.0, 1.0, 1.0), label=1,
                           n_bins=n_bins, max_radius=max_radius,
                           max_reference_voxels=max_reference_voxels)
    rdf = glam.get_rdf()
    r = np.arange(1, max_radius + 1)

    fig = plt.figure(figsize=(16, 9.5))
    grid = fig.add_gridspec(2, 3, height_ratios=[1, 1])

    # ROI を最も多く含むスライス
    z = int(np.argmax(roi.sum(axis=(1, 2))))
    ax = fig.add_subplot(grid[0, 0])
    ax.imshow(volume[z], cmap="gray", vmin=-200, vmax=200)
    ax.contour(roi[z], levels=[0.5], colors="r", linewidths=1.2)
    ax.set_title(f"CT + 再セグメント後の roi (slice z={z})\nroi = {voxels} voxels")
    ax.set_xticks([]); ax.set_yticks([])

    # ビン占有数。g(r) の大きさは、その濃度値がどれだけ稀かで決まります。
    ax = fig.add_subplot(grid[0, 1])
    hu = volume[roi > 0]
    edges = np.linspace(hu.min(), hu.max(), n_bins + 1)
    counts = np.histogram(hu, bins=edges)[0]
    ax.bar(range(n_bins), counts, color=plt.get_cmap("viridis")(
        np.linspace(0, 1, n_bins)))
    ax.set_yscale("log")
    ax.set_xlabel("離散化後の濃度値")
    ax.set_ylabel("ボクセル数")
    ax.set_title("ビン占有数。稀な濃度値ほど g(r) は大きく出ます")
    ax.grid(alpha=0.3, axis="y")

    # 自己親和性
    ax = fig.add_subplot(grid[0, 2])
    cmap = plt.get_cmap("viridis")
    for level in range(n_bins):
        ax.plot(r, rdf[1:, level, level], color=cmap(level / max(n_bins - 1, 1)), lw=1.2)
    ax.axhline(1.0, ls="--", c="gray", lw=1)
    ax.set_yscale("log")
    ax.set_xlabel("distance r [voxel = mm]")
    ax.set_ylabel(r"$g(\alpha,\alpha,r)$")
    ax.set_title("自己親和性（対数軸）。暗い色ほど低い濃度値")
    ax.grid(alpha=0.3)

    for col, (key, title, symmetric_scale) in enumerate(MATRICES[:3]):
        matrix = glam.get_matrix(key)
        ax = fig.add_subplot(grid[1, col])
        if symmetric_scale:
            im = ax.imshow(matrix, cmap="RdBu_r", norm=divergent(matrix),
                           interpolation="nearest")
        else:
            im = ax.imshow(matrix, cmap="viridis", interpolation="nearest")
        ax.set_xlabel(r"$\beta$"); ax.set_ylabel(r"$\alpha$")
        ax.set_title(title.replace("\n", " "), fontsize=10)
        fig.colorbar(im, ax=ax, fraction=0.046)

    fig.suptitle(f"IBSI CT radiomics phantom, GTV, {n_bins} bins, "
                 f"R={max_radius} voxel, 1 mm 等方", fontsize=13)
    fig.tight_layout(rect=[0, 0, 1, 0.96])
    path = os.path.join(OUT, "glam_ct_phantom.png")
    fig.savefig(path, dpi=110)
    plt.close(fig)
    return path


# --------------------------------------------------------------------------
# 図 4: 境界補正の有無で、ランダム参照状態と CDI がどう変わるか
# --------------------------------------------------------------------------
def figure_boundary_correction():
    """CDI は「観測とランダムの隔たり」で観測を割る量なので、ランダム状態を
    きっちり 1 に正規化してしまうと分母がほぼ 0 になり、値が 1 に張り付きます。
    その様子をそのまま見せる図です。"""
    image = blocks()
    mask = np.ones_like(image, dtype=np.float32)
    modes = [(True, "境界補正あり（既定・論文どおり）"),
             (False, "境界補正なし（参照実装と同じ理想球シェル）")]

    fig, axes = plt.subplots(2, 3, figsize=(15, 8.5))
    for row, (corrected, label) in enumerate(modes):
        glam = radiomicsj.GLAM(image, mask, n_bins=LEVELS, max_radius=MAX_RADIUS,
                               boundary_correction=corrected)
        r = np.arange(1, MAX_RADIUS + 1)
        structured = glam.get_rdf()
        random_state = glam.get_rdf(randomised=True)

        ax = axes[row, 0]
        ax.plot(r, structured[1:, 0, 0], marker="o", ms=3, label="観測 $g_{struct}$")
        ax.plot(r, random_state[1:, 0, 0], marker="s", ms=3, label="ランダム $g_{rand}$")
        ax.axhline(1.0, ls="--", c="gray", lw=1)
        ax.set_xlabel("distance r [voxel]"); ax.set_ylabel("g(0,0,r)")
        ax.set_title("動径分布関数"); ax.legend(fontsize=8); ax.grid(alpha=0.3)

        ax = axes[row, 1]
        ax.plot(r, np.log(np.maximum(structured[1:, 0, 0], 1e-9)),
                marker="o", ms=3, label=r"$\ln g_{struct}$")
        ax.plot(r, np.log(np.maximum(random_state[1:, 0, 0], 1e-9)),
                marker="s", ms=3, label=r"$\ln g_{rand}$")
        ax.axhline(0.0, ls="--", c="gray", lw=1)
        ax.set_xlabel("distance r [voxel]"); ax.set_ylabel("ln g")
        ax.set_title("CDI の分母は この 2 本の差"); ax.legend(fontsize=8); ax.grid(alpha=0.3)

        ax = axes[row, 2]
        cdi = glam.get_matrix("ConfigurationalDisorderIndex")
        im = ax.imshow(cdi, cmap="RdBu_r", norm=divergent(cdi), interpolation="nearest")
        for a in range(LEVELS):
            for b in range(LEVELS):
                ax.text(b, a, f"{cdi[a, b]:.3f}", ha="center", va="center", fontsize=8)
        ax.set_xticks(range(LEVELS)); ax.set_yticks(range(LEVELS))
        ax.set_xlabel(r"$\beta$"); ax.set_ylabel(r"$\alpha$")
        spread = float(np.nanmax(cdi) - np.nanmin(cdi))
        ax.set_title(f"CDI（振れ幅 {spread:.2f}）", fontsize=10)
        fig.colorbar(im, ax=ax, fraction=0.046)

        axes[row, 0].text(-0.28, 0.5, label, transform=axes[row, 0].transAxes,
                          rotation=90, va="center", ha="center", fontsize=11)

    fig.suptitle("境界補正を入れるとランダム状態は 1 に張り付き、CDI は情報を失います",
                 fontsize=13)
    fig.tight_layout(rect=[0.02, 0, 1, 0.96])
    path = os.path.join(OUT, "glam_boundary_correction.png")
    fig.savefig(path, dpi=110)
    plt.close(fig)
    return path


def main():
    os.makedirs(OUT, exist_ok=True)
    font = use_japanese_font()
    if font:
        print(f"日本語フォント: {font}")
    print("合成ファントムを解析します...")
    results = [glam_of(image) for _, image in PHANTOMS]
    print("  ", figure_phantoms_and_rdf(results))
    print("  ", figure_matrices(results))
    print("境界補正の効き方を比べます...")
    print("  ", figure_boundary_correction())
    print("実データ (IBSI CT radiomics phantom) を解析します...")
    print("  ", figure_real_data())


if __name__ == "__main__":
    main()
