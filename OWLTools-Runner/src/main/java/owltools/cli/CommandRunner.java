package owltools.cli;

import java.awt.Color;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.coode.oppl.AnnotationBasedSymbolTableFactory;
import org.coode.oppl.ChangeExtractor;
import org.coode.oppl.OPPLParser;
import org.coode.oppl.OPPLScript;
import org.coode.oppl.ParserFactory;
import org.coode.oppl.exceptions.RuntimeExceptionHandler;
import org.coode.owlapi.manchesterowlsyntax.ManchesterOWLSyntaxOntologyFormat;
import org.coode.owlapi.obo.parser.OBOOntologyFormat;
import org.coode.owlapi.turtle.TurtleOntologyFormat;
import org.coode.parsers.common.SystemErrorEcho;
import org.eclipse.jetty.server.Server;
import org.obolibrary.macro.ManchesterSyntaxTool;
import org.obolibrary.obo2owl.Obo2OWLConstants;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.expression.ParserException;
import org.semanticweb.owlapi.io.OWLFunctionalSyntaxOntologyFormat;
import org.semanticweb.owlapi.io.OWLXMLOntologyFormat;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationSubject;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLAxiomChange;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyIRIMapper;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLRuntimeException;
import org.semanticweb.owlapi.model.OWLSubAnnotationPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.RemoveAxiom;
import org.semanticweb.owlapi.model.RemoveImport;
import org.semanticweb.owlapi.model.SetOntologyID;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.util.AutoIRIMapper;
import org.semanticweb.owlapi.util.OWLEntityRenamer;
import org.semanticweb.owlapi.util.SimpleIRIMapper;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import owltools.cli.tools.CLIMethod;
import owltools.gfx.GraphicsConfig;
import owltools.gfx.GraphicsConfig.RelationConfig;
import owltools.gfx.OWLGraphLayoutRenderer;
import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;
import owltools.graph.OWLQuantifiedProperty;
import owltools.graph.OWLQuantifiedProperty.Quantifier;
import owltools.idmap.IDMapPairWriter;
import owltools.idmap.IDMappingPIRParser;
import owltools.idmap.UniProtIDMapParser;
import owltools.io.CatalogXmlIRIMapper;
import owltools.io.ChadoGraphClosureRenderer;
import owltools.io.CompactGraphClosureReader;
import owltools.io.CompactGraphClosureRenderer;
import owltools.io.GraphClosureRenderer;
import owltools.io.GraphReader;
import owltools.io.GraphRenderer;
import owltools.io.ImportClosureSlurper;
import owltools.io.OWLGsonRenderer;
import owltools.io.OWLJSONFormat;
import owltools.io.OWLPrettyPrinter;
import owltools.io.ParserWrapper;
import owltools.io.TableToAxiomConverter;
import owltools.mooncat.BridgeExtractor;
import owltools.mooncat.Mooncat;
import owltools.mooncat.PropertyViewOntologyBuilder;
import owltools.mooncat.QuerySubsetGenerator;
import owltools.mooncat.ontologymetadata.ImportChainDotWriter;
import owltools.mooncat.ontologymetadata.ImportChainExtractor;
import owltools.mooncat.ontologymetadata.OntologyMetadataMarkdownWriter;
import owltools.ontologyrelease.OntologyMetadata;
import owltools.reasoner.ExpressionMaterializingReasoner;
import owltools.reasoner.GraphReasonerFactory;
import owltools.reasoner.LazyExpressionMaterializingReasonerFactory;
import owltools.reasoner.OWLExtendedReasoner;
import owltools.web.OWLServer;
import uk.ac.manchester.cs.factplusplus.owlapiv3.FaCTPlusPlusReasonerFactory;
import uk.ac.manchester.cs.owlapi.modularity.ModuleType;
import uk.ac.manchester.cs.owlapi.modularity.SyntacticLocalityModuleExtractor;
import de.derivo.sparqldlapi.Query;
import de.derivo.sparqldlapi.QueryArgument;
import de.derivo.sparqldlapi.QueryBinding;
import de.derivo.sparqldlapi.QueryEngine;
import de.derivo.sparqldlapi.QueryResult;
import de.derivo.sparqldlapi.exceptions.QueryEngineException;
import de.derivo.sparqldlapi.exceptions.QueryParserException;
import de.derivo.sparqldlapi.types.QueryArgumentType;
import de.tudresden.inf.lat.jcel.owlapi.main.JcelReasoner;

/**
 * An instance of this class can execute owltools commands in sequence.
 * 
 * Typically, this class is called from a wrapper within its main() method.
 * 
 * Extend this class to implement additional functions. Use the {@link CLIMethod} 
 * annotation, to designate the relevant methods.
 * 
 * @author cjm
 *
 * @see GafCommandRunner
 * @see JsCommandRunner
 * @see SimCommandRunner
 * @see SolrCommandRunner
 * @see TaxonCommandRunner
 */
public class CommandRunner {

	private static Logger LOG = Logger.getLogger(CommandRunner.class);

	public OWLGraphWrapper g = null;
	public OWLOntology queryOntology = null;
	public boolean exitOnException = true;

	public OWLReasoner reasoner = null;
	public String reasonerName = "hermit";
	public Set<OWLObject> owlObjectCachedSet = null;

	Map<OWLClass,OWLClassExpression> queryExpressionMap = null;

	protected ParserWrapper pw = new ParserWrapper();
	protected OWLPrettyPrinter owlpp;

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


	public List<String> parseArgString(String str) {
		List<String> args = new ArrayList<String>();
		int p = 0;
		StringBuffer ns = new StringBuffer();
		while (p < str.length()) {
			if (str.charAt(p) == ' ') {
				if (ns.length() > 0) {
					args.add(ns.toString());
					ns = new StringBuffer();
				}
			}
			else {
				ns.append(str.charAt(p));
			}
			p++;
		}
		if (ns.length() > 0) {
			args.add(ns.toString());
		}		
		return args;
	}

	public void run(String[] args) throws Exception {
		Opts opts = new Opts(args);
		run(opts);
	}

