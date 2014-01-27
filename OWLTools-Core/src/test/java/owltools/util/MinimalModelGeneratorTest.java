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
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyIRIMapper;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.util.OWLEntityRenamer;

import owltools.OWLToolsTestBasics;
import owltools.graph.OWLGraphWrapper;
import owltools.io.CatalogXmlIRIMapper;
import owltools.io.ParserWrapper;
import owltools.vocab.OBOUpperVocabulary;

/**
 *  
 * 
 * 
 */
public class MinimalModelGeneratorTest extends AbstractMinimalModelGeneratorTest {
	private static Logger LOG = Logger.getLogger(MinimalModelGeneratorTest.class);

	OWLOntologyManager m;

	/**
	 * Basic test of minimal model generation. Takes an existential model of
	 * limb anatomy, creates prototype individuals.
	 * 
	 * Addresses following challenges
	 *  - autoclassification of digits into 'finger' or 'toe' (requires inverses)
	 *  - heuristic collapse of 'limb' into forelimb and generation of a single 'organism'
	 * 
	 * @throws OWLOntologyCreationException
	 * @throws OWLOntologyStorageException
	 * @throws IOException
	 */
	@Test
	public void testGenerateAnatomyDL() throws OWLOntologyCreationException, OWLOntologyStorageException, IOException {
		m = OWLManager.createOWLOntologyManager();
		m.addIRIMapper(new CatalogXmlIRIMapper(getResource("catalog-v001.xml")));
		OWLOntology tbox = m.loadOntologyFromOntologyDocument(getResource("basic-tbox-importer.omn"));
		mmg = new MinimalModelGenerator(tbox, new org.semanticweb.HermiT.Reasoner.ReasonerFactory());
		mmg.setAssertInverses(false);  // NOT NECESSARY FOR A DL REASONER
		int aboxImportsSize = mmg.getAboxOntology().getImportsClosure().size();
		int qboxImportsSize = mmg.getQueryOntology().getImportsClosure().size();

		LOG.info("Abox ontology imports: "+aboxImportsSize);
		LOG.info("Q ontology imports: "+qboxImportsSize);
		assertEquals(3, aboxImportsSize);
		assertEquals(4, qboxImportsSize);
		OWLClass c = getClass("hand");
		mmg.generateNecessaryIndividuals(c);
		
		// test that there is only one limb in the model
		// (collapsing step reduces the limb that must exist if an autopod exists
		//  into the already existing forelimb)
		LOG.info("#inds (after adding hand):"+mmg.getGeneratedIndividuals().size());
		assertEquals(4, mmg.getGeneratedIndividuals().size());
	
		// we have added hand, which generates at least one digit, which
		// is inferred to be of type finger (via inverse axioms)
		this.expectedIndividiuals("digit", 1);
		// test deepening
		this.expectedIndividiuals("finger", 1);

		expectedOPAs("post-hand, un-normalized", 6);
		mmg.normalizeDirections(partOf());
		expectedOPAs("post-hand, normalized", 3);
		
		// for debugging
		save("basic-abox");
		
		mmg.generateNecessaryIndividuals(getClass("foot"), true);
		LOG.info("#inds (after adding foot):"+mmg.getGeneratedIndividuals().size());
		assertEquals(7, mmg.getGeneratedIndividuals().size());
		
		this.expectedIndividiuals("digit", 2);

		// test deepening
		this.expectedIndividiuals("toe", 1);
		this.expectedIndividiuals("finger", 1);

		expectedOPAs("post-foot", 8);
		
		// for debugging
		save("basic-abox2");
	}

