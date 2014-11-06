package owltools.graph;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.model.OWLObject;

import owltools.OWLToolsTestBasics;
import owltools.graph.shunt.OWLShuntGraph;
import owltools.graph.shunt.OWLShuntNode;
import owltools.io.ParserWrapper;

public class OWLGraphGOTest extends OWLToolsTestBasics {
	
	private static Logger LOG = Logger.getLogger(OWLGraphWrapper.class);
	static OWLGraphWrapper wrapper;

    @BeforeClass
    public static void setUp() throws Exception {
    	wrapper = getOntologyWrapper("go.owl");
    }
	
	/*
	 * Testing the some of the relation functions in the graph wrapper on GO.
	 * First, let's look at the world of GO:0022008; specifically, the neighborhood above.
	 */
	@Test
	public void testGOGraph1() throws Exception{
		
		//
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
	}
	
	/*
	 * Testing the some of the relation functions in the graph wrapper on GO.
	 * Second, let's look at the world of GO:0007399; specifically, the neighborhood below.
	 */
	@Test
	public void testGOGraph2() throws Exception{
	
		// 
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
	}
		
	/*
	 * Third, lets make sure that the closure for isa_partof is actually getting everything.
	 */
	@Test
	public void testGOGraph3() throws Exception{
		
		// 
		OWLObject x3 = wrapper.getOWLClassByIdentifier("GO:0022008");
		Map<String, String> closure_map = wrapper.getRelationClosureMap(x3, RelationSets.getRelationSet(RelationSets.ISA_PARTOF));
		
		
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
	
	/*
	 * The idea here is to simulate/recreate the current situation in addTransitiveAncestorsToShuntGraph
	 * where is seems to be dropping a bunch of the expected nodes in one case: GO:0000124.
	 * https://github.com/kltm/amigo/issues/52
	 */
	@Test
	public void testClosureTruth() throws Exception{
		
		HashSet<String> ids = closureTestRunner(wrapper, true);

		LOG.info("remainder: " + ids.size());
		for( String id : ids ){
			LOG.info("remainder id: " + id);
		}
		assertEquals("the correct 24 entities were in the closure", 0, ids.size());
	}

	private HashSet<String> closureTestRunner(OWLGraphWrapper gw, boolean runAssertionsP){
		
		// The nodes we should have.
		HashSet<String> ids = new HashSet<String>();
		ids.add("GO:0032991");
		ids.add("GO:0000123");
		ids.add("GO:0044451");
		ids.add("GO:0044464");
		ids.add("GO:0031974");
		ids.add("GO:0043231");
		ids.add("GO:0031981");
		ids.add("GO:0005575");
		ids.add("GO:0044422");
		ids.add("GO:0070013");
		ids.add("GO:0000124");
		ids.add("GO:0070461");
		ids.add("GO:0044424");
		ids.add("GO:0043234");
		ids.add("GO:0005634");
		ids.add("GO:0043229");
		ids.add("GO:0043233");
		ids.add("GO:0044446");
		ids.add("GO:0043227");
		ids.add("GO:0005623");
		ids.add("GO:0044428");
		ids.add("GO:0043226");
		ids.add("GO:0005622");
		ids.add("GO:0005654");

		OWLObject saga = gw.getOWLClassByIdentifier("GO:0000124");
		String topicID = gw.getIdentifier(saga);
		
		List<String> rel_ids = RelationSets.getRelationSet(RelationSets.COMMON);
		OWLShuntGraph graphSegment = new OWLShuntGraph();
		graphSegment = gw.addTransitiveAncestorsToShuntGraph(saga, graphSegment, rel_ids);
		
		assertTrue(graphSegment.nodes.size() >= ids.size());
		
		for(OWLShuntNode node : graphSegment.nodes) {
			ids.remove(node.id);
		}
		ids.remove(topicID);
		
		return ids;	
	}
	
	private static OWLGraphWrapper getOntologyWrapper(String file) throws Exception{
		ParserWrapper pw = new ParserWrapper();
		OWLGraphWrapper graph = pw.parseToOWLGraph(getResourceIRIString(file));
		return graph;
	}
}
