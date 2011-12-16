package owltools.graph;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.junit.Test;
import org.obolibrary.obo2owl.Obo2Owl;
import org.obolibrary.oboformat.model.OBODoc;
import org.obolibrary.oboformat.parser.OBOFormatParser;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import owltools.OWLToolsTestBasics;
import owltools.graph.OWLGraphWrapper;


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

	private OWLGraphWrapper getOBO2OWLOntologyWrapper(String file) throws OWLOntologyCreationException, FileNotFoundException, IOException{
		OBOFormatParser p = new OBOFormatParser();
		OBODoc obodoc = p.parse(new BufferedReader(new FileReader(getResource(file))));
		Obo2Owl bridge = new Obo2Owl();
		OWLOntology ontology = bridge.convert(obodoc);
		OWLGraphWrapper wrapper = new OWLGraphWrapper(ontology);
		return wrapper;
	}
}
