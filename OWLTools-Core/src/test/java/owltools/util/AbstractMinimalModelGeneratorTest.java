package owltools.util;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.obolibrary.oboformat.parser.OBOFormatParserException;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.OWLToolsTestBasics;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;
import owltools.vocab.OBOUpperVocabulary;

/**
 *  
 * 
 * 
 */
public class AbstractMinimalModelGeneratorTest extends OWLToolsTestBasics {
	private static Logger LOG = Logger.getLogger(AbstractMinimalModelGeneratorTest.class);

	//
	protected MinimalModelGenerator mmg;


	

	protected void expectedOPAs(String msg, Integer size) {
		Set<OWLObjectPropertyAssertionAxiom> opas = 
				mmg.getAboxOntology().getAxioms(AxiomType.OBJECT_PROPERTY_ASSERTION);
		for (OWLObjectPropertyAssertionAxiom opa : opas) {
			LOG.info(msg+" : "+opa);
		}
		if (size != null)
			assertEquals(size.intValue(), opas.size());		
	}
	
	protected void expectFact(OWLIndividual subj, OWLObjectProperty prop, OWLIndividual obj) {
		OWLAxiom ax = mmg.getOWLDataFactory().getOWLObjectPropertyAssertionAxiom(
				prop,
				subj,
				obj);
		assertTrue(mmg.getAboxOntology().containsAxiom(ax, false));
	}
	
	protected void expectedIndividiuals(OWLClass c, Integer size) {
		Set<OWLNamedIndividual> inds =
				mmg.getReasoner().getInstances(c, false).getFlattened();
		LOG.info("|" + c+"| = "+inds.size());
		if (size != null)
			assertEquals(size.intValue(), inds.size());
	}

	@Deprecated
	protected IRI oboIRI(String frag) {
		return IRI.create("http://purl.obolibrary.org/obo/"+frag);
	}
	protected OWLObjectProperty getObjectProperty(IRI iri) {
		return mmg.getTboxOntology().getOWLOntologyManager().getOWLDataFactory().getOWLObjectProperty(iri);
	}

	protected OWLClass getClass(IRI iri) {
		return mmg.getTboxOntology().getOWLOntologyManager().getOWLDataFactory().getOWLClass(iri);
	}
	protected OWLClass getClass(OBOUpperVocabulary v) {
		return mmg.getTboxOntology().getOWLOntologyManager().getOWLDataFactory().getOWLClass(v.getIRI());
	}
	protected OWLNamedIndividual getIndividual(IRI iri) {
		return mmg.getTboxOntology().getOWLOntologyManager().getOWLDataFactory().getOWLNamedIndividual(iri);
	}
	
	@Deprecated
	protected OWLClass getOboClass(String id) {
		return mmg.getTboxOntology().getOWLOntologyManager().getOWLDataFactory().getOWLClass(oboIRI(id));
	}
	
	protected void save(String fn) throws OWLOntologyStorageException, IOException {
		save(fn, mmg.getAboxOntology());
	}

	protected void save(String fn, OWLOntology mont) throws OWLOntologyStorageException, IOException {
		FileOutputStream os = new FileOutputStream(new File("target/"+fn+".owl"));
		mmg.getOWLOntologyManager().saveOntology(mont, os);
		os.close();
	}
}
