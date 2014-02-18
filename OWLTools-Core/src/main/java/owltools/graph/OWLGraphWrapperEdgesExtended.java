package owltools.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;

import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLSubAnnotationPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubObjectPropertyOfAxiom;
import org.semanticweb.owlapi.model.UnknownOWLOntologyException;

import owltools.graph.OWLGraphEdge.OWLGraphEdgeSet;

/**
 * This class groups methods that could be modified, or added 
 * to <code>OWLGraphWrapper</code> and parent classes.
 * 
 * @author Frederic Bastian
 * @version January 2014
 * @since November 2013
 *
 */
public class OWLGraphWrapperEdgesExtended extends OWLGraphWrapperEdges {
	
	/**
	 * A cache for super property relations. Each <code>OWLObjectPropertyExpression</code> 
	 * is associated in the <code>Map</code> to a <code>LinkedHashSet</code> of 
	 * <code>OWLObjectPropertyExpression</code>s, that contains its super properties, 
	 * ordered from the more specific to the more general 
	 * (for instance, "in_deep_part_of", then "part_of", then "overlaps").
	 * @see #getSuperPropertyReflexiveClosureOf(OWLObjectPropertyExpression)
	 */
	private Map<OWLObjectPropertyExpression, LinkedHashSet<OWLObjectPropertyExpression>>
	    superPropertyCache;
	
	/**
	 * A cache for sub-property relations. Each <code>OWLObjectPropertyExpression</code> 
	 * is associated in the <code>Map</code> to a <code>LinkedHashSet</code> of 
	 * <code>OWLObjectPropertyExpression</code>s, that contains its sub-properties, 
	 * ordered from the more general to the more specific 
	 * (for instance, "overlaps", then "part_of", then "in_deep_part_of").
	 * @see #getSubPropertyClosureOf(OWLObjectPropertyExpression)
	 */
	private Map<OWLObjectPropertyExpression, LinkedHashSet<OWLObjectPropertyExpression>>
	    subPropertyCache;

    /**
     * A cache for relations to sub annotation properties. Each <code>OWLAnnotationProperty</code> 
     * is associated in the <code>Map</code> to a <code>LinkedHashSet</code> of 
     * <code>OWLAnnotationProperty</code>s, that contains its sub-properties, 
     * ordered from the more general to the more specific 
     * @see #getSubAnnotationPropertyClosureOf(OWLAnnotationProperty)
     */
    private Map<OWLAnnotationProperty, LinkedHashSet<OWLAnnotationProperty>>
        subAnnotationPropertyCache;

	/**
	 * Default constructor. 
	 * @param ontology 		The <code>OWLOntology</code> that this object wraps.
	 * @throws UnknownOWLOntologyException 	
	 * @throws OWLOntologyCreationException
	 */
	public OWLGraphWrapperEdgesExtended(OWLOntology ontology)
			throws UnknownOWLOntologyException, OWLOntologyCreationException {
		super(ontology);
    	this.subPropertyCache = new HashMap<OWLObjectPropertyExpression, 
    			LinkedHashSet<OWLObjectPropertyExpression>>();
        this.subAnnotationPropertyCache = new HashMap<OWLAnnotationProperty, 
                LinkedHashSet<OWLAnnotationProperty>>();
    	this.superPropertyCache = new HashMap<OWLObjectPropertyExpression, 
    			LinkedHashSet<OWLObjectPropertyExpression>>();
	}
	
	protected OWLGraphWrapperEdgesExtended(String iri)
			throws UnknownOWLOntologyException, OWLOntologyCreationException {
		super(iri);
    	this.subPropertyCache = new HashMap<OWLObjectPropertyExpression, 
    			LinkedHashSet<OWLObjectPropertyExpression>>();
    	this.superPropertyCache = new HashMap<OWLObjectPropertyExpression, 
    			LinkedHashSet<OWLObjectPropertyExpression>>();
	}
   	
    /**
     * Determine if <code>testObject</code> belongs to at least one of the subsets 
     * in <code>subsets</code>. 
     * 
     * @param testObject	An <code>OWLObject</code> for which we want to know if it belongs 
     * 						to a subset in <code>subsets</code>.
     * @param subsets		A <code>Collection</code> of <code>String</code>s that are 
     * 						the names of the subsets for which we want to check belonging 
     * 						of <code>testObject</code>.
     * @return				<code>true</code> if <code>testObject</code> belongs to a subset 
     * 						in <code>subsets</code>, <code>false</code> otherwise.
     */
    public boolean isOWLObjectInSubsets(OWLObject testObject, Collection<String> subsets) {
    	return !Collections.disjoint(subsets, this.getSubsets(testObject));
    }
    

