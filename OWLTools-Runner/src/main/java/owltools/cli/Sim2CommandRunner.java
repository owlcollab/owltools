package owltools.cli;

import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;

import owltools.cli.tools.CLIMethod;
import owltools.graph.OWLGraphWrapper;
import owltools.io.OWLPrettyPrinter;
import owltools.sim.io.DelimitedLineRenderer;
import owltools.sim.io.SimResultRenderer;
import owltools.sim.io.SimResultRenderer.AttributesSimScores;
import owltools.sim.io.SimResultRenderer.IndividualSimScores;
import owltools.sim2.SimpleOwlSim;
import owltools.sim2.SimpleOwlSim.Direction;
import owltools.sim2.SimpleOwlSim.EnrichmentConfig;
import owltools.sim2.SimpleOwlSim.EnrichmentResult;
import owltools.sim2.SimpleOwlSim.Metric;
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
    //TODO: load a default properties file
		simProperties.setProperty(SimConfigurationProperty.minimumMaxIC.toString(), SimConfigurationProperty.minimumMaxIC.defaultValue());
		simProperties.setProperty(SimConfigurationProperty.minimumSimJ.toString(), SimConfigurationProperty.minimumSimJ.defaultValue());

    /*by default, turn off metrics*/		
//		String[] metrics = SimConfigurationProperty.scoringMetrics.toString().split(",");
//		for (int i=0; i<Metric.values().length; i++) {
//    	simProperties.setProperty(Metric.values()[i].toString(),"0");
//    }    
		simProperties.setProperty(SimConfigurationProperty.scoringMetrics.toString(),SimConfigurationProperty.scoringMetrics.defaultValue());    
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
		opts.info("FILE", "saves similarity results as an OWL ontology. Use after --sim or --sim-all");
		pw.saveOWL(simOnt, opts.nextOpt(),g);
	}

	@CLIMethod("--merge-sim")
	public void mergeSim(Opts opts) throws Exception {
		g.mergeOntology(simOnt);
	}


	@CLIMethod("--remove-dangling-annotations")
	public void removeDangningAnnotations(Opts opts) throws Exception {
		OWLOntology ont = g.getSourceOntology();
		int n=0;
		Set<OWLAxiom> rmAxioms = new HashSet<OWLAxiom>();
		for (OWLNamedIndividual i : ont.getIndividualsInSignature()) {
			for (OWLClassAssertionAxiom ca : ont.getClassAssertionAxioms(i)) {
				OWLClassExpression cx = ca.getClassExpression();
				if (cx instanceof OWLClass) {
					OWLClass c = (OWLClass)cx;
					String label = g.getLabel(c);
					if (label == null)
						rmAxioms.add(ca);
					else
						n++;
				}
			}
		}
		LOG.info("Removing "+rmAxioms.size()+" axioms");
		ont.getOWLOntologyManager().removeAxioms(ont, rmAxioms);
		LOG.info("Remaining: "+n+" axioms");
	}

	public void attributeAllByAll(Opts opts) {
		try {
			sos.setSimProperties(simProperties);
			Set<OWLClass> atts = sos.getAllAttributeClasses();
			LOG.info("All by all for "+atts.size()+" classes");
			SimResultRenderer renderer = new DelimitedLineRenderer(resultOutStream);
			//print a header in the file that details what was done
			for (Object k : simProperties.keySet()){
				renderer.printComment(k+" = "+simProperties.getProperty(k.toString()));
			}

			for (OWLClass i : atts) {
				for (OWLClass j : atts) {
					AttributesSimScores scores = computeSim(i,j);
					renderSim(scores, renderer);
				}
			}
			LOG.info("FINISHED All by all for "+atts.size()+" classes");
			//LOG.info("Number of pairs filtered as they scored beneath threshold: "+this.numberOfPairsFiltered);
		}finally {
			IOUtils.closeQuietly(resultOutStream);
		}
	}



	/**
	 * performs all by all individual comparison
	 * @param opts 
	 */
	public void runOwlSim(Opts opts) {
		try {
			sos.setSimProperties(simProperties);
			if (opts.nextEq("-q")) {
				runOwlSimOnQuery(opts, opts.nextOpt());
				return;
			}
			Set<OWLNamedIndividual> insts = pproc.getOutputOntology().getIndividualsInSignature();
			LOG.info("All by all for "+insts.size()+" individuals");

			SimResultRenderer renderer = new DelimitedLineRenderer(resultOutStream);
			
			//print a header in the file that details what was done
			renderer.printComment("Properties for this run:");
			for (Object k : simProperties.keySet()){
				renderer.printComment(k+" = "+simProperties.getProperty(k.toString()));
				//TODO: output if the property is default
			}
			resultOutStream.flush();
			for (OWLNamedIndividual i : insts) {
				for (OWLNamedIndividual j : insts) {
					// similarity is symmetrical
					if (isComparable(i,j)) {
						IndividualSimScores scores = computeSim(i,j);
						renderSim(scores, renderer);
					}
					else {
						LOG.info("skipping "+i+" + "+j);
					}
				}
			}
			LOG.info("FINISHED All by all for "+insts.size()+" individuals");
			LOG.info("Number of pairs filtered as they scored beneath threshold: "+this.numberOfPairsFiltered);
		}
		finally{
			IOUtils.closeQuietly(resultOutStream);
		}
	}

	private void runOwlSimOnQuery(Opts opts, String q) {
		System.out.println("Query: "+q);
		OWLNamedIndividual qi = (OWLNamedIndividual) resolveEntity(q);
		System.out.println("Query Individual: "+qi);
		Set<OWLNamedIndividual> insts = pproc.getOutputOntology().getIndividualsInSignature();
		SimResultRenderer renderer = new DelimitedLineRenderer(resultOutStream);
		for (OWLNamedIndividual j : insts) {
			IndividualSimScores scores = computeSim(qi,j);
			renderSim(scores, renderer);
		}

	}

	private boolean isComparable(OWLNamedIndividual i, OWLNamedIndividual j) {
		String cmp = getProperty(SimConfigurationProperty.compare);
		if (cmp == null) {
			return i.compareTo(j) > 0;
		}
		else {
			String[] idspaces = cmp.split(",");
			if (i.getIRI().toString().contains("/"+idspaces[0]+"_") &&
					j.getIRI().toString().contains("/"+idspaces[1]+"_")) {
				return true;
			}
			else {
				return false;
			}
		}
	}

	boolean isHeaderLine = true;
	
	private void renderSim(AttributesSimScores simScores, SimResultRenderer renderer) {
		simScores.simjScoreLabel = "SimJ_Score";
		simScores.AsymSimJScoreLabel = "AsymSimJ_Score";
		simScores.lcsScorePrefix = "LCS";
		renderer.printAttributeSim(simScores, g);
	}
	
	private void renderSim(IndividualSimScores simScores, SimResultRenderer renderer) {
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
			numberOfPairsFiltered ++;
			return scores;
		}
		scores.simJScore = simJScore;
		
		scores.AsymSimJScore = sos.getAsymmerticAttributeJaccardSimilarity(a, b);
		
		ScoreAttributePair lcsScore = sos.getLowestCommonSubsumerIC(a, b);
		if ( lcsScore.score < getPropertyAsDouble(SimConfigurationProperty.minimumMaxIC)) {
			numberOfPairsFiltered ++;
			return scores;
		}
		scores.lcsScore = lcsScore;
		
		return scores;
	}

	private IndividualSimScores computeSim(OWLNamedIndividual i, OWLNamedIndividual j) {

		IndividualSimScores scores = new IndividualSimScores(i, j);
		
		scores.numberOfElementsI = sos.getAttributesForElement(i).size();
		scores.numberOfElementsJ = sos.getAttributesForElement(j).size();
		
		String[] metrics = getProperty(SimConfigurationProperty.scoringMetrics).split(",");

		//only do this test if we end up using the IC measures
		for (String metric : metrics) {
			if (sos.isICmetric(metric)) {				
				ScoreAttributesPair maxIC = sos.getSimilarityMaxIC(i, j);
				if (maxIC.score < getPropertyAsDouble(SimConfigurationProperty.minimumMaxIC)) {
					numberOfPairsFiltered ++;
					return scores;
				}
				scores.maxIC = maxIC;
				break;  //only need to test once.
			}
		}

		//only do this test if we end up using the J measures
		for (String metric : metrics) {
			if (sos.isJmetric(metric)) {				
				double simJ = sos.getElementJaccardSimilarity(i, j);
				if (simJ < getPropertyAsDouble(SimConfigurationProperty.minimumSimJ)) {
					numberOfPairsFiltered ++;
					return scores;
				}
				scores.simjScore = simJ;
				break; //only need to test once.
			}
		}

		for (String metric : metrics) {
			if (metric.equals(Metric.IC_MCS.toString())) {
				scores.bmaAsymIC = sos.getSimilarityBestMatchAverage(i, j, Metric.IC_MCS, Direction.A_TO_B);
				//TODO: do reciprocal BMA
				scores.bmaSymIC = sos.getSimilarityBestMatchAverage(i, j, Metric.IC_MCS, Direction.AVERAGE);		
			} else if (metric.equals(Metric.JACCARD.toString())) {
				scores.bmaAsymJ = sos.getSimilarityBestMatchAverage(i, j, Metric.JACCARD, Direction.A_TO_B);
				//TODO: do reciprocal BMA
				scores.bmaSymJ = sos.getSimilarityBestMatchAverage(i, j, Metric.JACCARD, Direction.AVERAGE);
			} else if (metric.equals(Metric.GIC.toString())) {
				scores.simGIC = sos.getElementGraphInformationContentSimilarity(i,j);
			}
		}
		return scores;
	}

	protected void loadProperties(Opts opts) throws IOException {
		while (opts.hasOpts()) {
			if (opts.nextEq("-p|--properties")) {
				loadProperties(opts.nextOpt());
	      //need to check if metrics are valid          	        
			}
			else if (opts.nextEq("--set")) {
				simProperties.setProperty(opts.nextOpt(), opts.nextOpt());
			}
			else if (opts.nextEq("-o")) {
				String file = opts.nextOpt();
				FileOutputStream fos;
				try {
					fos = new FileOutputStream(file);
					this.resultOutStream = new PrintStream(new BufferedOutputStream(fos));
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else {
				break;
			}
		}
		//TODO:perhaps there should be a default simProperties file?
		if (simProperties == null) {
			initProperties();
		}
	}


	private void loadProperties(String fn) throws IOException {
		if (simProperties == null)
			this.initProperties();
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
		for (Object k : simProperties.keySet()){
			System.out.println("# "+k+" = "+simProperties.getProperty(k.toString()));
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
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
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
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
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
			g =	new OWLGraphWrapper(ont);
		}
		else {
			System.out.println("adding support ont "+ont);
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
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
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
			sos.createElementAttributeMapFromOntology();
			runOwlSim(opts);
		}
		finally {
			pproc.dispose();
		}
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
		}
		finally {
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
		}
		finally {
			pproc.dispose();
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
			}
			else if (opts.nextEq("-i")) {
				ec.attributeInformationContentCutoff = Double.parseDouble(opts.nextOpt());				
			}
			else 
				break;
		}
		sos.setEnrichmentConfig(ec);
		OWLClass rc1 = this.resolveClass(opts.nextOpt());
		OWLClass rc2 = this.resolveClass(opts.nextOpt());
		OWLClass pc = g.getDataFactory().getOWLThing();
		List<EnrichmentResult> results = sos.calculateAllByAllEnrichment(pc, rc1, rc2);
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
			}
			else if (opts.nextEq("-i")) {
				ec.attributeInformationContentCutoff = Double.parseDouble(opts.nextOpt());				
			}
			else 
				break;
		}
		sos.setEnrichmentConfig(ec);
		OWLClass rc1 = this.resolveClass(opts.nextOpt());
		OWLClass rc2 = this.resolveClass(opts.nextOpt());
		OWLClass pc = g.getDataFactory().getOWLThing();
		List<EnrichmentResult> results = sos.calculateAllByAllEnrichment(pc, rc1, rc2);
		for (EnrichmentResult result : results) {
			System.out.println(render(result, owlpp));
		}
	}

	private String render(EnrichmentResult r, OWLPrettyPrinter owlpp) {
		return owlpp.render(r.sampleSetClass) +"\t"+ owlpp.render(r.enrichedClass)
		+"\t"+ r.pValue +"\t"+ r.pValueCorrected;
	}

}
