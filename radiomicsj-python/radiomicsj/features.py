import os
import jpype
import numpy as np
import scipy.ndimage

class BaseRadiomicsFeature:
    """すべてのテクスチャ特徴量クラスに共通するベースクラス"""
    def __init__(self, image_np: np.ndarray, mask_np: np.ndarray, spacing: tuple):
        if not jpype.isThreadAttachedToJVM():
            _ensure_jvm()
        
        # NumPyからImagePlusへの変換（共通処理）
        self.image_plus = _numpy_to_imageplus(image_np, spacing)
        self.mask_plus = _numpy_to_imageplus(mask_np, spacing)
        
        # サブクラスで定義されるJavaインスタンスとEnumクラス
        self._java_instance = None
        self._enum_class = None

    def calculate_feature(self, feature_name_or_id: str) -> float:
        """特徴名(例: 'JointAverage') または IBSI ID(例: '60VM') を指定して計算"""
        if self._java_instance is None or self._enum_class is None:
            raise NotImplementedError("Java instance or Enum class is not initialized.")

        try:
            enum_obj = getattr(self._enum_class, feature_name_or_id)
            target_id = str(enum_obj.id())
        except AttributeError:
            target_id = feature_name_or_id
            
        result = self._java_instance.calculate(target_id)
        return float(result) if result is not None else np.nan
    
    def calculate_features(self, feature_names_or_ids: list) -> dict:
        """
        指定されたリストに含まれる特徴量のみを計算して辞書で返す
        例: calculate_features([GLCM.JointMaximum, GLCM.JointEntropy])
        """
        results = {}
        for name_or_id in feature_names_or_ids:
            # 各特徴量を計算して格納
            results[name_or_id] = self.calculate_feature(name_or_id)
        return results

    def get_all_features(self) -> dict:
        """クラスが持つすべての特徴量を辞書形式で一括取得"""
        if self._enum_class is None:
            return {}
            
        results = {}
        for feature_enum in self._enum_class.values():
            feature_name = str(feature_enum.name())
            feature_id = str(feature_enum.id())
            results[feature_name] = self.calculate_feature(feature_id)
        return results

# =====================================================================
# 非テクスチャクラス（形態学・ヒストグラム・統計など）の実装
# =====================================================================

class Morphological(BaseRadiomicsFeature):
    # --- Feature Constants ---
    VolumeMesh = "VolumeMesh"
    VolumeVoxelCounting = "VolumeVoxelCounting"
    SurfaceAreaMesh = "SurfaceAreaMesh"
    SurfaceToVolumeRatio = "SurfaceToVolumeRatio"
    Compactness1 = "Compactness1"
    Compactness2 = "Compactness2"
    SphericalDisproportion = "SphericalDisproportion"
    Sphericity = "Sphericity"
    Asphericity = "Asphericity"
    CentreOfMassShift = "CentreOfMassShift"
    Maximum3DDiameter = "Maximum3DDiameter"
    MajorAxisLength = "MajorAxisLength"
    MinorAxisLength = "MinorAxisLength"
    LeastAxisLength = "LeastAxisLength"
    Elongation = "Elongation"
    Flatness = "Flatness"
    VolumeDensity_AxisAlignedBoundingBox = "VolumeDensity_AxisAlignedBoundingBox"
    AreaDensity_AxisAlignedBoundingBox = "AreaDensity_AxisAlignedBoundingBox"
    VolumeDensity_ApproximateEnclosingEllipsoid = "VolumeDensity_ApproximateEnclosingEllipsoid"
    AreaDensity_ApproximateEnclosingEllipsoid = "AreaDensity_ApproximateEnclosingEllipsoid"
    VolumeDensity_ConvexHull = "VolumeDensity_ConvexHull"
    AreaDensity_ConvexHull = "AreaDensity_ConvexHull"
    IntegratedIntensity = "IntegratedIntensity"
    MoransIIndex = "MoransIIndex"
    GearysCMeasure = "GearysCMeasure"

    def __init__(self, image_np: np.ndarray, mask_np: np.ndarray, spacing: tuple = (1.0, 1.0, 1.0), label: int = 1):
        super().__init__(image_np, mask_np, spacing)
        
        JHashMap = jpype.JClass("java.util.HashMap")
        RadiomicsFeature = jpype.JClass("io.github.tatsunidas.radiomics.features.RadiomicsFeature")
        JInteger = jpype.JClass("java.lang.Integer") # ⬅️ 修正
        
        java_settings = JHashMap()
        java_settings.put(str(RadiomicsFeature.LABEL), JInteger(label))
        
        self._enum_class = jpype.JClass("io.github.tatsunidas.radiomics.features.MorphologicalFeatureType")
        FeatureClass = jpype.JClass("io.github.tatsunidas.radiomics.features.MorphologicalFeatures")
        self._java_instance = FeatureClass(self.image_plus, self.mask_plus, java_settings)


