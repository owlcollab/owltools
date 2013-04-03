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
import org.semanticweb.owlapi.model.UnknownOWLOntologyException;

import owltools.graph.shunt.OWLShuntEdge;
import owltools.graph.shunt.OWLShuntGraph;
import owltools.graph.shunt.OWLShuntNode;

/**
 * @see OWLGraphWrapper
 * @see OWLGraphWrapperEdges
 */
public class OWLGraphWrapperEdgesAdvanced extends OWLGraphWrapperEdges {

	private static Logger LOG = Logger.getLogger(OWLGraphWrapper.class);

	protected OWLGraphWrapperEdgesAdvanced(OWLOntology ontology) throws UnknownOWLOntologyException, OWLOntologyCreationException {
		super(ontology);
	}

	protected OWLGraphWrapperEdgesAdvanced(String iri) throws OWLOntologyCreationException {
		super(iri);
	}

	// A cache of an arbitrary relationship closure for a certain object.
	private Map<OWLObject,Map<ArrayList<String>,Map<String,String>>> mgrcmCache = null;


	/**
	 * Convert a list of relationship IDs to a hash set of OWLObjectProperties.
	 * 
	 * @param relation_ids
	 * @return property hash
	 * @see #getRelationClosureMapEngine(OWLObject, ArrayList)
	 */
	public HashSet<OWLObjectProperty> relationshipIDsToPropertySet(List<String> relation_ids){
		HashSet<OWLObjectProperty> props = new HashSet<OWLObjectProperty>();
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
	 * @param the edge under consideration
	 * @param our properties set
	 * @return null, "simplesubclass", "typesubclass", or "identity".
	 * @see #getRelationClosureMap(OWLObject, ArrayList)
	 * @see #addDirectDescendentsToShuntGraph(OWLObject, OWLShuntGraph)
	 * @see #addStepwiseAncestorsToShuntGraph(OWLObject, OWLShuntGraph)
	 * @see #addTransitiveAncestorsToShuntGraph(OWLObject, OWLShuntGraph)
	 */
	public String classifyRelationship(OWLGraphEdge owlGraphEdge, OWLObject edgeDirector, Set<OWLObjectProperty> props){		
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
			System.out.println(owlGraphEdge);
		}
		
		return retval;
	}

