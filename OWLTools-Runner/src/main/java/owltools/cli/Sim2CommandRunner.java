package owltools.cli;

import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.commons.math3.stat.descriptive.AggregateSummaryStatistics;
import org.apache.log4j.Logger;
import org.coode.owlapi.turtle.TurtleOntologyFormat;
import org.eclipse.jetty.server.Server;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.Node;

import owltools.cli.tools.CLIMethod;
import owltools.graph.OWLGraphWrapper;
import owltools.io.OWLPrettyPrinter;
import owltools.sim2.EnrichmentConfig;
import owltools.sim2.EnrichmentResult;
import owltools.sim2.FastOwlSim;
import owltools.sim2.FastOwlSimFactory;
import owltools.sim2.OwlSim;
import owltools.sim2.OwlSim.ScoreAttributeSetPair;
import owltools.sim2.OwlSimFactory;
import owltools.sim2.SimStats;
import owltools.sim2.SimpleOwlSim;
import owltools.sim2.SimpleOwlSim.Direction;
import owltools.sim2.SimpleOwlSim.Metric;
import owltools.sim2.SimpleOwlSim.OutputFormat;
import owltools.sim2.SimpleOwlSim.ScoreAttributePair;
import owltools.sim2.SimpleOwlSim.SimConfigurationProperty;
import owltools.sim2.SimpleOwlSimFactory;
import owltools.sim2.UnknownOWLClassException;
import owltools.sim2.io.DelimitedLineRenderer;
import owltools.sim2.io.FormattedRenderer;
import owltools.sim2.io.JSONRenderer;
import owltools.sim2.io.OWLRenderer;
import owltools.sim2.io.SimResultRenderer;
import owltools.sim2.io.TabularRenderer;
import owltools.sim2.io.SimResultRenderer.AttributesSimScores;
import owltools.sim2.io.SimResultRenderer.IndividualSimScores;
import owltools.sim2.preprocessor.NullSimPreProcessor;
import owltools.sim2.preprocessor.PhenoSimHQEPreProcessor;
import owltools.sim2.preprocessor.PropertyViewSimPreProcessor;
import owltools.sim2.preprocessor.SimPreProcessor;
import owltools.sim2.scores.AttributePairScores;
import owltools.sim2.scores.ElementPairScores;
import owltools.web.OWLServer;



/**
 * Semantic similarity and information content.
 * 
 * @author cjm
 * 
 */
public class Sim2CommandRunner extends SimCommandRunner {

	private static final Logger LOG = Logger.getLogger(Sim2CommandRunner.class);

	private OWLOntology simOnt = null;

	private String defaultPropertiesFile = "default-sim.properties";

	OwlSim owlsim; // simple owlsim


	OwlSimFactory owlSimFactory = new SimpleOwlSimFactory();

	SimPreProcessor pproc;

	Properties simProperties = null;

	int numberOfPairsFiltered = 0;

	protected PrintStream resultOutStream = System.out;

	private void initProperties() {
		simProperties = new Properties();
		// TODO: load a default properties file
		simProperties.setProperty(SimConfigurationProperty.minimumMaxIC.toString(),
				SimConfigurationProperty.minimumMaxIC.defaultValue());
		simProperties.setProperty(SimConfigurationProperty.minimumSimJ.toString(),
				SimConfigurationProperty.minimumSimJ.defaultValue());
		simProperties.setProperty(SimConfigurationProperty.minimumAsymSimJ.toString(),
				SimConfigurationProperty.minimumAsymSimJ.defaultValue());
		simProperties.setProperty(SimConfigurationProperty.outputFormat.toString(),
				SimConfigurationProperty.outputFormat.defaultValue());

		/* by default, turn off metrics */
		// String[] metrics =
		// SimConfigurationProperty.scoringMetrics.toString().split(",");
		// for (int i=0; i<Metric.values().length; i++) {
		// simProperties.setProperty(Metric.values()[i].toString(),"0");
		// }
		simProperties.setProperty(
				SimConfigurationProperty.scoringMetrics.toString(),
				SimConfigurationProperty.scoringMetrics.defaultValue());
	}

	public String getProperty(SimConfigurationProperty p) {
		if (simProperties == null) {
			initProperties();
		}
		return simProperties.getProperty(p.toString());
	}

	private Double getPropertyAsDouble(SimConfigurationProperty p) {
		String v = getProperty(p);
		return Double.valueOf(v);
	}

	private Double getPropertyAsDouble(SimConfigurationProperty p, 
			Double dv) {
		Double v = getPropertyAsDouble(p);
		if (v==null)
			return dv;
		return v;
	}

	private Boolean getPropertyAsBoolean(SimConfigurationProperty p) {
		String v = getProperty(p);
		if (v == null) {
			return false;
		}
		return Boolean.valueOf(v);
	}

	private boolean isAboveMinimum(SimConfigurationProperty p, Double v) {
		Double min = getPropertyAsDouble(p);
		if (min == null)
			return true;
		return v >= min;
	}


	@CLIMethod("--save-sim")
	public void saveSim(Opts opts) throws Exception {
		opts.info("FILE",
				"saves similarity results as an OWL ontology. Use after --sim or --sim-all");
		pw.saveOWL(simOnt, opts.nextOpt(), g);
	}

	@CLIMethod("--merge-sim")
	public void mergeSim(Opts opts) throws Exception {
		g.mergeOntology(simOnt);
	}

	@CLIMethod("--remove-dangling-annotations")
	public void removeDangningAnnotations(Opts opts) throws Exception {
		OWLOntology ont = g.getSourceOntology();
		int n = 0;
		Set<OWLAxiom> rmAxioms = new HashSet<OWLAxiom>();
		for (OWLNamedIndividual i : ont.getIndividualsInSignature()) {
			for (OWLClassAssertionAxiom ca : ont.getClassAssertionAxioms(i)) {
				OWLClassExpression cx = ca.getClassExpression();
				if (cx instanceof OWLClass) {
					OWLClass c = (OWLClass) cx;
					String label = g.getLabel(c);
					if (label == null)
						rmAxioms.add(ca);
					else
						n++;
				}
			}
		}
		LOG.info("Removing " + rmAxioms.size() + " axioms");
		ont.getOWLOntologyManager().removeAxioms(ont, rmAxioms);
		LOG.info("Remaining: " + n + " axioms");
	}

	public void attributeAllByAllOld(Opts opts) throws UnknownOWLClassException {
		try {
			((SimpleOwlSim) owlsim).setSimProperties(simProperties);
			Set<OWLClass> atts = owlsim.getAllAttributeClasses();
			LOG.info("All by all for " + atts.size() + " classes");

			//set the renderer
			SimResultRenderer renderer = setRenderer();

			// print a header in the file that details what was done
			for (Object k : simProperties.keySet()) {
				renderer.printComment(k + " = "
						+ simProperties.getProperty(k.toString()));
			}

			int comparableCounter = 0;
			for (OWLClass i : atts) {
				LOG.info("Attr(i) = "+i);
				AttributesSimScores bestScores = null;
				for (OWLClass j : atts) {
					if (isComparable(i,j)) {
						comparableCounter++;
						AttributesSimScores scores = computeSim(i, j);
						if (!scores.isFiltered) {
							if (getProperty(SimConfigurationProperty.bestOnly) == null ||
									!getPropertyAsBoolean(SimConfigurationProperty.bestOnly)) {
								renderSim(scores, renderer);
							}
							if (bestScores == null || scores.simJScore > bestScores.simJScore) {
								bestScores = scores;
							}
						}
					}
				}
				if (bestScores == null) {
					//LOG.warn("No best match for "+i);
				}
				else {
					bestScores.isBestMatch = true;
					renderSim(bestScores, renderer);
				}

			}
			LOG.info("FINISHED All by all for " + atts.size() + " classes, comparisons = "+comparableCounter);
			// LOG.info("Number of pairs filtered as they scored beneath threshold: "+this.numberOfPairsFiltered);
			renderer.dispose();
		} finally {
			IOUtils.closeQuietly(resultOutStream);
		}
	}

	private Metric getMetric(String m) {
		for (Metric metric : Metric.values()) {
			if (m.equals(metric.toString())) {
				return metric;
			}
		}
		return null;
	}
	private Set<Metric> getMetrics(String[] ms) {
		Set<Metric> metrics = new HashSet<Metric>();
		for (String m : ms) {
			metrics.add(getMetric(m));
		}
		return metrics;
	}
	private Set<Metric> getMetrics() {
		String[] metrics = getProperty(SimConfigurationProperty.scoringMetrics)
				.split(",");
		return getMetrics(metrics);
	}

	private long tdelta(long prev) {
		return System.currentTimeMillis() - prev;
	}

