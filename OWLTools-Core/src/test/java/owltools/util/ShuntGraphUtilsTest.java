package owltools.util;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLPropertyExpression;

import owltools.OWLToolsTestBasics;
import owltools.graph.OWLGraphWrapper;
import owltools.graph.shunt.OWLShuntEdge;
import owltools.graph.shunt.OWLShuntGraph;
import owltools.graph.shunt.OWLShuntNode;
import owltools.vocab.OBOUpperVocabulary;

public class ShuntGraphUtilsTest extends OWLToolsTestBasics {

	@Test
	public void test() throws Exception {
		final OWLGraphWrapper g = getGraph("go.owl");
		
		OWLObject focusObject = g.getOWLClassByIdentifier("GO:0000124"); // SAGA complex
		Set<OWLPropertyExpression> props = new HashSet<OWLPropertyExpression>();
		props.add(g.getOWLObjectProperty(OBOUpperVocabulary.BFO_part_of.getIRI()));
		
		OWLShuntGraph shunt = ShuntGraphUtils.createShuntCraph(g, focusObject, props, true);
		
		containsNode("GO:0000124", shunt, g); // GO:0000124 SAGA complex
		containsNode("GO:0070461", shunt, g); // GO:0070461 SAGA-type complex
		containsNode("GO:0000123", shunt, g); // GO:0000123 histone acetyltransferase complex
		containsNode("GO:0044451", shunt, g); // GO:0044451 nucleoplasm part
		
		containsNode("GO:0005654", shunt, g); // GO:0005654 nucleoplasm
		containsNode("GO:0031981", shunt, g); // GO:0031981 nuclear lumen
		containsNode("GO:0016585", shunt, g); // GO:0016585 chromatin remodeling complex
		containsNode("GO:0044428", shunt, g); // GO:0044428 nuclear part
		
		containsNode("GO:0005634", shunt, g); // GO:0005634 nucleus
		containsNode("GO:0070013", shunt, g); // GO:0070013 intracellular organelle lumen
		containsNode("GO:0044446", shunt, g); // GO:0044446 intracellular organelle part
		containsNode("GO:0043231", shunt, g); // GO:0043231 intracellular membrane-bounded organelle
		
		containsNode("GO:0043229", shunt, g); // GO:0043229 intracellular organelle
		containsNode("GO:0044424", shunt, g); // GO:0044424 intracellular part
		containsNode("GO:0005622", shunt, g); // GO:0005622 intracellular
		containsNode("GO:0043233", shunt, g); // GO:0043233 organelle lumen
		
		containsNode("GO:0043227", shunt, g); // GO:0043227 membrane-bounded organelle
		containsNode("GO:0044422", shunt, g); // GO:0044422 organelle part
		containsNode("GO:0043234", shunt, g); // GO:0043234 protein complex
		containsNode("GO:0044464", shunt, g); // GO:0044464 cell part
		
		containsNode("GO:0031974", shunt, g); // GO:0031974 membrane-enclosed lumen
		containsNode("GO:0043226", shunt, g); // GO:0043226 organelle
		containsNode("GO:0032991", shunt, g); // GO:0032991 macromolecular complex
		containsNode("GO:0005623", shunt, g); // GO:0005623 cell

		containsNode("GO:0005575", shunt, g); // GO:0005575 cellular_component

		containsNotNode("GO:0071819", shunt, g); // GO:0071819 DUBm complex
		
		assertEquals(25, shunt.nodes.size());
		Set<OWLShuntEdge> remaining = new HashSet<OWLShuntEdge>(shunt.edges);
		
		containsRel("GO:0000124", "GO:0070461", "subClassOf", shunt, g, remaining); // SAGA complex -> SAGA-type complex
		containsRel("GO:0000124", "GO:0016585", "subClassOf", shunt, g, remaining); // SAGA complex -> chromatin remodeling complex
		containsRel("GO:0070461", "GO:0000123", "subClassOf", shunt, g, remaining); // SAGA-type complex -> histone acetyltransferase complex
		containsRel("GO:0000123", "GO:0044451", "subClassOf", shunt, g, remaining); // histone acetyltransferase complex -> nucleoplasm part
		containsRel("GO:0000123", "GO:0043234", "subClassOf", shunt, g, remaining); // histone acetyltransferase complex -> protein complex

		containsRel("GO:0044451", "GO:0005654", "http://purl.obolibrary.org/obo/BFO_0000050", shunt, g, remaining); // nucleoplasm part -part_of-> nucleoplasm
		containsRel("GO:0044451", "GO:0044428", "subClassOf", shunt, g, remaining); // nucleoplasm part -> nuclear part
		containsRel("GO:0044451", "GO:0005575", "subClassOf", shunt, g, remaining); // nucleoplasm part -> cellular_component
		containsRel("GO:0005654", "GO:0031981", "http://purl.obolibrary.org/obo/BFO_0000050", shunt, g, remaining); // nucleoplasm -part_of-> nuclear lumen
		containsRel("GO:0005654", "GO:0044428", "subClassOf", shunt, g, remaining); // nucleoplasm -> nuclear part
		
		containsRel("GO:0031981", "GO:0044428", "subClassOf", shunt, g, remaining); // nuclear lumen -> nuclear part
		containsRel("GO:0031981", "GO:0070013", "subClassOf", shunt, g, remaining); // nuclear lumen -> intracellular organelle lumen
		containsRel("GO:0016585", "GO:0044428", "subClassOf", shunt, g, remaining); // chromatin remodeling complex -> nuclear part
		containsRel("GO:0016585", "GO:0043234", "subClassOf", shunt, g, remaining); // chromatin remodeling complex -> protein complex
		containsRel("GO:0044428", "GO:0005634", "http://purl.obolibrary.org/obo/BFO_0000050", shunt, g, remaining); // nuclear part -> nucleus
		
		containsRel("GO:0044428", "GO:0044446", "subClassOf", shunt, g, remaining); // nuclear part -> intracellular organelle part
		containsRel("GO:0044428", "GO:0005575", "subClassOf", shunt, g, remaining); // nuclear part -> cellular_component
		containsRel("GO:0070013", "GO:0044446", "subClassOf", shunt, g, remaining); // intracellular organelle lumen -> intracellular organelle part
		containsRel("GO:0070013", "GO:0043233", "subClassOf", shunt, g, remaining); // intracellular organelle lumen -> organelle lumen
		containsRel("GO:0005634", "GO:0043231", "subClassOf", shunt, g, remaining); // nucleus -> intracellular membrane-bounded organelle
		
		containsRel("GO:0044446", "GO:0044424", "subClassOf", shunt, g, remaining); // intracellular organelle part -> intracellular part
		containsRel("GO:0044446", "GO:0044422", "subClassOf", shunt, g, remaining); // intracellular organelle part -> organelle part
		containsRel("GO:0044446", "GO:0043229", "http://purl.obolibrary.org/obo/BFO_0000050", shunt, g, remaining); // intracellular organelle part -part_of-> intracellular organelle
		containsRel("GO:0043231", "GO:0043229", "subClassOf", shunt, g, remaining); // intracellular membrane-bounded organelle -> intracellular organelle
		containsRel("GO:0043231", "GO:0043227", "subClassOf", shunt, g, remaining); // intracellular membrane-bounded organelle -> membrane-bounded organelle
		
		containsRel("GO:0043229", "GO:0044424", "subClassOf", shunt, g, remaining); // intracellular organelle -> intracellular part
		containsRel("GO:0043229", "GO:0043226", "subClassOf", shunt, g, remaining); // intracellular organelle -> organelle
		containsRel("GO:0044424", "GO:0044464", "subClassOf", shunt, g, remaining); // intracellular part -> cell part
		containsRel("GO:0044424", "GO:0005622", "http://purl.obolibrary.org/obo/BFO_0000050", shunt, g, remaining); // intracellular part -part_of-> intracellular
		containsRel("GO:0005622", "GO:0044464", "subClassOf", shunt, g, remaining); // intracellular -> cell part
		
		containsRel("GO:0043233", "GO:0044422", "subClassOf", shunt, g, remaining); // organelle lumen -> organelle part
		containsRel("GO:0043233", "GO:0031974", "subClassOf", shunt, g, remaining); // organelle lumen -> membrane-enclosed lumen
		containsRel("GO:0044464", "GO:0005575", "subClassOf", shunt, g, remaining); // cell part -> cellular_component
		containsRel("GO:0044464", "GO:0005623", "http://purl.obolibrary.org/obo/BFO_0000050", shunt, g, remaining); // cell part -part_of-> cell
		containsRel("GO:0043234", "GO:0032991", "subClassOf", shunt, g, remaining); // protein complex -> macromolecular complex
		
		containsRel("GO:0044422", "GO:0005575", "subClassOf", shunt, g, remaining); // organelle part -> cellular_component
		containsRel("GO:0044422", "GO:0043226", "http://purl.obolibrary.org/obo/BFO_0000050", shunt, g, remaining); // organelle part -part_of-> organelle
		containsRel("GO:0043227", "GO:0043226", "subClassOf", shunt, g, remaining); // membrane-bounded organelle -> organelle
		containsRel("GO:0005623", "GO:0005575", "subClassOf", shunt, g, remaining); // cell -> cellular_component
		containsRel("GO:0032991", "GO:0005575", "subClassOf", shunt, g, remaining); // macromolecular complex -> cellular_component
		
		containsRel("GO:0043226", "GO:0005575", "subClassOf", shunt, g, remaining); // organelle -> cellular_component
		containsRel("GO:0031974", "GO:0005575", "subClassOf", shunt, g, remaining); // membrane-enclosed lumen -> cellular_component
		
		
		// potential redundant relation, most likely introduced as a genus from an xp
		
		containsRel("GO:0044424", "GO:0005575", "subClassOf", shunt, g, remaining); // intracellular part -> cellular_component
		
		assertEquals(0, remaining.size());
		
	}


