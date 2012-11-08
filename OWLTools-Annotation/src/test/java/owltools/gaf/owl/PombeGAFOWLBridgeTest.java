package owltools.gaf.owl;

import static junit.framework.Assert.*;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Set;

import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import owltools.OWLToolsTestBasics;
import owltools.gaf.GafDocument;
import owltools.gaf.GafObjectsBuilder;
import owltools.gaf.GeneAnnotation;
import owltools.gaf.inference.AnnotationPredictor;
import owltools.gaf.inference.CompositionalClassPredictor;
import owltools.gaf.inference.Prediction;
import owltools.gaf.owl.GAFOWLBridge;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

public class PombeGAFOWLBridgeTest extends OWLToolsTestBasics{

	private Logger LOG = Logger.getLogger(PombeGAFOWLBridgeTest.class);
	
	@Ignore
	@Test
	public void testConversion() throws IOException, OWLOntologyCreationException, OWLOntologyStorageException, URISyntaxException{
		ParserWrapper pw = new ParserWrapper();
		OWLOntology ont = pw.parse("http://purl.obolibrary.org/obo/go.owl");
		OWLGraphWrapper g = new OWLGraphWrapper(ont);

		GafObjectsBuilder builder = new GafObjectsBuilder();

		GafDocument gafdoc = builder.buildDocument("http://geneontology.org/gene-associations/gene_association.GeneDB_Spombe.gz");

		GAFOWLBridge bridge = new GAFOWLBridge(g);
		OWLOntology gafOnt = g.getManager().createOntology();
		bridge.setTargetOntology(gafOnt);
		bridge.translate(gafdoc);
		
		OWLOntologyFormat owlFormat = new RDFXMLOntologyFormat();
		g.getManager().saveOntology(gafOnt, owlFormat, IRI.create(new File("/tmp/gaf.owl")));
		
		for (OWLAxiom ax : gafOnt.getAxioms()) {
			//LOG.info("AX:"+ax);
		}

	}

}
