package owltools.gaf.owl;

import java.io.File;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyFormat;

import owltools.OWLToolsTestBasics;
import owltools.gaf.GafDocument;
import owltools.gaf.GafObjectsBuilder;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

public class GAFOWLBridgeTest extends OWLToolsTestBasics{

	private Logger LOG = Logger.getLogger(GAFOWLBridgeTest.class);
	
	@Test
	public void testConversion() throws Exception{
		ParserWrapper pw = new ParserWrapper();
		OWLOntology ont = pw.parse(getResourceIRIString("go_xp_predictor_test_subset.obo"));
		OWLGraphWrapper g = new OWLGraphWrapper(ont);
		g.addSupportOntology(pw.parse(getResourceIRIString("gorel.owl")));

		GafObjectsBuilder builder = new GafObjectsBuilder();

		GafDocument gafdoc = builder.buildDocument(getResource("xp_inference_test.gaf"));

		GAFOWLBridge bridge = new GAFOWLBridge(g);
		OWLOntology gafOnt = g.getManager().createOntology();
		bridge.setTargetOntology(gafOnt);
		bridge.translate(gafdoc);
		
		OWLOntologyFormat owlFormat = new RDFXMLOntologyFormat();
		g.getManager().saveOntology(gafOnt, owlFormat, IRI.create(new File("/tmp/gaf.owl")));
		
		for (OWLAxiom ax : gafOnt.getAxioms()) {
			LOG.info("AX:"+ax);
		}

	}

}
