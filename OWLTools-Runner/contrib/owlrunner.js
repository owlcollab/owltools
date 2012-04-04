importPackage(java.io);
importPackage(Packages.org.semanticweb.owlapi.model);
importPackage(Packages.org.semanticweb.owlapi.io);
importPackage(Packages.owltools.cli);
importPackage(Packages.owltools.io);
importPackage(Packages.owltools.mooncat);
importPackage(Packages.org.obolibrary.macro);
importPackage(Packages.com.google.gson);

var runner;
var reasoner;
var mst;

// initializes runner with new CommandRunner
function init() {
    runner = new SimCommandRunner();
    runner.exitOnException = false;
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

// initializes a reasoner. E.g. reason("elk")
function reason(rn) {
    runner.run(["--reasoner",rn]);
    reasoner = runner.g.getReasoner();
}

// shortcuts
function elk() {
    reason("elk");
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

function parseManx(String expr) {
    mst = new ManchesterSyntaxTool(g().getSourceOntology(), g().getSupportOntologySet(), true);
    return mst.parseManchesterExpression(expr);
}

function dlq(String expr) {
    var x = parseManc(expr);
    
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

function getLabel(obj) {
    return g.getLabel(obj);
}

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
function pp(object) { 
  return pp(object, 1, true);
}

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
    if ( Object.keys(object).length > 0 ){
      if (embedded) { newline = true }
      var content = ""
      for (var key in object) { 
        content += spacer(depth + 1) + key.toString() + ": " + pp(object[key], depth+2, true) + ",\n" 
      }
      content = content.replace(/,\n\s*$/, "").replace(/^\s*/,"")
      pretty += "{ " + content + "\n" + spacer(depth) + "}"
    } else { pretty += "{}"}
  }
  else { pretty += object.toString() }
  return ((newline ? "\n" + spacer(depth) : "") + pretty)
}


print(">>>> Welcome to the OWLTools Rhino Shell!");
