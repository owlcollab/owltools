package owltools.cli;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.common.SolrException;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLAnnotationValueVisitorEx;
import org.semanticweb.owlapi.model.OWLAnonymousIndividual;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import com.google.common.base.Optional;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import owltools.cli.tools.CLIMethod;
import owltools.flex.FlexCollection;
import owltools.gaf.EcoTools;
import owltools.gaf.GafDocument;
import owltools.gaf.TaxonTools;
import owltools.gaf.parser.GafObjectsBuilder;
import owltools.gaf.parser.ParserListener;
import owltools.graph.OWLGraphWrapper;
import owltools.graph.RelationSets;
import owltools.graph.shunt.OWLShuntEdge;
import owltools.graph.shunt.OWLShuntGraph;
import owltools.graph.shunt.OWLShuntNode;
import owltools.io.CatalogXmlIRIMapper;
import owltools.io.ParserWrapper;
import owltools.panther.PANTHERForest;
import owltools.solrj.ComplexAnnotationSolrDocumentLoader;
import owltools.solrj.FlexSolrDocumentLoader;
import owltools.solrj.GafSolrDocumentLoader;
import owltools.solrj.ModelAnnotationSolrDocumentLoader;
import owltools.solrj.OntologyGeneralSolrDocumentLoader;
import owltools.solrj.OptimizeSolrDocumentLoader;
import owltools.solrj.PANTHERGeneralSolrDocumentLoader;
import owltools.solrj.PANTHERSolrDocumentLoader;
import owltools.solrj.loader.MockSolrDocumentLoader;
import owltools.solrj.loader.MockFlexSolrDocumentLoader;
import owltools.solrj.loader.MockGafSolrDocumentLoader;
import owltools.solrj.loader.MockModelAnnotationSolrDocumentLoader;
import owltools.solrj.loader.MockSolrDocumentCollection;
import owltools.yaml.golrconfig.ConfigManager;
import owltools.yaml.golrconfig.SolrSchemaXMLWriter;

/**
 *  Solr/GOlr loading.
 */	
@SuppressWarnings("deprecation")
public class SolrCommandRunner extends TaxonCommandRunner {

	private static final Logger LOG = Logger.getLogger(SolrCommandRunner.class);
	
	private String globalSolrURL = null;
	private File globalSolrLogFile = null;
	private ConfigManager aconf = null;
	private PANTHERForest pSet = null;
	private List<File> legoCatalogs = null;
	private List<File> legoFiles = null;
	private List<String> caFiles = null;
	private String legoModelPrefix = null;
	private String taxonSubsetName = null;
	private String ecoSubsetName = null;

	/**
	 * Output (STDOUT) a XML segment to put into the Solr schema file after reading the YAML file.
	 * 
	 * @param opts
	 */
	@CLIMethod("--solr-config")
	public void configRead(Opts opts) {
		
		LOG.info("Grab configuration files.");

		// Try and munge all of the configs together.
		aconf = new ConfigManager();
		List<String> confList = opts.nextList();
		for( String fsPath : confList ){

			LOG.info("Trying config found at: " + fsPath);
		
			// Attempt to parse the given config file.
			try {
				aconf.add(fsPath);
				LOG.info("Using config found at: " + fsPath);
			} catch (FileNotFoundException e) {
				LOG.info("Failure with config file at: " + fsPath);
				e.printStackTrace();
			}
		}
	}

	/**
	 * Output (STDOUT) XML segment to put into the Solr schema file after reading the YAML configuration file(s).
	 * 
	 * @param opts
	 */
	@CLIMethod("--solr-schema-dump")
	public void solrSchemaDump(Opts opts) {
		
		LOG.info("Dump Solr schema.");

		// Get the XML from the dumper into a string.
		String config_string = null;
		try {
			SolrSchemaXMLWriter ssxw = new SolrSchemaXMLWriter(aconf);
			config_string = ssxw.schema();
		} catch (XMLStreamException e) {
			e.printStackTrace();
		}
		
//		// Run the XML through a regexp to just get the parts we want.
//		String output = null;
//		//LOG.info("Current XML schema:\n" + config_string);
//		Pattern pattern = Pattern.compile("<!--START-->(.*)<!--STOP-->", Pattern.DOTALL);
//		Matcher matcher = pattern.matcher(config_string);
//		boolean matchFound = matcher.find();
//		if (matchFound) {
//			output = matcher.group(1); // not the global match, but inside
//			//LOG.info("Found:\n" + output);
//		}
		
//		// Either we got it, and we dump to STDOUT, or exception.
//		if( output == null || output.equals("") ){
//			throw new Error();
//		}else{
//  		System.out.println(output);
		System.out.println(config_string);
//		}
	}

	/**
	 * Set an optional Solr URL to use with Solr options so they don't have to
	 * be specified separately for every option.
	 * 
	 * @param opts
	 */
	@CLIMethod("--solr-url")
	public void setSolrUrl(Opts opts) {
	    opts.info("URL", "Note: pass 'mock' to create a mock loader (writes json docs to stdout");
		globalSolrURL = opts.nextOpt(); // shift it off of null
		LOG.info("Globally use GOlr server at: " + globalSolrURL);
	}
	
