package owltools.ontologyrelease.gui;

import static org.obolibrary.gui.GuiTools.addRowGap;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;

import org.obolibrary.gui.GuiTools.GBHelper;
import org.obolibrary.gui.GuiTools.SizedJPanel;

import owltools.InferenceBuilder;
import owltools.ontologyrelease.OortConfiguration;

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
	
	final JCheckBox allowOverwrite;
	
	final JRadioButton pelletRadioButton;
	final JRadioButton hermitRadioButton;
	final JRadioButton factppRadioButton;
	final JRadioButton jcelRadioButton;
	final JRadioButton elkRadioButton;

	private List<JComponent> mireotCheckboxes;
	private boolean defaultRecreateMireot;
	
	/**
	 * Create the panel with the given default values.
	 * 
	 * @param oortConfiguration 
	 */
	public OortGuiAdvancedPanel(OortConfiguration oortConfiguration) {
		super();
		
		this.panel = new JPanel();
		this.defaultRecreateMireot = oortConfiguration.isRecreateMireot();
		
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
		
		// reasoner radio buttons
		pelletRadioButton = new JRadioButton();
		hermitRadioButton = new JRadioButton();
		factppRadioButton = new JRadioButton();
		jcelRadioButton = new JRadioButton();
		elkRadioButton = new JRadioButton();
		
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
		
		createFancyCheckBox(pos, "Expand Xref Macros (advanced)", null, expandXrefsCheckBox);

		addRowGap(panel, pos.nextRow(), 5);
		
		createFancyCheckBox(pos, "Expand Shortcut Relations (advanced)", null, expandShortcutRelations);

		addRowGap(panel, pos.nextRow(), 5);
		
		mireotCheckboxes = createFancyCheckBox(pos, "Recreate Mireot (advanced)", null, recreateMireot);
		addRowGap(panel, pos.nextRow(), 5);
		mireotCheckboxes.addAll(createFancyCheckBox(pos, "Check and Repair Annotation Cardinalities", "<html><p>This setting is only used during Mireot. After merging the ontologies, this allows to detect violations for ontology tags (e.g., duplicate definition tags).</p></html>", repairAnnotationCardinality));
		setMireotButtonsEnabled(false);

		addRowGap(panel, pos.nextRow(), 5);
		
		createFancyCheckBox(pos, "Allow overwriting of existing release files", null, allowOverwrite);
		
		addRowGap(panel, pos.nextRow(), 5);
		
		createFancyCheckBox(pos, "Create OWL-EL profile ontology", null, writeELOntologyCheckBox);
		
		addRowGap(panel, pos.nextRow(), 5);
		
		createFancyCheckBox(pos, "Justify Asserted Sub Classes", null, justifyAssertedSubclasses);
		
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

	/**
	 * Create layout for reasoner selection
	 * 
	 * @param pos
	 */
	private void createReasonerPanel(GBHelper pos) {
		panel.add(new JLabel("Reasoner"), pos.nextRow().indentLeft(DEFAULT_INDENT));
		
		ButtonGroup reasonerGroup = new ButtonGroup();
		reasonerGroup.add(pelletRadioButton);
		reasonerGroup.add(hermitRadioButton);
		reasonerGroup.add(factppRadioButton);
		reasonerGroup.add(jcelRadioButton);
		reasonerGroup.add(elkRadioButton);
		
		JPanel panel = new JPanel(new GridLayout(3, 2, 20, 2));
		panel.add(hermitRadioButton);
		panel.add(pelletRadioButton);
		panel.add(jcelRadioButton);
		panel.add(elkRadioButton);
		panel.add(factppRadioButton);
		
		this.panel.add(panel, pos.nextRow().nextCol().width(2));
		
		hermitRadioButton.setText("HermiT");
		pelletRadioButton.setText("Pellet");
		jcelRadioButton.setText("JCEL");
		elkRadioButton.setText("ELK");
		factppRadioButton.setText("Fact++");
		
		addRowGap(this.panel, pos.nextRow(), 5);
		this.panel.add(new JLabel("(Both Hermit and Pellet should give the same results, Hermit is typically faster)"), 
				pos.nextRow().indentLeft(DEFAULT_INDENT).width(3).fill().expandW());
		
		hermitRadioButton.setSelected(true);
		factppRadioButton.setEnabled(false);
	}

	public void setAllowOverwrite(boolean allowOverwrite) {
		this.allowOverwrite.setSelected(allowOverwrite);
	}
	
	/**
	 * Enable or disable the Mireot-options check box components.
	 * 
	 * @param enabled
	 */
	void setMireotButtonsEnabled(boolean enabled) {
		boolean wasEnabled = recreateMireot.isEnabled();
		for (JComponent jComponent : mireotCheckboxes) {
			jComponent.setEnabled(enabled);
		}
		if (enabled) {
			// if enabled set the default value
			// but only if it wasn't enabled before 
			if (!wasEnabled) {
				recreateMireot.setSelected(defaultRecreateMireot);
			}
		}
		else {
			// If disabled remove, set current value to false
			recreateMireot.setSelected(false);
		}
	}

	void applyConfig(OortConfiguration configuration) {
		
		// options flags
		assertedCheckBox.setSelected(configuration.isAsserted());
		simpleCheckBox.setSelected(configuration.isSimple());
		expandXrefsCheckBox.setSelected(configuration.isExpandXrefs());
		allowOverwrite.setSelected(configuration.isAllowFileOverWrite());
		boolean mireot = configuration.isRecreateMireot();
		recreateMireot.setSelected(mireot);
		defaultRecreateMireot = mireot;
		if (mireot) {
			 
		}
		expandShortcutRelations.setSelected(configuration.isExpandShortcutRelations());
		writeELOntologyCheckBox.setSelected(configuration.isWriteELOntology());
		justifyAssertedSubclasses.setSelected(configuration.isJustifyAssertedSubclasses());
				
		String reasoner = configuration.getReasonerName();
		if (InferenceBuilder.REASONER_PELLET.equals(reasoner)) {
			pelletRadioButton.setSelected(true);
		}
		else if (InferenceBuilder.REASONER_JCEL.equals(reasoner)) {
			jcelRadioButton.setSelected(true);
		}
		else if (InferenceBuilder.REASONER_ELK.equals(reasoner)) {
			elkRadioButton.setSelected(true);
		}
		else {
			hermitRadioButton.setSelected(true);
		}				
	}
}
