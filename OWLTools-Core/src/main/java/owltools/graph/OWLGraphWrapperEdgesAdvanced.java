package owltools.graph;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.geneontology.reasoner.ExpressionMaterializingReasoner;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectInverseOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLPropertyExpression;
import org.semanticweb.owlapi.model.OWLPropertyExpressionVisitor;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.util.OWLClassExpressionVisitorAdapter;

import owltools.graph.shunt.OWLShuntEdge;
import owltools.graph.shunt.OWLShuntGraph;
import owltools.graph.shunt.OWLShuntNode;
import owltools.util.OwlHelper;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * @see OWLGraphWrapper
 * @see OWLGraphWrapperEdges
 */
public class OWLGraphWrapperEdgesAdvanced extends OWLGraphWrapperEdgesExtended implements Closeable {

	private static Logger LOG = Logger.getLogger(OWLGraphWrapper.class);

	protected OWLGraphWrapperEdgesAdvanced(OWLOntology ontology) {
		super(ontology);
	}

	protected OWLGraphWrapperEdgesAdvanced(String iri) throws OWLOntologyCreationException {
		super(iri);
	}
	
	private volatile ExpressionMaterializingReasoner reasoner = null;
	private volatile boolean isSynchronized = false;

	// A cache of an arbitrary relationship closure for a certain object.
	private LoadingCache<OWLObject, Map<List<String>,Map<String,String>>> cache = null;
	private int cacheSize = 100000; // default size
	
	public void setEdgesAdvancedCacheSize(int size) {
		this.cacheSize = size;
	}

	public long getCurrentEdgesAdvancedCacheSize() {
		if (cache != null) {
			return cache.size();
		}
		return 0;
	}
	
	private final Set<OWLObjectProperty> materializationPropertySet = new HashSet<OWLObjectProperty>();
	
	public synchronized void addPropertyForMaterialization(OWLObjectProperty p) {
		boolean add = materializationPropertySet.add(p);
		if (add) {
			isSynchronized = false;
		}
	}
	
	public synchronized void addPropertiesForMaterialization(Iterable<OWLObjectProperty> properties) {
		for (OWLObjectProperty p : properties) {
			addPropertyForMaterialization(p);
		}
	}
	
	public synchronized void addPropertyIdsForMaterialization(Iterable<String> propertyIds) {
		for (String propertyId : propertyIds) {
			OWLObjectProperty p = getOWLObjectPropertyByIdentifier(propertyId);
			if (p != null) {
				addPropertyForMaterialization(p);
			}
		}
	}
	
	private synchronized ExpressionMaterializingReasoner getMaterializingReasoner() {
		if (reasoner == null) {
			reasoner = new ExpressionMaterializingReasoner(getSourceOntology(), new ElkReasonerFactory());
			reasoner.materializeExpressions(materializationPropertySet);
			isSynchronized = true;
		}
		else if (isSynchronized == false) {
			reasoner.materializeExpressions(materializationPropertySet);
			isSynchronized = true;
		}
		return reasoner;
	}

	@Override
	public synchronized void close() throws IOException {
		if (reasoner != null) {
			reasoner.dispose();
			reasoner = null;
			isSynchronized = false;
		}
		neighborAxioms = null;
	}

