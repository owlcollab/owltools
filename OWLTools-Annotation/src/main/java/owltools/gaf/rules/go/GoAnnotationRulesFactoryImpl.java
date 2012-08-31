package owltools.gaf.rules.go;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.OWLOntology;

import owltools.gaf.rules.AnnotationRule;
import owltools.gaf.rules.AnnotationRulesFactoryImpl;
import owltools.gaf.rules.GenericReasonerValidationCheck;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

public class GoAnnotationRulesFactoryImpl extends AnnotationRulesFactoryImpl {
	
	private static final Logger logger = Logger.getLogger(GoAnnotationRulesFactoryImpl.class);

	private final Map<String, AnnotationRule> namedRules;
	
	public GoAnnotationRulesFactoryImpl() {
		this("http://www.geneontology.org/quality_control/annotation_checks/annotation_qc.xml",
				"http://www.geneontology.org/doc/GO.xrf_abbs",
				Arrays.asList("http://www.geneontology.org/ontology/editors/gene_ontology_write.obo",
					"http://www.geneontology.org/quality_control/annotation_checks/taxon_checks/taxon_go_triggers.obo",
					"http://www.geneontology.org/quality_control/annotation_checks/taxon_checks/ncbi_taxon_slim.obo",
					"http://www.geneontology.org/quality_control/annotation_checks/taxon_checks/taxon_union_terms.obo"),
					"http://purl.obolibrary.org/obo/eco.owl");
	}
	
	public GoAnnotationRulesFactoryImpl(OWLGraphWrapper graph, OWLGraphWrapper eco) {
		this("http://www.geneontology.org/quality_control/annotation_checks/annotation_qc.xml",
				"http://www.geneontology.org/doc/GO.xrf_abbs", graph, eco);
	}
	
	public GoAnnotationRulesFactoryImpl(String qcfile, String xrfabbslocation, List<String> ontologies, String eco) {
		this(qcfile, xrfabbslocation, getOntologies(ontologies), getEco(eco));
	}
	
	public GoAnnotationRulesFactoryImpl(String qcfile, String xrfabbslocation, OWLGraphWrapper graph, OWLGraphWrapper eco) {
		super(qcfile, graph);
		logger.info("Start preparing ontology checks");
		namedRules = new HashMap<String, AnnotationRule>();
		namedRules.put(BasicChecksRule.PERMANENT_JAVA_ID,  new BasicChecksRule(xrfabbslocation));
		namedRules.put(GoAnnotationTaxonRule.PERMANENT_JAVA_ID, new GoAnnotationTaxonRule(graph));
		namedRules.put(GoClassReferenceAnnotationRule.PERMANENT_JAVA_ID, new GoClassReferenceAnnotationRule(graph));
		namedRules.put(GenericReasonerValidationCheck.PERMANENT_JAVA_ID, new GenericReasonerValidationCheck());
		namedRules.put(GoNoISSProteinBindingRule.PERMANENT_JAVA_ID, new GoNoISSProteinBindingRule(eco));
		namedRules.put(GoBindingCheckWithFieldRule.PERMANENT_JAVA_ID, new GoBindingCheckWithFieldRule(eco));
		namedRules.put(GoIEPRestrictionsRule.PERMANENT_JAVA_ID, new GoIEPRestrictionsRule(graph, eco));
		namedRules.put(GoIPICatalyticActivityRestrictionsRule.PERMANENT_JAVA_ID, new GoIPICatalyticActivityRestrictionsRule(graph, eco));
		namedRules.put(GoICAnnotationRule.PERMANENT_JAVA_ID, new GoICAnnotationRule(eco));
		namedRules.put(GoIDAAnnotationRule.PERMANENT_JAVA_ID, new GoIDAAnnotationRule(eco));
		namedRules.put(GoIPIAnnotationRule.PERMANENT_JAVA_ID, new GoIPIAnnotationRule(eco));
		logger.info("Finished preparing ontology checks");
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
	
	private static OWLGraphWrapper getEco(String location) {
		try {
			ParserWrapper p = new ParserWrapper();
			OWLGraphWrapper wrapper = p.parseToOWLGraph(location);
			return wrapper;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	protected AnnotationRule getClassForName(String className) throws Exception {
		AnnotationRule rule = namedRules.get(className);
		if (rule != null) {
			return rule;
		}
		return super.getClassForName(className);
	}
	
}
