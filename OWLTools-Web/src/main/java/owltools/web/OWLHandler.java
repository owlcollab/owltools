package owltools.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.coode.owlapi.obo.parser.OBOOntologyFormat;
import org.obolibrary.macro.ManchesterSyntaxTool;
import org.semanticweb.owlapi.expression.ParserException;
import org.semanticweb.owlapi.io.OWLFunctionalSyntaxOntologyFormat;
import org.semanticweb.owlapi.io.OWLXMLOntologyFormat;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.frame.jsonld.ClassFrameLD;
import owltools.frame.jsonld.FrameMakerLD;
import owltools.frame.jsonld.OntologyFrameLD;
import owltools.gaf.inference.TaxonConstraintsEngine;
import owltools.gfx.GraphicsConfig;
import owltools.gfx.OWLGraphLayoutRenderer;
import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;
import owltools.io.OWLGsonRenderer;
import owltools.io.OWLPrettyPrinter;
import owltools.io.ParserWrapper;
import owltools.mooncat.Mooncat;
import owltools.mooncat.ontologymetadata.OntologySetMetadata;
import owltools.sim2.FastOwlSim;
import owltools.sim2.OwlSim;
import owltools.sim2.OwlSim.ScoreAttributeSetPair;
import owltools.sim2.OwlSimMetadata;
import owltools.sim2.SimJSONEngine;
import owltools.sim2.UnknownOWLClassException;
import owltools.version.VersionInfo;
import owltools.vocab.OBOUpperVocabulary;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * See http://code.google.com/p/owltools/wiki/WebServices
 * 
 * <h2>Conventions</h2>
 * 
 * Any method whose method name ends in "Command" can be called as a web API call
 * 
 * For example, a call http://localhost:9031/getSubClasses?id=GO:0005634
 * 
 * runs {@link getSubClassesCommand()} 
 * 
 * Note the java method takes no arguments. For each web API call, a new OWLHandler object is created,
 * with a new HttpServletRequest object, containing the http parameters of the web API call.
 * 
 * In this case the getSubClassesCommand() calls getParam("id") and uses the result to make the query 
 * using the OWLAPI reasoner API.
 * 
 * The results of any of the API calls can be returned in a variety of formats including plaintext, json,
 * or any OWL syntax. The implementing java method generally does not care about the format, it performs
 * its task and calls the output(...) method on each result.
 * 
 * <h2>Future plans</h2>
 *
 * This API may be superseded by a Web API based on guice or similar framework
 * 
 * @author cjm
 *
 */
public class OWLHandler {

	private static Logger LOG = Logger.getLogger(OWLHandler.class);

	private OWLGraphWrapper graph;
	private	HttpServletResponse response;
	private	HttpServletRequest request;
	private OWLPrettyPrinter owlpp;
	private OWLServer owlserver;
	private String format = null;
	private String commandName;
	private OwlSim sos;

	// consider replacing with a generic result list
	private Set<OWLAxiom> cachedAxioms = new HashSet<OWLAxiom>();
	private Set<OWLObject> cachedObjects = new HashSet<OWLObject>();

	// not yet implemented --
	// for owl, edges are translated to expressions.
	//          axioms are added to an ontology
	//           expressions? temp ontology?
	public enum ResultType {
		AXIOM, ENTITY, EDGE, ONTOLOGY
	}
	public enum Param {
		id, iri, label, taxid, expression,
		format, direct, reflexive, target,
		limit,
		ontology,
		classId,
		individualId,
		propertyId,
		fillerId,
		a, b, modelId, db,
		/**
		 * r : root node of subgraph, class id
		 */
		r
	}
	
	public class ServerMetadata {
		OntologySetMetadata ontologySetMetadata;
		OwlSimMetadata owlSimMetadata;
		String serverManifest;
		long totalMemoryInKB;
		long freeMemoryInKB;
		long memoryUsedInKB;
		
		public void setMemoryUsage() {
			Runtime rt = Runtime.getRuntime();
			totalMemoryInKB = rt.totalMemory() / 1024;
			freeMemoryInKB = rt.freeMemory() / 1024;
			memoryUsedInKB = (rt.totalMemory() - rt.freeMemory()) / 1024;
			LOG.info("mem used = "+memoryUsedInKB);
		}
		
	}

	public OWLHandler(OWLServer owlserver, OWLGraphWrapper graph,  HttpServletRequest request, HttpServletResponse response) throws IOException {
		super();
		this.owlserver = owlserver;
		this.graph = graph;
		this.request = request;
		this.response = response;
		//this.writer = response.getWriter();
		this.owlpp = new OWLPrettyPrinter(graph);
	}

	public String getFormat() {
		String fmtParam = getParam(Param.format);
		if (fmtParam != null && !fmtParam.equals(""))
			return fmtParam;
		if (format == null)
			return "txt";
		return format;
	}




