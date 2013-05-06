package owltools.cli;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.common.SolrException;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import com.google.gson.Gson;

import owltools.cli.tools.CLIMethod;
import owltools.flex.FlexCollection;
import owltools.gaf.EcoTools;
import owltools.gaf.GafDocument;
import owltools.gaf.GafObjectsBuilder;
import owltools.gaf.TaxonTools;
import owltools.graph.OWLGraphWrapper;
import owltools.graph.shunt.OWLShuntEdge;
import owltools.graph.shunt.OWLShuntGraph;
import owltools.graph.shunt.OWLShuntNode;
import owltools.panther.PANTHERForest;
import owltools.panther.PANTHERTree;
import owltools.solrj.FlexSolrDocumentLoader;
import owltools.solrj.GafSolrDocumentLoader;
import owltools.solrj.OntologyGeneralSolrDocumentLoader;
import owltools.solrj.OntologySolrLoader;
import owltools.solrj.OptimizeSolrDocumentLoader;
import owltools.solrj.PANTHERGeneralSolrDocumentLoader;
import owltools.solrj.PANTHERSolrDocumentLoader;
import owltools.yaml.golrconfig.ConfigManager;
import owltools.yaml.golrconfig.SolrSchemaXMLWriter;

/**
 *  Solr/GOlr loading.
 */	
@SuppressWarnings("deprecation")
public class SolrCommandRunner extends TaxonCommandRunner {

	private static final Logger LOG = Logger.getLogger(SolrCommandRunner.class);
	
	private String globalSolrURL = null;
	private ConfigManager aconf = null;
	private PANTHERForest pSet = null;

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
		globalSolrURL = opts.nextOpt(); // shift it off of null
		LOG.info("Globally use GOlr server at: " + globalSolrURL);
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
	public void purgeSolr(Opts opts) throws Exception {

		// Check to see if the global url has been set.
		String url = sortOutSolrURL(globalSolrURL);				

		// Wipe out the solr index at url.
		SolrServer server = new CommonsHttpSolrServer(url);
		try {
			server.deleteByQuery("*:*");
		} catch (SolrServerException e) {
			LOG.info("Purge at: " + url + " failed!");
			e.printStackTrace();
		}
		LOG.info("Purged: " + url);
	}
	
	/**
	 * Used for loading whatever ontology stuff we have into GOlr.
	 * 
	 * @param opts 
	 * @throws Exception
	 */
	@Deprecated
	@CLIMethod("--solr-load-ontology-old")
	public void loadOntologySolr(Opts opts) throws Exception {
		// Check to see if the global url has been set.
		String url = sortOutSolrURL(globalSolrURL);				

		// Actual ontology class loading.
		try {
			OntologySolrLoader loader = new OntologySolrLoader(url, g);
			loader.load();
		} catch (SolrServerException e) {
			LOG.info("Ontology load at: " + url + " failed!");
			e.printStackTrace();
		}
	}
	
