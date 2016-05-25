package owltools.mooncat;

import org.junit.Test;
import org.semanticweb.owlapi.formats.OBODocumentFormat;
import org.semanticweb.owlapi.model.OWLOntology;

import owltools.OWLToolsTestBasics;
import owltools.io.ParserWrapper;

/**
 * This is the main test class for PropertyViewOntologyBuilder
 * 
 * @author cjm
 *
 */
public class PropertyExtractorTest extends OWLToolsTestBasics {

	@Test
	public void testExtractBridge() throws Exception {
		ParserWrapper pw = new ParserWrapper();
		
		OWLOntology sourceOntol = pw.parseOBO(getResourceIRIString("go_xp_predictor_test_subset.obo"));
		OWLOntology propertyOntol = pw.parseOWL(getResourceIRIString("ro.owl"));
		PropertyExtractor pe = new PropertyExtractor(propertyOntol, sourceOntol);
		
		OWLOntology pont = pe.extractPropertyOntology();
		
		
		pw.saveOWL(pont, new OBODocumentFormat(), "target/foo.obo");
	}



}
