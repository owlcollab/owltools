package owltools.ontologyrelease;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.obolibrary.obo2owl.Obo2Owl;
import org.obolibrary.oboformat.parser.OBOFormatConstants.OboFormatTag;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.SetOntologyID;

/**
 * Tools to handle the writing and reading of version information of 
 * owl ontology files.
 * 
 * @author hdietze
 *
 */
public class OntologyVersionTools {
	
	private static final Logger logger = Logger.getLogger(OntologyVersionTools.class);

	private static final ThreadLocal<DateFormat> versionIRIDateFormat = new ThreadLocal<DateFormat>() {

		@Override
		protected DateFormat initialValue() {
			return new SimpleDateFormat("yyyy-MM-dd");
		}
	};
	
	private static final Pattern oboVersionIRIPattern = Pattern.compile("http://purl.obolibrary.org/obo/\\S+/([1-9][0-9]{3}-[0-9]{2}-[0-9]{2})/\\S+.owl");
	private static final Pattern oboInOWLRemarkPattern = Pattern.compile("[1-9][0-9]{3}-[0-9]{2}-[0-9]{2}");
	
	private OntologyVersionTools() {
		// No instances, static methods only.
	}
	
	/**
	 * Retrieve the ontology version as {@link Date}. Expects that 
	 * the version IRI is in the OBO specific format.
	 * 
	 * @param ontology
	 * @return date or null
	 */
	public static Date getOntologyVersionDate(OWLOntology ontology) {
		return parseVersion(getOntologyVersion(ontology));
	}
	
	/**
	 * Retrieve the ontology version as string.
	 * 
	 * @param ontology
	 * @return string or null
	 */
	public static String getOntologyVersion(OWLOntology ontology) {
		String version = null;
		IRI iri = getOntologyVersionIRI(ontology);
		if (iri != null) {
			version = iri.toString();
		}
		return version;
	}
	
	/**
	 * Retrieve the ontology version as IRI.
	 * 
	 * @param ontology
	 * @return iri or null
	 */
	public static IRI getOntologyVersionIRI(OWLOntology ontology) {
		OWLOntologyID ontologyID = ontology.getOntologyID();
		if (ontologyID != null) {
			return ontologyID.getVersionIRI();
		}
		return null;
	}
	
	/**
	 * Set the ontology version IRI in the OntologyID to the given string.
	 * 
	 * @param ontology target ontology
	 * @param versionIRI string which can be parsed in a legal IRI
	 * @return ontology change
	 */
	public static SetOntologyID setOntologyVersion(OWLOntology ontology, String versionIRI) {
		return setOntologyVersion(ontology, IRI.create(versionIRI));
	}
	
	/**
	 * Set the ontology version IRI in the OntologyID. 
	 * Uses the date, ontologyId, and filename to construct 
	 * an OBO specific IRI.
	 * 
	 * @param ontology
	 * @param date
	 * @param ontologyId
	 * @param filename
	 * @return ontology change
	 */
	public static SetOntologyID setOntologyVersion(OWLOntology ontology, Date date, String ontologyId, String filename) {
		IRI iri = createVersionIRI(ontologyId, filename, date);
		return setOntologyVersion(ontology, iri);
	}
	
	/**
	 * Set the ontology version IRI in the OntologyID.
	 * 
	 * @param ontology
	 * @param iri
	 * @return ontology change
	 */
	public static SetOntologyID setOntologyVersion(OWLOntology ontology, IRI iri) {
		OWLOntologyID ontologyID = new OWLOntologyID(ontology.getOntologyID().getOntologyIRI(), iri);
		SetOntologyID change = new SetOntologyID(ontology, ontologyID);
		ontology.getOWLOntologyManager().applyChange(change);
		return change;
	}
	
