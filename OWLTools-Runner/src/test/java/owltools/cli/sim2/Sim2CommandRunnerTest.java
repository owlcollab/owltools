package owltools.cli.sim2;

import org.junit.Ignore;
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
public class Sim2CommandRunnerTest extends AbstractCommandRunnerTest {
	
	@Override
	protected CommandRunner createCommandRunner() {
		return new Sim2CommandRunner();
	}

	@Test
	public void testSimRunnerMouse100() throws Exception {
		load("mp.obo");
		run("--load-instances "+path("mgi-g2p-100.txt"));
		run("--load-labels "+path("mgi-labels.txt"));
		//run("--sim-compare-atts -p "+path("test-sim.properties"));
		run("--sim-compare-atts");
		run("--sim-basic -p "+path("test-sim.properties") + " -o target/test100.out");

	}

	@Test
	public void testSimRunnerMouse100WithCache() throws Exception {
		load("mp.obo");
		run("--load-instances "+path("mgi-g2p-100.txt"));
		run("--load-labels "+path("mgi-labels.txt"));
		//load("Mus_musculus-label.owl");
		//load("Mus_musculus-label.obo");
		run("--sim-compare-atts -p "+path("test-sim.properties"));
		run("--sim-save-lcs-cache -m 2.0 target/lcs-cache"); // there is currently no method with this name?
		run("--sim-save-ic-cache target/ic-cache.ttl");

		//create a variety of sim property files and test here
		//this one tests that it runs with the default properties
		run("--sim-basic -p "+path("test-sim.properties") + " -o target/test100.out");

		// test with a fresh instance
		runner = createCommandRunner();
		runner.exitOnException = false;
		load("mp.obo");
		run("--load-instances "+path("mgi-g2p-100.txt"));
		run("--load-labels "+path("mgi-labels.txt"));
		
		//run("-o -f obo /tmp/foo.obo");	
		run("--sim-load-lcs-cache target/lcs-cache");
		run("--sim-load-ic-cache target/ic-cache.ttl");
		//TODO
		run("--sim-basic -p "+path("test-sim.properties") + " -o target/test100run2.out");


	}
	
	@Test
	public void testSimRunnerDefaultPropertiesFile() throws Exception {
    //will load the default properties file
		load("mp.obo");
		run("--load-instances "+path("mgi-g2p-100.txt"));
		run("--load-labels "+path("mgi-labels.txt"));
		run("--sim-basic -p "+path("default-sim.properties")+ " -o target/test100.default-sim.out");	
		run("--show-sim-properties");
//		run("--sim-basic -o target/test100.default.out");		
	}
	
	@Test
	public void testSimRunnerDefaultProperties() throws Exception {
    //will load the default properties file
		load("mp.obo");
		run("--load-instances "+path("mgi-g2p-100.txt"));
		run("--load-labels "+path("mgi-labels.txt"));
		run("--sim-basic -o target/test100.default.out");		
		run("--show-sim-properties");
	}	
	
	@Test
	public void testPrintReportsForAnnotations() throws Exception {
		load("mp.obo");
		run("--load-instances "+path("mgi-g2p-100.txt"));
		run("--load-labels "+path("mgi-labels.txt"));
		run("--show-instance-IC-values -o target/testICVals.out");
		run("--show-instance-stats -o target/testStats.out");
		run("--annotate-attr-groupings-as-table -gc MP:0010769,MP:0001919,MP:0002177,MP:0000003 -o target/groupingClassesTable.out");
		run("--annotate-attr-groupings-as-list -gc MP:0010769,MP:0001919,MP:0002177,MP:0000003 -o target/groupingClassesList.out");
		}


	@Ignore("There is no implementation for option ' --class-IC-pairs', renamed, missing commit?")
	@Test
	public void testClassICPairs() throws Exception {
		load("mp.obo");
		run("--load-instances "+path("mgi-g2p-100.txt"));
		run("--load-labels "+path("mgi-labels.txt"));
		run("--no-debug --class-IC-pairs");
	}

	@Ignore("There is no implementation for option ' --sim-lcs', renamed, missing commit?")
	@Test
	public void testSimNamedLCS() throws Exception {
		load("mp.obo");
		run("--sim-lcs MP:0005296 syndactyly");
	}

	
	public String path(String in) {
		return getResource(in).getAbsolutePath();
	}
	
}
