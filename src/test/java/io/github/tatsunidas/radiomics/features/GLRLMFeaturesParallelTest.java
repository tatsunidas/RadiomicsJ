package io.github.tatsunidas.radiomics.features;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.FloatProcessor;

/**
 * The parallel calculation of the 13 directions (GLRLMFeatures.parallel = true) must give exactly the same values as
 * the sequential calculation of the original code path (parallel = false): the matrices are integer run counts.
 */
class GLRLMFeaturesParallelTest {

	/** Random intensities and a random blob-like mask (label 1) with holes, so that runs are cut and the mask is sparse. */
	private static ImagePlus[] volume(long seed, int w, int h, int s, int levels, double fill) {
		Random r = new Random(seed);
		ImageStack img = new ImageStack(w, h);
		ImageStack msk = new ImageStack(w, h);
		for (int z = 0; z < s; z++) {
			float[] ip = new float[w * h];
			float[] mp = new float[w * h];
			for (int i = 0; i < w * h; i++) {
				// runs of equal intensity are likely (blocky noise), otherwise every run would have length 1
				ip[i] = (r.nextInt(100) < 55 && i > 0) ? ip[i - 1] : r.nextInt(levels) * 10f;
				mp[i] = r.nextDouble() < fill ? 1f : 0f;
			}
			img.addSlice(new FloatProcessor(w, h, ip));
			msk.addSlice(new FloatProcessor(w, h, mp));
		}
		return new ImagePlus[] { new ImagePlus("img", img), new ImagePlus("mask", msk) };
	}

	private static TreeMap<String, Long> features(ImagePlus[] v, int nBins, boolean parallel) throws Exception {
		boolean before = GLRLMFeatures.parallel;
		GLRLMFeatures.parallel = parallel;
		try {
			GLRLMFeatures f = new GLRLMFeatures(v[0].duplicate(), v[1].duplicate(), 1, true, nBins, null, null);
			TreeMap<String, Long> out = new TreeMap<>();
			for (GLRLMFeatureType t : GLRLMFeatureType.values()) {
				Double d = f.calculate(t.id());
				out.put(t.name(), Double.doubleToLongBits(d == null ? Double.NaN : d));
			}
			return out;
		} finally {
			GLRLMFeatures.parallel = before;
		}
	}

	private static void assertSame(ImagePlus[] v, int nBins) throws Exception {
		TreeMap<String, Long> seq = features(v, nBins, false);
		TreeMap<String, Long> par = features(v, nBins, true);
		assertFalse(seq.isEmpty());
		assertEquals(seq.keySet(), par.keySet());
		for (String id : seq.keySet()) {
			assertEquals(seq.get(id), par.get(id), id + ": sequential " + Double.longBitsToDouble(seq.get(id))
					+ " vs parallel " + Double.longBitsToDouble(par.get(id)));
		}
	}

	@Test
	void parallelEqualsSequential_smallPhantomLikeVolume() throws Exception {
		assertSame(volume(1, 5, 4, 4, 6, 0.9), 6);
	}

	@Test
	void parallelEqualsSequential_randomVolumes() throws Exception {
		assertSame(volume(2, 16, 14, 9, 8, 0.6), 8);
		assertSame(volume(3, 24, 20, 12, 16, 0.8), 16);
		assertSame(volume(4, 13, 11, 7, 4, 0.35), 4);
	}

	@Test
	void parallelIsRepeatable() throws Exception {
		ImagePlus[] v = volume(5, 18, 16, 10, 10, 0.7);
		TreeMap<String, Long> first = features(v, 10, true);
		for (int i = 0; i < 8; i++) {
			assertEquals(first, features(v, 10, true), "repeat " + i);
		}
	}

	@Test
	void theTestSeesRunsLongerThanOne() throws Exception {
		// Guard against a vacuous comparison: the volume must actually contain runs (not only length 1).
		TreeMap<String, Long> f = features(volume(6, 16, 14, 9, 4, 0.9), 4, false);
		String key = null;
		for (String k : f.keySet()) {
			if (k.equals(GLRLMFeatureType.LongRunEmphasis.name())) {
				key = k;
			}
		}
		assertTrue(key != null, "feature ids: " + f.keySet());
		double longRunEmphasis = Double.longBitsToDouble(f.get(key));
		assertTrue(longRunEmphasis > 1.0, "long run emphasis " + longRunEmphasis);
	}
}
