
importPackage(java.io);
importPackage(Packages.org.semanticweb.owlapi.model);
importPackage(Packages.org.semanticweb.owlapi.io);
importPackage(Packages.owltools.cli);
importPackage(Packages.owltools.io);
importPackage(Packages.owltools.mooncat);
importPackage(Packages.owltools.graph);
importPackage(Packages.org.obolibrary.macro);
importPackage(Packages.com.google.gson);

var runner;
var reasoner;
var mst;
var isElk = false;
var objmap;
var changes = [];
var currentFile;
var currentFmt;
var currentFrame;
var labelToIRIMap = {};
var gen;
var o = {}; // ontology class lookup

function wg() {
    return gen;
}
function setGen(newg) {
    gen = newg;
}
function maker() {
    return gen.getMaker();
}

function debug(x) {
    print(x);
}

// initializes runner with new CommandRunner
function init() {
    runner = new Sim2CommandRunner();
    runner.exitOnException = false;
    runner.isDisposeReasonerOnExit = false;
}

function reload_source() {
    var src = java.lang.System.getenv().get("OWLRUNNER_SOURCE");
    load(src);
}


// executes commands using owltools command line syntax.
// E.g.
//   x("-a limb"); // ancestors of limbs
//   x("foo.owl"); // loads foo.owl in graph wrapper
function x(args) {
    if (runner == null) {
        init();
    }
    runner.run(args.split(" "));
}

//function mkOpts(h) {
//    new Opts(java.lang.reflect.Array.newInstance(java.lang.String, 5))
//}

// initializes a reasoner. E.g. reason("elk")
function reason(rn) {
    runner.run(["--reasoner",rn]);
    reasoner = runner.g.getReasoner();
}

function getReasoner() {
    if (reasoner == null) {
        elk();
    }
    return reasoner;
}

function rq(expr) {
    if (isElk) {
        runner.run(["--reasoner-query","-m", expr]);
    }
    else {
        runner.run(["--reasoner-query",expr]);
    }
}

// shortcuts
function elk() {
    reason("elk");
    isElk = true;
}
function welk() {
    reason("welk");
}

// gets the current graph object
function g() {
    return runner.g;
}

// gets the current ontology object
function ont() {
    return runner.g.getSourceOntology();
}

function o2j(owlObj) {
    ogr = new OWLGsonRenderer(null);
    gson = new Gson();
    obj = ogr.convert(owlObj);
    return eval('('+gson.toJson(obj)+')');
}

function owlpp(obj) {
    var label = getLabel(obj);
    print(obj + "'"+label+"'");
}

function parseManx(expr) {
    mst = new ManchesterSyntaxTool(g().getSourceOntology(), g().getSupportOntologySet(), true);
    return mst.parseManchesterExpression(expr);
}

function dlq(expr) {
    var x = parseManx(expr);
    return getSubClasses(x, true);
}

function getSubClasses(expr,isDirect,isReflexive) {
    if (isDirect == null) {
        isDirect = true;
    }
    if (expr.isAnonymous() && isElk) {
        //print("Elk no like anon expressions!");
        var q = owl("http://x.org/query");
        print("Making query class: "+q);
        var ax = equivalentClasses(q,expr);
        print("ECA: "+ax);
        addAxiom(ax);
        expr = q;
    }
    var subcs = getReasoner().getSubClasses( expr, isDirect ).getFlattened();
    if (isReflexive) {
        subcs.addAll(getReasoner().getEquivalentClasses( expr ).getEntities());
    }
    return subcs.toArray();
}

function getRoots() {
    var classes = ont().getClassesInSignature().toArray();
    var roots = [];
    for (var k in classes) {
        var c = classes[k];
        if (isDeprecated(c)) {
            continue;
        }
        if (c.getSuperClasses(ont()).size() == 0) {
            roots.push(c);
        }
    }
    return roots;
}