	/**
	 * Set an optional file to use for logging load data (can be consumed by AmiGO 2).
	 * 
	 * @param opts
	 */
	@CLIMethod("--solr-log")
	public void setSolrLogFile(Opts opts) {
		String globalSolrLogFileName = opts.nextOpt(); // shift it off of null
		globalSolrLogFile = FileUtils.getFile(globalSolrLogFileName);
		if( globalSolrLogFile == null ){
			LOG.info("Could not find/use file: " + globalSolrLogFileName);
		}else{
			LOG.info("Globally use GOlr log file: " + globalSolrLogFile.getAbsolutePath());
		}
	}
	
	/**
	 * Manually purge the index to try again.
	 * Since this cascade is currently ordered, can be used to purge before we load.
	 * 
	 * @param opts
	 * @throws Exception
	 */
	@SuppressWarnings("deprecation")
	@CLIMethod("--solr-purge")
	public void purgeSolr(Opts opts) throws Exception  {

		// Check to see if the global url has been set.
		String url = sortOutSolrURL(globalSolrURL);				

		// Wipe out the solr index at url.
		SolrServer server = new CommonsHttpSolrServer(url);
		server.deleteByQuery("*:*");
		server.commit();

		// Probably worked, so let's destroy the log if there is one.
		if( globalSolrLogFile != null && globalSolrLogFile.exists() ){
		    boolean yes_p = globalSolrLogFile.delete();
		    if( yes_p ){
		        LOG.info("Deleted GOlr load log file.");
		    }else{
		        // Nothing there, doing nothing.
		    }
		}
		LOG.info("Purged: " + url);
	}
	
//	/**
//	 * Used for loading whatever ontology stuff we have into GOlr.
//	 * 
//	 * @param opts 
//	 * @throws Exception
//	 */
//	@Deprecated
//	@CLIMethod("--solr-load-ontology-old")
//	public void loadOntologySolr(Opts opts) throws Exception {
//		// Check to see if the global url has been set.
//		String url = sortOutSolrURL(globalSolrURL);				
//
//		// Actual ontology class loading.
//		try {
//			OntologySolrLoader loader = new OntologySolrLoader(url, g);
//			loader.load();
//		} catch (SolrServerException e) {
//			LOG.info("Ontology load at: " + url + " failed!");
//			e.printStackTrace();
//		}
//	}
	
	
	
	/**
	 * Flexible loader for ontologies--uses the YAML config to find loading functions.
	 * 
	 * @param opts
	 * @throws Exception
	 */
	@CLIMethod("--solr-load-ontology")
	public void flexLoadOntologySolr(Opts opts) throws Exception {
	    opts.info("[--min-classes MIN] [--allow-null]", "Loads current in-memory graph as ontology documents using flex");
		// pre-check ontology
		int code = preCheckOntology("Can't process an inconsistent ontology for solr", 
				"Can't process an ontology with unsatisfiable classes for solr", null);
		if (code != 0) {
			exit(code);
			return;
		}
		
		boolean allowNullOntologies = false;
		int minClasses = 100;
		while (opts.hasOpts()) {
            if (opts.nextEq("--min-classes")) {
                opts.info("NUM", "exit with non-zero if fewer classes encountered");
                minClasses = Integer.valueOf(opts.nextOpt());
            }
            else if (opts.nextEq("--allow-null")) {
                opts.info("", "if set, empty ontologies (0 axioms, no IRI) will be ignored rather than failing");
                allowNullOntologies = true;
            }
		    else {
		        break;
		    }
		}
		
		int numClasses = g.getAllOWLClasses().size();
		if (numClasses < minClasses) {
		    LOG.error("Fewer classes than expected: "+numClasses+" < "+minClasses);
		    exit(1);
		}
		
		for (OWLOntology o : g.getAllOntologies()) {
		    OWLOntologyID oid = o.getOntologyID();
		    if (oid == null || !oid.getOntologyIRI().isPresent()) {
		        if (o.getAxiomCount() == 0) {
		            if (!allowNullOntologies) {
		                LOG.error("Encountered null ontology: "+o);
		                LOG.error("perhaps an ontology IRI is misconfigured?");
		                LOG.error("run with --allow-null to ignore this");
		                exit(1);
		                
		            }
		        }
		    }
		}

		// Check to see if the global url has been set.
		String url = sortOutSolrURL(globalSolrURL);				

		// Grab the intermediate form.
		LOG.info("Assembling FlexCollection...");
		FlexCollection flex = new FlexCollection(aconf, g);
		
        boolean isMock = false;
		int nClasses = 0;
		// Actual ontology class loading.
		FlexSolrDocumentLoader loader;
		if (url.equals("mock")) {
		    loader = new MockFlexSolrDocumentLoader(flex);
		    isMock = true;
		}
		else {
		    loader = new FlexSolrDocumentLoader(url, flex);
		}

		LOG.info("Trying ontology flex load.");
		loader.load();

		// number of docs loaded MUST be equal to or higher than minClasses
		// (docs also comprises non-class documents, e.g. ObjectProperties)
		if (loader.getCurrentDocNumber() < minClasses) {
		    LOG.error("Fewer documents loaded than expected: "+loader.getCurrentDocNumber()+" < "+minClasses);
		    exit(1);			    
		}

		// Load likely successful--log it.
		//optionallyLogLoad("ontology", ???);
		// TODO: Well, this is a lame second best.
		//for( OWLOntology o : g.getAllOntologies() ){
		for( OWLOntology o : g.getManager().getOntologies() ){

		    //optionallyLogLoad("ontology", o.getOntologyID().toString());
		    // This is "correct", but I'm only getting one.
		    String ont_id = "unknown";
		    String ont_version = "unknown";
		    try {
		        if (o.getOntologyID() !=  null) {
		            Optional<IRI> optional = o.getOntologyID().getOntologyIRI();
		            if (optional.isPresent()) {
		                ont_id = optional.get().toString();
		            }
		            else {
		                LOG.info("Failed to get ID of: " + o.toString() + "!");
		            }
		        }
		    } catch (NullPointerException e) {
		        LOG.info("Failed to get ID of: " + o.toString() + "!");
		    }

		    try {
		        Optional<IRI> versionIRI = o.getOntologyID().getVersionIRI();
		        if (versionIRI.isPresent()){
		            ont_version = versionIRI.get().toString();
		            Pattern p = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})");
		            Matcher m = p.matcher(ont_version);
		            m.find();
		            ont_version = m.group(0);
		            //ont_version = StringUtils.substringBetween(ont_version, "releases/", "/");
		        }
		        else {
		            ont_version = null;
		        }
		        if( ont_version == null || ont_version.equals("") ){
		            // Sane fallback.
		            LOG.info("Failed to extract version of: " + ont_id + "!");
		        }
		    } catch (NullPointerException e) {
		        LOG.info("Failed to get version of: " + ont_id + "!");
		    } catch (IllegalStateException e) {
		        LOG.info("Failed to get match for version of: " + ont_id + "!");
		    }
		    optionallyLogLoad("ontology", ont_id, ont_version);

