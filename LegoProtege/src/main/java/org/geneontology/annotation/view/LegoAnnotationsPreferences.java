package org.geneontology.annotation.view;

import java.io.File;

import org.protege.editor.core.prefs.Preferences;
import org.protege.editor.core.prefs.PreferencesManager;

/**
 * Load and store preferences for the LEGO annotation view.
 */
public class LegoAnnotationsPreferences {

	private static final LegoAnnotationsPreferences INSTANCE = new LegoAnnotationsPreferences();

	public static final String PREFERENCES_SET_KEY = "org.geneontology.annotation.view";
	public static final String PREFERENCES_KEY = "LEGO";
	public static final String DOT_PATH_KEY = "dotPath";

	private String dotPath = "";

	private LegoAnnotationsPreferences() {
		super();
		loadFromPrefs();
	}

	public static LegoAnnotationsPreferences getInstance() {
		return INSTANCE;
	}

	public String getDotPath() {
		return dotPath;
	}

	public void setDotPath(String path) {
		if(System.getProperty("os.name").indexOf("OS X") != -1) {
			// On Mac
			if(path.endsWith(".app") && (new File(path)).isDirectory()) {
				// Graphviz - go inside bundle
				path += "/Contents/MacOS/dot";
			}
		}
		this.dotPath = path;
		savePrefs();
	}

	private static Preferences getPreferences() {
		return PreferencesManager.getInstance().getPreferencesForSet(PREFERENCES_SET_KEY, PREFERENCES_KEY);
	}

	private void loadFromPrefs() {
		dotPath = getPreferences().getString(DOT_PATH_KEY, "");
	}

	private void savePrefs() {
		getPreferences().putString(DOT_PATH_KEY, dotPath);
	}
}