	/**
	 * Experimental Flexible loader for ontologies.
	 * 
	 * @param opts
	 * @throws Exception
	 */
	@CLIMethod("--solr-load-ontology")
	public void flexLoadOntologySolr(Opts opts) throws Exception {

		// Check to see if the global url has been set.
		String url = sortOutSolrURL(globalSolrURL);				

		// Grab the intermediate form.
		LOG.info("Assembling FlexCollection...");
		FlexCollection flex = new FlexCollection(aconf, g);
		
		// Actual ontology class loading.
		try {
			FlexSolrDocumentLoader loader = new FlexSolrDocumentLoader(url, flex);
			LOG.info("Trying ontology flex load.");
			loader.load();
		} catch (SolrServerException e) {
			LOG.info("Ontology load at: " + url + " failed!");
			e.printStackTrace();
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
		try {
			OntologyGeneralSolrDocumentLoader loader = new OntologyGeneralSolrDocumentLoader(url, g);
			LOG.info("Trying ontology general load.");
			loader.load();
		} catch (SolrServerException e) {
			LOG.info("Ontology load at: " + url + " failed!");
			e.printStackTrace();
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
		// Check to see if the global url has been set.
		String url = sortOutSolrURL(globalSolrURL);
		
		// We should already have added the reasoner elsewhere on the commandline,
		// So there should be real no extra overhead here.
		EcoTools eco = new EcoTools(g, g.getReasoner(), true);
		TaxonTools taxo = new TaxonTools(g, g.getReasoner(), true);
		
		List<String> files = opts.nextList();
		for (String file : files) {
			LOG.info("Parsing GAF: [" + file + "]");
			GafObjectsBuilder builder = new GafObjectsBuilder();
			gafdoc = builder.buildDocument(file);
			loadGAFDoc(url, gafdoc, eco, taxo, pSet);
		}
		
		eco.dispose();
		taxo.dispose();
	}
	
	/**
	 * Requires the --gaf argument (or something else that fills the gafdoc object).
	 * 
	 * @param opts
	 * @throws Exception
	 */
	@CLIMethod("--solr-load-gaf")
	public void loadGafSolr(Opts opts) throws Exception {
		// Double check we're not going to do something silly, like try and
		// use a null variable...
		if( gafdoc == null ){
			System.err.println("No GAF document defined (maybe use '--gaf GAF-FILE') ");
			exit(1);
		}

		// We should already have added the reasoner elsewhere on the commandline,
		// So there should be real no extra overhead here.
		EcoTools eco = new EcoTools(g, g.getReasoner(), true);
		TaxonTools taxo = new TaxonTools(g, g.getReasoner(), true);

		// Check to see if the global url has been set.
		String url = sortOutSolrURL(globalSolrURL);
		// Doc load.
		loadGAFDoc(url, gafdoc, eco, taxo, pSet);

		eco.dispose();
		taxo.dispose();
	}
		
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
	@CLIMethod("--solr-load-gpads")
	public void loadGPADsSolr(Opts opts) throws Exception {
		// Check to see if the global url has been set.
		String url = sortOutSolrURL(globalSolrURL);

		List<String> files = opts.nextList();
		if( files.size() % 2 != 0 ){
			System.err.println("GPAD format comes in pairs; skipping...");
		}else{
			while( ! files.isEmpty() ){
				String car = files.remove(0);
				String cdr = files.remove(0);
				LOG.info("Parsing GPAD car: " + car);
				LOG.info("Parsing GPAD cdr: " + cdr);
				// TODO: a new buildDocument that takes the two GPAD arguments.
				//GafObjectsBuilder builder = new GafObjectsBuilder();
				//gafdoc = builder.buildDocument(car, cdr);
				//loadGAFDoc(url, gafdoc);
			}
		}
	}
	
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
		OWLShuntGraph g2 = wrapper.getSegmentShuntGraph(c);
		System.out.println(g2.toJSON());
	}
	private OWLGraphWrapper getOntologyWrapper(String file) throws OWLOntologyCreationException{
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
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
	 * Wrapper multiple places where there is direct GAF loading.
	 */
	private void loadGAFDoc(String url, GafDocument gafdoc, EcoTools eco, TaxonTools taxo, PANTHERForest pset) throws IOException{

		// Seth's head explodes with non-end return!
		// TODO: Ask Chris if there is any reason to have null on empty GAFs.
		// Better to just have an empty doc that we can still get meta-information out of.
		if( gafdoc == null ){
			LOG.warn("Huh, it looks like I'm going to skip an empty GAF...");// + gafdoc.getDocumentPath());
			return;
		}
		
		// Doc load.
		GafSolrDocumentLoader loader = new GafSolrDocumentLoader(url);
		loader.setEcoTools(eco);
		loader.setTaxonTools(taxo);
		loader.setPANTHERSet(pset);
		loader.setGafDocument(gafdoc);
		loader.setGraph(g);
		try {
			LOG.info("Loading server at: " + url + " with: " + gafdoc.getDocumentPath());
			loader.load();
		} catch (java.lang.NullPointerException e) { // can trigger when the GAF is empty
			LOG.warn("Huh...some null pointer exception...good luck! At: " + url + ", " + gafdoc.getDocumentPath());
			//LOG.warn("Message: " + e.getMessage());
			e.printStackTrace();
		} catch (SolrException e) { // can trigger when there is more than one PANTHER tree
			LOG.warn("Possible PANTHER error: " + url + " with: " + e.toString());
			e.printStackTrace();
		} catch (SolrServerException e) {
			LOG.warn("Something has gone south with Solr: " + url);
			e.printStackTrace();
		}
	}
}
