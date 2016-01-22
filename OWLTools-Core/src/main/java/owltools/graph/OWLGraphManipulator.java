package owltools.graph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.semanticweb.owlapi.change.ConvertEquivalentClassesToSuperClasses;
import org.semanticweb.owlapi.change.SplitSubClassAxioms;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.OWLSubPropertyChainOfAxiom;
import org.semanticweb.owlapi.model.RemoveAxiom;
import org.semanticweb.owlapi.model.RemoveImport;
import org.semanticweb.owlapi.model.UnknownOWLOntologyException;
import org.semanticweb.owlapi.model.parameters.ChangeApplied;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.util.OWLEntityRemover;

import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;
import owltools.graph.OWLQuantifiedProperty.Quantifier;

/**
 * This class provides functionalities to modify an ontology to simplify its graph structure. 
 * The resulting ontology will not be suitable for reasoning. The aim of this class 
 * is only to make an ontology suitable for a nicer graph display. 
 * <p>
 * Notably, at instantiation of this class, several operations are performed to obtain only 
 * simple edges between classes, similar to what the method {@link 
 * OWLGraphWrapperEdges#getOutgoingEdges(OWLObject) getOutgoingEdges} is simulating. 
 * The operations performed are, in order: 
 * <ol>
 *   <li>to reverse all {@code OWLObjectUnionOf}s, that are part of 
 *   an {@code OWLEquivalentClassesAxiom}, into individual {@code OWLSubClassOfAxiom}s, where 
 *   the named classes part of the {@code OWLObjectUnionOf} become subclasses, and 
 *   the top level named classes of the {@code OWLEquivalentClassesAxiom}, superclass. 
 *   <li>to relax all {@code OWLEquivalentClassesAxiom}s into {@code OWLSubClassOfAxiom}s 
 *   using a <a href=
 *   'http://owlapi.sourceforge.net/javadoc/org/semanticweb/owlapi/ConvertEquivalentClassesToSuperClasses.html'>
 *    ConvertEquivalentClassesToSuperClasses</a>.
 *   <li>to relax all {@code OWLSubClassOfAxiom}s, whose superclass is an 
 *   {@code OWLObjectIntersectionOf}, into multiple {@code OWLSubClassOfAxiom}s, using a <a 
 *   href='http://owlapi.sourceforge.net/javadoc/org/semanticweb/owlapi/SplitSubClassAxioms.html'>
 *   SplitSubClassAxioms</a>.
 *   <li>to remove any {@code OWLEquivalentClassesAxiom} or {@code OWLSubClassOfAxiom} using 
 *   an {@code OWLObjectUnionOf}.
 *   <li>to remove ll obsoletd {@code OWLClass}es.
 * </ol>
 * These operations will then allow to remove specific edges between terms, 
 * without impacting other edges that would be associated to them, through 
 * {@code OWLEquivalentClassesAxiom}s, or {@code OWLObjectIntersectionOf}s, or 
 * {@code OWLObjectUnionOf}s. These operations are doing for real what the method {@link 
 * OWLGraphWrapperEdges#getOutgoingEdges(OWLObject) getOutgoingEdges} is simulating 
 * under the hood.
 * <p>
 * Even before these operations take place, all imported ontologies are merged into 
 * the source ontology, then removed from the import closure, to be able 
 * to modify any relation or any class. 
 * <p>
 * <strong>Warning: </strong>these operations must be performed on an ontology 
 * already reasoned, as this class does not accept any reasoner for now. 
 * <p>
 * This class then allows to perform:
 * <ul>
 * <li><u>relation reduction</u> using regular composition rules, and composition over 
 * super properties, see {@link #reduceRelations()}. 
 * <li><u>relation reduction semantically incorrect</u> over relations is_a (SubClassOf) 
 * and part_of (or sub-relations), see {@link #reducePartOfIsARelations()}. 
 * <li><u>class removal and relation propagation</u>, using regular composition rules, 
 * and composition over super properties, see {@link #removeClassAndPropagateEdges(String)}.
 * <li><u>relation mapping to parent relations</u>, see {@link #mapRelationsToParent(Collection)} 
 * and {@link #mapRelationsToParent(Collection, Collection)}
 * <li><u>relation filtering or removal</u>, see {@link #filterRelations(Collection, boolean)} 
 * and {@link #removeRelations(Collection, boolean)}. 
 * <li><u>relation removal to subsets</u> if non orphan, see 
 * {@link #removeRelsToSubsets(Collection)} 
 * <li><u>subgraph filtering or removal</u>, see {@link #filterSubgraphs(Collection)} and 
 * {@link #removeSubgraphs(Collection, boolean)}. 
 * <li>a combination of these methods to <u>generate a simple ontology</u>, see 
 * {@link #simplifies(Collection, Collection, Collection, Collection, Collection) simplifies}.
 * 
 * @author Frederic Bastian
 * @version September 2014
 * @since June 2013
 */
public class OWLGraphManipulator {
	private final static Logger log = LogManager.getLogger(OWLGraphManipulator.class.getName());
	/**
	 * The {@code OWLGraphWrapper} on which the operations will be performed 
	 * (relation reductions, edge propagations, ...).
	 */
	private OWLGraphWrapper owlGraphWrapper;
	/**
	 * A {@code Set} of {@code OWLObjectPropertyExpression}s that are 
	 * the sub-properties of the "part_of" property (for instance, "deep_part_of").
	 * 
	 * @see #isAPartOfEdge(OWLGraphEdge)
	 */
	private Set<OWLObjectPropertyExpression> partOfRels;
	/**
	 * A {@code String} representing the OBO-style ID of the part_of relation. 
	 */
	private final static String PARTOFID    = "BFO:0000050";
	/**
	 * A {@code String} representing the OBO-style ID of the develops_from relation. 
	 */
	private final static String DVLPTFROMID = "RO:0002202";
	
	/**
	 * A {@code Set} of {@code String}s that are the string representations of the {@code IRI}s 
	 * of {@code OWLAnnotationProperty}s for which it is not allowed to have a cardinality 
	 * greater than one for a given {@code OWLAnnotationSubject}. 
	 * See {@code org.obolibrary.oboformat.model.Frame#check()} in oboformat library. 
	 * Used in method {@link #mergeImportClosure()}.
	 * 
	 * @see #mergeImportClosure()
	 */
	private final static Set<String> maxOneCardinalityAnnots = 
	        new HashSet<String>(Arrays.asList("http://purl.obolibrary.org/obo/IAO_0000115", 
	                "http://www.w3.org/2000/01/rdf-schema#label", 
	                "http://www.w3.org/2000/01/rdf-schema#comment", 
	                "http://purl.obolibrary.org/obo/IAO_0000427", 
	                "http://www.w3.org/2002/07/owl#deprecated", 
	                "http://purl.org/dc/elements/1.1/date", 
	                "http://www.geneontology.org/formats/oboInOwl#shorthand"));
	
	//*********************************
	//    CONSTRUCTORS
	//*********************************
	/**
     * Default constructor. This class should be instantiated only through 
     * the constructor {@code OWLGraphManipulator(OWLGraphWrapper)} or 
     * {@code OWLGraphManipulator(OWLOntology)}.
     * @see #OWLGraphManipulator(OWLGraphWrapper)
     * @see #OWLGraphManipulator(OWLOntology)
     */
    @SuppressWarnings("unused")
    private OWLGraphManipulator() throws OWLOntologyCreationException {
        this((OWLGraphWrapper) null);
    }
    /**
     * Constructor providing the {@code OWLOntology} on which modifications 
     * will be performed, that will be wrapped in a {@code OWLGraphWrapper}. 
     * 
     * @param ont   The {@code OWLOntology}  on which the operations will be 
     *              performed.
     * @throws OWLOntologyCreationException If an error occurred while wrapping 
     *                                      the {@code OWLOntology} into a 
     *                                      {@code OWLGraphWrapper}.
     * @throws UnknownOWLOntologyException  If an error occurred while wrapping 
     *                                      the {@code OWLOntology} into a 
     *                                      {@code OWLGraphWrapper}.
     */
    public OWLGraphManipulator(OWLOntology ont) throws UnknownOWLOntologyException {
        this(new OWLGraphWrapper(ont));
    }
    /**
     * Constructor providing the {@code OWLGraphWrapper} 
     * wrapping the ontology on which modifications will be performed. 
     * 
     * @param owlGraphWrapper   The {@code OWLGraphWrapper} on which the operations 
     *                          will be performed.
     * @throws OWLOntologyCreationException     If an error occurred while merging 
     *                                          the imported ontologies into the source 
     *                                          ontology.
     */
    public OWLGraphManipulator(OWLGraphWrapper owlGraphWrapper) {
        this(owlGraphWrapper, true);
    }
    /**
     * Constructor providing the {@code OWLGraphWrapper} 
     * wrapping the ontology on which modifications will be performed. 
     * 
     * @param owlGraphWrapper       The {@code OWLGraphWrapper} on which the operations 
     *                              will be performed.
     * @param performDefaultModifs  A {@code boolean} defining whether the ontology 
     *                              should be simplified at instantiation.
     * @throws OWLOntologyCreationException     If an error occurred while merging 
     *                                          the imported ontologies into the source 
     *                                          ontology.
     */
    public OWLGraphManipulator(OWLGraphWrapper owlGraphWrapper, boolean performDefaultModifs) {
        this.setOwlGraphWrapper(owlGraphWrapper);
        if (performDefaultModifs) {
            this.performDefaultModifications();
        }
    }

    //*********************************
    //  DEFAULT ONTOLOGY MODIFICATIONS
    //*********************************
    /**
     * Performs all default modifications needed before using this class, to get only 
     * simple edges between classes, similar to what the method {@link 
     * OWLGraphWrapperEdges#getOutgoingEdges(OWLObject) getOutgoingEdges} is simulating.
     * <p>
     * Also, all imported ontologies are merged into the source ontology, then removed 
     * from the import closure, to be able to modify any relation or any class. 
     * <p>
     * Methods called are, in order: 
     * <ol>
     *   <li>{@link #mergeImportClosure()}</li>
     *   <li>{@link #reverseOWLObjectUnionOfs()}
     *   <li>{@link #convertEquivalentClassesToSuperClasses()}
     *   <li>{@link #splitSubClassAxioms()}
     *   <li>{@link #removeOWLObjectUnionOfs()}
     *   <li>{@link #removeObsoleteClassRels()}
     * </ol>
     * 
     * @throws OWLOntologyCreationException     If an error occurred while merging 
     *                                          the imported ontologies into the source 
     *                                          ontology.
     * 
     * @see #mergeImportClosure()
     * @see #reverseOWLObjectUnionOfs()
     * @see #convertEquivalentClassesToSuperClasses()
     * @see #splitSubClassAxioms()
     * @see #removeOWLObjectUnionOfs()
     * @see #removeObsoleteClassRels()
     */
    private void performDefaultModifications() {
        this.mergeImportClosure();
        this.reverseOWLObjectUnionOfs();
        this.convertEquivalentClassesToSuperClasses();
        this.splitSubClassAxioms();
        this.removeOWLObjectUnionOfs();
        this.removeObsoleteClassRels();
        
        //check that all operations worked properly
        if (log.isEnabledFor(Level.WARN)) {
            for (OWLOntology ont : this.getOwlGraphWrapper().getAllOntologies()) {
                for (OWLEquivalentClassesAxiom ax: ont.getAxioms(AxiomType.EQUIVALENT_CLASSES)) {
                    if (ax.containsNamedEquivalentClass()) {
                        log.warn("Some EquivalentClassesAxioms were not removed as expected: " + 
                                ax);
                    }
                }
                for (OWLSubClassOfAxiom ax: ont.getAxioms(AxiomType.SUBCLASS_OF)) {
                    //we allow OWLObjectIntersectionOf for sub-class (GCI relation)
                    if (ax.getSuperClass() instanceof OWLObjectIntersectionOf || 
                            ax.getSuperClass() instanceof OWLObjectUnionOf) {
                        log.warn("Some OWLObjectIntersectionOf or OWLObjectUnionOf " +
                                "were not removed as expected: " + ax);
                    }
                }
            }
        }
    }
    
    /**
     * Merges {@code OWLAxiom}s from the import ontology closure, with the source ontology.
     * This method is similar to {@link OWLGraphWrapperBasic#mergeImportClosure(boolean)}, 
     * except that: i) an {@code OWLAxiom} will be added to the source ontology only if 
     * it is not already present in the source (without taking annotations into account); 
     * ii) a check is performed to ensure that there will not be more than 
     * one annotation axiom for a given subject, when the annotation property 
     * corresponds to a tag with max cardinality one in OBO format 
     * (see {@code org.obolibrary.oboformat.model.Frame#check()}). As for 
     * {@link OWLGraphWrapperBasic#mergeImportClosure(boolean)}, annotations on the ontology 
     * itself are not imported, and the import declarations are removed after execution. 
     */
    private void mergeImportClosure() {
        log.info("Merging axioms from import closure with source ontology...");
        OWLOntology sourceOnt = this.getOwlGraphWrapper().getSourceOntology();
        
        //the method OWLOntology.containsAxiomIgnoreAnnotations is really 
        //not well optimized, so we get all OWLAxioms without annotations 
        //from the source ontology. 
        log.debug("Retrieving OWLAxioms without annotations from source ontology...");
        Set<OWLAxiom> sourceAxiomsNoAnnots = new HashSet<OWLAxiom>();
        for (OWLAxiom ax: sourceOnt.getAxioms()) {
            sourceAxiomsNoAnnots.add(ax.getAxiomWithoutAnnotations());
        }
        if (log.isDebugEnabled()) {
            log.debug(sourceAxiomsNoAnnots.size() + " axioms without annotations retrieved " +
            		"over " + sourceOnt.getAxiomCount() + " axioms.");
        }
        
        for (OWLOntology importedOnt: sourceOnt.getImportsClosure()) {
            if (importedOnt.equals(sourceOnt)) {
                continue;
            }
            log.info("Merging " + importedOnt);
            
//            OWLDataFactory df = sourceOnt.getOWLOntologyManager().getOWLDataFactory();
//            OWLAnnotationProperty p = 
//                    df.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_COMMENT.getIRI());
//            OWLLiteral v = df.getOWLLiteral("Imported " + importedOnt);
//            OWLAnnotation ann = df.getOWLAnnotation(p, v);
//            AddOntologyAnnotation addAnn = 
//                    new AddOntologyAnnotation(sourceOnt, ann);
//            sourceOnt.getOWLOntologyManager().applyChange(addAnn);
            
            //filter the axioms imported to avoid redundancy (too bad there is not 
            //a method OWLOntology.getAxiomsIgnoreAnnotations())
            int importedAxiomCount = 0;
            importAxioms: for (OWLAxiom importedAx: importedOnt.getAxioms()) {
                if (sourceAxiomsNoAnnots.contains(importedAx.getAxiomWithoutAnnotations())) {
                    continue importAxioms;
                }
                    
                //if it is an annotation axion, we need to ensure that 
                //there will not be more than one axiom for a given subject, 
                //when the annotation corresponds to a tag with max cardinality one 
                //in OBO format (see org.obolibrary.oboformat.model.Frame#check()).
                if (importedAx instanceof OWLAnnotationAssertionAxiom) {
                    OWLAnnotationAssertionAxiom castAx = 
                            (OWLAnnotationAssertionAxiom) importedAx;

                    if (maxOneCardinalityAnnots.contains(
                            castAx.getProperty().getIRI().toString()) || 
                            maxOneCardinalityAnnots.contains(
                                    this.getOwlGraphWrapper().getIdentifier(
                                            castAx.getProperty()))) {

                        //check whether we have an annotation for the same subject 
                        //in the source ontology.
                        for (OWLAnnotationAssertionAxiom sourceAnnotAx: 
                            sourceOnt.getAnnotationAssertionAxioms(
                                    castAx.getSubject())) {
                            if (sourceAnnotAx.getProperty().equals(
                                    castAx.getProperty())) {
                                //discard the axiom from import ontology, there is already 
                                //an annotation with same property on same subject
                                log.trace("Discarding axiom: " + castAx);
                                continue importAxioms;
                            }
                        }
                    }
                }
                
                //all verifications passed, include the axiom
                importedAxiomCount++;
                sourceAxiomsNoAnnots.add(importedAx.getAxiomWithoutAnnotations());
                sourceOnt.getOWLOntologyManager().addAxiom(sourceOnt, importedAx);
            }
            log.info(importedAxiomCount + " axioms imported.");
        }

        //remove the import declarations
        Set<OWLImportsDeclaration> oids = sourceOnt.getImportsDeclarations();
        for (OWLImportsDeclaration oid : oids) {
            RemoveImport ri = new RemoveImport(sourceOnt, oid);
            sourceOnt.getOWLOntologyManager().applyChange(ri);
        }
        
        log.info("Done merging axioms.");
    }
    
