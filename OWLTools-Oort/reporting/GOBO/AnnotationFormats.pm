package GOBO::AnnotationFormats;

use strict;
use warnings;
use Data::Dumper;
use base 'Exporter';
our @EXPORT = qw(get_file_format get_gpi_spec get_gaf_spec get_gpad_spec transform can_transform write_errors);

=head2 GPI fields

Content	 Required?	 Cardinality	 Example
DB GP Form	 required	 1	 UniProtKB (database from which GP form comes)
DB GP Form ID	 required	 1	 P12345 (can be spliceform)
DB Object Symbol	 required	 1	 PHO3
DB Object Name	 optional	 0 or 1	 Toll-like receptor 4
DB Object Synonym	 optional	 0+, pipe-separated	 hToll|Tollbooth
DB Object Type	 required	 1	 protein
Taxon	 required	 1	 taxon:9606
Parent GP ID	 optional	 0 or 1
External GP xrefs	 optional	 0+, pipe-separated	 UniProtKB:P12345

=cut

my $gpi = {
	version => {
		major => '1',
		minor => '.0',
	},
	by_col => {
		gp_form_db => 1,
		gp_form_id => 2,
		gp_form_symbol => 3,
		gp_form_name => 4,
		gp_form_synonym => 5,
		gp_form_type => 6,
		taxon => 7,
		parent_gp_id => 8,
		gp_xrefs => 9,
	},
	in_order => [
	qw( gp_form_db
		gp_form_id
		gp_form_symbol
		gp_form_name
		gp_form_synonym
		gp_form_type
		taxon
		parent_gp_id
		gp_xrefs )
	],
};

sub get_gpi_spec {
	return $gpi;
}

=head2 GPAD fields

Content	 Required?	 Cardinality	 Example
DB GP Form	 required	 1	 UniProtKB (database from which GP form comes)
DB GP Form ID	 required	 1	 P12345 (can be spliceform)
Relationship	 required	 1	NOT part of
GO ID	 required	 1	 GO:0003993
Reference(s)	 required	 1 or greater	 PMID:2676709
Evidence Code ID	 required	 1	ECO:0000315
With (or) From	 optional	 0 or greater	 GO:0000346
Interacting taxon	 optional	 0 or 1	 9606
Date	 required	 1	 20090118
Assigned By	 required	 1	 SGD
Annotation XP	 optional	 0 or greater	part_of(CL:0000576)

=cut
my $gpad = {
	version => {
		major => '1',
		minor => '.0',
	},
	by_col => {
		gp_form_db => 1,
		gp_form_id => 2,
		relationship => 3,
		go_id => 4,
		reference => 5,
		eco_id => 6,
		with_from => 7,
		interacting_taxon => 8,
		date => 9,
		assigned_by => 10,
		annotation_xp => 11,
	},
	in_order => [
	qw( gp_form_db
		gp_form_id
		relationship
		go_id
		reference
		eco_id
		with_from
		interacting_taxon
		date
		assigned_by
		annotation_xp )
	],
};

sub get_gpad_spec {
	return $gpad;
}

=head2 GAF 2.0 fields

Column	 Content	 Required?	 Cardinality	 Example
1	DB	 required	 1	 UniProtKB
2	DB Object ID	 required	 1	 P12345
3	DB Object Symbol	 required	 1	 PHO3
4	Qualifier	 optional	 0 or greater	NOT
5	GO ID	 required	 1	 GO:0003993
6	DB:Reference (|DB:Reference)	 required	 1 or greater	 PMID:2676709
7	Evidence Code	 required	 1	IMP
8	With (or) From	 optional	 0 or greater	 GO:0000346
9	Aspect	 required	 1	 F
10	DB Object Name	 optional	 0 or 1	 Toll-like receptor 4
11	DB Object Synonym (|Synonym)	 optional	 0 or greater	 hToll|Tollbooth
12	DB Object Type	 required	 1	 protein
13	Taxon(|taxon)	 required	 1 or 2	 taxon:9606
14	Date	 required	 1	 20090118
15	Assigned By	 required	 1	 SGD
16	Annotation Extension	 optional	 0 or greater	part_of(CL:0000576)
17	Gene Product Form ID	 optional	 0 or 1	 UniProtKB:P12345-2

=cut

