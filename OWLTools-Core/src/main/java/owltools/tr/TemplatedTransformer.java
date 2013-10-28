package owltools.tr;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectInverseOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.RemoveAxiom;

/**
 * Examples:
 * 
 * <pre>
 * quality and inheres_in some entity EquivalentTo has_part some (entity and has_quality quality)  
 * </pre>
 * Annotated with:
 * treat-as-variable quality.IRI
 * treat-as-variable entity.IRI
 * 
 * will find all uses of the former pattern and replace them with the latter
 * 
 * Also (advanced):
 * <pre>
 * 'subdivision of skeleton' and subdivision_of some autopod
 * SubClassOf
 * 'subdivision of skeleton' and subdivision_of some limb
 * ,
 * autopod SubClassOf limb
 * </pre>
 * 
 * 
 *
 */
public class TemplatedTransformer {
	
	OWLOntology ontology;
	private static Logger LOG = Logger.getLogger(TemplatedTransformer.class);

	
	public class Mapping {
		OWLClassExpression src;
		OWLClassExpression tgt;
		Set<IRI> vars = new HashSet<IRI>();
		public boolean isReplace = true;
		
		public String toString() {
			return src + " -> " + tgt + " :: " + vars;
		}
	}
	
	public TemplatedTransformer(OWLOntology o) {
		ontology = o;
	}


	public Set<Mapping> getMappings() {
		Set<Mapping> ms = new HashSet<Mapping>();
		OWLAnnotationProperty vap = getVariableAnnotationProperty();
		for (OWLSubClassOfAxiom sca : ontology.getAxioms(AxiomType.SUBCLASS_OF, true)) {
			Mapping m = new Mapping();
			Set<OWLAnnotation> anns = sca.getAnnotations(vap);
			for (OWLAnnotation ann : anns) {
				IRI v = (IRI) ann.getValue();
				m.vars.add(v);
			}
			if (m.vars.size() > 0) {
				m.src = sca.getSubClass();
				m.tgt = sca.getSuperClass();
				ms.add(m);
				LOG.info("MAPPING: "+m);
			}
		}
		return ms;
		
	}
	

	public Set<OWLOntologyChange> tr() {
		Set<OWLOntologyChange> chgs = new HashSet<OWLOntologyChange>();
		for (Mapping m : getMappings()) {
			chgs.addAll(tr(m));
		}
		return chgs;
	}
	public Set<OWLOntologyChange> tr(Mapping m) {
		Set<OWLOntologyChange> chgs = new HashSet<OWLOntologyChange>();
		for (OWLOntology o : ontology.getImportsClosure()) {
			chgs.addAll(tr(o, m));
		}
		return chgs;
	}
	
	public Set<OWLOntologyChange> tr(OWLOntology o, Mapping m) {
		Set<OWLOntologyChange> chgs = new HashSet<OWLOntologyChange>();
		for (OWLAxiom ax : o.getAxioms()) {
			chgs.addAll(tr(ax, m));
		}
		return chgs;
	}

	
	public Set<OWLOntologyChange> tr(OWLAxiom inAxiom, Mapping m) {
		Set<OWLOntologyChange> chgs = new HashSet<OWLOntologyChange>();
		boolean isModified = false;
		OWLAxiom newAxiom = null;
		if (inAxiom instanceof OWLEquivalentClassesAxiom) {
			OWLEquivalentClassesAxiom aa = (OWLEquivalentClassesAxiom)inAxiom;
			Set<OWLClassExpression> xs = new HashSet<OWLClassExpression>();
			for (OWLClassExpression x : aa.getClassExpressions()) {
				OWLClassExpression x2 = replace(x, m);
				if (x2 == null) {
					xs.add(x);
				}
				else {
					isModified = true;
					xs.add(x2);
					LOG.info("  TR : "+x+ " ---> "+x2);
				}
			}
			if (isModified) {
				newAxiom = getOWLDataFactory().getOWLEquivalentClassesAxiom(xs);
			}
		}
		if (isModified) {
			if (m.isReplace) {
				chgs.add(new RemoveAxiom(ontology, inAxiom));
			}
			chgs.add(new AddAxiom(ontology, newAxiom));
		}
		return chgs;
		
	}

	/**
	 * Replace inx using m
	 * 
	 * @param inx
	 * @param m
	 * @return
	 */
	private OWLClassExpression replace(OWLClassExpression inx,
			Mapping m) {

		LOG.info("Testing: "+inx+" for mapping using "+m);
		// test to see if there is a match between the src pattern in
		// the mapping and the input expression
		BindingSet bset = unifyAll(inx, m.src, m.vars);
		if (bset == null) {
			// no match
			LOG.info("No match for: "+inx);
			return null;
		}
		LOG.info("Unified. Bindings: "+bset);
		return replaceVariables(inx, bset);
	}


	// UNIFICATION

	class BindingSet {
		Map<IRI,IRI> varMap = new HashMap<IRI,IRI>();
		public String toString() {
			return varMap.toString();
		}
		
	}
	private BindingSet mergeBindings(BindingSet... bs) {
		BindingSet nu = new BindingSet();
		for (BindingSet b : bs) {
			if (b == null)
				return null;
			nu.varMap.putAll(b.varMap);
		}
		return nu;
	}
	public BindingSet unifyAll(OWLClassExpression inExpr, OWLClassExpression templateExpr, Set<IRI> vars) {
		BindingSet bset = unify(inExpr, templateExpr, vars);
		if (bset != null && bset.varMap.keySet().size() != vars.size()) {
			// some variables were unbound
			return null;
		}
		return bset;
	}
	
