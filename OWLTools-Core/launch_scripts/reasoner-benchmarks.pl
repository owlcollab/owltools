#!/usr/bin/perl -w
use strict;
use FileHandle;

my $verbose = 1;

#our $PELLET_OPTS = $ENV{PELLET_OPTS} || '-Xss128m -Xms256m -Xmx4096m';
our $PELLET_OPTS = $ENV{PELLET_OPTS} || '-d64 -Xss256m -Xms4096m -Xmx4096m';
our $PELLET = "java $PELLET_OPTS -jar $ENV{PELLET}/lib/pellet-cli.jar extract -s DirectSubClassOf ";
our $HERMIT = "java  -DentityExpansionLimit=256000 -mx2048m -jar /Users/cjm/OWLTools/runtime/HermiT.jar -c -v2 ";
#our $HERMIT = "java -mx2048m -jar /Users/cjm/OWLTools/runtime/HermiT.jar -c -v2 ";
#our $PELLET = "java $PELLET_OPTS -jar ".$ENV{HOME}.'/src/pellet/lib/pellet.jar';

my $MAX_TIME = 3600;

my $totaltime_h = {};
my $reasonertime_h = {};
my $reasoner_h =
{
#    oboedit_lpr => sub {
#        my $f = shift;
#        my $benchf = shift;
#        run_oboedit_reasoner($f,$benchf);
#    },

#    oboedit_fcr => sub {
#        my $f = shift;
#        my $benchf = shift;
#        run_oboedit_reasoner($f, $benchf, "-reasonerfactory org.obo.reasoner.impl.ForwardChainingReasonerFactory");
#    },

    oboedit_rbr => sub {
        my $f = shift;
        my $benchf = shift;
        run_oboedit_reasoner($f, $benchf, "-reasonerfactory org.obo.reasoner.rbr.RuleBasedReasonerFactory");
    },

#    oboedit_pwr => sub {
#        my $f = shift;
#        my $benchf = shift;
#        run_oboedit_reasoner($f, $benchf, "-reasonerfactory org.obo.reasoner.impl.PelletWrappedReasonerFactory");
#    },

    owlapi_pellet => sub {
        my $f = shift;
        my $benchf = shift;
	my $owlfile = genowl($f);
        run_owl_reasoner($owlfile, $benchf, "--pellet");
    },

    owlapi_pellet_mr => sub {
        my $f = shift;
        my $benchf = shift;
	my $owlfile = genowl($f);
        run_owl_reasoner($owlfile, $benchf, "--pellet -r");
    },

    owlapi_factpp => sub {
        my $f = shift;
        my $benchf = shift;
	my $owlfile = genowl($f);
        run_owl_reasoner($owlfile, $benchf);
    },

    owlapi_factpp_mr => sub {
        my $f = shift;
        my $benchf = shift;
	my $owlfile = genowl($f);
        run_owl_reasoner($owlfile, $benchf, "-r");
    },

    pellet => sub {
        my $f = shift;
        my $benchf = shift;
	my $owlfile = genowl($f);
        runcmd("$PELLET $owlfile >& $benchf.tmp && head -1000 $benchf.tmp > $benchf");
    },

    hermit => sub {
        my $f = shift;
        my $benchf = shift;
	my $owlfile = genowl($f);
        runcmd("$HERMIT $owlfile >& $benchf");
    },

    blipkit => sub {
        my $f = shift;
        my $benchf = shift;
        runcmd("blip-reasoner -import_all -debug timing -debug reasoner -i $f -explain genus_differentia > $benchf.out >& $benchf ");
    },

    obdsql => sub {
        my $f = shift;
        my $benchf = shift;
        my $n = shift;
        unless (defined $n) {
            $n = $f;
            $n =~ s/.*\///;
            $n =~ s/\..*//;
            $n =~ tr/a-zA-Z0-9_//cd;
            $n = 'obdtest_'.$n;
        }
        runcmd("obd-create-db.pl --drop --reasoner perl -d $n $f >& $benchf");
    },

    godbsql => sub {
        my $f = shift;
        my $benchf = shift;
        my $n = shift;
        unless (defined $n) {
            $n = $f;
            $n =~ s/.*\///;
            $n =~ s/\..*//;
            $n =~ tr/a-zA-Z0-9_//cd;
            $n = 'godtest_'.$n;
        }
        $ENV{GO_NEW_ADD_ROOT}=1;
        runcmd("go-create-database-and-load.pl -d $n -h 127.0.0.1  -reasoner perl $f >& $benchf");
    },
};

