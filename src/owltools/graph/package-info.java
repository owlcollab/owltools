/**
<h2>OWLGraph -- OBO-like Graph Wrapper for OWL Ontologies</h2>

This class provides a facade onto one or more {@link
org.semanticweb.owlapi.model.OWLOntology} objects.

<h2 id="model">Basic Model</h2>

<p>

An OGW consists of a <b>source ontology</b>, and zero or more
<b>support ontologies</b>. The support ontologies are generally not
required to be connected to the source ontology via imports.

  </p>

<p>
See the core {@link owltools.graph.OWLGraphWrapper} object
</p>

<h2 id="metadata">Ontology Metadata</h2>

<p>

An OGW provides convenience methods for looking up entities by their
label, querying for synonyms, text definitions, OBO subsets, etc.

</p>

<p>

It is assumed that the ontology makes use of the vocabulary specified
in the official obo-owl mapping (i.e. a combination of IAO plus
addition custom vocabulary elements).

</p>


<h2 id="graph">Graph Operations</h2>

<h3>Background</h3>

<p>
The OWL API provides methods for querying and processing the axioms
and expressions in an OWL ontology. The OWL API also provides a
unified programmatic layer for accessing a variety of OWL reasoners
(Elk, FaCT++, HermiT and Pellet).
</p>

<p>
Sometimes it can be useful to view an ontology as a <i>graph</i>
rather than a collection of axioms. This allows for simpler processing
of <i>reachability queries</i>.  For example, given an OWL object such
as the class <i>hippocampus CA4</i> in an anatomy ontology (ma.owl),
we might want to know which classes are reachable from here, and what
is the relationship between these classes. Answers might include "part
of a brain" and "develops from part of a neural tube". OWL reasoners
can have unpredictable performance, and will only return named
superclasses.
</p>

<p>
This API provides a graph-like (or "OBO-format-like") view over an OWL
ontology or collection of OWL ontologies, and provides scalable and
valid reachability query answering.
</p>


       <h3>OWLGraph Formalism</h3>
       <p>
An OWLOntology <i>O</i> includes a set of axioms
<i>A</i>. The <i>signature</i> of an ontology is a set of named
entities and expressions <i>N</i>, and is equivalent to the union of
the signature of all axioms in the ontology.  <i>N</i> can be
partitioned into <i>named entities</i> and <i>anonymous expressions</i> -- the
latter are structures formed recursively from OWL constructs and named
entities. We omit literals here, but the formalism could be extended
to cover them.
       </p>

       <p>
We introduce the notion of an <i>OWLGraph</i>: this is a set of
edges <i>E</i> connecting the members of <i>N</i>, which are viewed as
nodes in the graph. Each edge in E is a tuples consisting of a source,
a target and an <i>edge label</i>. An edge label specifies how the
source is connected to the target, and is a composite structure
consisting of zero or more <i>quantified relations</i> and a distance:

<pre>
<code>
<b>E</b> = 'Edge(' <b>SourceNode</b> <b>QR</b>* <b>Distance</b> <b>Target</b> ')'
</code>
</pre>

       </p>

       <p>
A <i>Quantified Relation</i> is either a property-predicate pair, or a builtin predicate:

<pre>
<code>
<b>QR</b> =  <b>Property</b> 'some' |  <b>Property</b> 'all' |  <b>Property</b> 'value' |  <b>Property</b> Cardinality(<b>Int</b>,<b>Int</b>) | 'SUBCLASS_OF' | 'INSTANCE_OF' | 'IDENTICAL_TO'
</code>
</pre>
       </p>

       </p>
Note the edges in an OWLGraph are considerably different from the
triples of a RDF graph, in which edge labels
are <i>atomic</i>properties, as opposed to composite structures. An
OWLGraph does not add expressivity over either RDF or OWL abstract
syntax representation, but allows for simpler specification of
traversal operations.
       </p>

<h3>Construction of an OWLGraph from an OWLOntology</h3>

       <p>
The set of graph edges <i>E</i> is constructed using both the set of
axioms <i>A</i>, and the set of class expressions referenced
in <i>A</i>. Recall that this is a subset of <i>N</i>.
       </p>


<h4>Seeding from OWL Axioms</h4>

       <p>
The following two rules produce edges from the set of axioms A in O:
<ul>
<li>
<code>
SubClassOf(x y) ==> Edge(x,SUBCLASS_OF,y)
</code>
<li>
<code>
EquivalentClasses(x y) ==> Edge(x,SUBCLASS_OF,y), Edge(y,SUBCLASS_OF,x)
</code>
</ul>
       </p>

<h4>Seeding from OWL Expressions</h4>

       <p>
The following rules produces edges from the set of class expressions referenced in the signature of O:

<ul>
<li>
<code>
ObjectSomeValuesFrom(p x) ==> Edge( ObjectSomeValuesFrom(p x), p some, x)
</code>
<li>
<code>
ObjectOnlyValuesFrom(p x) ==> Edge( ObjectOnlyValuesFrom(p x), p only, x)
</code>
<li>
<code>
IntersectionOf(x1 x2 ... xn) ==> Edge( IntersectionOf(x1 x2 ... xn), SUBCLASS_OF, xi), for all xi in x1..xn
</code>
<li>
<code>
UnionOf(x1 x2 ... xn) ==> Edge( xn, SUBCLASS_OF, UnionOf(x1 x2 ... xn)), for all xi in x1..xn
</code>
</ul>

       </p>

       <p>
<h4>Examples</h4>

       <p>
Given:

<pre>
<code>
SubClassOf(nucleus ObjectSomeValuesFrom(partOf cell))
SubClassOf(nucleolus ObjectSomeValuesFrom(partOf nucleus))
EquivalentClasses(nuclear_chromosome IntersectionOf(chromosome ObjectSomeValuesFrom(partOf nucleus)))
</code>
</pre>

The following edges are generated:

<pre>
<code>
Edge(nucleus,                                                         SUBCLASS_OF, ObjectSomeValuesFrom(partOf cell))
Edge(objectSomeValuesFrom(partOf cell),                               partOf some, cell)
Edge(nucleolus,                                                       SUBCLASS_OF, ObjectSomeValuesFrom(partOf nucleus))
Edge(objectSomeValuesFrom(partOf nucleus),                            partOf some, nucleus)
Edge(nuclear_chrosome,                                                SUBCLASS_OF, IntersectionOf(chromosome ObjectSomeValuesFrom(partOf nucleus)))
Edge(IntersectionOf(chromosome ObjectSomeValuesFrom(partOf nucleus)), SUBCLASS_OF, nuclear_chrosome)
Edge(IntersectionOf(chromosome ObjectSomeValuesFrom(partOf nucleus)), SUBCLASS_OF, chromosome)
Edge(IntersectionOf(chromosome ObjectSomeValuesFrom(partOf nucleus)), SUBCLASS_OF, ObjectSomeValuesFrom(partOf nucleus))
</code>
</pre>
       </p>



<h3>Graph Closure</h3>

       <p>
The graph closure is the result of exhaustively applying the <i>expansion rules</i>:
<code>
Edge(x r1 r2 ... rn Dxy y), Edge(y rn+1 rn+2 ... rm  Dyz z) ==> Edge(x r1 .. rm Dxy+Dyz z)
</code>

Together with reduction rules that compact a list of quantified relations using a <i>composition table</i>:
       </p>

       <p>
<pre>
<code>
SUBCLASS_OF o SUBCLASS_OF         &rarr; SUBCLASS_OF
SUBCLASS_OF o P some              &rarr; P some
P some      o SUBCLASS_OF         &rarr; P some
SUBCLASS_OF o P only              &rarr; P only
P only      o SUBCLASS_OF         &rarr; P only
P1 some     o P2 some             &rarr; P some  <i>on condition:</i> TransitiveProperty(P) and subPropertyOf(P1 P) and subPropertyOf(P2 P)[*]
P1 some o P2 some o ... o Pn some &rarr; P some  <i>on condition:</i> subObjectPropertyOf( PropertyChain(P1 .. Pn) P)[**]

[*] assumes inferred subPropertyOf has been calculated - recall this is reflexive.
    if this condition is true for multiple values of the property P, the most specific property is chosen.
[**] again, RBox inference is used here.
</code>
</pre>

The table operates on any two consecutive QRs and reduces it to a single QR.

        </p>

       <p>
The above operations will produce <i>valid</i> results, but is not
guaranteed to be <i>complete</i>. For optimal results the ontology
should be <i>classified in advance</i> using standard OWL reasoning. 
        </p>

       <p>
The resulting edges correspond to <i>linear class expressions</i>, and
can be translated to OWL using a reversal of the seeding rules.
        </p>

<h4>Example</h4>

       <p>
Given the edges:

<pre>
<code>
Edge(nucleus,                              SUBCLASS_OF, ObjectSomeValuesFrom(partOf cell))
Edge(objectSomeValuesFrom(partOf cell),    partOf some, cell)
Edge(nucleolus,                            SUBCLASS_OF, ObjectSomeValuesFrom(partOf nucleus))
Edge(objectSomeValuesFrom(partOf nucleus), partOf some, nucleus)
</code>
</pre>

and the following axiom:

<pre>
<code>
TransitiveObjectProperty(partOf)
</code>
</pre>

The closure will be:

<pre>
<code>
Edge(nucleus,   SUBCLASS_OF partOf some, cell)
Edge(nucleolus, SUBCLASS_OF partOf some, nucleus)
Edge(nucleolus, SUBCLASS_OF partOf some SUBCLASS_OF partOf some, cell)
</code>
</pre>

(only edges between named entities are shown).
</p>

<p>
Using reduction rules / the composition table:

<pre>
<code>
Edge(nucleus,   partOf some, cell)
Edge(nucleolus, partOf some, nucleus)
Edge(nucleolus, partOf some, cell)
</code>
</pre>

       </p>

<h3>Graph Traversal Algrorithm</h3>

       <p>
Given a subject node s, we can efficiently find the set of edges with
all <i>reachable nodes</i> as targets:
<pre>
<code>
E = {}    -- returned edges
X = { Edge(s IDENTICAL_TO 0 s) }   -- scheduled edges; seed with reflexive edge
V = {} -- visited edges
while |X| > 0 :
  x = pop X
  if (x in V) :
    En = { x join e : e.s == x.t}
    V = V u En
    X = X u En
    Ex = { x join e : e in En }
    E = E u Ex
</code>
</pre>
Here the join operation joins two edges by concatenating QRs and applying the composition table above.
       </p>

    
       <p>
The algorithm is safe in the presence of cycles. The results will be
valid according to OWL semantics, but they may not be complete. We can
see intuitively that the time or space requirements of the algorithm
are not affected by the overall size of the ontology, only by the
number of reachable nodes (this is an attractive feature when working
with unions of large ontologies such as the FMA, where we only want to
use a subset of FMA axioms for any given query).

       </p>

       <p>
Other algorithms can be used - e.g. rule-based deduction - but this
will not have the same computational properties. 
       </p>

 */
package owltools.graph;

import owltools.graph.OWLGraphWrapper;
import owltools.gfx.OWLGraphLayoutRenderer;
