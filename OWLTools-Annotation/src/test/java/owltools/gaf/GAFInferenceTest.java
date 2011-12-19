package owltools.gaf;

import static junit.framework.Assert.*;

import java.io.IOException;
import java.util.Set;

import org.junit.Test;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import owltools.OWLToolsTestBasics;
import owltools.gaf.GafDocument;
import owltools.gaf.GafObjectsBuilder;
import owltools.gaf.GeneAnnotation;
import owltools.gaf.inference.AnnotationPredictor;
import owltools.gaf.inference.CompositionalClassPredictor;
import owltools.gaf.inference.Prediction;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

public class GAFInferenceTest extends OWLToolsTestBasics{

	@Test
	public void testParser() throws IOException, OWLOntologyCreationException{
		GafObjectsBuilder builder = new GafObjectsBuilder();

		GafDocument gafdoc = builder.buildDocument(getResource("xp_inference_test.gaf"));
		ParserWrapper pw = new ParserWrapper();

		OWLOntology ont = pw.parse(getResourceIRIString("go_xp_predictor_test_subset.obo"));
		OWLGraphWrapper g = new OWLGraphWrapper(ont);

		AnnotationPredictor ap = new CompositionalClassPredictor(gafdoc, g);

		Set<Prediction> predictions = ap.getAllPredictions();
		boolean ok1 = false;
		boolean ok2 = false;
		for (Prediction p : predictions) {
			GeneAnnotation a = p.getGeneAnnotation();
			String b = a.getBioentity();
			String c = a.getCls();
			System.out.println("p="+p+" // "+b+"-"+c);
			if (b.equals("FOO:FOO:1") && c.equals("GO:0032543"))
				ok1 = true;
			if (b.equals("FOO:FOO:3") && c.equals("GO:0032543"))
				ok2 = true;
		}
		assertTrue(ok1);
		assertTrue(ok2);
		assertTrue(predictions.size() == 2);

	}

}
