package owltools.cli;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.obolibrary.oboformat.model.FrameMergeException;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyIRIMapper;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.RemoveAxiom;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.util.AutoIRIMapper;
import org.semanticweb.owlapi.util.SimpleIRIMapper;

import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;

import owltools.gfx.OWLGraphLayoutRenderer;
import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;
import owltools.graph.OWLQuantifiedProperty;
import owltools.graph.OWLQuantifiedProperty.Quantifier;
import owltools.io.GraphClosureWriter;
import owltools.io.OWLPrettyPrinter;
import owltools.io.ParserWrapper;
import owltools.io.TableToAxiomConverter;
import owltools.mooncat.Mooncat;
import owltools.sim.DescriptionTreeSimilarity;
import owltools.sim.MultiSimilarity;
import owltools.sim.OWLObjectPair;
import owltools.sim.Reporter;
import owltools.sim.SimEngine;
import owltools.sim.JaccardSimilarity;
import owltools.sim.SimEngine.SimilarityAlgorithmException;
import owltools.sim.SimSearch;
import owltools.sim.Similarity;
import uk.ac.manchester.cs.factplusplus.owlapiv3.FaCTPlusPlusReasonerFactory;

public class CommandLineInterface {

	private static Logger LOG = Logger.getLogger(CommandLineInterface.class);


	private static class Opts {
		int i = 0;
		String[] args;
		boolean helpMode = false;

		public Opts(String[] args) {
			super();
			this.i = 0;
			this.args = args;
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
			// TODO Auto-generated method stub

		}

		public void info(String params, String desc) {
			if (this.nextArgIsHelp()) {
				System.out.println(args[i-1]+" "+params+"\t   "+desc);
				System.exit(0);
			}
		}

	}

