package owltools.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.obolibrary.obo2owl.Obo2Owl;
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
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLInverseObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectCardinalityRestriction;
import org.semanticweb.owlapi.model.OWLObjectComplementOf;
import org.semanticweb.owlapi.model.OWLObjectHasValue;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLPropertyExpression;
import org.semanticweb.owlapi.model.OWLRestriction;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.OWLSubObjectPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubPropertyChainOfAxiom;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.graph.OWLGraphEdge.OWLGraphEdgeSet;
import owltools.graph.OWLQuantifiedProperty.Quantifier;
import owltools.io.OWLPrettyPrinter;
import owltools.profile.Profiler;

/**
 * Methods to extract and traverse relation in the graph. Provides caching for
 * some time-intensive lookup methods.<br>
 * Set a {@link Config} object to customize the behavior for closures and used
 * properties.<br>
 * May use an {@link OWLReasoner}, if available, to infer relations between
 * entities.
 * 
 * @see OWLGraphWrapper
 * @see OWLGraphWrapperExtended
 */
public class OWLGraphWrapperEdges extends OWLGraphWrapperExtended {

	private static final Logger LOG = Logger.getLogger(OWLGraphWrapperEdges.class);

	OWLReasoner reasoner = null;
	Config config = new Config();

	private Map<OWLObject,Set<OWLGraphEdge>> edgeBySource = null;
	private Map<OWLObject,Set<OWLGraphEdge>> edgeByTarget = null;
	public Map<OWLObject,Set<OWLGraphEdge>> inferredEdgeBySource = null; // public to serialize
	private Map<OWLObject,Set<OWLGraphEdge>> inferredEdgeByTarget = null;

	// used to store mappings child->parent, where
	// parent = UnionOf( ..., child, ...)
	//private Map<OWLObject,Set<OWLObject>> extraSubClassOfEdges = null;
	
	// used to store mappings child->parent, where
    // parent = UnionOf( ..., child, ...)
	// we store the OWLGraphEdges outgoing from the child, rather than simply the parents, 
	// to be able to store the underlying OWLEquivalentClassesAxioms. 
	private Map<OWLObject, Set<OWLGraphEdge>> extraSubClassOfEdges = null;

	private final Object edgeCacheMutex = new Object();
	
	protected Profiler profiler = new Profiler();


	/**
	 * Configuration options. These are typically specific to a
	 * OWLGraphWrapper instance.
	 *
	 */
	public class Config {
		// by default the graph closure includes only named entities
		public boolean isIncludeClassExpressionsInClosure = true;

		// by default we do not follow complement of - TODO
		public boolean isFollowComplementOfInClosure = false;

		public boolean isCacheClosure = true;
		public boolean isMonitorMemory = false;

		// if set to non-null, this constrains graph traversal. TODO
		public Set<OWLQuantifiedProperty> graphEdgeIncludeSet = null;
		public Set<OWLQuantifiedProperty> graphEdgeExcludeSet = null;
		public OWLClass excludeMetaClass = null;

		/**
		 * @param p
		 * @param q
		 */
		public void excludeProperty(OWLObjectProperty p, Quantifier q) {
			if (graphEdgeExcludeSet == null)
				graphEdgeExcludeSet = new HashSet<OWLQuantifiedProperty>();
			graphEdgeExcludeSet.add(new OWLQuantifiedProperty(p, q));
		}

		/**
		 * @see #excludeProperty(OWLObjectProperty, Quantifier) - the default quantifier is some
		 * @param p
		 */
		public void excludeProperty(OWLObjectProperty p) {
			excludeProperty(p, Quantifier.SOME);
		}

		public void includeProperty(OWLObjectProperty p) {
			includeProperty(p, Quantifier.SOME);
		}
		public void includeProperty(OWLObjectProperty p, Quantifier q) {
			if (graphEdgeIncludeSet == null)
				graphEdgeIncludeSet = new HashSet<OWLQuantifiedProperty>();
			graphEdgeIncludeSet.add(new OWLQuantifiedProperty(p, q));
		}

		public void excludeAllWith(OWLAnnotationProperty ap, OWLOntology o) {
			for (OWLObjectProperty p : o.getObjectPropertiesInSignature(true)) {
				Set<OWLAnnotation> anns = p.getAnnotations(o, ap);
				for (OWLAnnotation ann : anns) {
					if (ann.getValue() instanceof OWLLiteral) {
						OWLLiteral v = (OWLLiteral) ann.getValue();
						if (v.parseBoolean()) {
							excludeProperty(p);
						}
					}

				}
			}
		}

		public void includeAllWith(OWLAnnotationProperty ap, OWLOntology o) {
			for (OWLObjectProperty p : o.getObjectPropertiesInSignature(true)) {
				Set<OWLAnnotation> anns = p.getAnnotations(o, ap);
				for (OWLAnnotation ann : anns) {
					if (ann.getValue() instanceof OWLLiteral) {
						OWLLiteral v = (OWLLiteral) ann.getValue();
						if (v.parseBoolean()) {
							includeProperty(p);
						}
					}

				}
			}
		}


	}

	protected OWLGraphWrapperEdges(OWLOntology ontology) {
		super(ontology);
	}

	protected OWLGraphWrapperEdges(String iri) throws OWLOntologyCreationException {
		super(iri);
	}

	public Profiler getProfiler() {
		return profiler;
	}

	public void setProfiler(Profiler profiler) {
		this.profiler = profiler;
	}


	public OWLReasoner getReasoner() {
		return reasoner;
	}

	/**
	 * @param reasoner
	 */
	public void setReasoner(OWLReasoner reasoner) {
		this.reasoner = reasoner;
	}

	public Config getConfig() {
		return config;
	}

	public void setConfig(Config config) {
		this.config = config;
	}


	// ----------------------------------------
	// BASIC GRAPH EDGE TRAVERSAL
	// ----------------------------------------



	/**
	 * retrieves direct edges from a source
	 * to the direct **named** target
	 * <ul>
	 * <li>e.g. if (A SubClassOf B) then outgoing(A) = { &lt;A,sub,B&gt;}</li>
	 * <li>e.g. if (A SubClassOf R some B) then outgoing(A) = { &lt;A, R-some, B&gt; }</li>
	 * <li>e.g. if (A SubClassOf R some (R2 some B)) then outgoing(A) = { &lt;A, [R-some,R2-same], B&gt; }</li>
	 * </ul>
	 * @param cls source
	 * @return all edges that originate from source to nearest named object target
	 */
	public Set<OWLGraphEdge> getOutgoingEdges(OWLObject cls) {
		return getOutgoingEdges(cls, null);
	}
	
	/**
	 * retrieves direct edges from a source
	 * to the direct **named** target for a given set of properties
	 * <ul>
	 * <li>e.g. if (A SubClassOf B) then outgoing(A) = { &lt;A,sub,B&gt;}</li>
	 * <li>e.g. if (A SubClassOf R some B) then outgoing(A) = { &lt;A, R-some, B&gt; }</li>
	 * <li>e.g. if (A SubClassOf R some (R2 some B)) then outgoing(A) = { &lt;A, [R-some,R2-same], B&gt; }</li>
	 * </ul>
	 * @param cls source
	 * @param props
	 * @return all edges that originate from source to nearest named object target
	 */
	public Set<OWLGraphEdge> getOutgoingEdges(OWLObject cls, Set<OWLPropertyExpression> props) {
		Set<OWLGraphEdge> pEdges = getPrimitiveOutgoingEdges(cls);
		LOG.debug("primitive edges:"+cls+" --> "+pEdges);

		Set<OWLGraphEdge> edges = new OWLGraphEdgeSet();
		for (OWLGraphEdge e : pEdges) {
		    edges.addAll(primitiveEdgeToFullEdges(e));
		}
        if (props != null) {
            filterEdges(edges, props);
        }
		LOG.debug("  all:"+cls+" --> "+edges);
		return edges;
	}

	public Set<OWLGraphEdge> getOutgoingEdges(OWLObject obj, boolean isClosure,boolean isReflexive) {
		if (isClosure) {
			if (isReflexive)
				return getOutgoingEdgesClosureReflexive(obj);
			else
				return getOutgoingEdgesClosure(obj);
		}
		else
			return getOutgoingEdgesClosure(obj);
	}

	/**
	 * Returns the {@code OWLGraphEdge}s representing the reversed {@code OWLObjectUnionOf} 
	 * from a {@code OWLEquivalentClassesAxiom}s (see {@link #cacheReverseUnionMap()}). 
	 * The source of the {@code OWLGraphEdge}s returned will be {@code child} (that 
	 * is actually a class expression part of the {@code OWLObjectUnionOf}), and the target
	 * the class expression which the {@code OWLEquivalentClassesAxiom} was associated to.
	 * <p>
	 * Note that you should not exposed these internal {@code OWLGraphEdge}s directly, 
	 * but copy then using the {@link OWLGraphEdge#OWLGraphEdge(OWLGraphEdge) copy-constructor} 
	 * before returning them.
	 * 
	 * @param child    The {@code OWLObject} for which we want the reversed 
	 *                 {@code OWLObjectUnionOf}s.
	 * @return         A {@code Set} of {@code OWLGraphEdge}s representing 
	 *                 the reversed {@code OWLObjectUnionOf}s.
	 */
	private Set<OWLGraphEdge> getOutgoingEdgesViaReverseUnion(OWLObject child) {
		synchronized (edgeCacheMutex) {
			if (extraSubClassOfEdges == null)
				cacheReverseUnionMap();
			
			if (extraSubClassOfEdges.containsKey(child)) 
			    return new OWLGraphEdgeSet(extraSubClassOfEdges.get(child));
			
		    return new OWLGraphEdgeSet();
		}
	}


