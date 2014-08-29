package owltools.util;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Set;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import owltools.OWLToolsTestBasics;
import owltools.vocab.OBOUpperVocabulary;

public class AbstractMinimalModelGeneratorTest extends OWLToolsTestBasics {
	private static Logger LOG = Logger.getLogger(AbstractMinimalModelGeneratorTest.class);

	protected ModelContainer mc;
	protected MinimalModelGenerator mmg;

	protected void expectedOPAs(String msg, Integer size) {
		Set<OWLObjectPropertyAssertionAxiom> opas = 
				mc.getAboxOntology().getAxioms(AxiomType.OBJECT_PROPERTY_ASSERTION);
		for (OWLObjectPropertyAssertionAxiom opa : opas) {
			LOG.info(msg+" : "+opa);
		}
		if (size != null)
			assertEquals(size.intValue(), opas.size());		
	}
	
	protected void expectFact(OWLIndividual subj, OWLObjectProperty prop, OWLIndividual obj) {
		OWLAxiom ax = mc.getOWLDataFactory().getOWLObjectPropertyAssertionAxiom(
				prop,
				subj,
				obj);
		assertTrue(mc.getAboxOntology().containsAxiom(ax, false));
	}
	
	protected void expectedIndividiuals(OWLClass c, Integer size) {
		Set<OWLNamedIndividual> inds =
				mc.getReasoner().getInstances(c, false).getFlattened();
		LOG.info("|" + c+"| = "+inds.size());
		if (size != null)
			assertEquals(size.intValue(), inds.size());
	}

	@Deprecated
	protected IRI oboIRI(String frag) {
		return IRI.create("http://purl.obolibrary.org/obo/"+frag);
	}
	protected OWLObjectProperty getObjectProperty(IRI iri) {
		return mc.getTboxOntology().getOWLOntologyManager().getOWLDataFactory().getOWLObjectProperty(iri);
	}

	protected OWLClass getClass(IRI iri) {
		return mc.getTboxOntology().getOWLOntologyManager().getOWLDataFactory().getOWLClass(iri);
	}
	protected OWLClass getClass(OBOUpperVocabulary v) {
		return mc.getTboxOntology().getOWLOntologyManager().getOWLDataFactory().getOWLClass(v.getIRI());
	}
	protected OWLNamedIndividual getIndividual(IRI iri) {
		return mc.getTboxOntology().getOWLOntologyManager().getOWLDataFactory().getOWLNamedIndividual(iri);
	}
	
	@Deprecated
	protected OWLClass getOboClass(String id) {
		return mc.getTboxOntology().getOWLOntologyManager().getOWLDataFactory().getOWLClass(oboIRI(id));
	}
	
	protected void save(String fn) throws OWLOntologyStorageException, IOException {
		save(fn, mc.getAboxOntology());
	}

	protected void save(String fn, OWLOntology mont) throws OWLOntologyStorageException, IOException {
		FileOutputStream os = new FileOutputStream(new File("target/"+fn+".owl"));
		mc.getOWLOntologyManager().saveOntology(mont, os);
		os.close();
	}
}
