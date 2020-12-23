package owltools.geneontologyowlapi5;

import com.github.jsonldjava.core.Context;
import org.geneontology.obographs.io.PrefixHelper;
import org.geneontology.obographs.model.*;
import org.geneontology.obographs.model.Node.Builder;
import org.geneontology.obographs.model.Node.RDFTYPES;
import org.geneontology.obographs.model.axiom.*;
import org.geneontology.obographs.model.meta.BasicPropertyValue;
import org.geneontology.obographs.model.meta.DefinitionPropertyValue;
import org.geneontology.obographs.model.meta.SynonymPropertyValue;
import org.geneontology.obographs.model.meta.SynonymPropertyValue.SCOPES;
import org.geneontology.obographs.model.meta.XrefPropertyValue;
import org.geneontology.obographs.owlapi.SynonymVocabulary;
import org.semanticweb.owlapi.model.*;

import java.util.*;
import java.util.stream.Collectors;


/**
 * Implements OWL to OG translation
 * ===
 * 
 * See <a href="https://github.com/geneontology/obographs/blob/master/README-owlmapping.md">OWL Mapping spec</a>
 * 
 * @see "[SPEC](https://github.com/geneontology/obographs/blob/master/README-owlmapping.md)"
 * 
 * 
 * @author cjm
 *
 */
public class FromOwl {

    public static final String SUBCLASS_OF = "is_a";
    public static final String SUBPROPERTY_OF = "subPropertyOf";
    public static final String INVERSE_OF = "inverseOf";

    private PrefixHelper prefixHelper;
    private Context context;

    class OBOClassDef {
        List<String> genusClassIds = new ArrayList<>();
        List<ExistentialRestrictionExpression> restrs = new ArrayList<>();
    }

    /**
     * 
     */
    public FromOwl() {
        prefixHelper = new PrefixHelper();
        context = prefixHelper.getContext();
    }

    /**
     * @param baseOntology
     * @return GraphDocument where each graph is an ontology in the ontology closure
     * @see <a href="https://github.com/geneontology/obographs/blob/master/README-owlmapping.md">OWL Mapping spec</a>
     */
    public GraphDocument generateGraphDocument(OWLOntology baseOntology) {
        List<Graph> graphs = new ArrayList<>();
        for (OWLOntology ont : baseOntology.getImportsClosure()) {
            graphs.add( generateGraph(ont));
        }
        return new GraphDocument.Builder().graphs(graphs).build();
    }

