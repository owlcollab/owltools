package owltools.ontologyrelease;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import owltools.graph.OWLGraphWrapper;
import owltools.io.OWLPrettyPrinter;

public class OntologyMetadata {

	OWLPrettyPrinter pp ;

	public enum MetadataField {
		NUMBER_OF_CLASSES,
		NUMBER_OF_NON_DEPRECATED_CLASSES,
		NUMBER_OF_OBJECT_PROPERTIES,
		NUMBER_OF_AXIOMS,
		AXIOM_TYPE
	}

	public enum MetadataQualifier {
		AXIOM_TYPE,
		INCLUDE_IMPORT_CLOSURE,
		OBJECT_PROPERTY
	}


	public void generate(OWLGraphWrapper g) {
		pp  = new OWLPrettyPrinter(g);

		OWLOntology o = g.getSourceOntology();
		Map<AxiomType<?>,Map<OWLObjectProperty,Integer>> axTypeToPropCountMap;

		Boolean[] bools = new Boolean[] {true, false};

		OWLAnnotationProperty dep = g.getDataFactory().getOWLAnnotationProperty(OWLRDFVocabulary.OWL_DEPRECATED.getIRI());
		for (Boolean ic : bools) {
			generateDatum(MetadataField.NUMBER_OF_CLASSES, MetadataQualifier.INCLUDE_IMPORT_CLOSURE, ic,
					o.getClassesInSignature(ic).size());
			int n = 0;
			for (OWLClass c : o.getClassesInSignature(ic)) {
				if (c.getAnnotations(o, dep).size() == 0) {
					n++;
				}
				else {
					
				}
			}
			generateDatum(MetadataField.NUMBER_OF_NON_DEPRECATED_CLASSES, MetadataQualifier.INCLUDE_IMPORT_CLOSURE, ic, n);
		}
		Set<AxiomType<?>> axTypes = new HashSet<AxiomType<?>>();
		axTypes.add(AxiomType.SUBCLASS_OF);
		axTypes.add(AxiomType.EQUIVALENT_CLASSES);
		for (Boolean ic : bools) {
			for (AxiomType<?> axType : axTypes) {
				// TODO - report importClosure
				generateDatum(MetadataField.NUMBER_OF_AXIOMS,
						MetadataQualifier.AXIOM_TYPE,
						axType,
						o.getAxiomCount(axType, ic));
			}
		}

		Set<OWLObjectProperty> objProps = o.getObjectPropertiesInSignature();

		// ----------------------------------------
		// generate table of object property usage
		// ----------------------------------------
		axTypeToPropCountMap = new HashMap<AxiomType<?>,Map<OWLObjectProperty,Integer>>();
		for (OWLAxiom ax : o.getAxioms()) {
			AxiomType<?> axType = ax.getAxiomType();
			if (!axTypeToPropCountMap.containsKey(axType))
				axTypeToPropCountMap.put(axType, new HashMap<OWLObjectProperty,Integer>());
			Map<OWLObjectProperty, Integer> pcMap = axTypeToPropCountMap.get(axType);
			Set<OWLObjectProperty> refOps = ax.getObjectPropertiesInSignature();
			for (OWLObjectProperty op : refOps) {
				if (!pcMap.containsKey(op))
					pcMap.put(op, 1);
				else
					pcMap.put(op, pcMap.get(op)+1);
			}
		}

		// do not show axiom types for which there is no object property usage
		Set<AxiomType<?>> axTypesToRemove = new HashSet<AxiomType<?>>();
		for (AxiomType<?> axType : axTypeToPropCountMap.keySet()) {
			if (axTypeToPropCountMap.get(axType).size() == 0) {
				axTypesToRemove.add(axType);
			}
		}
		for (AxiomType<?> axType :axTypesToRemove) {
			axTypeToPropCountMap.remove(axType);
		}

		List<String> cols = new Vector<String>();
		for (AxiomType<?> axType : axTypeToPropCountMap.keySet()) {
			cols.add(axType.getName());
		}
		generateDatum(MetadataField.AXIOM_TYPE, 
				null,
				null,
				cols);

		for (OWLObjectProperty op : objProps) {
			List<Integer> intList = new Vector<Integer>();
			for (AxiomType<?> axType : axTypeToPropCountMap.keySet()) {
				Map<OWLObjectProperty, Integer> pcMap = axTypeToPropCountMap.get(axType);
				Integer num = pcMap.get(op);
				if (num == null)
					num = 0;
				intList.add(num);
			}
			generateDatum(MetadataField.NUMBER_OF_AXIOMS, 
					MetadataQualifier.OBJECT_PROPERTY,
					op,
					intList);

		}
	}

	private void generateDatum(MetadataField mf, MetadataQualifier mq, Object qv) {
		print(mf.toString());
		if (mq != null && qv != null) {
			String s;
			if (qv instanceof OWLObject) {
				s = pp.render((OWLObject) qv);
			}
			else {
				s = qv.toString();
			}
			print("."+mq.toString()+"="+s);
		}

	}

	private void generateDatum(MetadataField mf, MetadataQualifier mq, Object qv, Integer number) {
		generateDatum(mf,mq,qv);
		print("\t");
		print(number);
		nl();
	}



	private void generateDatum(MetadataField mf, MetadataQualifier mq, Object qv, List list) {
		generateDatum(mf,mq,qv);
		for (Object item : list) {
			print("\t");
			if (item == null)
				print("null");
			else
				print(item.toString());
		}
		nl();
	}
	private void print(String string) {
		System.out.print(string);

	}
	private void print(Integer num) {
		print(num.toString());

	}

	private void nl() {
		print("\n");
	}


}
