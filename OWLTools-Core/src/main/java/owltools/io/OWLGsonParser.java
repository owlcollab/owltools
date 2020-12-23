package owltools.io;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNaryBooleanClassExpression;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLQuantifiedObjectRestriction;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLQuantifiedProperty;

import com.google.gson.Gson;

/**
 * Counterpart to OWLGsonRenderer
 * 
 * UNTESTED
 * 
 * @author cjm
 *
 */
public class OWLGsonParser {
	PrintWriter writer;

	Gson gson = new Gson();
	OWLOntologyManager manager;
	OWLDataFactory factory;

	public OWLOntology convertOntology(Map<Object,Object> m) throws OWLOntologyCreationException {
		Set<OWLAxiom> axioms = null;
		IRI ontologyIRI = null;
		List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
		List<IRI> importsIRIs = new ArrayList<IRI>();
		for (Object k : m.keySet()) {
			if (k.equals("axioms")) {
				axioms = convertAxioms((Object[]) m.get(k));			
			}
			else if (k.equals("iri")) {
				ontologyIRI = IRI.create((String) m.get(k));
			}
			else if (k.equals("annotations")) {
				// TODO
			}
			else if (k.equals("imports")) {
				IRI importIRI = IRI.create((String) m.get(k));
				importsIRIs.add(importIRI);
				
			}
		}
		OWLOntology ont = manager.createOntology(ontologyIRI);
		for (IRI importsIRI : importsIRIs) {
			AddImport ai = new AddImport(ont, manager.getOWLDataFactory().getOWLImportsDeclaration(importsIRI));
			changes.add(ai);
		}
		manager.applyChanges(changes);
		return ont;
	}
	public Set<OWLAxiom> convertAxioms(Object[] args) {
		Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
		for (Object arg : args) {
			axioms.add(convertAxiom((Map)arg));
		}
		return axioms;
	}
	public OWLAxiom convertAxiom(Map arg) {
		String type = (String) arg.get("type");
		Object[] args = (Object[]) arg.get("args");
		if (arg.containsKey("annotations")) {
			// TODO
		}
		OWLAxiom ax = null;
//		if (type eq 'AnnotationAssertion')
//			ax = factory.getOWLAnnotationAssertionAxiom(convert, annotation)
		return ax;
	}
	public Object convert() {
		return null;
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
				OWLQuantifiedObjectRestriction r = (OWLQuantifiedObjectRestriction)obj;
				arr = new Object[] {
						 convert(r.getProperty()),
						 convert(r.getFiller())
				};
				// TODO: QCRs
			}
			else if (obj instanceof OWLNaryBooleanClassExpression) {
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
		Optional<IRI> ontologyIRI = ont.getOntologyID().getOntologyIRI();
		if(ontologyIRI.isPresent()) {
			m.put("iri", convert(ontologyIRI.get()));
		}
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
		else if (a instanceof OWLEquivalentClassesAxiom) {
			arr = convertSet(((OWLEquivalentClassesAxiom)a).getClassExpressions());
		}
		else if (a instanceof OWLDisjointClassesAxiom) {
			arr = convertSet(((OWLDisjointClassesAxiom)a).getClassExpressions());
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
		/*
		for (Object obj : objs) {
			if (obj instanceof OWLAxiom)
				arr[i] = convert((OWLAxiom) obj);
			else if (obj instanceof OWLImportsDeclaration)
				arr[i] = convert((OWLImportsDeclaration) obj);
			else 
				arr[i] = convert((OWLObject) obj);
			i++;
		}
		*/
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
		Object obj = GeneralObjectDeserializer.fromJson("{\"a\":[1,2]}");
		if (obj instanceof Map) {
			for (Object k : ((Map)obj).keySet()) {
				Object v = ((Map)obj).get(k);
				System.out.println(k+"="+v);
				if (v instanceof Object[]) {
					Object[] arr = ((Object[])v);
					System.out.println("A="+arr[0]);
				}
			}
		}
	}


}
