package owltools.cli;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;

import org.junit.Test;

/**
 * Tests for {@link CommandRunner}.
 * 
 * these are somewhat ad-hoc at the moment - output is written to stdout;
 * no attempt made to check results
 * 
 */
public class MockSolrLoadCommandRunnerTest extends AbstractCommandRunnerTest {
	
	protected void init() {
		runner = new SolrCommandRunner();
	}

	@Test
	public void testRunner() throws Exception {
	    
	    PrintStream out = System.out;
	    System.setOut(new PrintStream(new BufferedOutputStream(new FileOutputStream("target/gafdocs.json"))));
	    
		init();
		load("go_sample_mf_subset.obo");
		run("--reasoner elk");
        run("--solr-url mock");
		String gafpath = getResource("test_gene_association_mgi.gaf").getAbsolutePath();
		run("--solr-load-gafs "+gafpath);
		System.setOut(out);
	}
	
	@Test
	public void testRunnerOboIds() throws Exception {

	    PrintStream out = System.out;
	    System.setOut(new PrintStream(new BufferedOutputStream(new FileOutputStream("target/oboids.json"))));

	    String confpath  = getResource("ont-config.yaml").getAbsolutePath();
	    
	    init();    
	    load("obo-ids-test.owl");
	    run("--reasoner elk");
	    run("--solr-url mock");
        run("--solr-config "+ confpath);
        run("--solr-load-ontology");
	    System.setOut(out);
	    System.out.println("Done!");
	}
	
}
