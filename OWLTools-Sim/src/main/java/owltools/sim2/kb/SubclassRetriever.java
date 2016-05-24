package owltools.sim2.kb;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLOntology;

import owltools.util.OwlHelper;

public class SubclassRetriever {

	private Map<IRI, OWLClass> subClasses;
	
	public SubclassRetriever(OWLClass initialClass, OWLOntology sourceOntology) {
		subClasses = new HashMap<IRI, OWLClass>();

		Set<OWLClassExpression> subClss = OwlHelper.getSubClasses(initialClass, sourceOntology);
		for (OWLClassExpression subCls : subClss) {
			subClasses.put(subCls.asOWLClass().getIRI(), subCls.asOWLClass());
			retrieveSubClasses(subCls.asOWLClass(), sourceOntology);
		}
	}

	private void retrieveSubClasses(OWLClass subClsAsClass, OWLOntology sourceOntology) {
		Set<OWLClassExpression> subClss = OwlHelper.getSubClasses(subClsAsClass, sourceOntology);
		for (OWLClassExpression subCls : subClss) {
			if (!subClasses.containsKey(subCls.asOWLClass().getIRI())) {
				subClasses.put(subCls.asOWLClass().getIRI(), subCls.asOWLClass());
				retrieveSubClasses(subCls.asOWLClass(), sourceOntology);
			}
		}
	}

	protected Map<IRI, OWLClass> getSubClasses() {
		return subClasses;
	}
}
