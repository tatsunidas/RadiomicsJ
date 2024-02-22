/*
 * Copyright 2022 Tatsuaki Kobayashi

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package io.github.tatsunidas.radiomics.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import javax.swing.JOptionPane;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.ResultsTable;
import ij.process.ImageProcessor;
import ij.util.DicomTools;
import io.github.tatsunidas.radiomics.features.DiagnosticsInfo;
import io.github.tatsunidas.radiomics.features.DiagnosticsInfoType;
import io.github.tatsunidas.radiomics.features.FractalFeatureType;
import io.github.tatsunidas.radiomics.features.FractalFeatures;
import io.github.tatsunidas.radiomics.features.GLCMFeatureType;
import io.github.tatsunidas.radiomics.features.GLCMFeatures;
import io.github.tatsunidas.radiomics.features.GLDZMFeatureType;
import io.github.tatsunidas.radiomics.features.GLDZMFeatures;
import io.github.tatsunidas.radiomics.features.GLRLMFeatureType;
import io.github.tatsunidas.radiomics.features.GLRLMFeatures;
import io.github.tatsunidas.radiomics.features.GLSZMFeatureType;
import io.github.tatsunidas.radiomics.features.GLSZMFeatures;
import io.github.tatsunidas.radiomics.features.IntensityBasedStatisticalFeatureType;
import io.github.tatsunidas.radiomics.features.IntensityBasedStatisticalFeatures;
import io.github.tatsunidas.radiomics.features.IntensityHistogramFeatureType;
import io.github.tatsunidas.radiomics.features.IntensityHistogramFeatures;
import io.github.tatsunidas.radiomics.features.IntensityVolumeHistogramFeatureType;
import io.github.tatsunidas.radiomics.features.IntensityVolumeHistogramFeatures;
import io.github.tatsunidas.radiomics.features.LocalIntensityFeatureType;
import io.github.tatsunidas.radiomics.features.LocalIntensityFeatures;
import io.github.tatsunidas.radiomics.features.MorphologicalFeatureType;
import io.github.tatsunidas.radiomics.features.MorphologicalFeatures;
import io.github.tatsunidas.radiomics.features.NGLDMFeatureType;
import io.github.tatsunidas.radiomics.features.NGLDMFeatures;
import io.github.tatsunidas.radiomics.features.NGTDMFeatureType;
import io.github.tatsunidas.radiomics.features.NGTDMFeatures;
import io.github.tatsunidas.radiomics.features.OperationalInfoFeatures;
import io.github.tatsunidas.radiomics.features.Shape2DFeatureType;
import io.github.tatsunidas.radiomics.features.Shape2DFeatures;

/**
 * 
 * @author tatsunidas <t_kobayashi@vis-ionary.com>
 *
 */
public class RadiomicsJ {

	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		
		org.apache.commons.cli.Options options = new org.apache.commons.cli.Options();
		options.addOption("i","images",true, "path to images dir(folder)");
		options.addOption("m","masks",true, "path to masks dir(folder)");
		options.addOption("o","output",true, "path to save result location dir(not file). default is user folder.");
		options.addOption("s","settings",true, "path to setting file(.properties)");
		options.addOption("h","help",false, "print usage information.");
		options.addOption("d","debug",false, "debug mode");
		options.addOption("t","test",false, "test mode");
		options.addOption("tdt","test-data-type",true, "test data type, 0:digital_phantom1, 1:ct_sample1");
		
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = null;
		try {
			cmd = parser.parse(options, args);
		} catch (ParseException e1) {
			e1.printStackTrace();
			System.exit(0);
		}
		
		boolean debug = false;
		boolean testMode = false;
		String imgFolderPath = null;
		String maskFolderPath = null;
		String outputDestPath = null;//System.getProperty("user.dir");//default
		String propFilePath = null;//settings file
		
