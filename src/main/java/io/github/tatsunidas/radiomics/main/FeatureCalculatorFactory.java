package io.github.tatsunidas.radiomics.main;

import java.lang.reflect.Constructor;
import java.util.Map;

import ij.ImagePlus;
import io.github.tatsunidas.radiomics.features.RadiomicsFeature;

public class FeatureCalculatorFactory {
//	RadiomicsFeature & Textureの両方を実装したクラスのみを許容する場合。
//	public <T extends RadiomicsFeature & Texture> FeatureCalculator create(FeatureSpecifier<T> spec) {
	public <T extends RadiomicsFeature> FeatureCalculator create(FeatureSpecifier<T> spec) {
		return (sub_vol, sub_mask) -> {
			try {
				// リフレクションを使い、指定されたクラスのコンストラクタを取得
				Constructor<? extends RadiomicsFeature> constructor = spec.featureClass.getConstructor(ImagePlus.class,
						ImagePlus.class, Map.class);
				// デフォルト設定でインスタンスを生成
				RadiomicsFeature featureInstance = constructor.newInstance(sub_vol, sub_mask, spec.settings);
				// 指定されたIDの特徴量を計算して返す
				return featureInstance.calculate(spec.featureId);
			} catch (Exception e) {
				throw new RuntimeException("Failed to create or calculate feature instance", e);
			}
		};
	}
}
