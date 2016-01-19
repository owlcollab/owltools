package owltools.mooncat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.graph.OWLGraphWrapper;
import owltools.util.OwlHelper;

/**
 * This utility is for making "composite" ontologies from the combination of a
 * species-specific ontology (e.g. ZFA) and a species-generic ontology (e.g.
 * Uberon or CL).
 * See: @{link http://purl.obolibrary.org/obo/uberon/references/reference_0000028} 
 * <p>
 * The goal is to avoid having a lattice of classes in the merged ontology -
 * e.g. zebrafish brain, mouse brain, fly brain, generic brain, ... -- at the
 * same time, as much of the species-specific logic should be retained, but in a
 * way that is biologically correct and logically valid. E.g. we do not want to infer
 * that all brains develop from a neural keel because the zebrafish brain does.
 * We do this by merging "duplicate" classes, but retaining species axioms as
 * "taxon GCIs".
 * <p>
 * The procedure relies on "taxonomic equivalence axioms" of the form
 * 
 * <code>
 *   zfa:brain EquivalentTo ubr:brain and part_of some tax:7954
 * </code>
 * <p>
 * These are available from URLs such as:
 * <code>
 *   http://purl.obolibrary.org/obo/uberon/bridge/uberon-bridge-to-zfa.owl
 * </code>
 * <p>
 * It uses a technique called "unfolding", whereby named classes (e.g.
 * zfa:brain) can be replaced by an equivalent class expression (e.g. ubr:brain
 * and part_of some tax:7954) whilst retaining equivalent entailments in the
 * ontology.
 * <p>
 * E.g <code>
 *  zfa:brain SubClassOf develops_from some zfa:neural keel
 *  ==>
 *  (ubr:brain and part_of some tax:7954) SubClassOf develops_from some zfa:neural keel
 *  ==>
 *  (ubr:brain and part_of some tax:7954) SubClassOf develops_from some (ubr:neural keel and part_of some tax:7954)
 * </code>
 * <p>
 * If there is no taxonomic equivalence axiom, the species class is retained.
 * <p>
 * In itself, this is fairly trivial, but the resulting ontology is not
 * necessarily easier to work with. In particular, the unfolded axioms can't be
 * represented in .obo format.
 * <p>
 * Additional procedures are performed - these maintain correctness, but may
 * lose some information - this is the tradeoff in a multi-species composite
 * ontology.
 * <p>
 * The key lossy transformation is to replace (X and part_of some T) with (X) --
 * in cases where it is safe to do so. For now, the only place this is done is
 * where the expression appears on the RHS of a SubClassOf axiom, either as the
 * sole expression, or directly within a SomeValuesFrom expression.
 * <p>
 * For example, we can replace
 * <code>
 *  (ubr:brain and part_of some tax:7954) SubClassOf develops_from some (ubr:neural keel and part_of some tax:7954)
 *  ==>
 *  (ubr:brain and part_of some tax:7954) SubClassOf develops_from some uber:neural keel
 * </code>
 * <p>
 * In obo-format this is represented as:
 * <p>
 * <code>
 * id: UBERON:nnn
 * name: brain
 * relationship: develops_from UBERON:mmmm {gci_relation="part_of",gci_filler="NCBITaxon:7954"} ! neural keel
 * </code>
 *
 * <h2>Label replacement</h2>
 * 
 * As an additional step, this tool will replace all remaining labels in classes that satisfy (part_of some TAXON) with
 * a user-configured suffixed label.
 * <p>
 * For example, the class "somite 23" from EHDAA2 may have no taxonomic equivalence axiom (we don't group
 * most serially homologous structures based purely on position). It would therefore not be unfolded by the
 * above technique. The same for the ZFA class "somite 23". To avoid confusion, these are relabeled
 * "somite 23 (zebrafish)" and "somite 23 (human)". In future we may explore the use of the obo-forundry unique
 * label here.
 * 
 * @author cjm
 * 
 */
public class SpeciesMergeUtil {

	private Logger LOG = Logger.getLogger(SpeciesMergeUtil.class);

	OWLGraphWrapper graph;

