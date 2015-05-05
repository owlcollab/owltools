package owltools.gaf.lego.server.handler;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import owltools.gaf.lego.IdStringManager;
import owltools.gaf.lego.IdStringManager.AnnotationShorthand;
import owltools.gaf.lego.UndoAwareMolecularModelManager;
import owltools.gaf.lego.json.JsonAnnotation;
import owltools.gaf.lego.json.JsonEvidenceInfo;
import owltools.gaf.lego.json.JsonModel;
import owltools.gaf.lego.json.JsonOwlFact;
import owltools.gaf.lego.json.JsonOwlIndividual;
import owltools.gaf.lego.json.JsonOwlObject;
import owltools.gaf.lego.json.JsonOwlObject.JsonOwlObjectType;
import owltools.gaf.lego.json.JsonRelationInfo;
import owltools.gaf.lego.json.MolecularModelJsonRenderer;
import owltools.gaf.lego.server.StartUpTool;
import owltools.gaf.lego.server.external.CombinedExternalLookupService;
import owltools.gaf.lego.server.external.ExternalLookupService;
import owltools.gaf.lego.server.external.ExternalLookupService.LookupEntry;
import owltools.gaf.lego.server.external.ProteinToolService;
import owltools.gaf.lego.server.external.TableLookupService;
import owltools.gaf.lego.server.handler.M3BatchHandler.Entity;
import owltools.gaf.lego.server.handler.M3BatchHandler.M3Argument;
import owltools.gaf.lego.server.handler.M3BatchHandler.M3BatchResponse;
import owltools.gaf.lego.server.handler.M3BatchHandler.M3Request;
import owltools.gaf.lego.server.handler.M3BatchHandler.Operation;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;
import owltools.util.ModelContainer;

