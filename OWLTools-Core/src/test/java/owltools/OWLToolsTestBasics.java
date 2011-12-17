package owltools;

import static junit.framework.Assert.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;

import org.obolibrary.oboformat.model.OBODoc;
import org.obolibrary.oboformat.writer.OBOFormatWriter;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

public class OWLToolsTestBasics {
	
	protected File getResource(String name) {
		assertNotNull(name);
		assertFalse(name.length() == 0);
		// TODO replace this with a mechanism not relying on the relative path
		File file = new File("src/test/resources/"+name);
		assertTrue("Requested resource does not exists: "+file, file.exists());
		return file;
	}
	
	protected String getResourceIRIString(String name) {
		return getResourceIRI(name).toString();
	}
	
	protected IRI getResourceIRI(String name) {
		File file = getResource(name);
		return IRI.create(file);
	}
	
	protected OWLGraphWrapper getGraph(String filename) throws OWLOntologyCreationException, IOException {
		ParserWrapper pw = new ParserWrapper();
		OWLOntology obodoc = pw.parse(getResource(filename).getAbsolutePath());
		return new OWLGraphWrapper(obodoc);
	}
	
	protected static void renderOBO(OBODoc oboDoc) throws IOException {
		OBOFormatWriter writer = new OBOFormatWriter();
		writer.setCheckStructure(true);
		StringWriter out = new StringWriter();
		BufferedWriter stream = new BufferedWriter(out);
		writer.write(oboDoc, stream);
		stream.close();
		System.out.println(out.getBuffer());
	}
}
