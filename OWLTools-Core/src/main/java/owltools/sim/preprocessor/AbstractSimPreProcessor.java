package owltools.sim.preprocessor;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.obolibrary.obo2owl.Owl2Obo;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import owltools.sim.SimpleOwlSim;

public abstract class AbstractSimPreProcessor implements SimPreProcessor {

	OWLOntology inputOntology;
	OWLOntology outputOntology;
	OWLReasoner reasoner;
	Map<OWLClass,Set<OWLClass>> viewMap = new HashMap<OWLClass,Set<OWLClass>>();
	Map<OWLClass,Map<OWLObjectProperty,OWLClass>> viewMapByProp = new HashMap<OWLClass,Map<OWLObjectProperty,OWLClass>>();
	Set<OWLClass> newClasses = new HashSet<OWLClass>();

	protected Logger LOG = Logger.getLogger(AbstractSimPreProcessor.class);
	private OWLReasonerFactory reasonerFactory = new ElkReasonerFactory();

	public OWLOntology getInputOntology() {
		return inputOntology;
	}

	public void setInputOntology(OWLOntology inputOntology) {
		this.inputOntology = inputOntology;
		if (outputOntology == null)
			outputOntology = inputOntology;
	}

	public OWLOntology getOutputOntology() {
		return outputOntology;
	}

	public void setOutputOntology(OWLOntology outputOntology) {
		this.outputOntology = outputOntology;
	}

	public OWLReasoner getReasoner() {
		if (reasoner == null) {
			reasoner = reasonerFactory.createReasoner(outputOntology); // buffering
		}
		return reasoner;
	}

	public void setReasoner(OWLReasoner reasoner) {
		this.reasoner = reasoner;
	}

	public void setReasonerFactory(OWLReasonerFactory reasonerFactory) {
		this.reasonerFactory = reasonerFactory;
	}


	protected void addViewMapping(OWLClass c, OWLObjectProperty p, OWLClass vc) {
		if (!viewMap.containsKey(c))
			viewMap.put(c, new HashSet<OWLClass>());
		viewMap.get(c).add(vc);	
		if (!viewMapByProp.containsKey(c))
			viewMapByProp.put(c, new HashMap<OWLObjectProperty,OWLClass>());
		viewMapByProp.get(c).put(p, vc);	

	}

	public Set<OWLClass> getViewClasses(OWLClass c) {
		return viewMap.get(c);
	}

	public abstract void preprocess();
	public abstract OWLClassExpression getLowestCommonSubsumer(OWLClassExpression a, OWLClassExpression b);

	public void makeReflexive(OWLObjectProperty p) {
		makeReflexive(p,getOWLDataFactory().getOWLThing());
	}

	/**
	 * makes class and all subclasses reflexive
	 * @param p
	 * @param rootClass
	 */
	public void makeReflexive(OWLObjectProperty p, OWLClass rootClass) {
		Set<OWLAxiom> newAxioms = 
			new HashSet<OWLAxiom>();
		for (OWLClass c : getReflexiveSubClasses(rootClass)) {
			newAxioms.add(getOWLDataFactory().getOWLSubClassOfAxiom(c, 
					getOWLDataFactory().getOWLObjectSomeValuesFrom(p, c)));
		}
		getOWLOntologyManager().addAxioms(outputOntology, newAxioms);
	}

	public Set<OWLAxiom> createPropertyView(OWLObjectProperty viewProperty) {

		String pLabel = getAnyLabel(viewProperty);
		return createPropertyView(viewProperty, getOWLDataFactory().getOWLThing(), pLabel+" %s");
	}

	public Set<OWLAxiom> createPropertyView(OWLObjectProperty viewProperty, OWLClass rootClass, String labelFormat) {
		return createPropertyView(viewProperty, getReflexiveSubClasses(rootClass), labelFormat); 
	}

