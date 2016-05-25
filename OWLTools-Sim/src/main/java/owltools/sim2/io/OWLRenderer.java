package owltools.sim2.io;

import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Set;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import owltools.graph.OWLGraphWrapper;
import owltools.io.OWLPrettyPrinter;
import owltools.sim2.SimpleOwlSim.Metric;
import owltools.sim2.SimpleOwlSim.ScoreAttributePair;
import owltools.sim2.scores.AttributePairScores;
import owltools.sim2.scores.ElementPairScores;

public class OWLRenderer extends AbstractRenderer implements SimResultRenderer {


	private static NumberFormat doubleRenderer = new DecimalFormat("#.###");

	boolean isHeaderLine = true;

	private OWLOntology ontology;

	public OWLRenderer(PrintStream resultOutStream) {
		this.resultOutStream = resultOutStream;
	}

	private OWLOntologyManager getOWLOntologyManager() {
		return graph.getManager();
	}
	private OWLDataFactory getOWLDataFactory() {
		return graph.getDataFactory();
	}
	private OWLOntology getOntology() {
		if (ontology == null) {
			try {
				ontology = getOWLOntologyManager().createOntology();
			} catch (OWLOntologyCreationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return ontology;
	}
	private void addAxiom(OWLAxiom ax) {
		getOWLOntologyManager().addAxiom(getOntology(), ax);
	}
	private OWLLiteral getLiteral(String value) {
		if (value == null) return null;
		return getOWLDataFactory().getOWLLiteral(value);
	}
	private OWLLiteral getLiteral(Double value) {
		if (value == null) return null;
		return getOWLDataFactory().getOWLLiteral(value);
	}
	private void addType(IRI i, String cn) {
		OWLClassExpression classExpression = getOWLDataFactory().getOWLClass(getIRI(cn));
		OWLIndividual individual = getOWLDataFactory().getOWLNamedIndividual(i);
		addAxiom(getOWLDataFactory().getOWLClassAssertionAxiom(classExpression, individual));
	}
	private void addFact(IRI subj, IRI property, String value) {
		addFact(subj, getOWLDataFactory().getOWLAnnotationProperty(property), value);
	}
	private void addFact(IRI subj, IRI property, Double value) {
		addFact(subj, getOWLDataFactory().getOWLAnnotationProperty(property), value);
	}
	private void addFact(IRI subj, OWLAnnotationProperty property, String value) {
		addAxiom(getOWLDataFactory().getOWLAnnotationAssertionAxiom(property, subj, 
				getLiteral(value)));
	}
	private void addFact(IRI subj, OWLAnnotationProperty property, Double value) {
		addAxiom(getOWLDataFactory().getOWLAnnotationAssertionAxiom(property, subj, 
				getLiteral(value)));		
	}
	private void addFact(OWLIndividual subj, OWLDataProperty property, Double value) {
		addAxiom(getOWLDataFactory().getOWLDataPropertyAssertionAxiom(property, subj, 
				getLiteral(value)));		
	}
	private void addFact(IRI subj, IRI property, IRI obj) {
		addFact(subj, getOWLDataFactory().getOWLAnnotationProperty(property), obj);
	}
	private void addFact(IRI subj, OWLAnnotationProperty property, IRI obj) {
		addAxiom(getOWLDataFactory().getOWLAnnotationAssertionAxiom(property, subj, obj));
	}
	private void addLabel(IRI subj, String value) {
		if (value == null) return;
		addFact(subj, OWLRDFVocabulary.RDFS_LABEL.getIRI(), value);
	}
	private void addMatch(IRI subj, IRI mi) {
		addFact(subj, getIRI("has_match"), mi);
	}
	private void addScore(OWLIndividual i, String m, Double s) {
		addFact(i, getOWLDataFactory().getOWLDataProperty(getIRI(m)), s);
	}
	private IRI getIRI(String s) {
		return  IRI.create("http://owlsim.org/vocab/"+s);

	}


	/* (non-Javadoc)
	 * @see owltools.sim.io.SimResultRenderer#printComment(java.lang.CharSequence)
	 */
	@Override
	public void printComment(CharSequence comment) {
		//
	}

	/* (non-Javadoc)
	 * @see owltools.sim.io.SimResultRenderer#printSim(owltools.sim.io.Foobar.SimScores, org.semanticweb.owlapi.model.OWLClass, org.semanticweb.owlapi.model.OWLClass, owltools.graph.OWLGraphWrapper)
	 */
	@Override
	@Deprecated
	public void printAttributeSim(AttributesSimScores simScores, OWLGraphWrapper graph)
	{
		this.graph = graph;

		OWLClass a = simScores.a;
		OWLClass b = simScores.b;

		IRI ai = a.getIRI();
		IRI bi = b.getIRI();
		//addLabel(ai, graph.getLabel(a));
		//addLabel(bi, graph.getLabel(b));

		IRI mi = IRI.create(ai + "-" + graph.getIdentifier(b));
		String mlabel = "match to "+graph.getLabel(b)+ " from "+graph.getLabel(a);
		addType(mi, "match");

		addMatch(ai, mi);
		addMatch(bi, mi);

		OWLIndividual m = getOWLDataFactory().getOWLNamedIndividual(mi);



		//scores
		if (simScores.simJScore != null) {
			mlabel += " EquivScore=" + doubleRenderer.format(simScores.simJScore);
			addScore(m, Metric.SIMJ.toString(), simScores.simJScore);
		}
		if (simScores.AsymSimJScore != null) {
			mlabel += " SubclassScore=" + doubleRenderer.format(simScores.AsymSimJScore);
			addScore(m, "AsymmetricSimJ", simScores.AsymSimJScore);
		}

		ScoreAttributePair lcs = simScores.lcsScore;
		if (lcs != null) {
			// LCS points to a class so we use an AP
			addFact(mi, getIRI("LCS"),  ((OWLNamedObject) lcs.attributeClass).getIRI());
			addScore(m, Metric.LCSIC.toString(), lcs.score);
		}

		addLabel(mi, mlabel);

	}

	@Override
	public void printIndividualPairSim(IndividualSimScores scores, OWLPrettyPrinter owlpp, OWLGraphWrapper graph) {
		// TODO
	}





	@Override
	public void printAttributeSimWithIndividuals(AttributesSimScores simScores, OWLPrettyPrinter owlpp,
			OWLGraphWrapper g, OWLNamedIndividual i, OWLNamedIndividual j) {

		// TODO
	}

	@Override
	public void printAttributeSim(AttributesSimScores simScores,
			OWLGraphWrapper graph, OWLPrettyPrinter owlpp) {
		// TODO Auto-generated method stub

	}

	@Override
	public void dispose() {
		try {
			graph.getManager().saveOntology(ontology, resultOutStream);
		} catch (OWLOntologyStorageException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	// NEW
	/* (non-Javadoc)
	 * @see owltools.sim.io.SimResultRenderer#printPairScores(owltools.sim2.scores.ElementPairScores, owltools.io.OWLPrettyPrinter, owltools.graph.OWLGraphWrapper)
	 */
	@Override
	public void printPairScores(ElementPairScores scores) {
		// TODO
	}
	/* (non-Javadoc)
	 * @see owltools.sim.io.SimResultRenderer#printPairScores(owltools.sim2.scores.AttributePairScores, owltools.io.OWLPrettyPrinter, owltools.graph.OWLGraphWrapper)
	 */
	@Override
	public void printPairScores(AttributePairScores simScores) {
		OWLClass a = simScores.getA();
		OWLClass b = simScores.getB();

		IRI ai = a.getIRI();
		IRI bi = b.getIRI();
		
		// we may want to make this optional in future - turn off
		// for now as it makes file too voluminous
		//addLabel(ai, graph.getLabel(a));
		//addLabel(bi, graph.getLabel(b));

		IRI mi = IRI.create(ai + "-" + graph.getIdentifier(b));
		String mlabel = "match to "+graph.getLabel(b)+ " from "+graph.getLabel(a);
		addType(mi, "match");

		addMatch(ai, mi);
		addMatch(bi, mi);

		OWLIndividual m = getOWLDataFactory().getOWLNamedIndividual(mi);



		//scores
		if (simScores.simjScore != null) {
			mlabel += " EquivScore=" + doubleRenderer.format(simScores.simjScore);
			addScore(m, Metric.SIMJ.toString(), simScores.simjScore);
		}
		if (simScores.asymmetricSimjScore != null) {
			mlabel += " SubclassScore=" + doubleRenderer.format(simScores.asymmetricSimjScore);
			addScore(m, "AsymmetricSimJ", simScores.asymmetricSimjScore);
		}

		Set<OWLClass> lcsSet = simScores.lcsSet;
		if (lcsSet != null) {
			// LCS points to a class so we use an AP
			for (OWLClass lcs : lcsSet ) {
				addFact(mi, getIRI("LCS"),  ((OWLNamedObject) lcs).getIRI());
			}
			addScore(m, Metric.LCSIC.toString(), simScores.lcsIC);
		}

		addLabel(mi, mlabel);	
	}

}

