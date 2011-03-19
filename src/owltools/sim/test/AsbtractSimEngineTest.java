package owltools.sim.test;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;
import owltools.sim.MaximumInformationContentSimilarity;
import owltools.sim.SimEngine;
import owltools.sim.Similarity;

import junit.framework.TestCase;

public class AsbtractSimEngineTest extends TestCase {

	public static void run(OWLGraphWrapper  wrapper, Similarity sa, OWLObject a, OWLObject b) throws Exception{
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

	
	protected static OWLGraphWrapper getOntologyWrapper(String file) throws OWLOntologyCreationException, IOException {
		ParserWrapper pw = new ParserWrapper();
		return pw.parseToOWLGraph(file);
	}
	
	
	
}
