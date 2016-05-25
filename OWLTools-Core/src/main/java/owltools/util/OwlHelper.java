package owltools.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLSubAnnotationPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.OWLSubObjectPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubPropertyAxiom;

public class OwlHelper {

	private OwlHelper() {
		// no instances
	}
	
	public static Set<OWLAnnotation> getAnnotations(OWLEntity e, OWLAnnotationProperty property, OWLOntology ont) {
		Set<OWLAnnotation> annotations;
		if (e != null && property != null && ont != null) {
			annotations = new HashSet<>();
			for (OWLAnnotationAssertionAxiom ax : ont.getAnnotationAssertionAxioms(e.getIRI())) {
				if (property.equals(ax.getProperty())) {
					annotations.add(ax.getAnnotation());
				}
			}
		}
		else {
			annotations = Collections.emptySet();
		}
		return annotations;
	}
	
	public static Set<OWLAnnotation> getAnnotations(OWLEntity e, OWLOntology ont) {
		Set<OWLAnnotation> annotations;
		if (e != null && ont != null) {
			Set<OWLAnnotationAssertionAxiom> axioms = ont.getAnnotationAssertionAxioms(e.getIRI());
			annotations = new HashSet<>(axioms.size());
			for(OWLAnnotationAssertionAxiom ax : axioms) {
				annotations.add(ax.getAnnotation());
			}
		}
		else {
			annotations = Collections.emptySet();
		}
		return annotations;
	}
	
	public static Set<OWLAnnotation> getAnnotations(OWLEntity e, Set<OWLOntology> ontolgies) {
		Set<OWLAnnotation> annotations;
		if (e != null && ontolgies != null && !ontolgies.isEmpty()) {
			annotations = new HashSet<>();
			for(OWLOntology ont : ontolgies) {
				annotations.addAll(getAnnotations(e, ont));
			}
		}
		else {
			annotations = Collections.emptySet();
		}
		return annotations;
	}
	
	public static Set<OWLClassExpression> getEquivalentClasses(OWLClass cls, OWLOntology ont) {
		Set<OWLClassExpression> expressions;
		if (cls != null && ont != null) {
			Set<OWLEquivalentClassesAxiom> axioms = ont.getEquivalentClassesAxioms(cls);
			expressions = new HashSet<>(axioms.size());
			for(OWLEquivalentClassesAxiom ax : axioms) {
				expressions.addAll(ax.getClassExpressions());
			}
			expressions.remove(cls); // set should not contain the query cls
		}
		else {
			expressions = Collections.emptySet();
		}
		return expressions;
	}
	
	public static Set<OWLClassExpression> getEquivalentClasses(OWLClass cls, Set<OWLOntology> ontologies) {
		Set<OWLClassExpression> expressions;
		if (cls != null && ontologies != null && ontologies.isEmpty() == false) {
			expressions = new HashSet<>();
			for(OWLOntology ont : ontologies) {
				expressions.addAll(getEquivalentClasses(cls, ont));
			}
		}
		else {
			expressions = Collections.emptySet();
		}
		return expressions;
	}
	
	public static Set<OWLClassExpression> getSuperClasses(OWLClass subCls, OWLOntology ont) {
		Set<OWLClassExpression> result;
		if (subCls != null && ont != null) {
			result = new HashSet<>();
			Set<OWLSubClassOfAxiom> axioms = ont.getSubClassAxiomsForSubClass(subCls);
			for (OWLSubClassOfAxiom axiom : axioms) {
				result.add(axiom.getSuperClass());
			}
		}
		else {
			result = Collections.emptySet();
		}
		return result;
	}
	
	public static Set<OWLClassExpression> getSuperClasses(OWLClass subCls, Set<OWLOntology> ontologies) {
		Set<OWLClassExpression> result;
		if (subCls != null && ontologies != null && ontologies.isEmpty() == false) {
			result = new HashSet<>();
			for(OWLOntology ont : ontologies) {
				result.addAll(getSuperClasses(subCls, ont));
			}
		}
		else {
			result = Collections.emptySet();
		}
		return result;
	}
	
	public static Set<OWLClassExpression> getSubClasses(OWLClass superCls, OWLOntology ont) {
		Set<OWLClassExpression> result;
		if (superCls != null && ont != null) {
			result = new HashSet<>();
			Set<OWLSubClassOfAxiom> axioms = ont.getSubClassAxiomsForSuperClass(superCls);
			for (OWLSubClassOfAxiom axiom : axioms) {
				result.add(axiom.getSubClass());
			}
		}
		else {
			result = Collections.emptySet();
		}
		return result;
	}
	
	public static Set<OWLClassExpression> getSubClasses(OWLClass superCls, Set<OWLOntology> ontologies) {
		Set<OWLClassExpression> result;
		if (superCls != null && ontologies != null && ontologies.isEmpty() == false) {
			result = new HashSet<>();
			for(OWLOntology ont : ontologies) {
				result.addAll(getSubClasses(superCls, ont));
			}
		}
		else {
			result = Collections.emptySet();
		}
		return result;
	}
	
