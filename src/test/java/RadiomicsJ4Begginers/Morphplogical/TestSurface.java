package RadiomicsJ4Begginers.Morphplogical;

import com.vis.radiomics.features.MorphologicalFeatureType;
import com.vis.radiomics.features.MorphologicalFeatures;
import com.vis.radiomics.main.TestDataLoader;

import ij.ImagePlus;

public class TestSurface {

	public static void main(String[] args) {
		ImagePlus[] imgAndMask = TestDataLoader.digital_phantom1();
		MorphologicalFeatures molph = new MorphologicalFeatures(imgAndMask[0], imgAndMask[1], 1);
		
		Double surface = molph.calculate(MorphologicalFeatureType.SurfaceAreaMesh.id());
		System.out.println("Volume (Mesh):" + surface);
	}

}
