// Direct (spatial) convolution of one plane with the complex Gabor kernel, one work item per output voxel.
// Same arithmetic as the FFT path of ImageFiltering.gaborPlane: out[r][c] = sum K[i][j] * P[r+2rr-i][c+2rc-j],
// P being the padded plane (rowAt / colAt map a padded index to a source index, -1 is a zero).
#ifdef USE_FP64
#pragma OPENCL EXTENSION cl_khr_fp64 : enable
typedef double real;
#else
typedef float real;
#endif

__kernel void gabor(__global const float* src, __global const int* rowAt, __global const int* colAt,
                    int strideS, int strideR, int strideC, int rows, int cols, int rr, int rc, int kr, int kc,
                    __global const real* kre, __global const real* kim, __global float* dst)
{
    int c = get_global_id(0), r = get_global_id(1), s = get_global_id(2);
    if (c >= cols || r >= rows) return;
    real re = 0, im = 0;
    long base = (long)s * strideS;
    for (int i = 0; i < kr; i++) {
        int ra = rowAt[r + 2 * rr - i];
        if (ra < 0) continue;
        long rbase = base + (long)ra * strideR;
        for (int j = 0; j < kc; j++) {
            int ca = colAt[c + 2 * rc - j];
            if (ca < 0) continue;
            real p = (real)src[rbase + (long)ca * strideC];
            re += kre[i * kc + j] * p;
            im += kim[i * kc + j] * p;
        }
    }
    dst[((long)s * rows + r) * cols + c] = (float)hypot(re, im);
}
