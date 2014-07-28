package owltools.gaf.io;

import static org.junit.Assert.*;
import static owltools.gaf.io.PseudoRdfXmlWriter.*;

import java.io.File;
import java.util.Arrays;

import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLClass;

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
