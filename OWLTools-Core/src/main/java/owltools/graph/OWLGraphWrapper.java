package owltools.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.obolibrary.obo2owl.Obo2OWLConstants;
import org.obolibrary.obo2owl.Obo2OWLConstants.Obo2OWLVocabulary;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.UnknownOWLOntologyException;

import owltools.util.OwlHelper;

/**
 * Wraps one or more OWLOntology objects providing convenient OBO-like operations 
 * 
 * <h3>Capabilities</h3>
 * <ul>
 * <li>convenience methods for OBO-like properties such as synonyms, textual definitions, obsoletion, replaced_by
 * <li>simple graph-like operations over ontologies, including reachability/closure queries that respect the OWL semantics
 * </ul>
 *
 * <h3>Data model</h3>
 * 
 * An instance of an OWLGraphWrapper wraps one or more {@link org.semanticweb.owlapi.model.OWLOntology} objects. One of these is designated
 * the <i>sourceOntology</i>, the others are designated <i>support ontologies</i>
 * (see {@link #getSourceOntology()} and {@link #getSupportOntologySet()}).
 * The source ontology may import the support
 * ontologies, but this is optional. Most OWLGraphWrapper methods operate over the union of the source ontology
 * and support ontologies. This is particularly useful for working with OBO Library ontologies, where axioms
 * connecting ontologies may be available as separate ontologies. 
 * 
 *  <h3>Graph operations</h3>
 *
 * See {@link owltools.graph}
 * 
 * <h3>Fetching objects</h3>
 * 
 * This wrapper provides convenience methods for fetching objects by OBO-Style IDs, IRIs or by labels.
 * Note that unlike the get* calls on {@link OWLDataFactory} objects, these only return an object if it
 * has been declared in either the source ontology or a support ontology.
 * 
 * See for example
 * 
 * <ul>
 *  <li>{@link #getOWLClass(String id)}
 *  <li>{@link #getOWLClassByIdentifier(String id)}
 *  <li>{@link #getOWLObjectByLabel(String label)}
 * </ul>
 * <h3>OBO Metadata</h3>
 * 
 * <h4>OBO-style identifiers</h4>
 * 
 * This class accepts the use of OBO-Format style identifiers in some contexts, e.g. GO:0008150
 * 
 * See methods such as
 * <ul>
 *  <li>{@link #getOWLClassByIdentifier(String id)}
 * </ul>
 * 
 * <h4>Textual metadata</h4>
 * 
 *  Documentation to follow....
 *
 * @see OWLGraphUtil
 * @author cjm
 * 
 * <p>
 * For maintenance purpose this class has been split into multiple classes:
 * <ul>
 *    <li>{@link OWLGraphWrapperBasic} Methods handling multiple {@link OWLOntology} objects in one graph</li>
 *    <li>{@link OWLGraphWrapperExtended} Methods to extract information about entities, includes OBO-style information </li>
 *    <li>{@link OWLGraphWrapperEdges} Methods for handling edges and relations</li>
 *    <li>{@link OWLGraphWrapperEdgesAdvanced} Methods related GOlr and advanced graph traversal options</li>
 * </ul>
 * </p>
 */
public class OWLGraphWrapper extends OWLGraphWrapperEdgesAdvanced {

	public static final String DEFAULT_IRI_PREFIX = Obo2OWLConstants.DEFAULT_IRI_PREFIX;

	@Deprecated
	OWLOntology ontology; // this is the ontology used for querying. may be the merge of sourceOntology+closure


	/**
	 * Create a new wrapper for an OWLOntology
	 * 
	 * @param ontology 
	 */
	public OWLGraphWrapper(OWLOntology ontology) {
		super(ontology);
	}

	/**
	 * Create a new wrapper for an OWLOntology
	 * 
	 * @param ontology
	 * @param isMergeImportClosure
	 * @throws UnknownOWLOntologyException
	 * @throws OWLOntologyCreationException
	 * @deprecated
	 */
	@Deprecated
	public OWLGraphWrapper(OWLOntology ontology, boolean isMergeImportClosure) throws UnknownOWLOntologyException, OWLOntologyCreationException {
		super(ontology);
		if (isMergeImportClosure) {
			// the query ontology is the source ontology plus the imports closure
			useImportClosureForQueries();
		}
	}

