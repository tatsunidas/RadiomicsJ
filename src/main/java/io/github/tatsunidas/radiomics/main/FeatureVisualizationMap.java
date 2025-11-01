package io.github.tatsunidas.radiomics.main;

import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.FolderOpener;
import ij.process.FloatProcessor;
import io.github.tatsunidas.radiomics.features.GLCMFeatureType;
import io.github.tatsunidas.radiomics.features.GLCMFeatures;
import io.github.tatsunidas.radiomics.features.RadiomicsFeature;
//import io.github.tatsunidas.radiomics.features.Texture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Usage: Map<String, ImagePlus fmap> fmaps = FeatureVisualizationMap.generate(...);
 * @author tatsunidas
 *
 */
public class FeatureVisualizationMap {
	
	/*
	 * how to
	 */
	public static void main(String[] args) {
		
		String data_folder_path = "/home/tatsunidas/git/RadiomicsJ/src/test/resources/data_sets-master/ibsi_1_ct_radiomics_phantom/dicom/image";
		int filter_size = 7; // 奇数を推奨

		// ★★★ 計算したい特徴をリストで自由に定義 ★★★
		List<FeatureSpecifier<?>> featuresToCalculate = new ArrayList<>();

		// 【ご要望の例】GLCMから2つの特徴を指定
		Map<String, Object> settings = new HashMap<>();
		settings.put(RadiomicsFeature.LABEL, 1/*careful with mask*/);
		settings.put(RadiomicsFeature.USE_BIN_COUNT, true);
		settings.put(RadiomicsFeature.nBins, 16);//USE_BIN_COUNT==trueの場合必須。
		featuresToCalculate.add(new FeatureSpecifier<>(GLCMFeatures.class, GLCMFeatureType.JointEntropy, settings));
		featuresToCalculate.add(new FeatureSpecifier<>(GLCMFeatures.class, GLCMFeatureType.DifferenceAverage, settings));

		// 【追加の例】別の特徴量ファミリー（GLRLM）からも特徴を指定
		// Map<String, Object> settings4GLRLM = new HashMap<>();
		// settings.put(RadiomicsFeature.LABEL, 1);
		// settings.put(RadiomicsFeature.USE_BIN_COUNT, true);
		// featuresToCalculate.add(new FeatureSpecifier(GLRLMFeatures.class, GLRLMFeatureType.GrayLevelNonUniformity, settings4GLRLM));
		// ====================

		System.out.println("Loading images...");
		ImagePlus img = FolderOpener.open(data_folder_path);

		if (img == null || img.getStackSize() <= 1) {
			System.err.println("Failed to open image/mask stacks. Please check paths or use test data.");
			return;
		}
		
		//full face mask test
		ImagePlus mask = ImagePreprocessing.createMask(img.getWidth(), img.getHeight(), img.getNSlices(), null,
				1/* label */, img.getCalibration().pixelWidth, img.getCalibration().pixelHeight,
				img.getCalibration().pixelDepth);

		// --- 定義されたリストに基づいて、すべての特徴量マップを順番に生成 ---
		for (FeatureSpecifier<?> spec : featuresToCalculate) {
			System.out.println("======================================================");
			System.out.println("Generating 3D feature map for: " + spec.getDisplayName());

			// 1. ファクトリを使って、現在のスペックに合わせた計算機を生成
			FeatureCalculator calculator = new FeatureCalculatorFactory().create(spec);

			// 2. マップを生成
			long startTime = System.currentTimeMillis();
			/*
			 * slice = -1 means calculate all.
			 */
			int slice = 40;
			boolean d2_mode = false;
			ImagePlus featureMap = generateFeatureMap(img, mask, slice, calculator, filter_size, false/*2d mode*/);
			long endTime = System.currentTimeMillis();
			System.out.println("--> Generation took " + (endTime - startTime) + " ms.");

			// 3. 結果を表示
			if (featureMap != null) {
				if(d2_mode) {
					featureMap.setTitle(spec.getDisplayName() + "_2D_map");
				}else {
					featureMap.setTitle(spec.getDisplayName() + "_3D_map");
				}
				featureMap.show();
			}
		}
		System.out.println("======================================================");
		System.out.println("All processing finished.");
	}
	
