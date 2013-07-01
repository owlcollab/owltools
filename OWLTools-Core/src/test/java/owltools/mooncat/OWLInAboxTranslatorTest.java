package owltools.mooncat;

import java.io.FileOutputStream;

import org.apache.log4j.Logger;
import org.coode.owlapi.turtle.TurtleOntologyFormat;
import org.junit.Test;
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

	private static boolean RENDER_ONTOLOGY_FLAG = false;
	
	private Logger LOG = Logger.getLogger(OWLInAboxTranslatorTest.class);

	
	@Test
	public void testTranslate() throws Exception {
		ParserWrapper pw = new ParserWrapper();
		
		OWLOntology o = pw.parseOWL(getResourceIRIString("mooncat/shadow-tbox-test.owl"));
		OWLInAboxTranslator t = new OWLInAboxTranslator(o);
		OWLOntology abox = t.translate();
		
		o.getOWLOntologyManager().saveOntology(abox,
				new TurtleOntologyFormat(),
				new FileOutputStream("target/shadow-abox.ttl"));
	}



}