    /**
     * Returns the direct child properties of <code>prop</code> in all ontologies.
     * @param prop      The <code>OWLAnnotationProperty</code> for which 
     *                  we want the direct sub-properties.
     * @return          A <code>Set</code> of <code>OWLAnnotationProperty</code>s 
     *                  that are the direct sub-properties of <code>prop</code>.
     * 
     * @see #getSubPropertyClosureOf(OWLObjectPropertyExpression)
     * @see #getSubPropertyReflexiveClosureOf(OWLObjectPropertyExpression)
     */
    public Set<OWLAnnotationProperty> getSubAnnotationPropertiesOf(
            OWLAnnotationProperty prop) {
        Set<OWLAnnotationProperty> subProps = new HashSet<OWLAnnotationProperty>();
        for (OWLOntology ont : this.getAllOntologies()) {
            //we need to iterate each annotation property, to get 
            //its getSubAnnotationPropertyOfAxioms and see if prop is its parent 
            //(there is no method "getSuperAnnotationPropertyOfAxioms").
            for (OWLAnnotationProperty subProp : ont.getAnnotationPropertiesInSignature()) {
                for (OWLSubAnnotationPropertyOfAxiom ax: 
                        ont.getSubAnnotationPropertyOfAxioms(subProp)) {
                    if (ax.getSuperProperty().equals(prop)) {
                        subProps.add(subProp);
                    }
                }
            }
        }
        return subProps;
    }
    /**
     * Returns all child properties of <code>prop</code> in all ontologies,  
     * ordered from the more general (closer from <code>prop</code>) to the more precise. 
     * 
     * @param prop  the <code>OWLAnnotationProperty</code> for which we want 
     *              the ordered sub-properties. 
     * @return      A <code>LinkedHashSet</code> of <code>OWLAnnotationProperty</code>s 
     *              ordered from the more general to the more precise.
     * 
     * @see #getSubAnnotationPropertiesOf(OWLAnnotationProperty)
     * @see #getSubAnnotationPropertyReflexiveClosureOf(OWLAnnotationProperty)
     */
    //TODO: DRY, it is almost the same code than getSubPropertyClosureOf
    public LinkedHashSet<OWLAnnotationProperty> getSubAnnotationPropertyClosureOf(
            OWLAnnotationProperty prop) {
        //try to get the sub-properties from the cache
        LinkedHashSet<OWLAnnotationProperty> subProps = 
                this.subAnnotationPropertyCache.get(prop);
        if (subProps != null) {
            return subProps;
        }
        subProps = new LinkedHashSet<OWLAnnotationProperty>();
        Stack<OWLAnnotationProperty> stack = new Stack<OWLAnnotationProperty>();
        stack.add(prop);
        while (!stack.isEmpty()) {
            OWLAnnotationProperty nextProp = stack.pop();
            Set<OWLAnnotationProperty> directSubs = this.getSubAnnotationPropertiesOf(nextProp);
            directSubs.removeAll(subProps);
            stack.addAll(directSubs);
            subProps.addAll(directSubs);
        }
        //put the sub-properties in cache
        this.subAnnotationPropertyCache.put(prop, subProps);
        
        return subProps;
    }
    /**
     * Returns all sub-properties of <code>prop</code> in all ontologies, 
     * and <code>prop</code> itself as the first element (reflexive). 
     * The returned sub-properties are ordered from the more general (the closest 
     * from <code>prop</code>) to the more precise.
     * 
     * @param prop  the <code>OWLAnnotationProperty</code> for which we want 
     *              the ordered sub-properties. 
     * @return      A <code>LinkedHashSet</code> of <code>OWLAnnotationProperty</code>s 
     *              ordered from the more general to the more precise, with <code>prop</code> 
     *              as the first element. 
     * 
     * @see #getSubAnnotationPropertiesOf(OWLAnnotationProperty)
     * @see #getSubAnnotationPropertyClosureOf(OWLAnnotationProperty)
     */
    public LinkedHashSet<OWLAnnotationProperty> getSubAnnotationPropertyReflexiveClosureOf(
            OWLAnnotationProperty prop) {
        
        LinkedHashSet<OWLAnnotationProperty> subProps = 
                new LinkedHashSet<OWLAnnotationProperty>();
        
        subProps.add(prop);
        subProps.addAll(this.getSubAnnotationPropertyClosureOf(prop));
        
        return subProps;
    }
    
