package owltools.mooncat;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.obolibrary.macro.MacroExpansionVisitor;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;
import owltools.graph.OWLQuantifiedProperty.Quantifier;

/**
 * This implements species subsetting (module generation) using
 * OWL reasoning.
 * 
 * The procedure is 
 * 
 * 1. to assert
 *      Thing SubClassOf in_taxon some My-taxon-of-interest
 * 
 * 2. Remove all unsatisfiable classes.
 * 
 * This is a somewhat brutal way of doing things. A more
 * elegant approach would be to test every class (C in_taxon some T),
 * but this would be a little slower. But it avoids stating falsehoods.
 * 
 * The current approach requires removing all taxon-crossing
 * relationships first to avoid removing too many
 * (e.g. if C is unsatisfiable, then homologous_to some C is
 * also unsatisfiable, we don't want to remove the homologs).
 * 
 * A few of these are hardcoded in the module, but the user
 * should be careful to remove others depending on their ontology.
 * 
 * See: https://github.com/obophenotype/uberon/wiki/Taxon-constraints
 * 
 * Note that for this approach to work properly, the ontology used needs to be merged  
 * with a taxonomy ontology providing relations between taxa, and disjoint classes axioms 
 * between sibling taxa (see 
 * http://douroucouli.wordpress.com/2012/04/24/taxon-constraints-in-owl/).
 * 
 * 
 * 
 * @author cjm
 * @author Frederic Bastian
 * 
 */
public class SpeciesSubsetterUtil {

	private Logger LOG = Logger.getLogger(SpeciesSubsetterUtil.class);

	OWLGraphWrapper graph;

	OWLOntology ont;
	OWLOntologyManager mgr;
	OWLDataFactory fac;
	public OWLReasoner reasoner;
	public OWLObjectProperty viewProperty;
	public OWLClass taxClass;
	public OWLClass rootClass;
	String[] defaultRelationsToRemove =
	{
		"evolved_from",
		"homologous_to",
		"evolved_multiple_times_in",
		//"shares ancestor with"
		"RO:0002158"
	};

    /**
     * A {@code String} that is the OBO-like ID of the "in_taxon" object property.
     */
    private final String IN_TAXON_ID = "RO:0002162";
	/**
	 * A {@code String} that is the OBO-like ID of the "only_in_taxon" object property.
	 */
	private final String ONLY_IN_TAXON_ID = "RO:0002160";
    /**
     * A {@code String} that is the OBO-like ID of the "never_in_taxon" annotation property.
     */
    private final String NEVER_IN_TAXON_ID = "RO:0002161";

    /**
     * Constructor providing the {@code OWLGraphWrapper} wrapping the {@code OWLOntology} 
     * used for species subsetting. This {@code OWLOntology} will be immediately modified 
     * to remove relations that could generate incorrect taxon constraints. 
     * 
     * @param g {@code OWLGraphWrapper} wrapping the {@code OWLOntology} to use.
     */
	public SpeciesSubsetterUtil(OWLGraphWrapper g) {
		graph = g;

        ont = graph.getSourceOntology();
        mgr = ont.getOWLOntologyManager();
        fac = mgr.getOWLDataFactory();
        
        //remove relations generating incorrect taxon constraints
        this.removeDefaultAxioms();
	}


