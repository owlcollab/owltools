package owltools.ontologyrelease;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.obolibrary.obo2owl.Obo2OWLConstants;

/**
 * Tools to handle the writing and reading of version information of 
 * owl ontology files.
 * 
 * @author hdietze
 *
 */
public class OntologyVersionTools {
	
	private static final ThreadLocal<DateFormat> versionIRIDateFormat = new ThreadLocal<DateFormat>() {

		@Override
		protected DateFormat initialValue() {
			return new SimpleDateFormat("yyyy-MM-dd");
		}
	};
	
	private static final Pattern versionIRIPattern = Pattern.compile(Obo2OWLConstants.DEFAULT_IRI_PREFIX+"\\S+/(.+)/\\S+\\.owl");
	
	private OntologyVersionTools() {
		// No instances, static methods only.
	}
	
	/**
	 * Try to parse the IRI as a version IRI using the both patterns 
	 * for version IRI and OboInOwl.
	 * 
	 * @param versionIRI
	 * @return date or null
	 */
	public static String parseVersion(String versionIRI) {
		if (versionIRI == null || versionIRI.length() <= Obo2OWLConstants.DEFAULT_IRI_PREFIX.length()) {
			return null;
		}
		Matcher versionIRIMatcher = versionIRIPattern.matcher(versionIRI);
		if (versionIRIMatcher.matches()) {
			return versionIRIMatcher.group(1);
		}
		return null;
	}
	
	/**
	 * Format a date into the canonical format of YYYY-MM-DD.
	 * 
	 * @param date
	 * @return string
	 */
	public static String format(Date date) {
		return versionIRIDateFormat.get().format(date);
	}
}
