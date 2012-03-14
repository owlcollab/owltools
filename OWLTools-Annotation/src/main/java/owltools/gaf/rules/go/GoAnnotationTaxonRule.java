package owltools.gaf.rules.go;

import owltools.gaf.rules.AnnotationTaxonRule;
import owltools.graph.OWLGraphWrapper;

/**
 * Checks if an annotation is valid according to GO taxon constraints.
 * 
 * Ensure that taxGraphWrapper contains:
 * 
 * <pre>
 * http://www.geneontology.org/ontology/obo_format_1_2/gene_ontology_ext.obo
 * http://www.geneontology.org/quality_control/annotation_checks/taxon_checks/taxon_go_triggers.obo
 * http://www.geneontology.org/quality_control/annotation_checks/taxon_checks/ncbi_taxon_slim.obo
 * http://www.geneontology.org/quality_control/annotation_checks/taxon_checks/taxon_union_terms.obo
 * </pre>
 * 
 * See also:
 * http://www.biomedcentral.com/1471-2105/11/530
 * 
 * @author cjm
 *
 */
public class GoAnnotationTaxonRule extends AnnotationTaxonRule {
	
	private static final String neverId = "RO:0002161";
	private static final String onlyId = "RO:0002160";
	
	/**
	 * @param graphWrapper
	 */
	public GoAnnotationTaxonRule(OWLGraphWrapper graphWrapper) {
		super(graphWrapper, neverId, onlyId);
	}

}

