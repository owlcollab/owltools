package owltools.graph;

import gnu.trove.set.hash.THashSet;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.AddOntologyAnnotation;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.RemoveImport;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import owltools.io.ParserWrapper;

/**
 * Basic methods for handling multiple {@link OWLOntology} objects as one graph.
 * Provide methods to add, remove, or merge support ontologies.
 * 
 * @see OWLGraphWrapper
 */
public class OWLGraphWrapperBasic {
	
	private static final Logger LOG = Logger.getLogger(OWLGraphWrapperBasic.class);

	protected OWLOntology sourceOntology; // graph is seeded from this ontology.

	protected Set<OWLOntology> supportOntologySet = new HashSet<OWLOntology>();

	protected OWLGraphWrapperBasic(OWLOntology ontology) {
		super();
		sourceOntology = ontology;
	}
	
	protected OWLGraphWrapperBasic(String iri) throws OWLOntologyCreationException {
		super();
		ParserWrapper pw = new ParserWrapper();
		OWLOntologyManager manager = pw.getManager();
		sourceOntology = manager.createOntology(IRI.create(iri));
	}

	private void addCommentToOntology(OWLOntology ont, String cmt) {
		OWLDataFactory df = getDataFactory();
		OWLAnnotationProperty p = 
				df.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_COMMENT.getIRI());
		OWLLiteral v = df.getOWLLiteral(cmt);
		OWLAnnotation ann = df.getOWLAnnotation(p, v);
		AddOntologyAnnotation addAnn = 
				new AddOntologyAnnotation(ont, ann);
		getManager().applyChange(addAnn);
	}
	/**
	 * adds an imports declaration between the source ontology and extOnt
	 * 
	 * @param extOnt
	 */
	public void addImport(OWLOntology extOnt) {
		AddImport ai = new AddImport(getSourceOntology(), getDataFactory().getOWLImportsDeclaration(extOnt.getOntologyID().getOntologyIRI()));
		getManager().applyChange(ai);
	}

	/**
	 * Adds all axioms from extOnt into source ontology
	 * 
	 * @param extOnt
	 * @throws OWLOntologyCreationException
	 */
	public void mergeOntology(OWLOntology extOnt) throws OWLOntologyCreationException {
		OWLOntologyManager manager = getManager();
		for (OWLAxiom axiom : extOnt.getAxioms()) {
			manager.applyChange(new AddAxiom(sourceOntology, axiom));
		}
		for (OWLImportsDeclaration oid: extOnt.getImportsDeclarations()) {
			manager.applyChange(new AddImport(sourceOntology, oid));
		}
		addCommentToOntology(sourceOntology, "Includes "+extOnt);
	}

	public void mergeOntology(OWLOntology extOnt, boolean isRemoveFromSupportList) throws OWLOntologyCreationException {
		mergeOntology(extOnt);
		if (isRemoveFromSupportList) {
			this.supportOntologySet.remove(extOnt);
		}
	}

	public void mergeSupportOntology(String ontologyIRI, boolean isRemoveFromSupportList) throws OWLOntologyCreationException {
		OWLOntology extOnt = null;
		for (OWLOntology ont : this.supportOntologySet) {
			if (ont.getOntologyID().getOntologyIRI().toString().equals(ontologyIRI)) {
				extOnt = ont;
				break;
			}
		}

		mergeOntology(extOnt);
		if (isRemoveFromSupportList) {
			this.supportOntologySet.remove(extOnt);
		}
	}

	public void mergeSupportOntology(String ontologyIRI) throws OWLOntologyCreationException {
		mergeSupportOntology(ontologyIRI, true);
	}

	/**
	 * Every OWLGraphWrapper objects wraps zero or one source ontologies.
	 * 
	 * @return ontology
	 */
	public OWLOntology getSourceOntology() {
		return sourceOntology;
	}

	public void setSourceOntology(OWLOntology sourceOntology) {
		this.sourceOntology = sourceOntology;
	}

	/**
	 * all operations are over a set of ontologies - the source ontology plus
	 * any number of supporting ontologies. The supporting ontologies may be drawn
	 * from the imports closure of the source ontology, although this need not be the case.
	 * 
	 * @return set of support ontologies
	 */
	public Set<OWLOntology> getSupportOntologySet() {
		return supportOntologySet;
	}

	public void setSupportOntologySet(Set<OWLOntology> supportOntologySet) {
		this.supportOntologySet = supportOntologySet;
	}

	public void addSupportOntology(OWLOntology o) {
		this.supportOntologySet.add(o);
	}
	public void removeSupportOntology(OWLOntology o) {
		this.supportOntologySet.remove(o);
	}

	/**
	 * Each ontology in the import closure of the source ontology is added to
	 * the list of support ontologies
	 * 
	 */
	public void addSupportOntologiesFromImportsClosure() {
		addSupportOntologiesFromImportsClosure(false);

	}
	/**
	 * Each ontology in the import closure of the source ontology
	 * (and the import closure of each existing support ontology, if
	 * doForAllSupportOntologies is true) is added to
	 * the list of support ontologies
	 * 
	 * @param doForAllSupportOntologies
	 */
	public void addSupportOntologiesFromImportsClosure(boolean doForAllSupportOntologies) {
		Set<OWLOntology> ios = new HashSet<OWLOntology>();
		ios.add(sourceOntology);
		
		if (doForAllSupportOntologies) {
			ios.addAll(getSupportOntologySet());
		}
		for (OWLOntology so : ios) {
			for (OWLOntology o : so.getImportsClosure()) {
				if (o.equals(sourceOntology))
					continue;
				addSupportOntology(o);
			}
		}
	}
	
	public void addImportsFromSupportOntologies() {
		OWLOntology sourceOntology = getSourceOntology();
		OWLDataFactory factory = getDataFactory();
		for (OWLOntology  o : getSupportOntologySet()) {
			OWLImportsDeclaration importsDeclaration = 
					factory.getOWLImportsDeclaration(o.getOntologyID().getOntologyIRI());
			AddImport ai = new AddImport(sourceOntology, importsDeclaration);
			LOG.info("Applying: "+ai);
			getManager().applyChange(ai);
		}
		this.setSupportOntologySet(new HashSet<OWLOntology>());
	}

	public void remakeOntologiesFromImportsClosure() throws OWLOntologyCreationException {
		remakeOntologiesFromImportsClosure((new OWLOntologyID()).getOntologyIRI());
	}

	public void remakeOntologiesFromImportsClosure(IRI ontologyIRI) throws OWLOntologyCreationException {
		addSupportOntologiesFromImportsClosure();
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		sourceOntology = manager.createOntology(sourceOntology.getAxioms(), ontologyIRI);
	}

	/**
	 * note: may move to mooncat
	 * @throws OWLOntologyCreationException
	 */
	public void mergeImportClosure() throws OWLOntologyCreationException {
		mergeImportClosure(false);
	}
	public void mergeImportClosure(boolean isRemovedImportsDeclarations) throws OWLOntologyCreationException {
		OWLOntologyManager manager = getManager();
		Set<OWLOntology> imports = sourceOntology.getImportsClosure();
		for (OWLOntology o : imports) {
			if (o.equals(sourceOntology))
				continue;
			
			String comment = "Includes "+o;
			LOG.info(comment);
			addCommentToOntology(sourceOntology, comment);
			
			manager.addAxioms(sourceOntology, o.getAxioms());
		}
		Set<OWLImportsDeclaration> oids = sourceOntology.getImportsDeclarations();
		for (OWLImportsDeclaration oid : oids) {
			RemoveImport ri = new RemoveImport(sourceOntology, oid);
			getManager().applyChange(ri);
		}
	}
	
	/**
	 * Merge a specific ontology from the import closure into the main ontology.
	 * Removes the import statement.
	 * 
	 * @param ontologyIRI id of the ontology to merge
	 * @throws OWLOntologyCreationException
	 */
	public void mergeSpecificImport(IRI ontologyIRI) throws OWLOntologyCreationException {
		OWLOntologyManager manager = getManager();
		Set<OWLOntology> imports = sourceOntology.getImportsClosure();
		for (OWLOntology o : imports) {
			if (o.equals(sourceOntology))
				continue;
			OWLOntologyID ontologyID = o.getOntologyID();
			if (ontologyID !=  null && ontologyIRI.equals(ontologyID.getOntologyIRI())) {
				String comment = "Includes "+o;
				LOG.info(comment);
				addCommentToOntology(sourceOntology, comment);
				manager.addAxioms(sourceOntology, o.getAxioms());	
			}
		}
		Set<OWLImportsDeclaration> oids = sourceOntology.getImportsDeclarations();
		for (OWLImportsDeclaration oid : oids) {
			if (ontologyIRI.equals(oid.getIRI())) {
				RemoveImport ri = new RemoveImport(sourceOntology, oid);
				getManager().applyChange(ri);
			}
		}
	}


	/**
	 * in general application code need not call this - it is mostly used internally
	 * 
	 * @return union of source ontology plus all supporting ontologies plus their import closures
	 */
	public Set<OWLOntology> getAllOntologies() {
		Set<OWLOntology> all = new HashSet<OWLOntology>(getSupportOntologySet());
		for (OWLOntology o : getSupportOntologySet()) {
			all.addAll(o.getImportsClosure());
		}
		all.add(getSourceOntology());
		all.addAll(getSourceOntology().getImportsClosure());
		return all;
	}

	public OWLDataFactory getDataFactory() {
		return getManager().getOWLDataFactory();
	}

	public OWLOntologyManager getManager() {
		return sourceOntology.getOWLOntologyManager();
	}

	/**
	 * fetches all classes, individuals and object properties in all ontologies.
	 * This set is a copy. Changes are not reflected in the ontologies.
	 * 
	 * @return all named objects
	 */
	public Set<OWLObject> getAllOWLObjects() {
		Set<OWLObject> obs = new THashSet<OWLObject>();
		for (OWLOntology o : getAllOntologies()) {
			obs.addAll(o.getClassesInSignature());
			obs.addAll(o.getIndividualsInSignature());
			obs.addAll(o.getObjectPropertiesInSignature());
		}
		return obs;
	}

	/**
	 * Fetch all {@link OWLClass} objects from all ontologies. 
	 * This set is a copy. Changes are not reflected in the ontologies.
	 * 
	 * @return set of all {@link OWLClass}
	 */
	public Set<OWLClass> getAllOWLClasses() {
		Set<OWLClass> owlClasses = new THashSet<OWLClass>();
		for (OWLOntology o : getAllOntologies()) {
			owlClasses.addAll(o.getClassesInSignature());
		}
		return owlClasses;
	}
	
	/**
	 * Fetch all {@link OWLSubClassOfAxiom} axioms for a given subClass
	 * ({@link OWLClass}) from all ontologies. This set is a copy. Changes are
	 * not reflected in the ontologies.
	 * 
	 * @param owlClass
	 * @return set of all {@link OWLSubClassOfAxiom}
	 */
	public Set<OWLSubClassOfAxiom> getAllOWLSubClassOfAxiomsForSubClass(OWLClass owlClass) {
		Set<OWLSubClassOfAxiom> axioms = new THashSet<OWLSubClassOfAxiom>();
		for (OWLOntology o : getAllOntologies()) {
			axioms.addAll(o.getSubClassAxiomsForSubClass(owlClass));
		}
		return axioms;
	}
	
	/**
	 * Fetch all {@link OWLSubClassOfAxiom} axioms for a given superClass
	 * ({@link OWLClass}) from all ontologies. This set is a copy. Changes are
	 * not reflected in the ontologies.
	 * 
	 * @param owlClass
	 * @return set of all {@link OWLSubClassOfAxiom}
	 */
	public Set<OWLSubClassOfAxiom> getAllOWLSubClassOfAxiomsForSuperClass(OWLClass owlClass) {
		Set<OWLSubClassOfAxiom> axioms = new THashSet<OWLSubClassOfAxiom>();
		for (OWLOntology o : getAllOntologies()) {
			axioms.addAll(o.getSubClassAxiomsForSuperClass(owlClass));
		}
		return axioms;
	}
}