my %depend_h = 
(
 owlapi_factpp_mr=>'owlapi_factpp',
 owlapi_pellet_mr=>'owlapi_pellet',
);
 

if (!@ARGV) {
    usage();
    exit(0);
}

my %only = ();
my @skipped = ();
my %continue;
my $collect = 0;
my $new_only = 0;

while ($ARGV[0] && $ARGV[0] =~ /(^\-.*)/) {
    my $opt = shift;
    if ($opt eq '-s' || $opt eq '--skip') {
	my $skip = shift @ARGV;
	push(@skipped, $skip);
        delete $reasoner_h->{$skip};
    }
    elsif ($opt eq '--only') {
	my $x = shift @ARGV;
        $only{$x} = 1;
    }
    elsif ($opt eq '--collect') {
        $collect = 1;
    }
    elsif ($opt eq '--new-only') {
        $new_only = 1;
    }
    elsif ($opt eq '-t' || $opt eq '--maxtime') {
	$MAX_TIME = shift @ARGV;
    }
    elsif ($opt eq '-c' || $opt eq '--continue') {
	$continue{shift @ARGV} = 1;
    }
    elsif ($opt eq '-h' || $opt eq '--help') {
        usage();
        exit(0);
    }
    else {
	print STDERR "UNKNOWN: $opt\n";
	usage();
	exit(1);
    }
}

if (%only) {
    if (@skipped) { 	
	print STDERR "Cannot combine --only and --skip\n";
	usage();
	exit(1);
    }
    foreach my $ r (sort (keys %$reasoner_h)) {
	delete $reasoner_h->{$r} unless $only{$r};
    }
    
}

my $summaryf = "results.tab";
my $dbf = "benchdb.tbl";
my $summaryfh;
if (keys %continue) {
    $summaryfh = FileHandle->new(">>$summaryf") || die $summaryf;
}
else {
    $summaryfh = FileHandle->new(">$summaryf") || die $summaryf;
}
my $dbfh = FileHandle->new(">>$dbf") || die $summaryf;

my @reasoners = keys %$reasoner_h;
printf $summaryfh "ONT\t";
printf $summaryfh (join("\t", @reasoners));
printf $summaryfh "\n";

my @files = @ARGV;
foreach my $f (@files) {
    foreach my $r (@reasoners) {
        logmsg("reasoner: $r");
        if ($depend_h{$r} && $reasonertime_h->{$f}->{$depend_h{$r}} eq 'NULL') {
            logmsg("No point running $r as $depend_h{$r} already failed");
            next;
        }
        my $outf = $f;
        $outf =~ s/.*\///;
        $outf =~ s/\..*//;
        $outf .= ".$r.bench";
        if ($collect) {
            logmsg("collecting: $outf");
        }
        elsif ($new_only && -e $outf) {
            logmsg("already have: $outf (running new-only)");
        }
        else {
            # run the reasoner
            my $t1 = time;
            $reasoner_h->{$r}->($f,$outf);
            my $t2 = time;
            my $td = $t2-$t1;
            $totaltime_h->{$f}->{$r} = $td;
            logmsg("totaltime: $f $r :: $td s");
        }
        my $rtime = parse_benchmarks($outf,$r);
        my $ts = (stat($outf))[9];
        
        
        if (defined $rtime) {
            #$rtime = $rtime * 1000; # seconds
            $reasonertime_h->{$f}->{$r} = $rtime;
            use POSIX 'strftime';
            
            #my $date_string = ctime(stat($outf)->mtime)->year;
            my $date_string = strftime "%Y%m%d", localtime((stat($outf))[9]);

            printf $dbfh "$date_string\t$f\t$r\t$reasonertime_h->{$f}->{$r}\n";
        }
        else {
            $rtime = "NULL";
        }


        logmsg("reasonertime: $f $r :: $rtime (seconds)");

    }
    printf $summaryfh "$f\t";
    printf $summaryfh join("\t", (map { $reasonertime_h->{$f}->{$_} } @reasoners));
    printf $summaryfh "\n";

}

