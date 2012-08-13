package owltools.ontologyverification.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObject;

import owltools.graph.OWLGraphWrapper;
import owltools.ontologyverification.CheckWarning;

/**
 * Simple cycle check using the asserted relations
 * 
 * TODO replace with BFS over is_a hierarchy
 */
public class CycleCheck extends AbstractCheck {

	public CycleCheck() {
		super("CYCLE_CHECK", "Cycle Check", false);
	}

	public Collection<CheckWarning> check(OWLGraphWrapper graph, Collection<OWLObject> allOwlObjects) {

		Collection<CheckWarning> out = new ArrayList<CheckWarning>();
		for(OWLObject owlObject : allOwlObjects){
			if (owlObject instanceof OWLClass) {
				OWLClass owlClass = (OWLClass) owlObject;
				checkForCycles(owlClass, graph, out);
			};
		}
		return out;
	}

	protected void checkForCycles(OWLClass cls, OWLGraphWrapper graph, Collection<CheckWarning> warnings) {
		// TODO create test case, which actually verifies that this code works
		Set<OWLObject> descendants = graph.getDescendants(cls);
		if (descendants.contains(cls)) {
			String label = graph.getLabelOrDisplayId(cls);
			String message = label + " is part of a cycle.";
			CheckWarning warning = new CheckWarning(getID(), message, isFatal(), cls.getIRI(), null);
			warnings.add(warning);
        }
	}

}
