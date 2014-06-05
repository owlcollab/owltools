This is a quick overview on how to setup a Java server for the MolecularModelManager (Minerva).

Pre-Requisites to build the code:
* Java (JDK 1.7 or later) as compiler
* Maven (3.0.x) Build-Tool
* SVN checkout of OWLTools trunk, command:
  svn checkout http://owltools.googlecode.com/svn/trunk/ owltools 

Build the code:
* Go to the bin folder in the MolecularModelServer project
* ./build-server.sh
  -> This will trigger a recursive build of the required projects and generate an executable jar.


Pre-Requisites to run the server
* go.owl (GO-SVN/trunk/ontology/go.owl) 
  In the future this will be replaced by go-plus.owl (GO-SVN/ontology/extensions/go-plus.owl)
* folder with GAFs (GO-SVN/trunk/gene-associations/)
* folder with model files (GO-SVN/trunk/experimental/lego/server/owl-models/)
* folder with taxon specific protein models (GO-SVN/trunk/experimental/lego/server/protein/subset/)
* Other ontologies required for the modelling. 

The required files are all available from the GO-SVN. No need to checkout the whole GO-SVN repo.
Local copies (and subsets) should be enough for testing (for now).
Furthermore, catalog xml files (if available) should be used to load local copies, instead of
retrieving ontologies from the web.

Start the MolecularModelManager server
* Build the code, will result in a jar
* Check memory settings in start-m3-server.sh, changes as needed.
* The start script is in the bin folder: start-m3-server.sh
  The Minerva server expects parameters for:
  -g path-to/go.owl
  -f path-to/owl-models
  --gaf-folder path-to/gaf-folder
  [--port 6800]
  --p path-to/protein/subset
  [-c path-to/catalog.xml]
  -i path-to/extensions/x-disjoint.owl
For more details and options, please check the source code of owltools.gaf.lego.server.StartUpTool

Full example using a catalog.xml, IRIs and assumes a full GO-SVN trunk checkout:
start-m3-server.sh -c go-trunk/ontology/extensions/catalog-v001.xml \
-g http://purl.obolibrary.org/obo/go.owl \
-i http://purl.obolibrary.org/obo/go/extensions/x-disjoint.owl \
-f go-trunk/experimental/lego/server/owl-models \
--gaf-folder go-trunk/gene-associations \
-p go-trunk/experimental/lego/server/protein/subset \
--port 6800

#Alternative for developers:
* Requires all the data (go, GAFs, and models)
* Build in eclipse, start as main with appropriate parameters.

