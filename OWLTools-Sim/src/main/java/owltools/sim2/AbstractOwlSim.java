package owltools.sim2;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.sim2.SimpleOwlSim.Direction;
import owltools.sim2.SimpleOwlSim.Metric;
import owltools.sim2.SimpleOwlSim.ScoreAttributePair;
import owltools.util.ClassExpressionPair;

public abstract class AbstractOwlSim implements OwlSim {

	private Logger LOG = Logger.getLogger(AbstractOwlSim.class);

	long totalTimeSimJ = 0;
	long totalCallsSimJ = 0;
	long totalTimeLCSIC = 0;
	long totalCallsLCSIC = 0;
	long totalTimeGIC = 0;
	long totalCallsGIC = 0;
	public SimStats simStats = new SimStats(); // TODO - force access via get/set?


	protected OWLReasoner reasoner;
	protected Integer corpusSize; // number of individuals in domain

	protected boolean isLCSCacheFullyPopulated = false;
	private boolean isNoLookupForLCSCache = false;
	private Properties simProperties;


	
	@Override
	public OWLOntology getSourceOntology() {
		return getReasoner().getRootOntology();
	}

	@Override
	public OWLReasoner getReasoner() {
		return reasoner;
	}

	
	public boolean isNoLookupForLCSCache() {
		return isNoLookupForLCSCache;
	}

	public void setNoLookupForLCSCache(boolean isNoLookupForLCSCache) {
		this.isNoLookupForLCSCache = isNoLookupForLCSCache;
	}

	@Override
	public Properties getSimProperties() {
		return simProperties;
	}

	@Override
	public void setSimProperties(Properties simProperties) {
		this.simProperties = simProperties;
	}

	
	public SimStats getSimStats() {
		return simStats;
	}

	public void setSimStats(SimStats simStats) {
		this.simStats = simStats;
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

	// CACHES
	public final String icIRIString = "http://owlsim.org/ontology/ic"; // TODO

	@Override
	public OWLOntology cacheInformationContentInOntology() throws OWLOntologyCreationException, UnknownOWLClassException {
		OWLOntologyManager mgr = getSourceOntology().getOWLOntologyManager();
		OWLDataFactory df = mgr.getOWLDataFactory();
		OWLOntology o = mgr.createOntology();
		OWLAnnotationProperty p = df.getOWLAnnotationProperty(IRI.create(icIRIString));
		for (OWLClass c : getSourceOntology().getClassesInSignature()) {
			Double ic = getInformationContentForAttribute(c);
			if (ic != null) {
				mgr.addAxiom(o,
						df.getOWLAnnotationAssertionAxiom(p, 
								c.getIRI(), 
								df.getOWLLiteral(ic)));
			}

		}
		return o;
	}

	protected abstract void setInformtionContectForAttribute(OWLClass c, Double v);
	protected abstract void clearInformationContentCache();
	
	@Override
	public void setInformationContentFromOntology(OWLOntology o) {
		OWLOntologyManager mgr = getSourceOntology().getOWLOntologyManager();
		OWLDataFactory df = mgr.getOWLDataFactory();
		clearInformationContentCache();
		//icCache = new HashMap<OWLClass, Double>();
		for (OWLAnnotationAssertionAxiom ax : o.getAxioms(AxiomType.ANNOTATION_ASSERTION)) {
			if (ax.getProperty().getIRI().toString().equals(icIRIString)) {
				OWLLiteral lit = (OWLLiteral) ax.getValue();
				OWLClass c = df.getOWLClass((IRI) ax.getSubject());
				Double v = lit.parseDouble();
				setInformtionContectForAttribute(c, v);
			}
		}
	}
	
	public void saveLCSCache(String fileName) throws IOException {
		saveLCSCache(fileName, null);
	}
	
	protected final String prefix = "http://purl.obolibrary.org/obo/";
	protected String getShortId(OWLClass c) {
		IRI x = ((OWLClass) c).getIRI();
		return x.toString().replace(prefix, ""); // todo - do not hardcode
	}
	protected OWLClass getOWLClassFromShortId(String id) {
		return getSourceOntology().getOWLOntologyManager().getOWLDataFactory().getOWLClass(IRI.create(prefix + id));
	}


}