    /**
     * Reverse all {@code OWLObjectUnionOf}s, that are operands in 
     * an {@code OWLEquivalentClassesAxiom}, into individual {@code OWLSubClassOfAxiom}s, where 
     * the classes part of the {@code OWLObjectUnionOf} become subclasses, and 
     * the original first operand of the {@code OWLEquivalentClassesAxiom} superclass. 
     * <p>
     * Note that such {@code OWLEquivalentClassesAxiom}s are not removed from the ontology, 
     * only {@code OWLSubClassOfAxiom}s are added. The axioms containing 
     * {@code OWLObjectUnionOf}s will be removed by calling {@link #removeOWLObjectUnionOfs()}, 
     * in order to give a chance to {@link #convertEquivalentClassesToSuperClasses()} 
     * to do its job before.
     * 
     * @see #performDefaultModifications()
     * @see #removeOWLObjectUnionOfs()
     * @see #convertEquivalentClassesToSuperClasses()
     */
    private void reverseOWLObjectUnionOfs() {
        log.info("Reversing OWLObjectUnionOfs into OWLSubClassOfAxioms");
        for (OWLOntology ont : this.getOwlGraphWrapper().getAllOntologies()) {
            for (OWLClass cls : ont.getClassesInSignature()) {
                for (OWLEquivalentClassesAxiom eca : ont.getEquivalentClassesAxioms(cls)) {
                    for (OWLClassExpression ce : eca.getClassExpressions()) {
                        if (ce instanceof OWLObjectUnionOf) {
                            for (OWLObject child : ((OWLObjectUnionOf)ce).getOperands()) {
                                //we reverse only named classes
                                if (child instanceof OWLClass) {
                                    this.getOwlGraphWrapper().getManager().addAxiom(ont, 
                                            ont.getOWLOntologyManager().getOWLDataFactory().
                                                getOWLSubClassOfAxiom((OWLClass) child, cls));
                                }
                            }
                        }
                    }
                }
            }
        }
        this.triggerWrapperUpdate();
        log.info("OWLObjectUnionOf reversion done.");
    }
    
    /**
     * Relaxes all {@code OWLEquivalentClassesAxiom}s into {@code OWLSubClassOfAxiom}s 
     * using a <a href=
     * 'http://owlapi.sourceforge.net/javadoc/org/semanticweb/owlapi/ConvertEquivalentClassesToSuperClasses.html'>
     * ConvertEquivalentClassesToSuperClasses</a>. It will allow split any operands 
     * that are {@code OWLObjectIntersectionOf}s.
     * <p>
     * Note that if several named class expressions are part of a same 
     * {@code OWLEquivalentClassesAxiom}, no {@code OWLSubClassOfAxiom} will be created 
     * between them, to avoid generating a cycle.
     * 
     * @see #performDefaultModifications()
     */
    private void convertEquivalentClassesToSuperClasses() {
        log.info("Relaxing OWLEquivalentClassesAxioms into OWLSubClassOfAxioms...");
        //first, get all OWLClasses in all OWLOntologies, as the 
        //ConvertEquivalentClassesToSuperClasses will then remove axioms for an OWLClass 
        //in all ontologies, so, if a class was present in several ontologies, 
        //it would be redundant.
        Set<OWLClass> allClasses = new HashSet<OWLClass>();
        for (OWLOntology ont: this.getOwlGraphWrapper().getAllOntologies()) {
            allClasses.addAll(ont.getClassesInSignature());
        }
        
        //now, remove for each class the ECAs. Store all changes before applying them, 
        //so that if several named classes are part of an ECA, the ECA is not removed 
        //after the iteration of the first named class. Also, changes will be filtered 
        //to avoid creating a cycle between such multiple named classes of an ECA.
        List<OWLOntologyChange> allChanges = new ArrayList<OWLOntologyChange>();
        for (OWLClass cls: allClasses) {
            ConvertEquivalentClassesToSuperClasses convert = 
                    new ConvertEquivalentClassesToSuperClasses(
                            this.getOwlGraphWrapper().getDataFactory(), cls, 
                            this.getOwlGraphWrapper().getAllOntologies(), 
                            this.getOwlGraphWrapper().getSourceOntology(), 
                            false); //disable splitting of OWLObjectIntersectionOf, 
                                    //otherwise it would be difficult to filter 
                                    //axioms created between named classes (see below)
            List<OWLOntologyChange> localChanges = convert.getChanges();
            
            //filter the changes to avoid creating a cycle between named classes
            Set<OWLOntologyChange> addAxiomsToCancel = new HashSet<OWLOntologyChange>();
            for (OWLOntologyChange change: localChanges) {
                if (change.isAddAxiom() && 
                    change.getAxiom() instanceof OWLSubClassOfAxiom && 
                    !((OWLSubClassOfAxiom) change.getAxiom()).getSuperClass().isAnonymous()) {
                    
                    //TODO: maybe we should store the OWLClasses involved, and share/exchange 
                    //all their related SubClassOfAxioms afterwards.
                    addAxiomsToCancel.add(change);
                    log.warn("An EquivalentClassesAxiom contained several named " +
                    		"class expressions, no SubClassOfAxiom will be created " +
                    		"between them. Axiom that will NOT be added: " + 
                    		change.getAxiom());
                }
            }
            localChanges.removeAll(addAxiomsToCancel);
            allChanges.addAll(localChanges);
        }
        this.getOwlGraphWrapper().getManager().applyChanges(allChanges);

        this.triggerWrapperUpdate();
        log.info("ECA relaxation done.");
    }
    
    /**
     * Relaxes all {@code OWLObjectIntersectionOf}s. This method will relax 
     * {@code OWLSubClassOfAxiom}s, whose superclass is an {@code OWLObjectIntersectionOf}, 
     * into multiple {@code OWLSubClassOfAxiom}s, using a <a 
     * href='http://owlapi.sourceforge.net/javadoc/org/semanticweb/owlapi/SplitSubClassAxioms.html'>
     * SplitSubClassAxioms</a>. It will also relax {@code OWLSubClassOfAxiom}s, whose 
     * superclass is an {@code OWLObjectSomeValuesFrom} with a filler being an 
     * {@code OWLObjectIntersectionOf}, into multiple {@code OWLSubClassOfAxiom}s with 
     * an {@code OWLObjectSomeValuesFrom} as superclass, with the same 
     * {@code OWLPropertyExpression}, and individual operands as filler. 
     * <p>
     * Note that it is likely that the {@code OWLObjectIntersectionOf}s where used in
     * {@code OWLEquivalentClassesAxiom}s, rather than in {@code OWLSubClassOfAxiom}s. 
     * But the method {@link #convertEquivalentClassesToSuperClasses()} would have transformed 
     * them into {@code OWLSubClassOfAxiom}s. It must be called before this method.
     * 
     * @see #performDefaultModifications()
     * @see #convertEquivalentClassesToSuperClasses()
     */
    private void splitSubClassAxioms() {
        log.info("Relaxing OWLSubClassOfAxioms whose superclass is an OWLObjectIntersectionOf");
        
        //first, split subClassOf axioms whose superclass is an OWLObjectIntersectionOf
        SplitSubClassAxioms split = new SplitSubClassAxioms(
                this.getOwlGraphWrapper().getAllOntologies(), 
                this.getOwlGraphWrapper().getDataFactory());
        this.getOwlGraphWrapper().getManager().applyChanges(split.getChanges());
        this.triggerWrapperUpdate();
        
        //some ontologies use an OWLObjectIntersectionOf as the filler of 
        //an OWLObjectSomeValuesFrom class expression. We go only one level down 
        //(i.e., we would not translate another OWLObjectSomeValuesFrom part of the 
        //OWLObjectIntersectionOf)
        OWLDataFactory dataFactory = this.getOwlGraphWrapper().getDataFactory();
        List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
        for (OWLOntology ont : this.getOwlGraphWrapper().getAllOntologies()) {
            for (OWLSubClassOfAxiom ax : ont.getAxioms(AxiomType.SUBCLASS_OF)) {
                OWLClassExpression superClsExpr = ax.getSuperClass();
                if (superClsExpr instanceof OWLObjectSomeValuesFrom) {
                    OWLObjectSomeValuesFrom someValuesFrom = 
                            (OWLObjectSomeValuesFrom) superClsExpr;
                    if (someValuesFrom.getFiller() instanceof OWLObjectIntersectionOf) {
                        //remove original axiom
                        changes.add(new RemoveAxiom(ont, ax));
                        
                        OWLObjectIntersectionOf filler = 
                                (OWLObjectIntersectionOf) someValuesFrom.getFiller();
                        for (OWLClassExpression op : filler.getOperands()) {
                            //we accept only OWLClasses, otherwise we would need to compose 
                            //OWLObjectPropertyExpressions
                            if (op instanceof OWLClass) {
                                OWLAxiom replAx = dataFactory.
                                        getOWLSubClassOfAxiom(ax.getSubClass(), 
                                        dataFactory.getOWLObjectSomeValuesFrom(
                                                    someValuesFrom.getProperty(), op));
                                changes.add(new AddAxiom(ont, replAx));
                            }
                        }
                    }
                    
                }
            }
        }
        this.getOwlGraphWrapper().getManager().applyChanges(changes);
        this.triggerWrapperUpdate();
        
        log.info("OWLObjectIntersectionOf relaxation done.");
    }
    
    /**
     * Remove any {@code OWLEquivalentClassesAxiom} containing an {@code OWLObjectUnionOf} 
     * as class expression, and any {@code OWLSubClassOfAxiom} whose superclass is an 
     * {@code OWLObjectUnionOf}.
     * 
     * @see #performDefaultModifications()
     * @see #reverseOWLObjectUnionOfs()
     */
    private void removeOWLObjectUnionOfs() {
        log.info("Removing OWLEquivalentClassesAxiom or OWLSubClassOfAxiom containig OWLObjectUnionOf...");
        
        for (OWLOntology ont : this.getOwlGraphWrapper().getAllOntologies()) {
            for (OWLAxiom ax: ont.getAxioms()) {
                boolean toRemove = false;
                if (ax instanceof OWLSubClassOfAxiom) {
                    if (((OWLSubClassOfAxiom) ax).getSuperClass() instanceof  OWLObjectUnionOf) {
                        toRemove = true;
                    }
                } else if (ax instanceof OWLEquivalentClassesAxiom) {
                    for (OWLClassExpression ce : 
                        ((OWLEquivalentClassesAxiom) ax).getClassExpressions()) {
                        if (ce instanceof  OWLObjectUnionOf) {
                            toRemove = true;
                            break;
                        }
                    }
                }
                if (toRemove) {
                    ont.getOWLOntologyManager().removeAxiom(ont, ax);
                }
            }
        }

        this.triggerWrapperUpdate();
        log.info("Done removing OWLObjectUnionOfs");
    }
    
    /**
     * Remove all relations incoming to or outgoing from obsolete classes. This is  
     * because we need to keep obsolete classes in the ontology for valuable replaced_by 
     * annotations, yet we do not want their relations to interfere with relation reduction, 
     * etc (obsolete classes should have no relations, but it often happen 
     * that they have some).
     * 
     * @see #performDefaultModifications()
     */
    private void removeObsoleteClassRels() {
        log.info("Removing all relations incoming to or outgoing from obsolete classes...");
        
        for (OWLOntology ont : this.getOwlGraphWrapper().getAllOntologies()) {
            for (OWLClass cls: ont.getClassesInSignature()) {
                if (this.getOwlGraphWrapper().isObsolete(cls) || 
                        this.getOwlGraphWrapper().getIsObsolete(cls)) {
                    Set<OWLGraphEdge> edges = this.getOwlGraphWrapper().getOutgoingEdges(cls);
                    edges.addAll(this.getOwlGraphWrapper().getIncomingEdges(cls));
                    this.removeEdges(edges);
                }
            }
        }
        
        log.info("Done removing relations of obsolete classes.");
    }

	//*********************************
	//    MAKE BASIC ONTOLOGY EXAMPLE
	//*********************************
    /**
     * Make a basic ontology, with only is_a, part_of, and develops_from relations, 
     * and redundant relations removed, and redundant relations over is_a/part_of 
     * removed. This method calls {@link #simplifies(Collection, Collection, Collection, 
     * Collection, Collection) simplifies}, by providing only 
     * the object property IDs of the "part_of" and "develops_from" relations.
     * 
     * @see #simplifies(Collection, Collection, Collection, Collection, Collection)
     */
    public void makeSimpleOntology() {
    	log.info("Start building a basic ontology...");

    	this.simplifies(Arrays.asList(PARTOFID, DVLPTFROMID), null, null, null, null);
    }
    
