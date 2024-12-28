package radiomics;

import ij.ImagePlus;
import ij.plugin.FolderOpener;
import io.github.tatsunidas.radiomics.features.GLDZMFeatureType;
import io.github.tatsunidas.radiomics.features.GLDZMFeatures;
import io.github.tatsunidas.radiomics.features.GLKZMFeatures;
import io.github.tatsunidas.radiomics.main.Utils;

public class BoundingBox {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String mp = "/home/tatsunidas/ダウンロード/case07_lumen_mask_in_tof";
		String fp = "/home/tatsunidas/ダウンロード/case07_fvm_padding";
		String ip = "/home/tatsunidas/ダウンロード/case7/20221125/DATA/ANON2024/20241115/085030/EX1/SE1";
		
		ImagePlus images = FolderOpener.open(ip);
		ImagePlus fvms = FolderOpener.open(fp);
		ImagePlus masks = FolderOpener.open(mp);
		
		ImagePlus crop_images = Utils.trimToBoundingBox(images, masks, 255, null);
		ImagePlus crop_fvms = Utils.trimToBoundingBox(fvms, masks, 255, null);
		ImagePlus crop_masks = Utils.trimToBoundingBox(masks, masks, 255, null);
		
		try {
			GLKZMFeatures glfzm = new GLKZMFeatures(crop_images, crop_masks, 255, true, 16, null);
			glfzm.setKineticsMap(crop_fvms, false, null, 0.1);
			glfzm.fillMatrix();
			
			System.out.println(glfzm.calculate(GLDZMFeatureType.SmallDistanceEmphasis.id()));
			System.out.println(glfzm.calculate(GLDZMFeatureType.LargeDistanceEmphasis.id()));
			System.out.println(glfzm.calculate(GLDZMFeatureType.LowGrayLevelZoneEmphasis.id()));
			System.out.println(glfzm.calculate(GLDZMFeatureType.HighGrayLevelZoneEmphasis.id()));
			System.out.println(glfzm.calculate(GLDZMFeatureType.SmallDistanceLowGrayLevelEmphasis.id()));
			System.out.println(glfzm.calculate(GLDZMFeatureType.SmallDistanceHighGrayLevelEmphasis.id()));
			System.out.println(glfzm.calculate(GLDZMFeatureType.LargeDistanceLowGrayLevelEmphasis.id()));
			System.out.println(glfzm.calculate(GLDZMFeatureType.LargeDistanceHighGrayLevelEmphasis.id()));
			System.out.println(glfzm.calculate(GLDZMFeatureType.GrayLevelNonUniformity.id()));
			System.out.println(glfzm.calculate(GLDZMFeatureType.GrayLevelNonUniformityNormalized.id()));
			System.out.println(glfzm.calculate(GLDZMFeatureType.ZoneDistanceNonUniformity.id()));
			System.out.println(glfzm.calculate(GLDZMFeatureType.ZoneDistanceNonUniformityNormalized.id()));
			System.out.println(glfzm.calculate(GLDZMFeatureType.ZonePercentage.id()));
			System.out.println(glfzm.calculate(GLDZMFeatureType.GrayLevelVariance.id()));
			System.out.println(glfzm.calculate(GLDZMFeatureType.ZoneDistanceVariance.id()));
			System.out.println(glfzm.calculate(GLDZMFeatureType.ZoneDistanceEntropy.id()));
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
