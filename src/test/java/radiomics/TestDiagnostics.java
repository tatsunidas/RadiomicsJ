package radiomics;

import RadiomicsJ.DiagnosticsInfo;
import RadiomicsJ.RadiomicsJ;
import RadiomicsJ.TestDataLoader;
import ij.ImagePlus;

public class TestDiagnostics {

	public static void main(String[] args) {
		ImagePlus ds_pair[] = TestDataLoader.sample_ct1();
		ImagePlus imp = ds_pair[0];
		ImagePlus mask = ds_pair[1];
		RadiomicsJ.targetLabel = 1;
		
		RadiomicsJ radi = new RadiomicsJ();
		radi.loadSettings("D:\\Dropbox\\Graphy-PlugIns\\radiomics\\ParamsTestCT_PAT1_Config_C.properties");
		radi.setDebug(true);
			
		
		ImagePlus resampled[] = radi.preprocessResample(imp, mask);
		ImagePlus re_segMask = radi.preprocessResegment(resampled[0], resampled[1],1);
		DiagnosticsInfo di = new DiagnosticsInfo(imp, mask, resampled[0], resampled[1], re_segMask, 1);
		di.toString();
		
		System.exit(0);
	}

}
