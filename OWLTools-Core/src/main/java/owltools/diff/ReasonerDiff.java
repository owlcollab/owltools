package owltools.diff;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLException;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.InferenceBuilder;
import owltools.graph.OWLGraphWrapper;

public class ReasonerDiff {

	private final List<OWLAxiom> newAxioms;
	private final List<OWLAxiom> removedInferredAxioms;
	
	/**
	 * @param newAxioms
	 * @param removedInferredAxioms
	 */
	protected ReasonerDiff(List<OWLAxiom> newAxioms,
			List<OWLAxiom> removedInferredAxioms) {
		this.newAxioms = newAxioms;
		this.removedInferredAxioms = removedInferredAxioms;
	}
	
	/**
	 * @return the newAxioms
	 */
	public List<OWLAxiom> getNewAxioms() {
		return newAxioms;
	}

	/**
	 * @return the removedInferredAxioms
	 */
	public List<OWLAxiom> getRemovedInferredAxioms() {
		return removedInferredAxioms;
	}

	public static ReasonerDiff createReasonerDiff(OWLGraphWrapper baseLine, OWLGraphWrapper change, String reasoner) throws OWLException {
		
		Set<OWLAxiom> baseInferences = getInferences(baseLine, reasoner);
		
		// add change to ontology
		for(OWLOntology ontology : change.getAllOntologies()) {
			baseLine.mergeOntology(ontology);
		}
		
		// get new inferences
		Set<OWLAxiom> changeInferences = getInferences(baseLine, reasoner);
	
		List<OWLAxiom> newAxioms = new ArrayList<OWLAxiom>();
		List<OWLAxiom> removedInferredAxioms = new ArrayList<OWLAxiom>();
		
		for (OWLAxiom owlAxiom : changeInferences) {
			if (baseInferences.contains(owlAxiom) == false) {
				newAxioms.add(owlAxiom);
			}
		}
		for(OWLAxiom owlAxiom : baseInferences) {
			if (changeInferences.contains(owlAxiom) == false) {
				removedInferredAxioms.add(owlAxiom);
			}
		}
		
		return new ReasonerDiff(newAxioms, removedInferredAxioms);
	}
	
	static Set<OWLAxiom> getInferences(OWLGraphWrapper graph, String reasonerName) throws OWLException {
		
		graph.mergeImportClosure();
		InferenceBuilder builder = new InferenceBuilder(graph, reasonerName);
		
		Set<OWLAxiom> inferredAxioms = new HashSet<OWLAxiom>();
		
		OWLOntology ontology = graph.getSourceOntology();
		OWLDataFactory dataFactory = ontology.getOWLOntologyManager().getOWLDataFactory();
		
		OWLReasoner reasoner = builder.getReasoner(ontology);
		for (OWLClass cls : ontology.getClassesInSignature()) {
			NodeSet<OWLClass> scs = reasoner.getSuperClasses(cls, true);
			for (Node<OWLClass> scSet : scs) {
				for (OWLClass sc : scSet) {
					if (sc.isOWLThing()) {
						continue; // do not report subclasses of owl:Thing
					}
					// we do not want to report inferred subclass links
					// if they are already asserted in the ontology
					boolean isAsserted = false;
					for (OWLClassExpression asc : cls.getSuperClasses(ontology)) {
						if (asc.equals(sc)) {
							// we don't want to report this
							isAsserted = true;
						}
					}
					// include any inferred axiom that is NOT already asserted in the ontology
					if (!isAsserted) {						
						inferredAxioms.add(dataFactory.getOWLSubClassOfAxiom(cls, sc));
					}
				}
			}
		}
		
		return inferredAxioms;
	}
	
}
