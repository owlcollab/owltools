package owltools.mooncat;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

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
	OWLOntology ontology;
	Set<OWLOntology> referencedOntologies = new HashSet<OWLOntology>();
	Set<OWLOntology> allOntologies = null;
	OWLGraphWrapper graph;
	Set<String> sourceOntologyPrefixes = null;

	public Mooncat(OWLOntologyManager manager, OWLDataFactory dataFactory,
			OWLOntology ontology) {
		super();
		this.manager = manager;
		this.dataFactory = dataFactory;
		this.ontology = ontology;
	}



	public Mooncat(OWLGraphWrapper g) {
		super();
		this.ontology = g.getSourceOntology();
		this.referencedOntologies = g.getSupportOntologySet();
		this.manager = OWLManager.createOWLOntologyManager();
		this.dataFactory = manager.getOWLDataFactory();
		this.graph = g;
	}



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
		return manager;
	}

	public void setManager(OWLOntologyManager manager) {
		this.manager = manager;
	}



	public OWLGraphWrapper getGraph() {
		return graph;
	}



	public void setGraph(OWLGraphWrapper graph) {
		this.graph = graph;
	}



	public OWLOntology getOntology() {
		return ontology;
	}

	public void setOntology(OWLOntology ontology) {
		this.ontology = ontology;
	}

	@Deprecated
	public void addImport(String importedIRIString) {
		OWLImportsDeclaration iax = dataFactory.getOWLImportsDeclaration(IRI.create(importedIRIString));
		AddImport addAx = new AddImport(ontology, iax);
		manager.applyChange(addAx);
	}




	/**
	 * @return set of entities that belong to a referenced ontology that are referenced in the source ontology
	 */
	public Set<OWLEntity> getExternalReferencedEntities() {
		OWLOntology ont = graph.getSourceOntology();
		Set<OWLEntity> objs = ont.getSignature(false);
		Set<OWLEntity> refObjs = new HashSet<OWLEntity>();
		for (OWLEntity obj : objs) {
			//LOG.info("considering: "+obj);
			// a reference ontology may have entities from the source ontology MIREOTed in..
			// allow a configuration with the URI prefix specified
			if (sourceOntologyPrefixes != null) {
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
					LOG.info("  refObj: "+obj);
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
	 * finds the full closure of all external referenced entities
	 * @return
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
		return objs;
	}

	/**
	 * find all axioms in closure of external referenced entities.
	 * 
	 * Steps:
	 * (1) find all referenced entities
	 * (2) find all axioms about these entities
	 * (3) filter these axioms such that the classes in the signature are in the subset
	 * 
	 * @return
	 */
	public Set<OWLAxiom> getClosureAxiomsOfExternalReferencedEntities() {
		Set<OWLObject> objs = getClosureOfExternalReferencedEntities();
		return getAxiomsForSubset(objs);
	}
	
	/**
	 * Given a subset (e.g. GO slim), find axioms
	 * 
	 * Steps:
	 * (1) for each class in subset, add axioms that are about this class
	 * (2) filter these axioms such that the classes in the signature are in the subset
	 * 
	 * 
	 * @param objs
	 * @return
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

				if (obj instanceof OWLClass) {
					LOG.info("class:"+obj);
					// includes SubClassOf(obj,?), disjoints, equivalents, ..
					axioms.addAll(refOnt.getAxioms((OWLClass) obj));
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
					if (!objs.contains(e)) {
						logger.info("removing:"+a+" -- E:"+e);
						includeThis = false;
						break;
					}
				}
			}
			if (includeThis)
				filteredAxioms.add(a);
		}
		return filteredAxioms;
	}
	
	/**
	 * merge minimal subset of referenced ontologies into the source ontology
	 * 
	 */
	public void mergeOntologies() {
		OWLOntology srcOnt = graph.getSourceOntology();
		Set<OWLAxiom> axioms = getClosureAxiomsOfExternalReferencedEntities();
		for (OWLAxiom a : axioms) {
			logger.info("Adding:"+a);
		}
		manager.addAxioms(srcOnt, axioms);
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
	 * Here a dangling entity is one that has no annotation assertions
	 * 
	 * @param ont
	 * @param obj
	 * @return
	 */
	public boolean isDangling(OWLOntology ont, OWLEntity obj) {
		if (obj.getAnnotationAssertionAxioms(ont).size()  == 0 ) {
			// in future also consider logical axioms
			return true;
		}		
		return false;
	}

	public void removeDanglingAxioms(OWLOntology ont) {
		Set<OWLClass> danglers = new HashSet<OWLClass>();
		Set<OWLAxiom> rmAxioms = new HashSet<OWLAxiom>();
		for (OWLClass obj : ont.getClassesInSignature()) {
			LOG.info("testing "+obj);
			if (isDangling(ont, obj)) {
				danglers.add(obj);
				rmAxioms.addAll(ont.getReferencingAxioms(obj));
			}
		}
		LOG.info("Removing "+rmAxioms.size()+" dangling axioms");
		graph.getManager().removeAxioms(ont, rmAxioms);
	}

	public void removeDanglingAxioms() {
		removeDanglingAxioms(getOntology());
	}

}
