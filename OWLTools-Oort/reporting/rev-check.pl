#!/usr/bin/perl -w

=head1 NAME

rev_check.pl - compare two OBO files

=head1 SYNOPSIS

 rev_check.pl

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

use File::Temp;
use GOBO::Logger;
use GOBO::FileCompareExtras;

my $logger;
my $defaults = GOBO::FileCompareExtras::get_config( $dist_dir );
if (! $defaults)
{	die("No default settings found. Dying");
}

$defaults->{output} = 'onto_errs-1-2000.txt';

run_script($defaults, []); #\@ARGV);

exit(0);

sub run_script {
	my $options = parse_options(@_);
	$logger->info( "Starting script..." );

	my $first = '1';
	my $last = '2455';
#	my $last = '5';
	my $data;

	open(OUT, "> " . $options->{output}) or die("Could not open " . $options->{output} . ": $!");

	my $this = $first;
	## fetch the data from the first file
	my $ref = get_file_data( "1.$this", $options);
	$data->{"1.$first"} = $$ref;
	my $next = $this + 1;

	## parse the 'first' file.
	while ($next <= $last)
	{	## retrieve the files from CVS
		my $d_ref = get_file_data( "1.$next", $options);
		$data->{"1.$next"} = $$d_ref;

		## compare the two files, look for errors
		my $t;

		## check whether previous errors have been righted
		if ($data->{lost_term} && keys %{$data->{lost_term}})
		{	## is it in the term list?
			foreach $t (sort keys %{$data->{lost_term}})
			{	if ($data->{"1.$next"}{Term}{$t})
				{	print OUT "1.$next: restored lost term $t\n";
					delete $data->{lost_term}{$t};
				}
				elsif ($data->{"1.$next"}{alt_id}{$t})
				{	print OUT "1.$next: lost term $t is now an alt_id\n";
					delete $data->{lost_term}{$t};
				}
			}
			if (! keys %{$data->{lost_term}})
			{	delete $data->{lost_term};
			}
		}

		if ($data->{lost_typedef} && keys %{$data->{lost_typedef}})
		{	foreach $t (sort keys %{$data->{lost_typedef}})
			{	if ($data->{"1.$next"}{Typedef}{$t})
				{	print OUT "1.$next: restored lost typedef $t\n";
					delete $data->{lost_typedef}{$t};
				}
			}
			if (! keys %{$data->{lost_typedef}})
			{	delete $data->{lost_typedef};
			}
		}

		if ($data->{unobsolete} && keys %{$data->{unobsolete}})
		{	foreach $t (sort keys %{$data->{unobsolete}})
			{	if ($data->{"1.$next"}{is_obsolete}{$t})
				{	print OUT "1.$next: unobsoleted term $t now obsolete. Phew!\n";
					delete $data->{unobsolete}{$t};
				}
			}
			if (! keys %{$data->{unobsolete}})
			{	delete $data->{unobsolete};
			}
		}
		if ($data->{alt_id_to_term} && keys %{$data->{alt_id_to_term}})
		{	foreach $t (sort keys %{$data->{alt_id_to_term}})
			{	if ($data->{"1.$next"}{alt_id}{$t})
				{	print OUT "1.$next: ex-alt_id-turned-term $t is now an alt_id again\n";
					delete $data->{alt_id_to_term}{$t};
				}
			}
			if (! keys %{$data->{alt_id_to_term}})
			{	delete $data->{alt_id_to_term};
			}
		}

		if ($data->{lost_alt_id} && keys %{$data->{lost_alt_id}})
		{
			foreach $t (sort keys %{$data->{lost_alt_id}})
			{	if ($data->{"1.$next"}{alt_id}{$t})
				{	print OUT "1.$next: restored lost alt_id $t\n";
					delete $data->{lost_alt_id}{$t};
				}
				elsif ($data->{"1.$next"}{alt_id}{$t})
				{	print OUT "1.$next: lost alt_id $t is now a term\n";
					delete $data->{lost_alt_id}{$t};
					$data->{alt_id_to_term}{$t}++;
				}
			}
			if (! keys %{$data->{lost_alt_id}})
			{	delete $data->{lost_alt_id};
			}
		}



		## check that no terms have been lost
		foreach $t (sort keys %{$data->{"1.$this"}{Term}})
		{	if (! $data->{"1.$next"}{Term}{$t} && ! $data->{"1.$next"}{alt_id}{$t})
			{	## lost it!
				print OUT "1.$next: $t has been lost\n";
				$data->{lost_term}{$t}++;
			}
		}

		## check no typedefs have been lost
		foreach $t (sort keys %{$data->{"1.$this"}{Typedef}})
		{	if (! $data->{"1.$next"}{Typedef}{$t})
			{	print OUT "1.$next: typedef $t has been lost\n";
				$data->{lost_typedef}{$t}++;
			}
		}

		## check for obsolete ==> extant
		foreach $t (sort keys %{$data->{"1.$this"}{is_obsolete}})
		{	if (! $data->{"1.$next"}{is_obsolete}{$t})
			{	print OUT "1.$next: $t is no longer obsolete\n";
				$data->{unobsolete}{$t}++;
			}
		}

		## check for alt_id ==> id
		foreach $t (sort keys %{$data->{"1.$this"}{alt_id}})
		{	if (! $data->{"1.$next"}{alt_id}{$t})
			{	if ($data->{"1.$next"}{Term}{$t})
				{	print OUT "1.$next: $t was an alt_id but is now an ID\n";
					$data->{alt_id_to_term}{$t}++;
				}
				else
				{	print OUT "1.$next: lost alt_id $t\n";
					$data->{lost_alt_id}{$t}++;
				}
			}
		}

		print OUT "Processed 1.$this\n";
#		print OUT "Dump of 1.$this: " . Dumper($data->{"1.$this"}) . "\n\n";

		delete $data->{"1.$this"};
		## increase this and next by one each.
		$this++;
		$next++;
	}

}


