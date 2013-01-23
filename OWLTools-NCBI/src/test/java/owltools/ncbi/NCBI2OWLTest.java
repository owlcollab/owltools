package owltools.ncbi;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

import org.junit.Test;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLClassAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.AxiomType;

import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;

import owltools.ncbi.NCBIOWL;
import owltools.ncbi.NCBI2OWL;

/**
 * Tests for {@link NCBI2OWL}.
 * 
 */
public class NCBI2OWLTest {
	private int oio = 8; // OboInOwl annotation properties
	private int props = 2; // other annotation properties: IAO_0000115 definition, has_rank
	private int ranks = 29; // specified ranks
	private int types = 16; // specified synonym types
	
	private int baseTaxa = 0;
	private int baseAnnotations =
		  ranks // rdfs:labels for each rank
		+ ranks // oio:hasOBONamespace for each rank
		+ 4 // annotations on has_rank
		+ 1 // rdfs:comment on taxonomic_rank
		+ types // rdfs:labels for each type
		+ types - 1 // oio:hasScope for each type except synonym_type_property
		+ oio // OIO rdfs:labels
		+ 1 // rdfs:label on synonym_type_property
		;

	private int sampleTaxa = 12; // taxa in sample.dat
	private int sampleAnnotations =
		  baseAnnotations
		+ 9 // RANK for most of the taxa
		+ sampleTaxa // SCIENTIFIC NAME for each taxon
		+ sampleTaxa // GD ID for each taxon
		+ sampleTaxa // oio:hasOBONamespace for each taxon
		+ 3 // GENBANK COMMON NAME
		+ 9 // SYNONYM
		+ 4 // IN-PART
		+ 3 // BLAST NAME
		;

	/**
	 *
	 */
	@Test public void testCreate() {

		assertEquals("Count ranks", ranks,
			NCBIOWL.ranks.size() + 1); // plus taxonomy_rank

		assertEquals("Count synonym types", types,
			NCBIOWL.synonymTypes.size() + 1); // plus synonym_type_property

		try {
			OWLOntology ontology = NCBIOWL.createOWLOntology();
			
			// Uncomment these lines to save the file.
			//String outputPath = "create.owl";
			//File outputFile = new File(outputPath);
			//IRI outputIRI = IRI.create(outputFile);
			//ontology.getOWLOntologyManager().saveOntology(ontology,
			//		outputIRI);

			try {
				testOntology(ontology, baseTaxa, baseAnnotations);
			} catch (Exception e) {
				System.out.println("Exception in testCreate testOntology: " + e.toString());
			} finally {
				ontology.getOWLOntologyManager().removeOntology(ontology);
			}
		} catch (Exception e) {
			System.out.println("Exception in testCreate: " + e.toString());
		}
	}

	/**
	 * Convert a sample.dat file and then check the ontology, one of
	 * the classes, one of the ranks, and one of the annotation properties.
	 */
	@Test public void testConvert() {
		String inputPath = "src/test/resources/sample.dat";
		String outputPath = "sample.owl";

		try {
			OWLOntology ontology = NCBI2OWL.convertToOWL(inputPath, null);
			
			// Uncomment these lines to save the file and axioms
			//String outputPath = "create.owl";
			//File outputFile = new File(outputPath);
			//IRI outputIRI = IRI.create(outputFile);
			//ontology.getOWLOntologyManager().saveOntology(ontology,
			//		outputIRI);
			//NCBI2OWL.printAxioms(ontology, "sample.txt");

			try {
				testOntology(ontology, sampleTaxa, sampleAnnotations);
				testBacteria(ontology);
				testActinobacteria(ontology);
				testSpecies(ontology);
				testExactSynonym(ontology);
			} finally {
				ontology.getOWLOntologyManager().removeOntology(ontology);
			}
		} catch (Exception e) {
			System.out.println("Exception in testConvert: " + e.toString());
		}
	}

	private void testOntology(OWLOntology ontology, int taxa,
			int annotations) {
		int declarations = taxa // taxon class declarations
			+ oio + props // OIO and other annotation properties
			+ types + ranks;
		assertEquals("Count declarations", declarations,
			ontology.getAxiomCount(AxiomType.DECLARATION));

		int taxaExceptRoot = 0;
		if (taxa > 0) { taxaExceptRoot = taxa - 1; }

		int subClasses = taxaExceptRoot // taxa except root
			+ ranks - 1; // ranks except taxonomy_rank
		assertEquals("Count subClass assertions", subClasses,
			ontology.getAxiomCount(AxiomType.SUBCLASS_OF));

		int subAnnotationProperties = types - 1; // taxa except synonym_type_property
		assertEquals("Count subClass assertions",
			subAnnotationProperties,
			ontology.getAxiomCount(
				AxiomType.SUB_ANNOTATION_PROPERTY_OF));

		assertEquals("Count annotation assertions", annotations,
			ontology.getAxiomCount(AxiomType.ANNOTATION_ASSERTION));

		int axioms = declarations
			+ subClasses
			+ subAnnotationProperties
			+ annotations;
		assertEquals("Count all axioms", axioms,
			ontology.getAxiomCount());
	}

