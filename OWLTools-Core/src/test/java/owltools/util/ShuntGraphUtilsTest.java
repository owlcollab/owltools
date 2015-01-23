package owltools.util;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLPropertyExpression;

import owltools.OWLToolsTestBasics;
import owltools.graph.OWLGraphWrapper;
import owltools.graph.shunt.OWLShuntEdge;
import owltools.graph.shunt.OWLShuntGraph;
import owltools.graph.shunt.OWLShuntNode;
import owltools.util.ShuntGraphUtils.ShuntGraphPair;
import owltools.vocab.OBOUpperVocabulary;

@SuppressWarnings("rawtypes")
public class ShuntGraphUtilsTest extends OWLToolsTestBasics {

	private static OWLGraphWrapper g = null;
	
	@BeforeClass
	public static void beforeClass() throws Exception {
		g = getGraph("go-saga-module.obo");
	}

	@Test
	public void testSagaComplex() throws Exception {
		OWLObject focusObject = g.getOWLClassByIdentifier("GO:0000124"); // SAGA complex
		Set<OWLPropertyExpression> props = new HashSet<OWLPropertyExpression>();
		props.add(g.getOWLObjectProperty(OBOUpperVocabulary.BFO_part_of.getIRI()));
		
		ShuntGraphPair pair = ShuntGraphUtils.createShuntGraphPair(g, focusObject, props, true);
		
		OWLShuntGraph topology = pair.getTopologyGraph();
		
		checkNodesSagaComplex(g, topology);
		checkTopologyRelationsSagaComplex(g, topology);
		
		OWLShuntGraph inferred = pair.getInferredGraph();
		
		checkNodesSagaComplex(g, inferred);
		checkInferredRelationsSagaComplex(g, inferred);
	}
	
	private void checkNodesSagaComplex(final OWLGraphWrapper g, OWLShuntGraph shunt) {
		Set<OWLShuntNode> remaining = new HashSet<OWLShuntNode>(shunt.nodes);
		
		containsNode("GO:0000124", shunt, g, remaining); // GO:0000124 SAGA complex
		containsNode("GO:0016585", shunt, g, remaining); // GO:0016585 chromatin remodeling complex
	
		checkNodesUpper(g, shunt, remaining);
		
		containsNotNode("GO:0071819", shunt, g); // GO:0071819 DUBm complex
		
		if (!remaining.isEmpty()) {
			for (OWLShuntNode node : remaining) {
				System.out.println(node.id+" "+node.lbl);
			}
		}
		assertEquals(0, remaining.size());
	}

	private void checkTopologyRelationsSagaComplex(final OWLGraphWrapper g, OWLShuntGraph shunt) {
		Set<OWLShuntEdge> remaining = new HashSet<OWLShuntEdge>(shunt.edges);
		
		containsRel("GO:0000124", "GO:0070461", "rdfs:subClassOf", shunt, g, remaining); // SAGA complex -> SAGA-type complex
		containsRel("GO:0000124", "GO:0016585", "rdfs:subClassOf", shunt, g, remaining); // SAGA complex -> chromatin remodeling complex
		containsRel("GO:0016585", "GO:0044428", "rdfs:subClassOf", shunt, g, remaining); // chromatin remodeling complex -> nuclear part
		containsRel("GO:0016585", "GO:0043234", "rdfs:subClassOf", shunt, g, remaining); // chromatin remodeling complex -> protein complex
		
		checkTopologyRelationsUpper(g, shunt, remaining);
		
		assertEquals(0, remaining.size());
	}

	private void checkInferredRelationsSagaComplex(final OWLGraphWrapper g, OWLShuntGraph shunt) {
		Set<OWLShuntEdge> remaining = new HashSet<OWLShuntEdge>(shunt.edges);
		
		containsRel("GO:0000124", "GO:0070461", "rdfs:subClassOf", shunt, g, remaining); // SAGA complex -> SAGA-type complex
		containsRel("GO:0000124", "GO:0016585", "rdfs:subClassOf", shunt, g, remaining); // SAGA complex -> chromatin remodeling complex
		checkInferredRelationsUpper("GO:0000124", g, shunt, remaining);
		
		if (!remaining.isEmpty()) {
			for (OWLShuntEdge edge : remaining) {
				OWLObject o = g.getOWLObjectByIdentifier(edge.obj);
				System.out.println(edge.sub+"; "+edge.obj+" "+g.getLabel(o)+"; "+edge.pred);
			}
		}
		assertEquals(0, remaining.size());
	}

