package owltools.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.coode.owlapi.obo.parser.OBOOntologyFormat;
import org.obolibrary.obo2owl.Obo2OWLConstants;
import org.semanticweb.owlapi.io.OWLFunctionalSyntaxOntologyFormat;
import org.semanticweb.owlapi.io.OWLXMLOntologyFormat;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.gaf.inference.TaxonConstraintsEngine;
import owltools.gfx.GraphicsConfig;
import owltools.gfx.OWLGraphLayoutRenderer;
import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;
import owltools.io.OWLGsonRenderer;
import owltools.io.OWLPrettyPrinter;
import owltools.io.ParserWrapper;
import owltools.mooncat.Mooncat;
import owltools.sim.SimEngine;

public class OWLHandler {

	private static Logger LOG = Logger.getLogger(OWLHandler.class);

	private OWLGraphWrapper graph;
	private	HttpServletResponse response;
	private	HttpServletRequest request;
	private OWLPrettyPrinter owlpp;
	private OWLServer owlserver;
	private String format = null;
	private String commandName;

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
		id, taxid,
		format, direct
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
		println("<h1>OntologyID: "+this.getOWLOntology().getOntologyID()+"</h2>");
		println("<ul>");
		for (OWLAnnotation ann : getOWLOntology().getAnnotations()) {
			println("<li>");
			print(ann.getProperty());
			println("<b>"+ann.getValue().toString()+"</b>");
			println("</li>");
		}
		println("</ul>");
	}

	public void helpCommand() throws IOException {
		headerHTML();
		List<String> commands = new ArrayList<String>();
		println("<ul>");
		for (Method m : this.getClass().getMethods()) {
			String mn = m.getName();
			if (mn.endsWith("Command")) {
				String c = mn.replace("Command", "");
				commands.add(c);
				println("<li>"+c+"</li>");
			}
		}
		println("</ul>");
	}

	public void getSuperClassesCommand() throws OWLOntologyCreationException, OWLOntologyStorageException, IOException {
		headerOWL();
		boolean direct = getParamAsBoolean(Param.direct, true);
		OWLReasoner r = getReasoner();
		OWLClass cls = (OWLClass) this.resolveEntity();
		LOG.info("finding superclasses of: "+cls);
		LOG.info("R:"+r.getSuperClasses(cls, direct));
		for (OWLClass sc : r.getSuperClasses(cls, direct).getFlattened()) {
			print(sc);
		}
	}

	public void allSubClassOfCommand() throws OWLOntologyCreationException, OWLOntologyStorageException, IOException {
		headerOWL();
		boolean direct = getParamAsBoolean(Param.direct, true);
		OWLReasoner r = getReasoner();
		for (OWLClass c : getOWLOntology().getClassesInSignature(true)) {
			for (OWLClass sc : r.getSuperClasses(c, direct).getFlattened()) {
				print(graph.getDataFactory().getOWLSubClassOfAxiom(c, sc));
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
			print(e);
		}
	}

	/**
	 * @see owltools.sim.SimEngine
	 */
	public void lcsExpressionCommand() throws IOException, OWLOntologyCreationException, OWLOntologyStorageException {
		if (isHelp()) {
			info("Returns least common subsumer class expression using OWLSim");
			return;
		}
		headerOWL();
		Set<OWLObject> objs = this.resolveEntityList();
		if (objs.size() == 2) {
			SimEngine se = new SimEngine(graph);
			OWLObject[] objsA = objs.toArray(new OWLObject[objs.size()]);
			OWLClassExpression lcs = se.getLeastCommonSubsumerSimpleClassExpression(objsA[0], objsA[1]);		
		}
		else {
			// TODO - throw
		}
	}


	/**
	 * information about an ontology object (class, property, individual)
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
			print(axiom);
		}
		for (OWLAxiom axiom : getOWLOntology().getAnnotationAssertionAxioms(cls.getIRI())) {
			print(axiom);
		}
	}

	/**
	 * visualize using QuickGO graphdraw. 
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
	 * @see Mooncat#makeSubsetOntology(Set, IRI)
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
			mooncat.makeSubsetOntology(tObjs,
					IRI.create(Obo2OWLConstants.DEFAULT_IRI_PREFIX+"temporary"));
		for (OWLAxiom axiom : subOnt.getAxioms()) {
			print(axiom); // TODO
		}
		graph.getManager().removeOntology(subOnt);
	}

	/**
	 * tests which of a set of input classes (specified using id) is applicable for a set of taxa
	 * (specified using taxid)
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
				print(testCls);
				print("\t");
				print(testTax);
				println("\t"+isOk);
			}
		}
	}

	// ----------------------------------------
	// END OF COMMANDS
	// ----------------------------------------


	// UTIL

	private OWLObject resolveEntity() {
		String id = getParam(Param.id);
		return graph.getOWLObjectByIdentifier(id);
	}

	private OWLClass resolveClass() {
		String id = getParam(Param.id);
		return graph.getOWLClassByIdentifier(id);
	}


	private Set<OWLObject> resolveEntityList() {
		String[] ids = getParams(Param.id);
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
			// TODO - unresolvable
			objs.add(graph.getOWLClassByIdentifier(id));
		}
		return objs;
	}

	public void info(String msg) throws IOException {
		headerHTML();
		println("<h2>Command: "+this.getCommandName()+"</h2>");
		println(msg);
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



	private void print(OWLAxiom axiom) throws IOException {
		String fmt = getFormat();
		if (fmt == null)
			fmt = "";
		if (fmt.equals("plain"))
			println(axiom.toString());
		else if (isOWLOntologyFormat(fmt)) {
			cache(axiom);
		}
		else if (fmt.equals("json")) {
			cache(axiom);
		}
		else
			println(owlpp.render(axiom));
	}

	private void println(String s) throws IOException {
		getWriter().println(s);
	}
	private void print(String s) throws IOException {
		getWriter().print(s);
	}


	private void print(OWLObject obj) throws IOException {
		String fmt = getFormat();
		if (fmt.equals("pretty"))
			print(owlpp.render(obj));
		else if (fmt.equals("json")) {
			//JSONPrinter jsonp = new JSONPrinter(response.getWriter());
			//jsonp.render(obj);
			cache(obj);
		}
		else if (isOWLOntologyFormat()) {
			// TODO - place objects into ontology, eg as subclass of result
			print(obj.toString());
		}
		else {
			// TODO - do this more generically, e.g. within owlpp
			if (getParam("idstyle") != null && obj instanceof OWLNamedObject) {
				if (getParam("idstyle").toLowerCase().equals("obo")) {
					print(graph.getIdentifier(obj));
				}
				else {
					print(obj.toString());
				}
			}
			else 
				print(obj.toString());
		}
	}

	private void print(OWLGraphEdge e) throws IOException {
		if (isOWLOntologyFormat() || getFormat().equals("json")) {
			// todo - json format for edge
			OWLObject x = graph.edgeToTargetExpression(e);
			LOG.info("EdgeToX:"+x);
			print(x);
		}
		else {
			println(owlpp.render(e)); // todo - cache
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
		IRI iri = IRI.create(Obo2OWLConstants.DEFAULT_IRI_PREFIX+"temporary/"+uuid.toString());
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
			rn = "structural";
		}
		return owlserver.getReasoner(rn);
	}

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
