See yamchester.md. Note we assume the same basics: JSON or YAML, use
of JSON-LD type contexts to make friendly tokens.

This is notes for an axiom-style YAML or JSON, i.e. functional syntax, but easily parseable.

The main decision point is whether arguments should be positional, or key-value

E.g.

```
 - a: SubClassOfAxiom
   subClass: finger
   superClass: digit
 - a: SubClassOfAxiom
   subClass: finger
   superClass: 
     a: SomeValuesFrom
     property: part_of
     filler: hand
```

vs

```
 - a: SubClassOfAxiom
   args:
    - finger
    - digit
 - a: SubClassOfAxiom
   args:
    - finger
    - a: SomeValuesFrom
      args: [part_of, hand]
```

I think it's clear the first is more modern programmer friendly. After
all if we were going the second route, we may as well just jump on the
S-Express and embrace the lispiness full-on:

```
 [ [SubClassOfAxiom, finger, digit], [SubClassOfAxiom, finger, [SomeValuesFrom, part_of, hand]] ]
```

But no one really wants this. So let's assume the first style.

This is easily extended to annotations


```
 - a: SubClassOfAxiom
   subClass: finger
   superClass: digit
   annotations:
    - property: comment
      value: simple is_a
 - a: SubClassOfAxiom
   subClass: finger
   superClass: 
     a: SomeValuesFrom
     property: part_of
     filler: hand
   annotations:
    - property: comment
      value: simple relationship
```

See yamchester.md for discussion of xsd types and languages. It may be better to bite the bullet and be consistent:

```
   annotations:
    - property: comment
      filler: 
        value: simple relationship
        lang: en
    - property: score
      filler: 
        value: 13.4
        type: float
```

YMMV.

With this in mind, the specification of axiom-yaml could be almost
generated automatically using the OWL2 spec. The type ("a") is axiom
or expression type, the keys are the names given in the BNF (or the
names of the implict variables in the owlapi). TBD: make this type
neutral? E.g. SomeValuesFrom (aka "some" in manchester) vs
ObjectSomeValuesFrom. I think I slightly prefer manchester style vocab
here and inference of the axiom type (but does this come with
penalties...?).

## Caveats

Obviously this is most appropriate to an application that is OWL
aware. Making use of an OWL-level representation in order to say draw
existential graphs or search by synonyms is kind of awkward.

