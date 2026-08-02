/*
 * Copyright [2026] [Tatsuaki Kobayashi]

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package io.github.tatsunidas.radiomics.features;

import org.apache.commons.math3.fitting.leastsquares.LeastSquaresBuilder;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;
import org.apache.commons.math3.fitting.leastsquares.MultivariateJacobianFunction;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.util.Pair;

/**
 * Numerical helpers behind {@link GLAMFeatures}.
 *
 * The GLAM descriptors are defined in terms of a handful of signal processing
 * primitives (trapezoidal integration, Savitzky-Golay smoothing, peak detection,
 * finite difference gradients and an exponential decay fit). The reference
 * implementation of the framework expresses them with numpy and scipy, so the
 * routines here follow the very same conventions, which keeps the Java values
 * comparable to the published ones.
 *
 * @author tatsunidas <t_kobayashi@vis-ionary.com>
 *         (implemented with the coding assistant Claude, Anthropic)
 */
class GLAMNumerics {

	private GLAMNumerics() {
	}

	/**
	 * Smoothing kernels only depend on the window and the polynomial order, and the
	 * same pair is asked for once per gray level curve, so they are kept.
	 */
	private static final java.util.concurrent.ConcurrentHashMap<Long, double[]> KERNEL_CACHE =
			new java.util.concurrent.ConcurrentHashMap<>();

	/**
	 * Trapezoidal integral over a uniform grid of unit spacing.
	 */
	static double trapezoid(double[] y) {
		if (y == null || y.length < 2) {
			return 0d;
		}
		double sum = 0d;
		for (int i = 0; i < y.length - 1; i++) {
			sum += (y[i] + y[i + 1]) * 0.5;
		}
		return sum;
	}

	/**
	 * Trapezoidal integral over an explicit, possibly non uniform, abscissa.
	 */
	static double trapezoid(double[] y, double[] x) {
		if (y == null || y.length < 2) {
			return 0d;
		}
		double sum = 0d;
		for (int i = 0; i < y.length - 1; i++) {
			sum += (y[i] + y[i + 1]) * 0.5 * (x[i + 1] - x[i]);
		}
		return sum;
	}

	/**
	 * Cumulative trapezoidal integral over a uniform grid of unit spacing.
	 * The first element is zero, so the result has the same length as the input.
	 */
	static double[] cumulativeTrapezoid(double[] y) {
		double[] out = new double[y.length];
		for (int i = 1; i < y.length; i++) {
			out[i] = out[i - 1] + (y[i - 1] + y[i]) * 0.5;
		}
		return out;
	}

	/**
	 * Central difference gradient over a uniform grid of unit spacing, with
	 * second order accurate one sided differences at both ends.
	 */
	static double[] gradient(double[] y) {
		int n = y.length;
		double[] out = new double[n];
		if (n == 0) {
			return out;
		}
		if (n == 1) {
			return out;
		}
		if (n == 2) {
			out[0] = y[1] - y[0];
			out[1] = out[0];
			return out;
		}
		for (int i = 1; i < n - 1; i++) {
			out[i] = (y[i + 1] - y[i - 1]) * 0.5;
		}
		out[0] = (-3d * y[0] + 4d * y[1] - y[2]) * 0.5;
		out[n - 1] = (3d * y[n - 1] - 4d * y[n - 2] + y[n - 3]) * 0.5;
		return out;
	}

	/**
	 * Smoothing kernel of a Savitzky-Golay filter, i.e. the weights that a least
	 * squares polynomial fit of the given order assigns to the centre sample of
	 * the window.
	 *
	 * @param window      number of samples in the window, must be odd
	 * @param polynomial  order of the fitted polynomial, must be below the window
	 */
	static double[] savitzkyGolayKernel(int window, int polynomial) {
		Long key = ((long) window << 32) | (polynomial & 0xffffffffL);
		double[] cached = KERNEL_CACHE.get(key);
		if (cached != null) {
			return cached;
		}
		double[] kernel = buildSavitzkyGolayKernel(window, polynomial);
		KERNEL_CACHE.put(key, kernel);
		return kernel;
	}

