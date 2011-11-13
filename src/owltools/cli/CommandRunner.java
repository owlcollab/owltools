package owltools.cli;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.coode.owlapi.manchesterowlsyntax.ManchesterOWLSyntaxEditorParser;
import org.obolibrary.oboformat.model.FrameMergeException;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.expression.OWLEntityChecker;
import org.semanticweb.owlapi.expression.ParserException;
import org.semanticweb.owlapi.expression.ShortFormEntityChecker;
import org.semanticweb.owlapi.io.OWLFunctionalSyntaxOntologyFormat;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationSubject;
import org.semanticweb.owlapi.model.OWLAnonymousClassExpression;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyIRIMapper;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.RemoveAxiom;
import org.semanticweb.owlapi.model.SetOntologyID;
import org.semanticweb.owlapi.reasoner.FreshEntityPolicy;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;
import org.semanticweb.owlapi.util.AutoIRIMapper;
import org.semanticweb.owlapi.util.BidirectionalShortFormProviderAdapter;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import owltools.gaf.GafDocument;
import owltools.gaf.GafObjectsBuilder;
import owltools.gaf.GeneAnnotation;
import owltools.gaf.inference.AnnotationPredictor;
import owltools.gaf.inference.CompositionalClassPredictor;
import owltools.gaf.inference.Prediction;
import owltools.gfx.OWLGraphLayoutRenderer;
import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;
import owltools.graph.OWLQuantifiedProperty;
import owltools.graph.OWLQuantifiedProperty.Quantifier;
import owltools.idmap.IDMapPairWriter;
import owltools.idmap.IDMappingPIRParser;
import owltools.idmap.UniProtIDMapParser;
import owltools.io.ChadoGraphClosureRenderer;
import owltools.io.CompactGraphClosureReader;
import owltools.io.CompactGraphClosureRenderer;
import owltools.io.GraphClosureRenderer;
import owltools.io.GraphReader;
import owltools.io.GraphRenderer;
import owltools.io.OWLPrettyPrinter;
import owltools.io.ParserWrapper;
import owltools.io.TableToAxiomConverter;
import owltools.mooncat.Mooncat;
import owltools.ontologyrelease.OntologyMetadata;
import owltools.sim.DescriptionTreeSimilarity;
import owltools.sim.MultiSimilarity;
import owltools.sim.OWLObjectPair;
import owltools.sim.Reporter;
import owltools.sim.SimEngine;
import owltools.sim.SimEngine.SimilarityAlgorithmException;
import owltools.sim.SimSearch;
import owltools.sim.Similarity;
import uk.ac.manchester.cs.factplusplus.owlapiv3.FaCTPlusPlusReasonerFactory;

import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;

import de.derivo.sparqldlapi.Query;
import de.derivo.sparqldlapi.QueryEngine;
import de.derivo.sparqldlapi.QueryResult;
import de.derivo.sparqldlapi.exceptions.QueryEngineException;
import de.derivo.sparqldlapi.exceptions.QueryParserException;
import de.tudresden.inf.lat.jcel.owlapi.main.JcelReasoner;

/**
 * An instance of this class can execute owltools commands in sequence.
 * 
 * This is typically wrapper from within a main() method
 * 
 * 
 * @author cjm
 *
 */
public class CommandRunner {

	private static Logger LOG = Logger.getLogger(CommandRunner.class);

	OWLGraphWrapper g = null;
	GafDocument gafdoc = null;
	OWLOntology queryOntology = null;
	Map<OWLClass,OWLClassExpression> queryExpressionMap = null;

	public class Opts {
		int i = 0;
		String[] args;
		boolean helpMode = false;

		public Opts(String[] args) {
			super();
			this.i = 0;
			this.args = args;
		}
		public Opts(List<String> args) {
			super();
			this.i = 0;
			this.args = args.toArray(new String[args.size()]);
		}

		public boolean hasArgs() {
			return i < args.length;
		}
		public boolean hasOpts() {
			return hasArgs() && args[i].startsWith("-");
		}

		public boolean nextEq(String eq) {
			if (helpMode) {
				System.out.println("    "+eq);
				return false;
			}
			if (eq.contains("|")) {
				return nextEq(eq.split("\\|"));
			}
			if (hasArgs()) {
				if (args[i].equals(eq)) {
					i++;
					return true;
				}
			}
			return false;
		}
		private boolean nextEq(String[] eqs) {
			for (String eq : eqs) {
				if (nextEq(eq))
					return true;
			}
			return false;
		}

		public boolean hasOpt(String opt) {
			for (int j=i; j<args.length; j++) {
				if (args[j].equals(opt))
					return true;
			}
			return false;
		}


