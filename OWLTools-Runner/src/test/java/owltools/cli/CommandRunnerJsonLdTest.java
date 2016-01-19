package owltools.cli;

import org.junit.Test;

/**
 * Tests for {@link CommandRunner}.
 * 
 * these are somewhat ad-hoc at the moment - output is written to stdout;
 * no attempt made to check results
 * 
 */
public class CommandRunnerJsonLdTest extends AbstractCommandRunnerTest {
	
	@Test
	public void testParse() throws Exception {
		load("ceph.obo");
		run("-o target/ceph.owl");
		run("--rdf-to-json-ld -o target/ceph.json target/ceph.owl");
		run("--json-ld-to-rdf -o target/ceph.rdf target/ceph.json");
				
	}
	

}
