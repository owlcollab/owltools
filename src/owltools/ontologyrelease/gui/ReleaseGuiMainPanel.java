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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import org.apache.log4j.Logger;
import org.obolibrary.gui.GuiTools.GBHelper;
import org.obolibrary.gui.GuiTools.SizedJPanel;
import org.obolibrary.gui.SelectDialog;

/**
 * GUI component for the minimum number of configurations
 * required for the release manager.
 */
public class ReleaseGuiMainPanel extends SizedJPanel {

	/// Generated
	private static final long serialVersionUID = 1281395185956670966L;

	private final static Logger LOGGER = Logger.getLogger(ReleaseGuiMainPanel.class); 
	
	private final Frame frame;
	private final ReleaseGuiAdvancedPanel advancedPanel;
	
	final JTextField outputFolderTextField;
	
	final JRadioButton rdfXmlRadioButton;

	final LinkedHashMap<String, Object> sources;

	/**
	 * Constructor allows to build a panel with default values.
	 * 
	 * @param frame
	 * @param defaultPaths
	 * @param defaultBase
	 * @param advancedPanel
	 */
	public ReleaseGuiMainPanel(Frame frame, Vector<String> defaultPaths, 
			File defaultBase, ReleaseGuiAdvancedPanel advancedPanel)
	{
		super();
		this.frame = frame;
		this.advancedPanel = advancedPanel;

		// create accessible fields
		// add default values to these fields
		
		// ontology file input files
		sources = new LinkedHashMap<String, Object>();
		
		// output format buttons
		rdfXmlRadioButton = new JRadioButton("RDF XML");
		
		// output folder
		String canonicalPath = getCanonicalPath(defaultBase);
		outputFolderTextField = createTextField(canonicalPath);
		
		setLayout(new GridBagLayout());
		GBHelper pos = new GBHelper();
		
		// input panel
		createInputPanel(pos, defaultPaths, canonicalPath);
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
	 * @param defaultPaths
	 * @param defaultFolder
	 */
	private void createInputPanel(GBHelper pos, Vector<String> defaultPaths, String defaultFolder) {
		// ontology file input files
		DefaultListModel inputFilesDataModel = new DefaultListModel();
		if (defaultPaths != null) {
			for (String inputFile : defaultPaths) {
				Object put = sources.put(inputFile, inputFile);
				if (put == null) {
					inputFilesDataModel.addElement(inputFile);
				}
				
			}
		}
		final JList inputSourcesJList = new JList(inputFilesDataModel);
		
		final JButton fileDialogAddButton = new JButton("Add File");
		final JButton urlDialogAddButton = new JButton("Add URL");
		final JButton fileRemoveButton = new JButton("Remove");
		
		// set the preferred dimensions for the three buttons
		final Dimension fileDialogButtonDimensions = fileDialogAddButton.getPreferredSize();
		final Dimension urlDialogButtonDimensions = urlDialogAddButton.getPreferredSize();
		if (fileDialogButtonDimensions != null && urlDialogButtonDimensions != null) {
			// use the widest button as reference
			int width = Math.max(fileDialogButtonDimensions.width, urlDialogButtonDimensions.width);
			Dimension preferredSize = new Dimension(width, fileDialogButtonDimensions.height);
			fileDialogAddButton.setPreferredSize(preferredSize);
			urlDialogAddButton.setPreferredSize(preferredSize);
			fileRemoveButton.setPreferredSize(preferredSize);
		}
		JScrollPane scrollPane = new JScrollPane(inputSourcesJList);
		inputSourcesJList.setPreferredSize(new Dimension(350, 60));
		
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
					Object old = sources.put(selected, file);
					// only update the model, if the file was not there before
					if (old == null) {
						updateInputSourceData(inputSourcesJList, sources);
					}
				}
			}
		});
		
		// add listener for adding a URL to the list model
		urlDialogAddButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				String str = JOptionPane.showInputDialog(frame, "Enter URL: ", 
						"URL Input for OORT", JOptionPane.PLAIN_MESSAGE);
				if (str != null) {
					str = str.trim();
					if (str.length() > 1) {
						try {
							URL url = new URL(str);
							str = url.toString();
							Object old = sources.put(str, url);
							// only update the model, if the file was not there before
							if (old == null) {
								updateInputSourceData(inputSourcesJList, sources);
							}
						} catch (MalformedURLException exception) {
							LOGGER.warn("Could not parse string as URL: "+str, exception);
						}
					}
				}
			}
		});
		
		// add listener for removing sources from the list model
		fileRemoveButton.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent e) {
				Object[] values = inputSourcesJList.getSelectedValues();
				if (values != null && values.length > 0) {
					for (Object object : values) {
						sources.remove(object);
					}
					updateInputSourceData(inputSourcesJList, sources);
				}
			}
		});
		
		add(new JLabel("Input"), pos);
		addRowGap(this, pos, 10);
		
		add(new JLabel("Ontology Files"), pos.nextRow());
		add(scrollPane, pos.nextCol().indentLeft(10).expandW().expandH().anchorCenter().height(4).fill());
		add(fileDialogAddButton, pos.nextCol().indentLeft(10));
		add(urlDialogAddButton, pos.nextRow().nextCol().nextCol().indentLeft(10));
		add(fileRemoveButton, pos.nextRow().nextCol().nextCol().indentLeft(10));
		pos.nextRow();
	}
	
	/**
	 * Create layout and listener for the output field.
	 * 
	 * @param pos
	 * @param defaultFolder 
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
	 * @param sourceList
	 * @param sources 
	 */
	private void updateInputSourceData(JList sourceList, Map<String, Object> sources) {
		DefaultListModel listModel = new DefaultListModel();
		for (String name : sources.keySet()) {
			listModel.addElement(name);
		}
		sourceList.setModel(listModel);
		
		// enable the mireot option, when there is more than one file. 
		advancedPanel.setMireotButtonsEnabled(sources.size() > 1);
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
