package owltools.gaf.lego.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLSubObjectPropertyOfAxiom;

import owltools.cli.Opts;
import owltools.gaf.lego.UndoAwareMolecularModelManager;
import owltools.gaf.lego.server.external.CombinedExternalLookupService;
import owltools.gaf.lego.server.external.ExternalLookupService;
import owltools.gaf.lego.server.external.ProteinToolService;
import owltools.gaf.lego.server.handler.JsonOrJsonpBatchHandler;
import owltools.gaf.lego.server.handler.JsonOrJsonpModelHandler;
import owltools.graph.OWLGraphWrapper;
import owltools.io.CatalogXmlIRIMapper;
import owltools.io.ParserWrapper;


public class StartUpTool {
	
	private static final Logger LOGGER = Logger.getLogger(StartUpTool.class);

	public static void main(String[] args) throws Exception {
		Opts opts = new Opts(args);
		
		// data configuration
		String ontology = null;
		String catalog = null;
		String modelFolder = null;
		String gafFolder = null; // optional
		String proteinOntologyFolder = null; // optional, should be replaced by external lookup service
		List<String> obsoleteImports = new ArrayList<String>();
		ExternalLookupService lookupService = null;
		boolean checkLiteralIds = true;

		// The subset of highly relevant relations is configured using super property
		// all direct children (asserted) are considered important
		String importantRelationParent = null;
		
		// provide a map from db name to taxonId
		// used to resolve a protain ontology (if available)
		// Map<String, String> dbToTaxon = ProteinTools.getDefaultDbToTaxon();
		
		// server configuration
		int port = 6800; 
		String contextPrefix = null; // root context by default
		
		while (opts.hasArgs()) {
			if (opts.nextEq("-g|--graph")) {
				ontology = opts.nextOpt();
			}
			else if (opts.nextEq("-c|--catalog")) {
				catalog = opts.nextOpt();
			}
			else if (opts.nextEq("-f|--model-folder")) {
				modelFolder = opts.nextOpt();
			}
			else if (opts.nextEq("-p|--protein-folder")) {
				proteinOntologyFolder = opts.nextOpt();
			}
			else if (opts.nextEq("--gaf-folder")) {
				gafFolder = opts.nextOpt();
			}
			else if (opts.nextEq("--context-prefix")) {
				contextPrefix = opts.nextOpt();
			}
			else if (opts.nextEq("--port")) {
				port = Integer.parseInt(opts.nextOpt());
			}
			else if (opts.nextEq("-i|--import|--additional-import")) {
				System.err.println("-i|--import|--additional-import is no longer supported, all imports are expected to be in the source ontology '-g|--graph'");
				System.exit(-1);
			}
			else if (opts.nextEq("--obsolete-import")) {
				obsoleteImports.add(StringUtils.trim(opts.nextOpt()));
			}
			else if (opts.nextEq("--set-relevant-relations")) {
				System.err.println("--set-relevant-relations is no longer supported, use '--set-important-relation-parent' instead");
				System.exit(-1);
			}
			else if (opts.nextEq("--add-relevant-relations")) {
				System.err.println("--add-relevant-relations is no longer supported, use '--set-important-relation-parent' instead");
				System.exit(-1);
			}
			else if (opts.nextEq("--add-relevant-relation")) {
				System.err.println("--add-relevant-relation is no longer supported, use '--set-important-relation-parent' instead");
				System.exit(-1);
			}
			else if (opts.nextEq("--set-important-relation-parent")) {
				importantRelationParent = opts.nextOpt();
			}
			else if (opts.nextEq("--skip-class-id-validation")) {
				checkLiteralIds = false;
			}
			else {
				break;
			}
		}
		if (ontology == null) {
			System.err.println("No ontology graph available");
			System.exit(-1);
		}
		if (modelFolder == null) {
			System.err.println("No model folder available");
			System.exit(-1);
		} 
		String contextString = "/";
		if (contextPrefix != null) {
			contextString = "/"+contextPrefix;
		}

		
		startUp(ontology, catalog, modelFolder, gafFolder, proteinOntologyFolder, 
				port, contextString, obsoleteImports, importantRelationParent,
				lookupService, checkLiteralIds);
	}
	
	/**
	 * Try to resolve the given string into an {@link OWLObjectProperty}.
	 * 
	 * @param rel
	 * @param g
	 * @return property or null
	 */
	public static OWLObjectProperty getRelation(String rel, OWLGraphWrapper g) {
		if (rel == null || rel.isEmpty()) {
			return null;
		}
		if (rel.startsWith("http://")) {
			IRI iri = IRI.create(rel);
			return g.getDataFactory().getOWLObjectProperty(iri);
		}
		// try to find property
		OWLObjectProperty p = g.getOWLObjectPropertyByIdentifier(rel);
		if (p == null) {
			// could not find by id, search by label
			OWLObject owlObject = g.getOWLObjectByLabel(rel);
			if (owlObject instanceof OWLObjectProperty) {
				p = (OWLObjectProperty) owlObject;
			}
		}
		return p;
	}
	