	/**
	 * creates a new {@link OWLOntology} as the source ontology
	 * 
	 * @param iri
	 * @throws OWLOntologyCreationException
	 */
	public OWLGraphWrapper(String iri) throws OWLOntologyCreationException {
		super(iri);
	}

	/**
	 * if called, copies all axioms from import closure into query ontology.
	 * 
	 * @throws UnknownOWLOntologyException
	 * @throws OWLOntologyCreationException
	 */
	@Deprecated
	public void useImportClosureForQueries() throws UnknownOWLOntologyException, OWLOntologyCreationException {
		this.ontology = 
			OWLManager.createOWLOntologyManager().createOntology(sourceOntology.getOntologyID().getOntologyIRI().get(), sourceOntology.getImportsClosure());
	}

	@Deprecated
	public void addQueryOntology(OWLOntology extOnt) throws OWLOntologyCreationException {
		Set<OWLAxiom> axioms = ontology.getAxioms();
		axioms.addAll(extOnt.getAxioms());
		this.ontology = 
			OWLManager.createOWLOntologyManager().createOntology(axioms, sourceOntology.getOntologyID().getOntologyIRI().get());	
	}

	@Deprecated
	public OWLOntology getOntology() {
		return getSourceOntology();
	}


	@Deprecated
	public void setOntology(OWLOntology ontology) {
		setSourceOntology(ontology);
	}



	/**
	 * It returns array of synonyms (is encoded as synonym in obo format and IAO_0000118 annotation property in OWL format) of a class
	 * @param c
	 * @return array of strings or null
	 */
	@Deprecated
	public String[] getSynonymStrings(OWLObject c) {
		OWLAnnotationProperty lap = getDataFactory().getOWLAnnotationProperty(IRI.create(DEFAULT_IRI_PREFIX + "IAO_0000118")); 
		Set<OWLAnnotation>anns = null;
		if (c instanceof OWLEntity) {
			anns = OwlHelper.getAnnotations((OWLEntity) c, lap, sourceOntology);
		}
		else {
			return null;
		}

		List<String> list = new ArrayList<String>();
		for (OWLAnnotation a : anns) {
			if (a.getValue() instanceof OWLLiteral) {
				OWLLiteral val = (OWLLiteral) a.getValue();
				list.add(val.getLiteral()); // return first - todo - check zero or one
			}
		}
		return list.toArray(new String[list.size()]);
	}

	/**
	 * It returns list of synonyms as encoded by OBO2OWL.
	 * 
	 * @param c
	 * @return list of synonyms or null
	 */
	public List<ISynonym> getOBOSynonyms(OWLObject c) {
		OWLEntity e;
		if (c instanceof OWLEntity) {
			e = (OWLEntity) c;
		}
		else {
			return null;
		}
		List<ISynonym> synonyms = null;

		synonyms = merge(synonyms, getOBOSynonyms(e, Obo2OWLVocabulary.IRI_OIO_hasExactSynonym));
		synonyms = merge(synonyms, getOBOSynonyms(e, Obo2OWLVocabulary.IRI_OIO_hasRelatedSynonym));
		synonyms = merge(synonyms, getOBOSynonyms(e, Obo2OWLVocabulary.IRI_OIO_hasNarrowSynonym));
		synonyms = merge(synonyms, getOBOSynonyms(e, Obo2OWLVocabulary.IRI_OIO_hasBroadSynonym));
		return synonyms;
	}

	private <T> List<T> merge(List<T> list1, List<T> list2) {
		if (list1 == null || list1.isEmpty()) {
			return list2;
		}
		if (list2 == null || list2.isEmpty()) {
			return list1;
		}
		List<T> synonyms = new ArrayList<T>(list1.size() + list2.size());
		synonyms.addAll(list1);
		synonyms.addAll(list2);
		return synonyms;
	}
	
	/**
	 * It returns String Listof synonyms.
	 * 
	 * The strange signature is due to being used for FlexLoader.
	 * 
	 * @param c
	 * @param sargs (currently unused)
	 * @return string list of synonyms
	 */
	public List<String> getOBOSynonymStrings(OWLObject c, List<String> sargs) {

		// Buffer against later possible changes.
		if( sargs == null )
			sargs = new ArrayList<String>();
		
		List<String> synStrings = new ArrayList<String>();

		// Term synonym gathering rather more irritating.
		List<ISynonym> syns = getOBOSynonyms(c);
		if( syns != null && ! syns.isEmpty() ){	
			for( ISynonym s : syns ){
				String synLabel = s.getLabel();

				// Standard neutral synonym.
				synStrings.add(synLabel);

				// EXPERIMENTAL: scoped synonym label.
				//String synScope = s.getScope();
				//String synScopeName = "synonym_label_with_scope_" + synScope.toLowerCase();
				//cls_doc.addField(synScopeName, synLabel);
			}
		}	

		return synStrings;
	}

