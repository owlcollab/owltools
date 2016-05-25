package owltools.sim2;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.HypergeometricDistributionImpl;
import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;

import owltools.io.OWLPrettyPrinter;

/**
 * @author cjm
 *
 */
public class ClassDifferentiaLearner  {

	private Logger LOG = Logger.getLogger(ClassDifferentiaLearner.class);
	OwlSim sim;

	public class InferredClassDefinition implements Comparable<InferredClassDefinition> {
		OWLClass baseClass;
		OWLClass genus;
		OWLClass differentia;
		Double pValue;



		public InferredClassDefinition(OWLClass baseClass, OWLClass genus,
				OWLClass differentia, Double pValue) {
			super();
			this.baseClass = baseClass;
			this.genus = genus;
			this.differentia = differentia;
			this.pValue = pValue;
		}

		@Override
		public String toString() {
			return baseClass + " EquivalentTo: " + genus +" AND " + differentia + " :: " + pValue;
		}


		@Override
		public int compareTo(InferredClassDefinition d2) {
			return this.pValue.compareTo((d2).pValue);
		}


	}

	Map<OWLClass, List<InferredClassDefinition>> defsByClass = 
			new HashMap<OWLClass, List<InferredClassDefinition>>();


	public ClassDifferentiaLearner(OwlSim sim) {
		super();
		this.sim = sim;
	}



	public void compute(OWLClass baseClassRoot, OWLClass diffClassRoot) throws UnknownOWLClassException, MathException {

		/*
		 * TODO:
		 * 
		 * here we treat the parent as background set; this isn't correct for
		 * the purposes of finding the best definition; e.g. when comparing
		 * 
		 * X = G and Y vs X = thing and Y
		 * 
		 * the former may be at a disadvantage; it may be locally good compared
		 * to other Gs but here it would be better to view (G AND Y) as the enriched
		 * class
		 * 
		 */
		for (OWLClass child : sim.getReasoner().getSubClasses(baseClassRoot, false).getFlattened()) {
			if (sim.getNumElementsForAttribute(child) == 0) {
				continue;
			}
			Vector<InferredClassDefinition> defs = new Vector<InferredClassDefinition>();
			
			for (OWLClass parent : sim.getReasoner().getSuperClasses(child, false).getFlattened()) {
				defs.addAll( 
						findDifferentia(child,
								parent, 
								diffClassRoot)
								);
				
			}
			Collections.sort(defs);
			if (defs.size() > 0) {
				System.out.println("BEST_FOR: "+child+" = "+defs.get(0));
			}
			else {
				System.out.println("NO MATCH: "+child);
			}
			defsByClass.put(child, defs);
			LOG.info("** KEYS="+defsByClass.keySet().size());
			for (InferredClassDefinition d : defs) {
				LOG.info("DEF:"+d);
			}
		}
	}
	
	public List<InferredClassDefinition> findDifferentia(OWLClass child, OWLClass parent, OWLClass diffClassRoot) throws UnknownOWLClassException, MathException {

		List<InferredClassDefinition> results = new Vector<InferredClassDefinition>();
		LOG.info("Finding: "+child+" against "+parent);
		for (OWLClass differentiaClass : sim.getReasoner()
				.getSubClasses(diffClassRoot, false).getFlattened()) {
			//LOG.info("  Testing: "+enrichedClass);
			if (differentiaClass.isBottomEntity()) {
				continue;
			}
			if (sim.getNumElementsForAttribute(differentiaClass) == 0) {
				LOG.info("Skipping: "+differentiaClass);
				continue;
			}
			InferredClassDefinition r = calculateGenusDifferentiaEnrichment(child, parent,
					differentiaClass);
			if (r != null)
				results.add(r);
		}
		Collections.sort(results);
		return results;
	}
	
	public InferredClassDefinition calculateGenusDifferentiaEnrichment(OWLClass sample, 
			OWLClass genus,
			OWLClass differentia) throws MathException, UnknownOWLClassException {

		long t = System.currentTimeMillis();
		// LOG.info("Hyper :"+populationClass
		// +" "+sampleSetClass+" "+enrichedClass);
		int populationClassSize;
		int enrichedClassSize ;
		if (sim.getNumElementsForAttribute(sample) == 0)
			return null;
		if (sim.getNumElementsForAttribute(genus) == 0)
			return null;
		if (sim.getNumElementsForAttribute(differentia) == 0)
			return null;
		populationClassSize = sim.getCorpusSize();
		OWLDataFactory df = sim.getSourceOntology().getOWLOntologyManager().getOWLDataFactory();
		//OWLObjectIntersectionOf enrichedClass = 
		//		df.getOWLObjectIntersectionOf(genus, differentia);
		enrichedClassSize = ((FastOwlSim) sim).getNumSharedElements(genus, differentia); 
				//sim.getReasoner().getInstances(enrichedClass, false).getNodes().size();
		int sampleSetClassSize = sim.getNumElementsForAttribute(sample);
		//OWLObjectIntersectionOf enrichedClassInSample = 
		//df.getOWLObjectIntersectionOf(enrichedClass, sample);
		int eiSetSize = ((FastOwlSim) sim).getNumSharedElements(genus, differentia, sample); 
		//sim.getReasoner().getInstances(enrichedClassInSample, false).getNodes().size();
		if (eiSetSize == 0) {
			return null;
		}
		long t1 = System.currentTimeMillis();

		//LOG.info(" shared elements: "+eiSet.size()+" for "+enrichedClass);
		HypergeometricDistributionImpl hg = new HypergeometricDistributionImpl(
				populationClassSize, sampleSetClassSize, enrichedClassSize);
		/*
		 * LOG.info("popsize="+getNumElementsForAttribute(populationClass));
		 * LOG.info("sampleSetSize="+getNumElementsForAttribute(sampleSetClass));
		 * LOG.info("enrichedClass="+getNumElementsForAttribute(enrichedClass));
		 */
		// LOG.info("both="+eiSet.size());
		double p = hg.cumulativeProbability(eiSetSize,
				Math.min(sampleSetClassSize, enrichedClassSize));
		//LOG.info("  p="+p);
		long t2 = System.currentTimeMillis();
		//double pCorrected = p * getCorrectionFactor(populationClass);
		
		return new InferredClassDefinition(sample, genus, differentia, p);
		//return new EnrichmentResult(sample, differentia, p, p, 
		//		populationClassSize, sampleSetClassSize, enrichedClassSize, eiSetSize);
	}


	public String toString() {
		StringBuffer sb = new StringBuffer();
		for (OWLClass c : defsByClass.keySet()) {
			sb.append(c +" :=\n");
			List<InferredClassDefinition> list = defsByClass.get(c);
			for (int i = 0; i < list.size(); i++) {
				InferredClassDefinition d = list.get(i);
				sb.append("  "+d+"\n");
			}
		}
		return sb.toString();
		
	}

	public String render(OWLPrettyPrinter owlpp) {
		StringBuffer sb = new StringBuffer();
		for (OWLClass c : defsByClass.keySet()) {
			sb.append(owlpp.render(c) +" :=\n");
			List<InferredClassDefinition> list = defsByClass.get(c);
			for (int i = 0; i < list.size(); i++) {
				InferredClassDefinition d = list.get(i);
				sb.append("  "+owlpp.render(d.genus)+" ^ "+owlpp.render(d.differentia)+" :: "+d.pValue+"\n");
			}
		}
		return sb.toString();
	}

}
