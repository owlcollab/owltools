package owltools.graph;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.obolibrary.oboformat.parser.OBOFormatParserException;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

import owltools.OWLToolsTestBasics;
import owltools.graph.OWLQuantifiedProperty.Quantifier;

public class OWLGraphWrapperEdgeTest extends OWLToolsTestBasics {

	@Test
	public void testEdges() throws Exception {
		OWLGraphWrapper wrapper = getGraph("graph/foo.obo");
		
		OWLObject x1 = wrapper.getOWLClassByIdentifier("FOO:0004");
		
		
		for (OWLGraphEdge e : wrapper.getOutgoingEdges(x1)) {
			OWLObject t = e.getTarget();

			if (t instanceof OWLNamedObject){				

				// Figure out object the bits.
				String objectID = wrapper.getIdentifier(t);
				String elabel = wrapper.getEdgeLabel(e);

				if( objectID.equals("FOO:0002") ){
					assertEquals("FOO:0002 is_a parent of FOO:0004:", elabel, "is_a");					
				}else if( objectID.equals("FOO:0003") ){
					assertEquals("FOO:0003 part_of parent of FOO:0004:", elabel, "part_of");
				}else{
					fail("not a parent of FOO:0004: " + objectID);
				}
			}
		}
		
		OWLObject x2 = wrapper.getOWLClassByIdentifier("FOO:0003");
		
		boolean kid_p = false;
		final Set<OWLGraphEdge> incomingEdges = wrapper.getIncomingEdges(x2);
		assertEquals("expects exactly one relation", 1, incomingEdges.size());
		for (OWLGraphEdge e : incomingEdges) {
			
			// TODO is it the source or the target?
			OWLObject s = e.getSource();
			if (s instanceof OWLNamedObject){				

				// Figure out subject the bits.
				String subjectID = wrapper.getIdentifier(s);
				//String subjectLabel = wrapper.getLabel(s);
				String elabel = wrapper.getEdgeLabel(e);

				if( subjectID.equals("FOO:0004") ){
					assertEquals("FOO:0004 part_of child of FOO:0003 (saw: " + elabel + ")", elabel, "part_of");
					kid_p = true;
				}
			}
		}
		
		assertTrue("require FOO:0004 as a part_of child of FOO:003:", kid_p);
		
	}
	
	@Test
	public void testEdgeCache() throws Exception {
		OWLGraphWrapper g = getGraph("graph/cache-test.obo");
		
		OWLOntology o = g.getSourceOntology();
		OWLOntologyManager m = o.getOWLOntologyManager();
		OWLDataFactory f = m.getOWLDataFactory();
		
		OWLClass orphan = g.getOWLClassByIdentifier("FOO:0004");
		OWLClass root = g.getOWLClassByIdentifier("FOO:0001");
		
		g.getEdgesBetween(orphan, root); //just to trigger the cache
		
		OWLSubClassOfAxiom ax = f.getOWLSubClassOfAxiom(orphan, root);
		AddAxiom addAx = new AddAxiom(o, ax);
		m.applyChange(addAx);
		
		Set<OWLGraphEdge> edges = g.getEdgesBetween(orphan, root);
		assertNotNull(edges);
		
		assertEquals(0, edges.size());
		
		g.clearCachedEdges(); // test clear cache method
		
		edges = g.getEdgesBetween(orphan, root);
		assertNotNull(edges);
		
		assertEquals(1, edges.size());
	}
	