	private static double[] buildSavitzkyGolayKernel(int window, int polynomial) {
		if (window % 2 == 0 || window < 1) {
			throw new IllegalArgumentException("Savitzky-Golay window must be a positive odd number.");
		}
		if (polynomial >= window) {
			throw new IllegalArgumentException("Savitzky-Golay polynomial order must be smaller than the window.");
		}
		int half = window / 2;
		double[][] design = new double[window][polynomial + 1];
		for (int i = 0; i < window; i++) {
			double t = i - half;
			double p = 1d;
			for (int j = 0; j <= polynomial; j++) {
				design[i][j] = p;
				p *= t;
			}
		}
		RealMatrix a = new Array2DRowRealMatrix(design, false);
		RealMatrix ata = a.transpose().multiply(a);
		DecompositionSolver solver = new LUDecomposition(ata).getSolver();
		// the smoothed centre value is the constant term of the fit, so we only
		// need the first row of (A'A)^-1 A'
		RealMatrix inverse = solver.getInverse();
		RealMatrix full = inverse.multiply(a.transpose());
		double[] kernel = new double[window];
		for (int i = 0; i < window; i++) {
			kernel[i] = full.getEntry(0, i);
		}
		return kernel;
	}

	/**
	 * Savitzky-Golay smoothing with zeros assumed outside the signal, which is the
	 * convention the GLAM reference implementation uses.
	 */
	static double[] savitzkyGolay(double[] y, int window, int polynomial) {
		if (y.length <= window) {
			return y.clone();
		}
		double[] kernel = savitzkyGolayKernel(window, polynomial);
		int half = window / 2;
		double[] out = new double[y.length];
		for (int i = 0; i < y.length; i++) {
			double sum = 0d;
			for (int k = 0; k < window; k++) {
				int idx = i + half - k;
				if (idx >= 0 && idx < y.length) {
					sum += kernel[k] * y[idx];
				}
			}
			out[i] = sum;
		}
		return out;
	}

	/**
	 * Indices of the local maxima of a signal. A plateau counts as a single
	 * maximum, reported at the middle of the plateau.
	 */
	static int[] localMaxima(double[] y) {
		int n = y.length;
		int[] peaks = new int[n / 2 + 1];
		int found = 0;
		int i = 1;
		int last = n - 1;
		while (i < last) {
			if (y[i - 1] < y[i]) {
				int ahead = i + 1;
				while (ahead < last && y[ahead] == y[i]) {
					ahead++;
				}
				if (y[ahead] < y[i]) {
					peaks[found++] = (i + ahead - 1) / 2;
					i = ahead;
				}
			}
			i++;
		}
		int[] out = new int[found];
		System.arraycopy(peaks, 0, out, 0, found);
		return out;
	}

	/**
	 * Topographic prominence of a peak: the height of the peak above the highest
	 * of the two lowest contours that separate it from any higher neighbour.
	 */
	static double prominence(double[] y, int peak) {
		double leftMin = y[peak];
		int left = peak;
		while (left >= 0 && y[left] <= y[peak]) {
			if (y[left] < leftMin) {
				leftMin = y[left];
			}
			left--;
		}
		double rightMin = y[peak];
		int right = peak;
		while (right < y.length && y[right] <= y[peak]) {
			if (y[right] < rightMin) {
				rightMin = y[right];
			}
			right++;
		}
		return y[peak] - Math.max(leftMin, rightMin);
	}

	/**
	 * Local maxima whose prominence reaches the given threshold.
	 */
	static int[] findPeaks(double[] y, double minProminence) {
		int[] candidates = localMaxima(y);
		int[] kept = new int[candidates.length];
		int found = 0;
		for (int p : candidates) {
			if (prominence(y, p) >= minProminence) {
				kept[found++] = p;
			}
		}
		int[] out = new int[found];
		System.arraycopy(kept, 0, out, 0, found);
		return out;
	}

	static double mean(double[] v) {
		if (v.length == 0) {
			return Double.NaN;
		}
		double s = 0d;
		for (double d : v) {
			s += d;
		}
		return s / v.length;
	}

