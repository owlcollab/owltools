[![Build Status](https://travis-ci.org/owlcollab/owltools.svg?branch=master)](https://travis-ci.org/owlcollab/owltools)
[![DOI](https://zenodo.org/badge/13996/owlcollab/owltools.svg)](https://zenodo.org/badge/latestdoi/13996/owlcollab/owltools)

For full documentation,

 * [see the Wiki](https://github.com/owlcollab/owltools/wiki).
 * [see the java API docs](https://owlcollab.github.io/owltools)

## OWLTools Build Instructions

The OWLTools use maven as a build tool.

These instructions assume that a valid maven installation is available. The recommended maven version is 3.0.x, whereby x denotes the latest release for this branch.

Update: OWLTools also requires `git`. Only a proper clone via git, will allow the build to complete.

During the build process, we extract the git version and branch information. These details (and the build date) will be added to the manifest of the jar. If the `.git` folder is not available the build process will fail.

### Building OWLTools

#### Prerequiste: Get source from Git

`git clone https://github.com/owlcollab/owltools.git`

#### Option 1: Command line

1) Change into to the folder of `OWLTools-Parent`

2a) Run command: `mvn clean install`: This will trigger a complete build of all OWLTools projects and generate the required jars for execution. Remark: As part of the build the tests are executed. Any failed test will stop the build.

2b) Build without test execution (Not Recommended): Run command: mvn clean install -DskipTests

#### Option 2: Eclipse

Requires either:
* Eclipse 3.7
* Eclipse 3.6 with installed maven plugin m2e

Use the provided Eclipse launch configurations to trigger the build. The configuration are located in the `OWLTools-Parent/eclipse-configs` folder.

## Downloading OWLTools

This option uses a pre-built JAR and script-wrapper instead of building owltools from the Java source. The JAR is built as part of the continuous building and test of the OWLTools project on a Jenkins server.

Go to http://build.berkeleybop.org/userContent/owltools/

Download [owltools](http://build.berkeleybop.org/userContent/owltools/owltools)

Note: you may need to make this file executable. Occasionally when downloaded files lose the "executable" permisison. You can do this with `chmod +x owltools`.

If you add this to your [PATH](https://en.wikipedia.org/wiki/PATH_(variable)), you can run owltools on the command line:

```
export PATH=$PATH:directory/to/where/you/downloaded/owltools/
owltools -h
```

## Running OWLTools (Unix and MacOS)

Running OWLTools requires a successful build, as described in the previous section.

+ OWLTools Command-Line Tools: The build produces a combined executable bash script and jar, to be found in `OWLTools-Runner/bin`
or in the `OWLTools-Runner/target` directory

+ OORT: The executables and the generated jar are both located in `OWLTools-Oort/bin`