    /**
     * Helper method to perform standard simplifications. All parameters are optional. 
     * Operations performed will be to call in order: 
     * <ul>
     *   <li>{@link #removeClassAndPropagateEdges(String)} on each of the {@code String} 
     *   in {@code classIdsToRemove}.
     *   <li>{@link #removeUnrelatedRelations(Collection)} using {@code relIds}.
     *   <li>{@link #reduceRelations()}
     *   <li>{@link #reducePartOfIsARelations()}
     *   <li>{@link #mapRelationsToParent(Collection)} using {@code relIds}.
     *   <li>{@link #filterRelations(Collection, boolean)} using {@code relIds} and 
     *   {@code true} as the second parameter.
     *   <li>{@link #removeSubgraphs(Collection, boolean)} using {@code toRemoveSubgraphRootIds} 
     *   and {@code false} as the second parameter.
     *   <li>{@link #filterSubgraphs(Collection)} using {@code toFilterSubgraphRootIds}.
     *   <li>{@link #removeRelsToSubsets(Collection, Collection)} using {@code subsetNames} 
     *   as first argument, and {@code toFilterSubgraphRootIds} as second argument. 
     * </ul>
     * @param classIdsToRemove          A {@code Collection} of {@code String}s to call 
     *                                  {@link #removeClassAndPropagateEdges(String)} 
     * @param relIds                    A {@code Collection} of {@code String}s to call 
     *                                  {@link #mapRelationsToParent(Collection)} and 
     *                                  #filterRelations(Collection, boolean)}.
     * @param toRemoveSubgraphRootIds   A {@code Collection} of {@code String}s to call 
     *                                  {@link #removeSubgraphs(Collection, boolean)}.
     *                                  on each of them.
     * @param toFilterSubgraphRootIds   A {@code Collection} of {@code String}s to call 
     *                                  {@link #filterSubgraphs(Collection)}.
     * @param subsetNames               A {@code Collection} of {@code String}s to call 
     *                                  {@link #removeRelsToSubsets(Collection)}.
     */
    public void simplifies(Collection<String> classIdsToRemove, Collection<String> relIds, 
            Collection<String> toRemoveSubgraphRootIds,
            Collection<String> toFilterSubgraphRootIds,   
            Collection<String> subsetNames) {
        

        for (String classIdToRemove: classIdsToRemove) {
            this.removeClassAndPropagateEdges(classIdToRemove);
        }
        
        if (relIds != null && !relIds.isEmpty()) {
            this.removeUnrelatedRelations(relIds);
        }
        
        this.reduceRelations();
        this.reducePartOfIsARelations();
        
        if (relIds != null && !relIds.isEmpty()) {
            this.mapRelationsToParent(relIds);
            this.filterRelations(relIds, true);
        }
        if (toRemoveSubgraphRootIds != null && !toRemoveSubgraphRootIds.isEmpty()) {
            this.removeSubgraphs(toRemoveSubgraphRootIds, false);
        }
        if (toFilterSubgraphRootIds != null && !toFilterSubgraphRootIds.isEmpty()) {
            this.filterSubgraphs(toFilterSubgraphRootIds);
        }
        if (subsetNames != null && !subsetNames.isEmpty()) {
            this.removeRelsToSubsets(subsetNames, toFilterSubgraphRootIds);
        }
    }

	//*********************************
	//    RELATION REDUCTIONS
	//*********************************
	/**
	 * Remove redundant relations. A relation is considered redundant 
	 * when there exists a composed relation between two classes 
	 * (separated by several relations), that is equivalent to -or more precise than- 
	 * a direct relation between these classes. The direct relation is considered redundant 
	 * and is removed. 
	 * This method returns the number of such direct redundant relations removed. 
	 * <p>
	 * When combining the relations, they are also combined over super properties (see 
	 * {@link OWLGraphWrapperEdgesExtended#combineEdgePairWithSuperPropsAndGCI(
	 * OWLGraphEdge, OWLGraphEdge)})
	 * <p>
	 * Examples of relations considered redundant by this method:
	 * <ul>
	 * <li>If r is transitive, if A r B r C, then A r C is a redundant relation. 
	 * <li>If r1 is the parent relation of r2, and r1 is transitive, and if 
	 * A r2 B r1 C, then A r1 C is a redundant relation (check relations composed 
	 * over super properties).
	 * <li>If r1 is the parent relation of r2, and r2 is transitive, and if 
	 * A r2 B r2 C, then A r1 C is a redundant relation (composed relation more precise 
	 * than the direct one).
	 * </ul>
	 * <p>
     * <strong>Important:</strong> If it is planed to also call 
     * {@link #filterRelations(Collection, boolean)}, and the ontology is very large, 
     * it is recommended to first call {@link #removeUnrelatedRelations(Collection)}, 
     * before calling this method.
	 * 
	 * @return 	An {@code int} representing the number of relations removed. 
	 * @see #reducePartOfIsARelations()
	 * @see #removeUnrelatedRelations(Collection)
	 */
	public int reduceRelations() {
		return this.reduceRelations(false);
	}
	/**
	 * Remove redundant relations by considering is_a (SubClassOf) 
	 * and part_of relations equivalent. This method removes <strong>only</strong> 
	 * these "fake" redundant relations over is_a and part_of, 
	 * and not the real redundant relations over, for instance, is_a relations only. 
	 * Note that the modified ontology will therefore not be semantically correct, 
	 * but will be easier to display, thanks to a simplified graph structure. 
	 * <p>
	 * This method is similar to {@link #reduceRelations()}, except is_a and part_of 
	 * are considered equivalent, and that only these "fake" redundant relations are removed. 
	 * <p>
	 * <strong>Warning: </strong>if you call both the methods {@code reduceRelations} 
	 * and {@code reducePartOfIsARelations} on the same ontologies, 
	 * you must call {@code reduceRelations} first, 
	 * as it is a semantically correct reduction.
	 * <p>
	 * Here are examples of relations considered redundant by this method:
	 * <ul>
	 * <li>If A is_a B is_a C, then A part_of C is considered redundant
	 * <li>If A in_deep_part_of B in_deep_part_of C, then A is_a C is considered redundant 
	 * (check for sub-properties of part_of)
	 * <li>If A part_of B, and A is_a B, then A part_of B is removed (check for redundant 
	 * direct outgoing edges; in case of redundancy, the part_of relation is removed)
	 * </ul>
	 * Note that redundancies such as A is_a B is_a C and A is_a C are not removed by this method, 
	 * but by {@link #reduceRelations()}.
	 * 
	 * @return 	An {@code int} representing the number of relations removed. 
	 * @see #reduceRelations()
	 */
	public int reducePartOfIsARelations() {
		return this.reduceRelations(true);
	}
	/**
	 * Perform relation reduction, that is either semantically correct, 
	 * or is also considering is_a (SubClassOf) and part_of relations equivalent, 
	 * depending on the parameter {@code reducePartOfAndIsA}. 
	 * <p>
	 * This method is needed to be called by {@link #reduceRelations()} (correct reduction) 
	 * and {@link #reducePartOfIsARelations()} (is_a/part_of equivalent), 
	 * as it is almost the same code to run.
	 *  
	 * @param reducePartOfAndIsA 	A {@code boolean} defining whether 
	 * 										is_a/part_of relations should be considered 
	 * 										equivalent. If {@code true}, they are.
	 * @return 		An {@code int} representing the number of relations removed. 
	 * @see #reduceRelations()
	 * @see #reducePartOfIsARelations()
	 */
	private int reduceRelations(boolean reducePartOfAndIsA) {
		if (!reducePartOfAndIsA) {
		    log.info("Start relation reduction...");
		} else {
			log.info("Start \"fake\" relation reduction over is_a/part_of...");
		}
		
		//we will go the hardcore way: iterate each class, 
		//and for each class, check each outgoing edges
		//todo?: everything could be done in one single walk from bottom nodes 
		//to top nodes, this would be much faster, but would require much more memory.
		Set<OWLClass> allClasses = 
		        new HashSet<OWLClass>(this.getOwlGraphWrapper().getAllOWLClasses());
		
		int relationsRemoved = 0;
        //variables for logging purpose
        int classIndex = 0;
		for (OWLClass iterateClass: allClasses) {
		    
		    if (log.isInfoEnabled()) {
		        classIndex++;
		        log.info("Start examining class " + classIndex + "/" + 
		                allClasses.size() + " " + iterateClass + "...");
		    }
		    //do not reduce relations for obsolete classes
		    if (!this.getOwlGraphWrapper().isRealClass(iterateClass)) {
		        log.trace("Obsolete class, discarded");
		        continue;
		    }
		    
		    Set<OWLGraphEdge> outgoingEdges = 
		            this.getOwlGraphWrapper().getOutgoingEdgesWithGCI(iterateClass);
		    //another set to keep track of the edgesToTest removed
		    Set<OWLGraphEdge> outgoingEdgesRemoved = new HashSet<OWLGraphEdge>();
		    //variables used for logging purpose
		    int edgeIndex = 0;
		    int outgoingEdgesCount = 0;
		    if (log.isDebugEnabled()) {
		        outgoingEdgesCount = outgoingEdges.size();
		    }
		    //now for each outgoing edge, try to see if it is redundant, 
		    //as compared to composed relations obtained by walks to the top 
		    //starting from the other outgoing edges, at any distance
		    for (OWLGraphEdge outgoingEdgeToTest: outgoingEdges) {
		        if (log.isTraceEnabled()) {
		            edgeIndex++;
		            log.trace("Start testing edge for redundancy " + edgeIndex + 
		                    "/" + outgoingEdgesCount + " " + outgoingEdgeToTest);
		        }
		        //do not reduce relations to obsolete classes
	            if (!this.getOwlGraphWrapper().isRealClass(outgoingEdgeToTest.getTarget())) {
	                log.trace("Edge going to obsolete class, discarded");
	                continue;
	            }
		        
		        boolean isRedundant = false;
		        for (OWLGraphEdge outgoingEdgeToWalk: outgoingEdges) {
		            if (outgoingEdgeToWalk.equals(outgoingEdgeToTest) || 
		                outgoingEdgesRemoved.contains(outgoingEdgeToWalk)) {
		                continue;
		            }
		            
		            isRedundant = this.areEdgesRedudant(outgoingEdgeToTest, 
		                    outgoingEdgeToWalk, reducePartOfAndIsA);
		            if (isRedundant) {
		                break;
		            }
		        }
		        if (isRedundant) {
		            if (this.removeEdge(outgoingEdgeToTest)) {
		                relationsRemoved++;
                        outgoingEdgesRemoved.add(outgoingEdgeToTest);
		                if (log.isDebugEnabled()) {
		                    log.debug("Tested edge is redundant and is removed: " + 
		                            outgoingEdgeToTest);
		                }
		            } else {
		                throw new AssertionError("Relation removal failed: " + 
		                        outgoingEdgeToTest);
		            }
		        } else {
		            if (log.isTraceEnabled()) {
		                log.trace("Done testing edge for redundancy, not redundant: " + 
		                        outgoingEdgeToTest);
		            }
		        }
		    }
	    }
		
	    if (log.isInfoEnabled()) {
		    log.info("Done relation reduction, " + relationsRemoved + " relations removed.");
	    }
	    return relationsRemoved;
	}
	
