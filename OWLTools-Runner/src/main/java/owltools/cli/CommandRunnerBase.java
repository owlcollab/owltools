package owltools.cli;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.cli.tools.CLIMethod;
import owltools.graph.OWLGraphWrapper;
import owltools.io.OWLPrettyPrinter;
import owltools.io.ParserWrapper;

public abstract class CommandRunnerBase {

	private static Logger LOG = Logger.getLogger(CommandRunnerBase.class);

	public OWLGraphWrapper g = null;
	public OWLOntology queryOntology = null;
	public boolean exitOnException = true;
	public boolean isDisposeReasonerOnExit = true;

	public OWLReasoner reasoner = null;
	public String reasonerName = "hermit";
	public Set<OWLObject> owlObjectCachedSet = null;

	Map<OWLClass,OWLClassExpression> queryExpressionMap = null;

	protected ParserWrapper pw = new ParserWrapper();

	/**
	 * Use {@link #getPrettyPrinter()} to access a pre-configured or the default pretty printer.
	 */
	private OWLPrettyPrinter owlpp;
	private String prettyPrinterFormat = null;
	private boolean prettyPrinterHideIds = false;

	public class OptionException extends Exception {

		// generated
		private static final long serialVersionUID = 8770773099868997872L;

		public OptionException(String msg) {
			super(msg);
		}

	}

	protected void exit(int code) {

		// if we are using this in a REPL context (e.g. owlrhino), we don't want to exit the shell
		// on an error - reporting the error is sufficient
		if (exitOnException)
			System.exit(code);
	}

	public void run(String[] args) throws Exception {
		Opts opts = new Opts(args);
		run(opts);
		if (isDisposeReasonerOnExit && reasoner != null) {
			LOG.info("disposing of "+reasoner);
			reasoner.dispose();
		}
	}

	public void run(Opts opts) throws Exception {
		runSingleIteration(opts);
		if (isDisposeReasonerOnExit && reasoner != null) {
			LOG.info("disposing of "+reasoner);
			reasoner.dispose();
		}
	}

	public void runSingleIteration(String[] args) throws Exception {
		Opts opts = new Opts(args);
		runSingleIteration(opts);
	}

	abstract void runSingleIteration(Opts opts) throws Exception;

	protected synchronized OWLPrettyPrinter getPrettyPrinter() {
		if (owlpp == null) {
			if ("Manchester".equals(prettyPrinterFormat)) {
				owlpp = OWLPrettyPrinter.createManchesterSyntaxPrettyPrinter(g);
			}
			else {
				// create default pretty printer
				owlpp = new OWLPrettyPrinter(g);
			}
			if (prettyPrinterHideIds) {
				owlpp.hideIds();
			}

		}
		return owlpp;
	}

	@CLIMethod("--pretty-printer-settings")
	public void handlePrettyPrinter(Opts opts) throws Exception {
		while (opts.hasOpts()) {
			if (opts.nextEq("-m|--manchester")) {
				prettyPrinterFormat = "Manchester";
			}
			else if (opts.nextEq("-f|-format")) {
				prettyPrinterFormat = opts.nextOpt();
			}
			else if (opts.nextEq("--hide-ids")) {
				prettyPrinterHideIds = true;
			}
			else {
				break;
			}
		}
	}
	
	@CLIMethod("--version")
	public void printVersion(Opts opts) throws Exception {
		printManifestEntry("git-revision-sha1", "UNKNOWN");
		printManifestEntry("git-revision-url", "UNKNOWN");
		printManifestEntry("git-branch", "UNKNOWN");
		printManifestEntry("git-dirty", "UNKNOWN");
	}
	
	private static String printManifestEntry(String key, String defaultValue) {
		String value = owltools.version.VersionInfo.getManifestVersion(key);
		if (value == null || value.isEmpty()) {
			value = "UNKNOWN";
		}
		System.out.println(key+"\t"+value);
		return value;
	}

	public void summarizeOntology(OWLOntology ont) {
		System.out.println("Ontology:"+ont);
		System.out.println("  Classes:"+ont.getClassesInSignature().size());
		System.out.println("  Individuals:"+ont.getIndividualsInSignature().size());
		System.out.println("  ObjectProperties:"+ont.getObjectPropertiesInSignature().size());
		System.out.println("  AxiomCount:"+ont.getAxiomCount());
	}

	public Set<OWLObject> resolveEntityList(Opts opts) {
		List<String> ids = opts.nextList();
		Set<OWLObject> objs = new HashSet<OWLObject>();
		for (String id: ids) {
			objs.add( resolveEntity(id) );
		}
		return objs;
	}

