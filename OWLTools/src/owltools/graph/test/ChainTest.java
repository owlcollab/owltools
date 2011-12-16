package owltools.graph.test;

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

import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;
import owltools.graph.OWLQuantifiedProperty;
import owltools.test.OWLToolsTestBasics;

public class ChainTest extends OWLToolsTestBasics {

	@Test
	public void testDescendants() throws Exception {
		OWLGraphWrapper  g =  getGraph("test_resources/multipath.obo");
		OWLObject a1 = g.getOWLObjectByIdentifier("A:1");
		OWLObject x1 = g.getOWLObjectByIdentifier("X:1");
		OWLObject z1 = g.getOWLObjectByIdentifier("Z:1");
		OWLObject y1 = g.getOWLObjectByIdentifier("Y:1");
		OWLObject y2 = g.getOWLObjectByIdentifier("Y:2");
		boolean ok1 = false;
		boolean ok2 = false;
		for (OWLGraphEdge e : g.getOutgoingEdgesClosureReflexive(x1)) {
			System.out.println(e);
			if (e.getTarget().equals(z1)) {
				if (e.getQuantifiedPropertyList().size() == 1) {
					OWLQuantifiedProperty qp = e.getFirstQuantifiedProperty();
					if (qp.isSubClassOf())
						ok1 = true;
					else if (qp.isSomeValuesFrom()) {
						ok2 = true;
					}
				}
			}
		}
		assertTrue(ok1);
		assertTrue(ok2);

		boolean ok3 = false;
		for (OWLObject d : g.queryDescendants((OWLClass)z1)) {
			System.out.println("z1 desc="+d);
			if (d.equals(x1)) {
				ok3 = true;
			}
		}
		assertTrue(ok3);

		ok3 = false;
		OWLObjectSomeValuesFrom qe = 
			g.getDataFactory().getOWLObjectSomeValuesFrom(g.getOWLObjectPropertyByIdentifier("BFO:0000050"), (OWLClassExpression) z1);
		System.out.println("QE="+qe);
		for (OWLObject d : g.queryDescendants(qe)) {
			System.out.println("part_of z1 desc="+d);
			if (d.equals(x1)) {
				ok3 = true;
			}
		}
		assertTrue(ok3);

		ok1 = false;
		ok2 = false;
		for (OWLGraphEdge e : g.getOutgoingEdgesClosureReflexive(a1)) {

			System.out.println("a1 anc="+e);
			if (e.getTarget().equals(y1)) {
				if (e.getQuantifiedPropertyList().size() == 1) {
					OWLQuantifiedProperty qp = e.getFirstQuantifiedProperty();
					if (qp.getPropertyId().equals("http://purl.obolibrary.org/obo/RO_0002131"))
						ok1 = true;
				}
			}
		}
		assertTrue(ok1);
		//assertTrue(ok2);

		ok3 = false;
		qe = 
			g.getDataFactory().getOWLObjectSomeValuesFrom(g.getOWLObjectPropertyByIdentifier("RO:0002131"), (OWLClassExpression) y1);
		System.out.println("QE="+qe);
		for (OWLObject d : g.queryDescendants(qe)) {
			System.out.println("overlaps y1 desc="+d);
			if (d.equals(a1)) {
				ok3 = true;
			}
		}
		assertTrue(ok3);


	}

}
