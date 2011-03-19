package owltools.io;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

import owltools.graph.OWLGraphWrapper;

public class OWLPrettyPrinter {
	OWLGraphWrapper graph;

	public OWLPrettyPrinter(OWLGraphWrapper graph) {
		super();
		this.graph = graph;
	}
	
	public void print(OWLAxiom ax) {
		if (ax instanceof OWLSubClassOfAxiom) {
			print(((OWLSubClassOfAxiom)ax).getSubClass());
			print(" SubClassOf ");
			print(((OWLSubClassOfAxiom)ax).getSuperClass());
			print("\n");
		}
		else {
			print(ax.toString());
		}
		
	}

	private void print(OWLObject obj) {
		String label = graph.getLabel(obj);
		if (label == null) {
			print(obj);
		}
		else {
			print(label+" ("+obj+")");
		}
	}
	
	public void print(String s) {
		System.out.println(s);
	}
}