	/**
	 * As DL test, but using Elk, with additional inverse assertion being provided by
	 * MMG
	 * 
	 * @throws OWLOntologyCreationException
	 * @throws OWLOntologyStorageException
	 * @throws IOException
	 */
	@Test
	public void testGenerateAnatomyUsingElk() throws OWLOntologyCreationException, OWLOntologyStorageException, IOException {
		m = OWLManager.createOWLOntologyManager();
		OWLOntology tbox = m.loadOntologyFromOntologyDocument(getResource("basic-tbox.omn"));
		mmg = new MinimalModelGenerator(tbox, new org.semanticweb.HermiT.Reasoner.ReasonerFactory());
		mmg.setAssertInverses(true); // NECESSARY FOR ELK
		int aboxImportsSize = mmg.getAboxOntology().getImportsClosure().size();
		int qboxImportsSize = mmg.getQueryOntology().getImportsClosure().size();

		LOG.info("Abox ontology imports: "+aboxImportsSize);
		LOG.info("Q ontology imports: "+qboxImportsSize);
		assertEquals(2, aboxImportsSize);
		assertEquals(3, qboxImportsSize);
		
		
		OWLClass c = getClass("hand");
		
		// for this test we leave the transitive reduction stee until later
		mmg.generateNecessaryIndividuals(c, true, false);
		
		// test that there is only one limb in the model
		// (collapsing step reduces the limb that must exist if an autopod exists
		//  into the already existing forelimb)
		LOG.info("#inds (after adding hand):"+mmg.getGeneratedIndividuals().size());
		assertEquals(4, mmg.getGeneratedIndividuals().size());
		
		// we have added hand, which generates at least one digit, which
		// is inferred to be of type finger (via inverse axioms)
		expectedIndividiuals("digit", 1);
		// test deepening
		expectedIndividiuals("finger", 1);
		
		expectFact("finger-proto", "part_of", "hand-proto");
		expectFact("hand-proto", "part_of", "forelimb-proto");
		expectFact("forelimb-proto", "part_of", "organism-proto");

		expectedOPAs("post-hand, un-normalized, no reduction", 8);

		mmg.performTransitiveReduction(partOf());
		expectedOPAs("post-hand, reduced", 6);

		//mmg.normalizeDirections(partOf());
		//expectedOPAs("post-hand, reduced, normalized", 3);
		
		
		// for debugging
		save("basic-abox-elk");
		
		mmg.generateNecessaryIndividuals(getClass("foot"), true);
		LOG.info("#inds (after adding foot):"+mmg.getGeneratedIndividuals().size());
		assertEquals(7, mmg.getGeneratedIndividuals().size());

		expectedIndividiuals("digit", 2);
		// test deepening
		expectedIndividiuals("finger", 1);
		expectedIndividiuals("toe", 1);

		expectedOPAs("post-foot, unprocessed", 12);

		
		mmg.normalizeDirections(partOf());
		expectedOPAs("post-foot, normalized", 6);
		
		expectFact("finger-proto", "part_of", "hand-proto");
		expectFact("hand-proto", "part_of", "forelimb-proto");
		expectFact("forelimb-proto", "part_of", "organism-proto");
		expectFact("toe-proto", "part_of", "foot-proto");
		expectFact("foot-proto", "part_of", "hindlimb-proto");
		expectFact("hindlimb-proto", "part_of", "organism-proto");

			
		mmg.extractModule();
		// for debugging
		save("basic-abox2-elk");
	}
	