class IntensityBasedStatistical(BaseRadiomicsFeature):
    # --- Feature Constants ---
    Mean = "Mean"
    Variance = "Variance"
    Skewness = "Skewness"
    Kurtosis = "Kurtosis"
    Median = "Median"
    Minimum = "Minimum"
    Percentile10 = "Percentile10"
    Percentile90 = "Percentile90"
    Maximum = "Maximum"
    Interquartile = "Interquartile"
    Range = "Range"
    MeanAbsoluteDeviation = "MeanAbsoluteDeviation"
    RobustMeanAbsoluteDeviation = "RobustMeanAbsoluteDeviation"
    MedianAbsoluteDeviation = "MedianAbsoluteDeviation"
    CoefficientOfVariation = "CoefficientOfVariation"
    QuartileCoefficientOfDispersion = "QuartileCoefficientOfDispersion"
    Energy = "Energy"
    RootMeanSquared = "RootMeanSquared"
    TotalEnergy = "TotalEnergy"
    StandardDeviation = "StandardDeviation"
    StandardError = "StandardError"

    def __init__(self, image_np: np.ndarray, mask_np: np.ndarray, spacing: tuple = (1.0, 1.0, 1.0), label: int = 1):
        super().__init__(image_np, mask_np, spacing)
        
        JHashMap = jpype.JClass("java.util.HashMap")
        RadiomicsFeature = jpype.JClass("io.github.tatsunidas.radiomics.features.RadiomicsFeature")
        JInteger = jpype.JClass("java.lang.Integer") # ⬅️ 修正
        
        java_settings = JHashMap()
        java_settings.put(str(RadiomicsFeature.LABEL), JInteger(label))
        
        self._enum_class = jpype.JClass("io.github.tatsunidas.radiomics.features.IntensityBasedStatisticalFeatureType")
        FeatureClass = jpype.JClass("io.github.tatsunidas.radiomics.features.IntensityBasedStatisticalFeatures")
        self._java_instance = FeatureClass(self.image_plus, self.mask_plus, java_settings)


class IntensityHistogram(BaseRadiomicsFeature):
    # --- Feature Constants ---
    MeanDiscretisedIntensity = "MeanDiscretisedIntensity"
    Variance = "Variance"
    Skewness = "Skewness"
    Kurtosis = "Kurtosis"
    Median = "Median"
    Minimum = "Minimum"
    Percentile10 = "Percentile10"
    Percentile90 = "Percentile90"
    Maximum = "Maximum"
    Mode = "Mode"
    Interquartile = "Interquartile"
    Range = "Range"
    MeanAbsoluteDeviation = "MeanAbsoluteDeviation"
    RobustMeanAbsoluteDeviation = "RobustMeanAbsoluteDeviation"
    MedianAbsoluteDeviation = "MedianAbsoluteDeviation"
    CoefficientOfVariation = "CoefficientOfVariation"
    QuartileCoefficientOfDispersion = "QuartileCoefficientOfDispersion"
    Entropy = "Entropy"
    Uniformity = "Uniformity"
    MaximumHistogramGradient = "MaximumHistogramGradient"
    MaximumHistogramGradientIntensity = "MaximumHistogramGradientIntensity"
    MinimumHistogramGradient = "MinimumHistogramGradient"
    MinimumHistogramGradientIntensity = "MinimumHistogramGradientIntensity"

    def __init__(self, image_np: np.ndarray, mask_np: np.ndarray, spacing: tuple = (1.0, 1.0, 1.0), 
                 label: int = 1, use_bin_count: bool = True, n_bins: int = 32, bin_width: float = 25.0):
        super().__init__(image_np, mask_np, spacing)
        
        JInteger = jpype.JClass("java.lang.Integer")
        JDouble = jpype.JClass("java.lang.Double")
        
        j_label = JInteger(label)
        j_n_bins = JInteger(n_bins) if use_bin_count else None
        j_bin_width = JDouble(bin_width) if not use_bin_count else None
        
        self._enum_class = jpype.JClass("io.github.tatsunidas.radiomics.features.IntensityHistogramFeatureType")
        FeatureClass = jpype.JClass("io.github.tatsunidas.radiomics.features.IntensityHistogramFeatures")
        
        # エラーログで判明した専用のコンストラクタ（個別の引数を渡す）を使用
        self._java_instance = FeatureClass(self.image_plus, self.mask_plus, j_label, use_bin_count, j_n_bins, j_bin_width)


