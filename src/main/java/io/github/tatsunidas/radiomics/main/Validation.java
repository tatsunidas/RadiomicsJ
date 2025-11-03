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
			
			System.out.println("Shape2DFeature Validation...");
			Shape2DFeatures d2f = new Shape2DFeatures(imgAndMask[0], imgAndMask[1], RadiomicsJ.label_, 1);
			d2f.calculate(Shape2DFeatureType.AreaFraction.id());
			System.out.println("Shape2DFeature Validation finish.");
			
			return res1;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public static boolean ibsi_ct_PAT1(ValidationConfigType type) {
		String paramPath = loadSettingsPropertiesByConfigType(type);
		if(paramPath == null) {
			System.out.println("Incompatible Validation Configuration Type ! :"+type);
			return false;
		}
		ImagePlus[] imgAndMask = TestDataLoader.sample_ct1();
		try {
			return testWithConfig(imgAndMask, type, paramPath);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
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

		ResultsTable res = radi.execute(ds[0], ds[1],RadiomicsJ.targetLabel);
		
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
			
			String sv = res.getStringValue(col, 0);
			Double dv = Double.valueOf(sv);
			
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
	
}
