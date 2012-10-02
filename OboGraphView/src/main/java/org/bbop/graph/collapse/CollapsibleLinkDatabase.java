package org.bbop.graph.collapse;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.bbop.graph.LinkDatabase;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.model.OWLObject;

public class CollapsibleLinkDatabase implements LinkDatabase {

	private final LinkDatabase linkDatabase;

	private Collection<OWLObject> visibleObjects = new LinkedHashSet<OWLObject>();
	private Collection<OWLObject> defaultVisibleObjects = new LinkedList<OWLObject>();
	private final List<ExpandCollapseListener> listeners = new LinkedList<ExpandCollapseListener>();
	
	public CollapsibleLinkDatabase(LinkDatabase linkDatabase, Set<OWLObject> initialSelection) {
		this.linkDatabase = linkDatabase;
		defaultVisibleObjects.addAll(linkDatabase.getAncestors(initialSelection, true));
		recache();
	}


	public void cleanupCache() {
		visibleObjects = refresh(visibleObjects);
		defaultVisibleObjects = refresh(defaultVisibleObjects);
	}

	protected Collection<OWLObject> refresh(Collection<OWLObject> ios) {
		Collection<OWLObject> out = new LinkedHashSet<OWLObject>();
		for (OWLObject io : ios) {
			if (io instanceof OWLNamedObject) {
				OWLObject fetched = linkDatabase.getObject(((OWLNamedObject)io).getIRI());
				if (fetched != null)
					out.add(fetched);
			}
		}
		return out;

	}

	public void addListener(ExpandCollapseListener listener) {
		listeners.add(listener);
	}

	public void removeListener(ExpandCollapseListener listener) {
		listeners.remove(listener);
	}

	public void setDefaultVisibleObjects(Collection<? extends OWLObject> c) {
		defaultVisibleObjects = new LinkedList<OWLObject>(c);
	}

	public void recache() {
		visibleObjects.clear();
		visibleObjects.addAll(defaultVisibleObjects);
	}

	@Override
	public Collection<OWLObject> getObjects() {
		return visibleObjects;
	}

	public boolean isVisible(OWLObject lo) {
		return visibleObjects.contains(lo);
	}

	public int getChildExpansionCount(OWLObject lo) {
		int expansionCount = 0;
		for(LinkDatabase.Link link : linkDatabase.getChildren(lo)){
			if (isVisible(link.getSource()))
				expansionCount++;
		}
		return expansionCount;
	}

	public int getParentExpansionCount(OWLObject lo) {
		int expansionCount = 0;
		for(LinkDatabase.Link link : linkDatabase.getParents(lo)){
			if (isVisible(link.getTarget()))
				expansionCount++;
		}
		return expansionCount;
	}

	protected boolean isChildExpanded(OWLObject lo) {
		for(LinkDatabase.Link link : linkDatabase.getChildren(lo)){
			if (isVisible(link.getSource()))
				return true;
		}
		return false;
	}

	protected boolean isParentExpanded(OWLObject lo) {
		for(LinkDatabase.Link link : linkDatabase.getParents(lo)){
			if (isVisible(link.getTarget()))
				return true;
		}
		return false;
	}

	public void setVisibleObjects(Collection<? extends OWLObject> objects, boolean setDefault) {
		Collection<OWLObject> oldVisible = new ArrayList<OWLObject>(visibleObjects);
		Collection<OWLObject> added = new ArrayList<OWLObject>(objects);
		Collection<OWLObject> deleted = new ArrayList<OWLObject>(visibleObjects);
		added.removeAll(oldVisible);
		deleted.removeAll(objects);
		if (!(visibleObjects.size() == objects.size() && objects.containsAll(visibleObjects))) {
			if (setDefault) {
				setDefaultVisibleObjects(objects);
				recache();
				fireExpansionStateChanged(added, deleted);
			} else {
				setDefaultVisibleObjects(Collections.<OWLObject>emptySet());
				recache();
				visibleObjects.addAll(objects);
				fireExpansionStateChanged(added, deleted);
			}
		}
	}

