package owltools;

import static junit.framework.Assert.*;

import java.util.Set;

import org.junit.Test;
import org.semanticweb.owlapi.model.OWLObject;

import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

public class FlyTest {

	@Test
	public void testConvertXPs() throws Exception {
		ParserWrapper pw = new ParserWrapper();
		OWLGraphWrapper g =
			pw.parseToOWLGraph("http://purl.obolibrary.org/obo/fbbt.obo");
		OWLObject wmb = g.getOWLObjectByIdentifier("FBbt:00004326"); // wing margin bristle
		OWLObject eso = g.getOWLObjectByIdentifier("FBbt:00005168"); // external sensory organ
		
		Set<OWLObject> ancs = g.getAncestorsReflexive(wmb);
		assertTrue(ancs.contains(wmb)); // reflexivity test
		assertTrue(ancs.contains(eso)); //wing margin bristle --> external sensory organ
		
		for (OWLObject c : ancs) {
			System.out.println(g.getIdentifier(c)+" "+g.getLabel(c)+" URI:"+c);
		}
	}
	
}
