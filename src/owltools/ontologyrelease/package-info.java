/**
   <h2>Oort - OBO Ontology Release Tool</h2>

   <h3>Oort User Guide</h3>

   {@link http://code.google.com/p/owltools/wiki/OBOReleaseManagerGUIDocumentation}


   <h3>Building the release manager</h3>

   Most users should use the pre-built GUI, which has its own installer:
   http://code.google.com/p/owltools/wiki/OBOReleaseManagerGUIDocumentation

   Developers can use the "ontology-release-jar" ant task

   Then run like this:
   <pre>
   java -Xmx2524M -classpath $PATH_TO_OWLTOOLS/OWLTools/runtime/ontologyrelease.jar owltools.ontologyrelease.OboOntologyReleaseRunner [ARGS]
   </pre>
   
   There is also a wrapper script

   <h3>Original Specification</h3>

   https://docs.google.com/document/d/1VzlfrF29NpWonRLcUT3JZLEH5PoWa-wJxsrJstMKV8M/edit?hl=en_US&authkey=COeH6vMP


 */
package owltools.ontologyrelease;

import owltools.ontologyrelease.OboOntologyReleaseRunner;
