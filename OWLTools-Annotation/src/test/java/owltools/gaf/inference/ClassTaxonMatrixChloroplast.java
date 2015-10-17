package owltools.gaf.inference;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;

import owltools.OWLToolsTestBasics;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

public class ClassTaxonMatrixChloroplast extends OWLToolsTestBasics {

	private static OWLGraphWrapper all;

	private static OWLClass cell_component;
	private static OWLClass chloroplast;
	private static OWLClass cytoplasm;

	private static OWLClass Bacteria;
	private static OWLClass Archaea;
	private static OWLClass Eukaryota;
	private static OWLClass Metazoa;
	private static OWLClass Fungi;
	
	
	private static Set<OWLClass> relevant;
	private static Set<OWLClass> taxa;


	@BeforeClass
	public static void beforeClass() throws Exception {
		ParserWrapper pw = new ParserWrapper();
		IRI iri = IRI.create(new File("src/test/resources/chloroplast-taxon-check-test.obo").getCanonicalFile());
		all = new OWLGraphWrapper(pw.parseOWL(iri));
		
		cell_component = all.getOWLClassByIdentifier("GO:0005575");
		chloroplast = all.getOWLClassByIdentifier("GO:0009507");
		cytoplasm = all.getOWLClassByIdentifier("GO:0005737");
		
		Bacteria = all.getOWLClassByIdentifier("NCBITaxon:2");
		Archaea = all.getOWLClassByIdentifier("NCBITaxon:2157");
		Eukaryota = all.getOWLClassByIdentifier("NCBITaxon:2759");
		Metazoa = all.getOWLClassByIdentifier("NCBITaxon:33208");
		Fungi = all.getOWLClassByIdentifier("NCBITaxon:4751");
		
		relevant = new HashSet<OWLClass>(Arrays.asList(cell_component, chloroplast, cytoplasm));
		taxa = new HashSet<OWLClass>(Arrays.asList(Bacteria, Archaea, Eukaryota, Metazoa, Fungi));
	}
	
	@Test
	public void testMatrix() throws Exception {
		ClassTaxonMatrix m = ClassTaxonMatrix.create(all, relevant, taxa);
		
		StringWriter stringWriter = new StringWriter();
		BufferedWriter writer = new BufferedWriter(stringWriter);
		ClassTaxonMatrix.write(m, all, writer);
		writer.close();
		
		// test: writing does not throw any exceptions and is non-empty
		String writtenMatrix = stringWriter.getBuffer().toString();
		System.out.println(writtenMatrix);
		assertTrue(writtenMatrix.length() > 0); 
		
		assertTrue(violation(chloroplast, Bacteria, m));
		assertTrue(violation(chloroplast, Archaea, m));
		assertFalse(violation(chloroplast, Eukaryota, m));
		assertTrue(violation(chloroplast, Metazoa, m));
		assertTrue(violation(chloroplast, Fungi, m));
		
		assertFalse(violation(cell_component, Bacteria, m));
		assertFalse(violation(cell_component, Archaea, m));
		assertFalse(violation(cell_component, Eukaryota, m));
		assertFalse(violation(cell_component, Metazoa, m));
		assertFalse(violation(cell_component, Fungi, m));
		
		assertFalse(violation(cytoplasm, Bacteria, m));
		assertFalse(violation(cytoplasm, Archaea, m));
		assertFalse(violation(cytoplasm, Eukaryota, m));
		assertFalse(violation(cytoplasm, Metazoa, m));
		assertFalse(violation(cytoplasm, Fungi, m));
	}

	private boolean violation(OWLClass cls, OWLClass taxon, ClassTaxonMatrix m) {
		Boolean value = m.get(cls, taxon);
		assertNotNull(value);
		return value.booleanValue() == false;
	}
}
