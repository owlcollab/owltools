#!/usr/bin/perl -w

=head1 NAME

compare-obo-files.pl - compare two OBO files

=head1 SYNOPSIS

 compare-obo-files.pl --file_1 old_gene_ontology.obo --file_2 gene_ontology.obo
 -m html -o results.html

=head1 DESCRIPTION

Compares two OBO files and records the differences between them, including:

* new terms

* term merges

* term obsoletions

* changes to term content, such as addition, removal or editing of features like
synonyms, xrefs, comments, def, etc..

* changes in relationships between terms

At present, only term differences are recorded in detail, although this could
be extended to other stanza types in an ontology file. The comparison is based
on creating hashes of term stanza data, mainly because hashes are more tractable
than objects.

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

=item -m || --mode I<html>

HTML mode, i.e. format the output as HTML (the default is a plain text file)

Other modes:
* text (default)
* rss - creates RSS articles for new terms and obsoletes
* email - composes a brief email with links to new terms and obsoletes

Multiple modes can be specified as follows:

 --mode html text

In this case, the output file names will be suffixed with '.html' and '.txt'

=item -l || --level I<medium>

level of detail to report about term changes; options are short (s), medium (m) or long (l)

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

use Template;
use File::Temp;
use MIME::Lite;
use GOBO::Logger;
use GOBO::FileCompareExtras;
use DateTime::Format::Strptime;
##
my $defaults = GOBO::FileCompareExtras::get_config( $dist_dir );
if (! $defaults)
{	die("No default settings found. Dying");
}

my @ordered_attribs = qw(id
is_anonymous
name
namespace
alt_id
def
comment
subset
synonym
xref
is_a
intersection_of
union_of
disjoint_from
relationship
is_obsolete
replaced_by
consider);

