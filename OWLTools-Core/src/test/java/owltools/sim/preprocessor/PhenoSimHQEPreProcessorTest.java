package owltools.sim.preprocessor;

import static org.junit.Assert.*;

import java.io.IOException;

import org.apache.commons.math.MathException;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.OWLToolsTestBasics;
import owltools.graph.OWLGraphWrapper;
import owltools.io.OWLPrettyPrinter;
import owltools.io.ParserWrapper;
import owltools.sim.preprocessor.PhenoSimHQEPreProcessor;
import owltools.sim.preprocessor.SimPreProcessor;

/**
 * This is the main test class for PropertyViewOntologyBuilder
 * 
 * @author cjm
 *
 */
public class PhenoSimHQEPreProcessorTest extends OWLToolsTestBasics {

	private Logger LOG = Logger.getLogger(PhenoSimHQEPreProcessorTest.class);
	OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
	OWLDataFactory df = manager.getOWLDataFactory();
	OWLOntology sourceOntol;
	SimPreProcessor pproc;
	OWLPrettyPrinter owlpp;
	OWLGraphWrapper g;

	@Test
	public void testPhenoSim() throws IOException, OWLOntologyCreationException, OWLOntologyStorageException, MathException {
		ParserWrapper pw = new ParserWrapper();
		sourceOntol = pw.parseOWL(getResourceIRIString("q-in-e.omn"));
		g =  new OWLGraphWrapper(sourceOntol);
		 owlpp = new OWLPrettyPrinter(g);
		
		// assume buffering
		OWLReasoner reasoner = new ElkReasonerFactory().createReasoner(sourceOntol);
		try {
			pproc = new PhenoSimHQEPreProcessor();
			pproc.setInputOntology(sourceOntol);
			pproc.setOutputOntology(sourceOntol);
			pproc.setReasoner(reasoner);

			pproc.preprocess();
			reasoner.flush();


			reasoner.flush();
			for (OWLNamedIndividual i : sourceOntol.getIndividualsInSignature()) {
				for (OWLNamedIndividual j : sourceOntol.getIndividualsInSignature()) {
					showLCS(i,j);
				}
			}

			// note: may be sensitive to ordering..
			testLCS("hypoplastic and affected retina phenotype",
					"hypoplastic and affected retina phenotype",
					"hypoplastic and affected retina phenotype");

			testLCS("hypoplastic and affected retina phenotype",
					"hyperplastic and affected ommatidium phenotype",
					"abnormal_morphology and affected photoreceptor-based entity phenotype");

			testLCS("hyperplastic and affected hand phenotype",
					"hypoplastic and affected hindlimb phenotype",
					"abnormal_morphology and affected limb structure phenotype");
		
		}
		finally{
			reasoner.dispose();
		}
		 
	}

	private void showLCS(OWLNamedIndividual i, OWLNamedIndividual j) {
		LOG.info("LCS of "+i+" vs "+j);
		;
		for (OWLClass xi : pproc.getReasoner().getTypes(i, true).getFlattened()) {
			for (OWLClass xj : pproc.getReasoner().getTypes(j, true).getFlattened()) {
				OWLClassExpression lcs = pproc.getLowestCommonSubsumer(xi, xj);
				LOG.info("    LCS of "+xi+" vs "+xj+" = "+owlpp.render(lcs));
			}
		}
	}


	private void testLCS(String x, String y, String a) {
		OWLClass xc = (OWLClass) g.getOWLObjectByLabel(x);
		OWLClass yc = (OWLClass) g.getOWLObjectByLabel(y);
		OWLClass ac = (OWLClass) g.getOWLObjectByLabel(a);
		LOG.info("Getting lcs of "+owlpp.render(xc)+" -vs- "+owlpp.render(yc));
		LOG.info("   Expecting: "+owlpp.render(ac));
		OWLClassExpression lcs = pproc.getLowestCommonSubsumer(xc, yc);
		LOG.info("        Got : "+owlpp.render(lcs));
		
		assertEquals(ac, lcs);

	}



}
