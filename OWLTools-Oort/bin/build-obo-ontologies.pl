#!/usr/bin/perl -w
use strict;

# For documentation, see usage() method, or run with "-h" option

my %selection = ();  # subset of ontologies to run on (defaults to all)
my $dry_run = 0;     # do not deploy if dry run is set
my $target_dir = './deployed-ontologies';  # in a production setting, this would be a path to web-visible area, e.g. berkeley CDN or NFS
my $is_compare_obo = 0;
my $email = '';

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
    elsif ($opt eq '-t' || $opt eq '--target-dir') {
        $target_dir = shift @ARGV;
    }
    elsif ($opt eq '-d' || $opt eq '--dry-run') {
        $dry_run = 1;
    }
    elsif ($opt eq '-c' || $opt eq '--compare-obo') {
        $is_compare_obo = 1;
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

# Build-in registry
my %ont_info = get_ont_info();

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
        my @OORT_ARGS = "--no-subsets --reasoner elk";
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
                # Oort places package files directly in target area, if successful
                $success = run($env."ontology-release-runner --skip-release-folder --skip-format owx --ignoreLock --allow-overwrite --outdir $ont @OORT_ARGS --asserted --simple $SRC");
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
        my @OORT_ARGS = "--reasoner elk";
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
        run("rsync $ont/$ont.obo $target_dir");
        run("rsync $ont/$ont.owl $target_dir");
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
    print "PROBLEMS WITH BUILD\n";
}
else {
    print "COMPLETED SUCCESSFULLY!\n";
}

exit $errcode;

