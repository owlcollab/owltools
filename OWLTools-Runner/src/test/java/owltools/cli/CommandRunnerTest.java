package owltools.cli;

import static junit.framework.Assert.*;

import org.junit.Test;

import owltools.OWLToolsTestBasics;

/**
 * Tests for {@link CommandRunner}.
 */
public class CommandRunnerTest extends OWLToolsTestBasics {
	
	CommandRunner runner;
	
	@Test
	public void testRunner() throws Exception {
		runner = new CommandRunner();
		boolean ok = false;
		String path = getResource("caro.obo").getAbsolutePath();
		System.out.println("Loading: "+path);
		run(path);
		assertTrue(ok);
	}
	
	private void run(String[] args) throws Exception {
		runner.run(args);
	}
	private void run(String argStr) throws Exception {
		runner.run(argStr.split(" "));
	}
	
}
