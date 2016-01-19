package owltools.mooncat;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLOntology;

import owltools.OWLToolsTestBasics;
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
