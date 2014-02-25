package owltools.gaf.eco;

import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;

public interface EcoMapper {
	
	/**
	 * Permanent URL for the evidence code ontology (ECO) owl file.
	 */
	public static final String ECO_PURL = "http://purl.obolibrary.org/obo/eco.owl";
	
	/**
	 * IRI for the evidence code ontology (ECO) owl file.
	 */
	public static final IRI ECO_PURL_IRI = IRI.create(ECO_PURL);
	
	/**
	 * Permanent URL for the mapping of GO evidence codes to ECO classes
	 */
	public static final String ECO_MAPPING_PURL = "http://purl.obolibrary.org/obo/eco/gaf-eco-mapping.txt";

	/**
	 * Retrieve the equivalent ECO class for the given GO evidence code. Assume, that the reference is 'default'.
	 * 
	 * @param code
	 * @return {@link OWLClass} or null
	 */
	public OWLClass getEcoClassForCode(String code);
	
	/**
	 * Retrieve the ECO classes for the given GO evidence code. Include the classes to be used with more specific references.
	 * 
	 * @param code
	 * @return set of classes, never null
	 */
	public Set<OWLClass> getAllEcoClassesForCode(String code);
	
	/**
	 * Retrieve the ECO class for the given GO evidence code and reference. If reference is null, assume default. 
	 * 
	 * @param code
	 * @param refCode
	 * @return {@link OWLClass} or null
	 */
	public OWLClass getEcoClassForCode(String code, String refCode);
	
	
	/**
	 * Check that the given GO code is a valid code with an existing mapping to ECO
	 * 
	 * @param code
	 * @return true if the code is a valid
	 */
	public boolean isGoEvidenceCode(String code);
	
	/**
	 * Retrieve the mapping from ECO classes to GO evidence codes.
	 * 
	 * @return mapping
	 */
	public Map<OWLClass, String> getCodesForEcoClasses();
}
