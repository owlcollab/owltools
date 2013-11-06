package owltools.sim2;

import static org.junit.Assert.*;

import java.util.List;

import org.apache.commons.math.MathException;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.OWLToolsTestBasics;
import owltools.graph.OWLGraphWrapper;
import owltools.io.OWLPrettyPrinter;
import owltools.io.ParserWrapper;
import owltools.io.TableToAxiomConverter;
import owltools.sim2.SimpleOwlSim;
import owltools.sim2.preprocessor.AutomaticSimPreProcessor;

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
	public void enrichmentTestGO() throws Exception, MathException {
		ParserWrapper pw = new ParserWrapper();
		sourceOntol = pw.parseOBO(getResourceIRIString("go-subset-t1.obo"));
		g = new OWLGraphWrapper(sourceOntol);
		IRI vpIRI = g.getOWLObjectPropertyByIdentifier("GOTESTREL:0000001").getIRI();
		TableToAxiomConverter ttac = new TableToAxiomConverter(g);
		ttac.config.axiomType = AxiomType.CLASS_ASSERTION;
		ttac.config.property = vpIRI;
		ttac.config.isSwitchSubjectObject = true;
		ttac.parse("src/test/resources/simplegaf-t1.txt");
		// assume buffering
		OWLReasoner reasoner = new ElkReasonerFactory().createReasoner(sourceOntol);
		try {
			OWLPrettyPrinter pp = new OWLPrettyPrinter(g);

			sos = new SimpleOwlSim(sourceOntol);

			AutomaticSimPreProcessor pproc = new AutomaticSimPreProcessor();
			pproc.setInputOntology(sourceOntol);
			pproc.setOutputOntology(sourceOntol);
			pproc.setReasoner(reasoner); // TODO - share

			sos.setSimPreProcessor(pproc);
			//sos.preprocess();
			pproc.preprocess();

			sos.createElementAttributeMapFromOntology();

			for (OWLNamedIndividual ind : sourceOntol.getIndividualsInSignature()) {
				System.out.println(ind);
				for (OWLClass c : reasoner.getTypes(ind, true).getFlattened()) {
					System.out.println("  T:"+c);
				}
			}
			//System.exit(0);

			//sos.addViewProperty(vpIRI);
			//sos.generatePropertyViews();
			//sos.saveOntology("/tmp/foo.owl");


			OWLClass rc1 = get("biological_process");
			OWLClass rc2 = get("cellular_component");
			OWLClass pc = g.getDataFactory().getOWLThing();

			EnrichmentConfig ec = new EnrichmentConfig();
			ec.pValueCorrectedCutoff = 0.05;
			ec.attributeInformationContentCutoff = 3.0;
			sos.setEnrichmentConfig(ec);

			int n = 0;
			for (OWLClass vrc1 : pproc.getViewClasses(rc1)) {
				for (OWLClass vrc2 : pproc.getViewClasses(rc2)) {
					List<EnrichmentResult> results = sos.calculateAllByAllEnrichment(pc, vrc1, vrc2);
					System.out.println("Results: "+vrc1+" "+vrc2);
					for (EnrichmentResult result : results) {
						System.out.println(render(result,pp));
						n++;
					}
				}
			}
			assertTrue(n > 0);
		}
		finally {
			reasoner.dispose();
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
