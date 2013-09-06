package owltools.cli;

import org.junit.Test;

/**
 * Tests for {@link CommandRunner}.
 * 
 * these are somewhat ad-hoc at the moment - output is written to stdout;
 * no attempt made to check results
 * 
 */
public class CommandRunnerTest extends AbstractCommandRunnerTest {
	
	@Test
	public void testTableRenderer() throws Exception {
		load("ceph.obo");
		run("--export-table target/ceph.tbl");
		//run("--extract-axioms -t EquivalentClasses -o -f obo target/foo.obo");
		run("--extract-mingraph --idspace CEPH -o -f obo target/foo.obo");
				
	}
	
	@Test
	public void testAxiomProcessing() throws Exception {
		load("ceph.obo");
		run("--remove-axiom-annotations -o -f obo target/ceph-p1.obo");
		run("--remove-annotation-assertions -l -o -f obo target/ceph-p2.obo");
				
	}
}
