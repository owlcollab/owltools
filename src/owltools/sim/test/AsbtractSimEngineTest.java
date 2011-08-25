package owltools.sim.test;

import java.io.IOException;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;
import owltools.sim.SimEngine;
import owltools.sim.Similarity;
import owltools.test.OWLToolsTestBasics;

public abstract class AsbtractSimEngineTest extends OWLToolsTestBasics {

	protected void run(OWLGraphWrapper  wrapper, Similarity sa, OWLObject a, OWLObject b) throws Exception{
		SimEngine se = new SimEngine(wrapper);
		System.out.println("Comparing "+a+" -vs- "+b);
		for (OWLObject x : wrapper.getDescendantsReflexive(a)) {
			System.out.println("d="+x);
		}
		
		Set<Similarity> r = se.calculateAllSimilarity(a, b);
		for (Similarity x : r) {
			System.out.println(x);
		}
		
	}
	
	protected OWLGraphWrapper getOntologyWrapper(String file) throws OWLOntologyCreationException, IOException {
		ParserWrapper pw = new ParserWrapper();
		return pw.parseToOWLGraph(getResourceIRIString(file));
	}
	
	protected OWLGraphWrapper getOntologyWrapperFromURL(String url) throws OWLOntologyCreationException, IOException {
		ParserWrapper pw = new ParserWrapper();
		return pw.parseToOWLGraph(url);
	}
	
	
}
