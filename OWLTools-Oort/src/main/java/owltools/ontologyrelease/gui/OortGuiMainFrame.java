package owltools.ontologyrelease.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import org.apache.commons.io.FileUtils;
import org.obolibrary.gui.GuiLogPanel;
import org.obolibrary.gui.GuiTools.SizedJPanel;

import owltools.InferenceBuilder;
import owltools.ontologyrelease.OortConfiguration;

/**
 * GUI main frame, calls all constructors for the sub components.
 */
public class OortGuiMainFrame extends JFrame {
	
	// generated
	private static final long serialVersionUID = 6955854898081677364L;
	
	private JPanel allPanel;
	private OortGuiMainPanel mainPanel;
	private OortGuiAdvancedPanel advancedPanel;
	private OortGuiQueryOntologyPanel dynamicOntologyPanel;
	private GuiLogPanel logPanel;
	private final BlockingQueue<String> logQueue;
	private final OortGuiConfiguration parameters;

	private JTabbedPane tabbedPane;

	private JButton releaseButton;
	
	/**
	 * @param parameters 
	 */
	public OortGuiMainFrame(OortGuiConfiguration parameters) {
		this(new ArrayBlockingQueue<String>(100), parameters);
	}
	
	/**
	 * Main constructor. 
	 * 
	 * @param logQueue Message queue for events to be shown in the log panel
	 * @param parameters 
	 */
	public OortGuiMainFrame(BlockingQueue<String> logQueue, OortGuiConfiguration parameters) {
		super();
		this.logQueue = logQueue;
		this.parameters = parameters;
		initialize();
	}

	private void initialize() {
		this.setSize(800, 500);
		// put the all panel in a scrollpane
		this.setContentPane(new JScrollPane(getAllPanel()));
		this.setTitle("OBO Release Manager");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setJMenuBar(createMenuBar());
		setVisible(true);
	}

	private JMenuBar createMenuBar() {
		return new OortGuiMenuBar(this, parameters);
	}

	/**
	 * Retrieve all panel, create new if it not exists.
	 * 
	 * @return all panel
	 */
	private JPanel getAllPanel() {
		if (allPanel == null) {
			// use tabs to structure the the three panels
			allPanel = new JPanel(new BorderLayout(10, 10));
			JTabbedPane tabbedPane = getTabbedPane();
			allPanel.add(tabbedPane, BorderLayout.CENTER);
			JPanel controlPanel = createControlPanel();
			allPanel.add(controlPanel, BorderLayout.PAGE_END);
		}
		return allPanel;
	}
	
	/**
	 * Retrieve tabbed pane, create new if it not exists.
	 * 
	 * @return tabbed pane
	 */
	private JTabbedPane getTabbedPane() {
		if (tabbedPane == null) {
			tabbedPane = new JTabbedPane();
			addTab(tabbedPane, "Input/Output", getMainPanel(getAdvancedPanel()));
			addTab(tabbedPane, "Advanced", getAdvancedPanel());
			addTab(tabbedPane, "Query Ontology", getDynamicOntologyPanel());
			addTab(tabbedPane, "Logs", getLogPanel());
		}
		return tabbedPane;
	}
	
	private void addTab(JTabbedPane tabbedPane, String title, SizedJPanel panel) {
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		tabbedPane.addTab(title, panel);
	}
	
