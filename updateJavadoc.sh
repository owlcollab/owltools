#!/bin/bash
# quick fail
set -e

# clean work directory
rm -rf work
mkdir -p work

# clone repo and switch to master
# Why? because there is no source code in gh-pages to avoid merge conflicts or building from an out-dated code base
cd work
git clone ../ owltools
cd owltools
git checkout master

# Build aggregated javadoc folder
cd OWLTools-Parent
createJavaDoc.sh

# go to top level gh-pages folder
# copy new Javadoc into top-level api folder
cd ../../..

cp -r work/owltools/OWLTools-Parent/target/apidocs api


