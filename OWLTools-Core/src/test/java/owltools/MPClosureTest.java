package owltools;

import static junit.framework.Assert.*;

import java.util.Set;

import org.junit.Test;
import org.semanticweb.owlapi.model.OWLObject;

import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

public class MPClosureTest {

	@Test
	public void testConvertXPs() throws Exception {
		ParserWrapper pw = new ParserWrapper();
		OWLGraphWrapper g =
			pw.parseToOWLGraph("http://purl.obolibrary.org/obo/mp.owl");
		//OWLOntology ont = g.getOntology();

		OWLObject aer = g.getOWLObjectByIdentifier("MP:0001676"); // apical ectoderm ridge
		OWLObject emb = g.getOWLObjectByIdentifier("MP:0001672"); // abnormal embryogenesis/ development
		
		Set<OWLObject> ancs = g.getAncestorsReflexive(aer);
		assertTrue(ancs.contains(aer)); // reflexivity test
		
		for (OWLObject c : ancs) {
			System.out.println(g.getIdentifier(c));
		}
	}
	
}