	private List<ISynonym> getOBOSynonyms(OWLEntity e, Obo2OWLVocabulary vocabulary) {
		// get all synonyms defined in the source ontology
		Set<ISynonym> synonymSet = getOBOSynonyms(e, vocabulary, sourceOntology);
		// iterate over import closure, as the OWL-API currently doesn't have a 
		// method get annotations and its axioms from imported ontologies 
		for(OWLOntology ont : sourceOntology.getImportsClosure()) {
			synonymSet = merge(synonymSet, getOBOSynonyms(e, vocabulary, ont));
		}
		
		// repeat for support ontologies
		for(OWLOntology support : getSupportOntologySet()) {
			synonymSet = merge(synonymSet, getOBOSynonyms(e, vocabulary, support));
			for(OWLOntology ont : support.getImportsClosure()) {
				synonymSet = merge(synonymSet, getOBOSynonyms(e, vocabulary, ont));
			}
		}
		if (synonymSet == null || synonymSet.isEmpty()) {
			return null;
		}
		
		// sort the result alphabetical
		List<ISynonym> synonyms = new ArrayList<ISynonym>(synonymSet);
		Collections.sort(synonyms, new Comparator<ISynonym>() {

			@Override
			public int compare(ISynonym o1, ISynonym o2) {
				int cmp = compareStrings(o1.getLabel(), o2.getLabel());
				if (cmp == 0) {
					cmp = compareStrings(o1.getScope(), o2.getScope());
				}
				if (cmp == 0) {
					cmp = compareStrings(o1.getCategory(), o2.getCategory());
				}
				return cmp;
			}
			
			private int compareStrings(String s1, String s2) {
				int cmp = 0;
				if (s1 != null) {
					if (s2 == null) {
						cmp = -1;
					}
					else {
						cmp = s1.compareTo(s2);
					}
				}
				else if (s2 != null) {
					cmp = 1;
				}
				return cmp ;
			}
		});
		return synonyms ;
	}
	
	private <T> Set<T> merge(Set<T> set1, Set<T> set2) {
		if (set1 == null || set1.isEmpty()) {
			return set2;
		}
		if (set2 == null || set2.isEmpty()) {
			return set1;
		}
		Set<T> synonyms = new HashSet<T>(set1);
		synonyms.addAll(set2);
		return synonyms;
	}

	private Set<ISynonym> getOBOSynonyms(OWLEntity e, Obo2OWLVocabulary vocabulary, OWLOntology ont) {
		OWLAnnotationProperty synonymProperty = getDataFactory().getOWLAnnotationProperty(vocabulary.getIRI());
		Set<OWLAnnotation> anns = OwlHelper.getAnnotations(e, synonymProperty, ont);
		Set<OWLAnnotationAssertionAxiom> annotationAssertionAxioms = ont.getAnnotationAssertionAxioms(e.getIRI());
		if (anns != null && !anns.isEmpty()) {
			Set<ISynonym> set = new HashSet<ISynonym>();
			for (OWLAnnotation a : anns) {
				if (a.getValue() instanceof OWLLiteral) {
					OWLLiteral val = (OWLLiteral) a.getValue();
					String label = val.getLiteral();
					if (label != null && label.length() > 0) {
						String category = null;
						Set<String> xrefs = null;
						SynonymDetails details = getOBOSynonymDetails(annotationAssertionAxioms, val, synonymProperty);
						if (details != null) {
							category = details.category;
							xrefs = details.xrefs;
						}
						Synonym s = new Synonym(label, vocabulary.getMappedTag(), category, xrefs);
						set.add(s);
					}
				}
			}
			if (!set.isEmpty()) {
				return set;
			}
		}
		return null;
	}
	
	private static class SynonymDetails {
		Set<String> xrefs = null;
		String category = null;
	}
	
