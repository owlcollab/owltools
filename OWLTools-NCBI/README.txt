NCBI Tool
James A. Overton <james@overton.ca>
2012-11-21

This tool converts data from the National Center for Biotechnology Information's (NCBI) Taxonomy Database into a Web Ontology Language (OWL) representation.

- NCBI Taxonomy Home: http://www.ncbi.nlm.nih.gov/taxonomy
- Latest data file: ftp://ftp.ebi.ac.uk/pub/databases/taxonomy/taxonomy.dat
- OWL: http://www.w3.org/TR/owl2-overview/
- OWLAPI: http://owlapi.sourceforge.net
- OBOFormat: http://oboformat.googlecode.com


# Usage

The main use of this tool is to take the latest line-based text representation of the NCBI Taxonomy database and convert it to an OWL/XML representation. The taxonomy.dat file is more than 200MB and the final OWL file is more than 800MB. The conversion process requires several GB of RAM and a few minutes.

Download the latest <code>taxonomy.dat</code> file and place it in your working directory. The following command will use 6GB of RAM to read that file and output an OWL file named <code>ncbitaxon.owl</code> (less RAM means slower operation):

    curl -O ftp://ftp.ebi.ac.uk/pub/databases/taxonomy/taxonomy.dat
    java -Xmx6G -jar bin/ncbi2owl.jar


## Advanced Usage

You can specify input and output files:

    java -Xmx6G -jar bin/ncbi2owl.jar src/test/resources/sample.dat sample.owl

You may also want to compare one OWL representation with another. This can be tricky to do. One approach is to serialize a list of all the axioms that OWLAPI represents, and to sort and compare the string representations of these axioms. This command generates a <code>sample.txt</code> file with an unsorted list of axioms:

    java -Xmx6G -jar bin/ncbi2owl.jar -a src/test/resources/sample.dat sample.txt

This command will convert a .dat file, save the OWL representation, and create a list of axioms:

    java -Xmx6G -jar bin/ncbi2owl.jar -ca src/test/resources/sample.dat sample.owl sample.txt

On a Unix system you can sort and compare lists of axioms using standard utilities:

    sort sample.txt > sample-sorted.txt
    sort sample2.txt > sample2-sorted.txt
    diff sample1-sorted.txt sample2-sorted.txt > diff.txt


## Debugging

This code uses Apache log4j for logging. You can override the default logger settings by creating a new <code>log4j.properties</code> that reconfigures the rootLogger:

    log4j.rootLogger=DEBUG, console
    log4j.appender.console=org.apache.log4j.ConsoleAppender
    log4j.appender.console.layout=org.apache.log4j.PatternLayout

Then tell Java to use the new log4j configuration:

    -Dlog4j.configuration=file:log4j.properties

For example:

    java -Xmx6G -Dlog4j.configuration=file:log4j.properties -jar bin/ncbi2owl.jar


# Build

This tool is built using Apache Maven: <http://maven.apache.org>. The build configuration depends on <code>OWLTools-Parent/pom.xml</code>, so it's best to place <code>OWLTools-NCBI</code> beside it in the <code>owltools</code> directory. To build an executable ncbi2owl.jar file, containing all dependencies, run:

    cd OWLTools-NCBI
    mvn clean package

You can also use other standard Maven commands:

    mvn compile
    mvn test


# Design

There are three classes:

- OWLConverter provides common utility functions for working with OWLAPI
- NCBIOWL creates and initializes the base OWL file
- NCBI2OWL converts .dat files to OWL format and provides the main entry point into the code

There is also a test class, NCBI2OWLTest, that provides unit tests and integration tests.


# Notes

The <code>taxonomy.dat</code> file has the following format:

    ID                        : 1760
    PARENT ID                 : 201174
    RANK                      : class
    GC ID                     : 11
    SCIENTIFIC NAME           : Actinobacteria
    GENBANK COMMON NAME       : high G+C Gram-positive bacteria
    SYNONYM                   : Actinomycetes
    SYNONYM                   : High GC gram-positive bacteria
    BLAST NAME                : high GC Gram+
    //

The <code>NCBI2OWL.convertToOWL<code> method reads the data file line by line, creating OWL classes as needed for each taxon, and then annotating the classes with the content for each line.

These fields are handled specially:

- ID: provides the IRI for the term, of the form http://purl.obolibrary.org/obo/NCBITaxon_1760
- PARENT ID: provides the IRI for the superClass
- RANK: this is linked to a term for the rank of the taxon
- GC ID: used to provide a cross-reference to Genbank
- SCIENTIFIC NAME: used to label the taxon

These fields are used to define synonyms using OBOFormat conventions:

- ACRONYM
- ANAMORPH
- BLAST NAME
- COMMON NAME
- EQUIVALENT NAME
- GENBANK ACRONYM
- GENBANK ANAMORPH
- GENBANK COMMON NAME
- GENBANK SYNONYM
- IN-PART
- MISNOMER
- MISSPELLING
- SYNONYM
- TELEOMORPH

Synonyms are annotated as an "exact", "related", or "broad" synonym. That annotation is then annotated with the specific synonym type. So "ACRONYM: XYZ" is annotated as "hasBroadSynonym XYZ" and that annotation is annotated with "hasSynonymType acronym". 

These fields are not handled:

- MGC ID
- INCLUDES





