#!/usr/bin/perl -w
use strict;

#my $f = pop @ARGV;
my %name2id = ();
my @lines = ();


while(<>) {
    
    if (m@^Prefix:@) {
        @lines = ($_);
        last;
    }
    chomp;
    my ($id,$name) = split(/\t/, $_);
    next unless $name;
    #$id =~ s@http://purl.obolibrary.org/obo/@@;
    $name =~ s/\W+/_/g;
    $name2id{$name} = $id;
}

# defaults
$name2id{protein} = 'http://purl.obolibrary.org/obo/PR_000000001';

unshift(@lines,
     "Prefix: regulates:=<http://purl.obolibrary.org/obo/RO_0002211>\n",
     "Prefix: part_of:=<http://purl.obolibrary.org/obo/BFO_0000050>\n"
);

my %class_h = ();
#open (F, $f) || die $f;
while(<>) {
    if (/\s\@(\w+)/) {
        $class_h{$1} = 1;
        #s/\s\@/ :/;
        s/\@(\w+)/$1:/g;
    }
    push(@lines, $_);
}
#close(F);

foreach my $c (keys %class_h) {
    my $id = $name2id{$c};
    unshift(@lines, "Prefix: $c:=<$id>\n");
    if (!$id) {
        die "LOOKUP FAILED: $c";
        $id = $c;
    }
    my $label = $c;
    $label =~ s/_/ /g; # TODO
    push(@lines, "\nClass: $c:\n");
    push(@lines, "  Annotations: rdfs:label \"$label\"\n");
}

foreach (@lines) {
    print $_;
}

exit 0;

sub scriptname {
    my @p = split(/\//,$0);
    pop @p;
}

sub usage {
    my $sn = scriptname();

    <<EOM;
$sn gaf.labels go.labels [...other label files] my-lego.owl.in props.owl [...other owl files] > my.lego.owl 

Takes as input a list of ID to label mapping files followed by a list
of OWL (functional notation) or lego-OWL files and generates an OWL (functional
notation) file.

A lego-OWL file is an OWL file with "lookup macros". E.g.

  Types: \@DNA_directed_RNA_polymerase_II_core_complex

and

  Types: \@NEDD4

will be replaced by symbol IRIs such as  :DNA_directed_RNA_polymerase_II_core_complex

In addition, equivalence axioms are added at the end

Class: :DNA_directed_RNA_polymerase_II_core_complex
  EquivalentTo: :GO_0005665
  Class: :GO_0005665

This allows the resulting OWL file to be used for basic purposes without importing all of GO

EOM
}

