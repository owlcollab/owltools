package owltools.graph;

import static junit.framework.Assert.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import owltools.OWLToolsTestBasics;

public class OWLGraphGOTest extends OWLToolsTestBasics {
	
	private static Logger LOG = Logger.getLogger(OWLGraphWrapper.class);
	static OWLGraphWrapper wrapper;

    @BeforeClass
    public static void setUp() {
    	try {
    		wrapper = getOntologyWrapper("go.owl");
		} catch (OWLOntologyCreationException e) {
			// Really shouldn't be here in test land.
			e.printStackTrace();
		}
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
	
	/*
	 * The idea here is to simulate/recreate the current situation in addTransitiveAncestorsToShuntGraph
	 * where is seems to be dropping a bunch of the expected nodes in one case: GO:0000124.
	 * https://github.com/kltm/amigo/issues/52
	 */
	@Ignore("we should add this once getOutgoingEdgesClosure is fixed") @Test
	public void testClosureTruth() throws Exception{
		closureTestRunner(wrapper, true);
	}

	/*
	 * The idea here is to make sure that nonDeterministicTestRunner is deterministic.
	 * We discovered that it sometimes gives a different answer, so we're seeing that it's
	 * at least consistant (even if not correct).
	 * 
	 * BUG: This function here DOES NOT catch the error, but it will hopefully slow the testing down enough that you
	 * come here and start looking into this again.
	 * 
	 * To reiterate -- this was failing, with witnesses, but only in repeated uses of restarting the VM (e.g. a new debug run in Eclipse)
	 */
	@Ignore("NOT a real test, but a placeholder, see comments") @Test
	public void testGraphDeterminism() throws Exception{

		
		int last_run = -1;
		for( int x = 1; x <= 10; x++ ){
			OWLGraphWrapper newWrapper = getOntologyWrapper("go.owl");
			int ndr = closureTestRunner(newWrapper, false);
			LOG.info("run with (" + x + "): " + ndr);
			if( last_run == -1 ){
				last_run = ndr;
			}else{
				assertEquals("this answer should always be the same", last_run, ndr);				
			}
		}		
	}

	// The runner for one of our more problematic test cases.
	// Sometimes returns 18, 30, 40, 42...a lot of things.
	private int closureTestRunner(OWLGraphWrapper gw, boolean runAssertionsP){
		
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

		int unfiltered_tally = 0;
		int filtered_tally = 0;

		OWLObject saga = gw.getOWLClassByIdentifier("GO:0000124");
		String topicID = gw.getIdentifier(saga);

		ArrayList<String> rel_ids = new ArrayList<String>();
		rel_ids.add("BFO:0000050");
		rel_ids.add("RO:0002211");
		rel_ids.add("RO:0002212");
		rel_ids.add("RO:0002213");
		HashSet<OWLObjectProperty> props = gw.relationshipIDsToPropertySet(rel_ids);
		Set<OWLGraphEdge> oge = gw.getOutgoingEdgesClosure(saga);
		for( OWLGraphEdge e : oge ){
			OWLObject target = e.getTarget();
			
			String rel = gw.classifyRelationship(e, target, props);
			//LOG.info("id: " + gw.getIdentifier(target) + ", " + rel);

			unfiltered_tally++;

			if( rel != null ){
				//LOG.info("\tclass" + rel);

				// Placeholders.
				String objectID = null;
				
				if( rel == "simplesubclass" ){
					objectID = gw.getIdentifier(target);
				}else if( rel == "typesubclass" ){
					OWLObjectSomeValuesFrom some = (OWLObjectSomeValuesFrom)target;
					OWLClassExpression clsexp = some.getFiller();
					OWLClass cls = clsexp.asOWLClass();
					objectID = gw.getIdentifier(cls);
				}

				//LOG.info("\t" + objectID);

				// Only add when subject, object, and relation are properly defined.
				if(	topicID != null && ! topicID.equals("") &&
					objectID != null &&	! objectID.equals("") ){

					// Remainder set removal.
					ids.remove(topicID);
					ids.remove(objectID);
					
					filtered_tally++;
				}
			}
		}
		
		// Our internal tests.
		if( runAssertionsP ){
			LOG.info("unfiltered tally: " + unfiltered_tally);
			LOG.info("\"correctness\" filtered tally: " + filtered_tally);
			LOG.info("remainder: " + ids.size());
			for( String id : ids ){
				LOG.info("remainder id: " + id);
			}
			assertEquals("have 24 ids in closure", 24, filtered_tally);
			assertEquals("the correct 24 entities were in the closure", 0, ids.size());
		}
			
		return filtered_tally;	
	}
	
	private static OWLGraphWrapper getOntologyWrapper(String file) throws OWLOntologyCreationException{
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology ontology = manager.loadOntologyFromOntologyDocument(getResource(file));
		return new OWLGraphWrapper(ontology);
	}
}
