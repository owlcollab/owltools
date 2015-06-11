Background notes to background notes: this rough outline may well be
misguided in attempting to marry requirements for an obo-like
replacement syntax for obo/owl with requirements for machine exchange
(particular JSON/web apps).

## Background

Notes for a standard YAML/JSON representation for OWL.

Uses include exchange, representation of ontology source in VCSs (a
standard ordering should be defined), APIs.

This document uses YAML as examples, but only the JSON subset of YAML
is used. Everything should be suitable for programmatic exchange
without requiring a YAML parser or producer.

This should not be taken as anywhere near a standard, normative
informative or otherwise.

### Frame-vs-Axiom style

There are standard dichotomies in RDF/OWL serializations, such as
Frame-style or Axiom-style. These are semantically equivalent, but
each may be suited to different purposes (both ease of machine
processing and human processing).

Frame style feels familiar to OO programmers. It's also convenient for
most people who think in terms of 'things'. However, frame-style can
become awkward. Some relationships don't belong to any one
'thing'. Also the typical programmatic representation of relationships
(e.g. as accessor variables in a java class or keys in a dictionary in
JSON/YAML) are convenient for simple cases, but may require
reification when we want to talk about the relationship.

Of course, Axiom-stye can be seen in an OO sense in that things are
just shifted up a level: the axioms are the objects. 

### Schema-style: Flexible vs Controlled

Here we introduce an additional dichotomy, between for want of better
terms schema-style or controlled.

With flexible-schema-style, the keys in the YAML/JSON are drawn from domain
vocabularies; with Controlled, the keys are from a limited set of
fixed constructs, typically drawn from the OWL vocabulary.

Example of flexible-schema-style:

```
 - id: john
   a: person
   owns:
     id: rufus
     a: dog
```

Example of controlled-style:

```
 - id: john
   a: person
   facts:
     property: owns
     value:
       id: rufus
       a: dog
```

JSON-LD is an example of a flexible-schema-style. This is attractive
in some scenarios. The objects are intuitive to traverse if one knows
the schema/ontology/properties in advance. However, it is less suited
to generic processing.

Note the flexible-schema-style works best when some JSON-LD-context
like mechanism contracts the vocabulary URIs. This would obviously be
a nightmare:

```
 - id: john
   a: person
   http://my.vocabulary.org/terms/REL_0000023:
     id: rufus
     a: dog
```

The two styles can be mixed to some extent. For example, some key
vocabulary terms in a fixed style may be given special status: label,
for example.

The remainded of this doc assumes flexible-schema-style, but ideas
could be mined for a controlled-style. However, it seems if going for
controlled style an 

## Prefixes and Context

Every document starts with a context. This is the YAML translation of a JSON-LD context, and can be used to specify prefixes:

For example:

```
context:
  obo: http://purl.obolibrary.org/obo/
  BFO: obo:BFO_
  FOO: obo:FOO_
  part_of: BFO:0000050
```

Certain prefixes are always assumed: owl, rdf, xsd, etc. In addition the following prefixes expand to the relevant URI:

 * label
 * comment

## Ontology list

Every document has an ontology *list* - this is in contrast to existing formats which assume one ontology per document (prohiniting bundling an 'archive' into one document)

Each ontology object may have an *id* (ontologyIRI), *version* (versionIRI), *imports* list, *Annotations* and object and axiom list.

```
ontologies:
  - id: obo:foo.owl
    imports: [obo:foo/imports/bar.owl, obo:foo/imports/foz.owl]
    Annotations:
      title: demo ontology
    objects:
       ...
    axioms:
       ...
```

Canonical ordering: TBD

## Objects

Example:

```
    objects:
      - a: owl:Class
        id: FOO:0000001
        label: first class
      - a: owl:Class
        id: FOO:0000002
        label: second class
```

Canonical ordering: sorted by IRI

### Logical Axioms

#### SubClassOf

to say FOO:1 is a subclass of FOO:2

```
      - a: owl:Class
        id: FOO:0000001
        label: first class
        SubClassOf:
          - a: FOO:0000002  # second class
```

Up until now we have not introduced anything that commits to
flexible-schema-style (other than granting 'label' special
status). Now we make our first commitment:

if we want to say FOO:1 is a subclass of part of some FOO:3:

```
      - a: owl:Class
        id: FOO:0000001
        label: first class
        SubClassOf:
          - part_of:
              a: FOO:0000003
```

Note the level of indirection allows a natural point for axiom annotations:

```
      - a: owl:Class
        id: FOO:0000001
        SubClassOf:
          - a: BAR:0000002  # second class 
            Annotations:
              - comment: an annotation axiom on a SubClassOf axiom
          - part_of:
              a: BAR:0000003
              Annotations:
                - comment: an annotation axiom on a SubClassOf axiom, with the superclass being a class expression
```

This is in distinction to how axiom annotations are done in RDF (as
reified triples) or in native-OWL formats. We retain the 

#### Equivalence axioms

```
      - a: owl:Class
        id: FOO:0000001
        EquivalentTo:
          - and:
              - a: FOO:0000002
              - part_of:
                  a: FOO:0000003
```

### Annotations

As a shortcut these can be associated with the object directly:

```
      - a: owl:Class
        id: FOO:0000001
        label: first class
```

or under Annotations:

```
      - a: owl:Class
        id: FOO:0000001
        label: first class
        Annotations:
          comment: this is a comment string
```

An extra level of indirection is allowed, to specify more properties:

```
      - a: owl:Class
        id: FOO:0000001
        label: first class
        Annotations:
          comment:
            v: this is a comment string
            a: xsd:string
            lang: en
```

And annotations on annotations can be introduced here

(note: if we are going dowm this route then we may as well commit to controlled schema)

## Axioms

This is for GCIs that do not fit into frame format

# JSON form

Every owl-yaml document can be syntactically translated to a valid JSON document using a standard YAML->JSON converter

# JSON-LD and RDF forms

Every owl-yaml document is a valid JSON-LD document and hence a valid RDF document

However, it should be noted that *this uses a different mapping*.

(Note: we don't *need* this part; owl-yaml can be treated purely as
syntax with a mapping to OWL, and from hence to a traditional
OWL-in-RDF interpretation, but there may be advantages to this
alternate mapping)


Salient points:

 * reification is avoided, at the expense of some intermediate objects
 * TBox follows ABox for existentials

As an example, of the latter, consider the OWL manchester syntax representation of fred:

    Individual: fred
    Types: owns some dog

And Joe:

    Individual: joe
    Facts: owns _:1

    Individual: _:1
    Types: dog

These state the same thing. The first is more syntactically compact in
syntaxes like Manchester. However, it introduces extra elements in the
RDF:

    :fred a owl:Restriction,
        owl:someValuesFrom :dog,
        owl:onProperty :owns
    ]

This is annoying as it does not mirror the standard RDF practice of
having properties be edge labels; here it is a node. Furthermore an
additional 'property' of someValuesFrom is introduced (this disappears
in the OWL interpretation, being an element of syntax).

Compare:

    :fred :owns [
        a :dog
    ]

This is much more natural from an RDF level

The same can be done for purely TBox representations:

    :PetOwner rdfs:subClassOf [
      :owns a :animal
    ]

However, here we have to introduce a change of semantics, as the
superclass should not be an individual.

Instead we can say:

    :PetOwner oy:subClassOf [
      :owns a :animal
    ]

I(PetOwner) \sub { x : exists y owns(x,y) & y in I(Animal) }