	/**
	 * 
	 * @param <T>
	 * @param img
	 * @param mask
	 * @param slice : -1 means all slice.
	 * @param filter_size: odd number is recommended.
	 * @param d2_mode : true, 2d(XY) filter will apply. false, 3d filter(XYZ) will apply.
	 * @param featureClass : e.g., GLCMFeatures.class
	 * @param settings : Map<RadiomicsFeature.String, Object2Calculate> settings.
	 * @param featureEnums : e.g., GLCMFeatureType.JointEntropy
	 * @return Map<FeatureName String, FeatureMap's ImagePlus> fmaps
	 */
	public static <T extends RadiomicsFeature> Map<String, ImagePlus> generate(
			ImagePlus img, ImagePlus mask, int slice, int filter_size, boolean d2_mode,
			Class<T> featureClass, Map<String, Object> settings, Enum<?>... featureEnums) {
		
		List<FeatureSpecifier<?>> featuresToCalculate = new ArrayList<>();
		for(Enum<?> ftype: featureEnums) {
			featuresToCalculate.add(new FeatureSpecifier<>(featureClass, ftype, settings));
		}
		
		Map<String, ImagePlus> fmaps = new HashMap<>();
		for (FeatureSpecifier<?> spec : featuresToCalculate) {
			System.out.println("======================================================");
			System.out.println("Generating feature map for: " + spec.getDisplayName());

			// 1. ファクトリを使って、現在のスペックに合わせた計算機を生成
			FeatureCalculator calculator = new FeatureCalculatorFactory().create(spec);

			// 2. マップを生成
			long startTime = System.currentTimeMillis();
			/*
			 * slice = -1 means calculate all.
			 */
			ImagePlus featureMap = generateFeatureMap(img, mask, slice, calculator, filter_size, d2_mode);
			long endTime = System.currentTimeMillis();
			System.out.println("--> Generation took " + (endTime - startTime) + " ms.");

			if (featureMap != null) {
				if(d2_mode) {
					featureMap.setTitle(spec.getDisplayName() + "_2D");
					fmaps.put(spec.getDisplayName() + "_2D", featureMap);
				}else {
					featureMap.setTitle(spec.getDisplayName() + "_3D");
					fmaps.put(spec.getDisplayName() + "_3D", featureMap);
				}
			}
		}
		System.out.println("======================================================");
		System.out.println("All processing finished.");
		return fmaps;
	}
	
	/**
     * 指定された座標を中心に、3Dフィルターサイズのサブボリュームを抽出する。
     * 画像の境界を越える場合は、存在する領域のみを抽出する。
     *
     * @param originalImage 元の画像スタック
     * @param cx 中心のx座標
     * @param cy 中心のy座標
     * @param cz 中心のz座標
     * @param filterSize フィルターの直径（奇数を推奨）
     * @return 抽出されたサブボリュームのImagePlus
     */
	private static ImagePlus getSubVolume(ImagePlus originalImage, int cx, int cy, int cz/*0 to N-1*/, int filterSize, boolean patch2DMode) {
		int w = originalImage.getWidth();
		int h = originalImage.getHeight();
		int s = originalImage.getNSlices();
		int r = filterSize / 2; // 半径

		// 境界チェックを行い、抽出範囲を決定
		int xStart = Math.max(0, cx - r);
		int yStart = Math.max(0, cy - r);
		int zStart = Math.max(0, cz - r);
		int xEnd = Math.min(w - 1, cx + r);
		int yEnd = Math.min(h - 1, cy + r);
		int zEnd = Math.min(s - 1, cz + r);
		
		if(patch2DMode) {
			zStart = cz;
			zEnd = cz;
		}

		int subW = xEnd - xStart + 1;
		int subH = yEnd - yStart + 1;
		//int subS = zEnd - zStart + 1;

		ImageStack subStack = new ImageStack(subW, subH);
		ImageStack originalStack = originalImage.getStack();

		for (int z = zStart; z <= zEnd; z++) {
			FloatProcessor subIp = new FloatProcessor(subW, subH);
			// originalStackは1-based index
			FloatProcessor originalIp = originalStack.getProcessor(z + 1).convertToFloatProcessor();
			for (int y = yStart; y <= yEnd; y++) {
				for (int x = xStart; x <= xEnd; x++) {
					subIp.setf(x - xStart, y - yStart, originalIp.getf(x, y));
				}
			}
			subStack.addSlice(subIp);
		}

		ImagePlus subVolume = new ImagePlus("sub-volume", subStack);
		// 元の画像のピクセルサイズ情報をコピー
		subVolume.setCalibration(originalImage.getCalibration().copy());
		return subVolume;
	}

