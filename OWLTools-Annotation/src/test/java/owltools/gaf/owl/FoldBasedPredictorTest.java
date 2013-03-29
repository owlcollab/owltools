package owltools.gaf.owl;

import java.io.File;
import java.util.Set;

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
import owltools.gaf.inference.FoldBasedPredictor;
import owltools.gaf.inference.Prediction;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

public class FoldBasedPredictorTest extends OWLToolsTestBasics{

	private Logger LOG = Logger.getLogger(FoldBasedPredictorTest.class);
	
	@Test
	public void testConversion() throws Exception{
		ParserWrapper pw = new ParserWrapper();
		OWLOntology ont = pw.parse(getResourceIRIString("mgi-exttest-go-subset.obo"));
		OWLGraphWrapper g = new OWLGraphWrapper(ont);
		//g.addSupportOntology(pw.parse(getResourceIRIString("gorel.owl")));

		GafObjectsBuilder builder = new GafObjectsBuilder();

		GafDocument gafdoc = builder.buildDocument(getResource("mgi-exttest.gaf"));

		FoldBasedPredictor fbp = new FoldBasedPredictor(gafdoc, g);
		Set<Prediction> preds = fbp.predict("MGI:TESTME");
		//Set<Prediction> preds = fbp.getAllPredictions();
		for (Prediction pred : preds) {
			LOG.info(pred);
		}
	}

}
