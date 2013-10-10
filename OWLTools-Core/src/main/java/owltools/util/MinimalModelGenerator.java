package owltools.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLPropertyExpression;
import org.semanticweb.owlapi.model.OWLRestriction;
import org.semanticweb.owlapi.model.RemoveImport;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.util.OWLEntityRenamer;

import uk.ac.manchester.cs.owlapi.modularity.ModuleType;
import uk.ac.manchester.cs.owlapi.modularity.SyntacticLocalityModuleExtractor;

/**
 * Generate a minimal models of a TBox or a subset of a TBox
 * 
 * @author cjm
 *
 */
public class MinimalModelGenerator {

	private static Logger LOG = Logger.getLogger(MinimalModelGenerator.class);

	private OWLReasonerFactory reasonerFactory = null;
	private OWLReasoner reasoner = null;
	private OWLOntology aboxOntology = null;
	private OWLOntology tboxOntology = null;
	private OWLOntology queryOntology = null;
	private String contextualizingSuffix = null;
	private Map<OWLClass,OWLClassExpression> queryClassMap = null;
	Map<OWLOntology,Set<OWLAxiom>> collectedAxioms = new HashMap<OWLOntology,Set<OWLAxiom>>();
	boolean isPrecomputePropertyClassCombinations = true;

	protected Map<OWLClass, OWLNamedIndividual> prototypeIndividualMap =
			new HashMap<OWLClass, OWLNamedIndividual>();

	/**
	 * The generator is seeded with a tbox (i.e. ontology). An abox will be created
	 * automatically
	 * 
	 * @param tbox
	 * @throws OWLOntologyCreationException
	 */
	public MinimalModelGenerator(OWLOntology tbox) throws OWLOntologyCreationException {
		tboxOntology = tbox;
		reasonerFactory = new ElkReasonerFactory();
		init();
	}
	/**
	 * Creates a generator with a pre-defined tbox (ontology) and abox (instance store).
	 * Note the abox should import the tbox
	 * 
	 * @param tbox
	 * @param abox
	 * @throws OWLOntologyCreationException
	 */
	public MinimalModelGenerator(OWLOntology tbox, OWLOntology abox) throws OWLOntologyCreationException {
		tboxOntology = tbox;
		aboxOntology = abox;
		reasonerFactory = new ElkReasonerFactory();
		init();
	}

	public MinimalModelGenerator(OWLOntology tbox,
			OWLReasonerFactory reasonerFactory) throws OWLOntologyCreationException {
		tboxOntology = tbox;
		this.reasonerFactory = reasonerFactory;
		init();
	}
	public MinimalModelGenerator(OWLOntology tbox, OWLOntology abox, OWLReasoner reasoner) throws OWLOntologyCreationException {
		tboxOntology = tbox;
		aboxOntology = abox;
		if (reasoner != null) {
			this.reasoner = reasoner;
			reasonerFactory = null;
		}
		else {
			reasonerFactory  = new ElkReasonerFactory();
		}
		init();
	}
	public MinimalModelGenerator(OWLOntology tbox, OWLOntology abox,
			OWLReasonerFactory rf) throws OWLOntologyCreationException {
		tboxOntology = tbox;
		aboxOntology = abox;
		reasonerFactory = rf;
		init();
	}

