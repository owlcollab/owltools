#!/bin/bash
set -e
# Any subsequent commands which fail will cause the shell script to exit immediately

## Change for different max memory settings 
MINERVA_MEMORY="4G"
## Default Minerva Port
MINERVA_PORT=6800

## Check that exactly one command-line argument is set
if [ $# -eq 0 ]
  then
    echo "Exactly one argument: GO_SVN root is required"
    exit 1
fi

## Use command-line input as the location of the GO_SVN
## Remove trailing slash
GO_SVN=${1%/}

## start Minerva
# use catalog xml and PURLs
java "-Xmx$MINERVA_MEMORY" -jar m3-server.jar \
-c "$GO_SVN"/ontology/extensions/catalog-v001.xml \
-g http://purl.obolibrary.org/obo/go/extensions/go-lego.owl \
--obsolete-import http://purl.obolibrary.org/obo/go.owl \
--obsolete-import http://purl.obolibrary.org/obo/go/extensions/x-disjoint.owl \
--obsolete-import http://purl.obolibrary.org/obo/ro.owl \
--obsolete-import http://purl.obolibrary.org/obo/go/extensions/ro_pending.owl \
--obsolete-import http://purl.obolibrary.org/obo/eco.owl \
--set-important-relation-parent http://purl.obolibrary.org/obo/LEGOREL_0000000 \
-f "$GO_SVN"/experimental/lego/server/owl-models \
--gaf-folder "$GO_SVN"/gene-associations \
-p "$GO_SVN"/experimental/lego/server/protein/subset \
--port $MINERVA_PORT