	@Test
	public void testGenerateAnatomyNoCollapse() throws OWLOntologyCreationException, OWLOntologyStorageException, IOException {
		m = OWLManager.createOWLOntologyManager();
		OWLOntology tbox = m.loadOntologyFromOntologyDocument(getResource("basic-tbox.omn"));
		mmg = new MinimalModelGenerator(tbox, new ElkReasonerFactory());
		mmg.setAssertInverses(true); // NECESSARY FOR ELK
		int aboxImportsSize = mmg.getAboxOntology().getImportsClosure().size();
		int qboxImportsSize = mmg.getQueryOntology().getImportsClosure().size();

		LOG.info("Abox ontology imports: "+aboxImportsSize);
		LOG.info("Q ontology imports: "+qboxImportsSize);
		assertEquals(2, aboxImportsSize);
		assertEquals(3, qboxImportsSize);
		
		
		OWLClass c = getClass("hand");
		
		// for this test we do NOT collapse.
		// The model should be valid, but may be incomplete w.r.t
		// sameAs assertions
		mmg.generateNecessaryIndividuals(c, false, true);
		
		// test that there is only one limb in the model
		// (collapsing step reduces the limb that must exist if an autopod exists
		//  into the already existing forelimb)
		LOG.info("#inds (after adding hand):"+mmg.getGeneratedIndividuals().size());
		assertEquals(4, mmg.getGeneratedIndividuals().size());
		
		// we have added hand, which generates at least one digit, which
		// is inferred to be of type finger (via inverse axioms)
		expectedIndividiuals("digit", 1);
		// test deepening
		expectedIndividiuals("finger", 1);
		
		
		mmg.generateNecessaryIndividuals(getClass("foot"), false, true);
		LOG.info("#inds (after adding foot):"+mmg.getGeneratedIndividuals().size());
		//assertEquals(7, mmg.getGeneratedIndividuals().size());

		expectedIndividiuals("digit", 2);
		// test deepening
		expectedIndividiuals("finger", 1);
		expectedIndividiuals("toe", 1);

		expectedOPAs("post-foot, unprocessed", 12);

		
		mmg.normalizeDirections(partOf());

			
		mmg.extractModule();
		// for debugging
		save("basic-abox-uncollapsed");
	}
	
	@Test
	public void testGeneratePrototypicalHuman() throws OWLOntologyCreationException, OWLOntologyStorageException, IOException {
		m = OWLManager.createOWLOntologyManager();
		OWLOntology tbox = m.loadOntologyFromOntologyDocument(getResource("basic-tbox.omn"));
		mmg = new MinimalModelGenerator(tbox, new ElkReasonerFactory());
		mmg.setAssertInverses(true);
		int aboxImportsSize = mmg.getAboxOntology().getImportsClosure().size();
		int qboxImportsSize = mmg.getQueryOntology().getImportsClosure().size();

		LOG.info("Abox ontology imports: "+aboxImportsSize);
		LOG.info("Q ontology imports: "+qboxImportsSize);
		assertEquals(2, aboxImportsSize);
		assertEquals(3, qboxImportsSize);
		OWLClass c = getClass("human");
		mmg.generateNecessaryIndividuals(c);
		
		OWLNamedIndividual forelimb = mmg.getOWLDataFactory().getOWLNamedIndividual(this.getIRI("forelimb-proto"));
		Set<OWLObjectSomeValuesFrom> rels = mmg.getExistentialRelationships(forelimb);
		for (OWLObjectSomeValuesFrom rel : rels) {
			LOG.info(" proto-forelimb IREL: "+rel);
		}
		
		// test that there is only one limb in the model
		// (collapsing step reduces the limb that must exist if an autopod exists
		//  into the already existing forelimb)
		LOG.info("#inds (after adding human):"+mmg.getGeneratedIndividuals().size());
        assertEquals(7, mmg.getGeneratedIndividuals().size());
		
		expectedIndividiuals("digit", 2);
		// test deepening
		expectedIndividiuals("finger", 1);
		expectedIndividiuals("toe", 1);
		expectedIndividiuals("limb", 2);

		expectedIndividiuals("forelimb", 1);
		expectedIndividiuals("hindlimb", 1);
		expectedIndividiuals("human", 1);

		mmg.normalizeDirections(partOf());

		expectedOPAs("normalized", 6);

		expectFact("finger-proto", "part_of", "hand-proto");
		expectFact("hand-proto", "part_of", "forelimb-proto");
		expectFact("forelimb-proto", "part_of", "human-proto");
		expectFact("toe-proto", "part_of", "foot-proto");
		expectFact("foot-proto", "part_of", "hindlimb-proto");
		expectFact("hindlimb-proto", "part_of", "human-proto");

		// for debugging
		mmg.extractModule();
		save("prototypical-human");
	}
	