	@Test
	public void testSagaTypeComplexWithChildren() throws Exception {
		OWLObject focusObject = g.getOWLClassByIdentifier("GO:0070461"); // SAGA-type complex
		Set<OWLPropertyExpression> props = new HashSet<OWLPropertyExpression>();
		props.add(g.getOWLObjectProperty(OBOUpperVocabulary.BFO_part_of.getIRI()));
		
		ShuntGraphPair pair = ShuntGraphUtils.createShuntGraphPair(g, focusObject, props, true);
		
		OWLShuntGraph topology = pair.getTopologyGraph();
		
		checkNodesSagaTypeComplexWithChildren(g, topology);
		checkTopologyRelationsSagaTypeComplexWithChildren(g, topology);
		
		OWLShuntGraph inferred = pair.getInferredGraph();
		
		checkNodesSagaTypeComplexWithChildren(g, inferred);
		checkInferredRelationsSagaTypeComplexWithChildren(g, inferred);
	}


	private void checkNodesSagaTypeComplexWithChildren(OWLGraphWrapper g, OWLShuntGraph shunt) {
		Set<OWLShuntNode> remaining = new HashSet<OWLShuntNode>(shunt.nodes);
		
		containsNode("GO:0000124", shunt, g, remaining); // GO:0000124 SAGA complex
		containsNode("GO:0030914", shunt, g, remaining); // GO:0030914 STAGA complex
		containsNode("GO:0033276", shunt, g, remaining); // GO:0033276 transcription factor TFTC complex
		containsNode("GO:0046695", shunt, g, remaining); // GO:0046695 SLIK (SAGA-like) complex
		containsNode("GO:0000125", shunt, g, remaining); // GO:0000125 PCAF complex
		containsNotNode("GO:0016585", shunt, g); // GO:0016585 chromatin remodeling complex
		
		checkNodesUpper(g, shunt, remaining);
		
		assertEquals(0, remaining.size());
	}

	private void checkTopologyRelationsSagaTypeComplexWithChildren(OWLGraphWrapper g, OWLShuntGraph shunt) {
		Set<OWLShuntEdge> remaining = new HashSet<OWLShuntEdge>(shunt.edges);
		
		containsRel("GO:0000124", "GO:0070461", "rdfs:subClassOf", shunt, g, remaining); // SAGA complex -> SAGA-type complex
		containsRel("GO:0030914", "GO:0070461", "rdfs:subClassOf", shunt, g, remaining); // SAGA complex -> STAGA complex
		containsRel("GO:0033276", "GO:0070461", "rdfs:subClassOf", shunt, g, remaining); // SAGA complex -> transcription factor TFTC complex
		containsRel("GO:0046695", "GO:0070461", "rdfs:subClassOf", shunt, g, remaining); // SAGA complex -> SLIK (SAGA-like) complex
		containsRel("GO:0000125", "GO:0070461", "rdfs:subClassOf", shunt, g, remaining); // SAGA complex -> PCAF complex
		
		checkTopologyRelationsUpper(g, shunt, remaining);
		
		assertEquals(0, remaining.size());
	}