	/**
	 * Check all {@link OWLAnnotationAssertionAxiom} to find the corresponding axiom for the given value ({@link OWLLiteral}).
	 * 
	 * @param annotationAssertionAxioms
	 * @param val
	 * @param synonymProperty
	 * @return {@link SynonymDetails} or null
	 */
	private SynonymDetails getOBOSynonymDetails(Set<OWLAnnotationAssertionAxiom> annotationAssertionAxioms, OWLLiteral val, OWLAnnotationProperty synonymProperty) {
		// quick extis, if there are no axioms
		if (annotationAssertionAxioms == null || annotationAssertionAxioms.isEmpty()) {
			return null;
		}
		Set<String> xrefs = new HashSet<String>();
		String category = null;
		
		// iterate of all axioms
		for (OWLAnnotationAssertionAxiom annotationAssertionAxiom : annotationAssertionAxioms) {

			// check if its is the corresponding value
			if (!val.equals(annotationAssertionAxiom.getValue())) {
				continue;
			}
			
			// check if it is the correct property
			if (synonymProperty.equals(annotationAssertionAxiom.getProperty())) {
				// analyze the annotations from the axiom
				Set<OWLAnnotation> annotations = annotationAssertionAxiom.getAnnotations();
				
				for (OWLAnnotation owlAnnotation : annotations) {
					IRI annotationTypeIRI = owlAnnotation.getProperty().getIRI();
					
					// check db xrefs
					if (Obo2OWLVocabulary.IRI_OIO_hasDbXref.getIRI().equals(annotationTypeIRI)) {
						OWLAnnotationValue owlAnnotationValue = owlAnnotation.getValue();
						if (owlAnnotationValue instanceof OWLLiteral) {
							String value = ((OWLLiteral) owlAnnotationValue).getLiteral();
							xrefs.add(value);
						}
					}
					// check synonym type
					else if (Obo2OWLVocabulary.hasSynonymType.getIRI().equals(annotationTypeIRI)) {
						OWLAnnotationValue owlAnnotationValue = owlAnnotation.getValue();
						if (owlAnnotationValue instanceof IRI) {
							category = ((IRI) owlAnnotationValue).getFragment();
						}
					}
				}
			}
		}
		// only return an object, if there are some value to report
		if (!xrefs.isEmpty() || category != null) {
			SynonymDetails details = new SynonymDetails();
			if (!xrefs.isEmpty()) {
				details.xrefs = xrefs;	
			}
			details.category = category;
			
			return details;
		}
		return null;
	}

	public static interface ISynonym {
		/**
		 * @return the label
		 */
		public String getLabel();

		/**
		 * @return the scope
		 */
		public String getScope();

		/**
		 * @return the category
		 */
		public String getCategory();

		/**
		 * @return the xrefs
		 */
		public Set<String> getXrefs();
	}


	public static class Synonym implements ISynonym {
		private String label;
		private String scope;
		private String category;
		private Set<String>  xrefs;

		/**
		 * @param label
		 * @param scope
		 * @param category
		 * @param xrefs
		 */
		public Synonym(String label, String scope, String category, Set<String> xrefs) {
			super();
			this.label = label;
			this.scope = scope;
			this.category = category;
			this.xrefs = xrefs;
		}

		@Override
		public String getLabel() {
			return label;
		}

		@Override
		public String getScope() {
			return scope;
		}

		@Override
		public String getCategory() {
			return category;
		}

		@Override
		public Set<String> getXrefs() {
			return xrefs;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("Synonym [");
			builder.append("label=");
			builder.append(label);
			if (scope != null) {
				builder.append(", scope=");
				builder.append(scope);
			}
			if (category != null) {
				builder.append(", category=");
				builder.append(category);
			}
			if (xrefs != null) {
				builder.append(", xrefs=");
				builder.append(xrefs);
			}
			builder.append("]");
			return builder.toString();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((category == null) ? 0 : category.hashCode());
			result = prime * result + ((label == null) ? 0 : label.hashCode());
			result = prime * result + ((scope == null) ? 0 : scope.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (obj instanceof ISynonym == false) {
				return false;
			}
			ISynonym other = (ISynonym) obj;
			if (label == null) {
				if (other.getLabel() != null) {
					return false;
				}
			}
			else if (!label.equals(other.getLabel())) {
				return false;
			}
			if (scope == null) {
				if (other.getScope() != null) {
					return false;
				}
			}
			else if (!scope.equals(other.getScope())) {
				return false;
			}
			if (category == null) {
				if (other.getCategory() != null) {
					return false;
				}
			}
			else if (!category.equals(other.getCategory())) {
				return false;
			}
			return true;
		}
	}


}

