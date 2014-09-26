package owltools;

import static org.junit.Assert.*;

import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLObject;

import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

public class CycleTest {

	@Test
	@Ignore("This test requires an external resource. This can lead to false positive failures.")
	public void testConvertXPs() throws Exception {
		ParserWrapper pw = new ParserWrapper();
		OWLGraphWrapper g =
			pw.parseToOWLGraph("http://purl.obolibrary.org/obo/fbbt.obo");
		OWLObject c = g.getOWLObjectByIdentifier("FBbt:00005048"); // tracheolar cell
		
		Set<OWLObject> ancs = g.getAncestorsReflexive(c);
		//assertTrue(ancs.contains(wmb)); // reflexivity test
		//assertTrue(ancs.contains(eso)); //wing margin bristle --> external sensory organ
		
		for (OWLObject a : ancs) {
			System.out.println(g.getIdentifier(a)+" "+g.getLabel(a));
		}
	}
	
}