    /**
	 * Returns the direct child properties of <code>prop</code> in all ontologies.
	 * @param prop 		The <code>OWLObjectPropertyExpression</code> for which 
	 * 					we want the direct sub-properties.
	 * @return 			A <code>Set</code> of <code>OWLObjectPropertyExpression</code>s 
	 * 					that are the direct sub-properties of <code>prop</code>.
     * 
     * @see #getSubPropertyClosureOf(OWLObjectPropertyExpression)
     * @see #getSubPropertyReflexiveClosureOf(OWLObjectPropertyExpression)
	 */
	public Set<OWLObjectPropertyExpression> getSubPropertiesOf(
			OWLObjectPropertyExpression prop) {
		Set<OWLObjectPropertyExpression> subProps = new HashSet<OWLObjectPropertyExpression>();
		for (OWLOntology ont : this.getAllOntologies()) {
			for (OWLSubObjectPropertyOfAxiom axiom : 
				    ont.getObjectSubPropertyAxiomsForSuperProperty(prop)) {
				subProps.add(axiom.getSubProperty());
			}
		}
		return subProps;
	}
	/**
     * Returns all child properties of <code>prop</code> in all ontologies,  
     * ordered from the more general (closer from <code>prop</code>) to the more precise 
     * (e.g., for the "overlaps" property, return "part_of" then "in_deep_part_of"). 
     * 
     * @param prop 	the <code>OWLObjectPropertyExpression</code> for which we want 
     * 				the ordered sub-properties. 
     * @return		A <code>LinkedHashSet</code> of <code>OWLObjectPropertyExpression</code>s 
     * 				ordered from the more general to the more precise.
     * 
     * @see #getSubPropertiesOf(OWLObjectPropertyExpression)
     * @see #getSubPropertyReflexiveClosureOf(OWLObjectPropertyExpression)
     */
	public LinkedHashSet<OWLObjectPropertyExpression> getSubPropertyClosureOf(
			OWLObjectPropertyExpression prop) {
		//try to get the sub-properties from the cache
		LinkedHashSet<OWLObjectPropertyExpression> subProps = 
				this.subPropertyCache.get(prop);
		if (subProps != null) {
			return subProps;
		}
    	subProps = new LinkedHashSet<OWLObjectPropertyExpression>();
		Stack<OWLObjectPropertyExpression> stack = new Stack<OWLObjectPropertyExpression>();
		stack.add(prop);
		while (!stack.isEmpty()) {
			OWLObjectPropertyExpression nextProp = stack.pop();
			Set<OWLObjectPropertyExpression> directSubs = this.getSubPropertiesOf(nextProp);
			directSubs.removeAll(subProps);
			stack.addAll(directSubs);
			subProps.addAll(directSubs);
		}
		//put the sub-properties in cache
		this.subPropertyCache.put(prop, subProps);
		
		return subProps;
	}
	/**
     * Returns all sub-properties of <code>prop</code> in all ontologies, 
     * and <code>prop</code> itself as the first element (reflexive). 
     * The returned sub-properties are ordered from the more general (the closest 
     * from <code>prop</code>) to the more precise.
     * For instance, if <code>prop</code> is "overlaps", the returned properties will be  
     * "overlaps", then "part_of", then "in_deep_part_of", .... 
     * 
     * @param prop 	the <code>OWLObjectPropertyExpression</code> for which we want 
     * 				the ordered sub-properties. 
     * @return		A <code>LinkedHashSet</code> of <code>OWLObjectPropertyExpression</code>s 
     * 				ordered from the more general to the more precise, with <code>prop</code> 
     * 				as the first element. 
     * 
     * @see #getSubPropertiesOf(OWLObjectPropertyExpression)
     * @see #getSubPropertyClosureOf(OWLObjectPropertyExpression)
     */
	public LinkedHashSet<OWLObjectPropertyExpression> getSubPropertyReflexiveClosureOf(
			OWLObjectPropertyExpression prop) {
		
		LinkedHashSet<OWLObjectPropertyExpression> subProps = 
				new LinkedHashSet<OWLObjectPropertyExpression>();
		
		subProps.add(prop);
		subProps.addAll(this.getSubPropertyClosureOf(prop));
		
		return subProps;
	}
	
