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
public class AttributeAllByAllCommandRunnerTest extends AbstractCommandRunnerTest {
	
	protected void init() {
		runner = new Sim2CommandRunner();
	}

	
	
	/**
	 * This example uses an ontology that has grouping classes like "pedal digit bone".
	 * 
	 * It should return sensible results even without a view
	 * 
	 * We expect g2 vs g3 at UBERON:0001449 'phalanx of pes' UBERON:0004248 'pedal digit bone'   
	 * @throws Exception
	 */
	@Test
	public void testCompareAtts() throws Exception {
		init();
		load("autopod.obo");
		//run("--load-instances "+path("autopod-data-test.txt"));
		run("--make-default-abox");
		run("--fsim-compare-atts -p "+path("test-sim.properties") +
				" --set minimumAsymSimJ 0.5 --set compare UBERON,UBERON" +
				" --set outputFormat TXT" +
				" -o target/autopod-att-matrix.out");		
	}

	@Test
	public void testCompareAttsFormatted() throws Exception {
		init();
		load("autopod.obo");
		//run("--load-instances "+path("autopod-data-test.txt"));
		run("--make-default-abox");
		run("--fsim-compare-atts -p "+path("test-sim.properties") +
				" --set minimumAsymSimJ 0.99 --set compare UBERON,UBERON" +
				" --set outputFormat Formatted" +
				" -o target/autopod-att-matrix.rpt");		
	}

	@Test
	public void testCompareAttsSaveInOWL() throws Exception {
		init();
		load("autopod.obo");
		//run("--load-instances "+path("autopod-data-test.txt"));
		run("--make-default-abox");
		run("--fsim-compare-atts -p "+path("test-sim.properties") +
				" --set outputFormat OWL" +
				" --set bestOnly true" +
				" --set minimumAsymSimJ 0.99 --set compare UBERON,UBERON" +
				" -o target/autopod-att-matrix.owl");		
	}


	
	public String path(String in) {
		return getResource(in).getAbsolutePath();
	}
	
}
