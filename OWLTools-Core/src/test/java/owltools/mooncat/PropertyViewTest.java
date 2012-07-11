package owltools.mooncat;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import owltools.OWLToolsTestBasics;
import owltools.graph.OWLGraphWrapper;
import owltools.io.OWLPrettyPrinter;
import owltools.io.ParserWrapper;

/**
 * This is the main test class for PropertyViewOntologyBuilder
 * 
 * @author cjm
 *
 */
public class PropertyViewTest extends OWLToolsTestBasics {

	private Logger LOG = Logger.getLogger(PropertyViewTest.class);


	/**
	 * setup:
	 * 
	 * SourceOntology
	 * O = (a subset of) GO, merged in with a relations ontology
	 * 
	 * elementsOntology
	 * E = {gene1, gene2, ..., gene1 SubClassOf R some GO_classA, ....}
	 * genes are represented as classes.  relationships to 
	 * class expressions using GO (e.g. g1 SubClassOf has_prototype some involved_in some P)
	 * [this may not be the final form used in GO]
	 * 
	 * property
	 * P = has_attribute [GORELTEST:0000001]
	 * this is a promiscuous property that has property chains defined such that it will
	 * connect genes to GO processes plus the processes which they are part of
	 * 
	 * We expect the resulting ontology O(P,E)' to classify the genes
	 * in a subsumption hierarchy that masks the partOf relations in O
	 * 
	 */
	@Test
	public void testGeneAssociationPropertyView() throws IOException, OWLOntologyCreationException, OWLOntologyStorageException {
		ParserWrapper pw = new ParserWrapper();
		OWLGraphWrapper g = pw.parseToOWLGraph(getResourceIRIString("test_gene_association_mgi_gaf.owl"));
		OWLOntology relOnt = pw.parseOWL(getResourceIRIString("go-annot-rel.owl"));
		g.mergeOntology(relOnt);
		OWLOntology sourceOntol = pw.parseOBO(getResourceIRIString("test_go_for_mgi_gaf.obo"));
		g.addSupportOntology(sourceOntol);
		OWLOntology annotOntol = g.getSourceOntology();

		OWLObjectProperty viewProperty = g.getOWLObjectPropertyByIdentifier("GORELTEST:0000001");

		PropertyViewOntologyBuilder pvob = 
			new PropertyViewOntologyBuilder(sourceOntol,
					annotOntol,
					viewProperty);
		pvob.setAssumeOBOStyleIRIs(true); // this is default but we assert anyway
		pvob.setViewLabelPrefixAndSuffix("", " gene");
		pvob.buildViewOntology(IRI.create("http://x.org"), IRI.create("http://y.org"));
		OWLOntology avo = pvob.getAssertedViewOntology();
		OWLReasonerFactory rf = new ElkReasonerFactory();
		OWLReasoner reasoner = rf.createReasoner(avo);
		reasoner.precomputeInferences(InferenceType.values()); // ELK
		pvob.buildInferredViewOntology(reasoner);
		OWLOntology ivo = pvob.getInferredViewOntology();
		g = new OWLGraphWrapper(ivo);
		OWLPrettyPrinter pp = new OWLPrettyPrinter(g);

		for (OWLAxiom a : ivo.getAxioms()) {
			LOG.info("GO: "+pp.render(a));
		}

		pw.saveOWL(pvob.getAssertedViewOntology(), "file:///tmp/zz.owl", g);

		pw.saveOWL(ivo, "file:///tmp/z.owl", g);
	}

