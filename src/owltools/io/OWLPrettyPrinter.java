package owltools.io;


import org.semanticweb.owlapi.io.OWLObjectRenderer;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.util.ShortFormProvider;
import org.semanticweb.owlapi.util.SimpleRenderer;

import owltools.graph.OWLGraphWrapper;

public class OWLPrettyPrinter {
	OWLGraphWrapper graph;
	
	OWLObjectRenderer renderer;
	ShortFormProvider shortFormProvider;

	public OWLPrettyPrinter(OWLGraphWrapper graph) {
		super();
		this.graph = graph;
		shortFormProvider = new LabelProvider(graph);
		renderer = new SimpleRenderer();
		renderer.setShortFormProvider(shortFormProvider);
		
	}
	
	public String render(OWLObject obj) {
		return renderer.render(obj);
	}
	
	public void print(OWLObject obj) {
		print(render(obj));
	}
	
	public void print(String s) {
		System.out.println(s);
	}
	
	public void hideIds() {
		((LabelProvider)shortFormProvider).hideIds = true;
	}
	
	public class LabelProvider implements ShortFormProvider  {
		
		OWLGraphWrapper graph;
		boolean hideIds = false;
		

		public LabelProvider(OWLGraphWrapper graph) {
			super();
			this.graph = graph;
		}

		public String getShortForm(OWLEntity entity) {
			if (hideIds) {
				return graph.getLabel(entity);
			}
			else {
				return graph.getIdentifier(entity) + " \""+ graph.getLabel(entity) + "\"";				
			}
		}

		public void dispose() {
			
		}
		
	}
}
