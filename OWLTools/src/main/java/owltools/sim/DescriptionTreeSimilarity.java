package owltools.sim;

import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLPropertyRange;
import org.semanticweb.owlapi.model.OWLQuantifiedRestriction;
import org.semanticweb.owlapi.model.OWLQuantifiedObjectRestriction;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;
import owltools.graph.OWLQuantifiedProperty;
import owltools.graph.OWLQuantifiedProperty.Quantifier;
import owltools.sim.OWLObjectPair;
import owltools.sim.SimEngine.SimilarityAlgorithmException;

/**
 * A similarity between two entities constructed by threading two description trees together.
 * 
 * ALGORITHM:
 * 
 * <pre>
 * <code>
 * build(a,b) :
 *   N = {}
 *   foreach direct parent edge Ea:
 *     find closest match for Ea.tgt in all reflexive ancestors of b
 *     Eb = edge between b and b'
 *     LCS = all least common reachable ancestors of a,b
 *     if |LCS| = 0 : n = null
 *              = 1 : n = < LCS[1] Ea Eb>
 *              > 1 : n = build(Ea.t,Eb.t)
 *                    extend n.Ea + Ea
 *                    extent n.Eb + Eb
 *      add n to N
 *      
 *   N' = map { pathToExpression } N
 *   if |N| = 0 : return null
 *          = 1 : return N'[1]
 *          > 1 : return IntersectionOf(N')
 *  </code>
 *  </pre>
 *   
 *   
 * @author cjm
 *
 */
public class DescriptionTreeSimilarity extends Similarity {
	private static Logger LOG = Logger.getLogger(DescriptionTreeSimilarity.class);

	OWLClassExpression lcs;
	OWLGraphWrapper graph;
	SimEngine se;
	// move to config?
	public boolean forceReflexivePropertyCreation = true;
	Set<OWLObject> visited = new HashSet<OWLObject>();
	Set<OWLObjectPair> visitedPairs = new HashSet<OWLObjectPair>();
	
	@Override
	public void calculate(SimEngine simEngine, OWLObject a, OWLObject b) throws SimilarityAlgorithmException {
		this.a = a;
		this.b = b;
		this.simEngine = simEngine;
		se = simEngine; // TODO - fix
		graph = simEngine.getGraph();
		ConvergentPath cp = buildDescription(a,b);
		lcs = combinePathsToMakeExpression(cp);
		if (cp == null)
			score = 0.0;
		else {
			Set<OWLObject> csl = graph.getAncestorsReflexive(lcs);
			Set<OWLObject> usl = se.getUnionSubsumers(a,b);
			
			score = ((double)csl.size()) / usl.size();
		}
	}


	private class ConvergentPath {
		OWLClassExpression x;
		OWLGraphEdge ea;
		OWLGraphEdge eb;
		double score = 0;

		public String toString() {
			return x.toString()+" EA:"+ea+" EB:"+eb;
		}
	}
	
	public double quickScore(Set<OWLObject> csl, Set<OWLObject> usl) {
		// we don't want to weight heavily against mismatches; the b tree
		// may be much 'bushier'
		// (see for example 'substantia nigra degenerates' vs 'Substantia nigra 291' in PKB
		//  - the latter has axon, dendrite and neuropil all degenerate)
		return ((double) (csl.size() * csl.size())) / usl.size();

	}

