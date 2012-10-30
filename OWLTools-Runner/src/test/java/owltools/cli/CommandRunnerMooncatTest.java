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
	public void testMakePropSubset() throws Exception {
		init();
		load("ceph.obo");
		run("-a tentacle");
		
		run("--make-subset-by-properties part_of // -o -f obo /tmp/foo.obo");
		
		//run("-o -f obo /tmp/foo.obo");
		
	}
	
	@Test
	public void testMakeSubOnt() throws Exception {
		init();
		load("ceph.obo");
		String[] qargs = {"--reasoner-query", "BFO_0000050 some CEPH_0000256"};
		run("--make-subset-by-properties part_of");
		run(qargs);
		
		run("--make-ontology-from-results -f http://purl.obolibrary.org/obo/foo.owl");
		
		run("-o -f obo /tmp/foo.obo");
		
	}

	@Test
	public void testMakeSubOnt2() throws Exception {
		init();
		load("ceph.obo");
		String[] qargs = {"--reasoner-query", "BFO_0000050 some CEPH_0000256"};
		run("--make-subset-by-properties part_of");
		run(qargs);
		
		run("--make-ontology-from-results -m http://purl.obolibrary.org/obo/foo.owl");
		
		run("-o -f obo /tmp/foo2.obo");
		
	}

	
	
}
