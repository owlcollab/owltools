package owltools.mooncat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.coode.owlapi.manchesterowlsyntax.ManchesterOWLSyntaxOntologyFormat;
import org.obolibrary.obo2owl.Obo2OWLConstants;
import org.semanticweb.owlapi.io.OWLFunctionalSyntaxOntologyFormat;
import org.semanticweb.owlapi.io.OWLXMLOntologyFormat;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLLogicalAxiom;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectInverseOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyCharacteristicAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.OWLSameIndividualAxiom;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.OWLSubPropertyChainOfAxiom;
import org.semanticweb.owlapi.model.OWLTransitiveObjectPropertyAxiom;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

/**
 *
 * Translates an OWL TBox into a "shadow" representation in the ABox.
 * 
 * Each class in O is a named individual in O' (the same IRI is used - if O and O' are used together
 * this introduces punning)
 * 
 * A subset of class axioms in O are translated to ObjectPropertyAxioms in O'
 * 
 * A SubClassOf B ==> OPE(A' SubClassOf' B')
 * A SubClassOf R some B ==> OPE(A' R' B')
 * 
 * A' is a type-level counterpart of A. We re-use the same IRI (punning), but in OWL2 terms they are
 * distinct entities.
 * 
 * Here R' denotes a type-level counterpart of R.
 * "SubClassOf'" is an OWL2-DL ObjectProperty that is a type-level counterpart of the RDFS/OWL-Full rdfs:subClassOf property 
 * 
 * A subset of axioms for R are also shadowed:
 * 
 * Transitive(R) ==> Transitive(R')
 * R <- S o T ==> R'<- S' o T'
 * 
 * New axioms are introduced:
 * 
 * R' o SubClassOf' ==> R'
 * SubClassOf' o R' ==> R'
 * 
 * <h2>Other class axioms</h2>
 * 
 * Equivalence axioms currently have an incomplete translation. Currently only "genus-differentia" style
 * are translated:
 * 
 * X = G and R1 some Y1 and ..
 * ==>
 * X' sameAs GenId
 * GenId SubClassOf' G'
 * GenId R1' Y1,
 * ....
 * 
 * Where GenId is an auto-generated IRI (e.g. using a UUID) to avoid falling outside OWL2
 * 
 * <h2>Validity and completeness</h2>
 * 
 * The translation is not complete, in that not every entailment in the tbox has a shadow in the
 * abox
 * 
 * The translation is intended to be valid, but there is no proof as yet. Intuitively, it should
 * be safe to translate A SubClassOf R some B ==> A' R' B' provided we do not also carry over
 * certain other axioms. For example, we do not do anything with InversePropertiesAxioms. We do not
 * want to state "nucleus (individual) part_of (type level) cell (individual)" and infer
 * "cell (individual) has_part (type level) nucleus (individual)"
 * 
 *  The same holds for symmetry axioms
 * 
 * <h2>Motivation</h2>
 * 
 * Whilst reasoning over a "traditional" OWL representation is best for the ontology
 * development part of the lifecycle, it is not optimal for querying and reasoning with data.
 * 
 * A rich TBox can be problematic in SPARQL, triplestores, and RDF-level representations in general
 * 
 * Even in an OWL environment, answering questions such as "what are the descendants of 'finger'" and
 * receiving a part_of edge to 'limb' are not directly supported in OWL reasoners - it is necessary to
 * materialize R-some-Y classes.
 * 
 * More generally, because classes are part of the TBox and not in the domain of discourse, certain
 * kinds of queries are harder. These include queries for homologous structures, where these structures
 * are traditionally represented as classes.
 * 
 * <h2>Interpretation</h2>
 * 
 * This shadow ABox can be thought of as an RO-2005 (Smith et al) ontology, where C' denotes a universal or
 * type (considered part of the domain of discourse, hence are OWL2-DL individuals), 
 * and R' denotes a type-level relation (connecting types -
 * which in the abox-shadow are modeled as OWL individuals).
 * 
 * However, no philosophical interpretation is imposed.
 * 
 * It may be the case that a different relationship between C and C' is desired. For example, if
 * C is an anatomical structure, it may be useful to have C' be interpreted as a kind of 'pattern',
 * either the pattern of C itself, or some kind of specification of C.
 * 
 * <h2>Practicalities</h2>
 * 
 * The resulting shadow ABox should fall in the subset of OWL2 supported by Elk.
 * 
 * However, getObjectPropertyValues(ind, pe) is not yet supported (0.3.2) which limits the
 * ability to do certain kinds of queries (e.g. finger partOfTLR ?x)
 * 
 * Elk also lacks support for ObjectInverseOf, which means we cannot circumvent the above
 * by asking for ?x inverse(partOfTLR) finger.
 * 
 * 
 * <h2>TODO</h2>
 * TODO - allow customized translation such that TLRs are only created for RO-2005 temporal translations
 * 
 * @author cjm
 */