	private void testBacteria(OWLOntology ontology) {
		String curie = "ncbi:2";
		IRI iri = OWLConverter.format.getIRI(curie);
		OWLDataFactory df = ontology.getOWLOntologyManager().
			getOWLDataFactory();
		OWLClass taxon = df.getOWLClass(iri);
		assertTrue("Bacteria class in signature",
			ontology.containsClassInSignature(iri));

		// Check axioms
		Set<OWLClassAxiom> axioms = ontology.getAxioms(taxon);
		assertEquals("Count class axioms for Bacteria", 1, axioms.size());
		assertEquals("SubClassOf(<http://purl.obolibrary.org/obo/NCBITaxon_2> <http://purl.obolibrary.org/obo/NCBITaxon_131567>)", axioms.toArray()[0].toString());

		// Check annotations
		List<String> values = new ArrayList<String>();
		values.add(expandAnnotation(curie, "ncbitaxon:has_rank", OWLConverter.format.getIRI("ncbi:superkingdom")));
		values.add(expandAnnotation(curie, "oio:hasOBONamespace", "ncbi_taxonomy"));
		values.add(expandAnnotation(curie, "oio:hasDbXref", "GC_ID:11"));
		values.add(expandLabel(curie, "rdfs:label", "Bacteria"));
		values.add(expandSynonym(curie, "ncbitaxon:genbank_common_name", "oio:hasExactSynonym", "eubacteria"));
		values.add(expandSynonym(curie, "ncbitaxon:synonym", "oio:hasRelatedSynonym", "not Bacteria Haeckel 1894"));
		values.add(expandSynonym(curie, "ncbitaxon:in_part", "oio:hasRelatedSynonym", "Prokaryota"));
		values.add(expandSynonym(curie, "ncbitaxon:in_part", "oio:hasRelatedSynonym", "Monera"));
		values.add(expandSynonym(curie, "ncbitaxon:in_part", "oio:hasRelatedSynonym", "Procaryotae"));
		values.add(expandSynonym(curie, "ncbitaxon:in_part", "oio:hasRelatedSynonym", "Prokaryotae"));
		values.add(expandSynonym(curie, "ncbitaxon:blast_name", "oio:hasRelatedSynonym", "eubacteria"));

		Set<OWLAnnotationAssertionAxiom> annotations = 
			ontology.getAnnotationAssertionAxioms(iri);
		assertEquals("Count annotations for Bacteria", values.size(), annotations.size());

		checkAnnotations(annotations, values);
	}

	private void testActinobacteria(OWLOntology ontology) {
		String curie = "ncbi:201174";
		IRI iri = OWLConverter.format.getIRI(curie);
		OWLDataFactory df = ontology.getOWLOntologyManager().
			getOWLDataFactory();
		OWLClass taxon = df.getOWLClass(iri);
		assertTrue("Actinobacteria class in signature",
			ontology.containsClassInSignature(iri));

		// Check axioms
		Set<OWLClassAxiom> axioms = ontology.getAxioms(taxon);
		assertEquals("Count class axioms for Actinobacteria", 1, axioms.size());
		assertEquals("SubClassOf(<http://purl.obolibrary.org/obo/NCBITaxon_201174> <http://purl.obolibrary.org/obo/NCBITaxon_2>)", axioms.toArray()[0].toString());

		// Check annotations
		List<String> values = new ArrayList<String>();
		values.add(expandAnnotation(curie, "ncbitaxon:has_rank", OWLConverter.format.getIRI("ncbi:phylum")));
		values.add(expandAnnotation(curie, "oio:hasOBONamespace", "ncbi_taxonomy"));
		values.add(expandAnnotation(curie, "oio:hasDbXref", "GC_ID:11"));
		values.add(expandLabel(curie, "rdfs:label", "Actinobacteria [NCBITaxon:201174]"));
		values.add(expandSynonym(curie, "ncbitaxon:synonym", "oio:hasRelatedSynonym", "'Actinobacteria'"));
		values.add(expandSynonym(curie, "ncbitaxon:synonym", "oio:hasRelatedSynonym", "not Actinobacteria Cavalier-Smith 2002"));
		values.add(expandSynonym(curie, "ncbitaxon:blast_name", "oio:hasRelatedSynonym", "actinobacteria"));

		Set<OWLAnnotationAssertionAxiom> annotations = 
			ontology.getAnnotationAssertionAxioms(iri);
		assertEquals("Count annotations for Actinobacteria",
				values.size(), annotations.size());

		checkAnnotations(annotations, values);
	}