	/**
	 * Convert a list of relationship IDs to a hash set of OWLObjectProperties.
	 * 
	 * @param relation_ids
	 * @return property hash
	 * @see #getRelationClosureMapEngine(OWLObject, List)
	 */
	public Set<OWLObjectProperty> relationshipIDsToPropertySet(List<String> relation_ids){
		Set<OWLObjectProperty> props = new HashSet<OWLObjectProperty>();
		for( String rel_id : relation_ids ){
			OWLObjectProperty oop = getOWLObjectPropertyByIdentifier(rel_id);
			if( oop != null ){
				props.add(oop);
			}
		}
		return props;
	}

	
	/**
	 * Classify the an edge and target as a human readable string for further processing.
	 * 
	 * @param owlGraphEdge edge under consideration
	 * @param edgeDirector 
	 * @param props properties set
	 * @return null, "simplesubclass", "typesubclass", or "identity".
	 * @see #addDirectDescendentsToShuntGraph
	 * @see #addStepwiseAncestorsToShuntGraph
	 */
	public String classifyRelationship(OWLGraphEdge owlGraphEdge, OWLObject edgeDirector, Set<? extends OWLPropertyExpression> props){		
		String retval = null;
		
		OWLQuantifiedProperty qp = owlGraphEdge.getSingleQuantifiedProperty();
		if( qp.isSubClassOf() || props.contains(qp.getProperty()) ){
			//OWLObject target = owlGraphEdge.getTarget();
			if( edgeDirector instanceof OWLClass ){
				retval = "simplesubclass";
			}else if( edgeDirector instanceof OWLObjectSomeValuesFrom ){
				OWLObjectSomeValuesFrom some = (OWLObjectSomeValuesFrom)edgeDirector;
				if( props.contains(some.getProperty()) ){
					OWLClassExpression clsexp = some.getFiller();
					if( ! clsexp.isAnonymous()){
						retval = "typesubclass";
					}
				}
			}
		}else if( qp.isIdentity() ){
			retval = "identity";
		}else{
			if (LOG.isDebugEnabled()) {
				LOG.debug("Skipping complex edge: "+owlGraphEdge);
			}
		}
		
		return retval;
	}

	
	/**
	 * Add a set of edges, as ancestors to x in OWLShuntGraph g.
	 * This is reflexive.
	 *
	 * @param x
	 * @param g
	 * @param rel_ids
	 * @return the modified OWLShuntGraph
	 */
	public OWLShuntGraph addStepwiseAncestorsToShuntGraph(OWLObject x, OWLShuntGraph g, List<String> rel_ids) {

		// Add this node, our seed.
		String topicID = getIdentifier(x);
		String topicLabel = getLabel(x);
		OWLShuntNode tn = new OWLShuntNode(topicID, topicLabel);
		g.addNode(tn);

		// NEW VERSION
		Set<OWLObjectProperty> props = relationshipIDsToPropertySet(rel_ids);
		for (OWLGraphEdge e : getOutgoingEdges(x, props)) {
			OWLObject target = e.getTarget();
			String rel = classifyRelationship(e, target, props);

			if( rel != null ){

				// Placeholders.
				String objectID = null;
				String objectLabel = null;
				String elabel = null;
				
				if( rel == "simplesubclass" ){
					objectID = getIdentifier(target);
					objectLabel = getLabelOrDisplayId(target);
					elabel = getEdgeLabel(e);
				}else if( rel == "typesubclass" ){
					OWLObjectSomeValuesFrom some = (OWLObjectSomeValuesFrom)target;
					OWLClassExpression clsexp = some.getFiller();
					OWLClass cls = clsexp.asOWLClass();
					objectID = getIdentifier(cls);
					objectLabel = getLabelOrDisplayId(cls);
					elabel = getEdgeLabel(e);
				}
				
				// Only add when subject, object, and relation are properly defined.
				if(	elabel != null &&
					topicID != null && ! topicID.equals("") &&
					objectID != null &&	! objectID.equals("") ){
	
					// Add node.
					OWLShuntNode sn = new OWLShuntNode(objectID, objectLabel);
					boolean wuzAdded = g.addNode(sn);

					// Recur on node if it already wasn't there.
					if( wuzAdded ){
						addStepwiseAncestorsToShuntGraph(target, g, rel_ids);
					}
				
					//Add edge 
					OWLShuntEdge se = new OWLShuntEdge(topicID, objectID, elabel);
					g.addEdge(se);
				}
			}
		}

// ORIGINAL VERSION
//		// Next, get all of the named ancestors and add them to our shunt graph.
//		// We need some traversal code going up!
//		for (OWLGraphEdge e : getOutgoingEdges(x)) {
//			OWLObject t = e.getTarget();
//			if (t instanceof OWLNamedObject){				
//
//				// Figure out object.
//				String objectID = getIdentifier(t);
//				String objectLabel = getLabel(t);
//
//				// Edge.
//				String elabel = getEdgeLabel(e);
//				
//				// Only add when subject, object, and relation are properly defined.
//				if( elabel != null &&
//					topicID != null && ! topicID.equals("") &&
//					objectID != null &&	! objectID.equals("") ){
//				
//					// Add node.
//					OWLShuntNode sn = new OWLShuntNode(objectID, objectLabel);
//					boolean wuzAdded = g.addNode(sn);
//
//					// Recur on node if it already wasn't there.
//					if( wuzAdded ){
//						addStepwiseAncestorsToShuntGraph(t, g);
//					}
//				
//					//Add edge 
//					OWLShuntEdge se = new OWLShuntEdge(topicID, objectID, elabel);
//					g.addEdge(se);
//				}
//			}
//		}
		
		return g;
	}

	
	/**
	 * Add a set of edges, as ancestors to x in OWLShuntGraph g.
	 * This is reflexive.
	 *
	 * @param x
	 * @param g
	 * @param rel_ids
	 * @return the modified OWLShuntGraph
	 */
	public OWLShuntGraph addTransitiveAncestorsToShuntGraph(OWLObject x, OWLShuntGraph g, List<String> rel_ids) {

		// Add this node, our seed.
		String topicID = getIdentifier(x);
		String topicLabel = getLabel(x);
		OWLShuntNode tn = new OWLShuntNode(topicID, topicLabel);
		g.addNode(tn);

		Set<OWLObjectProperty> props = relationshipIDsToPropertySet(rel_ids);
		if (x instanceof OWLClass) {
			addTransitiveAncestorsToShuntGraph((OWLClass) x, topicID, g, props);
		}
		else if (x instanceof OWLObjectProperty) {
			addTransitiveAncestorsToShuntGraph((OWLObjectProperty) x, topicID, g, props);
		}
		return g;
	}

	private void addTransitiveAncestorsToShuntGraph(final OWLClass cls, final String topicID, 
			final OWLShuntGraph g, final Set<OWLObjectProperty> props) {
		addPropertiesForMaterialization(props);
		ExpressionMaterializingReasoner materializingReasoner = getMaterializingReasoner();
		Set<OWLClassExpression> classExpressions = materializingReasoner.getSuperClassExpressions(cls, false);
		for (OWLClassExpression ce : classExpressions) {
			ce.accept(new OWLClassExpressionVisitorAdapter(){

				@Override
				public void visit(OWLClass cls) {
					if (cls.isBuiltIn() == false) {
						String elabel = "is_a";
						String objectID = getIdentifier(cls);
						String objectLabel = getLabelOrDisplayId(cls);
						if(topicID != null && objectID != null){
							// Add the node.
							OWLShuntNode on = new OWLShuntNode(objectID, objectLabel);
							g.addNode(on);

							// And the edges.
							OWLShuntEdge se = new OWLShuntEdge(topicID, objectID, elabel);
							g.addEdge(se);
						}
					}
				}

				@Override
				public void visit(OWLObjectSomeValuesFrom svf) {
					OWLObjectPropertyExpression pe = svf.getProperty();
					if (props.contains(pe)) {
						OWLObjectProperty p = pe.asOWLObjectProperty();
						OWLClassExpression filler = svf.getFiller();
						if (!filler.isAnonymous()) {
							OWLClass target = filler.asOWLClass();
							String objectID = null;
							String objectLabel = null;
							String elabel = null;
							objectID = getIdentifier(target);
							objectLabel = getLabelOrDisplayId(target);
							elabel = getLabel(p);
							if(topicID != null && elabel != null && objectID != null){
								// Add the node.
								OWLShuntNode on = new OWLShuntNode(objectID, objectLabel);
								g.addNode(on);

								// And the edges.
								OWLShuntEdge se = new OWLShuntEdge(topicID, objectID, elabel);
								g.addEdge(se);
							}
						}
					}
				}
				
			});
		}
	}
	
