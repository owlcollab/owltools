package owltools.io;

import static org.junit.Assert.*;

import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import owltools.OWLToolsTestBasics;

public class OboFormatOwlApiHandling extends OWLToolsTestBasics {
	
	@Test
	public void test() throws Exception {
		ParserWrapper pw = new ParserWrapper();
		try {
			IRI resourceIRI = getResourceIRI("obo/broken.obo");
			pw.parseOWL(resourceIRI);
			fail("Expected an exception.");
		} catch (OWLOntologyCreationException e) {
			// catch silent, expected!
		}
		
	}

}
