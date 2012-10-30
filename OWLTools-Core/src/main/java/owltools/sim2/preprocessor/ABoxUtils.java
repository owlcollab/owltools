package owltools.sim2.preprocessor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;

public class ABoxUtils {
	
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

}
