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
 * Features of the GLAM family.
 *
 * Each entry pairs one gray level affinity matrix ({@link GLAMMatrixType}) with
 * one statistic taken over that matrix. Statistics ignore the elements that are
 * not defined, which happens when a gray level does not occur inside the roi.
 *
 * DiagonalMean summarises the self affinity of the gray levels and
 * OffDiagonalMean the affinity between different gray levels. They are not
 * reported for matrices that are only defined for self pairs.
 *
 * GLAM is a recent proposal and has no IBSI identifiers, so the feature name
 * itself is used as the identifier.
 *
 * Reference: Physics-Informed Multiscale Decoding of Tissue Microstructure,
 * The Gray Level Affinity Metrics (GLAM) Framework, Journal of Imaging
 * Informatics in Medicine (2026), doi 10.1007/s10278-026-02132-6
 *
 * @author tatsunidas <t_kobayashi@vis-ionary.com>
 *         (implemented with the coding assistant Claude, Anthropic)
 */
public enum GLAMFeatureType {

	RDFPeakPosition_Mean(GLAMMatrixType.RDFPeakPosition, GLAMStatistic.Mean),
	RDFPeakPosition_Variance(GLAMMatrixType.RDFPeakPosition, GLAMStatistic.Variance),
	RDFPeakPosition_Skewness(GLAMMatrixType.RDFPeakPosition, GLAMStatistic.Skewness),
	RDFPeakPosition_Kurtosis(GLAMMatrixType.RDFPeakPosition, GLAMStatistic.Kurtosis),
	RDFPeakPosition_Minimum(GLAMMatrixType.RDFPeakPosition, GLAMStatistic.Minimum),
	RDFPeakPosition_Maximum(GLAMMatrixType.RDFPeakPosition, GLAMStatistic.Maximum),
	RDFPeakPosition_DiagonalMean(GLAMMatrixType.RDFPeakPosition, GLAMStatistic.DiagonalMean),
	RDFPeakPosition_OffDiagonalMean(GLAMMatrixType.RDFPeakPosition, GLAMStatistic.OffDiagonalMean),
	RDFDispersionRatio_Mean(GLAMMatrixType.RDFDispersionRatio, GLAMStatistic.Mean),
	RDFDispersionRatio_Variance(GLAMMatrixType.RDFDispersionRatio, GLAMStatistic.Variance),
	RDFDispersionRatio_Skewness(GLAMMatrixType.RDFDispersionRatio, GLAMStatistic.Skewness),
	RDFDispersionRatio_Kurtosis(GLAMMatrixType.RDFDispersionRatio, GLAMStatistic.Kurtosis),
	RDFDispersionRatio_Minimum(GLAMMatrixType.RDFDispersionRatio, GLAMStatistic.Minimum),
	RDFDispersionRatio_Maximum(GLAMMatrixType.RDFDispersionRatio, GLAMStatistic.Maximum),
	RDFDispersionRatio_DiagonalMean(GLAMMatrixType.RDFDispersionRatio, GLAMStatistic.DiagonalMean),
	RDFDispersionRatio_OffDiagonalMean(GLAMMatrixType.RDFDispersionRatio, GLAMStatistic.OffDiagonalMean),
	LogRDFPeakHeight_Mean(GLAMMatrixType.LogRDFPeakHeight, GLAMStatistic.Mean),
	LogRDFPeakHeight_Variance(GLAMMatrixType.LogRDFPeakHeight, GLAMStatistic.Variance),
	LogRDFPeakHeight_Skewness(GLAMMatrixType.LogRDFPeakHeight, GLAMStatistic.Skewness),
	LogRDFPeakHeight_Kurtosis(GLAMMatrixType.LogRDFPeakHeight, GLAMStatistic.Kurtosis),
	LogRDFPeakHeight_Minimum(GLAMMatrixType.LogRDFPeakHeight, GLAMStatistic.Minimum),
	LogRDFPeakHeight_Maximum(GLAMMatrixType.LogRDFPeakHeight, GLAMStatistic.Maximum),
	LogRDFPeakHeight_DiagonalMean(GLAMMatrixType.LogRDFPeakHeight, GLAMStatistic.DiagonalMean),
	LogRDFPeakHeight_OffDiagonalMean(GLAMMatrixType.LogRDFPeakHeight, GLAMStatistic.OffDiagonalMean),
	LogRDFMedian_Mean(GLAMMatrixType.LogRDFMedian, GLAMStatistic.Mean),
	LogRDFMedian_Variance(GLAMMatrixType.LogRDFMedian, GLAMStatistic.Variance),
	LogRDFMedian_Skewness(GLAMMatrixType.LogRDFMedian, GLAMStatistic.Skewness),
	LogRDFMedian_Kurtosis(GLAMMatrixType.LogRDFMedian, GLAMStatistic.Kurtosis),
	LogRDFMedian_Minimum(GLAMMatrixType.LogRDFMedian, GLAMStatistic.Minimum),
	LogRDFMedian_Maximum(GLAMMatrixType.LogRDFMedian, GLAMStatistic.Maximum),
	LogRDFMedian_DiagonalMean(GLAMMatrixType.LogRDFMedian, GLAMStatistic.DiagonalMean),
	LogRDFMedian_OffDiagonalMean(GLAMMatrixType.LogRDFMedian, GLAMStatistic.OffDiagonalMean),
	LogRDFVariance_Mean(GLAMMatrixType.LogRDFVariance, GLAMStatistic.Mean),
	LogRDFVariance_Variance(GLAMMatrixType.LogRDFVariance, GLAMStatistic.Variance),
	LogRDFVariance_Skewness(GLAMMatrixType.LogRDFVariance, GLAMStatistic.Skewness),
	LogRDFVariance_Kurtosis(GLAMMatrixType.LogRDFVariance, GLAMStatistic.Kurtosis),
	LogRDFVariance_Minimum(GLAMMatrixType.LogRDFVariance, GLAMStatistic.Minimum),
	LogRDFVariance_Maximum(GLAMMatrixType.LogRDFVariance, GLAMStatistic.Maximum),
	LogRDFVariance_DiagonalMean(GLAMMatrixType.LogRDFVariance, GLAMStatistic.DiagonalMean),
	LogRDFVariance_OffDiagonalMean(GLAMMatrixType.LogRDFVariance, GLAMStatistic.OffDiagonalMean),
	LogRDFSkewness_Mean(GLAMMatrixType.LogRDFSkewness, GLAMStatistic.Mean),
	LogRDFSkewness_Variance(GLAMMatrixType.LogRDFSkewness, GLAMStatistic.Variance),
	LogRDFSkewness_Skewness(GLAMMatrixType.LogRDFSkewness, GLAMStatistic.Skewness),
	LogRDFSkewness_Kurtosis(GLAMMatrixType.LogRDFSkewness, GLAMStatistic.Kurtosis),
	LogRDFSkewness_Minimum(GLAMMatrixType.LogRDFSkewness, GLAMStatistic.Minimum),
	LogRDFSkewness_Maximum(GLAMMatrixType.LogRDFSkewness, GLAMStatistic.Maximum),
	LogRDFSkewness_DiagonalMean(GLAMMatrixType.LogRDFSkewness, GLAMStatistic.DiagonalMean),
	LogRDFSkewness_OffDiagonalMean(GLAMMatrixType.LogRDFSkewness, GLAMStatistic.OffDiagonalMean),
	LogRDFKurtosis_Mean(GLAMMatrixType.LogRDFKurtosis, GLAMStatistic.Mean),
	LogRDFKurtosis_Variance(GLAMMatrixType.LogRDFKurtosis, GLAMStatistic.Variance),
	LogRDFKurtosis_Skewness(GLAMMatrixType.LogRDFKurtosis, GLAMStatistic.Skewness),
	LogRDFKurtosis_Kurtosis(GLAMMatrixType.LogRDFKurtosis, GLAMStatistic.Kurtosis),
	LogRDFKurtosis_Minimum(GLAMMatrixType.LogRDFKurtosis, GLAMStatistic.Minimum),
	LogRDFKurtosis_Maximum(GLAMMatrixType.LogRDFKurtosis, GLAMStatistic.Maximum),
	LogRDFKurtosis_DiagonalMean(GLAMMatrixType.LogRDFKurtosis, GLAMStatistic.DiagonalMean),
	LogRDFKurtosis_OffDiagonalMean(GLAMMatrixType.LogRDFKurtosis, GLAMStatistic.OffDiagonalMean),
	SecondVirialCoefficient_Mean(GLAMMatrixType.SecondVirialCoefficient, GLAMStatistic.Mean),
	SecondVirialCoefficient_Variance(GLAMMatrixType.SecondVirialCoefficient, GLAMStatistic.Variance),
	SecondVirialCoefficient_Skewness(GLAMMatrixType.SecondVirialCoefficient, GLAMStatistic.Skewness),
	SecondVirialCoefficient_Kurtosis(GLAMMatrixType.SecondVirialCoefficient, GLAMStatistic.Kurtosis),
	SecondVirialCoefficient_Minimum(GLAMMatrixType.SecondVirialCoefficient, GLAMStatistic.Minimum),
	SecondVirialCoefficient_Maximum(GLAMMatrixType.SecondVirialCoefficient, GLAMStatistic.Maximum),
	SecondVirialCoefficient_DiagonalMean(GLAMMatrixType.SecondVirialCoefficient, GLAMStatistic.DiagonalMean),
	SecondVirialCoefficient_OffDiagonalMean(GLAMMatrixType.SecondVirialCoefficient, GLAMStatistic.OffDiagonalMean),
	PotentialEnergy_Mean(GLAMMatrixType.PotentialEnergy, GLAMStatistic.Mean),
	PotentialEnergy_Variance(GLAMMatrixType.PotentialEnergy, GLAMStatistic.Variance),
	PotentialEnergy_Skewness(GLAMMatrixType.PotentialEnergy, GLAMStatistic.Skewness),
	PotentialEnergy_Kurtosis(GLAMMatrixType.PotentialEnergy, GLAMStatistic.Kurtosis),
	PotentialEnergy_Minimum(GLAMMatrixType.PotentialEnergy, GLAMStatistic.Minimum),
	PotentialEnergy_Maximum(GLAMMatrixType.PotentialEnergy, GLAMStatistic.Maximum),
	PotentialEnergy_DiagonalMean(GLAMMatrixType.PotentialEnergy, GLAMStatistic.DiagonalMean),
	PotentialEnergy_OffDiagonalMean(GLAMMatrixType.PotentialEnergy, GLAMStatistic.OffDiagonalMean),
	Compressibility_Mean(GLAMMatrixType.Compressibility, GLAMStatistic.Mean),
	Compressibility_Variance(GLAMMatrixType.Compressibility, GLAMStatistic.Variance),
	Compressibility_Skewness(GLAMMatrixType.Compressibility, GLAMStatistic.Skewness),
	Compressibility_Kurtosis(GLAMMatrixType.Compressibility, GLAMStatistic.Kurtosis),
	Compressibility_Minimum(GLAMMatrixType.Compressibility, GLAMStatistic.Minimum),
	Compressibility_Maximum(GLAMMatrixType.Compressibility, GLAMStatistic.Maximum),
	CoordinationNumber_Mean(GLAMMatrixType.CoordinationNumber, GLAMStatistic.Mean),
	CoordinationNumber_Variance(GLAMMatrixType.CoordinationNumber, GLAMStatistic.Variance),
	CoordinationNumber_Skewness(GLAMMatrixType.CoordinationNumber, GLAMStatistic.Skewness),
	CoordinationNumber_Kurtosis(GLAMMatrixType.CoordinationNumber, GLAMStatistic.Kurtosis),
	CoordinationNumber_Minimum(GLAMMatrixType.CoordinationNumber, GLAMStatistic.Minimum),
	CoordinationNumber_Maximum(GLAMMatrixType.CoordinationNumber, GLAMStatistic.Maximum),
	CoordinationNumber_DiagonalMean(GLAMMatrixType.CoordinationNumber, GLAMStatistic.DiagonalMean),
	CoordinationNumber_OffDiagonalMean(GLAMMatrixType.CoordinationNumber, GLAMStatistic.OffDiagonalMean),
	InverseCorrelationLength_Mean(GLAMMatrixType.InverseCorrelationLength, GLAMStatistic.Mean),
	InverseCorrelationLength_Variance(GLAMMatrixType.InverseCorrelationLength, GLAMStatistic.Variance),
	InverseCorrelationLength_Skewness(GLAMMatrixType.InverseCorrelationLength, GLAMStatistic.Skewness),
	InverseCorrelationLength_Kurtosis(GLAMMatrixType.InverseCorrelationLength, GLAMStatistic.Kurtosis),
	InverseCorrelationLength_Minimum(GLAMMatrixType.InverseCorrelationLength, GLAMStatistic.Minimum),
	InverseCorrelationLength_Maximum(GLAMMatrixType.InverseCorrelationLength, GLAMStatistic.Maximum),
	InverseCorrelationLength_DiagonalMean(GLAMMatrixType.InverseCorrelationLength, GLAMStatistic.DiagonalMean),
	InverseCorrelationLength_OffDiagonalMean(GLAMMatrixType.InverseCorrelationLength, GLAMStatistic.OffDiagonalMean),
	StructuralPressureIndex_Mean(GLAMMatrixType.StructuralPressureIndex, GLAMStatistic.Mean),
	StructuralPressureIndex_Variance(GLAMMatrixType.StructuralPressureIndex, GLAMStatistic.Variance),
	StructuralPressureIndex_Skewness(GLAMMatrixType.StructuralPressureIndex, GLAMStatistic.Skewness),
	StructuralPressureIndex_Kurtosis(GLAMMatrixType.StructuralPressureIndex, GLAMStatistic.Kurtosis),
	StructuralPressureIndex_Minimum(GLAMMatrixType.StructuralPressureIndex, GLAMStatistic.Minimum),
	StructuralPressureIndex_Maximum(GLAMMatrixType.StructuralPressureIndex, GLAMStatistic.Maximum),
	StructuralPressureIndex_DiagonalMean(GLAMMatrixType.StructuralPressureIndex, GLAMStatistic.DiagonalMean),
	StructuralPressureIndex_OffDiagonalMean(GLAMMatrixType.StructuralPressureIndex, GLAMStatistic.OffDiagonalMean),
	ConfigurationalDisorderIndex_Mean(GLAMMatrixType.ConfigurationalDisorderIndex, GLAMStatistic.Mean),
	ConfigurationalDisorderIndex_Variance(GLAMMatrixType.ConfigurationalDisorderIndex, GLAMStatistic.Variance),
	ConfigurationalDisorderIndex_Skewness(GLAMMatrixType.ConfigurationalDisorderIndex, GLAMStatistic.Skewness),
	ConfigurationalDisorderIndex_Kurtosis(GLAMMatrixType.ConfigurationalDisorderIndex, GLAMStatistic.Kurtosis),
	ConfigurationalDisorderIndex_Minimum(GLAMMatrixType.ConfigurationalDisorderIndex, GLAMStatistic.Minimum),
	ConfigurationalDisorderIndex_Maximum(GLAMMatrixType.ConfigurationalDisorderIndex, GLAMStatistic.Maximum),
	ConfigurationalDisorderIndex_DiagonalMean(GLAMMatrixType.ConfigurationalDisorderIndex, GLAMStatistic.DiagonalMean),
	ConfigurationalDisorderIndex_OffDiagonalMean(GLAMMatrixType.ConfigurationalDisorderIndex, GLAMStatistic.OffDiagonalMean),
	WassersteinDistance_Mean(GLAMMatrixType.WassersteinDistance, GLAMStatistic.Mean),
	WassersteinDistance_Variance(GLAMMatrixType.WassersteinDistance, GLAMStatistic.Variance),
	WassersteinDistance_Skewness(GLAMMatrixType.WassersteinDistance, GLAMStatistic.Skewness),
	WassersteinDistance_Kurtosis(GLAMMatrixType.WassersteinDistance, GLAMStatistic.Kurtosis),
	WassersteinDistance_Minimum(GLAMMatrixType.WassersteinDistance, GLAMStatistic.Minimum),
	WassersteinDistance_Maximum(GLAMMatrixType.WassersteinDistance, GLAMStatistic.Maximum),
	WassersteinDistance_DiagonalMean(GLAMMatrixType.WassersteinDistance, GLAMStatistic.DiagonalMean),
	WassersteinDistance_OffDiagonalMean(GLAMMatrixType.WassersteinDistance, GLAMStatistic.OffDiagonalMean),
	AssemblyCoupling_Mean(GLAMMatrixType.AssemblyCoupling, GLAMStatistic.Mean),
	AssemblyCoupling_Variance(GLAMMatrixType.AssemblyCoupling, GLAMStatistic.Variance),
	AssemblyCoupling_Skewness(GLAMMatrixType.AssemblyCoupling, GLAMStatistic.Skewness),
	AssemblyCoupling_Kurtosis(GLAMMatrixType.AssemblyCoupling, GLAMStatistic.Kurtosis),
	AssemblyCoupling_Minimum(GLAMMatrixType.AssemblyCoupling, GLAMStatistic.Minimum),
	AssemblyCoupling_Maximum(GLAMMatrixType.AssemblyCoupling, GLAMStatistic.Maximum),
	AssemblyCoupling_DiagonalMean(GLAMMatrixType.AssemblyCoupling, GLAMStatistic.DiagonalMean),
	AssemblyCoupling_OffDiagonalMean(GLAMMatrixType.AssemblyCoupling, GLAMStatistic.OffDiagonalMean),
	PhenotypicDistance_Mean(GLAMMatrixType.PhenotypicDistance, GLAMStatistic.Mean),
	PhenotypicDistance_Variance(GLAMMatrixType.PhenotypicDistance, GLAMStatistic.Variance),
	PhenotypicDistance_Skewness(GLAMMatrixType.PhenotypicDistance, GLAMStatistic.Skewness),
	PhenotypicDistance_Kurtosis(GLAMMatrixType.PhenotypicDistance, GLAMStatistic.Kurtosis),
	PhenotypicDistance_Minimum(GLAMMatrixType.PhenotypicDistance, GLAMStatistic.Minimum),
	PhenotypicDistance_Maximum(GLAMMatrixType.PhenotypicDistance, GLAMStatistic.Maximum),
	PhenotypicDistance_DiagonalMean(GLAMMatrixType.PhenotypicDistance, GLAMStatistic.DiagonalMean),
	PhenotypicDistance_OffDiagonalMean(GLAMMatrixType.PhenotypicDistance, GLAMStatistic.OffDiagonalMean),
	LocalPackingFraction_Mean(GLAMMatrixType.LocalPackingFraction, GLAMStatistic.Mean),
	LocalPackingFraction_Variance(GLAMMatrixType.LocalPackingFraction, GLAMStatistic.Variance),
	LocalPackingFraction_Skewness(GLAMMatrixType.LocalPackingFraction, GLAMStatistic.Skewness),
	LocalPackingFraction_Kurtosis(GLAMMatrixType.LocalPackingFraction, GLAMStatistic.Kurtosis),
	LocalPackingFraction_Minimum(GLAMMatrixType.LocalPackingFraction, GLAMStatistic.Minimum),
	LocalPackingFraction_Maximum(GLAMMatrixType.LocalPackingFraction, GLAMStatistic.Maximum),
	LocalPackingFraction_DiagonalMean(GLAMMatrixType.LocalPackingFraction, GLAMStatistic.DiagonalMean),
	LocalPackingFraction_OffDiagonalMean(GLAMMatrixType.LocalPackingFraction, GLAMStatistic.OffDiagonalMean),
	FrustrationIndex_Mean(GLAMMatrixType.FrustrationIndex, GLAMStatistic.Mean),
	FrustrationIndex_Variance(GLAMMatrixType.FrustrationIndex, GLAMStatistic.Variance),
	FrustrationIndex_Skewness(GLAMMatrixType.FrustrationIndex, GLAMStatistic.Skewness),
	FrustrationIndex_Kurtosis(GLAMMatrixType.FrustrationIndex, GLAMStatistic.Kurtosis),
	FrustrationIndex_Minimum(GLAMMatrixType.FrustrationIndex, GLAMStatistic.Minimum),
	FrustrationIndex_Maximum(GLAMMatrixType.FrustrationIndex, GLAMStatistic.Maximum),
	FrustrationIndex_DiagonalMean(GLAMMatrixType.FrustrationIndex, GLAMStatistic.DiagonalMean),
	FrustrationIndex_OffDiagonalMean(GLAMMatrixType.FrustrationIndex, GLAMStatistic.OffDiagonalMean);

	private final GLAMMatrixType matrix;
	private final GLAMStatistic statistic;

	private GLAMFeatureType(GLAMMatrixType matrix, GLAMStatistic statistic) {
		this.matrix = matrix;
		this.statistic = statistic;
	}

	/**
	 * The gray level affinity matrix this feature summarises.
	 */
	public GLAMMatrixType matrix() {
		return matrix;
	}

	/**
	 * The statistic taken over that matrix.
	 */
	public GLAMStatistic statistic() {
		return statistic;
	}

	public String id() {
		return name();
	}

	public static String findType(String id) {
		for (GLAMFeatureType glam : GLAMFeatureType.values()) {
			if (glam.id().equals(id)) {
				return glam.name();
			}
		}
		return null;
	}
}
