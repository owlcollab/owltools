package owltools.gaf.lego.server;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import owltools.cli.Opts;
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
		List<String> additionalImports = new ArrayList<String>();
		boolean allowBatch = true;
		
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

		
		startUp(ontology, catalog, modelFolder, gafFolder, port, contextString, additionalImports, allowBatch);
	}

	public static void startUp(String ontology, String catalog, String modelFolder, String gafFolder, int port, String contextString, List<String> additionalImports, boolean allowBatch)
			throws Exception {
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

		LOGGER.info("Setup Jetty config.");
		// Configuration: Use an already existing handler instance
		// Configuration: Use custom JSON renderer (GSON)
		ResourceConfig resourceConfig = new ResourceConfig();
		resourceConfig.register(GsonMessageBodyHandler.class);
		resourceConfig.register(RequireJsonpFilter.class);
		//resourceConfig.register(AuthorizationRequestFilter.class);
		if (allowBatch) {
			JsonOrJsonpBatchHandler modelHandler = new JsonOrJsonpBatchHandler(graph, models);
			resourceConfig = resourceConfig.registerInstances(modelHandler);
		}
		else {
			JsonOrJsonpModelHandler modelHandler = new JsonOrJsonpModelHandler(graph, models);
			resourceConfig = resourceConfig.registerInstances(modelHandler);
		}

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
		server.join();
	}
}