	private void checkInferredRelationsSagaTypeComplexWithChildren(final OWLGraphWrapper g, OWLShuntGraph shunt) {
		Set<OWLShuntEdge> remaining = new HashSet<OWLShuntEdge>(shunt.edges);
		
		containsRel("GO:0000124", "GO:0070461", "rdfs:subClassOf", shunt, g, remaining); // SAGA complex -> SAGA-type complex
		containsRel("GO:0030914", "GO:0070461", "rdfs:subClassOf", shunt, g, remaining); // SAGA complex -> STAGA complex
		containsRel("GO:0033276", "GO:0070461", "rdfs:subClassOf", shunt, g, remaining); // SAGA complex -> transcription factor TFTC complex
		containsRel("GO:0046695", "GO:0070461", "rdfs:subClassOf", shunt, g, remaining); // SAGA complex -> SLIK (SAGA-like) complex
		containsRel("GO:0000125", "GO:0070461", "rdfs:subClassOf", shunt, g, remaining); // SAGA complex -> GO:0000125 PCAF complex
		checkInferredRelationsUpper("GO:0070461", g, shunt, remaining);
		
		assertEquals(0, remaining.size());
	}
	
	
	@Test
	public void testSagaTypeComplexWithoutChildren() throws Exception {
		OWLObject focusObject = g.getOWLClassByIdentifier("GO:0070461"); // SAGA-type complex
		Set<OWLPropertyExpression> props = new HashSet<OWLPropertyExpression>();
		props.add(g.getOWLObjectProperty(OBOUpperVocabulary.BFO_part_of.getIRI()));
		
		ShuntGraphPair pair = ShuntGraphUtils.createShuntGraphPair(g, focusObject, props, false);
		
		OWLShuntGraph topology = pair.getTopologyGraph();
		
		checkNodesSagaTypeComplexWithoutChildren(g, topology);
		checkTopologyRelationsSagaTypeComplexWithoutChildren(g, topology);
		
		OWLShuntGraph inferred = pair.getInferredGraph();
		
		checkNodesSagaTypeComplexWithoutChildren(g, inferred);
		checkInferredRelationsSagaTypeComplexWithoutChildren(g, inferred);
	}
	
	private void checkNodesSagaTypeComplexWithoutChildren(OWLGraphWrapper g, OWLShuntGraph shunt) {
		Set<OWLShuntNode> remaining = new HashSet<OWLShuntNode>(shunt.nodes);
		
		containsNotNode("GO:0000124", shunt, g); // GO:0000124 SAGA complex
		containsNotNode("GO:0030914", shunt, g); // GO:0030914 STAGA complex
		containsNotNode("GO:0033276", shunt, g); // GO:0033276 transcription factor TFTC complex
		containsNotNode("GO:0046695", shunt, g); // GO:0046695 SLIK (SAGA-like) complex
		containsNotNode("GO:0000125", shunt, g); // GO:0000125 PCAF complex
		containsNotNode("GO:0016585", shunt, g); // GO:0016585 chromatin remodeling complex
		
		checkNodesUpper(g, shunt, remaining);
		
		assertEquals(0, remaining.size());
	}

	private void checkTopologyRelationsSagaTypeComplexWithoutChildren(OWLGraphWrapper g, OWLShuntGraph shunt) {
		Set<OWLShuntEdge> remaining = new HashSet<OWLShuntEdge>(shunt.edges);
		
		checkTopologyRelationsUpper(g, shunt, remaining);
		
		assertEquals(0, remaining.size());
	}

	private void checkInferredRelationsSagaTypeComplexWithoutChildren(final OWLGraphWrapper g, OWLShuntGraph shunt) {
		Set<OWLShuntEdge> remaining = new HashSet<OWLShuntEdge>(shunt.edges);
		
		checkInferredRelationsUpper("GO:0070461", g, shunt, remaining);
		
		assertEquals(0, remaining.size());
	}
	
