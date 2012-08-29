package org.geneontology.lego.dot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

import owltools.graph.OWLGraphWrapper;

public class CellularLocationTools {

	static OWLClassExpression searchCellularLocation(OWLClass cls, OWLGraphWrapper graph, Set<OWLObjectProperty> occurs_in) {
		Queue<OWLClass> queue = new Queue<OWLClass>();
		queue.add(cls);
		return searchCellularLocation(queue, graph, occurs_in);
	}
	
	private static OWLClassExpression searchCellularLocation(Queue<OWLClass> queue, OWLGraphWrapper graph, Set<OWLObjectProperty> occurs_in) {
		if (queue.isEmpty()) {
			return null;
		}
		List<OWLClass> nextLevel = new ArrayList<OWLClass>();
		while(!queue.isEmpty()) {
			OWLClass cls = queue.pop();
			for (OWLOntology ontology : graph.getAllOntologies()) {
				
				// equivalent classes
				Set<OWLEquivalentClassesAxiom> eqAxioms = ontology.getEquivalentClassesAxioms(cls);
				for (OWLEquivalentClassesAxiom axiom : eqAxioms) {
					Set<OWLClassExpression> expressions = axiom.getClassExpressionsMinus(cls);
					for (OWLClassExpression ce : expressions) {
						if (!ce.isAnonymous()) {
							nextLevel.add(ce.asOWLClass());
						}
						else if (ce instanceof OWLObjectSomeValuesFrom) {
							OWLObjectSomeValuesFrom expr = (OWLObjectSomeValuesFrom) ce;
							OWLObjectPropertyExpression propertyExpression = expr.getProperty();
							OWLClassExpression filler = expr.getFiller();
							if (occurs_in.contains(propertyExpression)) {
								return filler;
							}
							if (!filler.isAnonymous()) {
								nextLevel.add(filler.asOWLClass());
							}
						}
					}
				}
				
				// super classes
				Set<OWLSubClassOfAxiom> subAxioms = ontology.getSubClassAxiomsForSubClass(cls);
				for (OWLSubClassOfAxiom axiom : subAxioms) {
					OWLClassExpression ce = axiom.getSuperClass();
					if (!ce.isAnonymous()) {
						nextLevel.add(ce.asOWLClass());
					}
					else if (ce instanceof OWLObjectSomeValuesFrom) {
						OWLObjectSomeValuesFrom expr = (OWLObjectSomeValuesFrom) ce;
						OWLObjectPropertyExpression propertyExpression = expr.getProperty();
						OWLClassExpression filler = expr.getFiller();
						if (occurs_in.contains(propertyExpression)) {
							return filler;
						}
						if (!filler.isAnonymous()) {
							nextLevel.add(filler.asOWLClass());
						}
					}
				}
			}
		}
		queue.addAll(nextLevel);
		return searchCellularLocation(queue, graph, occurs_in);
	}
	
	private static class Queue<T> {
		
		private final Set<T> visited = new HashSet<T>();
		private final LinkedList<T> list = new LinkedList<T>();
		
		public synchronized T pop() {
			return list.removeFirst();
		}
		
		public synchronized boolean isEmpty() {
			return list.isEmpty();
		}
		
		public synchronized void addAll(Collection<T> c) {
			for (T t : c) {
				if (!visited.contains(t)) {
					list.add(t);
					visited.add(t);
				}
			}
		}
		
		public synchronized void add(T t) {
			if (!visited.contains(t)) {
				list.add(t);
				visited.add(t);
			}
		}
	}
}
