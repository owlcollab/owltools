#!/bin/sh
B=$1
C=$2
owltools --use-catalog $B --reasoner elk --assert-inferred-subclass-axioms -o -f obo --no-check $B-reasoned.obo
owltools --use-catalog $C --reasoner elk --assert-inferred-subclass-axioms -o -f obo --no-check $C-reasoned.obo
obo-simple-diff.pl -l $C-reasoned.obo $B-reasoned.obo > DIFF


