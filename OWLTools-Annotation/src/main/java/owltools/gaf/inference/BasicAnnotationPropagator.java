package owltools.gaf.inference;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLPropertyExpression;

import owltools.gaf.GafDocument;
import owltools.gaf.GeneAnnotation;
import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;
import owltools.graph.OWLQuantifiedProperty;
import owltools.graph.OWLQuantifiedProperty.Quantifier;

/**
 * This performs basic annotation inferences involving propagation
 * between the 3 sub-ontologies in GO
 * 
 * <ul>
 *  <li> MF -> BP over part_of
 *  <li> BP -> CC over occurs_in
 * </ul>
 * 
 * TODO: reimplement using OWL semantics and reasoning
 * 
 * @author cjm
 *
 */
public class BasicAnnotationPropagator extends AbstractAnnotationPredictor implements AnnotationPredictor {

	protected static Logger LOG = Logger.getLogger(BasicAnnotationPropagator.class);
	
	private static final String ASSIGNED_BY_CONSTANT = "GOC";
	
	private final Set<PropagationRule> propagationRules;
	private final Map<String, String> aspectMap;

	/**
	 * Create instance with default propagation rules.
	 * 
	 * @param gafDocument
	 * @param graph
	 */
	public BasicAnnotationPropagator(GafDocument gafDocument, OWLGraphWrapper graph) {
		this(gafDocument, graph, getDefaultGoPropagationRules(graph), createDefaultAspectMap(graph));
	}
	
	/**
	 * Create instance with specified propagation rules and aspect mapping for sub ontologies
	 * 
	 * @param gafDocument
	 * @param graph
	 * @param propagationRules
	 * @param aspectMap 
	 */
	protected BasicAnnotationPropagator(GafDocument gafDocument, OWLGraphWrapper graph, Set<PropagationRule> propagationRules, Map<String, String> aspectMap) {
		super(gafDocument, graph);
		this.propagationRules = propagationRules;
		this.aspectMap = aspectMap;
	}
	
	/**
	 * Create the default propagation rule set for the GeneOntology.
	 * 
	 * @param graph
	 * @return set of valid propagation rules
	 */
	private static Set<PropagationRule> getDefaultGoPropagationRules(OWLGraphWrapper graph) {
		Set<PropagationRule> set = new HashSet<PropagationRule>();
		
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
			set.add(new PropagationRule(getGoSubOntology(mf, graph), getGoSubOntology(bp, graph), part_of));
		}
		else {
			LOG.warn("Skipping MF -> BP over 'part_of'.");
		}
		
		
		// BP -> CC over occurs_in
		if (occurs_in != null && bp != null && cc != null) {
			set.add(new PropagationRule(getGoSubOntology(bp, graph), getGoSubOntology(cc, graph), occurs_in));
		}
		else {
			LOG.warn("Skipping BP -> MF over 'occurs_in'.");
		}
		