    /**
     * @param ontology
     * @return Graph generated from ontology
     */
    public Graph generateGraph(OWLOntology ontology) {

        SynonymVocabulary synonymVocabulary = new SynonymVocabulary();

        List<Edge> edges = new ArrayList<>();
        List<Node> nodes = new ArrayList<>();
        List<EquivalentNodesSet> ensets = new ArrayList<>();
        List<LogicalDefinitionAxiom> ldas = new ArrayList<>();
        Set<String> nodeIds = new LinkedHashSet<>();
        Map<String, RDFTYPES> nodeTypeMap = new LinkedHashMap<>();
        Map<String, String> nodeLabelMap = new LinkedHashMap<>();
        Map<String, DomainRangeAxiom.Builder> domainRangeBuilderMap = new LinkedHashMap<>();
        List<PropertyChainAxiom> pcas = new ArrayList<>();

        // Each node can be built from multiple axioms; use a builder for each nodeId
        Map<String, Meta.Builder> nodeMetaBuilderMap = new LinkedHashMap<>();

        Set<OWLAxiom> untranslatedAxioms = new LinkedHashSet<>();

        List<OWLAxiom> sortedAxioms = ontology.getAxioms().parallelStream().sorted().collect(Collectors.toList());
        // iterate over all axioms and push to relevant builders
        for (OWLAxiom ax : sortedAxioms) {

            Meta meta = getAnnotations(ax);

            if (ax instanceof OWLDeclarationAxiom) {
                OWLDeclarationAxiom dax = ((OWLDeclarationAxiom)ax);
                OWLEntity e = dax.getEntity();
                if (e instanceof OWLClass) {
                    setNodeType(getClassId((OWLClass) e), RDFTYPES.CLASS, nodeTypeMap);
                }
                else if (e instanceof OWLProperty) {
                    setNodeType(getPropertyId((OWLProperty) e), RDFTYPES.PROPERTY, nodeTypeMap);
                }
                else if (e instanceof OWLNamedIndividual) {
                    setNodeType(getIndividualId((OWLNamedIndividual) e), RDFTYPES.INDIVIDUAL, nodeTypeMap);
                }
            }
            else if (ax instanceof OWLLogicalAxiom) {
                // LOGICAL AXIOMS

                if (ax instanceof OWLSubClassOfAxiom) {
                    // SUBCLASS

                    OWLSubClassOfAxiom sca = (OWLSubClassOfAxiom)ax;
                    OWLClassExpression subc = sca.getSubClass();
                    OWLClassExpression supc = sca.getSuperClass();

                    String subj = null;
                    if (subc.isAnonymous()) {
                        // GCI

                        /*
                         * TBD: Model for GCIs. See https://github.com/obophenotype/uberon/wiki/Evolutionary-variability-GCIs
                        if (subc instanceof OWLObjectIntersectionOf) {
                            OBOClassDef cdef = getClassDef(((OWLObjectIntersectionOf)subc).getOperands());
                            if (cdef.genusClassIds.size() == 1) {
                                subj = cdef.genusClassIds.get(0);
                            }

                            if (subc instanceof OWLObjectSomeValuesFrom) {
                                // Some axioms such as phylogenetic GCIs are encoded this way
                                ExistentialRestrictionExpression r = getRestriction(subc);
                                subj = r.getFillerId();
                            }
                        }
                         */

                    }
                    else {
                        // SUBJECT IS NAMED CLASS (non-GCI)
                        subj = getClassId((OWLClass) subc);
                    }

                    if (subj != null) {

                        setNodeType(subj, RDFTYPES.CLASS, nodeTypeMap);

                        if (supc.isAnonymous()) {
                            if (supc instanceof OWLObjectSomeValuesFrom) {
                                ExistentialRestrictionExpression r = getRestriction(supc);
                                if (r == null) {
                                    untranslatedAxioms.add(sca);
                                }
                                else {
                                    Edge e = getEdge(subj, r.getPropertyId(), r.getFillerId());
                                    edges.add(e);
                                }
                            }
                            else if (supc instanceof OWLObjectAllValuesFrom) {
                                OWLObjectAllValuesFrom avf = (OWLObjectAllValuesFrom)supc;
                                DomainRangeAxiom.Builder b = getDRBuilder(avf.getProperty(), domainRangeBuilderMap);
                                if (avf.getFiller().isAnonymous()) {

                                }
                                else {
                                    Edge e = getEdge(subj, 
                                            b.predicateId(), 
                                            getClassId(avf.getFiller().asOWLClass()));
                                    b.addAllValuesFrom(e);
                                }
                            }
                            else {

                            }
                        }
                        else {
                            edges.add(getEdge(subj, SUBCLASS_OF, getClassId((OWLClass) supc)));

                        }
                    }
                    else {
                        untranslatedAxioms.add(sca);
                    }
                }
                else if (ax instanceof OWLClassAssertionAxiom) {
                    OWLClassAssertionAxiom ca = (OWLClassAssertionAxiom)ax;

                    String subj = getIndividualId(ca.getIndividual());
                    String pred = "type";
                    String obj;
                    OWLClassExpression cx = ca.getClassExpression();
                    if (cx.isAnonymous()) {
                        untranslatedAxioms.add(ca);
                        continue;
                    }
                    else {
                        obj = getClassId(cx.asOWLClass());
                    }
                    edges.add(getEdge(subj, pred, obj));
                    nodeIds.add(subj); // always include
                    nodeIds.add(obj); // always include

                }
                 else if (ax instanceof OWLObjectPropertyAssertionAxiom) {
                    OWLObjectPropertyAssertionAxiom opa = (OWLObjectPropertyAssertionAxiom)ax;

                    String subj = getIndividualId(opa.getSubject());
                    String obj = getIndividualId(opa.getObject());
                    if (opa.getProperty().isAnonymous()) {
                        untranslatedAxioms.add(opa);
                    }
                    else {
                        String pred = getPropertyId(opa.getProperty().asOWLObjectProperty());

                        edges.add(getEdge(subj, pred, obj));
                    }
                    nodeIds.add(subj); // always include subject node of an OPA

                }
                else if (ax instanceof OWLEquivalentClassesAxiom) {
                    // EQUIVALENT

                    OWLEquivalentClassesAxiom eca = (OWLEquivalentClassesAxiom)ax;
                    List<OWLClassExpression> xs = eca.getClassExpressionsAsList();
                    List<OWLClassExpression> anonXs = 
                            xs.stream().filter(x -> x.isAnonymous()).collect(Collectors.toList());
                    List<OWLClassExpression> namedXs = 
                            xs.stream().filter(x -> !x.isAnonymous()).collect(Collectors.toList());
                    Set<String> xClassIds = 
                            namedXs.stream().map(x -> getClassId((OWLClass)x)).collect(Collectors.toSet());
                    if (anonXs.isEmpty()) {
                        // EquivalentNodesSet

                        // all classes in equivalence axiom are named
                        // TODO: merge pairwise assertions into a clique
                        EquivalentNodesSet enset = 
                                new EquivalentNodesSet.Builder().nodeIds(xClassIds).build();
                        ensets.add(enset);
                    }
                    else {
                        // possibilities are:
                        //  - LogicalDefinitionAxiom
                        //  - TBD

                        if (anonXs.size() == 1 && namedXs.size() == 1) {

                            OWLClassExpression anonX = anonXs.get(0);
                            if (anonX instanceof OWLObjectIntersectionOf) {

                                Set<OWLClassExpression> ixs =
                                        ((OWLObjectIntersectionOf)anonX).getOperands();

                                OBOClassDef classDef = getClassDef(ixs);

                                //                                List<String> genusClassIds = new ArrayList<>();
                                //                                List<ExistentialRestrictionExpression> restrs = new ArrayList<>();
                                //                                boolean isLDA = true;
                                //                                for (OWLClassExpression ix : ixs) {
                                //                                    if (!ix.isAnonymous()) {
                                //                                        genusClassIds.add(getClassId((OWLClass)ix));
                                //                                    }
                                //                                    else if (ix instanceof OWLObjectSomeValuesFrom) {
                                //                                        restrs.add(getRestriction(ix));
                                //                                    }
                                //                                    else {
                                //                                        isLDA = false;
                                //                                        break;
                                //                                    }
                                //
                                //                                }

                                if (classDef != null) {
                                    LogicalDefinitionAxiom lda =
                                            new LogicalDefinitionAxiom.Builder().
                                            definedClassId(getClassId((OWLClass) namedXs.get(0))).
                                            genusIds(classDef.genusClassIds).
                                            restrictions(classDef.restrs).
                                            build();
                                    ldas.add(lda);
                                }
                                else {
                                    untranslatedAxioms.add(eca);
                                }
                            }
                        }
                        else {

                        }
                    }
                }

                else if (ax instanceof OWLObjectPropertyAxiom) {

                    if (ax instanceof OWLSubObjectPropertyOfAxiom) {
                        OWLSubObjectPropertyOfAxiom spa = (OWLSubObjectPropertyOfAxiom)ax;
                        if (spa.getSubProperty().isAnonymous()) {

                        }
                        else if (spa.getSuperProperty().isAnonymous()) {
                        }
                        else {
                            String subj = getPropertyId(spa.getSubProperty().asOWLObjectProperty());
                            String obj = getPropertyId(spa.getSuperProperty().asOWLObjectProperty());
                            edges.add(getEdge(subj, SUBPROPERTY_OF, obj));
                        }

                    }
                    else if (ax instanceof OWLInverseObjectPropertiesAxiom) {
                        OWLInverseObjectPropertiesAxiom ipa = (OWLInverseObjectPropertiesAxiom)ax;
                        if (ipa.getFirstProperty().isAnonymous()) {

                        }
                        else if (ipa.getSecondProperty().isAnonymous()) {
                        }
                        else {
                            String p1 = getPropertyId(ipa.getFirstProperty().asOWLObjectProperty());
                            String p2 = getPropertyId(ipa.getSecondProperty().asOWLObjectProperty());
                            edges.add(getEdge(p1, INVERSE_OF, p2));
                        }

                    }
                    else if (ax instanceof OWLSubPropertyChainOfAxiom) {
                        OWLSubPropertyChainOfAxiom spc = (OWLSubPropertyChainOfAxiom)ax;
                        if (spc.getSuperProperty().isAnonymous()) {

                        }
                        else {
                            String p = getPropertyId(spc.getSuperProperty().asOWLObjectProperty());
                            List<String> cpids = spc.getPropertyChain().stream().map(
                                    cp -> cp.isAnonymous() ? null : getPropertyId(cp.asOWLObjectProperty())).
                                    collect(Collectors.toList());
                            if (cpids.stream().filter(pid -> pid == null).collect(Collectors.toList()).size() == 0) {
                                pcas.add(new PropertyChainAxiom.Builder().predicateId(p).chainPredicateIds(cpids).build());
                            }

                        }

                    }
                    else {
                        translateObjectPropertyAxiom(ax, domainRangeBuilderMap);
                    }

                }

                else {
                    untranslatedAxioms.add(ax);
                }
            }
            else {
                // NON-LOGICAL AXIOMS
                if (ax instanceof OWLAnnotationAssertionAxiom) {
                    OWLAnnotationAssertionAxiom aaa = (OWLAnnotationAssertionAxiom)ax;
                    OWLAnnotationProperty p = aaa.getProperty();
                    OWLAnnotationSubject s = aaa.getSubject();

                    // non-blank nodes
                    if (s instanceof IRI) {

                        String subj = getNodeId((IRI)s);

                        OWLAnnotationValue v = aaa.getValue();
                        String lv = null;
                        if (v instanceof OWLLiteral) {
                            lv = ((OWLLiteral)v).getLiteral();
                        }
                        IRI pIRI = p.getIRI();
                        if (p.isLabel()) {
                            if (lv != null) {
                                nodeIds.add(subj);
                                nodeLabelMap.put(subj, lv);
                            }
                        }
                        else if (isDefinitionProperty(pIRI)) {
                            if (lv != null) {
                                DefinitionPropertyValue def = 
                                        new DefinitionPropertyValue.Builder().
                                        val(lv).
                                        xrefs(meta.getXrefsValues()).
                                        build();

                                Meta.Builder nb = getMetaBuilder(nodeMetaBuilderMap, subj);
                                nb.definition(def);
                                nodeIds.add(subj);
                            }

                        }
                        else if (isHasXrefProperty(pIRI)) {
                            if (lv != null) {
                                XrefPropertyValue xref = 
                                        new XrefPropertyValue.Builder().
                                        val(lv).build();

                                Meta.Builder nb = getMetaBuilder(nodeMetaBuilderMap, subj);
                                nb.addXref(xref);
                                nodeIds.add(subj);
                            }

                        }
                        else if (p.isDeprecated()) {
                            if (aaa.isDeprecatedIRIAssertion()) {
                                Meta.Builder nb = getMetaBuilder(nodeMetaBuilderMap, subj);
                                nb.deprecated(true);
                            }
                        }
                        else if (p.isComment()) {
                            Meta.Builder nb = getMetaBuilder(nodeMetaBuilderMap, subj);
                            nb.addComment(lv.toString());                         
                        }
                        else if (isOboInOwlIdProperty(pIRI)) {

                            // skip

                        }
                        else if (isInSubsetProperty(pIRI)) {


                            Meta.Builder nb = getMetaBuilder(nodeMetaBuilderMap, subj);
                            nb.addSubset(v.toString());
                            nodeIds.add(subj);


                        }
                        else if (synonymVocabulary.contains(pIRI.toString())) {
                            SCOPES scope = synonymVocabulary.get(pIRI.toString());
                            if (lv != null) {
                                String synonymType = null;
                                for (OWLAnnotation a : aaa.getAnnotations()) {
                                    if (a.getProperty().getIRI().toString().equals(SynonymVocabulary.SYNONYM_TYPE)) {
                                        synonymType = a.getValue().toString();
                                    }
                                    else {
                                        // TODO: capture these in meta
                                    }
                                }
                                SynonymPropertyValue syn = new SynonymPropertyValue.Builder().
                                        scope(scope).
                                        synonymType(synonymType).
                                        val(lv).
                                        xrefs(meta.getXrefsValues()).
                                        build();
                                Meta.Builder nb = getMetaBuilder(nodeMetaBuilderMap, subj);
                                nb.addSynonym(syn);
                                nodeIds.add(subj);
                            }
                        }
                        else {
                            Meta.Builder nb = getMetaBuilder(nodeMetaBuilderMap, subj);
                            String val;
                            if (v instanceof IRI)
                                val = ((IRI)v).toString();
                            else if (v instanceof OWLLiteral)
                                val = ((OWLLiteral)v).getLiteral();
                            else if (v instanceof OWLAnonymousIndividual)
                                val = ((OWLAnonymousIndividual)v).getID().toString();
                            else
                                val = null;
                                
                            
                            BasicPropertyValue pv = new BasicPropertyValue.Builder().
                                    pred(getPropertyId(p)).
                                    val(val).
                                    build();
                            
                            nb.addBasicPropertyValue(pv);
                            nodeIds.add(subj);
                        }

                    }
                    else {
                        // subject is anonymous
                        untranslatedAxioms.add(aaa);
                    }

                }
            }
        }

        for (String n : nodeIds) {
            Builder nb = new Node.Builder().
                    id(n).
                    label(nodeLabelMap.get(n));
            if (nodeMetaBuilderMap.containsKey(n)) {
                Meta meta = nodeMetaBuilderMap.get(n).build();
                nb.meta(meta);
            }
            if (nodeTypeMap.containsKey(n)) {
                nb.type(nodeTypeMap.get(n));
            }
            nodes.add(nb.build());
        }
        String gid = null;
        String version = null;
        OWLOntologyID ontId = ontology.getOntologyID();
        if (ontId != null) {
            Optional<IRI> iri = ontId.getOntologyIRI();
            if (iri.isPresent()) {
                gid = getNodeId(iri.orElse(null));
                if (ontId.getVersionIRI().isPresent())
                    version = getNodeId(ontId.getVersionIRI().orElse(null));
            }
        }

        Meta meta = getAnnotations(ontology.getAnnotations(), version);
        List<DomainRangeAxiom> domainRangeAxioms = 
                domainRangeBuilderMap.values().stream().map(DomainRangeAxiom.Builder::build).collect(Collectors.toList());
        return new Graph.Builder().
                id(gid).
                meta(meta).
                nodes(nodes).
                edges(edges).
                equivalentNodesSet(ensets).
                logicalDefinitionAxioms(ldas).
                domainRangeAxioms(domainRangeAxioms).
                propertyChainAxioms(pcas).
                build();
    }


