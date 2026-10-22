// For every point, the largest squared distance to any other point (single precision).
__kernel void maxdist(__global const float* xyz, int n, __global float* out)
{
    int i = get_global_id(0);
    if (i >= n) return;
    float x = xyz[3 * i], y = xyz[3 * i + 1], z = xyz[3 * i + 2];
    float best = 0.0f;
    for (int j = 0; j < n; j++) {
        float dx = x - xyz[3 * j], dy = y - xyz[3 * j + 1], dz = z - xyz[3 * j + 2];
        float d = dx * dx + dy * dy + dz * dz;
        if (d > best) best = d;
    }
    out[i] = best;
}
