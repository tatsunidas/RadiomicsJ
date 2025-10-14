package io.github.tatsunidas.radiomics.features;

import java.util.Map;
import java.util.Set;


public interface RadiomicsFeature {
	
	public static final String IMAGE = "IMAGE";
	public static final String MASK = "MASK";
	public static final String DISC_IMG = "DISC_IMG";
	public static final String ISO_MASK = "ISO_MASK";
	public static final String nBins = "nBins";
	public static final String USE_BIN_COUNT = "UseBinCount";
	public static final String BinWidth = "BinWidth";
	public static final String LABEL = "LABEL";
	public static final String WEIGHTING_NORM = "WEIGHTING_NORM";
	public static final String ALPHA = "ALPHA";
	public static final String DELTA = "DELTA";
	
	public static final String SLICE = "SLICE";
	
	//GLKZM ImagePlus kinetics, boolean useBinCount, Integer kinetics_nBins, Double binWidth
	public static final String KINETICS_IMG = "KINETICS_IMG";
	public static final String KINETICS_USE_BIN_COUNT = "KINETICS_USE_BIN_COUNT";
	public static final String KINETICS_nBins = "KINETICS_nBins";
	public static final String KINETICS_BinWidth = "KINETICS_BinWidth";
	
	public static final String IVH_MODE = "IVH_DISCRITIZATION_MODE";
	public static final String BinWidth_IVH = "BinWidth_IVH";
	public static final String BinCount_IVH = "BinCount_IVH";
	
	public static final String BOX_SIZES = "BOX_SIZES";
	
	/**
	 * calculate feature
	 * @param featureId
	 * @return
	 */
    Double calculate(String featureId);
    
    /**
     * 
     * @return available feature names
     */
    Set<String> getAvailableFeatures();
    
    /**
     * what is family name. 
     * @return such as GLCM
     */
    String getFeatureFamilyName();
    
    /**
     * how to calculate with. 
     * @return
     */
    Map<String, Object> getSettings();
    
    void buildup(Map<String, Object> settings);
}
