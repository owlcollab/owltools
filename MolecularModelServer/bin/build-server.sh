#!/bin/bash
cd ../../OWLTools-Parent
mvn clean package -am -pl ../MolecularModelServer -DskipTests -Dmaven.javadoc.skip=true -Dsource.skip=true

