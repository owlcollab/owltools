
/* 
 * Package: pachyowl.js
 *
 *
 *     Pachy -----> Maker
 *       ^
 *       |
 *     Generator <-- OWLFrame
 *
 */


importPackage(java.io);
importPackage(Packages.org.semanticweb.owlapi.model);
importPackage(Packages.org.semanticweb.owlapi.io);
importPackage(Packages.com.google.gson);

// ========================================
// SETUP
// ========================================
if (typeof bbop == 'undefined') { var bbop = {};}
if (typeof bbop.owl == 'undefined') { bbop.owl = {};}

// ========================================
// ENGINE
// ========================================

/* Namespace: bbop.owl.Pachy
 * 
 * constructor
 * 
 * Arguments:
 *  ont - OWLOntology
 */

bbop.owl.Pachy = function(ont) {
    if (ont != null) {
        //print("ONT="+ont);
        this.ontology = ont;
    }
    this.reasoner = null;
    this.maker = null;
    this.changes = [];
    this.generatedFrames = [];
}

/* Function: getOntology
 * returns: OWLOntology
 */
bbop.owl.Pachy.prototype.getOntology = function() {
    return this.ontology;
}
bbop.owl.Pachy.prototype.df = function() {
    return this.ontology.getOWLDataFactory();
}
bbop.owl.Pachy.prototype.getManager = function() {
    return this.ontology.getOWLOntologyManager();
}

/* Function: getMaker
 *
 * Returns: <Maker>
 */
bbop.owl.Pachy.prototype.getMaker = function() {
    if (this.maker == null) {
        this.maker = new bbop.owl.Maker(this);
    }
    return this.maker;
}

/* Function: getReasoner
 * returns: OWLReasoner
 */
bbop.owl.Pachy.prototype.getReasoner = function() {
    return this.reasoner;
    //return getReasoner(); // TODO - replace with OO
}


/* Function: grepAxioms
 *
 * filters axioms in ontology using a grep function.
 * The function takes on argument - the axiom - and returns
 * true if the axiom is to be included
 *
 * Arguments:
 *  grepFunc : function
 *
 * Returns: <OWLAxiom> []
 */
bbop.owl.Pachy.prototype.grepAxioms = function(grepFunc) {
    var inAxioms = this.ontology.getAxioms().toArray();
    var filteredAxioms = [];
    for (k in inAxioms) {
        var ax = inAxioms[k];
        if (grepFunc.call(this, ax)) {
            filteredAxioms.push(ax);
        }
    }
    return filteredAxioms;
}

/* Function: saveAxioms
 *
 * Arguments:
 *  axiom : <OWLAxiom> [] or OWLFrame
 *  file : fileName
 *  owlFormat : e.g. an instance of RDFXMLOntologyFormat
 *
 */
bbop.owl.Pachy.prototype.saveAxioms = function(obj, file, owlFormat) {
    var tempIRI = IRI.create("http://x.org#temp-"+java.util.UUID.randomUUID());

    var tmpOnt = gen.getManager().createOntology(tempIRI); // TODO
    var axioms = obj;
    if (obj instanceof bbop.owl.OWLFrame) {
        axioms = obj.toAxioms();
    }
    // add to temp ontology
    for (k in axioms) {
        gen.getManager().addAxiom(tmpOnt, axioms[k]);
    }
    this.saveOntology(tmpOnt, file, owlFormat);
}

/* Function: saveOntology
 *
 * Arguments:
 *  ontology: <OWLOntology>
 *  file : fileName
 *  owlFormat : e.g. an instance of RDFXMLOntologyFormat
 *
 */
bbop.owl.Pachy.prototype.saveOntology = function(ont, file, owlFormat) {

    if (owlFormat == null) {
        owlFormat = new RDFXMLOntologyFormat();
    }
    this.getManager().saveOntology(ont, owlFormat, IRI.create(new File(file)));
}

/* Function: save
 *
 * Purpose: saves current ontology
 *
 * Arguments:
 *  file : fileName
 *  owlFormat : e.g. an instance of RDFXMLOntologyFormat
 *
 */
bbop.owl.Pachy.prototype.save = function(file, owlFormat) {
    this.saveOntology(this.getOntology(), file, owlFormat);
}

// ----------------------------------------
// CHANGES
// ----------------------------------------

