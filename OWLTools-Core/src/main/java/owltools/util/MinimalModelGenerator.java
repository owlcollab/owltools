package owltools.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.UUID;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.RemoveImport;
import org.semanticweb.owlapi.util.OWLEntityRenamer;

import uk.ac.manchester.cs.owlapi.modularity.ModuleType;
import uk.ac.manchester.cs.owlapi.modularity.SyntacticLocalityModuleExtractor;

/**
 * Given a class C, materialize a minimal ABox M from the existential graph of C.
 * 
 * The graph will have a root instance i, where type(i,C) and i is connected to every node in the graph directly,
 * or indirectly via instances of other classes entailed to exist.
 *
 * 
 * @author cjm
 *
 */
public class MinimalModelGenerator {

	private static Logger LOG = Logger.getLogger(MinimalModelGenerator.class);
	static {
		Logger.getLogger("org.semanticweb.elk").setLevel(Level.ERROR);
	}
	protected final ModelContainer model;

	private String contextualizingSuffix = null;
	boolean isPrecomputePropertyClassCombinations = true;
	boolean isAssertInverses = true;
	boolean isRemoveAmbiguousIndividuals = true;
	Set<OWLClass> inclusionSet = null;
	Set<OWLClass> exclusionSet = null;
	Set<OWLClass> directExclusionSet = null;
	public boolean isStrict;

	protected Map<OWLClass, OWLNamedIndividual> prototypeIndividualMap =
			new HashMap<OWLClass, OWLNamedIndividual>();

	public MinimalModelGenerator(ModelContainer model) throws OWLOntologyCreationException {
		this.model = model;
		init();
	}

	public ModelContainer getModel() {
		return model;
	}
	
	/**
	 * {@link #setAssertInverses(boolean)}
	 * 
	 * @return true is inverses are to be asserted
	 */
	public boolean isAssertInverses() {
		return isAssertInverses;
	}
	/**
	 * If set, every asserted OPE(P X Y) will also generate OPE(P' Y X) where P' InverseOf P
	 * @param isAssertInverses
	 */
	public void setAssertInverses(boolean isAssertInverses) {
		this.isAssertInverses = isAssertInverses;
	}


	public boolean isRemoveAmbiguousIndividuals() {
		return isRemoveAmbiguousIndividuals;
	}

	/**
	 * @param isRemoveAmbiguousIndividuals
	 */
	public void setRemoveAmbiguousIndividuals(boolean isRemoveAmbiguousIndividuals) {
		this.isRemoveAmbiguousIndividuals = isRemoveAmbiguousIndividuals;
	}
	
	

	public Set<OWLClass> getInclusionSet() {
		return inclusionSet;
	}
	public void setInclusionSet(Set<OWLClass> inclusionSet) {
		this.inclusionSet = inclusionSet;
	}
	public Set<OWLClass> getExclusionSet() {
		return exclusionSet;
	}
	public void setExclusionSet(Set<OWLClass> exclusionSet) {
		this.exclusionSet = exclusionSet;
	}
	public Set<OWLClass> getDirectExclusionSet() {
		return directExclusionSet;
	}
	public void setDirectExclusionSet(Set<OWLClass> directExclusionSet) {
		this.directExclusionSet = directExclusionSet;
	}
	/**
	 * Initialization consists of:
	 * 
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
		if (contextualizingSuffix == null) {
			contextualizingSuffix = "-proto";
		}
	}

//	private OWLReasoner createReasoner() {
//		// reasoner -> query -> abox -> tbox
//		if (reasoner == null) {
//			LOG.debug("Creating reasoner on "+queryOntology+" ImportsClosure="+
//					queryOntology.getImportsClosure());
//			reasoner = reasonerFactory.createReasoner(queryOntology);
//		}
//		else {
//			LOG.debug("reusing reasoner: "+reasoner);
//			LOG.warn("check reasoning is pointing to query ontology");
//		}
//		return reasoner;
//	}

	private String getContextualizingSuffix() {
		return contextualizingSuffix;
	}
	/**
	 * @param contextualizingSuffix TODO
	 */
	public void setContextualizingSuffix(String contextualizingSuffix) {
		this.contextualizingSuffix = contextualizingSuffix;
	}

	/**
	 * @return true if configured to precompute PxC query classes
	 */
	public boolean isPrecomputePropertyClassCombinations() {
		return isPrecomputePropertyClassCombinations;
	}



	/**
	 * If set, all combinations of PxC will be generated for query purposes
	 * 
	 * This is not necessary for minimal model generation, as the set of
	 * existential expressions used in the tbox is sufficient to calculate
	 * the existential graph, but this precompute may be useful for other
	 * purposes - e.g. finding all ancestors over any relations
	 * 
	 * @param isPrecomputePropertyClassCombinations
	 */
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

