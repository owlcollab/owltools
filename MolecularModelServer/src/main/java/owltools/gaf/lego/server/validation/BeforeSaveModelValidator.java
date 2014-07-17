package owltools.gaf.lego.server.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.gaf.lego.LegoModelGenerator;
import owltools.gaf.lego.MolecularModelManager;
import owltools.gaf.lego.MolecularModelManager.LegoAnnotationType;
import owltools.gaf.lego.MolecularModelManager.UnknownIdentifierException;

public class BeforeSaveModelValidator {
	
	static boolean USE_CONSISTENCY_CHECKS = false;

	public List<String> validateBeforeSave(String modelId, MolecularModelManager<?> modelManager) throws UnknownIdentifierException {
		// get model
		LegoModelGenerator model = modelManager.getModel(modelId);
		if (model == null) {
			throw new UnknownIdentifierException("Could not find a model for id: "+modelId);
		}
		List<String> errors = new ArrayList<String>(3);
		// check that model has required meta data
		OWLOntology aboxOntology = model.getAboxOntology();
		boolean hasTitle = false;
		boolean hasContributor = false;
		
		// get ontology annotations
		Set<OWLAnnotation> annotations = aboxOntology.getAnnotations();
		for (OWLAnnotation annotation : annotations) {
			OWLAnnotationProperty p = annotation.getProperty();
			LegoAnnotationType legoType = LegoAnnotationType.getLegoType(p.getIRI());
			if (legoType != null) {
				// check for title
				if (LegoAnnotationType.title.equals(legoType)) {
					hasTitle = true;
				}
				// check for contributor
				else if (LegoAnnotationType.contributor.equals(legoType)) {
					hasContributor = true;
				}
			}
		}

		if (hasTitle == false) {
			errors.add("The model has no title. All models must have a human readable title.");
		}
		if (hasContributor == false) {
			errors.add("The model has no contributors. All models must have an association with their contributors.");
		}
		
		// check that model is consistent
		if (USE_CONSISTENCY_CHECKS) {
			OWLReasoner reasoner = model.getReasoner();
			if (reasoner.isConsistent() == false) {
				errors.add("The model is inconsistent. A Model must be consistent to be saved.");
			}
		}
		
		// require at least one declared instance
		Set<OWLNamedIndividual> individuals = aboxOntology.getIndividualsInSignature();
		if (individuals.isEmpty()) {
			errors.add("The model has no individuals/annotations. Empty models should not be saved.");
		}
		
		// avoid returning empty list
		if (errors.isEmpty()) {
			errors = null;
		}
		return errors;
	}
}
