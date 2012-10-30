package owltools.sim2.preprocessor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;

import owltools.sim.OWLClassExpressionPair;

public abstract class LCSEnabledSimPreProcessor extends AbstractSimPreProcessor {

	protected Map<OWLClassExpressionPair, OWLClass> lcsCache = new HashMap<OWLClassExpressionPair, OWLClass>();
	private Map< Set<Node<OWLClass>>, OWLClassExpression> csetToExpressionMap = new HashMap< Set<Node<OWLClass>>, OWLClassExpression>();
	
	protected double defaultNewIntersectionSimJThreshold = 0.75;
	
	// this is public mainly so that it can be set in junit tests
	public double defaultLCSElementFrequencyThreshold = 0.25;
	//private Map<OWLClassExpression,OWLClass> 


	
	protected void generateLeastCommonSubsumersForAttributeClasses() {
		Set<OWLClass> types = getAttributeClasses();
		generateLeastCommonSubsumers(types);
	}
	
	public void generateLeastCommonSubsumers(Set<OWLClass> leafClasses) {

		LOG.info("Generating LCSs en masse; leaf classes "+leafClasses.size());
		LOG.info("Num materialized class expressions (prior) "+materializedClassExpressionMap.size());
		for (OWLClass a : leafClasses) {
			LOG.info("  ALL vs: "+a+" '"+getAnyLabel(a)+"'");		
			for (OWLClass b : leafClasses) {
				// LCS operation is symmetric, only pre-compute one way
				if (a.compareTo(b) > 0) {
					OWLClass lcs = getLowestCommonSubsumerClass(a, b, leafClasses);
				}
			}
		}
		LOG.info("DONE all x all");		
		LOG.info("Num materialized class expressions (post) "+materializedClassExpressionMap.size());
	}

	
	// ----------- ----------- ----------- -----------
	// SUBSUMERS AND LOWEST COMMON SUBSUMERS
	// ----------- ----------- ----------- -----------

	public Set<Node<OWLClass>> getNamedSubsumers(OWLClassExpression a) {
		return getReasoner().getSuperClasses(a, false).getNodes();
	}
	
	public Set<Node<OWLClass>> getNamedSubsumers(OWLNamedIndividual a) {
		return getReasoner().getTypes(a, false).getNodes();
	}

	public Set<Node<OWLClass>> getNamedReflexiveSubsumers(OWLClassExpression a) {
		// TODO: consider caching
		Set<Node<OWLClass>> nodes =  new HashSet<Node<OWLClass>>(getReasoner().getSuperClasses(a, false).getNodes());
		nodes.add(getReasoner().getEquivalentClasses(a));
		return nodes;
	}

	public Set<Node<OWLClass>> getNamedCommonSubsumers(OWLClassExpression a, OWLClassExpression b) {
		Set<Node<OWLClass>> nodes = getNamedReflexiveSubsumers(a);
		nodes.retainAll(getNamedReflexiveSubsumers(b));
		return nodes;
	}
	public Set<Node<OWLClass>> getNamedCommonSubsumers(OWLNamedIndividual a, OWLNamedIndividual b) {
		Set<Node<OWLClass>> nodes = getNamedSubsumers(a);
		nodes.retainAll(getNamedSubsumers(b));
		return nodes;
	}

	// TODO - cache this
	public Set<Node<OWLClass>> getNamedLowestCommonSubsumers(OWLClassExpression a, OWLClassExpression b) {
		Set<Node<OWLClass>> nodes = getNamedCommonSubsumers(a, b);
		Set<Node<OWLClass>> rNodes = new HashSet<Node<OWLClass>>();
		for (Node<OWLClass> node : nodes) {
			// all ancestors (non-reflexive) of node are redundant
			rNodes.addAll(getReasoner().getSuperClasses(node.getRepresentativeElement(), false).getNodes());
		}
		nodes.removeAll(rNodes);
		return nodes;
	}

