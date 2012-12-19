#!/usr/bin/perl
use strict;
use warnings;

use DateTime;
use File::Copy;
use Data::Dumper;

my $bin_dir;
my $dist_dir;

BEGIN {
	use Cwd;
	use File::Basename;
	$bin_dir = dirname(__FILE__);
	$bin_dir = Cwd::abs_path($bin_dir);
	($dist_dir = $bin_dir) =~ s/bin\/?$//;
}

use lib ($dist_dir, $bin_dir);
use GOBO::FileCompareExtras;
use GOBO::Logger;
my $logger;

$logger = GOBO::Logger::get_logger();

#$logger->error("\@inc: " . join(", ", @INC));
#exit(0);

## read in our config
my $defaults = GOBO::FileCompareExtras::get_config( $dist_dir );
if (! $defaults)
{	$logger = GOBO::Logger::get_logger();
	$logger->logdie("No default settings found. Dying!");
}

my %vars = ( %$defaults );

## specific settings to substitute in if using the "gwg" settings
my %gwg_vars = (
	temp_dir => $ENV{HOME} . '/temp/',
	cvs_cmd => 'cvs -q -d :ext:aji@ext.geneontology.org:/share/go/cvs',
);


my $d1;
my $cmd;
my $status;
my $d_like;

if (@ARGV)
{	my @args = @ARGV;
	if (grep { $_ eq 'gwg' } @args)
	{	foreach (keys %gwg_vars)
		{	$vars{$_} = $gwg_vars{$_};
		}
	}
	## emulate only; no cvs checkouts or commits
	if (grep { $_ eq '-e' } @args)
	{	$vars{emulate} = 1;
	}
	## no cvs commits but allow checkouts
	if (grep { $_ eq '-n' } @args)
	{	$vars{no_cvs} = 1;
	}
	## only do the Saturday reports
	if (grep { $_ eq '-s' } @args)
	{	$vars{sat_only} = 1;
	}
	## verbose
	if (grep { $_ eq '-v' } @args)
	{	GOBO::Logger::init_with_config( 'verbose' );
		$logger = GOBO::Logger::get_logger();
	}
	else
	{	GOBO::Logger::init_with_config( 'standard' );
		$logger = GOBO::Logger::get_logger();
	}

	my ($day, $mon, $year);
	## parse incoming dates
	while (@args)
	{	my $d_arg = shift @args;
		if ($d_arg =~ /^(\d\d*)[\/-](\d\d*)[\/-](\d{4})$/)
		{	($day, $mon, $year) = ($1, $2, $3);
			$d_like++;
		}
		elsif ($d_arg =~ /^(\d{4})[\-\/](\d\d*)[\-\/](\d\d*)$/)
		{	($day, $mon, $year) = ($3, $2, $1);
			$d_like++;
		}
	}
	## date-like input but not enough date info
	if ($d_like)
	{	if (! $day || ! $mon || ! $year )
		{	$logger->logdie("Insufficient data to proceed! " . ($day || "DD" ) ."-".($mon||"MM")."-".($year||"YYYY"));
		}
		$d1 = DateTime->new( year => $year, month => $mon, day => $day );
		$logger->info("date: " . $d1->ymd);
	}
}

if (! $logger)
{	$logger = GOBO::Logger::get_logger();
}

## no d1; assume that the date is today.
if (! $d1)
{	## get today's date and then find out if we have the files we need.
	$d1 = DateTime->now();
#	$logger->info("now: " . $d1->ymd);
}

## d2 is yesterday
my $d2 = $d1->clone->subtract( days => 1 );
#$logger->info("yesterday: " . $d2->ymd);

## d3 is one week ago
my $d3;
if ($d1->day_abbr eq 'Sat')
{	# we're on a Saturday so need to save the file
	# also need the previous week's file to do the report
	$d3 = $d1->clone->subtract( days => 7 );
#	$logger->info("one week ago: " . $d3->ymd);
}

chdir $vars{cvs_repo};
$logger->info("moving to $vars{cvs_repo}");

my $fh;

my @test_list = ($d1, $d2);
my $today = $d1->day_abbr;
if ($today eq 'Sat')
{	push @test_list, $d3;
}

