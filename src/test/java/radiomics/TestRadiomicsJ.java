package radiomics;

import io.github.tatsunidas.radiomics.main.Validation;

public class TestRadiomicsJ {

	public static void main(String[] args) throws Exception {
		if(!Validation.ibsiDigitalPhantom()) {
			throw new Exception("Digital phantom validation failed !");
		};
	}
}
