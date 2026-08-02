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
 * Statistics that reduce a gray level affinity matrix to a single number.
 *
 * @author tatsunidas <t_kobayashi@vis-ionary.com>
 *         (implemented with the coding assistant Claude, Anthropic)
 */
public enum GLAMStatistic {

	/** Mean over every defined element of the matrix. */
	Mean,
	/** Population variance over every defined element. */
	Variance,
	/** Skewness over every defined element. */
	Skewness,
	/** Excess kurtosis over every defined element. */
	Kurtosis,
	/** Smallest defined element. */
	Minimum,
	/** Largest defined element. */
	Maximum,
	/** Mean of the self pairs, the diagonal of the matrix. */
	DiagonalMean,
	/** Mean of the pairs of different gray levels. */
	OffDiagonalMean;
}
