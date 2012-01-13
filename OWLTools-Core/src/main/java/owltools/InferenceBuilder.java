package owltools;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLRestriction;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.SetOntologyID;
import org.semanticweb.owlapi.profiles.OWL2ELProfile;
import org.semanticweb.owlapi.profiles.OWLProfileReport;
import org.semanticweb.owlapi.profiles.OWLProfileViolation;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

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
	public static final String REASONER_ELK = "elk";

	private final FactoryDetails reasonerFactoryDetails;
	private volatile OWLReasoner reasoner = null;
	private OWLGraphWrapper graph;
	List<OWLAxiom> redundantAxioms = new ArrayList<OWLAxiom>();
	List<OWLEquivalentClassesAxiom> equivalentNamedClassPairs = new ArrayList<OWLEquivalentClassesAxiom>();

	public InferenceBuilder(OWLGraphWrapper graph){
		this(graph, new PelletReasonerFactory(), false);
	}
	
	public InferenceBuilder(OWLGraphWrapper graph, String reasonerName){
		this(graph, reasonerName, false);
	}

	public InferenceBuilder(OWLGraphWrapper graph, String reasonerName, boolean enforceEL){
		this(graph, getFactory(reasonerName), enforceEL);
	}
	
	public InferenceBuilder(OWLGraphWrapper graph, OWLReasonerFactory factory, boolean enforceEL){
		this(graph, new FactoryDetails(factory), enforceEL);
	}
	
	private InferenceBuilder(OWLGraphWrapper graph, FactoryDetails reasonerFactoryDetails, boolean enforceEL){
		this.graph = graph;
		this.reasonerFactoryDetails = reasonerFactoryDetails;
		if (enforceEL) {
			this.graph = enforceEL(graph);
		}
	}
	
	private static FactoryDetails getFactory(String reasonerName) {
		if (REASONER_PELLET.equals(reasonerName)) {
			return new FactoryDetails(new PelletReasonerFactory());
		}
		else if (REASONER_HERMIT.equals(reasonerName)) {
			return new FactoryDetails(new Reasoner.ReasonerFactory());
		}
		else if (REASONER_JCEL.equals(reasonerName)) {
			return new FactoryDetails(new PlaceholderJcelFactory());
		}
		else if (REASONER_ELK.equals(reasonerName)) {
			return new FactoryDetails(new ElkReasonerFactory(), InferenceType.values());
		}
		throw new IllegalArgumentException("Unknown reasoner: "+reasonerName);
	}
	
	private static class FactoryDetails {
		
		final OWLReasonerFactory factory;
		final InferenceType[] precomputeInferences;
		
		FactoryDetails(OWLReasonerFactory factory, InferenceType[] precomputeInferences) {
			super();
			this.factory = factory;
			this.precomputeInferences = precomputeInferences;
		}
		
		FactoryDetails(OWLReasonerFactory factory) {
			this(factory, null);
		}
	}

	public static OWLGraphWrapper enforceEL(OWLGraphWrapper graph) {
		String origIRI = graph.getSourceOntology().getOntologyID().getOntologyIRI().toString();
		if (origIRI.endsWith(".owl")) {
			origIRI = origIRI.replace(".owl", "-el.owl");
		}
		else {
			origIRI = origIRI + "-el";
		}
		return enforceEL(graph, IRI.create(origIRI));
	}
	/**
	 * Create an ontology with EL as description logic profile. This is achieved by 
	 * removing the non-compatible axioms.
	 * 
	 * WARNING: Due to the data type restrictions of EL, all deprecation annotations 
	 * are removed in this process.
	 * 
	 * @param graph
	 * @return ontology limited to EL
	 */
	public static OWLGraphWrapper enforceEL(OWLGraphWrapper graph, IRI elOntologyIRI) {
		OWL2ELProfile profile = new OWL2ELProfile();
		OWLOntology sourceOntology = graph.getSourceOntology();
		OWLProfileReport report = profile.checkOntology(sourceOntology);
		if (!report.isInProfile()) {
			logger.info("Using el-vira to restrict "+graph.getOntologyId()+" to EL");
			OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

			// see el-vira (http://code.google.com/p/el-vira/) for groovy version
			try {
				OWLOntology infOnt = manager.createOntology(elOntologyIRI);
				
				// Remove violations
				List<OWLProfileViolation> violations = report.getViolations();
				Set<OWLAxiom> ignoreSet = new HashSet<OWLAxiom>();
				for (OWLProfileViolation violation : violations) {
					OWLAxiom axiom = violation.getAxiom();
					if (axiom!=null) {
					    ignoreSet.add(axiom);
					}
				}
				int count = 0;
				for(OWLAxiom axiom : sourceOntology.getAxioms()) {
					if (!ignoreSet.contains(axiom)) {
						manager.addAxiom(infOnt, axiom);
					}
					else {
						count += 1;
					}
				}
				logger.info("enforce EL process removed "+count+" axioms");
				return new OWLGraphWrapper(infOnt);
			} catch (OWLOntologyCreationException e) {
				logger.error("Could not create new Ontology for EL restriction", e);
				throw new RuntimeException(e);
			}
		}
		else {
			logger.info("enforce EL not required for "+graph.getOntologyId());
			return graph;
		}
	}

	public OWLGraphWrapper getOWLGraphWrapper(){
		return this.graph;
	}

	public void setOWLGraphWrapper(OWLGraphWrapper g){
		this.reasoner = null;
		this.graph = g;
	}

	public synchronized void setReasoner(OWLReasoner reasoner){
		this.reasoner = reasoner;
	}

	private synchronized OWLReasoner getReasoner(OWLOntology ontology){
		if(reasoner == null){
			OWLReasonerFactory factory = reasonerFactoryDetails.factory;
			String reasonerFactoryName = factory.getReasonerName();
			if (reasonerFactoryName == null) {
				reasonerFactoryName = factory.getClass().getSimpleName();
			}
			logger.info("Creating reasoner using: "+reasonerFactoryName);
			reasoner = factory.createReasoner(ontology);
			String reasonerName = reasoner.getReasonerName();
			if (reasonerName == null) {
				reasonerName = reasoner.getClass().getSimpleName();
			}
			logger.info("Created reasoner: "+reasonerName);
			if (reasonerFactoryDetails.precomputeInferences != null) {
				reasoner.precomputeInferences(reasonerFactoryDetails.precomputeInferences); // necessary for ELK
				logger.info("pre-computed inferences; types: "+reasonerFactoryDetails.precomputeInferences.length);
			}
		}
		return reasoner;
	}

	public List<OWLAxiom> getRedundantAxioms() {
		return redundantAxioms;
	}
	
	

	public List<OWLEquivalentClassesAxiom> getEquivalentNamedClassPairs() {
		return equivalentNamedClassPairs;
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
	 * side effects: sets redundantAxioms (@see #getRedundantAxioms())
	 * 
	 * @param alwaysAssertSuperClasses 
	 * @return inferred axioms
	 */
	public List<OWLAxiom> buildInferences(boolean alwaysAssertSuperClasses) {
		List<OWLAxiom> axiomsToAdd = new ArrayList<OWLAxiom>();

		List<OWLAxiom> equivAxiomsToAdd = new ArrayList<OWLAxiom>();

		OWLDataFactory dataFactory = graph.getDataFactory();

		OWLOntology ontology = graph.getSourceOntology();

		reasoner = getReasoner(ontology);

		Set<OWLClass> nrClasses = new HashSet<OWLClass>();

		logger.info("Finding asserted equivalencies...");
		for (OWLClass cls : ontology.getClassesInSignature()) {

			for (OWLClassExpression ec : cls.getEquivalentClasses(ontology)) {
				//System.out.println(cls+"=EC="+ec);
				if (alwaysAssertSuperClasses) {
					if (ec instanceof OWLObjectIntersectionOf) {
						for (OWLClassExpression x : ((OWLObjectIntersectionOf)ec).getOperands()) {
							// Translate equivalence axioms into weaker subClassOf axioms.
							if (x instanceof OWLRestriction) {
								// we only include restrictions - note that if the operand is
								// an OWLClass it will be inferred as a superclass (see below)
								OWLSubClassOfAxiom sca = dataFactory.getOWLSubClassOfAxiom(cls, x);
								if (!ontology.containsAxiom(sca))
									equivAxiomsToAdd.add(sca);
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
				
				if (logger.isDebugEnabled()) {
					logger.debug("Inferred Equiv: " + cls + " == " + ec);
				}
				if (ec instanceof OWLClass && !ec.equals(cls)) {
					OWLEquivalentClassesAxiom eca = graph.getDataFactory().getOWLEquivalentClassesAxiom(cls, ec);
					if (logger.isDebugEnabled()) {
						logger.info("Equivalent Named Class Pair: "+eca);
					}
					equivalentNamedClassPairs.add(eca);
				}



				if (cls.toString().compareTo(ec.toString()) > 0) // equivalence
					// is
					// symmetric:
					// report
					// each pair
					// once


					equivAxiomsToAdd.add(dataFactory.getOWLEquivalentClassesAxiom(cls, ec));
			}

			// REPORT INFERRED SUBCLASSES NOT ALREADY ASSERTED

			NodeSet<OWLClass> scs = reasoner.getSuperClasses(cls, true);
			for (Node<OWLClass> scSet : scs) {
				for (OWLClass sc : scSet) {
					if (sc.isOWLThing()) {
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
					
					// include any inferred axiom that is NOT already asserted in the ontology
					if (!isAsserted) {						
						axiomsToAdd.add(dataFactory.getOWLSubClassOfAxiom(cls, sc));
					}

				}
			}
		}

		// CHECK FOR REDUNDANCY
		logger.info("Checking for redundant assertions caused by inferences");
		redundantAxioms = new ArrayList<OWLAxiom>();
		for (OWLClass cls : ontology.getClassesInSignature()) {
			Set<OWLClassExpression> supers = cls.getSuperClasses(ontology);
			for (OWLAxiom ax : axiomsToAdd) {
				if (ax instanceof OWLSubClassOfAxiom) {
					OWLSubClassOfAxiom sax = (OWLSubClassOfAxiom)ax;
					if (sax.getSubClass().equals(cls)) {
						supers.add(sax.getSuperClass());
					}
				}
			}
			for (OWLClassExpression sup : supers) {
				if (sup instanceof OWLClass) {
					if (sup.isOWLThing()) {
						redundantAxioms.add(dataFactory.getOWLSubClassOfAxiom(cls, sup));
						continue;
					}
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

		axiomsToAdd.addAll(equivAxiomsToAdd);



		return axiomsToAdd;

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