	/**
	 * Initialization consists of:
	 * <ul>
	 * <li>Setting aboxOntology, if not set - defaults to a new ontology using tbox.IRI as base. 
	 *   Adds import to tbox.
	 * <li>Setting queryOntology, if not set. Adds abox imports queryOntology declaration
	 * <li>Creates a reasoner using reasonerFactory
	 * </ul>
	 * 
	 * @throws OWLOntologyCreationException
	 */
	private void init() throws OWLOntologyCreationException {
		// reasoner -> query -> abox -> tbox
		if (aboxOntology == null) {
			LOG.info("Creating abox ontology. mgr = "+getOWLOntologyManager());
			IRI ontologyIRI = IRI.create(tboxOntology.getOntologyID().getOntologyIRI()+"__abox");
			aboxOntology = getOWLOntologyManager().getOntology(ontologyIRI);
			if (aboxOntology != null) {
				LOG.warn("Clearing existing abox ontology");
				getOWLOntologyManager().removeOntology(aboxOntology);
			}
			aboxOntology = getOWLOntologyManager().createOntology(ontologyIRI);
			AddImport ai = new AddImport(aboxOntology, 
					getOWLDataFactory().getOWLImportsDeclaration(tboxOntology.getOntologyID().getOntologyIRI()));
			getOWLOntologyManager().applyChange(ai);
		}
		if (queryOntology == null) {
			// Imports: {q imports a, a imports t}
			LOG.info("Creating query ontology");

			IRI ontologyIRI = IRI.create(tboxOntology.getOntologyID().getOntologyIRI()+"__query"); 
			queryOntology = getOWLOntologyManager().getOntology(ontologyIRI);
			if (queryOntology == null) {
				queryOntology = getOWLOntologyManager().createOntology(ontologyIRI);
			}
			AddImport ai = new AddImport(queryOntology, 
					getOWLDataFactory().getOWLImportsDeclaration(aboxOntology.getOntologyID().getOntologyIRI()));
			getOWLOntologyManager().applyChange(ai);
		}
		LOG.info("manager(T) = "+tboxOntology.getOWLOntologyManager());
		LOG.info("manager(A) = "+aboxOntology.getOWLOntologyManager());
		LOG.info("manager(Q) = "+queryOntology.getOWLOntologyManager());

		if (contextualizingSuffix == null) {
			contextualizingSuffix = "-proto";
		}
		if (queryClassMap == null) {
			// TODO - we would like to do this before initializing the reasoner
		}
		if (reasoner == null) {			
			createReasoner();
		}
	}

	private OWLReasoner createReasoner() {
		// reasoner -> query -> abox -> tbox
		if (reasoner == null) {
			LOG.info("Creating reasoner on "+queryOntology);
			reasoner = reasonerFactory.createReasoner(queryOntology);
		}
		else {
			LOG.info("reusing reasoner: "+reasoner);
		}
		return reasoner;
	}

	protected void disposeReasoner() {
		if (reasoner != null) {
			reasoner.dispose();
			reasoner = null;
		}
	}

	/**
	 * The reasoner factory is used during intialization to
	 * generate a reasoner abject using abox as ontology
	 * 
	 * @param reasonerFactory
	 */
	public void setReasonerFactory(OWLReasonerFactory reasonerFactory) {
		this.reasonerFactory = reasonerFactory;
	}

	/**
	 * @return current reasoner, operating over abox
	 */
	public OWLReasoner getReasoner() {
		return reasoner;
	}
	/**
	 * @param reasoner
	 */
	public void setReasoner(OWLReasoner reasoner) {
		this.reasoner = reasoner;
	}

	/**
	 * The tbox ontology should contain class axioms used to generate minimal models in the
	 * abox ontology.
	 * 
	 * May be the same as abox, in which case generated abox axioms go in the same ontology 
	 * 
	 * @return tbox
	 */
	public OWLOntology getTboxOntology() {
		return tboxOntology;
	}
	/**
	 * @param tboxOntology
	 */
	public void setTboxOntology(OWLOntology tboxOntology) {
		this.tboxOntology = tboxOntology;
	}
	/**
	 * Note: ABox ontology should import TBox ontology
	 * @return abox
	 */
	public OWLOntology getAboxOntology() {
		return aboxOntology;
	}
	public OWLOntology getQueryOntology() {
		return queryOntology;
	}
	
	private String getContextualizingSuffix() {
		return contextualizingSuffix;
	}
	/**
	 * @param contextualizingSuffix TODO
	 */
	public void setContextualizingSuffix(String contextualizingSuffix) {
		this.contextualizingSuffix = contextualizingSuffix;
	}

	public boolean isPrecomputePropertyClassCombinations() {
		return isPrecomputePropertyClassCombinations;
	}
	public void setPrecomputePropertyClassCombinations(
			boolean isPrecomputePropertyClassCombinations) {
		this.isPrecomputePropertyClassCombinations = isPrecomputePropertyClassCombinations;
	}
	/**
	 * @return all individuals that have been generated by this MMG so far
	 */
	public Collection<OWLNamedIndividual> getGeneratedIndividuals() {
		return prototypeIndividualMap.values();
	}

	/**
	 * Note that the reasoner may be able to provide a more specific class
	 * @param i
	 * @return class that was used to generate i
	 */
	protected OWLClass getPrototypeClass(OWLNamedIndividual i) {
		if (prototypeIndividualMap.containsValue(i)) {
			for (OWLClass c : prototypeIndividualMap.keySet()) {
				if (prototypeIndividualMap.get(c).equals(i)) {
					return c;
				}
			}
		}
		return null;
	}


