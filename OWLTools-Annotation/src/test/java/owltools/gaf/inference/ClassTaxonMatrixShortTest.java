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

public class ClassTaxonMatrixShortTest extends OWLToolsTestBasics {

	private static OWLGraphWrapper all;

	private static OWLClass animal;
	private static OWLClass vertebrate;
	private static OWLClass mammal;
	private static OWLClass mouse;
	private static OWLClass human;
	private static OWLClass bird;
	private static OWLClass chicken;
	
	private static OWLClass hair;
	private static OWLClass hair_root;
	private static OWLClass whisker;
	private static OWLClass tip_of_whisker;
	
	private static Set<OWLClass> relevant;
	private static Set<OWLClass> taxa;


	@BeforeClass
	public static void beforeClass() throws Exception {
		ParserWrapper pw = new ParserWrapper();
		IRI iri = IRI.create(new File("src/test/resources/simple_taxon_check.owl").getCanonicalFile());
		all = new OWLGraphWrapper(pw.parseOWL(iri));
		
		animal = all.getOWLClass(IRI.create("http://foo.bar/animal"));
		vertebrate = all.getOWLClass(IRI.create("http://foo.bar/vertebrate"));
		mammal = all.getOWLClass(IRI.create("http://foo.bar/mammal"));
		mouse = all.getOWLClass(IRI.create("http://foo.bar/mouse"));
		human = all.getOWLClass(IRI.create("http://foo.bar/human"));
		bird = all.getOWLClass(IRI.create("http://foo.bar/bird"));
		chicken = all.getOWLClass(IRI.create("http://foo.bar/chicken"));
		
		hair = all.getOWLClass(IRI.create("http://foo.bar/hair"));
		hair_root = all.getOWLClass(IRI.create("http://foo.bar/hair_root"));
		whisker = all.getOWLClass(IRI.create("http://foo.bar/whisker"));
		tip_of_whisker = all.getOWLClass(IRI.create("http://foo.bar/tip_of_whisker"));
		
		relevant = new HashSet<OWLClass>(Arrays.asList(hair, hair_root, whisker, tip_of_whisker));
		taxa = new HashSet<OWLClass>(Arrays.asList(animal, vertebrate, mammal, mouse, human, bird, chicken));
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
		
		assertTrue(violation(whisker, human, m));
		assertFalse(violation(whisker, mammal, m));
		assertFalse(violation(whisker, vertebrate, m));
		assertTrue(violation(whisker, chicken, m));
		
		assertTrue(violation(tip_of_whisker, human, m));
		assertFalse(violation(tip_of_whisker, mammal, m));
		assertFalse(violation(tip_of_whisker, mouse, m));
		
		assertTrue(violation(hair, chicken, m));
		assertTrue(violation(hair_root, chicken, m));
		assertTrue(violation(hair, bird, m));
		assertTrue(violation(hair_root, bird, m));
		
		assertFalse(violation(hair, vertebrate, m));
		assertFalse(violation(hair_root, vertebrate, m));
	}

	private boolean violation(OWLClass cls, OWLClass taxon, ClassTaxonMatrix m) {
		Boolean value = m.get(cls, taxon);
		assertNotNull(value);
		return value.booleanValue() == false;
	}
}
