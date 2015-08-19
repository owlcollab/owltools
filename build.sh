#!/bin/sh
DIRNAME=`dirname $0`
PREV=`pwd`

cd $DIRNAME/OWLTools-Parent
mvn clean install -DskipTests -Dmaven.javadoc.skip=true -Dsource.skip=true

cd $PREV
