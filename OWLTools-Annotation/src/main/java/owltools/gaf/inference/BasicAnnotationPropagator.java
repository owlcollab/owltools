package owltools.gaf.inference;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.gaf.Bioentity;
import owltools.gaf.GafDocument;
import owltools.gaf.GeneAnnotation;
import owltools.graph.OWLGraphWrapper;

/**
 * This performs basic annotation inferences involving propagation
 * between the 3 sub-ontologies in GO
 * 
 * <ul>
 *  <li> MF -> BP over part_of
 *  <li> BP -> CC over occurs_in
 * </ul>
 * 
 */
public class BasicAnnotationPropagator extends AbstractAnnotationPredictor implements AnnotationPredictor {

	protected static Logger LOG = Logger.getLogger(BasicAnnotationPropagator.class);
	
	protected static boolean SKIP_IEA = false; 
	
	private static final String ASSIGNED_BY_CONSTANT = "GOC";
	private static final String gocheck_do_not_annotate = "gocheck_do_not_annotate";
	
	private OWLReasoner reasoner = null;
	private Map<String, Set<OWLClass>> propagationRules = null;
	private Map<String, String> aspectMap = null;

	/**
	 * Create instance.
	 * 
	 * @param gafDocument
	 * @param graph
	 */
	public BasicAnnotationPropagator(GafDocument gafDocument, OWLGraphWrapper graph) {
		super(gafDocument, graph);
		init();
	}
	
	private void init() {
		LOG.info("Start preparing propagation rules");
		OWLGraphWrapper graph = getGraph();
		ElkReasonerFactory factory = new ElkReasonerFactory();
		// assumes that all support ontologies have either been merged into or added as import
		reasoner = factory.createReasoner(graph.getSourceOntology());
		propagationRules = createPropagationRules(graph, reasoner);
		aspectMap = createDefaultAspectMap(graph);
		LOG.info("Finished preparing propagation rules");
	}
	
	/**
	 * Create the default propagation rule set tailored for the GeneOntology.
	 * 
	 * @param graph
	 * @param reasoner
	 * @return set of valid propagation rules
	 */
	protected Map<String, Set<OWLClass>> createPropagationRules(OWLGraphWrapper graph, OWLReasoner reasoner) {
		Map<String, Set<OWLClass>> map = new HashMap<String, Set<OWLClass>>();
		
		OWLClass mf = graph.getOWLClassByIdentifier("GO:0003674"); // molecular_function
		OWLClass bp = graph.getOWLClassByIdentifier("GO:0008150"); // biological_process
		OWLClass cc = graph.getOWLClassByIdentifier("GO:0005575"); // cellular_component
		
		OWLObjectProperty part_of = graph.getOWLObjectPropertyByIdentifier("part_of");
		OWLObjectProperty occurs_in = graph.getOWLObjectPropertyByIdentifier("occurs_in");
		if (occurs_in == null) {
			LOG.warn("Could not find relation 'occurs_in'.");
		}
		
		
		// MF -> BP over part_of
		if (part_of != null && mf != null && bp != null) {
			// get all classes in the mf and bp branch
			Set<OWLClass> mfClasses = reasoner.getSubClasses(mf, false).getFlattened();
			Set<OWLClass> bpClasses = reasoner.getSubClasses(bp, false).getFlattened();
			Map<Set<OWLClass>, Set<OWLClass>> cache = new HashMap<Set<OWLClass>, Set<OWLClass>>();
			
			OWLClass metabolicProcess = graph.getOWLClassByIdentifier("GO:0008152"); //  metabolic process
			for (OWLClass mfClass : mfClasses) {
				List<String> mfClassSubsets = graph.getSubsets(mfClass);
				if (mfClassSubsets.contains(gocheck_do_not_annotate)) {
					// do not propagate from do not annotate terms
					continue;
				}
				// exclude metabolic process ! GO:0008152
				if (mfClass.equals(metabolicProcess)) {
					continue;
				}
				Set<OWLObjectProperty> relations = Collections.singleton(part_of);
				Set<OWLClass> nonRedundantLinks = getNonRedundantLinkedClasses(mfClass, relations, graph, reasoner, bpClasses, cache);
				
				if (!nonRedundantLinks.isEmpty()) {
					// remove too high level targets and metabolic process
					if (metabolicProcess != null) {
						nonRedundantLinks.remove(metabolicProcess);
					}
					
					// iterate and delete unwanted
					removeUninformative(graph, nonRedundantLinks);
					
					// add to map
					if (!nonRedundantLinks.isEmpty()) {
						map.put(graph.getIdentifier(mfClass), nonRedundantLinks);
					}
				}
			}
		}
		else {
			LOG.warn("Skipping MF -> BP over 'part_of'.");
		}
		
		
		// BP -> CC over occurs_in
		if (occurs_in != null && bp != null && cc != null) {
			// get all classes in the bp and cc branch
			Set<OWLClass> bpClasses = reasoner.getSubClasses(bp, false).getFlattened();
			Set<OWLClass> ccClasses = reasoner.getSubClasses(cc, false).getFlattened();
			Map<Set<OWLClass>, Set<OWLClass>> cache = new HashMap<Set<OWLClass>, Set<OWLClass>>();
			
			for(OWLClass bpClass : bpClasses) {
				List<String> bpClassSubsets = graph.getSubsets(bpClass);
				if (bpClassSubsets.contains(gocheck_do_not_annotate)) {
					// do not propagate from do not annotate terms
					continue;
				}
				Set<OWLObjectProperty> relations = Collections.singleton(occurs_in);
				Set<OWLClass> nonRedundantLinks = getNonRedundantLinkedClasses(bpClass, relations, graph, reasoner, ccClasses, cache);
				
				if (!nonRedundantLinks.isEmpty()) {
					removeUninformative(graph, nonRedundantLinks);
					
					// add to map
					if (!nonRedundantLinks.isEmpty()) {
						map.put(graph.getIdentifier(bpClass), nonRedundantLinks);
					}
				}
			}
		}
		else {
			LOG.warn("Skipping BP -> CC over 'occurs_in'.");
		}
		
		if (map.isEmpty()) {
			// only fail if there are no propagation rules
			// the test case uses a custom ontology, which has no cc or 'occurs_in' relation
			throw new RuntimeException("Could not create any valid propgation rules. Is the correct ontology (GO) loaded?");
		}
		return Collections.unmodifiableMap(map);
	}