	/**
	 * gets the LCS. If multiple named LCSs exist in the ontology, then
	 * a class expression is generated from the filtered intersection
	 * 
	 * @param a
	 * @param b
	 * @return OWLClassExpression
	 */
	public OWLClassExpression getLowestCommonSubsumer(OWLClassExpression a, OWLClassExpression b) {
		return getLowestCommonSubsumer(a, b, null);
	}
	public OWLClassExpression getLowestCommonSubsumer(OWLClassExpression a, OWLClassExpression b, Set<OWLClass> leafClasses) {
		if (a.equals(b)) {
			return a;
		}
		if (a instanceof OWLClass && b instanceof OWLClass) {
			if (getNamedReflexiveSubsumers(a).contains(b)) {
				return b;
			}
			if (getNamedReflexiveSubsumers(b).contains(a)) {
				return a;
			}
		}

		// TODO: optimize this for all x all
		// returns the set of named LCSs that already exist
		Set<Node<OWLClass>> ccs = getNamedLowestCommonSubsumers(a,b);

		
		// single LCS - return this directly
		if (ccs.size() == 1) {
			return ccs.iterator().next().getRepresentativeElement();
		}
		
		if (csetToExpressionMap.containsKey(ccs))
			return csetToExpressionMap.get(ccs);

		// make a class expression from the filtered intersection of LCSs

		Set<OWLClass> ops = new HashSet<OWLClass>();
		OWLClass best = null;
		int bestAncScore = 0;
		for (Node<OWLClass> n : ccs) {
			OWLClass c = n.getRepresentativeElement();
			if (classesToSkip.contains(c))
				continue;
			// TODO: add additional custom filtering here
			/*
			if (isUpperLevel(c)) {
				// note that many upper level classes may already have been filtered out as possible candidates
				classesToSkip.add(c);
				continue;
			}
			*/
			boolean skip = false;

			if (leafClasses != null) {
				Set<OWLClass> descs = getReasoner().getSubClasses(c, false).getFlattened();
				descs.retainAll(leafClasses);
				int numDescendants = descs.size();
				String v = getProperty("newIntersectionFrequencyThreshold");
				double thresh = defaultLCSElementFrequencyThreshold;
				if (v != null && !v.equals("newIntersectionFrequencyThreshold")) {
					thresh = Double.valueOf(v);
				}
				if (numDescendants / ((float) leafClasses.size()) > thresh) {
					LOG.info("Skipping "+c+" as it has "+numDescendants+" out of "+leafClasses.size());
					classesToSkip.add(c);
					continue;				
				}
			}
			
			int numAncestors = this.getNamedReflexiveSubsumers(c).size();
			if (numAncestors > bestAncScore) {
				bestAncScore = numAncestors;
				best = c;
			}
			if (numAncestors < 3) { // TODO: configure
				// classes that are too near the root do not make informative LCS elements.
				// note that this is sensitive to graph structure, ontologies loaded etc
				classesToSkip.add(c);
				continue;
			}
			
			double lcsElementSimilarityThreshold = defaultNewIntersectionSimJThreshold;
			String v = getProperty("newIntersectionSimJThreshold");
			if (v != null && !v.equals("")) {
				lcsElementSimilarityThreshold = Double.valueOf(v);
			}
			
			// don't make intersections of similar elements.
			// TODO: exclude high-level classes. Do this via a class-based IC (intersection elements may not
			// directly classify the ABox) - but ontologies with lots of leaf nodes may bias; filter out
			// classes first? Based on simple %?
			for (Node<OWLClass> n2 : ccs) {
				OWLClass c2 = n2.getRepresentativeElement();
				if (c2.equals(c))
					continue;
				
				int numAncestors2 = this.getNamedReflexiveSubsumers(c2).size();
				// prioritize the one with highest score.
				// it it's a tie, then choose one deterministically
				if (numAncestors < numAncestors2 || (numAncestors==numAncestors2 && c.compareTo(c2) > 0)) {
					// TODO: configure simj thresh
					float sim = getMinAsymmetricJaccardIndex(c, c2);
					if (sim > lcsElementSimilarityThreshold) {
						LOG.info("SKIPPING: "+c+" ; too similar to "+n2+" SIM: "+sim);
						skip = true;
						break;
					}
				}
			}
			if (skip) {
				// Note: we do not add this to the skip cache here,
				// as this is dependent on other elements
				continue;
			}
			
			// not filtered
			ops.add(c);
		}

		OWLClassExpression lcsx;
		if (ops.size() == 1) {
			// only one passes
			lcsx = ops.iterator().next();
		}
		else if (ops.size() == 0) {
			// none pass: choose the best representative of the intersection
			lcsx = best;
		}
		else {
			lcsx = getOWLDataFactory().getOWLObjectIntersectionOf(ops);
		}
		csetToExpressionMap.put(ccs, lcsx);
		return lcsx;
	}

