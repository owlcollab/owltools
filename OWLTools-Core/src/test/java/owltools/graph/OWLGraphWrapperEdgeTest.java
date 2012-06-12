package owltools.graph;

import static junit.framework.Assert.*;

import java.util.Set;

import org.junit.Test;
import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.model.OWLObject;

import owltools.OWLToolsTestBasics;

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
	
}
