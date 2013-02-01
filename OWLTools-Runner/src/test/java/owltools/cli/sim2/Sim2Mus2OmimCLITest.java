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
public class Sim2Mus2OmimCLITest extends AbstractCommandRunnerTest {
	
	protected void init() {
		runner = new Sim2CommandRunner();
	}

	@Test
	public void testMus2Omim() throws Exception {
		init();
		load("mp.obo");
		load("hp.obo");
		load("mp_hp-align-equiv.obo");
		run("--merge-support-ontologies");
		run("--load-instances "+path("mgi-g2p-100.txt"));
		run("--load-instances "+path("omim-d2p-100.txt"));
		//load("Mus_musculus-label.owl");
		//load("Mus_musculus-label.obo");
		run("--sim-basic -p "+path("test-sim.properties") + " --set compare MGI,MIM -o target/test100.out");
		//run("--sim-basic");
		
		//run("-o -f obo /tmp/foo.obo");
		
	}

	
	public String path(String in) {
		return getResource(in).getAbsolutePath();
	}
	
}