	/**
	 * Add a set of edges, as ancestors to x in OWLShuntGraph g.
	 * This is reflexive.
	 * 
	 * This method uses the regulates relations: 'BFO_0000050', 'RO_0002211', 'RO_0002212', and 'RO_0002213'.
	 *
	 * @param x
	 * @param g
	 * @return the modified OWLShuntGraph
	 */
	public OWLShuntGraph addStepwiseAncestorsToShuntGraph(OWLObject x, OWLShuntGraph g) {

		// Add this node, our seed.
		String topicID = getIdentifier(x);
		String topicLabel = getLabel(x);
		OWLShuntNode tn = new OWLShuntNode(topicID, topicLabel);
		g.addNode(tn);

		// NEW VERSION
		ArrayList<String> rel_ids = new ArrayList<String>();
		rel_ids.add("BFO:0000050");
		rel_ids.add("RO:0002211");
		rel_ids.add("RO:0002212");
		rel_ids.add("RO:0002213");
		HashSet<OWLObjectProperty> props = relationshipIDsToPropertySet(rel_ids);
		for (OWLGraphEdge e : getOutgoingEdges(x)) {
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
						addStepwiseAncestorsToShuntGraph(target, g);
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
	 * This method uses the regulates relations: 'BFO_0000050', 'RO_0002211', 'RO_0002212', and 'RO_0002213'.
	 *
	 * @param x
	 * @param g
	 * @return the modified OWLShuntGraph
	 */
	public OWLShuntGraph addTransitiveAncestorsToShuntGraph(OWLObject x, OWLShuntGraph g) {

		// Add this node, our seed.
		String topicID = getIdentifier(x);
		String topicLabel = getLabel(x);
		OWLShuntNode tn = new OWLShuntNode(topicID, topicLabel);
		g.addNode(tn);

		// NEW VERSION
		ArrayList<String> rel_ids = new ArrayList<String>();
		rel_ids.add("BFO:0000050");
		rel_ids.add("RO:0002211");
		rel_ids.add("RO:0002212");
		rel_ids.add("RO:0002213");
		HashSet<OWLObjectProperty> props = relationshipIDsToPropertySet(rel_ids);
		Set<OWLGraphEdge> oge = getOutgoingEdgesClosure(x);
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

				LOG.info("\t(" + objectID + ", " + objectLabel + "): " + elabel);

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
	 * Add a set of edges, as descendents to x in OWLShuntGraph g.
	 * This is reflexive.
	 * 
	 * This method uses the regulates relations: 'BFO_0000050', 'RO_0002211', 'RO_0002212', and 'RO_0002213'.
	 *
	 * @param x
	 * @param g
	 * @return the modified OWLShuntGraph
	 */
	public OWLShuntGraph addDirectDescendentsToShuntGraph(OWLObject x, OWLShuntGraph g) {

		// Add this node, our seed.
		String topicID = getIdentifier(x);
		String topicLabel = getLabel(x);
		OWLShuntNode tn = new OWLShuntNode(topicID, topicLabel);
		g.addNode(tn);
		
		// NEW VERSION
		ArrayList<String> rel_ids = new ArrayList<String>();
		rel_ids.add("BFO:0000050");
		rel_ids.add("RO:0002211");
		rel_ids.add("RO:0002212");
		rel_ids.add("RO:0002213");
		HashSet<OWLObjectProperty> props = relationshipIDsToPropertySet(rel_ids);
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
	 * Gets all ancestors and direct descendents (distance == 1) that are OWLNamedObjects.
	 * i.e. excludes anonymous class expressions
	 * 
	 * TODO: we're current just doing distance == 1 up;
	 *       we'll want to have a more full graph in the future
	 * TODO: a work in progress
	 * 
	 * @param x
	 * @return set of named ancestors and direct descendents
	 */
	public OWLShuntGraph getSegmentShuntGraph(OWLObject x) {

		// Collection depot.
		OWLShuntGraph graphSegment = new OWLShuntGraph();

		// Add this node, our seed.
		String topicID = getIdentifier(x);
		String topicLabel = getLabel(x);
		OWLShuntNode tn = new OWLShuntNode(topicID, topicLabel);
		graphSegment.addNode(tn);

		// Next, get all of the named ancestors and add them to our shunt graph.
		graphSegment = addStepwiseAncestorsToShuntGraph(x, graphSegment);

		// Next, get all of the immediate descendents.
		graphSegment = addDirectDescendentsToShuntGraph(x, graphSegment);

		//		
		return graphSegment;
	}

//	/**
//	 * Gets all ancestors and direct descendents (distance == 1) that are OWLNamedObjects.
//	 * i.e. excludes anonymous class expressions
//	 * 
//	 * This is a curried FlexLoader s-expression version of {@link #getSegmentShuntGraph(OWLObject)}.
//	 * 
//	 * @param x
//	 * @param sargs
//	 * @return set of named ancestors and direct descendents
//	 * @see #getSegmentShuntGraph(OWLObject)
//	 */
//	public OWLShuntGraph getSegmentShuntGraph(OWLObject x, ArrayList<String> sargs) {
//		return getSegmentShuntGraph(x);
//	}

	/**
	 * Gets all ancestors that are OWLNamedObjects.
	 * i.e. excludes anonymous class expressions
	 * 
	 * This graph information is concerned almost exclusively with the arguments transitive relations with all of its ancestors.
	 * 
	 * @param x
	 * @return set of named ancestors and direct descendents
	 */
	public OWLShuntGraph getLineageShuntGraph(OWLObject x) {

		// Collection depot.
		OWLShuntGraph graphSegment = new OWLShuntGraph();

		// Add this node, our seed.
		String topicID = getIdentifier(x);
		String topicLabel = getLabel(x);
		OWLShuntNode tn = new OWLShuntNode(topicID, topicLabel);
		graphSegment.addNode(tn);

		// Next, get all of the named ancestors and add them to our shunt graph.
		graphSegment = addTransitiveAncestorsToShuntGraph(x, graphSegment);

		//		
		return graphSegment;
	}
	
	/**
	 * Return a JSONized version of the output of getSegmentShuntGraph
	 *
	 * @param x
	 * @return String representing part of the stepwise OWL graph
	 */
	public String getSegmentShuntGraphJSON(OWLObject x) {

		// Collection depot.
		OWLShuntGraph graphSegment = getSegmentShuntGraph(x);

		return graphSegment.toJSON();
	}

	/**
	 * Return a JSONized version of the output of getSegmentShuntGraph
	 *
	 * This is a curried FlexLoader s-expression version of {@link #getSegmentShuntGraphJSON(OWLObject)}.
	 *
	 * @param x
	 * @param sargs
	 * @return String representing part of the stepwise OWL graph
	 * @see #getSegmentShuntGraphJSON(OWLObject)
	 */
	public String getSegmentShuntGraphJSON(OWLObject x, ArrayList<String> sargs) {
		return getSegmentShuntGraphJSON(x);
	}

	/**
	 * Return a JSONized version of the output of getLineageShuntGraph
	 *
	 * @param x
	 * @return String representing part of the transitive OWL graph
	 */
	public String getLineageShuntGraphJSON(OWLObject x) {

		// Collection depot.
		OWLShuntGraph graphSegment = getLineageShuntGraph(x);

		return graphSegment.toJSON();
	}

	/**
	 * Return a JSONized version of the output of getLineageShuntGraph
	 *
	 * This is a curried FlexLoader s-expression version of {@link #getLineageShuntGraphJSON(OWLObject)}.
	 *
	 * @param x
	 * @param sargs
	 * @return String representing part of the transitive OWL graph
	 * @see #getLineageShuntGraphJSON(OWLObject)
	 */
	public String getLineageShuntGraphJSON(OWLObject x, ArrayList<String> sargs) {
		return getLineageShuntGraphJSON(x);
	}	
	
	


	/**
	 * Return a map of id to label for the closure of the ontology using the supplied relation id list and .isSubClassOf().
	 * 
	 * Intended for GOlr loading.
	 * 
	 * @param c
	 * @param relation_ids
	 * @return map of ids to their displayable labels
	 * @see #getRelationClosureMapEngine(OWLObject, ArrayList)
	 */
	public Map<String,String> getRelationClosureMap(OWLObject c, ArrayList<String> relation_ids){

		Map<String,String> retmap = new HashMap<String,String>();

		//private Map<OWLObject,Map<ArrayList<String>,Map<String,String>>> mgrcmCache = null;
		if( mgrcmCache == null ){ // initialize the cache, if necessary
				mgrcmCache = new HashMap<OWLObject,Map<ArrayList<String>,Map<String,String>>>();
		}
		if( mgrcmCache.containsKey(c) == false ){ // assemble level 1, if necessary
			mgrcmCache.put(c, new HashMap<ArrayList<String>,Map<String,String>>());
		}
		if( mgrcmCache.get(c).containsKey(relation_ids) == false ){ // generate
			retmap = getRelationClosureMapEngine(c, relation_ids);
			mgrcmCache.get(c).put(relation_ids, retmap);
		}else{ // return found
			retmap = mgrcmCache.get(c).get(relation_ids);
		}
		
		return retmap;
	}
		
	/**
	 * Generator for the cache in {@link #getRelationClosureMap(OWLObject, ArrayList)}.
	 * 
	 * @param c
	 * @param relation_ids
	 * @return map of ids to their displayable labels
	 * @see #getRelationClosureMap(OWLObject, ArrayList)
	 */
	public Map<String,String> getRelationClosureMapEngine(OWLObject c, ArrayList<String> relation_ids){
	//private Map<String,String> getRelationClosureMapEngine(OWLObject c, ArrayList<String> relation_ids){

		Map<String,String> relation_map = new HashMap<String,String>(); // capture labels/ids

		// Our relation collection.
		HashSet<OWLObjectProperty> props = new HashSet<OWLObjectProperty>();
		//final OWLObjectProperty partOfProperty = getOWLObjectPropertyByIdentifier("BFO:0000050");
		for( String rel_id : relation_ids ){
			props.add(getOWLObjectPropertyByIdentifier(rel_id));
		}
		
		Set<OWLGraphEdge> edges = getOutgoingEdgesClosureReflexive(c);
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
	 * Intended for GOlr loading.
	 * 
	 * This is a curried FlexLoader s-expression version of {@link #getIsaPartofIDClosure(OWLObject)}.
	 * 
	 * @param c
	 * @param sargs
	 * @return list of is_partof_closure ids
	 * @see #getIsaPartofIDClosure(OWLObject)
	 */
	@Deprecated
	public List<String> getIsaPartofIDClosure(OWLObject c, ArrayList<String> sargs){
		return getIsaPartofIDClosure(c);
	}

	/**
	 * Return a overlaps with getIsaPartofLabelClosure and stuff in GafSolrDocumentLoader.
	 * 
	 * Intended for GOlr loading.
	 * 
	 * @param c
	 * @param relation_ids
	 * @return list of is_partof_closure ids
	 */
	public List<String> getRelationIDClosure(OWLObject c, ArrayList<String> relation_ids){
		Map<String, String> foo = getRelationClosureMap(c, relation_ids);
		List<String> bar = new ArrayList<String>(foo.keySet());
		return bar;
	}

	/**
	 * Return a overlaps with getIsaPartofIDClosure and stuff in GafSolrDocumentLoader.
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
	 * Intended for GOlr loading.
	 * 
	 * This is a curried FlexLoader s-expression version of {@link #getIsaPartofLabelClosure(OWLObject)}.
	 * 
	 * @param c
	 * @param sargs
	 * @return list of is_partof_closure labels
	 * @see #getIsaPartofLabelClosure(OWLObject)
	 */
	@Deprecated
	public List<String> getIsaPartofLabelClosure(OWLObject c, ArrayList<String> sargs){
		return getIsaPartofLabelClosure(c);
	}

	/**
	 * Return a overlaps with getIsaPartofIDClosure and stuff in GafSolrDocumentLoader.
	 * Intended for GOlr loading.
	 * 
	 * @param c
	 * @param relation_ids
	 * @return list of is_partof_closure labels
	 */
	public List<String> getRelationLabelClosure(OWLObject c, ArrayList<String> relation_ids){
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


		ArrayList<String> ar = new ArrayList<String>();
		for(OWLClassExpression ce: st){
			if(ce instanceof OWLNamedObject)
				ar.add(getLabel(ce)); 
		}

		return ar.toArray(new String[ar.size()]);
	}


}

