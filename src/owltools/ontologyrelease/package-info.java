/**
   <h2>OBO Release Manager<h2>

   <h3>Building the release manager</h3>

   Eventually this will have its own installer. Developers do this to build:

   <pre>
   ant ontology-release-jar
   </pre>

   Then run like this:
   <pre>
   java -Xmx2524M -classpath $PATH_TO_OWLTOOLS/OWLTools/runtime/ontologyrelease.jar owltools.ontologyrelease.OboOntologyReleaseRunner [ARGS]
   </pre>


 */
package owltools.ontologyrelease;

import owltools.ontologyrelease.OboOntologyReleaseRunner;