my $gaf = {
	version => {
		major => '2',
		minor => '.0',
	},
	by_col => {
		db => 1,
		db_object_id => 2,
		db_object_symbol => 3,
		qualifier => 4,
		go_id => 5,
		reference => 6,
		evidence_code => 7,
		with_from => 8,
		aspect => 9,
		db_object_name => 10,
		db_object_synonym => 11,
		db_object_type => 12,
		taxon_int_taxon => 13,
		date => 14,
		assigned_by => 15,
		annotation_xp => 16,
		gp_object_form_id => 17,
	},
	in_order => [
	qw( db
		db_object_id
		db_object_symbol
		qualifier
		go_id
		reference
		evidence_code
		with_from
		aspect
		db_object_name
		db_object_synonym
		db_object_type
		taxon_int_taxon
		date
		assigned_by
		annotation_xp
		gp_object_form_id )
	],
};

sub get_gaf_spec {
	return $gaf;
}


sub get_file_format {
	my $f = shift;
	my ($format, $major, $minor);

	open (F, "< $f") or die "Unable to open $f for reading: $!";
	# loop until we find the first non-blank, non-comment line or a file format tag
	while (<F>) {
		next unless /\w/;
		if (/!\s*(gaf|gpad|gpi)-version:\s*((\d)(\.(\d))?)/) {
#			print STDERR "Found $_!\n";
			$format = $1;
			$major = $3;
			$minor = $4 || '.0';  ## hack due to Perl's stupidity with zeroes.
			last;
		}
		last if $_ !~ /^!/;
	}
	close F;
	return ($format, $major, $minor);
}

## Stuff for the conversion of GPAD + GPI => GAF 2.0
# hash that maps ECO identifiers to GO evidence codes
my $eco2ev = {
	'ECO:0000021' => 'IPI',
	'ECO:0000031' => 'ISS',
	'ECO:0000084' => 'IGC',
	'ECO:0000203' => 'IEA',
	'ECO:0000245' => 'RCA',
	'ECO:0000247' => 'ISA',
	'ECO:0000250' => 'ISS',
	'ECO:0000255' => 'ISM',
	'ECO:0000256' => 'IEA',
	'ECO:0000265' => 'IEA',
	'ECO:0000266' => 'ISO',
	'ECO:0000269' => 'EXP',
	'ECO:0000270' => 'IEP',
	'ECO:0000303' => 'NAS',
	'ECO:0000304' => 'TAS',
	'ECO:0000305' => 'IC',
	'ECO:0000307' => 'ND',
	'ECO:0000314' => 'IDA',
	'ECO:0000315' => 'IMP',
	'ECO:0000316' => 'IGI',
	'ECO:0000317' => 'IGC',
	'ECO:0000318' => 'IBA',
	'ECO:0000319' => 'IBD',
	'ECO:0000320' => 'IKR',
	'ECO:0000321' => 'IRD',
};


# hash that maps relation to ontology and (where appropriate) the GAF 2.0 equivalent
my $rln2qual = {
	contributes_to => 'contributes_to',
	colocalizes_with => 'colocalizes_with',
	'not' => 'NOT',
	'NOT' => 'NOT',
};

# hash that maps GO evidence codes to (reference-specific) ECO identifiers
my $ev2eco = {
	IEA => {
		'GO_REF:0000002' => 'ECO:0000256',
		'GO_REF:0000003' => 'ECO:0000265',
		'GO_REF:0000004' => 'ECO:0000203',
		'GO_REF:0000019' => 'ECO:0000265',
		'GO_REF:0000020' => 'ECO:0000256',
		'GO_REF:0000023' => 'ECO:0000203',
		'GO_REF:0000035' => 'ECO:0000265',
		default => 'ECO:0000203'
	},
	ND => { default => 'ECO:0000307' },
	EXP => { default => 'ECO:0000269' },
	IDA => { default => 'ECO:0000314' },
	IMP => { default => 'ECO:0000315' },
	IGI => { default => 'ECO:0000316' },
	IEP => { default => 'ECO:0000270' },
	IPI => { default => 'ECO:0000021' },
	TAS => { default => 'ECO:0000304' },
	NAS => { default => 'ECO:0000303' },
	IC => { default => 'ECO:0000305' },
	ISS => {
		'GO_REF:0000011' => 'ECO:0000255',
		'GO_REF:0000012' => 'ECO:0000031',
		'GO_REF:0000018' => 'ECO:0000031',
		'GO_REF:0000027' => 'ECO:0000031',
		default => 'ECO:0000250'
	},
	ISO => { default => 'ECO:0000266' },
	ISA => { default => 'ECO:0000247' },
	ISM => { default => 'ECO:0000255' },
	IGC => {
		'GO_REF:0000025' =>	'ECO:0000084',
		default => 'ECO:0000317'
	},
	IBA => { default => 'ECO:0000318' },
	IBD => { default => 'ECO:0000319' },
	IKR => { default => 'ECO:0000320' },
	IRD => { default => 'ECO:0000321' },
	RCA => { default => 'ECO:0000245' },
	IMR => { default => 'ECO:0000320' }
};