	/**
	 * Generates a graph of ABox axioms rooted at proto(c), where proto(c) is
	 * the prototype individual of class c.
	 * 
	 * For example, if c SubClassOf finger, SubClassOf part_of some some hand, then
	 * generate a finger individual, a hand individual, connect them via part_of  
	 * 
	 * 
	 * @param c
	 * @return prototype individual of type c
	 */
	public OWLNamedIndividual generateNecessaryIndividuals(OWLClassExpression c) {
		LOG.info("GNI type:"+c);
		if (prototypeIndividualMap.containsKey(c)) {
			// we assume a single prototype per class;
			// this also prevents cycles
			return prototypeIndividualMap.get(c);
		}
		Set<OWLObjectSomeValuesFrom> rels = new HashSet<OWLObjectSomeValuesFrom>();
		OWLClassExpression baseClass = null;
		if (c instanceof OWLClass) {
			// all is good
			baseClass = c;
		}
		else if (c instanceof OWLObjectIntersectionOf) {
			// decompose expression
			for (OWLClassExpression x : ((OWLObjectIntersectionOf)c).getOperands()) {
				if (x instanceof OWLObjectSomeValuesFrom) {
					rels.add((OWLObjectSomeValuesFrom) x);
				}
				else if (x instanceof OWLClass) {
					if (c instanceof OWLClass) {
						LOG.warn("Cannot handle expressions with >1 genus");
					}
					c = x;
					baseClass = x;
				}
				else {
					LOG.warn("Ignoring: "+x+" in "+c);
				}
			}
		}
		else if (c instanceof OWLObjectSomeValuesFrom) {
			rels.add((OWLObjectSomeValuesFrom) c);
			c = getOWLDataFactory().getOWLThing();
			baseClass = getOWLDataFactory().getOWLThing();
		}
		else {
			LOG.warn("Using unhandled class expression: "+c);
			baseClass = c;
		}

		OWLNamedIndividual sourceIndividual = generateBaseIndividual(c);
		LOG.info(" I:"+sourceIndividual);

		rels.addAll(getExistentialRelationships(sourceIndividual));
		for (OWLObjectSomeValuesFrom rel : rels) {
			LOG.info("  Rel: "+rel);
			OWLClassExpression jType = deepen(rel.getFiller(), rel.getProperty(), c);

			OWLNamedIndividual targetIndividual =
					generateNecessaryIndividuals(jType);
			addTriple(sourceIndividual, rel.getProperty(), targetIndividual);
		}

		return sourceIndividual;
	}

	/**
	 * Calls {@link #generateNecessaryIndividuals(OWLClassExpression)}, and if isCollapse is set,
	 * will call {@link #collapseIndividuals()}
	 * 
	 * @param c
	 * @param isCollapse
	 * @return
	 */
	public OWLNamedIndividual generateNecessaryIndividuals(OWLClass c, boolean isCollapse) {
		OWLNamedIndividual ind = generateNecessaryIndividuals(c);
		if (isCollapse) {
			collapseIndividuals();
		}
		return ind;
	}

	/**
	 * Adds a prototypical individual to the abox.
	 * 
	 * fills in prototypeIndividualMap, unless c satisfies {@link #isNeverMerge(c)}
	 * 
	 * @param c
	 * @return
	 */
	private OWLNamedIndividual generateBaseIndividual(OWLClassExpression c) {
		IRI iri;
		boolean isUniq = true;
		if (c instanceof OWLClass  && !this.isNeverMerge((OWLClass)c)) {
			iri = IRI.create(((OWLClass) c).getIRI().toString()+getContextualizingSuffix());
		}
		else {
			iri = IRI.create(aboxOntology.getOntologyID().getOntologyIRI()+"-"+UUID.randomUUID());
			isUniq = false;
		}
		OWLNamedIndividual ind = getOWLDataFactory().getOWLNamedIndividual(iri);
		addAxiom(getOWLDataFactory().getOWLClassAssertionAxiom(c, ind));
		if (isUniq) {
			prototypeIndividualMap.put((OWLClass) c, ind);
		}

		return ind;
	}


