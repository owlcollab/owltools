package owltools.util;

import java.util.Set;

import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
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
 *
 */
public class ShuntGraphUtils {

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
	 * Typically relation-indepedent paths are cached, but any path specific to a relation
	 * set is not cached. Consult OGW docs for details.
	 * 
	 * TODO: write test
	 * 
	 * @param ogw
	 * @param focusObject
	 * @param props - if null, all properties are included
	 * @param isIncludeDirectChildren - if true, include all children of focusObject in V
	 * @return
	 */
	public static OWLShuntGraph createShuntCraph(
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
			Set<OWLGraphEdge> edges = ogw.getOutgoingEdges(anc);
			
			for (OWLGraphEdge edge : edges) {
				if (!nodeSet.contains(edge.getTarget()))
					continue;
				String pred;
				OWLQuantifiedProperty qp = edge.getFirstQuantifiedProperty();
				if (qp.isSomeValuesFrom()) {
					pred = qp.getPropertyId();
				}
				else if (qp.isSubClassOf()) {
					pred = "subClassOf"; // TODO - check w Seth. May be better to reuse rdfs vocab
				}
				else {
					continue;
				}
				OWLShuntEdge sge = new OWLShuntEdge(edge.getSourceId(), edge.getTargetId(), pred);
				osg.addEdge(sge);
			}
			osg.addNode(new OWLShuntNode(ogw, anc));
		}
		
		return osg;
		
	}
 	
}