    /**
     * Returns all parent properties of <code>prop</code> in all ontologies, 
     * and <code>prop</code> itself as the first element (reflexive). 
     * Unlike the method <code>owltools.graph.OWLGraphWrapperEdges.getSuperPropertyReflexiveClosureOf</code>, 
     * the returned super properties here are ordered from the more precise to the more general 
     * (e.g., "in_deep_part_of", then "part_of", then "overlaps"). 
     * 
     * @param prop 	the <code>OWLObjectPropertyExpression</code> for which we want 
     * 				the ordered super properties. 
     * @return		A <code>LinkedHashSet</code> of <code>OWLObjectPropertyExpression</code>s 
     * 				ordered from the more precise to the more general, with <code>prop</code> 
     * 				as the first element. 
     */
	//TODO: Remove if OWLGraphWrapper changes its implementation
    public LinkedHashSet<OWLObjectPropertyExpression> getSuperPropertyReflexiveClosureOf(
    		OWLObjectPropertyExpression prop) {
    	
    	//try to get the super properties from the cache
    	LinkedHashSet<OWLObjectPropertyExpression> superProps = 
    			this.superPropertyCache.get(prop);
    	if (superProps == null) {

    		superProps = new LinkedHashSet<OWLObjectPropertyExpression>();
    		Stack<OWLObjectPropertyExpression> stack = 
    				new Stack<OWLObjectPropertyExpression>();
    		stack.add(prop);
    		while (!stack.isEmpty()) {
    			OWLObjectPropertyExpression nextProp = stack.pop();
    			Set<OWLObjectPropertyExpression> directSupers = 
    					this.getSuperPropertiesOf(nextProp);
    			directSupers.removeAll(superProps);
    			directSupers.remove(prop);
    			stack.addAll(directSupers);
    			superProps.addAll(directSupers);
    		}
    		//put superProps in cache
    		this.superPropertyCache.put(prop, superProps);
    	}

    	
    	LinkedHashSet<OWLObjectPropertyExpression> superPropsReflexive = 
    			new LinkedHashSet<OWLObjectPropertyExpression>();
    	superPropsReflexive.add(prop);
		superPropsReflexive.addAll(superProps);
		return superPropsReflexive;
	}
    
    /**
	 * Get the sub-relations of <code>edge</code>. This method returns 
	 * <code>OWLGraphEdge</code>s with their <code>OWLQuantifiedProperty</code>s 
	 * corresponding to the sub-properties of the properties of <code>edge</code> 
	 * (even indirect sub-properties), ordered from the more general relations 
	 * (the closest to <code>edge</code>) to the more precise relations. 
	 * The first <code>OWLGraphEdge</code> in the returned <code>Set</code> 
	 * is <code>edge</code> (reflexive method).
	 * <p>
	 * This is the opposite method of 
	 * <code>owltools.graph.OWLGraphWrapperEdges.getOWLGraphEdgeSubsumers(OWLGraphEdge)</code>, 
	 * with reflexivity added.
	 * 
	 * @param edge	A <code>OWLGraphEdge</code> for which all sub-relations 
	 * 				should be obtained.
	 * @return 		A <code>Set</code> of <code>OWLGraphEdge</code>s representing 
	 * 				the sub-relations of <code>edge</code> ordered from the more general 
	 * 				to the more precise relation, with <code>edge</code> as the first element. 
	 * 				An empty <code>Set</code> if the <code>OWLQuantifiedProperty</code>s 
	 * 				of <code>edge</code> have no sub-properties.
	 */
	public LinkedHashSet<OWLGraphEdge> getOWLGraphEdgeSubRelsReflexive(OWLGraphEdge edge) {
		return this.getOWLGraphEdgeSubRelsReflexive(edge, 0);
	}
	
