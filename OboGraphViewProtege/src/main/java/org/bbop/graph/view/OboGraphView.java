package org.bbop.graph.view;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;
import org.bbop.obo.GraphViewCanvas;
import org.protege.editor.owl.model.OWLModelManager;
import org.protege.editor.owl.model.OWLWorkspace;
import org.protege.editor.owl.model.inference.OWLReasonerManager;
import org.protege.editor.owl.model.inference.ReasonerStatus;
import org.protege.editor.owl.model.inference.ReasonerUtilities;
import org.protege.editor.owl.model.selection.OWLSelectionModel;
import org.protege.editor.owl.model.selection.OWLSelectionModelListener;
import org.protege.editor.owl.ui.view.AbstractOWLViewComponent;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLException;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyChangeListener;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.graph.OWLGraphWrapper;


/**
 * Main panel for showing the OBO style graph view.
 */
public class OboGraphView extends AbstractOWLViewComponent {

	// generated
	private static final long serialVersionUID = -7911138828225648L;

	private static final Logger LOGGER = Logger.getLogger(OboGraphView.class);

	private final OWLOntologyChangeListener ontologyChangeListener;
	private final OWLSelectionModelListener selectionListener;
	private volatile boolean valid = false;
	
	private final JPanel contentPanel;
	private final JButton updateButton;
	
	private GraphViewCanvas canvas = null;

	public OboGraphView() {
		ontologyChangeListener = new OWLOntologyChangeListener() {
			
			@Override
			public void ontologiesChanged(List<? extends OWLOntologyChange> change) throws OWLException {
				setInvalid();
			}
		};
		selectionListener = new OWLSelectionModelListener() {
			
			@Override
			public void selectionChanged() throws Exception {
				final OWLSelectionModel selectionModel = getOWLWorkspace().getOWLSelectionModel();
				final OWLClass owlClass = selectionModel.getLastSelectedClass();
				if (owlClass != null && canvas != null) {
					canvas.setSelected(Collections.<OWLObject>singleton(owlClass));
				}
			}
		};
		contentPanel = new JPanel(new BorderLayout());
		updateButton = new JButton("Synchronize View");
		updateButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					
					@Override
					public void run() {
						update();
					}
				});
			}
		});
		
	}

	@Override
	protected void initialiseOWLView() throws Exception {
		add(contentPanel, BorderLayout.CENTER);
		add(updateButton, BorderLayout.SOUTH);
		
		// register listener for ontology changes
		final OWLModelManager manager = getOWLModelManager();
		manager.addOntologyChangeListener(ontologyChangeListener);
		
		// register listener for selection changes
		final OWLWorkspace owlWorkspace = getOWLWorkspace();
		owlWorkspace.getOWLSelectionModel().addListener(selectionListener);
		
		// do not draw an initial graph canvas, as a reasoner is required.
		setInvalid();
	}

	@Override
	protected void disposeOWLView() {
		setInvalid();
		final OWLModelManager manager = getOWLModelManager();
		manager.removeOntologyChangeListener(ontologyChangeListener);
		
		final OWLWorkspace owlWorkspace = getOWLWorkspace();
		owlWorkspace.getOWLSelectionModel().removeListener(selectionListener);
	}

	
	private synchronized void setInvalid() {
		valid = false;
		if (canvas != null) {
			contentPanel.remove(canvas);
			canvas = null;
		}
	}
	
	private synchronized void setValid(GraphViewCanvas canvas) {
		this.canvas = canvas;
		contentPanel.add(canvas);
		valid = true;
		validate();
	}
	
	private synchronized void update() {
		if (valid) {
			// do nothing
			return;
		}
		final OWLModelManager manager = getOWLModelManager();
		final OWLWorkspace owlWorkspace = getOWLWorkspace();
		final OWLReasonerManager reasonerManager = manager.getOWLReasonerManager();
		final OWLClass selectedClass = owlWorkspace.getOWLSelectionModel().getLastSelectedClass();
		ReasonerUtilities.warnUserIfReasonerIsNotConfigured(owlWorkspace, reasonerManager);
		if (reasonerManager.getReasonerStatus() == ReasonerStatus.INITIALIZED) {
			OWLOntology ontology = manager.getActiveOntology();
			try {
				OWLReasoner reasoner = reasonerManager.getCurrentReasoner();
				
				OWLGraphWrapper graph = new OWLGraphWrapper(ontology);
				Set<OWLObject> selection = null;
				if (selectedClass != null) {
					selection = Collections.<OWLObject>singleton(selectedClass);
				}
				GraphViewCanvas canvas = new GraphViewCanvas(graph, reasoner, selection);
				setValid(canvas);
				
			} catch (Exception e) {
				handleError(owlWorkspace, e);
			}
		}
	}
	
	private void handleError(final OWLWorkspace owlWorkspace, Throwable e) {
		String message = "Unable to update view due to an internal error: "+e.getMessage();
		LOGGER.error(message, e);
		JOptionPane.showMessageDialog(owlWorkspace,
				message,
				"Internal Error.",
				JOptionPane.ERROR_MESSAGE);
	}
}
