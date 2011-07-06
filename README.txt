OWLTools/OWLGraph/OWLSim/OboOntologyRelease

At this time, this library is intended to satisfy a number of different use cases, including:

* Simplified access to the OWLAPI for common operations involving annotation properties and inter-class connectivity

* A fast way to perform sound transitive closure over a large and complex ontology

* A framework for ontology-based data mining, statistics, semantic similarity and class enrichment

* Provides tools to build an ontology release

These capabilities may be split into separate APIs at some point.

USE CASES

* Testing for "direct connectivity" between classes

An ontology contains multiple axioms, including basic all-some restrictions:

[1] a SubClassOf part_of some b

as well as nested expressions:

[2] c SubClassOf part_of some (develops_from some d)

Informally, we consider these connected in the sense there is no intermediate named objects. We wish to [i] test for connectivity between
pairs of classes such as <a,b> and <c,d>, and [ii] determine the relationship between these pairs ("part_of some" and "part_of some develops_from some"
respectively).

Currently this is difficult with the OWLAPI.

*  Testing for "indirect connectivity" between classes

An ontology contains axioms such as

[1] a SubClassOf part_of some b
[2] b SubClassOf part_of some c
[3] TransitiveProperty(part_of)

If we want to find classes whose instances are all part_of c we can ask a DL reasoner for subclasses of "part_of some c", and get the
desired answer of {a,b} (assuming the ontology is of a size it can be reasoned over using a DL reasoner).

However, starting from a is more difficult. If we want to find all "part_of ancestors" of a (more formally:
all classes X such that "part_of X" subsumes a) then we must either explicitly test all such class expressions,
or exhaustively name the set of "part_of X" class expressions prior to reasoning.

This is even more difficult if we want to find "all ancestors" of a.

* Testing for LCSs between classes (incl. class expressions)

Semantic similarity requires finding the LCS (Least Common Subsumer) of two classes. For optimal results we
want to include class expressions such that if we have

big_ear = ear and big
big_eye = eye and big
ear SubClassOf organ
eye SubClassOf organ

then

LCS(big_ear,big_eye) = organ and big

For optimal results we need to generate class expressions in the set of ancestors (see previous use case).

big_ear_human = human and has_part some big_ear
big_eye_sheep = sheep and has_part some big_eye
LCS = mammal and has_part some (organ and big)

(See email to OWL discussion list)

* Semantic similarity

given instance data such as

human1 facts: human and has_part some big_ear and has_part some small_eye and ...
human2 facts: human and ...

we want to determine the semantic similarity between these individuals based on their properties in common.
We can use a variety of metrics - jacard similarity, maximum information content of LCS, average IC
of all elements of LCS as considered independent....

* Obo Ontology Release Manager

It is an ant based command line tool which produces ontologies versions releases. 
The command 'bin/ontology-release-runner' builds an ontology release. This tool is supposed to be run 
from the location where a particular ontology release are to be maintained. 
In the process of producing a particular release this tool labels the release with a auto generated version id. 
The version id is maintained in the VERSION-INFO file. All files produced for a particular release are assembled in the directory of name
by the date of the release. Run the 'bin/ontology-release-runner --h' tool with the --h option to get help which parameter to pass the tool.

