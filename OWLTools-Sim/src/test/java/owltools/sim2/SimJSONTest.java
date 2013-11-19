package owltools.sim2;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.math.MathException;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.graph.OWLGraphWrapper;
import owltools.io.OWLPrettyPrinter;
import owltools.io.ParserWrapper;
import owltools.sim2.scores.ElementPairScores;

/**
 * Tests the JSON wrapper to OwlSim, used by OWLServer
 * 
 * @author cjm
 *
 */
public class SimJSONTest extends AbstractOWLSimTest {

	private Logger LOG = Logger.getLogger(SimJSONTest.class);

	@Test
	public void testCompareIndividuals() throws IOException, OWLOntologyCreationException, OWLOntologyStorageException, MathException, UnknownOWLClassException {
		ParserWrapper pw = new ParserWrapper();
		sourceOntol = pw.parseOWL(getResourceIRIString("sim/mp-subset-1.obo"));
		g =  new OWLGraphWrapper(sourceOntol);
		parseAssociations(getResource("sim/mgi-gene2mp-subset-1.tbl"), g);

		owlpp = new OWLPrettyPrinter(g);
		final int truncLen = 200;
		
		// assume buffering
		OWLReasoner reasoner = new ElkReasonerFactory().createReasoner(sourceOntol);
		try {

			createOwlSim();
				//sos.setReasoner(reasoner);
			LOG.info("Reasoner="+owlsim.getReasoner());

			SimJSONEngine sj = new SimJSONEngine(g, owlsim);

			//sos.saveOntology("/tmp/z.owl");

			reasoner.flush();
			for (OWLNamedIndividual i : sourceOntol.getIndividualsInSignature()) {
				//System.out.println("COMPARING: "+i);
				for (OWLNamedIndividual j : sourceOntol.getIndividualsInSignature()) {
					String jsonStr = sj.compareAttributeSetPair(
							owlsim.getAttributesForElement(i),
							owlsim.getAttributesForElement(j)
							);
					if(jsonStr.length() < truncLen)
						LOG.warn(jsonStr);
					else
						LOG.info("SAMPLE:"+jsonStr.substring(0,  truncLen));
				}
			}
			
			
			// test to ensure robust in face of unknown classes
			df = g.getDataFactory();
			OWLClass unkC = df.getOWLClass(IRI.create("http://x.org"));
			Set<OWLClass> uset = Collections.singleton(unkC);
			boolean isThrown = false;
			try {
				sj.compareAttributeSetPair(uset, uset);
			} catch (UnknownOWLClassException e) {
				// we expect this
				isThrown = true;
			}
			assertTrue(isThrown);
			sj.compareAttributeSetPair(uset, uset, true);
			
			OWLClass thing = df.getOWLThing();
			Set<OWLClass> things = Collections.singleton(unkC);
			
			// we expect no results here
			LOG.info("TxT:"+sj.compareAttributeSetPair(things, things, true));
			
		}
		finally {
			reasoner.dispose();
		}
	}

	@Test
	public void testSearch() throws IOException, OWLOntologyCreationException, OWLOntologyStorageException, MathException, UnknownOWLClassException {
		ParserWrapper pw = new ParserWrapper();
		sourceOntol = pw.parseOWL(getResourceIRIString("sim/mp-subset-1.obo"));
		g =  new OWLGraphWrapper(sourceOntol);
		parseAssociations(getResource("sim/mgi-gene2mp-subset-1.tbl"), g);

		owlpp = new OWLPrettyPrinter(g);
		final int truncLen = 200;
		
		// assume buffering
		OWLReasoner reasoner = new ElkReasonerFactory().createReasoner(sourceOntol);
		try {

			createOwlSim();
				//sos.setReasoner(reasoner);
			LOG.info("Reasoner="+owlsim.getReasoner());

			SimJSONEngine sj = new SimJSONEngine(g, owlsim);

			//sos.saveOntology("/tmp/z.owl");

			reasoner.flush();
			for (OWLNamedIndividual i : sourceOntol.getIndividualsInSignature()) {
				Set<OWLClass> atts = owlsim.getAttributesForElement(i);
				String jsonStr = sj.search(atts, "MGI", true);
				
				LOG.info(jsonStr);
			}
			
		}
		finally {
			reasoner.dispose();
		}
	}



}