	private void addTransitiveAncestorsToShuntGraph(final OWLObjectProperty p, final String topicID, 
			final OWLShuntGraph g, final Set<OWLObjectProperty> props) {
		// using the graph walker instead of a reasoner: ELK does not implement getSuperProperties()
		Set<OWLObjectPropertyExpression> closure = getSuperPropertyClosureOf(p);
		closure.add(p);
		for (OWLObjectPropertyExpression pe : closure) {
			pe.accept(new OWLPropertyExpressionVisitor() {
				
				@Override
				public void visit(OWLDataProperty property) {
					// ignore
				}
				
				@Override
				public void visit(OWLObjectInverseOf property) {
					// ignore
				}
				
				@Override
				public void visit(OWLObjectProperty property) {
					if (property.isBuiltIn()) {
						String elabel = "is_a";
						String objectID = getIdentifier(property);
						String objectLabel = getLabelOrDisplayId(property);
						if(topicID != null && objectID != null){
							// Add the node.
							OWLShuntNode on = new OWLShuntNode(objectID, objectLabel);
							g.addNode(on);

							// And the edges.
							OWLShuntEdge se = new OWLShuntEdge(topicID, objectID, elabel);
							g.addEdge(se);
						}
					}
				}

				@Override
				public void visit(OWLAnnotationProperty property) {
					// ignoe
				}
			});
		}
	}
	