	/**
	 * Modify the given set and remove uninformative classes.
	 * 
	 * @param graph
	 * @param nonRedundantLinks
	 */
	private void removeUninformative(OWLGraphWrapper graph, Set<OWLClass> nonRedundantLinks) {
		Iterator<OWLClass> linkIt = nonRedundantLinks.iterator();
		while (linkIt.hasNext()) {
			OWLClass link = linkIt.next();
			List<String> subsets = graph.getSubsets(link);
			if (subsets != null) {
				if (subsets.contains(gocheck_do_not_annotate)) {
					linkIt.remove();
				}
			}
		}
	}
	
	/**
	 * Retrieve the non redundant set of linked classes using the given
	 * relation. The reasoner is used to infer the super and subsets for the
	 * subClass hierarchy. Only return classes, which are in the given super set
	 * (a.k.a. ontology branch).<br>
	 * <br>
	 * This can be an expensive operation, especially the
	 * {@link #reduceToNonRedundant(Set, OWLReasoner)}. Therefore the results of
	 * that method are cached in a map.<br>
	 * For example, using the cache for GO, there was a reduction of the startup
	 * time from 95 seconds to 10 seconds.
	 * 
	 * @param c
	 * @param properties
	 * @param g
	 * @param reasoner
	 * @param superSet
	 * @param cache
	 * @return set of linked classes, never null
	 */
	protected static Set<OWLClass> getNonRedundantLinkedClasses(OWLClass c, Set<OWLObjectProperty> properties, OWLGraphWrapper g, OWLReasoner reasoner, Set<OWLClass> superSet, Map<Set<OWLClass>, Set<OWLClass>> cache) {
		// get all superClasses for the current class
		Set<OWLClass> currentSuperClasses = reasoner.getSuperClasses(c, false).getFlattened();
		currentSuperClasses.add(c);
		Set<OWLClass> linkedClasses = new HashSet<OWLClass>();
		for (OWLClass currentSuperClass : currentSuperClasses) {
			if (currentSuperClass.isBuiltIn()) {
				continue;
			}
			// find all direct links via the property to the selected super set
			linkedClasses.addAll(getDirectLinkedClasses(currentSuperClass, properties, g, superSet));
		}
		// create remove redundant super classes from link set
		Set<OWLClass> nonRedundantLinks = cache.get(linkedClasses);
		if (nonRedundantLinks == null) {
			nonRedundantLinks = reduceToNonRedundant(linkedClasses, reasoner);
			cache.put(linkedClasses, nonRedundantLinks);
		}
		
		return nonRedundantLinks;
	}
	