	/**
	 * attempt to deepen a class expression based on properties the individual holds.
	 * 
	 * E.g. if i=hand and j=digit, and p=has_part, then 
	 * we can reason that (digit and inverseOf(has_part) some hand) is a subclass
	 * of or equivalent to 'finger'
	 * 
	 * @param jType
	 * @param invProperty
	 * @param incoming
	 * @return
	 */
	private OWLClassExpression deepen(OWLClassExpression jType,
			OWLObjectPropertyExpression invProperty, OWLClassExpression incoming) {
		OWLObjectIntersectionOf jExpr = getOWLDataFactory().getOWLObjectIntersectionOf(
				jType,
				getOWLDataFactory().getOWLObjectSomeValuesFrom(
						getOWLDataFactory().getOWLObjectInverseOf(invProperty),
						incoming)
				);
		Set<OWLClass> deeperClasses = reasoner.getSuperClasses(
				jExpr,
				true).getFlattened();
		deeperClasses.addAll(reasoner.getEquivalentClasses(jExpr).getEntities());
		for (OWLClass dc: deeperClasses) {
			LOG.info(" DC="+dc);
			// don't include artificial
			if (queryClassMap.containsKey(dc))
				continue;
			if (reasoner.getSuperClasses(dc, false).getFlattened().contains(jType)) {
				LOG.info("   DEEPENED="+dc);
				// must be more specific that original choice
				jType = dc; // TODO - check for >1
			}
		}
		return jType;
	}


	/**
	 * generation of prototypes often generate multiple individuals where there
	 * should in fact be one. E.g.
	 * 
	 * <pre>
	 * hand SubClassOf: autopod, part_of some forelimb
	 * autopod SubClassOf: part_of some limb
	 * </pre>
	 * 
	 * here the generated instance graph is
	 * 
	 * <pre>
	 * hand-1 part_of forelimb-1
	 *        part_of some limb-1
	 * </pre>
	 *	
	 * There is nothing in the OWL to state that these are the same individual, but
	 * we make that assumption here.
	 * 
	 * 
	 * 
	 */
	public void collapseIndividuals() {
		LOG.info("Collapsing...");
		Map<OWLClass,OWLClass> mergeMap =
				new HashMap<OWLClass,OWLClass>();
		Set<OWLClass> hasMultipleCandidates = new HashSet<OWLClass>();
		Set<OWLClass> usedClasses = prototypeIndividualMap.keySet();
		for (OWLClass targetSpecificClass : usedClasses) {
			// e.g. forelimb (target)
			LOG.info(" testing individuals of SPECIFIC type: "+targetSpecificClass);

			// e.g. limb (candidate merge: limb -> forelimb)
			Set<OWLClass> mergeCandidates = reasoner.getSuperClasses(targetSpecificClass, false).getFlattened();

			// don't bother attempting to merge instances of classes that were not used
			mergeCandidates.retainAll(usedClasses);

			for (OWLClass sourceGenericClass : mergeCandidates) {
				// sourceGenericClass is a candidate for merging into targetSpecificClass
				if (isNeverMerge(sourceGenericClass))
					continue;

				// avoid splits: check if a merge has already been proposed for the source
				if (mergeMap.containsKey(sourceGenericClass)) {

					// e.g. src = limb, tgt = forelimb, existing = hindlimb
					OWLClass existingTarget = mergeMap.get(sourceGenericClass);

					if (reasoner.getSuperClasses(sourceGenericClass, false).
							getFlattened().contains(existingTarget)) {
						// overwrite existing entity if more specific.
						//  src=forelimb, existing=limb, tgt=left_forelimb
						// TODO? this: src = limb, tgt = left_forelimb, existing=forelimb
						mergeMap.put(sourceGenericClass, targetSpecificClass);
					}
					else {
						// this blocks the merge of s
						hasMultipleCandidates.add(sourceGenericClass);
					}
				}
				else {
					mergeMap.put(sourceGenericClass, targetSpecificClass);
				}

				//Set<OWLClass> subs = reasoner.getSubClasses(s, false).getFlattened();
				//subs.retainAll(usedClasses);
			}
		}

		// perform merge
		OWLEntityRenamer renamer = new OWLEntityRenamer(aboxOntology.getOWLOntologyManager(), 
				Collections.singleton(aboxOntology));
		for (OWLClass src : mergeMap.keySet()) {
			OWLNamedIndividual sourceIndividual = prototypeIndividualMap.get(src);
			OWLNamedIndividual targetIndividual = prototypeIndividualMap.get(mergeMap.get(src));

			LOG.info("  ?collapsing "+sourceIndividual+ " -> "+targetIndividual);
			if (hasMultipleCandidates.contains(src)) {
				LOG.info("   **multi-candidate");
				continue;
			}
			LOG.info("  ACTUALLY collapsing "+sourceIndividual+ " -> "+targetIndividual);
			prototypeIndividualMap.remove(src);
			applyChanges(renamer.changeIRI(sourceIndividual.getIRI(),
					targetIndividual.getIRI()));
		}
	}

