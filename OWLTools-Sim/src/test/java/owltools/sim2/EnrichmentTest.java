package owltools.sim2;

import static org.junit.Assert.*;

import java.io.File;
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
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.OWLToolsTestBasics;
import owltools.graph.OWLGraphWrapper;
import owltools.io.OWLPrettyPrinter;
import owltools.io.ParserWrapper;
import owltools.io.TableToAxiomConverter;
import owltools.mooncat.TransformationUtils;
import owltools.sim2.SimpleOwlSim;
import owltools.sim2.preprocessor.AutomaticSimPreProcessor;
import owltools.vocab.OBOUpperVocabulary;

/**
 * This is the main test class for PropertyViewOntologyBuilder
 * 
 * @author cjm
 *
 */
public class EnrichmentTest extends AbstractOWLSimTest {

	private Logger LOG = Logger.getLogger(EnrichmentTest.class);


	@Test
	public void enrichmentTestGO() throws Exception, MathException {
		ParserWrapper pw = new ParserWrapper();
		sourceOntol = pw.parseOBO(getResourceIRIString("go-subset-t1.obo"));
		OWLObjectProperty INVOLVED_IN = OBOUpperVocabulary.RO_involved_in.getObjectProperty(sourceOntol);
		Map<OWLClass, OWLClass> qmap =
				TransformationUtils.createObjectPropertyView(sourceOntol, sourceOntol,
						INVOLVED_IN, null, true);
		LOG.info("VIEW SIZE"+qmap.size());
		sourceOntol.getOWLOntologyManager().saveOntology(sourceOntol, IRI.create(new File("target/foo.owl")));

		g = new OWLGraphWrapper(sourceOntol);
		//IRI vpIRI = g.getOWLObjectPropertyByIdentifier("GOTESTREL:0000001").getIRI();

		TableToAxiomConverter ttac = new TableToAxiomConverter(g);
		ttac.config.axiomType = AxiomType.CLASS_ASSERTION;
		//ttac.config.property = vpIRI; //TODO
		ttac.config.property = INVOLVED_IN.getIRI();
		ttac.config.isSwitchSubjectObject = true;
		ttac.parse("src/test/resources/simplegaf-t1.txt");
		// assume buffering
		//OWLReasoner reasoner = new ElkReasonerFactory().createReasoner(sourceOntol);

		g.getManager().removeAxioms(sourceOntol,
				sourceOntol.getAxioms(AxiomType.DISJOINT_CLASSES));

		try {
			OWLPrettyPrinter pp = new OWLPrettyPrinter(g);

			createOwlSim();
			LOG.info("ont = "+owlsim.getSourceOntology());
			LOG.info("r = "+owlsim.getReasoner());

			owlsim.createElementAttributeMapFromOntology();

			for (OWLNamedIndividual ind : sourceOntol.getIndividualsInSignature()) {
				System.out.println(ind);
				for (OWLClass c : owlsim.getReasoner().getTypes(ind, true).getFlattened()) {
					System.out.println("  T:"+c);
				}
				for (OWLClassExpression c : ind.getTypes(sourceOntol)) {
					System.out.println("  T(Asserted):"+c);

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
			owlsim.setEnrichmentConfig(ec);
			OWLClass vc1 = qmap.get(rc1);
			OWLClass vc2 = qmap.get(rc2);
			int n = 0;
			List<EnrichmentResult> results = owlsim.calculateAllByAllEnrichment(pc, vc1, vc2);
			System.out.println("Results: "+rc1+" "+rc2);
			for (EnrichmentResult result : results) {
				System.out.println("R="+render(result,pp));
				n++;
			}
			assertTrue(n > 0);
			owlsim.showTimings();
		}
		finally {
			owlsim.getReasoner().dispose();
		}

	}




}