	private String expandLabel(String subject, String property,
			String value) {
		RDFXMLOntologyFormat format = OWLConverter.format;
		// TODO: Why is "Annotation" always doubled?
		return "AnnotationAssertion(" +
			property + " <" +
			format.getIRI(subject) + "> \"" +
			value +"\"^^xsd:string)";
	}

	private String expandAnnotation(String subject, String property,
			String value) {
		RDFXMLOntologyFormat format = OWLConverter.format;
		// TODO: Why is "Annotation" always doubled?
		return "AnnotationAssertion(<" +
			format.getIRI(property) + "> <" +
			format.getIRI(subject) + "> \"" +
			value +"\"^^xsd:string)";
	}

	private String expandAnnotation(String subject, String property,
			IRI value) {
		RDFXMLOntologyFormat format = OWLConverter.format;
		// TODO: Why is "Annotation" always doubled?
		return "AnnotationAssertion(<" +
			format.getIRI(property) + "> <" +
			format.getIRI(subject) + "> <" +
			value.toString() + ">)";
	}

	private String expandSynonym(String subject, String type, 
			String property, String value) {
		RDFXMLOntologyFormat format = OWLConverter.format;
		// TODO: Why is "Annotation" always doubled?
		return "AnnotationAssertion(Annotation(<" +
			format.getIRI("oio:hasSynonymType").toString() + "> <" +
			format.getIRI(type).toString() + ">) Annotation(<" +
			format.getIRI("oio:hasSynonymType").toString() + "> <" +
			format.getIRI(type).toString() + ">) <" +
			format.getIRI(property) + "> <" +
			format.getIRI(subject) + "> \"" +
			value +"\"^^xsd:string)";
	}

	private void testSpecies(OWLOntology ontology) {
		IRI iri = IRI.create("http://purl.obolibrary.org/obo/NCBITaxon_species");
		OWLDataFactory df = ontology.getOWLOntologyManager().
			getOWLDataFactory();
		OWLClass taxon = df.getOWLClass(iri);
		assertTrue("Species class in signature",
			ontology.containsClassInSignature(iri));
		
		// Check axioms
		Set<OWLClassAxiom> axioms = ontology.getAxioms(taxon);
		assertEquals("Count class axioms", 1, axioms.size());
		assertEquals("SubClassOf(<http://purl.obolibrary.org/obo/NCBITaxon_species> <http://purl.obolibrary.org/obo/NCBITaxon#_taxonomic_rank>)", axioms.toArray()[0].toString());

		// Check annotations
		List<String> values = new ArrayList<String>();
		values.add("AnnotationAssertion(<http://www.geneontology.org/formats/oboInOwl#hasOBONamespace> <http://purl.obolibrary.org/obo/NCBITaxon_species> \"ncbi_taxonomy\"^^xsd:string)");
		values.add("AnnotationAssertion(rdfs:label <http://purl.obolibrary.org/obo/NCBITaxon_species> \"species\"^^xsd:string)");

		Set<OWLAnnotationAssertionAxiom> annotations = 
			ontology.getAnnotationAssertionAxioms(iri);
		assertEquals("Count annotations for Species", 2, annotations.size());

		checkAnnotations(annotations, values);
	}

	private void testExactSynonym(OWLOntology ontology) {
		IRI iri = IRI.create("http://www.geneontology.org/formats/oboInOwl#hasExactSynonym");
		OWLDataFactory df = ontology.getOWLOntologyManager().
			getOWLDataFactory();
		OWLAnnotationProperty property = df.getOWLAnnotationProperty(iri);
		assertTrue("Exact Synonym property in signature",
			ontology.containsAnnotationPropertyInSignature(iri));
		
		// Check axioms
		Set<OWLAnnotationAxiom> axioms = ontology.getAxioms(property);
		assertEquals("Count class axioms", 0, axioms.size());

		// Check annotations
		List<String> values = new ArrayList<String>();
		values.add("AnnotationAssertion(rdfs:label <http://www.geneontology.org/formats/oboInOwl#hasExactSynonym> \"has_exact_synonym\"^^xsd:string)");

		Set<OWLAnnotationAssertionAxiom> annotations = 
			ontology.getAnnotationAssertionAxioms(iri);
		assertEquals("Count annotations for Exact", 1, annotations.size());

		checkAnnotations(annotations, values);
	}

