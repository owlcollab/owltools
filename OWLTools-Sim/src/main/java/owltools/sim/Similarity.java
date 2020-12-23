package owltools.sim;

import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;

import org.obolibrary.obo2owl.Obo2OWLConstants;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLPropertyRange;
import org.semanticweb.owlapi.model.OWLQuantifiedRestriction;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;
import owltools.sim.SimEngine.SimilarityAlgorithmException;

/**
 * Represents a pairwise similarity between two OWLObjects.
 * 
 * todo: consider making a and b immuntable
 *  
 * @author cjm
 *
 */
public abstract class Similarity {
	OWLObject a;
	OWLObject b;
	Double score;
	protected Double minScore;
	SimEngine simEngine;
	boolean isComparable = true;

	// for caching as OWL
	IRI persistentIRI;

	public Similarity() {
		super();
	}

	public Double getScore() {
		return score;
	}
	public void setScore(Double score) {
		this.score = score;
	}
	public void setScore(int score) {
		this.score = (double) score;
	}

	public boolean isAboveMinScore() {
		return minScore == null || score >= minScore;
	}

	public String toString() {
		return "S:"+score;
	}
	public void print() {
		print(System.out);
	}
	public void print(PrintStream s) {
		s.println(toString());
	}
	public void print(PrintStream s, OWLObject x) {
		if (x instanceof OWLNamedObject) {
			String label = simEngine.getGraph().getLabel(x);
			if (label == null)
				s.print(x.toString());
			else
				s.print(x.toString()+" \""+label+"\"");
		}
		else {
			printDescription(s,x);
		}
	}


	public void printDescription(PrintStream s, OWLObject x) {
		printDescription(s,x,0);
	}
	public void printDescription(PrintStream s, OWLObject x, int depth) {
		depth++;
		if (x == null) {
			s.print("NULL");
			return;
		}
		OWLGraphWrapper g = simEngine.getGraph();
		
		// for individuals, show their class
		if (x instanceof OWLNamedIndividual) {
			for (OWLGraphEdge e : g.getPrimitiveOutgoingEdges(x)) {
				OWLObject c = e.getTarget();
				if (c instanceof OWLClassExpression) {
					printDescription(s, c, depth);
				}
			}
			s.print(" :: ");
		}
		
		//
		if (x instanceof OWLObjectIntersectionOf) {
			int n = 0;
			for (OWLClassExpression y : ((OWLObjectIntersectionOf)x).getOperands()) {
				if (n>0)
					s.print(" and ");
				printDescription(s, y, depth);
				n++;
			}
		}
		else if (x instanceof OWLObjectUnionOf) {
			int n = 0;
			if (depth>0)
				s.print("(");
			for (OWLClassExpression y : ((OWLObjectUnionOf)x).getOperands()) {
				if (n>0)
					s.print(" or ");
				printDescription(s, y, depth);
				n++;
			}
			if (depth>0)
				s.print(")");
		}
		else if (x instanceof OWLQuantifiedRestriction) {
			OWLPropertyRange y = ((OWLQuantifiedRestriction<?>)x).getFiller();
			if (depth>0)
				s.print("(");
			printDescription(s,((OWLQuantifiedRestriction)x).getProperty());
			s.print(" ");
			if (x instanceof OWLObjectAllValuesFrom) {
				s.print("only");
			}
			else if (x instanceof OWLObjectSomeValuesFrom) {
				s.print("some");
			}
			s.print(" ");
			printDescription(s, y, depth);
			if (depth>0)
				s.print(")");
		}
		else if (x instanceof OWLNamedObject) {

			String label = simEngine.getGraph().getLabel(x);

			if (label == null) {
				label = ((OWLNamedObject)x).getIRI().getFragment();
			}
			if (label == null) {
				label = ((OWLNamedObject)x).getIRI().toString();
			}
			s.print(label);

			if (x instanceof OWLClass && label.contains("Phenotype Class")) {
				// TODO - make this configurable!
				for (OWLOntology ont : g.getAllOntologies()) {
					for (OWLEquivalentClassesAxiom ax : ont.getEquivalentClassesAxioms((OWLClass)x)) {
						for (OWLClassExpression y : ax.getClassExpressions()) {
							if (y instanceof OWLClass)
								continue;
							s.print("==");
							printDescription(s, y,depth);
						}
					}
				}
			}

		}
		else {
			s.print(x.toString());
		}
	}

