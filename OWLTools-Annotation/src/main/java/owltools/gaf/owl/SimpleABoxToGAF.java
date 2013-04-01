package owltools.gaf.owl;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import owltools.gaf.*;
import owltools.graph.OWLGraphWrapper;

import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * Translates a set of ClassAssertion axioms to a minimal GAF
 * 
 * @author cjm
 *
 */
public class SimpleABoxToGAF {

	private OWLGraphWrapper graph;

	public Set<GeneAnnotation> generateAssociations(OWLOntology ont) {
		Set<GeneAnnotation> assocs = new HashSet<GeneAnnotation>();
		for (OWLNamedIndividual i : ont.getIndividualsInSignature(true)) {
			assocs.addAll(generateAssociations(i, ont));
		}
		return assocs;
	}

	public Set<GeneAnnotation> generateAssociations(OWLNamedIndividual ind, OWLOntology ont) {
		Set<GeneAnnotation> assocs = new HashSet<GeneAnnotation>();
		String eid = graph.getIdentifier(ind);
		for (OWLClassExpression x : ind.getTypes(ont)) {
			GeneAnnotation ga = new GeneAnnotation();
			if (x.isAnonymous()) {
				// TODO
			}
			else {
				ga.setCls(graph.getIdentifier(x));
			}
			ga.setBioentity(eid);
			assocs.add(ga);
		}
		return assocs;
	}

}
