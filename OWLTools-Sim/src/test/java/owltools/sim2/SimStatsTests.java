package owltools.sim2;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math.MathException;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.obolibrary.oboformat.parser.OBOFormatParserException;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.OWLClass;
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
import owltools.sim2.scores.ElementPairScores;

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
	
	@Test
	public void testIxI() throws IOException, OWLOntologyCreationException, MathException, UnknownOWLClassException, OBOFormatParserException {
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
			owlsim.calculateMetricStats(owlsim.getAllElements(),owlsim.getAllElements());

			String[] metrics = {"bmaAsymIC","bmaSymIC","bmaInverseAsymIC", "combinedScore", "simJ", "simGIC","maxIC"};
			for (String m : metrics) {
				LOG.info("Test Summary(mean) for "+m+": "+owlsim.getMetricStats(Stat.MEAN).get(m).getSummary());
				LOG.info("Test Summary(min) for "+m+": "+owlsim.getMetricStats(Stat.MIN).get(m).getSummary());
				LOG.info("Test Summary(max) for "+m+": "+owlsim.getMetricStats(Stat.MAX).get(m).getSummary());
			}
		} finally {
			reasoner.dispose();
		}
		
	}
	
}
