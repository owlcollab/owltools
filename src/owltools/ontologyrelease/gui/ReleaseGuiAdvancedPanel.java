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
	 */
	public ReleaseGuiAdvancedPanel(String defaultReasoner, boolean defaultIsAsserted, boolean defaultIsSimple) {
		super();
		
		// options flags
		assertedCheckBox = new JCheckBox();
		simpleCheckBox = new JCheckBox();
		
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
				"<p>Creates from file 'X' the additional files named 'X-non-classified'</p></html>",
				defaultIsAsserted, 
				"simple", 
				"Enter Description for simple here, e.g., which new files are generated with which content",
				defaultIsSimple);
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
			String simpleLabel, String simpleDesc, boolean defaultIsSimple) {
		add(new JLabel("Options"), pos.nextRow().indentLeft(DEFAULT_INDENT));
		
		createFancyCheckBox(pos, assertedLabel, assertedDesc, defaultIsAsserted, assertedCheckBox);
		
		addRowGap(this, pos.nextRow(), 5);
		
		createFancyCheckBox(pos, simpleLabel, simpleDesc, defaultIsSimple, simpleCheckBox);
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
		
		add(factppRadioButton, pos.nextRow().nextCol());
		add(new JLabel("Fact++"), pos.nextCol());
		
		add(jcelRadioButton, pos.nextRow().nextCol());
		add(new JLabel("JCEL"), pos.nextCol());
		
		addRowGap(this, pos.nextRow(), 10);
		add(new JLabel("Do you want a long description for the reasoner and what is does?"), 
				pos.nextRow().indentLeft(DEFAULT_INDENT).width(3).fill().expandW());
		
		if (InferenceBuilder.REASONER_PELLET.equals(defaultReasoner)) {
			pelletRadioButton.setSelected(true);
		}
		else {
			hermitRadioButton.setSelected(true);
		}
		factppRadioButton.setEnabled(false);
		jcelRadioButton.setEnabled(false);
	}
	
}
