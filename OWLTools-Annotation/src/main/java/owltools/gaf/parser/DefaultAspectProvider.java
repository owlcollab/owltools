package owltools.gaf.parser;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.gaf.parser.GpadGpiObjectsBuilder.AspectProvider;
import owltools.graph.OWLGraphWrapper;

public class DefaultAspectProvider implements AspectProvider {

	private final Map<String, String> aspectMap;
	
	/**
	 * Create a new aspect provider with the given (complete) id-aspect mappings.
	 * 
	 * @param aspectMap
	 */
	public DefaultAspectProvider(Map<String, String> aspectMap) {
		this.aspectMap = aspectMap;
	}

	/**
	 * Create a new aspect provider from the given super classes and aspects. If
	 * available, use the reasoner to retrieve the subClasses, otherwise
	 * retrieve the descendants via the {@link OWLGraphWrapper}.
	 * 
	 * @param graph
	 * @param mappings
	 * @param reasoner
	 * @return provider
	 */
	public static AspectProvider createAspectProvider(OWLGraphWrapper graph, 
			Map<String, String> mappings, OWLReasoner reasoner)
	{
		Map<String, String> aspectMap = new HashMap<String, String>();
		for(Entry<String, String> entry : mappings.entrySet()) {
			String superClassId = entry.getKey();
			OWLClass superClass = graph.getOWLClassByIdentifier(superClassId);
			if (superClass == null) {
				continue;
			}
			String aspect = entry.getValue();
			if (reasoner != null) {
				mappings.put(superClassId, aspect);
				Set<OWLClass> subClasses = reasoner.getSubClasses(superClass, false).getFlattened();
				for (OWLClass subClass : subClasses) {
					String id = graph.getIdentifier(subClass);
					if (id != null) {
						mappings.put(id, aspect);
					}
				}
			}
			else {
				Set<OWLObject> descendants = graph.getDescendantsReflexive(superClass);
				for (OWLObject descendant : descendants) {
					String id = graph.getIdentifier(descendant);
					if (id != null) {
						mappings.put(id, aspect);
					}
				}
			}
		}
		AspectProvider p = new DefaultAspectProvider(aspectMap);
		return p;
	}
	
	@Override
	public String getAspect(String cls) {
		return aspectMap.get(cls);
	}

}
