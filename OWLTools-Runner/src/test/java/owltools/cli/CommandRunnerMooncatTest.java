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
public class CommandRunnerMooncatTest extends AbstractCommandRunnerTest {
	
	@Test
	public void testRunner() throws Exception {
		init();
		load("ceph.obo");
		run("-a tentacle");
		
		run("--make-subset-by-properties part_of // -o -f obo /tmp/foo.obo");
		
		//run("-o -f obo /tmp/foo.obo");
		
	}
	
	
}
