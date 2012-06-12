package owltools.cli;

import static junit.framework.Assert.*;

import java.io.File;

import org.junit.Test;

import owltools.OWLToolsTestBasics;

/**
 * Tests for {@link CommandRunner}.
 * 
 * these are somewhat ad-hoc at the moment - output is written to stdout;
 * no attempt made to check results
 * 
 */
public class CommandRunnerTest extends OWLToolsTestBasics {
	
	CommandRunner runner;
	
	@Test
	public void testRunner() throws Exception {
		runner = new CommandRunner();
		String path = getResource("ceph.obo").getAbsolutePath();
		System.out.println("Loading: "+path);
		run(path);
		run("-a tentacle");
		run("--reasoner-query -r elk -l tentacle");
		String[] args = {
				"--reasoner-query",
				"-r",
				"elk",
				"-d",
				"BFO_0000050 some CEPH_0000256"
		};
		run(args);
		run("--make-ontology-from-results http://x.org -o -f obo /tmp/foo.obo");
		
		run("--incoming-edges tentacle");
	}
	
	private void run(String[] args) throws Exception {
		runner.run(args);
	}
	private void run(String argStr) throws Exception {
		run(argStr.split(" "));
	}
	
	private void run(String[] args, String[] expectedLines) {
		// TODO
	}
	
	
	
}
