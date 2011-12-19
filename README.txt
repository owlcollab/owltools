For full documentation, see the Wiki:

 * http://code.google.com/p/owltools

 == OWLTools Build Instructions ==

The OWLTools use maven as a build tool. 

These instructions assume that a valid maven installation is available.
The recommended maven version is 3.0.x, whereby x denotes the latest 
release for this branch. 


 === Building OWLTools === 

 * Option 1: Command line

   1)  Change into to the folder of OWLTools-Parent
  
   2a) Run command: mvn clean package
       This will trigger a complete build of all OWLTools projects
       and generate the required jars for execution.
      
       Remark: As part of the build the tests are executed. Any 
               failed test will stop the build.
      
   2b) Build without test execution (Not Recommended)
       Run command: mvn clean package -Dmaven.test.skip.exec=true
   
 * Option 2: Eclipse
   
   Requires either: 
     * Eclipse 3.7  OR 
     * Eclipse 3.6 with installed maven plugin m2e
   
   Use the provided Eclipse launch configurations to trigger the build.
   The configuration are located in the OWLTools-Parent/eclipse-configs 
   folder.

   
 === Running OWLTools (Unix and MacOS) ===

 Running OWLTools requires a successful build, as described in the 
 previous section.
 
 * OWLTools Command-Line Tools:
   The executables and the generated jar are both located in:
   OWLTools-Parent/bin

 * OORT: 
   The executables and the generated jar are both located in:
   OWLTools-Oort/bin