	/**
	 * as above, but in scenario where we want to add abox assertions
	 * directly into main ontology
	 * 
	 * @throws OWLOntologyCreationException
	 * @throws OWLOntologyStorageException
	 * @throws IOException
	 */
	@Test
	public void testGenerateAnatomySameOntology() throws OWLOntologyCreationException, OWLOntologyStorageException, IOException {
		m = OWLManager.createOWLOntologyManager();
		OWLOntology tbox = m.loadOntologyFromOntologyDocument(getResource("basic-tbox.omn"));
		//OWLReasoner reasoner = new org.semanticweb.HermiT.Reasoner.ReasonerFactory().createReasoner(tbox);
		mmg = new MinimalModelGenerator(tbox, tbox, new ElkReasonerFactory());
		OWLClass c = getClass("hand");
		mmg.generateNecessaryIndividuals(c);
		
		// we have added hand, which generates at least one digit, which
		// is inferred to be of type finger (via inverse axioms)
		expectedIndividiuals("digit", 1);
		// test deepening
		expectedIndividiuals("finger", 1);
		
		expectFact("finger-proto", "part_of", "hand-proto");
		expectFact("hand-proto", "part_of", "forelimb-proto");
		expectFact("forelimb-proto", "part_of", "organism-proto");

		expectedOPAs("post-hand, reduced, but not normalized", 6);
		mmg.normalizeDirections(partOf());
		expectedOPAs("post-hand, reduced, normalized", 3);

		// TODO - check
		save("basic-abox-v2");
	}

	@Test
	public void testGenerateAnatomyWithImport() throws OWLOntologyCreationException, OWLOntologyStorageException, IOException {
		m = OWLManager.createOWLOntologyManager();
		OWLOntology abox = m.createOntology(IRI.create("http://x.org/foo/bar"));
		OWLOntology tbox = m.loadOntologyFromOntologyDocument(getResource("basic-tbox.omn"));
		AddImport ai = new AddImport(abox, 
				m.getOWLDataFactory().getOWLImportsDeclaration(
						tbox.getOntologyID().getOntologyIRI()));
		m.applyChange(ai);
		mmg = new MinimalModelGenerator(abox, abox, new ElkReasonerFactory());
		OWLClass c = getClass("hand");
		mmg.generateNecessaryIndividuals(c);
		
		// we have added hand, which generates at least one digit, which
		// is inferred to be of type finger (via inverse axioms)
		expectedIndividiuals("digit", 1);
		// test deepening
		expectedIndividiuals("finger", 1);
		
		expectFact("finger-proto", "part_of", "hand-proto");
		expectFact("hand-proto", "part_of", "forelimb-proto");
		expectFact("forelimb-proto", "part_of", "organism-proto");

		expectedOPAs("post-hand, reduced, but not normalized", 6);
		mmg.normalizeDirections(partOf());
		expectedOPAs("post-hand, reduced, normalized", 3);

		// TODO - check
		save("basic-abox-v2");
	}

	
	/**
	 * tests generation of a set of instances for glycolysis based on an axiom
	 * 
	 * has_part some (a1 and activates some (..(..(..))))
	 * 
	 * @throws OWLOntologyCreationException
	 * @throws OWLOntologyStorageException
	 * @throws IOException
	 */
	@Test
	public void testGenerateGlycolysis() throws OWLOntologyCreationException, OWLOntologyStorageException, IOException {
		m = OWLManager.createOWLOntologyManager();
		OWLOntology tbox = m.loadOntologyFromOntologyDocument(getResource("glycolysis-tbox.omn"));
		mmg = new MinimalModelGenerator(tbox, tbox, new org.semanticweb.HermiT.Reasoner.ReasonerFactory());
		OWLClass c = 
				tbox.getOWLOntologyManager().getOWLDataFactory().getOWLClass(IRI.create("http://purl.obolibrary.org/obo/GO_0006096"));

		mmg.generateNecessaryIndividuals(c);
		mmg.normalizeDirections(mmg.getOWLDataFactory().getOWLObjectProperty(
				OBOUpperVocabulary.BFO_part_of.getIRI()));
		
		this.expectedOPAs("glyc", 15);
		
		mmg.extractModule();
		//mmg.generateNecessaryIndividuals(getClass("foot"), true);
		save("glycolysis-tbox2abox");
	}
	
