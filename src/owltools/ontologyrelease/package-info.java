/**
   <h2>OBO Release Manager<h2>

   <h3>Building the release manager</h3>

   Eventually this will have its own installer.

   For now, make sure you have the most up to date version:

   <pre>
   cd OWLTools
   svn update
   </pre>

   The oboformat.jar should be up to date - but to be sure:

   <pre>
   cp ../oboformat/lib/oboformat.jar runtime/
   </pre>

   Developers do this to build (for the OWLTools dir):

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