	/**
	 * Lookup relation super classes in graph g for a given sub class c and property p.
	 * 
	 * @param c
	 * @param property
	 * @param g
	 * @return set of super classes or null
	 */
	protected static Set<OWLClass> getDirectLinkedClasses(OWLClass c, String property, OWLGraphWrapper g) {
		Set<OWLObjectProperty> properties = Collections.singleton(g.getOWLObjectPropertyByIdentifier(property));
		return getDirectLinkedClasses(c, properties, g, null);
	}
	
	/**
	 * Lookup relation super classes in graph g for a given sub class c and property p.
	 * If superSet not null, only retain super classes which are in the given super set.
	 * 
	 * @param c
	 * @param properties
	 * @param g
	 * @param superSet
	 * @return set of super classes or null
	 */
	protected static Set<OWLClass> getDirectLinkedClasses(OWLClass c, Set<OWLObjectProperty> properties, OWLGraphWrapper g, Set<OWLClass> superSet) {
		Set<OWLClass> links = new HashSet<OWLClass>();
		
		for(OWLOntology o : g.getAllOntologies()) {
			// check subClass axioms
			for (OWLSubClassOfAxiom sca : o.getSubClassAxiomsForSubClass(c)) {
				OWLClassExpression ce = sca.getSuperClass();
				if (ce instanceof OWLObjectSomeValuesFrom) {
					OWLObjectSomeValuesFrom someValuesFrom = (OWLObjectSomeValuesFrom) ce;
					OWLObjectPropertyExpression currentProperty = someValuesFrom.getProperty();
					if (properties.contains(currentProperty)) {
						OWLClassExpression filler = someValuesFrom.getFiller();
						if (filler.isAnonymous() == false) {
							OWLClass fillerCls = filler.asOWLClass();
							if (superSet == null || superSet.contains(fillerCls)) {
								links.add(fillerCls);
							}
						}
					}
				}
			}
			// check equivalent classes axioms
			for (OWLEquivalentClassesAxiom eqa : o.getEquivalentClassesAxioms(c)) {
				for (OWLClassExpression ce : eqa.getClassExpressions()) {
					if (ce instanceof OWLObjectIntersectionOf) {
						OWLObjectIntersectionOf intersectionOf = (OWLObjectIntersectionOf) ce;
						for(OWLClassExpression operand : intersectionOf.getOperands()) {
							if (operand instanceof OWLObjectSomeValuesFrom) {
								OWLObjectSomeValuesFrom someValuesFrom = (OWLObjectSomeValuesFrom) operand;
								OWLObjectPropertyExpression currentProperty = someValuesFrom.getProperty();
								if (properties.contains(currentProperty)) {
									OWLClassExpression filler = someValuesFrom.getFiller();
									if (filler.isAnonymous() == false) {
										OWLClass fillerCls = filler.asOWLClass();
										if (superSet == null || superSet.contains(fillerCls)) {
											links.add(fillerCls);
										}
									}
								}
							}
						}
					}
				}
			}
		}
		return links;
	}
	
