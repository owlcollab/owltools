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
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.TimeZone;
import java.util.UUID;
import java.util.Vector;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Server;
import org.geneontology.reasoner.ExpressionMaterializingReasoner;
import org.geneontology.reasoner.ExpressionMaterializingReasonerFactory;
import org.geneontology.reasoner.OWLExtendedReasoner;
import org.obolibrary.macro.MacroExpansionVisitor;
import org.obolibrary.macro.ManchesterSyntaxTool;
import org.obolibrary.obo2owl.Obo2OWLConstants;
import org.obolibrary.obo2owl.Obo2OWLConstants.Obo2OWLVocabulary;
import org.obolibrary.obo2owl.OboInOwlCardinalityTools;
import org.obolibrary.obo2owl.Owl2Obo;
import org.obolibrary.oboformat.model.Frame;
import org.obolibrary.oboformat.model.OBODoc;
import org.obolibrary.oboformat.parser.OBOFormatConstants.OboFormatTag;
import org.obolibrary.oboformat.parser.OBOFormatParser;
import org.obolibrary.oboformat.writer.OBOFormatWriter;
import org.obolibrary.oboformat.writer.OBOFormatWriter.NameProvider;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.FunctionalSyntaxDocumentFormat;
import org.semanticweb.owlapi.formats.ManchesterSyntaxDocumentFormat;
import org.semanticweb.owlapi.formats.OBODocumentFormat;
import org.semanticweb.owlapi.formats.OWLXMLDocumentFormat;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.formats.TurtleDocumentFormat;
import org.semanticweb.owlapi.io.OWLParserException;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.AddOntologyAnnotation;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationSubject;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyCharacteristicAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyIRIMapper;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLProperty;
import org.semanticweb.owlapi.model.OWLPropertyExpression;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.OWLSubObjectPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubPropertyChainOfAxiom;
import org.semanticweb.owlapi.model.RemoveAxiom;
import org.semanticweb.owlapi.model.RemoveImport;
import org.semanticweb.owlapi.model.SetOntologyID;
import org.semanticweb.owlapi.model.parameters.AxiomAnnotations;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.util.AutoIRIMapper;
import org.semanticweb.owlapi.util.OWLClassExpressionVisitorAdapter;
import org.semanticweb.owlapi.util.OWLEntityRenamer;
import org.semanticweb.owlapi.util.OWLEntityVisitorAdapter;
import org.semanticweb.owlapi.util.SimpleIRIMapper;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import owltools.InferenceBuilder.OWLClassFilter;
import owltools.RedundantInferences;
import owltools.RedundantInferences.RedundantAxiom;
import owltools.cli.tools.CLIMethod;
import owltools.gfx.GraphicsConfig;
import owltools.gfx.GraphicsConfig.RelationConfig;
import owltools.gfx.OWLGraphLayoutRenderer;
import owltools.graph.AxiomAnnotationTools;
import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;
import owltools.graph.OWLGraphWrapper.ISynonym;
import owltools.graph.OWLQuantifiedProperty;
import owltools.graph.OWLQuantifiedProperty.Quantifier;
import owltools.idmap.IDMapPairWriter;
import owltools.idmap.IDMappingPIRParser;
import owltools.idmap.UniProtIDMapParser;
import owltools.io.CatalogXmlIRIMapper;
import owltools.io.ChadoGraphClosureRenderer;
import owltools.io.CompactGraphClosureReader;
import owltools.io.CompactGraphClosureRenderer;
import owltools.io.EdgeTableRenderer;
import owltools.io.GraphClosureRenderer;
import owltools.io.GraphReader;
import owltools.io.GraphRenderer;
import owltools.io.ImportClosureSlurper;
import owltools.io.InferredParentRenderer;
import owltools.io.OWLJSONFormat;
import owltools.io.OWLPrettyPrinter;
import owltools.io.ParserWrapper;
import owltools.io.ParserWrapper.OWLGraphWrapperNameProvider;
import owltools.io.StanzaToOWLConverter;
import owltools.io.TableRenderer;
import owltools.io.TableToAxiomConverter;
import owltools.mooncat.BridgeExtractor;
import owltools.mooncat.EquivalenceSetMergeUtil;
import owltools.mooncat.Mooncat;
import owltools.mooncat.OWLInAboxTranslator;
import owltools.mooncat.PropertyExtractor;
import owltools.mooncat.PropertyViewOntologyBuilder;
import owltools.mooncat.ProvenanceReasonerWrapper;
import owltools.mooncat.QuerySubsetGenerator;
import owltools.mooncat.SpeciesMergeUtil;
import owltools.mooncat.SpeciesSubsetterUtil;
import owltools.mooncat.ontologymetadata.ImportChainDotWriter;
import owltools.mooncat.ontologymetadata.ImportChainExtractor;
import owltools.mooncat.ontologymetadata.OntologyMetadataMarkdownWriter;
import owltools.ontologyrelease.OboBasicDagCheck;
import owltools.ontologyrelease.OntologyMetadata;
import owltools.reasoner.GCIUtil;
import owltools.reasoner.GraphReasonerFactory;
import owltools.renderer.markdown.MarkdownRenderer;
import owltools.sim2.preprocessor.ABoxUtils;
import owltools.tr.LinkMaker;
import owltools.tr.LinkMaker.LinkMakerResult;
import owltools.tr.LinkMaker.LinkPattern;
import owltools.util.OwlHelper;
import owltools.web.OWLServer;
import uk.ac.manchester.cs.owlapi.modularity.ModuleType;
import uk.ac.manchester.cs.owlapi.modularity.SyntacticLocalityModuleExtractor;

import com.clarkparsia.owlapi.explanation.DefaultExplanationGenerator;
import com.clarkparsia.owlapi.explanation.ExplanationGenerator;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import de.derivo.sparqldlapi.Query;
import de.derivo.sparqldlapi.QueryArgument;
import de.derivo.sparqldlapi.QueryBinding;
import de.derivo.sparqldlapi.QueryEngine;
import de.derivo.sparqldlapi.QueryResult;
import de.derivo.sparqldlapi.exceptions.QueryEngineException;
import de.derivo.sparqldlapi.exceptions.QueryParserException;
import de.derivo.sparqldlapi.types.QueryArgumentType;

/**
 * An instance of this class can execute owltools commands in sequence.
 * 
 * Typically, this class is called from a wrapper within its main() method.
 * 
 * Extend this class to implement additional functions. Use the {@link CLIMethod} 
 * annotation, to designate the relevant methods.
 * 
 * @author cjm
 * @see GafCommandRunner
 * @see JsCommandRunner
 * @see SimCommandRunner
 * @see SolrCommandRunner
 * @see TaxonCommandRunner
 */
public class CommandRunner extends CommandRunnerBase {

	private static Logger LOG = Logger.getLogger(CommandRunner.class);

