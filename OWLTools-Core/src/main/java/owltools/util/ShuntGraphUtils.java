package owltools.util;

import java.util.Set;

import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLPropertyExpression;

import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;
import owltools.graph.OWLQuantifiedProperty;
import owltools.graph.shunt.OWLShuntEdge;
import owltools.graph.shunt.OWLShuntGraph;
import owltools.graph.shunt.OWLShuntNode;

/**
 * Alternative method for generating ShuntGraphs - experimental
 * 
 * @author cjm
 */
@SuppressWarnings("rawtypes")
public class ShuntGraphUtils {

	
	/**
	 * Generate two shunt graphs, one based on the topology and the other based
	 * on the inferred graph.
	 * 
	 * @param ogw
	 * @param focusObject
	 * @param props
	 * @param isIncludeDirectChildren
	 * @return graph pair
	 */
	public static ShuntGraphPair createShuntGraphPair(
			OWLGraphWrapper ogw,
			OWLObject focusObject,
			Set<OWLPropertyExpression> props,
			boolean isIncludeDirectChildren) {
		
		OWLShuntGraph topologyGraph = new OWLShuntGraph();
		OWLShuntGraph inferredGraph = new OWLShuntGraph();
		Set<OWLObject> nodeSet = ogw.getAncestorsReflexive(focusObject, props);
		if (isIncludeDirectChildren) {
			// include children for topology calculation
			// do not calculate the inferred view using the direct children
			// just add direct children to the inferred shunt graph
			for (OWLGraphEdge edge : ogw.getIncomingEdges(focusObject)) {
				OWLObject source = edge.getSource();
				if (source instanceof OWLNamedObject) {
					nodeSet.add(source);
					OWLShuntEdge sge = createShuntEdge(edge, ogw);
					if (sge != null) {
						inferredGraph.addEdge(sge);
						inferredGraph.addNode(new OWLShuntNode(ogw, source));
					}
				}
			}
		}
		// calculate the topology
		for (OWLObject anc : nodeSet) {
			if ((anc instanceof OWLNamedObject) == false) {
				continue;
			}
			Set<OWLGraphEdge> edges = ogw.getOutgoingEdges(anc);
			for (OWLGraphEdge edge : edges) {
				if (!nodeSet.contains(edge.getTarget())) {
					continue;
				}
				OWLShuntEdge sge = createShuntEdge(edge, ogw);
				if (sge != null) {
					topologyGraph.addEdge(sge);
				}
			}
			topologyGraph.addNode(new OWLShuntNode(ogw, anc));
		}
		
		// calculate the inferred view
		Set<OWLGraphEdge> edgesClosure = ogw.getOutgoingEdgesClosure(focusObject, props);
		for (OWLGraphEdge edge : edgesClosure) {
			if ((edge.getTarget() instanceof OWLNamedObject) == false) {
				continue;
			}
			OWLShuntEdge sge = createShuntEdge(edge, ogw);
			if (sge != null) {
				inferredGraph.addEdge(sge);
				inferredGraph.addNode(new OWLShuntNode(ogw, edge.getTarget()));
			}
		}
		inferredGraph.addNode(new OWLShuntNode(ogw, focusObject));
		
		return new ShuntGraphPair(topologyGraph, inferredGraph );
	}
	
	private static OWLShuntEdge createShuntEdge(OWLGraphEdge edge, OWLGraphWrapper ogw) {
		String pred;
		OWLQuantifiedProperty qp = edge.getLastQuantifiedProperty();
		if (qp.isSomeValuesFrom()) {
			pred = ogw.getIdentifier(qp.getProperty(), false);
		}
		else if (qp.isSubClassOf()) {
			pred = "rdfs:subClassOf"; // TODO standardize, may be use a global static variable?
		}
		else {
			return null;
		}
		return new OWLShuntEdge(edge.getSource(), edge.getTarget(), pred, ogw);
	}
	
	/**
	 * Generate a shunt graph G = (V,E) which is a subset of the ontology
	 * G<sup>O</sup> = (V<sup>O</sup>,E<sup>O</sup>)
	 * from a focus object X and relationset R
	 * such that:
	 * <li> V contains every node reachable from X over some relations r1,t2,... &in R,
	 *     including the reflexive case of V, and (optionally) all child terms of V.
	 * <li> E = { e : e &in; E<sup>O</sup>, e<sub>subj</sub> &in; V, e<sub>obj</sub> &in; V }
	 * 
	 * Note that caching is controlled by the underlying OWLGraphWrapper object.
	 * Typically relation-independent paths are cached, but any path specific to a relation
	 * set is not cached. Consult {@link OWLGraphWrapper} docs for details.
	 * 
	 * @param ogw
	 * @param focusObject
	 * @param props - if null, all properties are included
	 * @param isIncludeDirectChildren - if true, include all children of focusObject in V
	 * @return graph
	 */
	public static OWLShuntGraph createShuntGraph(
			OWLGraphWrapper ogw,
			OWLObject focusObject,
			Set<OWLPropertyExpression> props,
			boolean isIncludeDirectChildren) {
		
		OWLShuntGraph osg = new OWLShuntGraph();
		Set<OWLObject> nodeSet = 
				ogw.getAncestorsReflexive(focusObject, props);
		if (isIncludeDirectChildren) {
			for (OWLGraphEdge edge : ogw.getIncomingEdges(focusObject)) {
				nodeSet.add(edge.getSource());
			}
		}
		for (OWLObject anc : nodeSet) {
			if ((anc instanceof OWLNamedObject) == false) {
				continue;
			}
			Set<OWLGraphEdge> edges = ogw.getOutgoingEdges(anc);
			
			for (OWLGraphEdge edge : edges) {
				if (!nodeSet.contains(edge.getTarget())) {
					continue;
				}
				OWLShuntEdge sge = createShuntEdge(edge, ogw);
				if (sge != null) {
					osg.addEdge(sge);
				}
			}
			osg.addNode(new OWLShuntNode(ogw, anc));
		}
		
		return osg;
		
	}
 	
	/**
	 * Result object containing two corresponding shunt graphs.
	 */
	public static class ShuntGraphPair {
		
		private final OWLShuntGraph topologyGraph;
		private final OWLShuntGraph inferredGraph;
		
		/**
		 * @param topologyGraph
		 * @param inferredGraph
		 */
		public ShuntGraphPair(OWLShuntGraph topologyGraph, OWLShuntGraph inferredGraph) {
			this.topologyGraph = topologyGraph;
			this.inferredGraph = inferredGraph;
		}

		
		/**
		 * @return the topologyGraph
		 */
		public OWLShuntGraph getTopologyGraph() {
			return topologyGraph;
		}

		
		/**
		 * @return the inferredGraph
		 */
		public OWLShuntGraph getInferredGraph() {
			return inferredGraph;
		}
		
	}
}
