package owltools.sim;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;

import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;
import owltools.graph.OWLQuantifiedProperty;
import owltools.sim.SimEngine.SimilarityAlgorithmException;

/**
 * This is the standard method to use when comparing entities with multiple attributes.
 * 
 * This method will calculate all x all feature attributes, and then generate an aggregate
 * score for the entity pair. This is done using a "sub-similarity" method.
 * 
 * For all best-matching attributes (in both directions) an additional "deep similarity" check
 * is performed. Calculating this may be more computationally expensive than the method used to
 * do the all by all comparison.
 * 
 * The final score is the average of all best-matching attributes by the sub-similarity method.
 * TODO: also provide average of all best-matching by deep-sim method
 * 
 * This method is configurable.
 * 
 * By default, AsymmetricJaccardSimilarity is used to calculate all x all attributes for the entity pair.
 * 
 * ConjunctiveSetInformationContentRatioSimilarity is used for the "deep simularity" check - i.e. by default
 * all the best matching attributes scored by AsymmetricJaccardSimilarity will be compared by
 * ConjunctiveSetInformationContentRatioSimilarity.
 * 
 * The average of all ConjunctiveSetInformationContentRatioSimilarity scores for all best matches is
 * similar to avgICCS in Washington et al. The difference is that ConjunctiveSetInformationContentRatioSimilarity
 * can include dynamic LCSs.
 * 
 * This method can also be used to generate the maxIC - simply take the maximum of the IC of all 
 * reported ConjunctiveSetInformationContentRatioSimilarity values
 * 
 * @author cjm
 *
 */
public class MultiSimilarity extends Similarity {

	private static Logger LOG = Logger.getLogger(MultiSimilarity.class);

	public String[] deepSimMethods = 
	{
			"ConjunctiveSetInformationContentRatioSimilarity"
			//"ConjunctiveSetSimilarity"			
	};
	public String subSimMethod = "AsymmetricJaccardSimilarity";
	public String preSimMethod = "MaximumInformationContentSimilarity"; // not implemented yet

	public double aScore;
	public double bScore;
	
	public Map<OWLObject,Set<OWLObject>> featureToAttributeMap;
	Map<OWLObject,Similarity> aBest;
	Map<OWLObject,Similarity> bBest;
	Map<OWLObjectPair,List<Similarity>> deepSimMap;
	Map<String,Double> bestDeepSimScore;
	Map<String,OWLObjectPair> bestDeepSimPair;

	class ValueComparator implements Comparator {

		Map<?, ? extends Similarity> base;
		public ValueComparator(Map<?, ? extends Similarity> base) {
			this.base = base;
		}

		public int compare(Object a, Object b) {
			double scoreA = (base.get(a)).score;
			double scoreB = (base.get(b)).score;
			if(scoreA < scoreB) {
				return 1;
			} else if(scoreA == scoreB) {
				return 0;
			} else {
				return -1;
			}
		}
	}



	public String getSubSimMethod() {
		return subSimMethod;
	}

	public void setSubSimMethod(String subSimMethod) {
		this.subSimMethod = subSimMethod;
	}

	public String getPreSimMethod() {
		return preSimMethod;
	}

	public void setPreSimMethod(String preSimMethod) {
		this.preSimMethod = preSimMethod;
	}

	@Override
	public void calculate(SimEngine simEngine, OWLObject a, OWLObject b)
	throws SimilarityAlgorithmException {
		this.simEngine = simEngine;
		createFeatureToAttributeMap();
		calculate(a,b,featureToAttributeMap.get(a),featureToAttributeMap.get(b));
	}

