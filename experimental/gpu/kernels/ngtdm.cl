// For every voxel that can be the centre of an NGTDM entry, the sum and the count of its valid neighbours.
// Integer arithmetic only, so the host finishes the matrix with the same double operations as the CPU code.
__kernel void ngtdm_gather(__global const float* val, __global const uchar* valid, __global const uchar* center,
                           int w, int h, int s, __global const int* off, int n,
                           __global int* sum, __global int* cnt)
{
    int x = get_global_id(0), y = get_global_id(1), z = get_global_id(2);
    if (x >= w || y >= h || z >= s) return;
    long idx = x + (long)w * (y + (long)h * z);
    int sm = 0, c = 0;
    if (center[idx]) {
        for (int k = 0; k < n; k++) {
            int nx = x + off[3 * k], ny = y + off[3 * k + 1], nz = z + off[3 * k + 2];
            if (nx < 0 || nx >= w || ny < 0 || ny >= h || nz < 0 || nz >= s) continue;
            long j = nx + (long)w * (ny + (long)h * nz);
            if (valid[j]) { sm += (int)val[j]; c++; }
        }
    }
    sum[idx] = sm;
    cnt[idx] = c;
}