    /**
     * 3Dフィルターを用いて特徴量マップを生成する。
     *
     * @param image 入力画像
     * @param mask マスク画像（0より大きい値の領域を対象とする）
     * @param calculator 計算する特徴量のロジック
     * @param filterSize フィルターの直径（奇数を推奨）
     * @return 特徴量マップのImagePlus
     */
	public static ImagePlus generateFeatureMap(ImagePlus image, ImagePlus mask, int slice/*1 to N*/, FeatureCalculator calculator,
			int filterSize, boolean d2_mode) {
		
		if(image == null || image.getNSlices() == 0) {
			throw new IllegalArgumentException("Image is null or no slices, please check input images.");
		}
		
		if (image.getType() == ImagePlus.COLOR_RGB) {
			throw new IllegalArgumentException("It can read only grayscale images(8/16/32 bits)...sorry.");
		}
		
		if (mask == null) {
			throw new IllegalArgumentException("Null mask is not acceptable in FeatureMap...");
		}
		
		int w = image.getWidth();
		int h = image.getHeight();
		int s = image.getNSlices();
		
		if (w != mask.getWidth() || h != mask.getHeight() || s != mask.getNSlices()) {
			throw new IllegalArgumentException("Please input same dimension image and mask.");
		}
		
		if (slice != -1 && (slice > s || slice < 1)) {
			throw new IllegalArgumentException("Please input valid slice position. This slice position out-of-range.");
		}
		
		if (slice == -1) {
			System.out.println("Take too long time to generate feature map..., Take time to coffee-break.");
		}

		ImageStack outputStack = new ImageStack(w, h);// if specify depth, set the slice-position at addSlice.

		ImageStack maskStack = mask.getStack();
		
		int z_start = 0;
		int z_end = s;
		
		if(slice != -1) {
			z_start = slice-1;
			z_end = z_start+1;
		}

		for (int z = z_start; z < z_end; z++) {
			FloatProcessor outputIp = new FloatProcessor(w, h);
			FloatProcessor maskIp = maskStack.getProcessor(z + 1).convertToFloatProcessor();
			System.out.println("Processing slice: " + (z + 1) + "/" + s);

			for (int y = 0; y < h; y++) {
				for (int x = 0; x < w; x++) {
					if (maskIp.getf(x, y) < 1) {
						outputIp.setf(x, y, 0f);
						continue;
					}

					ImagePlus sub_vol = getSubVolume(image, x, y, z, filterSize, d2_mode/* 2d mode */);
					ImagePlus sub_mask = getSubVolume(mask, x, y, z, filterSize, d2_mode/* 2d mode */);

					try {
						Double value = calculator.calculate(sub_vol, sub_mask);
						if (value == null || Double.isNaN(value) || Double.isInfinite(value)) {
							outputIp.setf(x, y, 0f);
						} else {
							outputIp.setf(x, y, value.floatValue());
						}
					} catch (Exception e) {
						// ★★★★★ ここからが修正点 ★★★★★
						// InvocationTargetExceptionの中身（根本原因）を取得して表示する
						Throwable cause = e.getCause();
						if (cause != null) {
							System.err.println("--> Root Cause:");
							cause.printStackTrace(); // これが本当のエラー内容！
						} else {
							// もしラッパーでなければ、そのままエラーを表示
							e.printStackTrace();
						}
						// ★★★★★ ここまで ★★★★★
						System.err.println(
								"Error calculating feature at (" + x + "," + y + "," + z + "): " + e.getMessage());
						outputIp.setf(x, y, 0f);
					}
				}
			}
			outputStack.addSlice(outputIp);
		}
		String name = d2_mode ? "FeatureMap_2D":"FeatureMap_3D";
		ImagePlus fmap = new ImagePlus(name, outputStack);
		fmap.setCalibration(image.getCalibration());
		return fmap;
	}
}
