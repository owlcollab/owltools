package owltools;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLRestriction;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.vocab.OWLDataFactoryVocabulary;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import owltools.graph.OWLGraphWrapper;
import owltools.graph.OWLQuantifiedProperty.Quantifier;
import owltools.reasoner.PlaceholderJcelFactory;

import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;

/**
 * This class build inferred axioms of an ontology.
 * @author Shahid Manzoor
 *
 */
public class InferenceBuilder{

	protected final static Logger logger = Logger
	.getLogger(InferenceBuilder.class);


	public static final String REASONER_PELLET = "pellet";
	public static final String REASONER_HERMIT = "hermit";
	public static final String REASONER_JCEL = "jcel";

	private final OWLReasonerFactory factory;
	private volatile OWLReasoner reasoner = null;
	private OWLGraphWrapper graph;
	List<OWLAxiom> redundantAxioms = new ArrayList<OWLAxiom>();

	public InferenceBuilder(OWLGraphWrapper graph){
		this(graph, new PelletReasonerFactory());
	}

	public InferenceBuilder(OWLGraphWrapper graph, String reasonerName){
		this.graph = graph;
		// TODO decide if this should be done here, 
		// or if we want just the constructor with the factory
		if (REASONER_PELLET.equals(reasonerName)) {
			this.factory = new PelletReasonerFactory();
		}
		else if (REASONER_HERMIT.equals(reasonerName)) {
			this.factory = new Reasoner.ReasonerFactory();
		}
		else if (REASONER_JCEL.equals(reasonerName)) {
			this.factory = new PlaceholderJcelFactory();
		}
		else {
			throw new IllegalArgumentException("Unknown reasoner: "+reasonerName);
		}
	}

	public InferenceBuilder(OWLGraphWrapper graph, OWLReasonerFactory factory){
		this.factory = factory;
		this.graph = graph;
	}

	public OWLGraphWrapper getOWLGraphWrapper(){
		return this.graph;
	}

	public void setOWLGraphWrapper(OWLGraphWrapper g){
		this.reasoner = null;
		this.graph = g;
	}

	private synchronized OWLReasoner getReasoner(OWLOntology ontology){
		if(reasoner == null){
			logger.info("Creating reasoner using:"+factory.getReasonerName());
			reasoner = factory.createReasoner(ontology);
			logger.info("Created reasoner: "+reasoner.getReasonerName());
		}
		return reasoner;
	}

	public List<OWLAxiom> getRedundantAxioms() {
		return redundantAxioms;
	}

	public List<OWLAxiom> buildInferences() {
		return buildInferences(true);
	}

