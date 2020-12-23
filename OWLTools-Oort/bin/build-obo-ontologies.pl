#!/usr/bin/perl -w
use strict;
use YAML::Syck;

# For documentation, see usage() method, or run with "-h" option

my %selection = ();  # subset of ontologies to run on (defaults to all)
my %omit = ();  # subset of ontologies to omit (defaults to all)
my $dry_run = 0;     # do not deploy if dry run is set
my $target_dir = './deployed-ontologies';  # in a production setting, this would be a path to web-visible area, e.g. berkeley CDN or NFS
my $is_compare_obo = 0;
my $email = '';
my $registry_url = "https://raw.githubusercontent.com/OBOFoundry/OBOFoundry.github.io/master/registry/ontologies.yml";

while ($ARGV[0] && $ARGV[0] =~ /^\-/) {
    my $opt = shift @ARGV;
    print STDERR "OPT: $opt\n";

    if ($opt eq '-h' || $opt eq '--help') {
        print &usage();
        exit 0;
    }
    elsif ($opt eq '-s' || $opt eq '--select') {
        $selection{shift @ARGV} = 1;
    }
    elsif ($opt eq '-x' || $opt eq '--omit') {
        $omit{shift @ARGV} = 1;
    }
    elsif ($opt eq '-t' || $opt eq '--target-dir') {
        $target_dir = shift @ARGV;
    }
    elsif ($opt eq '-d' || $opt eq '--dry-run') {
        $dry_run = 1;
    }
    elsif ($opt eq '-c' || $opt eq '--compare-obo') {
        $is_compare_obo = 1;
    }
    elsif ($opt eq '-r' || $opt eq '--registry') {
        $registry_url = shift @ARGV;
        
    }
    elsif ($opt eq '-e' || $opt eq '--email') {
        $email = shift @ARGV;
    }
    else {
        die "unknown option: $opt";
    }
}

if (@ARGV) {
    die "unprocessed command line arguments: @ARGV";
}

my %ont_info = ();


# Build-in registry
%ont_info = get_ont_info();

# set up dir structure if not present
if (!(-d 'src')) {
    run("mkdir src");
}
if (!(-d 'failed-builds')) {
    run("mkdir failed-builds");
}
if (!(-d $target_dir)) {
    run("mkdir $target_dir");
}

# --GLOBALS--
my $ont;  # current ontology. Always an ontology ID such as 'go', cl', ...
my $n_errs = 0;   # total errs found
my @errs = ();    # err objects
my @onts_to_deploy = ();   # ont IDs that were successful
my @failed_onts = ();   # ont IDs that fail
my @failed_infallible_onts = ();   # ont IDs that fail that cause an error

