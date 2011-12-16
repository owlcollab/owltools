package owltools.io;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.obolibrary.oboformat.model.FrameMergeException;
import org.semanticweb.owlapi.io.OWLObjectRenderer;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClassAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.OWLQuantifiedObjectRestriction;
import org.semanticweb.owlapi.model.OWLRestriction;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.util.ShortFormProvider;
import org.semanticweb.owlapi.util.SimpleRenderer;

import com.google.gson.Gson;

import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;
import owltools.graph.OWLQuantifiedProperty;
import owltools.sim.SimEngine.SimilarityAlgorithmException;

public class OWLGsonRenderer {
	PrintWriter writer;

	Gson gson = new Gson();

	public OWLGsonRenderer(PrintWriter writer) {
		super();
		this.writer = writer;		
	}

	public OWLGsonRenderer() {
		// TODO Auto-generated constructor stub
	}



	public Object convert(OWLObject obj) {
		if (obj instanceof OWLEntity) {
			return convert(((OWLEntity)obj).getIRI());
		}
		else if (obj instanceof OWLClassExpression) {
			Map<String,Object> m = new HashMap<String,Object>();
			m.put("type", ((OWLClassExpression) obj).getClassExpressionType().toString());
			Object[] arr;
			if (obj instanceof OWLQuantifiedObjectRestriction) {
				OWLQuantifiedObjectRestriction r = (OWLQuantifiedObjectRestriction)obj;
				arr = new Object[] {
						 convert(r.getProperty()),
						 convert(r.getFiller())
				};
			}
			else {
				arr = new Object[0];
				// TODO
			}
			m.put("args", arr);
			return m;
		}
		else if (obj instanceof OWLLiteral) {
			return ((OWLLiteral)obj).getLiteral();
		}
		else {
			return obj.toString(); // TODO
		}
	}

	public Object convert(OWLOntology ont) {
		Map<String,Object> m = new HashMap<String,Object>();
		m.put("iri", convert(ont.getOntologyID().getOntologyIRI()));
		m.put("annotations", convertSet(ont.getAnnotations()));
		m.put("axioms", convertSet(ont.getAxioms()));
		return m;
	}
	
	public Object convert(OWLAxiom a) {
		Map<String,Object> m = new HashMap<String,Object>();
		m.put("type", a.getAxiomType().toString());
		Object[] arr;
		if (a instanceof OWLSubClassOfAxiom) {
			OWLSubClassOfAxiom ax = (OWLSubClassOfAxiom)a;
			arr = new Object[] {
					convert(ax.getSubClass()),
					convert(ax.getSuperClass())
			};
		}
		else if (a instanceof OWLEquivalentClassesAxiom) {
			arr = convertSet(((OWLEquivalentClassesAxiom)a).getClassExpressions());
		}
		else if (a instanceof OWLAnnotationAssertionAxiom) {
			OWLAnnotationAssertionAxiom ax = (OWLAnnotationAssertionAxiom)a;
			arr = new Object[] {
					convert(ax.getProperty()),
					convert(ax.getSubject()),
					convert(ax.getValue())
			};
		}
		else if (a instanceof OWLDeclarationAxiom) {
			arr = new Object[]{
					convert(((OWLDeclarationAxiom)a).getEntity())
			};
		}
		else {
			arr = new Object[0];
		}
		m.put("args", arr);
		return m;
	}


	private Object[] convertSet(Set objs) {
		Object[] arr = new Object[objs.size()];
		int i=0;
		for (Object obj : objs) {
			if (obj instanceof OWLAxiom)
				arr[i] = convert((OWLAxiom) obj);
			else 
				arr[i] = convert((OWLObject) obj);
			i++;
		}
		return arr;
	}

	public void render(OWLAxiom a) {
		writer.println(gson.toJson(convert(a)));
	}
	public void render(OWLGraphEdge a) {
		writer.println(gson.toJson(convert(a)));
	}
	public void render(OWLOntology a) {
		writer.println(gson.toJson(convert(a)));
	}
	public void render(Set a) {
		writer.println(gson.toJson(convertSet(a)));
	}


	public Object convert(OWLGraphEdge edge) {
		Map<String,Object> m = new HashMap<String,Object>();
		m.put("distance", edge.getDistance());
		m.put("source", convert(edge.getSource()));
		m.put("target", convert(edge.getTarget()));
		Object[] arr = new Object[edge.getQuantifiedPropertyList().size()];
		int i=0;
		for (OWLQuantifiedProperty qp : edge.getQuantifiedPropertyList()) {
			Map<String,Object> qpm = new HashMap<String,Object>();
			if (qp.getProperty() != null)
				qpm.put("property", convert(qp.getProperty()));
			qpm.put("quantifier", qp.getQuantifier());
			arr[i] = qpm;
			i++;
		}
		m.put("quantifiedPropertyList", arr);
		return m;
	}

	public static void main(String[] args) throws Exception {
		Gson gson = new Gson();
		System.out.println(gson.toJson(3));
	}


}