	public void run(Opts opts) throws Exception {

		Set<OWLSubClassOfAxiom> removedSubClassOfAxioms = null;
		GraphicsConfig gfxCfg = new GraphicsConfig();
		//Configuration config = new PropertiesConfiguration("owltools.properties");


		while (opts.hasArgs()) {

			if (opts.nextArgIsHelp()) {
				help();
				opts.setHelpMode(true);
			}

			//String opt = opts.nextOpt();
			//System.out.println("processing arg: "+opt);
			if (opts.nextEq("--pellet")) {
				System.err.println("The Pellet reasoner is no longer supported, use Hermit '--hermit' or ELK '--elk' instead");
				exit(-1);
			}
			else if (opts.nextEq("--hermit")) {
				reasonerName = "hermit";
			}
			else if (opts.nextEq("--elk")) {
				reasonerName = "elk";
			}
			else if (opts.nextEq("--use-reasoner")) {
				reasonerName =  opts.nextOpt();
			}
			else if (opts.nextEq("--reasoner")) {
				reasonerName = opts.nextOpt();
				g.setReasoner(createReasoner(g.getSourceOntology(),reasonerName,g.getManager()));
				reasoner = g.getReasoner();
			}
			else if (opts.nextEq("--init-reasoner")) {
				while (opts.hasOpts()) {
					if (opts.nextEq("-r")) {
						reasonerName = opts.nextOpt();
					}
					else {
						break;
					}
				}
				g.setReasoner(createReasoner(g.getSourceOntology(),reasonerName,g.getManager()));
				reasoner = g.getReasoner();
			}
			else if (opts.nextEq("--reasoner-dispose")) {
				reasoner.dispose();
			}
			else if (opts.nextEq("--no-reasoner")) {
				reasonerName = "";
			}
			else if (opts.nextEq("--log-info")) {
				Logger.getRootLogger().setLevel(Level.INFO);
			}
			else if (opts.nextEq("--log-debug")) {
				Logger.getRootLogger().setLevel(Level.DEBUG);
			}
			else if (opts.nextEq("--no-debug")) {
				Logger.getRootLogger().setLevel(Level.OFF);
			}
			else if (opts.nextEq("--monitor-memory")) {
				g.getConfig().isMonitorMemory = true;
			}
			else if (opts.nextEq("--list-classes")) {
				Set<OWLClass> clss = g.getSourceOntology().getClassesInSignature();
				for (OWLClass c : clss) {
					System.out.println(c);
				}
			}
			else if (opts.nextEq("--object-to-label-table")) {
				Set<OWLObject> objs = g.getAllOWLObjects();
				for (OWLObject c : objs) {
					if (c instanceof OWLNamedObject) {
						String label = g.getLabel(c);
						System.out.println(((OWLNamedObject)c).getIRI()+"\t"+label);
					}
				}
			}
			else if (opts.nextEq("--query-ontology")) {
				opts.info("[-m]", "specify an ontology that has classes to be used as queries. See also: --reasoner-query");
				boolean isMerge = false;
				while (opts.hasOpts()) {
					if (opts.nextEq("-m"))
						isMerge = true;
					else
						opts.nextOpt();
				}
				queryOntology = pw.parse(opts.nextOpt());
				queryExpressionMap = new HashMap<OWLClass,OWLClassExpression>();
				for (OWLClass qc : queryOntology.getClassesInSignature()) {
					for (OWLClassExpression ec : qc.getEquivalentClasses(queryOntology)) {
						queryExpressionMap.put(qc, ec);
					}
				}
				if (isMerge) {
					g.mergeOntology(queryOntology);
				}
			}
			else if (opts.nextEq("--merge")) {
				opts.info("ONT", "merges ONT into current source ontology");
				g.mergeOntology(pw.parse(opts.nextOpt()));
			}
			else if (opts.nextEq("--use-catalog") || opts.nextEq("--use-catalog-xml")) {
				opts.info("", "uses default catalog-v001.xml");
				pw.getManager().addIRIMapper(new CatalogXmlIRIMapper("catalog-v001.xml"));
			}
			else if (opts.nextEq("--catalog-xml")) {
				opts.info("CATALOG-FILE", "uses the specified file as a catalog");
				pw.getManager().addIRIMapper(new CatalogXmlIRIMapper(opts.nextOpt()));
			}
			else if (opts.nextEq("--map-ontology-iri")) {
				opts.info("OntologyIRI FILEPATH", "maps an ontology IRI to a file in your filesystem");
				OWLOntologyIRIMapper iriMapper = 
					new SimpleIRIMapper(IRI.create(opts.nextOpt()),
							IRI.create(new File(opts.nextOpt())));
				LOG.info("Adding "+iriMapper+" to "+pw.getManager());
				pw.getManager().addIRIMapper(iriMapper);
			}
			else if (opts.nextEq("--auto-ontology-iri")) {
				opts.info("[-r] ROOTDIR", "uses an AutoIRI mapper [EXPERIMENTAL]");
				boolean isRecursive = false;
				while (opts.hasOpts()) {
					if (opts.nextEq("-r")) {
						isRecursive = true;
					}
					else {
						break;
					}
				}
				File file = new File(opts.nextOpt());
				OWLOntologyIRIMapper iriMapper = new AutoIRIMapper(file, isRecursive);
				LOG.info("Adding "+iriMapper+" to "+pw.getManager()+" dir:"+file+" isRecursive="+isRecursive);
				pw.getManager().addIRIMapper(iriMapper);
			}
			else if (opts.nextEq("--remove-imports-declarations")) {
				Set<OWLImportsDeclaration> oids = g.getSourceOntology().getImportsDeclarations();
				for (OWLImportsDeclaration oid : oids) {
					RemoveImport ri = new RemoveImport(g.getSourceOntology(), oid);
					g.getManager().applyChange(ri);
				}
			}
			else if (opts.nextEq("--add-imports-declarations")) {
				List<String> importsIRIs = opts.nextList();
				for (String importIRI : importsIRIs) {
					AddImport ai = 
						new AddImport(g.getSourceOntology(),
								g.getDataFactory().getOWLImportsDeclaration(IRI.create(importIRI)));
					g.getManager().applyChange(ai);
				}
			}
			else if (opts.nextEq("--create-ontology")) {
				String iri = opts.nextOpt();
				if (!iri.startsWith("http:")) {
					iri = Obo2OWLConstants.DEFAULT_IRI_PREFIX+iri;
				}
				g = new OWLGraphWrapper(iri);
			}
			else if (opts.nextEq("--merge-import-closure") || opts.nextEq("--merge-imports-closure")) {
				opts.info("[--ni]", "All axioms from ontologies in import closure are copied into main ontology");
				boolean isRmImports = false;
				if (opts.nextEq("--ni")) {
					opts.info("", "removes imports declarations after merging");
					isRmImports = true;
				}
				g.mergeImportClosure(isRmImports);
			}
			else if (opts.nextEq("--merge-support-ontologies")) {
				for (OWLOntology ont : g.getSupportOntologySet())
					g.mergeOntology(ont);
				g.setSupportOntologySet(new HashSet<OWLOntology>());
			}
			else if (opts.nextEq("--add-support-from-imports")) {
				opts.info("", "All ontologies in direct import are removed and added as support ontologies");
				g.addSupportOntologiesFromImportsClosure();
			}
			else if (opts.nextEq("--add-imports-from-support|--add-imports-from-supports")) {
				g.addImportsFromSupportOntologies();
			}
			else if (opts.nextEq("-m") || opts.nextEq("--mcat")) {
				catOntologies(opts);
			}
			else if (opts.nextEq("--remove-external-entities")) {
				Mooncat m = new Mooncat(g);
				m.removeExternalEntities();
			}
			else if (opts.nextEq("--remove-dangling")) {
				Mooncat m = new Mooncat(g);
				m.removeDanglingAxioms();
			}
			else if (opts.nextEq("--make-subset-by-properties")) {
				Set<OWLObjectProperty> props = this.resolveObjectPropertyList(opts);
				Mooncat m = new Mooncat(g);
				m.retainAxiomsInPropertySubset(g.getSourceOntology(),props,reasoner);
				m.removeDanglingAxioms();
			}
			else if (opts.nextEq("--info")) {
				opts.info("","show ontology statistics");
				for (OWLOntology ont : g.getAllOntologies()) {
					summarizeOntology(ont);
				}
			}
			else if (opts.nextEq("--save-closure")) {
				opts.info("[-c] FILENAME", "write out closure of graph.");
				GraphRenderer gcw;
				if (opts.nextEq("-c")) {
					opts.info("", "compact storage option.");
					gcw = new CompactGraphClosureRenderer(opts.nextOpt());					
				}
				else {
					gcw = new GraphClosureRenderer(opts.nextOpt());
				}
				gcw.render(g);				
			}
			else if (opts.nextEq("--read-closure")) {
				opts.info("FILENAME", "reads closure previously saved using --save-closure (compact format only)");
				GraphReader gr = new CompactGraphClosureReader(g);
				gr.read(opts.nextOpt());	
				LOG.info("RESTORED CLOSURE CACHE");
				LOG.info("size="+g.inferredEdgeBySource.size());
			}
			else if (opts.nextEq("--save-closure-for-chado")) {
				opts.info("OUTPUTFILENAME",
				"saves the graph closure in a format that is oriented towards loading into a Chado database");
				boolean isChain = opts.nextEq("--chain");
				ChadoGraphClosureRenderer gcw = new ChadoGraphClosureRenderer(opts.nextOpt());
				gcw.isChain = isChain;
				gcw.render(g);				
			}
			else if (opts.nextEq("--remove-annotation-assertions")) {
				boolean isPreserveLabels = false;
				while (opts.hasOpts()) {
					if (opts.nextEq("-l")) {
						isPreserveLabels = true;
					}
					else
						break;
				}
				for (OWLOntology o : g.getAllOntologies()) {
					Set<OWLAnnotationAssertionAxiom> aas;

					if (isPreserveLabels) {
						aas = new HashSet<OWLAnnotationAssertionAxiom>();
						for (OWLAnnotationAssertionAxiom aaa : o.getAxioms(AxiomType.ANNOTATION_ASSERTION)) {
							if (!aaa.getProperty().isLabel()) {
								aas.add(aaa);
							}
						}
					}
					else {
						aas = o.getAxioms(AxiomType.ANNOTATION_ASSERTION);
					}
					g.getManager().removeAxioms(o, aas);

				}
			}
			else if (opts.nextEq("--apply-patch")) {
				opts.info("minusAxiomsOntology plusAxiomsOntology", "applies 'patch' to current ontology");
				OWLOntology ontMinus = pw.parse(opts.nextOpt());
				OWLOntology ontPlus = pw.parse(opts.nextOpt());
				OWLOntology src = g.getSourceOntology();
				Set<OWLAxiom> rmAxioms = ontMinus.getAxioms();
				Set<OWLAxiom> addAxioms = ontPlus.getAxioms();
				int numPre = src.getAxiomCount();
				LOG.info("Removing "+rmAxioms.size()+" axioms from src, current axiom count="+numPre);
				g.getManager().removeAxioms(src, rmAxioms);
				int numPost = src.getAxiomCount();
				LOG.info("Removed axioms from src, new axiom count="+numPost);
				if (numPre-numPost != rmAxioms.size()) {
					LOG.error("Some axioms not found!");
				}
				LOG.info("Adding "+addAxioms.size()+" axioms to src, current axiom count="+numPost);
				g.getManager().addAxioms(src, addAxioms);
				LOG.info("Added "+addAxioms.size()+" axioms to src, new count="+src.getAxiomCount());
				if (src.getAxiomCount() - numPost != addAxioms.size()) {
					LOG.error("Some axioms already there!");
				}

			}
			else if (opts.nextEq("--translate-xrefs-to-equivs")) {
				// TODO
				//g.getXref(c);
			}
			else if (opts.nextEq("--rename-entity")) {
				opts.info("OLD-IRI NEW-IRI", "used OWLEntityRenamer to switch IDs/IRIs");
				OWLEntityRenamer oer = new OWLEntityRenamer(g.getManager(), g.getAllOntologies());
				List<OWLOntologyChange> changes = oer.changeIRI(IRI.create(opts.nextOpt()),IRI.create(opts.nextOpt()));
				g.getManager().applyChanges(changes);
			}
			else if (opts.nextEq("--merge-equivalent-classes")) {
				opts.info("[-f FROM-URI-PREFIX]* [-t TO-URI-PREFIX]", "merges equivalent classes to a common ID space");
				List<String> prefixFroms = new Vector<String>();
				String prefixTo = null;
				while (opts.hasOpts()) {
					if (opts.nextEq("-f")) {
						prefixFroms.add(opts.nextOpt());
					}
					else if (opts.nextEq("-t")) {
						prefixTo = opts.nextOpt();
					}
					else
						break;
				}
				Map<OWLEntity,IRI> e2iri = new HashMap<OWLEntity,IRI>();
				LOG.info("building entity2IRI map...: " + prefixFroms + " --> "+prefixTo);
				OWLEntityRenamer oer = new OWLEntityRenamer(g.getManager(), g.getAllOntologies());

				// we only map classes in the source ontology - however,
				// we use equivalence axioms from the full complement of ontologies
				// TODO - allow arbitrary entities
				for (OWLEntity e : g.getSourceOntology().getClassesInSignature()) {
					//LOG.info("  testing "+c+" ECAs: "+g.getSourceOntology().getEquivalentClassesAxioms(c));
					// TODO - may be more efficient to invert order of testing
					String iriStr = e.getIRI().toString();
					boolean isMatch = false;
					for (String prefixFrom : prefixFroms) {
						if (iriStr.startsWith(prefixFrom)) {
							isMatch = true;
							break;
						}
					}
					if (prefixFroms.size()==0)
						isMatch = true;
					if (isMatch) {
						// we also scan support ontologies for equivalencies
						for (OWLOntology ont : g.getAllOntologies()) {
							OWLClass c = ont.getOWLOntologyManager().getOWLDataFactory().getOWLClass(e.getIRI());
							for (OWLEquivalentClassesAxiom eca : ont.getEquivalentClassesAxioms(c)) {
								for (OWLClass d : eca.getClassesInSignature()) {
									if (prefixTo == null || d.getIRI().toString().startsWith(prefixTo)) {
										e2iri.put(c, d.getIRI()); // TODO one-to-many
									}
								}
							}
						}
					}
				}
				LOG.info("Mapping "+e2iri.size()+" entities");
				// TODO - this is slow
				List<OWLOntologyChange> changes = oer.changeIRI(e2iri);
				g.getManager().applyChanges(changes);
				LOG.info("Mapped "+e2iri.size()+" entities!");
			}
			else if (opts.nextEq("--rename-entities-via-equivalent-classes")) {
				Map<OWLEntity,IRI> e2iri = new HashMap<OWLEntity,IRI>();
				OWLEntityRenamer oer = new OWLEntityRenamer(g.getManager(), g.getAllOntologies());

				Set<IRI> entities = new HashSet<IRI>();
				for (OWLClass c : g.getSourceOntology().getClassesInSignature()) {
					entities.add(c.getIRI());
				}				
				for (OWLAnnotationAssertionAxiom aaa : g.getSourceOntology().getAxioms(AxiomType.ANNOTATION_ASSERTION)) {
					if (aaa.getSubject() instanceof IRI) {
						entities.add((IRI) aaa.getSubject());
					}
				}

				// we only map classes in the source ontology - however,
				// we use equivalence axioms from the full complement of ontologies
				// TODO - allow arbitrary entities
				for (IRI e : entities) {

					for (OWLOntology ont : g.getAllOntologies()) {
						OWLClass c = ont.getOWLOntologyManager().getOWLDataFactory().getOWLClass(e);
						for (OWLClassExpression d : c.getEquivalentClasses(ont)) {
							if (d instanceof OWLClass)
								e2iri.put(c, ((OWLClass) d).getIRI()); 
						}
					}
				}
				LOG.info("Mapping "+e2iri.size()+" entities");
				// TODO - this is slow
				List<OWLOntologyChange> changes = oer.changeIRI(e2iri);
				g.getManager().applyChanges(changes);
				LOG.info("Mapped "+e2iri.size()+" entities!");
			}
			else if (opts.nextEq("--query-cw")) {
				opts.info("", "closed-world query");
				owlpp = new OWLPrettyPrinter(g);

				for (OWLClass qc : queryExpressionMap.keySet()) {
					System.out.println(" CWQueryClass: "+qc);
					System.out.println(" CWQueryClass: "+owlpp.render(qc)+" "+qc.getIRI().toString());
					OWLClassExpression ec = queryExpressionMap.get(qc);
					System.out.println(" CWQueryExpression: "+owlpp.render(ec));
					Set<OWLObject> results = g.queryDescendants(ec);
					for (OWLObject result : results) {
						if (result instanceof OWLClass) {
							System.out.println("  "+owlpp.render((OWLClass)result));
						}
					}
				}
			}
			else if (opts.nextEq("--extract-ontology-metadata")) {
				opts.info("[-c ONT-IRI]", "extracts annotations from ontology");
				String mdoIRI = "http://x.org";
				while (opts.hasOpts()) {
					if (opts.nextEq("-c")) {
						mdoIRI = opts.nextOpt();
					}
					else
						break;
				}
				OWLOntology mdo = ImportChainExtractor.extractOntologyMetadata(g, mdoIRI);
				g.setSourceOntology(mdo);
			}
			else if (opts.nextEq("--write-imports-dot")) {
				opts.info("OUT", "writes imports chain as dot file");
				String output = opts.nextOpt();
				ImportChainDotWriter writer = new ImportChainDotWriter(g);
				writer.renderDot(g.getSourceOntology(), g.getOntologyId(), output, true);
			}
			else if (opts.nextEq("--ontology-metadata-to-markdown")) {
				opts.info("OUT", "writes md from ontology metadata");
				String output = opts.nextOpt();
				BufferedWriter fileWriter = new BufferedWriter(new FileWriter(new File(output)));
				String s = OntologyMetadataMarkdownWriter.renderMarkdown(g, ".", true);
				fileWriter.write(s);
				fileWriter.close();
			}
			else if (opts.nextEq("--extract-bridge-ontologies")) {
				opts.info("OUT", "");
				String dir = "bridge/";
				String ontId = null;
				boolean isRemoveBridgeAxiomsFromSource = false;
				while (opts.hasOpts()) {
					if (opts.nextEq("-d")) {
						dir = opts.nextOpt();
					}
					else if (opts.nextEq("-x")) {
						isRemoveBridgeAxiomsFromSource = true;
					}
					else if (opts.nextEq("-s")) {
						ontId = opts.nextOpt();
					}
					else {
						break;
					}
				}
				BridgeExtractor be = new BridgeExtractor(g.getSourceOntology());
				be.subDir = dir;
				be.extractBridgeOntologies(ontId, isRemoveBridgeAxiomsFromSource);
				be.saveBridgeOntologies(dir);

			}
			else if (opts.nextEq("--oppl")) {
				opts.info("[--dry-run] [[-i OPPL-SCRIPT-FILE] | OPPL-STRING]", "runs an oppl script");
				boolean isDryRun = false;
				String script = null;
				while (opts.hasOpts()) {
					if (opts.nextEq("--dry-run")) {
						isDryRun = true;
					}
					else if (opts.nextEq("-i")) {
						script = FileUtils.readFileToString(new File(opts.nextOpt()));
					}
					else {
						break;
					}
				}
				if (script == null)
					script = opts.nextOpt();
				LOG.info("OPPL: "+script);
				ParserFactory parserFactory = new ParserFactory(g.getManager(), g.getSourceOntology(), 
						reasoner);
				AnnotationBasedSymbolTableFactory annotationBasedSymbolTableFactory = new AnnotationBasedSymbolTableFactory(
						g.getManager(),
						Arrays.asList(g.getDataFactory().getRDFSLabel().getIRI()));

				OPPLParser parser = parserFactory.build(
						new SystemErrorEcho(),
						annotationBasedSymbolTableFactory);


				OPPLScript parsed = parser.parse(script);
				ChangeExtractor extractor = new ChangeExtractor(new RuntimeExceptionHandler() {
					public void handlePatternSyntaxExcpetion(PatternSyntaxException e) {
						e.printStackTrace();
					}

					public void handleOWLRuntimeException(OWLRuntimeException e) {
						e.printStackTrace();
					}

					public void handleException(RuntimeException e) {
						e.printStackTrace();
					}
				}, true);
				List<OWLAxiomChange> changes = extractor.visit(parsed);
				for (OWLAxiomChange owlAxiomChange : changes) {
					if (!isDryRun) {
						LOG.info("APPLYING: "+owlAxiomChange);
						g.getManager().applyChange(owlAxiomChange);
					}
					else {
						LOG.info("DRY_RUN: "+owlAxiomChange);
					}
				}
			}
			else if (opts.nextEq("--sparql-dl")) {
				opts.info("\"QUERY-TEXT\"", "executes a SPARQL-DL query using the reasoner");
				/* Examples:
				 *  SELECT * WHERE { SubClassOf(?x,?y)}
				 */
				if (reasoner == null) {
					reasoner = createReasoner(g.getSourceOntology(),reasonerName,g.getManager());
				}
				String q = opts.nextOpt();
				System.out.println("Q="+q);
				OWLPrettyPrinter owlpp = new OWLPrettyPrinter(g);
				try {
					QueryEngine engine;
					Query query = Query.create(q);
					engine = QueryEngine.create(g.getManager(), reasoner, true);
					QueryResult result = engine.execute(query);
					if(query.isAsk()) {
						System.out.print("Result: ");
						if(result.ask()) {
							System.out.println("yes");
						}
						else {
							System.out.println("no");
						}
					}
					else {
						if(!result.ask()) {
							System.out.println("Query has no solution.\n");
						}
						else {
							System.out.println("Results:");
							for (int i=0; i < result.size(); i++) {
								System.out.print("["+i+"] ");
								QueryBinding qb = result.get(i);
								for (QueryArgument qa : qb.getBoundArgs()) {
									String k = qa.toString();
									System.out.print(" "+k+"=");
									QueryArgument v = qb.get(qa);
									String out = v.toString();
									if (v.getType().equals(QueryArgumentType.URI)) {
										out = owlpp.renderIRI(v.toString());
									}
									System.out.print(out+"; ");
								}
								System.out.println("");
							}
							//System.out.print(result);
							System.out.println("-------------------------------------------------");
							System.out.println("Size of result set: " + result.size());
						}
					}

				} catch (QueryParserException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (QueryEngineException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
			else if (opts.nextEq("--remove-abox")) {
				opts.info("", "removes all named individual declarations and all individual axioms (e.g. class/property assertion");
				for (OWLOntology ont : g.getAllOntologies()) {
					Set<OWLAxiom> rmAxioms = new HashSet<OWLAxiom>();
					rmAxioms.addAll(ont.getAxioms(AxiomType.DIFFERENT_INDIVIDUALS));
					rmAxioms.addAll(ont.getAxioms(AxiomType.CLASS_ASSERTION));
					rmAxioms.addAll(ont.getAxioms(AxiomType.OBJECT_PROPERTY_ASSERTION));
					for (OWLNamedIndividual ind : ont.getIndividualsInSignature()) {
						rmAxioms.add(g.getDataFactory().getOWLDeclarationAxiom(ind));
					}
					g.getManager().removeAxioms(ont, rmAxioms);
				}
			}
			else if (opts.nextEq("--i2c")) {
				opts.info("[-s]", "Converts individuals to classes");
				boolean isReplaceOntology = false;
				while (opts.hasOpts()) {
					if (opts.nextEq("-s")) {
						isReplaceOntology = true;
					}
					else {
						break;
					}
				}
				Set<OWLAxiom> axs = new HashSet<OWLAxiom>();
				OWLOntology ont = g.getSourceOntology();
				for (OWLNamedIndividual i : ont.getIndividualsInSignature()) {
					OWLClass c = g.getDataFactory().getOWLClass(i.getIRI());
					for (OWLClassExpression ce : i.getTypes(ont)) {
						axs.add(g.getDataFactory().getOWLSubClassOfAxiom(c, ce));
					}
					//g.getDataFactory().getOWLDe
					for (OWLClassAssertionAxiom ax : ont.getClassAssertionAxioms(i)) {
						g.getManager().removeAxiom(ont, ax);
					}
					for (OWLDeclarationAxiom ax : ont.getDeclarationAxioms(i)) {
						g.getManager().removeAxiom(ont, ax);
					}
					//g.getDataFactory().getOWLDeclarationAxiom(owlEntity)
				}
				if (isReplaceOntology) {
					for (OWLAxiom ax : g.getSourceOntology().getAxioms()) {
						g.getManager().removeAxiom(ont, ax);
					}
				}
				for (OWLAxiom axiom : axs) {
					g.getManager().addAxiom(ont, axiom);
				}
			}
			else if (opts.nextEq("--init-reasoner")) {
				opts.info("[-r reasonername]", "Creates a reasoner object");
				while (opts.hasOpts()) {
					if (opts.nextEq("-r")) {
						reasonerName = opts.nextOpt();
					}
					else {
						break;
					}
				}
				reasoner = createReasoner(g.getSourceOntology(),reasonerName,g.getManager());			
			}
			else if (opts.nextEq("--reasoner-query")) {
				opts.info("[-r reasonername] [-m] [-d] [-a] [-x] [-c IRI] (--stdin | CLASS-EXPRESSION)", 
						"Queries current ontology for descendants of CE using reasoner.\n"+
				"Enclose all labels in quotes (--stdin only). E.g. echo \"'part of' some 'tentacle'\" | owltools ceph.owl --reasoner-query --stdin");
				boolean isManifest = false;
				boolean isDescendants = true;
				boolean isIndividuals = false;
				boolean isAncestors = true;
				boolean isExtended = false;
				boolean isCache = false;
				boolean isRemoveUnsatisfiable = false;
				boolean isSubOntExcludeClosure = false;
				String subOntologyIRI = null;
				OWLClassExpression ce = null;
				String expression = null;

				while (opts.hasOpts()) {
					if (opts.nextEq("-r")) {
						reasoner = null;
						reasonerName = opts.nextOpt();
						if (reasonerName.toLowerCase().equals("elk"))
							isManifest = true;
					}
					else if (opts.nextEq("-m")) {
						opts.info("", 
						"manifests the class exression as a class equivalent to query CE and uses this as a query; required for Elk");
						isManifest = true;
					}
					else if (opts.nextEq("-d")) {
						isDescendants = true;
						isAncestors = false;
					}
					else if (opts.nextEq("-a")) {
						isDescendants = false;
						isAncestors = true;
					}
					else if (opts.nextEq("-i")) {
						isIndividuals = true;
					}
					else if (opts.nextEq("--stdin")) {
						try {
							BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
							System.out.print("> QUERY: ");
							expression = in.readLine();
						} catch (IOException e) {
						}					}
					else if (opts.nextEq("-x")) {
						isExtended = true;
					}
					else if (opts.nextEq("-c")) {
						if (opts.nextEq("--exclude-closure"))
							isSubOntExcludeClosure = true;
						subOntologyIRI = opts.nextOpt();
					}
					else if (opts.nextEq("--cache")) {
						isCache = true;
					}
					else if (opts.nextEq("-l")) {
						ce = (OWLClassExpression) resolveEntity(opts);
					}
					else {
						break;
					}
				}

				if (ce == null && expression == null)
					expression = opts.nextOpt();
				owlpp = new OWLPrettyPrinter(g);
				Set<OWLClass> results = new HashSet<OWLClass>();
				ManchesterSyntaxTool parser = new ManchesterSyntaxTool(g.getSourceOntology(), g.getSupportOntologySet());

				try {
					if (ce == null) {
						System.out.println("# PARSING: "+expression);
						ce = parser.parseManchesterExpression(expression);
					}
					System.out.println("# QUERY: "+owlpp.render(ce));
					if (ce instanceof OWLClass)
						results.add((OWLClass) ce);

					// some reasoners such as elk cannot query using class expressions - we manifest
					// the class expression as a named class in order to bypass this limitation
					if (isManifest && !(ce instanceof OWLClass)) {
						System.err.println("-m deprecated: consider using --reasoner welk");
						OWLClass qc = g.getDataFactory().getOWLClass(IRI.create("http://owltools.org/Q"));
						g.getManager().removeAxioms(g.getSourceOntology(), 
								g.getSourceOntology().getAxioms(qc));
						OWLEquivalentClassesAxiom ax = g.getDataFactory().getOWLEquivalentClassesAxiom(ce, qc);
						g.getManager().addAxiom(g.getSourceOntology(), ax);
						if (reasoner != null)
							reasoner.flush();
						ce = qc;
					}
					ExpressionMaterializingReasoner xr = null;
					if (isExtended) {
						if (reasoner != null) {
							LOG.error("Reasoner should NOT be set prior to creating EMR - unsetting");
						}
						xr = new ExpressionMaterializingReasoner(g.getSourceOntology());	
						LOG.info("materializing... [doing this before initializing reasoner]");					
						xr.materializeExpressions();
						LOG.info("set extended reasoner: "+xr);
					}
					if (reasoner == null) {
						reasoner = createReasoner(g.getSourceOntology(),reasonerName,g.getManager());
						LOG.info("created reasoner: "+reasoner);
					}
					if (xr != null) {
						xr.setWrappedReasoner(reasoner);
						reasoner = xr;					
					}
					if (isIndividuals) {
						for (OWLNamedIndividual r : reasoner.getInstances(ce, false).getFlattened()) {
							//results.add(r);
							if (!isCache)
								System.out.println("D: "+owlpp.render(r));
						}
					}
					if (isDescendants) {
						for (OWLClass r : reasoner.getSubClasses(ce, false).getFlattened()) {
							results.add(r);
							if (!isCache)
								System.out.println("D: "+owlpp.render(r));
						}
					}
					if (isAncestors) {
						if (isExtended) {
							for (OWLClassExpression r : ((OWLExtendedReasoner) reasoner).getSuperClassExpressions(ce, false)) {
								if (r instanceof OWLClass)
									results.add((OWLClass) r);

								if (!isCache)
									System.out.println("A:"+owlpp.render(r));
							}

						}
						else {
							for (OWLClass r : reasoner.getSuperClasses(ce, false).getFlattened()) {
								results.add(r);
								if (!isCache)
									System.out.println("A:"+owlpp.render(r));
							}
						}
					}


				} catch (ParserException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				finally {
					// always dispose parser to avoid a memory leak
					parser.dispose();
				}

				if (owlObjectCachedSet == null)
					owlObjectCachedSet = new HashSet<OWLObject>();
				owlObjectCachedSet.addAll(results);

				// ---
				// Create a sub-ontology
				// ---
				if (subOntologyIRI != null) {
					//g.mergeImportClosure();
					QuerySubsetGenerator subsetGenerator = new QuerySubsetGenerator();
					OWLOntology srcOnt = g.getSourceOntology();
					g.setSourceOntology(g.getManager().createOntology(IRI.create(subOntologyIRI)));
					g.addSupportOntology(srcOnt);

					subsetGenerator.createSubSet(g, results, g.getSupportOntologySet(), isSubOntExcludeClosure, 
							isSubOntExcludeClosure);
				}
			}
			else if (opts.nextEq("--make-ontology-from-results")) {
				// TODO - use Mooncat
				opts.info("[-m] [-f] IRI", "takes the most recent reasoner query and generates a subset ontology using ONLY classes from results");
				boolean followClosure = false;
				boolean useMooncat = false;
				while (opts.hasOpts()) {
					if (opts.nextEq("-f|--follow-closure|--file-gaps"))
						followClosure = true;
					else if (opts.nextEq("-m|--use-mooncat"))
						useMooncat = true;
					else
						break;
				}
				if (followClosure) useMooncat = true;
				String subOntologyIRI = opts.nextOpt();
				if (useMooncat) {
					Mooncat m = new Mooncat(g);
					Set<OWLClass> cs = new HashSet<OWLClass>();
					for (OWLObject obj : owlObjectCachedSet) {
						if (obj instanceof OWLClass)
							cs.add((OWLClass) obj);
					}
					// TODO
					OWLOntology subOnt = m.makeMinimalSubsetOntology(cs, IRI.create(subOntologyIRI), followClosure);
					g.setSourceOntology(subOnt);
				}
				else {

					Set<OWLAxiom> subsetAxioms = new HashSet<OWLAxiom>();
					Set <OWLObjectProperty> objPropsUsed = new HashSet<OWLObjectProperty>();
					for (OWLOntology mergeOntology : g.getAllOntologies()) {
						for (OWLObject cls : owlObjectCachedSet) {
							if (cls instanceof OWLClass) {
								// TODO - translate equivalence axioms; assume inferred for now
								for (OWLAxiom ax : mergeOntology.getAxioms((OWLClass)cls)) {
									boolean ok = true;
									for (OWLClass refCls : ax.getClassesInSignature()) {
										if (!owlObjectCachedSet.contains(refCls)) {
											LOG.info("Skipping: "+ax);
											ok = false;
											break;
										}
									}
									if (ok)
										subsetAxioms.add(ax);
								}
								for (OWLAxiom ax : mergeOntology.getAnnotationAssertionAxioms(((OWLClass)cls).getIRI())) {
									subsetAxioms.add(ax);
								}
							}
							subsetAxioms.add(g.getDataFactory().getOWLDeclarationAxiom(((OWLClass)cls)));
						}
					}
					for (OWLAxiom ax : subsetAxioms) {
						objPropsUsed.addAll(ax.getObjectPropertiesInSignature());
					}
					for (OWLObjectProperty p : objPropsUsed) {
						for (OWLOntology mergeOntology : g.getAllOntologies()) {
							subsetAxioms.addAll(mergeOntology.getAxioms(p));
							subsetAxioms.addAll(mergeOntology.getAnnotationAssertionAxioms(p.getIRI()));
						}
					}

					OWLOntology subOnt = g.getManager().createOntology(IRI.create(subOntologyIRI));
					g.getManager().addAxioms(subOnt, subsetAxioms);
					g.setSourceOntology(subOnt);
				}
			}
			else if (opts.nextEq("--reasoner-ask-all")) {
				opts.info("[-r REASONERNAME] [-s] [-a] AXIOMTYPE", "list all inferred equivalent named class pairs");
				boolean isReplaceOntology = false;
				boolean isAddToCurrentOntology = false;
				boolean isDirect = true;
				boolean isRemoveIndirect = false;
				while (opts.hasOpts()) {
					if (opts.nextEq("-r")) {
						opts.info("REASONERNAME", "E.g. elk");
						reasonerName = opts.nextOpt();
					}
					else if (opts.nextEq("-s")) {
						opts.info("", "Replaces ALL axioms in ontology with inferred axioms");
						isReplaceOntology = true;
					}
					else if (opts.nextEq("-a")) {
						opts.info("", "Add inferred axioms to current ontology");
						isAddToCurrentOntology = true;
					}
					else if (opts.nextEq("--remove-indirect")) {
						opts.info("", "Remove indirect assertions from current ontology");
						isRemoveIndirect = true;
					}
					else if (opts.nextEq("--indirect")) {
						opts.info("", "Include indirect inferences");
						isDirect = false;
					}
					else {
						break;
					}
				}
				if (reasoner == null) {
					reasoner = createReasoner(g.getSourceOntology(),reasonerName,g.getManager());
				}
				if (isRemoveIndirect && !isAddToCurrentOntology) {
					System.err.println("You asked to remove indirect but not to assert direct - I am proceeding, but check this is what you want");
				}
				if (isRemoveIndirect && !isDirect) {
					System.err.println("You asked to remove indirect and yet you want indirect inferences - invalid combination");
					System.exit(1);
				}
				Set<OWLAxiom> iAxioms = new HashSet<OWLAxiom>();
				Set<OWLAxiom> rmAxioms = new HashSet<OWLAxiom>();
				String q = opts.nextOpt().toLowerCase();
				owlpp = new OWLPrettyPrinter(g);
				OWLOntology ont = g.getSourceOntology();
				for (OWLClass c : g.getSourceOntology().getClassesInSignature()) {
					if (q.startsWith("e")) {
						for (OWLClass ec : reasoner.getEquivalentClasses(c)) {
							OWLEquivalentClassesAxiom ax = g.getDataFactory().getOWLEquivalentClassesAxiom(c, ec);
							if (!ont.containsAxiom(ax, true)) {
								LOG.info("INFERRED: "+owlpp.render(ax));
								iAxioms.add(ax);
							}
						}
					}
					else if (q.startsWith("s")) {
						Set<OWLClass> supers = reasoner.getSuperClasses(c, isDirect).getFlattened();
						for (OWLClass sc : supers) {
							OWLSubClassOfAxiom ax = g.getDataFactory().getOWLSubClassOfAxiom(c, sc);
							if (!ont.containsAxiom(ax, true)) {
								LOG.info("INFERRED: "+owlpp.render(ax));
								iAxioms.add(ax);
							}
						}
						if (isRemoveIndirect) {
							for (OWLClass sc : reasoner.getSuperClasses(c, false).getFlattened()) {
								if (!supers.contains(sc)) {
									OWLSubClassOfAxiom ax = g.getDataFactory().getOWLSubClassOfAxiom(c, sc);
									if (ont.containsAxiom(ax, true)) {
										rmAxioms.add(ax);
										LOG.info("INDIRECT: "+owlpp.render(ax));
									}
								}
							}							
						}
					}
				}
				if (q.startsWith("i")) {
					for (OWLNamedIndividual i : g.getSourceOntology().getIndividualsInSignature()) {
						Set<OWLClass> types = reasoner.getTypes(i, isDirect).getFlattened();
						for (OWLClass ce : types) {
							OWLClassAssertionAxiom ax = g.getDataFactory().getOWLClassAssertionAxiom(ce, i);
							if (!ont.containsAxiom(ax, true)) {
								LOG.info("INFERRED: "+owlpp.render(ax));
								iAxioms.add(ax);
							}
						}
						if (isRemoveIndirect) {
							for (OWLClass ce : reasoner.getTypes(i, false).getFlattened()) {
								if (!types.contains(ce)) {
									OWLClassAssertionAxiom ax = g.getDataFactory().getOWLClassAssertionAxiom(ce, i);
									if (ont.containsAxiom(ax, false)) {
										rmAxioms.add(ax);
										LOG.info("INDIRECT: "+owlpp.render(ax));
									}
								}
							}							
						}

					}
				}
				if (isReplaceOntology) {
					Set<OWLAxiom> allAxioms = ont.getAxioms();
					g.getManager().removeAxioms(ont, allAxioms);
					g.getManager().addAxioms(ont, iAxioms);
				}
				if (isAddToCurrentOntology) {
					System.out.println("Adding "+iAxioms.size()+" axioms");
					g.getManager().addAxioms(ont, iAxioms);
				}
				rmAxioms.retainAll(ont.getAxioms());
				if (rmAxioms.size() > 0) {
					System.out.println("Removing "+rmAxioms.size()+" axioms");
					g.getManager().removeAxioms(ont, rmAxioms);
				}
			}
			else if (opts.nextEq("--run-reasoner")) {
				opts.info("[-r reasonername] [--assert-implied] [--indirect]", "infer new relationships");
				boolean isAssertImplied = false;
				boolean isDirect = true;
				boolean isShowUnsatisfiable = false;

				while (opts.hasOpts()) {
					if (opts.nextEq("-r")) {
						reasonerName = opts.nextOpt();
					}
					else if (opts.nextEq("--assert-implied")) {
						isAssertImplied = true;
					}
					else if (opts.nextEq("--indirect")) {
						isDirect = false;
					}
					else if (opts.nextEq("-u|--list-unsatisfiable")) {
						isShowUnsatisfiable = true;
					}
					else {
						break;
					}
				}
				owlpp = new OWLPrettyPrinter(g);

				boolean isQueryProcessed = false;
				if (reasoner == null) {
					reasoner = createReasoner(g.getSourceOntology(),reasonerName,g.getManager());
				}
				if (isShowUnsatisfiable) {
					int n = 0;
					// NOTE: 
					for (OWLClass c : reasoner.getEquivalentClasses(g.getDataFactory().getOWLNothing())) {
						if (g.getDataFactory().getOWLNothing().equals(c))
							continue;
						System.out.println("UNSAT: "+owlpp.render(c));
						n++;
					}
					System.out.println("NUMBER_OF_UNSATISFIABLE_CLASSES: "+n);
					if (n > 0) {
						System.exit(1);
					}
				}

				if (opts.hasOpts()) {
					if (opts.nextEq("-i")) {
						OWLClass qc = (OWLClass)resolveEntity(opts);
						System.out.println("Getting individuals of class: "+qc);
						for (Node<OWLNamedIndividual> ni : reasoner.getInstances(qc, false)) {
							for (OWLNamedIndividual i : ni.getEntities()) {
								System.out.println(i);
							}
						}
						isQueryProcessed = true;
					}
				}
				if (queryExpressionMap != null) {
					// Assume --query-ontontology -m ONT has been processed
					for (OWLClass qc : queryExpressionMap.keySet()) {
						System.out.println(" CWQueryClass: "+owlpp.render(qc)+" "+qc.getIRI().toString());
						OWLClassExpression ec = queryExpressionMap.get(qc);
						System.out.println(" CWQueryExpression: "+owlpp.render(ec));
						// note jcel etc will not take class expressions
						NodeSet<OWLClass> results = reasoner.getSubClasses(qc, false);
						for (OWLClass result : results.getFlattened()) {
							if (reasoner.isSatisfiable(result)) {
								System.out.println("  "+owlpp.render(result));
							}
							else {
								// will not report unsatisfiable classes, as they trivially
								//LOG.error("unsatisfiable: "+owlpp.render(result));
							}
						}

					}
					isQueryProcessed = true;
				}

				if (!isQueryProcessed) {
					if (removedSubClassOfAxioms != null) {
						System.out.println("attempting to recapitulate "+removedSubClassOfAxioms.size()+" axioms");
						for (OWLSubClassOfAxiom a : removedSubClassOfAxioms) {
							OWLClassExpression sup = a.getSuperClass();
							if (sup instanceof OWLClass) {
								boolean has = false;
								for (Node<OWLClass> isup : reasoner.getSuperClasses(a.getSubClass(),false)) {
									if (isup.getEntities().contains(sup)) {
										has = true;
										break;
									}
								}
								System.out.print(has ? "POSITIVE: " : "NEGATIVE: ");
								System.out.println(owlpp.render(a));
							}
						}
					}
					System.out.println("all inferences");
					System.out.println("Consistent? "+reasoner.isConsistent());
					if (!reasoner.isConsistent()) {
						for (OWLClass c : reasoner.getUnsatisfiableClasses()) {
							System.out.println("UNSAT: "+owlpp.render(c));
						}
					}
					for (OWLObject obj : g.getAllOWLObjects()) {
						if (obj instanceof OWLClass) {
							Set<OWLClassExpression> assertedSuperclasses =
								((OWLClass) obj).getSuperClasses(g.getSourceOntology());
							//System.out.println(obj+ " #subclasses:"+
							//		reasoner.getSubClasses((OWLClassExpression) obj, false).getFlattened().size());
							for (OWLClass sup : reasoner.getSuperClasses((OWLClassExpression) obj, isDirect).getFlattened()) {
								if (assertedSuperclasses.contains(sup)) {
									continue;
								}
								System.out.println("INFERENCE: "+owlpp.render(obj)+" SubClassOf "+owlpp.render(sup));
								if (isAssertImplied) {
									OWLSubClassOfAxiom sca = g.getDataFactory().getOWLSubClassOfAxiom((OWLClass)obj, sup);
									g.getManager().addAxiom(g.getSourceOntology(), sca);
								}
							}
							for (OWLClass ec : reasoner.getEquivalentClasses(((OWLClassExpression) obj)).getEntities()) {
								if (!ec.equals(obj))
									System.out.println("INFERENCE: "+owlpp.render(obj)+" EquivalentTo "+owlpp.render(ec));
							}
						}
					}
				}
			}
			else if (opts.nextEq("--stash-subclasses")) {
				opts.info("[-a][--prefix PREFIX][--ontology RECAP-ONTOLOGY-IRI", 
				"removes all subclasses in current source ontology; after reasoning, try to re-infer these");
				boolean isDefinedOnly = true;
				Set<String> prefixes = new HashSet<String>();
				OWLOntology recapOnt = g.getSourceOntology();


				while (opts.hasOpts()) {
					if (opts.nextEq("--prefix")) {
						prefixes.add(opts.nextOpt());
					}
					else if (opts.nextEq("-a")) {
						isDefinedOnly = false;
					}
					else if (opts.nextEq("--ontology")) {
						IRI ontIRI = IRI.create(opts.nextOpt());
						recapOnt = g.getManager().getOntology(ontIRI);
						if (recapOnt == null) {
							LOG.error("Cannot find ontology: "+ontIRI+" from "+g.getManager().getOntologies().size());
							for (OWLOntology ont : g.getManager().getOntologies()) {
								LOG.error("  I have: "+ont.getOntologyID().getOntologyIRI().toString());
							}
							for (OWLOntology ont : g.getSourceOntology().getImportsClosure()) {
								LOG.error("  IC: "+ont.getOntologyID().getOntologyIRI().toString());
							}
						}
					}
					else {
						break;
					}
				}

				Set<OWLSubClassOfAxiom> allAxioms = recapOnt.getAxioms(AxiomType.SUBCLASS_OF);
				removedSubClassOfAxioms = new HashSet<OWLSubClassOfAxiom>();
				System.out.println("Testing "+allAxioms.size()+" SubClassOf axioms for stashing. Prefixes: "+prefixes.size());
				HashSet<RemoveAxiom> rmaxs = new HashSet<RemoveAxiom>();
				for (OWLSubClassOfAxiom a : allAxioms) {
					OWLClassExpression subc = a.getSubClass();
					if (!(subc instanceof OWLClass)) {
						continue;
					}
					OWLClassExpression supc = a.getSuperClass();
					if (!(supc instanceof OWLClass)) {
						continue;
					}
					if (prefixes.size() > 0) {
						boolean skip = true;
						for (String p : prefixes) {
							if (((OWLClass) subc).getIRI().toString().startsWith(p)) {
								skip = false;
								break;
							}
						}
						if (skip)
							break;
					}
					if (isDefinedOnly) {
						// TODO - imports closure
						if (((OWLClass)subc).getEquivalentClasses(g.getSourceOntology()).size() == 0) {
							continue;
						}
						if (((OWLClass)supc).getEquivalentClasses(g.getSourceOntology()).size() == 0) {
							continue;
						}
					}
					// TODO: remove it from the ontology in which it's asserted
					RemoveAxiom rmax = new RemoveAxiom(recapOnt,a);
					LOG.debug("WILL_REMOVE: "+a);
					rmaxs.add(rmax);
					removedSubClassOfAxioms.add(g.getDataFactory().getOWLSubClassOfAxiom(a.getSubClass(), a.getSuperClass()));
				}
				System.out.println("Will remove "+rmaxs.size()+" axioms");
				for (RemoveAxiom rmax : rmaxs) {
					g.getManager().applyChange(rmax);
				}
			}
			else if (opts.nextEq("--list-cycles")) {
				boolean failOnCycle = false;
				if (opts.nextEq("-f|--fail-on-cycle")) {
					failOnCycle = true;
				}
				int n = 0;
				for (OWLObject x : g.getAllOWLObjects()) {
					for (OWLObject y : g.getAncestors(x)) {
						if (g.getAncestors(y).contains(x)) {
							System.out.println(x + " in-cycle-with "+y);
							n++;
						}
					}
				}
				System.out.println("Number of cycles: "+n);
				if (n > 0 && failOnCycle)
					System.exit(1);
			}
			else if (opts.nextEq("-a|--ancestors")) {
				opts.info("LABEL", "list edges in graph closure to root nodes");
				OWLObject obj = resolveEntity(opts);
				System.out.println(obj+ " "+obj.getClass());
				Set<OWLGraphEdge> edges = g.getOutgoingEdgesClosureReflexive(obj);
				showEdges(edges);
			}
			else if (opts.nextEq("--ancestor-nodes")) {
				opts.info("LABEL", "list nodes in graph closure to root nodes");
				OWLObject obj = resolveEntity( opts);
				System.out.println(obj+ " "+obj.getClass());
				for (OWLObject a : g.getAncestors(obj)) 
					System.out.println(a);
			}
			else if (opts.nextEq("--parents-named")) {
				opts.info("LABEL", "list direct outgoing edges to named classes");
				OWLObject obj = resolveEntity( opts);
				System.out.println(obj+ " "+obj.getClass());
				Set<OWLGraphEdge> edges = g.getOutgoingEdges(obj);
				showEdges( edges);
			}
			else if (opts.nextEq("--parents")) {
				opts.info("LABEL", "list direct outgoing edges");
				OWLObject obj = resolveEntity( opts);
				System.out.println(obj+ " "+obj.getClass());
				Set<OWLGraphEdge> edges = g.getPrimitiveOutgoingEdges(obj);
				showEdges( edges);
			}
			else if (opts.nextEq("--grandparents")) {
				opts.info("LABEL", "list direct outgoing edges and their direct outgoing edges");
				OWLObject obj = resolveEntity( opts);
				System.out.println(obj+ " "+obj.getClass());
				Set<OWLGraphEdge> edges = g.getPrimitiveOutgoingEdges(obj);
				for (OWLGraphEdge e1 : edges) {
					System.out.println(e1);
					for (OWLGraphEdge e2 : g.getPrimitiveOutgoingEdges(e1.getTarget())) {
						System.out.println("    "+e2);

					}
				}
			}
			else if (opts.nextEq("--subsumers")) {
				opts.info("LABEL", "list named subsumers and subsuming expressions");
				OWLObject obj = resolveEntity( opts);
				Set<OWLObject> ancs = g.getSubsumersFromClosure(obj);
				for (OWLObject a : ancs) {
					System.out.println(a);
				}
			}
			else if (opts.nextEq("--incoming-edges")) {
				opts.info("LABEL", "list edges in graph to leaf nodes");
				OWLObject obj = resolveEntity( opts);
				System.out.println(obj+ " "+obj.getClass());
				Set<OWLGraphEdge> edges = g.getIncomingEdges(obj);
				showEdges( edges);
			}
			else if (opts.nextEq("--descendant-edges")) {
				opts.info("LABEL", "list edges in graph closure to leaf nodes");
				OWLObject obj = resolveEntity( opts);
				System.out.println(obj+ " "+obj.getClass());
				Set<OWLGraphEdge> edges = g.getIncomingEdgesClosure(obj);
				showEdges( edges);
			}
			else if (opts.nextEq("--descendants")) {
				opts.info("LABEL", "show all descendant nodes");
				OWLObject obj = resolveEntity( opts);
				owlpp = new OWLPrettyPrinter(g);
				System.out.println("#" + obj+ " "+obj.getClass()+" "+owlpp.render(obj));
				Set<OWLObject> ds = g.getDescendants(obj);
				for (OWLObject d : ds)
					System.out.println(d);
			}
			else if (opts.nextEq("--subsumed-by")) {
				opts.info("LABEL", "show all descendant nodes");
				OWLObject obj = resolveEntity( opts);
				System.out.println(obj+ " "+obj.getClass());
				Set<OWLObject> ds = g.queryDescendants((OWLClass)obj);
				for (OWLObject d : ds)
					System.out.println(d);
			}
			else if (opts.nextEq("-l") || opts.nextEq("--list-axioms")) {
				opts.info("LABEL", "lists all axioms for entity matching LABEL");
				OWLObject obj = resolveEntity( opts);
				owlpp = new OWLPrettyPrinter(g);
				owlpp.print("## Showing axiom for: "+obj);
				Set<OWLAxiom> axioms = g.getSourceOntology().getReferencingAxioms((OWLEntity) obj);
				owlpp.print(axioms);
				Set<OWLAnnotationAssertionAxiom> aaxioms = g.getSourceOntology().getAnnotationAssertionAxioms(((OWLNamedObject) obj).getIRI());
				for (OWLAxiom a : aaxioms) {
					System.out.println(owlpp.render(a));

				}
			}
			else if (opts.nextEq("--obsolete-class")) {
				opts.info("LABEL", "Add a deprecation axiom");
				OWLObject obj = resolveEntity( opts);
				owlpp = new OWLPrettyPrinter(g);
				owlpp.print("## Obsoleting: "+obj);
				Set<OWLAxiom> refAxioms = g.getSourceOntology().getReferencingAxioms((OWLEntity) obj);
				Set<OWLClassAxiom> axioms = g.getSourceOntology().getAxioms((OWLClass) obj);
				Set<OWLAxiom> rmAxioms = new HashSet<OWLAxiom>();
				for (OWLAxiom ax : axioms) {
					if (ax.isLogicalAxiom()) {
						rmAxioms.add(ax);
						System.out.println("REMOVING:"+owlpp.render(ax));
					}
				}
				for (OWLAxiom ax : refAxioms) {
					if (ax.isLogicalAxiom() && !rmAxioms.contains(ax)) {
						System.err.println("UH-OH: "+ax);
					}
				}
				g.getManager().removeAxioms(g.getSourceOntology(), rmAxioms);

				System.err.println("TODO");
			}
			else if (opts.nextEq("-d") || opts.nextEq("--draw")) {
				opts.info("[-o FILENAME] [-f FMT] LABEL/ID", "generates a file tmp.png made using QuickGO code");
				String imgf = "tmp.png";
				String fmt = "png";
				while (opts.hasOpts()) {
					if (opts.nextEq("-o")) {
						opts.info("FILENAME", "name of png file to save (defaults to tmp.png)");
						imgf = opts.nextOpt();
					}
					else if (opts.nextEq("-f")) {
						opts.info("FMT", "image format. See ImageIO docs for a list. Default: png");
						fmt = opts.nextOpt();
						if (imgf.equals("tmp.png")) {
							imgf = "tmp."+fmt;
						}
					}
					else if (opts.nextEq("-p")) {
						OWLObjectProperty p = resolveObjectProperty(opts.nextOpt());
						RelationConfig rc = gfxCfg.new RelationConfig();
						rc.color = Color.MAGENTA;
						gfxCfg.relationConfigMap.put(p, rc);
					}
					else {
						break;
					}
				}
				OWLObject obj = resolveEntity( opts);
				System.out.println(obj);
				OWLGraphLayoutRenderer r = new OWLGraphLayoutRenderer(g);
				r.graphicsConfig = gfxCfg;

				r.addObject(obj);
				r.renderImage(fmt, new FileOutputStream(imgf));
				//Set<OWLGraphEdge> edges = g.getOutgoingEdgesClosureReflexive(obj);
				//showEdges( edges);
			}
			else if (opts.nextEq("--draw-all")) {
				opts.info("", "draws ALL objects in the ontology (caution: small ontologies only)");
				OWLGraphLayoutRenderer r = new OWLGraphLayoutRenderer(g);

				r.addAllObjects();
				r.renderImage("png", new FileOutputStream("tmp.png"));
			}
			else if (opts.nextEq("--dump-node-attributes")) {
				opts.info("", "dumps all nodes attributes in CytoScape compliant format");
				FileOutputStream fos;
				PrintStream stream = null;
				try {
					fos = new FileOutputStream(opts.nextOpt());
					stream = new PrintStream(new BufferedOutputStream(fos));
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				stream.println("Label");
				for (OWLObject obj : g.getAllOWLObjects()) {
					String label = g.getLabel(obj);
					if (label != null)
						stream.println(g.getIdentifier(obj)+"\t=\t"+label);
				}
				stream.close();
			}
			else if (opts.nextEq("--dump-sif")) {
				opts.info("", "dumps CytoScape compliant sif format");
				FileOutputStream fos;
				PrintStream stream = null;
				try {
					fos = new FileOutputStream(opts.nextOpt());
					stream = new PrintStream(new BufferedOutputStream(fos));
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				for (OWLObject x : g.getAllOWLObjects()) {
					for (OWLGraphEdge e : g.getOutgoingEdges(x)) {
						OWLQuantifiedProperty qp = e.getSingleQuantifiedProperty();
						String label;
						if (qp.getProperty() != null)
							label = qp.getProperty().toString();
						else
							label = qp.getQuantifier().toString();
						if (label != null)
							stream.println(g.getIdentifier(x)+"\t"+label+"\t"+g.getIdentifier(e.getTarget()));

					}
				}
				stream.close();
			}
			else if (opts.nextEq("--sic|--slurp-import-closure")) {
				opts.info("[-d DIR] [-c CATALOG-OUT]","Saves local copy of import closure. Assumes sourceontology has imports");
				String dir = ".";
				String catfile = null;
				while (opts.hasOpts()) {
					if (opts.nextEq("-d")) {
						dir = opts.nextOpt();
					}
					else if (opts.nextEq("-c")) {
						catfile = opts.nextOpt();
					}
					else {
						break;
					}
				}
				ImportClosureSlurper ics = new ImportClosureSlurper(g.getSourceOntology());
				ics.save(dir, catfile);
			}
			else if (opts.nextEq("-o|--output")) {
				opts.info("FILE", "writes source ontology -- MUST BE specified as IRI, e.g. file://`pwd`/foo.owl");
				OWLOntologyFormat ofmt = new RDFXMLOntologyFormat();
				if ( g.getSourceOntology().getOntologyID() != null && g.getSourceOntology().getOntologyID().getOntologyIRI() != null) {
					String ontURIStr = g.getSourceOntology().getOntologyID().getOntologyIRI().toString();
					System.out.println("saving:"+ontURIStr);
				}
				if (opts.nextEq("-f")) {
					String ofmtname = opts.nextOpt();
					if (ofmtname.equals("manchester") || ofmtname.equals("omn")) {
						ofmt = new ManchesterOWLSyntaxOntologyFormat();
					}
					else if (ofmtname.equals("functional") || ofmtname.equals("ofn")) {
						ofmt = new OWLFunctionalSyntaxOntologyFormat();
					}
					else if (ofmtname.equals("turtle") || ofmtname.equals("ttl")) {
						ofmt = new TurtleOntologyFormat();
					}
					else if (ofmtname.equals("xml") || ofmtname.equals("owx")) {
						ofmt = new OWLXMLOntologyFormat();
					}
					else if (ofmtname.equals("ojs")) {
						ofmt = new OWLJSONFormat();
					}
					else if (ofmtname.equals("obo")) {
						ofmt = new OBOOntologyFormat();
					}
				}

				pw.saveOWL(g.getSourceOntology(), ofmt, opts.nextOpt(), g);
				//pw.saveOWL(g.getSourceOntology(), opts.nextOpt());
			}
			else if (opts.nextEq("--list-axioms")) {
				for (OWLAxiom a : g.getSourceOntology().getAxioms()) {
					System.out.println("AX:"+a);
				}
			}
			else if (opts.nextEq("--translate-undeclared-to-classes")) {
				for (OWLAnnotationAssertionAxiom a : g.getSourceOntology().getAxioms(AxiomType.ANNOTATION_ASSERTION)) {
					OWLAnnotationSubject sub = a.getSubject();
					if (sub instanceof IRI) {
						OWLObject e = g.getOWLObject(((IRI)sub));
						if (e == null) {
							OWLClass c = g.getDataFactory().getOWLClass((IRI)sub);
							OWLDeclarationAxiom ax = g.getDataFactory().getOWLDeclarationAxiom(c);
							g.getManager().addAxiom(g.getSourceOntology(), ax);
						}

					}
				}
			}
			else if (opts.nextEq("--show-metadata")) {
				OntologyMetadata omd = new OntologyMetadata();
				omd.generate(g);
			}
			else if (opts.nextEq("--follow-subclass")) {
				opts.info("", "follow subclass axioms (and also equivalence axioms) in graph traversal.\n"+
				"     default is to follow ALL. if this is specified then only explicitly specified edges followed");
				if (g.getConfig().graphEdgeIncludeSet == null)
					g.getConfig().graphEdgeIncludeSet = new HashSet<OWLQuantifiedProperty>();

				g.getConfig().graphEdgeIncludeSet.add(new OWLQuantifiedProperty(Quantifier.SUBCLASS_OF));	
			}
			else if (opts.nextEq("--follow-property")) {
				opts.info("PROP-LABEL", "follow object properties of this type in graph traversal.\n"+
				"     default is to follow ALL. if this is specified then only explicitly specified edges followed");
				OWLObjectProperty p = (OWLObjectProperty) resolveEntity( opts);
				if (g.getConfig().graphEdgeIncludeSet == null)
					g.getConfig().graphEdgeIncludeSet = new HashSet<OWLQuantifiedProperty>();

				g.getConfig().graphEdgeIncludeSet.add(new OWLQuantifiedProperty(p, null));	
			}
			else if (opts.nextEq("--exclude-property")) {
				opts.info("PROP-LABEL", "exclude object properties of this type in graph traversal.\n"+
				"     default is to exclude NONE.");
				OWLObjectProperty p = g.getOWLObjectProperty(opts.nextOpt());
				System.out.println("Excluding "+p+" "+p.getClass());
				if (g.getConfig().graphEdgeExcludeSet == null)
					g.getConfig().graphEdgeExcludeSet = new HashSet<OWLQuantifiedProperty>();

				g.getConfig().graphEdgeExcludeSet.add(new OWLQuantifiedProperty(p, null));	
			}
			else if (opts.nextEq("--exclusion-annotation-property")) {
				opts.info("[-o ONT] PROP-LABEL", "exclude object properties of this type in graph traversal.\n"+
				"     default is to exclude NONE.");
				OWLOntology xo = g.getSourceOntology();
				if (opts.hasOpts()) {
					if (opts.nextEq("-o")) {
						xo = pw.parse(opts.nextOpt());
					}
					else
						break;
				}
				OWLAnnotationProperty ap = (OWLAnnotationProperty) g.getOWLObjectByLabel(opts.nextOpt());				
				g.getConfig().excludeAllWith(ap, xo);	
			}
			else if (opts.nextEq("--inclusion-annotation-property")) {
				opts.info("[-o ONT] PROP-LABEL", "include object properties of this type in graph traversal.\n"+
				"     default is to include NONE.");
				OWLOntology xo = g.getSourceOntology();
				if (opts.hasOpts()) {
					if (opts.nextEq("-o")) {
						xo = pw.parse(opts.nextOpt());
					}
					else
						break;
				}
				OWLAnnotationProperty ap = (OWLAnnotationProperty) g.getOWLObjectByLabel(opts.nextOpt());				
				g.getConfig().includeAllWith(ap, xo);	
			}
			else if (opts.nextEq("--exclude-metaclass")) {
				opts.info("METACLASS-LABEL", "exclude classes of this type in graph traversal.\n"+
				"     default is to follow ALL classes");
				OWLClass c = (OWLClass) resolveEntity( opts);

				g.getConfig().excludeMetaClass = c;	
			}
			else if (opts.nextEq("--parse-tsv")) {
				opts.info("[-s] [-p PROPERTY] [-a AXIOMTYPE] [-t INDIVIDUALSTYPE] FILE", "parses a tabular file to OWL axioms");
				TableToAxiomConverter ttac = new TableToAxiomConverter(g);
				ttac.config.axiomType = AxiomType.CLASS_ASSERTION;
				while (opts.hasOpts()) {
					if (opts.nextEq("-s|--switch")) {
						opts.info("", "switch subject and object");
						ttac.config.isSwitchSubjectObject = true;
					}
					else if (opts.nextEq("-l|--label")) {
						ttac.config.setPropertyToLabel();
						ttac.config.axiomType = AxiomType.ANNOTATION_ASSERTION;
					}
					else if (opts.nextEq("--comment")) {
						ttac.config.setPropertyToComment();
						ttac.config.axiomType = AxiomType.ANNOTATION_ASSERTION;
					}
					else if (opts.nextEq("-m|--map-xrefs")) {
						ttac.buildClassMap(g);
					}
					else if (opts.nextEq("-p|--prop")) {
						ttac.config.property = ((OWLNamedObject) resolveObjectProperty( opts.nextOpt())).getIRI();
						//ttac.config.property = g.getOWLObjectProperty().getIRI();
					}
					else if (opts.nextEq("--default1")) {
						ttac.config.defaultCol1 = opts.nextOpt();
					}
					else if (opts.nextEq("--default2")) {
						ttac.config.defaultCol2 = opts.nextOpt();
					}
					else if (opts.nextEq("--iri-prefix")) {
						int col = 0;
						String x = opts.nextOpt();
						if (x.equals("1") || x.startsWith("s")) {
							col = 1;
						}
						else if (x.equals("2") || x.startsWith("o")) {
							col = 2;
						}
						else {
							//
						}
						String pfx = opts.nextOpt();
						// note that we do not put the full URI prefix here for now
						//if (!pfx.startsWith("http:"))
						//	pfx = "http://purl.obolibrary.org/obo/" + pfx + "_";
						if (pfx.startsWith("http:"))
							ttac.config.iriPrefixMap.put(col, pfx);
						else
							ttac.config.iriPrefixMap.put(col, pfx+":");
					}
					else if (opts.nextEq("-a|--axiom-type")) {
						ttac.config.setAxiomType(opts.nextOpt());
					}
					else if (opts.nextEq("-t|--individuals-type")) {
						System.out.println("setting types");
						ttac.config.individualsType = resolveClass( opts.nextOpt());
					}
					else {
						throw new OptionException(opts.nextOpt());
					}
				}
				String f = opts.nextOpt();
				System.out.println("tabfile: "+f);
				ttac.parse(f);
			}
			else if (opts.nextEq("--idmap-extract-pairs")) {
				opts.info("IDType1 IDType2 PIRMapFile", "extracts pairs from mapping file");
				IDMappingPIRParser p = new IDMappingPIRParser();
				IDMapPairWriter h = new IDMapPairWriter();
				h.setPair(opts.nextOpt(), opts.nextOpt());
				p.handler = h;
				p.parse(new File(opts.nextOpt()));				
			}
			else if (opts.nextEq("--parser-idmap")) {
				opts.info("UniProtIDMapFile", "...");
				UniProtIDMapParser p = new UniProtIDMapParser();
				p.parse(new File(opts.nextOpt()));		
				System.out.println("Types:"+p.idMap.size());
				// TODO...
			}
			else if (opts.nextEq("--extract-module")) {
				opts.info("SEED-OBJECTS", "Uses the OWLAPI module extractor");
				String modIRI = null;
				ModuleType mtype = ModuleType.STAR;
				boolean isTraverseDown = false;
				while (opts.hasOpts()) {
					if (opts.nextEq("-n")) {
						modIRI = opts.nextOpt();
					}
					else if (opts.nextEq("-d")) {
						isTraverseDown = true;
					}
					else if (opts.nextEq("-m") || opts.nextEq("--module-type")) {
						opts.info("MODULE-TYPE", "One of: STAR (default), TOP, BOT, TOP_OF_BOT, BOT_OF_TOP");
						mtype = ModuleType.valueOf(opts.nextOpt());
					}
					else {
						break;
					}
				}

				// module extraction not implemented for SAPs
				// this code can be replaced when this is fixed:
				// https://sourceforge.net/tracker/?func=detail&aid=3477470&group_id=90989&atid=595534
				Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
				for (OWLAxiom ax : g.getSourceOntology().getAxioms()) {
					if (ax instanceof OWLSubAnnotationPropertyOfAxiom)
						continue;
					axioms.add(ax);
				}

				OWLOntology baseOnt = g.getManager().createOntology(axioms);

				Set<OWLObject> objs = this.resolveEntityList(opts);

				Set<OWLEntity> seedSig = new HashSet<OWLEntity>();
				if (isTraverseDown) {
					OWLReasoner mr = this.createReasoner(baseOnt, reasonerName, g.getManager());
					try {
						for (OWLObject obj : objs) {
							if (obj instanceof OWLClassExpression) {
								seedSig.addAll(mr.getSubClasses((OWLClassExpression) obj, false).getFlattened());
							}
							else if (obj instanceof OWLObjectPropertyExpression) {
								for (OWLObjectPropertyExpression pe : mr.getSubObjectProperties((OWLObjectPropertyExpression) obj, false).getFlattened()) {
									if (pe instanceof OWLObjectProperty) {
										seedSig.add((OWLObjectProperty) pe);
									}
								}
							}
						}
					}
					finally {
						mr.dispose();
					}
				}
				SyntacticLocalityModuleExtractor sme = new SyntacticLocalityModuleExtractor(g.getManager(), baseOnt, mtype);
				for (OWLObject obj : objs) {
					if (obj instanceof OWLEntity) {
						seedSig.add((OWLEntity) obj);
					}
				}
				Set<OWLAxiom> modAxioms = sme.extract(seedSig);
				OWLOntology modOnt;
				if (modIRI == null) {
					modOnt = g.getManager().createOntology();
				}
				else {
					modOnt = g.getManager().createOntology(IRI.create(modIRI));
				}
				g.getManager().addAxioms(modOnt, modAxioms);
				g.setSourceOntology(modOnt);
			}
			else if (opts.nextEq("--translate-disjoint-to-equivalent|--translate-disjoints-to-equivalents")) {
				opts.info("", "adds (Xi and Xj  = Nothing) for every DisjointClasses(X1...Xn) where i<j<n");
				Mooncat m = new Mooncat(g);
				m.translateDisjointsToEquivalents();
			}
			else if (opts.nextEq("--build-property-view-ontology|--bpvo")) {
				opts.info("[-p PROPERTY] [-o OUTFILE] [-r REASONER] [--filter-unused] [--prefix STR] [--suffix STR] [--avfile FILE] [--i2c]", 
				"generates a new ontology O' from O using property P such that for each C in O, O' contains C' = P some C");
				OWLOntology sourceOntol = g.getSourceOntology();
				// TODO - for now assume exactly 0 or 1 support ontology; if 1, the support is the element ontology
				OWLOntology annotOntol;
				if (g.getSupportOntologySet().size() == 1)
					annotOntol = g.getSupportOntologySet().iterator().next();
				else if (g.getSupportOntologySet().size() == 0)
					annotOntol = g.getManager().createOntology();
				else
					throw new OptionException("must have zero or one support ontologies");

				OWLObjectProperty viewProperty = null;
				String outFile = null;
				String suffix = null;
				String prefix = null;
				boolean isFilterUnused = false;
				boolean isABoxToTBox = false;
				boolean isReplace = false;
				String avFile =  null;
				String viewIRI = "http://example.org/";
				while (opts.hasOpts()) {
					if (opts.nextEq("-p")) {
						opts.info("PROPERTY-ID-OR-LABEL", "The ObjectProperty P that is used to build the view");
						viewProperty = resolveObjectProperty(opts.nextOpt());
					}
					else if (opts.nextEq("-r")) {
						opts.info("REASONERNAME", "e.g. elk");
						reasonerName = opts.nextOpt();
					}
					else if (opts.nextEq("--prefix")) {
						opts.info("STR", "each class in O(P) will have this prefix in its label");
						prefix = opts.nextOpt();
					}
					else if (opts.nextEq("--suffix")) {
						opts.info("STR", "each class in O(P) will have this suffix in its label");
						suffix = opts.nextOpt();
					}
					else if (opts.nextEq("-o")) {
						opts.info("FILE", "file to save O(P)' [i.e. reasoned view ontology] into");
						outFile = opts.nextOpt();
					}
					else if (opts.nextEq("--view-iri")) {
						opts.info("IRI", "IRI for the view ontology");
						viewIRI = opts.nextOpt();
					}
					else if (opts.nextEq("--avfile")) {
						opts.info("FILE", "file to save O(P) [i.e. non-reasoner view ontology] into");
						avFile = opts.nextOpt();
					}
					else if (opts.nextEq("--filter-unused")) {
						opts.info("", "if set, any class or individual that is not subsumed by P some Thing is removed from O(P)");
						isFilterUnused = true;
					}
					else if (opts.nextEq("--replace")) {
						opts.info("", "if set, the source ontology is replaced with O(P)'");
						isReplace = true;
					}
					else if (opts.nextEq("" +
					"")) {
						annotOntol = g.getSourceOntology();
					}
					else if (opts.nextEq("--i2c")) {
						opts.info("", "if set, translate individuals to classes (e.g. for reasoning with Elk)");
						isABoxToTBox = true;
					}
					else
						break;
				}
				PropertyViewOntologyBuilder pvob = 
					new PropertyViewOntologyBuilder(sourceOntol,
							annotOntol,
							viewProperty);
				if (isABoxToTBox) {
					LOG.info("translation abox to tbox...");
					pvob.translateABoxToTBox();
				}
				pvob.setViewLabelPrefix(prefix);
				pvob.setViewLabelSuffix(suffix);
				pvob.buildViewOntology(IRI.create("http://x.org/assertedViewOntology"), IRI.create(viewIRI));
				pvob.setFilterUnused(isFilterUnused);
				OWLOntology avo = pvob.getAssertedViewOntology();
				if (avFile != null)
					pw.saveOWL(avo, avFile, g);
				OWLReasoner vr = createReasoner(avo, reasonerName, g.getManager());
				pvob.buildInferredViewOntology(vr);
				// save
				if (outFile != null)
					pw.saveOWL(pvob.getInferredViewOntology(), outFile, g);
				else if (isReplace) {
					g.setSourceOntology(pvob.getInferredViewOntology());
				}
				else {
					g.addSupportOntology(pvob.getInferredViewOntology());
				}
				vr.dispose();

			}
			else if (opts.nextEq("--report-profile")) {
				g.getProfiler().report();
			}
			else if (opts.nextEq("--no-cache")) {
				g.getConfig().isCacheClosure = false;
			}
			else if (opts.nextEq("--start-server")) {
				int port = 9000;
				while (opts.hasOpts()) {
					if (opts.nextEq("-p")) {
						port = Integer.parseInt(opts.nextOpt());
					}
					else {
						break;
					}
				}
				Server server = new Server(port);
				server.setHandler(new OWLServer(g));

				try {
					server.start();
					server.join();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else if (opts.nextEq("--create-ontology")) {
				opts.info("ONT-IRI", "creates a new OWLOntology and makes it the source ontology");
				g = new OWLGraphWrapper(opts.nextOpt());

			}
			else if (opts.nextEq("--parse-obo")) {
				String f  = opts.nextOpt();
				OWLOntology ont = pw.parseOBO(f);
				if (g == null)
					g =	new OWLGraphWrapper(ont);
				else {
					System.out.println("adding support ont "+ont);
					g.addSupportOntology(ont);
				}
			}
			else if (opts.hasArgs()) {
				// check first if it as an annotated method
				boolean called = false;
				Method[] methods = getClass().getMethods();
				for (Method method : methods) {
					CLIMethod cliMethod = method.getAnnotation(CLIMethod.class);
					if (cliMethod !=null) {
						if (opts.nextEq(cliMethod.value())) {
							called = true;
							try {
								method.invoke(this, opts);
							} catch (InvocationTargetException e) {
								// the underlying method has throw an exception
								Throwable cause = e.getCause();
								if (cause instanceof Exception) {
									throw ((Exception) cause);
								}
								throw e;
							}
						}
					}
				}
				if (called) {
					continue;
				}

				// Default is to treat argument as an ontology
				String f  = opts.nextOpt();
				try {
					OWLOntology ont = pw.parse(f);
					if (g == null) {
						g =	new OWLGraphWrapper(ont);
					}
					else {
						System.out.println("adding support ont "+ont);
						g.addSupportOntology(ont);
					}

				}
				catch (Exception e) {
					System.err.println("could not parse:"+f+" Exception:"+e);
					exit(1);
				}


				//paths.add(opt);
			}
			else {
				if (opts.isHelpMode())
					helpFooter();
				// should only reach here in help mode
			}
		}

		/*

		OWLGraphWrapper g;
		if (paths.size() == 0) {
			throw new Error("must specify at least one file");
		}

		if (paths.size() > 1) {
			if (merge) {
				// note: currently we can only merge obo files
				pw.parseOBOFiles(paths);
			}
			else {
				throw new Error("cannot load multiple files unless --merge is set");
			}
		}
		else {
			g =	pw.parseToOWLGraph(paths.get(0));
		}
		 */

		if (reasoner != null) {
			reasoner.dispose();
		}
	}

	@CLIMethod("--assert-inferred-subclass-axioms")
	public void assertInferredSubClassAxioms(Opts opts) throws Exception {
		boolean removeRedundant = true;
		boolean checkConsistency = true; 
		boolean useIsInferred = false;
		boolean ignoreNonInferredForRemove = false;
		boolean checkForNamedClassEquivalencies = true;
		while (opts.hasOpts()) {
			if (opts.nextEq("--removeRedundant"))
				removeRedundant = true;
			else if (opts.nextEq("--keepRedundant")) {
				removeRedundant = false;
			}
			else if (opts.nextEq("--markIsInferred")) {
				useIsInferred = true;
			}
			else if (opts.nextEq("--useIsInferred")) {
				useIsInferred = true;
				ignoreNonInferredForRemove = true;
			}
			else if (opts.nextEq("--ignoreNonInferredForRemove")) {
				ignoreNonInferredForRemove = true;
			}
			else if (opts.nextEq("--allowEquivalencies")) {
				checkForNamedClassEquivalencies = false;
			}
			else {
				break;
			}
		}
		AssertInferenceTool.assertInferences(g, removeRedundant, checkConsistency, useIsInferred, ignoreNonInferredForRemove, checkForNamedClassEquivalencies);
	}

	@CLIMethod("--create-biochebi")
	public void createBioChebi(Opts opts) throws Exception {
		String output = null;
		while (opts.hasOpts()) {
			if (opts.nextEq("-o|--output")) {
				output = opts.nextOpt();
			}
			else {
				break;
			}
		}
		if (g == null) {
			// load default template
			InputStream stream = loadResource("bio-chebi-input.owl");
			if (stream == null) {
				throw new RuntimeException("Could not load default bio chebi input file: 'bio-chebi-input.owl'");
			}
			g = new OWLGraphWrapper(pw.getManager().loadOntologyFromOntologyDocument(stream));
		}
		BioChebiGenerator.createBioChebi(g);
		if (output != null) {
			OWLOntology ontology = g.getSourceOntology();
			File outFile = new File(output);
			ontology.getOWLOntologyManager().saveOntology(ontology, IRI.create(outFile));
		}
	}

	private InputStream loadResource(String name) {
		InputStream inputStream = getClass().getResourceAsStream(name);
		if (inputStream == null) {
			inputStream = ClassLoader.getSystemResourceAsStream(name);
		}
		if (inputStream == null) {
			File file = new File(name);
			if (file.isFile() && file.canRead()) {
				try {
					return new FileInputStream(file);
				} catch (FileNotFoundException exception) {
					// intentionally empty
				}
			}
		}
		return inputStream;
	}


	private OWLReasoner createReasoner(OWLOntology ont, String reasonerName, 
			OWLOntologyManager manager) {
		OWLReasonerFactory reasonerFactory = null;
		OWLReasoner reasoner;
		LOG.info("Creating reasoner:"+reasonerName);
		if (reasonerName == null || reasonerName.equals("factpp"))
			reasonerFactory = new FaCTPlusPlusReasonerFactory();
		else if (reasonerName.equals("hermit")) {
			reasonerFactory = new org.semanticweb.HermiT.Reasoner.ReasonerFactory();			
		}
		else if (reasonerName.equals("ogr")) {
			reasonerFactory = new GraphReasonerFactory();			
		}
		else if (reasonerName.equals("elk")) {
			reasonerFactory = new ElkReasonerFactory();	
		}
		else if (reasonerName.equals("welk")) {
			reasonerFactory = new LazyExpressionMaterializingReasonerFactory();	
		}
		else if (reasonerName.equals("cb")) {
			Class<?> rfc;
			try {
				rfc = Class.forName("org.semanticweb.cb.owlapi.CBReasonerFactory");
				reasonerFactory =(OWLReasonerFactory) rfc.newInstance();			
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else if (reasonerName.equals("jcel")) {
			System.out.println("making jcel reasoner with:"+ont);
			reasoner = new JcelReasoner(ont);
			reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
			return reasoner;
		}
		else
			System.out.println("no such reasoner: "+reasonerName);

		reasoner = reasonerFactory.createReasoner(ont);
		return reasoner;
	}

	private void removeDanging() {
		Mooncat m = new Mooncat(g);
		m.removeDanglingAxioms();
	}


	private void catOntologies(Opts opts) throws OWLOntologyCreationException, IOException {
		opts.info("[-r|--ref-ont ONT] [-i|--use-imports]", "Catenate ontologies taking only referenced subsets of supporting onts.\n"+
		"        See Mooncat docs");
		Mooncat m = new Mooncat(g);
		ParserWrapper pw = new ParserWrapper();
		String newURI = null;
		while (opts.hasOpts()) {
			//String opt = opts.nextOpt();
			if (opts.nextEq("-r") || opts.nextEq("--ref-ont")) {
				LOG.error("DEPRECATED - list all ref ontologies on main command line");
				String f = opts.nextOpt();
				m.addReferencedOntology(pw.parseOWL(f));
			}
			else if (opts.nextEq("-s") || opts.nextEq("--src-ont")) {
				m.setOntology(pw.parseOWL(opts.nextOpt()));
			}
			else if (opts.nextEq("-p") || opts.nextEq("--prefix")) {
				m.addSourceOntologyPrefix(opts.nextOpt());
			}
			else if (opts.nextEq("-i") || opts.nextEq("--use-imports")) {
				System.out.println("using everything in imports closure");
				g.addSupportOntologiesFromImportsClosure();
			}
			else if (opts.nextEq("-n") || opts.nextEq("--new-uri")) {
				System.out.println("new URI for merged ontology");
				newURI = opts.nextOpt();
			}
			else {
				break;
				//opts.fail();
			}
		}
		//if (m.getReferencedOntologies().size() == 0) {
		//	m.setReferencedOntologies(g.getSupportOntologySet());
		//}
		//g.useImportClosureForQueries();
		//for (OWLAxiom ax : m.getClosureAxiomsOfExternalReferencedEntities()) {
		//	System.out.println("M_AX:"+ax);
		//}

		m.mergeOntologies();
		m.removeDanglingAxioms();
		if (newURI != null) {
			SetOntologyID soi = new SetOntologyID(g.getSourceOntology(),
					new OWLOntologyID(IRI.create(newURI)));
			g.getManager().applyChange(soi);
			/*
			HashSet<OWLOntology> cpOnts = new HashSet<OWLOntology>();
			LOG.info("srcOnt annots:"+g.getSourceOntology().getAnnotations().size());
			cpOnts.add(g.getSourceOntology());
			OWLOntology newOnt = g.getManager().createOntology(IRI.create(newURI), cpOnts);
			LOG.info("newOnt annots:"+newOnt.getAnnotations().size());

			//g.getDataFactory().getOWLOn
			g.setSourceOntology(newOnt);
			 */
		}
	}

	private void showEdges(Set<OWLGraphEdge> edges) {
		OWLPrettyPrinter owlpp = new OWLPrettyPrinter(g);
		for (OWLGraphEdge e : edges) {
			System.out.println(owlpp.render(e));
		}
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

	public Set<OWLObjectProperty> resolveObjectPropertyList(Opts opts) {
		List<String> ids = opts.nextList();
		Set<OWLObjectProperty> objs = new HashSet<OWLObjectProperty>();
		for (String id: ids) {
			objs.add( this.resolveObjectProperty(id) );
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
		return g.getOWLObjectPropertyByIdentifier(id);
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
		return g.getDataFactory().getOWLClass(IRI.create(id));
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