	public BindingSet unify(OWLClassExpression inExpr, OWLClassExpression templateExpr, Set<IRI> vars) {
		if (!(inExpr.getClassExpressionType().equals(templateExpr.getClassExpressionType()))) {
			return null;
		}
		if (!inExpr.isAnonymous()) {
			OWLNamedObject qq = (OWLNamedObject) inExpr;
			OWLNamedObject tt = (OWLNamedObject) templateExpr;
			return unify(qq.getIRI(), tt.getIRI(), vars);
		}
		if (inExpr instanceof OWLObjectSomeValuesFrom) {
			OWLObjectSomeValuesFrom qq = (OWLObjectSomeValuesFrom)inExpr;
			OWLObjectSomeValuesFrom tt = (OWLObjectSomeValuesFrom)templateExpr;
			BindingSet b1 = unify(qq.getProperty(), tt.getProperty(), vars);
			BindingSet b2 = unify(qq.getFiller(), tt.getFiller(), vars);
			return mergeBindings(b1, b2);
		}
		else if (inExpr instanceof OWLObjectIntersectionOf) {
			OWLObjectIntersectionOf qq = (OWLObjectIntersectionOf)inExpr;
			OWLObjectIntersectionOf tt = (OWLObjectIntersectionOf)templateExpr;
			BindingSet all = new BindingSet();
			for (OWLClassExpression qe : qq.getOperands()) {
				BindingSet m = null;
				for (OWLClassExpression te : qq.getOperands()) {
					m = unify(qe, te, vars);
					if (m != null)
						break;
				}
				if (m == null) {
					return null;
				}
				all = mergeBindings(all, m);
			}
			return all;
		}
		return null;
	}

	private BindingSet unify(IRI qin, IRI var,  Set<IRI> vars) {
		BindingSet bset = new BindingSet();
		if (vars.contains(var)) {
			LOG.info(" UNIFYING: ?"+var+" = "+qin);
			bset.varMap.put(var, qin);
			return bset;
		}
		if (qin.equals(var)) {
			// no variable unification but still a match
			return bset;
		}
		return null;
	}

	private BindingSet unify(OWLObjectPropertyExpression p,
			OWLObjectPropertyExpression template,  Set<IRI> vars) {
		if (p instanceof OWLObjectProperty && template instanceof OWLObjectProperty) {
			return unify(((OWLObjectProperty) p).getIRI(),
					((OWLObjectProperty) template).getIRI(),
					vars);
		}
		if (p instanceof OWLObjectInverseOf && template instanceof OWLObjectInverseOf) {
			return unify(((OWLObjectInverseOf)p).getInverse(),
					((OWLObjectInverseOf)template).getInverseProperty(),
					vars);
		}
		return null;
	}
	
	
	// REPLACEMENT
	
	private OWLClassExpression replaceVariables(OWLClassExpression inx,
			BindingSet bset) {
		if (inx instanceof OWLNamedObject) {
			IRI y = replaceIRI(((OWLNamedObject)inx).getIRI(), bset);
			if (inx instanceof OWLClass) {
				return getOWLDataFactory().getOWLClass(y);
			}
		}
		else if (inx instanceof OWLObjectSomeValuesFrom) {
			OWLObjectSomeValuesFrom svf = (OWLObjectSomeValuesFrom)inx;
			return getOWLDataFactory().getOWLObjectSomeValuesFrom(
					replaceVariables(svf.getProperty(),bset),
					replaceVariables(svf.getFiller(),bset));

		}
		else if (inx instanceof OWLObjectIntersectionOf) {
			Set<OWLClassExpression> es = new HashSet<OWLClassExpression>();
			for (OWLClassExpression e : ((OWLObjectIntersectionOf)inx).getOperands()) {
				es.add(replaceVariables(e, bset));
			}
			return getOWLDataFactory().getOWLObjectIntersectionOf(es);
		}
		else {
			
		}
		return null;
	}

	private OWLObjectPropertyExpression replaceVariables(
			OWLObjectPropertyExpression p, BindingSet bset) {
		if (p instanceof OWLObjectProperty) {
			IRI y = replaceIRI(((OWLObjectProperty)p).getIRI(), bset);
			return getOWLDataFactory().getOWLObjectProperty(y);
		}
		if (p instanceof OWLObjectInverseOf) {
			return getOWLDataFactory().getOWLObjectInverseOf(
					 replaceVariables(((OWLObjectInverseOf)p).getInverse(),
							 bset));
		}
		return null;
	}

	private IRI replaceIRI(IRI x, BindingSet bset) {
		if (bset.varMap.containsKey(x)) {
			LOG.info("  REPL: "+x + "==> "+ bset.varMap.get(x));
			return bset.varMap.get(x);
		}
		return x;
	}
	
	private OWLDataFactory getOWLDataFactory() {
		return ontology.getOWLOntologyManager().getOWLDataFactory();
	}

	private OWLAnnotationProperty getVariableAnnotationProperty() {
		return getOWLDataFactory().getOWLAnnotationProperty(
				IRI.create("http://www.geneontology.org/formats/oboInOwl#hasVariable"));
	}

}
