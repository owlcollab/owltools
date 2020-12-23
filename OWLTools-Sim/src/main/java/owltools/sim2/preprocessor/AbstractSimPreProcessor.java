package owltools.sim2.preprocessor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.obolibrary.obo2owl.OWLAPIOwl2Obo;
import org.obolibrary.obo2owl.Obo2OWLConstants;
import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import owltools.io.OWLPrettyPrinter;
import owltools.util.OwlHelper;

public abstract class AbstractSimPreProcessor implements SimPreProcessor {

	OWLOntology inputOntology;
	OWLOntology outputOntology;
	OWLReasoner reasoner;
	Map<OWLClass,Set<OWLClass>> viewMap = new HashMap<OWLClass,Set<OWLClass>>();
	Map<OWLClass,Map<OWLObjectProperty,OWLClass>> viewMapByProp = new HashMap<OWLClass,Map<OWLObjectProperty,OWLClass>>();
	Set<OWLClass> newClasses = new HashSet<OWLClass>();
	OWLPrettyPrinter owlpp = null;
	Map<OWLObjectProperty,String> propertyToFormatMap = new HashMap<OWLObjectProperty,String>();
	protected Map<OWLClassExpression,OWLClass> materializedClassExpressionMap = new HashMap<OWLClassExpression,OWLClass>();
	protected Set<OWLClass> classesToSkip = new HashSet<OWLClass>();
	protected boolean saveIntermediateStates = true;
	protected Properties simProperties;


	protected Logger LOG = Logger.getLogger(AbstractSimPreProcessor.class);
	private OWLReasonerFactory reasonerFactory = new ReasonerFactory();

	@Override
	public OWLOntology getInputOntology() {
		return inputOntology;
	}

	@Override
	public void setInputOntology(OWLOntology inputOntology) {
		this.inputOntology = inputOntology;
		if (outputOntology == null)
			outputOntology = inputOntology;
	}

	@Override
	public OWLOntology getOutputOntology() {
		return outputOntology;
	}

	@Override
	public void setOutputOntology(OWLOntology outputOntology) {
		this.outputOntology = outputOntology;
	}

	@Override
	public synchronized OWLReasoner getReasoner() {
		if (reasoner == null) {
			reasoner = reasonerFactory.createReasoner(outputOntology); // buffering
		}
		return reasoner;
	}

	@Override
	public synchronized void setReasoner(OWLReasoner reasoner) {
		if (this.reasoner != null && reasoner != this.reasoner) {
			reasoner.dispose();
		}
		this.reasoner = reasoner;
	}

	@Override
	public void setReasonerFactory(OWLReasonerFactory reasonerFactory) {
		this.reasonerFactory = reasonerFactory;
	}

	@Override
	public synchronized void dispose() {
		if (reasoner != null) {
			reasoner.dispose();
			reasoner = null;
		}
	}

	@Override
	public void setOWLPrettyPrinter(OWLPrettyPrinter owlpp) {
		this.owlpp = owlpp;
	}

	// TODO - use this
	public Properties getSimProperties() {
		return simProperties;
	}

	@Override
	public void setSimProperties(Properties simProperties) {
		this.simProperties = simProperties;
	}
	
	public String getProperty(String k) {
		if (simProperties == null)
			return null;
		else
			return simProperties.getProperty(k);
	}
	
	protected void addViewMapping(OWLClass c, OWLObjectProperty p, OWLClass vc) {
		if (!viewMap.containsKey(c))
			viewMap.put(c, new HashSet<OWLClass>());
		viewMap.get(c).add(vc);	
		if (!viewMapByProp.containsKey(c))
			viewMapByProp.put(c, new HashMap<OWLObjectProperty,OWLClass>());
		viewMapByProp.get(c).put(p, vc);	

	}

	@Override
	public Set<OWLClass> getViewClasses(OWLClass c) {
		return viewMap.get(c);
	}

	@Override
	public abstract void preprocess();
	
