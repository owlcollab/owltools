package owltools.sim;

import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

/**
 * score is the IC of the intersection of all attributes  divided my min IC of a or b
 * 
 * it's recommended this used as a sub-method of a MultiSimilarity check
 * 
 * @author cjm
 *
 */
public class ConjunctiveSetInformationContentRatioSimilarity extends Similarity {

	private static Logger LOG = Logger.getLogger(ConjunctiveSetInformationContentRatioSimilarity.class);
	Double lcsICRatio;
	public Set<OWLObject> lcsIntersectionSet = new HashSet<OWLObject>();
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (OWLObject obj : lcsIntersectionSet) {
			sb.append(obj+"; ");
		}
		return score + " "+sb.toString();
	}

	@Override
	public void calculate(SimEngine simEngine, OWLObject a, OWLObject b) {
		this.simEngine = simEngine;
		this.a = a;
		this.b = b;
		lcsIntersectionSet = simEngine.getLeastCommonSubsumers(a, b);
		LOG.info("LCSs:"+lcsIntersectionSet.size());
		score = simEngine.getInformationContent(lcsIntersectionSet);
		if (score == null) {
			score = 0.0;
			lcsICRatio = score;
		}
		else {
			Double aIC = simEngine.getInformationContent(a);
			Double bIC = simEngine.getInformationContent(b);
			if (aIC == null || bIC == null || aIC == 0 || bIC == 0) {
				lcsICRatio = 0.0;
			}
			else {
				lcsICRatio = score / Math.min(aIC, bIC);
			}
		}
		

	}
	
	public OWLClassExpression getLCS() {
		OWLDataFactory df = simEngine.getGraph().getDataFactory();
		OWLObjectProperty topRel = df.getOWLTopObjectProperty();
		Set<OWLClassExpression> ces = new HashSet<OWLClassExpression>();
		for (OWLObject obj : lcsIntersectionSet) {
			if (obj instanceof OWLClassExpression)
				ces.add(df.getOWLObjectSomeValuesFrom(topRel,
						(OWLClassExpression) obj));
		}
		OWLClassExpression ce =
			df.getOWLObjectIntersectionOf(ces);
		return ce;
	}

	// TODO - DRY - similar code to DescriptionTreeSimilarity
	@Override
	protected void translateResultsToOWLAxioms(String id, OWLNamedIndividual result, Set<OWLAxiom> axioms) {
		OWLDataFactory df = simEngine.getGraph().getDataFactory();
		
		// declare a named class for the LCS and make this equivalent to the anonymous expression
		OWLClass namedLCS = df.getOWLClass(IRI.create(id+"_LCS"));
		axioms.add(df.getOWLAnnotationAssertionAxiom(df.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL.getIRI()),
				namedLCS.getIRI(), 
				df.getOWLLiteral("LCS of "+simEngine.label(a)+" and "+simEngine.label(b))));
		axioms.add(df.getOWLEquivalentClassesAxiom(namedLCS, getLCS()));

		// link the similarity object to the named LCS
		OWLAnnotationProperty lcsp = df.getOWLAnnotationProperty(annotationIRI("has_least_common_subsumer"));
		axioms.add(df.getOWLAnnotationAssertionAxiom(lcsp, result.getIRI(), namedLCS.getIRI()));
	}
	
	// -------------
	// REPORTING
	// -------------
	public void report(Reporter r) {
		r.report(this,"pair_match_ic_icratio_subsumer",a,b,score,lcsICRatio,lcsIntersectionSet);
	}

	// -------------
	// DISPLAY
	// -------------
	public void print(PrintStream s) {
		s.println("IntersectionIC:"+toString()+"\n");
		for (OWLObject obj : lcsIntersectionSet) {
			print(s,obj);
		}

	}

}