	/**
	 * DNA replication
	 * 
	 * @throws OWLOntologyCreationException
	 * @throws OWLOntologyStorageException
	 * @throws IOException
	 */
	@Test
	public void testGenerateDNAReplication() throws OWLOntologyCreationException, OWLOntologyStorageException, IOException {
		m = OWLManager.createOWLOntologyManager();
		OWLOntology tbox = m.loadOntologyFromOntologyDocument(getResource("dna-replication-tbox.owl"));
		mmg = new MinimalModelGenerator(tbox, tbox, new ElkReasonerFactory());
		LOG.info("MMG = "+mmg);
		OWLClass c = 
				tbox.getOWLOntologyManager().getOWLDataFactory().getOWLClass(IRI.create("http://purl.obolibrary.org/obo/GO_0006261"));

		LOG.info("Generating for :"+c);
		mmg.generateNecessaryIndividuals(c);
		mmg.normalizeDirections(mmg.getOWLDataFactory().getOWLObjectProperty(
				OBOUpperVocabulary.BFO_part_of.getIRI()));
		
		//this.expectedOPAs("glyc", 15);
		
		mmg.extractModule();
		//mmg.generateNecessaryIndividuals(getClass("foot"), true);
		save("dna-replication-tbox2abox");
	}
	
	
	/**
	 * tests {@link MinimalModelGenerator#generateNecessaryIndividuals(OWLClassExpression)}
	 * 
	 * @throws OWLOntologyCreationException
	 * @throws OWLOntologyStorageException
	 * @throws IOException
	 */
	@Test
	public void testGeneratePathway() throws OWLOntologyCreationException, OWLOntologyStorageException, IOException {
		m = OWLManager.createOWLOntologyManager();
		OWLOntology tbox = m.loadOntologyFromOntologyDocument(getResource("basic-tbox.omn"));
		mmg = new MinimalModelGenerator(tbox, new org.semanticweb.HermiT.Reasoner.ReasonerFactory());
		//mmg.setPrecomputePropertyClassCombinations(false);
		OWLClass c = getClass("bar_response_pathway");
		mmg.generateNecessaryIndividuals(c, true);
		// TODO - check
		save("pathway-abox");

		mmg.normalizeDirections(partOf());
		mmg.normalizeDirections(getObjectProperty(getIRI("activates")));
		
		Set<OWLClass> occs = new HashSet<OWLClass>();
		occs.add(getClass(OBOUpperVocabulary.GO_molecular_function));
		occs.add(getClass(OBOUpperVocabulary.GO_biological_process));
		mmg.anonymizeIndividualsNotIn(occs);

		this.expectFact("mapkkk_activity-proto", "activates", "mapkk_activity-proto");
		this.expectFact("mapkk_activity-proto", "activates", "mapk_activity-proto");
		this.expectedIndividiuals("cellular_process", 5);
		
		this.expectedOPAs("all", 20);
		
		mmg.extractModule();
		// futzing
//		m.addAxioms(mmg.getAboxOntology(), tbox.getAxioms());
//		Set<OWLOntology> onts = new HashSet<OWLOntology>();
//		onts.add(tbox);
//		onts.add(mmg.getAboxOntology());
//		OWLOntology mont = m.createOntology(IRI.create("hhtp://x.org/merged"), onts);
		save("pathway-abox-merged");
	}

	

	/**
	 * Tests {@link MinimalModelGenerator#getMostSpecificClassExpression(OWLNamedIndividual)}
	 * 
	 * @throws OWLOntologyCreationException
	 * @throws OWLOntologyStorageException
	 * @throws IOException
	 */
	@Test
	public void testMSC() throws OWLOntologyCreationException, OWLOntologyStorageException, IOException {
		m = OWLManager.createOWLOntologyManager();
		OWLOntology tbox = m.loadOntologyFromOntologyDocument(getResource("pathway-abox.omn"));
		mmg = new MinimalModelGenerator(tbox, new org.semanticweb.HermiT.Reasoner.ReasonerFactory());
		OWLNamedIndividual i = getIndividual("pathway1");
		OWLClassExpression x = mmg.getMostSpecificClassExpression(i, null);
		LOG.info("MSCE:"+x); // TODO - roundtrip
		Set<OWLClass> sig = x.getClassesInSignature();
		LOG.info("|Sig|="+sig.size());
		assertEquals(4, sig.size());
		Set<OWLObjectProperty> psig = x.getObjectPropertiesInSignature();
		LOG.info("|PSig|="+psig.size());
		assertEquals(3, psig.size());
	}
	
