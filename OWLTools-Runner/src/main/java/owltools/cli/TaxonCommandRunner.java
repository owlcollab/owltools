package owltools.cli;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

import owltools.cli.tools.CLIMethod;
import owltools.gaf.inference.ClassTaxonMatrix;
import owltools.gaf.inference.TaxonConstraintsEngine;
import owltools.graph.OWLGraphEdge;
import owltools.io.OWLPrettyPrinter;

/**
 * Command-line module for taxon constraints.
 */
public class TaxonCommandRunner extends GafCommandRunner {

	private static final Logger LOG = Logger.getLogger(TaxonCommandRunner.class);

	@CLIMethod("--make-class-taxon-matrix")
	public void makeClassTaxonMatrix(Opts opts) throws Exception {
		opts.info("[-o|--output OUTPUT-FILE] [--query-taxa QUERY_TAXA_ONTOLOGY_IRI] [TAXON ...]", 
				"Specifiy relevant taxa either as list or as separate taxon ontology (Load via an IRI)" +
						"\nOptional parameter: OUTPUT-FILE (if not specified system out is used)" +
				"\nHINT: To create a class taxon load first the ontology with merged taxa.");

		Set<OWLClass> taxa = new HashSet<OWLClass>();
		File outputFile = null;

		while (opts.hasArgs()) {
			if (opts.nextEq("-o") || opts.nextEq("--output")) {
				outputFile = new File(opts.nextOpt());
			}
			else if(opts.nextEq("--query-taxa")) {
				OWLOntology queryTaxaOntology = pw.parse(opts.nextOpt());
				taxa.addAll(queryTaxaOntology.getClassesInSignature());
			}
			else if (opts.hasOpts()) {
				break;
			}
			else {
				taxa.add((OWLClass)resolveEntity(opts));
			}
		}

		if (taxa.isEmpty()) {
			LOG.warn("No taxa for matrix selected");
			return;
		}

		// set output writer
		final BufferedWriter writer;
		if (outputFile == null) {
			writer = new BufferedWriter(new PrintWriter(System.out));
		}
		else {
			writer = new BufferedWriter(new FileWriter(outputFile));
		}

		// create matrix
		ClassTaxonMatrix matrix = ClassTaxonMatrix.create(g, g.getSourceOntology().getClassesInSignature(), taxa);

		// write matrix
		ClassTaxonMatrix.write(matrix, g, writer);

		writer.close();
	}

	@CLIMethod("--make-taxon-set")
	public void makeTaxonSet(Opts opts) throws Exception {
		opts.info("[-s] TAXON","Lists all classes that are applicable for a specified taxon");
		String idspace = null;
		if (opts.nextEq("-s"))
			idspace = opts.nextOpt();
		OWLPrettyPrinter owlpp = getPrettyPrinter();
		TaxonConstraintsEngine tce = new TaxonConstraintsEngine(g);
		OWLClass tax = (OWLClass)this.resolveEntity(opts);
		Set<OWLObject> taxAncs = g.getAncestorsReflexive(tax);
		LOG.info("Tax ancs: "+taxAncs);
		Set<OWLClass> taxSet = new HashSet<OWLClass>();
		for (OWLClass c : g.getSourceOntology().getClassesInSignature()) {
			String cid = g.getIdentifier(c);
			if (idspace != null && !cid.startsWith(idspace+":"))
				continue;
			Set<OWLGraphEdge> edges = g.getOutgoingEdgesClosure(c);
			boolean isExcluded = !tce.isClassApplicable(c, tax, edges, taxAncs);
			/*
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
			 */
			if (isExcluded) {
				LOG.info("excluding: "+owlpp.render(c));
			}
			else {
				taxSet.add(c);
				System.out.println(cid);
			}
		}
	}

