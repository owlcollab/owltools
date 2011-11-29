package owltools.ontologyrelease.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.obolibrary.gui.GuiLogPanel;
import org.obolibrary.gui.GuiTools.SizedJPanel;
import org.obolibrary.gui.SelectDialog;

import owltools.InferenceBuilder;
import owltools.ontologyrelease.OortConfiguration;

/**
 * GUI main frame, calls all constructors for the sub components.
 */
public class ReleaseGuiMainFrame extends JFrame {
	
	private static final Logger LOGGER = Logger.getLogger(ReleaseGuiMainFrame.class);
	
	// generated
	private static final long serialVersionUID = 6955854898081677364L;
	
	private JPanel allPanel;
	private ReleaseGuiMainPanel mainPanel;
	private ReleaseGuiAdvancedPanel advancedPanel;
	private GuiLogPanel logPanel;
	private final BlockingQueue<String> logQueue;
	private final GUIOortConfiguration parameters;

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
		this.parameters = new GUIOortConfiguration();
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
		JMenuBar menuBar = new JMenuBar();
		
		// add file menu
		JMenu fileMenu = new JMenu("File");
		fileMenu.setMnemonic(KeyEvent.VK_F);
		menuBar.add(fileMenu);
		
		// load configuration from file
		JMenuItem loadItem = new JMenuItem("Load Configuration");
		fileMenu.add(loadItem);
		loadItem.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				String defaultFolder = FileUtils.getUserDirectoryPath();
				String title = "Select OORT configuation file";
				SelectDialog dialog = SelectDialog.getFileSelector(ReleaseGuiMainFrame.this, SelectDialog.LOAD, defaultFolder, title, null, null);
				dialog.show();
				File selected = dialog.getSelected();
				if (selected != null) {
					try {
						OortConfiguration.loadConfig(selected, parameters);
						applyConfig(parameters);
						LOGGER.info("Finished loading OORT config from file: "+selected);
					} catch (IOException exception) {
						LOGGER.warn("Could not load config file: "+selected, exception);
					}
				}
				
			}
		});
		
		// store configuration to file
		JMenuItem storeItem = new JMenuItem("Store Configuration");
		storeItem.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				getParametersFromGUI(true);
				String defaultFolder = FileUtils.getUserDirectoryPath();
				String title = "Select OORT configuation file";
				SelectDialog dialog = SelectDialog.getFileSelector(ReleaseGuiMainFrame.this, SelectDialog.SAVE, defaultFolder, title, null, null);
				dialog.show();
				File selected = dialog.getSelected();
				if (selected != null) {
					try {
						OortConfiguration.writeConfig(selected, parameters);
						LOGGER.info("Finished saving OORT config to file: "+selected);
					} catch (IOException exception) {
						LOGGER.warn("Could not save OORT config to file: "+selected, exception);
					}
				}
			}
		});
		fileMenu.add(storeItem);
		
		// Exit
		fileMenu.addSeparator();
		JMenuItem exitItem = new JMenuItem("Exit");
		exitItem.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				ReleaseGuiMainFrame.this.dispose();
				System.exit(0);
			}
		});
		fileMenu.add(exitItem);
		
		return menuBar;
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
	private boolean getParametersFromGUI(boolean silent) {
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
		
		// expandShortcutRelations
		parameters.setExpandShortcutRelations(advancedPanel.expandShortcutRelations.isSelected());
		
		// writeELOntology
		parameters.setWriteELOntology(advancedPanel.writeELOntologyCheckBox.isSelected());
		
		// JustifyAssertedSubclasses
		parameters.setJustifyAssertedSubclasses(advancedPanel.justifyAssertedSubclasses.isSelected());
		
		// paths
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
		JOptionPane.showMessageDialog(ReleaseGuiMainFrame.this, message);
	}

	/**
	 * Execute the actual release. 
	 * Override this method to implement the call to the release methods.
	 * 
	 * @param parameters 
	 */
	protected void executeRelease(GUIOortConfiguration parameters) {
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
	private SizedJPanel getMainPanel(ReleaseGuiAdvancedPanel advancedPanel) {
		if (mainPanel == null) {
			mainPanel = new ReleaseGuiMainPanel(this, parameters, advancedPanel);
		}
		return mainPanel;
	}
	
	/**
	 * Retrieve advanced panel, create new if it not exists.
	 * 
	 * @return advanced panel
	 */
	protected ReleaseGuiAdvancedPanel getAdvancedPanel() {
		if (advancedPanel == null) {
			advancedPanel = new ReleaseGuiAdvancedPanel(parameters);
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
	
	private void applyConfig(OortConfiguration configuration) {
		mainPanel.applyConfig(configuration);
		advancedPanel.applyConfig(configuration);
	}
} 