	/**
	 * Tests {@link MinimalModelGenerator#getMostSpecificClassExpression(OWLNamedIndividual)}
	 * 
	 * @throws OWLOntologyCreationException
	 * @throws OWLOntologyStorageException
	 * @throws IOException
	 */
	@Test
	public void testMSCGlycolysis() throws OWLOntologyCreationException, OWLOntologyStorageException, IOException {
		m = OWLManager.createOWLOntologyManager();
		OWLOntology tbox = m.loadOntologyFromOntologyDocument(getResource("glycolysis-abox.omn"));
		mmg = new MinimalModelGenerator(tbox, new org.semanticweb.HermiT.Reasoner.ReasonerFactory());
		OWLNamedIndividual i = 
				tbox.getOWLOntologyManager().getOWLDataFactory().getOWLNamedIndividual(IRI.create("http://purl.obolibrary.org/obo/GLY_TEST_0000001"));
		ArrayList<OWLObjectProperty> propertySet = new ArrayList<OWLObjectProperty>();
		propertySet.add(getObjectProperty(oboIRI("directly_activates")));
		propertySet.add(getObjectProperty(oboIRI("BFO_0000051")));
		OWLClassExpression x = mmg.getMostSpecificClassExpression(i, propertySet);
		LOG.info("MSCE:"+x); // TODO - roundtrip this
		Set<OWLClass> sig = x.getClassesInSignature();
		LOG.info("|Sig|="+sig.size());
		assertEquals(11, sig.size());
		Set<OWLObjectProperty> psig = x.getObjectPropertiesInSignature();
		LOG.info("|PSig|="+psig.size());
		assertEquals(2, psig.size());
	}
	
