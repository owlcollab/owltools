package owltools.mooncat;

import java.io.FileOutputStream;

import org.junit.Test;
import org.semanticweb.owlapi.formats.TurtleDocumentFormat;
import org.semanticweb.owlapi.model.OWLOntology;

import owltools.OWLToolsTestBasics;
import owltools.io.ParserWrapper;

/**
 * This is the main test class for PropertyViewOntologyBuilder
 * 
 * @author cjm
 *
 */
public class OWLInAboxTranslatorTest extends OWLToolsTestBasics {

	@Test
	public void testTranslate() throws Exception {
		ParserWrapper pw = new ParserWrapper();
		
		OWLOntology o = pw.parseOWL(getResourceIRIString("mooncat/shadow-tbox-test.owl"));
		OWLInAboxTranslator t = new OWLInAboxTranslator(o);
		OWLOntology abox = t.translate();
		
		o.getOWLOntologyManager().saveOntology(abox,
				new TurtleDocumentFormat(),
				new FileOutputStream("target/shadow-abox.ttl"));
	}



}