class IntensityVolumeHistogram(BaseRadiomicsFeature):
    # --- Feature Constants ---
    VolumeAtIntensityFraction10 = "VolumeAtIntensityFraction10"
    VolumeAtIntensityFraction90 = "VolumeAtIntensityFraction90"
    IntensityAtVolumeFraction10 = "IntensityAtVolumeFraction10"
    IntensityAtVolumeFraction90 = "IntensityAtVolumeFraction90"
    VolumeFractionDifferenceBetweenIntensityFractions = "VolumeFractionDifferenceBetweenIntensityFractions"
    IntensityFractionDifferenceBetweenVolumeFractions = "IntensityFractionDifferenceBetweenVolumeFractions"

    # ivh_mode 引数を追加し、デフォルト値を設定 (例: 2はbinCount, 1はbinWidth, 0はdiscretizeなし)
    def __init__(self, image_np: np.ndarray, mask_np: np.ndarray, spacing: tuple = (1.0, 1.0, 1.0), 
                 label: int = 1, use_bin_count: bool = True, n_bins: int = 32, bin_width: float = 25.0,
                 ivh_mode: int = 2): 
        super().__init__(image_np, mask_np, spacing)
        
        JHashMap = jpype.JClass("java.util.HashMap")
        RadiomicsFeature = jpype.JClass("io.github.tatsunidas.radiomics.features.RadiomicsFeature")
        JInteger = jpype.JClass("java.lang.Integer") 
        JBoolean = jpype.JClass("java.lang.Boolean") 
        JDouble = jpype.JClass("java.lang.Double")   
        
        java_settings = JHashMap()
        java_settings.put(str(RadiomicsFeature.LABEL), JInteger(label))
        java_settings.put(str(RadiomicsFeature.USE_BIN_COUNT), JBoolean(use_bin_count))
        
        # IVH_MODE を設定に追加
        java_settings.put(str(RadiomicsFeature.IVH_MODE), JInteger(ivh_mode))
        
        if use_bin_count:
            java_settings.put(str(RadiomicsFeature.nBins), JInteger(n_bins))
        else:
            java_settings.put(str(RadiomicsFeature.BinWidth), JDouble(bin_width))
        
        self._enum_class = jpype.JClass("io.github.tatsunidas.radiomics.features.IntensityVolumeHistogramFeatureType")
        FeatureClass = jpype.JClass("io.github.tatsunidas.radiomics.features.IntensityVolumeHistogramFeatures")
        self._java_instance = FeatureClass(self.image_plus, self.mask_plus, java_settings)


class LocalIntensity(BaseRadiomicsFeature):
    # --- Feature Constants ---
    LocalIntensityPeak = "LocalIntensityPeak"
    GlobalIntensityPeak = "GlobalIntensityPeak"

    def __init__(self, image_np: np.ndarray, mask_np: np.ndarray, spacing: tuple = (1.0, 1.0, 1.0), label: int = 1):
        super().__init__(image_np, mask_np, spacing)
        
        JHashMap = jpype.JClass("java.util.HashMap")
        RadiomicsFeature = jpype.JClass("io.github.tatsunidas.radiomics.features.RadiomicsFeature")
        JInteger = jpype.JClass("java.lang.Integer") 
        
        java_settings = JHashMap()
        java_settings.put(str(RadiomicsFeature.LABEL), JInteger(label))
        
        self._enum_class = jpype.JClass("io.github.tatsunidas.radiomics.features.LocalIntensityFeatureType")
        FeatureClass = jpype.JClass("io.github.tatsunidas.radiomics.features.LocalIntensityFeatures")
        self._java_instance = FeatureClass(self.image_plus, self.mask_plus, java_settings)

# =====================================================================
# 各テクスチャクラスの実装
# =====================================================================

