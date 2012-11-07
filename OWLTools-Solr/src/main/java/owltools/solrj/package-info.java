/**
 * <h2>OWLTools and Solr</h2>
 * 
 * <p>
 * This package contains major loading methods for taking files parsed by OWLTools and loading them into a Solr index.
 * </p>
 * 
 * <p>
 * Some of the basic groundwork is covered by {@link owltools.solrj.AbstractSolrLoader}, but the two extreme specific cases are
 * probably better examples for the possible ways of loading an index. 
 * </p>
 * 
 * <p>
 * There are presently two file loading methods: one fairly generic one for loading ontology files 
 * ({@link owltools.solrj.FlexSolrDocumentLoader}) that requires the use of a YAML configuration file and 
 * one narrow one that's tied to the Gene Ontology's specific schema for GAF files ({@link owltools.solrj.GafSolrDocumentLoader}).
 * </p>
 */
package owltools.solrj;

