package owltools.gaf;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.UnknownOWLOntologyException;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Methods to simplify the work with the taxons. 
 */
public class TaxonTools {
	
	public static final String TAXON_PURL = "http://purl.obolibrary.org/obo/ncbitaxon/subsets/taxslim.owl";
	
	private final OWLReasoner reasoner;
	private final boolean disposeReasonerP;
	
	public final static String NCBI = "NCBITaxon:";
	public static final String TAXON_PREFIX = "taxon:";
	
	/**
	 * Create an instance for the given reasoner.
	 * 
	 * @param reasoner
	 * @param disposeReasoner set to true, if the reasoner should also be disposed
	 * @throws UnknownOWLOntologyException
	 * @throws OWLOntologyCreationException
	 * 
	 * @see #dispose()
	 */
	public TaxonTools (OWLReasoner reasoner, boolean disposeReasoner) throws UnknownOWLOntologyException, OWLOntologyCreationException {

		// This has bitten me, so let's try and be specific...
		if( reasoner == null ){ throw new Error("No reasoner was specified for use with the TaxonTools. Add a reasoner for the command line"); }
		
		this.reasoner = reasoner;
		this.disposeReasonerP = disposeReasoner;
	}
			
	/**
	 * Wrapper method for the reasoner.
	 * 
	 * @param taxonClass
	 * @param reflexive
	 * @return set of super classes
	 */
	public Set<OWLClass> getAncestors(OWLClass taxonClass, boolean reflexive) {
		if (taxonClass == null) {
			return Collections.emptySet();
		}
		Set<OWLClass> result = new HashSet<OWLClass>();
		Set<OWLClass> set = reasoner.getSuperClasses(taxonClass, false).getFlattened();
		for (OWLClass cls : set) {
			if (cls.isBuiltIn() == false) {
				result.add(cls);
			}
		}
		if (reflexive) {
			result.add(taxonClass);
		}
		if (result.isEmpty()) {
			return Collections.emptySet();
		}
		return result;
		
	}
	
	/**
	 * Wrapper method for the reasoner
	 * 
	 * @param sources
	 * @param reflexive
	 * @return set of sub classes
	 */
	public Set<OWLClass> getDescendents(Set<OWLClass> sources, boolean reflexive) {
		if (sources == null || sources.isEmpty()) {
			return Collections.emptySet();
		}
		Set<OWLClass> result = new HashSet<OWLClass>();
		for (OWLClass source : sources) {
			Set<OWLClass> set = reasoner.getSubClasses(source, false).getFlattened();
			for (OWLClass cls : set) {
				if (cls.isBuiltIn() == false) {
					result.add(cls);
				}
			}
		}
		if (reflexive) {
			result.addAll(sources);
		}
		if (result.isEmpty()) {
			return Collections.emptySet();
		}
		return result;
	}
	
	/**
	 * Clean up the internal data structures, usually done as last operation.
	 */
	public void dispose() {
		if (disposeReasonerP) {
			reasoner.dispose();
		}
	}

}