class GLCM(BaseRadiomicsFeature):
	
	# --- Feature Constants ---
    JointMaximum = "JointMaximum"
    JointAverage = "JointAverage"
    JointVariance = "JointVariance"
    JointEntropy = "JointEntropy"
    DifferenceAverage = "DifferenceAverage"
    DifferenceVariance = "DifferenceVariance"
    DifferenceEntropy = "DifferenceEntropy"
    SumAverage = "SumAverage"
    SumVariance = "SumVariance"
    SumEntropy = "SumEntropy"
    AngularSecondMoment = "AngularSecondMoment"
    Contrast = "Contrast"
    Dissimilarity = "Dissimilarity"
    InverseDifference = "InverseDifference"
    NormalizedInverseDifference = "NormalizedInverseDifference"
    InverseDifferenceMoment = "InverseDifferenceMoment"
    NormalizedInverseDifferenceMoment = "NormalizedInverseDifferenceMoment"
    InverseVariance = "InverseVariance"
    Correlation = "Correlation"
    Autocorrection = "Autocorrection"
    ClusterTendency = "ClusterTendency"
    ClusterShade = "ClusterShade"
    ClusterProminence = "ClusterProminence"
    InformationalMeasureOfCorrelation1 = "InformationalMeasureOfCorrelation1"
    InformationalMeasureOfCorrelation2 = "InformationalMeasureOfCorrelation2"
	
    def __init__(self, image_np: np.ndarray, mask_np: np.ndarray, spacing: tuple = (1.0, 1.0, 1.0),
                 label: int = 1, delta: int = 1, use_bin_count: bool = True, 
                 n_bins: int = 32, bin_width: float = 25.0, weighting_norm: str = "euclidian"):
        super().__init__(image_np, mask_np, spacing)
        
        JInt = jpype.JClass("java.lang.Integer")
        JDouble = jpype.JClass("java.lang.Double")
        JString = jpype.JClass("java.lang.String")
        j_n_bins = JInt(n_bins) if use_bin_count else None
        j_bin_width = JDouble(bin_width) if not use_bin_count else None
        
        self._enum_class = jpype.JClass("io.github.tatsunidas.radiomics.features.GLCMFeatureType")
        GLCMFeatures_class = jpype.JClass("io.github.tatsunidas.radiomics.features.GLCMFeatures")
        self._java_instance = GLCMFeatures_class(
            self.image_plus, self.mask_plus, label, JInt(delta), 
            use_bin_count, j_n_bins, j_bin_width, JString(weighting_norm)
        )

    def get_matrix(self, angle: tuple) -> np.ndarray:
        java_matrix = self._java_instance.getMatrix(jpype.JArray(jpype.JInt)(angle))
        return np.array(java_matrix, dtype=np.float64) if java_matrix else None


class GLRLM(BaseRadiomicsFeature):
	
	# --- Feature Constants ---
    ShortRunEmphasis = "ShortRunEmphasis"
    LongRunEmphasis = "LongRunEmphasis"
    LowGrayLevelRunEmphasis = "LowGrayLevelRunEmphasis"
    HighGrayLevelRunEmphasis = "HighGrayLevelRunEmphasis"
    ShortRunLowGrayLevelEmphasis = "ShortRunLowGrayLevelEmphasis"
    ShortRunHighGrayLevelEmphasis = "ShortRunHighGrayLevelEmphasis"
    LongRunLowGrayLevelEmphasis = "LongRunLowGrayLevelEmphasis"
    LongRunHighGrayLevelEmphasis = "LongRunHighGrayLevelEmphasis"
    GrayLevelNonUniformity = "GrayLevelNonUniformity"
    GrayLevelNonUniformityNormalized = "GrayLevelNonUniformityNormalized"
    RunLengthNonUniformity = "RunLengthNonUniformity"
    RunLengthNonUniformityNormalized = "RunLengthNonUniformityNormalized"
    RunPercentage = "RunPercentage"
    GrayLevelVariance = "GrayLevelVariance"
    RunLengthVariance = "RunLengthVariance"
    RunEntropy = "RunEntropy"
	
    def __init__(self, image_np: np.ndarray, mask_np: np.ndarray, spacing: tuple = (1.0, 1.0, 1.0),
                 label: int = 1, use_bin_count: bool = True, n_bins: int = 32, 
                 bin_width: float = 25.0, weighting_norm: str = "euclidian"):
        super().__init__(image_np, mask_np, spacing)
        
        JInt = jpype.JClass("java.lang.Integer")
        JDouble = jpype.JClass("java.lang.Double")
        JString = jpype.JClass("java.lang.String")
        j_n_bins = JInt(n_bins) if use_bin_count else None
        j_bin_width = JDouble(bin_width) if not use_bin_count else None
        
        self._enum_class = jpype.JClass("io.github.tatsunidas.radiomics.features.GLRLMFeatureType")
        GLRLMFeatures_class = jpype.JClass("io.github.tatsunidas.radiomics.features.GLRLMFeatures")
        self._java_instance = GLRLMFeatures_class(
            self.image_plus, self.mask_plus, label, 
            use_bin_count, j_n_bins, j_bin_width, JString(weighting_norm)
        )

    def get_matrix(self, angle: tuple) -> np.ndarray:
        java_matrix = self._java_instance.getMatrix(jpype.JArray(jpype.JInt)(angle))
        return np.array(java_matrix, dtype=np.float64) if java_matrix else None