# --MAIN--
# Iterate over all ontologies attempting to build or mirror
# - external data is first pulled into a staging area
# - for methods that employ a conversion, a check is perfomed to see if the source has changed
# - if successful, built ontologies are copied to deployment area at end of iteration
foreach my $k (keys %ont_info) {
    $ont = $k;

    if (keys %selection) {
        next unless $selection{$ont};
    }
    if (keys %omit) {
        next if $omit{$ont};
    }
    debug("ONTOLOGY: $ont");

    my $info = $ont_info{$ont};
    my $method = lc($info->{method});
    my $source_url = $info->{source_url};

    my $success = 0;

    # Method: vcs -- Version Control System - mirror package directly from svn/git checkout/update
    if ($method eq 'vcs') {

        my $system = $info->{system} || 'svn';

        # we always checkout into a staging dir
        my $dir = "stage-$system-$ont";

        if (-d $dir) {
            # already checked out - issue update
            my $cmd = $info->{update};
            if (!$cmd) {
                if ($system eq 'svn') {
                    $cmd = 'svn --ignore-externals update';
                }
                elsif ($system eq 'git') {
                    $cmd = 'git pull';
                }
                else {
                    die "$system not known";
                }
            }
            $success = run("cd $dir && $cmd");
        }
        else {
            # initial checkout
            my $cmd = $info->{checkout};
            if ($cmd) {
                if ($cmd =~/svn.*https/) {
                    debug("WARNING: svn URL includes https - possible config error?");
                }
                $success = run("$cmd $dir");
            }
            else {
                $success = 0;
                debug("Config error: checkout not set for $ont");
            }
        }

        # allow optional subdir. E.g. if we check out to project root, we may want to copy from src/ontology to target
        my $srcdir = $dir;
        if ($info->{path}) {
            $srcdir .= "/".$info->{path};
        }

        # post-processing: TODO - add this for other commands
        if ($success && $info->{post_processing_command}) {
            $success = run("cd $srcdir && $info->{post_processing_command}");
        }

        # copy from staging checkout area to target
        if ($success) {
            $success = run("rsync --exclude=.svn -avz --delete $srcdir/ $ont");
        }
        else {
            debug("will not rsync to target as previous steps were not successful");
        }
    }

    # Method: obo2owl -- Build entire package from single obo file using OORT
    if ($method eq 'obo2owl') {
        my $SRC = "src/$ont.obo";
        my @OORT_ARGS = "--no-subsets --reasoner hermit";
        if ($info->{oort_args}) {
            @OORT_ARGS = $info->{oort_args};
        }
        my $env = '';
        if ($info->{oort_memory}) {
            $env = "OORT_MEMORY=$info->{oort_memory} ";
        }
        # TODO - no action if unchanged
        $success = wget($source_url, $SRC);
        if ($success) {
            
            if (is_different("$SRC.prev", $SRC) || !(-f "$ont/$ont.owl")) {

                # If the config states to auto-insert an ontology: header, then do it first
                if ($info->{insert_ontology_id}) {
                    # truly awful hack to insert the ontology id, required in obof1.4 and in particular by Oort;
                    # it would be better to add this as an option to Oort, but Oort development is frozen,
                    # and we will eventually replace the Oort command with ROBOT anyway
                    run("perl -pi -ne 's\@^format-version:\@ontology: $ont\\nformat-version:\@' $SRC");
                }

                # Oort places package files directly in target area, if successful
                my @skips = ('--skip-format owx', '--skip-format metadata');
                if ($ont eq 'pr' || $ont eq 'chebi' || $ont eq 'ncbitaxon') {
                    push(@skips, '--skip-format json');
                }
                $success = run($env."ontology-release-runner --skip-release-folder @skips --ignoreLock --allow-overwrite --outdir $ont @OORT_ARGS --asserted --simple $SRC");
                if ($success) {
                    run("mv $SRC $SRC.prev");
                }
                else {
                    debug("oort failed for: $SRC");
                }
            }
            else {
                debug("obo has not changed for $ont - will not rebuild");
            }

        }
        else {
            debug("will not run Oort as wget was unsuccessful");
        }
    }

    # Method: owl2obo -- Build entire package from single obo file using OORT
    if ($method eq 'owl2obo') {

        # TODO - reuse code with obo2owl. Keep separate for now, as owl2obo may require extra configuration
        my $SRC = "src/$ont.owl";
        my @OORT_ARGS = "--reasoner hermit";
        if ($info->{oort_args}) {
            @OORT_ARGS = $info->{oort_args};
        }
        $success = wget($source_url, $SRC);
        # TODO - less strict mode for owl2obo, many ontologies do not conform to obo constraints
        # TODO - allow options including translation of annotation axioms, merging of import closure, etc
        if ($success) {

            if (is_different("$SRC.prev", $SRC) || !(-f "$ont/$ont.obo")) {
                # Oort places package files directly in target area, if successful
                $success = run("ontology-release-runner --repair-cardinality --skip-release-folder --skip-format owx --ignoreLock --allow-overwrite --outdir $ont @OORT_ARGS --asserted --simple $SRC");
                if ($success) {
                    run("mv $SRC $SRC.prev");
                }
            }
            else {
                debug("owl has not changed for $ont - will not rebuild");
            }
        }
        else {
            debug("will not run Oort as wget was unsuccessful");
        }
    }

    # Method: archive -- Mirror package from archive
    if ($method eq 'archive') {
        my $SRC = "src/$ont-archive.zip";
        my $path = $info->{path};
        my $tmpdir = "tmp";
        if (!$path) {
            die "must set path for $ont";
        }
        $success = run("wget --no-check-certificate $source_url -O $SRC");
        if ($success) {
            if (-d $tmpdir) {
                $success = run("rm -rf $tmpdir");
            }
            $success = run("mkdir $tmpdir");
            if (!$success) {
                debug("Could not clear prepare archive dir: $tmpdir");
            }
            else {
                # chmod is necessary because of a weird jenkins bug
                $success = run("(cd $tmpdir && unzip -o ../$SRC && chmod -R 777 *)");
                if ($success) {
                    $success = run("rsync -avz --delete $tmpdir/$path/ $ont");
                    if ($success) {
                        debug("archive successful for $ont");
                    }
                    else {
                        debug("Failed to rsync to $ont");
                    }
                }
                else {
                    debug("unzip failed for $ont");
                }
            }
        }
        else {
            debug("wget failed on $source_url - no further action taken on $ont");
        }
    }

    if ($method eq 'custom') {
        die "not implemented";
    }


    # TEST
    if ((-f "$ont/$ont.obo") && (-f "$ont/$ont.owl")) {
        # ok
    }
    else {
        debug("Missing obo or owl files for $ont");
        $success = 0;
    }

    if ($is_compare_obo && $success) {
        # TODO - use boubastis
        my $this = "$ont/$ont.obo";
        my $last = "$target_dir/$ont/$ont.obo";
        if (is_different($this, $last,"$ont/central-obo-diff.txt")) {

            # central rss
            if (!(-d 'rss')) {
                run("mkdir rss");
            }
            # only compare if there are differences (i.e. cmp "fails")
            my $dargs = "--config 'html/ontology_name=$ont' --rss-path rss -f1 $last -f2 $this -m html text rss";
            if ($email) {
                $dargs .= " email --config email_to=$email";
                if ($info->{email_cc}) {
                    $dargs .= " --config email_cc=$info->{email_cc}";
                }
            }
            run("compare-obo-files.pl $dargs -o $ont/central-obo-diff");
            run("compare-defs.pl $dargs -o $ont/central-def-diff");
            my $date = `date +%Y-%m-%d`;
            chomp $date;
            if (!(-d "$ont/releases")) {
                run("mkdir $ont/releases");
            }
            # we don't create a full set of releases - only 
            if (!(-d "$ont/releases/$date")) {
                run("mkdir $ont/releases/$date");
            }
            run("cp $ont/*-diff* $ont/releases/$date");
        }
        else {
            debug("no change in $ont - not creating a diff");
        }
    }

    if ($success) {
        debug("Slated for deployment: $ont");
        push(@onts_to_deploy, $ont);
    }
    else {
        run("rsync -avz $ont failed-builds && rm -rf $ont");
        push(@failed_onts, $ont);
        if ($info->{infallible}) {
            push(@failed_infallible_onts, $ont);
        }
    }
}

