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

	/**
	 * Voxels the roi is grown by before the analysis windows are taken.
	 *
	 * Three pairs with the recommended filter size of seven, whose radius is also
	 * three, so that every voxel of the roi, including the ones on its edge, is
	 * measured in a window the roi fills completely.
	 */
	public static final int DEFAULT_MARGIN = 3;

	
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
			ImagePlus featureMap = generateFeatureMap(img, mask, slice, calculator, filter_size, false/*2d mode*/, 3 /*stride*/);
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
	
//	/**
//	 * 
//	 * @param <T>
//	 * @param img
//	 * @param mask
//	 * @param slice : -1 means all slice.
//	 * @param filter_size: odd number is recommended.
//	 * @param d2_mode : true, 2d(XY) filter will apply. false, 3d filter(XYZ) will apply.
//	 * @param featureClass : e.g., GLCMFeatures.class
//	 * @param settings : Map<RadiomicsFeature.String, Object2Calculate> settings.
//	 * @param featureEnums : e.g., GLCMFeatureType.JointEntropy
//	 * @return Map<FeatureName String, FeatureMap's ImagePlus> fmaps
//	 */
//	public static <T extends RadiomicsFeature> Map<String, ImagePlus> generate(
//			ImagePlus img, ImagePlus mask, int slice, int filter_size, boolean d2_mode,
//			Class<T> featureClass, Map<String, Object> settings, Enum<?>... featureEnums) {
//		
//		List<FeatureSpecifier<?>> featuresToCalculate = new ArrayList<>();
//		for(Enum<?> ftype: featureEnums) {
//			featuresToCalculate.add(new FeatureSpecifier<>(featureClass, ftype, settings));
//		}
//		
//		Map<String, ImagePlus> fmaps = new HashMap<>();
//		for (FeatureSpecifier<?> spec : featuresToCalculate) {
//			System.out.println("======================================================");
//			System.out.println("Generating feature map for: " + spec.getDisplayName());
//
//			// 1. ファクトリを使って、現在のスペックに合わせた計算機を生成
//			FeatureCalculator calculator = new FeatureCalculatorFactory().create(spec);
//
//			// 2. マップを生成
//			long startTime = System.currentTimeMillis();
//			/*
//			 * slice = -1 means calculate all.
//			 */
//			ImagePlus featureMap = generateFeatureMap(img, mask, slice, calculator, filter_size, d2_mode);
//			long endTime = System.currentTimeMillis();
//			System.out.println("--> Generation took " + (endTime - startTime) + " ms.");
//
//			if (featureMap != null) {
//				if(d2_mode) {
//					featureMap.setTitle(spec.getDisplayName() + "_2D");
//					fmaps.put(spec.getDisplayName() + "_2D", featureMap);
//				}else {
//					featureMap.setTitle(spec.getDisplayName() + "_3D");
//					fmaps.put(spec.getDisplayName() + "_3D", featureMap);
//				}
//			}
//		}
//		System.out.println("======================================================");
//		System.out.println("All processing finished.");
//		return fmaps;
//	}
//	
//
//
//    /**
//     * 3Dフィルターを用いて特徴量マップを生成する。
//     *
//     * @param image 入力画像
//     * @param mask マスク画像（0より大きい値の領域を対象とする）
//     * @param calculator 計算する特徴量のロジック
//     * @param filterSize フィルターの直径（奇数を推奨）
//     * @return 特徴量マップのImagePlus
//     */
//	public static ImagePlus generateFeatureMap(ImagePlus image, ImagePlus mask, int slice/*1 to N*/, FeatureCalculator calculator,
//			int filterSize, boolean d2_mode) {
//		
//		if(image == null || image.getNSlices() == 0) {
//			throw new IllegalArgumentException("Image is null or no slices, please check input images.");
//		}
//		
//		if (image.getType() == ImagePlus.COLOR_RGB) {
//			throw new IllegalArgumentException("It can read only grayscale images(8/16/32 bits)...sorry.");
//		}
//		
//		if (mask == null) {
//			throw new IllegalArgumentException("Null mask is not acceptable in FeatureMap...");
//		}
//		
//		int w = image.getWidth();
//		int h = image.getHeight();
//		int s = image.getNSlices();
//		
//		if (w != mask.getWidth() || h != mask.getHeight() || s != mask.getNSlices()) {
//			throw new IllegalArgumentException("Please input same dimension image and mask.");
//		}
//		
//		if (slice != -1 && (slice > s || slice < 1)) {
//			throw new IllegalArgumentException("Please input valid slice position. This slice position out-of-range.");
//		}
//		
//		if (slice == -1) {
//			System.out.println("Take too long time to generate feature map..., Take time to coffee-break.");
//		}
//
//		ImageStack outputStack = new ImageStack(w, h);// if specify depth, set the slice-position at addSlice.
//
//		ImageStack maskStack = mask.getStack();
//		
//		int z_start = 0;
//		int z_end = s;
//		
//		if(slice != -1) {
//			z_start = slice-1;
//			z_end = z_start+1;
//		}
//
//		for (int z = z_start; z < z_end; z++) {
//			FloatProcessor outputIp = new FloatProcessor(w, h);
//			FloatProcessor maskIp = maskStack.getProcessor(z + 1).convertToFloatProcessor();
//			System.out.println("Processing slice: " + (z + 1) + "/" + s);
//
//			for (int y = 0; y < h; y++) {
//				for (int x = 0; x < w; x++) {
//					if (maskIp.getf(x, y) < 1) {
//						outputIp.setf(x, y, 0f);
//						continue;
//					}
//
//					ImagePlus sub_vol = getSubVolume(image, x, y, z, filterSize, d2_mode/* 2d mode */);
//					ImagePlus sub_mask = getSubVolume(mask, x, y, z, filterSize, d2_mode/* 2d mode */);
//
//					try {
//						Double value = calculator.calculate(sub_vol, sub_mask);
//						if (value == null || Double.isNaN(value) || Double.isInfinite(value)) {
//							outputIp.setf(x, y, 0f);
//						} else {
//							outputIp.setf(x, y, value.floatValue());
//						}
//					} catch (Exception e) {
//						// ★★★★★ ここからが修正点 ★★★★★
//						// InvocationTargetExceptionの中身（根本原因）を取得して表示する
//						Throwable cause = e.getCause();
//						if (cause != null) {
//							System.err.println("--> Root Cause:");
//							cause.printStackTrace(); // これが本当のエラー内容！
//						} else {
//							// もしラッパーでなければ、そのままエラーを表示
//							e.printStackTrace();
//						}
//						// ★★★★★ ここまで ★★★★★
//						System.err.println(
//								"Error calculating feature at (" + x + "," + y + "," + z + "): " + e.getMessage());
//						outputIp.setf(x, y, 0f);
//					}
//				}
//			}
//			outputStack.addSlice(outputIp);
//		}
//		String name = d2_mode ? "FeatureMap_2D":"FeatureMap_3D";
//		ImagePlus fmap = new ImagePlus(name, outputStack);
//		fmap.setCalibration(image.getCalibration());
//		return fmap;
//	}
	
	/**
	 * @param <T>
	 * @param img
	 * @param mask
	 * @param slice : -1 means all slice.
	 * @param filter_size: odd number is recommended.
	 * @param d2_mode : true, 2d(XY) filter will apply. false, 3d filter(XYZ) will apply.
	 * @param stride : XY軸方向のストライド（間引き）幅
	 * @param featureClass : e.g., GLCMFeatures.class
	 * @param settings : Map<RadiomicsFeature.String, Object> settings.
	 * @param featureEnums : e.g., GLCMFeatureType.JointEntropy
	 * @return Map<FeatureName String, FeatureMap's ImagePlus> fmaps
	 */
	public static <T extends RadiomicsFeature> Map<String, ImagePlus> generate(
			ImagePlus img, ImagePlus mask, int slice, int filter_size, boolean d2_mode, int stride,
			Class<T> featureClass, Map<String, Object> settings, Enum<?>... featureEnums) {
		return generate(img, mask, slice, filter_size, d2_mode, stride, DEFAULT_MARGIN, featureClass, settings,
				featureEnums);
	}

	/**
	 * Same, with an explicit margin around the roi.
	 *
	 * @param margin how many voxels the mask is grown by before the windows are
	 *               taken, see {@link #DEFAULT_MARGIN}. Zero keeps the mask as it
	 *               is, which is how RadiomicsJ behaved before 2.3.0.
	 */
	public static <T extends RadiomicsFeature> Map<String, ImagePlus> generate(
			ImagePlus img, ImagePlus mask, int slice, int filter_size, boolean d2_mode, int stride, int margin,
			Class<T> featureClass, Map<String, Object> settings, Enum<?>... featureEnums) {

		List<FeatureSpecifier<?>> featuresToCalculate = new ArrayList<>();
		for(Enum<?> ftype: featureEnums) {
			featuresToCalculate.add(new FeatureSpecifier<>(featureClass, ftype, settings));
		}
		
		Map<String, ImagePlus> fmaps = new HashMap<>();
		for (FeatureSpecifier<?> spec : featuresToCalculate) {
			System.out.println("======================================================");
			System.out.println("Generating feature map for: " + spec.getDisplayName());

			FeatureCalculator calculator = new FeatureCalculatorFactory().create(spec);

			long startTime = System.currentTimeMillis();
			// stride を渡して実行
			ImagePlus featureMap = generateFeatureMap(img, mask, slice, calculator, filter_size, d2_mode, stride,
					margin);
			long endTime = System.currentTimeMillis();
			System.out.println("--> Generation took " + (endTime - startTime) + " ms.");

			if (featureMap != null) {
				String prefix = spec.getDisplayName() + (d2_mode ? "_2D" : "_3D");
				featureMap.setTitle(prefix);
				fmaps.put(prefix, featureMap);
			}
		}
		System.out.println("======================================================");
		System.out.println("All processing finished.");
		return fmaps;
	}

    /**
     * 3Dフィルターを用いて特徴量マップを生成する。（Stride対応版）
     * StrideはX,Y axisのみ。
     */
	public static ImagePlus generateFeatureMap(ImagePlus image, ImagePlus mask, int slice/*1 to N*/, FeatureCalculator calculator,
			int filterSize, boolean d2_mode, int stride) {
		return generateFeatureMap(image, mask, slice, calculator, filterSize, d2_mode, stride, DEFAULT_MARGIN);
	}

	/**
	 * Feature map with a margin around the roi.
	 *
	 * A voxel on the edge of the roi sees a window that is only partly filled by
	 * the roi, so its feature value is computed from fewer voxels than a voxel in
	 * the middle, and reads differently for that reason alone rather than because
	 * the texture differs. Growing the mask by a margin gives those edge voxels a
	 * full neighbourhood to be measured in.
	 *
	 * The margin only widens the mask the windows are cut from. The map that comes
	 * back still carries values exactly on the original roi and has the geometry of
	 * the input image, so nothing downstream has to change.
	 *
	 * If the grown mask would reach past the edge of the image, the image is padded
	 * first, by marching outwards from the border and filling each new voxel with
	 * the mean of the neighbours that are already known. The mask is padded with
	 * zeros and grown inside the padded frame, so the two stay aligned.
	 *
	 * @param margin voxels to grow the mask by, {@link #DEFAULT_MARGIN} by default.
	 *               Zero reproduces the behaviour before 2.3.0. Setting it to
	 *               filterSize / 2 guarantees a full window everywhere.
	 */
	public static ImagePlus generateFeatureMap(ImagePlus image, ImagePlus mask, int slice/*1 to N*/, FeatureCalculator calculator,
			int filterSize, boolean d2_mode, int stride, int margin) {

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

		// ★ strideを考慮して出力画像の縮小サイズを計算
		int out_w = (int) Math.ceil((double) w / stride);
		int out_h = (int) Math.ceil((double) h / stride);

		/*
		 * The windows are cut from a grown mask, so that a voxel on the edge of the roi
		 * is measured in a full neighbourhood. Output positions still come from the
		 * original mask, and the offset maps between the two coordinate systems.
		 */
		ImagePlus windowImage = image;
		ImagePlus windowMask = mask;
		int offsetXY = 0;
		int offsetZ = 0;
		if (margin > 0) {
			int padZ = d2_mode ? 0 : margin;
			if (roiReachesTheBorder(mask, margin, d2_mode)) {
				windowImage = padWithMarchingLocalMean(image, margin, padZ);
				windowMask = padWithZero(mask, margin, padZ);
				offsetXY = margin;
				offsetZ = padZ;
			}
			windowMask = dilate(windowMask, margin, d2_mode);
		}

		ImageStack outputStack = new ImageStack(out_w, out_h);
		ImageStack maskStack = mask.getStack();

		int z_start = 0;
		int z_end = s;
		if(slice != -1) {
			z_start = slice-1;
			z_end = z_start+1;
		}

		for (int z = z_start; z < z_end; z++) {
			FloatProcessor outputIp = new FloatProcessor(out_w, out_h);
			FloatProcessor maskIp = maskStack.getProcessor(z + 1).convertToFloatProcessor();
			System.out.println("Processing slice: " + (z + 1) + "/" + s);

			// ★ y と x のループを stride 幅で進める
			for (int y = 0; y < h; y += stride) {
				int out_y = y / stride; // 出力先の縮小座標
				for (int x = 0; x < w; x += stride) {
					int out_x = x / stride; // 出力先の縮小座標

					if (maskIp.getf(x, y) < 1) {
						outputIp.setf(out_x, out_y, 0f);
						continue;
					}

					// 部分抽出は「元画像の座標 (x, y)」で行う（重要）
					// マージンを付けた場合は、その分だけ座標をずらして参照する
					ImagePlus sub_vol = getSubVolume(windowImage, x + offsetXY, y + offsetXY, z + offsetZ, filterSize,
							d2_mode);
					ImagePlus sub_mask = getSubVolume(windowMask, x + offsetXY, y + offsetXY, z + offsetZ, filterSize,
							d2_mode);

					try {
						Double value = calculator.calculate(sub_vol, sub_mask);
						if (value == null || Double.isNaN(value) || Double.isInfinite(value)) {
							outputIp.setf(out_x, out_y, 0f);
						} else {
							outputIp.setf(out_x, out_y, value.floatValue());
						}
					} catch (Exception e) {
						Throwable cause = e.getCause();
						if (cause != null) {
							System.err.println("--> Root Cause:");
							cause.printStackTrace(); 
						} else {
							e.printStackTrace();
						}
						System.err.println("Error calculating feature at (" + x + "," + y + "," + z + "): " + e.getMessage());
						outputIp.setf(out_x, out_y, 0f);
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
	

	// ------------------------------------------------------------------
	// margin around the roi
	// ------------------------------------------------------------------

	/**
	 * True when any roi voxel sits within the margin of the edge of the image, so
	 * that growing the mask would run out of the image.
	 */
	private static boolean roiReachesTheBorder(ImagePlus mask, int margin, boolean d2_mode) {
		int w = mask.getWidth();
		int h = mask.getHeight();
		int s = mask.getNSlices();
		ImageStack stack = mask.getStack();
		for (int z = 0; z < s; z++) {
			boolean nearInZ = !d2_mode && (z < margin || z >= s - margin);
			FloatProcessor ip = stack.getProcessor(z + 1).convertToFloatProcessor();
			for (int y = 0; y < h; y++) {
				for (int x = 0; x < w; x++) {
					if (ip.getf(x, y) < 1) {
						continue;
					}
					if (nearInZ || x < margin || x >= w - margin || y < margin || y >= h - margin) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Grows a mask by the given number of voxels, using a cube shaped structuring
	 * element so that the growth is at least the margin in every direction, which
	 * is what a cubic analysis window needs.
	 */
	private static ImagePlus dilate(ImagePlus mask, int margin, boolean d2_mode) {
		int w = mask.getWidth();
		int h = mask.getHeight();
		int s = mask.getNSlices();
		int marginZ = d2_mode ? 0 : margin;

		float[][] source = new float[s][];
		ImageStack stack = mask.getStack();
		for (int z = 0; z < s; z++) {
			source[z] = (float[]) stack.getProcessor(z + 1).convertToFloatProcessor().getPixels();
		}

		ImageStack out = new ImageStack(w, h);
		for (int z = 0; z < s; z++) {
			FloatProcessor ip = new FloatProcessor(w, h);
			for (int y = 0; y < h; y++) {
				for (int x = 0; x < w; x++) {
					boolean hit = false;
					for (int dz = -marginZ; dz <= marginZ && !hit; dz++) {
						int zz = z + dz;
						if (zz < 0 || zz >= s) {
							continue;
						}
						for (int dy = -margin; dy <= margin && !hit; dy++) {
							int yy = y + dy;
							if (yy < 0 || yy >= h) {
								continue;
							}
							for (int dx = -margin; dx <= margin; dx++) {
								int xx = x + dx;
								if (xx < 0 || xx >= w) {
									continue;
								}
								if (source[zz][yy * w + xx] >= 1) {
									hit = true;
									break;
								}
							}
						}
					}
					ip.setf(x, y, hit ? 1f : 0f);
				}
			}
			out.addSlice(ip);
		}
		ImagePlus dilated = new ImagePlus(mask.getTitle() + "_dilated", out);
		dilated.setCalibration(mask.getCalibration().copy());
		return dilated;
	}

	/**
	 * Enlarges a mask with zeros, so that it keeps lining up with a padded image.
	 */
	private static ImagePlus padWithZero(ImagePlus mask, int padXY, int padZ) {
		int w = mask.getWidth();
		int h = mask.getHeight();
		int s = mask.getNSlices();
		int pw = w + 2 * padXY;
		int ph = h + 2 * padXY;
		int ps = s + 2 * padZ;

		ImageStack out = new ImageStack(pw, ph);
		ImageStack stack = mask.getStack();
		for (int z = 0; z < ps; z++) {
			FloatProcessor ip = new FloatProcessor(pw, ph);
			int sourceZ = z - padZ;
			if (sourceZ >= 0 && sourceZ < s) {
				FloatProcessor source = stack.getProcessor(sourceZ + 1).convertToFloatProcessor();
				for (int y = 0; y < h; y++) {
					for (int x = 0; x < w; x++) {
						ip.setf(x + padXY, y + padXY, source.getf(x, y));
					}
				}
			}
			out.addSlice(ip);
		}
		ImagePlus padded = new ImagePlus(mask.getTitle() + "_padded", out);
		padded.setCalibration(mask.getCalibration().copy());
		return padded;
	}

	/**
	 * Enlarges an image, filling the new voxels by marching outwards from the
	 * border: each round takes the voxels that touch what is already known and
	 * gives them the mean of those known neighbours, until nothing is left.
	 *
	 * Repeating the border value would create an artificial texture of perfectly
	 * constant lines, which is exactly what a texture feature would then measure.
	 * The local mean carries the border intensity outwards without inventing an
	 * edge.
	 */
	private static ImagePlus padWithMarchingLocalMean(ImagePlus image, int padXY, int padZ) {
		int w = image.getWidth();
		int h = image.getHeight();
		int s = image.getNSlices();
		int pw = w + 2 * padXY;
		int ph = h + 2 * padXY;
		int ps = s + 2 * padZ;

		float[][] value = new float[ps][pw * ph];
		boolean[][] known = new boolean[ps][pw * ph];
		ImageStack stack = image.getStack();
		int unknown = ps * pw * ph;
		for (int z = 0; z < s; z++) {
			FloatProcessor source = stack.getProcessor(z + 1).convertToFloatProcessor();
			int pz = z + padZ;
			for (int y = 0; y < h; y++) {
				for (int x = 0; x < w; x++) {
					int index = (y + padXY) * pw + (x + padXY);
					value[pz][index] = source.getf(x, y);
					known[pz][index] = true;
					unknown--;
				}
			}
		}

		int[] frontier = new int[pw * ph];
		float[] filled = new float[pw * ph];
		while (unknown > 0) {
			int grew = 0;
			for (int z = 0; z < ps; z++) {
				int found = 0;
				for (int y = 0; y < ph; y++) {
					for (int x = 0; x < pw; x++) {
						int index = y * pw + x;
						if (known[z][index]) {
							continue;
						}
						double sum = 0d;
						int count = 0;
						for (int dz = -1; dz <= 1; dz++) {
							int zz = z + dz;
							if (zz < 0 || zz >= ps) {
								continue;
							}
							for (int dy = -1; dy <= 1; dy++) {
								int yy = y + dy;
								if (yy < 0 || yy >= ph) {
									continue;
								}
								for (int dx = -1; dx <= 1; dx++) {
									int xx = x + dx;
									if (xx < 0 || xx >= pw) {
										continue;
									}
									int neighbour = yy * pw + xx;
									if (known[zz][neighbour]) {
										sum += value[zz][neighbour];
										count++;
									}
								}
							}
						}
						if (count > 0) {
							frontier[found] = index;
							filled[found] = (float) (sum / count);
							found++;
						}
					}
				}
				// commit after the sweep, so that one round only reads the previous state
				for (int i = 0; i < found; i++) {
					value[z][frontier[i]] = filled[i];
					known[z][frontier[i]] = true;
				}
				grew += found;
				unknown -= found;
			}
			if (grew == 0) {
				// nothing touches the known region any more, the rest stays at zero
				break;
			}
		}

		ImageStack out = new ImageStack(pw, ph);
		for (int z = 0; z < ps; z++) {
			FloatProcessor ip = new FloatProcessor(pw, ph);
			for (int i = 0; i < pw * ph; i++) {
				ip.setf(i % pw, i / pw, value[z][i]);
			}
			out.addSlice(ip);
		}
		ImagePlus padded = new ImagePlus(image.getTitle() + "_padded", out);
		padded.setCalibration(image.getCalibration().copy());
		return padded;
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
}
