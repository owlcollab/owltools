package owltools.graph;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;
import org.semanticweb.owlapi.model.OWLObjectProperty;

import owltools.OWLToolsTestBasics;
import owltools.io.ParserWrapper;

public class ExpandRelationChainTest extends OWLToolsTestBasics {

	@Test
	public void testExpandRelation() throws Exception {
		
		ParserWrapper p = new ParserWrapper();
		
		OWLGraphWrapper graph = p.parseToOWLGraph(getResourceIRIString("graph/expand_relation_chain.obo"));

		//OWLOntology ontology = graph.getSourceOntology();
		//OWLOntologyManager manager = ontology.getOWLOntologyManager();
		//manager.saveOntology(ontology, IRI.create(new File("expand_rel.owl")));

		List<OWLObjectProperty> relations = graph.getRelationOrChain("regulates_o_has_participant");
		
		assertEquals(2, relations.size());
		
		// if no chain exists return the property itself
		relations = graph.getRelationOrChain("foo_bar");
		
		assertEquals(1, relations.size());
		
		
		assertNull(graph.getRelationOrChain("foo_bar_2")); // this does not exist, expect null
		
		// check that occurs_in is not expanded
		relations = graph.getRelationOrChain("occurs_in");
		assertEquals(1, relations.size());
		assertEquals("occurs in", graph.getLabel(relations.get(0)));
	}

}
