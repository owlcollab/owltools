package owltools.sim2.scores;

import java.util.Set;

import org.semanticweb.owlapi.model.OWLClass;

/**
 * A set of scores for any given pair of attributes classes
 * 
 * @author cjm
 *
 */
public class AttributePairScores implements PairScores<OWLClass> {
	public OWLClass c;
	public OWLClass d;
	
	public Double lcsIC = null;
	public Set<OWLClass> lcsSet = null;
	
	public Double simjScore = null;
	public Double asymmetricSimjScore = null;
	public Double inverseAsymmetricSimjScore = null;
	
	public Double simGIC = null;
	public boolean isBestMatchForI = false;

	public AttributePairScores(OWLClass c, OWLClass d) {
		this.c = c;
		this.d = d;
	}
	@Override
	public OWLClass getA() {
		return c;
	}
	@Override
	public OWLClass getB() {
		return d;
	}

}