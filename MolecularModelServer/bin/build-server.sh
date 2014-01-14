#!/bin/bash
cd ../../OWLTools-Parent
mvn clean install -am -pl ../MolecularModelServer -Dmaven.test.skip.exec=true

