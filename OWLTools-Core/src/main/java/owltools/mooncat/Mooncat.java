package owltools.mooncat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.obolibrary.obo2owl.Obo2OWLConstants;
import org.obolibrary.obo2owl.Obo2OWLConstants.Obo2OWLVocabulary;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationSubject;
import org.semanticweb.owlapi.model.OWLAnnotationSubjectVisitor;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLAnonymousIndividual;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubAnnotationPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.RemoveAxiom;
import org.semanticweb.owlapi.model.RemoveOntologyAnnotation;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.util.OWLObjectVisitorAdapter;
import org.semanticweb.owlapi.vocab.OWL2Datatype;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;

import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;
import owltools.graph.OWLQuantifiedProperty;
import owltools.io.ParserWrapper;

/**
 * Given one source ontology referencing one or more referenced ontologies
 * (e.g. CL referencing PRO, GO, CHEBI, UBERON), merge/copy a
 * subset of axioms from the referenced ontologies into the source ontology.
 * 
 * This relies on a {@link OWLGraphWrapper} object being created, in which
 * the the source ontology is the primary ontology of interest, and the
 * support ontologies are the set of ontologies from which references are
 * drawn. For example, src=CL, sup={PRO,GO,CHEBI,UBERON}
 * 
 * The {@link owltools.graph} algorithm is used to find the reference closure -
 * i.e. all classes in the support ontologies referenced in the main ontology, 
 * together with their ancestors over subclass, equivalence and someValuesFrom.
 * 
 * As a first step, previously merged classes are removed. These are marked
 * out by an annotation assertion using IAO_0000412. Any classes merged in
 * get this assigned automatically.
 * 
 * In the future, owl imports will be supported.
 * 
 * @author cjm
 *
 */
public class Mooncat {

	private final static Logger LOG = Logger.getLogger(Mooncat.class);

	// TODO move this to some constants class or enum
	private static final IRI importedMarkerIRI = IRI.create(Obo2OWLConstants.DEFAULT_IRI_PREFIX+"IAO_0000412");

	OWLOntologyManager manager;
	OWLDataFactory dataFactory;
	//OWLOntology ontology; // delegate instead
	Set<OWLOntology> referencedOntologies = new HashSet<OWLOntology>();
	Set<OWLOntology> allOntologies = null;
	OWLGraphWrapper graph;
	Set<String> sourceOntologyPrefixes = null;

	public Mooncat(OWLOntologyManager manager, OWLDataFactory dataFactory,
			OWLOntology ontology) {
		super();
		this.manager = manager;
		this.dataFactory = dataFactory;
		//this.ontology = ontology;
	}



	public Mooncat(OWLGraphWrapper g) {
		super();
		//this.ontology = g.getSourceOntology();
		this.referencedOntologies = g.getSupportOntologySet();
		ParserWrapper pw = new ParserWrapper(); 
		this.manager = pw.getManager();
		this.dataFactory = manager.getOWLDataFactory();
		this.graph = g;
	}



	/**
	 * E.g. http://purl.obolibrary.org/obo/go_
	 * @return set of prefixes
	 */
	public Set<String> getSourceOntologyPrefixes() {
		return sourceOntologyPrefixes;
	}

	/**
	 * Set the source ontology prefixes.
	 * 
	 * @param sourceOntologyPrefixes
	 */
	public void setSourceOntologyPrefixes(Set<String> sourceOntologyPrefixes) {
		this.sourceOntologyPrefixes = sourceOntologyPrefixes;
	}

	/**
	 * Add a source ontology prefix to the internal set.
	 * 
	 * @param prefix
	 */
	public void addSourceOntologyPrefix(String prefix) {
		if (sourceOntologyPrefixes == null) 
			sourceOntologyPrefixes = new HashSet<String>();
		sourceOntologyPrefixes.add(prefix);
	}


	/**
	 * delegates to OWLGraphWrapper support ontologies
	 * 
	 * @return all support ontologies
	 */
	public Set<OWLOntology> getReferencedOntologies() {
		return graph.getSupportOntologySet();
	}

	/**
	 * @return union of referenced ontologies and source ontology
	 */
	public Set<OWLOntology> getAllOntologies() {
		return graph.getAllOntologies();
	}

	@Deprecated
	public void setReferencedOntologies(Set<OWLOntology> referencedOntologies) {
		this.referencedOntologies = referencedOntologies;
	}

	/**
	 * 
	 * @param refOnt
	 * @throws OWLOntologyCreationException 
	 */
	public void addReferencedOntology(OWLOntology refOnt) throws OWLOntologyCreationException {
		// TODO - imports
		graph.addSupportOntology(refOnt);
	}

	public void mergeIntoReferenceOntology(OWLOntology ont) throws OWLOntologyCreationException { 
		LOG.info("Merging "+ont+" into reference ontology");
		graph.mergeOntology(ont);
	}


	public OWLOntologyManager getManager() {
		// TODO - delegate?
		return manager;
	}

	public void setManager(OWLOntologyManager manager) {
		// TODO - delegate?
		this.manager = manager;
	}



	public OWLGraphWrapper getGraph() {
		return graph;
	}



	public void setGraph(OWLGraphWrapper graph) {
		this.graph = graph;
	}



	/**
	 * @return source ontology (delegated to OWLGraphWrapper)
	 */
	public OWLOntology getOntology() {
		return graph.getSourceOntology();
	}

	public void setOntology(OWLOntology ontology) {
		//this.ontology = ontology;
		graph.setSourceOntology(ontology);
	}

	@Deprecated
	public void addImport(String importedIRIString) {
		OWLImportsDeclaration iax = dataFactory.getOWLImportsDeclaration(IRI.create(importedIRIString));
		//AddImport addAx = new AddImport(ontology, iax);
		AddImport addAx = new AddImport(getOntology(), iax);
		manager.applyChange(addAx);
	}

	// ----------------------------
	// CORE METHODS
	// ----------------------------

