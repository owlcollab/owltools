package owltools.mooncat;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubAnnotationPropertyOfAxiom;
import org.semanticweb.owlapi.vocab.OWL2Datatype;

import owltools.graph.OWLGraphWrapper;

/**
 * Given one source ontology referencing one or more referenced ontologies
 * (e.g. CL referencing PRO, GO, CHEBI, UBERON), merge/copy selected axiom
 * from the referenced ontologies into the source ontology.
 * 
 * This relies on a {@link OWLGraphWrapper} object being created, in which
 * the the source ontology is the primary ontology of interest, and the
 * support ontologies are the set of ontologies from which references are
 * drawn.
 * For example, src=CL, sup={PRO,GO,CHEBI,UBERON}
 * 
 * In the future, owl imports will be supported
 * 
 * @author cjm
 *
 */
public class Mooncat {

	private final static Logger logger = Logger.getLogger(Mooncat.class);

	private static Logger LOG = Logger.getLogger(Mooncat.class);

	OWLOntologyManager manager;
	OWLDataFactory dataFactory;
	Set<OWLAxiom> mAxioms = new HashSet<OWLAxiom>();
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
		this.manager = OWLManager.createOWLOntologyManager();
		this.dataFactory = manager.getOWLDataFactory();
		this.graph = g;
	}



	/**
	 * E.g. http://purl.obolibrary.org/obo/go_
	 * @return
	 */
	public Set<String> getSourceOntologyPrefixes() {
		return sourceOntologyPrefixes;
	}



	public void setSourceOntologyPrefixes(Set<String> sourceOntologyPrefixes) {
		this.sourceOntologyPrefixes = sourceOntologyPrefixes;
	}

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
		OWLOntology srcOnt = graph.getSourceOntology();
		Set<OWLAxiom> axioms = getClosureAxiomsOfExternalReferencedEntities();

		// add ALL subannotprop axioms
		// - this is quite geared towards obo ontologies, where
		//   we want to preserve obo headers.
		// TODO: make this configurable
		for (OWLOntology refOnt : this.getReferencedOntologies()) {
			for (OWLSubAnnotationPropertyOfAxiom a : refOnt.getAxioms(AxiomType.SUB_ANNOTATION_PROPERTY_OF)) {
				axioms.add(a);
			}
		}

		for (OWLAxiom a : axioms) {
			logger.info("Adding:"+a);
		}
		manager.addAxioms(srcOnt, axioms);
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
	 * It is irrelevant whether go:1, ... is declared in the source (e.g. MIREOTED)
	 * 
	 * Note this only returns direct references. See
	 * {@link getClosureOfExternalReferencedEntities} for closure of references
	 * 
	 * @return all objects referenced by source ontology
	 */
	public Set<OWLEntity> getExternalReferencedEntities() {
		OWLOntology ont = graph.getSourceOntology();
		Set<OWLEntity> objs = ont.getSignature(false);
		Set<OWLEntity> refObjs = new HashSet<OWLEntity>();
		LOG.info("testing "+objs.size()+" objs to see if they are contained in: "+getReferencedOntologies());
		for (OWLEntity obj : objs) {
			//LOG.info("considering: "+obj);
			// a reference ontology may have entities from the source ontology MIREOTed in..
			// allow a configuration with the URI prefix specified
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
					refObjs.add(obj);
					LOG.info("  refObj: "+obj+" // "+sourceOntologyPrefixes);
					continue;
				}
			}
			else {
				// estimate by ref ontologies
				for (OWLOntology refOnt : getReferencedOntologies()) {
					//LOG.info("  refOnt: "+refOnt);
					if (refOnt.getDeclarationAxioms(obj).size() > 0) {
						refObjs.add(obj);
						LOG.info("  refObj: "+obj);
						continue;
					}
				}
			}
		}
		LOG.info("#refObjs: "+refObjs.size());

		return refObjs;
	}

	/**
	 * finds the full closure of all external referenced entities.
	 * 
	 * calls {@link getExternalReferencedEntities} and then finds all reflexive ancestors of this set.
	 * 
	 * to configure the travsersal, see {@link OWLGraphhWrapper}
	 * 
	 * @return closure of all external referenced entities
	 */
	public Set<OWLObject> getClosureOfExternalReferencedEntities() {
		Set<OWLObject> objs = new HashSet<OWLObject>();
		Set<OWLEntity> refs = getExternalReferencedEntities();
		LOG.info("direct external referenced entities: "+refs.size());
		for (OWLEntity ref : refs) {
			// todo - allow per-relation control
			// todo - make more efficient, allow passing of set of entities
			// todo - include object properties
			Set<OWLObject> ancs = graph.getAncestorsReflexive(ref);
			objs.addAll(ancs);
		}
		LOG.info("closure of direct external referenced entities: "+objs.size());

		// also include referenced annotation properties
		Set<OWLObject> extraObjs = new HashSet<OWLObject>();
		for (OWLObject obj : objs) {
			if (obj instanceof OWLEntity) {
				for (OWLOntology refOnt : this.getReferencedOntologies()) {
					for (OWLAnnotationAssertionAxiom aaa : ((OWLEntity)obj).getAnnotationAssertionAxioms(refOnt)) {
						extraObjs.add(aaa.getProperty());
						extraObjs.add(aaa.getValue());
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
	 * The first step is carried out by {@link getClosureOfExternalReferencedEntities}
	 * The second and third steps by {@link getAxiomsForSubset}
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
	 * "slim down" an ontology.
	 * 
	 * Given a set of objects (e.g. a GO slim), find axioms constituting sub-ontology
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
	 * @param objsInSubset
	 * @return axioms in subontology
	 */
	public Set<OWLAxiom> getAxiomsForSubset(Set<OWLObject> objs) {
		Set<OWLAxiom> finalAxioms = new HashSet<OWLAxiom>();
		LOG.info("inputObjs:"+objs.size());
		// first get a set of all candidate axioms;
		// we will later filter these
		for (OWLOntology refOnt : getReferencedOntologies()) {
			LOG.info("refOnt:"+refOnt);
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
					LOG.info("class:"+obj);
					// includes SubClassOf(obj,?), disjoints, equivalents, ..
					axioms.addAll(refOnt.getAxioms((OWLClass) obj));

					axioms.addAll(refOnt.getDeclarationAxioms((OWLClass) obj));
				}
				else if (obj instanceof OWLObjectProperty) {
					axioms.addAll(refOnt.getAxioms((OWLObjectProperty) obj));
				}
				else if (obj instanceof OWLNamedIndividual) {
					axioms.addAll(refOnt.getAxioms((OWLNamedIndividual) obj));
				}
				else if (obj instanceof OWLDataProperty) {
					axioms.addAll(refOnt.getAxioms((OWLDataProperty) obj));
				}
				else {
					// TODO
				}
				axioms.addAll(((OWLEntity) obj).getAnnotationAssertionAxioms(refOnt));
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

					if (e.getIRI().toURI().equals(OWL2Datatype.XSD_STRING.getURI())) {
						// datatypes are included in the signature - ignore these for filtering purposes
						// TODO: handle this more elegantly 
					}
					else if (!objs.contains(e)) {
						if (e instanceof OWLAnnotationProperty) {
							// TODO: we should ensure that these get declared in the subset ontology;
							// rely on the fact that the OWLAPI will infer this for now
							continue;
						}
						logger.info("removing:"+a+" because signature does not include:"+e+" // "+e.getIRI().toURI());
						includeThis = false;
						break;
					}
				}
			}
			if (includeThis)
				filteredAxioms.add(a);
		}
		logger.info("filtered axioms: "+filteredAxioms.size());
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
	 * @return
	 */
	public boolean isDangling(OWLOntology ont, OWLEntity obj) {
		if (obj.getAnnotationAssertionAxioms(ont).size()  == 0 ) {
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
	 * finds all classes for which {@link isDangling} is true
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
	 * reference - i.e. a reference a class in another ontology
	 * 
	 * @param ont
	 */
	public void removeDanglingAxioms(OWLOntology ont) {
		Set<OWLAxiom> rmAxioms = new HashSet<OWLAxiom>();
		for (OWLClass obj : ont.getClassesInSignature()) {
			//LOG.info("testing "+obj);
			if (isDangling(ont, obj)) {
				rmAxioms.addAll(ont.getReferencingAxioms(obj));
			}
		}
		LOG.info("Removing "+rmAxioms.size()+" dangling axioms");
		graph.getManager().removeAxioms(ont, rmAxioms);
	}

	public void removeDanglingAxioms() {
		removeDanglingAxioms(getOntology());
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
		rmSet.removeAll(subset);
		Set<OWLAxiom> rmAxioms = new HashSet<OWLAxiom>();
		for (OWLClass c : rmSet) {
			rmAxioms.addAll(c.getAnnotationAssertionAxioms(o));
			rmAxioms.addAll(o.getAxioms(c));
		}
		graph.getManager().removeAxioms(o, rmAxioms);
		if (removeDangling) {
			removeDanglingAxioms(o);
		}
	}


}
