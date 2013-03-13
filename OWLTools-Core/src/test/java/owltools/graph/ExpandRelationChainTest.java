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

		List<OWLObjectProperty> relations = graph.expandRelationChain("regulates_o_has_participant");
		
		assertEquals(2, relations.size());
	}

}