		public boolean nextEq(Collection<String> eqs) {
			for (String eq : eqs) {
				if (nextEq(eq))
					return true;
			}
			return false;
		}
		public String nextOpt() {
			String opt = args[i];
			i++;
			return opt;
		}
		public String peekArg() {
			if (hasArgs())
				return args[i];
			return null;
		}
		public boolean nextArgIsHelp() {
			if (hasArgs() && (args[i].equals("-h")
					|| args[i].equals("--help"))) {
				nextOpt();
				return true;
			}
			return false;
		}

		public void fail() {
			System.err.println("cannot process: "+args[i]);
			System.exit(1);

		}

		public void info(String params, String desc) {
			if (this.nextArgIsHelp()) {
				System.out.println(args[i-2]+" "+params+"\t   "+desc);
				System.exit(0);
			}
		}
	}

	public class OptionException extends Exception {
		public OptionException(String msg) {
			super(msg);
		}

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

	public void run(String[] args) throws OWLOntologyCreationException, IOException, FrameMergeException, SimilarityAlgorithmException, OWLOntologyStorageException, OptionException, URISyntaxException {
		Opts opts = new Opts(args);
		run(opts);
	}

	public void run(Opts opts) throws OWLOntologyCreationException, IOException, FrameMergeException, SimilarityAlgorithmException, OWLOntologyStorageException, OptionException, URISyntaxException {

		List<String> paths = new ArrayList<String>();

		String reasonerClassName = "com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory";
		OWLReasoner reasoner = null;
		String reasonerName = "pellet";
		boolean createNamedRestrictions = false;
		boolean createDefaultInstances = false;
		boolean merge = false;
		OWLOntology simOnt = null;
		Set<OWLSubClassOfAxiom> removedSubClassOfAxioms = null;
		OWLPrettyPrinter owlpp;

		//Configuration config = new PropertiesConfiguration("owltools.properties");


		String similarityAlgorithmName = "JaccardSimilarity";


		ParserWrapper pw = new ParserWrapper();


		while (opts.hasArgs()) {

			if (opts.nextArgIsHelp()) {
				help();
				opts.helpMode = true;
			}

			//String opt = opts.nextOpt();
			//System.out.println("processing arg: "+opt);
			if (opts.nextEq("--pellet")) {
				reasonerClassName = "com.clarkparsia.pellet.owlapiv3.Reasoner";
				reasonerName = "pellet";
			}
			else if (opts.nextEq("--hermit")) {
				reasonerClassName = "org.semanticweb.HermiT.Reasoner";
				reasonerName = "hermit";
			}
			else if (opts.nextEq("--reasoner")) {
				reasonerName = opts.nextOpt();
				g.setReasoner(createReasoner(g.getSourceOntology(),reasonerName,g.getManager()));
			}
			else if (opts.nextEq("--no-reasoner")) {
				reasonerClassName = "";
				reasonerName = "";
			}
			else if (opts.nextEq("-r") || opts.nextEq("--namerestr")) {
				createNamedRestrictions = true;
			}
			else if (opts.nextEq("-i") || opts.nextEq("--inst")) {
				createDefaultInstances = true;
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
			else if (opts.nextEq("--map-iri")) {
				//OWLOntologyIRIMapper iriMapper = new SimpleIRIMapper();

			}
			else if (opts.nextEq("--auto-iri")) {
				File file = new File(opts.nextOpt());
				OWLOntologyIRIMapper iriMapper = new AutoIRIMapper(file, false);
				pw.getManager().addIRIMapper(iriMapper);
			}
			else if (opts.nextEq("--remove-imports-declarations")) {
				OWLOntology ont = g.getManager().createOntology(g.getSourceOntology().getOntologyID().getOntologyIRI());
				for (OWLAxiom a : g.getSourceOntology().getAxioms()) {
					g.getManager().addAxiom(ont, a);
				}
				g.setSourceOntology(ont);
			}
			else if (opts.nextEq("--create-ontology")) {
				String iri = opts.nextOpt();
				if (!iri.startsWith("http:")) {
					iri = "http://purl.obolibrary.org/obo/"+iri;
				}
				g = new OWLGraphWrapper(iri);
			}
			else if (opts.nextEq("--merge-support-ontologies")) {
				for (OWLOntology ont : g.getSupportOntologySet())
					g.mergeOntology(ont);
				g.setSupportOntologySet(new HashSet<OWLOntology>());
			}
			else if (opts.nextEq("--add-support-from-imports")) {
				g.addSupportOntologiesFromImportsClosure();
			}
			else if (opts.nextEq("-m") || opts.nextEq("--mcat")) {
				catOntologies(opts);
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
			else if (opts.nextEq("--make-taxon-set")) {
				String idspace = null;
				if (opts.nextEq("-s"))
					idspace = opts.nextOpt();
				owlpp = new OWLPrettyPrinter(g);
				OWLClass tax = (OWLClass)this.resolveEntity(opts);
				Set<OWLObject> taxAncs = g.getAncestorsReflexive(tax);

				Set<OWLClass> taxSet = new HashSet<OWLClass>();
				for (OWLClass c : g.getSourceOntology().getClassesInSignature()) {
					String cid = g.getIdentifier(c);
					if (idspace != null && !cid.startsWith(idspace+":"))
						continue;
					boolean isExcluded = false;
					for (OWLGraphEdge e : g.getOutgoingEdgesClosure(c)) {
						if (isExcluded)
							break;
						for (OWLGraphEdge te : g.getOutgoingEdges(e.getTarget())) {
							OWLObjectProperty tp = te.getSingleQuantifiedProperty().getProperty();
							if (tp != null) {
								String tpl = g.getLabel(tp);
								if (tpl == null)
									continue;
								OWLObject rt = te.getTarget();
								// temp hack until RO is stable
								if (tpl.equals("only_in_taxon") || tpl.equals("only in taxon")) {
									if (!taxAncs.contains(rt) &&
											!g.getAncestors(rt).contains(tax)) {
										isExcluded = true;
										break;
									}
								}
								else if (tpl.equals("never_in_taxon") || tpl.equals("never in taxon")) {
									if (taxAncs.contains(rt)) {
										isExcluded = true;
										break;
									}
								}
							}
						}
					}
					if (isExcluded) {
						LOG.info("excluding: "+owlpp.render(c));
					}
					else {
						taxSet.add(c);
						System.out.println(cid);
					}
				}
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
							System.out.print(result);
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
				opts.info("[-r reasonername] [-m] CLASS-EXPRESSION", 
				"Queries current ontology for descendants of CE using reasoner");
				boolean isManifest = false;
				while (opts.hasOpts()) {
					if (opts.nextEq("-r")) {
						reasonerName = opts.nextOpt();
						if (reasonerName.toLowerCase().equals("elk"))
							isManifest = true;
					}
					else if (opts.nextEq("-m")) {
						opts.info("", 
						"manifests the class exression as a class equivalent to query CE and uses this as a query; required for Elk");
						isManifest = true;
					}
					else {
						break;
					}
				}
				String expression = opts.nextOpt();
				owlpp = new OWLPrettyPrinter(g);
				OWLEntityChecker entityChecker;
				entityChecker = new ShortFormEntityChecker(
						new BidirectionalShortFormProviderAdapter(
								g.getManager(),
								Collections.singleton(g.getSourceOntology()),
								new SimpleShortFormProvider()));
				ManchesterOWLSyntaxEditorParser parser = 
					new ManchesterOWLSyntaxEditorParser(g.getDataFactory(), expression);

				parser.setOWLEntityChecker(entityChecker);	
				try {
					OWLClassExpression ce = parser.parseClassExpression();
					System.out.println("# QUERY: "+owlpp.render(ce));
					if (isManifest) {
						OWLClass qc = g.getDataFactory().getOWLClass(IRI.create("http://owltools.org/Q"));
						OWLEquivalentClassesAxiom ax = g.getDataFactory().getOWLEquivalentClassesAxiom(ce, qc);
						g.getManager().addAxiom(g.getSourceOntology(), ax);
						ce = qc;
					}
					if (reasoner == null) {
						reasoner = createReasoner(g.getSourceOntology(),reasonerName,g.getManager());
					}
					for (OWLClass r : reasoner.getSubClasses(ce, false).getFlattened()) {
						System.out.println(owlpp.render(r));
					}


				} catch (ParserException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
			else if (opts.nextEq("--reasoner-ask-all")) {
				opts.info("", "list all inferred equivalent named class pairs");
				if (reasoner == null)
					reasoner = createReasoner(g.getSourceOntology(),reasonerName,g.getManager());
				String q = opts.nextOpt().toLowerCase();
				owlpp = new OWLPrettyPrinter(g);
				for (OWLClass c : g.getSourceOntology().getClassesInSignature()) {
					if (q.startsWith("e")) {
						for (OWLClass ec : reasoner.getEquivalentClasses(c)) {
							System.out.println(owlpp.render(c)+"\t"+owlpp.render(ec));
						}
					}
					else if (q.startsWith("s")) {
						for (OWLClass ec : reasoner.getSuperClasses(c, true).getFlattened()) {
							System.out.println(owlpp.render(c)+"\t"+owlpp.render(ec));
						}
					}
				}
			}
			else if (opts.nextEq("--run-reasoner")) {
				opts.info("[-r reasonername] [--assert-implied]", "infer new relationships");
				boolean isAssertImplied = false;
				boolean isDirect = true;

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
					else {
						break;
					}
				}
				owlpp = new OWLPrettyPrinter(g);

				boolean isQueryProcessed = false;
				reasoner = createReasoner(g.getSourceOntology(),reasonerName,g.getManager());
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
								for (Node<OWLClass> isup : reasoner.getSuperClasses(a.getSubClass(),true)) {
									if (isup.getEntities().contains(sup)) {
										has = true;
										break;
									}
								}
								System.out.print(has ? "POSITIVE: " : "NEGATIVE: ");
								System.out.println(a);
							}
						}
					}
					System.out.println("all inferences");
					System.out.println("Consistent? "+reasoner.isConsistent());
					for (OWLObject obj : g.getAllOWLObjects()) {
						if (obj instanceof OWLClass) {
							//System.out.println(obj+ " #subclasses:"+
							//		reasoner.getSubClasses((OWLClassExpression) obj, false).getFlattened().size());
							for (Node<OWLClass> sup : reasoner.getSuperClasses((OWLClassExpression) obj, isDirect)) {
								System.out.println(obj+" SubClassOf "+sup);
								if (isAssertImplied) {
									OWLSubClassOfAxiom sca = g.getDataFactory().getOWLSubClassOfAxiom((OWLClass)obj, sup.getRepresentativeElement());
									g.getManager().addAxiom(g.getSourceOntology(), sca);
								}
							}
							Node<OWLClass> ecs = reasoner.getEquivalentClasses(((OWLClassExpression) obj));
							System.out.println(obj+" EquivalentTo "+ecs);


						}
					}
				}


			}
			else if (opts.nextEq("--stash-subclasses")) {
				opts.info("", "removes all subclasses in current source ontology; after reasoning, try to re-infer these");
				removedSubClassOfAxioms = new HashSet<OWLSubClassOfAxiom>();
				System.out.println("Stashing "+removedSubClassOfAxioms.size()+" SubClassOf axioms");
				HashSet<RemoveAxiom> rmaxs = new HashSet<RemoveAxiom>();
				for (OWLSubClassOfAxiom a : g.getSourceOntology().getAxioms(AxiomType.SUBCLASS_OF)) {
					RemoveAxiom rmax = new RemoveAxiom(g.getSourceOntology(),a);
					rmaxs.add(rmax);
					removedSubClassOfAxioms.add(g.getDataFactory().getOWLSubClassOfAxiom(a.getSubClass(), a.getSuperClass()));
				}
				for (RemoveAxiom rmax : rmaxs) {
					g.getManager().applyChange(rmax);
				}
			}
			else if (opts.nextEq("--list-cycles")) {
				for (OWLObject x : g.getAllOWLObjects()) {
					for (OWLObject y : g.getAncestors(x)) {
						if (g.getAncestors(y).contains(x)) {
							System.out.println(x + " in-cycle-with "+y);
						}
					}
				}
			}
			else if (opts.nextEq("-a|--ancestors")) {
				opts.info("LABEL", "list edges in graph closure to root nodes");
				//System.out.println("i= "+i);
				OWLObject obj = resolveEntity(opts);
				System.out.println(obj+ " "+obj.getClass());
				Set<OWLGraphEdge> edges = g.getOutgoingEdgesClosureReflexive(obj);
				showEdges(edges);
			}
			else if (opts.nextEq("--ancestors-with-ic")) {
				opts.info("LABEL [-p COMPARISON_PROPERTY_URI]", "list edges in graph closure to root nodes, with the IC of the target node");
				SimEngine se = new SimEngine(g);
				if (opts.nextEq("-p")) {
					se.comparisonProperty =  g.getOWLObjectProperty(opts.nextOpt());
				}

				//System.out.println("i= "+i);
				OWLObject obj = resolveEntity(opts);
				System.out.println(obj+ " "+obj.getClass());
				Set<OWLGraphEdge> edges = g.getOutgoingEdgesClosureReflexive(obj);

				for (OWLGraphEdge e : edges) {
					System.out.println(e);
					System.out.println("  TARGET IC:"+se.getInformationContent(e.getTarget()));
				}
			}
			else if (opts.nextEq("--get-ic")) {
				opts.info("LABEL [-p COMPARISON_PROPERTY_URI]", "calculate information content for class");
				SimEngine se = new SimEngine(g);
				if (opts.nextEq("-p")) {
					se.comparisonProperty =  g.getOWLObjectProperty(opts.nextOpt());
				}

				//System.out.println("i= "+i);
				OWLObject obj = resolveEntity( opts);
				System.out.println(obj+ " "+" // IC:"+se.getInformationContent(obj));

			}
			else if (opts.nextEq("--ancestor-nodes")) {
				opts.info("LABEL", "list nodes in graph closure to root nodes");
				//System.out.println("i= "+i);
				OWLObject obj = resolveEntity( opts);
				System.out.println(obj+ " "+obj.getClass());
				for (OWLObject a : g.getAncestors(obj)) 
					System.out.println(a);
			}
			else if (opts.nextEq("--parents-named")) {
				opts.info("LABEL", "list direct outgoing edges to named classes");
				//System.out.println("i= "+i);
				OWLObject obj = resolveEntity( opts);
				System.out.println(obj+ " "+obj.getClass());
				Set<OWLGraphEdge> edges = g.getOutgoingEdges(obj);
				showEdges( edges);
			}
			else if (opts.nextEq("--parents")) {
				opts.info("LABEL", "list direct outgoing edges");
				//System.out.println("i= "+i);
				OWLObject obj = resolveEntity( opts);
				System.out.println(obj+ " "+obj.getClass());
				Set<OWLGraphEdge> edges = g.getPrimitiveOutgoingEdges(obj);
				showEdges( edges);
			}
			else if (opts.nextEq("--grandparents")) {
				opts.info("LABEL", "list direct outgoing edges and their direct outgoing edges");
				//System.out.println("i= "+i);
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
				//System.out.println("i= "+i);
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
				//System.out.println("i= "+i);
				OWLObject obj = resolveEntity( opts);
				System.out.println(obj+ " "+obj.getClass());
				Set<OWLObject> ds = g.getDescendants(obj);
				for (OWLObject d : ds)
					System.out.println(d);
			}
			else if (opts.nextEq("--subsumed-by")) {
				opts.info("LABEL", "show all descendant nodes");
				//System.out.println("i= "+i);
				OWLObject obj = resolveEntity( opts);
				System.out.println(obj+ " "+obj.getClass());
				Set<OWLObject> ds = g.queryDescendants((OWLClass)obj);
				for (OWLObject d : ds)
					System.out.println(d);
			}
			else if (opts.nextEq("--lcsx")) {
				opts.info("LABEL", "anonymous class expression 1");
				OWLObject a = resolveEntity( opts);

				opts.info("LABEL", "anonymous class expression 2");
				OWLObject b = resolveEntity( opts);
				System.out.println(a+ " // "+a.getClass());
				System.out.println(b+ " // "+b.getClass());

				SimEngine se = new SimEngine(g);
				OWLClassExpression lcs = se.getLeastCommonSubsumerSimpleClassExpression(a, b);

				System.out.println("LCS:"+lcs);
			}
			else if (opts.nextEq("--lcsx-all")) {
				opts.info("LABEL", "ont 1");
				String ont1 = opts.nextOpt();

				opts.info("LABEL", "ont 2");
				String ont2 = opts.nextOpt();

				if (simOnt == null) {
					simOnt = g.getManager().createOntology();
				}

				SimEngine se = new SimEngine(g);

				Set <OWLObject> objs1 = new HashSet<OWLObject>();
				Set <OWLObject> objs2 = new HashSet<OWLObject>();

				System.out.println(ont1+" -vs- "+ont2);
				for (OWLObject x : g.getAllOWLObjects()) {
					if (! (x instanceof OWLClass))
						continue;
					String id = g.getIdentifier(x);
					if (id.startsWith(ont1)) {
						objs1.add(x);
					}
					if (id.startsWith(ont2)) {
						objs2.add(x);
					}
				}
				Set lcsh = new HashSet<OWLClassExpression>();
				owlpp = new OWLPrettyPrinter(g);
				owlpp.hideIds();
				for (OWLObject a : objs1) {
					for (OWLObject b : objs2) {
						OWLClassExpression lcs = se.getLeastCommonSubsumerSimpleClassExpression(a, b);
						if (lcs instanceof OWLAnonymousClassExpression) {
							if (lcsh.contains(lcs))
								continue;
							lcsh.add(lcs);
							String label = owlpp.render(lcs);
							IRI iri = IRI.create("http://purl.obolibrary.org/obo/U_"+
									g.getIdentifier(a).replaceAll(":", "_")+"_" 
									+"_"+g.getIdentifier(b).replaceAll(":", "_"));
							OWLClass namedClass = g.getDataFactory().getOWLClass(iri);
							// TODO - use java obol to generate meaningful names
							OWLEquivalentClassesAxiom ax = g.getDataFactory().getOWLEquivalentClassesAxiom(namedClass , lcs);
							g.getManager().addAxiom(simOnt, ax);
							g.getManager().addAxiom(simOnt,
									g.getDataFactory().getOWLAnnotationAssertionAxiom(
											g.getDataFactory().getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL.getIRI()),
											iri,
											g.getDataFactory().getOWLLiteral(label)));
							LOG.info("LCSX:"+owlpp.render(a)+" -vs- "+owlpp.render(b)+" = "+label);
							//LOG.info("  Adding:"+owlpp.render(ax));
							LOG.info("  Adding:"+ax);

						}
					}					
				}


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
					else {
						break;
					}
				}
				//System.out.println("i= "+i);
				OWLObject obj = resolveEntity( opts);
				System.out.println(obj);
				OWLGraphLayoutRenderer r = new OWLGraphLayoutRenderer(g);