	public List<OWLClass> resolveClassList(Opts opts) {
		List<String> ids = opts.nextList();
		List<OWLClass> objs = new ArrayList<OWLClass>();
		for (String id: ids) {
			objs.add( (OWLClass) resolveEntity(id) );
		}
		return objs;
	}

	public Set<OWLObjectProperty> resolveObjectPropertyList(Opts opts) {
		List<String> ids = opts.nextList();
		Set<OWLObjectProperty> objs = new HashSet<OWLObjectProperty>();
		for (String id: ids) {
			if (id.equals("ALL-PROPERTIES"))
				return g.getSourceOntology().getObjectPropertiesInSignature();
			final OWLObjectProperty prop = this.resolveObjectProperty(id);
			if (prop != null) {
				objs.add(prop);
			}
		}
		return objs;
	}

	public List<OWLObjectProperty> resolveObjectPropertyListAsList(Opts opts) {
		List<String> ids = opts.nextList();
		List<OWLObjectProperty> objs = new ArrayList<OWLObjectProperty>();
		for (String id: ids) {
			if (id.equals("ALL-PROPERTIES"))
				return new ArrayList<OWLObjectProperty>(
						g.getSourceOntology().getObjectPropertiesInSignature());
			final OWLObjectProperty prop = this.resolveObjectProperty(id);
			if (prop != null) {
				objs.add(prop);
			}
		}
		return objs;
	}

	// todo - move to util
	public OWLObject resolveEntity(Opts opts) {
		String id = opts.nextOpt(); // in future we will allow resolution by name etc
		return resolveEntity(id);
	}

	public OWLObject resolveEntity(String id) {
		OWLObject obj = null;
		obj = g.getOWLObjectByLabel(id);
		if (obj != null)
			return obj;
		obj = g.getOWLObject(id);
		if (obj != null)
			return obj;		
		obj = g.getOWLObjectByIdentifier(id);
		if (obj == null) {
			LOG.error("Could not find an OWLObject for id: '"+id+"'");
		}
		return obj;
	}

	public OWLObjectProperty resolveObjectProperty(String id) {
		IRI i = null;
		i = g.getIRIByLabel(id);
		if (i == null && id.startsWith("http:")) {
			i = IRI.create(id);
		}
		if (i != null) {
			return g.getDataFactory().getOWLObjectProperty(i);
		}
		OWLObjectProperty prop = g.getOWLObjectPropertyByIdentifier(id);
		if (prop == null && IdTools.isIRIStyleIdSuffix(id)) {
			id = IdTools.convertToOboStyleId(id);
			prop = g.getOWLObjectPropertyByIdentifier(id);
		}
		if (prop == null) {
			LOG.warn("Could not find an OWLObjectProperty for id: '"+id+"'");
		}
		return prop;
	}
	public OWLClass resolveClass(String id) {
		IRI i = null;
		i = g.getIRIByLabel(id);
		if (i == null && id.startsWith("http:")) {
			i = IRI.create(id);
		}
		if (i != null) {
			return g.getDataFactory().getOWLClass(i);
		}
		OWLClass c = g.getOWLClassByIdentifier(id);
		if (c == null && IdTools.isIRIStyleIdSuffix(id)) {
			id = IdTools.convertToOboStyleId(id);
			c = g.getOWLClassByIdentifier(id);
		}
		if (c == null) {
			LOG.error("Could not find an OWLClass for id: '"+id+"'");
			return g.getDataFactory().getOWLClass(IRI.create(id));
		}
		return c;
	}

	public void help() {
		System.out.println("owltools [ONTOLOGY ...] [COMMAND ...]\n");
		System.out.println("Commands/Options");
		System.out.println("  (type 'owltools COMMAND -h' for more info)");
	}

	public void helpFooter() {
		System.out.println("\nOntologies:");
		System.out.println("  These are specified as IRIs. The IRI is typically  'file:PATH' or a URL");
		System.out.println("\nLabel Resolution:");
		System.out.println("  you can pass in either a class label (enclosed in single quotes), an OBO ID or a IRI");
		System.out.println("\nExecution:");
		System.out.println("  note that commands are processed *in order*. This allows you to run mini-pipelines" +
				"  or programs on the command line.");
		System.out.println("  Each command has its own 'grammar'. Type owltools COMMAND -h to see options.");
		System.out.println("  Any argument that is not a command is assumed to be an ontology, and an attempt is made to load it.");
		System.out.println("  (again, this happens sequentially).");
		System.out.println("\nExamples:");
		System.out.println("  ");

	}

}
