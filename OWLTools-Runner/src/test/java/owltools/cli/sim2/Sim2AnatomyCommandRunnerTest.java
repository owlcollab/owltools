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
public class Sim2AnatomyCommandRunnerTest extends AbstractCommandRunnerTest {
	
	protected void init() {
		runner = new Sim2CommandRunner();
	}

	
	/**
	 * We expect this to be empty
	 * @throws Exception
	 */
	@Test
	public void testSimRunnerAnatNoSubClass() throws Exception {
		init();
		load("autopod-parts.obo");
		run("--load-instances "+path("autopod-data-test.txt"));
		//run("--sim-compare-atts -p "+path("test-sim.properties"));
		run("--sim-basic -p "+path("test-sim.properties") + " -o target/autopod-test1.out");
		//run("--sim-basic");
		
		//run("-o -f obo /tmp/foo.obo");
		
	}
	
	/**
	 * We expect g2 vs g3 at UBERON:0001449 'phalanx of pes' UBERON:0004248 'pedal digit bone'   
	 * @throws Exception
	 */
	@Test
	public void testSimRunnerAnatWithSubClass() throws Exception {
		init();
		load("autopod.obo");
		run("--load-instances "+path("autopod-data-test.txt"));
		//run("--sim-compare-atts -p "+path("test-sim.properties"));
		run("--sim-basic -p "+path("test-sim.properties") + " -o target/autopod-test2.out");
		//run("--sim-basic");
		
		//run("-o -f obo /tmp/foo.obo");
		
	}

	@Test
	public void testSimRunnerAnatWithView() throws Exception {
		init();
		load("autopod-parts.obo");
		run("--set-sim-property analysisRelation BFO:0000050");
		run("--load-instances -p BFO:0000050 "+path("autopod-data-test.txt"));
		//run("--sim-compare-atts -p "+path("test-sim.properties"));
		run("--sim-basic -p "+path("test-sim.properties") + " -o target/autopod-test3.out");
		//run("--sim-basic");
		run("--show-sim-properties");
		run("-o file:///tmp/z.owl");
		
		//run("-o -f obo /tmp/foo.obo");
		
	}

	
	public String path(String in) {
		return getResource(in).getAbsolutePath();
	}
	
}
