package owltools.gaf.inference;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
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
		OWLGraphWrapper graph = getGraph();
		ElkReasonerFactory factory = new ElkReasonerFactory();
		// assumes that all support ontologies have either been merged into or added as import
		reasoner = factory.createReasoner(graph.getSourceOntology());
		propagationRules = createPropagationRules(graph, reasoner);
		aspectMap = createDefaultAspectMap(graph);
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
				
				Set<OWLClass> nonRedundantLinks = getNonRedundantLinkedClasses(mfClass, part_of, graph, reasoner, bpClasses);
				
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
			for(OWLClass bpClass : bpClasses) {
				List<String> bpClassSubsets = graph.getSubsets(bpClass);
				if (bpClassSubsets.contains(gocheck_do_not_annotate)) {
					// do not propagate from do not annotate terms
					continue;
				}
				
				Set<OWLClass> nonRedundantLinks = getNonRedundantLinkedClasses(bpClass, occurs_in, graph, reasoner, ccClasses);
				
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
	 * Retrieve the non redundant set of linked classes using the given relation.
	 * The reasoner is used to infer the super and subsets for the subClass hierarchy.
	 * Only return classes, which are in the given super set (a.k.a. ontology branch).
	 * 
	 * @param c
	 * @param property
	 * @param g
	 * @param reasoner
	 * @param superSet
	 * @return set of linked classes, never null 
	 */
	protected static Set<OWLClass> getNonRedundantLinkedClasses(OWLClass c, OWLObjectProperty property, OWLGraphWrapper g, OWLReasoner reasoner, Set<OWLClass> superSet) {
		// get all superClasses for the current class
		Set<OWLClass> currentSuperClasses = reasoner.getSuperClasses(c, false).getFlattened();
		currentSuperClasses.add(c);
		Set<OWLClass> linkedClasses = new HashSet<OWLClass>();
		for (OWLClass currentSuperClass : currentSuperClasses) {
			if (currentSuperClass.isBuiltIn()) {
				continue;
			}
			// find all direct links via the property to the selected super set
			linkedClasses.addAll(getDirectLinkedClasses(currentSuperClass, property, g, superSet));
		}
		// create remove redundant super classes from link set
		Set<OWLClass> nonRedundantLinks = reduceToNonRedundant(linkedClasses, reasoner);
		
		return nonRedundantLinks;
	}
	
	/**
	 * Lookup relation super classes in graph g for a given sub class c and property p.
	 * Only retain super classes which are in the given super set.
	 * 
	 * @param c
	 * @param property
	 * @param g
	 * @param superSet
	 * @return set of super classes, never null
	 */
	protected static Set<OWLClass> getDirectLinkedClasses(OWLClass c, OWLObjectProperty property, OWLGraphWrapper g, Set<OWLClass> superSet) {
		Set<OWLClass> links = new HashSet<OWLClass>();
		
		for(OWLOntology o : g.getAllOntologies()) {
			// check subClass axioms
			for (OWLSubClassOfAxiom sca : o.getSubClassAxiomsForSubClass(c)) {
				OWLClassExpression ce = sca.getSuperClass();
				if (ce instanceof OWLObjectSomeValuesFrom) {
					OWLObjectSomeValuesFrom someValuesFrom = (OWLObjectSomeValuesFrom) ce;
					OWLObjectPropertyExpression currentProperty = someValuesFrom.getProperty();
					if (property.equals(currentProperty)) {
						OWLClassExpression filler = someValuesFrom.getFiller();
						if (filler.isAnonymous() == false) {
							OWLClass fillerCls = filler.asOWLClass();
							if (superSet.contains(fillerCls)) {
								links.add(fillerCls);
							}
						}
					}
				}
			}
			// check equivalent classes axioms
			for (OWLEquivalentClassesAxiom eqa : o.getEquivalentClassesAxioms(c)) {
				for (OWLClassExpression ce : eqa.getClassExpressions()) {
					if (ce instanceof OWLObjectSomeValuesFrom) {
						OWLObjectSomeValuesFrom someValuesFrom = (OWLObjectSomeValuesFrom) ce;
						OWLObjectPropertyExpression currentProperty = someValuesFrom.getProperty();
						if (property.equals(currentProperty)) {
							OWLClassExpression filler = someValuesFrom.getFiller();
							if (filler.isAnonymous() == false) {
								OWLClass fillerCls = filler.asOWLClass();
								if (superSet.contains(fillerCls)) {
									links.add(fillerCls);
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
	 * (inferred) super classes of another class in the given set.
	 * 
	 * @param linkedClasses
	 * @param reasoner
	 * @return non redundant set, never null
	 */
	protected static Set<OWLClass> reduceToNonRedundant(Set<OWLClass> linkedClasses, OWLReasoner reasoner) {
		Set<OWLClass> nonRedundant = new HashSet<OWLClass>();
		for (OWLClass linked : linkedClasses) {
			Set<OWLClass> subClasses = reasoner.getSubClasses(linked, false).getFlattened();
			boolean noChildrenInLinks = true;
			for (OWLClass subClass : subClasses) {
				if (linkedClasses.contains(subClass)) {
					noChildrenInLinks = false;
					break;
				}
			}
			if (noChildrenInLinks) {
				nonRedundant.add(linked);
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
	

	public Set<Prediction> predictForBioEntity(Bioentity entity, Collection<GeneAnnotation> annotations) {
		Map<OWLClass, Prediction> allPredictions = new HashMap<OWLClass, Prediction>();
		for (GeneAnnotation ann : annotations) {
			// TODO move the exclusion list to it's own function for better customization
			if (ann.getEvidenceCls().equals("ND")) {
				// ignore top level annotations
				// Do *not* propagate
				continue;
			}
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
				if (allPredictions.containsKey(linkedClass)) {
					continue;
				}
				String aspect = aspectMap.get(getSubOntology(linkedClass));
				Prediction p = createPrediction(linkedClass, aspect, cid, ann);
				allPredictions.put(linkedClass, p);
			}
		}
		
		Set<Prediction> predictions = new HashSet<Prediction>();
		
		Set<OWLClass> nonRedundantClasses = reduceToNonRedundant(allPredictions.keySet(), reasoner);
		for (OWLClass nonRedundantClass : nonRedundantClasses) {
			predictions.add(allPredictions.get(nonRedundantClass));
		}
		
		return predictions;
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
		annP.setWithExpression(with);
		
		// c9 aspect
		annP.setAspect(aspect);
		
		// c10-c12
		// bio entity
		
		// c13 taxon
		annP.setActsOnTaxonId(source.getActsOnTaxonId());
		
		// c14 last update - day, when the inference was made
		annP.setLastUpdateDate(new Date());
		
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
