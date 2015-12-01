package owltools.cli;

import org.junit.Test;

/**
 * Tests for {@link CommandRunner}.
 * 
 * these are somewhat ad-hoc at the moment - output is written to stdout;
 * no attempt made to check results
 * 
 */
public class CommandRunnerMaterializeExistentialsTest extends AbstractCommandRunnerTest {
	

	
	@Test
	public void testMaterialize() throws Exception {
		run("--create-ontology test");
		load("forebrain.obo");
		run("--materialize-existentials -p BFO:0000050");
		run("--add-imports-from-supports -o target/forebrain-partview.owl");
		
		
	}

	@Test
	public void testRemoveRedundant() throws Exception {
		run("--create-ontology test2");
		load("forebrain-redundant.obo");
		run("--merge-support-ontologies --reasoner elk --remove-redundant-svfs");
		run("-o -f obo target/forebrain-nr.obo");
		
		
	}
	
	@Test
	public void testRemoveRedundantInferred() throws Exception {
		run("--create-ontology test2");
		load("forebrain-redundant.obo");
		run("--merge-support-ontologies --reasoner mexr --remove-redundant-inferred-svfs");
		run("-o -f obo target/forebrain-nr-inf.obo");
		
		
	}
	
	@Test
	public void testRemoveRedundantInferred2() throws Exception {
		//run("--create-ontology test2");
		load("existential-redundancy-test.obo");
		run("--reasoner mexr --remove-redundant-inferred-svfs");
		run("-o -f obo target/er-nr-inf.obo");
		
		
	}
	


	
	
}
