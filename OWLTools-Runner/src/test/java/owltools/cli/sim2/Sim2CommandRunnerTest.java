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
		run("--use-fsim");
		//run("--fsim-compare-atts");
		run("--fsim-compare-atts");
		run("--fsim-basic -p "+path("test-sim.properties") + " -o target/test100.out");

	}

	@Test
	public void testSimRunnerFindMatches() throws Exception {
		load("mp.obo");
		run("--load-instances "+path("mgi-g2p-1000.txt"));
		run("--load-labels "+path("mgi-labels.txt"));
		run("--fsim-find-matches -p "+path("test-sim.properties") + " --set outputFormat TXT -o target/fsim-search.out -q MGI:101757 -t MGI");

	}
	
	@Test
	public void testSearchAll() throws Exception {
		load("mp.obo");
		run("--load-instances "+path("mgi-g2p-1000.txt"));
		run("--load-labels "+path("mgi-labels.txt"));
		run("--fsim-search-all -p "+path("test-sim.properties") + " --set outputFormat TXT -o target/fsim-search-all.out");
	}


	@Test
	public void testSimRunnerMouse100WithCache() throws Exception {
		load("mp.obo");
		run("--load-instances "+path("mgi-g2p-1000.txt"));
		run("--load-labels "+path("mgi-labels.txt"));
		//load("Mus_musculus-label.owl");
		//load("Mus_musculus-label.obo");
		//run("--fsim-compare-atts");
		run("--sim-save-lcs-cache -m 3.0 --debug-class MP:0000001 target/lcs-cache"); 
		run("--sim-save-ic-cache target/ic-cache.ttl");
		run("--sim-save-state target/state.txt");

		//create a variety of sim property files and test here
		//this one tests that it runs with the default properties
		run("--fsim-basic -p "+path("test-sim.properties") + " -o target/test100run1.out");

		// test with a fresh instance
		runner = createCommandRunner();
		runner.exitOnException = false;
		load("mp.obo");
		run("--load-instances "+path("mgi-g2p-1000.txt"));
		run("--load-labels "+path("mgi-labels.txt"));
		
		//run("-o -f obo /tmp/foo.obo");	
		run("--sim-load-lcs-cache target/lcs-cache");
		run("--sim-load-ic-cache target/ic-cache.ttl");
		//TODO
		run("--fsim-basic -p "+path("test-sim.properties") + " -o target/test100run2.out");


	}
	
	@Test
	public void testSimRunnerDefaultPropertiesFile() throws Exception {
    //will load the default properties file
		load("mp.obo");
		run("--load-instances "+path("mgi-g2p-100.txt"));
		run("--load-labels "+path("mgi-labels.txt"));
		run("--fsim-basic -p "+path("default-sim.properties")+ " -o target/test100.default-sim.out");	
		run("--show-sim-properties");
//		run("--sim-basic -o target/test100.default.out");		
	}
	
	@Test
	public void testSimRunnerDefaultProperties() throws Exception {
    //will load the default properties file
		load("mp.obo");
		run("--load-instances "+path("mgi-g2p-100.txt"));
		run("--load-labels "+path("mgi-labels.txt"));
		run("--fsim-basic -o target/test100.default.out");		
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
