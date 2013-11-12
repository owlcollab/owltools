package owltools.cli.analysis;

import org.junit.Test;

import owltools.cli.AbstractCommandRunnerTest;
import owltools.cli.AnalysisCommandRunner;
import owltools.cli.CommandRunner;

/**
 * Tests for methods to generate simulated datasets in {@link AnalysisCommandRunner}.
 * Since the methods simply print data, the results are just written to 
 * stdout.
 * no attempt made to check results
 * 
 */
public class SimulatedDataCommandRunnerTest extends AbstractCommandRunnerTest {
	
	//protected void init() {
	//	runner = new AnalysisCommandRunner();
	//}

	@Override
	protected CommandRunner createCommandRunner() {
		return new AnalysisCommandRunner();
	}
	
	/**
	 * This test will create a simulated annotation file, using the
	 * leave-one-out combinatorics.
	 * @throws Exception
	 */
	@Test
	public void testCreateSimulatedReductions() throws Exception {
		//init();
		//this test has just 2:(3,6) entity:class annotation set

		load("hp.obo");
		run("--load-instances "+path("omim-d2p-1.txt"));
		run("--generate-simulated-reduction-data -o target/simulated_reduction.txt");
	}

	
	public String path(String in) {
		return getResource(in).getAbsolutePath();
	}
	
}