	/**
	 * Similar to {@link getOWLGraphEdgeSubRels(OWLGraphEdge)}, 
	 * except the <code>OWLQuantifiedProperty</code>s of <code>edge</code> are analyzed 
	 * starting from the index <code>propIndex</code>.
	 * 
	 * @param edge 		A <code>OWLGraphEdge</code> for which sub-relations 
	 * 					should be obtained, with properties analyzed from index 
	 * 					<code>propIndex</code>
	 * @param propIndex	An <code>int</code> representing the index of the 
	 * 					<code>OWLQuantifiedProperty</code> of <code>edge</code> 
	 * 					to start the analysis with.
	 * @return 		A <code>Set</code> of <code>OWLGraphEdge</code>s representing 
	 * 				the sub-relations of <code>edge</code> ordered from the more general 
	 * 				to the more precise relation, with <code>edge</code> as the first element, 
	 * 				and with only <code>OWLQuantifiedProperty</code> starting at index 
	 * 				<code>propIndex</code>. An empty <code>Set</code> 
	 * 				if the <code>OWLQuantifiedProperty</code>s of <code>edge</code> 
	 * 				have no sub-properties.
	 */
	private LinkedHashSet<OWLGraphEdge> getOWLGraphEdgeSubRelsReflexive(OWLGraphEdge edge, 
			int propIndex) {
	    //we do not use a mechanism such as provided by OWLGraphEdge.OWLGraphEdgeSet, 
	    //first because we need a LinkedHashSet, not a HashSet, second because 
	    //by definition, subRels will not be populated with potentially equal edges.
		LinkedHashSet<OWLGraphEdge> subRels = new LinkedHashSet<OWLGraphEdge>();
		if (propIndex >= edge.getQuantifiedPropertyList().size()) {
		    subRels.add(new OWLGraphEdge(edge.getSource(), edge.getTarget(), 
		            new Vector<OWLQuantifiedProperty>(), edge.getOntology(), 
		            edge.getAxioms()));
		    return subRels;
		}
		OWLQuantifiedProperty quantProp = edge.getQuantifiedPropertyList().get(propIndex);
		LinkedHashSet<OWLQuantifiedProperty> subQuantProps = 
		        new LinkedHashSet<OWLQuantifiedProperty>();
		subQuantProps.add(quantProp);
		OWLObjectProperty prop = quantProp.getProperty();
		if (prop != null) {
		    for (OWLObjectPropertyExpression propExp : this.getSubPropertyClosureOf(prop)) {
		        if (propExp.equals(this.getDataFactory().
		                getOWLTopObjectProperty()))
		            continue;
		        if (propExp instanceof OWLObjectProperty) {
		            OWLQuantifiedProperty newQp = 
		                    new OWLQuantifiedProperty(propExp, quantProp.getQuantifier());
		            boolean isExcluded = isExcluded(newQp);
		            if (!isExcluded) {
		                subQuantProps.add(newQp);
		            }
		        }
		    }
		}
		for (OWLQuantifiedProperty subQuantProp : subQuantProps) {
		    for (OWLGraphEdge nextPropEdge : this.getOWLGraphEdgeSubRelsReflexive(edge, 
		            propIndex+1)) {
		        List<OWLQuantifiedProperty> quantProps = new Vector<OWLQuantifiedProperty>();
		        quantProps.add(subQuantProp);
		        quantProps.addAll(nextPropEdge.getQuantifiedPropertyList());

		        subRels.add(new OWLGraphEdge(edge.getSource(),edge.getTarget(),
		                quantProps, edge.getOntology(), edge.getAxioms()));
		    }
		}

		return subRels;
	}
	
    
    /**
     * Combines <code>firstEdge</code> and <code>secondEdge</code> to create a new edge 
     * from the source of <code>firstEdge</code> to the target of <code>secondEdge</code>, 
     * with properties combined in a regular way, and over super properties.
     * <p>
     * This method is similar to 
     * <code>owltools.graph.OWLGraphWrapperEdges#combineEdgePair(OWLObject, OWLGraphEdge, 
     * OWLGraphEdge, int)</code>, 
     * except it also tries to combine the <code>OWLQuantifiedProperty</code>s of the edges 
     * over super properties (see {@link #combinePropertyPairOverSuperProperties(
     * OWLQuantifiedProperty, OWLQuantifiedProperty)}, currently combines over 
     * 2 properties only). 
     * 
     * @param firstEdge		A <code>OWLGraphEdge</code> that is the first edge to combine, 
     * 						its source will be the source of the new edge
     * @param secondEdge	A <code>OWLGraphEdge</code> that is the second edge to combine, 
     * 						its target will be the target of the new edge
     * @return 				A <code>OWLGraphEdge</code> resulting from the composition of 
     * 						<code>firstEdge</code> and <code>secondEdge</code>, 
     * 						with its <code>OWLQuantifiedProperty</code>s composed 
     * 						in a regular way, but also over super properties. 
     */
    public OWLGraphEdge combineEdgePairWithSuperProps(OWLGraphEdge firstEdge, 
    		OWLGraphEdge secondEdge) {
    	OWLGraphEdge combine = 
				this.combineEdgePair(
						firstEdge.getSource(), firstEdge, secondEdge, 0);
		
		if (combine != null) {
			//in case the relations were not combined, try to combine 
			//over super properties
			//TODO: combine over more than 2 properties
			if (combine.getQuantifiedPropertyList().size() == 2) {
				OWLQuantifiedProperty combinedQp = 
						this.combinePropertyPairOverSuperProperties(
								combine.getQuantifiedPropertyList().get(0), 
								combine.getQuantifiedPropertyList().get(1));
				if (combinedQp != null) {
					//successfully combined over super properties, 
					//create a combined edge
					List<OWLQuantifiedProperty>  qps = new ArrayList<OWLQuantifiedProperty>();
					qps.add(combinedQp);
					combine = this.createMergedEdge(firstEdge.getSource(), firstEdge, secondEdge);
					combine.setQuantifiedPropertyList(qps);
				}
			}
		}
		
		return combine;
    }
    
