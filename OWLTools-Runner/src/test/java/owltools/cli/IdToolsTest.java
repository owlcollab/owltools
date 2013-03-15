package owltools.cli;

import static org.junit.Assert.*;

import org.junit.Test;

public class IdToolsTest {

	@Test
	public void testIsIRIStyleIdSuffix() {
		is("BFO_0000050");
		is("RO_0002313");
		not("00_000");
		not("part_of");
		not("XX_XX");
		not("XX__000");
	}

	@Test
	public void testConvertToOboStyleId() {
		convert("BFO_0000050", "BFO:0000050");
		convert("RO_0002313", "RO:0002313");
	}

	private void convert(String id, String converted) {
		assertEquals(converted, IdTools.convertToOboStyleId(id));
	}
	
	private void is(String id) {
		assertTrue("Expected '"+id+"' to be recognized as ID", IdTools.isIRIStyleIdSuffix(id));
	}
	
	private void not(String id) {
		assertFalse("Expected '"+id+"' NOT to be recognized as ID", IdTools.isIRIStyleIdSuffix(id));
	}
}
