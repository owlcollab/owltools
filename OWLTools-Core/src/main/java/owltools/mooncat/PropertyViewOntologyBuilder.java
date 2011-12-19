package owltools.mooncat;

import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

public class PropertyViewOntologyBuilder {
	private OWLDataFactory owlDataFactory;
	private OWLOntologyManager owlOntologyManager;
	private OWLOntology sourceOntology;
	private OWLOntology unitsOntology;
	private OWLOntology assertedViewOntology;
	private OWLOntology inferredViewOntology;
	private OWLObjectProperty viewProperty;
	private boolean isUseOriginalClassIRIs = false;
	private String viewLabelPrefix;
	private Set<OWLEntity> viewEntities;
	private OWLClass viewRootClass;

	public PropertyViewOntologyBuilder(OWLOntology sourceOntology,
			OWLOntology unitsOntology, OWLObjectProperty viewProperty) {
		super();
		this.owlOntologyManager = OWLManager.createOWLOntologyManager();
		this.owlDataFactory = owlOntologyManager.getOWLDataFactory();
		this.sourceOntology = sourceOntology;
		this.unitsOntology = unitsOntology;
		this.viewProperty = viewProperty;
		init();
	}

	public PropertyViewOntologyBuilder(OWLDataFactory owlDataFactory,
			OWLOntologyManager owlOntologyManager, OWLOntology sourceOntology,
			OWLOntology unitsOntology, OWLObjectProperty viewProperty) {
		super();
		this.owlDataFactory = owlDataFactory;
		this.owlOntologyManager = owlOntologyManager;
		this.sourceOntology = sourceOntology;
		this.unitsOntology = unitsOntology;
		this.viewProperty = viewProperty;
		init();
	}



	private void init() {
		viewLabelPrefix = getAnyLabel(viewProperty)+" a";
		viewEntities = new HashSet<OWLEntity>();
	}



	public OWLOntology getAssertedViewOntology() {
		return assertedViewOntology;
	}

	public void setAssertedViewOntology(OWLOntology assertedViewOntology) {
		this.assertedViewOntology = assertedViewOntology;
	}

	public OWLOntology getInferredViewOntology() {
		return inferredViewOntology;
	}

	public void setInferredViewOntology(OWLOntology inferredViewOntology) {
		this.inferredViewOntology = inferredViewOntology;
	}



	public Set<OWLEntity> getViewEntities() {
		return viewEntities;
	}

	public void setViewEntities(Set<OWLEntity> viewEntities) {
		this.viewEntities = viewEntities;
	}

	public void addViewEntities(Set<OWLEntity> newEntities) {
		viewEntities.addAll(newEntities);
	}

	public OWLClass getViewRootClass() {
		return viewRootClass;
	}

	/**
	 * @param voIRI
	 * @throws OWLOntologyCreationException
	 */
	public void buildViewOntology(IRI voIRI, IRI ivoIRI) throws OWLOntologyCreationException {
		Set<OWLOntology> imports = new HashSet<OWLOntology>();
		imports.add(sourceOntology);
		imports.add(unitsOntology);
		// the AVO includes the source ontology and elemental ontology in its imports
		//  - when we reason, we need axioms from all
		assertedViewOntology = owlOntologyManager.createOntology(voIRI, imports);
		if (voIRI.equals(ivoIRI))
			inferredViewOntology = assertedViewOntology;
		else
			inferredViewOntology = owlOntologyManager.createOntology(ivoIRI);

		Set<OWLClass> sourceClasses = sourceOntology.getClassesInSignature();
		OWLClass thing = owlDataFactory.getOWLThing();
		sourceClasses.add(thing);
		for (OWLClass c : sourceClasses) {
			IRI vcIRI = makeViewClassIRI(c);
			OWLClass vc = owlDataFactory.getOWLClass(vcIRI);
			OWLObjectSomeValuesFrom vx = owlDataFactory.getOWLObjectSomeValuesFrom(viewProperty, c);
			OWLEquivalentClassesAxiom eca = 
				owlDataFactory.getOWLEquivalentClassesAxiom(vc, vx);
			owlOntologyManager.addAxiom(assertedViewOntology, eca);
			viewEntities.add(vc);
			if (c.equals(thing)) {
				viewRootClass = vc;
			}
		}		
		// todo - units
		//createViewRootClass();
	}


	/**
	 * @param reasoner
	 */
	public void buildInferredViewOntology(OWLReasoner reasoner) {
		
		// add units
		// todo - make this configurable
		// for now assume we include all entities that satisfy query
		OWLObjectSomeValuesFrom qx = owlDataFactory.getOWLObjectSomeValuesFrom(viewProperty, owlDataFactory.getOWLThing());
		Set<OWLEntity> genes = new HashSet<OWLEntity>();
		
		for (OWLClass gene : reasoner.getSubClasses(getViewRootClass(), false).getFlattened()) {
			//System.out.println("G:"+gene);
			genes.add((OWLEntity) gene);
		}
		addViewEntities(genes);

		
		for (OWLEntity e : viewEntities) {
			if (e instanceof OWLClass) {
				OWLClass c = (OWLClass) e;
				//System.out.println("C="+c);
				Set<OWLClass> scs = reasoner.getSuperClasses(c, true).getFlattened();
				for (OWLClass sc : scs) {
					if (viewEntities.contains(sc)) {
						OWLSubClassOfAxiom sca = owlDataFactory.getOWLSubClassOfAxiom(c, sc);
						owlOntologyManager.addAxiom(inferredViewOntology, sca);
					}
				}
			}
			else {
				// TODO - individuals
			}
		}
		// support this later; remember that individuals not supported in Elk
		/*
		for (OWLNamedIndividual ind : unitsOntology.getIndividualsInSignature()) {
			Set<OWLClass> scs = reasoner.getTypes(ind, true).getFlattened();
			for (OWLClass sc : scs) {
				OWLClassAssertionAxiom caa = owlDataFactory.getOWLClassAssertionAxiom(sc, ind);
				owlOntologyManager.addAxiom(inferredViewOntology, caa);
			}			
		}
		 */
	}

	/**
	 * @param c
	 * @return
	 */
	public IRI makeViewClassIRI(OWLClass c) {
		IRI vcIRI = c.getIRI();
		if (!isUseOriginalClassIRIs) {
			vcIRI = IRI.create(vcIRI.toString() + "_view");
			String vLabel = getAnyLabel(c);
			if (vLabel != null) {
				OWLAnnotationAssertionAxiom aaa = owlDataFactory.getOWLAnnotationAssertionAxiom(owlDataFactory.getRDFSLabel(), vcIRI, 
						owlDataFactory.getOWLLiteral(vLabel));
				// anything derived -- including labels -- goes in the derived ontology
				owlOntologyManager.addAxiom(inferredViewOntology, aaa);
			}
		}
		return vcIRI;
	}

	public String getAnyLabel(OWLEntity c) {
		String vLabel = null;
		// todo - ontology import closure
		for (OWLAnnotation ann : c.getAnnotations(sourceOntology, owlDataFactory.getRDFSLabel())) {
			OWLAnnotationValue v = ann.getValue();
			if (v instanceof OWLLiteral) {
				String label = ((OWLLiteral)v).getLiteral();
				vLabel = viewLabelPrefix + " " + label;
				break;
			}
		}
		return vLabel;
	}


}
