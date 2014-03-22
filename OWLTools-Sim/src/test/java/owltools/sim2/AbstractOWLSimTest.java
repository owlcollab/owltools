package owltools.sim2;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Set;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import owltools.OWLToolsTestBasics;
import owltools.graph.OWLGraphWrapper;
import owltools.io.OWLPrettyPrinter;
import owltools.io.TableToAxiomConverter;
import owltools.sim2.OwlSim.ScoreAttributeSetPair;
import owltools.sim2.io.FormattedRenderer;
import owltools.sim2.io.SimResultRenderer;
import owltools.sim2.preprocessor.SimPreProcessor;
import owltools.sim2.scores.ElementPairScores;

/**
 * Shared convenience methods for sim2 tests
 * 
 * @author cjm
 *
 */
public class AbstractOWLSimTest extends OWLToolsTestBasics {

	private Logger LOG = Logger.getLogger(AbstractOWLSimTest.class);
	protected OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
	protected OWLDataFactory df = manager.getOWLDataFactory();
	protected OWLOntology sourceOntol;
	
	@Deprecated
	protected SimPreProcessor pproc;
	protected OWLPrettyPrinter owlpp;
	protected OWLGraphWrapper g;
	protected OwlSim owlsim;
	protected OwlSimFactory owlSimFactory = new FastOwlSimFactory(); // default is now FOS
	protected SimResultRenderer renderer ;

	
	/**
	 * Parses a 2-column file into owl class assertions.
	 *  - Col1 : instance ID (e.g. gene)
	 *  - Col2 : type ID (e.g. phenotype) 
	 * 
	 * @param file
	 * @param g
	 * @throws IOException
	 */
	protected void parseAssociations(File file, OWLGraphWrapper g) throws IOException {
		TableToAxiomConverter ttac = new TableToAxiomConverter(g);
		ttac.config.axiomType = AxiomType.CLASS_ASSERTION;
		ttac.config.isSwitchSubjectObject = true;
		ttac.parse(file);			
	}

	@Deprecated
	protected void showSimOld(OWLNamedIndividual i, OWLNamedIndividual j) throws UnknownOWLClassException {

		if (i==j) return;
		double s = owlsim.getElementJaccardSimilarity(i, j);
		if (s > 0.1) {
			LOG.info("SimJ( "+i+" , "+j+" ) = "+s);

			ScoreAttributeSetPair maxic = owlsim.getSimilarityMaxIC(i, j);
			LOG.info("MaxIC( "+i+" , "+j+" ) = "+maxic.score+" "+show(maxic.attributeClassSet));

			ScoreAttributeSetPair bma = owlsim.getSimilarityBestMatchAverageAsym(i, j);
			LOG.info("BMAasym( "+i+" , "+j+" ) = "+bma.score+" "+show(bma.attributeClassSet));
		}

	}

	protected void showSim(OWLNamedIndividual i, OWLNamedIndividual j) throws UnknownOWLClassException {
		if (i==j) return;
		double s = owlsim.getElementJaccardSimilarity(i, j);
		if (s > 0.1) {
			LOG.info("SimJ( "+i+" , "+j+" ) = "+s);

			//ScoreAttributeSetPair maxic = owlsim.getSimilarityMaxIC(i, j);
			//LOG.info("MaxIC( "+i+" , "+j+" ) = "+maxic.score+" "+show(maxic.attributeClassSet));

			//ScoreAttributeSetPair bma = owlsim.getSimilarityBestMatchAverageAsym(i, j);
			//LOG.info("BMAasym( "+i+" , "+j+" ) = "+bma.score+" "+show(bma.attributeClassSet));
			ElementPairScores scores;
			try {
				scores = owlsim.getGroupwiseSimilarity(i,j);
				renderer.printPairScores(scores);
			} catch (CutoffException e) {
				e.printMessage();
			}
		}

	}
	
	// rendering

	protected String show(Set<OWLClass> attributeClassSet) {
		StringBuffer sb = new StringBuffer();
		for (OWLClassExpression c : attributeClassSet) {
			sb.append(owlpp.render(c) + " ; ");
		}
		return sb.toString();
	}

	protected OWLClass getTestClass(String iri) {
		return df.getOWLClass(IRI.create("http://x.org#"+iri));
	}
	protected OWLClass getOBOClass(String id) {
		return df.getOWLClass(IRI.create("http://purl.obolibrary.org/obo/"+id.replaceAll(":", "_")));
	}
	

	protected String render(EnrichmentResult r, OWLPrettyPrinter pp) {
		return pp.render(r.sampleSetClass) +" "+ pp.render(r.enrichedClass)
		+" "+ r.pValue +" "+ r.pValueCorrected;
	}

	protected OWLClass get(String label) {
		return (OWLClass)g.getOWLObjectByLabel(label);
	}


	protected void createOwlSim() {
		owlsim = owlSimFactory.createOwlSim(sourceOntol);
		
	}
	
	protected void setOutput(String f) throws FileNotFoundException {
		FileOutputStream fos = new FileOutputStream(f);
		PrintStream resultOutStream = new PrintStream(new BufferedOutputStream(fos));

		renderer = new FormattedRenderer(g, resultOutStream);

	}


}