	/**
	 * Check, for two {@code OWLGraphEdge}s {@code edgeToTest} and 
	 * {@code edgeToWalk}, outgoing from a same source, if a composed relation 
	 * equivalent to {@code edgeToTest} can be obtained by a walk starting from  
	 * {@code edgeToWalk}, to the top of the ontology (at any distance). 
	 * <p>
	 * In that case, {@code edgeToTest} is considered redundant, 
	 * and this method returns {@code true}. 
	 * if {@code reducePartOfAndIsA} is {@code true}, 
	 * only if a relation is a part_of relation on the one hand, an is_a relation 
	 * on the other hand, will they be considered equivalent. 
	 * <p>
	 * {@code edgeToTest} and {@code edgeToWalk} will also be considered redundant 
	 * if they have the same target, and {@code edgeToWalk} is a sub-relation of 
	 * {@code edgeToTest} (or, if {@code reducePartOfAndIsA} is {@code true}, 
	 * when {@code edgeToTest} is a part_of-like relation and {@code edgeToWalk} 
	 * a is_a relation (because we prefer to keep the is_a relation)).
	 * <p>
	 * Note that relations are also combined over super properties (see 
	 * {@link OWLGraphWrapperEdgesExtended#combineEdgePairWithSuperPropsAndGCI(
	 * OWLGraphEdge, OWLGraphEdge)}.
	 * 
	 * @param edgeToTest				The {@code OWLGraphEdge} to be checked 
	 * 									for redundancy. 
	 * @param edgeToWalk				The {@code OWLGraphEdge} that could potentially 
	 * 									lead to a relation equivalent to {@code edgeToTest}, 
	 * 									by combining each relation walked to the top 
	 * 									of the ontology.
	 * @param reducePartOfAndIsA		A {@code boolean} defining whether 
	 * 									is_a/part_of relations should be considered 
	 * 									equivalent. If {@code true}, they are.
	 * @return		{@code true} if {@code edgeToTest} is redundant as compared 
	 * 				to a relation obtained from {@code edgeToWalk}.
	 * @throws IllegalArgumentException If {@code edgeToTest} and {@code edgeToWalk}
	 * 									are equal, or if they are not outgoing from a same source.
	 * @see #reduceRelations()
	 * @see #reducePartOfIsARelations()
	 */
	private boolean areEdgesRedudant(OWLGraphEdge edgeToTest, OWLGraphEdge edgeToWalk, 
			boolean reducePartOfAndIsA) throws IllegalArgumentException {
	    if (log.isTraceEnabled()) {
	        log.trace("Edge tested for reundancy: " + edgeToTest + 
	                " - Edge starting the walk: " + edgeToWalk);
	    }
		//TODO: try to see from the beginning that there is no way 
		//edgeToTest and edgeToWalk are redundant. 
		//(it should be based on the actual chain rules, not hardcoded). 
		if (edgeToTest.equals(edgeToWalk) || 
				!edgeToTest.getSource().equals(edgeToWalk.getSource())) {
			throw new IllegalArgumentException("edgeToTest and edgeToWalk must be " +
					"different edges outgoing from a same OWLObject: " + 
					edgeToTest + " - " + edgeToWalk);
		}
		//if we want to reduce over is_a/part_of relations, and 
		//edgeToTest is not itself a is_a or part_of-like relation, 
		//no way to have a part_of/is_a redundancy
		if (reducePartOfAndIsA && 
			!this.isAPartOfEdge(edgeToTest) && 
			!this.isASubClassOfEdge(edgeToTest)) {
		    if (log.isTraceEnabled()) {
			    log.trace("Edge to test is not a is_a/part_of relation, " +
			    		"cannot be redundant: " + edgeToTest);
		    }
			return false;
		}

        OWLGraphWrapper wrapper = this.getOwlGraphWrapper();
        
        //do not reduce relations to obsolete classes
        if (!this.getOwlGraphWrapper().isRealClass(edgeToTest.getTarget())) {
            log.trace("Edge to test going to obsolete class, discarded");
            return false;
        }
        if (!this.getOwlGraphWrapper().isRealClass(edgeToWalk.getTarget())) {
            log.trace("Edge to walk going to obsolete class, discarded");
            return false;
        }
		
		//if they are completely unrelated GCI relations, 
		//no way to have redundancy
		if (!edgeToWalk.equalsGCI(edgeToTest) && 
                !wrapper.hasFirstEdgeMoreGeneralGCIParams(edgeToWalk, edgeToTest)) {
		    if (log.isTraceEnabled()) {
                log.trace("Unrelated GCI parameters, cannot be redundant. " + 
                        edgeToTest + " - " + edgeToWalk);
            }
		    return false;
		}
		
		//then, check that the edges are not themselves redundant. 
		//GCI params already checked
		if (edgeToTest.getTarget().equals(edgeToWalk.getTarget())) {
			//if we want to reduce over is_a/part_of relations
			if (reducePartOfAndIsA) {
				//then, we will consider edgeToTest redundant 
				//only if edgeToTest is a part_of-like relation, 
				//and edgeToWalk an is_a relation (because we prefer to keep the is_a relation, 
			    //the part_of relation will be removed when edgeToWalk will be tested)
				if (this.isAPartOfEdge(edgeToTest) && this.isASubClassOfEdge(edgeToWalk)) {
					return true;
				}
				//unless the part_of relation has more general GCI filler and relation
				else if (this.isAPartOfEdge(edgeToWalk) && this.isASubClassOfEdge(edgeToTest) && 
				        wrapper.hasFirstEdgeMoreGeneralGCIParams(edgeToWalk, edgeToTest)) {
				    return true;
				}
		    //check that they are not identical edges from two different ontologies; 
			//GCI parameters were already checked
			} else if (edgeToWalk.equalsIgnoreOntologyAndGCI(edgeToTest)) {
			    return true;
			//otherwise, check that edgeToWalk is not a sub-relation of edgeToTest.
			//GCI parameters were already checked.
			} else {
			    for (OWLGraphEdge subsume: this.getOwlGraphWrapper().
                        getOWLGraphEdgeSubsumers(edgeToWalk)) {
			        if (subsume.equalsIgnoreOntologyAndGCI(edgeToTest)) {
			            return true;
			        }
			    }
			}
		}
		
		//--------OK, real stuff starts here----------
		
	    //create a Deque to walk all composed relations, 
		//without using a recursive function.
	    Deque<OWLGraphEdge> walk = new ArrayDeque<OWLGraphEdge>();
	    walk.addFirst(edgeToWalk);
	    //store all composed relations, to be able to detect cycles
	    Set<OWLGraphEdge> allComposedEdges = new HashSet<OWLGraphEdge>();
	
	    OWLGraphEdge walkedEdge;
	    while ((walkedEdge = walk.pollFirst()) != null) {
	    	if (log.isTraceEnabled()) {
	    	    log.trace("Current combined edge tested: " + walkedEdge);
	    	}

	    	//get the outgoing edges starting from the target of walkedEdge, 
	    	//and compose these relations with it, 
	    	//trying to get a composed edge with only one relation (one property)
	    	nextEdge: for (OWLGraphEdge nextEdge: 
	    	    this.getOwlGraphWrapper().getOutgoingEdgesWithGCI(walkedEdge.getTarget())) {
	    	    
	    	    if (log.isTraceEnabled()) {
	                log.trace("Current raw edge walked: " + nextEdge);
	            }
	            
	    	    //do not reduce relations to obsolete classes
	            if (!this.getOwlGraphWrapper().isRealClass(nextEdge.getTarget())) {
	                log.trace("Edge going to obsolete class, discarded");
	                continue nextEdge;
	            }
	            //if they are completely unrelated GCI relations, 
	            //no way to have redundancy
	            if (!nextEdge.equalsGCI(edgeToTest) && 
	                    !wrapper.hasFirstEdgeMoreGeneralGCIParams(nextEdge, edgeToTest)) {
	                if (log.isTraceEnabled()) {
	                    log.trace("Unrelated GCI parameters, cannot be redundant, stop this walk.");
	                }
	                continue nextEdge;
	            }

	    	    //check that the target of nextEdge is not the source of edgeToTest. 
	    	    //We had problem with reciprocal relations between terms, using relations 
	    	    //that are the inverse of each other. For instance: 
	    	    //A synapsed_by B and A synapsed_to B
	    	    //B synapsed_by A and B synapsed_to A. 
	    	    //there will be several round walk generating non-identical composed relations, 
	    	    //before detecting a cycle with the code used later.
	    	    //-------------------------
	    	    //===NOTE JUNE 2014===: disabling this check, it might be a performance issue, 
	    	    //but maybe the results are incorrect when discarding such reciprocal relations.
	    	    //-------------------------
	    	    /*if (edgeToTest.getSource().equals(nextEdge.getTarget())) {
	    	        log.trace("A walk leads to the source of the edge currently under test, stop this walk.");
	    	        continue nextEdge;
	    	    }*/
	    	    
				//check that nextEdge has the target of edgeToTest
				//on its path, otherwise stop this walk here 
	    	    //(edgeToTest.getTarget() should be a named object, this is why 
	    	    //we can use the method getNamedAncestorsWithGCI)
				if (!nextEdge.getTarget().equals(edgeToTest.getTarget()) && 
				        !this.getOwlGraphWrapper().getNamedAncestorsWithGCI(
				        nextEdge.getTarget()).contains(edgeToTest.getTarget())) {
				    log.trace("Target not on path, stop this walk.");
					continue nextEdge;
				}
			    
	    		OWLGraphEdge combine = 
	    				this.getOwlGraphWrapper().combineEdgePairWithSuperPropsAndGCI(
	    				        walkedEdge, nextEdge);
	    		if (log.isTraceEnabled()) {
                    log.trace("Resulting combined edge: " + combine);
                }

	    		//if there is a cycle in the ontology: 
	    		boolean cycle = false;
	    		//we do not use the contains method, to be able to use equalsIgnoreOntology, 
	    		//rather than equals.
	    		for (OWLGraphEdge edgeAlreadyWalked: allComposedEdges) {
	    		    if (edgeAlreadyWalked.equalsIgnoreOntology(combine)) {
	    		        cycle = true;
	    		        break;
	    		    }
	    		}
	    		if (cycle) {
	    			if (log.isTraceEnabled()) {
	    			    log.trace("Relation already seen, stop this walk: " + combine);
	    			}
	    			continue nextEdge;
	    		}

    			//at this point, if the properties have not been combined, 
    			//there is nothing we can do.
	    		if (combine == null || combine.getQuantifiedPropertyList().size() > 1) {
	    			continue nextEdge;
	    		}

	    		//edges successfully combined into one relation,
	    		//check if this combined relation (or one of its parent relations) 
	    		//corresponds to edgeToTest; 
	    		//in that case, it is redundant and should be removed
	    		if (combine.getTarget().equals(edgeToTest.getTarget()) && 
	    		        (edgeToTest.equalsGCI(combine) || 
	    		            wrapper.hasFirstEdgeMoreGeneralGCIParams(combine, edgeToTest))) {
	    			//if we want to reduce over is_a and part_of relations
	    			if (reducePartOfAndIsA) {
	    				//part_of/is_a redundancy
	    				if (((this.isASubClassOfEdge(edgeToTest) && this.isAPartOfEdge(combine)) ||  
	    					 (this.isASubClassOfEdge(combine)    && this.isAPartOfEdge(edgeToTest)))) {
	    					
	    					return true;
	    				}
	    		    //GCI parameters were already checked
	    			} else if (edgeToTest.equalsIgnoreOntologyAndGCI(combine)) {
	    			    return true;
	    			} else {
	    			    for (OWLGraphEdge subsumer: this.getOwlGraphWrapper().
                                    getOWLGraphEdgeSubsumers(combine)) {
	                        //GCI parameters were already checked
	    			        if (subsumer.equalsIgnoreOntologyAndGCI(edgeToTest)) {
	    			            return true;
	    			        }
	    			    }
	    			}
	    			
		    		//otherwise, as we met the target of the tested edge, 
	    			//we can stop this walk here
	    			continue nextEdge;
	    		}

	    		//continue the walk for this combined edge
	    		walk.addFirst(combine);
	    		allComposedEdges.add(combine);
	    	}
	    }
	    
	    return false;
	}

	//*********************************
	//    REMOVE CLASS PROPAGATE EDGES
	//*********************************
    
    /**
	 * Remove the {@code OWLClass} with the OBO-style ID {@code classToRemoveId} 
	 * from the ontology, and propagate its incoming edges to the targets 
	 * of its outgoing edges. Each incoming edges are composed with each outgoing edges (see 
	 * {@link OWLGraphWrapperEdgesExtended#combineEdgePairWithSuperPropsAnGCI(
	 * OWLGraphEdge, OWLGraphEdge)}).
	 * <p>
	 * This method returns the number of relations propagated and actually added 
	 * to the ontology (propagated relations corresponding to a relation already 
	 * existing in the ontology, or a less precise relation than an already existing one, 
	 * will not be counted). It returns 0 only when no relations were propagated (or added). 
	 * Rather than returning 0 when the  {@code OWLClass} could not be found or removed, 
	 * an {@code IllegalArgumentException} is thrown. 
	 * 
	 * @param classToRemoveId 	A {@code String} corresponding to the OBO-style ID 
	 * 							of the {@code OWLClass} to remove. 
	 * @return 					An {@code int} corresponding to the number of relations 
	 * 							that could be combined, and that were actually added 
	 * 							to the ontology. 
	 * @throws IllegalArgumentException	If no {@code OWLClass} corresponding to 
	 * 									{@code classToRemoveId} could be found, 
	 * 									or if the class could not be removed. This is for the sake 
	 * 									of not returning 0 when such problems appear, but only 
	 * 									when no relations were propagated. 
	 */
    public int removeClassAndPropagateEdges(String classToRemoveId) 
    		throws IllegalArgumentException
    {
    	
    	OWLClass classToRemove = 
    			this.getOwlGraphWrapper().getOWLClassByIdentifier(classToRemoveId);
    	if (classToRemove == null) {
    		throw new IllegalArgumentException(classToRemoveId + 
    				" was not found in the ontology");
    	}
    	
    	if (log.isInfoEnabled()) {
    	    log.info("Start removing class " + classToRemove + 
    	            " and propagating edges...");
    	}
    	//update cache so that we make sure the last AssertionError at the end of the method 
    	//will not be thrown by mistake
    	this.getOwlGraphWrapper().clearCachedEdges();
    	
    	//propagate the incoming edges to the targets of the outgoing edges.
    	//start by iterating the incoming edges.
    	int couldNotCombineWarnings = 0;
    	//edges to be added for propagating relations
    	Set<OWLGraphEdge> newEdges = new HashSet<OWLGraphEdge>();
    	
    	for (OWLGraphEdge incomingEdge: 
    			    this.getOwlGraphWrapper().getIncomingEdgesWithGCI(classToRemove)) {
    	    
    	    //now propagate each incoming edge to each outgoing edge
    	    for (OWLGraphEdge outgoingEdge: 
    	        this.getOwlGraphWrapper().getOutgoingEdgesWithGCI(classToRemove)) {
    	        if (log.isDebugEnabled()) {
    	            log.debug("Trying to combine incoming edge " + incomingEdge + 
    	                    " with outgoing edge " + outgoingEdge);
    	        }
    	        //combine edges
    	        OWLGraphEdge combine = 
    	                this.getOwlGraphWrapper().combineEdgePairWithSuperPropsAndGCI(
    	                        incomingEdge, outgoingEdge);
    	        //successfully combined
    	        if (combine != null && combine.getQuantifiedPropertyList().size() == 1) {
    	            if (log.isDebugEnabled()) {
    	                log.debug("Successfully combining edges into: " + combine);
    	            }
    	            
    	            //check if there is an already existing relation equivalent 
    	            //to the combined one, or a more precise one
    	            boolean alreadyExist = false;
    	            for (OWLGraphEdge existingEdge: 
    	                this.getOwlGraphWrapper().getOutgoingEdgesWithGCI(combine.getSource())) {
    	                for (OWLGraphEdge combineTest: this.getOwlGraphWrapper().
                                getOWLGraphEdgeSubRelsReflexive(combine)) {
    	                    if (existingEdge.equalsIgnoreOntology(combineTest)) {
    	                        alreadyExist = true;
    	                        break;
    	                    } 
    	                }
    	            }
    	            if (!alreadyExist) {
    	                newEdges.add(combine);
    	                log.debug("Combined relation does not already exist and will be added");
    	            } else {
    	                log.debug("Equivalent or more precise relation already exist, combined relation not added");
    	            }
    	        } else {
    	            couldNotCombineWarnings++;
    	            log.debug("Could not combine edges.");
    	        }
    	    }
    	}
    	//now remove the class
    	if (log.isDebugEnabled()) {
    	    log.debug("Removing class " + classToRemove);
    	}
    	
    	if (!this.removeClass(classToRemove)) {
    		//if the class was not removed from the ontology, throw an IllegalArgumentException 
    		//(maybe it was only in the owltools cache for instance?).
    		//it allows to distinguish the case when this method returns 0 because 
    		//there was no incoming edge to propagate, from the case when it is because 
    		//the class was not removed from the ontology
    		throw new IllegalArgumentException(classToRemove + 
    				" could not be removed from the ontology");
    	}
    	
    	//now, add the propagated edges to the ontology
    	int edgesPropagated = this.addEdges(newEdges);
    	//test that everything went fine
    	if (edgesPropagated != newEdges.size()) {
    		throw new AssertionError("Incorrect number of propagated edges added, " +
    				"expected " + newEdges.size() + ", but was " + edgesPropagated);
    	}
    	
    	if (log.isInfoEnabled()) {
    	    log.info("Done removing class and propagating edges, " + edgesPropagated + 
    	    " edges propagated, " + couldNotCombineWarnings + " could not be propagated");
    	}
    	
    	return edgesPropagated;
    }

	//*********************************
	//    MAP RELATIONS TO PARENTS
	//*********************************

