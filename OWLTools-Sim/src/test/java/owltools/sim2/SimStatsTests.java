package owltools.sim2;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.math.MathException;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.obolibrary.oboformat.parser.OBOFormatParserException;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;
import owltools.sim2.OwlSim.Stat;
import owltools.sim2.OwlSim.StatsPerIndividual;

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
			LOG.info("individuals: "+owlsim.getSummaryStatistics().n.getN());
			LOG.info("mean(n/indiv): "+String.format("%1$.5f", owlsim.getSummaryStatistics().n.getMean()));
			LOG.info("mean(meanIC): "+String.format("%1$.5f", owlsim.getSummaryStatistics().mean.getMean()));
			LOG.info("mean(maxIC): "+String.format("%1$.5f", owlsim.getSummaryStatistics().max.getMean()));
			LOG.info("max(maxIC): "+String.format("%1$.5f", owlsim.getSummaryStatistics().max.getMax()));
			LOG.info("mean(sumIC): "+String.format("%1$.5f", owlsim.getSummaryStatistics().max.getMean()));
		}
		finally {
			reasoner.dispose();
		}
	}
	
	@Test
	public void testSubgraphStats() throws IOException, OWLOntologyCreationException, MathException, UnknownOWLClassException, OBOFormatParserException {
		ParserWrapper pw = new ParserWrapper();
		sourceOntol = pw.parseOBO(getResource("sim/mp-subset-1.obo").getAbsolutePath());
		g =  new OWLGraphWrapper(sourceOntol);
		parseAssociations(getResource("sim/mgi-gene2mp-subset-1.tbl"), g);
		setOutput("target/basic-owlsim-test.out");

		Set<String> upperLevelClassIDs = new HashSet<String>();
		upperLevelClassIDs.add("MP:0000001"); //root, should be the same as overall
		upperLevelClassIDs.add("MP:0000003"); //Adipose Tissue Morphology
		upperLevelClassIDs.add("MP:0001544"); //Abnormal Blood morphology
		upperLevelClassIDs.add("MP:0002160"); //Abnormal Reproductive System Morphology
		upperLevelClassIDs.add("MP:0002152"); //Abnormal Brain Morphology
		upperLevelClassIDs.add("MP:0003631"); //Nervous System Phenotype
		// assume buffering
		OWLReasoner reasoner = new ElkReasonerFactory().createReasoner(sourceOntol);
		try {
			this.createOwlSim();
			owlsim.createElementAttributeMapFromOntology();
			
			reasoner.flush();
			owlsim.computeSystemStats();			
			LOG.info("Overall statistical summary for Test:");
			LOG.info(owlsim.getSystemStats().toString());

			LOG.info("individuals: "+owlsim.getSummaryStatistics().n.getN());
			LOG.info("mean(n/indiv): "+String.format("%1$.5f", owlsim.getSummaryStatistics().n.getMean()));
			LOG.info("mean(meanIC): "+String.format("%1$.5f", owlsim.getSummaryStatistics().mean.getMean()));
			LOG.info("mean(maxIC): "+String.format("%1$.5f", owlsim.getSummaryStatistics().max.getMean()));
			LOG.info("max(maxIC): "+String.format("%1$.5f", owlsim.getSummaryStatistics().max.getMax()));
			LOG.info("mean(sumIC): "+String.format("%1$.5f", owlsim.getSummaryStatistics().sum.getMean()));

		  LOG.info("Computing subgraph stats for ids: "+upperLevelClassIDs.toString());
		  for (String id : upperLevelClassIDs) {
		  	OWLClass c = g.getOWLClassByIdentifier(id);
		  	owlsim.computeSystemStatsForSubgraph(c);
		  	StatsPerIndividual s = owlsim.getSummaryStatistics(c);
		  	LOG.info("Summary stats for "+g.getLabel(c)+" ("+id+"):\n"+s.toString());
		  }
		
		}
		finally {
			reasoner.dispose();
		}
	}
	
	@Test
	public void testIxI() throws Exception {
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
	
	@Test
	public void testAnnotationSufficiencyScore() throws OWLOntologyCreationException, OBOFormatParserException, IOException, UnknownOWLClassException {
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

			for (OWLNamedIndividual i : owlsim.getAllElements()) {
				LOG.info(i.toStringID()+" scores:");
				double score = owlsim.calculateOverallAnnotationSufficiencyForIndividual(i);
				LOG.info(owlsim.computeIndividualStats(i).getSummary()+"annotation_sufficiency: "+score);
			}
		} finally {
			reasoner.dispose();
		}
	}
}