	private boolean isNeverMerge(OWLClass c) {
		if (c.equals(getOWLDataFactory().getOWLNothing()))
			return true;
		// TODO - customize
		return false;
	}

	public void anonymizeIndividualsNotIn(OWLClass c) {
		anonymizeIndividualsNotIn(Collections.singleton(c));
	}

	public void anonymizeIndividualsNotIn(Set<OWLClass> cs) {
		Set<OWLNamedIndividual> inds = new HashSet<OWLNamedIndividual>();
		for (OWLNamedIndividual ind : aboxOntology.getIndividualsInSignature(true)) {
			Set<OWLClass> sups = reasoner.getTypes(ind, false).getFlattened();
			sups.retainAll(cs);
			if (sups.size() == 0) {
				LOG.info("SCHEDULING: "+ind);
				inds.add(ind);
			}
		}
		anonymizeIndividuals(inds);
	}


	public void anonymizeIndividuals(Set<OWLNamedIndividual> inds) {
		for (OWLNamedIndividual ind : inds) {
			anonymizeIndividual(ind);
		}
	}

	private void anonymizeIndividual(OWLNamedIndividual ind) {
		Set<OWLClass> types = reasoner.getTypes(ind, true).getFlattened();
		LOG.info("ANONYMIZING:"+ind);
		// replace incoming with class expressions
		Set<OWLAxiom> refAxioms = ind.getReferencingAxioms(aboxOntology, false);
		for (OWLAxiom ax : refAxioms) {
			if (ax instanceof OWLObjectPropertyAssertionAxiom) {
				OWLObjectPropertyAssertionAxiom pa = (OWLObjectPropertyAssertionAxiom)ax;
				for (OWLClass type : types) {
					if (type.equals(getOWLDataFactory().getOWLNothing()))
						continue;
					OWLClassExpression classExpression =
							getOWLDataFactory().getOWLObjectSomeValuesFrom(pa.getProperty(), type);
					OWLClassAssertionAxiom newAx = 
							getOWLDataFactory().getOWLClassAssertionAxiom(classExpression, pa.getSubject());
					addAxiom(newAx);
				}
			}
		}
		// remove incoming
		getOWLOntologyManager().removeAxioms(aboxOntology, refAxioms);

		// remove outgoing
		getOWLOntologyManager().removeAxioms(aboxOntology, aboxOntology.getAxioms(ind));

		// remove delcarations
		getOWLOntologyManager().removeAxiom(aboxOntology, getOWLDataFactory().getOWLDeclarationAxiom(ind));
	}
	/**
	 * Generate a class expression CE such that i instantiates CE, and there
	 * is no other CE' such that CE' is more specific that CE, and i instantiates CE',
	 * and CE' is guaranteed to subsume i even if additional facts are added to the ABox
	 * or TBox
	 * 
	 * This is equivalent to (TODO: proof) the class expression formed by walking
	 * the ABox graph starting from i, creating an existential restriction for each edge
	 * and intersecting it with the type of each node.
	 * 
	 * For more sophisticated algorithms, see the DL-learner package, which will
	 * abduce over multiple instances.
	 * 
	 * Alternatively we should start with the maximal path spanning all nodes - TODO
	 * http://en.wikipedia.org/wiki/Widest_path_problem
	 * (here it is actually the maximal widest path for all possible sink nodes)
	 * 
	 * @param i
	 * @return
	 */
	public OWLClassExpression getMostSpecificClassExpression(OWLNamedIndividual i) {
		return getMostSpecificClassExpression(i, new HashSet<OWLNamedIndividual>(), null);
	}
	public OWLClassExpression getMostSpecificClassExpression(
			OWLNamedIndividual i, List<OWLObjectProperty> propertySet) {
		return getMostSpecificClassExpression(i, new HashSet<OWLNamedIndividual>(), propertySet);
	}
	public OWLClassExpression getMostSpecificClassExpression(OWLNamedIndividual i,
			Set<OWLNamedIndividual> visited,
			List<OWLObjectProperty> propertySet) {
		visited.add(i);
		LOG.info("i="+i);
		Set<OWLClassExpression> elements = new HashSet<OWLClassExpression>();
		reasoner.flush();
		for (OWLClass typeClass : reasoner.getTypes(i, true).getFlattened()) {
			LOG.info(" t="+typeClass);
			if (queryClassMap != null && queryClassMap.containsKey(typeClass))
				continue;
			elements.add(typeClass);
		}
		// supplement this with asserted class expression types
		for (OWLClassExpression x : i.getTypes(aboxOntology.getImportsClosure())) {
			if (x.isAnonymous()) {
				Set<OWLClass> xDescs = reasoner.getSubClasses(x, false).getFlattened();
				xDescs.retainAll(elements);
				if (xDescs.size() == 0) {
					elements.add(x);
				}
			}
		}
		if (propertySet == null || propertySet.size() == 0) {
			propertySet = new ArrayList<OWLObjectProperty>();
			for (OWLObjectProperty p : aboxOntology.getObjectPropertiesInSignature(true)) {
				propertySet.add(p);
			}
		}

		for (OWLObjectProperty p : propertySet) {
			LOG.info(" p="+p);

			Set<OWLPropertyExpression> invProps = new HashSet<OWLPropertyExpression>();
			for (OWLOntology ont : aboxOntology.getImportsClosure()) {
				invProps.addAll(p.getInverses(ont));
			}
			for (OWLOntology ont : aboxOntology.getImportsClosure()) {
				Set<OWLIndividual> js = new HashSet<OWLIndividual>(i.getObjectPropertyValues(p, ont));
				// todo - make this more efficient
				if (invProps.size() > 0) {
					for (OWLPropertyExpression invProp : invProps) {
						LOG.info(" invP="+invProp);
						for (OWLIndividual j : aboxOntology.getIndividualsInSignature(true)) {
							if (j.getObjectPropertyValues((OWLObjectPropertyExpression) invProp, ont).contains(i)) {
								js.add(j);
							}
						}
					}
				}
				for (OWLIndividual j : js) {
					LOG.info("  j="+j);

					// note that as this method is recursive, it is possible to end up with
					// multiple paths. this could be resolved by rewriting to perform BF-search,
					// or by making the visitor set object-wide (should be synchronized)
					if (visited.contains(j)) {
						// TODO - provide an option to include back the original class
						//  for example, if we have a cycle of carbon atoms.
						// only do this for non-trivial cycles
						continue;
					}
					if (j instanceof OWLNamedIndividual) {
						OWLClassExpression jce = getMostSpecificClassExpression((OWLNamedIndividual) j, visited, propertySet);
						LOG.info("    jce="+jce);
						elements.add(
								getOWLDataFactory().getOWLObjectSomeValuesFrom(p, jce)
								);
					}
					else {
						LOG.warn("I wasn't expecting anonymous individuals in the abox: "+j);
					}
				}
			}
		}
		return getOWLDataFactory().getOWLObjectIntersectionOf(elements);
	}