	private void cacheReverseUnionMap() {
		synchronized (edgeCacheMutex) {
			extraSubClassOfEdges = new HashMap<OWLObject, Set<OWLGraphEdge>>();
			for (OWLOntology o : getAllOntologies()) {
				for (OWLClass cls : o.getClassesInSignature()) {
					for (OWLEquivalentClassesAxiom eca : o.getEquivalentClassesAxioms(cls)) {
						for (OWLClassExpression ce : eca.getClassExpressions()) {
							if (ce instanceof OWLObjectUnionOf) {
								for (OWLObject child : ((OWLObjectUnionOf)ce).getOperands()) {
									if (!extraSubClassOfEdges.containsKey(child)) {
									    extraSubClassOfEdges.put(child, new OWLGraphEdgeSet());
									}
									extraSubClassOfEdges.get(child).add(
									        createSubClassOfEdge(child,cls,o,eca));
								}
							}
						}
					}
				}
			}
		}
	}

	/**
	 * primitive edges connect any combination of named objects and expressions
	 * <p>
	 * e.g. (A SubClassOf R some B) =&gt; &lt;A,sub,R-some-B&gt;, &lt;R-some-B,R-some,B&gt;
	 * @param s source
	 * @param overProperties 
	 * @return set of {@link OWLGraphEdge}
	 */
	public Set<OWLGraphEdge> getPrimitiveOutgoingEdges(OWLObject s) {
		return getPrimitiveOutgoingEdges(s, null);
	}
	public Set<OWLGraphEdge> getPrimitiveOutgoingEdges(OWLObject s, Set<OWLPropertyExpression> overProperties) {
		profiler.startTaskNotify("getPrimitiveOutgoingEdges");
		Set<OWLGraphEdge> edges = new OWLGraphEdgeSet();
		for (OWLOntology o : getAllOntologies()) {
			if (s instanceof OWLClass) {

				for (OWLSubClassOfAxiom sca : o.getSubClassAxiomsForSubClass((OWLClass) s)) {
					edges.add(createSubClassOfEdge(sca.getSubClass(), sca.getSuperClass(), o, sca));
				}
				for (OWLEquivalentClassesAxiom eqa : o.getEquivalentClassesAxioms((OWLClass) s)) {
					for (OWLClassExpression ce : eqa.getClassExpressions()) {
						if (!ce.equals(s))
						    edges.add(createSubClassOfEdge(s, ce, o, eqa));
					}
				}
				for (OWLGraphEdge unionEdge : getOutgoingEdgesViaReverseUnion(s)) {
					if (unionEdge.getTarget() instanceof OWLClass)
					    edges.add(new OWLGraphEdge(unionEdge));
				}

			}
			else if (s instanceof OWLIndividual) {
				// TODO - do we care about punning?
				// need to define semantics here
				//System.err.println("getting individual axioms");
				for (OWLClassAssertionAxiom a : o.getClassAssertionAxioms((OWLIndividual) s)) {
				    edges.add(new OWLGraphEdge(s,a.getClassExpression(),null,
				            Quantifier.INSTANCE_OF, o, a));
				}
				for (OWLObjectPropertyAssertionAxiom a : o.getObjectPropertyAssertionAxioms((OWLIndividual) s)) {
				    edges.add(new OWLGraphEdge(s,a.getObject(),a.getProperty(),
				            Quantifier.PROPERTY_ASSERTION, o, a));
				}
			}
			else if (s instanceof OWLRestriction<?, ?, ?>) {
			    edges.add(restrictionToPrimitiveEdge((OWLRestriction<?, ?, ?>) s));
			}
			else if (s instanceof OWLObjectIntersectionOf) {
				for (OWLClassExpression ce : ((OWLObjectIntersectionOf)s).getOperands()) {
				    //provide a null OWLOntology, the ontology of the final OWLGraphEdge 
				    //will be obtained from the first OWLGraphEdge created from the OWLAxiom 
				    //leading to this OWLObjectIntersectionOf
				    edges.add(createSubClassOfEdge(s,ce, null));
				}
			}
			else if (s instanceof OWLObjectUnionOf) {
				// do nothing in this direction
			}
		}

		if (reasoner != null) {
			//			if (s instanceof OWLClassExpression) {
			// JCel can't do class expressions. TODO: make this more flexible
			if (s instanceof OWLClass) {
				for (Node<OWLClass> pn : reasoner.getSuperClasses( (OWLClassExpression)s, true)) {
					for (OWLClass p : pn.getEntities()) {
						OWLGraphEdge e = createSubClassOfEdge(s,p, getSourceOntology());
						e.getSingleQuantifiedProperty().setInferred(true);
						edges.add(e);
					}
				}
			}
		}

		filterEdges(edges, overProperties);
		profiler.endTaskNotify("getPrimitiveOutgoingEdges");

		return edges;
	}

	// TODO - DRY
	protected boolean isExcluded(OWLQuantifiedProperty qp) {
		if (config.graphEdgeIncludeSet != null) {
			LOG.debug("includes:"+config.graphEdgeIncludeSet);
			if (qp.getProperty() == null)
				return false;
			for (OWLQuantifiedProperty qtp : config.graphEdgeIncludeSet) {
				LOG.debug(" testing:"+qtp);
				if (qp.subsumes(qtp))
					return false;
			}
			LOG.debug(" not in inclusions list:"+qp);
			return true;
		}
		if (config.graphEdgeExcludeSet != null) {
			LOG.debug("excludes:"+config.graphEdgeExcludeSet);
			for (OWLQuantifiedProperty qtp : config.graphEdgeExcludeSet) {
				if (qtp.subsumes(qp))
					return true;
			}
			return false;
		}
		return false;
	}


	/**
	 * only include those edges that match user constraints.
	 * 
	 * default is to include
	 * 
	 * If the includeSet is specified, then the candidate property MUST be in this set.
	 * If the excludeSet is specified, then the candidate property MUST NOT be in this set.
	 * 
	 * Note there is generally little point in specifying both, but sometimes this may
	 * be useful; e.g. to configure a generic includeSet
	 * 
	 * @param edges
	 * @param overProperties 
	 */
	private void filterEdges(Set<OWLGraphEdge> edges) {
		 filterEdges(edges, null);
		
	}
	protected void filterEdges(Set<OWLGraphEdge> edges, Set<OWLPropertyExpression> overProperties) {
		Set<OWLGraphEdge> rmEdges = new OWLGraphEdgeSet();
		for (OWLGraphEdge e : edges) {
			if (overProperties != null) {
				if (e.getQuantifiedPropertyList().size() > 1) {
					// if a filter set is provided, do not yield any chains
					rmEdges.add(e);
					continue;					
				}
				OWLQuantifiedProperty qp = e.getSingleQuantifiedProperty();
				if (qp.isSomeValuesFrom() && !overProperties.contains(qp.getProperty())) {
					rmEdges.add(e);
					continue;
				}
			}
			if (isExcludeEdge(e)) {
				rmEdges.add(e);
			}
		}

		edges.removeAll(rmEdges);
	}

	public boolean isExcludeEdge(OWLGraphEdge edge) {

		if (config.graphEdgeExcludeSet != null ||
				config.graphEdgeIncludeSet != null) {
			for (OWLQuantifiedProperty qp : edge.getQuantifiedPropertyList()) {
				if (isExcluded(qp)) {
					LOG.debug("excluded:"+edge+" based on: "+qp);
					return true;
				}
			}
		}

		OWLObject t = edge.getTarget();
		if (t != null) {
			if (t instanceof OWLNamedObject) {
				OWLNamedObject nt = (OWLNamedObject) t;
				// TODO
				if (nt.getIRI().toString().startsWith("http://www.ifomis.org/bfo"))
					return true;
				if (t instanceof OWLClass && t.equals(getDataFactory().getOWLThing())) {
					return true;
				}

			}
		}
		return false;
	}

	// e.g. R-some-B ==> <R-some-B,R,B>
	private OWLGraphEdge restrictionToPrimitiveEdge(OWLRestriction<?,?,?> s) {
		OWLObjectPropertyExpression p = null;
		OWLObject t = null;
		OWLQuantifiedProperty.Quantifier q = null;
		if (s instanceof OWLObjectSomeValuesFrom) {
			t  = ((OWLObjectSomeValuesFrom)s).getFiller();
			p = (OWLObjectPropertyExpression) s.getProperty();
			q = OWLQuantifiedProperty.Quantifier.SOME;
		}
		else if (s instanceof OWLObjectAllValuesFrom) {
			t  = ((OWLObjectAllValuesFrom)s).getFiller();
			p = (OWLObjectPropertyExpression) s.getProperty();
			q = OWLQuantifiedProperty.Quantifier.ONLY;
		}
		else if (s instanceof OWLObjectHasValue) {
			t  = ((OWLObjectHasValue)s).getValue();
			p = (OWLObjectPropertyExpression) s.getProperty();
			q = OWLQuantifiedProperty.Quantifier.VALUE;
		}
		else if (s instanceof OWLObjectCardinalityRestriction) {
			OWLObjectCardinalityRestriction cardinalityRestriction = (OWLObjectCardinalityRestriction) s;
			if (cardinalityRestriction.getCardinality() > 0) {
				t = cardinalityRestriction.getFiller();
				p = cardinalityRestriction.getProperty();
				q = OWLQuantifiedProperty.Quantifier.SOME;
			}
			else {
				System.err.println("cannot handle negation:"+s);
			}
		}
		else {
			System.err.println("cannot handle:"+s);
		}
		//provide a null OWLOntology, the actual OWLOntology will be obtained from 
		//the first OWLGraphEdge created from the OWLAxiom leading to this OWLRestriction
		return new OWLGraphEdge(s,t,p,q, null);
	}

