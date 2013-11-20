package owltools.graph;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.model.OWLRestriction;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

import owltools.graph.OWLQuantifiedProperty.Quantifier;

/**
 * Note that when storing {@code OWLGraphEdge}s in a {@code Set}, an {@link OWLGraphEdgeSet} 
 * should be used. 
 */
public class OWLGraphEdge {
    
    /**
     * Represents a {@code Set} of {@code OWLGraphEdge}s. The {@link #add(OWLGraphEdge)} 
     * and {@link #addAll(Collection)} methods allow to merge the underlying axioms 
     * of the {@code OWLGraphEdge}s that are tried to be added with potential equal 
     * {@code OWLGraphEdge} already present in this {@code Set}. 
     * 
     * @author Frederic Bastian
     * @see #OWLGraphEdge#getAxioms()
     */
    public static class OWLGraphEdgeSet extends HashSet<OWLGraphEdge> {
        private static final long serialVersionUID = -3155229334639121333L;
        
        public OWLGraphEdgeSet() {
            super();
        }
        public OWLGraphEdgeSet(OWLGraphEdgeSet edges) {
            super(edges);
        }
        public OWLGraphEdgeSet(Set<OWLGraphEdge> edges) {
            super();
            this.addAll(edges);
        }
        
        /**
         * Try to add or merge each element of {@code edges} into this {@code OWLGraphEdgeSet}, 
         * as performed by the method {@link #add(OWLGraphEdge)}.
         * <p>
         * As a result of this method, some elements of {@code edges} could have 
         * been added to this {@code OWLGraphEdgeSet}, and/or some elements contained 
         * in this {@code OWLGraphEdgeSet} could have been replaced. 
         * 
         * @param edges A {@code Set} of {@code OWLGraphEdge}s to be added to 
         *              this {@code OWLGraphEdgeSet}, or that will provide their underlying 
         *              {@code OWLAxiom}s to potential equal edges already existing.
         * @return      A {@code boolean} always {@code true} to comply to 
         *              the {@code Set} signature.
         */
        @Override
        public boolean addAll(Collection<? extends OWLGraphEdge> edges) {
            for (OWLGraphEdge e: edges) {
                this.add(e);
            }
            return true;
        }
        /**
         * Try to add {@code edge} to this {@code OWLGraphEdgeSet}, or, if it already 
         * contains an equal {@code OWLGraphEdge}, merges their underlying {@code OWLAxiom}s 
         * (as returned by their {@code getOWLAxioms} methods).
         * <p>
         * As a result of this method, either this {@code OWLGraphEdgeSet} would contain 
         * one more element, or one of the element it contains would have been replaced.
         * 
         * @param edge  An {@code OWLGraphEdge} to be added to this {@code OWLGraphEdgeSet}, 
         *              or that will provide its underlying {@code OWLAxiom}s to potential 
         *              equal edge already existing.
         * @return      A {@code boolean} always {@code true} to comply to 
         *              the {@code Set} signature.
         */
        @Override
        public boolean add(OWLGraphEdge edge) {
            if (!super.add(edge)) {
                OWLGraphEdge toMerge = null;
                for (OWLGraphEdge e: this) {
                    if (e.equals(edge)) {
                        toMerge = e;
                        break;
                    }
                }
                super.remove(toMerge);
                super.add(toMerge.merge(edge));
            } 
            return true;
        }
    }
	
	private OWLObject source;
	private OWLObject target;
	private OWLOntology ontology;
	private int distance = 1;
	private List<OWLQuantifiedProperty> quantifiedPropertyList = new Vector<OWLQuantifiedProperty>();
	/**
	 * A {@code Set} containing the underlying {@code OWLAxiom}s that allowed 
	 * to produce this {@code OWLGraphEdge}. This is because a same {@code OWLGraphEdge} 
	 * (meaning, with the same {@code source}, {@code target}, {@code quantifiedPropertyList}, 
	 * {@code ontology}, see {@code hashCode} and {@code equals} methods) can be produced 
	 * by different {@code OWLAxiom}s. For instance, an {@code OWLEquivalentClassesAxiom} 
	 * and a {@code OWLSubClassOfAxiom} could produce equal {@code OWLGraphEdge}s; or 
	 * two structurally equivalent {@code OWLSubClassOfAxiom}s, but with different 
	 * {@code OWLAnnotation}s.
	 */
	private Set<OWLAxiom> underlyingAxioms = new HashSet<OWLAxiom>();
	
	public OWLGraphEdge(OWLObject source, OWLObject target,
			OWLOntology ontology, OWLQuantifiedProperty qp) {
		super();
		this.source = source;
		this.target = target;
		this.ontology = ontology;
		setSingleQuantifiedProperty(qp);
	}

