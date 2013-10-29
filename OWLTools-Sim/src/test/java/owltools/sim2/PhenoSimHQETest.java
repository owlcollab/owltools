package owltools.sim2;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Set;

import org.apache.commons.math.MathException;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
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
import owltools.sim2.OwlSim.ScoreAttributeSetPair;
import owltools.sim2.SimpleOwlSim;
import owltools.sim2.preprocessor.PhenoSimHQEPreProcessor;
import owltools.sim2.preprocessor.SimPreProcessor;

/**
 * This is the main test class for PropertyViewOntologyBuilder
 * 
 * @author cjm
 *
 */
public class PhenoSimHQETest extends OWLToolsTestBasics {

	private Logger LOG = Logger.getLogger(PhenoSimHQETest.class);
	OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
	OWLDataFactory df = manager.getOWLDataFactory();
	OWLOntology sourceOntol;
	SimPreProcessor pproc;
	OWLPrettyPrinter owlpp;
	OWLGraphWrapper g;
	SimpleOwlSim sos;

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
			pproc.setOWLPrettyPrinter(owlpp);
			((PhenoSimHQEPreProcessor)pproc).defaultLCSElementFrequencyThreshold = 0.7;

			//sos.setSimPreProcessor(pproc);
			//sos.preprocess();
			pproc.preprocess();
			reasoner.flush();

			sos = new SimpleOwlSim(sourceOntol);
			sos.setSimPreProcessor(pproc);
			sos.createElementAttributeMapFromOntology();

			//sos.saveOntology("/tmp/z.owl");

			reasoner.flush();
			for (OWLNamedIndividual i : sourceOntol.getIndividualsInSignature()) {
				for (OWLNamedIndividual j : sourceOntol.getIndividualsInSignature()) {
					showSim(i,j);
				}
			}
		}
		finally {
			reasoner.dispose();
		}
	}
	
	@Test
	public void testPhenoSimMouse() throws IOException, OWLOntologyCreationException, OWLOntologyStorageException, MathException {
		ParserWrapper pw = new ParserWrapper();
		sourceOntol = pw.parseOWL(getResourceIRIString("test_phenotype.owl"));
		g =  new OWLGraphWrapper(sourceOntol);
		 owlpp = new OWLPrettyPrinter(g);
		
		// assume buffering
		OWLReasoner reasoner = new ElkReasonerFactory().createReasoner(sourceOntol);
		try {
			pproc = new PhenoSimHQEPreProcessor();
			pproc.setInputOntology(sourceOntol);
			pproc.setOutputOntology(sourceOntol);
			pproc.setReasoner(reasoner);
			pproc.setOWLPrettyPrinter(owlpp);
			((PhenoSimHQEPreProcessor)pproc).defaultLCSElementFrequencyThreshold = 0.7;

			//sos.setSimPreProcessor(pproc);
			//sos.preprocess();
			pproc.preprocess();
			reasoner.flush();

			sos = new SimpleOwlSim(sourceOntol);
			sos.setSimPreProcessor(pproc);
			sos.createElementAttributeMapFromOntology();

			//sos.saveOntology("/tmp/z.owl");

			reasoner.flush();
			for (OWLNamedIndividual i : sourceOntol.getIndividualsInSignature()) {
				for (OWLNamedIndividual j : sourceOntol.getIndividualsInSignature()) {
					showSim(i,j);
				}
			}
		}
		finally {
			reasoner.dispose();
		}
	}


	private void showSim(OWLNamedIndividual i, OWLNamedIndividual j) {
		
		double s = sos.getElementJaccardSimilarity(i, j);
		System.out.println("SimJ( "+i+" , "+j+" ) = "+s);

		ScoreAttributeSetPair maxic = sos.getSimilarityMaxIC(i, j);
		System.out.println("MaxIC( "+i+" , "+j+" ) = "+maxic.score+" "+show(maxic.attributeClassSet));

		ScoreAttributeSetPair bma = sos.getSimilarityBestMatchAverageAsym(i, j);
		System.out.println("BMAasym( "+i+" , "+j+" ) = "+bma.score+" "+show(bma.attributeClassSet));
		
	}




	private String show(Set<OWLClass> attributeClassSet) {
		StringBuffer sb = new StringBuffer();
		for (OWLClassExpression c : attributeClassSet) {
			sb.append(owlpp.render(c) + " ; ");
		}
		return sb.toString();
	}

	private OWLClass get(String iri) {
		return df.getOWLClass(IRI.create("http://x.org#"+iri));
	}





}
