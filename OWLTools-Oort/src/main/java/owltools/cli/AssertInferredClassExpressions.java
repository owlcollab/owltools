package owltools.cli;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.geneontology.reasoner.ExpressionMaterializingReasoner;
import org.geneontology.reasoner.ExpressionMaterializingReasonerFactory;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.util.OWLClassExpressionVisitorAdapter;
import org.semanticweb.owlapi.util.OWLClassExpressionVisitorExAdapter;

import owltools.graph.OWLGraphWrapper;
import owltools.io.CatalogXmlIRIMapper;
import owltools.io.OWLPrettyPrinter;
import owltools.io.ParserWrapper;

import com.google.common.base.Predicate;
import com.google.common.collect.Sets;

public class AssertInferredClassExpressions {

	public static interface OWLClassFilter {
		
		public boolean use(OWLClass cls);
	}
	
	public static Map<OWLClass, Set<OWLObjectSomeValuesFrom>> getExpressions(OWLOntology ontology, Set<OWLObjectProperty> properties, OWLClassFilter filter) {
		final ExpressionMaterializingReasonerFactory rf = new ExpressionMaterializingReasonerFactory(new ElkReasonerFactory());
		final ExpressionMaterializingReasoner reasoner = rf.createReasoner(ontology);
		try {
			reasoner.materializeExpressions(properties);
			final Map<OWLClass, Set<OWLObjectSomeValuesFrom>> newAxioms = new HashMap<OWLClass, Set<OWLObjectSomeValuesFrom>>();
			for (OWLClass cls : ontology.getClassesInSignature()) {
				if (filter != null && filter.use(cls) == false) {
					continue;
				}
				// find existing some value froms
				Set<OWLObjectSomeValuesFrom> existingSVFs = getSuperSVFs(ontology.getSubClassAxiomsForSubClass(cls));
				// get all direct super classes
				Set<OWLClassExpression> directSuperCE = reasoner.getSuperClassExpressions(cls, true);
				// filter for SVFs
				Set<OWLObjectSomeValuesFrom> directSuperSVFs = getSVFs(directSuperCE);
				
				// missing direct SVFs, calculate using a set difference
				Set<OWLObjectSomeValuesFrom> missingSVFs = Sets.difference(directSuperSVFs, existingSVFs);
				
				// add to result set
				if (missingSVFs.isEmpty() == false) {
					missingSVFs = new HashSet<OWLObjectSomeValuesFrom>(missingSVFs);
					if (filter != null) {
						missingSVFs = filterSVFs(filter, missingSVFs);
					}
					if (missingSVFs.isEmpty() == false) {
						newAxioms.put(cls, missingSVFs);
					}
				}
			}
			return newAxioms;
		}
		finally {
			reasoner.dispose();
		}
	}

	private static Set<OWLObjectSomeValuesFrom> filterSVFs(final OWLClassFilter clsFilter, Set<OWLObjectSomeValuesFrom> svfs) {
		Predicate<OWLObjectSomeValuesFrom> predicate = new Predicate<OWLObjectSomeValuesFrom>() {

			@Override
			public boolean apply(OWLObjectSomeValuesFrom input) {
				OWLClassExpression filler = input.getFiller();
				Boolean result = filler.accept(new OWLClassExpressionVisitorExAdapter<Boolean>(){
					@Override
					public Boolean visit(OWLClass cls) {
						return clsFilter.use(cls);
					}
				});
				if (result != null) {
					return result.booleanValue();
				}
				return false;
			}
		};
		svfs = Sets.filter(svfs, predicate);
		return svfs;
	}

	private static Set<OWLObjectSomeValuesFrom> getSuperSVFs(Set<OWLSubClassOfAxiom> axioms) {
		final Set<OWLObjectSomeValuesFrom> svfs = new HashSet<OWLObjectSomeValuesFrom>();
		for (OWLSubClassOfAxiom existing : axioms) {
			OWLClassExpression superCE = existing.getSuperClass();
			superCE.accept(new OWLClassExpressionVisitorAdapter() {
				@Override
				public void visit(OWLObjectSomeValuesFrom svf) {
					svfs.add(svf);
				}

			});
		}
		return svfs;
	}
	
	
	private static Set<OWLObjectSomeValuesFrom> getSVFs(Set<OWLClassExpression> expressions) {
		final Set<OWLObjectSomeValuesFrom> svfs = new HashSet<OWLObjectSomeValuesFrom>();
		for(OWLClassExpression ce : expressions) {
			ce.accept(new OWLClassExpressionVisitorAdapter(){
				@Override
				public void visit(OWLObjectSomeValuesFrom svf) {
					svfs.add(svf);
				}
				
			});
		}
		return svfs;
	}
	
	public static void main(String[] args) throws Exception {
		String catalog = args[0];
		String file = args[1];
		final ParserWrapper pw = new ParserWrapper();
		pw.addIRIMapper(new CatalogXmlIRIMapper(catalog));
		final OWLOntology ontology = pw.parse(file);
		final OWLGraphWrapper g = new OWLGraphWrapper(ontology);
		OWLObjectProperty part_of = g.getOWLObjectPropertyByIdentifier("part_of");
		OWLObjectProperty has_part = g.getOWLObjectPropertyByIdentifier("has_part");
		OWLObjectProperty regulates = g.getOWLObjectPropertyByIdentifier("regulates");
		OWLObjectProperty negatively_regulates = g.getOWLObjectPropertyByIdentifier("negatively_regulates");
		OWLObjectProperty positively_regulates = g.getOWLObjectPropertyByIdentifier("positively_regulates");
		final OWLClassFilter filter = new OWLClassFilter() {
			
			@Override
			public boolean use(OWLClass cls) {
				String id = g.getIdentifier(cls.getIRI());
				return id.startsWith("GO:");
			}
		};
		Set<OWLObjectProperty> props = Sets.newHashSet(part_of);
		Map<OWLClass, Set<OWLObjectSomeValuesFrom>> expressions = getExpressions(ontology, props, filter);
		List<OWLClass> classes = new ArrayList<OWLClass>(expressions.keySet());
		Collections.sort(classes);
		OWLPrettyPrinter pp = new OWLPrettyPrinter(g);
		int count = 0;
		for (OWLClass cls : classes) {
			Set<OWLObjectSomeValuesFrom> svfs = expressions.get(cls);
			for (OWLObjectSomeValuesFrom svf : svfs) {
				StringBuilder sb = new StringBuilder();
				sb.append(pp.render(cls)).append("\t");
				sb.append(pp.render(svf.getProperty())).append("\t");
				sb.append(pp.render(svf.getFiller()));
				System.out.println(sb);
				count += 1;
			}
		}
		System.out.println("Count: "+count);
	}
}
