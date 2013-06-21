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
public class CommandRunnerMergeEquivTest extends AbstractCommandRunnerTest {
	
	@Test
	public void testMMergeEquivalent() throws Exception {
		init();
		String obo = "http://purl.obolibrary.org/obo";
		load("merge-equiv-test.obo");
		//run("--list-class-axioms NCBITaxon:7954");
		
		//run("--reasoner elk --merge-equivalent-classes -f MA -f FMA -t U");
		run("--merge-equivalent-classes -f MA -f FMA -t U");
		run("--reasoner elk");
		run("--merge-equivalent-classes -f HP -t MP");
		run("-o -f obo --no-check target/equiv-merged.obo");

	}
	


	
	
}
