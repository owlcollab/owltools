package owltools.ontologyrelease.gui;

import static org.obolibrary.gui.GuiTools.*;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.log4j.Logger;
import org.obolibrary.gui.SelectDialog;
import org.obolibrary.gui.GuiTools.GBHelper;
import org.obolibrary.gui.GuiTools.SizedJPanel;

import owltools.ontologyrelease.OortConfiguration;

public class OortGuiQueryOntologyPanel extends SizedJPanel {

	// Generated
	private static final long serialVersionUID = -1161204664170884076L;
	
	private final static Logger LOGGER = Logger.getLogger(OortGuiQueryOntologyPanel.class); 
	
	private static int DEFAULT_INDENT = 20;
	
	private final Frame frame;
	private final JPanel panel;
	
	final JCheckBox activateCheckBox = new JCheckBox();
	final JCheckBox removeQueryTermCheckBox = new JCheckBox();
	
	final JTextField queryOntologyField = new JTextField();
	final JTextField queryTargetField = new JTextField();
	
	final JRadioButton queryTargetIriRadioButton = new JRadioButton();
	final JRadioButton queryTargetLabelRadioButton = new JRadioButton();
	
	public OortGuiQueryOntologyPanel(Frame frame, OortConfiguration oortConfiguration) {
		super();
		this.panel = new JPanel();
		this.frame = frame;
		
		this.setLayout(new BorderLayout(1, 1));
		JScrollPane scrollPane = new JScrollPane(panel);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane.setWheelScrollingEnabled(true);
		this.add(scrollPane, BorderLayout.CENTER);
		
		// Layout
		panel.setLayout(new GridBagLayout());
		GBHelper pos = new GBHelper();
		
		final List<JComponent> subComponents = new ArrayList<JComponent>();
		
		// add checkbox which activates the following options
		createMainCheckBox(pos, subComponents);
		addRowGap(panel, pos.nextRow(), 10);
		
		// add query ontology (file or url)
		createFileSelector(pos, subComponents);
		
		addRowGap(panel, pos.nextRow(), 10);
		// get IRI or Label
		createQueryTargetSelector(pos, subComponents);
		
		addRowGap(panel, pos.nextRow(), 10);
		// add reference to IO panel
		createMessageForMainPanel(pos, subComponents);
		
		addRowGap(panel, pos.nextRow().expandH(), 10);
		
		applyConfig(oortConfiguration, subComponents);
	}

	void createMainCheckBox(GBHelper pos, final List<JComponent> subComponents) {
		activateCheckBox.setText("Create Ontology from Query");
		GBHelper p = pos.nextRow().indentLeft(DEFAULT_INDENT).width(3);
		p.insets.top += 15;
		panel.add(activateCheckBox, p);
		activateCheckBox.addChangeListener(new ChangeListener() {
			
			@Override
			public void stateChanged(ChangeEvent e) {
				updateGui(subComponents);
			}
		});
	}

