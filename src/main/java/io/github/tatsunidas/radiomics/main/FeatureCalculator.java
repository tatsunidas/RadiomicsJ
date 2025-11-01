package io.github.tatsunidas.radiomics.main;

import ij.ImagePlus;

public interface FeatureCalculator {
	Double calculate(ImagePlus img, ImagePlus mask);
}
