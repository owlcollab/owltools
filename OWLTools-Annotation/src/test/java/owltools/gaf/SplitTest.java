package owltools.gaf;

import static org.junit.Assert.*;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

public class SplitTest {

	/**
	 * Assert that the regex and the {@link StringUtils} produce the same result.
	 * 
	 * @see GAFParser
	 */
	@Test
	public void testSplit() {
		final String source = "a\t\t\ta";
		String[] splitOld = source.split("\\t");
		String[] splitNew = StringUtils.splitPreserveAllTokens(source, '\t');
		assertArrayEquals(splitOld, splitNew);
	}
}
