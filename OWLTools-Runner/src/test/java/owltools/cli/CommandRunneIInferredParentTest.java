package owltools.cli;

import org.junit.Test;

/**
 * Tests for {@link CommandRunner}.
 * 
 * these are somewhat ad-hoc at the moment - output is written to stdout;
 * no attempt made to check results
 * 
 */
public class CommandRunneIInferredParentTest extends AbstractCommandRunnerTest {
	
	@Test
	public void testExportParents() throws Exception {
		//String obo = "http://purl.obolibrary.org/obo";
		load("forebrain2.owl");
		
		run("--reasoner mexr --export-parents -p BFO:0000050 -o target/ip.out --merge-support-ontologies -o target/ip.owl");
	}
	
	@Test
	public void testExportParentsWithTaxa() throws Exception {
		//String obo = "http://purl.obolibrary.org/obo";
		load("forebrain2.owl");
		
		run("--reasoner mexr --log-error --export-parents -p BFO:0000050 BFO:0000051 -gp BFO:0000050 -gf NCBITaxon:7954 NCBITaxon:9606 NCBITaxon:7742 NCBITaxon:6040 -o target/ipt.out");
	}


}
