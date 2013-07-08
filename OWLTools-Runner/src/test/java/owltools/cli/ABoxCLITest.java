package owltools.cli;

import org.junit.Test;

/**
 * Tests for {@link CommandRunner}.
 * 
 * these are somewhat ad-hoc at the moment - output is written to stdout;
 * no attempt made to check results
 * 
 */
public class ABoxCLITest extends AbstractCommandRunnerTest {
	
	@Test
	public void testABoxToTBox() throws Exception {
		load("abox_test.owl");
		run("--abox-to-tbox -o -f obo target/abox.obo");
				
	}
	
}
