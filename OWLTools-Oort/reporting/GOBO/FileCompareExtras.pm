package GOBO::FileCompareExtras;

use strict;
use DateTime::Format::Strptime;

sub get_config {
	my $dist_dir = shift;

	my $cfg = {
	## Configuration settings
	## path to CVS repository
	cvs_repo   => $ENV{HOME},
	## CVS command
	cvs_cmd    => 'cvs -q -d :ext:aji@ext.geneontology.org:/share/go/cvs',
	## file if r1/d1 and/or r2/d2 are specified
	f          => 'go/ontology/editors/gene_ontology_write.obo',
	## location of perl directory where the Logger.pm and this module are kept
	dist_path  => $dist_dir,
	## location of templates; paths should be separated by a colon
	inc_path   => "templates:$dist_dir/templates",
	## paths to files saved in the same place each time
	rss_path   => $ENV{HOME} . '/go/www/rss/',
	## path from cvs_repo to where the ontology reports are saved (for reporter.pl)
	report_dir => "go/internal-reports/ontology/",
	## temp dir holding saved ontology downloads (for reporter.pl)
#	temp_dir   => $ENV{HOME} . '/tmp/go_temp/',
	temp_dir   => $ENV{HOME} . '/temp/',

	## create html/text reports if the files are identical (uncomment to enable)
	report_identical => 1,
	## level to report by default for compare-obo-files
	level => 'm',

	## email settings
	email_from  => 'aireland@lbl.gov',
	email_to    => 'go-watchers@lists.stanford.edu',
#	email_to    => 'aireland@lbl.gov',
	email_bcc   => '',

	## database settings
	dbname      => $ENV{GO_DBNAME} || 'go_latest_lite',
	dbhost      => $ENV{GO_DBHOST} || 'spitz',
	dbuser      => $ENV{GO_DBUSER} || '',
	dbpass      => $ENV{GO_DBPASS} || '',
	dbport      => $ENV{GO_DBSOCKET} || '',
	dbdriver    => $ENV{GO_DBDRIVER} || 'mysql',

	html => {
		ontology_name => 'Gene Ontology',
		ontology_url  => 'http://geneontology.org/',
		webmaster     => 'webmaster@geneontology.org',
		## base dir for URLs in html
		install_dir => 'http://www.geneontology.org/',
		## browser links
		term_links => {
			amigo => {
				text => 'AmiGO',
				url_prefix => 'http://amigo.geneontology.org/cgi-bin/amigo/term_details?term=',
				url_suffix => '',
			},
			quickgo => {
				text => 'QuickGO',
				url_prefix => 'http://www.ebi.ac.uk/ego/DisplayGoTerm?id=',
				url_suffix => '',
			},
		},
		primary => {  ## copy / paste the details of the primary URL here if more than one
			text => 'AmiGO',
			url_prefix => 'http://amigo.geneontology.org/cgi-bin/amigo/term_details?term=',
			url_suffix => '',
		},
		assoc => {    ## URL for term associations
			text => 'AmiGO',
			url_prefix => 'http://amigo.geneontology.org/cgi-bin/amigo/term_assoc.cgi?term=',
			url_suffix => '',
		},
		## info page to learn more about reports
		report_info_url => 'http://www.geneontology.org/GO.ontology.reports.shtml',
	},
};

	return $cfg;
}


=head2 trim_rss

Input:
 file => $file_loc  ## location of rss file
 old  => $date      ## cut off date for rss items to be removed

Output:
 $old_data  ## scalar containing the data to be put in the new file

=cut

sub trim_rss {
	my %args = (@_);
	my $old_data;
	my $parser = DateTime::Format::Strptime->new(pattern => "%a, %d %b %Y %H:%M:%S %z");

	if (! $args{file} || ! $args{date})
	{	die "Please specify a file and a date";
	}

	if (! -e $args{file})
	{	warn "File " . $args{file} . " cannot be found!";
		return;
	}

	{	## open the file and get the items.
		local( $/, *NEW ) ;
		open( NEW, $args{file} ) or die "Could not open " . $args{file} . ": $!";
		my $text = <NEW>;
		my @items = split("<item>", $text);
		my @ok;
		my $guids = {};
		$items[-1] =~ s/\s*<\/channel>\s*<\/rss>//sm;

		foreach (@items)
		{	# we're looking at an item
			my $dt;
			if (/\<\/item>/s)
			{	$_ =~ s/\n+/\n/gm;
				$_ =~ s/(\<\/item>).*?/$1\n/s;
				if (/<pubDate>(.*?)<\/pubDate>/)
				{	$dt = $parser->parse_datetime( $1 );
					if ($dt < $args{date})
					{	next;
					}
				}

				if (/<guid>(.*?)<\/guid>/m)
				{	if ($guids->{$1})
					{	## we already have this entry
					}
					else
					{	$guids->{$1} = { date => $dt, item => '<item>' . $_ };
					}
				}
			}
		}
		if (values %$guids)
		{	$old_data = join "", map { $_->{item} } sort { $a->{date} < $b->{date} } values %$guids;
		}
		close( NEW );
	}
	return $old_data;
}

1;