    /**
     * Perform a combination of a pair of <code>OWLQuantifiedProperty</code>s 
     * over super properties, unlike the method 
     * <code>owltools.graph.OWLGraphWrapperEdges.combinedQuantifiedPropertyPair</code>. 
     * <strong>Warning: </strong> note that you should call this method only after 
     * <code>combinedQuantifiedPropertyPair</code> failed to combine properties. 
     * <p>
     * This methods determines if <code>prop1</code> is a super property 
     * of <code>prop2</code> that can be combined, or <code>prop2</code> a super property 
     * of <code>prop1</code> that can be combined, 
     * or if they have a super property in common that can be combined. 
     * If such a suitable super property is identified, <code>prop1</code> and 
     * <code>prop2</code> are combined by calling the method 
     * <code>owltools.graph.OWLGraphWrapperEdges.combinedQuantifiedPropertyPair</code> 
     * on that super property, as a pair (notably to check for transitivity). 
     * All super properties will be sequentially tested from the more precise one 
     * to the more general one, trying to find one that can be combined. 
     * If no combination can be performed, return <code>null</code>.
     * <p>
     * For example: 
     * <ul>
     * <li>If property r2 is transitive, and is the super property of r1, then 
     * A r1 B * B r2 C --> A r2 C
     * <li>If property r3 is transitive, and is the super property of both r1 and r2, then 
     * A r1 B * B r2 C --> A r3 C 
     * </ul>
     * 
     * @param prop1 	First <code>OWLQuantifiedProperty</code> to combine
     * @param prop2		Second <code>OWLQuantifiedProperty</code> to combine
     * @return			A <code>OWLQuantifiedProperty</code> representing a combination 
     * 					of <code>prop1</code> and <code>prop2</code> over super properties. 
     * 					<code>null</code> if cannot be combined. 
     */
    OWLQuantifiedProperty combinePropertyPairOverSuperProperties(
            OWLQuantifiedProperty prop1, OWLQuantifiedProperty prop2) {
    	//local implementation of getSuperPropertyReflexiveClosureOf, to order super properties 
    	//from the more precise to the more general, with prop as the first element. 
    	//the first element is the property itself, 
    	//to check if it is a super property of the other property
    	LinkedHashSet<OWLObjectPropertyExpression> superProps1 = 
    			this.getSuperPropertyReflexiveClosureOf(prop1.getProperty());
    	LinkedHashSet<OWLObjectPropertyExpression> superProps2 = 
    			this.getSuperPropertyReflexiveClosureOf(prop2.getProperty());

    	//search for a common super property
    	//TODO: hmm, this looks like an error. Properties can be combined thanks to 
    	//chained composition rules, they do not have to be identical
    	superProps1.retainAll(superProps2);
    	
    	for (OWLObjectPropertyExpression prop: superProps1) {

    	    //code from OWLGraphWrapperEdges.getOWLGraphEdgeSubsumers
    	    if (!prop.equals(
    	            this.getDataFactory().getOWLTopObjectProperty()) && 

    	            prop instanceof OWLObjectProperty) {
    	        OWLQuantifiedProperty newQp = 
    	                new OWLQuantifiedProperty(prop, prop1.getQuantifier());
    	        boolean isExcluded = isExcluded(newQp);
    	        if (!isExcluded) {
    	            OWLQuantifiedProperty combined = combinedQuantifiedPropertyPair(newQp, newQp);
    	            if (combined != null) {
    	                return combined;
    	            }
    	        }
    	    }
    	}
    	return null;
    }
    
