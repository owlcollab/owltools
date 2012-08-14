package owltools.ontologyverification.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

import owltools.graph.OWLGraphWrapper;
import owltools.ontologyverification.CheckWarning;
import owltools.util.Adjacency;
import owltools.util.MappingTarjan;
import owltools.util.Tarjan;

/**
 * Simple cycle check using the asserted super/sub class relations.
 * Uses the Tarjan algorithm to identify strongly connected components.
 * Strongly connected components with more than one node are cycles.
 */
public class CycleCheck extends AbstractCheck {

	public CycleCheck() {
		super("CYCLE_CHECK", "Cycle Check", false);
	}
	
	@Override
	public Collection<CheckWarning> check(OWLGraphWrapper graph, Collection<OWLObject> allOwlObjects) {
		Collection<CheckWarning> out = new ArrayList<CheckWarning>();
		
		// find strongly connected components using the Tarjan algorithm
		Tarjan<OWLClass> tarjan = new MappingTarjan<OWLClass>();
		List<List<OWLClass>> scc = tarjan.executeTarjan(new OWLClassAdjacency(graph, allOwlObjects));
		
		// check all strongly connected components
		// if size > 1 its in a cycle
		for (List<OWLClass> component : scc) {
			final int size = component.size();
			if (size > 1) {
				// cycle
				// create message
				StringBuilder sb = new StringBuilder("Cycle detected with the following classes: ");
				List<IRI> iris = new ArrayList<IRI>(size);
				for (OWLClass owlClass : component) {
					if (!iris.isEmpty()) {
						sb.append(", ");
					}
					final IRI iri = owlClass.getIRI();
					iris.add(iri);
					sb.append(graph.getIdentifier(iri));
					
					final String label = graph.getLabel(owlClass);
					if (label != null) {
						sb.append(" (");
						sb.append(label);
						sb.append(")");
					}
					
				}
				CheckWarning warning = new CheckWarning(getID(), sb.toString(), isFatal(), iris, null);
				out.add(warning);
			}
		}
		
		return out;
	}
	
	static class OWLClassAdjacency implements Adjacency<OWLClass> {
		
		private final OWLGraphWrapper graph;
		private final Collection<OWLObject> allOwlObjects;

		/**
		 * @param graph
		 * @param allOwlObjects 
		 */
		OWLClassAdjacency(OWLGraphWrapper graph, Collection<OWLObject> allOwlObjects) {
			this.graph = graph;
			this.allOwlObjects = allOwlObjects;
		}

		@Override
		public List<OWLClass> getAdjacent(OWLClass cls) {
			Set<OWLClass> results = new HashSet<OWLClass>();
			Set<OWLOntology> allOntologies = graph.getAllOntologies();
			for (OWLOntology owlOntology : allOntologies) {
				Set<OWLSubClassOfAxiom> axioms = owlOntology.getSubClassAxiomsForSubClass(cls);
				if (axioms != null && !axioms.isEmpty()) {
					for (OWLSubClassOfAxiom axiom : axioms) {
						OWLClassExpression expression = axiom.getSuperClass();
						if (!expression.isAnonymous()) {
							results.add(expression.asOWLClass());
						}
					}
				}
			}
			if (results.isEmpty()) {
				return Collections.emptyList();
			}
			return new ArrayList<OWLClass>(results);
		}

		@Override
		public Iterable<OWLClass> getSources() {
			return new Iterable<OWLClass>() {
				
				@Override
				public Iterator<OWLClass> iterator() {
					final Iterator<OWLObject> allIterator = allOwlObjects.iterator();
					return new Iterator<OWLClass>() {

						private OWLClass next;
						
						@Override
						public boolean hasNext() {
							findNext();
							return next != null;
						}

						@Override
						public OWLClass next() {
							if( !hasNext() )
								throw new NoSuchElementException();

							OWLClass result = next;
							next = null;
							
							return result;
						}

						@Override
						public void remove() {
							throw new UnsupportedOperationException();
						}
						
						private void findNext() {
							if( next != null )
								return;
							
							while( allIterator.hasNext() ) {
								OWLObject owlObject = allIterator.next();
								if (owlObject instanceof OWLClass) {
									next = (OWLClass) owlObject;
									return;
								}
							}		
							next = null;
						}
					};
				}
			};
		}
	}
}