		    if (isMock) {
		        showMockDocs((MockSolrDocumentLoader) loader);
		    }

		}
		
	}
	


    /**
	 * Trivial hard-wired (and optional) method for loading collected
	 * ontology information into the "general" schema (general-config.yaml) for GO.
	 * This is a very dumb, but easy to understand, loader.
	 * 
	 * @param opts
	 * @throws Exception
	 */
	@CLIMethod("--solr-load-ontology-general")
	public void generalLoadOntologySolr(Opts opts) throws Exception {

		// Check to see if the global url has been set.
		String url = sortOutSolrURL(globalSolrURL);				

		// Actual ontology class loading.
		OntologyGeneralSolrDocumentLoader loader = new OntologyGeneralSolrDocumentLoader(url, g);
		LOG.info("Trying ontology general load.");
		loader.load();
	}
	
	/**
	 * Used for reading the ontology catalogs to be used for loading complex annotations.
	 * 
	 * @param opts
	 * @throws Exception
	 */
	@CLIMethod("--read-lego-catalogs")
	public void processLegoCatalogs(Opts opts) throws Exception {
		legoCatalogs = new ArrayList<File>();
		List<String> files = opts.nextList();
		for (String fstr : files) {
			LOG.info("Using file for Lego ontology catalog: " + fstr);
			File file = new File(fstr);
			legoCatalogs.add(file);
		}
	}

	/**
	 * Used for reading the lego files to be used for loading complex annotations.
	 * 
	 * @param opts
	 * @throws Exception
	 */
	@CLIMethod("--read-lego-files")
	public void processLegoFiles(Opts opts) throws Exception {
		legoFiles = new ArrayList<File>();
		List<String> files = opts.nextList();
		for (String fstr : files) {
			LOG.info("Using file for Lego: " + fstr);
			File file = new File(fstr);
			legoFiles.add(file);
		}
	}
	
	/**
	 * Used for reading the lego files to be used for loading complex annotations.
	 * 
	 * @param opts
	 * @throws Exception
	 */
	@CLIMethod("--read-model-folder")
	public void processModelFolder(Opts opts) throws Exception {
	    // TODO - make cofigurable; 
	    //see https://github.com/geneontology/minerva/commit/ebbe2937735a80dc7744e6cb516c5262755642e1
	    String fileSuffix = "ttl"; 
		legoFiles = new ArrayList<File>();
		String modelFolderString = opts.nextOpt();
		File modelFolder = new File(modelFolderString).getCanonicalFile();
		File[] modelFiles = modelFolder.listFiles(new FilenameFilter() {
		    final String fileSuffixFinal = fileSuffix;
			@Override
			public boolean accept(File dir, String name) {
			    if (fileSuffixFinal != null && fileSuffixFinal.length() > 0)
        	        return name.endsWith("."+fileSuffixFinal);
			    else
     		        return StringUtils.isAlphanumeric(name);
			}
		});
		Arrays.sort(modelFiles);
		for (File modelFile : modelFiles) {
			LOG.info("Using file for Lego: " + modelFile);
			legoFiles.add(modelFile);
		}
	}
	
	@CLIMethod("--read-model-url-prefix")
	public void processModelUrlPrefix(Opts opts) throws Exception {
		legoModelPrefix = opts.nextOpt();
	}

	/**
	 * Used for reading the lego files to be used for loading complex annotations.
	 * 
	 * @param opts
	 * @throws Exception
	 */
	@CLIMethod("--read-ca-list")
	public void processCAFiles(Opts opts) throws Exception {
		caFiles = new ArrayList<String>();
		List<String> files = opts.nextList();
		for (String fstr : files) {
			LOG.info("Using file for LEGO/CA: " + fstr);
			// Read file line by line, getting the model locations out.
			File file = new File(fstr);
			List<String> lines = FileUtils.readLines(file, "UTF-8");
			for (String line : lines) {
				String caloc = StringUtils.chomp(line);
				LOG.info("\tAdd file: " + caloc);
				caFiles.add(caloc);
			}
		}
	}
	
	@CLIMethod("--solr-load-models")
	public void loadModelAnnotations(Opts opts) throws Exception {
		Set<String> modelStateFilter = null;
		boolean removeDeprecatedModels = false;
		boolean removeTemplateModels = false;
        boolean removeUnsatisfiableModels = false;
        boolean exitIfUnsatisfiable = true;
        boolean exitIfLoadFails = true;
		while (opts.hasOpts()) {
			if (opts.nextEq("--defaultModelStateFilter|--productionModelStateFilter")) {
				if(modelStateFilter != null) { 
					modelStateFilter = new HashSet<String>();
				}
				// fill default
				modelStateFilter.add("production");
			}
			else if (opts.nextEq("--modelStateFilter")) {
				if(modelStateFilter != null) { 
					modelStateFilter = new HashSet<String>();
				}
				modelStateFilter.addAll(opts.nextList());
			}
			else if (opts.nextEq("--excludeDeprecatedModels|--excludeDeprecated")) {
				removeDeprecatedModels = true;
			}
			else if (opts.nextEq("--includeDeprecatedModels|--includeDeprecated")) {
				removeDeprecatedModels = false;
			}
			else if (opts.nextEq("--excludeTemplateModels|--excludeTemplate")) {
				removeTemplateModels = true;
			}
			else if (opts.nextEq("--includeTemplateModels|--includeTemplate")) {
				removeTemplateModels = false;
			}
			else if (opts.nextEq("--excludeUnsatisfiableModels|--excludeUnsatisfiable")) {
				removeUnsatisfiableModels = true;
			}
            else if (opts.nextEq("--includeUnsatisfiableModels|--includeUnsatisfiable")) {
                removeUnsatisfiableModels = false;
                exitIfUnsatisfiable = false;
            }
            else if (opts.nextEq("--noExitIfLoadFails")) {
                opts.info("", "carry on if fail to load any individual model fails. Default is fail fast");
                exitIfLoadFails = false;
            }
			else
				break;

		}
		
		// Check to see if the global url has been set.
		String url = sortOutSolrURL(globalSolrURL);

		// Ensure that legoCatalogs is defined, even is empty.
		if( legoCatalogs == null ){
			legoCatalogs = new ArrayList<File>();
			LOG.warn("Missing lego catalogs...");
		}

		// Only proceed if our environment was well-defined.
		if( legoFiles == null || legoModelPrefix == null || legoFiles.isEmpty() ){
			String details = "";
			if (legoFiles == null || legoFiles.isEmpty()) {
				details += " Missing legoFiles";
			}
			if (legoModelPrefix == null) {
				details += " Missing lego model prefix";
			}
			LOG.error("Lego environment not well defined--skipping: "+details);
			exit(-1);
		}else{
			LOG.warn("Start Loading models, count: "+legoFiles.size());
			// Ready the environment for every pass.
			ParserWrapper pw = new ParserWrapper();
			// Add all of the catalogs; possibly none.
			for( File legoCatalog : legoCatalogs ){
				pw.addIRIMapper(new CatalogXmlIRIMapper(legoCatalog));
			}
			OWLOntologyManager manager = pw.getManager();
			OWLReasonerFactory reasonerFactory = new ElkReasonerFactory();
			for( File legoFile : legoFiles ){
				String fname = legoFile.getName();
				OWLReasoner currentReasoner = null;
				OWLOntology model = null;
				try {
					model = pw.parseOWL(IRI.create(legoFile));
					
					//skip deprecated models
					boolean isDeprecated = isDeprecated(model);
					if (isDeprecated) {
						LOG.warn("Skipping deprecated model: "+fname);
						continue;
					}
					
					// Some sanity checks--some of the genereated ones are problematic.
					currentReasoner = reasonerFactory.createReasoner(model);
					boolean consistent = currentReasoner.isConsistent();
					if(consistent == false){
						// we need a consistent ontology for the closure calculations!
						LOG.warn("Skip since inconsistent: " + fname);
						continue;
					}
					Set<OWLClass> unsatisfiable = currentReasoner.getUnsatisfiableClasses().getEntitiesMinusBottom();
                    if (exitIfUnsatisfiable && unsatisfiable.isEmpty() == false) {
                        LOG.error("Unsatisfiable: " + fname+" == "+unsatisfiable);
                        System.exit(1);
                    }
                    if (removeUnsatisfiableModels && unsatisfiable.isEmpty() == false) {
                        LOG.warn("Skip since unsatisfiable: " + fname);
                        continue;
                    }
					
					ModelAnnotationSolrDocumentLoader loader = null;
					try {
						LOG.info("Trying complex annotation load of: " + fname);
						boolean isMock = false;
						String modelUrl = legoModelPrefix + fname;
						if (url.equals("mock")) {
						    loader = new MockModelAnnotationSolrDocumentLoader(url, model, currentReasoner, modelUrl, 
						            modelStateFilter, removeDeprecatedModels, removeTemplateModels);
						    isMock = true;
						}
						else {
						    loader = new ModelAnnotationSolrDocumentLoader(url, model, currentReasoner, modelUrl, 
						            modelStateFilter, removeDeprecatedModels, removeTemplateModels);
						}

						loader.load();
				        if (isMock) {
				            showMockDocs((MockModelAnnotationSolrDocumentLoader) loader);
				        }

					} catch (SolrServerException e) {
						LOG.info("Complex annotation load of " + fname + " at " + url + " failed!");
						e.printStackTrace();
						if (exitIfLoadFails) {
						    System.exit(1);
						}
					}
					finally {
						IOUtils.closeQuietly(loader);
					}
				} finally {
					// Cleanup reasoner and ontology.
					if (currentReasoner != null) {
						currentReasoner.dispose();
					}
					if (model != null) {
						manager.removeOntology(model);
					}
				}
			}
			LOG.info("Finished loading models.");
		}
	}

	private boolean isDeprecated(OWLOntology model) {
		boolean isDeprecated = false;
		for(OWLAnnotation modelAnnotation : model.getAnnotations()){
			if(modelAnnotation.getProperty().isDeprecated()) {
				OWLAnnotationValue value = modelAnnotation.getValue();
				isDeprecated = value.accept(new OWLAnnotationValueVisitorEx<Boolean>() {

					@Override
					public Boolean visit(IRI iri) {
						return false;
					}

					@Override
					public Boolean visit(OWLAnonymousIndividual individual) {
						return false;
					}

					@Override
					public Boolean visit(OWLLiteral literal) {
						String s = literal.getLiteral();
						return s.equalsIgnoreCase("true");
					}
				});
			}
		}
		return isDeprecated;
	}

	/**
	 * Experimental method for trying out the loading of complex_annotation doc type
	 * 
	 * @param opts
	 * @throws Exception
	 */
	@CLIMethod("--solr-load-complex-annotations")
	public void experimentalLoadComplexAnnotationSolr(Opts opts) throws Exception {

		// Check to see if the global url has been set.
		String url = sortOutSolrURL(globalSolrURL);				

		// Only proceed if our environment was well-defined.
		if( legoCatalogs == null || legoFiles == null || legoCatalogs.isEmpty() || legoFiles.isEmpty() ){
			LOG.warn("Lego environment not well defined--skipping.");
		}else{

			// Ready the environment for every pass.
			ParserWrapper pw = new ParserWrapper();
			// Add all of the catalogs.
			for( File legoCatalog : legoCatalogs ){
				pw.addIRIMapper(new CatalogXmlIRIMapper(legoCatalog));				
			}
			OWLOntologyManager manager = pw.getManager();
			OWLReasonerFactory reasonerFactory = new ElkReasonerFactory();
				
			// Actual loading--iterate over our list and load individually.
			for( File legoFile : legoFiles ){
				String fname = legoFile.getName();
				OWLReasoner currentReasoner = null;
				OWLOntology ontology = null;

				// TODO: Temp cover for missing group labels and IDs.
				//String agID = legoFile.getCanonicalPath();
				String agLabel = StringUtils.removeEnd(fname, ".owl");
				String agID = new String(agLabel);

				try {
					ontology = pw.parseOWL(IRI.create(legoFile));
					currentReasoner = reasonerFactory.createReasoner(ontology);
						
					// Some sanity checks--some of the genereated ones are problematic.
					boolean consistent = currentReasoner.isConsistent();
					if( consistent == false ){
						LOG.info("Skip since inconsistent: " + fname);
						continue;
					}
					Set<OWLClass> unsatisfiable = currentReasoner.getUnsatisfiableClasses().getEntitiesMinusBottom();
					
					// TODO - make configurable to allow fail fast
					if (unsatisfiable.isEmpty() == false) {
						LOG.info("Skip since unsatisfiable: " + fname);
						continue;
					}
					
					Set<OWLNamedIndividual> individuals = ontology.getIndividualsInSignature();
					Set<OWLAnnotation> modelAnnotations = ontology.getAnnotations();
					OWLGraphWrapper currentGraph = new OWLGraphWrapper(ontology);						
					try {
						LOG.info("Trying complex annotation load of: " + fname);
						ComplexAnnotationSolrDocumentLoader loader =
								new ComplexAnnotationSolrDocumentLoader(url, currentGraph, currentReasoner, individuals, modelAnnotations, agID, agLabel, fname);
						loader.load();
					} catch (SolrServerException e) {
						LOG.info("Complex annotation load of " + fname + " at " + url + " failed!");
						e.printStackTrace();
						System.exit(1);
					}
				} finally {
					// Cleanup reasoner and ontology.
					if (currentReasoner != null) {
						currentReasoner.dispose();
					}
					if (ontology != null) {
						manager.removeOntology(ontology);
					}
				}
			}
		}
	}

	/**
	 * Experimental method for trying out the loading of complex_annotation doc type.
	 * Works with --read-ca-list <file>.
	 * 
	 * @param opts
	 * @throws Exception
	 */
	@CLIMethod("--solr-load-complex-exp")
	public void loadComplexAnnotationSolr(Opts opts) throws Exception {

		// Check to see if the global url has been set.
		String url = sortOutSolrURL(globalSolrURL);				

		// Only proceed if our environment was well-defined.
		if( caFiles == null || caFiles.isEmpty() ){
			LOG.warn("LEGO environment not well defined--will skip loading LEGO/CA.");
		}else{

			// NOTE: These two lines are remainders from old code, and I'm not sure of their place in this world of ours.
			// I wish there was an arcitecture diagram somehwere...
			OWLOntologyManager manager = pw.getManager();
			OWLReasonerFactory reasonerFactory = new ElkReasonerFactory();
				
			// Actual loading--iterate over our list and load individually.
			for( String fname : caFiles ){
				OWLReasoner currentReasoner = null;
				OWLOntology ontology = null;

				// TODO: Temp cover for missing group labels and IDs.
				//String agID = legoFile.getCanonicalPath();
				String pretmp = StringUtils.removeEnd(fname, ".owl");
				String[] bits = StringUtils.split(pretmp, "/");
				String agID = bits[bits.length -1];
				String agLabel = new String(StringUtils.replaceOnce(agID, ":", "_"));
				
				try {
					ontology = pw.parseOWL(IRI.create(fname));
					currentReasoner = reasonerFactory.createReasoner(ontology);
						
					// Some sanity checks--some of the genereated ones are problematic.
					boolean consistent = currentReasoner.isConsistent();
					if( consistent == false ){
						LOG.info("Skip since inconsistent: " + fname);
						continue;
					}
					Set<OWLClass> unsatisfiable = currentReasoner.getUnsatisfiableClasses().getEntitiesMinusBottom();
					if (unsatisfiable.isEmpty() == false) {
						LOG.info("Skip since unsatisfiable: " + fname);
						continue;
					}
					
					Set<OWLNamedIndividual> individuals = ontology.getIndividualsInSignature();
					Set<OWLAnnotation> modelAnnotations = ontology.getAnnotations();
					OWLGraphWrapper currentGraph = new OWLGraphWrapper(ontology);						
					try {
						LOG.info("Trying complex annotation load of: " + fname);
						ComplexAnnotationSolrDocumentLoader loader =
								new ComplexAnnotationSolrDocumentLoader(url, currentGraph, currentReasoner, individuals, modelAnnotations, agID, agLabel, fname);
						loader.load();
					} catch (SolrServerException e) {
						LOG.info("Complex annotation load of " + fname + " at " + url + " failed!");
						e.printStackTrace();
						System.exit(1);
					}
				} finally {
					// Cleanup reasoner and ontology.
					if (currentReasoner != null) {
						currentReasoner.dispose();
					}
					if (ontology != null) {
						manager.removeOntology(ontology);
					}
				}
			}
		}
	}

	/**
	 * Dump experimental flexible loader output to JSON(?) blob.
	 * 
	 * @param opts
	 * @throws Exception
	 */
	@CLIMethod("--solr-dump-ontology")
	public void flexDumpOntologySolr(Opts opts) throws Exception {

		//Logger.getRootLogger().setLevel(Level.ERROR);
		
		// Grab the intermediate form.
		FlexCollection flex = new FlexCollection(aconf, g);

		// TODO: Make it a little nicer?
		
		// And dump it.
		Gson gson = new Gson();
		System.out.println(gson.toJson(flex));
	}
		
	/**
	 * Used for loading a list of GAFs into GOlr.
	 * 
	 * @param opts
	 * @throws Exception
	 */
	@CLIMethod("--solr-load-gafs")
	public void loadGAFsSolr(Opts opts) throws Exception {
		ParserListener lineCountReporter = null;
		while (opts.hasOpts()) {
			if (opts.nextEq("--report-line-count")) {
				lineCountReporter = new ParserListener() {

					@Override
					public boolean reportWarnings() {
						return false;
					}

					@Override
					public void parsing(String line, int lineNumber) {
						if (lineNumber % 100000 == 0) {
							LOG.info("Parsing line count: "+lineNumber);
						}
					}

					@Override
					public void parserWarning(String message, String line, int lineNumber) {
						// do nothing
					}

					@Override
					public void parserError(String errorMessage, String line, int lineNumber) {
						// do nothing
					}
				};
			}
			else
				break;

		}
		
		// Check to see if the global url has been set.
		String url = sortOutSolrURL(globalSolrURL);
		
		// We should already have added the reasoner elsewhere on the commandline,
		// So there should be real no extra overhead here.
		EcoTools eco = new EcoTools(g, g.getReasoner(), true);
		TaxonTools taxo = new TaxonTools(g.getReasoner(), true);
		
		List<String> files = opts.nextList();
		for (String file : files) {
			LOG.info("Parsing GAF: [" + file + "]");
			GafObjectsBuilder builder = new GafObjectsBuilder();
			if (lineCountReporter != null) {
				builder.getParser().addParserListener(lineCountReporter);
			}
			gafdoc = builder.buildDocument(file);
			loadGAFDoc(url, gafdoc, eco, taxo, pSet, taxonSubsetName, ecoSubsetName);
			
			// Load likely successful--log it.
			optionallyLogLoad("gaf", file, "n/a");
		}
		
		eco.dispose();
		taxo.dispose();
	}
	