    /**
     * Same method as {@link OWLGraphWrapperEdges.getOutgoingEdgesClosure(OWLObject)}, 
     * except that the list of connecting edge properties are not only combined using the 
     * composition rules as usual, but also over super properties (see for instance 
     * {@link #combineEdgePairWithSuperProps(OWLGraphEdge, OWLGraphEdge}).
     * 
     * @param s     The {@code OWLObject} which outgoing edges start from.
     * @return      A {@code Set} of {@code OWLGraphEdge}s that represent 
     *              the graph closure originating from {@code s}, 
     *              with {@code OWLQuantifiedProperty}s combined using 
     *              standard composition rules, but also over super-properties.
     */
    public Set<OWLGraphEdge> getOutgoingEdgesClosureOverSuperProps(OWLObject s) {
        Set<OWLGraphEdge> edges = this.getOutgoingEdgesClosure(s);
        Set<OWLGraphEdge> newEdgesCombined = new OWLGraphEdgeSet();
        
        for (OWLGraphEdge e: edges) {
            OWLGraphEdge newEdge = e;
            
            if (e.getQuantifiedPropertyList().size() > 1) {
                List<OWLQuantifiedProperty> combinedQps = new ArrayList<OWLQuantifiedProperty>();
                
                //we will try to combine a property from the original list with its predecessor, 
                //so we start the iteration at the second property of the original list.
                for (int i = 1; i < e.getQuantifiedPropertyList().size(); i++) {
                    //for the first iteration, get the first property of the original list
                    OWLQuantifiedProperty firstToCombine = e.getQuantifiedPropertyList().get(i - 1);
                    //after the first iteration, we should combine with the properties 
                    //already obtained at previous iterations. 
                    if (!combinedQps.isEmpty()) {
                        //get the latest property combined, and remove it from the List, 
                        //so that it can be replaced with a newly combined property
                        firstToCombine = combinedQps.remove(combinedQps.size() - 1);
                    }
                    
                    //OK, try to combine with the current property iterated
                    OWLQuantifiedProperty secondToCombine = e.getQuantifiedPropertyList().get(i);
                    OWLQuantifiedProperty combined = 
                            this.combinePropertyPairOverSuperProperties(
                                    firstToCombine, secondToCombine);
                    if (combined != null) {
                        combinedQps.add(combined);
                    } else {
                        //if the properties could not be combined, we put back 
                        //in combinedQps the property we removed from it, plus 
                        //the property currently iterated.
                        combinedQps.add(firstToCombine);
                        combinedQps.add(secondToCombine);
                    }
                }
                
                //combine done, let's see if we combined anything
                if (!combinedQps.equals(e.getQuantifiedPropertyList())) {
                    if (combinedQps.size() >= e.getQuantifiedPropertyList().size()) {
                        throw new AssertionError("Property composition should generate " +
                        		"less properties than in the original set of properties.");
                    }
                    
                    newEdge = new OWLGraphEdge(e.getSource(), e.getTarget(), combinedQps, 
                            e.getOntology(), e.getAxioms());
                    newEdge.setDistance(e.getDistance());
                }
            }
            
            newEdgesCombined.add(newEdge);
        }
        
        return newEdgesCombined;
    }

    
    //***************************************
    // CONVENIENT METHODS TO GET OWLCLASSES
    //***************************************
	/**
     * Get all <code>OWLClass</code>es from all ontologies, 
     * that are neither top entity (owl:thing), nor bottom entity (owl:nothing), 
     * nor deprecated ({@link OWLGraphWrapperExtended#isObsolete(OWLObject)} 
     * returns {@code false}).
     * 
     * @return 	a <code>Set</code> containing all "real" <code>OWLClass</code>es 
     *          from all ontologies.
     */
    public Set<OWLClass> getAllOWLClasses() {
    	//maybe classes can be shared between ontologies?
    	//use a Set to check
    	Set<OWLClass> allClasses = new HashSet<OWLClass>();
    	for (OWLOntology ont : this.getAllOntologies()) {
			for (OWLClass cls: ont.getClassesInSignature()) {
			    if (this.isRealClass(cls)) {
				    allClasses.add(cls);
			    }
			}
		}
    	return allClasses;
    }

    /**
     * Get only the <code>OWLClass</code>es from the {@code OWLOntology} returned 
     * by {@link #getSourceOntology()}, that are neither top entity (owl:thing), 
     * nor bottom entity (owl:nothing), nor deprecated ({@link 
     * OWLGraphWrapperExtended#isObsolete(OWLObject)} returns {@code false}).
     * 
     * @return  a <code>Set</code> of <code>OWLClass</code>es from the source ontology, 
     *          owl:thing, owl:nothing, deprecated classes excluded.
     */
    public Set<OWLClass> getAllOWLClassesFromSource() {
        Set<OWLClass> allClasses = new HashSet<OWLClass>();
        for (OWLClass cls: this.getSourceOntology().getClassesInSignature()) {
            if (this.isRealClass(cls)) {
                allClasses.add(cls);
            }
        }
        return allClasses;
    }
    
    /**
     * Return the <code>OWLClass</code>es root of any ontology 
     * (<code>OWLClass</code>es with no outgoing edges as returned by 
     * {OWLGraphWrapperEdges#getOutgoingEdges(OWLObject)}), and not deprecated 
     * ({@link OWLGraphWrapperExtended#isObsolete(OWLObject)} returns {@code false})).
     * 
     * @return	A <code>Set</code> of <code>OWLClass</code>es that are 
     * 			the roots of any ontology.
     */
    public Set<OWLClass> getOntologyRoots() {
    	Set<OWLClass> ontRoots = new HashSet<OWLClass>();
    	//TODO: modify OWLGraphWrapperEdges so that it could be possible to obtain 
    	//edges incoming to owl:thing. This would be much cleaner to get the roots.
    	for (OWLOntology ont: this.getAllOntologies()) {
			for (OWLClass cls: ont.getClassesInSignature()) {
				if (this.isRealClass(cls) && this.getOutgoingEdges(cls).isEmpty()) {
					ontRoots.add(cls);
				}
			}
		}
    	return ontRoots;
    }
    
    /**
     * Return the <code>OWLClass</code>es leaves of any ontology 
     * (<code>OWLClass</code>es with no incoming edges as returned by 
     * {OWLGraphWrapperEdges#getIncomingEdges(OWLObject)}), and not deprecated 
     * ({@link OWLGraphWrapperExtended#isObsolete(OWLObject)} returns {@code false})
     * 
     * @return  A <code>Set</code> of <code>OWLClass</code>es that are 
     *          the leaves of any ontology.
     */
    public Set<OWLClass> getOntologyLeaves() {
        Set<OWLClass> ontLeaves = new HashSet<OWLClass>();
        for (OWLOntology ont: this.getAllOntologies()) {
            for (OWLClass cls: ont.getClassesInSignature()) {
                if (this.isRealClass(cls) && this.getIncomingEdges(cls).isEmpty()) {
                    ontLeaves.add(cls);
                }
            }
        }
        return ontLeaves;
    }
    