	private void checkNodesUpper(final OWLGraphWrapper g, OWLShuntGraph shunt, Set<OWLShuntNode> remaining) {
		
		containsNode("GO:0070461", shunt, g, remaining); // GO:0070461 SAGA-type complex
		containsNode("GO:0000123", shunt, g, remaining); // GO:0000123 histone acetyltransferase complex
		containsNode("GO:0044451", shunt, g, remaining); // GO:0044451 nucleoplasm part
		
		containsNode("GO:0005654", shunt, g, remaining); // GO:0005654 nucleoplasm
		containsNode("GO:0031981", shunt, g, remaining); // GO:0031981 nuclear lumen
		containsNode("GO:0044428", shunt, g, remaining); // GO:0044428 nuclear part
		
		containsNode("GO:0005634", shunt, g, remaining); // GO:0005634 nucleus
		containsNode("GO:0070013", shunt, g, remaining); // GO:0070013 intracellular organelle lumen
		containsNode("GO:0044446", shunt, g, remaining); // GO:0044446 intracellular organelle part
		containsNode("GO:0043231", shunt, g, remaining); // GO:0043231 intracellular membrane-bounded organelle
		
		containsNode("GO:0043229", shunt, g, remaining); // GO:0043229 intracellular organelle
		containsNode("GO:0044424", shunt, g, remaining); // GO:0044424 intracellular part
		containsNode("GO:0005622", shunt, g, remaining); // GO:0005622 intracellular
		containsNode("GO:0043233", shunt, g, remaining); // GO:0043233 organelle lumen
		
		containsNode("GO:0043227", shunt, g, remaining); // GO:0043227 membrane-bounded organelle
		containsNode("GO:0044422", shunt, g, remaining); // GO:0044422 organelle part
		containsNode("GO:0043234", shunt, g, remaining); // GO:0043234 protein complex
		containsNode("GO:0044464", shunt, g, remaining); // GO:0044464 cell part
		
		containsNode("GO:0031974", shunt, g, remaining); // GO:0031974 membrane-enclosed lumen
		containsNode("GO:0043226", shunt, g, remaining); // GO:0043226 organelle
		containsNode("GO:0032991", shunt, g, remaining); // GO:0032991 macromolecular complex
		containsNode("GO:0005623", shunt, g, remaining); // GO:0005623 cell

		containsNode("GO:0005575", shunt, g, remaining); // GO:0005575 cellular_component
	}


