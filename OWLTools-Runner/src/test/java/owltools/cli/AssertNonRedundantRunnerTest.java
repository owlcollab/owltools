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
public class AssertNonRedundantRunnerTest extends AbstractCommandRunnerTest {
	
	@Test
	public void testRunner() throws Exception {
		init();
		load("assert-inferences-test.owl");
		run("--reasoner-ask-all -r elk -a --remove-indirect INDIVIDUALS");
	}
	
	
}
