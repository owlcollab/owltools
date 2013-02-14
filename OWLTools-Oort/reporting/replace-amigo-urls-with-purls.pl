#!/usr/bin/perl -npi
s@http://amigo.geneontology.org/cgi-bin/amigo/term_details\?term=(\w+):@http://purl.obolibrary.org/obo/$1_@g;