	public Set<OWLAxiom> createPropertyView(OWLObjectProperty viewProperty, Set<OWLClass> classes) {
		String pLabel = getAnyLabel(viewProperty);
		return createPropertyView(viewProperty, classes, pLabel+" %s");
	}
	public Set<OWLAxiom> createPropertyView(OWLObjectProperty viewProperty, Set<OWLClass> classes, String labelFormat) {
		OWLAnnotationProperty rdfsLabel = getOWLDataFactory().getRDFSLabel();
		Set<OWLAxiom> newAxioms = new HashSet<OWLAxiom>();
		for (OWLClass c : classes) {
			if (c.equals(getOWLDataFactory().getOWLNothing())) {
				continue;
			}
			OWLClass vc = getOWLDataFactory().getOWLClass(makeViewClassIRI(c.getIRI(), viewProperty.getIRI(), "-", false, false));
			OWLObjectSomeValuesFrom vx = getOWLDataFactory().getOWLObjectSomeValuesFrom(viewProperty, c);
			newAxioms.add(
					getOWLDataFactory().getOWLEquivalentClassesAxiom(vc, vx)
			);
			newAxioms.add(
					getOWLDataFactory().getOWLDeclarationAxiom(vc)
			);
			String label = getAnyLabel(c);
			String viewLabel = String.format(labelFormat, label);
			newAxioms.add(
					getOWLDataFactory().getOWLAnnotationAssertionAxiom(rdfsLabel, vc.getIRI(), 
							getOWLDataFactory().getOWLLiteral(viewLabel))
			);
			LOG.info("VIEW_CLASS:"+vc+" "+viewLabel+" = P some "+c);
			addViewMapping(c, viewProperty, vc);
			newClasses.add(vc);
		}	
		LOG.info("Num new classes:"+newClasses.size());
		addAxiomsToOutput(newAxioms);
		return newAxioms;
	}

	/**
	 * Call after createPropertyView()
	 * 
	 * if the view contains a class C' = P some C, and C' does not classify and individuals or classes in the ontology
	 * (excluding other view classes in this set), then C' and related axioms are removed
	 * 
	 * TODO: note that in general this should not be used if P is reflexive, as there will trivially be subclasses of the view class
	 * 
	 * @param newAxioms
	 */
	public void filterUnused(Set<OWLAxiom> newAxioms) {
		getReasoner().flush();
		Set<OWLAxiom> rmAxioms = new HashSet<OWLAxiom>();
		Set<OWLClass> rmClasses = new HashSet<OWLClass>();
		Set<OWLClass> viewClasses = extractClassesFromDeclarations(newAxioms);
		for (OWLClass vc : viewClasses) {
			LOG.info("Testing utility of "+vc);
			if (getReasoner().getInstances(vc, false).getFlattened().size() > 0) {
				continue;
			}
			Set<OWLClass> subs = getReasoner().getSubClasses(vc, false).getFlattened();
			subs.remove(vc);
			subs.removeAll(viewClasses);
			if (subs.size() > 0) {
				continue;
			}
			LOG.info("rmClass:"+vc);

			rmClasses.add(vc);
		}
		if (rmClasses.size() > 0) {
			for (OWLAxiom ax : newAxioms) {
				if (ax.getSignature().retainAll(rmClasses)) {
					rmAxioms.add(ax);
				}
			}
		}
		LOG.info("Removing axioms as they are unused: "+rmAxioms.size());
		// TODO - viewclassmap
		this.getOWLOntologyManager().removeAxioms(outputOntology, rmAxioms);
	}

	public void addAxiomsToOutput(Set<OWLAxiom> newAxioms) {
		getOWLOntologyManager().addAxioms(outputOntology, newAxioms);
		//		reasoner.flush();
	}

	public Set<OWLClass> extractClassesFromDeclarations(Set<OWLAxiom> axs) {
		Set<OWLClass> cs = new HashSet<OWLClass>();
		for (OWLAxiom ax : axs) {
			if (ax instanceof OWLDeclarationAxiom) {
				OWLEntity e = ((OWLDeclarationAxiom)ax).getEntity();
				if (e instanceof OWLClass)
					cs.add((OWLClass) e);
			}
		}
		return cs;
	}

	// UTIL

	public String getLabel(OWLEntity c, OWLOntology ont) {
		String label = null;		
		// todo - ontology import closure
		for (OWLAnnotation ann : c.getAnnotations(ont, getOWLDataFactory().getRDFSLabel())) {
			OWLAnnotationValue v = ann.getValue();
			if (v instanceof OWLLiteral) {
				label = ((OWLLiteral)v).getLiteral();
				break;
			}
		}
		return label;
	}

