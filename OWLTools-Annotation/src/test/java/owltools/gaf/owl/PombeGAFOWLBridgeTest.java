package owltools.gaf.owl;

import java.io.File;

import org.apache.log4j.Logger;
import org.junit.Ignore;
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

public class PombeGAFOWLBridgeTest extends OWLToolsTestBasics{

	private Logger LOG = Logger.getLogger(PombeGAFOWLBridgeTest.class);
	
	@Ignore
	@Test
	public void testConversion() throws Exception {
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