	// map from species-class to generic-class
	Map<OWLClass, OWLClass> ecmap = new HashMap<OWLClass, OWLClass>();
	Map<OWLClass, OWLClassExpression> exmap = new HashMap<OWLClass, OWLClassExpression>();
	public Set<OWLObjectProperty> includedProperties = null;
	OWLOntology ont;
	OWLOntologyManager mgr;
	OWLDataFactory fac;
	public OWLReasoner reasoner;
	public String suffix = "species specific";
	public OWLObjectProperty viewProperty;
	public OWLClass taxClass;
	Set<OWLClass> ssClasses;
	OWLClass rootSpeciesSpecificClass;


	public SpeciesMergeUtil(OWLGraphWrapper g) {
		graph = g;
	}

	/**
	 * 
	 * builds ecmap, a map between species-classes to generic-classes
	 * also populates ssClasses
	 * 
	 */
	public void createMap() {

		// create a root class for a species - e.g. "Drosophila part" -
		// then make it equivalent to an expression like "part_of some Drosophila"
		rootSpeciesSpecificClass = fac.getOWLClass(IRI.create(taxClass.getIRI()
				.toString() + "-part"));
		OWLClassExpression rx = fac.getOWLObjectSomeValuesFrom(viewProperty,
				taxClass);
		OWLEquivalentClassesAxiom qax = fac.getOWLEquivalentClassesAxiom(
				rootSpeciesSpecificClass, rx);
		mgr.addAxiom(ont, qax);
		reasoner.flush();
		LOG.info("Getting species classes via: " + rootSpeciesSpecificClass
				+ " == " + rx);

		ssClasses = reasoner.getSubClasses(rootSpeciesSpecificClass, false)
				.getFlattened();
		LOG.info("num ss Classes = " + ssClasses.size());
		mgr.removeAxiom(ont, qax);
		mgr.removeAxiom(ont,
				fac.getOWLDeclarationAxiom(rootSpeciesSpecificClass));
		// reasoner.flush();

		/*
		 * PropertyViewOntologyBuilder pvob = new
		 * PropertyViewOntologyBuilder(ont, ont, viewProperty);
		 * pvob.buildInferredViewOntology(reasoner); for (OWLEntity ve :
		 * pvob.getViewEntities()) { OWLClass vc = (OWLClass)ve; OWLClass c =
		 * pvob.getOriginalClassForViewClass(vc);
		 * reasoner.getEquivalentClasses(vc); ecmap.put(vc, c);
		 * 
		 * eqmap.put(vc, x); }
		 */
		for (OWLEquivalentClassesAxiom eca : ont.getAxioms(
				AxiomType.EQUIVALENT_CLASSES, Imports.INCLUDED)) {
			//LOG.info("TESTING ECA: " + eca);

			// Looking for: FBbt:nnn = Ubr:nnn and part_of some NCBITaxon:7997
			if (eca.getClassesInSignature().contains(taxClass)
					&& eca.getObjectPropertiesInSignature().contains(
							viewProperty)) {
				LOG.info("  CONTAINS_TAX_AND_PROP: " + eca);
				for (OWLClass c : eca.getClassesInSignature()) {
					if (!ssClasses.contains(c))
						continue;
					LOG.info("   ssC = " + c);
					for (OWLClassExpression x : eca.getClassExpressionsMinus(c)) {
						if (x instanceof OWLObjectIntersectionOf) {

							OWLObjectIntersectionOf oio = (OWLObjectIntersectionOf) x;
							for (OWLClassExpression ux : oio.getOperands()) {
								if (ux instanceof OWLClass) {
									exmap.put(c, x); // e.g. fbbt:nn = ubr:nn
									// and part_of dmel
									ecmap.put(c, (OWLClass) ux);
									LOG.info("MAP: " + c + " --> " + ux
											+ " // " + x);
								}
							}
						}
					}
				}

			}

		}
	}

