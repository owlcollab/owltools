package owltools.graph;

import static junit.framework.Assert.*;

import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import owltools.OWLToolsTestBasics;
import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;
import owltools.graph.OWLQuantifiedProperty;

public class TransitivePropertyTest extends OWLToolsTestBasics {

	@Test
	public void testDescendants() throws Exception {
		OWLGraphWrapper  g =  getGraph("transitive_property_test.owl");
		OWLObject f1 = g.getOWLObjectByIdentifier("FOO:1");
		OWLObject f2 = g.getOWLObjectByIdentifier("FOO:2");
		OWLObject f3 = g.getOWLObjectByIdentifier("FOO:3");

		boolean ok1 = false;
		boolean ok2 = false;
		int n = 0;
		for (OWLGraphEdge e : g.getOutgoingEdgesClosureReflexive(f1)) {
			if (e.getTarget() instanceof OWLClass) {
				System.out.println(e);
				if (e.getTarget().equals(f2)) {
					ok1 = true;
				}
				if (e.getTarget().equals(f3)) {
					ok2 = true;
				}
				n++;
			}
		}

		assertTrue(ok1);
		assertTrue(ok2);

		assertEquals(3, n);

	}

}
