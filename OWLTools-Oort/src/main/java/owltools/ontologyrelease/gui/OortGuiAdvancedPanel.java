package owltools.ontologyrelease.gui;

import static org.obolibrary.gui.GuiTools.*;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import org.obolibrary.gui.GuiTools.GBHelper;
import org.obolibrary.gui.GuiTools.SizedJPanel;
import org.obolibrary.gui.SelectDialog;

import owltools.InferenceBuilder;
import owltools.ontologyrelease.OortConfiguration;
import owltools.ontologyverification.OntologyCheck;

/**
 * Panel containing advanced options for the release manager.
 */
public class OortGuiAdvancedPanel extends SizedJPanel {

	// generated
	private static final long serialVersionUID = 6890664004411145411L;
	
	private static int DEFAULT_INDENT = 20;
	
	private final JPanel panel;

	final JCheckBox assertedCheckBox;
	final JCheckBox simpleCheckBox;
	final JCheckBox expandXrefsCheckBox;
	final JCheckBox recreateMireot;
	final JCheckBox repairAnnotationCardinality;
	final JCheckBox expandShortcutRelations;
	final JCheckBox writeELOntologyCheckBox;
	final JCheckBox justifyAssertedSubclasses;
	final JCheckBox writeSubSets;
	final JCheckBox gafToOwl;
	final JCheckBox writeOWL;
	final JCheckBox writeOWX;
	final JCheckBox writeOBO;
	
	final JCheckBox versionReportFiles;
	
	final JCheckBox addSupportFromImports;
	
	final JCheckBox allowOverwrite;
	
	final JTextField catalogXMLField;
	
	final JRadioButton hermitRadioButton;
	final JRadioButton factppRadioButton;
	final JRadioButton jcelRadioButton;
	final JRadioButton elkRadioButton;
	final JRadioButton moreRadioButton;
	
	final Map<OntologyCheck, JCheckBox> ontologyCheckBoxes;

	private final Frame frame;
	
	/**
	 * Create the panel with the given default values.
	 * 
	 * @param frame 
	 * @param oortConfiguration 
	 */
	public OortGuiAdvancedPanel(Frame frame, OortConfiguration oortConfiguration) {
		super();
		this.frame = frame;
		
		this.panel = new JPanel();
		
		this.setLayout(new BorderLayout(1, 1));
		JScrollPane scrollPane = new JScrollPane(panel);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane.setWheelScrollingEnabled(true);
		this.add(scrollPane, BorderLayout.CENTER);
		
		// options flags
		assertedCheckBox = new JCheckBox();
		simpleCheckBox = new JCheckBox();
		expandXrefsCheckBox = new JCheckBox();
		allowOverwrite = new JCheckBox();
		recreateMireot = new JCheckBox();
		repairAnnotationCardinality = new JCheckBox();
		expandShortcutRelations = new JCheckBox();
		writeELOntologyCheckBox = new JCheckBox();
		justifyAssertedSubclasses = new JCheckBox();
		writeSubSets = new JCheckBox();
		gafToOwl = new JCheckBox();
		addSupportFromImports = new JCheckBox();
		
		writeOBO = new JCheckBox();
		writeOWL = new JCheckBox();
		writeOWX = new JCheckBox();
		
		versionReportFiles = new JCheckBox();
		
		catalogXMLField = new JTextField();
		
		// reasoner radio buttons
		hermitRadioButton = new JRadioButton();
		factppRadioButton = new JRadioButton();
		jcelRadioButton = new JRadioButton();
		elkRadioButton = new JRadioButton();
		moreRadioButton = new JRadioButton();
		
		// ontolgy checks
		ontologyCheckBoxes = new HashMap<OntologyCheck, JCheckBox>();
		for (OntologyCheck check : OortConfiguration.getAvailableChecks()) {
			ontologyCheckBoxes.put(check, new JCheckBox(check.getLabel()));
		}
		
		// Layout
		panel.setLayout(new GridBagLayout());
		GBHelper pos = new GBHelper();
		
		// options
		createOptionPanel(pos, 
				"Make original", 
				"<html><p>Also make original ontology in obo and owl.</p>" +
				"<p>In addition to creating the main ontology file <X>, will make a file '<X>-non-classified'</p>" +
				"<p>This is the version does <i>not</i> have the reasoner-inferred links asserted</p></html>",
				"simple", 
				"<html><p>In addition the generating the main ontology, this will make a version</p>" +
				"<p> with all external classes and references to them removed</p></html>");
		addRowGap(panel, pos, 10);
		
		// catalog xml
		createCatalogXMLPanel(pos);
		
		// write format options
		createWriteFormatPanel(pos);
		
		// reasoner
		createReasonerPanel(pos);
		addRowGap(panel, pos, 5);
		
		// set values
		applyConfig(oortConfiguration);
		
	}
	