	public void attributeAllByAll(OwlSim sim, Opts opts) throws UnknownOWLClassException {
		try {

			Set<Metric> metrics = getMetrics();
			LOG.info("Metrics:" +metrics);

			//sim.setSimProperties(simProperties);
			Set<OWLClass> atts = sim.getAllAttributeClasses();
			//Set<OWLClass> atts = sim.getSourceOntology().getClassesInSignature(true);
			LOG.info("All by all for " + atts.size() + " classes");

			//set the renderer
			SimResultRenderer renderer = setRenderer();
			Runtime rt = Runtime.getRuntime();
			// print a header in the file that details what was done
			for (Object k : simProperties.keySet()) {
				renderer.printComment(k + " = "
						+ simProperties.getProperty(k.toString()));
			}

			int comparableCounter = 0;

			long totalTimeSimJ = 0;
			long totalCallsSimJ = 0;
			long totalTimeLCS = 0;
			long totalCallsLCS = 0;
			long totalTimeGIC = 0;
			long totalCallsGIC = 0;

			double totalScoreSimJ = 0;

			for (OWLClass i : atts) {
				if (!isComparable(i, null)) {
					continue;
				}
				LOG.info("Attr(i) = "+i);
				//AttributesSimScores bestScores = null;
				Set<OWLClass> js = new HashSet<OWLClass>();
				for (OWLClass j : atts) {
					if (isComparable(i,j)) {
						comparableCounter++;
						js.add(j);
					}
				}
				LOG.info("Attr(i) = "+i+ " compared with: "+js.size());
				ArrayList<AttributePairScores> best = null;
				ArrayList<AttributePairScores> allMatchesForI = 
						new ArrayList<AttributePairScores>(js.size());

				for (OWLClass j : js) {
					AttributePairScores scores = new AttributePairScores(i, j);
					//AttributesSimScores scores = new AttributesSimScores(i, j);

					long t;

					// TODO - normalize metrics
					if (metrics.contains(Metric.JACCARD) || metrics.contains(Metric.SIMJ)) {
						t = System.currentTimeMillis();
						scores.simjScore = sim.getAttributeJaccardSimilarity(i, j);
						totalScoreSimJ += scores.simjScore;
						totalTimeSimJ += tdelta(t);
						totalCallsSimJ++;

						if (!isAboveMinimum(SimConfigurationProperty.minimumSimJ, scores.simjScore))
							continue;
						scores.asymmetricSimjScore = sim.getAsymmetricAttributeJaccardSimilarity(i, j);
					}
					if (metrics.contains(Metric.LCSIC)) {
						t = System.currentTimeMillis();
						ScoreAttributeSetPair iclcs = sim.getLowestCommonSubsumerWithIC(i, j);
						scores.lcsIC = iclcs.score;
						scores.lcsSet = iclcs.attributeClassSet;
						totalTimeLCS += tdelta(t);
						totalCallsLCS++;
					}
					if (metrics.contains(Metric.GIC)) {

						t = System.currentTimeMillis();
						scores.simGIC = sim.getAttributeGraphInformationContentSimilarity(i, j);
						totalTimeGIC += tdelta(t);
						totalCallsGIC++;
					}

					if (best == null) {
						best = new ArrayList<AttributePairScores>();
						best.add(scores);
					}
					else {
						AttributePairScores bestRep = best.get(0);
						if (scores.simjScore == bestRep.simjScore) {
							best.add(scores);
						}
						else if (scores.simjScore > bestRep.simjScore) {
							best = new ArrayList<AttributePairScores>();
							best.add(scores);
						}
					}
					allMatchesForI.add(scores);
				}
				if (best == null || best.size()==0) {
					LOG.warn("No best macth for "+i);
				}
				else {
					for (AttributePairScores scores: best) {
						scores.isBestMatchForI = true;
					}
				}
				for (AttributePairScores scores: allMatchesForI) {
					if (getPropertyAsBoolean(SimConfigurationProperty.bestOnly) &&
							!scores.isBestMatchForI) {
						continue;
					}
					renderer.printPairScores(scores);
				}

				LOG.info("DONE Attr(i) = "+i+ " compared with: "+js.size());

				//				List<AttributesSimScores> scoresets = sim.compareAllAttributes(i, js);
				//				for (AttributesSimScores scores : scoresets) {
				//					if (!isFiltered(scores)) {
				//						if (getProperty(SimConfigurationProperty.bestOnly) == null ||
				//								(!getPropertyAsBoolean(SimConfigurationProperty.bestOnly) ||
				//										scores.isBestMatch)) {
				//							renderSim(scores, renderer);
				//						}
				//					}
				//				}
			}
			LOG.info("FINISHED All by all for " + atts.size() + " classes, comparisons = "+comparableCounter);
			// LOG.info("Number of pairs filtered as they scored beneath threshold: "+this.numberOfPairsFiltered);
			if (totalCallsSimJ > 0) {
				LOG.info("t(SimJ) ms = "+totalTimeSimJ + " / "+totalCallsSimJ + " = " + totalTimeSimJ / (double) totalCallsSimJ);
				LOG.info("avg(SimJ)  = " + totalScoreSimJ / (double) totalCallsSimJ);
			}
			if (totalCallsLCS > 0) {
				LOG.info("t(LCS) ms = "+totalTimeLCS + " / "+totalCallsLCS + " = " + totalTimeLCS / (double) totalCallsLCS);
			}
			if (totalCallsGIC > 0) {
				LOG.info("t(GIC) ms = "+totalTimeGIC + " / "+totalCallsGIC + " = " + totalTimeGIC / (double) totalCallsGIC);
			}
			renderer.dispose();
		} finally {
			IOUtils.closeQuietly(resultOutStream);
		}
	}

	/**
	 * Iterates through all individuals, generating similarity calculations for
	 * each of its attributes (ontology classes). As opposed to
	 * 
	 * {@link #attributeAllByAll }, which outputs the comparison of any two attributes only
	 *                     ever once, this method will necessarily output the
	 *                     pairwise-comparison repeatedly, if the same two
	 *                     attributes belong to two different sets of individuals.
	 * 
	 * @param opts
	 * @throws UnknownOWLClassException 
	 */
	public void attributeSimilarityAllByAllPairwise(Opts opts) throws UnknownOWLClassException {
		try {
			owlsim.setSimProperties(simProperties);
			Set<OWLNamedIndividual> insts = pproc.getOutputOntology()
					.getIndividualsInSignature();

			LOG.info("Pairwise attribute comparison for " + insts.size()
					+ " individuals");

			//set the renderer
			SimResultRenderer renderer = setRenderer();

			renderer.printComment("All-by-all pairwise attribute similarity");

			int attrCounter = 0;
			int comparableCounter = 0;
			for (OWLNamedIndividual i : insts) {
				for (OWLNamedIndividual j : insts) {
					if (isComparable(i, j)) {
						comparableCounter += 1;
						attributeSimilarityByPair(i, j);
						attrCounter += (owlsim.getAttributesForElement(i).size() + owlsim
								.getAttributesForElement(j).size());
					} else {
						LOG.info("skipping (not comparable): " + i + " + " + j);
					}
				}
			}

			LOG.info("FINISHED Pairwise All by all for " + comparableCounter
					+ " comparable individuals containing " + attrCounter + " classes");
			// LOG.info("Number of pairs filtered as they scored beneath threshold: "+this.numberOfPairsFiltered);
		} finally {
			IOUtils.closeQuietly(resultOutStream);
		}
	}

	/**
	 * Given two individuals, this fetches their respective attributes, computes
	 * their similarity (LCS), and renders the scores for all attribute-pairs.
	 * This assumes that i and j are comparable. This is not smart by any means,
	 * and therefore it is possible that (a,b) and (b,a) comparisons may be 
	 * included in the same output.
	 * 
	 * @param i
	 * @param j
	 * @throws UnknownOWLClassException 
	 */
	public void attributeSimilarityByPair(OWLNamedIndividual i,
			OWLNamedIndividual j) throws UnknownOWLClassException {
		if (isComparable(i, j)) {
			try {
				owlsim.setSimProperties(simProperties);

				//set the renderer
				SimResultRenderer renderer = setRenderer();


				Set<OWLClass> iatts = owlsim.getAttributesForElement(i);
				Set<OWLClass> jatts = owlsim.getAttributesForElement(j);
				for (OWLClass a : iatts) {
					for (OWLClass b : jatts) {
						AttributesSimScores scores = computeSim(a, b);
						//						renderSim(scores, renderer);
						renderAttrSimWithIndividuals(i, j, renderer, scores);
					}
				}
			} finally {
				IOUtils.closeQuietly(resultOutStream);
			}
		} else {
			LOG.info("skipping (not comparable): " + i + " + " + j);
		}
	}



	/**
	 * performs all by all individual comparison
	 * 
	 * @param opts
	 * @throws UnknownOWLClassException 
	 */
	@Deprecated
	public void runOwlSim(Opts opts) throws UnknownOWLClassException {
		try {
			if (owlsim instanceof SimpleOwlSim)
				((SimpleOwlSim) owlsim).setSimProperties(simProperties);
			if (opts.nextEq("-q")) {
				runOwlSimOnQuery(opts, opts.nextOpt());
				return;
			}
			Set<OWLNamedIndividual> insts = owlsim.getAllElements();
			LOG.info("All by all for " + insts.size() + " individuals");

			//set the renderer
			SimResultRenderer renderer = setRenderer();

			// print a header in the file that details what was done
			renderer.printComment("Properties for this run:");
			for (Object k : simProperties.keySet()) {
				renderer.printComment(k + " = "
						+ simProperties.getProperty(k.toString()));
				// TODO: output if the property is default
			}
			resultOutStream.flush();
			for (OWLNamedIndividual i : insts) {
				for (OWLNamedIndividual j : insts) {
					// similarity is symmetrical
					if (isComparable(i, j)) {
						IndividualSimScores scores = computeSim(i, j);
						owlsim.getSimStats().incrementIndividualPairCount();
						owlsim.getSimStats().incrementClassPairCount(scores.numberOfElementsI * scores.numberOfElementsJ);
						renderSim(scores, renderer);
					} else {
						LOG.info("skipping " + i + " + " + j);
					}
				}
			}
			LOG.info("FINISHED All by all for " + insts.size() + " individuals");
			LOG.info("Number of pairs filtered as they scored beneath threshold: "
					+ this.numberOfPairsFiltered);
		} finally {
			IOUtils.closeQuietly(resultOutStream);
		}
	}

