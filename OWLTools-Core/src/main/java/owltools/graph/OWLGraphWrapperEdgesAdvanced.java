package owltools.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLPropertyExpression;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import owltools.graph.shunt.OWLShuntEdge;
import owltools.graph.shunt.OWLShuntGraph;
import owltools.graph.shunt.OWLShuntNode;

/**
 * @see OWLGraphWrapper
 * @see OWLGraphWrapperEdges
 */
public class OWLGraphWrapperEdgesAdvanced extends OWLGraphWrapperEdgesExtended {

	private static Logger LOG = Logger.getLogger(OWLGraphWrapper.class);

	protected OWLGraphWrapperEdgesAdvanced(OWLOntology ontology) {
		super(ontology);
	}

	protected OWLGraphWrapperEdgesAdvanced(String iri) throws OWLOntologyCreationException {
		super(iri);
	}

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

	/**
	 * Convert a list of relationship IDs to a hash set of OWLObjectProperties.
	 * 
	 * @param relation_ids
	 * @return property hash
	 * @see #getRelationClosureMapEngine(OWLObject, List)
	 */
	public Set<OWLPropertyExpression> relationshipIDsToPropertySet(List<String> relation_ids){
		Set<OWLPropertyExpression> props = new HashSet<OWLPropertyExpression>();
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
	 * @see #getRelationClosureMap(OWLObject, List)
	 * @see #addDirectDescendentsToShuntGraph(OWLObject, OWLShuntGraph)
	 * @see #addStepwiseAncestorsToShuntGraph(OWLObject, OWLShuntGraph)
	 * @see #addTransitiveAncestorsToShuntGraph(OWLObject, OWLShuntGraph)
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
	 * @return the modified OWLShuntGraph
	 */
	public OWLShuntGraph addStepwiseAncestorsToShuntGraph(OWLObject x, OWLShuntGraph g, List<String> rel_ids) {

		// Add this node, our seed.
		String topicID = getIdentifier(x);
		String topicLabel = getLabel(x);
		OWLShuntNode tn = new OWLShuntNode(topicID, topicLabel);
		g.addNode(tn);

		// NEW VERSION
		Set<OWLPropertyExpression> props = relationshipIDsToPropertySet(rel_ids);
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
	 * @return the modified OWLShuntGraph
	 */
	public OWLShuntGraph addTransitiveAncestorsToShuntGraph(OWLObject x, OWLShuntGraph g, List<String> rel_ids) {

		// Add this node, our seed.
		String topicID = getIdentifier(x);
		String topicLabel = getLabel(x);
		OWLShuntNode tn = new OWLShuntNode(topicID, topicLabel);
		g.addNode(tn);

		// NEW VERSION
		Set<OWLPropertyExpression> props = relationshipIDsToPropertySet(rel_ids);
		Set<OWLGraphEdge> oge = getOutgoingEdgesClosure(x, props);
		for( OWLGraphEdge e : oge ){
			OWLObject target = e.getTarget();
			
			String rel = classifyRelationship(e, target, props);
			//LOG.info("id: " + getIdentifier(target) + ", " + rel);

			if( rel != null ){
				//LOG.info("\tclass" + rel);

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

				//LOG.info("\t(" + objectID + ", " + objectLabel + "): " + elabel);

				// Only add when subject, object, and relation are properly defined.
				if(	elabel != null &&
					topicID != null && ! topicID.equals("") &&
					objectID != null &&	! objectID.equals("") ){
				
					// Add the node.
					OWLShuntNode on = new OWLShuntNode(objectID, objectLabel);
					g.addNode(on);
					//LOG.info("\tadding node: " + objectID + ": "+ Boolean.toString(g.hasNode(on)));

					// And the edges.
					OWLShuntEdge se = new OWLShuntEdge(topicID, objectID, elabel);
					g.addEdge(se);
				}
			}
		}
		
// ORIGINAL VERSION
//		// Next, get all of the named ancestors and add them to our shunt graph.
//		// We need some traversal code going up!
//		for (OWLGraphEdge e : getOutgoingEdgesClosure(x)) {
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
//				if(	elabel != null &&
//					topicID != null && ! topicID.equals("") &&
//					objectID != null &&	! objectID.equals("") ){
//				
//					// Add the node.
//					OWLShuntNode on = new OWLShuntNode(objectID, objectLabel);
//					g.addNode(on);
//
//					// And the edges.
//					OWLShuntEdge se = new OWLShuntEdge(topicID, objectID, elabel);
//					g.addEdge(se);
//				}
//			}
//		}
	
		return g;
	}

	
	/**
	 * Add a set of edges, as descendants to x in OWLShuntGraph g.
	 * This is reflexive.
	 *
	 * @param x
	 * @param g
	 * @return the modified OWLShuntGraph
	 */
	public OWLShuntGraph addDirectDescendentsToShuntGraph(OWLObject x, OWLShuntGraph g, List<String> rel_ids) {

		// Add this node, our seed.
		String topicID = getIdentifier(x);
		String topicLabel = getLabel(x);
		OWLShuntNode tn = new OWLShuntNode(topicID, topicLabel);
		g.addNode(tn);
		
		// NEW VERSION
		Set<OWLPropertyExpression> props = relationshipIDsToPropertySet(rel_ids);
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
	 * @ param sargs
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
	 * @param c
	 * @param relation_ids
	 * @return map of ids to their displayable labels
	 * @see #getRelationClosureMap(OWLObject, List)
	 */
	public Map<String,String> getRelationClosureMapEngine(OWLObject c, List<String> relation_ids){

		Map<String,String> relation_map = new HashMap<String,String>(); // capture labels/ids

		// Our relation collection.
		Set<OWLPropertyExpression> props = relationshipIDsToPropertySet(relation_ids);
		
		Set<OWLGraphEdge> edges = getOutgoingEdgesClosureReflexive(c, props);
		for (OWLGraphEdge owlGraphEdge : edges) {
			OWLQuantifiedProperty qp = owlGraphEdge.getSingleQuantifiedProperty();
			//if (qp.isSubClassOf() || partOfProperty.equals(qp.getProperty())) {
			if (qp.isSubClassOf() || props.contains(qp.getProperty())) {
				OWLObject target = owlGraphEdge.getTarget();
				if (target instanceof OWLClass) {
					final String id = getIdentifier(target);
					final String label = getLabelOrDisplayId(target);
					relation_map.put(id, label);
				}else if (target instanceof OWLObjectSomeValuesFrom) {
					OWLObjectSomeValuesFrom some = (OWLObjectSomeValuesFrom)target;
					if (props.contains(some.getProperty())) {
						OWLClassExpression clsexp = some.getFiller();
						if( ! clsexp.isAnonymous()){
							OWLClass cls = clsexp.asOWLClass();
							final String id = getIdentifier(cls);
							final String label = getLabelOrDisplayId(cls);
							relation_map.put(id, label);
						}
					}
				}
			}else if (qp.isIdentity()) {
				final String id = getIdentifier(c);
				final String label = getLabelOrDisplayId(c);
				relation_map.put(id, label);
			}else {
				//System.out.println(owlGraphEdge);
			}
		}
		
		return relation_map;
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
		Set<OWLClassExpression> st = cls.getSubClasses(sourceOntology);


		List<String> ar = new ArrayList<String>();
		for(OWLClassExpression ce: st){
			if(ce instanceof OWLNamedObject)
				ar.add(getLabel(ce)); 
		}

		return ar.toArray(new String[ar.size()]);
	}


}