	public OWLGraphEdge(OWLRestriction<?, ?, ?> s, OWLObject t,
			OWLQuantifiedProperty el, OWLOntology o) {
		super();
		this.source = s;
		this.target = t;
		this.ontology = o;
		setSingleQuantifiedProperty(el);
	}

	public OWLGraphEdge(OWLObject source, OWLObject target, List<OWLQuantifiedProperty> qpl, OWLOntology ontology) {
		super();
		this.source = source;
		this.target = target;
		this.ontology = ontology;
		this.quantifiedPropertyList = qpl;
	}

    public OWLGraphEdge(OWLObject source, OWLObject target, List<OWLQuantifiedProperty> qpl, 
            OWLOntology ontology, Set<OWLAxiom> axioms) {
        this(source, target, qpl, ontology);
        this.underlyingAxioms.addAll(axioms);
    }

	
	public OWLGraphEdge(OWLObject source, OWLObject target, OWLOntology ontology) {
		super();
		this.source = source;
		this.target = target;
		this.ontology = ontology;
		setSingleQuantifiedProperty(new OWLQuantifiedProperty(Quantifier.SUBCLASS_OF)); // defaults to subclass
	}
	
    public OWLGraphEdge(OWLObject source, OWLObject target, OWLOntology ontology, 
            Set<OWLAxiom> underlyingAxioms) {
        this(source, target, ontology);
        this.underlyingAxioms.addAll(underlyingAxioms);
    }


	public OWLGraphEdge(OWLObject source, OWLObject target) {
		super();
		this.source = source;
		this.target = target;
	}


	public OWLGraphEdge(OWLObject s, OWLObject t, OWLObjectPropertyExpression p,
			Quantifier q, OWLOntology o) {
		super();
		OWLQuantifiedProperty el = new OWLQuantifiedProperty(p,q);
		this.source = s;
		this.target = t;
		this.ontology = o;
		setSingleQuantifiedProperty(el);
	}
	
	public OWLGraphEdge(OWLObject s, OWLObject t, OWLObjectPropertyExpression p,
            Quantifier q, OWLOntology o, OWLAxiom ax) {
        this(s, t, p, q, o);
        this.underlyingAxioms.add(ax);
    }


	public OWLGraphEdge(OWLObject s, OWLObject t, Quantifier q) {
		super();
		OWLQuantifiedProperty el = new OWLQuantifiedProperty(null,q);
		this.source = s;
		this.target = t;
		setSingleQuantifiedProperty(el);
	}

    /**
     * Copy constructor.
     * @param edge An {@code OWLGraphEdge} which attributes will be copy from 
     *             to instantiate this one.
     */
    public OWLGraphEdge(OWLGraphEdge edge) {
        super();
        this.source = edge.getSource();
        this.target = edge.getTarget();
        this.ontology = edge.getOntology();
        this.distance = edge.getDistance();
        this.quantifiedPropertyList = edge.getQuantifiedPropertyList();
        this.underlyingAxioms = edge.getAxioms();
    }

	public OWLObject getSource() {
		return source;
	}
	public String getSourceId() {
		return source.toString();
	}
	public void setSource(OWLObject source) {
		this.source = source;
	}
	public OWLObject getTarget() {
		return target;
	}
	public String getTargetId() {
		return target.toString();
	}

	public void setTarget(OWLObject target) {
		this.target = target;
	}
	
	public int getDistance() {
		return distance;
	}

	public void setDistance(int distance) {
		this.distance = distance;
	}

	/**
	 * @return copy of QPL
	 */
	public List<OWLQuantifiedProperty> getQuantifiedPropertyList() {
		return new Vector<OWLQuantifiedProperty>(quantifiedPropertyList);
	}

	public void setQuantifiedPropertyList(List<OWLQuantifiedProperty> qps) {
		this.quantifiedPropertyList = qps;
	}

	public OWLQuantifiedProperty getSingleQuantifiedProperty() {
		return quantifiedPropertyList.get(0);
	}
	
	public OWLQuantifiedProperty getFirstQuantifiedProperty() {
		return quantifiedPropertyList.get(0);
	}
	
	public OWLQuantifiedProperty getLastQuantifiedProperty() {
		return quantifiedPropertyList.get(quantifiedPropertyList.size()-1);
	}

	public void setSingleQuantifiedProperty(OWLQuantifiedProperty qp) {
		quantifiedPropertyList = new Vector<OWLQuantifiedProperty>();
		quantifiedPropertyList.add(qp);
	}

	public OWLQuantifiedProperty getFinalQuantifiedProperty() {
		return quantifiedPropertyList.get(quantifiedPropertyList.size()-1);
	}

	public OWLOntology getOntology() {
		return ontology;
	}
	public void setOntology(OWLOntology ontology) {
		this.ontology = ontology;
	}


	public boolean isSourceNamedObject() {
		return (source instanceof OWLNamedObject);
	}
	public boolean isTargetNamedObject() {
		return (target instanceof OWLNamedObject);
	}