	// UTIL



	/**
	 * @param c
	 * @return
	 */
	protected Set<OWLObjectSomeValuesFrom> getExistentialRelationships(OWLNamedIndividual ind) {
		//LOG.info("Querying: "+c);
		if (queryClassMap == null) {
			// TODO - document assumption that tbox does not change
			generateQueryOntology();
		}
		Set<OWLObjectSomeValuesFrom> results = new HashSet<OWLObjectSomeValuesFrom>();

		reasoner.flush();

		// all supers (direct and indirect)
		Set<OWLClass> supers = reasoner.getTypes(ind, false).getFlattened();
		LOG.info("Supers for "+ind+" is "+supers.size());

		// we only case about expressions in the query ontology, which should have
		// all expressions required
		supers.retainAll(queryClassMap.keySet());
		LOG.info("Supers in QMAP for "+ind+" is "+supers.size());

		// use only classes that are non-redundant (within QSet)
		Set<OWLClass> nrSet = new HashSet<OWLClass>(supers);
		for (OWLClass s : supers) {
			nrSet.removeAll(reasoner.getSuperClasses(s, false).getFlattened());
		}

		// map from materialized class to original expression
		for (OWLClass s : nrSet) {
			LOG.info(" SUP:"+s);
			OWLClassExpression x = queryClassMap.get(s);
			if (x instanceof OWLObjectSomeValuesFrom) {
				//LOG.info("  Result:"+x);
				results.add((OWLObjectSomeValuesFrom)x);
			}
			else {
				LOG.warn("Skipping: "+x+" (future versions may handle this)");
			}
		}
		return results;
	}

