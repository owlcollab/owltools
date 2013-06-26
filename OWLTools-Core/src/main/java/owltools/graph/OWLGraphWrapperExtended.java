package owltools.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.obolibrary.obo2owl.Obo2OWLConstants.Obo2OWLVocabulary;
import org.obolibrary.obo2owl.Obo2Owl;
import org.obolibrary.obo2owl.Owl2Obo;
import org.obolibrary.oboformat.model.OBODoc;
import org.obolibrary.oboformat.parser.OBOFormatConstants.OboFormatTag;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationSubject;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLFunctionalObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLInverseFunctionalObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLReflexiveObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLSymmetricObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLTransitiveObjectPropertyAxiom;
import org.semanticweb.owlapi.model.UnknownOWLOntologyException;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

/**
 * Methods to extract values from entities in the graph with potential multiple
 * {@link OWLOntology} objects. This includes methods, to extract OBO-style
 * information from the OWL representation.
 * 
 * @see OWLGraphWrapper
 * @see OWLGraphWrapperBasic
 */
public class OWLGraphWrapperExtended extends OWLGraphWrapperBasic {

	protected OWLGraphWrapperExtended(OWLOntology ontology) throws UnknownOWLOntologyException, OWLOntologyCreationException {
		super(ontology);
	}


	protected OWLGraphWrapperExtended(String iri) throws OWLOntologyCreationException {
		super(iri);
	}

	/**
	 * fetches the rdfs:label for an OWLObject
	 * <p>
	 * assumes zero or one rdfs:label
	 * 
	 * @param c
	 * @return label
	 */
	public String getLabel(OWLObject c) {
		return getAnnotationValue(c, getDataFactory().getRDFSLabel());
	}
	
	
	/**
	 * fetches the rdfs:label for an OWLObject
	 * <p>
	 * This is a curried FlexLoader s-expression version of {@link #getLabel(OWLObject)}.
	 * 
	 * @param c
	 * @param sargs
	 * @return label
	 * 
	 * @see #getLabel(OWLObject)
	 */
	public String getLabel(OWLObject c, ArrayList<String> sargs) {
		return getLabel(c);
	}

	
	public String getLabelOrDisplayId(OWLObject c) {
		String label = getLabel(c);
		if (label == null) {
			if (c instanceof OWLNamedObject) {
				OWLNamedObject nc = (OWLNamedObject)c;
				label = nc.getIRI().getFragment();
			}
			else {
				label = c.toString();
			}
		}
		return label;
	}

	/**
	 * tests if an OWLObject has been declared obsolete in the graph.
	 * 
	 * @param c
	 * @return boolean
	 */
	public boolean isObsolete(OWLObject c) {
		for (OWLOntology ont : getAllOntologies()) {
			for (OWLAnnotation ann : ((OWLEntity)c).getAnnotations(ont)) {
				if (ann.isDeprecatedIRIAnnotation()) {
					return true;
				}
			}
		}
		return false;
	}


	/**
	 * gets the value of rdfs:comment for an OWLObject
	 * 
	 * @param c
	 * @return comment of null
	 */
	public String getComment(OWLObject c) {
		OWLAnnotationProperty lap = getDataFactory().getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_COMMENT.getIRI()); 

