package owltools.gaf.lego.json;

import org.apache.commons.lang3.tuple.Pair;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLAnnotationValueVisitorEx;
import org.semanticweb.owlapi.model.OWLAnonymousIndividual;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.vocab.OWL2Datatype;

import com.google.gson.annotations.SerializedName;

import owltools.gaf.lego.IdStringManager;
import owltools.gaf.lego.IdStringManager.AnnotationShorthand;

public class JsonAnnotation {
	
	private static final String VALUE_TYPE_IRI = "IRI";
	
	public String key;
	public String value;
	@SerializedName("value-type")
	public String valueType; // optional, defaults to OWL string literal for null
	
	public static JsonAnnotation create(OWLAnnotationProperty p, OWLAnnotationValue value) {
		AnnotationShorthand annotationShorthand = AnnotationShorthand.getShorthand(p.getIRI());
		if (annotationShorthand != null) {
			// try to shorten IRIs for shorthand annotations
			return create(annotationShorthand.name(), value, true);
		}
		// use full IRI strings for non-shorthand annotations
		return create(p.getIRI().toString(), value, false);
	}
	
	public static JsonAnnotation create(OWLDataProperty p, OWLLiteral value) {
		String type = getType(value);
		return create(p.getIRI().toString(), value.getLiteral(), type);
	}
	
	private static String getType(OWLLiteral literal) {
		OWLDatatype datatype = literal.getDatatype();
		String type = null;
		if (datatype.isString() || datatype.isRDFPlainLiteral()) {
			// do nothing
		}
		else if (datatype.isBuiltIn()) {
			type = datatype.getBuiltInDatatype().getPrefixedName();
		}
		return type;
	}
	
	public static Pair<String, String> createSimplePair(OWLAnnotation an) {
		Pair<String, String> result = null;
		// only render shorthand annotations in simple pairs
		AnnotationShorthand shorthand = AnnotationShorthand.getShorthand(an.getProperty().getIRI());
		if (shorthand != null) {
			String value = an.getValue().accept(new OWLAnnotationValueVisitorEx<String>() {

				@Override
				public String visit(IRI iri) {
					return IdStringManager.getId(iri);
				}

				@Override
				public String visit(OWLAnonymousIndividual individual) {
					return null;
				}

				@Override
				public String visit(OWLLiteral literal) {
					return literal.getLiteral();
				}
			});
			if (value != null) {
				result = Pair.of(shorthand.name(), value);
			}
		}
		return result;
	}
	
	private static JsonAnnotation create(final String key, OWLAnnotationValue value, final boolean useShortId) {
		return value.accept(new OWLAnnotationValueVisitorEx<JsonAnnotation>() {

			@Override
			public JsonAnnotation visit(IRI iri) {
				String iriString;
				if (useShortId) {
					iriString = IdStringManager.getId(iri);
				}
				else {
					iriString = iri.toString();
				}
				return create(key, iriString, VALUE_TYPE_IRI);
			}

			@Override
			public JsonAnnotation visit(OWLAnonymousIndividual individual) {
				return null; // do nothing
			}

			@Override
			public JsonAnnotation visit(OWLLiteral literal) {
				return create(key, literal.getLiteral(), getType(literal));
			}
		});
	}
	
	public static JsonAnnotation create(AnnotationShorthand key, String value) {
		return create(key.name(), value, null);
	}
	
	private static JsonAnnotation create(String key, String value, String type) {
		JsonAnnotation a = new JsonAnnotation();
		a.key = key;
		a.value = value;
		a.valueType = type;
		return a;
	}
	
	boolean isIRIValue() {
		return VALUE_TYPE_IRI.equalsIgnoreCase(valueType);
	}
	
	public OWLAnnotationValue createAnnotationValue(OWLDataFactory f) {
		OWLAnnotationValue annotationValue;
		if (isIRIValue()) {
			annotationValue = IRI.create(value);
		}
		else {
			annotationValue = createLiteralInternal(f);
		}
		return annotationValue;
	}
	
	public OWLLiteral createLiteral(OWLDataFactory f) {
		OWLLiteral literal = null;
		if (isIRIValue() == false) {
			literal = createLiteralInternal(f);
		}
		return literal;
	}

	private OWLLiteral createLiteralInternal(OWLDataFactory f) {
		OWLLiteral literal;
		OWL2Datatype datatype = null;
		for(OWL2Datatype current : OWL2Datatype.values()) {
			if (current.getPrefixedName().equalsIgnoreCase(valueType)
					|| current.getShortForm().equalsIgnoreCase(valueType)) {
				datatype = current;
				break;
			}
		}
		if (datatype != null) {
			literal = f.getOWLLiteral(value, datatype);
		}
		else {
			literal = f.getOWLLiteral(value);
		}
		return literal;
	}
	
	

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((key == null) ? 0 : key.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		result = prime * result + ((valueType == null) ? 0 : valueType.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		JsonAnnotation other = (JsonAnnotation) obj;
		if (key == null) {
			if (other.key != null) {
				return false;
			}
		} else if (!key.equals(other.key)) {
			return false;
		}
		if (value == null) {
			if (other.value != null) {
				return false;
			}
		} else if (!value.equals(other.value)) {
			return false;
		}
		if (valueType == null) {
			if (other.valueType != null) {
				return false;
			}
		} else if (!valueType.equals(other.valueType)) {
			return false;
		}
		return true;
	}

}