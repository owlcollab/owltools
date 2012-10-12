package owltools.gaf.rules.go;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyIRIMapper;

import owltools.OWLToolsTestBasics;
import owltools.gaf.GafDocument;
import owltools.gaf.GafObjectsBuilder;
import owltools.gaf.GeneAnnotation;
import owltools.gaf.rules.AnnotationRule;
import owltools.gaf.rules.AnnotationRuleViolation;
import owltools.graph.OWLGraphWrapper;
import owltools.io.CatalogXmlIRIMapper;
import owltools.io.ParserWrapper;

/**
 * Tests for {@link GoIPICatalyticActivityRestrictionsRule}.
 */
public class GoIPICatalyticActivityRestrictionsRuleTest extends OWLToolsTestBasics {

	@Test
	public void test() throws Exception {
		GafObjectsBuilder builder = new GafObjectsBuilder();
		GafDocument gafdoc = builder.buildDocument(getResource("test_gene_association_mgi.gaf"));
		
		ParserWrapper p = new ParserWrapper();
		OWLOntologyIRIMapper mapper = new CatalogXmlIRIMapper(getResource("rules/ontology/extensions/catalog-v001.xml"));
		p.addIRIMapper(mapper);
		OWLOntology goTaxon = p.parse("http://purl.obolibrary.org/obo/go/extensions/x-taxon-importer.owl");
		
		AnnotationRule rule = new GoIPICatalyticActivityRestrictionsRule(new OWLGraphWrapper(goTaxon), getGraph("eco.obo"));
		List<GeneAnnotation> annotations = gafdoc.getGeneAnnotations();
		
		List<AnnotationRuleViolation> allViolations = new ArrayList<AnnotationRuleViolation>();
		for (GeneAnnotation annotation : annotations) {
			Set<AnnotationRuleViolation> violations = rule.getRuleViolations(annotation);
			if (violations != null && !violations.isEmpty()) {
				allViolations.addAll(violations);
			}
		}
		assertEquals(0, allViolations.size());
	}

}