	public void runSingleIteration(Opts opts) throws Exception {


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
				System.err.println("The Pellet reasoner is no longer supported, use Hermit '--hermit', or ELK '--elk' instead");
				exit(-1);
				return;
			}
			else if (opts.nextEq("--hermit")) {
				reasonerName = "hermit";
			}
			else if (opts.nextEq("--elk")) {
				reasonerName = "elk";
			}
			else if (opts.nextEq("--jfact")) {
				System.err.println("The JFact reasoner is no longer supported, use Hermit '--hermit', or ELK '--elk' instead");
				exit(-1);
				return;
			}
			else if (opts.nextEq("--more")) {
				System.err.println("The MORE reasoner is no longer supported, use Hermit '--hermit', or ELK '--elk' instead");
				exit(-1);
				return;
			}
			else if (opts.nextEq("--use-reasoner|--set-reasoner-name")) {
				reasonerName =  opts.nextOpt();
			}
			else if (opts.nextEq("--no-dispose")) {
				this.isDisposeReasonerOnExit = false;
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
			else if (opts.nextEq("--reasoner-flush")) {
				reasoner.flush();
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
			else if (opts.nextEq("--log-error")) {
				Logger.getRootLogger().setLevel(Level.ERROR);	
			}
			else if (opts.nextEq("--no-debug")) {
				Logger.getRootLogger().setLevel(Level.OFF);
			}
			else if (opts.nextEq("--no-logging")) {
				Logger.getRootLogger().setLevel(Level.OFF);
			}
			else if (opts.nextEq("--silence-elk")) {
				Logger.getLogger("org.semanticweb.elk").setLevel(Level.OFF);
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
				boolean useIds = false;
				while (opts.hasOpts()) {
					if (opts.nextEq("-i")) {
						useIds = true;
					}
					else {
						break;
					}
				}
				for (OWLObject c : objs) {
					if (c instanceof OWLNamedObject) {
						String label = g.getLabel(c);
						String id;
						if (useIds) {
							id = g.getIdentifier(c);
						}
						else {
							id = ((OWLNamedObject)c).getIRI().toString();
						}					
						System.out.println(id+"\t"+label);
					}
				}
			}
			else if (opts.nextEq("--write-all-subclass-relationships")) {
				for (OWLSubClassOfAxiom ax : g.getSourceOntology().getAxioms(AxiomType.SUBCLASS_OF)) {
					OWLClassExpression parent = ax.getSuperClass();
					OWLClassExpression child = ax.getSubClass();
					if (parent.isAnonymous() || child.isAnonymous())
						continue;
					System.out.println(g.getIdentifier(child) +
							"\t" +
							g.getIdentifier(parent));

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
					for (OWLClassExpression ec : OwlHelper.getEquivalentClasses(qc, queryOntology)) {
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
				pw.addIRIMapper(new CatalogXmlIRIMapper("catalog-v001.xml"));
			}
			else if (opts.nextEq("--catalog-xml")) {
				opts.info("CATALOG-FILE", "uses the specified file as a catalog");
				pw.addIRIMapper(new CatalogXmlIRIMapper(opts.nextOpt()));
			}
			else if (opts.nextEq("--map-ontology-iri")) {
				opts.info("OntologyIRI FILEPATH", "maps an ontology IRI to a file in your filesystem");
				OWLOntologyIRIMapper iriMapper = 
						new SimpleIRIMapper(IRI.create(opts.nextOpt()),
								IRI.create(new File(opts.nextOpt())));
				LOG.info("Adding "+iriMapper+" to "+pw.getManager());
				pw.addIRIMapper(iriMapper);
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
				pw.addIRIMapper(iriMapper);
			}
			else if (opts.nextEq("--remove-imports-declarations")) {
				Set<OWLImportsDeclaration> oids = g.getSourceOntology().getImportsDeclarations();
				for (OWLImportsDeclaration oid : oids) {
					RemoveImport ri = new RemoveImport(g.getSourceOntology(), oid);
					g.getManager().applyChange(ri);
				}
			}
			else if (opts.nextEq("--remove-import-declaration")) {
				opts.info("IRI", "Removes a specific import");
				String rmImport = opts.nextOpt();
				Set<OWLImportsDeclaration> oids = g.getSourceOntology().getImportsDeclarations();
				for (OWLImportsDeclaration oid : oids) {
					LOG.info("Testing "+oid.getIRI().toString()+" == "+rmImport);
					if (oid.getIRI().toString().equals(rmImport)) {
						RemoveImport ri = new RemoveImport(g.getSourceOntology(), oid);
						LOG.info(ri);
						g.getManager().applyChange(ri);
					}
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
			else if (opts.nextEq("--set-ontology-id")) {
				opts.info("[-v VERSION-IRI][-a] IRI", "Sets the OWLOntologyID (i.e. IRI and versionIRI)");
				IRI v = null;
				IRI iri = null;
				boolean isAnonymous = false;
				while (opts.hasOpts()) {
					if (opts.nextEq("-v|--version-iri")) {
						v = IRI.create(opts.nextOpt());
					}
					else if (opts.nextEq("-a|--anonymous")) {
						opts.info("", "if specified, do not specify an IRI");
						isAnonymous = true;
					}
					else {
						break;
					}
				}
				if (!isAnonymous)
					iri = IRI.create(opts.nextOpt());
				OWLOntologyID oid = new OWLOntologyID(Optional.fromNullable(iri), Optional.fromNullable(v));
				SetOntologyID soid;
				soid = new SetOntologyID(g.getSourceOntology(), oid);
				g.getManager().applyChange(soid);
			}
			else if (opts.nextEq("--add-ontology-annotation")) {
				opts.info("[-u] PROP VAL", "Sets an ontology annotation");
				OWL2Datatype dt = OWL2Datatype.XSD_STRING;
				while (opts.hasOpts()) {
					if (opts.nextEq("-u")) {
						opts.info("", "Ase xsd:anyURI as datatype");
						dt = OWL2Datatype.XSD_ANY_URI;
					}
					else
						break;
				}
				IRI p = IRI.create(opts.nextOpt());
				OWLLiteral v = g.getDataFactory().getOWLLiteral(opts.nextOpt(), dt);
				OWLAnnotation ann = g.getDataFactory().getOWLAnnotation(g.getDataFactory().getOWLAnnotationProperty(p), v);
				AddOntologyAnnotation addAnn = new AddOntologyAnnotation(g.getSourceOntology(), ann);
				g.getManager().applyChange(addAnn);
			}
			else if (opts.nextEq("--create-ontology")) {
				IRI v = null;
				while (opts.hasOpts()) {
					if (opts.nextEq("-v|--version-iri")) {
						v = IRI.create(opts.nextOpt());
					}
					else {
						break;
					}
				}

				String iri = opts.nextOpt();
				if (!iri.startsWith("http:")) {
					iri = Obo2OWLConstants.DEFAULT_IRI_PREFIX+iri;
				}
				g = new OWLGraphWrapper(iri);

				if (v != null) {
					OWLOntologyID oid = new OWLOntologyID(Optional.of(IRI.create(iri)), Optional.of(v));
					SetOntologyID soid;
					soid = new SetOntologyID(g.getSourceOntology(), oid);
					g.getManager().applyChange(soid);
				}
			}
			else if (opts.nextEq("--merge-import")) {
				opts.info("ONTOLOGY-IRI", "Ensure all axioms from the ontology are merged into the main ontology");
				String iriString = opts.nextOpt();
				g.mergeSpecificImport(IRI.create(iriString));
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
				opts.info("", "This will merge the support ontologies from the OWLGraphWrapper into the main ontology. This is usually required while working with a reasoner.");
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
			else if (opts.nextEq("--remove-entities-marked-imported")) {
				opts.info("","Removes all classes, individuals and object properties that are marked with IAO_0000412");
				Mooncat m = new Mooncat(g);
				m.removeExternalEntities();
			}
			else if (opts.nextEq("--remove-external-classes")) {
				opts.info("IDSPACE","Removes all classes not in the specified ID space");
				boolean removeDangling = true;
				while (opts.hasOpts()) {
					if (opts.nextEq("-k|--keepDangling")) {
						removeDangling = false;
					}
					else {
						break;
					}
				}
				String idspace = opts.nextOpt();
				Mooncat m = new Mooncat(g);
				m.removeClassesNotInIDSpace(idspace, removeDangling);
			}
			else if (opts.nextEq("--remove-dangling")) {
				Mooncat m = new Mooncat(g);
				m.removeDanglingAxioms();
			}
			else if (opts.nextEq("--remove-uninstantiated-classes")) {
				opts.info("", 
						"removes all classes for which the reasoner can infer no instances");
				Set<OWLAxiom> rmAxioms = new HashSet<OWLAxiom>();
				int n = 0;
				reasoner.flush();
				for (OWLClass obj : g.getAllOWLClasses()) {
					if (reasoner.getInstances(obj, false).getFlattened().size() == 0) {
						LOG.info("Unused: "+obj);
						n++;
						rmAxioms.addAll(g.getSourceOntology().getReferencingAxioms(obj, Imports.INCLUDED));
					}
				}
				LOG.info("Removing "+rmAxioms.size()+" referencing "+n+" unused classes");
				g.getManager().removeAxioms(g.getSourceOntology(), rmAxioms);
			}
			else if (opts.nextEq("--make-subset-by-properties")) {
				opts.info("PROPERTY-LIST",
						"make an ontology subset that excludes axioms that use properties not in the specified set.\n"+
								" Note the ontology should be relaxed e.g. X=A and R some B ==> X SubClassOf A" +
								"  A property list is a space-separated list of object property OBO-IDs, shorthands, URIs, or labels.\n"+
								"  Example: my.owl --make-subset-by-properties BFO:0000050 'develops from' // -o my-slim.owl \n"+
								"  The special symbol 'ALL-PROPERTIES' selects all properties in the signature.\n"+								
						"  The property list should be terminated by '//' (this is optional and a new command starting with '-' is sufficient to end the list)");
				boolean isForceRemoveDangling = false;
				boolean isSuppressRemoveDangling = false;
				while (opts.hasOpts()) {
					if (opts.nextEq("-f|--force")) {
						isForceRemoveDangling = true;
					}
					else if (opts.nextEq("-n|--no-remove-dangling")) {
						isSuppressRemoveDangling = true;
					}
					else {
						break;
					}
				}
				Set<OWLObjectProperty> props = this.resolveObjectPropertyList(opts);
				Mooncat m = new Mooncat(g);
				int numDanglingAxioms = m.getDanglingAxioms(g.getSourceOntology()).size();
				LOG.info("# Dangling axioms prior to filtering: "+numDanglingAxioms);
				if (numDanglingAxioms > 0) {
					if (!isForceRemoveDangling && !isSuppressRemoveDangling) {
						LOG.error("Will not proceed until dangling axioms removed, or -n or -f options are set");
						throw new Exception("Dangling axioms will be lost");
					}
				}
				m.retainAxiomsInPropertySubset(g.getSourceOntology(),props,reasoner);
				if (!isSuppressRemoveDangling) {
					LOG.info("# Dangling axioms post-filtering: " + m.getDanglingAxioms(g.getSourceOntology()).size());
					m.removeDanglingAxioms();
				}
			}
			else if (opts.nextEq("--list-class-axioms")) {
				OWLClass c = resolveClass(opts.nextOpt());
				System.out.println("Class = "+c);
				OWLPrettyPrinter owlpp = getPrettyPrinter();
				for (OWLClassAxiom ax : g.getSourceOntology().getAxioms(c, Imports.EXCLUDED)) {
					//System.out.println(ax);
					owlpp.print(ax);
				}
			}
			else if (opts.nextEq("--list-all-axioms")) {
				OWLPrettyPrinter owlpp = getPrettyPrinter();
				for (OWLAxiom a : g.getSourceOntology().getAxioms()) {
					owlpp.print(a);
				}
			}
			else if (opts.nextEq("--make-species-subset")) {
				opts.info("-t TAXCLASS","Creates a taxon-specific ontology");
				OWLObjectProperty viewProperty = null;
				OWLClass taxClass = null;
				String suffix = null;
				SpeciesSubsetterUtil smu = new SpeciesSubsetterUtil(g);
				while (opts.hasOpts()) {
					if (opts.nextEq("-t|--taxon")) {
						taxClass = this.resolveClass(opts.nextOpt());
					}
					else if (opts.nextEq("-p|--property")) {
						viewProperty = this.resolveObjectProperty(opts.nextOpt());
					}
					else if (opts.nextEq("-r|--root")) {
						smu.rootClass = this.resolveClass(opts.nextOpt());
					}
					else
						break;
				}
				smu.viewProperty = viewProperty;
				smu.taxClass = taxClass;
				smu.reasoner = reasoner;
				smu.removeOtherSpecies();
			}
			else if (opts.nextEq("--merge-species-ontology")) {
				opts.info("-t TAXCLASS","Creates a composite/merged species ontology");
				OWLObjectProperty viewProperty = g.getOWLObjectPropertyByIdentifier("BFO:0000050");
				OWLClass taxClass = null;
				String suffix = null;
				Set<OWLObjectProperty> includeProps = null;
				while (opts.hasOpts()) {
					if (opts.nextEq("-t|--taxon")) {
						taxClass = this.resolveClass(opts.nextOpt());
					}
					else if (opts.nextEq("-p|--property")) {
						viewProperty = this.resolveObjectProperty(opts.nextOpt());
					}
					else if (opts.nextEq("-q|--include-property")) {
						includeProps = this.resolveObjectPropertyList(opts);
						LOG.info("|IP|"+includeProps.size());
						LOG.info("IP"+includeProps);
					}
					else if (opts.nextEq("-s|--suffix")) {
						suffix = opts.nextOpt();
					}
					else
						break;
				}
				SpeciesMergeUtil smu = new SpeciesMergeUtil(g);
				smu.viewProperty = viewProperty;
				smu.taxClass = taxClass;
				smu.reasoner = reasoner;
				smu.includedProperties = includeProps;
				if (suffix != null)
					smu.suffix = suffix;
				smu.merge();
			}
			else if (opts.nextEq("--info")) {
				opts.info("","show ontology statistics");
				for (OWLOntology ont : g.getAllOntologies()) {
					summarizeOntology(ont);
				}
			}
			else if (opts.nextEq("--save-superclass-closure")) {
				opts.info("[-r reasoner] FILENAME", "write out superclass closure of graph.");
				GraphRenderer gcw;
				while (opts.hasOpts()) {
					if (opts.nextEq("-r|--reasoner")) {
						opts.info("REASONER", "select reasoner.");
						reasonerName = opts.nextOpt();				
					}
					else {
						break;
					}
				}
				String filename = opts.nextOpt();
				List<String> lines = new ArrayList<String>();
				for (OWLClass c : g.getAllOWLClasses()) {
					Set<OWLClass> ecs = reasoner.getEquivalentClasses(c).getEntities();
					Set<OWLClass> scs = reasoner.getSuperClasses(c, false).getFlattened();
					Set<OWLClass> all = new HashSet<OWLClass>(ecs);
					all.addAll(scs);
					List<String> ids = new ArrayList<String>();
					for (OWLClass sc : all) {
						ids.add(g.getIdentifier(sc));
					}
					Collections.sort(ids);
					lines.add(g.getIdentifier(c)+"\t"+StringUtils.join(ids.iterator(), ","));
				}
				Collections.sort(lines);
				FileUtils.writeLines(new File(filename), lines);
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
			else if (opts.nextEq("--export-table")) {
				opts.info("[-c] OUTPUTFILENAME",
						"saves the ontology in tabular format (PARTIALLY IMPLEMENTED)");
				boolean isWriteHeader = false;
				while (opts.hasOpts()) {
					if (opts.nextEq("-c"))
						opts.info("", "write column headers");
					isWriteHeader = true;
				}
				String out = opts.nextOpt();
				TableRenderer tr = new TableRenderer(out);
				tr.isWriteHeader = isWriteHeader;

				tr.render(g);				
			}
			else if (opts.nextEq("--export-edge-table")) {
				opts.info("OUTPUTFILENAME",
						"saves the ontology edges in tabular format");
				String out = opts.nextOpt();
				EdgeTableRenderer tr = new EdgeTableRenderer(out);
				tr.render(g);				
			}
			else if (opts.nextEq("--materialize-gcis")) {
				opts.info("[-m]",
						"infers axioms using GCIUtil");
				boolean isMerge = false;
				while (opts.hasOpts()) {
					if (opts.nextEq("-m|--merge")) {
						isMerge = true;
					}
					else {
						break;
					}
				}
				if (reasoner == null) {
					System.err.println("REASONER NOT INITIALIZED!");
				}

				OWLDataFactory df = g.getDataFactory();
				Set<OWLSubClassOfAxiom> axioms = 
						GCIUtil.getSubClassOfSomeValuesFromAxioms(g.getSourceOntology(), reasoner);

				if (!isMerge) {
					g.setSourceOntology(g.getManager().createOntology());					
				}
				g.getManager().addAxioms(g.getSourceOntology(), axioms);

			}
			else if (opts.nextEq("--assert-inferred-svfs")) {
				opts.info("[-p LIST] [-m] [-gp PROPERTY] [-gf FILLER]",
						"asserts inferred parents by property using ExtendedReasoner");
				String out = null;
				boolean isMerge = false;
				List<OWLObjectProperty> props = null;
				OWLObjectProperty gciProperty = null;
				List<OWLClass> gciFillers = null;
				while (opts.hasOpts()) {
					if (opts.nextEq("-p|--plist")) {
						props = this.resolveObjectPropertyListAsList(opts);
					}
					else if (opts.nextEq("-o|--output")) {
						out = opts.nextOpt();
					}
					else if (opts.nextEq("-gp|--gci-property")) {
						gciProperty = this.resolveObjectProperty(opts.nextOpt());
					}
					else if (opts.nextEq("-gf|--gci-fillers")) {
						gciFillers = resolveClassList(opts);
					}
					else if (opts.nextEq("-m|--merge")) {
						isMerge = true;
					}
					else {
						break;
					}
				}
				if (reasoner == null) {
					System.err.println("REASONER NOT INITIALIZED!");
				}
				if (!(reasoner instanceof OWLExtendedReasoner)) {
					System.err.println("REASONER NOT AN EXTENDED REASONER. Recommended: --reasoner mexr");
				}
				OWLExtendedReasoner emr = (OWLExtendedReasoner) reasoner;


				OWLDataFactory df = g.getDataFactory();
				Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
				for (OWLClass c : g.getAllOWLClasses()) {
					for (OWLObjectProperty p : props) {
						for (OWLClass  parent : emr.getSuperClassesOver(c, p, true)) {

							axioms.add(df.getOWLSubClassOfAxiom(c, 
									df.getOWLObjectSomeValuesFrom(p, parent)));
						}
					}
				}
				if (!isMerge) {
					g.setSourceOntology(g.getManager().createOntology());					
				}
				g.getManager().addAxioms(g.getSourceOntology(), axioms);

			}
			else if (opts.nextEq("--export-parents")) {
				opts.info("[-p LIST] [-o OUTPUTFILENAME]",
						"saves a table of all direct inferred parents by property using ExtendedReasoner");
				String out = null;
				List<OWLObjectProperty> props = null;
				OWLObjectProperty gciProperty = null;
				List<OWLClass> gciFillers = null;
				while (opts.hasOpts()) {
					if (opts.nextEq("-p|--plist")) {
						props = this.resolveObjectPropertyListAsList(opts);
					}
					else if (opts.nextEq("-o|--output")) {
						out = opts.nextOpt();
					}
					else if (opts.nextEq("-gp|--gci-property")) {
						gciProperty = this.resolveObjectProperty(opts.nextOpt());
					}
					else if (opts.nextEq("-gf|--gci-fillers")) {
						gciFillers = resolveClassList(opts);
					}
					else {
						break;
					}
				}
				InferredParentRenderer tr = 
						new InferredParentRenderer(out);
				tr.setProperties(props);
				if (gciFillers != null)
					tr.setGciFillers(gciFillers);
				if (gciProperty != null)
					tr.setGciProperty(gciProperty);
				tr.setReasoner((OWLExtendedReasoner) reasoner);
				tr.render(g);				
			}
			else if (opts.nextEq("--remove-annotation-assertions")) {
				opts.info("[-l][-d][-s][-r][-p IRI]*", 
						"removes annotation assertions to make a pure logic subset. Select axioms can be preserved");
				boolean isPreserveLabels = false;
				boolean isPreserveDefinitions = false;
				boolean isPreserveSynonyms = false;
				boolean isPreserveRelations = false;
				boolean isPreserveDeprecations = true;
				Set<IRI> preserveAnnotationPropertyIRIs = new HashSet<IRI>();
				while (opts.hasOpts()) {
					if (opts.nextEq("-l|--preserve-labels")) {
						opts.info("", "if specified, all rdfs labels are preserved");
						isPreserveLabels = true;
					}
					else if (opts.nextEq("-d|--preserve-definitions")) {
						opts.info("", "if specified, all obo text defs are preserved");
						isPreserveDefinitions = true;
					}
					else if (opts.nextEq("-s|--preserve-synonyms")) {
						opts.info("", "if specified, all obo-style synonyms are preserved");
						isPreserveSynonyms = true;
					}
					else if (opts.nextEq("--remove-deprecation-axioms")) {
						opts.info("", "if specified, all owl:deprecated in NOT preserved");
						isPreserveDeprecations = true;
					}
					else if (opts.nextEq("-r|--preserve-relations")) {
						opts.info("", "unless specified, all axioms about properties are removed");
						isPreserveRelations = true;
					}
					else if (opts.nextEq("-p|--preserve-property")) {
						opts.info("IRI",
								"if specified, all properties with IRI are preserved. Can be specified multiple times");
						preserveAnnotationPropertyIRIs.add(IRI.create(opts.nextOpt()));
					}
					else
						break;
				}
				for (OWLOntology o : g.getAllOntologies()) {
					Set<OWLAxiom> rmAxioms =
							new HashSet<OWLAxiom>();
					Set<OWLAxiom> keepAxioms = 
							new HashSet<OWLAxiom>();
					Set<OWLAnnotationProperty> propsToKeep = new HashSet<OWLAnnotationProperty>();
					rmAxioms.addAll( o.getAxioms(AxiomType.ANNOTATION_ASSERTION) );
					for (OWLAnnotationAssertionAxiom aaa : o.getAxioms(AxiomType.ANNOTATION_ASSERTION)) {
						IRI piri = aaa.getProperty().getIRI();
						if (isPreserveLabels) {
							if (aaa.getProperty().isLabel()) {
								keepAxioms.add(aaa);
							}
						}
						if (isPreserveDeprecations) {
							if (aaa.getProperty().isDeprecated()) {
								keepAxioms.add(aaa);
							}
						}
						if (isPreserveDefinitions) {
							if (piri.equals(Obo2OWLVocabulary.IRI_IAO_0000115.getIRI())) {
								keepAxioms.add(aaa);
							}
						}
						if (isPreserveSynonyms) {
							if (piri.equals(Obo2OWLVocabulary.IRI_OIO_hasBroadSynonym.getIRI()) ||
									piri.equals(Obo2OWLVocabulary.IRI_OIO_hasExactSynonym.getIRI()) ||
									piri.equals(Obo2OWLVocabulary.IRI_OIO_hasRelatedSynonym.getIRI()) ||
									piri.equals(Obo2OWLVocabulary.IRI_OIO_hasNarrowSynonym.getIRI())) {
								keepAxioms.add(aaa);
							}
						}
						if (preserveAnnotationPropertyIRIs.contains(piri))
							keepAxioms.add(aaa);

						// remove non-classes
						if (!isPreserveRelations) {
							if (aaa.getSubject() instanceof IRI) {
								OWLClass c = g.getDataFactory().getOWLClass((IRI) aaa.getSubject());
								if (o.getDeclarationAxioms(c).size() == 0) {
									keepAxioms.remove(aaa);
								}
							}
						}

						if (keepAxioms.contains(aaa)) {
							propsToKeep.add(aaa.getProperty());
						}
					}
					LOG.info("Number of annotation assertion axioms:"+rmAxioms.size());
					LOG.info("Axioms to preserve:"+keepAxioms.size());
					rmAxioms.removeAll(keepAxioms);
					LOG.info("Number of annotation assertion axioms being removed:"+rmAxioms.size());

					if (!isPreserveRelations) {
						for (OWLAnnotationProperty p : o.getAnnotationPropertiesInSignature()) {
							if (propsToKeep.contains(p))
								continue;
							rmAxioms.addAll(o.getAnnotationAssertionAxioms(p.getIRI()));
							rmAxioms.add(g.getDataFactory().getOWLDeclarationAxiom(p));
						}
						LOG.info("Total number of axioms being removed, including annotation properties:"+rmAxioms.size());
					}
					g.getManager().removeAxioms(o, rmAxioms);

					// TODO - remove axiom annotations

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
				opts.info("[-m PREFIX URI]* [-p PREFIX] [-a] [-n]", "Translates the OBO xref property (or alt_id property, if -a set) into equivalence axioms");
				Map<String,String> prefixMap = new HashMap<String,String>();
				Set<String> prefixes = new HashSet<String>();
				boolean isNew = false;
				boolean isUseAltIds = false;
				while (opts.hasOpts()) {
					if (opts.nextEq("-m")) {
						opts.info("PREFIX URI", "maps prefixs/DBs to URI prefixes");
						prefixMap.put(opts.nextOpt(), opts.nextOpt());
					}
					else if (opts.nextEq("-p")) {
						opts.info("PREFIX", "prefix to filter on");
						prefixes.add(opts.nextOpt());
					}
					else if (opts.nextEq("-a")) {
						opts.info("", "if true, use obo alt_ids");
						isUseAltIds = true;
					}
					else if (opts.nextEq("-n")) {
						opts.info("", "if set, will generate a new ontology containing only equiv axioms");
						isNew = true;
					}
					else {
						break;
					}
				}
				Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
				for (OWLClass c : g.getAllOWLClasses()) {
					List<String> xrefs = g.getXref(c);
					if (isUseAltIds) {
						xrefs = g.getAltIds(c);
						LOG.info("Class "+c+" altIds: "+xrefs.size());
					}
					for (String x : xrefs) {
						IRI iri = null;
						if (x.contains(" ")) {
							LOG.warn("Ignore xref with space: "+x);
							continue;
						}
						if (x.contains(":")) {
							String[] parts = x.split(":",2);
							if (prefixes.size() > 0 && !prefixes.contains(parts[0])) {
								continue;
							}
							if (prefixMap.containsKey(parts[0])) {
								iri = IRI.create(prefixMap.get(parts[0]) + parts[1]);
							}
						}
						if (iri == null) {
							iri = g.getIRIByIdentifier(x);
						}
						axioms.add(g.getDataFactory().getOWLEquivalentClassesAxiom(c,
								g.getDataFactory().getOWLClass(iri)));
					}
				}
				if (isNew) {
					g.setSourceOntology(g.getManager().createOntology());
				}
				g.getManager().addAxioms(g.getSourceOntology(), axioms);
			}
			else if (opts.nextEq("--repair-relations")) {
				opts.info("", "replaces un-xrefed relations with correct IRIs");
				OWLEntityRenamer oer = new OWLEntityRenamer(g.getManager(), g.getAllOntologies());
				List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange> ();
				for (OWLObjectProperty p : g.getSourceOntology().getObjectPropertiesInSignature()) {
					IRI piri = p.getIRI();
					if (piri.getFragment().equals("part_of")) {
						List<OWLOntologyChange> ch = oer.changeIRI(piri, g.getIRIByIdentifier("BFO:0000050"));
						changes.addAll(ch);						
					}
					if (piri.getFragment().equals("has_part")) {
						List<OWLOntologyChange> ch = oer.changeIRI(piri, g.getIRIByIdentifier("BFO:0000051"));
						changes.addAll(ch);						
					}
				}
				LOG.info("Repairs: "+changes.size());
				g.getManager().applyChanges(changes);
				OboInOwlCardinalityTools.checkAnnotationCardinality(g.getSourceOntology());
			}
			else if (opts.nextEq("--rename-entity")) {
				opts.info("OLD-IRI NEW-IRI", "used OWLEntityRenamer to switch IDs/IRIs");
				OWLEntityRenamer oer = new OWLEntityRenamer(g.getManager(), g.getAllOntologies());
				List<OWLOntologyChange> changes = oer.changeIRI(IRI.create(opts.nextOpt()),IRI.create(opts.nextOpt()));
				g.getManager().applyChanges(changes);
			}
			else if (opts.nextEq("--merge-equivalence-sets")) {
				opts.info("[-s PREFIX SCORE]* [-l PREFIX SCORE]* [-c PREFIX SCORE]* [-d PREFIX SCORE]*", "merges sets of equivalent classes. Prefix-based priorities used to determine representative member");
				EquivalenceSetMergeUtil esmu = new EquivalenceSetMergeUtil(g, reasoner);
				while (opts.hasOpts()) {
					if (opts.nextEq("-s")) {
						opts.info("PREFIX SCORE", "Assigns a priority score for a prefix used to determine REPRESENTATIVE IRI for merging. E.g. -s HP 5 -s MP 4");
						esmu.setPrefixScore( opts.nextOpt(), Double.parseDouble(opts.nextOpt()) );
					}
					else if (opts.nextEq("-l")) {
						opts.info("PREFIX SCORE", "Assigns a priority score to determine which LABEL should be used post-merge. E.g. -s HP 5 -s MP 4 means HP prefered");
						OWLAnnotationProperty p = g.getDataFactory().getOWLAnnotationProperty( OWLRDFVocabulary.RDFS_LABEL.getIRI() );
						esmu.setPropertyPrefixScore( p, opts.nextOpt(), Double.parseDouble(opts.nextOpt()) );
					}
					else if (opts.nextEq("-c")) {
						opts.info("PREFIX SCORE", "Assigns a priority score to determine which COMMENT should be used post-merge. E.g. -s HP 5 -s MP 4 means HP prefered");
						OWLAnnotationProperty p = g.getDataFactory().getOWLAnnotationProperty( OWLRDFVocabulary.RDFS_COMMENT.getIRI() );
						esmu.setPropertyPrefixScore( p, opts.nextOpt(), Double.parseDouble(opts.nextOpt()) );
					}
					else if (opts.nextEq("-d")) {
						opts.info("PREFIX SCORE", "Assigns a priority score to determine which DEFINITION should be used post-merge. E.g. -s HP 5 -s MP 4");

						OWLAnnotationProperty p = g.getDataFactory().getOWLAnnotationProperty( Obo2OWLVocabulary.IRI_IAO_0000115.getIRI() );
						esmu.setPropertyPrefixScore( p, opts.nextOpt(), Double.parseDouble(opts.nextOpt()) );
					}
					else {
						break;
					}
				}
				esmu.merge();
			}
			else if (opts.nextEq("--merge-equivalent-classes")) {
				opts.info("[-f FROM-URI-PREFIX]* [-t TO-URI-PREFIX] [-a] [-sa]", "merges equivalent classes, from source(s) to target ontology");
				List<String> prefixFroms = new Vector<String>();
				String prefixTo = null;
				boolean isKeepAllAnnotations = false;
				boolean isPrioritizeAnnotationsFromSource = false;
				while (opts.hasOpts()) {
					if (opts.nextEq("-f")) {
						opts.info("", "a URI or OBO prefix for the source entities. This may be listed multiple times");
						String pfx = opts.nextOpt();
						if (!pfx.startsWith("http"))
							pfx = "http://purl.obolibrary.org/obo/"+pfx+"_";
						prefixFroms.add(pfx);
					}
					else if (opts.nextEq("-t")) {
						opts.info("", "a URI or OBO prefix for the target entities. This must be listed once");
						prefixTo = opts.nextOpt();
						if (!prefixTo.startsWith("http"))
							prefixTo = "http://purl.obolibrary.org/obo/"+prefixTo+"_";
					}
					else if (opts.nextEq("-a|--keep-all-annotations")) {
						opts.info("", "if set, all annotations are preserved. Resulting ontology may have duplicate labels and definitions");
						isKeepAllAnnotations = true;
					}
					else if (opts.nextEq("-sa|--prioritize-annotations-from-source")) {
						opts.info("", "if set, then when collapsing label and def annotations, use the source annotation over the target");
						isPrioritizeAnnotationsFromSource = true;
					}
					else
						break;
				}

				Map<OWLEntity,IRI> e2iri = new HashMap<OWLEntity,IRI>();
				LOG.info("building entity2IRI map...: " + prefixFroms + " --> "+prefixTo);
				OWLEntityRenamer oer = new OWLEntityRenamer(g.getManager(), g.getAllOntologies());

				Set<OWLAxiom> rmAxioms = new HashSet<OWLAxiom>();
				// we only map classes in the source ontology - however,
				// we use equivalence axioms from the full complement of ontologies
				// TODO - allow arbitrary entities
				Map<Integer,Integer> binSizeMap = new HashMap<Integer,Integer>();
				for (OWLClass e : g.getSourceOntology().getClassesInSignature()) {
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
						Set<OWLClass> ecs = new HashSet<OWLClass>();
						if (reasoner != null) {
							ecs = reasoner.getEquivalentClasses(e).getEntities();
						}
						else {
							// we also scan support ontologies for equivalencies
							for (OWLOntology ont : g.getAllOntologies()) {
								// c is the same of e.. why do this?
								OWLClass c = ont.getOWLOntologyManager().getOWLDataFactory().getOWLClass(e.getIRI());

								for (OWLEquivalentClassesAxiom eca : ont.getEquivalentClassesAxioms(c)) {
									ecs.addAll(eca.getNamedClasses());
								}
							}
						}
						int size = ecs.size();
						if (binSizeMap.containsKey(size)) {
							binSizeMap.put(size, binSizeMap.get(size) +1);
						}
						else {
							binSizeMap.put(size, 1);
						}
						for (OWLClass d : ecs) {
							if (d.equals(e))
								continue;
							if (prefixTo == null || d.getIRI().toString().startsWith(prefixTo)) {

								// add to mapping. Renaming will happen later
								e2iri.put(e, d.getIRI()); // TODO one-to-many

								// annotation collapsing. In OBO, max cardinality of label, comment and definition is 1
								// note that this not guaranteed to work if multiple terms are being merged in
								if (!isKeepAllAnnotations) {
									OWLClass mainObj = d;
									OWLClass secondaryObj = e;
									if (isPrioritizeAnnotationsFromSource) {
										mainObj = e;
										secondaryObj = d;
									}
									// ensure OBO cardinality of properties is preserved
									for (OWLAnnotationAssertionAxiom aaa :
										g.getSourceOntology().getAnnotationAssertionAxioms(secondaryObj.getIRI())) {
										if (aaa.getProperty().isLabel()) {
											if (g.getLabel(mainObj) != null) {
												rmAxioms.add(aaa); // todo - option to translate to synonym
											}
										}
										if (aaa.getProperty().getIRI().equals(Obo2OWLVocabulary.IRI_IAO_0000115.getIRI())) {
											if (g.getDef(mainObj) != null) {
												rmAxioms.add(aaa);
											}
										}
										if (aaa.getProperty().isComment()) {
											if (g.getComment(mainObj) != null) {
												rmAxioms.add(aaa);
											}
										}
									}
								}
							}
						}
					}
				}
				for (Integer k : binSizeMap.keySet()) {
					LOG.info(" | Bin( "+k+" classes ) | = "+binSizeMap.get(k));
				}
				g.getManager().removeAxioms(g.getSourceOntology(), rmAxioms);
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
						for (OWLClassExpression d : OwlHelper.getEquivalentClasses(c, ont)) {
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
				OWLPrettyPrinter owlpp = getPrettyPrinter();

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
			else if (opts.nextEq("--ontology-to-markdown")) {
				opts.info("[-l LEVEL] DIR", "writes md from ontology");
				int level = 2;
				while (opts.hasOpts()) {
					if (opts.nextEq("-l|--level")) {
						level = Integer.parseInt(opts.nextOpt());
					}
					else
						break;

				}
				String dir = opts.nextOpt();
				MarkdownRenderer mr = new MarkdownRenderer();
				mr.setChunkLevel(level);
				mr.render(g.getSourceOntology(), dir);
			}
			else if (opts.nextEq("--add-obo-shorthand-to-properties")) {
				Set<OWLObjectProperty> props = g.getSourceOntology().getObjectPropertiesInSignature(Imports.INCLUDED);
				OWLDataFactory df = g.getDataFactory();
				Set<OWLAxiom> addAxioms = new HashSet<OWLAxiom>();
				Set<OWLAxiom> removeAxioms = new HashSet<OWLAxiom>();
				final String MODE_MISSING = "missing"; // add missing axioms
				final String MODE_REPLACE = "replace"; // replace all axioms
				final String MODE_ADD = "add"; // old mode, which is very broken
				String mode = MODE_MISSING; // safe default, only add missing axioms

				while (opts.hasOpts()) {
					if (opts.nextEq("-m|--add-missing")) {
						mode = MODE_MISSING;
					}
					else if (opts.nextEq("-r|--replace-all")) {
						mode = MODE_REPLACE;
					}
					else if (opts.nextEq("--always-add")) {
						// this models the old broken behavior, generally not recommended
						mode = MODE_ADD;
					}
					else {
						break;
					}
				}
				if (MODE_ADD.equals(mode)) {
					LOG.warn("Using the always add mode is not recommended. Make an explicit choice by either add missing '-m' or replace all '-r' shorthand information.");
				}
				final OWLAnnotationProperty shorthandProperty = df.getOWLAnnotationProperty(IRI.create("http://www.geneontology.org/formats/oboInOwl#shorthand"));
				final OWLAnnotationProperty xrefProperty = df.getOWLAnnotationProperty(IRI.create("http://www.geneontology.org/formats/oboInOwl#hasDbXref"));

				for (OWLObjectProperty prop : props) {
					if (prop.isBuiltIn()) {
						continue;
					}
					IRI entity = prop.getIRI();
					// retrieve existing
					Set<OWLAnnotationAssertionAxiom> annotationAxioms = g.getSourceOntology().getAnnotationAssertionAxioms(entity);
					Set<OWLAnnotationAssertionAxiom> shorthandAxioms = new HashSet<OWLAnnotationAssertionAxiom>();
					Set<OWLAnnotationAssertionAxiom> xrefAxioms = new HashSet<OWLAnnotationAssertionAxiom>();
					for (OWLAnnotationAssertionAxiom axiom : annotationAxioms) {
						OWLAnnotationProperty property = axiom.getProperty();
						if (shorthandProperty.equals(property)) {
							shorthandAxioms.add(axiom);
						}
						else if (xrefProperty.equals(property)) {
							xrefAxioms.add(axiom);
						}
					}

					// check what needs to be added
					boolean addShortHand = false;
					boolean addXref = false;

					if (MODE_REPLACE.equals(mode)) {
						// replace existing axioms
						removeAxioms.addAll(shorthandAxioms);
						removeAxioms.addAll(xrefAxioms);
						addShortHand = true;
						addXref = true;
					}
					else if (MODE_MISSING.equals(mode)) {
						// add missing axioms
						addShortHand = shorthandAxioms.isEmpty();
						addXref = xrefAxioms.isEmpty();
					}
					else if (MODE_ADD.equals(mode)) {
						// old broken mode: regardless of current axioms always add axioms
						addShortHand = true;
						addXref = true;
					}

					// create required axioms
					if (addShortHand) {
						// shorthand
						String id = g.getLabel(prop);
						if (id != null) {
							id = id.replaceAll(" ", "_");
							OWLAxiom ax = df.getOWLAnnotationAssertionAxiom(
									shorthandProperty,
									prop.getIRI(), 
									df.getOWLLiteral(id));
							addAxioms.add(ax);
							LOG.info(ax);
						}
						else {
							LOG.error("No label: "+prop);
						}
					}
					if (addXref) {
						// xref to OBO style ID
						String pid = Owl2Obo.getIdentifier(prop.getIRI());
						OWLAxiom ax = df.getOWLAnnotationAssertionAxiom(
								xrefProperty,
								prop.getIRI(), 
								df.getOWLLiteral(pid));
						addAxioms.add(ax);
						LOG.info(ax);
					}

				}
				// update axioms
				if (removeAxioms.isEmpty() == false) {
					LOG.info("Total axioms removed: "+removeAxioms.size());
					g.getManager().addAxioms(g.getSourceOntology(), removeAxioms);
				}
				if (addAxioms.isEmpty() == false) {
					LOG.info("Total axioms added: "+addAxioms.size());
					g.getManager().addAxioms(g.getSourceOntology(), addAxioms);
				}
			}
			else if (opts.nextEq("--extract-properties")) {
				LOG.warn("Deprecated - use --extract-module");
				opts.info("[-p PROP]* [--list PLIST] [--no-shorthand]", "extracts properties from source ontology. If properties not specified, then support ontologies will be used");
				Set<OWLProperty> props = new HashSet<OWLProperty>();
				boolean useProps = false;
				boolean isCreateShorthand = true;
				boolean isExpansive = false;
				boolean isUseSubProperties = false;

				UUID uuid = UUID.randomUUID();
				IRI newIRI = IRI.create("http://purl.obolibrary.org/obo/temporary/"+uuid.toString());
				while (opts.hasOpts()) {
					if (opts.nextEq("-p")) {
						opts.info("PROP", "Add this property to the set of interest");
						props.add(this.resolveObjectProperty(opts.nextOpt()));
						useProps = true;
					}
					else if (opts.nextEq("-s|--subproperties")) {
						opts.info("", "If set, subproperties are used");
						isUseSubProperties = true;
					}
					else if (opts.nextEq("--list")) {
						opts.info("PROPLIST", 
								"Terminated by '//'. Add these properties to the set of interest. ALL-POPERTIES for all");
						Set<OWLObjectProperty> nprops = this.resolveObjectPropertyList(opts);
						props.addAll(nprops);
						useProps = true;
					}
					else if (opts.nextEq("--no-shorthand")) {
						opts.info("", "Do not create OBO shorthands. Resulting OBO format will use numeric IDs as primary");
						isCreateShorthand = false;
					}
					else {
						break;
					}
				}

				PropertyExtractor pe;
				pe = new PropertyExtractor(g.getSourceOntology());
				pe.isCreateShorthand = isCreateShorthand;
				pe.isUseSubProperties = isUseSubProperties;
				OWLOntology pont;
				if (useProps) {
					// use user-specified proeprty list
					pont = pe.extractPropertyOntology(newIRI, props);
				}
				else {
					// use the support ontology as the source of property usages
					pont = pe.extractPropertyOntology(newIRI, g.getSupportOntologySet().iterator().next());
				}

				g.setSourceOntology(pont);
			}
			else if (opts.nextEq("--extract-mingraph")) {
				opts.info("", "Extracts a minimal graph ontology containing only label, subclass and equivalence axioms");
				String idspace = null;
				boolean isPreserveOntologyAnnotations = false;
				while (opts.hasOpts()) {
					if (opts.nextEq("--idspace")) {
						opts.info("IDSPACE", "E.g. GO. If set, only the reflexive closure of this ontology will be included");
						idspace = opts.nextOpt();
					}
					else if (opts.nextEq("-a|--preserve-ontology-annotations")) {
						opts.info("", "Set if ontology header is to be preserved");
						isPreserveOntologyAnnotations = true;
					}
					else {
						break;
					}
				}

				Set <OWLClass> seedClasses = new HashSet<OWLClass>();
				OWLOntology src = g.getSourceOntology();
				Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
				Set<OWLAnnotation> anns = new HashSet<OWLAnnotation>();
				if (isPreserveOntologyAnnotations) {
					anns = src.getAnnotations();
				}

				axioms.addAll(src.getAxioms(AxiomType.SUBCLASS_OF));
				axioms.addAll(src.getAxioms(AxiomType.EQUIVALENT_CLASSES));
				for (OWLAnnotationAssertionAxiom aaa : src.getAxioms(AxiomType.ANNOTATION_ASSERTION)) {
					if (aaa.getProperty().isLabel()) {
						axioms.add(aaa);
						//LOG.info("LABEL:"+aaa);
					}
				}
				removeAxiomsReferencingDeprecatedClasses(axioms);
				LOG.info("#axioms: "+axioms.size());
				for (OWLClass c : src.getClassesInSignature()) {
					String id = g.getIdentifier(c);
					if (idspace == null || id.startsWith(idspace+":")) {
						boolean isDep = false;
						for (OWLAnnotation ann : OwlHelper.getAnnotations(c, src)) {
							if (ann.isDeprecatedIRIAnnotation()) {
								isDep = true;
								break;
							}
						}
						if (!isDep) {
							seedClasses.add(c);
						}
					}
				}
				LOG.info("#classes: "+seedClasses.size());
				g.addSupportOntology(src);
				OWLOntology newOnt = src.getOWLOntologyManager().createOntology(axioms);
				Set<OWLClass> retainedClasses = removeUnreachableAxioms(newOnt, seedClasses);
				for (OWLClass c : retainedClasses) {
					newOnt.getOWLOntologyManager().addAxiom(newOnt, 
							g.getDataFactory().getOWLDeclarationAxiom(c));
				}

				PropertyExtractor pe;
				pe = new PropertyExtractor(src);
				pe.isCreateShorthand = true;
				OWLOntology pont;
				HashSet<OWLProperty> props = new HashSet<OWLProperty>();
				for (OWLObjectProperty p : newOnt.getObjectPropertiesInSignature()) {
					props.add(p);
				}
				pont = pe.extractPropertyOntology(null, props);
				axioms = new HashSet<OWLAxiom>();
				for (OWLAxiom axiom : pont.getAxioms()) {
					if (axiom instanceof OWLObjectPropertyCharacteristicAxiom) {
						axioms.add(axiom);
					}
					else if (axiom instanceof OWLSubObjectPropertyOfAxiom) {
						axioms.add(axiom);
					}
					else if (axiom instanceof OWLSubPropertyChainOfAxiom) {
						axioms.add(axiom);
					}
					else if (axiom instanceof OWLAnnotationAssertionAxiom) {
						OWLAnnotationAssertionAxiom aaa = (OWLAnnotationAssertionAxiom) axiom;
						if (aaa.getProperty().isLabel()) {
							axioms.add(axiom);
						}
						else if (aaa.getProperty().getIRI().toString().toLowerCase().contains("shorthand")) { // TODO: fix hack
							axioms.add(axiom);
						}
						else if (aaa.getProperty().getIRI().toString().toLowerCase().contains("xref")) { // TODO: fix hack
							axioms.add(axiom);
						}
					}
					else if (axiom instanceof OWLDeclarationAxiom) {
						axioms.add(axiom);
					}
				}
				newOnt.getOWLOntologyManager().addAxioms(newOnt, axioms);
				g.setSourceOntology(newOnt);	

				for (OWLAnnotation ann : anns ) {
					AddOntologyAnnotation addAnn = new AddOntologyAnnotation(g.getSourceOntology(), ann);
					g.getManager().applyChange(addAnn);
				}

				//g.mergeOntology(pont);
				AxiomAnnotationTools.reduceAxiomAnnotationsToOboBasic(newOnt);
				OboInOwlCardinalityTools.checkAnnotationCardinality(newOnt);
			}
			else if (opts.nextEq("--extract-axioms")) {
				opts.info("[-t TYPE]", "Extracts axioms of specified type into the source ontology (existing source is moved to support)");
				AxiomType axiomType = AxiomType.EQUIVALENT_CLASSES;
				while (opts.hasOpts()) {
					if (opts.nextEq("-t|--type")) {
						opts.info("AxiomType", "OWL2 syntax for axiom type. Default is EquivalentClasses");
						String axiomTypeStr = opts.nextOpt();
						axiomType = AxiomType.getAxiomType(axiomTypeStr);
						if (axiomType == null) {
							throw new OptionException("invalid axiom type "+axiomTypeStr+" -- must be OWL2 syntax, e.g. 'SubClassOf'");		
						}
					}
					else {
						break;
					}
				}

				OWLOntology src = g.getSourceOntology();
				LOG.info("axiomType = "+axiomType);
				Set<OWLAxiom> axioms = src.getAxioms(axiomType);
				LOG.info("#axioms: "+axioms.size());
				g.addSupportOntology(src);
				OWLOntology newOnt = src.getOWLOntologyManager().createOntology(axioms);
				g.setSourceOntology(newOnt);				
			}
			else if (opts.nextEq("--extract-bridge-ontologies")) {
				opts.info("[-d OUTDIR] [-x] [-s ONTID]", "");
				String dir = "bridge/";
				String ontId = null;
				boolean isRemoveBridgeAxiomsFromSource = false;
				RDFXMLDocumentFormat fmt = new RDFXMLDocumentFormat();
				while (opts.hasOpts()) {
					if (opts.nextEq("-d")) {
						opts.info("DIR", "bridge files are generated in this directory. Default: ./bridge/");
						dir = opts.nextOpt();
					}
					else if (opts.nextEq("-c")) {
						opts.info("TGT SRCLIST", "Combines all src onts to tgt. TODO");
						String tgt = opts.nextOpt();
						List<String> srcs = opts.nextList();
					}
					else if (opts.nextEq("-x")) {
						opts.info("", "If specified, bridge axioms are removed from the source");
						isRemoveBridgeAxiomsFromSource = true;
					}
					else if (opts.nextEq("-s")) {
						opts.info("ONTID", "If specified, ...");
						ontId = opts.nextOpt();
					}
					else {
						break;
					}
				}
				BridgeExtractor be = new BridgeExtractor(g.getSourceOntology());
				be.subDir = dir;
				be.extractBridgeOntologies(ontId, isRemoveBridgeAxiomsFromSource);
				be.saveBridgeOntologies(dir, fmt);

			}
			else if (opts.nextEq("--expand-macros")) {
				opts.info("", "performs expansion on assertions and expressions. "+
						"See http://oboformat.googlecode.com/svn/trunk/doc/obo-syntax.html#7");
				MacroExpansionVisitor mev = 
						new MacroExpansionVisitor(g.getSourceOntology());
				mev.expandAll();
			}
			else if (opts.nextEq("--expand-expression")) {
				opts.info("PROP EXPRESSION", "uses OBO Macros to expand expressions with PROP to the target expression using ?Y");
				OWLObjectProperty p = resolveObjectProperty(opts.nextOpt());
				String expr = opts.nextOpt();
				OWLAnnotationAssertionAxiom aaa = g.getDataFactory().getOWLAnnotationAssertionAxiom(
						g.getDataFactory().getOWLAnnotationProperty(Obo2OWLVocabulary.IRI_IAO_0000424.getIRI()),
						p.getIRI(), 
						g.getDataFactory().getOWLLiteral(expr));
				g.getManager().addAxiom(g.getSourceOntology(), aaa);
				MacroExpansionVisitor mev = 
						new MacroExpansionVisitor(g.getSourceOntology());
				mev.expandAll();
			}
			else if (opts.nextEq("--expand-assertion")) {
				opts.info("PROP ASSERTION", "uses OBO Macros to expand expressions with PROP to the target expression using ?X and ?Y");
				OWLNamedObject p = (OWLNamedObject) this.resolveEntity(opts.nextOpt());
				String expr = opts.nextOpt();
				OWLAnnotationAssertionAxiom aaa = g.getDataFactory().getOWLAnnotationAssertionAxiom(
						g.getDataFactory().getOWLAnnotationProperty(Obo2OWLVocabulary.IRI_IAO_0000425.getIRI()),
						p.getIRI(),
						g.getDataFactory().getOWLLiteral(expr));
				g.getManager().addAxiom(g.getSourceOntology(), aaa);
				MacroExpansionVisitor mev = 
						new MacroExpansionVisitor(g.getSourceOntology());
				mev.expandAll();
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
			else if (opts.nextEq("--remove-tbox")) {
				opts.info("", "removes all class axioms");
				for (OWLOntology ont : g.getAllOntologies()) {
					Set<OWLAxiom> rmAxioms = new HashSet<OWLAxiom>();
					for (OWLAxiom ax : ont.getAxioms()) {
						if (ax instanceof OWLClassAxiom) {
							rmAxioms.add(ax);
						}
						else if (ax instanceof OWLDeclarationAxiom) {
							if ( ((OWLDeclarationAxiom)ax).getEntity() instanceof OWLClass) {
								rmAxioms.add(ax);
							}
						}
						else if (ax instanceof OWLAnnotationAssertionAxiom) {
							OWLAnnotationSubject subj = ((OWLAnnotationAssertionAxiom)ax).getSubject();
							if (subj instanceof IRI) {
								// warning - excessive pruning if there is punning
								if (ont.getClassesInSignature(Imports.INCLUDED).contains(g.getDataFactory().getOWLClass((IRI) subj))) {
									rmAxioms.add(ax);
								}
							}
						}

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
					for (OWLClassExpression ce : OwlHelper.getTypes(i, ont)) {
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
				opts.info("[-r reasonername] [-m] [-d] [-a] [-x] [-c IRI] (--stdin | CLASS-EXPRESSION | -l CLASS-LABEL)", 
						"Queries current ontology for descendants, ancestors and equivalents of CE using reasoner.\n"+
						"Enclose all labels in quotes (--stdin only). E.g. echo \"'part of' some 'tentacle'\" | owltools ceph.owl --reasoner-query --stdin");
				boolean isManifest = false;
				boolean isDescendants = true;
				boolean isIndividuals = false;
				boolean isAncestors = true;
				boolean isEquivalents = true;
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
								"manifests the class exression as a class equivalent to query CE and uses this as a query; required for older versions of Elk");
						isManifest = true;
					}
					else if (opts.nextEq("-d")) {
						opts.info("", "show descendants, but not ancestors (default is both + equivs)");
						isDescendants = true;
						isAncestors = false;
					}
					else if (opts.nextEq("-a")) {
						opts.info("", "show ancestors, but not descendants (default is both + equivs)");
						isDescendants = false;
						isAncestors = true;
					}
					else if (opts.nextEq("-e")) {
						opts.info("", "show equivalents only (default is ancestors + descendants + equivs)");
						isDescendants = false;
						isAncestors = false;
					}
					else if (opts.nextEq("-i")) {
						opts.info("", "show inferred individuals, as well as ancestors/descendants/equivalents");
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
				OWLPrettyPrinter owlpp = getPrettyPrinter();
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
								g.getSourceOntology().getAxioms(qc, Imports.EXCLUDED));
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
						xr = new ExpressionMaterializingReasoner(g.getSourceOntology(), createReasonerFactory(reasonerName));	
						LOG.info("materializing... [doing this before initializing reasoner]");					
						xr.materializeExpressions();
						LOG.info("set extended reasoner: "+xr);
						reasoner = xr;
					}
					else {
						if (reasoner == null) {
							reasoner = createReasoner(g.getSourceOntology(), reasonerName, g.getManager());
						}
					}
					if (isIndividuals) {
						for (OWLNamedIndividual r : reasoner.getInstances(ce, false).getFlattened()) {
							//results.add(r);
							if (!isCache)
								System.out.println("D: "+owlpp.render(r));
						}
					}
					if (isEquivalents) {
						for (OWLClass r : reasoner.getEquivalentClasses(ce).getEntities()) {
							results.add(r);
							if (!isCache)
								System.out.println("E: "+owlpp.render(r));
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


				} catch (OWLParserException e) {
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
					if (opts.nextEq("-f|--follow-closure|--fill-gaps")) {
						opts.info("", 
								"using mooncat will have the effect of including the graph closure of all results in the output ontology");
						followClosure = true;
					}
					else if (opts.nextEq("-m|--use-mooncat")) {
						opts.info("", 
								"using mooncat will have the effect of including the graph closure of all results in the output ontology");
						useMooncat = true;
					}
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
								for (OWLAxiom ax : mergeOntology.getAxioms((OWLClass)cls, Imports.EXCLUDED)) {
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
							subsetAxioms.addAll(mergeOntology.getAxioms(p, Imports.EXCLUDED));
							subsetAxioms.addAll(mergeOntology.getAnnotationAssertionAxioms(p.getIRI()));
						}
					}

					OWLOntology subOnt = g.getManager().createOntology(IRI.create(subOntologyIRI));
					g.getManager().addAxioms(subOnt, subsetAxioms);
					g.setSourceOntology(subOnt);
				}
			}
			else if (opts.nextEq("--remove-equivalent-to-nothing-axioms")) {
				Set<OWLAxiom> axs = new HashSet<OWLAxiom>();
				OWLClass nothing = g.getDataFactory().getOWLNothing();
				for (OWLAxiom ax : g.getSourceOntology().getAxioms(AxiomType.EQUIVALENT_CLASSES)) {
					if (ax.getClassesInSignature().contains(nothing)) {
						axs.add(ax);
					}
				}
				g.getManager().removeAxioms(g.getSourceOntology(), axs);
			}
			else if (opts.nextEq("--check-disjointness-axioms")) {
				boolean isTranslateEquivalentToNothing = true;
				OWLPrettyPrinter owlpp = getPrettyPrinter();

				OWLOntology ont = g.getSourceOntology();
				Set<OWLObjectIntersectionOf> dPairs = new
						HashSet<OWLObjectIntersectionOf>();
				Map<OWLClassExpression, Set<OWLClassExpression>> dMap = 
						new HashMap<OWLClassExpression, Set<OWLClassExpression>>();
				OWLClass nothing = g.getDataFactory().getOWLNothing();
				Set<OWLAxiom> rmAxioms = new HashSet<OWLAxiom>();
				if (isTranslateEquivalentToNothing) {
					// TODO
					for (OWLEquivalentClassesAxiom eca : ont.getAxioms(AxiomType.EQUIVALENT_CLASSES)) {
						if (eca.contains(nothing)) {
							for (OWLClassExpression x : eca.getClassExpressionsMinus(nothing)) {
								if (x instanceof OWLObjectIntersectionOf) {
									dPairs.add((OWLObjectIntersectionOf) x);
									System.out.println("TRANSLATED:"+x);
								}

							}
							rmAxioms.add(eca);
						}

					}
				}

				for (OWLDisjointClassesAxiom dca : ont.getAxioms(AxiomType.DISJOINT_CLASSES)) {
					for (OWLClassExpression x : dca.getClassExpressions()) {
						for (OWLClassExpression y : dca.getClassExpressions()) {
							if (!x.equals(y)) {
								dPairs.add(g.getDataFactory().getOWLObjectIntersectionOf(x,y));
							}
						}						
					}
				}

				g.getManager().removeAxioms(ont, ont.getAxioms(AxiomType.DISJOINT_CLASSES));
				g.getManager().removeAxioms(ont, rmAxioms);
				reasoner.flush();
				for (OWLObjectIntersectionOf x : dPairs) {
					//System.out.println("TESTING: "+owlpp.render(x)+" using "+reasoner);
					for (Node<OWLClass> v : reasoner.getSubClasses(x, false)) {
						if (v.contains(nothing))
							continue;
						System.out.println("VIOLATION: "+owlpp.render(v.getRepresentativeElement())+" SubClassOf "+owlpp.render(x));
					}
				}

			}
			else if (opts.nextEq("--remove-disjoints")) {
				List<AxiomType<? extends OWLAxiom>> disjointTypes = new ArrayList<AxiomType<? extends OWLAxiom>>(); 
				disjointTypes.add(AxiomType.DISJOINT_CLASSES);
				disjointTypes.add(AxiomType.DISJOINT_UNION);
				disjointTypes.add(AxiomType.DISJOINT_OBJECT_PROPERTIES);
				disjointTypes.add(AxiomType.DISJOINT_DATA_PROPERTIES);
				for(AxiomType<? extends OWLAxiom> axtype : disjointTypes) {
					Set<? extends OWLAxiom> axioms = g.getSourceOntology().getAxioms(axtype);
					if (axioms.isEmpty() == false) {
						g.getManager().removeAxioms(g.getSourceOntology(), axioms);
					}
				}
			}
			else if (opts.nextEq("--abox-to-tbox")) {
				ABoxUtils.translateABoxToTBox(g.getSourceOntology());
			}
			else if (opts.nextEq("--make-default-abox")) {
				ABoxUtils.makeDefaultIndividuals(g.getSourceOntology());
			}
			else if (opts.nextEq("--tbox-to-abox")) {
				OWLInAboxTranslator t = new OWLInAboxTranslator(g.getSourceOntology());
				while (opts.hasOpts()) {
					if (opts.nextEq("-p|--preserve-iris|--preserve-object-properties")) {
						opts.info("", "Use the same OP IRIs for ABox shows (danger will robinson!)");
						t.setPreserveObjectPropertyIRIs(true);
					}
					else {
						break;
					}
				}
				OWLOntology abox = t.translate();
				g.setSourceOntology(abox);
			}
			else if (opts.nextEq("--map-abox-to-results")) {
				Set<OWLClass> cs = new HashSet<OWLClass>();
				for (OWLObject obj : owlObjectCachedSet) {
					if (obj instanceof OWLClass)
						cs.add((OWLClass) obj);
				}
				ABoxUtils.mapClassAssertionsUp(g.getSourceOntology(), reasoner, cs, null);
			}
			else if (opts.nextEq("--map-abox-to-namespace")) {
				String ns = opts.nextOpt();
				Set<OWLClass> cs = new HashSet<OWLClass>();
				for (OWLClass c : g.getSourceOntology().getClassesInSignature(Imports.INCLUDED)) {
					if (c.getIRI().toString().startsWith(ns))
						cs.add(c);
				}
				ABoxUtils.mapClassAssertionsUp(g.getSourceOntology(), reasoner, cs, null);
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
				OWLPrettyPrinter owlpp = getPrettyPrinter();
				OWLOntology ont = g.getSourceOntology();
				for (OWLClass c : g.getSourceOntology().getClassesInSignature()) {
					if (q.startsWith("e")) {
						for (OWLClass ec : reasoner.getEquivalentClasses(c)) {
							OWLEquivalentClassesAxiom ax = g.getDataFactory().getOWLEquivalentClassesAxiom(c, ec);
							if (!ont.containsAxiom(ax, Imports.INCLUDED, AxiomAnnotations.IGNORE_AXIOM_ANNOTATIONS)) {
								LOG.info("INFERRED: "+owlpp.render(ax));
								iAxioms.add(ax);
							}
						}
					}
					else if (q.startsWith("s")) {
						Set<OWLClass> supers = reasoner.getSuperClasses(c, isDirect).getFlattened();
						for (OWLClass sc : supers) {
							OWLSubClassOfAxiom ax = g.getDataFactory().getOWLSubClassOfAxiom(c, sc);
							ax.getObjectPropertiesInSignature();
							if (!ont.containsAxiom(ax, Imports.INCLUDED, AxiomAnnotations.IGNORE_AXIOM_ANNOTATIONS)) {
								LOG.info("INFERRED: "+owlpp.render(ax));
								iAxioms.add(ax);
							}
						}
						if (isRemoveIndirect) {
							for (OWLClass sc : reasoner.getSuperClasses(c, false).getFlattened()) {
								if (!supers.contains(sc)) {
									OWLSubClassOfAxiom ax = g.getDataFactory().getOWLSubClassOfAxiom(c, sc);
									if (ont.containsAxiom(ax, Imports.INCLUDED, AxiomAnnotations.IGNORE_AXIOM_ANNOTATIONS)) {
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
							if (!ont.containsAxiom(ax, Imports.INCLUDED, AxiomAnnotations.IGNORE_AXIOM_ANNOTATIONS)) {
								LOG.info("INFERRED: "+owlpp.render(ax));
								iAxioms.add(ax);
							}
						}
						if (isRemoveIndirect) {
							for (OWLClass ce : reasoner.getTypes(i, false).getFlattened()) {
								if (!types.contains(ce)) {
									OWLClassAssertionAxiom ax = g.getDataFactory().getOWLClassAssertionAxiom(ce, i);
									if (ont.containsAxiom(ax, Imports.EXCLUDED, AxiomAnnotations.IGNORE_AXIOM_ANNOTATIONS)) {
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
			else if (opts.nextEq("--annotate-with-reasoner")) {
				opts.info("[-c OntologyIRI]", "annotated existing and inferred subClassOf axioms with source");
				OWLOntology outputOntology = null;
				while (opts.hasOpts()) {
					if (opts.nextEq("-c||--create")) {
						outputOntology = g.getManager().createOntology(IRI.create(opts.nextOpt()));
					}
					else {
						break;
					}
				}
				ProvenanceReasonerWrapper pr = 
						new ProvenanceReasonerWrapper(g.getSourceOntology(), new ElkReasonerFactory());
				if (outputOntology != null) {
					pr.outputOntology = outputOntology;
				}
				pr.reason();		
				if (outputOntology != null) {
					g.setSourceOntology(outputOntology);
				}
			}
			else if (opts.nextEq("--run-reasoner")) {
				opts.info("[-r reasonername] [--assert-implied] [--indirect] [-u]", "infer new relationships");
				boolean isAssertImplied = false;
				boolean isDirect = true;
				boolean isShowUnsatisfiable = false;
				boolean isRemoveUnsatisfiable = false;
				boolean showExplanation = false;
				String unsatisfiableModule = null;
				boolean traceModuleAxioms = false; // related to unsatisfiableModule

				while (opts.hasOpts()) {
					if (opts.nextEq("-r")) {
						opts.info("REASONERNAME", "selects the reasoner to use");
						reasonerName = opts.nextOpt();
					}
					else if (opts.nextEq("--assert-implied")) {
						isAssertImplied = true;
					}
					else if (opts.nextEq("--indirect")) {
						opts.info("", "include indirect inferences");
						isDirect = false;
					}
					else if (opts.nextEq("-u|--list-unsatisfiable")) {
						opts.info("", "list all unsatisfiable classes");
						isShowUnsatisfiable = true;
					}
					else if (opts.nextEq("-e|--show-explanation")) {
						opts.info("", "add a single explanation for each unsatisfiable class");
						showExplanation = true;
					}
					else if (opts.nextEq("-x|--remove-unsatisfiable")) {
						opts.info("", "remove all unsatisfiable classes");
						isRemoveUnsatisfiable = true;
						isShowUnsatisfiable = true;
					}
					else if (opts.nextEq("-m|--unsatisfiable-module")) {
						opts.info("", "create a module for the unsatisfiable classes.");
						unsatisfiableModule = opts.nextOpt();
					}
					else if (opts.nextEq("--trace-module-axioms")) {
						traceModuleAxioms = true;
					}
					else {
						break;
					}
				}
				OWLPrettyPrinter owlpp = getPrettyPrinter();

				boolean isQueryProcessed = false;
				if (reasoner == null) {
					reasoner = createReasoner(g.getSourceOntology(),reasonerName,g.getManager());
				}
				if (isShowUnsatisfiable || isRemoveUnsatisfiable) {
					int n = 0;
					Set<OWLClass> unsats = new HashSet<OWLClass>();
					LOG.info("Finding unsatisfiable classes");
					Set<OWLClass> unsatisfiableClasses = reasoner.getUnsatisfiableClasses().getEntitiesMinusBottom();
					ExplanationGenerator explanationGenerator = null;
					if (showExplanation) {
						OWLReasonerFactory factory = createReasonerFactory(reasonerName);
						explanationGenerator = new DefaultExplanationGenerator(g.getManager(), factory, g.getSourceOntology(), reasoner, null);
					}
					for (OWLClass c : unsatisfiableClasses) {
						if (c.isBuiltIn()) {
							continue;
						}
						unsats.add(c);
						StringBuilder msgBuilder = new StringBuilder();
						msgBuilder.append("UNSAT: ").append(owlpp.render(c));
						if (explanationGenerator != null) {
							Set<OWLAxiom> explanation = explanationGenerator.getExplanation(c);
							if (explanation.isEmpty() == false) {
								msgBuilder.append('\t');
								msgBuilder.append("explanation:");
								for (OWLAxiom axiom : explanation) {
									msgBuilder.append('\t');
									msgBuilder.append(owlpp.render(axiom));
								}
							}
						}
						System.out.println(msgBuilder);
						n++;
					}
					System.out.println("NUMBER_OF_UNSATISFIABLE_CLASSES: "+n);
					if (unsatisfiableModule != null) {
						LOG.info("Creating module for unsatisfiable classes in file: "+unsatisfiableModule);
						ModuleType mtype = ModuleType.BOT;
						OWLOntologyManager m = g.getManager();
						SyntacticLocalityModuleExtractor sme = new SyntacticLocalityModuleExtractor(m, g.getSourceOntology(), mtype );
						Set<OWLEntity> seeds = new HashSet<OWLEntity>(unsatisfiableClasses);
						Set<OWLAxiom> axioms = sme.extract(seeds);
						OWLOntology module = m.createOntology();
						if (traceModuleAxioms) {
							axioms = traceAxioms(axioms, g, module.getOWLOntologyManager().getOWLDataFactory());
						}
						m.addAxioms(module, axioms);
						File moduleFile = new File(unsatisfiableModule).getCanonicalFile();
						m.saveOntology(module, IRI.create(moduleFile));
					}
					if (n > 0) {
						if (isRemoveUnsatisfiable) {
							Mooncat m = new Mooncat(g);
							m.removeSubsetClasses(unsats, true);
							isQueryProcessed = true;
						}
						else {
							LOG.error("Ontology has unsat classes - will not proceed");
							exit(1);
						}
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
				// this should probably be deprecated - deliberate
				// non-local effects from separate command
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
					LOG.info("Checking for consistency...");
					System.out.println("Consistent? "+reasoner.isConsistent());
					if (!reasoner.isConsistent()) {
						for (OWLClass c : reasoner.getUnsatisfiableClasses()) {
							System.out.println("UNSAT: "+owlpp.render(c));
						}
					}
					LOG.info("Iterating through all classes...");
					for (OWLObject obj : g.getAllOWLObjects()) {
						if (obj instanceof OWLClass) {
							OWLClass c = ((OWLClass) obj);
							// find all asserted parents in ontology and its import closure;
							// we do not want to re-assert 
							Set<OWLClassExpression> assertedSuperclasses =
									OwlHelper.getSuperClasses(c, g.getSourceOntology().getImportsClosure());
							//assertedSuperclasses.addAll(c.getEquivalentClasses(g.getSourceOntology().getImportsClosure()));
							//Set<OWLClass> eqCs = reasoner.getEquivalentClasses(c).getEntities();
							for (OWLClass sup : reasoner.getSuperClasses(c, isDirect).getFlattened()) {
								if (assertedSuperclasses.contains(sup)) {
									continue;
								}
								if (sup.isOWLThing())
									continue;
								System.out.println("INFERENCE: "+owlpp.render(obj)+" SubClassOf "+owlpp.render(sup));
								if (isAssertImplied) {
									OWLSubClassOfAxiom sca = g.getDataFactory().getOWLSubClassOfAxiom(c, sup);
									g.getManager().addAxiom(g.getSourceOntology(), sca);
								}
							}

							for (OWLClass ec : reasoner.getEquivalentClasses(((OWLClassExpression) obj)).getEntities()) {
								if (!ec.equals(obj))
									System.out.println("INFERENCE: "+owlpp.render(obj)+" EquivalentTo "+owlpp.render(ec));
								if (isAssertImplied) {
									OWLEquivalentClassesAxiom eca = g.getDataFactory().getOWLEquivalentClassesAxiom(c, ec);
									g.getManager().addAxiom(g.getSourceOntology(), eca);
								}

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
								LOG.error("  I have: "+ont.getOntologyID());
							}
							for (OWLOntology ont : g.getSourceOntology().getImportsClosure()) {
								LOG.error("  IC: "+ont.getOntologyID());
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
						if (OwlHelper.getEquivalentClasses((OWLClass)subc, g.getSourceOntology()).isEmpty()) {
							continue;
						}
						if (OwlHelper.getEquivalentClasses((OWLClass)supc, g.getSourceOntology()).isEmpty()) {
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
				Set<OWLPropertyExpression> props = new HashSet<OWLPropertyExpression>();
				boolean useProps = false;
				while (opts.hasOpts()) {
					if (opts.nextEq("-p")) {
						opts.info("PROP", "Add this property to the set of interest");
						props.add(this.resolveObjectProperty(opts.nextOpt()));
						useProps = true;
					}
					else if (opts.nextEq("--plist")) {
						opts.info("PROPLIST", "Terminated by '//'. Add these properties to the set of interest");
						Set<OWLObjectProperty> nprops = this.resolveObjectPropertyList(opts);
						props.addAll(nprops);
						useProps = true;
					}
					else {
						break;
					}
				}
				OWLObject obj = resolveEntity(opts);
				System.out.println(obj+ " "+obj.getClass()+" P:"+props);
				if (!useProps)
					props = null;
				Set<OWLGraphEdge> edges = g.getOutgoingEdgesClosureReflexive(obj,props);
				showEdges(edges);
			}
			else if (opts.nextEq("--ancestor-nodes")) {
				opts.info("LABEL", "list nodes in graph closure to root nodes");
				Set<OWLPropertyExpression> props = new HashSet<OWLPropertyExpression>();
				boolean useProps = false;
				while (opts.hasOpts()) {
					if (opts.nextEq("-p")) {
						opts.info("PROP", "Add this property to the set of interest");
						props.add(this.resolveObjectProperty(opts.nextOpt()));
						useProps = true;
					}
					else if (opts.nextEq("--plist")) {
						opts.info("PROPLIST", "Terminated by '//'. Add these properties to the set of interest");
						Set<OWLObjectProperty> nprops = this.resolveObjectPropertyList(opts);
						props.addAll(nprops);
						useProps = true;
					}
					else {
						break;
					}
				}
				OWLObject obj = resolveEntity(opts);
				System.out.println(obj+ " "+obj.getClass()+" P:"+props);
				if (!useProps)
					props = null;
				for (OWLObject a : g.getAncestors(obj, props)) 
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
				OWLPrettyPrinter owlpp = getPrettyPrinter();
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
				OWLPrettyPrinter owlpp = getPrettyPrinter();
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
				OWLPrettyPrinter owlpp = getPrettyPrinter();
				owlpp.print("## Obsoleting: "+obj);
				Set<OWLAxiom> refAxioms = g.getSourceOntology().getReferencingAxioms((OWLEntity) obj);
				Set<OWLClassAxiom> axioms = g.getSourceOntology().getAxioms((OWLClass) obj, Imports.EXCLUDED);
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
				String catfile = "catalog-v001.xml";
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
				opts.info("[-f FORMAT] [--prefix PREFIX URI]* FILE", "writes source ontology.");
				OWLDocumentFormat ofmt = new RDFXMLDocumentFormat();

				String ontURIStr = "";
				if ( g.getSourceOntology().getOntologyID() != null && g.getSourceOntology().getOntologyID().getOntologyIRI().isPresent()) {
					ontURIStr = g.getSourceOntology().getOntologyID().getOntologyIRI().get().toString();
				}
				while (opts.hasOpts()) {
					if (opts.nextEq("-f")) {
						opts.info("FORMAT", "omn OR ofn OR ttl OR owx OR ojs (experimental) OR obo (uses obooformat jar)");
						String ofmtname = opts.nextOpt();
						if (ofmtname.equals("manchester") || ofmtname.equals("omn")) {
							ofmt = new ManchesterSyntaxDocumentFormat();
						}
						else if (ofmtname.equals("functional") || ofmtname.equals("ofn")) {
							ofmt = new FunctionalSyntaxDocumentFormat();
						}
						else if (ofmtname.equals("turtle") || ofmtname.equals("ttl")) {
							ofmt = new TurtleDocumentFormat();
						}
						else if (ofmtname.equals("xml") || ofmtname.equals("owx")) {
							ofmt = new OWLXMLDocumentFormat();
						}
						else if (ofmtname.equals("ojs")) {
							ofmt = new OWLJSONFormat();
						}
						else if (ofmtname.equals("obo")) {
							if (opts.nextEq("-n|--no-check")) {
								pw.setCheckOboDoc(false);
							}
							ofmt = new OBODocumentFormat();
						}
					}
					else if (opts.nextEq("--prefix")) {
						opts.info("PREFIX URIBASE","use PREFIX as prefix. Note: specify this sub-arg AFTER -f");
						ofmt.asPrefixOWLOntologyFormat().setPrefix(opts.nextOpt(), opts.nextOpt());
					}
					else {
						break;
					}
				}

				LOG.info("saving:"+ontURIStr+" using "+ofmt);

				if (opts.hasArgs()) {
					String outputFile = opts.nextOpt();
					pw.saveOWL(g.getSourceOntology(), ofmt, outputFile);
					//pw.saveOWL(g.getSourceOntology(), opts.nextOpt());
				}
				else {
					final String msg = "Missing output file for '-o' OR '--output' option. Output was not written to a file.";
					throw new OptionException(msg);
				}

			}
			else if (opts.nextEq("--filter-axioms")) {
				Set<AxiomType> types = new HashSet<AxiomType>();
				while (opts.hasOpts()) {
					if (opts.nextEq("-t|--axiom-type")) {
						types.add( AxiomType.getAxiomType(opts.nextOpt()) );
					}
					else {
						break;
					}
				}
				for (OWLOntology o : g.getSourceOntology().getImportsClosure()) {
					Set<OWLAxiom> rmAxioms = new HashSet<OWLAxiom>();
					for (OWLAxiom ax :  o.getAxioms()) {
						if (!types.contains(ax.getAxiomType())) {
							rmAxioms.add(ax);
						}
					}
					LOG.info("Removing axioms: "+rmAxioms.size());
					g.getManager().removeAxioms(o, rmAxioms);
				}
			}
			else if (opts.nextEq("--remove-axioms")) {
				AxiomType t = null;
				while (opts.hasOpts()) {
					if (opts.nextEq("-t|--axiom-type")) {
						t = AxiomType.getAxiomType(opts.nextOpt());
					}
					else {
						break;
					}
				}
				for (OWLOntology o : g.getSourceOntology().getImportsClosure()) {
					Set<OWLAxiom> axioms = o.getAxioms(t);
					LOG.info("Removing axioms: "+axioms.size());
					g.getManager().removeAxioms(o, axioms);
				}
			}
			else if (opts.nextEq("--remove-axiom-annotations")) {
				for (OWLAxiom a : g.getSourceOntology().getAxioms()) {
					Set<OWLAnnotation> anns = a.getAnnotations();
					if (anns.size() > 0) {
						AxiomAnnotationTools.changeAxiomAnnotations(a, new HashSet<OWLAnnotation>(), g.getSourceOntology());						
					}
				}
			}
			else if (opts.nextEq("--make-super-slim")) {
				opts.info("IDSPACES", 
						"removes all classes not in the superclass closure of any ontology in one of the idspaces." +
						" also assers superclasses");
				boolean isTempReasoner = false;
				if (reasoner == null) {
					reasoner = this.createReasoner(g.getSourceOntology(), "elk", g.getManager());
					isTempReasoner = true;
				}
				String idspacesStr = opts.nextOpt();
				LOG.info("idsps = "+idspacesStr);
				String[] idarr = idspacesStr.split(",");
				Set<String> idspaces = new HashSet<String>(Arrays.asList(idarr));
				LOG.info("idsps = "+idspaces);
				Set<OWLClass> cs = new HashSet<OWLClass>();
				for (OWLClass c : g.getAllOWLClasses()) {	
					String id = g.getIdentifier(c);
					String[] idparts = id.split(":");
					String idspace = idparts[0];
					if (idspaces.contains(idspace)) {

						cs.addAll(reasoner.getEquivalentClasses(c).getEntities());
						cs.addAll(reasoner.getSuperClasses(c, false).getFlattened());
					}
				}
				AssertInferenceTool.assertInferences(g, false, false, false, true, false, false, false, null, null);
				Mooncat m = new Mooncat(g);
				m.removeSubsetComplementClasses(cs, true);
				if (isTempReasoner) {
					reasoner.dispose();
				}
			}
			else if (opts.nextEq("--split-ontology")) {
				opts.info("[-p IRI-PREFIX] [-s IRI-SUFFIX] [-d OUTDIR] [-l IDSPACE1 ... IDPSPACEn]", 
						"Takes current only extracts all axioms in ID spaces and writes to separate ontology PRE+lc(IDSPACE)+SUFFIX saving to outdir. Also adds imports");
				String prefix = g.getSourceOntology().getOntologyID().getOntologyIRI().get().toString().replace(".owl", "/");
				String suffix = "_import.owl";
				String outdir = ".";
				Set<String> idspaces = new HashSet<String>();
				while (opts.hasOpts()) {
					if (opts.nextEq("-p|--prefix"))
						prefix = opts.nextOpt();
					else if (opts.nextEq("-s|--suffix"))
						suffix = opts.nextOpt();
					else if (opts.nextEq("-d|--dir"))
						outdir = opts.nextOpt();
					else if (opts.nextEq("-l|--idspaces")) {
						idspaces.addAll(opts.nextList());
					}
					else
						break;
				}
				Mooncat m = new Mooncat(g);
				for (String idspace : idspaces) {
					LOG.info("Removing "+idspace);
					String name = prefix + idspace + suffix;
					IRI iri = IRI.create(name);
					OWLOntology subOnt = 
							g.getManager().createOntology(iri);
					m.transferAxiomsUsingIdSpace(idspace, subOnt);
					AddImport ai = 
							new AddImport(g.getSourceOntology(),
									g.getDataFactory().getOWLImportsDeclaration(iri));
					g.getManager().applyChange(ai);
					String path = outdir + "/" + name.replaceAll(".*/", "");
					FileOutputStream stream = new FileOutputStream(new File(path));
					g.getManager().saveOntology(subOnt, stream);
				}
			}
			else if (opts.nextEq("--remove-subset")) {
				opts.info("[-d] SUBSET", "Removes a subset (aka slim) from an ontology");
				boolean isRemoveDangling = true;
				while (opts.hasOpts()) {
					if (opts.nextEq("-d|--keep-dangling")) {
						opts.info("",
								"if specified, dangling axioms (ie pointing to removed classes) are preserved");
						isRemoveDangling = false;
					}
					else
						break;
				}
				String subset = opts.nextOpt();
				Set<OWLClass> cset = g.getOWLClassesInSubset(subset);
				LOG.info("Removing "+cset.size()+" classes");
				Mooncat m = new Mooncat(g);
				m.removeSubsetClasses(cset, isRemoveDangling);
			}
			else if (opts.nextEq("--extract-subset")) {
				opts.info("SUBSET", "Extract a subset (aka slim) from an ontology, storing subset in place of existing ontology");
				String subset = opts.nextOpt();
				Set<OWLClass> cset = g.getOWLClassesInSubset(subset);
				LOG.info("Removing "+cset.size()+" classes");
				Mooncat m = new Mooncat(g);
				m.removeSubsetComplementClasses(cset, false);
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
			else if (opts.nextEq("--create-abox-subset")) {
				opts.info("CLASS",
						"Remove all ClassAssertions where the CE is not a subclass of the specified class");
				OWLClass c = this.resolveClass(opts.nextOpt());
				LOG.info("SUBSET: "+c);
				//Set<OWLNamedIndividual> inds = g.getSourceOntology().getIndividualsInSignature(true);
				Set<OWLClassAssertionAxiom> caas =
						g.getSourceOntology().getAxioms(AxiomType.CLASS_ASSERTION);
				Set<OWLAxiom> rmAxioms = new HashSet<OWLAxiom>();
				for (OWLClassAssertionAxiom a : caas) {
					Set<OWLClass> sups = reasoner.getSuperClasses(a.getClassExpression(), false).getFlattened();
					if (!sups.contains(c)) {
						rmAxioms.add(a);
					}
				}
				LOG.info("Removing: "+rmAxioms.size() + " / "+caas.size());
				g.getManager().removeAxioms(g.getSourceOntology(), rmAxioms);
				reasoner.flush();
			}
			else if (opts.nextEq("--load-instances")) {
				TableToAxiomConverter ttac = new TableToAxiomConverter(g);
				ttac.config.axiomType = AxiomType.CLASS_ASSERTION;
				ttac.config.isSwitchSubjectObject = true;
				while (opts.hasOpts()) {
					if (opts.nextEq("-p|--property")) {
						ttac.config.property = ((OWLNamedObject) resolveObjectProperty( opts.nextOpt())).getIRI();
					}
					else {
						break;
					}
				}

				String f = opts.nextOpt();
				System.out.println("tabfile: "+f);
				ttac.parse(f);			
			}
			else if (opts.nextEq("--load-labels")) {
				TableToAxiomConverter ttac = new TableToAxiomConverter(g);
				ttac.config.setPropertyToLabel();
				ttac.config.axiomType = AxiomType.ANNOTATION_ASSERTION;
				String f = opts.nextOpt();
				ttac.parse(f);			
			}
			else if (opts.nextEq("--add-labels")) {
				Set<Integer> colsToLabel = new HashSet<Integer>();
				while (opts.hasOpts()) {
					if (opts.nextEq("-c|--column")) {
						opts.info("COLNUMS", "number of col to label (starting from 1). Can be comma-separated list");
						String v = opts.nextOpt();
						for (String cn : v.split(",")) { 
							colsToLabel.add(Integer.valueOf(cn)-1);
						}
					}
					else {
						break;
					}
				}	
				LOG.info("Labeling: "+colsToLabel);
				File f = opts.nextFile();
				List<String> lines = FileUtils.readLines(f);
				for (String line : lines) {
					String[] vals = line.split("\\t");

					for (int i=0; i<vals.length; i++) {
						if (i>0)
							System.out.print("\t");
						System.out.print(vals[i]);
						if (colsToLabel.contains(i)) {
							String label = "NULL";
							String v = vals[i];
							if (v != null && !v.equals("") && !v.contains(" ")) {
								OWLObject obj = g.getOWLObjectByIdentifier(v);
								if (obj != null) {
									label = g.getLabel(obj);
								}
							}
							System.out.print("\t"+label);
						}
					}
					System.out.println();
				}
			}
			else if (opts.nextEq("--parse-tsv")) {
				opts.info("[-s] [-l] [--comment] [-m] [-p PROPERTY] [-a AXIOMTYPE] [-t INDIVIDUALSTYPE] FILE", "parses a tabular file to OWL axioms");
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
					else if (opts.nextEq("--object-non-literal")) {
						ttac.config.isObjectLiteral = false;
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
			else if (opts.nextEq("--parse-stanzas")) {
				opts.info("[-m KEY PROPERTY]* [-s]  FILE", "parses a tabular file to OWL axioms");
				StanzaToOWLConverter sc = new StanzaToOWLConverter(g);
				while (opts.hasOpts()) {
					if (opts.nextEq("-m|--map")) {
						String k = opts.nextOpt();
						StanzaToOWLConverter.Mapping m = sc.new Mapping();
						String p = opts.nextOpt();
						m.property = this.resolveObjectProperty(p); // TODO - allow other types
						sc.config.keyMap.put(k, m);
					}
					else if (opts.nextEq("-s|--strict")) {
						opts.info("", "set if to be run in strict mode");
						sc.config.isStrict = true;
					}				
					else if (opts.nextEq("--prefix")) {
						sc.config.defaultPrefix = opts.nextOpt();
					}				
					else {
						continue;
					}
				}
				String f = opts.nextOpt();
				System.out.println("tabfile: "+f);
				sc.parse(f);
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
			else if (opts.nextEq("--extract-ontology-subset")) {
				opts.info("[-i FILE][-u IRI][-s SUBSET][--fill-gaps]", "performs slimdown using IDs from FILE or from named subset");
				IRI subOntIRI = IRI.create("http://purl.obolibrary.org/obo/"+g.getOntologyId()+"-subset");
				String fileName = null;
				String subset = null;
				boolean isFillGaps = false;
				boolean isSpanGaps = true;
				while (opts.hasOpts()) {
					if (opts.nextEq("-u|--uri|--iri")) {
						subOntIRI = IRI.create(opts.nextOpt());
					}
					else if (opts.nextEq("-i|--input-file")) {
						fileName = opts.nextOpt();
					}
					else if (opts.nextEq("-s|--subset")) {
						subset = opts.nextOpt();
					}
					else if (opts.nextEq("--fill-gaps")) {
						isFillGaps = true;
						isSpanGaps = false;
					}
					else if (opts.nextEq("--minimal")) {
						isFillGaps = false;
						isSpanGaps = false;
					}
					else {
						break;
					}
				}
				Mooncat m = new Mooncat(g);
				Set<OWLClass> cs = new HashSet<OWLClass>();

				if (fileName != null) {
					LOG.info("Reading IDs from: "+fileName);
					Set<String> unmatchedIds = new HashSet<String>();
					for (String line : FileUtils.readLines(new File(fileName))) {
						OWLClass c = g.getOWLClassByIdentifierNoAltIds(line);
						if (c == null) {
							unmatchedIds.add(line);
							continue;
						}
						cs.add(c);

					}
					LOG.info("# IDs = "+cs.size());
					if (unmatchedIds.size() > 0) {
						LOG.error(fileName+" contains "+unmatchedIds.size()+" unmatched IDs");
						for (String id : unmatchedIds) {
							LOG.error("UNMATCHED: "+id);
						}
					}
				}
				if (subset != null) {
					LOG.info("Adding IDs from "+subset);
					cs.addAll(g.getOWLClassesInSubset(subset));
				}
				if (cs.size() == 0) {
					LOG.warn("EMPTY SUBSET");
				}
				// todo
				LOG.info("Making subset ontology seeded from "+cs.size()+" classes");
				g.setSourceOntology(m.makeMinimalSubsetOntology(cs, subOntIRI, isFillGaps, isSpanGaps));
				LOG.info("Made subset ontology; # classes = "+cs.size());				
			}
			else if (opts.nextEq("--extract-module")) {
				opts.info("[-n IRI] [-d] [-s SOURCE-ONTOLOGY] [-c] [-m MODULE-TYPE] SEED-OBJECTS", "Uses the OWLAPI module extractor");
				String modIRI = null;
				ModuleType mtype = ModuleType.BOT;
				boolean isTraverseDown = false;
				boolean isMerge = false;
				OWLOntology baseOnt = g.getSourceOntology();
				IRI dcSource = null;

				while (opts.hasOpts()) {
					if (opts.nextEq("-n")) {
						modIRI = opts.nextOpt();
					}
					else if (opts.nextEq("-d")) {
						opts.info("", "Is set, will traverse down class hierarchy to form seed set");
						isTraverseDown = true;
					}
					else if (opts.nextEq("-c|--merge")) {
						opts.info("", "Is set, do not use a command-line specified seed object list - use the source ontology as list of seeds");
						isMerge = true;
					}
					else if (opts.nextEq("-s|--source")) {
						String srcName = opts.nextOpt();
						baseOnt = g.getManager().getOntology(IRI.create(srcName));
						if (baseOnt == null) {
							LOG.error("Could not find specified ontology "+srcName+" for --source");
						}
					}
					else if (opts.nextEq("-m") || opts.nextEq("--module-type")) {
						opts.info("MODULE-TYPE", "One of: STAR, TOP, BOT (default)");
						mtype = ModuleType.valueOf(opts.nextOpt());
					}
					else {
						break;
					}
				}
				Set<OWLObject> objs = new HashSet<OWLObject>();
				if (isMerge) {
					// add all relations and classes to seed set
					// merge support set closure
					objs.addAll( g.getSourceOntology().getObjectPropertiesInSignature() );
					objs.addAll( g.getSourceOntology().getClassesInSignature() );
					for (OWLOntology ont : g.getSupportOntologySet())
						g.mergeOntology(ont);
					g.setSupportOntologySet(new HashSet<OWLOntology>());
				}
				else {
					objs = this.resolveEntityList(opts);
				}
				LOG.info("OBJS: "+objs.size());

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
				if (dcSource == null) {	
					OWLOntologyID oid = baseOnt.getOntologyID();
					Optional<IRI> versionIRI = oid.getVersionIRI();
					if (versionIRI.isPresent()) {
						dcSource = versionIRI.get();
					}
					else {
						Optional<IRI> ontologyIRI = oid.getOntologyIRI();
						if (ontologyIRI.isPresent()) {
							dcSource = ontologyIRI.get();
						}
					}
				}
				g.getManager().addAxioms(modOnt, modAxioms);
				g.setSourceOntology(modOnt);
				if (dcSource != null) {	
					LOG.info("Setting source: "+dcSource);
					OWLAnnotation ann = 
							g.getDataFactory().getOWLAnnotation(g.getDataFactory().getOWLAnnotationProperty(
									IRI.create("http://purl.org/dc/elements/1.1/source")),
									dcSource);
					AddOntologyAnnotation addAnn = new AddOntologyAnnotation(g.getSourceOntology(), ann);
					g.getManager().applyChange(addAnn);
				}
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
				boolean isReplace = false;
				boolean noReasoner = false;
				boolean isCreateReflexiveClasses = false;

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
					else if (opts.nextEq("--no-reasoner|nr")) {
						opts.info("", "do not build an inferred view ontology");
						noReasoner = true;
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
					else if (opts.nextEq("--reflexive")) {
						opts.info("", "Treat property as reflexive");
						isCreateReflexiveClasses = true;
					}
					else if (opts.nextEq("--replace")) {
						opts.info("", "if set, the source ontology is replaced with O(P)'");
						isReplace = true;
					}
					else if (opts.nextEq("" +
							"")) {
						annotOntol = g.getSourceOntology();
					}
					else
						break;
				}
				PropertyViewOntologyBuilder pvob = 
						new PropertyViewOntologyBuilder(sourceOntol,
								annotOntol,
								viewProperty);
				pvob.setViewLabelPrefix(prefix);
				pvob.setViewLabelSuffix(suffix);
				pvob.buildViewOntology(IRI.create("http://x.org/assertedViewOntology"), IRI.create(viewIRI));
				pvob.setFilterUnused(isFilterUnused);
				pvob.setCreateReflexiveClasses(isCreateReflexiveClasses);
				OWLOntology avo = pvob.getAssertedViewOntology();
				if (avFile != null)
					pw.saveOWL(avo, avFile);
				if (noReasoner) {
					pvob.setInferredViewOntology(pvob.getAssertedViewOntology());
				}
				else {
					OWLReasoner vr = createReasoner(avo, reasonerName, g.getManager());
					pvob.buildInferredViewOntology(vr);
					vr.dispose();
				}
				// save
				if (outFile != null)
					pw.saveOWL(pvob.getInferredViewOntology(), outFile);
				else if (isReplace) {
					g.setSourceOntology(pvob.getInferredViewOntology());
				}
				else {
					g.addSupportOntology(pvob.getInferredViewOntology());
				}
			}
			else if (opts.nextEq("--materialize-property-inferences|--mpi")) {
				opts.info("[-p [-r] PROPERTY]... [-m|--merge]", "reasoned property view. Alternative to --bpvo");
				// TODO - incorporate this into sparql query
				Set<OWLObjectProperty> vps = new HashSet<OWLObjectProperty>();
				Set<OWLObjectProperty> reflexiveVps = new HashSet<OWLObjectProperty>();
				boolean isMerge = false;
				boolean isPrereason = true;
				while (opts.hasOpts()) {
					if (opts.nextEq("-p")) {
						opts.info("[-r] PROPERTY-ID-OR-LABEL", "The ObjectProperty P that is used to build the view. If -r is specified the view is reflexive");
						boolean isReflexive = false;
						if (opts.nextEq("-r|--reflexive"))
							isReflexive = true;
						String s = opts.nextOpt();
						OWLObjectProperty viewProperty = resolveObjectProperty(s);
						if (viewProperty == null) {
							// the method resolveObjectProperty, will log already a error
							// escalate to an exception 
							throw new IOException("Could not find an OWLObjectProperty for string: "+s);
						}
						vps.add(viewProperty);
						if (isReflexive)
							reflexiveVps.add(viewProperty);
					}
					else if (opts.nextEq("--merge|-m")) {
						isMerge = true;
					}
					else if (opts.nextEq("--no-assert-inferences|-n")) {
						isPrereason = false;
					}
					else {
						break;
					}
				}
				if (!isPrereason && !isMerge) {
					LOG.warn("ontology will be empty!");
				}
				OWLOntology baseOntology = g.getSourceOntology();
				OWLOntology vOnt = g.getManager().createOntology();
				if (!isMerge) {
					// make the source ontology the new view
					g.setSourceOntology(vOnt);
				}

				Set<OWLClass> allvcs = new HashSet<OWLClass>();
				for (OWLObjectProperty vp : vps) {
					PropertyViewOntologyBuilder pvob = 
							new PropertyViewOntologyBuilder(baseOntology, vp);
					if (reflexiveVps.contains(vp))
						pvob.setCreateReflexiveClasses(true);
					pvob.buildViewOntology();
					OWLOntology avo = pvob.getAssertedViewOntology();
					Set<OWLClass> vcs = avo.getClassesInSignature();
					LOG.info("view for "+vp+" num view classes: "+vcs.size());
					allvcs.addAll(vcs);
					g.mergeOntology(avo); // todo - more sophisticated
				}
				if (isPrereason) {
					if (reasoner == null) {
						reasoner = createReasoner(g.getSourceOntology(),reasonerName,g.getManager());
						LOG.info("created reasoner: "+reasoner);
					}
					for (OWLClass c : g.getSourceOntology().getClassesInSignature(Imports.INCLUDED)) {
						Set<OWLClass> scs = reasoner.getSuperClasses(c, false).getFlattened();
						for (OWLClass sc : scs) {
							OWLSubClassOfAxiom sca = g.getDataFactory().getOWLSubClassOfAxiom(c, sc);
							g.getManager().addAxiom(vOnt, sca);
						}
						// inferred (named classes) plus asserted (include class expressions)
						Set<OWLClassExpression> ecs = OwlHelper.getEquivalentClasses(c, g.getSourceOntology());
						ecs.addAll(reasoner.getEquivalentClasses(c).getEntities());
						for (OWLClassExpression ec : ecs) {
							if (ec.equals(c))
								continue;
							OWLEquivalentClassesAxiom eca = g.getDataFactory().getOWLEquivalentClassesAxiom(c, ec);
							g.getManager().addAxiom(vOnt, eca);

							// bidirectional subclass axioms for each equivalent pair
							OWLSubClassOfAxiom sca1 = g.getDataFactory().getOWLSubClassOfAxiom(c, ec);
							g.getManager().addAxiom(vOnt, sca1);

							OWLSubClassOfAxiom sca2 = g.getDataFactory().getOWLSubClassOfAxiom(ec, c);
							g.getManager().addAxiom(vOnt, sca2);

						}
					}
				}
				else {

				}
				// TODO - turn allvcs into bnodes
				if (isMerge) {
					g.mergeOntology(vOnt);
				}
				else {
					g.setSourceOntology(vOnt);
				}
			}
			else if (opts.nextEq("--materialize-existentials")) {
				Set<OWLObjectSomeValuesFrom> svfs = new HashSet<OWLObjectSomeValuesFrom>();
				Set<OWLObjectProperty> props = new HashSet<OWLObjectProperty>();
				while (opts.hasOpts()) {
					if (opts.nextEq("-p")) {
						props.add(this.resolveObjectProperty(opts.nextOpt()));
					}
					else if (opts.nextEq("-l|--list")) {
						props.addAll(this.resolveObjectPropertyList(opts));
					}
					else {
						break;
					}
				}
				LOG.info("Materializing: "+props);
				OWLPrettyPrinter owlpp = new OWLPrettyPrinter(g);
				for (OWLOntology ont : g.getAllOntologies()) {
					for (OWLAxiom ax : ont.getAxioms()) {
						if (ax instanceof OWLSubClassOfAxiom) {
							OWLClassExpression supc = ((OWLSubClassOfAxiom)ax).getSuperClass();
							if (supc instanceof OWLObjectSomeValuesFrom) {
								svfs.add((OWLObjectSomeValuesFrom) supc);
							}
						}
						else if (ax instanceof OWLEquivalentClassesAxiom) {
							for (OWLClassExpression x : ((OWLEquivalentClassesAxiom)ax).getClassExpressions()) {
								if (x instanceof OWLObjectIntersectionOf) {
									for (OWLClassExpression y : ((OWLObjectIntersectionOf)x).getOperands()) {
										if (y instanceof OWLObjectSomeValuesFrom) {
											svfs.add((OWLObjectSomeValuesFrom) y);
										}
									}
								}
							}
						}
					}
				}
				Set<OWLAxiom> newAxioms = new HashSet<OWLAxiom>();
				OWLDataFactory df = g.getDataFactory();
				for (OWLObjectSomeValuesFrom svf : svfs) {
					if (svf.getFiller().isAnonymous())
						continue;
					if (svf.getProperty().isAnonymous())
						continue;

					OWLObjectProperty p = (OWLObjectProperty) svf.getProperty();
					if (!props.contains(p))
						continue;
					OWLClass c = (OWLClass) svf.getFiller();

					PropertyViewOntologyBuilder pvob = new PropertyViewOntologyBuilder(g.getSourceOntology(), p);
					IRI xIRI = pvob.makeViewClassIRI(c.getIRI(), p.getIRI(), "-");
					String label = "Reflexive "+ g.getLabel(p) + " " + g.getLabel(c);
					OWLClass xc = df.getOWLClass(xIRI);
					newAxioms.add(df.getOWLEquivalentClassesAxiom(xc, svf));
					newAxioms.add(df.getOWLSubClassOfAxiom(c, xc));
					newAxioms.add(df.getOWLAnnotationAssertionAxiom(df.getRDFSLabel(), xIRI, df.getOWLLiteral(label)));
				}
				LOG.info("Adding "+newAxioms.size()+ " axioms");
				g.getManager().addAxioms(g.getSourceOntology(), newAxioms);
			}
			else if (opts.nextEq("--report-profile")) {
				g.getProfiler().report();
			}
			else if (opts.nextEq("--no-cache")) {
				g.getConfig().isCacheClosure = false;
			}
			else if (opts.nextEq("--repeat")) {
				List<String> ops = new ArrayList<String>();
				while (opts.hasArgs()) {
					if (opts.nextEq("--end")) {
						break;
					}
					else {
						String op = opts.nextOpt();
						ops.add(op);
					}
				}
				// TODO
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
			else if (opts.nextEq("--load-ontologies-as-imports")) {
				opts.info("[ONT]+", "loads and adds the specified ontologies as imports");
				List<String> ontologyList = opts.nextList();
				if (ontologyList == null || ontologyList.isEmpty()) {
					LOG.error("No ontologies specified for the command. At least one ontology is required.");
					exit(-1);
				}

				// create a new empty ontology if there is no previous graph
				final OWLOntologyManager m;
				final OWLOntology containerOntology;
				if (g == null) {
					m = pw.getManager();
					containerOntology = m.createOntology(IRI.generateDocumentIRI());
					g = new OWLGraphWrapper(containerOntology);
				}
				else {
					m = g.getManager();
					containerOntology = g.getSourceOntology();
				}
				final OWLDataFactory factory = m.getOWLDataFactory();

				for(String ont : ontologyList) {
					// load ontology
					OWLOntology owlOntology = pw.parse(ont);
					// check for usable ontology ID and ontology IRI
					OWLOntologyID ontologyID = owlOntology.getOntologyID();
					if (ontologyID == null) {
						LOG.error("The ontology: "+ont+" does not have a valid ontology ID");
						exit(-1);
					}
					else {
						Optional<IRI> documentIRI = ontologyID.getDefaultDocumentIRI();
						if (documentIRI.isPresent() == false) {
							LOG.error("The ontology: "+ont+" does not have a valid document IRI");
							exit(-1);
						}else {
							// add as import, instead of merge
							OWLImportsDeclaration importDeclaration = factory.getOWLImportsDeclaration(documentIRI.get());
							OWLOntologyChange change = new AddImport(containerOntology, importDeclaration);
							m.applyChange(change);
						}
					}
				}
			}
			else {
				// check first if there is a matching annotated method
				// always check, to support introspection via '-h'
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

				if (opts.hasArgs()) {
					// Default is to treat argument as an ontology
					String f  = opts.nextOpt();
					try {
						OWLOntology ont = null;
						if (f.endsWith("obo")) {
							ont = pw.parseOBO(f);
						} else {
							ont = pw.parse(f);
						}
						if (g == null) {
							g =	new OWLGraphWrapper(ont);
						}
						else {
							System.out.println("adding support ont "+ont);
							g.addSupportOntology(ont);
						}

					}
					catch (Exception e) {
						LOG.error("could not parse:"+f, e);
						if (exitOnException) {
							exit(1);	
						}
						else {
							throw e;
						}

					}
				}
				else {
					if (opts.isHelpMode()) {
						helpFooter();
						// should only reach here in help mode
					}
				}
			}
		}
	}

	static Set<OWLAxiom> traceAxioms(Set<OWLAxiom> axioms, OWLGraphWrapper g, OWLDataFactory df) {
		final OWLAnnotationProperty p = df.getOWLAnnotationProperty(IRI.create("http://trace.module/source-ont"));
		final Set<OWLOntology> ontologies = g.getSourceOntology().getImportsClosure();
		final Set<OWLAxiom> traced = new HashSet<OWLAxiom>();
		for (OWLAxiom axiom : axioms) {
			Set<OWLOntology> hits = new HashSet<OWLOntology>();
			for(OWLOntology ont : ontologies) {
				if (ont.containsAxiom(axiom)) {
					hits.add(ont);
				}
			}
			if (hits.isEmpty()) {
				traced.add(axiom);
			}
			else {
				Set<OWLAnnotation> annotations = new HashSet<OWLAnnotation>(axiom.getAnnotations());
				for (OWLOntology hit : hits) {
					Optional<IRI> hitIRI = hit.getOntologyID().getOntologyIRI();
					if(hitIRI.isPresent()) {
						annotations.add(df.getOWLAnnotation(p, hitIRI.get()));
					}
				}
				traced.add(AxiomAnnotationTools.changeAxiomAnnotations(axiom, annotations, df));
			}
		}
		return traced;
	}


	private Set<OWLClass> removeUnreachableAxioms(OWLOntology src,
			Set<OWLClass> seedClasses) {
		Stack<OWLClass> stack = new Stack<OWLClass>();
		stack.addAll(seedClasses);
		Set<OWLClass> visited = new HashSet<OWLClass>();
		visited.addAll(stack);

		while (!stack.isEmpty()) {
			OWLClass elt = stack.pop();
			Set<OWLClass> parents = new HashSet<OWLClass>();
			Set<OWLClassExpression> xparents = OwlHelper.getSuperClasses(elt, src);
			xparents.addAll(OwlHelper.getEquivalentClasses(elt, src));
			for (OWLClassExpression x : xparents) {
				parents.addAll(x.getClassesInSignature());
			}
			//parents.addAll(getReasoner().getSuperClasses(elt, true).getFlattened());
			//parents.addAll(getReasoner().getEquivalentClasses(elt).getEntities());
			parents.removeAll(visited);
			stack.addAll(parents);
			visited.addAll(parents);
		}

		LOG.info("# in closure set to keep: "+visited.size());

		Set<OWLAxiom> rmAxioms = new HashSet<OWLAxiom>();
		for (OWLClass c : src.getClassesInSignature()) {
			if (!visited.contains(c)) {
				//LOG.info("removing axioms for EL-unreachable class: "+c);
				rmAxioms.addAll(src.getAxioms(c, Imports.EXCLUDED));
				rmAxioms.add(src.getOWLOntologyManager().getOWLDataFactory().getOWLDeclarationAxiom(c));
			}
		}

		src.getOWLOntologyManager().removeAxioms(src, rmAxioms);
		LOG.info("Removed "+rmAxioms.size()+" axioms. Remaining: "+src.getAxiomCount());		
		return visited;
	}

	private void removeAxiomsReferencingDeprecatedClasses(Set<OWLAxiom> axioms) {
		Set<OWLAxiom> rmAxioms = new HashSet<OWLAxiom>();
		for (OWLAxiom axiom : axioms) {
			for (OWLClass c : axiom.getClassesInSignature()) {
				if (g.isObsolete(c)) {
					rmAxioms.add(axiom);
					break;
				}
			}
		}
		axioms.removeAll(rmAxioms);
	}

	@CLIMethod("--external-mappings-files")
	public void createExternalMappings(Opts opts) throws Exception {
		if (g == null) {
			System.err.println("No graph available for gaf-run-check.");
			exit(-1);
			return;
		}

		File headerFilesFolder = null;
		String headerFileSuffix = ".header"; 
		List<String> externalDbNames = null;
		File outputFolder = new File(".").getCanonicalFile();
		String commentPrefix = "!";
		String labelPrefix = "";
		while (opts.hasOpts()) {
			if (opts.nextEq("-o|--output|--output-folder"))
				outputFolder = opts.nextFile().getCanonicalFile();
			else if (opts.nextEq("--go-external-default")) {
				externalDbNames = Arrays.asList("EC","MetaCyc","Reactome","RESID","UM-BBD_enzymeID","UM-BBD_pathwayID","Wikipedia");
				labelPrefix = "GO:";
			}
			else if(opts.nextEq("--label-prefix")) {
				labelPrefix = opts.nextOpt();
			}
			else if(opts.nextEq("--externals")) {
				externalDbNames = opts.nextList();
			}
			else if (opts.nextEq("--load-headers-from")) {
				headerFilesFolder = opts.nextFile().getCanonicalFile();
			}
			else if (opts.nextEq("--load-headers")) {
				headerFilesFolder = new File(".").getCanonicalFile();
			}
			else if (opts.nextEq("--set-header-file-suffix")) {
				headerFileSuffix = opts.nextOpt();
			}
			else if (opts.nextEq("--comment-prefix")) {
				commentPrefix = opts.nextOpt();
			}
			else {
				break;
			}
		}
		if (externalDbNames == null || externalDbNames.isEmpty()) {
			System.err.println("No external db for extraction defined.");
			exit(-1);
			return;
		}

		// setup date string and ontology version strings
		StringBuilder header = new StringBuilder();
		OWLOntology ont = g.getSourceOntology();
		String ontologyId = Owl2Obo.getOntologyId(ont);
		String dataVersion = Owl2Obo.getDataVersion(ont);
		header.append(commentPrefix);
		header.append(" Generated on ");
		TimeZone tz = TimeZone.getTimeZone("UTC");
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
		df.setTimeZone(tz);
		header.append(df.format(new Date()));
		if (ontologyId != null) {
			header.append(" from the ontology '");
			header.append(ontologyId);
			header.append('\'');
			if (dataVersion != null) {
				header.append(" with data version: '");
				header.append(dataVersion);
				header.append('\'');
			}
		}
		header.append('\n');
		header.append(commentPrefix).append('\n');


		// load external mappings per db type
		for(String db : externalDbNames) {
			String prefix = db+":";
			Map<String, Set<OWLClass>> externalMappings = new HashMap<String, Set<OWLClass>>();
			Set<OWLClass> allOWLClasses = g.getAllOWLClasses();
			for (OWLClass owlClass : allOWLClasses) {
				boolean obsolete = g.isObsolete(owlClass);
				if (obsolete == false) {
					List<String> xrefs = g.getXref(owlClass);
					if (xrefs != null && !xrefs.isEmpty()) {
						for (String xref : xrefs) {
							if (xref.startsWith(prefix)) {
								String x = xref;
								int whitespacePos = xref.indexOf(' ');
								if (whitespacePos > 0) {
									x = xref.substring(0, whitespacePos);
								}
								Set<OWLClass> classSet = externalMappings.get(x);
								if (classSet == null) {
									classSet = new HashSet<OWLClass>();
									externalMappings.put(x, classSet);
								}
								classSet.add(owlClass);
							}
						}
					}
				}
			}
			// sort
			List<String> xrefList = new ArrayList<String>(externalMappings.keySet());
			Collections.sort(xrefList);

			// open writer
			BufferedWriter writer = new BufferedWriter(new FileWriter(new File(outputFolder, db.toLowerCase()+"2go")));

			// check for pre-defined headers
			if (headerFilesFolder != null) {
				File headerFile = new File(headerFilesFolder, db.toLowerCase()+headerFileSuffix);
				if (headerFile.isFile() && headerFile.canRead()) {
					LineIterator lineIterator = FileUtils.lineIterator(headerFile);
					while (lineIterator.hasNext()) {
						String line = lineIterator.next();
						// minor trickery
						// if the header lines do not have the comment prefix, add it
						if (line.startsWith(commentPrefix) == false) {
							writer.append(commentPrefix);
							writer.append(' ');
						}
						writer.append(line);
						writer.append('\n');
					}
				}
			}

			// add generated header
			writer.append(header);

			// append sorted xrefs
			for (String xref : xrefList) {
				Set<OWLClass> classes = externalMappings.get(xref);
				List<OWLClass> classesList = new ArrayList<OWLClass>(classes);
				Collections.sort(classesList);
				for (OWLClass cls : classesList) {
					String id = g.getIdentifier(cls);
					String lbl = g.getLabel(cls);
					writer.append(xref);
					writer.append(" > ");
					writer.append(labelPrefix);
					writer.append(lbl);
					writer.append(" ; ");
					writer.append(id);
					writer.append('\n');
				}
			}
			IOUtils.closeQuietly(writer);
		}
	}

	@CLIMethod("--assert-abox-inferences")
	public void assertAboxInferences(Opts opts) throws Exception {
		opts.info("", "Finds all inferred OPEs and ClassAssertions and asserts them. Does not handle DPEs. Resulting ontology can be used for sparql queries");
		Set<OWLAxiom> newAxioms = new HashSet<OWLAxiom>();
		OWLOntology ont = g.getSourceOntology();

		// TODO : move this to a utility class
		OWLOntologyManager mgr = ont.getOWLOntologyManager();
		OWLDataFactory df = mgr.getOWLDataFactory();
		for (OWLNamedIndividual ind : ont.getIndividualsInSignature(Imports.INCLUDED)) {
			LOG.info("Checking: "+ind);
			for (OWLObjectProperty p : ont.getObjectPropertiesInSignature(Imports.INCLUDED)) {
				NodeSet<OWLNamedIndividual> vs = reasoner.getObjectPropertyValues(ind, p);
				for (OWLNamedIndividual v : vs.getFlattened()) {
					LOG.info("NEW: "+ind+" -> "+p+" -> "+v);
					newAxioms.add(df.getOWLObjectPropertyAssertionAxiom(p, ind, v));
				}
			}
			for (OWLClass c : reasoner.getTypes(ind, false).getFlattened()) {
				newAxioms.add(df.getOWLClassAssertionAxiom(c, ind));
				LOG.info("NEW: "+ind+" :: "+c);
			}
		}
		LOG.info("# OF NEW AXIOMS: "+newAxioms.size());
		mgr.addAxioms(ont, newAxioms);
	}

	@CLIMethod("--assert-inferred-subclass-axioms")
	public void assertInferredSubClassAxioms(Opts opts) throws Exception {
		opts.info("[--removeRedundant] [--keepRedundant] [--always-assert-super-classes] [--markIsInferred] [--useIsInferred] [--ignoreNonInferredForRemove] [--allowEquivalencies] [--reportProfile]",
				"Adds SubClassOf axioms for all entailed direct SubClasses not already asserted");
		boolean removeRedundant = true;
		boolean checkConsistency = true; 
		boolean useIsInferred = false;
		boolean ignoreNonInferredForRemove = false;
		boolean checkForNamedClassEquivalencies = true;
		boolean checkForPotentialRedundant = false;
		boolean alwaysAssertSuperClasses = false;
		String reportFile = null;

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
			else if (opts.nextEq("--reportFile")) {
				reportFile = opts.nextOpt();
			}
			else if (opts.nextEq("--always-assert-super-classes")) {
				opts.info("", "if specified, always assert a superclass, " +
						"even if there exists an equivalence axiom is trivially entails in in solation");	
				alwaysAssertSuperClasses = true;
			}
			else {
				break;
			}
		}
		BufferedWriter reportWriter = null;
		if (reportFile != null) {
			reportWriter = new BufferedWriter(new FileWriter(reportFile));
		}
		OWLClassFilter filter = null;
		try {
			AssertInferenceTool.assertInferences(g, removeRedundant, checkConsistency, useIsInferred, ignoreNonInferredForRemove, checkForNamedClassEquivalencies, checkForPotentialRedundant, alwaysAssertSuperClasses, filter, reportWriter);
		}
		finally {
			IOUtils.closeQuietly(reportWriter);
		}
	}

	@CLIMethod("--remove-redundant-superclass")
	public void removeRedundantSubclasses(Opts opts) throws Exception {
		if (g == null) {
			LOG.error("No source ontology available.");
			exit(-1);
			return;
		}
		if (reasoner == null) {
			LOG.error("No resoner available.");
			exit(-1);
			return;
		}
		if (reasoner.isConsistent() == false) {
			LOG.error("Ontology is inconsistent.");
			exit(-1);
			return;
		}
		Set<OWLClass> unsatisfiableClasses = reasoner.getUnsatisfiableClasses().getEntitiesMinusBottom();
		if (unsatisfiableClasses.isEmpty() == false) {
			LOG.error("Ontology contains unsatisfiable classes, count: "+unsatisfiableClasses.size());
			for (OWLClass cls : unsatisfiableClasses) {
				LOG.error("UNSAT:\t"+g.getIdentifier(cls)+"\t"+g.getLabel(cls));
			}
			exit(-1);
			return;
		}
		final OWLOntology rootOntology = reasoner.getRootOntology();
		final List<RemoveAxiom> changes = new ArrayList<RemoveAxiom>();
		Set<OWLClass> allClasses = rootOntology.getClassesInSignature(Imports.EXCLUDED);
		LOG.info("Check classes for redundant super class axioms, all OWL classes count: "+allClasses.size());
		for(OWLClass cls : allClasses) {
			final Set<OWLClass> directSuperClasses = reasoner.getSuperClasses(cls, true).getFlattened();
			Set<OWLSubClassOfAxiom> subClassAxioms = rootOntology.getSubClassAxiomsForSubClass(cls);
			for (final OWLSubClassOfAxiom subClassAxiom : subClassAxioms) {
				subClassAxiom.getSuperClass().accept(new OWLClassExpressionVisitorAdapter(){

					@Override
					public void visit(OWLClass desc) {
						if (directSuperClasses.contains(desc) == false) {
							changes.add(new RemoveAxiom(rootOntology, subClassAxiom));
						}
					}
				});
			}
		}
		LOG.info("Found redundant axioms: "+changes.size());
		rootOntology.getOWLOntologyManager().applyChanges(changes);
		LOG.info("Removed axioms: "+changes.size());
	}

	/**
	 * GeneOntology specific function to create links between molecular
	 * functions and their corresponding processes. This method uses the exact
	 * matching of the equivalence axioms to establish the part_of relations.<br>
	 * All relations created by this method are going to be tagged with an axiom
	 * annotation http://purl.org/dc/terms/source and corresponding GO_REF.
	 * 
	 * @param opts 
	 * @throws Exception
	 */
	@CLIMethod("--create-part-of")
	public void createPartOfLinks(Opts opts) throws Exception {
		if (g == null) {
			LOG.error("No source ontology available.");
			exit(-1);
			return;
		}
		if (reasoner == null) {
			LOG.error("No resoner available.");
			exit(-1);
			return;
		}
		String goRef = "GO_REF:0000090";
		String annotationIRIString = "http://purl.org/dc/terms/source";
		String targetFileName = null;

		while (opts.hasOpts()) {
			if (opts.nextEq("--go-ref")) {
				goRef = opts.nextOpt();
			}
			else if (opts.nextEq("--annotation-iri")) {
				annotationIRIString = opts.nextOpt();
			}
			else if (opts.nextEq("--target-file")) {
				targetFileName = opts.nextOpt();
			}
			else {
				break;
			}
		}

		if (targetFileName == null) {
			LOG.error("No target-file as output was specified.");
			exit(-1);
			return;
		}
		final File targetFile = new File(targetFileName);
		final IRI targetFileIRI = IRI.create(targetFile);

		final IRI annotationIRI = IRI.create(annotationIRIString);

		// first hard coded test for MF -> BP mappings:
		// transporter activity -part_of-> transporter
		// transmembrane transporter activity -part_of-> transmembrane transport

		final OWLClass ta = g.getOWLClassByIdentifier("GO:0005215"); // transporter activity
		final OWLClass t = g.getOWLClassByIdentifier("GO:0006810"); // transport
		final OWLClass tmta = g.getOWLClassByIdentifier("GO:0022857"); // transmembrane transport activity
		final OWLClass tmt = g.getOWLClassByIdentifier("GO:0055085"); // transmembrane transport
		final OWLObjectProperty partOf = g.getOWLObjectPropertyByIdentifier("part_of");
		final OWLObjectProperty transports = g.getOWLObjectPropertyByIdentifier("transports_or_maintains_localization_of");

		List<LinkPattern> patterns = new ArrayList<LinkPattern>(2);
		patterns.add(new LinkPattern(ta, t, transports, partOf));
		patterns.add(new LinkPattern(tmta, tmt, transports, partOf));

		OWLDataFactory factory = g.getDataFactory();
		OWLAnnotationProperty property = factory.getOWLAnnotationProperty(annotationIRI);
		OWLAnnotation sourceAnnotation = factory.getOWLAnnotation(property, factory.getOWLLiteral(goRef));

		LinkMaker maker = new LinkMaker(g, reasoner);
		LinkMakerResult result = maker.makeLinks(patterns, sourceAnnotation, false);

		LOG.info("Predictions size: "+result.getPredictions().size());
		OWLPrettyPrinter pp = getPrettyPrinter();
		for (OWLAxiom ax : result.getPredictions()) {
			LOG.info(pp.render(ax));
		}
		LOG.info("Existing size: "+result.getExisiting().size());
		LOG.info("Modified size: "+result.getModified().size());

		OWLOntologyManager manager = g.getManager();
		manager.removeAxioms(g.getSourceOntology(), result.getExisiting());
		manager.addAxioms(g.getSourceOntology(), result.getModified());
		manager.addAxioms(g.getSourceOntology(), result.getPredictions());

		manager.saveOntology(g.getSourceOntology(), targetFileIRI);
	}

	@CLIMethod("--remove-redundant-svfs")
	public void removeRedundantSVFs(Opts opts) throws Exception {
		opts.info("", "removes redundant existentials: X R Some C, X R Some D, C SubClassOf* D");
		if (g == null) {
			LOG.error("No current ontology loaded");
			exit(-1);
		}
		if (reasoner == null) {
			LOG.error("No reasoner available for the current ontology");
			exit(-1);
		}
		while (opts.hasOpts()) {
			if (opts.nextEq("--report-file")) {
				//reportFile = opts.nextOpt();
			}
			else {
				break;
			}
		}
		Set<OWLSubClassOfAxiom> axioms = g.getSourceOntology().getAxioms(AxiomType.SUBCLASS_OF);
		Set<OWLSubClassOfAxiom> rmAxioms = new HashSet<OWLSubClassOfAxiom>();
		LOG.info("Candidates: " + axioms.size());
		for (OWLSubClassOfAxiom axiom : axioms) {
			if (axiom.getSubClass().isAnonymous())
				continue;
			OWLClass subClass = (OWLClass)axiom.getSubClass();
			if (axiom.getSuperClass() instanceof OWLObjectSomeValuesFrom) {
				//LOG.info("  TESTING " + axiom);
				OWLObjectSomeValuesFrom svf = ((OWLObjectSomeValuesFrom)axiom.getSuperClass());
				for (OWLSubClassOfAxiom msAxiom : g.getSourceOntology().getSubClassAxiomsForSubClass(subClass)) {
					if (msAxiom.getSuperClass() instanceof OWLObjectSomeValuesFrom) {
						OWLObjectSomeValuesFrom mssvf = ((OWLObjectSomeValuesFrom)msAxiom.getSuperClass());
						if (mssvf.getProperty().equals(svf.getProperty())) {
							if (!svf.getFiller().isAnonymous()) {
								if (reasoner.getSuperClasses(mssvf.getFiller(), false).
										containsEntity((OWLClass) svf.getFiller())) {
									LOG.info(axiom+" IS_REDUNDANT: "+mssvf.getFiller() +
											" more-specific-than "+svf.getFiller());
									rmAxioms.add(axiom);
								}
							}
						}
					}
					else if (!msAxiom.getSuperClass().isAnonymous()) {
						// TODO
					}
				}
			}
		}
		g.getManager().removeAxioms(g.getSourceOntology(), rmAxioms);
	}

	@CLIMethod("--remove-redundant-inferred-svfs")
	public void removeRedundantInferredSVFs(Opts opts) throws Exception {
		opts.info("", "removes redundant existentials using extended reasoner");
		if (g == null) {
			LOG.error("No current ontology loaded");
			exit(-1);
		}
		if (reasoner == null) {
			LOG.error("No reasoner available for the current ontology");
			exit(-1);
		}
		if (!(reasoner instanceof OWLExtendedReasoner)) {
			LOG.error("Reasoner is not extended");
			exit(-1);
		}
		OWLExtendedReasoner exr = (OWLExtendedReasoner)reasoner;
		while (opts.hasOpts()) {
			if (opts.nextEq("--report-file")) {
				//reportFile = opts.nextOpt();
			}
			else {
				break;
			}
		}
		OWLPrettyPrinter owlpp = new OWLPrettyPrinter(g);
		Set<OWLSubClassOfAxiom> axioms = g.getSourceOntology().getAxioms(AxiomType.SUBCLASS_OF);
		Set<OWLSubClassOfAxiom> rmAxioms = new HashSet<OWLSubClassOfAxiom>();
		LOG.info("Candidates: " + axioms.size());
		int n = 0;
		for (OWLSubClassOfAxiom axiom : axioms) {
			n++;
			if (n % 100 == 0) {
				LOG.info("Testing axiom #" +n);
			}
			if (axiom.getSubClass().isAnonymous())
				continue;
			OWLClass subClass = (OWLClass)axiom.getSubClass();
			if (axiom.getSuperClass() instanceof OWLObjectSomeValuesFrom) {
				OWLObjectSomeValuesFrom svf = ((OWLObjectSomeValuesFrom)axiom.getSuperClass());
				if (svf.getProperty().isAnonymous())
					continue;
				if (svf.getFiller().isAnonymous())
					continue;
				OWLObjectProperty p = (OWLObjectProperty)svf.getProperty();
				Set<OWLClass> directParents = exr.getSuperClassesOver(subClass, p, true);
				if (!directParents.contains(svf.getFiller())) {
					rmAxioms.add(axiom);
					LOG.info("  IS_REDUNDANT: "+owlpp.render(axiom)+" as filler not in "+directParents);
					for (OWLClass dp : directParents) {
						LOG.info("DIRECT_PARENT_OVER "+owlpp.render(p)+" "+owlpp.render(dp));
					}
				}
			}
		}
		g.getManager().removeAxioms(g.getSourceOntology(), rmAxioms);
	}

	@CLIMethod("--remove-redundant-inferred-super-classes")
	public void removeRedundantInferredSuperClassAxioms(Opts opts) throws Exception {
		String reportFile = null;
		if (g == null) {
			LOG.error("No current ontology loaded");
			exit(-1);
		}
		if (reasoner == null) {
			LOG.error("No reasoner available for the current ontology");
			exit(-1);
		}
		while (opts.hasOpts()) {
			if (opts.nextEq("--report-file")) {
				reportFile = opts.nextOpt();
			}
			else {
				break;
			}
		}
		LOG.info("Start finding and removing redundant and previously inferred super classes");
		Map<OWLClass, Set<RedundantAxiom>> allRedundantAxioms = RedundantInferences.removeRedundantSubClassAxioms(g.getSourceOntology(), reasoner);

		if (reportFile == null) {
			LOG.warn("No report file available, skipping report.");
		}
		else {
			BufferedWriter writer = new BufferedWriter(new FileWriter(reportFile));
			try {
				List<OWLClass> sortedClasses = new ArrayList<OWLClass>(allRedundantAxioms.keySet());
				Collections.sort(sortedClasses);
				for (OWLClass cls : sortedClasses) {
					Set<RedundantAxiom> redundants = allRedundantAxioms.get(cls);
					List<OWLClass> superClasses = new ArrayList<OWLClass>(redundants.size());
					Map<OWLClass, Set<OWLClass>> intermediateClasses = new HashMap<OWLClass, Set<OWLClass>>();
					for(RedundantAxiom redundant : redundants) {
						OWLSubClassOfAxiom axiom = redundant.getAxiom();
						OWLClass superClass = axiom.getSuperClass().asOWLClass();
						superClasses.add(superClass);
						intermediateClasses.put(superClass, redundant.getMoreSpecific());
					}
					Collections.sort(superClasses);
					for (OWLClass superClass : superClasses) {
						String subClassId = g.getIdentifier(cls);
						String subClassLabel = g.getLabel(cls);
						String superClassId = g.getIdentifier(superClass);
						String superClassLabel = g.getLabel(superClass);
						writer.append("REMOVE").append('\t').append(subClassId).append('\t');
						if (subClassLabel != null) {
							writer.append('\'').append(subClassLabel).append('\'');
						}
						writer.append('\t').append(superClassId).append('\t');
						if (superClassLabel != null) {
							writer.append('\'').append(superClassLabel).append('\'');
						}
						writer.append('\t').append("MORE SPECIFIC: ");
						for(OWLClass moreSpecific : intermediateClasses.get(superClass)) {
							String moreSpecificId = g.getIdentifier(moreSpecific);
							String moreSpecificLabel = g.getLabel(moreSpecific);
							writer.append('\t').append(moreSpecificId).append('\t');
							if (moreSpecificLabel != null) {
								writer.append('\'').append(moreSpecificLabel).append('\'');
							}
						}
						writer.append('\n');
					}
				}
			}
			finally {
				IOUtils.closeQuietly(writer);
			}
		}
	}

	@CLIMethod("--remove-subset-entities")
	public void removeSubsetEntities(Opts opts) throws Exception {
		opts.info("[SUBSET]+","Removes all classes, individuals and object properties that are in the specific subset(s)");
		List<String> subSets = opts.nextList();
		if (subSets == null || subSets.isEmpty()) {
			System.err.println("At least one subset is required for this function.");
			exit(-1);
		}
		// create annotation values to match
		Set<OWLAnnotationValue> values = new HashSet<OWLAnnotationValue>();
		OWLDataFactory f = g.getDataFactory();
		for(String subSet : subSets) {
			// subset as plain string
			values.add(f.getOWLLiteral(subSet));
			// subset as IRI
			values.add(IRI.create(Obo2OWLConstants.DEFAULT_IRI_PREFIX+"#"+subSet));
		}

		// get annotation property for subset
		OWLAnnotationProperty p = g.getAnnotationProperty(OboFormatTag.TAG_SUBSET.getTag());

		// collect all objects in the given subset
		final Set<OWLObject> entities = Mooncat.findTaggedEntities(p, values, g);
		LOG.info("Found "+entities.size()+" tagged objects.");

		if (entities.isEmpty() == false) {
			final List<RemoveAxiom> changes = Mooncat.findRelatedAxioms(entities, g);
			if (changes.isEmpty() == false) {
				LOG.info("applying changes to ontology, count: "+changes.size());
				g.getManager().applyChanges(changes);
			}
			else {
				LOG.info("No axioms found for removal.");
			}
		}
	}

	/**
	 * Simple helper to create a subset tag for matching entities, allows to specify exceptions
	 * 
	 * @param opts
	 * @throws Exception
	 */
	@CLIMethod("--create-subset-tags")
	public void createSubsetTags(Opts opts) throws Exception {
		opts.info("[-s|--source SOURCE] -n|--subset SUBSET_NAME -p PREFIX [-e|--exception EXCEPTION]", "Create subset tags for all classes and properties, which match the id prefix (OBO style). Specifiy exceptions to skip entities.");
		String source = null;
		String subset = null;
		String prefix = null;
		final Set<String> matchExceptions = new HashSet<String>();
		while (opts.hasOpts()) {
			if (opts.nextEq("-s|--source")) {
				source = opts.nextOpt();
			}
			else if (opts.nextEq("-n|--subset")) {
				subset = opts.nextOpt();
			}
			else if (opts.nextEq("-p|--prefix")) {
				prefix = opts.nextOpt();
			}
			else if (opts.nextEq("-e|--exception")) {
				matchExceptions.add(opts.nextOpt());
			}
			else {
				break;
			}
		}
		if (subset == null) {
			throw new RuntimeException("A subset is required.");
		}
		if (prefix == null) {
			throw new RuntimeException("A prefix is required.");
		}

		final Set<OWLEntity> signature;
		if (source != null) {
			ParserWrapper newPw = new ParserWrapper();
			newPw.addIRIMappers(pw.getIRIMappers());
			final OWLOntology sourceOntology = newPw.parse(source);
			signature = sourceOntology.getSignature(Imports.INCLUDED);
		}
		else {
			signature = new HashSet<OWLEntity>();
			for (OWLOntology o : g.getAllOntologies()) {
				signature.addAll(o.getSignature());
			}
		}
		final Set<IRI> upperLevelIRIs = new HashSet<IRI>();
		final String matchPrefix = prefix;
		for (OWLEntity owlEntity : signature) {
			owlEntity.accept(new OWLEntityVisitorAdapter(){

				@Override
				public void visit(OWLClass cls) {
					String id = Owl2Obo.getIdentifier(cls.getIRI());
					if (id.startsWith(matchPrefix) && !matchExceptions.contains(id)) {
						upperLevelIRIs.add(cls.getIRI());
					}
				}

				@Override
				public void visit(OWLObjectProperty property) {
					String id = Owl2Obo.getIdentifier(property.getIRI());
					if (id.startsWith(matchPrefix) && !matchExceptions.contains(id)) {
						upperLevelIRIs.add(property.getIRI());
					}
				}
			});
		}

		final OWLOntologyManager m = g.getManager();
		final OWLDataFactory f = g.getDataFactory();
		final OWLAnnotationProperty p = g.getAnnotationProperty(OboFormatTag.TAG_SUBSET.getTag());
		final OWLAnnotation annotation = f.getOWLAnnotation(p, IRI.create(Obo2OWLConstants.DEFAULT_IRI_PREFIX+"#"+subset));
		for (IRI iri : upperLevelIRIs) {
			OWLAnnotationAssertionAxiom ax = f.getOWLAnnotationAssertionAxiom(iri, annotation);
			m.addAxiom(g.getSourceOntology(), ax);
		}

	}


	@CLIMethod("--verify-changes")
	public void verifyChanges(Opts opts) throws Exception {
		String previousInput = null;
		String idFilterPrefix = null;
		boolean checkMissingLabels = false;
		String reportFile = null;
		while (opts.hasOpts()) {
			if (opts.nextEq("-p|--previous")) {
				previousInput = opts.nextOpt();
			}
			else if (opts.nextEq("--id-prefix-filter")) {
				idFilterPrefix = opts.nextOpt();
			}
			else if (opts.nextEq("--check-missing-labels")) {
				checkMissingLabels = true;
			}
			else if (opts.nextEq("-o|--report-file")) {
				reportFile = opts.nextOpt();
			}
			else {
				break;
			}
		}
		if (g == null) {
			LOG.error("No current ontology loaded for comparison");
			exit(-1);
		}
		else if (previousInput == null) {
			LOG.error("No previous ontology configured for comparison");
			exit(-1);
		}
		else {
			// create new parser & manager for clean load of previous ontology
			final ParserWrapper pw = new ParserWrapper();
			// use same IRI mappers as main parser
			List<OWLOntologyIRIMapper> mappers = this.pw.getIRIMappers();
			if (mappers != null) {
				for (OWLOntologyIRIMapper mapper : mappers) {
					pw.addIRIMapper(mapper);
				}
			}

			// load previous
			IRI previousIRI = IRI.create(new File(previousInput).getCanonicalFile());
			final OWLGraphWrapper previous = pw.parseToOWLGraph(previousIRI.toString());

			LOG.info("Start verifying changes.");

			// create (filtered) class ids and labels, obsolete, alt_ids
			// prev
			final Map<String, String> previousIdLabels = Maps.newHashMap();
			final Set<String> previousObsoletes = Sets.newHashSet();
			final Set<String> previousAltIds = Sets.newHashSet();
			extractClassInfo(previous, previousIdLabels, previousObsoletes,
					previousAltIds, idFilterPrefix);

			// current
			final Map<String, String> currentIdLabels = Maps.newHashMap();
			final Set<String> currentObsoletes = Sets.newHashSet();
			final Set<String> currentAltIds = Sets.newHashSet();
			extractClassInfo(g, currentIdLabels, currentObsoletes,
					currentAltIds, idFilterPrefix);

			// check that all ids are also in the current ontology
			boolean hasErrors = false;
			// normal ids
			final List<String> missingIds = Lists.newArrayList();
			final Map<String, String> missingLabels = Maps.newHashMap();
			for(String previousId : previousIdLabels.keySet()) {
				if (!(currentIdLabels.containsKey(previousId)
						|| currentAltIds.contains(previousId)
						|| currentObsoletes.contains(previousId))) {
					missingIds.add(previousId);
					hasErrors = true;
				}
				else if (checkMissingLabels && currentAltIds.contains(previousId)) {
					// this id has been merged into another class
					// optional: check that all primary labels of merged terms are still in the merged term

					final OWLObject currentObject = g.getOWLObjectByAltId(previousId);
					final String currentLbl = g.getLabel(currentObject);
					final String previousLbl = previousIdLabels.get(previousId);
					if (currentLbl != null && previousLbl != null) {
						if (currentLbl.equals(previousLbl) == false) {
							// check synonyms
							List<ISynonym> synonyms = g.getOBOSynonyms(currentObject);
							boolean found = false;
							if (synonyms != null) {
								for (ISynonym synonym : synonyms) {
									if (previousLbl.equals(synonym.getLabel())) {
										found = true;
										break;
									}
								}
							}
							if (found == false) {
								hasErrors = true;
								missingLabels.put(previousId, previousLbl);
							}
						}
					}
				}
			}
			if (!missingIds.isEmpty()) {
				Collections.sort(missingIds);
			}

			// alt_ids
			final List<String> missingAltIds = Lists.newArrayList(Sets.difference(previousAltIds, currentAltIds));
			if (!missingAltIds.isEmpty()) {
				Collections.sort(missingAltIds);
				hasErrors = true;
			}

			// obsolete
			Set<String> differenceObsolete = Sets.difference(previousObsoletes, currentObsoletes);
			if (!differenceObsolete.isEmpty()) {
				// special case: obsolete ids might be resurrected as valid ids
				differenceObsolete = Sets.difference(differenceObsolete, currentIdLabels.keySet());
			}
			final List<String> missingObsoletes = Lists.newArrayList(differenceObsolete);
			if (!missingObsoletes.isEmpty()) {
				Collections.sort(missingObsoletes);
				hasErrors = true;
			}
			LOG.info("Verification finished.");

			// clean up old file in case of no errors
			if (!hasErrors && reportFile != null) {
				FileUtils.deleteQuietly(new File(reportFile));
			}
			if (hasErrors) {
				LOG.error("The verification failed with the following errors.");
				PrintWriter writer = null;
				try {
					if (reportFile != null) {
						writer = new PrintWriter(new FileWriter(reportFile));
					}
					for(String missingId : missingIds) {
						LOG.error("Missing ID: "+missingId);
						if (writer != null) {
							writer.append("MISSING-ID").append('\t').append(missingId).println();
						}
					}
					for (String missingId : missingAltIds) {
						LOG.error("Missing alternate ID: "+missingId);
						if (writer != null) {
							writer.append("MISSING-ALT_ID").append('\t').append(missingId).println();
						}
					}
					for (String missingId : missingObsoletes) {
						LOG.error("Missing obsolete ID: "+missingId);
						if (writer != null) {
							writer.append("MISSING-OBSOLETE_ID").append('\t').append(missingId).println();
						}
					}
					for (Entry<String, String> missingEntry : missingLabels.entrySet()) {
						LOG.error("Missing primary label for merged term: '"+missingEntry.getValue()+"' "+missingEntry.getKey());
						if (writer != null) {
							writer.append("MISSING-LABEL").append('\t').append(missingEntry.getValue()).append('\t').append(missingEntry.getKey()).println();
						}
					}
				}
				finally {
					IOUtils.closeQuietly(writer);
				}

				exit(-1);
			}
		}
	}


	/**
	 * @param graph
	 * @param idLabels
	 * @param obsoletes
	 * @param allAltIds
	 * @param idFilterPrefix
	 */
	private void extractClassInfo(OWLGraphWrapper graph, Map<String, String> idLabels,
			Set<String> obsoletes, Set<String> allAltIds, String idFilterPrefix) {
		for(OWLObject obj : graph.getAllOWLObjects()) {
			if (obj instanceof OWLClass) {
				String id = graph.getIdentifier(obj);
				if (idFilterPrefix != null && !id.startsWith(idFilterPrefix)) {
					continue;
				}
				List<String> altIds = graph.getAltIds(obj);
				if (altIds != null) {
					allAltIds.addAll(altIds);
				}
				boolean isObsolete = graph.isObsolete(obj);
				if (isObsolete) {
					obsoletes.add(id);
				}
				else {
					String lbl = graph.getLabel(obj);
					idLabels.put(id, lbl);
				}
			}
		}
	}

	@CLIMethod("--create-biochebi")
	public void createBioChebi(Opts opts) throws Exception {
		final String chebiPURL = "http://purl.obolibrary.org/obo/chebi.owl";
		String chebiFile = null;
		String output = null;
		String ignoredSubset = "no_conj_equiv";
		while (opts.hasOpts()) {
			if (opts.nextEq("-o|--output")) {
				output = opts.nextOpt();
			}
			else if (opts.nextEq("-c|--chebi-file")) {
				chebiFile = opts.nextOpt();
			}
			else if (opts.nextEq("-i|--ignored-subset")) {
				ignoredSubset = opts.nextOpt();
			}
			else {
				break;
			}
		}
		if (chebiFile != null) {
			File inputFile = new File(chebiFile);
			OWLOntology chebiOWL = pw.parse(IRI.create(inputFile).toString());
			// sanity check:
			// check that the purl is the expected one
			boolean hasOntologyId = false;
			OWLOntologyID ontologyID = chebiOWL.getOntologyID();
			if (ontologyID != null) {
				Optional<IRI> ontologyIRI = ontologyID.getOntologyIRI();
				if (ontologyIRI.isPresent()) {
					hasOntologyId = chebiPURL.equals(ontologyIRI.get().toString());
				}
			}
			if (hasOntologyId == false) {
				throw new RuntimeException("The loaded ontology file ("+chebiFile+") does not have the expected ChEBI purl: "+chebiPURL);
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

		BioChebiGenerator.createBioChebi(g, ignoredSubset);
		if (output != null) {
			OWLOntology ontology = g.getSourceOntology();
			File outFile = new File(output);
			ontology.getOWLOntologyManager().saveOntology(ontology, IRI.create(outFile));
		}
	}

	@CLIMethod("--run-obo-basic-dag-check")
	public void runDAGCheck(Opts opts) throws Exception {
		if (g != null) {
			List<List<OWLObject>> cycles = OboBasicDagCheck.findCycles(g);
			if (cycles != null && !cycles.isEmpty()) {
				OWLPrettyPrinter pp = getPrettyPrinter();
				System.err.println("Found cycles in the graph");
				for (List<OWLObject> cycle : cycles) {
					StringBuilder sb = new StringBuilder("Cycle:");
					for (OWLObject owlObject : cycle) {
						sb.append(" ");
						sb.append(pp.render(owlObject));
					}
					System.err.println(sb);
				}
			}
		}
	}

//	@CLIMethod("--rdf-to-json-ld")
//	public void rdfToJsonLd(Opts opts) throws Exception {
//		String ofn = null;
//		while (opts.hasOpts()) {
//			if (opts.nextEq("-o")) {
//				ofn = opts.nextOpt();
//				LOG.info("SAVING JSON TO: "+ofn);
//			}
//			else {
//				break;
//			}
//		}
//		File inputFile = opts.nextFile();
//		LOG.info("input rdf: "+inputFile);
//		FileInputStream s = new FileInputStream(inputFile);
//		final Model modelResult = ModelFactory.createDefaultModel().read(
//				s, "", "RDF/XML");
//		final JenaRDFParser parser = new JenaRDFParser();
//		Options jsonOpts = new Options();
//
//		final Object json = JSONLD.fromRDF(modelResult, jsonOpts , parser);
//		FileOutputStream out = new FileOutputStream(ofn);
//		String jsonStr = JSONUtils.toPrettyString(json);
//		IOUtils.write(jsonStr, out);
//	}
//
//	@CLIMethod("--json-ld-to-rdf")
//	public void jsonLdToRdf(Opts opts) throws Exception {
//		String ofn = null;
//		while (opts.hasOpts()) {
//			if (opts.nextEq("-o")) {
//				ofn = opts.nextOpt();
//			}
//			else {
//				break;
//			}
//		}
//		final JSONLDTripleCallback callback = new JenaTripleCallback();
//
//		FileInputStream s = new FileInputStream(opts.nextFile());
//		Object json = JSONUtils.fromInputStream(s);
//		final Model model = (Model) JSONLD.toRDF(json, callback);
//
//		final StringWriter w = new StringWriter();
//		model.write(w, "TURTLE");
//
//		FileOutputStream out = new FileOutputStream(ofn);
//		IOUtils.write(w.toString(), out);
//	}

	@CLIMethod("--extract-annotation-value")
	public void extractAnnotationValue(Opts opts) throws Exception {
		String delimiter = "\t";
		String idPrefix = null;
		boolean addLabel = true;
		OWLAnnotationProperty valueProperty = null;
		String output = null;

		final OWLDataFactory f = g.getDataFactory();
		final OWLAnnotationProperty rdfsLabel = f.getRDFSLabel();

		while (opts.hasOpts()) {
			if (opts.nextEq("-p|--property")) {
				String propString = opts.nextOpt();
				valueProperty  = f.getOWLAnnotationProperty(IRI.create(propString));
			}
			else if (opts.nextEq("-o|--output")) {
				output = opts.nextOpt();
			}
			else if (opts.nextEq("-d|--delimiter")) {
				delimiter = opts.nextOpt();
			}
			else if (opts.nextEq("--id-prefix")) {
				idPrefix = opts.nextOpt();
			}
			else if (opts.nextEq("--excludeLabel")) {
				addLabel = false;
			}
			else {
				break;
			}
		}
		if (output == null) {
			LOG.error("No outfile specified.");
			exit(-1);
		}
		else if (valueProperty == null) {
			LOG.error("No property specified.");
			exit(-1);
		}
		else {
			List<String> lines = new ArrayList<String>();
			final Set<OWLOntology> allOntologies = g.getAllOntologies();
			LOG.info("Extracting values for property: "+valueProperty.getIRI());
			for(OWLClass cls : g.getAllOWLClasses()) {
				final String id = g.getIdentifier(cls);
				if (idPrefix != null && !id.startsWith(idPrefix)) {
					continue;
				}
				String label = null;
				String propertyValue = null;

				Set<OWLAnnotationAssertionAxiom> allAnnotationAxioms = new HashSet<OWLAnnotationAssertionAxiom>();
				for(OWLOntology ont : allOntologies) {
					allAnnotationAxioms.addAll(ont.getAnnotationAssertionAxioms(cls.getIRI()));
				}
				for (OWLAnnotationAssertionAxiom axiom : allAnnotationAxioms) {
					OWLAnnotationProperty currentProp = axiom.getProperty();
					if (valueProperty.equals(currentProp)) {
						OWLAnnotationValue av = axiom.getValue();
						if (av instanceof OWLLiteral) {
							propertyValue = ((OWLLiteral)av).getLiteral();
						}
					}
					else if (addLabel && rdfsLabel.equals(currentProp)) {
						OWLAnnotationValue av = axiom.getValue();
						if (av instanceof OWLLiteral) {
							label = ((OWLLiteral)av).getLiteral();
						}
					}
					// stop search once the values are available
					if (propertyValue != null) {
						if(addLabel) {
							if (label != null) {
								break;
							}
						}
						else {
							break;
						}
					}
				}

				// write the information
				StringBuilder sb = new StringBuilder();
				if (addLabel) {
					if (label != null && propertyValue != null) {
						sb.append(id);
						sb.append(delimiter);
						sb.append(label);
						sb.append(delimiter);
						sb.append(propertyValue);
					}
				} else {
					if (label != null && propertyValue != null) {
						sb.append(id);
						sb.append(delimiter);
						sb.append(propertyValue);
					}
				}
				lines.add(sb.toString());
			}
			LOG.info("Finished extraction, sorting output.");
			Collections.sort(lines);

			File outputFile = new File(output).getCanonicalFile();
			LOG.info("Write extracted properties to file: "+outputFile.getPath());
			BufferedWriter writer = null;
			try {
				writer = new BufferedWriter(new FileWriter(outputFile));
				for (String line : lines) {
					writer.append(line).append('\n');
				}
			}
			finally {
				IOUtils.closeQuietly(writer);
			}
		}

	}

	/**
	 * Extract all xps ({@link OWLEquivalentClassesAxiom}) from the loaded
	 * ontology. Requires a set of roots classes to restrict the set of
	 * extracted xps.
	 * 
	 * @param opts
	 * @throws Exception
	 */
	@CLIMethod("--extract-extension-file")
	public void extractExtensionFile(Opts opts) throws Exception {
		final Set<OWLClass> rootTerms = new HashSet<OWLClass>();
		String ontologyIRI = null;
		String outputFileOwl = null;
		String outputFileObo = null;
		String versionIRI = null;
		while (opts.hasOpts()) {
			if (opts.nextEq("-id|--ontology-id")) {
				ontologyIRI = opts.nextOpt();
			}
			else if (opts.nextEq("-owl|--output-owl")) {
				outputFileOwl = opts.nextOpt();
			}
			else if (opts.nextEq("-obo|--output-obo")) {
				outputFileObo = opts.nextOpt();
			}
			else if (opts.nextEq("-v|--version")) {
				versionIRI = opts.nextOpt();
			}
			else if (opts.nextEq("-t|--term")) {
				String term = opts.nextOpt();
				OWLClass owlClass = g.getOWLClassByIdentifierNoAltIds(term);
				if (owlClass != null) {
					rootTerms.add(owlClass);
				}
				else {
					throw new RuntimeException("Could not find a class for id: "+term);
				}
			}
			else {
				break;
			}
		}
		if (rootTerms.isEmpty()) {
			throw new RuntimeException("At least one term is required for filtering");
		}
		if (ontologyIRI == null) {
			throw new RuntimeException("An ontology IRI is required.");
		}

		final OWLOntologyID newID;
		final IRI newOntologyIRI = IRI.create(ontologyIRI);
		if (versionIRI != null) {
			final IRI newVersionIRI = IRI.create(versionIRI);
			newID = new OWLOntologyID(Optional.of(newOntologyIRI), Optional.of(newVersionIRI));
		}
		else {
			newID = new OWLOntologyID(Optional.of(newOntologyIRI), Optional.<IRI>absent());
		}
		final OWLOntologyManager m = g.getManager();
		final OWLOntology work = m.createOntology(newID);


		// filter axioms
		final Set<OWLObjectProperty> usedProperties = new HashSet<OWLObjectProperty>();
		final Set<OWLAxiom> filtered = new HashSet<OWLAxiom>();

		final OWLOntology source = g.getSourceOntology();

		// get relevant equivalent class axioms
		for(OWLClass cls : source.getClassesInSignature()) {
			Set<OWLEquivalentClassesAxiom> eqAxioms = source.getEquivalentClassesAxioms(cls);
			for (OWLEquivalentClassesAxiom eqAxiom : eqAxioms) {
				if (hasFilterClass(eqAxiom, rootTerms)) {
					filtered.add(eqAxiom);
					usedProperties.addAll(eqAxiom.getObjectPropertiesInSignature());
				}
			}
		}
		// add used properties
		for (OWLObjectProperty p : usedProperties) {
			filtered.addAll(source.getDeclarationAxioms(p));
			filtered.addAll(source.getAxioms(p, Imports.EXCLUDED));
			filtered.addAll(source.getAnnotationAssertionAxioms(p.getIRI()));
		}

		// add all axioms into the ontology
		m.addAxioms(work, filtered);

		// write ontology
		// owl
		if (outputFileOwl != null) {
			OutputStream outputStream = new FileOutputStream(outputFileOwl);
			try {
				m.saveOntology(work, new RDFXMLDocumentFormat(), outputStream);
			}
			finally {
				outputStream.close();
			}
		}
		// obo
		if (outputFileObo != null) {
			Owl2Obo owl2Obo = new Owl2Obo();
			OBODoc doc = owl2Obo.convert(work);

			OBOFormatWriter writer = new OBOFormatWriter();
			BufferedWriter fileWriter = null;
			try {
				fileWriter = new BufferedWriter(new FileWriter(outputFileObo));
				NameProvider nameprovider = new OWLGraphWrapperNameProvider(g);
				writer.write(doc, fileWriter, nameprovider);
			}
			finally {
				IOUtils.closeQuietly(fileWriter);
			}
		}
	}

	/**
	 * Retain only subclass of axioms and intersection of axioms if they contain
	 * a class in it's signature of a given set of parent terms.
	 * 
	 * For example, to create the x-chemical.owl do the following steps:
	 * <ol>
	 *   <li>Load ChEBI as main ontology graph</li>
	 *   <li>(Optional) load go, recommended for OBO write</li>
	 *   <li>Setup reasoner: '--elk --init-reasoner'</li>
	 *   <li>'--filter-extension-file'</li>
	 *   <li>Load extensions file using: '-e' or '--extension-file'</li>
	 *   <li>Add required root terms: '-t' or '--term', use multiple paramteres to add multiple terms</li>
	 *   <li>Set ontology IRI for filtered file: '-id' or '--ontology-id'</li>
	 *   <li> set output files:
	 *       <ul>
	 *       	<li>OWL: '-owl|--output-owl' owl-filename</li>
	 *          <li>OBO: '-obo|--output-obo' obo-filename</li>
	 *       </ul>
	 *   </li>
	 *   <li>(Optional) set version: '-v' or '--version'</li>
	 * </ol>
	 * @param opts
	 * @throws Exception
	 */
	@CLIMethod("--filter-extension-file")
	public void filterExtensionFile(Opts opts) throws Exception {
		String extensionFile = null;
		final Set<OWLClass> rootTerms = new HashSet<OWLClass>();
		String ontologyIRI = null;
		String outputFileOwl = null;
		String outputFileObo = null;
		String versionIRI = null;
		while (opts.hasOpts()) {
			if (opts.nextEq("-e|--extension-file")) {
				extensionFile = opts.nextOpt();
			}
			else if (opts.nextEq("-id|--ontology-id")) {
				ontologyIRI = opts.nextOpt();
			}
			else if (opts.nextEq("-owl|--output-owl")) {
				outputFileOwl = opts.nextOpt();
			}
			else if (opts.nextEq("-obo|--output-obo")) {
				outputFileObo = opts.nextOpt();
			}
			else if (opts.nextEq("-v|--version")) {
				versionIRI = opts.nextOpt();
			}
			else if (opts.nextEq("-t|--term")) {
				String term = opts.nextOpt();
				OWLClass owlClass = g.getOWLClassByIdentifierNoAltIds(term);
				if (owlClass != null) {
					rootTerms.add(owlClass);
				}
				else {
					throw new RuntimeException("Could not find a class for id: "+term);
				}
			}
			else {
				break;
			}
		}
		if (extensionFile == null) {
			throw new RuntimeException("No extension file was specified.");
		}
		if (rootTerms.isEmpty()) {
			throw new RuntimeException("At least one term is required for filtering");
		}
		if (ontologyIRI == null) {
			throw new RuntimeException("An ontology IRI is required.");
		}

		// create new parser and new OWLOntologyManager
		ParserWrapper p = new ParserWrapper();
		final OWLOntology work = p.parse(extensionFile);

		// update ontology ID
		final OWLOntologyID oldId = work.getOntologyID();
		final IRI oldVersionIRI;
		if(oldId != null && oldId.getVersionIRI().isPresent()) {
			oldVersionIRI = oldId.getVersionIRI().get();
		} else {
			oldVersionIRI = null;
		}

		final OWLOntologyID newID;
		final IRI newOntologyIRI = IRI.create(ontologyIRI);
		if (versionIRI != null) {
			final IRI newVersionIRI = IRI.create(versionIRI);
			newID = new OWLOntologyID(Optional.of(newOntologyIRI), Optional.of(newVersionIRI));
		}
		else if (oldVersionIRI != null) {
			newID = new OWLOntologyID(Optional.of(newOntologyIRI), Optional.of(oldVersionIRI));
		}
		else {
			newID = new OWLOntologyID(Optional.of(newOntologyIRI), Optional.<IRI>absent());
		}

		// filter axioms
		Set<OWLAxiom> allAxioms = work.getAxioms();
		for(OWLClass cls : work.getClassesInSignature()) {
			Set<OWLClassAxiom> current = work.getAxioms(cls, Imports.EXCLUDED);
			if (hasFilterClass(current, rootTerms) == false) {
				allAxioms.removeAll(work.getDeclarationAxioms(cls));
				allAxioms.removeAll(current);
				allAxioms.removeAll(work.getAnnotationAssertionAxioms(cls.getIRI()));
			}
		}

		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology filtered = manager.createOntology(newID);
		manager.addAxioms(filtered, allAxioms);

		// write ontology
		// owl
		if (outputFileOwl != null) {
			OutputStream outputStream = new FileOutputStream(outputFileOwl);
			try {
				manager.saveOntology(filtered, new RDFXMLDocumentFormat(), outputStream);
			}
			finally {
				outputStream.close();
			}
		}
		// obo
		if (outputFileObo != null) {
			Owl2Obo owl2Obo = new Owl2Obo();
			OBODoc doc = owl2Obo.convert(filtered);

			OBOFormatWriter writer = new OBOFormatWriter();
			BufferedWriter fileWriter = null;
			try {
				fileWriter = new BufferedWriter(new FileWriter(outputFileObo));
				NameProvider nameprovider = new OWLGraphWrapperNameProvider(g);
				writer.write(doc, fileWriter, nameprovider);
			}
			finally {
				IOUtils.closeQuietly(fileWriter);
			}
		}
	}

	/**
	 * Check that there is an axiom, which use a class (in its signature) that
	 * has a ancestor in the root term set.
	 * 
	 * @param axioms set to check
	 * @param rootTerms set root of terms
	 * @return boolean
	 */
	private boolean hasFilterClass(Set<OWLClassAxiom> axioms, Set<OWLClass> rootTerms) {
		if (axioms != null && !axioms.isEmpty()) {
			for (OWLClassAxiom ax : axioms) {
				if (ax instanceof OWLEquivalentClassesAxiom) {
					Set<OWLClass> signature = ax.getClassesInSignature();
					for (OWLClass sigCls : signature) {
						NodeSet<OWLClass> superClasses = reasoner.getSuperClasses(sigCls, false);
						for(OWLClass root : rootTerms) {
							if (superClasses.containsEntity(root)) {
								return true;
							}
						}
					}
				}
			}
		}
		return false;
	}

	/**
	 * Check that there is an axiom, which use a class (in its signature) that
	 * has a ancestor in the root term set.
	 * 
	 * @param axioms set to check
	 * @param rootTerms set root of terms
	 * @return boolean
	 */
	private boolean hasFilterClass(OWLEquivalentClassesAxiom axiom, Set<OWLClass> rootTerms) {
		if (axiom != null) {
			Set<OWLClass> signature = axiom.getClassesInSignature();
			for (OWLClass sigCls : signature) {
				NodeSet<OWLClass> superClasses = reasoner.getSuperClasses(sigCls, false);
				for(OWLClass root : rootTerms) {
					if (superClasses.containsEntity(root)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	@CLIMethod("--create-slim")
	public void createSlim(Opts opts) throws Exception {
		String idResource = null;
		String outputOwl = null;
		String outputObo = null;
		String oldOwl = null;
		String oldObo = null;
		IRI ontologyIRI = null;

		// parse CLI options
		while (opts.hasOpts()) {
			if (opts.nextEq("--output-owl")) {
				outputOwl = opts.nextOpt();
			}
			else if (opts.nextEq("--output-obo")) {
				outputObo = opts.nextOpt();
			}
			else if (opts.nextEq("-i|--ids")) {
				idResource = opts.nextOpt();
			}
			else if (opts.nextEq("--old-owl")) {
				oldOwl = opts.nextOpt();
			}
			else if (opts.nextEq("--old-obo")) {
				oldObo = opts.nextOpt();
			}
			else if (opts.nextEq("--iri")) {
				String iriString = opts.nextOpt();
				ontologyIRI = IRI.create(iriString);
			}
			else {
				break;
			}
		}
		// check required parameters
		if (idResource == null) {
			throw new RuntimeException("No identifier resource specified. A list of terms is required to create a slim.");
		}
		if (outputOwl == null && outputObo == null) {
			throw new RuntimeException("No output file specified. At least one output file (obo or owl) is needed.");
		}
		if (ontologyIRI == null) {
			throw new RuntimeException("No IRI found. An ontology IRI is required.");
		}
		// set of all OWL classes required in the slim.
		Set<OWLClass> seeds = new HashSet<OWLClass>();

		// create map of alternate identifiers for fast lookup
		Map<String, OWLObject> objectsByAltId = g.getAllOWLObjectsByAltId();

		// load list of identifiers from file
		LineIterator lineIterator = FileUtils.lineIterator(new File(idResource));
		while (lineIterator.hasNext()) {
			String line = lineIterator.next();
			if (line.startsWith("#")) {
				continue;
			}
			addId(line, seeds, objectsByAltId);
		}

		// (optional) load previous slim in OWL.
		// Check that all classes are also available in the new base ontology.
		if (oldOwl != null) {
			ParserWrapper pw = new ParserWrapper();
			OWLOntologyManager tempManager = pw.getManager();
			OWLOntology oldSlim = tempManager.loadOntologyFromOntologyDocument(new File(oldOwl));
			OWLGraphWrapper oldSlimGraph = new OWLGraphWrapper(oldSlim);
			Set<OWLClass> classes = oldSlim.getClassesInSignature();
			for (OWLClass owlClass : classes) {
				boolean found = false;
				for(OWLOntology o : g.getAllOntologies()) {
					if (o.getDeclarationAxioms(owlClass).isEmpty() == false) {
						found = true;
						seeds.add(owlClass);
						break;
					}
				}
				if (!found) {
					LOG.warn("Could not find old class ("+oldSlimGraph.getIdentifier(owlClass)+") in new ontology.");
				}
			}
			oldSlimGraph.close();

		}
		// (optional) load previous slim in OBO format.
		// Check that all classes are also available in the new base ontology.
		if (oldObo != null) {
			OBOFormatParser p = new OBOFormatParser();
			OBODoc oboDoc = p.parse(new File(oldObo));
			Collection<Frame> termFrames = oboDoc.getTermFrames();
			if (termFrames != null) {
				for (Frame frame : termFrames) {
					String id = frame.getId();
					addId(id, seeds, objectsByAltId);
				}
			}
		}
		// sanity check
		if (seeds.isEmpty()) {
			throw new RuntimeException("There are no classes in the seed set for the slim generation. Id problem or empty id resource?");
		}

		// create the slim
		Mooncat mooncat = new Mooncat(g);
		OWLOntology slim = mooncat.makeMinimalSubsetOntology(seeds, ontologyIRI, true, false);
		mooncat = null;

		// write the output
		if (outputOwl != null) {
			File outFile = new File(outputOwl);
			slim.getOWLOntologyManager().saveOntology(slim, IRI.create(outFile));
		}
		if (outputObo != null) {
			Owl2Obo owl2Obo = new Owl2Obo();
			OBODoc oboDoc = owl2Obo.convert(slim);
			OBOFormatWriter w = new OBOFormatWriter();
			w.write(oboDoc, outputObo);
		}

	}

	private void addId(String id, Set<OWLClass> seeds, Map<String, OWLObject> altIds) {
		id = StringUtils.trimToNull(id);
		if (id != null) {
			// #1 check alt_ids
			OWLObject owlObject = altIds.get(id);
			if (owlObject != null && owlObject instanceof OWLClass) {
				LOG.warn("Retrieving class "+g.getIdentifier(owlObject)+" by alt_id: "+id+"\nPlease consider updating your idenitifers.");
				seeds.add((OWLClass) owlObject);
			}
			// #2 use normal code path
			OWLClass cls = g.getOWLClassByIdentifier(id);
			if (cls != null) {
				seeds.add(cls);
			}
			else {
				LOG.warn("Could not find a class for id: "+id);
			}
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
		OWLReasonerFactory reasonerFactory = createReasonerFactory(reasonerName);
		if (reasonerFactory == null) {
			System.out.println("no such reasoner: "+reasonerName);
		}
		else {
			reasoner = reasonerFactory.createReasoner(ont);
			LOG.info("Created reasoner: "+reasoner);
		}
		return reasoner;
	}

	private OWLReasonerFactory createReasonerFactory(String reasonerName) {
		OWLReasonerFactory reasonerFactory = null;
		if (reasonerName.equals("hermit")) {
			reasonerFactory = new org.semanticweb.HermiT.ReasonerFactory();
		}
		else if (reasonerName.equals("ogr")) {
			reasonerFactory = new GraphReasonerFactory();
		}
		else if (reasonerName.equals("mexr")) {
			if (reasonerFactory == null) {
				// set default to ELK
				reasonerFactory = new ElkReasonerFactory();
			}
			reasonerFactory = new ExpressionMaterializingReasonerFactory(reasonerFactory);
		}
		else if (reasonerName.equals("elk")) {
			reasonerFactory = new ElkReasonerFactory();
		}
		else if (reasonerName.equals("welk")) {
			System.out.println("The wrapping elk reasoner is deprecated, using normal elk instead");
			reasonerFactory = new ElkReasonerFactory();
		}
		return reasonerFactory;
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
					new OWLOntologyID(Optional.of(IRI.create(newURI)), Optional.<IRI>absent()));
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

}