    /**
     * Returns A {@code Set} containing the underlying {@code OWLAxiom}s that allowed 
     * to produce this {@code OWLGraphEdge}. This is because a same {@code OWLGraphEdge} 
     * (meaning, with the same {@code source}, {@code target}, {@code quantifiedPropertyList}, 
     * {@code ontology}, see {@code hashCode} and {@code equals} methods) can be produced 
     * by different {@code OWLAxiom}s. For instance, an {@code OWLEquivalentClassesAxiom} and a 
     * {@code OWLSubClassOfAxiom} could produce equal {@code OWLGraphEdge}s; or 
     * two structurally equivalent {@code OWLSubClassOfAxiom}s, but with different 
     * {@code OWLAnnotation}s.
     * 
	 * @return A {@code Set} of the underlying {@code OWLAxiom}s that produced this 
	 *         {@code OWLGraphEdge}.
	 * @see #getSubClassOfAxioms()
	 */
	public Set<OWLAxiom> getAxioms() {
	    return new HashSet<OWLAxiom>(this.underlyingAxioms);
	}
	
	/**
	 * Returns only the {@code OWLSubClassOfAxiom}s among the {@code OWLAxiom}s 
	 * returned by {@link #getAxioms()}.
	 * 
	 * @return A {@code Set} of the underlying {@code OWLSubClassOfAxiom}s that 
	 *         produced this {@code OWLGraphEdge}.
	 * @see #getAxioms()
	 */
	public Set<OWLSubClassOfAxiom> getSubClassOfAxioms() {
	    Set<OWLSubClassOfAxiom> subs = new HashSet<OWLSubClassOfAxiom>();
	    for (OWLAxiom ax: this.underlyingAxioms) {
	        if (ax instanceof OWLSubClassOfAxiom) {
	            subs.add((OWLSubClassOfAxiom) ax);
	        }
	    }
	    return subs;
	}
    
	/**
	 * Merges this {@code OWLGraphEdge} and {@code edge} into a new one, containing 
	 * the union of their underlying {@code OWLAxiom}s, as returned by {@code getAxioms()}.
	 * This method is available only if this {@code OWLGraphEdge} and {@code edge} 
	 * are considered equal by the {@code equals} method (which does not take into 
	 * account the underlying {@code OWLAxiom}s), otherwise an 
	 * {@code IllegalArgumentException} is thrown. This method is useful to merge 
	 * several {@code OWLGraphEdge}s structurally equivalent, but produced from 
	 * different {@code OWLAxiom}s.
	 * 
	 * @param edge The {@code OWLGraphEdge} to be merged with this one.
	 * @return     A newly instantiated {@code OWLGraphEdge}, equal to {@code edge} 
	 *             and to this {@code OWLGraphEdge}, but with merged {@code OWLAxiom}s.
	 * @throws IllegalArgumentException    If {@code edge} and this {@code OWLGraphEdge} 
	 *                                     are not equal.
	 */
    public OWLGraphEdge merge(OWLGraphEdge edge) throws IllegalArgumentException {
        if (!this.equals(edge)) {
            throw new IllegalArgumentException("Two OWLGraphEdges must be considered " +
                    "equal to be merged");
        }
        
        OWLGraphEdge newEdge = new OWLGraphEdge(this);
        newEdge.underlyingAxioms.addAll(edge.underlyingAxioms);
        return newEdge;
    }

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		sb.append(source);
		sb.append(" ");
		sb.append(getQuantifiedPropertyList());
		sb.append(" ");
		sb.append(target);
        sb.append(" ");
        sb.append(underlyingAxioms);
		sb.append("]");
		return sb.toString();
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		List<OWLQuantifiedProperty> owlQuantifiedProperties = getQuantifiedPropertyList();
		result = prime * result + ((owlQuantifiedProperties == null) ? 0 : owlQuantifiedProperties.hashCode());
		result = prime * result + ((source == null) ? 0 : source.hashCode());
		result = prime * result + ((target == null) ? 0 : target.hashCode());
        result = prime * result + ((ontology == null) ? 0 : ontology.hashCode());
		return result;
	}
	
	public boolean isEq(Object a, Object b) {
		if (a == null && b == null)
			return true;
		if (a == null || b == null)
			return false;
		
		return a.equals(b);
	}
	
	@Override
	public boolean equals(Object e) {
		if(e == null || !(e instanceof OWLGraphEdge))
			return false;
		
		OWLGraphEdge other = (OWLGraphEdge) e;
		
		return isEq(other.getSource(),getSource()) &&
				isEq(other.getTarget(),getTarget()) &&
				isEq(quantifiedPropertyList,other.getQuantifiedPropertyList()) &&
                isEq(ontology,other.getOntology());
	}

}
