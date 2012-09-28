package org.bbop.graph.collapse;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.bbop.graph.LinkDatabase;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.graph.OWLGraphWrapper;

public class DefaultLinkDatabase implements LinkDatabase {

	private final OWLGraphWrapper graph;
	private final OWLReasoner reasoner;
	
	private final Set<OWLObject> roots;
	
	public DefaultLinkDatabase(OWLGraphWrapper graph, OWLReasoner reasoner) {
		super();
		this.graph = graph;
		this.reasoner = reasoner;
		this.roots = Collections.unmodifiableSet(findRoots(graph, reasoner));
	}
	
	static Set<OWLObject> findRoots(OWLGraphWrapper graph, OWLReasoner reasoner) {
		Set<OWLObject> roots = new HashSet<OWLObject>();
		OWLOntology ontology = graph.getSourceOntology();
		OWLDataFactory dataFactory = ontology.getOWLOntologyManager().getOWLDataFactory();
		OWLClass owlThing = dataFactory.getOWLThing();
		Set<OWLClass> flattened = reasoner.getSubClasses(owlThing, true).getFlattened();
		for (OWLClass owlClass : flattened) {
			if (owlClass.isBottomEntity() || owlClass.isTopEntity() || graph.isObsolete(owlClass)) {
				continue;
			}
			roots.add(owlClass);
		}
		return roots;
	}
	
	@Override
	public OWLObject getObject(IRI iri) {
		return graph.getOWLObject(iri);
	}

	@Override
	public Collection<Link> getChildren(OWLObject lo) {
		if (lo instanceof OWLClass) {
			final OWLClass target = (OWLClass)lo;
			Set<OWLClass> children = reasoner.getSubClasses(target, true).getFlattened();
			Set<Link> links = new HashSet<Link>();
			for (OWLClass source : children) {
				if (source.isOWLThing() || source.isOWLNothing() || graph.isObsolete(source)) {
					continue;
				}
				links.add(new Link(source, target, null));
			}
			if (links.isEmpty() == false) {
				return links;
			}
		}
		return Collections.emptySet();
	}

	@Override
	public Collection<Link> getParents(OWLObject lo) {
		if (lo instanceof OWLClass) {
			final OWLClass source = (OWLClass)lo;
			Set<OWLClass> parents = reasoner.getSuperClasses(source, true).getFlattened();
			Set<Link> links = new HashSet<Link>();
			for (OWLClass target : parents) {
				if (target.isOWLThing() || target.isOWLNothing() || graph.isObsolete(target)) {
					continue;
				}
				links.add(new Link(source, target, null));
			}
			if (links.isEmpty() == false) {
				return links;
			}
		}
		return Collections.emptySet();
	}

	@Override
	public boolean hasChildren(OWLObject lo) {
		return getChildren(lo).isEmpty() == false;
	}

	@Override
	public boolean hasParents(OWLObject lo) {
		return getParents(lo).isEmpty() == false;
	}

	@Override
	public Collection<OWLObject> getRoots() {
		return roots;
	}

	@Override
	public Collection<OWLObject> getObjects() {
		Set<OWLObject> filtered = new HashSet<OWLObject>();
		for (OWLObject owlObject : graph.getAllOWLObjects()) {
			if (owlObject instanceof OWLClass) {
				if (graph.isObsolete(owlObject) == false) {
					filtered.add(owlObject);
				}
			}
		}
		return filtered;
	}

	@Override
	public Collection<OWLObject> getDescendants(OWLObject term, boolean includeSelf) {
		if (term instanceof OWLClass) {
			OWLClass cls = (OWLClass) term;
			Set<OWLClass> descendants = reasoner.getSubClasses(cls, false).getFlattened();
			Set<OWLObject> result = new HashSet<OWLObject>();
			for (OWLClass descendant : descendants) {
				if (descendant.isOWLThing() || descendant.isOWLNothing()) {
					continue;
				}
				if (includeSelf == false && cls.equals(descendant)) {
					continue;
				}
				result.add(descendant);
			}
			if (result.isEmpty() == false) {
				return result;
			}
		}
		return Collections.emptySet();
	}

	@Override
	public Collection<OWLObject> getAncestors(OWLObject term, boolean includeSelf) {
		if (term instanceof OWLClass) {
			OWLClass cls = (OWLClass) term;
			Set<OWLClass> ancestors = reasoner.getSuperClasses(cls, false).getFlattened();
			Set<OWLObject> result = new HashSet<OWLObject>();
			for (OWLClass ancestor : ancestors) {
				if (ancestor.isOWLThing() || ancestor.isOWLNothing()) {
					continue;
				}
				if (includeSelf == false && cls.equals(ancestor)) {
					continue;
				}
				result.add(ancestor);
			}
			if (result.isEmpty() == false) {
				return result;
			}
		}
		return Collections.emptySet();
	}

}
