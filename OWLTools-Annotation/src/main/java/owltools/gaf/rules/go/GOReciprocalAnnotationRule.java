package owltools.gaf.rules.go;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.gaf.Bioentity;
import owltools.gaf.GafDocument;
import owltools.gaf.GeneAnnotation;
import owltools.gaf.eco.TraversingEcoMapper;
import owltools.gaf.rules.AbstractAnnotationRule;
import owltools.gaf.rules.AnnotationRuleViolation;
import owltools.gaf.rules.AnnotationRuleViolation.ViolationType;
import owltools.graph.OWLGraphWrapper;

/**
 * GO_AR:0000004
 */
public class GOReciprocalAnnotationRule extends AbstractAnnotationRule {
	
	/**
	 * The string to identify this class in the annotation_qc.xml and related factories.
	 * This is not supposed to be changed. 
	 */
	public static final String PERMANENT_JAVA_ID = "org.geneontology.rules.GO_AR_0000004";
	
	private static final String PROTEIN_BINDING_GO_ID = "GO:0005515";
	
	private final OWLGraphWrapper graph;
	private final Set<String> evidences;
	private final Set<OWLClass> proteinBindingClasses;

	public GOReciprocalAnnotationRule(OWLGraphWrapper graph, TraversingEcoMapper eco) {
		this.graph = graph;
		evidences = eco.getAllValidEvidenceIds("IPI",  true);
		
		OWLClass proteinBindingCls = graph.getOWLClassByIdentifier(PROTEIN_BINDING_GO_ID);
		if (proteinBindingCls == null) {
			throw new RuntimeException("No class found for identifier: "+PROTEIN_BINDING_GO_ID);
		}
		ElkReasonerFactory factory = new ElkReasonerFactory();
		OWLReasoner reasoner = null;
		try {
			reasoner = factory.createReasoner(graph.getSourceOntology());
			proteinBindingClasses = new HashSet<OWLClass>();
			proteinBindingClasses.add(proteinBindingCls);
			Set<OWLClass> subClasses = reasoner.getSubClasses(proteinBindingCls, false).getFlattened();
			for (OWLClass cls : subClasses) {
				if (!cls.isBottomEntity() && !cls.isTopEntity()) {
					proteinBindingClasses.add(cls);
				}
			}
		}
		finally {
			if (reasoner != null) {
				reasoner.dispose();
			}
		}
	}
	
	@Override
	public Set<AnnotationRuleViolation> getRuleViolations(GeneAnnotation a) {
		return Collections.emptySet();
	}
	
	@Override
	public boolean isAnnotationLevel() {
		return false;
	}

	@Override
	public Set<AnnotationRuleViolation> getRuleViolations(GafDocument gafDoc) {
		/*
		 * Algorithm for detecting the missing reciprocal IPI protein binding
		 * annotations:
		 * 
		 * Two pass algorithm
		 * 
		 * First pass:
		 * for each relevant annotation
		 *  add inverted pair to map, with annotation as value (for reporting)
		 * 
		 * Second pass:
		 * for each annotation
		 *  remove pair from map
		 * 
		 * If map is empty all reciprocal annotations are in the file,
		 * otherwise report recommended new annotations
		 */
		List<GeneAnnotation> relevantAnnotations = new ArrayList<GeneAnnotation>();
		Map<Pair, GeneAnnotation> reciprocals = new HashMap<Pair, GeneAnnotation>();
		
		// first pass
		for (GeneAnnotation ann : gafDoc.getGeneAnnotations()) {
			if (isIPI(ann) && isProteinBinding(ann)) {
				relevantAnnotations.add(ann);
				// bioEntity
				Bioentity bioentityObject = ann.getBioentityObject();
				String bioEntityId = bioentityObject.getId();
				// binds to withField
				Collection<String> withInfos = ann.getWithInfos();
				if (!withInfos.isEmpty()) {
					for (String with : withInfos) {
						// assume its a bioEntity id
						Pair p = new Pair(with, bioEntityId);
						reciprocals.put(p, ann);
					}
				}
			}
		}
		
		// second pass
		for (GeneAnnotation ann : relevantAnnotations) {
			// bioEntity
			Bioentity bioentityObject = ann.getBioentityObject();
			String bioEntityId = bioentityObject.getId();
			// binds to withField
			Collection<String> withInfos = ann.getWithInfos();
			if (!withInfos.isEmpty()) {
				for (String with : withInfos) {
					// assume its a bioEntity id
					Pair p = new Pair(bioEntityId, with);
					reciprocals.remove(p);
				}
			}
		}
		
		if (reciprocals.isEmpty() == false) {
			Set<AnnotationRuleViolation> results = new HashSet<AnnotationRuleViolation>();
			for (Pair p : reciprocals.keySet()) {
				GeneAnnotation ann = reciprocals.get(p);
				String message = "Missing reciprocal IPI annotation '"+p.entity1+"' with '"+p.entity2+"'";
				results.add(new AnnotationRuleViolation(getRuleId(), message, ann, ViolationType.Recommendation));
			}
			return results;
		}
		return Collections.emptySet();
	}

	private boolean isIPI(GeneAnnotation a) {
		String evidence = a.getShortEvidence();
		return evidence != null && evidences.contains(evidence);
	}
	
	private boolean isProteinBinding(GeneAnnotation a) {
		String cls = a.getCls();
		OWLClass owlClass = graph.getOWLClassByIdentifierNoAltIds(cls);
		return owlClass != null && proteinBindingClasses.contains(owlClass);
	}
	
	static class Pair {
		
		final String entity1;
		final String entity2;
		/**
		 * @param entity1
		 * @param entity2
		 */
		Pair(String entity1, String entity2) {
			this.entity1 = entity1;
			this.entity2 = entity2;
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((entity1 == null) ? 0 : entity1.hashCode());
			result = prime * result
					+ ((entity2 == null) ? 0 : entity2.hashCode());
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
			Pair other = (Pair) obj;
			if (entity1 == null) {
				if (other.entity1 != null)
					return false;
			} else if (!entity1.equals(other.entity1))
				return false;
			if (entity2 == null) {
				if (other.entity2 != null)
					return false;
			} else if (!entity2.equals(other.entity2))
				return false;
			return true;
		}
	}

	@Override
	public boolean isDocumentLevel() {
		return true;
	}
	
	

}