if ($vars{sat_only} && $today ne 'Sat')
{	$logger->logdie("Saturday reporting only mode; today is not Saturday. Dying")
}

my @get_list;
foreach my $d (@test_list)
{	$fh->{ $d->ymd } = $vars{temp_dir} . "go-".$d->ymd.".obo";
	if (-e $vars{temp_dir} . "go-".$d->ymd.".obo" && ! -z $vars{temp_dir} . "go-".$d->ymd.".obo" )
	{	## ok, we have it
		$logger->info("Found " . $vars{temp_dir} . "go-".$d->ymd.".obo");
	}
	else
	{	push @get_list, $d->ymd;
	}
}

if (scalar @get_list > 0)
{	foreach (@get_list)
	{	$cmd = $vars{cvs_cmd} . ' checkout -D '. $_ .' go/ontology/editors/gene_ontology_write.obo';
		$logger->info("cmd: $cmd");
		$status = `$cmd 2>&1` unless $vars{emulate};
		$logger->info("status: " . ($status || "none"));
		if ($status && $status =~ /\w/)
		{	$logger->info("status: $status");
			if ($status !~ /[UA] .*?gene_ontology_write.obo/)
			{	$logger->logdie( "Problem with CVS: $status" );
			}
		}
		move($vars{cvs_repo}."/go/ontology/editors/gene_ontology_write.obo", $vars{temp_dir} . "go-" . $_ . ".obo") unless $vars{emulate};
		$logger->info("moving go/ontology/editors/gene_ontology_write.obo to " . $vars{temp_dir} . "go-" . $_ . ".obo");
	}
}

chdir $dist_dir;
$logger->info("moving to $dist_dir");

## if it is a weekday, just generate the rss feeds
## comp_files_scr
if (! $vars{sat_only})
{	
	$cmd = "perl bin/compare-obo-files.pl -f1 " . $fh->{$d2->ymd} . " -f2 " . $fh->{$d1->ymd} . " -m rss email";
	if ($vars{verbose})
	{	$cmd .= " -v";
		$logger->info("cmd: $cmd");
	}

	$status = `$cmd 2>&1` unless $vars{emulate};
	$logger->info("status: " . ($status || "none"));

	## run the def differ report
	$cmd = "perl bin/compare-defs.pl -c -f1 " . $fh->{$d2->ymd} . " -f2 " . $fh->{$d1->ymd} . " -m rss email";
	if ($vars{verbose})
	{	$cmd .= " -v";
		$logger->info("cmd: $cmd");
	}
	$status = `$cmd 2>&1` unless $vars{emulate};
	$logger->info("status: " . ($status || "none"));
	
	## commit the rss files
	chdir $vars{cvs_repo};
	$logger->info("moving to $vars{cvs_repo}");
	$cmd = $vars{cvs_cmd} . ' ci -m "Automated RSS updates" go/www/rss/*';
	$logger->info("cmd: $cmd");
	$status = `$cmd 2>&1` unless ($vars{emulate} || $vars{no_cvs});
	$logger->info("status: " . ($status || "none"));

}

if ($today ne 'Sat')
{	if ($today ne 'Sun')
	{	## delete the $d2 file
		unlink $fh->{ $d2->ymd } || warn "Could not remove " . $fh->{ $d2->ymd } . ": $!";
	}
	exit(0);
}

## Weekly
## run the reports and the def differ

## create a directory for the reports
if (! -e $vars{cvs_repo}.'/'.$vars{report_dir})
{	mkdir $vars{cvs_repo}.'/'.$vars{report_dir};
}
else
{	$logger->info("Directory $vars{cvs_repo}/$vars{report_dir} already exists!");
}

if (! -e $vars{cvs_repo} .'/'. $vars{report_dir} . $d1->ymd)
{	$logger->info("Running mkdir $vars{report_dir}". $d1->ymd . "");
	mkdir $vars{cvs_repo}.'/'.$vars{report_dir} . $d1->ymd;
}
else
{	$logger->info("Directory $vars{cvs_repo}/$vars{report_dir}" . $d1->ymd . " already exists!");
}

chdir $dist_dir;
$logger->info("moving to $dist_dir");