# --SUBROUTINES--

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
    return
        (
         go => {
             infallible => 1,
             method => 'vcs',
             system => 'svn',
             checkout => 'svn --ignore-externals co svn://ext.geneontology.org/trunk/ontology',
         },
         #uberon => {
         #    infallible => 1,
         #    method => 'vcs',
         #    system => 'git',
         #    checkout => 'git clone https://github.com/cmungall/uberon.git',
         #},
         uberon => {
             infallible => 1,
             method => 'vcs',
             system => 'svn',
             checkout => 'svn --ignore-externals co http://svn.code.sf.net/p/obo/svn/uberon/trunk',
             email_cc => 'cjmungall@lbl.gov',
         },
         cl => {
             infallible => 1,
             method => 'vcs',
             system => 'svn',
             checkout => 'svn  --ignore-externals co http://cell-ontology.googlecode.com/svn/trunk/src/ontology',
             email_cc => 'cl_edit@googlegroups.com',
         },
         clo => {
             method => 'vcs',
             system => 'svn',
             checkout => 'svn --ignore-externals co http://clo-ontology.googlecode.com/svn/trunk/src/ontology',
             post_processing_command => 'owltools --use-catalog clo.owl --merge-imports-closure --ni -o -f obo --no-check clo.obo ',
         },
         fbbt => {
             infallible => 1,
             method => 'owl2obo',
             source_url => 'http://svn.code.sf.net/p/fbbtdv/code/fbbt/releases/fbbt.owl'
         },
         dpo => {
             infallible => 0,
             method => 'owl2obo',
             source_url => 'http://purl.obolibrary.org/obo/fbcv/dpo.owl',
         },
         #fbcv => {
         #    infallible => 1,
         #    method => 'vcs',
         #    system => 'svn',
         #    #checkout => 'svn checkout svn://svn.code.sf.net/p/fbcv/code-0/src/trunk/ontologies/',
         #    checkout => 'svn checkout svn://svn.code.sf.net/p/fbcv/code-0/releases/latest/',
         #},
         fbcv => {
             method => 'obo2owl',
             source_url => 'http://sourceforge.net/p/fbcv/code-0/HEAD/tree/src/trunk/ontologies/fbcv-edit.obo?format=raw',
         },
         #fbphenotype => {
         #    method => 'owl2obo',
         #    source_url => 'https://sourceforge.net/p/fbcv/code-0/HEAD/tree/releases/latest/FB_phenotype-non-classified.owl?format=raw',
         #},
         sibo => {
             method => 'vcs',
             system => 'git',
             checkout => 'git clone https://github.com/obophenotype/sibo.git',
         },
         ceph => {
             method => 'vcs',
             system => 'git',
             checkout => 'git clone https://github.com/obophenotype/cephalopod-ontology.git',
             path => 'src/ontology',
         },
         cteno => {
             method => 'vcs',
             system => 'git',
             checkout => 'git clone https://github.com/obophenotype/ctenophore-ontology.git',
             path => 'src/ontology',
         },
         ehdaa2 => {
             method => 'vcs',
             system => 'git',
             checkout => 'git clone https://github.com/obophenotype/human-developmental-anatomy-ontology.git',
             path => 'src/ontology',
         },
         aeo => {
             method => 'vcs',
             system => 'git',
             checkout => 'git clone https://github.com/obophenotype/human-developmental-anatomy-ontology.git',
             path => 'src/ontology',
         },
         vt => {
             method => 'vcs',
             system => 'svn',
             checkout => 'svn co http://phenotype-ontologies.googlecode.com/svn/trunk/src/ontology/vt',
         },
         poro => {
             method => 'vcs',
             infallible => 1,
             system => 'git',
             checkout => 'git clone https://github.com/obophenotype/porifera-ontology.git',
             path => '.',
         },
         #nbo => {
         #    notes => 'SWITCH',
         #    method => 'owl2obo',
         #    source_url => 'http://behavior-ontology.googlecode.com/svn/trunk/behavior.owl',
         #},
         nbo => {
             method => 'vcs',
             system => 'svn',
             checkout => 'svn co http://behavior-ontology.googlecode.com/svn/trunk',
             # TODO - rename
         },
         ro => {
             infallible => 1,
             method => 'vcs',
             system => 'svn',
             checkout => 'svn co http://obo-relations.googlecode.com/svn/trunk/src/ontology',
         },
         bspo => {
             infallible => 1,
             method => 'vcs',
             system => 'git',
             checkout => 'git clone https://github.com/obophenotype/biological-spatial-ontology.git',
             path => 'src/ontology',
         },

         hao => {
             method => 'vcs',
             system => 'svn',
             checkout => 'svn co http://obo.svn.sourceforge.net/svnroot/obo/ontologies/trunk/HAO',
         },

         omp => {
             method => 'obo2owl',
             source_url => 'https://sourceforge.net/p/microphenotypes/code/HEAD/tree/trunk/omp-edit.obo?format=raw',
         },

         fypo => {
             infallible => 1,
             method => 'obo2owl',
             source_url => 'https://sourceforge.net/p/pombase/code/HEAD/tree/phenotype_ontology/releases/latest/fypo.obo?format=raw',
         },
         #fypo => {
         #    method => 'vcs',
         #    system => 'svn',
         #    checkout => 'svn checkout svn://svn.code.sf.net/p/pombase/code/phenotype_ontology/releases/latest',
         #},
         chebi => {
             infallible => 1,
             method => 'archive',
             path => 'archive/main',
             source_url => 'http://build.berkeleybop.org/job/build-chebi/lastSuccessfulBuild/artifact/*zip*/archive.zip',
         },
         #'bio-attributes' => {
         #    method => 'archive',
         #    path => 'archive/main/go/extensions',   # <-- this will be changed later
         #    source_url => 'http://build.berkeleybop.org/job/build-bio-attributes/lastSuccessfulBuild/artifact/*zip*/archive.zip',
         #},
         oba => {
             infallible => 0,
             method => 'archive',
             path => 'archive/src/ontology',
             source_url => 'http://build.berkeleybop.org/job/build-oba/lastSuccessfulBuild/artifact/*zip*/archive.zip',
         },

         envo => {
             infallible => 1,
             method => 'vcs',
             system => 'git',
             checkout => 'git clone https://github.com/EnvironmentOntology/envo.git',
             path => 'src/envo',
             email_cc => 'cjmungall@lbl.gov',
         },
         gaz => {
             method => 'archive',
             path => 'archive',
             source_url => 'http://build.berkeleybop.org/job/build-gaz/lastSuccessfulBuild/artifact/*zip*/archive.zip',
#             source_url => 'http://obo.cvs.sourceforge.net/*checkout*/obo/obo/ontology/environmental/gaz.obo',
         },
         ma => {
             infallible => 1,
             method => 'obo2owl',
             source_url => 'ftp://ftp.informatics.jax.org/pub/reports/adult_mouse_anatomy.obo',
         },
         zfa => {
             infallible => 1, 
             notes => 'may be ready to switch to vcs soon',
             method => 'obo2owl',
             source_url => 'https://zebrafish-anatomical-ontology.googlecode.com/svn/trunk/src/preversion.zfish.obo',
             #source_url => 'https://zebrafish-anatomical-ontology.googlecode.com/svn/trunk/src/zebrafish_anatomy.obo',
         },
         #zfa_dev => {
         #    
         #    method => 'obo2owl',
         #    source_url => 'https://zebrafish-anatomical-ontology.googlecode.com/svn/trunk/src/preversion.zfish.obo',
         #},

         zfs => {
             infallible => 1,
             method => 'obo2owl',
             source_url => 'https://raw.githubusercontent.com/obophenotype/developmental-stage-ontologies/master/src/zfs/zfs.obo'
         },

         upheno => {
             method => 'archive',
             path => 'archive/ontology',
             source_url => 'http://build.berkeleybop.org/job/build-pheno-ontologies/lastSuccessfulBuild/artifact/*zip*/archive.zip',
         },

         hsapdv => {
             infallible => 1,
             method => 'obo2owl',
             source_url => 'https://raw.githubusercontent.com/obophenotype/developmental-stage-ontologies/master/src/hsapdv/hsapdv.obo',
         },
         mmusdv => {
             #infallible => 1,
             method => 'obo2owl',
             source_url => 'https://raw.githubusercontent.com/obophenotype/developmental-stage-ontologies/master/src/mmusdv/mmusdv.obo',
         },
         pdumdv => {
             method => 'obo2owl',
             source_url => 'https://raw.githubusercontent.com/obophenotype/developmental-stage-ontologies/master/src/pdumdv/pdumdv.obo',
         },
         olatdv => {
             method => 'obo2owl',
             source_url => 'https://raw.githubusercontent.com/obophenotype/developmental-stage-ontologies/master/src/olatdv/olatdv.obo',
         },
         rs => {
             method => 'obo2owl',
             source_url => 'ftp://rgd.mcw.edu/pub/ontology/rat_strain/rat_strain.obo',
         },
         cmo => {
             method => 'obo2owl',
             source_url => 'ftp://rgd.mcw.edu/pub/ontology/clinical_measurement/clinical_measurement.obo',
         },
         mmo => {
             method => 'obo2owl',
             source_url => 'ftp://rgd.mcw.edu/pub/ontology/measurement_method/measurement_method.obo',
         },
         xco => {
             method => 'obo2owl',
             source_url => 'ftp://rgd.mcw.edu/pub/ontology/experimental_condition/experimental_condition.obo',
         },

         po => {
             infallible => 1,
             notes => 'switch to vcs method',
             method => 'obo2owl',
             source_url => 'http://palea.cgrb.oregonstate.edu/viewsvn/Poc/tags/live/plant_ontology.obo?view=co',
         },
         caro => {
             notes => 'moving to owl soon',
             method => 'obo2owl',
             source_url => 'http://obo.cvs.sourceforge.net/*checkout*/obo/obo/ontology/anatomy/caro/caro.obo',
         },
         # OBSOLETE
         #ehda => {
         #    method => 'obo2owl',
         #    source_url => 'http://obo.cvs.sourceforge.net/*checkout*/obo/obo/ontology/anatomy/gross_anatomy/animal_gross_anatomy/human/human-dev-anat-staged.obo',
         #},
         emap => {
             notes => 'new url soon',
             method => 'obo2owl',
             source_url => 'ftp://ftp.hgu.mrc.ac.uk/pub/MouseAtlas/Anatomy/EMAP_combined.obo',
         },
         emapa => {
             notes => 'new url soon',
             method => 'obo2owl',
             source_url => 'ftp://ftp.hgu.mrc.ac.uk/pub/MouseAtlas/Anatomy/EMAPA.obo',
         },



         # GENERIC

         fma => {
             infallible => 1,
             method => 'obo2owl',
             source_url => 'http://svn.code.sf.net/p/obo/svn/fma-conversion/trunk/fma2_obo.obo',
         },
         imr => {
             method => 'obo2owl',
             source_url => 'http://www.inoh.org/ontologies/MoleculeRoleOntology.obo',
         },
         mfo => {
             method => 'obo2owl',
             source_url => 'http://obo.cvs.sourceforge.net/*checkout*/obo/obo/ontology/anatomy/gross_anatomy/animal_gross_anatomy/fish/medaka_ontology.obo',
         },
         hom => {
             method => 'vcs',
             system => 'git',
             checkout => 'git clone https://github.com/BgeeDB/homology-ontology.git',
         },
         pr => {
             infallible => 0,
             method => 'obo2owl',
             oort_args => '--no-reasoner',  ## PR now too big
             source_url => 'ftp://ftp.pir.georgetown.edu/databases/ontology/pro_obo/pro.obo',
         },
         gro => {
             method => 'obo2owl',
             source_url => 'http://palea.cgrb.oregonstate.edu/viewsvn/Poc/trunk/ontology/OBO_format/po_anatomy.obo?view=co',
         },
         mp => {
             infallible => 1,
             method => 'obo2owl',
             source_url => 'ftp://ftp.informatics.jax.org/pub/reports/MPheno_OBO.ontology',
         },
         symp => {
             method => 'obo2owl',
             source_url => 'http://gemina.svn.sourceforge.net/viewvc/gemina/trunk/Gemina/ontologies/gemina_symptom.obo',
         },
         pw => {
             method => 'obo2owl',
             source_url => 'ftp://rgd.mcw.edu/pub/data_release/ontology_obo_files/pathway/pathway.obo',
         },
         vo => {
             method => 'obo2owl',
             source_url => 'http://www.violinet.org/vo',
         },
         vario => {
             method => 'obo2owl',
             source_url => 'http://variationontology.org/vario_download/vario.obo',
         },
         eco => {
             notes => 'switch to vcs',
             infallible => 1,
             method => 'obo2owl',
             source_url => 'http://evidenceontology.googlecode.com/svn/trunk/eco.obo',
         },
         iev => {
             method => 'obo2owl',
             source_url => 'http://www.inoh.org/ontologies/EventOntology.obo',
         },
         ypo => {
             method => 'obo2owl',
             source_url => 'http://obo.cvs.sourceforge.net/*checkout*/obo/obo/ontology/phenotype/yeast_phenotype.obo',
         },
         doiddev => {
             infallible => 0,
             method => 'archive',
             path => 'archive/src/ontology',
             source_url => 'http://build.berkeleybop.org/job/build-doid/lastSuccessfulBuild/artifact/*zip*/archive.zip',
         },
         doid => {
             infallible => 1,
             method => 'obo2owl',
             source_url => 'http://sourceforge.net/p/diseaseontology/code/HEAD/tree/trunk/HumanDO.obo?format=raw',
         },
         ncbitaxon => {
             infallible => 0,
             method => 'archive',
             path => 'archive/src/ontology',
             source_url => 'http://build.berkeleybop.org/job/build-ncbitaxon/lastSuccessfulBuild/artifact/*zip*/archive.zip',
         },
         exo => {
             method => 'obo2owl',
             source_url => 'http://ctdbase.org/reports/CTD_exposure_ontology.obo',
         },
         lipro => {
             method => 'owl2obo',
             source_url => 'http://www.lipidprofiles.com/LipidOntology',
         },
         dron => {
             method => 'owl2obo',
             source_url => 'http://purl.obolibrary.org/obo/dron.owl',
         },
         flu => {
             method => 'owl2obo',
             source_url => ' http://purl.obolibrary.org/obo/flu.owl',
         },
         epo => {
             method => 'owl2obo',
             source_url => 'http://purl.obolibrary.org/obo/epo.owl',
         },
         to => {
             comment => 'switch to jenkins/archive',
             infallible => 1,
             method => 'obo2owl',
             source_url => 'http://palea.cgrb.oregonstate.edu/viewsvn/Poc/trunk/ontology/collaborators_ontology/gramene/traits/trait.obo?view=co',
         },
         nmr => {
             method => 'owl2obo',
             source_url => 'https://msi-workgroups.svn.sourceforge.net/svnroot/msi-workgroups/ontology/NMR.owl',
         },
         miro => {
             method => 'obo2owl',
             source_url => 'http://anobase.vectorbase.org/miro/miro_release.obo',
         },
         tads => {
             method => 'obo2owl',
             source_url => 'http://anobase.vectorbase.org/anatomy/tick_anatomy.obo',
         },
         swo => {
             method => 'owl2obo',
             source_url => 'http://theswo.svn.sourceforge.net/viewvc/theswo/trunk/src/release/swoinowl/swo_merged/swo_merged.owl',
         },
         rnao => {
             method => 'obo2owl',
             source_url => 'http://rnao.googlecode.com/svn/trunk/rnao.obo',
         },
         mod => {
             method => 'obo2owl',
             source_url => 'http://psidev.cvs.sourceforge.net/viewvc/psidev/psi/mod/data/PSI-MOD.obo',
         },
         mi => {
             method => 'obo2owl',
             source_url => 'http://psidev.cvs.sourceforge.net/viewvc/*checkout*/psidev/psi/mi/rel25/data/psi-mi25.obo',
         },
         aao => {
             method => 'obo2owl',
             source_url => 'http://github.com/seger/aao/raw/master/AAO_v2_edit.obo',
         },
         fbsp => {
             method => 'obo2owl',
             source_url => 'http://obo.cvs.sourceforge.net/*checkout*/obo/obo/ontology/taxonomy/fly_taxonomy.obo',
         },
         sep => {
             method => 'obo2owl',
             source_url => 'http://gelml.googlecode.com/svn/trunk/CV/sep.obo',
         },
         pato => {
             infallible => 1,
             method => 'obo2owl',
             source_url => 'https://raw.githubusercontent.com/obophenotype/pato/master/pato.obo',
         },
         pco => {
             method => 'owl2obo',
             oort_args => '--no-subsets --reasoner hermit',
             source_url => 'http://purl.obolibrary.org/obo/pco.owl',
         },
         trans => {
             method => 'obo2owl',
             source_url => 'http://gemina.cvs.sourceforge.net/*checkout*/gemina/Gemina/ontologies/transmission_process.obo',
         },
         xao => {
             infallible => 0,     ## can't build simple obo
             method => 'obo2owl',
             source_url => 'https://raw.githubusercontent.com/xenopus-anatomy/xao/master/xenopus_anatomy.obo'
         },
         mat => {
             method => 'obo2owl',
             source_url => 'http://obo.cvs.sourceforge.net/*checkout*/obo/obo/ontology/anatomy/gross_anatomy/animal_gross_anatomy/multispecies/minimal_anatomical_terminology.obo',
         },
         mpath => {
             method => 'obo2owl',
             source_url => 'http://mpath.googlecode.com/svn/trunk/mpath.obo',
         },
         mao => {
             method => 'obo2owl',
             source_url => 'http://bips.u-strasbg.fr/LBGI/MAO/mao.obo',
         },
         wbphenotype => {
             infallible => 0,
             method => 'obo2owl',
             source_url => 'http://tazendra.caltech.edu/~azurebrd/cgi-bin/forms/phenotype_ontology_obo.cgi',
         },
         mf => {
             method => 'owl2obo',
             source_url => 'http://purl.obolibrary.org/obo/mf.owl',
         },
         tgma => {
             method => 'obo2owl',
             source_url => 'http://anobase.vectorbase.org/anatomy/mosquito_anatomy.obo',
         },
         bila => {
             method => 'obo2owl',
             source_url => 'http://4dx.embl.de/4DXpress_4d/edocs/bilateria_mrca.obo',
         },
         tao => {
             method => 'obo2owl',
             source_url => 'http://purl.obolibrary.org/obo/tao.obo',
         },
         fao => {
             method => 'obo2owl',
             source_url => 'http://obo.cvs.sourceforge.net/*checkout*/obo/obo/ontology/anatomy/gross_anatomy/microbial_gross_anatomy/fungi/fungal_anatomy.obo',
         },
         wbbt => {
             infallible => 1,
             method => 'obo2owl',
             source_url => 'http://github.com/raymond91125/Wao/raw/master/WBbt.obo',
         },
         mfoem => {
             method => 'owl2obo',
             source_url => 'http://purl.obolibrary.org/obo/mfoem.owl',
         },
         nif_cell => {
             method => 'owl2obo',
             source_url => 'http://ontology.neuinfo.org/NIF/BiomaterialEntities/NIF-Cell.owl',
         },
         bootstrep => {
             method => 'obo2owl',
             source_url => 'http://www.ebi.ac.uk/Rebholz-srv/GRO/GRO_latest',
         },
         eo => {
             method => 'obo2owl',
             source_url => 'http://palea.cgrb.oregonstate.edu/viewsvn/Poc/trunk/ontology/collaborators_ontology/plant_environment/environment_ontology.obo',
         },
         bto => {
             method => 'obo2owl',
             source_url => 'http://www.brenda-enzymes.info/ontology/tissue/tree/update/update_files/BrendaTissueOBO',
         },
         wbls => {
             notes => 'switch to vcs in dev repo?',
             method => 'obo2owl',
             source_url => 'https://raw.github.com/draciti/Life-stage-obo/master/worm_development.obo',
         },
         sbo => {
             method => 'obo2owl',
             source_url => 'http://www.ebi.ac.uk/sbo/exports/Main/SBO_OBO.obo',
         },
         uo => {
             method => 'obo2owl',
             source_url => 'http://unit-ontology.googlecode.com/svn/trunk/unit.obo',
         },
         iao => {
             method => 'owl2obo',
             source_url => 'http://purl.obolibrary.org/obo/iao.owl',
         },
         nif_dysfunction => {
             method => 'owl2obo',
             source_url => 'http://ontology.neuinfo.org/NIF/Dysfunction/NIF-Dysfunction.owl',
         },
         apo => {
             method => 'obo2owl',
             source_url => 'http://obo.cvs.sourceforge.net/*checkout*/obo/obo/ontology/phenotype/ascomycete_phenotype.obo',
         },
         ato => {
             method => 'obo2owl',
             source_url => 'http://ontology1.srv.mst.edu/sarah/amphibian_taxonomy.obo',
         },
         #ehdaa => {
         #    method => 'obo2owl',
         #    source_url => 'http://obo.cvs.sourceforge.net/*checkout*/obo/obo/ontology/anatomy/gross_anatomy/animal_gross_anatomy/human/human-dev-anat-abstract.obo',
         #},
         fbdv => {
             infallible => 0,
             method => 'obo2owl',
             source_url => 'http://svn.code.sf.net/p/fbbtdv/code/fbdv/releases/latest/fbdv.obo'
         },
         cvdo => {
             method => 'owl2obo',
             source_url => 'http://purl.obolibrary.org/obo/cvdo.owl',
         },
         omrse => {
             method => 'owl2obo',
             source_url => 'http://omrse.googlecode.com/svn/trunk/omrse/omrse.owl',
         },
         ms => {
             method => 'obo2owl',
             source_url => 'http://psidev.cvs.sourceforge.net/*checkout*/psidev/psi/psi-ms/mzML/controlledVocabulary/psi-ms.obo',
         },
         spd => {
             method => 'obo2owl',
             source_url => 'http://obo.cvs.sourceforge.net/*checkout*/obo/obo/ontology/anatomy/gross_anatomy/animal_gross_anatomy/spider/spider_comparative_biology.obo',
         },
         pao => {
             method => 'obo2owl',
             source_url => 'po_anatomy.obo|http://palea.cgrb.oregonstate.edu/viewsvn/Poc/trunk/ontology/OBO_format/po_anatomy.obo?view=co',
         },
         nif_grossanatomy => {
             method => 'owl2obo',
             source_url => 'http://ontology.neuinfo.org/NIF/BiomaterialEntities/NIF-GrossAnatomy.owl',
         },
         #ev => {
         #    method => 'obo2owl',
         #    source_url => 'http://www.evocontology.org/uploads/Main/evoc_v2.7_obo.tar.gz',
         #},
         pgdso => {
             method => 'obo2owl',
             source_url => 'http://palea.cgrb.oregonstate.edu/viewsvn/Poc/trunk/ontology/OBO_format/po_temporal.obo?view=co',
         },
         cheminf => {
             method => 'owl2obo',
             source_url => 'http://semanticchemistry.googlecode.com/svn/trunk/ontology/cheminf.owl',
         },
         aero => {
             method => 'owl2obo',
             source_url => 'http://purl.obolibrary.org/obo/aero.owl',
         },
         obi => {
             method => 'owl2obo',
             source_url => 'http://purl.obofoundry.org/obo/obi.owl',
         },
         efo => {
             method => 'owl2obo',
             source_url => 'http://www.ebi.ac.uk/efo/efo.owl',
         },
         oae => {
             method => 'owl2obo',
             source_url => 'http://svn.code.sf.net/p/oae/code/trunk/src/ontology/oae.owl',
         },
         tto => {
             method => 'obo2owl',
             source_url => 'http://obo.cvs.sourceforge.net/*checkout*/obo/obo/ontology/taxonomy/teleost_taxonomy.obo',
         },
         fbbi => {
             method => 'obo2owl',
             source_url => 'https://raw.githubusercontent.com/dosumis/fbbi/master/src/ontology/fbbi.obo'
         },
         ddanat => {
             method => 'obo2owl',
             source_url => 'https://raw.githubusercontent.com/dictyBase/migration-data/master/ontologies/dicty_anatomy.obo'
         },
         ddpheno => {
             method => 'obo2owl',
             source_url => 'https://raw.githubusercontent.com/dictyBase/migration-data/master/ontologies/dicty_phenotypes.obo',
         },
         ero => {
             method => 'owl2obo',
             source_url => ' http://purl.obolibrary.org/obo/ero.owl',
         },
         rex => {
             method => 'obo2owl',
             source_url => 'http://obo.cvs.sourceforge.net/*checkout*/obo/obo/ontology/physicochemical/rex.obo',
         },
         idomal => {
             method => 'obo2owl',
             source_url => 'http://anobase.vectorbase.org/idomal/IDOMAL.obo',
         },
         taxrank => {
             method => 'obo2owl',
             source_url => 'https://raw.github.com/obophenotype/taxonomic-rank-ontology/master/src/ontology/taxrank.obo',
         },
         ido => {
             method => 'owl2obo',
             source_url => ' http://purl.obolibrary.org/obo/ido.owl',
         },
         cdao => {
             method => 'owl2obo',
             source_url => 'http://purl.obolibrary.org/obo/cdao.owl',
         },
         pd_st => {
             method => 'obo2owl',
             source_url => 'http://4dx.embl.de/platy/obo/Pdu_Stages.obo',
         },
         vsao => {
             method => 'obo2owl',
             source_url => 'http://phenoscape.svn.sourceforge.net/svnroot/phenoscape/tags/vocab-releases/VSAO/vsao.obo',
         },
         vhog => {
             method => 'obo2owl',
             source_url => 'http://bgee.unil.ch/download/vHOG.obo',
         },
         kisao => {
             method => 'obo2owl',
             source_url => 'http://biomodels.net/kisao/KISAO',
         },
         so => {
             notes => 'SWITCH',
             method => 'obo2owl',
             source_url => 'https://svn.code.sf.net/p/song/svn/trunk/so-xp.obo',
         },
         hp => {
             method => 'obo2owl',
             source_url => 'http://compbio.charite.de/svn/hpo/trunk/src/ontology/human-phenotype-ontology.obo',
         },


        );
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
