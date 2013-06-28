package owltools.gaf.inference;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;

import owltools.OWLToolsTestBasics;
import owltools.gaf.GafDocument;
import owltools.gaf.GafObjectsBuilder;
import owltools.gaf.GeneAnnotation;
import owltools.gaf.io.GafWriter;
import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;
import owltools.graph.OWLQuantifiedProperty;
import owltools.io.ParserWrapper;

public class BasicAnnotationPropagatorTest extends OWLToolsTestBasics {

	@Test
	public void testClosure() throws Exception {
		ParserWrapper pw = new ParserWrapper();
		OWLGraphWrapper g = pw.parseToOWLGraph(getResourceIRIString("lmajor_f2p_test_go_subset.obo"));
		
		OWLClass c = g.getOWLClassByIdentifier("GO:0004004"); // ATP-dependent RNA helicase activity
		
		boolean found = false;
		
		Set<OWLGraphEdge> closure = g.getOutgoingEdgesClosure(c);
		for (OWLGraphEdge edge : closure) {
			List<OWLQuantifiedProperty> propertyList = edge.getQuantifiedPropertyList();
			if (propertyList.size() == 1) {
				OWLQuantifiedProperty quantifiedProperty = propertyList.get(0);
				final OWLObjectProperty property = quantifiedProperty.getProperty();
				String property_id = g.getIdentifier(property);
				if ("part_of".equals(property_id)) {
					OWLObject target = edge.getTarget();
					if (target instanceof OWLClass) {
						String targetId = g.getIdentifier(target);
						if ("GO:0006200".equals(targetId)) {
							found = true;
						}
					}
				}
			}
		}
		
		assertTrue(found);
	}
	
	@Test
	public void test() throws Exception {
		ParserWrapper pw = new ParserWrapper();
		OWLGraphWrapper g = pw.parseToOWLGraph(getResourceIRIString("lmajor_f2p_test_go_subset.obo"));
		GafObjectsBuilder b = new GafObjectsBuilder();
		GafDocument gafDocument = b.buildDocument(getResource("lmajor_f2p_test.gaf"));
		
		Set<Prediction> allPredictions = predictAnnotations(g, gafDocument);
		assertEquals(1, allPredictions.size());
		final GeneAnnotation geneAnnotation = allPredictions.iterator().next().getGeneAnnotation();
		
		GafWriter writer = new GafWriter();
		ByteArrayOutputStream out  = new ByteArrayOutputStream();
		writer.setStream(new PrintStream(out));
		writer.write(geneAnnotation);
		out.flush();
		String writtenLine = out.toString().trim(); // trim to avoid hassle with tabs and new lines at the end
		String dateString = GeneAnnotation.GAF_Date_Format.get().format(new Date());
		String expectedLine1 = "GeneDB_Lmajor	LmjF.01.0770	LmjF.01.0770		GO:0006200	PMID:17087726	EXP	GO:0004004	P	eukaryotic initiation factor 4a, putative	LmjF01.0770	gene	taxon:347515	"+dateString+"	GOC";
		String expectedLine2 = "GeneDB_Lmajor	LmjF.01.0770	LmjF.01.0770		GO:0006200	PMID:17087726	EXP	GO:0008186	P	eukaryotic initiation factor 4a, putative	LmjF01.0770	gene	taxon:347515	"+dateString+"	GOC";
		assertTrue(expectedLine1.equals(writtenLine) || expectedLine2.equals(writtenLine));
	}

	/**
	 * @param g
	 * @param gafDocument
	 * @return predictions
	 */
	private Set<Prediction> predictAnnotations(OWLGraphWrapper g, GafDocument gafDocument) {
		AnnotationPredictor propagator = null;
		try {
			propagator = new BasicAnnotationPropagator(gafDocument, g);
			Set<Prediction> allPredictions = propagator.getAllPredictions();
			return allPredictions;
		}
		finally {
			if(propagator != null) {
				propagator.dispose();
			}
		}
	}

}
