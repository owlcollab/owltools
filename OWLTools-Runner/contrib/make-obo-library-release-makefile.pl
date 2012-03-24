#!/usr/bin/perl
use strict;
my $ns;
my %d = ();
my %fmth = ();

my $md_url = 'http://obo.cvs.sourceforge.net/viewvc/obo/obo/website/cgi-bin/ontologies.txt';

my @mdfiles=();

if (!@ARGV) {
    @ARGV = ('--fetch-metadata');
}

while (@ARGV && $ARGV[0] =~ /^\-/) {
    my $opt = shift @ARGV;
    print STDERR "O: $opt\n";
    if ($opt eq '--fetch-metadata') {
        cmd("wget -N $md_url -O ontologies.txt");
        push(@mdfiles, 'ontologies.txt');
    }
    else {
        die $opt;
    }
}
push(@mdfiles, @ARGV);
die unless @mdfiles;

foreach my $mdf (@mdfiles) {
    open(F,$mdf);
    while(<F>) {
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
    close(F);
}

my @targets = ();
my @rules = ();
foreach my $ns (keys %d)  {
    next unless $ns;
    my $ont = lc($ns);
    my $fmt = $fmth{$ns};
    next if $ont eq 'lipro'; # hermit does not complete
    next unless $fmt eq 'obo' || $fmt eq 'owl';

    my $srcf = "$ont/src/$ont.$fmt";

    push(@targets, "release-$ont");

    # first fetch
    push(@rules, "$srcf:\n\twget -N --no-check-certificate $d{$ns} -O \$@");

    # then build
    push(@rules, "$ont/$ont.owl: $srcf\n\tontology-release-runner --allow-overwrite -outdir --no-reasoner --asserted --simple \$<");

    # then release
    push(@rules, "release-$ont: $ont/$ont.owl\n\tcp $ont/$ont.owl ..; cp $ont/$ont.obo ..");

    cmd("mkdir $ont");
    cmd("mkdir $ont/src");

}
unshift(@rules, "all: @targets");

foreach (@rules) {
    print "$_\n\n";
}

exit 0;    

sub cmd {
    my $cmd = "@_";
    print STDERR "CMD:$cmd\n";
    system($cmd) && print STDERR "PROBLEM: $cmd\n";
}