	private OWLGraphEdge createSubClassOfEdge(OWLObject s, OWLClassExpression t, 
	        OWLOntology ont) {
		return new OWLGraphEdge(s,t,null,Quantifier.SUBCLASS_OF, ont);
	}

    private OWLGraphEdge createSubClassOfEdge(OWLObject s, OWLClassExpression t, 
            OWLOntology ont, OWLAxiom underlyingAxiom) {
        return new OWLGraphEdge(s,t,null,Quantifier.SUBCLASS_OF, ont, underlyingAxiom);
    }


	// extend an edge target until we hit a named object.
	// this could involve multiple extensions and "forks", e.g.
	// <A sub B^C> ==> <A sub B>, <A sub C>
	// NOTE: may be renamed to 'unfoldEdgeTarget'
	protected Set<OWLGraphEdge> primitiveEdgeToFullEdges(OWLGraphEdge e) {
		Set<OWLGraphEdge> edges = new OWLGraphEdgeSet();
		if (e.isTargetNamedObject()) {
			edges.add(e); // do nothing
		}
		else {
			// extend
			OWLObject s = e.getSource();
			Set<OWLGraphEdge> nextEdges = getOutgoingEdges(e.getTarget());
			for (OWLGraphEdge e2 : nextEdges) {
				OWLGraphEdge nu = this.combineEdgePair(s, e, e2, 1);
				if (nu != null)
				    edges.add(nu);
			}
		}
		filterEdges(edges);
		return edges;
	}

	private Set<OWLGraphEdge> unfoldEdgeSource(OWLGraphEdge e) {
		Set<OWLGraphEdge> edges = new OWLGraphEdgeSet();
		if (e.isSourceNamedObject()) {
			edges.add(e); // do nothing
		}
		else {
			// extend
//			OWLObject t = e.getTarget();
			Set<OWLGraphEdge> nextEdges = getIncomingEdges(e.getSource());
			for (OWLGraphEdge e2 : nextEdges) {
				OWLGraphEdge nu = this.combineEdgePairDown(e, e2, 1);
				if (nu != null)
				    edges.add(nu);
			}
		}
		filterEdges(edges);
		return edges;
	}

	/**
	 * caches full outgoing and incoming edges
	 * <p>
	 * in general you should not need to call this directly;
	 * used internally by this class.
	 * 
	 * @see OWLGraphWrapperEdges#clearCachedEdges()
	 * @see OWLGraphWrapperEdges#getPrimitiveIncomingEdges(OWLObject)
	 * @see OWLGraphWrapperEdges#getPrimitiveOutgoingEdges(OWLObject)
	 * @see OWLGraphWrapperEdges#getEdgesBetween(OWLObject, OWLObject)
	 */
	public void cacheEdges() {
		synchronized (edgeCacheMutex) {
			edgeBySource = new HashMap<OWLObject,Set<OWLGraphEdge>>();
			edgeByTarget = new HashMap<OWLObject,Set<OWLGraphEdge>>();
	
			// initialize with all named objects in ontology
			Stack<OWLObject> allObjs = new Stack<OWLObject>();
			allObjs.addAll(getAllOWLObjects());
	
			Set<OWLObject> visisted = new HashSet<OWLObject>();
	
			while (allObjs.size() > 0) {
				OWLObject s = allObjs.pop();
				if (visisted.contains(s))
					continue;
				visisted.add(s);
				if (!edgeBySource.containsKey(s))
					edgeBySource.put(s, new OWLGraphEdgeSet());
				for (OWLGraphEdge edge : getPrimitiveOutgoingEdges(s)) {
				    edgeBySource.get(s).add(edge);
					OWLObject t = edge.getTarget();
					if (!edgeByTarget.containsKey(t))
						edgeByTarget.put(t, new OWLGraphEdgeSet());
					edgeByTarget.get(t).add(edge);
	
					// we also want to get all edges from class expressions;
					// class expressions aren't in the initial signature, but
					// we add them here when we encounter them
					if (t instanceof OWLClassExpression) {
						allObjs.add(t);
					}
				}
			}
		}
	}
	
	
	/**
	 *  Clear the current edge cache.
	 *  
	 *  @see OWLGraphWrapperEdges#cacheEdges()
	 */
	public void clearCachedEdges() {
		synchronized (edgeCacheMutex) {
			edgeBySource = null;
			edgeByTarget = null;
			inferredEdgeBySource = null;
			inferredEdgeByTarget = null;
			extraSubClassOfEdges = null;
		}
	}

	/**
	 * @param t
	 * @return all edges that have t as a direct target
	 */
	public Set<OWLGraphEdge> getIncomingEdges(OWLObject t) {
		ensureEdgesCached();
		if (edgeByTarget.containsKey(t)) {
			HashSet<OWLGraphEdge> edges = new OWLGraphEdgeSet();
			for (OWLGraphEdge e :edgeByTarget.get(t)) {
			    edges.addAll(this.unfoldEdgeSource(e));
			}
			return edges;
		}
		
		return new OWLGraphEdgeSet();
	}

	public Set<OWLGraphEdge> getPrimitiveIncomingEdges(OWLObject t) {
		ensureEdgesCached();
		if (edgeByTarget.containsKey(t)) {
			return new OWLGraphEdgeSet(edgeByTarget.get(t));
		}
		
		return new OWLGraphEdgeSet();
	}

	private void ensureEdgesCached() {
		boolean buildCache = false;
		synchronized (edgeCacheMutex) {
			buildCache = (edgeByTarget == null || edgeBySource == null);
		}
		if (buildCache) {
			cacheEdges();
		}

	}


	/**
	 * pack/translate an edge (either asserted or a graph closure edge) into
	 * an OWL class expression according to the OWLGraph to OWLOntology
	 * translation rules.
	 * <p>
	 * (this is the reverse translation of the one from an OWLOntology to an
	 * OWLGraph)
	 * <p>
	 * e.g. after calling for the graph closure of an OWLClass a,
	 * we may get back an edge &lt;a [part_of-some, adjacent_to-some, has_part-some] b&gt;.
	 * after feeding this edge into this method we obtain the expression
	 *   part_of some (adjacent_to some (has_part some b))
	 * 
	 * @param e edge
	 * @return class expression equivalent to edge
	 */
	public OWLObject edgeToTargetExpression(OWLGraphEdge e) {
		return edgeToTargetExpression(e.getQuantifiedPropertyList().iterator(),e.getTarget());
	}

	private OWLObject edgeToTargetExpression(
			Iterator<OWLQuantifiedProperty> qpi, OWLObject t) {
		OWLDataFactory dataFactory = getDataFactory();
		if (qpi.hasNext()) {
			OWLQuantifiedProperty qp = qpi.next();
			OWLObject x = edgeToTargetExpression(qpi,t);
			OWLClassExpression t2;
			if (!(x instanceof OWLClassExpression)) {
				//System.err.println("Not a CE: "+x);
				HashSet<OWLNamedIndividual> ins = new HashSet<OWLNamedIndividual>();
				ins.add((OWLNamedIndividual) x);
				t2 = dataFactory.getOWLObjectOneOf(ins);
			}
			else {
				t2 = (OWLClassExpression) x;
			}

			if (qp.isSubClassOf()) {
				return t2;
			}
			else if (qp.isInstanceOf()) {
				return t2;
			}
			else if (qp.isIdentity()) {
				return t2;
			}
			else if (qp.isPropertyAssertion()) {
				return dataFactory.getOWLObjectSomeValuesFrom(qp.getProperty(), 
						(OWLClassExpression) t2);
			}
			else if (qp.isSomeValuesFrom()) {
				return dataFactory.getOWLObjectSomeValuesFrom(qp.getProperty(), 
						(OWLClassExpression) t2);
			}
			else if (qp.isAllValuesFrom()) {
				return dataFactory.getOWLObjectAllValuesFrom(qp.getProperty(), 
						(OWLClassExpression) t2);
			}
			else if (qp.isHasValue()) {
				if (x instanceof OWLNamedObject)
					return dataFactory.getOWLObjectHasValue(qp.getProperty(), 
							dataFactory.getOWLNamedIndividual(((OWLNamedObject) x).getIRI()));
				else {
					System.err.println("warning: treating "+x+" as allvaluesfrom");
					return dataFactory.getOWLObjectAllValuesFrom(qp.getProperty(), 
							(OWLClassExpression) x);
				}
			}
			else {
				System.err.println("cannot handle:"+qp);
				// TODO
				return null;
			}
		}
		else {
			return t;
		}
	}


	// ----------------------------------------
	// GRAPH CLOSURE METHODS
	// ----------------------------------------


	/**
	 * Retrieves the graph closure originating from source.
	 * E.g. if A SubClassOf R some B &amp; B SubClassOf S some C, then
	 * closure(A) = { &lt;A R-some B&gt;, &lt;A [R-some,S-some] C&gt;}.
	 * <p>
	 * Composition rules are used to compact the list of connecting edge labels
	 * (e.g. transitivity).
	 * <p>
	 * The resulting edges can be translated into class expressions using 
	 * method edgeToTargetExpression(e). E.g. in the above the expression would be
	 *   R some (S some C)
	 * 
	 * @param s source
	 * @return closure of edges originating from source
	 */
	public Set<OWLGraphEdge> getOutgoingEdgesClosure(OWLObject s) {
		return getOutgoingEdgesClosure(s, null);
	}
	
