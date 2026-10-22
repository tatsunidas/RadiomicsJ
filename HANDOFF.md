# HANDOFF

更新: 2026-10-22 / マシン: meerkat（Core Ultra 7 255H 16 コア + GTX 1050 Ti）

## 0. 次にやること
- 新しい実装の指示は無い。再開するなら、下の「未解決」から選び、着手前にプランモードで計画を出して承認を取る。
- リリース（版数・タグ）は未実施。やるときは先にユーザーに確認する。
- このブランチ（`gpu/phase1-benchmark`）は試作と計測の記録として残す。コードは master に入っているので、PR・マージはしない。

## 1. 現在の状態
- master `b46c36e`。マージ済み PR（squash、CI 全件 pass、未リリース）:
  - #38 Marching cubes（メッシュ同一、60.8 s → 0.23 s）
  - #39 NGTDM 並列（値同一、60.6 s → 0.31 s）
  - #40 GPU 基盤（任意の OpenCL、`BOOL_USE_GPU`、CPU へ退避）
  - #41 Gabor GPU / #42 LocalIntensity ピーク GPU（ビット一致）/ #43 最大 3D 径（頂点の重複除去 + GPU、ビット一致）
  - #44 GpuEstimator（`--gpu-estimate`、`estimate_gpu`）/ #45 見積もり表の頂点数 / #46 Simoncelli GPU（FFT）と見積もり
- 「GPU が使えれば GPU、使えなければ CPU」が既定。CLI は `--gpu`/`--no-gpu`、Python は `use_gpu`。
- 前回の「決めること」の結論: 1 NGTDM CPU 並列は採用（#39）。2 NGTDM の GPU 版は見送り。3 Gabor は GPU 化（#41、単精度、許容内一致）。4 切り替えは既定で GPU、速さは見積もり機能で助言（#44）。
- 計測の詳細は `experimental/gpu/RESULTS.md` 末尾の「実装後の結果」。

## 2. 未解決・注意点
- `libOpenCL.so` が無い環境では GPU は使われず CPU になる。meerkat には `libOpenCL.so.1` しか無い。恒久対応は `sudo ln -s libOpenCL.so.1 /usr/lib/x86_64-linux-gnu/libOpenCL.so`（ユーザーが実行。未実施）。試験では別ディレクトリの symlink を `LD_LIBRARY_PATH` で渡した。
- 同じコード・同じ入力で CPU 時間が約 2 倍ぶれた例がある（Simoncelli CPU 5.4〜6.0 s → 2.7〜3.2 s）。原因は不明。Gabor の以前の記録（CPU 1.3 s）も再現していない。
- 見積もりの Gabor は CT 規模で外挿になり、CPU 秒が実測（5.25 s）より低く出る（2.35 s や 1.88 s）。判定（CPU が速い）は実測と合った。
- Simoncelli で CPU の方が速くなる例は見つかっていない（負例なし）。GPU 版は単精度で、CPU との差は応答範囲の 2.7e-7、ビット一致ではない。
- Python の `estimate_gpu` は jpype 1.7.1（スクラッチの venv、`target/classes` 使用）で実行し、4 タスクの表が返ることを確認した。pip の配布物での確認は未実施。
- 既存コードの癖（直していない）: `NGTDMFeatures.fillNGTDM` は `Ngp` をリセットしない。GlobalIntensityPeak の初期値は 0。
- GRAPHY-Next の `CLAUDE.md` は `.gitignore:76` で git 管理外。Linux では `npm run dev-desktop` が動かない（Windows 用の .bat を呼ぶ）ことを、README.md で補うか `package.json` を OS 判定付きにするかの選択がユーザーの回答待ち（推薦は README）。memory には起動手順を記録済み。
- GRAPHY-Next は未コミットの `automator/tsconfig.tsbuildinfo` の差分がある（私の作業の前からあるもの）。