	/**
	 * Recursively build a class expression that subsumes a and b.
	 * 
	 * Algorithm: traverse description tree a, one edge at a time, finding the best subtree in b that
	 * matches. if there are multiple paths from a leading to a LCS, create an intersection expression.
	 */
	public ConvergentPath buildDescription(OWLObject a, OWLObject b) {
		Set<OWLObject> fullLCSs = se.getLeastCommonSubsumers(a,b);
		LOG.info("orig LCSs="+fullLCSs);
		return buildDescription(a,b,fullLCSs);
	}

	
	public ConvergentPath buildDescription(OWLObject a, OWLObject b, Set<OWLObject> fullLCSs) {

		//if (visited.contains(a))
		//	return null;
		

		LOG.info("building:"+a+" vs "+b);
		
		if (fullLCSs.size() == 1) {
			// edge case - should only happen on initial call when a<b or b<a
			ConvergentPath nextCp = new ConvergentPath();
			
			OWLObject x = fullLCSs.iterator().next();
			Set<OWLGraphEdge> extEdges = graph.getEdgesBetween(a, x);
			if (extEdges.size() > 0) {
				nextCp.ea = extEdges.iterator().next();
			}
			Set<OWLGraphEdge> bExtEdges = graph.getEdgesBetween(b,x);
			if (bExtEdges.size() > 0) {
				nextCp.eb = bExtEdges.iterator().next();
			}
			Set<OWLObject> csl = se.getCommonSubsumers(a, b);			
			Set<OWLObject> usl = se.getUnionSubsumers(a, b);

			// TODO
			nextCp.score = quickScore(csl,usl) * 10;
			nextCp.x = (OWLClassExpression) x;
			return nextCp;
		}


		// candidate expression
		OWLClassExpression x = null;

		ConvergentPath cp = new ConvergentPath();

		// convergence
		if (a.equals(b)) {
			LOG.info("   IDENTICAL");
			// TODO - move this to graphwrapper
			cp.x = makeClassExpression(a);
			cp.score = 1;
			return cp;
			//return combinePathsToMakeExpression(a, ea, eb);
		}

		// TODO - cache results
		OWLObjectPair pair = new OWLObjectPair(a,b);
		if (visitedPairs.contains(pair)) {
			LOG.info("already visited: "+pair);
			return null;
		}

		//visited.add(a);
		visitedPairs.add(pair);

		// iterate through all edges leading from a
		// TODO - only extend when no intersection is built
		Set<OWLClassExpression> subExprs = new HashSet<OWLClassExpression>();
		Set<ConvergentPath> subCps = new HashSet<ConvergentPath>();
		Set<OWLGraphEdge> aEdges = graph.getPrimitiveOutgoingEdges(a);
		LOG.info("  num edges to explore:"+aEdges.size());
		for (OWLGraphEdge aEdge : aEdges) {
			//OWLGraphEdge eaNew = graph.combineEdgePair(ea.getSource(), ea, e, 1);
			OWLObject aNext = aEdge.getTarget();
			if (se.nonSignificantObjects().contains(aNext))
				continue;
			//if (visited.contains(aNext))
			//	continue;
			//if (visitedPairs.contains(new OWLObjectPair(aNext,b)))
			
			LOG.info("  edge: "+aEdge);
			LOG.info("  fetching best match for "+aNext+" under: "+b);
			// get best match for a in {b, desc-of-b}
			// also - restrict subsumers
			// TODO
			OWLObject bNext = b;
			OWLGraphEdge bEdge = null;
			double best = 0;
			// a more efficient strategy would be to start at b and work up.
			// if parents are less well matched than child then prune branch - todo
			Set<OWLObject> bCandidates = graph.getAncestorsReflexive(b);
			if (bCandidates.contains(aNext)) {
				bCandidates.clear();
				bCandidates.add(aNext);
				LOG.info("exact match - optimized - using:"+bCandidates);
			}
			for (OWLObject candidate : bCandidates) {
				Set<OWLGraphEdge> bEdges = graph.getEdgesBetween(b, candidate);
				OWLGraphEdge candidateEdge = bEdges.iterator().next();

				// TODO - optimize - we only need to calculate aNext's subsumers once
				Set<OWLObject> csl = se.getCommonSubsumers(aNext, candidate);			
				Set<OWLObject> usl = se.getUnionSubsumers(aNext, candidate);
				
				//Set<OWLObject> lcsl = se.makeNonRedundant(csl);

				// we don't want to weight heavily against mismatches; the b tree
				// may be much 'bushier'
				// (see for example 'substantia nigra degenerates' vs 'Substantia nigra 291' in PKB
				//  - the latter has axon, dendrite and neuropil all degenerate)
				double score = quickScore(csl, usl);

				//score *= lcsl.size(); // assume LCSs independent
				//double score = csl.size();
				// todo - better edge comparison
				LOG.debug("    candidate: "+candidate+" |CSL|="+csl.size()+" score:"+score+" candidateEdge:"+candidateEdge);
				// TODO - better way of comparing expressions and named entities.
				// for now we only give bonues to matching property edges
				if (candidateEdge.getQuantifiedPropertyList().equals(aEdge.getQuantifiedPropertyList()) &&
						!aEdge.getSingleQuantifiedProperty().isSubClassOf()) {
					LOG.debug("      **edge match");
					score *= 1.5;
				}
				//LOG.info("    bEdges: "+bEdges);
				//OWLGraphEdge bEdge = bEdges.iterator().next();
				if (score > best) {
					best = score;
					bNext = candidate;
					bEdge = candidateEdge;
				}
			}
			if (best == 0)
				continue;

			LOG.info("  bNext: "+bNext+" sc:"+best+" EDGE:"+bEdge);

			Set<OWLObject> lcsl = se.getCommonSubsumers(aNext, bNext);
			LOG.info("    lcsl: "+lcsl);
			LOG.info("    current full size: "+fullLCSs.size());
			// cloning not necessary - TODO
			//lcsl.retainAll(new HashSet<OWLObject>(fullLCSs));
			//LOG.info("    lcsl, full: "+lcsl);

			if (lcsl.size() == 0)
				continue;

			// what do we do about multiple edges?
			// ideally choose the one closest to ea.
			// for now choose arbitrary one
			//Set<OWLGraphEdge> bEdges = graph.getEdgesBetween(b, bNext);
			//LOG.info("    bEdges: "+bEdges);
			//OWLGraphEdge bEdge = bEdges.iterator().next();
			//OWLGraphEdge ebNew = graph.combineEdgePair(eb.getSource(), eb,
			//		bEdges.iterator().next(), 1);

			ConvergentPath nextCp;
			if (lcsl.size() == 1) {
				// convergence; no need to recurse further
				nextCp = new ConvergentPath();
				nextCp.x = makeClassExpression(lcsl.iterator().next());
				Set<OWLGraphEdge> extEdges = graph.getEdgesBetween(aNext, nextCp.x);
				if (extEdges.size() == 0) {
					nextCp.ea = aEdge;
				}
				else {
					OWLGraphEdge extEdge = extEdges.iterator().next();
					nextCp.ea = graph.combineEdgePair(aEdge.getSource(), aEdge, extEdge, 1);
					//LOG.info(" CONVERGE:"+nextCp.x+" combine:"+e+" * "+extEdge+" = "+nextCp.ea);
				}
				Set<OWLGraphEdge> bExtEdges = graph.getEdgesBetween(bNext, nextCp.x);
				if (bExtEdges.size() == 0) {
					nextCp.eb = bEdge;
				}
				else {
					OWLGraphEdge extEdge = bExtEdges.iterator().next();
					nextCp.eb = graph.combineEdgePair(b, bEdge, extEdge, 1);
					//LOG.info(" CONVERGE:"+nextCp.x+" combine:"+e+" * "+extEdge+" = "+nextCp.ea);
				}
				//nextCp.eb = bEdge;
				//nextExpr = combinePathsToMakeExpression(lcsl.iterator().next(),e,bEdge);
				nextCp.score = best;
			}
			else {
				nextCp = buildDescription(aNext, bNext, lcsl);
				if (nextCp == null) {
					LOG.info("no description for "+aNext+" vs "+bNext);
					continue;
				}
				nextCp.score += best;

				if (nextCp.ea == null)
					nextCp.ea = aEdge;
				else 
					nextCp.ea = graph.combineEdgePair(a, aEdge, nextCp.ea, 1);
				
				if (nextCp.eb == null)
					nextCp.eb = bEdge;
				else 
					nextCp.eb = graph.combineEdgePair(b, bEdge, nextCp.eb, 1);
			}
			LOG.info("adding sub-CP "+nextCp);
			subCps.add(nextCp);
		}

		LOG.info("finished edges "+a+" vs "+b+" subCps.size=="+subCps.size());
		cp.score = 0;
		if (subCps.size() == 0) {
			return null;

		}
		else if (subCps.size() == 1) {
			cp = subCps.iterator().next();

			// edges must be extended here
			//x = subExprs.iterator().next();
		}
		else {
			// TODO - edges combined and reset here
			subExprs = new HashSet<OWLClassExpression>();
			LOG.info("combining sub-expressions for "+a+" vs "+b);
			for (ConvergentPath subCp : subCps) {
				LOG.info("  sub-CP:"+subCp);
				subExprs.add(combinePathsToMakeExpression(subCp));
				cp.score += subCp.score;
			}
			cp.x = graph.getDataFactory().getOWLObjectIntersectionOf(subExprs);
			if (a instanceof OWLNamedIndividual)
				cp.ea = new OWLGraphEdge(a,cp.x,Quantifier.INSTANCE_OF);
			else
				cp.ea = new OWLGraphEdge(a,cp.x,Quantifier.SUBCLASS_OF);
			if (b instanceof OWLNamedIndividual)
				cp.eb = new OWLGraphEdge(b,cp.x,Quantifier.INSTANCE_OF);
			else
				cp.eb = new OWLGraphEdge(b,cp.x,Quantifier.SUBCLASS_OF);

		}
		
		return cp;
	}

