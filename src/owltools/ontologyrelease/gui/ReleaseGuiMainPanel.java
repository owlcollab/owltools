package owltools.ontologyrelease.gui;

import static org.obolibrary.gui.GuiTools.addRowGap;
import static org.obolibrary.gui.GuiTools.createTextField;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import org.apache.log4j.Logger;
import org.obolibrary.gui.GuiTools.GBHelper;
import org.obolibrary.gui.GuiTools.SizedJPanel;
import org.obolibrary.gui.SelectDialog;
import org.semanticweb.owlapi.model.OWLOntologyFormat;

/**
 * GUI component for the minimum number of configurations
 * required for the release manager.
 */
public class ReleaseGuiMainPanel extends SizedJPanel {

	/// Generated
	private static final long serialVersionUID = 1281395185956670966L;

	private final static Logger LOGGER = Logger.getLogger(ReleaseGuiMainPanel.class); 
	
	private final Frame frame;
	
	final JList inputFileJList;
	
	final JTextField outputFolderTextField;
	
	final JRadioButton rdfXmlRadioButton;

	/**
	 * Constructor allows to build a panel with default values
	 * 
	 * @param frame
	 * @param defaultFormat
	 * @param defaultPaths
	 * @param defaultBase
	 */
	public ReleaseGuiMainPanel(Frame frame, OWLOntologyFormat defaultFormat, 
			Vector<String> defaultPaths, File defaultBase)
	{
		super();
		this.frame = frame;

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
		
		// output format buttons
		rdfXmlRadioButton = new JRadioButton("RDF XML");
		
		// output folder
		String canonicalPath = getCanonicalPath(defaultBase);
		outputFolderTextField = createTextField(canonicalPath);
		
		setLayout(new GridBagLayout());
		GBHelper pos = new GBHelper();
		
		// input panel
		createInputPanel(pos, canonicalPath);
		addRowGap(this, pos, 10);
		
		// output panel
		createOutputPanel(pos, canonicalPath);
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
	private void createInputPanel(GBHelper pos, String defaultFolder) {
		// store the files in linked hash map, advantage: maintains insert order
		final LinkedHashMap<String, File> files = new LinkedHashMap<String, File>();
		
		JButton fileDialogAddButton = new JButton("Add");
		JScrollPane scrollPane = new JScrollPane(inputFileJList);
		inputFileJList.setPreferredSize(new Dimension(350, 60));
		
		// use file chooser dialog for select input files
		final SelectDialog fileDialog = SelectDialog.getFileSelector(frame, 
				SelectDialog.LOAD,
				defaultFolder,
				"Input ontology file choose dialog", 
				"OBO & OWL files", 
				new String[]{"obo","owl"});
		
		// add listener for adding a file to the list model
		fileDialogAddButton.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent e) {
				fileDialog.show();
				String selected = fileDialog.getSelectedCanonicalPath();
				if (selected != null) {
					File file = new File(selected);
					File old = files.put(selected, file);
					// only update the model, if the file was not there before
					if (old == null) {
						updateInputFileData(inputFileJList, files);
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
		add(scrollPane, pos.nextCol().indentLeft(10).expandW().expandH().anchorCenter().height(4).fill());
		add(fileDialogAddButton, pos.nextCol().indentLeft(10));
		add(fileRemoveButton, pos.nextRow().nextCol().nextCol().indentLeft(10));
		pos.nextRow();
		pos.nextRow();
	}
	
	/**
	 * Create layout and listener for the output field.
	 * 
	 * @param pos
	 */
	private void createOutputPanel(GBHelper pos, String defaultFolder) {
		// file chooser for folders
		final SelectDialog folderDialog = 
			SelectDialog.getFolderSelector(frame, defaultFolder, "Work directory choose dialog");
		
		final JButton folderButton = new JButton("Select");
		
		// add listener for selecting an output folder
		folderButton.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent e) {
				folderDialog.show();
				String selected = folderDialog.getSelectedCanonicalPath();
				if (selected != null) {
					outputFolderTextField.setText(selected);
				}
			}
		});
		
		add(new JLabel("Output"),pos.nextRow());
		addRowGap(this, pos, 10);
		
		add(new JLabel("Folder"),pos.nextRow());
		add(outputFolderTextField,pos.nextCol().indentLeft(10).expandW().anchorCenter().fill());
		add(folderButton, pos.nextCol().indentLeft(10));
		addRowGap(this, pos, 2);
	}
	
	/**
	 * Update the list model with a new list of files. 
	 * 
	 * @param fileList
	 * @param files
	 */
	private void updateInputFileData(JList fileList, Map<String, File> files) {
		DefaultListModel listModel = new DefaultListModel();
		for (String name : files.keySet()) {
			listModel.addElement(name);
		}
		fileList.setModel(listModel);
	}

	/**
	 * Create layout for ontology format selection.
	 * 
	 * INFO: Currently unused. This method is implemented for 
	 * potential later use for exporting to different OWL formats.
	 * 
	 * @param pos
	 * @param defaultFormat
	 */
	@SuppressWarnings("unused")
	private void createFormatPanel(GBHelper pos, OWLOntologyFormat defaultFormat) {
		add(new JLabel("Ontology Format"), pos.nextRow());
		add(rdfXmlRadioButton, pos.nextRow().nextCol());
		rdfXmlRadioButton.setSelected(true);
	}
	
	/**
	 * Helper method to extract the canonical path from a file.
	 * 
	 * @param file
	 * @return canonical path
	 */
	private static String getCanonicalPath(File file) {
		try {
			return file.getCanonicalPath();
		} catch (IOException e) {
			LOGGER.error("Unable to get canonical path for file: "+file.getAbsolutePath(), e);
		}
		return null;
	}
}
