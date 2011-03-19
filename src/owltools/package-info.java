/**
 * OWLTools API
 * <p>
 * OWLTools is convenience java API on top of the OWL API. It provides:
 * <ul>
 * <li>convenience methods for OBO-like properties such as synonyms, textual definitions, obsoletion, replaced_by</li>
 * <li>simple graph-like operations over ontologies</li>
 * <li>visualization using the QuickGO graphs libraries</li>
 * </ul>
 * <p>
 * <b>Getting started:</b> 
 * <ul>
 * <li>The core model and API is in the package {@link owltools.graph}, the {@link owltools.graph.OWLGraphWrapper} wraps an OWL Ontology
 * <li>Graphical rendering is handled by the {@link owltools.gfx} package, see {@link owltools.gfx.OWLGraphLayoutRenderer} for QuickGO rendering
 * <li>Input/Output for various formats is in the {@link owltools.io} package (not much there yet)
 * <li>Experimental MIREOT-like support in the {@link owltools.mooncat} package
 * <li>Semantic similarity in {@link owltools.sim}
 * <li>Phenolog analyses in {@link owltools.phenolog}
 * </ul>
 * 
 * @see <a href="http://wiki.geneontology.org/index.php/OWLTools">OWLTools</a> for more context
 */
package owltools;

import owltools.graph.OWLGraphWrapper;
import owltools.gfx.OWLGraphLayoutRenderer;