	/**
	 * Find all asserted direct sub properties of the parent property.
	 * 
	 * @param parent
	 * @param g
	 * @return set
	 */
	public static Set<OWLObjectProperty> getAssertedSubProperties(OWLObjectProperty parent, OWLGraphWrapper g) {
		Set<OWLObjectProperty> properties = new HashSet<OWLObjectProperty>();
		for(OWLOntology ont : g.getAllOntologies()) {
			Set<OWLSubObjectPropertyOfAxiom> axioms = ont.getObjectSubPropertyAxiomsForSuperProperty(parent);
			for (OWLSubObjectPropertyOfAxiom axiom : axioms) {
				OWLObjectPropertyExpression subProperty = axiom.getSubProperty();
				if (subProperty instanceof OWLObjectProperty) {
					properties.add(subProperty.asOWLObjectProperty());
				}
			}
		}
		return properties;
	}

	public static void startUp(String ontology, String catalog, String modelFolder, 
			String gafFolder, String proteinOntologyFolder, int port, String contextString, 
			List<String> obsoleteImports, String importantRelationParent,
			ExternalLookupService lookupService, boolean checkLiteralIds) 
			throws Exception {
		// load ontology
		LOGGER.info("Start loading ontology: "+ontology);
		ParserWrapper pw = new ParserWrapper();
		// if available, set catalog
		if (catalog != null) {
			pw.addIRIMapper(new CatalogXmlIRIMapper(catalog));
		}
		OWLGraphWrapper graph = pw.parseToOWLGraph(ontology);
		
		// try to get important relations
		Set<OWLObjectProperty> importantRelations = null;
		if (importantRelationParent != null) {
			// try to find parent property
			OWLObjectProperty parentProperty = getRelation(importantRelationParent, graph);
			if (parentProperty != null) {
				// find all asserted direct sub properties of the parent property
				importantRelations = getAssertedSubProperties(parentProperty, graph);
				if (importantRelations.isEmpty()) {
					LOGGER.warn("Could not find any asserted sub properties for parent: "+importantRelationParent);
				}
			}
			else {
				LOGGER.warn("Could not find a property for rel: "+importantRelationParent);
			}
		}

		// create model manager
		LOGGER.info("Start initializing MMM");
		UndoAwareMolecularModelManager models = new UndoAwareMolecularModelManager(graph);
		// set folder to  models
		models.setPathToOWLFiles(modelFolder);
		
		// optional: set folder to GAFs for seeding models
		if (gafFolder != null) {
			models.setPathToGafs(gafFolder);
		}
		// configure obsolete imports for clean up of existing models
		models.addObsoleteImports(obsoleteImports);
		
		// configure protein name lookup using the deprecated organism specific protein ontologies
		if (proteinOntologyFolder != null) {
			ProteinToolService proteinService = new ProteinToolService(proteinOntologyFolder);
			if (lookupService != null) {
				lookupService = new CombinedExternalLookupService(Arrays.<ExternalLookupService>asList(lookupService));
			}
			else {
				lookupService = proteinService;
			}
			Set<IRI> additionalObsoletes = proteinService.getOntologyIRIs();
			if (additionalObsoletes != null) {
				models.addObsoleteImportIRIs(additionalObsoletes);
			}
		}
		
		// start server
		Server server = startUp(models, port, contextString, importantRelations, lookupService, checkLiteralIds);
		server.join();
	}
	
	public static Server startUp(UndoAwareMolecularModelManager models, int port, String contextString, 
			Set<OWLObjectProperty> relevantRelations, ExternalLookupService lookupService, boolean checkLiteralIds)
			throws Exception {
		LOGGER.info("Setup Jetty config.");
		// Configuration: Use an already existing handler instance
		// Configuration: Use custom JSON renderer (GSON)
		ResourceConfig resourceConfig = new ResourceConfig();
		resourceConfig.register(GsonMessageBodyHandler.class);
		resourceConfig.register(RequireJsonpFilter.class);
		//resourceConfig.register(AuthorizationRequestFilter.class);
		JsonOrJsonpBatchHandler batchHandler = new JsonOrJsonpBatchHandler(models, relevantRelations, lookupService);
		batchHandler.CHECK_LITERAL_IDENTIFIERS = checkLiteralIds;
		resourceConfig = resourceConfig.registerInstances(batchHandler);
		JsonOrJsonpModelHandler defaultModelHandler = new JsonOrJsonpModelHandler(models);
		resourceConfig = resourceConfig.registerInstances(defaultModelHandler);

		// setup jetty server port and context path
		Server server = new Server(port);

		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath(contextString);
		server.setHandler(context);
		ServletHolder h = new ServletHolder(new ServletContainer(resourceConfig));
		context.addServlet(h, "/*");

		// start jetty server
		LOGGER.info("Start server on port: "+port);
		server.start();
		return server;
	}
}