	/**
	 * generates a LCS expression and makes it a class if it is a class expression
	 * 
	 * @param a
	 * @param b
	 * @param leafClasses 
	 * @return named class representing LCS
	 */
	public OWLClass getLowestCommonSubsumerClass(OWLClassExpression a, OWLClassExpression b, Set<OWLClass> leafClasses) {
		
		OWLClassExpressionPair pair = new OWLClassExpressionPair(a,b);
		if (lcsCache.containsKey(pair)) {
			return lcsCache.get(pair);
		}

		OWLClassExpression x = getLowestCommonSubsumer(a,b,leafClasses);
		OWLClass lcs;
		/*
		if (lcsExpressionToClass.containsKey(x)) {
			lcs = lcsExpressionToClass.get(x);
		}
		*/
		if (x instanceof OWLClass)
			lcs = (OWLClass)x;
		else if (x instanceof OWLObjectIntersectionOf)
			lcs = makeClass((OWLObjectIntersectionOf) x);
		else
			lcs = null;
		lcsCache.put(pair, lcs);
		//LOG.info("LCS of "+a+" + "+b+" = "+lcs);
		return lcs;
	}
	
	/**
	 * given a CE of the form A1 and A2 and ... An,
	 * get a class with an IRI formed by concatenating parts of the IRIs of Ai,
	 * and create a label assertion, where the label is formed by concatenating label(A1)....
	 * 
	 * note that the reasoner will need to be synchronized after new classes are made
	 * 
	 * Note: modifies ontology but does not flush
	 * 
	 * @see materializeClassExpression(..)
	 * 
	 * @param x
	 * @return class
	 */
	public OWLClass makeClass(OWLObjectIntersectionOf x) {
		if (materializedClassExpressionMap.containsKey(x)) {
			return materializedClassExpressionMap.get(x);
		}
		//StringBuffer id = new StringBuffer();
		StringBuffer label = new StringBuffer();
		int n = 0;
		int nlabels = 0;
		
		LOG.info("LCS INTERSECTION: "+x);

		IRI intersectionIRI = null;
		// TODO - optimize following
		for (OWLClassExpression op : x.getOperands()) {
			n++;
			// throw exception if ops are not named
			OWLClass opc = (OWLClass)op;
			//String opid = opc.getIRI().toString();
			String oplabel = getAnyLabel(opc);

			if (n == 1) {
				intersectionIRI = opc.getIRI();
				// make base
				/*
				String prefix = opid.toString();
				prefix = prefix.replaceAll("#.*","#");
				if (prefix.startsWith(OBO_PREFIX))
					id.append(OBO_PREFIX);
				else 
					id.append(prefix);
					*/				
			}
			else {
				intersectionIRI = makeViewClassIRI(intersectionIRI, opc.getIRI());
			}
			
			if (n > 1) {
				label.append(" and ");
			}
			if (oplabel != null) {
				nlabels++;
				label.append("["+oplabel+"]");
			}
			else {
				label.append("?"+opc.getIRI().toString());
			}

		}
		OWLClass c = getOWLDataFactory().getOWLClass(intersectionIRI);
		Set<OWLAxiom> newAxioms = new HashSet<OWLAxiom>();
		newAxioms.add( getOWLDataFactory().getOWLEquivalentClassesAxiom(c, x) );
		newAxioms.add( getOWLDataFactory().getOWLDeclarationAxiom(c));
		//LOG.info(" Generated label("+c+")="+label.toString());
		if (nlabels > 0) {
			newAxioms.add( getOWLDataFactory().getOWLAnnotationAssertionAxiom(getOWLDataFactory().getRDFSLabel(), c.getIRI(), 
					getOWLDataFactory().getOWLLiteral(label.toString())));
		}
		//TODO
		//lcsExpressionToClass.put(x, c);
		LOG.info(" new LCS: "+c+" label: "+label.toString()+" == "+x);
		this.addAxiomsToOutput(newAxioms, false);
		materializedClassExpressionMap.put(x, c);
		return c;
	}

	
	// find similarity for purposes of determining if a LCS intersectionOf should be created.
	// E.g. morphology AND inheres_in some limb is good as the two do not have much in the way
	// of shared ancestors. Also "bone and part_of some limb" also good, but these will have
	// more shared ancestors depending on how the ontology is structured, upper ontologies etc.
	// note we can't use the ABox as these may be "inner" expressions with no named individuals
	// in the ontology (e.g. "bone"). Using descendant named classes may introduce more graph bias.
	private float getMinAsymmetricJaccardIndex(OWLClassExpression a, OWLClassExpression b) {
		// if a subsumes b, then SimMAJ(a,b) = 1
		// 'rewards' pairs that are distant from eachother
		Set<Node<OWLClass>> ci = getNamedCommonSubsumers(a,b); // a /\ b
		int div = Math.min(getNamedReflexiveSubsumers(a).size(), getNamedReflexiveSubsumers(b).size());
		return ci.size() / (float)div;
	}

	


}
