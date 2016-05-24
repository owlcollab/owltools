package owltools.gaf.io;

import org.junit.Test;

import owltools.OWLToolsTestBasics;
import owltools.gaf.GafDocument;
import owltools.gaf.parser.GafObjectsBuilder;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

/**
 * Tests for {@link OpenAnnotationRDFWriter}.
 */
public class OpenAnnotationRDFWriterTest extends OWLToolsTestBasics {

	private static final ParserWrapper pw = new ParserWrapper();
	private static OWLGraphWrapper graph = null;
	

	
	@Test
	public void testWriter() throws Exception {
		
		GafObjectsBuilder builder = new GafObjectsBuilder();
		GafDocument gaf = builder.buildDocument(getResource("test_gene_association_mgi.gaf"));
		
		OpenAnnotationRDFWriter w = new OpenAnnotationRDFWriter();
		w.setGafMode();
		w.write(gaf, "target/mgi-oa.ttl");
	}
	
	

}
