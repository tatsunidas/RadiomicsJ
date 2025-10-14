package io.github.tatsunidas.radiomics.features;

import java.util.HashMap;
import java.util.Map;

import ij.ImagePlus;

/**
 * 
 * @author tatsunidas
 *
 */
public abstract class AbstractRadiomicsFeature implements RadiomicsFeature{
	
	ImagePlus img; ImagePlus mask;
	/**
	 * Params to calculate any features
	 * also see RadiomicsFeature interface.
	 */
	Map<String, Object> settings;
	
	/**
	 * Should set all settings in child class.
	 */
	public AbstractRadiomicsFeature() {
		this.settings = new HashMap<>();
	}
	
	public AbstractRadiomicsFeature(ImagePlus img, ImagePlus mask, Map<String, Object> settings) {
		if(img == null || img.getNSlices() == 0) {
			throw new IllegalArgumentException("AbstractRadiomicsFeature: Image is null or no slices, please check input images.");
		}
		
		if (img.getType() == ImagePlus.COLOR_RGB) {
			throw new IllegalArgumentException("RadiomicsJ can read only grayscale images(8/16/32 bits)...sorry.");
		}
		
		if (mask != null) {
			if (img.getWidth() != mask.getWidth() || img.getHeight() != mask.getHeight() || img.getNSlices() != mask.getNSlices()) {
				throw new IllegalArgumentException("AbstractRadiomicsFeature: please input same dimension image and mask.");
			}
		}
		
		if(settings == null) {
			this.settings = new HashMap<>();
		}else {
			this.settings = settings;
		}
		this.img = img;
		/**
		 * In here, mask is null-able.
		 */
		this.mask = mask;
	}
}
