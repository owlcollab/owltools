package owltools.gaf.lego.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import owltools.cli.Opts;
import owltools.gaf.bioentities.ProteinTools;
import owltools.gaf.lego.MolecularModelManager;
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
		String proteinOntologyFolder = null; // optional
		List<String> additionalImports = new ArrayList<String>();

		// right now we are using the relation names to select as subset of relations
		// later this might be done as an annotation property in OWL
		Set<String> relevantRelations = new HashSet<String>();
		// relations marked as important
		relevantRelations.addAll(Arrays.asList(
				"part_of",
				"enabled_by",
				"directly activates",
				"directly provides input for", // weaker than 'directly activates'
				"occurs in",
				"has input",
				"has output",
				"starts with",
				"ends with"));
		
		boolean allowBatch = true;
		
		// provide a map from db name to taxonId
		// used to resolve a protain ontology (if available)
		Map<String, String> dbToTaxon = ProteinTools.getDefaultDbToTaxon();
		
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
			else if (opts.nextEq("-p|--port")) {
				port = Integer.parseInt(opts.nextOpt());
			}
			else if (opts.nextEq("-i|--import|--additional-import")) {
				additionalImports.add(StringUtils.trim(opts.nextOpt()));
			}
			else if (opts.nextEq("--no-batch")) {
				allowBatch = false;
			}
			else if (opts.nextEq("--allow-batch")) {
				allowBatch = true;
			}
			else if (opts.nextEq("--set-relevant-relations")) {
				relevantRelations.addAll(opts.nextList());
			}
			else if (opts.nextEq("--add-relevant-relations")) {
				relevantRelations.addAll(opts.nextList());
			}
			else if (opts.nextEq("--add-relevant-relation")) {
				relevantRelations.add(opts.nextOpt());
			}
			else if (opts.nextEq("--allow-batch")) {
				allowBatch = true;
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

		
		startUp(ontology, catalog, modelFolder, gafFolder, proteinOntologyFolder, port, contextString, additionalImports, allowBatch, relevantRelations, dbToTaxon);
	}

	public static void startUp(String ontology, String catalog, String modelFolder, 
			String gafFolder, String proteinOntologyFolder, int port, String contextString, 
			List<String> additionalImports, boolean allowBatch, Set<String> relevantRelations, 
			Map<String, String> dbToTaxon) throws Exception {
		// load ontology
		LOGGER.info("Start loading ontology: "+ontology);
		ParserWrapper pw = new ParserWrapper();
		// if available, set catalog
		if (catalog != null) {
			pw.addIRIMapper(new CatalogXmlIRIMapper(catalog));
		}
		OWLGraphWrapper graph = pw.parseToOWLGraph(ontology);

		// create model manager
		LOGGER.info("Start initializing MMM");
		MolecularModelManager models = new MolecularModelManager(graph);
		models.setPathToOWLFiles(modelFolder);
		if (gafFolder != null) {
			models.setPathToGafs(gafFolder);
		}
		models.addImports(additionalImports);
		if (proteinOntologyFolder != null) {
			models.setPathToProteinFiles(proteinOntologyFolder);
		}
		models.setDbToTaxon(dbToTaxon);
		
		Server server = startUp(models, port, contextString, allowBatch, relevantRelations);
		server.join();
	}
	
	public static Server startUp(MolecularModelManager models, int port, String contextString, boolean allowBatch, Set<String> relevantRelations)
			throws Exception {
		LOGGER.info("Setup Jetty config.");
		// Configuration: Use an already existing handler instance
		// Configuration: Use custom JSON renderer (GSON)
		ResourceConfig resourceConfig = new ResourceConfig();
		resourceConfig.register(GsonMessageBodyHandler.class);
		resourceConfig.register(RequireJsonpFilter.class);
		//resourceConfig.register(AuthorizationRequestFilter.class);
		if (allowBatch) {
			JsonOrJsonpBatchHandler batchHandler = new JsonOrJsonpBatchHandler(models, relevantRelations);
			resourceConfig = resourceConfig.registerInstances(batchHandler);
		}
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