	public void setOwlSim(OwlSim sos2) {
		this.sos = sos2;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public String getCommandName() {
		return commandName;
	}

	public void setCommandName(String commandName) {
		this.commandName = commandName;
	}

	// ----------------------------------------
	// COMMANDS
	// ----------------------------------------

	public void topCommand() throws OWLOntologyCreationException, OWLOntologyStorageException, IOException, UnknownOWLClassException {
		if (isHelp()) {
			info("Basic metadata about current ontology"); // TODO - json
			return;
		}
		OntologySetMetadata osmd = new OntologySetMetadata(this.getOWLOntology());
		String manifestVersion = VersionInfo.getManifestVersion("owltools-build-timestamp");
		osmd.serverManifestVersion = manifestVersion == null ? "unknown" : manifestVersion;
		ServerMetadata smd = new ServerMetadata();
		smd.ontologySetMetadata = osmd;
		smd.setMemoryUsage();
		smd.serverManifest = osmd.serverManifestVersion;
//		if (this.getOWLSim() != null) {
//			smd.owlSimMetadata = this.getOWLSim().getMetadata();
//		}
		returnJSON(smd);

	}
	
	// for testing only
	public void memCommand() throws OWLOntologyCreationException, OWLOntologyStorageException, IOException, UnknownOWLClassException {
		if (isHelp()) {
			info("Basic metadata about current ontology"); // TODO - json
			return;
		}
		ServerMetadata smd = new ServerMetadata();
		smd.setMemoryUsage();
		response.getWriter().write("Used: "+smd.memoryUsedInKB);
	}
	
	// for testing only
	public void threadDumpCommand() throws Exception {
		if (isHelp()) {
			info("WARNING: Will create a thread dump of the system.");
			return;
		}
		LOG.info("Start creating ThreadDump");
		ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        ThreadInfo[] threadInfos = threadMXBean.dumpAllThreads(true, true);
        PrintWriter writer = response.getWriter();
        for (ThreadInfo threadInfo : threadInfos) {
        	writer.append(toString(threadInfo));
        }
        LOG.info("Finished creating ThreadDump");
	}
	

	private static CharSequence toString(ThreadInfo threadInfo) {
		StringBuilder sb = new StringBuilder("\"" + threadInfo.getThreadName() + "\"" + " Id=" + threadInfo.getThreadId() + " " + threadInfo.getThreadState());
		if (threadInfo.getLockName() != null) {
			sb.append(" on " + threadInfo.getLockName());
		}
		if (threadInfo.getLockOwnerName() != null) {
			sb.append(" owned by \"" + threadInfo.getLockOwnerName() + "\" Id=" + threadInfo.getLockOwnerId());
		}
		if (threadInfo.isSuspended()) {
			sb.append(" (suspended)");
		}
		if (threadInfo.isInNative()) {
			sb.append(" (in native)");
		}
		sb.append('\n');
		StackTraceElement[] stackTrace = threadInfo.getStackTrace();
		int i = 0;
		for (; i < stackTrace.length; i++) {
			StackTraceElement ste = stackTrace[i];
			sb.append("\tat " + ste.toString());
			sb.append('\n');
			if (i == 0 && threadInfo.getLockInfo() != null) {
				Thread.State ts = threadInfo.getThreadState();
				switch (ts) {
				case BLOCKED:
					sb.append("\t-  blocked on " + threadInfo.getLockInfo());
					sb.append('\n');
					break;
				case WAITING:
					sb.append("\t-  waiting on " + threadInfo.getLockInfo());
					sb.append('\n');
					break;
				case TIMED_WAITING:
					sb.append("\t-  waiting on " + threadInfo.getLockInfo());
					sb.append('\n');
					break;
				default:
				}
			}

			for (MonitorInfo mi : threadInfo.getLockedMonitors()) {
				if (mi.getLockedStackDepth() == i) {
					sb.append("\t-  locked " + mi);
					sb.append('\n');
				}
			}
		}
		LockInfo[] locks = threadInfo.getLockedSynchronizers();
		if (locks.length > 0) {
			sb.append("\n\tNumber of locked synchronizers = " + locks.length);
			sb.append('\n');
			for (LockInfo li : locks) {
				sb.append("\t- " + li);
				sb.append('\n');
			}
		}
		sb.append('\n');
		return sb;
	}


	public void helpCommand() throws IOException {
		headerHTML();
		List<String> commands = new ArrayList<String>();
		outputLine("<ul>");
		for (Method m : this.getClass().getMethods()) {
			String mn = m.getName();
			if (mn.endsWith("Command")) {
				String c = mn.replace("Command", "");
				commands.add(c);
				outputLine("<li>"+c+"</li>");
			}
		}
		outputLine("</ul>");
	}

	/**
	 * Params: direct, (id | expression)
	 * @throws OWLOntologyCreationException
	 * @throws OWLOntologyStorageException
	 * @throws IOException
	 * @throws ParserException 
	 */
	public void getSubClassesCommand() throws OWLOntologyCreationException, OWLOntologyStorageException, IOException, ParserException {
		headerOWL();
		boolean direct = getParamAsBoolean(Param.direct, false);
		OWLReasoner r = getReasoner();
		OWLClassExpression cls = this.resolveClassExpression();
		LOG.info("finding subclasses of: "+cls+" using "+r);
		int n = 0;
		for (OWLClass sc : r.getSubClasses(cls, direct).getFlattened()) {
			output(sc);
			n++;
		}
		if (getParamAsBoolean(Param.reflexive, true)) {
			for (OWLClass sc : r.getEquivalentClasses(cls)) {
				output(sc);
			}
			n++;
		}
		LOG.info("results: "+n);
	}

	/**
	 * Params: direct, (id | expression)
	 * @throws OWLOntologyCreationException
	 * @throws OWLOntologyStorageException
	 * @throws IOException
	 * @throws ParserException 
	 */
	public void getSuperClassesCommand() throws OWLOntologyCreationException, OWLOntologyStorageException, IOException, ParserException {
		headerOWL();
		boolean direct = getParamAsBoolean(Param.direct, false);
		OWLReasoner r = getReasoner();
		OWLClassExpression cls = this.resolveClassExpression();
		LOG.info("finding superclasses of: "+cls);
		LOG.info("R:"+r.getSuperClasses(cls, direct));
		for (OWLClass sc : r.getSuperClasses(cls, direct).getFlattened()) {
			output(sc);
		}
		if (getParamAsBoolean(Param.reflexive, true)) {
			for (OWLClass sc : r.getEquivalentClasses(cls)) {
				output(sc);
			}	
		}
	}

	/**
	 * Params: id
	 * @throws OWLOntologyCreationException
	 * @throws OWLOntologyStorageException
	 * @throws IOException
	 * @throws ParserException
	 */
	public void getAxiomsCommand() throws OWLOntologyCreationException, OWLOntologyStorageException, IOException, ParserException {
		headerOWL();
		boolean direct = getParamAsBoolean(Param.direct, false);
		OWLObject obj = this.resolveEntity();
		LOG.info("finding axioms about: "+obj);
		Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
		if (obj instanceof OWLClass) {
			axioms.addAll(graph.getSourceOntology().getAxioms((OWLClass)obj));
		}
		if (obj instanceof OWLIndividual) {
			axioms.addAll(graph.getSourceOntology().getAxioms((OWLIndividual)obj));
		}
		if (obj instanceof OWLObjectProperty) {
			axioms.addAll(graph.getSourceOntology().getAxioms((OWLObjectProperty)obj));
		}

		for (OWLAxiom ax : axioms) {
			output(ax);
		}
	}



	/**
	 * Params: direct
	 * @throws OWLOntologyCreationException
	 * @throws OWLOntologyStorageException
	 * @throws IOException
	 */
	public void allSubClassOfCommand() throws OWLOntologyCreationException, OWLOntologyStorageException, IOException {
		headerOWL();
		boolean direct = getParamAsBoolean(Param.direct, true);
		OWLReasoner r = getReasoner();
		for (OWLClass c : getOWLOntology().getClassesInSignature(true)) {
			for (OWLClass sc : r.getSuperClasses(c, direct).getFlattened()) {
				output(graph.getDataFactory().getOWLSubClassOfAxiom(c, sc));
			}
		}
	}

	public void getOutgoingEdgesCommand() throws IOException, OWLOntologyCreationException, OWLOntologyStorageException {
		if (isHelp()) {
			info("Returns paths to reachable nodes in ontology graph");
			return;
		}
		headerOWL();
		boolean isClosure = getParamAsBoolean("closure", true);
		boolean isReflexive = getParamAsBoolean("reflexive", true);
		OWLObject obj = this.resolveEntity();
		LOG.info("finding edges from: "+obj);
		for (OWLGraphEdge e : graph.getOutgoingEdges(obj,isClosure,isReflexive)) {
			output(e);
		}
	}




	/**
	 * information about an ontology object (class, property, individual)
	 * 
	 * @throws OWLOntologyCreationException 
	 * @throws OWLOntologyStorageException 
	 * @throws IOException 
	 */
	public void aboutCommand() throws OWLOntologyCreationException, OWLOntologyStorageException, IOException {
		if (isHelp()) {
			info("Returns logical relationships and metadata associated with an ontology object");
			return;
		}
		headerOWL();
		String id = this.getParam(Param.id);
		OWLClass cls = graph.getOWLClassByIdentifier(id);
		for (OWLAxiom axiom : getOWLOntology().getAxioms(cls)) {
			output(axiom);
		}
		for (OWLAxiom axiom : getOWLOntology().getAnnotationAssertionAxioms(cls.getIRI())) {
			output(axiom);
		}
	}

	public void classCommand() throws OWLOntologyCreationException, OWLOntologyStorageException, IOException {
		if (isHelp()) {
			info("Returns json object describing a class");
			return;
		}
		String id = this.getParam(Param.id);
		OWLClass cls = graph.getOWLClassByIdentifier(id);
		FrameMakerLD fm = new FrameMakerLD(graph);
		ClassFrameLD f = fm.makeClassFrame(cls);
		returnJSON(f);
	}

	public void classSetCommand() throws OWLOntologyCreationException, OWLOntologyStorageException, IOException {
		if (isHelp()) {
			info("Returns json object describing a set of classes");
			return;
		}
		String[] ids = this.getParams(Param.id);
		Set<OWLClass> cset = new HashSet<OWLClass>();
		for (String id : ids) {
			OWLClass cls = graph.getOWLClassByIdentifier(id);
			cset.add(cls);
		}

		FrameMakerLD fm = new FrameMakerLD(graph);
		OntologyFrameLD f = fm.makeOntologyFrame(cset);
		returnJSON(f);
	}

	/**
	 * visualize using QuickGO graphdraw. 
	 * 
	 * @throws OWLOntologyCreationException 
	 * @throws OWLOntologyStorageException 
	 * @throws IOException 
	 */
	public void qvizCommand() throws OWLOntologyCreationException, OWLOntologyStorageException, IOException {
		String fmt = "png";
		headerImage(fmt);
		GraphicsConfig gfxCfg = new GraphicsConfig();
		Set<OWLObject> objs = resolveEntityList();
		OWLGraphLayoutRenderer r = new OWLGraphLayoutRenderer(graph);
		r.graphicsConfig = gfxCfg;

		r.addObjects(objs);
		r.renderImage(fmt, response.getOutputStream());
	}

	/**
	 * generates a sub-ontology consisting only of classes specified using the id param.
	 * If the include_ancestors param is true, then the transitive closure of the input classes is
	 * included. otherwise, intermediate classes are excluded and paths are filled.
	 * 
	 * @throws OWLOntologyCreationException 
	 * @throws OWLOntologyStorageException 
	 * @throws IOException 
	 * @see Mooncat#makeMinimalSubsetOntology(Set, IRI)
	 */
	public void makeSubsetOntologyCommand() throws OWLOntologyCreationException, OWLOntologyStorageException, IOException {
		headerOWL();
		Set<OWLClass> objs = resolveClassList();
		Set<OWLClass> tObjs = new HashSet<OWLClass>();
		if (getParamAsBoolean("include_ancestors")) {
			// TODO - more more efficient
			for (OWLClass obj : objs) {
				for (OWLObject t : graph.getAncestorsReflexive(obj)) {
					tObjs.add((OWLClass)t);
				}
			}
		}
		else {
			tObjs = objs;
		}
		Mooncat mooncat;
		mooncat = new Mooncat(graph);
		OWLOntology subOnt = 
				mooncat.makeMinimalSubsetOntology(tObjs,
						IRI.create("http://purl.obolibrary.org/obo/temporary"));
		for (OWLAxiom axiom : subOnt.getAxioms()) {
			output(axiom); // TODO
		}
		graph.getManager().removeOntology(subOnt);
	}

	/**
	 * tests which of a set of input classes (specified using id) is applicable for a set of taxa
	 * (specified using taxid)
	 * 
	 * @throws OWLOntologyCreationException 
	 * @throws OWLOntologyStorageException 
	 * @throws IOException 
	 */
	public void isClassApplicableForTaxonCommand() throws OWLOntologyCreationException, OWLOntologyStorageException, IOException {
		headerOWL();
		TaxonConstraintsEngine tce = new TaxonConstraintsEngine(graph);
		Set<OWLClass> testClsSet = resolveClassList();
		Set<OWLClass> testTaxSet = resolveClassList(Param.taxid);
		for (OWLClass testTax : testTaxSet) {
			Set<OWLObject> taxAncs = graph.getAncestorsReflexive(testTax);
			LOG.info("Tax ancs: "+taxAncs);
			for (OWLClass testCls : testClsSet) {
				Set<OWLGraphEdge> edges = graph.getOutgoingEdgesClosure(testCls);
				boolean isOk = tce.isClassApplicable(testCls, testTax, edges, taxAncs);
				// TODO - other formats
				output(testCls);
				print("\t");
				output(testTax);
				outputLine("\t"+isOk);
			}
		}
	}

	// sim2

	private OwlSim getOWLSim() throws UnknownOWLClassException {
		if (owlserver.sos == null) {
			LOG.info("Creating sim object"); // TODO - use factory
			owlserver.sos = new FastOwlSim(graph.getSourceOntology());
			owlserver.sos.createElementAttributeMapFromOntology();
		}
		return owlserver.sos;
	}

	public void getOwlSimMetadataCommand() throws OWLOntologyCreationException, OWLOntologyStorageException, IOException, UnknownOWLClassException {
		if (isHelp()) {
			info("Basic metadata about current owlsim instance"); // TODO - json
			return;
		}
		OwlSimMetadata osmd = getOWLSim().getMetadata();
		returnJSON(osmd);
	}


	public void getSimilarClassesCommand() throws IOException, OWLOntologyCreationException, OWLOntologyStorageException, UnknownOWLClassException {
		if (isHelp()) {
			info("Returns semantically similar classes using OWLSim2");
			return;
		}
		headerOWL();
		OWLClass a = this.resolveClass();
		OwlSim sos = getOWLSim();
		List<ScoreAttributeSetPair> saps = new ArrayList<ScoreAttributeSetPair>();
		for (OWLClass b : this.getOWLOntology().getClassesInSignature()) {
			double score = sos.getAttributeJaccardSimilarity(a, b);
			saps.add(new ScoreAttributeSetPair(score, b) );
		}
		Collections.sort(saps);
		int limit = 100;

		int n=0;
		for (ScoreAttributeSetPair sap : saps) {
			output(sap.getArbitraryAttributeClass());
			this.outputLine("score="+sap.score); // todo - jsonify
			n++;
			if (n > limit) {
				break;
			}
		}
	}

	public void getSimilarIndividualsCommand() throws IOException, OWLOntologyCreationException, OWLOntologyStorageException, UnknownOWLClassException {
		if (isHelp()) {
			info("Returns matching individuals using OWLSim2");
			return;
		}
		headerOWL();
		OWLNamedIndividual i = (OWLNamedIndividual) this.resolveEntity();
		LOG.info("i="+i);
		if (i == null) {
			LOG.error("Could not resolve id");
			// TODO - throw
			return;
		}
		OwlSim sos = getOWLSim();
		List<ScoreAttributeSetPair> saps = new Vector<ScoreAttributeSetPair>();
		for (OWLNamedIndividual j : this.getOWLOntology().getIndividualsInSignature()) {
			// TODO - make configurable
			ScoreAttributeSetPair match = sos.getSimilarityMaxIC(i, j);
			saps.add(match);
		}
		Collections.sort(saps);
		int limit = 100; // todo - configurable

		int n=0;
		for (ScoreAttributeSetPair sap : saps) {
			//output(sap.attributeClass); TODO
			this.outputLine("score="+sap.score); // todo - jsonify
			n++;
			if (n > limit) {
				break;
			}
		}
	}

	public void getLowestCommonSubsumersCommand() throws IOException, OWLOntologyCreationException, OWLOntologyStorageException, UnknownOWLClassException {
		if (isHelp()) {
			info("Returns LCSs using sim2");
			return;
		}
		headerOWL();
		OwlSim sos = getOWLSim();

		Set<OWLObject> objs = this.resolveEntityList();
		if (objs.size() == 2) {
			Iterator<OWLObject> oit = objs.iterator();
			OWLClass a = (OWLClass) oit.next();
			OWLClass b = (OWLClass) oit.next();
			Set<Node<OWLClass>> lcsNodes = sos.getNamedCommonSubsumers(a, b);
			for (Node<OWLClass> n : lcsNodes) {
				for (OWLClass c : n.getEntities()) {
					output(c);
				}
			}
		}
		else {
			// TODO - throw
		}
	}

	public void compareAttributeSetsCommand() throws Exception {
		if (isHelp()) {
			info("Returns LCSs, their ICs, and sim scores, for two sets of attributes (e.g. phenotypes for disease vs model) using sim2");
			return;
		}
		headerText();
		OwlSim sos = getOWLSim();

		Set<OWLClass> objAs = this.resolveClassList(Param.a);
		Set<OWLClass> objBs = this.resolveClassList(Param.b);
		LOG.info("Comparison set A:"+objAs);
		LOG.info("Comparison set B:"+objBs);

		SimJSONEngine sj = new SimJSONEngine(graph,sos);
		String jsonStr = sj.compareAttributeSetPair(objAs, objBs, true);
		LOG.info("Finished comparison");
		response.setContentType("application/json");
		response.getWriter().write(jsonStr);
	}

	public void searchByAttributeSetCommand() throws Exception {
		if (isHelp()) {
			info("Entities that have a similar attribute profile to the specified one, using sim2");
			return;
		}
		headerText();
		OwlSim sos = getOWLSim();
		Set<OWLClass> atts = this.resolveClassList(Param.a);

		SimJSONEngine sj = new SimJSONEngine(graph,sos);
		String targetIdSpace = getParam(Param.target);
		Integer limit = getParamAsInteger(Param.limit, 1000);
		String jsonStr = sj.search(atts, targetIdSpace, true, limit, true);
		LOG.info("Finished comparison");
		response.setContentType("application/json");
		response.getWriter().write(jsonStr);
	}



	public void getAnnotationSufficiencyScoreCommand() throws IOException, OWLOntologyCreationException, OWLOntologyStorageException, UnknownOWLClassException {
		if (isHelp()) {
			info("Specificity score for a set of annotations");
			return;
		}
		headerText();
		OwlSim sos = getOWLSim();
		Set<OWLClass> atts = this.resolveClassList(Param.a);
		LOG.info("Calculating AnnotationSufficiency score for "+atts.toString());
		SimJSONEngine sj = new SimJSONEngine(graph,sos);
		String jsonStr = sj.getAnnotationSufficiencyScore(atts);
		LOG.info("Finished getAnnotationSufficiencyScore");
		response.setContentType("application/json");
		response.getWriter().write(jsonStr);
	}

	/*
	 * That code is weird!!!!!
	 * 
	 * The idea is to guard an expensive initialization with a status.
	 */
	private enum InitStatus {
		never,
		busy,
		done
	}
	private static volatile InitStatus status = InitStatus.never;
	private static final Object MUTEX = new Object();
	
	public void getAttributeInformationProfileCommand() throws Exception {
		if (isHelp()) {
			info("Attribute Profile Information");
			return;
		}
		OwlSim sos = getOWLSim();
		Set<OWLClass> atts = this.resolveClassList(Param.a);
		Set<OWLClass> roots = this.resolveClassList(Param.r);
		SimJSONEngine sj = new SimJSONEngine(graph,sos);
		// check for init of slow stuff
		if (status != InitStatus.done) {
			synchronized (MUTEX) {
				if (status == InitStatus.busy) {
					// option busy -> send error back, do nothing
					response.sendError(HttpServletResponse.SC_REQUEST_TIMEOUT, "Server is currently initializing.");
					return; // exit!!!!
				}
				else if (status == InitStatus.never) {
					// option not done -> do it now, set info for busy
					status = InitStatus.busy;
				}
			} // end synchronized
		}
		String jsonStr;
		try {
			// this is the normal calculation, which as a side effect also does the init
			jsonStr = sj.getAttributeInformationProfile(atts,roots);
		}
		catch (Exception e) {
			// in case of error during the init: reset
			synchronized (MUTEX) {
				if (status == InitStatus.busy) {
					status = InitStatus.never;
				}
			}
			throw e;
		}
		finally {
			// all is well
			synchronized (MUTEX) {
				if (status == InitStatus.busy) {
					status = InitStatus.done;
				}
			}
			
		}
		// send result
		LOG.info("Finished getAttributeInformationProfileCommand");
		headerText();
		response.setContentType("application/json");
		response.getWriter().write(jsonStr);
	}
	
	public void getCoAnnotatedClassesCommand() throws Exception {
		if (isHelp()) {
			info("Fetch commonly co-annotated classes based on one or more attributes");
			return;
		}
		headerText();
		OwlSim sos = getOWLSim();
		Set<OWLClass> atts = this.resolveClassList(Param.a);

		SimJSONEngine sj = new SimJSONEngine(graph,sos);
		Integer limit = getParamAsInteger(Param.limit, 10);
		String jsonStr = "{}";

		//TODO allow user to specify a delimited list and get back multiple sets of coannotations
		//for now,just use the first one
		if (atts.size() > 0) {
			jsonStr = sj.getCoAnnotationListForAttribute(atts.iterator().next(),limit);
			LOG.info("Finished getting co-annotation list");
		} else {
			LOG.error("No classes specified to fetch co-annotation classes");
		}
		response.setContentType("application/json");
		response.getWriter().write(jsonStr);
		
	}
	
	//TODO the getCoAnnotationListForAttributes when >1 supplied is not working yet
/*	public void getCoAnnotationListForAttributeCommand() throws Exception {
		if (isHelp()) {
			info("Fetch commonly co-annotated classes based on one or more attributes");
			return;
		}
		headerText();
		OwlSim sos = getOWLSim();
		Set<OWLClass> atts = this.resolveClassList(Param.a);

		SimJSONEngine sj = new SimJSONEngine(graph,sos);
		Integer limit = getParamAsInteger(Param.limit, 10);
		String jsonStr = "{}";
		if (atts.size()>1) {
			jsonStr = sj.getCoAnnotationListForAttributes(atts, limit);
		} else if (atts.size() == 1) {
			jsonStr = sj.getCoAnnotationListForAttribute(atts.iterator().next());
		} 
		LOG.info("Finished getting co-annotation list, going to write json:");
		response.setContentType("application/json");
		response.getWriter().write(jsonStr);
	}
	*/

	// ----------------------------------------
	// WRITE/UPDATE OPERATIONS
	// ----------------------------------------

	@Deprecated
	public void assertTypeCommand() throws IOException, OWLOntologyCreationException, OWLOntologyStorageException, UnknownOWLClassException {
		if (isHelp()) {
			info("generates ClassAssertion");
			return;
		}
		OWLOntology ont = resolveOntology(Param.ontology);
		OWLClass c = resolveClass(Param.classId);
		OWLIndividual i = resolveIndividual(Param.individualId);
		addAxiom(ont, graph.getDataFactory().getOWLClassAssertionAxiom(c, i));
		String jsonStr = "";
		response.getWriter().write(jsonStr);
	}

	@Deprecated
	public void assertFactCommand() throws IOException, OWLOntologyCreationException, OWLOntologyStorageException, UnknownOWLClassException {
		if (isHelp()) {
			info("generates ClassAssertion");
			return;
		}
		OWLOntology ont = resolveOntology(Param.ontology);
		OWLIndividual i = resolveIndividual(Param.individualId);
		OWLIndividual j = resolveIndividual(Param.fillerId);
		OWLObjectProperty p = resolveObjectProperty(Param.propertyId);
		addAxiom(ont, graph.getDataFactory().getOWLObjectPropertyAssertionAxiom(p, i, j));
		String jsonStr = "";
		response.getWriter().write(jsonStr);
	}

	@Deprecated
	public void deleteFactCommand() throws IOException, OWLOntologyCreationException, OWLOntologyStorageException, UnknownOWLClassException {
		if (isHelp()) {
			info("generates ClassAssertion");
			return;
		}
		OWLOntology ont = resolveOntology(Param.ontology);
		OWLIndividual i = resolveIndividual(Param.individualId);
		OWLIndividual j = resolveIndividual(Param.fillerId);
		OWLObjectProperty p = resolveObjectProperty(Param.propertyId);
		for (OWLObjectPropertyAssertionAxiom ax : ont.getAxioms(AxiomType.OBJECT_PROPERTY_ASSERTION)) {
			if (ax.getSubject().equals(i)) {
				if (p == null || ax.getProperty().equals(p)) {
					if (j == null || ax.getObject().equals(j)) {
						removeAxiom(ont, graph.getDataFactory().getOWLObjectPropertyAssertionAxiom(p, i, j));
					}
				}
			}
		}
		String jsonStr = "";
		response.getWriter().write(jsonStr);
	}

	public void returnJSON(Object obj) throws IOException {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String js = gson.toJson(obj);
		response.getWriter().write(js);
	}

	public void assertEnabledByCommand() throws IOException, OWLOntologyCreationException, OWLOntologyStorageException, UnknownOWLClassException {
		if (isHelp()) {
			info("generates ClassAssertion");
			return;
		}
		OWLOntology ont = resolveOntology(Param.ontology);
		OWLIndividual i = resolveIndividual(Param.individualId);
		OWLClass j = resolveClass(Param.fillerId);
		OWLObjectProperty p = 
				OBOUpperVocabulary.GOREL_enabled_by.getObjectProperty(graph.getDataFactory());
		addSomeValuesFromClassAssertion(ont, i, j, p);
		String jsonStr = "";
		response.getWriter().write(jsonStr);
	}

	public void assertOccursInCommand() throws IOException, OWLOntologyCreationException, OWLOntologyStorageException, UnknownOWLClassException {
		if (isHelp()) {
			info("generates ClassAssertion");
			return;
		}
		OWLOntology ont = resolveOntology(Param.ontology);
		OWLIndividual i = resolveIndividual(Param.individualId);
		OWLClass j = resolveClass(Param.fillerId);
		OWLObjectProperty p = 
				OBOUpperVocabulary.BFO_occurs_in.getObjectProperty(graph.getDataFactory());
		addSomeValuesFromClassAssertion(ont, i, j, p);
		String jsonStr = "";
		response.getWriter().write(jsonStr);
	}





	// ----------------------------------------
	// END OF COMMANDS
	// ----------------------------------------


	// UTIL

	private void addSomeValuesFromClassAssertion(OWLOntology ont,
			OWLIndividual i, OWLClass j, OWLObjectProperty p) {
		OWLDataFactory df = ont.getOWLOntologyManager().getOWLDataFactory();
		OWLObjectSomeValuesFrom svf = 
				df.getOWLObjectSomeValuesFrom(p, j);
		addAxiom(ont, df.getOWLClassAssertionAxiom(svf, i));
	}

	private void addAxiom(OWLOntology ont,
			OWLAxiom ax) {
		LOG.info("Added: " + ax);
		// TODO - rollbacks
		graph.getManager().addAxiom(ont, ax);
	}
	private void removeAxiom(OWLOntology ont,
			OWLAxiom ax) {
		LOG.info("Removed: " + ax);
		// TODO - rollbacks
		graph.getManager().removeAxiom(ont, ax);
	}

	private OWLObject resolveEntity() {
		String id = getParam(Param.id);
		if (!id.contains(":"))
			id = id.replace("_", ":");
		return graph.getOWLObjectByIdentifier(id);
	}

	private OWLClass resolveClass() {
		String id = getParam(Param.id);
		return graph.getOWLClassByIdentifier(id);
	}
	private OWLClass resolveClass(Param p) {
		String id = getParam(p);
		return graph.getOWLClassByIdentifier(id);
	}
	private OWLClass resolveClassByLabel() {
		String id = getParam(Param.label);
		return (OWLClass) graph.getOWLObjectByLabel(id);
	}


	private OWLClassExpression resolveClassExpression() throws ParserException {
		if (hasParam(Param.id)) { 
			return resolveClass();
		}

		String expr = getParam(Param.expression);
		ManchesterSyntaxTool parser = new ManchesterSyntaxTool(graph.getSourceOntology(), graph.getSupportOntologySet());
		return parser.parseManchesterExpression(expr);
	}

	private OWLIndividual resolveIndividual(Param p) {
		String id = getParam(p);
		return graph.getOWLIndividualByIdentifier(id);
	}
	private OWLObjectProperty resolveObjectProperty(Param p) {
		String id = getParam(p);
		return graph.getOWLObjectPropertyByIdentifier(id);
	}


	private Set<OWLObject> resolveEntityList() {
		return resolveEntityList(Param.id);
	}
	private Set<OWLObject> resolveEntityList(Param p) {
		String[] ids = getParams(p);
		Set<OWLObject> objs = new HashSet<OWLObject>();
		for (String id : ids) {
			// TODO - unresolvable
			objs.add(graph.getOWLObjectByIdentifier(id));
		}
		return objs;
	}

	private Set<OWLClass> resolveClassList() {
		return resolveClassList(Param.id);
	}

	private OWLOntology resolveOntology(Param p) {
		String oid = getParam(p);
		for (OWLOntology ont : graph.getManager().getOntologies()) {
			String iri = ont.getOntologyID().getOntologyIRI().toString();
			// HACK
			if (iri.endsWith("/"+oid)) {
				return ont;
			}
		}
		return null;
	}

	private Set<OWLClass> resolveClassList(Param p) {
		Set<OWLClass> objs = new HashSet<OWLClass>();
		if (getParams(p) != null) {
			ArrayList<String> ids = new ArrayList<String>(Arrays.asList(getParams(p)));

			LOG.info("Param "+p+" IDs: "+ids.toString());
			for (String id : ids) {
				// we allow resolution by altId, if present; in future we
				// may want to check the altId map at this level so we can
				// provide metadata in the payload about any mappings provided.
				// See: https://github.com/monarch-initiative/monarch-app/issues/97
				OWLClass c = graph.getOWLClassByIdentifier(id, true);
				if (c == null) {
					// TODO - strict mode - for now we include unresolvable classes
					IRI iri = graph.getIRIByIdentifier(id);
					c = graph.getDataFactory().getOWLClass(iri);
					LOG.info("Unresolvable id:"+id+". Making temp class element:"+c.toString());
				}
				objs.add(c);
			}
		}
		LOG.info("Num objs: "+objs.size());
		return objs;
	}

	public void info(String msg) throws IOException {
		headerHTML();
		outputLine("<h2>Command: "+this.getCommandName()+"</h2>");
		outputLine(msg);
	}

	public void headerHTML() {
		response.setContentType("text/html;charset=utf-8");
		response.setStatus(HttpServletResponse.SC_OK);

	}

	public void headerText() {
		response.setContentType("text/plain;charset-utf-8");
		response.setStatus(HttpServletResponse.SC_OK);		
	}

	public void headerImage(String fmt) {
		response.setContentType("image/"+fmt);
		response.setStatus(HttpServletResponse.SC_OK);		
	}

	public void headerOWL() {
		if (isOWLOntologyFormat()) {
			LOG.info("using OWL ontology header");
			OWLOntologyFormat ofmt = this.getOWLOntologyFormat();
			if (ofmt instanceof RDFXMLOntologyFormat) {
				response.setContentType("application/rdf+xml;charset-utf-8");				
			}
			else if (ofmt instanceof OWLXMLOntologyFormat) {
				response.setContentType("application/xml;charset-utf-8");				
			}
			else {
				response.setContentType("text/plain;charset-utf-8");
			}
		}
		else {
			response.setContentType("text/plain;charset-utf-8");
		}
		response.setStatus(HttpServletResponse.SC_OK);
	}



	private void output(OWLAxiom axiom) throws IOException {
		String fmt = getFormat();
		if (fmt == null)
			fmt = "";
		if (fmt.equals("plain"))
			outputLine(axiom.toString());
		else if (isOWLOntologyFormat(fmt)) {
			cache(axiom);
		}
		else if (fmt.equals("json")) {
			cache(axiom);
		}
		else
			outputLine(owlpp.render(axiom));
	}

	private void outputLine(String s) throws IOException {
		getWriter().println(s);
	}
	private void print(String s) throws IOException {
		getWriter().print(s);
	}


	private void output(OWLObject obj) throws IOException {
		String fmt = getFormat();
		if (fmt.equals("pretty"))
			outputLine(owlpp.render(obj));
		else if (fmt.equals("json")) {
			//JSONPrinter jsonp = new JSONPrinter(response.getWriter());
			//jsonp.render(obj);
			cache(obj);
		}
		else if (isOWLOntologyFormat()) {
			// TODO - place objects into ontology, eg as subclass of result
			outputLine(obj.toString());
		}
		else {
			// TODO - do this more generically, e.g. within owlpp
			if (getParam("idstyle") != null && obj instanceof OWLNamedObject) {
				if (getParam("idstyle").toLowerCase().equals("obo")) {
					outputLine(graph.getIdentifier(obj));
				}
				else {
					outputLine(obj.toString());
				}
			}
			else 
				print(obj.toString());
		}
	}

	private void output(OWLGraphEdge e) throws IOException {
		if (isOWLOntologyFormat() || getFormat().equals("json")) {
			// todo - json format for edge
			OWLObject x = graph.edgeToTargetExpression(e);
			LOG.info("EdgeToX:"+x);
			output(x);
		}
		else {
			outputLine(owlpp.render(e)); // todo - cache
		}

	}


	/*
	private void cache(OWLAxiom axiom) {
		cachedAxioms.add(axiom);
	}
	 */

	private void cache(OWLObject obj) {
		cachedObjects.add(obj);
	}

	// when the result list is a collection of owl objects, we wait until
	// we have collected everything and then generate the results such that
	// the result conforms to the specified owl syntax.
	// TODO - redo this. Instead generate result objects
	public void printCachedObjects() throws OWLOntologyCreationException, OWLOntologyStorageException, IOException {
		if (getFormat().equals("json")) {
			OWLGsonRenderer jsonp = new OWLGsonRenderer(response.getWriter());
			if (cachedObjects.size() > 0) {
				jsonp.render(cachedObjects);
			}
			else {
				jsonp.render(cachedAxioms);
			}
		}
		else {
			// ontology format
			if (cachedAxioms.size() == 0)
				return;
			OWLOntology tmpOnt = getTemporaryOntology();
			graph.getManager().addAxioms(tmpOnt, cachedAxioms);
			OWLOntologyFormat ofmt = getOWLOntologyFormat();
			LOG.info("Format:"+ofmt);
			ParserWrapper pw = new ParserWrapper();
			//graph.getManager().saveOntology(tmpOnt, ofmt, response.getOutputStream());
			pw.saveOWL(tmpOnt, ofmt, response.getOutputStream());
			graph.getManager().removeOntology(tmpOnt);
			cachedAxioms = new HashSet<OWLAxiom>();
		}
	}

	// always remember to remove
	private OWLOntology getTemporaryOntology() throws OWLOntologyCreationException {
		UUID uuid = UUID.randomUUID();
		IRI iri = IRI.create("http://purl.obolibrary.org/obo/temporary/"+uuid.toString());
		//OWLOntology tmpOnt = graph.getManager().getOntology(iri);
		//if (iri == null)
		return graph.getManager().createOntology(iri);
	}

	private OWLOntologyFormat getOWLOntologyFormat() {
		return getOWLOntologyFormat(getFormat());
	}

	private OWLOntologyFormat getOWLOntologyFormat(String fmt) {
		OWLOntologyFormat ofmt = null;
		fmt = fmt.toLowerCase();
		if (fmt.equals("rdfxml"))
			ofmt = new RDFXMLOntologyFormat();
		else if (fmt.equals("owl"))
			ofmt = new RDFXMLOntologyFormat();
		else if (fmt.equals("rdf"))
			ofmt = new RDFXMLOntologyFormat();
		else if (fmt.equals("owx"))
			ofmt = new OWLXMLOntologyFormat();
		else if (fmt.equals("owf"))
			ofmt = new OWLFunctionalSyntaxOntologyFormat();
		else if (fmt.equals("obo"))
			ofmt = new OBOOntologyFormat();
		return ofmt;
	}

	private boolean isOWLOntologyFormat() {
		return isOWLOntologyFormat(getFormat());
	}

	private boolean isOWLOntologyFormat(String fmt) {
		return getOWLOntologyFormat(fmt) != null;
	}

	private OWLReasoner getReasoner() {
		String rn = getParam("reasoner");
		if (rn == null) {
			rn = "default";
		}
		return owlserver.getReasoner(rn);
	}

	private boolean hasParam(Param p) {
		String v = request.getParameter(p.toString());
		if (v == null || v.equals(""))
			return false;
		return true;
	}

	/**
	 * @param p
	 * @return the value of http parameter with key p
	 */
	private String getParam(Param p) {
		return request.getParameter(p.toString());
	}
	private String getParam(String p) {
		return request.getParameter(p);
	}
	private String[] getParams(Param p) {
		return request.getParameterValues(p.toString());
	}
	private String[] getParams(String p) {
		return request.getParameterValues(p);
	}


	private boolean isHelp() {
		return getParamAsBoolean("help");
	}

	private boolean getParamAsBoolean(Param p) {
		return getParamAsBoolean(p.toString(), false);
	}
	private boolean getParamAsBoolean(String p) {
		return getParamAsBoolean(p, false);
	}
	private boolean getParamAsBoolean(Param p, boolean def) {
		return getParamAsBoolean(p.toString(), def);
	}
	private boolean getParamAsBoolean(String p, boolean def) {
		String r = request.getParameter(p);
		if (r != null && r.toLowerCase().equals("true"))
			return true;
		if (r != null && r.toLowerCase().equals("false"))
			return false;
		else
			return def;
	}
	private Integer getParamAsInteger(Param p, Integer def) {
		String sv = request.getParameter(p.toString());
		if (sv == null || sv.equals(""))
			return def;
		Integer v = Integer.valueOf(sv);
		if (v == null) {
			v = def;
		}
		return v;
	}

	private PrintWriter getWriter() throws IOException {
		return response.getWriter();
	}

	private OWLOntology getOWLOntology() {
		return graph.getSourceOntology();
	}


}
