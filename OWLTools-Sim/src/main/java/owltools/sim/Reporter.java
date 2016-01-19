package owltools.sim;

import java.io.PrintStream;
import java.util.Set;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.model.OWLObject;

import owltools.graph.OWLGraphWrapper;

public class Reporter {
	
	private PrintStream printStream = System.out;
	private OWLGraphWrapper graph;
	private String prefix = "#";
	
	public Reporter(PrintStream printStream, OWLGraphWrapper graph) {
		super();
		this.printStream = printStream;
		this.graph = graph;
	}

	public Reporter(OWLGraphWrapper graph) {
		super();
		this.graph = graph;
	}



	public void report(Object... vals) {
		printStream.print(prefix);
		int n=0;
		for (Object val : vals) {
			if (n > 0)
				printDel();
			printVal(val);
			n++;
		}
		printStream.println();
	}

	private void printDel() {
		printStream.print("\t");
	}

	private void printVal(Object val) {
		if (val instanceof Set<?> && 
				(((Set)val).size() == 0 || ((Set) val).iterator().next() instanceof OWLObject)) {
			int n = 0;
			for (Object obj : (Set<?>)val) {
				if (obj instanceof OWLObject) {
					if (n>0) {
						printStream.print(" ^ ");
					}
					n++;
					printOWLObject((OWLObject)obj, " ");
				}
				else {
					System.err.println("Warning - expected OWLObject, got:"+obj);
				}
			}
		}
		else if (val instanceof OWLObject) {
			printOWLObject((OWLObject)val, "\t");

		}
		else if (val instanceof Similarity) {
			String c = val.getClass().getName();
			printStream.print(c);
		}
		else {
			printStream.print(val);
		}
		
	}

	private void printOWLObject(OWLObject obj, String del) {
		String id;
		if (obj instanceof OWLNamedObject) {
			IRI iri = ((OWLNamedObject)obj).getIRI();
			if (iri.getFragment() != null) {
				id = iri.getFragment();
			}
			else {
				id = iri.toString();
				id = id.replaceAll(".*/", "");
			}
		}
		else {
			/*
			if (obj instanceof OWLObjectIntersectionOf) {
				s.print("and");
				for (OWLClassExpression sx : ((OWLObjectIntersectionOf)x).getOperands()) {
					printX(s, sx, depth+1);
				}
			}
			else if (x instanceof OWLObjectUnionOf) {
				s.print("or");
				for (OWLClassExpression sx : ((OWLObjectUnionOf)x).getOperands()) {
					printX(s, sx, depth+1);
				}
			}
			else if (x instanceof OWLQuantifiedRestriction) {
				OWLQuantifiedRestriction qr = (OWLQuantifiedRestriction)x;
				s.print(qr.getProperty().toString()+" "+qr.getClassExpressionType());
				printX(s, qr.getFiller(), depth+1);
			}
			*/

			// todo - show class expressions
			id = obj.toString();
		}
		printStream.print(id);
		String label = graph.getLabel(obj);
		if (label == null) {
			label = "";
		}
		printStream.print(del);
		printStream.print("\'"+label+"\'");		
	}


}
