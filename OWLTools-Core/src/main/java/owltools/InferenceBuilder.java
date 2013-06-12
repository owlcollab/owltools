package owltools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLRestriction;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.profiles.OWL2ELProfile;
import org.semanticweb.owlapi.profiles.OWLProfileReport;
import org.semanticweb.owlapi.profiles.OWLProfileViolation;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import owltools.graph.OWLGraphWrapper;
import owltools.graph.OWLQuantifiedProperty.Quantifier;
import owltools.reasoner.PlaceholderJcelFactory;
import uk.ac.manchester.cs.jfact.JFactFactory;

/**
 * This class build inferred axioms of an ontology.
 * @author Shahid Manzoor
 *
 */
public class InferenceBuilder{

	protected final static Logger logger = Logger .getLogger(InferenceBuilder.class);

	public static final String REASONER_HERMIT = "hermit";
	public static final String REASONER_JCEL = "jcel";
	public static final String REASONER_ELK = "elk";
	public static final String REASONER_JFACT = "jfact";

	private final OWLReasonerFactory reasonerFactory;
	private volatile OWLReasoner reasoner = null;
	private OWLGraphWrapper graph;
	Set<OWLAxiom> redundantAxioms = new HashSet<OWLAxiom>();
	List<OWLEquivalentClassesAxiom> equivalentNamedClassPairs = new ArrayList<OWLEquivalentClassesAxiom>();

	public InferenceBuilder(OWLGraphWrapper graph){
		this(graph, new Reasoner.ReasonerFactory(), false);
	}
	
	public InferenceBuilder(OWLGraphWrapper graph, String reasonerName){
		this(graph, reasonerName, false);
	}

	public InferenceBuilder(OWLGraphWrapper graph, String reasonerName, boolean enforceEL){
		this(graph, getFactory(reasonerName), enforceEL);
	}
	
	public InferenceBuilder(OWLGraphWrapper graph, OWLReasonerFactory factory, boolean enforceEL){
		this.graph = graph;
		this.reasonerFactory = factory;
		if (enforceEL) {
			this.graph = enforceEL(graph);
		}
	}
	
	public static OWLReasonerFactory getFactory(String reasonerName) {
		if (REASONER_HERMIT.equals(reasonerName)) {
			return new Reasoner.ReasonerFactory();
		}
		else if (REASONER_JCEL.equals(reasonerName)) {
			return new PlaceholderJcelFactory();
		}
		else if (REASONER_ELK.equals(reasonerName)) {
			return new ElkReasonerFactory();
		}
		else if (REASONER_JFACT.equals(reasonerName)) {
			return new JFactFactory();
		}
		throw new IllegalArgumentException("Unknown reasoner: "+reasonerName);
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
	 * @param elOntologyIRI
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
		if (this.reasoner != null && this.reasoner != reasoner) {
			// dispose of the old reasoner
			this.reasoner.dispose();
		}
		this.reasoner = reasoner;
	}

	public synchronized OWLReasoner getReasoner(OWLOntology ontology){
		if(reasoner == null){
			String reasonerFactoryName = reasonerFactory.getReasonerName();
			if (reasonerFactoryName == null) {
				reasonerFactoryName = reasonerFactory.getClass().getSimpleName();
			}
			logInfo("Creating reasoner using: "+reasonerFactoryName);
			reasoner = reasonerFactory.createReasoner(ontology);
			String reasonerName = reasoner.getReasonerName();
			if (reasonerName == null) {
				reasonerName = reasoner.getClass().getSimpleName();
			}
			logInfo("Created reasoner: "+reasonerName);
		}
		return reasoner;
	}

	public Collection<OWLAxiom> getRedundantAxioms() {
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

		OWLOntology ontology = graph.getSourceOntology();
		reasoner = getReasoner(ontology);

		Inferences inferences = buildInferences(ontology, reasoner, alwaysAssertSuperClasses);
		
		equivalentNamedClassPairs = inferences.equivalentNamedClassPairs;
		redundantAxioms = inferences.redundantAxioms;
		
		return inferences.axiomsToAdd;
		

	}
	
	/**
	 * Results of building inferences.
	 */
	static class Inferences {
		List<OWLAxiom> axiomsToAdd = new ArrayList<OWLAxiom>();
		List<OWLEquivalentClassesAxiom> equivalentNamedClassPairs = new ArrayList<OWLEquivalentClassesAxiom>();
		Set<OWLAxiom> redundantAxioms = new HashSet<OWLAxiom>();
	}
	
