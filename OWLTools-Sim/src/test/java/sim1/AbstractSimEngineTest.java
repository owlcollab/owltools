package sim1;

import java.io.IOException;
import java.util.Set;

import org.apache.log4j.Logger;
import org.obolibrary.oboformat.parser.OBOFormatParserException;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import owltools.OWLToolsTestBasics;
import owltools.graph.OWLGraphWrapper;
import owltools.io.OWLPrettyPrinter;
import owltools.io.ParserWrapper;
import owltools.sim.SimEngine;
import owltools.sim.Similarity;

public abstract class AbstractSimEngineTest extends OWLToolsTestBasics {

	private static Logger LOG = Logger.getLogger(AbstractSimEngineTest.class);

	protected void run(OWLGraphWrapper  wrapper, Similarity sim, OWLObject a, OWLObject b) throws Exception{
		OWLPrettyPrinter pp = new OWLPrettyPrinter(wrapper);
		SimEngine se = new SimEngine(wrapper);
		LOG.debug("Comparing "+pp.render(a)+" -vs- "+pp.render(b));
		/*
		for (OWLObject x : wrapper.getDescendantsReflexive(a)) {
			System.out.println("d="+x);
		}
		*/
		
		OWLClassExpression lcs = se.getLeastCommonSubsumerSimpleClassExpression(a, b);
		if (lcs != null) {
			LOG.debug(" LCS: "+pp.render(lcs));
		}
		
		se.calculateSimilarity(sim, a, b);

		LOG.debug(sim);
		for (OWLAxiom ax : sim.translateResultsToOWLAxioms()) {
			LOG.debug("  Ax: "+pp.render(ax));
		}
		
	}
	
	protected void runAll(OWLGraphWrapper  wrapper, OWLObject a, OWLObject b) throws Exception{
		OWLPrettyPrinter pp = new OWLPrettyPrinter(wrapper);
		SimEngine se = new SimEngine(wrapper);
		LOG.debug("Comparing "+pp.render(a)+" -vs- "+pp.render(b));
		/*
		for (OWLObject x : wrapper.getDescendantsReflexive(a)) {
			System.out.println("d="+x);
		}
		*/
		
		OWLClassExpression lcs = se.getLeastCommonSubsumerSimpleClassExpression(a, b);
		if (lcs != null) {
			LOG.debug(" LCS: "+pp.render(lcs));
		}
		
		Set<Similarity> r = se.calculateAllSimilarity(a, b);
		for (Similarity sim : r) {

			LOG.debug(sim);
			for (OWLAxiom ax : sim.translateResultsToOWLAxioms()) {
				LOG.debug("  Ax: "+pp.render(ax));
			}
		}
		
	}

	
	protected OWLGraphWrapper getOntologyWrapper(String file) throws OWLOntologyCreationException, IOException, OBOFormatParserException {
		ParserWrapper pw = new ParserWrapper();
		return pw.parseToOWLGraph(getResourceIRIString(file));
	}
	
	protected OWLGraphWrapper getOntologyWrapperFromURL(String url) throws OWLOntologyCreationException, IOException, OBOFormatParserException {
		ParserWrapper pw = new ParserWrapper();
		return pw.parseToOWLGraph(url);
	}
	
	
}