function obo(id) {
    return "http://purl.obolibrary.org/obo/"+id;
}
function go(num) {
    return(obo("GO_"+num));
}

function get(label) {
    return g().getOWLObjectByLabel(label);
}
function lookup(label) {
    if (labelToIRIMap[label] != null) {
        return labelToIRIMap[label];
    }
    return g().getOWLObjectByLabel(label);
}

// NOTE: in ringojs, returns a string; in rhino a java.lang.String
function getLabel(obj) {
    return g().getLabel(obj);
}

function getIdLabelPair(obj) {
    return obj.toString()+" '"+g().getLabel(obj)+"'";
}

// ========================================
// DATA FACTORY METHODS
// ========================================

function get_data_factory() {
    return g().getDataFactory();
}

function df() {
    return g().getDataFactory();
}

// CLASS EXPRESSIONS
// note we assume object expressions for now - in future this should auto-detect


var someValuesFrom = function() {return maker().someValuesFrom.apply(maker(),arguments)};
var intersectionOf = function() {return maker().intersectionOf.apply(maker(),arguments)};
var subClassOf = function() {return maker().subClassOf.apply(maker(),arguments)};
var classAssertion = function() {return maker().classAssertion.apply(maker(),arguments)};
var equivalentClasses = function() {return maker().equivalentClasses.apply(maker(),arguments)};
var disjointClasses = function() {return maker().disjointClasses.apply(maker(),arguments)};
var annotationAssertion = function() {return maker().annotationAssertion.apply(maker(),arguments)};
var labelAssertion = function() {return maker().labelAssertion.apply(maker(),arguments)};
var ann = function() {return maker().ann.apply(maker(),arguments)};
var literal = function() {return maker().literal.apply(maker(),arguments)};

// util
var isDeprecated = function() {return wg().isDeprecated.apply(wg(),arguments)};

function makeFrame(sm) {
    return new bbop.owl.OWLFrame(wg(), sm);
}

function showFrame(sm) {
    var f = new bbop.owl.OWLFrame(wg(), sm);
    print(f.render());
    return f;
}

// ========================================
// CH..CH..CHANGES
// ========================================

function add(obj) {
    return gen.add(obj);
}

function applyChange(change) {
    g().getManager().applyChange(change);
    changes.push(change);
}

function addAxiom(ax) {
    var change = new AddAxiom(ont(), ax);
    applyChange(change);
}
function addAxioms(axs) {
    for (var k in axs) {
        addAxiom(axs[k]);
    }
}
function removeAxiom(ax) {
    var change = new RemoveAxiom(ont(), ax);
    applyChange(change);
    //g().getManager().removeAxiom(ont(),ax);    
}
function removeAxioms(axs) {
    for (var k in axs) {
        removeAxiom(axs[k]);
    }
}

function expandMacros() {
    var mev = new MacroExpansionVisitor(gen.getOntology());
    mev.expandAll();
    mev.dispose();
}

function saveAxioms(obj, file, owlFormat) {
    var tmpOnt = gen.getManager().createOntology(IRI.create("http://x.org#")); // TODO
    var axioms = obj;
    if (obj instanceof bbop.owl.OWLFrame) {
        axioms = obj.toAxioms();
    }
    for (var k in axioms) {
        gen.getManager().addAxiom(tmpOnt, axioms[k]);
    }
    var pw = new ParserWrapper();
    if (owlFormat == null) {
        owlFormat = new org.coode.owlapi.obo.parser.OBOOntologyFormat();
    }
    pw.saveOWL(tmpOnt, owlFormat, file, g());
    gen.getManager().removeOntology(tmpOnt);
}

function reload() {
    loadont(currentFile);
}

function loadont(f, isUseCatalog) {
    // todo - remember format

    // use owltools cli for now...
    if (isUseCatalog) {
        x("--use-catalog "+f);
    }
    else {
        x(f); 
    }
    currentFile = f;
}