my @single_attribs = qw(name namespace is_obsolete def comment is_anonymous );
my $logger;

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

	my @tags_to_parse = qw(name is_a relationship subset);
	my $tags_regex = qr/^(name|is_a|relationship|subset):/;

	if ($options->{level})
	{	$output->{level} = $options->{level};
	}

	## pull in the ontology data from each file.

	foreach my $f ('f1', 'f2')
	{
	#	$logger->warn("Ready to read in $f!");
	#	read in everything up to the first stanza (i.e. the header info)
		open(FH, "<" . $options->{$f}) or die("Could not open " . $options->{$f} . "! $!");

		# remove and parse the header
		local $/ = "\n[";

		my $header = <FH>;
		my @f_data;
		my $slash = rindex $options->{$f}, "/";
		if ($slash > -1)
		{	push @f_data, substr $options->{$f}, ++$slash;
		}
		else
		{	push @f_data, $options->{$f};
		}

		if ($header =~ /^data-version: (.*)$/m)
		{	push @f_data, "data version: $1";
		}
		if ($header =~ /^date: (.*)$/m)
		{	push @f_data, "date: $1";
			$output->{$f . "_date"} = $1;
		}
		if ($header =~ /cvs version: \$Revision:\s*(\S+)/m)
		{	push @f_data, "CVS revision: " . $1;
			$output->{$f . "_cvs"} = $1;
		}
		if ($header =~ /^default-namespace: (.*)$/m)
		{	$output->{$f . '_default_namespace'} = $1;
		}
		if (@f_data)
		{	$output->{$f . '_file_data'} =  join("; ", @f_data);
		}
		else
		{	$output->{$f . '_file_data'} = "unknown";
		}

		if ($identical)
		{	$logger->info("Parsed $f header; ignoring body");
			next;
		}

		$logger->info("Parsed $f header; starting body");
	#	$logger->debug("header: " . Dumper($data->{$f}{header}));
		my @all_lines;
		## we're pulling in chunks of text, separated by \n[
		while (<FH>)
		{	if (/^(\S+)\]\s*.*?^id:\s*(\S+)/sm)
			{	# store the data as a tag-value hash indexed by stanza type and id
				# data->{$file}{$stanza_type}{$stanza_id}
				my $type = $1;
				my $id = $2;
				my @lines = map { $_ =~ s/ ! .*//; $_ } grep { /^[^!]/ && /\w/ } split("\n", $_);
				$lines[0] = "[" . $lines[0];
				$data->{$f."_lines"}{$type}{$id} = [ sort @lines ];

				# save alt_ids
				if ($type eq 'Term' && grep { /^alt_id:/ } @lines)
				{	my @arr = map { $_ =~ s/alt_id:\s*//; $_ } grep { /^alt_id:/ } @lines;
					# check for dodgy alt ids...
					foreach (@arr)
					{	if ($data->{$f . "_alt_ids"}{$_} )
						{	$logger->info("$id: alt_id $_ is already assigned to " . $data->{$f . "_alt_ids"}{$_});
						}
						else
						{	$data->{$f . "_alt_ids"}{$_} = $id;
						}
					}
				}

				# extract the interesting data
				# skip obsoletes
				my $obs_flag;
				if ($type eq 'Term')
				{	## get stuff for stats
					$data->{$f . "_stats"}{total}++;
					if ($_ =~ /^is_obsolete: true/m)
					{	$data->{$f."_stats"}{obs}++;
						$data->{$f."_obs_terms"}{$id}++;
						$obs_flag++;
					}
					else
					{	## get the term's namespace...
						my $ns = 'unknown';
						if ($_ =~ /^namespace: (.*?)\s*$/m)
						{	$ns = $1;
						}
						else
						{	if ($output->{$f . '_default_namespace'})
							{	#$logger->warn("default_namespace: " . $parser->default_namespace);
								$ns = $output->{$f . '_default_namespace'};
							}
						}
						$data->{$f . "_stats"}{by_ns}{$ns}{total}++;
						if ($_ =~ /^def: /m)
						{	$data->{$f."_stats"}{by_ns}{$ns}{def}++;
							$data->{$f."_stats"}{def_not_obs}++;
						}
					}
				}
				next if $obs_flag;

			}
			else
			{	$logger->warn("Couldn't understand data!\n$_\n");
			}
		}
		close FH;
		$logger->info("Finished parsing $f body");
	}

	$logger->info("Finished parsing files!");

	if ($identical)
	{	## the files are identical. Prepare for output!
		output_data( options => $options, output => { %$output, %{$defaults->{html}} }, tt => $tt );
		exit(0);
	}

	## ANALYSIS STAGE! ##

	# ignore these tags when we're comparing hashes
	my @tags_to_ignore = qw(id);

	## check! Should we do this or not?
	if ($options->{subset})
	{	@tags_to_ignore = qw(id is_a relationship);
	}
	## end check!

	my $ignore_regex = '(' . join("|", @tags_to_ignore) . ')';
	$ignore_regex = qr/$ignore_regex/;
	$logger->info("Read data; starting term analysis");

	## ok, check through the terms and compare 'em
	## go through all the terms in f1 and add them to the stats

	foreach my $t (keys %{$data->{f1_lines}{Term}})
	{
		## check for term in f2
		## see if it is an alt ID (i.e. it has been merged)
		## if not, it may have been lost
		if (! $data->{f2_lines}{Term}{$t})
		{	# check it hasn't been merged
			if ($data->{f2_alt_ids}{$t})
			{
				# the term was merged. N'mind!
				$output->{f1_to_f2_merge}{$t} = $data->{f2_alt_ids}{$t};
				## make sure we have the data about the term in f1 and f2
				if (! $data->{f1_hash}{Term}{$t})
				{	$data->{f1_hash}{Term}{$t} = block_to_hash( join("\n", @{$data->{f1_lines}{Term}{$t}} ) );
				}
				if (! $data->{f2_hash}{Term}{ $data->{f2_alt_ids}{$t} })
				{	$data->{f2_hash}{Term}{ $data->{f2_alt_ids}{$t} } = block_to_hash( join("\n", @{$data->{f2_lines}{Term}{ $data->{f2_alt_ids}{$t} }} ) );
				}

			}
			else
			{	$logger->info("$t is only in file 1");
				$output->{f1_only}{$t}++;

				if (! $data->{f1_hash}{Term}{$t})
				{	$data->{f1_hash}{'Term'}{$t} = block_to_hash( join("\n", @{$data->{f1_lines}{Term}{$t}} ) );
				}


			}
		}
	}

	foreach my $t (sort keys %{$data->{f2_lines}{Term}})
	#foreach my $t (sort keys %{$data->{f2_hash}{Term}})
	{	#if (! $data->{f1_hash}{Term}{$t})
		if (! $data->{f1_lines}{Term}{$t})
		{	# check it hasn't been de-merged
			if ($data->{f1_alt_ids}{$t})
			{	# erk! it was an alt id... what's going on?!
				$logger->warn("$t was an alt id for " . $data->{f1_alt_ids}{$t} . " but it has been de-merged!");
				$output->{f1_to_f2_split}{$t} = $data->{f1_alt_ids}{$t};

				if (! $data->{f1_hash}{Term}{ $data->{f1_alt_ids}{$t} })
				{	$data->{f1_hash}{Term}{ $data->{f1_alt_ids}{$t} } = block_to_hash( join("\n", @{$data->{f1_lines}{Term}{ $data->{f1_alt_ids}{$t} }} ) );
				}
				if (! $data->{f2_hash}{Term}{$t})
				{	$data->{f2_hash}{Term}{$t} = block_to_hash( join("\n", @{$data->{f2_lines}{Term}{$t}} ) );
				}


			}
			else
			{	$output->{f2_only}{$t}++;
				if (! $data->{f2_hash}{Term}{$t})
				{	$data->{f2_hash}{'Term'}{$t} = block_to_hash( join("\n", @{$data->{f2_lines}{Term}{$t}} ) );
				}

			}
		}
	## the term is in f1 and f2. let's see if there are any differences
		else
		{	# quickly compare the arrays, see if they are the same
			## fx_str is composed of the sorted tag-value pairs
			next if join("\0", @{$data->{f1_lines}{Term}{$t}}) eq join("\0", @{$data->{f2_lines}{Term}{$t}});

			foreach my $f qw(f1 f2)
			{	if (! $data->{$f . "_hash"}{'Term'}{$t})
				{	$data->{$f . "_hash"}{'Term'}{$t} = block_to_hash( join("\n", @{$data->{$f . "_lines"}{Term}{$t}} ) );
				}
			}

			## the arrays are different. Let's see just how different they are...
			my $r = compare_hashes( f1 => $data->{f1_hash}{Term}{$t}, f2 => $data->{f2_hash}{Term}{$t}, to_ignore => $ignore_regex );
			if ($r)
			{	$data->{diffs}{Term}{both}{$t} = $r;

				$output->{term_changes}{$t} = $r;
				foreach (keys %$r)
				{	$data->{diffs}{Term}{all_tags_used}{$_}{$t}++;
				}
			}
		}
	}

	my @attribs = grep { exists $data->{diffs}{Term}{all_tags_used}{$_} && $_ ne 'id' } @ordered_attribs;

	$output->{term_change_attribs} = [ @attribs ] if @attribs;

	$logger->info("Checked for new and lost terms");

	foreach my $a qw(name namespace)
	{	if ($data->{diffs}{Term}{all_tags_used}{$a})
		{	map { $output->{$a . "_change" }{$_}++ } keys %{$data->{diffs}{Term}{all_tags_used}{$a}};
		}
	}

	if ($data->{diffs}{Term}{all_tags_used}{is_obsolete})
	{	foreach my $t (keys %{$data->{diffs}{Term}{all_tags_used}{is_obsolete}})
		{
			if ($data->{f2_obs_terms}{$t})
			#if ($data->{f2_hash}{Term}{$t}{is_obsolete})
			{	$output->{f2_obsoletes}{$t}++;
				$logger->debug("added $t to f2 obsoletes");
				if (! $data->{f2_hash}{Term}{$t})
				{	$data->{f2_hash}{Term}{$t} = block_to_hash( join("\n", @{$data->{f2_lines}{Term}{$t}} ) );
				}

			}
			else
			{	$output->{f1_obsoletes}{$t}++;
				if (! $data->{f2_hash}{Term}{$t})
				{	$data->{f2_hash}{Term}{$t} = block_to_hash( join("\n", @{$data->{f2_lines}{Term}{$t}} ) );
				}
				$logger->warn("added $t to f1 obsoletes");
			}
		}
	}

	$logger->debug("output - obsoletes: " . Dumper($output->{f2_obsoletes}) . "\n\nf1 obs: " . Dumper($output->{f1_obsoletes}) . "\n");

	$logger->info("Sorting and storing data");
	$output->{f1_term_lines} = $data->{f1_lines}{Term};
	$output->{f2_term_lines} = $data->{f2_lines}{Term};
	$output->{f1_term_hash} = $data->{f1_hash}{Term};
	$output->{f2_term_hash} = $data->{f2_hash}{Term};
	$output = generate_stats($output, $data);
	$output = compare_other_stanzas($output, $data);

	foreach (@single_attribs)
	{	$output->{single_value_attribs}{$_}++;
	}

	#$logger->warn("output keys: " . join("; ", sort keys %$output) . "");
	$logger->info("Printing results!");


	if ($options->{mode}{email} || $options->{mode}{rss})
	{
		## make sure that we need to create the new files
		if ($output->{f2_only} && scalar keys %{$output->{f2_only}} > 0)
		{	$output->{report}{new}++;
		}
		if ($output->{f2_obsoletes}  && scalar keys %{$output->{f2_obsoletes}} > 0)
		{	$output->{report}{obs}++;
		}

		if ($output->{report})
		{	## generate the date
			my $parser = DateTime::Format::Strptime->new(pattern => "%d:%m:%Y %H:%M");
			my $date;
			## get the date from the header of f2
			if ($output->{f2_date})
			{
				$date = $parser->parse_datetime( $output->{f2_date} );
			}
			else
			{	$date = DateTime->now();
			}

			$output->{date_object} = $date;
			$output->{full_date} = $date->strftime("%a, %d %b %Y %H:%M:%S %z"),
			$output->{nice_date} = join(" ", $date->day, $date->month_abbr, $date->year);

			if ($options->{mode}{rss})
			{	create_rss( options => $options, output => { %$output, %{$defaults->{html}} }, tt => $tt );
				delete $options->{mode}{rss};
			}
			if ($options->{mode}{email})
			{	create_email( options => $options, output => { %$output, %{$defaults->{html}} }, tt => $tt );
				delete $options->{mode}{email};
			}
		}
	}

	output_data( options => $options, output => { %$output, %{$defaults->{html}} }, tt => $tt );

}


