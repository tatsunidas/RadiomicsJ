/*
 * Copyright [2022] [Tatsuaki Kobayashi]

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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import ij.IJ;
import ij.ImagePlus;
import ij.measure.ResultsTable;
import io.github.tatsunidas.radiomics.features.FractalFeatureType;
import io.github.tatsunidas.radiomics.features.FractalFeatures;
import io.github.tatsunidas.radiomics.features.GLKZMFeatures;
import io.github.tatsunidas.radiomics.features.Shape2DFeatureType;
import io.github.tatsunidas.radiomics.features.Shape2DFeatures;

/**
 * Test to build using maven.
 * @author tatsunidas <t_kobayashi@vis-ionary.com>
 *
 */
public class Validation {
	
	//system string color 
	//e.g., System.out.println("\u001B[36m" + "finish");
	public static final String ANSI_RED = "\u001B[31m";
	public static final String ANSI_GREEN = "\u001B[32m";
	public static final String ANSI_YELLOW = "\u001B[33m";
	public static final String ANSI_BLUE = "\u001B[34m";
	public static final String ANSI_PURPLE = "\u001B[35m";
	public static final String ANSI_CYAN = "\u001B[36m";
	
	static final String referenceFile = "validation/IBSI_ValidationFile.xlsx";
	static final String digitalPhantomSettingsParam = "validation/ParamsTestDigitalPhantom1.properties";//3D basis
//	static final String ConfigurationASettingsParam = "validation/ParamsTestCT_PAT1_Config_A.properties";//2D basis
//	static final String ConfigurationBSettingsParam = "validation/ParamsTestCT_PAT1_Config_B.properties";//2D basis
	static final String ConfigurationCSettingsParam = "validation/ParamsTestCT_PAT1_Config_C.properties";//3D basis
	static final String ConfigurationDSettingsParam = "validation/ParamsTestCT_PAT1_Config_D.properties";//3D basis
	static final String ConfigurationESettingsParam = "validation/ParamsTestCT_PAT1_Config_E.properties";//tricubic spline interpolation
	
	public enum ValidationConfigType{
		A,B,C,D,E,P//P(digital phantom1) 
	}
	
	public enum ErrorRateType{
		IgnorableErrors, //<= 5%
		MinorErrors_Small,//<= 10%
		MinorErrors_Medium,//<= 20%
		MinorErrors_Large,//<= 30%
		SeriousErrors//> 30%
	}
	
	//answers
	static HashMap<String, Double> ans_digital_phantom;
	static HashMap<String, Double> ans_config_a;
	static HashMap<String, Double> ans_config_b;
	static HashMap<String, Double> ans_config_c;
	static HashMap<String, Double> ans_config_d;
	static HashMap<String, Double> ans_config_e;
	//tolerance
	static HashMap<String, Double> tole_digital_phantom;
	static HashMap<String, Double> tole_config_a;
	static HashMap<String, Double> tole_config_b;
	static HashMap<String, Double> tole_config_c;
	static HashMap<String, Double> tole_config_d;
	static HashMap<String, Double> tole_config_e;
	
	//debug
	public static void main(String[] args) {
		Validation.ibsiDigitalPhantom();
//		Validation.ibsi_ct_PAT1(ValidationConfigType.C);
	}
	
