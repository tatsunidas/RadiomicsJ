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

/**
 * The gray level affinity matrices of the GLAM family.
 *
 * Every entry is a nBins x nBins matrix whose element (alpha, beta) describes
 * the spatial relation between the gray levels alpha and beta, derived from the
 * radial distribution function g(alpha, beta, r). They play the same role that
 * the co-occurrence matrix plays for the GLCM family: the matrix itself is the
 * descriptor, and the reported features are statistics of that matrix.
 *
 * Reference: Physics-Informed Multiscale Decoding of Tissue Microstructure,
 * The Gray Level Affinity Metrics (GLAM) Framework, Journal of Imaging
 * Informatics in Medicine (2026), doi 10.1007/s10278-026-02132-6
 *
 * @author tatsunidas <t_kobayashi@vis-ionary.com>
 *         (implemented with the coding assistant Claude, Anthropic)
 */
public enum GLAMMatrixType {

	/** Distance at which g(r) reaches its maximum. */
	RDFPeakPosition(false),
	/** Variance over mean of g(r), a measure of how spread out the curve is. */
	RDFDispersionRatio(false),
	/** Maximum of log(1 + g(r)). */
	LogRDFPeakHeight(false),
	/** Median of log(1 + g(r)) over the range where the roi still contributes. */
	LogRDFMedian(false),
	/** Variance of log(1 + g(r)). */
	LogRDFVariance(false),
	/** Skewness of log(1 + g(r)). */
	LogRDFSkewness(false),
	/** Kurtosis of log(1 + g(r)). */
	LogRDFKurtosis(false),
	/** Net affinity between two gray levels, negative means attraction. */
	SecondVirialCoefficient(false),
	/** Integrated potential of mean force. */
	PotentialEnergy(false),
	/** Susceptibility of the texture to density fluctuations, self pairs only. */
	Compressibility(true),
	/** Number of neighbours inside the first coordination shell. */
	CoordinationNumber(false),
	/** Decay rate of the spatial correlation, the inverse correlation length. */
	InverseCorrelationLength(false),
	/** Pressure like descriptor of the mean force between voxel populations. */
	StructuralPressureIndex(false),
	/**
	 * Structural disorder relative to a randomised arrangement.
	 *
	 * Read this one with care. It is defined as
	 * <pre>
	 *   CDI(r) = ln g_structured(r) / (ln g_structured(r) - ln g_random(r))
	 * </pre>
	 * so it divides the observed ordering by how far the observed and the
	 * randomised state differ. Boundary correction pins the randomised state to
	 * one, which drives the second term of the denominator to zero and the whole
	 * ratio to one: on a blocky phantom the spread over the matrix falls from
	 * 1.12 without the correction to 0.17 with it. Where the arrangement really
	 * is random, both logarithms approach zero and the ratio becomes numerically
	 * unstable instead.
	 *
	 * In other words the variation of this index comes from the geometry of the
	 * roi rather than from the tissue. Turn boundary correction off
	 * (BOOL_GLAM_boundaryCorrection=0) when it, or {@link #FrustrationIndex}
	 * which divides by it, matters to the analysis.
	 */
	ConfigurationalDisorderIndex(false),
	/** Assembly cost, the transport distance to the randomised arrangement. */
	WassersteinDistance(false),
	/** Mixed derivative of the assembly cost, the thermodynamic entanglement. */
	AssemblyCoupling(false),
	/** Transport distance between the spatial topologies of two gray levels. */
	PhenotypicDistance(false),
	/** Volume fraction filled inside the first coordination shell. */
	LocalPackingFraction(false),
	/**
	 * Structural stress over structural disorder, the unjamming condition.
	 * It divides by {@link #ConfigurationalDisorderIndex}, so it inherits that
	 * index's dependence on the boundary correction.
	 */
	FrustrationIndex(false);

	private final boolean diagonalOnly;

	private GLAMMatrixType(boolean diagonalOnly) {
		this.diagonalOnly = diagonalOnly;
	}

	/**
	 * True when the matrix is only defined for self pairs, so that the off
	 * diagonal statistics are meaningless and are not reported.
	 */
	public boolean isDiagonalOnly() {
		return diagonalOnly;
	}
}