function save(f, fmt) {
    if (f == null) {
        f = currentFile;
    }
    if (fmt == null) {
        fmt = currentFmt;
    }
    gen.save(f, fmt);
}

function makeXP(genus, rel, dc) {

    print("Making...");
    var f = gen.generateXP(genus,rel,dc);
    currentFrame = f;
    print(f.render());
    print(f.toAxioms());
    print("Adding...");
    add(f);
    return f;
}


function makeClass() {
    var label = arguments[0];
    var sups = [];
    for(var i = 1; i < arguments.length; i++) {
        sups.push(arguments[i]);
    }
    var slotMap = {
        id: gen.genIRI(),
        label: label,
        subClassOf: sups
    };
    var f = new bbop.owl.OWLFrame(gen, slotMap);
    add(f);
    currentFrame = f;
    return f;
}

function makeFrame(slotMap) {
    if (slotMap.id == null) {
        slotMap.id = gen.genIRI();
    }
    var f = new bbop.owl.OWLFrame(gen, slotMap);
    currentFrame = f;
    return f;
}

function mireot(cls, ont) {
    m = new Mooncat(g());
    
}

// todo - axiom annotations
function tag(obj, slot, val) {
    var f = gen.getFrame(obj);
    currentFrame = f;
    if (f[slot] == null) {
        f[slot] = val;
    }
    else if (f[slot] instanceof Array) {
        f[slot].push(val);
    }
    else {
        f[slot] = [f[slot], val];
    }
    add(f); // modifies ontology
    return f;
}

function addImage(cls, img) {
    if (!(img instanceof OWLIndividual)) {
        if (!(img instanceof IRI)) {
            img = IRI.create(img);
        }
        img = df().getOWLNamedIndividual(img);
    }
    add( classAssertion( someValuesFrom(depicts, cls), img) );
}


// ========================================
// EVALUATION
// ========================================

//Array.prototype.diff = function(a) {
//    return this.filter(function(i) {return (a.indexOf(i) < 0);});
//};

//function arrayNotContains(a,i) {return (a.indexOf(i) < 0) };

function recapitulateAssertions(baseCls) {
    var classes = getSubClasses(baseCls, false, true);
    print("TESTING: "+classes.length);
    var axioms = ont().getAxioms(AxiomType.SUBCLASS_OF).toArray();
    print("CANDIDATE AXIOMS: "+axioms.length);

    var n_new = 0;
    for (var k in classes) {
        var c = classes[k];
        var assertedSups = c.getSuperClasses(ont()).toArray();
        var inferredSups = getReasoner().getSuperClasses(c, true).getFlattened().toArray();
        for (j in inferredSups) {
            var sup = inferredSups[j];
            if (assertedSups.indexOf(sup) == -1) {
                print("NEW: "+getIdLabelPair(c)+" SubClassOf "+getIdLabelPair(sup));
                n_new ++;
            }
        }
    }
    print("#NEW = "+n_new);
    print("Justifying. # axioms to check = "+axioms.length);

    var rmAxioms = [];
    for (var k in axioms) {
        var ax = axioms[k];
        if (classes.indexOf(ax.getSuperClass()) > -1) {
            print("RM: "+ax);
            rmAxioms.push(ax);
        }
    }
    print("REMOVING :"+rmAxioms.length);
    removeAxioms(rmAxioms);

    for (var k in rmAxioms) {
        var ax = rmAxioms[k];
        var sub = ax.getSubClass();
        var sup = ax.getSuperClass();
        var result;
        if (getReasoner().getSuperClasses(sub, true).contains(sup)) {
            result = 'OK';
        }
        else if (getReasoner().getSuperClasses(sub, false).getFlattened().contains(sup)) {
            //result = 'INDIRECT ## ' + getReasoner().getSuperClasses(sub, true).toArray().map(getLabel).join(" + ");
            result = 'INDIRECT';
        }
        else {
            result = 'FAIL';
        }
        print(result + " :: "+getIdLabelPair(sub)+" SubClassOf "+getIdLabelPair(sup));
    }
    print("DONE");
}

