#ifdef USE_FP64
#pragma OPENCL EXTENSION cl_khr_fp64 : enable
typedef double R;
typedef double2 R2;
#else
typedef float R;
typedef float2 R2;
#endif

inline R2 cmul(R2 a, R2 b) { return (R2)(a.x * b.x - a.y * b.y, a.x * b.y + a.y * b.x); }

inline int bitrev(int i, int logm) {
	uint v = (uint) i;
	v = ((v >> 1) & 0x55555555u) | ((v & 0x55555555u) << 1);
	v = ((v >> 2) & 0x33333333u) | ((v & 0x33333333u) << 2);
	v = ((v >> 4) & 0x0F0F0F0Fu) | ((v & 0x0F0F0F0Fu) << 4);
	v = ((v >> 8) & 0x00FF00FFu) | ((v & 0x00FF00FFu) << 8);
	v = (v >> 16) | (v << 16);
	return (int) (v >> (32 - logm));
}

/* a holds the input in bit reversed order; leaves the transform in natural order. m / 2 work items. */
inline void fftLocal(__local R2 *a, __global const R2 *tw, int m, int lid) {
	for (int len = 2; len <= m; len <<= 1) {
		barrier(CLK_LOCAL_MEM_FENCE);
		int hl = len >> 1;
		int pos = lid & (hl - 1);
		int j = (lid / hl) * len + pos;
		R2 w = tw[pos * (m / len)];
		R2 u = a[j], v = cmul(a[j + hl], w);
		a[j] = u + v;
		a[j + hl] = u - v;
	}
	barrier(CLK_LOCAL_MEM_FENCE);
}

inline void permute(__local R2 *a, int m, int logm, int lid) {
	for (int i = lid; i < m; i += m / 2) {
		int j = bitrev(i, logm);
		if (i < j) { R2 t = a[i]; a[i] = a[j]; a[j] = t; }
	}
	barrier(CLK_LOCAL_MEM_FENCE);
}

/* One work group per line along one axis. Lines are numbered so that line = high * stride + low. */
__kernel void fftLines(__global R2 *data, __global const R2 *tw, __global const R2 *chirp, __global const R2 *bf,
		__local R2 *a, int n, int m, int logm, int stride, int inverse) {
	int line = get_group_id(0);
	int lid = get_local_id(0);
	int low = line % stride, high = line / stride;
	long start = (long) high * stride * n + low;
	R sgn = inverse ? (R) -1 : (R) 1;
	if (m == n) {
		for (int i = lid; i < n; i += m / 2) {
			R2 x = data[start + (long) i * stride];
			x.y *= sgn;
			a[bitrev(i, logm)] = x;
		}
		barrier(CLK_LOCAL_MEM_FENCE);
		fftLocal(a, tw, m, lid);
		R s = inverse ? (R) 1 / n : (R) 1;
		for (int i = lid; i < n; i += m / 2) {
			R2 x = a[i];
			x.y *= sgn;
			data[start + (long) i * stride] = x * s;
		}
	} else {
		for (int i = lid; i < m; i += m / 2) {
			R2 x = (R2)(0, 0);
			if (i < n) {
				x = data[start + (long) i * stride];
				x.y *= sgn;
				x = cmul(x, chirp[i]);
			}
			a[bitrev(i, logm)] = x;
		}
		barrier(CLK_LOCAL_MEM_FENCE);
		fftLocal(a, tw, m, lid);
		for (int i = lid; i < m; i += m / 2) {
			R2 x = cmul(a[i], bf[i]);
			x.y = -x.y;
			a[i] = x;
		}
		barrier(CLK_LOCAL_MEM_FENCE);
		permute(a, m, logm, lid);
		fftLocal(a, tw, m, lid);
		R s = inverse ? (R) 1 / n : (R) 1;
		for (int i = lid; i < n; i += m / 2) {
			R2 x = a[i];
			x.y = -x.y / m;   /* conj of the inverse transform, 1 / m */
			x.x = x.x / m;
			x = cmul(x, chirp[i]);
			x.y *= sgn;
			data[start + (long) i * stride] = x * s;
		}
	}
}

/* data *= g(nu), nu from the three per-axis squared frequencies */
__kernel void bandpass(__global R2 *data, __global const R *n0, __global const R *n1, __global const R *n2,
		int e0, int e1, R vb) {
	int x = get_global_id(0), y = get_global_id(1), z = get_global_id(2);
	R nu = sqrt(n0[x] + n1[y] + n2[z]);
	R g = (nu >= vb / 4 && nu <= vb) ? cos((R) 1.5707963267948966 * log2(2 * nu / vb)) : (R) 0;
	long i = ((long) z * e1 + y) * e0 + x;
	data[i] = data[i] * g;
}