    private void translateObjectPropertyAxiom(
            OWLAxiom ax,
            Map<String, DomainRangeAxiom.Builder> domainRangeBuilderMap) {

        if (ax instanceof OWLObjectPropertyRangeAxiom) {
            OWLObjectPropertyRangeAxiom rax = (OWLObjectPropertyRangeAxiom)ax;
            OWLObjectPropertyExpression p = rax.getProperty();
            OWLClassExpression rc = rax.getRange();
            if (rc.isAnonymous()) {

            }
            else {
                DomainRangeAxiom.Builder b = getDRBuilder(p, domainRangeBuilderMap);
                b.addRangeClassId(getClassId(rc.asOWLClass()));

            }
        }
        else if (ax instanceof OWLObjectPropertyDomainAxiom) {
            OWLObjectPropertyDomainAxiom rax = (OWLObjectPropertyDomainAxiom)ax;
            OWLObjectPropertyExpression p = rax.getProperty();
            OWLClassExpression rc = rax.getDomain();
            if (rc.isAnonymous()) {

            }
            else {
                DomainRangeAxiom.Builder b = getDRBuilder(p, domainRangeBuilderMap);
                b.addDomainClassId(getClassId(rc.asOWLClass()));

            }
        }
    }

    private DomainRangeAxiom.Builder getDRBuilder(OWLObjectPropertyExpression p, Map<String, DomainRangeAxiom.Builder> domainRangeBuilderMap) {
        String pid;
        if (p.isAnonymous()) {
            //untranslatedAxioms.add(rax);
        }
        pid = getPropertyId((OWLObjectProperty) p);
        if (!domainRangeBuilderMap.containsKey(pid)) {
            domainRangeBuilderMap.put(pid, new DomainRangeAxiom.Builder().predicateId(pid));
        }
        return domainRangeBuilderMap.get(pid);

    }