# --REPORTING--
print "Build completed\n";
print "N_Errors: $n_errs\n";
foreach my $err (@errs) {
    print "ERROR: $err->{ont} $err->{cmd} $err->{err_text}\n";
}
printf "# Failed ontologies: %d\n", scalar(@failed_onts);
foreach my $font (@failed_onts) {
    print "FAIL: $font\n";
}
my $errcode = 0;

# --DEPLOYMENT--
# each successful ontology is copied to deployment area

$n_errs = 0; # reset
if ($dry_run) {
    debug("dry-run -- no deploy");
}
else {
    foreach my $ont (@onts_to_deploy) {
        debug("deploying $ont");
        # TODO - copy main .obo and .owl to top level
        run("rsync -avz --delete $ont/ $target_dir/$ont");

        deploy($ont, 'obo', $target_dir, 1);
        deploy($ont, 'owl', $target_dir, 1);
        deploy($ont, 'json', $target_dir, 0);
    }
    if (-d 'rss') {
        run("rsync -avz rss/ $target_dir/rss");
    }
}

if ($n_errs > 0) {
    $errcode = 1;
}

if (@failed_infallible_onts) {
    printf "# Failed ontologies: %d\n", scalar(@failed_onts);
    foreach my $font (@failed_infallible_onts) {
        print "FAIL: $font # THIS SHOULD NOT FAIL\n";
        $errcode = 1;
    }
}

