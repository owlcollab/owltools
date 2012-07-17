package owltools.sim;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math.MathException;
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
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import owltools.OWLToolsTestBasics;
import owltools.graph.OWLGraphWrapper;
import owltools.io.OWLPrettyPrinter;
import owltools.io.ParserWrapper;
import owltools.io.TableToAxiomConverter;
import owltools.sim.SimpleOwlSim.EnrichmentConfig;
import owltools.sim.SimpleOwlSim.EnrichmentResult;

/**
 * This is the main test class for PropertyViewOntologyBuilder
 * 
 * @author cjm
 *
 */
public class EnrichmentTest extends OWLToolsTestBasics {

	private Logger LOG = Logger.getLogger(EnrichmentTest.class);
	OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
	OWLDataFactory df = manager.getOWLDataFactory();
	OWLOntology sourceOntol;
	SimpleOwlSim sos;
	OWLGraphWrapper g;



	@Test
	public void testOwlSimPheno() throws IOException, OWLOntologyCreationException, OWLOntologyStorageException, MathException {
		ParserWrapper pw = new ParserWrapper();
		sourceOntol = pw.parseOBO(getResourceIRIString("go-subset-t1.obo"));
		g = new OWLGraphWrapper(sourceOntol);
		TableToAxiomConverter ttac = new TableToAxiomConverter(g);
		ttac.config.axiomType = AxiomType.CLASS_ASSERTION;
		ttac.config.isSwitchSubjectObject = true;
		ttac.parse("src/test/resources/simplegaf-t1.txt");

		OWLPrettyPrinter pp = new OWLPrettyPrinter(g);

		sos = new SimpleOwlSim(sourceOntol);
		sos.createElementAttributeMapFromOntology();
		sos.removeUnreachableAxioms();

		OWLClass rc1 = get("biological_process");
		OWLClass rc2 = get("cellular_component");
		OWLClass pc = g.getDataFactory().getOWLThing();

		EnrichmentConfig ec = sos.new EnrichmentConfig();
		ec.pValueCorrectedCutoff = 0.05;
		ec.attributeInformationContentCutoff = 3.0;
		sos.setEnrichmentConfig(ec);
		List<EnrichmentResult> results = sos.calculateAllByAllEnrichment(pc, rc1, rc2);
		for (EnrichmentResult result : results) {
			System.out.println(render(result,pp));
		}


	}

	private String render(EnrichmentResult r, OWLPrettyPrinter pp) {
		return pp.render(r.sampleSetClass) +" "+ pp.render(r.enrichedClass)
		+" "+ r.pValue +" "+ r.pValueCorrected;
	}

	private OWLClass get(String label) {
		return (OWLClass)g.getOWLObjectByLabel(label);
	}





}
