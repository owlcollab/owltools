package owltools.io;

import java.io.PrintWriter;
import java.util.Set;

import org.semanticweb.owlapi.io.OWLObjectRenderer;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClassAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLQuantifiedObjectRestriction;
import org.semanticweb.owlapi.model.OWLRestriction;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.util.ShortFormProvider;
import org.semanticweb.owlapi.util.SimpleRenderer;

import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;
import owltools.graph.OWLQuantifiedProperty;

@Deprecated
public class JSONPrinter {
	PrintWriter writer;

	public JSONPrinter(PrintWriter writer) {
		super();
		this.writer = writer;		
	}
	
	private void opMap() {
		writer.print("{");
	}
	private void clMap() {
		writer.print("}");
	}
	private void tv(String t, String v) {
		qt(t);
		writer.print(":");
		qt(v);
	}
	private void qt(String v) {
		// TODO - escape
		writer.print("\""+v+"\"");
	}
	private void jsonArr(Object... args) {
		writer.print("[");
		int i = 0;
		for (Object arg : args) {
			if (i>0)
				writer.print(", ");
			json(arg);
			i++;
		}
		writer.print("]");
	}
	
	private void json(Object obj) {
		if (obj instanceof OWLAxiom) {
			render((OWLAxiom)obj);
		}
		else if (obj instanceof OWLObject) {
			render((OWLObject)obj);
		}
		else {
			render(obj.toString());
		}
	}
	
	public void render(String s) {
		qt(s);
	}

	public void render(Set objs) {
		jsonArr(objs.toArray());
	}
	
	public void render(OWLObject obj) {
		if (obj instanceof OWLEntity) {
			render(((OWLEntity)obj).getIRI());
		}
		else if (obj instanceof OWLClassExpression) {
			opMap();
			tv("type", ((OWLClassExpression)obj).getClassExpressionType().toString());
			writer.print(", \"args\":");
			if (obj instanceof OWLQuantifiedObjectRestriction) {
				OWLQuantifiedObjectRestriction r = (OWLQuantifiedObjectRestriction)obj;
				jsonArr(r.getProperty(), r.getFiller());
			}
			else {
				// TODO
			}
			clMap();
		}
		else {
			render(obj.toString());
		}
	}

	public void render(OWLAxiom a) {
		opMap();
		tv("type", a.getAxiomType().toString());
		writer.print(", \"args\":");
		if (a instanceof OWLSubClassOfAxiom) {
			OWLSubClassOfAxiom ax = (OWLSubClassOfAxiom)a;
			jsonArr(ax.getSubClass(), ax.getSuperClass());
		}
		clMap();
	}
	
	public void render(OWLGraphEdge edge) {
		StringBuffer sb = new StringBuffer();
		//sb.append(render(edge.getSource())+ " [");
		int n=0;
		for (OWLQuantifiedProperty qp : edge.getQuantifiedPropertyList()) {
			if (n>0)
				sb.append(", ");
			if (qp.isInferred()) {
				sb.append("[INF]");
			}
			//sb.append(render(qp));
			n++;
		}
		//sb.append("]/"+edge.getDistance()+" "+render(edge.getTarget()));
	}


}
