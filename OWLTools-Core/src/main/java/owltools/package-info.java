/**
 * <h1>OWLTools - an OWL API wrapper</h1>
 * <p>
 * OWLTools is convenience java API and toolkit on top of the OWL API. It provides:
 * <ul>
 * <li>convenience methods for OBO-like properties such as synonyms, textual definitions, obsoletion, replaced_by</li>
 * <li>simple graph-like operations over ontologies</li>
 * <li>visualization using the QuickGO graphs libraries</li>
 * <li>A powerful <a href="http://code.google.com/p/owltools/wiki/CommandLineExamples">command line interface</a> that can be used to chain multiple operations</li>
 * </ul>
 * <p>
 * <h3>Projects using OWLTools</h3> 
 * <ul>
 * <li>The Oort
 * <li><a href="http://termgenie.org">TermGenie</a>
 * <li><a href="http://owlsim.org">OWLSim</a>
 * </ul>
 * <h3>Getting started:</h3> 
 * <ul>
 * <li>The core model and API is in the package {@link owltools.graph}, the {@link owltools.graph.OWLGraphWrapper} wraps an OWL Ontology
 * <li>Graphical rendering is handled by the {@link owltools.gfx} package, see {@link owltools.gfx.OWLGraphLayoutRenderer} for QuickGO rendering
 * <li>Input/Output for various formats is in the {@link owltools.io} package (not much there yet)
 * <li>MIREOT-like support in the {@link owltools.mooncat} package (used in Oort)
 * <li>Phenolog analyses in {@link owltools.phenolog}
 * </ul>
 * 
 * @see <a href="http://owltools.googlecode.com">The OWLTools Repository</a> for more information
 */
package owltools;