// this is temporary until we resolve ringo vs rhino differences
function javaString(s) {
    if (s == null) {
        return null;
    }
    if (s.replaceAll != null) {
        return s;
    }
    return new java.lang.String(s);
}

// ========================================
// OWL MANIPULATION
// ========================================

function getClassVariableName(obj) {
    var label = getLabel(obj);
    label = javaString(label); // TODO
    if (label == null && obj.getIRI != null) {
        var iri = obj.getIRI();
        if (iri != null) {
            label = iri.toString();
            label = javaString(label); // TODO
            if (label.contains("#")) {
                label = label.replaceAll(".*#","");
            }
            else if (label.contains("/")) {
                label = label.replaceAll(".*/","");
            }
        }
    }
    if (label != null) {
        label = safeify(label);
    }
    labelToIRIMap[label] = obj;
    return label;
}

function safeify(label) {
    label = javaString(label);
    label = label.replaceAll("\\W", "_");
    var c1 = label.substr(0,1);
    if (c1 >= '0' && c1 <= '9') {
        label = "_"+label;
    }
    return label;
}

function getAllOWLObjects() {
    var objs = g().getAllOWLObjects();
    objs.addAll(ont().getAnnotationPropertiesInSignature());
    return objs.toArray();
}

// experimental and DANGEROUS
//  sets a variable for every owl object.
//   e.g. organ = <http://...>
// TODO - warn if dupe label
function setClassVars() {
    var objs = getAllOWLObjects();
    for (var k in objs) {
        var obj = objs[k];
        if (!(obj instanceof OWLObject)) {
            continue;
        }
        //debug("making var for "+obj +" " + typeof obj);
        if (isDeprecated(obj)) {
            continue;
        }
        var label = getClassVariableName(obj);
        // no clobber
        while (this[label] != null || isReserved(label)) {
            print("Remapping "+label +" --> _" + label+" ( current value = "+this[label]+" )");
            label = '_'.label;
        }
        if (label != null) {
            //debug(" llabel="+label);
            eval("o."+label+" = obj");
        }
    }
}

function isReserved(s) {
    if (s == 'id') { return true };
    if (s == 'SubClassOf') { return true };
    if (s == 'EquivalentTo') { return true };
    return false;
}

// creates lookup index
// e.g. objmap.organ
function indexOnt() {
    objmap = {};
    var objs = getAllOWLObjects();
    //var objs = g().getAllOWLObjects().toArray();;
    for (var k in objs) {
        var obj = objs[k];
        var label = getClassVariableName(obj);
        if (label != null) {
            objmap[label] = obj;
        }
    }
}




// ========================================
// GENERIC
// ========================================

function dumpSubsets(matchPrefix, iriPrefix, dir) {
    var pw = new ParserWrapper();
    var subsetGenerator = new QuerySubsetGenerator();
    var man = g().getManager();
    var ont = g().getSourceOntology();
    var objs = ont.getClassesInSignature(true).toArray();
    //g().addSupportOntology(ont);
    owlFormat = new RDFXMLOntologyFormat();

    if (typeof reasoner == "undefined") {
        elk();
    }

    for (var k in objs) {
        var obj = objs[k];
        print(obj);
        var id = g().getIdentifier(obj);
        id = id.replace(":","_");
        id = id.replace(":","%3A");
        print(id);
        if (id.search(matchPrefix) == 0) {
            var descs = reasoner.getSubClasses(obj, false).getFlattened();
            print(descs);
            descs.add(obj);
            g().setSourceOntology(man.createOntology(IRI.create(iriPrefix + id + ".owl")));
            g().setSupportOntologySet(new java.util.HashSet);
            //g().addSupportOntology(ont);
            supps = new java.util.HashSet;
            supps.add(ont);
            subsetGenerator.createSubSet(g(), descs, supps);
            var fn = dir + "/" + id + ".owl";
            man.saveOntology(g().getSourceOntology(), owlFormat, IRI.create(new File(fn)));
            man.removeOntology(g().getSourceOntology());
            // pw.saveOWL(g().getSourceOntology(),fn);
            // TODO - release ontology
        }
    }
}


