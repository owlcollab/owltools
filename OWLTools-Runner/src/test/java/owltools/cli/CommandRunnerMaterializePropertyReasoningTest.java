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
public class CommandRunnerMaterializePropertyReasoningTest extends AbstractCommandRunnerTest {
	

	@Test
	public void testQueryCE() throws Exception {
		init();
		load("ceph.obo");
		run("--reasoner elk");
		run("--materialize-property-inferences -p BFO_0000050");
		run("-o -f turtle file:///tmp/z.ttl");
	}
}
