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
public class Sim2AnatomyCommandRunnerTest extends AbstractCommandRunnerTest {
	
	protected void init() {
		runner = new Sim2CommandRunner();
	}

	
	/**
	 * We expect this to be empty. 
	 * 
	 * Reason: autopod-parts.obo is a pure partonomy. There are no common superclasses, because
	 * there are no superclasses. It's necessary to create a view
	 * 
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
	 * This example uses an ontology that has grouping classes like "pedal digit bone".
	 * 
	 * It shoukd return sensible results even without a view
	 * 
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

	/**
	 * In this example we use an ontology that is pure partonomy
	 * (e.g. digit part_of hand part_of limb)
	 * 
	 * We create a view using (reflexive) partOf, giving us
	 *  digit SubClassOf digit-part
	 *  digit-part SubClassOf  autopod-part
	 *  autopod-part SubClassOf limb-part
	 *
	 * These should then be returned as common superclasses
	 * 
	 * @throws Exception
	 */
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
