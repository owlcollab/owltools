package owltools.gaf.io;

import static org.junit.Assert.*;
import static owltools.gaf.io.XgmmlWriter.*;

import java.util.Arrays;

import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLClass;

import owltools.OWLToolsTestBasics;
import owltools.gaf.GafDocument;
import owltools.gaf.GafObjectsBuilder;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

/**
 * Tests for {@link XgmmlWriter}.
 */
public class LegoXgmmlWriterTest extends OWLToolsTestBasics {

	private static final ParserWrapper pw = new ParserWrapper();
	private static OWLGraphWrapper graph = null;
	
	@BeforeClass
	public static void beforeClass() throws Exception {
		graph  = pw.parseToOWLGraph(getResourceIRIString("NEDD4.owl"));
	}
	
	@Test
	public void testXgmmlWriter() throws Exception {
		
		GafObjectsBuilder builder = new GafObjectsBuilder();
		GafDocument gaf = builder.buildDocument(getResource("test_gene_association_mgi.gaf"));
		
		XgmmlWriter w = new XgmmlWriter();
		w.write(System.out, graph, Arrays.asList(gaf));
	}

}