# hash containing the default relations for each ontology
my $aspect2rln = {
	P => 'actively_participates_in',
	F => 'actively_participates_in',
	C => 'part_of',
	default => 'annotated_to',
};

my $qual_order = [
	'NOT',
	'colocalizes_with',
	'contributes_to',
];

# hash that maps relation to ontology and (where appropriate) the GAF 2.0 equivalent
my $relations = {
# cellular_component
	part_of => { aspect => 'C', gaf_equivalent => '' },
	colocalizes_with => { aspect => 'C', gaf_equivalent => 'colocalizes_with' },
	active_in => { aspect => 'C' },
	transported_by => { aspect => 'C' },
	posttranslationally_modified_in => { aspect => 'C' },
	located_in_other_organism => { aspect => 'C' },
	located_in_host => { aspect => 'C' },
	member_of => { aspect => 'C' },
	intrinsic_to => { aspect => 'C' },
	extrinsic_to => { aspect => 'C' },
	spans => { aspect => 'C' },
	partially_spans => { aspect => 'C' },
# molecular_function
#	actively_participates_in => { aspect => 'F',  gaf_equivalent => '' },
	contributes_to => { aspect => 'F', gaf_equivalent => 'contributes_to' },
	functions_in_other_organism => { aspect => 'F' },
	functions_in_host => { aspect => 'F' },
	substrate_of => { aspect => 'F' },
# biological_process
#	actively_participates_in => { aspect => 'P', gaf_equivalent => '' }
};

