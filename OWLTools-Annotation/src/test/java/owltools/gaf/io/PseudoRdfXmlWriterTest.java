package owltools.gaf.io;

import static org.junit.Assert.*;
import static owltools.gaf.io.PseudoRdfXmlWriter.*;

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
 * Tests for {@link PseudoRdfXmlWriter}.
 */
public class PseudoRdfXmlWriterTest extends OWLToolsTestBasics {

	private static final ParserWrapper pw = new ParserWrapper();
	private static OWLGraphWrapper graph = null;
	
	@BeforeClass
	public static void beforeClass() throws Exception {
		graph  = pw.parseToOWLGraph(getResourceIRIString("go_xp_predictor_test_subset.obo"));
	}
	
	@Ignore
	@Test
	public void testWriter() throws Exception {
		
		GafObjectsBuilder builder = new GafObjectsBuilder();
		GafDocument gaf = builder.buildDocument(getResource("test_gene_association_mgi.gaf"));
		
		PseudoRdfXmlWriter w = new PseudoRdfXmlWriter();
		w.write(System.out, graph, gaf);
	}
	
	@Test
	public void testRelationWriter() throws Exception {
		PseudoRdfXmlWriter w = new PseudoRdfXmlWriter();
		OWLClass c = graph.getOWLClassByIdentifier("GO:0006417");
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		XMLStreamWriter writer  = w.createWriter(outputStream);

		writer.setPrefix("go", PseudoRdfXmlWriter.GO_NAMESPACE_URI);
		writer.setPrefix("rdf", PseudoRdfXmlWriter.RDF_NAMESPACE_URI);
		
		writer.writeStartElement("test");
		w.writeRelations(writer, c, graph);
		writer.writeEndElement();
		
		writer.close();
		assertEquals("\n<test>\n" +
				DEFAULT_INDENT+"<go:is_a rdf:resource=\"http://www.geneontology.org/go#GO:0010608\" />\n" +
				DEFAULT_INDENT+"<go:is_a rdf:resource=\"http://www.geneontology.org/go#GO:0032268\" />\n" +
				DEFAULT_INDENT+"<go:is_a rdf:resource=\"http://www.geneontology.org/go#GO:0065007\" />\n" +
				DEFAULT_INDENT+"<go:is_a rdf:resource=\"http://www.geneontology.org/go#GO:2000112\" />\n" +
				DEFAULT_INDENT+"<go:regulates rdf:resource=\"http://www.geneontology.org/go#GO:0006412\" />\n" +
				"</test>",
				outputStream.toString());
	}

}