	/**
	 * As {@link getOutgoingEdgesClosure(OWLObject s)}, but only consider the specified
	 * set of properties when walking the graph.
	 * 
	 * Advanced usage notice: note that if the desired set of properties is {P},
	 * and there exists a property chain Q o R --> P, then be sure to include Q and R in
	 * the specified set
	 * 
	 * @param s
	 * @param overProperties
	 * @return
	 */
	public Set<OWLGraphEdge> getOutgoingEdgesClosure(OWLObject s, Set<OWLPropertyExpression> overProperties) {
		synchronized (edgeCacheMutex) {
			// never use cache if a property list is specified (in future we may have one
			// cache per property set)
			if (config.isCacheClosure && overProperties == null) {
				if (inferredEdgeBySource == null)
					inferredEdgeBySource = new HashMap<OWLObject,Set<OWLGraphEdge>>();
				if (inferredEdgeBySource.containsKey(s)) {
					return new OWLGraphEdgeSet(inferredEdgeBySource.get(s));
				}
			}
			profiler.startTaskNotify("getOutgoingEdgesClosure");
	
			Stack<OWLGraphEdge> edgeStack = new Stack<OWLGraphEdge>();
			Set<OWLGraphEdge> closureSet = new OWLGraphEdgeSet();
			//Set<OWLGraphEdge> visitedSet = new HashSet<OWLGraphEdge>();
			Set<OWLObject> visitedObjs = new HashSet<OWLObject>();
			Map<OWLObject,Set<OWLGraphEdge>> visitedMap = new HashMap<OWLObject,Set<OWLGraphEdge>>();
			visitedObjs.add(s);
			visitedMap.put(s, new OWLGraphEdgeSet());
	
			// initialize. we seed the search with a reflexive identity edge DEPR
			//edgeStack.add(new OWLGraphEdge(s,s,null,Quantifier.IDENTITY,ontology));
	
			// seed stack
			edgeStack.addAll(getPrimitiveOutgoingEdges(s, overProperties));
			closureSet.addAll(edgeStack);
			while (!edgeStack.isEmpty()) {
				OWLGraphEdge ne = edgeStack.pop();
				//System.out.println("NEXT: "+ne+" //stack: "+edgeStack);
				int nextDist = ne.getDistance() + 1;
				Set<OWLGraphEdge> extSet = getPrimitiveOutgoingEdges(ne.getTarget(), overProperties);
				for (OWLGraphEdge extEdge : extSet) {
					//System.out.println("   EXT:"+extEdge);
					OWLGraphEdge nu = combineEdgePair(s, ne, extEdge, nextDist);
					if (nu == null)
						continue;
					//if (!isKeepEdge(nu))
					//	continue;
	
					OWLObject nuTarget = nu.getTarget();
					//System.out.println("     COMBINED:"+nu);
	
					// check for cycles. this is not as simple as
					// checking if we have visited the node, as we are interested
					// in different paths to the same node.
					// todo - check if there is an existing path to this node
					//  that is shorter
					//if (!visitedSet.contains(nu)) {
					boolean isEdgeVisited = false;
					if (visitedObjs.contains(nuTarget)) {
						// we have potentially visited this edge before
						//System.out.println("checking to see if  visisted "+nu);
						//System.out.println(nu.getFinalQuantifiedProperty());
						for (OWLGraphEdge ve : visitedMap.get(nuTarget)) {
							//System.out.println(" ve:"+ve.getFinalQuantifiedProperty());
							if (ve.getFinalQuantifiedProperty().equals(nu.getFinalQuantifiedProperty())) {
								//System.out.println("already visited: "+nu+" via: "+ve);
								isEdgeVisited = true;
							}
						}
						if (!isEdgeVisited) {
						    visitedMap.get(nuTarget).add(nu);
						}
					}
					else {
						visitedObjs.add(nuTarget);
						visitedMap.put(nuTarget, new OWLGraphEdgeSet());
						visitedMap.get(nuTarget).add(nu);
					}
	
					if (!isEdgeVisited) {
						//System.out.println("      *NOT VISITED:"+nu+" visistedSize:"+visitedSet.size());
						if (nu.getTarget() instanceof OWLNamedObject || 
								config.isIncludeClassExpressionsInClosure) {
						    closureSet.add(nu);
						}
						edgeStack.add(nu);
						//visitedSet.add(nu);		
	
					}
	
				}
			}
	
			if (config.isCacheClosure && overProperties == null) {
				inferredEdgeBySource.put(s, new OWLGraphEdgeSet(closureSet));
			}
			profiler.endTaskNotify("getOutgoingEdgesClosure");
			return closureSet;
		}
	}

	/**
	 * as {@link #getOutgoingEdgesClosure(OWLObject)}, but also includes an identity edge
	 * @param s
	 * @return set of {@link OWLGraphEdge}
	 */
	public Set<OWLGraphEdge> getOutgoingEdgesClosureReflexive(OWLObject s) {
		Set<OWLGraphEdge> edges = getOutgoingEdgesClosure(s);
		edges.add(new OWLGraphEdge(s,s,null,Quantifier.IDENTITY,getSourceOntology()));
		return edges;
	}
	
	/**
	 * as {@link #getOutgoingEdgesClosure(OWLObject, Set)}, but also include an identify edge
	 * @param s
	 * @param props
	 * @return
	 */
	public Set<OWLGraphEdge> getOutgoingEdgesClosureReflexive(OWLObject s, Set<OWLPropertyExpression> props) {
		Set<OWLGraphEdge> edges = getOutgoingEdgesClosure(s, props);
		edges.add(new OWLGraphEdge(s,s,null,Quantifier.IDENTITY,getSourceOntology()));
		return edges;
	}


	/**
	 * find the set of classes or class expressions subsuming source, using the graph closure.
	 * <p>
	 * this is just the composition of getOutgoingEdgesClosure and edgeToTargetExpression -- the
	 * latter method "packs" a chain of edges into a class expression
	 * <p>
	 * only "linear" expressions are found, corresponding to a path in the graph.
	 * e.g. [sub,part_of-some,develops_from-some] ==&gt; part_of some (develops_from some t)
	 * <p>
	 * if the edge consists entirely of subclass links, the the subsumers will be all
	 * named classes.
	 * 
	 * @param s source
	 * @return set of {@link OWLObject}
	 */
	public Set<OWLObject> getSubsumersFromClosure(OWLObject s) {
		Set<OWLObject> ts = new HashSet<OWLObject>();
		for (OWLGraphEdge e : getOutgoingEdgesClosure(s)) {
			for (OWLGraphEdge se : getOWLGraphEdgeSubsumers(e)) {
				ts.add(edgeToTargetExpression(se));
			}
			ts.add(edgeToTargetExpression(e));
		}
		return ts;
	}

	/**
	 * See {@link #getIncomingEdgesClosure(OWLObject s, boolean isComplete)}
	 * 
	 * @param s
	 * @param isComplete
	 * @return set of edges
	 */
	public Set<OWLGraphEdge> getOutgoingEdgesClosure(OWLObject s, boolean isComplete) {
		if (isComplete) {
			Set<OWLGraphEdge> edges = new OWLGraphEdgeSet();
			for (OWLGraphEdge e : getOutgoingEdgesClosure(s)) {
			    edges.addAll(getOWLGraphEdgeSubsumers(e));
			}
			return edges;
		}
		else {
			return getOutgoingEdgesClosure(s);
		}
	}

	/**
	 * See {@link #getIncomingEdgesClosure(OWLObject s, boolean isComplete)}
	 * 
	 * @param s
	 * @return set of edges, never null
	 */
	public Set<OWLGraphEdge> getCompleteOutgoingEdgesClosure(OWLObject s) {
		Set<OWLGraphEdge> edges = new OWLGraphEdgeSet();
		for (OWLGraphEdge e : getOutgoingEdgesClosure(s)) {
		    edges.addAll(getOWLGraphEdgeSubsumers(e));
		}
		return edges;
	}


	/**
	 * Treats an edge as a path and performs a query.
	 * <p>
	 * E.g &lt;x [R SOME] [S SOME] y&gt; will be treated as the class expression
	 *    R SOME (S SOME y)
	 * @param e
	 * @return set of {@link OWLObject}, never null
	 */
	public Set<OWLObject> queryDescendants(OWLGraphEdge e) {
		profiler.startTaskNotify("queryDescendants");
		Set<OWLObject> results = new HashSet<OWLObject>();
		// reflexivity
		results.add(this.edgeToTargetExpression(e));
		List<OWLQuantifiedProperty> eqpl = e.getQuantifiedPropertyList();

		// first find all subclasses of target (todo - optimize)
		for (OWLObject d1 : queryDescendants((OWLClassExpression)e.getTarget())) {
			//LOG.info("  Q="+d1);
			Set<OWLGraphEdge> dEdges = this.getIncomingEdgesClosure(d1, true);
			for (OWLGraphEdge dEdge : dEdges) {
				List<OWLQuantifiedProperty> dqpl = new Vector<OWLQuantifiedProperty>(dEdge.getQuantifiedPropertyList());

				if (dqpl.get(0).isInstanceOf()) {
					// the graph path from an individual will start with either
					// an instance-of QP, or a property assertion.
					// we ignore the instance-of here, as the query is implicitly for individuals
					// and classes
					dqpl.remove(dqpl.get(0));
				}				

				if (dqpl.equals(eqpl)) {
					results.add(dEdge.getSource());
				}
			}
		}
		profiler.endTaskNotify("queryDescendants");
		return results;
	}

	/**
	 * Performs a closed-world query using a DL expression as a set of boolean database-style constraints.
	 * <p>
	 * No attempt is made to optimize the query. The engine is incomplete and currently ontology implements
	 * queries for constructs that use AND, OR, SOME
	 * 
	 * @param t classExpression
	 * @return set of descendants
	 */
	public Set<OWLObject> queryDescendants(OWLClassExpression t) {
		return queryDescendants(t, true, true);
	}

