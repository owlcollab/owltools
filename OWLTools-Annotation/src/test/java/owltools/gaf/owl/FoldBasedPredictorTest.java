package owltools.gaf.owl;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Set;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyFormat;

import owltools.OWLToolsTestBasics;
import owltools.gaf.GafDocument;
import owltools.gaf.GafObjectsBuilder;
import owltools.gaf.inference.FoldBasedPredictor;
import owltools.gaf.inference.Prediction;
import owltools.graph.OWLGraphWrapper;
import owltools.io.OWLPrettyPrinter;
import owltools.io.ParserWrapper;

public class FoldBasedPredictorTest extends OWLToolsTestBasics{

	private Logger LOG = Logger.getLogger(FoldBasedPredictorTest.class);
	
	@Test
	public void testConversion() throws Exception{
		ParserWrapper pw = new ParserWrapper();
		OWLOntology ont = pw.parse(getResourceIRIString("mgi-exttest-go-subset.obo"));
		OWLGraphWrapper g = new OWLGraphWrapper(ont);
		//g.addSupportOntology(pw.parse(getResourceIRIString("gorel.owl")));
		OWLPrettyPrinter owlpp = new OWLPrettyPrinter(g);

		GafObjectsBuilder builder = new GafObjectsBuilder();

		GafDocument gafdoc = builder.buildDocument(getResource("mgi-exttest.gaf"));
		//GafDocument gafdoc = builder.buildDocument(getResource("foo.gaf"));

		FoldBasedPredictor fbp = new FoldBasedPredictor(gafdoc, g);
		OWLClass ecp = (OWLClass) g.getOWLObjectByLabel("epithelial cell proliferation");
		for (OWLClassExpression ex : ecp.getEquivalentClasses(g.getSourceOntology())) {
			LOG.info("ECA="+ex);
			// expect:  ObjectIntersectionOf(<http://purl.obolibrary.org/obo/GO_0008283> ObjectSomeValuesFrom(<http://purl.obolibrary.org/obo/TEST_1234567> <http://purl.obolibrary.org/obo/CL_0000066>))
		}
		Set<Prediction> preds = fbp.predict("MGI:TESTME");
		//Set<Prediction> preds = fbp.getAllPredictions();
		for (Prediction pred : preds) {
			LOG.info(pred);
		}
		assertTrue(preds.size() == 1);
		 preds = fbp.getAllPredictions();
		for (Prediction pred : preds) {
			LOG.info(pred.render(owlpp));
		}
		LOG.info("total preds: "+preds.size());
		/*
		OWLClass hcp = (OWLClass) 
		g.getOWLObjectByLabel("cell proliferation acts_on_population_of some hepatocyte");
		for (OWLClassExpression ex : hcp.getEquivalentClasses(g.getSourceOntology())) {
			LOG.info("EC for HCP ="+ex);
		}
		for(OWLClass sc : fbp.reasoner.getSuperClasses(hcp, false).getFlattened()) {
			LOG.info("SC ="+sc);
		}
		for(OWLClass ec : fbp.reasoner.getEquivalentClasses(hcp).getEntities()) {
			String label = g.getLabel(ec);
			LOG.info("hcp EQUIV_TO ="+ec+ " "+label);
		}
		*/
		
	}

}
