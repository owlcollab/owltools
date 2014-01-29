This is a quick overview on how to setup a Java server for the MolecularModelManager.

Pre-Requisites to build the code:
* Java (1.6 or later) as compiler
* Maven (3.0.x) Build-Tool
* SVN checkout of OWLTools trunk

Build the code:
* Go to the bin folder in the MolecularModelServer project
* ./build-server.sh
  -> This will trigger a recursive build of the required projects and generate an executable jar.


Pre-Requisites to run the server
* go.owl (GO-SVN/trunk/ontology/go.owl)
* folder with GAFs (GO-SVN/trunk/gene-associations/)
* folder with model files (GO-SVN/trunk/experimental/lego/server/owl-models/)
The required files are all available from the GO-SVN. No need to checkout the whole GO-SVN repo.
Local copies (and subsets) should be enough for testing.

Start the MolecularModelManager server
* Build the code, will result in a jar
* The start script is in the bin folder: start-m3-server.sh
  The script expects parameters for:
  -g path-to/go.owl
  -f path-to/owl-models
  --gaf-folder path-to/gaf-folder
 For more details an option please check the source code of owltools.gaf.lego.server.StartUpTool

#Alternative for developers:
* Requires all the data (go, GAFs, and models)
* Build in eclipse, start as main with appropriate parameters for the location of go.owl, 
  GAFs and model OWL-files.