class GLSZM(BaseRadiomicsFeature):
	
	# --- Feature Constants ---
    SmallZoneEmphasis = "SmallZoneEmphasis"
    LargeZoneEmphasis = "LargeZoneEmphasis"
    LowGrayLevelZoneEmphasis = "LowGrayLevelZoneEmphasis"
    HighGrayLevelZoneEmphasis = "HighGrayLevelZoneEmphasis"
    SmallZoneLowGrayLevelEmphasis = "SmallZoneLowGrayLevelEmphasis"
    SmallZoneHighGrayLevelEmphasis = "SmallZoneHighGrayLevelEmphasis"
    LargeZoneLowGrayLevelEmphasis = "LargeZoneLowGrayLevelEmphasis"
    LargeZoneHighGrayLevelEmphasis = "LargeZoneHighGrayLevelEmphasis"
    GrayLevelNonUniformity = "GrayLevelNonUniformity"
    GrayLevelNonUniformityNormalized = "GrayLevelNonUniformityNormalized"
    SizeZoneNonUniformity = "SizeZoneNonUniformity"
    SizeZoneNonUniformityNormalized = "SizeZoneNonUniformityNormalized"
    ZonePercentage = "ZonePercentage"
    GrayLevelVariance = "GrayLevelVariance"
    ZoneSizeVariance = "ZoneSizeVariance"
    ZoneSizeEntropy = "ZoneSizeEntropy"
	
    def __init__(self, image_np: np.ndarray, mask_np: np.ndarray, spacing: tuple = (1.0, 1.0, 1.0),
                 label: int = 1, use_bin_count: bool = True, n_bins: int = 32, bin_width: float = 25.0):
        super().__init__(image_np, mask_np, spacing)
        
        JInt = jpype.JClass("java.lang.Integer")
        JDouble = jpype.JClass("java.lang.Double")
        j_n_bins = JInt(n_bins) if use_bin_count else None
        j_bin_width = JDouble(bin_width) if not use_bin_count else None
        
        self._enum_class = jpype.JClass("io.github.tatsunidas.radiomics.features.GLSZMFeatureType")
        GLSZMFeatures_class = jpype.JClass("io.github.tatsunidas.radiomics.features.GLSZMFeatures")
        self._java_instance = GLSZMFeatures_class(
            self.image_plus, self.mask_plus, label, 
            use_bin_count, j_n_bins, j_bin_width
        )

    def get_matrix(self, raw: bool = False) -> np.ndarray:
        java_matrix = self._java_instance.getMatrix(raw)
        return np.array(java_matrix, dtype=np.float64) if java_matrix else None