	public Set<OWLObject> queryDescendants(OWLClassExpression t, boolean isInstances, boolean isClasses) {
		Set<OWLObject> results = new HashSet<OWLObject>();
		results.add(t);

		// transitivity and link composition
		Set<OWLGraphEdge> dEdges = this.getIncomingEdgesClosure(t, true);
		for (OWLGraphEdge dEdge : dEdges) {
			if (dEdge.getQuantifiedPropertyList().size() > 1)
				continue;
			OWLQuantifiedProperty qp = dEdge.getSingleQuantifiedProperty();
			if ((isInstances && qp.isInstanceOf()) || 
					(isClasses && qp.isSubClassOf()))
				results.add(dEdge.getSource());
		}

		if (t instanceof OWLObjectIntersectionOf) {
			Set<OWLObject> iresults = null;
			for (OWLClassExpression y : ((OWLObjectIntersectionOf)t).getOperands()) {
				if (iresults == null) {
					iresults = queryDescendants(y, isInstances, isClasses);
				}
				else {
					if (y instanceof OWLObjectComplementOf) {
						// mini-optimization: 
						// for "A and not B and ...", perform B and remove results from A.
						//
						// assumes the NOT precedes the initial operand, and is preferably
						// as far to the end as possible.
						// this could be easily improved upon, but this functionality
						// will eventually be subsumed by reasoners in any case...
						OWLClassExpression z = ((OWLObjectComplementOf) y).getOperand();
						iresults.removeAll(queryDescendants(z, isInstances, isClasses));
					}
					else {
						iresults.retainAll(queryDescendants(y, isInstances, isClasses));
					}
				}
			}
			results.addAll(iresults);
		}
		else if (t instanceof OWLObjectUnionOf) {
			for (OWLClassExpression y : ((OWLObjectUnionOf)t).getOperands()) {
				results.addAll(queryDescendants(y, isInstances, isClasses));
			}
		}
		else if (t instanceof OWLRestriction) {
			results.addAll(queryDescendants(restrictionToPrimitiveEdge((OWLRestriction) t)));
		}
		else if (t instanceof OWLObjectComplementOf) {
			// NOTE: this is closed-world negation
			// TODO: optimize by re-ordering clauses
			for (OWLOntology o : getAllOntologies()) {
				results.addAll(o.getClassesInSignature(true));
			}
			results.removeAll(queryDescendants( ((OWLObjectComplementOf) t).getOperand()));
		}
		// equivalent classes - substitute a named class in the query for an expression
		else if (t instanceof OWLClass) {
			for (OWLOntology ont : this.getAllOntologies()) {
				for (OWLEquivalentClassesAxiom ax : ont.getEquivalentClassesAxioms((OWLClass)t)) {
					for (OWLClassExpression y : ax.getClassExpressions()) {
						if (y instanceof OWLClass)
							continue;
						results.addAll(queryDescendants(y, isInstances, isClasses));
					}
				}
			}
		}
		else {
			LOG.error("Cannot handle:"+t);
		}

		return results;
	}

	/**
	 * @param s source
	 * @param t target
	 * @return all edges connecting source and target in the graph closure
	 */

	public Set<OWLGraphEdge> getEdgesBetween(OWLObject s, OWLObject t) {
		Set<OWLGraphEdge> allEdges = getOutgoingEdgesClosureReflexive(s);
		Set<OWLGraphEdge> edges = new OWLGraphEdgeSet();
		for (OWLGraphEdge e : allEdges) {
			if (e.getTarget().equals(t))
				edges.add(e);
		}
		return edges;
	}

	public Set<OWLGraphEdge> getCompleteEdgesBetween(OWLObject s, OWLObject t) {
		Set<OWLGraphEdge> edges = new OWLGraphEdgeSet();
		for (OWLGraphEdge e : getEdgesBetween(s,t)) {
			edges.add(e);
			for (OWLGraphEdge se : this.getOWLGraphEdgeSubsumers(e)) 
			    edges.add(se);
		}
		return edges;
	}

	/**
	 * returns all ancestors of an object. Here, ancestors is defined as any
	 * named object that can be reached from x over some path of asserted edges.
	 * relations are ignored.
	 * 
	 * @param x source
	 * @return all reachable target nodes, regardless of edges
	 */
	public Set<OWLObject> getAncestors(OWLObject x) {
		Set<OWLObject> ancs = new HashSet<OWLObject>();
		for (OWLGraphEdge e : getOutgoingEdgesClosure(x)) {
			ancs.add(e.getTarget());
		}
		return ancs;
	}

	/**
	 * returns all ancestors that can be reached over subclass or
	 * the specified set of relations
	 * 
	 * @param x the sourceObject
	 * @param overProps
	 * @return set of ancestors
	 */
	public Set<OWLObject> getAncestors(OWLObject x, Set<OWLPropertyExpression> overProps) {
		return getAncestors(x, overProps, false);
	}
	
	/**
	 * As {@link getAncestors(OWLObject s, Set<OWLProperty) overProps}, 
	 * but if isStrict is true, then only consider paths that include at least one edge
	 * with a property in the specified set. i.e. exclude subclass-only paths. 
	 * 
	 * @param s
	 * @param overProperties
	 * @return
	 */

	public Set<OWLObject> getAncestors(OWLObject x, Set<OWLPropertyExpression> overProps, boolean isStrict) {
		Set<OWLObject> ancs = new HashSet<OWLObject>();
		for (OWLGraphEdge e : getOutgoingEdgesClosure(x, overProps)) {
			boolean isAddMe = false;
			if (overProps != null) {
				List<OWLQuantifiedProperty> qps = e.getQuantifiedPropertyList();
				if (qps.size() == 0) {
					// identity
					if (!isStrict)
						isAddMe = true;
				}
				else if (qps.size() == 1) {
					OWLQuantifiedProperty qp = qps.get(0);
					if (qp.isIdentity()) {
						if (!isStrict)
							isAddMe = true;
					}
					else if (qp.isSubClassOf()) {
						if (!isStrict)
							isAddMe = true;
					}
					else if (qp.isSomeValuesFrom() && overProps.contains(qp.getProperty())) {
						isAddMe = true;
					}
				}
				else if (!isStrict) {
				    isAddMe = true;
				}
			}
			else {
				isAddMe = true;
			}
			if (isAddMe)
				ancs.add(e.getTarget());
		}
		return ancs;
	}

	public Set<OWLObject> getAncestorsReflexive(OWLObject x) {
		Set<OWLObject> ancs = new HashSet<OWLObject>(getAncestors(x));
		ancs.add(x);
		return ancs;
	}
	public Set<OWLObject> getAncestorsReflexive(OWLObject x, Set<OWLPropertyExpression> overProps) {
		Set<OWLObject> ancs = new HashSet<OWLObject>(getAncestors(x, overProps));
		ancs.add(x);
		return ancs;
	}

	/**
	 * Gets all ancestors that are OWLNamedObjects
	 * <p>
	 * i.e. excludes anonymous class expressions
	 * 
	 * @param x
	 * @return set of named ancestors
	 */
	public Set<OWLObject> getNamedAncestors(OWLObject x) {
		Set<OWLObject> ancs = new HashSet<OWLObject>();
		for (OWLGraphEdge e : getOutgoingEdgesClosure(x)) {
			if (e.getTarget() instanceof OWLNamedObject)
				ancs.add(e.getTarget());
		}
		return ancs;
	}
	public Set<OWLObject> getNamedAncestorsReflexive(OWLObject x) {
		Set<OWLObject> ancs = getNamedAncestors(x);
		ancs.add(x);
		return ancs;
	}

	/**
	 * Get the human readable label for an edge.
	 * Intended for use for things like the GO.
	 * 
	 * @param e
	 * @return either the human readable edge label or null if none could be found
	 */
	public String getEdgeLabel(OWLGraphEdge e) {
		String retstr = null;

		// Figure edge out.
		OWLQuantifiedProperty sprop= e.getSingleQuantifiedProperty();
		if( sprop.isSubClassOf() ){
			retstr = "is_a";
		}else if( sprop.isSomeValuesFrom() ){
			OWLObjectProperty oprop = sprop.getProperty();
			String prop_label = getLabel(oprop);
			if( prop_label != null && ! prop_label.equals("") )
				retstr = prop_label;
		}else{
			// Not a relation in the sense that we want.
		}
		
		return retstr;
	}

	/**
	 * gets all descendants d of x, where d is reachable by any path. Excludes self
	 * 
	 * @see #getAncestors
	 * @see owltools.graph
	 * @param x
	 * @return descendant objects
	 */
	public Set<OWLObject> getDescendants(OWLObject x) {
		Set<OWLObject> descs = new HashSet<OWLObject>();
		for (OWLGraphEdge e : getIncomingEdgesClosure(x)) {
			descs.add(e.getSource());
		}
		return descs;
	}
	/**
	 * gets all reflexive descendants d of x, where d is reachable by any path. Includes self
	 * 
	 * @see #getAncestors
	 * @see owltools.graph
	 * @param x
	 * @return descendant objects plus x
	 */
	public Set<OWLObject> getDescendantsReflexive(OWLObject x) {
		Set<OWLObject> getDescendants = getDescendants(x);
		getDescendants.add(x);
		return getDescendants;
	}