	private OWLClassExpression combinePathsToMakeExpression(ConvergentPath cp) {
		LOG.info("COMBINE:"+cp);
		if (cp == null) {
			return graph.getDataFactory().getOWLNothing();
		}
		return combinePathsToMakeExpression(cp.x, cp.ea, cp.eb);
	}

	/**
	 * given two paths to the same node..
	 * TODO - also include from tips of edges to LCS..?
	 * 
	 * @param x
	 * @param ea
	 * @param eb
	 * @return
	 */
	private OWLClassExpression combinePathsToMakeExpression(OWLObject x, OWLGraphEdge ea, OWLGraphEdge eb) {
		LOG.info("combining, tgt="+x+" EA="+ea+" EB="+eb);
		Set<OWLClassExpression> args = new HashSet<OWLClassExpression>();
		if (ea == null) {
			ea = new OWLGraphEdge(null,x,new Vector<OWLQuantifiedProperty>(),null);
			ea.setTarget(x);
		}
		if (eb == null) {
			eb = new OWLGraphEdge(null,x,new Vector<OWLQuantifiedProperty>(),null);
			eb.setTarget(x);
		}
		LOG.info("making Union of "+ea+" and "+eb);
		if (!ea.getTarget().equals(x)) {
			LOG.info("##EA no match!!");
		}
		if (!eb.getTarget().equals(x)) {
			LOG.info("##EB no match!!");
		}
		return makeUnion(ea,eb);

	}

