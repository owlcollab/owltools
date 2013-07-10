package owltools.cli;

import org.junit.Test;

/**
 * Tests for {@link CommandRunner}.
 * 
 * these are somewhat ad-hoc at the moment - output is written to stdout;
 * no attempt made to check results
 * 
 */
public class CommandRunnerSpeciesMergeTest extends AbstractCommandRunnerTest {
	
	@Test
	public void testMergeSpecies() throws Exception {
		//String obo = "http://purl.obolibrary.org/obo";
		load("forebrain.obo");
		//run("--list-class-axioms NCBITaxon:7954");
		
		run("--reasoner elk --merge-species-ontology -s Danio -t NCBITaxon:7954");
		run("-o -f obo target/forebrain-merged.obo");
		run("--assert-inferred-subclass-axioms --removeRedundant --allowEquivalencies");
		run("-o -f obo target/forebrain-merged-trimmed.obo");
		run("--descendants telencephalon");
		
	}
	

}