	protected Set<OWLObjectSomeValuesFrom> getExistentialRelationships(OWLClass c) {
		//LOG.info("Querying: "+c);
		if (queryClassMap == null) {
			// TODO - document assumption that tbox does not change
			generateQueryOntology();
		}
		Set<OWLObjectSomeValuesFrom> results = new HashSet<OWLObjectSomeValuesFrom>();

		reasoner.flush();

		// all supers (direct and indirect)
		Set<OWLClass> supers = reasoner.getSuperClasses(c, false).getFlattened();
		supers.addAll(reasoner.getEquivalentClasses(c).getEntities());

		// we only case about expressions in the query ontology, which should have
		// all expressions required
		supers.retainAll(queryClassMap.keySet());

		// use only classes that are non-redundant (within QSet)
		Set<OWLClass> nrSet = new HashSet<OWLClass>(supers);
		for (OWLClass s : supers) {
			nrSet.removeAll(reasoner.getSuperClasses(s, false).getFlattened());
		}

		// map from materialized class to original expression
		for (OWLClass s : nrSet) {
			LOG.info(" SUP:"+s);
			OWLClassExpression x = queryClassMap.get(s);
			if (x instanceof OWLObjectSomeValuesFrom) {
				//LOG.info("  Result:"+x);
				results.add((OWLObjectSomeValuesFrom)x);
			}
			else {
				LOG.warn("Skipping: "+x+" (future versions may handle this)");
			}
		}
		return results;
	}


	/**
	 * <b>Motivation</b>: OWL reasoners do not return superclass expressions
	 * If we want to find all class expressions that may hold for a class
	 * then we must pre-coordinate
	 * all possible expressions within the subset of OWL we care about.
	 * <br/>
	 * This class generates all satisfiable class expressions of the form
	 * r some c (for the cross-product of R x C), as well as all
	 * class expressions that have been used (which may include nested expressions)
	 * 
	 * The results are stored in queryClassMap
	 */
	private void generateQueryOntology() {
		queryClassMap = new HashMap<OWLClass,OWLClassExpression>(); 

		reasoner.flush();

		if (isPrecomputePropertyClassCombinations) {
			LOG.info("Precomputing all OP x Class combos");
			// cross-product of P x C
			// TODO - reflexivity and local reflexivity?
			for (OWLObjectProperty p : tboxOntology.getObjectPropertiesInSignature(true)) {
				LOG.info(" materializing P some C for P=:"+p);
				for (OWLClass c : tboxOntology.getClassesInSignature(true)) {
					OWLRestriction r = getOWLDataFactory().getOWLObjectSomeValuesFrom(p, c);
					//LOG.info(" QMAP:"+r);
					addClassExpressionToQueryMap(r);
				}
			}
		}

		// all expressions used in ontology
		for (OWLOntology ont : tboxOntology.getImportsClosure()) {
			LOG.info("Finding all nested anonymous expressions");
			for (OWLAxiom ax : tboxOntology.getAxioms()) {
				// TODO - check if this is the nest closure. ie (r some (r2 some (r3 some ...))) 
				for (OWLClassExpression x : ax.getNestedClassExpressions()) {
					if (x.isAnonymous()) {
						//LOG.info(" QMAP+:"+x);
						addClassExpressionToQueryMap(x);
					}
				}
			}
		}
		for (OWLOntology ont : collectedAxioms.keySet()) {
			LOG.info("TOTAL axioms in QMAP: "+collectedAxioms.get(ont).size());
		}
		addCollectedAxioms();
		reasoner.flush();
	}

	private void addClassExpressionToQueryMap(OWLClassExpression x) {
		if (!(x instanceof OWLObjectSomeValuesFrom)) {
			// in future we may support a wider variety of expressions - e.g. cardinality
			return;
		}
		// this makes things too slow
		//if (!reasoner.isSatisfiable(x)) {
		//	LOG.info("Not adding unsatisfiable query expression:" +x);
		//	return;
		//}
		IRI nxIRI = getSkolemIRI(x.getSignature());
		OWLClass nx = getOWLDataFactory().getOWLClass(nxIRI);
		OWLAxiom ax = getOWLDataFactory().getOWLEquivalentClassesAxiom(nx, x);
		collectAxiom(ax, queryOntology);
		queryClassMap.put(nx, x);
	}

