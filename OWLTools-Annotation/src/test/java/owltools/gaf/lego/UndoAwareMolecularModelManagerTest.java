package owltools.gaf.lego;

import static org.junit.Assert.*;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import owltools.OWLToolsTestBasics;
import owltools.gaf.lego.UndoAwareMolecularModelManager.ChangeEvent;
import owltools.gaf.lego.UndoAwareMolecularModelManager.UndoMetadata;
import owltools.gaf.lego.json.JsonOwlIndividual;
import owltools.gaf.lego.json.MolecularModelJsonRenderer;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;
import owltools.util.ModelContainer;

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
	public void testUndoRedo() throws Exception {
		String userId = "test-user-id";
		String modelId = m3.generateBlankModel(null, null);
		// GO:0001158 ! enhancer sequence-specific DNA binding
		Pair<String, OWLNamedIndividual> bindingId = m3.createIndividual(modelId, "GO:0001158", null, new UndoMetadata(userId));
		// BFO:0000066 GO:0005654 ! occurs_in nucleoplasm
		m3.addType(modelId, bindingId.getKey(), "BFO:0000066", "GO:0005654", new UndoMetadata(userId));
		
		ModelContainer model = m3.getModel(modelId);
		MolecularModelJsonRenderer renderer = new MolecularModelJsonRenderer(model);
		JsonOwlIndividual render1 = renderer.renderObject(bindingId.getRight());
		assertEquals(2, render1.type.length);
		
		// check event count
		Pair<List<ChangeEvent>,List<ChangeEvent>> undoRedoEvents = m3.getUndoRedoEvents(modelId);
		List<ChangeEvent> undoEvents = undoRedoEvents.getLeft();
		List<ChangeEvent> redoEvents = undoRedoEvents.getRight();
		assertEquals(0, redoEvents.size());
		assertEquals(2, undoEvents.size());
		
		// undo
		assertTrue(m3.undo(modelId, userId));
		
		JsonOwlIndividual render2 = renderer.renderObject(bindingId.getRight());
		assertEquals(1, render2.type.length);
		
		// redo
		assertTrue(m3.redo(modelId, userId));
		JsonOwlIndividual render3 = renderer.renderObject(bindingId.getRight());
		assertEquals(2, render3.type.length);
		
		// undo again
		assertTrue(m3.undo(modelId, userId));
		JsonOwlIndividual render4 = renderer.renderObject(bindingId.getRight());
		assertEquals(1, render4.type.length);
		
		// add new type
		// GO:0001664 ! G-protein coupled receptor binding
		m3.addType(modelId, bindingId.getKey(), "GO:0001664", new UndoMetadata(userId));
		
		// redo again, should fail
		assertFalse(m3.redo(modelId, userId));
	}

	static void printToJson(Object obj) {
		String json = MolecularModelJsonRenderer.renderToJson(obj, true);
		System.out.println("---------");
		System.out.println(json);
		System.out.println("---------");
	}
	
}