	private void checkAnnotations(Set<OWLAnnotationAssertionAxiom> axioms,
			List<String> values) {
		List<String> results = new ArrayList<String>();
		for (OWLAnnotationAssertionAxiom axiom : axioms) {
			results.add(axiom.toString());
		}
		assertEquals("Compare sizes", values.size(), results.size());
		java.util.Collections.sort(values);
		java.util.Collections.sort(results);
		for (int i=0; i < results.size(); i++) {
			assertEquals("Check Annotation " + i,
				values.get(i), results.get(i));
		}
	}

	/**
	 * Test parsing of various well-formed and malformed lines.
	 */
	@Test public void testParseLine() {
		testGoodLine("ID                        : 2", "id", "2");
		testGoodLine("PARENT ID                 : 131567",
				"parent id", "131567");
		testGoodLine("PARENT ID: 131567", "parent id", "131567");
		testGoodLine("PARENT ID:131567", "parent id", "131567");
		testGoodLine("RANK  : superkingdom", "rank", "superkingdom");
		testGoodLine("GC ID    : 11", "gc id", "11");
		testGoodLine("SCIENTIFIC NAME: Bacteria",
				"scientific name", "Bacteria");
		testGoodLine("GENBANK COMMON NAME       : eubacteria",
				"genbank common name", "eubacteria");
		testGoodLine("SYNONYM   : not Bacteria Haeckel 1894",
				"synonym", "not Bacteria Haeckel 1894");
		testGoodLine("IN-PART: Prokaryota  ", "in-part", "Prokaryota");
		testGoodLine("BLAST NAME  \t\t : eubacteria\t",
				"blast name", "eubacteria");

		// Turn the logger off to test bad lines.
		org.apache.log4j.Level level = NCBI2OWL.logger.getLevel();
		NCBI2OWL.logger.setLevel(org.apache.log4j.Level.OFF);

		testBadLine("");
		testBadLine("ID");
		testBadLine("ID   ; 1");
		testBadLine(": 1");
		testBadLine("   : 1");
		testBadLine("ID:   ");
		testBadLine("ID:");
		
		// Restore the logger level.
		NCBI2OWL.logger.setLevel(level);
	}
	
	private void testGoodLine(String line, String key, String value) {
		String[] result = NCBI2OWL.parseLine(line, 0);
		assertEquals("Match key: " + line, key, result[0]);
		assertEquals("Match value: " + line, value, result[1]);
	}

	private void testBadLine(String line) {
		assertNull("Bad line: " + line, 
                    NCBI2OWL.parseLine(line, 0));
	}

	/**
	 * Test the expected return values for a given set of lines in the dmp format.
	 */
	@Test
	public void testSplitDmpLine() {
		assertSplitDmpLine("", Collections.<String>emptyList()); // empty line return empty list
		assertSplitDmpLine("|", Collections.<String>singletonList(null));
		assertSplitDmpLine("5658    |       Leishmania      |       Leishmania <genus>      |       scientific name |", 
				Arrays.asList("5658", "Leishmania", "Leishmania <genus>", "scientific name"));
		assertSplitDmpLine("5659    |       Leishmania (Leishmania) amazonensis     |               |       synonym |", 
				Arrays.asList("5659", "Leishmania (Leishmania) amazonensis", null, "synonym"));
	}
	
	private void assertSplitDmpLine(String line, List<String> expected) {
		List<String> split = NCBI2OWL.splitDmpLine(line);
		assertArrayEquals(expected.toArray(new String[expected.size()]), 
				split.toArray(new String[split.size()]));
	}
	
	/**
	 * Test the expected return values for splitting a whitespace separated list of ids.
	 */
	@Test
	public void testSplitTaxonIds() {
		assertSplitTaxonIds("", Collections.<String>emptyList());
		assertSplitTaxonIds(" ", Collections.<String>emptyList());
		assertSplitTaxonIds("555", Collections.singletonList("555"));
		assertSplitTaxonIds("555 556", Arrays.asList("555","556"));
		assertSplitTaxonIds("555  556", Arrays.asList("555","556"));
		assertSplitTaxonIds("555  556 557 558   559", Arrays.asList("555","556","557","558","559"));
	}
	
	private void assertSplitTaxonIds(String line, List<String> expected) {
		List<String> split = NCBI2OWL.splitTaxonList(line);
		assertArrayEquals(expected.toArray(new String[expected.size()]), 
				split.toArray(new String[split.size()]));
	}
}
