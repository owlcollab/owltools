package owltools.sim;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;

import org.apache.commons.math.MathException;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

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
		ttac.config.property = g.getOWLObjectPropertyByIdentifier("GOTESTREL:0000001").getIRI();
		ttac.config.isSwitchSubjectObject = true;
		ttac.parse("src/test/resources/simplegaf-t1.txt");

		OWLPrettyPrinter pp = new OWLPrettyPrinter(g);

		sos = new SimpleOwlSim(sourceOntol);
		sos.createElementAttributeMapFromOntology();
		sos.removeUnreachableAxioms();

		OWLClass rc1 = get("biological_process");
		OWLClass rc2 = get("cellular_component");
		OWLClass pc = g.getDataFactory().getOWLThing();

		EnrichmentConfig ec = new EnrichmentConfig();
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