	@CLIMethod("--create-taxon-disjoint-over-in-taxon")
	public void createTaxonDisjointOverInTaxon(Opts opts) throws Exception {

		String outputFile = "taxslim-disjoint-over-in-taxon.owl";
		String ontologyIRI = "http://purl.obolibrary.org/obo/ncbitaxon/subsets/taxslim-disjoint-over-in-taxon.owl";
		List<String> imports = Arrays.asList("http://purl.obolibrary.org/obo/ncbitaxon/subsets/taxslim.owl");
		boolean isCopy = false;
		boolean isMerge = false;


		OWLClass root = null;
		boolean compact = false;
		boolean isSelf = false;
		while (opts.hasArgs()) {
			if (opts.nextEq("-r|--root")) {
				String s = opts.nextOpt();
				root = g.getOWLClassByIdentifier(s);
				if (root == null) {
					throw new RuntimeException("No class was found for the specified identifier: "+s);
				}
			}
			else if (opts.nextEq("-o")) {
				outputFile = opts.nextOpt();
			}
			else if (opts.nextEq("-s")) {
				opts.info("", "set if self-axioms of the form TaxN SubClassOf in_taxon some TaxN to be made");
				isSelf =true;
			}
			else if (opts.nextEq("-c|--copy")) {
				opts.info("", "set if the disjoint ontology is to be copied to the source ontology."+
						"output file will not be saved");
				isCopy = true;
			}
			else if (opts.nextEq("-m|--merge")) {
				opts.info("", "set if the disjoint ontology is to be merged into source ontology."+
						"output file will not be saved");
				isMerge = true;
				isCopy = true;
			}
			else if (opts.nextEq("--compact")) {
				compact = true;
			}
			else {
				break;
			}
		}
		if (root == null) {
			throw new RuntimeException("No root identifier specified.");
		}

		// Task: create disjoint axioms for all siblings in the slim
		// avoid functional recursion

		// create new disjoint ontology
		OWLOntologyManager m = g.getManager();
		OWLDataFactory f = m.getOWLDataFactory();
		OWLOntology disjointOntology;

		if (isMerge) {
			disjointOntology = g.getSourceOntology();
		}
		else {
			disjointOntology = m.createOntology(IRI.create(ontologyIRI));

			// setup imports
			for(String importIRI : imports) {
				OWLImportsDeclaration decl = f.getOWLImportsDeclaration(IRI.create(importIRI));
				m.applyChange(new AddImport(disjointOntology, decl));
			}
		}

		// create one property 'in taxon' to remove the dependency on ro.owl 
		final OWLObjectProperty inTaxon = f.getOWLObjectProperty(IRI.create("http://purl.obolibrary.org/obo/RO_0002162"));
		m.addAxiom(disjointOntology, f.getOWLDeclarationAxiom(inTaxon));
		OWLAxiom lblAxiom = f.getOWLAnnotationAssertionAxiom(inTaxon.getIRI(), f.getOWLAnnotation(f.getRDFSLabel(), f.getOWLLiteral("in taxon")));
		m.addAxiom(disjointOntology, lblAxiom);

		// add disjoints
		Queue<OWLClass> queue = new LinkedList<OWLClass>();
		queue.add(root);
		Set<OWLClass> done = new HashSet<OWLClass>();

		final OWLOntology ont = g.getSourceOntology();
		int axiomCount = 0;
		while (queue.isEmpty() == false) {
			OWLClass current = queue.remove();
			if (done.add(current)) {
				Set<OWLSubClassOfAxiom> axioms = ont.getSubClassAxiomsForSuperClass(current);
				Set<OWLClass> siblings = new HashSet<OWLClass>();
				for (OWLSubClassOfAxiom ax : axioms) {
					OWLClassExpression ce = ax.getSubClass();
					if (ce.isAnonymous() == false) {
						OWLClass subCls = ce.asOWLClass();
						siblings.add(subCls);
						queue.add(subCls);
					}
				}
				if (siblings.size() > 1) {
					Set<OWLAxiom> disjointAxioms = new HashSet<OWLAxiom>();
					if (compact) {
						// create compact disjoint and disjoint over never_in_taxon axioms
						disjointAxioms.add(f.getOWLDisjointClassesAxiom(siblings));
						disjointAxioms.add(createDisjoint(f, siblings, inTaxon));
					}
					else {
						// create pairwise disjoint and disjoint over never_in_taxon axioms
						for (OWLClass sibling1 : siblings) {
							for (OWLClass sibling2 : siblings) {
								if (sibling1 != sibling2) {
									disjointAxioms.add(createDisjoint(f, sibling1, sibling2, inTaxon));
									disjointAxioms.add(createDisjoint(f, sibling1, sibling2));
								}
							}
						}
					}
					m.addAxioms(disjointOntology, disjointAxioms);
					axiomCount += disjointAxioms.size();
				}
				if (isSelf) {
					OWLSubClassOfAxiom ax = f.getOWLSubClassOfAxiom(current, f.getOWLObjectSomeValuesFrom(inTaxon, current));
					m.addAxiom(disjointOntology, ax);
				}
			}
		}
		LOG.info("Created "+axiomCount+" disjoint axioms.");

		if (isCopy) {
			g.setSourceOntology(disjointOntology);
		}
		else {
			// save to file
			m.saveOntology(disjointOntology, new FileOutputStream(outputFile));
		}
	}

	/**
	 * @param f
	 * @param siblings
	 * @param inTaxon
	 * @return axiom
	 */
	protected OWLAxiom createDisjoint(OWLDataFactory f, Set<OWLClass> siblings, OWLObjectProperty inTaxon) {
		Set<OWLClassExpression> expressions = new HashSet<OWLClassExpression>();
		for (OWLClass cls : siblings) {
			expressions.add(f.getOWLObjectSomeValuesFrom(inTaxon, cls));
		}
		return f.getOWLDisjointClassesAxiom(expressions);
	}

	/**
	 * @param f
	 * @param sibling1
	 * @param sibling2
	 * @param property
	 * @return axiom
	 */
	protected OWLAxiom createDisjoint(OWLDataFactory f,	OWLClass sibling1, OWLClass sibling2, OWLObjectProperty property) {
		OWLClassExpression ce1 = f.getOWLObjectSomeValuesFrom(property, sibling1);
		OWLClassExpression ce2 = f.getOWLObjectSomeValuesFrom(property, sibling2);
		return f.getOWLDisjointClassesAxiom(ce1, ce2);
	}

	/**
	 * @param f
	 * @param sibling1
	 * @param sibling2
	 * @return axiom
	 */
	protected OWLAxiom createDisjoint(OWLDataFactory f,	OWLClass sibling1, OWLClass sibling2) {
		return f.getOWLDisjointClassesAxiom(sibling1, sibling2);
	}
}
