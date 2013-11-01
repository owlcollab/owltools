package owltools.sim2;

import java.util.Set;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.sim2.SimpleOwlSim.Direction;
import owltools.sim2.SimpleOwlSim.Metric;

public abstract class AbstractOwlSim implements OwlSim {
	
	private Logger LOG = Logger.getLogger(AbstractOwlSim.class);

	long totalTimeSimJ = 0;
	long totalCallsSimJ = 0;
	long totalTimeLCSIC = 0;
	long totalCallsLCSIC = 0;
	long totalTimeGIC = 0;
	long totalCallsGIC = 0;



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

	protected long tdelta(long prev) {
		return System.currentTimeMillis() - prev;
	}

	/**
	 * 
	 */
	public void showTimings() {
		LOG.info("Timings:");
		if (totalCallsSimJ > 0) {
			LOG.info("t(SimJ) ms = "+totalTimeSimJ + " / "+totalCallsSimJ + " = " + totalTimeSimJ / (double) totalCallsSimJ);
		}
		if (totalCallsLCSIC > 0) {
			LOG.info("t(LCS) ms = "+totalTimeLCSIC + " / "+totalCallsLCSIC + " = " + totalTimeLCSIC / (double) totalCallsLCSIC);
		}
		if (totalCallsGIC > 0) {
			LOG.info("t(GIC) ms = "+totalTimeGIC + " / "+totalCallsGIC + " = " + totalTimeGIC / (double) totalCallsGIC);
		}
	}

	public void showTimingsAndReset() {
		showTimings();
		totalTimeSimJ = 0;
		totalCallsSimJ = 0;
		totalTimeLCSIC = 0;
		totalCallsLCSIC = 0;
		totalTimeGIC = 0;
		totalCallsGIC = 0;

	}
	
	@Override
	public void precomputeAttributeAllByAll()  throws UnknownOWLClassException {
		LOG.info("precomputing attribute all x all");
		long t = System.currentTimeMillis();
		Set<OWLClass> cset = this.getAllAttributeClasses();
		int n=0;
		for (OWLClass c : cset ) {
			n++;
			if (n % 100 == 0) {
				LOG.info("Cached LCS for "+n+" / "+cset.size()+" attributes");
			}
			for (OWLClass d : cset ) {
				getLowestCommonSubsumerWithIC(c, d);
			}			
		}
		LOG.info("Time precomputing attribute all x all = "+tdelta(t));
		showTimingsAndReset();
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

	@Override
	public void dispose() {
		
	}
}