	/**
	 * return all individuals i where x is reachable from i
	 * @param x
	 * @return set of individual {@link OWLObject}s
	 */
	public Set<OWLObject> getIndividualDescendants(OWLObject x) {
		Set<OWLObject> descs = new HashSet<OWLObject>();
		for (OWLGraphEdge e : getIncomingEdgesClosure(x)) {
			OWLObject s = e.getSource();
			if (s instanceof OWLIndividual)
				descs.add(s);
		}
		return descs;
	}



	/**
	 * As {@link #getIncomingEdgesClosure(OWLObject t)}, but allows the option of including
	 * 'complete' edge list. A complete edge list also includes redundant subsuming paths. E.g
	 * <p>
	 * if there is a path &lt;x [R some] [S some] y&gt;
	 * and R' and S' are super-properties of R and S, then there will also be a path
	 * &lt;x [R' some] [S' some] y&gt;
	 * <p>
	 * The default is false, i.e. if the more specific path exists, only it will be returned
	 * 
	 * 
	 * @param t
	 * @param isComplete
	 * @return set of edges
	 */
	public Set<OWLGraphEdge> getIncomingEdgesClosure(OWLObject t, boolean isComplete) {
		if (isComplete) {
			Set<OWLGraphEdge> ccs = new OWLGraphEdgeSet();
			for (OWLGraphEdge e : getIncomingEdgesClosure(t)) {
			    ccs.addAll(getOWLGraphEdgeSubsumers(e));
			}
			return ccs;
		}
		else {
			return getIncomingEdgesClosure(t);
		}
	}

	/**
	 * gets all inferred edges coming in to the target edge
	 * <p>
	 * for every s, if t is reachable from s, then include the inferred edge between s and t.
	 * 
	 * @see #getOutgoingEdgesClosure
	 * @param t target
	 * @return all edges connecting all descendants of target to target
	 */
	public Set<OWLGraphEdge> getIncomingEdgesClosure(OWLObject t) {
		synchronized (edgeCacheMutex) {
			if (config.isCacheClosure) {
				if (inferredEdgeByTarget == null)
					inferredEdgeByTarget = new HashMap<OWLObject,Set<OWLGraphEdge>>();
				if (inferredEdgeByTarget.containsKey(t)) {
					return new OWLGraphEdgeSet(inferredEdgeByTarget.get(t));
				}
			}
			profiler.startTaskNotify("getIncomingEdgesClosure");
	
			Stack<OWLGraphEdge> edgeStack = new Stack<OWLGraphEdge>();
			Set<OWLGraphEdge> closureSet = new OWLGraphEdgeSet();
			//Set<OWLGraphEdge> visitedSet = new HashSet<OWLGraphEdge>();
			Set<OWLObject> visitedObjs = new HashSet<OWLObject>();
			Map<OWLObject,Set<OWLGraphEdge>> visitedMap = new HashMap<OWLObject,Set<OWLGraphEdge>>();
			visitedObjs.add(t);
			visitedMap.put(t, new OWLGraphEdgeSet());
	
			// initialize -
			// note that edges are always from src to tgt. here we are extending down from tgt to src
	
			//edgeStack.add(new OWLGraphEdge(t,t,ontology,new OWLQuantifiedProperty()));
			edgeStack.addAll(getPrimitiveIncomingEdges(t));
			closureSet.addAll(edgeStack);
	
			while (!edgeStack.isEmpty()) {
				OWLGraphEdge ne = edgeStack.pop();
	
				int nextDist = ne.getDistance() + 1;
	
				// extend down from this edge; e.g. [s, extEdge + ne, tgt] 
				Set<OWLGraphEdge> extSet = getPrimitiveIncomingEdges(ne.getSource());
				for (OWLGraphEdge extEdge : extSet) {
	
					// extEdge o ne --> nu
					//OWLGraphEdge nu = combineEdgePairDown(ne, extEdge, nextDist);
					OWLGraphEdge nu = combineEdgePair(extEdge.getSource(), extEdge, ne, nextDist);
					if (nu == null)
						continue;
	
					// TODO - no longer required?
					//if (!isKeepEdge(nu))
					//	continue;
	
					OWLObject nusource = nu.getSource();
	
					boolean isEdgeVisited = false;
					if (visitedObjs.contains(nusource)) {
						//isEdgeVisited = true;
						for (OWLGraphEdge ve : visitedMap.get(nusource)) {
							//System.out.println(" ve:"+ve.getFinalQuantifiedProperty());
							if (ve.getFirstQuantifiedProperty().equals(nu.getFirstQuantifiedProperty())) {
								//System.out.println("already visited: "+nu);
								// always favor the shorter path
								if (ve.getQuantifiedPropertyList().size() <= nu.getQuantifiedPropertyList().size()) {
									isEdgeVisited = true;
								}
							}
						}
						if (!isEdgeVisited) {
						    visitedMap.get(nusource).add(nu);
						}
	
					}
					else {
						visitedObjs.add(nusource);
						visitedMap.put(nusource, new OWLGraphEdgeSet());
						visitedMap.get(nusource).add(nu);
					}
	
					if (!isEdgeVisited) {
						if (nu.getSource() instanceof OWLNamedObject || 
								config.isIncludeClassExpressionsInClosure) {
						    closureSet.add(nu);
						}
						edgeStack.add(nu);
						//visitedSet.add(nu);		
	
					}
	
				}
			}
	
			if (config.isCacheClosure) {
				inferredEdgeByTarget.put(t, new OWLGraphEdgeSet(closureSet));
			}
	
	
	
			profiler.endTaskNotify("getIncomingEdgesClosure");
			return closureSet;
		}
	}

	/**
	 * Composes two graph edges into a new edge, using axioms in the ontology to determine the correct composition
	 * <p>
	 * For example,  Edge(x,SUBCLASS_OF,y) * Edge(y,SUBCLASS_OF,z) yields Edge(x,SUBCLASS_OF,z)
	 * <p>
	 * Note that property chains of length>2 are currently ignored
	 * 
	 * @param s - source node
	 * @param ne - edge 1
	 * @param extEdge - edge 2
	 * @param nextDist - new distance
	 * @return edge
	 */
	public OWLGraphEdge combineEdgePair(OWLObject s, OWLGraphEdge ne, OWLGraphEdge extEdge, int nextDist) {
		//System.out.println("combing edges: "+s+" // "+ne+ " * "+extEdge);
		// Create an edge with no edge labels; we will fill the label in later.
	    OWLGraphEdge mergedEdge = this.createMergedEdge(s, ne, extEdge);
	    if (mergedEdge == null) 
	        return null;
	    if (this.combineEdgePair(mergedEdge, 
	            ne.getQuantifiedPropertyList(), extEdge.getQuantifiedPropertyList(), 
	            nextDist)) {
	        return mergedEdge;
	    }
	    return null;
	}
	
	/**
	 * Combine the {@code OWLQuantifiedProperty} {@code List}s {@code neQp} and 
	 * {@code extEdgeQp} and assigned the resulting {@code OWLQuantifiedProperty}s 
	 * to {@code mergedEdge}. If the {@code OWLQuantifiedProperty}s could be 
	 * properly combined, {@code mergedEdge} will be modified as a result 
	 * of the call to this method, and the method will return {@code true}. 
	 * If the {@code OWLQuantifiedProperty}s could not be combined, 
	 * this method returns {@code false}, and {@code mergedEdge} is not be modified.
	 * <p>
	 * This method is notably used by {@link #combineEdgePair(OWLObject, OWLGraphEdge, 
	 * OWLGraphEdge, int)}, and was created so that this part of the logic could be used 
	 * on extending Java classes. 
	 * 
	 * @param mergedEdge   An {@code OWLGraphEdge} whose {@code OWLQuantifiedProperty}s 
     *                     will be defined by merging {@code neQp} and {@code extEdgeQp}.
     * @param neQp         A {@code List} of {@code OWLQuantifiedProperty}s to be combined 
     *                     with {@code extEdgeQp}.
     * @param extEdgeQp    A {@code List} of {@code OWLQuantifiedProperty}s to be combined 
     *                     with {@code neQp}.
     * @param nextDist     The distance to assign to {@code mergedEdge} by calling 
     *                     {@code OWLGraphEdge#setDistance(int)}.
	 * @return             {@code true} if {@code neQp} and {@code extEdgeQp} could be combined 
	 *                     and {@code mergedEdge} modified as a result.
	 */
	protected boolean combineEdgePair(OWLGraphEdge mergedEdge, List<OWLQuantifiedProperty> neQp, 
	        List<OWLQuantifiedProperty>  extEdgeQp, int nextDist) {
        if (mergedEdge == null)
            return false;
        List<OWLQuantifiedProperty> qpl1 = new Vector<OWLQuantifiedProperty>(neQp);
        List<OWLQuantifiedProperty> qpl2 = new Vector<OWLQuantifiedProperty>(extEdgeQp);

        while (qpl1.size() > 0 && qpl2.size() > 0) {
            OWLQuantifiedProperty combinedQP = combinedQuantifiedPropertyPair(qpl1.get(qpl1.size()-1),qpl2.get(0));
            if (combinedQP == null)
                break;
            if (isExcluded(combinedQP)) {
                return false;
            }
            qpl1.set(qpl1.size()-1, combinedQP);
            if (combinedQP.isIdentity())
                qpl1.subList(qpl1.size()-1,qpl1.size()).clear();
            qpl2.subList(0, 1).clear();
        }
        qpl1.addAll(qpl2);
        mergedEdge.setQuantifiedPropertyList(qpl1);
        mergedEdge.setDistance(nextDist);
        return true;
    }

