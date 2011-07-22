package owltools.ontologyrelease.gui;

import static org.obolibrary.gui.GuiTools.addRowGap;

import java.awt.GridBagLayout;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JRadioButton;

import org.obolibrary.gui.GuiTools.GBHelper;
import org.obolibrary.gui.GuiTools.SizedJPanel;

import owltools.InferenceBuilder;

/**
 * Panel containing advanced options for the release manager.
 */
public class ReleaseGuiAdvancedPanel extends SizedJPanel {

	// generated
	private static final long serialVersionUID = 6890664004411145411L;
	
	private static int DEFAULT_INDENT = 20;

	final JCheckBox assertedCheckBox;
	final JCheckBox simpleCheckBox;
	final JCheckBox expandXrefsCheckBox;
	
	final JCheckBox allowOverwrite;
	
	final JRadioButton pelletRadioButton;
	final JRadioButton hermitRadioButton;
	final JRadioButton factppRadioButton;
	final JRadioButton jcelRadioButton;
	
	/**
	 * Create the panel with the given default values.
	 * 
	 * @param defaultReasoner
	 * @param defaultIsAsserted
	 * @param defaultIsSimple
	 * @param defaultExpandXrefs 
	 */
	public ReleaseGuiAdvancedPanel(String defaultReasoner, boolean defaultIsAsserted, boolean defaultIsSimple, boolean defaultExpandXrefs, boolean defaultAllowOverwrite) {
		super();
		
		// options flags
		assertedCheckBox = new JCheckBox();
		simpleCheckBox = new JCheckBox();
		expandXrefsCheckBox = new JCheckBox();
		allowOverwrite = new JCheckBox();
		
		// reasoner radio buttons
		pelletRadioButton = new JRadioButton();
		hermitRadioButton = new JRadioButton();
		factppRadioButton = new JRadioButton();
		jcelRadioButton = new JRadioButton();
		
		// Layout
		setLayout(new GridBagLayout());
		GBHelper pos = new GBHelper();
		addRowGap(this, pos, 20);
		
		// options
		createOptionPanel(pos, 
				"Make original", 
				"<html><p>Also make original ontology in obo and owl.</p>" +
				"<p>In addition to creating the main ontology file <X>, will make a file '<X>-non-classified'</p>" +
				"<p>This is the version does <i>not</i> have the reasoner-inferred links asserted</p></html>",
				defaultIsAsserted, 
				"simple", 
				"<html><p>In addition the generating the main ontology, this will make a version</p>" +
				"<p> with all external classes and references to them removed</p></html>",
				defaultIsSimple,
				defaultExpandXrefs,
				defaultAllowOverwrite);
		addRowGap(this, pos, 20);
		
		
		
		// reasoner
		createReasonerPanel(pos, defaultReasoner);
		addRowGap(this, pos.expandH(), 10);
		
	}
	
	/**
	 * Create layout for the option flags
	 * 
	 * @param pos
	 * @param assertedLabel
	 * @param assertedDesc
	 * @param defaultIsAsserted
	 * @param simpleLabel
	 * @param simpleDesc
	 * @param defaultIsSimple
	 */
	private void createOptionPanel(GBHelper pos, 
			String assertedLabel, String assertedDesc, boolean defaultIsAsserted, 
			String simpleLabel, String simpleDesc, boolean defaultIsSimple, boolean defaultExpandXrefs,
			boolean defaultAllowOverwrite) {
		add(new JLabel("Options"), pos.nextRow().indentLeft(DEFAULT_INDENT));
		
		createFancyCheckBox(pos, assertedLabel, assertedDesc, defaultIsAsserted, assertedCheckBox);
		
		addRowGap(this, pos.nextRow(), 5);
		
		createFancyCheckBox(pos, simpleLabel, simpleDesc, defaultIsSimple, simpleCheckBox);
		
		addRowGap(this, pos.nextRow(), 15);
		
		createFancyCheckBox(pos, "Expand Xref Macros (advanced)", null, defaultExpandXrefs, expandXrefsCheckBox);

		addRowGap(this, pos.nextRow(), 15);
		
		createFancyCheckBox(pos, "Allow overwriting of existing release files", null, defaultAllowOverwrite, allowOverwrite);
		
		addRowGap(this, pos.nextRow(), 5);
	}
	
	/**
	 * Create a more complex check box with a label and description. 
	 * Use the existing {@link GridBagLayout} to align the components.
	 * The description is optional.
	 * 
	 * @param pos
	 * @param label
	 * @param desc
	 * @param defaultValue
	 * @param checkBox
	 */
	private void createFancyCheckBox(GBHelper pos, String label, String desc, boolean defaultValue, JCheckBox checkBox) {
		add(checkBox, pos.nextRow().nextCol());
		add(new JLabel(label), pos.nextCol().expandW());
		if (desc != null) {
			addRowGap(this, pos.nextRow(), 2);
			add(new JLabel(desc), pos.nextCol().nextCol().expandW().fill());
		}
		checkBox.setSelected(defaultValue);
	}

	/**
	 * Create layout for reasoner selection
	 * 
	 * @param pos
	 * @param defaultReasoner
	 */
	private void createReasonerPanel(GBHelper pos, String defaultReasoner) {
		add(new JLabel("Reasoner"), pos.nextRow().indentLeft(DEFAULT_INDENT));
		
		ButtonGroup reasonerGroup = new ButtonGroup();
		reasonerGroup.add(pelletRadioButton);
		reasonerGroup.add(hermitRadioButton);
		reasonerGroup.add(factppRadioButton);
		reasonerGroup.add(jcelRadioButton);
		
		add(hermitRadioButton, pos.nextRow().nextCol());
		add(new JLabel("HermiT"), pos.nextCol());
		
		add(pelletRadioButton, pos.nextRow().nextCol());
		add(new JLabel("Pellet"), pos.nextCol());
		
		add(jcelRadioButton, pos.nextRow().nextCol());
		add(new JLabel("JCEL"), pos.nextCol());

		add(factppRadioButton, pos.nextRow().nextCol());
		add(new JLabel("Fact++"), pos.nextCol());
		
		addRowGap(this, pos.nextRow(), 10);
		add(new JLabel("(Both Hermit and Pellet should give the same results, Hermit is typically faster)"), 
				pos.nextRow().indentLeft(DEFAULT_INDENT).width(3).fill().expandW());
		
		// set default, if nothing matches use Hermit
		if (InferenceBuilder.REASONER_PELLET.equals(defaultReasoner)) {
			pelletRadioButton.setSelected(true);
		}
		else if (InferenceBuilder.REASONER_JCEL.equals(defaultReasoner)) {
			jcelRadioButton.setSelected(true);
		}
		else {
			hermitRadioButton.setSelected(true);
		}
		factppRadioButton.setEnabled(false);
	}

	public void setAllowOverwrite(boolean allowOverwrite) {
		this.allowOverwrite.setSelected(allowOverwrite);
	}
}
