package owltools.gaf.lego.server.handler;

import static org.junit.Assert.*;
import static owltools.gaf.lego.MolecularModelJsonRenderer.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import owltools.gaf.lego.ManchesterSyntaxTool;
import owltools.gaf.lego.MolecularModelJsonRenderer;
import owltools.gaf.lego.MolecularModelJsonRenderer.KEY;
import owltools.gaf.lego.MolecularModelManager.LegoAnnotationType;
import owltools.gaf.lego.MolecularModelManager.UnknownIdentifierException;
import owltools.gaf.lego.UndoAwareMolecularModelManager;
import owltools.gaf.lego.server.StartUpTool;
import owltools.gaf.lego.server.external.CombinedExternalLookupService;
import owltools.gaf.lego.server.external.ExternalLookupService;
import owltools.gaf.lego.server.external.ExternalLookupService.LookupEntry;
import owltools.gaf.lego.server.external.ProteinToolService;
import owltools.gaf.lego.server.external.TableLookupService;
import owltools.gaf.lego.server.handler.M3BatchHandler.Entity;
import owltools.gaf.lego.server.handler.M3BatchHandler.M3Argument;
import owltools.gaf.lego.server.handler.M3BatchHandler.M3BatchResponse;
import owltools.gaf.lego.server.handler.M3BatchHandler.M3Expression;
import owltools.gaf.lego.server.handler.M3BatchHandler.M3ExpressionType;
import owltools.gaf.lego.server.handler.M3BatchHandler.M3Pair;
import owltools.gaf.lego.server.handler.M3BatchHandler.M3Request;
import owltools.gaf.lego.server.handler.M3BatchHandler.Operation;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;
import owltools.util.ModelContainer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@SuppressWarnings({"unchecked", "rawtypes"})
public class BatchModelHandlerTest {
	
	@Rule
    public TemporaryFolder folder = new TemporaryFolder();
	
	private static JsonOrJsonpBatchHandler handler = null;
	private static UndoAwareMolecularModelManager models = null;
	private static Set<OWLObjectProperty> importantRelations = null;
	
	private static final String uid = "test-user";
	private static final String intention = "test-intention";
	private static final String packetId = "foo-packet-id";

