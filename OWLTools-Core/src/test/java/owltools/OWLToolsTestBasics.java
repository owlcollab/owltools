package owltools;

import static org.junit.Assert.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;

import org.obolibrary.oboformat.model.OBODoc;
import org.obolibrary.oboformat.parser.OBOFormatParserException;
import org.obolibrary.oboformat.writer.OBOFormatWriter;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

public class OWLToolsTestBasics {
	
	protected static File getResource(String name) {
		assertNotNull(name);
		assertFalse(name.length() == 0);
		// TODO replace this with a mechanism not relying on the relative path
		File file = new File("src/test/resources/"+name);
		assertTrue("Requested resource does not exists: "+file, file.exists());
		return file;
	}
	
	protected static String getResourceIRIString(String name) {
		return getResourceIRI(name).toString();
	}
	
	protected static IRI getResourceIRI(String name) {
		File file = getResource(name);
		return IRI.create(file);
	}
	
	protected static OWLGraphWrapper getGraph(String filename) throws OWLOntologyCreationException, IOException, OBOFormatParserException {
		ParserWrapper pw = new ParserWrapper();
		OWLOntology obodoc = pw.parse(getResource(filename).getAbsolutePath());
		return new OWLGraphWrapper(obodoc);
	}
	
	protected static void renderOBO(OBODoc oboDoc) throws IOException {
		System.out.println(renderOBOtoString(oboDoc));
	}
	
	protected static String renderOBOtoString(OBODoc oboDoc) throws IOException {
		OBOFormatWriter writer = new OBOFormatWriter();
		writer.setCheckStructure(true);
		StringWriter out = new StringWriter();
		BufferedWriter stream = new BufferedWriter(out);
		writer.write(oboDoc, stream);
		stream.close();
		return out.getBuffer().toString();
	}
}
