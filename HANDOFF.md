# HANDOFF

更新: 2026-10-25 / マシン: pop-os（Maven Central・PyPI の配布を実施。GPU 無し）。前回は meerkat（Core Ultra 7 255H 16 コア + GTX 1050 Ti）

## 0. 次にやること
- 再開したら: `git fetch && git log --oneline origin/master -3` で master が `f228ae9` から進んでいないか確認する。この HANDOFF.md は master には無く、本ブランチにだけある。
- 新しい実装の指示は無い。候補: 見積もりの安定化の調査、実 DICOM での CLI 通常モード確認。再開するなら、下の「未解決」から選び、着手前にプランモードで計画を出して承認を取る。
- **v2.6.0 は公開済み**（2026-10-25、タグ `v2.6.0`、jar 3 点）。**Maven Central（`mvn clean deploy -DskipTests -Dexec.skip=true`、Central ポータルで手動 Publish）と PyPI（`target/radiomicsj-python` で build → twine）にも 2026-10-25 に 2.6.0 を公開済み**。配布前に wheel を新規 venv に入れて `smoke/installed_smoke.py` が通ることを確認した。`mvn deploy`/`install` は `conda build` を呼ぶので、conda の無い機では `-Dexec.skip=true` が要る。次のリリースは、先にユーザーに確認する。
- このブランチ（`gpu/phase1-benchmark`）は試作と計測の記録として残す。コードは master に入っているので、PR・マージはしない。

## 1. 現在の状態
- master `f228ae9`。**v2.6.0 公開済み**（https://github.com/tatsunidas/RadiomicsJ/releases/tag/v2.6.0、Maven Central・PyPI にも 2.6.0 公開済み）。マージ済み PR（squash、CI 全件 pass）:
  - #38 Marching cubes（メッシュ同一、60.8 s → 0.23 s）
  - #39 NGTDM 並列（値同一、60.6 s → 0.31 s）
  - #40 GPU 基盤（任意の OpenCL、`BOOL_USE_GPU`、CPU へ退避）
  - #41 Gabor GPU / #42 LocalIntensity ピーク GPU（ビット一致）/ #43 最大 3D 径（頂点の重複除去 + GPU、ビット一致）
  - #44 GpuEstimator（`--gpu-estimate`、`estimate_gpu`）/ #45 見積もり表の頂点数 / #46 Simoncelli GPU（FFT）と見積もり
  - #47 ピーク初期値 0 → −∞（負の最大平均を 0 にしない）/ #48 リリース 2.6.0 と README の GPU 節 / #49 CLI: `-o` 指定時は結果ウィンドウを出さず終了（表示のある環境で JVM が終了しなかった。v2.5.2 以前から）
- 「GPU が使えれば GPU、使えなければ CPU」が既定。CLI は `--gpu`/`--no-gpu`、Python は `use_gpu`。
- 前回の「決めること」の結論: 1 NGTDM CPU 並列は採用（#39）。2 NGTDM の GPU 版は見送り。3 Gabor は GPU 化（#41、単精度、許容内一致）。4 切り替えは既定で GPU、速さは見積もり機能で助言（#44）。
- 計測の詳細は `experimental/gpu/RESULTS.md` 末尾の「実装後の結果」。

## 2. 未解決・注意点
- `libOpenCL.so` は meerkat で symlink 済み（2026-10-24、ユーザーが sudo で実行）。`LD_LIBRARY_PATH` なしで GPU を使うことを確認した。他のマシンで GPU を使うには、同名のリンクが要る場合がある（JOCL は番号なしの名前を探す）。無ければ CPU で動く。
- 同じコード・同じ入力で CPU 時間が約 2 倍ぶれた例がある（Simoncelli CPU 5.4〜6.0 s → 2.7〜3.2 s）。原因は不明。Gabor の以前の記録（CPU 1.3 s）も再現していない。
- 見積もりの Gabor は CT 規模で外挿になり、CPU 秒が実測（5.25 s）より低く出る（2.35 s や 1.88 s）。判定（CPU が速い）は実測と合った。
- Simoncelli で CPU の方が速くなる例は見つかっていない（負例なし）。GPU 版は単精度で、CPU との差は応答範囲の 2.7e-7、ビット一致ではない。
- Python の `estimate_gpu` は jpype 1.7.1（スクラッチの venv、`target/classes` 使用）で実行し、4 タスクの表が返ることを確認した。pip の配布物での確認は未実施。
- 見積もり（`--gpu-estimate`）の CPU 秒は較正のたびに大きく動く（CT 規模の LOCAL_PEAK が 1.26 s → 5.58 s、Gabor が 2.35 s → 1.03 s）。版が変わるとキャッシュが作り直されるが、原因は未確認。判定（CPU/GPU）は変わらなかった。CHANGELOG には載せていない（ユーザー判断）。
- CLI の `-o` 無しでは従来どおり結果ウィンドウが開き、表示のある環境では JVM が残る（仕様として残した）。`main` は `System.exit` を呼ぶのでテストにできず、修正は手動で確認した（通常モードは、ファントムを tif に書き出した入力。実 DICOM のフォルダでは未実行）。
- Windows / macOS の GPU 実機は未確認（CI は CPU 経路）。公開物の確認は、Release の jar で `-t -tdt 0 -o` と `--gpu-estimate` を実行した（Linux、GTX 1050 Ti）。
- 既存コードの癖（直していない）: `NGTDMFeatures.fillNGTDM` は `Ngp` をリセットしない。
- GRAPHY-Next の `CLAUDE.md` は `.gitignore:76` で git 管理外。Linux では `npm run dev-desktop` が動かない（Windows 用の .bat を呼ぶ）ことを、README.md で補うか `package.json` を OS 判定付きにするかの選択がユーザーの回答待ち（推薦は README）。memory には起動手順を記録済み。
- GRAPHY-Next は未コミットの `automator/tsconfig.tsbuildinfo` の差分がある（私の作業の前からあるもの）。
