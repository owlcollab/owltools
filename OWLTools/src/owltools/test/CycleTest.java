package owltools.test;

import static junit.framework.Assert.*;

import java.io.IOException;
import java.util.Set;

import org.junit.Test;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

public class CycleTest {

	@Test
	public void testConvertXPs() throws IOException, OWLOntologyCreationException, OWLOntologyStorageException {
		ParserWrapper pw = new ParserWrapper();
		OWLGraphWrapper g =
			pw.parseToOWLGraph("http://purl.org/obo/obo/FBbt.obo");
		OWLObject c = g.getOWLObjectByIdentifier("FBbt:00005048"); // tracheolar cell
		
		Set<OWLObject> ancs = g.getAncestorsReflexive(c);
		//assertTrue(ancs.contains(wmb)); // reflexivity test
		//assertTrue(ancs.contains(eso)); //wing margin bristle --> external sensory organ
		
		for (OWLObject a : ancs) {
			System.out.println(g.getIdentifier(a)+" "+g.getLabel(a));
		}
	}
	
}
