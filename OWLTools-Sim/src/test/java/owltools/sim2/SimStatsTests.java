package owltools.sim2;

import java.io.IOException;

import org.apache.commons.math.MathException;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.obolibrary.oboformat.parser.OBOFormatParserException;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.graph.OWLGraphWrapper;
import owltools.graph.OWLGraphWrapperEdges;
import owltools.io.OWLPrettyPrinter;
import owltools.io.ParserWrapper;
import owltools.sim2.AbstractOWLSimTest;
import owltools.sim2.SimpleOwlSim;
import owltools.sim2.UnknownOWLClassException;
import owltools.sim2.io.FormattedRenderer;

/**
 * This is the main test class for SimStats
 * 
 * @author nlw
 *
 */
public class SimStatsTests extends AbstractOWLSimTest {

	private Logger LOG = Logger.getLogger(SimStatsTests.class);
	
	@Test
	public void testStats() throws IOException, OWLOntologyCreationException, OWLOntologyStorageException, MathException, UnknownOWLClassException {
		ParserWrapper pw = new ParserWrapper();
		sourceOntol = pw.parseOWL(getResourceIRIString("sim/mp-subset-1.obo"));
		g =  new OWLGraphWrapper(sourceOntol);
		parseAssociations(getResource("sim/mgi-gene2mp-subset-1.tbl"), g);
		setOutput("target/basic-owlsim-test.out");

		owlpp = new OWLPrettyPrinter(g);

		// assume buffering
		OWLReasoner reasoner = new ElkReasonerFactory().createReasoner(sourceOntol);
		try {
			this.createOwlSim();
			owlsim.createElementAttributeMapFromOntology();
			
			reasoner.flush();
			owlsim.computeSystemStats();
			LOG.info("Overall statistical summary for Test:");
			LOG.info(owlsim.getSystemStats().toString());
			LOG.info("Averaged statistical summary (Mean) for Test:");
			LOG.info(owlsim.getSummaryStatistics(1).toString());
			LOG.info("Averaged statistical summary (Max) for Test:");
			LOG.info(owlsim.getSummaryStatistics(4).toString());

//			LOG.info("mean(maxIC):"+owlsim.getSummaryStatistics(4).getMean());
//			LOG.info("max(maxIC):"+owlsim.getSummaryStatistics(4).getMax());
//			LOG.info("mean(sumIC):"+owlsim.getSummaryStatistics(2).getMean());
			LOG.info("Averaged statistical summary (Sum) for Test:");
			LOG.info(owlsim.getSummaryStatistics(2).toString());

		}
		finally {
			reasoner.dispose();
		}
	}

}