	private void calculate(OWLObject a, OWLObject b, Set<OWLObject> aAtts,
			Set<OWLObject> bAtts) throws SimilarityAlgorithmException {

		LOG.debug(a+" size="+aAtts.size());
		LOG.debug(b+" size="+bAtts.size());
		if (aAtts.size() == 0 || bAtts.size() ==0) {
			isComparable = false;
			return;
		}
		Map<OWLObjectPair,Double> smap = new HashMap<OWLObjectPair,Double>();

		aBest = new HashMap<OWLObject,Similarity>();
		bBest = new HashMap<OWLObject,Similarity>();
		int ia = 0;

		LOG.info("INIT: Calculating all by all for attributes of object pair...");
		// all by all comparison of each attribute in each feature
		for (OWLObject aAtt : aAtts) {
			LOG.debug(" aAtt="+aAtt+" "+ia+"/"+aAtts.size());
			ia++;
			int ib = 0;
			for (OWLObject bAtt : bAtts) {
				LOG.debug(" bAtt="+bAtt+" "+ib+"/"+bAtts.size());
				ib++;
				Similarity ss = simEngine.getSimilarityAlgorithm(subSimMethod);
				ss.calculate(simEngine, aAtt, bAtt);
				ss.a = aAtt;
				ss.b = bAtt;
				Double sc = ss.getScore();
				LOG.debug("DONE; SCORE="+sc+" in: "+aAtt+" -vs "+bAtt);

				smap.put(new OWLObjectPair(aAtt,bAtt), sc);
				if (!aBest.containsKey(aAtt) ||
						sc > aBest.get(aAtt).getScore()) {
					aBest.put(aAtt, ss);
				}
				if (!bBest.containsKey(bAtt) ||
						sc > bBest.get(bAtt).getScore()) {
					bBest.put(bAtt, ss);
				}
			}
		} // DONE all-by-all
		LOG.info("DONE: Calculating all by all for attributes of object pair...");

		// now calculate score based on BEST of each
		double totalSc = 0.0;
		int n = 0;
		Set <OWLObject>allAtts = new HashSet<OWLObject>();
		allAtts.addAll(aAtts);
		allAtts.addAll(bAtts);

		// do further analysis on best scoring matches
		if (deepSimMethods.length > 0) {
			deepSimMap = new HashMap<OWLObjectPair,List<Similarity>>();
			bestDeepSimScore = new HashMap<String,Double>();
			bestDeepSimPair = new HashMap<String,OWLObjectPair>(); // todo - make a set for ties
			for (String deepSimMethod : deepSimMethods) {
				double bestScore = 0.0;
				for (OWLObject att : aAtts) {
					Similarity s1 = aBest.get(att);
					Similarity s2 = getDeepSim(deepSimMethod, s1.a,s1.b);
					OWLObjectPair pair = new OWLObjectPair(s1.a, s1.b);
					if (!deepSimMap.containsKey(pair))						
						deepSimMap.put(pair, new Vector<Similarity>());
					deepSimMap.get(pair).add(s2);
					if (s2.getScore() > bestScore) {
						bestScore = s2.getScore();
						this.bestDeepSimPair.put(deepSimMethod, pair);
					}
				}

				for (OWLObject att : bAtts) {
					Similarity s1 = bBest.get(att);
					Similarity s2 = getDeepSim(deepSimMethod,s1.b,s1.a);
					OWLObjectPair pair = new OWLObjectPair(s1.b, s1.a);
					if (!deepSimMap.containsKey(pair))						
						deepSimMap.put(pair, new Vector<Similarity>());
					deepSimMap.get(pair).add(s2);
					if (s2.getScore() > bestScore) {
						bestScore = s2.getScore();
						this.bestDeepSimPair.put(deepSimMethod, pair);
					}
				}
				bestDeepSimScore.put(deepSimMethod, bestScore);

			}

			//ss.calculate(simEngine, aAtt, bAtt);

		}

		int nA = 0;
		int nB = 0;
		aScore = 0.0; // highest when phenotypes of a are subsumed by phenotypes of b
		bScore = 0.0; // highest when phenotypes of b are subsumed by phenotypes of a
		for (OWLObject att : aAtts) {
			double s =  aBest.get(att).getScore();
			totalSc += s;
			aScore += s;
			nA++;
			n++;
		}
		for (OWLObject att : bAtts) {
			double s = bBest.get(att).getScore();
			totalSc += s;
			bScore += s;
			nB++;
			n++;
		}
		aScore /= nA;
		bScore /= nB;



		//ss.calculate(simEngine, aAtt, bAtt);


		this.score = totalSc / n;
	}

