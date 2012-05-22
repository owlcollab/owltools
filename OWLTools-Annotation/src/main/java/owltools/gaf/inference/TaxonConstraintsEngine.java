package owltools.gaf.inference;

import java.util.Set;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;

import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;

/**
 * 
 * 
 * @author cjm
 *
 */
public class TaxonConstraintsEngine {

	// TODO - unduplicate GOLD code

	protected static Logger LOG = Logger.getLogger(TaxonConstraintsEngine.class);

	private OWLGraphWrapper graph;

	public TaxonConstraintsEngine(OWLGraphWrapper graph) {
		this.graph = graph;
	}

	/**
	 * @param cid
	 * @param taxId
	 * @return
	 */
	public boolean isClassApplicable(String cid, String taxId) {
		return isClassApplicable(graph.getOWLClassByIdentifier(cid),
				graph.getOWLClassByIdentifier(taxId));
	}

	/**
	 * returns true if testOntologyCls is applicable for testTax
	 * 
	 * @param testOntologyCls
	 * @param testTax
	 * @return
	 */
	public boolean isClassApplicable(OWLClass c, OWLClass testTax) {
		Set<OWLObject> taxAncs = graph.getAncestorsReflexive(testTax);
		Set<OWLGraphEdge> testClsEdges = graph.getOutgoingEdgesClosure(c);
		return isClassApplicable(c, testTax, testClsEdges, taxAncs);
	}
	
	// we have already calculated the closure of edges from c
	public boolean isClassApplicable(OWLClass c, OWLClass testTax, 
			Set<OWLGraphEdge> testClsEdges, Set<OWLObject> taxAncs) {
		boolean isInvalid = false;
		for (OWLGraphEdge e : testClsEdges) {
			if (isInvalid)
				break;
			OWLObject tgt = e.getTarget();
			Set<OWLGraphEdge> nextEdges = graph.getOutgoingEdges(tgt);			
			// never_in_taxon may also be encoded as an annotation assertion which is expanded to a disjointness axiom
			if (tgt instanceof OWLEntity) {
				for (OWLAnnotationAssertionAxiom aaa : graph.getSourceOntology().getAnnotationAssertionAxioms(((OWLEntity)tgt).getIRI())) {
					String rid = graph.getIdentifier(aaa.getProperty());
					if (rid.equals("RO:0002161")) {
						if (taxAncs.contains(graph.getOWLClass(aaa.getValue()))) {
							isInvalid = true;
							LOG.info("invalid: <"+c+" "+testTax+"> reason:"+aaa);
							break;
						}
					}
				}
			}
			if (tgt instanceof OWLClass) {
				for (OWLDisjointClassesAxiom dca : graph.getSourceOntology().getDisjointClassesAxioms((OWLClass) tgt)) {
					for (OWLClassExpression ce : dca.getClassExpressionsMinus((OWLClass)tgt)) {
						if (ce instanceof OWLObjectSomeValuesFrom) {
							String rid = graph.getIdentifier(((OWLObjectSomeValuesFrom)ce).getProperty());
							if (rid.equals("RO:0002162")) {
								OWLClassExpression tc = ((OWLObjectSomeValuesFrom)ce).getFiller();
								if (tc instanceof OWLClass &&
										taxAncs.contains(graph.getOWLClass((OWLClass)tc))) {
									isInvalid = true;
									LOG.info("invalid: <"+c+" "+testTax+"> reason:"+dca);
									break;
								}
							
							}
						}
					}
				}
			}
			
			for (OWLGraphEdge te : nextEdges) {
				OWLObjectProperty tp = te.getSingleQuantifiedProperty().getProperty();
				
				
				if (tp != null) {
					String tpl = graph.getLabel(tp);
					String tpid = graph.getIdentifier(tp);
					if (tpl == null)
						continue;
					// DANGER WILL ROBINSON!
					//  never_in_taxon may be encoded as a shortcut hasValue restriction,
					//  which means the target will be an individual. Convert to class;
					OWLClass restrTaxon;
					if (te.getTarget() instanceof OWLClass) {
						restrTaxon = (OWLClass) te.getTarget();
					}
					else {
						restrTaxon = graph.getOWLClass(te.getTarget());
					}
					// temp relation test hack until RO is stable
					if (tpl.equals("only_in_taxon") || tpl.equals("only in taxon") || tpid.equals("RO:0002160")) {
						// specified taxon can potentially be a leaf taxon OR intermediate.
						// if C only_in_taxon restrTaxon, then testTax must EITHER
						//  * be a subclass of restrTaxon (typical case)
						//  * OR a superclass of restrTaxon
						// Note the latter case is for when we want to extract a sub-ontology
						// for a high level clade such as vertebrate - we still want to include
						// mammal-specific structures.
						// However, if we are interested in the ur-Vertebrate, the logic is different
						// (TODO: option to control this)
						if (!(taxAncs.contains(restrTaxon) ||
								graph.getAncestors(restrTaxon).contains(testTax))) {
							isInvalid = true;
							if(LOG.isDebugEnabled()) {
								LOG.debug("invalid: <"+c+" "+testTax+"> restrTaxon="+restrTaxon+" // "+taxAncs.contains(restrTaxon));
							}
							break;
						}
					}
					else if (tpl.equals("never_in_taxon") || tpl.equals("never in taxon") || tpid.equals("RO:0002161")) {
						// if C never_in_taxon taxonRestr, then testTax cannot be a subclass of restrTaxon 
						if (taxAncs.contains(restrTaxon)) {
							isInvalid = true;
							if (LOG.isDebugEnabled()) {
								LOG.debug("invalid: <" + c + " " + testTax + "> reason:" + te);
							}
							break;
						}
					}
				}
			}
		}
		return !isInvalid;
	}

}
