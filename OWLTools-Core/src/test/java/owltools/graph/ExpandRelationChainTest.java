package owltools.graph;

import static org.junit.Assert.*;

import java.io.File;
import java.util.List;

import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import owltools.OWLToolsTestBasics;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

public class ExpandRelationChainTest extends OWLToolsTestBasics {

	@Test
	public void testExpandRelation() throws Exception {
		
		ParserWrapper p = new ParserWrapper();
		
		OWLGraphWrapper graph = p.parseToOWLGraph(getResourceIRIString("graph/expand_relation_chain.obo"));
		
		OWLOntology ontology = graph.getSourceOntology();
		OWLOntologyManager manager = ontology.getOWLOntologyManager();
		manager.saveOntology(ontology, IRI.create(new File("exapnd_rel.owl")));
		
		List<OWLObjectProperty> relations = graph.expandRelationChain("regulates_o_has_participant");
		
		assertEquals(2, relations.size());
		
	}

}