	/**
	 *  combine [srcEdge + tgtEdge]
	 *  <p>
	 *  srcEdge o tgtEdge --&gt; returned edge
	 *  
	 * @see #combineEdgePair(OWLObject s, OWLGraphEdge ne, OWLGraphEdge extEdge, int nextDist) 
	 * @param tgtEdge
	 * @param srcEdge
	 * @param nextDist
	 * @return edge
	 */
	private OWLGraphEdge combineEdgePairDown(OWLGraphEdge tgtEdge, OWLGraphEdge srcEdge, int nextDist) {
		// fill in edge label later
		// todo
	    OWLGraphEdge nu = this.createMergedEdge(srcEdge.getSource(), srcEdge, tgtEdge);
	    if (nu == null)
	        return null;
		nu.setDistance(nextDist);
		Vector<OWLQuantifiedProperty> qps = new Vector<OWLQuantifiedProperty>();

		// put all but the final one in a new list
		int n = 0;
//		int size = tgtEdge.getQuantifiedPropertyList().size();
		OWLQuantifiedProperty finalQP = null;
		for (OWLQuantifiedProperty qp : tgtEdge.getQuantifiedPropertyList()) {
			n++;
			if (n > 1)
				qps.add(qp);
			else
				finalQP = qp;
		}
		// TODO
		// join src+tgt edge
		OWLQuantifiedProperty combinedQP = 
			combinedQuantifiedPropertyPair(srcEdge.getFinalQuantifiedProperty(), tgtEdge.getSingleQuantifiedProperty());
		//combinedQuantifiedPropertyPair(tgtEdge.getFinalQuantifiedProperty(), srcEdge.getSingleQuantifiedProperty());
		if (combinedQP == null) {
			qps.add(finalQP);
			qps.add(srcEdge.getSingleQuantifiedProperty());
		}
		else {
			qps.add(combinedQP);
		}
		nu.setQuantifiedPropertyList(qps);
		return nu;
	}
	
	/**
	 * Create a new {@code OWLGraphEdge} going from {@code source}, to the target of 
	 * {@code targetEdge}, by merging the underling {@code OWLAxiom}s of {@code sourceEdge} 
	 * and {@code targetEdge} (as returned by {@link OWLGraphEdge#getAxioms()}), 
	 * and setting the {@code OWLOntology} of this new edge with the one of either 
	 * {@code sourceEdge} or {@code targetEdge}, as well as their gci_filler and gi_relation.
	 * {@code OWLQuantifiedProperty}s are not set.
	 * 
	 * @param source       The {@code OWLObject} which this new edge will originate from.
	 * @param sourceEdge   The {@code OWLGraphEdge} to merge with {@code targetEdge}.
	 * @param targetEdge   The {@code OWLGraphEdge} going to the target of the new edge 
	 *                     created, and to be merged with {@code sourceEdge}.
	 * @return             A newly created {@code OWLGraphEdge}, 
	 *                     with no {@code OWLQuantifiedProperty} set.
	 */
	private OWLGraphEdge createMergedEdge(OWLObject source, OWLGraphEdge sourceEdge, 
	        OWLGraphEdge targetEdge) {
	    //if sourceEdge and targetEdge have different gci_filler 
	    //or gci_relation, cannot be combined. For a combination using relations 
	    //between OWLClasses for fillers and between OWLObjectProperties for gci relations, 
	    //see OWLGraphEdgesExtended#createMergedEdgeWithGCI
	    if (sourceEdge.isGCI() && targetEdge.isGCI() && !sourceEdge.equalsGCI(targetEdge)) {
	        return null;
	    }
	    //merges the underlying axioms of these edges
        Set<OWLAxiom> axioms = new HashSet<OWLAxiom>(sourceEdge.getAxioms());
        axioms.addAll(targetEdge.getAxioms());
        return new OWLGraphEdge(source, targetEdge.getTarget(), 
                (sourceEdge.getOntology() != null ? 
                        sourceEdge.getOntology() : targetEdge.getOntology()), 
                axioms, 
                (sourceEdge.isGCI() ? 
                        sourceEdge.getGCIFiller() : targetEdge.getGCIFiller()), 
                (sourceEdge.isGCI() ? 
                        sourceEdge.getGCIRelation() : targetEdge.getGCIRelation()));
	}

	/**
	 * Edge composition rules
	 * 
	 * TODO - property chains of length > 2
	 * @param x 
	 * @param y 
	 * @return property or null
	 */
	protected OWLQuantifiedProperty combinedQuantifiedPropertyPair(OWLQuantifiedProperty x, OWLQuantifiedProperty y) {

		if (x.isSubClassOf() && y.isSubClassOf()) { // TRANSITIVITY OF SUBCLASS
			return new OWLQuantifiedProperty(Quantifier.SUBCLASS_OF);
		}
		else if (x.isInstanceOf() && y.isSubClassOf()) { // INSTANCE OF CLASS IS INSTANCE OF SUPERCLASS
			return new OWLQuantifiedProperty(Quantifier.INSTANCE_OF);
		}
		else if (x.isSubClassOf() && y.isSomeValuesFrom()) { // TRANSITIVITY OF SUBCLASS: existentials
			return new OWLQuantifiedProperty(y.getProperty(),Quantifier.SOME);
		}
		else if (x.isSomeValuesFrom() && y.isSubClassOf()) { // TRANSITIVITY OF SUBCLASS: existentials
			return new OWLQuantifiedProperty(x.getProperty(),Quantifier.SOME);
		}
		else if (x.isSubClassOf() && y.isAllValuesFrom()) {
			return new OWLQuantifiedProperty(y.getProperty(),Quantifier.ONLY);
		}
		else if (x.isAllValuesFrom() && y.isSubClassOf()) {
			return new OWLQuantifiedProperty(x.getProperty(),Quantifier.ONLY);
		}
		else if (x.isSomeValuesFrom() &&
				y.isSomeValuesFrom() &&
				x.getProperty() != null && 
				x.getProperty().equals(y.getProperty()) && 
				x.getProperty().isTransitive(this.getAllOntologies())) {//RO is often imported, 
		                                                                //doesn't make sense to check only the source ontology
			return new OWLQuantifiedProperty(x.getProperty(),Quantifier.SOME);
		}
		else if (x.isSomeValuesFrom() &&
				y.isSomeValuesFrom() &&
				chain(x.getProperty(), y.getProperty()) != null) { // TODO: length>2
			return new OWLQuantifiedProperty(chain(x.getProperty(), y.getProperty()),Quantifier.SOME);
		}
		else if (x.isPropertyAssertion() &&
				y.isPropertyAssertion() &&
				x.getProperty() != null && 
				x.getProperty().equals(y.getProperty()) && 
				x.getProperty().isTransitive(sourceOntology)) { // todo
			return new OWLQuantifiedProperty(x.getProperty(),Quantifier.PROPERTY_ASSERTION);
		}
		else if (x.isPropertyAssertion() &&
				y.isPropertyAssertion() &&
				x.getProperty() != null && 
				isInverseOfPair(x.getProperty(),y.getProperty())) {
			return new OWLQuantifiedProperty(Quantifier.IDENTITY); // TODO - doesn't imply identity for classes
		}
		else {
			// cannot combine - caller will add QP to sequence
			return null;
		}
	}

	// true if there is a property chain such that p1 o p2 --> p3, where p3 is returned
	private OWLObjectProperty chain(OWLObjectProperty p1, OWLObjectProperty p2) {
		if (p1 == null || p2 == null)
			return null;
		if (getPropertyChainMap().containsKey(p1)) {

			for (List<OWLObjectProperty> list : getPropertyChainMap().get(p1)) {
				if (p2.equals(list.get(0))) {
					return list.get(1);
				}
			}
		}
		return null;
	}

	// TODO - currently hardcoded for simple property chains
	Map<OWLObjectProperty,Set<List<OWLObjectProperty>>> pcMap = null;
	private Map<OWLObjectProperty,Set<List<OWLObjectProperty>>> getPropertyChainMap() {
		if (pcMap == null) {
			pcMap = new HashMap<OWLObjectProperty,Set<List<OWLObjectProperty>>>();
			for (OWLSubPropertyChainOfAxiom a : sourceOntology.getAxioms(AxiomType.SUB_PROPERTY_CHAIN_OF)) {
				//LOG.info("CHAIN:"+a+" // "+a.getPropertyChain().size());
				if (a.getPropertyChain().size() == 2) {
					OWLObjectPropertyExpression p1 = a.getPropertyChain().get(0);
					OWLObjectPropertyExpression p2 = a.getPropertyChain().get(1);
					//LOG.info("  xxCHAIN:"+p1+" o "+p2);
					if (p1 instanceof OWLObjectProperty && p2 instanceof OWLObjectProperty) {
						List<OWLObjectProperty> list = new Vector<OWLObjectProperty>();
						list.add((OWLObjectProperty) p2);
						list.add((OWLObjectProperty) a.getSuperProperty());
						if (!pcMap.containsKey(p1)) 
							pcMap.put((OWLObjectProperty) p1, new HashSet<List<OWLObjectProperty>>());
						pcMap.get((OWLObjectProperty) p1).add(list);
						//LOG.info("  xxxCHAIN:"+p1+" ... "+list);
					}
				}
				else {
					// TODO
				}
			}
		}
		return pcMap;
	}