my $transforms = {
	## creating GAF from GPI and GPAD files
	## GPAD int taxon + GPI taxon => GAF taxon int taxon
	'taxon_int_taxon' => sub {
		my %args = (@_);
		## concatenate gpi taxon and gpad int_taxon
		if ($args{gpad_data}->[ $gpad->{by_col}{interacting_taxon} ] ne "")
		{	return join('|', map { "taxon:$_" } ($args{metadata}->{by_id}{$args{id}}[ $gpi->{by_col}{taxon} ], $args{gpad_data}->[ $gpad->{by_col}{interacting_taxon} ]) );
		}
		else
		{	#if (! $args{metadata}->{by_id}{$args{id}}[ $gpi->{by_col}{taxon} ])
			#{
			#	print STDERR "gpi data for id $args{id}: " . Dumper($args{metadata}->{by_id}{$args{id}});
			#}
			return "taxon:" . $args{metadata}->{by_id}{$args{id}}[ $gpi->{by_col}{taxon} ];
		}
	},
	'aspect' => sub {
		my %args = (@_);
		## find out the term and get the ontology data
		## could also add something here to get this data from the relationship

		if (defined $args{ontology}->{ $args{gpad_data}->[$gpad->{by_col}{go_id}] })
		{	return $args{ontology}->{ $args{gpad_data}->[$gpad->{by_col}{go_id}] };
		}
		else
		{	${$args{errs}}->{gpad}{no_aspect}{ $args{gpad_data}->[$gpad->{by_col}{go_id}] }++;
			return "";
		}
	},
	'evidence_code' => sub {
		my %args = (@_);
		## check our ECO ID and find what it translates into
		if ( $eco2ev->{ $args{gpad_data}->[ $gpad->{by_col}{eco_id} ] })
		{	return $eco2ev->{ $args{gpad_data}->[ $gpad->{by_col}{eco_id} ]};
		}
		else
		{	${$args{errs}}->{gpad}{no_ev_code}{ $args{gpad_data}->[ $gpad->{by_col}{eco_id} ]}++;
			${$args{errs}}->{line_err}++;
			return "";
		}
	},
	'qualifier' => sub {
		my %args = (@_);
		## check our relations and see how many we can translate back into GAF 2.0 language
		my @rlns = split(/[ \|]/, $args{gpad_data}->[ $gpad->{by_col}{relationship} ]);
		my $qual;
		foreach (@rlns)
		{	if ($rln2qual->{$_})
			{	push @$qual, $rln2qual->{$_};
			}
			else
			{	if ($_ ne 'actively_participates_in' && $_ ne 'part_of')
				{	print STDERR "Could not find rln2qual data for $_!\n";
					${$args{errs}}->{line_err}++;
					${$args{errs}}->{gpad}{unknown_qual}{$_}++;
				}
			}
		}

		if ($qual && @$qual)
		{	return join('|', @$qual);
		}
		return '';
	},
	'db' => sub {
		my %args = (@_);
		##
		my ($db, $ref);
		if ($args{parent} =~ /\w/)
		{	## put in the parent id
			($db, $ref) = split /:/, $args{parent}, 2;
		}
		else
		{	#if (! $args{metadata}->{by_id}{ $args{id} }[ $gpi->{by_col}{gp_form_db} ])
			#{
			#	print STDERR "gpi data for id $args{id}: " . Dumper($args{metadata}->{by_id}{ $args{id} });
			#}

			($db, $ref) = split /:/, $args{metadata}->{by_id}{ $args{id} }[ $gpi->{by_col}{gp_form_db} ], 2;
		}
		return $db;
	},
	'db_object_id' => sub {
		my %args = (@_);
		##
		if ($args{parent} =~ /\w/)
		{	## put in the parent id
			my ($db, $ref) = split /:/, $args{parent}, 2;
			return $ref;
		}
		else
		{	return $args{metadata}->{by_id}{ $args{id} }->[ $gpi->{by_col}{gp_form_id} ];
		}
	},
	'gp_object_form_id' => sub {
		my %args = (@_);
		if ($args{parent} =~ /\w/)
		{	## return the child ID
			return $args{id};
		}
		return;
	},
	'db_object_symbol' => sub {
		my %args = (@_);
		if ($args{parent} =~ /\w/)
		{	## use the parent data
			if ($args{metadata}->{by_id}{$args{parent}})
			{	return $args{metadata}->{by_id}{$args{parent}}[ $gpi->{by_col}{gp_form_symbol} ] || '';
			}
		}
		return $args{metadata}->{by_id}{$args{id}}[ $gpi->{by_col}{gp_form_symbol} ] || '';
	},
	'db_object_name' => sub {
		my %args = (@_);
		if ($args{parent} =~ /\w/)
		{	## use the parent data
			if ($args{metadata}->{by_id}{$args{parent}})
			{	return $args{metadata}->{by_id}{$args{parent}}[ $gpi->{by_col}{gp_form_name} ] || '';
			}
		}
		return $args{metadata}->{by_id}{$args{id}}[ $gpi->{by_col}{gp_form_name} ] || '';
	},
	'db_object_synonym' => sub {
		my %args = (@_);
		if ($args{parent} =~ /\w/)
		{	## use the parent data
			if ($args{metadata}->{by_id}{$args{parent}})
			{	return $args{metadata}->{by_id}{$args{parent}}[ $gpi->{by_col}{gp_form_synonym} ] || '';
			}
		}
		return $args{metadata}->{by_id}{$args{id}}[ $gpi->{by_col}{gp_form_synonym} ] || '';
	},
	'db_object_type' => sub {
		my %args = (@_);
		if ($args{parent} =~ /\w/)
		{	## use the parent data
			if ($args{metadata}->{by_id}{$args{parent}})
			{	return $args{metadata}->{by_id}{$args{parent}}[ $gpi->{by_col}{gp_form_type} ] || '';
			}
		}
		return $args{metadata}->{by_id}{$args{id}}[ $gpi->{by_col}{gp_form_type} ] || '';
	},

	## GAF => GPAD + GPI
	'gp_form_db' => sub {
		my %args = (@_);
		if ($args{gaf_data}->[ $gaf->{by_col}{gp_object_form_id} ])
		{	## remove the db, return
			my ($db, $key) = split /:/, $args{gaf_data}->[ $gaf->{by_col}{gp_object_form_id} ], 2;
			return $db;
		}
		return $args{gaf_data}->[ $gaf->{by_col}{db} ];
	},
	'gp_form_id' => sub {
		my %args = (@_);
		if ($args{gaf_data}->[ $gaf->{by_col}{gp_object_form_id} ])
		{	## remove the db, return
			my ($db, $key) = split /:/, $args{gaf_data}->[ $gaf->{by_col}{gp_object_form_id} ], 2;
			if ($key)
			{	return $key;
			}
		}
		return $args{gaf_data}->[ $gaf->{by_col}{db_object_id} ];
	},
	## GPI specific
	'gp_xrefs' => sub {
		return '';
	},
	'gp_form_symbol' => sub {
		my %args = (@_);
		return $args{gaf_data}->[ $gaf->{by_col}{db_object_symbol} ] || '';
	},
	'gp_form_name' => sub {
		my %args = (@_);
		return $args{gaf_data}->[ $gaf->{by_col}{db_object_name} ] || '';
	},
	'gp_form_synonym' => sub {
		my %args = (@_);
		return $args{gaf_data}->[ $gaf->{by_col}{db_object_synonym} ] || '';
	},
	'gp_form_type' => sub {
		my %args = (@_);
		return $args{gaf_data}->[ $gaf->{by_col}{db_object_type} ] || '';
	},
	'taxon' => sub {
		my %args = (@_);
		## split up the taxon and interacting taxon
		my @taxa = map { s/taxon://g; $_ } split(/\|/, $args{gaf_data}->[ $gaf->{by_col}{taxon_int_taxon} ]);
		return $taxa[0];
	},
	'parent_gp_id' => sub {
		my %args = (@_);
		if ($args{gaf_data}->[ $gaf->{by_col}{gp_object_form_id} ])
		{	## this is a spliceform
			return $args{gaf_data}->[ $gaf->{by_col}{db} ] .":". $args{gaf_data}->[ $gaf->{by_col}{db_object_id} ];
		}
		return '';
	},
	## GPAD specific
	'interacting_taxon' => sub {
		my %args = (@_);
		## split up the taxon and interacting taxon
		my @taxa = map { s/taxon://g; $_ } split(/\|/, $args{gaf_data}->[ $gaf->{by_col}{taxon_int_taxon} ]);
#		print STDERR "taxa: " . Dumper(\@taxa);
		return $taxa[1] || '';
	},
	'eco_id' => sub {
		my %args = (@_);
		my $ev = $args{gaf_data}->[ $gaf->{by_col}{evidence_code} ];
		my $ref = $args{gaf_data}->[ $gaf->{by_col}{reference} ];
		# translate the evidence code into the appropriate ECO identifier
		if ($ev2eco->{$ev})
		{	return $ev2eco->{$ev}{$ref} || $ev2eco->{$ev}{'default'};
		}
		else
		{	${$args{errs}}->{gaf}{no_eco_id}{$ev}++;
			${$args{errs}}->{line_err}++;
			return '';
		}
	},
	'relationship' => sub {
		my %args = (@_);
		my @quals = split(/\|/, $args{gaf_data}->[ $gaf->{by_col}{qualifier} ]);
		my @rlns;
		if (@quals)
		{	## need to sort these!!
			foreach my $q (@$qual_order)
			{	if (grep { /^$q$/i } @quals)
				{	push @rlns, $q;
					last if scalar @quals == 1;
				}
			}
		}
		## get the term aspect and add the relationship
		if (! $aspect2rln->{ $args{gaf_data}->[ $gaf->{by_col}{aspect} ] })
		{	${$args{errs}}->{gaf}{invalid_aspect}{ $gaf->{by_col}{aspect} }++;
			push @rlns, $aspect2rln->{ 'default' };
		}
		else
		{	push @rlns, $aspect2rln->{ $args{gaf_data}->[ $gaf->{by_col}{aspect} ] };
		}
		return join('|', @rlns);
	},
};

sub transform {
	my $tfm = shift;
	if (! $transforms->{$tfm})
	{	warn "$tfm: no such transform!";
		return;
	}
	return $transforms->{$tfm}(@_);
}

sub can_transform {
	my $tfm = shift;
	return 1 if $transforms->{$tfm};
	return;
}

##	write_errors( errs => $errs, options => $opt, logger => $logger );

sub write_errors {
	my %args = (@_);
	my $msg;

	return unless $args{errs} && keys %{$args{errs}};
	my $errs = $args{errs};

	if ($errs->{gaf})
	{	$msg .= "Errors in GAF file\n";

		foreach my $key (keys %{$errs->{gaf}})
		{	##
			if ($key eq 'unknown_gpi_col')
			{	$msg .= "Unknown column found when creating GPI file. Affects:\n" . join(", ", sort keys %{$errs->{gaf}{unknown_gpi_col}}) . "\n\n";
			}
			elsif ($key eq 'unknown_gpad_col')
			{	$msg .= "Unknown column found when creating GPAD file. Affects:\n" . join(", ", sort keys %{$errs->{gaf}{unknown_gpad_col}}) . "\n\n";
			}
			elsif ($key eq 'no_eco_id')
			{	$msg .= "No corresponding ECO IDs found for the following evidence codes:\n" . join(", ", sort keys %{$errs->{gaf}{no_eco_id}}) . "\n\n";
			}
			elsif ($key eq 'invalid_aspect')
			{	$msg .= "Invalid aspect found in GAF file:\n" . join(", ", sort keys %{$errs->{gaf}{invalid_aspect}} ) . "\n\n";
			}
			elsif ($key eq 'too_many_children')
			{	$msg .= "Parent GP was not in GAF and more than one child was found; did not know where to get the GP info from. Affects:\n" . join(", ", sort keys %{$errs->{gaf}{too_many_children}} ) . "\n\n";
			}
			else
			{	$msg .= "Unexplained error of type $key found\n\n";
			}
		}
		$msg .= "\n\n";
		delete $errs->{gaf};
	}

	if ($errs->{gpi})
	{	$msg .= "Errors in GPI file\n";
		foreach my $key (keys %{$errs->{gpi}})
		{	if ($key eq 'too_many_parents')
			{	$msg .= "More than one parent_gp_id found; most common parent ID used. Affects:\n" . join(", ", sort keys %{$errs->{gpi}{too_many_parents}} ) . "\n\n";
			}
			elsif ($key eq 'no_metadata')
			{	$msg .= "No metadata found for parent_gp_id; affects:\n" . join(", ", sort keys %{$errs->{gpi}{no_metadata}}) . "\n\n";
			}
			else
			{	$msg .= "Unexplained error of type $key found\n\n";
			}
		}
		$msg .= "\n\n";
		delete $errs->{gpi};
	}

	if ($errs->{gpad})
	{	$msg .= "Errors in GPAD file\n";

		foreach my $key (keys %{$errs->{gpad}})
		{
			if ($key eq 'no_aspect')
			{	$msg .= "Found terms with no namespace or that are obsolete:\n" . join(", ", sort keys %{$errs->{gpad}{no_aspect}} ) . "\n\n";
			}
			elsif ($key eq 'no_ev_code')
			{	$msg .= "No mapping for the following ECO IDs:\n" . join(", ", sort keys %{$errs->{gpad}{no_ev_code}} ) . "\n\n";
			}
			elsif ($key eq 'unknown_qual')
			{	$msg .= "No mapping for the following qualifiers:\n" . join(", ", sort keys %{$errs->{gpad}{no_ev_code}} ) . "\n\n";
			}
			else
			{	$msg .= "Unexplained error of type $key found\n\n";
			}
		}
		$msg .= "\n\n";
		delete $errs->{gpad};
	}

	if ($errs->{ont})
	{	$msg .= "Errors in ontology file\n";

		foreach my $key (keys %{$errs->{ont}})
		{	if ($key eq 'no_id')
			{	$msg .= "Found " . (scalar @{$errs->{ont}{no_id}}) . " term stanzas with no ID\n";
			}
			elsif ($key eq 'no_ns')
			{	$msg .= "Found " . (scalar @{$errs->{ont}{no_ns}}) . " term stanzas with no namespace:\n" . join(", ", @{$errs->{ont}{no_ns}} ) . "\n\n";
			}
			elsif ($key eq 'invalid_ns')
			{	$msg .= "Found term stanzas with an invalid namespace:\n" . join("\n", map { "$_: " . join(", ", @{$errs->{ont}{invalid_ns}{$_}}) } sort keys %{$errs->{ont}{invalid_ns}} )  . "\n\n";
			}
			else
			{	$msg .= "Unexplained error of type $key found\n\n";
			}
		}

		$msg .= "\n\n";
		delete $errs->{ont};
	}

	if (keys %$errs)
	{	foreach (keys %$errs)
		{	$msg .= "Unexplained error of type $_ found\n";
		}
	}

	if ($args{options}->{log_fh})
	{	my $fh = $args{options}->{log_fh};
		print $fh $msg;
	}
	else
	{	$args{logger}->error( $msg );
	}

}

1;
