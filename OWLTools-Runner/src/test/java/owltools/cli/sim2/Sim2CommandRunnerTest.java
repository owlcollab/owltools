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
public class Sim2CommandRunnerTest extends AbstractCommandRunnerTest {
	
	protected void init() {
		runner = new Sim2CommandRunner();
	}

	
	@Test
	public void testSimRunnerMouse100() throws Exception {
		init();
		load("mp.obo");
		run("--load-instances "+path("mgi-g2p-100.txt"));
		run("--load-labels "+path("mgi-labels.txt"));
		//load("Mus_musculus-label.owl");
		//load("Mus_musculus-label.obo");
		run("--sim-compare-atts -p "+path("test-sim.properties"));
		run("--sim-basic -p "+path("test-sim.properties") + " -o test100.out");
		//run("--sim-basic");
		
		//run("-o -f obo /tmp/foo.obo");
		
	}

	
	public String path(String in) {
		return getResource(in).getAbsolutePath();
	}
	
}
