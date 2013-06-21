package owltools.cli;

import static junit.framework.Assert.*;

import java.io.File;

import org.junit.Test;

import owltools.OWLToolsTestBasics;

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
		init();
		load("abox_test.owl");
		run("--abox-to-tbox -o -f obo target/abox.obo");
				
	}
	
	
}