	private void checkTopologyRelationsUpper(final OWLGraphWrapper g, OWLShuntGraph shunt, Set<OWLShuntEdge> remaining) {
		
		containsRel("GO:0070461", "GO:0000123", "rdfs:subClassOf", shunt, g, remaining); // SAGA-type complex -> histone acetyltransferase complex
		containsRel("GO:0000123", "GO:0044451", "rdfs:subClassOf", shunt, g, remaining); // histone acetyltransferase complex -> nucleoplasm part
		containsRel("GO:0000123", "GO:0043234", "rdfs:subClassOf", shunt, g, remaining); // histone acetyltransferase complex -> protein complex

		containsRel("GO:0044451", "GO:0005654", "BFO:0000050", shunt, g, remaining); // nucleoplasm part -part_of-> nucleoplasm
		containsRel("GO:0044451", "GO:0044428", "rdfs:subClassOf", shunt, g, remaining); // nucleoplasm part -> nuclear part
		containsRel("GO:0044451", "GO:0005575", "rdfs:subClassOf", shunt, g, remaining); // nucleoplasm part -> cellular_component
		containsRel("GO:0005654", "GO:0031981", "BFO:0000050", shunt, g, remaining); // nucleoplasm -part_of-> nuclear lumen
		containsRel("GO:0005654", "GO:0044428", "rdfs:subClassOf", shunt, g, remaining); // nucleoplasm -> nuclear part
		
		containsRel("GO:0031981", "GO:0044428", "rdfs:subClassOf", shunt, g, remaining); // nuclear lumen -> nuclear part
		containsRel("GO:0031981", "GO:0070013", "rdfs:subClassOf", shunt, g, remaining); // nuclear lumen -> intracellular organelle lumen
		containsRel("GO:0044428", "GO:0005634", "BFO:0000050", shunt, g, remaining); // nuclear part -> nucleus
		
		containsRel("GO:0044428", "GO:0044446", "rdfs:subClassOf", shunt, g, remaining); // nuclear part -> intracellular organelle part
		containsRel("GO:0044428", "GO:0005575", "rdfs:subClassOf", shunt, g, remaining); // nuclear part -> cellular_component
		containsRel("GO:0070013", "GO:0044446", "rdfs:subClassOf", shunt, g, remaining); // intracellular organelle lumen -> intracellular organelle part
		containsRel("GO:0070013", "GO:0043233", "rdfs:subClassOf", shunt, g, remaining); // intracellular organelle lumen -> organelle lumen
		containsRel("GO:0005634", "GO:0043231", "rdfs:subClassOf", shunt, g, remaining); // nucleus -> intracellular membrane-bounded organelle
		
		containsRel("GO:0044446", "GO:0044424", "rdfs:subClassOf", shunt, g, remaining); // intracellular organelle part -> intracellular part
		containsRel("GO:0044446", "GO:0044422", "rdfs:subClassOf", shunt, g, remaining); // intracellular organelle part -> organelle part
		containsRel("GO:0044446", "GO:0043229", "BFO:0000050", shunt, g, remaining); // intracellular organelle part -part_of-> intracellular organelle
		containsRel("GO:0043231", "GO:0043229", "rdfs:subClassOf", shunt, g, remaining); // intracellular membrane-bounded organelle -> intracellular organelle
		containsRel("GO:0043231", "GO:0043227", "rdfs:subClassOf", shunt, g, remaining); // intracellular membrane-bounded organelle -> membrane-bounded organelle
		
		containsRel("GO:0043229", "GO:0044424", "rdfs:subClassOf", shunt, g, remaining); // intracellular organelle -> intracellular part
		containsRel("GO:0043229", "GO:0043226", "rdfs:subClassOf", shunt, g, remaining); // intracellular organelle -> organelle
		containsRel("GO:0044424", "GO:0044464", "rdfs:subClassOf", shunt, g, remaining); // intracellular part -> cell part
		containsRel("GO:0044424", "GO:0005622", "BFO:0000050", shunt, g, remaining); // intracellular part -part_of-> intracellular
		containsRel("GO:0005622", "GO:0044464", "rdfs:subClassOf", shunt, g, remaining); // intracellular -> cell part
		
		containsRel("GO:0043233", "GO:0044422", "rdfs:subClassOf", shunt, g, remaining); // organelle lumen -> organelle part
		containsRel("GO:0043233", "GO:0031974", "rdfs:subClassOf", shunt, g, remaining); // organelle lumen -> membrane-enclosed lumen
		containsRel("GO:0044464", "GO:0005575", "rdfs:subClassOf", shunt, g, remaining); // cell part -> cellular_component
		containsRel("GO:0044464", "GO:0005623", "BFO:0000050", shunt, g, remaining); // cell part -part_of-> cell
		containsRel("GO:0043234", "GO:0032991", "rdfs:subClassOf", shunt, g, remaining); // protein complex -> macromolecular complex
		
		containsRel("GO:0044422", "GO:0005575", "rdfs:subClassOf", shunt, g, remaining); // organelle part -> cellular_component
		containsRel("GO:0044422", "GO:0043226", "BFO:0000050", shunt, g, remaining); // organelle part -part_of-> organelle
		containsRel("GO:0043227", "GO:0043226", "rdfs:subClassOf", shunt, g, remaining); // membrane-bounded organelle -> organelle
		containsRel("GO:0005623", "GO:0005575", "rdfs:subClassOf", shunt, g, remaining); // cell -> cellular_component
		containsRel("GO:0032991", "GO:0005575", "rdfs:subClassOf", shunt, g, remaining); // macromolecular complex -> cellular_component
		
		containsRel("GO:0043226", "GO:0005575", "rdfs:subClassOf", shunt, g, remaining); // organelle -> cellular_component
		containsRel("GO:0031974", "GO:0005575", "rdfs:subClassOf", shunt, g, remaining); // membrane-enclosed lumen -> cellular_component
		
		// potential redundant relation, most likely introduced as a genus from an xp
		
		containsRel("GO:0044424", "GO:0005575", "rdfs:subClassOf", shunt, g, remaining); // intracellular part -> cellular_component
	}
	
	
	private void checkInferredRelationsUpper(String id, final OWLGraphWrapper g, OWLShuntGraph shunt, Set<OWLShuntEdge> remaining) {
		
		// minimal set of paths
		containsRel(id, "GO:0000123", "rdfs:subClassOf", shunt, g, remaining); // -> histone acetyltransferase complex
		containsRel(id, "GO:0044451", "rdfs:subClassOf", shunt, g, remaining); // -> nucleoplasm part
		containsRel(id, "GO:0005654", "BFO:0000050", shunt, g, remaining); // -part_of-> nucleoplasm
		containsRel(id, "GO:0031981", "BFO:0000050", shunt, g, remaining); // -part_of-> nuclear lumen
		containsRel(id, "GO:0044428", "rdfs:subClassOf", shunt, g, remaining); // -> nuclear part
		containsRel(id, "GO:0005634", "BFO:0000050", shunt, g, remaining); // -part_of-> nucleus
		containsRel(id, "GO:0070013", "BFO:0000050", shunt, g, remaining); // -part_of-> intracellular organelle lumen
		containsRel(id, "GO:0044446", "rdfs:subClassOf", shunt, g, remaining); // -> intracellular organelle part
		containsRel(id, "GO:0043231", "BFO:0000050", shunt, g, remaining); // -part_of-> intracellular membrane-bounded organelle
		containsRel(id, "GO:0043229", "BFO:0000050", shunt, g, remaining); // -part_of-> intracellular organelle
		containsRel(id, "GO:0044424", "rdfs:subClassOf", shunt, g, remaining); // -> intracellular part
		containsRel(id, "GO:0005622", "BFO:0000050", shunt, g, remaining); // -part_of-> intracellular
		containsRel(id, "GO:0043233", "BFO:0000050", shunt, g, remaining); // -part_of-> organelle lumen
		containsRel(id, "GO:0043227", "BFO:0000050", shunt, g, remaining); // -part_of-> membrane-bounded organelle
		containsRel(id, "GO:0044422", "rdfs:subClassOf", shunt, g, remaining); // -> organelle part
		containsRel(id, "GO:0043234", "rdfs:subClassOf", shunt, g, remaining); // -> protein complex
		containsRel(id, "GO:0044464", "rdfs:subClassOf", shunt, g, remaining); // -> cell part
		containsRel(id, "GO:0031974", "BFO:0000050", shunt, g, remaining); // -part_of-> membrane-enclosed lumen
		containsRel(id, "GO:0043226", "BFO:0000050", shunt, g, remaining); // -part_of-> organelle
		containsRel(id, "GO:0032991", "rdfs:subClassOf", shunt, g, remaining); // -> macromolecular complex
		containsRel(id, "GO:0005623", "BFO:0000050", shunt, g, remaining); // SAGA complex -shunt> cell
		containsRel(id, "GO:0005575", "rdfs:subClassOf", shunt, g, remaining); // -> cellular_component

		// additional paths via part_of
		
		containsRel(id, "GO:0044446", "BFO:0000050", shunt, g, remaining); // -part_of-> intracellular organelle part
		containsRel(id, "GO:0044464", "BFO:0000050", shunt, g, remaining); // -part_of-> cell part
		containsRel(id, "GO:0044428", "BFO:0000050", shunt, g, remaining); // -part_of-> nuclear part
		containsRel(id, "GO:0044422", "BFO:0000050", shunt, g, remaining); // -part_of-> organelle part
		containsRel(id, "GO:0005575", "BFO:0000050", shunt, g, remaining); // -part_of-> cellular_component
		containsRel(id, "GO:0044424", "BFO:0000050", shunt, g, remaining); // -part_of-> intracellular part
		
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
	
	private void containsNode(String id, OWLShuntGraph shuntGraph, OWLGraphWrapper graph, Set<OWLShuntNode> remaining) {
		boolean found = false;
		OWLObject c = graph.getOWLObjectByIdentifier(id);
		for (OWLShuntNode node: shuntGraph.nodes) {
			if(id.equals(node.id)) {
				found = true;
				remaining.remove(node);
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
