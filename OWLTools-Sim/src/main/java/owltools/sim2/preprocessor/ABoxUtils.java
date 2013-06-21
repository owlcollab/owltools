package owltools.sim2.preprocessor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSameIndividualAxiom;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

public class ABoxUtils {
	
	private static Logger LOG = Logger.getLogger(ABoxUtils.class);

	public static void randomizeClassAssertions(OWLOntology ont, int num) {
		Set<OWLClassAssertionAxiom> caas = new HashSet<OWLClassAssertionAxiom>();
		Set<OWLNamedIndividual> inds = ont.getIndividualsInSignature(true);
		OWLNamedIndividual[] indArr = (OWLNamedIndividual[]) inds.toArray();
		for (OWLNamedIndividual ind : inds) {
			caas.addAll( ont.getClassAssertionAxioms(ind) );
		}
		for (OWLClassAssertionAxiom caa : caas) {
			OWLIndividual randomIndividual = null;
			ont.getOWLOntologyManager().getOWLDataFactory().getOWLClassAssertionAxiom(caa.getClassExpression(), 
					randomIndividual);
		}
		ont.getOWLOntologyManager().removeAxioms(ont, caas);
	}

	public static void mapClassAssertionsUp(OWLOntology ont, OWLReasoner reasoner, Set<OWLClass> targetClasses, Set<OWLClass> inputClasses) {
		Set<OWLAxiom> rmAxioms = new HashSet<OWLAxiom>();
		Set<OWLAxiom> newAxioms = new HashSet<OWLAxiom>();
		Map<OWLClass, Set<OWLClass>> cmap = new HashMap<OWLClass, Set<OWLClass>>();
		LOG.info("Target classes: "+targetClasses.size());
		int n=0;
		for (OWLClassAssertionAxiom ca : ont.getAxioms(AxiomType.CLASS_ASSERTION)) {
			if (ca.getClassExpression().isAnonymous())
				continue;
			OWLClass c = (OWLClass) ca.getClassExpression();
			n++;
			if (inputClasses == null || inputClasses.contains(c)) {
				if (targetClasses.contains(c))
					continue;
				Set<OWLClass> supers;
				if (cmap.containsKey(c)) {
					supers = cmap.get(c);
				}
				else {
					supers = reasoner.getSuperClasses(c, false).getFlattened();
					supers.addAll(reasoner.getEquivalentClasses(c).getEntities());
					LOG.info(c+" has #supers: "+supers.size());
					LOG.info(c + " ===> "+supers);
					supers.retainAll(targetClasses);
					LOG.info(c+" has #supersInTargetSet: "+supers.size());
					supers = getNonRedundant(supers, reasoner);
					//LOG.info(c+" has #NRsupers: "+supers.size());
					//LOG.info(c + " ===> "+supers);
					cmap.put(c, supers);
					if (supers.size() > 0) {
						LOG.info(c + " ===> "+supers);
					}
				}
				rmAxioms.add(ca);
				OWLIndividual individual = ca.getIndividual();
				for (OWLClass sc : supers) {
					newAxioms.add(ont.getOWLOntologyManager().getOWLDataFactory().getOWLClassAssertionAxiom(sc, individual));
				}
			}
		}
		LOG.info("named class assertions (start): "+n);
		LOG.info("rm:"+rmAxioms.size());
		LOG.info("new:"+newAxioms.size());
		ont.getOWLOntologyManager().removeAxioms(ont, rmAxioms);
		ont.getOWLOntologyManager().addAxioms(ont, newAxioms);
	}

	private static Set<OWLClass> getNonRedundant(Set<OWLClass> cs,
			OWLReasoner reasoner) {
		Set<OWLClass> nrcs = new HashSet<OWLClass>(cs);
		for (OWLClass c : cs) {
			nrcs.removeAll(reasoner.getSuperClasses(c, false).getFlattened());
		}
		return nrcs;
	}

	/**
	 * generates SubClassOf axioms from ClassAssertion axioms
	 * 
	 * Note that property assertions are currently ignored
	 * 
	 * @param srcOnt
	 * @throws OWLOntologyCreationException 
	 */
	public static void translateABoxToTBox(OWLOntology srcOnt) throws OWLOntologyCreationException {
		Set<OWLAxiom> axs = new HashSet<OWLAxiom>();
		Set<OWLAxiom> rmAxioms = new HashSet<OWLAxiom>();
		OWLOntologyManager owlOntologyManager = srcOnt.getOWLOntologyManager();
		OWLDataFactory owlDataFactory = owlOntologyManager.getOWLDataFactory();
		for (OWLNamedIndividual i : srcOnt.getIndividualsInSignature()) {
			rmAxioms.add(owlDataFactory.getOWLDeclarationAxiom(i));
			OWLClass c = owlDataFactory.getOWLClass(i.getIRI()); // pun
			axs.add(owlDataFactory.getOWLDeclarationAxiom(c));
			
			// ClassAssertion -> SubClass
			for (OWLClassExpression ce : i.getTypes(srcOnt)) {
			}
			for (OWLClassAssertionAxiom ax : srcOnt.getClassAssertionAxioms(i)) {
				rmAxioms.add(ax);
				axs.add(owlDataFactory.getOWLSubClassOfAxiom(c, ax.getClassExpression()));
			}
			for (OWLObjectPropertyAssertionAxiom ax : srcOnt.getObjectPropertyAssertionAxioms(i)) {
				if (ax.getObject().isAnonymous()) {
					LOG.error("anon individuals not supported");
					continue;
				}
				rmAxioms.add(ax);
				OWLClass ce = owlDataFactory.getOWLClass(((OWLNamedObject) ax.getObject()).getIRI());
				axs.add(owlDataFactory.getOWLSubClassOfAxiom(c,
						owlDataFactory.getOWLObjectSomeValuesFrom(ax.getProperty(), ce)));
			}
		}
		for (OWLSameIndividualAxiom ax : srcOnt.getAxioms(AxiomType.SAME_INDIVIDUAL)) {
			rmAxioms.add(ax);
			Set<OWLClassExpression> xs = new HashSet<OWLClassExpression>();
			for (OWLIndividual i : ax.getIndividuals()) {
				if (i.isAnonymous()){
					LOG.error("anon individuals not supported");
					continue;
				}
				xs.add(owlDataFactory.getOWLClass(((OWLNamedObject) i).getIRI()));
			}
			axs.add(owlDataFactory.getOWLEquivalentClassesAxiom(xs));
		}

		for (OWLAxiom axiom : axs) {
			owlOntologyManager.addAxiom(srcOnt, axiom);
		}
		for (OWLAxiom axiom : rmAxioms) {
			owlOntologyManager.removeAxiom(srcOnt, axiom);
		}
		// TODO - in future communicate leaf nodes a different way
	}

	/**
	 * Creates a "fake" individual for every class. Uses punning.
	 * 
	 * @param srcOnt
	 * @throws OWLOntologyCreationException
	 */
	public static void makeDefaultIndividuals(OWLOntology srcOnt) throws OWLOntologyCreationException {
		OWLOntologyManager m = srcOnt.getOWLOntologyManager();
		OWLDataFactory df = m.getOWLDataFactory();
		for (OWLClass c : srcOnt.getClassesInSignature(true)) {
			IRI iri = c.getIRI();
			OWLNamedIndividual ind = df.getOWLNamedIndividual(iri);
			m.addAxiom(srcOnt, df.getOWLDeclarationAxiom(ind));
			m.addAxiom(srcOnt, df.getOWLClassAssertionAxiom(c, ind));
		}
	}
	
	

}