		if(cmd.hasOption("h") || cmd.hasOption("help")) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("RadiomicsJ", options);
			System.exit(0);
		}
		if(cmd.hasOption("i") || cmd.hasOption("images")){
			imgFolderPath = cmd.getOptionValue("images");
		}
		if(cmd.hasOption("m") || cmd.hasOption("masks")){
			maskFolderPath = cmd.getOptionValue("masks");
		}
		if(cmd.hasOption("o") || cmd.hasOption("output")){
			outputDestPath = cmd.getOptionValue("output");
			if(outputDestPath == null || outputDestPath.replace("　","").trim().length()==0) {
				outputDestPath = System.getProperty("user.dir");
			}
		}
		if(cmd.hasOption("s") || cmd.hasOption("settings")){
			propFilePath = cmd.getOptionValue("settings");
		}
		if(cmd.hasOption("d") || cmd.hasOption("debug")){
			debug = true;
		}
		if(cmd.hasOption("t") || cmd.hasOption("test")){
			testMode = true;
		}
		if(cmd.hasOption("tdt") || cmd.hasOption("test-data-type")){
			String tdt = cmd.getOptionValue("tdt");
			tdt = tdt.replace(" ", "").replace("　", "");
			if(tdt != null && tdt.length()!=0) {
				try {
					testDataType = Integer.parseInt(tdt);
				}catch(NumberFormatException e) {
					//do nothing
				}
			}
		}
		
		if(imgFolderPath == null || maskFolderPath == null) {
			if(!testMode) {
				System.out.println("Sorry, can not read image or mask folders.");
				System.out.println("Please check input paths,");
				System.exit(0);
			}
		}
		
		File imgDir = null;
		File maskDir = null;
		if(!testMode) {
			imgDir = new File(imgFolderPath);
			maskDir = new File(maskFolderPath);
			if(imgDir.isDirectory() && maskDir.isDirectory()) {
				if (imgDir.listFiles().length != maskDir.listFiles().length) {
					System.out.println("Sorry, can not read correctly image and mask file pairs.");
					System.out.println(
							"Please check input images and masks, Are these same size(same number of slices) ?? (or, contained no need files ?)");
					System.out.println(
							imgDir.listFiles().length + " images, " + maskDir.listFiles().length + " masks found.");
					System.exit(0);
				}
			}
		}
		
		RadiomicsJ radiomics = new RadiomicsJ();
		radiomics.setDebug(debug);
		radiomics.loadSettings(propFilePath);
		
		if(!testMode) {
			try {
				ResultsTable res = radiomics.execute(imgDir, maskDir, RadiomicsJ.targetLabel);
				if (res != null) {
					res.show(RadiomicsJ.resultWindowTitle);
					if(outputDestPath !=null) {
						if(outputDestPath.endsWith(".csv")) {
							res.save(outputDestPath);
							System.out.println("finish calculation, result save to " + outputDestPath);
						}else {
							DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");  
							LocalDateTime now = LocalDateTime.now();  
							String timeStamp = dtf.format(now);
							File outDest = new File(outputDestPath);
							if(!outDest.exists()) {
								outDest.mkdirs();
							}
							res.save(outDest.getAbsolutePath() + File.separator + "RadiomicsFeatures-"+timeStamp+".csv");
							System.out.println("finish calculation, result save to " + outDest.getAbsolutePath() + File.separator + "RadiomicsFeatures-"+timeStamp+".csv");
						}
					}
				}else {
					JOptionPane.showMessageDialog(null, "RadiomicsJ can not perform feature extraction. Please check image, mask and settings.");
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(0);
			}
		}else {
			ImagePlus imp = null;
			ImagePlus mask = null;
			if(testDataType == 0) {
				ImagePlus[] ds = TestDataLoader.digital_phantom1();
				imp = ds[0];
				mask = ds[1];
			}else if(testDataType == 1) {
				ImagePlus[] ds = TestDataLoader.sample_ct1();
				imp = ds[0];
				mask = ds[1];
			}else if(testDataType == 2) {
				ImagePlus[] ds = TestDataLoader.validationDataAt("001", "PET");
				imp = ds[0];
				mask = ds[1];
			}
			try {
				ResultsTable res = radiomics.execute(imp, mask, RadiomicsJ.targetLabel);
				if(res != null) {
					res.show(resultWindowTitle);
				}
				if (res != null && outputDestPath != null && outputDestPath.length()!=0) {
					res.save(outputDestPath + File.separator + "RadiomicsFeatures.csv");
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(0);
			}
		}
	}
	
	/**
	 * radiomicsj version
	 */
	public static String version = "2.1.3";
	
	/**
	 * use test data
	 */
	public static boolean debug = false;
	
	/**
	 * result window title
	 */
	public static final String resultWindowTitle = "Radiomics Features";
	
	/**
	 * original input images (stack)
	 */
	private ImagePlus originalImp;
	
	/**
	 * original input masks (stack)
	 */
	private ImagePlus originalMask;
	
	/**
	 * Analysis images using float processor.
	 */
	private ImagePlus currentImp;
	
	/**
	 * Analysis masks using float processor.
	 */
	private ImagePlus currentMask;
	
	/**
	 * resampled images
	 */
	private ImagePlus resampledImp;
	
	/**
	 * resampled masks
	 */
	private ImagePlus resampledMask;
	
	/**
	 * Resegmented mask by;
	 * 1: range filter
	 * 2: remove outliers
	 */
	private ImagePlus resegmentedMask;
	
	/**
	 * discretise images
	 */
	public static ImagePlus discretiseImp;
	
	/**
	 * isovoxelized mask.
	 * created after resampling and re-segment.
	 */
	protected static ImagePlus isoMask;
	
//	/**
//	 * resegmented voxels in Roi.
//	 * if normalized is on, insert normalized voxels
//	 */
//	protected static double[] resegmented_voxels;
//	
//	/**
//	 * re-segment and discretize voxels in Roi.
//	 * if normalized is on, as normalized voxels
//	 */
//	protected static double[] resegmented_discretised_voxels;
	
	/**
	 * 0 : digital phantom1
	 * 1 : sample ct1
	 */
	static int testDataType = 0;
	
	/**
	 * interpolation for image.
	 * NEAREST_NEIGHBOR=0, BILINEAR=1, BICUBIC=2;
	 */
	public static int interpolation2D = ImageProcessor.NEAREST_NEIGHBOR;
	public static int interpolation_mask2D = ImageProcessor.NEAREST_NEIGHBOR;
	
	
	public static final int TRILINEAR = 100;//3D
	public static final int NEAREST3D = 101;//3D
	public static final int TRICUBIC_SPLINE = 102;//3D
	public static final int TRICUBIC_POLYNOMIAL = 103;//3D
	
	public static int interpolation3D = TRILINEAR;
	public static int interpolation_mask3D = TRILINEAR;
	
	/**
	 * re-identify mask label value in voxels after interpolation.
	 * mask label >= mask, and mask < (mask label+PartialVolumeThareshold)
	 */
	public static double mask_PartialVolumeThareshold = 0.5;
	
	/**
	 * 1 - 255 : enable label number.
	 * Target label value to specify voxels in roi.
	 * When preprocess, mask is created by this label.
	 * Then mask pixel value was convert to (1) which is defined by following label_.
	 */
	public static Integer targetLabel = 1;
	
	/**
	 * When calculate features, label value always to be 1.
	 * Because to deal with partial volume effects by resampling. 
	 */
	public static final Integer label_ = 1;//label to compute
	
	/**
	 * Standardize original images at preprocessing 
	 */
	public static boolean normalize = false;
	
	/**
	 * remove outlier in original image at preprocessing
	 */
	public static boolean removeOutliers = false;
	
	/**
	 * if removeOutliers is true,
	 * normalized(standardized) pixel value is set limited range between,
	 * (mean - z_scre*sd) <= norm_x <= (mean + z_scre*sd).
	 * if norm_x > (mean + z_scre*sd), then norm_x = (mean + z_scre*sd).
	 * if norm_x < (mean - z_scre*sd), then norm_x = (mean - z_scre*sd).
	 * 
	 * z_score means (two-sided tails)...
	 * 80%CI=1.28
	 * 90%CI=1.645
	 * 95%CI=1.96 
	 * 95.45%CI=2.0
	 * 99%CI=2.58
	 * 99.73%CI=3.0(default)
	 */
	public static double zScore = 3d;
	
	/**
	 * scale to normalizeScale * ((X - mean)/sd)
	 */
	public static double normalizeScale = 1.0;
	
	/**
	 * Only used for to calculate energy, total energy, RMS in intensity based statistical features.
	 * No negative value(>=0, in unit value).
	 */
	public static Double densityShift = 0d;
	
	/**
	 * re-segmentation range min pixel value, in unit value(e.g, HU).
	 */
	public static Double rangeMin = null;// in unit value.
	
	/**
	 * re-segmentation range max pixel value, in unit value(e.g, HU).
	 */
	public static Double rangeMax = null;// in unit value.

	/**
	 * if true, binCount discrete is performed (and binWidth is ignored).
	 */
	public static boolean BOOL_USE_FixedBinNumber = true;
	
	/**
	 * fixed bin size ( 32 default, 6 to 64 is suitable range).
	 * see, Utils.getNumOfBinsByMinMaxRange()
	 */
	public static Integer nBins = 32;
	
	/**
	 * fixed bin width
	 * to calculate nBins ((Xgl - Xgl[min])/binWidth)+1.
	 * Value is should specified in unit.
	 */
	public static Double binWidth = 25d;//in unit.
	
	static double[] resamplingFactorXYZ=null;
	
	/**
	 * num of bins to calculate IVH features under FBN discrete.
	 */
	public static Integer IVH_binCount = 1000;
	
	/**
	 * bin width to calculate IVH features under FBS discrete.
	 */
	public static Double IVH_binWidth = 2.5;
	
	/**
	 * IVH_mode 0 : no discrete
	 * IVH mode 1 : binWidth discrete
	 * IVH mode 2 : binCount discrete
	 */
	public static Integer IVH_mode = 0;
	
	/**
	 * Texture param:NGLDM coarseness
	 */
	public static Integer alpha = 0;
	
	/**
	 * Texture param:GLCM delta (distance)
	 */
	public static Integer deltaGLCM = 1;
	
	/**
	 * Texture param: NGTDM delta (distance)
	 */
	public static Integer deltaNGToneDM = 1;//distance
	
	/**
	 * Texture param:NGLDM delta (distance)
	 */
	public static Integer deltaNGLevelDM = 1;//distance
	
	/**
	 * (Not implemented)
	 * weighting norm type
	 */
	public static final String[] weighting_norms = new String[] { "no_weighting", "manhattan", "euclidian", "infinity" };
	
	/**
	 * (Not implemented)
	 * weighting norm
	 */
	public static String weightingNorm = null;
	
	/**
	 * To calculate fractal D.
	 * default [2,3,4,6,8,12,16,32,64]
	 * null-able.
	 */
	public static int[] box_sizes = null;
	
	/**
	 * calculate slice by slice and no aggregation.
	 */
	public static boolean force2D = false;
	
	/**
	 * if you want no default features (such as MoransIIndex etc), turn this on. 
	 */
	public static boolean activate_no_default_features = false;
	
	//features
	
	/**
	 * if true, calculate operational info and output to result table.
	 */
	boolean BOOL_enableOperationalInfo = true;
	
	/**
	 * if true, calculate diagnostics info (voxel info) and output to result table.
	 */
	boolean BOOL_enableDiagnostics = true;
	
	/**
	 * if true, calculate morphological features and output to result table.
	 */
	boolean BOOL_enableMorphological = true;
	
	/**
	 * and so on
	 */
	boolean BOOL_enableLocalIntensityFeatures = true;
	boolean BOOL_enableIntensityBasedStatistics = true;
	boolean BOOL_enableIntensityHistogram = true;
	boolean BOOL_enableIntensityVolumeHistogram = true;
	boolean BOOL_enableGLCM = true;
	boolean BOOL_enableGLRLM = true;
	boolean BOOL_enableGLSZM = true;
	boolean BOOL_enableGLDZM = true;
	boolean BOOL_enableNGTDM = true;
	boolean BOOL_enableNGLDM = true;
	boolean BOOL_enableFractal = true;
	
	/**
	 * Not implemented.
	 */
	boolean BOOL_enableHomological = false;
	
	/**
	 * if force2D is true, can enable to calculate shape2d.
	 * else, ignored.
	 */
	boolean BOOL_enableShape2D = false;
	
	/**
	 * Handle to specify features will be excluded.
	 */
	private HashSet<String> excluded;
	
	/**
	 * deprecated features
	 * 
	 * These features cannot calculate.
	 * 
	 * MorphologicalFeatureType.VolumeDensity_OrientedMinimumBoundingBox
	 * MorphologicalFeatureType.AreaDensity_OrientedMinimumBoundingBox
	 * MorphologicalFeatureType.VolumeDensity_MinimumVolumeEnclosingEllipsoid
	 * MorphologicalFeatureType.AreaDensity_MinimumVolumeEnclosingEllipsoid
	 * IntensityVolumeHistogramFeatureType.AreaUnderTheIVHCurve
	 * NGLDMFeatureType.DependenceCountPercentage
	 */
	public static final String[] excluded_list = new String[] {
			/*Morphplogical feature*/
			MorphologicalFeatureType.VolumeDensity_OrientedMinimumBoundingBox.name(),
			MorphologicalFeatureType.AreaDensity_OrientedMinimumBoundingBox.name(),
			MorphologicalFeatureType.VolumeDensity_MinimumVolumeEnclosingEllipsoid.name(),
			MorphologicalFeatureType.AreaDensity_MinimumVolumeEnclosingEllipsoid.name(),
			/*IntensityVolumeHistogramFeature*/
			IntensityVolumeHistogramFeatureType.AreaUnderTheIVHCurve.name(),
			/*NGLDM*/
			NGLDMFeatureType.DependenceCountPercentage.name(),
	};
	
	/**
	 * No default features are not included as default.
	 * because calculation cost(time) much high (take too long time to calculate).
	 * if you use these features, turn on activate_include_no_default=true.
	 * 
	 * MorphologicalFeatureType.MoransIIndex
	 * MorphologicalFeatureType.GearysCMeasure
	 */
	public static final String[] no_default_list = new String[] {
			MorphologicalFeatureType.MoransIIndex.name(),
			MorphologicalFeatureType.GearysCMeasure.name(),
	};
	
	/**
	 * Default constructor
	 */
	public RadiomicsJ() {
		initExcludeList();
	}
	
	/**
	 * if true, debug mode on
	 * @param debug
	 */
	public void setDebug(boolean debug) {
		RadiomicsJ.debug = debug;
	}
	
	/**
	 * initialize exclude features list.
	 * 
	 */
	private void initExcludeList() {
		excluded = new HashSet<String>();
		for(String excludedName : excluded_list) {
			excluded.add(excludedName);
		}
		if(!activate_no_default_features) {
			for(String nodefaultName : no_default_list) {
				excluded.add(nodefaultName);
			}
		}
	}
	
	/**
	 * 
	 * @return excluded features
	 */
	public HashSet<String> getExcludedFeatures(){
		return excluded;
	}
	
	/**
	 * 
	 * @param name feature name
	 */
	public void removeExcludedFeatures(String name) {
		if(excluded.contains(name)) {
			ArrayList<String> remove = new ArrayList<String>();
			remove.add(name);
			excluded.removeAll(remove);
		}
	}
	
	/**
	 * 
	 */
	public void activateNoDefaultFeatures() {
		for(String name:no_default_list) {
			removeExcludedFeatures(name);
		}
	}
	
	/**
	 * 
	 * @param propFilePath
	 */
	public void loadSettings(String propFilePath) {
		if (propFilePath == null) {
			return;
		}
		File propFile = null;
		Properties prop = null;
		if (propFilePath != null) {
			propFile = new File(propFilePath);
			if (propFile == null || !propFile.exists()) {
				System.out.println("Sorry, can not read properties file correctly.");
				System.out.println("Please check input paths.");
				System.exit(0);
			} else {
				try (FileInputStream in = new FileInputStream(propFile);) {
					prop = new Properties();
					prop.load(in);
				} catch (IOException e) {
					System.out.println("Ouch, fail to read properties file...");
					e.printStackTrace();
					System.exit(0);
				}
			}
		}
		loadSettings(prop);
	}
	
	/**
	 * 
	 * @param propFilePathInResource validation/***.properties (no need slash"/" in head) 
	 */
	public void loadSettingsFromResource(String propFilePathInResource) {
		if (propFilePathInResource == null) {
			return;
		}
		URL refUrl = RadiomicsJ.class.getClassLoader().getResource(propFilePathInResource);
		File propFile = null;
		try {
			propFile = new File(refUrl.toURI());
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		Properties prop = null;
		if (propFile == null || !propFile.exists()) {
			System.out.println("Sorry, can not read properties file correctly.");
			System.out.println("Please check input paths.");
			System.exit(0);
		} else {
			try (FileInputStream in = new FileInputStream(propFile);) {
				prop = new Properties();
				prop.load(in);
			} catch (IOException e) {
				System.out.println("Ouch, fail to read properties file...");
				e.printStackTrace();
				System.exit(0);
			}
		}
		loadSettings(prop);
	}
	
	/**
	 * Load calculation settings
	 * @param prop properties
	 */
	public void loadSettings(Properties prop) {
		if(prop == null) {
			System.out.println("properties file is null. Calculation is performed by using default.");
			return;
		}
		Set<Object> keys = prop.keySet();
		for(Object key : keys) {
			if(key instanceof String) {
				String keyString = (String)key;
				String val = prop.getProperty(keyString);
				if(val == null) {
					continue;
				}
				val = val.replace(" ", "");
				val = val.replace("　", "");
				val = val.replace("\n", "");
				val = val.replace("\t", "");
				if(val != null && val.length()!=0) {
					if(keyString.equals(SettingParams.INT_label.name())) {
						try{
							Integer n = Integer.valueOf(val);
							if(n < 1) {
								n = 1;
							}else if(n>255){
								n=255;
							}
							RadiomicsJ.targetLabel = n;
						}catch(NumberFormatException e) {
							//keep default
							continue;
						}
					}
					if(keyString.equals(SettingParams.INT_binCount.name())) {
						try{
							Integer n = Integer.valueOf(val);
							if(n < 1) {
								n = 1;
							}
							RadiomicsJ.nBins = n;
						}catch(NumberFormatException e) {
							//keep default
							continue;
						}
					}
					if(keyString.equals(SettingParams.INT_interpolation2D.name())) {
						try{
							Integer n = Integer.valueOf(val);
							if(n < 0 || n > 2) {
								n = 0;
							}
							RadiomicsJ.interpolation2D = n;
						}catch(NumberFormatException e) {
							//keep default
							continue;
						}
					}
					if(keyString.equals(SettingParams.INT_interpolation_mask2D.name())) {
						try{
							Integer n = Integer.valueOf(val);
							if(n < 0 || n > 2) {
								n = 0;
							}
							RadiomicsJ.interpolation_mask2D = n;
						}catch(NumberFormatException e) {
							//keep default
							continue;
						}
					}
					if(keyString.equals(SettingParams.INT_interpolation3D.name())) {
						try{
							Integer n = Integer.valueOf(val);
							if(n < 0) {
								n = 0;
							}
							RadiomicsJ.interpolation3D = n;
						}catch(NumberFormatException e) {
							//keep default
							continue;
						}
					}
					if(keyString.equals(SettingParams.INT_interpolation_mask3D.name())) {
						try{
							Integer n = Integer.valueOf(val);
							if(n < 0) {
								n = 0;
							}
							RadiomicsJ.interpolation_mask3D = n;
						}catch(NumberFormatException e) {
							//keep default
							continue;
						}
					}
					if(keyString.equals(SettingParams.DOUBLE_binWidth.name())) {
						try{
							Double n = Double.valueOf(val);
							if(n < 0d) {
								continue;
							}
							RadiomicsJ.binWidth = n;
						}catch(NumberFormatException e) {
							//keep default
							continue;
						}
					}
					if(keyString.equals(SettingParams.DOUBLE_zScore.name())) {
						try{
							Double n = Double.valueOf(val);
							if(n < 0d) {
								continue;
							}
							RadiomicsJ.zScore = n;
						}catch(NumberFormatException e) {
							//keep default
							continue;
						}
					}
					if(keyString.equals(SettingParams.DOUBLE_normalizeScale.name())) {
						try{
							Double n = Double.valueOf(val);
							if(n < 0d) {
								continue;
							}
							RadiomicsJ.normalizeScale = n;
						}catch(NumberFormatException e) {
							//keep default
							continue;
						}
					}
					if(keyString.equals(SettingParams.DOUBLE_densityShift.name())) {
						try{
							Double n = Double.valueOf(val);
							if(n < 0d) {
								continue;
							}
							RadiomicsJ.densityShift = n;
						}catch(NumberFormatException e) {
							//keep default
							continue;
						}
					}
					if(keyString.equals(SettingParams.DOUBLE_rangeMax.name())) {
						try{
							Double n = Double.valueOf(val);
							RadiomicsJ.rangeMax = n;
						}catch(NumberFormatException e) {
							//keep default
							continue;
						}
					}
					if(keyString.equals(SettingParams.DOUBLE_rangeMin.name())) {
						try{
							Double n = Double.valueOf(val);
							RadiomicsJ.rangeMin = n;
						}catch(NumberFormatException e) {
							//keep default
							continue;
						}
					}
					if(keyString.equals(SettingParams.INT_alpha.name())) {
						try{
							Integer n = Integer.valueOf(val);
							if(n < 0) {
								n = 0;
							}
							RadiomicsJ.alpha = n;
						}catch(NumberFormatException e) {
							//keep default
							continue;
						}
					}
					if(keyString.equals(SettingParams.INT_deltaGLCM.name())) {
						try{
							Integer n = Integer.valueOf(val);
							if(n < 1) {
								n = 1;
							}
							RadiomicsJ.deltaGLCM = n;
						}catch(NumberFormatException e) {
							//keep default
							continue;
						}
					}
					if(keyString.equals(SettingParams.INT_deltaNGTDM.name())) {
						try{
							Integer n = Integer.valueOf(val);
							if(n < 1) {
								n = 1;
							}
							RadiomicsJ.deltaNGToneDM = n;
						}catch(NumberFormatException e) {
							//keep default
							continue;
						}
					}
					if(keyString.equals(SettingParams.INT_deltaNGLDM.name())) {
						try{
							Integer n = Integer.valueOf(val);
							if(n < 1) {
								n = 1;
							}
							RadiomicsJ.deltaNGLevelDM = n;
						}catch(NumberFormatException e) {
							//keep default
							continue;
						}
					}
					if(keyString.equals(SettingParams.STRING_weightingNorm.name())) {
						if(val.length()<1) {
							RadiomicsJ.weightingNorm = null;//default
						}else {
							RadiomicsJ.weightingNorm = Utils.isValidWeightingNormName(val) ? val:null;
						}
					}
					//INTARRAY_box_sizes
					if(keyString.equals(SettingParams.INTARRAY_box_sizes.name())) {
						if(val.length()<1) {
							RadiomicsJ.box_sizes = null;//default
						}else {
							//if string parsing was failed, set null automatically.
							RadiomicsJ.box_sizes = Utils.s2ints(val);
						}
					}
					if(keyString.equals(SettingParams.INT_IVH_binCount.name())) {
						try{
							Integer n = Integer.valueOf(val);
							if(n < 1) {
								n = 1;
							}
							RadiomicsJ.IVH_binCount = n;
						}catch(NumberFormatException e) {
							//keep default
							continue;
						}
					}
					if(keyString.equals(SettingParams.DOUBLE_IVH_binWidth.name())) {
						try{
							Double n = Double.valueOf(val);
							if(n < 0) {
								n = Math.abs(n);
							}
							RadiomicsJ.IVH_binWidth = n;
						}catch(NumberFormatException e) {
							//keep default
							continue;
						}
					}
					if(keyString.equals(SettingParams.INT_IVH_MODE.name())) {
						try{
							Integer n = Integer.valueOf(val);
							if(n < 0) {
								n = 0;
							}
							RadiomicsJ.IVH_mode = n;
						}catch(NumberFormatException e) {
							//keep default
							continue;
						}
					}
					if(keyString.equals(SettingParams.BOOL_USE_FixedBinNumber.name())) {
						RadiomicsJ.BOOL_USE_FixedBinNumber = val.equals("1") || val.toLowerCase().equals("true") ? true:false;
					}
					if(keyString.equals(SettingParams.BOOL_normalize.name())) {
						RadiomicsJ.normalize = val.equals("1") || val.toLowerCase().equals("true") ? true:false;
					}
					if(keyString.equals(SettingParams.BOOL_removeOutliers.name())) {
						RadiomicsJ.removeOutliers = val.equals("1") || val.toLowerCase().equals("true") ? true:false;
					}
					//DOUBLEARRAY_resamplingFactorXYZ
					if(keyString.equals(SettingParams.DOUBLEARRAY_resamplingFactorXYZ.name())) {
						if(val.length()<1) {
							RadiomicsJ.resamplingFactorXYZ = null;//default
						}else {
							//if string parsing was failed, set null automatically.
							RadiomicsJ.resamplingFactorXYZ = Utils.s2doubles(val);
						}
					}
					if(keyString.equals(SettingParams.BOOL_force2D.name())) {
						RadiomicsJ.force2D = val.equals("1") || val.toLowerCase().equals("true") ? true:false;
					}
					if(keyString.equals(SettingParams.BOOL_activate_no_default_features.name())) {
						RadiomicsJ.activate_no_default_features = val.equals("1") || val.toLowerCase().equals("true") ? true:false;
					}
					if(keyString.equals(SettingParams.DOUBLE_Mask_PartialVolumeThareshold.name())) {
						try{
							Double n = Double.valueOf(val);
							if(n < 0 || n > 0.999) {
								n = 0.5;
							}
							RadiomicsJ.mask_PartialVolumeThareshold = n;
						}catch(NumberFormatException e) {
							//keep default
							continue;
						}
					}
					
					/*
					 * features
					 */
					if(keyString.equals(SettingParams.BOOL_enableOperationalInfo.name())) {
						this.BOOL_enableOperationalInfo = val.equals("1") || val.toLowerCase().equals("true") ? true:false;
					}
					if(keyString.equals(SettingParams.BOOL_enableDiagnostics.name())) {
						this.BOOL_enableDiagnostics = val.equals("1") || val.toLowerCase().equals("true") ? true:false;
					}
					if(keyString.equals(SettingParams.BOOL_enableIntensityBasedStatistics.name())) {
						this.BOOL_enableIntensityBasedStatistics = val.equals("1") || val.toLowerCase().equals("true") ? true:false;
					}
					if(keyString.equals(SettingParams.BOOL_enableIntensityHistogram.name())) {
						this.BOOL_enableIntensityHistogram = val.equals("1") || val.toLowerCase().equals("true") ? true:false;
					}
					if(keyString.equals(SettingParams.BOOL_enableIntensityVolumeHistogram.name())) {
						this.BOOL_enableIntensityVolumeHistogram = val.equals("1") || val.toLowerCase().equals("true") ? true:false;
					}
					if(keyString.equals(SettingParams.BOOL_enableMorphological.name())) {
						this.BOOL_enableMorphological = val.equals("1") || val.toLowerCase().equals("true") ? true:false;
					}
					if(keyString.equals(SettingParams.BOOL_enableLocalIntensityFeatures.name())) {
						this.BOOL_enableLocalIntensityFeatures = val.equals("1") || val.toLowerCase().equals("true") ? true:false;
					}
					if(keyString.equals(SettingParams.BOOL_enableGLCM.name())) {
						this.BOOL_enableGLCM = val.equals("1") || val.toLowerCase().equals("true") ? true:false;
					}
					if(keyString.equals(SettingParams.BOOL_enableGLRLM.name())) {
						this.BOOL_enableGLRLM = val.equals("1") || val.toLowerCase().equals("true") ? true:false;
					}
					if(keyString.equals(SettingParams.BOOL_enableGLSZM.name())) {
						this.BOOL_enableGLSZM = val.equals("1") || val.toLowerCase().equals("true") ? true:false;
					}
					if(keyString.equals(SettingParams.BOOL_enableGLDZM.name())) {
						this.BOOL_enableGLDZM = val.equals("1") || val.toLowerCase().equals("true") ? true:false;
					}
					if(keyString.equals(SettingParams.BOOL_enableNGTDM.name())) {
						this.BOOL_enableNGTDM = val.equals("1") || val.toLowerCase().equals("true") ? true:false;
					}
					if(keyString.equals(SettingParams.BOOL_enableNGLDM.name())) {
						this.BOOL_enableNGLDM = val.equals("1") || val.toLowerCase().equals("true") ? true:false;
					}
					if(keyString.equals(SettingParams.BOOL_enableHomological.name())) {
						this.BOOL_enableHomological = val.equals("1") || val.toLowerCase().equals("true") ? true:false;
					}
					if(keyString.equals(SettingParams.BOOL_enableFractal.name())) {
						this.BOOL_enableFractal = val.equals("1") || val.toLowerCase().equals("true") ? true:false;
					}
					if(keyString.equals(SettingParams.BOOL_enableShape2D.name())) {
						this.BOOL_enableShape2D = val.equals("1") || val.toLowerCase().equals("true") ? true:false;
					}
				}
			}
		}
	}
	
	/**
	 * create copy as float processor, and replace target label to analysis mask label(1).
	 * @param originalImp
	 * @param originalMask
	 * @param targetLabel
	 * @throws Exception
	 */
	public void preprocess(ImagePlus originalImp, ImagePlus originalMask, Integer targetLabel) throws Exception {
		if(originalMask == null) {
			int w = originalImp.getWidth();
			int h = originalImp.getHeight();
			int s = originalImp.getNSlices();
			originalMask = ImagePreprocessing.createMask(w, h, s, null, label_, originalImp.getCalibration().pixelWidth, originalImp.getCalibration().pixelHeight, originalImp.getCalibration().pixelDepth);
		}
		if(debug) {
			System.out.println("preparing analysis images ...");
		}
		//label to 1.
		preprocessAnalysisReady(originalImp, originalMask, targetLabel);
		
		if(currentImp == null || currentMask == null) {
			System.out.println("RadiomicsJ:preprocess()::Creating Mask was failed. return.");
			return;
		}
		
		if(debug) {
			System.out.println("perform resampling ...");
		}
		preprocessResample(currentImp, currentMask);
		if(debug) {
			System.out.println("perform re-segmentation ...");
		}
		//use label 1.
		preprocessResegment(resampledImp, resampledMask, RadiomicsJ.label_);
		/*
		 * normalize
		 */
		if(normalize) {
			if(debug) {
				System.out.println("perform normalization ...");
			}
			resampledImp = ImagePreprocessing.normalize(resampledImp, resegmentedMask, RadiomicsJ.label_);
		}
		if(debug) {
			System.out.println("perform discretisation ...");
		}
		preprocessDiscretise(resampledImp, resegmentedMask, RadiomicsJ.label_);
	}
	
	/**
	 * convert to float32 and,
	 * mask label will convert label(1), this is because deal with partial volume effects at interpolation.
	 * @param img
	 * @param mask
	 * @return
	 */
	public ImagePlus[] preprocessAnalysisReady(ImagePlus img, ImagePlus mask, Integer targetLabel) {
		currentImp = Utils.createImageCopyAsFloat(img, false);
		/*
		 * convert target label(any) to analysis label(1) 
		 */
		currentMask = Utils.initMaskAsFloatAndConvertLabelOne(mask, targetLabel);
		
		if(Utils.isBlankMaskStack(currentMask, RadiomicsJ.label_)) {
			currentImp = null;
			currentMask = null;
			new Exception("Mask does not have target label. Can not compute features.");
		}
		return new ImagePlus[] {currentImp, currentMask};
	}
	
	/**
	 * resample x-y-z.
	 * @param img
	 * @param mask
	 * @return
	 */
	public ImagePlus[] preprocessResample(ImagePlus img, ImagePlus mask) {
		if(resamplingFactorXYZ != null) {
			if(force2D) {
				//ignore z
				resampledImp = Utils.resample2D(img, false, resamplingFactorXYZ[0], resamplingFactorXYZ[1], RadiomicsJ.interpolation2D);
				resampledMask = Utils.resample2D(mask, true, resamplingFactorXYZ[0], resamplingFactorXYZ[1], RadiomicsJ.interpolation2D);
			}else {
				// trilinear interpolation
				resampledImp = Utils.resample3D(img, false, resamplingFactorXYZ[0], resamplingFactorXYZ[1], resamplingFactorXYZ[2]);
				resampledMask = Utils.resample3D(mask, true, resamplingFactorXYZ[0], resamplingFactorXYZ[1], resamplingFactorXYZ[2]);
			}
		}else {
			//AS-IS
			resampledImp = Utils.createImageCopy(img);
			resampledMask = Utils.createMaskCopy(mask);
		}
		return new ImagePlus[] {resampledImp, resampledMask};
	}
	
	/**
	 * If did preprocessAnalysisReady() before, 
	 * targetLabel should be "1".
	 * @param img
	 * @param mask
	 * @return
	 */
	public ImagePlus preprocessResegment(ImagePlus img, ImagePlus mask, Integer targetLabel) {
		
		/*
		 * rangeFilter
		 */
		if(rangeMax != null && rangeMin != null) {
			resegmentedMask = ImagePreprocessing.rangeFiltering(img, mask, targetLabel, rangeMax, rangeMin);
		}else {
			resegmentedMask = Utils.createMaskCopy(mask);
		}
		
		if(removeOutliers) {
			// get new mask removed outliers
			resegmentedMask = ImagePreprocessing.outlierFiltering(img, resegmentedMask, targetLabel);
		}
		
		if(resegmentedMask == null) {
			resegmentedMask = Utils.createMaskCopy(mask);
		}
		return resegmentedMask;
	}
	
	/**
	 * create discretise image.
	 * 
	 * @param resampled resampled image stack
	 * @param resegmentedMask resampled and resegmented mask stack
	 * @param targetLabel if preprocessAnalysisReady() performed before, targetLabel should be "1".
	 * @return
	 * @throws Exception
	 */
	public ImagePlus preprocessDiscretise(ImagePlus resampled, ImagePlus resegmentedMask, Integer targetLabel) throws Exception {
		discretiseImp = null;
		if(BOOL_USE_FixedBinNumber) {
			discretiseImp = Utils.discrete(resampled, resegmentedMask, targetLabel, RadiomicsJ.nBins);
		}else {
			/*
			 * Fixed Bin Width
			 */
			discretiseImp = Utils.discreteByBinWidth(resampled, resegmentedMask, targetLabel, binWidth);
			RadiomicsJ.nBins = Utils.getNumOfBinsByMax(discretiseImp, resegmentedMask, targetLabel);
		}
		return discretiseImp;
	}
	
	/**
	 * 
	 * @param imgSeriesFileFolder
	 * @param maskSeriesFileFolder
	 * @param targetLabel
	 * @return ResultsTable
	 * @throws Exception
	 */
	public ResultsTable execute(File imgSeriesFileFolder, File maskSeriesFileFolder, Integer targetLabel) throws Exception {
		if(activate_no_default_features) {
			activateNoDefaultFeatures();
		}
		
		// do not do this, to read NifTi.
//		if(!imgSeriesFileFolder.isDirectory() || !maskSeriesFileFolder.isDirectory()) {
//			System.out.println("Sorry, please input directory. return null.");
//			return null;
//		}
		/*
		 * nifti
		 */
		if(imgSeriesFileFolder.isFile()) {
			if(imgSeriesFileFolder.getName().contains(".nii.gz")) {
				String p2i = imgSeriesFileFolder.getAbsolutePath();
				String p2m = maskSeriesFileFolder.getAbsolutePath();
				Object img = null;
				Object mask = null;
				img = IJ.runPlugIn("Nifti_Reader", p2i);
		        mask = IJ.runPlugIn("Nifti_Reader", p2m);
		        if(img == null) {
		        	System.out.println("Can not load nifti file... return null.");
		        	return null;
		        }
				if(force2D) {
					return extractAllSlice((ImagePlus)img,(ImagePlus)mask,targetLabel);
				}else {
					return extractAll((ImagePlus)img,(ImagePlus)mask,targetLabel);
				}
			}else if(imgSeriesFileFolder.getName().endsWith(".tif") || imgSeriesFileFolder.getName().endsWith(".tiff")) {
				String p2i = imgSeriesFileFolder.getAbsolutePath();
				String p2m = maskSeriesFileFolder.getAbsolutePath();
				ImagePlus img = new ImagePlus(p2i);
				ImagePlus mask = new ImagePlus(p2m);
				if(force2D) {
					return extractAllSlice(img, mask,targetLabel);
				}else {
					return extractAll(img, mask,targetLabel);
				}
			}
		}else if(imgSeriesFileFolder.listFiles().length == 1 && maskSeriesFileFolder.listFiles().length == 1) {
			imgSeriesFileFolder = imgSeriesFileFolder.listFiles()[0];
			maskSeriesFileFolder = maskSeriesFileFolder.listFiles()[0];
			if(imgSeriesFileFolder.getName().contains(".nii.gz")) {
				String p2i = imgSeriesFileFolder.getAbsolutePath();
				String p2m = maskSeriesFileFolder.getAbsolutePath();
				Object img = null;
				Object mask = null;
				img = IJ.runPlugIn("Nifti_Reader", p2i);
		        mask = IJ.runPlugIn("Nifti_Reader", p2m);
		        if(img == null) {
		        	System.out.println("Can not load nifti file... return null.");
		        	return null;
		        }
				if(force2D) {
					return extractAllSlice((ImagePlus)img,(ImagePlus)mask,targetLabel);
				}else {
					return extractAll((ImagePlus)img,(ImagePlus)mask,targetLabel);
				}
			}else if(imgSeriesFileFolder.getName().endsWith(".tif") || imgSeriesFileFolder.getName().endsWith(".tiff")) {
				String p2i = imgSeriesFileFolder.getAbsolutePath();
				String p2m = maskSeriesFileFolder.getAbsolutePath();
				ImagePlus img = new ImagePlus(p2i);
				ImagePlus mask = new ImagePlus(p2m);
				if(force2D) {
					return extractAllSlice(img, mask,targetLabel);
				}else {
					return extractAll(img, mask,targetLabel);
				}
			}
		}
		
		if(force2D) {
			return extractAllSlice(imgSeriesFileFolder.listFiles(), maskSeriesFileFolder.listFiles(),targetLabel);
		}else {
			return extractAll(new ImagePlus(imgSeriesFileFolder.getAbsolutePath()), new ImagePlus(maskSeriesFileFolder.getAbsolutePath()),targetLabel);
		}
	}
	
	/**
	 * 
	 * @param img
	 * @param mask
	 * @param targetLabel
	 * @return ResultsTable
	 * @throws Exception
	 */
	public ResultsTable execute(ImagePlus img, ImagePlus mask, Integer targetLabel) throws Exception {
		if(activate_no_default_features) {
			activateNoDefaultFeatures();
		}
		if(force2D) {
			return extractAllSlice(img, mask,targetLabel);
		}else {
			return extractAll(img, mask,targetLabel);
		}
	}
	
	/**
	 * 3D basis feature extraction.
	 * @param img
	 * @param mask
	 * @param targetLabel
	 * @return ResuktsTable
	 * @throws Exception
	 */
	public ResultsTable extractAll(ImagePlus img, ImagePlus mask, Integer targetLabel) throws Exception {
		originalImp = img;
		originalMask = mask;
		/*
		 * create label 1 mask
		 * resample
		 * re-segmentation
		 * standardization
		 */
		preprocess(originalImp, originalMask, targetLabel);
		return compute(resampledImp, resegmentedMask,RadiomicsJ.label_);
	}
	
	
	/**
	 * Extract features slice by slice (2D basis without any aggregation)
	 * images and masks file names should be same or set sortable names pair (it can keep order images and masks list).
	 * 
	 * @throws Exception 
	 */
	public ResultsTable extractAllSlice(File[] images, File[] masks, Integer targetLabel) throws Exception {
		/*
		 * if dicom
		 */
		ImagePlus sampleI = new ImagePlus(images[0].getAbsolutePath());
		ImagePlus sampleM = new ImagePlus(masks[0].getAbsolutePath());
		String sopInstUID_I = DicomTools.getTag(sampleI, "0008,0018");
		String sopInstUID_M = DicomTools.getTag(sampleM, "0008,0018");
		if(sopInstUID_I != null && sopInstUID_M != null) {
			File parentImgFolder = images[0].getParentFile();
			File parentMaskFolder = masks[0].getParentFile();
			ImagePlus is = new ImagePlus(parentImgFolder.getAbsolutePath());
			ImagePlus ms = new ImagePlus(parentMaskFolder.getAbsolutePath());
			//sort by image number
			is.setStack(DicomTools.sort(is.getStack()));
			ms.setStack(DicomTools.sort(ms.getStack()));
			//do not preprocess here yet
			return extractAllSlice(is, ms, targetLabel);
		}else {
			/*
			 * check matching names pair ?
			 * if found no-match, return exception ?
			 */
			ArrayList<String> iNameArray = new ArrayList<>();
			ArrayList<String> mNameArray = new ArrayList<>();
			int s = images.length;
			for(int i=0;i<s;i++) {
				String iName = images[i].getName();
				String mName = masks[i].getName();
				iNameArray.add(iName);
				mNameArray.add(mName);
			}
			Collections.sort(iNameArray);
			Collections.sort(mNameArray);
			int w = sampleI.getWidth();
			int h = sampleI.getHeight();
			ImageStack is = new ImageStack(w, h);
			ImageStack ms = new ImageStack(w, h);
			for(int i=0;i<s;i++) {
				String iName = iNameArray.get(i);
				String mName = mNameArray.get(i);
				File imgF = null;
				File mskF = null;
				for(int j=0;j<s;j++) {
					if(images[j].getName().equals(iName)) {
						imgF = images[j];
					}
					if(masks[j].getName().equals(mName)) {
						mskF = masks[j];
					}
				}
				is.addSlice(new ImagePlus(imgF.getAbsolutePath()).getProcessor());
				ms.addSlice(new ImagePlus(mskF.getAbsolutePath()).getProcessor());
			}
			ImagePlus ds_i = new ImagePlus("images", is);
			ds_i.setCalibration(sampleI.getCalibration());
			ImagePlus ds_m = new ImagePlus("masks", ms);
			ds_m.setCalibration(sampleI.getCalibration());//set calibration using from image
			//do not preprocess here yet
			return extractAllSlice(ds_i, ds_m, targetLabel);
		}
	}
	
	/**
	 * Extract features slice by slice (2D basis without any aggregation)
	 * @param images : stack series images
	 * @param masks : stack series masks
	 * @return
	 */
	public ResultsTable extractAllSlice(ImagePlus images, ImagePlus masks, Integer targetLabel) {
		originalImp = images;
		originalMask = masks;
		ResultsTable rt = null;
		int num = images.getNSlices();
		for(int i=0;i<num;i++) {
			try {
				currentImp = new ImagePlus(i+"", images.getStack().getProcessor(i+1).duplicate());
				currentMask = new ImagePlus(i+"", masks.getStack().getProcessor(i+1).duplicate());
				currentImp.setCalibration(images.getCalibration());
				currentMask.setCalibration(masks.getCalibration());
				if(Utils.isBlankMaskSlice(currentMask.getProcessor(), targetLabel)) {
					continue;
				}
				/*
				 * do preprocessing
				 */
				preprocess(currentImp, currentMask, targetLabel);
				System.out.println("Compute at "+(i+1));
				ResultsTable table = compute(resampledImp, resegmentedMask,RadiomicsJ.label_);
				if(rt == null) {
					rt = table;
				}else {
					if(table != null) {
						rt.incrementCounter();
						String[] headings = table.getHeadings();
						int row  = 0;
						for(String h : headings) {
							if(h.contains("OperationalInfo_")) {
								String v = table.getStringValue(h, row);
								rt.addValue(h, v);
							}else {
								double v = table.getValue(h, row);
								rt.addValue(h, v);
							}
						}
					}
				}
			} catch (Exception e) {
				System.err.println("Something strange skip this images and masks stack pair -> "+images.getTitle()+" and "+masks.getTitle());
				e.printStackTrace();
				continue;
			}
		}
		return rt;
	}
	
	/**
	 * perform computations
	 * @param img
	 * @param mask
	 * @param targetLabel : if preprocess() is done, set to (1)
	 * @return ResultsTable
	 * @throws Exception
	 */
	public ResultsTable compute(ImagePlus img, ImagePlus mask, Integer targetLabel) throws Exception {

		if(img == null) {
			return null;
		}
		
		ResultsTable rt = ResultsTable.getResultsTable(resultWindowTitle);
		if(rt == null) {
			rt = new ResultsTable();
		}
		rt.incrementCounter();
		
		if(BOOL_enableOperationalInfo) {
			if(debug) {
				System.out.println("=======================");
				System.out.println("OperationalInformations");
			}
			OperationalInfoFeatures oif = new OperationalInfoFeatures(img);
			java.util.HashMap<String,String> info = oif.getInfo();
			Iterator<String> keys = info.keySet().iterator();
			while(keys.hasNext()) {
				String key = keys.next();
				String val = info.get(key);
				rt.addValue("OperationalInfo_" + key, val == null ? "NaN":val);
				if(debug) {
					System.out.println(key+", "+val);
				}
			}
		}
		
		/*
		 * diagnostics...
		 */
		if(BOOL_enableDiagnostics) {
			if(debug) {
				System.out.println("=======================");
				System.out.println("Diagnostics information");
			}
			
			DiagnosticsInfo di = new DiagnosticsInfo(currentImp, currentMask, resampledImp, resampledMask, resegmentedMask, targetLabel);
			
			for(DiagnosticsInfoType dinfo : DiagnosticsInfoType.values()) {
				Double i = di.getDiagnosticsBy(dinfo.name());
				if(i == null) {
					rt.addValue("Diagnostics_" + dinfo.name(), "NaN");
				}else {
					rt.addValue("Diagnostics_" + dinfo.name(), i);
				}
				if(debug) {
					System.out.println(dinfo.name()+", "+i);
				}
			}
		}
		
		if(BOOL_enableMorphological) {
			if(debug) {
				System.out.println("=======================");
				System.out.println("Morphological features");
			}
			MorphologicalFeatures f = new MorphologicalFeatures(img, mask, targetLabel);
			for (MorphologicalFeatureType ft : MorphologicalFeatureType.values()) {
				if(excluded.contains(ft.name())) {
					continue;
				}
				Double feature = f.calculate(ft.id());
				if(feature == null || feature == Double.NaN) {
					rt.addValue("Morphology_" + ft.name(), "NaN");
				}else {
					rt.addValue("Morphology_" + ft.name(), feature);
				}
				if(debug) {
					System.out.println(ft.name()+", "+feature);
				}
			}
		}
		
		if(BOOL_enableLocalIntensityFeatures) {
			if(debug) {
				System.out.println("=======================");
				System.out.println("Local intensity features");
			}
			LocalIntensityFeatures f = new LocalIntensityFeatures(img, mask, targetLabel);
			for (LocalIntensityFeatureType ft : LocalIntensityFeatureType.values()) {
				if(excluded.contains(ft.name())) {
					continue;
				}
				Double feature = f.calculate(ft.id());
				if(feature == null || feature == Double.NaN) {
					rt.addValue("LocalIntensity_" + ft.name(), "NaN");
				}else {
					rt.addValue("LocalIntensity_" + ft.name(), feature);
				}
				if(debug) {
					System.out.println(ft.name()+", "+feature);
				}
			}
		}
		
		if(BOOL_enableIntensityBasedStatistics) {
			if(debug) {
				System.out.println("=================================");
				System.out.println("IntensityBasedStatistics features");
			}
			IntensityBasedStatisticalFeatures executer = new IntensityBasedStatisticalFeatures(img, mask,targetLabel);
			for (IntensityBasedStatisticalFeatureType f : IntensityBasedStatisticalFeatureType.values()) {
				if(excluded.contains(f.name())) {
					continue;
				}
				Double feature = executer.calculate(f.id());
				if (feature == null || feature == Double.NaN) {
					rt.addValue("IntensityBasedStatistical_" + f.name(), "NaN");
				} else {
					rt.addValue("IntensityBasedStatistical_" + f.name(), feature);
				}
				if(debug) {
					System.out.println(f.name()+", "+feature);
				}
			}
		}
		
		if(BOOL_enableIntensityHistogram) {
			if(debug) {
				System.out.println("=================================");
				System.out.println("IntensityHistogram features");
			}
			IntensityHistogramFeatures executer = new IntensityHistogramFeatures(img, mask, targetLabel, BOOL_USE_FixedBinNumber, nBins,binWidth);
			for (IntensityHistogramFeatureType f : IntensityHistogramFeatureType.values()) {
				if(excluded.contains(f.name())) {
					continue;
				}
				Double feature = executer.calculate(f.id());
				if (feature == null || feature == Double.NaN) {
					rt.addValue("IntensityHistogram_" + f.name(), "NaN");
				} else {
					rt.addValue("IntensityHistogram_" + f.name(), feature);
				}
				if(debug) {
					System.out.println(f.name()+", "+feature);
				}
			}
		}
		
		if(BOOL_enableIntensityVolumeHistogram) {
			if(debug) {
				System.out.println("=================================");
				System.out.println("IntensityVolumeHistogram features");
			}
			IntensityVolumeHistogramFeatures executer = new IntensityVolumeHistogramFeatures(img, mask, targetLabel, IVH_mode);
			for (IntensityVolumeHistogramFeatureType f : IntensityVolumeHistogramFeatureType.values()) {
				if(excluded.contains(f.name())) {
					continue;
				}
				Double feature = executer.calculate(f.id());
				if (feature == null || feature == Double.NaN) {
					rt.addValue("IntensityVolumeHistogram_" + f.name(), "NaN");
				} else {
					rt.addValue("IntensityVolumeHistogram_" + f.name(), feature);
				}
				if(debug) {
					System.out.println(f.name()+", "+feature);
				}
			}
		}
		
		if(BOOL_enableShape2D && force2D) {
			if(debug) {
				System.out.println("=================================");
				System.out.println("Shape2D features");
			}
			if(img.getNSlices() > 1) {
				System.out.println("RadiomicsJ: Cannot calculate Shape2D, because image has multi slices.");
				System.out.println("If you need Shape2D, Try create single slice imageplus, then perform Shape2D.");
				System.out.println("Or, you can try to use Shape2DFeatures class on another scripts.");
			}else {
				Shape2DFeatures shape2DExecuter = new Shape2DFeatures(img, mask,1,targetLabel);
				for (Shape2DFeatureType shape : Shape2DFeatureType.values()) {
					if(excluded.contains(shape.name())) {
						continue;
					}
					Double feature = shape2DExecuter.calculate(shape.id());
					if (feature == null || feature == Double.NaN) {
						rt.addValue("Shape2D_" + shape.name(), "NaN");
					} else {
						rt.addValue("Shape2D_" + shape.name(), feature);
					}
					if (debug) {
						System.out.println(shape.name() + ", " + feature);
					}
				}
			}
		}
		
		if(BOOL_enableGLCM) {
			if(debug) {
				System.out.println("=================================");
				System.out.println("GLCM features");
			}
			GLCMFeatures glcmExecuter = new GLCMFeatures(img, 
														 mask, 
														 targetLabel,
														 deltaGLCM,
														 BOOL_USE_FixedBinNumber, 
														 nBins,
														 binWidth,
														 weightingNorm);
			for (GLCMFeatureType glcm : GLCMFeatureType.values()) {
				if(excluded.contains(glcm.name())) {
					continue;
				}
				Double feature = glcmExecuter.calculate(glcm.id());
				if (feature == null|| feature == Double.NaN) {
					rt.addValue("GLCM_" + glcm.name(), "NaN");
				} else {
					rt.addValue("GLCM_" + glcm.name(), feature);
				}
				if (debug) {
					System.out.println(glcm.name() + ", " + feature);
				}
			}
		}
		
		if(BOOL_enableGLRLM) {
			if(debug) {
				System.out.println("=================================");
				System.out.println("GLRLM features");
			}
			GLRLMFeatures glrlmExecuter = new GLRLMFeatures(img, 
															mask, 
															targetLabel,
															BOOL_USE_FixedBinNumber,
															nBins, 
															binWidth,
															weightingNorm);
			for (GLRLMFeatureType glrlm : GLRLMFeatureType.values()) {
				if(excluded.contains(glrlm.name())) {
					continue;
				}
				Double feature = glrlmExecuter.calculate(glrlm.id());
				if (feature == null|| feature == Double.NaN) {
					rt.addValue("GLRLM_" + glrlm.name(), "NaN");
				} else {
					rt.addValue("GLRLM_" + glrlm.name(), feature);
				}
				if (debug) {
					System.out.println(glrlm.name() + ", " + feature);
				}
			}
		}
		
		if(BOOL_enableGLSZM) {
			if(debug) {
				System.out.println("=================================");
				System.out.println("GLSZM features");
			}
			GLSZMFeatures glszmExecuter = new GLSZMFeatures(img,
															mask,
															targetLabel,
															BOOL_USE_FixedBinNumber,
															nBins,
															binWidth);
			for (GLSZMFeatureType glszm : GLSZMFeatureType.values()) {
				if(excluded.contains(glszm.name())) {
					continue;
				}
				Double feature = glszmExecuter.calculate(glszm.id());
				if (feature == null || feature == Double.NaN) {
					rt.addValue("GLSZM_" + glszm.name(), "NaN");
				} else {
					rt.addValue("GLSZM_" + glszm.name(), feature);
				}
				if (debug) {
					System.out.println(glszm.name() + ", " + feature);
				}
			}
		}
		
		if(BOOL_enableGLDZM) {
			if(debug) {
				System.out.println("=================================");
				System.out.println("GLDZM features");
			}
			GLDZMFeatures gldzmExecuter = new GLDZMFeatures(img,
															mask,
															targetLabel,
															BOOL_USE_FixedBinNumber,
															nBins,
															binWidth);
			for (GLDZMFeatureType gldzm : GLDZMFeatureType.values()) {
				if(excluded.contains(gldzm.name())) {
					continue;
				}
				Double feature = gldzmExecuter.calculate(gldzm.id());
				if (feature == null || feature == Double.NaN) {
					rt.addValue("GLDZM_" + gldzm.name(), "NaN");
				} else {
					rt.addValue("GLDZM_" + gldzm.name(), feature);
				}
				if (debug) {
					System.out.println(gldzm.name() + ", " + feature);
				}
			}
		}
		
		if(BOOL_enableNGTDM) {
			if(debug) {
				System.out.println("=================================");
				System.out.println("NGTDM features");
			}
			NGTDMFeatures ngtdmExecuter = new NGTDMFeatures(img,
															mask,
															targetLabel,
															deltaNGToneDM,
															BOOL_USE_FixedBinNumber,
															nBins,
															binWidth);
			for (NGTDMFeatureType ngtdm : NGTDMFeatureType.values()) {
				if(excluded.contains(ngtdm.name())) {
					continue;
				}
				Double feature = ngtdmExecuter.calculate(ngtdm.id());
				if (feature == null || feature == Double.NaN) {
					rt.addValue("NGTDM_" + ngtdm.name(), "NaN");
				} else {
					rt.addValue("NGTDM_" + ngtdm.name(), feature);
				}
				if (debug) {
					System.out.println(ngtdm.name() + ", " + feature);
				}
			}
		}
		
		if(BOOL_enableNGLDM) {
			if(debug) {
				System.out.println("=================================");
				System.out.println("NGLDM features");
			}
			NGLDMFeatures gldmExecuter = new NGLDMFeatures(img, 
															mask, 
															targetLabel,
															alpha, 
															deltaNGLevelDM,
															BOOL_USE_FixedBinNumber,
															nBins,
															binWidth);
			for(NGLDMFeatureType gldm:NGLDMFeatureType.values()) {
				if(excluded.contains(gldm.name())) {
					continue;
				}
				Double feature = gldmExecuter.calculate(gldm.id());
				if(feature == null || feature == Double.NaN) {
					rt.addValue("NGLDM_"+gldm.name(), "NaN");
				}else {
					rt.addValue("NGLDM_"+gldm.name(), feature);
				}
				if (debug) {
					System.out.println(gldm.name() + ", " + feature);
				}
			}
		}

		if(BOOL_enableFractal) {
			if(debug) {
				System.out.println("=================================");
				System.out.println("Fractal features");
			}
			FractalFeatures fractalExecuter = null;
			if(force2D) {
				fractalExecuter = new FractalFeatures(img, mask, targetLabel, 1, RadiomicsJ.box_sizes);
			}else {
				fractalExecuter = new FractalFeatures(img, mask, targetLabel, null, RadiomicsJ.box_sizes);
			}
			if(fractalExecuter != null) {
				for(FractalFeatureType fractal:FractalFeatureType.values()) {
					if(excluded.contains(fractal.name())) {
						continue;
					}
					Double feature = fractalExecuter.calculate(fractal.id());
					if(feature == null || feature == Double.NaN) {
						rt.addValue("Fractal_"+fractal.name(), "NaN");
					}else {
						rt.addValue("Fractal_"+fractal.name(), feature);
					}
					if (debug) {
						System.out.println(fractal.name() + ", " + feature);
					}
				}
			}
		}
		System.gc();
		return rt;
	}
}
