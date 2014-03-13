package owltools.gaf.io;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

import org.junit.BeforeClass;
import org.junit.Test;

import owltools.OWLToolsTestBasics;
import owltools.gaf.GafDocument;
import owltools.gaf.parser.GafObjectsBuilder;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

/**
 * Tests for {@link XgmmlWriter}.
 */
public class XgmmlWriterTest extends OWLToolsTestBasics {
	
	/*
	 * If required, set this flag to true, to print the generated output.
	 */
	private static final boolean printOutput = false;

	private static final ParserWrapper pw = new ParserWrapper();
	private static OWLGraphWrapper graph = null;
	
	@BeforeClass
	public static void beforeClass() throws Exception {
		graph  = pw.parseToOWLGraph(getResourceIRIString("xgmml_test.obo"));
	}
	
	@Test
	public void testXgmmlWriter() throws Exception {
		
		GafObjectsBuilder builder = new GafObjectsBuilder();
		GafDocument gaf = builder.buildDocument(getResource("test_gene_association_mgi.gaf"));
		
		XgmmlWriter w = new XgmmlWriter();
		
		// use byte array as temp buffer
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		w.write(out, graph, Arrays.asList(gaf));
		out.close();
		
		if (printOutput) {
			// convert byte array into a String and print
			System.out.println(new String(out.toByteArray()));
		}
	}

}