	private boolean isInverseOfPair(OWLObjectProperty p1, OWLObjectProperty p2) {
		for (OWLOntology ont : getAllOntologies()) {
			for (OWLInverseObjectPropertiesAxiom a : ont.getInverseObjectPropertyAxioms(p1)) {
				if (a.getFirstProperty().equals(p2) ||
						a.getSecondProperty().equals(p2)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Find all edges of the form [i INST c] in the graph closure.
	 * (this includes both direct assertions, plus assertions to objects
	 *  that link to c via a chain of SubClassOf assertions)
	 * <p> 
	 * the semantics are the same as inferred ClassAssertion axioms
	 * 
	 * @param c owlClass
	 * @return all individuals classified here via basic graph traversal
	 */
	public Set<OWLIndividual> getInstancesFromClosure(OWLClass c) {
		Set<OWLIndividual> ins = new HashSet<OWLIndividual>();
		for (OWLOntology o : getAllOntologies()) {
			// iterate through all individuals; sequential scan may be slow for
			// large knowledge bases
			for (OWLIndividual in : o.getIndividualsInSignature()) {
				for (OWLGraphEdge e : getEdgesBetween(in, c)) {
					List<OWLQuantifiedProperty> qps = e.getQuantifiedPropertyList();
					// check for edges of the form < i INSTANCE_OF c >
					// we exclude relation chaims, e.g. <i [INSTANCE_OF PART_OF-some] c>
					if (qps.size() == 1 && qps.get(0).isInstanceOf()) {
						ins.add(in);
						break;
					}
				}
			}
		}
		return ins;
	}

	/**
	 * Finds all edges between an instance i and he given class c.
	 * <p>
	 * this includes inferred class assertions, as well as chains such as
	 * <p>
	 * i has_part j, j inst_of k, k part_of some c
	 * 
	 * @param c owlClass
	 * @return all edges in closure between an instance and owlClass
	 */
	public Set<OWLGraphEdge> getInstanceChainsFromClosure(OWLClass c) {
		Set<OWLGraphEdge> edges = new OWLGraphEdgeSet();
		for (OWLOntology o : getAllOntologies()) {
			// iterate through all individuals; sequential scan may be slow for
			// large knowledge bases
			for (OWLIndividual in : o.getIndividualsInSignature()) {
			    edges.addAll(getEdgesBetween(in, c));
			}
		}
		return edges;
	}


	// ----------------------------------------
	// BASIC WRAPPER UTILITIES
	// ----------------------------------------

	/**
	 * returns parent properties of p in all ontologies
	 * @param p
	 * @return set of properties
	 */
	public Set<OWLObjectPropertyExpression> getSuperPropertiesOf(OWLObjectPropertyExpression p) {
		Set<OWLObjectPropertyExpression> ps = new HashSet<OWLObjectPropertyExpression>();
		for (OWLOntology ont : getAllOntologies()) {
			for (OWLSubObjectPropertyOfAxiom a : ont.getObjectSubPropertyAxiomsForSubProperty(p)) {
				ps.add(a.getSuperProperty());
			}
		}
		return ps;
	}

	public Set<OWLObjectPropertyExpression> getSuperPropertyClosureOf(OWLObjectPropertyExpression p) {
		Set<OWLObjectPropertyExpression> superProps = new HashSet<OWLObjectPropertyExpression>();
		Stack<OWLObjectPropertyExpression> stack = new Stack<OWLObjectPropertyExpression>();
		stack.add(p);
		while (!stack.isEmpty()) {
			OWLObjectPropertyExpression nextProp = stack.pop();
			Set<OWLObjectPropertyExpression> directSupers = getSuperPropertiesOf(nextProp);
			directSupers.removeAll(superProps);
			stack.addAll(directSupers);
			superProps.addAll(directSupers);
		}
		return superProps;
	}


	public Set<OWLObjectPropertyExpression> getSuperPropertyReflexiveClosureOf(OWLObjectPropertyExpression p) {
		Set<OWLObjectPropertyExpression> superProps = getSuperPropertyClosureOf(p);
		superProps.add(p);
		return superProps;
	}

	/**
	 * generalizes over quantified properties
	 * 
	 * @param e
	 * @return set of edges
	 */
	public Set<OWLGraphEdge> getOWLGraphEdgeSubsumers(OWLGraphEdge e) {
		return getOWLGraphEdgeSubsumers(e, 0);
	}
	
	
	public Set<OWLGraphEdge> getOWLGraphEdgeSubsumers(OWLGraphEdge e, int i) {
		Set<OWLGraphEdge> subsumers = new OWLGraphEdgeSet();
		if (i >= e.getQuantifiedPropertyList().size()) {
			subsumers.add(new OWLGraphEdge(e.getSource(), e.getTarget(), 
			        new Vector<OWLQuantifiedProperty>(), e.getOntology(), e.getAxioms(), 
			        e.getGCIFiller(), e.getGCIRelation()));
			return subsumers;
		}
		OWLQuantifiedProperty qp = e.getQuantifiedPropertyList().get(i);
		Set<OWLQuantifiedProperty> superQps = new HashSet<OWLQuantifiedProperty>();
		superQps.add(qp);
		OWLObjectProperty p = qp.getProperty();
		if (p != null) {
			for (OWLObjectPropertyExpression pe : getSuperPropertyClosureOf(p)) {
				if (pe.equals(this.getDataFactory().getOWLTopObjectProperty()))
					continue;
				if (pe instanceof OWLObjectProperty) {
					OWLQuantifiedProperty newQp = new OWLQuantifiedProperty(pe, qp.getQuantifier());
					if (!isExcluded(newQp)) {
						superQps.add(newQp);
					}
				}
			}
		}
		for (OWLQuantifiedProperty sqp : superQps) {
			for (OWLGraphEdge se : getOWLGraphEdgeSubsumers(e, i+1)) {
				List<OWLQuantifiedProperty> qpl = new Vector<OWLQuantifiedProperty>();
				qpl.add(sqp);
				qpl.addAll(se.getQuantifiedPropertyList());

				subsumers.add(new OWLGraphEdge(e.getSource(),e.getTarget(),
						qpl, e.getOntology(), e.getAxioms(), 
						e.getGCIFiller(), e.getGCIRelation()));
			}
		}

		return subsumers;
	}

	/**
	 * If available, return the elements of the equivalent property chain.
	 * Return the {@link OWLObjectProperty} as a list with one element or if available, the equivalent chain.
	 * If the id does not map to an known property, return null.
	 * 
	 * @param id the id of the property
	 * @return list or null
	 */
	public List<OWLObjectProperty> getRelationOrChain(String id) {
		OWLObjectProperty p = getOWLObjectPropertyByIdentifier(id);
		if (p != null) {
			final List<OWLObjectProperty> chain = expandRelationChain(p);
			if (chain != null) {
				return chain;
			}
			else {
				return Collections.singletonList(p);
			}
		}
		return null;
	}
	
	/**
	 * If available, return the elements of the equivalent property chain.
	 * <p>
	 * WARNING: If multiple chains exist, only the first one is returned.
	 * This should not happen for any ontology converted from OBO to OWL,
	 * as multiple equivalent_to chains are a violation of OBO specification.
	 * 
	 * @param property the property to be expanded
	 * @return the chain as a list or null if no chain was found
	 */
	public List<OWLObjectProperty> expandRelationChain(OWLObjectProperty property) {
		
		// create the annotation property which marks sub property chain as a valid expansion for a property
		IRI iri = IRI.create(Obo2Owl.IRI_PROP_isReversiblePropertyChain);
		final OWLAnnotationProperty prop = getDataFactory().getOWLAnnotationProperty(iri);
		
		// get all OWLSubPropertyChainOfAxiom from all ontologies in this graph for a given property 
		Set<OWLSubPropertyChainOfAxiom> relevant = new HashSet<OWLSubPropertyChainOfAxiom>();
		for (OWLOntology owlOntology : getAllOntologies()) {
			Set<OWLSubPropertyChainOfAxiom> axioms = owlOntology.getAxioms(AxiomType.SUB_PROPERTY_CHAIN_OF);
			// filter for relevant axioms
			// this is only required because the OWL-API is missing a method
			
			for (OWLSubPropertyChainOfAxiom subPropertyChainOf : axioms) {
				Set<OWLAnnotation> annotations = subPropertyChainOf.getAnnotations(prop);
				boolean isReversiblePropertyChain = false;
				if (annotations != null && !annotations.isEmpty()) {
					for (OWLAnnotation owlAnnotation : annotations) {
						OWLAnnotationValue value = owlAnnotation.getValue();
						if (value instanceof OWLLiteral) {
							OWLLiteral lit = (OWLLiteral) value;
							if (lit.isBoolean()) {
								isReversiblePropertyChain = lit.parseBoolean();
							}
							else {
								String litString = lit.getLiteral();
								isReversiblePropertyChain = "true".equalsIgnoreCase(litString);
							}
							
						}
					}
				}
				if (isReversiblePropertyChain && property.equals(subPropertyChainOf.getSuperProperty())) {
					relevant.add(subPropertyChainOf);
				}
			}
		}
		
		// print warning, there should be either one or no axiom.
		if (relevant.size() > 1) {
			OWLPrettyPrinter pp = new OWLPrettyPrinter(this);
			StringBuilder sb = new StringBuilder("Found multiple OWLSubPropertyChainOfAxioms for property: ");
			sb.append(property.getIRI());
			sb.append("\nAxioms:");
			for (OWLSubPropertyChainOfAxiom axiom : relevant) {
				sb.append(pp.render(axiom));
				sb.append('\n');
			}
			LOG.warn(sb.toString());
		}
		
		// extract the chain
		for (OWLSubPropertyChainOfAxiom subPropertyChainOf : relevant) {
			List<OWLObjectPropertyExpression> chain = subPropertyChainOf.getPropertyChain();
			List<OWLObjectProperty> expanded = new ArrayList<OWLObjectProperty>();
			for (OWLObjectPropertyExpression e : chain) {
				// currently only one level of nesting expected
				if (e.isAnonymous() == false) {
					expanded.add(e.asOWLObjectProperty());
				}
			}
			if (expanded.isEmpty() == false) {
				return expanded;
			}
		}
		
		// default value
		return null;
	}
}

