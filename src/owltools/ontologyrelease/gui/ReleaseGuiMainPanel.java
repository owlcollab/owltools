package owltools.ontologyrelease.gui;

import static org.obolibrary.gui.GuiTools.addRowGap;
import static org.obolibrary.gui.GuiTools.createTextField;

import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;

import org.apache.log4j.Logger;
import org.obolibrary.gui.GuiTools.GBHelper;
import org.obolibrary.gui.GuiTools.SizedJPanel;
import org.semanticweb.owlapi.model.OWLOntologyFormat;

/**
 * GUI component for the minimum number of configurations required for the conversion.
 */
public class ReleaseGuiMainPanel extends SizedJPanel {

	/// Generated
	private static final long serialVersionUID = 1281395185956670966L;

	private final static Logger LOGGER = Logger.getLogger(ReleaseGuiMainPanel.class); 
	
	final JList inputFileJList;
	final JCheckBox assertedCheckBox;
	final JCheckBox simpleCheckBox;
	final JTextField outputFolderTextField;
	
	final JRadioButton pelletRadioButton;
	final JRadioButton hermitRadioButton;
	final JRadioButton factppRadioButton;
	final JRadioButton jcelRadioButton;
	
	final JRadioButton rdfXmlRadioButton;

	// Can be replaced with FileNameExtensionFilter in JAVA6
	private final static FileFilter OBO_OWL_FILE_FILTER = new FileFilter() {
		
		private final Set<String> extensions = new HashSet<String>(Arrays.asList("obo","owl"));
		
		@Override
		public String getDescription() {
			return "OBO & OWL files";
		}
		
		@Override
		public boolean accept(File f) {
			if (f != null) {
	            if (f.isDirectory()) {
	                return true;
	            }
	            String fileName = f.getName();
	            int i = fileName.lastIndexOf('.');
	            if (i > 0 && i < fileName.length() - 1) {
	                String extension = fileName.substring(i + 1).toLowerCase();
	                return extensions.contains(extension);
	            }
	        }
	        return false;
		}
	};
	
	/**
	 * Constructor allows to build a panel with default values
	 * 
	 * @param defaultFormat
	 * @param defaultReasoner
	 * @param defaultIsAsserted
	 * @param defaultIsSimple
	 * @param defaultPaths
	 * @param defaultBase
	 */
	public ReleaseGuiMainPanel(OWLOntologyFormat defaultFormat, 
			String defaultReasoner, boolean defaultIsAsserted, boolean defaultIsSimple,
			Vector<String> defaultPaths, File defaultBase)
	{
		super();

		// create accessible fields
		// add default values to these fields
		
		// ontology file input files
		DefaultListModel inputFilesDataModel = new DefaultListModel();
		if (defaultPaths != null) {
			for (String inputFile : defaultPaths) {
				inputFilesDataModel.addElement(inputFile);
			}
		}
		inputFileJList = new JList(inputFilesDataModel);
		
		// options flags
		assertedCheckBox = new JCheckBox("asserted");
		simpleCheckBox = new JCheckBox("simple");
		
		// reasoner radio buttons
		pelletRadioButton = new JRadioButton("Pellet");
		hermitRadioButton = new JRadioButton("HermiT");
		factppRadioButton = new JRadioButton("Fact++");
		jcelRadioButton = new JRadioButton("JCEL");
		
		// output format buttons
		rdfXmlRadioButton = new JRadioButton("RDF XML");
		
		// output folder
		String canonicalPath = getCanonicalPath(defaultBase);
		outputFolderTextField = createTextField(canonicalPath);
		
		setLayout(new GridBagLayout());
		GBHelper pos = new GBHelper();
		
		// input panel
		createInputPanel(pos);
		addRowGap(this, pos, 10);
		
		// output panel
		createOutputPanel(pos);
		addRowGap(this, pos, 10);
		
		// options
		createOptionPanel(pos, defaultIsAsserted, defaultIsSimple);
		addRowGap(this, pos, 10);
		
		// reasoner
		createReasonerPanel(pos, defaultReasoner);
		addRowGap(this, pos, 10);
		
//		// format
//		createFormatPanel(pos, defaultFormat);
//		addRowGap(this, pos, 10);
	}
	
