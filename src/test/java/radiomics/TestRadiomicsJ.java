package radiomics;

import features.RadiomicsJ;
import features.TestDataLoader;
import features.Utils;
import ij.ImagePlus;
import ij.measure.ResultsTable;

import org.apache.poi.ss.usermodel.Sheet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class TestRadiomicsJ {
	
	static String referenceFile = "D:\\Dropbox\\Graphy-PlugIns\\radiomics\\IBSI_ValidationFile.xlsx";
	/*
	 * answers
	 */
	HashMap<String, Double> ans_digital_phantom;
	HashMap<String, Double> ans_config_a;
	HashMap<String, Double> ans_config_b;
	HashMap<String, Double> ans_config_c;
	HashMap<String, Double> ans_config_d;
	HashMap<String, Double> ans_config_e;
	//tolerance
	HashMap<String, Double> tole_digital_phantom;
	HashMap<String, Double> tole_config_a;
	HashMap<String, Double> tole_config_b;
	HashMap<String, Double> tole_config_c;
	HashMap<String, Double> tole_config_d;
	HashMap<String, Double> tole_config_e;

	public static void main(String[] args) throws Exception {
				
		//test1
//		args = new String[] {
//				"-d",
//				"-i",
//				"D:\\Dropbox\\Graphy-PlugIns\\radiomics\\src\\test\\resources\\data_sets-master\\ibsi_1_digital_phantom\\nifti\\image\\phantom.nii.gz",
//				"-m",
//				"D:\\Dropbox\\Graphy-PlugIns\\radiomics\\src\\test\\resources\\data_sets-master\\ibsi_1_digital_phantom\\nifti\\mask\\mask.nii.gz",
//		};
		
		//test2
//		args = new String[] {
//				"-d",
//				"-i",
//				"D:\\Dropbox\\Graphy-PlugIns\\radiomics\\src\\test\\resources\\data_sets-master\\ibsi_1_digital_phantom\\nifti\\image",
//				"-m",
//				"D:\\Dropbox\\Graphy-PlugIns\\radiomics\\src\\test\\resources\\data_sets-master\\ibsi_1_digital_phantom\\nifti\\mask",
//		};
		
		//test3
//		args = new String[] {
//				"-d",
//				"-i",
//				"D:\\Dropbox\\Graphy-PlugIns\\radiomics\\src\\test\\resources\\data_sets-master\\ibsi_1_digital_phantom\\nifti\\image",
//				"-m",
//				"D:\\Dropbox\\Graphy-PlugIns\\radiomics\\src\\test\\resources\\data_sets-master\\ibsi_1_digital_phantom\\nifti\\mask",
//				"-o",
//				"C:\\Users\\tatsunidas\\Desktop"
//		};
		
		//test4 : force2D
//		args = new String[] {
//				"-d",
//				"-s",
//				"D:\\Dropbox\\Graphy-PlugIns\\radiomics\\ParamsTest_Force2D_CT_PAT1_Config_C.properties",
//				"-i",
//				"D:\\Dropbox\\Graphy-PlugIns\\radiomics\\src\\test\\resources\\data_sets-master\\ibsi_1_ct_radiomics_phantom\\nifti\\image\\phantom.nii.gz",
//				"-m",
//				"D:\\Dropbox\\Graphy-PlugIns\\radiomics\\src\\test\\resources\\data_sets-master\\ibsi_1_ct_radiomics_phantom\\nifti\\mask\\mask.nii.gz",
//				"-o",
//				"C:\\Users\\tatsunidas\\Desktop"
//		};
		
//		RadiomicsJ.main(args);
		
		TestRadiomicsJ test = new TestRadiomicsJ();
		test.buildAnswers();
		
		/*
		 * sandbox
		 */
//		test.testSandbox();
		
		/*
		 * digital phantom test
		 */
		test.testWithConfig("P", "D:\\Dropbox\\Graphy-PlugIns\\radiomics\\ParamsTestDigitalPhantom1.properties");
		
		/*
		 * CT PAT1 test
		 */
		//config c
//		test.testWithConfig("C", "D:\\Dropbox\\Graphy-PlugIns\\radiomics\\ParamsTestCT_PAT1_Config_C.properties");
		
		//config d
//		test.testWithConfig("D", "D:\\Dropbox\\Graphy-PlugIns\\radiomics\\ParamsTestCT_PAT1_Config_D.properties");
		
		//config e
		/*
		 * tricubic interpolation is not implemented.
		 */
		
		/*
		 * test JSRT 2022
		 */
//		test.testJSRT2022();
		
		/*
		 * PET test
		 */
//		testValidationPET_STS001();
		
		/*
		 * 1p-19q
		 */
//		test.testValidation_1p19q("LGG-104");
		
	}
	
	/**
	 *  when using main
	 *  datatTyps 0 = digital phantom, 1 = PAT1 CT
	 */
	public void testRadiomicsMain(String propPath, String dataType) {
		String[] arg = new String[] {
				"-s",
				propPath,
				"-d",//debug
				"-tdt",//test data type, 0:phantom, 1:PAT1(CT).
				dataType
		};
		RadiomicsJ.main(arg);
	}
	
	public void testSandbox() throws Exception {
		System.out.println("==== ===== ===== ===== ===== ====");
		System.out.println("=====         TEST          =====");
		System.out.println("==== ===== ===== ===== ===== ====");
		ImagePlus ds[] = TestDataLoader.sample_ct1();
		RadiomicsJ radi = new RadiomicsJ();
		radi.loadSettings("D:\\Dropbox\\Graphy-PlugIns\\radiomics\\ParamsSandbox.properties");
		radi.setDebug(true);
		radi.execute(ds[0], ds[1], RadiomicsJ.targetLabel);
		System.out.println("finish calculation without any error !");
		System.exit(0);
	}
	
	
	/**
	 * 
	 * @param configType A B C D E, and P(digital phantom1)
	 * @param propPath
	 * @throws Exception
	 */
	public void testWithConfig(String configType, String propPath) throws Exception {
		System.out.println("==== ===== ===== ===== ===== ====");
		System.out.println("=====     CONFIG "+configType+" TEST     =====");
		System.out.println("==== ===== ===== ===== ===== ====");
		ImagePlus ds[] = null;
		if(configType.equals("P")) {
			ds = TestDataLoader.digital_phantom1();
		}else {
			ds = TestDataLoader.sample_ct1();
		}
		RadiomicsJ radi = new RadiomicsJ();
		radi.loadSettings(propPath);
		radi.setDebug(true);
		ResultsTable res = radi.execute(ds[0], ds[1],RadiomicsJ.targetLabel);
		System.out.println("finish calculation without any error !");
		System.out.println(" ===  START VALIDATION  ===");
		String header[] = res.getHeadings();
		for(String h : header) {
			String h2 = header2familyName_3D(h);
			if(h2 == null) {
				continue;
			}
			int col = res.getColumnIndex(h);
			Double ans = null;
			Double tole = null;
			if(configType.equals("A")) {
				ans = ans_config_a.get(h2);
				tole = tole_config_a.get(h2);
			}else if(configType.equals("B")){
				ans = ans_config_b.get(h2);
				tole = tole_config_b.get(h2);
			}else if(configType.equals("C")){
				ans = ans_config_c.get(h2);
				tole = tole_config_c.get(h2);
			}else if(configType.equals("D")){
				ans = ans_config_d.get(h2);
				tole = tole_config_d.get(h2);
			}else if(configType.equals("E")){
				ans = ans_config_e.get(h2);
				tole = tole_config_e.get(h2);
			}else if(configType.equals("P")){
				ans = ans_digital_phantom.get(h2);
				tole = tole_digital_phantom.get(h2);
			}
			
			if(ans == null) {
				System.out.println(h + " : answer is null");
				continue;
			}
			
			String sv = res.getStringValue(col, 0);
			Double dv = Double.valueOf(sv);
			if(dv==null || Double.isNaN(dv)) {
				System.err.println(h + " : result value is null or NaN.");
			}else {
				dv = roundOff(dv , ans);
				if((dv >= (ans-tole)) && dv <= (ans+tole)) {
					System.out.println(h+ " : Clear ( output: "+dv+", ans: "+ans+", tolerance: "+tole+" )");
				}else {
					System.err.println(h+ " : NotMatch ( output: "+dv+", ans: "+ans+", tolerance: "+tole+" )");
				}
			}
		}
		System.exit(0);
	}
	
	
	public void testValidationPET_STS001() throws Exception {
		ImagePlus ds[] = TestDataLoader.validationDataAt("001", "PET");
		RadiomicsJ radi = new RadiomicsJ();
		radi.loadSettings("D:\\Dropbox\\Graphy-PlugIns\\radiomics\\ParamsTestPET.properties");
		radi.setDebug(true);
		ResultsTable res = radi.execute(ds[0], ds[1],RadiomicsJ.targetLabel);
		if(res != null) {
			res.show("Radiomics Features");
		}
		System.out.println("finish calculation without any error !");
		System.exit(0);
	}
	
	public void testValidation_1p19q(String subj) throws Exception {
		RadiomicsJ radi = new RadiomicsJ();
		radi.loadSettings("D:\\Dropbox\\Graphy-PlugIns\\radiomics\\ParamsTest_Config_JSRT2022.properties");
		radi.setDebug(true);
		String p2i = "C:\\Users\\tatsunidas\\Desktop\\resampled"+File.separator+subj+File.separator+"t1ce.nii.gz";
		String p2m = "C:\\Users\\tatsunidas\\Desktop\\resampled"+File.separator+subj+File.separator+"mask.nii.gz";
		ImagePlus imp = TestDataLoader.loadNifTi(p2i);
		ImagePlus mask = TestDataLoader.loadNifTi(p2m);
		imp.show();
		mask.show();
		ResultsTable res = radi.execute(imp, mask, 255);
		if(res != null) {
			res.show(RadiomicsJ.resultWindowTitle);
		}
		System.out.println("finish calculation without any error !");
	}
	
	public void testJSRT2022() throws Exception {
		RadiomicsJ radi = new RadiomicsJ();
		radi.loadSettings("D:\\Dropbox\\Graphy-PlugIns\\radiomics\\ParamsTest_Config_JSRT2022.properties");
		radi.setDebug(true);
		File[] subjects = new File("C:\\Users\\tatsunidas\\Desktop\\resampled").listFiles();
		ResultsTable agg_res = null; //result aggregation table
		@SuppressWarnings("unused")
		int itr = 0;
		@SuppressWarnings("unused")
		int stop = 3;
		for(File subj : subjects) {
//			if(itr == stop){
//				break;
//			}
			
			String subj_name = subj.getName();
			
//			if(!subj_name.equals("LGG-231")) {
//				continue;
//			}
			
			System.out.println("*********************************************");
			System.out.println("   START CALCULATION : "+subj_name);
			System.out.println("*********************************************");
			File t1ce = new File(subj.getAbsolutePath()+File.separator+"t1ce.nii.gz");
			File t2 = new File(subj.getAbsolutePath()+File.separator+"t2.nii.gz");
			File mask = new File(subj.getAbsolutePath()+File.separator+"mask.nii.gz");
			ResultsTable res = new ResultsTable();
			res.incrementCounter();
			res.addValue("ID", subj_name);
			ResultsTable res1 = radi.execute(t1ce, mask,RadiomicsJ.targetLabel);
			ResultsTable res2 = radi.execute(t2, mask,RadiomicsJ.targetLabel);
			String[] header = res1.getHeadings();
			for(String h:header) {
				if(h.contains("OperationalInfo_")) {
					String v = res1.getStringValue(h, 0);
					res.addValue("T1WCE_"+h, v);
				}else {
					double v = res1.getValue(h, 0);
					res.addValue("T1WCE_"+h, v);
				}
			}
			for(String h:header) {
				if(h.contains("OperationalInfo_")) {
					String v = res2.getStringValue(h, 0);
					res.addValue("T2W_"+h, v);
				}else {
					double v = res2.getValue(h, 0);
					res.addValue("T2W_"+h, v);
				}
			}
			agg_res = Utils.combineTables(agg_res, res);
//			itr ++;
			System.gc();
		}
		agg_res.save("JSRT_result.csv");
		agg_res.show("JSRT 2022 result");
	}
	
	public void buildAnswers() {
		System.out.println("Loading answers...");
		String path = referenceFile;
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
		try (
				FileInputStream excelFile = new FileInputStream(new File(path));
				Workbook workbook = new XSSFWorkbook(excelFile);
			){
            
			for(int i = 0;i<5;i++) {
				Sheet datatypeSheet = workbook.getSheetAt(i);//digital phantom1(0),...
				System.out.println(datatypeSheet.getSheetName());
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
//	                	if(datatypeSheet.getSheetName().equals("config C")) {
//	                		System.out.println();
//	                	}
	                    Cell currentCell = cellIterator.next();
	                    /*
	                     * A data_set	
	                     * B family	
	                     * C image_biomarker	
	                     * D RadiomicsJ_NAME	
	                     * E consensus	
	                     * F benchmark_value	
	                     * G tolerance
	                     */
//	                    if(datatypeSheet.getSheetName().equals("config C")) {
//	                    	System.out.println();
//	                    }
	                    String col_name = CellReference.convertNumToColString(currentCell.getColumnIndex());
	                    if(col_name.equals("A") && ds_type == null) {
	                    	ds_type = currentCell.getStringCellValue();
	                    	if(ds_type.equals("data_set")) {
	                    		ds_type = null;
	                    		break;
	                    	}else {
	                    		continue;
	                    	}
	                    }
//	                    if(ds_type == null) {
//	                    	System.out.println();
//	                    }
//	                    if(ds_type.equals("data_set")) {
//	                    	continue;
//	                    }
	                    if(col_name.equals("B")) {
	                    	family = currentCell.getStringCellValue(); count++;
	                    }else if(col_name.equals("D")) {
	                    	RadiomicsJ_NAME = currentCell.getStringCellValue();count++;
	                    }else if(col_name.equals("F")) {
	                    	benchmark_value = currentCell.getNumericCellValue();count++;
	                    }else if(col_name.equals("G")) {
	                    	tolerance = currentCell.getNumericCellValue();count++;
	                    }
	                    if(count == 4) {
	                    	if(RadiomicsJ_NAME == null || RadiomicsJ_NAME.trim().length()==0) {
                        		continue;
                        	}
	                    	if(ds_type.equals("digital phantom")) {
	                        	ans_digital_phantom.put(family+"_"+RadiomicsJ_NAME, benchmark_value);
	                        	tole_digital_phantom.put(family+"_"+RadiomicsJ_NAME, tolerance);
//	                        	System.out.println(family+"_"+RadiomicsJ_NAME+" bench : "+benchmark_value+", tolerance: "+tolerance);
	                        }else if(ds_type.equals("configuration A")) {
	                        	ans_config_a.put(family+"_"+RadiomicsJ_NAME, benchmark_value);
	                        	tole_config_a.put(family+"_"+RadiomicsJ_NAME, tolerance);
	                        }else if(ds_type.equals("configuration B")) {
	                        	ans_config_b.put(family+"_"+RadiomicsJ_NAME, benchmark_value);
	                        	tole_config_b.put(family+"_"+RadiomicsJ_NAME, tolerance);
	                        }else if(ds_type.equals("configuration C")) {
	                        	ans_config_c.put(family+"_"+RadiomicsJ_NAME, benchmark_value);
	                        	tole_config_c.put(family+"_"+RadiomicsJ_NAME, tolerance);
//	                        	System.out.println(family+"_"+RadiomicsJ_NAME+" bench : "+benchmark_value+", tolerance: "+tolerance);
	                        }else if(ds_type.equals("configuration D")) {
	                        	ans_config_d.put(family+"_"+RadiomicsJ_NAME, benchmark_value);
	                        	tole_config_d.put(family+"_"+RadiomicsJ_NAME, tolerance);
	                        }else if(ds_type.equals("configuration E")) {
	                        	ans_config_e.put(family+"_"+RadiomicsJ_NAME, benchmark_value);
	                        	tole_config_e.put(family+"_"+RadiomicsJ_NAME, tolerance);
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
	}
	
	String header2familyName_3D(String header) {
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
	
	Double roundOff(Double out, Double ans) {
		double a = ans;
		double b = out;
		String numString = String.valueOf(a);
		int decimal = numString.substring(numString.indexOf(".")+1).length();
	    double roundOff = Math.round(b * Math.pow(10, decimal))/Math.pow(10, decimal);
//		System.out.println(roundOff);
	    return roundOff;
	}
	
}