    private void setNodeType(String id, RDFTYPES t,
            Map<String, RDFTYPES> nodeTypeMap) {
        nodeTypeMap.put(id, t);
    }

    private Meta.Builder getMetaBuilder(Map<String, Meta.Builder> nodeMetaBuilderMap, String id) {
        if (!nodeMetaBuilderMap.containsKey(id))
            nodeMetaBuilderMap.put(id, new Meta.Builder());
        return nodeMetaBuilderMap.get(id);
    }

    /**
     * Translate all axiom annotations into a Meta object
     * 
     * @param ax
     * @return
     */
    private Meta getAnnotations(OWLAxiom ax) {
        return(getAnnotations(ax.getAnnotations()));
    }
    private Meta getAnnotations(Set<OWLAnnotation> anns) {
        return getAnnotations(anns, null);
    }
    private Meta getAnnotations(Set<OWLAnnotation> anns, String version) {
            
        List<XrefPropertyValue> xrefs = new ArrayList<>();
        List<BasicPropertyValue> bpvs = new ArrayList<>();
        List<String> inSubsets = new ArrayList<>();
        boolean isDeprecated = false;
        for (OWLAnnotation ann : anns) {
            OWLAnnotationProperty p = ann.getProperty();
           
            OWLAnnotationValue v = ann.getValue();
            String val = v instanceof IRI ? ((IRI)v).toString() : ((OWLLiteral)v).getLiteral();
            if (ann.isDeprecatedIRIAnnotation()) {
                isDeprecated = true;
            }
            else if (isHasXrefProperty(p.getIRI())) {
                xrefs.add(new XrefPropertyValue.Builder().val(val).build());
            }
            else if (isInSubsetProperty(p.getIRI())) {
                inSubsets.add(val);
            }
            else if (isHasSynonymTypeProperty(p.getIRI())) {
                inSubsets.add(val);
            }
            else {
                bpvs.add(new BasicPropertyValue.Builder().
                        pred(getPropertyId(p)).
                        val(val).
                        build());
            }
        }
        org.geneontology.obographs.model.Meta.Builder b = new Meta.Builder();
        if (version != null) {
            b.version(version);
        }
        Meta.Builder builder = 
                b.basicPropertyValues(bpvs).
                subsets(inSubsets).
                xrefs(xrefs);
        if (isDeprecated)
            builder.deprecated(true);
        
        return builder.build();
    }