	@Override
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
		propertyToFormatMap.put(viewProperty, labelFormat);
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
		addAxiomsToOutput(newAxioms, true);
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

	public void addAxiomToOutput(OWLAxiom newAxiom, boolean isFlush) {
		addAxiomsToOutput(Collections.singleton(newAxiom), isFlush);
	}
	public void addAxiomsToOutput(Set<OWLAxiom> newAxioms, boolean isFlush) {
		if (newAxioms.size() > 0) {
			LOG.info("Adding axioms: "+newAxioms.size());
			LOG.info("Example axiom: "+newAxioms.iterator().next());

			getOWLOntologyManager().addAxioms(outputOntology, newAxioms);
			if (isFlush) {
				flush();  // NOTE - assumes this method is called outside reasoning loop
			}
		}
	}
	public void removeAxiomsFromOutput(Set<OWLAxiom> rmAxioms, boolean isFlush) {
		if (rmAxioms.size() > 0) {
			LOG.info("Removing axioms: "+rmAxioms.size());
			LOG.info("Example axiom: "+rmAxioms.iterator().next());

			getOWLOntologyManager().removeAxioms(outputOntology, rmAxioms);
			if (isFlush) {
				flush();  // NOTE - assumes this method is called outside reasoning loop
			}
		}
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
		OWLAnnotationProperty property = getOWLDataFactory().getRDFSLabel();
		for (OWLAnnotation ann : OwlHelper.getAnnotations(c, property, ont)) {
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
				String baseId = OWLAPIOwl2Obo.getIdentifier(vcIRI);
				String relId = OWLAPIOwl2Obo.getIdentifier(vpIRI);
				vcIRIstr = Obo2OWLConstants.DEFAULT_IRI_PREFIX
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

	public Set<OWLClass> assertInferredForAttributeClasses() {
		Set<OWLAxiom> newAxioms = new HashSet<OWLAxiom>();
		Set<OWLClass> retainedClasses = new HashSet<OWLClass>();
		Set<OWLClass> usedClasses= new HashSet<OWLClass>(); // including indirect. Not used?
		for (OWLNamedIndividual ind : outputOntology.getIndividualsInSignature(Imports.INCLUDED)) {
			usedClasses.addAll(getReasoner().getTypes(ind, false).getFlattened());
		}
		LOG.info("Inferred direct classes from ABox:"+usedClasses.size());
		for (OWLClass c : usedClasses) {
			retainedClasses.add(c);
			for (OWLClass s : getReasoner().getSuperClasses(c, true).getFlattened()) {
				newAxioms.add(getOWLDataFactory().getOWLSubClassOfAxiom(c, s));
				retainedClasses.add(s);
			}
			for (OWLClass s : getReasoner().getEquivalentClasses(c)) {
				newAxioms.add(getOWLDataFactory().getOWLEquivalentClassesAxiom(c, s));
				retainedClasses.add(s);
			}
			for (OWLNamedIndividual ind : getReasoner().getInstances(c, true).getFlattened()) {
				// note: in future may wish to retain non-grouping informative classes - e.g. species
				newAxioms.add(getOWLDataFactory().getOWLClassAssertionAxiom(c, ind));
			}
		}
		addAxiomsToOutput(newAxioms, true);
		return usedClasses;
	}

	public void removeDisjointClassesAxioms() {
		getOWLOntologyManager().removeAxioms(outputOntology, 
				outputOntology.getAxioms(AxiomType.DISJOINT_CLASSES));

	}

	// todo - remove all 'Thing' classes
	public void trim() {
		Set<OWLClass> retainedClasses = assertInferredForAttributeClasses();
		Set<OWLClass> unused = outputOntology.getClassesInSignature(Imports.INCLUDED);
		LOG.info("Keeping "+retainedClasses.size()+" out of "+unused.size());
		unused.removeAll( retainedClasses );
		Set<OWLAxiom> rmAxioms = new HashSet<OWLAxiom>();
		for (OWLClass c : unused) {
			rmAxioms.addAll(outputOntology.getReferencingAxioms(c));
			rmAxioms.addAll(outputOntology.getAnnotationAssertionAxioms(c.getIRI()));
		}
		LOG.info("Removing unused: "+rmAxioms.size());
		getOWLOntologyManager().removeAxioms(outputOntology, rmAxioms);
		flush();
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
			classes = inputOntology.getClassesInSignature(Imports.INCLUDED);
		}
		else {
			classes = getReasoner().getSubClasses(rootClass, false).getFlattened();
			classes.add(rootClass);
		}
		return classes;
	}

	/**
	 * @return all named classes inferred to be direct types for the set of individuals
	 */
	protected Set<OWLClass> getAttributeClasses() {
		Set<OWLClass> types = new HashSet<OWLClass>();
		for (OWLNamedIndividual ind : this.outputOntology.getIndividualsInSignature(Imports.INCLUDED)) {
			types.addAll(getReasoner().getTypes(ind, true).getFlattened());
		}
		return types;
	}

	@Deprecated
	public Set<OWLClassExpression> getDirectAttributeClassExpressions() {
		Set<OWLClassExpression> types = new HashSet<OWLClassExpression>();
		for (OWLNamedIndividual ind : inputOntology.getIndividualsInSignature(Imports.INCLUDED)) {
			types.addAll(OwlHelper.getTypes(ind, inputOntology));
		}
		LOG.info("Num attribute expressions = "+types.size());
		return types;
	}

	public Set<OWLClass> materializeClassExpressionsReferencedBy(OWLObjectProperty p) {
		Set<OWLClassExpression> xs = new HashSet<OWLClassExpression>();
		for (OWLAxiom ax : outputOntology.getReferencingAxioms(p, Imports.INCLUDED)) {
			if (ax instanceof OWLSubClassOfAxiom) {
				xs.addAll(getClassExpressionReferencedBy(p, ((OWLSubClassOfAxiom)ax).getSuperClass()));
			}
			else if (ax instanceof OWLClassAssertionAxiom) {
				xs.addAll(getClassExpressionReferencedBy(p, ((OWLClassAssertionAxiom)ax).getClassExpression()));
			}
			else if (ax instanceof OWLEquivalentClassesAxiom) {
				for (OWLClassExpression x : ((OWLEquivalentClassesAxiom)ax).getClassExpressions()) {
					xs.addAll(getClassExpressionReferencedBy(p,x));
				}
			}
		}
		return materializeClassExpressions(xs);
	}

	/**
	 * E.g. if x = A and B and q some (p some z), return z 
	 * 
	 * @param p
	 * @param x
	 * @return all expressions that follow a 'p' in a SOME restriction
	 */
	private Set<OWLClassExpression> getClassExpressionReferencedBy(OWLObjectProperty p, OWLClassExpression x) {
		Set<OWLClassExpression> xs = new HashSet<OWLClassExpression>();
		if (x instanceof OWLObjectSomeValuesFrom) {
			OWLObjectSomeValuesFrom svf = (OWLObjectSomeValuesFrom)x;
			if (svf.getProperty().equals(p)) {
				return Collections.singleton(svf.getFiller());
			}
			else {
				return getClassExpressionReferencedBy(p, svf.getFiller());
			}
		}
		else if (x instanceof OWLObjectIntersectionOf) {
			for (OWLClassExpression op : ((OWLObjectIntersectionOf)x).getOperands()) {
				xs.addAll(getClassExpressionReferencedBy(p,op));
			}
		}
		else if (x instanceof OWLObjectUnionOf) {
			for (OWLClassExpression op : ((OWLObjectUnionOf)x).getOperands()) {
				xs.addAll(getClassExpressionReferencedBy(p,op));
			}
		}

		return xs;
	}

	public OWLClass materializeClassExpression(OWLClassExpression ce) {
		if (materializedClassExpressionMap.containsKey(ce))
			return materializedClassExpressionMap.get(ce);
		else
			return materializeClassExpressions(Collections.singleton(ce)).iterator().next();
	}
	/**
	 * Note: does not flush
	 * @param ces
	 * @return classes
	 */
	public Set<OWLClass> materializeClassExpressions(Set<OWLClassExpression> ces) {
		LOG.info("Materializing class expressions: "+ces.size());
		OWLAnnotationProperty rdfsLabel = getOWLDataFactory().getRDFSLabel();
		Set<OWLAxiom> newAxioms = new HashSet<OWLAxiom>();
		Set<OWLClass> newClasses = new HashSet<OWLClass>();
		for (OWLClassExpression ce : ces) {
			if (ce instanceof OWLClass) {
				newClasses.add((OWLClass) ce);
				continue;
			}
			if (materializedClassExpressionMap.containsKey(ce)) {
				newClasses.add(materializedClassExpressionMap.get(ce));
				continue;
			}

			OWLClass mc = getOWLDataFactory().getOWLClass(IRI.create("http://x.org#"+MD5(ce.toString())));
			newAxioms.add(getOWLDataFactory().getOWLDeclarationAxiom(mc));
			newAxioms.add(getOWLDataFactory().getOWLEquivalentClassesAxiom(mc, ce));
			newAxioms.add(
					getOWLDataFactory().getOWLAnnotationAssertionAxiom(rdfsLabel, mc.getIRI(), 
							getOWLDataFactory().getOWLLiteral(generateLabel(ce)))
			);
			LOG.info(mc + " EQUIV_TO "+ce);
			materializedClassExpressionMap.put(ce, mc);
			newClasses.add(mc);
		}
		// some CEs will be identical, but they will be mapped to the same class.
		// we might be able to optimize by pre-filtering dupes
		addAxiomsToOutput(newAxioms, false);
		LOG.info("Materialized "+ces.size()+ " class expressions, axioms: "+newAxioms.size()+", new classes:"+newClasses.size());


		return newClasses;
	}

	public String MD5(String md5) {
		try {
			java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
			byte[] array = md.digest(md5.getBytes());
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < array.length; ++i) {
				sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1,3));
			}
			return sb.toString();
		} catch (java.security.NoSuchAlgorithmException e) {
		}
		return null;
	}

	public String generateLabel(OWLClassExpression x) {
		StringBuffer sb = new StringBuffer();
		if (x instanceof OWLObjectSomeValuesFrom) {
			OWLObjectSomeValuesFrom svf = (OWLObjectSomeValuesFrom) x;	
			OWLObjectPropertyExpression p = svf.getProperty();
			if (propertyToFormatMap.containsKey(p)) {
				return String.format(propertyToFormatMap.get(p), generateLabel(svf.getFiller()));
			}
			else {
				String pStr = p.toString();
				if (p instanceof OWLEntity) {
					pStr = getAnyLabel((OWLEntity)p);
				}
				return pStr + " some "+generateLabel(svf.getFiller());
			}
		}
		else if (x instanceof OWLObjectIntersectionOf) {
			OWLObjectIntersectionOf oio = (OWLObjectIntersectionOf) x;
			for (OWLClassExpression op : oio.getOperands()) {
				if (sb.length() > 0) {
					sb.append(" and ");
				}
				sb.append(generateLabel(op));
			}
			return sb.toString();
		}
		else if (x instanceof OWLClass) {
			return this.getAnyLabel((OWLClass) x);
		}
		return x.toString();
	}
	
	/**
	 * note: approximation used, may end up removing too many.
	 * 
	 * E.g. if P = q1 and inh some e1, and O type has (q1 and inh some e1), then P
	 * will not be reachable by simple graph walking. Consider materializing first
	 */
	protected void removeUnreachableAxioms() {
		LOG.info("Removing axioms unreachable from ABox. Starting with: "+outputOntology.getAxiomCount());

		Stack<OWLClass> stack = new Stack<OWLClass>();
		for (OWLNamedIndividual ind : outputOntology.getIndividualsInSignature(Imports.INCLUDED)) {
			stack.addAll(getReasoner().getTypes(ind, true).getFlattened());
			for (OWLClassExpression x : OwlHelper.getTypes(ind, outputOntology.getImportsClosure())) {
				stack.addAll(x.getClassesInSignature());
			}
		}
		Set<OWLClass> visited = new HashSet<OWLClass>();
		visited.addAll(stack);

		while (!stack.isEmpty()) {
			OWLClass elt = stack.pop();
			Set<OWLClass> parents = new HashSet<OWLClass>();
			Set<OWLClassExpression> xparents = OwlHelper.getSuperClasses(elt, inputOntology);
			xparents.addAll(OwlHelper.getEquivalentClasses(elt, inputOntology));
			for (OWLClassExpression x : xparents) {
				parents.addAll(x.getClassesInSignature());
			}
			parents.addAll(getReasoner().getSuperClasses(elt, true).getFlattened());
			parents.addAll(getReasoner().getEquivalentClasses(elt).getEntities());
			parents.removeAll(visited);
			stack.addAll(parents);
			visited.addAll(parents);
		}

		Set<OWLAxiom> rmAxioms = new HashSet<OWLAxiom>();
		for (OWLClass c : outputOntology.getClassesInSignature()) {
			if (!visited.contains(c)) {
				LOG.info("removing axioms for EL-unreachable class: "+c);
				rmAxioms.addAll(outputOntology.getAxioms(c, Imports.EXCLUDED));
				rmAxioms.add(getOWLDataFactory().getOWLDeclarationAxiom(c));
			}
		}


		getOWLOntologyManager().removeAxioms(outputOntology, rmAxioms);
		LOG.info("Removed "+rmAxioms.size()+" axioms. Remaining: "+outputOntology.getAxiomCount());
	}

	// note: this is currently somewhat obo-format specific. Make this configurable - TODO
	public boolean isUpperLevel(OWLClass c) {
		// TODO - cache
		Set<OWLAnnotation> anns = OwlHelper.getAnnotations(c, inputOntology);
		for (OWLAnnotation ann : anns) {
			String ap = ann.getProperty().getIRI().toString();
			OWLAnnotationValue v = ann.getValue();
			if (v instanceof IRI) {
				IRI iv = (IRI)v;
				if (ap.endsWith("inSubset")) {
					// TODO - formalize this
					if (iv.toString().contains("upper_level")) {
						return true;
					}
					// this tag is used in uberon
					if (iv.toString().contains("non_informative")) {
						return true;
					}
					// hack: representation of early dev a bit problematic
					// temporary: find a way to exclude these axiomatically
					if (iv.toString().contains("early_development")) {
						return true;
					}
				}

			}
		}
		return false;
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

	public void saveState(String state) {
		String fileBase = getProperty("cacheFileBase");
		if (fileBase == null || fileBase.equals("")) {
			fileBase = "/tmp/owlsim";
		}
		
		if (saveIntermediateStates) {
			String fn = fileBase+"-"+state+".owl";
			FileOutputStream os;
			try {
				os = new FileOutputStream(new File(fn));
				OWLDocumentFormat owlFormat = new RDFXMLDocumentFormat();

				getOWLOntologyManager().saveOntology(outputOntology, owlFormat, os);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (OWLOntologyStorageException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}
	
	protected void ignoreClasses(Set<String> labels) {
		for (OWLClass c : inputOntology.getClassesInSignature(Imports.INCLUDED)) {
			String label = this.getAnyLabel(c);
			if (labels.contains(label)) {
				classesToSkip.add(c);
			}
		}
	}
	
	@Override
	public OWLObjectProperty getAboxProperty() {
		return null;
	}


}
