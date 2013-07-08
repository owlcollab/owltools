package owltools.cli;

import org.junit.Test;

/**
 * Tests for {@link CommandRunner}.
 * 
 * these are somewhat ad-hoc at the moment - output is written to stdout;
 * no attempt made to check results
 * 
 */
public class CommandRunnerMaterializePropertyReasoningTest extends AbstractCommandRunnerTest {
	
	@Test
	public void testQueryCE() throws Exception {
		load("ceph.obo");
		run("--reasoner elk");
		run("--materialize-property-inferences -p BFO_0000050");
		run("-o -f turtle file:///tmp/z.ttl");
	}
	
}