	public void merge() throws UnmergeableOntologyException {

		ont = graph.getSourceOntology();
		mgr = ont.getOWLOntologyManager();
		fac = mgr.getOWLDataFactory();

		createMap(); // build ecmap

		// assume that all classes under consideration are in the direct
		// ontology (which imports generic)
		for (OWLClass c : ssClasses) {
			if (c.isBottomEntity())
				continue;
			if (!reasoner.isSatisfiable(c)) {
				throw new UnmergeableOntologyException("unsatisfiable class: "+c);
			}
			LOG.info("ssClass = " + c);

			// Do not preserve GCIs for upper-level classes
			// TODO - make this configurable
			//			if (ecmap.containsKey(c)) {
			//				if (isSkippable(ecmap.get(c))) {
			//					LOG.info("Skipping: "+c);
			//					continue;
			//				}
			//			}

			Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
			Set<OWLAxiom> newAxioms = new HashSet<OWLAxiom>();
			axioms.addAll(ont.getAxioms(c, Imports.EXCLUDED));
			axioms.addAll(ont.getAnnotationAssertionAxioms(c.getIRI()));
			for (OWLClass p : reasoner.getSuperClasses(c, true).getFlattened()) {
				axioms.add(fac.getOWLSubClassOfAxiom(c, p));
			}

			// rewrite axioms
			for (OWLAxiom axiom : axioms) {
				//LOG.info("  axiom: " + axiom);
				OWLAxiom newAxiom;
				if (axiom instanceof OWLSubClassOfAxiom)
					newAxiom = tr((OWLSubClassOfAxiom) axiom);
				else if (axiom instanceof OWLEquivalentClassesAxiom)
					newAxiom = tr((OWLEquivalentClassesAxiom) axiom);
				else if (axiom instanceof OWLAnnotationAssertionAxiom)
					newAxiom = tr(c, (OWLAnnotationAssertionAxiom) axiom);
				else
					newAxiom = null;

				if (newAxiom != null && ecmap.containsKey(c)) {
					// avoid axioms like: (ubr:brain and part_of some fly) SubClassOf multi-tissue-structure
					//  only keep rewritten axioms if they refer exclusively to high-value classes.
					//  however, non-unfolded classes will keep their axioms
					for (OWLClass sc : newAxiom.getClassesInSignature()) {
						if (isSkippable(sc)) {
							newAxiom = null;
							break;
						}
					}
				}
				if (newAxiom == null)
					continue;

				if (newAxiom != null) {
					// ignore if axiom contains root class (e.g. fly anatomical structure)
					if (!newAxiom.getClassesInSignature().contains(
							rootSpeciesSpecificClass))
						newAxioms.add(newAxiom);
				} else {
					// failed to rewrite axiom - will not be added to set
				}

			}

			mgr.removeAxioms(ont, axioms);
			mgr.addAxioms(ont, newAxioms);
		}
	}

	private OWLAxiom tr(OWLClass c, OWLAnnotationAssertionAxiom ax) {
		OWLAnnotationProperty p = ax.getProperty();
		if (!ecmap.containsKey(c)) {
			// preserve as-is, exception for labels
			if (p.isLabel()) {
				OWLLiteral lit = (OWLLiteral) ax.getValue();
				String newVal = lit.getLiteral() + " (" + suffix + ")";
				return fac.getOWLAnnotationAssertionAxiom(ax.getProperty(),
						ax.getSubject(), fac.getOWLLiteral(newVal));
			}
			return ax;

		} else {
			// the class is merged - ditch its axioms
			// (in future some may be preserved - syns?)
			// fac.getOWLAnnotationAssertionAxiom(ax.getProperty(), ecmap,
			// ax.getValue());
			return null;
		}
	}