	protected void fireExpansionStateChanged(Collection<OWLObject> shown, Collection<OWLObject> hidden) {
		ExpansionEvent e = null;
		int size = listeners.size();
		for (int i = 0; i < size && i < listeners.size(); i++) {
			ExpandCollapseListener listener = listeners.get(i);
			if (e == null) {
				e = new ExpansionEvent(this, shown, hidden);
			}
			listener.expandStateChanged(e);
		}
	}

	public boolean shouldBeTrimmed(LinkDatabase.Link link) {
//		if (!TermUtil.isImplied(link))
//			return false;
//		// trim any links to parents that are redundant with
//		// a GRANDPARENT link
//		// if any trimmed link is a GIVEN link, it is redundant
//		for(LinkDatabase.Link parentLink : getParents(link.getChild(), true)){
//			if (parentLink.equals(link)) {
//				continue;
//			}
//			if (!(parentLink.getType().equals(link.getType()) || parentLink
//					.getType().equals(OBOProperty.IS_A)))
//				continue;
//			boolean sawType = parentLink.getType().equals(link.getType());
//
//
//			// for each grandparent link accessible via the current
//			// parent link...
//			for(LinkDatabase.Link gpLink : getParents(parentLink.getParent(), true)){
//				// see if the grandparent link has the same type
//				// and parent as the current link. if it does,
//				// the current link is redundant with the grandparent
//				// link and should be removed
//
//				if ((((!sawType || link.getType().isTransitive()) && link
//						.getType().equals(gpLink.getType())) || (sawType && gpLink
//						.getType().equals(OBOProperty.IS_A)))
//						&& link.getParent().equals(gpLink.getParent())) {
//
//					return true;
//				}
//			}
//
//			// add a section where we trim links that have a sibling link
//			// with the same parent, but a more specific type,
//			// than the current link
//		}
		return false;
	}

	@Override
	public Collection<LinkDatabase.Link> getChildren(OWLObject lo) {
		return getChildren(lo, false);
	}

	public Collection<LinkDatabase.Link> getChildren(OWLObject lo, boolean ignoreTrimming) {
		Set<LinkDatabase.Link> children = new HashSet<LinkDatabase.Link>();
		for(LinkDatabase.Link link : linkDatabase.getChildren(lo)){
			if (isVisible(link.getSource()) && (ignoreTrimming || !shouldBeTrimmed(link)))
				children.add(link);
		}
		return children;
	}

	@Override
	public Collection<LinkDatabase.Link> getParents(OWLObject lo) {
		return getParents(lo, false);
	}

	public Collection<LinkDatabase.Link> getParents(OWLObject lo, boolean ignoreTrimming) {
		Set<LinkDatabase.Link> parents = new HashSet<LinkDatabase.Link>();
		for(LinkDatabase.Link link : linkDatabase.getParents(lo)){
			if (isVisible(link.getTarget()) && (ignoreTrimming || !shouldBeTrimmed(link)))
				parents.add(link);
		}
		return parents;
	}

	@Override
	public OWLObject getObject(IRI id) {
		OWLObject out = linkDatabase.getObject(id);
		if (visibleObjects.contains(out))
			return out;
		else
			return null;
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
	public Set<OWLObject> getRoots() {
		return linkDatabase.getRoots();
	}

	@Override
	public Set<OWLObject> getDescendants(OWLObject term, boolean includeSelf) {
		return linkDatabase.getDescendants(term, includeSelf);
	}

	@Override
	public Set<OWLObject> getAncestors(OWLObject term, boolean includeSelf) {
		return linkDatabase.getAncestors(term, includeSelf);
	}

	@Override
	public Set<OWLObject> getAncestors(Set<OWLObject> terms, boolean includeSelf) {
		return linkDatabase.getAncestors(terms, includeSelf);
	}

}
