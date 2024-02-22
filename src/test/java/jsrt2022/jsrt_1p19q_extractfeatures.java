package jsrt2022;

import ij.ImagePlus;
import ij.measure.ResultsTable;
import io.github.tatsunidas.radiomics.main.RadiomicsJ;
import io.github.tatsunidas.radiomics.main.TestDataLoader;
import io.github.tatsunidas.radiomics.main.Utils;
import java.io.File;

/**
 * Example
 * @author tatsunidas
 *
 */
public class jsrt_1p19q_extractfeatures {

	/**
	 * eXtract feature from stack.
	 * @param subj
	 * @throws Exception
	 */
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
	
	/**
	 * Extract feature all folders and series.
	 * @throws Exception
	 */
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
	
}