	/**
	 * Add a set of edges, as descendants to x in OWLShuntGraph g.
	 * This is reflexive.
	 *
	 * @param x
	 * @param g
	 * @param rel_ids
	 * @return the modified OWLShuntGraph
	 */
	public OWLShuntGraph addDirectDescendentsToShuntGraph(OWLObject x, OWLShuntGraph g, List<String> rel_ids) {

		// Add this node, our seed.
		String topicID = getIdentifier(x);
		String topicLabel = getLabel(x);
		OWLShuntNode tn = new OWLShuntNode(topicID, topicLabel);
		g.addNode(tn);
		
		// NEW VERSION
		Set<OWLObjectProperty> props = relationshipIDsToPropertySet(rel_ids);
		for (OWLGraphEdge e : getIncomingEdges(x)) {
			OWLObject source = e.getSource();
			String rel = classifyRelationship(e, source, props);

			if( rel != null ){

				// Placeholders.
				String subjectID = null;
				String subjectLabel = null;
				String elabel = null;

				if( rel == "simplesubclass" ){
					subjectID = getIdentifier(source);
					subjectLabel = getLabelOrDisplayId(source);
					elabel = getEdgeLabel(e);
				}else if( rel == "typesubclass" ){
					OWLObjectSomeValuesFrom some = (OWLObjectSomeValuesFrom)source;
					OWLClassExpression clsexp = some.getFiller();
					OWLClass cls = clsexp.asOWLClass();
					subjectID = getIdentifier(cls);
					subjectLabel = getLabelOrDisplayId(cls);
					elabel = getEdgeLabel(e);
				}

				// Only add when subject, object, and relation are properly defined.
				if( elabel != null &&
					topicID != null && ! topicID.equals("") &&
					subjectID != null && ! subjectID.equals("") ){

					// Add node.
					OWLShuntNode sn = new OWLShuntNode(subjectID, subjectLabel);
					g.addNode(sn);

					//Add edge.
					OWLShuntEdge se = new OWLShuntEdge(subjectID, topicID, elabel);
					g.addEdge(se);
				}
			}
		}
		
// ORIGINAL VERSION
//		// Next, get all of the immediate descendents.
//		// Yes, this could be done more efficiently by reworking 
//		// getIncomingEdgesClosure for our case, but I'm heading towards
//		// proof of concept right now; optimization later.
//		// Basically, toss anything that is not of distance 1--we already got
//		// reflexive above.
//		for (OWLGraphEdge e : getIncomingEdges(x)) { // TODO: use getIsaPartofClosureMap as a reference for how this should be done
//		//for (OWLGraphEdge e : getPrimitiveIncomingEdges(x)) { // this failed--maybe faster, but dropped our regulates
//			OWLObject t = e.getSource();
//			if( t instanceof OWLNamedObject ){
//
//				// Figure out subject.
//				String subjectID = getIdentifier(t);
//				String subjectLabel = getLabel(t);
//
//				// Figure edge out.
//				String elabel = getEdgeLabel(e);
//
//				// Only add when subject, object, and relation are properly defined.
//				if( elabel != null &&
//					topicID != null && ! topicID.equals("") &&
//					subjectID != null && ! subjectID.equals("") ){
//
//					// Add node.
//					OWLShuntNode sn = new OWLShuntNode(subjectID, subjectLabel);
//					g.addNode(sn);
//
//					//Add edge.
//					OWLShuntEdge se = new OWLShuntEdge(subjectID, topicID, elabel);
//					g.addEdge(se);
//
//					// TODO/BUG: detecting that only "is_a" children are being found--make
//					// a test case and see what people see.
////					if( ! elabel.equals("is_a") ){
////						LOG.info("Edge label: "+ elabel);						
////					}
//				}
//			}
//		}	

		return g;
	}

	
	/**
	 * Gets all ancestors and direct descendants (distance == 1) that are OWLNamedObjects.
	 * i.e. excludes anonymous class expressions
	 * <p>
	 * TODO: we're current just doing distance == 1 up;
	 *       we'll want to have a more full graph in the future
	 * <p>
	 * TODO: a work in progress
	 * 
	 * @param x
	 * @param rel_ids
	 * @return set of named ancestors and direct descendents
	 */
	public OWLShuntGraph getSegmentShuntGraph(OWLObject x, List<String> rel_ids) {

		// Collection depot.
		OWLShuntGraph graphSegment = new OWLShuntGraph();

		// Add this node, our seed.
		String topicID = getIdentifier(x);
		String topicLabel = getLabel(x);
		OWLShuntNode tn = new OWLShuntNode(topicID, topicLabel);
		graphSegment.addNode(tn);

		// Next, get all of the named ancestors and add them to our shunt graph.
		graphSegment = addStepwiseAncestorsToShuntGraph(x, graphSegment, rel_ids);

		// Next, get all of the immediate descendents.
		graphSegment = addDirectDescendentsToShuntGraph(x, graphSegment, rel_ids);

		//		
		return graphSegment;
	}

	
	/**
	 * Gets all ancestors that are OWLNamedObjects.
	 * i.e. excludes anonymous class expressions
	 * <p>
	 * This graph information is concerned almost exclusively with the arguments transitive relations with all of its ancestors.
	 * 
	 * @param x
	 * @param rel_ids
	 * @return set of named ancestors and direct descendents
	 */
	public OWLShuntGraph getLineageShuntGraph(OWLObject x, List<String> rel_ids) {

		// Collection depot.
		OWLShuntGraph graphSegment = new OWLShuntGraph();

		// Add this node, our seed.
		String topicID = getIdentifier(x);
		String topicLabel = getLabel(x);
		OWLShuntNode tn = new OWLShuntNode(topicID, topicLabel);
		graphSegment.addNode(tn);

		// Next, get all of the named ancestors and add them to our shunt graph.
		graphSegment = addTransitiveAncestorsToShuntGraph(x, graphSegment, rel_ids);

		//		
		return graphSegment;
	}

	
	/**
	 * Return a JSONized version of the output of getSegmentShuntGraph
	 *
	 * Defaults to is_a/part_of and regulates.
	 *
	 * @param x
	 * @param sargs
	 * @return String representing part of the stepwise OWL graph
	 */
	public String getSegmentShuntGraphJSON(OWLObject x, List<String> sargs) {

		// Collection depot.		
		OWLShuntGraph graphSegment = getSegmentShuntGraph(x, sargs);

		return graphSegment.toJSON();
	}

	
	/**
	 * Return a JSONized version of the output of getLineageShuntGraph
	 * @param x
	 * @param sargs
	 * @return String representing part of the transitive OWL graph
	 */
	public String getLineageShuntGraphJSON(OWLObject x, List<String> sargs) {

		// Collection depot.
		OWLShuntGraph graphSegment = getLineageShuntGraph(x, sargs);

		return graphSegment.toJSON();
	}

	
	/**
	 * Return a map of id to label for the closure of the ontology using the supplied relation id list and .isSubClassOf().
	 * <p>
	 * Intended for GOlr loading.
	 * 
	 * @param c
	 * @param relation_ids
	 * @return map of ids to their displayable labels
	 * @see #getRelationClosureMapEngine(OWLObject, List)
	 */
	public Map<String,String> getRelationClosureMap(OWLObject c, final List<String> relation_ids){

		// init cache
		if (cache == null) {
			cache = CacheBuilder.newBuilder()
				.maximumSize(cacheSize)
				.build(new CacheLoader<OWLObject, Map<List<String>,Map<String,String>>>() {

					@Override
					public Map<List<String>, Map<String, String>> load(OWLObject key) {
						return new HashMap<List<String>,Map<String,String>>();
					}
				});
		}
		
		Map<List<String>, Map<String, String>> mgrcm = cache.getUnchecked(c);
		Map<String,String> retmap = mgrcm.get(relation_ids);
		if( retmap == null ){ // generate
			retmap = getRelationClosureMapEngine(c, relation_ids);
			mgrcm.put(relation_ids, retmap);
		}
		return retmap;
	}

	
	/**
	 * Generator for the cache in {@link #getRelationClosureMap(OWLObject, List)}.
	 * 
	 * @param obj
	 * @param relation_ids
	 * @return map of ids to their displayable labels
	 * @see #getRelationClosureMap(OWLObject, List)
	 */
	public Map<String,String> getRelationClosureMapEngine(OWLObject obj, List<String> relation_ids){

		final Map<String,String> relation_map = new HashMap<String,String>(); // capture labels/ids
		
		// reflexive
		String id = getIdentifier(obj);
		String label = getLabel(obj);
		relation_map.put(id, label);
		
		// Our relation collection.
		final Set<OWLObjectProperty> props = relationshipIDsToPropertySet(relation_ids);
		
		if (obj instanceof OWLClass) {
			addIdLabelClosure((OWLClass) obj, true, props, relation_map);
		}
		else if (obj instanceof OWLObjectProperty) {
			addIdLabelClosure((OWLObjectProperty) obj, true, relation_map);
		}
		return relation_map;
	}
	
