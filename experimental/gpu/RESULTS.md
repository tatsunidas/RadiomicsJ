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
