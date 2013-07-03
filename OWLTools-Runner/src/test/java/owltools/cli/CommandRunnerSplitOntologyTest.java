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
public class CommandRunnerSplitOntologyTest extends AbstractCommandRunnerTest {
	

	
	@Test
	public void testSplitOntology() throws Exception {
		init();
		load("forebrain.obo");
		run("--split-ontology --idspaces ncbitaxon -d target -o target/forebrain-min.owl");
		
		
	}
	
	@Test
	public void testSplitOntology2() throws Exception {
		init();
		load("forebrain.obo");
		run("--split-ontology -p http://x.org/foo/ -s -imports.owl --idspaces ncbitaxon --idspaces ncbitaxon uberon -d target -o target/forebrain-min2.owl");
		
		
	}

	


	
	
}
