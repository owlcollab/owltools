package owltools.io;


import java.util.Set;

import org.semanticweb.owlapi.io.OWLObjectRenderer;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClassAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.util.ShortFormProvider;
import org.semanticweb.owlapi.util.SimpleRenderer;

import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;
import owltools.graph.OWLQuantifiedProperty;

public class OWLPrettyPrinter {
	OWLGraphWrapper graph;

	OWLObjectRenderer renderer;
	ShortFormProvider shortFormProvider;

	public OWLPrettyPrinter(OWLGraphWrapper graph, OWLObjectRenderer r) {
		super();
		this.graph = graph;
		shortFormProvider = new LabelProvider(graph);
		renderer = r;
		renderer.setShortFormProvider(shortFormProvider);
	}


	public OWLPrettyPrinter(OWLGraphWrapper graph) {
		super();
		this.graph = graph;
		shortFormProvider = new LabelProvider(graph);
		renderer = new SimpleRenderer();
		renderer.setShortFormProvider(shortFormProvider);

	}

	public String render(OWLObject obj) {
		return obj == null ? "-" : renderer.render(obj);
	}

	public String renderId(String id) {
		return render(graph.getOWLObjectByIdentifier(id));
	}
	
	public String renderIRI(String iri) {
		return render(graph.getOWLObject(IRI.create(iri)));

	}



	public String render(OWLAxiom a) {
		return renderer.render(a);
	}

	public String render(OWLGraphEdge edge) {
		StringBuffer sb = new StringBuffer();
		sb.append(render(edge.getSource())+ " [");
		int n=0;
		for (OWLQuantifiedProperty qp : edge.getQuantifiedPropertyList()) {
			if (n>0)
				sb.append(", ");
			if (qp.isInferred()) {
				sb.append("[INF]");
			}
			sb.append(render(qp));
			n++;
		}
		sb.append("]/"+edge.getDistance()+" "+render(edge.getTarget()));
		return sb.toString();
	}


	private Object render(OWLQuantifiedProperty qp) {
		OWLObjectProperty p = qp.getProperty();
		return (p== null ? "" : render(qp.getProperty())+" ")+qp.getQuantifier();
	}



	public void print(OWLObject obj) {
		print(render(obj));
	}

	public void print(Set<OWLAxiom> axioms) {
		for (OWLAxiom a : axioms) {
			print(render(a));
		}
	}


	public void print(String s) {
		System.out.println(s);
	}

	public void hideIds() {
		((LabelProvider)shortFormProvider).hideIds = true;
	}
	public void noQuoteLabels() {
		((LabelProvider)shortFormProvider).quoteLabels = false;
	}

	public class LabelProvider implements ShortFormProvider  {

		OWLGraphWrapper graph;
		boolean hideIds = false;
		boolean quoteLabels = true;


		public LabelProvider(OWLGraphWrapper graph) {
			super();
			this.graph = graph;
		}

		
		public String getShortForm(OWLEntity entity) {
			String label = getLabelOrId(entity);
			if (hideIds) {
				return label;
			}
			return getTruncatedId(entity) + " "+ label;
		}

		public String getLabelOrId(OWLEntity entity) {
			String label = graph.getLabel(entity);
			if (label == null) {
				label = getTruncatedId(entity);
			}
			else {
				if (quoteLabels) {
					label = "'" + label +"'";
				}
			}
			return label;
		}
		
		public String getTruncatedId(OWLEntity entity) {
			String id = graph.getIdentifier(entity);
			id = id.replaceAll(".*#", "");
			id = id.replaceAll(".*/", "");
			return id;
		}

		public void dispose() {

		}

	}




}