	@Test
	public void testSimpleView() throws IOException, OWLOntologyCreationException, OWLOntologyStorageException {
		ParserWrapper pw = new ParserWrapper();
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLDataFactory df = manager.getOWLDataFactory();
		OWLOntology sourceOntol = pw.parseOWL(getResourceIRIString("facet_view_input.owl"));

		OWLObjectProperty viewProperty = 
			df.getOWLObjectProperty(IRI.create("http://purl.obolibrary.org/obo/RO_0002206"));

		PropertyViewOntologyBuilder pvob = 
			new PropertyViewOntologyBuilder(sourceOntol,
					manager.createOntology(),
					viewProperty);
		pvob.setAssumeOBOStyleIRIs(false);

		pvob.buildViewOntology(IRI.create("http://x.org"), IRI.create("http://y.org"));
		OWLOntology avo = pvob.getAssertedViewOntology();
		for (OWLAxiom a : avo.getAxioms()) {
			LOG.info("ASSERTED_VIEW_ONT: "+a);
		}
		
		OWLReasonerFactory rf = new ElkReasonerFactory();
		OWLReasoner reasoner = rf.createReasoner(avo);
		reasoner.precomputeInferences(InferenceType.values()); // ELK
		pvob.buildInferredViewOntology(reasoner);
		for (OWLEntity e : pvob.getViewEntities()) {
			LOG.info(" VE: "+e);
		}

		// TODO - less dumb way
		Set<String> m = new HashSet<String>();
		m.add("EquivalentClasses(<http://purl.obolibrary.org/obo/#left_autopod-RO_0002206> <http://purl.obolibrary.org/obo/#right_autopod-RO_0002206> )");


		OWLOntology ivo = pvob.getInferredViewOntology();
		for (OWLAxiom a : ivo.getAxioms()) {
			if (a instanceof OWLEquivalentClassesAxiom) {
				System.out.println("\""+a+"\"");
			}
			if (m.contains(a.toString())) {
				System.out.println("FOUND: \""+a+"\"");
				m.remove(a.toString());
			}
			//m.put(a.toString(), true);
			//if (a instanceof OWLSubClassOfAxiom)
			//	System.out.println("\""+a+"\"");
		}
		assertEquals(0, m.size());
	}

	@Test
	public void testPhenoView() throws IOException, OWLOntologyCreationException, OWLOntologyStorageException {
		ParserWrapper pw = new ParserWrapper();
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLDataFactory df = manager.getOWLDataFactory();
		OWLOntology sourceOntol = pw.parseOWL(getResourceIRIString("q-in-e.omn"));

		OWLObjectProperty viewProperty = 
			df.getOWLObjectProperty(IRI.create("http://x.org#related_to"));

		PropertyViewOntologyBuilder pvob = 
			new PropertyViewOntologyBuilder(sourceOntol,
					sourceOntol,
					viewProperty);
		pvob.setAssumeOBOStyleIRIs(false);

		pvob.buildViewOntology(IRI.create("http://x.org"), IRI.create("http://y.org"));
		OWLOntology avo = pvob.getAssertedViewOntology();
		for (OWLAxiom a : avo.getAxioms()) {
			LOG.info("ASSERTED_VIEW_ONT: "+a);
		}

		OWLReasonerFactory rf = new ElkReasonerFactory();
		OWLReasoner reasoner = rf.createReasoner(avo);
		reasoner.precomputeInferences(InferenceType.values()); // ELK
		LOG.info("Building inferred view");
		pvob.buildInferredViewOntology(reasoner);
		for (OWLEntity e : pvob.getViewEntities()) {
			LOG.info(" VE: "+e);
		}
		for (OWLAxiom a : pvob.getInferredViewOntology().getAxioms()) {
			LOG.info(" VA: "+a);
		}
		


	}


	@Test
	public void testPhenoViewWithIndividuals() throws IOException, OWLOntologyCreationException, OWLOntologyStorageException {
		ParserWrapper pw = new ParserWrapper();
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLDataFactory df = manager.getOWLDataFactory();
		OWLOntology sourceOntol = pw.parseOWL(getResourceIRIString("q-in-e.omn"));

		OWLObjectProperty viewProperty = 
			df.getOWLObjectProperty(IRI.create("http://x.org#has_phenotype_involving"));

		OWLOntology elementsOntology = sourceOntol;

		PropertyViewOntologyBuilder pvob = 
			new PropertyViewOntologyBuilder(sourceOntol,
					elementsOntology,
					viewProperty);

		pvob.setClassifyIndividuals(true);
		pvob.setFilterUnused(true);
		pvob.setViewLabelPrefix("involves ");
		pvob.setViewLabelSuffix("");
		pvob.setAssumeOBOStyleIRIs(false);
		//pvob.setUseOriginalClassIRIs(true);

		pvob.buildViewOntology(IRI.create("http://x.org"), IRI.create("http://y.org"));
		OWLOntology avo = pvob.getAssertedViewOntology();
		for (OWLAxiom a : avo.getAxioms()) {
			LOG.info("ASSERTED_VIEW_ONT: "+a);
		}

		OWLReasonerFactory rf;
		// we use hermit as elk does not classify individuals yet
		rf = new org.semanticweb.HermiT.Reasoner.ReasonerFactory();

		OWLReasoner reasoner = rf.createReasoner(avo);
		reasoner.precomputeInferences(InferenceType.values()); 

		OWLGraphWrapper g = new OWLGraphWrapper(pvob.getInferredViewOntology());
		OWLPrettyPrinter pp = new OWLPrettyPrinter(g);
		g.addSupportOntology(pvob.getAssertedViewOntology());
		LOG.info("Building inferred view");
		pvob.buildInferredViewOntology(reasoner);
		boolean ok1 = false;
		int numClassAssertions = 0;
		for (OWLEntity e : pvob.getViewEntities()) {
			LOG.info(" VE: "+e+" LABEL:"+g.getLabel(e));
			if (e instanceof OWLClass) {
				if (g.getLabel(e) != null && g.getLabel(e).equals("involves limb")) {
					ok1 = true;
				}
			}
			else {
				if (e instanceof OWLNamedIndividual) {
					Set<OWLClassAssertionAxiom> caas = pvob.getInferredViewOntology().getClassAssertionAxioms((OWLNamedIndividual) e);
					for (OWLClassAssertionAxiom caa : caas) {
						LOG.info("  CAA:"+pp.render(caa));
						numClassAssertions++;
					}	
				}

			}
		}
		assertTrue(ok1);
		LOG.info("class assertions:"+numClassAssertions);
		assertEquals(12, numClassAssertions); // TODO - CHECK
		LOG.info(pvob.getViewEntities().size());
		assertEquals(23, pvob.getViewEntities().size());


	}