bbop.owl.Pachy.prototype.applyChange = function(change) {
    this.getManager().applyChange(change);
    this.changes.push(change);
    return change;
}

/* Function: add
 * Adds an axiom or axioms to ontology
 * Arguments:
 *  ax : OWLAxiom or bbop.owl.OWLFrame
 */
bbop.owl.Pachy.prototype.add = function(ax) {
    if (ax instanceof OWLAxiom) {
        return this.addAxiom(ax);
    }
    else if (ax instanceof bbop.owl.OWLFrame) {
        return this.addAxioms(ax.toAxioms());
    }
    else {
        print("FAIL: "+ax);
    }
}

/* Function: addAxiom
 * Adds an axiom to ontology
 * Arguments:
 *  ax : OWLAxiom
 */
bbop.owl.Pachy.prototype.addAxiom = function(ax) {
    var change = new AddAxiom(ont(), ax);
    return this.applyChange(change);
}

/* Function: addAxioms
 * Adds axioms to ontology
 * Arguments:
 *  ax : OWLAxiom[]
 */
bbop.owl.Pachy.prototype.addAxioms = function(axs) {
    for (k in axs) {
        this.addAxiom(axs[k]);
    }
    return axs;
}

/* Function: removeAxiom
 * Removes an axiom from ontology
 * Arguments:
 *  ax : OWLAxiom
 */
bbop.owl.Pachy.prototype.removeAxiom = function(ax) {
    var change = new RemoveAxiom(ont(), ax);
    this.applyChange(change);
    //g().getManager().removeAxiom(ont(),ax);    
}

bbop.owl.Pachy.prototype.removeAxioms = function(axs) {
    for (k in axs) {
        this.removeAxiom(axs[k]);
    }
}

bbop.owl.Pachy.prototype.replaceAxiom = function(oldAxiom, newAxioms) {
    this.removeAxiom(oldAxiom);
    this.addAxioms(newAxioms);
}

// ----------------------------------------
// UTIL
// ----------------------------------------

/* Function: getAnnotations
 *
 * Argument:
 *  obj: OWLNamedObject or IRI or IRI-as-string
 *  prop: OWLAnnotationProperty or IRI or IRI-as-string
 *
 * returns: OWLAnnotation[]
 */
bbop.owl.Pachy.prototype.getAnnotations = function(obj,prop) {
    if (!(obj instanceof OWLNamedObject)) {
        // note: it doesn't matter what kind of OWLNamedObject we create here
        if (!(obj instanceof IRI)) {
            obj = IRI.create(obj);
        }
        if (obj instanceof IRI) {
            obj = df().getOWLClass(obj);
        }
    }
    if (prop == null) {
        return obj.getAnnotations(this.getOntology()).toArray();
    }
    prop = this.getMaker().ensureAnnotationProperty(prop);
    var anns = obj.getAnnotations(this.getOntology(), prop).toArray();
    return anns;
}

/* Function: getLabel
 *
 * Argument:
 *  obj: OWLNamedObject or IRI or IRI-as-string
 *
 * returns: string
 */
bbop.owl.Pachy.prototype.getLabel = function(obj) {
    var anns = this.getAnnotations(obj, org.semanticweb.owlapi.vocab.OWLRDFVocabulary.RDFS_LABEL.getIRI());
    var label = null;
    for (k in anns) {
        if (label != null) {
            print("WARNING: multi-labels "+obj); // TODO
        }
        label = anns[k].getValue().getLiteral();
    }
    return label;
}

/* Function: isDeprecated
 * Arguments:
 *  obj : OWLObject
 *
 * returns: boolean
 */
bbop.owl.Pachy.prototype.isDeprecated = function(obj) {
    var anns = this.getAnnotations(obj, this.df().getOWLAnnotationProperty(org.semanticweb.owlapi.vocab.OWLRDFVocabulary.OWL_DEPRECATED.getIRI()));
    for (k in anns) {
        if (anns[k].getValue && anns[k].getValue().getLiteral() == 'true') {
            return true;
        }
    }
    return false;
}

/* Function: getFrame
 *
 * Argument:
 *  obj: OWLObject
 *
 * returns: <OWLFrame>
 */
bbop.owl.Pachy.prototype.getFrame = function(obj) {
    var f = new bbop.owl.OWLFrame(this, obj);
    return f;
}


