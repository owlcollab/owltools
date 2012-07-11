package owltools.graph;

import static junit.framework.Assert.*;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.obolibrary.obo2owl.Obo2Owl;
import org.obolibrary.oboformat.model.OBODoc;
import org.obolibrary.oboformat.parser.OBOFormatParser;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

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
	}
	
	private OWLGraphWrapper getOBO2OWLOntologyWrapper(String file) throws OWLOntologyCreationException, FileNotFoundException, IOException{
		OBOFormatParser p = new OBOFormatParser();
		OBODoc obodoc = p.parse(new BufferedReader(new FileReader(getResource(file))));
		Obo2Owl bridge = new Obo2Owl();
		OWLOntology ontology = bridge.convert(obodoc);
		OWLGraphWrapper wrapper = new OWLGraphWrapper(ontology);
		return wrapper;
	}
}