	// this will replace the method above
	public void runOwlSim(OwlSim owlsim, Opts opts) throws UnknownOWLClassException {
		try {
			//owlsim.setSimProperties(simProperties);
			//			if (opts.nextEq("-q")) {
			//				runOwlSimOnQuery(opts, opts.nextOpt());
			//				return;
			//			}
			Set<OWLNamedIndividual> insts = owlsim.getAllElements();
			LOG.info("All by all for " + insts.size() + " individuals");
			double minSimJ = getPropertyAsDouble(SimConfigurationProperty.minimumSimJ, 0.1);
			LOG.info("min(SimJ)="+minSimJ);
			double minMaxIC = getPropertyAsDouble(SimConfigurationProperty.minimumMaxIC, 3.0);
			LOG.info("min(MaxIC)="+minMaxIC);

			
			//set the renderer
			SimResultRenderer renderer = setRenderer();

			// print a header in the file that details what was done
			renderer.printComment("Properties for this run:");
			for (Object k : simProperties.keySet()) {
				renderer.printComment(k + " = "
						+ simProperties.getProperty(k.toString()));
				// TODO: output if the property is default
			}
			resultOutStream.flush();
			for (OWLNamedIndividual i : insts) {
				for (OWLNamedIndividual j : insts) {
					// similarity is symmetrical
					if (isComparable(i, j)) {
						// TODO - optimize this
						ElementPairScores scores = owlsim.getGroupwiseSimilarity(i, j);
						if (scores.simjScore < minSimJ)
							continue;
						if (scores.maxIC < minMaxIC)
							continue;
						//sos.getSimStats().incrementIndividualPairCount();
						//sos.getSimStats().incrementClassPairCount(scores.numberOfElementsI * scores.numberOfElementsJ);
						renderer.printPairScores(scores);
					} else {
						LOG.info("skipping " + i + " + " + j);
					}
				}
			}
			if (owlsim instanceof FastOwlSim)
				owlsim.showTimings();
			LOG.info("FINISHED All by all for " + insts.size() + " individuals");
			LOG.info("Number of pairs filtered as they scored beneath threshold: "
					+ this.numberOfPairsFiltered);
			renderer.dispose();

		} finally {
			IOUtils.closeQuietly(resultOutStream);
		}
	}
	private void runOwlSimOnQuery(Opts opts, String q) throws UnknownOWLClassException {
		System.out.println("Query: " + q);
		OWLNamedIndividual qi = (OWLNamedIndividual) resolveEntity(q);
		System.out.println("Query Individual: " + qi);
		Set<OWLNamedIndividual> insts = pproc.getOutputOntology()
				.getIndividualsInSignature();

		SimResultRenderer renderer = setRenderer();

		for (OWLNamedIndividual j : insts) {
			IndividualSimScores scores = computeSim(qi, j);
			renderSim(scores, renderer);
		}

	}

	/**
	 * by default, i and j are comparable only if i>j (i.e. self and symmetric
	 * comparisons not permitted). If {@link SimConfigurationProperty#bidirectional}
	 * is true, then self and symmetric are permitted.
	 * 
	 * if {@link SimConfigurationProperty#compare} is set to a string "A, B" then
	 * only comparisons between objects with ID spaces A and B are compared.
	 * 
	 * This method can be applied to classes or to individuals. It is designed
	 * to be use in an all x all comparison of either classes or individuals
	 * 
	 * it can be called with a null value for j. If j==null, then isComparable
	 * is true if i is comparable to anything. This will fail if the first
	 * entry in the compare property does not match the ID space of i.
	 * 
	 * @param i - not null
	 * @param j - may be null
	 * @return true if i and j are comparable
	 */
	private boolean isComparable(OWLNamedObject i, OWLNamedObject j) {
		if (g.getIsObsolete(i))
			return false;
		if (j != null && g.getIsObsolete(j))
			return false;
		String cmp = getProperty(SimConfigurationProperty.compare);
		if (cmp == null) {
			if (j==null)
				return true;
			Boolean bidi = getPropertyAsBoolean(SimConfigurationProperty.bidirectional);
			if (bidi == null || !bidi) {
				return i.compareTo(j) > 0;
			}
			else {
				return true;
			}

		} else {
			String[] idspaces = cmp.split(",");
			if (i.getIRI().toString().contains("/" + idspaces[0] + "_")
					&& (j==null || j.getIRI().toString().contains("/" + idspaces[1] + "_"))) {
				return true;
			} else {
				return false;
			}
		}
	}

	/**
	 * Determines if the named individual belongs to the given idSpace
	 * This assumes the pattern in the IRI for the individual is like:
	 * .../IDSPACE_<identifier>
	 * @param i
	 * @param idSpace
	 * @return boolean
	 */
	private boolean isOfIDSpace(OWLNamedIndividual i, String idSpace) {
		//TODO: this should be more robust - should probably be regex with $ included
		if (idSpace == null) {
			return false;
		} else {
			if (i.getIRI().toString().contains("/" + idSpace + "_")) {				
				return true;
			} else {
				return false;
			}
		}
	}

	boolean isHeaderLine = true;

	private void renderSim(AttributesSimScores simScores,
			SimResultRenderer renderer) {
		simScores.simjScoreLabel = "SimJ_Score";
		simScores.AsymSimJScoreLabel = "AsymSimJ_Score";
		simScores.lcsScorePrefix = "LCS";
		renderer.printAttributeSim(simScores, g);
	}

	/**
	 * @param i
	 * @param j
	 * @param renderer
	 * @param simScores 
	 */
	private void renderAttrSimWithIndividuals(OWLNamedIndividual i,
			OWLNamedIndividual j, SimResultRenderer renderer,
			AttributesSimScores simScores) {
		simScores.simjScoreLabel = "SimJ_Score";
		simScores.AsymSimJScoreLabel = "AsymSimJ_Score";
		simScores.lcsScorePrefix = "LCS";
		renderer.printAttributeSimWithIndividuals(simScores, getPrettyPrinter(), g, i, j);

	}

	private void renderSim(IndividualSimScores simScores,
			SimResultRenderer renderer) {
		simScores.simjScoreLabel = "SimJ_Score";
		simScores.maxICLabel = Metric.MAXIC.toString();
		simScores.simjScoreLabel = Metric.SIMJ.toString();
		simScores.bmaAsymICLabel = "BMAasymIC";
		simScores.bmaSymICLabel = "BMAsymIC";
		simScores.bmaAsymJLabel = "BMAasymJ";
		simScores.bmaSymJLabel = "BMAsymJ";
		simScores.simGICLabel = "SimGIC";
		renderer.printIndividualPairSim(simScores, getPrettyPrinter(), g);
	}

	/**
	 * Compute the attribute x attribute (class x class) similarity
	 * 
	 * @param a
	 * @param b
	 * @return scores
	 * @throws UnknownOWLClassException 
	 */
	private AttributesSimScores computeSim(OWLClass a, OWLClass b) throws UnknownOWLClassException {

		AttributesSimScores scores = new AttributesSimScores(a, b);
		double simJScore = owlsim.getAttributeJaccardSimilarity(a, b);
		if (simJScore < getPropertyAsDouble(SimConfigurationProperty.minimumSimJ)) {
			numberOfPairsFiltered++;
			scores.isFiltered = true;
			return scores;
		}
		scores.simJScore = simJScore;

		double asymSimJScore = owlsim.getAsymmetricAttributeJaccardSimilarity(a, b);

		if (asymSimJScore < getPropertyAsDouble(SimConfigurationProperty.minimumAsymSimJ)) {
			numberOfPairsFiltered++;
			scores.isFiltered = true;
			return scores;
		}
		scores.AsymSimJScore = asymSimJScore;

		ScoreAttributePair lcsScore = ((SimpleOwlSim) owlsim).getLowestCommonSubsumerIC(a, b);
		if (lcsScore.score < getPropertyAsDouble(SimConfigurationProperty.minimumMaxIC)) {
			numberOfPairsFiltered++;
			scores.isFiltered = true;
			return scores;
		}
		scores.lcsScore = lcsScore;

		return scores;
	}

