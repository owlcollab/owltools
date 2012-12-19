package GOBO::Logger;

use strict;
use Cwd;
use Log::Log4perl qw(get_logger :levels :no_extra_logdie_message);
use Log::Log4perl::Level;

use Exporter;
use vars qw(@ISA @EXPORT);
@ISA = qw(Exporter); # Log::Log4perl);
@EXPORT = qw( $ALL $TRACE $DEBUG $INFO $WARN $ERROR $FATAL $OFF );
use Carp;
use Data::Dumper;
my $logger;

#BEGIN {
#
#}

my $basic =
'class   = Log::Log4perl::Layout::PatternLayout
pattern = %p %M[%L]%n%m%n%n
time_pat = %d [%p] %M %l%n%m%n%n
';
my $stderr =
'## Log to STDERR
log4perl.appender.StdErr        = Log::Log4perl::Appender::Screen
log4perl.appender.StdErr.mode   = append
log4perl.appender.StdErr.layout = ${class}
log4perl.appender.StdErr.layout.ConversionPattern = ${pattern}
log4perl.appender.StdErr.stderr = 1
';
my $stdout =
'## Log to STDOUT (terminal)
log4perl.appender.Term        = Log::Log4perl::Appender::Screen
log4perl.appender.Term.mode   = append
log4perl.appender.Term.layout = ${class}
log4perl.appender.Term.layout.ConversionPattern = ${pattern}
log4perl.appender.Term.stderr = 0
';
my $text =
'log4perl.appender.Text          = Log::Log4perl::Appender::File
log4perl.appender.Text.filename = ' . $ENV{HOME} . '/galaxy-obo/log
log4perl.appender.Text.mode     = append
log4perl.appender.Text.layout   = ${class}
log4perl.appender.Text.layout.ConversionPattern = ${pattern}
log4perl.appender.Text.stderr   = 1
';

my $extras = {
	'stderr_thresh' => 'log4perl.appender.StdErr.Threshold = FATAL',
	'stdout_thresh' => 'log4perl.appender.Term.Threshold = FATAL',
	'text_thresh' => 'log4perl.appender.Text.Threshold = FATAL',
	'debug' => 'log4perl.logger = DEBUG, StdErr',
	'info' => 'log4perl.logger = INFO, StdErr',
	'warn' => 'log4perl.logger = WARN, StdErr',
	'fatal' => 'log4perl.logger = FATAL, StdErr',
	'fataltxt' => 'log4perl.logger = FATAL, Text',
	'onemsg' => 'log4perl.oneMessagePerAppender = 1',

};

my $config = {
	'galaxy' => join("\n", $basic, $text, $extras->{text_thresh}, $extras->{fataltxt}, $extras->{onemsg}),
	'verbose' => join("\n", $basic, $stderr, $stdout, $extras->{info}, $extras->{onemsg}),
	'standard' => join("\n", $basic, $stderr, $stdout, $extras->{warn}, $extras->{onemsg}),
	'debug' => join("\n", $basic, $stderr, $stdout, $extras->{debug}, $extras->{onemsg}),
};

INIT {
	my $init = 'standard';
	if ( $ENV{GO_VERBOSE} )
	{	$init = 'debug';
	}

#	print STDERR "Running INIT code...\n";
	Log::Log4perl::init( \$config->{$init} );
	$logger = get_logger();

	$SIG{__WARN__} = sub {
		local $Log::Log4perl::caller_depth = $Log::Log4perl::caller_depth + 1;
		if (@_)
		{
			$logger->warn("Warning!\n@_");
#			WARN @_;  ## this doesn't work!
#			Log::Log4perl::WARN( @_ );  ## nor does this.
		}
	};

	$SIG{__DIE__} = sub {
		if($^S) {
	# We're in an eval {} and don't want log
	# this message but catch it later
			return;
		}
#		local $Log::Log4perl::caller_depth =
		$Log::Log4perl::caller_depth++;
		$logger->logdie(@_);
#		LOGDIE @_;  ## this don't work
#		Log::Log4perl::LOGDIE( @_ );  ## this don't work neither
	};


}

sub init_with_config_str {
	my $conf = shift;
	Log::Log4perl::init( \$conf );
}

sub init_with_config {
	my $conf = shift;
	if (! $config->{$conf})
	{	$logger->error("$conf: config not found!");
	}
	else
	{	Log::Log4perl::init( \$config->{$conf} );
	}
}

1;

__END__

=head2 NAME

GOBO::Logger

=head2 DESCRIPTION

Wrapper for Log::Log4perl

=cut