// reasoner ancestors
//  Example: rancs("limb").map( function(a) {return g().getLabel(a)} )
function rancs(label) {
    return reasoner.getSubClasses( get(label), true ).getFlattened().toArray();
}

// generic javascript prettyprint
//function pp(object) { 
//  return pp(object, 1, true);
//}

function pp(object, depth, embedded) { 
  typeof(depth) == "number" || (depth = 0)
  typeof(embedded) == "boolean" || (embedded = false)
  var newline = false
  var spacer = function(depth) { var spaces = ""; for (var i=0;i<depth;i++) { spaces += "  "}; return spaces }
  var pretty = ""
  if (      typeof(object) == "undefined" ) { pretty += "undefined" }
  else if ( typeof(object) == "boolean" || 
            typeof(object) == "number" ) {    pretty += object.toString() } 
  else if ( typeof(object) == "string" ) {    pretty += "\"" + object + "\"" } 
  else if (        object  == null) {         pretty += "null" } 
  else if ( object instanceof(Array) ) {
    if ( object.length > 0 ) {
      if (embedded) { newline = true }
      var content = ""
      for each (var item in object) { content += pp(item, depth+1) + ",\n" + spacer(depth+1) }
      content = content.replace(/,\n\s*$/, "").replace(/^\s*/,"")
      pretty += "[ " + content + "\n" + spacer(depth) + "]"
    } else { pretty += "[]" }
  } 
  else if (typeof(object) == "object") {
      if (object instanceof OWLObject) {
          pretty += jsRenderOWL(object);
      }
      else if (object instanceof java.lang.Object) {
          pretty += object;
      }      
      else if ( Object.keys(object).length > 0 ){
          if (embedded) { newline = true }
          var content = ""
          for (var key in object) { 
              content += spacer(depth + 1) + key.toString() + ": " + pp(object[key], depth+2, true) + ",\n" 
          }
          content = content.replace(/,\n\s*$/, "").replace(/^\s*/,"")
          pretty += "{ " + content + "\n" + spacer(depth) + "}"
      } 
      else { pretty += "{}"}
  }
    else { pretty += object.toString() }
    return ((newline ? "\n" + spacer(depth) : "") + pretty)
}

function jsRenderOWL(object, depth, embedded) {
    if (object instanceof OWLNamedObject) {
        return getClassVariableName(object);
    }
    else if (object instanceof OWLObjectSomeValuesFrom) {
        return "someValuesFrom(" + jsRenderOWL(object.getProperty()) +" , " + jsRenderOWL(object.getFiller())+") ";
    }
    else if (object instanceof OWLAnnotation) {
        return "ann(" + jsRenderOWL(object.getProperty()) +" , " + jsRenderOWL(object.getValue())+") ";
    }
    else if (object instanceof OWLObjectIntersectionOf) {
        var args = object.getOperandsAsList().toArray();
        var args2 = args.map(function(x){ return jsRenderOWL(x, depth, embedded)})
        return "intersectionOf(" + args2.join(", ") + ")";
    }
    return object;
}

// TODO - OO
function owl(obj) {
    // in future this may perform translation of json objects to OWL
    if (typeof obj == 'string') {
        // todo - other types
        obj = this.df().getOWLClass(IRI.create(obj));
    }
    return obj;
}

function showAxioms() {
    var owlpp = new OWLPrettyPrinter(g());  
    var axioms = ont().getAxioms().toArray();
    for (var k in axioms) {
        print(owlpp.render(axioms[k]));
    }
}

print(">>>> Welcome to the OWLTools Rhino Shell!");

