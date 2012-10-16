package org.geneontology.annotation.view;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;
import org.geneontology.annotation.view.GraphvizImageRenderer.GraphvizConfigurationError;
import org.geneontology.lego.model.LegoTools.UnExpectedStructureException;
import org.protege.editor.core.ui.util.UIUtil;
import org.protege.editor.owl.model.OWLModelManager;
import org.protege.editor.owl.model.OWLWorkspace;
import org.protege.editor.owl.model.inference.OWLReasonerManager;
import org.protege.editor.owl.model.inference.ReasonerStatus;
import org.protege.editor.owl.model.inference.ReasonerUtilities;
import org.protege.editor.owl.ui.view.AbstractOWLViewComponent;
import org.semanticweb.owlapi.model.OWLException;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyChangeListener;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.graph.OWLGraphWrapper;


/**
 * Main panel for visualizing LEGO annotations.
 */
public class LegoAnnotationView extends AbstractOWLViewComponent {
	
	private static final String DEFAULT_EXPORT_FILE_TYPE = "png";
	private static final String UPDATE_VIEW_BUTTON_LABEL = "Update View";
	private static final String EXPORT_IMAGE_BUTTON_LABEL = "Export Image";

	private static final Logger LOGGER = Logger.getLogger(LegoAnnotationView.class);

	// generated
	private static final long serialVersionUID = 5346977837170670409L;
	
	private final OWLOntologyChangeListener listener;
	private final JPanel panel;
	private final JButton updateButton;
	private final JButton exportButton;
	
	private volatile boolean valid = false;
	
	private volatile BufferedImage currentImage = null;
	
	public LegoAnnotationView() {
		listener = new OWLOntologyChangeListener() {
			
			@Override
			public void ontologiesChanged(List<? extends OWLOntologyChange> change) throws OWLException {
				setInvalid();
			}
		};
		panel = new JPanel();
		updateButton = new JButton(UPDATE_VIEW_BUTTON_LABEL);
		updateButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent event) {
				SwingUtilities.invokeLater(new Runnable() {
					
					@Override
					public void run() {
						update();
					}
				});
			}
		});
		exportButton = new JButton(EXPORT_IMAGE_BUTTON_LABEL);
		exportButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				String title = "Save as Image";
				Set<String> extensions = new HashSet<String>();
				String desc = "Select a file to save the Lego Annotation View as Image.";
				final File file = UIUtil.saveFile(new JFrame(), title, desc, extensions);
				if (file != null) {
					SwingUtilities.invokeLater(new Runnable() {
						
						@Override
						public void run() {
							export(file);
						}
					});
				}
			}
		});
	}

	@Override
	protected void initialiseOWLView() throws Exception {
		setLayout(new BorderLayout());
		JScrollPane scrollPane = new JScrollPane(panel);
		add(scrollPane, BorderLayout.CENTER);
		JPanel buttonPanel = new JPanel();
		buttonPanel.add(updateButton);
		buttonPanel.add(exportButton);
		add(buttonPanel, BorderLayout.SOUTH);
		OWLModelManager manager = getOWLModelManager();
		manager.addOntologyChangeListener(listener);
		setInvalid();
	}

	@Override
	protected void disposeOWLView() {
		OWLModelManager manager = getOWLModelManager();
		manager.removeOntologyChangeListener(listener);
	}

	
	private synchronized void setInvalid() {
		valid = false;
		panel.removeAll();
		panel.add(new JLabel("<html><h1>No Visualization Available</h1>" +
				"<div>Please use the '"+UPDATE_VIEW_BUTTON_LABEL+"' button to generate the view of the current LEGO annotations.</div></html>"));
	}
	
	private synchronized void export(File outputFile) {
		if (currentImage == null) {
			// do nothing
			return;
		}
		final OWLWorkspace owlWorkspace = getOWLWorkspace();
		try {
			String format = DEFAULT_EXPORT_FILE_TYPE;
			String fileName = outputFile.getName();
			if (fileName.length() < 4) {
				outputFile = new File(outputFile.getParentFile(), fileName+"."+DEFAULT_EXPORT_FILE_TYPE);
			}
			else {
				final int suffixDotPos = fileName.lastIndexOf('.');
				if (suffixDotPos < 0) {
					outputFile = new File(outputFile.getParentFile(), fileName+"."+DEFAULT_EXPORT_FILE_TYPE);
				}
				else if ((suffixDotPos + 3) < fileName.length()) {
					format = fileName.substring(suffixDotPos + 1);
				}
			}
			ImageIO.write(currentImage, format, outputFile);
		} catch (IOException e) {
			handleError(owlWorkspace, e);
		}
	}
	
	private synchronized void update() {
		if (valid) {
			// do nothing
			return;
		}
		final OWLModelManager manager = getOWLModelManager();
		final OWLWorkspace owlWorkspace = getOWLWorkspace();
		final OWLReasonerManager reasonerManager = manager.getOWLReasonerManager();

		ReasonerUtilities.warnUserIfReasonerIsNotConfigured(owlWorkspace, reasonerManager);
		if (reasonerManager.getReasonerStatus() == ReasonerStatus.INITIALIZED) {
			OWLOntology ontology = manager.getActiveOntology();
			try {
				OWLGraphWrapper graph = new OWLGraphWrapper(ontology);
				OWLReasoner reasoner = reasonerManager.getCurrentReasoner();
				
				currentImage = GraphvizImageRenderer.renderLegoAnnotations(graph, reasoner);
				if (currentImage != null) {
					panel.removeAll();
					
					ImageZoomerPanel zoomer = new ImageZoomerPanel(currentImage, 10.0d);
					panel.add(zoomer);
					
					valid = true;
					panel.repaint();
					repaint();
				}
			} catch (UnExpectedStructureException e) {
				// TODO tell the user more about the unexpected LEGO annotation structure
				handleError(owlWorkspace, e);
			} catch (GraphvizConfigurationError e) {
				LOGGER.error(e.getMessage());
				JOptionPane.showMessageDialog(owlWorkspace,
						e.getMessage()+" Please check the LEGO annotation settings in the Protege Preferences.",
						"Graphviz Configuration Error.",
						JOptionPane.ERROR_MESSAGE);
			} catch (Exception e) {
				handleError(owlWorkspace, e);
			}
		}
	}

	private void handleError(final OWLWorkspace owlWorkspace, Throwable e) {
		String message = "Unable to create LEGO annotation view due to an internal error: "+e.getMessage();
		LOGGER.error(message, e);
		JOptionPane.showMessageDialog(owlWorkspace,
				message,
				"Internal Error.",
				JOptionPane.ERROR_MESSAGE);
	}
}
