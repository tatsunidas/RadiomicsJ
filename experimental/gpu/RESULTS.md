# GPU prototype results (RadiomicsJ 2.5.2, master b5dea01)

Machine: 8 CPU cores, Java 21.0.11, NVIDIA GeForce GTX 1050 Ti (4 GB, OpenCL 3.0, FP64 at 1/32 of FP32) on an eGPU
over Thunderbolt 3 (boltctl: 20 Gb/s, 1 lane). Each case was run alone, nothing else on the CPU. Seconds, median of the
repeats after one cold run (64: 3 repeats; 128 and real data: 1 repeat, so a single sample).
Real data: IBSI validation STS_001 CT, 198x202x59 voxels, 0.977 x 0.977 x 3.27 mm, ROI 117,145 voxels, 32 bins.
The Intel UHD 620 is not an OpenCL device here (no driver), so it was not measured.

## Gabor (sigma 5 mm, lambda 2 mm, gamma 1.5, MIRROR padding, 3D). GPU = direct convolution, not FFT.
| data | pooling | CPU FFT (double) | GPU fp32 | GPU fp64 | fp32 error (max abs, relative to max) |
|---|---|---|---|---|---|
| 64^3, 1 mm | none | 0.171 | 0.231 | 0.280 | 2.9e-4 (1.3e-6) |
| 64^3, 1 mm | MAX, 4 angles | 0.446 | 0.826 | 1.077 | 3.2e-4 (1.2e-6) |
| 128^3, 1 mm | none | 1.051 | 1.990 | 2.421 | 4.0e-4 (1.5e-6) |
| 128^3, 1 mm | MAX, 4 angles | 3.874 | 7.511 | 9.613 | 3.8e-4 (1.3e-6) |
| STS_001 CT | none | 1.908 | 1.037 | 1.573 | 4.1e-4 (1.8e-6) |
| STS_001 CT | MAX, 4 angles | 6.513 | 3.747 | 6.122 | 3.3e-3 (1.4e-6) |

The GPU wins on the real CT and loses on the cubes. The CPU FFT pads each plane to a power of two (a 198 x 202 plane with a
31 voxel margin becomes 512 x 512), which costs it on the real geometry; the direct sum has no such padding but costs
kernel size squared per voxel. Which side wins depends on the geometry, so a switch would have to be decided per input.
The kernel is not tuned (one work item per output voxel, global memory only); about 90% of the GPU time is the kernel.

## NGTDM (delta 1 and 2, 32 bins). Neighbour sums and counts on the GPU, matrix finished on the host.
Every GPU and parallel CPU result equals the current code bit for bit (Arrays.equals on the matrix).
| data | delta | current CPU code | CPU, parallel gather | GPU gather |
|---|---|---|---|---|
| 64^3 | 1 | 5.999 | 0.0047 | 0.0043 |
| 64^3 | 2 | 11.978 | 0.0066 | 0.0046 |
| 128^3 | 1 | 48.63 | 0.0348 | 0.0277 |
| 128^3 | 2 | 96.43 | 0.0526 | 0.0300 |
| STS_001 CT | 1 | 8.907 | 0.0193 | 0.0258 |
| STS_001 CT | 2 | 17.075 | 0.0122 | 0.0259 |

The current code loops over every grey level and every voxel, with an ArrayList of neighbours per voxel, so most of the
gain is the algorithm and not the GPU. Against the parallel CPU gather the GPU is 1.1x to 1.7x faster on the cubes and
slower on the real CT; there the upload and download (about 0.01 s each, bits of a 20 Gb/s link) are as long as the kernel.
Not fixed here: Ngp is not reset by fillNGTDM, so a second call on the same object adds to it.

## Not measured
Feature values after a GPU Gabor filter against the IBSI tolerances; other families; the Intel iGPU; a GPU FFT;
a tuned direct kernel; repeats for the 128^3 and real-data cases.

## CPU time per family on meerkat (段 1, 2026-10-22)
Machine: Intel Core Ultra 7 255H, 16 cores, Java 21.0.10, nothing else running on purpose (a Chrome and one JVM were idle in the background).
`FamilyBenchmark`, one family at a time, 32 bins, 3D. Seconds. Real data: STS_001 CT, ROI 117,145 voxels, **1 repeat** (a single sample).
Cube 64^3, 1 mm, ROI 83,056 voxels: median of 3 repeats after a warm up.
| family | STS_001 CT | 64^3 |
|---|---|---|
| Morphological | 121.457 | 30.391 |
| NGTDM | 79.895 | 6.014 |
| GLAM (voxels not isotropic on the CT, warned) | 7.440 | 2.163 |
| LocalIntensityFeatures | 1.798 | 3.493 |
| GLRLM | 2.142 | 0.258 |
| NGLDM | 0.749 | 0.269 |
| GLSZM | 0.477 | 0.076 |
| GLDZM | 0.280 | 0.098 |
| GLCM | 0.182 | 0.060 |
| IntensityHistogram | 0.053 | 0.042 |
| IntensityBasedStatistics | 0.043 | 0.028 |
| IntensityVolumeHistogram | 0.036 | 0.170 |
Not measured here: the filters (Gabor, Wavelet, LoG, Laws, Simoncelli) and Fractal; the cause of the Morphological time is not looked at yet.

