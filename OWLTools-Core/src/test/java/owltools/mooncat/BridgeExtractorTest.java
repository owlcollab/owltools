package owltools.mooncat;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import owltools.OWLToolsTestBasics;
import owltools.graph.OWLGraphWrapper;
import owltools.io.OWLPrettyPrinter;
import owltools.io.ParserWrapper;

/**
 * This is the main test class for PropertyViewOntologyBuilder
 * 
 * @author cjm
 *
 */
public class BridgeExtractorTest extends OWLToolsTestBasics {

	private static boolean RENDER_ONTOLOGY_FLAG = false;
	
	private Logger LOG = Logger.getLogger(BridgeExtractorTest.class);

	
	@Test
	public void testExtractBridge() throws Exception {
		ParserWrapper pw = new ParserWrapper();
		
		OWLOntology sourceOntol = pw.parseOBO(getResourceIRIString("extract_bridge_test.obo"));
		BridgeExtractor be = new BridgeExtractor(sourceOntol);
		
		 be.extractBridgeOntologies("go", true);
		 /*
		 for (OWLOntology o : bridgeOnts) {
			 LOG.info("BRIDGE: "+o);
		 }
		 */
		 be.saveBridgeOntologies("out/bridgestest/");
	}

	@Test
	public void testExtractGoTaxonBridge() throws Exception {
		ParserWrapper pw = new ParserWrapper();
		
		OWLOntology sourceOntol = pw.parseOBO(getResourceIRIString("taxon_go_triggers.obo"));
		BridgeExtractor be = new BridgeExtractor(sourceOntol);
		
		 be.extractBridgeOntologies("go", true);
		 be.saveBridgeOntologies("out/go/");
	}



}
