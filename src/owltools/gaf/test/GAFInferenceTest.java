package owltools.gaf.test;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import owltools.gaf.GAFParser;
import owltools.gaf.GafDocument;
import owltools.gaf.GafObjectsBuilder;
import owltools.gaf.GeneAnnotation;
import owltools.gaf.inference.AnnotationPredictor;
import owltools.gaf.inference.CompositionalClassPredictor;
import owltools.gaf.inference.Prediction;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;
import junit.framework.TestCase;

public class GAFInferenceTest extends TestCase {

	public static void testParser() throws IOException, OWLOntologyCreationException{
		GafObjectsBuilder builder = new GafObjectsBuilder();

		GafDocument gafdoc = builder.buildDocument(new File("test_resources/xp_inference_test.gaf"));
		ParserWrapper pw = new ParserWrapper();

		OWLOntology ont = pw.parse("test_resources/go_xp_predictor_test_subset.obo");
		OWLGraphWrapper g = new OWLGraphWrapper(ont);

		AnnotationPredictor ap = new CompositionalClassPredictor(gafdoc, g);

		Set<Prediction> predictions = ap.getAllPredictions();
		boolean ok1 = false;
		boolean ok2 = false;
		for (Prediction p : predictions) {
			System.out.println("p="+p);
			GeneAnnotation a = p.getGeneAnnotation();
			String b = a.getBioentity();
			String c= a.getCls();
			if (b.equals("FOO:1") && c.equals("GO:0032543"))
				ok1 = true;
			if (b.equals("FOO:3") && c.equals("GO:0032543"))
				ok1 = true;
		}
		assertTrue(ok1);
		assertTrue(ok2);
		assertTrue(predictions.size() == 2);

	}

}