	public static Set<OWLClassExpression> getTypes(OWLIndividual i, OWLOntology ont) {
		Set<OWLClassExpression> types;
		if (ont != null && i != null && i.isNamed()) {
			types = getTypes(i.asOWLNamedIndividual(), ont);
		}
		else {
			types = Collections.emptySet();
		}
		return types;
	}
	
	public static Set<OWLClassExpression> getTypes(OWLNamedIndividual i, OWLOntology ont) {
		Set<OWLClassExpression> types;
		if (i != null && ont != null) {
			types = new HashSet<>();
			for (OWLClassAssertionAxiom axiom : ont.getClassAssertionAxioms(i)) {
				types.add(axiom.getClassExpression());
			}
		}
		else {
			types = Collections.emptySet();
		}
		return types;
	}
	
	public static Set<OWLClassExpression> getTypes(OWLNamedIndividual i, Set<OWLOntology> ontologies) {
		Set<OWLClassExpression> types;
		if (i != null && ontologies != null && ontologies.isEmpty() == false) {
			types = new HashSet<>();
			for(OWLOntology ont : ontologies) {
				types.addAll(getTypes(i, ont));
			}
		}
		else {
			types = Collections.emptySet();
		}
		return types;
	}
	
	public static Map<OWLObjectPropertyExpression, Set<OWLIndividual>> getObjectPropertyValues(OWLIndividual i, OWLOntology ont) {
		Set<OWLObjectPropertyAssertionAxiom> axioms = ont.getObjectPropertyAssertionAxioms(i);
		Map<OWLObjectPropertyExpression, Set<OWLIndividual>> result = new HashMap<>();
		for(OWLObjectPropertyAssertionAxiom ax : axioms) {
			Set<OWLIndividual> inds = result.get(ax.getProperty());
			if (inds == null) {
				inds = new HashSet<>();
				result.put(ax.getProperty(), inds);
			}
			inds.add(ax.getObject());
		}
		return result;
	}
	
	public static boolean isTransitive(OWLObjectPropertyExpression property, OWLOntology ontology) {
		return !ontology.getTransitiveObjectPropertyAxioms(property).isEmpty();
	}

	public static boolean isTransitive(OWLObjectPropertyExpression property, Set<OWLOntology> ontologies) {
		for (OWLOntology ont : ontologies) {
			if (isTransitive(property, ont)) {
				return true;
			}
		}
		return false;
	}
	
	public static Set<OWLAnnotationProperty> getSubProperties(OWLAnnotationProperty superProp, OWLOntology ont) {
		return getSubProperties(superProp, Collections.singleton(ont));
	}
	
	public static Set<OWLAnnotationProperty> getSubProperties(OWLAnnotationProperty superProp, Set<OWLOntology> ontologies) {
		Set<OWLAnnotationProperty> result = new HashSet<OWLAnnotationProperty>();
		for (OWLOntology ont : ontologies) {
			for (OWLSubAnnotationPropertyOfAxiom ax : ont.getAxioms(AxiomType.SUB_ANNOTATION_PROPERTY_OF)) {
				if (ax.getSuperProperty().equals(superProp)) {
					result.add(ax.getSubProperty());
				}
			}
		}
		return result;
	}
	
	public static Set<OWLAnnotationProperty> getSuperProperties(OWLAnnotationProperty subProp, OWLOntology ont) {
		return getSuperProperties(subProp, Collections.singleton(ont));
	}
	
	public static Set<OWLAnnotationProperty> getSuperProperties(OWLAnnotationProperty subProp, Set<OWLOntology> ontologies) {
		Set<OWLAnnotationProperty> result = new HashSet<OWLAnnotationProperty>();
		for (OWLOntology ont : ontologies) {
			for (OWLSubAnnotationPropertyOfAxiom ax : ont.getAxioms(AxiomType.SUB_ANNOTATION_PROPERTY_OF)) {
				if (ax.getSubProperty().equals(subProp)) {
					result.add(ax.getSuperProperty());
				}
			}
		}
		return result;
	}

	public static Set<OWLObjectPropertyExpression> getSuperProperties(OWLObjectPropertyExpression prop, OWLOntology ont) {
		Set<OWLObjectPropertyExpression> result = new HashSet<>();
		Set<OWLSubObjectPropertyOfAxiom> axioms = ont.getObjectSubPropertyAxiomsForSubProperty(prop);
		for (OWLSubPropertyAxiom<OWLObjectPropertyExpression> axiom : axioms) {
			result.add(axiom.getSuperProperty());
		}
		return result;
	}
	
	public static Set<OWLObjectPropertyExpression> getSubProperties(OWLObjectPropertyExpression prop, OWLOntology ont) {
		Set<OWLObjectPropertyExpression> results = new HashSet<>();
		Set<OWLSubObjectPropertyOfAxiom> axioms = ont.getObjectSubPropertyAxiomsForSuperProperty(prop);
		for (OWLSubObjectPropertyOfAxiom axiom : axioms) {
			results.add(axiom.getSubProperty());
		}
		return results;
	}
}