	private static ExternalLookupService lookupService;

	

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		init(new ParserWrapper());
	}

	static void init(ParserWrapper pw) throws OWLOntologyCreationException, IOException {
		final OWLGraphWrapper graph = pw.parseToOWLGraph("http://purl.obolibrary.org/obo/go/extensions/go-lego.owl");
		final OWLObjectProperty legorelParent = StartUpTool.getRelation("http://purl.obolibrary.org/obo/LEGOREL_0000000", graph);
		assertNotNull(legorelParent);
		importantRelations = StartUpTool.getAssertedSubProperties(legorelParent, graph);
		assertFalse(importantRelations.isEmpty());
		
		models = new UndoAwareMolecularModelManager(graph);
		models.setPathToGafs("src/test/resources/gaf");
		ProteinToolService proteinService = new ProteinToolService("src/test/resources/ontology/protein/subset");
		models.addObsoleteImportIRIs(proteinService.getOntologyIRIs());
		lookupService = new CombinedExternalLookupService(proteinService, createTestProteins());
		handler = new JsonOrJsonpBatchHandler(models, importantRelations, lookupService);
		JsonOrJsonpBatchHandler.ADD_INFERENCES = true;
		JsonOrJsonpBatchHandler.USE_CREATION_DATE = true;
		JsonOrJsonpBatchHandler.USE_USER_ID = true;
		JsonOrJsonpBatchHandler.VALIDATE_BEFORE_SAVE = true;
		JsonOrJsonpBatchHandler.ENFORCE_EXTERNAL_VALIDATE = true;
	}
	
	private static ExternalLookupService createTestProteins() {
		List<LookupEntry> testEntries = new ArrayList<LookupEntry>();
		testEntries.add(new LookupEntry("UniProtKB:P0000", "P0000", "protein", "fake-taxon-id"));
		testEntries.add(new LookupEntry("UniProtKB:P0001", "P0001", "protein", "fake-taxon-id"));
		testEntries.add(new LookupEntry("UniProtKB:P0002", "P0002", "protein", "fake-taxon-id"));
		testEntries.add(new LookupEntry("UniProtKB:P0003", "P0003", "protein", "fake-taxon-id"));
		return new TableLookupService(testEntries);
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
		final String modelId = generateBlankModel();
		
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
		batch2[0].arguments.expressions[1].literal = "UniProtKB:P0001"; // fake
		
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
		batch2[1].arguments.expressions[0].type = M3ExpressionType.svf.getLbl();
		batch2[1].arguments.expressions[0].onProp = "RO:0002333"; // enabled_by
		batch2[1].arguments.expressions[0].literal = "GO:0043234 and (('has part' some UniProtKB:P0002) OR ('has part' some UniProtKB:P0003))";
		
		M3BatchResponse resp2 = handler.m3Batch(uid, intention, packetId, batch2, true);
		assertEquals(resp2.message, M3BatchResponse.MESSAGE_TYPE_SUCCESS, resp2.message_type);
		String individual1 = null;
		String individual2 = null;
		List<Map<Object, Object>> iObjs = (List) resp2.data.get(KEY_INDIVIDUALS);
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
		
		M3BatchResponse resp3 = handler.m3Batch(uid, intention, packetId, batch3, true);
		assertEquals(resp3.message, M3BatchResponse.MESSAGE_TYPE_SUCCESS, resp3.message_type);
		
		// delete complex expression type
		M3Request[] batch4 = new M3Request[1];
		batch4[0] = new M3Request();
		batch4[0].entity = Entity.individual.name();
		batch4[0].operation = Operation.removeType.getLbl();
		batch4[0].arguments = new M3Argument();
		batch4[0].arguments.modelId = modelId;
		batch4[0].arguments.individual = individual2;
		batch4[0].arguments.expressions = new M3Expression[1];
		batch4[0].arguments.expressions[0] = new M3Expression();
		batch4[0].arguments.expressions[0].type = M3ExpressionType.svf.getLbl();
		batch4[0].arguments.expressions[0].onProp = "RO:0002333"; // enabled_by
		// "GO:0043234 and (('has part' some UniProtKB:P0002) OR ('has part' some UniProtKB:P0003))";
		batch4[0].arguments.expressions[0].expressions = new M3Expression[2];

		// GO:0043234
		batch4[0].arguments.expressions[0].expressions[0] = new M3Expression();
		batch4[0].arguments.expressions[0].expressions[0].type = M3ExpressionType.clazz.getLbl();
		batch4[0].arguments.expressions[0].expressions[0].literal = "GO:0043234";

		//'has part' some UniProtKB:P0002
		batch4[0].arguments.expressions[0].expressions[1] = new M3Expression();
		batch4[0].arguments.expressions[0].expressions[1].type = M3ExpressionType.union.getLbl();
		batch4[0].arguments.expressions[0].expressions[1].expressions = new M3Expression[2];
		
		batch4[0].arguments.expressions[0].expressions[1].expressions[0] = new M3Expression();
		batch4[0].arguments.expressions[0].expressions[1].expressions[0].type = M3ExpressionType.svf.getLbl();
		batch4[0].arguments.expressions[0].expressions[1].expressions[0].onProp = "BFO:0000051"; // has_part
		batch4[0].arguments.expressions[0].expressions[1].expressions[0].literal = "UniProtKB:P0002";
		
		// 'has part' some UniProtKB:P0003
		batch4[0].arguments.expressions[0].expressions[1].expressions[1] = new M3Expression();
		batch4[0].arguments.expressions[0].expressions[1].expressions[1].type = M3ExpressionType.svf.getLbl();
		batch4[0].arguments.expressions[0].expressions[1].expressions[1].onProp = "BFO:0000051"; // has_part
		batch4[0].arguments.expressions[0].expressions[1].expressions[1].literal = "UniProtKB:P0003";
		
		M3BatchResponse resp4 = handler.m3Batch(uid, intention, packetId, batch4, true);
		assertEquals(resp4.message, M3BatchResponse.MESSAGE_TYPE_SUCCESS, resp4.message_type);
		List<Map<Object, Object>> iObjs4 = (List) resp4.data.get(KEY_INDIVIDUALS);
		assertEquals(1, iObjs4.size());
		List<Map> types = (List<Map>) iObjs4.get(0).get(KEY.type);
		assertEquals(1, types.size());
	}
	
	@Test
	public void testParseComplex() throws Exception {
		String modelId = models.generateBlankModel(null, null);
		ModelContainer model = models.getModel(modelId);
		OWLGraphWrapper graph = new OWLGraphWrapper(model.getAboxOntology());
		ManchesterSyntaxTool tool = new ManchesterSyntaxTool(graph, true);
		
		String expr = "GO:0043234 and ('has part' some UniProtKB:P0002) and ('has part' some UniProtKB:P0003)";
		
		OWLClassExpression clsExpr = tool.parseManchesterExpression(expr);
		assertNotNull(clsExpr);
	}
	
	@Test
	public void testParseComplexOr() throws Exception {
		final String modelId = models.generateBlankModel(null, null);
		
		M3Expression expression = new M3Expression();
		expression.type = M3ExpressionType.svf.getLbl();
		expression.onProp = "RO:0002333"; // enabled_by
		expression.literal = "('has part' some UniProtKB:F1NGQ9) or ('has part' some UniProtKB:F1NH29)";
		
		OWLClassExpression ce = M3ExpressionParser.parse(modelId, expression, models, lookupService);
		assertNotNull(ce);
		System.out.println(ce);
	}
	
	@Test(expected=UnknownIdentifierException.class)
	public void testParseComplexFail() throws Exception {
		final String modelId = models.generateBlankModel(null, null);
		
		M3Expression expression = new M3Expression();
		expression.type = M3ExpressionType.svf.getLbl();
		expression.onProp = "RO:0002333"; // enabled_by
		expression.literal = "('has part' some UniProtKB:F000F1) or ('has part' some UniProtKB:F000F2)";
		
		M3ExpressionParser.parse(modelId, expression, models, lookupService);
	}
	
	@Test
	public void testModelAnnotations() throws Exception {
		assertTrue(JsonOrJsonpBatchHandler.USE_CREATION_DATE);
		assertTrue(JsonOrJsonpBatchHandler.USE_USER_ID);
		
		final String modelId = generateBlankModel();
		
		final Map<Object, Object> data1 = renderModel(modelId);
		List annotations1 = (List) data1.get("annotations");
		assertNotNull(annotations1);
		// creation date
		// user id
		assertEquals(2, annotations1.size());
		
		// create annotations
		M3Request[] batch1 = new M3Request[1];
		batch1[0] = new M3Request();
		batch1[0].entity = Entity.model.name();
		batch1[0].operation = Operation.addAnnotation.getLbl();
		batch1[0].arguments = new M3Argument();
		batch1[0].arguments.modelId = modelId;

		batch1[0].arguments.values = new M3Pair[2];
		batch1[0].arguments.values[0] = new M3Pair();
		batch1[0].arguments.values[0].key = LegoAnnotationType.comment.name();
		batch1[0].arguments.values[0].value = "comment 1";
		batch1[0].arguments.values[1] = new M3Pair();
		batch1[0].arguments.values[1].key = LegoAnnotationType.comment.name();
		batch1[0].arguments.values[1].value = "comment 2";
		
		M3BatchResponse resp1 = handler.m3Batch(uid, intention, packetId, batch1, true);
		assertEquals(resp1.message, M3BatchResponse.MESSAGE_TYPE_SUCCESS, resp1.message_type);
		
		
		Map<Object, Object> data2 = renderModel(modelId);
		List annotations2 = (List) data2.get("annotations");
		assertNotNull(annotations2);
		assertEquals(4, annotations2.size());
		
		
		// remove one annotation
		M3Request[] batch2 = new M3Request[1];
		batch2[0] = new M3Request();
		batch2[0].entity = Entity.model.name();
		batch2[0].operation = Operation.removeAnnotation.getLbl();
		batch2[0].arguments = new M3Argument();
		batch2[0].arguments.modelId = modelId;

		batch2[0].arguments.values = new M3Pair[1];
		batch2[0].arguments.values[0] = new M3Pair();
		batch2[0].arguments.values[0].key = LegoAnnotationType.comment.name();
		batch2[0].arguments.values[0].value = "comment 1";

		M3BatchResponse resp2 = handler.m3Batch(uid, intention, packetId, batch2, true);
		assertEquals(resp2.message, M3BatchResponse.MESSAGE_TYPE_SUCCESS, resp2.message_type);
		
		Map<Object, Object> data3 = renderModel(modelId);
		List annotations3 = (List) data3.get("annotations");
		assertNotNull(annotations3);
		assertEquals(3, annotations3.size());
	}

	/**
	 * @param modelId
	 * @return data
	 */
	private Map<Object, Object> renderModel(final String modelId) {
		final ModelContainer model = models.getModel(modelId);
		final MolecularModelJsonRenderer renderer = JsonOrJsonpBatchHandler.createModelRenderer(model, lookupService);
		final Map<Object, Object> data = renderer.renderModel();
		return data;
	}
	
	@Test
	public void testMultipleMeta() throws Exception {
		models.setPathToOWLFiles(folder.newFolder().getCanonicalPath());
		models.dispose();
		
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
		
		M3BatchResponse response = handler.m3Batch(uid, intention, packetId, requests, true);
		assertEquals(uid, response.uid);
		assertEquals(intention, response.intention);
		assertEquals(M3BatchResponse.MESSAGE_TYPE_SUCCESS, response.message_type);
		final List<Map<String, Object>> relations = (List)((Map) response.data).get("relations");
		final OWLGraphWrapper tbox = models.getGraph();
		final OWLObjectProperty part_of = tbox.getOWLObjectPropertyByIdentifier("part_of");
		assertNotNull(part_of);
		final String partOfJsonId = MolecularModelJsonRenderer.getId(part_of, tbox);
		boolean hasPartOf = false;
		for (Map<String, Object> map : relations) {
			String id = (String)map.get("id");
			assertNotNull(id);
			if (partOfJsonId.equals(id)) {
				assertEquals("true", map.get("relevant"));
				hasPartOf = true;
			}
		}
		assertTrue(relations.size() > 100);
		assertTrue(hasPartOf);

		final List<Map<String, Object>> evidences = (List)((Map) response.data).get("evidence");
		assertTrue(evidences.size() > 100);
		
		final Set<String> modelIds = (Set)((Map) response.data).get("model_ids");
		assertEquals(0, modelIds.size());
	}

	@Test
	public void testProteinNames() throws Exception {
		
		M3Request[] batch1 = new M3Request[1];
		batch1[0] = new M3Request();
		batch1[0].entity = Entity.model.name();
		batch1[0].operation = Operation.generateBlank.getLbl();
		batch1[0].arguments = new M3Argument();
		batch1[0].arguments.db = "goa_chicken";
		
		M3BatchResponse response1 = handler.m3Batch(uid, intention, packetId, batch1, true);
		assertEquals(uid, response1.uid);
		assertEquals(intention, response1.intention);
		assertEquals(M3BatchResponse.MESSAGE_TYPE_SUCCESS, response1.message_type);
		final String modelId = (String) response1.data.get("id");
		
		// check that protein id resolves to the expected label
		final String proteinId = "UniProtKB:F1NGQ9";
		final String proteinLabel = "FZD1";
		final String taxonId = "9031"; // TODO
		LookupEntry entry = lookupService.lookup(proteinId, taxonId);
		assertEquals(proteinLabel, entry.label);
		
		// try to generate a model with a protein and protein label
		M3Request[] batch2 = new M3Request[1];
		batch2[0] = new M3Request();
		batch2[0].entity = Entity.individual.name();
		batch2[0].operation = Operation.create.getLbl();
		batch2[0].arguments = new M3Argument();
		batch2[0].arguments.modelId = modelId;
		batch2[0].arguments.subject = "GO:0006915"; // apoptotic process
		batch2[0].arguments.expressions = new M3Expression[1];
		batch2[0].arguments.expressions[0] = new M3Expression();
		batch2[0].arguments.expressions[0].type = "svf";
		batch2[0].arguments.expressions[0].onProp = "RO:0002333"; // enabled_by
		batch2[0].arguments.expressions[0].literal = proteinId;
		
		M3BatchResponse response2 = handler.m3Batch(uid, intention, packetId, batch2, true);
		assertEquals(uid, response2.uid);
		assertEquals(intention, response2.intention);
		assertEquals(response2.message, M3BatchResponse.MESSAGE_TYPE_SUCCESS, response2.message_type);
		List<Map<Object, Object>> iObjs = (List) response2.data.get(KEY_INDIVIDUALS);
		assertEquals(1, iObjs.size());
		Map<Object, Object> individual = iObjs.get(0);
		Map onProperty = (Map)((List) individual.get(KEY.type)).get(1);
		Map svf = (Map) onProperty.get(KEY.someValuesFrom);
		assertEquals(proteinId, svf.get(KEY.id));
		assertEquals(proteinLabel, svf.get(KEY.label));
	}
	
	@Test
	public void testCreateBlankModelFromGAF() throws Exception {
		models.dispose();
		
		M3Request[] batch1 = new M3Request[1];
		batch1[0] = new M3Request();
		batch1[0].entity = Entity.model.name();
		batch1[0].operation = Operation.generateBlank.getLbl();
		batch1[0].arguments = new M3Argument();
		batch1[0].arguments.db = "goa_chicken";
		
		M3BatchResponse response1 = handler.m3Batch(uid, intention, packetId, batch1, true);
		assertEquals(uid, response1.uid);
		assertEquals(intention, response1.intention);
		assertEquals(M3BatchResponse.MESSAGE_TYPE_SUCCESS, response1.message_type);
		final String modelId1 = (String) response1.data.get("id");
		
		M3Request[] batch2 = new M3Request[1];
		batch2[0] = new M3Request();
		batch2[0].entity = Entity.model.name();
		batch2[0].operation = Operation.generateBlank.getLbl();
		batch2[0].arguments = new M3Argument();
		batch2[0].arguments.db = "goa_chicken";
		
		M3BatchResponse response2 = handler.m3Batch(uid, intention, packetId, batch2, true);
		assertEquals(uid, response2.uid);
		assertEquals(intention, response2.intention);
		assertEquals(response2.message, M3BatchResponse.MESSAGE_TYPE_SUCCESS, response2.message_type);
		final String modelId2 = (String) response2.data.get("id");
		
		assertNotEquals(modelId1, modelId2);
		
		M3Request[] batch3 = new M3Request[1];
		batch3[0] = new M3Request();
		batch3[0].entity = Entity.model.name();
		batch3[0].operation = Operation.generateBlank.getLbl();
		batch3[0].arguments = new M3Argument();
		batch3[0].arguments.db = "jcvi";
		
		M3BatchResponse response3 = handler.m3Batch(uid, intention, packetId, batch3, true);
		assertEquals(uid, response3.uid);
		assertEquals(intention, response3.intention);
		assertEquals(response3.message, M3BatchResponse.MESSAGE_TYPE_SUCCESS, response3.message_type);
		final String modelId3 = (String) response3.data.get("id");
		
		assertNotEquals(modelId1, modelId3);
		assertNotEquals(modelId2, modelId3);
	}
	
	@Test
	@Ignore("This test takes way to loong to execute.")
	public void testCreateModelFromGAF() throws Exception {
		models.dispose();
		
		M3Request[] batch1 = new M3Request[1];
		batch1[0] = new M3Request();
		batch1[0].entity = Entity.model.name();
		batch1[0].operation = Operation.generate.getLbl();
		batch1[0].arguments = new M3Argument();
		batch1[0].arguments.db = "goa_chicken";
		batch1[0].arguments.subject = "GO:0004637";
		
		M3BatchResponse response1 = handler.m3Batch(uid, intention, packetId, batch1, true);
		assertEquals(uid, response1.uid);
		assertEquals(intention, response1.intention);
		assertEquals(response1.message, M3BatchResponse.MESSAGE_TYPE_SUCCESS, response1.message_type);
		final String modelId1 = (String) response1.data.get("id");
		
		M3Request[] batch2 = new M3Request[1];
		batch2[0] = new M3Request();
		batch2[0].entity = Entity.model.name();
		batch2[0].operation = Operation.generate.getLbl();
		batch2[0].arguments = new M3Argument();
		batch2[0].arguments.db = "goa_chicken";
		batch2[0].arguments.subject = "GO:0005509";
		
		M3BatchResponse response2 = handler.m3Batch(uid, intention, packetId, batch2, true);
		assertEquals(uid, response2.uid);
		assertEquals(intention, response2.intention);
		assertEquals(response2.message, M3BatchResponse.MESSAGE_TYPE_SUCCESS, response2.message_type);
		final String modelId2 = (String) response2.data.get("id");
		
		assertNotEquals(modelId1, modelId2);
		
		M3Request[] batch3 = new M3Request[1];
		batch3[0] = new M3Request();
		batch3[0].entity = Entity.model.name();
		batch3[0].operation = Operation.generate.getLbl();
		batch3[0].arguments = new M3Argument();
		batch3[0].arguments.db = "jcvi";
		batch3[0].arguments.subject = "GO:0003887";
		
		M3BatchResponse response3 = handler.m3Batch(uid, intention, packetId, batch3, true);
		assertEquals(uid, response3.uid);
		assertEquals(intention, response3.intention);
		assertEquals(response3.message, M3BatchResponse.MESSAGE_TYPE_SUCCESS, response3.message_type);
		final String modelId3 = (String) response3.data.get("id");
		
		assertNotEquals(modelId1, modelId3);
		assertNotEquals(modelId2, modelId3);
	}
	
	@Test
	public void testDelete() throws Exception {
		models.dispose();
		
		final String modelId = generateBlankModel();
		
		// create
		M3Request[] batch1 = new M3Request[1];
		batch1[0] = new M3Request();
		batch1[0].entity = Entity.individual.name();
		batch1[0].operation = Operation.create.getLbl();
		batch1[0].arguments = new M3Argument();
		batch1[0].arguments.modelId = modelId;
		batch1[0].arguments.subject = "GO:0008104"; // protein localization
		batch1[0].arguments.expressions = new M3Expression[2];
		batch1[0].arguments.expressions[0] = new M3Expression();
		batch1[0].arguments.expressions[0].type = "svf";
		batch1[0].arguments.expressions[0].onProp = "RO:0002333"; // enabled_by
		batch1[0].arguments.expressions[0].literal = "UniProtKB:P0000";
		
		batch1[0].arguments.expressions[1] = new M3Expression();
		batch1[0].arguments.expressions[1].type = "svf";
		batch1[0].arguments.expressions[1].onProp = "BFO:0000050"; // part_of
		batch1[0].arguments.expressions[1].literal = "'apoptotic process'";
		
		M3BatchResponse response1 = handler.m3Batch(uid, intention, packetId, batch1, true);
		assertEquals(uid, response1.uid);
		assertEquals(intention, response1.intention);
		assertEquals(response1.message, M3BatchResponse.MESSAGE_TYPE_SUCCESS, response1.message_type);
		
		
		List<Map<Object, Object>> iObjs1 = (List) response1.data.get(KEY_INDIVIDUALS);
		assertEquals(1, iObjs1.size());
		Map<Object, Object> individual1 = iObjs1.get(0);
		assertNotNull(individual1);
		final String individualId = (String) individual1.get(MolecularModelJsonRenderer.KEY.id);
		assertNotNull(individualId);
		
		List<Map<Object, Object>> types1 = (List) individual1.get(MolecularModelJsonRenderer.KEY.type);
		assertEquals(3, types1.size());
		String apopId = null;
		for(Map<Object, Object> e : types1) {
			Object cType = e.get(MolecularModelJsonRenderer.KEY.type);
			if (MolecularModelJsonRenderer.VAL.Restriction.equals(cType)) {
				Map<Object, Object> svf = (Map<Object, Object>) e.get(MolecularModelJsonRenderer.KEY.someValuesFrom);
				String id = (String) svf.get(MolecularModelJsonRenderer.KEY.id);
				if (id.equals("GO:0006915")) {
					apopId = id;
					break;
				}
			}
		}
		assertNotNull(apopId);
		
		// delete
		M3Request[] batch2 = new M3Request[1];
		batch2[0] = new M3Request();
		batch2[0].entity = Entity.individual.name();
		batch2[0].operation = Operation.removeType.getLbl();
		batch2[0].arguments = new M3Argument();
		batch2[0].arguments.modelId = modelId;
		batch2[0].arguments.individual = individualId;
		
		batch2[0].arguments.expressions = new M3Expression[1];
		batch2[0].arguments.expressions[0] = new M3Expression();
		batch2[0].arguments.expressions[0].type = "svf";
		batch2[0].arguments.expressions[0].onProp = "BFO:0000050"; // part_of
		batch2[0].arguments.expressions[0].literal = apopId;
		
		
		M3BatchResponse response2 = handler.m3Batch(uid, intention, packetId, batch2, true);
		assertEquals(uid, response2.uid);
		assertEquals(intention, response2.intention);
		assertEquals(response2.message, M3BatchResponse.MESSAGE_TYPE_SUCCESS, response2.message_type);
		
		List<Map<Object, Object>> iObjs2 = (List) response2.data.get(KEY_INDIVIDUALS);
		assertEquals(1, iObjs2.size());
		Map<Object, Object> individual2 = iObjs2.get(0);
		assertNotNull(individual2);
		List<Map<Object, Object>> types2 = (List) individual2.get(MolecularModelJsonRenderer.KEY.type);
		assertEquals(2, types2.size());
	}
	
	@Test
	public void testModelSearch() throws Exception {
		models.setPathToOWLFiles(folder.newFolder().getCanonicalPath());
		models.dispose();

		final String modelId = generateBlankModel();
		
		// create
		M3Request[] batch1 = new M3Request[1];
		batch1[0] = new M3Request();
		batch1[0].entity = Entity.individual.name();
		batch1[0].operation = Operation.create.getLbl();
		batch1[0].arguments = new M3Argument();
		batch1[0].arguments.modelId = modelId;
		batch1[0].arguments.subject = "GO:0008104"; // protein localization
		batch1[0].arguments.expressions = new M3Expression[2];
		batch1[0].arguments.expressions[0] = new M3Expression();
		batch1[0].arguments.expressions[0].type = "svf";
		batch1[0].arguments.expressions[0].onProp = "RO:0002333"; // enabled_by
		batch1[0].arguments.expressions[0].literal = "UniProtKB:P0000";
		
		batch1[0].arguments.expressions[1] = new M3Expression();
		batch1[0].arguments.expressions[1].type = "svf";
		batch1[0].arguments.expressions[1].onProp = "BFO:0000050"; // part_of
		batch1[0].arguments.expressions[1].literal = "GO:0006915";
		
		M3BatchResponse response1 = handler.m3Batch(uid, intention, packetId, batch1, true);
		assertEquals(uid, response1.uid);
		assertEquals(intention, response1.intention);
		assertEquals(response1.message, M3BatchResponse.MESSAGE_TYPE_SUCCESS, response1.message_type);
	
		// search
		M3Request[] batch2 = new M3Request[1];
		batch2[0] = new M3Request();
		batch2[0].entity = Entity.model.name();
		batch2[0].operation = Operation.search.getLbl();
		batch2[0].arguments = new M3Argument();
		batch2[0].arguments.values = new M3Pair[1];
		batch2[0].arguments.values[0] = new M3Pair();
		batch2[0].arguments.values[0].key = "id";
		batch2[0].arguments.values[0].value = "GO:0008104";
		
		M3BatchResponse response2 = handler.m3Batch(uid, intention, packetId, batch2, true);
		assertEquals(uid, response2.uid);
		assertEquals(intention, response2.intention);
		assertEquals(response2.message, M3BatchResponse.MESSAGE_TYPE_SUCCESS, response2.message_type);
		
		Set<String> foundIds = (Set<String>) response2.data.get("model_ids");
		assertEquals(1, foundIds.size());
		assertTrue(foundIds.contains(modelId));
	}
	
	@Test
	public void testCreateModelAndIndividualBatch() throws Exception {
		M3Request[] batch = new M3Request[2];
		batch[0] = new M3Request();
		batch[0].entity = Entity.model.name();
		batch[0].operation = Operation.generateBlank.getLbl();
		batch[1] = new M3Request();
		batch[1].entity = Entity.individual.name();
		batch[1].operation = Operation.createComposite.getLbl();
		batch[1].arguments = new M3Argument();
		batch[1].arguments.subject = "GO:0003674"; // molecular function
		batch[1].arguments.predicate = "BFO:0000050"; // part of
		batch[1].arguments.object = "GO:0008150"; // biological process
		batch[1].arguments.expressions = new M3Expression[1];
		batch[1].arguments.expressions[0] = new M3Expression();
		batch[1].arguments.expressions[0].type = "svf";
		batch[1].arguments.expressions[0].onProp = "RO:0002333"; // enabled_by
		batch[1].arguments.expressions[0].literal = "UniProtKB:P0000";
		
		M3BatchResponse response = handler.m3Batch(uid, intention, packetId, batch, true);
		assertEquals(uid, response.uid);
		assertEquals(intention, response.intention);
		assertEquals(response.message, M3BatchResponse.MESSAGE_TYPE_SUCCESS, response.message_type);
	}
	
	@Test
	public void testInconsistentModel() throws Exception {
		models.dispose();
		
		final String modelId = generateBlankModel();
		
		// create
		M3Request[] batch1 = new M3Request[1];
		batch1[0] = new M3Request();
		batch1[0].entity = Entity.individual.name();
		batch1[0].operation = Operation.create.getLbl();
		batch1[0].arguments = new M3Argument();
		batch1[0].arguments.modelId = modelId;
		batch1[0].arguments.subject = "GO:0009653"; // anatomical structure morphogenesis
		batch1[0].arguments.expressions = new M3Expression[1];
		batch1[0].arguments.expressions[0] = new M3Expression();
		batch1[0].arguments.expressions[0].type = "class";
		batch1[0].arguments.expressions[0].literal = "GO:0048856"; // anatomical structure development
		
		M3BatchResponse response1 = handler.m3Batch(uid, intention, packetId, batch1, true);
		assertEquals(uid, response1.uid);
		assertEquals(intention, response1.intention);
		assertEquals(response1.message, M3BatchResponse.MESSAGE_TYPE_SUCCESS, response1.message_type);
		Map<Object, Object> data = response1.data;
		Object inconsistentFlag = data.get("inconsistent_p");
		assertEquals(Boolean.TRUE, inconsistentFlag);
	}

	@Test
	public void testInferencesRedundant() throws Exception {
		models.dispose();
		assertTrue(JsonOrJsonpBatchHandler.ADD_INFERENCES);
		
		final String modelId = generateBlankModel();
		
		// GO:0009826 ! unidimensional cell growth
		// GO:0000902 ! cell morphogenesis
		// should infer only one type: 'unidimensional cell growth'
		// 'cell morphogenesis' is a super-class and redundant
		
		// create
		M3Request[] batch1 = new M3Request[1];
		batch1[0] = new M3Request();
		batch1[0].entity = Entity.individual.name();
		batch1[0].operation = Operation.create.getLbl();
		batch1[0].arguments = new M3Argument();
		batch1[0].arguments.modelId = modelId;
		batch1[0].arguments.subject = "GO:0000902"; // cell morphogenesis
		batch1[0].arguments.expressions = new M3Expression[1];
		batch1[0].arguments.expressions[0] = new M3Expression();
		batch1[0].arguments.expressions[0].type = "class";
		batch1[0].arguments.expressions[0].literal = "GO:0009826"; // unidimensional cell growth

		M3BatchResponse response1 = handler.m3Batch(uid, intention, packetId, batch1, true);
		assertEquals(uid, response1.uid);
		assertEquals(intention, response1.intention);
		assertEquals(response1.message, M3BatchResponse.MESSAGE_TYPE_SUCCESS, response1.message_type);
		Map<Object, Object> data = response1.data;
		assertNull("Model should not be inconsistent", data.get("inconsistent_p"));
		List inferred = (List) data.get(KEY_INDIVIDUALS_INFERENCES);
		assertNotNull(inferred);
		assertEquals(1, inferred.size());
		Map inferredData = (Map) inferred.get(0);
		List types = (List) inferredData.get(KEY.type);
		assertEquals(1, types.size());
		Map type = (Map) types.get(0);
		assertEquals("GO:0009826", type.get(KEY.id));
	}
	
	@Test
	public void testInferencesAdditional() throws Exception {
		models.dispose();
		
		final String modelId = generateBlankModel();
		
		// GO:0051231 ! spindle elongation
		// part_of GO:0000278 ! mitotic cell cycle
		// should infer one new type: GO:0000022 ! mitotic spindle elongation
		
		// create
		M3Request[] batch1 = new M3Request[1];
		batch1[0] = new M3Request();
		batch1[0].entity = Entity.individual.name();
		batch1[0].operation = Operation.create.getLbl();
		batch1[0].arguments = new M3Argument();
		batch1[0].arguments.modelId = modelId;
		batch1[0].arguments.subject = "GO:0051231"; // spindle elongation
		batch1[0].arguments.expressions = new M3Expression[1];
		batch1[0].arguments.expressions[0] = new M3Expression();
		batch1[0].arguments.expressions[0].type = "svf";
		batch1[0].arguments.expressions[0].onProp = "BFO:0000050"; // part_of
		batch1[0].arguments.expressions[0].literal = "GO:0000278"; // mitotic cell cycle

		M3BatchResponse response1 = handler.m3Batch(uid, intention, packetId, batch1, true);
		assertEquals(uid, response1.uid);
		assertEquals(intention, response1.intention);
		assertEquals(response1.message, M3BatchResponse.MESSAGE_TYPE_SUCCESS, response1.message_type);
		Map<Object, Object> data = response1.data;
		assertNull("Model should not be inconsistent", data.get("inconsistent_p"));
		List inferred = (List) data.get(KEY_INDIVIDUALS_INFERENCES);
		assertNotNull(inferred);
		assertEquals(1, inferred.size());
		Map inferredData = (Map) inferred.get(0);
		List types = (List) inferredData.get(KEY.type);
		assertEquals(1, types.size());
		Map type = (Map) types.get(0);
		assertEquals("GO:0000022", type.get(KEY.id));
	}
	
	@Test
	public void testValidationBeforeSave() throws Exception {
		assertTrue(JsonOrJsonpBatchHandler.VALIDATE_BEFORE_SAVE);
		models.dispose();
		
		final String modelId = generateBlankModel();
		
		// try to save
		M3Request[] batch = new M3Request[1];
		batch[0] = new M3Request();
		batch[0].entity = Entity.model.name();
		batch[0].operation = Operation.storeModel.getLbl();
		batch[0].arguments = new M3Argument();
		batch[0].arguments.modelId = modelId;
		M3BatchResponse resp1 = handler.m3Batch(uid, intention, packetId, batch, true);
		assertEquals("This operation must fail as the model has no title or individuals", M3BatchResponse.MESSAGE_TYPE_ERROR, resp1.message_type);
		assertNotNull(resp1.commentary);
		assertTrue(resp1.commentary.contains("title"));
	}
	
	@Test
	public void testPrivileged() throws Exception {
		M3Request[] batch = new M3Request[1];
		batch[0] = new M3Request();
		batch[0].entity = Entity.model.name();
		batch[0].operation = Operation.generateBlank.getLbl();
		M3BatchResponse resp1 = handler.m3Batch(uid, intention, packetId, batch, false);
		assertEquals(M3BatchResponse.MESSAGE_TYPE_ERROR, resp1.message_type);
		assertTrue(resp1.message.contains("Insufficient"));
	}
	
	@Test
	public void testExportLegacy() throws Exception {
		final String modelId = generateBlankModel();
		
		// create
		M3Request[] batch1 = new M3Request[1];
		batch1[0] = new M3Request();
		batch1[0].entity = Entity.individual.name();
		batch1[0].operation = Operation.create.getLbl();
		batch1[0].arguments = new M3Argument();
		batch1[0].arguments.modelId = modelId;
		batch1[0].arguments.subject = "GO:0008104"; // protein localization
		batch1[0].arguments.expressions = new M3Expression[2];
		batch1[0].arguments.expressions[0] = new M3Expression();
		batch1[0].arguments.expressions[0].type = "svf";
		batch1[0].arguments.expressions[0].onProp = "RO:0002333"; // enabled_by
		batch1[0].arguments.expressions[0].literal = "UniProtKB:P0000";
		
		batch1[0].arguments.expressions[1] = new M3Expression();
		batch1[0].arguments.expressions[1].type = "svf";
		batch1[0].arguments.expressions[1].onProp = "BFO:0000050"; // part_of
		batch1[0].arguments.expressions[1].literal = "GO:0006915";
		
		M3BatchResponse response1 = handler.m3Batch(uid, intention, packetId, batch1, true);
		assertEquals(uid, response1.uid);
		assertEquals(intention, response1.intention);
		assertEquals(response1.message, M3BatchResponse.MESSAGE_TYPE_SUCCESS, response1.message_type);
		
		
		M3Request[] batch2 = new M3Request[1];
		batch2[0] = new M3Request();
		batch2[0].operation = Operation.exportModelLegacy.getLbl();
		batch2[0].entity = Entity.model.name();
		batch2[0].arguments = new M3Argument();
		batch2[0].arguments.modelId = modelId;
//		batch2[0].arguments.format = "gpad"; // optional, default is gaf 
		
		M3BatchResponse response2 = handler.m3Batch(uid, intention, packetId, batch2, true);
		assertEquals(uid, response2.uid);
		assertEquals(intention, response2.intention);
		assertEquals(response2.message, M3BatchResponse.MESSAGE_TYPE_SUCCESS, response2.message_type);
		Object exportString = response2.data.get(Operation.exportModel.getLbl());
//		System.out.println("----------------");
//		System.out.println(exportString);
//		System.out.println("----------------");
		assertNotNull(exportString);
	}
	
	@Test
	public void testUndoRedo() throws Exception {
		final String modelId = generateBlankModel();

		// create
		M3Request[] batch1 = new M3Request[1];
		batch1[0] = new M3Request();
		batch1[0].entity = Entity.individual.name();
		batch1[0].operation = Operation.create.getLbl();
		batch1[0].arguments = new M3Argument();
		batch1[0].arguments.modelId = modelId;
		batch1[0].arguments.subject = "GO:0008104"; // protein localization
		batch1[0].arguments.expressions = new M3Expression[2];
		batch1[0].arguments.expressions[0] = new M3Expression();
		batch1[0].arguments.expressions[0].type = "svf";
		batch1[0].arguments.expressions[0].onProp = "RO:0002333"; // enabled_by
		batch1[0].arguments.expressions[0].literal = "UniProtKB:P0000";

		batch1[0].arguments.expressions[1] = new M3Expression();
		batch1[0].arguments.expressions[1].type = "svf";
		batch1[0].arguments.expressions[1].onProp = "BFO:0000050"; // part_of
		batch1[0].arguments.expressions[1].literal = "'apoptotic process'";

		M3BatchResponse response1 = handler.m3Batch(uid, intention, packetId, batch1, true);
		assertEquals(uid, response1.uid);
		assertEquals(intention, response1.intention);
		assertEquals(response1.message, M3BatchResponse.MESSAGE_TYPE_SUCCESS, response1.message_type);


		List<Map<Object, Object>> iObjs1 = (List) response1.data.get(KEY_INDIVIDUALS);
		assertEquals(1, iObjs1.size());
		Map<Object, Object> individual1 = iObjs1.get(0);
		assertNotNull(individual1);
		final String individualId = (String) individual1.get(MolecularModelJsonRenderer.KEY.id);
		assertNotNull(individualId);

		List<Map<Object, Object>> types1 = (List) individual1.get(MolecularModelJsonRenderer.KEY.type);
		assertEquals(3, types1.size());
		String apopId = null;
		for(Map<Object, Object> e : types1) {
			Object cType = e.get(MolecularModelJsonRenderer.KEY.type);
			if (MolecularModelJsonRenderer.VAL.Restriction.equals(cType)) {
				Map<Object, Object> svf = (Map<Object, Object>) e.get(MolecularModelJsonRenderer.KEY.someValuesFrom);
				String id = (String) svf.get(MolecularModelJsonRenderer.KEY.id);
				if (id.equals("GO:0006915")) {
					apopId = id;
					break;
				}
			}
		}
		assertNotNull(apopId);
		
		// check undo redo list
		M3Request[] batch2 = new M3Request[1];
		batch2[0] = new M3Request();
		batch2[0].entity = Entity.model.name();
		batch2[0].operation = Operation.getUndoRedo.getLbl();
		batch2[0].arguments = new M3Argument();
		batch2[0].arguments.modelId = modelId;
		M3BatchResponse response2 = handler.m3Batch(uid, intention, packetId, batch2, true);
		assertEquals(uid, response2.uid);
		assertEquals(intention, response2.intention);
		assertEquals(response2.message, M3BatchResponse.MESSAGE_TYPE_SUCCESS, response2.message_type);
		List<Object> undo2 = (List<Object>) response2.data.get("undo");
		List<Object> redo2 = (List<Object>) response2.data.get("redo");
		assertTrue(undo2.size() > 1);
		assertTrue(redo2.isEmpty());

		// delete
		M3Request[] batch3 = new M3Request[1];
		batch3[0] = new M3Request();
		batch3[0].entity = Entity.individual.name();
		batch3[0].operation = Operation.removeType.getLbl();
		batch3[0].arguments = new M3Argument();
		batch3[0].arguments.modelId = modelId;
		batch3[0].arguments.individual = individualId;

		batch3[0].arguments.expressions = new M3Expression[1];
		batch3[0].arguments.expressions[0] = new M3Expression();
		batch3[0].arguments.expressions[0].type = "svf";
		batch3[0].arguments.expressions[0].onProp = "BFO:0000050"; // part_of
		batch3[0].arguments.expressions[0].literal = apopId;


		M3BatchResponse response3 = handler.m3Batch(uid, intention, packetId, batch3, true);
		assertEquals(uid, response3.uid);
		assertEquals(intention, response3.intention);
		assertEquals(response3.message, M3BatchResponse.MESSAGE_TYPE_SUCCESS, response3.message_type);

		List<Map<Object, Object>> iObjs3 = (List) response3.data.get(KEY_INDIVIDUALS);
		assertEquals(1, iObjs3.size());
		Map<Object, Object> individual3 = iObjs3.get(0);
		assertNotNull(individual3);
		List<Map<Object, Object>> types3 = (List) individual3.get(MolecularModelJsonRenderer.KEY.type);
		assertEquals(2, types3.size());
		
		// check undo redo list
		M3Request[] batch4 = new M3Request[1];
		batch4[0] = new M3Request();
		batch4[0].entity = Entity.model.name();
		batch4[0].operation = Operation.getUndoRedo.getLbl();
		batch4[0].arguments = new M3Argument();
		batch4[0].arguments.modelId = modelId;
		
		M3BatchResponse response4 = handler.m3Batch(uid, intention, packetId, batch4, true);
		assertEquals(uid, response4.uid);
		assertEquals(intention, response4.intention);
		assertEquals(response4.message, M3BatchResponse.MESSAGE_TYPE_SUCCESS, response4.message_type);
		List<Object> undo4 = (List<Object>) response4.data.get("undo");
		List<Object> redo4 = (List<Object>) response4.data.get("redo");
		assertTrue(undo4.size() > 1);
		assertTrue(redo4.isEmpty());
		
		// undo
		M3Request[] batch5 = new M3Request[1];
		batch5[0] = new M3Request();
		batch5[0].entity = Entity.model.name();
		batch5[0].operation = Operation.undo.getLbl();
		batch5[0].arguments = new M3Argument();
		batch5[0].arguments.modelId = modelId;
		
		M3BatchResponse response5 = handler.m3Batch(uid, intention, packetId, batch5, true);
		assertEquals(uid, response5.uid);
		assertEquals(intention, response5.intention);
		assertEquals(response5.message, M3BatchResponse.MESSAGE_TYPE_SUCCESS, response5.message_type);

		
		// check undo redo list
		M3Request[] batch6 = new M3Request[1];
		batch6[0] = new M3Request();
		batch6[0].entity = Entity.model.name();
		batch6[0].operation = Operation.getUndoRedo.getLbl();
		batch6[0].arguments = new M3Argument();
		batch6[0].arguments.modelId = modelId;
		
		M3BatchResponse response6 = handler.m3Batch(uid, intention, packetId, batch6, true);
		assertEquals(uid, response6.uid);
		assertEquals(intention, response6.intention);
		assertEquals(response6.message, M3BatchResponse.MESSAGE_TYPE_SUCCESS, response6.message_type);
		List<Object> undo6 = (List<Object>) response6.data.get("undo");
		List<Object> redo6 = (List<Object>) response6.data.get("redo");
		assertTrue(undo6.size() > 1);
		assertTrue(redo6.size() == 1);
		
	}
	
	/**
	 * @return modelId
	 */
	private String generateBlankModel() {
		// create blank model
		M3Request[] batch = new M3Request[1];
		batch[0] = new M3Request();
		batch[0].entity = Entity.model.name();
		batch[0].operation = Operation.generateBlank.getLbl();
		M3BatchResponse resp1 = handler.m3Batch(uid, intention, null, batch, true);
		assertEquals(resp1.message, M3BatchResponse.MESSAGE_TYPE_SUCCESS, resp1.message_type);
		assertNotNull(resp1.packet_id);
		String modelId = (String) resp1.data.get("id");
		assertNotNull(modelId);
		return modelId;
	}
	
	static void printJson(Object resp) {
		GsonBuilder builder = new GsonBuilder();
		builder.setPrettyPrinting();
		Gson gson = builder.create();
		String json = gson.toJson(resp);
		System.out.println("---------");
		System.out.println(json);
		System.out.println("---------");
	}
}