	/**
	 * 
	 * merge minimal subset of referenced ontologies into the source ontology.
	 * 
	 * This is the main entry method for this class.
	 * 
	 * finds all external ontology axioms required to make a sub-ontology such that the source is
	 * "graph-complete", then add these axioms to source ontology
	 */
	public void mergeOntologies() {
		// remove directives which only make sense, while working in non-merged ontologies 
		removeDirectives();
		
		// refresh existing MIREOT set
		LOG.info("Removing entities marked with IAO_0000412 (imported_from) -- will not remove dangling classes at this stage");
		removeExternalOntologyClasses(false);

		OWLOntology srcOnt = graph.getSourceOntology();
		LOG.info("Getting all referenced objects, finding their graph closure, and all axioms that mention entities in this closure");
		Set<OWLAxiom> axioms = getClosureAxiomsOfExternalReferencedEntities();

		// add ALL subannotprop axioms
		addSubAnnotationProperties(axioms);

		for (OWLAxiom a : axioms) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Adding:" + a);
			}
		}
		manager.addAxioms(srcOnt, axioms);
	}



	private void removeDirectives() {
		// TODO decide: move the set of directives into a constant/static collection?
		
		Set<IRI> directivesIRIs = new HashSet<IRI>();
		directivesIRIs.add(Obo2OWLVocabulary.IRI_OIO_LogicalDefinitionViewRelation.getIRI());
		directivesIRIs.add(Obo2OWLVocabulary.IRI_OIO_treatXrefsAsEquivalent.getIRI());
		directivesIRIs.add(Obo2OWLVocabulary.IRI_OIO_treatXrefsAsGenusDifferentia.getIRI());
		directivesIRIs.add(Obo2OWLVocabulary.IRI_OIO_treatXrefsAsHasSubClass.getIRI());
		directivesIRIs.add(Obo2OWLVocabulary.IRI_OIO_treatXrefsAsIsA.getIRI());
		directivesIRIs.add(Obo2OWLVocabulary.IRI_OIO_treatXrefsAsRelationship.getIRI());
		directivesIRIs.add(Obo2OWLVocabulary.IRI_OIO_treatXrefsAsReverseGenusDifferentia.getIRI());
		
		OWLOntology o = graph.getSourceOntology();
		for(OWLAnnotation ann : o.getAnnotations()) {
			final OWLAnnotationProperty property = ann.getProperty();
			if (directivesIRIs.contains(property.getIRI())) {
				manager.applyChange(new RemoveOntologyAnnotation(o, ann));
			}
		}
	}



	void addSubAnnotationProperties(Set<OWLAxiom> axioms) {
		// add ALL subannotprop axioms
		// - this is quite geared towards obo ontologies, where
		//   we want to preserve obo headers.
		// TODO: make this configurable
		LOG.info("adding SubAnnotationProperties");
		Set<OWLAxiom> sapAxioms = new HashSet<OWLAxiom>();
		for (OWLOntology refOnt : this.getReferencedOntologies()) {
			for (OWLSubAnnotationPropertyOfAxiom a : refOnt.getAxioms(AxiomType.SUB_ANNOTATION_PROPERTY_OF)) {
				sapAxioms.add(a);
				Set<OWLAnnotationAssertionAxiom> s = refOnt.getAnnotationAssertionAxioms(a.getSubProperty().getIRI());
				if (s != null && !s.isEmpty()) {
					for (OWLAnnotationAssertionAxiom owlAnnotationAssertionAxiom : s) {
						sapAxioms.add(owlAnnotationAssertionAxiom);
					}
				}
			}
		}
		axioms.addAll(sapAxioms);
	}

	// If sourceOntologyPrefixes is set, this this test succeeds or fails depending on whether obj.IRI starts with one of these prefixes;
	// otherwise, obj is external if it is declared in a referencedontology
	private boolean isInExternalOntology(OWLEntity obj) {
		if (sourceOntologyPrefixes != null && sourceOntologyPrefixes.size() > 0) {
			//LOG.info("  prefixes: "+sourceOntologyPrefixes);
			String iri = obj.getIRI().toString();
			boolean isSrc = false;
			for (String prefix : sourceOntologyPrefixes) {
				if (iri.startsWith(prefix)) {
					isSrc = true;
					break;
				}
			}
			if (!isSrc) {
				LOG.info("  refObj: "+obj+" // "+sourceOntologyPrefixes);
				return true;
			}
		}
		else {
			// estimate by ref ontologies
			// TODO: this is not reliable
			for (OWLOntology refOnt : getReferencedOntologies()) {
				//LOG.info("  refOnt: "+refOnt);
				if (refOnt.getDeclarationAxioms(obj).size() > 0) {
					if (LOG.isDebugEnabled()) {
						LOG.debug("  refObj: " + obj);
					}
					return true;
				}
			}
		}
		return false;
	}


	/**
	 * 
	 * returns set of entities that belong to a referenced ontology that are referenced in the source ontology.
	 * 
	 * If the source ontology is not explicitly declared, then all entities that are referenced in the source
	 * ontology and declared in a reference ontology are returned.
	 * 
	 * Example: if the source ontology is cl, and cl contains axioms that reference go:1, go:2, ...
	 * and go is in the set of referenced ontologies, then {go:1,go:2,...} will be in the returned set.
	 * It is irrelevant whether go:1, ... is declared in the source (e.g. MIREOTed)
	 * 
	 * Note this only returns direct references. See
	 * {@link #getClosureOfExternalReferencedEntities()} for closure of references
	 * 
	 * @return all objects referenced by source ontology
	 */
	public Set<OWLEntity> getExternalReferencedEntities() {
		OWLOntology ont = graph.getSourceOntology();
		Set<OWLEntity> objs = ont.getSignature(Imports.EXCLUDED);
		Set<OWLEntity> refObjs = new HashSet<OWLEntity>();
		LOG.info("testing "+objs.size()+" objs to see if they are contained in: "+getReferencedOntologies());
		for (OWLEntity obj : objs) {
			//LOG.info("considering: "+obj);
			// a reference ontology may have entities from the source ontology MIREOTed in..
			// allow a configuration with the URI prefix specified
			if (isInExternalOntology(obj)) {
				refObjs.add(obj);

			}
		}
		LOG.info("#refObjs: "+refObjs.size());

		return refObjs;
	}

	/**
	 * finds the full closure of all external referenced entities.
	 * TODO: include object properties
	 * 
	 * calls {@link #getExternalReferencedEntities()} and then finds all reflexive ancestors of this set.
	 * 
	 * to configure the traversal, see {@link OWLGraphWrapper}
	 * 
	 * @return closure of all external referenced entities
	 */
	public Set<OWLObject> getClosureOfExternalReferencedEntities() {
		// the closure set, to be returned
		Set<OWLObject> objs = new HashSet<OWLObject>();

		// set of entities in external ontologies referenced in source ontology
		Set<OWLEntity> refs = getExternalReferencedEntities();
		LOG.info("direct external referenced entities: "+refs.size());

		// build the closure
		for (OWLEntity ref : refs) {
			// todo - allow per-relation control
			// todo - make more efficient, allow passing of set of entities
			// todo - include object properties
			Set<OWLObject> ancs = graph.getAncestorsReflexive(ref);
			objs.addAll(ancs);
		}
		LOG.info("closure of direct external referenced entities: "+objs.size());

		// extraObjs is the set of properties (annotation and object)
		// that are in the signatures of all referencing axioms
		Set<OWLObject> extraObjs = new HashSet<OWLObject>();
		for (OWLObject obj : objs) {
			if (obj instanceof OWLEntity) {
				for (OWLOntology refOnt : this.getReferencedOntologies()) {
					for (OWLAnnotationAssertionAxiom aaa : refOnt.getAnnotationAssertionAxioms(((OWLEntity)obj).getIRI())) {
						extraObjs.add(aaa.getProperty());
						extraObjs.add(aaa.getValue());
					}
					
					for (OWLAxiom ax : refOnt.getReferencingAxioms((OWLEntity)obj)) {
						extraObjs.addAll(ax.getObjectPropertiesInSignature());
					}
				}
			}
		}
		objs.addAll(extraObjs);
		return objs;
	}

	/**
	 * find all axioms in closure of external referenced entities.
	 * 
	 * Steps:
	 * <ul>
	 *  <li> find all referenced entities and their closure
	 *  <li> find all axioms about these entities
	 *  <li> filter these axioms such that the classes in the signature are in the subset
	 * </ul>
	 * 
	 * The first step is carried out by {@link #getClosureOfExternalReferencedEntities()}
	 * The second and third steps by {@link #getAxiomsForSubset(Set)}
	 * 
	 * Example: if the source is cl, and cl references go classes, and go is in the set of referenced 
	 * ontologies, then this will return a collection of axioms constituting a sub-ontology of go
	 * such that the graph closure of cl is complete.
	 * 
	 * @return axioms for sub-ontology
	 */
	public Set<OWLAxiom> getClosureAxiomsOfExternalReferencedEntities() {
		Set<OWLObject> objs = getClosureOfExternalReferencedEntities();
		return getAxiomsForSubset(objs);
	}

	/**
	 * Given a set of objects (e.g. a GO slim), find axioms constituting sub-ontology - i.e. "slimmed down" axioms
	 * 
	 * Steps:
	 * <ul>
	 *  <li> for each class in subset, add axioms that are about this class
	 *  <li> filter these axioms such that all the classes in the axiom signature are in the subset
	 * </ul>
	 * 
	 * For example, if O contains the axiom [A SubClassOf R some B] and A &isin; S and B &notin; S,
	 * then this axiom will *not* be added for the set S
	 * 
	 * Currently, all annotation assertions about an object are added by default
	 * 
	 * 
	 * @param objs
	 * @return axioms in subontology
	 */
	public Set<OWLAxiom> getAxiomsForSubset(Set<OWLObject> objs) {
		Set<OWLAxiom> finalAxioms = new HashSet<OWLAxiom>();
		LOG.info("inputObjs:"+objs.size());
		// first get a set of all candidate axioms;
		// we will later filter these
		for (OWLOntology refOnt : getReferencedOntologies()) {
			LOG.info("refOnt:"+refOnt);
			IRI refOntIRI = null;
			Optional<IRI> ontologyIRI = refOnt.getOntologyID().getOntologyIRI();
			if (ontologyIRI.isPresent()) {
				refOntIRI = ontologyIRI.get();
			}
			Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
			for (OWLObject obj : objs) {
				if (!(obj instanceof OWLEntity))
					continue;

				if (belongsToSource((OWLEntity) obj)) {
					LOG.info(obj+" declared in source; source axioms will take priority");
					continue;
				}

				// explicitly add entity declarations
				if (obj instanceof OWLEntity) {
					OWLDeclarationAxiom da = dataFactory.getOWLDeclarationAxiom((OWLEntity)obj);
					axioms.add(da);
				}

				if (obj instanceof OWLClass) {
					if (LOG.isDebugEnabled()) {
						LOG.debug("class:" + obj);
					}
					// includes SubClassOf(obj,?), disjoints, equivalents, ..
					final OWLClass c = (OWLClass) obj;
					axioms.addAll(refOnt.getAxioms(c, Imports.EXCLUDED));
					Set<? extends OWLAxiom> declarationAxioms = refOnt.getDeclarationAxioms(c);
					if (!declarationAxioms.isEmpty()) {
						if(refOntIRI != null) {
							OWLAnnotationProperty property = dataFactory.getOWLAnnotationProperty(importedMarkerIRI);
							axioms.add(dataFactory.getOWLAnnotationAssertionAxiom(property , c.getIRI(), refOntIRI));
						}
						axioms.addAll(declarationAxioms);
					}
				}
				else if (obj instanceof OWLObjectProperty) {
					final OWLObjectProperty p = (OWLObjectProperty) obj;
					if (!refOnt.getDeclarationAxioms(p).isEmpty() && refOntIRI != null) {
						OWLAnnotationProperty property = dataFactory.getOWLAnnotationProperty(importedMarkerIRI);
						axioms.add(dataFactory.getOWLAnnotationAssertionAxiom(property , p.getIRI(), refOntIRI));
					}
					axioms.addAll(refOnt.getAxioms(p, Imports.EXCLUDED));
				}
				else if (obj instanceof OWLNamedIndividual) {
					final OWLNamedIndividual i = (OWLNamedIndividual) obj;
					if(!refOnt.getDeclarationAxioms(i).isEmpty() && refOntIRI != null) {
						OWLAnnotationProperty property = dataFactory.getOWLAnnotationProperty(importedMarkerIRI);
						axioms.add(dataFactory.getOWLAnnotationAssertionAxiom(property , i.getIRI(), refOntIRI));
					}
					axioms.addAll(refOnt.getAxioms(i, Imports.EXCLUDED));
				}
				else if (obj instanceof OWLDataProperty) {
					axioms.addAll(refOnt.getAxioms((OWLDataProperty) obj, Imports.EXCLUDED));
				}
				else {
					// TODO
				}
				axioms.addAll(refOnt.getAnnotationAssertionAxioms(((OWLEntity) obj).getIRI()));
			}
			finalAxioms.addAll(axioms);
			LOG.info("closure axioms:"+axioms.size());
		}

		// filter axioms, such that the full signature must be in the set of input entities
		// (i.e. no "dangling references")
		// TODO - prevent MIREOT clashes..
		Set<OWLAxiom> filteredAxioms = new HashSet<OWLAxiom>();
		for (OWLAxiom a : finalAxioms) {
			boolean includeThis = true;


			// make this configurable
			if (a instanceof OWLAnnotationAssertionAxiom) {
				// include by default
			}
			else {

				for (OWLEntity e : a.getSignature()) {

					if (e.getIRI().equals(OWL2Datatype.XSD_STRING.getIRI())) {
						// datatypes are included in the signature - ignore these for filtering purposes
						// TODO: handle this more elegantly 
					}
					else if (!objs.contains(e)) {
						if (e instanceof OWLAnnotationProperty) {
							// TODO: we should ensure that these get declared in the subset ontology;
							// rely on the fact that the OWLAPI will infer this for now
							continue;
						}
						if (LOG.isDebugEnabled()) {
							LOG.debug("removing:"+a+" because signature does not include:"+e+" // "+e.getIRI().toURI());
						}
						includeThis = false;
						break;
					}
				}
			}
			if (includeThis)
				filteredAxioms.add(a);
		}
		LOG.info("filtered axioms: "+filteredAxioms.size());
		return filteredAxioms;
	}


	// -----------------------
	// UTIL METHODS
	// -----------------------


	public boolean belongsToSource(OWLEntity obj) {
		if (getOntology().getDeclarationAxioms((OWLEntity) obj).size() > 0) {
			if (!isDangling(getOntology(),obj)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Here a dangling entity is one that has no annotation assertions;
	 * 
	 * this is not a perfect test, as conceivable a class could lack
	 * assertions; however, this would not be the case for any ontology
	 * following obo library principles.
	 * 
	 * this is probably the most reliable test - simply checking
	 * for declarations is insufficient as the owlapi may infer declarations
	 * 
	 * @param ont
	 * @param obj
	 * @return boolean
	 */
	public boolean isDangling(OWLOntology ont, OWLEntity obj) {
		// TODO - import closure
		if (ont.getAnnotationAssertionAxioms(obj.getIRI()).isEmpty()) {
			// in future also consider logical axioms;
			/// problematic - e.g. for symmetric axioms like disjointWith
			return true;
		}		
		return false;
	}


	public Set<OWLClass> getDanglingClasses() {
		return getDanglingClasses(this.getOntology());
	}

	/**
	 * finds all classes for which {@link #isDangling(OWLOntology, OWLEntity)} is true
	 * 
	 * @param ont
	 * @return set of classes that contain a dangling reference
	 */
	public Set<OWLClass> getDanglingClasses(OWLOntology ont) {
		Set<OWLClass> danglers = new HashSet<OWLClass>();
		for (OWLClass obj : ont.getClassesInSignature()) {
			if (isDangling(ont, obj)) {
				danglers.add(obj);
			}
		}
		return danglers;
	}

	/**
	 * Removes any axiom from ont if that axiom contains a dangling
	 * reference - i.e. a reference a class in another ontology.
	 * 
	 * TODO : replace equivalence axioms with weaker axioms;
	 * e.g. if O contains X = A and R some B, and B is in O', then we
	 * want to weaken to X SubClassOf A (and possibly X SubClassOf R some Thing)
	 * 
	 * @param ont
	 */
	public void removeDanglingAxioms(OWLOntology ont) {
		Set<OWLAxiom> rmAxioms = getDanglingAxioms(ont);
		LOG.info("Removing "+rmAxioms.size()+" dangling axioms");
		graph.getManager().removeAxioms(ont, rmAxioms);
		LOG.info("FINISHED Removing "+rmAxioms.size()+" dangling axioms");
	}

	/**
	 * @see #removeDanglingAxioms(OWLOntology)
	 */
	public void removeDanglingAxioms() {
		removeDanglingAxioms(getOntology());
	}

	public Set<OWLAxiom> getDanglingAxioms(OWLOntology ont) {
		Set<OWLAxiom> rmAxioms = new HashSet<OWLAxiom>();
		for (OWLClass obj : ont.getClassesInSignature()) {
			if (isDangling(ont, obj)) {
				rmAxioms.addAll(ont.getReferencingAxioms(obj));
			}
		}
		for (OWLNamedIndividual obj : ont.getIndividualsInSignature()) {
			if (isDangling(ont, obj)) {
				rmAxioms.addAll(ont.getReferencingAxioms(obj));
			}
		}
		for (OWLObjectProperty obj : ont.getObjectPropertiesInSignature()) {
			if (isDangling(ont, obj)) {
				rmAxioms.addAll(ont.getReferencingAxioms(obj));
			}
		}
		return rmAxioms;
	}



	/**
	 * Remove all classes *not* in subset.
	 * 
	 * This means:
	 *   * remove all annotation assertions for that class
	 *   * remove all logical axioms about that class
	 *   
	 *   If removeDangling is true, also remove all axioms that reference this class
	 * 
	 * @param subset
	 * @param removeDangling
	 */
	public void removeSubsetComplementClasses(Set<OWLClass> subset, boolean removeDangling) {
		OWLOntology o = getOntology();
		Set<OWLClass> rmSet = o.getClassesInSignature();
		rmSet.removeAll(subset); // remove all classes not in subset
		Set<OWLAxiom> rmAxioms = new HashSet<OWLAxiom>();
		LOG.info("Num of classes to be removed = "+rmSet.size());
		for (OWLClass c : rmSet) {
			rmAxioms.addAll(o.getAnnotationAssertionAxioms(c.getIRI()));
			rmAxioms.addAll(o.getAxioms(c, Imports.EXCLUDED));
		}
		graph.getManager().removeAxioms(o, rmAxioms);
		if (removeDangling) {
			removeDanglingAxioms(o);
		}
	}
	
	public void removeClassesNotInIDSpace(String idspace, boolean removeDangling) {
		Set<OWLClass> coreSubset = new HashSet<OWLClass>();
		LOG.info("Removing classes not in: "+idspace);
		idspace = idspace.toLowerCase()+":";
		for (OWLClass c : graph.getSourceOntology().getClassesInSignature()) {
			String id = graph.getIdentifier(c).toLowerCase();
			if (id.startsWith(idspace)) {
				coreSubset.add(c);
			}
		}
		LOG.info("Core size:"+coreSubset.size());
		removeSubsetComplementClasses(coreSubset, removeDangling);
	}
	
	/**
	 * Remove a set of classes from the ontology
	 * 
	 * @param rmSet
	 * @param removeDangling
	 */
	public void removeSubsetClasses(Set<OWLClass> rmSet, boolean removeDangling) {
		OWLOntology o = getOntology();
		Set<OWLAxiom> rmAxioms = new HashSet<OWLAxiom>();
		for (OWLClass c : rmSet) {
			rmAxioms.addAll(o.getAnnotationAssertionAxioms(c.getIRI()));
			rmAxioms.addAll(o.getAxioms(c, Imports.EXCLUDED));
		}
		LOG.info("Removing "+rmAxioms.size() +" axioms");
		graph.getManager().removeAxioms(o, rmAxioms);
		if (removeDangling) {
			LOG.info("Removing dangling axioms");
			removeDanglingAxioms(o);
		}
	}

	public OWLOntology makeClosureSubsetOntology(Set<OWLClass> subset, IRI subOntIRI) throws OWLOntologyCreationException {
		Set<OWLObject> objs = getClosureOfExternalReferencedEntities();
		Set<OWLAxiom> axioms = getAxiomsForSubset(objs);



		OWLOntology subOnt = manager.createOntology(axioms, subOntIRI);

		return subOnt;

	}

	/**
	 * 
	 * @param subset
	 * @param subOntIRI
	 * @return subOntology
	 * @throws OWLOntologyCreationException
	 */
	public OWLOntology makeMinimalSubsetOntology(Set<OWLClass> subset, IRI subOntIRI) throws OWLOntologyCreationException {
		return makeMinimalSubsetOntology(subset, subOntIRI, false, true);
	}
	public OWLOntology makeMinimalSubsetOntology(Set<OWLClass> subset, IRI subOntIRI, boolean isFillGaps) throws OWLOntologyCreationException {
		return makeMinimalSubsetOntology(subset, subOntIRI, isFillGaps, true);
	}
	
	/**
	 * Given a set of classes (e.g. those corresponding to an obo-subset or go-slim), and an ontology
	 * in which these are declared, generate a sub-ontology.
	 * 
	 * The sub-ontology will ONLY include classes in the subset (unless isFillGaps is true).
	 * 
	 * It will remove any axioms that refer to classes not in the subset.
	 * Basic graph inference is used to ensure that as many entailments as possible
	 * are preserved.
	 *  
	 * note: this does the same as the perl script go-slimdown, used by the GO Consortium
	 * 
	 * @param subset
	 * @param subOntIRI
	 * @param isFillGaps - if true, subset will be extended to include intermediates terms
	 * @param isSpanGaps - if true, subset classes will be unaltered, but inferred relationships will span gaps
	 * @return subOntology
	 * @throws OWLOntologyCreationException
	 */
	public OWLOntology makeMinimalSubsetOntology(Set<OWLClass> subset, IRI subOntIRI, boolean isFillGaps, Boolean isSpanGaps) throws OWLOntologyCreationException {
		OWLOntology o = getOntology();

		// if fill gaps is true, first extend the subset to closure
		if (isFillGaps) {
			Set<OWLClass> extSet = new HashSet<OWLClass>();
			for (OWLClass c : subset) {
				for (OWLObject p : graph.getAncestors(c))
					if (p instanceof OWLClass)
						extSet.add((OWLClass)p);
			}
			subset.addAll(extSet);
		}

		Set<IRI> iriExcludeSubset = new HashSet<IRI>(); // classes to exclude
		for (OWLClass c : getGraph().getSourceOntology().getClassesInSignature()) {
			if (!subset.contains(c))
				iriExcludeSubset.add(c.getIRI());
		}
		Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
		LOG.info("Testing "+o.getAxioms().size()+" axioms");
		for (OWLAxiom a : o.getAxioms()) {
			boolean isInclude = false;
			/*
			if (a instanceof OWLDeclarationAxiom) {
				((OWLDeclarationAxiom)a).getEntity();
			}
			 */
			if (a instanceof OWLSubClassOfAxiom) {
				if (subset.contains(((OWLSubClassOfAxiom)a).getSubClass())) {
					isInclude = true;
				}
			}
			else if (a instanceof OWLEquivalentClassesAxiom) {
				for (OWLClass x : ((OWLEquivalentClassesAxiom)a).getClassesInSignature()) {
					if (subset.contains(x)) {
						isInclude = true;
						break;
					}
				}
			}
			else if (a instanceof OWLAnnotationAssertionAxiom) {
				// exclude AAA if (1) it is for a class and (2) the class is not in subset
				OWLAnnotationSubject subj = ((OWLAnnotationAssertionAxiom)a).getSubject();
				if (subj instanceof IRI && !iriExcludeSubset.contains(subj)) {
					isInclude = true;
				}
			}
			else {
				isInclude = true;
			}
			if (isInclude) {
				axioms.add(a);
				LOG.debug("including axiom: "+a);
			}
		}
		LOG.info("Base axioms to be added: "+axioms.size());

		LOG.info("Checking closure for: "+subset.size());

		// transitive reduction
		int n = 0;
		if (!isFillGaps && isSpanGaps) {
			// if gaps are not filled, then they are spanned
			for (OWLClass x : subset) {
				Set<OWLGraphEdge> edges = getGraph().getOutgoingEdgesClosure(x);
				for (OWLGraphEdge e : edges) {
					if (subset.contains(e.getTarget())) {
						List<OWLQuantifiedProperty> qpl = e.getQuantifiedPropertyList();
						if (qpl.size() == 1) {
							boolean isRedundant = false;
							OWLQuantifiedProperty qp = qpl.get(0);
							for (OWLGraphEdge e2 : edges) {
								if (subset.contains(e2.getTarget())) {
									for (OWLGraphEdge e3 : getGraph().getOutgoingEdgesClosure(e2.getTarget())) {
										if (e3.getTarget().equals(e.getTarget())) {
											OWLGraphEdge e4 = getGraph().combineEdgePair(e.getSource(), e2, e3, 0);
											if (e4 != null && e4.getQuantifiedPropertyList().equals(qpl)) {
												isRedundant = true;
											}
										}
									}
								}
							}
							if (!isRedundant) {
								if (qp.isSubClassOf()) {
									axioms.add(dataFactory.getOWLSubClassOfAxiom((OWLClass)e.getSource(), (OWLClass)e.getTarget()));
									n++;
								}
								else if (qp.hasProperty()) {
									axioms.add(dataFactory.getOWLSubClassOfAxiom((OWLClass)e.getSource(), 
											dataFactory.getOWLObjectSomeValuesFrom(qp.getProperty(), (OWLClass)e.getTarget())));
									n++;
								}
								else {
								}
							}
						}
					}
				}
			}
		}
		LOG.info("# of gaps spanned = "+n);

		// Note that the OWLAPI is slow to remove axioms from an existing ontology.
		// It is more efficient to remove the axioms from the seed set first.
		// In order to do this, we make a temporary sub-ontology with all the axioms,
		// use this to calculate the axioms that need removing, and then finally create the
		// final sub-ontology
		OWLOntology subOnt = manager.createOntology(axioms);
		Set<OWLAxiom> rmAxioms = getDanglingAxioms(subOnt);
		LOG.info("Removing "+rmAxioms.size()+" dangling axioms from "+subOntIRI);
		axioms.removeAll(rmAxioms);
		subOnt = manager.createOntology(axioms, subOntIRI);

		this.removeDanglingAxioms(subOnt);
		return subOnt;

	}

	/**
	 * Check, if the named object has the annotation property IAO:0000412, 
	 * declaring the object as imported.
	 * 
	 * @param named
	 * @param ontology
	 * @return true if the item has the annotation property IAO:0000412
	 */
	public static boolean isImportMarkedEntity(OWLNamedObject named, OWLOntology ontology) {
		for (OWLAnnotationAssertionAxiom axiom : ontology.getAnnotationAssertionAxioms(named.getIRI())) {
			OWLAnnotationProperty property = axiom.getProperty();
			if (importedMarkerIRI.equals(property.getIRI())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Removes all classes that have IAO_0000412 'imported from' set to another ontology.
	 * 
	 * Removes from both source ontology and referenced ontologies
	 * 
	 * @param removeDangling
	 */
	public void removeExternalOntologyClasses(boolean removeDangling) {
		OWLOntology ont = graph.getSourceOntology();
		removeExternalEntities(true, ont);
		// todo - make this optional
		for(OWLOntology refOnt : getReferencedOntologies()) {
			LOG.info("Removing imported entities (IAO_0000412) from:"+refOnt);
			removeExternalEntities(false, refOnt);
		}
		if (removeDangling) {
			LOG.info("removing dangling");
			removeDanglingAxioms(ont);
		}
	}

	public void removeExternalEntities() {
		removeExternalEntities(this.getOntology());
	}

	/**
	 * Removes all classes, individuals and object properties that are marked with
	 * IAO_0000412
	 * 
	 * @param ont
	 */
	public void removeExternalEntities(OWLOntology ont) {
		removeExternalEntities(true, ont);
	}

	/**
	 * removes external entities:
	 *  - anything marked with IAO_0000412 is removed
	 *  - if isMain is true then anything that is also present in an external ontology is removed
	 * 
	 * @param isMain
	 * @param ont
	 */
	public void removeExternalEntities(boolean isMain, OWLOntology ont) {
		Set<OWLEntity> objs = ont.getSignature(Imports.EXCLUDED);
		Set<OWLClass> rmClasses = new HashSet<OWLClass>();
		Set<OWLObjectProperty> rmProperties = new HashSet<OWLObjectProperty>();
		Set<OWLIndividual> rmIndividuals = new HashSet<OWLIndividual>();
		LOG.info("removing external entities marked with IAO_0000412 from: "+ont);
		if (isMain) {
			LOG.info("testing " + objs.size()
					+ " objs to see if they are contained in the follow referenced ontologies: "
					+ getReferencedOntologies());
		}
		for (OWLEntity obj : objs) {
			final boolean isMarked = isImportMarkedEntity(obj, ont);
			if (obj instanceof OWLClass) {
				if ((isMain && isInExternalOntology(obj)) || isMarked) {
					rmClasses.add((OWLClass) obj);
				}
			}
			else if (obj instanceof OWLObjectProperty && isMarked) {
				rmProperties.add((OWLObjectProperty) obj);
			}
			else if (obj instanceof OWLIndividual  && isMarked) {
				rmIndividuals.add((OWLIndividual) obj);
			}
		}
		Set<OWLAxiom> rmAxioms = new HashSet<OWLAxiom>();
		for (OWLClass c : rmClasses) {
			rmAxioms.addAll(ont.getAnnotationAssertionAxioms(c.getIRI()));
			rmAxioms.addAll(ont.getDeclarationAxioms(c));
			rmAxioms.addAll(ont.getAxioms(c, Imports.EXCLUDED));
		}
		for (OWLObjectProperty p : rmProperties) {
			rmAxioms.addAll(ont.getAnnotationAssertionAxioms(p.getIRI()));
			rmAxioms.addAll(ont.getDeclarationAxioms(p));
			rmAxioms.addAll(ont.getAxioms(p, Imports.EXCLUDED));
		}
		for (OWLIndividual i : rmIndividuals) {
			rmAxioms.addAll(ont.getAxioms(i, Imports.EXCLUDED));
		}
		if (!rmAxioms.isEmpty()) {
			LOG.info("Removing "+rmAxioms.size()+" external axioms for: "+ ont.getOntologyID());
			ont.getOWLOntologyManager().removeAxioms(ont, rmAxioms);
		}
	}
	
	public static Set<OWLObject> findTaggedEntities(OWLAnnotationProperty p, Set<OWLAnnotationValue> values, final OWLGraphWrapper graph) {
		if (p == null || values == null || values.isEmpty()) {
			return Collections.emptySet();
		}
		final Set<OWLObject> entities = new HashSet<OWLObject>();
		Set<OWLOntology> allOntologies = graph.getAllOntologies();
		for (OWLOntology ontology : allOntologies) {
			Set<OWLAnnotationAssertionAxiom> axioms = ontology.getAxioms(AxiomType.ANNOTATION_ASSERTION);
			for (OWLAnnotationAssertionAxiom axiom : axioms) {
				if (p.equals(axiom.getProperty()) && values.contains(axiom.getValue())) {
					axiom.getSubject().accept(new OWLAnnotationSubjectVisitor(){

						@Override
						public void visit(IRI iri) {
							OWLObject owlObject = graph.getOWLObject(iri);
							if (owlObject != null) {
								entities.add(owlObject);
							}
						}

						@Override
						public void visit(OWLAnonymousIndividual individual) {
							// do nothing
						}
					});
				}
			}
		}
		return entities;
	}
	
	public static List<RemoveAxiom> findRelatedAxioms(final Set<OWLObject> entities, final OWLGraphWrapper graph) {
		if (entities.isEmpty()) {
			return Collections.emptyList();
		}
		List<RemoveAxiom> changes = new ArrayList<RemoveAxiom>();

		for(final OWLOntology ont : graph.getAllOntologies()) {
			final Set<OWLAxiom> toRemove = new HashSet<OWLAxiom>();
			// find all annotations
			for(final OWLObject obj : entities) {
				obj.accept(new OWLObjectVisitorAdapter(){

					@Override
					public void visit(OWLClass cls) {
						toRemove.addAll(ont.getAnnotationAssertionAxioms(cls.getIRI()));
					}

					@Override
					public void visit(OWLObjectProperty property) {
						toRemove.addAll(ont.getAnnotationAssertionAxioms(property.getIRI()));
					}

					@Override
					public void visit(OWLNamedIndividual individual) {
						toRemove.addAll(ont.getAnnotationAssertionAxioms(individual.getIRI()));
					}
				});
			};
			// find all axioms that use one of the entities in its signature
			for(OWLAxiom axiom : ont.getAxioms()) {
				Set<OWLEntity> signature = axiom.getSignature();
				if (Sets.intersection(signature, entities).isEmpty() == false){
					toRemove.add(axiom);
				};
			}
			if (toRemove.isEmpty() == false) {
				for (OWLAxiom axiom : toRemove) {
					changes.add(new RemoveAxiom(ont, axiom));
				}
			}
		}
		return changes;
	}

	/**
	 * For every pair X DisjointWith Y, generate an axiom
	 * A and Y = Nothing
	 * 
	 * (may become deprecated after Elk supports disjoints)
	 * 
	 */
	public void translateDisjointsToEquivalents() {
		for (OWLOntology ont : getAllOntologies()) {
			translateDisjointsToEquivalents(ont, getManager(), dataFactory);
		}
	}

	/**
	 * For every pair X DisjointWith Y, generate an axiom
	 * A and Y = Nothing
	 * 
	 * (may become deprecated after Elk supports disjoints)
	 * 
	 * @param ont
	 * @param manager
	 * @param dataFactory
	 */
	public static void translateDisjointsToEquivalents(OWLOntology ont, OWLOntologyManager manager, OWLDataFactory dataFactory) {
		for (OWLDisjointClassesAxiom dca : ont.getAxioms(AxiomType.DISJOINT_CLASSES, Imports.INCLUDED)) {
			for (OWLClassExpression ce1 : dca.getClassExpressions()) {
				for (OWLClassExpression ce2 : dca.getClassExpressions()) {
					if (ce1.compareTo(ce2) <= 0)
						continue;
					OWLEquivalentClassesAxiom eca = dataFactory.getOWLEquivalentClassesAxiom(dataFactory.getOWLNothing(),
							dataFactory.getOWLObjectIntersectionOf(ce1, ce2));
					manager.addAxiom(ont, eca);
					// TODO - remove if requested
				}
			}
		}
	}

	/**
	 * 
	 * @param props
	 */
	public void retainAxiomsInPropertySubset(Set<OWLObjectProperty> props) {
		retainAxiomsInPropertySubset(this.getOntology(), props);
	}

	/**
	 * @param ont
	 * @param filterProps
	 */
	public static void retainAxiomsInPropertySubset(OWLOntology ont, Set<OWLObjectProperty> filterProps) {
		retainAxiomsInPropertySubset(ont, filterProps, null);
	}

	/**
	 * given an ontology *ont* and a set of object properties *filterProps*, remove all axioms from ontology that
	 * have an object property P in their signature where P is not in *filterProps*
	 * 
	 * @param ont
	 * @param filterProps
	 * @param reasoner
	 */
	public static void retainAxiomsInPropertySubset(OWLOntology ont, Set<OWLObjectProperty> filterProps, OWLReasoner reasoner) {
		LOG.info("Removing axioms that use properties not in set: "+filterProps);
		Set<OWLAxiom> rmAxioms = new HashSet<OWLAxiom>();
		Set<OWLAxiom> newAxioms = new HashSet<OWLAxiom>();
		OWLDataFactory df = ont.getOWLOntologyManager().getOWLDataFactory();
		for (OWLAxiom ax : ont.getAxioms()) {
			Set<OWLObjectProperty> ps = ax.getObjectPropertiesInSignature();
			ps.removeAll(filterProps);
			if (ps.size() > 0) {
				rmAxioms.add(ax);

				// if p not-in SubSet, and
				//  A = X SubClassOf p Some Y,
				// then rewrite as
				//  A = X SubClassOf p' some Y, where p' SubPropertyOf p
				// rewrite as weaker axioms.
				// TOOD - Elk does not support superobjectprops - do in wrapper for now?
				if (reasoner != null) {
					if (ax instanceof OWLSubClassOfAxiom) {
						OWLSubClassOfAxiom sca = (OWLSubClassOfAxiom)ax;
						if (!sca.getSubClass().isAnonymous()) {
							if (sca.getSuperClass() instanceof OWLObjectSomeValuesFrom) {
								OWLObjectSomeValuesFrom svf = (OWLObjectSomeValuesFrom) sca.getSuperClass();
								OWLObjectPropertyExpression p = svf.getProperty();
								Set<OWLObjectPropertyExpression> sps = reasoner.getSuperObjectProperties(p, false).getFlattened();
								sps.retainAll(filterProps);
								for (OWLObjectPropertyExpression sp : sps) {
									OWLObjectSomeValuesFrom newSvf = df.getOWLObjectSomeValuesFrom(sp, svf.getFiller());
									OWLSubClassOfAxiom newSca = df.getOWLSubClassOfAxiom(sca.getSubClass(), newSvf);
									LOG.info("REWRITE: "+sca+" --> "+newSca);
									newAxioms.add(newSca);
								}
							}
						}
					}
					//else if (ax instanceof OWLEquivalentClassesAxiom) {
					//	((OWLEquivalentClassesAxiom)ax).getClassExpressions();
					//}
				}
			}
		}
		LOG.info("Removing "+rmAxioms.size()+" axioms");
		ont.getOWLOntologyManager().removeAxioms(ont, rmAxioms);
		LOG.info("Adding "+newAxioms.size()+" axioms");
		ont.getOWLOntologyManager().addAxioms(ont, newAxioms);
	}

	public Map<OWLObject,Set<OWLObject>> createSubsetMap(String ss) {
		return createSubsetMap(graph.getOWLObjectsInSubset(ss));
	}
	
	/**
	 * Note: this assumes you have already filtered any relations not to be
	 * propagated over from the graph
	 * 
	 * @param subset
	 * @return map
	 */
	public Map<OWLObject,Set<OWLObject>> createSubsetMap(Set<OWLObject> subset) {
		Map<OWLObject, Set<OWLObject>> ssm = new HashMap<OWLObject,Set<OWLObject>>();
		for (OWLObject s : graph.getAllOWLObjects()) {
			Set<OWLObject> ancs = graph.getAncestorsReflexive(s);
			ancs.retainAll(subset);
			removeRedundant(ancs);
			ssm.put(s, ancs);
		}
		return ssm;
	}

	public void removeRedundant(Set<OWLObject> ancs) {
		Set<OWLObject> rmObjs = new HashSet<OWLObject>();
		for (OWLObject c : ancs) {
			rmObjs.addAll(graph.getAncestors(c));
		}
		ancs.removeAll(rmObjs);
		
	}

	public void extractModules() {
		OWLOntology ont = graph.getSourceOntology();
		extractModules(ont);
	}



	public void extractModules(OWLOntology ont) {
		Set<OWLOntology> ionts = ont.getImports();
		Set<OWLClass> iclasses = new HashSet<OWLClass>();
		for (OWLOntology iont : ionts) {
			iclasses.addAll(iont.getClassesInSignature(Imports.INCLUDED));
		}
		extractModules(ont.getClassesInSignature(), iclasses);
		
	}

	public void extractModules(Set<OWLClass> classesInSignature,
			Set<OWLClass> iclasses) {
		// TODO Auto-generated method stub
		
	}



	/**
	 * Assumes OBO-style IDspaces; specifically URI contains "..../IDSPACE_..." 
	 * @param idspace
	 * @param subOnt
	 */
	public void transferAxiomsUsingIdSpace(String idspace, OWLOntology subOnt) {
		Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
		LOG.info("ID Space: "+idspace);
		for (OWLClass c : getOntology().getClassesInSignature()) {
			String iriStr = c.getIRI().toString().toLowerCase();
			if (iriStr.contains("/"+idspace.toLowerCase()+"_")) {
				LOG.info("MATCH: "+c);
				axioms.addAll(getOntology().getDeclarationAxioms(c));
				axioms.addAll(getOntology().getAxioms(c, Imports.EXCLUDED));
				axioms.addAll(getOntology().getAnnotationAssertionAxioms(c.getIRI()));
			}
		}
		LOG.info("Transferring "+axioms.size()+" axioms from "+getOntology()+" to "+subOnt);
		this.getManager().removeAxioms(getOntology(), axioms);
		this.getManager().addAxioms(subOnt, axioms);
	}

}
