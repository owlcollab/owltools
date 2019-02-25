package owltools;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

import owltools.graph.OWLGraphWrapper;

public class TBoxUnFoldingToolTest extends OWLToolsTestBasics {
	
	private static OWLGraphWrapper graph;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		Logger.getLogger(TBoxUnFoldingTool.class).setLevel(Level.DEBUG);
		graph = getGraph("unfold/mini_go_slim.owl");
	}

	@Test
	public void testUnfold() throws Exception {
		
		Set<String> parents = Collections.singleton("GO:0008150"); // biological_process
		TBoxUnFoldingTool unFoldingTool = new TBoxUnFoldingTool(graph, parents, InferenceBuilder.REASONER_ELK);
		
		String id = "GO:2000606"; // regulation of cell proliferation involved in mesonephros development
		
		String unfold = unFoldingTool.unfoldToString(id);
		
		assertEquals("EquivalentClasses(GO:2000606 'regulation of cell proliferation involved in mesonephros development' ObjectIntersectionOf(GO:0065007 'biological regulation' ObjectSomeValuesFrom(RO:0002211 'regulates' ObjectIntersectionOf(GO:0008283 'cell proliferation' ObjectSomeValuesFrom(BFO:0000050 'part_of' GO:0001823 'mesonephros development')))) )",
				unfold);
	}
	
}
