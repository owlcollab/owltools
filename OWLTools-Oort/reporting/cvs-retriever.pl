#!/usr/bin/perl -w

use strict;
use warnings;

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

use GOBO::Logger;
use GOBO::FileCompareExtras;

my $var = GOBO::FileCompareExtras::get_config($dist_dir);

my %args;
my $errs;
my $warn;

while (@ARGV)
{	my $x = shift @ARGV;
	if ($x eq '-d' || $x eq '--date')
	{	if (@ARGV && defined $ARGV[0] && $ARGV[0] !~ /^\-/)
		{	$args{date} = shift @ARGV;
		}
	}
	elsif ($x eq '-r' || $x eq '--revision')
	{	if (@ARGV && defined $ARGV[0] && $ARGV[0] !~ /^\-/)
		{	$args{rev} = shift @ARGV;
		}
	}
	elsif ($x eq '-o' || $x eq '--output')
	{	if (@ARGV && defined $ARGV[0] && $ARGV[0] !~ /^\-/)
		{	$args{output} = shift @ARGV;
		}
	}
	elsif ($x eq '-f' || $x eq '--f_name')
	{	if (@ARGV && defined $ARGV[0] && $ARGV[0] !~ /^\-/)
		{	$args{f_name} = shift @ARGV;
		}
	}
	elsif ($x eq '-v' || $x eq '--verbose')
	{	$args{verbose} = 1;
	}
	elsif ($x eq '-g' || $x eq '--galaxy')
	{	$args{galaxy} = 1;
	}
	elsif ($x eq '-h' || $x eq '--help')
	{	system("perldoc", $0);
		exit(0);
	}
	else
	{	## ignore
		push @$warn, "Ignoring argument $x";
	}
}

## sanitize args
if (! $args{rev} && ! $args{date})
{	$var->{now}++;
}
elsif ($args{rev} && $args{date})
{	push @$errs, "Please specify EITHER a revision number OR a date";
}

if ($args{rev})
{	if ($args{rev} =~ /^(\d+\.\d+|\w*)$/)
	{	$var->{revision} = $1;
	}
	else
	{	push @$warn, "Please use numerical revisions if possible";
	}
}

if ($args{date})
{	if ($args{date} =~ /^(\d{4})[-\/](\d{1,2})[-\/](\d{1,2})$/)
	{	$var->{date} = "$1\-$2\-$3";
	}
	else
	{	push @$errs, "Please use the date format YYYY-MM-DD";
	}
}

## change to the CVS dir now.
chdir( $var->{cvs_repo} );

if ($args{output})
{
	## check if the file exists
	if (-w $args{output})
	{	$var->{output} = $args{output};
	}
	else
	{	## check the directory exists
		if ($args{output} =~ /^(.+)\/.+/)
		{	my $dir = $1;
			if (! -e $dir)
			{	push @$errs, "The output directory $dir does not exist";
			}
			else
			{	$var->{output} = $args{output};
			}
		}
		else
		{	$var->{output} = $args{output};
		}
	}
}

if ($args{f_name})
{	## check the file exists
	if (-e $args{f_name})
	{	$var->{f_name} = $args{f_name};
	}
	else
	{	## check the directory exists
		if ($args{f_name} =~ /^(.+)\/.+/)
		{	my $dir = $1;
			if (! -e $dir)
			{	push @$errs, "The directory for " . $args{f_name} . " does not exist";
			}
			else
			{	$var->{f_name} = $args{f_name};
			}
		}
		else
		{	$var->{f_name} = $args{f_name};
		}
	}
}
else
{	$var->{f_name} = $var->{f};
}


my $logger;

if ($args{galaxy})
{	GOBO::Logger::init_with_config( 'galaxy' );
	$logger = GOBO::Logger::get_logger();
}
elsif ($args{verbose} || $ENV{DEBUG})
{	GOBO::Logger::init_with_config( 'verbose' );
	$logger = GOBO::Logger::get_logger();
}
else
{	GOBO::Logger::init_with_config( 'standard' );
	$logger = GOBO::Logger::get_logger();
}

if ($warn && @$warn)
{	$logger->warn(join("\n", map { " - " . $_ } @$warn ) );
}

if ($errs && @$errs)
{	$logger->logdie("Please correct the following parameters to run the script:\n" . ( join("\n", map { " - " . $_ } @$errs ) ) . "\nThe help documentation can be accessed with the command\n\t$0 --help");
}

chdir $var->{cvs_repo};
my $cmd_str;

if ($var->{now})
{	$cmd_str = "-A";
}
elsif ($var->{revision})
{	$cmd_str = "-r " . $var->{revision};
}
elsif ($var->{date})
{	$cmd_str = "-D " . $var->{date};
}

$cmd_str .= " " . $var->{f_name};
if ($var->{output})
{	$cmd_str = $var->{cvs_cmd} . " co -p " . $cmd_str . " > " . $var->{output};
}
else
{	$cmd_str = $var->{cvs_cmd} . " co " . $cmd_str;
}

$logger->info("ready to perform command\n" . $cmd_str);

my $output = `$cmd_str 2>&1`;
my $regex = qr/^[AU] $var->{f_name}/;
if (! $output || $output =~ /$regex/is)
{	## this looks OK!
	#$logger->info("Looks great!");
	exit(0);
}
else
{	$logger->logdie("Output from CVS command:\n$output");
}


=head1 NAME

cvs-retriever.pl - "simple" wrapper to get files from CVS

=head1 SYNOPSIS

 cvs-retriever.pl -d 2011-01-01

=head1 DESCRIPTION

Retrieves files from a CVS repository by date / revision / most current version

If date and revision are unspecified, it will retrieve the most recent version of the file.

If both date AND revision are specified, the script will die with an error.



=head2 Input parameters

=head3 Optional

=over

=item -d || --date I<yyyy-mm-dd>

Specify the date of the file to retrieve

=item -r || --revision I<1.23456>

Specify the revision number of the file to retrieve

=item -o || --output I</path/to/file>

Where to save the file, if not in the default location in the local CVS repository

=item -f || --f_name I</path/to/file>

The file to retrieve, if not go/ontology/editors/gene_ontology_write.obo

=item -v || --verbose

prints various messages during the execution of the script

=back

=head3 Configuration Options

There is a configuration hash at the top of the file that can be altered to suit your CVS situation. The configurable variables are

 ## the directory in which the repository is located
 - cvs_repo => $ENV{HOME},
 ## the generic CVS command
 - cvs_cmd => 'cvs -q -d :ext:obo@ext.geneontology.org:/share/go/cvs',
 ## default file to be retrieved by the command
 - f_name  => 'go/ontology/editors/gene_ontology_write.obo',

=cut
