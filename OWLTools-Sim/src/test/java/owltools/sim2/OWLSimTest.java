package owltools.sim2;

import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.graph.OWLGraphWrapper;
import owltools.io.OWLPrettyPrinter;
import owltools.io.ParserWrapper;
import owltools.sim2.AbstractOWLSimTest;
import owltools.sim2.SimpleOwlSim.SimConfigurationProperty;
import owltools.sim2.UnknownOWLClassException;
import owltools.sim2.scores.ElementPairScores;

/**
 * This is the main test class for PropertyViewOntologyBuilder
 * 
 * @author cjm
 *
 */
public class OWLSimTest extends AbstractOWLSimTest {

	private Logger LOG = Logger.getLogger(OWLSimTest.class);
	OWLReasoner reasoner;
	private void setup() throws UnknownOWLClassException {
		reasoner = new ElkReasonerFactory().createReasoner(sourceOntol);
		this.createOwlSim();
		owlsim.createElementAttributeMapFromOntology();
		Properties p = new Properties();
		p.setProperty(SimConfigurationProperty.minimumMaxIC.toString(), "0.01");
		p.setProperty(SimConfigurationProperty.minimumSimJ.toString(), "0.01");
		
		owlsim.setSimProperties(p);
		reasoner.flush();
	
	}
	
	@Test
	public void testBasicSim() throws Exception {
		ParserWrapper pw = new ParserWrapper();
		sourceOntol = pw.parseOWL(getResource("sim/simont.owl").getAbsolutePath());
		g =  new OWLGraphWrapper(sourceOntol);

		String CACHE = "target/owlsim.cache";
		
		owlpp = new OWLPrettyPrinter(g);

		// assume buffering
		try {

			setup();
			testMatches();
			owlsim.saveLCSCache(CACHE);
			reasoner.dispose();
			
			setup();			
			owlsim.loadLCSCache(CACHE);
			testMatches();
			
			

		}
		finally {
			reasoner.dispose();
		}
	}
	
	private void testMatches() throws Exception {
		for (OWLNamedIndividual i : sourceOntol.getIndividualsInSignature()) {
			LOG.info("Finding matches for "+i+" Types: "+owlsim.getAttributesForElement(i));
			List<ElementPairScores> matches = owlsim.findMatches(i, null, 0, 0);
			LOG.info("|matches|="+matches.size());
			for (ElementPairScores m : matches) {
				LOG.info("  m="+m);
				if (i.equals(m.j)) {
					LOG.info("Self best match for "+i+" "+m.combinedScore+" "+m.rank);
					Assert.assertTrue(100 == m.combinedScore);
					//Assert.assertTrue(m.rank <= 2);
				}
			}
		}
		
		isAllEquiv("x1", "x2");
		isAllEquiv("x3", "x4", "x4b");
		isAllEquiv("x5", "x6");		
	}

	private void isAllEquiv(String... xs) throws Exception {
		Set<OWLNamedIndividual> inds = new HashSet<OWLNamedIndividual>();
		for (String x : xs) {
			OWLNamedIndividual xi = getOBOIndividual("SIM:"+x);
			LOG.info("Lookuo " +x +" = "+xi);
			inds.add(xi);
		}
		int nFails = 0;
		for (OWLNamedIndividual xi : inds) {
			int n = 0;
			List<ElementPairScores> matches = owlsim.findMatches(xi, null, 0, 0);
			for (ElementPairScores m : matches) {
				LOG.info("M="+m);
				if (inds.contains(m.j)) {
					n++;
					if (m.combinedScore < 100) {
						LOG.info("  **M="+m);
						nFails++;
					}
					else {
						
					}
				}
//				if (inds.contains(m.j)) {
//					n++;
//					if (m.rank >= inds.size()) {
//						LOG.info("  **Expected this to be ranked="+m);
//						nFails ++;
//					}
//				}
//				else {
//					if (m.rank < inds.size()) {
//						LOG.info("  **Ranked too highly="+m);
//
//						nFails ++;
//					}
//				}
			}
			for (String y : xs) {
				
			}
			Assert.assertTrue(n == inds.size());

		}
		Assert.assertTrue(nFails == 0);
	}



}
