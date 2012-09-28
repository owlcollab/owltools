package org.bbop.graph;

import java.awt.Dimension;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.log4j.Logger;
import org.bbop.graph.LinkDatabase.Link;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObject;

import edu.umd.cs.piccolo.PNode;

public class LinkDatabaseLayoutEngine {

	//initialize logger
	protected final static Logger logger = Logger.getLogger(LinkDatabaseLayoutEngine.class);

	private final LinkDatabase linkDatabase;
	private final GraphLayout graphLayout;
	private final NodeSizeProvider sizeProvider;
	private final NodeLabelProvider labelProvider;
	private final NamedChildProvider provider = DefaultNamedChildProvider.getInstance();
	private final NodeFactory factory;

	protected int initialNodeHeight = 40;

	

	public LinkDatabaseLayoutEngine(LinkDatabase linkDatabase, GraphLayout graphLayout, NodeFactory factory, NodeSizeProvider sizeProvider, NodeLabelProvider labelProvider) {
		super();
		this.linkDatabase = linkDatabase;
		this.graphLayout = graphLayout;
		this.factory = factory;
		this.sizeProvider = sizeProvider;
		this.labelProvider = labelProvider;
	}

	protected void initializeData() {
		
		final Collection<OWLObject> objects = linkDatabase.getObjects();
		for (OWLObject owlObject : objects) {
			
			if (owlObject instanceof OWLClass) {
				OWLClass cls = (OWLClass) owlObject;
				
				graphLayout.addNode(cls);
				int width = 0;
				int height = initialNodeHeight;
				Dimension d = sizeProvider.getSize(cls);
				if (d != null) {
					if (d.getHeight() > 0)
						height = (int) d.getHeight();
					if (d.getWidth() > 0)
						width = (int) d.getWidth();
				}
				graphLayout.setNodeDimensions(cls, width, height);
			}
		}
		
		for (OWLObject owlObject : objects) {
			
			Collection<Link> links = linkDatabase.getChildren(owlObject);
			for (Link link : links) {
				graphLayout.addEdge(link);
			}
		}
	}

	protected String getLabel(OWLObject io) {
		return labelProvider.getLabel(io);
	}
	
	public GraphLayout getLayout() {
		graphLayout.reset();
		initializeData();
		graphLayout.doLayout();
		return graphLayout;
	}

	public synchronized PNode getNewLayer() {
		graphLayout.reset();
		initializeData();
		graphLayout.doLayout();
		PNode out = new PNode();
		final List<OWLObject> current = new ArrayList<OWLObject>(linkDatabase.getObjects());
		Collections.sort(current, new Comparator<OWLObject>() {

			@Override
			public int compare(OWLObject o1, OWLObject o2) {
				String l1 = getLabel(o1);
				String l2 = getLabel(o2);
				return l1.compareToIgnoreCase(l2);
			}
		});
		
		for (OWLObject owlObject : current) {
			Shape s = graphLayout.getNodeShape(owlObject);
			PNode p = factory.createNode(owlObject, s);

			provider.setNamedChild(owlObject, out, p);

			Collection<Link> links = linkDatabase.getChildren(owlObject);
			for (Link link : links) {
				Shape currentShape = graphLayout.getEdgeShape(link);
				OELink linkp = factory.createLink(link, currentShape);
				provider.setNamedChild(link, out, linkp);
			}

		}
		return out;
	}

}
