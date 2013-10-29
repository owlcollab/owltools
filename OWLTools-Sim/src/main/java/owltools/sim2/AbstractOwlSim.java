package owltools.sim2;

import java.util.Set;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.sim2.SimpleOwlSim.Direction;
import owltools.sim2.SimpleOwlSim.Metric;

public abstract class AbstractOwlSim implements OwlSim {

	protected OWLReasoner reasoner;
	private Integer corpusSize; // number of individuals in domain

	@Override
	public OWLOntology getSourceOntology() {
		return getReasoner().getRootOntology();
	}

	@Override
	public OWLReasoner getReasoner() {
		return reasoner;
	}
	
	@Override
	public Set<Node<OWLClass>> getNamedSubsumers(OWLClass a) {
		return getReasoner().getSuperClasses(a, false).getNodes();
	}


	/* (non-Javadoc)
	 * @see owltools.sim2.OwlSim#getCorpusSize()
	 */	
	public int getCorpusSize() {
		if (corpusSize == null) {
			corpusSize = getAllElements().size();
		}
		return corpusSize;
	}

	/* (non-Javadoc)
	 * @see owltools.sim2.OwlSim#setCorpusSize(int)
	 */
	public void setCorpusSize(int size) {
		corpusSize = size;
	}

	/* (non-Javadoc)
	 * @see owltools.sim2.OwlSim#getEntropy()
	 */
	
	@Override
	public Double getEntropy() {
		return getEntropy(getAllAttributeClasses());
	}

	/* (non-Javadoc)
	 * @see owltools.sim2.OwlSim#getEntropy(java.util.Set)
	 */
	
	@Override
	public Double getEntropy(Set<OWLClass> cset) {
		double e = 0.0;
		for (OWLClass c : cset) {
			int freq = getNumElementsForAttribute(c);
			if (freq == 0)
				continue;
			double p = ((double) freq) / getCorpusSize();
			e += p * Math.log(p) ;
		}
		return -e / Math.log(2);
	}


}
