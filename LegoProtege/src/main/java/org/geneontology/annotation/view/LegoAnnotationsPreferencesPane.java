package org.geneontology.annotation.view;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;

import org.protege.editor.core.ui.util.UIUtil;
import org.protege.editor.owl.ui.preferences.OWLPreferencesPanel;

/**
 * Configuration panel for the LEGO annotation viewer.
 */
public class LegoAnnotationsPreferencesPane extends OWLPreferencesPanel {

	// generated
	private static final long serialVersionUID = 9039692749683957563L;
	
	private JTextField pathField;
	
	@Override
	public void initialise() throws Exception {
		 setLayout(new BorderLayout(12, 12));
         setBorder(BorderFactory.createTitledBorder("Dot Application Path"));
         add(createUI(), BorderLayout.NORTH);
	}

	protected JComponent createUI() {
        Box panel = new Box(BoxLayout.LINE_AXIS);

        pathField = new JTextField(15);
        pathField.setText(LegoAnnotationsPreferences.getInstance().getDotPath());

        JButton browseButton = new JButton("Browse");
        browseButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				 browseForPath();
			}
		});

        panel.add(new JLabel("Path:"));
        panel.add(pathField);
        panel.add(browseButton);

        return panel;
    }
	
	protected void browseForPath() {
        Set<String> exts = new HashSet<String>();
        exts.add("dot");
        exts.add("app");
        exts.add("exe");
        exts.add("bin");
        File file = UIUtil.openFile(new JFrame(), "Dot Application", "Please select the dot application", exts);
        if(file != null) {
            pathField.setText(file.getPath());
        }
    }


	@Override
	public void dispose() throws Exception {
		// do nothing
	}

	@Override
	public void applyChanges() {
		LegoAnnotationsPreferences.getInstance().setDotPath(pathField.getText());
	}

}
