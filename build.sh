#!/bin/sh
DIRNAME=`dirname $0`
PREV=`pwd`

cd $DIRNAME/OWLTools-Parent
mvn clean package -DskipTests -Dmaven.javadoc.skip=true -Dsource.skip=true

cd $PREV
