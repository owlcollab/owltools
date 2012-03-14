package owltools.gaf.rules.go;

import java.util.Arrays;
import java.util.List;

import org.semanticweb.owlapi.model.OWLOntology;

import owltools.gaf.rules.AnnotationRule;
import owltools.gaf.rules.AnnotationRulesFactoryImpl;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

public class GoAnnotationRulesFactoryImpl extends AnnotationRulesFactoryImpl {

	private final BasicChecksRule basicChecksRule;
	private final GoAnnotationTaxonRule taxonRule;
	private final GoClassReferenceAnnotationRule referenceAnnotationRule;
	
	public GoAnnotationRulesFactoryImpl() {
		this("http://www.geneontology.org/quality_control/annotation_checks/annotation_qc.xml",
				"http://www.geneontology.org/doc/GO.xrf_abbs",
				"http://www.geneontology.org/ontology/editors/gene_ontology_write.obo",
				Arrays.asList("http://www.geneontology.org/ontology/obo_format_1_2/gene_ontology_ext.obo",
					"http://www.geneontology.org/quality_control/annotation_checks/taxon_checks/taxon_go_triggers.obo",
					"http://www.geneontology.org/quality_control/annotation_checks/taxon_checks/ncbi_taxon_slim.obo",
					"http://www.geneontology.org/quality_control/annotation_checks/taxon_checks/taxon_union_terms.obo"));
	}
	
	public GoAnnotationRulesFactoryImpl(String qcfile, String xrfabbslocation, String ontologylocation, List<String> taxonomylocation) {
		this(qcfile, xrfabbslocation, getOntology(ontologylocation), getOntologies(taxonomylocation));
	}
	
	public GoAnnotationRulesFactoryImpl(String qcfile, String xrfabbslocation, OWLGraphWrapper graphWrapper, OWLGraphWrapper taxGraphWrapper) {
		super(qcfile);
		basicChecksRule = new BasicChecksRule(xrfabbslocation);
		taxonRule = new GoAnnotationTaxonRule(graphWrapper, taxGraphWrapper);
		referenceAnnotationRule = new GoClassReferenceAnnotationRule(graphWrapper);
	}
	
	private static OWLGraphWrapper getOntology(String ontologylocation) {
		try {
			ParserWrapper p = new ParserWrapper();
			OWLGraphWrapper graphWrapper = p.parseToOWLGraph(ontologylocation);
			return graphWrapper;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private static OWLGraphWrapper getOntologies(List<String> ontologylocations) {
		try {
			ParserWrapper p = new ParserWrapper();
			OWLGraphWrapper wrapper = null;
			for (String location : ontologylocations) {
				if(wrapper==null){
					wrapper = p.parseToOWLGraph(location);
				}else{
					wrapper.addSupportOntology(p.parse(location));
				}
			}
			for(OWLOntology sont: wrapper.getSupportOntologySet()){
				wrapper.mergeOntology(sont);
			}
			return wrapper;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	} 
	
	@Override
	protected AnnotationRule getClassForName(String className) throws Exception {
		if ("org.geneontology.gold.rules.BasicChecksRule".equals(className)) {
			return basicChecksRule;
		}
		if ("org.geneontology.gold.rules.AnnotationTaxonRule".equals(className)) {
			return taxonRule;
		}
		if ("org.geneontology.gold.rules.GoClassReferenceAnnotationRule".equals(className)) {
			return referenceAnnotationRule;
		}
		return super.getClassForName(className);
	}
}
