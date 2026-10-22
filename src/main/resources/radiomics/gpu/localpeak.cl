// Mean of the voxels inside the sphere around every voxel of the region (single precision).
// dx, dy, dz are the offsets of the sphere, made on the host; a voxel outside the image is left out.
// Voxels outside the region get -INFINITY.
__kernel void localpeak(__global const float* img, __global const uchar* roi,
                        __global const int* dx, __global const int* dy, __global const int* dz, int noff,
                        int w, int h, int s, __global float* out)
{
    int x = get_global_id(0), y = get_global_id(1), z = get_global_id(2);
    if (x >= w || y >= h || z >= s) return;
    long idx = ((long)z * h + y) * w + x;
    if (!roi[idx]) { out[idx] = -INFINITY; return; }
    float sum = 0.0f;
    int n = 0;
    for (int i = 0; i < noff; i++) {
        int xx = x + dx[i], yy = y + dy[i], zz = z + dz[i];
        if (xx < 0 || xx >= w || yy < 0 || yy >= h || zz < 0 || zz >= s) continue;
        sum += img[((long)zz * h + yy) * w + xx];
        n++;
    }
    out[idx] = sum / n;
}
