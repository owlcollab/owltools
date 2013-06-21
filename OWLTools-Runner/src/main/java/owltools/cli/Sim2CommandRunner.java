package owltools.cli;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
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
	 * {@link attributeAllByAll }, which outputs the comparison of any two attributes only
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
			Set<OWLNamedIndividual> insts = sos.getSourceOntology().getIndividualsInSignature();
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
	 * @param scores
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
				sos.simStats.setMax(Metric.MAXIC,scores.maxIC.score);
				break; // only need to test once.
			}
		}

		//only do this test if we end up using the J measures
		for (String metric : metrics) {
			if (sos.isJmetric(metric)) {
				double simJ = sos.getElementJaccardSimilarity(i, j);
				if (simJ < getPropertyAsDouble(SimConfigurationProperty.minimumSimJ)) {
					numberOfPairsFiltered++;
					return scores;
				}
				scores.simjScore = simJ;
				sos.simStats.setMax(Metric.SIMJ,scores.simjScore);
				break; // only need to test once.
			}
		}

		for (String metric : metrics) {
			if (metric.equals(Metric.IC_MCS.toString())) {
				scores.bmaAsymIC = sos.getSimilarityBestMatchAverage(i, j,
						Metric.IC_MCS, Direction.A_TO_B);
				sos.simStats.setMax(Metric.IC_MCS,scores.bmaAsymIC.score);
				// TODO: do reciprocal BMA
				scores.bmaSymIC = sos.getSimilarityBestMatchAverage(i, j,
						Metric.IC_MCS, Direction.AVERAGE);
			} else if (metric.equals(Metric.JACCARD.toString())) {
				scores.bmaAsymJ = sos.getSimilarityBestMatchAverage(i, j,
						Metric.JACCARD, Direction.A_TO_B);
				// TODO: do reciprocal BMA
				sos.simStats.setMax(Metric.JACCARD,scores.bmaAsymJ.score);
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
			if (sos == null) {
				pproc = new NullSimPreProcessor();
				if (simProperties.containsKey(SimConfigurationProperty.analysisRelation.toString())) {
					pproc = new PropertyViewSimPreProcessor();
					String relId = (String) simProperties.get((SimConfigurationProperty.analysisRelation.toString()));
					OWLObjectProperty rel = g.getOWLObjectPropertyByIdentifier(relId);
					PropertyViewSimPreProcessor xpproc = ((PropertyViewSimPreProcessor)pproc);

					LOG.info("View relation = "+rel);
					xpproc.analysisRelation = rel;
				}
				pproc.setSimProperties(simProperties);
				pproc.setInputOntology(g.getSourceOntology());
				pproc.setOutputOntology(g.getSourceOntology());
				sos = new SimpleOwlSim(g.getSourceOntology());
				sos.setSimPreProcessor(pproc);
				pproc.preprocess();
				
			}
			sos.createElementAttributeMapFromOntology();
			runOwlSim(opts);
		} finally {
			if (pproc != null)
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
	public void simBasicStats() throws Exception {
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
		opts.info("", "all by all comparison of classes (attributes)");
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
		SimResultRenderer renderer;
		String f = sos.getSimProperties().getProperty(SimConfigurationProperty.outputFormat.toString());
		if (f == null || f.equals(OutputFormat.TXT.name())) {
			renderer = new TabularRenderer(resultOutStream);
		} else if (f.equals(OutputFormat.CSV.name())) {
			renderer = new TabularRenderer(resultOutStream, ",", "# ");				
		} else if (f.equals(OutputFormat.ROW.name())) {
			renderer = new DelimitedLineRenderer(resultOutStream);
		} else if (f.equals(OutputFormat.JSON.name())) {
			//TODO add JSON renderer call when completed
			renderer = new TabularRenderer(resultOutStream);
		} else {
			renderer = new TabularRenderer(resultOutStream);				
		}
		return renderer;
	}

}
