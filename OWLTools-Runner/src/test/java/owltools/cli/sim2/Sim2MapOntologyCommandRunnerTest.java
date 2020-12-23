package owltools.cli.sim2;

import org.junit.Test;

import owltools.cli.AbstractCommandRunnerTest;
import owltools.cli.Sim2CommandRunner;

/**
 * Tests a pipeline in which a MP and HP annotations are loaded, a subset is made for the nervous system,
 * and all annotations are mapped to HP
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
		run("--reasoner-query -r hermit HP_0000707"); // abnormality of nervous system
		run("--reasoner-dispose");
		run("--make-ontology-from-results http://x.org");
		run("--load-instances "+path("mgi-g2p-1000.txt"));
		run("--remove-dangling-annotations");
		run("-o target/ns-test.owl");
		run("--reasoner hermit");
		run("--map-abox-to-namespace http://purl.obolibrary.org/obo/HP_");
		run("--fsim-basic -p "+path("test-sim.properties") + " -o target/test100.out");
		run("-o -f obo /tmp/foo.obo");
		
	}

	
	
}