	protected IRI getSkolemIRI(OWLEntity... objsArr) {
		return getSkolemIRI(new HashSet<OWLEntity>(Arrays.asList(objsArr)));
	}
	protected IRI getSkolemIRI(Set<OWLEntity> objs) {
		// TODO Auto-generated method stub
		IRI iri;
		StringBuffer sb = new StringBuffer();
		for (OWLEntity obj : objs) {
			sb.append("/"+getFragmentID(obj));
		}
		iri = IRI.create("http://x.org"+sb.toString());
		return iri;
	}

	protected String getFragmentID(OWLObject obj) {
		if (obj instanceof OWLNamedObject) {
			return ((OWLNamedObject) obj).getIRI().toString().replaceAll(".*/", "");
		}
		return UUID.randomUUID().toString();
	}

	// ABOX ONTOLOGY MANAGEMENT


	private void addTriple(OWLNamedIndividual sourceIndividual,
			OWLObjectPropertyExpression property,
			OWLNamedIndividual targetIndividual) {
		OWLAxiom ax = getOWLDataFactory().getOWLObjectPropertyAssertionAxiom(property, sourceIndividual, targetIndividual);
		addAxiom(ax);
	}

	private void applyChanges(List<OWLOntologyChange> changeIRI) {
		getOWLOntologyManager().applyChanges(changeIRI);
	}

	private void addAxiom(OWLAxiom ax) {
		addAxiom(ax, aboxOntology);
	}
	private void addAxiom(OWLAxiom ax, OWLOntology ont) {
		LOG.info("Adding: "+ax+" to "+ont);
		getOWLOntologyManager().addAxiom(ont, ax);
	}


	/**
	 * Collects an axiom to be added to ont at some later time.
	 * Cal {@link #addCollectedAxioms()} to add these
	 * 
	 * @param ax
	 * @param ont
	 */
	protected void collectAxiom(OWLAxiom ax, OWLOntology ont) {
		//LOG.info("Collecting: "+ax+" to "+ont);
		if (!collectedAxioms.containsKey(ont))
			collectedAxioms.put(ont, new HashSet<OWLAxiom>());
		collectedAxioms.get(ont).add(ax);
	}

	/**
	 * Adds all collected axioms to their specified destination 
	 */
	protected void addCollectedAxioms() {
		for (OWLOntology ont : collectedAxioms.keySet())
			addCollectedAxioms(ont);
	}

	/**
	 * Adds all collected axioms to ont
	 * @param ont
	 */
	private void addCollectedAxioms(OWLOntology ont) {
		if (collectedAxioms.containsKey(ont)) {
			getOWLOntologyManager().addAxioms(ont, collectedAxioms.get(ont));
			collectedAxioms.remove(ont);
		}

	}

	/**
	 * @return data factory for tbox
	 */
	protected OWLDataFactory getOWLDataFactory() {
		return getOWLOntologyManager().getOWLDataFactory();
	}

	/**
	 * @return ontology manager for tbox
	 */
	protected OWLOntologyManager getOWLOntologyManager() {
//		if (aboxOntology != null) {
//			return aboxOntology.getOWLOntologyManager();
//		}
		return tboxOntology.getOWLOntologyManager();
	}

	/**
	 * Extract a module from tboxOntology using aboxOntology as seed.
	 * As a side-effect, will remove abox imports tbox axiom, and add extracted axioms
	 * to abox.
	 * 
	 * 
	 * @see SyntacticLocalityModuleExtractor
	 */
	public void extractModule() {
		SyntacticLocalityModuleExtractor sme = new SyntacticLocalityModuleExtractor(getOWLOntologyManager(),
				aboxOntology,
				ModuleType.BOT);

		Set<OWLEntity> objs = new HashSet<OWLEntity>();
		objs.addAll( aboxOntology.getObjectPropertiesInSignature() );
		objs.addAll( aboxOntology.getClassesInSignature() );


		Set<OWLAxiom> modAxioms = sme.extract(objs);
		for (OWLImportsDeclaration oid : aboxOntology.getImportsDeclarations()) {
			getOWLOntologyManager().applyChange(new RemoveImport(aboxOntology, oid));
		}
		getOWLOntologyManager().addAxioms(aboxOntology, modAxioms);
	}



}