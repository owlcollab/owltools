package owltools.cli;

import org.junit.Test;

/**
 * Tests for {@link CommandRunner}.
 * 
 * these are somewhat ad-hoc at the moment - output is written to stdout;
 * no attempt made to check results
 * 
 */
public class CommandRunnerDisjointTest extends AbstractCommandRunnerTest {
	
	@Test
	public void testQueryCE() throws Exception {
		load("forebrain-disjoint-violations.obo");
		run("--no-debug --expand-macros --reasoner welk --check-disjointness-axioms");
	}
	
}
