package owltools.cli;

import org.junit.Test;

/**
 * Tests for {@link CommandRunner}.
 * 
 * these are somewhat ad-hoc at the moment - output is written to stdout;
 * no attempt made to check results
 * 
 */
public class CommandRunnerOntologyTransformationsTest extends AbstractCommandRunnerTest {
	

	@Test
	public void testExpand() throws Exception {
		init();
		load("ceph.obo");
		String[] args = {
			"--expand-expression",
			"BFO_0000050",
			"BFO_0000051 some ?Y"
		};
		run(args);
		run("-o -f obo target/expand.obo");
	}

}