	/**
	 * Given a set of classes, create a new non-redundant set with respect to
	 * the inferred super class hierarchy. Remove all classes which are
	 * (inferred) super classes of another class in the given set.<br>
	 * <br>
	 * WARNING: This can be an expensive operation.
	 * 
	 * @param classes
	 * @param reasoner
	 * @return non redundant set, never null
	 */
	protected static Set<OWLClass> reduceToNonRedundant(Set<OWLClass> classes, OWLReasoner reasoner) {
		long start = System.currentTimeMillis();
		Set<OWLClass> nonRedundant = new HashSet<OWLClass>();
		for (OWLClass currentCls : classes) {
			Set<OWLClass> subClasses = reasoner.getSubClasses(currentCls, false).getFlattened();
			boolean noChildrenInLinks = true;
			for (OWLClass subClass : subClasses) {
				if (classes.contains(subClass)) {
					noChildrenInLinks = false;
					break;
				}
			}
			if (noChildrenInLinks) {
				nonRedundant.add(currentCls);
			}
		}
		return nonRedundant;
	}

	/**
	 * Create the mapping from the sub ontology to the aspect in the GAF.
	 * 
	 * @param graph
	 * @return aspect mapping
	 */
	protected Map<String, String> createDefaultAspectMap(OWLGraphWrapper graph) {
		Map<String, String> map = new HashMap<String, String>();
		
		OWLClass mf = graph.getOWLClassByIdentifier("GO:0003674"); // molecular_function
		if (mf != null) {
			map.put(getGoSubOntology(mf, graph), "F");
		}
		
		OWLClass bp = graph.getOWLClassByIdentifier("GO:0008150"); // biological_process
		if (bp != null) {
			map.put(getGoSubOntology(bp, graph), "P");
		}
		
		OWLClass cc = graph.getOWLClassByIdentifier("GO:0005575"); // cellular_component
		if (cc != null) {
			map.put(getGoSubOntology(cc, graph), "C");
		}
		
		if (map.isEmpty()) {
			// only fail if there are mappings
			// the test case uses a custom ontology, which has no cc branch
			throw new RuntimeException("Could not create any valid aspect mappings. Is the correct ontology (GO) loaded?");
		}
		
		return Collections.unmodifiableMap(map);
	}
	
	/**
	 * Get the specific sub ontology.
	 * 
	 * @param c
	 * @param g
	 * @return sub ontology or null
	 */
	protected String getGoSubOntology(OWLClass c, OWLGraphWrapper g) {
		return g.getNamespace(c);
	}
	
	/**
	 * Handle the predictions. Keeps track of predictions by evidence type and
	 * predicted class.<br>
	 * It also keeps track of redundant predictions. Predictions are considered
	 * redundant but not the same, if they predict the same class and evidence
	 * code, but with different ref or other constraints.<br>
	 * Assumes that all prediction are made for the same bioentity.
	 */
	private static class AllPreditions {
		private final Map<String, Map<OWLClass, List<Prediction>>> allPredictions = new HashMap<String, Map<OWLClass, List<Prediction>>>();
		
		/**
		 * Add a prediction.
		 * 
		 * @param linked
		 * @param prediction
		 */
		void add(OWLClass linked, Prediction prediction) {
			
			GeneAnnotation annotation = prediction.getGeneAnnotation();
			String evidenceCls = annotation.getEvidenceCls();
			Map<OWLClass, List<Prediction>> evidenceGroup = allPredictions.get(evidenceCls);
			if (evidenceGroup == null) {
				evidenceGroup = new HashMap<OWLClass, List<Prediction>>();
				allPredictions.put(evidenceCls, evidenceGroup);
			}
			List<Prediction> predictions = evidenceGroup.get(linked);
			if (predictions == null) {
				predictions = new ArrayList<Prediction>();
				evidenceGroup.put(linked, predictions);
			}
			if (predictions.isEmpty() == false) {
				// check that it is not fully redundant with the existing annotations
				boolean add = false;
				for(Prediction existing : predictions) {
					// assumptions: bioentity, cls, evidence code, assigned-by and aspect are the same
					GeneAnnotation a1 = existing.getGeneAnnotation();
					GeneAnnotation a2 = prediction.getGeneAnnotation();
					if (!StringUtils.equals(a1.getReferenceId(), a2.getReferenceId())) {
						add = true;
						break;
					}
					if (!StringUtils.equals(a1.getRelation(), a2.getRelation())) {
						add = true;
						break;
					}
					if (!StringUtils.equals(a1.getActsOnTaxonId(), a2.getActsOnTaxonId())) {
						add = true;
						break;
					}
					if (!StringUtils.equals(a1.getLastUpdateDate(), a2.getLastUpdateDate())) {
						add = true;
						break;
					}
					if (!StringUtils.equals(a1.getExtensionExpression(), a2.getExtensionExpression())) {
						add = true;
						break;
					}
					if (!StringUtils.equals(a1.getGeneProductForm(), a2.getGeneProductForm())) {
						add = true;
						break;
					}
				}
				if (add) {
					predictions.add(prediction);
				}
				
			}
			else {
				// do not run any checks for the first prediction
				predictions.add(prediction);
			}
			
		}
		