    /**
     * Replace the sub-relations of {@code parentRelations} by these parent relations. 
     * {@code parentRelations} contains the OBO-style IDs of the parent relations 
     * (for instance, "BFO:0000050"). All their sub-relations will be replaced by 
     * these parent relations. If a relation in {@code parentRelations} is the sub-relation 
     * of another parent relation, it will not be mapped to this other parent relation, 
     * and their common sub-relations will be mapped to the more precise one.
     * <p>
     * For instance, if {@code parentRelations} contains "RO:0002202" ("develops_from" ID), 
     * all sub-relations will be replaced: "transformation_of" relations will be replaced 
     * by "develops_from", "immediate_transformation_of" will be replaced by "develops_from", ...
     * <p>
     * Note that if mapping a relation to its parent produces an already existing relation, 
     * the sub-relation will then be simply removed.
     * <p>
     * <strong>Important:</strong> If it is planed to also call {@link #reduceRelations()}, 
     * this method must be called first, otherwise, already mapped relations could reappear 
     * due to property chain rules.
     * 
     * @param parentRelations 	A {@code Collection} of {@code String}s containing 
     * 							the OBO-style IDs of the parent relations, that should replace 
     * 							all their sub-relations.
     * @return					An {@code int} that is the number of relations replaced 
     * 							or removed.
     * 
     * @see #mapRelationsToParent(Collection, Collection)
     */
    public int mapRelationsToParent(Collection<String> parentRelations) {
    	return this.mapRelationsToParent(parentRelations, null);
    }
    /**
     * Identical to {@link #mapRelationsToParent(Collection)}, except that this method 
     * allows to filter relations not to be mapped to parents: 
     * <p>
     * If a sub-relation of a relation in {@code parentRelations} should not be mapped, 
     * its OBO-style ID should be added to {@code relsExcluded}. All sub-relations 
     * of {@code relsExcluded} are excluded from replacement. 
     * <p>
     * Note that if mapping a relation to its parent produces an already existing relation, 
     * the sub-relation will then be simply removed.
     * <p>
     * <strong>Important:</strong> If it is planed to also call {@link #reduceRelations()}, 
     * this method must be called first, otherwise, already mapped relations could reappear 
     * due to property chain rules.
     * 
     * @param parentRelations 	A {@code Collection} of {@code String}s containing 
     * 							the OBO-style IDs of the parent relations, that should replace 
     * 							all their sub-relations, except those in {@code relsExcluded}.
     * @param relsExcluded		A {@code Collection} of {@code String}s containing 
     * 							the OBO-style IDs of the relations excluded from replacement. 
     * 							All their sub-relations will be also be excluded.
     * @return					An {@code int} that is the number of relations replaced 
     * 							or removed.
     * 
     * @see #mapRelationsToParent(Collection)
     */
    public int mapRelationsToParent(Collection<String> parentRelations, 
    		Collection<String> relsExcluded)
    {
        if (log.isInfoEnabled()) {
    	    log.info("Replacing relations by their parent relation: " + parentRelations + 
    	            " - except relations: " + relsExcluded);
        }
    	//update cache so that we make sure the last AssertionError at the end of the method 
    	//will not be thrown by mistake
    	this.getOwlGraphWrapper().clearCachedEdges();
    	
    	//first, get the properties corresponding to the excluded relations, 
    	//and their sub-relations, not to be mapped to their parent
    	Set<OWLObjectPropertyExpression> relExclProps = 
    			new HashSet<OWLObjectPropertyExpression>();
    	if (relsExcluded != null) {
    		for (String relExclId: relsExcluded) {
    			OWLObjectProperty relExclProp = 
    					this.getOwlGraphWrapper().getOWLObjectPropertyByIdentifier(relExclId);
    			if (relExclProp != null) {
    				relExclProps.addAll(
    					this.getOwlGraphWrapper().getSubPropertyReflexiveClosureOf(relExclProp));
    			}
    		}
    	}
    	
    	//get the properties corresponding to the parent relations, 
    	//and create a map where their sub-properties are associated to it.
    	Map<OWLObjectPropertyExpression, OWLObjectPropertyExpression> subPropToParent = 
    			new HashMap<OWLObjectPropertyExpression, OWLObjectPropertyExpression>();
    	
    	//get all the parent Object Properties first
    	Set<OWLObjectProperty> parentProps = new HashSet<OWLObjectProperty>();
    	for (String parentRelId: parentRelations) {
    		OWLObjectProperty parentProp = 
    				this.getOwlGraphWrapper().getOWLObjectPropertyByIdentifier(parentRelId);
    		if (parentProp != null) {
    		    parentProps.add(parentProp);
    		}
    	}
    	//get and filter their subproperties
    	for (OWLObjectProperty parentProp: parentProps) {
    	    Set<OWLObjectPropertyExpression> subProps = 
                    this.getOwlGraphWrapper().getSubPropertyClosureOf(parentProp);
    	    for (OWLObjectPropertyExpression subProp: subProps) {
    	        //put in the map only the sub-properties that actually need to be mapped: 
    	        //properties not excluded, and not itself a parentProp to map relations to.
    	        if (!relExclProps.contains(subProp) &&
    	                !parentProps.contains(subProp)) {
    	            //also check that if the mapping is already present, we use 
    	            //the more precise one (in case one of the property in parentProps 
    	            //is the parent of another property in parentProps)
    	            OWLObjectPropertyExpression existingParentProp = subPropToParent.get(subProp);
    	            if (existingParentProp == null || !subProps.contains(existingParentProp)) {
    	                subPropToParent.put(subProp, parentProp);
    	            }
    	        }
    	    }
    	}
    	
    	//now, check each outgoing edge of each OWL class of each ontology
    	Set<OWLClass> allClasses = new HashSet<OWLClass>();
    	for (OWLOntology ontology: this.getOwlGraphWrapper().getAllOntologies()) {
    	    allClasses.addAll(ontology.getClassesInSignature());
    	}
    	Set<OWLGraphEdge> edgesToRemove = new HashSet<OWLGraphEdge>();
    	Set<OWLGraphEdge> edgesToAdd    = new HashSet<OWLGraphEdge>();
    	for (OWLClass iterateClass: allClasses) {
    	    for (OWLGraphEdge edge: 
    	        this.getOwlGraphWrapper().getOutgoingEdgesWithGCI(iterateClass)) {
    	        
    	        //if it is a sub-property that should be mapped to a parent
    	        OWLObjectPropertyExpression parentProp;
    	        if ((parentProp = subPropToParent.get(
    	                edge.getSingleQuantifiedProperty().getProperty())) != null) {
    	            
    	            //store the edge to remove and to add, to perform all modifications 
    	            //at once (more efficient)
    	            edgesToRemove.add(edge);
    	            OWLGraphEdge newEdge = 
    	                    new OWLGraphEdge(edge.getSource(), edge.getTarget(), 
    	                            parentProp, edge.getSingleQuantifiedProperty().getQuantifier(), 
    	                            edge.getOntology(), null, 
    	                            edge.getGCIFiller(), edge.getGCIRelation());
    	            //check that the new edge does not already exists 
    	            //(redundancy in the ontology?)
    	            boolean alreadyExist = false;
    	            for (OWLGraphEdge existingEdge: 
    	                this.getOwlGraphWrapper().getOutgoingEdgesWithGCI(iterateClass)) {
    	                if (existingEdge.equalsIgnoreOntology(newEdge)) {
    	                    alreadyExist = true;
    	                    break;
    	                }
    	            }
    	            if (!alreadyExist) {
    	                edgesToAdd.add(newEdge);
    	                if (log.isDebugEnabled()) {
    	                    log.debug("Replacing relation " + edge + " by " + newEdge);
    	                }
    	            } else {
    	                if (log.isDebugEnabled()) {
    	                    log.debug("Removing " + edge + ", but " + newEdge + 
    	                            " already exists, will not be added");
    	                }
    	            }
    	        } 
    	    }
    	}
    	
    	if (log.isDebugEnabled()) {
    	    log.debug("Expecting " + edgesToRemove.size() + " relations to be removed, " + 
    	        edgesToAdd.size() + " relations to be added");
    	}
    	//the number of relations removed and added can be different if some direct 
    	//redundant relations were present in the ontology 
    	//(e.g., A part_of B and A in_deep_part_of B)
    	int removeCount = this.removeEdges(edgesToRemove);
    	int addCount    = this.addEdges(edgesToAdd);
    	
    	if (removeCount == edgesToRemove.size() && addCount == edgesToAdd.size()) {
    	    if (log.isInfoEnabled()) {
    		    log.info("Done replacing relations by their parent relation, " + 
    	            removeCount + " relations removed, " + addCount + " added");
    	    }
    	    return removeCount;
    	}
    	
    	throw new AssertionError("The relations were not correctly added or removed, " +
    			"expected " + edgesToRemove.size() + " relations removed, was " + removeCount + 
    			", expected " + edgesToAdd.size() + " relations added, was " + addCount);
    }
	
	//*********************************
	//    SUBGRAPH FILTERING OR REMOVAL
	//*********************************
	
    /**
     * Keep in the ontologies only the subgraphs starting 
     * from the provided {@code OWLClass}es, and their ancestors. 
     * {@code allowedSubgraphRootIds} contains the OBO-style IDs 
     * of these subgraph roots as {@code String}s.
     * <p>
     * All classes not part of these subgraphs, and not ancestors of these allowed roots, 
     * will be removed from the ontology. Also, any direct relation between an ancestor 
     * of a subgraph root and one of its descendants will be removed, 
     * as this would represent an undesired subgraph. 
     * <p>
     * This method returns the OBO-like IDs of the {@code OWLClass}es removed as a result.
     * <p>
     * This is the opposite method of {@code removeSubgraphs(Collection, boolean)}.
     * 
     * @param allowedSubgraphRootIds 	A {@code Collection} of {@code String}s 
     * 									representing the OBO-style IDs of the 
     * 									{@code OWLClass}es that are the roots of the 
     * 									subgraphs that will be kept in the ontology. 
     * 									Their ancestors will be kept as well.
     * @return                      A {@code Collection} of {@code String}s that are 
     *                              the OBO-like ID of the {@code OWLClass}es removed 
     *                              as a result of this method call.
     * @see #removeSubgraphs(Collection, boolean)
     */
    public Set<String> filterSubgraphs(Collection<String> allowedSubgraphRootIds) {
        int classCount   = 0;
        if (log.isInfoEnabled()) {
    	    log.info("Start filtering subgraphs of allowed roots: " + 
                allowedSubgraphRootIds);
    	    classCount = this.getOwlGraphWrapper().getAllOWLClasses().size();
        }

    	//first, we get all OWLObjects descendants and ancestors of the allowed roots, 
    	//to define which OWLObjects should be kept in the ontology. 
    	//We store ancestors and descendants in different collections to check 
    	//for undesired relations after class removals (see end of the method). 
        Set<OWLClass> allowedSubgraphRoots = new HashSet<OWLClass>();
    	Set<OWLClass> ancestors            = new HashSet<OWLClass>();
        Set<OWLClass> descendants          = new HashSet<OWLClass>();
    	for (String allowedRootId: allowedSubgraphRootIds) {
    		OWLClass allowedRoot = 
    				this.getOwlGraphWrapper().getOWLClassByIdentifierNoAltIds(allowedRootId);
    		
    		if (allowedRoot != null) {
    			allowedSubgraphRoots.add(allowedRoot);
    			if (log.isDebugEnabled()) {
    			    log.debug("Allowed root class: " + allowedRoot);
    			}
    			
    			//get all its descendants and ancestors
    			//fill the Collection toKeep and ancestorsIds
    			Set<OWLClass> descs = 
    					this.getOwlGraphWrapper().getOWLClassDescendantsWithGCI(allowedRoot);
    			if (log.isDebugEnabled()) {
    			    log.debug("Allowed descendant classes: " + descs);
    			}
    			descendants.addAll(descs);
    			Set<OWLClass> rootAncestors = 
    					this.getOwlGraphWrapper().getOWLClassAncestorsWithGCI(allowedRoot);
    			ancestors.addAll(rootAncestors);
    			if (log.isDebugEnabled()) {
				    log.debug("Allowed ancestor classes: " + rootAncestors);
    			}
    		} else {
    		    if (log.isDebugEnabled()) {
    			    log.debug("Discarded root class: " + allowedRootId);
    		    }
    		}
    	}
    	
    	//remove unwanted classes
        Set<OWLClass> toKeep = new HashSet<OWLClass>();
        toKeep.addAll(allowedSubgraphRoots);
        toKeep.addAll(ancestors);
        toKeep.addAll(descendants);
        if (log.isDebugEnabled()) {
            log.debug("Allowed classes: " + toKeep);
        }
        Set<String> classIdsRemoved = new HashSet<String>();
    	for (OWLClass classRemoved: this.filterClasses(toKeep)) {
    	    classIdsRemoved.add(this.getOwlGraphWrapper().getIdentifier(classRemoved));
    	}
    	if (log.isDebugEnabled()) {
    	    log.debug("Classes removed: " + classIdsRemoved);
    	}
    	
    	//remove relations between any ancestor of an allowed root, that is not also 
    	//the children of another allowed root, and descendants of those allowed roots, 
    	//as it would represent an undesired subgraph. 
    	Set<OWLGraphEdge> edgesToRemove = new HashSet<OWLGraphEdge>();
    	ancestor: for (OWLClass ancestor: ancestors) {
    		//if this ancestor is also an allowed root, or also a descendant of another 
    	    //allowed root, all relations to it are allowed
    		if (allowedSubgraphRoots.contains(ancestor) || 
    		        descendants.contains(ancestor)) {
    			continue ancestor;
    		}
    		
    		//get direct descendants of the ancestor
    	    for (OWLGraphEdge incomingEdge: 
    				    this.getOwlGraphWrapper().getIncomingEdgesWithGCI(ancestor)) {
    	        
    	        OWLObject directDescendant = incomingEdge.getSource(); 
    	        if (directDescendant instanceof OWLClass) { 
    	            //if the descendant is not an allowed root, nor an ancestor 
    	            //of an allowed root, then the relation should be removed.
    	            if (!allowedSubgraphRoots.contains(directDescendant) && 
    	                    !ancestors.contains(directDescendant)) {
    	                
    	                edgesToRemove.add(incomingEdge);
    	                if (log.isDebugEnabled()) {
    	                    log.debug("Undesired subgraph, relation between " + 
    	                            ancestor + " and " + directDescendant + " removed");
    	                }
    	            }
    	        } 
    	    }
    	}
    	int edgesRemoved = this.removeEdges(edgesToRemove);
    	
    	if (edgesRemoved != edgesToRemove.size()) {
    		throw new AssertionError("Incorrect number of relations removed, expected " + 
    				edgesToRemove.size() + ", but was " + edgesRemoved);
    	}
    	
    	if (log.isInfoEnabled()) {
    	    log.info("Done filtering subgraphs of allowed roots, " + classIdsRemoved.size() + 
    	            " classes removed over " + classCount + " classes total, " + 
    	            edgesRemoved + " undesired relations removed.");
    	}
    	
    	return classIdsRemoved;
    }
    
