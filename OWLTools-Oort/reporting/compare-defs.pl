#!/usr/bin/perl -w

=head1 NAME

compare-defs.pl - compare term defs between two OBO files

=head1 SYNOPSIS

 compare-defs.pl --file_1 old_gene_ontology.obo --file_2 gene_ontology.obo
 -o results.txt

=head1 DESCRIPTION

Compares the defs in two OBO files and records the differences between them

=head2 Input parameters

=head3 Required

=over

=item -o || --output /path/to/file_name

output file for results

=back

=head3 Configuration options

=over

=item Comparing two existing files

Enter the two files using the following syntax:

 -f1 /path/to/file_name  -f2 /path/to/file_2_name

where f1 is the "old" ontology file and f2 is the "new" file

=item Comparing ontology files from two different dates

Enter the file and the two dates using this syntax:

 -f /path/to/file -d1 "date one here" -d2 "date two here"

The dates must be in a CVS-parseable form, e.g. "01 Dec 2010" or "2010-01-30" (YYYY-MM-DD)

If -f is left blank, the default file used is /go/ontology/editors/gene_ontology_write.obo

If d2 is omitted, the most recent version of file I<f> will be used.

=item Comparing ontology files by revision number

Enter the file and the two revisions using this syntax:

 -f /path/to/file -r1 1.2345 -r2 1.2346

The dates must be in a CVS-parseable form, e.g. "01 Dec 2010" or "2010-01-30" (YYYY-MM-DD)

If -f is left blank, the default file used is /go/ontology/editors/gene_ontology_write.obo

If r2 is omitted, the most recent version of file I<f> will be used.

=back

=head3 Optional switches

=over

=item -c || --db_counts

Get annotation counts for any terms that have changed from a database. Note that
the appropriate DB connection parameters should be entered into the "defaults"
at the top of the file.

=item -m || --mode

Choose the file format for output, either text or html. Defaults to HTML

=item -v || --verbose

prints various messages during the execution of the script

=back

=cut

use strict;
use Data::Dumper;
$Data::Dumper::Sortkeys = 1;

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

package main;
use strict;
use Template;
use File::Temp;
use MIME::Lite;
use GOBO::Logger;
use Text::WordDiff;
use GOBO::FileCompareExtras;
use DateTime::Format::Strptime;

my $logger;

## read in our config
my $defaults = GOBO::FileCompareExtras::get_config( $dist_dir );
if (! $defaults)
{	die("No default settings found. Dying");
}

run_script($defaults, \@ARGV);

exit(0);

