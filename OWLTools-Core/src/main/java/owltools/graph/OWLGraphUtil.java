package owltools.graph;

import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLObject;

/**
 * general static methods for additional graph operations
 *
 */
public class OWLGraphUtil {
	
	/**
	 * @param g
	 * @param x
	 * @param y
	 * @return set of edges
	 */
	public static Set<OWLGraphEdge> findCommonSubsumersAsEdges(OWLGraphWrapper g,
			OWLObject x, OWLObject y) {
		Set<OWLGraphEdge> xs = g.getOutgoingEdgesClosure(x);
		Set<OWLGraphEdge> ys = g.getOutgoingEdgesClosure(y);
		xs.retainAll(ys);
		return xs;
	}
	
	public static Set<OWLGraphEdge> findLeastCommonSubsumersAsEdges(OWLGraphWrapper g,
			OWLObject x, OWLObject y) {
		Set<OWLGraphEdge> cses = findCommonSubsumersAsEdges(g,x,y);
		Set<OWLGraphEdge> lcses = new HashSet<OWLGraphEdge>();
		lcses.addAll(cses);
		for (OWLGraphEdge cse: cses) {
			Set<OWLObject> zs = g.getAncestors(cse.getSource());
			for (OWLObject z : zs) {
				for (OWLGraphEdge lcse: lcses) {
					if (lcse.getSource().equals(z))
						lcses.remove(lcse);
				}
			}
		}
		return lcses;
	}
	
	public static Set<OWLObject> findCommonAncestors(OWLGraphWrapper g,
				OWLObject x, OWLObject y) {
		Set<OWLObject> xs = g.getAncestors(x);
		Set<OWLObject> ys = g.getAncestors(y);
		xs.retainAll(ys);
		return xs;
	}
	public static Set<OWLObject> findLeastCommonAncestors(OWLGraphWrapper g,
			OWLObject x, OWLObject y) {
		Set<OWLObject> cas = findCommonAncestors(g,x,y);
		Set<OWLObject> lcas = new HashSet<OWLObject>();
		lcas.addAll(cas);
		for (OWLObject z : cas) {
			lcas.removeAll(g.getAncestors(z));
		}
		return lcas;
	}

}
