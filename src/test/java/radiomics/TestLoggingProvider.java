package radiomics;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.logging.log4j.util.ProviderUtil;
import org.junit.jupiter.api.Test;

/**
 * Apache POI logs through the Log4j API. Without an implementation on the class path the API
 * prints "Log4j API could not find a logging provider." on the first use, for instance when
 * the validation reads its Excel file.
 */
public class TestLoggingProvider {

	@Test
	public void log4jApiHasAProvider() {
		assertTrue(ProviderUtil.hasProviders(), "no Log4j provider on the class path");
	}
}
