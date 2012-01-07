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
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
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
import owltools.io.ParserWrapper;
import owltools.mooncat.Mooncat;

/**
 * This is the main test class for PropertyViewOntologyBuilder
 * 
 * @author cjm
 *
 */
public class PropertyViewTest extends OWLToolsTestBasics {
	
	private Logger LOG = Logger.getLogger(PropertyViewTest.class);


	/**
	 * the data ontology consists of genes (as classes) and relationships to 
	 * class expressions using GO (e.g. g1 SubClassOf has_prototype some involved_in some P)
	 * 
	 * the ontology is a subset of GO
	 * 
	 * we build a view ontology using the has_attribute property
	 * 
	 * @throws IOException
	 * @throws OWLOntologyCreationException
	 * @throws OWLOntologyStorageException
	 */
	@Test
	public void testAnnotationPropertyView() throws IOException, OWLOntologyCreationException, OWLOntologyStorageException {
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

		pvob.buildViewOntology(IRI.create("http://x.org"), IRI.create("http://y.org"));
		OWLOntology avo = pvob.getAssertedViewOntology();
		OWLReasonerFactory rf = new ElkReasonerFactory();
		OWLReasoner reasoner = rf.createReasoner(avo);
		reasoner.precomputeInferences(InferenceType.values()); // ELK
		pvob.buildInferredViewOntology(reasoner);
		OWLOntology ivo = pvob.getInferredViewOntology();
		for (OWLAxiom a : ivo.getAxioms()) {
			System.out.println(a);
		}
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
		Map<String,Boolean> m = new HashMap<String,Boolean>();
		m.put("EquivalentClasses(<http://purl.obolibrary.org/obo/#left_autopod_view> <http://purl.obolibrary.org/obo/#right_autopod_view>)",
				false);
		
		OWLOntology ivo = pvob.getInferredViewOntology();
		for (OWLAxiom a : ivo.getAxioms()) {
			m.put(a.toString(), true);
			//if (a instanceof OWLSubClassOfAxiom)
				System.out.println("\""+a+"\"");
		}
		for (Boolean tv : m.values()) {
			assertTrue(false);
		}
	}

}
