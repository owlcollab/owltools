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
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
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
import owltools.gaf.ExtensionExpression;
import owltools.gaf.GafDocument;
import owltools.gaf.GeneAnnotation;
import owltools.graph.OWLGraphWrapper;

/**
 * This performs basic annotation inferences involving propagation between the 3
 * sub-ontologies in GO
 * 
 * <ul>
 * <li>MF -> BP over part_of
 * <li>BP -> CC over occurs_in
 * </ul>
 * 
 * This approach pre-computes the possible non-redundant propagations classes
 * for each class during the setup. The actual prediction step looks at the
 * annotations for each bioentity and evidence code as a separate unit.
 * <br>
 * All annotations, which were assigned by the 'GOC', are considered to be
 * predictions and are ignored for the new predictions. This has the effect that
 * even if there are 'old' predictions in the GAF, they still we be re-inferred
 * for the report.
 */
public class BasicAnnotationPropagator extends AbstractAnnotationPredictor implements AnnotationPredictor {

	protected static Logger LOG = Logger.getLogger(BasicAnnotationPropagator.class);
	
	protected static boolean SKIP_IEA = true; 
	
	private static final String ASSIGNED_BY_CONSTANT = "GOC";
	private static final String gocheck_do_not_annotate = "gocheck_do_not_annotate";
	
	private OWLReasoner reasoner = null;
	private Map<String, Set<OWLClass>> propagationRules = null;
	private Map<String, String> aspectMap = null;

	private boolean isInitialized = false;
	private final boolean throwExceptions;

	/**
	 * Create instance.
	 * 
	 * @param gafDocument
	 * @param graph
	 * @param throwExceptions
	 */
	public BasicAnnotationPropagator(GafDocument gafDocument, OWLGraphWrapper graph, boolean throwExceptions) {
		super(gafDocument, graph);
		this.throwExceptions = throwExceptions;
		isInitialized = init();
	}
	
	private boolean init() {
		LOG.info("Start preparing propagation rules");
		OWLGraphWrapper graph = getGraph();
		ElkReasonerFactory factory = new ElkReasonerFactory();
		// assumes that all support ontologies have either been merged into or added as import
		reasoner = factory.createReasoner(graph.getSourceOntology());
		if (reasoner.isConsistent() == false) {
			LOG.error("The converted annotations and ontology have produced an inconsistent model.");
			if (throwExceptions) {
				throw new RuntimeException("The converted annotations and ontology have produced an inconsistent model.");
			}
			return false;
		}
		propagationRules = createPropagationRules(graph, reasoner);
		aspectMap = createDefaultAspectMap(graph);
		LOG.info("Finished preparing propagation rules");
		return true;
	}
	
	@Override
	public boolean isInitialized() {
		return isInitialized;
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
		if (part_of == null)
			LOG.warn("Could not find relation by id 'part_of'.");
		if (part_of.toString().contains("BFO") != true)
			throw new RuntimeException("The property mapped to 'part_of' does not come from BFO. Is the correct ontology (GO) loaded?");

		OWLObjectProperty occurs_in = graph.getOWLObjectPropertyByIdentifier("occurs_in");
		if (occurs_in == null)
			LOG.warn("Could not find relation by id 'occurs_in'.");
		if (occurs_in.toString().contains("BFO") != true)
			throw new RuntimeException("The property mapped to 'occurs_in' does not come from BFO. Is the correct ontology (GO) loaded?");
		
		
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
			String mfKey = getGoSubOntology(mf, graph);
			if (mfKey == null) 
				throw new RuntimeException("Could not retrieve sub-ontology for GO:0003674 (molecular_function). The value of the OBO-namespace tag does not exist.");
			
			map.put(mfKey, "F");
		}
		
		OWLClass bp = graph.getOWLClassByIdentifier("GO:0008150"); // biological_process
		if (bp != null) {
			String bpKey = getGoSubOntology(bp, graph);
			if (bpKey == null) 
				throw new RuntimeException("Could not retrieve sub-ontology for GO:0008150 (biological_process). The value of the OBO-namespace tag does not exist.");
			
			map.put(bpKey, "P");
		}
		
		OWLClass cc = graph.getOWLClassByIdentifier("GO:0005575"); // cellular_component
		if (cc != null) {
			String ccKey = getGoSubOntology(cc, graph);
			if (ccKey == null) 
				throw new RuntimeException("Could not retrieve sub-ontology for GO:0005575 (celluar_component). The value of the OBO-namespace tag does not exist.");
			
			map.put(ccKey, "C");
		}
		