	// Generate an individual <generatedIndividual> and a triple
	// [ incomingSource, incomingProperty, generatedIndividual ]
	// where generatedIndividual is of type.
	// will also generate additional triples, where existence is entailed
	private OWLNamedIndividual generateNecessaryIndividualsImpl(OWLClassExpression c, 
			OWLNamedIndividual incomingSource, OWLObjectPropertyExpression incomingProperty) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("GNI type:"+c+" // src="+incomingSource+" via "+incomingProperty);
		}
		OWLNamedIndividual generatedIndividual;
		if (prototypeIndividualMap.containsKey(c)) {
			// we assume a single prototype per class;
			// this also prevents cycles

			generatedIndividual = prototypeIndividualMap.get(c);
			if (incomingProperty != null) {
				addTriple(incomingSource, incomingProperty, generatedIndividual);
			}
			return generatedIndividual;
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
			c = model.getOWLDataFactory().getOWLThing();
			baseClass = model.getOWLDataFactory().getOWLThing();
		}
		else {
			LOG.warn("Using unhandled class expression: "+c);
			baseClass = c;
		}

		generatedIndividual = generateBaseIndividual(c);
		LOG.debug(" I:"+generatedIndividual);

		if (incomingProperty != null) {
			addTriple(incomingSource, incomingProperty, generatedIndividual);
		}


		rels.addAll(getExistentialRelationships(generatedIndividual));
		// The sorting makes the traversal of the graph deterministic
		// Handles the different sort order of sets in Java 8
		List<OWLObjectSomeValuesFrom> sortedRels = new ArrayList<OWLObjectSomeValuesFrom>(rels);
		Collections.sort(sortedRels);
		for (OWLObjectSomeValuesFrom rel : sortedRels) {

			OWLClassExpression jType = deepen(rel.getFiller(), rel.getProperty(), c);

			boolean isExcluded = false;
			if (directExclusionSet != null) {
				if (directExclusionSet.contains(jType)) {
					isExcluded = true;
				}
			}
			if (isExcluded) {
				continue;
			}
			if (exclusionSet != null) {
				for (OWLClass testC : exclusionSet) {
					if (model.getReasoner().getSuperClasses(jType, false).getFlattened().contains(testC)) {
						isExcluded = true;
						break;
					}
				}
			}
			if (isExcluded) {
				continue;
			}
			if (inclusionSet != null) {
				isExcluded = true;
				for (OWLClass testC : inclusionSet) {
					if (model.getReasoner().getSuperClasses(jType, false).getFlattened().contains(testC)) {
						isExcluded = false;
						break;
					}


				}
			}
			if (isExcluded) {
				continue;
			}
			OWLNamedIndividual targetIndividual =
					generateNecessaryIndividualsImpl(jType, generatedIndividual, rel.getProperty());
		}
		return generatedIndividual;
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
		return generateNecessaryIndividuals(c, true, true);
	}


	/**
	 * Calls {@link #generateNecessaryIndividuals(OWLClassExpression)}, and if isCollapse is set,
	 * will call {@link #collapseIndividuals()}
	 * 
	 * @param c
	 * @param isCollapse
	 * @return individual
	 */
	public OWLNamedIndividual generateNecessaryIndividuals(OWLClassExpression c, boolean isCollapse) {
		return generateNecessaryIndividuals(c, isCollapse, true);
	}

	/**
	 * Calls {@link #generateNecessaryIndividuals(OWLClassExpression)}
	 * 
	 *  <li> if isCollapse is set, will call {@link #collapseIndividuals()}
	 *  <li> if isTransitivelyReduce is set, will call {@link #performTransitiveReduction()}
	 * @param c
	 * @param isCollapse
	 * @param isTransitivelyReduce
	 * @return individual
	 */
	public OWLNamedIndividual generateNecessaryIndividuals(OWLClassExpression c, boolean isCollapse,
			boolean isTransitivelyReduce) {
		OWLNamedIndividual ind = generateNecessaryIndividualsImpl(c, null, null);
		if (isCollapse) {
			collapseIndividuals();
		}
		if (isTransitivelyReduce) {
			performTransitiveReduction();
		}
		return ind;
	}


	/**
	 * Adds a prototypical individual to the abox.
	 * 
	 * fills in prototypeIndividualMap, unless c satisfies {@link #isNeverMerge}
	 * 
	 * @param c
	 * @return individual
	 */
	private OWLNamedIndividual generateBaseIndividual(OWLClassExpression c) {
		IRI iri;
		boolean isUniq = true;
		if (c instanceof OWLClass  && !this.isNeverMerge((OWLClass)c)) {
			iri = IRI.create(((OWLClass) c).getIRI().toString()+getContextualizingSuffix());
		}
		else {
			iri = IRI.create(model.getAboxOntology().getOntologyID().getOntologyIRI()+"-"+UUID.randomUUID());
			isUniq = false;
		}
		OWLNamedIndividual ind = model.getOWLDataFactory().getOWLNamedIndividual(iri);
		model.addAxiom(model.getOWLDataFactory().getOWLClassAssertionAxiom(c, ind));
		if (isUniq) {
			prototypeIndividualMap.put((OWLClass) c, ind);
		}

		return ind;
	}

	public void generateAllNecessaryIndividuals() {
		generateAllNecessaryIndividuals(false, false);
	}
	public void generateAllNecessaryIndividuals(boolean isCollapse, boolean isReduce) {
		OWLEntityRenamer renamer = new OWLEntityRenamer(model.getOWLOntologyManager(), 
				Collections.singleton(model.getAboxOntology()));
		List<OWLOntologyChange> chgs = new ArrayList<OWLOntologyChange>();
		for (OWLNamedIndividual ind : model.getTboxOntology().getIndividualsInSignature(true)) {
			for (OWLClassExpression cx : ind.getTypes(model.getTboxOntology())) {
				OWLNamedIndividual j = 
						generateNecessaryIndividuals(cx, isCollapse, isReduce);
				chgs.addAll(renamer.changeIRI(j.getIRI(), ind.getIRI()));
				//m.addAxiom(m.getOWLDataFactory().getOWLO, axiom)
			}
		}
		LOG.debug("Changes = "+chgs.size());
		model.getOWLOntologyManager().applyChanges(chgs);
	}

	/**
	 * attempt to deepen a class expression based on properties the individual holds.
	 * 
	 * E.g. if i=hand and j=digit, and p=has_part, then 
	 * we can reason that (digit and inverseOf(has_part) some hand) is a subclass
	 * of or equivalent to 'finger'
	 * 
	 * This may typically require hermit
	 * 
	 * @param jType
	 * @param invProperty
	 * @param incoming
	 * @return expression
	 */
	private OWLClassExpression deepen(OWLClassExpression jType,
			OWLObjectPropertyExpression invProperty, OWLClassExpression incoming) {
		OWLObjectPropertyExpression ipe = model.getOWLDataFactory().getOWLObjectInverseOf(invProperty);
		Set<OWLObjectPropertyExpression> invProps = getInverseObjectProperties(invProperty);
		if (invProps.size() > 0) {
			// it should not matter which one is selected, unless
			// ontology has equiv properties and reasoner does not handle equiv props
			// (Elk is fine - http://code.google.com/p/elk-reasoner/wiki/OwlFeatures)
			ipe = invProps.iterator().next();
		}
		OWLObjectIntersectionOf jExpr = model.getOWLDataFactory().getOWLObjectIntersectionOf(
				jType,
				model.getOWLDataFactory().getOWLObjectSomeValuesFrom(
						ipe,
						incoming)
				);
		Set<OWLClass> deeperClasses = model.getReasoner().getSuperClasses(
				jExpr,
				true).getFlattened();
		deeperClasses.addAll(model.getReasoner().getEquivalentClasses(jExpr).getEntities());
		for (OWLClass dc: deeperClasses) {
			// don't include artificial
			if (model.isQueryClass(dc))
				continue;
			//LOG.debug(fo(" Deepen_candidate="+dc);
			if (model.getReasoner().getSuperClasses(dc, false).getFlattened().contains(jType)) {
				LOG.debug("   DEEPENED_TO="+dc);
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
	 */
	public void collapseIndividuals() {
		LOG.debug("Collapsing...");

		// map between generic prototypes and specific
		// prototypes - at the end, if this 1:1,
		// the generic will be mapped into the specific.
		// e.g. limb(limb-proto) -> forelimb(limb-proto)
		Map<OWLNamedIndividual, OWLNamedIndividual> mergeMap = 
				new HashMap<OWLNamedIndividual,OWLNamedIndividual>();

		// set of prototypes that are possibly ambiguous;
		// e.g. limb -> {forelimb, hindlimb}
		Set<OWLNamedIndividual> hasMultipleCandidates = new HashSet<OWLNamedIndividual>();

		// iterate through all classes that have prototypes
		Collection<OWLNamedIndividual> individuals = this.getGeneratedIndividuals();
		for (OWLNamedIndividual targetSpecificPrototype : individuals) {
			// e.g. forelimb (target)
			//LOG.debug(" testing SPECIFIC target prototype (to be merged into): "+targetSpecificPrototype);

			// e.g. limb (candidate merge: limb -> forelimb)
			Set<OWLClass> mergeClassCandidates = 
					model.getReasoner().getTypes(targetSpecificPrototype, false).getFlattened();

			// all types for target prototype
			Set<OWLClass> targetInferredTypes = model.getReasoner().getTypes(targetSpecificPrototype, false).
					getFlattened();

			for (OWLClass sourceGenericClass : mergeClassCandidates) {

				// only classes that correspond to a prototype
				if (!prototypeIndividualMap.containsKey(sourceGenericClass))
					continue;

				OWLNamedIndividual sourceGenericPrototype =
						prototypeIndividualMap.get(sourceGenericClass);

				if (sourceGenericPrototype.equals(targetSpecificPrototype))
					continue;

				// never merge owl:Thing, etc
				if (isNeverMerge(sourceGenericClass))
					continue;

				// avoid splits: check if a merge has already been proposed for the source
				if (mergeMap.containsKey(sourceGenericPrototype)) {

					//LOG.debug("  considering pushing down: "+sourceGenericPrototype);

					// Test if generic class already slated for merging;
					// e.g. src = limb, tgt = forelimb, existing = hindlimb
					OWLNamedIndividual existingTargetPrototype =
							mergeMap.get(sourceGenericPrototype);
					//LOG.debug("    existingTgt = "+existingTargetPrototype);

					Set<OWLClass> existingInferredTypes = 
							model.getReasoner().getTypes(existingTargetPrototype, false).getFlattened();

					// overwrite existing entity if more specific.
					//  src=forelimb, existing=limb, tgt=left_forelimb
					// TODO? this: src = limb, tgt = left_forelimb, existing=forelimb
					if (targetInferredTypes.containsAll(existingInferredTypes)) {
						// overwrite with target, as target is more specific
						mergeMap.put(sourceGenericPrototype, targetSpecificPrototype);
						LOG.debug("       overwriting existing target "+existingTargetPrototype+" ==> "+targetSpecificPrototype);
					}
					else if (existingInferredTypes.containsAll(targetInferredTypes)) {
						// keep existing, as existing is more specific
						LOG.debug("       keeping existing target "+existingTargetPrototype+", which is more specific than "+targetSpecificPrototype);
					}
					else {
						// this blocks the merge of s
						Set<OWLClass> uniqueToTarget = new HashSet<OWLClass>(targetInferredTypes);
						uniqueToTarget.removeAll(existingInferredTypes);
						LOG.debug("       DUAL TARGETS "+sourceGenericPrototype+ " ==> { "+existingTargetPrototype+" ==OR== "+targetSpecificPrototype+" }");
						hasMultipleCandidates.add(sourceGenericPrototype);
					}
				}
				else {
					mergeMap.put(sourceGenericPrototype, targetSpecificPrototype);
				}
			}
		}

		// perform merge
		Set<OWLClass> staleClasses = new HashSet<OWLClass>();

		// TODO - a more elegant way to do this is to first to start without assumptions of individual
		// distinctness or identity, incrementally added sameAs axioms (based on heuristics and user params)
		// testing for inconsistency with each step.
		// as a final step all sameAs axioms could be merged using the OER
		OWLEntityRenamer renamer = new OWLEntityRenamer(model.getAboxOntology().getOWLOntologyManager(), 
				Collections.singleton(model.getAboxOntology()));
		for (OWLNamedIndividual sourceIndividual : mergeMap.keySet()) {
			OWLNamedIndividual targetIndividual = mergeMap.get(sourceIndividual);

			LOG.debug("  ?collapsing "+sourceIndividual+ " -> "+targetIndividual);
			if (hasMultipleCandidates.contains(sourceIndividual)) {
				LOG.debug("   **multi-candidate; skipping");
				continue;
			}
			LOG.debug("  ACTUALLY collapsing "+sourceIndividual+ " -> "+targetIndividual);
			for (OWLClass c: prototypeIndividualMap.keySet()) {
				if (prototypeIndividualMap.get(c).equals(sourceIndividual)) {
					staleClasses.add(c);
				}
			}
			model.applyChanges(renamer.changeIRI(sourceIndividual.getIRI(),
					targetIndividual.getIRI()));
		}

		if (this.isRemoveAmbiguousIndividuals) {
			for (OWLNamedIndividual i : hasMultipleCandidates) {
				for (OWLClass c: prototypeIndividualMap.keySet()) {
					if (prototypeIndividualMap.get(c).equals(i)) {
						staleClasses.add(c);
					}
				}				
			}
			Set<OWLAxiom> rmAxioms = new HashSet<OWLAxiom>();
			for (OWLAxiom ax : model.getAboxOntology().getAxioms()) {
				Set<OWLNamedIndividual> inds = ax.getIndividualsInSignature();
				inds.retainAll(hasMultipleCandidates);
				if (inds.size() > 0) {
					LOG.debug("REMOVING AMBIGUOUS INDIVIDUAL:"+ax);
					rmAxioms.add(ax);
				}
			}
			model.getAboxOntology().getOWLOntologyManager().removeAxioms(model.getAboxOntology(), rmAxioms);
		}

		for (OWLClass c : staleClasses)
			prototypeIndividualMap.remove(c);

	}

	/**
	 * Moves all axioms about src to tgt
	 * 
	 * @param src
	 * @param tgt
	 * @return true if success (currently always true)
	 */
	protected boolean mergeInto(OWLNamedIndividual src, OWLNamedIndividual tgt) {
		if (src.equals(tgt))
			return true;
		LOG.debug("Merging "+src+" into "+tgt);
		OWLEntityRenamer renamer = new OWLEntityRenamer(model.getAboxOntology().getOWLOntologyManager(), 
				Collections.singleton(model.getAboxOntology()));
		model.applyChanges(renamer.changeIRI(src.getIRI(),
				tgt.getIRI()));
		return true;
	}

	private boolean isNeverMerge(OWLClass c) {
		if (c.equals(model.getOWLDataFactory().getOWLNothing()))
			return true;
		// TODO - customize
		if (isStrict)
			return true;
		return false;
	}

	/**
	 * calls {@link #anonymizeIndividual(OWLNamedIndividual)}
	 * 
	 * @param c
	 */
	public void anonymizeIndividualsNotIn(OWLClass c) {
		anonymizeIndividualsNotIn(Collections.singleton(c));
	}

	/**
	 * calls {@link #anonymizeIndividual(OWLNamedIndividual)} on any individual not in one of c1, ..., cn
	 * 
	 * @param cs
	 */
	public void anonymizeIndividualsNotIn(Set<OWLClass> cs) {
		Set<OWLNamedIndividual> inds = new HashSet<OWLNamedIndividual>();
		for (OWLNamedIndividual ind : model.getAboxOntology().getIndividualsInSignature(true)) {
			Set<OWLClass> sups = model.getReasoner().getTypes(ind, false).getFlattened();
			sups.retainAll(cs);
			if (sups.size() == 0) {
				LOG.debug("SCHEDULING: "+ind);
				inds.add(ind);
			}
		}
		anonymizeIndividuals(inds);
	}


	/**
	 * calls {@link #anonymizeIndividual(OWLNamedIndividual)} on each member of the set
	 * @param inds
	 */
	public void anonymizeIndividuals(Set<OWLNamedIndividual> inds) {
		for (OWLNamedIndividual ind : inds) {
			anonymizeIndividual(ind);
		}
	}

	/**
	 * Remove all mention of ind, but retain equivalence in the ontology by replacing all OPEs that
	 * reference ind with an existential axiom.
	 * 
	 * E.g. if: p1 occurs_in c1, c1 Type C and we anonymize c1, then we add an axiom p1 Type occurs_in some C
	 * 
	 * @param ind
	 */
	private void anonymizeIndividual(OWLNamedIndividual ind) {
		Set<OWLClass> types = model.getReasoner().getTypes(ind, true).getFlattened();
		LOG.debug("ANONYMIZING:"+ind);
		// replace incoming with class expressions
		Set<OWLAxiom> refAxioms = ind.getReferencingAxioms(model.getAboxOntology(), false);
		for (OWLAxiom ax : refAxioms) {
			if (ax instanceof OWLObjectPropertyAssertionAxiom) {
				OWLObjectPropertyAssertionAxiom pa = (OWLObjectPropertyAssertionAxiom)ax;
				for (OWLClass type : types) {
					if (type.equals(model.getOWLDataFactory().getOWLNothing()))
						continue;
					OWLClassExpression classExpression =
							model.getOWLDataFactory().getOWLObjectSomeValuesFrom(pa.getProperty(), type);
					OWLClassAssertionAxiom newAx = 
							model.getOWLDataFactory().getOWLClassAssertionAxiom(classExpression, pa.getSubject());
					model.addAxiom(newAx);
				}
			}
		}
		// remove incoming
		model.getOWLOntologyManager().removeAxioms(model.getAboxOntology(), refAxioms);

		// remove outgoing
		model.getOWLOntologyManager().removeAxioms(model.getAboxOntology(), model.getAboxOntology().getAxioms(ind));

		// remove delcarations
		model.getOWLOntologyManager().removeAxiom(model.getAboxOntology(), model.getOWLDataFactory().getOWLDeclarationAxiom(ind));
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
	 * @return expression
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
		LOG.debug("i="+i);
		Set<OWLClassExpression> elements = new HashSet<OWLClassExpression>();
		model.getReasoner().flush();
		for (OWLClass typeClass : model.getReasoner().getTypes(i, true).getFlattened()) {
			LOG.debug(" t="+typeClass);
			if (model.isQueryClass(typeClass))
				continue;
			elements.add(typeClass);
		}
		// supplement this with asserted class expression types
		for (OWLClassExpression x : i.getTypes(model.getAboxOntology().getImportsClosure())) {
			if (x.isAnonymous()) {
				Set<OWLClass> xDescs = model.getReasoner().getSubClasses(x, false).getFlattened();
				xDescs.retainAll(elements);
				if (xDescs.size() == 0) {
					elements.add(x);
				}
			}
		}
		if (propertySet == null || propertySet.size() == 0) {
			propertySet = new ArrayList<OWLObjectProperty>();
			for (OWLObjectProperty p : model.getAboxOntology().getObjectPropertiesInSignature(true)) {
				propertySet.add(p);
			}
		}

		for (OWLObjectProperty p : propertySet) {
			LOG.debug(" p="+p);

			// TODO - use core method
			Set<OWLObjectPropertyExpression> invProps = getInverseObjectProperties(p);


			for (OWLOntology ont : model.getAboxOntology().getImportsClosure()) {
				Set<OWLIndividual> js = new HashSet<OWLIndividual>(i.getObjectPropertyValues(p, ont));
				// todo - make this more efficient
				if (invProps.size() > 0) {
					for (OWLObjectPropertyExpression invProp : invProps) {
						LOG.debug(" invP="+invProp);
						for (OWLIndividual j : model.getAboxOntology().getIndividualsInSignature(true)) {
							if (j.getObjectPropertyValues((OWLObjectPropertyExpression) invProp, ont).contains(i)) {
								js.add(j);
							}
						}
					}
				}
				for (OWLIndividual j : js) {
					LOG.debug("  j="+j);

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
						LOG.debug("    jce="+jce);
						elements.add(
								model.getOWLDataFactory().getOWLObjectSomeValuesFrom(p, jce)
								);
					}
					else {
						LOG.warn("I wasn't expecting anonymous individuals in the abox: "+j);
					}
				}
			}
		}
		return model.getOWLDataFactory().getOWLObjectIntersectionOf(elements);
	}

	// UTIL

	protected Map<OWLClass,OWLClassExpression> getQueryClassMap() {
		return model.getQueryClassMap(isPrecomputePropertyClassCombinations);
	}

	private Set<OWLObjectPropertyExpression> getInverseObjectProperties(
			OWLObjectPropertyExpression property) {
		Set<OWLObjectPropertyExpression> invProps = new HashSet<OWLObjectPropertyExpression>();
		for (OWLOntology ont : model.getAboxOntology().getImportsClosure()) {
			invProps.addAll(property.getInverses(ont));
		}
		return invProps;
	}
	/**
	 * Given an individual i, return a set of existential restrictions <P,C>,
	 * where i instantiates P some C.
	 * 
	 * The implementation for this method depends on the pre-materialization of a
	 * Query Ontology containing <P some C> combinations.
	 * 
	 * Redundant expressions are filtered out
	 * 
	 * @param ind
	 * @return set of existential restrictions
	 */
	protected Set<OWLObjectSomeValuesFrom> getExistentialRelationships(OWLNamedIndividual ind) {
		//LOG.debug("Querying: "+c);
		Map<OWLClass, OWLClassExpression> queryClassMap = getQueryClassMap();
		Set<OWLObjectSomeValuesFrom> results = new HashSet<OWLObjectSomeValuesFrom>();

		model.getReasoner().flush();

		// all supers (direct and indirect)
		Set<OWLClass> supers = model.getReasoner().getTypes(ind, false).getFlattened();
		LOG.debug("Supers for "+ind+" is "+supers.size());

		// we only case about expressions in the query ontology, which should have
		// all expressions required
		supers.retainAll(queryClassMap.keySet());
		LOG.debug("Supers in QMAP for "+ind+" is "+supers.size());

		// use only classes that are non-redundant (within QSet)
		Set<OWLClass> nrSet = new HashSet<OWLClass>(supers);
		for (OWLClass s : supers) {
			nrSet.removeAll(model.getReasoner().getSuperClasses(s, false).getFlattened());
		}

		// map from materialized class to original expression
		for (OWLClass s : nrSet) {
			//LOG.debug(" SUP:"+s);
			OWLClassExpression x = queryClassMap.get(s);
			if (x instanceof OWLObjectSomeValuesFrom) {
				//LOG.debug("  Result:"+x);
				results.add((OWLObjectSomeValuesFrom)x);
			}
			else {
				LOG.warn("Skipping: "+x+" (future versions may handle this)");
			}
		}
		return results;
	}

	protected Set<OWLObjectSomeValuesFrom> getExistentialRelationships(OWLClassExpression c) {
		return getExistentialRelationships(c, true);
	}
	protected Set<OWLObjectSomeValuesFrom> getExistentialRelationships(OWLClassExpression c, boolean isDirect) {
		//LOG.debug("Querying: "+c);
		Map<OWLClass, OWLClassExpression> queryClassMap = getQueryClassMap();
		Set<OWLObjectSomeValuesFrom> results = new HashSet<OWLObjectSomeValuesFrom>();

		model.getReasoner().flush();

		// all supers (direct and indirect)
		Set<OWLClass> supers = model.getReasoner().getSuperClasses(c, false).getFlattened();
		supers.addAll(model.getReasoner().getEquivalentClasses(c).getEntities());

		// we only case about expressions in the query ontology, which should have
		// all expressions required
		supers.retainAll(queryClassMap.keySet());

		// use only classes that are non-redundant (within QSet)
		Set<OWLClass> nrSet = new HashSet<OWLClass>(supers);
		if (isDirect) {
			for (OWLClass s : supers) {
				nrSet.removeAll(model.getReasoner().getSuperClasses(s, false).getFlattened());
			}
		}

		// map from materialized class to original expression
		for (OWLClass s : nrSet) {
			//LOG.debug(" SUP:"+s);
			OWLClassExpression x = queryClassMap.get(s);
			if (x instanceof OWLObjectSomeValuesFrom) {
				//LOG.debug("  Result:"+x);
				results.add((OWLObjectSomeValuesFrom)x);
			}
			else {
				LOG.warn("Skipping: "+x+" (future versions may handle this)");
			}
		}
		return results;
	}

	public Set<OWLClassExpression> getSuperClassExpressions(OWLClassExpression x, boolean isDirect) {
		Set<OWLClassExpression> results = new HashSet<OWLClassExpression>(getExistentialRelationships(x, isDirect));
		Set<OWLClass> supers = new HashSet<OWLClass>(model.getReasoner().getSuperClasses(x, false).getFlattened());
		supers.removeAll(model.getQueryClasses());
		Set<OWLClass> nrSet = new HashSet<OWLClass>(supers);
		if (isDirect) {
			for (OWLClass s : supers) {
				nrSet.removeAll(model.getReasoner().getSuperClasses(s, false).getFlattened());
			}
		}
		results.addAll(nrSet);
		return results;
	}

	// ABOX ONTOLOGY MANAGEMENT


	protected void addTriple(OWLNamedIndividual sourceIndividual,
			OWLObjectPropertyExpression property,
			OWLNamedIndividual targetIndividual) {
		Set<OWLObjectPropertyAssertionAxiom> axioms = 
				getFactAxioms(sourceIndividual, property, targetIndividual, isAssertInverses());
		model.addAxioms(axioms);
	}

	private Set<OWLObjectPropertyAssertionAxiom> getFactAxioms(OWLNamedIndividual sourceIndividual,
			OWLObjectPropertyExpression property,
			OWLNamedIndividual targetIndividual,
			boolean isIncludeInverses) {
		Set<OWLObjectPropertyAssertionAxiom> axioms = 
				new HashSet<OWLObjectPropertyAssertionAxiom>();
		axioms.add(model.getOWLDataFactory().getOWLObjectPropertyAssertionAxiom(property, 
				sourceIndividual, targetIndividual));
		if (isIncludeInverses) {
			for (OWLObjectPropertyExpression ip : this.getInverseObjectProperties(property)) {
				//LOG.debug("Including inverse: "+ip+" "+targetIndividual+" "+sourceIndividual);
				axioms.add(model.getOWLDataFactory().getOWLObjectPropertyAssertionAxiom(ip, 
						targetIndividual,
						sourceIndividual 
						));

			}
		}
		return axioms;
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
		SyntacticLocalityModuleExtractor sme = new SyntacticLocalityModuleExtractor(model.getOWLOntologyManager(),
				model.getAboxOntology(),
				ModuleType.BOT);

		Set<OWLEntity> objs = new HashSet<OWLEntity>();
		objs.addAll( model.getAboxOntology().getObjectPropertiesInSignature() );
		objs.addAll( model.getAboxOntology().getClassesInSignature() );


		Set<OWLAxiom> modAxioms = sme.extract(objs);
		for (OWLImportsDeclaration oid : model.getAboxOntology().getImportsDeclarations()) {
			model.getOWLOntologyManager().applyChange(new RemoveImport(model.getAboxOntology(), oid));
		}
		model.getOWLOntologyManager().addAxioms(model.getAboxOntology(), modAxioms);
	}

	/**
	 * Motivation:
	 * 
	 * After generation, we may be left with axioms:
	 * <pre>
	 * OPE(P X Y)
	 * OPE(P' Y X)
	 * InverseProperties(P P')
	 * </pre>
	 * 
	 * This is useful for reasoning with inverse-unaware reasoners,
	 * but is otherwise redundant and can clutter displays.
	 * 
	 * An alternate scenario is where we have a mixture of P and P'
	 * non-redundant assertions, and wish to choose one direction
	 * as the standard.
	 * 
	 * Calling normalizeDirections(P) will remove OPE(P' Y X) in the above
	 * if OPE(P X Y) is not present, this will be added.
	 * 
	 * @param p
	 */
	public void normalizeDirections(OWLObjectPropertyExpression p) {
		LOG.debug("Normalizing: "+p);
		Set<OWLObjectPropertyExpression> invProps =
				p.getInverses(model.getAboxOntology().getImportsClosure());
		LOG.debug("Inverse props: "+invProps);
		Set<OWLAxiom> rmAxioms = new HashSet<OWLAxiom>();
		Set<OWLAxiom> newAxioms = new HashSet<OWLAxiom>();
		for (OWLObjectPropertyAssertionAxiom opa : 
			model.getAboxOntology().getAxioms(AxiomType.OBJECT_PROPERTY_ASSERTION)) {
			if (invProps.contains(opa.getProperty())) {
				LOG.debug("  FLIPPING:"+opa);
				rmAxioms.add(opa);
				newAxioms.add(model.getOWLDataFactory().getOWLObjectPropertyAssertionAxiom(p, 
						opa.getObject(), opa.getSubject()));

			}
		}
		model.getAboxOntology().getOWLOntologyManager().addAxioms(model.getAboxOntology(), newAxioms);
		model.getAboxOntology().getOWLOntologyManager().removeAxioms(model.getAboxOntology(), rmAxioms);
	}

	public void performTransitiveReduction() {
		for (OWLObjectProperty p : model.getAboxOntology().getObjectPropertiesInSignature(true)) {
			if (p.isTransitive(model.getAboxOntology().getImportsClosure())) {
				performTransitiveReduction(p);
			}
		}
	}

	/**
	 * TODO - use reasoner if DL reasoner available
	 * (currently Elk 0.4.x will not perform OPE inferences)
	 * 
	 * Limitations:
	 *  does not use
	 *   - subPropertyOf
	 *   - equivalentProperties
	 *   - subPropertyChain
	 * @param p
	 */
	public void performTransitiveReduction(OWLObjectPropertyExpression p) {
		Set<OWLAxiom> rmAxioms = new HashSet<OWLAxiom>();
		for (OWLNamedIndividual i : model.getAboxOntology().getIndividualsInSignature()) {
			if (hasCycle(i, p)) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("Property "+p+" has cyles from "+i+" -- no transitive reductions");
				}
				continue;
			}

			Set<OWLIndividual> redundant = new HashSet<OWLIndividual>();
			Set<OWLIndividual> js = getIndividualsInProperPath(i, p);
			//LOG.debug("PATH("+i+") ==> "+js);
			for (OWLIndividual j : js) {
				redundant.addAll(getIndividualsInProperPath(j, p));
			}
			for (OWLIndividual j : redundant) {
				// TODO - check for cycles (can happen in pathological scenarios)
				if (j instanceof OWLNamedIndividual) {
					//LOG.debug("redundant: OPE("+p+" "+i+" "+j+"); PATH(i) = "+js);
					rmAxioms.addAll(this.getFactAxioms(i, p, (OWLNamedIndividual) j, true));
				}
				else {
					LOG.warn("Ignoring redundant link to anon individual: "+i+" -> "+j+" over "+p);
				}
			}
		}
		if (LOG.isDebugEnabled()) {
			LOG.debug("removing redundant axioms, #="+rmAxioms.size());
		}
		model.getAboxOntology().getOWLOntologyManager().removeAxioms(model.getAboxOntology(), rmAxioms);
	}

	public boolean hasCycle(OWLIndividual i, OWLObjectPropertyExpression p) {
		return hasCycle(i, p, new HashSet<OWLIndividual>());
	}
	public boolean hasCycle(OWLIndividual i, OWLObjectPropertyExpression p,
			Set<OWLIndividual> visited) {
		if (visited.contains(i)) {
			return true;
		}
		// make a copy
		Set<OWLIndividual> v2 = new HashSet<OWLIndividual>(visited);
		v2.add(i);
		Set<OWLIndividual> nextSet = 
				getDirectOutgoingIndividuals(i, Collections.singleton(p));

		for (OWLIndividual j : nextSet) {
			if (hasCycle(j, p, v2)) {
				return true;
			}
		}
		return false;
	}

	public Set<OWLIndividual> getIndividualsInProperPath(OWLIndividual i, OWLObjectPropertyExpression p) {
		return getIndividualsInProperPath(i, Collections.singleton(p));
	}

	public Set<OWLIndividual> getIndividualsInProperPath(OWLIndividual i, Set<OWLObjectPropertyExpression> ps) {
		// TODO - use reasoner if not hermit
		Set<OWLIndividual> results = new HashSet<OWLIndividual>();
		Set<OWLIndividual> visited = new HashSet<OWLIndividual>();
		Stack<OWLIndividual> stack = new Stack<OWLIndividual>();
		stack.add(i);
		while (!stack.isEmpty()) {
			OWLIndividual x = stack.pop();
			Set<OWLIndividual> nextSet = getDirectOutgoingIndividuals(x, ps);
			nextSet.removeAll(visited);
			stack.addAll(nextSet);
			results.addAll(nextSet);
			visited.addAll(nextSet);
		}
		return results;
	}

	private Set<OWLIndividual> getDirectOutgoingIndividuals(OWLIndividual i,
			Set<OWLObjectPropertyExpression> ps) {
		// TODO - subproperties
		Set<OWLIndividual> results = new HashSet<OWLIndividual>();

		for (OWLObjectPropertyExpression p : ps) {
			results.addAll(i.getObjectPropertyValues(p, model.getAboxOntology()));
			for (OWLObjectPropertyExpression invProp : getInverseObjectProperties(p)) {
				// todo - make this more efficient
				//LOG.debug(" invP="+invProp);
				for (OWLIndividual j : model.getAboxOntology().getIndividualsInSignature(true)) {
					if (j.getObjectPropertyValues((OWLObjectPropertyExpression) invProp, model.getAboxOntology()).contains(i)) {
						results.add(j);
					}
				}
			}
		}
		return results;
	}

}