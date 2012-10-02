package owltools.io;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNaryBooleanClassExpression;
import org.semanticweb.owlapi.model.OWLNaryClassAxiom;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectCardinalityRestriction;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLQuantifiedObjectRestriction;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLQuantifiedProperty;

import com.google.gson.Gson;

public class OWLGsonRenderer {
	PrintWriter writer;

	Gson gson = new Gson();

	public OWLGsonRenderer(PrintWriter writer) {
		super();
		this.writer = writer;		
	}
	
	public Object convert(OWLImportsDeclaration obj) {
		return convert(((OWLImportsDeclaration)obj).getIRI());
	}

	public Object convert(OWLObject obj) {
		if (obj instanceof IRI) {
			return obj.toString();
		}
		else if (obj instanceof OWLEntity) {
			return convert(((OWLEntity)obj).getIRI());
		}
		else if (obj instanceof OWLClassExpression) {
			// {type: <Class|SomeValuesFrom|...>,  args: [...]
			Map<String,Object> m = new HashMap<String,Object>();
			m.put("type", ((OWLClassExpression) obj).getClassExpressionType().toString());
			Object[] arr;
			if (obj instanceof OWLQuantifiedObjectRestriction) {
				// all/some
				OWLQuantifiedObjectRestriction r = (OWLQuantifiedObjectRestriction)obj;
				arr = new Object[] {
						 convert(r.getProperty()),
						 convert(r.getFiller())
				};
			}
			else if (obj instanceof OWLObjectCardinalityRestriction) {
				// QCR
				OWLObjectCardinalityRestriction r = (OWLObjectCardinalityRestriction)obj;
				// note: properties are 2nd argument in QCRs
				arr = new Object[] {
						r.getCardinality(),
						 convert(r.getProperty()),
						 convert(r.getFiller())
						 
				};
				// TODO: QCRs
			}
			else if (obj instanceof OWLNaryBooleanClassExpression) {
				// intersection & union
				arr = convertSet( ((OWLNaryBooleanClassExpression)obj).getOperands());
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
		m.put("imports", convertSet(ont.getImportsDeclarations()));
		return m;
	}
	
	/**
	 * 
	 * @param a
	 * @return map of form {type: <AxiomType>, args: [....]}
	 */
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
		else if (a instanceof OWLClassAssertionAxiom) {
			OWLClassAssertionAxiom ax = (OWLClassAssertionAxiom)a;
			arr = new Object[] {
						convert(ax.getClassExpression()),
								convert(ax.getIndividual())
			};	
		}
		else if (a instanceof OWLNaryClassAxiom) {
			// disjoint & equivClasses
			arr = convertSet(((OWLNaryClassAxiom)a).getClassExpressions());
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
			else if (obj instanceof OWLImportsDeclaration)
				arr[i] = convert((OWLImportsDeclaration) obj);
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
		flush();
	}
	public void render(Set a) {
		writer.println(gson.toJson(convertSet(a)));
	}

	public void flush() {
		writer.flush();
		
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