	private OWLClassExpression makeUnion(OWLGraphEdge ea, OWLGraphEdge eb) {
		OWLClassExpression xa = (OWLClassExpression)graph.edgeToTargetExpression(ea);
		OWLClassExpression xb = (OWLClassExpression)graph.edgeToTargetExpression(eb);
		return makeUnionWithReduction(xa,xb);
	}

	private OWLClassExpression makeUnionWithReduction(OWLClassExpression xa, OWLClassExpression xb) {
		if (xa.equals(xb))
			return xa;
		OWLClassExpression rx = makeUnionUsingReflexiveProperty(xa,xb);
			
		if (rx == null)
			rx = makeUnionUsingReflexiveProperty(xb,xa);
		if (rx == null) {
			rx = makeUnionBasic(xa,xb);
		}
		return rx;		
	}
	
	private OWLObjectPropertyExpression propertySubsumer(OWLObjectPropertyExpression p1, OWLObjectPropertyExpression p2) {
		if (p1.equals(p2))
			return p1;
		// TODO - check for topObjectProp?
		if (graph.getSuperPropertiesOf(p1).contains(p2)) {
			return p2;
		}
		if (graph.getSuperPropertiesOf(p2).contains(p1)) {
			return p1;
		}
		return null;
	}

	/**
	 * makes a reduced union expression.
	 * 
	 * Uses the following two reduction rules:
	 * 
	 * (r1 some X) U (r2 some Y) ==> lcs(r1,r2) some MakeUnionOf(X,Y)
	 * (r1 some X) U X ==> reflexive-version-of-r1 some X
	 * 
	 * TODO: test for (r some r some X) u (r some X) cases. needs to be done over final expression.
	 *  
	 * if a reduced form cannot be made, returns null
	 * 
	 * @param xa
	 * @param xb
	 * @return
	 */
	private OWLClassExpression makeUnionUsingReflexiveProperty(OWLClassExpression xa, OWLClassExpression xb) {
		LOG.info("testing if there is a more compact union expression for "+xa+" ++ "+xb);
		OWLDataFactory df = graph.getDataFactory();
		if (xa instanceof OWLQuantifiedRestriction) {
			// TODO - check before casting
			OWLObjectProperty prop = (OWLObjectProperty) ((OWLQuantifiedRestriction) xa).getProperty();
			OWLClassExpression xaRest = (OWLClassExpression) ((OWLQuantifiedRestriction)xa).getFiller();
			if (xb instanceof OWLQuantifiedRestriction) {
				OWLObjectPropertyExpression p2 =
					propertySubsumer(prop, 
						((OWLQuantifiedObjectRestriction) xb).getProperty());
				
				if (p2 != null) {
					OWLClassExpression xbRest = (OWLClassExpression) ((OWLQuantifiedRestriction)xb).getFiller();
					OWLClassExpression x = makeUnionWithReduction(xaRest,xbRest);
					// todo - mixing some and all
					if (xa instanceof OWLObjectSomeValuesFrom)
						return df.getOWLObjectSomeValuesFrom(p2,x);
					else if (xa instanceof OWLObjectAllValuesFrom)
						return df.getOWLObjectAllValuesFrom(p2, x);
				}
			}
			LOG.info("  test: "+xaRest+" == "+xb);
			

			if (xaRest.equals(xb)) {
				LOG.info("     TRUE: "+xaRest+" == "+xb);

				OWLObjectProperty rprop = null;
				if (graph.getIsReflexive(prop)) {
					rprop = prop;
				}
				if (forceReflexivePropertyCreation) {
					OWLOntologyManager manager = graph.getManager();
					OWLOntology ont = graph.getSourceOntology();
					rprop = 
						df.getOWLObjectProperty(IRI.create(prop.getIRI().toString()+"_reflexive"));
					manager.applyChange(new AddAxiom(ont, df.getOWLSubObjectPropertyOfAxiom(prop, rprop)));
					manager.applyChange(new AddAxiom(ont, df.getOWLTransitiveObjectPropertyAxiom(rprop)));
					LOG.info("  reflexive prop:"+rprop);

				}
				if (rprop != null) {
					if (xa instanceof OWLObjectSomeValuesFrom)
						return df.getOWLObjectSomeValuesFrom(rprop,xb);
					else if (xa instanceof OWLObjectAllValuesFrom)
						return df.getOWLObjectAllValuesFrom(rprop, xb);
				}

			}
		}
		return null;
	}


