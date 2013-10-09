package org.geneontology.model;

import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JOptionPane;

import org.apache.log4j.Logger;
import org.protege.editor.owl.model.OWLModelManager;
import org.protege.editor.owl.model.OWLWorkspace;
import org.protege.editor.owl.model.inference.OWLReasonerManager;
import org.protege.editor.owl.model.inference.ReasonerStatus;
import org.protege.editor.owl.model.inference.ReasonerUtilities;
import org.protege.editor.owl.ui.action.SelectedOWLClassAction;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import owltools.graph.OWLGraphWrapper;
import owltools.util.MinimalModelGenerator;

public class MakeMinimalModelMenuAction extends SelectedOWLClassAction {

	// generated
	private static final long serialVersionUID = -4300910091424151154L;

	private static final Logger LOGGER = Logger.getLogger(MakeMinimalModelMenuAction.class);
	
	private volatile MinimalModelGenerator modelGenerator = null;

	@Override
	protected void initialiseAction() throws Exception {
		// do nothing
	}

	@Override
	public synchronized void actionPerformed(ActionEvent event) {
		final OWLModelManager manager = getOWLModelManager();
		final OWLWorkspace owlWorkspace = getOWLWorkspace();
		final OWLReasonerManager reasonerManager = manager.getOWLReasonerManager();
		
		ReasonerUtilities.warnUserIfReasonerIsNotConfigured(owlWorkspace, reasonerManager);
		OWLNamedIndividual individual = null;
		if (reasonerManager.getReasonerStatus() == ReasonerStatus.INITIALIZED) {
			try {
				OWLOntology ontology = manager.getActiveOntology();
				if (modelGenerator == null) {
					modelGenerator = new MinimalModelGenerator(ontology, ontology, reasonerManager.getCurrentReasoner());
				}
				else {
					modelGenerator.setReasoner(reasonerManager.getCurrentReasoner());
				}
				individual = modelGenerator.generateNecessaryIndividuals(getOWLClass(), true);
				
//				Set<OWLClass> occs = new HashSet<OWLClass>();
//				OWLGraphWrapper g = new OWLGraphWrapper(ontology);
//				occs.add(g.getOWLClassByIdentifier("GO:0003674"));
//				occs.add(g.getOWLClassByIdentifier("GO:0008150"));
//				modelGenerator.anonymizeIndividualsNotIn(occs);
			} catch (OWLOntologyCreationException exception) {
				handleError(owlWorkspace, exception);
			}
		}
		LOGGER.info("Finished: "+individual);
	}

	private void handleError(final OWLWorkspace owlWorkspace, Throwable e) {
		String message = "Unable to make minimal model due to an internal error: "+e.getMessage();
		LOGGER.error(message, e);
		JOptionPane.showMessageDialog(owlWorkspace,
				message,
				"Internal Error.",
				JOptionPane.ERROR_MESSAGE);
	}
	
	@Override
	public void dispose() {
		modelGenerator = null;
		super.dispose();
	}

}
