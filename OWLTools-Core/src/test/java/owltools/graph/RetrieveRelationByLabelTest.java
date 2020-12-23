package owltools.graph;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.FileReader;

import org.junit.Test;
import org.obolibrary.obo2owl.OWLAPIObo2Owl;
import org.obolibrary.oboformat.model.OBODoc;
import org.obolibrary.oboformat.parser.OBOFormatParser;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;

import owltools.OWLToolsTestBasics;


public class RetrieveRelationByLabelTest extends OWLToolsTestBasics {

	@Test
	public void findRelations() throws Exception {
		OWLGraphWrapper wrapper = getOBO2OWLOntologyWrapper("go_xp_predictor_test_subset.obo");
		
		OWLObject negatively_regulates = wrapper.getOWLObjectByLabel("negatively_regulates");
		assertNotNull("All relations need to be retrievable by label.", negatively_regulates);
		
		OWLObject part_of = wrapper.getOWLObjectByLabel("part_of");
		assertNotNull("All relations need to be retrievable by label.", part_of);
		
		OWLObject occurs_in = wrapper.getOWLObjectByLabel("occurs in");
		assertNotNull("All relations need to be retrievable by label.",occurs_in);
	}
	
	@Test
	public void getRelationById() throws Exception {
		OWLGraphWrapper g = getOBO2OWLOntologyWrapper("graph/expand_relation_chain.obo");
		
		OWLObject part_of = g.getOWLObjectByIdentifier("part_of");
		assertNotNull(part_of);
		OWLObject bfo_50 = g.getOWLObjectByIdentifier("BFO:0000050");
		assertNotNull(bfo_50);
		assertEquals(part_of, bfo_50);
		
		assertEquals("BFO:0000050", g.getIdentifier(part_of));
		
		OWLObject foo_bar1 = g.getOWLObjectByIdentifier("foo_bar");
		assertNotNull(foo_bar1);
		OWLObject foo_bar2 = g.getOWLObjectByLabel("foo_bar");
		assertNotNull(foo_bar2);
		assertEquals(foo_bar1, foo_bar2);
		
		assertNull(g.getOWLObjectPropertyByIdentifier("non_existing"));
	}

	private OWLGraphWrapper getOBO2OWLOntologyWrapper(String file) throws Exception{
		OBOFormatParser p = new OBOFormatParser();
		OBODoc obodoc = p.parse(new BufferedReader(new FileReader(getResource(file))));
		OWLAPIObo2Owl bridge = new OWLAPIObo2Owl(setupManager());
		OWLOntology ontology = bridge.convert(obodoc);
		OWLGraphWrapper wrapper = new OWLGraphWrapper(ontology);
		return wrapper;
	}
}