    /**
     * Delegates to {@link #removeSubgraphs(Collection, boolean, Collection)}, 
     * with the last {@code Collection} argument being {@code null}.
     * 
     * @param subgraphRootIds   See {@link #removeSubgraphs(Collection, boolean, Collection)}.
     * @param keepSharedClasses See {@link #removeSubgraphs(Collection, boolean, Collection)}.
     * @return                  {@link #removeSubgraphs(Collection, boolean, Collection)}.
     * @see #removeSubgraphs(Collection, boolean, Collection)
     */
    public Set<String> removeSubgraphs(Collection<String> subgraphRootIds, 
            boolean keepSharedClasses) {
        return this.removeSubgraphs(subgraphRootIds, keepSharedClasses, null);
    }
    /**
     * Remove from the ontology the subgraphs starting 
     * from the {@code OWLClass}es with their ID in {@code subgraphRootIds}. 
     * The subgraph starting from the {@code OWLClass}es with their ID 
     * in {@code allowedSubgraphRootIds} will not be removed, even if part of 
     * subgraph to remove. {@code subgraphRootIds} and {@code allowedSubgraphRootIds} 
     * contain the OBO-style IDs of {@code OWLClass}es. 
     * <p>
     * If a class is part of a subgraph to remove, but also of a subgraph not to be removed, 
     * it will be kept in the ontology if {@code keepSharedClasses} is {@code true}, 
     * and only classes that are solely part of the subgraphs to remove 
     * will be deleted. If {@code keepSharedClasses} is {@code false}, 
     * all classes part of a subgraph to remove will be removed.
     * <p>
     * This method returns the number of {@code OWLClass}es removed as a result.
     * <p>
     * This is the opposite method of {@code filterSubgraphs(Collection<String>)}.
     * 
     * @param subgraphRootIds 		    A {@code Collection} of {@code String}s 
     * 								    representing the OBO-style IDs of the {@code OWLClass}es 
     * 								    that are the roots of the subgraphs to be removed. 
     * @param keepSharedClasses 	    A {@code boolean} defining whether classes part both of 
     * 								    a subgraph to remove and a subgraph not to be removed,  
     * 								    should be deleted. If {@code true}, they will be kept, 
     * 								    otherwise, they will be deleted.
     * @param allowedSubgraphRootIds    A {@code Collection} of {@code String}s 
     *                                  representing the OBO-style IDs of the 
     *                                  {@code OWLClass}es that are the roots of the 
     *                                  subgraphs that are excluded from removal. 
     * @return 						A {@code Collection} of {@code String}s that are 
     *                              the OBO-like ID of the {@code OWLClass}es removed.
     * @see #filterSubgraphs(Collection)
     */
    public Set<String> removeSubgraphs(Collection<String> subgraphRootIds, 
            boolean keepSharedClasses, Collection<String> allowedSubgraphRootIds) {
        int classCount   = 0;
        if (log.isInfoEnabled()) {
    	    log.info("Start removing subgraphs of undesired roots: " + subgraphRootIds);
    	    classCount = this.getOwlGraphWrapper().getAllOWLClasses().size();
        }
    	
    	//roots of the ontology not in subgraphRoots and not ancestors of subgraphRoots 
    	//are considered as roots of subgraphs to keep, in case we want to keep shared classes.
    	//So, we store the roots of the ontology, then for each class 
    	//in subgraphRoots, we'll remove it and its ancestors from this list of valid root IDs.
    	Collection<OWLClass> ontRoots = new ArrayList<OWLClass>();
    	if (keepSharedClasses) {
    		ontRoots = this.getOwlGraphWrapper().getOntologyRoots();
    	}

        //subgraphs excluded from removal
    	Set<OWLClass> excudedFromRemoval = new HashSet<OWLClass>();
        if (allowedSubgraphRootIds != null) {
            for (String allowedSubgraphRootId: allowedSubgraphRootIds) {
                OWLClass allowedSubgraphRoot = 
                        this.getOwlGraphWrapper().getOWLClassByIdentifier(allowedSubgraphRootId);
                if (allowedSubgraphRoot == null) {
                    throw new IllegalArgumentException(allowedSubgraphRootId + " was requested " +
                    		"to be kept in the ontology, but it does not exist.");
                }
                excudedFromRemoval.add(allowedSubgraphRoot);
                excudedFromRemoval.addAll(
                        this.getOwlGraphWrapper().getOWLClassDescendantsWithGCI(allowedSubgraphRoot));
            }
            if (log.isDebugEnabled()) {
                log.debug("OWLClasses excluded from removal: " + excudedFromRemoval);
            }
        }
        
    	
    	Set<String> classIdsRemoved = new HashSet<String>();
    	rootLoop: for (String rootId: subgraphRootIds) {
    		OWLClass subgraphRoot = 
    				this.getOwlGraphWrapper().getOWLClassByIdentifier(rootId);
    		if (subgraphRoot == null) {
    		    if (log.isDebugEnabled()) {
    			    log.debug("Discarded root class, maybe already removed: " + rootId);
    		    }
    			continue rootLoop;
    		}
    		if (log.isDebugEnabled()) {
    		    log.debug("Examining subgraph from root: " + subgraphRoot);
    		}
    		
    		//we need all descendants of subgraphRoot, to determine the classes to remove
    		Set<OWLClass> classesToDel = new HashSet<OWLClass>();
            classesToDel.add(subgraphRoot);
            classesToDel.addAll(this.getOwlGraphWrapper().getOWLClassDescendantsWithGCI(subgraphRoot));
            //subgraphs excluded from removal
            classesToDel.removeAll(excudedFromRemoval);
        	
        	if (!keepSharedClasses) {
        		//this part is easy, simply remove all descendants of subgraphRoots 
        		if (log.isDebugEnabled()) {
        		    log.debug("Subgraph being deleted, descendants of subgraph " +
        		    		"root to remove: " + classesToDel);
        		}
                for (OWLClass classRemoved: this.removeClasses(classesToDel)) {
                    classIdsRemoved.add(this.getOwlGraphWrapper().getIdentifier(classRemoved));
                }
        		continue rootLoop;
        	}
    		
        	//This part is more tricky: 
        	//as we do not want to remove classes that are part of other subgraphs, 
        	//it is not just as simple as removing all descendants of 
        	//the roots of the subgraphs to remove. 
        	
        	//first, we need to remove each subgraph independently, 
        	//because of the following case: 
        	//if: D is_a C, C is_a B, B is_a A, and D is_a A;
        	//and we want to remove the subgraphs with the root B, and the root D;
        	//as we make sure to keep the ancestors of the roots of the subgraphs, 
        	//this would lead to fail to remove C (because it is an ancestor of D). 
        	//if we first remove subgraph B, C will be removed.
        	//if we first remove subgraph D, then subgraph B, C will also be removed.
        	//if we were trying to remove both subgraphs at the same time, we will identify 
        	//C as an ancestor of D and would not remove it. 
        	
        	//So: for each subgraph root, we identify its ancestors. 
        	//Then, for each of these ancestors, we identify its direct descendants, 
        	//that are not ancestors of the current root analyzed, 
        	//nor this root itself. 
        	//These descendants will be considered as roots of subgraphs to be kept. 
    	    Set<OWLClass> toKeep = new HashSet<OWLClass>();
    	    //First we need all the ancestors of this subgraph root
		    Set<OWLClass> ancestors = 
		    		this.getOwlGraphWrapper().getOWLClassAncestorsWithGCI(subgraphRoot);
    		
    		//also, each root of the ontology, not in subgraphRoots, and not in ancestors, 
    		//is considered a root of a subgraph to be kept, all its descendants should be kept
    		for (OWLClass ontRoot: ontRoots) {
				//valid ontology root, get all its descendants to be kept
    			if (!subgraphRoot.equals(ontRoot) && !ancestors.contains(ontRoot)) {
    				toKeep.add(ontRoot);
    				if (log.isDebugEnabled()) {
    				    log.debug("Allowed ontology root: " + ontRoot);
    				}
    				Set<OWLClass> descendants = 
    						this.getOwlGraphWrapper().getOWLClassDescendantsWithGCI(ontRoot);
            		toKeep.addAll(descendants);
            		if (log.isDebugEnabled()) {
            		    log.debug("Allowed classes of an allowed ontology root: " + 
    						descendants);
            		}
    			}
    		}
    	
    		//now, we try to identify the roots of the subgraphs not to be removed, 
    		//which are direct descendants of the ancestors we just identified
    		for (OWLClass ancestor: ancestors) {
    		    if (log.isDebugEnabled()) {
                  log.debug("Examining ancestor to identify roots of allowed subgraph: " + 
                		ancestor);
    		    }
                //ancestor of the root of the subgraph to remove are always allowed
                toKeep.add(ancestor);
                
    			//check direct descendants of the ancestor
    			for (OWLClass directDescendant: 
    				    this.getOwlGraphWrapper().getOWLClassDirectDescendantsWithGCI(ancestor)) {
    				if (!ancestors.contains(directDescendant) && 
							!subgraphRoot.equals(directDescendant)) {
    				    if (log.isDebugEnabled()) {
    					    log.debug("Descendant root of an allowed subgraph to keep: " + 
    							directDescendant);
    				    }
						//at this point, why not just calling filterSubgraphs 
						//on these allowed roots, could you ask.
						//Well, first, because we also need to keep the ancestors 
						//stored in ancestorIds, as some of them might not be ancestor 
						//of these allowed roots. Second, because we do not need to check 
						//for relations that would represent undesired subgraphs, 
						//as in filterSubgraphs (see end of that method).

						toKeep.add(directDescendant);
						Set<OWLClass> allowedDescendants = 
							this.getOwlGraphWrapper().getOWLClassDescendantsWithGCI(directDescendant);
						toKeep.addAll(allowedDescendants);
						if (log.isDebugEnabled()) {
						    log.debug("Allowed classes of an allowed subgraph: " + 
								allowedDescendants);
						}
					} else {
					    if (log.isDebugEnabled()) {
    					    log.debug("Descendant NOT root of an allowed subgraph to keep: " + 
    							directDescendant);
					    }
    				}
    			}
    		}

    		//remove shared classes from removal (subgraph excluded from removal 
    		//have already been removed from classesToDel)
    		classesToDel.removeAll(toKeep);
    		if (log.isDebugEnabled()) {
    		    log.debug("OWLClasses to keep: " + toKeep);
                log.debug("OWLClasses to remove: " + classesToDel);
    		}
    		for (OWLClass classRemoved: this.removeClasses(classesToDel)) {
                classIdsRemoved.add(this.getOwlGraphWrapper().getIdentifier(classRemoved));
            }
    	}
    	
    	if (log.isInfoEnabled()) {
    	    log.info("Done removing subgraphs of undesired roots, " + classIdsRemoved.size() + 
    	            " classes removed over " + classCount + " classes total.");
    	}
    	
    	return classIdsRemoved;
    }

	//*********************************
	//    RELATION FILTERING OR REMOVAL
	//*********************************
    
