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
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.OWLToolsTestBasics;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

/**
 */
public class MinimalModelGeneratorTest extends OWLToolsTestBasics {
	private static Logger LOG = Logger.getLogger(MinimalModelGeneratorTest.class);

	OWLOntologyManager m;
	OWLOntology tbox;
	MinimalModelGenerator mmg;

	// this test may disappeard
	@Test
	public void testImports() throws OWLOntologyCreationException, OWLOntologyStorageException, IOException, OBOFormatParserException {
		ParserWrapper pw = new ParserWrapper();
		OWLGraphWrapper g = pw.parseToOWLGraph(getResourceIRIString("go-pombase-basicset.obo"));
		tbox = g.getSourceOntology();
		mmg = new MinimalModelGenerator(tbox, new org.semanticweb.HermiT.Reasoner.ReasonerFactory());
		int aboxImportsSize = mmg.getAboxOntology().getImportsClosure().size();
		int qboxImportsSize = mmg.getQueryOntology().getImportsClosure().size();

		LOG.info("Abox ontology imports: "+aboxImportsSize);
		LOG.info("Q ontology imports: "+qboxImportsSize);
		assertEquals(2, aboxImportsSize);
		assertEquals(3, qboxImportsSize);
		OWLClass c = getClass("hand");
		mmg.generateNecessaryIndividuals(c, true);
		// TODO - check
		save("basic-abox");
		
		mmg.generateNecessaryIndividuals(getClass("foot"), true);
		save("basic-abox2");
	}

	@Test
	public void testGenerateAnatomy() throws OWLOntologyCreationException, OWLOntologyStorageException, IOException {
		m = OWLManager.createOWLOntologyManager();
		tbox = m.loadOntologyFromOntologyDocument(getResource("basic-tbox.omn"));
		mmg = new MinimalModelGenerator(tbox, new org.semanticweb.HermiT.Reasoner.ReasonerFactory());
		int aboxImportsSize = mmg.getAboxOntology().getImportsClosure().size();
		int qboxImportsSize = mmg.getQueryOntology().getImportsClosure().size();

		LOG.info("Abox ontology imports: "+aboxImportsSize);
		LOG.info("Q ontology imports: "+qboxImportsSize);
		assertEquals(2, aboxImportsSize);
		assertEquals(3, qboxImportsSize);
		OWLClass c = getClass("hand");
		mmg.generateNecessaryIndividuals(c, true);
		// TODO - check
		save("basic-abox");
		
		mmg.generateNecessaryIndividuals(getClass("foot"), true);
		save("basic-abox2");
	}
	
	@Test
	public void testGenerateAnatomySameOntology() throws OWLOntologyCreationException, OWLOntologyStorageException, IOException {
		m = OWLManager.createOWLOntologyManager();
		tbox = m.loadOntologyFromOntologyDocument(getResource("basic-tbox.omn"));
		//OWLReasoner reasoner = new org.semanticweb.HermiT.Reasoner.ReasonerFactory().createReasoner(tbox);
		mmg = new MinimalModelGenerator(tbox, tbox, new ElkReasonerFactory());
		OWLClass c = getClass("hand");
		mmg.generateNecessaryIndividuals(c, true);
		// TODO - check
		save("basic-abox-v2");
	}

	
	@Test
	public void testGenerateGlycolysis() throws OWLOntologyCreationException, OWLOntologyStorageException, IOException {
		m = OWLManager.createOWLOntologyManager();
		tbox = m.loadOntologyFromOntologyDocument(getResource("glycolysis-tbox.omn"));
		mmg = new MinimalModelGenerator(tbox, tbox, new org.semanticweb.HermiT.Reasoner.ReasonerFactory());
		OWLClass c = 
				tbox.getOWLOntologyManager().getOWLDataFactory().getOWLClass(IRI.create("http://purl.obolibrary.org/obo/GO_0006096"));

		mmg.generateNecessaryIndividuals(c, true);
		
		//mmg.generateNecessaryIndividuals(getClass("foot"), true);
		save("glycolysis-tbox2abox");
	}
	
	@Test
	public void testGeneratePathway() throws OWLOntologyCreationException, OWLOntologyStorageException, IOException {
		m = OWLManager.createOWLOntologyManager();
		tbox = m.loadOntologyFromOntologyDocument(getResource("basic-tbox.omn"));
		mmg = new MinimalModelGenerator(tbox, new org.semanticweb.HermiT.Reasoner.ReasonerFactory());
		//mmg.setPrecomputePropertyClassCombinations(false);
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
		OWLClassExpression x = mmg.getMostSpecificClassExpression(i, null);
		LOG.info("MSCE:"+x);
	}
	
	@Test
	public void testMSCGlycolysis() throws OWLOntologyCreationException, OWLOntologyStorageException, IOException {
		m = OWLManager.createOWLOntologyManager();
		tbox = m.loadOntologyFromOntologyDocument(getResource("glycolysis-abox.omn"));
		mmg = new MinimalModelGenerator(tbox, new org.semanticweb.HermiT.Reasoner.ReasonerFactory());
		OWLNamedIndividual i = 
				tbox.getOWLOntologyManager().getOWLDataFactory().getOWLNamedIndividual(IRI.create("http://purl.obolibrary.org/obo/GLY_TEST_0000001"));
		ArrayList<OWLObjectProperty> propertySet = new ArrayList<OWLObjectProperty>();
		propertySet.add(getObjectProperty(oboIRI("directly_activates")));
		propertySet.add(getObjectProperty(oboIRI("BFO_0000051")));
		OWLClassExpression x = mmg.getMostSpecificClassExpression(i, propertySet);
		LOG.info("MSCE:"+x);
	}

	protected IRI getIRI(String frag) {
		return IRI.create("http://x.org/"+frag);
	}
	protected IRI oboIRI(String frag) {
		return IRI.create("http://purl.obolibrary.org/obo/"+frag);
	}
	protected OWLObjectProperty getObjectProperty(IRI iri) {
		return tbox.getOWLOntologyManager().getOWLDataFactory().getOWLObjectProperty(iri);
	}

	protected OWLClass getClass(String frag) {
		return tbox.getOWLOntologyManager().getOWLDataFactory().getOWLClass(getIRI(frag));
	}
	protected OWLNamedIndividual getIndividual(String frag) {
		return tbox.getOWLOntologyManager().getOWLDataFactory().getOWLNamedIndividual(getIRI(frag));
	}
	

	protected OWLClass getOboClass(String id) {
		return tbox.getOWLOntologyManager().getOWLDataFactory().getOWLClass(oboIRI(id));
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