		if (set.isEmpty()) {
			// only fail if there are no propagation rules
			// the test case uses a custom ontology, which has no cc or 'occurs_in' relation
			throw new RuntimeException("Could not create any valid propgation rules. Is the correct ontology (GO) loaded?");
		}
		return Collections.unmodifiableSet(set);
	}
	
	private static Map<String, String> createDefaultAspectMap(OWLGraphWrapper graph) {
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
	 * Get the GO specific sub ontology.
	 * 
	 * @param c
	 * @param g
	 * @return sub ontology or null
	 */
	private static String getGoSubOntology(OWLClass c, OWLGraphWrapper g) {
		return g.getNamespace(c);
	}
	
	/**
	 * Triple representing a valid path for the propagation of a gene annotation.
	 * <br>
	 * Provides hashcode and equals methods, to allow use in sets or maps.
	 */
	protected static class PropagationRule {
		
		public final String sourceSubOntology;
		public final String targetSubOntology;
		public final OWLObjectProperty relationship;
		
		
		/**
		 * @param sourceSubOntology
		 * @param targetSubOntology
		 * @param relationship
		 */
		protected PropagationRule(String sourceSubOntology, String targetSubOntology, OWLObjectProperty relationship) {
			this.sourceSubOntology = sourceSubOntology;
			this.targetSubOntology = targetSubOntology;
			this.relationship = relationship;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((relationship == null) ? 0 : relationship.hashCode());
			result = prime * result
					+ ((sourceSubOntology == null) ? 0 : sourceSubOntology.hashCode());
			result = prime * result
					+ ((targetSubOntology == null) ? 0 : targetSubOntology.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			PropagationRule other = (PropagationRule) obj;
			if (relationship == null) {
				if (other.relationship != null)
					return false;
			} else if (!relationship.equals(other.relationship))
				return false;
			if (sourceSubOntology == null) {
				if (other.sourceSubOntology != null)
					return false;
			} else if (!sourceSubOntology.equals(other.sourceSubOntology))
				return false;
			if (targetSubOntology == null) {
				if (other.targetSubOntology != null)
					return false;
			} else if (!targetSubOntology.equals(other.targetSubOntology))
				return false;
			return true;
		}
	}

	@SuppressWarnings("rawtypes")
	public Set<Prediction> predict(String bioentity) {
		
		Set<Prediction> predictions = new HashSet<Prediction>();
		Set<GeneAnnotation> anns = getGafDocument().getGeneAnnotations(bioentity);
		Set<OWLClass> aClasses = new HashSet<OWLClass>();
		
		for (GeneAnnotation ann : anns) {
			// TODO move the exclusion list to it's own function for better customization
			if (ann.getEvidenceCls().equals("ND")) {
				// ignore top level annotations
				continue;
			}
			String compositeQualifier = StringUtils.trimToEmpty(ann.getCompositeQualifier());
			if (compositeQualifier.isEmpty() == false) {
				// ignore annotations with a qualifier.
				// Do *not* propagate
				continue;
			}

			String cid = ann.getCls();
			OWLClass c = getGraph().getOWLClassByIdentifier(cid);
			String subOnt = getSubOntology(c);
			if (subOnt == null) {
				// quick check: ignore null sub branches
				continue;
			}

			// TODO at some point this should be done with a reasoner, who also return anonymous class expressions, such as 'part_of some OWLClass'
			final Set<OWLGraphEdge> outgoingEdgesClosure = getGraph().getOutgoingEdgesClosure(c);
			for (OWLGraphEdge e : outgoingEdgesClosure) {
				final OWLObject target = e.getTarget();
				if (target instanceof OWLClass) {
					final OWLClass targetClass = (OWLClass)target;
					
					String ancSubOnt = getSubOntology(targetClass);
					if (ancSubOnt == null) {
						// quick check: ignore null sub branches 
						continue;
					}
					boolean sameSubOntology = StringUtils.equals(subOnt, ancSubOnt);
					if (sameSubOntology) {
						// quick check: ignore pairs with the same sub ontology branch
						continue;
					}
					final List<OWLQuantifiedProperty> propertyList = e.getQuantifiedPropertyList();
					if (propertyList.size() != 1) {
						// quick check: ignore chains
						continue;
					}
					final OWLQuantifiedProperty owlQuantifiedProperty = propertyList.get(0);
					if (owlQuantifiedProperty.getQuantifier() != Quantifier.SOME) {
						// quick check: require some
						continue;
					}
					OWLObjectProperty prop = owlQuantifiedProperty.getProperty();
					if (prop == null) {
						// quick check: ignore null properties
						continue;
					}
					// check that this triple matches a valid rule
					if (propagationRules.contains(new PropagationRule(subOnt, ancSubOnt, prop))) {
						// NEW INFERENCES
						aClasses.add(c);
						String aspect = aspectMap.get(ancSubOnt);
						predictions.add(getPrediction(targetClass, aspect , ann));
					}
				}
			}
		}

		// filter redundant annotations, use only the subClassOf hierarchy
		Set<OWLPropertyExpression> set = Collections.emptySet(); // only subClassOf
		setAndFilterRedundantPredictions(predictions, aClasses, set);
		return predictions;
	}

	protected String getSubOntology(OWLClass c) {
		return getGoSubOntology(c, getGraph());
	}

	protected Prediction getPrediction(OWLClass c, String aspect, GeneAnnotation source) {
		
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
		annP.setEvidenceCls("IC");
		
		// c8 with expression
		annP.setWithExpression(source.getWithExpression());
		
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



}
