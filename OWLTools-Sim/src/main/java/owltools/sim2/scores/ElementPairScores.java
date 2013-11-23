package owltools.sim2.scores;

import java.util.Set;
import java.util.Vector;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import owltools.sim2.OwlSim;
import owltools.sim2.OwlSim.ScoreAttributeSetPair;

/**
 * A pair of scores for any given pair of individuals
 * 
 * @author cjm
 *
 */
public class ElementPairScores implements PairScores<OWLNamedIndividual>, Comparable<ElementPairScores> {
	public OWLNamedIndividual i;
	public OWLNamedIndividual j;

	public Vector<OWLClass> cs;
	public Vector<OWLClass> ds;
	
	public Integer combinedScore;
	
	public Double avgIC = null;
	public Double maxIC = null;
	public Set<OWLClass> maxICwitness = null;
	
	public Double simjScore = null;
	public Double asymmetricSimjScore = null;
	public Double inverseAsymmetricSimjScore = null;
	
	
	public Double bmaSymIC = null;
	public Double bmaAsymIC = null;
	public Double bmaInverseAsymIC = null;
	
	//public ScoreAttributeSetPair bmaAsymJ = null;
	
	//public ScoreAttributeSetPair bmaSymJ = null;
	
	public Double simGIC = null;
	public Double asymmetricSimGIC;
	public Double inverseAsymmetricSimGIC;
	
	//ScoreMatrix<Double> icMatrix;
	public ScoreMatrix<ScoreAttributeSetPair> iclcsMatrix;
		
	public ElementPairScores(OWLNamedIndividual i, OWLNamedIndividual j) {
		this.i = i;
		this.j = j;
	}

	// note: a renderer should be used here
	public String toString() {
		return i+" "+j+" maxIC:" + maxIC + " maxIC(term): "+ maxICwitness + " simJ: " + simjScore + " bma(IC):" + bmaSymIC;
	}

	@Override
	public OWLNamedIndividual getA() {
		return i;
	}

	@Override
	public OWLNamedIndividual getB() {
		return j;
	}

	public int getNumberOfAttributesForI() {
		return cs.size();
	}
	public int getNumberOfAttributesForJ() {
		return ds.size();
	}

	@Override
	public int compareTo(ElementPairScores s2) {
		return s2.combinedScore.compareTo(this.combinedScore);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((i == null) ? 0 : i.hashCode());
		result = prime * result + ((j == null) ? 0 : j.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ElementPairScores other = (ElementPairScores) obj;
		if (i == null) {
			if (other.i != null)
				return false;
		} else if (!i.equals(other.i))
			return false;
		if (j == null) {
			if (other.j != null)
				return false;
		} else if (!j.equals(other.j))
			return false;
		return true;
	}
	
	
}