package owltools.ontologyrelease.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;

import org.obolibrary.gui.GuiLogPanel;
import org.obolibrary.gui.GuiTools.SizedJPanel;

import owltools.InferenceBuilder;

/**
 * GUI main frame, calls all constructors for the sub components.
 */
public class ReleaseGuiMainFrame extends JFrame {
	
	// generated
	private static final long serialVersionUID = 6955854898081677364L;
	
	private JPanel allPanel;
	private ReleaseGuiMainPanel mainPanel;
	private ReleaseGuiAdvancedPanel advancedPanel;
	private GuiLogPanel logPanel;
	private final BlockingQueue<String> logQueue;
	private final OboOntologyReleaseRunnerParameters parameters;

	private JTabbedPane tabbedPane;

	private JButton releaseButton;
	
	/**
	 * Default constructor, required only for testing the GUI as bean.
	 */
	public ReleaseGuiMainFrame() {
		this(new ArrayBlockingQueue<String>(100));
	}
	
	/**
	 * Main constructor. 
	 * 
	 * @param logQueue Message queue for events to be shown in the log panel
	 */
	public ReleaseGuiMainFrame(BlockingQueue<String> logQueue) {
		super();
		this.logQueue = logQueue;
		this.parameters = new OboOntologyReleaseRunnerParameters();
		initialize();
	}

	private void initialize() {
		this.setSize(800, 500);
		// put the all panel in a scrollpane
		this.setContentPane(new JScrollPane(getAllPanel()));
		this.setTitle("OBO Release Manager");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);
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
			addTab(tabbedPane, "Input/Output", getMainPanel());
			addTab(tabbedPane, "Advanced", getAdvancedPanel());
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
				boolean success = getParametersFromGUI();
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
	 * @return boolean
	 */
	private boolean getParametersFromGUI() {
		// format
		// currently only one ontology format, do nothing
		
		// reasoner
		if (advancedPanel.pelletRadioButton.isSelected()) {
			parameters.setReasoner(InferenceBuilder.REASONER_PELLET);
		}
		else if (advancedPanel.hermitRadioButton.isSelected()) {
			parameters.setReasoner(InferenceBuilder.REASONER_HERMIT);
		}
		
		// asserted
		parameters.setAsserted(advancedPanel.assertedCheckBox.isSelected());
		
		// simple
		parameters.setSimple(advancedPanel.simpleCheckBox.isSelected());
		
		// paths
		ListModel inputFileModel = mainPanel.inputFileJList.getModel();
		Vector<String> paths = new Vector<String>();
		for (int i = 0; i < inputFileModel.getSize(); i++) {
			String path = inputFileModel.getElementAt(i).toString();
			paths.add(path);
		}
		if (paths.isEmpty()) {
			renderInputError("Configuration error. Please specify at least one ontology file to release");
			return false;
		}
		parameters.setPaths(paths);
		
		// base
		String base = mainPanel.outputFolderTextField.getText();
		if (base == null || base.length() < 1) {
			renderInputError("Configuration error. Please specify the output directory");
			return false;
		}
		parameters.setBase(new File(base));
		
		File baseDirectory = new File(base);

		if (!baseDirectory.exists()) {
			renderInputError("The base directory at " + base + " does not exist");
			return false;
		}

		if (!baseDirectory.canRead()) {
			renderInputError("Can't read the base directory at " + base);
			return false;
		}

		if (!baseDirectory.canWrite()) {
			renderInputError("Can't write in the base directory " + base);
			return false;
		}
		
		return true;
	}
	
	private void renderInputError(String message) {
		JOptionPane.showMessageDialog(ReleaseGuiMainFrame.this, message);
	}

	/**
	 * Execute the actual release. 
	 * Override this method to implement the call to the release methods.
	 * 
	 * @param config
	 */
	protected void executeRelease(OboOntologyReleaseRunnerParameters parameters) {
		// for tests print all parameter variables
		StringBuilder sb = new StringBuilder("-------------------------\n");
		sb.append(parameters.toString());
		logQueue.add(sb.toString());
	}
	
	/**
	 * Retrieve main panel, create new if it not exists.
	 * 
	 * @return main panel
	 */
	private SizedJPanel getMainPanel() {
		if (mainPanel == null) {
			mainPanel = new ReleaseGuiMainPanel(this, 
					parameters.getFormat(), 
					parameters.getPaths(), 
					parameters.getBase());
		}
		return mainPanel;
	}
	
	/**
	 * Retrieve advanced panel, create new if it not exists.
	 * 
	 * @return advanced panel
	 */
	private SizedJPanel getAdvancedPanel() {
		if (advancedPanel == null) {
			advancedPanel = new ReleaseGuiAdvancedPanel(
					parameters.getReasoner(), 
					parameters.isAsserted(), 
					parameters.isSimple());
		}
		return advancedPanel;
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
} 
