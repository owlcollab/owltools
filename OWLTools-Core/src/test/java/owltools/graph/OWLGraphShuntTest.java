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
import owltools.graph.shunt.OWLShuntEdge;
import owltools.graph.shunt.OWLShuntGraph;
import owltools.graph.shunt.OWLShuntNode;

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

		// Gross counts.
		//System.err.println("JSON: " + g.toJSON());
		assertEquals("node count okay", g.nodes.size(), 13); // remeber: two kids
		assertEquals("edge count okay", g.edges.size(), 16); // remember: two kids
		
		// Now let's go through and exhaust this subgraph...nodes.
		assertEquals("n01", g.hasNode(new OWLShuntNode("GO:0008150")), true);
		assertEquals("n02", g.hasNode(new OWLShuntNode("GO:0032502")), true);
		assertEquals("n03", g.hasNode(new OWLShuntNode("GO:0032501")), true);
		assertEquals("n04", g.hasNode(new OWLShuntNode("GO:0048856")), true);
		assertEquals("n05", g.hasNode(new OWLShuntNode("GO:0009987")), true);
		assertEquals("n06", g.hasNode(new OWLShuntNode("GO:0007275")), true);
		assertEquals("n07", g.hasNode(new OWLShuntNode("GO:0048869")), true);
		assertEquals("n08", g.hasNode(new OWLShuntNode("GO:0048731")), true);
		assertEquals("n09", g.hasNode(new OWLShuntNode("GO:0030154")), true);
		assertEquals("n10", g.hasNode(new OWLShuntNode("GO:0007399")), true);
		assertEquals("n11", g.hasNode(new OWLShuntNode("GO:0022008")), true);
		assertEquals("n12", g.hasNode(new OWLShuntNode("GO:0048699")), true);
		assertEquals("n13", g.hasNode(new OWLShuntNode("GO:0042063")), true);

		// Now let's go through and exhaust this subgraph...edges.
		assertEquals("e-02", g.hasEdge(new OWLShuntEdge("GO:0048699", "GO:0022008", "is_a")), true);
		assertEquals("e-01", g.hasEdge(new OWLShuntEdge("GO:0042063", "GO:0022008", "is_a")), true);
		assertEquals("e01", g.hasEdge(new OWLShuntEdge("GO:0022008", "GO:0007399", "part_of")), true);
		assertEquals("e02", g.hasEdge(new OWLShuntEdge("GO:0022008", "GO:0030154", "is_a")), true);
		assertEquals("e03", g.hasEdge(new OWLShuntEdge("GO:0007399", "GO:0048731", "is_a")), true);
		assertEquals("e04", g.hasEdge(new OWLShuntEdge("GO:0030154", "GO:0048869", "is_a")), true);
		assertEquals("e05", g.hasEdge(new OWLShuntEdge("GO:0048731", "GO:0007275", "part_of")), true);
		assertEquals("e06", g.hasEdge(new OWLShuntEdge("GO:0048731", "GO:0048856", "is_a")), true);
		assertEquals("e07", g.hasEdge(new OWLShuntEdge("GO:0007275", "GO:0032501", "is_a")), true);
		assertEquals("e08", g.hasEdge(new OWLShuntEdge("GO:0007275", "GO:0032502", "is_a")), true);
		assertEquals("e09", g.hasEdge(new OWLShuntEdge("GO:0048856", "GO:0032502", "is_a")), true);
		assertEquals("e10", g.hasEdge(new OWLShuntEdge("GO:0048869", "GO:0032502", "is_a")), true);
		assertEquals("e11", g.hasEdge(new OWLShuntEdge("GO:0048869", "GO:0009987", "is_a")), true);
		assertEquals("e12", g.hasEdge(new OWLShuntEdge("GO:0032501", "GO:0008150", "is_a")), true);
		assertEquals("e13", g.hasEdge(new OWLShuntEdge("GO:0032502", "GO:0008150", "is_a")), true);
		assertEquals("e14", g.hasEdge(new OWLShuntEdge("GO:0009987", "GO:0008150", "is_a")), true);		
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
