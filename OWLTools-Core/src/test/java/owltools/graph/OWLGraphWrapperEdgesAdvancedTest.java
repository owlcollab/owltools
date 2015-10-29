package owltools.graph;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObjectProperty;

import owltools.OWLToolsTestBasics;

public class OWLGraphWrapperEdgesAdvancedTest extends OWLToolsTestBasics {

	@Test
	public void testGetOnlyInTaxon() throws Exception {
		OWLGraphWrapper graph = getGraph("graph/explainConstraints.owl");
		OWLClass c1 = graph.getOWLClass(IRI.create("http://purl.obolibrary.org/obo/UBERON_0000001"));
		Set<String> taxon1 = graph.getOnlyInTaxon(c1, Collections.<String>emptyList());
		assertEquals(1, taxon1.size());
		assertEquals("NCBITaxon:1", taxon1.iterator().next());
		
		OWLClass c2 = graph.getOWLClass(IRI.create("http://purl.obolibrary.org/obo/UBERON_0000002"));
		Set<String> taxon2 = graph.getOnlyInTaxon(c2, Collections.<String>emptyList());
		assertEquals(1, taxon2.size());
		assertEquals("NCBITaxon:2", taxon2.iterator().next());
		
		Set<String> closure = graph.getOnlyInTaxonClosure(c2, Collections.<String>emptyList());
		assertEquals(2, closure.size());
		assertTrue(closure.contains("NCBITaxon:1"));
		assertTrue(closure.contains("NCBITaxon:2"));
		
		OWLClass c3 = graph.getOWLClass(IRI.create("http://purl.obolibrary.org/obo/UBERON_0000003"));
		Set<String> taxon3 = graph.getOnlyInTaxon(c3, Collections.<String>emptyList());
		assertEquals(0, taxon3.size());
	}

	@Test
	public void testGetSvfClasses() throws Exception {
		OWLGraphWrapper graph = getGraph("graph/explainConstraints.owl");
		IRI onlyInTaxonIRI = IRI.create("http://purl.obolibrary.org/obo/RO_0002160");
		OWLObjectProperty onlyInTaxon = graph.getDataFactory().getOWLObjectProperty(onlyInTaxonIRI);
		
		OWLClass c1 = graph.getOWLClass(IRI.create("http://purl.obolibrary.org/obo/UBERON_0000001"));
		OWLClass c2 = graph.getOWLClass(IRI.create("http://purl.obolibrary.org/obo/UBERON_0000002"));
		OWLClass c3 = graph.getOWLClass(IRI.create("http://purl.obolibrary.org/obo/UBERON_0000003"));
		
		Set<OWLClass> svf1 = graph.getSvfClasses(c1, onlyInTaxon);
		Set<OWLClass> svf2 = graph.getSvfClasses(c2, onlyInTaxon);
		Set<OWLClass> svf3 = graph.getSvfClasses(c3, onlyInTaxon);
		
		assertEquals(1, svf1.size());
		assertEquals(1, svf2.size());
		assertEquals(0, svf3.size());
	}
	
	@Test
	public void testClosure() throws Exception {
		OWLGraphWrapper graph = null;
		try {
			graph = getGraph("graph/reg-of-ribosome-biogenesis.owl");
			List<String> relations = Arrays.asList("BFO:0000050",
					"BFO:0000066",
					"RO:0002211",
					"RO:0002212",
					"RO:0002213",
					"RO:0002215",
					"RO:0002216");
			
			final OWLClass c1 = graph.getOWLClassByIdentifier("GO:0090069"); // regulation of ribosome biogenesis
			assertNotNull(c1);
			
			List<String> result = graph.getRelationIDClosure(c1, relations);
			assertTrue(result.contains("GO:0044087"));
			assertTrue(result.contains("GO:0008150"));
			assertTrue(result.contains("GO:0090069"));
			assertTrue(result.contains("GO:0042254"));
			assertTrue(result.contains("GO:0044085"));
			assertTrue(result.contains("GO:0050789"));
			assertTrue(result.contains("GO:0065007"));
			assertTrue(result.contains("GO:0022613"));
			assertTrue(result.contains("GO:0071840"));
		}
		finally {
			IOUtils.closeQuietly(graph);
		}
	}

}