sub get_file_data {
	my ($r, $opt) = @_;

	my $temp = File::Temp->new();

	my $cmd = "perl " . $defaults->{dist_path} . "/bin/cvs-retriever.pl -r " . $r. " -o " . $temp->filename;

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
	{	$logger->logdie("CVS problem: $@");
	}

#	my $temp = '/Users/gwg/go/ontology/editors/gene_ontology_write.obo.'. $r;
	my $h;
	## $temp is now the ontology file we want. Woohoo!
	## pull in the ontology data from the file to be committed
	open(FH, "<" . $temp) or die("Could not open revision $r: $!");
	{
		local $/ = "\n[";
		# remove and parse the header
		## we're pulling in chunks of text, separated by \n[
		while (<FH>)
		{	if (/^(\S+)\]\s*.*?^id:\s*(\S+)/sm)
			{	# store the data as a tag-value hash indexed by stanza type and id
				# data->{$file}{$stanza_type}{$stanza_id}
				my $type = $1;
				my $id = $2;
## Data to save:
#	is_a, relationship
#	intersection_of, union_of, disjoint_from
#	is_obsolete
#	replaced_by, consider
				if ($type eq 'Term')
				{
					while (/^relationship: (\S+) (\S+)/gm)
					{	$h->{e}{$1}{$2}++;
						$h->{all_terms}{$2}++;
					}
					while (/^is_a: (\S+)/gm)
					{	$h->{e}{is_a}{$1}++;
						$h->{all_terms}{$1}++;
					}
					if (/^is_obsolete: true/m)
					{	$h->{is_obsolete}{$id}++;
					}
					while (/^(replaced_by|consider|alt_id): (\S+)/gm)
					{	$h->{$1}{$2}++;
					}
					$h->{Term}{$id}++;
				}
				elsif ($type eq 'Typedef')
				{	if (/^name: (.+)$/s)
					{	$h->{$type}{$id} = $1;
					}
					else
					{	$h->{$type}{$id}++;
					}
				}
			}
		}
	}
	close(FH);

	foreach (keys %{$h->{alt_id}})
	{	if ($h->{Term}{$_})
		{	$logger->warn("$r: $_ is a term ID and an alt_id");
		}
	}

	foreach (keys %{$h->{e}})
	{	next if $_ eq 'is_a'; ## built in
		if (! $h->{Typedef}{$_})
		{	$logger->warn("$r: no Typedef for $_");
		}
	}

	foreach my $n qw(consider replaced_by)
	{	foreach my $id (keys %{$h->{$n}})
		{	if ($h->{is_obsolete}{$id})
			{	$logger->warn("$r: $n term $id is obsolete!");
			}
			if ($h->{alt_id}{$id})
			{	$logger->warn("$r: $n term $id is an alt_id!");
			}
		}
	}

	foreach my $t (keys %{$h->{all_terms}})
	{	if (! $h->{Term}{$t})
		{	$logger->warn("$r: no term stanza for $t");
		}
		elsif ($h->{is_obsolete}{$t})
		{	$logger->warn("$r: link to obsolete term $t");
		}
		elsif ($h->{alt_id}{$t})
		{	$logger->warn("$r: link to alt_id $t");
		}
	}

	return \$h;
}

sub parse_options {
	my ($opt, $args) = @_;
	my $errs;

	if ($args)
	{
		while (@$args && $args->[0] =~ /^\-/) {
			my $o = shift @$args;
			if ($o eq '-h' || $o eq '--help') {
				system("perldoc", $0);
				exit(0);
			}
			elsif ($o eq '-v' || $o eq '--verbose') {
				$opt->{verbose} = 1;
			}
			elsif ($o eq '--galaxy') {
				$opt->{galaxy} = 1;
			}
			elsif ($o eq '-o' || $o eq '--output') {
				if (@$args && $args->[0] !~ /^\-/)
				{	$opt->{output} = shift @$args;
				}
			}
			else {
				push @$errs, "Ignored nonexistent option $o";
			}
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