class GLDZM(BaseRadiomicsFeature):
	
	# --- Feature Constants ---
    SmallDistanceEmphasis = "SmallDistanceEmphasis"
    LargeDistanceEmphasis = "LargeDistanceEmphasis"
    LowGrayLevelZoneEmphasis = "LowGrayLevelZoneEmphasis"
    HighGrayLevelZoneEmphasis = "HighGrayLevelZoneEmphasis"
    SmallDistanceLowGrayLevelEmphasis = "SmallDistanceLowGrayLevelEmphasis"
    SmallDistanceHighGrayLevelEmphasis = "SmallDistanceHighGrayLevelEmphasis"
    LargeDistanceLowGrayLevelEmphasis = "LargeDistanceLowGrayLevelEmphasis"
    LargeDistanceHighGrayLevelEmphasis = "LargeDistanceHighGrayLevelEmphasis"
    GrayLevelNonUniformity = "GrayLevelNonUniformity"
    GrayLevelNonUniformityNormalized = "GrayLevelNonUniformityNormalized"
    ZoneDistanceNonUniformity = "ZoneDistanceNonUniformity"
    ZoneDistanceNonUniformityNormalized = "ZoneDistanceNonUniformityNormalized"
    ZonePercentage = "ZonePercentage"
    GrayLevelVariance = "GrayLevelVariance"
    ZoneDistanceVariance = "ZoneDistanceVariance"
    ZoneDistanceEntropy = "ZoneDistanceEntropy"
	
    def __init__(self, image_np: np.ndarray, mask_np: np.ndarray, spacing: tuple = (1.0, 1.0, 1.0),
                 label: int = 1, use_bin_count: bool = True, n_bins: int = 32, bin_width: float = 25.0):
        super().__init__(image_np, mask_np, spacing)
        
        JInt = jpype.JClass("java.lang.Integer")
        JDouble = jpype.JClass("java.lang.Double")
        j_n_bins = JInt(n_bins) if use_bin_count else None
        j_bin_width = JDouble(bin_width) if not use_bin_count else None
        
        self._enum_class = jpype.JClass("io.github.tatsunidas.radiomics.features.GLDZMFeatureType")
        GLDZMFeatures_class = jpype.JClass("io.github.tatsunidas.radiomics.features.GLDZMFeatures")
        self._java_instance = GLDZMFeatures_class(
            self.image_plus, self.mask_plus, label, 
            use_bin_count, j_n_bins, j_bin_width
        )

    def get_matrix(self, raw: bool = False) -> np.ndarray:
        java_matrix = self._java_instance.getMatrix(raw)
        return np.array(java_matrix, dtype=np.float64) if java_matrix else None


class NGLDM(BaseRadiomicsFeature):
	
	# --- Feature Constants ---
    LowDependenceEmphasis = "LowDependenceEmphasis"
    HighDependenceEmphasis = "HighDependenceEmphasis"
    LowGrayLevelCountEmphasis = "LowGrayLevelCountEmphasis"
    HighGrayLevelCountEmphasis = "HighGrayLevelCountEmphasis"
    LowDependenceLowGrayLevelEmphasis = "LowDependenceLowGrayLevelEmphasis"
    LowDependenceHighGrayLevelEmphasis = "LowDependenceHighGrayLevelEmphasis"
    HighDependenceLowGrayLevelEmphasis = "HighDependenceLowGrayLevelEmphasis"
    HighDependenceHighGrayLevelEmphasis = "HighDependenceHighGrayLevelEmphasis"
    GrayLevelNonUniformity = "GrayLevelNonUniformity"
    GrayLevelNonUniformityNormalized = "GrayLevelNonUniformityNormalized"
    DependenceCountNonUniformity = "DependenceCountNonUniformity"
    DependenceCountNonUniformityNormalized = "DependenceCountNonUniformityNormalized"
    DependenceCountPercentage = "DependenceCountPercentage"
    GrayLevelVariance = "GrayLevelVariance"
    DependenceCountVariance = "DependenceCountVariance"
    DependenceCountEntropy = "DependenceCountEntropy"
    DependenceCountEnergy = "DependenceCountEnergy"
	
    def __init__(self, image_np: np.ndarray, mask_np: np.ndarray, spacing: tuple = (1.0, 1.0, 1.0),
                 label: int = 1, alpha: int = 0, delta: int = 1, 
                 use_bin_count: bool = True, n_bins: int = 32, bin_width: float = 25.0):
        super().__init__(image_np, mask_np, spacing)
        
        JInt = jpype.JClass("java.lang.Integer")
        JDouble = jpype.JClass("java.lang.Double")
        j_n_bins = JInt(n_bins) if use_bin_count else None
        j_bin_width = JDouble(bin_width) if not use_bin_count else None
        
        self._enum_class = jpype.JClass("io.github.tatsunidas.radiomics.features.NGLDMFeatureType")
        NGLDMFeatures_class = jpype.JClass("io.github.tatsunidas.radiomics.features.NGLDMFeatures")
        self._java_instance = NGLDMFeatures_class(
            self.image_plus, self.mask_plus, label, JInt(alpha), JInt(delta), 
            use_bin_count, j_n_bins, j_bin_width
        )

    def get_matrix(self) -> np.ndarray:
        java_matrix = self._java_instance.getMatrix()
        return np.array(java_matrix, dtype=np.float64) if java_matrix else None


