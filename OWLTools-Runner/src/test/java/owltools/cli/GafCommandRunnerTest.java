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
public class GafCommandRunnerTest extends AbstractCommandRunnerTest {
	
	protected void init() {
		runner = new GafCommandRunner();
	}

	@Test
	public void testRunner() throws Exception {
		init();
		load("go_sample_mf_subset.obo");
		String gafpath = getResource("test_gene_association_mgi.gaf").getAbsolutePath();
		run("--gaf "+gafpath);
		
		run("--extract-ontology-subset-by-gaf -u http://x.org/foo -o -f obo /tmp/foo.obo");
		
		//run("-o -f obo /tmp/foo.obo");
		
	}
	
	@Test
	public void testFolder() throws Exception {
		init();
		load("go_sample_mf_subset.obo");
		String gafpath = getResource("test_gene_association_mgi.gaf").getAbsolutePath();
		run("--gaf "+gafpath);
		
		run("--gaf-fold-extensions");
		String opath = "target/tmp.gaf";
		run("--write-gaf "+opath);
		run("-o -f obo target/foo.obo");
		run("-o file:///tmp//foo.owl");
		
		//run("-o -f obo /tmp/foo.obo");
		
	}

	
	
}