				r.addObject(obj);
				r.renderImage(fmt, new FileOutputStream(imgf));
				//Set<OWLGraphEdge> edges = g.getOutgoingEdgesClosureReflexive(obj);
				//showEdges( edges);
			}
			else if (opts.nextEq("--draw-all")) {
				opts.info("", "draws ALL objects in the ontology (caution: small ontologies only)");
				//System.out.println("i= "+i);
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
			else if (opts.nextEq("--all-class-ic")) {
				opts.info("", "show calculated Information Content for all classes");
				SimEngine se = new SimEngine(g);
				Similarity sa = se.getSimilarityAlgorithm(similarityAlgorithmName);
				//  no point in caching, as we only check descendants of each object once
				g.getConfig().isCacheClosure = false;
				for (OWLObject obj : g.getAllOWLObjects()) {
					if (se.hasInformationContent(obj)) {
						System.out.println(obj+"\t"+se.getInformationContent(obj));
					}
				}
			}
			else if (opts.nextEq("--sim-method")) {
				opts.info("metric", "sets deafult similarity metric. Type --all to show all TODO");
				similarityAlgorithmName = opts.nextOpt();
			}
			else if (opts.nextEq("--sim-all")) {
				opts.info("", "calculates similarity between all pairs");
				Double minScore = null;
				SimEngine se = new SimEngine(g);
				if (opts.hasOpts()) {
					if (opts.nextEq("-m|--min")) {
						minScore = Double.valueOf(opts.nextOpt());
					}
					else if (opts.nextEq("-s|--subclass-of")) {
						se.comparisonSuperclass = resolveEntity(opts);
					}
				}
				//Similarity metric = se.getSimilarityAlgorithm(similarityAlgorithmName);
				//SimilarityAlgorithm metric = se.new JaccardSimilarity();
				se.calculateSimilarityAllByAll(similarityAlgorithmName, minScore);
				//System.out.println(metric.getClass().getName());
			}
			else if (opts.nextEq("--sim")) {
				Reporter reporter = new Reporter(g);
				opts.info("[-m metric] A B", "calculates similarity between A and B");
				boolean nr = false;
				Vector<OWLObjectPair> pairs = new Vector<OWLObjectPair>();
				String subSimMethod = null;

				boolean isAll = false;
				SimEngine se = new SimEngine(g);
				while (opts.hasOpts()) {
					System.out.println("sub-opts for --sim");
					if (opts.nextEq("-m")) {
						similarityAlgorithmName = opts.nextOpt();
					}
					else if (opts.nextEq("-p")) {
						se.comparisonProperty =  g.getOWLObjectProperty(opts.nextOpt());
					}
					else if (opts.nextEq("--min-ic")) {
						se.minimumIC = Double.valueOf(opts.nextOpt());
					}
					else if (opts.nextEq("--sub-method")) {
						opts.info("MethodName","sets the method used to compare all attributes in a MultiSim test");
						subSimMethod = opts.nextOpt();
					}
					else if (opts.nextEq("--query")) {
						OWLObject q = resolveEntity(opts.nextOpt());
						SimSearch search = new SimSearch(se, reporter);

						isAll = true;
						boolean isClasses = true;
						boolean isInstances = true;
						int MAX_PAIRS = 50; // todo - make configurable
						while (opts.hasOpts()) {
							if (opts.nextEq("-i"))
								isClasses = false;
							else if (opts.nextEq("-c"))
								isInstances = false;
							else if (opts.nextEq("--max-hits"))
								MAX_PAIRS = Integer.parseInt(opts.nextOpt());
							else
								break;
						}
						search.setMaxHits(MAX_PAIRS);
						OWLObject cc = resolveEntity(opts.nextOpt());
						Set<OWLObject> candidates = g.queryDescendants((OWLClass)cc, isInstances, isClasses);
						candidates.remove(cc);
						search.setCandidates(candidates);
						System.out.println("  numCandidates:"+candidates.size());

						List<OWLObject> hits = search.search(q);
						System.out.println("  hits:"+hits.size());
						int n = 0;
						for (OWLObject hit : hits) {
							if (n < MAX_PAIRS)
								pairs.add(new OWLObjectPair(q,hit));
							n++;
							System.out.println("HIT:"+n+"\t"+g.getLabelOrDisplayId(hit));
						}
						while (opts.nextEq("--include")) {
							OWLObjectPair pair = new OWLObjectPair(q,resolveEntity(opts.nextOpt()));

							if (!pairs.contains(pair)) {
								pairs.add(pair);
								System.out.println("adding_extra_pair:"+pair);
							}
							else {
								System.out.println("extra_pair_alrwady_added:"+pair);
							}
						}
					}
					else if (opts.nextEq("-a|--all")) {
						isAll = true;
						boolean isClasses = true;
						boolean isInstances = true;
						if (opts.nextEq("-i"))
							isClasses = false;
						if (opts.nextEq("-c"))
							isInstances = false;
						OWLObject anc = resolveEntity(opts.nextOpt());
						System.out.println("Set1:"+anc+" "+anc.getClass());
						Set<OWLObject> objs = g.queryDescendants((OWLClass)anc, isInstances, isClasses);
						objs.remove(anc);
						System.out.println("  Size1:"+objs.size());
						Set<OWLObject> objs2 = objs;
						if (opts.nextEq("--vs")) {
							OWLObject anc2 = resolveEntity(opts.nextOpt());
							System.out.println("Set2:"+anc2+" "+anc2.getClass());
							objs2 = g.queryDescendants((OWLClass)anc2, isInstances, isClasses);
							objs2.remove(anc2);
							System.out.println("  Size2:"+objs2.size());
						}
						for (OWLObject a : objs) {
							if (!(a instanceof OWLNamedObject)) {
								continue;
							}
							for (OWLObject b : objs2) {
								if (!(b instanceof OWLNamedObject)) {
									continue;
								}
								if (a.equals(b))
									continue;
								//if (a.compareTo(b) <= 0)
								//	continue;
								OWLObjectPair pair = new OWLObjectPair(a,b);
								System.out.println("Scheduling:"+pair);
								pairs.add(pair);
							}							
						}

					}
					else if (opts.nextEq("-s|--subclass-of")) {
						se.comparisonSuperclass = resolveEntity(opts);
					}
					else if (opts.nextEq("--no-create-reflexive")) {
						nr = true;
					}
					else {
						// not recognized - end of this block of opts
						break;
						//System.err.println("???"+opts.nextOpt());
					}
				}
				if (isAll) {
					// TODO
					//se.calculateSimilarityAllByAll(similarityAlgorithmName, 0.0);
				}
				else {
					pairs.add(new OWLObjectPair(resolveEntity(opts.nextOpt()),
							resolveEntity(opts.nextOpt())));

				}
				for (OWLObjectPair pair : pairs) {

					OWLObject oa = pair.getA();
					OWLObject ob = pair.getB();

					Similarity metric = se.getSimilarityAlgorithm(similarityAlgorithmName);
					if (nr) {
						((DescriptionTreeSimilarity)metric).forceReflexivePropertyCreation = false;
					}
					if (subSimMethod != null)
						((MultiSimilarity)metric).setSubSimMethod(subSimMethod);

					System.out.println("comparing: "+oa+" vs "+ob);
					Similarity r = se.calculateSimilarity(metric, oa, ob);
					//System.out.println(metric+" = "+r);
					metric.print();
					metric.report(reporter);
					if (simOnt == null) {
						simOnt = g.getManager().createOntology();
					}
					if (opts.hasOpt("--save-sim")) {
						metric.addResultsToOWLOntology(simOnt);
					}
				}
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
					if (ofmtname.equals("functional")) {
						ofmt = new OWLFunctionalSyntaxOntologyFormat();
					}

				}

				pw.saveOWL(g.getSourceOntology(), ofmt, opts.nextOpt());
				//pw.saveOWL(g.getSourceOntology(), opts.nextOpt());
			}
			else if (opts.nextEq("--save-sim")) {
				opts.info("FILE", "saves similarity results as an OWL ontology. Use after --sim or --sim-all");
				pw.saveOWL(simOnt, opts.nextOpt());
			}
			else if (opts.nextEq("--merge-sim")) {
				opts.info("FILE", "merges similarity results into source OWL ontology. Use after --sim or --sim-all");
				g.mergeOntology(simOnt);
			}
			else if (opts.nextEq("--list-axioms")) {
				for (OWLAxiom a : g.getSourceOntology().getAxioms()) {
					System.out.println("AX:"+a);
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
						if (!pfx.startsWith("http:"))
							pfx = "http://purl.obolibrary.org/obo/" + pfx + "_";
						ttac.config.iriPrefixMap.put(col, pfx);
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
			else if (opts.nextEq("--gaf")) {
				GafObjectsBuilder builder = new GafObjectsBuilder();
				gafdoc = builder.buildDocument(opts.nextOpt());				
				//gafdoc = builder.buildDocument(new File(opts.nextOpt()));				
			}
			else if (opts.nextEq("--gaf-xp-predict")) {
				owlpp = new OWLPrettyPrinter(g);
				if (gafdoc == null) {
					System.err.println("No gaf document (use '--gaf GAF-FILE') ");
					System.exit(1);
				}
				AnnotationPredictor ap = new CompositionalClassPredictor(gafdoc, g);
				Set<Prediction> predictions = ap.getAllPredictions();
				System.out.println("Predictions:"+predictions.size());
				for (Prediction p : predictions) {
					System.out.println(p.render(owlpp));
				}
			}
			else if (opts.nextEq("--gaf-term-counts")) {
				// TODO - ensure has_part and other relations are excluded
				owlpp = new OWLPrettyPrinter(g);
				Map<OWLObject,Set<String>> aMap = new HashMap<OWLObject,Set<String>>();
				for (GeneAnnotation a : gafdoc.getGeneAnnotations()) {
					OWLObject c = g.getOWLObjectByIdentifier(a.getCls());
					for (OWLObject x : g.getAncestorsReflexive(c)) {
						if (!aMap.containsKey(x))
							aMap.put(x, new HashSet<String>());
						aMap.get(x).add(a.getBioentity());
					}
				}
				for (OWLObject c : g.getAllOWLObjects()) {
					if (c instanceof OWLClass) {
						if (g.isObsolete(c))
							continue;
						System.out.println(g.getIdentifier(c)+"\t"+g.getLabel(c)+"\t"+
								(aMap.containsKey(c) ? aMap.get(c).size() : "0"));
					}
				}
			}
			else if (opts.nextEq("--gaf-query")) {
				opts.info("LABEL", "list edges in graph closure to root nodes");
				//System.out.println("i= "+i);
				OWLObject obj = resolveEntity(opts);
				Set<OWLObject> descs = g.getDescendantsReflexive(obj);
				for (GeneAnnotation a : gafdoc.getGeneAnnotations()) {
					OWLObject c = g.getOWLObjectByIdentifier(a.getCls());
					if (descs.contains(c)) {
						System.out.println(g.getIdentifier(c)+"\t"+a.getBioentityObject()+"\t"+a.getBioentityObject().getSymbol());
					}
				}
			}
			else if (opts.nextEq("--report-profile")) {
				g.getProfiler().report();
			}
			else if (opts.nextEq("--no-cache")) {
				g.getConfig().isCacheClosure = false;
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
				String f  = opts.nextOpt();
				try {
					OWLOntology ont = pw.parse(f);
					if (g == null)
						g =	new OWLGraphWrapper(ont);
					else {
						System.out.println("adding support ont "+ont);
						g.addSupportOntology(ont);
					}

				}
				catch (Exception e) {
					System.err.println("could not parse:"+f+" Exception:"+e);
					System.exit(1);
				}


				//paths.add(opt);
			}
			else {
				if (opts.helpMode)
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

	}

	private OWLReasoner createReasoner(OWLOntology ont, String reasonerName, 
			OWLOntologyManager manager) {
		OWLReasonerFactory reasonerFactory = null;
		OWLReasoner reasoner;
		LOG.info("Creating reasoner:"+reasonerName);
		if (reasonerName == null || reasonerName.equals("factpp"))
			reasonerFactory = new FaCTPlusPlusReasonerFactory();
		else if (reasonerName.equals("pellet"))
			reasonerFactory = new PelletReasonerFactory();
		else if (reasonerName.equals("hermit")) {
			//return new org.semanticweb.HermiT.Reasoner.ReasonerFactory().createReasoner(ont);
			reasonerFactory = new org.semanticweb.HermiT.Reasoner.ReasonerFactory();			
		}
		else if (reasonerName.equals("elk")) {
			//SimpleConfiguration rconf = new SimpleConfiguration(FreshEntityPolicy.ALLOW, Long.MAX_VALUE);
			reasonerFactory = new ElkReasonerFactory();	
			//reasoner = reasonerFactory.createReasoner(ont, rconf);
			reasoner = reasonerFactory.createNonBufferingReasoner(ont);
			System.out.println(reasonerFactory+" "+reasoner+" // "+InferenceType.values());
			reasoner.precomputeInferences(InferenceType.values());
			return reasoner;
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



	// todo - move to util
	public OWLObject resolveEntity(Opts opts) {
		OWLObject obj = null;
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
