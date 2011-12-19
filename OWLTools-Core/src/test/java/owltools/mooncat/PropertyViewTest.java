package owltools.mooncat;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
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
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
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
	public void testBuildInferredPropertyView() throws IOException, OWLOntologyCreationException, OWLOntologyStorageException {
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

}