	public OWLAxiom tr(OWLSubClassOfAxiom ax) {
		OWLClassExpression trSub = trx(ax.getSubClass(), true);
		OWLClassExpression trSuper = trx(ax.getSuperClass(), false);
		/*
		if (ecmap.containsKey(trSuper) && isSkippable(ecmap.get(trSuper))) {
			return null;
		}
		if (trSuper instanceof OWLClass && isSkippable((OWLClass) trSuper)) {
			return null;
		}
		 */
		if (trSub == null)
			return null;
		if (trSuper == null)
			return null;
		// E.g. (uber:1 and part_of fly) SubClassOf uber:1
		if (trSub.getClassesInSignature().contains(trSuper))
			return null;

		// e.g. (uber:neuron and part_of some fly) SubClassOf uber:cell -- already have uber:neuron SCA ubr>cell
		if (!trSub.equals(ax.getSubClass()) &&
				reasoner.getSuperClasses(ecmap.get(ax.getSubClass()), false).getFlattened().contains(trSuper)) {
			return null;
		}
		// e.g. (uber:forebrain and part_of some fly) SubClassOf part_of some uber:brain -- already have
		// note that reasoners won't return class expressions, check for direct
		if (!trSub.equals(ax.getSubClass())) {
			Set<OWLClass> ancs = new HashSet<OWLClass>();
			ancs.addAll(reasoner.getSuperClasses(ecmap.get(ax.getSubClass()), false).getFlattened());
			ancs.addAll(reasoner.getEquivalentClasses(ecmap.get(ax.getSubClass())).getEntities());

			for (OWLClass p : ancs) {
				for (OWLSubClassOfAxiom sca : ont.getSubClassAxiomsForSubClass(p)) {
					LOG.info("  CHECKING IF THE SAME: "+sca.getSuperClass()+" == "+trSuper);
					if (sca.getSuperClass().equals(trSuper)) {
						LOG.info("   **SAME**");
						return null;
					}
				}
			}
			if (includedProperties != null && includedProperties.size() > 0) {

				boolean ok = false;
				for (OWLObjectProperty p : ax.getObjectPropertiesInSignature()) {
					if (includedProperties.contains(p)) {
						LOG.info("Including "+ax+" due to "+p);
						ok = true;
						break;
					}
				}
				if (!ok) {
					return null;
				}
			}

		}
		return fac.getOWLSubClassOfAxiom(trSub, trSuper);
	}

	public OWLAxiom tr(OWLEquivalentClassesAxiom ax) {
		Set<OWLClassExpression> xs = new HashSet<OWLClassExpression>();
		for (OWLClassExpression x : ax.getClassExpressions()) {
			OWLClassExpression tx = trx(x, true);
			if (tx == null)
				return null;
			xs.add(tx);
		}
		return fac.getOWLEquivalentClassesAxiom(xs);
	}

	private OWLClassExpression trx(OWLClassExpression x, boolean mustBeEquiv) {
		if (!x.isAnonymous()) {
			// named class - e.g. fbbt:123

			if (mustBeEquiv) {
				// UNFOLD - e.g. to (uber:123 and part_of some fly)
				if (exmap.containsKey(x)) {
					return exmap.get(x);
				} else {
					return x;
				}
			} else {
				// get the parent class in the generic ontology
				if (exmap.containsKey(x))
					return ecmap.get(x);
				// reasoner.getSuperClasses(x, true);
				return x;
			}
		} else {
			// class expression

			// e.g. part_of some fbbt:brain
			if (x instanceof OWLObjectSomeValuesFrom) {
				OWLObjectSomeValuesFrom svf = (OWLObjectSomeValuesFrom) x;
				return fac.getOWLObjectSomeValuesFrom(svf.getProperty(),
						trx(svf.getFiller(), mustBeEquiv));
			}
		}

		return null;
	}

	public class UnmergeableOntologyException extends Exception {

		public UnmergeableOntologyException(String msg) {
			super(msg);
		}

	}

	public boolean isSkippable(OWLClass c) {
		Set<OWLAnnotation> anns = OwlHelper.getAnnotations(c, ont);
		for (OWLAnnotation ann : anns) {
			String ap = ann.getProperty().getIRI().toString();
			OWLAnnotationValue v = ann.getValue();
			if (v instanceof IRI) {
				IRI iv = (IRI)v;
				if (ap.endsWith("inSubset")) {
					// TODO - formalize this
					if (iv.toString().contains("upper_level")) {
						return true;
					}
					// this tag is used in uberon
					if (iv.toString().contains("non_informative")) {
						return true;
					}
					// hack: representation of early dev a bit problematic
					// temporary: find a way to exclude these axiomatically
					if (iv.toString().contains("early_development")) {
						return true;
					}
				}

			}
		}	
		return false;
	}

}
