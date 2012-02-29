package owltools.graph;

import static junit.framework.Assert.*;

import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import owltools.OWLToolsTestBasics;
import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;
import owltools.graph.OWLGraphWrapper.Config;
import owltools.graph.OWLQuantifiedProperty;

public class GraphExclusionTest extends OWLToolsTestBasics {



	@Test
	public void testAllEdges() throws Exception {
		OWLGraphWrapper  g =  getGraph("graph_exclusion_test.owl");
		int n = 0;
		for (OWLObject obj : g.getAllOWLObjects()) {
			for (OWLGraphEdge e : g.getOutgoingEdgesClosure(obj)) {
				//System.out.println(e);
				n++;
			}
		}
		System.out.println(n);
		assertEquals(208, n);

		n = 0;
		boolean okTrunk = false;
		boolean okChain = false;
		OWLClass finger = g.getOWLClassByIdentifier("FOO:finger");
		OWLClass trunk = g.getOWLClassByIdentifier("FOO:trunk");
		OWLObjectProperty p = g.getOWLObjectProperty("http://purl.obolibrary.org/obo/FOO_part_of_has_part");
		for (OWLGraphEdge e : g.getOutgoingEdgesClosure(finger)) {
			//System.out.println(e);
			n++;
			if (e.getTarget().equals(trunk)) {
				okTrunk = true;
			}
			if (e.getSingleQuantifiedProperty().getProperty() != null) {
				//System.out.println(e.getSingleQuantifiedProperty().getProperty());
				if (e.getSingleQuantifiedProperty().getProperty().equals(p)) {
					okChain = true;
				}
			}
		}
		assertTrue(okTrunk);
		assertTrue(okChain);
	}

	@Test
	public void testAllEdgesWithFilter() throws Exception {
		OWLGraphWrapper  g =  getGraph("graph_exclusion_test.owl");
		Config conf = g.getConfig();
		OWLAnnotationProperty ap = (OWLAnnotationProperty) g.getOWLObjectByLabel("exclude me");
		conf.excludeAllWith(ap, g.getSourceOntology());
		for (OWLQuantifiedProperty qp : conf.graphEdgeExcludeSet) {
			System.out.println("  EXCLUDE="+qp);
		}
		int n = 0;
		for (OWLObject obj : g.getAllOWLObjects()) {
			for (OWLGraphEdge e : g.getOutgoingEdgesClosure(obj)) {
				//System.out.println(e);
				n++;
			}
		}
		System.out.println(n);
		//assertEquals(153, n);

		n = 0;
		boolean okTrunk = true;
		boolean okChain = true;
		OWLClass finger = g.getOWLClassByIdentifier("FOO:finger");
		OWLClass trunk = g.getOWLClassByIdentifier("FOO:trunk");
		OWLObjectProperty p = g.getOWLObjectPropertyByIdentifier("FOO:part_of_has_part");
		for (OWLGraphEdge e : g.getOutgoingEdgesClosure(finger)) {
			System.out.println(e);
			n++;
			if (e.getTarget().equals(trunk)) {
				okTrunk = true;
			}
			if (e.getSingleQuantifiedProperty().getProperty() != null &&
					e.getSingleQuantifiedProperty().getProperty().equals(p)) {
				okChain = false;
			}

		}
		assertTrue(okTrunk);
	}

}
