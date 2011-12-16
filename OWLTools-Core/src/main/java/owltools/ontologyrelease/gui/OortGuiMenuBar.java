package owltools.ontologyrelease.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.prefs.Preferences;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.obolibrary.gui.SelectDialog;

import owltools.ontologyrelease.OortConfiguration;

/**
 * Menu bar for OORT. This menu currently has the options to 
 * load or store OORT configurations files, a history of recently used 
 * OORT configuration files, and to exit OORT.
 */
public class OortGuiMenuBar extends JMenuBar {

	private static final Logger LOGGER = Logger.getLogger(OortGuiMenuBar.class);

	// generated
	private static final long serialVersionUID = -8587366759371533588L;
	
	private final OortGuiMainFrame frame;
	private final OortConfiguration parameters;
	
	private final JMenuItem loadItem;
	private final JMenuItem storeItem;
	private final JMenuItem exitItem;
	private final JMenu fileMenu;
	
	private final FileHistory fileHistory;

	/**
	 * Create a new menu instance.
	 * 
	 * @param frame reference parent frame.
	 * @param parameters OORT configuration, will be used for loading and storing of the configuration.
	 */
	public OortGuiMenuBar(final OortGuiMainFrame frame, OortConfiguration parameters) {
		this.frame = frame;
		this.parameters = parameters;
		fileMenu = new JMenu("File");
		fileMenu.setMnemonic(KeyEvent.VK_F);
		this.add(fileMenu);

		loadItem = new JMenuItem("Load Configuration");
		storeItem = new JMenuItem("Store Configuration");
		exitItem = new JMenuItem("Exit");
		
		fileHistory = new FileHistory(frame.getClass());
		
		// load config file history
		fileHistory.loadHistory();
		
		// load config from file
		loadItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				String defaultFolder = FileUtils.getUserDirectoryPath();
				String title = "Select OORT configuation file";
				SelectDialog dialog = SelectDialog.getFileSelector(frame,
						SelectDialog.LOAD, defaultFolder, title, null, null);
				dialog.show();
				File selected = dialog.getSelected();
				if (selected != null) {
					boolean success = loadConfigFile(selected);
					if (success) {
						fileHistory.updateHistory(selected);
					}
				}
			}
		});

		// store configuration to file
		storeItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				frame.getParametersFromGUI(true);
				String defaultFolder = FileUtils.getUserDirectoryPath();
				String title = "Select OORT configuation file";
				SelectDialog dialog = SelectDialog.getFileSelector(frame,
						SelectDialog.SAVE, defaultFolder, title, null, null);
				dialog.show();
				File selected = dialog.getSelected();
				if (selected != null) {
					boolean success = writeConfigFile(selected);
					if (success) {
						fileHistory.updateHistory(selected);
					}
				}
			}
		});
		
		// exit item listener
		exitItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				frame.dispose();
				System.exit(0);
			}
		});
		
		
		// Menu Layout
		renderFileMenu(false);
	}

	/**
	 * Render the content of the file menu, including the 
	 * configuration file history.
	 * 
	 * @param clear if true, clear the current content from the file menu
	 */
	private void renderFileMenu(boolean clear) {
		if (clear) {
			fileMenu.removeAll();
		}
		fileMenu.add(loadItem);
		fileMenu.add(storeItem);
		if (!fileHistory.isEmpty()) {
			fileMenu.addSeparator();
			for(final Entry<String,File> entry :  fileHistory.entrySet()) {
				JMenuItem item = new JMenuItem(entry.getKey());
				item.addActionListener(new ActionListener() {
					
					@Override
					public void actionPerformed(ActionEvent e) {
						loadConfigFile(entry.getValue());
					}
				});
				fileMenu.add(item);
			}
		}
		fileMenu.addSeparator();
		fileMenu.add(exitItem);
	}
	
	/**
	 * Encapsulate the history for recently used configuration files using 
	 * an LRU cache and the {@link Preferences} API for persisting the 
	 * history.
	 *
	 */
	static class FileHistory {
		
		private static final String PREFERENCE_KEY_PREFIX_CONFIG_FILE_HISTORY = "Oort.Gui.Config.File.History.";
		private static final int MAX_HISTORY_ITEMS = 5;
		
		private final LRUCache<String, File> historyItems = new LRUCache<String, File>(MAX_HISTORY_ITEMS);
		private final Class<?> historyClass;
	
		/**
		 * Create a new instance, use the given class to resolve 
		 * the corresponding {@link Preferences} data.
		 * 
		 * @param historyClass
		 */
		FileHistory(Class<?> historyClass) {
			this.historyClass = historyClass;
		}
		
		/**
		 * @return entry set of the history.
		 */
		Set<Entry<String, File>> entrySet() {
			return historyItems.entrySet();
		}

		/**
		 * @return true, if the history is empty.
		 */
		boolean isEmpty() {
			return historyItems.isEmpty();
		}

		/**
		 * load the history entries via the {@link Preferences}. 
		 */
		void loadHistory() {
			// config file history 
			Preferences prefs = Preferences.userNodeForPackage(historyClass);
			if (prefs != null) {
				for (int i = 1; i <= MAX_HISTORY_ITEMS; i++) {
					String key = PREFERENCE_KEY_PREFIX_CONFIG_FILE_HISTORY + i;
					String fileString = prefs.get(key, null);
					if (fileString != null) {
						fileString = fileString.trim();
						if (fileString.length() > 1) {
							historyItems.put(fileString, new File(fileString));
						}
					}
				}
			}
		}
		
		/**
		 * store the history entries via the {@link Preferences}. 
		 */
		void storeHistory() {
			Preferences prefs = Preferences.userNodeForPackage(historyClass);
			if (prefs != null) {
				Iterator<String> iterator = historyItems.keySet().iterator();
				for (int i = 1; i <= MAX_HISTORY_ITEMS; i++) {
					String key = PREFERENCE_KEY_PREFIX_CONFIG_FILE_HISTORY + i;
					String value = null;
					if (iterator.hasNext()) {
						value = iterator.next();
					}
					if (value != null) {
						prefs.put(key, value);
					}
				}
			}
		}
		
		/**
		 * Add a file to the history. Write the history, if this operation 
		 * changed the history.
		 * 
		 * @param file
		 */
		void updateHistory(File file) {
			try {
				String key = file.getCanonicalPath();
				List<String> oldList = historyItems.getKeyList();
				historyItems.put(key, file);
				List<String> newList = historyItems.getKeyList();
				if (differs(oldList, newList)) {
					storeHistory();
				}
			} catch (IOException e) {
				LOGGER.warn("Could not add file to history: "+file, e);
			}
		}
		
		private boolean differs(List<String> oldList, List<String> newList) {
			if (oldList.size() != newList.size()) {
				return true;
			}
			for (int i = 0; i < oldList.size(); i++) {
				if (oldList.get(i).equals(newList.get(i)) == false) {
					return true;
				}
			}
			return false;
		}

		/**
		 * Internal LRU cache implementation using {@link LinkedHashMap}.
		 *
		 * @param <K> key type
		 * @param <V> value type
		 */
		private static class LRUCache<K, V> extends LinkedHashMap<K, V>  {
		
			// generated
			private static final long serialVersionUID = 7310516524956767409L;
			
			private final int maxSize;
		
			LRUCache(int maxSize) {
				super(maxSize + 1);
				this.maxSize = maxSize;
			}
		
			@Override
			protected boolean removeEldestEntry(Entry<K, V> eldest) {
				return size() > maxSize;
			}
			
			/**
			 * @return copy of the current key set as list
			 */
			public List<K> getKeyList() {
				List<K> list = new ArrayList<K>(size());
				final Set<K> keySet = keySet();
				for(K key : keySet) {
					list.add(key);
				}
				return list;
			}
		}
	}
	
	private boolean loadConfigFile(File file) {
		if (file != null && file.isFile() && file.canRead()) {
			try {
				OortConfiguration.loadConfig(file, parameters);
				frame.applyConfig(parameters);
				LOGGER.info("Finished loading OORT config from file: " + file);
				return true;
				
			} catch (IOException exception) {
				LOGGER.warn("Could not load config file: " + file, exception);
			}
		}
		return false;
	}
	
	private boolean writeConfigFile(File file) {
		if (file != null) {
			try {
				OortConfiguration.writeConfig(file, parameters);
				LOGGER.info("Finished saving OORT config to file: " + file);
				return true;
			} catch (IOException exception) {
				LOGGER.warn("Could not save OORT config to file: " + file, exception);
			}
		}
		return false;
	}
}