bbop.owl.Pachy.prototype.getFrameMap = function() {
    var axioms = this.getOntology().getAxioms().toArray();
    var f = new bbop.owl.OWLFrame(this);
    print("Axioms:"+axioms.length);
    var fmap = f.axiomsToFrameMap(axioms);
    return fmap;
}

/* 
 * Namespace: bbop.owl.Maker
 *
 * contructor
 *
 * Arguments:
 *  parent : bbop.owl.Pachy
 *
 */
bbop.owl.Maker = function(parent) {
    this.parent = parent;
}

bbop.owl.Maker.prototype.df = function() {
    return this.parent.df();
}

bbop.owl.Maker.prototype.ensureClassExpression = function(obj) {
    // in future this may perform translation of json objects to OWL
    if (typeof obj == 'string' || obj instanceof String) {
        obj = IRI.create(obj);
    }
    if (obj instanceof IRI) {
        obj = this.df().getOWLClass(obj);
    }
    if (!(obj instanceof OWLClassExpression)) {
        print("WARNING: not CEX: "+obj);
    }
    return obj;
}

bbop.owl.Maker.prototype.ensureAnnotationProperty = function(prop) {
    if (!(prop instanceof OWLAnnotationProperty)) {
        if (!(prop instanceof IRI)) {
            prop = IRI.create(prop);
        }
        if (prop instanceof IRI) {
            prop = df().getOWLAnnotationProperty(prop);
        }
    }
    return prop;
}


/* Function: someValuesFrom
 * Arguments:
 *  p : OWLProperty
 *  filler : OWLExpression
 *
 * returns: OWLSomeValuesFrom
 */
bbop.owl.Maker.prototype.someValuesFrom = function(p, filler) {
    if (p instanceof OWLDataPropertyExpression) {
        return this.df().getOWLDataSomeValuesFrom(p, filler);
    }
    else {
        return this.df().getOWLObjectSomeValuesFrom(p, filler);
    }
}

/* Function: intersectionOf
 * Arguments:
 *  x1, x2, ... : OWLClassExpression
 *
 * returns: OWLObjectIntersectionOf or OWLDataIntersectionOf
 */
bbop.owl.Maker.prototype.intersectionOf = function() {
    var xset = new java.util.HashSet();
    var isData = false;
    for (k=0; k<arguments.length; k++) {
        // todo - detect isData
        xset.add(arguments[k]);
    }
    if (isData) {
        return get_data_factory().getOWLDataIntersectionOf(xset);
    }
    else {
        return get_data_factory().getOWLObjectIntersectionOf(xset);
    }
}

bbop.owl.Maker.prototype.makeAnnotationProperty = function(p) {
    if (typeof p == 'string') {
        p = IRI.create(p);
    }
    if (p instanceof IRI) {
        p = this.df().getOWLAnnotationProperty(p);
    }
    return p;
}

/* Function: ann
 * Arguments:
 *  p : OWLAnnotationProperty or IRI or string
 *  v : value
 *
 * Returns: OWLAnnotation
 */
bbop.owl.Maker.prototype.ann = function(p,v) {
    p = this.makeAnnotationProperty(p);
    if (typeof v == 'string') {
        v = this.literal(v);
    }
    return this.df().getOWLAnnotation(p,v);
};


// AXIOMS

/* Function: subClassOf
 * Arguments:
 *  sub : OWLClassExpression
 *  sup : OWLClassExpression
 *
 * Returns: OWLAxiom
 */
bbop.owl.Maker.prototype.subClassOf = function (sub,sup) { return this.df().getOWLSubClassOfAxiom(sub,sup) }

/* Function: classAssertion
 * Arguments:
 *  c : OWLClassExpression
 *  i : OWLIndividual
 *
 * Returns: OWLAxiom
 */
bbop.owl.Maker.prototype.classAssertion = function (c,i) { return this.df().getOWLClassAssertionAxiom(c,i) }

/* Function: equivalentClasses
 * Arguments:
 *  x1, x2, ... : OWLClassExpression
 *
 * Returns: OWLAxiom
 */
bbop.owl.Maker.prototype.equivalentClasses = function() {
    var set = new java.util.HashSet();
    for (k=0; k<arguments.length; k++) {
        set.add(this.ensureClassExpression(arguments[k]));
    }
    return this.df().getOWLEquivalentClassesAxiom(set);
}