	private void addIdLabelClosure(OWLClass c, boolean reflexive, 
			final Set<OWLObjectProperty> props, final Map<String,String> relation_map) {
		addPropertiesForMaterialization(props);
		ExpressionMaterializingReasoner materializingReasoner = getMaterializingReasoner();
		Set<OWLClassExpression> classExpressions = materializingReasoner.getSuperClassExpressions(c, false);
		OWLEntity owlThing = this.getManager().getOWLDataFactory().getOWLThing();
		
		// remove owl:Thing - although logically valid, it is not of use to the consumer
		// see https://github.com/geneontology/amigo/issues/395
		classExpressions = classExpressions.stream().filter(x -> x.getSignature().contains(owlThing)).collect(Collectors.toSet());
		for (OWLClassExpression ce : classExpressions) {
			ce.accept(new OWLClassExpressionVisitorAdapter(){

				@Override
				public void visit(OWLClass cls) {
					if (cls.isBuiltIn() == false) {
						final String id = getIdentifier(cls);
						final String label = getLabelOrDisplayId(cls);
						relation_map.put(id, label);
					}
				}

				@Override
				public void visit(OWLObjectSomeValuesFrom svf) {
					if (props.contains(svf.getProperty())) {
						OWLClassExpression filler = svf.getFiller();
						if (!filler.isAnonymous()) {
							OWLClass cls = filler.asOWLClass();
							final String id = getIdentifier(cls);
							final String label = getLabelOrDisplayId(cls);
							relation_map.put(id, label);
						}
					}
				}
				
			});
		}
	}
	
	private void addIdLabelClosure(OWLObjectProperty p, boolean reflexive,
			final Map<String,String> relation_map) {
		// using the graph walker instead of a reasoner: ELK does not implement getSuperProperties()
		Set<OWLObjectPropertyExpression> closure = getSuperPropertyClosureOf(p);
		closure.add(p);
		for (OWLObjectPropertyExpression pe : closure) {
			pe.accept(new OWLPropertyExpressionVisitor() {
				
				@Override
				public void visit(OWLDataProperty property) {
					// ignore
				}
				
				@Override
				public void visit(OWLObjectInverseOf property) {
					// ignore
				}
				
				@Override
				public void visit(OWLObjectProperty property) {
					if (property.isBuiltIn() == false) {
						final String id = getIdentifier(property);
						final String label = getLabelOrDisplayId(property);
						relation_map.put(id, label);
					}
				}

				@Override
				public void visit(OWLAnnotationProperty property) {
					// ignore
				}
			});
		}
	}
	
	/**
	 * Return a overlaps with getIsaPartofLabelClosure and stuff in GafSolrDocumentLoader.
	 * <p>
	 * Intended for GOlr loading.
	 * 
	 * @param c
	 * @return map of is_partof_closure ids to their displayable labels
	 */
	@Deprecated
	public Map<String,String> getIsaPartofClosureMap(OWLObject c){

		Map<String,String> isa_partof_map = new HashMap<String,String>(); // capture labels/ids
		final OWLObjectProperty partOfProperty = getOWLObjectPropertyByIdentifier("BFO:0000050");
		
		Set<OWLGraphEdge> edges = getOutgoingEdgesClosureReflexive(c);
		for (OWLGraphEdge owlGraphEdge : edges) {
			OWLQuantifiedProperty qp = owlGraphEdge.getSingleQuantifiedProperty();
			if (qp.isSubClassOf() || partOfProperty.equals(qp.getProperty())) {
				OWLObject target = owlGraphEdge.getTarget();
				if (target instanceof OWLClass) {
					final String id = getIdentifier(target);
					final String label = getLabelOrDisplayId(target);
					isa_partof_map.put(id, label);
				}else if (target instanceof OWLObjectSomeValuesFrom) {
					OWLClassExpression clsexp = ((OWLObjectSomeValuesFrom)target).getFiller();
					if( ! clsexp.isAnonymous()){
						OWLClass cls = clsexp.asOWLClass();
						final String id = getIdentifier(cls);
						final String label = getLabelOrDisplayId(cls);
						isa_partof_map.put(id, label);
					}
				}
			}else if (qp.isIdentity()) {
				final String id = getIdentifier(c);
				final String label = getLabelOrDisplayId(c);
				isa_partof_map.put(id, label);
			}else {
				//System.out.println(owlGraphEdge);
			}
		}
		
		return isa_partof_map;
	}

	/**
	 * Return a overlaps with getIsaPartofLabelClosure and stuff in GafSolrDocumentLoader.
	 * <p>
	 * Intended for GOlr loading.
	 * 
	 * @param c
	 * @return list of is_partof_closure ids
	 */
	@Deprecated
	public List<String> getIsaPartofIDClosure(OWLObject c){
		Map<String, String> foo = getIsaPartofClosureMap(c);
		List<String> bar = new ArrayList<String>(foo.keySet());
		return bar;
	}

	/**
	 * Return a overlaps with getIsaPartofLabelClosure and stuff in GafSolrDocumentLoader.
	 * <p>
	 * Intended for GOlr loading.
	 * <p>
	 * This is a curried FlexLoader s-expression version of {@link #getIsaPartofIDClosure(OWLObject)}.
	 * 
	 * @param c
	 * @param sargs
	 * @return list of is_partof_closure ids
	 * @see #getIsaPartofIDClosure(OWLObject)
	 */
	@Deprecated
	public List<String> getIsaPartofIDClosure(OWLObject c, List<String> sargs){
		return getIsaPartofIDClosure(c);
	}

	/**
	 * Return a overlaps with getIsaPartofLabelClosure and stuff in GafSolrDocumentLoader.
	 * <p>
	 * Intended for GOlr loading.
	 * 
	 * @param c
	 * @param relation_ids
	 * @return list of is_partof_closure ids
	 */
	public List<String> getRelationIDClosure(OWLObject c, List<String> relation_ids){
		Map<String, String> foo = getRelationClosureMap(c, relation_ids);
		List<String> bar = new ArrayList<String>(foo.keySet());
		return bar;
	}

