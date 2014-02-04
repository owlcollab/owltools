package owltools.graph;

import static junit.framework.Assert.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.obolibrary.obo2owl.Obo2Owl;
import org.obolibrary.oboformat.model.OBODoc;
import org.obolibrary.oboformat.parser.OBOFormatConstants.OboFormatTag;
import org.obolibrary.oboformat.parser.OBOFormatParser;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;

import owltools.OWLToolsTestBasics;
import owltools.graph.OWLGraphWrapper.ISynonym;

public class OWLGraphWrapperTest extends OWLToolsTestBasics {

	@Test
	public void testGetOBOSynonymsWithXrefs() throws Exception{
		OWLGraphWrapper  wrapper = getOBO2OWLOntologyWrapper("caro.obo");
		
		OWLObject cls = wrapper.getOWLClass(OWLGraphWrapper.DEFAULT_IRI_PREFIX + "CARO_0001001");
		
		List<ISynonym> synonyms = wrapper.getOBOSynonyms(cls);
		assertEquals(1, synonyms.size());
		ISynonym synonym = synonyms.get(0);
		assertEquals("nerve fiber bundle", synonym.getLabel());
		assertEquals("EXACT", synonym.getScope());
		assertNull(synonym.getCategory());
		assertNotNull(synonym.getXrefs());
		assertEquals(1, synonym.getXrefs().size());
		assertEquals("FBC:DOS", synonym.getXrefs().iterator().next());
	}
	
	@Test
	public void testGetOBOSynonymsMultipleScopes() throws Exception{
		OWLGraphWrapper wrapper = getOBO2OWLOntologyWrapper("ncbi_taxon_slim.obo");
		
		OWLObject cls = wrapper.getOWLClass(OWLGraphWrapper.DEFAULT_IRI_PREFIX + "NCBITaxon_10088");
		
		List<ISynonym> synonyms = wrapper.getOBOSynonyms(cls);
		assertEquals(2, synonyms.size());
		ISynonym synonym1 = synonyms.get(0);
		ISynonym synonym2 = synonyms.get(1);
		// TODO what is the right order of synonyms?
		if (synonym1.getLabel().equals("mice")) {
			ISynonym temp = synonym1;
			synonym1 = synonym2;
			synonym2 = temp;
		}
		assertEquals("Nannomys", synonym1.getLabel());
		assertEquals("RELATED", synonym1.getScope());
		assertNull(synonym1.getCategory());
		assertNull(synonym1.getXrefs());
		
		assertEquals("mice", synonym2.getLabel());
		assertEquals("EXACT", synonym2.getScope());
		assertNull(synonym2.getCategory());
		assertNull(synonym2.getXrefs());
	}
	
	@Test
	public void testGetOBOSynonymsCategory() throws Exception {
		OWLGraphWrapper wrapper = getOBO2OWLOntologyWrapper("synonym_category_test.obo");
		
		OWLClass cls1 = wrapper.getOWLClassByIdentifier("TEST:0001");
		List<ISynonym> synonyms1 = wrapper.getOBOSynonyms(cls1);
		assertEquals(2, synonyms1.size());
		
		ISynonym syn11 = synonyms1.get(0);
		assertEquals("Japanese Synonym", syn11.getLabel());
		assertEquals("Japanese", syn11.getCategory());
		assertEquals("FOO:bar", syn11.getXrefs().iterator().next());
		assertEquals(OboFormatTag.TAG_EXACT.getTag(), syn11.getScope());
		
		ISynonym syn12 = synonyms1.get(1);
		assertEquals("simple 1", syn12.getLabel());
		assertNull(syn12.getCategory());
		assertEquals("FOO:bar", syn12.getXrefs().iterator().next());
		assertEquals(OboFormatTag.TAG_EXACT.getTag(), syn12.getScope());
		
		OWLClass cls2 = wrapper.getOWLClassByIdentifier("TEST:0002");
		List<ISynonym> synonyms2 = wrapper.getOBOSynonyms(cls2);
		assertEquals(2, synonyms2.size());
		
		ISynonym syn21 = synonyms2.get(0);
		assertEquals("Spanish Synonym", syn21.getLabel());
		assertEquals("Spanish", syn21.getCategory());
		assertNull(syn21.getXrefs());
		assertEquals(OboFormatTag.TAG_EXACT.getTag(), syn21.getScope());
		
		ISynonym syn22 = synonyms2.get(1);
		assertEquals("simple 2", syn22.getLabel());
		assertNull(syn22.getCategory());
		assertNull(syn22.getXrefs());
		assertEquals(OboFormatTag.TAG_EXACT.getTag(), syn22.getScope());
	}
	
	@Test
	public void testAltIds() throws Exception {
		OWLGraphWrapper graph = getOBO2OWLOntologyWrapper("omma.obo");
		
		// test single retrieval
		assertNotNull(graph.getOWLObjectByAltId("FBbt:00000113"));
		
		// test multiple retrieval
		Set<String> altIds = new HashSet<String>(Arrays.asList("FBbt:00004474","FBbt:00002653","FBbt:00005253","FBbt:00005321"));
		Map<String, OWLObject> map = graph.getOWLObjectsByAltId(altIds);
		assertEquals("FBbt:00005396", graph.getIdentifier(map.get("FBbt:00004474")));
		assertEquals("FBbt:00007234", graph.getIdentifier(map.get("FBbt:00002653")));
		assertEquals("FBbt:00025990", graph.getIdentifier(map.get("FBbt:00005253")));
		assertNull(map.get("FBbt:00005321"));
		assertEquals(3, map.size());
		
		// test retrieve all
		Map<String, OWLObject> all = graph.getAllOWLObjectsByAltId();
		assertEquals(4, all.size());
		
		assertEquals("ventral neurogenic region", graph.getLabel(graph.getOWLClassByIdentifier("FBbt:00000113", true)));
		assertEquals(null, graph.getOWLClassByIdentifier("FBbt:00000113", false));
		assertEquals(null, graph.getOWLClassByIdentifier("xyz", false));
	}
	
	@Test
	public void testSubset() throws Exception {
		OWLGraphWrapper graph = getOBO2OWLOntologyWrapper("omma.obo");
		Set<String> subsets = graph.getAllUsedSubsets();
		assertEquals(5, subsets.size());
		boolean ok = true;
		boolean isFound = false;
		for (String s : subsets) {
			Set<OWLObject> objs = graph.getOWLObjectsInSubset(s);
			System.out.println("# "+s+" = "+objs.size());
			if (s.equals("cur")) {
				isFound = true;
				assertEquals(90, objs.size());
			}
			for (OWLObject obj : objs) {
				List<String> subsetsToCheck = graph.getSubsets(obj);
				if (!subsetsToCheck.contains(s)) {
					ok = false;
				}
			}
		}
		
		assertTrue(isFound);
		assertTrue(ok);
	}

	
	private OWLGraphWrapper getOBO2OWLOntologyWrapper(String file) throws Exception{
		OBOFormatParser p = new OBOFormatParser();
		OBODoc obodoc = p.parse(new BufferedReader(new FileReader(getResource(file))));
		Obo2Owl bridge = new Obo2Owl();
		OWLOntology ontology = bridge.convert(obodoc);
		OWLGraphWrapper wrapper = new OWLGraphWrapper(ontology);
		return wrapper;
	}
}
