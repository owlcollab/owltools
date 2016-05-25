package owltools.gaf.owl;

import java.io.File;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
import org.semanticweb.owlapi.model.OWLOntology;

import owltools.OWLToolsTestBasics;
import owltools.gaf.GafDocument;
import owltools.gaf.parser.GafObjectsBuilder;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

public class MgiGAFOWLBridgeTest extends OWLToolsTestBasics{

	private Logger LOG = Logger.getLogger(MgiGAFOWLBridgeTest.class);
	
	@Test
	public void testConversion() throws Exception{
		ParserWrapper pw = new ParserWrapper();
		OWLOntology ont = pw.parse(getResourceIRIString("go_xp_predictor_test_subset.obo"));
		OWLGraphWrapper g = new OWLGraphWrapper(ont);
		g.addSupportOntology(pw.parse(getResourceIRIString("gorel.owl")));

		GafObjectsBuilder builder = new GafObjectsBuilder();

		GafDocument gafdoc = builder.buildDocument(getResource("mgi-exttest.gaf"));

		GAFOWLBridge bridge = new GAFOWLBridge(g);
		bridge.setGenerateIndividuals(false);
		OWLOntology gafOnt = g.getManager().createOntology();
		bridge.setTargetOntology(gafOnt);
		bridge.translate(gafdoc);
		
		OWLDocumentFormat owlFormat = new RDFXMLDocumentFormat();
		g.getManager().saveOntology(gafOnt, owlFormat, IRI.create(new File("target/gaf.owl")));
		
		for (OWLAxiom ax : gafOnt.getAxioms()) {
			//LOG.info("AX:"+ax);
		}

	}
	
	@Test
	public void testConversionToIndividuals() throws Exception{
		ParserWrapper pw = new ParserWrapper();
		OWLOntology ont = pw.parse(getResourceIRIString("go_xp_predictor_test_subset.obo"));
		OWLGraphWrapper g = new OWLGraphWrapper(ont);
		g.addSupportOntology(pw.parse(getResourceIRIString("gorel.owl")));

		GafObjectsBuilder builder = new GafObjectsBuilder();

		GafDocument gafdoc = builder.buildDocument(getResource("mgi-exttest.gaf"));

		GAFOWLBridge bridge = new GAFOWLBridge(g);
		bridge.setGenerateIndividuals(false);
		OWLOntology gafOnt = g.getManager().createOntology();
		bridge.setTargetOntology(gafOnt);
		bridge.setBasicAboxMapping(true);
		bridge.translate(gafdoc);
		
		OWLDocumentFormat owlFormat = new RDFXMLDocumentFormat();
		g.getManager().saveOntology(gafOnt, owlFormat, IRI.create(new File("target/gaf-abox.owl")));
		
		for (OWLAxiom ax : gafOnt.getAxioms()) {
			//LOG.info("AX:"+ax);
		}

	}

}
