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
import owltools.sim2.AbstractOwlSim.Stat;
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
	public void testStats() throws IOException, OWLOntologyCreationException, MathException, UnknownOWLClassException, OBOFormatParserException {
		ParserWrapper pw = new ParserWrapper();
		sourceOntol = pw.parseOBO(getResource("sim/mp-subset-1.obo").getAbsolutePath());
		g =  new OWLGraphWrapper(sourceOntol);
		parseAssociations(getResource("sim/mgi-gene2mp-subset-1.tbl"), g);
		setOutput("target/basic-owlsim-test.out");

		// assume buffering
		OWLReasoner reasoner = new ElkReasonerFactory().createReasoner(sourceOntol);
		try {
			this.createOwlSim();
			owlsim.createElementAttributeMapFromOntology();
			
			reasoner.flush();
			owlsim.computeSystemStats();
			LOG.info("Overall statistical summary for Test:");
			LOG.info(owlsim.getSystemStats().toString());
			LOG.info("Averaged statistical summary for Individuals in Test:");
//			LOG.info(owlsim.getSummaryStatistics(Stat.MEAN).toString());
			LOG.info("individuals: "+owlsim.getSummaryStatistics(Stat.N).getN());
			LOG.info("mean(n/indiv): "+String.format("%1$.5f", owlsim.getSummaryStatistics(Stat.N).getMean()));
			LOG.info("mean(meanIC): "+String.format("%1$.5f", owlsim.getSummaryStatistics(Stat.MEAN).getMean()));
			LOG.info("mean(maxIC): "+String.format("%1$.5f", owlsim.getSummaryStatistics(Stat.MAX).getMean()));
			LOG.info("max(maxIC): "+String.format("%1$.5f", owlsim.getSummaryStatistics(Stat.MAX).getMax()));
			LOG.info("mean(sumIC): "+String.format("%1$.5f", owlsim.getSummaryStatistics(Stat.SUM).getMean()));
		}
		finally {
			reasoner.dispose();
		}
	}

}
