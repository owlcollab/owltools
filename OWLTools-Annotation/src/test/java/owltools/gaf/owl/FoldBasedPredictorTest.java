package owltools.gaf.owl;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLOntology;

import owltools.OWLToolsTestBasics;
import owltools.gaf.GafDocument;
import owltools.gaf.inference.FoldBasedPredictor;
import owltools.gaf.inference.Prediction;
import owltools.gaf.parser.GafObjectsBuilder;
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

		FoldBasedPredictor fbp = new FoldBasedPredictor(gafdoc, g, true);
		OWLClass ecp = (OWLClass) g.getOWLObjectByLabel("epithelial cell proliferation");
		for (OWLClassExpression ex : ecp.getEquivalentClasses(g.getSourceOntology())) {
			LOG.info("ECA="+ex);
			// expect:  ObjectIntersectionOf(<http://purl.obolibrary.org/obo/GO_0008283> ObjectSomeValuesFrom(<http://purl.obolibrary.org/obo/TEST_1234567> <http://purl.obolibrary.org/obo/CL_0000066>))
		}
		List<Prediction> preds = fbp.predict("MGI:TESTME");
		//Set<Prediction> preds = fbp.getAllPredictions();
		for (Prediction pred : preds) {
			LOG.info(pred);
		}
		assertEquals(1, preds.size());
		// GO:0050673 'epithelial cell proliferation'
		assertEquals("GO:0050673", preds.get(0).getGeneAnnotation().getCls());
		
		// TESTME2
		preds = fbp.predict("MGI:TESTME2");
		assertEquals(1, preds.size());
		// GO:0050701 'interleukin-1 secretion'
		assertEquals("GO:0050701", preds.get(0).getGeneAnnotation().getCls());
		
		// TESTME3
		preds = fbp.predict("MGI:TESTME3");
		assertEquals(1, preds.size());
		// GO:0005623 'cell'
		assertEquals("GO:0005623", preds.get(0).getGeneAnnotation().getCls());
		
		// check that only the expected predictions are here
		preds = fbp.getAllPredictions();
		assertEquals(3, preds.size());
		for (Prediction pred : preds) {
			LOG.info(pred.render(owlpp));
		}
		LOG.info("total preds: "+preds.size());
	}

}