	private boolean isFiltered(AttributesSimScores scores) {

		if (scores.simJScore != null && scores.simJScore < getPropertyAsDouble(SimConfigurationProperty.minimumSimJ)) {
			numberOfPairsFiltered++;
			scores.isFiltered = true;
			return true;
		}


		if (scores.AsymSimJScore != null && scores.AsymSimJScore < 
				getPropertyAsDouble(SimConfigurationProperty.minimumAsymSimJ)) {
			numberOfPairsFiltered++;
			scores.isFiltered = true;
			return true;
		}

		if (scores.lcsScore != null && scores.lcsScore.score < getPropertyAsDouble(SimConfigurationProperty.minimumMaxIC)) {
			numberOfPairsFiltered++;
			scores.isFiltered = true;
			return true;
		}

		return false;
	}

	private IndividualSimScores computeSim(OWLNamedIndividual i,
			OWLNamedIndividual j) throws UnknownOWLClassException {

		IndividualSimScores scores = new IndividualSimScores(i, j);

		scores.numberOfElementsI = owlsim.getAttributesForElement(i).size();
		scores.numberOfElementsJ = owlsim.getAttributesForElement(j).size();

		String[] metrics = getProperty(SimConfigurationProperty.scoringMetrics)
				.split(",");

		// only do this test if we end up using the IC measures
		for (String metric : metrics) {
			if (Metric.isICmetric(metric)) {
				ScoreAttributeSetPair maxIC = owlsim.getSimilarityMaxIC(i, j);
				if (maxIC.score < getPropertyAsDouble(SimConfigurationProperty.minimumMaxIC)) {
					numberOfPairsFiltered++;
					return scores;
				}
				scores.maxIC = maxIC;
				owlsim.getSimStats().setValue(Metric.MAXIC,scores.maxIC.score);
				break; // only need to test once.
			}
		}

		// only do this test if we end up using the J measures
		for (String metric : metrics) {
			if (Metric.isJmetric(metric)) {
				double simJ = owlsim.getElementJaccardSimilarity(i, j);
				if (simJ < getPropertyAsDouble(SimConfigurationProperty.minimumSimJ)) {
					numberOfPairsFiltered++;
					return scores;
				}
				scores.simjScore = simJ;
				owlsim.getSimStats().setValue(Metric.SIMJ,scores.simjScore);
				break; // only need to test once.
			}
		}

		for (String metric : metrics) {
			if (metric.equals(Metric.IC_MCS.toString())) {
				scores.bmaAsymIC = owlsim.getSimilarityBestMatchAverage(i, j,
						Metric.IC_MCS, Direction.A_TO_B);
				owlsim.getSimStats().setValue(Metric.IC_MCS,scores.bmaAsymIC.score);
				// TODO: do reciprocal BMA
				scores.bmaSymIC = owlsim.getSimilarityBestMatchAverage(i, j,
						Metric.IC_MCS, Direction.AVERAGE);
			} else if (metric.equals(Metric.JACCARD.toString())) {
				scores.bmaAsymJ = owlsim.getSimilarityBestMatchAverage(i, j,
						Metric.JACCARD, Direction.A_TO_B);
				// TODO: do reciprocal BMA
				owlsim.getSimStats().setValue(Metric.JACCARD,scores.bmaAsymJ.score);
				scores.bmaSymJ = owlsim.getSimilarityBestMatchAverage(i, j,
						Metric.JACCARD, Direction.AVERAGE);
			} else if (metric.equals(Metric.GIC.toString())) {
				scores.simGIC = owlsim.getElementGraphInformationContentSimilarity(i, j);
			}
		}
		return scores;
	}




	private String renderPair(OWLNamedIndividual i, OWLNamedIndividual j,
			OWLPrettyPrinter owlpp) {
		// return i+"\t"+owlpp.render(i)+"\t"+j+"\t"+owlpp.render(j);
		return owlpp.render(i) + "\t" + owlpp.render(j);
	}

	private String renderAttributes(Set<Node<OWLClass>> atts,
			OWLPrettyPrinter owlpp) {
		List<String> s = new ArrayList<String>();
		for (Node<OWLClass> n : atts) {
			OWLClass c = n.getRepresentativeElement();
			s.add(owlpp.render(c));
		}
		return (StringUtils.join(s, " | "));
	}