    /**
     * Return the <code>OWLClass</code>es descendant of <code>parentClass</code>.
     * This method is the same than 
     * <code>owltools.graph.OWLGraphWrapperEdges.getDescendants(OWLObject)</code>, 
     * except it returns only <code>OWLClass</code>es.
     * 
     * @param parentClass 
     * @return 	A <code>Set</code> of <code>OWLClass</code>es being the descendants 
     * 			of <code>parentClass</code>.
     */
    public Set<OWLClass> getOWLClassDescendants(OWLClass parentClass) {
    	Set<OWLClass> descendants = new HashSet<OWLClass>();
		for (OWLObject descendant: 
			    this.getDescendants(parentClass)) {
			if (this.isRealClass(descendant)) {
				descendants.add((OWLClass) descendant);
			}
		}
		
		return descendants;
    }
    /**
     * Return the <code>OWLClass</code>es directly descendant of <code>parentClass</code>.
     * This method returns all sources of all edges incoming to <code>parentClass</code>, 
     * that are <code>OWLClass</code>es.
     * 
     * @param parentClass   The {@code OWLClass} for which we want the direct 
     *                      descendant {@code OWLClass}es
     * @return 	A <code>Set</code> of <code>OWLClass</code>es being the direct descendants 
     * 			of <code>parentClass</code>.
     * @see owltools.graph.OWLGraphWrapperEdges#getIncomingEdges(OWLObject)
     */
    public Set<OWLClass> getOWLClassDirectDescendants(OWLClass parentClass) {
    	
    	Set<OWLClass> directDescendants = new HashSet<OWLClass>();
    	for (OWLGraphEdge incomingEdge: 
    		    this.getIncomingEdges(parentClass)) {

    		OWLObject directDescendant = incomingEdge.getSource();
    		if (this.isRealClass(directDescendant)) { 
    			directDescendants.add((OWLClass) directDescendant);
    		}
    	}
		
    	return directDescendants;
    }
    /**
     * Return the <code>OWLClass</code>es that are direct parent of <code>subClass</code>.
     * This method returns all sources of all edges incoming to <code>subClass</code>, 
     * that are <code>OWLClass</code>es.
     * 
     * @param subClass 
     * @return  A <code>Set</code> of <code>OWLClass</code>es being the direct parents 
     *          of <code>subClass</code>.
     * @see owltools.graph.OWLGraphWrapperEdges#getOutgoingEdges(OWLObject)
     */
    public Set<OWLClass> getOWLClassDirectAncestors(OWLClass subClass) {
        
        Set<OWLClass> directParents = new HashSet<OWLClass>();
        for (OWLGraphEdge outgoingEdge: 
                this.getOutgoingEdges(subClass)) {

            OWLObject directParent = outgoingEdge.getTarget();
            if (this.isRealClass(directParent)) { 
                directParents.add((OWLClass) directParent);
            }
        }
        
        return directParents;
    }
    
    
    /**
     * Return the <code>OWLClass</code>es ancestor of <code>sourceClass</code>.
     * This method is the same than 
     * <code>owltools.graph.OWLGraphWrapperEdges.getAncestors(OWLObject)</code>, 
     * except it returns only the ancestor that are <code>OWLClass</code>es.
     * 
     * @param sourceClass 
     * @return 	A <code>Set</code> of <code>OWLClass</code>es being the ancestors 
     * 			of <code>sourceClass</code>.
     */
    public Set<OWLClass> getOWLClassAncestors(OWLClass sourceClass) {
    	
    	Set<OWLClass> ancestors = new HashSet<OWLClass>();
		for (OWLObject ancestor: 
			    this.getAncestors(sourceClass)) {
			if (this.isRealClass(ancestor)) {
				ancestors.add((OWLClass) ancestor);
			}
		}
		
		return ancestors;
    }
    
    /**
     * Determines that {@code object} is an {@code OWLClass} that is neither owl:thing, 
     * nor owl:nothing, and that it is not deprecated 
     * ({@link OWLGraphWrapperExtended#isObsolete(OWLObject)} returns {@code false}).
     * 
     * @param object    An {@code OWLObject} to be checked to be an {@code OWLClass} 
     *                  actually used.
     * @return          {@code true} if {@code object} is an {@code OWLClass} that is not 
     *                  owl:thing, nor owl:nothing, and is not deprecated.
     */
    private boolean isRealClass(OWLObject object) {
        return (object instanceof OWLClass) && !isObsolete(object) && 
                !object.isTopEntity() && !object.isBottomEntity();
    }
}
