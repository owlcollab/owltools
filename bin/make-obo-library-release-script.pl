#!/usr/bin/perl
use strict;
my $ns;
my %d = ();
my %fmth = ();
while(<>) {
    chomp;
    if (/^(\S+)\t+(.*)/) {
        my ($t,$v) = ($1,$2);
        if ($t eq 'namespace') {
            $ns = $v;
        }
        elsif ($t eq 'download') {
            if ($v && !$d{$ns}) {
                $d{$ns} = $v;
            }
        }
        elsif ($t eq 'source') {
            if ($v && !$d{$ns}) {
                $d{$ns} = $v;
            }
        }
        elsif ($t eq 'prerelease_download') {
            # always takes priority..
            $d{$ns} = $v;
        }
        elsif ($t eq 'format') {
            $fmth{$ns} = $v;
        }
    }
    else {
        $ns = '';
    }
}

foreach my $ns (keys %d)  {
    next unless $ns;
    my $ont = lc($ns);
    my $fmt = $fmth{$ns};
    next if $ont eq 'lipro'; # hermit does not complete
    next unless $fmt eq 'obo' || $fmt eq 'owl';
    cmd("mkdir $ont");
    my $srcf = "$ont/$ont-src.$fmt";
    cmd("wget -N --no-check-certificate $d{$ns} -O $srcf");
    cmd("ontology-release-runner -outdir $ont -reasoner jcel --asserted --simple $srcf");
}
exit 0;    

sub cmd {
    print "@_\n";
}