/* Function: disjointClasses
 * Arguments:
 *  x1, x2, ... : OWLClassExpression
 *
 * Returns: OWLAxiom
 */
bbop.owl.Maker.prototype.disjointClasses = function() {
    var set = new java.util.HashSet();
    for (k=0; k<arguments.length; k++) {
        set.add(arguments[k]);
    }
    return this.df().getOWLDisjointClassesAxiom(set);
};

/* Function: disjointUntion
 * Arguments:
 *  c : OWLClass
 *  x1, x2, ... : OWLClassExpression
 *
 * Returns: OWLAxiom
 */
bbop.owl.Maker.prototype.disjointUnion = function(c) {
    var set = new java.util.HashSet();
    for (i=1; i<arguments.length; i++) {
        set.add(this.ensureClassExpression(arguments[i]));
    }
    return this.df().getOWLDisjointUnionAxiom(c, set);
}

/* Function: annotationAssertion
 * Arguments:
 *  p : OWLAnnotationProperty
 *  s : OWLObject or IRI or string
 *  v : value
 *
 * Returns: OWLAxiom
 */
bbop.owl.Maker.prototype.annotationAssertion = function(p,s,v) {
    if (typeof p == 'string') {
        p = IRI.create(p);
    }
    if (p instanceof IRI) {
        p = this.df().getOWLAnnotationProperty(p);
    }
    if (typeof v == 'string') {
        v = this.literal(v);
    }
    if (s.getIRI != null) {
        s = s.getIRI();
    }
    if (!(s instanceof IRI)) {
        s = IRI.create(s);
    }
    return this.df().getOWLAnnotationAssertionAxiom(p,s,v);
};

/* Function: labelAssertion
 * Arguments:
 *  s : OWLObject or IRI or string
 *  v : value
 *
 * Returns: OWLAxiom
 */
bbop.owl.Maker.prototype.labelAssertion = function(s,v) {
    return this.annotationAssertion(this.df().getOWLAnnotationProperty(org.semanticweb.owlapi.vocab.OWLRDFVocabulary.RDFS_LABEL.getIRI()),
                                    s,v);
};

bbop.owl.Maker.prototype.literal = function(v) {
    return this.df().getOWLLiteral(v);
};



// ========================================

/* 
 * Namespace: bbop.owl.Generator
 * 
 * constructor
 *
 * Extends: bbop.owl.Pachy
 *
 * Purpose: ontology generation methods
 * 
 * constructor: Generator
 */

bbop.owl.Generator = function() {
    bbop.owl.Pachy.apply(this, arguments);
    this.lastId = 0;
    this.idspace = "GO";
}
bbop.owl.Generator.prototype = new bbop.owl.Pachy();
//bbop.owl.Generator.prototype.constructor = bbop.owl.Pachy;

/* Function: GenIRI
 *
 * generators an available IRI within the default ID space
 *
 * Returns: IRI string
 */
bbop.owl.Generator.prototype.genIRI = function() {
    this.lastId++;
    var localId = java.lang.String.format("%07d", new java.lang.Integer(this.lastId));
    var iriStr = "http://purl.obolibrary.org/obo/"+this.idspace+"_"+localId;
    var iri = IRI.create(iriStr);
    var isUsed = false;
    var id = this.idspace+":"+localId;
    
    if (this.getOntology().getAnnotationAssertionAxioms(iri).size() > 0) {
        isUsed = true;
    }
    else {
        var c = this.df().getOWLClass(iri);
        if (this.getOntology().getAxioms(c).size() > 0) {
            isUsed = true;
        }
    }
    if (!isUsed) {
        var aaas = this.getOntology().getAxioms(AxiomType.ANNOTATION_ASSERTION).toArray();
        print("Checking AAAs "+aaas.length+" for "+id);
        for (k in aaas) {
            var ax = aaas[k];
            v = ax.getValue();
            if (v.getLiteral != null && v.getLiteral().toString() == iriStr) {
                print("used in assertion: "+ax);
                isUsed = true;
                break;
            }
            if (v.getLiteral != null && v.getLiteral().toString() == id) {
                print("used in assertion: "+ax);
                isUsed = true;
                break;
            }
        }
    }

    if (isUsed) {
        print(" USED: "+iri);
        return this.genIRI();
    }
    else {
        return iri;
    }
}


