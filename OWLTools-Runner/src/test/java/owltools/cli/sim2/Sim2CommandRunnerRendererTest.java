package owltools.cli.sim2;

import static org.junit.Assert.*;

import org.junit.Ignore;
import org.junit.Test;


import owltools.cli.AbstractCommandRunnerTest;
import owltools.cli.CommandRunner;
import owltools.cli.Sim2CommandRunner;

public class Sim2CommandRunnerRendererTest extends AbstractCommandRunnerTest {
	
	@Override
	protected CommandRunner createCommandRunner() {
		return new Sim2CommandRunner();
	}

	@Test
	public void testSimRunnerMouse100TXT() throws Exception {
		load("mp.obo");
		run("--load-instances "+path("mgi-g2p-100.txt"));
		run("--load-labels "+path("mgi-labels.txt"));
		run("--sim-basic -p "+path("test-sim-txt.properties") + " -o target/testRendererTXT.out");
	}

	@Test
	public void testSimRunnerMouse100ROW() throws Exception {
		load("mp.obo");
		run("--load-instances "+path("mgi-g2p-100.txt"));
		run("--load-labels "+path("mgi-labels.txt"));
		run("--sim-basic -p "+path("test-sim-row.properties") + " -o target/testRendererROW.out");
	}

	@Test
	public void testSimRunnerMouse100JSON() throws Exception {
		load("mp.obo");
		run("--load-instances "+path("mgi-g2p-100.txt"));
		run("--load-labels "+path("mgi-labels.txt"));
		run("--sim-basic -p "+path("test-sim-json.properties") + " -o target/testRendererJSON.out");
	}

	
	public String path(String in) {
		return getResource(in).getAbsolutePath();
	}
	
}