	/**
	 * combines two class expressions to a UnionOf expression.
	 * if both are identical, returns the expression.
	 * does not attempt to reduce the union expression
	 * 
	 * @param xa
	 * @param xb
	 * @return
	 */
	private OWLClassExpression makeUnionBasic(OWLClassExpression xa, OWLClassExpression xb) {
		if (xa.equals(xb)) {
			return xa;
		}
		else {
			Set<OWLClassExpression> args = new HashSet<OWLClassExpression>();
			args.add(xa);
			args.add(xb);
			return graph.getDataFactory().getOWLObjectUnionOf(args);
		}
	}
	
	private OWLClassExpression makeClassExpression(OWLObject x) {
		if (x instanceof OWLClassExpression)
			return (OWLClassExpression) x;
		else if (x instanceof OWLNamedIndividual) {
			// TODO - move this to graphwrapper
			Set<OWLNamedIndividual> nis = new HashSet<OWLNamedIndividual>();
			nis.add((OWLNamedIndividual) x);
			return graph.getDataFactory().getOWLObjectOneOf(nis);
		}
		else {
			System.err.println("cannot make CE from:"+x);
		}
		return null;
	}
	
	// ----
	
	
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
		OWLDataFactory df = graph.getDataFactory();
		
		// declare a named class for the LCS and make this equivalent to the anonymous expression
		OWLClass namedLCS = df.getOWLClass(IRI.create(id+"_LCS"));
		axioms.add(df.getOWLAnnotationAssertionAxiom(df.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL.getIRI()),
				namedLCS.getIRI(), 
				df.getOWLLiteral("LCS of "+simEngine.label(a)+" and "+simEngine.label(b))));
		axioms.add(df.getOWLEquivalentClassesAxiom(namedLCS, lcs));

		// link the similarity object to the named LCS
		OWLAnnotationProperty lcsp = df.getOWLAnnotationProperty(annotationIRI("has_least_common_subsumer"));
		axioms.add(df.getOWLAnnotationAssertionAxiom(lcsp, result.getIRI(), namedLCS.getIRI()));
	}
	
	

	// -- I/O - MOVE THIS!
	
	public void print(PrintStream s) {
		s.println("LCS: "+score);
		printX(s,lcs);
	}

	public void printX(PrintStream s, OWLObject x) {
		printX(s,x,0);
		s.print("\n");
	}

	// TODO - move to make this reusable
	public void printX(PrintStream s, OWLObject x, int depth) {
		tab(s,depth);
		if (x instanceof OWLObjectIntersectionOf) {
			s.print("and");
			for (OWLClassExpression sx : ((OWLObjectIntersectionOf)x).getOperands()) {
				printX(s, sx, depth+1);
			}
		}
		else if (x instanceof OWLObjectUnionOf) {
			s.print("or");
			for (OWLClassExpression sx : ((OWLObjectUnionOf)x).getOperands()) {
				printX(s, sx, depth+1);
			}
		}
		else if (x instanceof OWLQuantifiedRestriction) {
			OWLQuantifiedRestriction qr = (OWLQuantifiedRestriction)x;
			s.print(qr.getProperty().toString()+" "+qr.getClassExpressionType());
			printX(s, qr.getFiller(), depth+1);
		}
		else if (x instanceof OWLNamedObject) {
			String label = graph.getLabel(x);
			if (label != null)
				s.print(x.toString()+" \""+label+"\"");
			else
				s.print(x);
		}
		else {
			s.print(x);
		}
	}

	private void tab(PrintStream s, int depth) {
		s.print("\n");
		for (int i=0; i<depth; i++)
			s.print("  ");

	}

}