	/**
	 * Thread safe and state-less function to generate the inferences for the given ontology and reasoner.
	 * 
	 * @param ontology
	 * @param reasoner
	 * @param alwaysAssertSuperClasses
	 * @return inferences object (never null)
	 */
	Inferences buildInferences(OWLOntology ontology, OWLReasoner reasoner, boolean alwaysAssertSuperClasses) {
		List<OWLAxiom> equivAxiomsToAdd = new ArrayList<OWLAxiom>();
		OWLDataFactory dataFactory = ontology.getOWLOntologyManager().getOWLDataFactory();
		Inferences inferences = new Inferences();
		
		logInfo("Finding asserted equivalencies...");
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
								if (hasAssertedSubClassAxiom(cls, x, ontology) == false) {
									OWLSubClassOfAxiom sca = dataFactory.getOWLSubClassOfAxiom(cls, x);
									equivAxiomsToAdd.add(sca);
								}
							}
						}
					}
				}
			}
		}
		logInfo("Finding inferred superclasses...");
		for (OWLClass cls : ontology.getClassesInSignature()) {
			if (cls.isOWLNothing() || cls.isBottomEntity() || cls.isOWLThing()) {
				continue; // do not report these
			}

			// REPORT INFERRED EQUIVALENCE BETWEEN NAMED CLASSES
			for (OWLClass ec : reasoner.getEquivalentClasses(cls)) {
				if (cls.equals(ec))
					continue;
				
				if (isDebug()) {
					logDebug("Inferred Equiv: " + cls + " == " + ec);
				}
				if (ec.equals(cls) == false) {
					OWLEquivalentClassesAxiom eca = dataFactory.getOWLEquivalentClassesAxiom(cls, ec);
					if (isDebug()) {
						logDebug("Equivalent Named Class Pair: "+eca);
					}
					inferences.equivalentNamedClassPairs.add(eca);
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
			for (OWLClass sc : scs.getFlattened()) {
				if (sc.isOWLThing()) {
					continue; // do not report subclasses of owl:Thing
				}

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
					inferences.axiomsToAdd.add(dataFactory.getOWLSubClassOfAxiom(cls, sc));
				}

			}
		}

		// CHECK FOR REDUNDANCY
		logInfo("Checking for redundant assertions caused by inferences");
		inferences.redundantAxioms = getRedundantAxioms(ontology, reasoner, dataFactory);

		inferences.axiomsToAdd.addAll(equivAxiomsToAdd);

		logInfo("Done building inferences");
		
		return inferences;
	}

	/**
	 * Check the ontology for an existing subClassOf axiom with the given sub- and superclass. 
	 * This search ignores axiom annotations (i.e. is_inferred=true).
	 * 
	 * WARNING: Do not use {@link OWLOntology#containsAxiomIgnoreAnnotations(OWLAxiom)}
	 * In the current OWL-API, this seems to be very very slow.
	 * 
	 * @param subClass
	 * @param superClass
	 * @param ontology
	 * @return true, if there is an axiom for this subClass statement.
	 */
	protected boolean hasAssertedSubClassAxiom(OWLClass subClass, OWLClassExpression superClass, OWLOntology ontology) {
		Set<OWLSubClassOfAxiom> existing = ontology.getSubClassAxiomsForSubClass(subClass);
		if (existing != null) {
			for (OWLSubClassOfAxiom sca : existing) {
				if (superClass.equals(sca.getSuperClass())) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Search for redundant axioms.
	 * 
	 * @param ontology
	 * @param reasoner
	 * @param dataFactory
	 * @return set of all redundant axioms
	 */
	protected Set<OWLAxiom> getRedundantAxioms(OWLOntology ontology, OWLReasoner reasoner, OWLDataFactory dataFactory)
	{
		Set<OWLAxiom> redundantAxioms = new HashSet<OWLAxiom>();
		for (OWLClass cls : ontology.getClassesInSignature()) {
			updateRedundant(cls, ontology, redundantAxioms, reasoner, dataFactory);
		}
		return redundantAxioms;
	}

	/**
	 * Update the set of redundant axioms for the given {@link OWLClass} cls.
	 * 
	 * @param cls
	 * @param ontology
	 * @param redundantAxioms
	 * @param reasoner
	 * @param dataFactory
	 */
	protected static void updateRedundant(OWLClass cls, OWLOntology ontology, Set<OWLAxiom> redundantAxioms, 
			OWLReasoner reasoner, OWLDataFactory dataFactory)
	{
		final OWLClass owlThing = dataFactory.getOWLThing();
		
		// get all direct super classes
		final Set<OWLClass> direct = reasoner.getSuperClasses(cls, true).getFlattened();
		direct.remove(owlThing);

		// get all super classes (includes direct ones)
		final Set<OWLClass> indirect = reasoner.getSuperClasses(cls, false).getFlattened();
		indirect.remove(owlThing);

		// remove direct super classes from all -> redundant super classes
		indirect.removeAll(direct);
		// rename
		final Set<OWLClass> redundant = indirect;

		// filter
		// subclass of axioms, which have a super class in the redundant set
		Set<OWLSubClassOfAxiom> axioms = ontology.getSubClassAxiomsForSubClass(cls);
		for (OWLSubClassOfAxiom subClassOfAxiom : axioms) {
			OWLClassExpression ce = subClassOfAxiom.getSuperClass();
			if (!ce.isAnonymous()) {
				OWLClass superClass = ce.asOWLClass();
				if (redundant.contains(superClass)) {
					redundantAxioms.add(subClassOfAxiom);
				}
			}
		}
	}


	public static class ConsistencyReport {
		
		public final List<String> errors;
		public final Set<OWLEntity> unsatisfiable;
		
		ConsistencyReport(String error) {
			this.errors = Collections.singletonList(error);
			this.unsatisfiable = null;
		}

		ConsistencyReport(List<String> errors, Set<OWLEntity> unsatisfiable) {
			this.errors = errors;
			this.unsatisfiable = unsatisfiable;
		}
		
	}
	
	public ConsistencyReport performConsistencyChecks(){

		if(graph == null){
			return new ConsistencyReport("The ontology is not set.");
		}

		OWLOntology ont = graph.getSourceOntology();
		reasoner = getReasoner(ont);
		long t1 = System.currentTimeMillis();

		logInfo("Consistency check started............");
		boolean consistent = reasoner.isConsistent();

		logInfo("Is the ontology consistent ....................." + consistent + ", " + (System.currentTimeMillis()-t1)/100);

		List<String> errors = new ArrayList<String>();
		Set<OWLEntity> unsatisfiable = new HashSet<OWLEntity>();
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
				logInfo("unsat: "+cls.getIRI());
				if (cls.equals(nothing)) {
					// nothing to see here, move along
					continue;
				}
				errors.add ("unsatisfiable: " + graph.getIdentifier(cls) + " : " + graph.getLabel(cls));
				unsatisfiable.add(cls);
			}
		}


		return new ConsistencyReport(errors, unsatisfiable);

	}
	
	public static class PotentialRedundant {
		
		final OWLClass a;
		final OWLObjectProperty p;
		final OWLClass b;
		
		final OWLAxiom axiomOne;
		final OWLAxiom axiomTwo;
		
		/**
		 * @param axiomOne
		 * @param axiomTwo
		 * @param a
		 * @param p
		 * @param b
		 */
		public PotentialRedundant(OWLAxiom axiomOne, OWLAxiom axiomTwo,
				OWLClass a, OWLObjectProperty p, OWLClass b) {
			this.axiomOne = axiomOne;
			this.axiomTwo = axiomTwo;
			this.a = a;
			this.p = p;
			this.b = b;
		}

		/**
		 * @return the axiomOne
		 */
		public OWLAxiom getAxiomOne() {
			return axiomOne;
		}
		
		/**
		 * @return the axiomTwo
		 */
		public OWLAxiom getAxiomTwo() {
			return axiomTwo;
		}

		/**
		 * @return the a
		 */
		public OWLClass getClassA() {
			return a;
		}

		/**
		 * @return the p
		 */
		public OWLObjectProperty getProperty() {
			return p;
		}

		/**
		 * @return the b
		 */
		public OWLClass getClassB() {
			return b;
		}
		
		public static Comparator<PotentialRedundant> PRINT_COMPARATOR = new Comparator<PotentialRedundant>() {

			@Override
			public int compare(PotentialRedundant o1, PotentialRedundant o2) {
				IRI p1 = o1.getProperty().getIRI();
				IRI p2 = o2.getProperty().getIRI();
				int compareTo = p1.compareTo(p2);
				if (compareTo != 0) {
					return compareTo;
				}
				IRI a1 = o1.getClassA().getIRI();
				IRI a2 = o2.getClassA().getIRI();
				compareTo = a1.compareTo(a2);
				if (compareTo != 0) {
					return compareTo;
				}
				IRI b1 = o1.getClassB().getIRI();
				IRI b2 = o2.getClassB().getIRI();
				compareTo = b1.compareTo(b2);
				return compareTo;
			}
		};
		
	}
	
	/**
	 * Check the list of axioms for potential redundant subClass axioms of type:
	 * <pre>
	 *   A SubClassOf R some B
	 *    and
	 *   A SubClassOf B
	 * </pre>
	 * 
	 * @param inferredAxioms
	 * @return list of axiom pairs
	 */
	public List<PotentialRedundant> checkPotentialRedundantSubClassAxioms(Collection<? extends OWLAxiom> inferredAxioms) {
		List<PotentialRedundant> result = new ArrayList<PotentialRedundant>();
		for(OWLAxiom axiom : inferredAxioms) {
			if (axiom instanceof OWLSubClassOfAxiom) {
				OWLSubClassOfAxiom main = (OWLSubClassOfAxiom) axiom;
				OWLClassExpression mainSuperClassCE = main.getSuperClass();
				if (mainSuperClassCE.isAnonymous()) {
					continue;
				}
				OWLClassExpression mainSubClassCE = main.getSubClass();
				if (mainSubClassCE.isAnonymous()) {
					continue;
				}
				OWLClass mainSuperClass = mainSuperClassCE.asOWLClass();
				OWLClass mainSubClass = mainSubClassCE.asOWLClass();
				Set<OWLSubClassOfAxiom> subClassAxioms = graph.getAllOWLSubClassOfAxiomsForSubClass(mainSubClass);
				if (subClassAxioms != null && subClassAxioms.size() > 1) {
					for (OWLSubClassOfAxiom current : subClassAxioms) {
						if (main == current) {
							continue;
						}
						OWLClassExpression currentSuperClass = current.getSuperClass();
						if (currentSuperClass.isAnonymous() && currentSuperClass instanceof OWLObjectSomeValuesFrom) {
							OWLObjectSomeValuesFrom someValuesFrom = (OWLObjectSomeValuesFrom) currentSuperClass;
							final OWLClassExpression filler = someValuesFrom.getFiller();
							if (mainSuperClass.equals(filler)) {
								final OWLObjectPropertyExpression property = someValuesFrom.getProperty();
								final OWLClassExpression subClass = current.getSubClass();
								result.add(new PotentialRedundant(main, current, subClass.asOWLClass(), property.asOWLObjectProperty(), filler.asOWLClass()));
							}
						}
					}
				}
			}
		}
		if (result != null && !result.isEmpty()) {
			return result;
		}
		return null;
	}
	
	
	/**
	 * Check all classes for potential redundant subClass axioms of type:
	 * <pre>
	 *   A SubClassOf R some B
	 *    and
	 *   A SubClassOf B
	 * </pre>
	 * 
	 * @return list of axiom pairs
	 */
	public List<PotentialRedundant> checkPotentialRedundantSubClassAxioms() {
		List<PotentialRedundant> result = new ArrayList<PotentialRedundant>();
		for(OWLClass cls : graph.getAllOWLClasses()) {
			Set<OWLSubClassOfAxiom> axioms = graph.getAllOWLSubClassOfAxiomsForSubClass(cls);
			if (axioms.size() > 1) {
				// only check sets with more than one axiom
				for (OWLSubClassOfAxiom main : axioms) {
					OWLClassExpression mainSuperClassCE = main.getSuperClass();
					if (mainSuperClassCE.isAnonymous()) {
						continue;
					}
					OWLClass mainSuperClass = mainSuperClassCE.asOWLClass();
					for (OWLSubClassOfAxiom current : axioms) {
						if (main == current) {
							continue;
						}
						OWLClassExpression currentSuperClass = current.getSuperClass();
						if (currentSuperClass.isAnonymous() && currentSuperClass instanceof OWLObjectSomeValuesFrom) {
							OWLObjectSomeValuesFrom someValuesFrom = (OWLObjectSomeValuesFrom) currentSuperClass;
							final OWLClassExpression filler = someValuesFrom.getFiller();
							if (mainSuperClass.equals(someValuesFrom.getFiller())) {
								final OWLObjectPropertyExpression property = someValuesFrom.getProperty();
								final OWLClassExpression subClass = current.getSubClass();
								final PotentialRedundant redundant = new PotentialRedundant(main, current, subClass.asOWLClass(), property.asOWLObjectProperty(), filler.asOWLClass());
								result.add(redundant);
							}
						}
					}
				}
			}
		}
		
		if (result != null && !result.isEmpty()) {
			return result;
		}
		return null;
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

	/**
	 * Dispose the internal reasoner
	 */
	public synchronized void dispose() {
		if (reasoner != null) {
			reasoner.dispose();
			reasoner = null;
		}
	}
	
	protected void logInfo(String msg) {
		logger.info(msg);
	}
	
	protected boolean isDebug() {
		return logger.isDebugEnabled();
	}
	
	protected void logDebug(String msg) {
		logger.debug(msg);
	}

}