public class OWLInAboxTranslator {

	OWLOntology ontology;
	private Logger LOG = Logger.getLogger(OWLInAboxTranslator.class);
	private Set<OWLAxiom> newAxioms = new HashSet<OWLAxiom>();
	private Set<OWLObjectProperty> instanceLevelRelations = new HashSet<OWLObjectProperty>();
	private Set<OWLNamedIndividual> instances = new HashSet<OWLNamedIndividual>();


	public OWLInAboxTranslator(OWLOntology ontology) {
		super();
		this.ontology = ontology;
	}

	/**
	 * @return new ontology containing ABox shadow of original ontology
	 * @throws OWLOntologyCreationException
	 */
	public OWLOntology translate() throws OWLOntologyCreationException {
		OWLOntology targetOntology = ontology.getOWLOntologyManager().createOntology();
		return translateInto(targetOntology);
	}


	/**
	 * Adds all shadow axioms of original ontology to target
	 * 
	 * @param targetOntology
	 * @return targetOntology
	 */
	public OWLOntology translateInto(OWLOntology targetOntology) {
		for (OWLAxiom ax : ontology.getAxioms()) {
			if (ax instanceof OWLSubClassOfAxiom)
				tr((OWLSubClassOfAxiom) ax);
			else if (ax instanceof OWLEquivalentClassesAxiom)
				tr((OWLEquivalentClassesAxiom) ax);
			else if (ax instanceof OWLObjectPropertyCharacteristicAxiom)
				tr((OWLObjectPropertyCharacteristicAxiom) ax);
			else if (ax instanceof OWLSubPropertyChainOfAxiom)
				tr((OWLSubPropertyChainOfAxiom) ax);
			else if (ax instanceof OWLAnnotationAssertionAxiom)
				tr((OWLAnnotationAssertionAxiom) ax);
			else if (ax instanceof OWLClassAxiom) {
				LOG.debug("ignoring: "+ax);
			}
			else if (ax instanceof OWLDeclarationAxiom) {
				tr((OWLDeclarationAxiom)ax);
			}
			else
				add(ax);

		}

		// for each relation R used in instance-level context between two classes,
		// ensure that R' has a label "R (type level)"
		for (OWLObjectProperty p : this.instanceLevelRelations) {
			OWLObjectProperty pt = (OWLObjectProperty)trTypeLevel(p);

			// label - append 'type level'
			for (OWLAnnotationAssertionAxiom ax : ontology.getAnnotationAssertionAxioms(p.getIRI())) {
				if (ax.getProperty().isLabel()) {
					OWLLiteral lit = (OWLLiteral) ax.getValue();
					String label = lit.getLiteral() + " (type level)";
					add(getOWLDataFactory().getOWLAnnotationAssertionAxiom(ax.getProperty(), pt.getIRI(), 
							getOWLDataFactory().getOWLLiteral(label)));
				}
			}

			// property chains over 'isA'
			List<OWLObjectPropertyExpression> chain1 = new ArrayList<OWLObjectPropertyExpression>(2);
			chain1.add(pt);
			chain1.add(getSubClassOfAsTLR());
			add(getOWLDataFactory().getOWLSubPropertyChainOfAxiom(chain1, pt));

			List<OWLObjectPropertyExpression> chain2 = new ArrayList<OWLObjectPropertyExpression>(2);
			chain2.add(getSubClassOfAsTLR());
			chain2.add(pt);
			add(getOWLDataFactory().getOWLSubPropertyChainOfAxiom(chain2, pt));

			// decl
			add(getOWLDataFactory().getOWLDeclarationAxiom(pt));
		}
		// ensure "is_a" is declared transitive
		add(getOWLDataFactory().getOWLTransitiveObjectPropertyAxiom(getSubClassOfAsTLR()));

		for (OWLNamedIndividual i : instances) {
			add(getOWLDataFactory().getOWLDeclarationAxiom(i));
		}

		ontology.getOWLOntologyManager().addAxioms(targetOntology, newAxioms);
		return targetOntology;
	}


