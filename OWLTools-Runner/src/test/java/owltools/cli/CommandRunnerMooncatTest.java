package owltools.cli;

import java.io.File;

import org.eclipse.jetty.util.log.Log;
import org.junit.Test;

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
		load("ceph.obo");
		run("-a tentacle");
		run("--remove-dangling");
		run("-o -f obo target/ceph-rd.obo");
		
		run("--make-subset-by-properties part_of // -o -f obo target/ceph-part-of-slim.obo");
		
		//run("-o -f obo /tmp/foo.obo");
		
	}
	
	@Test
	public void testMakeSubOnt() throws Exception {
		load("ceph.obo");
		String[] qargs = {"--reasoner-query", "BFO_0000050 some CEPH_0000256"};
		run("--make-subset-by-properties -f part_of");
		run(qargs);
		
		run("--make-ontology-from-results -f http://purl.obolibrary.org/obo/foo.owl");
		
		run("-o -f obo target/ceph-tentacle.obo");
		
	}

	@Test
	public void testMakeSuperSlim() throws Exception {
		load("super-slim-test.obo");
		run("--make-super-slim X,Y -o -f obo target/super-slim.obo");	
	}


	@Test
	public void testExtractModule() throws Exception {
		load("ceph.obo");
		run("--extract-module -n test tentacle");
		run("-o -f obo target/tentacle-module.obo");
		
	}

	@Test
	public void testRemoveExternal() throws Exception {
		load("ceph.obo");
		run("--remove-external-classes CEPH");
		run("-o -f obo target/ceph-basic.obo");
		
	}

	@Test
	public void testExtractOntologySubset() throws Exception {
		load("ceph.obo");
		File subsetIdFile = getResource("ceph-subset.txt");
		System.out.println(subsetIdFile);
		run("--extract-ontology-subset --fill-gaps -i "+subsetIdFile+" -o -f obo target/ceph-subset-filled.obo");
		// we expect the resulting file to have all paths to root, including classes not in subset: e.g. appendage, head
		
		run("--extract-ontology-subset -i "+subsetIdFile+" -o -f obo target/ceph-subset-gapped.obo");
		// we expect the resulting file to have gaps between classes be spanned - i.e. tentacular club connects directly to tentacle
		
	}
}
