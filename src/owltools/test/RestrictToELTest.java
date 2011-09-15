package owltools.test;

import static junit.framework.Assert.*;

import java.io.File;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.semanticweb.owlapi.io.OWLXMLOntologyFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

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
		OWLGraphWrapper gEL = InferenceBuilder.enforceEL(g);
		assertTrue(g.getSourceOntology().getAxiomCount() > gEL.getSourceOntology().getAxiomCount());
		writeOWLOntolog(gEL, "pizza-2007-02-12-el.owl");
	}
	
	/**
	 * Test whether the ontology has the same ontology id after the conversion.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testRetainOntologyId() throws Exception {
		ParserWrapper pw = new ParserWrapper();
		OWLGraphWrapper g = pw.parseToOWLGraph(getResourceIRIString("simple-deprecated.owl"));
		String ontologyId = "fribble";
		assertEquals(ontologyId, g.getOntologyId());
		OWLGraphWrapper gEL = InferenceBuilder.enforceEL(g);
		assertEquals(ontologyId, gEL.getOntologyId());
	}
	
	/**
	 * Test whether the ontology still has deprecation annotations after the conversion.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testRetainDeprecated() throws Exception {
		ParserWrapper pw = new ParserWrapper();
		OWLGraphWrapper g = pw.parseToOWLGraph(getResourceIRIString("simple-deprecated.owl"));
		String label = "X:1";
		assertTrue(isDeprecated(label, g));
		OWLGraphWrapper gEL = InferenceBuilder.enforceEL(g);
		writeOWLOntolog(gEL, "simple-deprecated-el.owl");
		assertTrue(isDeprecated(label, gEL));
	}
	
	private boolean isDeprecated(String label, OWLGraphWrapper graph) {
		OWLObject owlObject = graph.getOWLObjectByLabel(label);
		assertNotNull(owlObject);
		if (owlObject instanceof OWLNamedObject) {
			IRI iri = ((OWLNamedObject) owlObject).getIRI();
			OWLOntology ontology = graph.getSourceOntology();
			Set<OWLAnnotationAssertionAxiom> assertionAxioms = ontology.getAnnotationAssertionAxioms(iri);
			assertNotNull(assertionAxioms);
			OWLDataFactory owlDataFactory = ontology.getOWLOntologyManager().getOWLDataFactory();
			OWLAnnotationProperty deprecated = owlDataFactory.getOWLDeprecated();
			for (OWLAnnotationAssertionAxiom axiom : assertionAxioms) {
				OWLAnnotationProperty property = axiom.getProperty();
				if (deprecated.equals(property)) {
					OWLAnnotationValue value = axiom.getValue();
					if (value instanceof OWLLiteral) {
						OWLLiteral literal = (OWLLiteral) value;
						String literalValue = literal.getLiteral();
						if(literal.getDatatype().isBoolean()) {
							boolean b = Boolean.parseBoolean(literalValue);
							return b;
						}
					}
				}
			}			
		}
		return false;
	}
	
	private void writeOWLOntolog(OWLGraphWrapper gmod, String fileName) throws OWLOntologyStorageException {
		File file = new File(FileUtils.getTempDirectory(), fileName);
		IRI iri = IRI.create(file);
		System.out.println("Saving ontology to file: "+file);
		gmod.getSourceOntology().getOWLOntologyManager().saveOntology(gmod.getSourceOntology(), new OWLXMLOntologyFormat(), iri);
	}
}