	public void removeOtherSpecies() {
		
		MacroExpansionVisitor mev = new MacroExpansionVisitor(ont);
		mev.expandAll();
		
		if (viewProperty == null) {
			IRI iri = graph.getIRIByIdentifier("RO:0002162");
			LOG.info("View property.IRI = "+iri);

			viewProperty = fac.getOWLObjectProperty(iri);
		}
		LOG.info("View property = "+viewProperty);

		/*
		Mooncat m = new Mooncat(graph);
		m.retainAxiomsInPropertySubset(graph.getSourceOntology(),props,null);
		m.removeDanglingAxioms();
		*/
		
		Set<OWLAxiom> rmAxioms = new HashSet<OWLAxiom>();
		if (rootClass == null)
			rootClass = fac.getOWLThing();
		//rootClass = graph.getOWLClassByIdentifier("UBERON:0001062");
		LOG.info("AE = "+rootClass);
		LOG.info("TC = "+taxClass);
		OWLClassExpression rx = fac.getOWLObjectSomeValuesFrom(viewProperty,
				taxClass);
		OWLSubClassOfAxiom qax = fac.getOWLSubClassOfAxiom(
				rootClass, rx);
		mgr.addAxiom(ont, qax);
		LOG.info("Constraint: "+qax);
		
		// flush reasoner, otherwise changes made to the ontology are not used for reasoning
		reasoner.flush();
		Set<OWLClass> ucs = reasoner.getEquivalentClasses(fac.getOWLNothing()).getEntities();
		LOG.info("UCS: "+ucs.size());
		for (OWLClass uc : ucs) {
			LOG.info("Removing: "+uc+" "+graph.getLabel(uc));
			rmAxioms.addAll(ont.getAxioms(uc));
			rmAxioms.add(fac.getOWLDeclarationAxiom(uc));
			rmAxioms.addAll(ont.getAnnotationAssertionAxioms(uc.getIRI()));
		}
		mgr.removeAxioms(ont, rmAxioms);
	}
	
	/**
	 * Removes from the {@code OWLOntology} wrapped into {@link #graph} the relations 
	 * defined in {@link #defaultRelationsToRemove}.
	 * 
	 * @return an {@code int} that is the number of {@code OWLAxiom}s actually removed.
	 */
	private int removeDefaultAxioms() {
	    //Set<OWLObjectProperty> props = new HashSet<OWLObjectProperty>();
        Set<OWLAxiom> rmAxioms = new HashSet<OWLAxiom>();
        for (String p : defaultRelationsToRemove) {
            IRI iri = graph.getIRIByIdentifier(p);
            OWLObjectProperty prop = fac.getOWLObjectProperty(iri);
            if (prop != null) {
                LOG.info(" FILTERING: "+prop);
                //props.add(prop);
                for (OWLAxiom ax : ont.getAxioms()) {
                    if (ax.getObjectPropertiesInSignature().contains(prop)) {
                        LOG.info("REMOVING:"+ax);
                        rmAxioms.add(ax);
                    }
                }
            }
        }

        LOG.info("Removing: "+rmAxioms.size());
        return mgr.removeAxioms(ont, rmAxioms).size();
	}
	
	
	//**********************************
	// EXPLAIN TAXON CONSTRAINT METHODS
	//**********************************
	
