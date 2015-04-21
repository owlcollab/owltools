package owltools.gaf.lego.server.handler;

import static owltools.gaf.lego.server.handler.OperationsTools.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.geneontology.reasoner.ExpressionMaterializingReasoner;
import org.geneontology.reasoner.ExpressionMaterializingReasonerFactory;
import org.geneontology.reasoner.OWLExtendedReasonerFactory;
import org.glassfish.jersey.server.JSONP;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.OWLClass;

import owltools.annotation.lego.generate.GolrSeedingDataProvider;
import owltools.annotation.lego.generate.ModelSeeding;
import owltools.gaf.lego.MolecularModelManager.UnknownIdentifierException;
import owltools.gaf.lego.UndoAwareMolecularModelManager;
import owltools.gaf.lego.UndoAwareMolecularModelManager.UndoMetadata;
import owltools.gaf.lego.json.JsonModel;
import owltools.gaf.lego.json.MolecularModelJsonRenderer;
import owltools.gaf.lego.server.external.ExternalLookupService;
import owltools.gaf.lego.server.handler.M3SeedHandler.SeedResponse.SeedResponseData;
import owltools.graph.OWLGraphWrapper;
import owltools.util.ModelContainer;

public class JsonOrJsonpSeedHandler implements M3SeedHandler {

	public static final String JSONP_DEFAULT_CALLBACK = "jsonp";
	public static final String JSONP_DEFAULT_OVERWRITE = "json.wrf";
	
	private static final Logger logger = Logger.getLogger(JsonOrJsonpSeedHandler.class);
	
	private final UndoAwareMolecularModelManager m3;
	private final String golrUrl;
	private final OWLExtendedReasonerFactory<ExpressionMaterializingReasoner> factory;
	private final ExternalLookupService externalLookupService;
	
	public JsonOrJsonpSeedHandler(UndoAwareMolecularModelManager m3, String golr, ExternalLookupService externalLookupService) {
		this.m3 = m3;
		this.golrUrl = golr;
		this.externalLookupService = externalLookupService;
		factory = new ExpressionMaterializingReasonerFactory(new ElkReasonerFactory());
	}

	@Override
	@JSONP(callback = JSONP_DEFAULT_CALLBACK, queryParam = JSONP_DEFAULT_OVERWRITE)
	public SeedResponse fromProcessPost(String intention, String packetId, SeedRequest request) {
		// only privileged calls are allowed
		SeedResponse response = new SeedResponse(null, intention, packetId);
		return error(response, "Insufficient permissions for seed operation.", null);
	}

	@Override
	@JSONP(callback = JSONP_DEFAULT_CALLBACK, queryParam = JSONP_DEFAULT_OVERWRITE)
	public SeedResponse fromProcessPostPrivileged(String uid, String intention, String packetId, SeedRequest request) {
		return fromProcess(uid, intention, checkPacketId(packetId), request);
	}

	@Override
	@JSONP(callback = JSONP_DEFAULT_CALLBACK, queryParam = JSONP_DEFAULT_OVERWRITE)
	public SeedResponse fromProcessGet(String intention, String packetId, SeedRequest request) {
		// only privileged calls are allowed
		SeedResponse response = new SeedResponse(null, intention, packetId);
		return error(response, "Insufficient permissions for seed operation.", null);
	}

	@Override
	@JSONP(callback = JSONP_DEFAULT_CALLBACK, queryParam = JSONP_DEFAULT_OVERWRITE)
	public SeedResponse fromProcessGetPrivileged(String uid, String intention, String packetId, SeedRequest request) {
		return fromProcess(uid, intention, checkPacketId(packetId), request);
	}

	private static String checkPacketId(String packetId) {
		if (packetId == null) {
			packetId = PacketIdGenerator.generateId();
		}
		return packetId;
	}
	
	private SeedResponse fromProcess(String uid, String intention, String packetId, SeedRequest request) {
		SeedResponse response = new SeedResponse(uid, intention, packetId);
		try {
			requireNotNull(request, "The request may not be null.");
			uid = normalizeUserId(uid);
			UndoMetadata token = new UndoMetadata(uid);
			ModelContainer model = getModel(request.modelId);
			return seedFromProcess(request, request.modelId, model, response, token);
		} catch (Exception e) {
			return error(response, "Could not successfully handle batch request.", e);
		} catch (Throwable t) {
			logger.error("A critical error occured.", t);
			return error(response, "An internal error occured at the server level.", t);
		}
	}
	
	private ModelContainer getModel(String modelId) throws Exception {
		requireNotNull(modelId, "model id may not be null for seeding");
		final ModelContainer model = m3.getModel(modelId);
		if (model == null) {
			throw new UnknownIdentifierException("Could not retrieve a model for id: "+modelId);
		}
		return model;
	}
	
	private SeedResponse seedFromProcess(SeedRequest request, String modelId, ModelContainer model, SeedResponse response, UndoMetadata token) throws Exception {
		// check required fields
		requireNotNull(request.process, "");
		requireNotNull(request.taxon, "");
		
		// prepare seeder
		OWLGraphWrapper graph = new OWLGraphWrapper(model.getAboxOntology());
		Set<OWLClass> locationRoots = new HashSet<OWLClass>();
		if (request.locationRoots != null) {
			for(String loc : request.locationRoots) {
				OWLClass cls = graph.getOWLClassByIdentifier(loc);
				if (cls != null) {
					locationRoots.add(cls);
				}
			}
		}
		Set<String> evidenceRestriction = request.evidenceRestriction != null ? new HashSet<String>(Arrays.asList(request.evidenceRestriction)) : null;
		Set<String> blackList = request.ignoreList != null ? new HashSet<String>(Arrays.asList(request.ignoreList)) : null;
		Set<String> taxonRestriction = Collections.singleton(request.taxon);
		ExpressionMaterializingReasoner reasoner = factory.createReasoner(model.getAboxOntology());
		reasoner.setIncludeImports(true);
		GolrSeedingDataProvider provider = new GolrSeedingDataProvider(golrUrl, graph, 
				reasoner, locationRoots, evidenceRestriction, taxonRestriction, blackList);
		ModelSeeding<UndoMetadata> seeder = new ModelSeeding<UndoMetadata>(reasoner, provider);

		// seed
		seeder.seedModel(modelId, model, m3, request.process, token);
		
		// render result
		// create response.data
		response.data = new SeedResponseData();
		reasoner.flush();
		response.data.inconsistentFlag = reasoner.isConsistent();
		
		MolecularModelJsonRenderer renderer = createModelRenderer(model, externalLookupService);
		// render complete model
		JsonModel jsonModel = renderer.renderModel();
		response.data.individuals = jsonModel.individuals;
		response.data.facts = jsonModel.facts;
		response.data.properties = jsonModel.properties;
		response.data.individualsInferred = renderer.renderModelInferences(reasoner);
		
		return response;
	}
	
	/*
	 * commentary is now to be a string, not an unknown multi-leveled object.
	 */
	private SeedResponse error(SeedResponse state, String msg, Throwable e) {
		state.messageType = "error";
		state.message = msg;
		if (e != null) {

			// Add in the exception name if possible.
			String ename = e.getClass().getName();
			if( ename != null ){
				state.message = state.message + " Exception: " + ename + ".";
			}
			
			// And the exception message.
			String emsg = e.getMessage();
			if( emsg != null ){
				state.message = state.message + " " + emsg;
			}
			
			// Add the stack trace as commentary.
			StringWriter stacktrace = new StringWriter();
			e.printStackTrace(new PrintWriter(stacktrace));
			state.commentary = stacktrace.toString();
		}
		return state;
	}
}