	/**
	 * Population variance, the estimator numpy and scipy use by default.
	 */
	static double variance(double[] v) {
		if (v.length == 0) {
			return Double.NaN;
		}
		double m = mean(v);
		double s = 0d;
		for (double d : v) {
			double e = d - m;
			s += e * e;
		}
		return s / v.length;
	}

	/**
	 * Fisher-Pearson skewness without the sample size correction.
	 */
	static double skewness(double[] v) {
		if (v.length == 0) {
			return Double.NaN;
		}
		double m = mean(v);
		double m2 = 0d;
		double m3 = 0d;
		for (double d : v) {
			double e = d - m;
			m2 += e * e;
			m3 += e * e * e;
		}
		m2 /= v.length;
		m3 /= v.length;
		if (m2 <= 0d) {
			return 0d;
		}
		return m3 / Math.pow(m2, 1.5);
	}

	/**
	 * Excess kurtosis without the sample size correction.
	 */
	static double kurtosis(double[] v) {
		if (v.length == 0) {
			return Double.NaN;
		}
		double m = mean(v);
		double m2 = 0d;
		double m4 = 0d;
		for (double d : v) {
			double e = d - m;
			double e2 = e * e;
			m2 += e2;
			m4 += e2 * e2;
		}
		m2 /= v.length;
		m4 /= v.length;
		if (m2 <= 0d) {
			return -3d;
		}
		return m4 / (m2 * m2) - 3d;
	}

	static double median(double[] v) {
		if (v.length == 0) {
			return Double.NaN;
		}
		double[] sorted = v.clone();
		java.util.Arrays.sort(sorted);
		int n = sorted.length;
		if (n % 2 == 1) {
			return sorted[n / 2];
		}
		return (sorted[n / 2 - 1] + sorted[n / 2]) * 0.5;
	}

	/**
	 * Index of the largest element, first occurrence wins.
	 */
	static int argMax(double[] v, int from, int to) {
		int best = from;
		for (int i = from + 1; i < to; i++) {
			if (v[i] > v[best]) {
				best = i;
			}
		}
		return best;
	}

	/**
	 * Least squares fit of A * exp(-decayRate * x), returning the decay rate.
	 * Both parameters are constrained to be non negative, as in the reference
	 * implementation. Returns NaN when the fit does not converge.
	 */
	static double fitExponentialDecayRate(double[] x, double[] y, double amplitudeGuess, double decayGuess) {
		if (x.length < 3) {
			return Double.NaN;
		}
		final double[] xs = x;
		MultivariateJacobianFunction model = new MultivariateJacobianFunction() {
			@Override
			public Pair<RealVector, RealMatrix> value(RealVector point) {
				double a = point.getEntry(0);
				double k = point.getEntry(1);
				double[] values = new double[xs.length];
				double[][] jacobian = new double[xs.length][2];
				for (int i = 0; i < xs.length; i++) {
					double e = Math.exp(-k * xs[i]);
					values[i] = a * e;
					jacobian[i][0] = e;
					jacobian[i][1] = -a * xs[i] * e;
				}
				return new Pair<RealVector, RealMatrix>(new ArrayRealVector(values, false),
						new Array2DRowRealMatrix(jacobian, false));
			}
		};
		try {
			LeastSquaresOptimizer.Optimum optimum = new LevenbergMarquardtOptimizer().optimize(new LeastSquaresBuilder()
					.start(new double[] { Math.max(amplitudeGuess, 1e-12), decayGuess })
					.model(model)
					.target(y)
					.weight(MatrixUtils.createRealIdentityMatrix(y.length))
					.parameterValidator(point -> {
						// keep both parameters inside the non negative half space
						RealVector clamped = point.copy();
						clamped.setEntry(0, Math.max(0d, clamped.getEntry(0)));
						clamped.setEntry(1, Math.max(0d, clamped.getEntry(1)));
						return clamped;
					})
					.maxEvaluations(1000)
					.maxIterations(1000)
					.build());
			return optimum.getPoint().getEntry(1);
		} catch (Exception e) {
			return Double.NaN;
		}
	}
}
