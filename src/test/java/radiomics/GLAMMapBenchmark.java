package radiomics;

import java.util.HashMap;
import java.util.Map;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import io.github.tatsunidas.radiomics.features.GLAMFeatureType;
import io.github.tatsunidas.radiomics.features.GLAMFeatures;
import io.github.tatsunidas.radiomics.features.GLCMFeatureType;
import io.github.tatsunidas.radiomics.features.GLCMFeatures;
import io.github.tatsunidas.radiomics.features.RadiomicsFeature;
import io.github.tatsunidas.radiomics.main.FeatureCalculator;
import io.github.tatsunidas.radiomics.main.FeatureCalculatorFactory;
import io.github.tatsunidas.radiomics.main.FeatureSpecifier;
import io.github.tatsunidas.radiomics.main.FeatureVisualizationMap;

/**
 * How long a feature visualization map takes, and how that changes with the
 * kernel, the number of bins and the number of threads.
 *
 * GLAM walks every pair of voxels inside a window, so its cost grows with the
 * sixth power of the kernel width. This measures where that stops being
 * practical, which is the number a caller needs before it offers GLAM maps to
 * anyone.
 *
 * <pre>
 * mvn test-compile exec:java -Dexec.mainClass=radiomics.GLAMMapBenchmark -Dexec.classpathScope=test
 * </pre>
 */
public class GLAMMapBenchmark {

	/** Side of the volume the map is computed over. */
	private static final int WIDTH = 64;
	private static final int HEIGHT = 64;
	private static final int DEPTH = 24;
	/** Side of the centred box the roi covers. */
	private static final int ROI_W = 32;
	private static final int ROI_H = 32;
	private static final int ROI_D = 12;

	public static void main(String[] args) {
		ImagePlus image = blockyPhantom(WIDTH, HEIGHT, DEPTH, 8, 4);
		ImagePlus mask = boxMask(WIDTH, HEIGHT, DEPTH, ROI_W, ROI_H, ROI_D, 1);

		System.out.println("volume " + WIDTH + "x" + HEIGHT + "x" + DEPTH + ", roi " + ROI_W + "x" + ROI_H + "x"
				+ ROI_D + " = " + (ROI_W * ROI_H * ROI_D) + " voxels, cores "
				+ Runtime.getRuntime().availableProcessors());
		System.out.println();

		System.out.println("GLCM_JointEntropy, the baseline family");
		System.out.printf("%-10s %-8s %-10s %12s%n", "kernel", "stride", "threads", "elapsed ms");
		for (int stride : new int[] { 1, 3 }) {
			for (boolean parallel : new boolean[] { false, true }) {
				FeatureVisualizationMap.parallel = parallel;
				long ms = time(image, mask, glcm(16), 7, stride);
				System.out.printf("%-10d %-8d %-10s %12d%n", 7, stride, parallel ? "many" : "one", ms);
			}
		}
		System.out.println();

		System.out.println("GLAM_SecondVirialCoefficient_Mean, maxRadius = kernel / 2");
		System.out.printf("%-10s %-8s %-8s %-10s %12s%n", "kernel", "bins", "stride", "threads", "elapsed ms");
		for (int kernel : new int[] { 7, 11, 15 }) {
			for (int bins : new int[] { 8, 16 }) {
				for (int stride : new int[] { 1, 3 }) {
					FeatureVisualizationMap.parallel = true;
					long ms = time(image, mask, glam(bins, kernel / 2), kernel, stride);
					System.out.printf("%-10d %-8d %-8d %-10s %12d%n", kernel, bins, stride, "many", ms);
				}
			}
		}
		System.out.println();

		// what the single matrix mode is worth: one feature against all 150
		System.out.println("GLAM, cost of one matrix against every matrix (kernel 7, bins 16, stride 3)");
		FeatureVisualizationMap.parallel = true;
		long one = time(image, mask, glam(16, 3), 7, 3);
		long all = time(image, mask, glamEveryFeature(16, 3), 7, 3);
		System.out.printf("one matrix   %8d ms%n", one);
		System.out.printf("all matrices %8d ms%n", all);
		System.out.printf("ratio        %8.1fx%n", all / (double) Math.max(1, one));
	}