if ($errcode) {
    # http://stackoverflow.com/questions/8148122/how-to-mark-a-build-unstable-in-jenkins-when-running-shell-scripts
    print "UNSTABLE: PROBLEMS WITH BUILD\n";
}
else {
    print "COMPLETED SUCCESSFULLY!\n";
}

# note that if we reach this point we want to exit with a successful error code;
# the build may still be marked unstable for jenkins
exit 0;
#exit $errcode;

# --SUBROUTINES--

sub deploy {
    my ($ont, $fmt, $target_dir, $is_forced) = @_;
    my $srcf = "$ont/$ont.$fmt";
    if (! -f $srcf && !$is_forced) {
        print STDERR "NOT FOUND: $srcf\n";
        return;
    }
    
    run("rsync $srcf $target_dir");
    run("gzip -c $srcf > $srcf.gz && rsync $srcf.gz $target_dir");

}

# Run command in the shell
# globals affected: $n_errs, @errs
# returns non-zero if success
sub run {
    my $cmd = shift @_;
    debug("  RUNNING: $cmd");
    my $err = system("$cmd 2> ERR");
    if ($err) {
        my $err_text = `cat ERR`;
        print STDERR "ERROR RUNNING: $cmd [in $ont ] code: $err\n";
        print STDERR $err_text;
        push(@errs, { ont => $ont,
                      cmd => $cmd,
                      err => $err,
                      err_text => $err_text });
        $n_errs ++;
    }    
    return !$err;
}

sub is_different {
    my $this = shift;
    my $last = shift || 'obo';
    my $out = shift || 'diff.tmp';

    my $diffcmd = "diff -b $this $last > $out";
    debug("CMD: $diffcmd");
    my $is_different = system($diffcmd);
    debug("comparing $this to $last == $is_different");
    return $is_different;
}

sub wget {
    my ($url, $tgt) = @_;
    return run("wget -T 300 --no-check-certificate '$url' -O $tgt");
}

sub debug {
    my $t = `date`;
    chomp $t;
    print STDERR "$t :: @_\n";
}

# Each ontology has build metadata in a lookup table. See documentation at bottom of file for overview
#
# Keys:
#  - method : see below. Currently: obo2owl, owl2obo, vcs or archive
#  - source_url : required for obo2owl or owl2obo or archive methods. For obo<->owl the entire package is build from this one file. for archive, this is the location of the archive file.
#  - checkout : required for vcs method. The command to checkout from scratch the repo. Note this is suffixed with a loca dir name - do not add this to the cfg.
#  - system : required for vcs method. Currently one of: git OR svn
#  - path: required for archive, optional for vcs. This is the path in the archive that corresponds to the top level of the package. 
#  - infallible : if a build of this ontology fails, the exit code of this script is an error (resulting in red ball if run in jenkins). This should only be set for ontologies with responsive maintainers.
#
# Notes:
#  For VCS, the checkout command should be to what would correspond to the top level of the package.
#  For all wget operations, --no-check-certificate is used
#
# Remember:
#  - For googlecode, anon checkouts should use http, not https
sub get_ont_info {
    my %ont_info = ();
    
    open(F, "curl $registry_url|");
    my $md = Load(join("", <F>));
    close(F);

    foreach my $ont (@{$md->{ontologies}}) {
        my $k = $ont->{id};
        $ont_info{$k} = $ont->{build};
    }
    return %ont_info;
}

