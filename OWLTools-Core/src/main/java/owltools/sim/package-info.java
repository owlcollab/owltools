/**
   <h2>OWLSim -- Semantic Similarity over OWL ontologies<h2>
   
   <h3>Synopsis</h3>
   <h4>Command Line Use</h4>
   <pre>
   <code>
owltools file:test_resources/pizza.owl --sim-method DescriptionTreeSimilarity --sim http://owl.cs.manchester.ac.uk/2009/07/sssw/pizza#MeatFishAndVegetarianPizza-Closed http://owl.cs.manchester.ac.uk/2009/07/sssw/pizza#VegetarianToppingsPizza-Closed
   </code>
   </pre>

   <h3>Introduction</h3>

<p>
One drawback with most existing bioinformatics ontology-based
similarity analyses is the lack of OWL semantics. For example, most
existing semantic similarity analyses treat the ontology as a simple
graph. The subset of analyses that incorporate edge labels do so in
ad-hoc ways, rather than taking advantage of the full semantics of the
relationship.
</p>

<p>
These analyses are also dependent on all potential useful grouping
classes being specified in advance, in a pre-coordinated
ontology. This results in a lack of precision, as it is all but
impossible to anticipate all potential grouping classes in advance.
</p>

<p>
Here we define a small number of methods for scoring similarity of two
entities based on shared properties. The methods are completely
neutral w.r.t the nature of these entities - they could be two
individuals, two classes, or an individual and a class. They can be
any type of individual or class, and can be compared with respect to
any attribute. For example, organisms can be compared by phenotype or
pizzas can be compared by toppings. The only requirement is that all
the information is specified in OWL. The more heavily axiomatized the
OWL is, the more informative the results will be.
</p>

<p>
The primary innovation is the use of a novel algorithm for computing
the least common subsuming description. Unlike naive LCA computations,
this also includes <i>anonymous</i> classes in the result. These are
constructed on the fly. This has utility in a number of situations;
for example, when comparing two complex descriptions, one using a
human anatomy ontology and another using a zebrafish anatomy ontology,
if we include a bridging ontology such as uberon then novel
descriptions are generated that subsume both.
</p>

   <h3>Definitions</h3>

<h4>Common Ancestors and Common Subsumers</h4>

<h5>Subsumed by</h5>
<p>

<i>A</i> <b>subsumed by</b> <i>B</i> if <code>SubClassOf(A
B)</code>. This can be either asserted or inferred. Subsumption is
transitive, reflexive and anti-symmetric. The inverses relation is
<b>subsumes</b>.

</p>

<h5>Descendant of</h5>
<p>

<i>A</i> <b>is a descendant of</b> <i>B</i> if there is an OWL Graph
Edge in which A is the source and B is the target. See {@link
owltools.graph} for a description of OWL Graphs. The inverse relation
is <b>is ancestor of</b>. Descent is a super-property of subsumption
(i.e. if A subsumed by B, the A is a descendant of B).

</p>

<h5>Common Subsumers</h5>
<p>

<i>A</i> <b>is a common subsumer of</b> <i>B</i> and <i>C</i> if
<i>A</i> <b>subsumes</b> <i>B</i> and <i>A</i> <b>subsumes</b> <i>C</i>

</p>

<p> As we count owl:Thing in the universe of objects, if B and C are
satisfiable then it follows that they must have at least one CS,
owl:Thing </p>

<h5>Least Common Subsumers</h5>
<p>

<i>A</i> <b>is a Least Common Subsumer (LCS) of</b> <i>B</i> and <i>C</i> if:
<ul>
<li><i>A</i> <b>is a Common Subsumer of</b> <i>B</i> and <i>C</i>, and
<li>There is no <i>A'</i> such that:
<ul>
<li> <i>A'</i> <b>is a Common Subsumer of</b> <i>B</i> and <i>C</i>, and
<li> <i>A'</i> <b>is subsumed by</b> <i>A</i>, and
<li> <i>A</i> <b>is NOT subsumed by</b> <i>A'</i>, and
</ul>
</ul>

note that the reflexivity of the subsumption relation necessitates the
final clause to ensure that equivalent objects are included in the LCS set.

</p>


<h5>Common Ancestors</h5>

<p>

<i>A</i> <b>is a common ancestor of</b> <i>B</i> and <i>C</i> if
<i>A</i> <b>is a ancestor of</b> <i>B</i> and <i>A</i> <b>is a
ancestor of</b> <i>C</i>

</p>

<h5>Least Common Ancestors</h5>
<p>

<i>A</i> <b>is a Least Common Ancestor (LCP) of</b> <i>B</i> and <i>C</i> if:
<ul>
<li><i>A</i> <b>is a Common Ancestor of</b> <i>B</i> and <i>C</i>, and
<li>There is no <i>A'</i> such that:
<ul>
<li> <i>A'</i> <b>is a Common Ancestor of</b> <i>B</i> and <i>C</i>, and
<li> <i>A'</i> <b>is an ancestor of</b> <i>A</i>, and
<li> <i>A</i> <b>is NOT an ancestor by</b> <i>A'</i>, and
</ul>
</ul>

(we use P rather than A, as we reserve A for "anonymous", see below)
</p>
<p>
note that the lack of anti-symmetric characteristic on the ancestor
relation necessitates the final clause to ensure that objects related
via cycles are included in the LCS set. For example:

<pre>
<code>
SubClassOf(wheel SomeValuesFrom(partOf car))
SubClassOf(car SomeValuesFrom(hasPart wheel))
SubClassOf(audi car)
SubClassOf(bmw car)

P(audi) = { audi, car, wheel}
P(bmw) = { bmw, car, wheel}
LCP(audi, bmw) = { car, wheel }
</code>
</pre>

</p>



<h5>Named Objects and Named Subsumers</h5>

<p> <i>A</i> <b>is a named subsumer of</b> <i>B</i> if A is a subsumer
of B, and A is in the set of named objects.</p>

<p>
Example:

<pre>
<code>
SubClassOf(car SomeValuesFrom(hasPart wheel))
SubClassOf(audi car)

S<sup>Named</sup>(audi) = { audi, car, owl:Thing}
</code>
</pre>
</p>



<h5>Anonymous Expression Subsumers</h5>

<p>

<i>A</i> <b>is an anonymous subsumer of</b> <i>B</i> if A is a
subsumer of B, and A is in the set of class expressions.

</p>

<p> With many ontologies, the number of anonymous subsumers will be
larger than the set of named subsumers, because there are many
combinations of expressions that can be made that subsume the source
object.
</p>

<p>
Example:

<pre>
<code>
SubClassOf(car SomeValuesFrom(hasPart wheel))
SubClassOf(wheel SomeValuesFrom(hasQuality round))
SubClassOf(audi car)

S<sup>Anon</sup>(audi) = { 
  SomeValuesFrom(hasPart,wheel)
  SomeValuesFrom(hasPart,owl:Thing)
  SomeValuesFrom(hasPart,SomeValuesFrom(hasQuality,round))
  SomeValuesFrom(hasPart,SomeValuesFrom(hasQuality,Thing))
}
</code>
</pre>
</p>

<h5>Referenced Entities</h5>

<p>
<i>X</i> is a referenced entity if it is in the signature of the ontology, or set of ontologies. All named objected are referenced

<code>
SubClassOf(wheel SomeValuesFrom(partOf car))
SubClassOf(car vehicle)
ObjectProperty(partOf)

R = { partOf car vehicle wheel SomeValuesFrom(partOf car) }
</code>
</pre>

Note that in the above example, <code>SomeValuesFrom(partOf
vehicle)</code> is <b>not</b> in the set of referenced entities, even
though it is a satisfiable class expression.

</p>


<h5>Common Referenced Ancestors</h5>

<p>

<i>A</i> <b>is a Common Referenced Ancestor (CRP) of</b> <i>B</i> and <i>C</i> if:
<ul>
<li><i>A</i> <b>is a Least Common Ancestor (CA) of</b> <i>B</i> and <i>C</i>
<li>A is <b>referenced</b> in the set of ontologies
</ul>

<pre>
<code>
SubClassOf(car SomeValuesFrom(hasPart wheel))
SubClassOf(wheel SomeValuesFrom(hasQuality round))
SubClassOf(audi car)
SubClassOf(bmw car)

CRP(audi bmw) = { car wheel round SomeValuesFrom(hasPart wheel) SomeValuesFrom(hasQuality round) }
</code>
</pre>
</p>

<h5>Least Common Named Subsumers</h5>
<p>

<i>A</i> <b>is a Least Common Named Subsumer (LCNS) of</b> <i>B</i> and <i>C</i> if:
<ul>
<li><i>A</i> <b>is a Least Common Subsumer (LCNS) of</b> <i>B</i> and <i>C</i>, and
<li><i>A</i> is a named object
</ul>

</p>

<p>
if <code>A &isin; LCNS(B,C)</code> then <code>A &isin; LCS(B,C)</code>
</p>

<p>
Note that calculation of the LCNS is relatively efficient, as the set is derived from the universe of named objects
</p>

<h5>Least Common Anonymous Subsumers</h5>
<p>

<i>A</i> <b>is a Least Common Anonymous Subsumer (LCAS) of</b> <i>B</i> and <i>C</i> if:
<ul>
<li><i>A</i> <b>is a Least Common Subsumer (LCNS) of</b> <i>B</i> and <i>C</i>, and
<li><i>A</i> is an anonymous class expression.
</ul>

</p>

<p>
if <code>A &isin; LCAS(B,C)</code> then <code>A &isin; LCS(B,C)</code> -- ie LCS includes anonymous classes
</p>

<p>
Calculation of LCAS is a non-trivial problem, because the universe of possible anonymous class expressions is very large and under certain models, infinite.
</p>

<p>
For any two classes B and C, under OWL-DL there is always at least one trivial LCAS: <code>UnionOf(B C)</code>
</p>

<h5>Least Common Meaningful Anonymous Subsumers</h5>

<p>

<i>A</i> <b>is a Least Common Meaningful Subsumer (LCMS) of</b> <i>B</i> and <i>C</i> if:
<ul>
<li><i>A</i> <b>is a Least Common Subsumer (LCS) of</b> <i>B</i> and <i>C</i>, and
<li><i>A</i> does not reduce to a union expression between B and C
</ul>

<h5>Example</h5>

<p>
<pre>
<code>
EquivalentClasses(car IntersectionOf(vehicle motorized ObjectExactCardinality(hasPart car_wheel 4)))
EquivalentClasses(bicycle IntersectionOf(vehicle ComplementOf(motorized) ObjectExactCardinality(hasPart bicycle_wheel 2)))

SubClassOf(car_wheel wheel)
SubClassOf(bike_wheel wheel)

# subsumers:
LCMAS(car,bicycle) = { IntersectionOf(vehicle ObjectMinCardinality(hasPart wheel 2) ObjectMaxCardinality(hasPart wheel 4) } 
LCAS(car,bicycle) = LCMAS &cup; { UnionOf(car bicycle) }
LCMS(car,bicycle) = LCMAS &cup;
LCS(car,bicycle) = LCAS
LCNS(car,bicycle) = { vehicle }
LCNP(car,bicycle) = { vehicle, wheel }
LCRP(car,bicycle) = { vehicle, wheel }
</code>
</pre>

The LCMAS is the most informative of the measures, as it contains the most specific non-trivial description of what is shared
between cars and bicycles based on the knowledge available (they are vehicles with between 2 and 4 wheels)

</p>

<p>
(note: def of M needs modified to eliminate the description "vehicle with 2 wheels or with 4 wheels" which has a smaller number of members, but does not generalize as well)
</p>

<p>

If we add an axiom:

<pre>
<code>
SubClassOf(vehicle SomeValuesFrom(hasPart wheel))
</code>
</pre>

Then this adds a reference to a new class expression changes the results as follows:

<pre>
<code>
LCNS(car,bicycle) = { vehicle }
LCRS(car,bicycle) = { vehicle SomeValuesFrom(hasPart wheel) }
LCNP(car,bicycle) = { vehicle, wheel }
LCRP(car,bicycle) = { vehicle, SomeValuesFrom(hasPart wheel) }

</code>
</pre>


<h3>Algorithms</h3>
<h4>Calculation of Common Referenced Ancestors (CRP and LCRP)</h4>

<p>
<pre>
<code>
CRP(a,b) :
  P = {}
  for x in ancestor_of(a)
    add x to P
  for x in ancestor_of(b)
    add x to P
  RETURN P
 </code>
 </pre>

<pre>
<code>
LCRP(a,b) :
  P = CRP(a,b)
  for x in P:
    IF (exists y in P AND
       x ancestor_of y AND
       NOT(y ancestor_of x))
    THEN remove x from P
  RETURN P
 </code>
 </pre>
</p>

<h4>Calculation of LCS (including LCAS)</h4>

<p>
This algorithm finds an approximation of the meaningful least common
subsumers (including both anonymous and named). Recall that using
standard LCA algorithms will only give us the least
common <i>referenced</i> subsumer, which misses informative answers
from the much wider set of possible anonymous expressions.
</p>

<p>
The basic idea can be informally sketched as follows. The descriptions
of a and b can be conceived of as <i>description trees</i>, with each
branchpoint representing set of sub-expressions join by
the <b>IntersectionOf</b> construct. For example, the description
<i>motorized vehicle with 4 red wheels</i> is a tree with 4 leaves and
3 branch points:

<ul>
<li>IntersectionOf(vehicle motorized ExactCardinality(hasPart red_wheel 4)
<ul>
 <li>vehicle</li>
 <li>motorized</li>
 <li>
  ExactCardinality(hasPart red_wheel 4)
  <ul>
   <li>wheel</li>
   <li>SomeValuesFrom(hasColor red)
    <ul>
     <li>red</li>
    </ul>
   </li>
  </ul>
 </li>
</ul>
</li>
</ul>

If we include axioms from the ontology and add edges derived using the
OWL Graph traversal algorithm, we end up with a much bushier tree. For
example, <i>vehicle</i> might no longer be a leaf node, if we include
a SUBCLASS edge to <i>manufactured object</i>. Note that here we are
reversing the normal terminology of trees when applied to ontologies -
our root is the initial description and the leaves are ancestors.
</p>

<p>

We attempt to find a maximally informative tree that subsumes two
given trees by aligning the branchpoints. We start at the roots of
both trees, and traverse downwards, finding the best match for each
node in the opposing tree. We calculate the LCRP at each point; if
there are multiple non-redundant ancestors then we create an
IntersectionOf expression. Where there is one, we use the LCRP.

</p>

<pre>
<code>
LCS(a,b) :
  X = {}
  EA = all edges emanating from a
  for ea in EA:

    # find best matching edge for ea
    eb = null
    score = 0
    for bx in LCRP(b):
      for eb' in findEdge(b,bx)
        score' = scoreMatch(ea,eb')
        if score' > score:
          score = score'
          eb = eb'

    x = mkUnionExpression(ea, eb, LCS(ea.tgt. eb.tgt))
    add x to X

  # combine
  if |X| = 0 : return owl:Thing
         = 1 : return only element of X
         > 1 : return IntersectionOf(X)
 </code>
 </pre>

<h4>Examples</h4>

<p>

Given the following ontology:

<pre>
<code>
Ontology(test)
axon_terminals_degenerated_in_CA4 EquivalentTo phenotypeOf some hasPart some (CA4 and hasPart some (axon_terminal and hasQuality some degenerated))
dendrites_degenerated_in_CA3      EquivalentTo phenotypeOf some hasPart some (CA3 and hasPart some (dendrite and hasQuality some degenerated))
axon_terminal SubClassOf cell_process and partOf some neuron
dendrite SubClassOf cell_process and partOf some neuron
CA4 SubClassOf partOf some hippocampus
CA3 SubClassOf partOf some hippocampus
org1 instanceOf human and hasPhenotype some axon_terminals_degenerated_in_CA4
org2 instanceOf human and hasPhenotype some dendrites_degenerated_in_CA3
 </code>
 </pre>

The answer to <code>LCS(org1,org2)</code> is:

<code>
human and hasPhenotype some (partOf some hippocampus) and hasPart some ((cell_process and partOf some neuron) and hasQuality some degenerated)
</code>


<h3>Similarity Measures</h3>

<h4>Jaccard Similarity</h4>

<p>

Jaccard Similarity is the number of common attributes divided by the
union of attributes. We define SimJ-RP using the set of referenced
ancestors:

<pre>
<code>
SimJ-RP(x,y) :
  return | CRP(x,y) | / | RP(x) &cup; RP(y) |
</code>
</pre>

One limitation of this technique is that it does not take into account
anonymous expressions.

</p>

<p>

We do not want to perform SimJ over all anonymous expressions, as
there are potentially massive numbers of such expressions. Instead we
can first compute the LCS (which includes anonymous expressions) using
the above algorith, and then calculate the SimJ using only referenced
ancestors.

<pre>
<code>
SimJ-LCS-RP(x,y) :
  z = LCS(x,y)
  return | RP(z) | / | RP(x) &cup; RP(y) |
</code>
</pre>

We illustrate these using the following ontology:

<pre>
<code>
EquivalentClasses(car IntersectionOf(vehicle motorized ObjectExactCardinality(hasPart car_wheel 4)))
EquivalentClasses(bicycle IntersectionOf(vehicle ComplementOf(motorized) ObjectExactCardinality(hasPart bicycle_wheel 2)))

SubClassOf(car_wheel wheel)
SubClassOf(bike_wheel wheel)

# subsumers:
SimJ-RP(car,bicycle) =
  | {vehicle, wheel |
  -------------------
  | { car, bicycle, vehicle, bicycle_wheel, car_wheel, wheel, motorized, complementOf(motorized),
      IntersectionOf(vehicle motorized ObjectExactCardinality(hasPart car_wheel 4)), 
      ObjectExactCardinality(hasPart car_wheel 4),
      IntersectionOf(vehicle ComplementOf(motorized) ObjectExactCardinality(hasPart bicycle_wheel 2))
      ObjectExactCardinality(hasPart bicycle_wheel 2)
       |
  = 1/6
SimJ-LCS-RP(car,bicycle) =
  | {vehicle, wheel, IntersectionOf(vehicle ObjectCardinalityBetween(hasPart wheel 2 4)) ObjectCardinalityBetween(hasPart wheel 2 4) |
  -------------------
  | ... |
  = 1/3
</code>
</pre>
</p>

<h4>Information Content</h4>

<h4>Average Best Match</h4>

<p>
The methods above work for any OWL individuals or classes - they are
completely generic. We can achieve higher precision if we compare with
respect to a particular attribute. The choice of attribute is
application-dependent. For example, with phenotype comparison, we may
wish to compare two entities with respect to the hasPhenotype object
property. We could specify this using a DL-like query <code>atts.x = {
p : p &isin; SomeValuesFrom(phenotypeOf x) }</code>
</p>

<p>

The Average Best Match is the average of all best matches between each
attribute in x and the set of attributes in y, averaged for both x to
y and y to x.

<pre>
<code>
ABM(x,y) :
  score = 0;
  n = 0;
  for a in x.atts:
    score += bestMatch(a, y.atts)
    n++
  for a in y.atts:
    score += bestMatch(a, x.atts)
    n++
  RETURN score / n

bestMatch(a1, atts) :
  score = 0;
  for a2 in atts:
    score' = getScore(a1,a2)
    score = max(score, score')
  return score
 </code>
 </pre>

ABM can be used with a number of different scoring schemes to score
attribute pairs. For example, ABM with Jaccard similarity of the LCS
would be called ABM-J-LCS.

<pre>
<code>
SimJ-LCS(x,y) :
  z = LCS(x,y)
  return | LCRP(z) | / | RP(x) &cup; RP(y) |
</code>
</pre>



</p>

<h3>Examples</h3>

<h4>OMIM</h4>

   <pre>
   <code>
owltools file:imports.owl --sim-method MultiSimilarity --sim -p http://purl.obolibrary.org/obo/RO_0002200 http://purl.obolibrary.org/obo/MGI_101759 http://purl.obolibrary.org/obo/MGI_101761
   </code>
   </pre>

   <pre>
   <code>
owltools file:imports.owl --sim-method MultiSimilarity --sim -p 'has phenotype' http://purl.obolibrary.org/obo/MGI_101759 http://purl.obolibrary.org/obo/MGI_101761
   </code>
   </pre>




@author cjm

 */
package owltools.sim;

import owltools.cli.CommandLineInterface;