    /**
     * Filter the {@code OWLAxiom}s in the ontologies to keep only  
     * those that correspond to {@code OWLObjectProperty}s listed in {@code allowedRels}, 
     * as OBO-style IDs. {@code is_a} relations 
     * will not be removed, whatever the content of {@code allowedRels}. 
     * <p>
     * If {@code allowSubRels} is {@code true}, then the relations 
     * that are subproperties of the allowed relations are also kept 
     * (e.g., if RO:0002131 "overlaps" is allowed, and {@code allowSubRels} 
     * is {@code true}, then RO:0002151 "partially_overlaps" is also allowed). 
     * <p>
     * This method is similar to the {@link 
     * owltools.mooncat.Mooncat#retainAxiomsInPropertySubset(OWLOntology, Set) 
     * Mooncat#retainAxiomsInPropertySubset} method, the differences lie only in 
     * the options of these methods, and the way sub-properties are retrieved 
     * (via a reasonner in {@code Mooncat}, via {@code OWLGraphWrapper} in this method). 
     * Also, the {@code Mooncat} method try to replace a {@code OWLObjectProperty} 
     * to remove, by an allowed super-property, while here, users should use 
     * {@link #mapRelationsToParent(Collection)}.
     * <p>
     * <strong>Important:</strong> If it is planed to also call {@link #reduceRelations()}, 
     * and the ontology is very large, it is recommended to first call 
     * {@link #removeUnrelatedRelations(Collection)}, before calling {@link #reduceRelations()}.
     * 
     * @param allowedRels 		A {@code Collection} of {@code String}s 
     * 							representing the OBO-style IDs of the relations 
     * 							to keep in the ontology, e.g. "BFO:0000050". 
     * @param allowSubRels		A {@code boolean} defining whether sub-relations 
     * 							of the allowed relations should also be kept. 
     * @return          An {@code int} representing the number of {@code OWLSubClassOfAxiom}s 
     *                  removed as a result (but other axioms are removed as well). 
     * @see #removeRelations(Collection, boolean)
     * @throws IllegalArgumentException If an ID in {@code rels} did not allow to identify 
     *                                  an {@code OWLObjectProperty}.
     * @see #removeUnrelatedRelations(Collection)
     */
    public int filterRelations(Collection<String> allowedRels, boolean allowSubRels) {
        if (log.isInfoEnabled()) {
    	    log.info("Start filtering allowed relations " + allowedRels);
        }
    	
    	int relsRemoved = this.filterOrRemoveRelations(allowedRels, allowSubRels, true);

    	if (log.isInfoEnabled()) {
    	    log.info("Done filtering allowed relations, " + relsRemoved + 
    	            " relations removed.");
    	}
    	return relsRemoved;
    }

    
    /**
     * Remove from all {@code OWLOntologies} wrapped by this object 
     * the {@code OWLObjectProperty}s not related to the provided {@code allowedRels}.
     * <p> 
     * This method is useful to mirror the method 
     * {@link #filterRelations(Collection, boolean)}, to remove all completely unrelated relations 
     * before calling {@link #reduceRelations()}. This is because relation reduction 
     * can be slow for large ontologies, but we cannot simply call the method 
     * {@code filterRelations} before the reduction, as a composed relation 
     * could for instance lead to an allowed relation; so we do not want to remove 
     * the relations used for composition before relation reduction, but we do want 
     * to remove relations that cannot lead to an allowed relation. 
     * <p>
     * The {@code OWLOntology}s wrapped by this object will be modified as a result 
     * of the call to this method.
     * 
     * @param allowedRels       A {@code Collection} of {@code String}s 
     *                          representing the OBO-like IDs of the relations 
     *                          to keep in the ontology, e.g. "BFO:0000050". 
     * @see #reduceRelations()
     * @see #filterRelations(Collection, boolean)
     */
    public void removeUnrelatedRelations(Collection<String> allowedRels) {
        log.info("Removing relations unrelated to allowed relations...");
        if (allowedRels == null || allowedRels.isEmpty()) {
            log.info("Nothing to be done, exiting method.");
            return;
        }
        
        OWLGraphWrapper wrapper = this.getOwlGraphWrapper();
        Deque<OWLObjectPropertyExpression> walkProps = 
                new ArrayDeque<OWLObjectPropertyExpression>();
        
        //first, we retrieve the allowed rels
        for (String propId: allowedRels) {
            //get the OWLObjectProperty corresponding to the iterated ID
            OWLObjectProperty prop = wrapper.getOWLObjectPropertyByIdentifier(propId);
            //maybe the ID was not an OBO-like ID, but a String corresponding to an IRI
            if (prop == null) {
                prop = wrapper.getOWLObjectProperty(propId);
            }
            //could not find an property corresponding to the ID
            if (prop == null) {
                throw new IllegalArgumentException("The ID '" + propId + 
                        "' does not correspond to any OWLObjectProperty");
            }
            walkProps.addFirst(prop);
        }
        
        //now, we retrieve all associated properties, sub- and super-properties
        Set<OWLObjectPropertyExpression> propsToConsider = 
                new HashSet<OWLObjectPropertyExpression>();
        OWLObjectPropertyExpression iteratedProp;
        while ((iteratedProp = walkProps.pollFirst()) != null) {
            //avoid cycle of props
            if (propsToConsider.contains(iteratedProp)) {
                continue;
            }
            
            propsToConsider.add(iteratedProp);
            //we keep the super-properties, but we don't examine them further 
            //to get their sub-properties, etc. 
            propsToConsider.addAll(wrapper.getSuperPropertyClosureOf(iteratedProp));
            //we keep all the sub-properties, and will examine them to get their chained properties etc.
            walkProps.addAll(wrapper.getSubPropertyClosureOf(iteratedProp));
            
            //now, also consider equivalent relations and relations whose chain can generate 
            //an allowed rel.
            Set<OWLAxiom> propAxioms = new HashSet<OWLAxiom>();
            for (OWLOntology ont: wrapper.getAllOntologies()) {
                propAxioms.addAll(ont.getAxioms(iteratedProp, Imports.EXCLUDED));
                if (iteratedProp instanceof OWLEntity) {
                    //need the referencing axioms for the OWLSubPropertyChainOfAxiom, 
                    //weirdly it is not returned by ont.getAxioms
                    propAxioms.addAll(ont.getReferencingAxioms(((OWLEntity) iteratedProp)));
                }
            }
            for (OWLAxiom ax: propAxioms) {
                if (ax instanceof OWLSubPropertyChainOfAxiom) {
                    OWLSubPropertyChainOfAxiom chainAx = (OWLSubPropertyChainOfAxiom) ax;
                    if (chainAx.getSuperProperty().equals(iteratedProp)) {
                        for (OWLObjectPropertyExpression propExpr: chainAx.getPropertyChain()) {
                            walkProps.addLast(propExpr);
                        }
                    }
                } else if (ax instanceof OWLEquivalentObjectPropertiesAxiom) {
                    for (OWLObjectPropertyExpression propExpr: 
                        ((OWLEquivalentObjectPropertiesAxiom) ax).getPropertiesMinus(iteratedProp)) {

                        walkProps.addLast(propExpr);
                    }
                }
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("Allowed OWLObjectProperties: {}" + propsToConsider);
        }
        
        
        //now, remove unrelated props
        Set<OWLObjectProperty> propsRemoved = new HashSet<OWLObjectProperty>();
        for (OWLOntology ont: wrapper.getAllOntologies()) {

            OWLEntityRemover remover = new OWLEntityRemover(Collections.singleton(ont));
            
            for (OWLObjectProperty prop: ont.getObjectPropertiesInSignature()) {
                if (!propsToConsider.contains(prop)) {
                    propsRemoved.add(prop);
                    prop.accept(remover);
                } 
            }
            
            ont.getOWLOntologyManager().applyChanges(remover.getChanges());
            wrapper.clearCachedEdges();
        }
        
        if (log.isInfoEnabled()) {
            log.info("Done removing relations unrelated to allowed relations, " + 
                    propsRemoved.size() + " relations removed: " + propsRemoved);
        }
    }
    /**
     * Remove the {@code OWLAxiom}s in the ontologies  
     * corresponding to the {@code OWLObjectProperty}s listed in {@code forbiddenRels}, 
     * as OBO-style IDs. {@code is_a} relations 
     * will not be removed, whatever the content of {@code forbiddenRels}. 
     * <p>
     * If {@code forbidSubRels} is {@code true}, then the relations 
     * that are subproperties of the relations to remove are also removed 
     * (e.g., if RO:0002131 "overlaps" should be removed, and {@code forbidSubRels} 
     * is {@code true}, then RO:0002151 "partially_overlaps" is also removed). 
     * <p>
     * This method is the inverse of the {@link 
     * owltools.mooncat.Mooncat#retainAxiomsInPropertySubset(OWLOntology, Set) 
     * Mooncat#retainAxiomsInPropertySubset} method, the differences lie also in 
     * the options of these methods, and the way sub-properties are retrieved 
     * (via a reasonner in {@code Mooncat}, via {@code OWLGraphWrapper} in this method).
     * Also, the {@code Mooncat} method try to replace a {@code OWLObjectProperty} 
     * to remove, by an allowed super-property, while here, users should use 
     * {@link #mapRelationsToParent(Collection)}.
     * <p>
     * <strong>Important:</strong> If it is planed to also call {@link #reduceRelations()}, 
     * this method must be called first, otherwise, already removed relations could reappear 
     * due to property chain rules.
     * 
     * @param forbiddenRels 	A {@code Collection} of {@code String}s 
     * 							representing the OBO-style IDs of the relations 
     * 							to remove from the ontology, e.g. "BFO:0000050". 
     * @param forbidSubRels		A {@code boolean} defining whether sub-relations 
     * 							of the relations to remove should also be removed. 
     * @return          An {@code int} representing the number of {@code OWLSubClassOfAxiom}s 
     *                  removed as a result (but other axioms are removed as well). 
     * @see #filterRelations(Collection, boolean)
     * @throws IllegalArgumentException If an ID in {@code rels} did not allow to identify 
     *                                  an {@code OWLObjectProperty}.
     */
    public int removeRelations(Collection<String> forbiddenRels, boolean forbidSubRels)
    {
        if (log.isInfoEnabled()) {
    	    log.info("Start removing relations " + forbiddenRels);
        }
    	
    	int relsRemoved = this.filterOrRemoveRelations(forbiddenRels, forbidSubRels, false);

    	if (log.isInfoEnabled()) {
    	    log.info("Done removing relations, " + relsRemoved + " relations removed.");
    	}
    	return relsRemoved;
    }
    /**
     * Filter the {@code OWLAxiom}s in the ontologies to keep or remove 
     * (depending on the {@code filter} parameter)  
     * those that correspond to {@code OWLObjectProperty}s listed in {@code rels}, 
     * as OBO-style IDs. 
     * <p>
     * If {@code filter} is {@code true}, then the relations listed 
     * in {@code rels} should be kept, and all others removed. 
     * If {@code filter} is {@code false}, relations in {@code rels} 
     * should be removed, and all others conserved. The methods 
     * {@link #filterRelations(Collection, boolean)} and 
     * {@link #removeRelations(Collection, boolean)} delegate to this one.
     * <p>
     * If {@code subRels} is {@code true}, then the relations 
     * that are subproperties of the relations in {@code rels} are also kept or removed, 
     * depending on the {@code filter} parameter
     * (e.g., if {@code filter} is {@code true}, and if {@code rels} 
     * contains the RO:0002131 "overlaps" relation, 
     * and if {@code subRels} is {@code true}, 
     * then the relation RO:0002151 "partially_overlaps" will also be kept in the ontology). 
     * <p>
     * This method is similar to {@link 
     * owltools.mooncat.Mooncat#retainAxiomsInPropertySubset(OWLOntology, Set) 
     * Mooncat#retainAxiomsInPropertySubset} method, the differences lie in 
     * the options of these methods, and the way sub-properties are retrieved 
     * (via a reasonner in {@code Mooncat}, via {@code OWLGraphWrapper} in this method).
     * Also, the {@code Mooncat} method try to replace a {@code OWLObjectProperty} 
     * to remove, by an allowed super-property, while here, users should use 
     * {@link #mapRelationsToParent(Collection)}.
     * 
     * @param rels		A {@code Collection} of {@code String}s 
     * 					representing the OBO-style IDs of the relations 
     * 					to keep or to remove (depending on 
     * 					the {@code filter} parameter), e.g. "BFO:0000050". 
     * @param subRels	A {@code boolean} defining whether sub-relations 
     * 					of the relations listed in {@code rels} should also 
     * 					be examined for removal or conservation. 
     * @param filter	A {@code boolean} defining whether relations listed 
     * 					in {@code rels} (and their sub-relations if {@code subRels} 
     * 					is {@code true}) should be kept, or removed. 
     * 					If {@code true}, they will be kept, otherwise they will be removed.
     * @return 			An {@code int} representing the number of {@code OWLSubClassOfAxiom}s 
     * 					removed as a result (but other axioms are removed as well). 
     * 
     * @see #filterRelations(Collection, boolean)
     * @see #removeRelations(Collection, boolean)
     * @throws IllegalArgumentException If an ID in {@code rels} did not allow to identify 
     *                                  an {@code OWLObjectProperty}.
     */
    private int filterOrRemoveRelations(Collection<String> rels, boolean subRels, 
    		boolean filter) throws IllegalArgumentException {

    	//*** Obtain the OWLObjectProperties to consider ***
    	Set<OWLObjectPropertyExpression> propsToConsider = 
    	        new HashSet<OWLObjectPropertyExpression>();
    	for (String propId: rels) {
    	    //get the OWLObjectProperty corresponding to the iterated ID
    	    OWLObjectProperty prop = 
    	            this.getOwlGraphWrapper().getOWLObjectPropertyByIdentifier(propId);
    	    //maybe the ID was not an OBO-like ID, but a String corresponding to an IRI
    	    if (prop == null) {
    	        prop = this.getOwlGraphWrapper().getOWLObjectProperty(propId);
    	    }
    	    //could not find an property corresponding to the ID
    	    if (prop == null) {
    	        throw new IllegalArgumentException("The ID '" + propId + 
    	                "' does not correspond to any OWLObjectProperty");
    	    }
    	    
    	    propsToConsider.add(prop);
    	    if (subRels) {
    	        propsToConsider.addAll(this.getOwlGraphWrapper().getSubPropertyClosureOf(prop));
    	    }
    	}
    	if (log.isDebugEnabled()) {
    	    if (propsToConsider.isEmpty()) {
    	        log.debug("Filter or remove any axioms containing an OWLObjectProperty");
    	    } else {
    	        log.debug("OWLObjectProperties considered: " + propsToConsider);
    	    }
    	}
    	
    	//*** now, identify the axioms to remove ***
    	int classAxiomsRmCount = 0;
    	for (OWLOntology ont: this.getOwlGraphWrapper().getAllOntologies()) {
            for (OWLAxiom ax : ont.getAxioms()) {
                Set<OWLObjectProperty> ps = ax.getObjectPropertiesInSignature();
                boolean hasChanged = ps.removeAll(propsToConsider);
                //if we wanted to keep only the properties to consider, 
                //but there are others properties in this axiom: remove the axiom.
                //if we wanted to remove the properties to consider, 
                //and we find some in this axiom: remove the axiom
                if ((filter && ps.size() > 0) ||
                        (!filter && hasChanged)) { 

                    if (log.isDebugEnabled()) {
                        log.debug("Axioms to remove (is a OWLSubClassOfAxiom: " + 
                            (ax instanceof OWLSubClassOfAxiom) + "): " + ax);
                    }
                    ChangeApplied changes = ont.getOWLOntologyManager().removeAxiom(ont, ax);
                    if (log.isEnabledFor(Level.WARN) && changes != ChangeApplied.SUCCESSFULLY) {
                        log.warn("The axiom " + ax + " was not removed");
                    }
                    //count the number of OWLClassAxioms actually removed
                    if (ax instanceof OWLSubClassOfAxiom && changes == ChangeApplied.SUCCESSFULLY) {
                        classAxiomsRmCount++;
                    }
                }
            }
    	}
    	
    	return classAxiomsRmCount;
    }
    
	//*********************************
	//    REMOVE RELS TO SUBSETS IF NON ORPHAN
	//*********************************
    
    /**
     * Delegates method call to {@link #removeRelsToSubsets(Collection, Collection)}, 
     * with no {@code OWLClass}es excluded specified. 
     * 
     * @param subsets   See same name argument in 
     *                  {@link #removeRelsToSubsets(Collection, Collection)}.
     * @return          See value returned by 
     *                  {@link #removeRelsToSubsets(Collection, Collection)}.
     * @see #removeRelsToSubsets(Collection, Collection)
     */
    public int removeRelsToSubsets(Collection<String> subsets) {
        return this.removeRelsToSubsets(subsets, null);
    }
    
    /**
	 * Remove is_a and part_of incoming edges to {@code OWLClass}es 
	 * in {@code subsets}, only if the source of the incoming edge 
	 * will not be left orphan of other is_a/part_of relations to {@code OWLClass}es 
	 * not in {@code subsets}. {@code OWLClass}es with their OBO-like ID or IRI 
	 * in {@code classIdsExcluded} will be excluded from this mechanism, even if 
	 * they are part of a targeted subset (meaning, none of their incoming edges will be 
	 * removed)
	 * <p>
	 * <strong>Warning:</strong> please note that the resulting ontology will not be 
	 * semantically correct. It is the same kind of modifications made by 
	 * {@link #reducePartOfIsARelations()}, considering is_a (SubClassOf) 
	 * and part_of relations (or sub-relations, for instance, "in_deep_part_of") equivalent, 
	 * and that result in a simplified graph structure for display, but an incorrect ontology.
	 * <p>
	 * For instance: 
	 * <ul>
	 * <li>If A part_of B and A is_a C, and if C belongs to a targeted subset, 
	 * then relation A is_a C will be removed, as A will still have a part_of relation 
	 * to a class not in a targeted subset. 
	 * <li>If A is_a C, and if C belongs to a targeted subset, the relation will not be removed, 
	 * as A would not have any other is_a/part_of relation.
	 * <li>If A part_of B and A is_a C, and both B and C belong to a targeted subset, 
	 * then no relation will be removed, as A would have no is_a/part_of relation 
	 * to a class not in a targeted subset. 
	 * </ul>
	 * @param subsets 	A {@code Collection} of {@code String}s representing 
	 * 					the names of the targeted subsets, for which 
	 * 					member {@code OWLClass}es should have their is_a/part_of 
	 * 					incoming edges removed.
	 * @param classIdsExcluded A {@code Collection} of {@code String}s that are the OBO-like 
	 *                         IDs or IRIs of {@code OWLClass}es, whose their incoming edges
	 *                         should not be removed.  
	 * @return			An {@code int} that is the number of is_a/part_of 
	 * 					relations (or sub-relations) removed.
	 */
	public int removeRelsToSubsets(Collection<String> subsets, 
	        Collection<String> classIdsExcluded) {
	    if (log.isInfoEnabled()) {
		    log.info("Start removing is_a/part_of relations to subsets if non orphan: " +
				subsets + " - Classes excluded: " + classIdsExcluded);
	    }
		
		//update cache so that we make sure the last AssertionError at the end of the method 
		//will not be thrown by mistake
		this.getOwlGraphWrapper().clearCachedEdges();
		
		//first, get all classes in subsets
		Set<OWLClass> classesInSubsets = new HashSet<OWLClass>();
		for (String subsetId: subsets) {
			classesInSubsets.addAll(
					this.getOwlGraphWrapper().getOWLClassesInSubset(subsetId));
		}
		Set<OWLClass> classesExcluded = new HashSet<OWLClass>();
		if (classIdsExcluded != null) {
		    for (String classId: classIdsExcluded) {
		        OWLClass cls = this.getOwlGraphWrapper().getOWLClassByIdentifier(classId);
		        if (cls == null) {
		            cls = this.getOwlGraphWrapper().getOWLClass(classId);
		        }
		        if (cls != null) {
		            classesExcluded.add(cls);
		        }
		    }
		}
		classesInSubsets.removeAll(classesExcluded);
		
		//now check each source of the incoming edges to the classes in the subsets
		Set<OWLGraphEdge> edgesToRemove = new HashSet<OWLGraphEdge>();
		//to make sure incoming edges' sources are examined only once
		Set<OWLObject> sourcesExamined = new HashSet<OWLObject>();
		for (OWLClass subsetClass: classesInSubsets) {
			
			for (OWLGraphEdge incomingEdge: 
					    this.getOwlGraphWrapper().getIncomingEdgesWithGCI(subsetClass)) {
			    
			    //if this is not a is_a nor a part_of-like relation, skip
			    if (!this.isASubClassOfEdge(incomingEdge) && 
			            !this.isAPartOfEdge(incomingEdge)) {
			        continue;
			    }
			    
			    OWLObject sourceObject = incomingEdge.getSource();
			    if (sourcesExamined.contains(sourceObject)) {
			        continue;
			    }
			    if (sourceObject instanceof OWLClass) {
			        //do nothing if the source class is itself in subsets
			        if (this.getOwlGraphWrapper().isOWLObjectInSubsets(
			                sourceObject, subsets)) {
			            continue;
			        }
			        
			        //now distinguish is_a/part_of outgoing edges of the source class 
			        //going to classes in subsets and to classes not in subsets
			        Set<OWLGraphEdge> edgesToSubset    = new HashSet<OWLGraphEdge>();
			        Set<OWLGraphEdge> edgesNotToSubset = new HashSet<OWLGraphEdge>();
			        for (OWLGraphEdge outgoingEdge: 
			            this.getOwlGraphWrapper().getOutgoingEdgesWithGCI(sourceObject)) {
			            
			            //if this is not a is_a or part_of-like relation, skip it
			            if (!this.isASubClassOfEdge(outgoingEdge) && 
			                    !this.isAPartOfEdge(outgoingEdge)) {
			                continue;
			            }
			            OWLObject targetObject = outgoingEdge.getTarget();
			            if (targetObject instanceof OWLClass) {
			                if (this.getOwlGraphWrapper().isOWLObjectInSubsets(
			                        targetObject, subsets)) {
			                    edgesToSubset.add(outgoingEdge);
			                } else if (!outgoingEdge.isGCI()) {
			                    //consider this relation only if it is not a GCI, 
			                    // we want to consider generic relations at least
			                    edgesNotToSubset.add(outgoingEdge);
			                }
			            }
			        }
			        
			        //now, check if the source class has is_a/part_of outgoing edges to targets 
			        //not in subsets, and if it is the case, 
			        //remove all its is_a/part_of outgoing edges to targets in subsets
			        if (!edgesNotToSubset.isEmpty()) {
			            if (log.isDebugEnabled()) {
			                log.debug("Relations to remove: " + edgesToSubset);
			            }
			            edgesToRemove.addAll(edgesToSubset);
			        } 
			    } 
			    sourcesExamined.add(sourceObject);
			}
		}
		
		int edgesRemovedCount = this.removeEdges(edgesToRemove);
		//check that everything went fine
		if (edgesRemovedCount != edgesToRemove.size()) {
			throw new AssertionError("Incorrect count of edges removed, expected " + 
					edgesToRemove.size() + " but was " + edgesRemovedCount);
		}
		
		if (log.isInfoEnabled()) {
		    log.info("Done removing is_a/part_of relations to subset if non orphan, " + 
		        edgesRemovedCount + " relations removed");
		}
		return edgesRemovedCount;
	}

	//*********************************
	//    UTILS
	//*********************************
	/**
	 * Remove direct edges between the {@code OWLObject}s with the OBO-like IDs 
	 * {@code sourceId} and {@code targetId}.
	 * 
	 * @param sourceId A {@code String} that is the OBO-like ID of the {@code OWLObject} 
	 *                 whose edges to remove outgoing from.
	 * @param targetId A {@code String} that is the OBO-like ID of the {@code OWLObject} 
     *                 whose edges to remove incoming to.
	 * @return         An {@code int} that is the number of {@code OWLGraphEdge}s 
	 *                 removed as a result.
	 */
	public int removeDirectEdgesBetween(String sourceId, String targetId) {
	    OWLObject source = this.getOwlGraphWrapper().getOWLObjectByIdentifier(sourceId);
        if (source == null) {
            throw new IllegalArgumentException(sourceId + " was not found in the ontology");
        }
        OWLObject target = this.getOwlGraphWrapper().getOWLObjectByIdentifier(targetId);
        if (target == null) {
            throw new IllegalArgumentException(targetId + " was not found in the ontology");
        }
        
        Set<OWLGraphEdge> edgesToRemove = new HashSet<OWLGraphEdge>();
        for (OWLGraphEdge edge: this.getOwlGraphWrapper().getOutgoingEdgesWithGCI(source)) {
            if (edge.getTarget().equals(target)) {
                edgesToRemove.add(edge);
            }
        }
        int edgesRemoved = this.removeEdges(edgesToRemove);
        
        if (log.isInfoEnabled()) {
            log.info("Edges between " + sourceId + " and " + targetId + " removed, " + 
                    edgesRemoved + " removed.");
        }
        
        return edgesRemoved;
	}
	/**
	 * Remove {@code edge} from its ontology. It means that the {@code OWLAxiom}s 
	 * returned by the method {@code OWLGraphEdge#getAxioms()}, that allowed to generate 
	 * {@code edge}, will be removed from the ontology. 
	 * 
	 * @param edge 	   The {@code OWLGraphEdge} to be removed from the ontology. 
	 * @return         {@code true} if all underlying {@code OWLAxiom}s of {@code edge} 
	 *                 were actually present in its ontology, and removed. 
	 * @see #removeEdges(Collection)
	 */
	public boolean removeEdge(OWLGraphEdge edge) {
	    ChangeApplied status = edge.getOntology().getOWLOntologyManager().removeAxioms(
	            edge.getOntology(), edge.getAxioms());
	    this.triggerWrapperUpdate();
	    return (status == ChangeApplied.SUCCESSFULLY);
	}
	/**
	 * Remove {@code edges} from their related ontology. It means that the {@code OWLAxiom}s 
     * returned by the method {@code OWLGraphEdge#getAxioms()}, that allowed to generate 
     * the {@code OWLGraphEdge}s, will be removed from the ontology. 
	 * 
	 * @param edges 	A {@code Collection} of {@code OWLGraphEdge}s 
	 * 					to be removed from their ontology. 
	 * @return 			An {@code int} representing the number of {@code OWLGraphEdge}s 
	 * 					that were actually removed 
	 * @see #removeEdge(OWLGraphEdge)
	 */
	public int removeEdges(Collection<OWLGraphEdge> edges) {
		int edgeCount = 0;
		for (OWLGraphEdge edge: edges) {
			if (this.removeEdge(edge)) {
				edgeCount++;
			} 
		}
		return edgeCount;
	}
	/**
	 * Add {@code edge} to its related ontology. 
	 * This method transforms the {@code OWLGraphEdge} {@code edge} 
	 * into an {@code OWLSubClassOfAxiom}, then add it to the ontology   
     * and update the {@code OWLGraphWrapper} container. 
	 * 
	 * @param edge 		The {@code OWLGraphEdge} to be added to its related ontology. 
	 * @return 			{@code true} if {@code edge} was actually added 
	 * 					to the ontology. 
	 */
	public boolean addEdge(OWLGraphEdge edge) {
	    //this.getAxiom was used here in former versions
	    OWLSubClassOfAxiom newAxiom = edge.getOntology().getOWLOntologyManager().
        getOWLDataFactory().getOWLSubClassOfAxiom(
            (OWLClassExpression) this.getOwlGraphWrapper().edgeToSourceExpression(edge), 
            (OWLClassExpression) this.getOwlGraphWrapper().edgeToTargetExpression(edge));
	    
	    ChangeApplied status = edge.getOntology().getOWLOntologyManager().addAxiom(
	            edge.getOntology(), newAxiom);
	    this.triggerWrapperUpdate();
	    return (status == ChangeApplied.SUCCESSFULLY);
	}
	/**
	 * Add {@code edges} to their related ontology. 
	 * This method transforms the {@code OWLGraphEdge}s in {@code edge}s 
	 * into {@code OWLSubClassOfAxiom}s, then add them to the ontology,   
     * and update the {@code OWLGraphWrapper} container. 
	 * 
	 * @param edges		A {@code Set} of {@code OWLGraphEdge}s 
	 * 					to be added to their ontology. 
	 * @return 			An {@code int} representing the number of {@code OWLGraphEdge}s 
	 * 					that were actually added 
	 * @see #addEdge(OWLGraphEdge)
	 */
	public int addEdges(Set<OWLGraphEdge> edges) {
		int edgeCount = 0;
		for (OWLGraphEdge edge: edges) {
			if (this.addEdge(edge)) {
				edgeCount++;
			} 
		}
		return edgeCount;
	}
	/**
     * Remove from all ontologies the {@code OWLClass} {@code classToDel},   
     * and then update the {@code OWLGraphWrapper} container. 
     * 
     * @param classToDel	 	an {@code OWLClass} to be removed 
     * 							from the ontologies. 
	 * @return 					{@code true} if {@code classToDel} was actually 
	 * 							removed from the ontology. 
     */
    public boolean removeClass(OWLClass classToDel) {
        OWLEntityRemover remover = new OWLEntityRemover(
                this.getOwlGraphWrapper().getAllOntologies());
        classToDel.accept(remover);
        if (this.applyChanges(remover.getChanges())) {
            if (log.isDebugEnabled()) {
                log.debug("Removing OWLClass " + classToDel);
            }
            this.triggerWrapperUpdate();
            return true;
        } 
        if (log.isDebugEnabled()) {
            log.debug("Fail removing OWLClass " + classToDel);
        }
        return false;
    }
	/**
     * Remove from all ontologies all {@code OWLClass}es 
     * present in {@code classesToDel},   
     * and then update the {@code OWLGraphWrapper} container. 
     * 
     * @param classesToDel	 	a {@code Set} of {@code OWLClass}es 
     * 							to be removed from the ontologies. 
     * @return					A {@code Set} of {@code OWLClass}es representing the classes 
     * 							actually removed as a result. 
     */
    public Set<OWLClass> removeClasses(Set<OWLClass> classesToDel) {
    	Set<OWLClass> classesRemoved = new HashSet<OWLClass>();
    	for (OWLClass classToDel: classesToDel) {
		    if (this.removeClass(classToDel)) {
		        classesRemoved.add(classToDel);
		    } 
    	}
    	return classesRemoved;
    }
    /**
     * Filter from the ontologies all {@code OWLClass}es 
     * present in {@code classesToKeep},  
     * and then update the {@code OWLGraphWrapper} container. 
     * 
     * @param classesToKeep 	a {@code Set} of {@code OWLClass}s 
     * 							that are classes to be kept in the ontology. 
     * @return                  A {@code Set} of {@code OWLClass}es representing the classes 
     *                          actually removed as a result. 
     */
    public Set<OWLClass> filterClasses(Set<OWLClass> classesToKeep) {
    	//now remove all classes not included in classesToKeep
        Set<OWLClass> classesRemoved = new HashSet<OWLClass>();
    	for (OWLOntology o : this.getOwlGraphWrapper().getAllOntologies()) {
    		for (OWLClass iterateClass: o.getClassesInSignature()) {
    		    log.info(iterateClass);
			    if (!classesToKeep.contains(iterateClass) && 
			            this.removeClass(iterateClass)) {
	                classesRemoved.add(iterateClass);
			    }
    		}
    	}
    	return classesRemoved;
    }
    
    /**
     * Determine if {@code edge} represents an is_a relation.
     * 
     * @param edge	The {@code OWLGraphEdge} to test.
     * @return		{@code true} if {@code edge} is an is_a (SubClassOf) relation.
     */
    //TODO: move this to OWLGraphWrapperEdgesExtended
    public boolean isASubClassOfEdge(OWLGraphEdge edge) {
    	return (edge.getSingleQuantifiedProperty().getProperty() == null && 
				edge.getSingleQuantifiedProperty().getQuantifier().equals(
				        Quantifier.SUBCLASS_OF));
    }
    
    /**
     * Determine if {@code edge} represents a part_of relation or one of its sub-relations 
     * (e.g., "deep_part_of").
     * 
     * @param edge	The {@code OWLGraphEdge} to test.
     * @return		{@code true} if {@code edge} is a part_of relation, 
     * 				or one of its sub-relations.
     */
    //TODO: move this to OWLGraphWrapperEdgesExtended
    public boolean isAPartOfEdge(OWLGraphEdge edge) {
    	if (this.partOfRels == null) {
    		this.partOfRels = this.getOwlGraphWrapper().getSubPropertyReflexiveClosureOf(
        			this.getOwlGraphWrapper().getOWLObjectPropertyByIdentifier(PARTOFID));
    	}
    	return (partOfRels.contains(edge.getSingleQuantifiedProperty().getProperty()) && 
    	        edge.getSingleQuantifiedProperty().getQuantifier().equals(
    	                Quantifier.SOME));
    }
 
   
//    /**
//	 * Convenient method to get a {@code OWLSubClassOfAxiom} corresponding to 
//	 * the provided {@code OWLGraphEdge}.
//	 * 
//	 * @param edge 			An {@code OWLGraphEdge} to transform 
//	 * 								into a {@code OWLSubClassOfAxiom}
//	 * @return OWLSubClassOfAxiom 	The {@code OWLSubClassOfAxiom} corresponding 
//	 * 								to {@code OWLGraphEdge}.
//	 */
//	private OWLSubClassOfAxiom getAxiom(OWLGraphEdge edge) {
//		
//		return edge.getOntology().getOWLOntologyManager().
//		    getOWLDataFactory().getOWLSubClassOfAxiom(
//		        (OWLClassExpression) edge.getSource(), 
//				(OWLClassExpression) this.getOwlGraphWrapper().edgeToTargetExpression(edge));
//	}
	
    /**
     * Convenient method to apply {@code changes} to the ontology.
     * 
     * @param changes 	The {@code List} of {@code OWLOntologyChange}s 
     * 					to be applied to the ontology. 
     * @return 			{@code true} if all changes were applied, 
     * 					{@code false} otherwise. 
     */
    private boolean applyChanges(List<? extends OWLOntologyChange> changes) {
    	
    	ChangeApplied status = this.getOwlGraphWrapper().getManager().applyChanges(changes);
    	if (status == ChangeApplied.SUCCESSFULLY) {
        	return true;
    	}
    	return false;
    }
    /**
     * Convenient method to trigger an update of the {@code OWLGraphWrapper} 
     * on which modifications are performed.
     */
    private void triggerWrapperUpdate() {
    	this.getOwlGraphWrapper().clearCachedEdges();
        //this.getOwlGraphWrapper().cacheEdges();
    }
    
    
	//*********************************
	//    GETTERS/SETTERS
	//*********************************
	/**
	 * Get the {@code OWLGraphWrapper} on which modifications 
	 * are performed.
	 * 
	 * @return the  {@code OWLGraphWrapper} wrapped by this class.
	 */
	public OWLGraphWrapper getOwlGraphWrapper() {
		return this.owlGraphWrapper;
	}
	/**
	 * @param owlGraphWrapper the {@code OWLGraphWrapper} that this class manipulates.
	 * @see #owlGraphWrapper
	 */
	private void setOwlGraphWrapper(OWLGraphWrapper owlGraphWrapper) {
		this.owlGraphWrapper = owlGraphWrapper;
	}
}