    private Edge getEdge(String subj, String pred, String obj) {
        return getEdge(subj, pred, obj, null);
    }


    private Edge getEdge(String subj, String pred, String obj, List<ExistentialRestrictionExpression> gciQualifiers) {
        return new Edge.Builder().sub(subj).pred(pred).obj(obj).build();
    }

    private ExistentialRestrictionExpression getRestriction(
            OWLClassExpression x) {
        if (x instanceof OWLObjectSomeValuesFrom) {
            OWLObjectSomeValuesFrom r = (OWLObjectSomeValuesFrom)x;
            OWLPropertyExpression p = r.getProperty();
            OWLClassExpression f = r.getFiller();
            if (p instanceof OWLObjectProperty && !f.isAnonymous()) {

                return new ExistentialRestrictionExpression.Builder()
                .propertyId(getPropertyId((OWLObjectProperty) p))
                .fillerId(getClassId((OWLClass) f))
                .build();
            }
        }
        return null;
    }

    private OBOClassDef getClassDef(Set<OWLClassExpression> ixs) {
        OBOClassDef def = new OBOClassDef();
        boolean isLDA = true;
        for (OWLClassExpression ix : ixs) {
            if (!ix.isAnonymous()) {
                def.genusClassIds.add(getClassId((OWLClass)ix));
            }
            else if (ix instanceof OWLObjectSomeValuesFrom) {
                def.restrs.add(getRestriction(ix));
            }
            else {
                isLDA = false;
                break;
            }

        } 
        if (!isLDA) {
            return null;
        }
        return def;
    }