//	/**
//	 * Requires the --gaf argument (or something else that fills the gafdoc object).
//	 * 
//	 * @param opts
//	 * @throws Exception
//	 */
//	@CLIMethod("--solr-load-gaf")
//	public void loadGafSolr(Opts opts) throws Exception {
//		// Double check we're not going to do something silly, like try and
//		// use a null variable...
//		if( gafdoc == null ){
//			System.err.println("No GAF document defined (maybe use '--gaf GAF-FILE') ");
//			exit(1);
//		}
//
//		// We should already have added the reasoner elsewhere on the commandline,
//		// So there should be real no extra overhead here.
//		EcoTools eco = new EcoTools(g, g.getReasoner(), true);
//		TaxonTools taxo = new TaxonTools(g, g.getReasoner(), true);
//
//		// Check to see if the global url has been set.
//		String url = sortOutSolrURL(globalSolrURL);
//		// Doc load.
//		loadGAFDoc(url, gafdoc, eco, taxo, pSet);
//
//		// Load likely successful--log it.
//		optionallyLogLoad("gaf", gafdoc.getId());
//
//		eco.dispose();
//		taxo.dispose();
//	}
		
	/**
	 * Requires the --read-panther argument (or something else that fills the pSet object).
	 * 
	 * Read the PANTHER family data in to the family-config.yaml schema.
	 * 
	 * @param opts
	 * @throws Exception
	 */
	@CLIMethod("--solr-load-panther")
	public void loadPANTHERSolr(Opts opts) throws Exception {
		// Double check we're not going to do something silly, like try and
		// use a null variable...
		if( pSet == null ){
			System.err.println("No PANTHER documents defined (maybe use '--read-panther <panther directory>') ");
			exit(1);
			return;
		}

		// Check to see if the global url has been set.
		String url = sortOutSolrURL(globalSolrURL);
		
		// Doc load.
		PANTHERSolrDocumentLoader loader = new PANTHERSolrDocumentLoader(url);
		loader.setPANTHERSet(pSet);
		loader.setGraph(g);
		loader.load();
	}
		
	/**
	 * Requires the --read-panther argument (or something else that fills the pSet object).
	 * 
	 * Read the PANTHER family data in to the general-config.yaml schema.
	 * 
	 * @param opts
	 * @throws Exception
	 */
	@CLIMethod("--solr-load-panther-general")
	public void loadPANTHERGeneralSolr(Opts opts) throws Exception {
		// Double check we're not going to do something silly, like try and
		// use a null variable...
		if( pSet == null ){
			System.err.println("No PANTHER documents defined (maybe use '--read-panther <panther directory>') ");
			exit(1);
			return;
		}

		// Check to see if the global url has been set.
		String url = sortOutSolrURL(globalSolrURL);
		
		// Doc load.
		PANTHERGeneralSolrDocumentLoader loader = new PANTHERGeneralSolrDocumentLoader(url);
		loader.setPANTHERSet(pSet);
		loader.setGraph(g);
		loader.load();
	}
		
	/**
	 * Used for loading a list of GPAD pairs into GOlr.
	 * 
	 * @param opts
	 * @throws Exception
	 */
