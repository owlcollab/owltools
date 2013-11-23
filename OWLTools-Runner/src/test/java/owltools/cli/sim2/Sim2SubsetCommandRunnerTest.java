package owltools.cli.sim2;

import org.junit.Test;

import owltools.cli.AbstractCommandRunnerTest;
import owltools.cli.CommandRunner;
import owltools.cli.Sim2CommandRunner;

/**
 * Tests for {@link CommandRunner}.
 * 
 * these are somewhat ad-hoc at the moment - output is written to stdout;
 * no attempt made to check results
 * 
 */
public class Sim2SubsetCommandRunnerTest extends AbstractCommandRunnerTest {
	
	protected void init() {
		runner = new Sim2CommandRunner();
	}

	
	@Test
	public void testSimRunnerSubset() throws Exception {
		init();
		load("mp.obo");
		run("--reasoner-query MP_0003631");
		run("--make-ontology-from-results http://x.org");
		run("--load-instances "+path("mgi-g2p-100.txt"));
		run("--remove-dangling-annotations");
		run("--load-labels "+path("mgi-labels.txt"));
		//load("Mus_musculus-label.owl");
		//load("Mus_musculus-label.obo");
		run("--fsim-basic -p "+path("test-sim.properties") + " -o target/test100.out");
		//run("--sim-basic");
		
		//run("-o -f obo /tmp/foo.obo");
		
	}

	
	
}
