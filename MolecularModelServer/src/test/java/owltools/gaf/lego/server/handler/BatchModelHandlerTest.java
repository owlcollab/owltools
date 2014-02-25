package owltools.gaf.lego.server.handler;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import owltools.gaf.lego.MolecularModelJsonRenderer.KEY;
import owltools.gaf.lego.MolecularModelManager;
import owltools.gaf.lego.MolecularModelManager.LegoAnnotationType;
import owltools.gaf.lego.server.handler.M3BatchHandler.Entity;
import owltools.gaf.lego.server.handler.M3BatchHandler.M3Argument;
import owltools.gaf.lego.server.handler.M3BatchHandler.M3BatchResponse;
import owltools.gaf.lego.server.handler.M3BatchHandler.M3Expression;
import owltools.gaf.lego.server.handler.M3BatchHandler.M3Pair;
import owltools.gaf.lego.server.handler.M3BatchHandler.M3Request;
import owltools.gaf.lego.server.handler.M3BatchHandler.Operation;
import owltools.gaf.lego.server.handler.M3Handler.M3Response;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

@SuppressWarnings({"unchecked", "rawtypes"})
public class BatchModelHandlerTest {
	
	@Rule
    public TemporaryFolder folder = new TemporaryFolder();
	
	private static JsonOrJsonpBatchHandler handler = null;
	private static MolecularModelManager models = null;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		ParserWrapper pw = new ParserWrapper();
		OWLGraphWrapper graph = pw.parseToOWLGraph("http://purl.obolibrary.org/obo/go.owl");
		models = new MolecularModelManager(graph);
		handler = new JsonOrJsonpBatchHandler(models);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		if (handler != null) {
			handler = null;
		}
		if (models != null) {
			models.dispose();
		}
	}
	
	@Test
	public void test() throws Exception {
		String uid = "1";
		String intention = "foo";
		
		// create blank model
		M3Request[] batch1 = new M3Request[1];
		batch1[0] = new M3Request();
		batch1[0].entity = Entity.model.name();
		batch1[0].operation = Operation.generateBlank.getLbl();
		M3BatchResponse resp1 = handler.m3Batch(uid, intention, batch1);
		assertEquals(resp1.message, "success", resp1.message_type);
		final String modelId = (String) resp1.data.get("id");
		
		// create two individuals
		M3Request[] batch2 = new M3Request[2];
		batch2[0] = new M3Request();
		batch2[0].entity = Entity.individual.name();
		batch2[0].operation = Operation.create.getLbl();
		batch2[0].arguments = new M3Argument();
		batch2[0].arguments.modelId = modelId;
		batch2[0].arguments.subject = "GO:0006915"; // apoptotic process
		batch2[0].arguments.expressions = new M3Expression[2];
		batch2[0].arguments.expressions[0] = new M3Expression();
		batch2[0].arguments.expressions[0].type = "svf";
		batch2[0].arguments.expressions[0].onProp = "BFO:0000066"; // occurs_in
		batch2[0].arguments.expressions[0].literal = "GO:0005623"; // cell

		batch2[0].arguments.expressions[1] = new M3Expression();
		batch2[0].arguments.expressions[1].type = "svf";
		batch2[0].arguments.expressions[1].onProp = "RO:0002333"; // enabled_by
		batch2[0].arguments.expressions[1].literal = "UniProtKB:P09411";
		
		batch2[0].arguments.values = new M3Pair[2];
		batch2[0].arguments.values[0] = new M3Pair();
		batch2[0].arguments.values[0].key = LegoAnnotationType.comment.name();
		batch2[0].arguments.values[0].value = "comment 1";
		batch2[0].arguments.values[1] = new M3Pair();
		batch2[0].arguments.values[1].key = LegoAnnotationType.comment.name();
		batch2[0].arguments.values[1].value = "comment 2";
		
		batch2[1] = new M3Request();
		batch2[1].entity = Entity.individual.name();
		batch2[1].operation = Operation.create.getLbl();
		batch2[1].arguments = new M3Argument();
		batch2[1].arguments.modelId = modelId;
		batch2[1].arguments.subject = "GO:0043276"; // anoikis
		batch2[1].arguments.expressions = new M3Expression[1];
		batch2[1].arguments.expressions[0] = new M3Expression();
		batch2[1].arguments.expressions[0].type = "svf";
		batch2[1].arguments.expressions[0].onProp = "RO:0002333"; // enabled_by
		batch2[1].arguments.expressions[0].literal = "UniProtKB:P09412";
		
		M3BatchResponse resp2 = handler.m3Batch(uid, intention, batch2);
		assertEquals(resp2.message, "success", resp2.message_type);
		String individual1 = null;
		String individual2 = null;
		List<Map<Object, Object>> iObjs = (List) resp2.data.get("individuals");
		assertEquals(2, iObjs.size());
		for(Map<Object, Object> iObj : iObjs) {
			String id = (String) iObj.get(KEY.id);
			if (id.contains("6915")) {
				individual1 = id;
			}
			else {
				individual2 = id;
			}
		}
		
		// create fact
		M3Request[] batch3 = new M3Request[1];
		batch3[0] = new M3Request();
		batch3[0].entity = Entity.edge.name();
		batch3[0].operation = Operation.add.getLbl();
		batch3[0].arguments = new M3Argument();
		batch3[0].arguments.modelId = modelId;
		batch3[0].arguments.subject = individual1;
		batch3[0].arguments.object = individual2;
		batch3[0].arguments.predicate = "BFO:0000050"; // part_of
		
		M3BatchResponse resp3 = handler.m3Batch(uid, intention, batch3);
		assertEquals(resp3.message, "success", resp3.message_type);
	}
	
	@Test
	public void testMultipleMeta() throws Exception {
		models.setPathToOWLFiles(folder.newFolder().getCanonicalPath());
		models.dispose();
		String uid = "1";
		String intention = "foo";
		M3Request[] requests = new M3Request[3];
		// get relations
		requests[0] = new M3Request();
		requests[0].entity = Entity.relations.name();
		requests[0].operation = Operation.get.getLbl();
		// get evidences
		requests[1] = new M3Request();
		requests[1].entity = Entity.evidence.name();
		requests[1].operation = Operation.get.getLbl();
		// get model ids
		requests[2] = new M3Request();
		requests[2].entity = Entity.model.name();
		requests[2].operation = Operation.allModelIds.getLbl();
		
		M3BatchResponse response = handler.m3Batch(uid, intention, requests);
		assertEquals(uid, response.uid);
		assertEquals(intention, response.intention);
		assertEquals(M3Response.SUCCESS, response.message_type);
		final List<Map<String, Object>> relations = (List)((Map) response.data).get("relations");
		assertTrue(relations.size() > 100);

		final List<Map<String, Object>> evidences = (List)((Map) response.data).get("evidence");
		assertTrue(evidences.size() > 100);
		
		final Set<String> modelIds = (Set)((Map) response.data).get("models_all");
		for (String string : modelIds) {
			System.out.println(string);
		}
		assertEquals(0, modelIds.size());
	}

}
