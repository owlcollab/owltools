package owltools.gaf.eco;

import java.util.Set;

import org.semanticweb.owlapi.model.OWLClass;

public interface TraversingEcoMapper extends EcoMapper {

	/**
	 * Traversing method for the ECO ontology.
	 * 
	 * @param sources
	 * @param reflexive
	 * @return set of super classes
	 */
	public Set<OWLClass> getAncestors(Set<OWLClass> sources, boolean reflexive);
	
	/**
	 * Traversing method for the ECO ontology.
	 * 
	 * @param source
	 * @param reflexive
	 * @return set of super classes
	 */
	public Set<OWLClass> getAncestors(OWLClass source, boolean reflexive);
	
	/**
	 * Traversing method for the ECO ontology.
	 * 
	 * @param sources
	 * @param reflexive
	 * @return set of sub classes
	 */
	public Set<OWLClass> getDescendents(Set<OWLClass> sources, boolean reflexive);
	
	/**
	 * Traversing method for the ECO ontology.
	 * 
	 * @param source
	 * @param reflexive
	 * @return set of sub classes
	 */
	public Set<OWLClass> getDescendents(OWLClass source, boolean reflexive);
	
	
	/**
	 * Get all strings which are valid identifiers for a given evidence code.
	 * This includes, the the codes itself and valid OBO-style identifier from ECO.
	 * 
	 * @param code
	 * @param includeChildren
	 * @return set of ids
	 */
	public Set<String> getAllValidEvidenceIds(String code, boolean includeChildren);
	
	/**
	 * Get all strings which are valid identifiers for the given evidence codes.
	 * This includes, the the codes itself and valid OBO-style identifier from ECO.
	 * 
	 * @param codes
	 * @param includeChildren
	 * @return set of ids
	 */
	public Set<String> getAllValidEvidenceIds(Set<String> codes, boolean includeChildren);
	
	/**
	 * Dispose this instance
	 */
	public void dispose();
}
