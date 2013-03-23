package owltools.gaf.rules.go;

import static org.junit.Assert.*;

import java.util.Set;

import org.junit.Test;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyIRIMapper;

import owltools.gaf.GafDocument;
import owltools.gaf.GafObjectsBuilder;
import owltools.gaf.rules.AnnotationRule;
import owltools.gaf.rules.AnnotationRuleViolation;
import owltools.graph.OWLGraphWrapper;
import owltools.io.CatalogXmlIRIMapper;
import owltools.io.ParserWrapper;

public class GOReciprocalAnnotationRuleTest extends AbstractRuleTestHelper {

	@Test
	public void test() throws Exception {
		GafObjectsBuilder builder = new GafObjectsBuilder();
		GafDocument gafdoc = builder.buildDocument(getResource("test_gene_association_mgi.gaf"));
		
		ParserWrapper p = new ParserWrapper();
		OWLOntologyIRIMapper mapper = new CatalogXmlIRIMapper(getResource("rules/ontology/extensions/catalog-v001.xml"));
		p.addIRIMapper(mapper);
		OWLOntology goTaxon = p.parse("http://purl.obolibrary.org/obo/go/extensions/x-taxon-importer.owl");
		
		
		AnnotationRule rule = new GOReciprocalAnnotationRule(new OWLGraphWrapper(goTaxon), eco);
		
		Set<AnnotationRuleViolation> violations = rule.getRuleViolations(gafdoc);
		assertEquals(5, violations.size());
	}

}