sub run_script {
	my $options = parse_options(@_);
	$logger->info( "Parsed options. Now starting script..." );

	## quick 'diff' check of whether the files are identical or not
	my $cmd = "diff -w -q -i '" . $options->{f1} . "' '" . $options->{f2} . "'";

	my $status = `$cmd`;
	my $identical;
	## the files are identical!
	if (! $status)
	{	## if we're reporting identical files and the output is html or txt, carry on
		if ($options->{report_identical} && ($options->{mode}{html} || $options->{mode}{txt}))
		{	$identical++;
			$logger->warn("The two files specified appear to be identical");
		}
		## otherwise, die
		else
		{	$logger->logdie("The two files specified appear to be identical!");
		}
	}

	my $t_args = {
		INCLUDE_PATH => $options->{inc_path},
	};
	my $tt = Template->new($t_args) || $logger->logdie("$Template::ERROR");

	my $data;
	my $output;

#	open(OUT, ">" . $options->{'output'}) or die("Could not create " . $options->{output} . " : $!");

	open(FH, "<" . $options->{'f1'}) or die("Could not open f1, " . $options->{'f1'} . "! $!");
	my @arr;
	# remove and parse the header
	{	local $/ = "\n[";
		@arr = grep { /(^date: | cvs version)/i } split("\n", <FH> );
		## date: 04:01:2011 16:56
		## remark: cvs version: $Revision: 1.1692 $
		foreach (@arr)
		{	if ($_ =~ /date: (.+)$/)
			{	$data->{f1_date} = $1;
			}
			elsif ($_ =~ /cvs version: \$Revision: (\S+)/)
			{	$data->{f1_cvs} = $1;
			}
		}
		if ( ! $data->{f1_date} || ! $data->{f1_cvs} )
		{	$logger->warn("Could not find the data or cvs version of f1, " . $options->{'f1'});
		}
		if (! $identical)
		{	#$logger->info( "Parsed $f header; starting body" );
			my @lines;
			{	local $/ = "\n[";
				while (<FH>)
				{	if (/^(\S+)\]\s*.*?^id:\s*(\S+)/sm)
					{	# extract the interesting data
						if ($1 eq "Term")
						{	my $h;
							map {
								if (/(.*?): ?(.+)( ?\!.*)?/)
								{	$h->{$1} = $2;
								}
							} grep { /^(id|name|def|is_obsolete):/ } split("\n", $_);

							if ($h->{def})
							{	## clip off the def xrefs
								if ($h->{def} =~ /^\"(.*)\"\s*(\[.*)/)
								{	$h->{def} = $1;
									$h->{simple_def} = lc($h->{def});
									$h->{simple_def} =~ s/[^a-z0-9 ]//g;
								}
								else
								{	$logger->warn("Could not parse def for " . $h->{id});
								}
							}
							$data->{f1}{ $h->{id} } = $h;
						}
					}
				}
			}
		}
	}
	close(FH);

	$logger->info( "Parsed " . $options->{f1} );

	open(FH, "<" . $options->{'f2'}) or die("Could not open f2, " . $options->{'f2'} . "! $!");
	# remove and parse the header
	{	local $/ = "\n[";
		@arr = grep { /(^date: | cvs version)/i } split("\n", <FH> );
		foreach (@arr)
		{	if ($_ =~ /date: (.+)$/)
			{	$data->{f2_date} = $1;
			}
			elsif ($_ =~ /cvs version: \$Revision: (\S+)/)
			{	$data->{f2_cvs} = $1;
			}
		}
		if (! $data->{f2_date} || ! $data->{f2_cvs})
		{	warn "Could not find the data or cvs version of f2, " . $options->{'f2'};
		}
		if (! $identical)
		{
		#	$logger->info( "Parsed $f header; starting body" );
			my @lines;
			{	local $/ = "\n[";
				while (<FH>)
				{	if (/^(\S+)\]\s*.*?^id:\s*(\S+)/sm)
					{	# extract the interesting data
						if ($1 eq "Term")
						{	my $h;
							map {
								if (/(.*?): ?(.+)( ?\!.*)?/)
								{	$h->{$1} = $2;
								}
							} grep { /^(id|name|def|is_obsolete):/ } split("\n", $_);

							if ($h->{def})
							{	## clip off the def xrefs
								if ($h->{def} =~ /^\"(.*)\"\s*(\[.*)/)
								{	$h->{def} = $1;
									$h->{simple_def} = lc($h->{def});
									$h->{simple_def} =~ s/[^a-z0-9 ]//g;
								}
								else
								{	warn "Could not parse def for " . $h->{id};
								}
							}

							if ($data->{f1}{ $h->{id} })
							{	## existing term
								if ($data->{f1}{$h->{id}}{simple_def} && $h->{simple_def} && $h->{simple_def} ne $data->{f1}{ $h->{id} }{simple_def})
								{	if ($h->{is_obsolete})
									{	$logger->info( "Got an obsolete term!" );
										$h->{simple_def} =~ s/^obsolete\s*//;
										if ($h->{simple_def} eq $data->{f1}{$h->{id}}{simple_def})
										{	## term has been obsoleted. Don't show.
											$logger->info("Ignoring obsolete term " . $h->{id});
										}
										else
										{	$logger->info("Including obsolete term " . $h->{id});
											$data->{changed}{$h->{id}}++;
											$data->{f2}{$h->{id}} = $h;
										}
									}
									else
									{	$data->{changed}{$h->{id}}++;
										$data->{f2}{$h->{id}} = $h;
									}
								}
							}
						}
					}
				}
			}
		}
	}
	close(FH);

	$logger->info("Finished parsing files!");

	if ($identical)
	{	## the files are identical. Prepare for output!
		output_data( options => $options, output => { data => $data, %{$defaults->{html}} }, tt => $tt );
		exit(0);
	}


	foreach (keys %{$data->{changed}})
	{	my $diff = word_diff(\$data->{f1}{$_}{def}, \$data->{f2}{$_}{def}, { STYLE => 'HTMLTwoLinesLite' });
		if ($diff)
		{	if ($diff =~ /div class="file"/)
			{	$diff =~ s/(<\/?)div/$1span/gm;
				my @arr = split(/<\/span>\s*<span class="file"/, $diff, 2);
				$data->{f1}{$_}{def_diffs} = $arr[0] . "</span>";
				$data->{f2}{$_}{def_diffs} = '<span class="file"' . $arr[1];
			}
			else
			{
				my @arr = split(/<\/span>\s*<span class="file"/, $diff, 2);
				$data->{f1}{$_}{def_diffs} = $arr[0] . "</span>";
				$data->{f2}{$_}{def_diffs} = '<span class="file"' . $arr[1];
			}
		}
	}

	$logger->info( "Parsed " . $options->{f2} . "\nGathering results for printing...");

	if (! $data->{changed})
	{	$output->{no_ontology_changes} = 1;
	#	close OUT;
	#	die("No changed definitions were found in the files specified.");
	}

	## optional: connect to a db to find annotations
	if ($options->{db_counts})
	{	get_db_counts( options => $options, data => $data );
	}

	$output->{data} = $data;
	
	## clean up our mess
	if ($options->{f_moved})
	{	## move this back
		my $cmd = "cp " . $options->{f_moved} . " " . $options->{f};
		`$cmd`;
	}


	## email / rss reports: only if there have been changes
	if ($options->{mode}{email} || $options->{mode}{rss})
	{	if ($data->{changed})
		{	## generate the date
			my $parser = DateTime::Format::Strptime->new(pattern => "%d:%m:%Y %H:%M");
			my $date;
			## get the date from the header of f2
			if ($data->{f2_date})
			{
				$date = $parser->parse_datetime( $data->{f2_date} );
			}
			else
			{	$date = DateTime->now();
			}

			$output->{date_object} = $date;
			$output->{full_date} = $date->strftime("%a, %d %b %Y %H:%M:%S %z"),
			$output->{nice_date} = join(" ", $date->day, $date->month_abbr, $date->year);
		}
		if ($options->{mode}{rss})
		{	create_rss( options => $options, output => { %$output, %{$defaults->{html}} }, tt => $tt ) if $data->{changed};
		}
		if ($options->{mode}{email})
		{	create_email( options => $options, output => { %$output, %{$defaults->{html}} }, tt => $tt ) if $data->{changed};
		}
		delete $options->{mode}{rss};
		delete $options->{mode}{email};
	}

	output_data( options => $options, output => { %$output, %{$defaults->{html}} }, tt => $tt );
}