@SuppressWarnings("unchecked")
public class BatchModelHandlerTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private static JsonOrJsonpBatchHandler handler = null;
	private static UndoAwareMolecularModelManager models = null;
	private static Set<OWLObjectProperty> importantRelations = null;
	private final static DateGenerator dateGenerator = new DateGenerator();

	static final String uid = "test-user";
	static final String intention = "test-intention";
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
		handler = new JsonOrJsonpBatchHandler(models, importantRelations, lookupService) {

			@Override
			protected String generateDateString() {
				// hook for overriding the date generation with a custom counter
				if (dateGenerator.useCounter) {
					int count = dateGenerator.counter;
					dateGenerator.counter += 1;
					return Integer.toString(count);
				}
				return super.generateDateString();
			}
		};
		JsonOrJsonpBatchHandler.ADD_INFERENCES = true;
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
	public void testTypeOperations() throws Exception {
		final String modelId = generateBlankModel();
		
		// create two individuals
		List<M3Request> batch = new ArrayList<M3Request>();
		M3Request r = BatchTestTools.addIndividual(modelId, "GO:0006915"); // apoptotic process
		r.arguments.assignToVariable = "i1";
		r.arguments.values = new JsonAnnotation[2];
		r.arguments.values[0] = JsonAnnotation.create(AnnotationShorthand.comment, "comment 1");
		r.arguments.values[1] = JsonAnnotation.create(AnnotationShorthand.comment, "comment 2");
		batch.add(r);
		
		r = new M3Request();
		r.entity = Entity.individual;
		r.operation = Operation.addType;
		r.arguments = new M3Argument();
		r.arguments.modelId = modelId;
		r.arguments.individual = "i1";
		r.arguments.expressions = new JsonOwlObject[1];
		r.arguments.expressions[0] = BatchTestTools.createSvf("BFO:0000066", "GO:0005623"); // occurs_in, cell
		batch.add(r);
		
		r = new M3Request();
		r.entity = Entity.individual;
		r.operation = Operation.addType;
		r.arguments = new M3Argument();
		r.arguments.modelId = modelId;
		r.arguments.individual = "i1";
		r.arguments.expressions = new JsonOwlObject[1];
		r.arguments.expressions[0] = new JsonOwlObject();
		r.arguments.expressions[0].type = JsonOwlObjectType.SomeValueFrom;
		r.arguments.expressions[0].property = new JsonOwlObject();
		r.arguments.expressions[0].property.type = JsonOwlObjectType.ObjectProperty;
		r.arguments.expressions[0].property.id = "RO:0002333"; // enabled_by
		// "GO:0043234 and (('has part' some UniProtKB:P0002) OR ('has part' some UniProtKB:P0003))";
		r.arguments.expressions[0].filler = createComplexExpr();
		batch.add(r);
		
		r = BatchTestTools.addIndividual(modelId, "GO:0043276", 
				BatchTestTools.createSvf("RO:0002333", "GO:0043234")); // enabled_by
		batch.add(r);
		
		M3BatchResponse resp2 = handler.m3Batch(uid, intention, packetId, batch.toArray(new M3Request[batch.size()]), true);
		assertEquals(resp2.message, M3BatchResponse.MESSAGE_TYPE_SUCCESS, resp2.messageType);
		String individual1 = null;
		String individual2 = null;
		JsonOwlIndividual[] iObjs = BatchTestTools.responseIndividuals(resp2);
		assertEquals(2, iObjs.length);
		for(JsonOwlIndividual iObj : iObjs) {
			String id = iObj.id;
			if (id.contains("6915")) {
				individual1 = id;
				assertEquals(3, iObj.type.length);
			}
			else {
				individual2 = id;
				assertEquals(2, iObj.type.length);
			}
		}
		assertNotNull(individual1);
		assertNotNull(individual2);
		
		// create fact
		M3Request[] batch3 = new M3Request[1];
		batch3[0] = new M3Request();
		batch3[0].entity = Entity.edge;
		batch3[0].operation = Operation.add;
		batch3[0].arguments = new M3Argument();
		batch3[0].arguments.modelId = modelId;
		batch3[0].arguments.subject = individual1;
		batch3[0].arguments.object = individual2;
		batch3[0].arguments.predicate = "BFO:0000050"; // part_of
		
		M3BatchResponse resp3 = handler.m3Batch(uid, intention, packetId, batch3, true);
		assertEquals(resp3.message, M3BatchResponse.MESSAGE_TYPE_SUCCESS, resp3.messageType);
		
		// delete complex expression type
		M3Request[] batch4 = new M3Request[1];
		batch4[0] = new M3Request();
		batch4[0].entity = Entity.individual;
		batch4[0].operation = Operation.removeType;
		batch4[0].arguments = new M3Argument();
		batch4[0].arguments.modelId = modelId;
		batch4[0].arguments.individual = individual1;
		batch4[0].arguments.expressions = new JsonOwlObject[1];
		batch4[0].arguments.expressions[0] = new JsonOwlObject();
		batch4[0].arguments.expressions[0].type = JsonOwlObjectType.SomeValueFrom;
		batch4[0].arguments.expressions[0].property = new JsonOwlObject();
		batch4[0].arguments.expressions[0].property.type = JsonOwlObjectType.ObjectProperty;
		batch4[0].arguments.expressions[0].property.id = "RO:0002333"; // enabled_by
		// "GO:0043234 and (('has part' some UniProtKB:P0002) OR ('has part' some UniProtKB:P0003))";
		batch4[0].arguments.expressions[0].filler = createComplexExpr();
		
		M3BatchResponse resp4 = handler.m3Batch(uid, intention, packetId, batch4, true);
		assertEquals(resp4.message, M3BatchResponse.MESSAGE_TYPE_SUCCESS, resp4.messageType);
		JsonOwlIndividual[] iObjs4 = BatchTestTools.responseIndividuals(resp4);
		assertEquals(1, iObjs4.length);
		JsonOwlObject[] types = iObjs4[0].type;
		assertEquals(2, types.length);
	}
	
	private static JsonOwlObject createComplexExpr() {
		// "GO:0043234 and (('has part' some UniProtKB:P0002) OR ('has part' some UniProtKB:P0003))";
		JsonOwlObject expr = new JsonOwlObject();
		expr.type = JsonOwlObjectType.IntersectionOf;
		expr.expressions = new JsonOwlObject[2];

		// GO:0043234
		expr.expressions[0] = new JsonOwlObject();
		expr.expressions[0].type = JsonOwlObjectType.Class;
		expr.expressions[0].id = "GO:0043234";

		// OR
		expr.expressions[1] = new JsonOwlObject();
		expr.expressions[1].type = JsonOwlObjectType.UnionOf;
		expr.expressions[1].expressions = new JsonOwlObject[2];
		
		//'has part' some UniProtKB:P0002
		expr.expressions[1].expressions[0] = BatchTestTools.createSvf("BFO:0000051", "UniProtKB:P0002");
		
		// 'has part' some UniProtKB:P0003
		expr.expressions[1].expressions[1] = BatchTestTools.createSvf("BFO:0000051", "UniProtKB:P0003");
		
		return expr;
	}

	
	
	@Test
	public void testAddIndividual() throws Exception {
		final String modelId = generateBlankModel();
		
		// create one individuals
		M3Request[] batch2 = new M3Request[1];
		batch2[0] = new M3Request();
		batch2[0].entity = Entity.individual;
		batch2[0].operation = Operation.add;
		batch2[0].arguments = new M3Argument();
		batch2[0].arguments.modelId = modelId;
		batch2[0].arguments.expressions = new JsonOwlObject[1];
		batch2[0].arguments.expressions[0] = new JsonOwlObject();
		batch2[0].arguments.expressions[0].type = JsonOwlObjectType.Class;
		batch2[0].arguments.expressions[0].id = "GO:0006915"; // apoptotic process
		
		M3BatchResponse resp = handler.m3Batch(uid, intention, packetId, batch2, true);
		assertEquals(resp.message, M3BatchResponse.MESSAGE_TYPE_SUCCESS, resp.messageType);
		JsonOwlIndividual[] iObjs = BatchTestTools.responseIndividuals(resp);
		assertEquals(1, iObjs.length);
	}
	
	@Test
	public void testModelAnnotations() throws Exception {
		assertTrue(JsonOrJsonpBatchHandler.USE_USER_ID);
		
		final String modelId = generateBlankModel();
		
		JsonModel data1 = renderModel(modelId);
		assertNotNull(data1.annotations);
		// creation date
		// user id
		assertEquals(2, data1.annotations.length);
		
		// create annotations
		M3Request[] batch1 = new M3Request[1];
		batch1[0] = new M3Request();
		batch1[0].entity = Entity.model;
		batch1[0].operation = Operation.addAnnotation;
		batch1[0].arguments = new M3Argument();
		batch1[0].arguments.modelId = modelId;

		batch1[0].arguments.values = new JsonAnnotation[2];
		batch1[0].arguments.values[0] = new JsonAnnotation();
		batch1[0].arguments.values[0].key = AnnotationShorthand.comment.name();
		batch1[0].arguments.values[0].value = "comment 1";
		batch1[0].arguments.values[1] = new JsonAnnotation();
		batch1[0].arguments.values[1].key = AnnotationShorthand.comment.name();
		batch1[0].arguments.values[1].value = "comment 2";
		
		M3BatchResponse resp1 = handler.m3Batch(uid, intention, packetId, batch1, true);
		assertEquals(resp1.message, M3BatchResponse.MESSAGE_TYPE_SUCCESS, resp1.messageType);
		
		
		JsonModel data2 = renderModel(modelId);
		assertNotNull(data2.annotations);
		assertEquals(4, data2.annotations.length);
		
		
		// remove one annotation
		M3Request[] batch2 = new M3Request[1];
		batch2[0] = new M3Request();
		batch2[0].entity = Entity.model;
		batch2[0].operation = Operation.removeAnnotation;
		batch2[0].arguments = new M3Argument();
		batch2[0].arguments.modelId = modelId;

		batch2[0].arguments.values = new JsonAnnotation[1];
		batch2[0].arguments.values[0] = new JsonAnnotation();
		batch2[0].arguments.values[0].key = AnnotationShorthand.comment.name();
		batch2[0].arguments.values[0].value = "comment 1";

		M3BatchResponse resp2 = handler.m3Batch(uid, intention, packetId, batch2, true);
		assertEquals(resp2.message, M3BatchResponse.MESSAGE_TYPE_SUCCESS, resp2.messageType);
		
		JsonModel data3 = renderModel(modelId);
		JsonAnnotation[] annotations3 = data3.annotations;
		assertNotNull(annotations3);
		assertEquals(3, annotations3.length);
	}

	/**
	 * @param modelId
	 * @return data
	 */
	private JsonModel renderModel(final String modelId) {
		final ModelContainer model = models.getModel(modelId);
		final MolecularModelJsonRenderer renderer = OperationsTools.createModelRenderer(model, lookupService);
		JsonModel data = renderer.renderModel();
		return data;
	}

	@Test
	public void testMultipleMeta() throws Exception {
		models.setPathToOWLFiles(folder.newFolder().getCanonicalPath());
		models.dispose();
		
		M3Request[] requests = new M3Request[1];
		// get meta
		requests[0] = new M3Request();
		requests[0].entity = Entity.meta;
		requests[0].operation = Operation.get;
		
		M3BatchResponse response = handler.m3Batch(uid, intention, packetId, requests, true);
		assertEquals(uid, response.uid);
		assertEquals(intention, response.intention);
		assertEquals(M3BatchResponse.MESSAGE_TYPE_SUCCESS, response.messageType);
		final JsonRelationInfo[] relations = BatchTestTools.responseRelations(response);
		final OWLGraphWrapper tbox = models.getGraph();
		final OWLObjectProperty part_of = tbox.getOWLObjectPropertyByIdentifier("part_of");
		assertNotNull(part_of);
		final String partOfJsonId = IdStringManager.getId(part_of, tbox);
		boolean hasPartOf = false;
		for (JsonRelationInfo info : relations) {
			String id = info.id;
			assertNotNull(id);
			if (partOfJsonId.equals(id)) {
				assertEquals(true, info.relevant);
				hasPartOf = true;
			}
		}
		assertTrue(relations.length > 100);
		assertTrue(hasPartOf);

		final JsonEvidenceInfo[] evidences = BatchTestTools.responseEvidences(response);
		assertTrue(evidences.length > 100);
		
		final Set<String> modelIds = BatchTestTools.responseModelsIds(response);
		assertEquals(0, modelIds.size());
	}

	@Test
	public void testProteinNames() throws Exception {
		
		M3Request[] batch1 = new M3Request[1];
		batch1[0] = new M3Request();
		batch1[0].entity = Entity.model;
		batch1[0].operation = Operation.add;
		batch1[0].arguments = new M3Argument();
		
		M3BatchResponse response1 = handler.m3Batch(uid, intention, packetId, batch1, true);
		assertEquals(uid, response1.uid);
		assertEquals(intention, response1.intention);
		assertEquals(M3BatchResponse.MESSAGE_TYPE_SUCCESS, response1.messageType);
		final String modelId = BatchTestTools.responseId(response1);
		
		// check that protein id resolves to the expected label
		final String proteinId = "UniProtKB:F1NGQ9";
		final String proteinLabel = "FZD1";
		final String taxonId = "9031"; // TODO
		LookupEntry entry = lookupService.lookup(proteinId, taxonId);
		assertEquals(proteinLabel, entry.label);
		
		// try to generate a model with a protein and protein label
		M3Request[] batch2 = new M3Request[2];
		batch2[0] = BatchTestTools.addIndividual(modelId, "GO:0006915");
		batch2[0].arguments.assignToVariable = "i1";
		
		batch2[1] = new M3Request();
		batch2[1].entity = Entity.individual;
		batch2[1].operation = Operation.addType;
		batch2[1].arguments = new M3Argument();
		batch2[1].arguments.modelId = modelId;
		batch2[1].arguments.individual = "i1";
		batch2[1].arguments.expressions = new JsonOwlObject[1];
		batch2[1].arguments.expressions[0] = BatchTestTools.createSvf("RO:0002333", proteinId); // enabled_by
		
		M3BatchResponse response2 = handler.m3Batch(uid, intention, packetId, batch2, true);
		assertEquals(uid, response2.uid);
		assertEquals(intention, response2.intention);
		assertEquals(response2.message, M3BatchResponse.MESSAGE_TYPE_SUCCESS, response2.messageType);
		JsonOwlIndividual[] iObjs = BatchTestTools.responseIndividuals(response2);
		assertEquals(1, iObjs.length);
		JsonOwlIndividual individual = iObjs[0];
		JsonOwlObject[] typeObjs = individual.type;
		assertEquals(2, typeObjs.length);
		boolean found = false;
		for (JsonOwlObject typeObj : typeObjs) {
			if (JsonOwlObjectType.SomeValueFrom == typeObj.type) {
				found = true;
				JsonOwlObject filler = typeObj.filler;
				assertEquals(proteinId, filler.id);
				assertEquals(proteinLabel, filler.label);
			}
		}
		assertTrue(found);
	}

	@Test
	public void testAddBlankModel() throws Exception {
		models.dispose();
		
		M3Request[] batch1 = new M3Request[1];
		batch1[0] = new M3Request();
		batch1[0].entity = Entity.model;
		batch1[0].operation = Operation.add;
		batch1[0].arguments = new M3Argument();
		
		M3BatchResponse response1 = handler.m3Batch(uid, intention, packetId, batch1, true);
		assertEquals(uid, response1.uid);
		assertEquals(intention, response1.intention);
		assertEquals(M3BatchResponse.MESSAGE_TYPE_SUCCESS, response1.messageType);
		final String modelId1 = BatchTestTools.responseId(response1);
		
		M3Request[] batch2 = new M3Request[1];
		batch2[0] = new M3Request();
		batch2[0].entity = Entity.model;
		batch2[0].operation = Operation.add;
		batch2[0].arguments = new M3Argument();
		
		M3BatchResponse response2 = handler.m3Batch(uid, intention, packetId, batch2, true);
		assertEquals(uid, response2.uid);
		assertEquals(intention, response2.intention);
		assertEquals(response2.message, M3BatchResponse.MESSAGE_TYPE_SUCCESS, response2.messageType);
		final String modelId2 = BatchTestTools.responseId(response2);
		
		assertNotEquals(modelId1, modelId2);
		
		M3Request[] batch3 = new M3Request[1];
		batch3[0] = new M3Request();
		batch3[0].entity = Entity.model;
		batch3[0].operation = Operation.add;
		batch3[0].arguments = new M3Argument();
		
		M3BatchResponse response3 = handler.m3Batch(uid, intention, packetId, batch3, true);
		assertEquals(uid, response3.uid);
		assertEquals(intention, response3.intention);
		assertEquals(response3.message, M3BatchResponse.MESSAGE_TYPE_SUCCESS, response3.messageType);
		final String modelId3 = BatchTestTools.responseId(response3);
		
		assertNotEquals(modelId1, modelId3);
		assertNotEquals(modelId2, modelId3);
	}
	
	@Test
	public void testDelete() throws Exception {
		models.dispose();
		
		final String modelId = generateBlankModel();
		
		// create
		M3Request[] batch1 = new M3Request[1];
		batch1[0] = BatchTestTools.addIndividual(modelId, "GO:0008104", // protein localization
				BatchTestTools.createSvf("RO:0002333", "UniProtKB:P0000"), // enabled_by
				BatchTestTools.createSvf("BFO:0000050", "GO:0006915")); // part_of apoptotic process
		
		M3BatchResponse response1 = handler.m3Batch(uid, intention, packetId, batch1, true);
		assertEquals(uid, response1.uid);
		assertEquals(intention, response1.intention);
		assertEquals(response1.message, M3BatchResponse.MESSAGE_TYPE_SUCCESS, response1.messageType);
		
		JsonOwlIndividual[] iObjs1 = BatchTestTools.responseIndividuals(response1);
		assertEquals(1, iObjs1.length);
		JsonOwlIndividual individual1 = iObjs1[0];
		assertNotNull(individual1);
		assertNotNull(individual1.id);
		
		JsonOwlObject[] types1 = individual1.type;
		assertEquals(3, types1.length);
		String apopId = null;
		for(JsonOwlObject e : types1) {
			if (JsonOwlObjectType.SomeValueFrom == e.type) {
				if (e.filler.id.equals("GO:0006915")) {
					apopId = e.filler.id;
					break;
				}
			}
		}
		assertNotNull(apopId);
		
		// delete
		M3Request[] batch2 = new M3Request[1];
		batch2[0] = new M3Request();
		batch2[0].entity = Entity.individual;
		batch2[0].operation = Operation.removeType;
		batch2[0].arguments = new M3Argument();
		batch2[0].arguments.modelId = modelId;
		batch2[0].arguments.individual = individual1.id;
		
		batch2[0].arguments.expressions = new JsonOwlObject[1];
		batch2[0].arguments.expressions[0] = BatchTestTools.createSvf("BFO:0000050", apopId); // part_of
		
		M3BatchResponse response2 = handler.m3Batch(uid, intention, packetId, batch2, true);
		assertEquals(uid, response2.uid);
		assertEquals(intention, response2.intention);
		assertEquals(response2.message, M3BatchResponse.MESSAGE_TYPE_SUCCESS, response2.messageType);
		
		JsonOwlIndividual[] iObjs2 = BatchTestTools.responseIndividuals(response2);
		assertEquals(1, iObjs2.length);
		JsonOwlIndividual individual2 = iObjs2[0];
		assertNotNull(individual2);
		JsonOwlObject[] types2 = individual2.type;
		assertEquals(2, types2.length);
	}
	
	@Test
	public void testDeleteEvidenceIndividuals() throws Exception {
		models.dispose();
		final String modelId = generateBlankModel();
		
		// setup model
		// simple four individuals (mf, bp, 2 evidences) with a fact in between bp and mf 
		final List<M3Request> batch1 = new ArrayList<M3Request>();
		
		// evidence1
		M3Request r = BatchTestTools.addIndividual(modelId, "ECO:0000000"); // evidence from ECO
		r.arguments.assignToVariable = "evidence-var1";
		r.arguments.values = BatchTestTools.singleAnnotation(AnnotationShorthand.source, "PMID:000000");
		batch1.add(r);

		// evidence2
		r = BatchTestTools.addIndividual(modelId, "ECO:0000001"); // evidence from ECO
		r.arguments.assignToVariable = "evidence-var2";
		r.arguments.values = BatchTestTools.singleAnnotation(AnnotationShorthand.source, "PMID:000001");
		batch1.add(r);
		
		// evidence3
		r = BatchTestTools.addIndividual(modelId, "ECO:0000002"); // evidence from ECO
		r.arguments.assignToVariable = "evidence-var3";
		r.arguments.values = BatchTestTools.singleAnnotation(AnnotationShorthand.source, "PMID:000002");
		batch1.add(r);
		
		// activity/mf
		r = BatchTestTools.addIndividual(modelId, "GO:0003674"); // molecular function
		r.arguments.assignToVariable = "mf";
		r.arguments.values = BatchTestTools.singleAnnotation(AnnotationShorthand.evidence, "evidence-var1");
		batch1.add(r);

		// process
		r = BatchTestTools.addIndividual(modelId, "GO:0008150"); // biological process
		r.arguments.values = BatchTestTools.singleAnnotation(AnnotationShorthand.evidence, "evidence-var3");
		r.arguments.assignToVariable = "bp";
		batch1.add(r);

		// activity -> process
		r = BatchTestTools.addEdge(modelId, "mf", "BFO:0000050", "bp"); // part_of
		r.arguments.values = BatchTestTools.singleAnnotation(AnnotationShorthand.evidence, "evidence-var2");
		batch1.add(r); // part_of
		
		final M3BatchResponse response1 = handler.m3Batch(uid, intention, packetId, batch1.toArray(new M3Request[batch1.size()]), true);
		assertEquals(uid, response1.uid);
		assertEquals(intention, response1.intention);
		assertEquals(response1.message, M3BatchResponse.MESSAGE_TYPE_SUCCESS, response1.messageType);
		
		// find individuals
		JsonOwlIndividual[] iObjs1 = BatchTestTools.responseIndividuals(response1);
		assertEquals(5, iObjs1.length);
		String evidence1 = null;
		String evidence2 = null;
		String evidence3 = null;
		String mf = null;
		String bp = null;
		for (JsonOwlIndividual iObj : iObjs1) {
			String id = iObj.id;
			assertNotNull(id);
			JsonOwlObject[] types = iObj.type;
			assertNotNull(types);
			assertEquals(1, types.length);
			JsonOwlObject typeObj = types[0];
			String typeId = typeObj.id;
			assertNotNull(typeId);
			if ("GO:0003674".equals(typeId)) {
				mf = id;
			}
			else if ("GO:0008150".equals(typeId)) {
				bp = id;
			}
			else if ("ECO:0000000".equals(typeId)) {
				evidence1 = id;
			}
			else if ("ECO:0000001".equals(typeId)) {
				evidence2 = id;
			}
			else if ("ECO:0000002".equals(typeId)) {
				evidence3 = id;
			}
		}
		assertNotNull(evidence1);
		assertNotNull(evidence2);
		assertNotNull(evidence3);
		assertNotNull(mf);
		assertNotNull(bp);
		
		// one edge
		JsonOwlFact[] facts1 = BatchTestTools.responseFacts(response1);
		assertEquals(1, facts1.length);
		assertEquals(3, facts1[0].annotations.length); // evidence, date, contributor
		
		// remove fact evidence
		final List<M3Request> batch2 = new ArrayList<M3Request>();
		r = BatchTestTools.removeIndividual(modelId, evidence2);
		batch2.add(r);
		
		final M3BatchResponse response2 = handler.m3Batch(uid, intention, packetId, batch2.toArray(new M3Request[batch2.size()]), true);
		assertEquals(uid, response2.uid);
		assertEquals(intention, response2.intention);
		assertEquals(response2.message, M3BatchResponse.MESSAGE_TYPE_SUCCESS, response2.messageType);
		
		final M3BatchResponse response3 = checkCounts(modelId, 4, 1);
		JsonOwlFact[] factsObjs = BatchTestTools.responseFacts(response3);
		assertEquals(2, factsObjs[0].annotations.length); // date and contributor remain
		
		// delete bp evidence instance
		final List<M3Request> batch4 = new ArrayList<M3Request>();
		r = BatchTestTools.removeIndividual(modelId, evidence3);
		batch4.add(r);
		
		final M3BatchResponse response4 = handler.m3Batch(uid, intention, packetId, batch4.toArray(new M3Request[batch4.size()]), true);
		assertEquals(uid, response4.uid);
		assertEquals(intention, response4.intention);
		assertEquals(response4.message, M3BatchResponse.MESSAGE_TYPE_SUCCESS, response4.messageType);
		
		final M3BatchResponse response5 = checkCounts(modelId, 3, 1);
		JsonOwlIndividual[] indivdualObjs5 = BatchTestTools.responseIndividuals(response5);
		boolean found = false;
		for (JsonOwlIndividual iObj : indivdualObjs5) {
			String id = iObj.id;
			assertNotNull(id);
			JsonOwlObject[] types = iObj.type;
			assertNotNull(types);
			assertEquals(1, types.length);
			JsonOwlObject typeObj = types[0];
			String typeId = typeObj.id;
			assertNotNull(typeId);
			if ("GO:0008150".equals(typeId)) {
				found = true;
				assertTrue(iObj.annotations.length == 2); // date and contributor remain
			}
		}
		assertTrue(found);
		
		
		// delete mf instance -> delete also mf evidence instance and fact
		final List<M3Request> batch6 = new ArrayList<M3Request>();
		r = BatchTestTools.removeIndividual(modelId, mf);
		batch6.add(r);
		
		final M3BatchResponse response6 = handler.m3Batch(uid, intention, packetId, batch6.toArray(new M3Request[batch6.size()]), true);
		assertEquals(uid, response6.uid);
		assertEquals(intention, response6.intention);
		assertEquals(response6.message, M3BatchResponse.MESSAGE_TYPE_SUCCESS, response6.messageType);
		
		M3BatchResponse response7 = checkCounts(modelId, 1, 0);
		JsonOwlIndividual[] iObjs7 = BatchTestTools.responseIndividuals(response7);
		assertEquals(bp, iObjs7[0].id); 
	}
	
	@Test
	public void testInconsistentModel() throws Exception {
		models.dispose();
		
		final String modelId = generateBlankModel();
		
		// create
		M3Request[] batch1 = new M3Request[1];
		batch1[0] = BatchTestTools.addIndividual(modelId, "GO:0009653", // anatomical structure morphogenesis
				BatchTestTools.createClass("GO:0048856")); // anatomical structure development
		
		M3BatchResponse response1 = handler.m3Batch(uid, intention, packetId, batch1, true);
		assertEquals(uid, response1.uid);
		assertEquals(intention, response1.intention);
		assertEquals(response1.message, M3BatchResponse.MESSAGE_TYPE_SUCCESS, response1.messageType);
		Boolean inconsistentFlag = BatchTestTools.responseInconsistent(response1);
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
		batch1[0] = BatchTestTools.addIndividual(modelId, "GO:0000902", // cell morphogenesis
				BatchTestTools.createClass("GO:0009826")); // unidimensional cell growth

		M3BatchResponse response1 = handler.m3Batch(uid, intention, packetId, batch1, true);
		assertEquals(uid, response1.uid);
		assertEquals(intention, response1.intention);
		assertEquals(response1.message, M3BatchResponse.MESSAGE_TYPE_SUCCESS, response1.messageType);
		assertNull("Model should not be inconsistent", BatchTestTools.responseInconsistent(response1));
		JsonOwlIndividual[] inferred = BatchTestTools.responseInferences(response1);
		assertNotNull(inferred);
		assertEquals(1, inferred.length);
		JsonOwlIndividual inferredData = inferred[0];
		JsonOwlObject[] types = inferredData.type;
		assertEquals(1, types.length);
		JsonOwlObject type = types[0];
		assertEquals(JsonOwlObjectType.Class, type.type);
		assertEquals("GO:0009826", type.id);
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
		batch1[0] = BatchTestTools.addIndividual(modelId, "GO:0051231", // spindle elongation
				BatchTestTools.createSvf("BFO:0000050", "GO:0000278")); // part_of, mitotic cell cycle

		M3BatchResponse response1 = handler.m3Batch(uid, intention, packetId, batch1, true);
		assertEquals(uid, response1.uid);
		assertEquals(intention, response1.intention);
		assertEquals(response1.message, M3BatchResponse.MESSAGE_TYPE_SUCCESS, response1.messageType);
		assertNull("Model should not be inconsistent", BatchTestTools.responseInconsistent(response1));
		JsonOwlIndividual[] inferred = BatchTestTools.responseInferences(response1);
		assertNotNull(inferred);
		assertEquals(1, inferred.length);
		JsonOwlIndividual inferredData = inferred[0];
		JsonOwlObject[] types = inferredData.type;
		assertEquals(1, types.length);
		JsonOwlObject type = types[0];
		assertEquals(JsonOwlObjectType.Class, type.type);
		assertEquals("GO:0000022", type.id);
	}
	
	@Test
	public void testValidationBeforeSave() throws Exception {
		assertTrue(JsonOrJsonpBatchHandler.VALIDATE_BEFORE_SAVE);
		models.dispose();
		
		final String modelId = generateBlankModel();
		
		// try to save
		M3Request[] batch = new M3Request[1];
		batch[0] = new M3Request();
		batch[0].entity = Entity.model;
		batch[0].operation = Operation.storeModel;
		batch[0].arguments = new M3Argument();
		batch[0].arguments.modelId = modelId;
		M3BatchResponse resp1 = handler.m3Batch(uid, intention, packetId, batch, true);
		assertEquals("This operation must fail as the model has no title or individuals", M3BatchResponse.MESSAGE_TYPE_ERROR, resp1.messageType);
		assertNotNull(resp1.commentary);
		assertTrue(resp1.commentary.contains("title"));
	}
	
	@Test
	public void testPrivileged() throws Exception {
		M3Request[] batch = new M3Request[1];
		batch[0] = new M3Request();
		batch[0].entity = Entity.model;
		batch[0].operation = Operation.add;
		M3BatchResponse resp1 = handler.m3Batch(uid, intention, packetId, batch, false);
		assertEquals(M3BatchResponse.MESSAGE_TYPE_ERROR, resp1.messageType);
		assertTrue(resp1.message.contains("Insufficient"));
	}
	
	@Test
	public void testExportLegacy() throws Exception {
		final String modelId = generateBlankModel();
		
		// create
		M3Request[] batch1 = new M3Request[1];
		batch1[0] = BatchTestTools.addIndividual(modelId, "GO:0008104", // protein localization
				BatchTestTools.createSvf("RO:0002333", "UniProtKB:P0000"), // enabled_by
				BatchTestTools.createSvf("BFO:0000050", "GO:0006915")); // part_of
		
		M3BatchResponse response1 = handler.m3Batch(uid, intention, packetId, batch1, true);
		assertEquals(uid, response1.uid);
		assertEquals(intention, response1.intention);
		assertEquals(response1.message, M3BatchResponse.MESSAGE_TYPE_SUCCESS, response1.messageType);
		
		
		M3Request[] batch2 = new M3Request[1];
		batch2[0] = new M3Request();
		batch2[0].operation = Operation.exportModelLegacy;
		batch2[0].entity = Entity.model;
		batch2[0].arguments = new M3Argument();
		batch2[0].arguments.modelId = modelId;
//		batch2[0].arguments.format = "gpad"; // optional, default is gaf 
		
		M3BatchResponse response2 = handler.m3Batch(uid, intention, packetId, batch2, true);
		assertEquals(uid, response2.uid);
		assertEquals(intention, response2.intention);
		assertEquals(response2.message, M3BatchResponse.MESSAGE_TYPE_SUCCESS, response2.messageType);
		String exportString = BatchTestTools.responseExport(response2);
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
		batch1[0] = BatchTestTools.addIndividual(modelId, "GO:0008104", // protein localization
				BatchTestTools.createSvf("RO:0002333", "UniProtKB:P0000"), // enabled_by
				BatchTestTools.createSvf("BFO:0000050", "GO:0006915")); // part_of apoptotic process

		M3BatchResponse response1 = handler.m3Batch(uid, intention, packetId, batch1, true);
		assertEquals(uid, response1.uid);
		assertEquals(intention, response1.intention);
		assertEquals(response1.message, M3BatchResponse.MESSAGE_TYPE_SUCCESS, response1.messageType);


		JsonOwlIndividual[] iObjs1 = BatchTestTools.responseIndividuals(response1);
		assertEquals(1, iObjs1.length);
		JsonOwlIndividual individual1 = iObjs1[0];
		assertNotNull(individual1);
		final String individualId = individual1.id;
		assertNotNull(individualId);

		JsonOwlObject[] types1 = individual1.type;
		assertEquals(3, types1.length);
		String apopId = null;
		for(JsonOwlObject e : types1) {
			if (JsonOwlObjectType.SomeValueFrom == e.type) {
				if (e.filler.id.equals("GO:0006915")) {
					apopId = e.filler.id;
					break;
				}
			}
		}
		assertNotNull(apopId);
		
		// check undo redo list
		M3Request[] batch2 = new M3Request[1];
		batch2[0] = new M3Request();
		batch2[0].entity = Entity.model;
		batch2[0].operation = Operation.getUndoRedo;
		batch2[0].arguments = new M3Argument();
		batch2[0].arguments.modelId = modelId;
		M3BatchResponse response2 = handler.m3Batch(uid, intention, packetId, batch2, true);
		assertEquals(uid, response2.uid);
		assertEquals(intention, response2.intention);
		assertEquals(response2.message, M3BatchResponse.MESSAGE_TYPE_SUCCESS, response2.messageType);
		List<Object> undo2 = (List<Object>) response2.data.undo;
		List<Object> redo2 = (List<Object>) response2.data.redo;
		assertTrue(undo2.size() > 1);
		assertTrue(redo2.isEmpty());

		// delete
		M3Request[] batch3 = new M3Request[1];
		batch3[0] = new M3Request();
		batch3[0].entity = Entity.individual;
		batch3[0].operation = Operation.removeType;
		batch3[0].arguments = new M3Argument();
		batch3[0].arguments.modelId = modelId;
		batch3[0].arguments.individual = individualId;
		batch3[0].arguments.expressions = new JsonOwlObject[]{ BatchTestTools.createSvf("BFO:0000050", apopId)};

		M3BatchResponse response3 = handler.m3Batch(uid, intention, packetId, batch3, true);
		assertEquals(uid, response3.uid);
		assertEquals(intention, response3.intention);
		assertEquals(response3.message, M3BatchResponse.MESSAGE_TYPE_SUCCESS, response3.messageType);

		JsonOwlIndividual[] iObjs3 = BatchTestTools.responseIndividuals(response3);
		assertEquals(1, iObjs3.length);
		JsonOwlIndividual individual3 = iObjs3[0];
		assertNotNull(individual3);
		JsonOwlObject[] types3 = individual3.type;
		assertEquals(2, types3.length);
		
		// check undo redo list
		M3Request[] batch4 = new M3Request[1];
		batch4[0] = new M3Request();
		batch4[0].entity = Entity.model;
		batch4[0].operation = Operation.getUndoRedo;
		batch4[0].arguments = new M3Argument();
		batch4[0].arguments.modelId = modelId;
		
		M3BatchResponse response4 = handler.m3Batch(uid, intention, packetId, batch4, true);
		assertEquals(uid, response4.uid);
		assertEquals(intention, response4.intention);
		assertEquals(response4.message, M3BatchResponse.MESSAGE_TYPE_SUCCESS, response4.messageType);
		List<Object> undo4 = (List<Object>) response4.data.undo;
		List<Object> redo4 = (List<Object>) response4.data.redo;
		assertTrue(undo4.size() > 1);
		assertTrue(redo4.isEmpty());
		
		// undo
		M3Request[] batch5 = new M3Request[1];
		batch5[0] = new M3Request();
		batch5[0].entity = Entity.model;
		batch5[0].operation = Operation.undo;
		batch5[0].arguments = new M3Argument();
		batch5[0].arguments.modelId = modelId;
		
		M3BatchResponse response5 = handler.m3Batch(uid, intention, packetId, batch5, true);
		assertEquals(uid, response5.uid);
		assertEquals(intention, response5.intention);
		assertEquals(response5.message, M3BatchResponse.MESSAGE_TYPE_SUCCESS, response5.messageType);

		
		// check undo redo list
		M3Request[] batch6 = new M3Request[1];
		batch6[0] = new M3Request();
		batch6[0].entity = Entity.model;
		batch6[0].operation = Operation.getUndoRedo;
		batch6[0].arguments = new M3Argument();
		batch6[0].arguments.modelId = modelId;
		
		M3BatchResponse response6 = handler.m3Batch(uid, intention, packetId, batch6, true);
		assertEquals(uid, response6.uid);
		assertEquals(intention, response6.intention);
		assertEquals(response6.message, M3BatchResponse.MESSAGE_TYPE_SUCCESS, response6.messageType);
		List<Object> undo6 = (List<Object>) response6.data.undo;
		List<Object> redo6 = (List<Object>) response6.data.redo;
		assertTrue(undo6.size() > 1);
		assertTrue(redo6.size() == 1);
		
	}

	@Test
	public void testAllIndividualEvidenceDelete() throws Exception {
		/*
		 * create three individuals, two facts and two evidence individuals
		 */
		// blank model
		final String modelId = generateBlankModel();
		final List<M3Request> batch1 = new ArrayList<M3Request>();
		
		// evidence1
		M3Request r = BatchTestTools.addIndividual(modelId, "ECO:0000000"); // evidence from ECO
		r.arguments.assignToVariable = "evidence-var1";
		r.arguments.values = BatchTestTools.singleAnnotation(AnnotationShorthand.source, "PMID:000000");
		batch1.add(r);

		// evidence2
		r = BatchTestTools.addIndividual(modelId, "ECO:0000001"); // evidence from ECO
		r.arguments.assignToVariable = "evidence-var2";
		r.arguments.values = BatchTestTools.singleAnnotation(AnnotationShorthand.source, "PMID:000001");
		batch1.add(r);

		// activity/mf
		r = BatchTestTools.addIndividual(modelId, "GO:0003674"); // molecular function
		r.arguments.assignToVariable = "mf";
		batch1.add(r);

		// process
		r = BatchTestTools.addIndividual(modelId, "GO:0008150"); // biological process
		r.arguments.assignToVariable = "bp";
		batch1.add(r);

		// location/cc
		r = BatchTestTools.addIndividual(modelId, "GO:0005575"); // cellular component
		r.arguments.assignToVariable = "cc";
		batch1.add(r);

		// activity -> process
		r = BatchTestTools.addEdge(modelId, "mf", "BFO:0000050", "bp"); // part_of
		r.arguments.values = BatchTestTools.singleAnnotation(AnnotationShorthand.evidence, "evidence-var1");
		batch1.add(r); // part_of
		
		// activity -> cc
		r = BatchTestTools.addEdge(modelId, "mf", "BFO:0000066", "cc"); // occurs_in
		r.arguments.values = BatchTestTools.singleAnnotation(AnnotationShorthand.evidence, "evidence-var2");
		batch1.add(r);
		
		final M3BatchResponse response1 = handler.m3Batch(uid, intention, packetId, batch1.toArray(new M3Request[batch1.size()]), true);
		assertEquals(uid, response1.uid);
		assertEquals(intention, response1.intention);
		assertEquals(response1.message, M3BatchResponse.MESSAGE_TYPE_SUCCESS, response1.messageType);
		
		// find individuals
		JsonOwlIndividual[] iObjs1 = BatchTestTools.responseIndividuals(response1);
		assertEquals(5, iObjs1.length);
		String evidence1 = null;
		String evidence2 = null;
		String mf = null;
		String bp = null;
		String cc = null;
		for (JsonOwlIndividual iObj : iObjs1) {
			String id = iObj.id;
			assertNotNull(id);
			JsonOwlObject[] types = iObj.type;
			assertNotNull(types);
			assertEquals(1, types.length);
			JsonOwlObject typeObj = types[0];
			String typeId = typeObj.id;
			assertNotNull(typeId);
			if ("GO:0003674".equals(typeId)) {
				mf = id;
			}
			else if ("GO:0008150".equals(typeId)) {
				bp = id;
			}
			else if ("GO:0005575".equals(typeId)) {
				cc = id;
			}
			else if ("ECO:0000000".equals(typeId)) {
				evidence1 = id;
			}
			else if ("ECO:0000001".equals(typeId)) {
				evidence2 = id;
			}
		}
		assertNotNull(evidence1);
		assertNotNull(evidence2);
		assertNotNull(mf);
		assertNotNull(bp);
		assertNotNull(cc);
		
		// two edges
		JsonOwlFact[] facts1 = BatchTestTools.responseFacts(response1);
		assertEquals(2, facts1.length);
		
		/*
		 * delete one fact and expect that the associated evidence is also deleted
		 */
		// delete: mf -part_of-> bp 
		r = BatchTestTools.deleteEdge(modelId, mf, "BFO:0000050", bp);
		M3BatchResponse response2 = handler.m3Batch(uid, intention, packetId, new M3Request[]{r}, true);
		assertEquals(uid, response2.uid);
		assertEquals(intention, response2.intention);
		assertEquals(response2.message, M3BatchResponse.MESSAGE_TYPE_SUCCESS, response2.messageType);
		
		JsonOwlIndividual[] iObjs2 = BatchTestTools.responseIndividuals(response2);
		assertEquals(2, iObjs2.length); // should return the two individuals affected
		
		// get the whole model to check global counts
		checkCounts(modelId, 4, 1);
		
		/*
		 * delete one individuals of an fact and expect a cascading delete, including the evidence
		 */
		r = BatchTestTools.removeIndividual(modelId, cc);
		M3BatchResponse response3 = handler.m3Batch(uid, intention, packetId, new M3Request[]{r}, true);
		assertEquals(uid, response3.uid);
		assertEquals(intention, response3.intention);
		assertEquals(response3.message, M3BatchResponse.MESSAGE_TYPE_SUCCESS, response3.messageType);
		
		JsonOwlIndividual[] iObjs3 = BatchTestTools.responseIndividuals(response3);
		assertEquals(2, iObjs3.length);
		JsonOwlFact[] facts3 = BatchTestTools.responseFacts(response3);
		assertEquals(0, facts3.length);
		
		checkCounts(modelId, 2, 0);
	}
	
	private M3BatchResponse checkCounts(String modelId, int individuals, int facts) {
		M3Request r = new M3Request();
		r.entity = Entity.model;
		r.operation = Operation.get;
		r.arguments = new M3Argument();
		r.arguments.modelId = modelId;
		final M3BatchResponse response = handler.m3Batch(uid, intention, packetId, new M3Request[]{r }, true);
		assertEquals(uid, response.uid);
		assertEquals(intention, response.intention);
		assertEquals(response.message, M3BatchResponse.MESSAGE_TYPE_SUCCESS, response.messageType);
		JsonOwlIndividual[] iObjs = BatchTestTools.responseIndividuals(response);
		assertEquals(individuals, iObjs.length);
		JsonOwlFact[] factsObjs = BatchTestTools.responseFacts(response);
		assertEquals(facts, factsObjs.length);
		return response;
	}
	
	@Test
	public void testAllIndividualUseCase() throws Exception {
		/*
		 * Create a full set of individuals for an activity diagram of a gene.
		 */
		// blank model
		final String modelId = generateBlankModel();
		List<M3Request> batch = new ArrayList<M3Request>();
		
		// evidence
		M3Request r = BatchTestTools.addIndividual(modelId, "ECO:0000000"); // evidence from ECO
		r.arguments.assignToVariable = "evidence-var";
		r.arguments.values = BatchTestTools.singleAnnotation(AnnotationShorthand.source, "PMID:000000");
		batch.add(r);
		
		// activity/mf
		r = BatchTestTools.addIndividual(modelId, "GO:0003674"); // molecular function
		r.arguments.assignToVariable = "mf";
		batch.add(r);

		// process
		r = BatchTestTools.addIndividual(modelId, "GO:0008150"); // biological process
		r.arguments.assignToVariable = "bp";
		batch.add(r);

		// location/cc
		r = BatchTestTools.addIndividual(modelId, "GO:0005575"); // cellular component
		r.arguments.assignToVariable = "cc";
		batch.add(r);

		// gene
		r = BatchTestTools.addIndividual(modelId, "MGI:000000"); // fake gene (not in the test set of known genes!)
		r.arguments.assignToVariable = "gene";
		batch.add(r);
		
		// relations
		// activity -> gene
		r = BatchTestTools.addEdge(modelId, "mf", "RO:0002333", "gene"); // enabled_by
		r.arguments.values = BatchTestTools.singleAnnotation(AnnotationShorthand.evidence, "evidence-var");
		batch.add(r); 
		
		// activity -> process
		r = BatchTestTools.addEdge(modelId, "mf", "BFO:0000050", "bp"); // part_of
		r.arguments.values = BatchTestTools.singleAnnotation(AnnotationShorthand.evidence, "evidence-var");
		batch.add(r); // part_of
		
		// activity -> cc
		r = BatchTestTools.addEdge(modelId, "mf", "BFO:0000066", "cc"); // occurs_in
		r.arguments.values = BatchTestTools.singleAnnotation(AnnotationShorthand.evidence, "evidence-var");
		batch.add(r);
		
		/*
		 * Test for annoying work-around until the external validation is more stable
		 */
		boolean defaultIdPolicy = handler.CHECK_LITERAL_IDENTIFIERS;
		M3BatchResponse response;
		try {
			handler.CHECK_LITERAL_IDENTIFIERS = false;
			response = handler.m3Batch(uid, intention, packetId, batch.toArray(new M3Request[batch.size()]), true);
		}
		finally {
			handler.CHECK_LITERAL_IDENTIFIERS = defaultIdPolicy;
		}
		assertEquals(uid, response.uid);
		assertEquals(intention, response.intention);
		assertEquals(response.message, M3BatchResponse.MESSAGE_TYPE_SUCCESS, response.messageType);
		
		
	}
	
	@Test
	public void testVariables1() throws Exception {
		/*
		 * TASK: create three individuals (mf,bp,cc) and a directed relation
		 * between the new instances
		 */
		final String modelId = generateBlankModel();
		final M3Request[] batch = new M3Request[5];
		batch[0] = new M3Request();
		batch[0].entity = Entity.individual;
		batch[0].operation = Operation.add;
		batch[0].arguments = new M3Argument();
		batch[0].arguments.modelId = modelId;
		BatchTestTools.setExpressionClass(batch[0].arguments, "GO:0003674"); // molecular function
		batch[0].arguments.assignToVariable = "mf";

		batch[1] = new M3Request();
		batch[1].entity = Entity.individual;
		batch[1].operation = Operation.add;
		batch[1].arguments = new M3Argument();
		batch[1].arguments.modelId = modelId;
		BatchTestTools.setExpressionClass(batch[1].arguments, "GO:0008150"); // biological process
		batch[1].arguments.assignToVariable = "bp";

		batch[2] = new M3Request();
		batch[2].entity = Entity.edge;
		batch[2].operation = Operation.add;
		batch[2].arguments = new M3Argument();
		batch[2].arguments.modelId = modelId;
		batch[2].arguments.subject = "mf";
		batch[2].arguments.predicate = "BFO:0000050"; // part_of
		batch[2].arguments.object = "bp";

		batch[3] = new M3Request();
		batch[3].entity = Entity.individual;
		batch[3].operation = Operation.add;
		batch[3].arguments = new M3Argument();
		batch[3].arguments.modelId = modelId;
		BatchTestTools.setExpressionClass(batch[3].arguments, "GO:0005575"); // cellular component
		batch[3].arguments.assignToVariable = "cc";

		batch[4] = new M3Request();
		batch[4].entity = Entity.edge;
		batch[4].operation = Operation.add;
		batch[4].arguments = new M3Argument();
		batch[4].arguments.modelId = modelId;
		batch[4].arguments.subject = "mf";
		batch[4].arguments.predicate = "BFO:0000066"; // occurs_in
		batch[4].arguments.object = "cc";

		M3BatchResponse response = handler.m3Batch(uid, intention, packetId, batch, true);
		assertEquals(uid, response.uid);
		assertEquals(intention, response.intention);
		assertEquals(response.message, M3BatchResponse.MESSAGE_TYPE_SUCCESS, response.messageType);

		JsonOwlIndividual[] iObjs = BatchTestTools.responseIndividuals(response);
		assertEquals(3, iObjs.length);
		String mf = null;
		String bp = null;
		String cc = null;
		for (JsonOwlIndividual iObj : iObjs) {
			String id = iObj.id;
			assertNotNull(id);
			JsonOwlObject[] types = iObj.type;
			assertNotNull(types);
			assertEquals(1, types.length);
			JsonOwlObject typeObj = types[0];
			String typeId = typeObj.id;
			assertNotNull(typeId);
			if ("GO:0003674".equals(typeId)) {
				mf = id;
			}
			else if ("GO:0008150".equals(typeId)) {
				bp = id;
			}
			else if ("GO:0005575".equals(typeId)) {
				cc = id;
			}
		}
		assertNotNull(mf);
		assertNotNull(bp);
		assertNotNull(cc);
		
		JsonOwlFact[] facts = BatchTestTools.responseFacts(response);
		assertEquals(2, facts.length);
		boolean mfbp = false;
		boolean mfcc = false;
		for (JsonOwlFact fact : facts) {
			String subject = fact.subject;
			String property = fact.property;
			String object = fact.object;
			assertNotNull(subject);
			assertNotNull(property);
			assertNotNull(object);
			if (mf.equals(subject) && "BFO:0000050".equals(property) && bp.equals(object)) {
				mfbp = true;
			}
			if (mf.equals(subject) && "BFO:0000066".equals(property) && cc.equals(object)) {
				mfcc = true;
			}
		}
		assertTrue(mfbp);
		assertTrue(mfcc);
	}

	@Test
	public void testVariables2() throws Exception {
		/*
		 * TASK: try to use an undefined variable
		 */
		final String modelId = generateBlankModel();
		final M3Request[] batch = new M3Request[2];
		batch[0] = new M3Request();
		batch[0].entity = Entity.individual;
		batch[0].operation = Operation.add;
		batch[0].arguments = new M3Argument();
		batch[0].arguments.modelId = modelId;
		BatchTestTools.setExpressionClass(batch[0].arguments, "GO:0003674"); // molecular function
		batch[0].arguments.assignToVariable = "mf";

		batch[1] = new M3Request();
		batch[1].entity = Entity.edge;
		batch[1].operation = Operation.add;
		batch[1].arguments = new M3Argument();
		batch[1].arguments.modelId = modelId;
		batch[1].arguments.subject = "mf";
		batch[1].arguments.predicate = "BFO:0000050"; // part_of
		batch[1].arguments.object = "foo";

		M3BatchResponse response = handler.m3Batch(uid, intention, packetId, batch, true);
		assertEquals(uid, response.uid);
		assertEquals(intention, response.intention);
		assertEquals("The operation should fail with an unknown identifier exception", 
				M3BatchResponse.MESSAGE_TYPE_ERROR, response.messageType);
		assertTrue(response.message, response.message.contains("UnknownIdentifierException"));
		assertTrue(response.message, response.message.contains("foo")); // unknown
	}
	
	@Test
	public void testDeprecatedModel() throws Exception {
		models.setPathToOWLFiles(folder.newFolder().getCanonicalPath());
		models.dispose();
		
		final String modelId1 = generateBlankModel();
		final String modelId2 = generateBlankModel();
		
		// add deprecated annotation to model 2
		final M3Request[] batch1 = new M3Request[]{new M3Request()};
		batch1[0].entity = Entity.model;
		batch1[0].operation = Operation.addAnnotation;
		batch1[0].arguments = new M3Argument();
		batch1[0].arguments.modelId = modelId2;
		batch1[0].arguments.values = new JsonAnnotation[1];
		batch1[0].arguments.values[0] = new JsonAnnotation();
		batch1[0].arguments.values[0].key = AnnotationShorthand.deprecated.name();
		batch1[0].arguments.values[0].value = Boolean.TRUE.toString();
		
		M3BatchResponse response1 = handler.m3Batch(uid, intention, packetId, batch1, true);
		assertEquals(uid, response1.uid);
		assertEquals(intention, response1.intention);
		assertEquals(response1.message, M3BatchResponse.MESSAGE_TYPE_SUCCESS, response1.messageType);
		
		
		final M3Request[] batch2 = new M3Request[]{new M3Request()};
		batch2[0].entity = Entity.meta;
		batch2[0].operation = Operation.get;
		
		M3BatchResponse response2 = handler.m3Batch(uid, intention, packetId, batch2, true);
		assertEquals(uid, response2.uid);
		assertEquals(intention, response2.intention);
		assertEquals(response2.message, M3BatchResponse.MESSAGE_TYPE_SUCCESS, response2.messageType);
		
		Map<String,Map<String,String>> map = BatchTestTools.responseModelsMeta(response2);
		assertEquals(2, map.size());
		// model 1
		Map<String, String> modelData = map.get(modelId1);
		assertNotNull(modelData);
		assertFalse(modelData.containsKey(AnnotationShorthand.deprecated.name()));
		
		// model 2, deprecated
		modelData = map.get(modelId2);
		assertNotNull(modelData);
		assertTrue(modelData.containsKey(AnnotationShorthand.deprecated.name()));
		assertEquals("true", modelData.get(AnnotationShorthand.deprecated.name()));
	}
	
	@Test
	public void testAutoAnnotationsForAddType() throws Exception {
		/*
		 * test that if a type is added or removed from an individual also
		 * updates the contributes annotation
		 */
		final String modelId = generateBlankModel();
		// create individual
		final M3Request[] batch1 = new M3Request[1];
		batch1[0] = new M3Request();
		batch1[0].entity = Entity.individual;
		batch1[0].operation = Operation.add;
		batch1[0].arguments = new M3Argument();
		batch1[0].arguments.modelId = modelId;
		BatchTestTools.setExpressionClass(batch1[0].arguments, "GO:0003674"); // molecular function
		
		String uid1 = "1";
		M3BatchResponse response1 = handler.m3Batch(uid1, intention, packetId, batch1, true);
		assertEquals(uid1, response1.uid);
		assertEquals(intention, response1.intention);
		assertEquals(response1.message, M3BatchResponse.MESSAGE_TYPE_SUCCESS, response1.messageType);
		
		// find contributor
		JsonOwlIndividual[] individuals1 = BatchTestTools.responseIndividuals(response1);
		assertEquals(1, individuals1.length);
		
		final String id = individuals1[0].id;
		
		JsonAnnotation[] annotations1 = individuals1[0].annotations;
		assertEquals(2, annotations1.length);
		String contrib1 = null;
		for (JsonAnnotation annotation : annotations1) {
			if (AnnotationShorthand.contributor.name().equals(annotation.key)) {
				contrib1 = annotation.value;
			}
		}
		assertNotNull(contrib1);
		assertEquals(uid1, contrib1);
		
		// remove type
		final M3Request[] batch2 = new M3Request[1];
		batch2[0] = new M3Request();
		batch2[0].entity = Entity.individual;
		batch2[0].operation = Operation.removeType;
		batch2[0].arguments = new M3Argument();
		batch2[0].arguments.modelId = modelId;
		batch2[0].arguments.individual = id;
		BatchTestTools.setExpressionClass(batch2[0].arguments, "GO:0003674"); // molecular function
		
		String uid2 = "2";
		M3BatchResponse response2 = handler.m3Batch(uid2, intention, packetId, batch2, true);
		assertEquals(uid2, response2.uid);
		assertEquals(intention, response2.intention);
		assertEquals(response2.message, M3BatchResponse.MESSAGE_TYPE_SUCCESS, response2.messageType);
		
		// find contributor and compare with prev
		JsonOwlIndividual[] individuals2 = BatchTestTools.responseIndividuals(response2);
		assertEquals(1, individuals2.length);
		
		JsonAnnotation[] annotations2 = individuals2[0].annotations;
		assertEquals(3, annotations2.length);
		Set<String> contribSet1 = new HashSet<String>();
		for (JsonAnnotation annotation : annotations2) {
			if (AnnotationShorthand.contributor.name().equals(annotation.key)) {
				contribSet1.add(annotation.value);
			}
		}
		assertEquals(2, contribSet1.size());
		assertTrue(contribSet1.contains(uid1));
		assertTrue(contribSet1.contains(uid2));
		
		// add type
		final M3Request[] batch3 = new M3Request[1];
		batch3[0] = new M3Request();
		batch3[0].entity = Entity.individual;
		batch3[0].operation = Operation.addType;
		batch3[0].arguments = new M3Argument();
		batch3[0].arguments.modelId = modelId;
		batch3[0].arguments.individual = id;
		BatchTestTools.setExpressionClass(batch3[0].arguments, "GO:0003674"); // molecular function
		
		String uid3 = "3";
		M3BatchResponse response3 = handler.m3Batch(uid3, intention, packetId, batch3, true);
		assertEquals(uid3, response3.uid);
		assertEquals(intention, response3.intention);
		assertEquals(response3.message, M3BatchResponse.MESSAGE_TYPE_SUCCESS, response3.messageType);
		
		// find contributor and compare with prev
		JsonOwlIndividual[] individuals3 = BatchTestTools.responseIndividuals(response3);
		assertEquals(1, individuals3.length);
		
		JsonAnnotation[] annotations3 = individuals3[0].annotations;
		assertEquals(4, annotations3.length);
		Set<String> contribSet2 = new HashSet<String>();
		for (JsonAnnotation annotation : annotations3) {
			if (AnnotationShorthand.contributor.name().equals(annotation.key)) {
				contribSet2.add(annotation.value);
			}
		}
		assertEquals(3, contribSet2.size());
		assertTrue(contribSet2.contains(uid1));
		assertTrue(contribSet2.contains(uid2));
		assertTrue(contribSet2.contains(uid3));
	}
	
	static class DateGenerator {
		
		boolean useCounter = false;
		int counter = 0;
	}
	
	@Test
	public void testUpdateDateAnnotation() throws Exception {
		/*
		 * test that the last modification date is update for every change of an
		 * individual or fact
		 */
		try {
			dateGenerator.counter = 0;
			dateGenerator.useCounter = true;
			
			// test update with add/remove annotation of a fact
			final String modelId = generateBlankModel();
			
			// setup initial fact with two individuals
			final M3Request[] batch1 = new M3Request[3];
			batch1[0] = new M3Request();
			batch1[0].entity = Entity.individual;
			batch1[0].operation = Operation.add;
			batch1[0].arguments = new M3Argument();
			batch1[0].arguments.modelId = modelId;
			BatchTestTools.setExpressionClass(batch1[0].arguments, "GO:0003674"); // molecular function
			batch1[0].arguments.assignToVariable = "mf";

			batch1[1] = new M3Request();
			batch1[1].entity = Entity.individual;
			batch1[1].operation = Operation.add;
			batch1[1].arguments = new M3Argument();
			batch1[1].arguments.modelId = modelId;
			BatchTestTools.setExpressionClass(batch1[1].arguments, "GO:0008150"); // biological process
			batch1[1].arguments.assignToVariable = "bp";

			batch1[2] = new M3Request();
			batch1[2].entity = Entity.edge;
			batch1[2].operation = Operation.add;
			batch1[2].arguments = new M3Argument();
			batch1[2].arguments.modelId = modelId;
			batch1[2].arguments.subject = "mf";
			batch1[2].arguments.predicate = "BFO:0000050"; // part_of
			batch1[2].arguments.object = "bp";
			
			M3BatchResponse response1 = handler.m3Batch(uid, intention, packetId, batch1, true);
			assertEquals(uid, response1.uid);
			assertEquals(intention, response1.intention);
			assertEquals(response1.message, M3BatchResponse.MESSAGE_TYPE_SUCCESS, response1.messageType);
			
			// find fact and date annotation
			String prevDate = null;
			{
				JsonOwlFact[] responseFacts = BatchTestTools.responseFacts(response1);
				assertEquals(1, responseFacts.length);
				Set<String> dates = new HashSet<String>();
				for(JsonAnnotation ann : responseFacts[0].annotations) {
					if (AnnotationShorthand.date.name().equals(ann.key)) {
						dates.add(ann.value);
					}
				}
				assertEquals(1, dates.size());
				prevDate = dates.iterator().next();
				assertNotNull(prevDate);
			}
			String mf = null;
			String bp = null;
			{
				JsonOwlIndividual[] responseIndividuals = BatchTestTools.responseIndividuals(response1);
				assertEquals(2, responseIndividuals.length);
				for (JsonOwlIndividual iObj : responseIndividuals) {
					String id = iObj.id;
					assertNotNull(id);
					JsonOwlObject[] types = iObj.type;
					assertNotNull(types);
					assertEquals(1, types.length);
					JsonOwlObject typeObj = types[0];
					String typeId = typeObj.id;
					assertNotNull(typeId);
					if ("GO:0003674".equals(typeId)) {
						mf = id;
					}
					else if ("GO:0008150".equals(typeId)) {
						bp = id;
					}
				}
			}
			assertNotNull(mf);
			assertNotNull(bp);
			
			// add comment to fact
			final M3Request[] batch2 = new M3Request[1];
			batch2[0] = new M3Request();
			batch2[0].entity = Entity.edge;
			batch2[0].operation = Operation.addAnnotation;
			batch2[0].arguments = new M3Argument();
			batch2[0].arguments.modelId = modelId;
			batch2[0].arguments.subject = mf;
			batch2[0].arguments.predicate = "BFO:0000050"; // part_of
			batch2[0].arguments.object = bp;
			batch2[0].arguments.values = BatchTestTools.singleAnnotation(AnnotationShorthand.comment, "foo");
			
			M3BatchResponse response2 = handler.m3Batch(uid, intention, packetId, batch2, true);
			assertEquals(uid, response2.uid);
			assertEquals(intention, response2.intention);
			assertEquals(response2.message, M3BatchResponse.MESSAGE_TYPE_SUCCESS, response2.messageType);
			
			// find fact and compare date with prev
			{
				JsonOwlFact[] responseFacts = BatchTestTools.responseFacts(response2);
				assertEquals(1, responseFacts.length);
				Set<String> dates = new HashSet<String>();
				for(JsonAnnotation ann : responseFacts[0].annotations) {
					if (AnnotationShorthand.date.name().equals(ann.key)) {
						dates.add(ann.value);
					}
				}
				assertEquals(1, dates.size());
				String currentDate = dates.iterator().next();
				assertNotNull(currentDate);
				assertNotEquals(prevDate, currentDate);
				prevDate = currentDate;
			}
			
			// remove comment from fact
			final M3Request[] batch3 = new M3Request[1];
			batch3[0] = new M3Request();
			batch3[0].entity = Entity.edge;
			batch3[0].operation = Operation.removeAnnotation;
			batch3[0].arguments = new M3Argument();
			batch3[0].arguments.modelId = modelId;
			batch3[0].arguments.subject = mf;
			batch3[0].arguments.predicate = "BFO:0000050"; // part_of
			batch3[0].arguments.object = bp;
			batch3[0].arguments.values = BatchTestTools.singleAnnotation(AnnotationShorthand.comment, "foo");
			
			M3BatchResponse response3 = handler.m3Batch(uid, intention, packetId, batch3, true);
			assertEquals(uid, response3.uid);
			assertEquals(intention, response3.intention);
			assertEquals(response3.message, M3BatchResponse.MESSAGE_TYPE_SUCCESS, response3.messageType);
			
			// find fact and compare date with prev
			{
				JsonOwlFact[] responseFacts = BatchTestTools.responseFacts(response3);
				assertEquals(1, responseFacts.length);
				Set<String> dates = new HashSet<String>();
				for(JsonAnnotation ann : responseFacts[0].annotations) {
					if (AnnotationShorthand.date.name().equals(ann.key)) {
						dates.add(ann.value);
					}
				}
				assertEquals(1, dates.size());
				String currentDate = dates.iterator().next();
				assertNotNull(currentDate);
				assertNotEquals(prevDate, currentDate);
				prevDate = currentDate;
			}
			
			
			// test update with add/remove type of an individual
			// find individual and date annotation
			
			String individualId = null;
			{
				JsonOwlIndividual[] individuals1 = BatchTestTools.responseIndividuals(response1);
				assertEquals(2, individuals1.length);
				final Set<String> dates = new HashSet<String>();
				for (JsonOwlIndividual individual : individuals1) {
					individualId = individual.id;
					assertNotNull(individualId);
					JsonOwlObject[] types = individual.type;
					assertNotNull(types);
					assertEquals(1, types.length);
					JsonOwlObject typeObj = types[0];
					String typeId = typeObj.id;
					assertNotNull(typeId);
					if ("GO:0003674".equals(typeId)) {
						for(JsonAnnotation annotation : individual.annotations) {
							if (AnnotationShorthand.date.name().equals(annotation.key)) {
								dates.add(annotation.value);
							}
						}
					}
				}
				assertEquals(1, dates.size());
				prevDate = dates.iterator().next();
				assertNotNull(prevDate);
			}
			
			// remove type
			final M3Request[] batch4 = new M3Request[1];
			batch4[0] = new M3Request();
			batch4[0].entity = Entity.individual;
			batch4[0].operation = Operation.removeType;
			batch4[0].arguments = new M3Argument();
			batch4[0].arguments.modelId = modelId;
			batch4[0].arguments.individual = individualId;
			BatchTestTools.setExpressionClass(batch4[0].arguments, "GO:0003674");
			
			M3BatchResponse response4 = handler.m3Batch(uid, intention, packetId, batch4, true);
			assertEquals(uid, response4.uid);
			assertEquals(intention, response4.intention);
			assertEquals(response4.message, M3BatchResponse.MESSAGE_TYPE_SUCCESS, response4.messageType);
			
			// find individual and compare date with prev
			{
				JsonOwlIndividual[] responseIndividuals = BatchTestTools.responseIndividuals(response4);
				assertEquals(1, responseIndividuals.length);
				final Set<String> dates = new HashSet<String>();
				for(JsonAnnotation annotation : responseIndividuals[0].annotations) {
					if (AnnotationShorthand.date.name().equals(annotation.key)) {
						dates.add(annotation.value);
					}
				}
				assertEquals(1, dates.size());
				String currentDate = dates.iterator().next();
				assertNotNull(currentDate);
				assertNotEquals(prevDate, currentDate);
				prevDate = currentDate;
			}
			
			// add type
			final M3Request[] batch5 = new M3Request[1];
			batch5[0] = new M3Request();
			batch5[0].entity = Entity.individual;
			batch5[0].operation = Operation.addType;
			batch5[0].arguments = new M3Argument();
			batch5[0].arguments.modelId = modelId;
			batch5[0].arguments.individual = individualId;
			BatchTestTools.setExpressionClass(batch5[0].arguments, "GO:0003674");
			
			M3BatchResponse response5 = handler.m3Batch(uid, intention, packetId, batch5, true);
			assertEquals(uid, response5.uid);
			assertEquals(intention, response5.intention);
			assertEquals(response5.message, M3BatchResponse.MESSAGE_TYPE_SUCCESS, response5.messageType);
			
			// find individual and compare date with prev
			{
				JsonOwlIndividual[] responseIndividuals = BatchTestTools.responseIndividuals(response5);
				assertEquals(1, responseIndividuals.length);
				final Set<String> dates = new HashSet<String>();
				for(JsonAnnotation annotation : responseIndividuals[0].annotations) {
					if (AnnotationShorthand.date.name().equals(annotation.key)) {
						dates.add(annotation.value);
					}
				}
				assertEquals(1, dates.size());
				assertEquals(1, dates.size());
				String currentDate = dates.iterator().next();
				assertNotNull(currentDate);
				assertNotEquals(prevDate, currentDate);
			}
		}
		finally {
			dateGenerator.useCounter = false;
		}
	}
	
	/**
	 * @return modelId
	 */
	private String generateBlankModel() {
		String modelId = BatchTestTools.generateBlankModel(handler);
		return modelId;
	}
}