	/**
	 * Create layout for the option flags
	 * 
	 * @param pos
	 * @param assertedLabel
	 * @param assertedDesc
	 * @param simpleLabel
	 * @param simpleDesc
	 */
	private void createOptionPanel(GBHelper pos, 
			String assertedLabel, String assertedDesc, 
			String simpleLabel, String simpleDesc)
	{
		panel.add(new JLabel("Options"), pos.nextRow().indentLeft(DEFAULT_INDENT));
		
		createFancyCheckBox(pos, assertedLabel, assertedDesc, assertedCheckBox);
		
		addRowGap(panel, pos.nextRow(), 5);
		
		createFancyCheckBox(pos, simpleLabel, simpleDesc, simpleCheckBox);
		
		addRowGap(panel, pos.nextRow(), 10);
		
		createFancyCheckBox(pos, "Write SubSets", null, writeSubSets);
		
		addRowGap(panel, pos.nextRow(), 10);
		
		createFancyCheckBox(pos, "Check for GAF files", null, gafToOwl);
		
		addRowGap(panel, pos.nextRow(), 10);
		
		createFancyCheckBox(pos, "Expand Xref Macros (advanced)", null, expandXrefsCheckBox);

		addRowGap(panel, pos.nextRow(), 5);
		
		createFancyCheckBox(pos, "Expand Shortcut Relations (advanced)", null, expandShortcutRelations);

		addRowGap(panel, pos.nextRow(), 5);
		
		createFancyCheckBox(pos, "Remove Imports (advanced)", 
				"<html><p>This option will remove the import statments form the ontology (in all formats).</p>" +
				"<p>To avoid dangling links, use the 'Recreate Mireot' option to include the relevant <br>" +
				"terms in the ontology.</p></html>", addSupportFromImports);
		
		addRowGap(panel, pos.nextRow(), 5);
		
		createFancyCheckBox(pos, "Recreate Mireot (advanced)", null, recreateMireot);
		addRowGap(panel, pos.nextRow(), 5);
		
		createFancyCheckBox(pos, "Check and Repair Annotation Cardinalities", 
				"<html><p>This setting is only used during Mireot.</p>" +
				"<p>After merging the ontologies, the ontology terms are <br>" +
				"checked for violations (e.g., duplicate definition tags).</p></html>", repairAnnotationCardinality);

		addRowGap(panel, pos.nextRow(), 5);
		
		createFancyCheckBox(pos, "Allow overwriting of existing release files", null, allowOverwrite);
		
		addRowGap(panel, pos.nextRow(), 5);
		
		createFancyCheckBox(pos, "Create OWL-EL profile ontology", null, writeELOntologyCheckBox);
		
		addRowGap(panel, pos.nextRow(), 5);
		
		createFancyCheckBox(pos, "Justify Asserted Sub Classes", null, justifyAssertedSubclasses);
		
		// versionReportFiles
		addRowGap(panel, pos.nextRow(), 5);
		
		createFancyCheckBox(pos, "Version Report Files", "<html><p>This will create the report files in the staging directory.<ul><li>The advantage is that the report files are included in the release folder (i.e. to compare between different versions.).</li> <li>A disadvantage is that the report files are only visible in the after a successfull run.</li></ul></p></html>", versionReportFiles);
		
		
		addRowGap(panel, pos.nextRow(), 10);
		
		panel.add(new JLabel("Ontology Checks"), pos.nextRow().indentLeft(DEFAULT_INDENT).width(3));
		List<OntologyCheck> checks = new ArrayList<OntologyCheck>(ontologyCheckBoxes.keySet());
		Collections.sort(checks, new Comparator<OntologyCheck>() {

			@Override
			public int compare(OntologyCheck o1, OntologyCheck o2) {
				return o1.getLabel().compareTo(o2.getLabel());
			}
		});
		for(OntologyCheck check : checks) {
			JCheckBox box = ontologyCheckBoxes.get(check);
			addRowGap(panel, pos.nextRow(), 5);
			panel.add(box, pos.nextRow().nextCol().width(2).expandW());
		}
		
		addRowGap(panel, pos.nextRow(), 10);
	}
	