	/**
	 * @param simEngine
	 * @param a
	 * @param b
	 * @throws SimilarityAlgorithmException
	 */
	public abstract void calculate(SimEngine simEngine, OWLObject a, OWLObject b) throws SimilarityAlgorithmException;

	public OWLOntology createOWLOntologyFromResults() throws OWLOntologyCreationException {
		OWLGraphWrapper graph = simEngine.getGraph();
		OWLOntology ont = graph.getManager().createOntology();
		addResultsToOWLOntology(ont);
		return ont;
	}

	/**
	 * translates similarity results into OWL Axioms and saves axioms into an OWL Ontology
	 *
	 * @param ont
	 */
	public void addResultsToOWLOntology(OWLOntology ont) {
		if (!isComparable)
			return;
		OWLGraphWrapper graph = simEngine.getGraph();
		for (OWLAxiom axiom: translateResultsToOWLAxioms()) {
			AddAxiom aa = new AddAxiom(ont, axiom);
			graph.getManager().applyChange(aa);
		}
	}

	public Set<OWLAxiom> translateResultsToOWLAxioms() {

		System.out.println("TRANSLATING TO OWL AXIOM:"+this);
		OWLGraphWrapper graph = simEngine.getGraph();
		Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();

		OWLDataFactory df = graph.getDataFactory();
		if (!(a instanceof OWLNamedObject)) {
			System.err.println(a+ "not named - cant write OWL results");
			return axioms;
		}
		if (!(b instanceof OWLNamedObject)) {
			System.err.println(b+ "not named - cant write OWL results");
			return axioms;
		}
		IRI ia = ((OWLNamedObject) a).getIRI();
		IRI ib = ((OWLNamedObject) b).getIRI();
		String[] toksA = splitIRI(ia);
		String[] toksB = splitIRI(ib);
		String id = toksA[0]+toksA[1]+"-vs-"+toksB[1];

		persistentIRI = IRI.create(id);

		// each similarity is stored as an individual of class similarity_relationship
		OWLNamedIndividual result = df.getOWLNamedIndividual(persistentIRI);
		OWLClass ac = df.getOWLClass(annotationIRI("similarity_relationship"));
		axioms.add(df.getOWLClassAssertionAxiom(ac, result));
		axioms.add(df.getOWLAnnotationAssertionAxiom(df.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL.getIRI()),
				result.getIRI(), 
				df.getOWLLiteral("Similarity relationship between "+simEngine.label(a)+" and "+simEngine.label(b))));

		// each object in the pair is connected to the similarity
		OWLAnnotationProperty p = df.getOWLAnnotationProperty(annotationIRI("has_similarity_relationship"));
		axioms.add(df.getOWLAnnotationAssertionAxiom(p, ((OWLNamedObject) a).getIRI(), result.getIRI()));
		axioms.add(df.getOWLAnnotationAssertionAxiom(p, ((OWLNamedObject) b).getIRI(), result.getIRI()));

		// every similarity has a score
		OWLAnnotationProperty sp = df.getOWLAnnotationProperty(annotationIRI("has_score"));
		axioms.add(df.getOWLAnnotationAssertionAxiom(sp, result.getIRI(), df.getOWLLiteral(score)));

		translateResultsToOWLAxioms(id, result, axioms);

		return axioms;
	}


	protected abstract void translateResultsToOWLAxioms(String id, OWLNamedIndividual result, Set<OWLAxiom> axioms);

	protected String[] splitIRI(IRI x) {
		String s = x.toString();
		String id = null;
		if (s.startsWith(Obo2OWLConstants.DEFAULT_IRI_PREFIX)) {
			id = s.replaceAll(Obo2OWLConstants.DEFAULT_IRI_PREFIX, "");
			return new String[]{Obo2OWLConstants.DEFAULT_IRI_PREFIX,id};
		}
		for (String del : new String[]{"#","/",":"}) {
			if (s.contains(del)) {
				String[] r = s.split(del,2);
				r[0] = r[0]+del;
				return r;
			}

		}
		return new String[]{"",s};
	}

	protected IRI annotationIRI(String name) {
		return IRI.create("http://purl.obolibrary.org/obo/IAO/score#"+name);
	}


	/**
	 * @param r
	 */
	public void report(Reporter r) {
		r.report(this,"pair_match",a,b,score);
	}

}