class NGTDM(BaseRadiomicsFeature):
	
	# --- Feature Constants ---
    Coarseness = "Coarseness"
    Contrast = "Contrast"
    Busyness = "Busyness"
    Complexity = "Complexity"
    Strength = "Strength"
	
    def __init__(self, image_np: np.ndarray, mask_np: np.ndarray, spacing: tuple = (1.0, 1.0, 1.0),
                 label: int = 1, delta: int = 1, use_bin_count: bool = True, 
                 n_bins: int = 32, bin_width: float = 25.0):
        super().__init__(image_np, mask_np, spacing)
        
        JInt = jpype.JClass("java.lang.Integer")
        JDouble = jpype.JClass("java.lang.Double")
        j_n_bins = JInt(n_bins) if use_bin_count else None
        j_bin_width = JDouble(bin_width) if not use_bin_count else None
        
        self._enum_class = jpype.JClass("io.github.tatsunidas.radiomics.features.NGTDMFeatureType")
        NGTDMFeatures_class = jpype.JClass("io.github.tatsunidas.radiomics.features.NGTDMFeatures")
        self._java_instance = NGTDMFeatures_class(
            self.image_plus, self.mask_plus, label, JInt(delta), 
            use_bin_count, j_n_bins, j_bin_width
        )

    def get_matrix(self) -> np.ndarray:
        # NGTDMのgetMatrix()は double[][] (各行が [grayLevel, Ni, Pi, Si]) を返します
        java_matrix = self._java_instance.getMatrix()
        return np.array(java_matrix, dtype=np.float64) if java_matrix else None


def _ensure_jvm():
    """JVMが起動していなければ起動し、スレッドをアタッチする"""
    if not jpype.isJVMStarted():
        # features.py と同じフォルダにある 'jars' フォルダ内の全jarを読み込む
        current_dir = os.path.dirname(os.path.abspath(__file__))
        jar_path = os.path.join(current_dir, "jars", "*")
        
        # JVMを起動（もしcore.pyで別の起動オプションを使っている場合は合わせてください）
        jpype.startJVM(jpype.getDefaultJVMPath(), classpath=[jar_path], convertStrings=True)
        
    if not jpype.isThreadAttachedToJVM():
        jpype.attachThreadToJVM()


def _numpy_to_imageplus(arr: np.ndarray, spacing: tuple = (1.0, 1.0, 1.0)):
    """NumPy配列(Z, Y, X)をJavaのImagePlusに変換するヘルパー関数"""
    _ensure_jvm()
    
    z, y, x = arr.shape
    ImageStack = jpype.JClass("ij.ImageStack")
    FloatProcessor = jpype.JClass("ij.process.FloatProcessor")
    ImagePlus = jpype.JClass("ij.ImagePlus")
    Calibration = jpype.JClass("ij.measure.Calibration")

    stack = ImageStack(x, y)
    for i in range(z):
        # 確実にNumPy配列のfloat32として処理
        slice_arr = np.array(arr[i, :, :]).astype(np.float32).flatten()
        j_float_array = jpype.JArray(jpype.JFloat)(slice_arr)
        bp = FloatProcessor(x, y, j_float_array)
        stack.addSlice(bp)

    imp = ImagePlus("numpy_img", stack)
    cal = Calibration()
    cal.pixelWidth = float(spacing[0])
    cal.pixelHeight = float(spacing[1])
    cal.pixelDepth = float(spacing[2])
    imp.setCalibration(cal)
    return imp
 
 