	private void tr(OWLSubPropertyChainOfAxiom ax) {
		List<OWLObjectPropertyExpression> chain = new ArrayList<OWLObjectPropertyExpression>();
		for (OWLObjectPropertyExpression p : ax.getPropertyChain()) {
			chain.add(trTypeLevel(p));
		}
		add(getOWLDataFactory().getOWLSubPropertyChainOfAxiom(chain, trTypeLevel(ax.getSuperProperty())));
	}

	private void tr(OWLDeclarationAxiom ax) {
		// do nothing: instances and tlrs declared fresh
	}

	private void tr(OWLAnnotationAssertionAxiom ax) {	
		add(ax);
	}


	/**
	 * Transitive(R) ==> Transitive(R')
	 * 
	 * @param ax
	 */
	private void tr(OWLObjectPropertyCharacteristicAxiom ax) {
		if (ax instanceof OWLTransitiveObjectPropertyAxiom) {
			OWLObjectPropertyExpression pt = trTypeLevel(ax.getProperty());
			if (pt instanceof OWLObjectProperty) {
				add(getOWLDataFactory().getOWLTransitiveObjectPropertyAxiom(pt));
			}
		}
		add(ax); // pass-through
	}

	private void tr(OWLEquivalentClassesAxiom ax) {
		// INCOMPLETE - TODO
		for (OWLClassExpression x1 : ax.getClassExpressions()) {
			if (x1.isAnonymous())
				continue;
			OWLClass c = (OWLClass) x1;
			for (OWLClassExpression x : ax.getClassExpressionsMinus(c)) {
				if (x.isAnonymous()) {
					if (x instanceof OWLObjectIntersectionOf) {
						OWLNamedIndividual i = classToIndividual((OWLClass)c);
						OWLIndividual j = this.anonClassToIndividual(x, true);
						//add(getOWLDataFactory().getOWLObjectPropertyAssertionAxiom(sameAs, i, j));
						OWLSameIndividualAxiom sameIndividualsAxiom = getOWLDataFactory().getOWLSameIndividualAxiom(i,j);
						add(sameIndividualsAxiom);
						for (OWLClassExpression y : ((OWLObjectIntersectionOf)x).getOperands()) {
							if (y.isAnonymous()) {
								OWLObjectPropertyAssertionAxiom e = trEdge(j, y);
								add(e);
							}
							else {
								OWLObjectProperty p = getSubClassOfAsTLR();
								OWLObjectPropertyAssertionAxiom e = 
									getOWLDataFactory().getOWLObjectPropertyAssertionAxiom(p, j, classToIndividual((OWLClass) y));
								add(e);

							}
						}
					}
				}

			}
		}
	}


	/**
	 * A SubClassOf B ==> A is_a B
	 * A SubClassOf R some B ==> A R' B
	 * A SubClassOf R some S some B ==> A R' _ S' B
	 * 
	 * @param ax
	 */
	private void tr(OWLSubClassOfAxiom ax) {
		OWLClassExpression subc = ax.getSubClass();
		OWLClassExpression supc = ax.getSuperClass();

		if (subc.isAnonymous())
			return;

		OWLNamedIndividual i = classToIndividual((OWLClass)subc);

		if (supc.isAnonymous()) {
			// A SubClassOf R some B ==> A R' B
			OWLObjectPropertyAssertionAxiom e = trEdge(i, supc);
			add(e);
		}
		else {
			// A SubClassOf B ==> A is_a B
			OWLNamedIndividual j = classToIndividual((OWLClass)supc);
			OWLObjectProperty p = getSubClassOfAsTLR();
			OWLObjectPropertyAssertionAxiom e = 
				getOWLDataFactory().getOWLObjectPropertyAssertionAxiom(p, i, j);
			add(e);
		}	
	}

