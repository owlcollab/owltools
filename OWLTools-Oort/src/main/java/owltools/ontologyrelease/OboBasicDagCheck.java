package owltools.ontologyrelease;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLObject;

import owltools.graph.OWLGraphWrapper;
import owltools.util.Adjacency;
import owltools.util.MappingTarjan;
import owltools.util.Tarjan;

public class OboBasicDagCheck {

	/**
	 * Find cycles of any relationship. Uses the OWLGraphWrapper methods to find direct ancestors.
	 * 
	 * @param graph
	 * @return list of cycles.
	 */
	public static List<List<OWLObject>> findCycles(final OWLGraphWrapper graph) {
		// find strongly connected components using the Tarjan algorithm
		// use parameter to request only components with more than one node
		Tarjan<OWLObject> tarjan = new MappingTarjan<OWLObject>(true);
		Adjacency<OWLObject> adjacency = new Adjacency<OWLObject>() {

			@Override
			public List<OWLObject> getAdjacent(OWLObject source) {
				Set<OWLObject> ancestors = graph.getAncestors(source);
				if (ancestors == null || ancestors.isEmpty()) {
					return Collections.emptyList();
				}
				return new ArrayList<OWLObject>(ancestors);
			}

			@Override
			public Iterable<OWLObject> getSources() {
				return graph.getAllOWLObjects();
			}
		};
		List<List<OWLObject>> scc = tarjan.executeTarjan(adjacency);
		return scc;
	}
	
}