bbop.owl.Generator.prototype.concatLiteral = function() {
    var aa = Array.prototype.slice.call(arguments, 0);
    var toks = 
        aa.map(
        function(t){
            if (typeof t == 'string') {
                return t;
            }
            else {
                return this.getLabel(t); 
            }
        }).join(" ");
    return toks;
};


/*
 * Function: generateXP
 *
 * Generates a class frame using a basic genus-differentia pattern
 * 
 * Parameters:
 *  genus - the base parent class
 *  relation - the OWLObjectProperty of the differentiating characteristic
 *  filler - the OWLClassExpression of the differentiating characteristic
 * 
 * Returns: bbop.owl.OWLFrame
 */
bbop.owl.Generator.prototype.generateXP = function(genus, relation, diff) {
    var iri = this.genIRI();
    var id = iri.toString();
    var label = this.concatLiteral(genus,'of',diff);
    var m = this.getMaker();
    var ex = m.intersectionOf(genus, this.maker.someValuesFrom(relation,diff));
    print("EX = "+ex);
    var slotMap = {
        id: id,
        label: label,
        //annotations: {property:has_related_synonym, value: this.concatLiteral(diff,genus)},
        annotations: m.ann(has_exact_synonym, this.concatLiteral(diff,genus)),
        definition: this.concatLiteral('a',genus,'that is',relation,'a',diff),
        equivalentTo: ex
    };
    var f = new bbop.owl.OWLFrame(this, slotMap);
    f.stamp();
    this.generatedFrames.push(f);
    return f;
}

bbop.owl.Generator.prototype.makeFrames = function() {
    var gen = this;
    var aa = Array.prototype.slice.call(arguments, 0);
    return aa.map(function(args) {return gen.makeFrame.apply(gen,args)});
}


// ========================================

/* 
 * 
 * Namespace: bbop.owl.OWLFrame
 * 
 * An OWLFrame is a representation of axioms associated with a particular OWLObject.
 * It consists of a slotMap, which is a dictionary with the following keys:
 *
 *  id : a IRI string
 *  type : (TODO) e.g. "Class"
 *  SubClassOf, equivalentTo, annotations, ... : axiom info (list or single value)
 * 
 * In addition, "flattened" representations are allowed.
 * here, the key is the IRI for an OWLProperty.
 *  - if this is an annotation property IRI, then this is a shorthand for an "annotations" key with this property.
 *  - if this is an object property IRI, then this is a shorthand for an "subClassOf" key with a someValuesFrom expression with this property
 * 
 * 
 * constructor
 *
 * Arguments:
 *  generator - bbop.owl.Generator
 *  obj - either an OWLObject or a slotMap dictionary
 */
bbop.owl.OWLFrame = function(gen, obj) {
    this.generator = gen;
    this.type = null;
    // translate from an OWLObject (e.g. OWLClass) to a frame
    if (obj instanceof OWLObject) {
        var axioms = gen.getOntology().getAxioms(obj);
        var annAxioms = gen.getOntology().getAnnotationAssertionAxioms(obj.getIRI());
        axioms.addAll(annAxioms);
        var fmap = this.axiomsToFrameMap(axioms.toArray());
        this.slotMap = fmap[this.getKey(obj)];
    }
    else {
        this.slotMap = obj;
    }
}

/* Function: flatten
 * 
 * translates:
 *  annotations: [ann(p1,v1), ann(p2,v2), ..] ==> p1: v1, p2: v2, ..
 * and
 *  subClassOf: [someValuesFrom(p1,v1),someValuesFrom(p2,v2), ..] ==> p1: v1, p2: v2, ..
 */