	/**
	 * uses deepSimMethod to calculate similarity between attributes a and b
	 * 
	 * @param deepSimMethod
	 * @param a
	 * @param b
	 * @return
	 * @throws SimilarityAlgorithmException
	 */
	private Similarity getDeepSim(String deepSimMethod, OWLObject a, OWLObject b) throws SimilarityAlgorithmException {
		Similarity s = simEngine.getSimilarityAlgorithm(deepSimMethod);
		s.calculate(simEngine, a, b);
		return s;
	}

	/**
	 * build mapping between features (e.g. organisms) and attributes (e.g. phenotypes)
	 * 
	 */
	private void createFeatureToAttributeMap() {
		featureToAttributeMap = new HashMap<OWLObject,Set<OWLObject>>();
		featureToAttributeMap.put(a, simEngine.getAttributesFor(a));
		featureToAttributeMap.put(b, simEngine.getAttributesFor(b));
	}


	public Set<OWLObject> sortMapByScore(Map<OWLObject,Similarity> map) {
		ValueComparator bvc =  new ValueComparator(map);
		TreeMap<OWLObject,Similarity> sorted_map = new TreeMap(bvc);
		sorted_map.putAll(map);
		return sorted_map.keySet();
	}

	public void report(Reporter r) {
		r.report(this, "feature_pair_score", a, b, score);
		r.report(this, "feature_pair_score_a", a, b, aScore);
		r.report(this, "feature_pair_score_b", a, b, bScore);
		
		if (bestDeepSimScore != null) {
			for (String m : bestDeepSimScore.keySet()) {
				r.report(this, "feature_pair_best_score",
						m,a,b,
						bestDeepSimScore.get(m),
						bestDeepSimPair.get(m).getA(),
						bestDeepSimPair.get(m).getB());
			}
		}

		Vector<OWLObjectPair> pairs = new Vector<OWLObjectPair>();
		for (OWLObject bestMatchForA : sortMapByScore(aBest)) {
			OWLObject bestMatchInB = aBest.get(bestMatchForA).b;
			r.report(this, "feature_pair_best_attribute_match_A",
					a,
					b,
					bestMatchForA,
					bestMatchInB);
			OWLObjectPair pair = new OWLObjectPair(bestMatchForA,bestMatchInB);
			pairs.add(pair);
			aBest.get(bestMatchForA).report(r);
			if (deepSimMap != null && deepSimMap.containsKey(pair)) {
				for (Similarity ds : deepSimMap.get(pair)) {
					ds.report(r);
				}
			}
		}
		for (OWLObject bestMatchForB : sortMapByScore(bBest)) {
			OWLObject bestMatchInA = bBest.get(bestMatchForB).a;
			r.report(this, "feature_pair_best_attribute_match_B",
					a,
					b,
					bestMatchForB,
					bestMatchInA);
			OWLObjectPair pair = new OWLObjectPair(bestMatchForB,bestMatchInA);
			pairs.add(pair);
			bBest.get(bestMatchForB).report(r);
			if (deepSimMap != null && deepSimMap.containsKey(pair)) {
				for (Similarity ds : deepSimMap.get(pair)) {
					ds.report(r);
				}
			}
		}
		/*
		for (OWLObjectPair pair : pairs) {
			if (deepSimMap != null && deepSimMap.containsKey(pair)) {
				for (Similarity ds : deepSimMap.get(pair)) {
					ds.report(r);
				}
			}
		}
		 */
	}


	public void print(PrintStream s) {
		s.print("COMPARISON:");
		print(s,a);
		s.print(" vs ");
		print(s,b);
		s.println();
		if (!isComparable) {
			s.println("**not comparable");
			return;
		}
		s.println("AVG-BEST: "+score);
		s.println("AVG-BEST-A: "+aScore);
		s.println("AVG-BEST-B: "+bScore);
		s.println("BEST-MATCHES(A): "+aBest.keySet().size());
		for (OWLObject att : sortMapByScore(aBest)) {
			printX(s,att,aBest,bBest,aBest.get(att).b);
		}
		s.println("BEST-MATCHES(B): "+bBest.keySet().size());
		for (OWLObject att : sortMapByScore(bBest)) {
			printX(s,att,bBest,aBest,bBest.get(att).a);
		}
	}