	/**
	 * Create a more complex check box with a label and description. 
	 * Use the existing {@link GridBagLayout} to align the components.
	 * The description is optional.
	 * 
	 * @param pos
	 * @param label
	 * @param desc
	 * @param checkBox
	 * @return List of {@link JComponent}
	 */
	private List<JComponent> createFancyCheckBox(GBHelper pos, String label, String desc, JCheckBox checkBox) {
		panel.add(checkBox, pos.nextRow().nextCol());
		List<JComponent> components = new ArrayList<JComponent>(3);
		components.add(checkBox);
		JLabel jLabel = new JLabel(label);
		components.add(jLabel);
		panel.add(jLabel, pos.nextCol().expandW());
		if (desc != null) {
			addRowGap(panel, pos.nextRow(), 2);
			JLabel descJLabel = new JLabel(desc);
			components.add(descJLabel);
			panel.add(descJLabel, pos.nextCol().nextCol().expandW().fill());
		}
		return components;
	}
	
	private void createCatalogXMLPanel(GBHelper pos) {
		panel.add(new JLabel("catalog.xml File"), pos.nextRow().indentLeft(DEFAULT_INDENT).width(3));
		addRowGap(this.panel, pos.nextRow(), 5);
		JPanel catalogXMLInputPanel = new JPanel(new BorderLayout(10, 0));
		catalogXMLInputPanel.add(catalogXMLField, BorderLayout.CENTER);
		final JButton selectFileButton = new JButton("Select File");
		catalogXMLInputPanel.add(selectFileButton, BorderLayout.LINE_END);
		
		final SelectDialog fileDialog = SelectDialog.getFileSelector(frame, 
				SelectDialog.LOAD,
				".",
				"Select catalog.xml dialog", 
				"XML files", 
				new String[]{"xml"});		
		
		selectFileButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				fileDialog.show();
				String selected = fileDialog.getSelectedCanonicalPath();
				if (selected != null) {
					catalogXMLField.setText(selected);
				}
			}
		});
		
		panel.add(catalogXMLInputPanel, pos.nextRow().nextCol().nextCol().width(2).expandW().indentRight(DEFAULT_INDENT).fill());
		addRowGap(this.panel, pos.nextRow(), 10);
	}

	private void createWriteFormatPanel(GBHelper pos) {
		panel.add(new JLabel("Write Formats"), pos.nextRow().indentLeft(DEFAULT_INDENT).width(3));
		addRowGap(this.panel, pos.nextRow(), 5);
		panel.add(writeOBO, pos.nextRow().nextCol());
		panel.add(new JLabel("OBO"), pos.nextCol().expandW());
		
		panel.add(writeOWL, pos.nextRow().nextCol());
		panel.add(new JLabel("OWL"), pos.nextCol().expandW());
		
		panel.add(writeOWX, pos.nextRow().nextCol());
		panel.add(new JLabel("OWX"), pos.nextCol().expandW());
		addRowGap(this.panel, pos.nextRow(), 10);
	}

	/**
	 * Create layout for reasoner selection
	 * 
	 * @param pos
	 */
	private void createReasonerPanel(GBHelper pos) {
		panel.add(new JLabel("Reasoner"), pos.nextRow().indentLeft(DEFAULT_INDENT).width(3));
		addRowGap(this.panel, pos.nextRow(), 5);
		
		ButtonGroup reasonerGroup = new ButtonGroup();
		reasonerGroup.add(hermitRadioButton);
		reasonerGroup.add(factppRadioButton);
		reasonerGroup.add(jcelRadioButton);
		reasonerGroup.add(elkRadioButton);
		reasonerGroup.add(moreRadioButton);
		
		JPanel panel = new JPanel(new GridLayout(3, 2, 20, 2));
		panel.add(hermitRadioButton);
		panel.add(jcelRadioButton);
		panel.add(elkRadioButton);
		panel.add(moreRadioButton);
		panel.add(factppRadioButton);
		
		
		this.panel.add(panel, pos.nextRow().nextCol().width(2));
		
		hermitRadioButton.setText("HermiT");
		jcelRadioButton.setText("JCEL");
		elkRadioButton.setText("ELK");
		moreRadioButton.setText("MORe");
		factppRadioButton.setText("Fact++");
		
		addRowGap(this.panel, pos.nextRow(), 10);
		hermitRadioButton.setSelected(true);
		factppRadioButton.setEnabled(false);
	}

	public void setAllowOverwrite(boolean allowOverwrite) {
		this.allowOverwrite.setSelected(allowOverwrite);
	}
	
	void applyConfig(OortConfiguration configuration) {
		
		// options flags
		assertedCheckBox.setSelected(configuration.isAsserted());
		simpleCheckBox.setSelected(configuration.isSimple());
		expandXrefsCheckBox.setSelected(configuration.isExpandXrefs());
		allowOverwrite.setSelected(configuration.isAllowFileOverWrite());
		recreateMireot.setSelected(configuration.isRecreateMireot());
		expandShortcutRelations.setSelected(configuration.isExpandShortcutRelations());
		writeELOntologyCheckBox.setSelected(configuration.isWriteELOntology());
		justifyAssertedSubclasses.setSelected(configuration.isJustifyAssertedSubclasses());
		writeSubSets.setSelected(configuration.isWriteSubsets());
		gafToOwl.setSelected(configuration.isGafToOwl());
		addSupportFromImports.setSelected(configuration.isAddSupportFromImports());
		
		Set<String> skipFormats = configuration.getSkipFormatSet();
		writeOBO.setSelected(!skipFormats.contains("obo"));
		writeOWL.setSelected(!skipFormats.contains("owl"));
		writeOWX.setSelected(!skipFormats.contains("owx"));
		
		versionReportFiles.setSelected(configuration.isVersionReportFiles());
		
		catalogXMLField.setText("");
		String catalogXML = configuration.getCatalogXML();
		if (catalogXML != null) {
			catalogXMLField.setText(catalogXML);
		}
		
		String reasoner = configuration.getReasonerName();
		if (InferenceBuilder.REASONER_JCEL.equals(reasoner)) {
			jcelRadioButton.setSelected(true);
		}
		else if (InferenceBuilder.REASONER_ELK.equals(reasoner)) {
			elkRadioButton.setSelected(true);
		}
		else if (InferenceBuilder.REASONER_MORE.equals(reasoner)) {
			moreRadioButton.setSelected(true);
		}
		else {
			hermitRadioButton.setSelected(true);
		}
		
		for(Entry<OntologyCheck, JCheckBox> entry : ontologyCheckBoxes.entrySet()) {
			entry.getValue().setSelected(false);
		}
		List<OntologyCheck> checks = configuration.getOntologyChecks();
		for (OntologyCheck check : checks) {
			JCheckBox checkBox = ontologyCheckBoxes.get(check);
			if (checkBox != null) {
				checkBox.setSelected(true);
			}
		}
		
	}
}