bbop.owl.OWLFrame.prototype.flatten = function() { 
    if (this.slotMap.annotations != null) {
        if (!(this.slotMap.annotations instanceof Array)) {
            this.slotMap.annotations = [this.slotMap.annotations];
        }
        for (k in this.slotMap.annotations) {
            var ann = this.slotMap.annotations[k];
            var p = ann.getProperty();
            var pid = this.getKey(p);
            if (!this.slotMap[pid]) {
                this.slotMap[pid] = ann.getValue();
            }
            else {
                if (!(this.slotMap[pid] instanceof Array)) {
                    this.slotMap[pid] = [this.slotMap[pid]];
                }
                this.slotMap[pid].push(ann.getValue());
            }
        }
        delete this.slotMap.annotations;
    }
    if (this.slotMap.subClassOf != null) {
        var newSupers = [];
        if (!(this.slotMap.subClassOf instanceof Array)) {
            this.slotMap.subClassOf = [this.slotMap.subClassOf];
        }
        for (k in this.slotMap.subClassOf) {
            var sup = this.slotMap.subClassOf[k];
            if (sup instanceof OWLObjectSomeValuesFrom) {
                var p = sup.getProperty();
                var pid = this.getKey(p);
                if (!this.slotMap[pid]) {
                    this.slotMap[pid] = sup.getFiller();
                }
                else {
                    if (!(this.slotMap[pid] instanceof Array)) {
                        this.slotMap[pid] = [this.slotMap[pid]];
                    }
                    this.slotMap[pid].push(sup.getFiller());
                }
            }
            else {
                newSupers.push(sup);
            }
        }
        this.slotMap.subClassOf = newSupers;
    }
    
}

/* Function: stamp
 *
 * Adds default slot values to frame, including:
 *  - dc:creator
 *  - dc:date
 *
 * Returns: string
 */
bbop.owl.OWLFrame.prototype.stamp = function() {
    //print("STAMPING...");
    if (this.slotMap.id == null) {
        this.slotMap.id = this.generator.genIRI().toString();
    }
    if (this.slotMap.date == null) {
        //this.slotMap.date = '';
    }
    if (this.generator.defaultSlotMap != null) {
        for (k in this.generator.defaultSlotMap) {
            if (this.slotMap[k] == null) {
                this.slotMap[k] = this.generator.defaultSlotMap[k];
            }
        }
    }
    return this;
};


/* Function: render
 *
 * Renders the frame as javascript
 *
 * Returns: string
 */
bbop.owl.OWLFrame.prototype.render = function() {
    return this.pp(this.slotMap);
};

bbop.owl.OWLFrame.prototype.quote = function(s) { 
    return "\"" + s + "\"";
}


bbop.owl.OWLFrame.prototype.pp = function(object, depth, embedded) { 
    typeof(depth) == "number" || (depth = 0)
    typeof(embedded) == "boolean" || (embedded = false)
    var newline = false
    var spacer = function(depth) { var spaces = ""; for (var i=0;i<depth;i++) { spaces += "  "}; return spaces }
    var pretty = ""
    if (      typeof(object) == "undefined" ) { pretty += "undefined" }
    else if ( typeof(object) == "boolean" || 
              typeof(object) == "number" ) {    pretty += object.toString() } 
    else if ( typeof(object) == "string" ) {    pretty += this.quote(object) }
    else if ( object instanceof String ) {    pretty += this.quote(object) }
    else if (        object  == null) {         pretty += "null" } 
    else if ( object instanceof(Array) ) {
        if ( object.length > 0 ) {
            if (embedded) { newline = true }
            var content = ""
            for each (var item in object) { content += this.pp(item, depth+1) + ",\n" + spacer(depth+1) }
            content = content.replace(/,\n\s*$/, "").replace(/^\s*/,"")
            pretty += "[ " + content + "\n" + spacer(depth) + "]"
        } else { pretty += "[]" }
    } 
    else if (typeof(object) == "object") {
        if (object instanceof OWLObject) {
            
            if (object instanceof OWLNamedObject) {
                pretty += getClassVariableName(object); // TODO
            }
            else if (object instanceof OWLObjectSomeValuesFrom) {
                pretty += "someValuesFrom(" + this.pp(object.getProperty()) +" , " + this.pp(object.getFiller())+") ";
            }
            else if (object instanceof OWLAnnotation) {
                pretty += "ann(" + this.pp(object.getProperty()) +" , " + this.pp(object.getValue())+") ";
            }
            else if (object instanceof OWLLiteral) {                
                pretty += this.quote(object.getLiteral()); // TODO
            }
            else if (object instanceof OWLObjectIntersectionOf) {
                var args = object.getOperandsAsList().toArray();
                var args2 = args.map(function(x){ return this.pp(x, depth, embedded)})
                pretty += "intersectionOf(" + args2.join(", ") + ")";
            }
            else {
                // TODO
                pretty += object.toString();
            }
        }
        else if (object instanceof java.lang.Object) {
            pretty += object;
        }      
        // TODO Object.keys() not in distributed rhino?
        else if ( !(Object.keys) || Object.keys(object).length > 0 ){
            if (embedded) { newline = true }
            var content = ""
            for (var key in object) { 
                var keyStr = key.toString();
                if (keyStr.indexOf("http:")) {
                    //print("LOOKUP: "+key);
                    keyStr = getClassVariableName(IRI.create(key)); // TODO
                    if (keyStr == null) {
                        keyStr = key.toString();
                    }
                }
                content += spacer(depth + 1) + keyStr + ": " + this.pp(object[key], depth+2, true) + ",\n" 
            }
            content = content.replace(/,\n\s*$/, "").replace(/^\s*/,"")
            pretty += "{ " + content + "\n" + spacer(depth) + "}"
        } 
        else { pretty += "{}"}
    }
    else { pretty += object.toString() }
    return ((newline ? "\n" + spacer(depth) : "") + pretty)
}