	/**
	 * Return a overlaps with getIsaPartofIDClosure and stuff in GafSolrDocumentLoader.
	 * <p>
	 * Intended for GOlr loading.
	 * 
	 * @param c
	 * @return list of is_partof_closure labels
	 */
	@Deprecated
	public List<String> getIsaPartofLabelClosure(OWLObject c){
		Map<String, String> foo = getIsaPartofClosureMap(c);
		List<String> bar = new ArrayList<String>(foo.values());
		return bar;
	}

	/**
	 * Return a overlaps with getIsaPartofIDClosure and stuff in GafSolrDocumentLoader.
	 * <p>
	 * Intended for GOlr loading.
	 * <p>
	 * This is a curried FlexLoader s-expression version of {@link #getIsaPartofLabelClosure(OWLObject)}.
	 * 
	 * @param c
	 * @param sargs
	 * @return list of is_partof_closure labels
	 * @see #getIsaPartofLabelClosure(OWLObject)
	 */
	@Deprecated
	public List<String> getIsaPartofLabelClosure(OWLObject c, List<String> sargs){
		return getIsaPartofLabelClosure(c);
	}

	/**
	 * Return a overlaps with getIsaPartofIDClosure and stuff in GafSolrDocumentLoader.
	 * <p>
	 * Intended for GOlr loading.
	 * 
	 * @param c
	 * @param relation_ids
	 * @return list of is_partof_closure labels
	 */
	public List<String> getRelationLabelClosure(OWLObject c, List<String> relation_ids){
		Map<String, String> foo = getRelationClosureMap(c, relation_ids);
		List<String> bar = new ArrayList<String>(foo.values());
		return bar;
	}

	/**
	 * Return the names of the asserted subClasses of the cls (Class) 
	 * passed in the argument
	 * 
	 * 
	 * @param cls
	 * @return array of of strings
	 */
	@Deprecated
	public String[] getSubClassesNames(OWLClass cls){
		Set<OWLClassExpression> st = OwlHelper.getSubClasses(cls, sourceOntology);


		List<String> ar = new ArrayList<String>();
		for(OWLClassExpression ce: st){
			if(ce instanceof OWLNamedObject)
				ar.add(getLabel(ce)); 
		}

		return ar.toArray(new String[ar.size()]);
	}

	public String categorizeNamespace(OWLObject c, Map<String, Object> configuration) {
		String result = null;
		if (c instanceof OWLNamedObject) {
			Map<String, String> idspaceMappings = new HashMap<String, String>();
			Set<String> useNamespace = new HashSet<String>();
			boolean useFallback = true;
			// configuration
			if (configuration != null) {
				// check mapped id spaces
				Object mapped = configuration.get("idspace-map");
				if (mapped != null && mapped instanceof Map<?, ?>) {
					for(Entry<?, ?> entry : ((Map<?, ?>) mapped).entrySet()) {
						idspaceMappings.put(entry.getKey().toString(), entry.getValue().toString());
					}
				}
				
				// check use namespace
				Object useNamespaceConfig = configuration.get("use-namespace");
				if (useNamespaceConfig != null && useNamespaceConfig instanceof Iterable<?>) {
					for (Object o : (Iterable<?>)useNamespaceConfig) {
						useNamespace.add(o.toString());
					}
				}
				
				// check use fallback
				Object fallbackConfig = configuration.get("use-fallback");
				if (fallbackConfig != null) {
					String fallbackString = fallbackConfig.toString();
					if (Boolean.FALSE.toString().equalsIgnoreCase(fallbackString)) {
						useFallback = false;
					}
				}
			}
			result = categorizeObject((OWLNamedObject) c, idspaceMappings, useNamespace, useFallback);
		}
		return result;
		
	}
	
	/**
	 * Create a category for a given named object. There are three options:
	 * <ol>
	 *  <li>use id space to lookup category string in the id space map</li>
	 *  <li>use OBO namespace, if the is space is in useNamespace set</li>
	 *  <li>fallback: use id space prefix (string before first colon)</li>
	 * </ol>
	 * 
	 * @param named
	 * @param idspaceMappings
	 * @param useNamespace
	 * @param useFallback if false ignore fall-back
	 * @return category or null
	 */
	public String categorizeObject(OWLNamedObject named,  Map<String,String> idspaceMappings, 
			Set<String> useNamespace, boolean useFallback){
		String result = null;
		String idSpace = getIdspace(named.getIRI());
		if (idSpace != null) {
			if (idspaceMappings.containsKey(idSpace)) {
				result = idspaceMappings.get(idSpace);
			}
			else if (useNamespace.contains(idSpace)) {
				result = getNamespace(named);
			}
			else if (useFallback){
				result = idSpace;
			}
			// normalize
			if (result != null) {
				// replace backslash, dash, underscore, or slash with a whitespace
				result = result.replaceAll("[\\-_/]", " ");
			}
		}
		return result;
	}
	
	private String getIdspace(IRI iri) {
		String idSpace = null;
		String identifier = getIdentifier(iri);
		if (identifier != null) {
			int colonPos = identifier.indexOf(':');
			if (colonPos > 0) {
				idSpace = identifier.substring(0, colonPos);
			}
		}
		return idSpace;
	}
	
	
	/**
	 * Helper function for flex loader for un-used single string fields
	 * 
	 * @param c
	 * @param sargs
	 * @return always null
	 */
	public String getDummyString(OWLObject c, List<String> sargs) {
		return null;
	}
	
	/**
	 * Helper function for flex loader for un-used single List of string fields
	 * 
	 * @param c
	 * @param sargs
	 * @return always an empty list
	 */
	public List<String> getDummyStrings(OWLObject c, List<String> sargs) {
		return Collections.emptyList();
	}

	public Set<String> getOnlyInTaxon(OWLObject x, List<String> sargs) {
		Set<OWLClass> classes = getOnlyInTaxonSvfClasses(x);
		if (classes.isEmpty() == false) {
			Set<String> ids = new HashSet<String>();
			for(OWLClass cls : classes) {
				ids.add(getIdentifier(cls));
			}
			return ids;
		}
		return Collections.emptySet();
	}

