package owltools.graph;

import static junit.framework.Assert.*;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import org.junit.Test;
import org.obolibrary.obo2owl.Obo2Owl;
import org.obolibrary.oboformat.model.OBODoc;
import org.obolibrary.oboformat.parser.OBOFormatParser;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import owltools.OWLToolsTestBasics;
import owltools.graph.OWLGraphWrapper.ISynonym;
import owltools.graph.shunt.OWLShuntGraph;

public class OWLGraphShuntTest extends OWLToolsTestBasics {
	
	/*
	 * Testing the getGraphSegment and getGraphSegementJSON functions.
	 */
	@Test
	public void testShuntGraph() throws Exception{
		OWLGraphWrapper wrapper = getOntologyWrapper("go.owl");
		
		OWLObject c = wrapper.getOWLClass(OWLGraphWrapper.DEFAULT_IRI_PREFIX + "GO_0022008");

		// Exists.
		OWLShuntGraph g = wrapper.getSegmentShuntGraph(c);
		assertNotNull("have a shunt graph, not a complete failure apparently", g);

		// Counts.
		//System.err.println("JSON: " + g.toJSON());
		assertEquals("node count okay", g.nodes.size(), 5);
		assertEquals("edge count okay", g.edges.size(), 4);
}

//	private OWLGraphWrapper getOBO2OWLOntologyWrapper(String file) throws OWLOntologyCreationException, FileNotFoundException, IOException{
//		OBOFormatParser p = new OBOFormatParser();
//		OBODoc obodoc = p.parse(new BufferedReader(new FileReader(getResource(file))));
//		Obo2Owl bridge = new Obo2Owl();
//		OWLOntology ontology = bridge.convert(obodoc);
//		OWLGraphWrapper wrapper = new OWLGraphWrapper(ontology);
//		return wrapper;
//	}
	
	private OWLGraphWrapper getOntologyWrapper(String file) throws OWLOntologyCreationException{
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology ontology = manager.loadOntologyFromOntologyDocument(getResource(file));
		return new OWLGraphWrapper(ontology);
	}
}
