package owltools.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
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
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import com.google.gson.Gson;

import owltools.gaf.inference.TaxonConstraintsEngine;
import owltools.gfx.GraphicsConfig;
import owltools.gfx.OWLGraphLayoutRenderer;
import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;
import owltools.io.OWLGsonRenderer;
import owltools.io.OWLPrettyPrinter;
import owltools.io.ParserWrapper;
import owltools.mooncat.Mooncat;
import owltools.sim2.OwlSim;
import owltools.sim2.OwlSim.ScoreAttributeSetPair;
import owltools.sim2.SimJSONEngine;
import owltools.sim2.SimpleOwlSim;
import owltools.sim2.SimpleOwlSim.Metric;
import owltools.sim2.SimpleOwlSim.ScoreAttributePair;
import owltools.sim2.UnknownOWLClassException;

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
		format, direct, reflexive,
		a, b
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

	public void topCommand() throws OWLOntologyCreationException, OWLOntologyStorageException, IOException {
		if (isHelp()) {
			info("Basic metadata about current ontology"); // TODO - json
			return;
		}
		headerHTML();
		outputLine("<h1>OntologyID: "+this.getOWLOntology().getOntologyID()+"</h2>");
		outputLine("<ul>");
		for (OWLAnnotation ann : getOWLOntology().getAnnotations()) {
			outputLine("<li>");
			output(ann.getProperty());
			outputLine("<b>"+ann.getValue().toString()+"</b>");
			outputLine("</li>");
		}
		outputLine("</ul>");
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
	 * @throws IOException 
	 * @throws OWLOntologyCreationException 
	 * @throws OWLOntologyStorageException 
	 * @see owltools.sim.SimEngine
	 */
	@Deprecated
//	public void lcsExpressionCommand() throws IOException, OWLOntologyCreationException, OWLOntologyStorageException {
//		if (isHelp()) {
//			info("Returns least common subsumer class expression using OWLSim");
//			return;
//		}
//		headerOWL();
//		Set<OWLObject> objs = this.resolveEntityList();
//		if (objs.size() == 2) {
//			SimEngine se = new SimEngine(graph);
//			OWLObject[] objsA = objs.toArray(new OWLObject[objs.size()]);
//			OWLClassExpression lcs = se.getLeastCommonSubsumerSimpleClassExpression(objsA[0], objsA[1]);		
//		}
//		else {
//			// TODO - throw
//		}
//	}


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
			owlserver.sos = new SimpleOwlSim(graph.getSourceOntology());
			owlserver.sos.createElementAttributeMapFromOntology();
		}
		return owlserver.sos;
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
	
	public void compareAttributeSetsCommand() throws IOException, OWLOntologyCreationException, OWLOntologyStorageException, UnknownOWLClassException {
		if (isHelp()) {
			info("Returns LCSs and their ICs for two sets of attributes (e.g. phenotypes for disease vs model) using sim2");
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
		response.getWriter().write(jsonStr);
	}
	
	public void searchByAttributeSet() throws IOException, OWLOntologyCreationException, OWLOntologyStorageException, UnknownOWLClassException {
		if (isHelp()) {
			info("Entities that have a similar attribute profile to the specified one, using sim2");
			return;
		}
		headerText();
		OwlSim sos = getOWLSim();
		
		Set<OWLClass> atts = this.resolveClassList(Param.id);
		IRI iri = IRI.create("http://owlsim/"+UUID.randomUUID());
//		for (OWLNamedIndividual i : sos.getAllElements()) {
//			OWLNamedIndividual j;
//			sos.getElementJaccardSimilarity(i, j);
//		}
//		SimJSONEngine sj = new SimJSONEngine(graph,sos);
		// TODO
		//response.getWriter().write(jsonStr);
	}

	// ----------------------------------------
	// END OF COMMANDS
	// ----------------------------------------


	// UTIL

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

	private Set<OWLClass> resolveClassList(Param p) {
		String[] ids = getParams(p);
		Set<OWLClass> objs = new HashSet<OWLClass>();
		for (String id : ids) {
			OWLClass c = graph.getOWLClassByIdentifier(id);
			if (c == null) {
				// TODO - strict mode - for now we include unresolvable classes
				IRI iri = graph.getIRIByIdentifier(id);
				c = graph.getDataFactory().getOWLClass(iri);
			}
			objs.add(c);
		}
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
			pw.saveOWL(tmpOnt, ofmt, response.getOutputStream(), null);
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

	private PrintWriter getWriter() throws IOException {
		return response.getWriter();
	}

	private OWLOntology getOWLOntology() {
		return graph.getSourceOntology();
	}


}