def generate_feature_map(image_np: np.ndarray, mask_np: np.ndarray, mask_label: int, spacing: tuple, 
                         feature_class: type, feature_id: str, settings: dict,
                         filter_size: int = 7, d2_mode: bool = False, stride: int = 1, slice_idx: int = -1) -> np.ndarray:
    """
    指定した特徴量の可視化マップ(Feature Map)を生成し、補間して返す
    """
    _ensure_jvm()
    
    # 💡 引数として渡された Pythonのクラス名(例: GLCM) を自動で文字列化
    feature_class_name = feature_class.__name__
    
    # 1. ImagePlusに変換
    image_plus = _numpy_to_imageplus(image_np, spacing)
    mask_plus = _numpy_to_imageplus(mask_np, spacing)
    
    # 2. Javaのクラスや引数を準備
    JHashMap = jpype.JClass("java.util.HashMap")
    JString = jpype.JClass("java.lang.String")
    JInteger = jpype.JClass("java.lang.Integer")  # 確実にオブジェクトとして渡す用
    JDouble = jpype.JClass("java.lang.Double")
    JBoolean = jpype.JClass("java.lang.Boolean")
    FeatureVisualizationMap = jpype.JClass("io.github.tatsunidas.radiomics.main.FeatureVisualizationMap")
    RadiomicsFeature = jpype.JClass("io.github.tatsunidas.radiomics.features.RadiomicsFeature")
    
    # 🌟 Pythonのキー名を、Java側の "本物の定数文字列" に動的にマッピング！
    key_mapping = {
        "label": str(RadiomicsFeature.LABEL),
        "useBinCount": str(RadiomicsFeature.USE_BIN_COUNT),
        "nBins": str(RadiomicsFeature.nBins),
        "binWidth": str(RadiomicsFeature.BinWidth),
    }
    
    # settings を Javaの HashMap に変換
    java_settings = JHashMap()
    for k, v in settings.items():
        # マッピングがあればJavaの定数を使い、なければ元のキーを使う
        java_key = key_mapping.get(k, str(k))
        
        # 🌟 jpype.JInt ではなく java.lang.Integer 等のオブジェクトとして明示的に格納
        if isinstance(v, bool):
            java_settings.put(java_key, JBoolean(v))
        elif isinstance(v, int):
            java_settings.put(java_key, JInteger(v))
        elif isinstance(v, float):
            java_settings.put(java_key, JDouble(v))
        else:
            java_settings.put(java_key, JString(str(v)))

    # FeatureClass と Enum の取得
    target_class = jpype.JClass(f"io.github.tatsunidas.radiomics.features.{feature_class_name}Features")
    enum_class = jpype.JClass(f"io.github.tatsunidas.radiomics.features.{feature_class_name}FeatureType")
    feature_enum = getattr(enum_class, feature_id)
    
    # Enum引数の配列を作成
    enum_array = jpype.JArray(jpype.JClass("java.lang.Enum"))(1)
    enum_array[0] = feature_enum

    # 3. Javaメソッド呼び出し！ (ここでストライド計算が走る)
    print(f"Calculating Map... {feature_class_name} / {feature_id} (Stride: {stride})")
    fmaps_java = FeatureVisualizationMap.generate(
        image_plus, mask_plus, slice_idx, filter_size, d2_mode, stride,
        target_class, java_settings, enum_array
    )
    
    # 4. 返ってきた縮小版のマップを取り出す
    result_key = f"{feature_class_name}Features_{feature_enum.name()}_{'2D' if d2_mode else '3D'}"
    small_map_imp = fmaps_java.get(JString(result_key))
    
    if small_map_imp is None:
        raise ValueError("Java returned null for feature map. Check logs for errors.")
        
    # JavaのImagePlus(縮小版)をNumPy配列に変換
    z, h, w = image_np.shape
    small_z, small_h, small_w = small_map_imp.getNSlices(), small_map_imp.getHeight(), small_map_imp.getWidth()
    small_map_np = np.zeros((small_z, small_h, small_w), dtype=np.float32)
    
    for i in range(small_z):
        ip = small_map_imp.getStack().getProcessor(i + 1)
        pixels = np.array(ip.getPixels(), dtype=np.float32).reshape((small_h, small_w))
        small_map_np[i, :, :] = pixels

    # 5. scipy で元のサイズに線形補間 (order=1)
    zoom_factors = (
        1.0, 
        h / small_h, 
        w / small_w
    )
    restored_map = scipy.ndimage.zoom(small_map_np, zoom_factors, order=1)
    
    # NumPyのzoomで微妙にサイズがズレる（1ピクセル等）場合はスライスで補正
    restored_map = restored_map[:z, :h, :w]
    
    # 6. 滲み取り（元のマスク領域外を0にする）
    restored_map[mask_np < mask_label] = 0.0

    return restored_map
 
        