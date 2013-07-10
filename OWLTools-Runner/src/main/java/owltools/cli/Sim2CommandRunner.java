package owltools.cli;

import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.commons.math3.stat.descriptive.AggregateSummaryStatistics;
import org.apache.log4j.Logger;
import org.coode.owlapi.turtle.TurtleOntologyFormat;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.Node;

import owltools.cli.tools.CLIMethod;
import owltools.graph.OWLGraphWrapper;
import owltools.io.OWLPrettyPrinter;
import owltools.sim.io.DelimitedLineRenderer;
import owltools.sim.io.SimResultRenderer;
import owltools.sim.io.SimResultRenderer.AttributesSimScores;
import owltools.sim.io.SimResultRenderer.IndividualSimScores;
import owltools.sim.io.TabularRenderer;
import owltools.sim2.SimStats;
import owltools.sim2.SimpleOwlSim;
import owltools.sim2.SimpleOwlSim.Direction;
import owltools.sim2.SimpleOwlSim.EnrichmentConfig;
import owltools.sim2.SimpleOwlSim.EnrichmentResult;
import owltools.sim2.SimpleOwlSim.Metric;
import owltools.sim2.SimpleOwlSim.OutputFormat;
import owltools.sim2.SimpleOwlSim.ScoreAttributePair;
import owltools.sim2.SimpleOwlSim.ScoreAttributesPair;
import owltools.sim2.SimpleOwlSim.SimConfigurationProperty;
import owltools.sim2.preprocessor.NullSimPreProcessor;
import owltools.sim2.preprocessor.PhenoSimHQEPreProcessor;
import owltools.sim2.preprocessor.PropertyViewSimPreProcessor;
import owltools.sim2.preprocessor.SimPreProcessor;



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

	SimpleOwlSim sos;

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

	public void attributeAllByAll(Opts opts) {
		try {
			sos.setSimProperties(simProperties);
			Set<OWLClass> atts = sos.getAllAttributeClasses();
			LOG.info("All by all for " + atts.size() + " classes");

			//set the renderer
			SimResultRenderer renderer = setRenderer();

			// print a header in the file that details what was done
			for (Object k : simProperties.keySet()) {
				renderer.printComment(k + " = "
						+ simProperties.getProperty(k.toString()));
			}

			for (OWLClass i : atts) {
				for (OWLClass j : atts) {
					//TODO: isComparable?
					AttributesSimScores scores = computeSim(i, j);
					renderSim(scores, renderer);
				}
			}
			LOG.info("FINISHED All by all for " + atts.size() + " classes");
			// LOG.info("Number of pairs filtered as they scored beneath threshold: "+this.numberOfPairsFiltered);
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
	 */
	public void attributeSimilarityAllByAllPairwise(Opts opts) {
		try {
			sos.setSimProperties(simProperties);
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
						attrCounter += (sos.getAttributesForElement(i).size() + sos
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
	 */
	public void attributeSimilarityByPair(OWLNamedIndividual i,
			OWLNamedIndividual j) {
		if (isComparable(i, j)) {
			try {
				sos.setSimProperties(simProperties);

				//set the renderer
				SimResultRenderer renderer = setRenderer();


				Set<OWLClass> iatts = sos.getAttributesForElement(i);
				Set<OWLClass> jatts = sos.getAttributesForElement(j);
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
	 */
	public void runOwlSim(Opts opts) {
		try {
			sos.setSimProperties(simProperties);
			if (opts.nextEq("-q")) {
				runOwlSimOnQuery(opts, opts.nextOpt());
				return;
			}
			Set<OWLNamedIndividual> insts = pproc.getOutputOntology()
					.getIndividualsInSignature();
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
						sos.simStats.incrementIndividualPairCount();
						sos.simStats.incrementClassPairCount(scores.numberOfElementsI * scores.numberOfElementsJ);
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

	private void runOwlSimOnQuery(Opts opts, String q) {
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

	private boolean isComparable(OWLNamedIndividual i, OWLNamedIndividual j) {
		String cmp = getProperty(SimConfigurationProperty.compare);
		if (cmp == null) {
			return i.compareTo(j) > 0;
		} else {
			String[] idspaces = cmp.split(",");
			if (i.getIRI().toString().contains("/" + idspaces[0] + "_")
					&& j.getIRI().toString().contains("/" + idspaces[1] + "_")) {
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
	 */
	private AttributesSimScores computeSim(OWLClass a, OWLClass b) {

		AttributesSimScores scores = new AttributesSimScores(a, b);
		double simJScore = sos.getAttributeJaccardSimilarity(a, b);
		if (simJScore < getPropertyAsDouble(SimConfigurationProperty.minimumSimJ)) {
			numberOfPairsFiltered++;
			return scores;
		}
		scores.simJScore = simJScore;

		scores.AsymSimJScore = sos.getAsymmerticAttributeJaccardSimilarity(a, b);

		ScoreAttributePair lcsScore = sos.getLowestCommonSubsumerIC(a, b);
		if (lcsScore.score < getPropertyAsDouble(SimConfigurationProperty.minimumMaxIC)) {
			numberOfPairsFiltered++;
			return scores;
		}
		scores.lcsScore = lcsScore;

		return scores;
	}

	private IndividualSimScores computeSim(OWLNamedIndividual i,
			OWLNamedIndividual j) {

		IndividualSimScores scores = new IndividualSimScores(i, j);

		scores.numberOfElementsI = sos.getAttributesForElement(i).size();
		scores.numberOfElementsJ = sos.getAttributesForElement(j).size();
				
		String[] metrics = getProperty(SimConfigurationProperty.scoringMetrics)
				.split(",");
		
		// only do this test if we end up using the IC measures
		for (String metric : metrics) {
			if (sos.isICmetric(metric)) {
				ScoreAttributesPair maxIC = sos.getSimilarityMaxIC(i, j);
				if (maxIC.score < getPropertyAsDouble(SimConfigurationProperty.minimumMaxIC)) {
					numberOfPairsFiltered++;
					return scores;
				}
				scores.maxIC = maxIC;
				sos.simStats.setValue(Metric.MAXIC,scores.maxIC.score);
				break; // only need to test once.
			}
		}

		// only do this test if we end up using the J measures
		for (String metric : metrics) {
			if (sos.isJmetric(metric)) {
				double simJ = sos.getElementJaccardSimilarity(i, j);
				if (simJ < getPropertyAsDouble(SimConfigurationProperty.minimumSimJ)) {
					numberOfPairsFiltered++;
					return scores;
				}
				scores.simjScore = simJ;
				sos.simStats.setValue(Metric.SIMJ,scores.simjScore);
				break; // only need to test once.
			}
		}

		for (String metric : metrics) {
			if (metric.equals(Metric.IC_MCS.toString())) {
				scores.bmaAsymIC = sos.getSimilarityBestMatchAverage(i, j,
						Metric.IC_MCS, Direction.A_TO_B);
				sos.simStats.setValue(Metric.IC_MCS,scores.bmaAsymIC.score);
				// TODO: do reciprocal BMA
				scores.bmaSymIC = sos.getSimilarityBestMatchAverage(i, j,
						Metric.IC_MCS, Direction.AVERAGE);
			} else if (metric.equals(Metric.JACCARD.toString())) {
				scores.bmaAsymJ = sos.getSimilarityBestMatchAverage(i, j,
						Metric.JACCARD, Direction.A_TO_B);
				// TODO: do reciprocal BMA
				sos.simStats.setValue(Metric.JACCARD,scores.bmaAsymJ.score);
				scores.bmaSymJ = sos.getSimilarityBestMatchAverage(i, j,
						Metric.JACCARD, Direction.AVERAGE);
			} else if (metric.equals(Metric.GIC.toString())) {
				scores.simGIC = sos.getElementGraphInformationContentSimilarity(i, j);
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
	public void phenoSim(Opts opts) throws Exception {
		loadProperties(opts);
		try {
			pproc = new PhenoSimHQEPreProcessor();
			pproc.setSimProperties(simProperties);

			pproc.setInputOntology(g.getSourceOntology());
			pproc.setOutputOntology(g.getSourceOntology());
			pproc.preprocess();
			pproc.getReasoner().flush();
			sos = new SimpleOwlSim(g.getSourceOntology());
			sos.setSimPreProcessor(pproc);
			sos.createElementAttributeMapFromOntology();
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
	public void phenoSimAttributeMatrix(Opts opts) throws Exception {
		loadProperties(opts);
		try {
			pproc = new PhenoSimHQEPreProcessor();
			pproc.setSimProperties(simProperties);

			pproc.setInputOntology(g.getSourceOntology());
			pproc.setOutputOntology(g.getSourceOntology());
			pproc.preprocess();
			pproc.getReasoner().flush();
			sos = new SimpleOwlSim(g.getSourceOntology());
			sos.setSimPreProcessor(pproc);
			sos.createElementAttributeMapFromOntology();
			// pproc.saveState("/tmp/phenosim-analysis-ontology.owl");
			attributeAllByAll(opts);
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
			sos = new SimpleOwlSim(g.getSourceOntology());
			sos.setSimPreProcessor(pproc);
			sos.createElementAttributeMapFromOntology();
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
			sos = new SimpleOwlSim(g.getSourceOntology());
			sos.setSimPreProcessor(pproc);
			pproc.preprocess();
			sos.createElementAttributeMapFromOntology();
			runOwlSim(opts);
		} finally {
			pproc.dispose();
		}
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
		LOG.info(Metric.JACCARD.name() + " max: " + sos.simStats.getMax(Metric.JACCARD));		
	}	

	@CLIMethod("--sim-dl-query")
	public void simDlQuery(Opts opts) throws Exception {
		loadProperties(opts);
		try {
			// TODO
			pproc = new NullSimPreProcessor();
			pproc.setInputOntology(g.getSourceOntology());
			pproc.setOutputOntology(g.getSourceOntology());
			sos = new SimpleOwlSim(g.getSourceOntology());
			sos.setSimPreProcessor(pproc);
			sos.createElementAttributeMapFromOntology();
			runOwlSim(opts);
		} finally {
			pproc.dispose();
		}
	}

	@CLIMethod("--sim-compare-atts")
	public void simAttMatch(Opts opts) throws Exception {
		// assumes that individuals in abox are of types named classes in tbox
		loadProperties(opts);
		try {
			pproc = new NullSimPreProcessor();
			pproc.setInputOntology(g.getSourceOntology());
			pproc.setOutputOntology(g.getSourceOntology());
			sos = new SimpleOwlSim(g.getSourceOntology());
			sos.setSimPreProcessor(pproc);
			sos.createElementAttributeMapFromOntology();
			this.attributeAllByAll(opts);
		} finally {
			pproc.dispose();
		}
	}

	@CLIMethod("--sim-pair-compare-atts")
	public void simPairwiseAttMatch(Opts opts) throws Exception {
		// assumes that individuals in abox are of types named classes in tbox
		loadProperties(opts);
		try {
			pproc = new NullSimPreProcessor();
			pproc.setInputOntology(g.getSourceOntology());
			pproc.setOutputOntology(g.getSourceOntology());
			sos = new SimpleOwlSim(g.getSourceOntology());
			sos.setSimPreProcessor(pproc);
			sos.createElementAttributeMapFromOntology();
			this.attributeSimilarityAllByAllPairwise(opts);
		} finally {
			pproc.dispose();
		}
	}

	@CLIMethod("--sim-save-lcs-cache")
	public void simSaveLCSCache(Opts opts) throws Exception {
		opts.info("OUTFILE", "saves a LCS cache to a file");
		Double thresh = null;
		while (opts.hasOpts()) {
			if (opts.nextEq("-m|--min-ic")) {
				thresh = Double.valueOf(opts.nextOpt());
			}
			else {
				break;
			}
		}
		sos.saveLCSCache(opts.nextOpt(), thresh);
	}

	@CLIMethod("--sim-load-lcs-cache")
	public void simLoadLCSCache(Opts opts) throws Exception {
		opts.info("INFILE", "loads a LCS cache from a file");
		if (sos == null) {
			sos = new SimpleOwlSim(g.getSourceOntology());
		}
		sos.loadLCSCache(opts.nextOpt());
	}

	@CLIMethod("--sim-save-ic-cache")
	public void simSaveICCache(Opts opts) throws Exception {
		opts.info("OUTFILE", "saves ICs as RDF/turtle cache");
		if (sos == null) {
			sos = new SimpleOwlSim(g.getSourceOntology());
		}
		OWLOntology o = sos.cacheInformationContentInOntology();
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
		if (sos == null) {
			sos = new SimpleOwlSim(g.getSourceOntology());
		}
		OWLOntology o = 
			g.getManager().loadOntologyFromOntologyDocument(opts.nextFile());
		sos.setInformationContentFromOntology(o);
	}




	@CLIMethod("--sim-lcs")
	public void simLCS(Opts opts) throws Exception {
		opts.info("", "find LCS of two classes");
		loadProperties(opts);
		OWLClass c1 = (OWLClass) this.resolveClass(opts.nextOpt());
		OWLClass c2 = (OWLClass) this.resolveClass(opts.nextOpt());
		OWLPrettyPrinter owlpp = getPrettyPrinter();
		if (sos == null) {
			sos = new SimpleOwlSim(g.getSourceOntology());
			sos.createElementAttributeMapFromOntology();
		}
		sos.createElementAttributeMapFromOntology();
		Set<Node<OWLClass>> lcsSet = sos.getNamedLowestCommonSubsumers(c1, c2);

		for (Node<OWLClass> lcsNode : lcsSet) {
			System.out.println(owlpp.render(lcsNode.getRepresentativeElement()));
		}
	}


	// TODO
	@CLIMethod("--enrichment-analysis")
	public void owlsimEnrichmentAnalysis(Opts opts) throws Exception {
		opts.info("", "performs enrichment on gene set. TODO");
		OWLPrettyPrinter owlpp = getPrettyPrinter();
		if (sos == null) {
			sos = new SimpleOwlSim(g.getSourceOntology());
			sos.createElementAttributeMapFromOntology();
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
		sos.setEnrichmentConfig(ec);
		OWLClass rc1 = this.resolveClass(opts.nextOpt());
		OWLClass rc2 = this.resolveClass(opts.nextOpt());
		OWLClass pc = g.getDataFactory().getOWLThing();
		List<EnrichmentResult> results = sos.calculateAllByAllEnrichment(pc, rc1,
				rc2);
		for (EnrichmentResult result : results) {
			System.out.println(render(result, owlpp));
		}
	}

	// NEW
	@CLIMethod("--all-by-all-enrichment-analysis")
	public void owlsimEnrichmentAnalysisAllByAll(Opts opts) throws Exception {
		opts.info("", "performs all by all enrichment");
		OWLPrettyPrinter owlpp = getPrettyPrinter();
		if (sos == null) {
			sos = new SimpleOwlSim(g.getSourceOntology());
			sos.createElementAttributeMapFromOntology();
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
		sos.setEnrichmentConfig(ec);
		OWLClass rc1 = this.resolveClass(opts.nextOpt());
		OWLClass rc2 = this.resolveClass(opts.nextOpt());
		OWLClass pc = g.getDataFactory().getOWLThing();
		List<EnrichmentResult> results = sos.calculateAllByAllEnrichment(pc, rc1,
				rc2);
		for (EnrichmentResult result : results) {
			System.out.println(render(result, owlpp));
		}
	}

	@CLIMethod("--class-IC-pairs")
	public void classICPairs(Opts opts) throws Exception {
		opts.info("", "show all classes with their IC");
		OWLPrettyPrinter owlpp = getPrettyPrinter();
		if (sos == null) {
			sos = new SimpleOwlSim(g.getSourceOntology());
			sos.createElementAttributeMapFromOntology();
		}
		for (OWLClass c : g.getSourceOntology().getClassesInSignature()) {
			Double ic = sos.getInformationContentForAttribute(c);
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
	
	/**
	 * Set the Renderer to use based on the configurable parameter: outputFormat
	 * @return renderer
	 */
	private SimResultRenderer setRenderer() {
		//set the renderer
		SimResultRenderer renderer = null;
		String f = sos.getSimProperties().getProperty(SimConfigurationProperty.outputFormat.toString());
		if (f != null) {
			if (f.equals(OutputFormat.TXT.name())) {
				renderer = new TabularRenderer(resultOutStream);
			} else if (f.equals(OutputFormat.CSV.name())) {
				renderer = new TabularRenderer(resultOutStream, ",", "# ");				
			} else if (f.equals(OutputFormat.ROW.name())) {
				renderer = new DelimitedLineRenderer(resultOutStream);
			} else if (f.equals(OutputFormat.JSON.name())) {
				//TODO add JSON renderer call when completed
				renderer = new TabularRenderer(resultOutStream);
			}
		}
		if (renderer == null) {
			// default
			renderer = new TabularRenderer(resultOutStream);				
		}
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
			if (sos == null) {
  			sos = new SimpleOwlSim(g.getSourceOntology());
	  		sos.setSimPreProcessor(pproc);
		  	sos.createElementAttributeMapFromOntology();
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
  			for (OWLClass c : sos.getAttributesForElement(i)) {
  				statsPerIndividual.addIndividualIC(sos.getInformationContentForAttribute(c));  		
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
			if (sos == null) {
  			sos = new SimpleOwlSim(g.getSourceOntology());
	  		sos.setSimPreProcessor(pproc);
		  	sos.createElementAttributeMapFromOntology();
			}
			Set<OWLNamedIndividual> insts = pproc.getOutputOntology()
					.getIndividualsInSignature();
			LOG.info("Writing IC values for all " + insts.size() + " annotations.");

    	for (OWLNamedIndividual i : insts) {
  			for (OWLClass c : sos.getAttributesForElement(i)) {
    			resultOutStream.print(g.getIdentifier(i));
    			resultOutStream.print("\t");
    			resultOutStream.print(g.getIdentifier(c));
    			resultOutStream.print("\t");
    			resultOutStream.print(sos.getInformationContentForAttribute(c));
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
		if (sos == null) {
			sos = new SimpleOwlSim(g.getSourceOntology());
  		sos.setSimPreProcessor(pproc);
	  	sos.createElementAttributeMapFromOntology();
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
			for (OWLClass c : sos.getAttributesForElement(i)) {
				resultOutStream.print(g.getIdentifier(i));
 				resultOutStream.print("\t");
 				resultOutStream.print(owlpp.render(c));
  			resultOutStream.print("\t");
  			resultOutStream.print(sos.getInformationContentForAttribute(c));
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
		if (sos == null) {
			sos = new SimpleOwlSim(g.getSourceOntology());
  		sos.setSimPreProcessor(pproc);
	  	sos.createElementAttributeMapFromOntology();
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
			for (OWLClass c : sos.getAttributesForElement(i)) {
				resultOutStream.print(g.getIdentifier(i));
 				resultOutStream.print("\t");
 				resultOutStream.print(owlpp.render(c));
  			resultOutStream.print("\t");
  			resultOutStream.print(sos.getInformationContentForAttribute(c));
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
