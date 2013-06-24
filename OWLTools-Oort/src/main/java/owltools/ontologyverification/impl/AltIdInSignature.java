package owltools.ontologyverification.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAxiom;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;

import owltools.graph.OWLGraphWrapper;
import owltools.io.OWLPrettyPrinter;
import owltools.ontologyverification.CheckWarning;

public class AltIdInSignature extends AbstractCheck {

	public static final String SHORT_HAND = "altid-in-signature";
	
	public AltIdInSignature() {
		super("ALTID_IN_SIGNATURE", "Alternate Identifier in Signature Check", false, null);
	}

	@Override
	public Collection<CheckWarning> check(OWLGraphWrapper graph, Collection<OWLObject> allOwlObjects) {
		OWLPrettyPrinter pp = new OWLPrettyPrinter(graph);
		List<CheckWarning> out = new ArrayList<CheckWarning>();
		
		Map<String, OWLObject> altIds = graph.getAllOWLObjectsByAltId();
		
		for (OWLObject owlObject : allOwlObjects) {
			if (owlObject instanceof OWLClass) {
				OWLClass owlClass = (OWLClass) owlObject;
				if (graph.isObsolete(owlClass) == false) {
					check(owlClass, graph, altIds, out, pp);
				}
			}
		}
		return out;
	}
	
	protected void check(OWLClass owlClass, OWLGraphWrapper graph, Map<String, OWLObject> altIds, List<CheckWarning> warnings, OWLPrettyPrinter pp) {
		final Set<OWLOntology> allOntologies = graph.getAllOntologies();
		for(OWLOntology ontology : allOntologies){
			Set<OWLClassAxiom> axioms = ontology.getAxioms(owlClass);
			if (axioms != null && !axioms.isEmpty()) {
				// check axioms 
				for (OWLClassAxiom axiom : axioms) {
					Set<OWLClass> signature = axiom.getClassesInSignature();
					for (OWLClass signatureClass : signature) {
						String identifier = graph.getIdentifier(signatureClass);
						if (identifier != null) {
							OWLObject replacement = altIds.get(identifier);
							if (replacement != null) {
								StringBuilder sb = new StringBuilder();
								sb.append("Alternate Identifier ");
								sb.append(identifier);
								sb.append(" for main class ");
								String replacementId = graph.getIdentifier(replacement);
								if (replacementId != null) {
									sb.append(replacementId);
									String replacementLabel = graph.getLabel(replacement);
									if (replacementLabel != null) {
										sb.append(" '").append(replacementLabel).append('\'');
									}
								}
								else {
									sb.append(replacement);
								}
								sb.append(" in axiom: ");
								sb.append(pp.render(axiom));
								warnings.add(new CheckWarning(getID(), sb.toString() , isFatal(), owlClass.getIRI()));
							}
						}
					}
				}
			}
		}
	}

}
