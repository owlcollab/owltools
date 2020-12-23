package owltools.gaf.rules.go;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import owltools.gaf.GeneAnnotation;
import owltools.gaf.rules.AbstractAnnotationRule;
import owltools.gaf.rules.AnnotationRuleViolation;
import owltools.gaf.rules.AnnotationRuleViolation.ViolationType;
import owltools.graph.OWLGraphWrapper;

/**
 * GO_AR:0000015
 */
public class GoMultipleTaxonRule extends AbstractAnnotationRule {
	
	/**
	 * The string to identify this class in the annotation_qc.xml and related factories.
	 * This is not supposed to be changed. 
	 */
	public static final String PERMANENT_JAVA_ID = "org.geneontology.rules.GO_AR_0000015";
	
	private static final String GO_ID_MULTI_ORGANISM_PROCESS = "GO:0051704"; // multi-organism process
	
	private final OWLGraphWrapper graph;
	private final Set<OWLClass> multiOrganismClasses;
	
	public GoMultipleTaxonRule(OWLGraphWrapper graph) {
		this.graph = graph;
		multiOrganismClasses = new HashSet<OWLClass>();
		
		OWLClass mop = graph.getOWLClassByIdentifier(GO_ID_MULTI_ORGANISM_PROCESS);
		if (mop == null) {
			throw new RuntimeException("Could not find class for 'multi-organism process' id: "+GO_ID_MULTI_ORGANISM_PROCESS);
		}
		multiOrganismClasses.add(mop);
		OWLReasonerFactory factory = new ReasonerFactory();
		OWLReasoner reasoner = null;
		try {
			reasoner = factory.createReasoner(graph.getSourceOntology());
			Set<OWLClass> subClasses = reasoner.getSubClasses(mop, false).getFlattened();
			for (OWLClass owlClass : subClasses) {
				if (owlClass.isBottomEntity() || owlClass.isTopEntity()) {
					continue;
				}
				multiOrganismClasses.add(owlClass);
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
		String actsOnTaxonId = null;
		Pair<String, String> pair = a.getActsOnTaxonId();
		if (pair != null) {
			actsOnTaxonId = pair.getLeft();
		}
		if (actsOnTaxonId == null || actsOnTaxonId.isEmpty()) {
			return Collections.emptySet();
		}
		
		final OWLClass tax2 = graph.getOWLClassByIdentifierNoAltIds(actsOnTaxonId);
		if (tax2 == null) {
			AnnotationRuleViolation v = new AnnotationRuleViolation(getRuleId(), "Could not retrieve a class for taxonCls: "+actsOnTaxonId, a, ViolationType.Warning);
			return Collections.singleton(v);
		}
		
		String annotationCls = a.getCls();
		String taxonCls = a.getBioentityObject().getNcbiTaxonId();
		
		if (taxonCls == null) {
			return Collections.emptySet();
		}
		
		final OWLObject cls = graph.getOWLObjectByIdentifier(annotationCls);
		final OWLObject tax = graph.getOWLObjectByIdentifier(taxonCls);
		
		if (cls == null) {
			return Collections.emptySet();
		}
		if (tax2.equals(tax) == false && multiOrganismClasses.contains(cls) == false) {
			AnnotationRuleViolation v = new AnnotationRuleViolation(getRuleId(), "While using dual taxons, multiple organism terms are required, but was: "+annotationCls, a, ViolationType.Warning);
			return Collections.singleton(v);
		}
		
		return Collections.emptySet();
	}

}
