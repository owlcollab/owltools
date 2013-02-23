package owltools.ontologyverification.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAxiom;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;

import owltools.graph.OWLGraphWrapper;
import owltools.io.OWLPrettyPrinter;
import owltools.ontologyverification.CheckWarning;

public class ObsoleteClassInSignature extends AbstractCheck {

	public static final String SHORT_HAND = "obsolete-in-signature";
	
	public ObsoleteClassInSignature() {
		super("OBSOLETE_CLASS_IN_SIGNATURE", "Obsolete Class in Signature Check", true, null);
	}

	@Override
	public Collection<CheckWarning> check(OWLGraphWrapper graph, Collection<OWLObject> allOwlObjects) {
		OWLPrettyPrinter pp = new OWLPrettyPrinter(graph);
		List<CheckWarning> out = new ArrayList<CheckWarning>();
		for (OWLObject owlObject : allOwlObjects) {
			if (owlObject instanceof OWLClass) {
				OWLClass owlClass = (OWLClass) owlObject;
				if (graph.isObsolete(owlClass) == false) {
					check(owlClass, graph, out, pp);
				}
			}
		}
		return out;
	}
	
	protected void check(OWLClass owlClass, OWLGraphWrapper graph, List<CheckWarning> warnings, OWLPrettyPrinter pp) {
		final Set<OWLOntology> allOntologies = graph.getAllOntologies();
		for(OWLOntology ontology : allOntologies){
			Set<OWLClassAxiom> axioms = ontology.getAxioms(owlClass);
			if (axioms != null && !axioms.isEmpty()) {
				// check axioms 
				for (OWLClassAxiom axiom : axioms) {
					Set<OWLClass> signature = axiom.getClassesInSignature();
					for (OWLClass signatureClass : signature) {
						if (graph.isObsolete(signatureClass)) {
							final IRI iri = signatureClass.getIRI();
							String message = "Obsolete class "+iri+" in axiom: "+pp.render(axiom);
							warnings.add(new CheckWarning(getID(), message , isFatal(), owlClass.getIRI()));
						}
					}
				}
			}
		}
	}

}