	/**
	 * Retrieve the ontology version from the OboInOwl remark.
	 * 
	 * @param ontology
	 * @return version or null
	 */
	public static String getOboInOWLVersion(OWLOntology ontology) {
		Set<OWLAnnotationAssertionAxiom> axioms = ontology.getAnnotationAssertionAxioms(ontology.getOntologyID().getOntologyIRI());
		if (axioms != null && !axioms.isEmpty()) {
			OWLOntologyManager manager = ontology.getOWLOntologyManager();
			OWLDataFactory fac = manager.getOWLDataFactory();

			OWLAnnotationProperty ap = fac.getOWLAnnotationProperty(Obo2Owl.trTagToIRI(OboFormatTag.TAG_REMARK.getTag()));
			for (OWLAnnotationAssertionAxiom axiom : axioms) {
				OWLAnnotation annotation = axiom.getAnnotation();
				if (annotation != null) {
					if (ap.equals(annotation.getProperty())) {
						OWLAnnotationValue value = annotation.getValue();
						if (value instanceof OWLLiteral) {
							return ((OWLLiteral) value).getLiteral();
						}
					}
				}
			}
		}
		return null;
	}
	
	/**
	 * Retrieve the ontology version from the OboInOwl remark and parse as {@link Date}.
	 * 
	 * @param ontology
	 * @return date or null
	 */
	public static Date getOboInOWLVersionDate(OWLOntology ontology) {
		return parseVersion(getOboInOWLVersion(ontology));
	}
	
	/**
	 * Set the OboInOWL remark to the given ontology version.
	 * 
	 * @param ontology
	 * @param version
	 * @return ontology change
	 */
	public static AddAxiom setOboInOWLVersion(OWLOntology ontology, String version) {
		OWLOntologyManager manager = ontology.getOWLOntologyManager();
		OWLDataFactory fac = manager.getOWLDataFactory();

		OWLAnnotationProperty ap = fac.getOWLAnnotationProperty(Obo2Owl.trTagToIRI(OboFormatTag.TAG_REMARK.getTag()));
		OWLAnnotation ann = fac .getOWLAnnotation(ap, fac.getOWLLiteral(version));

		OWLAxiom ax = fac.getOWLAnnotationAssertionAxiom(ontology.getOntologyID().getOntologyIRI(), ann);
		AddAxiom change = new AddAxiom(ontology, ax);
		manager.applyChange(change);
		return change;
	}
	
	/**
	 * Set the OboInOWL remark to the given date.
	 * 
	 * @param ontology
	 * @param date
	 * @return ontology change
	 */
	public static AddAxiom setOboInOWLVersion(OWLOntology ontology, Date date) {
		return setOboInOWLVersion(ontology, format(date));
	}
	
	/**
	 * Check, whether the given version string is an OBO-specific IRI.
	 * 
	 * @param version
	 * @return true, if the version matches an OBO specific ontology version pattern
	 */
	public static boolean isOBOOntologyVersion(String version) {
		if (version != null && version.length() >= 49) {
			// minimum length: http://purl.obolibrary.org/obo/x/YYYY-MM-DD/x.owl"
			return oboVersionIRIPattern.matcher(version).matches();
		}
		return false;
	}
	
	/**
	 * Try to parse the string as a {@link Date} using the both patterns 
	 * for version IRI and OboInOwl.
	 * 
	 * @param version
	 * @return date or null
	 */
	public static Date parseVersion(String version) {
		if (version == null || version.length() < 10) {
			// minimum length: YYYY-MM-DD
			return null;
		}
		Matcher oboInOwlMatcher = oboInOWLRemarkPattern.matcher(version);
		if (oboInOwlMatcher.matches()) {
			try {
				return versionIRIDateFormat.get().parse(version);
			} catch (ParseException e) {
				logger.debug("Could not parse date from version: "+version);
				return null;
			}
		}
		Matcher oboVersionIRIMatcher = oboVersionIRIPattern.matcher(version);
		if (oboVersionIRIMatcher.matches()) {
			String dateString = oboVersionIRIMatcher.group(1);
			try {
				return versionIRIDateFormat.get().parse(dateString);
			} catch (ParseException e) {
				logger.debug("Could not parse date from version: "+version+"with date: "+dateString);
				return null;
			}
		}
		return null;
	}
	
	private static IRI createVersionIRI(String ontologyId, String fn, Date date) {
		IRI versionIRI = IRI.create("http://purl.obolibrary.org/obo/"+ontologyId+"/"+format(date)+"/"+fn+".owl");
		return versionIRI;
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