	/**
	 * @param s
	 * @param att  -- attribute matched
	 * @param bestMap
	 * @param bestMap2 -- map in opposite direction
	 * @param bestMapObj -- opposite attribute
	 */
	private void printX(PrintStream s, OWLObject att, Map<OWLObject, Similarity> bestMap, Map<OWLObject, Similarity> bestMap2, OWLObject bestMapObj) {
		Similarity bestmatch = bestMap.get(att);
		s.println("  Attr Pair Score: "+bestmatch.getScore());
		//if (bestMap2.get(att).score == bestmatch.getScore())
		//	s.println("  **reciprocal**");
		s.print("  ");
		printDescription(s,att);
		s.print(" -vs- ");
		printDescription(s,bestMapObj);
		s.println();
		s.print("  A:");
		s.println(att);
		s.print("  B:");
		s.print(bestMapObj);
		s.println();
		printSubSim(s, bestmatch);
		OWLObjectPair pair = new OWLObjectPair(att,bestMapObj);
		Set<Similarity> alreadyPrinted = new HashSet<Similarity>();
		if (deepSimMap != null && deepSimMap.containsKey(pair)) {
			for (Similarity ds : deepSimMap.get(pair)) {
				if (alreadyPrinted.contains(ds))
					continue;
				printSubSim(s, ds);
				alreadyPrinted.add(ds);
			}
		}
		s.println();

	}

	private void printSubSim(PrintStream s, Similarity subSim) {
		// TODO - DRY
		s.print("  "+subSim.getClass().toString().replaceAll(".*\\.", ""));
		if (subSim instanceof DescriptionTreeSimilarity) {
			s.print("  Shared:");
			printDescription(s, ((DescriptionTreeSimilarity)subSim).lcs);
		}
		else if (subSim instanceof ConjunctiveSetSimilarity) {
			s.print("  Shared:");
			for (OWLObject x : ((ConjunctiveSetSimilarity)subSim).bestSubsumers) {
				print(s,x);
				s.print(" ");
			}
		}
		else if (subSim instanceof ConjunctiveSetInformationContentRatioSimilarity) {
			s.print("  Shared:");
			for (OWLObject x : ((ConjunctiveSetInformationContentRatioSimilarity)subSim).lcsIntersectionSet) {
				print(s,x);
				s.print(" ");
			}
			s.print("LCS_IC_Ratio:"+((ConjunctiveSetInformationContentRatioSimilarity)subSim).lcsICRatio);
		}
		s.print(" Score:"+subSim.getScore());
		s.println();

	}

	/**
	 * adds additional axioms specific to this method.
	 * Creates a named LCS class equivalent to the generated expression
	 * 
	 * @param id
	 * @param result
	 * @param axioms
	 */
	@Override
	protected void translateResultsToOWLAxioms(String id, OWLNamedIndividual result, Set<OWLAxiom> axioms) {
		OWLDataFactory df = simEngine.getGraph().getDataFactory();

		OWLAnnotationProperty pa = df.getOWLAnnotationProperty(annotationIRI("has_best_match_for_object_1"));
		OWLAnnotationProperty pb = df.getOWLAnnotationProperty(annotationIRI("has_best_match_for_object_2"));

		for (OWLObject att : aBest.keySet()) {
			if (!(att instanceof OWLNamedObject)) {
				System.err.println("skipping");
				continue;
			}
			axioms.addAll(aBest.get(att).translateResultsToOWLAxioms());
			axioms.add(df.getOWLAnnotationAssertionAxiom(pa, result.getIRI(), aBest.get(att).persistentIRI));
		}
		for (OWLObject att : bBest.keySet()) {
			if (!(att instanceof OWLNamedObject)) {
				System.err.println("skipping");
				continue;
			}
			axioms.addAll(bBest.get(att).translateResultsToOWLAxioms());
			axioms.add(df.getOWLAnnotationAssertionAxiom(pb, result.getIRI(), bBest.get(att).persistentIRI));
		}
		
		if (deepSimMap != null) {
			for (List<Similarity> ss : deepSimMap.values()) {
				for (Similarity s : ss) {
					axioms.addAll(s.translateResultsToOWLAxioms());
				}
			}
		}

	}


}