//	@CLIMethod("--solr-load-gpads")
//	public void loadGPADsSolr(Opts opts) throws Exception {
//		// Check to see if the global url has been set.
//		//String url = sortOutSolrURL(globalSolrURL);
//
//		List<String> files = opts.nextList();
//		if( files.size() % 2 != 0 ){
//			System.err.println("GPAD format comes in pairs; skipping...");
//		}else{
//			while( ! files.isEmpty() ){
//				String car = files.remove(0);
//				String cdr = files.remove(0);
//				LOG.info("Parsing GPAD car: " + car);
//				LOG.info("Parsing GPAD cdr: " + cdr);
//				// TODO: a new buildDocument that takes the two GPAD arguments.
//				//GafObjectsBuilder builder = new GafObjectsBuilder();
//				//gafdoc = builder.buildDocument(car, cdr);
//				//loadGAFDoc(url, gafdoc);
//			}
//		}
//	}
	
	/**
	 * Used for applying the panther trees to the currently data run.
	 * This must be run before the command is given to load a GAF.
	 * 
	 * The first argument in the list is for PANTHER7.2_HMM_classifications
	 * or whatever it will be called.
	 * 
	 * The rest are for directories that contain the actual PANTHER trees.
	 * 
	 * @param opts
	 * @throws Exception
	 */
	@CLIMethod("--read-panther")
	public void processPantherTrees(Opts opts) throws Exception {

//		// The first argument must be the associated HMM data dump.
//		String treeClassifications = opts.nextOpt();
//		LOG.info("Using file for PANTHER labels/classifications: " + treeClassifications);
//		File tcFile = new File(treeClassifications);		
		
		// The rest of the arguments are 
		// Go through the listed directories and collect the PANTHER
		// tree files.
		String treeDir = opts.nextOpt();
		//List<File> pFilesCollection = new ArrayList<File>();
//		while( ! treeDirs.isEmpty() ){
//			String tDirName = treeDirs.remove(0);
//			LOG.info("Using directory for PANTHER tree discovery: " + tDirName );
//			File pDir = new File(tDirName);
//			//FileFilter pFileFilter = new WildcardFileFilter("PTHR*.tree");
//			File[] pFiles = pDir.listFiles(pFileFilter);
//			if( pFiles !=null ){
//				for( File pFile : pFiles ){
//					pFilesCollection.add(pFile);
//					//LOG.info("Processing PANTHER tree: " + pFile.getAbsolutePath());
//				}			
//			}
//		}
		
		// Process the files and ready them for use in the Loader.
		File pDir = new File(treeDir);
		pSet = new PANTHERForest(pDir);
		LOG.info("Found " + pSet.getNumberOfFilesInSet() + " trees and " + pSet.getNumberOfIdentifiersInSet() + " identifiers.");
	}

	@CLIMethod("--solr-taxon-subset-name")
	public void solrTaxonSubsetName(Opts opts) throws Exception {
		taxonSubsetName = opts.nextOpt();
		
	}

	@CLIMethod("--solr-eco-subset-name")
	public void solrEcoSubsetName(Opts opts) throws Exception {
		ecoSubsetName = opts.nextOpt();
	}

	/**
	 * Requires the --solr-url argument.
	 * 
	 * Ignoring necessity, sends the optimize to the Solr index. I hope you have double the space!
	 * 
	 * @param opts
	 * @throws Exception
	 */
	@CLIMethod("--solr-optimize")
	public void optimizeSolr(Opts opts) throws Exception {

		// Check to see if the global url has been set.
		String url = sortOutSolrURL(globalSolrURL);
		
		// Doc load.
		OptimizeSolrDocumentLoader optimizer = new OptimizeSolrDocumentLoader(url);
		optimizer.load();
	}
	
	/**
	 * Used for generating output for units tests in other languages.
	 * Output some JSON graph serializations.
	 * 
	 * NOTE: In order to cut down on clutter, this method depends on relative
	 * resources in the test/resources. This may change later on--be warned.
	 * 
	 * @param opts
	 * @throws Exception
	 */
	@CLIMethod("--solr-shunt-test")
	public void dumpShuntGraphs(Opts opts) throws Exception {

		File here = new File(".");
		String here_str = null;
		try {
			here_str = here.getCanonicalPath();
		} catch (IOException e) {
			LOG.warn("Apparently, no \"here\" to be had...");
			e.printStackTrace();
		}
		LOG.warn("NOTE: In order to cut down on clutter, this method depends on relative ("+ here_str +") resources in the test/resources. This may change later on--be warned.");

		// A trivial output.
		OWLShuntGraph g1 = new OWLShuntGraph();
		g1.addNode(new OWLShuntNode("a", "A"));
		g1.addNode(new OWLShuntNode("b", "B"));
		g1.addEdge(new OWLShuntEdge("a", "b"));
		System.out.println(g1.toJSON());

		// TODO: A more realistic output (the segement graph is still partial).
		OWLGraphWrapper wrapper = getOntologyWrapper("go.owl");		
		OWLObject c = wrapper.getOWLClass(OWLGraphWrapper.DEFAULT_IRI_PREFIX + "GO_0022008");
		List<String> rel_ids = RelationSets.getRelationSet(RelationSets.COMMON);
		OWLShuntGraph g2 = wrapper.getSegmentShuntGraph(c, rel_ids);
		System.out.println(g2.toJSON());
	}
	private OWLGraphWrapper getOntologyWrapper(String file) throws OWLOntologyCreationException{
		ParserWrapper pw = new ParserWrapper();
		OWLOntologyManager manager = pw.getManager();
		OWLOntology ontology = manager.loadOntologyFromOntologyDocument(getTestResource(file));
		return new OWLGraphWrapper(ontology);
	}
	private File getTestResource(String name) {
		// TODO: Replace this with a mechanism not relying on the relative path--see above
		File file = new File("../OWLTools-Core/src/test/resources/" + name);
		return file;
	}

	/*
	 * Convert all solr URL handling through here.
	 */
	//private String sortOutSolrURL(Opts opts, String globalSolrURL) throws Exception {
	private String sortOutSolrURL(String globalSolrURL) throws Exception {

		String url = null;
		if( globalSolrURL == null ){
			//url = opts.nextOpt();
		}else{
			url = globalSolrURL;
		}
		LOG.info("Use GOlr server at: " + url);

		if( url == null ){
			throw new Exception();
		}
		
		return url;
	}

	/*
	 * Define the time right now.
	 * DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
	 */
	private String dateNow(){		
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		String now = df.format(new Date());
		return now;
	}
	
	/*
	 * Log out the URL, type, and date. 
	 */
	private void optionallyLogLoad(String type, String uri, String version){

		// Naturally, if we haven't defined the file, skip the logging.
		if( globalSolrLogFile != null ){

			// Actually logging, don in an instant.
			String ll = type + "\t" + version + "\t" + dateNow() + "\t" + uri + "\n";
			try {
				FileUtils.writeStringToFile(globalSolrLogFile, ll, true);
				LOG.info("Should be logging GOlr load at: " + globalSolrLogFile.getAbsolutePath());
			} catch (IOException e) {
				LOG.info("Unable to write to GOlr load log file at: " + globalSolrLogFile.getAbsolutePath());
				e.printStackTrace();
			}
		}else{
			LOG.info("Skip logging: No GOlr load log specified.");			
		}
	}
	
	/*
	 * Wrapper multiple places where there is direct GAF loading.
	 */
	private void loadGAFDoc(String url, GafDocument gafdoc, EcoTools eco, TaxonTools taxo, PANTHERForest pset, String taxonSubsetName, String ecoSubsetName) throws IOException, SolrServerException{

		// Seth's head explodes with non-end return!
		// TODO: Ask Chris if there is any reason to have null on empty GAFs.
		// Better to just have an empty doc that we can still get meta-information out of.
		if( gafdoc == null ){
			LOG.warn("Huh, it looks like I'm going to skip an empty GAF...");// + gafdoc.getDocumentPath());
			return;
		}
		
		// Doc load.
		boolean isMock = false;
		GafSolrDocumentLoader loader;
		if (url.equals("mock")) {
		    loader = new MockGafSolrDocumentLoader();
		    isMock = true;
		}
		else {
		    loader = new GafSolrDocumentLoader(url);
		}
		loader.setEcoTools(eco);
		loader.setTaxonTools(taxo);
		loader.setPANTHERSet(pset);
		loader.setGafDocument(gafdoc);
		loader.setTaxonSubsetName(taxonSubsetName);
		loader.setEcoSubsetName(ecoSubsetName);
		loader.setGraph(g);
		LOG.info("Loading server at: " + url + " with: " + gafdoc.getDocumentPath());
		loader.load();

        if (isMock) {
            showMockDocs((MockSolrDocumentLoader) loader);
        }
	}
	
	   // intended for debugging
    private void showMockDocs(MockSolrDocumentLoader loader) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        for (Map<String, Object> d : loader.getDocumentCollection().getDocuments()) {
            String json = gson.toJson(d);
            System.out.println(json);                  
        }
    }
}