sub output_data {
	my %args = (@_);
	my $tt = $args{tt};

	foreach my $m (keys %{$args{options}->{mode}})
	{	next unless ($m eq 'txt' || $m eq 'html');
		$tt->process(
			$m . '_report.tmpl',
			$args{output},
			$args{options}->{mode}{$m} )
		|| die $tt->error(), "\n";
	}
}

sub create_email {
	my %args = (@_);
	my $tt = $args{tt};

	my $titles = {
		new => 'New ' . $defaults->{html}{ontology_name} . ' Term Digest',
		obs => 'New ' . $defaults->{html}{ontology_name} . ' Obsoletes',
	};

	foreach my $x qw( obs new )
	{	## check whether we need to produce the report
		if (! $args{output}->{report} || ! $args{output}->{report}{$x})
		{	next;
		}
		$logger->info("Processing the $x report...");
		my $body;
		$tt->process(
			$x . '_term_email.tmpl',
			$args{output},
			\$body )
		|| die $tt->error(), "\n";

		# Construct the MIME::Lite object.
		my $mail = MIME::Lite->new(
			From     => $args{options}->{email_from},
			To       => $args{options}->{email_to},
#			Bcc      => $maintainer},
			Subject  => $titles->{$x},
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
		$logger->info("Finished processing $x report!");
	}
}

sub create_rss {
	my %args = (@_);
	my $tt = $args{tt};

	my $files = {
		new => $args{options}->{rss_path} . 'new_term.rss',
		obs => $args{options}->{rss_path} . 'obs_term.rss',
	};
        #print STDERR "FILES: $files->{new} $files->{obs}\n";

	my $date = $args{output}->{date_object};
	my $old = $date->clone->subtract( months => 1 );

	my $parser = DateTime::Format::Strptime->new(pattern => "%a, %d %b %Y %H:%M:%S %z");

	## create the new term rss
	## pull in the existing rss file
	foreach my $x qw(new obs)
	{	my $old_data = GOBO::FileCompareExtras::trim_rss( file => $files->{$x}, date => $old );
		## check whether we need to produce the report
		if (! $args{output}->{report} || ! $args{output}->{report}{$x} )
		{	next;
		}
		$tt->process(
			$x . '_term_rss.tmpl',
			{ %{$args{output}}, old_data => $old_data },
			$files->{$x},
			)
		|| die $tt->error(), "\n";
	}

}

sub compare_other_stanzas {
	my $output = shift;
	my $d = shift;
	my $ignore = qw/id/;
	## compare the other types of stanza
	my $s_types;
	foreach (keys %{$d->{f1_lines}}, keys %{$d->{f2_lines}})
	{	$s_types->{$_}++;
	}
	delete $s_types->{'Term'};

	foreach my $type (keys %$s_types)
	{
		foreach my $t (keys %{$d->{f1_lines}{$type}})
		{	if (! $d->{f2_lines}{$type} || ! $d->{f2_lines}{$type}{$t})
			{	$logger->warn("$type $t is only in file 1");
				$output->{other}{f1_only}{$type}{$t}++;
				my @names = map { $_ =~ s/name:\s*//; $_ } grep { /^name: (.+)$/m } @{$d->{f1_lines}{$type}{$t}};
				if (@names)
				{	$output->{other}{f1_only}{$type}{$t} = { name => $names[0] };
				}
			}
		}
		foreach my $t (keys %{$d->{f2_lines}{$type}})
		{	if (! $d->{f1_lines}{$type}|| ! $d->{f1_lines}{$type}{$t})
			{	$output->{other}{f2_only}{$type}{$t}++;
				my @names = map { $_ =~ s/name:\s*//; $_ } grep { /^name: (.+)$/m } @{$d->{f2_lines}{$type}{$t}};
				if (@names)
				{	$output->{other}{f2_only}{$type}{$t} = { name => $names[0] };
				}
			}
			if ($d->{f1_lines}{$type}{$t})
			{	# quickly compare the arrays, see if they are the same
				## fx_str is composed of the sorted tag-value pairs
				next if join("\0", @{$d->{f1_lines}{$type}{$t}}) eq join("\0", @{$d->{f2_lines}{$type}{$t}});

				foreach my $f qw(f1 f2)
				{	if (! $d->{$f . "_hash"}{$type}{$t})
					{	$d->{$f . "_hash"}{$type}{$t} = block_to_hash( join("\n", @{$d->{$f . "_lines"}{$type}{$t}} ) );
					}
				}

				## the arrays are different. Let's see just how different they are...
				my $r = compare_hashes( f1 => $d->{f1_hash}{$type}{$t}, f2 => $d->{f2_hash}{$type}{$t} );
				if ($r)
				{	$d->{diffs}{$type}{both}{$t} = $r;

					$output->{$type . "_changes"}{$t} = $r;
					foreach (keys %$r)
					{	$d->{diffs}{$type}{all_tags_used}{$_}{$t}++;
					}

					$output->{other}{both}{$type}{$t} = $r;
					if (! $output->{other}{both}{$type}{$t}{name} && ( $d->{f2_hash}{$type}{$t}{name} || $d->{f1_hash}{$type}{$t}{name}) )
					{	$output->{other}{both}{$type}{$t}{name} = $d->{f2_hash}{$type}{$t}{name}[0] || $d->{f1_hash}{$type}{$t}{name}[0];
					}
				}
			}
		}
	}
	return $output;
}


sub generate_stats {
	my $vars = shift;
	my $d = shift;

#	$logger->warn("f1 stats: " . Dumper($d->{f1_stats}) . "\nf2 stats: " . Dumper($d->{f2_stats}) . "\n");

	$vars->{f2_stats} = $d->{f2_stats};
	$vars->{f1_stats} = $d->{f1_stats};
	map { $vars->{ontology_list}{$_}++ } (keys %{$vars->{f1_stats}{by_ns}}, keys %{$vars->{f2_stats}{by_ns}});

	foreach my $f qw( f1 f2 )
	{	foreach my $o (keys %{$vars->{$f . "_stats"}{by_ns}})
		{	## we have def => n terms defined
			## total => total number of terms
			if (! $vars->{$f. "_stats"}{by_ns}{$o}{def})
			{	$vars->{$f. "_stats"}{by_ns}{$o}{def} = 0;
				$vars->{$f. "_stats"}{by_ns}{$o}{def_percent} = 0;
			}
			else
			{	$vars->{$f . "_stats"}{by_ns}{$o}{def_percent} = sprintf("%.1f", $vars->{$f. "_stats"}{by_ns}{$o}{def} / $vars->{$f. "_stats"}{by_ns}{$o}{total} * 100);
			}
		}
		foreach my $x qw(obs def_not_obs)
		{	if (! $vars->{$f."_stats"}{$x})
			{	$vars->{$f."_stats"}{$x} = 0;
				$vars->{$f."_stats"}{$x . "_percent"} = 0;
			}
			else
			{	$vars->{$f."_stats"}{$x . "_percent"} = sprintf("%.1f", $vars->{$f. "_stats"}{$x} / $vars->{$f. "_stats"}{total} * 100);
			}
		}
	}

	foreach my $x qw(obs def_not_obs total)
	{	$vars->{delta}{$x} = $vars->{f2_stats}{$x} - $vars->{f1_stats}{$x};
		$vars->{delta}{$x . "_percent"} = sprintf("%.1f", $vars->{delta}{$x} / $vars->{f1_stats}{$x} * 100);
	}

	foreach my $x qw( f1 f2 )
	{	$vars->{$x."_stats"}{extant} = $vars->{$x."_stats"}{total} - $vars->{$x."_stats"}{obs};
		$vars->{$x."_stats"}{def_extant_percent} = sprintf("%.1f", $vars->{$x."_stats"}{def_not_obs} / $vars->{$x."_stats"}{extant} * 100);
	}

	foreach my $o (keys %{$vars->{ontology_list}})
	{	if ($vars->{f1_stats}{by_ns}{$o} && $vars->{f2_stats}{by_ns}{$o})
		{	$vars->{delta}{$o} = $vars->{f2_stats}{by_ns}{$o}{total} - $vars->{f1_stats}{by_ns}{$o}{total};
		}
	}

#	foreach my $x qw(f1_stats f2_stats delta)
#	{	print STDERR "$x: " . Dumper( $vars->{$x} )."\n";
#	}


	return $vars;
}


sub get_term_data {
	my %args = (@_);
	my $d = $args{data};
	my $output = $args{output};
	my $t = $args{term};
	my $to_get = $args{data_to_get};
	my $f = $args{f_data} || 'f2';

#	$logger->warn("args: " . join("\n", map { $_ . ": " . Dumper($args{$_}) } qw(term data_to_get f_data)) ."");


#	$logger->warn("data: " . Dumper($d->{$f . "_hash"}{Term}{$t}) . "\n");

#	if (! $d->{$f . "_hash"}{Term}{$t} && ! $args{f_data})
	if (! $d->{$f . "_lines"}{Term}{$t} && ! $args{f_data})
	{	## we weren't explicitly looking for the data from f2...
		$logger->warn("Couldn't find data for $t in $f; trying again...");
		if ($f eq 'f2')
		{	$f = 'f1';
		}
		else
		{	$f = 'f2';
		}
		get_term_data(%args, f_data => $f);
		return;
	}

	foreach my $x (@$to_get)
	{	next if $output->{$f}{$t}{$x};
		my @arr = grep { /^$x:/ } @{$d->{$f . "_lines"}{Term}{$t}};
		next unless @arr;
		if (grep { /^$x$/ } @single_attribs)
		{	($output->{$f}{$t}{$x} = $arr[0]) =~ s/$x:\s*//;
		}
		else
		{	$output->{$f}{$t}{$x} = [ map { s/$x:\s*//; $_ } @arr ];
		}

		if ($x eq 'anc')
		{	if (grep { /^is_obsolete/ } @{$d->{$f . "_lines"}{Term}{$t}})
			{	$output->{$f}{$t}{anc} = ['obsolete'];
			}
			else
			{	if ($d->{$f}{trimmed})
				{	my $stts = $d->{$f}{trimmed}->statements_in_ix_by_node_id('ontology_links', $t);
					if (@$stts)
					{	my %parent_h;
						map { $parent_h{$_->target->id} = 1 } @$stts;
						$output->{$f}{$t}{anc} = [ sort keys %parent_h ];
					}
				}
			}
		}
		if ($x eq 'namespace' && grep { /^is_obsolete/ } @{$d->{$f . "_lines"}{Term}{$t}})
		{	$output->{$f}{$t}{$x} = 'obsolete';
		}
	}


#	$logger->warn("wanted " . join(", ", @$to_get) . " for $t from $f; returning: " . Dumper($output->{$f}{$t}) . "");
}


=head2 Script methods

=head2 block_to_hash

input:  a multi-line block of text (preferably an OBO format stanza!)
output: lines in the array split up by ": " and put into a hash
        of the form key-[array of values]

Directly does what could otherwise be accomplished by block_to_sorted_array
and tag_val_arr_to_hash

=cut

sub block_to_hash {
	my $block = shift;

	my $arr;
	foreach ( split( "\n", $block ) )
	{	next unless /\S/;
		next if /^(id: \S+|\[|\S+\])\s*$/;
		$_ =~ s/^(.+?:)\s*(.+)\s*^\\!\s.*$/$1 $2/;
		$_ =~ s/\s*$//;
		## look for a " that isn't escaped
		if ($_ =~ /^def: *\"(.+)(?<!\\)\" *\[(.+)\]/)
		{	my ($def, $xref) = ($1, $2);
			push @$arr, ( "def: $def", "def_xref: $xref" );
		}
		else
		{	push @$arr, $_;
		}
	}
	return undef unless $arr && @$arr;
	my $h;
	foreach (@$arr)
	{	my ($k, $v) = split(": ", $_, 2);
		if (! $k || ! $v)
		{	#$logger->warn("line: $_");
		}
		else
		{	push @{$h->{$k}}, $v;
		}
	}

	map { $h->{$_} = [ sort @{$h->{$_}} ] } keys %$h;

	return $h;
}


=head2 compare_hashes

input:  hash containing
        f1 => $f1_term_data
        f2 => $f2_term_data
        to_ignore => regexp for hash keys to ignore

output: hash of differences in the form
        {hash key}{ f1 => [ values unique to f1 ]
                    f2 => [ values unique to f2 ] }

=cut

sub compare_hashes {
	my %args = (@_);
	my $f1 = $args{f1};
	my $f2 = $args{f2};
	my $ignore = $args{to_ignore};

	my $results;
	my $all_values;
	foreach my $p (keys %$f1)
	{	# skip these guys
		next if defined($ignore) && $p =~ /^$ignore$/;
		if (! $f2->{$p})
		{	$results->{$p}{f1} += scalar @{$f1->{$p}};
			$all_values->{$p}{f1} = $f1->{$p};
		}
		else
		{	# find the same / different values
			my @v1 = values %$f1;
			my @v2 = values %$f2;

			my %count;
			foreach my $e (@{$f1->{$p}})
			{	$count{$e}++;
			}
			foreach my $e (@{$f2->{$p}})
			{	$count{$e} += 10;
			}

			foreach my $e (keys %count) {
				next if $count{$e} == 11;
				if ($count{$e} == 1)
				{	$results->{$p}{f1}++;
					push @{$all_values->{$p}{f1}}, $e;
				}
				elsif ($count{$e} == 10)
				{	$results->{$p}{f2}++;
					push @{$all_values->{$p}{f2}}, $e;
				}
			}
		}
	}
	foreach (keys %$f2)
	{	if (! $f1->{$_})
		{	$results->{$_}{f2} += scalar @{$f2->{$_}};
			$all_values->{$_}{f2} = $f2->{$_};
		}
	}

#	return { summary => $results, with_values => $all_values };
	return $all_values;
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
		elsif ($o eq '--rss-path') {
			if (@$args && $args->[0] !~ /^\-/)
			{	$opt->{rss_path} = shift @$args;
                                $opt->{rss_path} .= '/' unless $opt->{rss_path} =~ /\/$/;
			}
		}
		elsif ($o eq '--config') {
                    if (@$args && $args->[0] !~ /^\-/) {
                        my $a = shift @$args;
                        if ($a =~ /(.+)\s*=(.*)/) {
                            my ($k,$v) = ($1,$2);
                            my @lookup = split(/\//,$k);
                            if (@lookup ==1) {
                                $opt->{$k} = $v;
                            }
                            elsif (@lookup ==2) {
                                $opt->{$lookup[0]}->{$lookup[1]} = $v;
                            }
                            elsif (@lookup ==3) {
                                $opt->{$lookup[0]}->{$lookup[1]}->{$lookup[2]} = $v;
                            }
                            else {
                                die "@lookup";
                            }
                        }
                        else {
                            die $a;
                        }
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
		elsif ($o eq '-l' || $o eq '--level') {
			if (@$args && $args->[0] !~ /^\-/)
			{	my $l = shift @$args;
				$opt->{level} = lc($l);
			}
		}
#		elsif ($o eq '-c' || $o eq '--db_counts') {
#			$opt->{db_counts} = 1;
#			require DBI;
#			require DBD::mysql;
#		}
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
	my ($opt, $errs) = @_;

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

	## level not configured
	if (! $opt->{level})
	{	$opt->{level} = 'm';
	}
	else
	{	if (! grep { $_ eq $opt->{level} } qw(s m l short medium long) )
		{	push @$errs, "the output level " . $opt->{level} . " is invalid. Valid options are 'short', 'medium' and 'long'";
		}
		## abbreviate the level designator
		$opt->{level} = substr($opt->{level}, 0, 1)
	}

	if (! $opt->{mode})
	{	$opt->{mode}{txt} = 1;
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
			## seond file not specified: use the current date instead
			if (! $opt->{r2} && ! $opt->{d2})
			{	my $temp = File::Temp->new();
				my $cmd = "perl " . $defaults->{dist_path} . "/bin/cvs-retriever.pl -v -o " . $temp->filename;
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
		{	if (-e $_ . "/" . $m . '_report.tmpl' || -e $_ . "/new_term_" . $m . ".tmpl")
			{	$pass->{$m}++;
#				last;
			}

		}
	}
	if (! $pass || scalar keys %{$opt->{mode}} != scalar keys %$pass)
	{	push @$errs, "could not find the template file; check the paths in \$defaults->{inc_path}";
		$logger->warn("found: " . Dumper($pass));
	}

#	if (! $opt->{subset} && ! $opt->{brief})
#	{	## no subset specified and in full text mode - must supply a subset
#		push @$errs, "specify a subset using -s <subset_name>";
#	}

	if ($errs && @$errs)
	{	$logger->logdie("Please correct the following parameters to run the script:\n" . ( join("\n", map { " - " . $_ } @$errs ) ) . "\nThe help documentation can be accessed with the command\n\t" . scr_name() . " --help");
	}
	return $opt;
}

## script name, minus path
sub scr_name {
	my $n = $0;
	$n =~ s/^.*\///;
	return $n;
}

=head1 AUTHOR

Amelia Ireland

=head1 SEE ALSO

L<GOBO::Graph>, L<GOBO::InferenceEngine>, L<GOBO::Doc::FAQ>

=cut
