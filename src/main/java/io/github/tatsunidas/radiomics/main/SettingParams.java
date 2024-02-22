/*
 * Copyright [2022] [Tatsuaki Kobayashi]

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
package com.vis.radiomics.main;

/**
 * 
 * how to:
 * SettingParams binWidth = SettingParams.binWidth;
 * System.out.println(binWidth.name());//variable name
 * System.out.println(binWidth.ordinal());
 * 
 * @author tatsunidas
 */
public enum SettingParams {
	/*
	 * header is needed.
	 * INT_, DOUBLE_ : numeriacal param. cast to it.
	 * BOOL_ : boolean param
	 */
	INT_label,
	INT_interpolation2D,//for original img
	INT_interpolation_mask2D,//for original img
	INT_interpolation3D,
	INT_interpolation_mask3D,
	DOUBLE_Mask_PartialVolumeThareshold,//0.5
	BOOL_USE_FixedBinNumber,
	DOUBLE_binWidth,
	INT_binCount,
	BOOL_normalize,
	BOOL_removeOutliers,
	DOUBLE_zScore,
	DOUBLE_normalizeScale,
	DOUBLE_densityShift,
	DOUBLE_rangeMax,
	DOUBLE_rangeMin,
	INT_IVH_binCount,
	DOUBLE_IVH_binWidth,
	INT_IVH_MODE,
	INT_alpha,//NGLDM, NGTDM search range 
	INT_deltaGLCM,//GLCM distance
	INT_deltaNGTDM,//NGTDM distance
	INT_deltaNGLDM,//NGLDM distance
	STRING_weightingNorm,
	INTARRAY_box_sizes,//to calsulate fractal D
	DOUBLEARRAY_resamplingFactorXYZ,
	BOOL_force2D,//slice by slice calculation
	BOOL_activate_no_default_features,
	BOOL_enableIntensityBasedStatistics,
	BOOL_enableLocalIntensityFeatures,
	BOOL_enableIntensityHistogram,
	BOOL_enableIntensityVolumeHistogram,
	BOOL_enableMorphological,
	BOOL_enableShape2D,
	BOOL_enableGLCM,
	BOOL_enableGLRLM,
	BOOL_enableGLSZM,
	BOOL_enableGLDZM,
	BOOL_enableNGTDM,
	BOOL_enableNGLDM,
	BOOL_enableHomological,
	BOOL_enableFractal,
	BOOL_enableOperationalInfo,//date, os, version, modality name, manufacturer, 
	BOOL_enableDiagnostics,
	;

}
