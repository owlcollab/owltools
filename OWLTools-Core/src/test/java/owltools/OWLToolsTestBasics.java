package owltools;

import static org.junit.Assert.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.obolibrary.oboformat.model.OBODoc;
import org.obolibrary.oboformat.parser.OBOFormatParserException;
import org.obolibrary.oboformat.writer.OBOFormatWriter;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

public class OWLToolsTestBasics {
    
    private static Logger LOG = Logger.getLogger(OWLToolsTestBasics.class);

	
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
		LOG.debug(renderOBOtoString(oboDoc));
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
	
	private static boolean ignore(OWLAxiom a) {
	    if (a instanceof OWLAnnotationAssertionAxiom) {
	        if (((OWLAnnotationAssertionAxiom)a).getProperty().getIRI().
	                equals(IRI.create("http://www.geneontology.org/formats/oboInOwl#id"))) {
	            return true;
	        }
	    }
	    return false;
	}
	
	
	protected static int compare(OWLOntology ont1, OWLOntology ont2) {
        Set<OWLAxiom> notIn1 = new HashSet<>();
        Set<OWLAxiom> notIn2 = new HashSet<>();
        for (OWLAxiom a1 : ont1.getAxioms()) {
            if (!ont2.containsAxiom(a1) && !ignore(a1)) {
                LOG.error("ont2 missing "+a1);
                notIn2.add(a1);
            }
        }
        
        for (OWLAxiom a2 : ont2.getAxioms()) {
            if (!ont1.containsAxiom(a2) && !ignore(a2)) {
                LOG.error("ont1 missing "+a2);
                notIn2.add(a2);
            }
        }
        
        return notIn1.size() + notIn2.size();
	}
}
