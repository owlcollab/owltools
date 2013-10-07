package owltools.util;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.OWLToolsTestBasics;

/**
 */
public class MinimalModelGeneratorTest extends OWLToolsTestBasics {
	private static Logger LOG = Logger.getLogger(MinimalModelGeneratorTest.class);

	OWLOntologyManager m;
	OWLOntology tbox;
	MinimalModelGenerator mmg;

	@Test
	public void testGenerateAnatomy() throws OWLOntologyCreationException, OWLOntologyStorageException, IOException {
		m = OWLManager.createOWLOntologyManager();
		tbox = m.loadOntologyFromOntologyDocument(getResource("basic-tbox.omn"));
		mmg = new MinimalModelGenerator(tbox, new org.semanticweb.HermiT.Reasoner.ReasonerFactory());
		OWLClass c = getClass("hand");
		mmg.generateNecessaryIndividuals(c, true);
		// TODO - check
		save("basic-abox");
		
		mmg.generateNecessaryIndividuals(getClass("foot"), true);
		save("basic-abox2");
	}
	
	@Test
	public void testGeneratePathway() throws OWLOntologyCreationException, OWLOntologyStorageException, IOException {
		m = OWLManager.createOWLOntologyManager();
		tbox = m.loadOntologyFromOntologyDocument(getResource("basic-tbox.omn"));
		mmg = new MinimalModelGenerator(tbox, new org.semanticweb.HermiT.Reasoner.ReasonerFactory());
		OWLClass c = getClass("bar_response_pathway");
		mmg.generateNecessaryIndividuals(c, true);
		// TODO - check
		save("pathway-abox");

		Set<OWLClass> occs = new HashSet<OWLClass>();
		occs.add(getOboClass("GO_0003674"));
		occs.add(getOboClass("GO_0008150"));
		mmg.anonymizeIndividualsNotIn(occs);

		// futzing
		m.addAxioms(mmg.getAboxOntology(), tbox.getAxioms());
		Set<OWLOntology> onts = new HashSet<OWLOntology>();
		onts.add(tbox);
		onts.add(mmg.getAboxOntology());
		OWLOntology mont = m.createOntology(IRI.create("hhtp://x.org/merged"), onts);
		save("pathway-abox-merged", mont);
	}



	@Test
	public void testMSC() throws OWLOntologyCreationException, OWLOntologyStorageException, IOException {
		m = OWLManager.createOWLOntologyManager();
		tbox = m.loadOntologyFromOntologyDocument(getResource("pathway-abox.omn"));
		mmg = new MinimalModelGenerator(tbox, new org.semanticweb.HermiT.Reasoner.ReasonerFactory());
		OWLNamedIndividual i = getIndividual("pathway1");
		OWLClassExpression x = mmg.getMostSpecificClassExpression(i);
		LOG.info("MSCE:"+x);
	}

	protected IRI getIRI(String frag) {
		return IRI.create("http://x.org/"+frag);
	}

	protected OWLClass getClass(String frag) {
		return tbox.getOWLOntologyManager().getOWLDataFactory().getOWLClass(getIRI(frag));
	}
	protected OWLNamedIndividual getIndividual(String frag) {
		return tbox.getOWLOntologyManager().getOWLDataFactory().getOWLNamedIndividual(getIRI(frag));
	}
	

	protected OWLClass getOboClass(String id) {
		return tbox.getOWLOntologyManager().getOWLDataFactory().getOWLClass(IRI.create("http://purl.obolibrary.org/obo/"+id));
	}
	
	protected void save(String fn) throws OWLOntologyStorageException, IOException {
		save(fn, mmg.getAboxOntology());
	}

	
	protected void save(String fn, OWLOntology mont) throws OWLOntologyStorageException, IOException {
		FileOutputStream os = new FileOutputStream(new File("target/"+fn+".owl"));
		m.saveOntology(mont, os);
		os.close();
	}
}
