package owltools.sim.test;

import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLObject;

import owltools.graph.OWLGraphWrapper;
import owltools.sim.MaximumInformationContentSimilarity;
import owltools.sim.Similarity;

public class RetainAllTest extends AsbtractSimEngineTest {

	public static void testRetainAll() throws Exception{
		Set<String> s1 = new HashSet<String>();
		Set<String> s2 = new HashSet<String>();
		s1.add("a");
		s1.add("b");
		s1.add("c");
		s2.add("b");
		s2.add("c");
		s2.add("d");
		s1.retainAll(s2);
		System.out.println(s1.size());
		System.out.println(s2.size());
		
	}	
	
	
}