	public Set<String> getOnlyInTaxonLabels(OWLObject x, List<String> sargs) {
		Set<OWLClass> classes = getOnlyInTaxonSvfClasses(x);
		if (classes.isEmpty() == false) {
			Set<String> labels = new HashSet<String>();
			for(OWLClass cls : classes) {
				labels.add(getLabelOrDisplayId(cls));
			}
			return labels;
		}
		return Collections.emptySet();
	}
	
	public Map<String, String> getOnlyInTaxonLabelMap(OWLObject x, List<String> sargs) {
		Set<OWLClass> classes = getOnlyInTaxonSvfClasses(x);
		if (classes.isEmpty() == false) {
			Map<String, String> labelMap = new HashMap<String, String>();
			for(OWLClass cls : classes) {
				labelMap.put(getIdentifier(cls), getLabelOrDisplayId(cls));
			}
			return labelMap;
		}
		return Collections.emptyMap();
	}
	
	private Set<OWLClass> getOnlyInTaxonSvfClasses(OWLObject x) {
		if (x != null && x instanceof OWLClass) {
			OWLClass c = (OWLClass) x;
			IRI onlyInTaxonIRI = IRI.create("http://purl.obolibrary.org/obo/RO_0002160");
			OWLObjectProperty onlyInTaxon = getDataFactory().getOWLObjectProperty(onlyInTaxonIRI);
			return getSvfClasses(c, onlyInTaxon);
		}
		return Collections.emptySet();
	}
	
	Set<OWLClass> getSvfClasses(OWLClass c, OWLObjectProperty p) {
		Set<OWLSubClassOfAxiom> axioms = new HashSet<OWLSubClassOfAxiom>();
		for(OWLOntology ont : getAllOntologies()) {
			axioms.addAll(ont.getSubClassAxiomsForSubClass(c));
		}
		Set<OWLClass> superClasses = new HashSet<OWLClass>();
		for (OWLSubClassOfAxiom axiom : axioms) {
			OWLClassExpression expr = axiom.getSuperClass();
			if (expr instanceof OWLObjectSomeValuesFrom) {
				OWLObjectSomeValuesFrom svf = (OWLObjectSomeValuesFrom) expr;
				if (p.equals(svf.getProperty())) {
					OWLClassExpression filler = svf.getFiller();
					if (filler instanceof OWLClass) {
						superClasses.add((OWLClass) filler);
					}
				}
			}
		}
		return superClasses;
	}

	public Set<String> getOnlyInTaxonClosure(OWLObject x, List<String> relation_ids) {
		Set<OWLClass> classes = getOnlyInTaxonSvfClasses(x);
		if (classes.isEmpty() == false) {
			Set<String> ids = new HashSet<String>();
			for(OWLClass cls : classes) {
				ids.addAll(getRelationIDClosure(cls, relation_ids));
			}
			return ids;
		}
		return Collections.emptySet();
	}

	public Set<String> getOnlyInTaxonClosureLabels(OWLObject x, List<String> relation_ids) {
		Set<OWLClass> classes = getOnlyInTaxonSvfClasses(x);
		if (classes.isEmpty() == false) {
			Set<String> labels = new HashSet<String>();
			for(OWLClass cls : classes) {
				labels.addAll(getRelationLabelClosure(cls, relation_ids));
			}
			return labels;
		}
		return Collections.emptySet();
	}
	
	/**
	 * Retrieve direct neighbors of x in a shunt graph.
	 * <p>
	 * Intended for GOlr loading.
	 * <p>
	 * This is a curried FlexLoader s-expression version of {@link #getNeighbors(OWLObject)}.
	 * 
	 * @param x
	 * @param sargs
	 * @return shunt graph
	 */
	public OWLShuntGraph getNeighbors(OWLObject x, List<String> sargs) {
		return getNeighbors(x);
	}
	
	/**
	 * Retrieve direct neighbors of x in a shunt graph JSON string.
	 * <p>
	 * Intended for GOlr loading.
	 * <p>
	 * This is a curried FlexLoader s-expression version of
	 * {@link #getNeighbors(OWLObject)} with a conversion of the graph to a JSON
	 * string.
	 * 
	 * @param x
	 * @param sargs
	 * @return json shunt graph
	 */
	public String getNeighborsJSON(OWLObject x, List<String> sargs) {
		return getNeighbors(x).toJSON();
	}
	
	/**
	 * Retrieve direct neighbors of x in a shunt graph JSON string. Limit the maximum size of the edges.
	 * 
	 * This is a curried FlexLoader s-expression version of
	 * {@link #getNeighborsLimited(OWLObject, int)} with a conversion of the graph to a JSON
	 * string.
	 * 
	 * @param x
	 * @param sargs
	 * @return json shunt graph
	 */
	public String getNeighborsLimitedJSON(OWLObject x, List<String> sargs) {
		int limit = 100;
		if (sargs.isEmpty() == false) {
			String first = sargs.get(0);
			try {
				limit = Integer.parseInt(first);
			}
			catch (NumberFormatException exception) {
				LOG.error("Could not parse number: '"+first+"'", exception);
			}
		}
		OWLShuntGraph shunt = getNeighborsLimited(x, limit);
		return shunt.toJSON();
	}
	
	public OWLShuntGraph getNeighborsLimited(OWLObject x, int edgeLimit) {
		OWLShuntGraph all = createNeighbors(x, -1);
		if (all.edges.size() <= edgeLimit) {
			return all;
		}
		OWLShuntGraph sub = createNeighbors(x, edgeLimit);
		sub.setIncomplete(all.nodes.size(), all.edges.size());
		return sub;
	}
	
