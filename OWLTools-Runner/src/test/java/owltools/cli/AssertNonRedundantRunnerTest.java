package owltools.cli;

import org.junit.Test;

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
		load("assert-inferences-test.owl");
		run("--reasoner-ask-all -r elk -a --remove-indirect INDIVIDUALS");
	}
	
	
}
