package owltools.cli;

import owltools.OWLToolsTestBasics;

/**
 * Tests for {@link CommandRunner}.
 * 
 * these are somewhat ad-hoc at the moment - output is written to stdout;
 * no attempt made to check results
 * 
 */
public class AbstractCommandRunnerTest extends OWLToolsTestBasics {
	
	protected CommandRunner runner;
	
	protected void init() {
		runner = new CommandRunner();
	}
	
	protected void load(String file) throws Exception {
		String path = getResource(file).getAbsolutePath();
		run(path);		
	}
	
	protected void run(String[] args) throws Exception {
		runner.runSingleIteration(args);
	}
	protected void run(String argStr) throws Exception {
		run(argStr.split(" "));
	}
	
	protected void run(String[] args, String[] expectedLines) {
		// TODO
	}
	
	public String path(String in) {
		return getResource(in).getAbsolutePath();
	}

	
}