		if (map.isEmpty() || map.containsKey(null)) {
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
			String evidenceCls = annotation.getShortEvidence();
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
					if (!equalsList(a1.getReferenceIds(), a2.getReferenceIds())) {
						add = true;
						break;
					}
					if (!StringUtils.equals(a1.getRelation(), a2.getRelation())) {
						add = true;
						break;
					}
					if (!equals(a1.getActsOnTaxonId(), a2.getActsOnTaxonId())) {
						add = true;
						break;
					}
					if (!StringUtils.equals(a1.getLastUpdateDate(), a2.getLastUpdateDate())) {
						add = true;
						break;
					}
					if (!equalsExprs(a1.getExtensionExpressions(), a2.getExtensionExpressions())) {
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
		
		private <T1,T2> boolean equals(Pair<T1,T2> p1, Pair<T1,T2> p2) {
			if (p1 == null) {
				return p1 == p2;
			}
			return p1.equals(p2);
		}
		
		private boolean equalsList(List<String> l1, List<String> l2) {
			if (l1 == null && l2 == null) {
				return true;
			}
			if (l1 == null|| l2 == null) {
				return false;
			}
			if (l1.size() != l2.size()) {
				return false;
			}
			boolean matches = true;
			for (int i = 0; i < l2.size(); i++) {
				boolean eq = StringUtils.equals(l1.get(i), l2.get(i));
				if (eq == false) {
					 matches = false;
					 break;
				}
			}
			return matches;
		}
		
		private boolean equalsExprs(List<List<ExtensionExpression>> l1, List<List<ExtensionExpression>> l2) {
			if (l1 == null && l2 == null) {
				return true;
			}
			if (l1 == null|| l2 == null) {
				return false;
			}
			if (l1.size() != l2.size()) {
				return false;
			}
			boolean matches = true;
			for (int i = 0; i < l2.size(); i++) {
				boolean eq = equalsExpr(l1.get(i), l2.get(i));
				if (eq == false) {
					 matches = false;
					 break;
				}
			}
			return matches;
		}
		
		private boolean equalsExpr(List<ExtensionExpression> l1, List<ExtensionExpression> l2) {
			if (l1 == null && l2 == null) {
				return true;
			}
			if (l1 == null|| l2 == null) {
				return false;
			}
			if (l1.size() != l2.size()) {
				return false;
			}
			boolean matches = true;
			for (int i = 0; i < l2.size(); i++) {
				ExtensionExpression expr1 = l1.get(i);
				ExtensionExpression expr2 = l2.get(i);
				if (expr1 == null) {
					if (expr1 != expr2) {
						matches = false;
						break;
					}
				}
				boolean eq = expr1.equals(expr2);
				if (eq == false) {
					 matches = false;
					 break;
				}
			}
			return matches;
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
	
	@Override
	public List<Prediction> predictForBioEntities(Map<Bioentity, ? extends Collection<GeneAnnotation>> annMap) {
		List<Prediction> allPredictions = new ArrayList<Prediction>();
		for(Entry<Bioentity, ? extends Collection<GeneAnnotation>> entry : annMap.entrySet()) {
			allPredictions.addAll(predictForBioEntity(entry.getKey(), entry.getValue()));
		}
		return allPredictions;
	}

	public List<Prediction> predictForBioEntity(Bioentity e, Collection<GeneAnnotation> annotations) {
		OWLGraphWrapper g = getGraph();
		AllPreditions allPredictions = new AllPreditions();
		Map<String, List<GeneAnnotation>> annotationsByEvidence = new HashMap<String, List<GeneAnnotation>>();
		for (GeneAnnotation ann : annotations) {
			
			// TODO move the exclusion list to it's own function for better customization
			final String evidenceCls = ann.getShortEvidence();
			if (evidenceCls.equals("ND")) {
				// ignore top level annotations
				// Do *not* propagate
				continue;
			}
			if (SKIP_IEA && "IEA".equals(evidenceCls)) {
				// Do *not* propagate from IEA
				continue;
			}
			if ("GOC".equals(ann.getAssignedBy())) {
				// if the annotation was assigned by the GOC, assume it is an previous prediction and ignore it.
				continue;
			}
			
			// add to group
			List<GeneAnnotation> group = annotationsByEvidence.get(evidenceCls);
			if (group == null) {
				group = new ArrayList<GeneAnnotation>();
				annotationsByEvidence.put(evidenceCls, group);
			}
			group.add(ann);
			
			if (ann.hasQualifiers()) {
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
				p.setReason(createReason(linkedClass, aspect, cid, evidenceCls, g));
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
	
	private String createReason(OWLClass predicted, String type, String source, String evidence, OWLGraphWrapper g) {
		StringBuilder sb = new StringBuilder();
		sb.append(g.getIdentifier(predicted));
		sb.append('\t');
		String predictedLabel = g.getLabel(predicted);
		if (predictedLabel != null) {
			sb.append(predictedLabel);
		}
		sb.append('\t');
		sb.append("AnnotationPropagation");
		sb.append('\t');
		sb.append(type);
		sb.append('\t');
		sb.append(source);
		sb.append('\t');
		OWLClass sourceCls = g.getOWLClassByIdentifier(source);
		if(sourceCls != null) {
			String sourceLabel = g.getLabel(sourceCls);
			if (sourceLabel != null) {
				sb.append(sourceLabel);
			}
		}
		sb.append('\t');
		sb.append(evidence);
		return sb.toString();
	}
	
	private Set<OWLClass> getIsaPartofSuperClassClosureAndAspect(Collection<GeneAnnotation> annotations, String aspect) {
		OWLGraphWrapper g = getGraph();
		Set<OWLClass> classes = new HashSet<OWLClass>();
		for (GeneAnnotation ann : annotations) {
			if(aspect.equals(ann.getAspect())) {
				OWLClass cls = g.getOWLClassByIdentifierNoAltIds(ann.getCls());
				if (cls != null) {
					// may not find a class, if an alt_id is used
					classes.add(cls);	
				}
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
		
		// c6 referenceIds
		annP.addReferenceIds(source.getReferenceIds());
		
		// c7 evidence
		annP.setEvidence(source.getShortEvidence(), source.getEcoEvidenceCls());
		
		// c8 with expression
		// because we propagate the evidence code, we also have to propagate the with column
		annP.setWithInfos(source.getWithInfos());
		
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