bbop.owl.OWLFrame.prototype.addFrame = function() {
    // TODO
    this.generator.addAxioms(this.toAxioms());
};

bbop.owl.OWLFrame.prototype.ensureHasId = function() {
    if (this.slotMap.id == null) {
        this.slotMap.id = this.generator.genIRI().toString();
    }
    return this.slotMap.id;
}

/* Function: toAxioms
 *
 * Returns: OWLAxiom[]
 */
bbop.owl.OWLFrame.prototype.toAxioms = function() {
    this.ensureHasId();
    var f = this.slotMap;
    var id = f.id;
    var obj = id;
    //print("Generating axioms for frame: "+id);
    var maker = this.generator.getMaker();

    // todo - types
    if (!(obj instanceof OWLClassExpression)) {
         obj = maker.ensureClassExpression(id);
    }
    //print("  Obj: "+obj + " " + obj instanceof OWLClass);
    var axioms = [];
    for (k in f) {
        var v = f[k];
        var vs = v;
        if (!(v instanceof Array)) {
            vs = [v];
        }
        // TODO - split this to allow generation of individual axioms
        for (var i=0; i<vs.length; i++) {
            var v = vs[i];
            //print(k+" = "+v + " // "+i+" of "+vs.length);
            switch(k.toLowerCase()) 
            {
            case 'id' : 
                break;
            case 'subclassof' :
                axioms.push(maker.subClassOf(obj, maker.ensureClassExpression(v)));
                break;
            case 'equivalentto' :
                axioms.push(maker.equivalentClasses(obj, maker.ensureClassExpression(v)));
                break;
            case 'disjointwith' :
                axioms.push(maker.disjointClasses(obj, maker.ensureClassExpression(v)));
                break;
            case 'label' :
                axioms.push(maker.labelAssertion(obj, maker.literal(v)));
                break;
            case 'annotations' :
                axioms.push(maker.annotationAssertion(v.property, obj, maker.literal(v.value)));
                break;
            default :
                // todo - allow properties
                var p = k;
                if (typeof p == 'string') {
                    p = lookup(k);
                }
                if (p instanceof OWLAnnotationProperty) {
                    axioms.push(maker.annotationAssertion(p, obj, maker.literal(v)));
                }
                else if (p instanceof OWLObjectProperty) {
                    axioms.push(maker.subClassOf(obj, maker.someValuesFrom(p, maker.ensureClassExpression(v))));
                }
                else {
                    print("unknown: "+k);
                }
            }
        }
    }
    //print("axioms:"+axioms.length);

    for (k in axioms) {
        var a = axioms[k];
    }
    return axioms;
};

bbop.owl.OWLFrame.prototype.getKey = function(obj) {
    if (obj instanceof OWLNamedObject) {
        return obj.getIRI().toString();
    }
    if (obj instanceof IRI) {
        return obj.toString();
    }
    return obj.toString();
}