	void createFileSelector(GBHelper pos, final List<JComponent> subComponents) {
		subComponents.add(queryOntologyField);
		
		JLabel desc = new JLabel("1. Query Ontology");
		subComponents.add(desc);
		panel.add(desc, pos.nextRow().expandW().indentLeft(DEFAULT_INDENT*2).width(3).indentRight(DEFAULT_INDENT));
		addRowGap(panel, pos.nextRow(), 5);
		
		GBHelper fieldPos = pos.nextRow().expandW().indentLeft(DEFAULT_INDENT*3).width(3).indentRight(DEFAULT_INDENT).fill();
		panel.add(queryOntologyField, fieldPos);
		addRowGap(panel, pos.nextRow(), 5);
		final JButton selectFileButton = new JButton("Select File");
		subComponents.add(selectFileButton);
		panel.add(selectFileButton, pos.nextRow().indentLeft(DEFAULT_INDENT*3));
		
		// use file chooser dialog for select input files
		final SelectDialog fileDialog = SelectDialog.getFileSelector(frame, 
				SelectDialog.LOAD,
				".",
				"Query ontology file choose dialog", 
				"OBO & OWL files", 
				new String[]{"obo","owl"});
				
		// add listener for adding a file to the list model
		selectFileButton.addActionListener(new ActionListener() {
					
			public void actionPerformed(ActionEvent e) {
				fileDialog.show();
				String selected = fileDialog.getSelectedCanonicalPath();
				if (selected != null) {
					queryOntologyField.setText(selected);
				}
			}
		});
				
		
		final JButton selectUrlButton = new JButton("Select URL");
		subComponents.add(selectUrlButton);
		panel.add(selectUrlButton, pos.nextCol().indentLeft(5));
		
		// add listener for adding a URL to the list model
		selectUrlButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				String str = JOptionPane.showInputDialog(frame, "Enter URL: ", 
						"URL for query ontology", JOptionPane.PLAIN_MESSAGE);
				if (str != null) {
					str = str.trim();
					if (str.length() > 1) {
						try {
							URL url = new URL(str);
							str = url.toString();
							queryOntologyField.setText(str);
						} catch (MalformedURLException exception) {
							LOGGER.warn("Could not parse string as URL: "+str, exception);
						}
					}
				}
			}
		});
	}

	void createQueryTargetSelector(GBHelper pos, List<JComponent> subComponents) {
		subComponents.add(queryTargetField);
		subComponents.add(queryTargetIriRadioButton);
		subComponents.add(queryTargetLabelRadioButton);
		subComponents.add(removeQueryTermCheckBox);
		
		JLabel desc = new JLabel("2. Named query as found in the query ontology");
		subComponents.add(desc);
		panel.add(desc, pos.nextRow().expandW().indentLeft(DEFAULT_INDENT*2).width(3).indentRight(DEFAULT_INDENT));
		addRowGap(panel, pos.nextRow(), 5);

		final ButtonGroup group = new ButtonGroup();
		group.add(queryTargetIriRadioButton);
		group.add(queryTargetLabelRadioButton);
		queryTargetIriRadioButton.setText("Identified by IRI");
		queryTargetLabelRadioButton.setText("Identified by label");
		
		panel.add(queryTargetIriRadioButton, pos.nextRow().indentLeft(DEFAULT_INDENT*3).width(2));
		panel.add(queryTargetLabelRadioButton, pos.nextRow().indentLeft(DEFAULT_INDENT*3).width(2));
		addRowGap(panel, pos.nextRow(), 5);
		
		GBHelper fieldPos = pos.nextRow().expandW().indentLeft(DEFAULT_INDENT*3).width(3).indentRight(DEFAULT_INDENT).fill();
		panel.add(queryTargetField, fieldPos);
		
		addRowGap(panel, pos.nextRow(), 5);
		removeQueryTermCheckBox.setText("Remove named query from ontology");
		panel.add(removeQueryTermCheckBox, pos.nextRow().indentLeft(DEFAULT_INDENT*3).width(2));
		
	}
	
	void createMessageForMainPanel(GBHelper pos, List<JComponent> subComponents) {
		JLabel heading = new JLabel("3. Add source ontologies and choose output folder");
		subComponents.add(heading);
		panel.add(heading, pos.nextRow().expandW().indentLeft(DEFAULT_INDENT*2).width(3).indentRight(DEFAULT_INDENT));
		addRowGap(panel, pos.nextRow(), 5);
		JLabel message = new JLabel("Set sources and output in the INPUT/OUTPUT panel.");
		subComponents.add(message);
		panel.add(message, pos.nextRow().expandW().indentLeft(DEFAULT_INDENT*3).width(3).indentRight(DEFAULT_INDENT));
	}

	void applyConfig(OortConfiguration configuration, final List<JComponent> subComponents) {
		activateCheckBox.setSelected(configuration.isUseQueryOntology());
		queryOntologyField.setText(configuration.getQueryOntology());
		queryTargetField.setText(configuration.getQueryOntologyReference());
		if (configuration.isQueryOntologyReferenceIsIRI()) {
			queryTargetIriRadioButton.setSelected(true);
		}
		else {
			queryTargetLabelRadioButton.setSelected(true);
		}
		removeQueryTermCheckBox.setSelected(configuration.isRemoveQueryOntologyReference());
		updateGui(subComponents);
	}

	private void updateGui(final List<JComponent> subComponents) {
		for(JComponent component : subComponents) {
			component.setEnabled(activateCheckBox.isSelected());
		}
	}
}
