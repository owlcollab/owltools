package owltools.version;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class VersionInfo {

	private VersionInfo() {
		// make constructor private
	}
	
	/**
	 * Try to retrieve the value for the given key from a manifest file.
	 * Returns the first match or null, if it does not exist. 
	 * 
	 * @param key
	 * @return string value or null
	 */
	public static String getManifestVersion(String key) {
		Enumeration<URL> resEnum;
	    try {
	        resEnum = Thread.currentThread().getContextClassLoader().getResources(JarFile.MANIFEST_NAME);
	        while (resEnum.hasMoreElements()) {
	            try {
	                URL url = resEnum.nextElement();
	                InputStream is = url.openStream();
	                if (is != null) {
	                    Manifest manifest = new Manifest(is);
	                    Attributes mainAttribs = manifest.getMainAttributes();
	                    String version = mainAttribs.getValue(key);
	                    if(version != null) {
	                        return version;
	                    }
	                }
	            }
	            catch (Exception exception) {
	                // Silently ignore problematic manifests in classpath
	            }
	        }
	    } catch (IOException ioException) {
	        // Silently ignore any IO issues with manifests
	    }
	    return null; 
	}
}