	private void containsRel(String s, String t, String r, OWLShuntGraph shuntGraph, OWLGraphWrapper graph, Set<OWLShuntEdge> remaining) {
		boolean found = false;
		OWLObject cs = graph.getOWLObjectByIdentifier(s);
		OWLObject ct = graph.getOWLObjectByIdentifier(t);
		for (OWLShuntEdge edge : shuntGraph.edges) {
			if (s.equals(edge.sub) && t.equals(edge.obj) && r.equals(edge.pred)) {
				found = true;
				remaining.remove(edge);
				break;
			}
		}
		assertTrue("Did not find edge: ("+
				graph.getLabelOrDisplayId(cs)+"; "+
				graph.getLabelOrDisplayId(ct)+"; "+
				r+")", found);
	}
	
	private void containsNode(String id, OWLShuntGraph shuntGraph, OWLGraphWrapper graph) {
		boolean found = false;
		OWLObject c = graph.getOWLObjectByIdentifier(id);
		for (OWLShuntNode node: shuntGraph.nodes) {
			if(id.equals(node.id)) {
				found = true;
				break;
			}
		}
		assertTrue("Did not find node for id: "+graph.getLabelOrDisplayId(c), found);
	}
	
	private void containsNotNode(String id, OWLShuntGraph shuntGraph, OWLGraphWrapper graph) {
		boolean found = false;
		OWLObject c = graph.getOWLObjectByIdentifier(id);
		for (OWLShuntNode node: shuntGraph.nodes) {
			if(id.equals(node.id)) {
				found = true;
				break;
			}
		}
		assertFalse("Did not expect to find node for id: "+graph.getLabelOrDisplayId(c), found);
	}
}
