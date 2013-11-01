package owltools.sim2;

import java.io.File;
import java.io.IOException;
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
import owltools.sim2.preprocessor.SimPreProcessor;

/**
 * This is the main test class for PropertyViewOntologyBuilder
 * 
 * @author cjm
 *
 */
public class AbstractOWLSimTest extends OWLToolsTestBasics {

	private Logger LOG = Logger.getLogger(AbstractOWLSimTest.class);
	OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
	OWLDataFactory df = manager.getOWLDataFactory();
	OWLOntology sourceOntol;
	SimPreProcessor pproc;
	OWLPrettyPrinter owlpp;
	OWLGraphWrapper g;
	OwlSim sos;

	
	protected void parseAssociations(File file, OWLGraphWrapper g) throws IOException {
		TableToAxiomConverter ttac = new TableToAxiomConverter(g);
		ttac.config.axiomType = AxiomType.CLASS_ASSERTION;
		ttac.config.isSwitchSubjectObject = true;
		ttac.parse(file);			
	}



	protected void showSim(OWLNamedIndividual i, OWLNamedIndividual j) throws UnknownOWLClassException {

		if (i==j) return;
		double s = sos.getElementJaccardSimilarity(i, j);
		if (s > 0.1) {
			LOG.info("SimJ( "+i+" , "+j+" ) = "+s);

			ScoreAttributeSetPair maxic = sos.getSimilarityMaxIC(i, j);
			LOG.info("MaxIC( "+i+" , "+j+" ) = "+maxic.score+" "+show(maxic.attributeClassSet));

			ScoreAttributeSetPair bma = sos.getSimilarityBestMatchAverageAsym(i, j);
			LOG.info("BMAasym( "+i+" , "+j+" ) = "+bma.score+" "+show(bma.attributeClassSet));
		}

	}




	protected String show(Set<OWLClass> attributeClassSet) {
		StringBuffer sb = new StringBuffer();
		for (OWLClassExpression c : attributeClassSet) {
			sb.append(owlpp.render(c) + " ; ");
		}
		return sb.toString();
	}

	private OWLClass get(String iri) {
		return df.getOWLClass(IRI.create("http://x.org#"+iri));
	}





}
