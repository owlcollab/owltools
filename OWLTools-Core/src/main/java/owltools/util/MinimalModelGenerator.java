package owltools.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.semanticweb.HermiT.Reasoner.ReasonerFactory;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
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
import org.semanticweb.owlapi.model.OWLRestriction;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.util.OWLEntityRenamer;

/**
 * Generate a minimal models of a TBox or a subset of a TBox
 * 
 * @author cjm
 *
 */
public class MinimalModelGenerator {

	private static Logger LOG = Logger.getLogger(MinimalModelGenerator.class);

	private OWLReasonerFactory reasonerFactory = new ElkReasonerFactory();
	private OWLReasoner reasoner;
	private OWLOntology aboxOntology;
	private OWLOntology tboxOntology;
	private OWLOntology queryOntology;
	private String contextualizingSuffix;
	private Map<OWLClass,OWLClassExpression> queryClassMap;

	private Map<OWLClass, OWLNamedIndividual> prototypeIndividualMap =
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
		init();
	}

	public MinimalModelGenerator(OWLOntology tbox,
			ReasonerFactory reasonerFactory) throws OWLOntologyCreationException {
		tboxOntology = tbox;
		this.reasonerFactory = reasonerFactory;
		init();
	}
	private void init() throws OWLOntologyCreationException {
		if (aboxOntology == null) {
			IRI ontologyIRI = IRI.create(tboxOntology.getOntologyID().getOntologyIRI()+"__abox");
			aboxOntology = tboxOntology.getOWLOntologyManager().createOntology(ontologyIRI);
			AddImport ai = new AddImport(aboxOntology, 
					getOWLDataFactory().getOWLImportsDeclaration(tboxOntology.getOntologyID().getOntologyIRI()));
			getOWLOntologyManager().applyChange(ai);
		}
		if (queryOntology == null) {
			// Imports: {q imports a, a imports t}

			IRI ontologyIRI = IRI.create(tboxOntology.getOntologyID().getOntologyIRI()+"__query"); 
			queryOntology = aboxOntology.getOWLOntologyManager().createOntology(ontologyIRI);
			AddImport ai = new AddImport(queryOntology, 
					getOWLDataFactory().getOWLImportsDeclaration(aboxOntology.getOntologyID().getOntologyIRI()));
			getOWLOntologyManager().applyChange(ai);
		}

		if (contextualizingSuffix == null) {
			contextualizingSuffix = "-proto";
		}
		if (queryClassMap == null) {
			// TODO - we would like to do this before initializing the reasoner
		}
		if (reasoner == null) {			
			reasoner = reasonerFactory.createReasoner(queryOntology);
		}
	}



	public void setReasonerFactory(OWLReasonerFactory reasonerFactory) {
		this.reasonerFactory = reasonerFactory;
	}
	public OWLReasoner getReasoner() {
		return reasoner;
	}
	public void setReasoner(OWLReasoner reasoner) {
		this.reasoner = reasoner;
	}
	public OWLOntology getTboxOntology() {
		return tboxOntology;
	}
	public void setTboxOntology(OWLOntology tboxOntology) {
		this.tboxOntology = tboxOntology;
	}
	/**
	 * Note: ABox ontology should import TBox ontology
	 * @return
	 */
	public OWLOntology getAboxOntology() {
		return aboxOntology;
	}
	private String getContextualizingSuffix() {
		return contextualizingSuffix;
	}
	public void setContextualizingSuffix(String contextualizingSuffix) {
		this.contextualizingSuffix = contextualizingSuffix;
	}

	/**
	 * Generates a graph of ABox axioms rooted at proto(c), where proto(c) is
	 * the prototype individual of class c
	 * 
	 * @param c
	 * @return prototype individual of type c
	 */
	public OWLNamedIndividual generateNecessaryIndividuals(OWLClassExpression c) {
		LOG.info("GNI:"+c);
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
		if (c instanceof OWLObjectIntersectionOf) {
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

		reasoner.flush();
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
	 * Adds a prototypical individual to the abox
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


	private OWLClassExpression deepen(OWLClassExpression jType,
			OWLObjectPropertyExpression invProperty, OWLClassExpression incoming) {
		// attempt to deepen. E.g. if i=hand and j=digit, and p=has_part, then 
		// we can reason that (digit and inverseOf(has_part) some hand) is a subclass
		// of or equivalent to 'finger'
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
		reasoner.flush();
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
	 * @param i
	 * @return
	 */
	public OWLClassExpression getMostSpecificClassExpression(OWLNamedIndividual i) {
		return getMostSpecificClassExpression(i, new HashSet<OWLNamedIndividual>(), null);
	}
	public OWLClassExpression getMostSpecificClassExpression(OWLNamedIndividual i,
			Set<OWLNamedIndividual> visited,
			Set<OWLObjectProperty> propertySet) {
		visited.add(i);
		LOG.info("i="+i);
		Set<OWLClassExpression> elements = new HashSet<OWLClassExpression>();
		reasoner.flush();
		for (OWLClass typeClass : reasoner.getTypes(i, true).getFlattened()) {
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
		for (OWLObjectProperty p : aboxOntology.getObjectPropertiesInSignature(true)) {
			LOG.info(" p="+p);
			if (propertySet != null && !propertySet.contains(p))
				continue;
			for (OWLOntology ont : aboxOntology.getImportsClosure()) {
				for (OWLIndividual j : i.getObjectPropertyValues(p, ont)) {
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
						OWLClassExpression jce = getMostSpecificClassExpression((OWLNamedIndividual) j);
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
	private Set<OWLObjectSomeValuesFrom> getExistentialRelationships(OWLNamedIndividual ind) {
		//LOG.info("Querying: "+c);
		if (queryClassMap == null) {
			// TODO - document assumption that tbox does not change
			generateQueryOntology();
		}
		Set<OWLObjectSomeValuesFrom> results = new HashSet<OWLObjectSomeValuesFrom>();

		reasoner.flush();

		// all supers (direct and indirect)
		Set<OWLClass> supers = reasoner.getTypes(ind, false).getFlattened();

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

	@Deprecated
	private Set<OWLObjectSomeValuesFrom> old_____getExistentialRelationships(OWLClass c) {
		Set<OWLClassExpression> supers = c.getSuperClasses(tboxOntology);
		Set<OWLObjectSomeValuesFrom> results = new HashSet<OWLObjectSomeValuesFrom>();
		for (OWLClassExpression ec : c.getEquivalentClasses(tboxOntology)) {
			if (ec instanceof OWLObjectIntersectionOf) {
				supers.addAll(((OWLObjectIntersectionOf)ec).getOperands());
			}
		}
		for (OWLClassExpression s : supers) {
			if (s instanceof OWLObjectSomeValuesFrom)
				results.add((OWLObjectSomeValuesFrom)s);
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

		LOG.info("BM="+reasoner.getBufferingMode());
		reasoner.flush();

		// cross-product of P x C
		// TODO - reflexivity and local reflexivity?
		for (OWLObjectProperty p : tboxOntology.getObjectPropertiesInSignature(true)) {
			for (OWLClass c : tboxOntology.getClassesInSignature(true)) {
				OWLRestriction r = getOWLDataFactory().getOWLObjectSomeValuesFrom(p, c);
				LOG.info(" QMAP:"+r);
				addClassExpressionToQueryMap(r);
			}
		}

		// all expressions used in ontology
		for (OWLOntology ont : tboxOntology.getImportsClosure()) {
			for (OWLAxiom ax : tboxOntology.getAxioms()) {
				// TODO - check if this is the nest closure. ie (r some (r2 some (r3 some ...))) 
				for (OWLClassExpression x : ax.getNestedClassExpressions()) {
					LOG.info(" QMAP+:"+x);
					addClassExpressionToQueryMap(x);
				}
			}
		}
		reasoner.flush();
	}

	private void addClassExpressionToQueryMap(OWLClassExpression x) {
		if (!(x instanceof OWLObjectSomeValuesFrom)) {
			// in future we may support a wider variety of expressions - e.g. cardinality
			return;
		}
		if (!reasoner.isSatisfiable(x)) {
			LOG.info("Not adding unsatisfiable query expression:" +x);
			return;
		}
		IRI nxIRI = getSkolemIRI(x.getSignature());
		OWLClass nx = getOWLDataFactory().getOWLClass(nxIRI);
		OWLAxiom ax = getOWLDataFactory().getOWLEquivalentClassesAxiom(nx, x);
		addAxiom(ax, queryOntology);
		queryClassMap.put(nx, x);
	}

	private IRI getSkolemIRI(Set<OWLEntity> objs) {
		// TODO Auto-generated method stub
		IRI iri;
		StringBuffer sb = new StringBuffer();
		for (OWLEntity obj : objs) {
			sb.append("/"+getFragmentID(obj));
		}
		iri = IRI.create("http://x.org"+sb.toString());
		return iri;
	}

	private String getFragmentID(OWLObject obj) {
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
		aboxOntology.getOWLOntologyManager().addAxiom(ont, ax);
	}

	private OWLDataFactory getOWLDataFactory() {
		return getOWLOntologyManager().getOWLDataFactory();
	}

	private OWLOntologyManager getOWLOntologyManager() {
		return aboxOntology.getOWLOntologyManager();
	}




}