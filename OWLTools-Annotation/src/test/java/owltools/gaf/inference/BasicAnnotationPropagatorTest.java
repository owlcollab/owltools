package owltools.gaf.inference;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

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
	public void test1() throws Exception {
		ParserWrapper pw = new ParserWrapper();
		OWLGraphWrapper g = pw.parseToOWLGraph(getResourceIRIString("rules/ontology/go.owl"));
		ElkReasonerFactory f = new ElkReasonerFactory();
		OWLReasoner reasoner = f.createReasoner(g.getSourceOntology());
		
		assertMapping("GO:0035556", "occurs_in", g, reasoner, 'F', "GO:0005622");
		assertMapping("GO:0009881", "part_of", g, reasoner, 'P', "GO:0007165");
		
		
		Set<OWLClass> closure = BasicAnnotationPropagator.getIsaPartofSuperClassClosure(Collections.singleton(g.getOWLClassByIdentifier("GO:0006415")), g, reasoner);
		assertTrue(closure.contains(g.getOWLClassByIdentifier("GO:0006412")));
	}
	
	
	private void assertMapping(String source, String prop, OWLGraphWrapper g, OWLReasoner r, char aspect, String...mappings) {
		OWLClass c = g.getOWLClassByIdentifier(source);
		assertNotNull(c);
		OWLObjectProperty p = g.getOWLObjectPropertyByIdentifier(prop);
		assertNotNull(p);
		Set<OWLObjectProperty> properties = Collections.singleton(p);
		Set<OWLClass> superSet = null;
		if (aspect == 'P') {
			 superSet = r.getSubClasses(g.getOWLClassByIdentifier("GO:0008150"), false).getFlattened();
		}
		else if (aspect == 'F') {
			superSet = r.getSubClasses(g.getOWLClassByIdentifier("GO:0005575"), false).getFlattened();
		}
		
		Map<Set<OWLClass>, Set<OWLClass>> cache = new HashMap<Set<OWLClass>, Set<OWLClass>>();
		Set<OWLClass> linkedClasses = BasicAnnotationPropagator.getNonRedundantLinkedClasses(c, properties , g, r, superSet, cache);
		assertEquals(mappings.length, linkedClasses.size());
		for (String expected : mappings) {
			assertTrue(linkedClasses.contains(g.getOWLClassByIdentifier(expected)));
		}
	}
	
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
		
		List<Prediction> allPredictions = predictAnnotations(g, gafDocument);
		assertEquals(1, allPredictions.size());
		final GeneAnnotation geneAnnotation1 = allPredictions.get(0).getGeneAnnotation();
		
		GafWriter writer = new GafWriter();
		ByteArrayOutputStream out  = new ByteArrayOutputStream();
		writer.setStream(new PrintStream(out));
		writer.write(geneAnnotation1);
		out.flush();
		String writtenLine = out.toString().trim(); // trim to avoid hassle with tabs and new lines at the end
		String expectedLine = "GeneDB_Lmajor	LmjF.01.0770	LmjF.01.0770		GO:0006200	PMID:17087726	EXP		P	eukaryotic initiation factor 4a, putative	LmjF01.0770	gene	taxon:347515	20090402	GOC";
		assertEquals(expectedLine, writtenLine);
	}

	/**
	 * @param g
	 * @param gafDocument
	 * @return predictions
	 */
	private List<Prediction> predictAnnotations(OWLGraphWrapper g, GafDocument gafDocument) {
		AnnotationPredictor propagator = null;
		try {
			propagator = new BasicAnnotationPropagator(gafDocument, g);
			List<Prediction> allPredictions = propagator.getAllPredictions();
			return allPredictions;
		}
		finally {
			if(propagator != null) {
				propagator.dispose();
			}
		}
	}

}