	/**
	 * Create layout and listeners for the ontology input files to be released.
	 * 
	 * @param pos
	 */
	private void createInputPanel(GBHelper pos) {
		// store the files in linked hash map, advantage: maintains insert order
		final LinkedHashMap<String, File> files = new LinkedHashMap<String, File>();
		
		// use file chooser dialog for select input files
		final JFileChooser fc = new JFileChooser();
		String defaultInputFolder = ".";
		if (lastAddedFile != null) {
			File latestFile = files.get(lastAddedFile);
			if (latestFile != null) {
				String canonicalPath = getCanonicalPath(latestFile.getParentFile());
				if (canonicalPath != null) {
					defaultInputFolder = canonicalPath;
				}
			}
		}
		fc.setCurrentDirectory(new File(defaultInputFolder));
		fc.setDialogTitle("Input ontology file choose dialog");
		fc.setFileFilter(OBO_OWL_FILE_FILTER);
		JScrollPane scrollPane = new JScrollPane(inputFileJList);
		inputFileJList.setPreferredSize(new Dimension(350, 60));
		
		JButton fileDialogAddButton = new JButton("Add");
		
		// add listener for adding a file to the list model
		fileDialogAddButton.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent e) {
				int returnVal = fc.showOpenDialog(ReleaseGuiMainPanel.this);
	
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					File file = fc.getSelectedFile();
					String path = getCanonicalPath(file);
					if (path != null) {
						File old = files.put(path, file);
						// only update the model, if the file was not there before
						if (old == null) {
							updateInputFileData(inputFileJList, files);
						}
					}
				}
			}
		});
		
		// add listener for removing files from the list model
		JButton fileRemoveButton = new JButton("Remove");
		fileRemoveButton.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent e) {
				Object[] values = inputFileJList.getSelectedValues();
				if (values != null && values.length > 0) {
					for (Object object : values) {
						files.remove(object);
					}
					updateInputFileData(inputFileJList, files);
				}
			}
		});
		
		add(new JLabel("Input"), pos);
		addRowGap(this, pos, 10);
		
		add(new JLabel("Ontology Files"), pos.nextRow());
		add(scrollPane, pos.nextCol().expandW().expandH().anchorCenter().height(4).fill());
		add(fileDialogAddButton, pos.nextCol().indentLeft(10));
		pos.nextRow();
		add(fileRemoveButton, pos.nextCol().nextCol().indentLeft(10));
		pos.nextRow();
		pos.nextRow();
	}

	/**
	 * Create layout and listener for the output field.
	 * 
	 * @param pos
	 */
	private void createOutputPanel(GBHelper pos) {
		// file chooser for folders
		final JFileChooser folderFC = new JFileChooser();
		folderFC.setCurrentDirectory(new File(outputFolderTextField.getText()));
		folderFC.setDialogTitle("Work directory choose dialog");
		folderFC.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		folderFC.setAcceptAllFileFilterUsed(false);
		
		// file choosers for output file names
		final JFileChooser fileFC = new JFileChooser();
		fileFC.setFileFilter(OBO_OWL_FILE_FILTER);
		
		final JButton folderButton = new JButton("Select");
		
		// add listener for selecting an output folder
		folderButton.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent e) {
				int returnVal = folderFC.showOpenDialog(ReleaseGuiMainPanel.this);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					File file = folderFC.getSelectedFile();
					// set new folder and remove any previous file names
					setOutputNames(file, null);
				}
			}
		});
		
		add(new JLabel("Output"),pos.nextRow());
		addRowGap(this, pos, 10);
		
		add(new JLabel("Folder"),pos.nextRow());
		add(outputFolderTextField,pos.nextCol().expandW().anchorCenter().fill());
		add(folderButton, pos.nextCol().indentLeft(10));
		addRowGap(this, pos, 2);
	}
	
	private void setOutputNames(File folder, String name) {
		try {
			String absolutePath = folder.getCanonicalPath();
			outputFolderTextField.setText(absolutePath);
		} catch (IOException e) {
			LOGGER.error("Can't convert file to pathname", e);
		}
	}

	private String lastAddedFile = null;

	/**
	 * Update the list model with a new list of files. 
	 * Keeps also the {@link #lastAddedFile} value up-to-date.
	 * 
	 * @param fileList
	 * @param files
	 */
	private void updateInputFileData(JList fileList, Map<String, File> files) {
		DefaultListModel listModel = new DefaultListModel();
		lastAddedFile = null;
		for (String name : files.keySet()) {
			listModel.addElement(name);
			lastAddedFile = name;
		}
		fileList.setModel(listModel);
	}

	/**
	 * Create layout for ontology release option flags
	 * 
	 * @param pos
	 * @param defaultIsAsserted
	 * @param defaultIsSimple
	 */
	private void createOptionPanel(GBHelper pos, boolean defaultIsAsserted, boolean defaultIsSimple) {
		add(new JLabel("Options"), pos.nextRow());
		add(assertedCheckBox, pos.nextRow().nextCol());
		add(simpleCheckBox, pos.nextRow().nextCol());
		
		// set default value for buttons
		assertedCheckBox.setSelected(defaultIsAsserted);
		simpleCheckBox.setSelected(defaultIsSimple);
	}

	/**
	 * Create layout for reasoner selection
	 * 
	 * @param pos
	 * @param defaultReasoner
	 */
	private void createReasonerPanel(GBHelper pos, String defaultReasoner) {
		add(new JLabel("Reasoner"), pos.nextRow());
		
		ButtonGroup reasonerGroup = new ButtonGroup();
		reasonerGroup.add(pelletRadioButton);
		reasonerGroup.add(hermitRadioButton);
		reasonerGroup.add(factppRadioButton);
		reasonerGroup.add(jcelRadioButton);
		
		JPanel reasonerPanel = new JPanel(new GridLayout(2, 2, 20, 5));
		reasonerPanel.add(pelletRadioButton);
		reasonerPanel.add(hermitRadioButton);
		reasonerPanel.add(factppRadioButton);
		reasonerPanel.add(jcelRadioButton);
		
		pelletRadioButton.setSelected(true);
		hermitRadioButton.setEnabled(false);
		factppRadioButton.setEnabled(false);
		jcelRadioButton.setEnabled(false);
		
		add(reasonerPanel, pos.nextCol());
	}

	/**
	 * Create layout for ontology format selection
	 * 
	 * @param pos
	 * @param defaultFormat
	 */
	private void createFormatPanel(GBHelper pos, OWLOntologyFormat defaultFormat) {
		add(new JLabel("Ontology Format"), pos.nextRow());
		add(rdfXmlRadioButton, pos.nextRow().nextCol());
		rdfXmlRadioButton.setSelected(true);
	}
	
	private static String getCanonicalPath(File file) {
		try {
			return file.getCanonicalPath();
		} catch (IOException e) {
			LOGGER.error("Unable to get canonical path for file: "+file.getAbsolutePath(), e);
		}
		return null;
	}
}
