package owltools.cli;

import org.junit.Test;

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
	
	@Test
	public void testQuery() throws Exception {
		init();
		load("gaf/mgi-exttest-go-subset.obo");
		String gafpath = getResource("gaf/mgi-exttest.gaf").getAbsolutePath();
		run("--gaf "+gafpath);
		
		run("--gaf-query GO:0008152");
		String opath = "target/gafq-output.gaf";
		run("--write-gaf "+opath);
		
		
	}


	@Test
	public void testMap2Slim() throws Exception {
		init();
		load("gaf/mgi-exttest-go-subset.obo");
		String gafpath = getResource("gaf/mgi-exttest.gaf").getAbsolutePath();
		run("--gaf "+gafpath);
		
		run("--map2slim -s goslim_plant");
		String opath = "target/map2slim-output.gaf";
		run("--write-gaf "+opath);
		
		
	}
	
	@Test
	public void testMap2SlimIds() throws Exception {
		init();
		load("gaf/mgi-exttest-go-subset.obo");
		String gafpath = getResource("gaf/mgi-exttest.gaf").getAbsolutePath();
		run("--gaf "+gafpath);
		
		String slimpath = getResource("gaf/goslim_plant.terms").getAbsolutePath();
		run("--map2slim --idfile "+slimpath);
		String opath = "target/map2slim-output-ids.gaf";
		run("--write-gaf "+opath);
		
		
	}
	
	@Test
	public void testAddLabels() throws Exception {
		init();
		load("gaf/mgi-exttest-go-subset.obo");
		String gafpath = getResource("gaf/mgi-exttest.gaf").getAbsolutePath();
		run("--add-labels -c 1,5 "+gafpath);

		
	}


	
	
}