### Filters on meerkat (段 1, 2026-10-22)
One filter pass, 3D, MIRROR padding. Seconds. Cubes: `StageBenchmark` (64^3: median of 3; 128^3: 1 repeat). Real CT: a scratch program, 2 runs, the second one is shown (1 sample).
| filter | STS_001 CT | 64^3 | 128^3 |
|---|---|---|---|
| Simoncelli level 1 | 5.657 | 0.997 | 5.939 |
| Gabor 5:2:1.5:0, MAX pooling | (see the Gabor table above, pop-os) | 0.179 | 1.533 |
| Laws L5E5E5, MAX | 0.515 | 0.094 | 0.290 |
| Wavelet db3 LLH level 1, MAX | 0.303 | 0.053 | 0.285 |
| Mean, support 5 | 0.021 | 0.003 | 0.036 |
| LoG sigma 1 / 3 mm | refused: the CT voxels are not isotropic | 0.010 / 0.011 | 0.050 / 0.077 |
Gabor was not run on the real CT on meerkat, so its number there is not known yet.
Fractal on the real CT (STS_001): **1.4 s** (2 runs, 1.625 s and 1.419 s; Fractal_Capacity 1.953) with every other family switched off explicitly. The 417 s / 594 s seen first came from my scratch program, which set only `BOOL_activate_no_default_features` and so left the other families on; that key only drops the "no default" features, it does not switch families off. Box counting itself is cheap: all nine box sizes together take under 0.5 s (size 2: 0.23 s, 6482 boxes).
Cube feature times (StageBenchmark, with preprocess included): LocalIntensityFeatures is 3.2 s at 64^3 and 25.1 s at 128^3; GLRLM 0.61 s and 4.06 s.

### Where the time goes (段 1, stack sampling of the main thread once per second, meerkat, 2026-10-22)
Sampled with jstack; the counts are small (16, 18, 81 samples), so read them as shares, not as exact times. Worker threads are not sampled.
| target | run | samples | where |
|---|---|---|---|
| Morphological, STS_001 CT | 87.9 s (one run) | 81 | 48 (59 %) in `MCTriangulator.processCube` line 112, in ImageJ `ByteProcessor.setMinAndMax` called through `stack.getProcessor(z).getf(x, y)` once per cube corner; 33 (41 %) in `MorphologicalFeatures.getMaximum3DDiameterByMesh`, an all-pairs loop over the mesh vertices (line 244 caller) |
| LocalIntensityFeatures, 128^3 | 19.8 s (one run) | 18 | 18 (100 %) in `getGlobalIntensityPeak` -> `getSphericalVoxels` (line 163): for every ROI voxel, a sphere of voxels is collected into an `ArrayList<Double>` and averaged |
| Simoncelli level 1, STS_001 CT | 4.1-4.5 s (4 runs) | 16 | 16 (100 %) in `ImageFiltering.fft` / `fftAny` / `fft3` (a CPU FFT) |
The Morphological run took 87.9 s here, against 121.5 s in the family table above (same data, same code); the two single runs differ by 30 %, which is not explained.

## 実装後の結果 (meerkat, 2026-10-22, master b46c36e)
GPU 版は master に入った（PR #38-#46、詳細は `CHANGELOG.md`）。GTX 1050 Ti (OpenCL)、STS_001 CT。
| 処理 | CPU | GPU | 備考 |
|---|---|---|---|
| Simoncelli level 1 (198x202x59, MIRROR) | 2.7-3.2 s | 0.18-0.30 s | 単精度、CPU との最大差は応答範囲の 2.7e-7。PERIODIC は CPU 2.7 s / GPU 0.08 s |
| 最大 3D 径 (頂点 22080) | 0.44 s（見積もり表の実測） | 未取得（本節では） | 値はビット一致 |
| Gabor (CT 規模) | 5.25 s | 4.39 s | 以前の記録 CPU 1.3 s / GPU 4.0 s は再現せず、原因不明 |
同じ CPU 条件で Simoncelli が 5.4-6.0 s と 2.7-3.2 s の 2 通りに出た（原因不明）。

見積もり（`GpuEstimator`）: 較正に使っていない 6 サイズで Simoncelli の速い側の判定は 6/6 一致、秒数は約 2 倍以内。CPU が勝つ例は見つかっていない。Gabor の CT 規模は外挿で、見積もりの CPU 秒は実測より低い。
