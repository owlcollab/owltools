package owltools.diff;

import java.util.List;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLException;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import owltools.InferenceBuilder;
import owltools.graph.OWLGraphWrapper;

public class ReasonerDiff {

	private final List<OWLAxiom> newAxioms;
	private final List<OWLAxiom> redundantAxioms;
	
	/**
	 * @param newAxioms
	 * @param redundantAxioms
	 */
	protected ReasonerDiff(List<OWLAxiom> newAxioms,
			List<OWLAxiom> redundantAxioms) {
		this.newAxioms = newAxioms;
		this.redundantAxioms = redundantAxioms;
	}
	
	/**
	 * @return the newAxioms
	 */
	public List<OWLAxiom> getNewAxioms() {
		return newAxioms;
	}

	/**
	 * @return the redundantAxioms
	 */
	public List<OWLAxiom> getRedundantAxioms() {
		return redundantAxioms;
	}

	public static ReasonerDiff createReasonerDiff(OWLGraphWrapper baseLine, OWLGraphWrapper change, String reasoner) throws OWLException {
		// pre-reason both ontologies
		preReasonOntology(baseLine, reasoner);
		preReasonOntology(change, reasoner);
		
		// add change to ontology
		for(OWLOntology ontology : change.getAllOntologies()) {
			baseLine.mergeOntology(ontology);
		}
		
		// get new inferences
		InferenceBuilder builder = new InferenceBuilder(baseLine, reasoner);
		List<OWLAxiom> newAxioms = builder.buildInferences();
		List<OWLAxiom> redundantAxioms = builder.getRedundantAxioms();
		
		return new ReasonerDiff(newAxioms, redundantAxioms);
	}
	
	static void preReasonOntology(OWLGraphWrapper graph, String reasoner) {
		InferenceBuilder builder = new InferenceBuilder(graph, reasoner);
		OWLOntology ontology = graph.getSourceOntology();
		OWLOntologyManager manager = ontology.getOWLOntologyManager();
		
		List<OWLAxiom> inferredAxioms = builder.buildInferences();
		for (OWLAxiom owlAxiom : inferredAxioms) {
			manager.addAxiom(ontology, owlAxiom);
		}
		List<OWLAxiom> redundantAxioms = builder.getRedundantAxioms();
		for (OWLAxiom owlAxiom : redundantAxioms) {
			manager.removeAxiom(ontology, owlAxiom);
		}
	}
	
}