$summaryfh->close();


logmsg("Done!");
exit 0;

sub run_oboedit_reasoner {
    my $f = shift;
    my $benchf = shift;
    my $argstr = shift || '';
    $ENV{OBO2OBO_JAVAARGS}='-d64 -Xmx4096M'; # takes precedence over vmoptions
    runcmd("obo2obo -o $argstr -saveimpliedlinks $benchf.out $f >& $benchf");    
}

sub run_owl_reasoner {
    my $f = shift;
    my $benchf = shift;
    my $argstr = shift || '';
    my $pwd = `pwd`;
    chomp $pwd;
    runcmd("owlreasoner $argstr file://$pwd/$f >& $benchf.tmp && head -1000 $benchf.tmp > $benchf");    
}

sub logmsg {
    return unless $verbose;
    my $msg = shift;
    my $t = time;
    print STDERR "LOG $t : $msg\n";
}

sub runcmd {
    my $cmd = shift;
    my $errref = shift;
    logmsg("CMD: $cmd");
    # HARDCODE: 5hr limit (CPU time)
    my $t = time;
    my $err = system("ulimit -t $MAX_TIME; (($cmd) > OUT) >&  ERR");
    my $t2 = time;
    my $td = $t2-$t;
    print STDERR "TIME: $td\n";
    print STDERR "STDERR [lines 1-50]:\n";
    print STDERR `head -50 ERR`;
    if ($err) {
        print "STDOUT [lines 1-50]:\n";
        print `head -50 OUT`;
        return 1;
    }
    return 0;
}

# will not make an owl file if one exists already
# todo: timestamps
sub genowl {
    my $f = shift;
    my $owlf = "$f.owl";
    #runcmd("test $owlf -nt $f || go2owl $f > $owlf");
    #runcmd("test $owlf -nt $f || blip -f obo -i $f io-convert -to owl -o $owlf");
    runcmd("test $owlf -nt $f || obo2owl -o file://`pwd`/$owlf $f");
    return $owlf;
}

sub parse_benchmarks {
    my $f = shift;
    my $r = shift;
    my $re;
    my $ms;
    my $mult = 1;
    my $smh = 0;
    if ($r =~ /sql/) { # go & obd
        $re = 'Duration: (\S+)';
        #$mult = 1000;
    }
    elsif ($r =~ /oboedit/) {
        $re = 'Total reasoner time = (\S+)';
        $mult = 1000;
    }
    elsif ($r =~ /owlapi/) {
        $re = 'Total reasoner time = (\S+)';
        $mult = 1000;
    }
    elsif ($r =~ /hermit/) {
        $re = 'action completed in (\S+) msec';
        $mult = 1000;
    }
    elsif ($r =~ /blip/) {
        $re = 'Total reasoner time = (\S+)';
    }
    elsif ($r =~ /pellet/) {
        # new:
        # Classifying finished in 00:00:01
        $re = 'Classif.*ing finished in (\d+):(\d+):(\d+)'; # TYPO!
        $mult = 1;
        $smh = 1;
        # old:
        # Time: 5826 ms (Loading: 2519 Species Validation: 1909 Consistency: 70 Classification: 1322 )
        #$re = '^Time:.*Classification: (\d+)';
        #$mult = 1000;
    }
    else {
    }
    my $fh = FileHandle->new($f);
    if (!$fh) {
        if (!$collect) {
            #die("assertion error: $f");
        }
        logmsg("no benchmarks for $f");
        return undef;
    }
    logmsg("collecting: $f $smh $re");
    while (<$fh>) {
        if (/$re/) {
            if ($smh) {
                $ms = $3 + $2*60 + $1*3600;
            }
            else {
                $ms = $1 / $mult;
            }
            last;
        }
    }
    $fh->close;
    #if (!(defined $ms)) {
    #    $ms = "n/a";
    #}
    return $ms;
}

sub usage {
    print <<EOM
reasoner-benchmarks.pl [-s TOOL-TO-SKIP...] [--only TOOL] [-t MAXTIME-SECONDS] FILE*
EOM
}