	/**
	 * Testing calculation accuracy using the IBSI digital phantom under the default condition. 
	 * @return all clear or not
	 */
	public static boolean ibsiDigitalPhantom() {
		ImagePlus[] imgAndMask = TestDataLoader.digital_phantom1_scratch();
		try {
			boolean res1 = testWithConfig(imgAndMask, ValidationConfigType.P, digitalPhantomSettingsParam );
			//additional check
			//here, no problem if ff is no any exception.
			System.out.println("Check Original features.");
			System.out.println("Fractal feature Validation...");
			FractalFeatures ff = new FractalFeatures(imgAndMask[0], imgAndMask[1], RadiomicsJ.label_, null);
			ff.calculate(FractalFeatureType.Capacity.id());
			System.out.println("Fractal feature Validation finish.");
			
			//GLKZM test
			if(!GLKZMFeatures.test()) {
				return false;
			}
			
			try {
				System.out.println("Shape2DFeature Validation...");
				Shape2DFeatures d2f = new Shape2DFeatures(imgAndMask[0], imgAndMask[1], RadiomicsJ.label_, 1);
				d2f.calculate(Shape2DFeatureType.AreaFraction.id());
				System.out.println("Shape2DFeature Validation finish.");
			} catch (Exception e) {
				System.out.println(e);
				return false;
			}
			
			return res1;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * DEBUG ONLY.
	 *
	 * Validation with the IBSI CT radiomics phantom (PAT1).
	 * The CT phantom images are NOT packaged into the distributed jar, see
	 * announceDebugOnlyFeature(). Run it from the source tree, where the data sets
	 * of src/test/resources are on the classpath.
	 *
	 * @param type configuration C or D. (A, B and E have no settings file yet)
	 * @return all clear or not. false, when the data sets are not available.
	 */
	public static boolean ibsi_ct_PAT1(ValidationConfigType type) {
		announceDebugOnlyFeature("IBSI CT radiomics phantom (PAT1), configuration " + type);
		String paramPath = loadSettingsPropertiesByConfigType(type);
		if(paramPath == null) {
			System.out.println("Incompatible Validation Configuration Type ! :"+type);
			return false;
		}
		ImagePlus[] imgAndMask = null;
		try {
			imgAndMask = TestDataLoader.sample_ct1();
		} catch (Throwable t) {
			//IllegalStateException(resource missing), IOException, and so on.
			reportDataSetNotAvailable("ibsi_1_ct_radiomics_phantom", t);
			return false;
		}
		if(imgAndMask == null || imgAndMask[0] == null || imgAndMask[1] == null) {
			reportDataSetNotAvailable("ibsi_1_ct_radiomics_phantom", null);
			return false;
		}
		try {
			return testWithConfig(imgAndMask, type, paramPath);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Tell the user that this validation needs the bundled data sets,
	 * that are available in the source tree only.
	 *
	 * @param what name of the validation
	 */
	static void announceDebugOnlyFeature(String what) {
		System.out.println(ANSI_YELLOW + "[DEBUG ONLY] " + what);
		System.out.println("[DEBUG ONLY] Only the IBSI digital phantom validation is available in the distributed jar,");
		System.out.println("[DEBUG ONLY] because it builds the phantom in code (TestDataLoader.digital_phantom1_scratch()).");
		System.out.println("[DEBUG ONLY] Every other validation reads the IBSI data sets of src/test/resources,");
		System.out.println("[DEBUG ONLY] that are excluded from the jar to keep it small.");
	}

	/**
	 * @param dataSetName data set folder name under data_sets-master
	 * @param cause null-able
	 */
	static void reportDataSetNotAvailable(String dataSetName, Throwable cause) {
		System.err.println(ANSI_RED + "Validation: the data set \"" + dataSetName + "\" was not found on the classpath.");
		System.err.println("Validation: this is expected when running from the distributed jar.");
		System.err.println("Validation: to run it, put src/test/resources (target/test-classes) on the classpath.");
		if(cause != null) {
			System.err.println("Validation: cause -> " + cause.getClass().getName() + " : " + cause.getMessage());
		}
	}
	
	/**
	 * 
	 * @param configType A B C D E, and P(digital phantom1)
	 * @param propPath
	 * @throws Exception
	 */
	public static boolean testWithConfig(ImagePlus ds[], ValidationConfigType configType, String propPath) throws Exception {
		buildAnswers();
		System.out.println("==== ===== ===== ===== ===== ====");
		System.out.println("CONFIG TYPE\t"+configType);
		System.out.println("==== ===== ===== ===== ===== ====\n");

		RadiomicsJ radi = new RadiomicsJ();
		radi.loadSettingsFromResource(propPath);
		RadiomicsJ.force2D = false;//fail safe
		radi.setDebug(true);

		ResultsTable res = radi.execute(ds[0], ds[1], RadiomicsJ.targetLabel);
		
		if(RadiomicsJ.debug)
			System.out.println("\nFinish calculating features without any error !\n");
		
		System.out.println("===== ===== ===== ===== ===== =====");
		System.out.println("          START VALIDATION");
		System.out.println("===== ===== ===== ===== ===== =====\n");
		String header[] = res.getHeadings();
		ArrayList<String> no_matches = new ArrayList<>();
		ArrayList<String> no_match_serius = new ArrayList<>();
		ArrayList<String> errors = new ArrayList<>();
		for(String h : header) {
			String h2 = header2familyName_3D(h);
			if(h2 == null) {
				continue;
			}
			int col = res.getColumnIndex(h);
			Double ans = null;
			Double tole = null;
			if(configType == ValidationConfigType.A) {
				ans = ans_config_a.get(h2);
				tole = tole_config_a.get(h2);
			}else if(configType == ValidationConfigType.B){
				ans = ans_config_b.get(h2);
				tole = tole_config_b.get(h2);
			}else if(configType == ValidationConfigType.C){
				ans = ans_config_c.get(h2);
				tole = tole_config_c.get(h2);
			}else if(configType == ValidationConfigType.D){
				ans = ans_config_d.get(h2);
				tole = tole_config_d.get(h2);
			}else if(configType == ValidationConfigType.E){
				ans = ans_config_e.get(h2);
				tole = tole_config_e.get(h2);
			}else if(configType == ValidationConfigType.P){
				ans = ans_digital_phantom.get(h2);
				tole = tole_digital_phantom.get(h2);
			}
			
			if(ans == null) {
				if(RadiomicsJ.debug)
					System.out.println(h + " : answer is null");
				continue;
			}
			
			/*
			 * Must not use getStringValue() here.
			 * It formats the value by the ResultsTable decimal places (3 digits as default),
			 * that rounds e.g, 0.00155 to 0.002 and reports a false 30% error.
			 */
			Double dv = res.getValueAsDouble(col, 0);

			if(dv==null || Double.isNaN(dv)) {
				System.err.println(h + " : result value is null or NaN.");
				errors.add(h + " : result value is null or NaN.");
			}else {
				boolean validMatch = validMatch(dv, ans, tole);
				if(validMatch) {
					System.out.println(ANSI_CYAN+h+ " : Clear ( output: "+dv+", ans: "+ans+", tolerance: "+tole+" )");
				}else {
					double error = errorRateCheck(dv, ans, tole);
					ErrorRateType errorType = errorType(error);
					String Color = null;
					if(errorType == ErrorRateType.SeriousErrors || errorType == ErrorRateType.MinorErrors_Large) {
						Color = ANSI_RED;
						no_match_serius.add(h+ " : NoMatch ( output: "+dv+", ans: "+ans+", tolerance: "+tole+" ) "+errorType+","+IJ.d2s(error, 3));
					}else {
						Color = ANSI_PURPLE;
					}
					System.err.println(Color+h+ " : NoMatch ( output: "+dv+", ans: "+ans+", tolerance: "+tole+" ) "+errorType+","+IJ.d2s(error, 3));
					no_matches.add(h+ " : NoMatch ( output: "+dv+", ans: "+ans+", tolerance: "+tole+" ) "+errorType+","+IJ.d2s(error, 3));
				}
			}
		}
		if(no_match_serius.isEmpty() && errors.size()==0) {
			System.out.println(ANSI_CYAN+"All Clear, congrats !");
			
			System.out.println("\n===============================================");
			System.out.println("NO MATCHES (including strict/accurate calculation)");
			System.out.println("===============================================");
			System.out.println("\n"+ANSI_RED+"Please check these features...");
			for(String msg:no_matches) {
				System.out.println(msg);
			}
			return true;
		}else {
			System.out.println("\n===============================================");
			System.out.println("NO MATCHES");
			System.out.println("===============================================");
			System.out.println("\n"+ANSI_RED+"Please check these features...");
			for(String msg:no_matches) {
				System.out.println(msg);
			}
			if(errors.size() != 0) {
				System.out.println("\n===============================================");
				System.out.println("CALCULATION FAILED");
				System.out.println("===============================================");
				System.out.println("\n"+ANSI_RED+"Please check these features...");
				for(String msg:no_matches) {
					System.out.println(msg);
				}
			}
			return false;
		}
	}
	
	/**
	 * load validation excel file.
	 */
	private static void buildAnswers() {
		System.out.println("Loading answers...");
		ans_digital_phantom = new HashMap<String, Double>();
		ans_config_a = new HashMap<String, Double>();
		ans_config_b = new HashMap<String, Double>();
		ans_config_c = new HashMap<String, Double>();
		ans_config_d = new HashMap<String, Double>();
		ans_config_e = new HashMap<String, Double>();
		tole_digital_phantom = new HashMap<String, Double>();
		tole_config_a = new HashMap<String, Double>();
		tole_config_b = new HashMap<String, Double>();
		tole_config_c = new HashMap<String, Double>();
		tole_config_d = new HashMap<String, Double>();
		tole_config_e = new HashMap<String, Double>();
		
		//from jar
		if(!new File("./validation").exists()) {
			new File("./validation").mkdirs();
		}
		
		if(!new File("./"+referenceFile).exists()) {
			try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(referenceFile)) {
				Files.copy(is, new File("./" + referenceFile).toPath(), StandardCopyOption.REPLACE_EXISTING);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		try (FileInputStream excelFile = new FileInputStream(new File("./"+referenceFile));
			  Workbook workbook = new XSSFWorkbook(excelFile);) {
			for (int i = 0; i < 5; i++) {
				Sheet datatypeSheet = workbook.getSheetAt(i);// digital phantom1(0),...
				if (RadiomicsJ.debug) System.out.println(datatypeSheet.getSheetName());
				Iterator<Row> iterator = datatypeSheet.iterator();
				String ds_type = null;
				while (iterator.hasNext()) {
					Row currentRow = iterator.next();
					Iterator<Cell> cellIterator = currentRow.iterator();
					String family = null;
					String RadiomicsJ_NAME = null;
					Double benchmark_value = null;
					Double tolerance = null;
					int count = 0;
					while (cellIterator.hasNext()) {
						Cell currentCell = cellIterator.next();
						String col_name = CellReference.convertNumToColString(currentCell.getColumnIndex());
						if (col_name.equals("A") && ds_type == null) {
							ds_type = currentCell.getStringCellValue();
							if (ds_type.equals("data_set")) {
								ds_type = null;
								break;
							} else {
								continue;
							}
						}
						if (col_name.equals("B")) {
							family = currentCell.getStringCellValue();
							count++;
						} else if (col_name.equals("D")) {
							RadiomicsJ_NAME = currentCell.getStringCellValue();
							count++;
						} else if (col_name.equals("F")) {
							benchmark_value = currentCell.getNumericCellValue();
							count++;
						} else if (col_name.equals("G")) {
							tolerance = currentCell.getNumericCellValue();
							count++;
						}
						if (count == 4) {
							if (RadiomicsJ_NAME == null || RadiomicsJ_NAME.trim().length() == 0) {
								continue;
							}
							if (ds_type.equals("digital phantom")) {
								ans_digital_phantom.put(family + "_" + RadiomicsJ_NAME, benchmark_value);
								tole_digital_phantom.put(family + "_" + RadiomicsJ_NAME, tolerance);
//	                        	System.out.println(family+"_"+RadiomicsJ_NAME+" bench : "+benchmark_value+", tolerance: "+tolerance);
							} else if (ds_type.equals("configuration A")) {
								ans_config_a.put(family + "_" + RadiomicsJ_NAME, benchmark_value);
								tole_config_a.put(family + "_" + RadiomicsJ_NAME, tolerance);
							} else if (ds_type.equals("configuration B")) {
								ans_config_b.put(family + "_" + RadiomicsJ_NAME, benchmark_value);
								tole_config_b.put(family + "_" + RadiomicsJ_NAME, tolerance);
							} else if (ds_type.equals("configuration C")) {
								ans_config_c.put(family + "_" + RadiomicsJ_NAME, benchmark_value);
								tole_config_c.put(family + "_" + RadiomicsJ_NAME, tolerance);
//	                        	System.out.println(family+"_"+RadiomicsJ_NAME+" bench : "+benchmark_value+", tolerance: "+tolerance);
							} else if (ds_type.equals("configuration D")) {
								ans_config_d.put(family + "_" + RadiomicsJ_NAME, benchmark_value);
								tole_config_d.put(family + "_" + RadiomicsJ_NAME, tolerance);
							} else if (ds_type.equals("configuration E")) {
								ans_config_e.put(family + "_" + RadiomicsJ_NAME, benchmark_value);
								tole_config_e.put(family + "_" + RadiomicsJ_NAME, tolerance);
							}
							break;
						}
					}
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Finish Loading answers...");
	}
	
	static String header2familyName_3D(String header) {
		String familyName = null;
		if(header.contains("Morphology_")) {
			//return as-is
			return header;
		}else if(header.contains("LocalIntensity_")) {
			String ibName = header.substring(header.indexOf("_")+1);
			familyName = "Local intensity_"+ibName;
		}else if(header.contains("IntensityBasedStatistical_")) {
			String ibName = header.substring(header.indexOf("_")+1);
			familyName = "Statistics_"+ibName;
		}else if(header.contains("IntensityHistogram_")) {
			String ibName = header.substring(header.indexOf("_")+1);
			familyName = "Intensity histogram_"+ibName;
		}else if(header.contains("IntensityVolumeHistogram_")) {
			String ibName = header.substring(header.indexOf("_")+1);
			familyName = "Intensity volume histogram_"+ibName;
		}else if(header.contains("GLCM_")) {
			String ibName = header.substring(header.indexOf("_")+1);
			familyName = "Co-occurrence matrix (3D, averaged)_"+ibName;
		}else if(header.contains("GLRLM_")) {
			String ibName = header.substring(header.indexOf("_")+1);
			familyName = "Run length matrix (3D, averaged)_"+ibName;
		}else if(header.contains("GLSZM_")) {
			String ibName = header.substring(header.indexOf("_")+1);
			familyName = "Size zone matrix (3D)_"+ibName;
		}else if(header.contains("GLDZM_")) {
			String ibName = header.substring(header.indexOf("_")+1);
			familyName = "Distance zone matrix (3D)_"+ibName;
		}else if(header.contains("NGTDM_")) {
			String ibName = header.substring(header.indexOf("_")+1);
			familyName = "Neighbourhood grey tone difference matrix (3D)_"+ibName;
		}else if(header.contains("NGLDM_")) {
			String ibName = header.substring(header.indexOf("_")+1);
			familyName = "Neighbouring grey level dependence matrix (3D)_"+ibName;
		}
		return familyName;
	}
	
	private static boolean validMatch(Double out, Double ans, Double tole) {
		if((out >= (ans-tole)) && out <= (ans+tole)) {
			return true;
		}else {
			double dif = Math.abs(out - ans);
			return (dif/ans) < 0.01;//to avoid rounding error
		}
	}
	
	private static double errorRateCheck(Double out, Double ans, Double tole) {
		double under_error = Math.abs(out-(ans - tole))/(ans - tole);
		double top_error = Math.abs(out-(ans + tole))/(ans + tole);
		if(under_error >= top_error) {
			return top_error;
		}else {
			return under_error;
		}
	}
	
	private static ErrorRateType errorType(double errorRate) {
		if(errorRate <= 0.05) {
			return ErrorRateType.IgnorableErrors;
		}else if(errorRate <= 0.10) {
			return ErrorRateType.MinorErrors_Small;
		}else if(errorRate <= 0.20) {
			return ErrorRateType.MinorErrors_Medium;
		}else if(errorRate <= 0.30) {
			return ErrorRateType.MinorErrors_Large;
		}else {
			return ErrorRateType.SeriousErrors;
		}
	}
	
	static String loadSettingsPropertiesByConfigType(ValidationConfigType t) {
		switch (t) {
		case A:return null;
		case B:return null;
		case C:return ConfigurationCSettingsParam;
		case D:return ConfigurationDSettingsParam;
		case E:return ConfigurationESettingsParam;
		case P:return digitalPhantomSettingsParam;
		default:return null;
		}
	}

	// ---------------------------------------------------------------------------------------------
	// Structured report (does not print, does not write files). See ValidationReport for the rule.
	// ---------------------------------------------------------------------------------------------

	private static final double TOLERANCE_EPS = 1e-9;
	static final int MIN_SIGNIFICANT_DIGITS = 3;

	/**
	 * Validate against the IBSI 1 reference values and return a structured report.
	 * <p>
	 * Nothing is printed and no file is written (the reference table is read from the classpath stream).
	 * Static settings of {@link RadiomicsJ} are changed while it runs (as in the legacy methods) and are
	 * restored to their defaults at the end, so <b>call it while no other thread uses RadiomicsJ</b>.
	 *
	 * @param type P (digital phantom, built in code, works from the distributed jar), C or D (CT radiomics
	 *             phantom; needs the data sets of src/test/resources on the classpath, like {@link #ibsi_ct_PAT1})
	 * @throws IllegalArgumentException for configurations that have no settings file (A, B, E)
	 * @throws IllegalStateException    when the data set or the reference table is not available, or the
	 *                                  calculation fails
	 */
	public static ValidationReport report(ValidationConfigType type) {
		return report(type, null, null);
	}

	/**
	 * @param referenceXlsx reference table stream to use instead of the bundled one (tests, other versions). null: bundled
	 * @param settingsOverride properties text to use instead of the bundled settings file. null: bundled
	 */
	static ValidationReport report(ValidationConfigType type, InputStream referenceXlsx, String settingsOverride) {
		String dataSetName;
		switch (type) {
		case P:
			dataSetName = "digital phantom";
			break;
		case C:
			dataSetName = "configuration C";
			break;
		case D:
			dataSetName = "configuration D";
			break;
		default:
			throw new IllegalArgumentException("No settings file for configuration " + type + " (only P, C and D can be validated).");
		}
		String settingsFile = loadSettingsPropertiesByConfigType(type);
		ImagePlus[] ds;
		try {
			ds = type == ValidationConfigType.P ? TestDataLoader.digital_phantom1_scratch() : TestDataLoader.sample_ct1();
		} catch (Throwable t) {
			throw new IllegalStateException("The data set for configuration " + type + " is not available: " + t, t);
		}
		if (ds == null || ds[0] == null || ds[1] == null) {
			throw new IllegalStateException("The data set for configuration " + type + " is not available.");
		}

		IbsiReferenceTable table;
		try (InputStream in = referenceXlsx != null ? referenceXlsx
				: Thread.currentThread().getContextClassLoader().getResourceAsStream(referenceFile)) {
			if (in == null) {
				throw new IllegalStateException("Reference table not found on the classpath: " + referenceFile);
			}
			table = IbsiReferenceTable.read(in);
		} catch (IOException e) {
			throw new IllegalStateException("Cannot read the reference table: " + e.getMessage(), e);
		}

		String settingsText = settingsOverride != null ? settingsOverride : readResourceText(settingsFile);
		java.util.Properties prop = new java.util.Properties();
		try {
			prop.load(new java.io.StringReader(settingsText));
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}

		// RadiomicsJ prints progress to stdout/stderr regardless of its debug flag. Keep the report silent.
		java.io.PrintStream out0 = System.out;
		java.io.PrintStream err0 = System.err;
		java.io.PrintStream sink = new java.io.PrintStream(new java.io.OutputStream() {
			@Override
			public void write(int b) {
			}
		});
		ResultsTable res;
		try {
			System.setOut(sink);
			System.setErr(sink);
			RadiomicsJ radi = new RadiomicsJ();
			radi.loadSettings(prop);
			RadiomicsJ.force2D = false;
			radi.setDebug(false);
			res = radi.execute(ds[0], ds[1], RadiomicsJ.targetLabel);
		} catch (Exception e) {
			throw new IllegalStateException("The calculation failed: " + e, e);
		} finally {
			System.setOut(out0);
			System.setErr(err0);
			RadiomicsJ.resetSettings();
		}

		// result column of every 3D family name
		HashMap<String, String> columnOf = new HashMap<>();
		for (String h : res.getHeadings()) {
			String key = header2familyName_3D(h);
			if (key != null) {
				columnOf.put(key, h);
			}
		}
		java.util.List<ValidationReport.Row> rows = new ArrayList<>();
		int diagnostics = 0;
		for (IbsiReferenceTable.Entry e : table.of(dataSetName)) {
			if (e.family != null && e.family.startsWith("Diagnostics")) {
				diagnostics++;
				continue;
			}
			if (e.value == null) {
				continue;// no reference value: nothing to validate
			}
			String key = e.family + "_" + e.radiomicsjName;
			String column = e.radiomicsjName == null ? null : columnOf.get(key);
			Double computed = null;
			if (column != null) {
				double d = res.getValueAsDouble(res.getColumnIndex(column), 0);
				computed = Double.isNaN(d) || Double.isInfinite(d) ? null : d;
			}
			ValidationReport.Verdict v;
			if (e.referenceStatus != null && !e.referenceStatus.isEmpty()) {
				v = ValidationReport.Verdict.NO_MANUAL_REFERENCE;
			} else if (e.radiomicsjName == null || !isSupported3DFamily(e.family)) {
				v = ValidationReport.Verdict.NOT_SUPPORTED;
			} else if (column == null || computed == null) {
				v = ValidationReport.Verdict.NOT_COMPUTED;
			} else {
				v = judge(computed, e.value, e.tolerance);
			}
			rows.add(new ValidationReport.Row(e.family, e.imageBiomarker, e.radiomicsjName, column, e.consensus, e.value,
					e.tolerance, computed, v, e.referenceStatus));
		}
		return new ValidationReport(dataSetName, RadiomicsJ.version, referenceXlsx != null ? "(supplied)" : referenceFile,
				table.sha256, settingsFile, settingsText, rows, diagnostics);
	}

	/** families that {@link #header2familyName_3D} produces (3D features only). */
	private static boolean isSupported3DFamily(String family) {
		if (family == null) {
			return false;
		}
		return family.equals("Morphology") || family.equals("Local intensity") || family.equals("Statistics")
				|| family.equals("Intensity histogram") || family.equals("Intensity volume histogram")
				|| family.equals("Co-occurrence matrix (3D, averaged)") || family.equals("Run length matrix (3D, averaged)")
				|| family.equals("Size zone matrix (3D)") || family.equals("Distance zone matrix (3D)")
				|| family.equals("Neighbourhood grey tone difference matrix (3D)")
				|| family.equals("Neighbouring grey level dependence matrix (3D)");
	}

	/** the verdict of one computed value, see {@link ValidationReport#RULE}. */
	static ValidationReport.Verdict judge(double computed, double reference, Double tolerance) {
		if (tolerance != null) {
			double diff = Math.abs(computed - reference);
			return diff <= tolerance * (1 + TOLERANCE_EPS) ? ValidationReport.Verdict.PASS : ValidationReport.Verdict.FAIL;
		}
		// no tolerance: equal at N significant digits, N = max(3, digits of the printed reference value).
		// The reference values of the table are printed with at most 3 significant digits (checked in the tests).
		java.math.BigDecimal ref = java.math.BigDecimal.valueOf(reference).stripTrailingZeros();
		java.math.MathContext mc = new java.math.MathContext(Math.max(MIN_SIGNIFICANT_DIGITS, ref.precision()),
				java.math.RoundingMode.HALF_UP);
		return new java.math.BigDecimal(computed).round(mc).compareTo(ref.round(mc)) == 0
				? ValidationReport.Verdict.PASS_AT_REFERENCE_DIGITS
				: ValidationReport.Verdict.FAIL;
	}

	private static String readResourceText(String resource) {
		try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)) {
			if (in == null) {
				throw new IllegalStateException("Settings file not found on the classpath: " + resource);
			}
			java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
			byte[] buf = new byte[8192];
			int n;
			while ((n = in.read(buf)) > 0) {
				bos.write(buf, 0, n);
			}
			return new String(bos.toByteArray(), java.nio.charset.StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

}