		return getAnnotationValue(c, lap);
	}


	/**
	 * gets the value of rdfs:comment for an OWLObject
	 * <p>
	 * This is a curried FlexLoader s-expression version of {@link #getComment(OWLObject)}.
	 * 
	 * @param c
	 * @param sargs
	 * @return comment of null
	 * @see #getComment(OWLObject)
	 */
	public String getComment(OWLObject c, ArrayList<String> sargs) {
		OWLAnnotationProperty lap = getDataFactory().getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_COMMENT.getIRI()); 

		return getAnnotationValue(c, lap);
	}


	/**
	 * fetches the value of a single-valued annotation property for an OWLObject
	 * <p>
	 * TODO: provide a flag that determines behavior in the case of >1 value
	 * 
	 * @param c
	 * @param lap
	 * @return value
	 */
	public String getAnnotationValue(OWLObject c, OWLAnnotationProperty lap) {
		Set<OWLAnnotation>anns = new HashSet<OWLAnnotation>();
		if (c instanceof OWLEntity) {
			for (OWLOntology ont : getAllOntologies()) {
				// TODO : import closure
				anns.addAll(((OWLEntity) c).getAnnotations(ont,lap));
			}
		}
		else {
			return null;
		}
		for (OWLAnnotation a : anns) {
			if (a.getValue() instanceof OWLLiteral) {
				OWLLiteral val = (OWLLiteral) a.getValue();
				return val.getLiteral(); // return first - TODO - check zero or one
			}
		}
		return null;
	}

	/**
	 * gets the values of all annotation assertions to an OWLObject for a particular annotation property
	 * 
	 * @param c
	 * @param lap
	 * @return list of values or null
	 */
	public List<String> getAnnotationValues(OWLObject c, OWLAnnotationProperty lap) {
		Set<OWLAnnotation>anns = new HashSet<OWLAnnotation>();
		if (c instanceof OWLEntity) {
			for (OWLOntology ont : getAllOntologies()) {
				anns.addAll(((OWLEntity) c).getAnnotations(ont,lap));
			}
		}
		else {
			return null;
		}

		ArrayList<String> list = new ArrayList<String>();
		for (OWLAnnotation a : anns) {
			if (a.getValue() instanceof OWLLiteral) {
				OWLLiteral val = (OWLLiteral) a.getValue();
				list.add( val.getLiteral()); 
			}
			else if (a.getValue() instanceof IRI) {
				IRI val = (IRI)a.getValue();
				list.add( getIdentifier(val) ); 
			}

		}

		return list;
	}


	/**
	 * Gets the textual definition of an OWLObject
	 * <p>
	 * assumes zero or one def
	 * <p>
	 * It returns the definition text (encoded as def in obo format and IAO_0000115 annotation property in OWL format) of a class
	 * 
	 * @param c
	 * @return definition
	 */
	public String getDef(OWLObject c) {
		OWLAnnotationProperty lap = getDataFactory().getOWLAnnotationProperty(Obo2OWLVocabulary.IRI_IAO_0000115.getIRI()); 

		return getAnnotationValue(c, lap);
	}

	/**
	 * Gets the textual definition of an OWLObject
	 * <p>
	 * This is a curried FlexLoader s-expression version of {@link #getDef(OWLObject)}.
	 * 
	 * @param c
	 * @param sargs
	 * @return definition
	 * 
	 * @see #getDef(OWLObject)
	 */
	public String getDef(OWLObject c, ArrayList<String> sargs) {
		return getDef(c);
	}
	
	
	/**
	 * It returns the value of the is_metadata_tag tag.
	 * 
	 * @param c could OWLClass or OWLObjectProperty
	 * @return boolean
	 */
	public boolean getIsMetaTag(OWLObject c) {
		OWLAnnotationProperty lap = getAnnotationProperty(OboFormatTag.TAG_IS_METADATA_TAG.getTag());

		String val = getAnnotationValue(c, lap);

		return val == null ? false: Boolean.valueOf(val);
	}

	/**
	 * It returns the value of the subset tag.
	 * 
	 * @param c could OWLClass or OWLObjectProperty
	 * @return values
	 */
	// TODO - return set
	public List<String> getSubsets(OWLObject c) {
		//OWLAnnotationProperty lap = getAnnotationProperty(OboFormatTag.TAG_SUBSET.getTag());
		OWLAnnotationProperty lap = getDataFactory().getOWLAnnotationProperty(Obo2OWLVocabulary.IRI_OIO_inSubset.getIRI());
		return getAnnotationValues(c, lap);
	}

	/**
	 * It returns the value of the subset tag.
	 * <p>
	 * This is a curried FlexLoader s-expression version of {@link #getSubsets(OWLObject)}.
	 * 
	 * @param c could OWLClass or OWLObjectProperty
	 * @param sargs
	 * @return values
	 * @see #getSubsets(OWLObject)
	 */
	public List<String> getSubsets(OWLObject c, ArrayList<String> sargs) {
		return getSubsets(c);
	}

	/**
	 * fetches all subset names that are used
	 * 
	 * @return all subsets used in source ontology
	 */
	public Set<String> getAllUsedSubsets() {
		Set<String> subsets = new HashSet<String>();
		for (OWLObject x : getAllOWLObjects()) {
			subsets.addAll(getSubsets(x));
		}
		return subsets;
	}

	/**
	 * given a subset name, find all OWLObjects (typically OWLClasses) assigned to that subset
	 * 
	 * @param subset
	 * @return set of {@link OWLObject}
	 */
	public Set<OWLObject> getOWLObjectsInSubset(String subset) {
		Set<OWLObject> objs = new HashSet<OWLObject>();
		for (OWLObject x : getAllOWLObjects()) {
			if (getSubsets(x).contains(subset))
				objs.add(x);
		}
		return objs;		
	}

	/**
	 * given a subset name, find all OWLClasses assigned to that subset
	 * 
	 * @param subset
	 * @return set of {@link OWLClass}
	 */
	public Set<OWLClass> getOWLClassesInSubset(String subset) {
		Set<OWLClass> objs = new HashSet<OWLClass>();
		for (OWLObject x : getAllOWLObjects()) {
			if (getSubsets(x).contains(subset) && x instanceof OWLClass)
				objs.add((OWLClass) x);
		}
		return objs;		
	}


	/**
	 * It returns the value of the domain tag
	 * 
	 * @param prop
	 * @return domain string or null
	 */
	public String getDomain(OWLObjectProperty prop){
		Set<OWLClassExpression> domains = prop.getDomains(sourceOntology);

		for(OWLClassExpression ce: domains){
			return getIdentifier(ce);
		}

		return null;
	}


	/**
	 * It returns the value of the range tag
	 * 
	 * @param prop
	 * @return range or null
	 */
	public String getRange(OWLObjectProperty prop){
		Set<OWLClassExpression> domains = prop.getRanges(sourceOntology);

		for(OWLClassExpression ce: domains){
			return getIdentifier(ce);
		}

		return null;
	}


	/**
	 * It returns the values of the replaced_by tag or IAO_0100001 annotation.
	 * 
	 * @param c could OWLClass or OWLObjectProperty
	 * @return list of values
	 */
	public List<String> getReplacedBy(OWLObject c) {
		OWLAnnotationProperty lap = getDataFactory().getOWLAnnotationProperty(Obo2OWLVocabulary.IRI_IAO_0100001.getIRI());

		return getAnnotationValues(c, lap);
	}

	/**
	 * It returns the values of the consider tag.
	 * 
	 * @param c could OWLClass or OWLObjectProperty
	 * @return list of values
	 */
	public List<String> getConsider(OWLObject c) {
		OWLAnnotationProperty lap = getAnnotationProperty(OboFormatTag.TAG_CONSIDER.getTag());

		return getAnnotationValues(c, lap);
	}


	/**
	 * It returns the value of the is-obsolete tag.
	 * 
	 * @param c could OWLClass or OWLObjectProperty
	 * @return boolean
	 */
	public boolean getIsObsolete(OWLObject c) {
		OWLAnnotationProperty lap = getAnnotationProperty(OboFormatTag.TAG_IS_OBSELETE.getTag()); 

		String val = getAnnotationValue(c, lap);

		return val == null ? false: Boolean.valueOf(val);
	}

	/**
	 * It returns the value of the is-obsolete tag.
	 * <p>
	 * The odd signature is for use with FlexLoader.
	 * 
	 * @param c could OWLClass or OWLObjectProperty
	 * @param sargs (unsused)
	 * 
	 * @return String
	 */
	public String getIsObsoleteBinaryString(OWLObject c, ArrayList<String> sargs) {
		OWLAnnotationProperty lap = getAnnotationProperty(OboFormatTag.TAG_IS_OBSELETE.getTag()); 
		String val = getAnnotationValue(c, lap);
		return val == null ? "false": "true";
		//return val;
	}

	/**
	 * Get the annotation property value for a tag.
	 * 
	 * @see #getAnnotationPropertyValues(OWLObject c, String tag)
	 * @param c
	 * @param tag
	 * @return String
	 */
	public String getAnnotationPropertyValue(OWLObject c, String tag) {
		OWLAnnotationProperty lap = getAnnotationProperty(tag);
		return getAnnotationValue(c, lap);
	}

	
	/**
	 * Get the annotation property value for a tag.
	 * <p>
	 * This is a curried FlexLoader s-expression version of {@link #getAnnotationPropertyValue(OWLObject, String)}.
	 * <p>
	 * Currently, this function will only accept an argument of length 1.
	 * 
	 * @param c
	 * @param tags
	 * @return String
	 * @see #getAnnotationPropertyValues(OWLObject c, String tag)
	 */
	public String getAnnotationPropertyValue(OWLObject c, ArrayList<String> tags) {
		String retval = null;
		if( tags != null && tags.size() == 1 ){
			String tag = tags.get(0);
			retval = getAnnotationPropertyValue(c, tag);
			
		}
		return retval;
	}

	/**
	 * Get the annotation property values for a tag.
	 * 
	 * @see #getAnnotationPropertyValue(OWLObject c, String tag)
	 * @param c
	 * @param tag
	 * @return String List
	 */
	public List<String> getAnnotationPropertyValues(OWLObject c, String tag) {
		OWLAnnotationProperty lap = getAnnotationProperty(tag);
		return getAnnotationValues(c, lap);
	}

	/**
	 * Get the annotation property values for a tag.
	 * <p>
	 * This is a curried FlexLoader s-expression version of {@link #getAnnotationPropertyValues(OWLObject, String)}.
	 * <p>
	 * Currently, this function will only accept an argument of length 1.
	 * 
	 * @see #getAnnotationPropertyValues(OWLObject c, String tag)
	 * @param c
	 * @param tags
	 * @return String List
	 */
	public List<String> getAnnotationPropertyValues(OWLObject c, ArrayList<String> tags) {
		List<String> retvals = new ArrayList<String>();
		if( tags != null && tags.size() == 1 ){
			String tag = tags.get(0);
			retvals = getAnnotationPropertyValues(c, tag);			
		}
		return retvals;
	}

	/**
	 * It returns the values of the alt_id tag
	 * 
	 * @param c
	 * @return list of identifiers
	 */
	public List<String> getAltIds(OWLObject c) {
		OWLAnnotationProperty lap = getAnnotationProperty(OboFormatTag.TAG_ALT_ID.getTag());
		return getAnnotationValues(c, lap);
	}

	/**
	 * It returns the value of the builtin tag
	 * 
	 * @param c
	 * @return boolean
	 */
	@Deprecated
	public boolean getBuiltin(OWLObject c) {
		OWLAnnotationProperty lap = getAnnotationProperty(OboFormatTag.TAG_BUILTIN.getTag());

		String val = getAnnotationValue(c, lap);

		return val == null ? false: Boolean.valueOf(val);
	}

	/**
	 * It returns the value of the is_anonymous tag
	 * 
	 * @param c
	 * @return boolean
	 */
	@Deprecated
	public boolean getIsAnonymous(OWLObject c) {
		OWLAnnotationProperty lap = getAnnotationProperty(OboFormatTag.TAG_IS_ANONYMOUS.getTag());

		String val = getAnnotationValue(c, lap);

		return val == null ? false: Boolean.valueOf(val);
	}


	/**
	 * It translates a oboformat tag into an OWL annotation property
	 * 
	 * @param tag
	 * @return {@link OWLAnnotationProperty}
	 */
	public OWLAnnotationProperty getAnnotationProperty(String tag){
		return getDataFactory().getOWLAnnotationProperty(Obo2Owl.trTagToIRI(tag));
	}


	/**
	 * It returns the value of the OBO-namespace tag
	 * <p>
	 * Example: if the OWLObject is the GO class GO:0008150, this would return "biological_process"
	 * 
	 * @param c
	 * @return namespace
	 */
	public String getNamespace(OWLObject c) {
		OWLAnnotationProperty lap = getAnnotationProperty(OboFormatTag.TAG_NAMESPACE.getTag());

		return getAnnotationValue(c, lap);
	}

	
	/**
	 * It returns the value of the OBO-namespace tag.
	 * <p>
	 * This is a curried FlexLoader s-expression version of {@link #getNamespace(OWLObject)}.
	 * 
	 * @param c
	 * @param sargs
	 * @return namespace
	 * 
	 * @see #getNamespace(OWLObject)
	 */
	public String getNamespace(OWLObject c, ArrayList<String> sargs) {
		return getNamespace(c);
	}

	
	/**
	 * It returns the value of the created_by tag
	 * 
	 * @param c
	 * @return value or null
	 */
	public String getCreatedBy(OWLObject c) {
		OWLAnnotationProperty lap = getAnnotationProperty(OboFormatTag.TAG_CREATED_BY.getTag()); 

		return getAnnotationValue(c, lap);
	}


	/**
	 * It returns the value of the is_anti_symmetric tag or IAO_0000427 annotation
	 * 
	 * @param c
	 * @return boolean
	 */
	public boolean getIsAntiSymmetric(OWLObject c) {
		OWLAnnotationProperty lap = getDataFactory().getOWLAnnotationProperty(Obo2OWLVocabulary.IRI_IAO_0000427.getIRI());

		String val = getAnnotationValue(c, lap);

		return val == null ? false: Boolean.valueOf(val);
	}


	/**
	 * It returns the value of the is_cyclic tag
	 * 
	 * @param c
	 * @return boolean
	 */
	public boolean getIsCyclic(OWLObject c) {
		OWLAnnotationProperty lap = getAnnotationProperty(OboFormatTag.TAG_IS_CYCLIC.getTag()); 

		String val = getAnnotationValue(c, lap);

		return val == null ? false: Boolean.valueOf(val);
	}


	// TODO - fix for multiple ontologies
	/**
	 * true if c is transitive in the source ontology
	 * 
	 * @param c
	 * @return boolean
	 */
	public boolean getIsTransitive(OWLObjectProperty c) {
		Set<OWLTransitiveObjectPropertyAxiom> ax = sourceOntology.getTransitiveObjectPropertyAxioms(c);

		return ax.size()>0;
	}

	// TODO - fix for multiple ontologies
	/**
	 * true if c is functional in the source ontology
	 * 
	 * @param c
	 * @return boolean
	 */
	public boolean getIsFunctional(OWLObjectProperty c) {
		Set<OWLFunctionalObjectPropertyAxiom> ax = sourceOntology.getFunctionalObjectPropertyAxioms(c);

		return ax.size()>0;
	}

	// TODO - fix for multiple ontologies
	public boolean getIsInverseFunctional(OWLObjectProperty c) {
		Set<OWLInverseFunctionalObjectPropertyAxiom> ax = sourceOntology.getInverseFunctionalObjectPropertyAxioms(c);

		return ax.size()>0;
	}



	// TODO - fix for multiple ontologies
	public boolean getIsReflexive(OWLObjectProperty c) {
		Set<OWLReflexiveObjectPropertyAxiom> ax = sourceOntology.getReflexiveObjectPropertyAxioms(c);

		return ax.size()>0;
	}

	// TODO - fix for multiple ontologies
	public boolean getIsSymmetric(OWLObjectProperty c) {
		Set<OWLSymmetricObjectPropertyAxiom> ax = sourceOntology.getSymmetricObjectPropertyAxioms(c);

		return ax.size()>0;
	}

	/**
	 * get the values of of the obo xref tag
	 * 
	 * @param c
	 * @return It returns null if no xref annotation is found.
	 */

	public List<String> getXref(OWLObject c){
		OWLAnnotationProperty lap = getAnnotationProperty(OboFormatTag.TAG_XREF.getTag());

		Set<OWLAnnotation>anns = null;
		if (c instanceof OWLEntity) {
			anns = ((OWLEntity) c).getAnnotations(sourceOntology,lap);
		}
		else {
			return null;
		}
		List<String> list = new ArrayList<String>();
		for (OWLAnnotation a : anns) {

			if (a.getValue() instanceof OWLLiteral) {
				OWLLiteral val = (OWLLiteral) a.getValue();
				list.add( val.getLiteral()) ;
			}
		}
		return list;
	}

	/**
	 * get the values of of the obo xref tag
	 * <p>
	 * This is a curried FlexLoader s-expression version of {@link #getXref(OWLObject)}.
	 * 
	 * @param c
	 * @param sargs
	 * @return It returns null if no xref annotation is found.
	 * @see #getXref(OWLObject)
	 */
	public List<String> getXref(OWLObject c, ArrayList<String> sargs){
		return getXref(c);
	}

	/**
	 * Get the definition xrefs (IAO_0000115)
	 * 
	 * @param c
	 * @return list of definition xrefs
	 */
	public List<String> getDefXref(OWLObject c){
		OWLAnnotationProperty lap = getDataFactory().getOWLAnnotationProperty(Obo2OWLVocabulary.IRI_IAO_0000115.getIRI()); 
		OWLAnnotationProperty xap = getAnnotationProperty(OboFormatTag.TAG_XREF.getTag());
		
		List<String> list = new ArrayList<String>();

		if(c instanceof OWLEntity){
			for (OWLAnnotationAssertionAxiom oaax :((OWLEntity) c).getAnnotationAssertionAxioms(sourceOntology)){

				if(lap.equals(oaax.getProperty())){

					for(OWLAnnotation a: oaax.getAnnotations(xap)){
						if(a.getValue() instanceof OWLLiteral){
							list.add( ((OWLLiteral)a.getValue()).getLiteral() );
						}
					}
				}

			}
		}

		return list;
	}

	
	/**
	 * Get the definition xrefs (IAO_0000115)
	 * <p>
	 * This is a curried FlexLoader s-expression version of {@link #getDefXref(OWLObject)}.
	 * 
	 * @param c
	 * @param sargs
	 * @return list of definition xrefs
	 * @see #getDefXref(OWLObject)
	 */
	public List<String> getDefXref(OWLObject c, ArrayList<String> sargs){
		return getDefXref(c);
	}

	
	/**
	 * gets the OBO-style ID of the specified object. E.g. "GO:0008150"
	 * 
	 * @param owlObject
	 * @return OBO-style identifier, using obo2owl mapping
	 */
	public String getIdentifier(OWLObject owlObject) {
		return Owl2Obo.getIdentifierFromObject(owlObject, this.sourceOntology, null);
	}

	/**
	 * Same as {@link #getIdentifier(OWLObject)} but a different profile to support the FlexLoader.
	 * <p>
	 * The s-expressions arguments go unused.
	 * 
	 * @param owlObject
	 * @param sargs (unused)
	 * @return OBO-style identifier, using obo2owl mapping
	 * 
	 * @see #getIdentifier(OWLObject)
	 */
	public String getIdentifier(OWLObject owlObject, ArrayList<String> sargs) {
		return getIdentifier(owlObject);
	}


	/**
	 * gets the OBO-style ID of the specified object. E.g. "GO:0008150"
	 * 
	 * @param iriId
	 * @return OBO-style identifier, using obo2owl mapping
	 */
	public String getIdentifier(IRI iriId) {
		return Owl2Obo.getIdentifier(iriId);
	}

	public IRI getIRIByIdentifier(String id) {
		
		// special magic for finding IRIs from a non-standard identifier
		// This is the case for relations (OWLObject properties) with a short hand
		// or for relations with a non identifiers with-out a colon, e.g. negative_regulation
		if (!id.contains(":")) {
			final OWLAnnotationProperty shortHand = getDataFactory().getOWLAnnotationProperty(Obo2OWLVocabulary.IRI_OIO_shorthand.getIRI());
			final OWLAnnotationProperty oboIdInOwl = getDataFactory().getOWLAnnotationProperty(Obo2Owl.trTagToIRI(OboFormatTag.TAG_ID.getTag()));
			for (OWLOntology o : getAllOntologies()) {
				for(OWLObjectProperty p : o.getObjectPropertiesInSignature()) {
					// check for short hand or obo ID in owl
					Set<OWLAnnotation> annotations = p.getAnnotations(o);
					if (annotations != null) {
						for (OWLAnnotation owlAnnotation : annotations) {
							OWLAnnotationProperty property = owlAnnotation.getProperty();
							if ((shortHand != null && shortHand.equals(property)) 
									|| (oboIdInOwl != null && oboIdInOwl.equals(property)))
							{
								OWLAnnotationValue value = owlAnnotation.getValue();
								if (value != null && value instanceof OWLLiteral) {
									OWLLiteral literal = (OWLLiteral) value;
									String shortHandLabel = literal.getLiteral();
									if (id.equals(shortHandLabel)) {
										return p.getIRI();
									}
								}
							}
						}
					}
				}
			}
		}
		
		
		// otherwise use the obo2owl method
		Obo2Owl b = new Obo2Owl();
		b.setObodoc(new OBODoc());
		return b.oboIdToIRI(id);
	}

	/**
	 * Given an OBO-style ID, return the corresponding OWLObject, if it is declared - otherwise null
	 * 
	 * @param id - e.g. GO:0008150
	 * @return object with id or null
	 */
	public OWLObject getOWLObjectByIdentifier(String id) {
		IRI iri = getIRIByIdentifier(id);
		if (iri != null)
			return getOWLObject(iri);
		return null;
	}

	/**
	 * Given an OBO-style ID, return the corresponding OWLObjectProperty, if it is declared - otherwise null
	 * 
	 * @param id - e.g. GO:0008150
	 * @return OWLObjectProperty with id or null
	 */
	public OWLObjectProperty getOWLObjectPropertyByIdentifier(String id) {
		IRI iri = getIRIByIdentifier(id);
		if (iri != null) 
			return getOWLObjectProperty(iri);
		return null;
	}

	/**
	 * Given an OBO-style ID, return the corresponding OWLNamedIndividual, if it is declared - otherwise null
	 * 
	 * @param id - e.g. GO:0008150
	 * @return OWLNamedIndividual with id or null
	 */
	public OWLNamedIndividual getOWLIndividualByIdentifier(String id) {
		IRI iri = getIRIByIdentifier(id);
		if (iri != null) 
			return getOWLIndividual(iri);
		return null;
	}

	/**
	 * Given an OBO-style ID, return the corresponding OWLClass, if it is declared - otherwise null
	 * 
	 * @param id - e.g. GO:0008150
	 * @return OWLClass with id or null
	 */
	public OWLClass getOWLClassByIdentifier(String id) {
		IRI iri = getIRIByIdentifier(id);
		if (iri != null)
			return getOWLClass(iri);
		return null;
	}

	/**
	 * fetches an OWL Object by rdfs:label
	 * <p>
	 * if there is >1 match, return the first one encountered
	 * 
	 * @param label
	 * @return object or null
	 */
	public OWLObject getOWLObjectByLabel(String label) {
		IRI iri = getIRIByLabel(label);
		if (iri != null)
			return getOWLObject(iri);
		return null;
	}

	/**
	 * fetches an OWL IRI by rdfs:label
	 * 
	 * @param label
	 * @return IRI or null
	 */
	public IRI getIRIByLabel(String label) {
		try {
			return getIRIByLabel(label, false);
		} catch (SharedLabelException e) {
			// note that it should be impossible to reach this point
			// if getIRIByLabel is called with isEnforceUnivocal = false
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * fetches an OWL IRI by rdfs:label, optionally testing for uniqueness
	 * <p>
	 * TODO: index labels. This currently scans all labels in the ontology, which is expensive
	 * 
	 * @param label
	 * @param isEnforceUnivocal
	 * @return IRI or null
	 * @throws SharedLabelException if >1 IRI shares input label
	 */
	public IRI getIRIByLabel(String label, boolean isEnforceUnivocal) throws SharedLabelException {
		IRI iri = null;
		for (OWLOntology o : getAllOntologies()) {
			Set<OWLAnnotationAssertionAxiom> aas = o.getAxioms(AxiomType.ANNOTATION_ASSERTION);
			for (OWLAnnotationAssertionAxiom aa : aas) {
				OWLAnnotationValue v = aa.getValue();
				OWLAnnotationProperty property = aa.getProperty();
				if (property.isLabel() && v instanceof OWLLiteral) {
					if (label.equals( ((OWLLiteral)v).getLiteral())) {
						OWLAnnotationSubject subject = aa.getSubject();
						if (subject instanceof IRI) {
							if (isEnforceUnivocal) {
								if (iri != null && !iri.equals((IRI)subject)) {
									throw new SharedLabelException(label,iri,(IRI)subject);
								}
								iri = (IRI)subject;
							}
							else {
								return (IRI)subject;
							}
						}
						else {
							//return null;
						}
					}
				}
			}
		}
		return iri;
	}

	/**
	 * Find the corresponding {@link OWLObject} for a given OBO-style alternate identifier.
	 * <p>
	 * WARNING: This methods scans all object annotations in all ontologies. 
	 * This is an expensive method.
	 * <p>
	 * If there are multiple altIds use {@link #getOWLObjectsByAltId(Set)} for more efficient retrieval.
	 * Also consider loading all altId-mappings using {@link #getAllOWLObjectsByAltId()}.
	 * 
	 * @param altIds
	 * @return {@link OWLObject} or null
	 * 
	 * @see #getOWLObjectsByAltId(Set)
	 * @see #getAllOWLObjectsByAltId()
	 */
	public OWLObject getOWLObjectByAltId(String altIds) {
		Map<String, OWLObject> map = getOWLObjectsByAltId(Collections.singleton(altIds));
		return map.get(altIds);
	}

	/**
	 * Find the corresponding {@link OWLObject}s for a given set of OBO-style alternate identifiers.
	 * <p>
	 * WARNING: This methods scans all object annotations in all ontologies. 
	 * This is an expensive method.
	 * <p>
	 * Consider loading all altId-mappings using {@link #getAllOWLObjectsByAltId()}.
	 * 
	 * @param altIds
	 * @return map of altId to OWLObject (never null)
	 * @see #getAllOWLObjectsByAltId()
	 */
	public Map<String, OWLObject> getOWLObjectsByAltId(Set<String> altIds) {
		final Map<String, OWLObject> results = new HashMap<String, OWLObject>();
		final OWLAnnotationProperty altIdProperty = getAnnotationProperty(OboFormatTag.TAG_ALT_ID.getTag());
		if (altIdProperty == null) {
			return Collections.emptyMap();
		}
		for (OWLOntology o : getAllOntologies()) {
			Set<OWLAnnotationAssertionAxiom> aas = o.getAxioms(AxiomType.ANNOTATION_ASSERTION);
			for (OWLAnnotationAssertionAxiom aa : aas) {
				OWLAnnotationValue v = aa.getValue();
				OWLAnnotationProperty property = aa.getProperty();
				if (altIdProperty.equals(property) && v instanceof OWLLiteral) {
					String altId = ((OWLLiteral)v).getLiteral();
					if (altIds.contains(altId)) {
						OWLAnnotationSubject subject = aa.getSubject();
						if (subject instanceof IRI) {
							OWLObject obj = getOWLObject((IRI) subject);
							if (obj != null) {
								results.put(altId, obj);
							}
						}
					}
				}
			}
		}
		return results;
	}

	/**
	 * Find all corresponding {@link OWLObject}s with an OBO-style alternate identifier.
	 * <p>
	 * WARNING: This methods scans all object annotations in all ontologies. 
	 * This is an expensive method.
	 * 
	 * @return map of altId to OWLObject (never null)
	 */
	public Map<String, OWLObject> getAllOWLObjectsByAltId() {
		final Map<String, OWLObject> results = new HashMap<String, OWLObject>();
		final OWLAnnotationProperty altIdProperty = getAnnotationProperty(OboFormatTag.TAG_ALT_ID.getTag());
		if (altIdProperty == null) {
			return Collections.emptyMap();
		}
		for (OWLOntology o : getAllOntologies()) {
			Set<OWLAnnotationAssertionAxiom> aas = o.getAxioms(AxiomType.ANNOTATION_ASSERTION);
			for (OWLAnnotationAssertionAxiom aa : aas) {
				OWLAnnotationValue v = aa.getValue();
				OWLAnnotationProperty property = aa.getProperty();
				if (altIdProperty.equals(property) && v instanceof OWLLiteral) {
					String altId = ((OWLLiteral)v).getLiteral();
					OWLAnnotationSubject subject = aa.getSubject();
					if (subject instanceof IRI) {
						OWLObject obj = getOWLObject((IRI) subject);
						if (obj != null) {
							results.put(altId, obj);
						}
					}
				}
			}
		}
		return results;
	}
	
	/**
	 * Returns an OWLClass given an IRI string
	 * <p>
	 * the class must be declared in either the source ontology, or in a support ontology,
	 * otherwise null is returned
	 * 
	 * @param s IRI string
	 * @return {@link OWLClass}
	 */
	public OWLClass getOWLClass(String s) {
		IRI iri = IRI.create(s);
		return getOWLClass(iri);
	}

	/**
	 * Returns an OWLClass given an IRI
	 * <p>
	 * the class must be declared in either the source ontology, or in a support ontology,
	 * otherwise null is returned
	 *
	 * @param iri
	 * @return {@link OWLClass}
	 */
	public OWLClass getOWLClass(IRI iri) {
		OWLClass c = getDataFactory().getOWLClass(iri);
		for (OWLOntology o : getAllOntologies()) {
			if (o.getDeclarationAxioms(c).size() > 0) {
				return c;
			}
		}
		return null;
	}

	/**
	 * @param x
	 * @return {@link OWLClass}
	 */
	public OWLClass getOWLClass(OWLObject x) {
		IRI iri;
		if (x instanceof IRI) {
			iri = (IRI)x;
		}
		else if (x instanceof OWLNamedObject) {
			iri = ((OWLNamedObject)x).getIRI();
		}
		else {
			return null;
		}
		return getDataFactory().getOWLClass(iri);
	}


	/**
	 * Returns an OWLNamedIndividual with this IRI <b>if it has been declared</b>
	 * in the source or support ontologies. Returns null otherwise.
	 * 
	 * @param iri
	 * @return {@link OWLNamedIndividual}
	 */
	public OWLNamedIndividual getOWLIndividual(IRI iri) {
		OWLNamedIndividual c = getDataFactory().getOWLNamedIndividual(iri);
		for (OWLOntology o : getAllOntologies()) {
			for (OWLDeclarationAxiom da : o.getDeclarationAxioms(c)) {
				if (da.getEntity() instanceof OWLNamedIndividual) {
					return (OWLNamedIndividual) da.getEntity();
				}
			}
		}
		return null;
	}

	/**
	 * @see #getOWLIndividual(IRI)
	 * @param s
	 * @return {@link OWLNamedIndividual}
	 */
	public OWLNamedIndividual getOWLIndividual(String s) {
		IRI iri = IRI.create(s);
		return getOWLIndividual(iri);
	}

	/**
	 * Returns the OWLObjectProperty with this IRI
	 * <p>
	 * Must have been declared in one of the ontologies
	 * 
	 * @param iri
	 * @return {@link OWLObjectProperty}
	 */
	public OWLObjectProperty getOWLObjectProperty(String iri) {
		return getOWLObjectProperty(IRI.create(iri));
	}

	public OWLObjectProperty getOWLObjectProperty(IRI iri) {
		OWLObjectProperty p = getDataFactory().getOWLObjectProperty(iri);
		for (OWLOntology o : getAllOntologies()) {
			if (o.getDeclarationAxioms(p).size() > 0) {
				return p;
			}
		}
		return null;
	}

	public OWLAnnotationProperty getOWLAnnotationProperty(IRI iri) {
		OWLAnnotationProperty p = getDataFactory().getOWLAnnotationProperty(iri);
		for (OWLOntology o : getAllOntologies()) {
			if (o.getDeclarationAxioms(p).size() > 0) {
				return p;
			}
		}
		return null;
	}



	/**
	 * Returns the OWLObject with this IRI
	 * (where IRI is specified as a string - e.g http://purl.obolibrary.org/obo/GO_0008150)
	 * 
	 * @param s IRI string
	 * @see #getOWLObject(IRI iri)
	 * @return {@link OWLObject}
	 */
	public OWLObject getOWLObject(String s) {
		return getOWLObject(IRI.create(s));
	}

	/**
	 * Returns the OWLObject with this IRI
	 * <p>
	 * Must have been declared in one of the ontologies
	 * <p>
	 * Currently OWLObject must be one of OWLClass, OWLObjectProperty or OWLNamedIndividual
	 * <p>
	 * If the ontology employs punning and there different entities with the same IRI, then
	 * the order of precedence is OWLClass then OWLObjectProperty then OWLNamedIndividual
	 *
	 * @param s entity IRI
	 * @return {@link OWLObject}
	 */
	public OWLObject getOWLObject(IRI s) {
		OWLObject o;
		o = getOWLClass(s);
		if (o == null) {
			o = getOWLIndividual(s);
		}
		if (o == null) {
			o = getOWLObjectProperty(s);
		}
		if (o == null) {
			o = getOWLAnnotationProperty(s);
		}
		return o;
	}

	/**
	 * gets the OBO-style ID of the source ontology IRI. E.g. "go"
	 * 
	 * @return id of source ontology
	 */
	public String getOntologyId(){
		return Owl2Obo.getOntologyId(this.getSourceOntology());
	}

}