	/**
	 * @param i - source
	 * @param supc - target expression (e.g. R some B)
	 * @return OWLObjectPropertyAssertionAxiom or null
	 */
	private OWLObjectPropertyAssertionAxiom trEdge(OWLIndividual i, OWLClassExpression supc) {
		if (supc instanceof OWLObjectSomeValuesFrom) {
			OWLObjectSomeValuesFrom svf = (OWLObjectSomeValuesFrom)supc;
			OWLObjectPropertyExpression p = trTypeLevel(svf.getProperty());
			OWLIndividual j;
			if (svf.getFiller().isAnonymous()) {
				j = anonClassToIndividual(svf.getFiller());
				add(trEdge(j, svf.getFiller()));
			}
			else {
				j = classToIndividual((OWLClass)svf.getFiller());
			}

			OWLObjectPropertyAssertionAxiom e = getOWLDataFactory().getOWLObjectPropertyAssertionAxiom(p, i, j); 
			return e;
		}
		return null;

	}

	/**
	 * @param p
	 * @return type-level form of relation
	 */
	private OWLObjectPropertyExpression trTypeLevel(
			OWLObjectPropertyExpression p) {
		if (p instanceof OWLObjectInverseOf) {
			OWLObjectPropertyExpression p2 = trTypeLevel(((OWLObjectInverseOf)p).getInverse());
			return getOWLDataFactory().getOWLObjectInverseOf(p2);
		}
		else {
			instanceLevelRelations.add((OWLObjectProperty)p);
			IRI iri = ((OWLObjectProperty) p).getIRI();
			return trTypeLevel(iri);
		}
	}

	private OWLObjectPropertyExpression trTypeLevel(IRI iri) {
		return getOWLDataFactory().getOWLObjectProperty(IRI.create(iri.toString()+"TLR"));
	}

	/**
	 * Cognate of "is_a" relation as defined in RO-2005
	 * @return
	 */
	private OWLObjectProperty getSubClassOfAsTLR() {
		//return  getOWLDataFactory().getOWLObjectProperty(IRI.create("http:x.org/is_a"));
		return (OWLObjectProperty) trTypeLevel(OWLRDFVocabulary.RDFS_SUBCLASS_OF.getIRI());
	}


	private OWLIndividual anonClassToIndividual(OWLClassExpression owlClassExpression) {
		return anonClassToIndividual(owlClassExpression, false);
	}


	/**
	 * Translates an anonymous class expression C to an ABox shadow representation C'
	 * 
	 * If isMaterialize is true, a IRI is created for C' - otherwise it is anonymous
	 * 
	 * @param owlClassExpression
	 * @param isMaterialize
	 * @return ABox shadow of input class
	 */
	private OWLIndividual anonClassToIndividual(OWLClassExpression owlClassExpression, boolean isMaterialize) {
		if (isMaterialize) {
			UUID uuid = UUID.randomUUID();
			//UUID uuid = UUID.fromString(owlClassExpression.toString());
			OWLNamedIndividual i = getOWLDataFactory().getOWLNamedIndividual(IRI.create("urn:uuid:"+uuid.toString()));
			instances.add(i);
			return i;
		}
		else 
			return getOWLDataFactory().getOWLAnonymousIndividual();
	}


	private OWLNamedIndividual classToIndividual(OWLClass c) {
		OWLNamedIndividual i = getOWLDataFactory().getOWLNamedIndividual(c.getIRI());
		instances.add(i);
		return i;
	}




	private OWLDataFactory getOWLDataFactory() {
		return ontology.getOWLOntologyManager().getOWLDataFactory();
	}


	private void add(OWLAxiom ax) {
		if (ax != null) {
			newAxioms.add(ax);
		}

	}

}