sub output_data {
	my %args = (@_);
	my $tt = $args{tt};

	$logger->info("options: " . Dumper($args{options}));

	foreach my $m (keys %{$args{options}->{mode}})
	{	next unless ($m eq 'txt' || $m eq 'html');
		$tt->process(
			'def_changes_' . $m . '.tmpl',
			$args{output},
			$args{options}->{mode}{$m} )
		|| die $tt->error(), "\n";
	}
}

sub create_email {
	my %args = (@_);
	my $tt = $args{tt};

	$logger->info("Processing the email report...");
	my $body;
	$tt->process(
		'def_changes_email.tmpl',
		$args{output},
		\$body )
	|| die $tt->error(), "\n";

	# Construct the MIME::Lite object.
	my $mail = MIME::Lite->new(
		From     => $args{options}->{email_from},
		To       => $args{options}->{email_to},
#		Bcc      => $maintainer},
		Subject  => 'New ' . $defaults->{html}{ontology_name} . ' Definition Changes',
		Encoding => 'quoted-printable',
		Data     => $body,
		Type     => 'text/html',
	);

	# This is added to prevent post-send attachments (e.g. as part
	# of an email list) messing up the html segment. Comment out
	# if this is unnecessary.
	$mail->attach(
		Data => ' ',
		Type => 'text/plain',
	);

	# Finally, send the mail.
	if ($args{options}->{smtp_server}) {
		$mail->send( 'smtp', $args{options}->{smtp_server} ) || $logger->error("Could not send mail!");
	}
	else {
		$mail->send() || $logger->error("Could not send mail!");
	}
	$logger->info( $mail->as_string );
	$logger->info("Finished processing email report!");
}