	/**
	 * Provides explanations about the sources of some taxon constraints on 
	 * the {@code OWLClass}es provided through {@code clsIds}, related to the taxa 
	 * provided through {@code taxonIds}. This method allows to know why a given term, 
	 * in the {@code OWLOntology} provided at instantiation, is defined as existing, 
	 * or not existing, in some given taxa. 
	 * <p>
	 * For each requested {@code OWLClass}, explanations are provided as paths going from 
	 * the {@code OWLClass}, to a taxon constraint pertinent to any of the requested taxa. 
	 * A path is represented as a {@code List} of {@code OWLObject}s. The first 
	 * {@code OWLObject} is always one of the requested {@code OWLClass}. 
	 * Following {@code OWLObject}s are either {@code OWLClass}es, or anonymous 
	 * {@code OWLClassExpression}s, representing the targets of {@code SubClassOfAxiom}s. 
	 * The final {@code OWLObject} is either an anonymous {@code OWLClassExpression}s 
	 * representing a "only_in_taxon" relation, or an {@code OWLAnnotation} 
	 * representing a "never_in_taxon" annotation.
	 * <p>
	 * If some of the requested {@code OWLClass}es are not found in the returned 
	 * explanations, or their explanations do not cover all requested taxa, it means 
	 * there is no particular explanation for existence of these {@code OWLClass}es 
	 * in the taxon, they simply exist by default.
	 * 
	 * @param clsIds       A {@code Set} of {@code String}s that are the OBO-like IDs 
	 *                     of the {@code OWLClass} for which we want to retrieve 
	 *                     explanations about taxon constraints, from the ontology 
	 *                     provided at instantiation of this class.
	 * @param taxonIds     A {@code Set} of {@code String}s that are the OBO-like IDs 
	 *                     of the taxa for which we want to retrieve explanations 
	 *                     about presence or absence of the {@code OWLClass}es 
	 *                     provided through {@code clsIds}.
	 * @return             A {@code Collection} of {@code List}s of {@code OWLObject}s, 
	 *                     where each {@code List} correspond to a walk explaining 
	 *                     a taxon constraint.
	 * @throws IllegalArgumentException    If some of the requested {@code OWLClass}es 
	 *                                     or requested taxa could not be found in 
	 *                                     the ontology provided at instantiation.
	 */
	public Collection<List<OWLObject>> explainTaxonConstraint(Collection<String> clsIds, 
	        Collection<String> taxonIds) throws IllegalArgumentException {
	    if (LOG.isInfoEnabled()) {
	        LOG.info("Start explaining taxon constraints of classes " + clsIds + 
	                " in taxa " + taxonIds); 
	    }
        
        //get the requested taxa, and all their ancestors.
	    //also, to replace a check for disjoint classes axioms between sibling taxa, 
	    //we associate in a Map each requested taxon to its descendants.
	    Set<OWLClass> taxa = new HashSet<OWLClass>();
        Set<OWLClass> taxonAncestors = new HashSet<OWLClass>();
        Map<OWLClass, Set<OWLClass>> taxaWithDescendants = 
                new HashMap<OWLClass, Set<OWLClass>>();
        for (String taxonId: taxonIds) {
            OWLClass taxon = graph.getOWLClassByIdentifier(taxonId);
            if (taxon == null) {
                throw new IllegalArgumentException("The provided taxon ID " + 
                    taxonId + " does not exist in the provided ontology.");
            }
            taxa.add(taxon);
            taxonAncestors.addAll(graph.getOWLClassAncestors(taxon));
            taxaWithDescendants.put(taxon, graph.getOWLClassDescendants(taxon));
        }
        
        //get object property only_in_taxon (to obtain taxon restrictions), 
        //and annotation property never_in_taxon (only to return convenient explanations).
        OWLObjectProperty onlyInTaxon = graph.getOWLObjectPropertyByIdentifier(ONLY_IN_TAXON_ID);
        OWLAnnotationProperty neverInTaxon = fac.getOWLAnnotationProperty(
                graph.getIRIByIdentifier(NEVER_IN_TAXON_ID));

        //get all "never in taxon" information (class expressions using "in taxon", 
        //equivalent to owl:nothing)
        Map<OWLClass, Set<OWLClass>> neverInTaxa = this.neverInTaxa();
        
        //*************Start iterating each OWLClass***************
        //now, for each class for which we want to explain taxon constraints, 
        //we walk all possible paths to the root of the ontology. We stop examining 
        //a path as soon as it allows to explain a taxon constraint, but other paths 
        //would still be examined.
        Collection<List<OWLObject>> allExplanations = new ArrayList<List<OWLObject>>();
        for (String clsId: clsIds) {
            OWLClass cls = graph.getOWLClassByIdentifier(clsId);
            if (cls == null) {
                throw new IllegalArgumentException("The provided class ID " + 
                    clsId + " does not exist in the provided ontology.");
            }
            
            //a deque storing all possible paths to some taxon constraints, going from cls.
            //each walk is represented by a List of OWLObjects, the first one being cls.
            //each step of the walk is represented by an OWLObject, that is either 
            //an OWLClass, or an anonymous class expression. The final step leading 
            //to a taxon constraint is either an anonymous class expression using 
            //"only_in_taxon", or an annotation using "never_in_taxon".
            Deque<List<OWLObject>> walks = new ArrayDeque<List<OWLObject>>();
            walks.add(new ArrayList<OWLObject>(Arrays.asList(cls)));
            List<OWLObject> aWalk;
            Set<OWLClass> alreadyWalked = new HashSet<OWLClass>();
            
            //**************walk each path to the root**************
            while ((aWalk = walks.pollFirst()) != null) {
                //get the class which to continue the walk from
                OWLObject currentStep = aWalk.get(aWalk.size() - 1);
                OWLClass clsToWalk = null;
                
                if (currentStep instanceof OWLClass) {
                    clsToWalk = (OWLClass) currentStep;
                } else {
                    //it should be an anonymous class expression, we can extract the target 
                    //using the getOutgoingEdges method.
                    Set<OWLGraphEdge> edges = graph.getOutgoingEdges(currentStep);
                    if (edges.size() != 1 || 
                         !(edges.iterator().next().getTarget() instanceof OWLClass)) {
                        throw new AssertionError("Unexpected class expression walked: " + 
                                currentStep);
                    }
                    clsToWalk = (OWLClass) edges.iterator().next().getTarget();
                }
                
                if (!alreadyWalked.add(clsToWalk)) {
                    continue;
                }
                
                //***************only_in_taxon check***************
                //check whether there is "only_in_taxon" restrictions for this OWLClass.
                for (OWLGraphEdge edge: graph.getOutgoingEdges(clsToWalk)) {
                    if (edge.getFinalQuantifiedProperty().getProperty() != null && 
                        edge.getFinalQuantifiedProperty().isSomeValuesFrom() && 
                        edge.getFinalQuantifiedProperty().getProperty().equals(onlyInTaxon) && 
                        edge.getTarget() instanceof OWLClass) {
                        //we need to know whether the taxon targeted by 
                        //the "only_in_taxon" restriction is disjoint from one 
                        //of the requested taxa (in that case, we would have found 
                        //the explanation about non-existence of the class), or whether 
                        //it is equal to, or is the ancestor of, one of the requested taxa 
                        //(in that case, we would have an explanation about the existence 
                        //of the class).
                        OWLClass targetedTaxon = (OWLClass) edge.getTarget();
                        boolean constraintUsed = false;
                        //if the taxon targeted by the "only_in_taxon" relation is one of 
                        //the requested taxa, or is the ancestor of one of the requested taxa. 
                        //So, this constraint explains the existence of the structure 
                        //in one of the requested taxa.
                        if (taxa.contains(targetedTaxon) || 
                                taxonAncestors.contains(targetedTaxon)) {
                            constraintUsed = true;
                        } else {
                            //otherwise, we need to verify whether the targeted taxon 
                            //is disjoint from some of the requested taxa, in that case 
                            //it would explain non-existence of the structure in some 
                            //of the requested taxa.
                            for (Set<OWLClass> descendants: taxaWithDescendants.values()) {
                                //this is the condition replacing check on disjoint taxa: 
                                //if one of the requested taxa is neither equal 
                                //to the targeted taxon, nor descendant of the targeted 
                                //taxon (previous test), nor an ancestor of the targeted 
                                //taxon (this test), then it means it is a taxon disjoint 
                                //from the targeted taxon.
                                if (!descendants.contains(targetedTaxon)) {
                                    constraintUsed = true;
                                    break;
                                }
                            } 
                        }
                        if (constraintUsed) {
                            //explanation found
                            List<OWLObject> explanation = new ArrayList<OWLObject>(aWalk);
                            explanation.add(graph.edgeToTargetExpression(edge));
                            allExplanations.add(explanation);
                        } 
                    } 
                }
                
                //**************never_in_taxon check***************
                //check whether there is "never_in_taxon" restrictions for this OWLClass.
                Set<OWLClass> clsNeverInTaxa = neverInTaxa.get(clsToWalk);
                if (clsNeverInTaxa != null) {
                    for (OWLClass targetedTaxon: clsNeverInTaxa) {
                        //for each of the taxa targeted by the "never_in_taxon", we check 
                        //whether it is equal to one of the requested taxa, or is 
                        //the ancestor of one of the requested taxa. In that case, 
                        //we would have found explanation for nonexistence of the term.
                        if (taxa.contains(targetedTaxon) || 
                                taxonAncestors.contains(targetedTaxon)) {
                            //explanation found.
                            //we create a fake annotation using "never_in_taxon", 
                            //to store it in allExplanations.
                            List<OWLObject> explanation = new ArrayList<OWLObject>(aWalk);
                            explanation.add(fac.getOWLAnnotation(neverInTaxon, 
                                    targetedTaxon.getIRI()));
                            allExplanations.add(explanation);
                        }
                    }
                }
                
                //****************continue walk to the root*****************
                //we continue the walk even if we found an explanation, maybe 
                //there is a contradictory taxon constraint further
                for (OWLGraphEdge edge: graph.getOutgoingEdges(clsToWalk)) {
                    List<OWLObject> walkContinue = new ArrayList<OWLObject>(aWalk);
                    walkContinue.add(graph.edgeToTargetExpression(edge));
                    walks.offerLast(walkContinue);
                }
            }
        }
        
        if (LOG.isInfoEnabled()) {
            LOG.info("Done explaining taxon constraints, explanations: " + allExplanations); 
        }
        return allExplanations;
    }

    
    /**
     * Gets from {@link #graph} the taxon constraint information about {@code OWLClass}es 
     * defined as <strong>not</code> existing in some taxa. It means we want the classes 
     * equivalent to owl:nothing, over "in taxon" object property (see {@link #inTaxonId}), 
     * and the targeted taxon.
     * <p>
     * This method returns a {@code Map} where keys are {@code OWLClass}es with 
     * constraints on taxon non-existence, the associated value being a {@code Set} 
     * of {@code OWLClass}es representing the taxa which it does <strong>not</strong> 
     * exist in.
     * 
     * @return              A {@code Map} with {@code OWLClass}es as keys, each 
     *                      associated to a {@code Set} of {@code OWLClass}es 
     *                      that are the taxa which it never exits in.
     */
    private Map<OWLClass, Set<OWLClass>> neverInTaxa() {
        LOG.trace("Getting \"never in taxon\" information...");
        
        //A Map where OWLClasses to check are keys, associated to a Set of OWLClasses 
        //that are the taxa in which the OWLClass never exits.
        Map<OWLClass, Set<OWLClass>> notInTaxa = new HashMap<OWLClass, Set<OWLClass>>();
        
        fac = graph.getManager().getOWLDataFactory();
        ont = graph.getSourceOntology();
        OWLObjectProperty inTaxon = graph.getOWLObjectPropertyByIdentifier(IN_TAXON_ID);
        OWLClass nothing = fac.getOWLNothing();
        
        //we want the classes equivalent to owl:nothing over "in taxon" object property, 
        //and the targeted taxon
        for (OWLEquivalentClassesAxiom equiAxiom : ont.getEquivalentClassesAxioms(nothing)) {
            //check only axioms that use the "in taxon" object property
            if (!equiAxiom.getObjectPropertiesInSignature().contains(inTaxon)) {
                continue;
            }
            //valid axiom, get equivalent class expressions
            OWLClass uberonCls = null;
            OWLClass taxon = null;
            for (OWLClassExpression ce : equiAxiom.getClassExpressions()) {
                if (ce.equals(nothing)) {
                    continue;
                }
                //classes not existing in a taxon are described as the intersection 
                //of the class, and of a restriction over "in taxon", targeting a taxon. 
                //The OWLGraphWrapper will decompose those into one edge representing 
                //a subClassOf relation to the class, and another edge representing 
                //the restriction to the taxon
                for (OWLGraphEdge edge: graph.getOutgoingEdges(ce)) {
                    //edge subClassOf to the class to check
                    if (edge.getSingleQuantifiedProperty().getProperty() == null && 
                            edge.getSingleQuantifiedProperty().getQuantifier() == 
                            Quantifier.SUBCLASS_OF && 
                            edge.getTarget() instanceof OWLClass) {
                        if (uberonCls != null) {
                            throw new AssertionError("Error, several taxon restrictions " +
                            		"in a same equivalent class axiom.");
                        }
                        uberonCls = (OWLClass) edge.getTarget();
                        
                    } 
                    //edge "in taxon" to the targeted taxon
                    else if (edge.getFinalQuantifiedProperty().getProperty() != null && 
                            edge.getFinalQuantifiedProperty().isSomeValuesFrom() && 
                            edge.getFinalQuantifiedProperty().getProperty().equals(inTaxon) && 
                            edge.getTarget() instanceof OWLClass) {
                        if (taxon != null) {
                            throw new AssertionError("Error, several taxon restrictions " +
                                    "in a same equivalent class axiom.");
                        }
                        taxon = (OWLClass) edge.getTarget();
                    }
                }
            }
            
            if (uberonCls != null && taxon != null) {
                if (!notInTaxa.containsKey(uberonCls)) {
                    notInTaxa.put(uberonCls, new HashSet<OWLClass>());
                }
                notInTaxa.get(uberonCls).add(taxon);
            }
        }

        if (LOG.isTraceEnabled()) {
            LOG.trace("Done getting \"never in taxon\" information: " + notInTaxa);
        }
        return notInTaxa;
    }
}
