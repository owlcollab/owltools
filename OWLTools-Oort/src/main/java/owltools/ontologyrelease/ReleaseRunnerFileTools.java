package owltools.ontologyrelease;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;


/**
 * Provide a staging directory for the release runners. 
 */
abstract class ReleaseRunnerFileTools {

	private static final String EXTENSIONS_DIRECTORY_NAME = "extensions";
	private static final String SUBSETS_DIRECTORY_NAME = "subsets";
	private static final String RELEASE_DIRECTORY_NAME = "releases";
	private static final String STAGING_DIRECTORY_NAME = "staging";
	private static final String STAGING_DIRECTORY_LOCK_FILE_NAME = ".lock";
	
	private final File base;
	private final File staging;
	private final File lockFile;
	private final Logger logger;
	private final boolean useReleasesFolder;

	/**
	 * @param base directory
	 * @param logger
	 * @param useReleasesFolder 
	 * @param ignoreLock
	 * @throws IOException
	 */
	ReleaseRunnerFileTools(File base, Logger logger, boolean useReleasesFolder, boolean ignoreLock) throws IOException {
		super();
		this.logger = logger;
		this.useReleasesFolder = useReleasesFolder;
		
		// base
		this.base = base;
		
		checkFolder(base);
		
		// staging directory
		staging = new File(base, STAGING_DIRECTORY_NAME);
		checkFolder(staging);
		
		// lock file
		lockFile = new File(staging, STAGING_DIRECTORY_LOCK_FILE_NAME);
		boolean success = lockFile.createNewFile();
		if (!success && !ignoreLock) {
			if (!forceLock(lockFile)) {
				throw new IOException("Could not lock staging directory via lock file: "+lockFile.getAbsolutePath());
			}
			FileUtils.touch(lockFile);
		}
		logger.info("Using staging folder for release manager: "+staging.getAbsolutePath());
		
		// clean staging
		cleanDirectory(staging, STAGING_DIRECTORY_LOCK_FILE_NAME);
		
		// sub directories
		File subsets = new File(staging, SUBSETS_DIRECTORY_NAME);
		checkFolder(subsets);

		File extensions = new File(staging, EXTENSIONS_DIRECTORY_NAME);
		checkFolder(extensions);
	}
	
	boolean forceLock(File file) {
		return false;
	}
	
	BufferedWriter getWriter(String fileName) throws IOException {
		return new BufferedWriter(new OutputStreamWriter(getOutputSteam(fileName), "UTF-8"));
	}
	
	OutputStream getOutputSteam(String fileName) throws IOException {
		// fail early, if the release manager would try to overwrite an existing file in the commit.
		checkNew(new File(base, fileName));
		// cleared: either overwrite is okay or is a new file 
		// But keep working in the staging directory
		File stagingFile = new File(staging, fileName);
		
		// create sub folder
		stagingFile.getParentFile().mkdirs();
		
		logger.info("saving to " + stagingFile.getAbsolutePath());
		return new FileOutputStream(stagingFile);
	}
	
	/**
	 * Check whether the file is new. Throw an {@link IOException}, 
	 * if the file already exists and {@link OortConfiguration#isAllowFileOverWrite()} 
	 * is not set to true.
	 * 
	 * @param file
	 * @return file return the same file to allow chaining with other operations
	 * @throws IOException
	 */
	abstract File checkNew(File file) throws IOException;
	
	/**
	 * Create a release with the files in the staging directory.
	 * Skip the release, if the previous release has equal content.
	 * 
	 * @param version
	 * @return true, if the commit and copy was done.
	 * @throws IOException
	 */
	boolean commit(String version) throws IOException {
		try {
			if (useReleasesFolder) {
				File releasesFolder = new File(base, RELEASE_DIRECTORY_NAME);
				checkFolder(releasesFolder);
				// make version specific releases sub folder
				File versionFolder = new File(releasesFolder, version);
				checkFolder(versionFolder);
				// copy from staging directory into version specific releases folder
				copyContents(staging, versionFolder, STAGING_DIRECTORY_LOCK_FILE_NAME);
			}
			// copy stuff from staging directory into the live directory
			copyContents(staging, base, STAGING_DIRECTORY_LOCK_FILE_NAME);
			
			return true;
		} finally {
			// delete staging content, including the lock file
			FileUtils.cleanDirectory(staging);
		}
	}
	
	private void copyContents(File sourceFolder, File targetFolder, final String...ignores) throws IOException {
		FileUtils.copyDirectory(sourceFolder, targetFolder, createIngoreFilter(ignores), true);
	}
	
	
	/**
	 * Clean a directory, by deleting all files and folders in it. Retain the 
	 * top level files or folders, if their names are in the ignores array.
	 * 
	 * @param folder
	 * @param ignores
	 * @throws IOException
	 */
	private void cleanDirectory(final File folder, final String...ignores) throws IOException {
		File[] files = folder.listFiles(createIngoreFilter(ignores));
		if (files.length > 0) {
			logger.info("Cleaning folder: "+folder.getAbsolutePath());
			for (File file : files) {
				if (!FileUtils.isSymlink(file)) {
					// if file is symlink, do not recurse, only delete the link.
					file.delete();
				} else {
					FileUtils.forceDelete(file);
				}
			}
		}
	}
	
	private static FileFilter createIngoreFilter(final String...ignores) {
		FileFilter filter = null;
		if (ignores != null && ignores.length > 0) {
			filter = new FileFilter() {
				Set<String> names = createSet(ignores);
				
				@Override
				public boolean accept(File pathname) {
					return !names.contains(pathname.getName());
				}
			};
		}
		return filter;
	}
	
	private static <T> Set<T> createSet(T[] values) {
		if (values.length == 1) {
			return Collections.singleton(values[0]);
		}
		return new HashSet<T>(Arrays.asList(values));
	}
	
	static void checkFolder(File folder) throws IOException {
		FileUtils.forceMkdir(folder);
		if (!folder.exists()) {
			throw new IOException("Could not create directory: "+folder.getAbsolutePath());
		}
		if (!folder.isDirectory()) {
			throw new IOException(folder.getAbsolutePath()+" already exists, but is not a directory");
		}
		if (!folder.canWrite()) {
			throw new IOException("Can't write in directory: "+folder.getAbsolutePath());
		}
	}
	
	void deleteLockFile() {
		FileUtils.deleteQuietly(lockFile);
	}
	
}