	/**
	 * Test the behavior of {@code OWLGraphEdge}s to store their underlying 
	 * {@code OWLAxiom}s (different {@code OWLAxiom}s can produce a same 
	 * {@code OWLGraphEdge}, which will store these {@code OWLAxiom}s).
	 */
	@Test
	public void testOWLGraphEdgeAxioms() throws OWLOntologyCreationException, OBOFormatParserException, IOException {
        OWLGraphWrapper g = getGraph("graph/owlgraphedge.obo");
        
        OWLOntology o = g.getSourceOntology();
        OWLOntologyManager m = o.getOWLOntologyManager();
        OWLDataFactory f = m.getOWLDataFactory();
        
        
        
        OWLClass root = g.getOWLClassByIdentifier("FOO:0001");
        OWLClass cls1 = g.getOWLClassByIdentifier("FOO:0003");
        
        Set<OWLGraphEdge> edges = g.getOutgoingEdges(cls1);
        Set<OWLGraphEdge> expectedEdges = new HashSet<OWLGraphEdge>();
        expectedEdges.add(new OWLGraphEdge(cls1, root, g.getSourceOntology()));
        assertEquals("Incorrect OWLGraphEdges generated for Foo:0003", expectedEdges, edges);
        
        OWLGraphEdge edge = edges.iterator().next();
        Set<OWLAxiom> expectedAxioms = new HashSet<OWLAxiom>();
        expectedAxioms.add(f.getOWLSubClassOfAxiom(cls1, root));
        expectedAxioms.add(f.getOWLEquivalentClassesAxiom(cls1, root));
        assertEquals("Incorrect underlying OWLAxioms", expectedAxioms, edge.getAxioms());
        

        
        OWLClass cls2 = g.getOWLClassByIdentifier("FOO:0004");
        OWLClass cls3 = g.getOWLClassByIdentifier("FOO:0002");
        
        edges = g.getOutgoingEdges(cls2);
        expectedEdges = new HashSet<OWLGraphEdge>();
        OWLGraphEdge edge1 = new OWLGraphEdge(cls2, cls3, g.getSourceOntology());
        OWLObjectSomeValuesFrom clsExp = f.getOWLObjectSomeValuesFrom(
                g.getOWLObjectPropertyByIdentifier("BFO:0000050"), cls1);
        OWLGraphEdge edge2 = new OWLGraphEdge(cls2, cls1, clsExp.getProperty(), 
                OWLQuantifiedProperty.Quantifier.SOME, g.getSourceOntology());
        expectedEdges.add(edge1);
        expectedEdges.add(edge2);
        assertEquals("Incorrect OWLGraphEdges generated for Foo:0004", expectedEdges, 
                edges);
        
        OWLAxiom equivAx = f.getOWLEquivalentClassesAxiom(cls2, 
                f.getOWLObjectIntersectionOf(cls3, clsExp));
        for (OWLGraphEdge edgeToTest: edges) {
            expectedAxioms = new HashSet<OWLAxiom>();
            if (edgeToTest.equals(edge1)) {
                expectedAxioms.add(f.getOWLSubClassOfAxiom(cls2, cls3));
                expectedAxioms.add(equivAx);
            } else if (edgeToTest.equals(edge2)) {
                expectedAxioms.add(f.getOWLSubClassOfAxiom(cls2, clsExp));
                expectedAxioms.add(equivAx);
                
            }
            assertEquals("Incorrect underlying OWLAxioms", expectedAxioms, edgeToTest.getAxioms());
        }
        
        
        
        edges = g.getOutgoingEdges(cls3);
        expectedEdges = new HashSet<OWLGraphEdge>();
        expectedEdges.add(new OWLGraphEdge(cls3, root, g.getSourceOntology()));
        assertEquals("Incorrect OWLGraphEdges generated for Foo:0002", expectedEdges, edges);
        
        edge = edges.iterator().next();
        expectedAxioms = new HashSet<OWLAxiom>();
        expectedAxioms.add(f.getOWLSubClassOfAxiom(cls3, root));
        
        Set<OWLAnnotation> annots = new HashSet<OWLAnnotation>();
        annots.add(f.getOWLAnnotation(
            f.getOWLAnnotationProperty(IRI.create("http://www.geneontology.org/formats/oboInOwl#source")), 
            f.getOWLLiteral("mysource")));
        expectedAxioms.add(f.getOWLSubClassOfAxiom(cls3, root, annots));
        
        assertEquals("Incorrect underlying OWLAxioms", expectedAxioms, edge.getAxioms());
        
        

        OWLClass cls4 = g.getOWLClassByIdentifier("FOO:0005");
        OWLClass cls5 = g.getOWLClassByIdentifier("FOO:0006");
        
        edges = g.getOutgoingEdges(cls5);
        expectedEdges = new HashSet<OWLGraphEdge>();
        expectedEdges.add(new OWLGraphEdge(cls5, root, g.getSourceOntology()));
        assertEquals("Incorrect OWLGraphEdges generated for Foo:0006", expectedEdges, edges);
        
        edge = edges.iterator().next();
        expectedAxioms = new HashSet<OWLAxiom>();
        expectedAxioms.add(f.getOWLSubClassOfAxiom(cls5, root));
        
        equivAx = f.getOWLEquivalentClassesAxiom(root, 
                f.getOWLObjectUnionOf(cls4, cls5));
        expectedAxioms.add(equivAx);
        
        assertEquals("Incorrect underlying OWLAxioms", expectedAxioms, edge.getAxioms());
        
        
        
        edges = g.getOutgoingEdges(cls4);
        expectedEdges = new HashSet<OWLGraphEdge>();
        expectedEdges.add(new OWLGraphEdge(cls4, root, g.getSourceOntology()));
        assertEquals("Incorrect OWLGraphEdges generated for Foo:0005", expectedEdges, edges);
        
        edge = edges.iterator().next();
        expectedAxioms = new HashSet<OWLAxiom>();
        expectedAxioms.add(equivAx);
        
        assertEquals("Incorrect underlying OWLAxioms", expectedAxioms, edge.getAxioms());
        
        
//        OWLIndividual ind = g.getOWLIndividualByIdentifier("john");
//        
//        edges = g.getOutgoingEdges(ind);
//        expectedEdges = new HashSet<OWLGraphEdge>();
//        expectedEdges.add(new OWLGraphEdge(ind, f.getOWLClassAssertionAxiom(root, ind), 
//                null, Quantifier.INSTANCE_OF, g.getSourceOntology()));
//        assertEquals("Incorrect OWLGraphEdges generated for John", expectedEdges, edges);
//        
//        edge = edges.iterator().next();
//        expectedAxioms = new HashSet<OWLAxiom>();
//        expectedAxioms.add(f.getOWLClassAssertionAxiom(root, ind));
//        
//        assertEquals("Incorrect underlying OWLAxioms", expectedAxioms, edge.getAxioms());
        
	}
	
}
