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

	@Test
	public void test() {
		assertFalse(Validation.ibsiDigitalPhantom());
	}
}
