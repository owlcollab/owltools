package owltools.gaf.inference;

import static org.junit.Assert.*;

import java.io.BufferedWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLClass;

import owltools.OWLToolsTestBasics;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

public class ClassTaxonMatrixTest extends OWLToolsTestBasics {

	private static OWLGraphWrapper go;
	private static OWLClass rat;
	private static OWLClass yeast;
	private static Set<OWLClass> classes;


	@BeforeClass
	public static void beforeClass() throws Exception {
		ParserWrapper pw = new ParserWrapper();
		go = pw.parseToOWLGraph(getResourceIRIString("rules/taxon/gene_ontology_ext.obo"));
		classes = go.getSourceOntology().getClassesInSignature();
		go.mergeOntology(pw.parse(getResourceIRIString("rules/taxon/ncbi_taxon_slim.obo")));
		go.mergeOntology(pw.parse(getResourceIRIString("rules/taxon/taxon_go_triggers.obo")));
		go.mergeOntology(pw.parse(getResourceIRIString("rules/taxon/taxon_union_terms.obo")));

		rat = go.getOWLClassByIdentifier("NCBITaxon:10114");
		yeast = go.getOWLClassByIdentifier("NCBITaxon:4932");
	}
	
	@Test
	public void testMatrix() throws Exception {
		ClassTaxonMatrix m = ClassTaxonMatrix.create(go, classes, Arrays.asList(rat, yeast));
		
		StringWriter stringWriter = new StringWriter();
		BufferedWriter writer = new BufferedWriter(stringWriter);
		ClassTaxonMatrix.write(m, writer);
		writer.close();
		
		// test: writing does not throw any exceptions and is non-empty
		String writtenMatrix = stringWriter.getBuffer().toString();
		assertTrue(writtenMatrix.length() > 0); 
		
		// spindle pole body duplication in nuclear envelope
		checkTerm(m, "GO:0007103", false, true); 
		
		// plant-type cell wall cellulose metabolic process
		checkTerm(m, "GO:0052541", false, false);
		
		// branching involved in mammary gland duct morphogenesis
		checkTerm(m, "GO:0060444", true, false);
	}

	
	private void checkTerm(ClassTaxonMatrix m, String term, boolean...expected) {
		OWLClass owlClass = go.getOWLClassByIdentifier(term);
		assertEquals(expected[0], m.get(owlClass, rat));
		assertEquals(expected[1], m.get(owlClass, yeast));
	}
}
