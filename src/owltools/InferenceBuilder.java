package owltools;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.vocab.OWLDataFactoryVocabulary;

import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;
import owltools.graph.OWLGraphWrapper;
import owltools.graph.OWLQuantifiedProperty.Quantifier;

/**
 * This class build inferred axioms of an ontology.
 * @author Shahid Manzoor
 *
 */
public class InferenceBuilder{


	private OWLReasoner reasoner;

	private OWLGraphWrapper graph;

	public OWLGraphWrapper getOWLGraphWrapper(){
		return this.graph;
	}

	public void setOWLGraphWrapper(OWLGraphWrapper g){
		this.reasoner = null;
		this.graph =g;
	}


	public InferenceBuilder(OWLGraphWrapper graph){
		this.graph = graph;
	}


	private OWLReasoner getReasoner(OWLOntology ontology){
		if(reasoner == null){
			PelletReasonerFactory factory = new PelletReasonerFactory();
			reasoner = factory.createReasoner(ontology);
		}

		return reasoner;
	}

	public List<OWLAxiom> buildInferences() {
		return buildInferences(false);
	}

	public List<OWLAxiom> buildInferences(boolean treatEquivalenceAxiomsAsAssertions) {
		List<OWLAxiom> sedges = new ArrayList<OWLAxiom>();

		List<OWLAxiom> eedges = new ArrayList<OWLAxiom>();

		OWLDataFactory dataFactory = graph.getDataFactory();

		OWLOntology ontology = graph.getSourceOntology();

		reasoner = getReasoner(ontology);

		Set<OWLClass> nrClasses = new HashSet<OWLClass>();

		for (OWLClass cls : ontology.getClassesInSignature()) {
			if (nrClasses.contains(cls))
				continue; // do not report these

			// REPORT INFERRED EQUIVALENCE BETWEEN NAMED CLASSES
			for (OWLClass ec : reasoner.getEquivalentClasses(cls)) {
				if (nrClasses.contains(ec))
					continue; // do not report these

				if (cls.toString().equals(ec.toString()))
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
						continue;
					}
					if (nrClasses.contains(sc))
						continue; // do not report subclasses of owl:Thing

					// we do not want to report inferred subclass links
					// if they are already asserted in the ontology
					boolean isAsserted = false;
					for (OWLClassExpression asc : cls.getSuperClasses(ontology)) {
						if (asc.equals(sc)) {
							// we don't want to report this
							isAsserted = true;
						}
					}

					if (treatEquivalenceAxiomsAsAssertions) {
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

		System.out.println("Consistency check started............");
		boolean consistent = reasoner.isConsistent();

		System.out.println("Is the ontology consistent ....................." + consistent + ", " + (System.currentTimeMillis()-t1)/100);

		if(!consistent){
			errors.add("The ontology '" + graph.getOntologyId() + " ' is not consistent");
		}

		// We can easily get a list of unsatisfiable classes.  (A class is unsatisfiable if it
		// can't possibly have any instances).  Note that the getunsatisfiableClasses method
		// is really just a convenience method for obtaining the classes that are equivalent
		// to owl:Nothing.
		Node<OWLClass> unsatisfiableClasses = reasoner.getUnsatisfiableClasses();
		if (unsatisfiableClasses.getSize() > 0) {
			for(OWLClass cls : unsatisfiableClasses) {
				System.out.println(cls.getIRI());
				if (cls.toString().endsWith(":Nothing"))
					continue;
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