	/**
	 * if alwaysAssertSuperClasses then ensure
	 * that superclasses are always asserted for every equivalence
	 * axiom, except in the case where a more specific superclass is already
	 * in the set of inferences.
	 * 
	 * this is because applications - particularly obof-centered ones -
	 * ignore equivalence axioms by default
	 * 
	 * @param treatEquivalenceAxiomsAsAssertions
	 * @return
	 */
	public List<OWLAxiom> buildInferences(boolean alwaysAssertSuperClasses) {
		List<OWLAxiom> sedges = new ArrayList<OWLAxiom>();

		List<OWLAxiom> eedges = new ArrayList<OWLAxiom>();

		OWLDataFactory dataFactory = graph.getDataFactory();

		OWLOntology ontology = graph.getSourceOntology();

		reasoner = getReasoner(ontology);

		Set<OWLClass> nrClasses = new HashSet<OWLClass>();

		logger.info("Finding inferred equivalencies...");
		for (OWLClass cls : ontology.getClassesInSignature()) {

			for (OWLClassExpression ec : cls.getEquivalentClasses(ontology)) {
				//System.out.println(cls+"=EC="+ec);
				if (alwaysAssertSuperClasses) {
					if (ec instanceof OWLObjectIntersectionOf) {
						for (OWLClassExpression x : ((OWLObjectIntersectionOf)ec).getOperands()) {
							// TODO: turn into subclass axiom and add
							if (x instanceof OWLRestriction) {
								eedges.add(dataFactory.getOWLSubClassOfAxiom(cls, x));
							}
						}
					}
				}
			}

		}
		logger.info("Finding inferred superclasses...");
		for (OWLClass cls : ontology.getClassesInSignature()) {
			if (nrClasses.contains(cls))
				continue; // do not report these

			// REPORT INFERRED EQUIVALENCE BETWEEN NAMED CLASSES
			for (OWLClass ec : reasoner.getEquivalentClasses(cls)) {
				if (nrClasses.contains(ec))
					continue; // do not report these

				if (cls.equals(ec))
					continue;


				if (cls.toString().compareTo(ec.toString()) > 0) // equivalence
					// is
					// symmetric:
					// report
					// each pair
					// once


					eedges.add(dataFactory.getOWLEquivalentClassesAxiom(cls, ec));
			}

			// REPORT INFERRED SUBCLASSES NOT ALREADY ASSERTED

			NodeSet<OWLClass> scs = reasoner.getSuperClasses(cls, true);
			for (Node<OWLClass> scSet : scs) {
				for (OWLClass sc : scSet) {
					if (sc.equals(OWLDataFactoryVocabulary.OWLThing)) {
						continue; // do not report subclasses of owl:Thing
					}
					if (nrClasses.contains(sc))
						continue; 

					// we do not want to report inferred subclass links
					// if they are already asserted in the ontology
					boolean isAsserted = false;
					for (OWLClassExpression asc : cls.getSuperClasses(ontology)) {
						if (asc.equals(sc)) {
							// we don't want to report this
							isAsserted = true;
						}
					}

					if (!alwaysAssertSuperClasses) {
						// when generating obo, we do NOT want equivalence axioms treated as
						// assertions
						for (OWLClassExpression ec : cls
								.getEquivalentClasses(ontology)) {

							if (ec instanceof OWLObjectIntersectionOf) {
								OWLObjectIntersectionOf io = (OWLObjectIntersectionOf) ec;
								for (OWLClassExpression op : io.getOperands()) {
									if (op.equals(sc)) {
										isAsserted = true;
									}
								}
							}
						}
					}
					if (!isAsserted) {						
						sedges.add(dataFactory.getOWLSubClassOfAxiom(cls, sc));
					}

				}
			}
		}

		// CHECK FOR REDUNDANCY
		logger.info("Checking for redundant assertions caused by inferences");
		redundantAxioms = new ArrayList<OWLAxiom>();
		for (OWLClass cls : ontology.getClassesInSignature()) {
			Set<OWLClassExpression> supers = cls.getSuperClasses(ontology);
			for (OWLAxiom ax : sedges) {
				if (ax instanceof OWLSubClassOfAxiom) {
					OWLSubClassOfAxiom sax = (OWLSubClassOfAxiom)ax;
					if (sax.getSubClass().equals(cls)) {
						supers.add(sax.getSuperClass());
					}
				}
			}
			for (OWLClassExpression sup : supers) {
				if (sup instanceof OWLClass) {
					for (Node<OWLClass> supNode : reasoner.getSuperClasses(sup,false)) {
						for (OWLClass sup2 : supNode.getEntities()) {
							if (supers.contains(sup2)) {
								redundantAxioms.add(dataFactory.getOWLSubClassOfAxiom(cls, sup2) );
							}
						}
					}
				}
			}
		}		

		sedges.addAll(eedges);



		return sedges;

	}


	public List<String> performConsistencyChecks(){

		List<String> errors = new ArrayList<String>();

		if(graph == null){
			errors.add("The ontology is not set.");
			return errors;
		}

		OWLOntology ont = graph.getSourceOntology();
		reasoner = getReasoner(ont);
		long t1 = System.currentTimeMillis();

		logger.info("Consistency check started............");
		boolean consistent = reasoner.isConsistent();

		logger.info("Is the ontology consistent ....................." + consistent + ", " + (System.currentTimeMillis()-t1)/100);

		if(!consistent){
			errors.add("The ontology '" + graph.getOntologyId() + " ' is not consistent");
		}

		// We can easily get a list of unsatisfiable classes.  (A class is unsatisfiable if it
		// can't possibly have any instances).  Note that the getunsatisfiableClasses method
		// is really just a convenience method for obtaining the classes that are equivalent
		// to owl:Nothing.
		OWLClass nothing = graph.getDataFactory().getOWLNothing();
		Node<OWLClass> unsatisfiableClasses = reasoner.getUnsatisfiableClasses();
		if (unsatisfiableClasses.getSize() > 0) {
			for(OWLClass cls : unsatisfiableClasses.getEntities()) {
				logger.info("unsat: "+cls.getIRI());
				if (cls.equals(nothing)) {
					// nothing to see here, move along
					continue;
				}
				errors.add ("unsatisfiable: " + graph.getIdentifier(cls) + " : " + graph.getLabel(cls));
			}
		}


		return errors;

	}

	public Set<Set<OWLAxiom>> getExplaination(String c1, String c2, Quantifier quantifier){
		/*OWLAxiom ax = null;
		OWLDataFactory dataFactory = graph.getDataFactory();
		OWLClass cls1 = dataFactory.getOWLClass(IRI.create(c1));
		OWLClass cls2 = dataFactory.getOWLClass(IRI.create(c2));

		if(quantifier == Quantifier.EQUIVALENT){
			ax = dataFactory.getOWLEquivalentClassesAxiom(cls1, cls2);
		}else{
			ax = dataFactory.getOWLSubClassOfAxiom(cls1, cls2);
		}


		//graph.getManager().applyChange(new AddAxiom(graph.getSourceOntology(), ax));

		DefaultExplanationGenerator gen = new DefaultExplanationGenerator(graph.getManager(), factory, infOntology, 
				reasoner,null);


		return gen.getExplanations(ax);*/

		return null;
	}

}