sub usage() {

    <<EOM;
build-obo-ontologies.pl [-d|--dry-run] [-s ONT]* [-t|--target-dir TARGET]

ABOUT

This is the script that builds the contents of http://berkeleybop.org/ontologies/

Each ontology is placed in the top level (e.g. ma.obo, ma.owl), and
also in an ontology specific subdirectory (e.g. ma/ma.obo) along with
other derived artefacts.

The script can be executed locally.

DEPENDENCIES

 * Oort (command line)
 * svn client
 * git client
 * wget

PURPOSE

Builds or mirrors ontologies from the OBO library. After execution,
the directory from which this program was run will contain directories
such as:

  ma/
  fbbt/
  go/

These will also be copied to TARGET (default: "./deployed-ontologies")

Each of these should correspond to the structure of the corresponding obolibrary purl. For example,

  go/
    go.obo
    go.owl
    go-simple.obo
    subsets/
      goslim_plant.obo
      goslim_prok.obo
      ...

This can be used to build local copies of ontologies to be used with
an OWL catalog (TODO: document owltools directory mapper here).

This script is the one used by Jenkins to make a daily build here:

 http://berkeleybop.org/ontologies/

Which is currently the default fallback for unregistered purls (and
registered purls are welcome to redirect here, or to register to their
preferred hosting solution).

HOW IT WORKS

The script uses an internal registry to determine how to build each
ontology. There are currently 3 methods:

  * obo2owl
  * owl2obo
  * archive
  * vcs

The "obo2owl" method is intended for ontologies that publish a single
obo file, and do not take control of building owl or other derived
files in a obolibrary compliant way. It runs Oort to produce a
standard layout.

The owl2obo method also runs oort.

The vcs method is used when an ontology publishes release and derived
files in a consistent directory structure. It simply checks out the
project and rsyncs the specified subdirectory to the target. Currently
this is git or svn only.

The archive method is used when an ontology publishes the standard
files in a standard structure as an archive file (currently zip only,
but easily extended to tgz). This is currently used for ontologies
that are built via Jenkins, as jenkins publishes targets in a zip
archive.

HISTORY AND COORDINATION WITH OBO-REGISTRY

Historically, the Berkeley obo2owl pipeline consumed the
ontologies.txt file and generated owl for all obo ontologies, using
the "source" or "download" tag. This caused a number of problems - the
same field was used by some legacy applications that could not consume
more "advanced" obo meaning the build pipeline produced owl from
"dumbed down" versions of ontologies.

The ontologies.txt registry method is being overhauled, but there is
still a need for a build pipeline that handles some of the
peculiarities of each ontology, and can handle the many legacy obo
ontologies. In the future every ontology should use oort or a similar
tool to publish the full package, but an interim solution is
required. Even then, some ontologies require a place to distribute
their package (historically VCS has been used as the download
mechanism but this can be slow, and it can be inefficient to manage
multiple derived rdf/xml owl files in a VCS).

Once this new script is in place, the contents of
berkeleybop.org/ontologies/ will be populated using one of the above
methods for each ontology. Each ontology is free to either ignore this
and redirect their purls as they please, or alternatively, point their
purls at the central berkeley location.

The decision to keep the registry as a hash embedded in this script
allows for programmatic configurability, which is good for a lot of
important ontologies that do not yet publish their entire package in a
library-compliant way. In future this script should become less
necessary.

SEE ALSO

This may be a better long term approach for publishing ontologies:

 * http://gitorious.org/ontology-maven-plugins/ninox-maven-plugin

EOM
}