	/**
	 * Retrieve direct neighbors of x in a shunt graph.
	 * 
	 * @param x
	 * @return shunt graph
	 */
	public OWLShuntGraph getNeighbors(OWLObject x) {
		return createNeighbors(x, -1);
	}
	

	private Map<OWLClass, Set<OWLSubClassOfAxiom>> neighborAxioms = null;
	private final Object neighborAxiomsMutex = new Object();
	
	/**
	 * Retrieve direct neighbors of x in a shunt graph.
	 * 
	 * @param x
	 * @return shunt graph
	 */
	private OWLShuntGraph createNeighbors(OWLObject x, int edgeLimit) {
		final OWLShuntGraph shunt = new OWLShuntGraph();
		
		final String xID = getIdentifier(x);
		final String xLabel = getLabel(x);
		final OWLShuntNode xNode = new OWLShuntNode(xID, xLabel);
		shunt.addNode(xNode);
		
		if (x instanceof OWLClass) {
			synchronized (neighborAxiomsMutex) {
				if (neighborAxioms == null) {
					neighborAxioms = initNeighborAxioms();
				}
			}
			Map<OWLClass, OWLShuntNode> nodes = new HashMap<OWLClass, OWLShuntNode>();
			OWLClass cls = (OWLClass) x;
			Set<OWLSubClassOfAxiom> subClassAxioms = neighborAxioms.get(cls);
			Set<OWLEquivalentClassesAxiom> equivAxioms = new HashSet<OWLEquivalentClassesAxiom>();
			for(OWLOntology ont : getAllOntologies()) {
				equivAxioms.addAll(ont.getEquivalentClassesAxioms(cls));
			}
			
			if (subClassAxioms != null) {
				for(OWLSubClassOfAxiom ax : subClassAxioms) {
					addShuntNodeAndEdge(ax.getSubClass(), ax.getSuperClass(), shunt, nodes);
					if (edgeLimit > 0 && shunt.edges.size() >= edgeLimit) {
						return shunt;
					}
				}
			}
			for(OWLEquivalentClassesAxiom ax : equivAxioms) {
				for(OWLClassExpression ce : ax.getClassExpressions()) {
					
					if (x.equals(ce)) {
						continue;
					}
					else if (ce instanceof OWLObjectIntersectionOf) {
						OWLObjectIntersectionOf intersection = (OWLObjectIntersectionOf) ce;
						for(OWLClassExpression op :  intersection.getOperands()) {
							addShuntNodeAndEdge(cls, op, shunt, nodes);
							if (edgeLimit > 0 && shunt.edges.size() >= edgeLimit) {
								return shunt;
							}
						}
					}
				}
			}
		}
		return shunt;
	}
	
	private Map<OWLClass, Set<OWLSubClassOfAxiom>> initNeighborAxioms() {
		Map<OWLClass, Set<OWLSubClassOfAxiom>> result = new HashMap<OWLClass, Set<OWLSubClassOfAxiom>>();
		for(OWLOntology ont : getAllOntologies()) {
			for(OWLSubClassOfAxiom ax : ont.getAxioms(AxiomType.SUBCLASS_OF)) {
				Set<OWLClass> inSignature = ax.getClassesInSignature();
				for (OWLClass cls : inSignature) {
					Set<OWLSubClassOfAxiom> neighbors = result.get(cls);
					if (neighbors == null) {
						neighbors = new HashSet<OWLSubClassOfAxiom>();
						result.put(cls, neighbors);
					}
					neighbors.add(ax);
				}
			}
		}
		
		return result;
	}

	private void addShuntNodeAndEdge(OWLClassExpression sourceCE, OWLClassExpression target, OWLShuntGraph shunt,
			Map<OWLClass, OWLShuntNode> allNodes)
	{
		if (sourceCE instanceof OWLClass == false) {
			return;
		}
		OWLClass source = (OWLClass) sourceCE;
		String sourceID;
		OWLShuntNode sourceNode = allNodes.get(source);
		if (sourceNode == null) {
			sourceID = getIdentifier(source);
			sourceNode = new OWLShuntNode(sourceID, getLabel(source));
			allNodes.put(source, sourceNode);
			shunt.addNode(sourceNode);
		}
		else {
			sourceID = sourceNode.getId();
		}
		if (target instanceof OWLClass) {
			OWLClass superCls = target.asOWLClass();
			OWLShuntNode node = allNodes.get(superCls);
			String nodeId;
			if (node == null) {
				nodeId = getIdentifier(superCls);
				String nodeLabel = getLabel(superCls);
				node = new OWLShuntNode(nodeId, nodeLabel);
				allNodes.put(superCls, node);
				shunt.addNode(node);
			}
			else {
				nodeId = node.getId();
			}
			shunt.addEdge(new OWLShuntEdge(sourceID, nodeId, "is_a"));
		}
		else if (target instanceof OWLObjectSomeValuesFrom) {
			OWLObjectSomeValuesFrom svf = (OWLObjectSomeValuesFrom) target;
			OWLClassExpression filler = svf.getFiller();
			if (filler instanceof OWLClass) {
				OWLClass superCls = filler.asOWLClass();
				OWLShuntNode node = allNodes.get(superCls);
				String nodeId;
				if (node == null) {
					nodeId = getIdentifier(superCls);
					String nodeLabel = getLabel(superCls);
					node = new OWLShuntNode(nodeId, nodeLabel);
					allNodes.put(superCls, node);
					shunt.addNode(node);
				}
				else {
					nodeId = node.getId();
				}
				String rel = getIdentifier(svf.getProperty());
				shunt.addEdge(new OWLShuntEdge(sourceID, nodeId, rel));
			}
		}
	}
}

