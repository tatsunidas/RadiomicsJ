"""GLAM (Gray Level Affinity Metrics) のラッパーテスト。

同じヒストグラムを持つが空間的な並び方だけが違う 3 つのデジタルファントムを
作り、GLAM がその違いを読み取れることを確認します。

    pytest tests/test_glam.py -s
"""
import numpy as np
import radiomicsj


SIZE = 24
LEVELS = 4


def _equal_counts(rng=None):
    values = np.arange(SIZE ** 3) % LEVELS
    if rng is not None:
        rng.shuffle(values)
    return values.reshape(SIZE, SIZE, SIZE).astype(np.float32)


def salt_and_pepper():
    """濃度値をボクセル単位でばらまいた配置。"""
    return _equal_counts(np.random.default_rng(20260802))


def blocks():
    """同じヒストグラムを 6 ボクセル角のブロックにまとめた配置。"""
    block = 6
    per_axis = SIZE // block
    rng = np.random.default_rng(20260802)
    block_level = np.arange(per_axis ** 3) % LEVELS
    rng.shuffle(block_level)
    block_level = block_level.reshape(per_axis, per_axis, per_axis)
    return np.kron(block_level, np.ones((block, block, block))).astype(np.float32)


def onion():
    """同じヒストグラムを中心からの距離順に並べた同心球配置。"""
    c = (SIZE - 1) / 2.0
    z, y, x = np.mgrid[:SIZE, :SIZE, :SIZE]
    d = (z - c) ** 2 + (y - c) ** 2 + (x - c) ** 2
    order = np.argsort(d.ravel(), kind="stable")
    levels = np.empty(SIZE ** 3, dtype=np.float32)
    levels[order] = np.minimum(np.arange(SIZE ** 3) // (SIZE ** 3 // LEVELS), LEVELS - 1)
    return levels.reshape(SIZE, SIZE, SIZE)


def _glam(image):
    mask = np.ones_like(image, dtype=np.float32)
    return radiomicsj.GLAM(image, mask, spacing=(1.0, 1.0, 1.0),
                           n_bins=LEVELS, max_radius=12)


def test_glam_separates_arrangements_with_the_same_histogram():
    phantoms = {"salt and pepper": salt_and_pepper(), "blocks": blocks(), "onion": onion()}

    histograms = {name: np.bincount(img.astype(int).ravel(), minlength=LEVELS)
                  for name, img in phantoms.items()}
    reference = list(histograms.values())[0]
    for name, hist in histograms.items():
        assert np.array_equal(hist, reference), f"{name} のヒストグラムが揃っていません"
    print("\nヒストグラムはすべて同一:", reference.tolist())

    profiles = {}
    virials = {}
    for name, image in phantoms.items():
        glam = _glam(image)
        assert glam.n_bins == LEVELS
        assert glam.max_radius == 12

        rdf = glam.get_rdf()
        assert rdf.shape == (13, LEVELS, LEVELS)
        profiles[name] = rdf[1:, 0, 0]

        virial = glam.get_matrix(radiomicsj.GLAM.SecondVirialCoefficient)
        assert virial.shape == (LEVELS, LEVELS)
        # 濃度値 0 の自己親和性。負なら自分自身に引き合う（塊をつくる）。
        # 対角の平均は使いません。同心球では中心のコアが強く引き合う一方で
        # 外側の殻は自分自身から遠いので反発側に出るため、平均すると
        # 意味のある構造が打ち消されてしまいます。
        virials[name] = float(virial[0, 0])

        features = glam.get_all_features()
        assert len(features) == 150
        key = glam.feature_name(radiomicsj.GLAM.SecondVirialCoefficient,
                                radiomicsj.GLAM.DiagonalMean)
        assert key in features
        print(f"  {name:16s} B2(0,0) = {virials[name]:12.2f}"
              f"   g(0,0,r=1..4) = {np.round(profiles[name][:4], 3).tolist()}")

    # ばらまいた配置は、どの距離でも偶然と区別がつかない
    assert np.allclose(profiles["salt and pepper"], 1.0, atol=0.05)
    # まとまった配置は近距離で強く自己集合する
    assert profiles["blocks"][0] > 2.0
    assert profiles["onion"][0] > 2.0
    # 同心球は最も遠くまで相関が続く
    assert profiles["onion"][-1] > profiles["blocks"][-1]
    # 自己親和性の強さも同じ順序になる
    assert virials["onion"] < virials["blocks"] < virials["salt and pepper"]


def test_glam_randomised_state_is_reproducible():
    """既定の閉形式ランダム状態は決定的で、平坦な参照になります。"""
    image = blocks()
    first = _glam(image).get_rdf(randomised=True)
    second = _glam(image).get_rdf(randomised=True)
    assert np.array_equal(first, second)
    # 境界補正を入れた既定設定では、ランダム状態はほぼ 1 になります
    assert np.allclose(first[1:], 1.0, atol=0.01)


def test_glam_matrices_are_all_available():
    glam = _glam(blocks())
    matrices = glam.get_matrices()
    assert len(matrices) == 19
    for name, matrix in matrices.items():
        assert matrix.shape == (LEVELS, LEVELS), name
    # 表現型距離は真の距離なので、対角は 0 で対称
    pheno = matrices["PhenotypicDistance"]
    assert np.allclose(np.diag(pheno), 0.0)
    assert np.allclose(pheno, pheno.T)


if __name__ == "__main__":
    test_glam_separates_arrangements_with_the_same_histogram()
    test_glam_randomised_state_is_reproducible()
    test_glam_matrices_are_all_available()
    print("GLAM wrapper tests: ok")
