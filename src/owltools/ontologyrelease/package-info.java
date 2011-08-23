/**
   <h2>OBO Release Manager</h2>

   This is an ant based command line tool which produces ontologies
   versions releases.  The command 'bin/ontology-release-runner'
   builds an ontology release. This tool is supposed to be run from
   the location where a particular ontology release are to be
   maintained.  In the process of producing a particular release this
   tool labels the release with a auto generated version id.  The
   version id is maintained in the VERSION-INFO file. All files
   produced for a particular release are assembled in the directory of
   name by the date of the release. Run the
   'bin/ontology-release-runner --h' tool with the --h option to get
   help which parameter to pass the tool.  To build the latest jar for
   the ontology release manager run the 'ant ontology-release-jar' ant
   command.  '

   <h3>GUI</h3>

   http://code.google.com/p/owltools/wiki/OBOReleaseManagerGUIDocumentation

   <h3>Specification</h3>

   https://docs.google.com/document/d/1VzlfrF29NpWonRLcUT3JZLEH5PoWa-wJxsrJstMKV8M/edit?hl=en_US&authkey=COeH6vMP

   <h3>Building the release manager</h3>

   Most users should use the pre-built GUI, which has its own installer:
   http://code.google.com/p/owltools/wiki/OBOReleaseManagerGUIDocumentation

   Developers can use the "ontology-release-jar" ant task

   Then run like this:
   <pre>
   java -Xmx2524M -classpath $PATH_TO_OWLTOOLS/OWLTools/runtime/ontologyrelease.jar owltools.ontologyrelease.OboOntologyReleaseRunner [ARGS]
   </pre>
   
   There is also a wrapper script


 */
package owltools.ontologyrelease;

import owltools.ontologyrelease.OboOntologyReleaseRunner;
