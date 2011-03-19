package owltools.sim.test;

import java.util.HashSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.semanticweb.owlapi.model.OWLObject;

import owltools.graph.OWLGraphWrapper;
import owltools.sim.MaximumInformationContentSimilarity;
import owltools.sim.Similarity;

public class TreeMapTest extends AsbtractSimEngineTest {

	public static void testRetainAll() throws Exception{
		
		SortedMap<Integer,String> m = new TreeMap<Integer,String>();

		m.put(1, "one");
		m.put(8, "eight");
		m.put(4, "four");
		m.put(3, "three");
		m.put(11, "eleven");
		m.put(100, "one hundred");
		m.put(7, "seven");
		
		System.out.println("KEYS:");
		for (Integer k : m.keySet()) {
			System.out.println(k);
		}
		System.out.println("VALUES:");
		for (String v : m.values()) {
			System.out.println(v);
		}
		
	}	
	
	
}