    //    private String shortenIRI(IRI iri) {
    //        prefixHelper
    //    }

    private String getPropertyId(OWLObjectProperty p) {
        return p.getIRI().toString();
    }
    private String getPropertyId(OWLAnnotationProperty p) {
        return p.getIRI().toString();
    }
    private String getClassId(OWLClass c) {
        return c.getIRI().toString();
    }
    private String getPropertyId(OWLProperty p) {
        return p.getIRI().toString();
    }
    private String getIndividualId(OWLIndividual owlIndividual) {
        if (owlIndividual instanceof OWLNamedIndividual)
            return owlIndividual.asOWLNamedIndividual().getIRI().toString();
        else
            return owlIndividual.asOWLAnonymousIndividual().getID().toString(); // TODO - document blank nodes
    }

    /**
     * TODO: optionally compact the IRI using a prefix map
     * 
     * @param s
     * @return Id or IRI
     */
    private String getNodeId(IRI s) {
        return s.toString();
    }

    public boolean isDefinitionProperty(IRI iri) {
        return iri.toString().equals("http://purl.obolibrary.org/obo/IAO_0000115");
    }

    public boolean isHasXrefProperty(IRI iri) {
        return iri.toString().equals("http://www.geneontology.org/formats/oboInOwl#hasDbXref");
    }

    public boolean isInSubsetProperty(IRI iri) {
        return iri.toString().equals("http://www.geneontology.org/formats/oboInOwl#inSubset");
    }

    public boolean isHasSynonymTypeProperty(IRI iri) {
        return iri.toString().equals("http://www.geneontology.org/formats/oboInOwl#hasSynonymType");
    }
    
    public boolean isOboInOwlIdProperty(IRI iri) {
        return iri.toString().equals("http://www.geneontology.org/formats/oboInOwl#id");
    }

}
