package radiomics;

import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.Test;
import io.github.tatsunidas.radiomics.main.Validation;

/**
 * Unit test
 * @author tatsunidas
 *
 */
public class TestRadiomicsJ {
	
	public static void main(String args[]) {
		new TestRadiomicsJ().test();
	}

	@Test
	public void test() {
		//validation success will return true. 
		assertFalse(Validation.ibsiDigitalPhantom()==false);
	}
}
