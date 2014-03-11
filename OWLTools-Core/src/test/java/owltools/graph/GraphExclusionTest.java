package owltools.graph;

import static org.junit.Assert.*;

import java.util.Set;

import org.junit.Test;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;

import owltools.OWLToolsTestBasics;
import owltools.graph.OWLGraphWrapperEdges.Config;

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
		assertEquals(245, n);

		n = 0;
		boolean okTrunk = false;
		boolean okChain = false;
		OWLClass finger = g.getOWLClassByIdentifier("FOO:finger");
		OWLClass trunk = g.getOWLClassByIdentifier("FOO:trunk");
		OWLObjectProperty partOfHasPart = g.getOWLObjectProperty("http://purl.obolibrary.org/obo/FOO_part_of_has_part");
		for (OWLGraphEdge e : g.getOutgoingEdgesClosure(finger)) {
			//System.out.println(e);
			n++;
			if (e.getTarget().equals(trunk)) {
				// partOf some hand partOf some limb adjacentTo some trunk
				okTrunk = true;
			}
			if (e.getSingleQuantifiedProperty().getProperty() != null) {
				//System.out.println(e.getSingleQuantifiedProperty().getProperty());
				if (e.getSingleQuantifiedProperty().getProperty().equals(partOfHasPart)) {
					okChain = true;
				}
			}
		}
		assertTrue(okTrunk);
		assertTrue(okChain);
	}

	@Test
	public void testAllEdgesWithExclusionFilter() throws Exception {
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
			System.out.println("FILTERED: "+e);
			n++;
			if (e.getTarget().equals(trunk)) {
				// partOf some hand partOf some limb adjacentTo some trunk
				System.err.println(" This edge should have been filtered: "+e);
				okTrunk = false;
			}
			if (e.getSingleQuantifiedProperty().getProperty() != null &&
					e.getSingleQuantifiedProperty().getProperty().equals(p)) {
				okChain = false;
			}

		}
		assertTrue(okTrunk);
		assertTrue(okChain);
	}
	
	@Test
	public void testAllEdgesWithInclusionFilter() throws Exception {
		OWLGraphWrapper  g =  getGraph("graph_exclusion_test.owl");
		Config conf = g.getConfig();
		OWLAnnotationProperty ap = (OWLAnnotationProperty) g.getOWLObjectByLabel("include me");
		conf.includeAllWith(ap, g.getSourceOntology());
		for (OWLQuantifiedProperty qp : conf.graphEdgeIncludeSet) {
			System.out.println("  INCLUDE="+qp);
		}
		boolean isExcluded = true;
		int n = 0;
		for (OWLObject obj : g.getAllOWLObjects()) {
			for (OWLGraphEdge e : g.getOutgoingEdgesClosure(obj)) {
				for (OWLQuantifiedProperty qp : e.getQuantifiedPropertyList()) {
					if (qp.getProperty() != null && qp.getProperty().getIRI().toString().contains("part_of_has_part")) {
						isExcluded = false;
					}
				}
				//System.out.println(e);
				n++;
			}
		}
		assertTrue(isExcluded);
		System.out.println("TOTAL EDGES: "+n);
		assertTrue(n>0);
		//assertEquals(153, n);

		n = 0;
		boolean okTrunk = true;
		boolean okChain = true;
		OWLClass finger = g.getOWLClassByIdentifier("FOO:finger");
		OWLClass trunk = g.getOWLClassByIdentifier("FOO:trunk");
		OWLObjectProperty partOfHasPart = g.getOWLObjectPropertyByIdentifier("FOO:part_of_has_part");
		for (OWLGraphEdge e : g.getOutgoingEdgesClosure(finger)) {
			System.out.println("INCL-FILTERED: "+e);
			n++;
			if (e.getTarget().equals(trunk)) {
				// partOf some hand partOf some limb adjacentTo some trunk
				System.err.println(" This edge should have been filtered: "+e);
				okTrunk = false;
			}
			if (e.getSingleQuantifiedProperty().getProperty() != null &&
					e.getSingleQuantifiedProperty().getProperty().equals(partOfHasPart)) {
				okChain = false;
			}

		}
		assertTrue(okTrunk);
		assertTrue(okChain);
	}
	
	@Test
	public void testReflexive() throws Exception {
		OWLGraphWrapper  g =  getGraph("graph_exclusion_test.owl");
		Config conf = g.getConfig();
		OWLAnnotationProperty ap = (OWLAnnotationProperty) g.getOWLObjectByLabel("include me");
		conf.includeAllWith(ap, g.getSourceOntology());
		for (OWLQuantifiedProperty qp : conf.graphEdgeIncludeSet) {
			System.out.println("  INCLUDE="+qp);
		}
		
		OWLClass finger = g.getOWLClassByIdentifier("FOO:finger");
		Set<OWLGraphEdge> edgesR = g.getCompleteEdgesBetween(finger, finger);
		System.out.println("RE="+edgesR);
		
	}

}