## comp_files_scr
$cmd = "perl bin/compare-obo-files.pl -f1 " . $fh->{$d3->ymd} . " -f2 " . $fh->{$d1->ymd} . " -m text html -o ";

## run the weekly ontology reports
foreach my $c (
	$vars{cvs_repo}.'/'.$vars{report_dir} . $d1->ymd . "/weekly-" . $d1->ymd . " -l l",
	$vars{cvs_repo}.'/'.$vars{report_dir} . $d1->ymd . "/weekly-med-" . $d1->ymd
	)
{	if ($vars{verbose})
	{	$c .= " -v";
		$logger->info("cmd: $cmd$c");
	}
	$status = `$cmd$c 2>&1` unless $vars{emulate};
	$logger->info("status: " . ($status || "none"));
	if ($status)
	{	## oh crap, something went wrong...
	}

}

## run the def differ report
$cmd = "perl bin/compare-defs.pl -f1 " . $fh->{$d3->ymd} . " -f2 " . $fh->{$d1->ymd} . " -c -o " . $vars{cvs_repo}.'/'.$vars{report_dir} . $d1->ymd . "/weekly-defs-" . $d1->ymd . ".html";
if ($vars{verbose})
{	$cmd .= " -v";
	$logger->info("cmd: $cmd");
}
$status = `$cmd 2>&1` unless $vars{emulate};
$logger->info("status: " . ($status || "none"));
if ($status)
{	## oh crap, something went wrong...
	if ($status =~ /The two files specified appear to be identical!/)
	{
	}

}

chdir $vars{cvs_repo};
$logger->info("moving to $vars{cvs_repo}");

## add the files to cvs and then commit them
$cmd = $vars{cvs_cmd} . ' add '. $vars{report_dir} . $d1->ymd . ' ; ' . $vars{cvs_cmd} . ' add '. $vars{report_dir} . $d1->ymd . '/weekly* ; ' . $vars{cvs_cmd} . ' ci -m "Automated report commits" ' . $vars{report_dir} . $d1->ymd . '/weekly*';
$logger->info("cmd: $cmd");
$status = `$cmd 2>&1` unless ($vars{emulate} || $vars{no_cvs});
$logger->info("status: " . ($status || "none"));
if ($status)
{	## oh crap, something went wrong...
}

## clean up the files
## delete the $d2 and $d3 file
unless ($vars{emulate})
{	unlink $fh->{ $d2->ymd }, $fh->{ $d3->ymd };
}


## /home/user/local/bin/publish_news.pl -v -e -u myname -p mypass -t "Test Story 002" -b "This is the second story." -T "5,6"
chdir $dist_dir;
$logger->info("moving to $dist_dir");

my $pub_h = {
	u => 'gobot',
	p => 'rivphaw.8NT',
	title => "GO Weekly Ontology Report for " . join(" ", $d1->day, $d1->month_name, $d1->year ),
	T => "1,6,9",
};

(my $report_url = $vars{report_dir}) =~ s/\/*go//;
$report_url .= '/' unless substr($report_url, -1, 1) eq '/';

## send an email about the new files
$pub_h->{story} = 'Greetings GO ontology watchers,

The following weekly ontology reports are now available:

Ontology changes, full details: <a href="http://geneontology.org/'. $report_url . $d1->ymd . '/weekly-' . $d1->ymd . '.html">web page</a> | <a href="http://geneontology.org/' . $report_url . $d1->ymd . '/weekly-' . $d1->ymd . '.txt">text</a>

Ontology changes, less detailed: <a href="http://geneontology.org/' . $report_url . $d1->ymd . '/weekly-med-' . $d1->ymd . '.html">web page</a> | <a href="http://geneontology.org/' . $report_url . $d1->ymd . '/weekly-med-' . $d1->ymd . '.txt">text</a>

Definition changes: <a href="http://geneontology.org/' . $report_url . $d1->ymd . '/weekly-defs-' . $d1->ymd . '.html">web page</a>

Please enjoy!
';

$cmd = "perl bin/publish_news.pl -v -e " . join(" ", map { "-" . $_ . " '" . $pub_h->{$_} . "' " } keys %$pub_h);
$logger->info("cmd: $cmd");

$logger->info("vars: " . Dumper(\%vars) . "");

exit(0);
