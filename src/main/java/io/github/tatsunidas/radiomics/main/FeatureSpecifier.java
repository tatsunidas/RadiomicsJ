package io.github.tatsunidas.radiomics.main;

import java.util.Map;

import io.github.tatsunidas.radiomics.features.RadiomicsFeature;

//RadiomicsFeature & Textureの両方を実装したクラスのみを許容する場合。
//public static class FeatureSpecifier <T extends RadiomicsFeature & Texture> {
public class FeatureSpecifier<T extends RadiomicsFeature> {

	public final Class<T> featureClass;
	public final String featureId;
	public final Map<String, Object> settings;
	final String displayName;

	public FeatureSpecifier(Class<T> featureClass, Enum<?> featureEnum, Map<String, Object> settings) {
		this.featureClass = featureClass;

		// RadiomicsFeatureクラスのenumはid()メソッドを持つ
		try {
			this.featureId = (String) featureEnum.getClass().getMethod("id").invoke(featureEnum);
		} catch (Exception e) {
			throw new IllegalArgumentException("Enum must have an id() method.", e);
		}
		this.settings = settings;
		this.displayName = featureClass.getSimpleName() + "_" + featureEnum.name();
	}

	public String getDisplayName() {
		return displayName;
	}
}