// generate frames from axioms
bbop.owl.OWLFrame.prototype.axiomsToFrameMap = function(axioms, renderer) {

    var fmap = {};
    for (var k in axioms) {
        var ax = axioms[k];
        if (ax instanceof OWLSubClassOfAxiom) {
            var x = this.getKey(ax.getSubClass());
            if (fmap[x] == null) {
                fmap[x] = { id:x };
            }
            if (fmap[x].subClassOf == null) {
                fmap[x].subClassOf = [];
            }
            //fmap[x].subClassOf.push(render(ax.getSuperClass()));
            fmap[x].subClassOf.push(ax.getSuperClass());
        }
        else if (ax instanceof OWLEquivalentClassesAxiom) {
            var xs = ax.getNamedClasses().toArray();
            for (k in xs) {
                var xobj = xs[k];
                var x = this.getKey(xobj);
                if (fmap[x] == null) {
                    fmap[x] = { id:x };
                }
                if (fmap[x].equivalentTo == null) {
                    fmap[x].equivalentTo = [];
                }
                var rest = ax.getClassExpressionsMinus(xobj).toArray();
                for (k2 in rest) {
                    fmap[x].equivalentTo.push(rest[k2]);
                }
            }
        }
        else if (ax instanceof OWLAnnotationAssertionAxiom) {
            var x = this.getKey(ax.getSubject());
            if (fmap[x] == null) {
                fmap[x] = { id:x };
            }
            if (fmap[x].annotations == null) {
                fmap[x].annotations = [];
            }
            //fmap[x].annotations.push({property: ax.getProperty(), value: ax.getValue()});
            fmap[x].annotations.push(ax.getAnnotation());
        }
        else if (ax instanceof OWLObjectPropertyCharacteristicAxiom) {
            var x = this.getKey(ax.getProperty());
            if (fmap[x] == null) {
                fmap[x] = { id:x };
            }
            if (fmap[x].characteristics == null) {
                fmap[x].characteristics = [];
            }
            fmap[x].characteristics.push(ax.getAxiomType());
            
        }
        else if (ax instanceof OWLDeclarationAxiom) {
            var t = ax.getEntity().getEntityType().getName();
            if (fmap[x] == null) {
                fmap[x] = { id:x };
            }
            if (fmap[x].declaration == null) {
                fmap[x].declaration = t;
            }
            else if (fmap[x].declaration instanceof Array) {
                fmap[x].declaration.push(t);
            }
            else {
                fmap[x].declaration = [fmap[x].declaration, t];
            }
        }
        else {
            print("Cannot process: "+ax);
        }
    }
    return fmap;
};

bbop.owl.OWLFrame.prototype.axiomsToFrame = function(axioms, id) {
    var fmap = this.axiomsToFrameMap(axioms);
    return fmap[id];
};

/* Function: merge
 *
 * Purpose: merges an OWLFrame into this one
 *
 * Arguments:
 *  f2 - <OWLFrame>
 */
bbop.owl.OWLFrame.prototype.merge = function(f2) {
    for (k in f2.slotMap) {
        if (this.slotMap[k] == null) {
            this.slotMap[k] = f2.slotMap[k];
        }
        else if (this.slotMap[k] instanceof Array) {
            if (f2.slotMap[k] instanceof Array) {
                this.slotMap[k] = this.slotMap.concat(f2.slotMap[k]);
            }
            else {
                this.slotMap[k].push(f2.slotMap[k]);
            }
        }
        else {
            // this.slotMap[k] is single valued
            if (f2.slotMap[k] instanceof Array) {
                var cur = this.slotMap[k];
                this.slotMap[k] = f2.slotMap[k].concat(cur);
            }
            else {
                this.slotMap[k] = [this.slotMap[k], f2.slotMap[k]];
            }
        }
    }
};

/* Function: set
 *
 * Purpose: sets slot values
 *
 * Arguments:
 *  k : slot
 *  v : value
 */
bbop.owl.OWLFrame.prototype.set = function(k,v) {
    if (this.slotMap[k] == null) {
        this.slotMap[k] = v;
    }
    else if (this.slotMap[k] instanceof Array) {
        this.slotMap[k].push(v);
    }
    else {
        // this.slotMap[k] is single valued
        this.slotMap[k] = [this.slotMap[k], v];
    }
    return this;
};

// experimental
bbop.owl.OWLFrame.prototype.sed = function(sedFunc) {
    anns = this.slotMap.annotations;
    var maker = this.generator.getMaker();
    if (!(anns instanceof Array)) {
        anns = [anns];
    }
    for (k in anns) {
        var ann = anns[k];
        var v = ann.getValue();
        if (v.getLiteral != null) {
            var vLit = vl.getLiteral().toString();
            var vNew = sedFunc.call(this, vl);
            anns[k] = maker.ann(ann.getProperty(), vNew);
        }
    }
};

