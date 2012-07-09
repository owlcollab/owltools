package owltools.graph;

import static junit.framework.Assert.*;

import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import owltools.OWLToolsTestBasics;

public class OWLGraphGOTest extends OWLToolsTestBasics {
	
	private static Logger LOG = Logger.getLogger(OWLGraphWrapper.class);

	/*
	 * Testing the some of the relation functions in the graph wrapper on GO.
	 */
	@Test
	public void testGOGraph() throws Exception{
		OWLGraphWrapper wrapper = getOntologyWrapper("go.owl");
		
		// First, let's look at the world of GO:0022008; specifically, the neighborhood above.
		OWLObject x1 = wrapper.getOWLClassByIdentifier("GO:0022008");

		// In this loop we're checking that GO:0022008 has two known parents, one is_a and one part_of.
		// Anything else is an error.
		for (OWLGraphEdge e : wrapper.getOutgoingEdges(x1)) {
			OWLObject t = e.getTarget();

			if (t instanceof OWLNamedObject){				

				// Figure out object the bits.
				String objectID = wrapper.getIdentifier(t);
				//String objectLabel = wrapper.getLabel(t);
				String elabel = wrapper.getEdgeLabel(e);

				if( objectID.equals("GO:0030154") ){
					assertEquals("GO:0030154 part_of parent of GO_0022008:", elabel, "is_a");					
				}else if( objectID.equals("GO:0007399") ){
					assertEquals("GO:0007399 part_of parent of GO_0022008:", elabel, "part_of");
				}else{
					fail("not a parent of GO_0022008: " + objectID);
				}
			}
		}
		
		// Second, let's look at the world of GO:0007399; specifically, the neighborhood below.
		OWLObject x2 = wrapper.getOWLClassByIdentifier("GO:0007399");

		// In this loop we're checking that GO:0007399, known from above, has GO:0022008 as a part_of child somewhere.
		// Anything else is an error.
		boolean kid_p = false;
		final Set<OWLGraphEdge> incomingEdges = wrapper.getIncomingEdges(x2);
		for (OWLGraphEdge e : incomingEdges) {
			OWLObject s = e.getSource();
			
			if (s instanceof OWLNamedObject){				

				// Figure out subject the bits.
				String subjectID = wrapper.getIdentifier(s);
				//String subjectLabel = wrapper.getLabel(s);
				String elabel = wrapper.getEdgeLabel(e);

				if( subjectID.equals("GO:0022008") ){
					assertEquals("GO:0022008 part_of child of GO_0007399 (saw: " + elabel + ")", elabel, "part_of");
					kid_p = true;
				}
			}
		}
		assertTrue("saw GO:0022008 as a child of GO_0007399:", kid_p);
		
		
		// Third, lets make sure that the closure for isa_partof is actually getting everything.
		OWLObject x3 = wrapper.getOWLClassByIdentifier("GO:0022008");
		Map<String, String> closure_map = wrapper.getIsaPartofClosureMap(x3);
		
		
		// And make sure they are the right ones.
		int tally = 0;
		for( String key : closure_map.keySet() ){
			//LOG.info("key: " + key + ", label: " + closure_map.get(key));
			assertNotNull("the closure ids should never be null", key);
			if( key != null ){
				if( key.equals("GO:0022008") ||
					key.equals("GO:0030154") ||
					key.equals("GO:0007399") ||
					key.equals("GO:0048731") ||
					key.equals("GO:0048869") ||
					key.equals("GO:0048856") ||
					key.equals("GO:0007275") ||
					key.equals("GO:0009987") ||
					key.equals("GO:0032502") ||
					key.equals("GO:0032501") ||
					key.equals("GO:0008150") ){
					tally++;
				}
			}
		}
		// Do we have the right number and right allocation of ancestors?
		assertEquals("have 11 ids in closure", 11, closure_map.keySet().size());
		assertEquals("have correct 11 ids in closure", 11, tally);
		
	}
	
	private OWLGraphWrapper getOntologyWrapper(String file) throws OWLOntologyCreationException{
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology ontology = manager.loadOntologyFromOntologyDocument(getResource(file));
		return new OWLGraphWrapper(ontology);
	}
}
