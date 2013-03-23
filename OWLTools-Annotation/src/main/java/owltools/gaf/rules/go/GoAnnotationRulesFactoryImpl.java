package owltools.gaf.rules.go;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import owltools.gaf.eco.EcoMapper;
import owltools.gaf.eco.EcoMapperFactory;
import owltools.gaf.eco.TraversingEcoMapper;
import owltools.gaf.rules.AnnotationRule;
import owltools.gaf.rules.AnnotationRulesFactoryImpl;
import owltools.gaf.rules.GenericReasonerValidationCheck;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

public class GoAnnotationRulesFactoryImpl extends AnnotationRulesFactoryImpl {
	
	private static final Logger logger = Logger.getLogger(GoAnnotationRulesFactoryImpl.class);

	private final Map<String, AnnotationRule> namedRules;
	
	public GoAnnotationRulesFactoryImpl(ParserWrapper parserWrapper) {
		this("http://www.geneontology.org/quality_control/annotation_checks/annotation_qc.xml",
				"http://www.geneontology.org/doc/GO.xrf_abbs",
				parserWrapper,
				"http://purl.obolibrary.org/obo/go/extensions/x-taxon-importer.owl",
				"http://purl.obolibrary.org/obo/eco.owl");
	}
	
	public GoAnnotationRulesFactoryImpl(String qcfile, String xrfabbslocation, ParserWrapper p, String go, String eco) {
		this(qcfile, xrfabbslocation, getGO(p, go), getEco(p, eco));
	}
	
	public GoAnnotationRulesFactoryImpl(OWLGraphWrapper graph, TraversingEcoMapper eco) {
		this("http://www.geneontology.org/quality_control/annotation_checks/annotation_qc.xml",
				"http://www.geneontology.org/doc/GO.xrf_abbs", graph, eco);
	}

	public GoAnnotationRulesFactoryImpl(String qcfile, String xrfabbslocation, OWLGraphWrapper graph, TraversingEcoMapper eco) {
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
		namedRules.put(GoNDAnnotationRule.PERMANENT_JAVA_ID, new GoNDAnnotationRule(eco));
		namedRules.put(GOReciprocalAnnotationRule.PERMANENT_JAVA_ID, new GOReciprocalAnnotationRule(graph, eco));
		namedRules.put(GoMultipleTaxonRule.PERMANENT_JAVA_ID, new GoMultipleTaxonRule(graph));
		namedRules.put(GoNoHighLevelTermAnnotationRule.PERMANENT_JAVA_ID, new GoNoHighLevelTermAnnotationRule(graph, eco));
		logger.info("Finished preparing ontology checks");
	}

	private static OWLGraphWrapper getGO(ParserWrapper p, String location) {
		try {
			OWLGraphWrapper graph =  p.parseToOWLGraph(location);
			return graph;
			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private static TraversingEcoMapper getEco(ParserWrapper p, String location) {
		try {
			TraversingEcoMapper eco = EcoMapperFactory.createTraversingEcoMapper(p, location);
			return eco;
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
