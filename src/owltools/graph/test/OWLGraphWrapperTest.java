package owltools.graph.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import junit.framework.TestCase;

import org.obolibrary.obo2owl.Obo2Owl;
import org.obolibrary.oboformat.model.OBODoc;
import org.obolibrary.oboformat.parser.OBOFormatParser;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import owltools.graph.OWLGraphWrapper;
import owltools.graph.OWLGraphWrapper.Synonym;

public class OWLGraphWrapperTest extends TestCase {

	public static void testSynonyms() throws Exception{
		OWLGraphWrapper  wrapper =  getOntologyWrapper();
		
		OWLObject cls = wrapper.getOWLClass(OWLGraphWrapper.DEFAULT_IRI_PREFIX + "CHEBI_15355");
		
		String s[] = wrapper.getSynonymStrings(cls);
		assertTrue(s.length>0);
	}
	
	public static void testDef() throws Exception{
		OWLGraphWrapper  wrapper =  getOntologyWrapper();
		
		OWLObject cls = wrapper.getOWLClass(OWLGraphWrapper.DEFAULT_IRI_PREFIX + "CHEBI_15355");
		
		String s = wrapper.getDef(cls);
		assertTrue(s != null);
	}

	public static void testSubClassesNames() throws Exception{
		OWLGraphWrapper  wrapper =  getOntologyWrapper();
		
		OWLClass cls =(OWLClass) wrapper.getOWLClass(OWLGraphWrapper.DEFAULT_IRI_PREFIX + "CHEBI_33429");
		
		String s[] = wrapper.getSubClassesNames(cls);
		assertTrue(s.length>0);
	}

	
	private static OWLGraphWrapper getOntologyWrapper() throws OWLOntologyCreationException{
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		return new OWLGraphWrapper( 
				manager.loadOntologyFromOntologyDocument(
						new File("test_resources/test.owl")));
	}

	public static void testGetOBOSynonymsWithXrefs() throws Exception{
		OWLGraphWrapper  wrapper =  getOBO2OWLOntologyWrapper("test_resources/caro.obo");
		
		OWLObject cls = wrapper.getOWLClass(OWLGraphWrapper.DEFAULT_IRI_PREFIX + "CARO_0001001");
		
		List<Synonym> synonyms = wrapper.getOBOSynonyms(cls);
		assertEquals(1, synonyms.size());
		Synonym synonym = synonyms.get(0);
		assertEquals("nerve fiber bundle", synonym.getLabel());
		assertEquals("EXACT", synonym.getScope());
		assertNull(synonym.getCategory());
		assertNotNull(synonym.getXrefs());
		assertEquals(1, synonym.getXrefs().size());
		assertEquals("FBC:DOS", synonym.getXrefs().iterator().next());
	}
	
	public static void testGetOBOSynonymsMultipleScopes() throws Exception{
		OWLGraphWrapper  wrapper =  getOBO2OWLOntologyWrapper("test_resources/ncbi_taxon_slim.obo");
		
		OWLObject cls = wrapper.getOWLClass(OWLGraphWrapper.DEFAULT_IRI_PREFIX + "NCBITaxon_10088");
		
		List<Synonym> synonyms = wrapper.getOBOSynonyms(cls);
		assertEquals(2, synonyms.size());
		Synonym synonym1 = synonyms.get(0);
		Synonym synonym2 = synonyms.get(1);
		// TODO what is the right order of synonyms?
		if (synonym1.getLabel().equals("mice")) {
			Synonym temp = synonym1;
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
	
	private static OWLGraphWrapper getOBO2OWLOntologyWrapper(String file) throws OWLOntologyCreationException, FileNotFoundException, IOException{
		OBOFormatParser p = new OBOFormatParser();
		OBODoc obodoc = p.parse(new BufferedReader(new FileReader(new File(file))));
		Obo2Owl bridge = new Obo2Owl();
		OWLOntology ontology = bridge.convert(obodoc);
		OWLGraphWrapper wrapper = new OWLGraphWrapper(ontology);
		return wrapper;
	}
}
