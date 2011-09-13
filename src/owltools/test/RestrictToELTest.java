package owltools.test;

import static junit.framework.Assert.*;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.semanticweb.owlapi.io.OWLXMLOntologyFormat;
import org.semanticweb.owlapi.model.IRI;

import owltools.InferenceBuilder;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;


public class RestrictToELTest extends OWLToolsTestBasics {

	/**
	 * Simple test for {@link InferenceBuilder#enforceEL(OWLGraphWrapper)}.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testEnforceEL() throws Exception {
		// Simple test for the method: no logic checks here
		ParserWrapper pw = new ParserWrapper();
		OWLGraphWrapper g = pw.parseToOWLGraph(getResourceIRIString("pizza-2007-02-12.owl"));
		OWLGraphWrapper gmod = InferenceBuilder.enforceEL(g);
		assertTrue(g.getSourceOntology().getAxiomCount() > gmod.getSourceOntology().getAxiomCount());
		File file = new File(FileUtils.getTempDirectory(), "pizza-2007-02-12-el.owl");
		IRI iri = IRI.create(file);
		System.out.println("Saving ontology to file: "+file);
		gmod.getSourceOntology().getOWLOntologyManager().saveOntology(gmod.getSourceOntology(), new OWLXMLOntologyFormat(), iri);
	}
}