sub create_rss {
	my %args = (@_);
	my $tt = $args{tt};

	my $def_file = $args{options}->{rss_path} . 'def_diffs.rss';

	my $date = $args{output}->{date_object};
	my $old = $date->clone->subtract( months => 1 );

	my $parser = DateTime::Format::Strptime->new(pattern => "%a, %d %b %Y %H:%M:%S %z");

	## create the def rss
	my $old_data = GOBO::FileCompareExtras::trim_rss( file => $def_file, date => $old );

	$tt->process(
		'def_changes_rss.tmpl',
		{ %{$args{output}}, old_data => $old_data },
		$def_file,
	) || die $tt->error(), "\n";
}


# parse the options from the command line
sub parse_options {
	my ($opt, $args) = @_;
	my $errs;

	while (@$args && $args->[0] =~ /^\-/) {
		my $o = shift @$args;
		## file to use if r1/d1 and r2/d2 are being used
		if ($o eq '-f' || $o eq '--file' || $o eq '--file') {
			if (@$args && $args->[0] !~ /^\-/)
			{	$opt->{f} = shift @$args;
			}
		}
		elsif ($o eq '-f1' || $o eq '--file_1' || $o eq '--file_one') {
			if (@$args && $args->[0] !~ /^\-/)
			{	$opt->{f1} = shift @$args;
			}
		}
		elsif ($o eq '-f2' || $o eq '--file_2' || $o eq '--file_two') {
			if (@$args && $args->[0] !~ /^\-/)
			{	$opt->{f2} = shift @$args;
			}
		}
		elsif ($o eq '-d1' || $o eq '--date_1' || $o eq '--date_one') {
			if (@$args && $args->[0] !~ /^\-/)
			{	$opt->{d1} = shift @$args;
				$opt->{d1} =~ s/(^["']|["']$)//g;
			}
		}
		elsif ($o eq '-d2' || $o eq '--date_2' || $o eq '--date_two') {
			if (@$args && $args->[0] !~ /^\-/)
			{	$opt->{d2} = shift @$args;
				$opt->{d2} =~ s/(^["']|["']$)//g;
			}
		}
		elsif ($o eq '-r1' || $o eq '--rev_1' || $o eq '--rev_one') {
			if (@$args && $args->[0] !~ /^\-/)
			{	$opt->{r1} = shift @$args;
				$opt->{r1} =~ s/(^["']|["']$)//g;
			}
		}
		elsif ($o eq '-r2' || $o eq '--rev_2' || $o eq '--rev_two') {
			if (@$args && $args->[0] !~ /^\-/)
			{	$opt->{r2} = shift @$args;
				$opt->{r2} =~ s/(^["']|["']$)//g;
			}
		}
		elsif ($o eq '-o' || $o eq '--output') {
			if (@$args && $args->[0] !~ /^\-/)
			{	$opt->{output} = shift @$args;
			}
		}
		elsif ($o eq '-m' || $o eq '--mode') {
			while (@$args && $args->[0] !~ /^\-/)
			{	my $m = shift @$args;
				$m = lc($m);
				if (grep { $m eq $_ } qw(txt text html rss email))
				{	$m = 'txt' if $m eq 'text';
					$opt->{mode}{$m} = 1;
				}
			}
		}
		elsif ($o eq '-c' || $o eq '--db_counts') {
			$opt->{db_counts} = 1;
			require DBI;
			require DBD::mysql;
		}
		elsif ($o eq '-h' || $o eq '--help') {
			system("perldoc", $0);
			exit(0);
		}
		elsif ($o eq '-v' || $o eq '--verbose') {
			$opt->{verbose} = 1;
		}
		elsif ($o eq '--galaxy') {
			$opt->{galaxy} = 1;
		}
		else {
			push @$errs, "Ignored nonexistent option $o";
		}
	}
	return check_options($opt, $errs);
}


# process the input params
sub check_options {
	my ($opt, $errs) = (@_);

	if (!$opt)
	{	GOBO::Logger::init_with_config( 'standard' );
		$logger = GOBO::Logger::get_logger();
		$logger->logdie("Error: please ensure you have specified the input file(s) and/or date(s)/revision(s) and an output file.\nThe help documentation can be accessed with the command\n\t" . scr_name() . " --help");
	}

	if (! $opt->{verbose})
	{	$opt->{verbose} = $ENV{GO_VERBOSE} || 0;
	}

	if ($opt->{galaxy})
	{	GOBO::Logger::init_with_config( 'galaxy' );
		$logger = GOBO::Logger::get_logger();
	}
	elsif ($opt->{verbose} || $ENV{DEBUG})
	{	GOBO::Logger::init_with_config( 'verbose' );
		$logger = GOBO::Logger::get_logger();
	}
	else
	{	GOBO::Logger::init_with_config( 'standard' );
		$logger = GOBO::Logger::get_logger();
	}

	if ($errs && @$errs)
	{	foreach (@$errs)
		{	$logger->error($_);
		}
	}
	undef $errs;

	if (! $opt->{mode})
	{	$opt->{mode}{html} = 1;
	}

	## make sure that html and text files have an output specified
	if (! $opt->{output} && ( $opt->{mode}{html} || $opt->{mode}{txt} ))
	{	push @$errs, "specify an output file using -o /path/to/<file_name>";
	}
	else
	{	if ($opt->{mode}{html} && $opt->{mode}{txt})
		{	## use the file name from $opt->{output} plus suffix
			foreach my $m qw( html txt )
			{	$opt->{mode}{$m} = $opt->{output} . "." . $m;
				## make sure that if the file exists, we can write to it
				if (-e $opt->{output} && ! -w $opt->{output})
				{	push @$errs, $opt->{output} . " already exists and cannot to be written to";
				}
			}
		}
		elsif ($opt->{mode}{html} || $opt->{mode}{txt})
		{	foreach my $m qw( html txt )
			{	if ($opt->{mode}{$m})
				{	## give the file the appropriate suffix if lacking
					if ($opt->{output} !~ /\.$m$/ && ! $opt->{galaxy})
					{	$opt->{mode}{$m} = $opt->{output} . ".$m";
						$logger->warn($m . " output will be saved in file " . $opt->{mode}{$m});
					}
					else
					{	$opt->{mode}{$m} = $opt->{output};
					}
					## make sure that if the file exists, we can write to it
					if (-e $opt->{mode}{$m} && ! -w $opt->{mode}{$m})
					{	push @$errs, $opt->{mode}{$m} . " already exists and cannot to be written to";
					}
				}
			}
		}
	}

	## INPUT OPTIONS:
	## - specify f1 and f2
	## - specify f, r1/d1, r2/d2
	## - specify f, r1/d1 and use most recent file as r2/d2

	if ($opt->{f1} || $opt->{f2})
	{	foreach my $f qw(f1 f2)
		{	if (!$opt->{$f})
			{	push @$errs, "specify an input file using -$f /path/to/<file_name>";
			}
			elsif (! -e $opt->{$f})
			{	push @$errs, "the file " . $opt->{$f} . " could not be found.";
			}
			elsif (! -r $opt->{$f} || -z $opt->{$f})
			{	push @$errs, "the file " . $opt->{$f} . " could not be read.";
			}
		}
	}
	elsif ($opt->{d1} || $opt->{d2} || $opt->{r1} || $opt->{r2})
	{	## make sure that f is specified!!
		if (! $opt->{f})
		{	push @$errs, "specify an input file using -f /path/to/<file_name>";
		}

		## make sure that we don't have mixed params
		if ($opt->{r1} && $opt->{d1} || $opt->{r1} && $opt->{d2} || $opt->{r2} && $opt->{d1} || $opt->{r2} && $opt->{d2})
		{	push @$errs, "please use either date or revisions, not both";
		}

		## make sure that we have d1 / r1 if we have d2 / r2
		if ( $opt->{d2} && ! $opt->{d1} || $opt->{r2} && ! $opt->{r1} )
		{	push @$errs, "r2/d2 is specified but r1/d1 is not";
		}

		if (! $errs)
		{
			## OK, if we are getting files from CVS, we need to fetch the files and store them somewhere
			## move any existing file out of the way
			my $f_name = $opt->{f};
			$logger->debug( "f_name: " . $f_name );
			if (-e $f_name)
			{	my $cmd = "cp $f_name $f_name" . "-current";
				`$cmd`;
				warn "Moved current $f_name to $f_name" . "-current";
				$opt->{f_moved} = $f_name . "-current";
			}

			foreach my $x qw( r d )
			{	foreach my $n qw(1 2)
				{	next unless $opt->{ $x . $n };
					my $temp = File::Temp->new();
					my $cmd = "perl " . $defaults->{dist_path} . "/bin/cvs-retriever.pl -v -" . $x . " " . $opt->{$x.$n}. " -o " . $temp->filename;
					if ($opt->{galaxy})
					{	$cmd .= " -g";
					}
					elsif ($opt->{verbose})
					{	$cmd .= " -v";
					}
					$logger->info("cmd: $cmd");
					my $status;
					eval { $status = `$cmd` };
					$logger->info("status: $status");
					if ($@)
					{	$logger->error("CVS problem: $@");
					}
					else
					{	## turn the file into a temporary file
						$opt->{"f" . $n} = $temp;
					}
				}
			}
			## second file not specified: use the current date instead
			if (! $opt->{r2} && ! $opt->{d2})
			{	my $temp = File::Temp->new();
				my $cmd = "perl " . $defaults->{dist_path} . "/bin/cvs-retriever.pl -v -o " . $temp->filename;
				if ($opt->{galaxy})
				{	$cmd .= " -g";
				}
				elsif ($opt->{verbose})
				{	$cmd .= " -v";
				}
				my $status;
				eval { $status = `$cmd` };
				$logger->info("status: $status");
				if ($@)
				{	$logger->error("CVS problem: $@");
				}
				else
				{	## turn the file into a temporary file
					$opt->{f2} = $temp;
				}
			}
		}
	}
	else
	{	push @$errs, "specify either a pair of files to compare or a file name and dates or revisions to compare";
	}

	## make sure that we can find the template directory!
	my @paths = split(":", $opt->{inc_path});
	my $pass;
	foreach (@paths)
	{	$_ =~ s/\/$//;
		foreach my $m (keys %{$opt->{mode}})
		{	if (-e $_ . "/def_changes_" . $m . '.tmpl')
			{	$pass->{$m}++;
#				last;
			}
		}
	}
	if (! $pass || scalar keys %{$opt->{mode}} != scalar keys %$pass)
	{	push @$errs, "could not find the template file; check the paths in \$defaults->{inc_path}";
	}

	if ($errs && @$errs)
	{	$logger->logdie("Please correct the following parameters to run the script:\n" . ( join("\n", map { " - " . $_ } @$errs ) ) . "\nThe help documentation can be accessed with the command\n\t" . scr_name() . " --help");
	}

	return $opt;
}

sub get_db_counts {
	my %args = (@_);
	my $options = $args{options};
	my $data = $args{data};

	if (! $args{options} || ! $options->{dbdriver} || ! $options->{dbname} || ! $options->{dbhost})
	{	$logger->error("Missing parameters for DB connection!");
		return;
	}
	## connect
	my $dsn = "DBI:" . $options->{dbdriver}
	. ":database=" . $options->{dbname}
	. ";host=" . $options->{dbhost};
	if ($options->{dbport})
	{	$dsn .= ";port=" . $options->{dbport};
	}

	my $dbh = DBI->connect( $dsn, $options->{dbuser}, $options->{dbpass} ) or warn "Could not connection to the database: " . $DBI::errstr;
	if ($dbh)
	{	my $i_data = $dbh->selectall_hashref( 'SELECT * FROM instance_data', 'release_name' );
		## save this info
		foreach (keys %$i_data)
		{	$data->{db_data}{$_} = $i_data->{$_};
			last;
		}
		$logger->info( "data->{db_data}: " . Dumper($data->{db_data}));

		## get direct associations
		my $direct = $dbh->selectall_arrayref( 'SELECT term.acc, COUNT(association.id) FROM association INNER JOIN term ON term.id=association.term_id WHERE term.acc IN ("' . join('","', keys %{$data->{changed}}) . '") GROUP BY term.id' );
		if ($direct && @$direct)
		{	foreach (@$direct)
			{	## [ term.acc, count ]
				$data->{f2}{ $_->[0] }{direct} = $_->[1];
			}
			$logger->info( "direct annots: " . Dumper($direct));
		}
		## get indirect associations
		my $indirect = $dbh->selectall_arrayref( 'SELECT term.acc, COUNT(association.id) FROM term INNER JOIN graph_path ON term.id=graph_path.term1_id INNER JOIN association ON association.term_id=graph_path.term2_id WHERE term.acc IN ("' . join('","', keys %{$data->{changed}}) . '") GROUP BY term.id' );
		if ($indirect && @$indirect)
		{	foreach (@$indirect)
			{	## [ term.acc, count ]
				$data->{f2}{ $_->[0] }{indirect} = $_->[1];
			}
			$logger->info( "indirect annots: " . Dumper($indirect));
		}
	}
}

sub scr_name {
	my $n = $0;
	$n =~ s/^.*\///;
	return $n;
}

1;

{	package HTMLTwoLinesLite;
	use strict;
	use HTML::Entities qw(encode_entities);
	use base 'Text::WordDiff::Base';
	use Data::Dumper;

	sub file_header {
		my $self = shift;
		my $header = $self->SUPER::file_header(@_);

		if ($header)
		{	$self->{__str1} = $self->{__str2} = qq{<span class="file"><span class="fileheader">$header</span>};
		}
		else
		{	$self->{__str1} = $self->{__str2} = '<span class="file">';
		}
		return '';

		return '<span class="file">' unless $header;
		return qq{<span class="file"><span class="fileheader">$header</span>};
	}

	sub hunk_header {
		my $self = shift;
	#	$self->{__str1} .= '<span class="hunk">';
	#	$self->{__str2} .= '<span class="hunk">';
		return '';
	}
	sub hunk_footer {
		my $self = shift;
	#	$self->{__str1} .= '</span>';
	#	$self->{__str2} .= '</span>';
		return '';
	}

	sub file_footer {
		my $self = shift;
		$self->{__str1} .= '</span>';
		$self->{__str2} .= '</span>';
		return $self->{__str1} . $self->{__str2};
	}

	sub same_items {
		my $self = shift;
		$self->{__str1} .= encode_entities( join '', @_ );
		$self->{__str2} .= encode_entities( join '', @_ );
		return '';
	}

	sub delete_items {
		my $self = shift;
		$self->{__str1} .= '<del>' . encode_entities( join '', @_ ) . '</del>';
		return '';
	}

	sub insert_items {
		my $self = shift;
		$self->{__str2} .= '<ins>' . encode_entities( join '', @_ ) . '</ins>';
		return '';
	}

	1;
}

1;


################################################################################

=head1 AUTHOR

Amelia Ireland

=cut