	protected void loadProperties(Opts opts) throws IOException {
		while (opts.hasOpts()) {
			if (opts.nextEq("-p|--properties")) {
				loadProperties(opts.nextOpt());
				// need to check if metrics are valid
			} else if (opts.nextEq("--set")) {
				simProperties.setProperty(opts.nextOpt(), opts.nextOpt());
			} else if (opts.nextEq("-o")) {
				String file = opts.nextOpt();
				FileOutputStream fos;
				try {
					LOG.info("Writing results to "+file);
					fos = new FileOutputStream(file);
					this.resultOutStream = new PrintStream(new BufferedOutputStream(fos));
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				break;
			}
		}
		// TODO:perhaps there should be a default simProperties file?
		if (simProperties == null) {
			initProperties();
		}
	}

	private void loadProperties(String fn) throws IOException {
		if (simProperties == null) this.initProperties();
		FileInputStream myInputStream = new FileInputStream(fn);
		simProperties.load(myInputStream);

		try {
			showSimProperties(null);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		myInputStream.close(); // better in finally block

	}

	@CLIMethod("--show-sim-properties")
	public void showSimProperties(Opts opts) throws Exception {
		for (Object k : simProperties.keySet()) {
			System.out.println("# " + k + " = "
					+ simProperties.getProperty(k.toString()));
		}
	}

	@CLIMethod("--set-sim-property")
	public void setSimProperty(Opts opts) throws Exception {
		if (simProperties == null) {
			simProperties = new Properties();
		}
		simProperties.setProperty(opts.nextOpt(), opts.nextOpt());
	}

	@CLIMethod("--phenosim")
	@Deprecated
	public void phenoSim(Opts opts) throws Exception {
		loadProperties(opts);
		try {
			pproc = new PhenoSimHQEPreProcessor();
			pproc.setSimProperties(simProperties);

			pproc.setInputOntology(g.getSourceOntology());
			pproc.setOutputOntology(g.getSourceOntology());
			pproc.preprocess();
			pproc.getReasoner().flush();
			owlsim = new SimpleOwlSim(g.getSourceOntology());
			((SimpleOwlSim) owlsim).setSimPreProcessor(pproc);
			owlsim.createElementAttributeMapFromOntology();
			// pproc.saveState("/tmp/phenosim-analysis-ontology.owl");
			runOwlSim(opts);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			LOG.info("clearing up...");
			if (pproc != null) {
				pproc.dispose();
			}
		}

	}

	@CLIMethod("--phenosim-attribute-matrix")
	@Deprecated
	public void phenoSimAttributeMatrix(Opts opts) throws Exception {
		loadProperties(opts);
		try {
			pproc = new PhenoSimHQEPreProcessor();
			pproc.setSimProperties(simProperties);

			pproc.setInputOntology(g.getSourceOntology());
			pproc.setOutputOntology(g.getSourceOntology());
			pproc.preprocess();
			pproc.getReasoner().flush();
			owlsim = owlSimFactory.createOwlSim(g.getSourceOntology());
			((SimpleOwlSim) owlsim).setSimPreProcessor(pproc);
			owlsim.createElementAttributeMapFromOntology();
			// pproc.saveState("/tmp/phenosim-analysis-ontology.owl");
			attributeAllByAllOld(opts);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			LOG.info("clearing up...");
			if (pproc != null) {
				pproc.dispose();
			}
		}

	}

	@CLIMethod("--sim-resume")
	@Deprecated
	public void simResume(Opts opts) throws Exception {
		loadProperties(opts);
		OWLOntology ont = pw.parse("file:///tmp/phenosim-analysis-ontology.owl");
		if (g == null) {
			g = new OWLGraphWrapper(ont);
		} else {
			System.out.println("adding support ont " + ont);
			g.addSupportOntology(ont);
		}
		try {
			pproc = new NullSimPreProcessor();
			pproc.setSimProperties(simProperties);
			pproc.setInputOntology(g.getSourceOntology());
			pproc.setOutputOntology(g.getSourceOntology());
			owlsim = owlSimFactory.createOwlSim(g.getSourceOntology());
			((SimpleOwlSim) owlsim).setSimPreProcessor(pproc);
			owlsim.createElementAttributeMapFromOntology();
			runOwlSim(opts);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (pproc != null) {
				pproc.dispose();
			}
		}
	}

	@CLIMethod("--sim-basic")
	public void simBasic(Opts opts) throws Exception {
		// assumes that individuals in abox are of types named classes in tbox
		loadProperties(opts);
		try {
			pproc = new NullSimPreProcessor();
			if (simProperties.containsKey(SimConfigurationProperty.analysisRelation
					.toString())) {
				pproc = new PropertyViewSimPreProcessor();
				String relId = (String) simProperties
						.get((SimConfigurationProperty.analysisRelation.toString()));
				OWLObjectProperty rel = g.getOWLObjectPropertyByIdentifier(relId);
				PropertyViewSimPreProcessor xpproc = ((PropertyViewSimPreProcessor) pproc);

				LOG.info("View relation = " + rel);
				xpproc.analysisRelation = rel;
			}
			pproc.setSimProperties(simProperties);
			pproc.setInputOntology(g.getSourceOntology());
			pproc.setOutputOntology(g.getSourceOntology());
			owlsim = owlSimFactory.createOwlSim(g.getSourceOntology());
			if (owlsim instanceof SimpleOwlSim)
				((SimpleOwlSim) owlsim).setSimPreProcessor(pproc);
			pproc.preprocess();
			owlsim.createElementAttributeMapFromOntology();
			runOwlSim(opts);
		} finally {
			owlsim.dispose();
			pproc.dispose();
		}
	}

	@CLIMethod("--fsim-basic")
	public void fsimBasic(Opts opts) throws Exception {
		// assumes that individuals in abox are of types named classes in tbox
		owlSimFactory = new FastOwlSimFactory();
		loadProperties(opts);
		try {
			if (owlsim == null) {
				owlsim = owlSimFactory.createOwlSim(g.getSourceOntology());
				owlsim.createElementAttributeMapFromOntology();
			}
			runOwlSim(owlsim, opts);
		} finally {
			owlsim.dispose();
		}
	}

	@CLIMethod("--use-fsim")
	public void useFastOwlSim(Opts opts) throws Exception {
		owlSimFactory = new FastOwlSimFactory();
	}




	/**
	 * This method will report to the user some metadata about the previous similarity
	 * run.  The similarity run will save some basic statistics into the 
	 * properties, which are recalled here.  Included stats are: counts, min/max per metric,
	 * and more TBD.
	 * @param opts
	 * @throws Exception
	 */
	@CLIMethod("--sim-basic-stats")
	public void simBasicStats(Opts opts) throws Exception {
		//TODO: iterate over all metrics, and print out results, perhaps put method into renderer
		LOG.info(Metric.JACCARD.name() + " max: " + owlsim.getSimStats().getMax(Metric.JACCARD));		
	}	

	@CLIMethod("--sim-dl-query")
	public void simDlQuery(Opts opts) throws Exception {
		loadProperties(opts);
		try {
			// TODO
			pproc = new NullSimPreProcessor();
			pproc.setInputOntology(g.getSourceOntology());
			pproc.setOutputOntology(g.getSourceOntology());
			owlsim = owlSimFactory.createOwlSim(g.getSourceOntology());
			((SimpleOwlSim) owlsim).setSimPreProcessor(pproc);
			owlsim.createElementAttributeMapFromOntology();
			runOwlSim(opts);
		} finally {
			pproc.dispose();
		}
	}

	@CLIMethod("--sim-compare-atts")
	public void simAttMatchOld(Opts opts) throws Exception {
		// assumes that individuals in abox are of types named classes in tbox
		loadProperties(opts);
		try {
			pproc = new NullSimPreProcessor();
			pproc.setInputOntology(g.getSourceOntology());
			pproc.setOutputOntology(g.getSourceOntology());
			owlsim = owlSimFactory.createOwlSim(g.getSourceOntology());
			if (owlsim instanceof SimpleOwlSim)
				((SimpleOwlSim) owlsim).setSimPreProcessor(pproc);
			owlsim.createElementAttributeMapFromOntology();
			attributeAllByAllOld(opts);
		} finally {
			pproc.dispose();
		}
	}

	@CLIMethod("--fsim-compare-atts")
	public void fsimCompareAtts(Opts opts) throws Exception {
		// assumes that individuals in abox are of types named classes in tbox
		loadProperties(opts);
		owlsim = new FastOwlSim(g.getSourceOntology());
		owlsim.createElementAttributeMapFromOntology();
		attributeAllByAll(owlsim, opts);
		((FastOwlSim) owlsim).showTimings();

	}

	// get an attribute set for comparison based on compare property;
	// if n=0, setA; if n=1, setB
	private Set<OWLClass> getAttSet(int n) {
		String cmp = getProperty(SimConfigurationProperty.compare);
		String[] idspaces = cmp.split(",");
		Set<OWLClass> cset = owlsim.getAllAttributeClasses();
		Set<OWLClass> cs = new HashSet<OWLClass>();
		LOG.info("|cset|="+cset.size());
		for (OWLClass c : cset) {
			if (g.isObsolete(c))
				continue;
			if (c.getIRI().toString().contains("/" + idspaces[n] + "_")) {
				cs.add(c);
			}
		}
		return cs;
	}

	@CLIMethod("--fsim-bench-simj")
	public void fsimCompareAttsSimJ(Opts opts) throws Exception {

		loadProperties(opts);
		owlSimFactory = new FastOwlSimFactory();
		owlsim = owlSimFactory.createOwlSim(g.getSourceOntology());
		owlsim.createElementAttributeMapFromOntology();

		String cmp = getProperty(SimConfigurationProperty.compare);
		String[] idspaces = cmp.split(",");
		Set<OWLClass> cset = owlsim.getAllAttributeClasses();
		Set<OWLClass> cs = new HashSet<OWLClass>();
		Set<OWLClass> ds = new HashSet<OWLClass>();
		LOG.info("|cset|="+cset.size());
		for (OWLClass c : cset) {
			if (c.getIRI().toString().contains("/" + idspaces[0] + "_")) {
				cs.add(c);
			}
			if (c.getIRI().toString().contains("/" + idspaces[1] + "_")) {
				ds.add(c);
			}

		}
		LOG.info("|cs|="+cs.size());
		LOG.info("|ds|="+ds.size());
		long t = System.currentTimeMillis();
		int n=0;
		for (OWLClass c : cs) {
			n++;
			if (n % 100 ==0) {
				LOG.info("N="+n);
			}
			for (OWLClass d : ds) {
				double score = owlsim.getAttributeJaccardSimilarity(c, d);
			}
		}
		LOG.info("t(All x All)="+tdelta(t));
	}

	@CLIMethod("--fsim-bench-lcs")
	public void fsimCompareAttsLCS(Opts opts) throws Exception {

		loadProperties(opts);
		owlSimFactory = new FastOwlSimFactory();
		owlsim = owlSimFactory.createOwlSim(g.getSourceOntology());
		owlsim.createElementAttributeMapFromOntology();

		Set<OWLClass> cs = getAttSet(0);
		Set<OWLClass> ds = getAttSet(1);
		LOG.info("|cs|="+cs.size());
		LOG.info("|ds|="+ds.size());
		long t = System.currentTimeMillis();
		int n=0;
		for (OWLClass c : cs) {
			n++;
			if (n % 100 ==0) {
				LOG.info("N="+n);
			}
			for (OWLClass d : ds) {
				owlsim.getLowestCommonSubsumerWithIC(c, d, 3.0);
			}
		}
		LOG.info("fsim-bench-simj t(All x All)="+tdelta(t)+" comparisons:"+cs.size() +" * "+ds.size());
	}

	@CLIMethod("--fsim-att-top-simj")
	public void fsimAttTopSimJ(Opts opts) throws Exception {
		opts.info("OWLSIMPARAMS", 
				"Finds top matches for each attribute in C, shows scores. Faster than fsim-atts");
		loadProperties(opts);
		owlSimFactory = new FastOwlSimFactory();
		owlsim = owlSimFactory.createOwlSim(g.getSourceOntology());
		owlsim.createElementAttributeMapFromOntology();
		owlsim.setDisableLCSCache(true);

		Set<OWLClass> cs = getAttSet(0);
		Set<OWLClass> ds = getAttSet(1);
		LOG.info("|cs|="+cs.size());
		LOG.info("|ds|="+ds.size());
		long t = System.currentTimeMillis();
		int n=0;
		for (OWLClass c : cs) {
			String cid = g.getIdentifier(c);
			String clabel = g.getLabel(c);
			n++;
			if (n % 100 ==0) {
				LOG.info("N="+n);
			}
			int best = 0;
			Set<OWLClass> bestWitnesses = new HashSet<OWLClass>();

			for (OWLClass d : ds) {
				int score = owlsim.getAttributeJaccardSimilarityAsPercent(c, d);
				if (score == best) {
					if (score > 0)
						bestWitnesses.add(d);
				}
				else if (score > best) {
					best = score;
					bestWitnesses = new HashSet<OWLClass>(Collections.singleton(d));
				}
			}
			if (bestWitnesses.size() == 0) {
				bestWitnesses.add(g.getDataFactory().getOWLThing());
			}
			for (OWLClass bestWitness : bestWitnesses) {
				ScoreAttributeSetPair lcsic = owlsim.getLowestCommonSubsumerWithIC(c, bestWitness);
				StringBuffer lcsId = new StringBuffer();
				StringBuffer lcsName = new StringBuffer();

				for (OWLClass lcs : lcsic.attributeClassSet) {
					lcsId.append(g.getIdentifier(lcs)+";");
					lcsName.append(g.getLabel(lcs)+";");
				}

				resultOutStream.println(cid+"\t"+
						clabel+"\t"+
						g.getIdentifier(bestWitness)+"\t"+
						g.getLabel(bestWitness)+"\t"+
						best+"\t"+
						owlsim.getAsymmetricAttributeJaccardSimilarityAsPercent(c, bestWitness)+"\t"+
						lcsic.score+"\t"+
						lcsId+"\t"+lcsName
						);
			}
		}
		LOG.info("fsim-att-top-simj t(All x All)="+tdelta(t)+" comparisons:"+cs.size() +" * "+ds.size());
		resultOutStream.close();
	}

	@CLIMethod("--fsim-compare-atts-lite")
	public void fsimCompareAttsLite(Opts opts) throws Exception {
		// assumes that individuals in abox are of types named classes in tbox
		loadProperties(opts);
		owlsim = new FastOwlSim(g.getSourceOntology());
		simProperties.setProperty(SimConfigurationProperty.scoringMetrics.toString(),
				Metric.SIMJ.toString());
		owlsim.createElementAttributeMapFromOntology();
		attributeAllByAll(owlsim, opts);
		if (owlsim instanceof FastOwlSim) {
			((FastOwlSim) owlsim).showTimings();
		}
	}


	@CLIMethod("--sim-test")
	public void simTest(Opts opts) throws Exception {
		// assumes that individuals in abox are of types named classes in tbox
		loadProperties(opts);
		try {
			FastOwlSim sim = new FastOwlSim(g.getSourceOntology());

			// temporary - required for renderer
			owlsim = owlSimFactory.createOwlSim(g.getSourceOntology());
			owlsim.setSimProperties(simProperties);

			sim.createElementAttributeMapFromOntology();
			attributeAllByAll(sim, opts);
		} finally {
			//pproc.dispose();
		}
	}

	@CLIMethod("--fsim-test")
	public void fsimTest(Opts opts) throws Exception {
		// assumes that individuals in abox are of types named classes in tbox
		loadProperties(opts);
		owlsim = new FastOwlSim(g.getSourceOntology());
		//sos.setSimProperties(simProperties);

		owlsim.createElementAttributeMapFromOntology();

		attributeAllByAll(owlsim, opts);
	}

	@CLIMethod("--sim-pair-compare-atts")
	public void simPairwiseAttMatch(Opts opts) throws Exception {
		// assumes that individuals in abox are of types named classes in tbox
		loadProperties(opts);
		try {
			pproc = new NullSimPreProcessor();
			pproc.setInputOntology(g.getSourceOntology());
			pproc.setOutputOntology(g.getSourceOntology());
			owlsim = owlSimFactory.createOwlSim(g.getSourceOntology());
			((SimpleOwlSim) owlsim).setSimPreProcessor(pproc);
			owlsim.createElementAttributeMapFromOntology();
			this.attributeSimilarityAllByAllPairwise(opts);
		} finally {
			pproc.dispose();
		}
	}

	@CLIMethod("--sim-save-lcs-cache")
	public void simSaveLCSCache(Opts opts) throws Exception {
		opts.info("[-m ICTHRESHOLD] OUTFILE", "saves a LCS cache to a file. This should be called AFTER --sim-compare-atts");
		Double thresh = null;
		while (opts.hasOpts()) {
			if (opts.nextEq("-m|--min-ic")) {
				opts.info("ICTHRESHOLD", "If the IC of the LCS is less than this value, an entry is not written.\n" +
						"After subsequent loading of the cache, pairs with no entry are equivalent to pairs with a LCS with IC=0");
				thresh = Double.valueOf(opts.nextOpt());
			}
			else {
				break;
			}
		}

		// No SOS object, so all by all has not yet been calculated
		if (owlsim == null) {
			owlsim = owlSimFactory.createOwlSim(g.getSourceOntology());
			owlsim.createElementAttributeMapFromOntology();
			owlsim.setNoLookupForLCSCache(true);
			Set<OWLClass> atts = owlsim.getAllAttributeClasses();
			LOG.info("Number of attribute classes: "+atts.size());
			for (OWLClass i : atts) {
				LOG.info("Comparing "+i+" to all attributes");
				for (OWLClass j : atts) {
					if (i.compareTo(j) < 0)
						continue;
					owlsim.getLowestCommonSubsumerWithIC(i, j, thresh);					
				}
			}

		}

		owlsim.saveLCSCache(opts.nextOpt(), thresh);
		LOG.info("Saved cache");
	}



	@CLIMethod("--sim-load-lcs-cache")
	public void simLoadLCSCache(Opts opts) throws Exception {
		opts.info("INFILE", "loads a LCS cache from a file");
		if (owlsim == null) {
			owlsim = owlSimFactory.createOwlSim(g.getSourceOntology());
		}
		owlsim.loadLCSCache(opts.nextOpt());
	}

	@CLIMethod("--sim-save-ic-cache")
	public void simSaveICCache(Opts opts) throws Exception {
		opts.info("OUTFILE", "saves ICs as RDF/turtle cache");
		if (owlsim == null) {
			owlsim = owlSimFactory.createOwlSim(g.getSourceOntology());
		}
		OWLOntology o = owlsim.cacheInformationContentInOntology();
		TurtleOntologyFormat fmt = new TurtleOntologyFormat();
		fmt.setPrefix("obo", "http://purl.obolibrary.org/obo/");
		fmt.setPrefix("sim", "http://owlsim.org/ontology/");
		g.getManager().saveOntology(o, 
				fmt,
				IRI.create(opts.nextFile()));
	}

	@CLIMethod("--sim-load-ic-cache")
	public void simLoadICCache(Opts opts) throws Exception {
		opts.info("INFILE", "loads ICs from RDF/turtle cache");
		if (owlsim == null) {
			owlsim = owlSimFactory.createOwlSim(g.getSourceOntology());
		}
		OWLOntology o = 
				g.getManager().loadOntologyFromOntologyDocument(opts.nextFile());
		owlsim.setInformationContentFromOntology(o);
	}




	@CLIMethod("--sim-lcs")
	public void simLCS(Opts opts) throws Exception {
		opts.info("", "find LCS of two classes");
		loadProperties(opts);
		OWLClass c1 = (OWLClass) this.resolveClass(opts.nextOpt());
		OWLClass c2 = (OWLClass) this.resolveClass(opts.nextOpt());
		OWLPrettyPrinter owlpp = getPrettyPrinter();
		if (owlsim == null) {
			owlsim = owlSimFactory.createOwlSim(g.getSourceOntology());
			owlsim.createElementAttributeMapFromOntology();
		}
		owlsim.createElementAttributeMapFromOntology();
		Set<Node<OWLClass>> lcsSet = owlsim.getNamedLowestCommonSubsumers(c1, c2);

		for (Node<OWLClass> lcsNode : lcsSet) {
			System.out.println(owlpp.render(lcsNode.getRepresentativeElement()));
		}
	}


	// TODO
	@CLIMethod("--enrichment-analysis")
	public void owlsimEnrichmentAnalysis(Opts opts) throws Exception {
		opts.info("", "performs enrichment on gene set. TODO");
		OWLPrettyPrinter owlpp = getPrettyPrinter();
		if (owlsim == null) {
			owlsim = owlSimFactory.createOwlSim(g.getSourceOntology());
			owlsim.createElementAttributeMapFromOntology();
		}
		EnrichmentConfig ec = new EnrichmentConfig();
		while (opts.hasOpts()) {
			if (opts.nextEq("-p")) {
				ec.pValueCorrectedCutoff = Double.parseDouble(opts.nextOpt());
			} else if (opts.nextEq("-i")) {
				ec.attributeInformationContentCutoff = Double.parseDouble(opts
						.nextOpt());
			} else
				break;
		}
		SimpleOwlSim ee = (SimpleOwlSim)owlsim;
		ee.setEnrichmentConfig(ec);
		OWLClass rc1 = this.resolveClass(opts.nextOpt());
		OWLClass rc2 = this.resolveClass(opts.nextOpt());
		OWLClass pc = g.getDataFactory().getOWLThing();
		List<EnrichmentResult> results = ee.calculateAllByAllEnrichment(pc, rc1,
				rc2);
		for (EnrichmentResult result : results) {
			System.out.println(render(result, owlpp));
		}
	}

	// NEW
	@CLIMethod("--all-by-all-enrichment-analysis")
	public void owlsimEnrichmentAnalysisAllByAll(Opts opts) throws Exception {
		opts.info("[-p pValCutOff] [-i IC_Cutoff] SAMPLECLASS TESTCLASS", 
				"performs all by all enrichment on every c x d where c Sub SAMPLECLASS and d Sub TESTCLASS");
		OWLPrettyPrinter owlpp = getPrettyPrinter();
		if (owlsim == null) {
			owlsim = owlSimFactory.createOwlSim(g.getSourceOntology());
			owlsim.createElementAttributeMapFromOntology();
		}
		EnrichmentConfig ec = new EnrichmentConfig();
		while (opts.hasOpts()) {
			if (opts.nextEq("-p")) {
				ec.pValueCorrectedCutoff = Double.parseDouble(opts.nextOpt());
			} else if (opts.nextEq("-i")) {
				ec.attributeInformationContentCutoff = Double.parseDouble(opts
						.nextOpt());
			} else
				break;
		}
		((SimpleOwlSim) owlsim).setEnrichmentConfig(ec);
		OWLClass rc1 = this.resolveClass(opts.nextOpt());
		OWLClass rc2 = this.resolveClass(opts.nextOpt());
		OWLClass pc = g.getDataFactory().getOWLThing();
		List<EnrichmentResult> results = ((SimpleOwlSim) owlsim).calculateAllByAllEnrichment(pc, rc1,
				rc2);
		for (EnrichmentResult result : results) {
			System.out.println(render(result, owlpp));
		}
	}

	@CLIMethod("--class-IC-pairs")
	public void classICPairs(Opts opts) throws Exception {
		opts.info("", "show all classes with their IC");
		OWLPrettyPrinter owlpp = getPrettyPrinter();
		if (owlsim == null) {
			owlsim = owlSimFactory.createOwlSim(g.getSourceOntology());
			owlsim.createElementAttributeMapFromOntology();
		}
		for (OWLClass c : g.getSourceOntology().getClassesInSignature()) {
			Double ic = owlsim.getInformationContentForAttribute(c);
			if (ic != null) {
				System.out.println(owlpp.render(c)+"\t"+ic);
			}
		}
	}


	private String render(EnrichmentResult r, OWLPrettyPrinter owlpp) {
		return owlpp.render(r.sampleSetClass) + "\t"
				+ owlpp.render(r.enrichedClass) + "\t" + r.pValue + "\t"
				+ r.pValueCorrected;
	}


	@CLIMethod("--start-sim-server")
	public void startSimServer(Opts opts) throws Exception {

		int port = 9000;
		while (opts.hasOpts()) {
			if (opts.nextEq("-p")) {
				port = Integer.parseInt(opts.nextOpt());
			}
			else {
				break;
			}
		}
		LOG.info("Starting server on port "+port+" using sim: "+owlsim);
		Server server = new Server(port);
		if (owlsim == null)
			this.createSlim(opts);
		server.setHandler(new OWLServer(g, owlsim));


		try {
			server.start();
			server.join();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Set the Renderer to use based on the configurable parameter: outputFormat
	 * @return renderer
	 */
	private SimResultRenderer setRenderer() {
		SimResultRenderer renderer = null;
		String f = simProperties.getProperty(SimConfigurationProperty.outputFormat.toString());
		if (f != null) {
			// TODO - standardize constructs / use factory
			if (f.equals(OutputFormat.TXT.name())) {
				renderer = new TabularRenderer(resultOutStream);
			} else if (f.equals(OutputFormat.CSV.name())) {
				renderer = new TabularRenderer(resultOutStream, ",", "# ");				
			} else if (f.equals(OutputFormat.ROW.name())) {
				renderer = new DelimitedLineRenderer(resultOutStream);
			} else if (f.equals(OutputFormat.JSON.name())) {
				renderer = new JSONRenderer(resultOutStream);
			} else if (f.toLowerCase().equals(OutputFormat.OWL.name().toLowerCase())) {
				renderer = new OWLRenderer(resultOutStream);
			} else if (f.toLowerCase().equals(OutputFormat.FORMATTED.name().toLowerCase())) {
				renderer = new FormattedRenderer(resultOutStream, new OWLPrettyPrinter(g));
			}
		}
		if (renderer == null) {
			// default
			renderer = new TabularRenderer(resultOutStream);				
		}
		renderer.setGraph(g);
		return renderer;
	}

	/**
	 * Computes & reports on some summary statistics on the loaded data set.
	 * Will iterate through all individuals, and generate summary
	 * statistics for each, reporting on them at the individual level,
	 * summarizing at the IDspace level, and computing an overall summary of
	 * all loaded instances.
	 * Tab-delimited output format: 
	 * idSpace | individual | n | min | max | mean | stdev
	 * 
	 * @param opts 
	 * 
	 * @throws Exception
	 */
	@CLIMethod("--show-instance-stats")
	public void instanceStats(Opts opts) throws Exception {
		try {
			loadProperties(opts);

			pproc = new NullSimPreProcessor();
			pproc.setInputOntology(g.getSourceOntology());
			pproc.setOutputOntology(g.getSourceOntology());
			if (owlsim == null) {
				owlsim = owlSimFactory.createOwlSim(g.getSourceOntology());
				((SimpleOwlSim) owlsim).setSimPreProcessor(pproc);
				owlsim.createElementAttributeMapFromOntology();
			}
			Set<OWLNamedIndividual> insts = pproc.getOutputOntology()
					.getIndividualsInSignature();
			LOG.info("Computing summary stats " + insts.size() + " individuals");

			//			SummaryStatistics meanOfIndividualSums = new SummaryStatistics();

			HashMap<String, ArrayList<SimStats>> statsPerIndividualPerIDSpace = new HashMap<String, ArrayList<SimStats>>();
			HashMap<String, SummaryStatistics> meanOfIndividualSumsPerIDSpace = new HashMap<String, SummaryStatistics>();

			//for each of the idspaces, calculate the basic stats of the 
			//loaded individuals
			// idSpace | individual | n | min | max | mean | stdev | sum
			StringBuffer header = new StringBuffer();
			header.append("ID Space").append("\t");
			header.append("Instance").append("\t");
			header.append("N").append("\t");
			header.append("Min IC").append("\t");
			header.append("Max IC").append("\t");
			header.append("Mean IC").append("\t");
			header.append("Stdev IC").append("\t");
			header.append("Sum IC");
			resultOutStream.println(header);

			for (OWLNamedIndividual i : insts) {

				SimStats statsPerIndividual = new SimStats();
				String idSpace = getIDSpace(i);

				ArrayList<SimStats> statsPerIDSpace;
				SummaryStatistics sumsPerIDSpace;

				if (!statsPerIndividualPerIDSpace.containsKey(idSpace)) {
					LOG.info("New IDspace found:" + idSpace);
					statsPerIDSpace = new ArrayList<SimStats>();
					sumsPerIDSpace = new SummaryStatistics();
				} else {
					statsPerIDSpace = statsPerIndividualPerIDSpace.get(idSpace);
					sumsPerIDSpace = meanOfIndividualSumsPerIDSpace.get(idSpace);
				}
				for (OWLClass c : owlsim.getAttributesForElement(i)) {
					statsPerIndividual.addIndividualIC(owlsim.getInformationContentForAttribute(c));  		
				}
				statsPerIDSpace.add(statsPerIndividual);
				statsPerIndividualPerIDSpace.put(idSpace, statsPerIDSpace);
				sumsPerIDSpace.addValue(statsPerIndividual.individualsIC.getSum());
				meanOfIndividualSumsPerIDSpace.put(idSpace,  sumsPerIDSpace);

				// idSpace | individual | n | min | max | mean | stdev | sum
				resultOutStream.print(idSpace);
				resultOutStream.print("\t");
				resultOutStream.print(g.getIdentifier(i));
				resultOutStream.print("\t");
				resultOutStream.print(statsPerIndividual.individualsIC.getN());
				resultOutStream.print("\t");
				resultOutStream.print(statsPerIndividual.individualsIC.getMin());
				resultOutStream.print("\t");
				resultOutStream.print(statsPerIndividual.individualsIC.getMax());
				resultOutStream.print("\t");
				resultOutStream.print(statsPerIndividual.individualsIC.getMean());
				resultOutStream.print("\t");
				resultOutStream.print(statsPerIndividual.individualsIC.getStandardDeviation());
				resultOutStream.print("\t");
				resultOutStream.print(statsPerIndividual.individualsIC.getSum());
				resultOutStream.println();
				resultOutStream.flush();
			}
			LOG.info("FINISHED computing summary stats for " + insts.size() + " individuals for the following IDspaces: " + statsPerIndividualPerIDSpace.keySet().toString());

			resultOutStream.println("Summary per IDspace:");
			header = new StringBuffer();
			header.append("ID Space").append("\t");
			header.append("\t");
			header.append("N").append("\t");
			header.append("Min IC").append("\t");
			header.append("Max IC").append("\t");
			header.append("Mean IC").append("\t");
			header.append("Stdev IC").append("\t");
			header.append("Sum IC").append("\t");
			header.append("Mean of Invidivual Sums IC");
			resultOutStream.println(header);

			Collection<SummaryStatistics> overallaggregate = new ArrayList<SummaryStatistics>();

			for (String idSpace : statsPerIndividualPerIDSpace.keySet()) {

				Collection<SummaryStatistics> aggregate = new ArrayList<SummaryStatistics>();

				for (SimStats statsPerIndividual : statsPerIndividualPerIDSpace.get(idSpace)) {
					aggregate.add(statsPerIndividual.individualsIC);
					overallaggregate.add(statsPerIndividual.individualsIC);

				}
				StatisticalSummary aggregatedStats = AggregateSummaryStatistics.aggregate(aggregate);

				resultOutStream.print(idSpace + "-overall");
				resultOutStream.print("\t");
				resultOutStream.print("\t");
				resultOutStream.print(aggregatedStats.getN());
				resultOutStream.print("\t");
				resultOutStream.print(aggregatedStats.getMin());
				resultOutStream.print("\t");
				resultOutStream.print(aggregatedStats.getMax());
				resultOutStream.print("\t");
				resultOutStream.print(aggregatedStats.getMean());
				resultOutStream.print("\t");
				resultOutStream.print(aggregatedStats.getStandardDeviation());				
				resultOutStream.print("\t");
				resultOutStream.print(aggregatedStats.getSum());		
				resultOutStream.print("\t");
				resultOutStream.println(meanOfIndividualSumsPerIDSpace.get(idSpace).getMean());
				resultOutStream.flush();

			}

			StatisticalSummary overallaggregatedStats = AggregateSummaryStatistics.aggregate(overallaggregate);
			resultOutStream.print("All");
			resultOutStream.print("\t");
			resultOutStream.print("\t");
			resultOutStream.print(overallaggregatedStats.getN());
			resultOutStream.print("\t");
			resultOutStream.print(overallaggregatedStats.getMin());
			resultOutStream.print("\t");
			resultOutStream.print(overallaggregatedStats.getMax());
			resultOutStream.print("\t");
			resultOutStream.print(overallaggregatedStats.getMean());
			resultOutStream.print("\t");
			resultOutStream.print(overallaggregatedStats.getStandardDeviation());				
			resultOutStream.print("\t");
			resultOutStream.println(overallaggregatedStats.getSum());				
			resultOutStream.flush();

		} finally {
			IOUtils.closeQuietly(resultOutStream);
		}

	}	

	/**
	 * Helper function to parse out the OBO-style "idspace" from any individual
	 * It splits on the underscore in the last element if an individual's IRI, like:
	 * http://purl.obolibrary.org/obo/HP_0002715
	 * will give you "HP"
	 * This assumes that IDspaces can consist only of letters, and the id
	 * itself is consists only of numbers or letters.
	 * @param i
	 * @return idSpace string, or empty string if no match is found
	 */
	private String getIDSpace(OWLNamedIndividual i) {
		Pattern idPattern = Pattern.compile("/[a-zA-Z]+_[a-zA-Z0-9]+$");
		Matcher m = idPattern.matcher(i.getIRI().toString());
		String id = "";
		String idSpace  = "";
		if (m.find()) {
			id = m.group();
			id = id.replaceFirst("/", "");
			idSpace = id.split("_")[0];
		}
		return idSpace;
	}

	/**
	 * Prints a tab-delimited report of the IC measures for each annotation (class)
	 * made to each instance.  Report could be used for external statistical
	 * analysis.
	 * instance ID | class ID | IC
	 * @param opts
	 * @throws Exception
	 */
	@CLIMethod("--show-instance-IC-values")
	public void instanceICValues(Opts opts) throws Exception {
		try {
			loadProperties(opts);
			pproc = new NullSimPreProcessor();
			pproc.setInputOntology(g.getSourceOntology());
			pproc.setOutputOntology(g.getSourceOntology());
			if (owlsim == null) {
				owlsim = owlSimFactory.createOwlSim(g.getSourceOntology());
				((SimpleOwlSim) owlsim).setSimPreProcessor(pproc);
				owlsim.createElementAttributeMapFromOntology();
			}
			Set<OWLNamedIndividual> insts = pproc.getOutputOntology()
					.getIndividualsInSignature();
			LOG.info("Writing IC values for all " + insts.size() + " annotations.");

			for (OWLNamedIndividual i : insts) {
				for (OWLClass c : owlsim.getAttributesForElement(i)) {
					resultOutStream.print(g.getIdentifier(i));
					resultOutStream.print("\t");
					resultOutStream.print(g.getIdentifier(c));
					resultOutStream.print("\t");
					resultOutStream.print(owlsim.getInformationContentForAttribute(c));
					resultOutStream.println();
					resultOutStream.flush();
				}
			}

		} finally {
			IOUtils.closeQuietly(resultOutStream);
		}

	}	

	/**
	 * Given a set of classes, will print a table of grouping class(es) for each
	 * annotation, and show a binary flag if the given annotation belongs to 
	 * that class. For example, if a gene is annotated to a given HPO class, and
	 * the user supplies a set of terms to group the phenotypes (abnormality of
	 * the eye, abnormality of the ear, etc), this will indicate, based on the
	 * reasoned subsumption hierarchy, which of the grouping classes the
	 * annotation belongs.   Similar to {@link #showAttributeGroupingsAsList}.
	 * 
	 * @param opts
	 * @throws Exception
	 */
	@CLIMethod("--annotate-attr-groupings-as-table")
	public void showAttributeGroupingsInTable(Opts opts) throws Exception {
		//for testing: phenotype, inheritance, onset
		//TODO: load as file?
		//TODO:do we want a default?
		//the ids in the following list must have the id form idspace:number
		//    String myGroupingClasses = "HP:0000118,HP:0000005,HP:0000004";  
		opts.info("-gc CLASSLIST","List of ontology classes for grouping attributes, comma-separated");
		String myGroupingClasses = "";
		if (opts.nextEq("-gc")) {			
			myGroupingClasses = opts.nextOpt();
		} else {
			LOG.error("No grouping classes specified");
			return;
		}

		Set<OWLClass> groupingClasses =  new HashSet<OWLClass>();
		for (String id : myGroupingClasses.split(",")) {
			OWLClass c = g.getOWLClassByIdentifier(id);
			groupingClasses.add(c);
		}
		loadProperties(opts);
		OWLPrettyPrinter owlpp = getPrettyPrinter();
		pproc = new NullSimPreProcessor();
		pproc.setInputOntology(g.getSourceOntology());
		pproc.setOutputOntology(g.getSourceOntology());
		if (owlsim == null) {
			owlsim = owlSimFactory.createOwlSim(g.getSourceOntology());
			((SimpleOwlSim) owlsim).setSimPreProcessor(pproc);
			owlsim.createElementAttributeMapFromOntology();
		}
		Set<OWLNamedIndividual> insts = pproc.getOutputOntology()
				.getIndividualsInSignature();
		LOG.info("Identifying groupings via subsumption for all " + insts.size() + " annotations.");

		StringBuffer header = new StringBuffer();
		header.append("Individual ID").append("\t");
		header.append("Class ID").append("\t");
		header.append("IC");
		for (OWLClass gc : groupingClasses) {
			header.append("\t");
			header.append(owlpp.render(gc));
		}
		resultOutStream.println(header);
		resultOutStream.flush();

		for (OWLNamedIndividual i : insts) {
			for (OWLClass c : owlsim.getAttributesForElement(i)) {
				resultOutStream.print(g.getIdentifier(i));
				resultOutStream.print("\t");
				resultOutStream.print(owlpp.render(c));
				resultOutStream.print("\t");
				resultOutStream.print(owlsim.getInformationContentForAttribute(c));
				for (OWLClass gc : groupingClasses) {
					resultOutStream.print("\t");
					if (g.getAncestors(c).contains(gc)) {
						resultOutStream.print("1");
					} else {
						resultOutStream.print("0");
					} 				
				}
				resultOutStream.println();
				resultOutStream.flush();
			}
		}


	}

	/**
	 * Given a set of classes, will print the grouping class(es) for each
	 * annotation in a comma-separated list. For example, if a gene is annotated 
	 * to a given HPO class, and
	 * the user supplies a set of term ids to group the phenotypes (abnormality of
	 * the eye, abnormality of the ear, etc), this will indicate, based on the
	 * reasoned subsumption hierarchy, which of the grouping classes the
	 * annotation belongs.  If an annotation does not belong to any of the
	 * grouping classes, the column will be empty.  Similar to {@link #showAttributeGroupingsInTable}
	 * 
	 * @param opts
	 * @throws Exception
	 */
	@CLIMethod("--annotate-attr-groupings-as-list")
	public void showAttributeGroupingsAsList(Opts opts) throws Exception {
		opts.info("-gc CLASSLIST","List of ontology classes for grouping attributes");
		String myGroupingClasses = "";
		if (opts.nextEq("-gc")) {			
			myGroupingClasses = opts.nextOpt();
		} else {
			LOG.error("No grouping classes specified");
			return;
		}

		Set<OWLClass> groupingClasses =  new HashSet<OWLClass>();
		for (String id : myGroupingClasses.split(",")) {
			OWLClass c = g.getOWLClassByIdentifier(id);
			groupingClasses.add(c);
		}
		loadProperties(opts);
		OWLPrettyPrinter owlpp = getPrettyPrinter();
		pproc = new NullSimPreProcessor();
		pproc.setInputOntology(g.getSourceOntology());
		pproc.setOutputOntology(g.getSourceOntology());
		if (owlsim == null) {
			owlsim = owlSimFactory.createOwlSim(g.getSourceOntology());
			((SimpleOwlSim) owlsim).setSimPreProcessor(pproc);
			owlsim.createElementAttributeMapFromOntology();
		}
		Set<OWLNamedIndividual> insts = pproc.getOutputOntology()
				.getIndividualsInSignature();
		LOG.info("Identifying groupings via subsumption for all " + insts.size() + " annotations.");

		StringBuffer header = new StringBuffer();
		header.append("Individual ID").append("\t");
		header.append("Class ID").append("\t");
		header.append("IC").append("\t");
		header.append("Grouping Classes");
		resultOutStream.println(header);
		resultOutStream.flush();

		for (OWLNamedIndividual i : insts) {
			for (OWLClass c : owlsim.getAttributesForElement(i)) {
				resultOutStream.print(g.getIdentifier(i));
				resultOutStream.print("\t");
				resultOutStream.print(owlpp.render(c));
				resultOutStream.print("\t");
				resultOutStream.print(owlsim.getInformationContentForAttribute(c));
				resultOutStream.print("\t");
				List<String> l = new ArrayList<String>();
				for (OWLClass gc : groupingClasses) {
					if (g.getAncestors(c).contains(gc)) {
						l.add(owlpp.render(gc));
					} 				
				}
				resultOutStream.print(StringUtils.join(l," | "));
				resultOutStream.println();
				resultOutStream.flush();
			}
		}
	}	
}