	private JPanel createControlPanel() {
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 10));
		releaseButton = new JButton("Make Release");
		releaseButton.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent e) {
				boolean success = getParametersFromGUI(false);
				if (success) {
					// switch to log tab
					tabbedPane.setSelectedComponent(logPanel);
					SwingUtilities.invokeLater(new Runnable() {
						
//						@Override
						public void run() {
							// do work
							executeRelease(parameters);
						}
					});
				}
			}
		});
		panel.add(releaseButton, BorderLayout.LINE_END);
		return panel;
	}

	/**
	 * Retrieve the parameters values by reading the fields and states from the GUI.
	 * 
	 * @param silent if true, no warnings during parameter update. 
	 * @return boolean
	 */
	boolean getParametersFromGUI(boolean silent) {
		// format
		// currently only one ontology format, do nothing
		
		// reasoner
		if (advancedPanel.pelletRadioButton.isSelected()) {
			parameters.setReasonerName(InferenceBuilder.REASONER_PELLET);
		}
		else if (advancedPanel.hermitRadioButton.isSelected()) {
			parameters.setReasonerName(InferenceBuilder.REASONER_HERMIT);
		}
		else if (advancedPanel.jcelRadioButton.isSelected()) {
			parameters.setReasonerName(InferenceBuilder.REASONER_JCEL);
		}
		else if (advancedPanel.elkRadioButton.isSelected()) {
			parameters.setReasonerName(InferenceBuilder.REASONER_ELK);
		}
		
		// write formats
		parameters.getSkipFormatSet().clear();
		if (!advancedPanel.writeOBO.isSelected()) {
			parameters.addToSkipFormatSet("obo");
		}
		if (!advancedPanel.writeOWL.isSelected()) {
			parameters.addToSkipFormatSet("owl");
		}
		if (!advancedPanel.writeOWX.isSelected()) {
			parameters.addToSkipFormatSet("owx");
		}
		
		// asserted
		parameters.setAsserted(advancedPanel.assertedCheckBox.isSelected());
		
		// simple
		parameters.setSimple(advancedPanel.simpleCheckBox.isSelected());
		
		// expandXrefs
		parameters.setExpandXrefs(advancedPanel.expandXrefsCheckBox.isSelected());
		
		// allowOverwrite
		parameters.setAllowFileOverWrite(advancedPanel.allowOverwrite.isSelected());
		
		// recreateMireot
		parameters.setRecreateMireot(advancedPanel.recreateMireot.isSelected());
		
		// repairAnnotationCardinality
		parameters.setRepairAnnotationCardinality(advancedPanel.repairAnnotationCardinality.isSelected());
		
		// expandShortcutRelations
		parameters.setExpandShortcutRelations(advancedPanel.expandShortcutRelations.isSelected());
		
		// writeELOntology
		parameters.setWriteELOntology(advancedPanel.writeELOntologyCheckBox.isSelected());
		
		// JustifyAssertedSubclasses
		parameters.setJustifyAssertedSubclasses(advancedPanel.justifyAssertedSubclasses.isSelected());
		
		// writeSubSets
		parameters.setWriteSubsets(advancedPanel.writeSubSets.isSelected());
		
		// gafToOwl
		parameters.setGafToOwl(advancedPanel.gafToOwl.isSelected());
		
		// useQueryOntology
		parameters.setUseQueryOntology(dynamicOntologyPanel.activateCheckBox.isSelected());
		
		// queryOntology
		parameters.setQueryOntology(dynamicOntologyPanel.queryOntologyField.getText());
		
		// queryOntologyReference
		parameters.setQueryOntologyReference(dynamicOntologyPanel.queryTargetField.getText());
		
		// queryOntologyReferenceIsIRI
		parameters.setQueryOntologyReferenceIsIRI(dynamicOntologyPanel.queryTargetIriRadioButton.isSelected());
		
		// removeQueryOntologyReference
		parameters.setRemoveQueryOntologyReference(dynamicOntologyPanel.removeQueryTermCheckBox.isSelected());
		
		// paths
		parameters.getPaths().clear();
		for(String source : mainPanel.sources.keySet()) {
			parameters.addPath(source);
		}
		if (!silent && parameters.getPaths().isEmpty()) {
			renderInputError("Configuration error. Please specify at least one ontology file to release");
			return false;
		}
		
		// base
		String base = mainPanel.outputFolderTextField.getText();
		if (base == null || base.length() < 1) {
			renderInputError("Configuration error. Please specify the output directory");
			return false;
		}
		parameters.setBase(new File(base));
		
		File baseDirectory = parameters.getBase();

		if (!silent) {
			if (!baseDirectory.exists()) {
				try {
					FileUtils.forceMkdir(baseDirectory);
				} catch (IOException e) {
					renderInputError("Can't create the base directory at "
							+ baseDirectory);
				}
			}
			if (!baseDirectory.canRead()) {
				renderInputError("Can't read the base directory at " + base);
				return false;
			}
			if (!baseDirectory.canWrite()) {
				renderInputError("Can't write in the base directory " + base);
				return false;
			}
		}
		return true;
	}
	
	private void renderInputError(String message) {
		JOptionPane.showMessageDialog(OortGuiMainFrame.this, message);
	}

	/**
	 * Execute the actual release. 
	 * Override this method to implement the call to the release methods.
	 * 
	 * @param parameters 
	 */
	protected void executeRelease(OortGuiConfiguration parameters) {
		// for tests print all parameter variables
		StringBuilder sb = new StringBuilder("-------------------------\n");
		sb.append(parameters.toString());
		logQueue.add(sb.toString());
	}
	
	/**
	 * Retrieve main panel, create new if it not exists.
	 * 
	 * @param advancedPanel 
	 * @return main panel
	 */
	private SizedJPanel getMainPanel(OortGuiAdvancedPanel advancedPanel) {
		if (mainPanel == null) {
			mainPanel = new OortGuiMainPanel(this, parameters, advancedPanel);
		}
		return mainPanel;
	}
	
	/**
	 * Retrieve advanced panel, create new if it not exists.
	 * 
	 * @return advanced panel
	 */
	protected OortGuiAdvancedPanel getAdvancedPanel() {
		if (advancedPanel == null) {
			advancedPanel = new OortGuiAdvancedPanel(parameters);
		}
		return advancedPanel;
	}
	
	private OortGuiQueryOntologyPanel getDynamicOntologyPanel() {
		if (dynamicOntologyPanel == null) {
			dynamicOntologyPanel = new OortGuiQueryOntologyPanel(this, parameters);
		}
		return dynamicOntologyPanel;
	}

	/**
	 * Retrieve log panel, create new if it not exists.
	 * 
	 * @return log panel
	 */
	private SizedJPanel getLogPanel() {
		if (logPanel == null) {
			logPanel = new GuiLogPanel(logQueue);
		}
		return logPanel;
	}
	
	protected void disableReleaseButton() {
		releaseButton.setEnabled(false);
	}
	
	protected void enableReleaseButton() {
		releaseButton.setEnabled(true);
	}
	
	void applyConfig(OortConfiguration configuration) {
		mainPanel.applyConfig(configuration);
		advancedPanel.applyConfig(configuration);
		dynamicOntologyPanel.applyConfig(configuration);
	}
} 
