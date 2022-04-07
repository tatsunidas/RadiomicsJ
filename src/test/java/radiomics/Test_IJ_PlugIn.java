package radiomics;

import java.awt.Checkbox;
import java.io.File;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;

import javax.swing.JOptionPane;

import RadiomicsJ.RadiomicsJ;
import RadiomicsJ.SettingParams;
import RadiomicsJ.TestDataLoader;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.measure.ResultsTable;


public class Test_IJ_PlugIn {

	public static void main(String[] args) {
		
		GenericDialog gd = new GenericDialog("RadiomicsJ");
//		gd.addImageChoice("images", null);
//		gd.addImageChoice("masks", null);
		gd.addNumericField("Mask label(between 1 to 255)", 1, 0);//label, digit
		gd.addCheckbox("Use fixed bin number", true);
		gd.addNumericField("bin count", 32, 0);
		gd.addNumericField("bin width", 25, 2);
		
		gd.addCheckbox("removeOutliers", false);
		gd.addNumericField("z score", 3);
		
		//TODO, but I do not need now.
//		gd.addCheckbox("normalize", false);
		
		gd.addCheckbox("Range filering", false);
		gd.addNumericField("Range Max", 3000);
		gd.addNumericField("Range Min", -1500);
		
		gd.addCheckbox("Resample [mm]", false);
		gd.addNumericField("voxels x", 1.0);
		gd.addNumericField("voxels y", 1.0);
		gd.addNumericField("voxels z(when force2D is true, ignored)", 1.0);
		
		gd.addCheckbox("Activate_no_default_features", false);
		gd.addCheckbox("force 2D (also check Shape2D on your needs.)", false);
		gd.addNumericField("IVH mode(0:None,1:IVHBinWidth,2:IVHBinCount)", 0);
		gd.addNumericField("IVH_binCount", 1000, 0);
		gd.addNumericField("IVH_binWidth", 2.5, 2);
		/*
		 * additional items will colapse gui of generic dialog...
		 */
		gd.addMessage("Enable info/features");
		final String[] labels = new String[] {
				"Operational", "Diagnostics", "Morphological", "LocalIntensity", "IntensityStats", 
				"Histogram", "VolumeHistogram", "GLCM","GLRLM", "GLSZM",
				"GLDZM", "NGTDM", "NGLDM", "Fractal", "Shape2D"};
		boolean[] defBools = new boolean[] {
				true,true,true,true,true,
				true,true,true,true,true,
				true,true,true,true,false
				};
		gd.addCheckboxGroup(3, 5, labels, defBools);
		
		gd.showDialog();
		
		if(gd.wasOKed()) {
			
//			ImagePlus images = gd.getNextImage();
//			ImagePlus masks = gd.getNextImage();
			
			String p2i = "C:\\Users\\tatsunidas\\Desktop\\resampled"+File.separator+"LGG-104"+File.separator+"t1ce.nii.gz";
			String p2m = "C:\\Users\\tatsunidas\\Desktop\\resampled"+File.separator+"LGG-104"+File.separator+"mask.nii.gz";
			ImagePlus images = TestDataLoader.loadNifTi(p2i);
			ImagePlus masks = TestDataLoader.loadNifTi(p2m);
			
			images.show();
			masks.show();
			
			if(images.equals(masks)) {
				IJ.log("RadiomicsJ needs images and mask pair, sorry, it is return.");
				return;
			}
			
			//set to prop
			/**
			 * please cast to INT to int, Double to double.
			 */
			Properties prop = new Properties();
			prop.put(SettingParams.INT_label.name(), String.valueOf((int)gd.getNextNumber()));
			prop.put(SettingParams.BOOL_USE_FixedBinNumber.name(), gd.getNextBoolean()==true ? "true":"false");
			prop.put(SettingParams.INT_binCount.name(), String.valueOf((int)gd.getNextNumber()));
			prop.put(SettingParams.DOUBLE_binWidth.name(), String.valueOf(gd.getNextNumber()));
			prop.put(SettingParams.BOOL_removeOutliers.name(), gd.getNextBoolean()==true ? "true":"false");
			prop.put(SettingParams.DOUBLE_zScore.name(), String.valueOf(gd.getNextNumber()));
			
			// ignore normalize.
//			prop.put(SettingParams.BOOL_normalize.name(), gd.getNextBoolean()==true ? "true":"false");
			
			//range filtering
			if(gd.getNextBoolean()) {
				prop.put(SettingParams.DOUBLE_rangeMax.name(), String.valueOf(gd.getNextNumber()));
				prop.put(SettingParams.DOUBLE_rangeMin.name(), String.valueOf(gd.getNextNumber()));
			}else {
				//consume
				gd.getNextNumber();
				gd.getNextNumber();
			}
			
			//resampling
			if(gd.getNextBoolean()) {
				double x = gd.getNextNumber();
				double y = gd.getNextNumber();
				double z = gd.getNextNumber();
				prop.put(SettingParams.DOUBLEARRAY_resamplingFactorXYZ.name(), x+","+y+","+z);
			}else {
				//consume
				gd.getNextNumber();
				gd.getNextNumber();
				gd.getNextNumber();
			}
			
			prop.put(SettingParams.BOOL_activate_no_default_features.name(), gd.getNextBoolean() ? "true":"false");
			prop.put(SettingParams.BOOL_force2D.name(), gd.getNextBoolean() ? "true":"false");
			prop.put(SettingParams.INT_IVH_MODE.name(), String.valueOf((int)gd.getNextNumber()));
			prop.put(SettingParams.INT_IVH_binCount.name(), String.valueOf((int)gd.getNextNumber()));
			prop.put(SettingParams.DOUBLE_IVH_binWidth.name(), String.valueOf(gd.getNextNumber()));
			
			@SuppressWarnings("unchecked")
			Vector<java.awt.Checkbox> enables = gd.getCheckboxes();
			Iterator<Checkbox> elems = enables.iterator();
			while(elems.hasNext()) {
				Checkbox chk = elems.next();
				String label = chk.getLabel();
				boolean state = chk.getState();
				if(label.equals(labels[0])) {
					prop.put(SettingParams.BOOL_enableOperationalInfo.name(), state ? "true":"false");
				}else if(label.equals(labels[1])) {
					prop.put(SettingParams.BOOL_enableDiagnostics.name(), state ? "true":"false");
				}else if(label.equals(labels[2])) {
					prop.put(SettingParams.BOOL_enableMorphological.name(), state ? "true":"false");
				}else if(label.equals(labels[3])) {
					prop.put(SettingParams.BOOL_enableLocalIntensityFeatures.name(), state ? "true":"false");
				}else if(label.equals(labels[4])) {
					prop.put(SettingParams.BOOL_enableIntensityBasedStatistics.name(), state ? "true":"false");
				}else if(label.equals(labels[5])) {
					prop.put(SettingParams.BOOL_enableIntensityHistogram.name(), state ? "true":"false");
				}else if(label.equals(labels[6])) {
					prop.put(SettingParams.BOOL_enableIntensityVolumeHistogram.name(), state ? "true":"false");
				}else if(label.equals(labels[7])) {
					prop.put(SettingParams.BOOL_enableGLCM.name(), state ? "true":"false");
				}else if(label.equals(labels[8])) {
					prop.put(SettingParams.BOOL_enableGLRLM.name(), state ? "true":"false");
				}else if(label.equals(labels[9])) {
					prop.put(SettingParams.BOOL_enableGLSZM.name(), state ? "true":"false");
				}else if(label.equals(labels[10])) {
					prop.put(SettingParams.BOOL_enableGLDZM.name(), state ? "true":"false");
				}else if(label.equals(labels[11])) {
					prop.put(SettingParams.BOOL_enableNGTDM.name(), state ? "true":"false");
				}else if(label.equals(labels[12])) {
					prop.put(SettingParams.BOOL_enableNGLDM.name(), state ? "true":"false");
				}else if(label.equals(labels[13])) {
					prop.put(SettingParams.BOOL_enableFractal.name(), state ? "true":"false");
				}else if(label.equals(labels[14])) {
					prop.put(SettingParams.BOOL_enableShape2D.name(), state ? "true":"false");
				}
			}
			
			RadiomicsJ radiomics = new RadiomicsJ();
			//radiomics.setDebug(true);
			radiomics.loadSettings(prop);
			try {
				ResultsTable res = radiomics.execute(images, masks, RadiomicsJ.targetLabel);
				if (res != null) {
					res.show("RadiomicsJ results");
				}else {
					JOptionPane.showMessageDialog(null, "RadiomicsJ can not perform feature extraction. Please check image, mask and settings.");
				}
			} catch (Exception e) {
				IJ.log(e.getMessage());
				return;
			}
		}else {
			return;
		}
	}
}