		/**
		 * Retrieve all evidence codes, which have predictions.
		 * 
		 * @return collection, never null
		 */
		Collection<String> getEvidences() {
			return Collections.unmodifiableCollection(allPredictions.keySet());
		}
		
		/**
		 * Retrieve all classes, for which there are predictions, given an evidence code.
		 * 
		 * @param evidence
		 * @return classes, never null
		 */
		Set<OWLClass> getClasses(String evidence) {
			Map<OWLClass, List<Prediction>> classes = allPredictions.get(evidence);
			if (classes == null) {
				return Collections.emptySet();
			}
			return Collections.unmodifiableSet(classes.keySet());
		}
		
		/**
		 * Retrieve predictions for a given evidence code and prediction class.
		 * 
		 * @param evidence
		 * @param cls
		 * @return predictions, never null
		 */
		List<Prediction> getPredictions(String evidence, OWLClass cls) {
			Map<OWLClass, List<Prediction>> classes = allPredictions.get(evidence);
			if (classes == null) {
				return Collections.emptyList();
			}
			List<Prediction> predictions = classes.get(cls);
			if (predictions == null) {
				predictions = Collections.emptyList();
			}
			return predictions;
		}
	}
	

	public List<Prediction> predictForBioEntity(Bioentity entity, Collection<GeneAnnotation> annotations) {
		AllPreditions allPredictions = new AllPreditions();
		Map<String, List<GeneAnnotation>> annotationsByEvidence = new HashMap<String, List<GeneAnnotation>>();
		for (GeneAnnotation ann : annotations) {
			
			// TODO move the exclusion list to it's own function for better customization
			final String evidenceCls = ann.getEvidenceCls();
			if (evidenceCls.equals("ND")) {
				// ignore top level annotations
				// Do *not* propagate
				continue;
			}
			if (SKIP_IEA && "IEA".equals(evidenceCls)) {
				// Do *not* propagate from IEA
				continue;
			}
			
			// add to group
			List<GeneAnnotation> group = annotationsByEvidence.get(evidenceCls);
			if (group == null) {
				group = new ArrayList<GeneAnnotation>();
				annotationsByEvidence.put(evidenceCls, group);
			}
			group.add(ann);
			
			String compositeQualifier = StringUtils.trimToEmpty(ann.getCompositeQualifier());
			if (compositeQualifier.isEmpty() == false) {
				// ignore annotations with a qualifier.
				// Do *not* propagate
				continue;
			}

			String cid = ann.getCls();
			Set<OWLClass> linkedClasses = propagationRules.get(cid);
			if (linkedClasses == null || linkedClasses.isEmpty()) {
				// no nodes to propagate to
				continue;
			}
			
			for (OWLClass linkedClass : linkedClasses) {
				String aspect = aspectMap.get(getSubOntology(linkedClass));
				Prediction p = createPrediction(linkedClass, aspect, cid, ann);
				allPredictions.add(linkedClass, p);
			}
		}
		
		List<Prediction> predictions = new ArrayList<Prediction>();
		
		for(String evidence : allPredictions.getEvidences()) {
			Set<OWLClass> nonRedundantClasses = reduceToNonRedundant(allPredictions.getClasses(evidence), reasoner);
		
			// only add the predictions, if they are more specialized
			if (!nonRedundantClasses.isEmpty()) {
				List<GeneAnnotation> annotationGroup = annotationsByEvidence.get(evidence);
				if (annotationGroup != null && !annotationGroup.isEmpty()) {
					for(OWLClass cls : nonRedundantClasses) {
						List<Prediction> currentPredictions = allPredictions.getPredictions(evidence, cls);
						if (currentPredictions.isEmpty() == false) {
							String aspect = currentPredictions.get(0).getGeneAnnotation().getAspect();
							Set<OWLClass> existing = getIsaPartofSuperClassClosureAndAspect(annotationGroup, aspect);
							
							if (existing.contains(cls) == false) {
								// the cls is more specific than any existing annotation
								// add to the predictions
								predictions.addAll(currentPredictions);
							}
						}
					}
				}
				else {
					for(OWLClass cls : nonRedundantClasses) {
						List<Prediction> currentPredictions = allPredictions.getPredictions(evidence, cls);
						if (predictions.isEmpty() == false) {
							predictions.addAll(currentPredictions);
						}
					}
				}
			}
		}
		return predictions;
	}
	