	protected String getAnyLabel(OWLEntity c) {
		String label = getLabel(c, inputOntology);
		if (label == null) {
			// non-OBO-style ontology
			label = c.getIRI().getFragment();
			if (label == null) {
				label = c.getIRI().toString();
				label = label.replaceAll(".*/", "");
			}
		}
		return label;
	}

	public IRI makeViewClassIRI(IRI vcIRI, IRI vpIRI) {
		return makeViewClassIRI(vcIRI, vpIRI, "-", false, false);
	}

	public IRI makeViewClassIRI(IRI vcIRI, IRI vpIRI, String sep, boolean isUseOriginalClassIRIs, boolean isAssumeOBOStyleIRIs) {
		if (!isUseOriginalClassIRIs) {
			String vcIRIstr = vcIRI.toString();
			if (isAssumeOBOStyleIRIs) {
				String baseId = Owl2Obo.getIdentifier(vcIRI);
				String relId = Owl2Obo.getIdentifier(vpIRI);
				vcIRIstr = "http://purl.obolibrary.org/obo/"
					+ baseId.replace(":", "_") + sep
					+ relId.replace("_", "-").replace(":", "-");
			}
			else {
				String frag = vpIRI.getFragment();
				if (frag == null || frag.equals("")) {
					frag = vpIRI.toString().replaceAll(".*\\/", "view");
				}
				vcIRIstr = vcIRIstr + sep + frag;
			}
			vcIRI = IRI.create(vcIRIstr);
		}
		return vcIRI;
	}


	public Set<OWLClass> getReflexiveSubClasses(OWLClassExpression rootClassExpr) {
		Set<OWLClass> classes;
		OWLClass rootClass;
		if (rootClassExpr instanceof OWLClass) {
			rootClass = (OWLClass)rootClassExpr;
		}
		else {
			rootClass = materializeClassExpression(rootClassExpr);
		}
		
		if (rootClass.equals(getOWLDataFactory().getOWLThing())) {
			classes = inputOntology.getClassesInSignature(true);
		}
		else {
			classes = reasoner.getSubClasses(rootClass, false).getFlattened();
			classes.add(rootClass);
		}
		return classes;
	}



	public Set<OWLClassExpression> getDirectAttributeClassExpressions() {
		Set<OWLClassExpression> types = new HashSet<OWLClassExpression>();
		for (OWLNamedIndividual ind : inputOntology.getIndividualsInSignature(true)) {
			types.addAll(ind.getTypes(inputOntology));
		}
		LOG.info("Num attribute expressions = "+types.size());
		return types;
	}


	public OWLClass materializeClassExpression(OWLClassExpression ce) {
		Set<OWLAxiom> axs = materializeClassExpressions(Collections.singleton(ce));
		return this.extractClassesFromDeclarations(axs).iterator().next();
	}
	public Set<OWLAxiom> materializeClassExpressions(Set<OWLClassExpression> ces) {
		Set<OWLAxiom> newAxioms = new HashSet<OWLAxiom>();
		for (OWLClassExpression ce : ces) {
			if (ce instanceof OWLClass)
				continue;
			OWLClass mc = getOWLDataFactory().getOWLClass(IRI.create("http://x.org#"+UUID.randomUUID().toString()));
			newAxioms.add(getOWLDataFactory().getOWLDeclarationAxiom(mc));
			newAxioms.add(getOWLDataFactory().getOWLEquivalentClassesAxiom(mc, ce));
			LOG.info(mc + " EQUIV_TO "+ce);
		}
		this.addAxiomsToOutput(newAxioms);
		return newAxioms;
	}

	OWLDataFactory getOWLDataFactory() {
		return getOWLOntologyManager().getOWLDataFactory();
	}

	OWLOntologyManager getOWLOntologyManager() {
		return inputOntology.getOWLOntologyManager();
	}

	public void flush() {
		LOG.info("flushing...");
		getReasoner().flush();
	}
}