	@Test
	public void testPhenoViewWithIndividualsTranslated() throws IOException, OWLOntologyCreationException, OWLOntologyStorageException {
		ParserWrapper pw = new ParserWrapper();
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLDataFactory df = manager.getOWLDataFactory();
		OWLOntology sourceOntol = pw.parseOWL(getResourceIRIString("q-in-e.omn"));

		OWLObjectProperty viewProperty = 
			df.getOWLObjectProperty(IRI.create("http://x.org#has_phenotype_involving"));

		// in this test, the individuals are in the source ontology
		OWLOntology elementsOntology = sourceOntol;

		PropertyViewOntologyBuilder pvob = 
			new PropertyViewOntologyBuilder(sourceOntol,
					elementsOntology,
					viewProperty);

		pvob.translateABoxToTBox();

		pvob.setClassifyIndividuals(false);
		pvob.setFilterUnused(true);
		pvob.setViewLabelPrefix("involves ");
		pvob.setAssumeOBOStyleIRIs(false);
		//pvob.setUseOriginalClassIRIs(true);

		pvob.buildViewOntology(IRI.create("http://x.org"), IRI.create("http://y.org"));
		OWLOntology avo = pvob.getAssertedViewOntology();
		for (OWLAxiom a : avo.getAxioms()) {
			LOG.info("ASSERTED_VIEW_ONT: "+a);
		}

		OWLReasonerFactory rf;
		rf = new ElkReasonerFactory();

		OWLReasoner reasoner = rf.createReasoner(avo);
		reasoner.precomputeInferences(InferenceType.values()); 

		OWLGraphWrapper g = new OWLGraphWrapper(pvob.getInferredViewOntology());
		OWLPrettyPrinter pp = new OWLPrettyPrinter(g);
		g.addSupportOntology(pvob.getAssertedViewOntology());
		LOG.info("Building inferred view");
		pvob.buildInferredViewOntology(reasoner);
		
		g = new OWLGraphWrapper(pvob.getAssertedViewOntology());

		pw.saveOWL(pvob.getAssertedViewOntology(), "file:///tmp/zz.owl", g);

		Set<OWLSubClassOfAxiom> scas = pvob.getInferredViewOntology().getAxioms(AxiomType.SUBCLASS_OF);
		for (OWLSubClassOfAxiom sca : scas) {
			LOG.info("IVO AXIOM: "+sca);
		}

		
		boolean ok1 = false;
		int numClassifications = 0;
		for (OWLEntity e : pvob.getViewEntities()) {
			LOG.info(" VE: "+e+" LABEL:"+g.getLabel(e));
			if (e instanceof OWLClass) {
				if (g.getLabel(e) != null && g.getLabel(e).equals("involves limb")) {
					ok1 = true;
				}
				if (pvob.getElementsOntology().getClassesInSignature().contains(e)) {
					LOG.info(" Class:"+pp.render(e));
					Set<OWLClassExpression> supers = ((OWLClass) e).getSuperClasses(pvob.getInferredViewOntology());
					for (OWLClassExpression sup : supers) {
						LOG.info("  SubClassOf:"+pp.render(sup));
						numClassifications++;
					}	
				}
			}
		}
		assertTrue(ok1);
		LOG.info("class assertions:"+numClassifications);
		assertEquals(12, numClassifications); // TODO - CHECK
		LOG.info(pvob.getViewEntities().size());
		assertEquals(23, pvob.getViewEntities().size());
		


	}



}
