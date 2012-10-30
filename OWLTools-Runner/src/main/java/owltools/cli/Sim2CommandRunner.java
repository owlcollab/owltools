package owltools.cli;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import owltools.cli.tools.CLIMethod;
import owltools.graph.OWLGraphWrapper;
import owltools.io.OWLPrettyPrinter;
import owltools.sim2.SimpleOwlSim;
import owltools.sim2.SimpleOwlSim.EnrichmentConfig;
import owltools.sim2.SimpleOwlSim.EnrichmentResult;
import owltools.sim2.SimpleOwlSim.ScoreAttributesPair;
import owltools.sim2.SimpleOwlSim.SimProperty;
import owltools.sim2.preprocessor.NullSimPreProcessor;
import owltools.sim2.preprocessor.PhenoSimHQEPreProcessor;
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
	SimpleOwlSim sos;
	SimPreProcessor pproc;
	Properties simProperties = null;
	int numberOfPairsFiltered = 0;

	private void initProperties() {
		simProperties = new Properties();

		simProperties.setProperty(SimProperty.minimumMaxIC.toString(), "4.0");
		simProperties.setProperty(SimProperty.minimumSimJ.toString(), "0.25");
	}

	public String getProperty(SimProperty p) {
		if (simProperties == null) {
			initProperties();
		}
		return simProperties.getProperty(p.toString());
	}

	private Double getPropertyAsDouble(SimProperty p) {
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
		opts.info("FILE", "merges similarity results into source OWL ontology. Use after --sim or --sim-all");
		g.mergeOntology(simOnt);
	}


	



	/**
	 * performs all by all individual comparison
	 * @param opts 
	 */
	public void runOwlSim(Opts opts) {
		sos.setSimProperties(simProperties);
		owlpp = new OWLPrettyPrinter(g);
		if (opts.nextEq("-q")) {
			runOwlSimOnQuery(opts, opts.nextOpt());
			return;
		}
		Set<OWLNamedIndividual> insts = pproc.getOutputOntology().getIndividualsInSignature();
		LOG.info("All by all for "+insts.size()+" individuals");
		for (OWLNamedIndividual i : insts) {
			for (OWLNamedIndividual j : insts) {
				// similarity is symmetrical
				if (isComparable(i,j)) {
					showSim(i,j);
				}
				else {
					LOG.info("skipping "+i+" + "+j);
				}
			}
		}
		LOG.info("FINISHED All by all for "+insts.size()+" individuals");
		LOG.info("Number of pairs filtered as they scored beneath threshold: "+this.numberOfPairsFiltered);
	}

	private void runOwlSimOnQuery(Opts opts, String q) {
		System.out.println("Query: "+q);
		OWLNamedIndividual qi = (OWLNamedIndividual) resolveEntity(q);
		System.out.println("Query Individual: "+qi);
		Set<OWLNamedIndividual> insts = pproc.getOutputOntology().getIndividualsInSignature();
		for (OWLNamedIndividual j : insts) {
			showSim(qi,j);
		}

	}

	private boolean isComparable(OWLNamedIndividual i, OWLNamedIndividual j) {
		String cmp = getProperty(SimProperty.compare);
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

	private void showSim(OWLNamedIndividual i, OWLNamedIndividual j) {
		ScoreAttributesPair maxic = sos.getSimilarityMaxIC(i, j);
		if ( maxic.score < getPropertyAsDouble(SimProperty.minimumMaxIC)) {
			numberOfPairsFiltered ++;
			return;
		}
		float s = sos.getElementJaccardSimilarity(i, j);
		if (s < getPropertyAsDouble(SimProperty.minimumSimJ)) {
			numberOfPairsFiltered ++;
			return;
		}
		ScoreAttributesPair bma = sos.getSimilarityBestMatchAverageAsym(i, j);

		System.out.println("SimJ\t"+renderPair(i,j)+"\t"+s);

		System.out.println("MaxIC\t"+renderPair(i,j)+"\t"+maxic.score+"\t"+show(maxic.attributeClassSet));

		System.out.println("BMAasym\t"+renderPair(i,j)+"\t"+bma.score+"\t"+show(bma.attributeClassSet));	
	}

	private String renderPair(OWLNamedIndividual i, OWLNamedIndividual j) {
		return i+"\t"+owlpp.render(i)+"\t"+j+"\t"+owlpp.render(j);
	}


	protected void loadProperties(Opts opts) throws IOException {
		while (opts.hasOpts()) {
			if (opts.nextEq("-p|--properties")) {
				loadProperties(opts.nextOpt());
			}
			else {
				break;
			}
		}
	}


	private void loadProperties(String fn) throws IOException {
		simProperties = new Properties();
		FileInputStream myInputStream = new FileInputStream(fn);
		simProperties.load(myInputStream);        
		String myPropValue = simProperties.getProperty("propKey");
		//-------------------------------------------------
		String key = "";
		String value = "";
		for (Map.Entry<Object, Object> propItem : simProperties.entrySet()) {
			key = (String) propItem.getKey();
			value = (String) propItem.getValue();
			System.out.println("# "+key + " = "+value);
		}
		//-------------------------------------------------
		myInputStream.close(); // better in finally block
		//-------------------------------------------------
	}

	@CLIMethod("--phenosim")
	public void phenoSim(Opts opts) throws OWLOntologyCreationException, OWLOntologyStorageException, IOException {
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
			sos.saveOntology("/tmp/phenosim-analysis-ontology.owl");
			runOwlSim(opts);
		}
		catch (Exception e) {
			e.printStackTrace();
			if (sos.getReasoner() != null) {
				sos.getReasoner().dispose();
			}
		}
		finally {
			LOG.info("clearing up...");
			if (sos.getReasoner() != null) {
				sos.getReasoner().dispose();
			}
		}

	}

	@CLIMethod("--sim-resume")
	public void simResume(Opts opts) throws OWLOntologyCreationException, OWLOntologyStorageException, IOException {
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
			if (sos.getReasoner() != null) {
				sos.getReasoner().dispose();
			}
		}
		finally {
			sos.getReasoner().dispose();
		}
	}


	@CLIMethod("--sim-basic")
	public void simBasic(Opts opts) throws OWLOntologyCreationException, OWLOntologyStorageException, IOException {
		// assumes that individuals in abox are of types named classes in tbox
		loadProperties(opts);
		try {
			pproc = new NullSimPreProcessor();
			pproc.setInputOntology(g.getSourceOntology());
			pproc.setOutputOntology(g.getSourceOntology());
			sos = new SimpleOwlSim(g.getSourceOntology());
			sos.setSimPreProcessor(pproc);
			sos.createElementAttributeMapFromOntology();
			runOwlSim(opts);
		}
		finally {
			sos.getReasoner().dispose();
		}
	}





	// NEW
	@CLIMethod("--enrichment-analysis")
	public void owlsimEnrichmentAnalysis(Opts opts) throws Exception {
		owlpp = new OWLPrettyPrinter(g);
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
			System.out.println(render(result));
		}
	}

	private String render(EnrichmentResult r) {
		return owlpp.render(r.sampleSetClass) +"\t"+ owlpp.render(r.enrichedClass)
		+"\t"+ r.pValue +"\t"+ r.pValueCorrected;
	}


	private String show(Set<OWLClassExpression> cset) {
		StringBuffer sb = new StringBuffer();
		for (OWLClassExpression c : cset) {
			sb.append(owlpp.render(c) + "\t");
		}
		return sb.toString();
	}


}