	private static long time(ImagePlus image, ImagePlus mask, FeatureCalculator calculator, int kernel, int stride) {
		long start = System.currentTimeMillis();
		FeatureVisualizationMap.generateFeatureMap(image, mask, -1, calculator, kernel, false, stride);
		return System.currentTimeMillis() - start;
	}

	private static FeatureCalculator glcm(int bins) {
		Map<String, Object> settings = baseSettings(bins);
		settings.put(RadiomicsFeature.DELTA, Integer.valueOf(1));
		return new FeatureCalculatorFactory()
				.create(new FeatureSpecifier<>(GLCMFeatures.class, GLCMFeatureType.JointEntropy, settings));
	}

	private static FeatureCalculator glam(int bins, int maxRadius) {
		Map<String, Object> settings = baseSettings(bins);
		settings.put(RadiomicsFeature.GLAM_MAX_RADIUS, Integer.valueOf(maxRadius));
		return new FeatureCalculatorFactory().create(new FeatureSpecifier<>(GLAMFeatures.class,
				GLAMFeatureType.SecondVirialCoefficient_Mean, settings));
	}

	/**
	 * Reads every one of the 150 features out of one window, which forces all
	 * nineteen matrices and stands for what the lazy build saves.
	 */
	private static FeatureCalculator glamEveryFeature(int bins, int maxRadius) {
		Map<String, Object> settings = baseSettings(bins);
		settings.put(RadiomicsFeature.GLAM_MAX_RADIUS, Integer.valueOf(maxRadius));
		return (subVolume, subMask) -> {
			GLAMFeatures glam = new GLAMFeatures(subVolume, subMask, new HashMap<>(settings));
			double last = Double.NaN;
			for (GLAMFeatureType type : GLAMFeatureType.values()) {
				Double value = glam.calculate(type.id());
				if (value != null) {
					last = value;
				}
			}
			return last;
		};
	}

	private static Map<String, Object> baseSettings(int bins) {
		Map<String, Object> settings = new HashMap<>();
		settings.put(RadiomicsFeature.LABEL, Integer.valueOf(1));
		settings.put(RadiomicsFeature.USE_BIN_COUNT, Boolean.TRUE);
		settings.put(RadiomicsFeature.nBins, Integer.valueOf(bins));
		return settings;
	}

	/**
	 * Gray levels laid out in cubes, so that a window sees real structure rather
	 * than noise and the peak search has something to find.
	 */
	private static ImagePlus blockyPhantom(int w, int h, int d, int blockSize, int levels) {
		ImageStack stack = new ImageStack(w, h);
		for (int z = 0; z < d; z++) {
			FloatProcessor ip = new FloatProcessor(w, h);
			for (int y = 0; y < h; y++) {
				for (int x = 0; x < w; x++) {
					int block = (x / blockSize) + (y / blockSize) + (z / blockSize);
					ip.setf(x, y, (block % levels) * 100f);
				}
			}
			stack.addSlice(ip);
		}
		ImagePlus imp = new ImagePlus("blocks", stack);
		imp.getCalibration().pixelWidth = 1d;
		imp.getCalibration().pixelHeight = 1d;
		imp.getCalibration().pixelDepth = 1d;
		return imp;
	}

	private static ImagePlus boxMask(int w, int h, int d, int roiW, int roiH, int roiD, int label) {
		int x0 = (w - roiW) / 2;
		int y0 = (h - roiH) / 2;
		int z0 = (d - roiD) / 2;
		ImageStack stack = new ImageStack(w, h);
		for (int z = 0; z < d; z++) {
			ByteProcessor ip = new ByteProcessor(w, h);
			if (z >= z0 && z < z0 + roiD) {
				for (int y = y0; y < y0 + roiH; y++) {
					for (int x = x0; x < x0 + roiW; x++) {
						ip.set(x, y, label);
					}
				}
			}
			stack.addSlice(ip);
		}
		ImagePlus imp = new ImagePlus("box", stack);
		imp.getCalibration().pixelWidth = 1d;
		imp.getCalibration().pixelHeight = 1d;
		imp.getCalibration().pixelDepth = 1d;
		return imp;
	}
}
