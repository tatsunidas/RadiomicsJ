# HANDOFF

更新: 2026-10-21 22:23 / マシン: pop-os

## 0. 次にやること
- 下の「決めること」4 点にユーザーが答えたら、その順に進める（各機能は着手前にプランモードで計画を出して承認を取る）。
- 結果の一覧は `experimental/gpu/RESULTS.md`。レポート（非公開 Artifact）: https://claude.ai/artifact/Q7xo5EXUMqZddvgoWmthF5
- 試作の再実行（このマシン）: `mvn -o compile` のあと、`CP=<mvn dependency:build-classpath の出力> OCL_LIB=<libOpenCL.so を置いたディレクトリ> experimental/gpu/run.sh <64|128|real:001:CT> <繰り返し回数>`（`run.sh` 冒頭のコメント参照）。

決めること（私の推薦つき）:
1. NGTDM の CPU 並列版（近傍の合計・個数を並列に数え、行列は現行と同じ順序の倍精度演算で仕上げる）を本体に入れるか。現行の行列とビット単位で一致。推薦: 入れる。
2. NGTDM の GPU 版。実 CT では CPU 並列版より遅く、立方体で 1.1〜1.7 倍。推薦: 今は見送り、試作は資産として残す。
3. Gabor。推薦: まず実データで IBSI の許容誤差に収まるか確認 → 収まればカーネル最適化。GPU 上の FFT はその後。倍精度は 1.06〜1.2 倍しか速くならないので単精度前提。
4. Gabor の CPU/GPU 切り替えを、予想時間の見積もりによる自動にするか、利用者の指定にするか。

## 1. 現在の状態
- リポジトリ: `~/radiomicsj-workspace/RadiomicsJ`（RadiomicsJ 2.5.2）。ブランチ `gpu/phase1-benchmark`、commit `c9e3b7e` を origin に push 済み。PR・マージはしていない。未コミットの変更なし。
- ブランチの中身: `src/test/java/radiomics/{FamilyBenchmark,StageBenchmark}.java`（CPU の段階別計測。`mvn test` には入らない手動実行用）と `experimental/gpu/`（OpenCL 試作。Maven のビルドには入っていない。pom.xml は未変更）。
- 計測結果（STS_001 CT、198×202×59、ROI 11.7 万ボクセル）: Gabor は GPU 単精度が CPU の FFT より 1.7〜1.8 倍速い。立方体の合成データ（64³・128³）では GPU が遅い。NGTDM は現行 CPU の 8.9 秒が CPU 並列版で 0.02 秒、GPU 版は 0.026 秒。
- GPU 本体への組み込み（設定キー `BOOL_USE_GPU`、CLI の `--gpu`、Python 側の指定、フォールバック）は未着手。承認済みの計画の段階 4 以降に当たる。計画ファイル: `~/.claude/plans/npm-run-dev-desktop-streamed-pnueli.md`。

## 2. 決めたこと・理由
- バックエンドは OpenCL（JOCL 2.0.5）。Java 8 で動き、NVIDIA 以外にも広げられるため。CUDA と TornadoVM は採らない（NVIDIA 専用、JDK 21 以上が要る）。
- GPU の依存は任意にする（pom の既定スコープに入れない）。既定は CPU のまま、GPU は明示指定のみ。
- 「処理が軽くても、速くなるなら GPU を使う」（ユーザー指示 2026-10-21）。軽い処理を最初から対象外にせず、タスクごとの実測（転送時間込み）で決める。
- ユーザーは前回の 3 点（試作の対象・精度・実データ）を「推奨で進めて」と承認済み。推奨は、Gabor と NGTDM から始める、倍精度と単精度の両方で測る、実データで測り直す、だった。

## 3. 未解決・注意点
- 速度の値は、他の計測と CPU を奪い合うと汚れる。1 本ずつ単独で測ること。128³ と実データは繰り返し 1 回だけの測定。
- 未測定: Gabor 後の特徴値が IBSI の許容誤差に収まるか、他のファミリー、Intel 内蔵 GPU（OpenCL デバイスとして見えない）、GPU 上の FFT、カーネルの最適化。
- このマシンの eGPU（GTX 1050 Ti）は Thunderbolt 3 の 1 レーン（20 Gb/s）。転送の多い処理には不利。デスクトップの PCIe なら NGTDM の GPU は有利になるはずだが、推測。
- JOCL は `libOpenCL.so`（バージョン番号なしの名前）を探す。このマシンには `libOpenCL.so.1` しか無いので、ユーザー領域に symlink を置いて `LD_LIBRARY_PATH` で渡した。利用者の環境で無い場合は CPU に落とす設計にする。
- 既存コードの癖（直していない）: `NGTDMFeatures.fillNGTDM` は `Ngp` をリセットしないので、同じオブジェクトで 2 回呼ぶと加算される。
- GRAPHY-Next の `CLAUDE.md` は `.gitignore:76` で git 管理外。Linux では `npm run dev-desktop` が動かない（Windows 用の .bat を呼ぶ）ことを、README.md で補うか `package.json` を OS 判定付きにするかの選択がユーザーの回答待ち（推薦は README）。memory には起動手順を記録済み。
- GRAPHY-Next は未コミットの `automator/tsconfig.tsbuildinfo` の差分がある（私の作業の前からあるもの）。
