package owltools.gaf.lego;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import owltools.OWLToolsTestBasics;
import owltools.gaf.lego.MolecularModelJsonRenderer.KEY;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

public class UndoAwareMolecularModelManagerTest extends OWLToolsTestBasics {

	static OWLGraphWrapper g = null;
	static UndoAwareMolecularModelManager m3 = null;
	
	@BeforeClass
	public static void beforeClass() throws Exception {
		ParserWrapper pw = new ParserWrapper();
		g = pw.parseToOWLGraph(getResourceIRIString("go-mgi-signaling-test.obo"));;
		m3 = new UndoAwareMolecularModelManager(g);
	}
	
	@AfterClass
	public static void afterClass() throws Exception {
		if (m3 != null) {
			m3.dispose();
		}
	}
	
	@Test
	@SuppressWarnings("unchecked")
	public void testUndoRedo() throws Exception {
		String userId = "test-user-id";
		String modelId = m3.generateBlankModel(null, null);
		// GO:0001158 ! enhancer sequence-specific DNA binding
		Pair<String, OWLNamedIndividual> bindingId = m3.createIndividual(modelId, "GO:0001158", null, userId);
		// BFO:0000066 GO:0005654 ! occurs_in nucleoplasm
		m3.addType(modelId, bindingId.getKey(), "BFO:0000066", "GO:0005654", userId);
		
		LegoModelGenerator model = m3.getModel(modelId);
		MolecularModelJsonRenderer renderer = new MolecularModelJsonRenderer(model);
		Map<Object, Object> render1 = renderer.renderObject(bindingId.getRight());
		List<Map<Object,Object>> types1 = (List<Map<Object,Object>>) render1.get(KEY.type);
		assertEquals(2, types1.size());
		
		// undo
		assertTrue(m3.undo(modelId, userId));
		
		Map<Object, Object> render2 = renderer.renderObject(bindingId.getRight());
		List<Map<Object,Object>> types2 = (List<Map<Object,Object>>) render2.get(KEY.type);
		assertEquals(1, types2.size());
		
		// redo
		assertTrue(m3.redo(modelId, userId));
		Map<Object, Object> render3 = renderer.renderObject(bindingId.getRight());
		List<Map<Object,Object>> types3 = (List<Map<Object,Object>>) render3.get(KEY.type);
		assertEquals(2, types3.size());
		
		// undo again
		assertTrue(m3.undo(modelId, userId));
		Map<Object, Object> render4 = renderer.renderObject(bindingId.getRight());
		List<Map<Object,Object>> types4 = (List<Map<Object,Object>>) render4.get(KEY.type);
		assertEquals(1, types4.size());
		
		// add new type
		// GO:0001664 ! G-protein coupled receptor binding
		m3.addType(modelId, bindingId.getKey(), "GO:0001664", userId);
		
		// redo again, should fail
		assertFalse(m3.redo(modelId, userId));
	}

	static void printToJson(Object obj) {
		GsonBuilder builder = new GsonBuilder();
		builder.setPrettyPrinting();
		Gson gson = builder.create();
		String json = gson.toJson(obj);
		System.out.println("---------");
		System.out.println(json);
		System.out.println("---------");
	}
	
}
