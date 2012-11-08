package owltools.cli.sim2;

import static junit.framework.Assert.*;

import java.io.File;

import org.eclipse.jetty.util.log.Log;
import org.junit.Ignore;
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
public class MapAboxTest extends AbstractCommandRunnerTest {
	
	protected void init() {
		runner = new Sim2CommandRunner();
	}

	@Ignore
	@Test
	public void testSimRunnerSubset() throws Exception {
		init();
		load("mgi-test-ns.owl");
		//run("--reasoner elk");
		run("--init-reasoner -r elk");
		System.out.println("reasonerX = "+runner.reasoner);
		//run("--reasoner-query -r elk MP_0002064");
		run("--reasoner-query MP_0002064");
		System.out.println("reasonerY = "+runner.reasoner);
		//run("-a MP:0002064");
		//run("--map-abox-to-namespace http://purl.obolibrary.org/obo/HP_");
		run("--reasoner-dispose");
		
	}
	
	
}
