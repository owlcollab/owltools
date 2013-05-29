package owltools.cli;

import org.junit.Test;

/**
 * Tests for {@link CommandRunner}.
 * 
 * these are somewhat ad-hoc at the moment - output is written to stdout;
 * no attempt made to check results
 * 
 */
public class CommandRunnerReasonerQueryTest extends AbstractCommandRunnerTest {
	

	@Test
	public void testQueryCE() throws Exception {
		init();
		load("ceph.obo");
		String[] qargs = {"--reasoner-query", "BFO_0000050 some CEPH_0000256"};
		run(qargs);
		run("--make-ontology-from-results http://x.org");
		run("--export-table /tmp/part-of-tentacle.tbl");
	}
	
	@Test
	public void testQueryCE2() throws Exception {
		init();
		load("ceph.obo");
		String[] qargs = {"--reasoner-query", "UBERON_0000062 and BFO_0000050 some UBERON_0001032"};
		run(qargs);
				
	}


	@Test
	public void testQueryNC() throws Exception {
		init();
		load("ceph.obo");
		String[] qargs = {"--reasoner-query", "CEPH_0000256"};
		run(qargs);
				
	}

	
}
