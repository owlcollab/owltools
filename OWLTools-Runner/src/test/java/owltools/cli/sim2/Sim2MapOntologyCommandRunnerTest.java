package owltools.cli.sim2;

import static junit.framework.Assert.*;

import java.io.File;

import org.junit.Test;

import owltools.OWLToolsTestBasics;
import owltools.cli.AbstractCommandRunnerTest;
import owltools.cli.Sim2CommandRunner;

/**
 * Tests for {@link CommandRunner}.
 * 
 * these are somewhat ad-hoc at the moment - output is written to stdout;
 * no attempt made to check results
 * 
 */
public class Sim2MapOntologyCommandRunnerTest extends AbstractCommandRunnerTest {
	
	protected void init() {
		runner = new Sim2CommandRunner();
	}

	
	@Test
	public void testSimRunnerSubset() throws Exception {
		init();
		load("mp.obo");
		load("hp.obo");
		load("mp_hp-align-equiv.obo");
		run("--merge-support-ontologies");
		run("--reasoner-query -r elk HP_0000707"); // abnormality of nervous system
		run("--reasoner-dispose");
		run("--make-ontology-from-results http://x.org");
		run("--load-instances "+path("mgi-g2p-1000.txt"));
		run("--remove-dangling-annotations");
		run("-o file:///tmp/foo.owl");
		run("--reasoner elk");
		run("--map-abox-to-namespace http://purl.obolibrary.org/obo/HP_");
		run("--sim-basic -p "+path("test-sim.properties") + " -o test100.out");
		run("-o -f obo /tmp/foo.obo");
		
	}

	
	
}