	public static void main(String[] args) throws OWLOntologyCreationException, IOException, FrameMergeException, SimilarityAlgorithmException, OWLOntologyStorageException {

		List<String> paths = new ArrayList<String>();
		//Integer i=0;
		// REDUNDANT: see new method
		//String reasonerClassName = "uk.ac.manchester.cs.factplusplus.owlapiv3.Reasoner";
		String reasonerClassName = "com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory";
		String reasonerName = "pellet";
		boolean createNamedRestrictions = false;
		boolean createDefaultInstances = false;
		boolean merge = false;
		OWLOntology simOnt = null;
		Set<OWLSubClassOfAxiom> removedSubClassOfAxioms = null;
		OWLPrettyPrinter owlpp;

		//Configuration config = new PropertiesConfiguration("owltools.properties");


		String similarityAlgorithmName = "JaccardSimilarity";


		OWLGraphWrapper g = null;
		ParserWrapper pw = new ParserWrapper();

		Opts opts = new Opts(args);

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
			else if (opts.nextEq("--monitor-memory")) {
				g.getConfig().isMonitorMemory = true;
			}
			else if (opts.nextEq("--list-classes")) {
				Set<OWLClass> clss = g.getSourceOntology().getClassesInSignature();
				for (OWLClass c : clss) {
					System.out.println(c);
				}
			}
			else if (opts.nextEq("--merge")) {
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
			else if (opts.nextEq("--merge-support-ontologies")) {
				for (OWLOntology ont : g.getSupportOntologySet())
					g.mergeOntology(ont);
				g.setSupportOntologySet(new HashSet<OWLOntology>());
			}
			else if (opts.nextEq("--add-support-from-imports")) {
				g.addSupportOntologiesFromImportsClosure();
			}
			else if (opts.nextEq("-m") || opts.nextEq("--mcat")) {
				catOntologies(g,opts);
			}
			else if (opts.nextEq("--info")) {
				opts.info("","show ontology statistics");
				for (OWLOntology ont : g.getAllOntologies()) {
					summarizeOntology(ont);
				}
			}
			else if (opts.nextEq("--save-closure")) {
				GraphClosureWriter gcw = new GraphClosureWriter(opts.nextOpt());
				gcw.saveClosure(g);				
			}
			else if (opts.nextEq("--serialize-closure")) {
				GraphClosureWriter gcw = new GraphClosureWriter(opts.nextOpt());
				gcw.serializeClosure(g);
			}
			else if (opts.nextEq("--run-reasoner")) {
				opts.info("[-r reasonername]", "infer new relationships");
				if (opts.hasOpts()) {
					if (opts.nextEq("-r")) {
						reasonerName = opts.nextOpt();
					}
				}
				OWLReasoner reasoner = createReasoner(g.getSourceOntology(),reasonerName,g.getManager());
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
				for (OWLObject obj : g.getAllOWLObjects()) {
					if (obj instanceof OWLClass) {
						for (Node<OWLClass> sup : reasoner.getSuperClasses((OWLClassExpression) obj, true)) {
							System.out.println(obj+" SubClassOf "+sup);
						}
						Node<OWLClass> ecs = reasoner.getEquivalentClasses(((OWLClassExpression) obj));
						System.out.println(obj+" EquivalentTo "+ecs);


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
			else if (opts.nextEq("-a|--ancestors")) {
				opts.info("LABEL", "list edges in graph closure to root nodes");
				//System.out.println("i= "+i);
				OWLObject obj = resolveEntity(g, opts);
				System.out.println(obj+ " "+obj.getClass());
				Set<OWLGraphEdge> edges = g.getOutgoingEdgesClosureReflexive(obj);
				showEdges(edges);
			}
			else if (opts.nextEq("--ancestor-nodes")) {
				opts.info("LABEL", "list nodes in graph closure to root nodes");
				//System.out.println("i= "+i);
				OWLObject obj = resolveEntity(g, opts);
				System.out.println(obj+ " "+obj.getClass());
				for (OWLObject a : g.getAncestors(obj)) 
					System.out.println(a);
			}
			else if (opts.nextEq("--parents-named")) {
				opts.info("LABEL", "list direct outgoing edges to named classes");
				//System.out.println("i= "+i);
				OWLObject obj = resolveEntity(g, opts);
				System.out.println(obj+ " "+obj.getClass());
				Set<OWLGraphEdge> edges = g.getOutgoingEdges(obj);
				showEdges(edges);
			}
			else if (opts.nextEq("--parents")) {
				opts.info("LABEL", "list direct outgoing edges");
				//System.out.println("i= "+i);
				OWLObject obj = resolveEntity(g, opts);
				System.out.println(obj+ " "+obj.getClass());
				Set<OWLGraphEdge> edges = g.getPrimitiveOutgoingEdges(obj);
				showEdges(edges);
			}
			else if (opts.nextEq("--grandparents")) {
				opts.info("LABEL", "list direct outgoing edges and their direct outgoing edges");
				//System.out.println("i= "+i);
				OWLObject obj = resolveEntity(g, opts);
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
				OWLObject obj = resolveEntity(g, opts);
				Set<OWLObject> ancs = g.getSubsumersFromClosure(obj);
				for (OWLObject a : ancs) {
					System.out.println(a);
				}
			}
			else if (opts.nextEq("--incoming-edges")) {
				opts.info("LABEL", "list edges in graph to leaf nodes");
				OWLObject obj = resolveEntity(g, opts);
				System.out.println(obj+ " "+obj.getClass());
				Set<OWLGraphEdge> edges = g.getIncomingEdges(obj);
				showEdges(edges);
			}
			else if (opts.nextEq("--descendant-edges")) {
				opts.info("LABEL", "list edges in graph closure to leaf nodes");
				OWLObject obj = resolveEntity(g, opts);
				System.out.println(obj+ " "+obj.getClass());
				Set<OWLGraphEdge> edges = g.getIncomingEdgesClosure(obj);
				showEdges(edges);
			}
			else if (opts.nextEq("--descendants")) {
				opts.info("LABEL", "show all descendant nodes");
				//System.out.println("i= "+i);
				OWLObject obj = resolveEntity(g, opts);
				System.out.println(obj+ " "+obj.getClass());
				Set<OWLObject> ds = g.getDescendants(obj);
				for (OWLObject d : ds)
					System.out.println(d);
			}
			else if (opts.nextEq("--subsumed-by")) {
				opts.info("LABEL", "show all descendant nodes");
				//System.out.println("i= "+i);
				OWLObject obj = resolveEntity(g, opts);
				System.out.println(obj+ " "+obj.getClass());
				Set<OWLObject> ds = g.queryDescendants((OWLClass)obj);
				for (OWLObject d : ds)
					System.out.println(d);
			}
			else if (opts.nextEq("-d") || opts.nextEq("--draw")) {
				opts.info("LABEL", "generates a file tmp.png made using QuickGO code");
				//System.out.println("i= "+i);
				OWLObject obj = resolveEntity(g, opts);
				System.out.println(obj);
				OWLGraphLayoutRenderer r = new OWLGraphLayoutRenderer(g);

				r.addObject(obj);
				r.renderImage("png", new FileOutputStream("tmp.png"));
				Set<OWLGraphEdge> edges = g.getOutgoingEdgesClosureReflexive(obj);
				showEdges(edges);
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
						se.comparisonSuperclass = resolveEntity(g,opts);
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
						OWLObject q = resolveEntity(g,opts.nextOpt());
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
						OWLObject cc = resolveEntity(g,opts.nextOpt());
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
							OWLObjectPair pair = new OWLObjectPair(q,resolveEntity(g,opts.nextOpt()));

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
						OWLObject anc = resolveEntity(g,opts.nextOpt());
						System.out.println("Set1:"+anc+" "+anc.getClass());
						Set<OWLObject> objs = g.queryDescendants((OWLClass)anc, isInstances, isClasses);
						objs.remove(anc);
						System.out.println("  Size1:"+objs.size());
						Set<OWLObject> objs2 = objs;
						if (opts.nextEq("--vs")) {
							OWLObject anc2 = resolveEntity(g,opts.nextOpt());
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
						se.comparisonSuperclass = resolveEntity(g,opts);
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
					pairs.add(new OWLObjectPair(resolveEntity(g,opts.nextOpt()),
							resolveEntity(g,opts.nextOpt())));

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
				opts.info("FILE", "writes source ontology -- specified as IRI, e.g. file://`pwd`/foo.owl");
				pw.saveOWL(g.getSourceOntology(), opts.nextOpt());
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
				OWLObjectProperty p = (OWLObjectProperty) resolveEntity(g, opts);
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
				OWLClass c = (OWLClass) resolveEntity(g, opts);

				g.getConfig().excludeMetaClass = c;	
			}
			else if (opts.nextEq("--parse-tsv")) {
				opts.info("[-s] FILE", "parses a tabular file to OWL axioms");
				TableToAxiomConverter ttac = new TableToAxiomConverter(g);
				ttac.config.axiomType = AxiomType.CLASS_ASSERTION;
				while (opts.hasOpts()) {
					if (opts.nextEq("-s")) {
						ttac.config.isSwitchSubjectObject = true;
					}
					else if (opts.nextEq("-l|--label")) {
						ttac.config.setPropertyToLabel();
						ttac.config.axiomType = AxiomType.ANNOTATION_ASSERTION;
					}
					else if (opts.nextEq("-p|--prop")) {
						ttac.config.property = ((OWLNamedObject) resolveEntity(g, opts)).getIRI();
					}
					else if (opts.nextEq("-a|--axiom-type")) {
						ttac.config.setAxiomType(opts.nextOpt());
					}
					else if (opts.nextEq("-t|--individuals-type")) {
						System.out.println("setting types");
						ttac.config.individualsType = resolveClass(g, opts.nextOpt());
					}
					else {
						// TODO - other options
					}
				}
				String f = opts.nextOpt();
				System.out.println("tabfile: "+f);
				ttac.parse(f);
			}
			else if (opts.nextEq("--no-cache")) {
				g.getConfig().isCacheClosure = false;
			}
			else if (opts.nextEq("--create-ontology")) {
				opts.info("ONT-IRI", "creates a new OWLOntology and makes it the source ontology");
				g = new OWLGraphWrapper(opts.nextOpt());

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

	private static OWLReasoner createReasoner(OWLOntology ont, String reasonerName, 
			OWLOntologyManager manager) {
		OWLReasonerFactory reasonerFactory = null;
		if (reasonerName == null || reasonerName.equals("factpp"))
			reasonerFactory = new FaCTPlusPlusReasonerFactory();
		else if (reasonerName.equals("pellet"))
			reasonerFactory = new PelletReasonerFactory();
		else if (reasonerName.equals("hermit")) {
			//return new org.semanticweb.HermiT.Reasoner.ReasonerFactory().createReasoner(ont);
			//reasonerFactory = new org.semanticweb.HermiT.Reasoner.ReasonerFactory();			
		}
		else
			System.out.println("no such reasoner: "+reasonerName);

		OWLReasoner reasoner = reasonerFactory.createReasoner(ont);
		return reasoner;
	}

	private static void catOntologies(OWLGraphWrapper g, Opts opts) throws OWLOntologyCreationException, IOException {
		opts.info("[-r|--ref-ont ONT] [-i|--use-imports]", "Catenate ontologies taking only referenced subsets of supporting onts.\n"+
		"        See Mooncat docs");
		Mooncat m = new Mooncat(g);
		ParserWrapper pw = new ParserWrapper();
		if (opts.hasOpts()) {
			//String opt = opts.nextOpt();
			if (opts.nextEq("-r") || opts.nextEq("--ref-ont")) {
				String f = opts.nextOpt();
				m.addReferencedOntology(pw.parseOWL(f));
			}
			else if (opts.nextEq("-s") || opts.nextEq("--src-ont")) {
				m.setOntology(pw.parseOWL(opts.nextOpt()));
			}
			else if (opts.nextEq("-i") || opts.nextEq("--use-imports")) {
				System.out.println("using everything in imports closure");
				g.addSupportOntologiesFromImportsClosure();
			}
			else {
				opts.fail();
			}
		}
		if (m.getReferencedOntologies().size() == 0) {
			m.setReferencedOntologies(g.getSupportOntologySet());
		}
		//g.useImportClosureForQueries();
		for (OWLAxiom ax : m.getClosureAxiomsOfExternalReferencedEntities()) {
			System.out.println("M_AX:"+ax);
		}

		m.mergeOntologies();
	}

	private static void showEdges(Set<OWLGraphEdge> edges) {
		for (OWLGraphEdge e : edges) {
			System.out.println(e);
		}
	}

	public static void summarizeOntology(OWLOntology ont) {
		System.out.println("Ontology:"+ont);
		System.out.println("  Classes:"+ont.getClassesInSignature().size());
		System.out.println("  Individuals:"+ont.getIndividualsInSignature().size());
		System.out.println("  ObjectProperties:"+ont.getObjectPropertiesInSignature().size());
		System.out.println("  AxiomCount:"+ont.getAxiomCount());
	}



	// todo - move to util
	public static OWLObject resolveEntity(OWLGraphWrapper g, Opts opts) {
		OWLObject obj = null;
		String id = opts.nextOpt(); // in future we will allow resolution by name etc
		return resolveEntity(g,id);
	}

	public static OWLObject resolveEntity(OWLGraphWrapper g, String id) {
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

	public static OWLObjectProperty resolveObjectProperty(OWLGraphWrapper g, String id) {
		OWLObject obj = null;
		obj = g.getOWLObjectByLabel(id);
		if (obj != null)
			return (OWLObjectProperty) obj;
		return g.getOWLObjectProperty(id);
	}
	public static OWLClass resolveClass(OWLGraphWrapper g, String id) {
		OWLObject obj = null;
		obj = g.getOWLObjectByLabel(id);
		if (obj != null)
			return (OWLClass) obj;
		return g.getDataFactory().getOWLClass(IRI.create(id));
	}

	public static void help() {
		System.out.println("owltools [ONTOLOGY ...] [COMMAND ...]\n");
		System.out.println("Commands/Options");
		System.out.println("  (type 'owltools COMMAND -h' for more info)");
	}

	public static void helpFooter() {
		System.out.println("\nOntologies:");
		System.out.println("  These are specified as IRIs. The IRI is typically  'file:PATH' or a URL");
		System.out.println("\nLabel Resolution:");
		System.out.println("  you can pass in either a class label (enclosed in single quotes), an OBO ID or a IRI");
		System.out.println("\nExecution:");
		System.out.println("  note that commands are processed *in order*. This allows you to run mini-pipelines" +
		"  or programs on the command line");
		System.out.println("\nExamples:");
		System.out.println("  ");

	}

}
