package owltools.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;

import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;
import owltools.graph.OWLQuantifiedProperty;
import owltools.graph.OWLQuantifiedProperty.Quantifier;

public class CompactGraphClosureReader extends AbstractClosureReader {

	public CompactGraphClosureReader(OWLGraphWrapper g) {
		super(g);
	}

	@Override
	public void read() throws IOException {
		InputStreamReader isr = new InputStreamReader(stream);
		BufferedReader reader = new BufferedReader(isr);
		OWLOntology ont = graph.getSourceOntology();
		String row;
		if (graph.inferredEdgeBySource == null) {
			graph.inferredEdgeBySource = new HashMap<OWLObject,Set<OWLGraphEdge>>();
		}
		while (true) {
			row = reader.readLine();
			if (row == null) {
				break;
			}
			String[] vals = row.split("\t");
			int len = vals.length;
			OWLObject src = getObject(vals[0]);
			Collection<OWLGraphEdge> edges = new HashSet<OWLGraphEdge>();
			for (int i=1; i<len; i++) {
				OWLGraphEdge e = parseEdge(ont, src, vals[i]);
				edges.add(e);
			}
			graph.inferredEdgeBySource.put(src, new HashSet<OWLGraphEdge>(edges));
		}
	}

	private OWLGraphEdge parseEdge(OWLOntology ont, OWLObject src, String edgeStr) {
		OWLGraphEdge e;
		String[] vals = edgeStr.split(",");
		int len = vals.length;
		OWLObject tgt = getObject(vals[len-2]);	
		int dist = Integer.parseInt(vals[len-1]);
		OWLQuantifiedProperty[] qpa = new OWLQuantifiedProperty[len-1];
		for (int i=0; i<len-2; i++) {
			OWLQuantifiedProperty qp = parseQP(vals[i]);
			qpa[i] = qp;
		}
		List<OWLQuantifiedProperty> qpl = Arrays.asList(qpa); // fixed length, cannot be appended to

		e = new OWLGraphEdge(src, tgt, qpl, ont);
		e.setDistance(dist);
		return e;
	}

	private OWLQuantifiedProperty parseQP(String qpString) {
		OWLQuantifiedProperty qp = new OWLQuantifiedProperty();
		String[] vals = qpString.split(" ");
		if (vals.length == 1) {
			if (qpString.equals("SUBCLASS_OF"))
				qp.setQuantifier(Quantifier.SUBCLASS_OF);
		}
		else {
			if (vals[1].equals("SOME"))
				qp.setQuantifier(Quantifier.SOME);
			else if (vals[1].equals("ONLY"))
				qp.setQuantifier(Quantifier.ONLY);
			
			qp.setProperty(getProperty(vals[0]));
		}
		return qp;
		
	}

	private OWLObjectProperty getProperty(String s) {
		return graph.getOWLObjectProperty(prefix(s));
	}

	private OWLObject getObject(String s) {
		return graph.getOWLObject(prefix(s));
	}

	private String prefix(String s) {
		if (s.contains(":"))
			return s;
		// TODO - make this more generic
		return "http://purl.obolibrary.org/obo/"+s;
	}

}