	@Test
	public void testAllIndividuals() throws OWLOntologyCreationException, OWLOntologyStorageException, IOException {
		m = OWLManager.createOWLOntologyManager();
		OWLOntology tbox = m.loadOntologyFromOntologyDocument(getResource("anonClassAssertions.owl"));
		mmg = new MinimalModelGenerator(tbox);
		mmg.isStrict = true;
		mmg.generateAllNecessaryIndividuals();
//		OWLEntityRenamer renamer = new OWLEntityRenamer(m, 
//				Collections.singleton(mmg.getAboxOntology()));
//		List<OWLOntologyChange> chgs = new ArrayList<OWLOntologyChange>();
//		for (OWLNamedIndividual ind : tbox.getIndividualsInSignature(true)) {
//			//OWLDeclarationAxiom decl = m.getOWLDataFactory().getOWLDeclarationAxiom(ind);
//			//m.addAxiom(mmg.getAboxOntology(), decl);
//			for (OWLClassExpression cx : ind.getTypes(tbox)) {
//				OWLNamedIndividual j = 
//						mmg.generateNecessaryIndividuals(cx, false, false);
//				chgs.addAll(renamer.changeIRI(j.getIRI(), ind.getIRI()));
//				//m.addAxiom(m.getOWLDataFactory().getOWLO, axiom)
//			}
//		}
//		LOG.info("Changes = "+chgs.size());
//		m.applyChanges(chgs);
		save("foo");
	}
	
	
	@Test
	public void testTransitiveCycle2() throws OWLOntologyCreationException, OWLOntologyStorageException, IOException {
		m = OWLManager.createOWLOntologyManager();
		OWLOntology tbox = m.loadOntologyFromOntologyDocument(getResource("cycle.omn"));
		mmg = new MinimalModelGenerator(tbox);
		mmg.setAssertInverses(true); // NECESSARY FOR ELK

		
		OWLClass c = getClass("cyclic_molecule2");
		
		// for this test we leave the transitive reduction stee until later
		mmg.generateNecessaryIndividuals(c, true, false);
		
		// we expect the molecule plus 6 atoms = 7
		LOG.info("#inds (after adding CM):"+mmg.getGeneratedIndividuals().size());
		assertEquals(7, mmg.getGeneratedIndividuals().size());
		
		expectedIndividiuals("c", 6);
		// test deepening
		expectedIndividiuals("molecule", 1);
		
		expectFact("c1-proto", "part_of", "cyclic_molecule2-proto");
		expectFact("c1-proto", "connected_to", "c2-proto");
		expectFact("c6-proto", "connected_to", "c1-proto");

		OWLObjectProperty connectedTo = mmg.getOWLDataFactory().getOWLObjectProperty(this.getIRI("connected_to"));
		expectedOPAs("pre-reduced", 8);
		// we expect this to have no-effect; connected_to should not be reduced
		// as it contains cycles
		mmg.performTransitiveReduction(connectedTo);
		expectedOPAs("reduced", 8);

		mmg.normalizeDirections(connectedTo);
		//expectedOPAs("post-hand, reduced, normalized", 3);
		
		
		// for debugging
		save("cycle-abox-elk");
	}
	
//  This one can lead to cycles:	
//	@Test
//	public void testTransitiveCycle1() throws OWLOntologyCreationException, OWLOntologyStorageException, IOException {
//		m = OWLManager.createOWLOntologyManager();
//		OWLOntology tbox = m.loadOntologyFromOntologyDocument(getResource("cycle.omn"));
//		mmg = new MinimalModelGenerator(tbox);
//		mmg.setAssertInverses(true); // NECESSARY FOR ELK
//
//		
//		OWLClass c = getClass("cyclic_molecule");
//		
//		// for this test we leave the transitive reduction stee until later
//		mmg.generateNecessaryIndividuals(c, true, false);
//		
//		// we expect the molecule plus 6 atoms = 7
//		LOG.info("#inds (after adding CM):"+mmg.getGeneratedIndividuals().size());
//		assertEquals(7, mmg.getGeneratedIndividuals().size());
//		
//		expectedIndividiuals("c", 6);
//		// test deepening
//		expectedIndividiuals("molecule", 1);
//		
//		expectFact("c1-proto", "part_of", "cyclic_molecule2-proto");
//		expectFact("c1-proto", "connected_to", "c2-proto");
//		expectFact("c6-proto", "connected_to", "c1-proto");
//
//		OWLObjectProperty connectedTo = mmg.getOWLDataFactory().getOWLObjectProperty(this.getIRI("connected_to"));
//		expectedOPAs("pre-reduced", 8);
//		// we expect this to have no-effect; connected_to should not be reduced
//		// as it contains cycles
//		mmg.performTransitiveReduction(connectedTo);
//		expectedOPAs("reduced", 8);
//
//		mmg.normalizeDirections(connectedTo);
//		//expectedOPAs("post-hand, reduced, normalized", 3);
//		
//		
//		// for debugging
//		save("cycle-abox-elk");
//	}
	// UTIL
	
	private OWLObjectProperty partOf() {
		return mmg.getOWLDataFactory().getOWLObjectProperty(this.getIRI("part_of"));
	}

	
	protected void expectFact(String subj, String prop, String obj) {
		expectFact(getIndividual(subj), getObjectProperty(getIRI(prop)), getIndividual(obj));
	}
	
	protected void expectedIndividiuals(String cn, Integer size) {
		expectedIndividiuals(getClass(cn), size);
	}


	protected IRI getIRI(String frag) {
		return IRI.create("http://x.org/"+frag);
	}

	protected OWLClass getClass(String frag) {
		return getClass(getIRI(frag));
	}
	protected OWLNamedIndividual getIndividual(String frag) {
		return mmg.getTboxOntology().getOWLOntologyManager().getOWLDataFactory().getOWLNamedIndividual(getIRI(frag));
	}
	

}
