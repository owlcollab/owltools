package owltools.cli.sim2;

import static junit.framework.Assert.*;

import java.io.File;

import org.junit.Ignore;
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
public class PhenoSimCLITest extends AbstractCommandRunnerTest {
	
	protected void init() {
		runner = new Sim2CommandRunner();
	}

	// TODO: find a smaller example
	
	@Ignore
	@Test
	public void testSimRunner() throws Exception {
		init();
		load("mp-ns-xp.owl");
		run("--load-instances "+path("mgi-g2p-100.txt"));
		run("--phenosim -p "+path("test-sim.properties")+" -o phenosim.out");
		
		//run("-o -f obo /tmp/foo.obo");
		
	}
	
	
}