	private Set<OWLClass> getIsaPartofSuperClassClosureAndAspect(Collection<GeneAnnotation> annotations, String aspect) {
		OWLGraphWrapper g = getGraph();
		Set<OWLClass> classes = new HashSet<OWLClass>();
		for (GeneAnnotation ann : annotations) {
			if(aspect.equals(ann.getAspect())) {
				OWLClass cls = g.getOWLClassByIdentifier(ann.getCls());
				classes.add(cls);
			}
		}
		if (classes.isEmpty()) {
			return Collections.emptySet();
		}
		return getIsaPartofSuperClassClosure(classes, g, reasoner);
	}
	
	protected static Set<OWLClass> getIsaPartofSuperClassClosure(Collection<OWLClass> annotations, OWLGraphWrapper graph, OWLReasoner r) {
		Set<OWLClass> classes = new HashSet<OWLClass>();
		for (OWLClass owlClass : annotations) {
			classes.add(owlClass);
			classes.addAll(r.getSuperClasses(owlClass, false).getFlattened());
		}
		
		LinkedList<OWLClass> queue = new LinkedList<OWLClass>(classes);
		while (queue.isEmpty() == false) {
			OWLClass cls = queue.removeFirst();
			Set<OWLClass> partOfLinks = getDirectLinkedClasses(cls, "part_of", graph);
			if (!partOfLinks.isEmpty()) {
				for (OWLClass partOfLink : partOfLinks) {
					boolean added = classes.add(partOfLink);
					if(added) {
						// only look for more superClasses
						Set<OWLClass> flattened = r.getSuperClasses(partOfLink, false).getFlattened();
						for (OWLClass newClass : flattened) {
							if (classes.add(newClass)) {
								queue.add(newClass);
							}
						}
					}
				}
			}
		}
		return classes;
	}
	
	
	protected String getSubOntology(OWLClass c) {
		return getGoSubOntology(c, getGraph());
	}

	protected Prediction createPrediction(OWLClass c, String aspect, String with, GeneAnnotation source) {
		
		GeneAnnotation annP = new GeneAnnotation();
		// c1-c3
		annP.setBioentity(source.getBioentity());
		annP.setBioentityObject(source.getBioentityObject());
		
		// c4 composite qualifier
		// do *not* copy, in-fact do not propagate an annotation, which has qualifiers.
		
		// c5 cls
		annP.setCls(getGraph().getIdentifier(c));
		
		// c6 referenceId
		annP.setReferenceId(source.getReferenceId());
		
		// c7 evidence
		annP.setEvidenceCls(source.getEvidenceCls());
		
		// c8 with expression
		// because we propagate the evidence code, we also have to propagate the with column
		annP.setWithExpression(source.getWithExpression());
		
		// c9 aspect
		annP.setAspect(aspect);
		
		// c10-c12
		// bio entity
		
		// c13 taxon
		annP.setActsOnTaxonId(source.getActsOnTaxonId());
		
		// c14 last update
		// because we propagate the evidence code, we also have to propagate the date
		annP.setLastUpdateDate(source.getLastUpdateDate());
		
		// c15 assigned by GOC
		annP.setAssignedBy(ASSIGNED_BY_CONSTANT);
		
		// c16 extension - drop
		// do *not* copy
		
		// c17 ISO form
		annP.setGeneProductForm(source.getGeneProductForm());
		
		Prediction prediction = new Prediction(annP);
		return prediction;
	}

	@Override
	public void dispose() {
		if (reasoner != null) {
			reasoner.dispose();
		}
	}

}
